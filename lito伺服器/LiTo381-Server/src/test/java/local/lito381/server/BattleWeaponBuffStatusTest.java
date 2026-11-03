package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleWeaponBuffStatusTest {
    @Test void holyWeaponAppliesExactMeleeOnlyModifiers() {
        BattleWeaponBuffStatus status = new BattleWeaponBuffStatus();
        long start = TimeUnit.SECONDS.toNanos(5);
        assertEquals(0, status.activate(start, 1_200, 8, 1, 1, 0, 0));
        BattleWeaponBuffStatus.Modifier active = status.modifier(start);
        assertEquals(8, active.skillId());
        assertEquals(1, active.hit(false));
        assertEquals(1, active.damage(false));
        assertEquals(0, active.hit(true));
        assertEquals(0, active.damage(true));
    }

    @Test void blessWeaponAppliesExactMeleeAndRangedModifiersAndExpires() {
        BattleWeaponBuffStatus status = new BattleWeaponBuffStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        assertEquals(0, status.activate(start, 1_200, 48, 2, 2, 2, 0));

        BattleWeaponBuffStatus.Modifier active = status.modifier(start);
        assertEquals(48, active.skillId());
        assertEquals(2, active.hit(false));
        assertEquals(2, active.damage(false));
        assertEquals(2, active.hit(true));
        assertEquals(0, active.damage(true));
        assertEquals(1_200, active.remainingSeconds());
        assertTrue(active.active());

        assertEquals(0, status.expire(start + TimeUnit.SECONDS.toNanos(1_200) - 1)
                .skillId());
        assertEquals(48, status.expire(start + TimeUnit.SECONDS.toNanos(1_200))
                .skillId());
        assertFalse(status.modifier(start).active());
    }

    @Test void classicWeaponGroupReplacesAndCancellationClearsAuthoritativeLease() {
        BattleWeaponBuffStatus status = new BattleWeaponBuffStatus();
        assertEquals(0, status.activate(0, 100, 8, 1, 1, 0, 0));
        assertEquals(8, status.activate(1, 100, 48, 2, 2, 2, 0));
        assertEquals(48, status.modifier(1).skillId());
        assertEquals(new BattleWeaponBuffStatus.Cancellation(48),
                status.cancelMagicEffects());
        assertFalse(status.modifier(1).active());
        assertFalse(status.cancelMagicEffects().present());
    }

    @Test void invalidWeaponBuffLeasesAreRejected() {
        BattleWeaponBuffStatus status = new BattleWeaponBuffStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 1, 48, 2, 2, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 48, 2, 2, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1, 47, 2, 2, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1, 48, 0, 0, 0, 0));
    }

    @Test void namedRemovalDoesNotClearAnotherWeaponBuff() {
        BattleWeaponBuffStatus status = new BattleWeaponBuffStatus();
        status.activate(0, 1_200, BattleSpellCatalog.HOLY_WEAPON,
                1, 1, 0, 0);
        assertFalse(status.removeSkill(BattleSpellCatalog.BLESSED_ARMOR));
        assertEquals(BattleSpellCatalog.HOLY_WEAPON,
                status.modifier(0).skillId());
        assertTrue(status.removeSkill(BattleSpellCatalog.HOLY_WEAPON));
        assertFalse(status.modifier(0).active());
    }
}
