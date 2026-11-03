package local.lito381.server;

import java.nio.ByteBuffer;
import static local.lito381.server.ModernWorldProtocol.*;

/** Explicit complete-VIP lifecycle, selected independently from READ and gear.
 * Shares only the bounded request/metadata layout with the ordinary lifecycle;
 * never projects a VIP capture through an ordinary world or audience. */
final class ModernVipWorldProtocol {
    static final int REQUEST_KIND=84,RESULT_KIND=85,REQUEST_BYTES=112,MIN_RESULT_BYTES=168;
    static final int MAX_RESULT_BYTES=MIN_RESULT_BYTES+ModernBattleVipWire.MAX_APPEARANCE_BYTES;
    static boolean negotiated(ModernSessionBoundary.Negotiated peer) {
        return ModernSessionExtensions.allowsVipWorld(peer)&&ModernBattleWorldProtocol.negotiated(peer);
    }
    static byte[] encodeRequest(ModernApplicationProtocol.VipWorldRequest message,ModernSessionBoundary.Negotiated peer)
            throws ModernApplicationProtocol.ProtocolException {
        require(peer);
        var bytes=ModernBattleWorldProtocol.encodeRequest(new ModernApplicationProtocol.BattleWorldRequest(message.requestId(),message.request()),peer.version());
        ByteBuffer.wrap(bytes).putShort(12,(short)REQUEST_KIND);return bytes;
    }
    static ModernApplicationProtocol.VipWorldRequest decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated peer)
            throws ModernApplicationProtocol.ProtocolException {
        require(peer);
        try {
            if(bytes==null||bytes.length!=REQUEST_BYTES)throw bad();var b=ByteBuffer.wrap(bytes);
            if(b.getInt()!=0x4c544d31||b.getInt()!=REQUEST_BYTES)throw bad();int id=b.getInt();
            if(id==0||b.getShort()!=REQUEST_KIND||b.getShort()!=0)throw bad();
            var result=new ModernApplicationProtocol.VipWorldRequest(id,ModernBattleWorldProtocol.getRequest(b));
            if(b.hasRemaining())throw bad();return result;
        }catch(IllegalArgumentException|java.nio.BufferUnderflowException invalid){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static int state(ModernWorldService.VipWorldReply reply){return reply.world()==null?0:reply.payload()==null?1:2;}
    static void validate(ModernApplicationProtocol.VipWorldResult message) {
        if(message.requestId()==0)throw bad();var q=message.echo();var r=message.reply();
        if(r.reason().ordinal()>Reason.NOT_ACTIVE.ordinal())throw bad();
        if(r.world()!=null&&q.matchId()!=0&&(r.world().roomId()!=q.roomId()||r.world().matchId()!=q.matchId()||r.world().generation()!=q.generation()))throw bad();
        if(r.payload()!=null&&!r.payload().view().personal().scope().equals(q.scope()))throw bad();
        if(r.payload()==null&&r.lifecycle()!=null) {
            var l=r.lifecycle();boolean preparing=l.phase()==Phase.PREPARING;
            if(l.settlement()!=null||l.revision()<1||preparing&&(l.revision()!=1||l.abortReason()!=AbortReason.NONE)
                    ||!preparing&&(l.phase()!=Phase.ABORTED||l.revision()<2||l.abortReason()==AbortReason.NONE))throw bad();
        }
        if(r.status()==Status.OK)switch(q.action()) {
            case READ -> {if(q.matchId()!=0&&r.world()==null)throw bad();}
            case PREPARE -> {if(state(r)!=1||r.lifecycle().phase()!=Phase.PREPARING||r.lifecycle().revision()!=1
                    ||r.world().roomId()!=q.roomId()||r.world().generation()!=q.roomRevision()+1)throw bad();}
            case LOADED -> {
                if(r.payload()==null||!r.payload().view().world().manifest().digest().equals(q.digest()))throw bad();
                var v=r.payload().view();var self=v.world().actors().stream().filter(a->a.pose().entityId()==v.personal().entityId()).findFirst().orElseThrow(ModernVipWorldProtocol::bad);
                if(!self.pose().loaded())throw bad();
            }
            case LEAVE -> {if(r.world()!=null)throw bad();}
        }
    }
    static byte[] encodeResult(ModernApplicationProtocol.VipWorldResult message,ModernSessionBoundary.Negotiated peer)
            throws ModernApplicationProtocol.ProtocolException {
        require(peer);validate(message);var r=message.reply();
        var echo=encodeRequest(new ModernApplicationProtocol.VipWorldRequest(message.requestId(),message.echo()),peer);
        if(r.payload()!=null)r.payload().requirePeer(peer);
        var payload=r.payload()==null?new byte[0]:ModernBattleVipWire.encodeSelected(r.payload(),peer);
        var b=ByteBuffer.allocate(MIN_RESULT_BYTES+payload.length);b.put(echo).putInt(4,b.capacity()).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)state(r)).put((byte)0);
        if(r.world()==null)b.put(new byte[48]);
        else {
            b.putLong(r.world().roomId()).putLong(r.world().matchId()).putLong(r.world().generation());
            ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());
        }
        b.putInt(payload.length).put(payload);return b.array();
    }
    private static void require(ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        if(!negotiated(peer))throw new ModernApplicationProtocol.ProtocolException();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete VIP world lifecycle");}
    private ModernVipWorldProtocol(){}
}
