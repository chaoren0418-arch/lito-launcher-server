package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BattleAnimationCatalogTest {
    @Test
    void packaged380ResourceContainsTheCompleteImportedTable() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        assertEquals(30_812, catalog.entryCount());
        assertTrue(catalog.spriteCount() > 6_000);
        assertEquals(BattleAnimationCatalog.DEFAULT_RESOURCE, catalog.source());
    }

    @Test
    void humanoid61UsesWeaponWalkAndAttackPairs() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        assertEquals(0, catalog.requireWalkAction(61, BattleActionCodes.WALK));
        assertEquals(1, catalog.requireAttackAction(61, BattleActionCodes.WALK));
        assertEquals(4, catalog.requireWalkAction(61, BattleActionCodes.SWORD_WALK));
        assertEquals(5, catalog.requireAttackAction(61, BattleActionCodes.SWORD_WALK));
        assertEquals(640, catalog.requireTimedAction(61, 0).durationMillis());
        assertEquals(800, catalog.requireTimedAction(61, 1).durationMillis());
    }

    @Test
    void idleThreeCannotBeUsedAsThePutObjectWeaponStance() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        assertFalse(catalog.supportsTimedAction(61, BattleActionCodes.IDLE));
        assertThrows(IllegalArgumentException.class,
                () -> catalog.requireWalkAction(61, BattleActionCodes.IDLE));
    }

    @Test
    void nativeCastleStewardsRequireOnlyTheirPassiveWalkAnimation() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        for (int gfx : new int[] {460, 782, 971, 1766}) {
            assertEquals(BattleActionCodes.WALK,
                    catalog.requireWalkAction(gfx, BattleActionCodes.WALK));
        }
        assertFalse(catalog.supportsTimedAction(460, BattleActionCodes.ATTACK));
    }

    @Test
    void nativeDragonKnightChainswordFallsBackToItsSpearFamily() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        for (int gfx : new int[] {6658, 6661}) {
            assertFalse(catalog.supportsTimedAction(
                    gfx, BattleActionCodes.CHAINSWORD_WALK));
            int walk = catalog.resolveEquippedWalkAction(
                    gfx, BattleActionCodes.CHAINSWORD_WALK);
            assertEquals(BattleActionCodes.SPEAR_WALK, walk);
            assertEquals(BattleActionCodes.SPEAR_ATTACK,
                    catalog.requireAttackAction(gfx, walk));
        }
    }

    @Test
    void awakenAntharasDragonSupportsWalkAttackAndCast() {
        BattleAnimationCatalog catalog = BattleAnimationCatalog.get();
        assertEquals(BattleActionCodes.SPEAR_WALK,
                catalog.resolveEquippedWalkAction(
                        9_362, BattleActionCodes.CHAINSWORD_WALK));
        assertEquals(BattleActionCodes.SPEAR_ATTACK,
                catalog.requireAttackAction(9_362, BattleActionCodes.SPEAR_WALK));
        assertTrue(catalog.supportsTimedAction(9_362, BattleActionCodes.SPELL_CAST));
    }
}
