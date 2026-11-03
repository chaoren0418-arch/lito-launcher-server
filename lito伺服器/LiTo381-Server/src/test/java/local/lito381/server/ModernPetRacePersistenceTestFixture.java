package local.lito381.server;

import java.util.List;
import java.util.UUID;
import static local.lito381.server.ModernPetRaceSettlement.*;

/** Pure authority fixtures; no map/resource loading, database or server. */
final class ModernPetRacePersistenceTestFixture {
    static final UUID ID=UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    static Event event(){return event(ID,7,123456789);}
    static Event event(UUID id,long match,long recorded) {
        var manifest=new ModernWorldMap.Manifest(514,List.of(new ModernWorldMap.SectorManifest(
                new ModernWorldMap.SectorKey(0x7fff,0x8000),137784,"01".repeat(32))),"02".repeat(32));
        var gates=List.of(new ModernPetRace.Gate(32726,32727,32784,32784),new ModernPetRace.Gate(32728,32729,32784,32784));
        var rows=List.of(
                new ModernPetRaceProtocol.ResultRow(new TournamentPresence.CharacterKey(7,3333),
                        new ModernPetRace.Runner(1,11,24,4,0,1,1_000_000_000L,ModernPetRace.Dnf.NONE,2145)),
                new ModernPetRaceProtocol.ResultRow(new TournamentPresence.CharacterKey(42,3333),
                        new ModernPetRace.Runner(2,20,0,0,0,0,0,ModernPetRace.Dnf.WINDOW_CLOSED,2145)));
        var key=new ModernWorldBinding.Key(0,565);
        String course=ModernPetRace.courseHash(key,manifest,gates,2145,200_000_000);
        String content=ModernPetRaceProtocol.resultHash(id,match,11,20,key,manifest,course,1,11_000_000_000L,rows);
        return new Event(id,match,11,20,565,manifest,200_000_000,gates,course,1,11_000_000_000L,rows,content,recorded);
    }
    static Event recorded(Event e,long recorded) {
        return new Event(e.resultId(),e.matchId(),e.generation(),e.terminalRevision(),e.logicalMapId(),e.manifest(),
                e.stepIntervalNanos(),e.gates(),e.courseSha256(),e.firstFinisherEntity(),e.finishWindowDeadlineElapsedNanos(),
                e.rows(),e.contentSha256(),recorded);
    }
    private ModernPetRacePersistenceTestFixture(){}
}

