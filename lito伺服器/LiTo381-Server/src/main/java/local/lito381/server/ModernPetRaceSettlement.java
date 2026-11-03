package local.lito381.server;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Ranked race result namespace. Never increments DM/Honor WIN/LOSS or source rewards. */
final class ModernPetRaceSettlement {
    record Event(UUID resultId,long matchId,long generation,long terminalRevision,int logicalMapId,
                 ModernWorldMap.Manifest manifest,long stepIntervalNanos,List<ModernPetRace.Gate> gates,
                 String courseSha256,long firstFinisherEntity,long finishWindowDeadlineElapsedNanos,
                 List<ModernPetRaceProtocol.ResultRow> rows,String contentSha256,long recordedAtMillis) {
        Event {
            Objects.requireNonNull(resultId);Objects.requireNonNull(manifest);gates=List.copyOf(gates);rows=List.copyOf(rows);
            if(resultId.equals(ModernPetRaceProtocol.ZERO_ID)||matchId<1||generation<1||terminalRevision<1||recordedAtMillis<0
                    ||rows.size()<2||rows.size()>10||stepIntervalNanos<1_000_000||stepIntervalNanos>10_000_000_000L)throw invalid();
            var key=new ModernWorldBinding.Key(0,logicalMapId);
            validateManifest(manifest);
            ModernPetRaceProtocol.validateGatesManifest(manifest,gates);
            var snapshot=new ModernPetRaceProtocol.Snapshot(true,ModernPetRace.Phase.COMPLETE,ModernPetRaceProtocol.Settlement.PENDING,
                    courseSha256,firstFinisherEntity,finishWindowDeadlineElapsedNanos,terminalRevision,resultId,contentSha256,gates,
                    rows.stream().map(ModernPetRaceProtocol.ResultRow::runner).toList());
            TournamentPresence.CharacterKey previous=null;
            for(var row:rows){var next=row.source();if(previous!=null&&(next.sourceId()<previous.sourceId()
                    ||next.sourceId()==previous.sourceId()&&next.objectId()<=previous.objectId()))throw invalid();previous=next;}
            if(!courseSha256.equals(ModernPetRace.courseHash(key,manifest,gates,rows.get(0).runner().baseGraphicsId(),stepIntervalNanos))
                    ||!contentSha256.equals(ModernPetRaceProtocol.resultHash(resultId,matchId,generation,terminalRevision,key,manifest,courseSha256,
                    firstFinisherEntity,finishWindowDeadlineElapsedNanos,rows)))throw invalid();
        }
        byte[] authoritativeBody(){return ModernPetRaceProtocol.resultBody(resultId,matchId,generation,terminalRevision,new ModernWorldBinding.Key(0,logicalMapId),manifest,courseSha256,firstFinisherEntity,finishWindowDeadlineElapsedNanos,rows);}
        boolean sameAuthority(Event other){return other!=null&&java.util.Arrays.equals(authoritativeBody(),other.authoritativeBody());}
        @Override public String toString(){return "ModernPetRaceResult[redacted]";}
    }
    static Event event(ModernWorldProtocol.Frame frame,long recordedAtMillis) {
        var race=frame.race();if(!race.complete())throw invalid();
        var rows=new java.util.ArrayList<ModernPetRaceProtocol.ResultRow>();for(int i=0;i<race.runners().size();i++)
            rows.add(new ModernPetRaceProtocol.ResultRow(frame.actors().get(i).member().identity().humanKey(),race.runners().get(i)));
        return new Event(race.resultId(),frame.matchId(),frame.generation(),race.terminalRevision(),frame.room().summary().config().mapId(),frame.manifest(),
                frame.actors().get(0).stepIntervalNanos(),race.gates(),race.courseSha256(),race.firstFinisherEntity(),race.finishWindowDeadlineElapsedNanos(),rows,race.contentSha256(),recordedAtMillis);
    }
    /** Service persistence consumes the exact canonical terminal, not a legacy
     * World Frame and its unrelated skill/history constraints. */
    static Event event(ModernBattleObservation.Capture capture,ModernPetRaceProtocol.Snapshot race,long recordedAtMillis) {
        if(!race.complete()||capture.modes().race()==null
                ||!race.equals(ModernPetRaceProtocol.fromState(capture.modes().race(),capture.modes().raceResultId())))throw invalid();
        var rows=new java.util.ArrayList<ModernPetRaceProtocol.ResultRow>();
        for(int i=0;i<race.runners().size();i++)rows.add(new ModernPetRaceProtocol.ResultRow(
                capture.actors().get(i).pose().member().identity().humanKey(),race.runners().get(i)));
        return new Event(race.resultId(),capture.identity().matchId(),capture.identity().generation(),race.terminalRevision(),
                capture.room().summary().config().mapId(),capture.manifest(),capture.actors().get(0).pose().stepIntervalNanos(),
                race.gates(),race.courseSha256(),race.firstFinisherEntity(),race.finishWindowDeadlineElapsedNanos(),rows,race.contentSha256(),recordedAtMillis);
    }
    @FunctionalInterface interface Store {void persist(Event event,long absoluteDeadlineNanos)throws Exception;}
    interface Reservation extends AutoCloseable {
        void record(Event event,long absoluteDeadlineNanos)throws Exception;
        ModernPetRaceProtocol.Settlement state();
        @Override void close();
    }
    interface JournaledStore extends Store,AutoCloseable {
        Reservation reserve(long matchId,long generation)throws Exception;
        boolean accepting();
        @Override void close();
        boolean awaitTermination(long timeoutMillis)throws InterruptedException;
        boolean isQuiescent();
    }
    enum Failure { CONFLICT, INVALID_STORAGE, DEADLINE, UNAVAILABLE, COMMIT_UNKNOWN }
    static final class SettlementException extends Exception {
        private final Failure failure;
        SettlementException(Failure failure){super("modern race settlement: "+Objects.requireNonNull(failure));this.failure=failure;}
        Failure failure(){return failure;}
    }
    private ModernPetRaceSettlement(){}
    static byte[] binding(ModernServerConfig config)throws Exception {
        if(config.raceSettlement()==null||!config.worlds().raceEnabled())throw invalid();
        var bytes=new java.io.ByteArrayOutputStream();try(var out=new java.io.DataOutputStream(bytes)) {
            out.writeUTF("modern-pet-race-outbox-routing-v1");route(out,config.raceSettlement().database());
            var sources=config.sources().stream().sorted(java.util.Comparator.comparingInt(ModernServerConfig.SourceConfig::id)).toList();out.writeInt(sources.size());
            for(var source:sources){out.writeInt(source.id());route(out,source.database());out.writeUTF(source.database().passwordScheme());}
            config.worlds().writeRaceBinding(out);
        }
        return java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
    }
    private static void route(java.io.DataOutputStream out,ModernServerConfig.DbConfig db)throws java.io.IOException {
        out.writeUTF(db.host());out.writeInt(db.port());out.writeUTF(db.database());out.writeUTF(db.sslMode());
    }
    private static void validateManifest(ModernWorldMap.Manifest manifest) {
        if(manifest.mapId()<0||manifest.mapId()>65535||manifest.sectors().isEmpty()||manifest.sectors().size()>ModernWorldMap.MAX_SECTORS
                ||!manifest.digest().matches("[0-9a-f]{64}")||manifest.digest().equals(ModernPetRaceProtocol.ZERO_HASH))throw invalid();
        long total=0;int x=-1,y=-1;
        for(var sector:manifest.sectors()) {
            if(sector.key().x()<x||sector.key().x()==x&&sector.key().y()<=y||sector.byteLength()<49158||sector.byteLength()>ModernWorldMap.MAX_SECTOR_BYTES
                    ||!sector.rawSha256().matches("[0-9a-f]{64}")||sector.rawSha256().equals(ModernPetRaceProtocol.ZERO_HASH))throw invalid();
            total=Math.addExact(total,sector.byteLength());if(total>ModernWorldMap.MAX_TOTAL_BYTES)throw invalid();x=sector.key().x();y=sector.key().y();
        }
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid modern race result");}
}
