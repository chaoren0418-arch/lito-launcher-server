package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleItemWeaponEnchantStatusTest {
    @Test void shadowFangBindsDamageToTheSelectedWeaponAndExpires() {
        BattleItemWeaponEnchantStatus status = new BattleItemWeaponEnchantStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        assertEquals(0, status.activate(start, 192, BattleSpellCatalog.SHADOW_FANG,
                101, "$100", 5, 5));

        BattleItemWeaponEnchantStatus.Modifier active = status.modifier(start, 101);
        assertTrue(active.active());
        assertEquals(BattleSpellCatalog.SHADOW_FANG, active.skillId());
        assertEquals(5, active.damage(false));
        assertEquals(5, active.damage(true));
        assertEquals(192, active.remainingSeconds());
        assertFalse(status.modifier(start, 102).active());

        assertEquals(List.of(), status.expire(
                start + TimeUnit.SECONDS.toNanos(192) - 1));
        assertEquals(List.of(new BattleItemWeaponEnchantStatus.Expiration(
                        BattleSpellCatalog.SHADOW_FANG, 101, "$100")),
                status.expire(start + TimeUnit.SECONDS.toNanos(192)));
        assertFalse(status.modifier(start, 101).active());
    }

    @Test void separateWeaponsKeepIndependentLeasesAndSameObjectRecastRefreshes() {
        BattleItemWeaponEnchantStatus status = new BattleItemWeaponEnchantStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        assertEquals(0, status.activate(start, 192, BattleSpellCatalog.SHADOW_FANG,
                101, "first", 5, 5));
        assertEquals(0, status.activate(start + 1, 192,
                BattleSpellCatalog.SHADOW_FANG, 102, "second", 5, 5));
        assertEquals(2, status.activeLeaseCount(start + 2));

        long recast = start + TimeUnit.SECONDS.toNanos(100);
        assertEquals(BattleSpellCatalog.SHADOW_FANG,
                status.activate(recast, 192, BattleSpellCatalog.SHADOW_FANG,
                        101, "first", 5, 5));
        assertEquals(192, status.modifier(recast, 101).remainingSeconds());
        assertEquals(93, status.modifier(recast, 102).remainingSeconds());
        // Cancellation has no entry point by design: the retained immunity
        // table preserves every item-local Shadow Fang lease.
        assertEquals(2, status.activeLeaseCount(recast));
    }

    @Test void enchantWeaponReplacesSameObjectShadowFangAndCanBeRemovedAlone() {
        BattleItemWeaponEnchantStatus status = new BattleItemWeaponEnchantStatus();
        long start = TimeUnit.SECONDS.toNanos(30);
        assertEquals(0, status.activate(start, 192,
                BattleSpellCatalog.SHADOW_FANG, 101, "first", 5, 5));
        assertEquals(BattleSpellCatalog.SHADOW_FANG,
                status.activate(start + 1, 1_800,
                        BattleSpellCatalog.ENCHANT_WEAPON,
                        101, "first", 2, 2));
        BattleItemWeaponEnchantStatus.Modifier modifier =
                status.modifier(start + 2, 101);
        assertEquals(BattleSpellCatalog.ENCHANT_WEAPON, modifier.skillId());
        assertEquals(2, modifier.damage(false));
        assertEquals(2, modifier.damage(true));
        assertEquals(BattleSpellCatalog.ENCHANT_WEAPON, status.remove(101));
        assertFalse(status.modifier(start + 3, 101).active());
        assertEquals(0, status.remove(101));
    }

    @Test void invalidItemWeaponEnchantLeasesAreRejectedAndClearDropsAll() {
        BattleItemWeaponEnchantStatus status = new BattleItemWeaponEnchantStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 192, BattleSpellCatalog.SHADOW_FANG,
                        1, "weapon", 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, BattleSpellCatalog.SHADOW_FANG,
                        1, "weapon", 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 192, 106, 1, "weapon", 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 192, BattleSpellCatalog.SHADOW_FANG,
                        0, "weapon", 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 192, BattleSpellCatalog.SHADOW_FANG,
                        1, "", 5, 5));

        status.activate(0, 192, BattleSpellCatalog.SHADOW_FANG,
                1, "weapon", 5, 5);
        status.clear();
        assertEquals(0, status.activeLeaseCount(0));
    }

    @Test void giranNpcEnchantWeaponUsesTheSameItemLocalLeaseModel() {
        BattleItemWeaponEnchantStatus status = new BattleItemWeaponEnchantStatus();
        assertEquals(0, status.activate(0, 1800,
                ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_SKILL_ID,
                501, "weapon", 2, 2));
        BattleItemWeaponEnchantStatus.Modifier modifier = status.modifier(0, 501);
        assertTrue(modifier.active());
        assertEquals(2, modifier.damage(false));
        assertEquals(2, modifier.damage(true));
        assertEquals(1800, modifier.remainingSeconds());
    }
}
