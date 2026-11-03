package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleFireWeaponStatusTest {
    @TempDir Path temporary;

    @Test void refreshReplacesDeadlineWithoutStackingMeleeDamage() {
        BattleFireWeaponStatus status = new BattleFireWeaponStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, 4));
        assertEquals(new BattleFireWeaponStatus.Snapshot(true, 4, 960),
                status.snapshot(start));
        assertEquals(4, status.meleeDamage(start));
        assertEquals(4, status.physicalDamage(false, start));
        assertEquals(0, status.physicalDamage(true, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, 4));
        assertEquals(4, status.meleeDamage(refresh));
        assertFalse(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleFireWeaponStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleFireWeaponStatus status = new BattleFireWeaponStatus();
        status.activate(0, 960, 4);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 960, 4);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("fire-weapon.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedFireWeaponBuff("FireElf",
                new GameStateStore.TimedFireWeaponBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedFireWeaponBuff("FireElf").remainingSeconds());

        state = new GameStateStore(file, true);
        state.saveTimedFireWeaponBuff("FireElf",
                GameStateStore.TimedFireWeaponBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedFireWeaponBuff("FireElf").active());
    }

    @Test void invalidNativeLeaseAndDurableInputsFailClosed() {
        BattleFireWeaponStatus status = new BattleFireWeaponStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, 4));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 4));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 961, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedFireWeaponBuff(961));
    }
}
