package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleElementalResistanceTest {
    @Test void fireWallUsesSameFamilyPositiveOnlyQuantizedReduction() {
        assertEquals(20, BattleElementalResistance.fireWallDamage(20, -128));
        assertEquals(20, BattleElementalResistance.fireWallDamage(20, 0));
        assertEquals(20, BattleElementalResistance.fireWallDamage(20, 3));
        assertEquals(19, BattleElementalResistance.fireWallDamage(20, 4));
        assertEquals(14, BattleElementalResistance.fireWallDamage(20, 30));
        assertEquals(10, BattleElementalResistance.fireWallDamage(20, 50));
        assertEquals(0, BattleElementalResistance.fireWallDamage(20, 100));
        assertEquals(0, BattleElementalResistance.fireWallDamage(20, 127));
    }

    @Test void fireWallResistanceRejectsValuesOutsideClassicDomains() {
        assertEquals(0, BattleElementalResistance.fireWallDamage(0, 30));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.fireWallDamage(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.fireWallDamage(20, -129));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.fireWallDamage(20, 128));
    }

    @Test void ordinaryMagicUsesSignedResistanceAndCasterIntelligence() {
        // INT 8 produces the retained minimum intelligence term of one.
        assertEquals(21, BattleElementalResistance.magicDamage(20, 8, 0));
        assertEquals(16, BattleElementalResistance.magicDamage(20, 8, 30));
        assertEquals(27, BattleElementalResistance.magicDamage(20, 8, -30));
        // INT 18 produces an intelligence term of six before truncation.
        assertEquals(25, BattleElementalResistance.magicDamage(20, 18, 30));
        assertEquals(0, BattleElementalResistance.magicDamage(20, 8, 127));
        assertEquals(0, BattleElementalResistance.magicDamage(0, 18, -128));
    }

    @Test void ordinaryMagicRejectsValuesOutsideClassicDomains() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.magicDamage(-1, 8, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.magicDamage(20, -1, 0));
        // Equipment SP is added after getInt(); the retained source does not
        // cap that intermediate sum to the character byte domain.
        assertEquals(237, BattleElementalResistance.magicDamage(20, 128, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.magicDamage(20, 8, -129));
        assertThrows(IllegalArgumentException.class,
                () -> BattleElementalResistance.magicDamage(20, 8, 128));
    }

    @Test void schema4FeedsWideResistanceIntoActualDamageCalculations() {
        System.setProperty("lito.numeric.breakthrough", "4");
        try {
            assertEquals(0, BattleElementalResistance.fireWallDamage(20, 300));
            assertEquals(0, BattleElementalResistance.magicDamage(20, 18, 300));
            assertTrue(BattleElementalResistance.magicDamage(20, 18, -300) > 20);
            assertThrows(IllegalArgumentException.class,
                    () -> BattleElementalResistance.magicDamage(
                            20, 18, 1_000_000));
        } finally {
            System.clearProperty("lito.numeric.breakthrough");
        }
    }
}
