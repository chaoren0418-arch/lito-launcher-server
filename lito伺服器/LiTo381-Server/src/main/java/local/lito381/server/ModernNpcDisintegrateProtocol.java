package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;

/** v29 skill77 against a typed NPC. Requests never contain damage, mana or RNG. */
final class ModernNpcDisintegrateProtocol {
    static final int VERSION=29, REQUEST_KIND=46, RESULT_KIND=47, REQUEST_BYTES=80;
    record Reply(ModernNpcProtocol.Status status,ModernNpcDisintegrateState.Reason reason,ModernNpcProtocol.Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==ModernNpcProtocol.Status.OK)!=(reason==NONE)
                    ||frame==null&&!(status==ModernNpcProtocol.Status.UNAVAILABLE&&reason==NOT_ACTIVE)
                    ||frame!=null&&status!=ModernNpcProtocol.Status.OK&&status!=ModernNpcProtocol.Status.DENIED)throw bad();}
    }
    static void validate(ModernApplicationProtocol.NpcDisintegrateResult message) {
        var r=Objects.requireNonNull(message.echo());var reply=Objects.requireNonNull(message.reply());var f=reply.frame();if(f==null)return;
        var w=f.combat().world();
        if(!f.scope().equals(r.target().scope())||w.descriptor()==null||!w.descriptor().has(ModernMatchDescriptor.NPC_DISINTEGRATE)
                ||reply.reason()==NOT_ACTIVE&&w.phase()==ModernWorldProtocol.Phase.ACTIVE)throw bad();
        var id=ModernParticipantId.human(r.player().sourceId(),r.player().objectId());
        var a=w.actors().stream().filter(p->p.member().identity().equals(id)).findFirst().orElseThrow(ModernNpcDisintegrateProtocol::bad);
        var v=f.combat().vital(a.entityId());
        var s=f.combat().npcDisintegrations().stream().filter(p->p.entityId()==a.entityId()).findFirst().orElseThrow(ModernNpcDisintegrateProtocol::bad);
        var c=w.casts().stream().filter(p->p.entityId()==a.entityId()).findFirst().orElseThrow(ModernNpcDisintegrateProtocol::bad);
        var life=w.quest().life(a.entityId());long incarnation=life==null?1:life.incarnation();
        if(reply.status()==ModernNpcProtocol.Status.OK) {
            var h=s.hit();
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE&&w.phase()!=ModernWorldProtocol.Phase.FINISHED
                    ||v.hp()==0||incarnation!=r.casterLife()||s.revision()!=w.revision()
                    ||s.lastSequence()!=r.sequence()||c.requestSequence()!=r.sequence()
                    ||h==null||!h.target().equals(r.target())||h.targetLife()!=r.targetLife()||h.casterLife()!=r.casterLife())throw bad();
        } else if(reply.reason()!=NOT_ACTIVE) {
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||(reply.reason()==STALE_STATE?c.requestSequence()<r.sequence():c.requestSequence()!=r.sequence()))throw bad();
            var target=f.actors().stream().filter(n->n.serial()==r.target().serial()).findFirst().orElse(null);
            if(reply.reason()==DEAD&&v.hp()!=0||reply.reason()==NO_MP&&v.mp()>=s.mpCost()
                    ||reply.reason()==LAWFUL&&s.lawful()>=500||reply.reason()==TARGET_DEAD&&target!=null&&target.hp()>0)throw bad();
        }
    }
    static void putRequest(ByteBuffer b,ModernNpcDisintegrateState.Request r,int version){ModernNpcDisintegrateWire.putRequest(b,r,version);}
    static ModernNpcDisintegrateState.Request getRequest(ByteBuffer b,int version){return ModernNpcDisintegrateWire.getRequest(b,version);}
    static void putResult(ByteBuffer b,ModernApplicationProtocol.NpcDisintegrateResult m,int version)throws ModernApplicationProtocol.ProtocolException {
        validate(m);putRequest(b,m.echo(),version);var r=m.reply();
        b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)(r.frame()==null?0:1)).put((byte)0);
        if(r.frame()!=null)ModernNpcProtocol.putFrame(b,r.frame(),version);
    }
    static ModernApplicationProtocol.NpcDisintegrateResult getResult(ByteBuffer b,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        var request=getRequest(b,version);int status=Byte.toUnsignedInt(b.get()),reason=Byte.toUnsignedInt(b.get()),present=Byte.toUnsignedInt(b.get());
        if(b.get()!=0||status>=ModernNpcProtocol.Status.values().length||reason>=ModernNpcDisintegrateState.Reason.values().length||present>1)throw bad();
        return new ModernApplicationProtocol.NpcDisintegrateResult(id,request,new Reply(ModernNpcProtocol.Status.values()[status],ModernNpcDisintegrateState.Reason.values()[reason],present==1?ModernNpcProtocol.getFrame(b,version):null));
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        int required=ModernSessionProtocol.CAP_NPC_DISINTEGRATE|ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_REGEN;
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&required)==required;
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC77 application contract");}
    private ModernNpcDisintegrateProtocol(){}
}
