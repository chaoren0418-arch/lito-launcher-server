package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernChatProtocolTest {
    static ModernChatProtocol.Request read() {return new ModernChatProtocol.Request(new ModernRooms.Scope(7,3333,-1,3),0,0,0,0,ModernChatProtocol.Action.READ,"");}
    static ModernChat.Result result(ModernChatProtocol.Request r,List<ModernChat.Message> messages) {
        return new ModernChat.Result(ModernChat.Status.OK,new ModernChat.Snapshot(new ModernChat.Audience(r.scope().channelId(),r.roomId()),
                1,r.sequence(),messages.isEmpty()?0:messages.get(messages.size()-1).id(),true,false,messages));
    }
    @Test void exactRequestAndStrictVersionBoundary() throws Exception {
        var request=new ModernApplicationProtocol.ChatRequest(-1,read());
        var bytes=ModernApplicationProtocol.encode(request,32);
        assertEquals(76,bytes.length);
        assertEquals("4c544d310000004cffffffff00320000000000070000000000000d05ffffffffffffffff03000000"+
                "000000000000000000000000000000000000000000000000000000000000000000000000",java.util.HexFormat.of().formatHex(bytes));
        assertEquals(request,ModernApplicationProtocol.decode(bytes,32));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),32));
        for(int version=1;version<32;version++) {
            int v=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),v));
        }
    }
    @Test void largestUnicodeBatchHasItsOwnBoundAndRoundTrips() throws Exception {
        var request=read();var messages=new ArrayList<ModernChat.Message>();
        for(int i=1;i<=64;i++)messages.add(new ModernChat.Message(i,new TournamentPresence.CharacterKey(i%2==0?7:42,3333),"a".repeat(256),"b".repeat(256),"😀".repeat(128)));
        var response=new ModernApplicationProtocol.ChatResult(10,request,result(request,messages));
        var bytes=ModernApplicationProtocol.encode(response,32);
        assertTrue(bytes.length>65536);assertTrue(bytes.length<ModernChatProtocol.MAX_FRAME_BYTES);
        assertEquals(response,ModernApplicationProtocol.decode(bytes,32));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),32));
        assertFalse(response.toString().contains("😀"));
    }
    @Test void malformedReservedFlagsCountUtf8AndTruncationFail() throws Exception {
        var request=new ModernChatProtocol.Request(read().scope(),0,1,1,0,ModernChatProtocol.Action.SEND,"測試");
        var row=new ModernChat.Message(1,new TournamentPresence.CharacterKey(7,3333),"3333","太陽神",request.text());
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.ChatResult(2,request,result(request,List.of(row))),32);
        for(int at:new int[]{37,38,39,73}) {
            var damaged=bytes.clone();damaged[at]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(damaged,32));
        }
        int tail=76+6;
        for(int[] mutation:new int[][]{{tail,4},{tail+1,4},{tail+3,65},{bytes.length-1,255}}) {
            var damaged=bytes.clone();damaged[mutation[0]]=(byte)mutation[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(damaged,32));
        }
        for(int length=0;length<bytes.length;length++) {
            var shortFrame=java.util.Arrays.copyOf(bytes,length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame,32));
        }
        var oversized=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(oversized).putInt(4,ModernChatProtocol.MAX_FRAME_BYTES+1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(oversized),32));
    }
    @Test void sendCannotForgeSourceOrAcknowledgeAnUnseenOwnMessage() {
        var r=new ModernChatProtocol.Request(read().scope(),0,1,1,0,ModernChatProtocol.Action.SEND,"hello");
        var other=new ModernChat.Message(1,new TournamentPresence.CharacterKey(42,3333),"3333","月亮神","hello");
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernChatProtocol.validate(r,result(r,List.of(other))));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernChatProtocol.validate(r,result(r,List.of())));
        assertThrows(IllegalArgumentException.class,()->new ModernChatProtocol.Request(read().scope(),0,0,0,1,ModernChatProtocol.Action.READ,""));
    }
    @Test void chatRequiresDependenciesAndDoesNotLeakIntoOlderAckOrDefaultRuntime() throws Exception {
        int caps=39|ModernSessionProtocol.CAP_CHAT;
        for(int bit:new int[]{1,2,4,32}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,0));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps,caps&~bit));
        }
        var hello=new ModernSessionProtocol.Hello(1,1,32,caps,7,1);
        var policy=ModernSessionBoundary.Policy.rooms(false).withChat();
        var modern=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(policy).acceptHello(ModernSessionProtocol.encode(hello)));
        assertEquals(caps,modern.capabilities());assertEquals(32,modern.version());
        var old=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(policy.withMaxVersion(31)).acceptHello(ModernSessionProtocol.encode(hello)));
        assertEquals(39,old.capabilities());assertEquals(31,old.version());
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_CHAT);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,31,caps,7,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,31,1,caps)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(caps,caps,31));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->new ModernSessionBoundary(policy.withMaxVersion(31)).acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,32,caps,caps,1))));
    }
}
