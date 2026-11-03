package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual source authority, durable override store and engine. Not a wire/UI grant. */
class ModernArmorProtectionEquipmentTest {
    @TempDir Path root;
    static final long SECOND=1_000_000_000L;
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source seed) {
        var s=ModernGreaterHasteEquipmentTest.enoughMp(seed);var q=s.input().equipment();var p=q.source().persisted();
        var saved=new ModernCharacterData.PersistedValues(10000,p.maxMp(),10000,p.savedMp(),p.armorClass(),p.strength(),p.dexterity(),
                p.constitution(),p.intelligence(),p.wisdom(),p.charisma(),p.experience(),p.lawful(),p.originalStrength(),p.originalDexterity(),
                p.originalConstitution(),p.originalIntelligence(),p.originalWisdom(),p.food225());
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),saved,q.source().inventory());
        var equipment=new ModernEquipmentFacts.Qualified(row,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(row));
        return ModernArmorProtectionLoadoutTest.protection(new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(equipment,true,s.input().weapons()),
                s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth()));
    }
    ModernBattleCanonicalEquipmentTest.Fixture fixture(FileOps files)throws Exception {
        return new ModernBattleCanonicalEquipmentTest.Fixture(root,files,ModernBattleEngine.PreparedMember::withPlayerHealing,
                ModernArmorProtectionEquipmentTest::source,true);
    }
    static ModernBattleEngine.ArmorProtectionView state(ModernBattleCanonicalEquipmentTest.Fixture f){return f.world.engine.armorProtectionSnapshot().get(1L);}
    static void hit(ModernBattleCanonicalEquipmentTest.Fixture f,long seq){assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.world.attack(1,seq,1).status());}
    static void active(ModernBattleCanonicalEquipmentTest.Fixture f){ModernGreaterHasteEquipmentTest.active(f);}
    static ModernArmorProcProtection.Selection selection(ModernBattleCanonicalEquipmentTest.Fixture f) {
        return f.capture().actors().get(0).loadout().fighter().armorProtection().select(state(f).state().actor());
    }
    @Test void sourceAuthorityUsesSameBoundBundleAndReloadsDurableRevisionForNextMatch()throws Exception {
        try(var f=fixture(new FileOps(){})) {
            var first=f.call(op->f.authority.preparePhysical(f.principal,f.player().row(),op));
            assertTrue(first.supported());assertNotNull(first.armorProtection());assertEquals(0,first.profile().equipmentRevision());
            assertTrue(f.stored().isEmpty());var gear=f.read();active(f);var changed=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
            var next=f.call(op->f.authority.preparePhysical(f.principal,f.player().row(),op));
            assertEquals(1,next.profile().equipmentRevision());assertTrue(next.profile().equipped().stream().noneMatch(Equipped::equipped));
            assertEquals(ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH,next.profile().vitalPolicy());
            assertTrue(next.armorProtection().select(state(f).state().actor()).rules().isEmpty());
            assertEquals(1,first.armorProtection().select(state(f).state().actor()).rules().size(),"old immutable preparation retained");
        }
    }
    @Test void durableUnequipPreservesExistingLeaseButStopsNewTriggersAndReequipDoesNotRefreshIt()throws Exception {
        try(var f=fixture(new FileOps(){})) {
            var gear=f.read();active(f);hit(f,1);var initial=state(f);assertTrue(initial.state().active(0));
            var before=f.capture().actors().get(0);var off=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,off.publication().disposition());assertEquals(initial,state(f));
            assertEquals(before.runtime(),f.capture().actors().get(0).runtime());assertTrue(selection(f).rules().isEmpty());
            assertEquals(1,selection(f).equipmentRevision());f.world.elapsed(SECOND);hit(f,2);
            assertTrue(state(f).triggers().isEmpty());assertEquals(initial.state().untilNanos(),state(f).state().untilNanos());
            var reequipped=f.set(off.equipment().snapshot(),true);assertEquals(2,selection(f).equipmentRevision());
            assertEquals(1,selection(f).rules().size());assertEquals(initial.state().untilNanos(),state(f).state().untilNanos());
            f.world.elapsed(3*SECOND);assertEquals(0,state(f).state().untilNanos());hit(f,3);
            assertEquals(6*SECOND,state(f).state().untilNanos());assertTrue(state(f).triggers().get(0).appliedProtection());
            assertEquals(2,reequipped.publication().equipmentRevision());assertEquals(2,f.stored().orElseThrow().revision());
        }
    }
    @Test void triggerOrUnusedStageCatalogDriftRejectsEvenNoopWithoutWritingOrReplacingLiveState()throws Exception {
        try(var f=fixture(new FileOps(){})) {
            var gear=f.read();active(f);hit(f,1);var old=f.sources.get(0);var before=f.capture();var armor=state(f);
            long id=old.armorProcs().byItem().keySet().iterator().next();var rule=old.armorProcs().byItem().get(id).get(0);
            var changed=new ModernArmorProcs.Rule(rule.id(),id,-1,88,0,50,0,0,0,0,0,0,0,68,3,1,11101,1);
            var unused=new ModernArmorProcs.Rule(999999,id,99,88,0,100,0,0,0,0,0,0,0,68,9,1,11101,1);
            for(var rules:List.of(List.of(changed),List.of(rule,unused))) {
                f.sources.set(0,ModernArmorProtectionLoadoutTest.withRules(old,Set.of(),Map.of(id,rules)));
                assertThrows(IllegalStateException.class,()->f.set(gear,true));assertEquals(before,f.capture());assertEquals(armor,state(f));
                assertTrue(f.stored().isEmpty());
            }
            f.sources.set(0,old);assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,f.set(gear,true).publication().disposition());
        }
    }
    @Test void missingNewProofCannotFallBackToOrdinaryProviderOrEnableProtectionOnOldActor()throws Exception {
        try(var f=fixture(new FileOps(){})) {
            var gear=f.read();active(f);var s=f.sources.get(0);var before=f.capture();
            var ordinary=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("second provider read");},null,
                    (p,id,obj)->new PhysicalLoaded(s,binding(s.input().equipment()).fingerprint()),f.store,f.world.epoch::get);
            assertThrows(IllegalStateException.class,()->f.call(op->ordinary.executeInMatch(f.principal,f.player().row(),op,
                    new Request(7,3333,1,SET,false,1,gear.revision(),gear.viewToken()),f.reserve(f.proof()))));
            assertEquals(before,f.capture());assertTrue(f.stored().isEmpty());
        }
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(java.nio.file.Files.createDirectory(root.resolve("ordinary")),new FileOps(){})) {
            var gear=f.read();active(f);var s=ModernArmorProtectionLoadoutTest.protection(f.sources.get(0));var before=f.capture();
            var injected=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("second provider read");},null,
                    (p,id,obj)->new PhysicalLoaded(s,binding(s.input().equipment()).fingerprint(),ModernArmorProtectionLoadoutTest.boundInScope(s)),f.store,f.world.epoch::get);
            assertThrows(IllegalStateException.class,()->f.call(op->injected.executeInMatch(f.principal,f.player().row(),op,
                    new Request(7,3333,1,SET,false,1,gear.revision(),gear.viewToken()),f.reserve(f.proof()))));
            assertEquals(before,f.capture());assertTrue(f.stored().isEmpty());
        }
    }
    @Test void separateButEqualPhysicalBundleCannotBorrowCatalogCertificate()throws Exception {
        var s=source(ModernBattleLoadoutTest.source(7));var bound=ModernArmorProtectionLoadoutTest.bound(s);
        var copy=new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),s.weaponProcCatalogIds());
        assertEquals(s,copy);assertNotSame(s,copy);
        assertThrows(IllegalArgumentException.class,()->new PhysicalLoaded(copy,binding(s.input().equipment()).fingerprint(),bound));
    }
    @Test void cancellationOrRetiredSelectionAfterArmorQualificationCannotPublishOrWrite()throws Exception {
        for(boolean cancel:List.of(false,true))try(var h=new ModernPhysicalCombatAuthorityTest.Harness(root.resolve("scope-"+cancel))) {
            h.input.set(source(ModernBattleEngineHonorTest.source(7,h.row,1000,1000,0)));
            h.authority=new ModernEquipmentAuthority(h.provider,h.combat,(p,id,obj)->{
                h.reads.incrementAndGet();var s=h.input.get();var bound=ModernArmorProtectionLoadoutTest.boundInScope(s);
                if(cancel)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);else h.lease.invalidate();
                return new PhysicalLoaded(s,ModernCombatProfileTest.BINDING,bound);
            },h.store,()->1000L);
            assertThrows(Exception.class,()->h.read(ModernCombatAuthorityTest.PRINCIPAL,h.row));
            assertEquals(1,h.reads.get());assertEquals(0,h.otherReads.get());assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }
    @Test void damageAndNewProcDuringDurableIoSurviveGearPublicationWithoutRestoringCapturedVitals()throws Exception {
        var ref=new AtomicReference<ModernBattleCanonicalEquipmentTest.Fixture>();
        var armor=new AtomicReference<ModernBattleEngine.ArmorProtectionView>();
        var observation=new AtomicReference<ModernBattleObservation.Capture>();
        try(var f=fixture(new FileOps(){@Override public void force(FileChannel channel)throws IOException {
            FileOps.super.force(channel);ModernBattleCanonicalEquipmentTest.onOtherThread(()->{
                var x=ref.get();hit(x,1);armor.set(state(x));observation.set(x.capture());return null;});
        }})) {
            ref.set(f);var gear=f.read();active(f);var committed=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,committed.publication().disposition());assertEquals(armor.get(),state(f));
            var a=observation.get().actors().get(0).resources();var b=f.capture().actors().get(0).resources();
            assertTrue(a.flows().damaged()>0);assertEquals(a.flows().damaged(),b.flows().damaged());assertTrue(b.vitals().hp()<=a.vitals().hp());
            assertTrue(selection(f).rules().isEmpty());assertEquals(1,f.stored().orElseThrow().revision());
        }
    }
    @Test void failedForceDoesNotChangeGearProtectionOrVitalsAndAllowsRetry()throws Exception {
        var fail=new java.util.concurrent.atomic.AtomicBoolean(true);
        try(var f=fixture(new FileOps(){@Override public void force(FileChannel channel)throws IOException {
            if(fail.getAndSet(false))throw new IOException("fixture failure");FileOps.super.force(channel);
        }})) {
            var gear=f.read();active(f);hit(f,1);var before=f.capture();var armor=state(f);var rejected=f.set(gear,false);
            assertEquals(BACKEND_UNAVAILABLE,rejected.equipment().status());assertNull(rejected.publication());
            assertEquals(before,f.capture());assertEquals(armor,state(f));assertTrue(f.stored().isEmpty());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,f.set(gear,false).publication().disposition());
        }
    }
    @Test void expiryDuringRenameIsNotUndoneByPublishingNewGear()throws Exception {
        var ref=new AtomicReference<ModernBattleCanonicalEquipmentTest.Fixture>();
        try(var f=fixture(new FileOps(){@Override public void move(Path from,Path to)throws IOException {
            FileOps.super.move(from,to);ModernBattleCanonicalEquipmentTest.onOtherThread(()->{ref.get().world.elapsed(3*SECOND);return null;});
        }})) {
            ref.set(f);var gear=f.read();active(f);hit(f,1);assertTrue(state(f).state().active(0));var result=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,result.publication().disposition());
            assertEquals(0,state(f).state().untilNanos());assertTrue(state(f).triggers().isEmpty());assertEquals(1,selection(f).equipmentRevision());
            assertEquals(1,f.stored().orElseThrow().revision());
        }
    }
    @Test void abortAfterRenamePreservesDurableGearWithoutRevivingOldProtection()throws Exception {
        var ref=new AtomicReference<ModernBattleCanonicalEquipmentTest.Fixture>();
        try(var f=fixture(new FileOps(){@Override public void move(Path from,Path to)throws IOException {
            FileOps.super.move(from,to);ModernBattleCanonicalEquipmentTest.onOtherThread(()->{ref.get().world.engine.abortObservation();return null;});
        }})) {
            ref.set(f);var gear=f.read();active(f);hit(f,1);var result=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,result.publication().disposition());
            assertEquals(BACKEND_UNAVAILABLE,result.equipment().status());assertEquals(1,f.stored().orElseThrow().revision());
            assertEquals(0,state(f).state().untilNanos());assertTrue(state(f).triggers().isEmpty());
            assertEquals(0,f.capture().actors().get(0).resources().equipmentRevision());
        }
    }
}
