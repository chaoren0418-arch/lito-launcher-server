package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernHasteCast.Reason.NONE;
import static local.lito381.server.ModernBattleEngineTimedTest.SECOND;
import static local.lito381.server.ModernHasteCatalog.Status.QUALIFIED;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernHasteTargetsTest {
    static ModernHasteTargets.Player key(ModernBattleObservation.Capture c,int target) {
        return new ModernHasteTargets.Player(ModernBattleHasteTest.target(c,target).participant());
    }
    static ModernSpellRuntime.Receipt receipt(ModernBattleObservation.Capture c,int caster) {
        return c.actors().get(caster).runtime().receipts().get(LANE);
    }
    static ModernBattleObservation.Capture with(ModernBattleObservation.Capture c,ModernHasteTargets.State s) {
        return new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),c.loadingOriginNanos(),
                c.actors(),c.modes(),c.npcs(),c.result(),s);
    }
    @Test void oneCasterKeepsBothVictimsAfterTheLastPerLaneReceiptHasMovedOn()throws Exception {
        try(var f=fixture()) {
            var initial=active(f);assertTrue(initial.hasteTargets().targets().isEmpty());
            var first=cast(f,0,1,1).observation();var old=receipt(first,0);f.elapsed(2*SECOND);
            var second=cast(f,0,2,0).observation();var latest=receipt(second,0);var state=second.hasteTargets();
            assertEquals(2,state.targets().size());assertEquals(old,state.targets().get(key(second,1)).active());
            assertEquals(latest,state.targets().get(key(second,0)).active());assertNotEquals(old,latest);assertEquals(80,second.actors().get(0).resources().flows().spent());
            assertDoesNotThrow(()->state.validate(second));assertEquals(second,f.engine.serviceObservation());
            assertEquals(1,first.hasteTargets().targets().size());assertThrows(UnsupportedOperationException.class,()->state.targets().clear());
            f.elapsed(1200*SECOND);var expired=f.engine.serviceObservation();assertEquals(1,expired.hasteTargets().targets().size());
            assertTrue(expired.hasteTargets().targets().containsKey(key(expired,0)));assertEquals(latest,expired.hasteTargets().targets().get(key(expired,0)).active());
            f.elapsed(1202*SECOND);assertTrue(f.engine.serviceObservation().hasteTargets().targets().isEmpty());
        }
    }
    @Test void differentCasterRefreshAtTheSameClockReplacesTheExactSourceAndCannotReuseTheOldProof()throws Exception {
        try(var f=fixture()) {
            var c=active(f);var first=cast(f,0,1,1).observation();var old=first.hasteTargets();
            var second=cast(f,1,1,1).observation();assertEquals(first.actors().get(1).buffs().hasteUntilNanos(),second.actors().get(1).buffs().hasteUntilNanos());
            assertEquals(receipt(second,1),second.hasteTargets().targets().get(key(c,1)).active());
            assertEquals(42,second.hasteTargets().targets().get(key(c,1)).active().batch().source().sourceId());
            assertThrows(IllegalArgumentException.class,()->with(second,old));
        }
    }
    @Test void otherBuffsKeepHasteProofButPotionAndGreaterHasteOverwriteIt()throws Exception {
        try(var f=fixture()) {
            active(f);var first=cast(f,0,1,0).observation();var proof=first.hasteTargets();f.elapsed(SECOND);
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40016).reason());assertEquals(proof,f.engine.serviceObservation().hasteTargets());
            f.elapsed(2*SECOND);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());var potion=f.engine.serviceObservation();
            assertTrue(potion.hasteTargets().targets().isEmpty());assertEquals(receipt(first,0),receipt(potion,0));
        }
        try(var f=new ModernBattleEngineTimedTest.Fixture(3,2,1000,1000,false,
                p->enable(ModernBattleGreaterHasteTest.enable(p,ModernGreaterHasteCatalog.Status.QUALIFIED),QUALIFIED))) {
            active(f);assertEquals(NONE,cast(f,0,1,0).reason());f.elapsed(2*SECOND);
            var r=f.engine.greaterHaste(f.players.get(0).owner(),ModernBattleGreaterHasteTest.request(f,0,2));assertEquals(ModernGreaterHasteCast.Reason.NONE,r.reason());
            assertTrue(r.observation().hasteTargets().targets().isEmpty());assertEquals(ModernTimedEffects.HasteOrigin.GREATER_HASTE,r.observation().actors().get(0).buffs().hasteOrigin());
        }
    }
    @Test void denialAndCancelledPreparationCannotInventAnEffectOrRefundAnExistingGrant()throws Exception {
        try(var f=fixture(40,QUALIFIED,QUALIFIED)) {
            active(f);var first=cast(f,0,1,1).observation();assertEquals(ModernHasteCast.Reason.TOO_FAST,cast(f,0,2,0).reason());
            assertEquals(first.hasteTargets(),f.engine.serviceObservation().hasteTargets());f.elapsed(2*SECOND);
            assertEquals(ModernHasteCast.Reason.NO_MP,cast(f,0,3,0).reason());assertEquals(first.hasteTargets(),f.engine.serviceObservation().hasteTargets());
            f.rooms.releasePreparation(f.ticket);var ended=f.engine.serviceObservation();assertTrue(ended.hasteTargets().targets().isEmpty());
            assertEquals(40,ended.actors().get(0).resources().flows().spent());assertEquals(ModernBattleEngine.Phase.ABORTED,ended.phase());
        }
    }
    @Test void missingForeignAndUnneededTargetEvidenceCannotEnterACapture()throws Exception {
        try(var f=fixture()) {
            var initial=active(f);var c=cast(f,0,1,1).observation();
            assertThrows(IllegalArgumentException.class,()->with(c,null));
            assertThrows(IllegalArgumentException.class,()->with(c,ModernHasteTargets.State.empty(c.identity())));
            assertThrows(IllegalArgumentException.class,()->with(initial,c.hasteTargets()));
            var id=c.identity();var foreign=new ModernTargetHistory.World(id.roomId(),id.matchId(),id.generation()+1);
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.State(foreign,c.hasteTargets().targets()));
            var wrong=Map.<ModernHasteTargets.Key,ModernHasteTargets.Entry>of(key(c,0),c.hasteTargets().targets().get(key(c,1)));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.State(id,wrong));
            assertThrows(IllegalArgumentException.class,()->with(c,new ModernHasteTargets.State(foreign,Map.of())));
        }
    }
    @Test void sourceScopeClockAndCurrentLedgerCannotBeBorrowedForRetainedProof()throws Exception {
        try(var f=fixture()) {
            active(f);var c=cast(f,0,1,1).observation();var r=receipt(c,0);var b=r.batch();
            var swapped=new ModernTargetHistory.Batch(b.world(),new ModernRooms.Scope(99,9999,1,0),
                    new ModernTargetHistory.Participant(1,ModernParticipantId.human(99,9999)),LANE,b.use(),List.of());
            var forged=new ModernSpellRuntime.Receipt(swapped,r.casts(),r.spent(),r.atNanos(),r.detail());
            assertThrows(IllegalArgumentException.class,()->with(c,new ModernHasteTargets.State(c.identity(),Map.of(key(c,1),new ModernHasteTargets.Entry(forged,null)))));
            var later=new ModernSpellRuntime.Receipt(b,r.casts(),r.spent(),r.atNanos()+1,r.detail());
            assertThrows(IllegalArgumentException.class,()->with(c,new ModernHasteTargets.State(c.identity(),Map.of(key(c,1),new ModernHasteTargets.Entry(later,null)))));
            var actors=new ArrayList<>(c.actors());var a=actors.get(0);var emptyRuntime=ModernSpellRuntime.State.initial(a.runtime().identity(),a.runtime().lanes(),a.runtime().birthRevision(),a.runtime().initialLawful());
            actors.set(0,new ModernBattleObservation.CapturedActor(a.pose(),a.scope(),a.loadout(),a.skills(),a.vital(),a.resources(),a.castIntervalNanos(),a.histories(),a.targets(),emptyRuntime,
                    a.supplies(),a.weight(),a.buffs(),a.ammo(),a.regeneration(),a.regenClock(),a.presentation()));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),c.loadingOriginNanos(),actors,c.modes(),c.npcs(),c.result(),c.hasteTargets()));
        }
    }
    @Test void storedEntryRejectsNormalSpellAndPlayerWalkAliases()throws Exception {
        try(var f=fixture()) {
            active(f);f.cast(0,1,4);var c=f.engine.serviceObservation();var arrow=c.actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.LIGHT_ARROW);
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.Entry(arrow,null));f.elapsed(2*SECOND);c=cast(f,0,2,1).observation();var haste=receipt(c,0);
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.Entry(null,haste));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.Entry(null,null));
        }
    }
}
