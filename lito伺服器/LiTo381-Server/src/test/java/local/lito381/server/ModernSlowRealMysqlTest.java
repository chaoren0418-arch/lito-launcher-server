package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Fresh owned database only. Qualification is not a usable skill or a TLS cast. */
class ModernSlowRealMysqlTest {
    static final String FIRST=ModernGreaterHasteRealMysqlTest.FIRST;
    static final String SECOND=ModernGreaterHasteRealMysqlTest.SECOND;
    record Read(ModernSlow.Profile profile,ModernSlowCatalog.Snapshot catalog,long hasteItems) { }

    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void selectedColumnPolicyAccountsWideStatsAndRevocationUseOnlyOwnedSelectSources() throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);
        var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null,"explicit owned MySQL/shared root required");
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls,false,false,true);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            ModernGreaterHasteRealMysqlTest.install(f); install(f);
            var sun=new ModernJdbcBackend(f.config.sources().get(0).database(),1000,5000);
            var moon=new ModernJdbcBackend(f.config.sources().get(1).database(),1000,5000);
            var daos=Map.of(7,sun,42,moon);
            var repo=ModernSlowRepository.jdbc(Map.of(7,new ModernSlowRepository.Source(sun,FIRST),42,new ModernSlowRepository.Source(moon,SECOND)));
            var verifier=ModernAccountAuthentication.repositories(Map.of(
                    7,ModernAccountAuthentication.SourceBinding.direct("太陽神",sun::authenticate),
                    42,ModernAccountAuthentication.SourceBinding.direct("月亮神",moon::authenticate)),account->false);
            var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,
                    ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
            var first=read(gate,repo,principal,daos,7);var second=read(gate,repo,principal,daos,42);
            assertTrue(first.catalog.accepts(first.profile));assertEquals(18,first.profile.mpCost());
            assertEquals(0,first.hasteItems);assertEquals(0,second.hasteItems);
            assertFalse(first.catalog.gmTarget());assertFalse(second.catalog.gmTarget());
            for(long access:new long[]{199,200,4294967296L,Long.MIN_VALUE}) {
                admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET AccessLevel="+access);
                assertEquals(access>=200,read(gate,repo,principal,daos,7).catalog.gmTarget());
                assertFalse(read(gate,repo,principal,daos,42).catalog.gmTarget());
            }
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET AccessLevel=NULL");
            assertEquals(ModernBackendExecutor.Failure.BACKEND,assertThrows(ModernBackendExecutor.BackendException.class,
                    ()->read(gate,repo,principal,daos,7)).failure());
            admin(f,"ALTER TABLE `"+ModernQuestSqlFixture.source(7)+"`.`characters` CHANGE AccessLevel hidden_access BIGINT NULL DEFAULT 0");
            assertEquals(ModernBackendExecutor.Failure.BACKEND,assertThrows(ModernBackendExecutor.BackendException.class,
                    ()->read(gate,repo,principal,daos,7)).failure());
            admin(f,"ALTER TABLE `"+ModernQuestSqlFixture.source(7)+"`.`characters` CHANGE hidden_access AccessLevel BIGINT NULL DEFAULT 0");
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET AccessLevel=0");
            // Both accounts explicitly use FIRST for equipment; SECOND is the
            // second learned-skill catalog only. Never pretend the fixture has
            // a second full equipment DB or infer it from skill catalog choice.
            admin(f,"UPDATE `"+FIRST+"`.`armor` SET haste_item=4294967296 WHERE item_id=20023");
            assertEquals(1,read(gate,repo,principal,daos,7).hasteItems);
            assertEquals(0,read(gate,repo,principal,daos,42).hasteItems);
            admin(f,"UPDATE `"+FIRST+"`.`weapon` SET haste_item=-9223372036854775808");
            assertEquals(2,read(gate,repo,principal,daos,7).hasteItems);
            assertEquals(1,read(gate,repo,principal,daos,42).hasteItems);
            assertEquals(0,first.hasteItems);
            admin(f,"UPDATE `"+FIRST+"`.`weapon` SET haste_item=0");
            for(String table:List.of("weapon","armor")) {
                admin(f,"UPDATE `"+FIRST+"`.`"+table+"` SET haste_item=NULL");
                var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(gate,repo,principal,daos,7));
                assertEquals(ModernBackendExecutor.Failure.BACKEND,failure.failure());assertNull(failure.getCause());
                admin(f,"UPDATE `"+FIRST+"`.`"+table+"` SET haste_item=0");
            }
            admin(f,"ALTER TABLE `"+FIRST+"`.`armor` CHANGE haste_item hidden_haste_item BIGINT NULL DEFAULT 0");
            assertEquals(ModernBackendExecutor.Failure.BACKEND,assertThrows(ModernBackendExecutor.BackendException.class,
                    ()->read(gate,repo,principal,daos,7)).failure());
            // Old features do not acquire a new SQL schema dependency.
            assertTrue(gate.call(10000,scope->sun.physicalCharacter(7,principal.accountForSource(7),3333,FIRST,true)).isPresent());
            admin(f,"ALTER TABLE `"+FIRST+"`.`armor` CHANGE hidden_haste_item haste_item BIGINT NULL DEFAULT 0");
            admin(f,"UPDATE `"+FIRST+"`.`armor` SET haste_item=2 WHERE item_id=20023");
            assertEquals(ModernSlowCatalog.Status.UNLEARNED,second.catalog.status());
            assertFalse(first.catalog.accepts(second.profile));
            assertEquals("fixture-user",principal.accountForSource(7));
            assertEquals("FIXTURE-USER",principal.accountForSource(42));
            assertTrue(gate.call(10000,scope->moon.slowCharacter(42,"fixture-user",3333,SECOND)).isEmpty());
            assertTrue(gate.call(10000,scope->repo.character(principal,7,3334)).isEmpty());
            assertThrows(SecurityException.class,()->repo.character(principal,99,3333));
            admin(f,"INSERT INTO `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` VALUES(3333,29)");
            var learned=read(gate,repo,principal,daos,42);
            assertTrue(learned.catalog.accepts(learned.profile));
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET ranged=8 WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,read(gate,repo,principal,daos,42).catalog.status());
            assertEquals(ModernSlowCatalog.Status.QUALIFIED,read(gate,repo,principal,daos,7).catalog.status());
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET ranged=-1 WHERE skill_id=29");
            admin(f,"DELETE FROM `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.UNLEARNED,read(gate,repo,principal,daos,42).catalog.status());
            assertEquals(ModernSlowCatalog.Status.QUALIFIED,learned.catalog.status());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Intel=5000000000");
            assertEquals(15,read(gate,repo,principal,daos,7).profile.mpCost());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET is_equipped=0 WHERE id=1003");
            assertEquals(15,read(gate,repo,principal,daos,7).profile.mpCost());
            assertEquals(0,read(gate,repo,principal,daos,7).hasteItems);
            assertEquals(18,first.profile.mpCost());
            for(String change:List.of("itemConsumeId=0","itemConsumeCount=0","probability_dice=0","ranged=8","sysmsgID_happen=0","sysmsgID_stop=277","reuseDelay=1000")) {
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET "+change+" WHERE skill_id=29");
                assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET itemConsumeId=40318,itemConsumeCount=1,probability_dice=12,ranged=-1,sysmsgID_happen=277,sysmsgID_stop=0,reuseDelay=500 WHERE skill_id=29");
            }
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面技能控制` SET bonus_int=0.30000000000000004 WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills_負面技能控制` MODIFY bonus_int DECIMAL(30,25)");
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面技能控制` SET bonus_int=0.3000000000000000000000001 WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面技能控制` SET bonus_int=0.3 WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.QUALIFIED,catalog(sun).status());
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面技能控制` SET bonus_int=NULL WHERE skill_id=29");rejected(sun);
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面技能控制` SET bonus_int=0.3 WHERE skill_id=29");
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills_負面技能控制` MODIFY bonus_int VARCHAR(32)");rejected(sun);
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills_負面技能控制` MODIFY bonus_int DOUBLE NOT NULL");
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面boss免疫` SET modifier=0 WHERE skill_id=29");
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"UPDATE `"+FIRST+"`.`skills_負面boss免疫` SET modifier=5 WHERE skill_id=29");
            admin(f,"INSERT INTO `"+FIRST+"`.`skills_魔屏無效技能` VALUES(29)");
            assertEquals(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"DELETE FROM `"+FIRST+"`.`skills_魔屏無效技能` WHERE skill_id=29");
            admin(f,"RENAME TABLE `"+FIRST+"`.`skills_負面boss免疫` TO `"+FIRST+"`.`slow_boss_hidden`");rejected(sun);
            admin(f,"RENAME TABLE `"+FIRST+"`.`slow_boss_hidden` TO `"+FIRST+"`.`skills_負面boss免疫`");
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=1");
            assertTrue(ModernEquipmentSourceJdbcTest.scoped(()->sun.slowCharacter(7,"fixture-user",3333,FIRST)).isEmpty());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=0");
            assertEquals(ModernSlowCatalog.Status.QUALIFIED,catalog(sun).status());
            var skills=gate.call(10000,scope->sun.skillsCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,skills.skills().stream().filter(s->s.skillId()==29).findFirst().orElseThrow().support());
            var connections=new ModernSettlementConnections(f.config.sources().get(0).database(),1000,3000,1);
            try {
                try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5))) {
                    c.setReadOnly(false);
                    try(var statement=c.prepareStatement("UPDATE `"+FIRST+"`.`skills` SET mpConsume=1 WHERE skill_id=29")) {
                        statement.setQueryTimeout(2);assertEquals(1142,assertThrows(SQLException.class,statement::executeUpdate).getErrorCode());
                    }
                }
            } finally {connections.close();assertTrue(connections.awaitTermination(5000));assertTrue(connections.isQuiescent());}
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertTrue(executor.isQuiescent());
            assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
            System.err.println("SLOW29_SQL: source-qualified cost/catalog and equipment haste, exact signed i64 flags and missing/NULL rejection, actual range/messages, exact coefficients, INT5000000000, revoke/reload and read-only grants; no cast or material consumption enabled");
        }
    }

    static void install(ModernQuestSqlFixture f) throws Exception {
        for(int source:List.of(7,42))admin(f,"ALTER TABLE `"+ModernQuestSqlFixture.source(source)+"`.`characters` ADD AccessLevel BIGINT NULL DEFAULT 0");
        for(String table:List.of("weapon","armor"))
            admin(f,"ALTER TABLE `"+FIRST+"`.`"+table+"` ADD haste_item BIGINT NULL DEFAULT 0");
        for(String db:List.of(FIRST,SECOND)) {
            admin(f,"INSERT INTO `"+db+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,itemConsumeId,itemConsumeCount,reuseDelay,buffDuration,target_to,probability_dice,attr,type,ranged,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail) VALUES(29,'$1455','緩速術','buff',4,4,20,40318,1,500,64,3,12,8,1,-1,16,19,752,277,0,280)");
            admin(f,"INSERT INTO `"+db+"`.`skills_item` VALUES(29,NULL,NULL)");
            admin(f,"ALTER TABLE `"+db+"`.`skills_負面技能控制` ADD prob_high BIGINT NOT NULL,ADD prob_equal BIGINT NOT NULL,ADD prob_low BIGINT NOT NULL,ADD bonus_str DOUBLE NOT NULL,ADD bonus_dex DOUBLE NOT NULL,ADD bonus_int DOUBLE NOT NULL,ADD check_resist_type BIGINT NOT NULL,ADD check_magic_level TINYINT(1) NOT NULL,ADD check_magic_hit TINYINT(1) NOT NULL,ADD mr_multiplier DOUBLE NOT NULL,ADD con_multiplier DOUBLE NOT NULL,ADD wis_multiplier DOUBLE NOT NULL");
            admin(f,"INSERT INTO `"+db+"`.`skills_負面技能控制` VALUES(29,55,45,35,0,0,0.3,0,0,1,0.05,0,0)");
            admin(f,"CREATE TABLE `"+db+"`.`skills_負面boss免疫` (skill_id BIGINT PRIMARY KEY,modifier BIGINT NOT NULL)");
            admin(f,"INSERT INTO `"+db+"`.`skills_負面boss免疫` VALUES(29,5)");
            admin(f,"CREATE TABLE `"+db+"`.`skills_魔屏無效技能` (skill_id BIGINT PRIMARY KEY)");
            for(String table:List.of("skills_負面boss免疫","skills_魔屏無效技能"))
                admin(f,"GRANT SELECT ON `"+db+"`.`"+table+"` TO '"+ModernQuestSqlFixture.READER+"'@'127.0.0.1'");
        }
        admin(f,"INSERT INTO `"+ModernQuestSqlFixture.source(7)+"`.`character_skills` VALUES(3333,29)");
    }
    private static Read read(ModernBackendExecutor.SessionGate gate,ModernSlowRepository repo,
            ModernAccountAuthentication.Principal principal,Map<Integer,ModernJdbcBackend> daos,int id) throws Exception {
        return gate.call(10000,scope->{
            var source=daos.get(id).physicalCharacter(id,principal.accountForSource(id),3333,FIRST,true,false,false,false,true).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),System.currentTimeMillis());
            assertEquals(fighter.hasteItemCount().orElseThrow()>0,ModernSlow.equipmentImmune(fighter));
            return new Read(ModernSlow.qualify(fighter),repo.character(principal,id,3333).orElseThrow(),fighter.hasteItemCount().orElseThrow());
        });
    }
    private static ModernSlowCatalog.Snapshot catalog(ModernJdbcBackend dao) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->dao.slowCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
    }
    private static void rejected(ModernJdbcBackend dao) {
        var error=assertThrows(SQLException.class,()->catalog(dao));
        assertEquals("modern JDBC backend unavailable",error.getMessage());assertNull(error.getCause());
        assertNull(error.getNextException());assertEquals(0,error.getSuppressed().length);
    }
    private static void admin(ModernQuestSqlFixture f,String sql) throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement(sql)) {
            s.setQueryTimeout(3);s.execute();
        }
    }
}
