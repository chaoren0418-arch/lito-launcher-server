package local.lito381.server;

import java.util.List;
import java.util.UUID;

/** Qualified course and entrant mirrors bound to the actual canonical world.
 * No map rescan, source aliases, legacy receipt offset or invented combat. */
final class ModernBattleRaceProof {
    static void validate(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,
            ModernPetRace.State race,UUID resultId) {
        if(race==null){if(resultId!=null)throw bad();return;}
        var config=room.summary().config();var course=race.course();
        var expected=switch(phase) {
            case LOADING,COUNTDOWN->ModernPetRace.Phase.WAITING;
            case ACTIVE->race.firstFinisherEntity()==0?ModernPetRace.Phase.RUNNING:ModernPetRace.Phase.FINISH_WINDOW;
            case FINISHED->ModernPetRace.Phase.COMPLETE;case ABORTED->ModernPetRace.Phase.ABORTED;
        };
        if(race.phase()!=expected||!course.key().equals(new ModernWorldBinding.Key(config.mode(),config.mapId()))
                ||!course.map().manifest().equals(manifest)||race.observedElapsedNanos()>Math.max(0,clock.activeElapsedNanos())
                ||(race.phase()==ModernPetRace.Phase.COMPLETE)!=(resultId!=null)
                ||resultId!=null&&(resultId.getMostSignificantBits()|resultId.getLeastSignificantBits())==0
                ||race.terminalRevision()>0&&race.terminalRevision()!=revision)throw bad();
        for(int i=0;i<actors.size();i++) {
            var a=actors.get(i);var pose=a.pose();var e=race.entrants().get(i);var r=e.runner();
            if(!pose.member().identity().isHuman()||!pose.member().identity().humanKey().equals(e.source())
                    ||!pose.position().equals(e.position())||pose.lastMoveSequence()!=e.moveSequence()
                    ||pose.stepIntervalNanos()!=course.stepIntervalNanos()||a.vital()!=null||a.resources()!=null
                    ||r.finishElapsedNanos()>=race.activeLimitNanos()
                    ||race.phase()==ModernPetRace.Phase.WAITING&&pose.lastMoveSequence()!=0)throw bad();
            if(r.revision()==revision&&r.dnf()==ModernPetRace.Dnf.NONE&&r.progressMoveSequence()>0) {
                int gate=r.nextCheckpoint()==0?course.gates().size()-1:(int)r.nextCheckpoint()-1;
                if(pose.lastMoveSequence()!=r.progressMoveSequence()||!course.gates().get(gate).contains(pose.position()))throw bad();
            }
        }
    }
    static void publication(ModernBattleObservation.World w,ModernMatchDescriptor descriptor) {
        var race=w.modes().race();if(race!=null&&race.activeLimitNanos()!=Math.multiplyExact((long)descriptor.activeTimeoutMillis(),1_000_000L))throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical race proof");}
    private ModernBattleRaceProof(){}
}
