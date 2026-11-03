package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleAwakenStatusTest {
    @Test void activationIsIndefiniteDrainsEveryFourSecondsAndSameCastTogglesOff() {
        BattleAwakenStatus status = new BattleAwakenStatus();
        long start = 1_000_000_000L;
        BattleAwakenStatus.Toggle activated = status.toggle(
                start, 185, -12, 127, 9_362, 8, 4, 0, 0, 0);
        assertTrue(activated.activated());
        assertFalse(activated.deactivated());
        assertEquals(new BattleAwakenStatus.Snapshot(
                        185, -12, 127, 9_362, 8, 4, 0, 0, 0,
                        start + TimeUnit.SECONDS.toNanos(4)),
                activated.snapshot());
        assertEquals(0, status.pollMpDrainTicks(
                start + TimeUnit.SECONDS.toNanos(3)));
        assertEquals(1, status.pollMpDrainTicks(
                start + TimeUnit.SECONDS.toNanos(4)));
        assertEquals(3, status.pollMpDrainTicks(
                start + TimeUnit.SECONDS.toNanos(16)));

        BattleAwakenStatus.Toggle deactivated = status.toggle(
                start + TimeUnit.SECONDS.toNanos(17),
                185, -12, 127, 9_362, 8, 4, 0, 0, 0);
        assertFalse(deactivated.activated());
        assertTrue(deactivated.deactivated());
        assertEquals(BattleAwakenStatus.Snapshot.none(), status.snapshot());
    }

    @Test void cancellationHasNoEntryPointAndLifecycleClearReturnsPreviousLease() {
        BattleAwakenStatus status = new BattleAwakenStatus();
        status.toggle(0, 185, -12, 127, 9_362, 8, 4, 0, 0, 0);
        assertTrue(status.clear().active());
        assertFalse(status.clear().active());
        assertThrows(IllegalArgumentException.class,
                () -> status.toggle(-1, 185, -12, 127, 9_362, 8, 4, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.toggle(0, 185, -12, 127, 9_362, 0, 4, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.pollMpDrainTicks(-1));
    }

    @Test void fafurionCarriesMrAndAllElementResistanceAndExcludesAntharas() {
        BattleAwakenStatus status = new BattleAwakenStatus();
        BattleAwakenStatus.Toggle activated = status.toggle(
                0, 190, 0, 0, 9_364, 8, 4, 30, 30, 0);
        assertTrue(activated.activated());
        assertEquals(30, activated.snapshot().magicResistanceModifier());
        assertEquals(30, activated.snapshot().allElementalResistance());
        assertThrows(IllegalStateException.class,
                () -> status.toggle(1, 185, -12, 127, 9_362, 8, 4, 0, 0, 0));
        assertEquals(190, status.snapshot().skillId());
    }

    @Test void valakasCarriesAllSixAbilityScoresAndExcludesFafurion() {
        BattleAwakenStatus status = new BattleAwakenStatus();
        BattleAwakenStatus.Toggle activated = status.toggle(
                0, 195, 0, 0, 9_363, 8, 4, 0, 0, 5);
        assertEquals(5, activated.snapshot().allAbilityScoreBonus());
        assertThrows(IllegalStateException.class,
                () -> status.toggle(1, 190, 0, 0, 9_364, 8, 4, 30, 30, 0));
        assertEquals(195, status.snapshot().skillId());
    }
}
