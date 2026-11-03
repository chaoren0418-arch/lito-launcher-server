package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernBattleRaceProofTest {
    static ModernBattleObservation.Capture race(ModernBattleObservation.Capture c,ModernPetRace.State s,UUID result) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),m.timeAttack(),m.siege(),s,result,m.timeAttackResult(),m.siegeResult()));
    }
    static ModernPetRace.State entrants(ModernPetRace.State s,List<ModernPetRace.Entrant> rows) {
        return new ModernPetRace.State(s.matchId(),s.generation(),s.revision(),s.course(),s.phase(),s.activeLimitNanos(),s.observedElapsedNanos(),
                s.firstFinisherEntity(),s.finishWindowDeadlineElapsedNanos(),s.terminalRevision(),rows);
    }
    @Test void raceSourceCannotBeAnotherCharacterInTheSameDatabase()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var c=f.engine.serviceObservation();var s=c.modes().race();var e=s.entrants().get(0);
            var wrong=new ModernPetRace.Entrant(new TournamentPresence.CharacterKey(e.source().sourceId(),4444),e.position(),e.moveSequence(),e.runner());
            assertThrows(IllegalArgumentException.class,()->race(c,entrants(s,ModernBattleLifeProofTest.replace(s.entrants(),0,wrong)),null).world());
        }
    }
    @Test void entrantPositionAndAttemptSequenceMustMirrorTheActualActor()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var c=f.engine.serviceObservation();var s=c.modes().race();var e=s.entrants().get(0);
            for(var wrong:List.of(new ModernPetRace.Entrant(e.source(),ModernPetRaceTest.FIRST_GATE,e.moveSequence(),e.runner()),
                    new ModernPetRace.Entrant(e.source(),e.position(),1,e.runner())))
                assertThrows(IllegalArgumentException.class,()->race(c,entrants(s,ModernBattleLifeProofTest.replace(s.entrants(),0,wrong)),null).world());
        }
    }
    @Test void completeRaceCannotOmitItsOneResultIdentity()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();
            assertNotNull(c.modes().raceResultId());assertEquals(ModernBattleEngine.Phase.FINISHED,c.world().phase());
            assertThrows(IllegalArgumentException.class,()->race(c,c.modes().race(),null).world());
            assertThrows(IllegalArgumentException.class,()->race(c,c.modes().race(),new UUID(0,0)).world());
        }
    }
    @Test void activeWorldCannotRetainTheWaitingRaceAndCannotPublishAPrematureResult()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var waiting=f.engine.serviceObservation().modes().race();f.active();var c=f.engine.serviceObservation();
            assertThrows(IllegalArgumentException.class,()->race(c,waiting,null).world());
            assertThrows(IllegalArgumentException.class,()->race(c,c.modes().race(),UUID.randomUUID()).world());
        }
    }
    @Test void separatelyQualifiedCourseCannotChangeTheActualActorsPace()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var c=f.engine.serviceObservation();var s=c.modes().race();
            var course=ModernPetRace.qualify(f.binding.key(),f.map,f.map.manifest(),f.course.gates(),f.course.baseGraphicsId(),ModernBattleEnginePetRaceTest.STEP*2,
                    s.entrants().stream().map(ModernPetRace.Entrant::position).toList(),()->false);
            var changed=new ModernPetRace.State(s.matchId(),s.generation(),s.revision(),course,s.phase(),s.activeLimitNanos(),s.observedElapsedNanos(),
                    s.firstFinisherEntity(),s.finishWindowDeadlineElapsedNanos(),s.terminalRevision(),s.entrants());
            assertThrows(IllegalArgumentException.class,()->race(c,changed,null).world());
        }
    }
    @Test void publicationDurationMustEqualTheRealRaceLimitEvenIfCaptureAndDescriptorAgree()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var c=f.engine.serviceObservation();var l=new ModernBattleEngine.Limits(c.limits().loadTimeoutMillis(),c.limits().countdownNanos(),60_000);
            var forged=new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),l,c.loadingOriginNanos(),c.actors(),c.modes(),c.npcs(),c.result());
            var descriptor=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PET_RACE,ModernMatchDescriptor.PET_RACE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(forged,descriptor,null,null));
        }
    }
    @Test void collisionsClockOnlyReadsAndIndividualDepartureKeepExactMirrorsAndNoCombat()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();var before=f.engine.serviceObservation().world();f.clock.incrementAndGet();var later=f.engine.serviceObservation().world();
            assertEquals(before.revision(),later.revision());assertTrue(later.modes().race().observedElapsedNanos()>before.modes().race().observedElapsedNanos());
            assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,2).status());var denied=f.engine.serviceObservation().world();
            assertEquals(1,denied.actors().get(0).pose().lastMoveSequence());assertEquals(1,denied.modes().race().entrants().get(0).moveSequence());
            assertEquals(0,denied.modes().race().runners().get(0).progressMoveSequence());
            f.engine.withdrawRacer(f.b.owner(),ModernPetRace.Dnf.LEFT);f.step(0,ModernPetRaceTest.SECOND);
            var free=f.engine.serviceObservation().world();assertEquals(free.actors().get(0).pose().position(),free.actors().get(1).pose().position());
            assertNull(free.actors().get(0).vital());assertNull(free.actors().get(0).resources());
        }
    }
    @Test void realFourLapsFinishWindowAndLateReadKeepOneTerminalIdentity()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();var first=f.finish(0);var active=f.engine.serviceObservation().world();assertEquals(ModernPetRace.Phase.FINISH_WINDOW,active.modes().race().phase());
            assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE,f.move(0,0).status());assertEquals(active,f.engine.serviceObservation().world());
            f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();var done=c.world();var id=c.modes().raceResultId();
            assertNotNull(id);assertEquals(done.revision(),done.modes().race().terminalRevision());
            f.elapsed(first.race().finishWindowDeadlineElapsedNanos()+100_000_000_000L);var late=f.engine.serviceObservation();
            assertEquals(id,late.modes().raceResultId());assertEquals(done.modes(),late.world().modes());assertEquals(done.revision(),late.revision());
        }
    }
}
