package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * SELECT-only skill import inside the DAO's owned connection/scope. Two bounded
 * statements: optional effect-column qualification, then one owner/skills/catalog
 * LEFT JOIN. No primary-source fallback, skill purchase, or native u32 identity.
 * MyISAM does not make those two reads a transactional catalog generation.
 */
final class ModernSkillsJdbc {
    static final int MAX_RAW_ROWS = 512;
    private ModernSkillsJdbc() {}

    static String sql(String catalogDatabase, boolean effectColumn) {
        return sql(catalogDatabase,effectColumn,false);
    }
    static String sql(String catalogDatabase, boolean effectColumn,boolean fireStorm) {
        String table = table(catalogDatabase);
        return "SELECT c.objid,LEFT(c.char_name,257) AS char_name,OCTET_LENGTH(c.char_name) AS char_name_bytes,"
                + "c.Type AS character_type,c.Sex AS character_sex,c.level,"
                + "cs.char_obj_id AS learned_owner,cs.skill_id AS learned_id,s.skill_id AS template_id,s.skill_level,s.skill_number,"
                + "LEFT(s.nameid,257) AS name_token,OCTET_LENGTH(s.nameid) AS name_token_bytes,"
                + "LEFT(s.name,257) AS catalog_name,OCTET_LENGTH(s.name) AS catalog_name_bytes,"
                + "s.mpConsume AS mp_cost,s.hpConsume AS hp_cost,s.itemConsumeId AS item_id,"
                + "s.itemConsumeCount AS item_count,s.reuseDelay AS reuse_millis,"
                + "LEFT(s.target,257) AS target,OCTET_LENGTH(s.target) AS target_bytes,"
                + "s.type AS spell_type,s.action_id,s.castgfx," + (effectColumn ? "s.effect_skill_id" : "0") + " AS effect_skill_id,"
                + "s.buffDuration AS buff_duration,s.target_to,s.damage_value,s.damage_dice,s.damage_dice_count,"
                + "s.probability_value,s.probability_dice,s.attr,s.lawful,s.ranged AS maximum_range,s.area,s.`through` AS through_flag,s.castgfx2"
                + (fireStorm?",s.id AS template_action,s.sysmsgID_happen AS sysmsg_happen,s.sysmsgID_stop AS sysmsg_stop,s.sysmsgID_fail AS sysmsg_fail":"")
                + " FROM characters c LEFT JOIN character_skills cs ON cs.char_obj_id=c.objid"
                + " LEFT JOIN " + table + " s ON s.skill_id=cs.skill_id"
                + " WHERE c.objid=? AND c.account_name=? AND c.Banned=0 ORDER BY cs.skill_id LIMIT " + (MAX_RAW_ROWS + 1);
    }

    static Optional<ModernSkills.Snapshot> read(Connection connection, ModernBackendScope scope,
            int sourceId, String canonicalAccount, long objectId, String catalogDatabase) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,false);
    }
    static Optional<ModernSkills.Snapshot> read(Connection connection, ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean fireStorm) throws Exception {
        if (sourceId <= 0 || objectId <= 0 || !ModernCharacterData.validText(canonicalAccount)
                || canonicalAccount.getBytes(StandardCharsets.UTF_8).length > 64) throw unavailable();
        table(catalogDatabase); // Identifier rejection precedes any JDBC call.
        scope.checkActive();
        try {
            boolean effectColumn = hasEffectColumn(connection, scope, catalogDatabase);
            Optional<ModernSkills.Snapshot> value;
            try (PreparedStatement statement = connection.prepareStatement(sql(catalogDatabase, effectColumn,fireStorm))) {
                setup(statement, scope, MAX_RAW_ROWS + 1);
                statement.setLong(1, objectId); statement.setString(2, canonicalAccount);
                scope.checkActive();
                try (ResultSet rows = statement.executeQuery()) { value = decode(rows, scope, sourceId, objectId,fireStorm); }
            }
            if(fireStorm&&value.isPresent()&&value.get().skills().stream().anyMatch(s->s.support()==ModernSkills.Support.FIRE_STORM)
                    &&!fireExtras(connection,scope,catalogDatabase)) {
                var original=value.get();value=Optional.of(new ModernSkills.Snapshot(original.sourceId(),original.character(),original.skills().stream()
                        .map(s->s.skillId()==70?new ModernSkills.Skill(70,s.nameToken(),s.mpCost(),s.reuseMillis(),ModernSkills.Support.UNSUPPORTED_TEMPLATE):s).toList()));
            }
            scope.checkActive();
            return value;
        } catch (Exception failure) {
            scope.checkActive(); // Cancellation wins over generic source failure.
            throw unavailable(); // Never expose driver SQL, account, or connection details.
        }
    }

    static boolean hasEffectColumn(Connection connection, ModernBackendScope scope, String catalogDatabase) throws Exception {
        // Metadata can hide columns for restricted users. Only this exact query's
        // unknown-column vendor+state pair proves absence; permission errors do not.
        try {
            PreparedStatement opened;
            try { opened = connection.prepareStatement("SELECT `effect_skill_id` FROM " + table(catalogDatabase) + " LIMIT 0"); }
            catch (SQLException failure) { throw queryFailure(failure); }
            try (PreparedStatement statement = opened) {
                setup(statement, scope, 1); // Setup/close errors are NEVER absence evidence.
                ResultSet result;
                scope.checkActive();
                try { result = statement.executeQuery(); }
                catch (SQLException failure) { throw queryFailure(failure); }
                try (ResultSet rows = result) {
                    if (rows.next()) throw unavailable();
                    scope.checkActive();
                }
            }
            scope.checkActive();
            return true;
        } catch (AbsentColumn absence) {
            if (absence.getSuppressed().length != 0) throw unavailable();
            scope.checkActive();
            return false;
        }
    }
    private static SQLException queryFailure(SQLException failure) {
        return failure.getErrorCode() == 1054 && "42S22".equals(failure.getSQLState())
                && failure.getSuppressed().length == 0 && failure.getNextException() == null
                ? new AbsentColumn() : unavailable();
    }
    private static final class AbsentColumn extends SQLException {
        private static final long serialVersionUID = 1L;
        AbsentColumn() { super("qualified absent modern skill column"); }
    }

    private static Optional<ModernSkills.Snapshot> decode(ResultSet rows, ModernBackendScope scope,
            int sourceId, long objectId,boolean fireStorm) throws Exception {
        ModernApplicationProtocol.CharacterRow character = null;
        var skills = new ArrayList<ModernSkills.Skill>();
        long previous = 0;
        int raw = 0;
        boolean empty = false;
        while (rows.next()) {
            scope.checkActive();
            if (++raw > MAX_RAW_ROWS) throw unavailable();
            long id = number(rows, "objid"), type = number(rows, "character_type"), sex = number(rows, "character_sex");
            if (id != objectId || type < 0 || type > 65535 || sex < 0 || sex > 1) throw unavailable();
            var candidate = new ModernApplicationProtocol.CharacterRow(id, text(rows, "char_name"), (int)type, (int)sex, number(rows,"level"));
            if (candidate.level() < 1) throw unavailable();
            if (character == null) character = candidate;
            else if (!character.equals(candidate)) throw unavailable();
            var learned = ModernJdbcNumbers.nullable(rows, "learned_id");
            var learnedOwner = ModernJdbcNumbers.nullable(rows, "learned_owner");
            var template = ModernJdbcNumbers.nullable(rows, "template_id");
            if (learned.isEmpty()) {
                if (raw != 1 || template.isPresent() || learnedOwner.isPresent()) throw unavailable();
                empty = true;
                continue;
            }
            long skillId = learned.getAsLong();
            if (empty || skillId <= previous || learnedOwner.isEmpty() || learnedOwner.getAsLong() != objectId
                    || template.isEmpty() || template.getAsLong() != skillId) throw unavailable();
            previous = skillId;
            long level = number(rows,"skill_level");
            if (level < 0) throw unavailable();
            // Explicit retained admission, including the historical level-zero
            // placeholders. A non-native template is not an executable modern skill.
            if (skillId > NativeSkillMastery.MAX_SKILL_ID || level > NativeSkillMastery.MAX_SKILL_LEVEL
                    || !NativeSkillMastery.importable((int)skillId, (int)level)) continue;
            long mp = nonnegative(rows,"mp_cost"), reuse = nonnegative(rows,"reuse_millis");
            var support = ModernSkills.Support.UNIMPLEMENTED;
            if (skillId == 5) {
                long number = nonnegative(rows,"skill_number"), hp = nonnegative(rows,"hp_cost");
                long item = nonnegative(rows,"item_id"), count = nonnegative(rows,"item_count");
                long spellType = nonnegative(rows,"spell_type"), action = nonnegative(rows,"action_id");
                long gfx = nonnegative(rows,"castgfx"), effect = nonnegative(rows,"effect_skill_id");
                String target = text(rows,"target");
                boolean supported = level == 1 && number == 4 && mp == 10 && hp == 0 && item == 0 && count == 0
                        && reuse == 10 && "none".equals(target) && spellType == 128 && action == 19 && gfx == 169 && effect == 0;
                support = supported ? ModernSkills.Support.TELEPORT : ModernSkills.Support.UNSUPPORTED_TEMPLATE;
            } else if (skillId == 4 || skillId == 1 || skillId==70&&fireStorm) {
                // Exact selected catalog row and retained Light Arrow (not a
                // permissive type=damage switch). No effect alias, area, drain,
                // probability, status, HP/material/lawful cost, or extra gfx.
                long number=nonnegative(rows,"skill_number"), hp=nonnegative(rows,"hp_cost");
                long item=nonnegative(rows,"item_id"), count=nonnegative(rows,"item_count");
                long spellType=nonnegative(rows,"spell_type"), action=nonnegative(rows,"action_id");
                long gfx=nonnegative(rows,"castgfx"), effect=nonnegative(rows,"effect_skill_id");
                String target=text(rows,"target");
                long duration=nonnegative(rows,"buff_duration"), targetTo=nonnegative(rows,"target_to");
                long value=nonnegative(rows,"damage_value"), die=nonnegative(rows,"damage_dice"), dice=nonnegative(rows,"damage_dice_count");
                long probability=nonnegative(rows,"probability_value"), probabilityDie=nonnegative(rows,"probability_dice");
                long attr=nonnegative(rows,"attr"), lawful=number(rows,"lawful"), range=number(rows,"maximum_range");
                long area=nonnegative(rows,"area"), through=nonnegative(rows,"through_flag"), gfx2=nonnegative(rows,"castgfx2");
                boolean supported=level==1&&number==3&&mp==3&&hp==0&&item==0&&count==0&&reuse==10
                        &&"attack".equals(target)&&spellType==64&&action==18&&gfx==167&&effect==0
                        &&duration==0&&targetTo==3&&value==6&&die==10&&dice==1&&probability==0&&probabilityDie==0
                        &&attr==16&&lawful==0&&range==10&&area==0&&through==0&&gfx2==0;
                if(skillId==1) supported=level==1&&number==0&&mp==4&&hp==0&&item==0&&count==0&&reuse==10
                        &&"buff".equals(target)&&spellType==16&&action==19&&gfx==11722&&effect==0
                        &&duration==0&&targetTo==3&&value==2&&die==4&&dice==0&&probability==0&&probabilityDie==0
                        &&attr==4&&lawful==0&&range==-1&&area==0&&through==0&&gfx2==0;
                if(skillId==70){
                    long templateAction=nonnegative(rows,"template_action"),happen=nonnegative(rows,"sysmsg_happen"),stop=nonnegative(rows,"sysmsg_stop"),fail=nonnegative(rows,"sysmsg_fail");
                    supported=level==9&&number==5&&mp==48&&hp==0&&item==0&&count==0&&reuse==2000
                        &&"none".equals(target)&&spellType==64&&action==18&&gfx==1819&&effect==0
                        &&duration==0&&targetTo==3&&value==24&&die==15&&dice==8&&probability==0&&probabilityDie==0
                        &&attr==2&&lawful==0&&range==0&&area==3&&through==0&&gfx2==0&&templateAction==32&&happen==0&&stop==0&&fail==0;
                }
                support=supported?(skillId==70?ModernSkills.Support.FIRE_STORM:skillId==1?ModernSkills.Support.BASIC_HEAL:ModernSkills.Support.LIGHT_ARROW):ModernSkills.Support.UNSUPPORTED_TEMPLATE;
            }
            skills.add(new ModernSkills.Skill((int)skillId, nameToken(rows), mp, reuse, support));
        }
        scope.checkActive();
        return character == null ? Optional.empty() : Optional.of(new ModernSkills.Snapshot(sourceId, character, skills));
    }
    private static long number(ResultSet rows, String column) throws SQLException { return ModernJdbcNumbers.required(rows,column); }
    private static long nonnegative(ResultSet rows, String column) throws SQLException {
        long value = number(rows,column); if (value < 0) throw unavailable(); return value;
    }
    private static String nameToken(ResultSet rows) throws SQLException {
        String value = rows.getString("name_token");
        long bytes = number(rows,"name_token_bytes");
        // Retained catalog skill 87 has nameid='', not a malformed learned grant.
        // Only a proven empty value may use this SAME joined template's display
        // name. NULL/blank/bad bytes never manufacture a token or fallback source.
        if (value != null && value.isEmpty() && bytes == 0) {
            value = rows.getString("catalog_name");
            bytes = number(rows,"catalog_name_bytes");
        }
        if (!ModernCharacterData.validText(value) || bytes < 1 || bytes > 256
                || value.getBytes(StandardCharsets.UTF_8).length != bytes) throw unavailable();
        return value;
    }
    private static String text(ResultSet rows, String column) throws SQLException {
        String value = rows.getString(column);
        long bytes = number(rows,column + "_bytes");
        if (!ModernCharacterData.validText(value) || bytes < 1 || bytes > 256
                || value.getBytes(StandardCharsets.UTF_8).length != bytes) throw unavailable();
        return value;
    }
    private static String table(String database) {
        if (database == null || !database.matches("[A-Za-z0-9_]{1,64}")) throw new IllegalArgumentException("explicit modern skill catalog required");
        return "`" + database + "`.`skills`";
    }
    /** Opt-in only. No malformed/duplicate extension becomes an implicit no-op. */
    static String fireExtrasSql(String database) {
        table(database);String prefix="`"+database+"`.";
        return "SELECT 0 AS extension_kind,(itemids IS NULL) AS ids_null,(counts IS NULL) AS counts_null FROM "
                +"(SELECT itemids,counts FROM "+prefix+"`skills_item` WHERE skill_id=70 LIMIT 2) item_rows UNION ALL "
                +"SELECT 1 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                +"(SELECT skill_id FROM "+prefix+"`skills_buff_ability` WHERE skill_id=70 LIMIT 1) ability_rows LIMIT 3";
    }
    private static boolean fireExtras(Connection connection,ModernBackendScope scope,String database)throws Exception {
        int count=0;boolean supported=true;
        try(var statement=connection.prepareStatement(fireExtrasSql(database))) {
            setup(statement,scope,3);scope.checkActive();try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    scope.checkActive();if(++count>3)throw unavailable();
                    long kind=number(rows,"extension_kind"),ids=number(rows,"ids_null"),counts=number(rows,"counts_null");
                    if(kind<0||kind>1||ids<0||ids>1||counts<0||counts>1)throw unavailable();
                    supported&=kind==0&&ids==1&&counts==1;
                }
            }
        }
        scope.checkActive();return supported&&count==1;
    }
    static void setup(PreparedStatement statement, ModernBackendScope scope, int maxRows) throws Exception {
        scope.checkActive(); statement.setMaxRows(maxRows);
        statement.setQueryTimeout((int)Math.max(1L, ((long)scope.remainingMillis() + 999L) / 1000L));
        scope.checkActive();
    }
    private static SQLException unavailable() { return new SQLException("modern learned skills unavailable"); }
}
