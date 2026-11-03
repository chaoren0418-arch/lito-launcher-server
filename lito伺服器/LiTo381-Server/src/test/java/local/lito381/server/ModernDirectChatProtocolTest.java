package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernDirectChatProtocolTest {
    static ModernDirectChatProtocol.Request read(){return new ModernDirectChatProtocol.Request(new ModernRooms.Scope(7,3333,-1,3),0,1,0,0,ModernDirectChat.Action.READ,null,null,"");}
    static ModernDirectChat.Peer owner(){return new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(7,3333),"Sun Knight","太陽神");}
    static ModernDirectChat.Result result(ModernDirectChatProtocol.Request r,List<ModernDirectChat.Message> messages,List<ModernDirectChat.Address> blocked){
        return new ModernDirectChat.Result(ModernDirectChat.Status.OK,new ModernDirectChat.Snapshot(owner(),r.sequence(),
                messages.isEmpty()?0:messages.get(messages.size()-1).id(),true,false,ModernDirectChat.Preferences.DEFAULT,blocked,messages));
    }
    @Test void exactRequestAndIndependentVersionBoundary()throws Exception{
        var message=new ModernApplicationProtocol.DirectChatRequest(-1,read());var bytes=ModernApplicationProtocol.encode(message,32);
        assertEquals(84,bytes.length);
        assertEquals("4c544d3100000054ffffffff00340000"+"000000070000000000000d05ffffffffffffffff03000000"
                +"0000000000000000000000000000000100000000000000000000000000000000"+"000000000000000000000000",java.util.HexFormat.of().formatHex(bytes));
        assertEquals(message,ModernApplicationProtocol.decode(bytes,32));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),32));
        for(int version=1;version<32;version++){final int v=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),v));}
    }
    @Test void largestBatchIsBoundedAndPreservesBothQualifiedPeers()throws Exception{
        var messages=new ArrayList<ModernDirectChat.Message>();var blocked=new ArrayList<ModernDirectChat.Address>();
        var self=new ModernDirectChat.Peer(owner().key(),"a".repeat(256),"b".repeat(256));
        var other=new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(42,3333),"c".repeat(256),"d".repeat(256));
        for(int i=1;i<=64;i++)messages.add(new ModernDirectChat.Message(i,i,other,self,"😀".repeat(128)));
        for(int i=1;i<=16;i++)blocked.add(new ModernDirectChat.Address(i,"z".repeat(256)));
        var result=new ModernDirectChat.Result(ModernDirectChat.Status.OK,new ModernDirectChat.Snapshot(self,0,64,true,false,ModernDirectChat.Preferences.DEFAULT,blocked,messages));
        var message=new ModernApplicationProtocol.DirectChatResult(1,read(),result);var bytes=ModernApplicationProtocol.encode(message,32);
        assertTrue(bytes.length>100_000);assertTrue(bytes.length<ModernDirectChatProtocol.MAX_FRAME_BYTES);
        assertEquals(message,ModernApplicationProtocol.decode(bytes,32));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),32));
        assertFalse(message.toString().contains("😀"));
    }
    @Test void rawSpellingIsEchoedAndOnlyAuthorityNormalizes()throws Exception{
        for(String name:List.of("  MOON Mage  ","İ","ΟΣ","角色👩‍💻")){
            var target=new ModernDirectChatProtocol.Target(42,name);
            var request=new ModernDirectChatProtocol.Request(read().scope(),0,1,1,0,ModernDirectChat.Action.BLOCK,target,null,"");
            var message=new ModernApplicationProtocol.DirectChatRequest(9,request);
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,32),32));
            assertEquals(name,request.target().name());assertEquals(new ModernDirectChat.Address(42,name),request.command().target());
            var reply=new ModernApplicationProtocol.DirectChatResult(9,request,result(request,List.of(),List.of(target.address())));
            assertEquals(reply,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(reply,32),32));
        }
    }
    @Test void malformedReservedFlagsCountsUtf8AndEveryTruncationFail()throws Exception{
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.DirectChatResult(2,read(),result(read(),List.of(),List.of())),32);
        for(int[] mutation:new int[][]{{37,1},{38,1},{39,1},{73,8},{74,1},{75,1},{84,5},{85,4},{86,8},{87,17},{89,65},{90,1},{bytes.length-1,255}}){
            var bad=bytes.clone();bad[mutation[0]]=(byte)mutation[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,32));}
        for(int length=0;length<bytes.length;length++){var bad=java.util.Arrays.copyOf(bytes,length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,32));}
        var header=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(header).putInt(4,ModernDirectChatProtocol.MAX_FRAME_BYTES+1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),32));
        var request=ModernApplicationProtocol.encode(new ModernApplicationProtocol.DirectChatRequest(2,read()),32);
        for(int at:new int[]{73,79}){var bad=request.clone();bad[at]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,32));}
    }
    @Test void actionsAndResultsCannotClaimUnseenSendOrForeignIdentity()throws Exception{
        var request=new ModernDirectChatProtocol.Request(read().scope(),0,1,1,0,ModernDirectChat.Action.SEND,new ModernDirectChatProtocol.Target(42,"Moon Mage"),null,".de 20");
        var other=new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(42,3333),"Moon Mage","月亮");
        var good=result(request,List.of(new ModernDirectChat.Message(1,1,owner(),other,".de 20")),List.of());
        ModernDirectChatProtocol.validate(request,good);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernDirectChatProtocol.validate(request,result(request,List.of(),List.of())));
        var wrong=new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(7,99),"Moon Mage","太陽神");
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernDirectChatProtocol.validate(request,result(request,List.of(new ModernDirectChat.Message(1,1,owner(),wrong,".de 20")),List.of())));
        assertThrows(IllegalArgumentException.class,()->new ModernDirectChatProtocol.Request(read().scope(),0,0,0,0,ModernDirectChat.Action.READ,null,null,""));
        assertThrows(IllegalArgumentException.class,()->new ModernDirectChatProtocol.Request(read().scope(),0,1,0,0,ModernDirectChat.Action.READ,request.target(),null,""));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernDirectChatProtocol.validate(read(),new ModernDirectChat.Result(ModernDirectChat.Status.RATE_LIMITED,result(read(),List.of(),List.of()).snapshot())));
    }
    @Test void capabilityRequiresChatAndOldPeersNeverEnrollOrReceiveTheNewBit()throws Exception{
        int old=39|ModernSessionProtocol.CAP_CHAT,caps=old|ModernSessionProtocol.CAP_DIRECT_CHAT;
        for(int bit:new int[]{1,2,4,32,ModernSessionProtocol.CAP_CHAT}){
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,0));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps,caps&~bit));}
        var policy=ModernSessionBoundary.Policy.rooms(false).withDirectChat();
        for(int offered:new int[]{39,old,caps}){
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(policy).acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,32,offered,7,1))));
            assertEquals(offered,ack.capabilities());assertEquals(32,ack.version());}
        var oldAck=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(policy.withMaxVersion(31)).acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,32,caps,7,1))));
        assertEquals(39,oldAck.capabilities());assertEquals(31,oldAck.version());
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_DIRECT_CHAT);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,31,1,caps)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(caps,caps,31));
    }
    @Test void missingMiddleMessageIsNotAHistoryTruncation() {
        var other=new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(42,3333),"Moon Mage","月亮");
        var rows=List.of(new ModernDirectChat.Message(1,1,other,owner(),"first"),new ModernDirectChat.Message(3,2,other,owner(),"third"));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernDirectChatProtocol.validate(read(),result(read(),rows,List.of())));
    }
}
