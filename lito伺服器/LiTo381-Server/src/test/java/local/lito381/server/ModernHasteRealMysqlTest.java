package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Owned, fresh MySQL only. Skill43 source facts, not a TLS cast or UI test. */
class ModernHasteRealMysqlTest {
    static final String FIRST=ModernGreaterHasteRealMysqlTest.FIRST;
    static final String SECOND=ModernGreaterHasteRealMysqlTest.SECOND;
    record Read(ModernHaste.Profile profile,ModernHasteCatalog.Snapshot learned) { }

    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void realSourceAccountsReloadLearnedStateEquipmentAndWideIntWithoutEnablingCast()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);
        var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null,"explicit isolated MySQL and shared root required");
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls,false,false,true);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            // Reuse the owned SQL/schema seed, not skill54's qualifier or a
            // production source. Both independent skills must remain distinct.
            ModernGreaterHasteRealMysqlTest.install(f);
            install(f);
            var sun=new ModernJdbcBackend(f.config.sources().get(0).database(),1000,5000);
            var moon=new ModernJdbcBackend(f.config.sources().get(1).database(),1000,5000);
            var daos=Map.of(7,sun,42,moon);
            var repo=ModernHasteRepository.jdbc(Map.of(7,new ModernHasteRepository.Source(sun,FIRST),42,new ModernHasteRepository.Source(moon,SECOND)));
            var verifier=ModernAccountAuthentication.repositories(Map.of(
                    7,ModernAccountAuthentication.SourceBinding.direct("太陽神",sun::authenticate),
                    42,ModernAccountAuthentication.SourceBinding.direct("月亮神",moon::authenticate)),account->false);
            var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,
                    ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
            var first=read(gate,repo,principal,daos,7);var second=read(gate,repo,principal,daos,42);
            assertTrue(first.learned.accepts(first.profile));assertEquals(18,first.profile.mpCost());
            assertEquals(ModernHasteCatalog.Status.UNLEARNED,second.learned.status());
            assertFalse(first.learned.accepts(second.profile));
            assertEquals("fixture-user",principal.accountForSource(7));assertEquals("FIXTURE-USER",principal.accountForSource(42));
            assertTrue(gate.call(10000,scope->moon.hasteCharacter(42,"fixture-user",3333,SECOND)).isEmpty());
            assertTrue(gate.call(10000,scope->repo.character(principal,7,3334)).isEmpty());
            assertThrows(SecurityException.class,()->repo.character(principal,99,3333));
            admin(f,"INSERT INTO `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` VALUES(3333,43)");
            var learned=read(gate,repo,principal,daos,42);
            assertTrue(learned.learned.accepts(learned.profile));assertEquals(40,learned.profile.mpCost());
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET buffDuration=2400 WHERE skill_id=43");
            assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE,read(gate,repo,principal,daos,42).learned.status());
            assertEquals(ModernHasteCatalog.Status.QUALIFIED,read(gate,repo,principal,daos,7).learned.status());
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET buffDuration=1200 WHERE skill_id=43");
            admin(f,"DELETE FROM `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` WHERE skill_id=43");
            assertEquals(ModernHasteCatalog.Status.UNLEARNED,read(gate,repo,principal,daos,42).learned.status());
            assertEquals(ModernHasteCatalog.Status.QUALIFIED,learned.learned.status());
            assertEquals(ModernHasteCatalog.Status.UNLEARNED,second.learned.status());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET is_equipped=0 WHERE id=1003");
            assertEquals(38,read(gate,repo,principal,daos,7).profile.mpCost());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Intel=5000000000");
            assertEquals(33,read(gate,repo,principal,daos,7).profile.mpCost());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET is_equipped=1 WHERE id=1003");
            assertEquals(15,read(gate,repo,principal,daos,7).profile.mpCost());
            assertEquals(18,first.profile.mpCost(),"old read must remain immutable");
            // Wind Helm affects54 only; do not share its current-equipment test.
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET item_id=20023 WHERE id=1003");
            assertEquals(33,read(gate,repo,principal,daos,7).profile.mpCost());
            var skill54=gate.call(10000,scope->sun.greaterHasteCharacter(7,principal.accountForSource(7),3333,FIRST)).orElseThrow();
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,skill54.status());
            for(String change:List.of("mpConsume=39","effect_skill_id=1","target='none'","target_to=0","castgfx=3104")) {
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET "+change+" WHERE skill_id=43");
                assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume=40,effect_skill_id=0,target='buff',target_to=3,castgfx=755 WHERE skill_id=43");
            }
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume DECIMAL(21,1)");
            for(String value:List.of("NULL","40.5","9223372036854775808")) {
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume="+value+" WHERE skill_id=43");rejected(sun);
            }
            admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume=40 WHERE skill_id=43");
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume VARCHAR(32)");rejected(sun);
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume DOUBLE");rejected(sun);
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume BIGINT");
            admin(f,"UPDATE `"+FIRST+"`.`skills_item` SET itemids='40010' WHERE skill_id=43");
            assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"UPDATE `"+FIRST+"`.`skills_item` SET itemids=NULL WHERE skill_id=43");
            admin(f,"INSERT INTO `"+FIRST+"`.`skills_buff_ability` VALUES(43)");
            assertEquals(ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"DELETE FROM `"+FIRST+"`.`skills_buff_ability` WHERE skill_id=43");
            admin(f,"RENAME TABLE `"+FIRST+"`.`skills_item` TO `"+FIRST+"`.`skills_item_hidden`");rejected(sun);
            admin(f,"RENAME TABLE `"+FIRST+"`.`skills_item_hidden` TO `"+FIRST+"`.`skills_item`");
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=1");
            assertTrue(ModernEquipmentSourceJdbcTest.scoped(()->sun.hasteCharacter(7,"fixture-user",3333,FIRST)).isEmpty());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=0");
            assertEquals(ModernHasteCatalog.Status.QUALIFIED,catalog(sun).status());
            var skills=gate.call(10000,scope->sun.skillsCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,skills.skills().stream().filter(s->s.skillId()==43).findFirst().orElseThrow().support());
            var connections=new ModernSettlementConnections(f.config.sources().get(0).database(),1000,3000,1);
            try {
                try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5))) {
                    c.setReadOnly(false);
                    try(var s=c.prepareStatement("UPDATE `"+FIRST+"`.`skills` SET mpConsume=1 WHERE skill_id=43")) {
                        s.setQueryTimeout(2);assertEquals(1142,assertThrows(SQLException.class,s::executeUpdate).getErrorCode());
                    }
                }
            } finally {connections.close();assertTrue(connections.awaitTermination(5000));assertTrue(connections.isQuiescent());}
            assertEquals(ModernHasteCatalog.Status.QUALIFIED,catalog(sun).status());
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertTrue(executor.isQuiescent());
            assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
            System.err.println("HASTE43_SQL: separate source/account/catalog, learn/revoke, equipment reload, INT5000000000, template rejection and SELECT-only grants verified; no cast capability enabled");
        }
    }
    /** Skill43 rows in a freshly owned fixture whose common/GH schema was seeded. */
    static void install(ModernQuestSqlFixture f)throws Exception {
        for(String catalog:List.of(FIRST,SECOND)) {
            admin(f,"INSERT INTO `"+catalog+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,buffDuration,target_to,attr,type,ranged,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail) VALUES(43,'$1463','加速術','buff',6,2,40,10,1200,3,8,2,-1,4,19,755,184,185,280)");
            admin(f,"INSERT INTO `"+catalog+"`.`skills_item` VALUES(43,NULL,NULL)");
        }
        admin(f,"INSERT INTO `"+FIRST+"`.`armor`(item_id,name_id,type,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist) VALUES(20013,'$209','helm',1,1,1,1,1,1,1)");
        admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET item_id=20013 WHERE id=1003");
        admin(f,"INSERT INTO `"+ModernQuestSqlFixture.source(7)+"`.`character_skills` VALUES(3333,43)");
    }

    private static Read read(ModernBackendExecutor.SessionGate gate,ModernHasteRepository repo,
            ModernAccountAuthentication.Principal principal,Map<Integer,ModernJdbcBackend> daos,int id)throws Exception {
        return gate.call(10000,scope->{
            var source=daos.get(id).physicalCharacter(id,principal.accountForSource(id),3333,FIRST,true).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),System.currentTimeMillis());
            return new Read(ModernHaste.qualify(fighter),repo.character(principal,id,3333).orElseThrow());
        });
    }
    private static ModernHasteCatalog.Snapshot catalog(ModernJdbcBackend dao)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->dao.hasteCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
    }
    private static void rejected(ModernJdbcBackend dao) {
        var failure=assertThrows(SQLException.class,()->catalog(dao));
        assertEquals("modern JDBC backend unavailable",failure.getMessage());assertNull(failure.getCause());
        assertNull(failure.getNextException());assertEquals(0,failure.getSuppressed().length);
    }
    private static void admin(ModernQuestSqlFixture f,String sql)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement(sql)) {
            s.setQueryTimeout(3);s.execute();
        }
    }
}
