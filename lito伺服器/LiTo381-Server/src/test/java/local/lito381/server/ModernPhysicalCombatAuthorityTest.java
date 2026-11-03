package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernCombatAuthorityTest.PRINCIPAL;
import static local.lito381.server.ModernCombatProfileTest.BINDING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernPhysicalCombatAuthorityTest {
    @TempDir Path directory;

    @Test void oneFreshPhysicalBundlePublishesWithoutOldProviderReadsOrOverrideWrites() throws Exception {
        try(var h=new Harness(directory)) {
            var result=h.read(PRINCIPAL,h.row);
            assertTrue(result.supported());assertEquals(ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH,result.profile().vitalPolicy());
            assertEquals(100,result.profile().effective().initialHp());assertEquals(1,h.reads.get());assertEquals(0,h.otherReads.get());
            assertTrue(h.admitted);assertFalse(h.lease.hasActivePermit());assertEquals(0,h.records());
        }
    }

    @Test void oldCombatConstructorCannotInferNewGrowthOrProcProof() throws Exception {
        try(var h=new Harness(directory)) {
            h.authority=new ModernEquipmentAuthority(h.provider,h.combat,h.store);
            var failure=assertThrows(IOException.class,()->h.read(PRINCIPAL,h.row));
            assertEquals("physical source qualification unavailable",failure.getMessage());assertEquals(0,h.reads.get());assertEquals(0,h.otherReads.get());
        }
    }

    @Test void differentSourceSameObjectRowAndItemsNeverProduceHybridFighter() throws Exception {
        try(var h=new Harness(directory)) {
            h.input.set(ModernPhysicalCombatTest.fixtureSource(42,h.row,100,10,1));
            assertThrows(IOException.class,()->h.read(PRINCIPAL,h.row));assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }

    @Test void completeFreshCharacterIdentityMustMatchSelectedIncludingLevel() throws Exception {
        for(int changed=0;changed<4;changed++)try(var h=new Harness(directory.resolve("identity"+changed))) {
            var row=new ModernApplicationProtocol.CharacterRow(3333,changed==0?"different":h.row.name(),changed==1?1:0,changed==2?1:0,changed==3?31:30);
            assertThrows(IOException.class,()->h.read(PRINCIPAL,row));assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }

    @Test void unauthorizedSourceAndWrongObjectStopBeforeAnyPhysicalProvider() throws Exception {
        try(var h=new Harness(directory)) {
            var moon=new ModernAccountAuthentication.Principal("moon",Set.of(new ModernAccountAuthentication.Source(42,"Moon")),Map.of(42,"moon"));
            assertThrows(SecurityException.class,()->h.read(moon,h.row));
            assertThrows(SecurityException.class,()->h.read(PRINCIPAL,new ModernApplicationProtocol.CharacterRow(3334,h.row.name(),0,0,30)));
            assertEquals(0,h.reads.get());
        }
    }

    @Test void lateProviderAfterSelectionRetirementCannotAdmitPhysicalOutput() throws Exception {
        try(var h=new Harness(directory)) {
            h.beforeReturn=h.lease::invalidate;
            var failure=assertThrows(IOException.class,()->h.read(PRINCIPAL,h.row));
            assertEquals("physical preparation retired before publication",failure.getMessage());assertFalse(h.admitted);
            assertTrue(h.lease.isRetired());assertEquals(0,h.records());
        }
    }

    @Test void cancellationAfterQualifiedReadWinsOverPublicationAndNoStoreWrite() throws Exception {
        try(var h=new Harness(directory)) {
            h.beforeReturn=()->ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->h.read(PRINCIPAL,h.row));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }

    @Test void retainedDesiredOverrideFeedsSamePhysicalInputAndOwnerMismatchDoesNotFallBack() throws Exception {
        try(var h=new Harness(directory)) {
            h.storeOverride(BINDING);var result=h.read(PRINCIPAL,h.row);
            assertEquals(1,result.profile().equipmentRevision());assertEquals(ModernCombatProfile.WeaponState.UNARMED,result.profile().weapon().state());
            assertEquals(1,h.records());assertTrue(result.supported());
        }
        try(var h=new Harness(directory.resolve("wrong-owner"))) {
            byte[] other=new byte[32];other[0]=1;h.storeOverride(new ModernEquipmentOverrideStore.Fingerprint(other));
            assertThrows(IOException.class,()->h.read(PRINCIPAL,h.row));assertFalse(h.admitted);assertEquals(1,h.records());
        }
    }

    @Test void unsupportedWeaponRemainsExplicitEvenThoughItsFreshSourceIsFullyQualified() throws Exception {
        try(var h=new Harness(directory)) {
            h.input.set(ModernPhysicalCombatTest.fixtureSource(7,h.row,100,10,4));
            var result=h.read(PRINCIPAL,h.row);assertFalse(result.supported());
            assertEquals(ModernPhysicalCombat.UnsupportedReason.RANGED,result.unsupportedReason());assertEquals(0,h.records());
        }
    }

    @Test void actualBackendWorkKeepsPublicationPinUntilItsRealCleanup() throws Exception {
        try(var h=new Harness(directory)) {
            var executor=new ModernBackendExecutor(1,0);
            try(var gate=executor.openSession()) {
                var operation=new AtomicReference<ModernEquipmentLease.Operation>();
                var result=gate.call(3000,new ModernBackendExecutor.Work<ModernPhysicalCombat.Fighter>() {
                    @Override public ModernPhysicalCombat.Fighter run(ModernBackendScope scope)throws Exception {
                        var op=h.lease.operation(scope);operation.set(op);var fighter=h.authority.preparePhysical(PRINCIPAL,h.row,op);
                        assertTrue(op.isCommitAdmitted());h.lease.invalidate();assertFalse(h.lease.isRetired());return fighter;
                    }
                    @Override public void close(){if(operation.get()!=null)operation.get().close();}
                });
                assertTrue(result.supported());assertTrue(h.lease.isRetired());assertFalse(h.lease.hasActivePermit());assertEquals(0,h.records());
            }finally{executor.close();assertTrue(executor.awaitTermination(3000));}
        }
    }

    @Test void arithmeticFailureDuringQualificationCannotAdmitOrWrite() throws Exception {
        try(var h=new Harness(directory)) {
            h.input.set(ModernPhysicalCombatTest.fixtureSource(7,h.row,100,Long.MIN_VALUE,1));
            var error=assertThrows(ModernPhysicalCombat.CombatException.class,()->h.read(PRINCIPAL,h.row));
            assertEquals(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW,error.failure());assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }

    static final class Harness implements AutoCloseable {
        final Path directory;final ModernEquipmentOverrideStore store;
        final ModernApplicationProtocol.CharacterRow row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",0,0,30);
        final AtomicReference<ModernPhysicalCombat.Source> input=new AtomicReference<>(ModernPhysicalCombatTest.fixtureSource(7,row,100,10,1));
        final AtomicInteger reads=new AtomicInteger(),otherReads=new AtomicInteger();
        final TournamentPresence.CharacterKey key=new TournamentPresence.CharacterKey(7,3333);
        final ModernEquipmentLease lease=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});
        final ModernEquipmentAuthority.Provider provider=(p,id,obj)->{otherReads.incrementAndGet();return new ModernEquipmentAuthority.Loaded(input.get().input().equipment(),BINDING);};
        final ModernEquipmentAuthority.CombatProvider combat=(p,id,obj)->{otherReads.incrementAndGet();return new ModernEquipmentAuthority.CombatLoaded(input.get().input(),BINDING);};
        ModernEquipmentAuthority authority;Runnable beforeReturn=()->{};boolean admitted;
        Harness(Path path)throws Exception {
            directory=path;Files.createDirectories(path);store=new ModernEquipmentOverrideStore(path);
            authority=new ModernEquipmentAuthority(provider,combat,(p,id,obj)->{
                assertEquals("sun-owner",p.accountForSource(id));assertEquals(7,id);assertEquals(3333,obj);
                reads.incrementAndGet();beforeReturn.run();return new ModernEquipmentAuthority.PhysicalLoaded(input.get(),BINDING);
            },store,()->1000L);
        }
        ModernPhysicalCombat.Fighter read(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected)throws Exception {
            return ModernEquipmentSourceJdbcTest.scoped(()->{
                try(var op=lease.operation(ModernBackendScope.current())) {
                    var result=authority.preparePhysical(principal,selected,op);admitted=op.isCommitAdmitted();return result;
                }
            });
        }
        void storeOverride(ModernEquipmentOverrideStore.Fingerprint owner)throws Exception {
            ModernEquipmentSourceJdbcTest.scoped(()->{
                var scope=ModernBackendScope.current();var writer=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});
                try(writer;var op=writer.operation(scope)) {
                    var item=input.get().input().equipment().items().get(1L);
                    store.compareAndSet(key,ModernEquipmentOverrideStore.Expected.absent(),new ModernEquipmentOverrideStore.Replacement(owner,
                            List.of(new ModernEquipmentOverrideStore.ItemOverride(1,1,ModernEquipmentReconciliation.fingerprint(item),false))),scope,op);
                }return null;
            });
        }
        long records()throws Exception{try(var files=Files.list(directory)){return files.filter(p->p.toString().endsWith(".meo")).count();}}
        @Override public void close(){lease.close();store.close();assertTrue(store.isQuiescent());}
    }
}
