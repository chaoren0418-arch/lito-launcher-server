package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
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

class ModernCombatAuthorityTest {
    @TempDir Path directory;
    static final ModernAccountAuthentication.Principal PRINCIPAL=new ModernAccountAuthentication.Principal("sun-owner",
            Set.of(new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),
            Map.of(7,"sun-owner",42,"moon-owner"));

    @Test void sameFreshCombatProviderIsReadOnceAndProfileAdmissionNeverWritesOverrideFile() throws Exception {
        try(var h=new Harness(directory)) {
            var profile=h.read(PRINCIPAL,h.qualified.source().character());
            assertEquals(1,h.reads.get());assertEquals(0,h.legacyReads.get());assertTrue(h.wasAdmitted);
            assertEquals(ModernCombatProfile.WeaponState.QUALIFIED,profile.weapon().state());
            assertEquals(0,profile.equipmentRevision());assertFalse(profile.attackImplemented());
            assertEquals(0,h.records());assertFalse(h.lease.hasActivePermit());
        }
    }

    @Test void oldAuthorityConstructorCannotGuessWeaponFactsFromLegacyFixture() throws Exception {
        try(var h=new Harness(directory)) {
            h.authority=new ModernEquipmentAuthority(h.oldProvider,h.store,()->1000L);
            var failure=assertThrows(IOException.class,()->h.read(PRINCIPAL,h.qualified.source().character()));
            assertEquals("combat source qualification unavailable",failure.getMessage());assertEquals(0,h.legacyReads.get());
            assertEquals(0,h.reads.get());assertEquals(0,h.records());
        }
    }

    @Test void combatProviderWithUnqualifiedWeaponCannotPassAsACombatQualifiedSource() throws Exception {
        try(var h=new Harness(directory)) {
            h.input.set(ModernCombatProfile.Input.unqualified(h.qualified));
            assertThrows(IOException.class,()->h.read(PRINCIPAL,h.qualified.source().character()));
            assertFalse(h.wasAdmitted);assertEquals(0,h.records());
        }
    }

    @Test void sameObjectIdOtherSourceIsRejectedEvenWhenRosterAndInventoryAreIdentical() throws Exception {
        try(var h=new Harness(directory)) {
            var old=h.qualified;var catalog=old.catalog();
            var otherSource=new ModernCharacterData.Snapshot(42,old.source().character(),old.source().persisted(),old.source().inventory());
            var other=new Qualified(otherSource,old.items(),new Catalog(42,catalog.resourceDatabase(),catalog.lookups(),catalog.verifiedSetIds(),
                    catalog.sets(),catalog.combos(),catalog.vipTableState()),ModernItemLifetime.Observation.verifiedPermanent(otherSource));
            h.input.set(new ModernCombatProfile.Input(other,true,h.input.get().weapons()));
            assertThrows(IOException.class,()->h.read(PRINCIPAL,old.source().character()));assertFalse(h.wasAdmitted);
        }
    }

    @Test void freshLevelNameClassSexMustMatchSelectedRowNotOnlyObjectId() throws Exception {
        for(int variant=0;variant<4;variant++)try(var h=new Harness(directory.resolve("changed"+variant))) {
            var row=h.qualified.source().character();
            var selected=new ModernApplicationProtocol.CharacterRow(row.objectId(),variant==0?"changed":row.name(),
                    variant==1?1:row.characterClass(),variant==2?1:row.sex(),variant==3?10:row.level());
            assertThrows(IOException.class,()->h.read(PRINCIPAL,selected));assertFalse(h.wasAdmitted);assertEquals(0,h.records());
        }
    }

    @Test void unauthorizedSourceIsRejectedBeforeAnyProviderAndHasNoPrimaryFallback() throws Exception {
        try(var h=new Harness(directory)) {
            var principal=new ModernAccountAuthentication.Principal("moon-owner",Set.of(new ModernAccountAuthentication.Source(42,"Moon")),Map.of(42,"moon-owner"));
            assertThrows(SecurityException.class,()->h.read(principal,h.qualified.source().character()));assertEquals(0,h.reads.get());
        }
    }

    @Test void leaseInvalidationInsideSlowProviderPreventsLatePublicationEvenWithValidRows() throws Exception {
        try(var h=new Harness(directory)) {
            h.beforeReturn=h.lease::invalidate;
            var error=assertThrows(IOException.class,()->h.read(PRINCIPAL,h.qualified.source().character()));
            assertEquals("combat preparation retired before publication",error.getMessage());assertFalse(h.wasAdmitted);
            assertTrue(h.lease.isRetired());assertEquals(0,h.records());
        }
    }

    @Test void scopeCancellationAfterSourceReadCannotReachStoreOrPublication() throws Exception {
        try(var h=new Harness(directory)) {
            h.beforeReturn=()->ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
            var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->h.read(PRINCIPAL,h.qualified.source().character()));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());assertFalse(h.wasAdmitted);assertEquals(0,h.records());
        }
    }

    @Test void storedOverrideReconciliationIsReadOnlyAndBindingMismatchNeverFallsBackToSource() throws Exception {
        try(var h=new Harness(directory)) {
            h.storeOverride(false,BINDING);
            var profile=h.read(PRINCIPAL,h.qualified.source().character());
            assertEquals(1,profile.equipmentRevision());assertEquals(ModernCombatProfile.WeaponState.UNARMED,profile.weapon().state());
            assertEquals(1,h.records());
        }
        try(var h=new Harness(directory.resolve("wrong-binding"))) {
            byte[] other=new byte[32];other[0]=1;
            h.storeOverride(false,new ModernEquipmentOverrideStore.Fingerprint(other));
            assertThrows(IOException.class,()->h.read(PRINCIPAL,h.qualified.source().character()));assertFalse(h.wasAdmitted);assertEquals(1,h.records());
        }
    }

    @Test void actualBackendWorkOwnsPublicationPinUntilFinallyAndReturnsOnlyAfterCleanup() throws Exception {
        try(var h=new Harness(directory)) {
            var executor=new ModernBackendExecutor(1,0);
            try(var gate=executor.openSession()) {
                AtomicReference<ModernEquipmentLease.Operation> operation=new AtomicReference<>();
                var result=gate.call(3000,new ModernBackendExecutor.Work<ModernCombatProfile>() {
                    @Override public ModernCombatProfile run(ModernBackendScope scope) throws Exception {
                        var op=h.lease.operation(scope);operation.set(op);
                        var profile=h.authority.prepareCombat(PRINCIPAL,h.qualified.source().character(),op,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
                        assertTrue(op.isCommitAdmitted());h.lease.invalidate();assertFalse(h.lease.isRetired());return profile;
                    }
                    @Override public void close() {if(operation.get()!=null)operation.get().close();}
                });
                assertTrue(h.lease.isRetired());assertFalse(h.lease.hasActivePermit());assertNotNull(result);assertEquals(0,h.records());
            } finally {executor.close();assertTrue(executor.awaitTermination(3000));}
        }
    }

    @Test void arithmeticFailureBeforeAdmissionDoesNotProduceFileOrSuccessfulProfile() throws Exception {
        try(var h=new Harness(directory)) {
            var item=simpleArmor(1,1,bonus(Stat.MAX_HP,Long.MAX_VALUE));
            var q=qualified(List.of(item));h.input.set(new ModernCombatProfile.Input(q,true,Map.of()));
            assertThrows(ArithmeticException.class,()->h.read(PRINCIPAL,q.source().character()));assertFalse(h.wasAdmitted);assertEquals(0,h.records());
        }
    }

    static final class Harness implements AutoCloseable {
        final Path directory;
        final ModernEquipmentOverrideStore store;
        final Qualified qualified=qualified(List.of(ModernCombatProfileTest.weapon(1,1,true)));
        final AtomicReference<ModernCombatProfile.Input> input=new AtomicReference<>(new ModernCombatProfile.Input(qualified,true,
                Map.of(1L,ModernCombatProfileTest.facts(1,1,1))));
        final AtomicInteger reads=new AtomicInteger(),legacyReads=new AtomicInteger();
        final TournamentPresence.CharacterKey key=new TournamentPresence.CharacterKey(7,3333);
        final ModernEquipmentLease lease=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});
        final ModernEquipmentAuthority.Provider oldProvider=(principal,id,obj)->{legacyReads.incrementAndGet();return new ModernEquipmentAuthority.Loaded(qualified,BINDING);};
        ModernEquipmentAuthority authority;
        Runnable beforeReturn=()->{};
        boolean wasAdmitted;
        Harness(Path path)throws Exception {
            directory=path;Files.createDirectories(path);store=new ModernEquipmentOverrideStore(path);
            authority=new ModernEquipmentAuthority(oldProvider,(principal,id,obj)->{
                assertEquals("sun-owner",principal.accountForSource(id));assertEquals(7,id);assertEquals(3333,obj);reads.incrementAndGet();
                beforeReturn.run();return new ModernEquipmentAuthority.CombatLoaded(input.get(),BINDING);
            },store,()->1000L);
        }
        ModernCombatProfile read(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected)throws Exception {
            return ModernEquipmentSourceJdbcTest.scoped(()->{
                try(var operation=lease.operation(ModernBackendScope.current())) {
                    var result=authority.prepareCombat(principal,selected,operation,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
                    wasAdmitted=operation.isCommitAdmitted();return result;
                }
            });
        }
        void storeOverride(boolean equipped,ModernEquipmentOverrideStore.Fingerprint owner)throws Exception {
            ModernEquipmentSourceJdbcTest.scoped(()->{
                var scope=ModernBackendScope.current();
                var writer=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});
                try(writer;var op=writer.operation(scope)) {
                    var item=qualified.items().get(1L);
                    store.compareAndSet(key,ModernEquipmentOverrideStore.Expected.absent(),new ModernEquipmentOverrideStore.Replacement(owner,
                            List.of(new ModernEquipmentOverrideStore.ItemOverride(1,1,ModernEquipmentReconciliation.fingerprint(item),equipped))),scope,op);
                }
                return null;
            });
        }
        long records()throws Exception {try(var files=Files.list(directory)){return files.filter(path->path.toString().endsWith(".meo")).count();}}
        @Override public void close(){lease.close();store.close();assertTrue(store.isQuiescent());}
    }
}
