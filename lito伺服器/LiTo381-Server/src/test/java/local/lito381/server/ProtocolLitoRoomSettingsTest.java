package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolLitoRoomSettingsTest {
    private static byte[] request(int opcode, String name, int attributes,
                                  String password, int capacity, int... tail) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(opcode);
        out.writeBytes(name.getBytes(Protocol.LEGACY_TEXT));
        out.write(0);
        out.write(attributes);
        out.writeBytes(password.getBytes(Protocol.LEGACY_TEXT));
        out.write(0);
        out.write(capacity);
        out.write(capacity >>> 8);
        for (int value : tail) out.write(value);
        return out.toByteArray();
    }

    @Test void parsesCompactCreateAndEditAtTheNativeCapacityBoundary() {
        for (int opcode : new int[] {Protocol.C_LITO_CREATE_ROOM, Protocol.C_LITO_EDIT_ROOM}) {
            Protocol.LitoRoomSettingsRequest parsed = Protocol.litoRoomSettingsRequest(
                    request(opcode, "Arena", GameHub.RoomSettings.ALLOW_OBSERVERS,
                            "", 200), opcode);
            assertEquals("Arena", parsed.name());
            assertEquals(200, parsed.capacity());
            assertEquals(Protocol.LitoRoomSettingsLayout.COMPACT, parsed.layout());
            assertFalse(parsed.hasOptions());
            assertEquals(List.of(), parsed.selections());
        }
    }

    @Test void parsesLegacyZeroTailAndExactExtendedBlob() {
        Protocol.LitoRoomSettingsRequest legacy = Protocol.litoRoomSettingsRequest(
                request(Protocol.C_LITO_CREATE_ROOM, "Legacy", 0, "", 2,
                        0, 0, 0, 0, 0),
                Protocol.C_LITO_CREATE_ROOM);
        assertTrue(legacy.hasOptions());
        assertEquals(List.of(), legacy.selections());

        Protocol.LitoRoomSettingsRequest extended = Protocol.litoRoomSettingsRequest(
                request(Protocol.C_LITO_EDIT_ROOM, "Extended", 0x0C, "1234", 200,
                        7, 2, 127, 31, 3, 1, 128, 255),
                Protocol.C_LITO_EDIT_ROOM);
        assertEquals(Protocol.LitoRoomSettingsLayout.OPTIONS, extended.layout());
        assertEquals(7, extended.option1());
        assertEquals(2, extended.option2());
        assertEquals(127, extended.option3());
        assertEquals(31, extended.option4());
        assertEquals(List.of(1, 128, 255), extended.selections());
        assertEquals(7, extended.eventFlags());
        assertEquals(2, extended.eventType());
        assertEquals(127, extended.winnerCount());
        assertEquals(31, extended.allowedClassMask());
        assertEquals(List.of(1, 128, 255), extended.allowedServerIds());
    }

    @Test void rejectsPartialMismatchedAndZeroSelectionTails() {
        byte[] compact = request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", 4);
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(append(compact, 1),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", 4,
                                1, 1, 1, 1, 1),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", 4,
                                1, 1, 1, 1, 1, 0),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", 4,
                                1, 1, 1, 1, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                        Protocol.C_LITO_CREATE_ROOM));
    }

    @Test void rejectsValuesOutsideExactProducerDomains() {
        for (int[] tail : new int[][] {
                {8, 1, 1, 1, 0},
                {1, 3, 1, 1, 0},
                {1, 1, 0, 1, 0},
                {1, 1, 128, 1, 0},
                {1, 1, 1, 32, 0},
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.litoRoomSettingsRequest(
                            request(Protocol.C_LITO_EDIT_ROOM, "Arena", 0, "", 4, tail),
                            Protocol.C_LITO_EDIT_ROOM));
        }
    }

    @Test void rejectsInvalidStringsAttributesCapacitiesAndOpcode() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "", 0, "", 4),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "A".repeat(90), 0, "", 4),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 1, "", 4),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 4, "", 4),
                        Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "12345", 4),
                        Protocol.C_LITO_CREATE_ROOM));
        for (int capacity : new int[] {1, 201}) {
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.litoRoomSettingsRequest(
                            request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", capacity),
                            Protocol.C_LITO_CREATE_ROOM));
        }
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        new byte[] {(byte) Protocol.C_LITO_CREATE_ROOM, (byte) 0x81, 0,
                                0, 0, 4, 0}, Protocol.C_LITO_CREATE_ROOM));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSettingsRequest(
                        request(Protocol.C_LITO_CREATE_ROOM, "Arena", 0, "", 4),
                        Protocol.C_LITO_EDIT_ROOM));
    }

    private static byte[] append(byte[] source, int value) {
        byte[] result = java.util.Arrays.copyOf(source, source.length + 1);
        result[source.length] = (byte) value;
        return result;
    }
}
