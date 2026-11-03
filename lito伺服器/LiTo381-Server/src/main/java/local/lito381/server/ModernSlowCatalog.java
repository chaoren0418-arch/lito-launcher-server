package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.sql.Types;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** SELECT-only source qualification for the future explicit skill29 contract.
 * The caller owns the read-only connection and deadline. A qualified catalog
 * does not bypass authenticated preparation, version/target or cast admission. */
final class ModernSlowCatalog {
    enum Status { UNLEARNED, UNSUPPORTED_TEMPLATE, QUALIFIED }
    record Snapshot(TournamentPresence.CharacterKey key, Status status, OptionalLong accessLevel) {
        Snapshot(TournamentPresence.CharacterKey key,Status status) { this(key,status,OptionalLong.empty()); }
        Snapshot { Objects.requireNonNull(key); Objects.requireNonNull(status);Objects.requireNonNull(accessLevel); }
        boolean gmTarget() { return accessLevel.orElseThrow(()->new IllegalArgumentException("unqualified slow target access"))>=200; }
        boolean accepts(ModernSlow.Profile profile) {
            return status == Status.QUALIFIED && profile != null && key.equals(profile.key());
        }
        @Override public String toString() { return "ModernSlowCatalog[redacted]"; }
    }
    private static final String[] FIELDS = {
        "skill_level", "skill_number", "mp_cost", "hp_cost", "item_id", "item_count", "reuse_millis",
        "buff_duration", "target_to", "damage_value", "damage_dice", "damage_dice_count", "probability_value",
        "probability_dice", "attr", "spell_type", "lawful", "maximum_range", "area", "through_flag",
        "template_action", "action_id", "castgfx", "castgfx2", "sysmsg_happen", "sysmsg_stop", "sysmsg_fail", "effect_skill_id"
    };
    // Exact retained 380_db.sql row29. ranged=-1 and happen=277; do not
    // copy the older BattleSpellCatalog's range8/stop277 reinterpretation.
    private static final long[] VALUES = {4,4,20,0,40318,1,500,64,3,0,0,0,0,12,8,1,0,-1,0,0,16,19,752,0,277,0,280,0};

    static String sql(String database, boolean effectColumn) {
        String table = prefix(database) + "`skills`";
        return "SELECT c.objid,c.AccessLevel AS access_level,cs.char_obj_id AS learned_owner,cs.skill_id AS learned_id,s.skill_id AS template_id,"
                + "s.skill_level,s.skill_number,s.mpConsume AS mp_cost,s.hpConsume AS hp_cost,s.itemConsumeId AS item_id,"
                + "s.itemConsumeCount AS item_count,s.reuseDelay AS reuse_millis,s.buffDuration AS buff_duration,s.target_to,"
                + "s.damage_value,s.damage_dice,s.damage_dice_count,s.probability_value,s.probability_dice,s.attr,s.type AS spell_type,"
                + "s.lawful,s.ranged AS maximum_range,s.area,s.`through` AS through_flag,s.id AS template_action,s.action_id,s.castgfx,s.castgfx2,"
                + "s.sysmsgID_happen AS sysmsg_happen,s.sysmsgID_stop AS sysmsg_stop,s.sysmsgID_fail AS sysmsg_fail,"
                + (effectColumn ? "s.effect_skill_id" : "0") + " AS effect_skill_id,LEFT(s.target,5) AS target,OCTET_LENGTH(s.target) AS target_bytes"
                + " FROM characters c LEFT JOIN character_skills cs ON cs.char_obj_id=c.objid AND cs.skill_id=29"
                + " LEFT JOIN " + table + " s ON s.skill_id=cs.skill_id"
                + " WHERE c.objid=? AND c.account_name=? AND c.Banned=0 LIMIT 2";
    }
    static String extrasSql(String database) {
        String p = prefix(database);
        return "SELECT 0 AS extension_kind,(itemids IS NULL) AS ids_null,(counts IS NULL) AS counts_null FROM "
                + "(SELECT itemids,counts FROM " + p + "`skills_item` WHERE skill_id=29 LIMIT 2) items UNION ALL "
                + "SELECT 1 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                + "(SELECT skill_id FROM " + p + "`skills_buff_ability` WHERE skill_id=29 LIMIT 1) buffs UNION ALL "
                + "SELECT 2 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                + "(SELECT skill_id FROM " + p + "`skills_魔屏無效技能` WHERE skill_id=29 LIMIT 1) counter LIMIT 4";
    }
    static Optional<Snapshot> read(Connection connection, ModernBackendScope scope, int sourceId,
            String account, long objectId, String database) throws Exception {
        if (sourceId < 1 || objectId < 1 || !ModernCharacterData.validText(account)
                || account.getBytes(StandardCharsets.UTF_8).length > 64) throw unavailable();
        prefix(database); scope.checkActive();
        try {
            boolean effect = ModernSkillsJdbc.hasEffectColumn(connection, scope, database);
            Status status = null;OptionalLong access=OptionalLong.empty();
            try (var statement = connection.prepareStatement(sql(database, effect))) {
                ModernSkillsJdbc.setup(statement, scope, 2);
                statement.setLong(1, objectId); statement.setString(2, account); scope.checkActive();
                try (var rows = statement.executeQuery()) {
                    int count = 0;
                    while (rows.next()) {
                        scope.checkActive();
                        if (++count > 1 || number(rows, "objid") != objectId) throw unavailable();
                        access=OptionalLong.of(number(rows,"access_level"));
                        var learned = ModernJdbcNumbers.nullable(rows, "learned_id");
                        var owner = ModernJdbcNumbers.nullable(rows, "learned_owner");
                        var template = ModernJdbcNumbers.nullable(rows, "template_id");
                        if (learned.isEmpty()) {
                            if (owner.isPresent() || template.isPresent()) throw unavailable();
                            status = Status.UNLEARNED;
                        } else {
                            if (learned.getAsLong() != 29 || owner.isEmpty() || owner.getAsLong() != objectId
                                    || template.isEmpty() || template.getAsLong() != 29) throw unavailable();
                            boolean qualified = true;
                            for (int i = 0; i < FIELDS.length; i++) qualified &= number(rows, FIELDS[i]) == VALUES[i];
                            String target = rows.getString("target"); long bytes = number(rows, "target_bytes");
                            if (target == null || bytes < 1 || bytes > 4
                                    || target.getBytes(StandardCharsets.UTF_8).length != bytes) throw unavailable();
                            qualified &= "buff".equals(target);
                            status = qualified ? Status.QUALIFIED : Status.UNSUPPORTED_TEMPLATE;
                        }
                    }
                }
            }
            scope.checkActive();
            if (status == Status.QUALIFIED) {
                // Read every mandatory table, even after an unsupported extension.
                // Missing/denied policy data is unavailable, never a default rule.
                boolean supported = extras(connection, scope, database);
                supported &= negative(connection, scope, database);
                supported &= boss(connection, scope, database);
                if (!supported) status = Status.UNSUPPORTED_TEMPLATE;
            }
            scope.checkActive();
            return status == null ? Optional.empty() : Optional.of(new Snapshot(new TournamentPresence.CharacterKey(sourceId, objectId), status,access));
        } catch (Exception failure) {
            scope.checkActive();
            throw unavailable();
        }
    }
    private static boolean extras(Connection connection, ModernBackendScope scope, String database) throws Exception {
        scope.checkActive();
        int count = 0; boolean qualified = true;
        try (var statement = connection.prepareStatement(extrasSql(database))) {
            ModernSkillsJdbc.setup(statement, scope, 4); scope.checkActive();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    scope.checkActive(); if (++count > 4) throw unavailable();
                    long kind = number(rows, "extension_kind"), ids = number(rows, "ids_null"), counts = number(rows, "counts_null");
                    if (kind < 0 || kind > 2 || ids < 0 || ids > 1 || counts < 0 || counts > 1) throw unavailable();
                    qualified &= kind == 0 && ids == 1 && counts == 1;
                }
            }
        }
        scope.checkActive(); return qualified && count == 1;
    }

    static String negativeSql(String database) {
        return "SELECT skill_id,prob_high,prob_equal,prob_low,bonus_str,bonus_dex,bonus_int,"
                + "check_resist_type,check_magic_level,check_magic_hit,mr_multiplier,con_multiplier,wis_multiplier"
                + " FROM " + prefix(database) + "`skills_負面技能控制` WHERE skill_id=29 LIMIT 2";
    }
    static String bossSql(String database) {
        return "SELECT skill_id,modifier FROM " + prefix(database) + "`skills_負面boss免疫` WHERE skill_id=29 LIMIT 2";
    }
    private static boolean negative(Connection connection, ModernBackendScope scope, String database) throws Exception {
        scope.checkActive();
        int count = 0; boolean qualified = true;
        String[] integers = {"skill_id","prob_high","prob_equal","prob_low","check_resist_type","check_magic_level","check_magic_hit"};
        long[] values = {29,55,45,35,0,0,1};
        String[] coefficients = {"bonus_str","bonus_dex","bonus_int","mr_multiplier","con_multiplier","wis_multiplier"};
        String[] factors = {"0","0","0.3","0.05","0","0"};
        try (var statement = connection.prepareStatement(negativeSql(database))) {
            ModernSkillsJdbc.setup(statement, scope, 2); scope.checkActive();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    scope.checkActive(); if (++count > 2) throw unavailable();
                    for (int i = 0; i < integers.length; i++) qualified &= number(rows, integers[i]) == values[i];
                    for (int i = 0; i < coefficients.length; i++) qualified &= coefficient(rows, coefficients[i], factors[i]);
                }
            }
        }
        scope.checkActive(); return qualified && count == 1;
    }
    private static boolean boss(Connection connection, ModernBackendScope scope, String database) throws Exception {
        scope.checkActive();
        int count = 0; boolean qualified = true;
        try (var statement = connection.prepareStatement(bossSql(database))) {
            ModernSkillsJdbc.setup(statement, scope, 2); scope.checkActive();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    scope.checkActive(); if (++count > 2) throw unavailable();
                    qualified &= number(rows, "skill_id") == 29 && number(rows, "modifier") == 5;
                }
            }
        }
        scope.checkActive(); return qualified && count == 1;
    }
    /** The retained coefficient columns are DOUBLE, unlike exact integer facts.
     * Do not CAST/round them in SQL, coerce strings, or accept epsilon-near values.
     * DECIMAL coefficients must compare exactly without conversion to double. */
    private static boolean coefficient(ResultSet rows, String column, String expected) throws SQLException {
        int index = rows.findColumn(column);
        var metadata = rows.getMetaData();
        if (index < 1 || metadata == null) throw unavailable();
        int type = metadata.getColumnType(index);
        var wanted = new BigDecimal(expected);
        if (type == Types.DOUBLE || type == Types.FLOAT) {
            double value = rows.getDouble(index);
            if (rows.wasNull() || !Double.isFinite(value)) throw unavailable();
            return value == wanted.doubleValue();
        }
        if (type != Types.DECIMAL && type != Types.NUMERIC && type != Types.TINYINT
                && type != Types.SMALLINT && type != Types.INTEGER && type != Types.BIGINT) throw unavailable();
        var value = rows.getBigDecimal(index);
        if (value == null || rows.wasNull()) throw unavailable();
        return value.compareTo(wanted) == 0;
    }
    private static long number(ResultSet rows, String column) throws SQLException { return ModernJdbcNumbers.required(rows, column); }
    private static String prefix(String db) {
        if (db == null || !db.matches("[A-Za-z0-9_]{1,64}")) throw new IllegalArgumentException("explicit slow catalog required");
        return "`" + db + "`.";
    }
    private static SQLException unavailable() { return new SQLException("modern slow source unavailable"); }
    private ModernSlowCatalog() { }
}
