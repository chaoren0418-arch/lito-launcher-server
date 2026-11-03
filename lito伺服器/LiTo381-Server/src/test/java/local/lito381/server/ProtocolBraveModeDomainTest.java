package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolBraveModeDomainTest {
    @Test
    void namesEveryAuthenticatedProducerModeExactlyOnce() {
        List<Integer> values = Arrays.stream(Protocol.BraveMode.values())
                .map(Protocol.BraveMode::wireValue)
                .toList();

        assertEquals(List.of(0, 1, 3, 4, 5, 6), values);
        assertEquals(values.size(), values.stream().distinct().count());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.BraveMode.fromWire(2));
    }

    @Test
    void writesEveryTypedModeAsExactNativeDchPacket() {
        for (Protocol.BraveMode mode : Protocol.BraveMode.values()) {
            assertArrayEquals(new byte[] {
                    (byte) Protocol.S_EMOTION, 1, 0, 0, 0,
                    (byte) mode.wireValue(), 0, 0
            }, Protocol.braveSpeed(1, mode, 0), mode.name());
        }
    }

    @Test
    void rawIntegerWriterAndNullModeAreRejected() {
        assertFalse(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("braveSpeed"))
                .map(Method::getParameterTypes)
                .anyMatch(types -> types.length == 3 && types[1] == int.class));
        assertThrows(NullPointerException.class,
                () -> Protocol.braveSpeed(1, null, 0));
    }
}
