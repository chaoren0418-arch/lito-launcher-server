package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Version-four durable siege proof. No client may submit a winner, turn an
 * abort into a loss, or strip this proof into the old deathmatch wire. Shares
 * the source-qualified modern ledger; no native/source character SQL writes. */
final class ModernSiegeSettlement {
    static final int PROOF_BYTES=308;
    enum End { INNER_DESTROYED, TIMEOUT }
    record Proof(long roomId,int physicalMapId,String terrainSha256,String configSha256,
                 int attacker,int winner,End end,int durationMillis,int respawnMillis,long elapsedMillis,
                 long gateRevision,List<ModernSiege.Objective> towers) {
        Proof {
            Objects.requireNonNull(end);towers=List.copyOf(towers);
            if(roomId<1||physicalMapId<0||physicalMapId>65535||!hash(terrainSha256)||!hash(configSha256)
                    ||attacker<0||attacker>1||winner<0||winner>1||durationMillis<50||durationMillis>1_800_000
                    ||respawnMillis<1||respawnMillis>60_000||elapsedMillis<0||elapsedMillis>durationMillis
                    ||gateRevision<0||towers.size()!=3)throw bad();
            var cells=new java.util.HashSet<ModernWorldMap.Cell>();
            for(int i=0;i<3;i++) {
                var t=towers.get(i);
                if(t.tower().slot().ordinal()!=i||!cells.add(t.tower().cell())||t.hitCount()>t.revision()
                        ||t.hp()==0&&t.deathRevision()!=t.revision())throw bad();
            }
            boolean outerDead=towers.get(0).hp()==0&&towers.get(1).hp()==0,innerDead=towers.get(2).hp()==0;
            if((gateRevision>0)!=outerDead||outerDead&&gateRevision!=Math.max(towers.get(0).deathRevision(),towers.get(1).deathRevision())
                    ||gateRevision==0&&towers.get(2).hitCount()!=0
                    ||towers.get(2).hitCount()>0&&towers.get(2).revision()<=gateRevision)throw bad();
            if(end==End.INNER_DESTROYED?(!innerDead||!outerDead||winner!=attacker||elapsedMillis>=durationMillis)
                    :(innerDead||winner==attacker||elapsedMillis!=durationMillis))throw bad();
        }
        void validate(int mode,int logicalMap,long revision,List<ModernMatchSettlement.Participant> rows) {
            if(mode!=3||!java.util.Set.of(547,563,573).contains(logicalMap)||revision<1||gateRevision>=revision)throw bad();
            int[] teams={0,0};
            for(var p:rows){teams[p.team()]++;if(p.verdict()!=(p.team()==winner?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS))throw bad();}
            if(teams[0]<15||teams[1]<15||teams[0]>50||teams[1]>50)throw bad();
            for(var t:towers) {
                if(t.revision()>revision||t.lastActor()>rows.size()
                        ||t.hitCount()>0&&rows.get(Math.toIntExact(t.lastActor()-1)).team()!=attacker
                        ||(t.tower().slot()!=ModernSiege.Slot.INNER||end==End.TIMEOUT)&&t.revision()>=revision)throw bad();
            }
            if(end==End.INNER_DESTROYED&&towers.get(2).deathRevision()!=revision)throw bad();
        }
        @Override public String toString(){return "SiegeResultProof[redacted]";}
    }
    static ModernMatchSettlement.Event event(ModernBattleEngine.Snapshot world,UUID id,long recordedAtMillis) {
        world.siege().validate(world);
        if(!world.siege().finished()||world.phase()!=ModernBattleEngine.Phase.FINISHED)throw bad();
        var state=world.siege().state();var config=state.config();
        if(state.finishRevision()!=world.revision())throw bad();
        var proof=new Proof(state.scope().roomId(),world.manifest().mapId(),world.manifest().digest(),configHash(config),state.attacker(),state.winner(),
                state.winner()==state.attacker()?End.INNER_DESTROYED:End.TIMEOUT,Math.toIntExact(config.durationMillis()),Math.toIntExact(config.respawnMillis()),
                state.finishedAtMillis(),state.gateRevision(),state.objectives());
        var participants=world.actors().stream().map(a->new ModernMatchSettlement.Participant(a.entityId(),a.member().identity(),a.member().team(),
                a.member().team()==state.winner()?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        return new ModernMatchSettlement.Event(id,world.matchId(),world.generation(),world.revision(),3,config.key().logicalMapId(),recordedAtMillis,participants,null,proof);
    }
    /** Canonical observation overload: no legacy spell/capacity frame needed. */
    static ModernMatchSettlement.Event event(ModernBattleObservation.World world,UUID id,long recordedAtMillis) {
        var state=world.modes().siege().state();
        if(state==null||!state.finished()||world.phase()!=ModernBattleEngine.Phase.FINISHED||state.finishRevision()!=world.revision())throw bad();
        var config=state.config();
        var proof=new Proof(state.scope().roomId(),world.manifest().mapId(),world.manifest().digest(),configHash(config),state.attacker(),state.winner(),
                state.winner()==state.attacker()?End.INNER_DESTROYED:End.TIMEOUT,Math.toIntExact(config.durationMillis()),Math.toIntExact(config.respawnMillis()),
                state.finishedAtMillis(),state.gateRevision(),state.objectives());
        var rows=world.actors().stream().map(a->new ModernMatchSettlement.Participant(a.pose().entityId(),a.pose().member().identity(),a.pose().member().team(),
                a.pose().member().team()==state.winner()?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        return new ModernMatchSettlement.Event(id,world.identity().matchId(),world.identity().generation(),world.revision(),3,config.key().logicalMapId(),recordedAtMillis,rows,null,proof);
    }
    /** Ordered camp preference and all configured geometry/HP/timing are
     * fingerprinted. Gate SET enumeration order must not change the identity. */
    static String configHash(ModernSiege.Config config) {
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            out.writeUTF("modern-siege-policy-v1");out.writeInt(config.key().mode());out.writeInt(config.key().logicalMapId());
            for(var t:config.towers()){out.writeByte(t.slot().ordinal());cell(out,t.cell());out.writeInt(t.gfx());out.writeLong(t.maximumHp());}
            var gate=config.gate().stream().sorted(Comparator.comparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).toList();
            out.writeInt(gate.size());for(var c:gate)cell(out,c);
            for(var camp:List.of(config.attackCamp(),config.defenseCamp())){out.writeInt(camp.size());for(var c:camp)cell(out,c);}
            cell(out,config.portal().outside());cell(out,config.portal().inside());out.writeLong(config.durationMillis());out.writeLong(config.respawnMillis());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(IOException|NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    static void write(DataOutputStream out,Proof p)throws IOException {
        out.writeInt(p.physicalMapId());out.write(HexFormat.of().parseHex(p.terrainSha256()));out.write(HexFormat.of().parseHex(p.configSha256()));out.writeLong(p.roomId());
        out.writeByte(p.attacker());out.writeByte(p.winner());out.writeByte(p.end().ordinal());out.writeByte(0);
        out.writeInt(p.durationMillis());out.writeInt(p.respawnMillis());out.writeLong(p.elapsedMillis());out.writeLong(p.gateRevision());
        for(var t:p.towers()) {
            cell(out,t.tower().cell());out.writeInt(t.tower().gfx());out.writeLong(t.tower().maximumHp());out.writeLong(t.hp());out.writeLong(t.damageLoss());
            out.writeLong(t.hitCount());out.writeLong(t.revision());out.writeLong(t.deathRevision());out.writeLong(t.lastActor());
        }
    }
    static Proof read(ByteBuffer in) {
        if(in.remaining()!=PROOF_BYTES)throw bad();
        int map=in.getInt();byte[] terrain=new byte[32],config=new byte[32];in.get(terrain).get(config);long room=in.getLong();
        int attacker=Byte.toUnsignedInt(in.get()),winner=Byte.toUnsignedInt(in.get()),end=Byte.toUnsignedInt(in.get());
        if(in.get()!=0||end>=End.values().length)throw bad();
        int duration=in.getInt(),respawn=in.getInt();long elapsed=in.getLong(),gate=in.getLong();var towers=new ArrayList<ModernSiege.Objective>();
        for(var slot:ModernSiege.Slot.values()) {
            var tower=new ModernSiege.Tower(slot,new ModernWorldMap.Cell(in.getInt(),in.getInt()),in.getInt(),in.getLong());
            towers.add(new ModernSiege.Objective(tower,in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong()));
        }
        return new Proof(room,map,HexFormat.of().formatHex(terrain),HexFormat.of().formatHex(config),attacker,winner,End.values()[end],duration,respawn,elapsed,gate,towers);
    }
    private static void cell(DataOutputStream out,ModernWorldMap.Cell cell)throws IOException {out.writeInt(cell.x());out.writeInt(cell.y());}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}")&&!value.equals("0".repeat(64));}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid siege result proof");}
    private ModernSiegeSettlement(){}
}
