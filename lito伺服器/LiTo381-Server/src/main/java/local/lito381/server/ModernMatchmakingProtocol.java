package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** v34 independent automatic-grouping frames. No passwords, client-supplied
 * scores or native selectors. The nested room view is the confirmed authority,
 * not an optimistic destination inferred from a queue acknowledgement. */
final class ModernMatchmakingProtocol {
    static final int VERSION=34, REQUEST_KIND=58, RESULT_KIND=59;
    static final int REQUEST_BYTES=56, MIN_RESULT_BYTES=60, MAX_FRAME_BYTES=ModernRoomProtocol.MAX_FRAME_BYTES+80;
    enum Action { READ, START, CANCEL }
    enum Status { OK, BUSY, UNAVAILABLE }
    record Request(ModernRooms.Scope scope, Action action, long ticketId, ModernMatchmaking.Criteria criteria) {
        Request {
            Objects.requireNonNull(scope); Objects.requireNonNull(action);
            if(ticketId<0 || action==Action.START&&(ticketId!=0||criteria==null)
                    || action!=Action.START&&criteria!=null || action==Action.CANCEL&&ticketId==0)throw bad();
        }
        @Override public String toString(){return "ModernMatchmaking.Request[redacted]";}
    }
    record Reply(Status status, ModernMatchmaking.Result result) {
        Reply { Objects.requireNonNull(status);if((status==Status.OK)!=(result!=null))throw bad(); }
        static Reply result(ModernMatchmaking.Result value){return new Reply(Status.OK,value);}
        static Reply busy(){return new Reply(Status.BUSY,null);}
        static Reply unavailable(){return new Reply(Status.UNAVAILABLE,null);}
        @Override public String toString(){return "ModernMatchmaking.Reply[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated selected) {
        return selected!=null && selected.version()>=VERSION
                && (selected.capabilities()&ModernSessionProtocol.CAP_MATCHMAKING)!=0;
    }
    static void putRequest(ByteBuffer out,Request r) {
        var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch())
                .put((byte)s.channelId()).put((byte)r.action().ordinal()).putShort((short)0).putLong(r.ticketId());
        putCriteria(out,r.criteria());
    }
    static Request getRequest(ByteBuffer in) {
        var scope=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),u8(in));
        var action=at(Action.values(),u8(in));zero(in,2);long ticket=in.getLong();
        return new Request(scope,action,ticket,getCriteria(in,action==Action.START));
    }
    private static void putCriteria(ByteBuffer out,ModernMatchmaking.Criteria c) {
        if(c==null){out.putLong(0);return;}
        out.put((byte)c.mode()).put((byte)0).putShort((short)c.players()).putInt(c.mapId());
    }
    private static ModernMatchmaking.Criteria getCriteria(ByteBuffer in,boolean present) {
        if(!present){zero(in,8);return null;}
        int mode=u8(in);zero(in,1);int players=Short.toUnsignedInt(in.getShort()),map=in.getInt();
        return new ModernMatchmaking.Criteria(mode,map,players);
    }
    static void putResult(ByteBuffer out,Request echo,Reply reply,int version)throws ModernApplicationProtocol.ProtocolException {
        validate(echo,reply);if(version<VERSION)throw bad();
        putRequest(out,echo);out.put((byte)reply.status().ordinal()).put((byte)0).putShort((short)0);
        if(reply.result()==null)return;
        var result=reply.result();out.put((byte)result.state().ordinal()).put((byte)0).putShort((short)0).putLong(result.ticketId());
        putCriteria(out,result.criteria());
        ModernRoomProtocol.putResult(out,nested(echo.scope(),result.rooms()),version);
    }
    static ModernApplicationProtocol.MatchmakingResult getResult(ByteBuffer in,int requestId,int version)
            throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();var echo=getRequest(in);var status=at(Status.values(),u8(in));zero(in,3);
        ModernMatchmaking.Result result=null;
        if(status==Status.OK){
            var state=at(ModernMatchmaking.State.values(),u8(in));zero(in,3);long ticket=in.getLong();
            var criteria=getCriteria(in,state!=ModernMatchmaking.State.IDLE);
            var rooms=ModernRoomProtocol.getResult(in,1,version);
            if(!rooms.echo().equals(nestedEcho(echo.scope())))throw bad();
            ModernRoomProtocol.validate(rooms);
            result=new ModernMatchmaking.Result(state,ticket,criteria,rooms.result());
        }
        var reply=new Reply(status,result);validate(echo,reply);
        return new ModernApplicationProtocol.MatchmakingResult(requestId,echo,reply);
    }
    static void validate(Request echo,Reply reply)throws ModernApplicationProtocol.ProtocolException {
        Objects.requireNonNull(echo);Objects.requireNonNull(reply);
        if(reply.status()!=Status.OK){if(echo.action()!=Action.START)throw bad();return;}
        var result=reply.result();var status=result.rooms().status();
        ModernRoomProtocol.validate(nested(echo.scope(),result.rooms()));
        if(status==ModernRooms.Status.CONFLICT){
            if(echo.action()==Action.START||echo.ticketId()==0||echo.ticketId()==result.ticketId())throw bad();
        } else if(echo.ticketId()!=0 && result.state()!=ModernMatchmaking.State.IDLE && result.ticketId()!=echo.ticketId())throw bad();
        if(status==ModernRooms.Status.OK)switch(echo.action()) {
            case START -> {if(result.state()!=ModernMatchmaking.State.WAITING||!echo.criteria().equals(result.criteria()))throw bad();}
            case READ -> {if(echo.ticketId()!=0&&result.state()==ModernMatchmaking.State.IDLE)throw bad();}
            case CANCEL -> {if(result.state()==ModernMatchmaking.State.WAITING)throw bad();}
        }
    }
    private static ModernRoomProtocol.Echo nestedEcho(ModernRooms.Scope scope){return new ModernRoomProtocol.Echo(scope,ModernRooms.Action.READ,0,0,0);}
    private static ModernApplicationProtocol.RoomResult nested(ModernRooms.Scope scope,ModernRooms.Result result){
        return new ModernApplicationProtocol.RoomResult(1,nestedEcho(scope),result);
    }
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static void zero(ByteBuffer in,int bytes){for(int i=0;i<bytes;i++)if(in.get()!=0)throw bad();}
    private static <T>T at(T[] values,int index){if(index<0||index>=values.length)throw bad();return values[index];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern matchmaking frame");}
    private ModernMatchmakingProtocol(){}
}
