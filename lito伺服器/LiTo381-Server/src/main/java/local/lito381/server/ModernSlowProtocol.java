package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** v47 targeted skill29 with owner-private material proof. All authority is server-owned; the request contains
 * only the observed caster and target identities/stamps, never cost or time. */
final class ModernSlowProtocol {
    static final int VERSION=47, REQUEST_KIND=76, RESULT_KIND=77, REQUEST_BYTES=184, RESULT_PREFIX_BYTES=216;
    static final int MAX_RESULT_BYTES=RESULT_PREFIX_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    static boolean negotiated(ModernSessionBoundary.Negotiated p){return ModernBattleSpeedWire.negotiated(p)&&p.version()>=VERSION;}
    record Request(int requestId,ModernBattleCommandLife.Proof proof,long sequence,long equipmentRevision,
                   int x,int y,long buffRevision,ModernSlowCast.Target target) {
        Request {
            Objects.requireNonNull(proof);Objects.requireNonNull(target);
            if(requestId==0||sequence<1||equipmentRevision<0||buffRevision<0||buffRevision>proof.revision())throw bad();
            if(target instanceof ModernSlowCast.Player p&&p.buffRevision()>proof.revision())throw bad();
            if(target instanceof ModernSlowCast.Npc n&&(!proof.world().contains(n.id().scope())||n.changedRevision()>proof.revision()))throw bad();
        }
        ModernSlowCast.Request command(){return new ModernSlowCast.Request(proof.player(),proof.world().matchId(),proof.world().generation(),sequence,
                equipmentRevision,proof.life(),new ModernWorldMap.Cell(x,y),buffRevision,target);}
        @Override public String toString(){return "SlowIntent[redacted]";}
    }
    record Reply(Request echo,ModernSlowCast.Reason reason,ModernBattleAudience.View view,ModernBattleLifecycle lifecycle) {
        Reply {Objects.requireNonNull(echo);Objects.requireNonNull(reason);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);validate(echo,reason,view,lifecycle);}
        @Override public String toString(){return "SlowReceipt[redacted]";}
    }
    static byte[] encodeRequest(Request q,int version) {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION)throw bad();
        var b=ByteBuffer.allocate(REQUEST_BYTES);var p=q.proof();var s=p.player();var w=p.world();
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(q.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());
        b.putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        b.putLong(q.sequence()).putLong(q.equipmentRevision()).putInt(q.x()).putInt(q.y()).putLong(q.buffRevision());
        if(q.target() instanceof ModernSlowCast.Player t) {
            b.putLong(0);var identity=t.participant().identity();
            b.putLong(t.participant().entityId()).putInt(identity.kind().ordinal()).putInt(identity.sourceId()).putLong(identity.id());
            b.putLong(t.life()).putLong(t.equipmentRevision()).putInt(t.cell().x()).putInt(t.cell().y()).putLong(t.buffRevision());
        } else {
            var t=(ModernSlowCast.Npc)q.target();b.put((byte)1).put(new byte[7]);var scope=t.id().scope();
            b.putLong(scope.roomId()).putLong(scope.matchId()).putLong(scope.generation()).putLong(t.id().serial());
            b.putLong(t.life()).putLong(t.changedRevision()).putInt(t.cell().x()).putInt(t.cell().y());
        }
        return b.array();
    }
    static Request decodeRequest(byte[] bytes,int version) {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||bytes==null||bytes.length!=REQUEST_BYTES)throw bad();
        var b=ByteBuffer.wrap(bytes);if(b.getInt()!=0x4c544d31||b.getInt()!=REQUEST_BYTES)throw bad();int id=b.getInt();
        if(b.getShort()!=REQUEST_KIND||b.getShort()!=0)throw bad();
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var p=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        long sequence=b.getLong(),gear=b.getLong();int x=b.getInt(),y=b.getInt();long buff=b.getLong();
        int type=Byte.toUnsignedInt(b.get());for(int i=0;i<7;i++)if(b.get()!=0)throw bad();
        ModernSlowCast.Target target;
        if(type==0) {
            long entity=b.getLong();int kind=b.getInt();if(kind<0||kind>=ModernParticipantId.Kind.values().length)throw bad();
            var identity=new ModernParticipantId(ModernParticipantId.Kind.values()[kind],b.getInt(),b.getLong());
            target=new ModernSlowCast.Player(new ModernTargetHistory.Participant(entity,identity),b.getLong(),b.getLong(),new ModernWorldMap.Cell(b.getInt(),b.getInt()),b.getLong());
        } else if(type==1) {
            var scope=new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong());
            target=new ModernSlowCast.Npc(new ModernNpcRoster.Id(scope,b.getLong()),b.getLong(),b.getLong(),new ModernWorldMap.Cell(b.getInt(),b.getInt()));
        } else throw bad();
        return new Request(id,p,sequence,gear,x,y,buff,target);
    }
    private static void validate(Request q,ModernSlowCast.Reason reason,ModernBattleAudience.View v,ModernBattleLifecycle l) {
        var p=q.proof();var w=v.world();
        if(!p.player().equals(v.personal().scope())||!p.world().equals(w.identity())||p.entity()!=v.personal().entityId()
                ||p.revision()>w.revision()||!w.descriptor().has(ModernMatchDescriptor.SLOW)
                ||l.revision()<=w.revision()||!Objects.equals(l.result(),w.result()))throw bad();
        boolean overlay=l.phase()==ModernWorldProtocol.Phase.ABORTED&&w.phase()!=ModernBattleEngine.Phase.FINISHED&&w.result()==null;
        if(!overlay&&(l.phase()!=ModernWorldProtocol.Phase.valueOf(w.phase().name())
                ||l.abortReason()!=ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name())))throw bad();
        var a=w.actors().stream().filter(actor->actor.pose().entityId()==p.entity()).findFirst().orElseThrow(ModernSlowProtocol::bad);
        var r=a.spells().receipts().get(ModernUseHistory.Lane.SLOW);
        var materials=Objects.requireNonNull(v.personal().materials());
        if(reason==ModernSlowCast.Reason.NOT_ACTIVE) {
            if(l.phase()==ModernWorldProtocol.Phase.ACTIVE&&w.phase()==ModernBattleEngine.Phase.ACTIVE)throw bad();return;
        }
        if(w.phase()!=ModernBattleEngine.Phase.ACTIVE||a.spells().sequence()!=q.sequence()
                ||a.spells().requestRevision()!=w.revision()||w.revision()<=p.revision())throw bad();
        if(reason==ModernSlowCast.Reason.NONE) {
            if(r==null||r.impact().use().sequence()!=q.sequence()||r.impact().use().revision()!=w.revision()
                    ||r.impact().use().before().equipmentRevision()!=q.equipmentRevision()||r.impact().use().before().life()!=p.life()
                    ||r.impact().use().before().x()!=q.x()||r.impact().use().before().y()!=q.y()
                    ||!(r.detail() instanceof ModernSlowReceipt d)
                    ||materials.lastSequence()!=q.sequence()||materials.revision()!=w.revision()
                    ||materials.lastLife()!=p.life()||materials.lastEquipmentRevision()!=q.equipmentRevision())throw bad();
            if(q.target() instanceof ModernSlowCast.Player t) {
                if(!(d.target() instanceof ModernSlowReceipt.Player effect)||!t.participant().equals(effect.participant())
                        ||t.life()!=effect.life()||t.equipmentRevision()!=effect.equipmentRevision()||!t.cell().equals(effect.cell())
                        ||t.buffRevision()!=effect.before().revision())throw bad();
            } else {
                var t=(ModernSlowCast.Npc)q.target();
                if(!(d.target() instanceof ModernSlowReceipt.Npc effect)||!t.id().equals(effect.id())||t.life()!=effect.life()
                        ||!t.cell().equals(effect.cell())||t.changedRevision()>effect.rosterRevision())throw bad();
            }
        } else if(r!=null&&r.impact().use().sequence()>=q.sequence()||materials.lastSequence()>=q.sequence())throw bad();
        if(reason==ModernSlowCast.Reason.DEAD&&a.vital().values().hp()!=0)throw bad();
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
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid slow contract");}
    private ModernSlowProtocol(){}
}
