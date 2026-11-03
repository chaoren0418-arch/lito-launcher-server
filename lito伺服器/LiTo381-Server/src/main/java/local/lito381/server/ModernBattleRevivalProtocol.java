package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernApplicationProtocol.*;

/** v40 exact-death confirmation. Canonical revisions only; never a living
 * attack envelope, legacy NPC ACK, LOAD permit, or automatic respawn. */
final class ModernBattleRevivalProtocol {
    static final int VERSION=40, REQUEST_KIND=68, RESULT_KIND=69, REQUEST_BYTES=96, TRAILER_BYTES=32;
    static final int MIN_RESULT_BYTES=REQUEST_BYTES+TRAILER_BYTES;
    static final int MAX_RESULT_BYTES=MIN_RESULT_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    enum Reason { REVIVED, NOT_ACTIVE, NOT_DEAD, STALE_LIFE, SPAWN_BLOCKED, NUMERIC }
    record Request(ModernBattleCommandLife.Proof proof,long deathRevision) {
        Request {
            Objects.requireNonNull(proof);
            if(deathRevision<1||deathRevision>proof.revision())throw bad();
        }
        @Override public String toString(){return "BattleRevivalRequest[redacted]";}
    }
    record Reply(Reason reason,ModernBattleAudience.View view,ModernBattleLifecycle lifecycle) {
        Reply {Objects.requireNonNull(reason);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);}
        @Override public String toString(){return "BattleRevivalReply[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated peer) {
        return peer!=null&&peer.version()>=VERSION&&ModernBattleWorldProtocol.negotiated(peer);
    }
    static byte[] encodeRequest(BattleRevivalRequest message,int version)throws ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||message.requestId()==0)throw new ProtocolException();
        var q=message.request();var p=q.proof();var s=p.player();var w=p.world();var b=ByteBuffer.allocate(REQUEST_BYTES);
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(message.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());
        b.putLong(p.entity()).putLong(p.life()).putLong(p.revision()).putLong(q.deathRevision());return b.array();
    }
    static Request getRequest(ByteBuffer b) {
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        return new Request(new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong()),b.getLong());
    }
    static void validate(BattleRevivalResult message) {
        var q=message.echo();var p=q.proof();var r=message.reply();var v=r.view();var w=v.world();var l=r.lifecycle();
        if(message.requestId()==0||!p.player().equals(v.personal().scope())||!p.world().equals(w.identity())
                ||p.entity()!=v.personal().entityId()||p.revision()>w.revision()
                ||!w.descriptor().has(ModernMatchDescriptor.QUEST_LIVES)||l.revision()<=w.revision()
                ||!Objects.equals(l.result(),w.result()))throw bad();
        var quest=w.modes().quest();if(quest==null)throw bad();
        var life=quest.lives().stream().filter(a->a.entityId()==p.entity()).findFirst().orElseThrow(ModernBattleRevivalProtocol::bad);
        var actor=w.actors().stream().filter(a->a.pose().entityId()==p.entity()).findFirst().orElseThrow(ModernBattleRevivalProtocol::bad);
        boolean same=life.incarnation()==p.life()&&life.deathRevision()==q.deathRevision();
        if(r.reason()==Reason.NOT_ACTIVE) {
            if(l.phase()==ModernWorldProtocol.Phase.ACTIVE)throw bad();
            return;
        }
        // A later service cancellation may retain this exact successful or
        // denied engine capture. It hides presentation, not the real receipt.
        if(l.phase()!=ModernWorldProtocol.Phase.ACTIVE&&l.phase()!=ModernWorldProtocol.Phase.ABORTED
                ||w.phase()!=ModernBattleEngine.Phase.ACTIVE)throw bad();
        switch(r.reason()) {
            case REVIVED -> {
                if(p.life()==Long.MAX_VALUE||life.incarnation()!=p.life()+1||life.dead()
                        ||life.deathRevision()!=q.deathRevision()||life.respawnRevision()!=w.revision()||w.revision()<=p.revision()
                        ||actor.vital()==null||actor.vital().values().hp()!=actor.vital().values().maximumHp()
                        ||!actor.pose().position().equals(life.spawn()))throw bad();
            }
            case NOT_DEAD -> {if(!same||life.dead())throw bad();}
            case STALE_LIFE -> {if(same)throw bad();}
            case SPAWN_BLOCKED,NUMERIC -> {if(!same||!life.dead())throw bad();}
            default -> throw bad();
        }
    }
    static byte[] encodeResult(BattleRevivalResult message,int version)throws ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeResult(BattleRevivalResult message,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeResult(BattleRevivalResult message,ModernBattleWire.Encoding format)throws ProtocolException {
        int version=format.version();
        validate(message);var r=message.reply();var echo=encodeRequest(new BattleRevivalRequest(message.requestId(),message.echo()),version);
        var payload=format.payload(r.view());
        var b=ByteBuffer.allocate(Math.addExact(MIN_RESULT_BYTES,payload.length));b.put(echo).putInt(4,b.capacity()).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.reason().ordinal()).put(new byte[3]);ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());
        b.putInt(payload.length).put(payload);return b.array();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical death confirmation");}
    private ModernBattleRevivalProtocol(){}
}
