package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernFilteredChatProtocolTest {
    static ModernFilteredChat.Request read(){return new ModernFilteredChat.Request(new ModernFilteredChat.Token(new ModernRooms.Scope(7,3333,-1,3),0,0),ModernFilteredChat.Action.READ,ModernFilteredChat.Kind.NORMAL,0,0,"");}
    static ModernDirectChat.Peer owner(){return new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(7,3333),"A","S");}
    static ModernFilteredChat.Result result(ModernFilteredChat.Request r,ModernFilteredChat.Status status,boolean available,
            long cursor,boolean truncated,List<ModernFilteredChat.Message> messages,ModernFilteredChat.Receipt receipt){
        return new ModernFilteredChat.Result(status,new ModernFilteredChat.Snapshot(new ModernFilteredChat.Token(r.token().scope(),r.token().roomId(),1),
                owner(),r.sequence(),cursor,available,truncated,messages),receipt);
    }
    static ModernFilteredChat.Request send(){return new ModernFilteredChat.Request(new ModernFilteredChat.Token(read().token().scope(),0,1),
            ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.WORLD,1,0,"世界👋");}
    static ModernFilteredChat.Receipt receipt(ModernFilteredChat.Request r){return new ModernFilteredChat.Receipt(r.sequence(),owner(),r.kind(),r.text());}
    @Test void exactSharedGoldensAndVersionBoundary()throws Exception{
        var request=new ModernApplicationProtocol.FilteredChatRequest(-1,read());var bytes=ModernApplicationProtocol.encode(request,33);
        assertEquals("4c544d310000004cffffffff00360000000000070000000000000d05ffffffffffffffff03000000000000000000000000000000000000000000000000000000000000000000000000000000",java.util.HexFormat.of().formatHex(bytes));
        assertEquals(request,ModernApplicationProtocol.decode(bytes,33));
        var response=new ModernApplicationProtocol.FilteredChatResult(1,read(),result(read(),ModernFilteredChat.Status.OK,true,0,false,List.of(),null));
        var responseBytes=ModernApplicationProtocol.encode(response,33);
        assertEquals("4c544d310000007e0000000100370000000000070000000000000d05ffffffffffffffff030000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000100000000000000000000000000000000000000070000000000000d05000141000153",java.util.HexFormat.of().formatHex(responseBytes));
        assertEquals(response,ModernApplicationProtocol.decode(responseBytes,33));
        for(int i=1;i<33;i++){final int v=i;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request,v));
            var stream=new ByteArrayInputStream(responseBytes);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(stream,v));
            assertEquals(responseBytes.length-16,stream.available());}
    }
    @Test void receiptConfirmsAcceptanceWithoutSelfDeliveryAndCannotBeOmittedOrAltered()throws Exception{
        var r=send();var result=result(r,ModernFilteredChat.Status.OK,true,0,false,List.of(),receipt(r));
        var message=new ModernApplicationProtocol.FilteredChatResult(8,r,result);var bytes=ModernApplicationProtocol.encode(message,33);
        assertEquals(message,ModernApplicationProtocol.decode(bytes,33));assertTrue(result.snapshot().messages().isEmpty());
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernFilteredChatProtocol.validate(r,result(r,ModernFilteredChat.Status.OK,true,0,false,List.of(),null)));
        for(var wrong:List.of(new ModernFilteredChat.Receipt(1,owner(),ModernFilteredChat.Kind.TRADE,r.text()),new ModernFilteredChat.Receipt(1,owner(),r.kind(),"另一則")))
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernFilteredChatProtocol.validate(r,result(r,ModernFilteredChat.Status.OK,true,0,false,List.of(),wrong)));
        assertFalse(message.toString().contains("世界"));assertFalse(result.receipt().toString().contains("世界"));
    }
    @Test void operationUnavailableDoesNotInventCounterExhaustionAndReadHasStrictStatus()throws Exception{
        var r=send();var denied=result(r,ModernFilteredChat.Status.UNAVAILABLE,true,0,false,List.of(),null);
        ModernFilteredChatProtocol.validate(r,denied);
        for(var status:ModernFilteredChat.Status.values())if(status!=ModernFilteredChat.Status.OK)
            ModernFilteredChatProtocol.validate(r,result(r,status,true,0,false,List.of(),null));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernFilteredChatProtocol.validate(read(),result(read(),ModernFilteredChat.Status.UNAVAILABLE,true,0,false,List.of(),null)));
        assertThrows(IllegalArgumentException.class,()->result(r,ModernFilteredChat.Status.SILENCED,true,0,false,List.of(),receipt(r)));
    }
    @Test void cursorRequiresExactSuffixAndTruthfulTruncation()throws Exception{
        var m=new ModernFilteredChat.Message(3,owner(),ModernFilteredChat.Kind.NORMAL,"third");
        ModernFilteredChatProtocol.validate(read(),result(read(),ModernFilteredChat.Status.OK,true,3,true,List.of(m),null));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernFilteredChatProtocol.validate(read(),result(read(),ModernFilteredChat.Status.OK,true,3,false,List.of(m),null)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernFilteredChatProtocol.validate(read(),result(read(),ModernFilteredChat.Status.OK,true,3,false,List.of(),null)));
        assertThrows(IllegalArgumentException.class,()->result(read(),ModernFilteredChat.Status.OK,true,3,false,
                List.of(new ModernFilteredChat.Message(1,owner(),ModernFilteredChat.Kind.NORMAL,"first"),m),null));
    }
    @Test void largestTypedBatchRemainsBoundedAndStreamDoesNotConsumeNextFrame()throws Exception{
        var rows=new ArrayList<ModernFilteredChat.Message>();
        for(int i=1;i<=64;i++)rows.add(new ModernFilteredChat.Message(i,new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(42,3333),"a".repeat(256),"b".repeat(256)),ModernFilteredChat.Kind.values()[i%3],"😀".repeat(128)));
        var m=new ModernApplicationProtocol.FilteredChatResult(1,read(),result(read(),ModernFilteredChat.Status.OK,true,64,false,rows,null));
        var bytes=ModernApplicationProtocol.encode(m,33);assertTrue(bytes.length>65536);assertTrue(bytes.length<ModernFilteredChatProtocol.MAX_FRAME_BYTES);
        assertEquals(m,ModernApplicationProtocol.decode(bytes,33));var stream=new ByteArrayInputStream(java.util.stream.IntStream.range(0,2).collect(java.io.ByteArrayOutputStream::new,(out,i)->out.writeBytes(bytes),(a,b)->a.writeBytes(b.toByteArray())).toByteArray());
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(stream,33));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(stream,33));assertEquals(0,stream.available());
    }
    @Test void allTruncationsReservedEnumsUtf8AndOversizeAreRejected()throws Exception{
        var r=send();var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.FilteredChatResult(1,r,result(r,ModernFilteredChat.Status.OK,true,0,false,List.of(),receipt(r))),33);
        int tail=76+r.text().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        for(int[] change:new int[][]{{37,1},{38,1},{39,1},{72,2},{73,3},{tail,5},{tail+1,4},{tail+2,2},{tail+3,1},{tail+5,65},{tail+6,1},{bytes.length-1,255}}){
            var bad=bytes.clone();bad[change[0]]=(byte)change[1];assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,33));}
        for(int length=0;length<bytes.length;length++){var shortFrame=java.util.Arrays.copyOf(bytes,length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame,33));}
        var extra=java.util.Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,33));
        var header=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(header).putInt(4,ModernFilteredChatProtocol.MAX_FRAME_BYTES+1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),33));
    }
    @Test void filteredCapabilityIsExplicitAndStrippedForOldVersionWithoutWeakeningDependencies()throws Exception{
        int caps=39|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT;
        for(int bit:new int[]{1,2,4,32,ModernSessionProtocol.CAP_CHAT,ModernSessionProtocol.CAP_DIRECT_CHAT}){
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,0));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps,caps&~bit));}
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_FILTERED_CHAT);
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,33,caps,7,1));
        for(int version:new int[]{31,32,33}){
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(new ModernSessionBoundary(ModernSessionBoundary.Policy.rooms(false).withFilteredChat().withMaxVersion(version)).acceptHello(hello));
            assertEquals(version==31?39:version==32?caps&~ModernSessionProtocol.CAP_FILTERED_CHAT:caps,ack.capabilities());}
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,32,caps,7,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,32,1,caps)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(caps,caps,32));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->new ModernSessionBoundary(ModernSessionBoundary.Policy.rooms(false).withDirectChat()).acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,33,caps,caps,1))));
    }
    @Test void trustedRoomsResultRoundTripsReceiptAndSilencedDenialWithoutInventedSelfInbox()throws Exception{
        try(var f=new ModernSpeechAuthorityTest.Fixture()){
            var p=f.player(7,1);var initial=f.read(p);
            f.rooms.moderatedDirectChat(p.owner(),new ModernDirectChatProtocol.Request(p.scope(),0,initial.token().membership(),
                    1,0,ModernDirectChat.Action.PREFERENCES,null,new ModernDirectChat.Preferences(true,false,true),""),ModernSpeechAuthorityTest.USER);
            var request=new ModernFilteredChat.Request(initial.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.WORLD,1,0,"不接自己的世界訊息");
            var accepted=f.rooms.filteredChatAuthorized(p.owner(),request,ModernSpeechAuthorityTest.USER);
            assertEquals(0,accepted.snapshot().cursor());assertTrue(accepted.snapshot().messages().isEmpty());assertNotNull(accepted.receipt());
            var message=new ModernApplicationProtocol.FilteredChatResult(1,request,accepted);
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,33),33));
            f.mute(p,20,false);request=new ModernFilteredChat.Request(initial.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.TRADE,2,0,"禁言");
            var denied=f.rooms.filteredChatAuthorized(p.owner(),request,ModernSpeechAuthorityTest.USER);
            assertEquals(ModernFilteredChat.Status.SILENCED,denied.status());assertNull(denied.receipt());assertEquals(0,denied.snapshot().cursor());
            message=new ModernApplicationProtocol.FilteredChatResult(2,request,denied);
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,33),33));
        }
    }
    @Test void finalLegalSequenceCanBeAcceptedWithoutCounterWrap()throws Exception{
        var r=new ModernFilteredChat.Request(new ModernFilteredChat.Token(read().token().scope(),0,1),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.TRADE,Long.MAX_VALUE,0,"last");
        var m=new ModernApplicationProtocol.FilteredChatResult(1,r,result(r,ModernFilteredChat.Status.OK,false,0,false,List.of(),receipt(r)));
        assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,33),33));
        assertThrows(IllegalArgumentException.class,()->result(r,ModernFilteredChat.Status.OK,true,0,false,List.of(),receipt(r)));
    }
}
