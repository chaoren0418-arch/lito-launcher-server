package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleVipCommandState.Rejection.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real engine admission and isolated durable CAS. This is not the production
 * equipment authority, a network SET receipt, SQL mutation or UI qualification. */
class ModernBattleVipCommandStateTest {
    @TempDir Path directory;
    static ModernBattleVipCommandState.Proof proof(ModernBattleEngineVipTest.Fixture f,int index) {
        return ModernBattleVipCommandState.from(ModernBattleVipObservation.from(f.view()).forOwner(f.players.get(index).scope()));
    }
    static ModernBattleVipCommandState.EquipmentAdmission admit(ModernBattleEngineVipTest.Fixture f,int index,
            ModernBattleVipCommandState.Proof proof) {
        return f.engine.admitVipEquipmentChange(f.players.get(index).owner(),proof);
    }
    static ModernBattleVipObservation.Versions versions(long saved,long runtime,long sequence) {
        return new ModernBattleVipObservation.Versions(saved,runtime,sequence);
    }
    static void rejected(ModernBattleVipCommandState.Rejection reason,ModernBattleVipCommandState.EquipmentAdmission out) {
        assertEquals(reason,out.rejection());assertNull(out.change());assertNotNull(out.observation());
    }
    @Test void proofUsesOneQualifiedOwnerCaptureAndNeverReplacesOldVersions()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            var capture=f.active();var view=ModernBattleVipObservation.from(capture).forOwner(f.players.get(0).scope());
            var old=ModernBattleVipCommandState.from(view);
            assertEquals(f.players.get(0).scope(),old.life().player());assertEquals(capture.identity(),old.life().world());
            assertEquals(1,old.life().entity());assertEquals(1,old.life().life());assertEquals(capture.revision(),old.life().revision());
            assertEquals(versions(0,0,0),old.versions());
            f.epoch.set(2000);var fresh=proof(f,0);
            assertEquals(versions(0,1,1),fresh.versions());assertEquals(versions(0,0,0),old.versions());
            assertEquals(old,ModernBattleVipCommandState.from(view));assertEquals("VipCommandState[redacted]",old.toString());
        }
    }
    @Test void oldLifeOnlyAdmissionCannotReserveVipEvenWhenAllVersionsEqual()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();
            for(long now:List.of(1000L,2000L)) {
                f.epoch.set(now);var p=proof(f,0);
                var old=f.engine.admitCanonicalEquipmentChange(f.players.get(0).owner(),p.life());
                assertEquals(ModernBattleEquipmentProtocol.Reason.UNAVAILABLE,old.reason());assertNull(old.change());
                assertThrows(IllegalStateException.class,()->f.engine.beginCanonicalEquipmentChange(f.players.get(0).owner(),p.life()));
                try(var current=admit(f,0,p).change()){assertNotNull(current);current.confirmUnchanged();}
            }
        }
    }
    @Test void expiryIsAdvancedBeforeComparingProofAndRecoveryDoesNotInventSave()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();var old=proof(f,0);f.epoch.set(2000);
            var out=admit(f,0,old);rejected(STALE_RUNTIME_ABILITY,out);
            var recovery=ModernBattleVipObservation.from(out.observation()).forOwner(f.players.get(0).scope());
            assertEquals(versions(0,1,1),recovery.personal().equipment().versions());
            assertTrue(out.observation().actors().get(0).loadout().fighter().vip().state().stored().isEmpty());
            assertEquals(0,out.observation().actors().get(0).vital().attackSequence());
            try(var change=admit(f,0,ModernBattleVipCommandState.from(recovery)).change()) {
                assertNotNull(change);var receipt=change.confirmUnchanged();
                assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,receipt.disposition());
                assertEquals(0,receipt.equipmentRevision());assertEquals(1,receipt.resources().balance().equipmentRevision());
                assertEquals(out.observation(),change.observation());
            }
        }
    }
    @Test void eachVersionRejectsIndependentlyWithoutHoldingReservation()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();f.epoch.set(2000);var p=proof(f,0);var before=f.view();
            var bad=List.of(versions(1,1,1),versions(0,0,1),versions(0,2,1),versions(0,1,0),versions(0,1,Long.MAX_VALUE));
            var reasons=List.of(STALE_SAVED_EQUIPMENT,STALE_RUNTIME_ABILITY,STALE_RUNTIME_ABILITY,STALE_LIFECYCLE,STALE_LIFECYCLE);
            for(int i=0;i<bad.size();i++) {
                var out=admit(f,0,new ModernBattleVipCommandState.Proof(p.life(),bad.get(i)));rejected(reasons.get(i),out);
                assertEquals(before,out.observation());
                try(var change=admit(f,0,p).change()){assertNotNull(change);}
            }
            assertEquals(before,f.view());
            assertEquals(NONE,ModernBattleVipCommandState.compare(versions(0,0,Long.MAX_VALUE),versions(0,0,Long.MAX_VALUE)),
                    "lifecycle is not bounded by the saved/runtime version");
        }
    }
    @Test void sourceOwnerEpochChannelWorldEntityAndFutureRevisionRemainAuthenticated()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            f.active();var p=proof(f,0);var life=p.life();var s=life.player();var w=life.world();var before=f.view();
            var forged=List.of(
                new ModernBattleCommandLife.Proof(f.players.get(1).scope(),w,1,1,life.revision()),
                new ModernBattleCommandLife.Proof(new ModernRooms.Scope(s.sourceId(),s.objectId()+1,s.epoch(),s.channelId()),w,1,1,life.revision()),
                new ModernBattleCommandLife.Proof(new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch()+1,s.channelId()),w,1,1,life.revision()),
                new ModernBattleCommandLife.Proof(new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch(),1),w,1,1,life.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId()+1,w.matchId(),w.generation()),1,1,life.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId(),w.matchId()+1,w.generation()),1,1,life.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId(),w.matchId(),w.generation()+1),1,1,life.revision()),
                new ModernBattleCommandLife.Proof(s,w,2,1,life.revision()),
                new ModernBattleCommandLife.Proof(s,w,1,1,life.revision()+1));
            for(var bad:forged)assertThrows(SecurityException.class,()->admit(f,0,new ModernBattleVipCommandState.Proof(bad,p.versions())));
            assertThrows(SecurityException.class,()->admit(f,1,p));assertEquals(before,f.view());
            try(var change=admit(f,0,p).change()){assertNotNull(change);}
        }
    }
    @Test void loadingStaleLifeDeadAndAbortedNeverReserve()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,-99L))) {
            rejected(NOT_ACTIVE,admit(f,0,proof(f,0)));f.active();var p=proof(f,0);var life=p.life();
            rejected(STALE_LIFE,admit(f,0,new ModernBattleVipCommandState.Proof(new ModernBattleCommandLife.Proof(
                    life.player(),life.world(),life.entity(),life.life()+1,life.revision()),p.versions())));
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1,1,1).status());rejected(DEAD,admit(f,0,p));
            f.engine.abortObservation();rejected(NOT_ACTIVE,admit(f,1,proof(f,1)));
        }
    }
    @Test void vipProofCannotOptLegacyWorldIntoNewMutationPath()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();var life=ModernBattleCommandLifeTest.proof(f.engine,0);var p=new ModernBattleVipCommandState.Proof(life,versions(0,0,0));
            var before=f.engine.serviceObservation();rejected(UNAVAILABLE,f.engine.admitVipEquipmentChange(f.players.get(0).owner(),p));
            assertEquals(before,f.engine.serviceObservation());
            try(var change=f.engine.beginCanonicalEquipmentChange(f.players.get(0).owner(),life)){assertNotNull(change);}
        }
    }
    @Test void unrelatedWorldAdvanceDoesNotInvalidateOwnerVersionsAndBusyIsReleased()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            f.active();var p=proof(f,0);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1,1,1).status());
            assertTrue(f.view().revision()>p.life().revision());
            var accepted=admit(f,0,p);assertEquals(NONE,accepted.rejection());
            try(var change=accepted.change()) {
                rejected(BUSY,admit(f,0,p));assertEquals("VipEquipmentAdmission[redacted]",accepted.toString());
                change.captureNoMutation();assertNull(change.publication());assertEquals(f.view(),change.observation());
            }
            try(var change=admit(f,0,p).change()){assertNotNull(change);}
        }
    }
    @Test void postExpiryRealCasKeepsSavedOneRuntimeTwoAndRejectsReplayedProof()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L));var store=new ModernEquipmentOverrideStore(directory)) {
            f.active();f.epoch.set(2000);var before=proof(f,0);
            try(var change=admit(f,0,before).change()) {
                var q=f.sources.get(0).input().equipment();
                var fresh=ModernVipLoadout.qualifyForRebuild(f.sources.get(0),ModernCombatProfileTest.binding(q),
                        ModernCombatProfileTest.stored(q,1,Map.of(3L,true)),2000,0);
                var candidate=ModernVipCharacterState.prepareOrdinaryEquipment(change.vipState(),fresh,2000);change.prepareVip(candidate,2000);
                var commit=ModernBattleEngineVipTest.commit(store,change,candidate);var receipt=change.publish(commit);
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,receipt.disposition());assertEquals(1,receipt.equipmentRevision());
                assertEquals(2,receipt.resources().balance().equipmentRevision());
                var observed=ModernBattleVipObservation.from(change.observation()).forOwner(f.players.get(0).scope());
                assertEquals(versions(1,2,2),observed.personal().equipment().versions());
                assertEquals(change.observation(),f.view());assertEquals(100,observed.personal().history().resources().vitals().hp());
                try(var call=new ModernEquipmentOverrideStoreTest.Call(commit.stored().key())) {
                    assertEquals(commit.stored(),store.read(call.lease.key(),call.scope).orElseThrow());
                }
            }
            rejected(STALE_SAVED_EQUIPMENT,admit(f,0,before));
            try(var change=admit(f,0,proof(f,0)).change()){assertNotNull(change);}
            assertEquals(versions(0,1,1),proof(f,1).versions());
        }
    }
    @Test void expiryBetweenReservationAndDurableCasCannotUseRefreshedVersions()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L));var store=new ModernEquipmentOverrideStore(directory)) {
            f.active();var p=proof(f,0);
            try(var change=admit(f,0,p).change()) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                f.epoch.set(2000);
                assertEquals(ModernEquipmentOverrideStore.Failure.INVALID_OPERATION,assertThrows(ModernEquipmentOverrideStore.StoreException.class,
                        ()->ModernBattleEngineVipTest.commit(store,change,candidate)).failure());
                change.captureNoMutation();assertNull(change.publication());assertEquals(ModernBattleEngine.Phase.ACTIVE,change.observation().phase());
                try(var call=new ModernEquipmentOverrideStoreTest.Call(candidate.nextStore().orElseThrow().key())) {
                    assertTrue(store.read(call.lease.key(),call.scope).isEmpty());
                }
            }
            rejected(STALE_RUNTIME_ABILITY,admit(f,0,p));
            try(var fresh=admit(f,0,proof(f,0)).change()){assertNotNull(fresh);}
        }
    }
    @Test void damageDuringDurableRenameRetainsLiveHpAndExactPublicationCapture()throws Exception {
        var during=new AtomicReference<ModernBattleObservation.Capture>();
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            f.active();
            try(var change=admit(f,0,proof(f,0)).change();var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws IOException {
                    var attack=f.attack(1,1,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,attack.status());during.set(attack.observation());
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            })) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var commit=ModernBattleEngineVipTest.commit(store,change,candidate);var receipt=change.publish(commit);
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,receipt.disposition());
                var before=during.get().actors().get(0).resources();var after=change.observation().actors().get(0).resources();
                assertTrue(before.vitals().hp()<200);assertEquals(Math.min(100,before.vitals().hp()),after.vitals().hp());
                assertEquals(before.flows().damaged(),after.flows().damaged());assertEquals(f.view(),change.observation());
            }
        }
    }
    @Test void expiryAfterDurableRenameKeepsSavedEvidenceWithoutInventingLiveSuccess()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();
            try(var change=admit(f,0,proof(f,0)).change();var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws IOException {
                    f.epoch.set(2000);f.view();ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            })) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var commit=ModernBattleEngineVipTest.commit(store,change,candidate);var receipt=change.publish(commit);
                assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,receipt.disposition());assertNull(receipt.resources());
                assertEquals(1,receipt.equipmentRevision());assertEquals(ModernBattleEngine.Phase.ABORTED,change.observation().phase());
                var recovery=ModernBattleVipObservation.from(change.observation()).forOwner(f.players.get(0).scope());
                assertEquals(versions(0,1,1),recovery.personal().equipment().versions());
                try(var call=new ModernEquipmentOverrideStoreTest.Call(commit.stored().key())) {
                    assertEquals(commit.stored(),store.read(call.lease.key(),call.scope).orElseThrow());
                }
            }
        }
    }
    @Test void malformedProofAndAdmissionDoNotHavePermissiveDefaults()throws Exception {
        assertThrows(IllegalArgumentException.class,()->versions(-1,0,0));assertThrows(IllegalArgumentException.class,()->versions(1,0,0));
        assertThrows(IllegalArgumentException.class,()->versions(0,0,-1));
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            f.active();var p=proof(f,0);
            assertThrows(NullPointerException.class,()->new ModernBattleVipCommandState.Proof(null,p.versions()));
            assertThrows(NullPointerException.class,()->new ModernBattleVipCommandState.Proof(p.life(),null));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleVipCommandState.EquipmentAdmission(NONE,null,f.view()));
            try(var change=admit(f,0,p).change()) {
                assertThrows(IllegalArgumentException.class,()->new ModernBattleVipCommandState.EquipmentAdmission(BUSY,change,f.view()));
            }
        }
    }
    @Test void fullVipReservationCannotBeHandedToLegacyEquipmentQualification()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            f.active();var before=f.view();
            try(var change=admit(f,0,proof(f,0)).change()) {
                var source=f.sources.get(0);var q=source.input().equipment();var binding=ModernCombatProfileTest.binding(q);
                var saved=ModernCombatProfileTest.stored(q,1,Map.of(3L,true)).orElseThrow();
                assertThrows(IllegalStateException.class,()->change.requireSource(source,binding,java.util.Optional.empty(),1000));
                assertThrows(IllegalStateException.class,()->change.prepare(source,binding,saved,1000));
                assertFalse(change.admit());assertEquals(before,f.view());
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);
                assertDoesNotThrow(()->change.prepareVip(candidate,1000));
            }
            assertEquals(before,f.view());
        }
    }
}
