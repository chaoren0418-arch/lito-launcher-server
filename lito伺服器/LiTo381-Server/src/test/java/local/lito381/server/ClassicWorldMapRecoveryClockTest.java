package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class ClassicWorldMapRecoveryClockTest {
    @Test
    void firesAtNativeIntervalAndRestartsOnMapOrRuleChange() {
        ClassicWorldMapRecoveryClock clock = new ClassicWorldMapRecoveryClock();
        ClassicWorldMapRecoveryCatalog.Rule undersea =
                ClassicWorldMapRecoveryCatalog.ruleFor(63, 0, 0);
        long start = TimeUnit.SECONDS.toNanos(1);
        assertFalse(clock.poll(63, undersea, start));
        assertFalse(clock.poll(63, undersea,
                start + TimeUnit.SECONDS.toNanos(63)));
        assertTrue(clock.poll(63, undersea,
                start + TimeUnit.SECONDS.toNanos(64)));
        assertFalse(clock.poll(63, undersea,
                start + TimeUnit.SECONDS.toNanos(127)));
        assertTrue(clock.poll(63, undersea,
                start + TimeUnit.SECONDS.toNanos(128)));

        ClassicWorldMapRecoveryCatalog.Rule another =
                ClassicWorldMapRecoveryCatalog.ruleFor(65, 0, 0);
        assertFalse(clock.poll(65, another,
                start + TimeUnit.SECONDS.toNanos(129)));
        assertFalse(clock.poll(65, null,
                start + TimeUnit.SECONDS.toNanos(193)));
        assertFalse(clock.poll(65, another,
                start + TimeUnit.SECONDS.toNanos(194)));
    }

    @Test
    void rejectsMismatchedRulesAndInvalidClockInputs() {
        ClassicWorldMapRecoveryClock clock = new ClassicWorldMapRecoveryClock();
        ClassicWorldMapRecoveryCatalog.Rule undersea =
                ClassicWorldMapRecoveryCatalog.ruleFor(63, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> clock.poll(65, undersea, 1));
        assertThrows(IllegalArgumentException.class,
                () -> clock.poll(-1, undersea, 1));
        assertThrows(IllegalArgumentException.class,
                () -> clock.poll(63, undersea, -1));
    }
}
