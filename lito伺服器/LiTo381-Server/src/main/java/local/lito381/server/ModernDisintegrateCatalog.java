package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Explicit, SELECT-only skill77 qualification for v27 catalog and preparation.
 * Older catalog peers retain their original wire meanings. No default-source
 * or malformed-template fallback.
 */
final class ModernDisintegrateCatalog {
    enum Status { UNLEARNED, UNSUPPORTED_TEMPLATE, QUALIFIED }
    record Snapshot(TournamentPresence.CharacterKey key,Status status) {
        Snapshot {java.util.Objects.requireNonNull(key);java.util.Objects.requireNonNull(status);}
        boolean accepts(ModernDisintegrate.Profile profile){return status==Status.QUALIFIED&&key.equals(profile.key());}
        @Override public String toString(){return "ModernDisintegrateCatalog[redacted]";}
    }
    private ModernDisintegrateCatalog(){}
    // Exact selected tuple. 500 is a runtime gate; this row's lawful is +1.
    private static final String[] FIELDS={"skill_level","skill_number","mp_cost","hp_cost","item_id","item_count","reuse_millis",
            "buff_duration","target_to","damage_value","damage_dice","damage_dice_count","probability_value","probability_dice",
            "attr","spell_type","lawful","maximum_range","area","through_flag","template_action","action_id","castgfx","castgfx2",
            "sysmsg_happen","sysmsg_stop","sysmsg_fail","effect_skill_id"};
    private static final long[] VALUES={10,4,70,0,0,0,5000,0,3,190,15,8,0,0,16,64,1,10,0,0,16,18,11746,0,0,0,0,0};
    static String sql(String database,boolean effectColumn) {
        String table=prefix(database)+"`skills`";
        return "SELECT c.objid,cs.char_obj_id AS learned_owner,cs.skill_id AS learned_id,s.skill_id AS template_id,"
                +"s.skill_level,s.skill_number,s.mpConsume AS mp_cost,s.hpConsume AS hp_cost,s.itemConsumeId AS item_id,"
                +"s.itemConsumeCount AS item_count,s.reuseDelay AS reuse_millis,s.buffDuration AS buff_duration,s.target_to,"
                +"s.damage_value,s.damage_dice,s.damage_dice_count,s.probability_value,s.probability_dice,s.attr,s.type AS spell_type,"
                +"s.lawful,s.ranged AS maximum_range,s.area,s.`through` AS through_flag,s.id AS template_action,s.action_id,s.castgfx,s.castgfx2,"
                +"s.sysmsgID_happen AS sysmsg_happen,s.sysmsgID_stop AS sysmsg_stop,s.sysmsgID_fail AS sysmsg_fail,"
                +(effectColumn?"s.effect_skill_id":"0")+" AS effect_skill_id,LEFT(s.target,7) AS target,OCTET_LENGTH(s.target) AS target_bytes"
                +" FROM characters c LEFT JOIN character_skills cs ON cs.char_obj_id=c.objid AND cs.skill_id=77"
                +" LEFT JOIN "+table+" s ON s.skill_id=cs.skill_id"
                +" WHERE c.objid=? AND c.account_name=? AND c.Banned=0 LIMIT 2";
    }
    static String extrasSql(String database) {
        String p=prefix(database);
        return "SELECT 0 AS extension_kind,(itemids IS NULL) AS ids_null,(counts IS NULL) AS counts_null FROM "
                +"(SELECT itemids,counts FROM "+p+"`skills_item` WHERE skill_id=77 LIMIT 2) items UNION ALL "
                +"SELECT 1 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                +"(SELECT skill_id FROM "+p+"`skills_buff_ability` WHERE skill_id=77 LIMIT 1) buffs UNION ALL "
                +"SELECT 2 AS extension_kind,1 AS ids_null,1 AS counts_null FROM "
                +"(SELECT skill_id FROM "+p+"`skills_負面技能控制` WHERE skill_id=77 LIMIT 1) negative LIMIT 4";
    }
    static Optional<Snapshot> read(Connection connection,ModernBackendScope scope,int sourceId,
            String account,long objectId,String database)throws Exception {
        if(sourceId<1||objectId<1||!ModernCharacterData.validText(account)||account.getBytes(StandardCharsets.UTF_8).length>64)throw unavailable();
        prefix(database);scope.checkActive();
        try {
            boolean effect=ModernSkillsJdbc.hasEffectColumn(connection,scope,database);
            Status status=null;
            try(var statement=connection.prepareStatement(sql(database,effect))) {
                ModernSkillsJdbc.setup(statement,scope,2);statement.setLong(1,objectId);statement.setString(2,account);scope.checkActive();
                try(var rows=statement.executeQuery()) {
                    int count=0;
                    while(rows.next()) {
                        scope.checkActive();if(++count>1||number(rows,"objid")!=objectId)throw unavailable();
                        var learned=ModernJdbcNumbers.nullable(rows,"learned_id");
                        var owner=ModernJdbcNumbers.nullable(rows,"learned_owner");
                        var template=ModernJdbcNumbers.nullable(rows,"template_id");
                        if(learned.isEmpty()) {
                            if(owner.isPresent()||template.isPresent())throw unavailable();status=Status.UNLEARNED;
                        }else {
                            if(learned.getAsLong()!=77||owner.isEmpty()||owner.getAsLong()!=objectId||template.isEmpty()||template.getAsLong()!=77)throw unavailable();
                            boolean qualified=true;
                            for(int i=0;i<FIELDS.length;i++)qualified&=number(rows,FIELDS[i])==VALUES[i];
                            String target=rows.getString("target");long bytes=number(rows,"target_bytes");
                            if(target==null||bytes<1||bytes>6||target.getBytes(StandardCharsets.UTF_8).length!=bytes)throw unavailable();
                            qualified&="attack".equals(target);
                            status=qualified?Status.QUALIFIED:Status.UNSUPPORTED_TEMPLATE;
                        }
                    }
                }
            }
            scope.checkActive(); // A false final next() may still complete cancellation.
            if(status==Status.QUALIFIED&&!extras(connection,scope,database))status=Status.UNSUPPORTED_TEMPLATE;
            scope.checkActive();return status==null?Optional.empty():Optional.of(new Snapshot(new TournamentPresence.CharacterKey(sourceId,objectId),status));
        }catch(Exception failure){scope.checkActive();throw unavailable();}
    }
    private static boolean extras(Connection connection,ModernBackendScope scope,String database)throws Exception {
        int count=0;boolean qualified=true;
        try(var statement=connection.prepareStatement(extrasSql(database))) {
            ModernSkillsJdbc.setup(statement,scope,4);scope.checkActive();try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    scope.checkActive();if(++count>4)throw unavailable();
                    long kind=number(rows,"extension_kind"),ids=number(rows,"ids_null"),counts=number(rows,"counts_null");
                    if(kind<0||kind>2||ids<0||ids>1||counts<0||counts>1)throw unavailable();
                    qualified&=kind==0&&ids==1&&counts==1;
                }
            }
        }
        return qualified&&count==1;
    }
    private static long number(ResultSet rows,String column)throws SQLException{return ModernJdbcNumbers.required(rows,column);}
    private static String prefix(String db) {
        if(db==null||!db.matches("[A-Za-z0-9_]{1,64}"))throw new IllegalArgumentException("explicit disintegrate catalog required");return "`"+db+"`.";
    }
    private static SQLException unavailable(){return new SQLException("modern disintegrate source unavailable");}
}
