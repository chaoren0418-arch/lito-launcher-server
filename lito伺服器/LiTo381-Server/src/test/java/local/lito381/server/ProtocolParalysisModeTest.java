package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProtocolParalysisModeTest {
    @Test
    void namesEverySameVersionProducerModeExactlyOnce() {
        List<Integer> values = Arrays.stream(Protocol.ParalysisMode.values())
                .map(Protocol.ParalysisMode::wireValue)
                .toList();

        assertEquals(List.of(2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 22, 23, 24, 25),
                values);
        assertEquals(values.size(), values.stream().distinct().count());
    }

    @Test
    void writesEveryTypedModeAsExactNativeTwoBytePacket() {
        for (Protocol.ParalysisMode mode : Protocol.ParalysisMode.values()) {
            assertArrayEquals(
                    new byte[] {(byte) Protocol.S_PARALYSE, (byte) mode.wireValue()},
                    Protocol.paralysis(mode), mode.name());
        }
    }

    @Test
    void receiverOnlyValuesHaveNoIntegerWriterEscapeHatch() {
        assertFalse(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("paralysis"))
                .map(Method::getParameterTypes)
                .anyMatch(types -> types.length == 1 && types[0] == int.class));
        assertThrows(NullPointerException.class, () -> Protocol.paralysis(null));
    }
}
