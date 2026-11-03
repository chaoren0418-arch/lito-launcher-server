package local.lito381.server;

import static local.lito381.server.ModernVipEquipment.Field.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernVipLifecycleSqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void realCatalogReloadAndDeletionCannotRewriteAppliedTemplatesOrEitherPlayersSql()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42))s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
            }
            var states=new HashMap<Integer,ModernVipLifecycle.State>();var savedAc=new HashMap<Integer,Long>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();});
                        long now=System.currentTimeMillis();var load=ModernVipLifecycleTest.load(data,0,Map.of(),now,5641);
                        var state=ModernVipLifecycle.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),load.owner(),1,0),load,now);
                        assertEquals(22,state.total().get(CHA));assertEquals(100,state.total().get(ADENA_RATE));
                        states.put(source.id(),state);savedAc.put(source.id(),data.input().equipment().source().persisted().armorClass());
                    }
                    // Only this newly owned fixture's catalog is changed; player rows are not SET by the ledger.
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                        s.setQueryTimeout(3);String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
                        s.execute("UPDATE "+catalog+"`道具_自訂穿脫能力` SET `魅力`=9000,`血量`=4294967299,`狀態圖編號`=506 WHERE `道具編號`=300086");
                        s.execute("UPDATE "+catalog+"`poly_變身賦予能力` SET addCha=3000 WHERE gfxId=5641");
                    }
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();});
                        long now=System.currentTimeMillis();var before=states.get(source.id());var next=ModernVipLifecycleTest.load(data,1,Map.of(),now,5641);
                        assertEquals(12000,next.total().get(CHA));assertEquals(4_294_967_299L,next.total().get(MAX_HP));
                        var candidate=ModernVipLifecycle.prepareEquipmentRevision(before,next,now);
                        var held=ModernVipLifecycleTest.accept(candidate,now);assertEquals(before.total(),held.total());assertEquals(before.shape(),held.shape());
                        assertEquals(java.util.Set.of(505L),held.icons());assertEquals(ModernVipEquipment.Abilities.zero(),candidate.delta());
                        var removed=ModernVipLifecycleTest.accept(ModernVipLifecycle.prepareRetainedUnequip(held,ModernExpiredVipSqlFixture.VIP_OBJECT,now),now);
                        var fresh=ModernVipLifecycleTest.load(data,3,Map.of(ModernExpiredVipSqlFixture.VIP_OBJECT,true),now,5641);
                        var equipped=ModernVipLifecycleTest.accept(ModernVipLifecycle.prepareEquip(removed,fresh,ModernExpiredVipSqlFixture.VIP_OBJECT,now),now);
                        assertEquals(12000,equipped.total().get(CHA));assertEquals(java.util.Set.of(506L),equipped.icons());
                        assertEquals(4_294_967_299L,equipped.total().get(MAX_HP));
                        assertEquals(savedAc.get(source.id()).longValue(),data.input().equipment().source().persisted().armorClass());
                        assertFalse(ModernPhysicalCombat.qualify(data,ModernCombatProfileTest.binding(data.input().equipment()),Optional.empty(),now).supported());
                        states.put(source.id(),held);
                    }
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                        s.setQueryTimeout(3);s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`道具_自訂穿脫能力` WHERE `道具編號`=300086");
                        s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`poly_變身賦予能力` WHERE gfxId=5641");
                    }
                    long now=System.currentTimeMillis();
                    for(var entry:states.entrySet()) {
                        var before=entry.getValue();var removal=ModernVipLifecycle.prepareRetainedUnequip(before,ModernExpiredVipSqlFixture.VIP_OBJECT,now);
                        assertEquals(-22,removal.delta().get(CHA));assertEquals(-100,removal.delta().get(ADENA_RATE));
                        assertEquals(0,removal.delta().get(MAX_HP));assertEquals(java.util.Set.of(505L),removal.removedIcons());
                        assertEquals(ModernVipEquipment.Abilities.zero(),ModernVipLifecycleTest.accept(removal,now).total());
                        var wrong=states.get(entry.getKey()==7?42:7);
                        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(removal,wrong.actor(),before,1,now));
                    }
                } finally {executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42))try(var rows=s.executeQuery("SELECT i.is_equipped,i.count,t.usertime FROM `"+ModernQuestSqlFixture.source(id)+"`.character_items i JOIN `"
                        +ModernQuestSqlFixture.source(id)+"`.character_items_time t ON t.itemr_obj_id=i.id WHERE i.id="+ModernExpiredVipSqlFixture.VIP_OBJECT+" AND i.char_id=3333")) {
                    assertTrue(rows.next());assertEquals(1,rows.getLong(1));assertEquals(1,rows.getLong(2));
                    assertTrue(rows.getString(3).startsWith("2999-01-01"));assertFalse(rows.next());
                }
            }
            System.err.println("VIP_LIFECYCLE_SQL sources=2 reloadPinned=true deletedCatalogRemoval=true freshReequip=true i64=true rawPlayersPreserved=true combatStillRejected=true");
            fixture.successful();
        }
    }
}
