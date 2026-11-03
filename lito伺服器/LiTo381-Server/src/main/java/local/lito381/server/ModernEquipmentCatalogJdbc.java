package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Bounded SELECT-only equipment-rule catalog reader. The caller owns the connection,
 * raw-socket/connection leases and a single operation scope (ModernJdbcBackend.read).
 * This opens no connections, performs no transactions/writes and claims no MyISAM
 * cross-statement consistency. A production facts loader must pin/qualify one immutable
 * source/catalog generation before combining its item snapshot with these rule rows.
 * Malformed/empty combo requirements are rejected, never silently converted into
 * a disabled or universally active combination. Unread fields follow the traced
 * LegacyEquipmentMagicLoader subset, not all newer classic-server effects.
 * Metadata visibility alone never proves optional absence: fixed LIMIT 0 guards
 * require the exact query-phase vendor/SQLState pair and clean cleanup. Guard
 * results are scoped to this Reader, at most once per table/optional-column pair.
 * This does not pin a schema generation. Numeric fields require exact i64 values;
 * integral DECIMAL is accepted, approximate/text types and lossy conversions fail.
 */
final class ModernEquipmentCatalogJdbc {
    private static final int MAX_COLUMNS = 256;
    private static final int MAX_COMBO_TEXT = 4_096;
    private static final String VIP_TABLE = "道具_自訂穿脫能力";
    private static final Set<String> STANDARD_TABLES = Set.of(
            "裝備_weapon_greater_類型","裝備_armor_greater_類型",
            "裝備_weapon_bless_類型","裝備_armor_bless_類型",
            "裝備_weapon_bless_指定","裝備_armor_bless_指定",
            "裝備_weapon_enchant_指定","裝備_armor_enchant_指定",
            "裝備_weapon_enchant_類型","裝備_armor_enchant_類型","裝備_特殊_卡片_能力");
    private record Field(Stat stat, String column, boolean optional) {}
    private static final List<Field> STANDARD = fields("STR:add_str,DEX:add_dex,INT:add_int,WIS:add_wis,CON:add_con,"
            + "CHA:add_cha?,MAX_HP:add_hp?,MAX_MP:add_mp?,POTION_RECOVERY:add_potion_recovery?,SP:add_sp,MR:add_mr,"
            + "HP_REGEN:add_hpr,MP_REGEN:add_mpr,MAGIC_DAMAGE:add_magic_dmg,MAGIC_HIT:add_magic_hit?,"
            + "BLIND:regist_blind?,SLEEP:regist_sleep?,FREEZE:regist_freeze?,STONE:regist_stone?,SUSTAIN:regist_sustain?,"
            + "STUN:regist_stun?,STUN_SUCCESS:add_stun_success?,MELEE_HIT:add_hit?,MELEE_DAMAGE:add_dmg?,"
            + "RANGED_HIT:add_bow_hit?,RANGED_DAMAGE:add_bow_dmg?,PIERCE:add_pierce?,AC:add_ac?,EVASION:add_er?,"
            + "REDUCTION:add_reduction?,MELEE_REDUCTION:add_melee_redux?,RANGED_REDUCTION:add_bow_redux?,"
            + "IGNORE_REDUCTION:add_ignore_reduction?,PVP_REDUCTION:add_pvp_redux?,WATER:add_water?,WIND:add_wind?,EARTH:add_earth?");
    private static final Set<String> OPTIONAL_STANDARD_COLUMNS = STANDARD.stream().filter(Field::optional)
            .map(Field::column).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final List<Field> STONE_FIELDS = fields("STR:add_str,DEX:add_dex,INT:add_int,WIS:add_wis,CON:add_con,"
            + "MAX_HP:add_hp,MAX_MP:add_mp,SP:add_sp,MR:add_mr");
    private static final List<Field> VIP_FIELDS = fields("STR:力量,DEX:敏捷,INT:智力,WIS:精神,CON:體質,MR:抗魔,SP:魔攻,"
            + "HP_REGEN:回血,MP_REGEN:回魔,BLIND:暗黑耐性,SLEEP:睡眠耐性,FREEZE:寒冰耐性,STONE:石化耐性,"
            + "SUSTAIN:支撐耐性,STUN:昏迷耐性,FIRE:火屬性防禦,WATER:水屬性防禦,WIND:風屬性防禦,EARTH:地屬性防禦,"
            + "MELEE_HIT:近戰命中,MELEE_DAMAGE:近戰傷害,RANGED_HIT:遠攻命中,RANGED_DAMAGE:遠攻傷害");
    private static final List<Field> SET_FIELDS = fields("STR:add_str,DEX:add_dex,INT:add_int,WIS:add_wis,CON:add_con,CHA:add_cha,"
            + "MAX_HP:add_hp,MAX_MP:add_mp,MR:m_def,HP_REGEN:add_hpr,MP_REGEN:add_mpr,POTION_RECOVERY:add_potion_recovery,"
            + "MAGIC_DAMAGE:magic_dmg_modifier,MAGIC_HIT:add_magic_hit,BLIND:regist_blind,SLEEP:regist_sleep,FREEZE:regist_freeze,"
            + "STONE:regist_stone,SUSTAIN:regist_sustain,STUN:regist_stun,STUN_SUCCESS:add_stun_success,"
            + "FIRE:defense_fire,WATER:defense_water,WIND:defense_wind,EARTH:defense_earth,MELEE_HIT:add_hitup,MELEE_DAMAGE:add_dmgup,"
            + "RANGED_HIT:add_bow_hitup,RANGED_DAMAGE:add_bow_dmgup,PIERCE:add_pierce,REDUCTION:damage_reduction,"
            + "WEIGHT_REDUCTION:weight_reduction,AC:ac,EVASION:add_er,IGNORE_REDUCTION:add_ignore_reduction,PVP_REDUCTION:add_pvp_redux");
    private static final List<Field> COMBO_FIELDS = fields("STR:add_str,DEX:add_dex,INT:add_int,WIS:add_wis,CON:add_con,CHA:add_cha,"
            + "MAX_HP:add_hp,MAX_MP:add_mp,MR:add_mr,HP_REGEN:add_hpr,MP_REGEN:add_mpr,POTION_RECOVERY:add_potion_recovery,"
            + "MAGIC_DAMAGE:add_magic_dmg,MAGIC_HIT:add_magic_hit,BLIND:regist_blind,SLEEP:regist_sleep,FREEZE:regist_freeze,"
            + "STONE:regist_stone,SUSTAIN:regist_sustain,STUN:regist_stun,STUN_SUCCESS:add_stun_success,"
            + "FIRE:add_fire,WATER:add_water,WIND:add_wind,EARTH:add_earth,MELEE_HIT:add_hit,MELEE_DAMAGE:add_dmg,"
            + "RANGED_HIT:add_bow_hit,RANGED_DAMAGE:add_bow_dmg,PIERCE:add_pierce,REDUCTION:add_reduction,"
            + "WEIGHT_REDUCTION:add_weight_reduction,AC:add_ac,EVASION:add_er,MELEE_REDUCTION:add_melee_redux,"
            + "RANGED_REDUCTION:add_bow_redux,IGNORE_REDUCTION:add_ignore_reduction,PVP_REDUCTION:add_pvp_redux");

    private ModernEquipmentCatalogJdbc() {}

    static Catalog read(Connection connection, ModernBackendScope scope, int sourceId, String resourceDatabase,
                        Set<RuleKey> requiredLookups, Set<Long> requiredSetIds) throws Exception {
        if (connection == null || scope == null || sourceId < 1 || resourceDatabase == null
                || !resourceDatabase.matches("[A-Za-z0-9_]{1,64}") || requiredLookups == null
                || requiredLookups.size() > MAX_RULES || requiredSetIds == null || requiredSetIds.size() > MAX_SET_ROWS) throw unavailable();
        scope.checkActive();
        try {
            if (ModernBackendScope.current() != scope || !connection.isReadOnly()) throw unavailable();
            var reader = new Reader(connection, scope, resourceDatabase);
            var lookups = new LinkedHashMap<RuleKey, RuleValue>();
            boolean vipPresent = !reader.columns(VIP_TABLE, true).isEmpty();
            var keys = requiredLookups.stream().sorted(Comparator.comparing((RuleKey key) -> key.kind().ordinal())
                    .thenComparing(key -> key.itemKind().ordinal()).thenComparingLong(RuleKey::first).thenComparingLong(RuleKey::second)).toList();
            for (RuleKey key : keys) {
                scope.checkActive();
                if (key.kind() == RuleKind.VIP && !vipPresent) continue;
                lookups.put(key, reader.rule(key));
            }
            var sets = new ArrayList<SetRule>();
            for (long setId : requiredSetIds.stream().sorted().toList()) {
                if (setId < 1) throw unavailable();
                reader.sets(setId, sets);
            }
            var result = new Catalog(sourceId, resourceDatabase, lookups, requiredSetIds, sets, reader.combos(),
                    vipPresent ? VipTableState.VERIFIED_PRESENT : VipTableState.VERIFIED_ABSENT);
            scope.checkActive();
            return result;
        } catch (Exception failure) {
            scope.checkActive();
            // SQL/driver exceptions may contain identifiers/credentials/values; no cause escapes.
            throw unavailable();
        }
    }

    private static final class Reader {
        private final Connection connection;
        private final ModernBackendScope scope;
        private final String database;
        private final Map<String, Set<String>> columns = new HashMap<>();
        private final Set<AbsenceKey> verifiedAbsent = new HashSet<>();
        Reader(Connection connection, ModernBackendScope scope, String database) {
            this.connection = connection; this.scope = scope; this.database = database;
        }
        Set<String> columns(String table, boolean optionalTable) throws Exception {
            if (optionalTable && !VIP_TABLE.equals(table)) throw unavailable();
            qualified(table); // closed table-name guard before any metadata query
            scope.checkActive();
            Set<String> cached = columns.get(table);
            if (cached != null) {
                if (!optionalTable && cached.isEmpty()) throw unavailable();
                return cached;
            }
            String sql = "SELECT LEFT(COLUMN_NAME,257) AS COLUMN_NAME,OCTET_LENGTH(COLUMN_NAME) AS COLUMN_NAME_BYTES"
                    + " FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? "
                    + "ORDER BY ORDINAL_POSITION LIMIT " + (MAX_COLUMNS + 1);
            Set<String> result = new HashSet<>();
            try (PreparedStatement statement = prepare(sql, MAX_COLUMNS + 1)) {
                statement.setString(1, database); statement.setString(2, table);
                try (ResultSet row = statement.executeQuery()) {
                    while (row.next()) {
                        scope.checkActive();
                        String column = row.getString("COLUMN_NAME");
                        long bytes = number(row,"COLUMN_NAME_BYTES");
                        if (bytes < 1 || bytes > 256 || !ModernCharacterData.validText(column)
                                || column.getBytes(StandardCharsets.UTF_8).length != bytes || !column.equals(column.strip())
                                || !result.add(column.toLowerCase(Locale.ROOT)) || result.size() > MAX_COLUMNS) throw unavailable();
                    }
                }
            }
            if (!optionalTable && result.isEmpty()) throw unavailable();
            if (optionalTable && result.isEmpty()) verifyAbsent(new AbsenceKey(table,null));
            Set<String> copy = Set.copyOf(result); columns.put(table, copy); return copy;
        }
        String projection(String table, List<Field> fields) throws Exception {
            Set<String> present = columns(table, false);
            var byStat = new EnumMap<Stat, Field>(Stat.class);
            for (Field field : fields) {
                if (!field.optional() && !present.contains(field.column())) throw unavailable();
                if (byStat.put(field.stat(), field) != null) throw unavailable();
            }
            var parts = new ArrayList<String>();
            for (Stat stat : Stat.values()) {
                Field field = byStat.get(stat);
                if (field != null && field.optional() && !present.contains(field.column())) {
                    verifyAbsent(new AbsenceKey(table,field.column()));
                }
                // Zero only for a source-unread field or query-proven optional absence.
                String expression = field != null && present.contains(field.column()) ? "`" + field.column() + "`" : "0";
                parts.add(expression + " AS s" + stat.ordinal());
            }
            return String.join(",", parts);
        }
        void verifyAbsent(AbsenceKey key) throws Exception {
            scope.checkActive();
            boolean missingTable = key.column() == null;
            if (missingTable ? !VIP_TABLE.equals(key.table())
                    : !STANDARD_TABLES.contains(key.table()) || !OPTIONAL_STANDARD_COLUMNS.contains(key.column())) throw unavailable();
            if (verifiedAbsent.contains(key)) return;
            String sql = "SELECT " + (missingTable ? "1" : "`"+key.column()+"`")
                    + " FROM " + qualified(key.table()) + " LIMIT 0";
            try {
                try (PreparedStatement statement = prepareGuard(sql,missingTable)) {
                    statement.setMaxRows(1);
                    statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999L)/1000L));
                    try (ResultSet rows = executeGuard(statement,missingTable)) {
                        if (rows.next()) throw unavailable();
                        scope.checkActive();
                    }
                }
                // A successful guard contradicts the metadata snapshot. Neither
                // permissions nor concurrent schema changes may become zeroes.
                throw unavailable();
            } catch (MissingCatalogElement missing) {
                scope.checkActive();
                if (missing.getSuppressed().length != 0 || missing.sql.getSuppressed().length != 0) throw unavailable();
                verifiedAbsent.add(key);
            }
        }
        PreparedStatement prepareGuard(String sql,boolean missingTable) throws SQLException,MissingCatalogElement {
            try { return connection.prepareStatement(sql); }
            catch (SQLException failure) { throwIfAbsent(failure,missingTable); throw failure; }
        }
        ResultSet executeGuard(PreparedStatement statement,boolean missingTable) throws SQLException,MissingCatalogElement {
            try { return statement.executeQuery(); }
            catch (SQLException failure) { throwIfAbsent(failure,missingTable); throw failure; }
        }
        RuleValue rule(RuleKey key) throws Exception {
            String table, where;
            List<Field> fields;
            int parameters = 2;
            switch (key.kind()) {
                case CARD -> { table = "裝備_特殊_卡片_能力"; where = "item_id=?"; fields = standard(true, true); parameters = 1; }
                case STONE -> { table = "裝備_特殊_魔石_魔法"; where = "item_id=? AND tier=?"; fields = STONE_FIELDS; }
                case VIP -> { table = "道具_自訂穿脫能力"; where = "`道具編號`=?"; fields = VIP_FIELDS; parameters = 1; }
                default -> {
                    String kind = key.itemKind() == ModernCharacterData.ItemKind.ARMOR ? "armor" : "weapon";
                    String type = kind + "_type";
                    switch (key.kind()) {
                        case GREATER_TYPE -> { table = "裝備_" + kind + "_greater_類型"; where = type + "=? AND tier_id=?"; }
                        case BLESS_TYPE -> { table = "裝備_" + kind + "_bless_類型"; where = type + "=? AND bless_id=?"; }
                        case BLESS_ITEM -> { table = "裝備_" + kind + "_bless_指定"; where = "item_id=? AND bless_id=?"; }
                        case ENCHANT_ITEM -> { table = "裝備_" + kind + "_enchant_指定"; where = "item_id=? AND enchant_level=?"; }
                        case ENCHANT_TYPE -> { table = "裝備_" + kind + "_enchant_類型"; where = type + "=? AND over_enchant=?"; }
                        default -> throw unavailable();
                    }
                    fields = standard(key.kind() == RuleKind.GREATER_TYPE || key.kind() == RuleKind.BLESS_ITEM, false);
                }
            }
            String sql = "SELECT " + projection(table, fields) + " FROM " + qualified(table) + " WHERE " + where + " LIMIT 2";
            try (PreparedStatement statement = prepare(sql, 2)) {
                statement.setLong(1, key.first()); if (parameters == 2) statement.setLong(2, key.second());
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) { scope.checkActive(); return RuleValue.absent(); }
                    Ability value = ability(row);
                    if (row.next()) throw unavailable();
                    scope.checkActive(); return RuleValue.present(value);
                }
            }
        }
        void sets(long setId, List<SetRule> destination) throws Exception {
            String table = "裝備_特殊_套裝_能力";
            String sql = "SELECT set_id,piece_count," + projection(table, SET_FIELDS) + " FROM " + qualified(table)
                    + " WHERE set_id=? ORDER BY piece_count LIMIT " + (MAX_SET_ROWS + 1);
            try (PreparedStatement statement = prepare(sql, MAX_SET_ROWS + 1)) {
                statement.setLong(1, setId);
                try (ResultSet row = statement.executeQuery()) {
                    while (row.next()) {
                        scope.checkActive();
                        if (destination.size() == MAX_SET_ROWS || number(row, "set_id") != setId) throw unavailable();
                        destination.add(new SetRule(setId, number(row, "piece_count"), ability(row)));
                    }
                }
            }
        }
        List<ComboRule> combos() throws Exception {
            String table = "裝備_特殊_卡片_組合";
            String sql = "SELECT combo_id,LEFT(require_cards," + (MAX_COMBO_TEXT + 1)
                    + ") AS require_cards,OCTET_LENGTH(require_cards) AS require_cards_bytes," + projection(table, COMBO_FIELDS)
                    + " FROM " + qualified(table) + " ORDER BY combo_id LIMIT " + (MAX_COMBOS + 1);
            var result = new ArrayList<ComboRule>();
            try (PreparedStatement statement = prepare(sql, MAX_COMBOS + 1); ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    scope.checkActive();
                    if (result.size() == MAX_COMBOS) throw unavailable();
                    String text = row.getString("require_cards");
                    long bytes = number(row, "require_cards_bytes");
                    if (text == null || text.isEmpty() || text.length() > MAX_COMBO_TEXT || bytes < 1 || bytes > MAX_COMBO_TEXT) throw unavailable();
                    result.add(new ComboRule(number(row, "combo_id"), parseCards(text), ability(row)));
                }
            }
            return List.copyOf(result);
        }
        PreparedStatement prepare(String sql, int maxRows) throws Exception {
            scope.checkActive();
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                statement.setMaxRows(maxRows);
                statement.setQueryTimeout((int) Math.max(1L, ((long) scope.remainingMillis() + 999L) / 1000L));
                scope.checkActive(); return statement;
            } catch (Exception failure) { statement.close(); throw failure; }
        }
        Ability ability(ResultSet row) throws Exception {
            scope.checkActive();
            var values = new EnumMap<Stat, Long>(Stat.class);
            for (Stat stat : Stat.values()) values.put(stat, number(row, "s" + stat.ordinal()));
            return new Ability(values);
        }
        String qualified(String table) {
            // table is constructed only by the closed RuleKind switch / fixed set-combo readers.
            if (!STANDARD_TABLES.contains(table) && !Set.of(VIP_TABLE,"裝備_特殊_魔石_魔法",
                    "裝備_特殊_套裝_能力","裝備_特殊_卡片_組合").contains(table)) throw new IllegalArgumentException("invalid equipment catalog table");
            return "`" + database + "`.`" + table + "`";
        }
    }
    private record AbsenceKey(String table,String column) {}
    private static void throwIfAbsent(SQLException failure,boolean table) throws MissingCatalogElement {
        if (failure.getErrorCode() == (table ? 1146 : 1054)
                && (table ? "42S02" : "42S22").equals(failure.getSQLState())) throw new MissingCatalogElement(failure);
    }
    /** Query-phase only: setup/ResultSet/statement cleanup errors cannot prove absence. */
    private static final class MissingCatalogElement extends Exception {
        final SQLException sql;
        MissingCatalogElement(SQLException sql) { super("optional equipment catalog element candidate absent"); this.sql=sql; }
    }

    private static List<Field> standard(boolean fire, boolean weight) {
        var result = new ArrayList<>(STANDARD);
        if (fire) result.add(new Field(Stat.FIRE, "add_fire", false));
        if (weight) result.add(new Field(Stat.WEIGHT_REDUCTION, "add_weight_reduction", false));
        return List.copyOf(result);
    }
    private static List<Field> fields(String specification) {
        var result = new ArrayList<Field>();
        for (String token : specification.split(",")) {
            String[] parts = token.split(":");
            boolean optional = parts[1].endsWith("?");
            result.add(new Field(Stat.valueOf(parts[0]), optional ? parts[1].substring(0, parts[1].length() - 1) : parts[1], optional));
        }
        return List.copyOf(result);
    }
    private static Set<Long> parseCards(String text) throws SQLException {
        String[] tokens = text.split(",", -1);
        if (tokens.length < 1 || tokens.length > MAX_COMBO_CARDS) throw unavailable();
        Set<Long> cards = new HashSet<>();
        for (String token : tokens) {
            String normalized = token.trim();
            if (!normalized.matches("[0-9]{1,19}")) throw unavailable();
            try {
                long card = Long.parseLong(normalized);
                if (card <= 1) throw unavailable();
                // Duplicate requirements are a set-membership test in retained containsAll.
                cards.add(card);
            } catch (NumberFormatException invalid) { throw unavailable(); }
        }
        return Set.copyOf(cards);
    }
    private static long number(ResultSet row, String column) throws SQLException {
        return ModernJdbcNumbers.required(row,column);
    }
    private static SQLException unavailable() { return new SQLException("modern equipment catalog is unavailable"); }
}
