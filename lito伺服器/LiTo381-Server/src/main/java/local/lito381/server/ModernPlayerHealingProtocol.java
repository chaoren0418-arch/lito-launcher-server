package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/** Complete skill1 candidate contract. No current negotiated version, listener
 * or application dispatcher admits these opcodes. No client amount/cost/time. */
final class ModernPlayerHealingProtocol {
    static final int REQUEST_KIND=78,RESULT_KIND=79,REQUEST_BYTES=160,RESULT_PREFIX_BYTES=192;
    static final int MAX_RESULT_BYTES=RESULT_PREFIX_BYTES+ModernBattleHealingWire.MAX_BYTES;
    record Request(int requestId,ModernBattleCommandLife.Proof proof,long sequence,long equipmentRevision,
                   int x,int y,ModernPlayerHealingCast.Target target) {
        Request {
            Objects.requireNonNull(proof);Objects.requireNonNull(target);
            if(requestId==0||sequence<1||equipmentRevision<0||target.participant().entityId()>100)throw bad();
            if(target.participant().entityId()==proof.entity()&&(!target.participant().identity().equals(
                    ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))
                    ||target.life()!=proof.life()||target.equipmentRevision()!=equipmentRevision
                    ||target.cell().x()!=x||target.cell().y()!=y))throw bad();
        }
        ModernPlayerHealingCast.Request command(){return new ModernPlayerHealingCast.Request(proof.player(),proof.world().matchId(),proof.world().generation(),
                sequence,equipmentRevision,proof.life(),new ModernWorldMap.Cell(x,y),target);}
        @Override public String toString(){return "PlayerHealingIntent[redacted]";}
    }
    record Reply(Request echo,ModernPlayerHealingCast.Reason reason,ModernBattleAudience.View view,ModernBattleLifecycle lifecycle) {
        Reply {Objects.requireNonNull(echo);Objects.requireNonNull(reason);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);validate(echo,reason,view,lifecycle);}
        @Override public String toString(){return "PlayerHealingReceipt[redacted]";}
    }
    static byte[] encodeCandidateRequest(Request q) {
        var b=ByteBuffer.allocate(REQUEST_BYTES);var p=q.proof();var s=p.player();var w=p.world();var t=q.target();var id=t.participant().identity();
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(q.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation()).putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        b.putLong(q.sequence()).putLong(q.equipmentRevision()).putInt(q.x()).putInt(q.y());
        b.putLong(t.participant().entityId()).putInt(id.kind().ordinal()).putInt(id.sourceId()).putLong(id.id());
        b.putLong(t.life()).putLong(t.equipmentRevision()).putInt(t.cell().x()).putInt(t.cell().y());
        return b.array();
    }
    static Request decodeCandidateRequest(byte[] bytes) {
        if(bytes==null||bytes.length!=REQUEST_BYTES)throw bad();var b=ByteBuffer.wrap(bytes);
        if(b.getInt()!=0x4c544d31||b.getInt()!=REQUEST_BYTES)throw bad();int id=b.getInt();
        if(b.getShort()!=REQUEST_KIND||b.getShort()!=0)throw bad();
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var proof=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        long sequence=b.getLong(),gear=b.getLong();int x=b.getInt(),y=b.getInt();
        long entity=b.getLong();int kind=b.getInt();if(kind<0||kind>=ModernParticipantId.Kind.values().length)throw bad();
        var participant=new ModernTargetHistory.Participant(entity,new ModernParticipantId(ModernParticipantId.Kind.values()[kind],b.getInt(),b.getLong()));
        var target=new ModernPlayerHealingCast.Target(participant,b.getLong(),b.getLong(),new ModernWorldMap.Cell(b.getInt(),b.getInt()));
        return new Request(id,proof,sequence,gear,x,y,target);
    }
    /** Check fixed header before allocation; coalesced next requests stay unread. */
    static Request readCandidateRequest(InputStream input)throws IOException {
        var header=input.readNBytes(16);if(header.length!=16)throw bad();var h=ByteBuffer.wrap(header);
        if(h.getInt()!=0x4c544d31||h.getInt()!=REQUEST_BYTES||h.getInt()==0||h.getShort()!=REQUEST_KIND||h.getShort()!=0)throw bad();
        var body=input.readNBytes(REQUEST_BYTES-16);if(body.length!=REQUEST_BYTES-16)throw bad();
        var b=ByteBuffer.allocate(REQUEST_BYTES).put(header).put(body);return decodeCandidateRequest(b.array());
    }
    private static void validate(Request q,ModernPlayerHealingCast.Reason reason,ModernBattleAudience.View v,ModernBattleLifecycle l) {
        var p=q.proof();var w=v.world();
        if(!p.player().equals(v.personal().scope())||!p.world().equals(w.identity())||p.entity()!=v.personal().entityId()
                ||p.revision()>w.revision()||!w.descriptor().has(ModernMatchDescriptor.PLAYER_HEALING)
                ||l.revision()<=w.revision()||!Objects.equals(l.result(),w.result()))throw bad();
        boolean overlay=l.phase()==ModernWorldProtocol.Phase.ABORTED&&w.phase()!=ModernBattleEngine.Phase.FINISHED&&w.result()==null;
        if(!overlay&&(l.phase()!=ModernWorldProtocol.Phase.valueOf(w.phase().name())
                ||l.abortReason()!=ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name())))throw bad();
        var a=w.actors().stream().filter(actor->actor.pose().entityId()==p.entity()).findFirst().orElseThrow(ModernPlayerHealingProtocol::bad);
        if(a.playerHealing()==null||a.playerHealing().outgoing()==null)throw bad();
        var r=a.playerHealing().receipt();
        if(reason==ModernPlayerHealingCast.Reason.NOT_ACTIVE) {
            if(l.phase()==ModernWorldProtocol.Phase.ACTIVE&&w.phase()==ModernBattleEngine.Phase.ACTIVE)throw bad();return;
        }
        if(w.phase()!=ModernBattleEngine.Phase.ACTIVE||a.spells().sequence()!=q.sequence()
                ||a.spells().requestRevision()!=w.revision()||w.revision()<=p.revision())throw bad();
        if(reason==ModernPlayerHealingCast.Reason.NONE) {
            if(r==null)throw bad();var i=r.impact();var u=i.use();var t=q.target();
            if(u.sequence()!=q.sequence()||u.revision()!=w.revision()||u.before().equipmentRevision()!=q.equipmentRevision()
                    ||u.before().life()!=p.life()||u.before().x()!=q.x()||u.before().y()!=q.y()
                    ||!i.recipient().equals(t.participant())||i.target().before().life()!=t.life()
                    ||i.target().before().equipmentRevision()!=t.equipmentRevision()
                    ||!i.target().before().cell().equals(t.cell()))throw bad();
            var target=w.actors().stream().filter(actor->actor.pose().entityId()==t.participant().entityId()).findFirst().orElseThrow(ModernPlayerHealingProtocol::bad);
            if(!i.equals(target.playerHealing().incoming().last()))throw bad();
        } else if(r!=null&&r.impact().use().sequence()>=q.sequence())throw bad();
        if(reason==ModernPlayerHealingCast.Reason.DEAD&&a.vital().values().hp()!=0)throw bad();
    }
    static byte[] encodeCandidateReply(Reply r) {
        return encodeReply(r,ModernBattleWire.encodePlayerHealing(r.view()),MAX_RESULT_BYTES);
    }
    static byte[] encodeReply(Reply r,ModernSessionBoundary.Negotiated peer) {
        if(!ModernSessionExtensions.allowsCommands(peer))throw bad();
        var format=ModernBattleWire.Encoding.selected(peer);
        return encodeReply(r,format.payload(r.view()),Math.addExact(RESULT_PREFIX_BYTES,format.maximum()));
    }
    private static byte[] encodeReply(Reply r,byte[] data,int maximum) {
        var echo=encodeCandidateRequest(r.echo());
        int length=Math.addExact(RESULT_PREFIX_BYTES,data.length);if(length>maximum)throw bad();var b=ByteBuffer.allocate(length);
        b.put(echo).putInt(4,length).putShort(12,(short)RESULT_KIND).put((byte)r.reason().ordinal()).put(new byte[3]);
        ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());b.putInt(data.length).put(data);return b.array();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid player healing contract");}
    private ModernPlayerHealingProtocol(){}
}
