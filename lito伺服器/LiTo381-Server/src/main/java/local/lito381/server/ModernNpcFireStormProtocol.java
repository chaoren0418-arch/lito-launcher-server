package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;

/** v31 typed NPC/structure AoE. No target list, damage, MP or RNG from clients.
 * Codec support alone does not admit a room, source catalog or player control. */
final class ModernNpcFireStormProtocol {
    static final int VERSION=31, REQUEST_KIND=48, RESULT_KIND=49;
    record Reply(ModernNpcProtocol.Status status,ModernNpcFireStormState.Reason reason,ModernNpcProtocol.Frame frame) {
        Reply {
            Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==ModernNpcProtocol.Status.OK)!=(reason==NONE)
                    ||frame==null&&!(status==ModernNpcProtocol.Status.UNAVAILABLE&&reason==NOT_ACTIVE)
                    ||frame!=null&&status!=ModernNpcProtocol.Status.OK&&status!=ModernNpcProtocol.Status.DENIED)throw bad();
        }
    }
    static void validate(ModernApplicationProtocol.NpcFireStormResult message) {
        var r=Objects.requireNonNull(message.echo());var reply=Objects.requireNonNull(message.reply());var f=reply.frame();if(f==null)return;
        var w=f.combat().world();
        if(!f.scope().equals(r.world())||w.descriptor()==null||!w.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM)
                ||reply.reason()==NOT_ACTIVE&&w.phase()==ModernWorldProtocol.Phase.ACTIVE)throw bad();
        var id=ModernParticipantId.human(r.player().sourceId(),r.player().objectId());
        var a=w.actors().stream().filter(p->p.member().identity().equals(id)).findFirst().orElseThrow(ModernNpcFireStormProtocol::bad);
        var v=f.combat().vital(a.entityId());
        var s=f.combat().npcFireStorms().stream().filter(p->p.entityId()==a.entityId()).findFirst().orElseThrow(ModernNpcFireStormProtocol::bad);
        var c=w.casts().stream().filter(p->p.entityId()==a.entityId()).findFirst().orElseThrow(ModernNpcFireStormProtocol::bad);
        var life=w.quest().life(a.entityId());long incarnation=life==null?1:life.incarnation();
        // Other NPC/mode events may advance publication after this exact cast.
        // The immutable ledger binds sequence/life/origin; full-frame validation
        // bounds its revision and proves MP/HP. Do not require it to be last globally.
        if(reply.status()==ModernNpcProtocol.Status.OK) {
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE&&w.phase()!=ModernWorldProtocol.Phase.FINISHED
                    ||v.hp()==0||incarnation!=r.casterLife()||s.revision()>w.revision()
                    ||s.lastSequence()!=r.sequence()||c.requestSequence()!=r.sequence()
                    ||s.casterLife()!=r.casterLife()||s.castCount()==0||!s.origin().equals(a.position()))throw bad();
        } else if(reply.reason()!=NOT_ACTIVE) {
            if(w.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||(reply.reason()==STALE_STATE?c.requestSequence()<r.sequence():c.requestSequence()!=r.sequence())
                    ||reply.reason()==DEAD&&v.hp()!=0||reply.reason()==NO_MP&&v.mp()>=s.mpCost())throw bad();
        }
    }
    static void putResult(ByteBuffer b,ModernApplicationProtocol.NpcFireStormResult m,int version)throws ModernApplicationProtocol.ProtocolException {
        validate(m);ModernNpcFireStormWire.putRequest(b,m.echo(),version);var r=m.reply();
        b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)(r.frame()==null?0:1)).put((byte)0);
        if(r.frame()!=null)ModernNpcProtocol.putFrame(b,r.frame(),version);
    }
    static ModernApplicationProtocol.NpcFireStormResult getResult(ByteBuffer b,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        var request=ModernNpcFireStormWire.getRequest(b,version);
        int status=Byte.toUnsignedInt(b.get()),reason=Byte.toUnsignedInt(b.get()),present=Byte.toUnsignedInt(b.get());
        if(b.get()!=0||status>=ModernNpcProtocol.Status.values().length||reason>=ModernNpcFireStormState.Reason.values().length||present>1)throw bad();
        return new ModernApplicationProtocol.NpcFireStormResult(id,request,new Reply(ModernNpcProtocol.Status.values()[status],
                ModernNpcFireStormState.Reason.values()[reason],present==1?ModernNpcProtocol.getFrame(b,version):null));
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        int required=ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_REGEN;
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&required)==required;
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC70 application contract");}
    private ModernNpcFireStormProtocol(){}
}
