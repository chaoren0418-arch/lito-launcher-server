package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernDisintegrateState.Reason.*;

/** v27 fixed skill77 actor request. No client damage, MP, RNG, lawful or resource
 * address. Both life tokens are explicit; old CastRequest remains unchanged. */
final class ModernDisintegrateProtocol {
    static final int VERSION=27, REQUEST_KIND=44, RESULT_KIND=45, REQUEST_BYTES=72;
    record Reply(ModernSpellProtocol.Status status,ModernDisintegrateState.Reason reason,ModernCombatProtocol.Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==ModernSpellProtocol.Status.OK)!=(reason==NONE)
                    ||frame==null&&!(status==ModernSpellProtocol.Status.UNAVAILABLE&&reason==NOT_ACTIVE)
                    ||frame!=null&&status!=ModernSpellProtocol.Status.OK&&status!=ModernSpellProtocol.Status.DENIED)throw bad();}
    }
    static void validate(ModernApplicationProtocol.DisintegrateResult message) {
        var r=Objects.requireNonNull(message.echo());var reply=Objects.requireNonNull(message.reply());var f=reply.frame();if(f==null)return;
        var w=f.world();
        if(reply.reason()==NOT_ACTIVE&&w.phase()==ModernWorldProtocol.Phase.ACTIVE)throw bad();
        if(w.matchId()!=r.matchId()||w.generation()!=r.generation()||w.descriptor()==null||!w.descriptor().has(ModernMatchDescriptor.DISINTEGRATE))throw bad();
        var self=ModernParticipantId.human(r.scope().sourceId(),r.scope().objectId());
        var actor=w.actors().stream().filter(a->a.member().identity().equals(self)).findFirst().orElseThrow(ModernDisintegrateProtocol::bad);
        if(actor.entityId()==r.targetEntityId()||w.actors().stream().noneMatch(a->a.entityId()==r.targetEntityId()))throw bad();
        var v=f.vital(actor.entityId());var target=f.vital(r.targetEntityId());
        var state=f.disintegrations().get(Math.toIntExact(actor.entityId()-1));
        var clock=w.casts().get(Math.toIntExact(actor.entityId()-1));
        if(reply.status()==ModernSpellProtocol.Status.OK) {
            var h=state.hit();
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE&&w.phase()!=ModernWorldProtocol.Phase.FINISHED
                    ||v.hp()==0||state.revision()!=w.revision()||state.lastSequence()!=r.sequence()||clock.requestSequence()!=r.sequence()
                    ||h==null||h.targetEntityId()!=r.targetEntityId()||h.casterLife()!=r.casterLife()||h.targetLife()!=r.targetLife())throw bad();
        } else if(reply.status()==ModernSpellProtocol.Status.DENIED&&reply.reason()!=NOT_ACTIVE) {
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||(reply.reason()==STALE_STATE?clock.requestSequence()<r.sequence():clock.requestSequence()!=r.sequence()))throw bad();
            if(reply.reason()==DEAD&&v.hp()!=0||reply.reason()==TARGET_DEAD&&target.hp()!=0
                    ||reply.reason()==NO_MP&&v.mp()>=state.mpCost()||reply.reason()==LAWFUL&&state.lawful()>=500)throw bad();
        }
    }
    static void putRequest(ByteBuffer out,ModernDisintegrateState.Request r) {
        var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId());zeroes(out,3);
        out.putLong(r.matchId()).putLong(r.generation()).putLong(r.sequence()).putLong(r.casterLife()).putLong(r.targetEntityId()).putLong(r.targetLife());
    }
    static ModernDisintegrateState.Request getRequest(ByteBuffer in) {
        int source=in.getInt();long obj=in.getLong(),epoch=in.getLong();int channel=Byte.toUnsignedInt(in.get());zero(in,3);
        return new ModernDisintegrateState.Request(new ModernRooms.Scope(source,obj,epoch,channel),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong());
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.DisintegrateResult m,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();validate(m);putRequest(out,m.echo());var r=m.reply();
        out.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)(r.frame()==null?0:1)).put((byte)0);
        if(r.frame()!=null)ModernCombatProtocol.putFrame(out,r.frame(),version);
    }
    static ModernApplicationProtocol.DisintegrateResult getResult(ByteBuffer in,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();var request=getRequest(in);int status=Byte.toUnsignedInt(in.get()),reason=Byte.toUnsignedInt(in.get()),present=Byte.toUnsignedInt(in.get());zero(in,1);
        if(status>=ModernSpellProtocol.Status.values().length||reason>=ModernDisintegrateState.Reason.values().length||present>1)throw bad();
        return new ModernApplicationProtocol.DisintegrateResult(id,request,new Reply(ModernSpellProtocol.Status.values()[status],ModernDisintegrateState.Reason.values()[reason],present==1?ModernCombatProtocol.getFrame(in,version):null));
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&(ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))
                ==(ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR);
    }
    private static void zeroes(ByteBuffer b,int n){while(n-->0)b.put((byte)0);}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Disintegrate wire contract");}
    private ModernDisintegrateProtocol(){}
}
