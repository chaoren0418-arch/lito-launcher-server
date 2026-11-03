package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Actual room capabilities and MOVE commits, with a deterministic monotonic clock. */
class ModernBattleEnginePetRaceTest {
    static final long STEP=100_000_000L;
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=rooms.player(7,3333,80,0),b=rooms.player(42,3333,80,0);
        final ModernWorldMap map=ModernPetRaceTest.map();
        final ModernWorldBinding binding=new ModernWorldBinding(0,565,map);
        final ModernPetRace.Course course;
        final ModernRooms.FrozenPreparation ticket;
        final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(-10_000_000_000L);
        final ModernBattleEngine.Limits limits;
        long activeStart;
        Fixture()throws Exception{this(30_000,0);}
        Fixture(int activeMillis,long countdown)throws Exception{
            limits=new ModernBattleEngine.Limits(2000,countdown,activeMillis);
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,0,565,0)));
            ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
            for(var p:List.of(a,b))ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var s=rooms.view(a).joined().summary();var capture=rooms.rooms.prepareWorld(a.owner(),a.scope(),s.id(),s.revision());
            ModernRoomsTest.accepted(capture.result());ticket=capture.ticket();
            var spawns=List.of(ModernPetRaceTest.MIDDLE,ModernPetRaceTest.SECOND);
            course=ModernPetRace.qualify(binding.key(),map,map.manifest(),ModernPetRaceTest.gates(),2145,STEP,spawns,()->false);
            var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<ticket.participants().size();i++){var p=ticket.participants().get(i);var m=p.member();
                prepared.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),new ModernApplicationProtocol.CharacterRow(m.key().objectId(),m.name(),m.characterClass(),m.sex(),m.level()),spawns.get(i),STEP));}
            engine=new ModernBattleEngine(ticket,binding,prepared,List.of(),limits,clock::get,()->1000,
                    bound->{throw new AssertionError("Race does not borrow combat RNG");},false,null,()->false,course);
        }
        ModernBattleEngine.Snapshot active(){
            for(var p:List.of(a,b))engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());
            clock.addAndGet(limits.countdownNanos());activeStart=clock.get();return engine.tick();
        }
        ModernBattleEngine.MoveResult move(int who,int heading){var p=who==0?a:b;var actor=engine.tick().actors().get(who);
            return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),actor.lastMoveSequence()+1,actor.position().x(),actor.position().y(),heading);}
        ModernBattleEngine.Snapshot step(int who,ModernWorldMap.Cell to){
            clock.addAndGet(STEP);var actor=engine.tick().actors().get(who);var from=actor.position();
            int dx=Integer.signum(to.x()-from.x()),dy=Integer.signum(to.y()-from.y());int heading=dx==0?(dy<0?0:4):dy==0?(dx>0?2:6):dx>0?(dy<0?1:3):(dy>0?5:7);
            var result=move(who,heading);assertEquals(ModernBattleEngine.MoveStatus.MOVED,result.status());return result.snapshot();
        }
        ModernBattleEngine.Snapshot finish(int who){
            while(engine.tick().race().runners().get(who).placement()==0){
                var actor=engine.tick().actors().get(who);var runner=engine.tick().race().runners().get(who);
                if(!actor.position().equals(ModernPetRaceTest.MIDDLE))step(who,ModernPetRaceTest.MIDDLE);
                step(who,runner.nextCheckpoint()==0?ModernPetRaceTest.FIRST_GATE:ModernPetRaceTest.LAST_GATE);
            }return engine.tick();
        }
        void elapsed(long n){clock.set(activeStart+n);}
        ModernWorldProtocol.Frame frame(){return frame(engine.tick());}
        ModernWorldProtocol.Frame frame(ModernBattleEngine.Snapshot s){
            return new ModernWorldProtocol.Frame(s.matchId(),s.generation(),s.revision()+1,ModernWorldProtocol.Phase.valueOf(s.phase().name()),
                    ModernWorldProtocol.AbortReason.valueOf(s.abortReason().name()),s.room(),s.manifest(),s.actors(),null,List.of(),
                    new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PET_RACE,1024,2000,limits.loadTimeoutMillis(),limits.countdownNanos(),limits.activeTimeoutMillis()),s.race());
        }
        public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
    }
    @Test void loadingAndCountdownRejectMovesWithoutConsumingSequenceOrInitialRaceReceipt()throws Exception{
        try(var f=new Fixture(30_000,1_000_000_000L)){
            assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE,f.move(0,0).status());
            assertEquals(0,f.engine.tick().race().runners().get(0).progressMoveSequence());assertEquals(0,f.engine.tick().actors().get(0).lastMoveSequence());ModernPetRaceProtocolTest.roundtrip(f.frame(),f.a.scope());
            for(var p:List.of(f.a,f.b))f.engine.ackLoaded(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
            var countdown=f.engine.tick();assertEquals(ModernBattleEngine.Phase.COUNTDOWN,countdown.phase());assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE,f.move(0,0).status());assertEquals(countdown,f.engine.tick());ModernPetRaceProtocolTest.roundtrip(f.frame(),f.a.scope());
            f.clock.addAndGet(f.limits.countdownNanos());f.activeStart=f.clock.get();f.engine.tick();var entered=f.step(0,ModernPetRaceTest.FIRST_GATE);assertEquals(1,entered.race().runners().get(0).progressMoveSequence());assertEquals(1,entered.race().runners().get(0).nextCheckpoint());f.frame();
        }
    }
    @Test void fullFourLapsAreAtomicWithMoveAndFixedWindowDoesNotFinishEarly()throws Exception{
        try(var f=new Fixture()){f.active();var first=f.finish(0);assertEquals(ModernBattleEngine.Phase.ACTIVE,first.phase());assertNull(first.result());assertTrue(first.vitals().isEmpty());
            var runner=first.race().runners().get(0);assertEquals(4,runner.completedLaps());assertEquals(1,runner.placement());assertEquals(first.revision()+1,runner.revision());
            assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE,f.move(0,0).status());assertEquals(first,f.engine.tick());ModernPetRaceProtocolTest.roundtrip(f.frame(),f.a.scope());
            long deadline=first.race().finishWindowDeadlineElapsedNanos();f.elapsed(deadline-1);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.engine.tick().phase());
            f.elapsed(deadline);var done=f.engine.tick();assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());assertEquals(ModernPetRaceProtocol.Settlement.PENDING,done.race().settlement());
            assertEquals(ModernPetRace.Dnf.WINDOW_CLOSED,done.race().runners().get(1).dnf());assertEquals(done.revision()+1,done.race().terminalRevision());
            var event=ModernPetRaceSettlement.event(f.frame(done),1000);assertEquals(List.of(7,42),event.rows().stream().map(r->r.source().sourceId()).toList());
            assertTrue(event.rows().stream().allMatch(r->r.source().objectId()==3333));f.elapsed(deadline+100_000_000_000L);assertEquals(done,f.engine.tick());
        }
    }
    @Test void rejectedCollisionCadenceReplayAndCrossGenerationNeverAdvanceCheckpoint()throws Exception{
        try(var f=new Fixture()){f.active();var rejected=f.move(0,2);assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,rejected.status());assertEquals(0,rejected.snapshot().race().runners().get(0).nextCheckpoint());
            var first=f.step(0,ModernPetRaceTest.FIRST_GATE);var receipt=first.race();assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,4).status());assertEquals(receipt,f.engine.tick().race());
            var actor=f.engine.tick().actors().get(0);assertThrows(SecurityException.class,()->f.engine.move(f.a.owner(),f.a.scope(),f.ticket.matchId(),f.ticket.generation(),actor.lastMoveSequence(),actor.position().x(),actor.position().y(),4));
            assertThrows(SecurityException.class,()->f.engine.move(f.a.owner(),f.a.scope(),f.ticket.matchId(),f.ticket.generation()+1,actor.lastMoveSequence()+1,actor.position().x(),actor.position().y(),4));assertEquals(receipt,f.engine.tick().race());
        }
    }
    @Test void individualLeftOrDisconnectedStopsOnlyThatActorAndRemovesOnlyItsOccupancy()throws Exception{
        for(var reason:List.of(ModernPetRace.Dnf.LEFT,ModernPetRace.Dnf.DISCONNECTED))try(var f=new Fixture()){
            f.active();var retired=f.engine.withdrawRacer(f.b.owner(),reason);assertEquals(ModernBattleEngine.Phase.ACTIVE,retired.phase());assertEquals(reason,retired.race().runners().get(1).dnf());assertEquals(1,f.engine.ownerCount());
            var actor=retired.actors().get(1);assertThrows(SecurityException.class,()->f.engine.move(f.b.owner(),f.b.scope(),f.ticket.matchId(),f.ticket.generation(),actor.lastMoveSequence()+1,actor.position().x(),actor.position().y(),0));
            assertEquals(retired,f.engine.tick());ModernPetRaceProtocolTest.roundtrip(f.frame(retired),f.a.scope());
            f.rooms.rooms.raceParticipantWithdrawn(f.b.owner(),f.ticket);f.rooms.rooms.disconnect(f.b.owner());assertTrue(f.ticket.isLive());
            assertEquals(ModernPetRaceTest.SECOND,f.step(0,ModernPetRaceTest.SECOND).actors().get(0).position());assertEquals(2,f.frame().actors().size());
            assertThrows(SecurityException.class,()->f.engine.snapshot(f.b.owner(),f.b.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void lastOwnersMayDisappearAfterWinnerButTheFixedWindowAndRankedResultSurvive()throws Exception{
        try(var f=new Fixture()){f.active();var first=f.finish(0);long deadline=first.race().finishWindowDeadlineElapsedNanos();
            for(var p:List.of(f.a,f.b)){f.engine.withdrawRacer(p.owner(),ModernPetRace.Dnf.DISCONNECTED);f.rooms.rooms.raceParticipantWithdrawn(p.owner(),f.ticket);f.rooms.rooms.disconnect(p.owner());}
            assertEquals(0,f.engine.ownerCount());assertEquals(0,f.rooms.rooms.roomCount());assertTrue(f.ticket.isLive());
            f.elapsed(deadline-1);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.engine.tick().phase());f.elapsed(deadline);assertEquals(ModernBattleEngine.Phase.FINISHED,f.engine.tick().phase());f.frame();
        }
    }
    @Test void deadlineOrderAndPreactiveDepartureNeverInventAResult()throws Exception{
        try(var f=new Fixture(5000,0)){f.active();var first=f.finish(0);assertTrue(first.race().finishWindowDeadlineElapsedNanos()>5_000_000_000L);
            f.elapsed(100_000_000_000L);var done=f.engine.tick();assertEquals(ModernBattleEngine.Phase.ABORTED,done.phase());assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,done.abortReason());assertFalse(done.race().complete());f.frame();}
        try(var f=new Fixture()){f.rooms.rooms.disconnect(f.b.owner());assertEquals(ModernBattleEngine.Phase.ABORTED,f.engine.tick().phase());assertNull(f.engine.tick().result());f.frame();}
    }
    @Test void bothFinishersRetainTheirPlacesWithoutClosingTheWindowEarly()throws Exception{
        try(var f=new Fixture()){f.active();var first=f.finish(0);long deadline=first.race().finishWindowDeadlineElapsedNanos();var second=f.finish(1);
            assertEquals(ModernBattleEngine.Phase.ACTIVE,second.phase());assertEquals(List.of(1L,2L),second.race().runners().stream().map(ModernPetRace.Runner::placement).toList());assertEquals(deadline,second.race().finishWindowDeadlineElapsedNanos());
            var frozen=second.race().runners();f.elapsed(deadline);assertEquals(frozen,f.engine.tick().race().runners());f.frame();}
    }
}
