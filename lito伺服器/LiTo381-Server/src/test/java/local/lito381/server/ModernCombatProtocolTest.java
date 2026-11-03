package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernCombatProtocol.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernCombatProtocolTest {
    // Independent literal-width arithmetic, shared with Rust. The embedded
    // WORLD bytes retain the previous cross-language golden unchanged.
    static final String REQUEST_HEX="4c544d3100000048fedcba9800160000000000070000000000000d05ffffffffffffffff030000000000000000000009000000000000000600000000000000000000000000000000";
    static final String NONE_HEX="4c544d310000004cfedcba9800170000000000070000000000000d05ffffffffffffffff03000000000000000000000900000000000000060000000000000000000000000000000003010000";
    static final String FRAME_PREFIX="4c544d31000001a1fedcba9800170000000000070000000000000d05ffffffffffffffff03000000000000000000000900000000000000060000000000000000000000000000000000000100";
    static final String VITAL_HEX="000000000000000100000000ee6b280000000001dcd650000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002faf080";
    static final String FRAME_HEX=FRAME_PREFIX+ModernWorldProtocolTest.FRAME_HEX.substring(248)+"00010000"+VITAL_HEX;
    static Request read(){return new Request(new ModernRooms.Scope(7,3333,-1,3),Action.READ,9,6,0,0);}
    static Frame frame(){return new Frame(ModernWorldProtocolTest.frame(),List.of(new Vital(1,4_000_000_000L,8_000_000_000L,0,0,Long.MIN_VALUE,0,0,0,0,0,0,50_000_000)));}
    static byte[] hex(String value){return HexFormat.of().parseHex(value);}
    @Test void exactJavaRustGoldenRequestNoneAndSignedI64Frame() throws Exception {
        assertEquals(REQUEST_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(new ModernApplicationProtocol.CombatRequest(0xfedcba98,read()))));
        var none=new ModernApplicationProtocol.CombatResult(0xfedcba98,read(),new Reply(Status.UNAVAILABLE,Reason.NOT_ACTIVE,null));
        assertEquals(NONE_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(none)));
        var full=new ModernApplicationProtocol.CombatResult(0xfedcba98,read(),new Reply(Status.OK,Reason.NONE,frame()));
        assertEquals(FRAME_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(full)));
        assertEquals(full,ModernApplicationProtocol.decode(hex(FRAME_HEX)));
        assertEquals(none,ModernApplicationProtocol.decode(hex(NONE_HEX)));
    }
    @Test void allTruncationsAndTrailingReservedUnknownKindsAreRejected() {
        for(var text:List.of(REQUEST_HEX,NONE_HEX,FRAME_HEX)) {
            var good=hex(text);
            for(int n=0;n<good.length;n++){var cut=Arrays.copyOf(good,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut));}
            var trailing=Arrays.copyOf(good,good.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(trailing));
            for(int index:new int[]{14,38,39}){var bad=good.clone();bad[index]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad));}
        }
        var bad=hex(FRAME_HEX);bad[bad.length-104+49]=1;
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad));
    }
    @Test void requestPositiveScopeAndUnusedFieldsDoNotAlias() {
        var scope=read().scope();
        assertThrows(IllegalArgumentException.class,()->new Request(scope,Action.READ,0,6,0,0));
        assertThrows(IllegalArgumentException.class,()->new Request(scope,Action.READ,9,6,1,0));
        assertThrows(IllegalArgumentException.class,()->new Request(scope,Action.ATTACK,9,6,0,2));
        assertDoesNotThrow(()->new Request(scope,Action.ATTACK,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE));
    }
    @Test void exactCoverageAndNumericBoundsRejectCorruptAuthorityValues() {
        long[][] fields={{8,-1},{8,8_000_000_001L},{16,0},{24,-1},{24,1},{32,-1},{56,-1},{64,-1},{64,2},{72,-1},{72,2},{80,1},{88,2},{96,999_999},{96,10_000_000_001L}};
        for(var field:fields){var bad=hex(FRAME_HEX);ByteBuffer.wrap(bad).putLong(bad.length-104+(int)field[0],field[1]);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad),Arrays.toString(field));}
        for(int offset:new int[]{0,48}){var bad=hex(FRAME_HEX);if(offset==0)ByteBuffer.wrap(bad).putLong(bad.length-104,2);else bad[bad.length-104+48]=19;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad));}
        assertThrows(IllegalArgumentException.class,()->new Frame(frame().world(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Frame(frame().world(),List.of(frame().vitals().get(0),frame().vitals().get(0))));
    }
    @Test void onlyUnavailableNotActiveMayOmitSnapshotAndIdentityMustMatch() throws Exception {
        for(var status:Status.values())for(var reason:Reason.values())if(!(status==Status.UNAVAILABLE && reason==Reason.NOT_ACTIVE))assertThrows(IllegalArgumentException.class,()->new Reply(status,reason,null));
        var wrong=new Request(new ModernRooms.Scope(42,3333,-1,3),Action.READ,9,6,0,0);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.CombatResult(1,wrong,new Reply(Status.OK,Reason.NONE,frame()))));
        var stale=new Request(read().scope(),Action.READ,9,7,0,0);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.CombatResult(1,stale,new Reply(Status.OK,Reason.NONE,frame()))));
    }
    @Test void fullMatchDeathAndEventTargetAreStrict() {
        assertThrows(IllegalArgumentException.class,()->new Vital(1,0,20,0,0,0,0,0,0,0,0,0,50_000_000));
        assertDoesNotThrow(()->new Vital(1,0,20,0,0,0,0,0,0,1,1,0,50_000_000));
        var selfTarget=new Vital(1,20,20,0,0,0,0,1,1,0,0,1,50_000_000);
        assertThrows(IllegalArgumentException.class,()->new Frame(frame().world(),List.of(selfTarget)));
        var unknownTarget=new Vital(1,20,20,0,0,0,0,1,1,0,0,2,50_000_000);
        assertThrows(IllegalArgumentException.class,()->new Frame(frame().world(),List.of(unknownTarget)));
    }
    @Test void combatRequiresExplicitPolicyAndEveryDependencyOldMasksStayStable() throws Exception {
        assertEquals(15,ModernSessionBoundary.Policy.current().supportedCapabilities());
        assertEquals(31,ModernSessionBoundary.Policy.equipment().supportedCapabilities());
        assertEquals(303,ModernSessionBoundary.Policy.rooms(false).supportedCapabilities());
        assertEquals(319,ModernSessionBoundary.Policy.rooms(true).supportedCapabilities());
        int descriptor=ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        assertEquals(367,ModernSessionBoundary.Policy.world(false).supportedCapabilities()&~descriptor);
        assertEquals(383,ModernSessionBoundary.Policy.world(true).supportedCapabilities()&~descriptor);
        assertEquals(511,ModernSessionBoundary.Policy.world(true,true).supportedCapabilities()&~descriptor);
        for(var policy:List.of(ModernSessionBoundary.Policy.world(false),ModernSessionBoundary.Policy.world(true),
                ModernSessionBoundary.Policy.world(true,true)))assertEquals(descriptor,policy.supportedCapabilities()&descriptor);
        assertThrows(IllegalArgumentException.class,()->ModernSessionBoundary.Policy.world(false,true));
        for(int bit=1;bit<=64;bit<<=1){int mask=255^bit;assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(mask,0));}
        ModernSessionProtocol.validateCapabilities(255,7);
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(256,0));
    }
}
