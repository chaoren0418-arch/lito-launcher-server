package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Cooperative result variant for the shared modern ledger, not a two-team
 * victory or a new source-character table. Persistence version 3 retains the
 * terminal cause, policy/terrain identities and each participant's eligibility.
 * No current player wire may silently discard this proof. */
final class ModernTimeAttackSettlement {
    static final int PROOF_BYTES=124;
    record Proof(long roomId,int physicalMapId,String terrainSha256,String planSha256,
                 ModernTimeAttack.End end,int wave,int waveCount,int leader,int durationMillis,
                 long startedMillis,long observedMillis,long aliveMask,long departedMask,long towerDeathRevision) {
        Proof {
            Objects.requireNonNull(end);
            if(roomId<1||physicalMapId<0||physicalMapId>65535||!hash(terrainSha256)||!hash(planSha256)
                    ||end==ModernTimeAttack.End.NONE||waveCount<2||waveCount>32||wave<0||wave>=waveCount
                    ||leader<1||leader>60||durationMillis<50||durationMillis>1_800_000||startedMillis< -1||startedMillis>=60_000
                    ||observedMillis<0||observedMillis>ModernNpcRoster.MAX_ELAPSED_MILLIS||startedMillis>observedMillis
                    ||aliveMask<0||aliveMask>>>60!=0||departedMask<0||departedMask>>>60!=0||towerDeathRevision<0
                    ||(startedMillis>=0)!=(towerDeathRevision>0)
                    ||startedMillis<0&&(wave!=0||end!=ModernTimeAttack.End.PARTY_DEFEATED)
                    ||end==ModernTimeAttack.End.TIMEOUT&&observedMillis-startedMillis!=durationMillis
                    ||startedMillis>=0&&end!=ModernTimeAttack.End.TIMEOUT&&observedMillis-startedMillis>=durationMillis
                    ||end==ModernTimeAttack.End.CLEARED&&wave!=waveCount-1)throw bad();
            long leaderBit=1L<<(leader-1),eligible=aliveMask&~departedMask;
            if(end==ModernTimeAttack.End.LEADER_DEFEATED&&((aliveMask|departedMask)&leaderBit)!=0
                    ||end==ModernTimeAttack.End.PARTY_DEFEATED&&(eligible!=0||(departedMask&leaderBit)==0)
                    ||(end==ModernTimeAttack.End.CLEARED||end==ModernTimeAttack.End.TIMEOUT)&&(eligible&leaderBit)==0)throw bad();
        }
        void validate(int mode,int map,long revision,List<ModernMatchSettlement.Participant> rows) {
            if(mode!=4||map!=540||rows.isEmpty()||rows.size()>60||leader>rows.size()||towerDeathRevision>0&&towerDeathRevision>=revision
                    ||aliveMask>>>rows.size()!=0||departedMask>>>rows.size()!=0)throw bad();
            for(var row:rows) {
                long bit=1L<<(row.entityId()-1);
                var expected=end==ModernTimeAttack.End.CLEARED&&(aliveMask&bit)!=0&&(departedMask&bit)==0
                        ?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS;
                if(!row.identity().isHuman()||row.team()!=0||row.verdict()!=expected)throw bad();
            }
        }
        @Override public String toString(){return "CooperativeResultProof[redacted]";}
    }

    static ModernMatchSettlement.Event event(ModernBattleEngine.Snapshot world,UUID id,long recordedAtMillis) {
        world.timeAttack().validate(world);
        if(!world.timeAttack().finished())throw bad();
        var s=world.timeAttack().state();long alive=0,departed=0;
        for(var v:world.vitals())if(v.hp()>0)alive|=1L<<(v.entityId()-1);
        for(long entity:s.departed())departed|=1L<<(entity-1);
        var proof=new Proof(s.scope().roomId(),world.manifest().mapId(),world.manifest().digest(),planHash(s.plan()),
                s.end(),s.wave(),s.plan().waves().size(),Math.toIntExact(s.leader()),s.plan().durationMillis(),
                s.startedMillis(),s.observedMillis(),alive,departed,s.towerDeathRevision());
        return new ModernMatchSettlement.Event(id,world.matchId(),world.generation(),world.revision(),4,540,recordedAtMillis,
                world.timeAttack().outcomes(world),proof);
    }

    /** Canonical observation overload: no legacy spell/capacity frame needed. */
    static ModernMatchSettlement.Event event(ModernBattleObservation.World world,UUID id,long recordedAtMillis) {
        var s=world.modes().timeAttack().state();
        if(s==null||!world.modes().timeAttack().finished()||world.phase()!=ModernBattleEngine.Phase.FINISHED)throw bad();
        long alive=0,departed=0;
        for(var a:world.actors())if(a.vital().values().hp()>0)alive|=1L<<(a.pose().entityId()-1);
        for(long entity:s.departed())departed|=1L<<(entity-1);
        var proof=new Proof(s.scope().roomId(),world.manifest().mapId(),world.manifest().digest(),planHash(s.plan()),
                s.end(),s.wave(),s.plan().waves().size(),Math.toIntExact(s.leader()),s.plan().durationMillis(),
                s.startedMillis(),s.observedMillis(),alive,departed,s.towerDeathRevision());
        var rows=world.actors().stream().map(a->new ModernMatchSettlement.Participant(a.pose().entityId(),a.pose().member().identity(),0,
                s.won()&&!s.departed().contains(a.pose().entityId())&&a.vital().values().hp()>0?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        return new ModernMatchSettlement.Event(id,world.identity().matchId(),world.identity().generation(),world.revision(),4,540,recordedAtMillis,rows,proof);
    }

    /** Ordered explicit policy, never a claim of recovered historical weights. */
    static String planHash(ModernTimeAttack.Plan plan) {
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            out.writeUTF("modern-cooperative-time-attack-plan-v1");out.writeInt(plan.key().mode());out.writeInt(plan.key().logicalMapId());
            cell(out,plan.tower().cell());out.writeInt(plan.tower().graphic());out.writeLong(plan.tower().maximumHp());out.writeInt(plan.durationMillis());
            out.writeInt(plan.waves().size());
            for(var wave:plan.waves()) {
                out.writeInt(wave.kind().ordinal());out.writeInt(wave.spawns().size());
                for(var spawn:wave.spawns()) {
                    var t=spawn.template();out.writeInt(t.catalogSourceId());out.writeUTF(t.catalogDatabase());out.writeInt(t.npcId());out.writeInt(t.graphic());
                    out.writeLong(t.maximumHp());out.writeLong(t.maximumMp());out.writeLong(t.walkMillis());out.writeLong(t.attackMillis());
                    out.writeInt(spawn.lane());out.writeInt(spawn.stage());cell(out,spawn.cell());out.writeInt(spawn.heading());
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(IOException|NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    static void write(DataOutputStream out,Proof p)throws IOException {
        out.writeInt(p.physicalMapId());out.write(HexFormat.of().parseHex(p.terrainSha256()));out.write(HexFormat.of().parseHex(p.planSha256()));
        out.writeLong(p.roomId());out.writeByte(p.end().ordinal());out.writeByte(p.wave());out.writeByte(p.waveCount());out.writeByte(p.leader());
        out.writeInt(p.durationMillis());out.writeLong(p.startedMillis());out.writeLong(p.observedMillis());out.writeLong(p.aliveMask());
        out.writeLong(p.departedMask());out.writeLong(p.towerDeathRevision());
    }
    static Proof read(ByteBuffer in) {
        if(in.remaining()!=PROOF_BYTES)throw bad();
        int map=in.getInt();byte[] terrain=new byte[32],plan=new byte[32];in.get(terrain).get(plan);
        long room=in.getLong();int end=Byte.toUnsignedInt(in.get()),wave=Byte.toUnsignedInt(in.get()),count=Byte.toUnsignedInt(in.get()),leader=Byte.toUnsignedInt(in.get());
        if(end>=ModernTimeAttack.End.values().length)throw bad();
        return new Proof(room,map,HexFormat.of().formatHex(terrain),HexFormat.of().formatHex(plan),ModernTimeAttack.End.values()[end],wave,count,leader,
                in.getInt(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong());
    }
    private static void cell(DataOutputStream out,ModernWorldMap.Cell cell)throws IOException {out.writeInt(cell.x());out.writeInt(cell.y());}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}")&&!value.equals("0".repeat(64));}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid cooperative result authority");}
    private ModernTimeAttackSettlement(){}
}
