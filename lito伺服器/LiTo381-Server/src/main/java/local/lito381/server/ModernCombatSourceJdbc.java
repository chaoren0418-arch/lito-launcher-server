package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Opt-in combat supplement to the SAME bounded character/items/templates SELECT
 * used by EquipmentSourceJdbc. No second item query, global cache, missing-field
 * zero fallback, SQL write or new connection. Catalog rules still use multiple
 * read-only queries: this is explicitly NOT a MyISAM atomic generation.
 */
final class ModernCombatSourceJdbc {
    private static final Map<String, Integer> MATERIALS = Map.ofEntries(
            Map.entry("none",0),Map.entry("liquid",1),Map.entry("web",2),Map.entry("vegetation",3),
            Map.entry("animalmatter",4),Map.entry("paper",5),Map.entry("cloth",6),Map.entry("leather",7),
            Map.entry("wood",8),Map.entry("bone",9),Map.entry("dragonscale",10),Map.entry("iron",11),
            Map.entry("steel",12),Map.entry("copper",13),Map.entry("silver",14),Map.entry("gold",15),
            Map.entry("platinum",16),Map.entry("mithril",17),Map.entry("blackmithril",18),Map.entry("glass",19),
            Map.entry("gemstone",20),Map.entry("mineral",21),Map.entry("oriharukon",22));
    record Source(ModernEquipmentSourceJdbc.Source equipment, Map<Long, ModernCombatProfile.WeaponFacts> weapons) {
        Source {
            Objects.requireNonNull(equipment); weapons = Map.copyOf(weapons);
            int count = 0;
            for (var item : equipment.instances()) {
                if (item.source().template().kind() != ModernCharacterData.ItemKind.WEAPON) continue;
                count++;
                var fact = weapons.get(item.source().objectId());
                if (fact == null || fact.objectId() != item.source().objectId()
                        || fact.catalogId() != item.source().template().catalogId()
                        || fact.retainedType() != item.template().retainedType()) throw new IllegalArgumentException("invalid combat source bundle");
            }
            if (weapons.size() != count) throw new IllegalArgumentException("invalid combat source bundle");
        }
        ModernCombatProfile.Input qualify(ModernEquipmentFacts.Catalog catalog) {
            return new ModernCombatProfile.Input(equipment.qualify(catalog), true, weapons);
        }
        @Override public String toString() { return "CombatSourceBundle[redacted]"; }
    }
    private ModernCombatSourceJdbc() {}

    static Optional<Source> read(Connection connection, ModernBackendScope scope, int sourceId,
            String canonicalAccount, long objectId, String catalogDatabase) throws Exception {
        var collector = new Collector();
        // Collector never escapes on a failed/duplicate/late source row. Only a
        // fully decoded, closed, scope-active source snapshot can publish it.
        var source = ModernEquipmentSourceJdbc.readCombat(connection, scope, sourceId,
                canonicalAccount, objectId, catalogDatabase, collector);
        scope.checkActive();
        return source.map(value -> new Source(value, collector.weapons));
    }

    static String projection() {
        return ",w.dmg_small AS combat_small,w.dmg_large AS combat_large,w.`range` AS combat_range,"
                + "LEFT(w.material,33) AS combat_material,OCTET_LENGTH(w.material) AS combat_material_bytes,"
                + "w.bless AS combat_bless,w.dmg_cri_chance AS combat_critical_chance,w.dmg_cri AS combat_critical_multiplier,"
                + "w.dmg_max_chance AS combat_maximum_chance,i.attr_enchant_kind AS combat_attribute_kind,"
                + "i.attr_enchant_level AS combat_attribute_level";
    }
    static final class Collector {
        private final Map<Long, ModernCombatProfile.WeaponFacts> weapons = new LinkedHashMap<>();
        void accept(ResultSet row, ModernEquipmentFacts.Instance item) throws Exception {
            if (item.source().template().kind() != ModernCharacterData.ItemKind.WEAPON) return;
            var fact = weapon(row, item);
            if (weapons.putIfAbsent(fact.objectId(), fact) != null) throw unavailable();
        }
    }
    static ModernCombatProfile.WeaponFacts weapon(ResultSet rows, ModernEquipmentFacts.Instance item) throws SQLException {
        try {
            if (item == null || item.source().template().kind() != ModernCharacterData.ItemKind.WEAPON) throw unavailable();
            String material = rows.getString("combat_material");
            boolean nullMaterial = rows.wasNull();
            long length = ModernJdbcNumbers.required(rows,"combat_material_bytes");
            if (nullMaterial || material == null || material.length() > 32 || !StandardCharsets.UTF_8.newEncoder().canEncode(material)
                    || material.getBytes(StandardCharsets.UTF_8).length != length || !MATERIALS.containsKey(material)) throw unavailable();
            return new ModernCombatProfile.WeaponFacts(item.source().objectId(),item.source().template().catalogId(),
                    item.template().retainedType(),ModernJdbcNumbers.required(rows,"combat_small"),
                    ModernJdbcNumbers.required(rows,"combat_large"),ModernJdbcNumbers.required(rows,"combat_range"),
                    MATERIALS.get(material),ModernJdbcNumbers.required(rows,"combat_bless"),
                    ModernJdbcNumbers.required(rows,"combat_maximum_chance"),ModernJdbcNumbers.required(rows,"combat_critical_chance"),
                    multiplier(rows),attribute(rows,"combat_attribute_kind"),attribute(rows,"combat_attribute_level"));
        } catch (Exception failure) { throw unavailable(); }
    }
    private static long attribute(ResultSet rows, String column) throws SQLException {
        // These two selected instance columns are nullable in 380_ac_1.sql;
        // CharacterItemPersistenceService:870-871 and Legacy loader:575-576
        // deliberately use getInt without wasNull (SQL NULL = no enchant).
        // Missing columns, noninteger values and errors still fail closed.
        return ModernJdbcNumbers.nullable(rows,column).orElse(0);
    }
    private static double multiplier(ResultSet rows) throws SQLException {
        int index = rows.findColumn("combat_critical_multiplier");
        var metadata = rows.getMetaData();
        if (index < 1 || metadata == null) throw unavailable();
        switch (metadata.getColumnType(index)) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                    Types.DECIMAL, Types.NUMERIC, Types.FLOAT, Types.REAL, Types.DOUBLE -> { }
            default -> throw unavailable();
        }
        double value = rows.getDouble(index);
        if (rows.wasNull() || !Double.isFinite(value) || value < 0) throw unavailable();
        return value;
    }
    private static SQLException unavailable() { return new SQLException("modern combat source unavailable"); }
}
