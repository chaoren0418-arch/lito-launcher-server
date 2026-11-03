package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernAmmoProtocolTest {
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot source) {
        var world=new ModernWorldProtocol.Frame(source.matchId(),source.generation(),source.revision()+1,
                ModernWorldProtocol.Phase.valueOf(source.phase().name()),ModernWorldProtocol.AbortReason.valueOf(source.abortReason().name()),
                source.room(),source.manifest(),source.actors(),source.result()==null?null:new ModernWorldProtocol.Completion(source.result(),ModernWorldProtocol.SettlementState.PENDING),
                source.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var vitals=source.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),
                v.attackSequence(),event(v.attackRevision()),event(v.hurtRevision()),event(v.deathRevision()),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,vitals,source.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),
                source.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),source.weights(),
                source.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList(),source.ammo().stream().map(ModernAmmo.State::wireRevision).toList());
    }
    private static long event(long n){return n==0?0:n+1;}
    static ModernApplicationProtocol.AmmoResult result(ModernBattleEngineAmmoTest.Fixture f,long seq,long item,ModernBattleEngine.AmmoResult reply) {
        return new ModernApplicationProtocol.AmmoResult(1,f.request(seq,item),new ModernAmmoProtocol.Reply(
                reply.reason()==ModernAmmoProtocol.Reason.NONE?ModernAmmoProtocol.Status.OK:ModernAmmoProtocol.Status.DENIED,reply.reason(),frame(reply.snapshot())));
    }
    static void roundtrip(ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,11);assertEquals(message,ModernApplicationProtocol.decode(bytes,11));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),11));
    }
    private static void assertReadPreservesHistory(ModernBattleEngineAmmoTest.Fixture f,ModernBattleEngine.Snapshot confirmed)throws Exception {
        var player=f.prepared.get(0);
        var request=new ModernAmmoProtocol.Request(player.scope(),ModernAmmoProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0);
        var read=f.engine.ammo(player.owner(),request);
        assertEquals(ModernAmmoProtocol.Reason.NONE,read.reason());assertEquals(confirmed,read.snapshot());
        assertEquals(1,read.snapshot().ammo().get(0).shotCount());
        roundtrip(new ModernApplicationProtocol.AmmoResult(3,request,new ModernAmmoProtocol.Reply(
                ModernAmmoProtocol.Status.OK,read.reason(),frame(read.snapshot()))));
        assertEquals(read,f.engine.ammo(player.owner(),request));
        var invalidRead=new ModernApplicationProtocol.AmmoResult(4,request,new ModernAmmoProtocol.Reply(
                ModernAmmoProtocol.Status.DENIED,ModernAmmoProtocol.Reason.NOT_ACTIVE,frame(confirmed)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(invalidRead,11));
        var invalidSelect=new ModernApplicationProtocol.AmmoResult(5,f.request(99,40746),new ModernAmmoProtocol.Reply(
                ModernAmmoProtocol.Status.BUSY,ModernAmmoProtocol.Reason.NOT_ACTIVE,frame(confirmed)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(invalidSelect,11));
        assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,f.select(99,40746).reason());
        assertEquals(confirmed,f.engine.tick());
    }
    @Test void finishedAmmoReadIsOkAndKeepsEntireShotLedgerAfterRoomRelease()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture(1,2,10000,true,0)) {
            f.active();var finished=f.attack(1).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,finished.phase());
            f.room.rooms.releasePreparation(f.ticket);assertReadPreservesHistory(f,finished);
        }
    }
    @Test void abortedAmmoReadIsOkIncludingCancelledTicketGuardFallback()throws Exception {
        for(boolean cancelled:new boolean[]{false,true})try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var shot=f.attack(1).snapshot();
            if(cancelled)f.room.rooms.releasePreparation(f.ticket);else f.engine.abort();
            var aborted=f.engine.tick();assertEquals(ModernBattleEngine.Phase.ABORTED,aborted.phase());
            assertEquals(shot.ammo(),aborted.ammo());assertReadPreservesHistory(f,aborted);
        }
    }
    @Test void inactiveSelectionRepliesRoundtripWithoutInventingAcceptedSequence()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture(0,2,7,false,3_000_000_000L)) {
            var before=f.engine.tick();roundtrip(result(f,99,40746,f.select(99,40746)));assertEquals(before,f.engine.tick());
            f.active();var countdown=f.engine.tick();roundtrip(result(f,99,40746,f.select(99,40746)));assertEquals(countdown,f.engine.tick());
            f.clock.addAndGet(65_000_000_000L);var terminal=f.engine.tick();roundtrip(result(f,99,40746,f.select(99,40746)));assertEquals(terminal,f.engine.tick());
        }
    }
    @Test void successfulAndDeniedSelectionHaveExactTypedProofAndCannotBeRelabelled()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var denied=result(f,1,40010,f.select(1,40010));roundtrip(denied);
            for(var reason:List.of(ModernAmmoProtocol.Reason.EMPTY,ModernAmmoProtocol.Reason.DEAD,ModernAmmoProtocol.Reason.NOT_ACTIVE)) {
                var forged=new ModernApplicationProtocol.AmmoResult(1,denied.echo(),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.DENIED,reason,denied.reply().frame()));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(forged,11));
            }
            var selected=result(f,2,40746,f.select(2,40746));roundtrip(selected);
            var wrongSequence=new ModernApplicationProtocol.AmmoResult(1,f.request(3,40746),selected.reply());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(wrongSequence,11));
            var noAmmo=new ModernApplicationProtocol.AmmoResult(1,selected.echo(),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.DENIED,ModernAmmoProtocol.Reason.EMPTY,selected.reply().frame()));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(noAmmo,11));
        }
    }
    @Test void hitMissAndNoAmmoFramesCarryConservedLedgersThroughAllCodecs()throws Exception {
        for(boolean miss:new boolean[]{false,true})try(var f=new ModernBattleEngineAmmoTest.Fixture(0,1,7,false,0)) {
            f.miss=miss;f.active();var shot=f.attack(1);var frame=frame(shot.snapshot());var scope=f.prepared.get(0).scope();
            roundtrip(new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.ATTACK,f.ticket.matchId(),f.ticket.generation(),1,2),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame)));
            roundtrip(new ModernApplicationProtocol.AmmoResult(2,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,frame)));
            f.clock.addAndGet(1_000_000_000L);var empty=f.attack(2);
            roundtrip(new ModernApplicationProtocol.CombatResult(3,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.ATTACK,f.ticket.matchId(),f.ticket.generation(),2,2),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.NO_AMMO,frame(empty.snapshot()))));
            roundtrip(result(f,1,40744,f.select(1,40744)));
        }
    }
    @Test void sourceCollisionDoesNotAuthorizeOtherOwnerAndFullRosterCannotBeDropped()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var snap=frame(f.engine.tick());assertEquals(snap.world().actors().get(0).member().key().objectId(),snap.world().actors().get(1).member().key().objectId());
            assertNotEquals(snap.world().actors().get(0).member().key(),snap.world().actors().get(1).member().key());
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(snap.world(),snap.vitals(),snap.damages(),snap.items(),snap.weights(),snap.buffs(),List.of(snap.ammo().get(0))));
            var spoof=new ModernRooms.Scope(8,1,1,0);var req=new ModernAmmoProtocol.Request(spoof,ModernAmmoProtocol.Action.READ,f.ticket.matchId(),f.ticket.generation(),0,0);
            assertThrows(SecurityException.class,()->f.engine.ammo(f.prepared.get(0).owner(),req));
        }
    }
}
