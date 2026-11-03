package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleWeightStatusTest {
    @Test
    void independentNativeLeasesExpireAndExposeOnlyTheProvenRegenBypasses() {
        BattleWeightStatus status = new BattleWeightStatus();
        long now = TimeUnit.SECONDS.toNanos(10);

        status.activate(BattleSpellCatalog.DECREASE_WEIGHT, now, 1_800);
        status.activate(BattleSpellCatalog.EXOTIC_VITALIZE, now, 960);
        BattleWeightStatus.Snapshot active = status.snapshot(now);
        assertEquals(1_800, active.decreaseWeightSeconds());
        assertEquals(960, active.exoticVitalizeSeconds());
        assertEquals(0, active.additionalFireSeconds());
        assertTrue(active.decreaseWeightActive());
        assertTrue(active.regenerationBypassActive());

        long exoticExpired = now + TimeUnit.SECONDS.toNanos(961);
        assertTrue(status.expire(exoticExpired));
        BattleWeightStatus.Snapshot decreasedOnly = status.snapshot(exoticExpired);
        assertTrue(decreasedOnly.decreaseWeightActive());
        assertFalse(decreasedOnly.regenerationBypassActive());

        status.activate(BattleSpellCatalog.ADDITIONAL_FIRE, exoticExpired, 960);
        assertTrue(status.snapshot(exoticExpired).regenerationBypassActive());
        status.clear();
        BattleWeightStatus.Snapshot cleared = status.snapshot(exoticExpired);
        assertFalse(cleared.decreaseWeightActive());
        assertFalse(cleared.regenerationBypassActive());
    }

    @Test
    void magicCancellationClearsAllWeightSpellLeasesWithoutTouchingCooldowns() {
        BattleWeightStatus status = new BattleWeightStatus();
        status.activate(BattleSpellCatalog.DECREASE_WEIGHT, 0, 1_800);
        status.activate(BattleSpellCatalog.EXOTIC_VITALIZE, 0, 960);
        status.activate(BattleSpellCatalog.ADDITIONAL_FIRE, 0, 960);
        assertTrue(status.cancelMagicEffects());
        assertEquals(new BattleWeightStatus.Snapshot(0, 0, 0), status.snapshot(1));
        assertFalse(status.cancelMagicEffects());
    }
}
