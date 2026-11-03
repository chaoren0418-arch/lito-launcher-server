package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

final class ProtocolLitoObserverStateDomainTest {
    @Test void nativeStateDomainIsExactlyDisabledAndEnabled() {
        assertArrayEquals(new int[] {0, 1},
                Arrays.stream(Protocol.LitoObserverState.values())
                        .mapToInt(Protocol.LitoObserverState::wireValue).toArray());
        assertEquals(Protocol.LitoObserverState.DISABLED,
                Protocol.LitoObserverState.fromWire(0));
        assertEquals(Protocol.LitoObserverState.ENABLED,
                Protocol.LitoObserverState.fromWire(1));
        assertFalse(Protocol.LitoObserverState.DISABLED.enabled());
        assertTrue(Protocol.LitoObserverState.ENABLED.enabled());
        assertEquals(Protocol.LitoObserverState.DISABLED,
                Protocol.LitoObserverState.fromEnabled(false));
        assertEquals(Protocol.LitoObserverState.ENABLED,
                Protocol.LitoObserverState.fromEnabled(true));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoObserverState.fromWire(2));
    }

    @Test void cE1ParserReturnsOnlyTypedStatesAndRejectsOtherEnvelopes() {
        assertEquals(Protocol.LitoObserverState.DISABLED,
                Protocol.litoObserverRequest(bytes(0xE1, 0)));
        assertEquals(Protocol.LitoObserverState.ENABLED,
                Protocol.litoObserverRequest(bytes(0xE1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverRequest(bytes(0xE1, 1, 0)));
    }

    @Test void sE5WriterEmitsEachTypedStateExactly() {
        assertArrayEquals(bytes(0xE5, 0), Protocol.litoObserverResult(
                Protocol.LitoObserverState.DISABLED));
        assertArrayEquals(bytes(0xE5, 1), Protocol.litoObserverResult(
                Protocol.LitoObserverState.ENABLED));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoObserverResult(null));
    }

    @Test void protocolBoundaryExposesNoBooleanOrRawIntegerWriter() throws Exception {
        assertEquals(Protocol.LitoObserverState.class,
                Protocol.class.getDeclaredMethod("litoObserverRequest", byte[].class)
                        .getReturnType());
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod("litoObserverResult", boolean.class));
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod("litoObserverResult", int.class));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
