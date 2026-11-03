package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernNavigationProtocolTest {
    private static final String SELECT = "4c544d3100000024fedcba9800090000000000070000000000000d05ffffffffffffffff";
    private static final String RETURNED = "4c544d310000002cfedcba98000a00000c0000ff000000070000000000000d05ffffffffffffffff00000000";
    private static final String LEAVE = "4c544d3100000024fedcba98000d0000000000070000000000000d05ffffffffffffffff";
    private static final String LEFT = "4c544d31000000c3fedcba98000a00000d0001ff000000070000000000000d05ffffffffffffffff01040000"
            + "0000000000000d0500014e0003010000000000ee6b2800"
            + "000000000000000000000064000000000000000f7fffffffffffffff00026330"
            + "010000000000000000000064000000000000000f7fffffffffffffff00026331"
            + "020000000000000000000064000000000000000f7fffffffffffffff00026332"
            + "030000000000000000000064000000000000000f7fffffffffffffff00026333";
    static List<ModernApplicationProtocol.ChannelRow> channels() {
        return java.util.stream.IntStream.range(0, 4).mapToObj(id ->
                new ModernApplicationProtocol.ChannelRow(id, "channel" + id, id + 1, 1000,
                        80 - id * 10, id == 0 ? Long.MAX_VALUE : 89 - id * 10)).toList();
    }
    private static ModernApplicationProtocol.NavigationResult chosen(int action, int status, int phase, int channel) {
        return new ModernApplicationProtocol.NavigationResult(0xfedcba98, action, status, phase, channel, 7, 3333, -1,
                new ModernApplicationProtocol.CharacterRow(3333, "勇者", 3, 1, 4_000_000_000L), channels());
    }
    @Test void sharedGoldenSelectionAndReturnPreserveUnsignedEpoch() throws Exception {
        var selection = new ModernApplicationProtocol.CharacterSelection(0xfedcba98, 7, 3333, -1);
        assertArrayEquals(HexFormat.of().parseHex(SELECT), ModernApplicationProtocol.encode(selection));
        assertEquals(selection, ModernApplicationProtocol.decode(HexFormat.of().parseHex(SELECT)));
        var returned = new ModernApplicationProtocol.NavigationResult(0xfedcba98, 12, 0, 0, 255, 7, 3333, -1, null, List.of());
        assertArrayEquals(HexFormat.of().parseHex(RETURNED), ModernApplicationProtocol.encode(returned));
        assertEquals(returned, ModernApplicationProtocol.decode(HexFormat.of().parseHex(RETURNED)));
    }
    @Test void channelAndSelectedCharacterSnapshotRoundTripsWithI64Level() throws Exception {
        for (var message : List.of(chosen(9, 0, 1, 255), chosen(11, 0, 2, 3), chosen(11, 1, 1, 255),
                chosen(11, 1, 2, 0), chosen(13, 0, 1, 255), new ModernApplicationProtocol.ChannelEnter(3, 7, 3333, -1, 3),
                new ModernApplicationProtocol.CharacterReturn(4, 7, 3333, -1))) {
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        }
        for (int status = 1; status <= 3; status++) {
            var denied = new ModernApplicationProtocol.NavigationResult(2, 9, status, 0, 255, 7, 3333, -1, null, List.of());
            assertEquals(denied, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(denied)));
        }
    }
    @Test void busyBackRetainsSelectedPhaseAndRejectsOtherFailureShapes() throws Exception {
        for (var message : List.of(chosen(12,3,1,255), chosen(12,3,2,3))) {
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        }
        for (var invalid : List.of(chosen(12,1,1,255),chosen(12,2,2,0),chosen(12,0,1,255),
                new ModernApplicationProtocol.NavigationResult(1,12,3,0,255,7,3333,-1,null,List.of()))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(invalid));
        }
        byte[] golden = HexFormat.of().parseHex(LEFT); golden[16]=12; golden[17]=3;
        assertArrayEquals(golden, ModernApplicationProtocol.encode(ModernApplicationProtocol.decode(golden)));
        assertEquals(3, ((ModernApplicationProtocol.NavigationResult)ModernApplicationProtocol.decode(golden)).status());
    }
    @Test void sharedGoldenLobbyLeaveRetainsSelectedIdentityAndUnsignedEpoch() throws Exception {
        var request = new ModernApplicationProtocol.LobbyLeave(0xfedcba98, 7, 3333, -1);
        assertArrayEquals(HexFormat.of().parseHex(LEAVE), ModernApplicationProtocol.encode(request));
        assertEquals(request, ModernApplicationProtocol.decode(HexFormat.of().parseHex(LEAVE)));
        var rows = java.util.stream.IntStream.range(0, 4).mapToObj(id ->
                new ModernApplicationProtocol.ChannelRow(id, "c" + id, 0, 100, 15, Long.MAX_VALUE)).toList();
        var reply = new ModernApplicationProtocol.NavigationResult(0xfedcba98, 13, 0, 1, 255, 7, 3333, -1,
                new ModernApplicationProtocol.CharacterRow(3333, "N", 3, 1, 4_000_000_000L), rows);
        assertArrayEquals(HexFormat.of().parseHex(LEFT), ModernApplicationProtocol.encode(reply));
        assertEquals(reply, ModernApplicationProtocol.decode(HexFormat.of().parseHex(LEFT)));
        var input = new java.io.ByteArrayInputStream(HexFormat.of().parseHex(LEAVE + LEAVE));
        assertArrayEquals(HexFormat.of().parseHex(LEAVE), ModernApplicationProtocol.readFrame(input));
        assertEquals(36, input.available(), "new kind retains one-frame bounded framing");
    }
    @Test void lobbyLeaveRejectsMalformedContextHeadersTruncationAndTrailingData() throws Exception {
        for (var request : List.of(new ModernApplicationProtocol.LobbyLeave(0, 7, 3333, 1),
                new ModernApplicationProtocol.LobbyLeave(1, 0, 3333, 1),
                new ModernApplicationProtocol.LobbyLeave(1, -1, 3333, 1),
                new ModernApplicationProtocol.LobbyLeave(1, 7, 0, 1),
                new ModernApplicationProtocol.LobbyLeave(1, 7, -1, 1),
                new ModernApplicationProtocol.LobbyLeave(1, 7, 3333, 0))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(request));
        }
        byte[] frame = HexFormat.of().parseHex(LEAVE);
        for (int length = 0; length < frame.length; length++) {
            byte[] truncated = Arrays.copyOf(frame, length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(truncated));
        }
        for (int[] range : new int[][]{{8,12},{16,20},{20,28},{28,36}}) {
            byte[] invalid = frame.clone(); Arrays.fill(invalid, range[0], range[1], (byte) 0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        for (int index : new int[]{12,14,15}) {
            byte[] invalid = frame.clone(); invalid[index] = (byte) 0xff;
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        byte[] trailing = Arrays.copyOf(frame, frame.length + 1); ByteBuffer.wrap(trailing).putInt(4, trailing.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(trailing));
    }
    @Test void lobbyLeaveAcknowledgmentCannotDropSelectionOrClaimAnotherPhase() throws Exception {
        for (var result : List.of(chosen(13, 1, 1, 255), chosen(13, 2, 1, 255), chosen(13, 3, 1, 255),
                chosen(13, 0, 2, 0), chosen(13, 0, 1, 0),
                new ModernApplicationProtocol.NavigationResult(1, 13, 0, 0, 255, 7, 3333, 1, null, List.of()),
                new ModernApplicationProtocol.NavigationResult(1, 13, 0, 1, 255, 7, 3333, 1, null, channels()))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(result));
        }
        byte[] frame = HexFormat.of().parseHex(LEFT);
        for (int[] mutation : new int[][]{{17,1},{18,0},{18,2},{19,0},{40,0},{41,3},{42,1}}) {
            byte[] invalid = frame.clone(); invalid[mutation[0]] = (byte) mutation[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        byte[] mismatch = frame.clone(); ByteBuffer.wrap(mismatch).putLong(44, 3334);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(mismatch));
    }
    @Test void navigationShapeCountsReservedBitsAndIdentityAreStrict() throws Exception {
        byte[] original = ModernApplicationProtocol.encode(chosen(9, 0, 1, 255));
        for (int[] mutation : new int[][]{{16,10},{17,4},{18,3},{19,4},{40,2},{41,5},{42,1},{43,1}}) {
            byte[] changed = original.clone(); changed[mutation[0]] = (byte) mutation[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(changed));
        }
        for (int[] range : new int[][]{{20,24},{24,32},{32,40}}) {
            byte[] changed = original.clone(); Arrays.fill(changed, range[0], range[1], (byte) 0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(changed));
        }
        byte[] mismatch = original.clone(); ByteBuffer.wrap(mismatch).putLong(44, 3334);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(mismatch));
        for (int length = 0; length < original.length; length++) {
            byte[] partial = Arrays.copyOf(original, length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(partial));
        }
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(chosen(9, 1, 1, 255)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(chosen(12, 0, 2, 0)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(new ModernApplicationProtocol.ChannelEnter(1, 7, 3333, 1, 4)));
    }
    @Test void channelBoundsRejectOverCapacityWrongOrderAndDuplicateLabels() {
        for (var invalid : List.of(
                new ModernApplicationProtocol.ChannelRow(1, "channel0", 1, 1000, 80, Long.MAX_VALUE),
                new ModernApplicationProtocol.ChannelRow(0, "channel1", 1, 1000, 80, Long.MAX_VALUE),
                new ModernApplicationProtocol.ChannelRow(0, "channel0", 1001, 1000, 80, Long.MAX_VALUE),
                new ModernApplicationProtocol.ChannelRow(0, "channel0", 1, 65536, 80, Long.MAX_VALUE),
                new ModernApplicationProtocol.ChannelRow(0, "channel0", 1, 1000, 0, Long.MAX_VALUE),
                new ModernApplicationProtocol.ChannelRow(0, "channel0", 1, 1000, 80, 79))) {
            var rows = new java.util.ArrayList<>(channels()); rows.set(0, invalid);
            var message = chosen(9, 0, 1, 255);
            var bad = new ModernApplicationProtocol.NavigationResult(message.requestId(), 9, 0, 1, 255, 7, 3333, -1, message.selected(), rows);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(bad));
        }
    }
    @Test void navigationIsExplicitlyNegotiatedWithoutBreakingAuthOnlyClients() throws Exception {
        var modern = new ModernSessionBoundary(ModernSessionBoundary.Policy.current());
        var negotiated = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(modern.acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 7, 7, 1))));
        assertEquals(7, negotiated.capabilities());
        var oldClient = new ModernSessionBoundary(ModernSessionBoundary.Policy.current());
        assertEquals(new ModernSessionProtocol.Ack(2, 1, 1, 3), ModernSessionProtocol.decode(oldClient.acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(2, 1, 1, 3, 3, 1)))));
        var authenticationOnly = new ModernSessionBoundary(new ModernSessionBoundary.Policy(3, 3));
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> authenticationOnly.acceptHello(
                ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 7, 7, 1))));
    }
}
