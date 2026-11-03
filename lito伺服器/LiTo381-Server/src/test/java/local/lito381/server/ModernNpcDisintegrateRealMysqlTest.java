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

/** Actual SELECT-only character/equipment/skill77 and NPC catalogs. Both
 * victim-only and complete engine casts; not a network/renderer/UI proof. */
class ModernNpcDisintegrateRealMysqlTest {
    record Read(ModernPhysicalCombat.Fighter caster,ModernDisintegrateCatalog.Snapshot skill,
                ModernNpcCatalog.Catalog catalog,ModernSkills.Snapshot skills) {}
    static Read read(ModernBackendExecutor.SessionGate gate,ModernServerConfig.SourceConfig source)throws Exception {
        return gate.call(10000,scope->{
            var dao=new ModernJdbcBackend(source.database(),1000,5000);
            assertTrue(dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD).accepted());
            var physical=dao.physicalCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase(),true,false,true,false).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(physical,ModernCombatProfileTest.binding(physical.input().equipment()),Optional.empty(),System.currentTimeMillis());
            var skill=dao.disintegrateCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow();
            // NPC catalog source is the configured 7, never the player's 42.
            return new Read(fighter,skill,dao.npcCatalog(7,source.catalogDatabase(),List.of(45262)),
                    dao.skillsCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow());
        });
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void isolatedSqlSkillQualificationAndFullWidthNpcDamageKeepSourceAndCatalogNamespaces()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls,false,false,true);
            var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            long hp=9_007_199_254_740_993L;String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
            // Only the newly owned fixture owner seeds DDL/data. Production
            // character schemas and running user's MySQL are not modified.
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                install(c,hp);
            }
            var first=read(gate,f.config.sources().get(0));var unlearned=read(gate,f.config.sources().get(1));
            var profile=ModernDisintegrate.qualify(first.caster());
            assertEquals(new TournamentPresence.CharacterKey(7,3333),first.caster().key());
            assertEquals(new TournamentPresence.CharacterKey(42,3333),unlearned.caster().key());
            assertTrue(first.skill().accepts(profile));assertEquals(ModernDisintegrateCatalog.Status.UNLEARNED,unlearned.skill().status());
            assertFalse(unlearned.skill().accepts(ModernDisintegrate.qualify(unlearned.caster())));
            assertFalse(first.skill().accepts(ModernDisintegrate.qualify(unlearned.caster())),"equal objid cannot borrow another source's learned skill");
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` VALUES(3333,77)");
            }
            var second=read(gate,f.config.sources().get(1));
            assertEquals(ModernDisintegrateCatalog.Status.UNLEARNED,unlearned.skill().status(),"old generation remains frozen");
            var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,3);
            for(var input:List.of(first,second)) {
                assertTrue(input.skill().accepts(ModernDisintegrate.qualify(input.caster())));
                var row=input.catalog().npcs().get(45262);
                assertEquals(new ModernNpcCatalog.Key(7,ModernQuestSqlFixture.CATALOG,45262),row.key());
                assertEquals(hp,row.get(ModernNpcCatalog.Field.HP));assertEquals(50,row.get(ModernNpcCatalog.Field.MR));
                // Source7 is an original-INT12 knight (hit+2, MP-2), while
                // source42 is an original-INT12 mage (no such bonuses).
                boolean knight=input.caster().key().sourceId()==7;
                var offense=input.caster().magic().offense();
                assertEquals(12,offense.effectiveIntelligence());assertEquals(0,offense.itemSpellPower());
                assertEquals(0,offense.originalDamage());assertEquals(0,offense.equipmentDamage());
                assertEquals(knight?2:0,offense.originalHit());
                long expected=knight?164:162; // floor(216 * (100-floor((50-hit)/2)) / 100)
                try(var roster=new ModernNpcRoster(ModernNpcRosterTest.SCOPE,map,List.of(cells.get(0),cells.get(1)),20,3000)) {
                    var npc=roster.spawn(ModernNpcRosterTest.SCOPE,List.of(new ModernNpcRoster.Spawn(row.rosterTemplate(),0,1,cells.get(0),2)),Set.of(cells.get(2))).get(0);
                    var plan=ModernNpcDisintegrate.prepareIncoming(input.caster(),input.catalog(),roster.snapshot(),npc.id(),1,b->b==15?0:99,0);
                    assertEquals(input.caster().key(),plan.attacker());assertEquals(row.key(),plan.catalogKey());
                    assertEquals(expected,plan.outcome().rawDamage());assertEquals(11746,plan.outcome().effect());
                    assertEquals(knight?68:70,plan.mpCost());
                    assertEquals(hp-expected,roster.commitIncoming(plan).hp());
                    assertEquals(hp,row.get(ModernNpcCatalog.Field.HP));
                    assertEquals(300,input.caster().profile().effective().initialMp());
                    assertEquals(500,input.caster().profile().source().persisted().lawful());
                }
            }
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"UPDATE "+catalog+"`npc` SET mr=9223372036854775807");
                execute(c,"UPDATE "+catalog+"`skills` SET mpConsume=69 WHERE skill_id=77");
                execute(c,"DELETE FROM `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` WHERE skill_id=77");
            }
            var malformed=read(gate,f.config.sources().get(0));var revoked=read(gate,f.config.sources().get(1));
            assertEquals(ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE,malformed.skill().status());
            assertFalse(malformed.skill().accepts(profile));assertEquals(ModernDisintegrateCatalog.Status.UNLEARNED,revoked.skill().status());
            assertTrue(first.skill().accepts(profile));assertTrue(second.skill().accepts(ModernDisintegrate.qualify(second.caster())));
            assertEquals(50,first.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.MR));
            assertEquals(Long.MAX_VALUE,malformed.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.MR));
            assertEquals(0,ModernNpcDisintegrate.againstNpc(profile,malformed.catalog(),ModernNpcMeleeTest.actor(malformed.catalog()),b->0,0).rawDamage(),"numeric calculation alone grants no learned permission");
            for(var input:List.of(malformed,revoked)) {
                assertEquals(300,input.caster().profile().effective().initialMp());
                assertEquals(500,input.caster().profile().source().persisted().lawful());
                assertEquals(hp,input.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP));
            }
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
    static void install(Connection c,long hp)throws Exception {
        if(hp<1)throw new IllegalArgumentException();String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
        execute(c,"ALTER TABLE "+catalog+"`skills` ADD id BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_happen BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_stop BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_fail BIGINT NOT NULL DEFAULT 0");
        execute(c,"CREATE TABLE "+catalog+"`skills_item` (skill_id BIGINT PRIMARY KEY,itemids VARCHAR(64) NULL,counts VARCHAR(64) NULL) ENGINE=InnoDB");
        execute(c,"CREATE TABLE "+catalog+"`skills_buff_ability` (skill_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        execute(c,"CREATE TABLE "+catalog+"`skills_負面技能控制` (skill_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        execute(c,"INSERT INTO "+catalog+"`skills`(skill_id,id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx,target_to,damage_value,damage_dice,damage_dice_count,attr,lawful,ranged) VALUES(77,16,'$1485','究極光裂術','attack',10,4,70,5000,64,18,11746,3,190,15,8,16,1,10)");
        execute(c,"INSERT INTO "+catalog+"`skills_item` VALUES(77,NULL,NULL)");
        execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(7)+"`.`character_skills` VALUES(3333,77)");
        for(int source:List.of(7,42))execute(c,"UPDATE `"+ModernQuestSqlFixture.source(source)+"`.`characters` SET MaxMp=300,CurMp=300,Lawful=500");
        execute(c,"UPDATE "+catalog+"`npc` SET hp="+hp+",mr=50,damage_reduction=9223372036854775807");
    }
    static void execute(Connection c,String sql)throws Exception {
        try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}
    }
}
