package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolDrunkenModeDomainTest {
    @Test
    void namesEveryRetainedProducerModeExactlyOnce() {
        List<Integer> values = Arrays.stream(Protocol.DrunkenMode.values())
                .map(Protocol.DrunkenMode::wireValue)
                .toList();

        assertEquals(List.of(0, 1, 2, 8), values);
        assertEquals(values.size(), values.stream().distinct().count());
    }

    @Test
    void writesEveryTypedModeAsExactNativeDcPacket() {
        for (Protocol.DrunkenMode mode : Protocol.DrunkenMode.values()) {
            assertArrayEquals(new byte[] {
                    (byte) Protocol.S_DRUNKEN, 1, 0, 0, 0,
                    (byte) mode.wireValue()
            }, Protocol.drunken(1, mode), mode.name());
        }
    }

    @Test
    void rawIntegerWriterAndNullModeAreRejected() {
        assertFalse(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("drunken"))
                .map(Method::getParameterTypes)
                .anyMatch(types -> types.length == 2 && types[1] == int.class));
        assertThrows(NullPointerException.class,
                () -> Protocol.drunken(1, null));
    }
}
