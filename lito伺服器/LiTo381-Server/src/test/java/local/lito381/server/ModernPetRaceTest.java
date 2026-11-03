package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernPetRaceTest {
    static final ModernWorldMap.SectorKey SECTOR=new ModernWorldMap.SectorKey(0x8000,0x8000);
    static final ModernWorldBinding.Key KEY=new ModernWorldBinding.Key(0,565);
    static final ModernWorldMap.Cell MIDDLE=cell(4,5),SECOND=cell(5,5),FIRST_GATE=cell(4,4),LAST_GATE=cell(4,6);
    static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(SECTOR.origin().x()+x,SECTOR.origin().y()+y);}
    static List<ModernPetRace.Gate> gates(){return List.of(gate(FIRST_GATE),gate(LAST_GATE));}
    static ModernPetRace.Gate gate(ModernWorldMap.Cell cell){return new ModernPetRace.Gate(cell.x(),cell.x(),cell.y(),cell.y());}
    static ModernWorldMap map()throws Exception{return ModernWorldMap.fromSectorBytes(5143,Map.of(SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);}
    static ModernPetRace.Course course()throws Exception {var map=map();return ModernPetRace.qualify(KEY,map,map.manifest(),gates(),5065,100_000_000L,List.of(MIDDLE,SECOND),()->false);}
    static ModernPetRace.State initial(int limit)throws Exception{return ModernPetRace.initial(11,19,course(),List.of(
            new TournamentPresence.CharacterKey(7,3333),new TournamentPresence.CharacterKey(42,3333)),List.of(MIDDLE,SECOND),limit);}
    static final class Match {
        ModernPetRace.State state;long elapsed,revision;
        Match(int limit)throws Exception{state=ModernPetRace.activate(initial(limit),++revision);}
        void step(long entity,ModernWorldMap.Cell target){var entrant=state.entrant(entity);elapsed+=1_000_000L;
            state=ModernPetRace.moved(state,11,19,entity,entrant.moveSequence()+1,entrant.position(),target,true,elapsed,++revision);}
        void reject(long entity){var entrant=state.entrant(entity);elapsed+=1_000_000L;
            state=ModernPetRace.moved(state,11,19,entity,entrant.moveSequence()+1,entrant.position(),entrant.position(),false,elapsed,++revision);}
        void finish(long entity){
            while(state.entrant(entity).runner().placement()==0){var entrant=state.entrant(entity);
                if(!entrant.position().equals(MIDDLE))step(entity,MIDDLE);
                step(entity,entrant.runner().nextCheckpoint()==0?FIRST_GATE:LAST_GATE);
            }
        }
        void advance(long now){elapsed=now;state=ModernPetRace.advance(state,now,++revision);}
    }
    @Test void qualifiedCourseBindsLogicalPhysicalFullManifestGatesFormAndCadence()throws Exception {
        var map=map();var c=course();assertEquals(5143,c.map().mapId());assertEquals(565,c.key().logicalMapId());assertEquals(2,c.gates().size());
        assertEquals(c.digest(),ModernPetRace.courseHash(KEY,map.manifest(),gates(),5065,100_000_000L));
        assertNotEquals(c.digest(),ModernPetRace.courseHash(new ModernWorldBinding.Key(0,569),map.manifest(),gates(),5065,100_000_000L));
        assertNotEquals(c.digest(),ModernPetRace.courseHash(KEY,map.manifest(),gates(),938,100_000_000L));
        assertNotEquals(c.digest(),ModernPetRace.courseHash(KEY,map.manifest(),gates(),5065,100_000_001L));
        assertNotEquals(c.digest(),ModernPetRace.courseHash(KEY,map.manifest(),List.of(gates().get(1),gates().get(0)),5065,100_000_000L));
        assertThrows(UnsupportedOperationException.class,()->c.gates().clear());
        var forged=new ModernWorldMap.Manifest(565,map.manifest().sectors(),map.digest());
        assertEquals(ModernWorldMap.Failure.CHANGED_FILE,assertThrows(ModernWorldMap.MapException.class,()->ModernPetRace.qualify(KEY,map,forged,gates(),5065,100_000_000L,List.of(MIDDLE,SECOND),()->false)).failure());
    }
    @Test void twoSourcesWithSameObjectAreDistinctAndOnlyTwoThroughTenSortedHumansAreAdmitted()throws Exception {
        var initial=initial(30_000);assertEquals(2,initial.entrants().size());assertEquals(3333,initial.entrants().get(0).source().objectId());assertEquals(3333,initial.entrants().get(1).source().objectId());
        var ids=new ArrayList<TournamentPresence.CharacterKey>();var spawns=new ArrayList<ModernWorldMap.Cell>();
        for(int i=0;i<10;i++){ids.add(new TournamentPresence.CharacterKey(7,3333+i));spawns.add(cell(10+i,10));}
        assertEquals(10,ModernPetRace.initial(1,1,course(),ids,spawns,30_000).entrants().size());
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.initial(1,1,course(),ids.subList(0,1),spawns.subList(0,1),30_000));
        ids.add(new TournamentPresence.CharacterKey(7,3343));spawns.add(cell(20,10));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.initial(1,1,course(),ids,spawns,30_000));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.initial(1,1,course(),List.of(new TournamentPresence.CharacterKey(7,3333),new TournamentPresence.CharacterKey(7,3333)),List.of(MIDDLE,SECOND),30_000));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.initial(1,1,course(),List.of(new TournamentPresence.CharacterKey(42,3333),new TournamentPresence.CharacterKey(7,3333)),List.of(MIDDLE,SECOND),30_000));
    }
    @Test void rejectedMoveReplayAndCrossGenerationCannotManufactureGateProgress()throws Exception {
        var waiting=initial(30_000);assertSame(waiting,ModernPetRace.moved(waiting,11,19,1,1,MIDDLE,MIDDLE,false,0,1));
        var match=new Match(30_000);match.reject(1);assertEquals(1,match.state.entrant(1).moveSequence());assertEquals(0,match.state.entrant(1).runner().progressMoveSequence());
        var before=match.state;
        assertThrows(SecurityException.class,()->ModernPetRace.moved(before,11,20,1,2,MIDDLE,FIRST_GATE,true,2_000_000,3));
        assertThrows(SecurityException.class,()->ModernPetRace.moved(before,11,19,1,1,MIDDLE,FIRST_GATE,true,2_000_000,3));
        assertThrows(SecurityException.class,()->ModernPetRace.moved(before,11,19,1,3,MIDDLE,FIRST_GATE,true,2_000_000,3));
        assertThrows(SecurityException.class,()->ModernPetRace.moved(before,11,19,1,2,SECOND,FIRST_GATE,true,2_000_000,3));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.moved(before,11,19,1,2,MIDDLE,cell(20,20),true,2_000_000,3));
        match.step(1,FIRST_GATE);assertEquals(2,match.state.entrant(1).runner().progressMoveSequence());assertEquals(1,match.state.entrant(1).runner().nextCheckpoint());assertEquals(0,before.entrant(1).runner().nextCheckpoint());
    }
    @Test void gatesMustBeEnteredInOrderAndFourWholeLapsAssignOnePlacement()throws Exception {
        var match=new Match(30_000);match.step(1,LAST_GATE);assertEquals(0,match.state.entrant(1).runner().nextCheckpoint());match.step(1,MIDDLE);
        for(int lap=0;lap<4;lap++){match.step(1,FIRST_GATE);assertEquals(lap,match.state.entrant(1).runner().completedLaps());match.step(1,MIDDLE);match.step(1,LAST_GATE);
            assertEquals(lap+1,match.state.entrant(1).runner().completedLaps());if(lap<3)match.step(1,MIDDLE);}
        var winner=match.state.entrant(1).runner();assertEquals(1,winner.placement());assertEquals(0,winner.nextCheckpoint());assertEquals(ModernPetRace.Phase.FINISH_WINDOW,match.state.phase());
        assertEquals(winner.finishElapsedNanos()+10_000_000_000L,match.state.finishWindowDeadlineElapsedNanos());
        assertFalse(winner.competing());var before=winner;match.reject(1);assertEquals(before,match.state.entrant(1).runner());
    }
    @Test void laterFinishersNeverExtendFixedTenSecondsAndAllFinishedDoNotEndEarly()throws Exception {
        var match=new Match(30_000);match.finish(1);long deadline=match.state.finishWindowDeadlineElapsedNanos();match.finish(2);
        assertEquals(2,match.state.entrant(2).runner().placement());assertEquals(deadline,match.state.finishWindowDeadlineElapsedNanos());assertEquals(ModernPetRace.Phase.FINISH_WINDOW,match.state.phase());
        match.advance(deadline-1);assertEquals(ModernPetRace.Phase.FINISH_WINDOW,match.state.phase());
        match.advance(deadline);assertEquals(ModernPetRace.Phase.COMPLETE,match.state.phase());assertEquals(match.state.revision(),match.state.terminalRevision());
        assertTrue(match.state.runners().stream().allMatch(r->r.dnf()==ModernPetRace.Dnf.NONE));
    }
    @Test void finishWindowMarksRemainingRacerDnfAndLateTickUsesEarlierPlannedDeadline()throws Exception {
        var match=new Match(30_000);match.finish(1);var winner=match.state.entrant(1).runner();
        match.advance(35_000_000_000L);assertEquals(ModernPetRace.Phase.COMPLETE,match.state.phase());
        assertEquals(winner,match.state.entrant(1).runner());assertEquals(ModernPetRace.Dnf.WINDOW_CLOSED,match.state.entrant(2).runner().dnf());assertEquals(0,match.state.entrant(2).runner().placement());
    }
    @Test void earlierSafetyTimeoutIsNotClampedIntoAnAwardAndMoveAtDeadlineCannotFinish()throws Exception {
        var match=new Match(500);match.finish(1);assertTrue(match.state.finishWindowDeadlineElapsedNanos()>match.state.activeLimitNanos());
        match.advance(11_000_000_000L);assertEquals(ModernPetRace.Phase.ABORTED,match.state.phase());assertEquals(1,match.state.entrant(1).runner().placement());
        var other=new Match(50);var before=other.state;
        var terminal=ModernPetRace.moved(before,11,19,1,1,MIDDLE,FIRST_GATE,true,50_000_000L,2);
        assertEquals(ModernPetRace.Phase.ABORTED,terminal.phase());assertEquals(0,terminal.entrant(1).runner().progressMoveSequence());assertEquals(MIDDLE,terminal.entrant(1).position());
    }
    @Test void individualWithdrawalRetainsOthersAndFinishedHistorySurvivesLastOwnerExit()throws Exception {
        var match=new Match(30_000);match.state=ModernPetRace.withdraw(match.state,11,19,2,ModernPetRace.Dnf.DISCONNECTED,0,++match.revision);
        var retired=match.state;assertEquals(retired,ModernPetRace.moved(retired,11,19,2,1,SECOND,SECOND,false,0,++match.revision));
        assertEquals(ModernPetRace.Phase.RUNNING,match.state.phase());assertEquals(ModernPetRace.Dnf.DISCONNECTED,match.state.entrant(2).runner().dnf());match.finish(1);
        var before=match.state;match.state=ModernPetRace.withdraw(before,11,19,1,ModernPetRace.Dnf.LEFT,match.elapsed,++match.revision);assertEquals(before,match.state);
        match.advance(match.state.finishWindowDeadlineElapsedNanos());assertEquals(ModernPetRace.Phase.COMPLETE,match.state.phase());assertEquals(1,match.state.entrant(1).runner().placement());
        var nobody=new Match(30_000);nobody.state=ModernPetRace.withdraw(nobody.state,11,19,1,ModernPetRace.Dnf.LEFT,0,++nobody.revision);
        nobody.state=ModernPetRace.withdraw(nobody.state,11,19,2,ModernPetRace.Dnf.LEFT,0,++nobody.revision);assertEquals(ModernPetRace.Phase.ABORTED,nobody.state.phase());assertEquals(0,nobody.state.firstFinisherEntity());
    }
    @Test void signedNanoOriginWrapIsHarmlessButClockRegressionAndOverflowAreRejected()throws Exception {
        var match=new Match(30_000);long start=Long.MAX_VALUE-1_000_000L,now=start+5_000_000L;
        match.advance(now-start);assertEquals(5_000_000L,match.state.observedElapsedNanos());var before=match.state;
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.advance(before,4_999_999L,9));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.advance(before,-1,9));
        assertThrows(IllegalStateException.class,()->ModernPetRace.activate(before,10));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.abort(before,5_000_000L,before.revision()));
        assertThrows(IllegalArgumentException.class,()->initial(1_800_001));
    }
    @Test void missingOverlappingUnreachableGateAndCancellationFailQualificationWithoutFallback()throws Exception {
        var map=map();var positions=List.of(MIDDLE,SECOND);
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.qualify(KEY,map,map.manifest(),List.of(gates().get(0),gate(cell(100,100))),5065,1_000_000L,positions,()->false));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.qualify(KEY,map,map.manifest(),List.of(gates().get(0),new ModernPetRace.Gate(FIRST_GATE.x()-1,FIRST_GATE.x()+1,FIRST_GATE.y(),FIRST_GATE.y())),5065,1_000_000L,positions,()->false));
        assertThrows(IllegalArgumentException.class,()->ModernPetRace.qualify(KEY,map,map.manifest(),gates(),189,1_000_000L,positions,()->false));
        assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,()->ModernPetRace.qualify(KEY,map,map.manifest(),gates(),5065,1_000_000L,positions,()->true)).failure());
        byte[] split=ModernWorldMapTest.openSectorBytes();for(int y=0;y<64;y++)ModernWorldMapTest.setAttributes(split,15,y,(short)0,(short)1);
        var disconnected=ModernWorldMap.fromSectorBytes(5143,Map.of(SECTOR,split),()->false);
        assertEquals(ModernWorldMap.Failure.INVALID_SPAWNS,assertThrows(ModernWorldMap.MapException.class,()->ModernPetRace.qualify(KEY,disconnected,disconnected.manifest(),List.of(gate(cell(4,4)),gate(cell(20,4))),5065,1_000_000L,positions,()->false)).failure());
    }
}
