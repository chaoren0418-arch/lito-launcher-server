package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.Objects;
import static local.lito381.server.ModernWorldProtocol.*;

/** v39 lifecycle channel. PREPARING has an attachment, not an invented engine
 * capture. DATA and persistence come from one publication; no legacy Frame. */
final class ModernBattleWorldProtocol {
    static final int VERSION=39,REQUEST_KIND=66,RESULT_KIND=67,REQUEST_BYTES=112,METADATA_BYTES=56;
    static final int MIN_RESULT_BYTES=REQUEST_BYTES+METADATA_BYTES;
    static final int MAX_RESULT_BYTES=MIN_RESULT_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    enum Action { READ,PREPARE,LOADED,LEAVE }
    enum State { DETACHED,BEFORE_ENGINE,DATA }
    record Request(ModernRooms.Scope scope,Action action,long roomId,long roomRevision,long matchId,long generation,String digest) {
        Request {
            Objects.requireNonNull(scope);Objects.requireNonNull(action);
            if(digest==null||!digest.matches("[0-9a-f]{64}"))throw bad();
            boolean world=roomId>0&&matchId>0&&generation>0;
            boolean empty=roomId==0&&matchId==0&&generation==0;
            boolean zero=ZERO_DIGEST.equals(digest);
            if(!switch(action){
                case READ -> roomRevision==0&&zero&&(world||empty);
                case PREPARE -> roomId>0&&roomRevision>0&&roomRevision<Long.MAX_VALUE&&matchId==0&&generation==0&&zero;
                case LOADED -> roomRevision==0&&world&&!zero;
                case LEAVE -> roomRevision==0&&world&&zero;
            })throw bad();
        }
        ModernWorldProtocol.Request command() {
            return new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.valueOf(action.name()),0,
                    action==Action.PREPARE?roomId:0,roomRevision,matchId,generation,0,0,0,digest);
        }
        @Override public String toString(){return "BattleWorldRequest[redacted]";}
    }
    record Reply(Status status,Reason reason,ModernTargetHistory.World world,ModernBattleLifecycle lifecycle,ModernBattleAudience.View view) {
        Reply {
            Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==Status.OK)!=(reason==Reason.NONE)||reason.ordinal()>Reason.NOT_ACTIVE.ordinal()
                    ||(world==null)!=(lifecycle==null)||world==null&&view!=null)throw bad();
            if(lifecycle!=null) {
                if(view==null) {
                    if(lifecycle.phase()!=Phase.PREPARING&&lifecycle.phase()!=Phase.ABORTED
                            ||lifecycle.result()!=null||lifecycle.settlement()!=null||lifecycle.race().complete())throw bad();
                } else if(!world.equals(view.world().identity())||lifecycle.revision()<=view.world().revision()
                        ||!Objects.equals(lifecycle.result(),view.world().result()))throw bad();
            }
        }
        State state(){return world==null?State.DETACHED:view==null?State.BEFORE_ENGINE:State.DATA;}
        @Override public String toString(){return "BattleWorldReply[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        return p!=null&&p.version()>=VERSION&&ModernBattleOperationProtocol.negotiated(p);
    }
    static byte[] encodeRequest(ModernApplicationProtocol.BattleWorldRequest message,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||message.requestId()==0)throw new ModernApplicationProtocol.ProtocolException();
        var r=message.request();var s=r.scope();var b=ByteBuffer.allocate(REQUEST_BYTES);
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(message.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.put((byte)r.action().ordinal()).put(new byte[7]).putLong(r.roomId()).putLong(r.roomRevision()).putLong(r.matchId()).putLong(r.generation());
        b.put(HexFormat.of().parseHex(r.digest()));return b.array();
    }
    static Request getRequest(ByteBuffer b) {
        var scope=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        int action=Byte.toUnsignedInt(b.get());if(action>=Action.values().length)throw bad();
        for(int i=0;i<7;i++)if(b.get()!=0)throw bad();
        long room=b.getLong(),revision=b.getLong(),match=b.getLong(),generation=b.getLong();var digest=new byte[32];b.get(digest);
        return new Request(scope,Action.values()[action],room,revision,match,generation,HexFormat.of().formatHex(digest));
    }
    static void validate(ModernApplicationProtocol.BattleWorldResult m) {
        if(m.requestId()==0)throw bad();var q=m.echo();var r=m.reply();
        if(r.world()!=null&&q.matchId()!=0&&(r.world().roomId()!=q.roomId()||r.world().matchId()!=q.matchId()||r.world().generation()!=q.generation()))throw bad();
        if(r.view()!=null&&!r.view().personal().scope().equals(q.scope()))throw bad();
        if(r.status()==Status.OK)switch(q.action()) {
            case READ -> {if(q.matchId()!=0&&r.world()==null)throw bad();}
            case PREPARE -> {if(r.state()!=State.BEFORE_ENGINE||r.lifecycle().phase()!=Phase.PREPARING||r.lifecycle().revision()!=1
                    ||r.world().roomId()!=q.roomId()||r.world().generation()!=q.roomRevision()+1)throw bad();}
            case LOADED -> {
                if(r.view()==null||!r.view().world().manifest().digest().equals(q.digest()))throw bad();
                var self=r.view().world().actors().stream().filter(a->a.pose().entityId()==r.view().personal().entityId()).findFirst().orElseThrow();
                if(!self.pose().loaded())throw bad();
            }
            case LEAVE -> {if(r.world()!=null)throw bad();}
        }
    }
    static byte[] encodeResult(ModernApplicationProtocol.BattleWorldResult message,int version)throws ModernApplicationProtocol.ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeResult(ModernApplicationProtocol.BattleWorldResult message,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        return encodeResult(message,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeResult(ModernApplicationProtocol.BattleWorldResult message,ModernBattleWire.Encoding format)throws ModernApplicationProtocol.ProtocolException {
        int version=format.version();
        validate(message);var r=message.reply();var echo=encodeRequest(new ModernApplicationProtocol.BattleWorldRequest(message.requestId(),message.echo()),version);
        var payload=r.view()==null?new byte[0]:format.payload(r.view());
        var b=ByteBuffer.allocate(MIN_RESULT_BYTES+payload.length);b.put(echo).putInt(4,b.capacity()).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)r.state().ordinal()).put((byte)0);
        if(r.world()==null)b.put(new byte[48]);
        else {
            b.putLong(r.world().roomId()).putLong(r.world().matchId()).putLong(r.world().generation());
            ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());
        }
        b.putInt(payload.length).put(payload);return b.array();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical world lifecycle");}
    private ModernBattleWorldProtocol(){}
}
