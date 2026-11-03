package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernWorldProtocolTest {
    // Independent literal-width big-endian construction, not output of either codec.
    static final String REQUEST_HEX="4c544d3100000078fedcba9800140000000000070000000000000d05ffffffffffffffff030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    static final String FRAME_HEX="4c544d3100000165fedcba9800150000000000070000000000000d05ffffffffffffffff030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000009000000000000000600000000000000010100000000000000000000050000000000000006000204100000021c0001520001000101000000000000070000000000000d0500010000000000070000000000000d0500000000ee6b2800000301000100000000014e000153000100010000020222222222222222222222222222222222222222222222222222222222222222227fff80000000c012111111111111111111111111111111111111111111111111111111111111111100010000000000000000000100007fd4000080100200000000000000000000000000000002faf080";
    static Request read() {return new Request(new ModernRooms.Scope(7,3333,-1,3),Action.READ,0,0,0,0,0,0,0,0,ZERO_DIGEST);}
    static Frame frame() {
        var key=new TournamentPresence.CharacterKey(7,3333);
        var member=new ModernRooms.Member(key,"N","S",4_000_000_000L,3,1,0,true);
        var summary=new ModernRooms.Summary(5,6,new ModernRooms.Config("R",2,4,540,16),1,1,true);
        var room=new ModernRooms.RoomSnapshot(summary,key,List.of(member));
        var manifest=new ModernWorldMap.Manifest(514,List.of(new ModernWorldMap.SectorManifest(
                new ModernWorldMap.SectorKey(0x7fff,0x8000),49170,"11".repeat(32))),"22".repeat(32));
        var actor=new ModernBattleEngine.Actor(1,member,new ModernWorldMap.Cell(32724,32784),2,false,0,50_000_000);
        return new Frame(9,6,1,Phase.LOADING,AbortReason.NONE,room,manifest,List.of(actor));
    }
    @Test void javaRustExactRequestAndFullFrameGolden() throws Exception {
        var request=new ModernApplicationProtocol.WorldRequest(0xfedcba98,read());
        var result=new ModernApplicationProtocol.WorldResult(0xfedcba98,read(),new Reply(Status.OK,Reason.NONE,frame()));
        assertEquals(REQUEST_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(request)));
        assertEquals(FRAME_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(result)));
        assertEquals(request,ModernApplicationProtocol.decode(HexFormat.of().parseHex(REQUEST_HEX)));
        assertEquals(result,ModernApplicationProtocol.decode(HexFormat.of().parseHex(FRAME_HEX)));
    }
    @Test void everyTruncationReservedAndTrailingBytesFail() throws Exception {
        for(String hex:List.of(REQUEST_HEX,FRAME_HEX)) {
            byte[] good=HexFormat.of().parseHex(hex);
            for(int n=0;n<good.length;n++) {
                byte[] cut=Arrays.copyOf(good,n);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut),"length "+n);
            }
            byte[] extra=Arrays.copyOf(good,good.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra));
            good[39]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(good));
        }
    }
    @Test void unusedRequestFieldsAndNegativeNumericAliasesRejected() {
        var s=read().scope();
        for(int field=0;field<7;field++) {
            final int f=field;
            assertThrows(IllegalArgumentException.class,()->new Request(s,Action.READ,f==0?1:0,f==1?1:0,f==2?1:0,
                    f==3?1:0,0,f==4?1:0,f==5?1:0,0,f==6?"11".repeat(32):ZERO_DIGEST));
        }
        assertDoesNotThrow(()->new Request(s,Action.MOVE,7,0,0,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,ZERO_DIGEST));
        assertThrows(IllegalArgumentException.class,()->new Request(s,Action.LOADED,0,0,0,9,6,0,0,0,ZERO_DIGEST));
        assertThrows(IllegalArgumentException.class,()->new Request(s,Action.LEAVE,0,0,0,-1,6,0,0,0,ZERO_DIGEST));
    }
    @Test void noFrameMeansNoneOnlyAcknowledgedLeaveClearsWorld() throws Exception {
        var leave=new Request(read().scope(),Action.LEAVE,0,0,0,9,6,0,0,0,ZERO_DIGEST);
        var ok=new ModernApplicationProtocol.WorldResult(1,leave,new Reply(Status.OK,Reason.NONE,null));
        assertEquals(ok,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(ok)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(
                new ModernApplicationProtocol.WorldResult(1,leave,new Reply(Status.OK,Reason.NONE,frame()))));
        assertThrows(IllegalArgumentException.class,()->new Reply(Status.OK,Reason.BLOCKED,null));
        assertThrows(IllegalArgumentException.class,()->new Reply(Status.DENIED,Reason.NONE,null));
    }
    @Test void prepareLoadedAndMoveMustConfirmActualRequestedOutcome() throws Exception {
        var f=frame();var scope=read().scope();
        var prepare=new Request(scope,Action.PREPARE,0,5,5,0,0,0,0,0,ZERO_DIGEST);
        assertDoesNotThrow(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,prepare,new Reply(Status.OK,Reason.NONE,f))));
        var loaded=new Request(scope,Action.LOADED,0,0,0,9,6,0,0,0,f.manifest().digest());
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,loaded,new Reply(Status.OK,Reason.NONE,f))));
        var a=f.actors().get(0);var moved=new ModernBattleEngine.Actor(1,a.member(),new ModernWorldMap.Cell(32725,32784),2,true,1,a.stepIntervalNanos());
        var active=new Frame(9,6,2,Phase.ACTIVE,AbortReason.NONE,f.room(),f.manifest(),List.of(moved));
        var move=new Request(scope,Action.MOVE,2,0,0,9,6,1,32724,32784,ZERO_DIGEST);
        assertDoesNotThrow(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,move,new Reply(Status.OK,Reason.NONE,active))));
        var wrong=new Request(scope,Action.MOVE,2,0,0,9,6,2,32724,32784,ZERO_DIGEST);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,wrong,new Reply(Status.OK,Reason.NONE,active))));
    }
    @Test void exactSourceIdentityNotObjectIdAlone() throws Exception {
        var f=frame();var other=new Request(new ModernRooms.Scope(42,3333,-1,3),Action.READ,0,0,0,0,0,0,0,0,ZERO_DIGEST);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,other,new Reply(Status.OK,Reason.NONE,f))));
        var wrong=new Request(read().scope(),Action.READ,0,0,0,9,7,0,0,0,ZERO_DIGEST);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(1,wrong,new Reply(Status.OK,Reason.NONE,f))));
    }
    @Test void manifestSortedCountTotalSizeAndHashAreStrict() {
        var f=frame();var sector=f.manifest().sectors().get(0);
        var cases=new ArrayList<ModernWorldMap.Manifest>();
        cases.add(new ModernWorldMap.Manifest(514,List.of(sector,sector),f.manifest().digest()));
        cases.add(new ModernWorldMap.Manifest(514,List.of(),f.manifest().digest()));
        cases.add(new ModernWorldMap.Manifest(514,List.of(sector),"AA".repeat(32)));
        cases.add(new ModernWorldMap.Manifest(514,List.of(new ModernWorldMap.SectorManifest(sector.key(),8*1024*1024+1,sector.rawSha256())),f.manifest().digest()));
        var large=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<5;i++)large.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(0x7fff,0x8000+i),8*1024*1024,sector.rawSha256()));
        cases.add(new ModernWorldMap.Manifest(514,large,f.manifest().digest()));
        for(var manifest:cases)assertThrows(IllegalArgumentException.class,()->new Frame(9,6,1,f.phase(),f.abortReason(),f.room(),manifest,f.actors()));
    }
    @Test void phaseLoadedActorSequenceAndPositionMustAgree() {
        var f=frame();var a=f.actors().get(0);
        assertThrows(IllegalArgumentException.class,()->new Frame(9,6,1,Phase.ACTIVE,AbortReason.NONE,f.room(),f.manifest(),f.actors()));
        assertThrows(IllegalArgumentException.class,()->new Frame(9,6,1,Phase.ABORTED,AbortReason.NONE,f.room(),f.manifest(),f.actors()));
        var loaded=new ModernBattleEngine.Actor(1,a.member(),a.position(),2,true,0,a.stepIntervalNanos());
        assertThrows(IllegalArgumentException.class,()->new Frame(9,6,1,Phase.PREPARING,AbortReason.NONE,f.room(),f.manifest(),List.of(loaded)));
        var outside=new ModernBattleEngine.Actor(1,a.member(),new ModernWorldMap.Cell(Integer.MAX_VALUE,0),2,false,0,a.stepIntervalNanos());
        assertThrows(IllegalArgumentException.class,()->new Frame(9,6,1,Phase.LOADING,AbortReason.NONE,f.room(),f.manifest(),List.of(outside)));
    }
    @Test void oversizedReplyRejectedBeforeAllocatingBodyAndRequestKeeps64KiB() {
        for(int kind:new int[]{20,21}) {
            int limit=kind==21?MAX_FRAME_BYTES:ModernApplicationProtocol.MAX_FRAME_BYTES;
            var header=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(limit+1).putInt(1).putShort((short)kind).putShort((short)0).array();
            var stream=new ByteArrayInputStream(header);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(stream));
            assertEquals(0,stream.available());
        }
    }
    @Test void worldCapabilityIsOptInAndLegacyPoliciesDoNotGrow() throws Exception {
        assertEquals(15,ModernSessionBoundary.Policy.current().supportedCapabilities());
        assertEquals(31,ModernSessionBoundary.Policy.equipment().supportedCapabilities());
        assertEquals(303,ModernSessionBoundary.Policy.rooms(false).supportedCapabilities());
        assertEquals(319,ModernSessionBoundary.Policy.rooms(true).supportedCapabilities());
        int descriptor=ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        assertEquals(367,ModernSessionBoundary.Policy.world(false).supportedCapabilities()&~descriptor);
        assertEquals(383,ModernSessionBoundary.Policy.world(true).supportedCapabilities()&~descriptor);
        for(var policy:List.of(ModernSessionBoundary.Policy.world(false),ModernSessionBoundary.Policy.world(true)))
            assertEquals(descriptor,policy.supportedCapabilities()&descriptor);
        for(int mask:new int[]{64,68,96,128,254,256})assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(mask,0));
        ModernSessionProtocol.validateCapabilities(111,7);ModernSessionProtocol.validateCapabilities(127,7);
    }
}
