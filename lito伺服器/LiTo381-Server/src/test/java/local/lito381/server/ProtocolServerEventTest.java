package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Locks every S_EVENT subtype currently produced by the server to a named domain. */
final class ProtocolServerEventTest {
    @Test
    void namesEveryImplementedProductionSubtypeExactlyOnce() {
        assertEquals(List.of(0, 1, 2, 3, 4, 6, 9, 33),
                Arrays.stream(Protocol.ServerEventType.values())
                        .map(Protocol.ServerEventType::wireValue)
                        .toList());
    }

    @Test
    void writesEveryImplementedBodyShapeWithItsTypedSubtype() {
        assertArrayEquals(bytes(0x7B, 0, 5, 0, 0, 0),
                Protocol.castleWarEvent(
                        Protocol.ServerEventType.CASTLE_WAR_BEGIN, 5));
        assertArrayEquals(bytes(0x7B, 1, 5, 0, 0, 0),
                Protocol.castleWarEvent(
                        Protocol.ServerEventType.CASTLE_WAR_END, 5));
        assertArrayEquals(bytes(0x7B, 2, 5, 0, 0, 0),
                Protocol.castleWarEvent(
                        Protocol.ServerEventType.CASTLE_WAR_GOING, 5));
        assertArrayEquals(bytes(0x7B, 3),
                Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.CASTLE_WAR_INITIATIVE));
        assertArrayEquals(bytes(0x7B, 4),
                Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.CASTLE_WAR_OCCUPIED));
        assertArrayEquals(bytes(0x7B, 6, 7), Protocol.smsResult(7));
        assertArrayEquals(bytes(0x7B, 9), Protocol.marriedEvent());
        assertArrayEquals(bytes(
                        0x7B, 33,
                        4, 3, 2, 1,
                        0x40, 0xD8, 0x4E, 0,
                        5, 0, 0, 0),
                Protocol.addressChallenge(0x0102_0304L, 0x004E_D840L, 5));
    }

    @Test
    void rejectsCrossFamilyTypedSubtypesAndRetainsNoIntegerOverload() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarEvent(
                        Protocol.ServerEventType.CLIENT_INTEGRITY_CHALLENGE, 5));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarOwnershipEvent(
                        Protocol.ServerEventType.MARRIED));

        Method phase = Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("castleWarEvent"))
                .findFirst().orElseThrow();
        assertArrayEquals(new Class<?>[] {Protocol.ServerEventType.class, int.class},
                phase.getParameterTypes());
        Method ownership = Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("castleWarOwnershipEvent"))
                .findFirst().orElseThrow();
        assertArrayEquals(new Class<?>[] {Protocol.ServerEventType.class},
                ownership.getParameterTypes());
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
