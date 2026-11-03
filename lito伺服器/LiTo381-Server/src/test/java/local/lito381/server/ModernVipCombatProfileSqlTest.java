package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Numeric integration using a newly owned MySQL fixture, never a player database. */
class ModernVipCombatProfileSqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void twoSourcesKeepWideReductionAndPinnedRemovalWithoutWritingImportedCharacters()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
                s.execute("CREATE TABLE "+catalog+"`角色_精神成長` (id BIGINT PRIMARY KEY,`魔防` BIGINT NOT NULL) ENGINE=InnoDB");
                for(int i=1;i<=50;i++)s.execute("INSERT INTO "+catalog+"`角色_精神成長` VALUES("+i+",0)");
                s.execute("ALTER TABLE "+catalog+"weapon ADD COLUMN magic_bow BIGINT NOT NULL DEFAULT 0,ADD COLUMN arrow_spr BIGINT NOT NULL DEFAULT -1");
                s.execute("UPDATE "+catalog+"`道具_自訂穿脫能力` SET `變身編號`=0,`血量`=4294967299,`傷害減免`=4294967301,`魔法傷害減免`=4294967303,"
                        +"`回血`=13,`回魔`=17,`近戰傷害`=7,`經驗獲取率`=100,`金幣獲取率`=150 WHERE `道具編號`=300086");
                for(int id:List.of(7,42)) {
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.characters SET MaxHp=100 WHERE objid=3333");
                }
            }
            var states=new HashMap<Integer,ModernVipCharacterState>();var fighters=new HashMap<Integer,ModernPhysicalCombat.Fighter>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),true,true,true,false).orElseThrow();});
                        var binding=ModernCombatProfileTest.binding(data.input().equipment());long now=System.currentTimeMillis();
                        var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),binding.key(),1,0),data,binding,Optional.empty(),now,0);
                        var fighter=ModernPhysicalCombat.qualifyVip(state,now);
                        assertTrue(fighter.supported());assertTrue(fighter.magic().supported());assertTrue(fighter.ranged().defenseSupported());assertTrue(fighter.regen().supported());
                        assertFalse(ModernPhysicalCombat.qualify(data,binding,Optional.empty(),now).supported());
                        assertEquals(4_294_967_399L,fighter.profile().effective().maximumHp());assertEquals(4_294_967_301L,fighter.defense().armorReduction());
                        assertEquals(4_294_967_301L,fighter.ranged().values().defense().armorReduction());
                        assertEquals(4_294_967_301L,fighter.magic().defense().armorReduction());assertEquals(4_294_967_303L,fighter.magic().defense().magicOnlyReduction());
                        assertEquals(7,fighter.offense().itemDamage());assertEquals(13,fighter.regen().values().equipmentHpRegen());assertEquals(17,fighter.regen().values().equipmentMpRegen());
                        assertEquals(100,fighter.vip().experienceBonusPercent());assertEquals(150,fighter.vip().adenaBonusPercent());
                        states.put(source.id(),state);fighters.put(source.id(),fighter);
                    }
                }finally{executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            assertEquals(3333,fighters.get(7).key().objectId());assertEquals(3333,fighters.get(42).key().objectId());
            assertNotEquals(fighters.get(7).key(),fighters.get(42).key());
            assertThrows(IllegalArgumentException.class,()->fighters.get(7).vip().require(fighters.get(42).vip().source(),fighters.get(42).profile()));
            // A catalog edit must not alter an already-applied contribution or its inverse.
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);s.execute("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`道具_自訂穿脫能力` SET `傷害減免`=1,`魔法傷害減免`=2 WHERE `道具編號`=300086");
            }
            for(int id:List.of(7,42)) {
                var fighter=fighters.get(id);var before=states.get(id);long now=System.currentTimeMillis();
                assertEquals(4_294_967_301L,fighter.defense().armorReduction());assertEquals(4_294_967_303L,fighter.magic().defense().magicOnlyReduction());
                var attacker=ModernVipCombatProfileTest.magicValues(30,0,0);
                var impact=ModernMagicCombat.resolveValues(attacker,fighter.magic().values(),37,b->b-1);
                assertEquals(0,impact.appliedDamage());assertEquals(37,impact.remainingHp());assertFalse(impact.death());
                var remove=ModernVipCharacterState.prepareRetainedUnequip(before,ModernExpiredVipSqlFixture.VIP_OBJECT,now);
                var next=ModernVipCharacterStateTest.accept(remove,before,now,37,11);var empty=ModernPhysicalCombat.qualifyVip(next,now);
                assertTrue(empty.supported());assertTrue(empty.magic().supported());assertEquals(100,empty.profile().effective().maximumHp());
                assertEquals(0,empty.defense().armorReduction());assertEquals(0,empty.magic().defense().magicOnlyReduction());assertEquals(0,empty.offense().itemDamage());
                assertEquals(0,empty.regen().values().equipmentHpRegen());assertEquals(0,empty.regen().values().equipmentMpRegen());
                assertEquals(0,empty.vip().experienceBonusPercent());assertEquals(0,empty.vip().adenaBonusPercent());
                assertEquals(37,empty.profile().effective().initialHp());assertEquals(11,empty.profile().effective().initialMp());
                assertEquals(1,next.ledger().equipmentRevision());
            }
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42))try(var rows=s.executeQuery("SELECT c.Ac,c.MaxHp,i.is_equipped,i.count,t.usertime FROM `"+ModernQuestSqlFixture.source(id)
                        +"`.characters c JOIN `"+ModernQuestSqlFixture.source(id)+"`.character_items i ON i.char_id=c.objid JOIN `"+ModernQuestSqlFixture.source(id)
                        +"`.character_items_time t ON t.itemr_obj_id=i.id WHERE c.objid=3333 AND i.id="+ModernExpiredVipSqlFixture.VIP_OBJECT)) {
                    assertTrue(rows.next());assertEquals(states.get(id).profile().vip().source().input().equipment().source().persisted().armorClass(),rows.getLong(1));
                    assertEquals(100,rows.getLong(2));assertEquals(1,rows.getLong(3));assertEquals(1,rows.getLong(4));assertTrue(rows.getString(5).startsWith("2999-01-01"));assertFalse(rows.next());
                }
            }
            assertEquals(0,fixture.mysql.activeConnections());
            System.err.println("VIP_COMBAT_SQL sources=2 sameObjectId=true completeI64=true physicalMagicBowRegen=true pinnedRemoval=true liveVitals=true sourcePreserved=true noEngineOrRewardGrant=true");
            fixture.successful();
        }
    }
}
