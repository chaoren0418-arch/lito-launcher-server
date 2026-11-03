package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Real authentication, per-source JDBC, learned catalog and equipped profile.
 * All DDL/mutations belong to a fresh owned fixture, never production SQL.
 * Does not claim a wire capability, live SET, cast or UI integration. */
class ModernGreaterHasteRealMysqlTest {
    static final String SECOND="greater_haste_moon";
    static final String FIRST=ModernQuestSqlFixture.CATALOG;
    static final List<String> TABLES=List.of("skills","skills_item","skills_buff_ability","skills_負面技能控制");
    record Read(ModernPhysicalCombat.Fighter fighter,ModernGreaterHasteCatalog.Snapshot learned) {}

    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void actualSourceSwitchReloadQualificationAndNumericRejectionUseSelectOnlyAccounts()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);
        var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null,"explicit isolated MySQL and shared root required");
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls,false,false,true);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            install(f);
            var sun=new ModernJdbcBackend(f.config.sources().get(0).database(),1000,5000);
            var moon=new ModernJdbcBackend(f.config.sources().get(1).database(),1000,5000);
            var daos=Map.of(7,sun,42,moon);
            var repo=ModernGreaterHasteRepository.jdbc(Map.of(
                    7,new ModernGreaterHasteRepository.Source(sun,FIRST),
                    42,new ModernGreaterHasteRepository.Source(moon,SECOND)));
            var verifier=ModernAccountAuthentication.repositories(Map.of(
                    7,ModernAccountAuthentication.SourceBinding.direct("太陽神",sun::authenticate),
                    42,ModernAccountAuthentication.SourceBinding.direct("月亮神",moon::authenticate)),account->false);
            var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,
                    ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
            assertEquals("fixture-user",principal.accountForSource(7));
            assertEquals("FIXTURE-USER",principal.accountForSource(42));
            var first=read(gate,repo,principal,daos,7);
            var second=read(gate,repo,principal,daos,42);
            var firstProfile=ModernGreaterHaste.qualify(first.fighter());
            assertTrue(first.learned().accepts(firstProfile));assertEquals(28,firstProfile.mpCost());
            assertEquals(ModernGreaterHasteCatalog.Status.UNLEARNED,second.learned().status());
            assertFalse(first.learned().accepts(ModernGreaterHaste.qualify(second.fighter())));
            // Account lookup is byte/case-sensitive in character tables; using
            // the display/global account for Moon cannot silently work here.
            assertTrue(gate.call(10000,scope->moon.greaterHasteCharacter(42,"fixture-user",3333,SECOND)).isEmpty());
            assertTrue(gate.call(10000,scope->repo.character(principal,7,3334)).isEmpty());
            assertThrows(SecurityException.class,()->repo.character(principal,99,3333));

            admin(f,"INSERT INTO `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` VALUES(3333,54)");
            var learnedMoon=read(gate,repo,principal,daos,42);
            assertTrue(learnedMoon.learned().accepts(ModernGreaterHaste.qualify(learnedMoon.fighter())));
            assertEquals(60,ModernGreaterHaste.qualify(learnedMoon.fighter()).mpCost());
            assertEquals(ModernGreaterHasteCatalog.Status.UNLEARNED,second.learned().status(),"old read is immutable");
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET buffDuration=2399 WHERE skill_id=54");
            assertEquals(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE,read(gate,repo,principal,daos,42).learned().status());
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,read(gate,repo,principal,daos,7).learned().status());
            admin(f,"UPDATE `"+SECOND+"`.`skills` SET buffDuration=2400 WHERE skill_id=54");
            admin(f,"DELETE FROM `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` WHERE skill_id=54");
            assertEquals(ModernGreaterHasteCatalog.Status.UNLEARNED,read(gate,repo,principal,daos,42).learned().status());
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,learnedMoon.learned().status());

            // Fresh source equipment baseline, not a claim about live match SET.
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET is_equipped=0 WHERE id=1003");
            assertEquals(58,ModernGreaterHaste.qualify(read(gate,repo,principal,daos,7).fighter()).mpCost());
            assertEquals(28,ModernGreaterHaste.qualify(first.fighter()).mpCost());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Intel=5000000000");
            var wide=read(gate,repo,principal,daos,7);
            assertEquals(5_000_000_000L,wide.fighter().profile().effective().intelligence());
            assertEquals(52,ModernGreaterHaste.qualify(wide.fighter()).mpCost());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET is_equipped=1 WHERE id=1003");
            assertEquals(25,ModernGreaterHaste.qualify(read(gate,repo,principal,daos,7).fighter()).mpCost());
            assertEquals(300,first.fighter().profile().effective().initialMp());

            for(String change:List.of("mpConsume=59","effect_skill_id=1","target='self'")) {
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET "+change+" WHERE skill_id=54");
                assertEquals(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
                resetSkill(f);
            }
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume DECIMAL(21,1)");
            for(String value:List.of("NULL","60.5","9223372036854775808")) {
                admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume="+value+" WHERE skill_id=54");
                rejected(sun);
            }
            admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume=60 WHERE skill_id=54");
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume VARCHAR(32)");rejected(sun);
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume DOUBLE");rejected(sun);
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` MODIFY mpConsume BIGINT");
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,catalog(sun).status());

            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` DROP COLUMN effect_skill_id");
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,catalog(sun).status(),"only certified absent optional column may use zero");
            admin(f,"ALTER TABLE `"+FIRST+"`.`skills` ADD effect_skill_id BIGINT NOT NULL DEFAULT 0");
            admin(f,"UPDATE `"+FIRST+"`.`skills_item` SET itemids='40010' WHERE skill_id=54");
            assertEquals(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"UPDATE `"+FIRST+"`.`skills_item` SET itemids=NULL WHERE skill_id=54");
            admin(f,"INSERT INTO `"+FIRST+"`.`skills_buff_ability` VALUES(54)");
            assertEquals(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE,catalog(sun).status());
            admin(f,"DELETE FROM `"+FIRST+"`.`skills_buff_ability` WHERE skill_id=54");
            admin(f,"RENAME TABLE `"+FIRST+"`.`skills_item` TO `"+FIRST+"`.`skills_item_hidden`");rejected(sun);
            admin(f,"RENAME TABLE `"+FIRST+"`.`skills_item_hidden` TO `"+FIRST+"`.`skills_item`");
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=1");
            assertTrue(ModernEquipmentSourceJdbcTest.scoped(()->sun.greaterHasteCharacter(7,"fixture-user",3333,FIRST)).isEmpty());
            admin(f,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`characters` SET Banned=0");
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,catalog(sun).status());
            // The ordinary player catalog must still advertise UNIMPLEMENTED,
            // not infer executable support merely from these newly read facts.
            var skills=gate.call(10000,scope->sun.skillsCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,skills.skills().stream().filter(s->s.skillId()==54).findFirst().orElseThrow().support());

            var connections=new ModernSettlementConnections(f.config.sources().get(0).database(),1000,3000,1);
            try {
                try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5))) {
                    c.setReadOnly(false);
                    try(var s=c.prepareStatement("UPDATE `"+FIRST+"`.`skills` SET mpConsume=1 WHERE skill_id=54")) {
                        s.setQueryTimeout(2);assertEquals(1142,assertThrows(SQLException.class,s::executeUpdate).getErrorCode());
                    }
                }
            } finally {connections.close();assertTrue(connections.awaitTermination(5000));assertTrue(connections.isQuiescent());}
            assertEquals(ModernGreaterHasteCatalog.Status.QUALIFIED,catalog(sun).status());
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertTrue(executor.isQuiescent());
            assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
            System.err.println("HASTE_SQL: authenticated source7/source42, separate catalogs, source gear reload, INT5000000000, malformed templates and SELECT-only grants verified; cast support remains unimplemented");
        }
    }

    private static Read read(ModernBackendExecutor.SessionGate gate,ModernGreaterHasteRepository repo,
            ModernAccountAuthentication.Principal principal,Map<Integer,ModernJdbcBackend> daos,int id)throws Exception {
        return gate.call(10000,scope->{
            var source=daos.get(id).physicalCharacter(id,principal.accountForSource(id),3333,FIRST,true).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),
                    Optional.empty(),System.currentTimeMillis());
            return new Read(fighter,repo.character(principal,id,3333).orElseThrow());
        });
    }
    private static ModernGreaterHasteCatalog.Snapshot catalog(ModernJdbcBackend dao)throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(()->dao.greaterHasteCharacter(7,"fixture-user",3333,FIRST)).orElseThrow();
    }
    private static void rejected(ModernJdbcBackend dao) {
        var failure=assertThrows(SQLException.class,()->catalog(dao));
        assertEquals("modern JDBC backend unavailable",failure.getMessage());assertNull(failure.getCause());
        assertNull(failure.getNextException());assertEquals(0,failure.getSuppressed().length);
    }
    private static void resetSkill(ModernQuestSqlFixture f)throws Exception {
        admin(f,"UPDATE `"+FIRST+"`.`skills` SET mpConsume=60,effect_skill_id=0,target='none' WHERE skill_id=54");
    }
    // Shared owned-SQL seed; callers must construct their own explicit rules.
    static void install(ModernQuestSqlFixture f)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(15))) {
            execute(c,"ALTER TABLE `"+FIRST+"`.`skills` ADD id BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_happen BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_stop BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_fail BIGINT NOT NULL DEFAULT 0");
            execute(c,"CREATE TABLE `"+FIRST+"`.`skills_item` (skill_id BIGINT PRIMARY KEY,itemids VARCHAR(64),counts VARCHAR(64))");
            execute(c,"CREATE TABLE `"+FIRST+"`.`skills_buff_ability` (skill_id BIGINT PRIMARY KEY)");
            execute(c,"CREATE TABLE `"+FIRST+"`.`skills_負面技能控制` (skill_id BIGINT PRIMARY KEY)");
            execute(c,"INSERT INTO `"+FIRST+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,buffDuration,attr,type,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop) VALUES(54,'$1976','強力加速術','none',7,5,60,10,2400,8,2,32,19,3104,184,185)");
            execute(c,"INSERT INTO `"+FIRST+"`.`skills_item` VALUES(54,NULL,NULL)");
            execute(c,"CREATE DATABASE `"+SECOND+"` CHARACTER SET utf8mb4");
            for(String table:TABLES) {
                execute(c,"CREATE TABLE `"+SECOND+"`.`"+table+"` LIKE `"+FIRST+"`.`"+table+"`");
                execute(c,"INSERT INTO `"+SECOND+"`.`"+table+"` SELECT * FROM `"+FIRST+"`.`"+table+"`");
                execute(c,"GRANT SELECT ON `"+SECOND+"`.`"+table+"` TO '"+ModernQuestSqlFixture.READER+"'@'127.0.0.1'");
            }
            for(int id:List.of(7,42)) {
                execute(c,"ALTER TABLE `"+ModernQuestSqlFixture.source(id)+"`.`characters` MODIFY account_name VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
                execute(c,"UPDATE `"+ModernQuestSqlFixture.source(id)+"`.`characters` SET MaxMp=300,CurMp=300");
            }
            execute(c,"UPDATE `"+ModernQuestSqlFixture.source(42)+"`.`accounts` SET login='FIXTURE-USER'");
            execute(c,"UPDATE `"+ModernQuestSqlFixture.source(42)+"`.`characters` SET account_name='FIXTURE-USER'");
            execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(7)+"`.`character_skills` VALUES(3333,54)");
            execute(c,"INSERT INTO `"+FIRST+"`.`armor`(item_id,name_id,type,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist) VALUES(20023,'$209','helm',1,1,1,1,1,1,1)");
            // This fixture previously qualified only a sword. A real helmet
            // also requires explicit armor rule tables; never relax the
            // production reader's missing-table/permission distinction.
            for(String suffix:List.of("greater_類型","bless_類型","bless_指定")) {
                execute(c,"CREATE TABLE `"+FIRST+"`.`裝備_armor_"+suffix+"` LIKE `"+FIRST+"`.`裝備_weapon_"+suffix+"`");
                if(!suffix.equals("bless_指定"))execute(c,"ALTER TABLE `"+FIRST+"`.`裝備_armor_"+suffix+"` CHANGE weapon_type armor_type BIGINT NOT NULL DEFAULT 0");
            }
            execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(7)+"`.`character_items`(id,char_id,item_id,count,is_equipped,is_id,bless) VALUES(1003,3333,20023,1,1,1,0)");
        }
    }
    private static void admin(ModernQuestSqlFixture f,String sql)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))){execute(c,sql);}
    }
    private static void execute(Connection c,String sql)throws Exception {
        try(var s=c.prepareStatement(sql)){s.setQueryTimeout(3);s.execute();}
    }
}
