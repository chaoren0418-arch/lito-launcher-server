package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

/** SELECT-only reader, always called inside ModernJdbcBackend's owned attempt. */
final class ModernCharacterDataJdbc {
    private ModernCharacterDataJdbc() {}

    /**
     * One statement reads character, instances and their exact catalog together.
     * This also avoids assuming repeatable-read transactions cover MyISAM tables.
     * LEFT JOIN preserves missing or ambiguous catalog rows for rejection rather
     * than silently showing a partial/empty inventory. Every string is bounded
     * before Connector/J materializes it; the original octet length is validated.
     */
    static String sql(String resourceDatabase) {
        return "SELECT " + sourceProjection() + sourceJoins(resourceDatabase) + ownerPredicateAndOrder();
    }
    /** Fixed shared clauses for stricter, single-statement modern source readers. */
    static String sourceProjection() {
        return "c.objid,LEFT(c.char_name,257) AS char_name,OCTET_LENGTH(c.char_name) AS char_name_bytes,"
                + "c.Type AS character_type,c.Sex AS character_sex,c.level,"
                + "c.MaxHp,c.MaxMp,c.CurHp,c.CurMp,c.Ac,c.Str,c.Dex,c.Con,c.Intel,c.Wis,c.Cha,c.Exp,c.Lawful,"
                + "c.OriginalStr,c.OriginalDex,c.OriginalCon,c.OriginalInt,c.OriginalWis,c.Food,"
                + "i.id AS item_objid,i.item_id AS catalog_id,i.count AS item_count,i.is_equipped,"
                + "i.enchantlvl,i.durability,i.is_id,i.bless,i.is_sealed,i.charge_count,"
                + projection("w") + "," + projection("a") + "," + projection("e");
    }
    static String sourceJoins(String resourceDatabase) {
        if (resourceDatabase == null || !resourceDatabase.matches("[A-Za-z0-9_]{1,64}")) {
            throw new IllegalArgumentException("explicit modern catalog database required");
        }
        String catalog = "`" + resourceDatabase + "`.";
        return " FROM characters c LEFT JOIN character_items i ON i.char_id=c.objid"
                + " LEFT JOIN " + catalog + "`weapon` w ON w.item_id=i.item_id"
                + " LEFT JOIN " + catalog + "`armor` a ON a.item_id=i.item_id"
                + " LEFT JOIN " + catalog + "`etcitem` e ON e.item_id=i.item_id";
    }
    static String ownerPredicateAndOrder() {
        return " WHERE c.objid=? AND c.account_name=? AND c.Banned=0 ORDER BY i.id LIMIT "
                + (ModernCharacterData.MAX_ITEMS + 1);
    }
    private static String projection(String alias) {
        return alias + ".item_id AS " + alias + "_id,LEFT(" + alias + ".name_id,257) AS " + alias
                + "_name,OCTET_LENGTH(" + alias + ".name_id) AS " + alias + "_name_bytes,"
                + alias + ".invgfx AS " + alias + "_inventory_graphic,"
                + alias + ".grdgfx AS " + alias + "_ground_graphic," + alias + ".weight AS " + alias + "_weight";
    }
    static Optional<ModernCharacterData.Snapshot> read(Connection connection, ModernBackendScope scope,
            int sourceId, String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        if (sourceId <= 0 || objectId <= 0 || !ModernCharacterData.validText(canonicalAccount)
                || canonicalAccount.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 64) throw unavailable();
        scope.checkActive();
        try (PreparedStatement statement = connection.prepareStatement(sql(resourceDatabase))) {
            statement.setMaxRows(ModernCharacterData.MAX_ITEMS + 1);
            statement.setQueryTimeout((int) Math.max(1L, ((long) scope.remainingMillis() + 999L) / 1000L));
            statement.setLong(1, objectId); statement.setString(2, canonicalAccount);
            try (ResultSet rows = statement.executeQuery()) {
                return decode(rows, scope, sourceId, objectId);
            }
        }
    }

    static Optional<ModernCharacterData.Snapshot> decode(ResultSet rows, ModernBackendScope scope,
            int sourceId, long objectId) throws Exception {
        return decode(rows, scope, sourceId, objectId, null);
    }
    @FunctionalInterface interface ItemObserver {
        void validatedItem(ResultSet row, ModernCharacterData.InventoryItem item) throws Exception;
    }
    /** Observer output is provisional until the complete decoder returns successfully. */
    static Optional<ModernCharacterData.Snapshot> decode(ResultSet rows, ModernBackendScope scope,
            int sourceId, long objectId, ItemObserver observer) throws Exception {
        ModernApplicationProtocol.CharacterRow character = null;
        ModernCharacterData.PersistedValues persisted = null;
        var items = new ArrayList<ModernCharacterData.InventoryItem>();
        boolean emptyInventory = false;
        long previous = 0;
        while (rows.next()) {
            scope.checkActive();
            long id = number(rows, "objid");
            long type = number(rows, "character_type"), sex = number(rows, "character_sex");
            if (id != objectId || type < 0 || type > 65535 || sex < 0 || sex > 1) throw unavailable();
            var row = new ModernApplicationProtocol.CharacterRow(id, text(rows, "char_name"),
                    (int) type, (int) sex, number(rows, "level"));
            var values = new ModernCharacterData.PersistedValues(
                    number(rows,"MaxHp"),number(rows,"MaxMp"),number(rows,"CurHp"),number(rows,"CurMp"),
                    number(rows,"Ac"),number(rows,"Str"),number(rows,"Dex"),number(rows,"Con"),
                    number(rows,"Intel"),number(rows,"Wis"),number(rows,"Cha"),number(rows,"Exp"),number(rows,"Lawful"),
                    number(rows,"OriginalStr"),number(rows,"OriginalDex"),number(rows,"OriginalCon"),
                    number(rows,"OriginalInt"),number(rows,"OriginalWis"),number(rows,"Food"));
            if (character == null) { character = row; persisted = values; }
            else if (!character.equals(row) || !persisted.equals(values)) throw unavailable();
            var optionalItemId = ModernJdbcNumbers.nullable(rows,"item_objid");
            if (optionalItemId.isEmpty()) {
                if (emptyInventory || !items.isEmpty()) throw unavailable();
                emptyInventory = true;
                continue;
            }
            long itemId = optionalItemId.getAsLong();
            if (emptyInventory || items.size() == ModernCharacterData.MAX_ITEMS || itemId <= previous) throw unavailable();
            previous = itemId;
            long catalogId = number(rows, "catalog_id");
            String chosen = null;
            for (String alias : new String[] {"w", "a", "e"}) {
                var matched = ModernJdbcNumbers.nullable(rows,alias + "_id");
                if (matched.isPresent()) {
                    if (chosen != null || matched.getAsLong() != catalogId) throw unavailable();
                    chosen = alias;
                }
            }
            if (chosen == null) throw unavailable();
            var kind = switch (chosen) { case "w" -> ModernCharacterData.ItemKind.WEAPON;
                case "a" -> ModernCharacterData.ItemKind.ARMOR; default -> ModernCharacterData.ItemKind.ETC; };
            var template = new ModernCharacterData.ItemTemplate(catalogId, kind, text(rows, chosen + "_name"),
                    number(rows, chosen + "_inventory_graphic"), number(rows, chosen + "_ground_graphic"),
                    number(rows, chosen + "_weight"));
            var item = new ModernCharacterData.InventoryItem(itemId, template, number(rows,"item_count"),
                    flag(rows,"is_equipped"), number(rows,"enchantlvl"), number(rows,"durability"),
                    flag(rows,"is_id"), number(rows,"bless"), flag(rows,"is_sealed"), number(rows,"charge_count"));
            if (observer != null) observer.validatedItem(rows, item);
            items.add(item);
        }
        scope.checkActive();
        return character == null ? Optional.empty() : Optional.of(new ModernCharacterData.Snapshot(sourceId, character, persisted, items));
    }
    private static long number(ResultSet rows, String column) throws SQLException {
        return ModernJdbcNumbers.required(rows,column);
    }
    private static boolean flag(ResultSet rows, String column) throws SQLException {
        long value = number(rows, column); if (value != 0 && value != 1) throw unavailable(); return value == 1;
    }
    private static String text(ResultSet rows, String column) throws SQLException {
        String value = rows.getString(column);
        long bytes = number(rows, column + "_bytes");
        if (bytes < 1 || bytes > ModernCharacterData.MAX_TEXT_BYTES || !ModernCharacterData.validText(value)) throw unavailable();
        return value;
    }
    private static SQLException unavailable() { return new SQLException("modern persisted character data unavailable"); }
}
