package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernSessionBoundaryTest {
    @Test void naturalRegenCapabilityRequiresWeightCombatAndNeverLeaksIntoVersionsOneThroughEleven()throws Exception {
        assertEquals(8192,ModernSessionProtocol.CAP_REGEN);var enabled=ModernSessionBoundary.Policy.world(true,true,true,true,false,false,true);
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true,true,true,true).supportedCapabilities()&8192);
        assertEquals(0,ModernSessionBoundary.Policy.current().supportedCapabilities()&8192);
        for(int version:new int[]{1,9,10,11,12}) {
            var boundary=new ModernSessionBoundary(enabled);boundary.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,version,16383,7,1)));
            assertEquals(version==12?8192:0,boundary.negotiated().orElseThrow().capabilities()&8192);
        }
        for(int missing:new int[]{1024,128})assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,12,16383&~missing,7,1)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(16383,16383,11));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,11,1,16383)));
    }
    @Test void arrowCapabilityIsExplicitDependentAndVersionElevenOnly()throws Exception {
        assertEquals(2097151,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT|ModernSessionProtocol.CAP_BATTLE_DATA));assertEquals(4096,ModernSessionProtocol.CAP_ARROW);
        assertEquals(0,ModernSessionBoundary.Policy.current().supportedCapabilities()&4096);
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true,true,true).supportedCapabilities()&4096);
        var policy=ModernSessionBoundary.Policy.world(true,true,true,true,true,true);
        for(int version:new int[]{10,11}) {
            var boundary=new ModernSessionBoundary(policy);boundary.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,version,8191,7,1)));
            assertEquals(version==11?4096:0,boundary.negotiated().orElseThrow().capabilities()&4096);
        }
        for(int bit:new int[]{2048,1024,512,128})assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,11,8191&~bit,7,1)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(8191,8191,10));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,10,1,8191)));
    }
    // Identical to modern_lito/protocol.rs; request ID deliberately exercises the u32 high bit.
    private static final String GOLDEN_HELLO =
            "4c544d3100000020fedcba9800010000000100010000000f0000000700010000";
    private static final String GOLDEN_ACK =
            "4c544d3100000018fedcba9800020000000100010000000f";

    private static byte[] bytes(String hex) { return HexFormat.of().parseHex(hex); }
    private static ModernSessionProtocol.Hello hello() {
        return new ModernSessionProtocol.Hello(0xfedcba98, 1, 1, 15, 7, 1);
    }
    private static ModernSessionBoundary boundary() {
        return new ModernSessionBoundary(ModernSessionBoundary.Policy.current());
    }

    @Test void rustGoldenVectorsMatchBothDirections() throws Exception {
        assertArrayEquals(bytes(GOLDEN_HELLO), ModernSessionProtocol.encode(hello()));
        assertEquals(hello(), ModernSessionProtocol.decode(bytes(GOLDEN_HELLO)));
        var ack = new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 15);
        assertArrayEquals(bytes(GOLDEN_ACK), ModernSessionProtocol.encode(ack));
        assertEquals(ack, ModernSessionProtocol.decode(bytes(GOLDEN_ACK)));
        var output = new ByteArrayOutputStream();
        ModernSessionProtocol.writeFrame(output, hello());
        assertArrayEquals(bytes(GOLDEN_HELLO), output.toByteArray());
    }

    @Test void realNegotiationReturnsMatchingAckButNeverAuthenticates() throws Exception {
        var session = boundary();
        assertEquals(ModernSessionBoundary.State.WAITING_FOR_HELLO, session.state());
        assertTrue(session.negotiated().isEmpty());
        assertArrayEquals(bytes(GOLDEN_ACK), session.acceptHello(bytes(GOLDEN_HELLO)));
        assertEquals(ModernSessionBoundary.State.NEGOTIATED, session.state());
        assertEquals(new ModernSessionBoundary.Negotiated(0xfedcba98, 1, 1, 15),
                session.negotiated().orElseThrow());
        assertThrows(ModernSessionProtocol.ProtocolException.class,
                () -> session.acceptHello(bytes(GOLDEN_HELLO)));
        assertEquals(ModernSessionBoundary.State.CLOSED, session.state());
        assertTrue(session.negotiated().isEmpty());
        assertEquals(ModernSessionProtocol.ErrorCode.CLOSED,
                assertThrows(ModernSessionProtocol.ProtocolException.class,
                        () -> session.acceptHello(bytes(GOLDEN_HELLO))).code());
    }

    @Test void schemaVersionAndBothSidesRequiredCapabilitiesAreEnforced() throws Exception {
        assertRejected(new ModernSessionProtocol.Hello(1, ModernSessionProtocol.MAX_VERSION + 1, ModernSessionProtocol.MAX_VERSION + 2, 3, 3, 1),
                ModernSessionProtocol.ErrorCode.UNSUPPORTED_VERSION);
        assertRejected(new ModernSessionProtocol.Hello(1, 1, 1, 3, 3, 4),
                ModernSessionProtocol.ErrorCode.UNSUPPORTED_SCHEMA);
        assertRejected(new ModernSessionProtocol.Hello(1, 1, 1, 1, 1, 1),
                ModernSessionProtocol.ErrorCode.UNSUPPORTED_CAPABILITY);
        var narrow = new ModernSessionBoundary(new ModernSessionBoundary.Policy(1, 1));
        assertEquals(ModernSessionProtocol.ErrorCode.UNSUPPORTED_CAPABILITY,
                assertThrows(ModernSessionProtocol.ProtocolException.class,
                        () -> narrow.acceptHello(bytes(GOLDEN_HELLO))).code());
        var range = boundary();
        var rangeAck = ModernSessionProtocol.decode(range.acceptHello(ModernSessionProtocol.encode(
                new ModernSessionProtocol.Hello(1, 1, 65535, 3, 3, 1))));
        assertEquals(new ModernSessionProtocol.Ack(1, ModernSessionProtocol.MAX_VERSION, 1, 3), rangeAck);
        var versionFourOnly = boundary();
        assertEquals(new ModernSessionProtocol.Ack(1, 4, 1, 3), ModernSessionProtocol.decode(
                versionFourOnly.acceptHello(ModernSessionProtocol.encode(
                        new ModernSessionProtocol.Hello(1, 4, 4, 3, 3, 1)))));
    }

    @Test void versionTwoUsesOldKnownCapabilityMaskAndKeepsVersionOneClientsCompatible() throws Exception {
        var modern = new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true, true));
        var offered = new ModernSessionProtocol.Hello(19, 1, 2, 255, 7, 1);
        var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(modern.acceptHello(ModernSessionProtocol.encode(offered)));
        assertEquals(new ModernSessionProtocol.Ack(19, 2, 1, 255), ack);
        // Same server selects the old shape for an unchanged deployed client.
        var legacy = new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true, true));
        assertEquals(new ModernSessionProtocol.Ack(19, 1, 1, 255), ModernSessionProtocol.decode(legacy.acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(19, 1, 1, 255, 7, 1)))));
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                new ModernSessionProtocol.Hello(19, 1, 2, 255 | ModernSessionProtocol.CAP_SLOW_DATA, 7, 1)));
    }

    @Test void optionalCapabilitiesArePerConnectionAndNotNativeGlobalSchema() throws Exception {
        String previous = System.getProperty("lito.numeric.breakthrough");
        try {
            System.setProperty("lito.numeric.breakthrough", "0");
            var first = new ModernSessionBoundary(new ModernSessionBoundary.Policy(1, 1));
            var second = boundary();
            var firstHello = new ModernSessionProtocol.Hello(9, 1, 1, 3, 1, 1);
            assertEquals(new ModernSessionProtocol.Ack(9, 1, 1, 1),
                    ModernSessionProtocol.decode(first.acceptHello(ModernSessionProtocol.encode(firstHello))));
            System.setProperty("lito.numeric.breakthrough", "4");
            assertArrayEquals(bytes(GOLDEN_ACK), second.acceptHello(bytes(GOLDEN_HELLO)));
            assertEquals(1, first.negotiated().orElseThrow().numericSchema());
            assertEquals(1, first.negotiated().orElseThrow().capabilities());
            assertEquals(15, second.negotiated().orElseThrow().capabilities());
            first.close();
            assertEquals(ModernSessionBoundary.State.NEGOTIATED, second.state());
        } finally {
            if (previous == null) System.clearProperty("lito.numeric.breakthrough");
            else System.setProperty("lito.numeric.breakthrough", previous);
        }
    }

    @Test void characterDataRemainsOptionalAndOlderNavigationPeersStillNegotiateSeven() throws Exception {
        var oldServer = new ModernSessionBoundary(new ModernSessionBoundary.Policy(7, 3));
        assertEquals(new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 7),
                ModernSessionProtocol.decode(oldServer.acceptHello(bytes(GOLDEN_HELLO))));
        assertEquals(new ModernSessionProtocol.Ack(19, 1, 1, 7),
                ModernSessionProtocol.decode(boundary().acceptHello(ModernSessionProtocol.encode(
                        new ModernSessionProtocol.Hello(19, 1, 1, 7, 7, 1)))));
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                new ModernSessionProtocol.Hello(19, 1, 1, 16, 0, 1)));
    }

    @Test void equipmentIsRecognizedButOnlyAuthorityPolicyAdvertisesIt() throws Exception {
        var hello = new ModernSessionProtocol.Hello(0xfedcba98, 1, 1, 31, 7, 1);
        byte[] offered = ModernSessionProtocol.encode(hello);
        assertEquals("4c544d3100000020fedcba9800010000000100010000001f0000000700010000", HexFormat.of().formatHex(offered));
        assertEquals(hello, ModernSessionProtocol.decode(offered));
        assertEquals(15, ModernSessionBoundary.Policy.current().supportedCapabilities());
        assertEquals(3, ModernSessionBoundary.Policy.current().requiredCapabilities());
        assertEquals(31, ModernSessionBoundary.Policy.equipment().supportedCapabilities());
        assertEquals(3, ModernSessionBoundary.Policy.equipment().requiredCapabilities());
        assertEquals(new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 15),
                ModernSessionProtocol.decode(boundary().acceptHello(offered)));
        var equipment = new ModernSessionBoundary(ModernSessionBoundary.Policy.equipment());
        byte[] ack = equipment.acceptHello(offered);
        assertEquals("4c544d3100000018fedcba9800020000000100010000001f", HexFormat.of().formatHex(ack));
        assertEquals(new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 31), ModernSessionProtocol.decode(ack));
        assertEquals(ModernSessionBoundary.State.NEGOTIATED, equipment.state());
        assertEquals(31, equipment.negotiated().orElseThrow().capabilities());
    }

    @Test void equipmentDependenciesAreRequiredInEachOfferedSelectedAndRequiredMask() throws Exception {
        for (int capabilities : new int[] {16,17,19,20,23,24,27,32,64}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1, 1, 1, capabilities, 0, 1)));
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Ack(1, 1, 1, capabilities)));
        }
        for (int required : new int[] {16,17,19,20,23,24,27}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1, 1, 1, 31, required, 1)));
            assertThrows(IllegalArgumentException.class, () -> new ModernSessionBoundary.Policy(31, required));
        }
        assertDoesNotThrow(() -> ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 31, 31, 1)));
        // Dependency violations received over the wire close the boundary too.
        byte[] malformed = ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 31, 7, 1));
        java.nio.ByteBuffer.wrap(malformed).putInt(24, 16);
        assertMalformedClosed(malformed);
    }

    @Test void requiringEquipmentCannotSilentlyFallBackToReadOnlyPolicy() throws Exception {
        var required = new ModernSessionProtocol.Hello(1, 1, 1, 31, 31, 1);
        assertRejected(required, ModernSessionProtocol.ErrorCode.UNSUPPORTED_CAPABILITY);
        var authority = new ModernSessionBoundary(ModernSessionBoundary.Policy.equipment());
        assertEquals(new ModernSessionProtocol.Ack(1, 1, 1, 31),
                ModernSessionProtocol.decode(authority.acceptHello(ModernSessionProtocol.encode(required))));
        var readOnlyPeer = new ModernSessionBoundary(ModernSessionBoundary.Policy.equipment());
        assertEquals(new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 15),
                ModernSessionProtocol.decode(readOnlyPeer.acceptHello(bytes(GOLDEN_HELLO))));
    }

    @Test void roomsAreOptionalRequireNavigationAndDoNotImplyEquipmentAuthority() throws Exception {
        assertEquals(303, ModernSessionBoundary.Policy.rooms(false).supportedCapabilities());
        assertEquals(319, ModernSessionBoundary.Policy.rooms(true).supportedCapabilities());
        byte[] offered = ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(19, 1, 1, 63, 7, 1));
        for (var policy : ListHolder.POLICIES) {
            var boundary = new ModernSessionBoundary(policy);
            assertEquals(new ModernSessionProtocol.Ack(19, 1, 1, policy.supportedCapabilities() & ~ModernSessionProtocol.CAP_GM_BOTS),
                    ModernSessionProtocol.decode(boundary.acceptHello(offered)));
        }
        for (int invalid : new int[] {32,33,34,35,40,43}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1, 1, 1, invalid, 0, 1)));
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.encode(
                    new ModernSessionProtocol.Hello(1, 1, 1, 63, invalid, 1)));
            assertThrows(IllegalArgumentException.class, () -> new ModernSessionBoundary.Policy(63, invalid));
        }
        assertRejected(new ModernSessionProtocol.Hello(1, 1, 1, 63, 39, 1),
                ModernSessionProtocol.ErrorCode.UNSUPPORTED_CAPABILITY);
        assertDoesNotThrow(() -> ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 39, 39, 1)));
    }

    private static final class ListHolder {
        static final ModernSessionBoundary.Policy[] POLICIES = {
                ModernSessionBoundary.Policy.current(), ModernSessionBoundary.Policy.equipment(),
                ModernSessionBoundary.Policy.rooms(false), ModernSessionBoundary.Policy.rooms(true)};
    }

    @Test void malformedFramesUnknownsAndReservedFieldsFailClosed() throws Exception {
        for (int[] mutation : new int[][] {{0, 0}, {7, 33}, {12, 1}, {15, 1}, {20, 128}, {31, 1}}) {
            byte[] frame = bytes(GOLDEN_HELLO);
            frame[mutation[0]] = (byte) mutation[1];
            assertMalformedClosed(frame);
        }
        byte[] zeroId = bytes(GOLDEN_HELLO);
        Arrays.fill(zeroId, 8, 12, (byte) 0);
        assertMalformedClosed(zeroId);
        for (int length = 0; length < 32; length++) {
            assertMalformedClosed(Arrays.copyOf(bytes(GOLDEN_HELLO), length));
        }
        assertMalformedClosed(new byte[ModernSessionProtocol.MAX_FRAME_BYTES + 1]);
        assertMalformedClosed(Arrays.copyOf(bytes(GOLDEN_ACK), 25));
        assertMalformedClosed(bytes(GOLDEN_ACK)); // Reflection is not a client HELLO.
        assertMalformedClosed(null);
    }

    @Test void encodeRejectsBadCallerConstructedMessagesToo() {
        for (var invalid : new ModernSessionProtocol.Hello[] {
                new ModernSessionProtocol.Hello(0, 1, 1, 3, 3, 1),
                new ModernSessionProtocol.Hello(1, 2, 1, 3, 3, 1),
                new ModernSessionProtocol.Hello(1, 1, 65536, 3, 3, 1),
                new ModernSessionProtocol.Hello(1, 1, 1, 1, 3, 1),
                new ModernSessionProtocol.Hello(1, 1, 1, 128, 0, 1),
                new ModernSessionProtocol.Hello(1, 1, 1, 3, 3, 0)}) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,
                    () -> ModernSessionProtocol.encode(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> new ModernSessionBoundary.Policy(3, 0));
    }

    @Test void framedStreamsHandleFragmentationCoalescingAndZeroReads() throws Exception {
        byte[] combined = new byte[56];
        System.arraycopy(bytes(GOLDEN_HELLO), 0, combined, 0, 32);
        System.arraycopy(bytes(GOLDEN_ACK), 0, combined, 32, 24);
        var source = new ByteArrayInputStream(combined);
        InputStream fragmented = new InputStream() {
            boolean zero = true;
            @Override public int read() { return source.read(); }
            @Override public int read(byte[] target, int offset, int length) {
                zero = !zero;
                return zero ? 0 : source.read(target, offset, Math.min(length, 1));
            }
        };
        assertArrayEquals(bytes(GOLDEN_HELLO), ModernSessionProtocol.readFrame(fragmented));
        assertEquals(24, source.available());
        assertArrayEquals(bytes(GOLDEN_ACK), ModernSessionProtocol.readFrame(fragmented));
        assertEquals(0, source.available());
    }

    @Test void invalidLengthsAreRejectedBeforeReadingBodyAndNativeMagicBeforeHeader() {
        byte[] oversized = bytes(GOLDEN_HELLO);
        Arrays.fill(oversized, 4, 8, (byte) 0xff);
        var source = new ByteArrayInputStream(oversized);
        assertThrows(ModernSessionProtocol.ProtocolException.class,
                () -> ModernSessionProtocol.readFrame(source));
        assertEquals(16, source.available());
        var nativeBytes = new ByteArrayInputStream(new byte[32]);
        assertThrows(ModernSessionProtocol.ProtocolException.class,
                () -> ModernSessionProtocol.readFrame(nativeBytes));
        assertEquals(28, nativeBytes.available());
        for (int length = 0; length < 32; length++) {
            var truncated = new ByteArrayInputStream(Arrays.copyOf(bytes(GOLDEN_HELLO), length));
            assertThrows(ModernSessionProtocol.ProtocolException.class,
                    () -> ModernSessionProtocol.readFrame(truncated));
        }
    }

    private static void assertRejected(ModernSessionProtocol.Hello hello,
                                       ModernSessionProtocol.ErrorCode expected) throws Exception {
        var session = boundary();
        byte[] frame = ModernSessionProtocol.encode(hello);
        assertEquals(expected, assertThrows(ModernSessionProtocol.ProtocolException.class,
                () -> session.acceptHello(frame)).code());
        assertEquals(ModernSessionBoundary.State.CLOSED, session.state());
        assertTrue(session.negotiated().isEmpty());
    }

    private static void assertMalformedClosed(byte[] frame) {
        var session = boundary();
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> session.acceptHello(frame));
        assertEquals(ModernSessionBoundary.State.CLOSED, session.state());
        assertTrue(session.negotiated().isEmpty());
    }
}
