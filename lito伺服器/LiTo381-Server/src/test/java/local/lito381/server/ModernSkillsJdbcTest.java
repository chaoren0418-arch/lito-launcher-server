package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Owned synthetic JDBC only. All unexpected/write operations fail the test. */
class ModernSkillsJdbcTest {
    private ModernSkills.Snapshot readFire(Jdbc jdbc)throws Exception {
        try(var config=config()){return scoped(scope->dao(config,jdbc).skillsCharacter(7,"owner",3333,"catalog",true)).orElseThrow();}
    }
    private static Map<String,Object> fire(){
        var r=lightArrow();r.put("learned_id",70L);r.put("template_id",70L);r.put("skill_level",9L);r.put("skill_number",5L);
        r.put("name_token","$1975");r.put("mp_cost",48L);r.put("reuse_millis",2000L);r.put("target","none");r.put("target_bytes",4L);
        r.put("castgfx",1819L);r.put("damage_value",24L);r.put("damage_dice",15L);r.put("damage_dice_count",8L);
        r.put("attr",2L);r.put("maximum_range",0L);r.put("area",3L);r.put("template_action",32L);
        r.put("sysmsg_happen",0L);r.put("sysmsg_stop",0L);r.put("sysmsg_fail",0L);return r;
    }
    @Test void fireStormRequiresExplicitReaderOptInAndExactSameSourceNullItemExtension()throws Exception {
        var old=new Jdbc(List.of(fire()));assertEquals(ModernSkills.Support.UNIMPLEMENTED,read(old).skills().get(0).support());assertEquals(2,old.sql.size());
        var jdbc=new Jdbc(List.of(lightArrow(),skill(5),fire()));var result=readFire(jdbc);
        assertEquals(ModernSkills.Support.FIRE_STORM,result.skills().get(2).support());assertEquals(48,result.skills().get(2).mpCost());
        assertEquals(2000,result.skills().get(2).reuseMillis());assertEquals(3,jdbc.sql.size());assertEquals(List.of(1,513,3),jdbc.maxRows);
        assertEquals(ModernSkillsJdbc.fireExtrasSql("catalog"),jdbc.sql.get(2));assertEquals(3,jdbc.statementCloses);assertEquals(3,jdbc.resultCloses);assertEquals(1,jdbc.connectionCloses);
    }
    @Test void fireStormAllQualifiedTupleDifferencesRejectWithoutGuessingAlternateReuseOrEffect()throws Exception {
        for(String field:List.of("skill_level","skill_number","mp_cost","reuse_millis","hp_cost","item_id","item_count","spell_type","action_id","castgfx","effect_skill_id",
                "buff_duration","target_to","damage_value","damage_dice","damage_dice_count","probability_value","probability_dice","attr","lawful","maximum_range","area","through_flag","castgfx2","template_action","sysmsg_happen","sysmsg_stop","sysmsg_fail")){
            var row=fire();row.put(field,((Number)row.get(field)).longValue()+1);var j=new Jdbc(List.of(row));
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,readFire(j).skills().get(0).support(),field);assertEquals(2,j.sql.size());
            row=fire();row.put(field,null);var bad=new Jdbc(List.of(row));assertThrows(SQLException.class,()->readFire(bad),field);assertEquals(1,bad.connectionCloses);
        }
        for(long reuse:List.of(400L,500L)){var row=fire();row.put("reuse_millis",reuse);assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,readFire(new Jdbc(List.of(row))).skills().get(0).support());}
    }
    @Test void missingDuplicateMaterialOrBuffExtensionNeverBecomesAnImplicitNoOp()throws Exception {
        var good=Map.<String,Object>of("extension_kind",0L,"ids_null",1L,"counts_null",1L);
        for(var extra:List.of(List.<Map<String,Object>>of(),List.of(good,good),List.of(Map.<String,Object>of("extension_kind",0L,"ids_null",0L,"counts_null",1L)),
                List.of(good,Map.<String,Object>of("extension_kind",1L,"ids_null",1L,"counts_null",1L)))){
            var j=new Jdbc(List.of(fire()));j.extraRows=extra;assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,readFire(j).skills().get(0).support());assertEquals(1,j.connectionCloses);
        }
        var malformed=new Jdbc(List.of(fire()));malformed.extraRows=List.of(Map.of("extension_kind",0L,"ids_null",2L,"counts_null",1L));assertThrows(SQLException.class,()->readFire(malformed));
        var absent=new Jdbc(List.of(skill(5)));readFire(absent);assertEquals(2,absent.sql.size());
    }
    @Test void exactRetainedBasicHealIsQualifiedAlongsideArrowAndTeleportFromSameJoinedSource()throws Exception{
        var jdbc=new Jdbc(List.of(basicHeal(),lightArrow(),skill(5)));var s=read(jdbc);
        assertEquals(ModernSkills.Support.BASIC_HEAL,s.skills().get(0).support());assertEquals(1,s.skills().get(0).skillId());
        assertEquals(4,s.skills().get(0).mpCost());assertEquals(10,s.skills().get(0).reuseMillis());assertEquals(2,jdbc.sql.size());
        assertTrue(s.lightArrow().isPresent());assertTrue(s.teleport().isPresent());
    }
    @Test void basicHealRequiresEveryActualTupleFieldAndNeverInfersAnEffectAlias()throws Exception{
        for(String key:List.of("skill_number","mp_cost","reuse_millis","hp_cost","item_id","item_count","spell_type","action_id","castgfx","effect_skill_id",
                "buff_duration","target_to","damage_value","damage_dice","damage_dice_count","probability_value","probability_dice","attr","lawful","maximum_range","area","through_flag","castgfx2")){
            var row=basicHeal();row.put(key,((Number)row.get(key)).longValue()+1);assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(row))).skills().get(0).support(),key);
        }
        var row=basicHeal();row.put("target","heal");assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(row))).skills().get(0).support());
        for(String field:List.of("skill_number","mp_cost","reuse_millis","hp_cost","item_id","item_count","spell_type","action_id","castgfx","effect_skill_id",
                "buff_duration","target_to","damage_value","damage_dice","damage_dice_count","probability_value","probability_dice","attr","area","through_flag","castgfx2")){
            row=basicHeal();row.put(field,-1L);reject(List.of(row));
            row=basicHeal();row.put(field,null);reject(List.of(row));
        }
        // Lawful and range are intentionally signed source fields, but NULL is
        // still unknown. Retained BasicHeal range=-1 is the qualified tuple.
        for(String field:List.of("lawful","maximum_range")){
            row=basicHeal();row.put(field,null);reject(List.of(row));
        }
    }
    private static Map<String,Object> basicHeal(){
        var row=lightArrow();row.put("learned_id",1L);row.put("template_id",1L);row.put("skill_number",0L);row.put("name_token","$1436");
        row.put("mp_cost",4L);row.put("target","buff");row.put("target_bytes",4L);row.put("spell_type",16L);row.put("action_id",19L);row.put("castgfx",11722L);
        row.put("damage_value",2L);row.put("damage_dice",4L);row.put("damage_dice_count",0L);row.put("attr",4L);row.put("maximum_range",-1L);return row;
    }
    @Test void actualLightArrowTemplateAndTeleportQualifyTogetherWithoutExtraQueries() throws Exception {
        var jdbc=new Jdbc(List.of(lightArrow(),skill(5)));
        var snapshot=read(jdbc);var arrow=snapshot.lightArrow().orElseThrow();
        assertEquals(4,arrow.skillId());assertEquals("$1439",arrow.nameToken());assertEquals(3,arrow.mpCost());assertEquals(10,arrow.reuseMillis());
        assertTrue(snapshot.teleport().isPresent());assertEquals(2,jdbc.sql.size());
        assertEquals(2,jdbc.statementCloses);assertEquals(2,jdbc.resultCloses);assertEquals(1,jdbc.connectionCloses);
        assertTrue(jdbc.sql.get(1).contains("s.damage_value,s.damage_dice,s.damage_dice_count"));
        assertTrue(jdbc.sql.get(1).contains("s.ranged AS maximum_range"));assertTrue(jdbc.readOnly);
    }
    @Test void everyExactAttackTupleDifferenceIsUnsupportedNotGenericTypeBasedDamage() throws Exception {
        for(String field:List.of("skill_level","skill_number","mp_cost","reuse_millis","hp_cost","item_id","item_count",
                "spell_type","action_id","castgfx","effect_skill_id","buff_duration","target_to","damage_value","damage_dice",
                "damage_dice_count","probability_value","probability_dice","attr","lawful","maximum_range","area","through_flag","castgfx2")) {
            var row=lightArrow();row.put(field,((Number)row.get(field)).longValue()+1);
            var snapshot=read(new Jdbc(List.of(row)));assertTrue(snapshot.lightArrow().isEmpty(),field);
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,snapshot.skills().get(0).support(),field);
        }
        for(String target:List.of("none","self","Attack")) {
            var row=lightArrow();row.put("target",target);row.put("target_bytes",(long)target.length());
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(row))).skills().get(0).support());
        }
    }
    @Test void attackNullNumericTypeLossAndContradictoryTextLengthsNeverGrantOrDisappear() throws Exception {
        for(String field:List.of("damage_value","damage_dice","damage_dice_count","buff_duration","target_to","probability_value",
                "probability_dice","attr","lawful","maximum_range","area","through_flag","castgfx2"))
            for(Object value:new Object[]{null,new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
                var row=lightArrow();row.put(field,value);reject(List.of(row));
            }
        for(String field:List.of("damage_value","damage_dice","damage_dice_count","buff_duration","target_to","probability_value",
                "probability_dice","attr","area","through_flag","castgfx2")) {
            var row=lightArrow();row.put(field,-1L);reject(List.of(row));
        }
        for(long bytes:new long[]{0,1,5,7,257}) {var row=lightArrow();row.put("target_bytes",bytes);reject(List.of(row));}
        var negativeRange=lightArrow();negativeRange.put("maximum_range",-1L);
        assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(negativeRange))).skills().get(0).support());
    }
    @Test void learnedAttackGrantStillRequiresExactCharacterOwnerAndSourceCatalog() throws Exception {
        try(var config=config()) {
            var jdbc=new Jdbc(List.of(lightArrow()));
            var snapshot=scoped(scope->dao(config,jdbc).skillsCharacter(42,"moon-owner",3333,"moon_catalog")).orElseThrow();
            assertEquals(42,snapshot.sourceId());assertTrue(snapshot.lightArrow().isPresent());
            assertEquals(Map.of(1,3333L,2,"moon-owner"),jdbc.parameters);assertTrue(jdbc.sql.get(1).contains("`moon_catalog`.`skills`"));
        }
        for(String field:List.of("learned_owner","template_id","objid")) {var row=lightArrow();row.put(field,99L);reject(List.of(row));}
        var missing=new Jdbc(List.of(character()));assertTrue(read(missing).lightArrow().isEmpty());
    }
    @Test void attackEffectColumnAbsenceRequiresSameStrictProofAndSameSourceNameFallback() throws Exception {
        var row=lightArrow();row.put("name_token","");row.put("name_token_bytes",0L);
        row.put("catalog_name","光箭");row.put("catalog_name_bytes",6L);
        var jdbc=new Jdbc(List.of(row));jdbc.guardPrepareFailure=true;jdbc.guardFailure=new SQLException("synthetic","42S22",1054);
        assertEquals("光箭",read(jdbc).lightArrow().orElseThrow().nameToken());assertEquals(2,jdbc.sql.size());
        var bad=new Jdbc(List.of(lightArrow()));bad.guardFailure=new SQLException("permission","42000",1142);reject(bad);
    }
    @TempDir Path temporary;
    private ModernServerConfig config() throws Exception {
        String identity=temporary.resolve("listener.p12").toAbsolutePath().toString().replace('\\','/');
        String input="schema=1\nbind.port=0\ntls.pkcs12="+identity+"\ntls.password.env=TLS\nsources=7\n"
                +"source.7.name=Sun\nsource.7.catalog.database=catalog\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\n"
                +"source.7.mysql.database=characters_only\nsource.7.mysql.username.env=USER\nsource.7.mysql.password.env=PASSWORD\n"
                +"source.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        return ModernServerConfig.parse(input.getBytes(StandardCharsets.UTF_8),name->"synthetic"+name);
    }
    private static ModernJdbcBackend dao(ModernServerConfig config,Jdbc jdbc)throws Exception {
        return new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,jdbc);
    }
    @FunctionalInterface private interface Scoped<T>{T run(ModernBackendScope scope)throws Exception;}
    private static <T>T scoped(Scoped<T> action)throws Exception {
        var scope=new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable action){action.run();return true;} public void remove(Runnable action){}
        });
        ModernBackendScope.bind(scope);
        try{return action.run(scope);}finally{scope.finish();ModernBackendScope.unbind();}
    }
    private ModernSkills.Snapshot read(Jdbc jdbc)throws Exception {
        try(var config=config()){return scoped(scope->dao(config,jdbc).skillsCharacter(7,"owner",3333,"catalog")).orElseThrow();}
    }

    @Test void exactOwnerSingleDataStatementQualifiedTeleportAndAllWideFields()throws Exception {
        var row=skill(5);row.put("objid",Long.MAX_VALUE);row.put("learned_owner",Long.MAX_VALUE);row.put("level",Long.MAX_VALUE);
        var wide=skill(6);wide.put("objid",Long.MAX_VALUE);wide.put("learned_owner",Long.MAX_VALUE);wide.put("level",Long.MAX_VALUE);
        wide.put("mp_cost",Long.MAX_VALUE);wide.put("reuse_millis",Long.MAX_VALUE);
        var jdbc=new Jdbc(List.of(row,wide));String account="owner' OR '1'='1";
        try(var config=config()){
            var value=scoped(scope->dao(config,jdbc).skillsCharacter(7,account,Long.MAX_VALUE,"catalog")).orElseThrow();
            assertEquals(Long.MAX_VALUE,value.character().objectId());assertEquals(Long.MAX_VALUE,value.character().level());
            assertTrue(value.teleport().isPresent());assertEquals(Long.MAX_VALUE,value.skills().get(1).mpCost());
            assertEquals(Long.MAX_VALUE,value.skills().get(1).reuseMillis());
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,value.skills().get(1).support());
            assertEquals(Map.of(1,Long.MAX_VALUE,2,account),jdbc.parameters);
            assertEquals(2,jdbc.sql.size());assertEquals(ModernSkillsJdbc.sql("catalog",true),jdbc.sql.get(1));
            assertTrue(jdbc.sql.get(1).contains("WHERE c.objid=? AND c.account_name=? AND c.Banned=0"));
            assertFalse(jdbc.sql.get(1).contains(account));assertEquals(List.of(1,513),jdbc.maxRows);
            assertTrue(jdbc.timeouts.stream().allMatch(n->n>=1&&n<=10));assertTrue(jdbc.readOnly);
            assertEquals(1,jdbc.connections);assertEquals(2,jdbc.statementCloses);assertEquals(2,jdbc.resultCloses);
            assertEquals(1,jdbc.connectionCloses);assertTrue(jdbc.properties.isEmpty());
        }
    }
    @Test void noLearnedRowsAreARealEmptySnapshotButNoOwnedCharacterIsAbsent()throws Exception {
        assertTrue(read(new Jdbc(List.of(character()))).skills().isEmpty());
        try(var config=config()){
            var jdbc=new Jdbc(List.of());assertTrue(scoped(scope->dao(config,jdbc).skillsCharacter(7,"owner",3333,"catalog")).isEmpty());
            assertEquals(2,jdbc.resultCloses);assertEquals(1,jdbc.connectionCloses);
        }
    }
    @Test void reservedLevelZeroAndOutsideNativeBitmapAreExplicitlyNotImported()throws Exception {
        var reserved=skill(24);reserved.put("skill_level",0L);
        var reservedWithLevel=skill(92);var tooHigh=skill(221);var highLevel=skill(222);highLevel.put("skill_level",29L);
        assertTrue(read(new Jdbc(List.of(reserved,reservedWithLevel,tooHigh,highLevel))).skills().isEmpty());
    }
    @Test void complete512RawRowsPassAnd513CannotPublishPartialSkills()throws Exception {
        var rows=new ArrayList<Map<String,Object>>();for(int n=1000;n<1512;n++)rows.add(skill(n));
        assertTrue(read(new Jdbc(rows)).skills().isEmpty());rows.add(skill(1512));reject(rows);
    }
    @Test void repeatedOutOfOrderOrMalformedOwnerAndOrphanRowsFailClosed()throws Exception {
        for(var rows:List.of(List.of(skill(5),skill(5)),List.of(skill(6),skill(5)),List.of(character(),character()),
                List.of(character(),skill(5)),List.of(skill(5),character())))reject(rows);
        for(String field:List.of("template_id","learned_owner")){var row=skill(5);row.put(field,null);reject(List.of(row));}
        for(String field:List.of("objid","learned_owner","template_id")){var row=skill(5);row.put(field,99L);reject(List.of(row));}
        var nullLearned=skill(5);nullLearned.put("learned_id",null);reject(List.of(nullLearned));
        var zero=skill(0);reject(List.of(zero));var negative=skill(-1);reject(List.of(negative));
    }
    @Test void allRequiredTeleportNumbersAndStringsRejectNull()throws Exception {
        for(String column:List.of("objid","character_type","character_sex","level","char_name","char_name_bytes",
                "skill_level","skill_number","name_token","name_token_bytes","mp_cost","reuse_millis","hp_cost",
                "item_id","item_count","target","target_bytes","spell_type","action_id","castgfx","effect_skill_id")){
            var row=skill(5);row.put(column,null);reject(List.of(row));
        }
    }
    @Test void everyChangedTeleportExecutionFieldIsUnqualifiedNotZeroDefaulted()throws Exception {
        for(String column:List.of("skill_level","skill_number","mp_cost","hp_cost","item_id","item_count",
                "reuse_millis","spell_type","action_id","castgfx","effect_skill_id")){
            var row=skill(5);row.put(column,((Number)row.get(column)).longValue()+1);
            var result=read(new Jdbc(List.of(row)));assertTrue(result.teleport().isEmpty(),column);
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,result.skills().get(0).support(),column);
        }
        var row=skill(5);row.put("target","attack");row.put("target_bytes",6L);
        assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(row))).skills().get(0).support());
    }
    @Test void negativeTemplateNumbersAreMalformedNotUnsupported()throws Exception {
        for(String column:List.of("skill_level","skill_number","mp_cost","hp_cost","item_id","item_count",
                "reuse_millis","spell_type","action_id","castgfx","effect_skill_id")){
            var row=skill(5);row.put(column,-1L);reject(List.of(row));
        }
    }
    @Test void exactIntegerBoundaryRejectsFractionOverflowApproximateAndText()throws Exception {
        for(String column:List.of("objid","learned_id","mp_cost","reuse_millis","castgfx"))
            for(Object value:List.of(new BigDecimal("1.25"),new BigDecimal("9223372036854775808"),1.0d,1.0f,"1")){
                var row=skill(5);row.put(column,value);reject(List.of(row));
            }
        var row=skill(5);row.replaceAll((key,value)->value instanceof Number n?new BigDecimal(n.toString()).setScale(3):value);
        assertTrue(read(new Jdbc(List.of(row))).teleport().isPresent());
    }
    @Test void textIsStrictAndBoundedBeforeMaterialization()throws Exception {
        for(String column:List.of("char_name","name_token","target")){
            for(String value:List.of("","\u00a0","x\0y","\ud800","界".repeat(86))){
                var row=skill(5);row.put(column,value);row.put(column+"_bytes",value.getBytes(StandardCharsets.UTF_8).length);reject(List.of(row));
            }
            for(long bytes:new long[]{0,257,Long.MAX_VALUE}){var row=skill(5);row.put(column+"_bytes",bytes);reject(List.of(row));}
        }
        var row=skill(5);row.put("name_token","界".repeat(85));row.put("name_token_bytes",255L);
        assertTrue(read(new Jdbc(List.of(row))).teleport().isPresent());
    }
    @Test void retainedSkill87EmptyNameIdUsesSameTemplateDisplayNameWithoutGrantingItsExecution()throws Exception {
        // db/lito_static.sql skill 87: level11/number6/MP15/reuse8000/nameid=''.
        var stun=emptyName(87,"衝擊之暈");stun.put("skill_level",11L);stun.put("skill_number",6L);
        stun.put("mp_cost",15L);stun.put("reuse_millis",8000L);
        var jdbc=new Jdbc(List.of(skill(5),stun));var result=read(jdbc);
        assertEquals(List.of(5,87),result.skills().stream().map(ModernSkills.Skill::skillId).toList());
        assertTrue(result.teleport().isPresent());assertEquals("$1440",result.skills().get(0).nameToken());
        var imported=result.skills().get(1);assertEquals("衝擊之暈",imported.nameToken());
        assertEquals(15,imported.mpCost());assertEquals(8000,imported.reuseMillis());
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,imported.support());
        assertEquals(2,jdbc.sql.size());assertEquals(2,jdbc.statementCloses);assertEquals(2,jdbc.resultCloses);
        assertTrue(jdbc.sql.get(1).contains("LEFT(s.name,257) AS catalog_name,OCTET_LENGTH(s.name) AS catalog_name_bytes"));
    }
    @Test void fallbackOnlyForActualEmptyTokenWithExactZeroBytesAndNeverForMalformedToken()throws Exception {
        for(String value:new String[]{null," ","\u00a0","x\0y","\ud800","界".repeat(86)}) {
            var row=emptyName(87,"衝擊之暈");row.put("name_token",value);
            row.put("name_token_bytes",value==null?0L:(long)value.getBytes(StandardCharsets.UTF_8).length);reject(List.of(row));
        }
        for(Long bytes:new Long[]{null,-1L,1L,257L}) {
            var row=emptyName(87,"衝擊之暈");row.put("name_token_bytes",bytes);reject(List.of(row));
        }
        for(long bytes:new long[]{0,1,4,6}) {
            var row=emptyName(87,"衝擊之暈");row.put("name_token","$1440");row.put("name_token_bytes",bytes);reject(List.of(row));
        }
        var valid=skill(5);valid.put("catalog_name","\0");valid.put("catalog_name_bytes",1L);
        assertEquals("$1440",read(new Jdbc(List.of(valid))).teleport().orElseThrow().nameToken());
    }
    @Test void fallbackDisplayNameRemainsRequiredExactUtf8AndBounded()throws Exception {
        for(String name:new String[]{null,""," ","\u00a0","x\0y","\ud800","界".repeat(86)})
            reject(List.of(emptyName(87,name)));
        for(Long bytes:new Long[]{null,-1L,0L,1L,13L,257L,Long.MAX_VALUE}) {
            var row=emptyName(87,"衝擊之暈");row.put("catalog_name_bytes",bytes);reject(List.of(row));
        }
        var boundary=emptyName(87,"界".repeat(85)+"x");
        assertEquals(256,read(new Jdbc(List.of(boundary))).skills().get(0).nameToken().getBytes(StandardCharsets.UTF_8).length);
    }
    @Test void teleportEmptyTokenDisplayNameDoesNotAlterItsExecutionTupleQualification()throws Exception {
        var good=emptyName(5,"指定傳送");
        assertEquals("指定傳送",read(new Jdbc(List.of(good))).teleport().orElseThrow().nameToken());
        for(String field:List.of("skill_level","skill_number","mp_cost","hp_cost","item_id","item_count",
                "reuse_millis","spell_type","action_id","castgfx","effect_skill_id")) {
            var changed=emptyName(5,"指定傳送");changed.put(field,((Number)changed.get(field)).longValue()+1);
            var result=read(new Jdbc(List.of(changed)));assertTrue(result.teleport().isEmpty(),field);
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,result.skills().get(0).support(),field);
        }
        var target=emptyName(5,"指定傳送");target.put("target","attack");target.put("target_bytes",6L);
        assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(target))).skills().get(0).support());
    }
    @Test void emptyTokenDisplayNamesAreBoundToTheAuthorizedSourceCatalogNotPrimary()throws Exception {
        try(var config=config()) {
            var sun=new Jdbc(List.of(emptyName(87,"Sun template")));var moon=new Jdbc(List.of(emptyName(87,"Moon template")));
            var principal=new ModernAccountAuthentication.Principal("sun-owner",Set.of(
                    new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),
                    Map.of(7,"sun-owner",42,"moon-owner"));
            var repo=ModernSkillsRepository.jdbc(Map.of(7,new ModernSkillsRepository.Source(dao(config,sun),"sun_catalog"),
                    42,new ModernSkillsRepository.Source(dao(config,moon),"moon_catalog")));
            assertEquals("Sun template",scoped(scope->repo.character(principal,7,3333)).orElseThrow().skills().get(0).nameToken());
            assertEquals("Moon template",scoped(scope->repo.character(principal,42,3333)).orElseThrow().skills().get(0).nameToken());
            assertEquals("sun-owner",sun.parameters.get(2));assertEquals("moon-owner",moon.parameters.get(2));
            assertTrue(sun.sql.get(1).contains("`sun_catalog`.`skills`"));assertTrue(moon.sql.get(1).contains("`moon_catalog`.`skills`"));
            assertEquals(2,sun.sql.size());assertEquals(2,moon.sql.size());
        }
    }
    @Test void changingAnyCharacterFieldDuringJoinRejectsWholeSnapshot()throws Exception {
        for(String column:List.of("char_name","character_type","character_sex","level")){
            var second=skill(6);second.put(column,column.equals("char_name")?"other":column.equals("character_sex")?1L:2L);
            reject(List.of(skill(5),second));
        }
    }
    @Test void absentEffectColumnRequiresExactQueryErrorAndCleanRetirement()throws Exception {
        for(boolean prepare:new boolean[]{true,false}){
            var jdbc=new Jdbc(List.of(skill(5)));jdbc.guardFailure=new SQLException("synthetic", "42S22",1054);jdbc.guardPrepareFailure=prepare;
            assertTrue(read(jdbc).teleport().isPresent());assertEquals(ModernSkillsJdbc.sql("catalog",false),jdbc.sql.get(1));
            assertEquals(prepare?1:2,jdbc.statementCloses);assertEquals(1,jdbc.resultCloses);
        }
    }
    @Test void missingTablePermissionsWrongStateAndChainedErrorsNeverImplyAbsentColumn()throws Exception {
        var chained=new SQLException("synthetic","42S22",1054);chained.setNextException(new SQLException("other"));
        var suppressed=new SQLException("synthetic","42S22",1054);suppressed.addSuppressed(new SQLException("cleanup"));
        for(var error:List.of(new SQLException("permission","42000",1142),new SQLException("table","42S02",1146),
                new SQLException("wrong state","42000",1054),new SQLException("wrong vendor","42S22",1142),chained,suppressed)){
            var jdbc=new Jdbc(List.of(skill(5)));jdbc.guardFailure=error;reject(jdbc);assertEquals(1,jdbc.sql.size());
        }
    }
    @Test void setupOrCleanupUnknownColumnErrorsNeverImplyAbsentColumn()throws Exception {
        for(String phase:List.of("setup","statement","result")){
            var jdbc=new Jdbc(List.of(skill(5)));
            if(phase.equals("setup"))jdbc.guardSetupFailure=true;
            else if(phase.equals("statement"))jdbc.guardStatementCloseFailure=true;
            else jdbc.guardResultCloseFailure=true;
            reject(jdbc);assertEquals(1,jdbc.sql.size());
        }
        var jdbc=new Jdbc(List.of(skill(5)));jdbc.guardFailure=new SQLException("unknown","42S22",1054);
        jdbc.guardStatementCloseFailure=true;reject(jdbc);assertEquals(1,jdbc.sql.size());
    }
    @Test void limitZeroGuardCannotReturnRowsOrSwallowCancellation()throws Exception {
        var unexpected=new Jdbc(List.of(skill(5)));unexpected.guardRows=true;reject(unexpected);
        var jdbc=new Jdbc(List.of(skill(5)));jdbc.cancelGuard=true;
        assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));assertEquals(1,jdbc.sql.size());
    }
    @Test void cancelBeforeConnectOrDuringFinalNextClosesWithoutReturningPartialSnapshot()throws Exception {
        try(var config=config()){
            var before=new Jdbc(List.of(skill(5)));
            assertThrows(ModernBackendExecutor.BackendException.class,()->scoped(scope->{
                scope.cancel(ModernBackendExecutor.Failure.CANCELLED);return dao(config,before).skillsCharacter(7,"owner",3333,"catalog");}));
            assertEquals(0,before.connections);
        }
        for(int next:new int[]{1,2}){
            var jdbc=new Jdbc(List.of(skill(5)));jdbc.cancelDataNext=next;
            assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(2,jdbc.resultCloses);assertEquals(2,jdbc.statementCloses);assertEquals(1,jdbc.connectionCloses);
        }
    }
    @Test void badCatalogIdentifierRejectsBeforeConnectAndNoFailureLeaksDriverMessage()throws Exception {
        try(var config=config()){
            for(String catalog:new String[]{null,"","schema`.`skills","schema;DROP TABLE x","a".repeat(65)}){
                var jdbc=new Jdbc(List.of());assertThrows(IllegalArgumentException.class,
                        ()->scoped(scope->dao(config,jdbc).skillsCharacter(7,"owner",3333,catalog)));assertEquals(0,jdbc.connections);
            }
        }
        var jdbc=new Jdbc(List.of(skill(5)));jdbc.dataFailure=new SQLException("secret SQL owner host",new Exception("secret"));
        var error=assertThrows(SQLException.class,()->read(jdbc));assertEquals("modern JDBC backend unavailable",error.getMessage());
        assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
    }
    @Test void sourceRepositoryKeepsCanonicalAccountsAndSameObjectSeparatedWithoutFallback()throws Exception {
        try(var config=config()){
            var sun=new Jdbc(List.of(skill(5)));var moonRow=skill(6);moonRow.put("char_name","Moon");moonRow.put("char_name_bytes",4L);
            var moon=new Jdbc(List.of(moonRow));var principal=new ModernAccountAuthentication.Principal("sun-owner",Set.of(
                    new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),Map.of(7,"sun-owner",42,"moon-owner"));
            var sources=new HashMap<Integer,ModernSkillsRepository.Source>();sources.put(7,new ModernSkillsRepository.Source(dao(config,sun),"sun_catalog"));
            sources.put(42,new ModernSkillsRepository.Source(dao(config,moon),"moon_catalog"));var repo=ModernSkillsRepository.jdbc(sources);sources.clear();
            var first=scoped(scope->repo.character(principal,7,3333)).orElseThrow();var second=scoped(scope->repo.character(principal,42,3333)).orElseThrow();
            assertTrue(first.teleport().isPresent());assertTrue(second.teleport().isEmpty());assertEquals(7,first.sourceId());assertEquals(42,second.sourceId());
            assertEquals("sun-owner",sun.parameters.get(2));assertEquals("moon-owner",moon.parameters.get(2));
            assertTrue(sun.sql.get(1).contains("`sun_catalog`.`skills`"));assertTrue(moon.sql.get(1).contains("`moon_catalog`.`skills`"));
            assertThrows(SecurityException.class,()->scoped(scope->repo.character(principal,9,3333)));
            var missing=ModernSkillsRepository.jdbc(Map.of(7,new ModernSkillsRepository.Source(dao(config,sun),"sun_catalog")));
            assertThrows(IllegalStateException.class,()->scoped(scope->missing.character(principal,42,3333)));
            assertEquals(1,sun.connections);assertEquals(1,moon.connections);
        }
    }
    @Test void unavailableAndMalformedSourceMapAreExplicitErrors() {
        assertThrows(IllegalArgumentException.class,()->ModernSkillsRepository.jdbc(Map.of()));
        assertThrows(IllegalStateException.class,()->ModernSkillsRepository.unavailable().character(null,7,3333));
    }

    private void reject(List<Map<String,Object>> rows)throws Exception{reject(new Jdbc(rows));}
    private void reject(Jdbc jdbc)throws Exception{
        assertThrows(SQLException.class,()->read(jdbc));assertEquals(1,jdbc.connectionCloses);
    }
    private static Map<String,Object> character(){
        var row=new HashMap<String,Object>();row.put("objid",3333L);row.put("char_name","Sun");row.put("char_name_bytes",3L);
        row.put("character_type",1L);row.put("character_sex",0L);row.put("level",80L);return row;
    }
    private static Map<String,Object> skill(long id){
        var row=character();row.put("learned_owner",3333L);row.put("learned_id",id);row.put("template_id",id);row.put("skill_level",1L);
        row.put("skill_number",4L);row.put("name_token","$1440");row.put("name_token_bytes",5L);row.put("mp_cost",10L);row.put("reuse_millis",10L);
        row.put("hp_cost",0L);row.put("item_id",0L);row.put("item_count",0L);row.put("target","none");row.put("target_bytes",4L);
        row.put("spell_type",128L);row.put("action_id",19L);row.put("castgfx",169L);row.put("effect_skill_id",0L);return row;
    }
    private static Map<String,Object> lightArrow() {
        // db/lito_static.sql:46189, native LIGHT_ARROW in BattleSpellCatalog.
        var row=skill(4);row.put("skill_number",3L);row.put("name_token","$1439");row.put("mp_cost",3L);
        row.put("target","attack");row.put("target_bytes",6L);row.put("spell_type",64L);row.put("action_id",18L);row.put("castgfx",167L);
        row.put("buff_duration",0L);row.put("target_to",3L);row.put("damage_value",6L);row.put("damage_dice",10L);row.put("damage_dice_count",1L);
        row.put("probability_value",0L);row.put("probability_dice",0L);row.put("attr",16L);row.put("lawful",0L);row.put("maximum_range",10L);
        row.put("area",0L);row.put("through_flag",0L);row.put("castgfx2",0L);return row;
    }
    private static Map<String,Object> emptyName(long id,String name) {
        var row=skill(id);row.put("name_token","");row.put("name_token_bytes",0L);row.put("catalog_name",name);
        row.put("catalog_name_bytes",name==null?null:(long)name.getBytes(StandardCharsets.UTF_8).length);return row;
    }
    private static final class Jdbc implements ModernJdbcBackend.Connector {
        final List<Map<String,Object>> rows;final List<String> sql=new ArrayList<>();final List<Integer> maxRows=new ArrayList<>(),timeouts=new ArrayList<>();
        final Map<Integer,Object> parameters=new HashMap<>();int connections,statementCloses,resultCloses,connectionCloses,cancelDataNext;
        boolean readOnly,guardPrepareFailure,guardSetupFailure,guardStatementCloseFailure,guardResultCloseFailure,guardRows,cancelGuard;
        SQLException guardFailure,dataFailure;Properties properties;
        List<Map<String,Object>> extraRows=List.of(Map.of("extension_kind",0L,"ids_null",1L,"counts_null",1L));
        Jdbc(List<Map<String,Object>> rows){this.rows=List.copyOf(rows);}
        public Connection connect(String url,Properties properties){connections++;this.properties=properties;
            return proxy(Connection.class,(method,args)->switch(method){
                case "setReadOnly"->{readOnly=(boolean)args[0];yield null;}
                case "setNetworkTimeout","abort"->null;
                case "close"->{connectionCloses++;yield null;}
                case "prepareStatement"->{String query=(String)args[0];sql.add(query);boolean guard=query.endsWith(" LIMIT 0");
                    if(guard&&guardPrepareFailure)throw guardFailure;yield statement(guard,query.contains(" AS extension_kind")?extraRows:rows);}
                default->throw new AssertionError("unexpected connection operation "+method);
            });
        }
        private PreparedStatement statement(boolean guard,List<Map<String,Object>> selectedRows){return proxy(PreparedStatement.class,(method,args)->switch(method){
            case "setMaxRows"->{maxRows.add((int)args[0]);if(guard&&guardSetupFailure)throw new SQLException("setup","42S22",1054);yield null;}
            case "setQueryTimeout"->{timeouts.add((int)args[0]);yield null;}
            case "setLong","setString"->{parameters.put((int)args[0],args[1]);yield null;}
            case "executeQuery"->{if(guard&&guardFailure!=null)throw guardFailure;if(!guard&&dataFailure!=null)throw dataFailure;yield result(guard,selectedRows);}
            case "close"->{statementCloses++;if(guard&&guardStatementCloseFailure)throw new SQLException("close","42S22",1054);yield null;}
            default->throw new AssertionError("unexpected statement operation "+method);
        });}
        private ResultSet result(boolean guard,List<Map<String,Object>> rows){
            int[] index={-1};boolean[] wasNull={false};var columns=new ModernJdbcNumbersTest.Columns(column->rows.get(index[0]).get(column),Map.of());
            return proxy(ResultSet.class,(method,args)->switch(method){
                case "next"->{index[0]++;if(guard&&cancelGuard||!guard&&cancelDataNext==index[0]+1)
                    ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
                    yield guard?guardRows&&index[0]==0:index[0]<rows.size();}
                case "findColumn"->columns.find((String)args[0]);case "getMetaData"->columns.metadata();case "wasNull"->wasNull[0];
                case "getString","getBigDecimal"->{Object value=rows.get(index[0]).get(columns.name(args[0]));wasNull[0]=value==null;
                    yield method.equals("getString")?value:ModernJdbcNumbersTest.Columns.decimal(value);}
                case "close"->{resultCloses++;if(guard&&guardResultCloseFailure)throw new SQLException("close","42S22",1054);yield null;}
                default->throw new AssertionError("unexpected result operation "+method);
            });
        }
    }
    @FunctionalInterface private interface Invocation{Object call(String method,Object[] args)throws Throwable;}
    private static <T>T proxy(Class<T> type,Invocation call){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (instance,method,args)->call.call(method.getName(),args)));}
}
