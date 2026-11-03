package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleBurningWeaponStatusTest {
    @TempDir Path temporary;

    @Test void refreshReplacesDeadlineAndOnlyMeleeConsumesBothModifiers() {
        BattleBurningWeaponStatus status = new BattleBurningWeaponStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, 3, 6));
        assertEquals(new BattleBurningWeaponStatus.Snapshot(true, 3, 6, 960),
                status.snapshot(start));
        assertEquals(3, status.physicalHit(false, start));
        assertEquals(6, status.physicalDamage(false, start));
        assertEquals(0, status.physicalHit(true, start));
        assertEquals(0, status.physicalDamage(true, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, 3, 6));
        assertFalse(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleBurningWeaponStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndDurableRemainingTimeRoundTrip() throws Exception {
        BattleBurningWeaponStatus status = new BattleBurningWeaponStatus();
        status.activate(0, 960, 3, 6);
        assertTrue(status.cancel());
        assertFalse(status.cancel());

        Path file = temporary.resolve("burning-weapon.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedBurningWeaponBuff("BurningElf",
                new GameStateStore.TimedBurningWeaponBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedBurningWeaponBuff("BurningElf").remainingSeconds());
        state.saveTimedBurningWeaponBuff("BurningElf",
                GameStateStore.TimedBurningWeaponBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedBurningWeaponBuff("BurningElf").active());
    }

    @Test void invalidNativeLeaseAndDurableInputsFailClosed() {
        BattleBurningWeaponStatus status = new BattleBurningWeaponStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, 3, 6));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 3, 6));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0, 6));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 3, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedBurningWeaponBuff(961));
    }
}
