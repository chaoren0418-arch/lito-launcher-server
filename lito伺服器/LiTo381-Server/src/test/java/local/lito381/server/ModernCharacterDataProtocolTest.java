package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class ModernCharacterDataProtocolTest {
    static final String REQUEST_HEX = "4c544d3100000024fedcba98000e0000000000070000000000000d05ffffffffffffffff";
    static final String DENIED_HEX = "4c544d3100000028fedcba98000f0000000000070000000000000d05ffffffffffffffff01000000";
    static final String DATA_HEX = "4c544d3100000133fedcba98000f0000000000070000000000000d05ffffffffffffffff000000000000000000000d05"
            + "00014e0003010000000000ee6b28000000000000000001000000000000000200000000000000030000000000000004ff"
            + "fffffffffffffb0000000000000006000000000000000700000000000000080000000000000009000000000000000a00"
            + "0000000000000b000000000000000cfffffffffffffff3000000000000000e000000000000000f000000000000001000"
            + "000000000000110000000000000012000000000000001300010000000000000000006300000000000000647fffffffff"
            + "ffffff8000000000000000000000000000000500000000000000ff000000000000000600000000000000070000000000"
            + "00000800000000000000090005000000022431";

    static ModernCharacterData.PersistedValues persisted() {
        return new ModernCharacterData.PersistedValues(1,2,3,4,-5,6,7,8,9,10,11,12,-13,14,15,16,17,18,19);
    }
    static ModernCharacterData.Snapshot snapshot(int sourceId, long objectId) {
        var template = new ModernCharacterData.ItemTemplate(100, ModernCharacterData.ItemKind.WEAPON, "$1", 7, 8, 9);
        var item = new ModernCharacterData.InventoryItem(99, template, Long.MAX_VALUE, true, Long.MIN_VALUE, 5, false, 255, true, 6);
        return new ModernCharacterData.Snapshot(sourceId,
                new ModernApplicationProtocol.CharacterRow(objectId, "N", 3, 1, 4_000_000_000L), persisted(), List.of(item));
    }
    private static ModernApplicationProtocol.CharacterDataResult result(ModernCharacterData.Snapshot snapshot) {
        return new ModernApplicationProtocol.CharacterDataResult(0xfedcba98, 7, 3333, -1, 0, snapshot);
    }

    @Test void sharedGoldenRequestDeniedAndCompleteDataPreserveSignedAndUnsignedFields() throws Exception {
        var request = new ModernApplicationProtocol.CharacterDataRequest(0xfedcba98, 7, 3333, -1);
        var denied = new ModernApplicationProtocol.CharacterDataResult(0xfedcba98, 7, 3333, -1, 1, null);
        var data = result(snapshot(7, 3333));
        for (var pair : List.of(new Object[] {REQUEST_HEX, request}, new Object[] {DENIED_HEX, denied}, new Object[] {DATA_HEX, data})) {
            byte[] golden = HexFormat.of().parseHex((String) pair[0]);
            assertArrayEquals(golden, ModernApplicationProtocol.encode((ModernApplicationProtocol.Message) pair[1]));
            assertEquals(pair[1], ModernApplicationProtocol.decode(golden));
        }
        assertEquals(307, HexFormat.of().parseHex(DATA_HEX).length);
        assertEquals(-1L, ((ModernApplicationProtocol.CharacterDataResult) ModernApplicationProtocol.decode(
                HexFormat.of().parseHex(DATA_HEX))).selectionEpoch());
        assertThrows(ModernSessionProtocol.ProtocolException.class,
                () -> ModernSessionProtocol.decode(ModernApplicationProtocol.encode(request)));
        for (int status : new int[] {1,2,3}) {
            var failed = new ModernApplicationProtocol.CharacterDataResult(1, 7, 3333, Long.MIN_VALUE, status, null);
            assertEquals(failed, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(failed)));
        }
    }

    @Test void full512ItemLargeFrameRoundTripsButDoesNotWidenOtherKindsOrHello() throws Exception {
        var template = new ModernCharacterData.ItemTemplate(Long.MAX_VALUE, ModernCharacterData.ItemKind.ETC,
                "n".repeat(256), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        var items = LongStream.rangeClosed(1, 512).mapToObj(id -> new ModernCharacterData.InventoryItem(id, template,
                Long.MAX_VALUE, false, Long.MIN_VALUE, Long.MAX_VALUE, true, 255, true, Long.MAX_VALUE)).toList();
        var wide = new ModernCharacterData.PersistedValues(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 225);
        var snapshot = new ModernCharacterData.Snapshot(Integer.MAX_VALUE,
                new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE, "界".repeat(85), 65535, 1, Long.MAX_VALUE), wide, items);
        var data = new ModernApplicationProtocol.CharacterDataResult(-1, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, 0, snapshot);
        byte[] frame = ModernApplicationProtocol.encode(data);
        assertTrue(frame.length > ModernApplicationProtocol.MAX_FRAME_BYTES);
        assertTrue(frame.length < ModernCharacterDataProtocol.MAX_FRAME_BYTES);
        assertEquals(frame.length, ByteBuffer.wrap(frame).getInt(4));
        assertEquals(data, ModernApplicationProtocol.decode(frame));
        assertArrayEquals(frame, ModernApplicationProtocol.readFrame(new ByteArrayInputStream(frame)));
        for (int kind : new int[] {3,4,5,6,7,8,9,10,11,12,13,14}) {
            byte[] wrongKind = frame.clone(); ByteBuffer.wrap(wrongKind).putShort(12, (short) kind);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(wrongKind));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(Arrays.copyOf(wrongKind, 16))));
        }
        assertEquals(40, ModernSessionProtocol.MAX_FRAME_BYTES); // Separate extension HELLO, not character data.
        assertEquals(32, ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, 3, 3, 1)).length);
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.decode(frame));
        var handshakeInput = new ByteArrayInputStream(frame);
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.readFrame(handshakeInput));
        assertEquals(frame.length - 16, handshakeInput.available());
    }

    @Test void zeroInventoryStillContainsACompletePersistedCharacterAndConsumesOneFrame() throws Exception {
        var source = snapshot(7, 3333);
        var empty = new ModernCharacterData.Snapshot(7, source.character(), source.persisted(), List.of());
        var data = result(empty); byte[] frame = ModernApplicationProtocol.encode(data);
        assertEquals(219, frame.length); assertEquals(data, ModernApplicationProtocol.decode(frame));
        byte[] both = new byte[frame.length * 2];
        System.arraycopy(frame, 0, both, 0, frame.length); System.arraycopy(frame, 0, both, frame.length, frame.length);
        var input = new ByteArrayInputStream(both);
        assertArrayEquals(frame, ModernApplicationProtocol.readFrame(input)); assertEquals(frame.length, input.available());
        assertArrayEquals(frame, ModernApplicationProtocol.readFrame(input)); assertNull(ModernApplicationProtocol.readFrame(input));
    }

    @Test void malformedStatusPresenceAndSnapshotIdentityAreRejectedOnEncodeAndDecode() throws Exception {
        for (var message : List.of(new ModernApplicationProtocol.CharacterDataResult(1, 7, 3333, 1, -1, null),
                new ModernApplicationProtocol.CharacterDataResult(1, 7, 3333, 1, 4, null),
                new ModernApplicationProtocol.CharacterDataResult(1, 7, 3333, 1, 0, null),
                new ModernApplicationProtocol.CharacterDataResult(1, 7, 3333, 1, 1, snapshot(7, 3333)),
                result(snapshot(42, 3333)), result(snapshot(7, 3334)))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message));
        }
        for (int status : new int[] {1,2,3,4,255}) {
            byte[] frame = HexFormat.of().parseHex(DATA_HEX); frame[36] = (byte) status;
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(frame));
        }
        byte[] noData = HexFormat.of().parseHex(DENIED_HEX); noData[36] = 0;
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(noData));
        byte[] wrongObject = HexFormat.of().parseHex(DATA_HEX); ByteBuffer.wrap(wrongObject).putLong(40, 3334);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(wrongObject));
    }

    @Test void requestAndResponseRejectUnknownKindsZeroContextReservedTruncationAndTrailingData() throws Exception {
        for (String hex : List.of(REQUEST_HEX, DATA_HEX)) {
            byte[] original = HexFormat.of().parseHex(hex);
            for (int length = 0; length < original.length; length++) {
                byte[] truncated = Arrays.copyOf(original, length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(truncated));
            }
            for (int[] range : new int[][] {{8,12},{16,20},{20,28},{28,36}}) {
                byte[] invalid = original.clone(); Arrays.fill(invalid, range[0], range[1], (byte) 0);
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
            }
            for (int kind : new int[] {0,1,2,16,65535}) {
                byte[] invalid = original.clone(); ByteBuffer.wrap(invalid).putShort(12, (short) kind);
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
            }
            for (int offset : new int[] {14,15}) {
                byte[] invalid = original.clone(); invalid[offset] = 1;
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
            }
            byte[] trailing = Arrays.copyOf(original, original.length + 1); ByteBuffer.wrap(trailing).putInt(4, trailing.length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(trailing));
        }
        byte[] header = Arrays.copyOf(HexFormat.of().parseHex(DATA_HEX), 16);
        for (int length : new int[] {-1,15,ModernCharacterDataProtocol.MAX_FRAME_BYTES + 1}) {
            byte[] invalid = header.clone(); ByteBuffer.wrap(invalid).putInt(4, length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(invalid)));
        }
        assertThrows(EOFException.class, () -> ModernApplicationProtocol.readFrame(
                new ByteArrayInputStream(Arrays.copyOf(HexFormat.of().parseHex(DATA_HEX), 306))));
    }

    @Test void inventoryCountFlagsKindTextAndAllReservedFieldsFailClosed() {
        byte[] original = HexFormat.of().parseHex(DATA_HEX);
        for (int offset : new int[] {37,38,39,54,217,218,301,302}) {
            byte[] invalid = original.clone(); invalid[offset] = 1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        for (int count : new int[] {0,2,513,65535}) {
            byte[] invalid = original.clone(); ByteBuffer.wrap(invalid).putShort(215, (short) count);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        for (int[] change : new int[][] {{299,3},{299,255},{300,8},{300,255},{305,0},{305,255},{50,0},{50,255}}) {
            byte[] invalid = original.clone(); invalid[change[0]] = (byte) change[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        for (int offset : new int[] {48,303}) {
            byte[] invalid = original.clone(); ByteBuffer.wrap(invalid).putShort(offset, (short) 257);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
        for (long[] change : new long[][] {{63,0},{207,226},{219,0},{227,0},{235,0},{251,-1},{259,256},{267,-1},{275,-1}}) {
            byte[] invalid = original.clone(); ByteBuffer.wrap(invalid).putLong((int) change[0], change[1]);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(invalid));
        }
    }
}
