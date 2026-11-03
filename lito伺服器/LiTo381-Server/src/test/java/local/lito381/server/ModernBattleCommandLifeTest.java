package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleCommandLife.Rejection.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real engine death/respawn and typed commands. No SQL, sockets, GUI, sleep,
 * invented HP or reflection. Wire/worker integration is deliberately not
 * claimed by these in-process tests. */
class ModernBattleCommandLifeTest {
    private static final long SECOND=1_000_000_000L;
    static ModernBattleCommandLife.Proof proof(ModernBattleEngine engine,int index) {
        var c=engine.serviceObservation();var a=c.actors().get(index);
        return new ModernBattleCommandLife.Proof(a.scope(),c.identity(),a.pose().entityId(),
                a.resources()==null?1:Math.incrementExact(a.resources().life()),c.revision());
    }
    private static ModernBattleEngineHonorTest.Fixture honor()throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(100,1,100,SECOND);
    }
    @Test void legacyRequestReallyRemainsValidAcrossRespawnAndMustNotBeCalledLifeProtected()throws Exception {
        try(var f=honor()) {
            f.active();var queued=f.castRequest(1,1,5,0);var old=proof(f.engine,1);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            f.elapsed(5*SECOND);var revived=f.engine.tick();
            assertEquals(2,proof(f.engine,1).life());assertEquals(1,old.life());
            assertEquals(0,revived.casts().get(1).requestSequence());
            var legacy=f.engine.cast(f.players.get(1).owner(),queued);
            assertEquals(ModernSpellProtocol.Reason.NONE,legacy.reason(),
                    "explicit characterization of the still-unguarded legacy wire path");
            assertEquals(1,legacy.snapshot().casts().get(1).castCount());
        }
    }
    @Test void queuedOldLifeCannotInvokeCommandAndFreshLifeKeepsExistingTypedAck()throws Exception {
        try(var f=honor()) {
            f.active();var old=proof(f.engine,1);var queued=f.castRequest(1,1,5,0);var owner=f.players.get(1).owner();
            f.cast(0,1,4,2);f.elapsed(5*SECOND);var invoked=new AtomicInteger();
            var rejected=f.engine.observeLifeCommand(owner,old,()->{invoked.incrementAndGet();return f.engine.cast(owner,queued);});
            assertEquals(STALE_LIFE,rejected.rejection());assertNull(rejected.accepted());assertEquals(0,invoked.get());
            var before=f.engine.tick();assertEquals(0,before.casts().get(1).requestSequence());assertEquals(100,before.vitals().get(1).mp());
            assertEquals(1,rejected.observation().actors().get(1).resources().life());
            var accepted=f.engine.observeLifeCommand(owner,proof(f.engine,1),()->f.engine.cast(owner,queued));
            assertEquals(NONE,accepted.rejection());assertEquals(ModernSpellProtocol.Reason.NONE,accepted.accepted().outcome().reason());
            assertSame(accepted.observation(),accepted.accepted().outcome().observation());
            assertEquals(1,accepted.observation().actors().get(1).runtime().sequence());
        }
    }
    @Test void sourceOwnerEpochChannelRoomGenerationEntityAndFutureRevisionCannotBorrowProof()throws Exception {
        try(var f=honor()) {
            f.active();var p=proof(f.engine,0);var s=p.player();var w=p.world();var owner=f.players.get(0).owner();
            var forged=List.of(
                new ModernBattleCommandLife.Proof(f.players.get(1).scope(),w,p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch()+1,s.channelId()),w,p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch(),1),w,p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId()+1,w.matchId(),w.generation()),p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId(),w.matchId()+1,w.generation()),p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(s,new ModernTargetHistory.World(w.roomId(),w.matchId(),w.generation()+1),p.entity(),p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(s,w,2,p.life(),p.revision()),
                new ModernBattleCommandLife.Proof(s,w,p.entity(),p.life(),p.revision()+1));
            var before=f.engine.serviceObservation();
            for(var bad:forged)assertThrows(SecurityException.class,()->f.engine.observeLifeCommand(owner,bad,()->{fail("foreign proof executed");return f.engine.serviceObservation();}));
            assertThrows(SecurityException.class,()->f.engine.observeLifeCommand(f.players.get(1).owner(),p,()->f.engine.serviceObservation()));
            assertThrows(SecurityException.class,()->f.engine.observeLifeCommand(owner,p,()->f.cast(1,1,5,0)),"callback cannot borrow a different authenticated actor");
            assertThrows(SecurityException.class,()->f.engine.observeLifeCommand(owner,p,()->f.engine.serviceObservation()),"a read is not a typed actor mutation");
            assertEquals(before,f.engine.serviceObservation());assertEquals(0,f.draws);
        }
    }
    @Test void loadingDeadAndCancelledAdmissionsNeverExecuteOrConsumeSequence()throws Exception {
        try(var f=honor()) {
            var owner=f.players.get(1).owner();var loading=proof(f.engine,1);
            assertEquals(NOT_ACTIVE,f.engine.observeLifeCommand(owner,loading,()->{fail("loading");return f.engine.serviceObservation();}).rejection());
            f.active();var old=proof(f.engine,1);f.cast(0,1,4,2);
            assertEquals(DEAD,f.engine.observeLifeCommand(owner,old,()->{fail("dead");return f.engine.serviceObservation();}).rejection());
            assertEquals(0,f.engine.tick().casts().get(1).requestSequence());
            f.rooms.releasePreparation(f.ticket);
            assertEquals(NOT_ACTIVE,f.engine.observeLifeCommand(owner,old,()->{fail("cancelled");return f.engine.serviceObservation();}).rejection());
        }
    }
    @Test void sameHpAndCoordinatesAfterDeathRespawnInsideResolverDoNotHideChangedLife()throws Exception {
        for(boolean revive:List.of(false,true))try(var f=honor()) {
            f.active();var p=proof(f.engine,1);var owner=f.players.get(1).owner();var before=f.engine.tick();
            f.randomOverride=bound->{
                f.randomOverride=null;
                assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
                if(revive)f.elapsed(5*SECOND);
                return bound-1;
            };
            var result=f.engine.observeLifeCommand(owner,p,()->f.cast(1,1,5,0));
            assertEquals(revive?STALE_LIFE:DEAD,result.rejection());assertNull(result.accepted());
            var after=f.engine.tick();
            assertEquals(before.actors().get(1).position(),after.actors().get(1).position());
            assertEquals(revive?1:0,after.vitals().get(1).hp());assertEquals(100,after.vitals().get(1).mp());
            assertEquals(before.casts().get(1),after.casts().get(1));
            assertEquals(1,after.damages().get(0).castCount(),"retain the real independent killing command");
            f.elapsed(6*SECOND);
            assertEquals(NONE,f.engine.observeLifeCommand(owner,proof(f.engine,1),()->f.cast(1,1,5,0)).rejection(),"fence released after rejection");
        }
    }
    @Test void questConfirmationChangesLifeWithoutErasingAttackSequenceAndOldCommandIsRejected()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();var p=proof(f.engine,0);var owner=f.players.get(0).owner();
            var dead=ModernQuestLifeTest.die(f,100);assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            var target=ModernQuestEngineTest.living(f,0);var before=f.view();
            var stale=f.engine.observeLifeCommand(owner,p,()->f.attack(0,1,target));
            assertEquals(STALE_LIFE,stale.rejection());assertEquals(before,f.view());
            var fresh=f.engine.observeLifeCommand(owner,proof(f.engine,0),()->f.attack(0,1,target));
            assertEquals(NONE,fresh.rejection());assertEquals(1,fresh.observation().actors().get(0).vital().attackSequence());
        }
    }
    @Test void failedOrNestedCallbackCannotLeakFenceIntoLaterCommands()throws Exception {
        try(var f=honor()) {
            f.active();var p=proof(f.engine,0);var owner=f.players.get(0).owner();
            assertThrows(UnsupportedOperationException.class,()->f.engine.observeLifeCommand(owner,p,()->{throw new UnsupportedOperationException("fixture");}));
            assertThrows(IllegalStateException.class,()->f.engine.observeLifeCommand(owner,p,()->{
                f.engine.observeLifeCommand(owner,p,()->f.cast(0,1,5,0));return f.engine.serviceObservation();
            }));
            var result=f.engine.observeLifeCommand(owner,p,()->f.cast(0,1,5,0));
            assertEquals(NONE,result.rejection());assertEquals(ModernSpellProtocol.Reason.NONE,result.accepted().outcome().reason());
            assertThrows(SecurityException.class,()->f.engine.observeLifeCommand(owner,p,()->f.cast(0,1,5,0)),"life admission cannot bypass typed duplicate sequence");
        }
    }
    @Test void malformedProofAndResultAreBoundedAndDiagnosticTextIsRedacted()throws Exception {
        try(var f=honor()) {
            var p=proof(f.engine,0);
            for(long n:new long[]{0,-1,101,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->new ModernBattleCommandLife.Proof(p.player(),p.world(),n,1,1));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleCommandLife.Proof(p.player(),p.world(),1,0,1));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleCommandLife.Proof(p.player(),p.world(),1,1,0));
            assertEquals("BattleCommandLife[redacted]",p.toString());
            assertThrows(IllegalArgumentException.class,()->ModernBattleCommandLife.Result.rejected(NONE,f.engine.serviceObservation()));
        }
    }
    @Test void siegeCampRespawnRejectsPriorLifeButNewLifeCanUseTheSameUnusedMoveSequence()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1234,0,true)) {
            f.active();f.walk(1,ModernBattleEngineSiegeTest.cell(31,11));f.waitMillis(200);f.portal(1);
            f.walk(1,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);var p=proof(f.engine,1);
            var dead=f.attack(0,1,2).snapshot();assertEquals(0,dead.vitals().get(1).hp());
            f.waitMillis(1234);var before=f.tick();var actor=before.actors().get(1);var owner=f.players.get(1).owner();
            assertEquals(dead.actors().get(1).lastMoveSequence(),actor.lastMoveSequence());
            var seq=actor.lastMoveSequence()+1;var s=f.players.get(1).scope();
            java.util.function.Supplier<ModernBattleEngine.MoveResult> move=()->f.engine.move(owner,s,f.ticket.matchId(),f.ticket.generation(),seq,actor.position().x(),actor.position().y(),4);
            assertEquals(STALE_LIFE,f.engine.observeLifeCommand(owner,p,move).rejection());assertEquals(before,f.tick());
            var fresh=f.engine.observeLifeCommand(owner,proof(f.engine,1),move);
            assertEquals(NONE,fresh.rejection());assertEquals(ModernBattleEngine.MoveStatus.MOVED,fresh.accepted().outcome().status());
        }
    }
    @Test void acceptedItemAndAmmoKeepTheirOwnSequenceStockAndUnsupportedDenials()throws Exception {
        try(var f=honor()) {
            f.active();var owner=f.players.get(0).owner();var p=proof(f.engine,0);var s=f.players.get(0).scope();
            var request=new ModernItemProtocol.Request(s,ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40010);
            var result=f.engine.observeLifeCommand(owner,p,()->f.engine.item(owner,request));
            assertEquals(NONE,result.rejection());assertEquals(ModernItemProtocol.Reason.NONE,result.accepted().outcome().reason());
            assertEquals(1,result.accepted().outcome().snapshot().items().get(0).requestSequence());
            assertEquals(1,result.accepted().outcome().snapshot().items().get(0).useCount());
            assertEquals(0,result.accepted().outcome().snapshot().items().get(0).restored(),"full HP does not fabricate recovery");
        }
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var p=proof(f.engine,0);var owner=f.prepared.get(0).owner();
            var selected=f.engine.observeLifeCommand(owner,p,()->f.select(1,40746));
            assertEquals(NONE,selected.rejection());assertEquals(ModernAmmoProtocol.Reason.NONE,selected.accepted().outcome().reason());
            var denied=f.engine.observeLifeCommand(owner,p,()->f.select(2,40010));
            assertEquals(NONE,denied.rejection());assertEquals(ModernAmmoProtocol.Reason.UNSUPPORTED,denied.accepted().outcome().reason());
            assertEquals(selected.accepted().outcome().snapshot().ammo().get(0).stocks(),denied.accepted().outcome().snapshot().ammo().get(0).stocks());
            assertEquals(2,denied.accepted().outcome().snapshot().ammo().get(0).requestSequence());
        }
    }
}
