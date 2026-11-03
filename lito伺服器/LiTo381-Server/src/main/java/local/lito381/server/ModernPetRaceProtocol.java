package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** v16 World-only race authority. No player supplied checkpoint, clock, placement or result. */
final class ModernPetRaceProtocol {
    static final int HEADER_BYTES=112,RUNNER_BYTES=72,GATE_BYTES=16;
    static final String ZERO_HASH="0".repeat(64);
    static final UUID ZERO_ID=new UUID(0,0);
    enum Settlement { NONE, PENDING, SAVED, FAILED }
    record ResultRow(TournamentPresence.CharacterKey source,ModernPetRace.Runner runner) {
        ResultRow { Objects.requireNonNull(source);Objects.requireNonNull(runner); }
    }
    record Snapshot(boolean enabled,ModernPetRace.Phase phase,Settlement settlement,String courseSha256,
                    long firstFinisherEntity,long finishWindowDeadlineElapsedNanos,long terminalRevision,
                    UUID resultId,String contentSha256,List<ModernPetRace.Gate> gates,List<ModernPetRace.Runner> runners) {
        Snapshot {
            Objects.requireNonNull(phase);Objects.requireNonNull(settlement);Objects.requireNonNull(resultId);
            gates=List.copyOf(gates);runners=List.copyOf(runners);
            if(!hash(courseSha256)||!hash(contentSha256)||firstFinisherEntity<0||finishWindowDeadlineElapsedNanos<0||terminalRevision<0)throw bad();
            if(!enabled) {
                if(phase!=ModernPetRace.Phase.WAITING||settlement!=Settlement.NONE||!courseSha256.equals(ZERO_HASH)
                        ||firstFinisherEntity!=0||finishWindowDeadlineElapsedNanos!=0||terminalRevision!=0||!resultId.equals(ZERO_ID)
                        ||!contentSha256.equals(ZERO_HASH)||!gates.isEmpty()||!runners.isEmpty())throw bad();
            }else {
                if(gates.size()<2||gates.size()>ModernPetRace.MAX_GATES||new HashSet<>(gates).size()!=gates.size()
                        ||runners.size()<2||runners.size()>10||courseSha256.equals(ZERO_HASH)||firstFinisherEntity>runners.size())throw bad();
                boolean complete=phase==ModernPetRace.Phase.COMPLETE;
                if(complete?(settlement==Settlement.NONE||resultId.equals(ZERO_ID)||contentSha256.equals(ZERO_HASH))
                        :(settlement!=Settlement.NONE||!resultId.equals(ZERO_ID)||!contentSha256.equals(ZERO_HASH)))throw bad();
                if((complete||phase==ModernPetRace.Phase.ABORTED)!=(terminalRevision>0))throw bad();
                var placements=new HashSet<Long>();long first=0,graphics=runners.get(0).baseGraphicsId();
                for(int i=0;i<runners.size();i++) {
                    var r=runners.get(i);
                    if(r.entityId()!=i+1L||r.baseGraphicsId()!=graphics||r.nextCheckpoint()>=gates.size()
                            ||r.progressMoveSequence()<r.completedLaps()*gates.size()+r.nextCheckpoint()
                            ||terminalRevision>0&&r.revision()>terminalRevision)throw bad();
                    if(r.placement()>0&&!placements.add(r.placement()))throw bad();if(r.placement()==1)first=r.entityId();
                    if(r.dnf()==ModernPetRace.Dnf.WINDOW_CLOSED&&(!complete||r.revision()!=terminalRevision))throw bad();
                    if(phase==ModernPetRace.Phase.WAITING&&(r.revision()!=0||r.progressMoveSequence()!=0))throw bad();
                    if(complete&&r.competing())throw bad();
                }
                if(first!=firstFinisherEntity||(first==0)!=(finishWindowDeadlineElapsedNanos==0))throw bad();
                if(first>0&&finishWindowDeadlineElapsedNanos!=Math.addExact(runners.get((int)first-1).finishElapsedNanos(),ModernPetRace.FINISH_WINDOW_NANOS))throw bad();
                if((phase==ModernPetRace.Phase.WAITING||phase==ModernPetRace.Phase.RUNNING)&&first!=0
                        ||(phase==ModernPetRace.Phase.FINISH_WINDOW||complete)&&first==0)throw bad();
                long previous=0;for(long p=1;p<=placements.size();p++) {
                    long place=p;var r=runners.stream().filter(v->v.placement()==place).findFirst().orElseThrow(ModernPetRaceProtocol::bad);
                    if(r.finishElapsedNanos()<previous||r.finishElapsedNanos()>finishWindowDeadlineElapsedNanos)throw bad();previous=r.finishElapsedNanos();
                }
            }
        }
        static Snapshot disabled(){return new Snapshot(false,ModernPetRace.Phase.WAITING,Settlement.NONE,ZERO_HASH,0,0,0,ZERO_ID,ZERO_HASH,List.of(),List.of());}
        boolean complete(){return enabled&&phase==ModernPetRace.Phase.COMPLETE;}
        Snapshot withSettlement(Settlement next){
            if(!complete()||settlement!=Settlement.PENDING||next!=Settlement.SAVED&&next!=Settlement.FAILED)throw bad();
            return new Snapshot(true,phase,next,courseSha256,firstFinisherEntity,finishWindowDeadlineElapsedNanos,terminalRevision,resultId,contentSha256,gates,runners);
        }
    }
    private ModernPetRaceProtocol(){}
    static Snapshot aborted(Snapshot old,long revision){return !old.enabled()?old:new Snapshot(true,ModernPetRace.Phase.ABORTED,Settlement.NONE,old.courseSha256(),
            old.firstFinisherEntity(),old.finishWindowDeadlineElapsedNanos(),revision,ZERO_ID,ZERO_HASH,old.gates(),old.runners());}
    static Snapshot fromState(ModernPetRace.State state,UUID resultId) {
        var rows=state.runners().stream().map(ModernPetRaceProtocol::wireRevision).toList();
        long terminal=wireRevision(state.terminalRevision());boolean complete=state.phase()==ModernPetRace.Phase.COMPLETE;
        UUID id=complete?Objects.requireNonNull(resultId):ZERO_ID;
        var resultRows=new ArrayList<ResultRow>();for(int i=0;i<rows.size();i++)resultRows.add(new ResultRow(state.entrants().get(i).source(),rows.get(i)));
        String hash=complete?resultHash(id,state.matchId(),state.generation(),terminal,state.course().key(),state.course().map().manifest(),state.course().digest(),state.firstFinisherEntity(),state.finishWindowDeadlineElapsedNanos(),resultRows):ZERO_HASH;
        return new Snapshot(true,state.phase(),complete?Settlement.PENDING:Settlement.NONE,state.course().digest(),state.firstFinisherEntity(),
                state.finishWindowDeadlineElapsedNanos(),terminal,id,hash,state.course().gates(),rows);
    }
    static ModernPetRace.Runner wireRevision(ModernPetRace.Runner r) {
        return new ModernPetRace.Runner(r.entityId(),wireRevision(r.revision()),r.progressMoveSequence(),r.completedLaps(),r.nextCheckpoint(),r.placement(),r.finishElapsedNanos(),r.dnf(),r.baseGraphicsId());
    }
    private static long wireRevision(long revision){return revision==0?0:Math.incrementExact(revision);}
    static void validate(ModernWorldProtocol.Frame world,Snapshot race) {
        if(!race.enabled())return;
        if(world.room().summary().config().mode()!=0||race.runners().size()!=world.actors().size()||world.completion()!=null
                ||world.descriptor()==null||world.descriptor().features()!=1024||world.descriptor().ruleset().ordinal()!=4
                ||race.terminalRevision()>world.revision())throw bad();
        var expected=switch(world.phase()) {
            case PREPARING,LOADING,COUNTDOWN->ModernPetRace.Phase.WAITING;
            case ACTIVE->race.firstFinisherEntity()==0?ModernPetRace.Phase.RUNNING:ModernPetRace.Phase.FINISH_WINDOW;
            case FINISHED->ModernPetRace.Phase.COMPLETE;case ABORTED->ModernPetRace.Phase.ABORTED;
        };
        if(race.phase()!=expected)throw bad();
        validateGatesManifest(world.manifest(),race.gates());
        long interval=world.actors().get(0).stepIntervalNanos(),limit=Math.multiplyExact((long)world.descriptor().activeTimeoutMillis(),1_000_000L);
        var occupied=new HashSet<ModernWorldMap.Cell>();var identities=new ArrayList<ResultRow>();
        for(int i=0;i<race.runners().size();i++) {
            var actor=world.actors().get(i);var r=race.runners().get(i);
            if(!actor.member().identity().isHuman()||actor.stepIntervalNanos()!=interval||r.revision()>world.revision()
                    ||r.progressMoveSequence()>actor.lastMoveSequence()||r.finishElapsedNanos()>=limit
                    ||race.phase()==ModernPetRace.Phase.WAITING&&actor.lastMoveSequence()!=0
                    ||r.competing()&&!occupied.add(actor.position()))throw bad();
            if(r.revision()==world.revision()&&r.dnf()==ModernPetRace.Dnf.NONE&&r.progressMoveSequence()>0) {
                int index=r.nextCheckpoint()==0?race.gates().size()-1:(int)r.nextCheckpoint()-1;
                if(actor.lastMoveSequence()!=r.progressMoveSequence()||!race.gates().get(index).contains(actor.position()))throw bad();
            }
            identities.add(new ResultRow(actor.member().identity().humanKey(),r));
        }
        var key=new ModernWorldBinding.Key(0,world.room().summary().config().mapId());
        if(!race.courseSha256().equals(ModernPetRace.courseHash(key,world.manifest(),race.gates(),race.runners().get(0).baseGraphicsId(),interval)))throw bad();
        if(race.complete()) {
            if(race.finishWindowDeadlineElapsedNanos()>limit||!race.contentSha256().equals(resultHash(race.resultId(),world.matchId(),world.generation(),race.terminalRevision(),key,
                    world.manifest(),race.courseSha256(),race.firstFinisherEntity(),race.finishWindowDeadlineElapsedNanos(),identities)))throw bad();
            if(race.settlement()==Settlement.PENDING?world.revision()!=race.terminalRevision():world.revision()!=Math.incrementExact(race.terminalRevision()))throw bad();
        }
    }
    /** Cheap wire/storage shape proof only; startup still qualifies every gate against actual terrain. */
    static void validateGatesManifest(ModernWorldMap.Manifest manifest,List<ModernPetRace.Gate> gates) {
        for(var gate:gates) {
            boolean intersects=false;
            for(var sector:manifest.sectors()) {
                long x=32704L+((long)sector.key().x()-0x7fff)*64L;
                long y=32704L+((long)sector.key().y()-0x7fff)*64L;
                if(gate.minX()<=x+63&&gate.maxX()>=x&&gate.minY()<=y+63&&gate.maxY()>=y){intersects=true;break;}
            }
            if(!intersects)throw bad();
        }
    }
    static String resultHash(UUID id,long match,long generation,long terminal,ModernWorldBinding.Key key,ModernWorldMap.Manifest manifest,
                             String course,long first,long deadline,List<ResultRow> rows) {
        return digest(resultBody(id,match,generation,terminal,key,manifest,course,first,deadline,rows));
    }
    static byte[] resultBody(UUID id,long match,long generation,long terminal,ModernWorldBinding.Key key,ModernWorldMap.Manifest manifest,
                             String course,long first,long deadline,List<ResultRow> rows) {
        try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)) {
            out.writeUTF("LITO_MODERN_PET_RACE_RESULT_V1");out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());
            out.writeLong(match);out.writeLong(generation);out.writeLong(terminal);out.writeInt(key.mode());out.writeInt(key.logicalMapId());out.writeInt(manifest.mapId());
            out.write(HexFormat.of().parseHex(manifest.digest()));out.write(HexFormat.of().parseHex(course));out.writeLong(first);out.writeLong(deadline);out.writeInt(rows.size());
            for(var row:rows){var r=row.runner();out.writeLong(r.entityId());out.writeInt(row.source().sourceId());out.writeLong(row.source().objectId());
                out.writeLong(r.revision());out.writeLong(r.progressMoveSequence());out.writeLong(r.completedLaps());out.writeLong(r.nextCheckpoint());
                out.writeLong(r.placement());out.writeLong(r.finishElapsedNanos());out.writeLong(r.dnf().ordinal());out.writeLong(r.baseGraphicsId());}
        }return bytes.toByteArray();}catch(java.io.IOException impossible){throw new IllegalStateException(impossible);}
    }
    static String digest(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    static void put(ByteBuffer out,Snapshot race) {
        if(!race.enabled()){out.putLong(0);return;}
        out.put((byte)1).put((byte)race.phase().ordinal()).put((byte)race.settlement().ordinal()).put((byte)0)
                .putShort((short)race.gates().size()).put((byte)race.runners().size()).put((byte)ModernPetRace.LAPS);
        out.put(HexFormat.of().parseHex(race.courseSha256())).putLong(race.firstFinisherEntity()).putLong(race.finishWindowDeadlineElapsedNanos()).putLong(race.terminalRevision());
        out.putLong(race.resultId().getMostSignificantBits()).putLong(race.resultId().getLeastSignificantBits()).put(HexFormat.of().parseHex(race.contentSha256()));
        for(var gate:race.gates())out.putInt(gate.minX()).putInt(gate.maxX()).putInt(gate.minY()).putInt(gate.maxY());
        for(var r:race.runners())out.putLong(r.entityId()).putLong(r.revision()).putLong(r.progressMoveSequence()).putLong(r.completedLaps()).putLong(r.nextCheckpoint())
                .putLong(r.placement()).putLong(r.finishElapsedNanos()).putLong(r.dnf().ordinal()).putLong(r.baseGraphicsId());
    }
    static Snapshot get(ByteBuffer in) {
        int enabled=Byte.toUnsignedInt(in.get()),phase=Byte.toUnsignedInt(in.get()),settlement=Byte.toUnsignedInt(in.get()),zero=Byte.toUnsignedInt(in.get());
        int gateCount=Short.toUnsignedInt(in.getShort()),runnerCount=Byte.toUnsignedInt(in.get()),laps=Byte.toUnsignedInt(in.get());
        if(enabled==0){if(phase!=0||settlement!=0||zero!=0||gateCount!=0||runnerCount!=0||laps!=0)throw bad();return Snapshot.disabled();}
        if(enabled!=1||phase>=ModernPetRace.Phase.values().length||settlement>=Settlement.values().length||zero!=0||laps!=4||gateCount<2||gateCount>64||runnerCount<2||runnerCount>10)throw bad();
        byte[] hash=new byte[32];in.get(hash);String course=HexFormat.of().formatHex(hash);long first=in.getLong(),deadline=in.getLong(),terminal=in.getLong();
        var uuid=new UUID(in.getLong(),in.getLong());in.get(hash);String content=HexFormat.of().formatHex(hash);
        var gates=new ArrayList<ModernPetRace.Gate>();for(int i=0;i<gateCount;i++)gates.add(new ModernPetRace.Gate(in.getInt(),in.getInt(),in.getInt(),in.getInt()));
        var runners=new ArrayList<ModernPetRace.Runner>();for(int i=0;i<runnerCount;i++) {
            long entity=in.getLong(),revision=in.getLong(),sequence=in.getLong(),completed=in.getLong(),next=in.getLong(),placement=in.getLong(),elapsed=in.getLong(),dnf=in.getLong(),graphics=in.getLong();
            if(dnf<0||dnf>=ModernPetRace.Dnf.values().length)throw bad();runners.add(new ModernPetRace.Runner(entity,revision,sequence,completed,next,placement,elapsed,ModernPetRace.Dnf.values()[(int)dnf],graphics));
        }
        return new Snapshot(true,ModernPetRace.Phase.values()[phase],Settlement.values()[settlement],course,first,deadline,terminal,uuid,content,gates,runners);
    }
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern race authority");}
}
