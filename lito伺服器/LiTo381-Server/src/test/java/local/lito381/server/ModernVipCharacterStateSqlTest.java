package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ModernVipCharacterStateSqlTest {
    @TempDir Path directory;
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void twoSourceRebuiltFramesRetainAppliedCatalogAndMatchRealIsolatedStoreCasWithoutSourceWrites()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42)) {
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
                    // This fixture defaults to 1 HP; admit a genuinely injured
                    // 5-HP live actor only after explicitly seeding a 100-HP base.
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.characters SET MaxHp=100 WHERE objid=3333");
                }
            }
            var states=new HashMap<Integer,ModernVipCharacterState>();var savedAc=new HashMap<Integer,Long>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession();var store=new ModernEquipmentOverrideStore(directory)) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();});
                        long now=System.currentTimeMillis();var binding=ModernCombatProfileTest.binding(data.input().equipment());
                        var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),binding.key(),1,0),data,binding,Optional.empty(),now,5641);
                        assertEquals(100,state.profile().character().effective().maximumHp());
                        assertEquals(22,state.ledger().total().get(ModernVipEquipment.Field.CHA));states.put(source.id(),state);
                        savedAc.put(source.id(),data.input().equipment().source().persisted().armorClass());
                    }
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                        s.setQueryTimeout(3);String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
                        s.execute("UPDATE "+catalog+"`道具_自訂穿脫能力` SET `魅力`=9000,`血量`=4294967299,`傷害減免`=37,`魔法傷害減免`=41 WHERE `道具編號`=300086");
                        s.execute("UPDATE "+catalog+"`poly_變身賦予能力` SET addCha=3000 WHERE gfxId=5641");
                    }
                    for(var source:fixture.config.sources()) {
                        var before=states.get(source.id());long now=System.currentTimeMillis();
                        var remove=ModernVipCharacterState.prepareRetainedUnequip(before,ModernExpiredVipSqlFixture.VIP_OBJECT,now);
                        assertEquals(-22,remove.vip().delta().get(ModernVipEquipment.Field.CHA));
                        var other=states.get(source.id()==7?42:7);
                        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.acceptAt(remove,before,other.ledger().actor(),0,now,5,2));
                        var staged=new AtomicReference<ModernVipCharacterState>();
                        try(var lease=new ModernEquipmentLease.Issuer().open(new Object(),before.ledger().actor().owner(),1,()->{})) {
                            var commit=gate.call(5000,scope->{try(var op=lease.operation(scope)) {
                                return store.compareAndSet(before.ledger().actor().owner(),ModernEquipmentOverrideStore.Expected.absent(),remove.nextStore().orElseThrow().value(),scope,op,()->{
                                    staged.set(ModernVipCharacterStateTest.accept(remove,before,System.currentTimeMillis(),5,2));return true;
                                });
                            }});
                            assertTrue(commit.changed());assertEquals(remove.nextStore().orElseThrow(),commit.stored());
                        }
                        var empty=staged.get();assertEquals(5,empty.profile().character().effective().initialHp());assertEquals(0,empty.profile().genericDamageReduction());
                        assertFalse(ModernVipCharacterStateTest.flag(empty,ModernExpiredVipSqlFixture.VIP_OBJECT));
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var fresh=gate.call(5000,scope->{var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();});
                        long nextNow=System.currentTimeMillis();var load=ModernVipCharacterStateTest.load(fresh,2,Map.of(ModernExpiredVipSqlFixture.VIP_OBJECT,true),nextNow,5641);
                        var equip=ModernVipCharacterState.prepareEquip(empty,load,ModernExpiredVipSqlFixture.VIP_OBJECT,nextNow);
                        try(var lease=new ModernEquipmentLease.Issuer().open(new Object(),before.ledger().actor().owner(),1,()->{})) {
                            var commit=gate.call(5000,scope->{try(var op=lease.operation(scope)) {
                                return store.compareAndSet(before.ledger().actor().owner(),ModernEquipmentOverrideStore.Expected.of(empty.stored().orElseThrow()),equip.nextStore().orElseThrow().value(),scope,op,()->{
                                    staged.set(ModernVipCharacterStateTest.accept(equip,empty,System.currentTimeMillis(),3,1));return true;
                                });
                            }});
                            assertEquals(equip.nextStore().orElseThrow(),commit.stored());assertTrue(commit.changed());
                        }
                        var active=staged.get();assertEquals(3,active.profile().character().effective().initialHp());assertEquals(1,active.profile().character().effective().initialMp());
                        assertEquals(4_294_967_299L,active.ledger().total().get(ModernVipEquipment.Field.MAX_HP));
                        assertEquals(12000,active.ledger().total().get(ModernVipEquipment.Field.CHA));assertEquals(37,active.profile().genericDamageReduction());assertEquals(41,active.profile().magicOnlyReduction());
                        assertEquals(2,active.ledger().equipmentRevision());assertEquals(active.stored(),gate.call(5000,scope->store.read(active.ledger().actor().owner(),scope)));
                        assertFalse(ModernPhysicalCombat.qualify(fresh,load.binding(),active.stored(),nextNow).supported());states.put(source.id(),active);
                    }
                    // Removal uses the pinned complete contribution even after deletion.
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                        s.setQueryTimeout(3);s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`道具_自訂穿脫能力` WHERE `道具編號`=300086");
                        s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`poly_變身賦予能力` WHERE gfxId=5641");
                    }
                    for(var state:states.values()) {
                        long now=System.currentTimeMillis();var remove=ModernVipCharacterState.prepareRetainedUnequip(state,ModernExpiredVipSqlFixture.VIP_OBJECT,now);
                        var after=ModernVipCharacterStateTest.accept(remove,state,now,1,0);
                        assertEquals(0,after.profile().genericDamageReduction());assertEquals(0,after.profile().magicOnlyReduction());
                        assertEquals(1,after.profile().character().effective().initialHp());
                        // A pure calculation must not persist the third proposed revision.
                        assertEquals(state.stored(),gate.call(5000,scope->store.read(state.ledger().actor().owner(),scope)));
                    }
                }finally{executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42))try(var rows=s.executeQuery("SELECT c.Ac,i.is_equipped,i.count,t.usertime FROM `"+ModernQuestSqlFixture.source(id)+"`.characters c JOIN `"
                        +ModernQuestSqlFixture.source(id)+"`.character_items i ON i.char_id=c.objid JOIN `"+ModernQuestSqlFixture.source(id)
                        +"`.character_items_time t ON t.itemr_obj_id=i.id WHERE c.objid=3333 AND i.id="+ModernExpiredVipSqlFixture.VIP_OBJECT)) {
                    assertTrue(rows.next());assertEquals(savedAc.get(id).longValue(),rows.getLong(1));assertEquals(1,rows.getLong(2));assertEquals(1,rows.getLong(3));
                    assertTrue(rows.getString(4).startsWith("2999-01-01"));assertFalse(rows.next());
                }
            }
            assertEquals(0,fixture.mysql.activeConnections());
            System.err.println("VIP_CHARACTER_STATE_SQL sources=2 sameObjectId=true pinnedRemoval=true freshReequip=true isolatedCas=true completeI64=true liveVitals=true sourcePreserved=true noRuntimeGrant=true");
            fixture.successful();
        }
    }
}
