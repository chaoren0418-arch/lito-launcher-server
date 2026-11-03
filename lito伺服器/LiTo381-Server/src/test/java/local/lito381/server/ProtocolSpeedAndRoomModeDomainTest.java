package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolSpeedAndRoomModeDomainTest {
    @Test
    void speedModesMatchReceiverAndSameVersionProducerDomain() {
        List<Integer> values = Arrays.stream(Protocol.SpeedMode.values())
                .map(Protocol.SpeedMode::wireValue)
                .toList();

        assertEquals(List.of(0, 1, 2), values);
        assertEquals(values.size(), values.stream().distinct().count());
        for (Protocol.SpeedMode mode : Protocol.SpeedMode.values()) {
            assertArrayEquals(new byte[] {
                    (byte) Protocol.S_SPEED, 1, 0, 0, 0,
                    (byte) mode.wireValue(), 0, 0
            }, Protocol.speed(1, mode, 0), mode.name());
        }
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.SpeedMode.fromWire(3));
        assertThrows(NullPointerException.class,
                () -> Protocol.speed(1, null, 0));
    }

    @Test
    void roomModesMatchNativeCategoryAndRendererDomain() {
        List<Integer> values = Arrays.stream(BattleRules.Mode.values())
                .map(BattleRules.Mode::wireId)
                .toList();

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), values);
        assertEquals(values.size(), values.stream().distinct().count());
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            assertArrayEquals(new byte[] {
                    (byte) Protocol.S_LITO_GAME_INFO, 2, 1, 0, 0, 0,
                    (byte) mode.wireId()
            }, Protocol.litoRoomMode(1, mode), mode.name());
        }
        assertThrows(NullPointerException.class,
                () -> Protocol.litoRoomMode(1, null));
    }

    @Test
    void typedWritersExposeNoIntegerModeEscapeHatch() {
        assertFalse(hasIntegerModeOverload("speed", 1));
        assertFalse(hasIntegerModeOverload("litoRoomMode", 1));
    }

    private static boolean hasIntegerModeOverload(String name, int index) {
        return Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .map(Method::getParameterTypes)
                .anyMatch(types -> types.length > index && types[index] == int.class);
    }
}
