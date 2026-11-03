package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Actual source character/equipment/skill and NPC SQL, but victim-side magic
 * preparation/commit only: not an engine MP debit, wire/renderer or full match. */
class ModernNpcMagicRealMysqlTest {
    record Read(ModernMagicCombat.Fighter caster,ModernSkills.Snapshot skills,ModernNpcCatalog.Catalog catalog){}
    static Read read(ModernBackendExecutor.SessionGate gate,ModernServerConfig.SourceConfig source)throws Exception {
        return gate.call(10000,scope->{
            var dao=new ModernJdbcBackend(source.database(),1000,5000);
            assertTrue(dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD).accepted());
            var physical=dao.physicalCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase(),true,false,true,false).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(physical,ModernCombatProfileTest.binding(physical.input().equipment()),Optional.empty(),System.currentTimeMillis());
            var skills=dao.skillsCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow();
            return new Read(fighter.magic(),skills,dao.npcCatalog(source.id(),source.catalogDatabase(),List.of(45262)));
        });
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void actualReadOnlySourcesAndI64NpcCatalogDriveTheVictimPlanWithoutSourceWrites()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls);var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            long hp=9_007_199_254_740_993L;
            // Only this newly owned fixture is writable. Formal source schemas,
            // config and running MySQL are never used for seeding this test.
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"CREATE TABLE `"+ModernQuestSqlFixture.CATALOG+"`.`角色_精神成長` (id BIGINT PRIMARY KEY,`魔防` BIGINT NOT NULL) ENGINE=MyISAM");
                for(int id=1;id<=50;id++)execute(c,"INSERT INTO `"+ModernQuestSqlFixture.CATALOG+"`.`角色_精神成長` VALUES("+id+",0)");
                execute(c,"INSERT INTO `"+ModernQuestSqlFixture.CATALOG+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx,target_to,damage_value,damage_dice,damage_dice_count,attr,ranged) VALUES(4,'$1439','Light Arrow','attack',1,3,3,10,64,18,167,3,6,10,1,16,10)");
                for(int source:List.of(7,42))execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(source)+"`.`character_skills` VALUES(3333,4)");
                execute(c,"UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`npc` SET hp="+hp+",mr=50,damage_reduction=9223372036854775807");
            }
            var first=read(gate,f.config.sources().get(0));var second=read(gate,f.config.sources().get(1));
            assertNotEquals(first.caster().key(),second.caster().key());assertEquals(3333,first.caster().key().objectId());
            var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,3);
            for(var input:List.of(first,second)) {
                assertTrue(input.caster().supported());assertTrue(input.skills().lightArrow().isPresent());
                var catalog=input.catalog();var row=catalog.npcs().get(45262);assertEquals(hp,row.get(ModernNpcCatalog.Field.HP));assertEquals(50,row.get(ModernNpcCatalog.Field.MR));
                try(var roster=new ModernNpcRoster(ModernNpcRosterTest.SCOPE,map,List.of(cells.get(0),cells.get(1)),20,3000)) {
                    var npc=roster.spawn(ModernNpcRosterTest.SCOPE,List.of(new ModernNpcRoster.Spawn(row.rosterTemplate(),0,1,cells.get(0),2)),Set.of(cells.get(2))).get(0);
                    var plan=ModernNpcMagic.prepareIncoming(input.caster(),catalog,roster.snapshot(),npc.id(),1,new ModernNpcMeleeTest.Rolls(9,99));
                    assertEquals(input.caster().key(),plan.attacker());assertEquals(row.key(),plan.catalogKey());assertTrue(plan.outcome().appliedDamage()>0);
                    var after=roster.commitIncoming(plan);assertEquals(hp-plan.outcome().appliedDamage(),after.hp());
                    assertEquals(hp,row.get(ModernNpcCatalog.Field.HP));assertEquals(input.caster().mpCost(),plan.mpCost());
                }
            }
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`npc` SET mr=9223372036854775807");
                execute(c,"DELETE FROM `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` WHERE skill_id=4");
            }
            var changed=read(gate,f.config.sources().get(1));assertTrue(changed.skills().skills().isEmpty());assertTrue(second.skills().lightArrow().isPresent());
            assertEquals(Long.MAX_VALUE,changed.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.MR));
            assertEquals(50,second.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.MR),"old generation does not refresh behind a cast");
            assertEquals(0,ModernNpcMagic.againstNpc(changed.caster(),changed.catalog(),ModernNpcMeleeTest.actor(changed.catalog()),new ModernNpcMeleeTest.Rolls(9,99)).rawDamage());
            assertEquals(hp,changed.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP));
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
    private static void execute(Connection c,String sql)throws Exception {try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}}
}
