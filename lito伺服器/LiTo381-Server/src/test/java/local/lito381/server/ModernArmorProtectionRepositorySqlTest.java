package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Disposable MySQL only: production repository + scoped authority, not live admission. */
class ModernArmorProtectionRepositorySqlTest {
    @TempDir Path directory;
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void configuredSourceAndOwnerRouteOneQualifiedBundleThroughAuthorityWithoutSourceWrites()throws Exception {
        var home=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(home!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(home),Path.of(shared),tls)) {
            ModernArmorProcsSqlTest.seed(fixture);ModernArmorProtectionSourceSqlTest.seedSkill(fixture);
            var original=ModernArmorProcsSqlTest.snapshot(fixture);var connections=new AtomicInteger();
            var configured=new HashMap<Integer,ModernEquipmentRepository.Source>();var accounts=new HashMap<Integer,String>();
            var authorized=new HashSet<ModernAccountAuthentication.Source>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession();var store=new ModernEquipmentOverrideStore(directory)) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000,(url,properties)->{
                            connections.incrementAndGet();return new com.mysql.cj.jdbc.Driver().connect(url,properties);
                        });
                        var account=gate.call(5000,scope->dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD));
                        assertTrue(account.accepted());accounts.put(source.id(),account.account());
                        authorized.add(new ModernAccountAuthentication.Source(source.id(),"fixture-"+source.id()));
                        configured.put(source.id(),new ModernEquipmentRepository.Source(source,dao));
                    }
                    var principal=new ModernAccountAuthentication.Principal("armor-repository",authorized,accounts);
                    var prospective=ModernEquipmentRepository.armorProtectionJdbc(configured,false,false,false,false,false,false);
                    var ordinary=ModernEquipmentRepository.physicalJdbc(configured);
                    assertTrue(prospective.armorProtectionSource(7));assertTrue(prospective.armorProtectionSource(42));assertFalse(prospective.armorProtectionSource(99));
                    assertFalse(ordinary.armorProtectionSource(7));
                    var authority=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("second equipment read");},null,prospective,store);
                    var fighters=new ArrayList<ModernPhysicalCombat.Fighter>();
                    for(var id:List.of(7,42)) {
                        int before=connections.get();
                        var loaded=gate.call(5000,scope->prospective.read(principal,id,3333));
                        assertEquals(before+1,connections.get());assertNotNull(loaded.protection());assertSame(loaded.source(),loaded.protection().physical());
                        var row=loaded.source().input().equipment().source().character();
                        before=connections.get();var lease=new ModernEquipmentLease.Issuer().open(new Object(),new TournamentPresence.CharacterKey(id,3333),1,()->{});
                        try(lease) {
                            var fighter=gate.call(5000,scope->{try(var op=lease.operation(scope)){return authority.preparePhysical(principal,row,op);}});
                            assertTrue(fighter.supported());assertNotNull(fighter.armorProtection());assertEquals(0,fighter.profile().equipmentRevision());
                            var actor=new ModernArmorProcProtection.Actor(UUID.randomUUID(),fighter.key(),1,0);
                            var rules=fighter.armorProtection().select(actor).rules();assertEquals(id==7?1:2,rules.size());
                            if(id==42){assertEquals(52,rules.get(1).rule().id());assertEquals(0,rules.get(1).rule().chance(true));}
                            assertEquals(id==7?3:6,rules.get(0).rule().seconds());assertEquals(11101,rules.get(0).rule().graphics());
                            fighters.add(fighter);
                        }
                        assertEquals(before+1,connections.get(),"authority does not split gear and skill across connections");
                        var legacy=gate.call(5000,scope->ordinary.read(principal,id,3333));assertNull(legacy.protection());
                        before=connections.get();
                        var unselected=gate.call(5000,scope->prospective.readForWorld(principal,id,3333,false));
                        assertNull(unselected.protection());assertEquals(legacy.source().input().equipment().source(),unselected.source().input().equipment().source());
                        assertEquals(legacy.source().growth(),unselected.source().growth());assertEquals(legacy.source().templates(),unselected.source().templates());
                        assertEquals(legacy.source().procCatalogIds(),unselected.source().procCatalogIds());assertEquals(legacy.ownerBinding(),unselected.ownerBinding());
                        assertEquals(before+1,connections.get(),"fresh observation timestamps are not shared between separate reads");
                        var selected=gate.call(5000,scope->prospective.readForWorld(principal,id,3333,true));
                        assertNotNull(selected.protection());assertSame(selected.source(),selected.protection().physical());
                        before=connections.get();assertThrows(Exception.class,()->gate.call(5000,scope->ordinary.readForWorld(principal,id,3333,true)));
                        assertEquals(before,connections.get(),"an unconfigured armor read never opens SQL");
                        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,ModernPhysicalCombat.qualify(legacy.source(),
                                new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(id,3333),legacy.ownerBinding()),
                                Optional.empty(),System.currentTimeMillis()).unsupportedReason());
                        assertTrue(gate.call(5000,scope->store.read(new TournamentPresence.CharacterKey(id,3333),scope)).isEmpty());
                    }
                    int before=connections.get();var onlySun=new ModernAccountAuthentication.Principal("only-sun",
                            Set.of(new ModernAccountAuthentication.Source(7,"fixture-7")),Map.of(7,accounts.get(7)));
                    assertThrows(Exception.class,()->gate.call(5000,scope->prospective.read(onlySun,42,3333)));
                    assertEquals(before,connections.get(),"source authorization precedes a SQL connection");
                    assertNotEquals(fighters.get(0).key(),fighters.get(1).key());
                    assertThrows(IllegalArgumentException.class,()->fighters.get(0).armorProtection().select(
                            new ModernArmorProcProtection.Actor(UUID.randomUUID(),fighters.get(1).key(),1,0)));
                }finally{executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            assertEquals(original,ModernArmorProcsSqlTest.snapshot(fixture));assertEquals(0,fixture.mysql.activeConnections());
            System.err.println("ARMOR_PROTECTION_REPOSITORY_SQL_OK sources=2 sameConnection=true ownerRouting=true authority=true ordinaryRejected=true sourceUnchanged=true publicGrant=false");
            fixture.successful();
        }
    }
}
