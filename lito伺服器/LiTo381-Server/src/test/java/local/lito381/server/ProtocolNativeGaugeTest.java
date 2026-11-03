package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Locks the exact 0..29 gauge domains rendered by the selected LiTo.bin. */
final class ProtocolNativeGaugeTest {
    @Test
    void retainedFood225ProjectsAndSaturatesWithoutWrapping() {
        assertEquals(0, Protocol.nativeFoodGauge(-1));
        assertEquals(0, Protocol.nativeFoodGauge(0));
        assertEquals(15, Protocol.nativeFoodGauge(113));
        assertEquals(24, Protocol.nativeFoodGauge(190));
        assertEquals(29, Protocol.nativeFoodGauge(225));
        assertEquals(29, Protocol.nativeFoodGauge(999));
    }

    @Test
    void retainedWeight240ProjectsAndSaturatesWithoutWrapping() {
        assertEquals(0, Protocol.nativeWeightGauge(-1));
        assertEquals(0, Protocol.nativeWeightGauge(0));
        assertEquals(15, Protocol.nativeWeightGauge(120));
        assertEquals(23, Protocol.nativeWeightGauge(190));
        assertEquals(29, Protocol.nativeWeightGauge(240));
        assertEquals(29, Protocol.nativeWeightGauge(999));
    }
}
