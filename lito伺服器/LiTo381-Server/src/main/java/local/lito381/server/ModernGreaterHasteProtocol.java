package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Explicitly negotiated v42 self-cast contract. Provider/owner and client
 * scene admission remain separate from successful wire negotiation.
 * No target, client clock, price, duration or optimistic success is accepted. */
final class ModernGreaterHasteProtocol {
    static final int VERSION=42,REQUEST_KIND=72,RESULT_KIND=73,REQUEST_BYTES=120,RESULT_PREFIX_BYTES=152;
    static final int MAX_RESULT_BYTES=RESULT_PREFIX_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        int required=ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_TIMED
                |ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&required)==required;
    }
    /** Schema selection is per world, not a global version upgrade. */
    static int dataSchema(ModernBattleAudience.View v,int version) {
        if(v.world().descriptor().has(ModernMatchDescriptor.GREATER_HASTE)) {
            if(version<VERSION)throw bad();return ModernBattleWire.GREATER_HASTE_SCHEMA;
        }
        return version>=ModernBattleReadProtocol.PRESENTATION_VERSION?ModernBattleWire.PRESENTATION_SCHEMA:ModernBattleWire.SCHEMA;
    }
    record Request(int requestId,ModernBattleCommandLife.Proof proof,long sequence,long equipmentRevision,
                   int x,int y,long buffRevision) {
        Request {
            Objects.requireNonNull(proof);
            if(requestId==0||sequence<1||equipmentRevision<0||buffRevision<0||buffRevision>proof.revision())throw bad();
        }
        ModernGreaterHasteCast.Request command() {
            var p=proof;return new ModernGreaterHasteCast.Request(p.player(),p.world().matchId(),p.world().generation(),
                    sequence,equipmentRevision,p.life(),x,y,buffRevision);
        }
        @Override public String toString(){return "GreaterHasteIntent[redacted]";}
    }
    record Reply(Request echo,ModernGreaterHasteCast.Reason reason,ModernBattleAudience.View view,ModernBattleLifecycle lifecycle) {
        Reply {Objects.requireNonNull(echo);Objects.requireNonNull(reason);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);validate(echo,reason,view,lifecycle);}
        @Override public String toString(){return "GreaterHasteReceipt[redacted]";}
    }
    static byte[] encodeRequest(Request q,int version) {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION)throw bad();var p=q.proof();var s=p.player();var w=p.world();var b=ByteBuffer.allocate(REQUEST_BYTES);
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(q.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());
        b.putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        b.putLong(q.sequence()).putLong(q.equipmentRevision()).putInt(q.x()).putInt(q.y()).putLong(q.buffRevision());return b.array();
    }
    static Request decodeRequest(byte[] bytes,int version) {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||bytes==null||bytes.length!=REQUEST_BYTES)throw bad();
        var b=ByteBuffer.wrap(bytes);if(b.getInt()!=0x4c544d31||b.getInt()!=REQUEST_BYTES)throw bad();int id=b.getInt();
        if(b.getShort()!=REQUEST_KIND||b.getShort()!=0)throw bad();
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var p=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        return new Request(id,p,b.getLong(),b.getLong(),b.getInt(),b.getInt(),b.getLong());
    }
    private static void validate(Request q,ModernGreaterHasteCast.Reason reason,ModernBattleAudience.View v,ModernBattleLifecycle l) {
        var p=q.proof();var w=v.world();
        if(!p.player().equals(v.personal().scope())||!p.world().equals(w.identity())||p.entity()!=v.personal().entityId()
                ||p.revision()>w.revision()||!w.descriptor().has(ModernMatchDescriptor.GREATER_HASTE)
                ||l.revision()<=w.revision()||!Objects.equals(l.result(),w.result()))throw bad();
        boolean overlay=l.phase()==ModernWorldProtocol.Phase.ABORTED&&w.phase()!=ModernBattleEngine.Phase.FINISHED&&w.result()==null;
        if(!overlay&&(l.phase()!=ModernWorldProtocol.Phase.valueOf(w.phase().name())
                ||l.abortReason()!=ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name())))throw bad();
        var a=w.actors().stream().filter(x->x.pose().entityId()==p.entity()).findFirst().orElseThrow(ModernGreaterHasteProtocol::bad);
        var r=a.spells().receipts().get(ModernUseHistory.Lane.GREATER_HASTE);
        if(reason==ModernGreaterHasteCast.Reason.NOT_ACTIVE) {
            if(l.phase()==ModernWorldProtocol.Phase.ACTIVE&&w.phase()==ModernBattleEngine.Phase.ACTIVE)throw bad();return;
        }
        if(w.phase()!=ModernBattleEngine.Phase.ACTIVE||a.spells().sequence()!=q.sequence()
                ||a.spells().requestRevision()!=w.revision()||w.revision()<=p.revision())throw bad();
        if(reason==ModernGreaterHasteCast.Reason.NONE) {
            if(r==null||r.impact().use().sequence()!=q.sequence()||r.impact().use().revision()!=w.revision()
                    ||r.impact().use().before().equipmentRevision()!=q.equipmentRevision()||r.impact().use().before().life()!=p.life()
                    ||r.impact().use().before().x()!=q.x()||r.impact().use().before().y()!=q.y()
                    ||a.buffs()==null||a.buffs().revision()!=w.revision()
                    ||!(a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.GREATER_HASTE
                        ||w.descriptor().has(ModernMatchDescriptor.SLOW)
                            &&r.detail() instanceof ModernSpellRuntime.GreaterHasteNeutralized n
                            &&n.after().entityId()==p.entity()&&n.after().revision()==w.revision()
                            &&a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.NONE
                            &&a.buffs().hasteUntilNanos()==0&&a.buffs().slowUntilNanos()==0))throw bad();
        } else if(r!=null&&r.impact().use().sequence()>=q.sequence())throw bad();
        if(reason==ModernGreaterHasteCast.Reason.DEAD&&a.vital().values().hp()!=0)throw bad();
    }
    static byte[] encodeReply(Reply r,int version) {
        return encodeReply(r,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeReply(Reply r,ModernSessionBoundary.Negotiated peer) {
        return encodeReply(r,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeReply(Reply r,ModernBattleWire.Encoding format) {
        var echo=encodeRequest(r.echo(),format.version());var data=format.payload(r.view());
        int length=Math.addExact(RESULT_PREFIX_BYTES,data.length);if(length>format.resultLimit(MAX_RESULT_BYTES))throw bad();var b=ByteBuffer.allocate(length);
        b.put(echo).putInt(4,length).putShort(12,(short)RESULT_KIND);b.put((byte)r.reason().ordinal()).put(new byte[3]);
        ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());b.putInt(data.length).put(data);return b.array();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid greater haste contract");}
    private ModernGreaterHasteProtocol(){}
}
