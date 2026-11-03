package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernMatchmakingProtocolTest {
    static final int BASE_CAPS=127|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
    static final int CAPS=BASE_CAPS|ModernSessionProtocol.CAP_MATCHMAKING;
    static ModernMatchmakingProtocol.Request request(ModernRooms.Scope scope,ModernMatchmakingProtocol.Action action,long id){
        return new ModernMatchmakingProtocol.Request(scope,action,id,action==ModernMatchmakingProtocol.Action.START?ModernMatchmakingTest.pair():null);
    }
    static ModernApplicationProtocol.MatchmakingResult response(ModernMatchmakingProtocol.Request echo,ModernMatchmaking.Result result){
        return new ModernApplicationProtocol.MatchmakingResult(-1,echo,ModernMatchmakingProtocol.Reply.result(result));
    }
    static byte[] roundTrip(ModernApplicationProtocol.Message message)throws Exception{
        var bytes=ModernApplicationProtocol.encode(message,34);assertEquals(message,ModernApplicationProtocol.decode(bytes,34));return bytes;
    }
    @Test void exactRequestGoldenAndAllOlderVersionsRejectBeforeBodyRead()throws Exception{
        var scope=new ModernRooms.Scope(7,3333,-1,3);
        var req=new ModernApplicationProtocol.MatchmakingRequest(-1,request(scope,ModernMatchmakingProtocol.Action.START,0));
        var bytes=roundTrip(req);assertEquals(56,bytes.length);
        assertEquals("4c544d3100000038ffffffff003a0000000000070000000000000d05ffffffffffffffff0301000000000000000000000100000200000202",java.util.HexFormat.of().formatHex(bytes));
        for(int i=1;i<34;i++){final int v=i;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(req,v));
            var input=new ByteArrayInputStream(bytes);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(input,v));
            assertEquals(bytes.length-16,input.available());
        }
    }
    @Test void idleQueueMatchedCancelRaceAndStaleAdmissionRoundTripAuthoritativeRoom()throws Exception{
        try(var f=new ModernMatchmakingTest.Fixture()){
            var a=f.player(7,3333,80,0);var b=f.player(42,3333,80,0);
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.READ,0),f.read(a,0)));
            var started=f.start(a,ModernMatchmakingTest.pair(),5,5);
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.START,0),started));
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.START,0),f.start(a,ModernMatchmakingTest.pair(),5,5)));
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.READ,started.ticketId()),f.read(a,started.ticketId())));
            f.start(b,ModernMatchmakingTest.pair(),5,5);var matched=f.read(a,started.ticketId());
            assertEquals(2,matched.rooms().view().joined().members().size());
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.READ,started.ticketId()),matched));
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.CANCEL,started.ticketId()),f.cancel(a,started.ticketId())));
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.CANCEL,999),f.cancel(a,999)));
            f.leave(a);
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.READ,started.ticketId()),f.read(a,started.ticketId())));
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.CANCEL,started.ticketId()),f.cancel(a,started.ticketId())));
            f.create(a,""); // IDLE can truthfully include a manually joined room.
            roundTrip(response(request(a.scope(),ModernMatchmakingProtocol.Action.READ,0),f.read(a,0)));
        }
    }
    @Test void backendFailureHasNoInventedZeroHistoryQueueOrRoom()throws Exception{
        var start=request(new ModernRooms.Scope(7,3333,1,0),ModernMatchmakingProtocol.Action.START,0);
        for(var reply:List.of(ModernMatchmakingProtocol.Reply.busy(),ModernMatchmakingProtocol.Reply.unavailable())){
            var bytes=roundTrip(new ModernApplicationProtocol.MatchmakingResult(1,start,reply));assertEquals(60,bytes.length);
            var read=request(start.scope(),ModernMatchmakingProtocol.Action.READ,0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.MatchmakingResult(1,read,reply),34));
        }
    }
    @Test void reservedEnumsNegativeAliasesTruncationAndTrailingBytesFail()throws Exception{
        try(var f=new ModernMatchmakingTest.Fixture()){
            var p=f.player();var echo=request(p.scope(),ModernMatchmakingProtocol.Action.START,0);
            var req=roundTrip(new ModernApplicationProtocol.MatchmakingRequest(1,echo));
            var reply=roundTrip(response(echo,f.start(p,ModernMatchmakingTest.pair(),0,0)));
            for(var bytes:List.of(req,reply)){
                for(int n=0;n<bytes.length;n++){var cut=Arrays.copyOf(bytes,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,34));}
                var extra=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,34));
            }
            for(int[] edit:new int[][]{{37,3},{38,1},{39,1},{40,128},{48,6},{49,1},{50,255},{51,0}}){
                var bad=req.clone();bad[edit[0]]=(byte)edit[1];assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,34));
            }
            for(int[] edit:new int[][]{{56,3},{57,1},{58,1},{59,1},{60,3},{61,1},{62,1},{63,1},{64,128},{73,1},{80,1}}){
                var bad=reply.clone();bad[edit[0]]=(byte)edit[1];assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,34));
            }
            var header=Arrays.copyOf(reply,16);ByteBuffer.wrap(header).putInt(4,ModernMatchmakingProtocol.MAX_FRAME_BYTES+1);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),34));
            var joined=new byte[reply.length*2];System.arraycopy(reply,0,joined,0,reply.length);System.arraycopy(reply,0,joined,reply.length,reply.length);
            var input=new ByteArrayInputStream(joined);assertArrayEquals(reply,ModernApplicationProtocol.readFrame(input,34));assertArrayEquals(reply,ModernApplicationProtocol.readFrame(input,34));assertEquals(0,input.available());
        }
    }
    @Test void noMatchedResultWithoutSelfAndNoWaitingCancelOrUnrelatedNestedEcho()throws Exception{
        try(var f=new ModernMatchmakingTest.Fixture()){
            var p=f.player();var started=f.start(p,ModernMatchmakingTest.pair(),0,0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(response(request(p.scope(),ModernMatchmakingProtocol.Action.CANCEL,started.ticketId()),started),34));
            assertThrows(IllegalArgumentException.class,()->new ModernMatchmaking.Result(ModernMatchmaking.State.MATCHED,1,started.criteria(),started.rooms()));
            var req=request(p.scope(),ModernMatchmakingProtocol.Action.START,0);var bytes=roundTrip(response(req,started));
            bytes[100]=1; // nested room channel differs from echoed selection.
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,34));
            assertThrows(IllegalArgumentException.class,()->new ModernMatchmakingProtocol.Request(p.scope(),ModernMatchmakingProtocol.Action.CANCEL,0,null));
        }
    }
    @Test void capabilityOptInDependenciesAndDowngradeRemainStrict()throws Exception{
        for(int bit:new int[]{1,2,4,8,16,32,64,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR}){
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(CAPS&~bit,0));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(CAPS,CAPS&~bit));
        }
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_MATCHMAKING);
        for(int v:new int[]{15,33,34}){
            var policy=new ModernSessionBoundary.Policy(CAPS,7,v);
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(policy).acceptHello(
                    ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,34,CAPS,7,1))));
            assertEquals(v,ack.version());assertEquals(v==34?CAPS:BASE_CAPS,ack.capabilities());
        }
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,33,CAPS,7,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,33,1,CAPS)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(CAPS,CAPS,33));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->new ModernSessionBoundary(new ModernSessionBoundary.Policy(BASE_CAPS,7,34)).acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,34,CAPS,CAPS,1))));
    }
    @Test void v34WorldCodecRetainsExistingFrameContract()throws Exception{
        var request=ModernWorldProtocolTest.read();
        var message=new ModernApplicationProtocol.WorldResult(1,request,new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,null));
        assertArrayEquals(ModernApplicationProtocol.encode(message,33),roundTrip(message));
    }
}
