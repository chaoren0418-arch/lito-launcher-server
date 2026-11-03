package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleRangedEvasionTest {
    @Test void replaysClassLevelDexterityOriginalEquipmentAndBuffLayers() {
        // Knight level 40: 10; DEX 18: 5; original DEX 16: 3;
        // equipment: 4; Earth/Life/Birth/Image Eye ER: 10.
        assertEquals(32, BattleRangedEvasion.total(1, 40, 18, 16, 4, 10));
        assertEquals(3, BattleRangedEvasion.original(0, 18));
        assertEquals(0, BattleRangedEvasion.original(2, 18));
        assertEquals(2, BattleRangedEvasion.original(6, 14));
    }

    @Test void bowSecondRollHitsOnlyWhenErIsStrictlyBelowOneToOneHundred() {
        assertFalse(BattleRangedEvasion.hitsAfterEvasion(10, 10));
        assertTrue(BattleRangedEvasion.hitsAfterEvasion(10, 11));
        assertFalse(BattleRangedEvasion.hitsAfterEvasion(100, 100));
        assertTrue(BattleRangedEvasion.hitsAfterEvasion(-1, 1));
    }

    @Test void darkElfDressEvasionMovesTheSameFixedRollFromHitToMiss() {
        int base = BattleRangedEvasion.total(4, 52, 10, 10, 0, 0);
        int dressed = BattleRangedEvasion.total(4, 52, 10, 10, 0, 12);
        assertEquals(9, base);
        assertEquals(21, dressed);
        assertTrue(BattleRangedEvasion.hitsAfterEvasion(base, 15));
        assertFalse(BattleRangedEvasion.hitsAfterEvasion(dressed, 15));
    }
}
