package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernApplicationProtocolTest {
    @Test void goldenAuthAndPingUseIndependentApplicationFraming() throws Exception {
        try (var auth = new ModernApplicationProtocol.Auth(2, "alice", "secret".toCharArray())) {
            byte[] encoded = ModernApplicationProtocol.encode(auth);
            assertEquals("4c544d310000001f00000002000300000005616c6963650006736563726574", HexFormat.of().formatHex(encoded));
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.decode(encoded));
            assertFalse(auth.toString().contains("secret"));
            Arrays.fill(encoded, (byte) 0);
        }
        assertEquals("4c544d3100000010ffffffff00070000", HexFormat.of().formatHex(
                ModernApplicationProtocol.encode(new ModernApplicationProtocol.Ping(-1))));
    }

    @Test void allMessagesRoundTripWithWideStatsAndOpaqueUnsignedEpoch() throws Exception {
        List<ModernApplicationProtocol.Message> messages = List.of(
                new ModernApplicationProtocol.AuthResult(3, 0, "玩家", List.of(new ModernApplicationProtocol.Source(7, "太陽神"))),
                new ModernApplicationProtocol.AuthResult(4, 1, "", List.of()),
                new ModernApplicationProtocol.AuthResult(5, 2, "", List.of()),
                new ModernApplicationProtocol.CharacterRequest(6, 7, -1L),
                new ModernApplicationProtocol.CharacterResult(7, 7, -1L, 0,
                        List.of(new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE, "角色", 65535, 1, Long.MAX_VALUE))),
                new ModernApplicationProtocol.CharacterResult(8, 7, 1, 1, List.of()),
                new ModernApplicationProtocol.CharacterResult(9, 7, 2, 2, List.of()),
                new ModernApplicationProtocol.Ping(10), new ModernApplicationProtocol.Pong(11));
        for (var message : messages) assertEquals(message,
                ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        try (var original = new ModernApplicationProtocol.Auth(2, "玩家", "  ".toCharArray())) {
            byte[] bytes = ModernApplicationProtocol.encode(original);
            var decoded = (ModernApplicationProtocol.Auth) ModernApplicationProtocol.decode(bytes);
            assertEquals("玩家", decoded.account());
            assertArrayEquals(original.password(), decoded.password());
            decoded.close();
            assertArrayEquals(new char[2], decoded.password());
            Arrays.fill(bytes, (byte) 0);
        }
    }

    @Test void lengthsAreUtf8BytesNotCharactersAndStrictlyDecoded() throws Exception {
        for (var auth : List.of(new ModernApplicationProtocol.Auth(2, "玩".repeat(22), new char[]{'a'}),
                new ModernApplicationProtocol.Auth(2, "alice", "密".repeat(86).toCharArray()),
                new ModernApplicationProtocol.Auth(2, "alice", new char[]{'\ud800'}),
                new ModernApplicationProtocol.Auth(2, "alice", new char[]{'a', '\0'}))) {
            try (auth) { assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(auth)); }
        }
        try (var auth = new ModernApplicationProtocol.Auth(2, "a", new char[]{'b'})) {
            byte[] frame = ModernApplicationProtocol.encode(auth);
            frame[18] = (byte) 0xff;
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(frame));
            Arrays.fill(frame, (byte) 0);
        }
    }

    @Test void malformedHeadersLengthsUnknownKindsTrailingDataAndReservedAreRejected() throws Exception {
        byte[] valid = ModernApplicationProtocol.encode(new ModernApplicationProtocol.Ping(2));
        for (int offset : new int[]{0, 4, 8, 12, 14}) {
            byte[] bad = valid.clone(); bad[offset] = (byte) 0xff;
            if (offset == 8) Arrays.fill(bad, 8, 12, (byte) 0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bad));
        }
        byte[] trailing = Arrays.copyOf(valid, 17); ByteBuffer.wrap(trailing).putInt(4, 17);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(trailing));
        byte[] oversized = valid.clone(); ByteBuffer.wrap(oversized).putInt(4, -1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(oversized)));
        assertThrows(EOFException.class,
                () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(Arrays.copyOf(valid, 15))));
        assertNull(ModernApplicationProtocol.readFrame(new ByteArrayInputStream(new byte[0])));
    }

    @Test void invalidStatusesCountsSourcesDuplicatesAndOrderNeverBecomePartialSuccess() throws Exception {
        var source = new ModernApplicationProtocol.Source(7, "Sun");
        for (var result : List.of(
                new ModernApplicationProtocol.AuthResult(2, 3, "", List.of()),
                new ModernApplicationProtocol.AuthResult(2, 0, "alice", List.of()),
                new ModernApplicationProtocol.AuthResult(2, 1, "alice", List.of()),
                new ModernApplicationProtocol.AuthResult(2, 1, "", List.of(source)),
                new ModernApplicationProtocol.AuthResult(2, 0, "alice", List.of(source, source)),
                new ModernApplicationProtocol.AuthResult(2, 0, "alice", List.of(source, new ModernApplicationProtocol.Source(6, "Moon"))),
                new ModernApplicationProtocol.AuthResult(2, 0, "alice", List.of(source, new ModernApplicationProtocol.Source(8, "Sun"))),
                new ModernApplicationProtocol.AuthResult(2, 0, "alice", List.of(new ModernApplicationProtocol.Source(-1, "Sun"))))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(result));
        }
        byte[] malformedCount = ModernApplicationProtocol.encode(new ModernApplicationProtocol.AuthResult(2, 1, "", List.of()));
        ByteBuffer.wrap(malformedCount).putShort(18, (short) 65);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(malformedCount));
    }

    @Test void invalidCharacterSemanticsAndRowReservedAreRejected() throws Exception {
        var valid = new ModernApplicationProtocol.CharacterRow(3333, "Knight", 1, 0, 80);
        for (var row : List.of(new ModernApplicationProtocol.CharacterRow(0, "N", 1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(-1, "N", 1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", -1, 0, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", 1, 2, 1),
                new ModernApplicationProtocol.CharacterRow(1, "N", 1, 0, 0))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(
                    new ModernApplicationProtocol.CharacterResult(2, 7, 1, 0, List.of(row))));
        }
        for (var response : List.of(new ModernApplicationProtocol.CharacterResult(2, 7, 0, 0, List.of()),
                new ModernApplicationProtocol.CharacterResult(2, 7, 1, 1, List.of(valid)),
                new ModernApplicationProtocol.CharacterResult(2, 7, 1, 0, List.of(valid, valid)))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(response));
        }
        byte[] reserved = ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterResult(2, 7, 1, 0, List.of(valid)));
        reserved[reserved.length - 9] = 1;
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(reserved));
    }

    @Test void frameReaderConsumesExactlyOneFrame() throws Exception {
        byte[] ping = ModernApplicationProtocol.encode(new ModernApplicationProtocol.Ping(2));
        byte[] both = new byte[ping.length * 2];
        System.arraycopy(ping, 0, both, 0, ping.length); System.arraycopy(ping, 0, both, ping.length, ping.length);
        var input = new ByteArrayInputStream(both);
        assertArrayEquals(ping, ModernApplicationProtocol.readFrame(input));
        assertEquals(ping.length, input.available());
        assertArrayEquals(ping, ModernApplicationProtocol.readFrame(input));
    }

    @Test void equipmentDispatchCoexistsWithOldCharacterDataAndDoesNotWidenOrdinaryKinds() throws Exception {
        var request = new ModernApplicationProtocol.EquipmentRequest(22, ModernEquipmentProtocolTest.read());
        var result = new ModernApplicationProtocol.EquipmentResult(22, request.request(),
                new ModernEquipmentAuthority.Result(ModernEquipmentAuthority.OK, false, 0, 0, ModernEquipmentProtocolTest.snapshot()));
        for (var message : List.of(request, result, new ModernApplicationProtocol.CharacterDataRequest(23, 7, 3333, -1),
                new ModernApplicationProtocol.CharacterDataResult(23, 7, 3333, -1, 0, ModernCharacterDataProtocolTest.snapshot(7, 3333)),
                new ModernApplicationProtocol.Ping(24))) {
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        }
        assertArrayEquals(HexFormat.of().parseHex(ModernCharacterDataProtocolTest.DATA_HEX), ModernApplicationProtocol.encode(
                new ModernApplicationProtocol.CharacterDataResult(0xfedcba98, 7, 3333, -1, 0, ModernCharacterDataProtocolTest.snapshot(7, 3333))));
        for (var invalid : List.of(new ModernApplicationProtocol.EquipmentResult(0, request.request(), result.result()),
                new ModernApplicationProtocol.EquipmentResult(1, null, result.result()),
                new ModernApplicationProtocol.EquipmentResult(1, request.request(), null))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(invalid));
        }
        byte[] header = ModernApplicationProtocol.encode(new ModernApplicationProtocol.Pong(25));
        ByteBuffer.wrap(header).putInt(4, ModernApplicationProtocol.MAX_FRAME_BYTES + 1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header)));
    }
}
