package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ClassicLineageClockTest {
    @Test
    void retainedClockAcceleratesRealTimeSixfoldAndAlignsToThreeSeconds() {
        long base = ClassicLineageClock.REAL_BASE_EPOCH_MILLIS;
        assertEquals(0, ClassicLineageClock.atEpochMillis(base));
        assertEquals(3, ClassicLineageClock.atEpochMillis(base + 500));
        assertEquals(6, ClassicLineageClock.atEpochMillis(base + 1_000));
        assertEquals(6, ClassicLineageClock.atEpochMillis(base + 1_499));
        assertEquals(9, ClassicLineageClock.atEpochMillis(base + 1_500));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicLineageClock.atEpochMillis(base - 1));
    }

    @Test
    void nativeLocaltimeViewUsesTheSelectedTaipeiClientZone() {
        assertEquals(8 * 3_600, ClassicLineageClock.nativeClientSecondOfDay(0));
        assertEquals(9 * 3_600,
                ClassicLineageClock.nativeClientSecondOfDay(3_600));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicLineageClock.nativeClientSecondOfDay(-1));
    }
}
