package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Catalog qualification for the configured-duration, self skill68 armor proc.
 * Not a learned-skill check, manual cast, resource certificate or engine grant.
 * The caller owns a scoped read-only connection and source/character admission. */
final class ModernArmorProtectionCatalog {
    enum Status { MISSING, UNSUPPORTED_TEMPLATE, QUALIFIED }
    static final int SKILL=68, GRAPHIC=228;
    static final String RULESET="MODERN_ARMOR_PROTECTION_68_V1";
    /** Explicit source-preparation result. Still not combat/resource permission.
     * Both reads use the caller's SAME scoped SELECT-only connection, without
     * claiming MyISAM transaction isolation or caching a different source. */
    record BoundSource(ModernPhysicalCombat.Source physical,Snapshot skill) {
        BoundSource {
            Objects.requireNonNull(physical);Objects.requireNonNull(skill);
            if(physical.input().equipment().source().sourceId()!=skill.sourceId()
                    ||!physical.input().equipment().catalog().resourceDatabase().equals(skill.database()))throw invalid();
        }
        ModernArmorProcProtection.Selection select(ModernCombatProfile profile,ModernArmorProcProtection.Actor actor) {
            return skill.select(physical,profile,actor);
        }
        @Override public String toString(){return "ArmorProtectionSource[redacted]";}
    }
    static BoundSource bind(Connection connection,ModernBackendScope scope,ModernPhysicalCombat.Source source)throws Exception {
        Objects.requireNonNull(source);
        var facts=source.input().equipment();
        return new BoundSource(source,read(connection,scope,facts.source().sourceId(),facts.catalog().resourceDatabase()));
    }
    static final class Snapshot {
        private final int sourceId;
        private final String database;
        private final Status status;
        private Snapshot(int sourceId,String database,Status status) {
            this.sourceId=sourceId;this.database=database;this.status=Objects.requireNonNull(status);
        }
        int sourceId(){return sourceId;} String database(){return database;} Status status(){return status;}
        ModernArmorProcProtection.Selection select(ModernPhysicalCombat.Source source,ModernCombatProfile profile,
                ModernArmorProcProtection.Actor actor) {
            Objects.requireNonNull(source);Objects.requireNonNull(profile);Objects.requireNonNull(actor);
            if(sourceId!=source.input().equipment().source().sourceId()
                    ||!database.equals(source.input().equipment().catalog().resourceDatabase()))throw invalid();
            var selection=ModernArmorProcProtection.select(source,profile,actor);
            boolean active=selection.rules().stream().anyMatch(e->e.rule().chance(true)>0||e.rule().chance(false)>0);
            if(active&&status!=Status.QUALIFIED)throw invalid();
            return selection;
        }
        @Override public String toString(){return "ArmorProtectionCatalog[redacted]";}
    }
    private static final String[] FIELDS={
        "skill_level","skill_number","mp_cost","hp_cost","item_id","item_count","reuse_millis","buff_duration",
        "target_to","damage_value","damage_dice","damage_dice_count","probability_value","probability_dice","attr",
        "spell_type","lawful","maximum_range","area","through_flag","template_action","action_id","castgfx","castgfx2",
        "sysmsg_happen","sysmsg_stop","sysmsg_fail","effect_skill_id"
    };
    // Full selected SQL row68. The manual MP/material/reuse/default duration
    // are evidence only: the armor proc does NOT spend or use those values.
    private static final long[] VALUES={9,3,30,0,40318,2,1500,32,4,0,0,0,0,0,0,2,0,-1,0,0,8,19,228,0,314,315,280,0};
    static String sql(String database,boolean effectColumn) {
        String p=prefix(database);
        return "SELECT skill_id AS template_id,skill_level,skill_number,mpConsume AS mp_cost,hpConsume AS hp_cost,"
                +"itemConsumeId AS item_id,itemConsumeCount AS item_count,reuseDelay AS reuse_millis,buffDuration AS buff_duration,"
                +"target_to,damage_value,damage_dice,damage_dice_count,probability_value,probability_dice,attr,type AS spell_type,"
                +"lawful,ranged AS maximum_range,area,`through` AS through_flag,id AS template_action,action_id,castgfx,castgfx2,"
                +"sysmsgID_happen AS sysmsg_happen,sysmsgID_stop AS sysmsg_stop,sysmsgID_fail AS sysmsg_fail,"
                +(effectColumn?"effect_skill_id":"0")+" AS effect_skill_id,LEFT(target,5) AS target,OCTET_LENGTH(target) AS target_bytes"
                +" FROM "+p+"`skills` WHERE skill_id=68 LIMIT 2";
    }
    static String extrasSql(String database) {
        String p=prefix(database);
        return "SELECT 0 AS extension_kind,(itemids IS NULL) AS ids_null,(counts IS NULL) AS counts_null FROM "
                +"(SELECT itemids,counts FROM "+p+"`skills_item` WHERE skill_id=68 LIMIT 2) items UNION ALL "
                +"SELECT 1 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                +"(SELECT skill_id FROM "+p+"`skills_buff_ability` WHERE skill_id=68 LIMIT 1) buffs UNION ALL "
                +"SELECT 2 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                +"(SELECT skill_id FROM "+p+"`skills_負面技能控制` WHERE skill_id=68 LIMIT 1) negative LIMIT 4";
    }
    static Snapshot read(Connection connection,ModernBackendScope scope,int sourceId,String database)throws Exception {
        if(connection==null||scope==null||scope!=ModernBackendScope.current()||sourceId<1)throw invalid();
        prefix(database);scope.checkActive();
        try {
            if(!connection.isReadOnly())throw unavailable();
            boolean effect=ModernSkillsJdbc.hasEffectColumn(connection,scope,database);
            Status status=Status.MISSING;
            try(var statement=connection.prepareStatement(sql(database,effect))) {
                ModernSkillsJdbc.setup(statement,scope,2);scope.checkActive();
                try(var rows=statement.executeQuery()) {
                    int count=0;
                    while(rows.next()) {
                        scope.checkActive();
                        if(++count>1||ModernJdbcNumbers.required(rows,"template_id")!=SKILL)throw unavailable();
                        boolean qualified=true;
                        for(int i=0;i<FIELDS.length;i++)qualified&=ModernJdbcNumbers.required(rows,FIELDS[i])==VALUES[i];
                        String target=rows.getString("target");long length=ModernJdbcNumbers.required(rows,"target_bytes");
                        if(target==null||length<0||length>4||target.getBytes(java.nio.charset.StandardCharsets.UTF_8).length!=length)throw unavailable();
                        qualified&="buff".equals(target);
                        status=qualified?Status.QUALIFIED:Status.UNSUPPORTED_TEMPLATE;
                    }
                }
            }
            scope.checkActive();
            if(status==Status.QUALIFIED&&!extras(connection,scope,database))status=Status.UNSUPPORTED_TEMPLATE;
            scope.checkActive();return new Snapshot(sourceId,database,status);
        }catch(Exception failure){scope.checkActive();throw unavailable();}
    }
    private static boolean extras(Connection connection,ModernBackendScope scope,String database)throws Exception {
        int count=0;boolean qualified=true;
        try(var statement=connection.prepareStatement(extrasSql(database))) {
            ModernSkillsJdbc.setup(statement,scope,4);scope.checkActive();
            try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    scope.checkActive();if(++count>4)throw unavailable();
                    long kind=ModernJdbcNumbers.required(rows,"extension_kind"),ids=ModernJdbcNumbers.required(rows,"ids_null"),counts=ModernJdbcNumbers.required(rows,"counts_null");
                    if(kind<0||kind>2||ids<0||ids>1||counts<0||counts>1)throw unavailable();
                    qualified&=kind==0&&ids==1&&counts==1;
                }
            }
        }
        return qualified&&count==1;
    }
    private static String prefix(String database){ModernPhysicalCombat.identifier(database);return "`"+database+"`.";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid armor protection catalog binding");}
    private static SQLException unavailable(){return new SQLException("modern armor protection catalog unavailable");}
    private ModernArmorProtectionCatalog(){}
}
