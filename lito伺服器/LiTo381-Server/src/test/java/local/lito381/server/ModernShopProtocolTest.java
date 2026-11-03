package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;

class ModernShopProtocolTest {
    static final String BODY="000000070000000000000d05ffffffffffffffff0200000001000000"
            +"00000000000000000000000000000000"+"00000000000000020000000000000003"
            +"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+"0000000000009c4a0000000000000003";
    static final String BUY_HEX="4c544d310000007cfedcba9800200000"+BODY;
    static final String BUSY_HEX="4c544d3100000080fedcba9800210000"+BODY+"030b0000";
    static ModernShopProtocol.Request golden(){byte[] token=new byte[32];Arrays.fill(token,(byte)0xaa);return new ModernShopProtocol.Request(
            new ModernRooms.Scope(7,3333,-1,2),Action.BUY,0,0,2,3,new Fingerprint(token),40010,3);}
    static void rejects(byte[] bytes){assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,8));}
    @Test void independentGoldenRequestAndBusyReplyHaveExactBytesAndFullEpochWidth()throws Exception {
        var request=new ModernApplicationProtocol.ShopRequest(0xfedcba98,golden());
        assertArrayEquals(HexFormat.of().parseHex(BUY_HEX),ModernApplicationProtocol.encode(request,8));
        assertEquals(request,ModernApplicationProtocol.decode(HexFormat.of().parseHex(BUY_HEX),8));
        var reply=new ModernApplicationProtocol.ShopResult(request.requestId(),request.request(),ModernShopProtocol.Reply.exceptional(Status.BUSY,Reason.BUSY));
        assertArrayEquals(HexFormat.of().parseHex(BUSY_HEX),ModernApplicationProtocol.encode(reply,8));
        assertEquals(reply,ModernApplicationProtocol.decode(HexFormat.of().parseHex(BUSY_HEX),8));
    }
    @Test void everyTruncationUnknownKindReservedStatusAndUnusedFieldFailsClosed() {
        byte[] valid=HexFormat.of().parseHex(BUY_HEX);
        for(int i=0;i<valid.length;i++)rejects(Arrays.copyOf(valid,i));
        for(int offset:new int[]{14,15,37,38,39,41,42,43}){byte[] malformed=valid.clone();malformed[offset]=1;rejects(malformed);}
        byte[] malformed=valid.clone();malformed[40]=3;rejects(malformed);
        malformed=valid.clone();malformed[40]=0;rejects(malformed); // READ must not carry mutation facts.
        malformed=valid.clone();ByteBuffer.wrap(malformed).putLong(116,1_000_001);rejects(malformed);
        for(int offset:new int[]{124,125,126,127}){malformed=HexFormat.of().parseHex(BUSY_HEX);malformed[offset]=(byte)255;rejects(malformed);}
    }
    @Test void oldVersionsNeverDecodeOrEncodeNewKindsAndOldCapabilitiesRemainOptional()throws Exception {
        var r=new ModernApplicationProtocol.ShopRequest(1,golden());
        for(int version=1;version<8;version++){int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(r,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(HexFormat.of().parseHex(BUY_HEX),v));}
        assertEquals(15,ModernSessionBoundary.Policy.current().supportedCapabilities());assertEquals(31,ModernSessionBoundary.Policy.equipment().supportedCapabilities());
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true).supportedCapabilities()&512);
        var policy=ModernSessionBoundary.Policy.world(true,true,true);
        for(int version=1;version<=8;version++){
            var b=new ModernSessionBoundary(policy);var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(b.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,version,1023,7,1))));
            assertEquals(version==8?512:0,ack.capabilities()&512);
        }
        for(int dependency:new int[]{4,32,64,128}){int missing=1023&~dependency;assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(missing,0));}
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(1023,767,7));
    }
    @Test void fullSnapshotRoundTripsNativeRoomUnreadyStateAndSourceNames()throws Exception {
        var fixture=new ModernRoomsTest.Fixture();var player=fixture.player(7,3333,80,0);fixture.create(player);
        fixture.act(player,ModernRooms.Action.READY,1);assertTrue(fixture.rooms.unreadyForActivity(player.owner()));
        var current=fixture.rooms.execute(player.owner(),new ModernRooms.Command(player.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var joined=current.view().joined().summary();var request=new ModernShopProtocol.Request(player.scope(),Action.READ,joined.id(),joined.revision(),0,0,new Fingerprint(new byte[32]),0,0);
        var a=ModernShopAuthorityTest.authority(20000);var outcome=a.apply(Action.READ,0,0,null,0,0,ModernShopAuthorityTest.catalog());
        var reply=new ModernApplicationProtocol.ShopResult(1,request,new ModernShopProtocol.Reply(outcome,current));
        byte[] bytes=ModernApplicationProtocol.encode(reply,8);assertEquals(reply,ModernApplicationProtocol.decode(bytes,8));
        assertFalse(((ModernApplicationProtocol.ShopResult)ModernApplicationProtocol.decode(bytes,8)).reply().rooms().view().joined().members().get(0).ready());
        for(int offset:new int[]{192,194}){byte[] invalid=bytes.clone();ByteBuffer.wrap(invalid).putShort(offset,(short)257);rejects(invalid);}
    }
    @Test void maximalDisplayCatalogAndDirectoryFitBeforeAnyNetworkAllocation()throws Exception {
        var fixture=new ModernRoomsTest.Fixture();var player=fixture.player(7,3333,80,0);fixture.create(player);
        for(int i=0;i<99;i++)fixture.create(fixture.player());
        var rows=new ArrayList<Product>();var owned=new ArrayList<Owned>();
        for(int i=1;i<=256;i++){rows.add(new Product(i,i,Long.MAX_VALUE,1,"x".repeat(256),Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,
                ModernShopCatalog.Support.HEALING,new ModernShopCatalog.Healing(1,Long.MAX_VALUE,1,Long.MAX_VALUE,10000)));owned.add(new Owned(i,1_000_000,Long.MAX_VALUE));}
        var room=fixture.rooms.execute(player.owner(),new ModernRooms.Command(player.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var summary=room.view().joined().summary();var echo=new ModernShopProtocol.Request(player.scope(),Action.READ,summary.id(),summary.revision(),0,0,new Fingerprint(new byte[32]),0,0);
        var snap=new Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,new Fingerprint(new byte[32]),rows,owned);
        var msg=new ModernApplicationProtocol.ShopResult(1,echo,new ModernShopProtocol.Reply(new Outcome(Status.OK,Reason.NONE,snap),room));
        var bytes=ModernApplicationProtocol.encode(msg,8);assertTrue(bytes.length>65536);assertTrue(bytes.length<=ModernShopProtocol.MAX_FRAME_BYTES);
        assertEquals(msg,ModernApplicationProtocol.decode(bytes,8));
    }
    @Test void oversizedHeaderRejectedBeforeBodyAllocationAndNestedRoomCannotBecomeArbitraryMessage()throws Exception {
        byte[] header=HexFormat.of().parseHex(BUSY_HEX);ByteBuffer.wrap(header).putInt(4,ModernShopProtocol.MAX_FRAME_BYTES+1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new java.io.ByteArrayInputStream(header)));
        var scope=golden().scope();var invalidSnapshot=new Snapshot(1,0,100,100,new Fingerprint(new byte[32]),List.of(),List.of());
        assertThrows(IllegalArgumentException.class,()->new ModernShopProtocol.Reply(new Outcome(Status.OK,Reason.NONE,invalidSnapshot),null));
        assertThrows(IllegalArgumentException.class,()->new Outcome(Status.OK,Reason.BACKEND,invalidSnapshot));
        assertThrows(IllegalArgumentException.class,()->new ModernShopProtocol.Request(scope,Action.READ,1,0,0,0,new Fingerprint(new byte[32]),0,0));
    }
}
