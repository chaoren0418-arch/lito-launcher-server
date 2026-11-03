package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** v35 explicitly negotiated READ -> owner-filtered canonical LTB1 data.
 * No command ACK, SQL saved receipt, legacy frame conversion or SET authority.
 * PREPARING/UNAVAILABLE are service states, never invented engine phases. */
final class ModernBattleReadProtocol {
    static final int VERSION=35, REQUEST_KIND=60, RESULT_KIND=61;
    static final int REQUEST_BYTES=64, MIN_RESULT_BYTES=72;
    static final int PRESENTATION_VERSION=36;
    static final int MAX_FRAME_BYTES=MIN_RESULT_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    enum State { PREPARING, DATA, UNAVAILABLE }
    record Request(ModernRooms.Scope scope,ModernTargetHistory.World world) {
        Request {Objects.requireNonNull(scope);Objects.requireNonNull(world);}
        @Override public String toString(){return "BattleReadRequest[redacted]";}
    }
    record Reply(State state,ModernBattleAudience.View view,ModernBattleVipWire.Payload vip) {
        Reply(State state,ModernBattleAudience.View view){this(state,view,null);}
        Reply {Objects.requireNonNull(state);if((state==State.DATA)!=((view!=null)^(vip!=null))||view!=null&&vip!=null)
            throw new IllegalArgumentException("invalid battle read state");}
        static Reply vip(ModernBattleVipWire.Payload payload){return new Reply(State.DATA,null,Objects.requireNonNull(payload));}
        @Override public String toString(){return "BattleReadReply[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&ModernSessionProtocol.CAP_BATTLE_DATA)!=0;
    }
    static void putRequest(ByteBuffer b,Request r) {
        var s=r.scope();var w=r.world();b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());
    }
    static Request getRequest(ByteBuffer b) {
        return new Request(new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt()),
                new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong()));
    }
    /** Exact allocation after bounded LTB1 counting; no maximum-size scratch. */
    static byte[] encodeResult(ModernApplicationProtocol.BattleReadResult result,int version)throws ModernApplicationProtocol.ProtocolException {
        return encodeResult(result,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeResult(ModernApplicationProtocol.BattleReadResult result,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        return encodeResult(result,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeResult(ModernApplicationProtocol.BattleReadResult result,ModernBattleWire.Encoding format)throws ModernApplicationProtocol.ProtocolException {
        int version=format.version();
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||result.requestId()==0||result.echo()==null||result.reply()==null)
            throw new ModernApplicationProtocol.ProtocolException();
        var r=result.reply();var v=r.view();
        if(v!=null&&(!v.personal().scope().equals(result.echo().scope())||!v.world().identity().equals(result.echo().world())))
            throw new ModernApplicationProtocol.ProtocolException();
        if(r.vip()!=null&&(!r.vip().view().personal().scope().equals(result.echo().scope())
                ||!r.vip().view().world().identity().equals(result.echo().world())))
            throw new ModernApplicationProtocol.ProtocolException();
        var payload=r.vip()!=null?format.payloadVip(r.vip()):v==null?new byte[0]:format.payload(v);
        var b=ByteBuffer.allocate(MIN_RESULT_BYTES+payload.length);
        b.putInt(0x4c544d31).putInt(b.capacity()).putInt(result.requestId()).putShort((short)RESULT_KIND).putShort((short)0);
        putRequest(b,result.echo());b.put((byte)r.state().ordinal()).put((byte)0).putShort((short)0).putInt(payload.length).put(payload);
        return b.array();
    }
    private ModernBattleReadProtocol(){}
}
