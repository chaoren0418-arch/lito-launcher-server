package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Read-only reconstruction of L1Inventory.getWeight() from a selected source. */
final class LegacyInventoryWeightLoader {
    private LegacyInventoryWeightLoader() {}

    static void healthCheck(Connection connection, String resourceDatabase)
            throws SQLException {
        if (connection == null) throw new IllegalArgumentException("legacy connection is required");
        check(connection, "SELECT item_id,count FROM character_items LIMIT 0");
        for (String table : new String[] {"armor", "weapon", "etcitem"}) {
            check(connection, "SELECT item_id,weight FROM "
                    + LegacyEquipmentMagicLoader.qualified(resourceDatabase, table)
                    + " LIMIT 0");
        }
    }

    static SourceInventoryWeightStats load(
            Connection connection, String resourceDatabase,
            long characterObjectId, double serverWeightRate) throws SQLException {
        if (connection == null || characterObjectId <= 0
                || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid legacy inventory-weight request");
        }
        String sql = """
                SELECT i.id, i.item_id, i.count,
                       a.item_id AS armor_id, a.weight AS armor_weight,
                       w.item_id AS weapon_id, w.weight AS weapon_weight,
                       e.item_id AS etc_id, e.weight AS etc_weight
                  FROM character_items i
                  LEFT JOIN %s a ON a.item_id=i.item_id
                  LEFT JOIN %s w ON w.item_id=i.item_id
                  LEFT JOIN %s e ON e.item_id=i.item_id
                 WHERE i.char_id=?
                 ORDER BY i.id
                """.formatted(
                        LegacyEquipmentMagicLoader.qualified(resourceDatabase, "armor"),
                        LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon"),
                        LegacyEquipmentMagicLoader.qualified(resourceDatabase, "etcitem"));
        int total = 0;
        int itemRows = 0;
        int armorRows = 0;
        int weaponRows = 0;
        int etcRows = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    long count = row.getLong("count");
                    boolean armor = row.getObject("armor_id") != null;
                    boolean weapon = row.getObject("weapon_id") != null;
                    boolean etc = row.getObject("etc_id") != null;
                    int matches = (armor ? 1 : 0) + (weapon ? 1 : 0) + (etc ? 1 : 0);
                    if (count <= 0 || matches != 1) {
                        throw new SQLException("inventory item has invalid count or template: object="
                                + row.getLong("id") + " item=" + row.getInt("item_id"));
                    }
                    int unitWeight;
                    if (armor) {
                        unitWeight = row.getInt("armor_weight");
                        armorRows++;
                    } else if (weapon) {
                        unitWeight = row.getInt("weapon_weight");
                        weaponRows++;
                    } else {
                        unitWeight = row.getInt("etc_weight");
                        etcRows++;
                    }
                    if (unitWeight < 0) {
                        throw new SQLException("inventory template has negative weight: item="
                                + row.getInt("item_id"));
                    }
                    total = BattleInventoryWeight.addWeight(
                            total, BattleInventoryWeight.itemWeight(count, unitWeight));
                    itemRows++;
                }
            }
        }
        return new SourceInventoryWeightStats(
                total, itemRows, armorRows, weaponRows, etcRows, serverWeightRate);
    }

    private static void check(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet ignored = statement.executeQuery()) {
            // Metadata-only startup validation.
        }
    }
}
