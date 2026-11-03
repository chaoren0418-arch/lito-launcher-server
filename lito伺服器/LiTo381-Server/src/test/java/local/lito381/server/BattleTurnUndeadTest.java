package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BattleTurnUndeadTest {
    @Test void requiresBothSourceIsTuAndUndeadTypeOneOrThree() {
        assertTrue(BattleTurnUndead.eligible(stats(1, true, 0)));
        assertTrue(BattleTurnUndead.eligible(stats(3, true, 0)));
        assertFalse(BattleTurnUndead.eligible(stats(0, true, 0)));
        assertFalse(BattleTurnUndead.eligible(stats(2, true, 0)));
        assertFalse(BattleTurnUndead.eligible(stats(1, false, 0)));
        assertFalse(BattleTurnUndead.eligible(null));
    }

    @Test void reproducesClassicMagicBonusBoundaries() {
        assertEquals(-2, BattleTurnUndead.magicBonus(5));
        assertEquals(-1, BattleTurnUndead.magicBonus(8));
        assertEquals(0, BattleTurnUndead.magicBonus(11));
        assertEquals(1, BattleTurnUndead.magicBonus(14));
        assertEquals(2, BattleTurnUndead.magicBonus(17));
        assertEquals(3, BattleTurnUndead.magicBonus(18));
        assertEquals(9, BattleTurnUndead.magicBonus(24));
        assertEquals(10, BattleTurnUndead.magicBonus(27));
        assertEquals(15, BattleTurnUndead.magicBonus(40));
    }

    @Test void reproducesAllClassicClassMagicLevels() {
        assertEquals(2, BattleTurnUndead.magicLevel(0, 50));
        assertEquals(10, BattleTurnUndead.magicLevel(1, 50));
        assertEquals(6, BattleTurnUndead.magicLevel(2, 50));
        assertEquals(1, BattleTurnUndead.magicLevel(3, 50));
        assertEquals(2, BattleTurnUndead.magicLevel(4, 50));
        assertEquals(4, BattleTurnUndead.magicLevel(5, 50));
        assertEquals(6, BattleTurnUndead.magicLevel(6, 50));
    }

    @Test void wizardUsesPlusOneDiceAndCapsAtNinety() {
        int probability = BattleTurnUndead.probability(
                1, 50, 18, 2, 3, 0, 7, -1, sides -> sides);
        assertEquals(90, probability);
        assertTrue(BattleTurnUndead.succeeds(probability, 90));
        assertFalse(BattleTurnUndead.succeeds(probability, 91));
    }

    @Test void nonWizardUsesMinusOneDiceAndRetainsOnePercentFloor() {
        int probability = BattleTurnUndead.probability(
                0, 1, 8, 0, 0, 150, 7, -1, sides -> 1);
        assertEquals(1, probability);
        assertTrue(BattleTurnUndead.succeeds(probability, 1));
        assertFalse(BattleTurnUndead.succeeds(probability, 2));
    }

    @Test void retainedEquipmentMagicHitIsSignedAndIndependentFromOriginalHit() {
        assertEquals(3, BattleTurnUndead.probability(
                0, 10, 11, 1, -3, 0, 7, -1, sides -> 4));
    }

    @Test void appliesBossModifierAfterNinetyPercentCap() {
        int probability = BattleTurnUndead.probability(
                1, 50, 18, 2, 3, 0, 7, 3, sides -> sides);
        assertEquals(30, probability);
        assertEquals(0, BattleTurnUndead.probability(
                1, 50, 18, 2, 3, 0, 7, 0, sides -> sides));
    }

    @Test void rejectsInvalidRollsAndTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleTurnUndead.magicLevel(7, 50));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTurnUndead.probability(
                        1, 50, 18, 0, 0, 0, 7, -1, sides -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTurnUndead.succeeds(91, 1));
    }

    private static SourceNpcPhysicalStats stats(
            int undead, boolean isTu, int magicResistance) {
        return new SourceNpcPhysicalStats(
                1, 0, false, 0, false, undead, 0, magicResistance, isTu);
    }
}
