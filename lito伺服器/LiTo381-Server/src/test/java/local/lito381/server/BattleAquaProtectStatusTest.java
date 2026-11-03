package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleAquaProtectStatusTest {
    @Test void refreshesWithoutStackingAndExpiresAt960Seconds() {
        BattleAquaProtectStatus status = new BattleAquaProtectStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, 5));
        assertEquals(new BattleAquaProtectStatus.Snapshot(true, 5, 960),
                status.snapshot(start));
        assertEquals(5, status.evasionRate(start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, 5));
        assertEquals(5, status.evasionRate(refresh));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleAquaProtectStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationIsIdempotentAndInputsFailClosed() {
        BattleAquaProtectStatus status = new BattleAquaProtectStatus();
        status.activate(0, 960, 5);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }

    @Test void plusFiveErChangesOnlyTheRangedSecondRollAndCoexists() {
        int baseEr = 20;
        int solidCarriage = 15;
        int aquaProtect = 5;
        assertTrue(BattleRangedEvasion.hitsAfterEvasion(
                baseEr + solidCarriage, 36));
        assertFalse(BattleRangedEvasion.hitsAfterEvasion(
                baseEr + solidCarriage + aquaProtect, 36));
        assertEquals(40, baseEr + solidCarriage + aquaProtect);
    }
}
