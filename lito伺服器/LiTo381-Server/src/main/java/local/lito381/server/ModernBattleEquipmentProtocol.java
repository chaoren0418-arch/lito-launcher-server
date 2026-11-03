package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernApplicationProtocol.*;

/** v41 live SET: exact selection/world/life plus the existing equipment CAS.
 * Durable-but-retired is explicit, never encoded as rollback or live success.
 * Legacy kinds 16/17 retain their old, outside-world meaning. */
final class ModernBattleEquipmentProtocol {
    static final int VERSION=41, REQUEST_KIND=70, RESULT_KIND=71, REQUEST_BYTES=160;
    static final int RESULT_PREFIX_BYTES=204;
    static final int MAX_RESULT_BYTES=RESULT_PREFIX_BYTES+ModernEquipmentProtocol.MAX_FRAME_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    enum Reason { NONE, NOT_ACTIVE, STALE_LIFE, DEAD, BUSY, UNAVAILABLE }
    enum Disposition { NOT_COMMITTED, UNCHANGED, APPLIED, PERSISTED_RETIRED }
    record Request(ModernBattleCommandLife.Proof proof,ModernEquipmentAuthority.Request equipment) {
        Request {
            Objects.requireNonNull(proof);Objects.requireNonNull(equipment);
            var p=proof.player();
            if(equipment.action()!=ModernEquipmentAuthority.SET||p.sourceId()!=equipment.sourceId()
                    ||p.objectId()!=equipment.objectId()||p.epoch()!=equipment.epoch())throw bad();
        }
        @Override public String toString(){return "BattleEquipmentRequest[redacted]";}
    }
    record Reply(Reason reason,Disposition disposition,long durableRevision,ModernEquipmentAuthority.Result equipment,
                 ModernBattleAudience.View view,ModernBattleLifecycle lifecycle) {
        Reply {Objects.requireNonNull(reason);Objects.requireNonNull(disposition);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);}
        @Override public String toString(){return "BattleEquipmentReply[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated peer) {
        return peer!=null&&peer.version()>=VERSION&&ModernBattleWorldProtocol.negotiated(peer)
                &&(peer.capabilities()&ModernSessionProtocol.CAP_EQUIPMENT)!=0;
    }
    static byte[] encodeRequest(BattleEquipmentRequest message,int version)throws ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||message.requestId()==0)throw new ProtocolException();
        var q=message.request();var p=q.proof();var s=p.player();var w=p.world();var b=ByteBuffer.allocate(REQUEST_BYTES);
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(message.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());
        b.putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        ModernEquipmentProtocol.putRequest(b,q.equipment());return b.array();
    }
    static Request getRequest(ByteBuffer b)throws ProtocolException {
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var p=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        return new Request(p,ModernEquipmentProtocol.getRequest(b));
    }
    static long life(ModernBattleAudience.View view) {
        // The command proof uses the shared resource ledger for quest, honor
        // and siege lives. Non-quest does not imply the first incarnation.
        var actor=view.world().actors().stream().filter(a->a.pose().entityId()==view.personal().entityId())
                .findFirst().orElseThrow(ModernBattleEquipmentProtocol::bad);
        return actor.resources()==null?1:Math.incrementExact(actor.resources().life());
    }
    static void validate(BattleEquipmentResult message)throws ProtocolException {
        var q=message.echo();var p=q.proof();var r=message.reply();var v=r.view();var w=v.world();var l=r.lifecycle();
        if(message.requestId()==0||!p.player().equals(v.personal().scope())||!p.world().equals(w.identity())
                ||p.entity()!=v.personal().entityId()||p.revision()>w.revision()||l.revision()<=w.revision()
                ||!Objects.equals(l.result(),w.result()))throw bad();
        if(r.reason()!=Reason.NONE) {
            if(r.equipment()!=null||r.disposition()!=Disposition.NOT_COMMITTED||r.durableRevision()!=-1)throw bad();
            if(r.reason()==Reason.NOT_ACTIVE&&l.phase()==ModernWorldProtocol.Phase.ACTIVE)throw bad();
            if(r.reason()==Reason.STALE_LIFE&&life(v)==p.life())throw bad();
            if(r.reason()==Reason.DEAD&&(life(v)!=p.life()||w.actors().stream().filter(a->a.pose().entityId()==p.entity())
                    .anyMatch(a->a.vital()==null||a.vital().values().hp()!=0)))throw bad();
            return;
        }
        var e=Objects.requireNonNull(r.equipment());
        ModernEquipmentProtocol.validate(new EquipmentResult(message.requestId(),q.equipment(),e));
        switch(r.disposition()) {
            case NOT_COMMITTED -> {if(e.status()==ModernEquipmentAuthority.OK||r.durableRevision()!=-1)throw bad();}
            case UNCHANGED,APPLIED -> {
                boolean changed=r.disposition()==Disposition.APPLIED;
                var personal=v.personal().equipment();
                if(e.status()!=ModernEquipmentAuthority.OK||e.changed()!=changed||e.snapshot()==null||personal==null
                        ||r.durableRevision()!=e.snapshot().revision()||personal.revision()!=r.durableRevision()
                        ||!personal.source().equals(e.snapshot().source())||!personal.equipped().equals(e.snapshot().equipped())
                        ||!personal.lifetimes().equals(e.snapshot().lifetimes())||life(v)!=p.life()
                        ||changed&&w.revision()<=p.revision())throw bad();
            }
            case PERSISTED_RETIRED -> {
                if(q.equipment().expectedRevision()==Long.MAX_VALUE||r.durableRevision()!=q.equipment().expectedRevision()+1
                        ||e.status()!=ModernEquipmentAuthority.BACKEND_UNAVAILABLE||e.snapshot()!=null
                        ||v.personal().equipment()==null||v.personal().equipment().revision()!=q.equipment().expectedRevision()
                        ||l.phase()!=ModernWorldProtocol.Phase.ABORTED&&l.phase()!=ModernWorldProtocol.Phase.FINISHED)throw bad();
            }
        }
    }
    static byte[] encodeResult(BattleEquipmentResult message,int version)throws ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeResult(BattleEquipmentResult message,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeResult(BattleEquipmentResult message,ModernBattleWire.Encoding format)throws ProtocolException {
        int version=format.version();
        validate(message);var r=message.reply();var echo=encodeRequest(new BattleEquipmentRequest(message.requestId(),message.echo()),version);
        var equipment=r.equipment()==null?new byte[0]:ModernApplicationProtocol.encode(new EquipmentResult(message.requestId(),message.echo().equipment(),r.equipment()),version);
        var payload=format.payload(r.view());
        int size=Math.addExact(RESULT_PREFIX_BYTES,Math.addExact(equipment.length,payload.length));
        if(size>format.resultLimit(MAX_RESULT_BYTES))throw new ProtocolException();
        var b=ByteBuffer.allocate(size);b.put(echo).putInt(4,size).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.reason().ordinal()).put((byte)r.disposition().ordinal()).putShort((short)0).putLong(r.durableRevision());
        ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());
        b.putInt(equipment.length).putInt(payload.length).put(equipment).put(payload);return b.array();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid live equipment receipt");}
    private ModernBattleEquipmentProtocol(){}
}
