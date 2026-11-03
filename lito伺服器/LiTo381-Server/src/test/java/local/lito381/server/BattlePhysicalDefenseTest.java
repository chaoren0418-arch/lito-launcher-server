package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePhysicalDefenseTest {
    @Test void validatesAndPreservesTheSourceNpcSize() {
        SourceNpcPhysicalStats small = SourceNpcPhysicalStats.fromSourceRow(
                45262, -4, "small", 2, false);
        SourceNpcPhysicalStats large = SourceNpcPhysicalStats.fromSourceRow(
                45289, -11, "LARGE", 3, true);

        assertFalse(small.large());
        assertTrue(large.large());
        assertThrows(IllegalArgumentException.class,
                () -> SourceNpcPhysicalStats.fromSourceRow(
                        1, 10, "medium", 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceNpcPhysicalStats(1, 10, false, -1, false));
    }

    @Test void usesTheRetainedNpcAcDivisorAndInclusiveReductionBand() {
        SourceNpcPhysicalStats target = new SourceNpcPhysicalStats(
                45262, -4, false, 2, false);
        assertEquals(2, BattlePhysicalDefense.npcAcMaximum(target));

        BattlePhysicalDefense.NpcOutcome low =
                BattlePhysicalDefense.againstNpc(30, target, 0, false, false);
        BattlePhysicalDefense.NpcOutcome high =
                BattlePhysicalDefense.againstNpc(30, target, 1, false, false);
        assertEquals(4, low.totalReduction());
        assertEquals(26, low.damage());
        assertEquals(5, high.totalReduction());
        assertEquals(25, high.damage());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDefense.againstNpc(
                        30, target, 2, false, false));
    }

    @Test void appliesFixedReductionThenTheHardSkinRangedHalf() {
        SourceNpcPhysicalStats hard = new SourceNpcPhysicalStats(
                45610, 10, true, 1, true);

        BattlePhysicalDefense.NpcOutcome ranged =
                BattlePhysicalDefense.againstNpc(21, hard, 0, true, false);
        assertEquals(1, ranged.totalReduction());
        assertEquals(10, ranged.damage());
        assertTrue(ranged.hardRangedHalved());
        assertEquals(20, BattlePhysicalDefense.againstNpc(
                21, hard, 0, true, true).damage());
        assertEquals(20, BattlePhysicalDefense.againstNpc(
                21, hard, 0, false, false).damage());
        assertEquals(0, BattlePhysicalDefense.againstNpc(
                1, hard, 0, true, false).damage());
    }

    @Test void preservesEveryOriginal381PlayerClassAcDivisor() {
        int armorClass = -50; // max(0, 10 - -50) == 60
        assertEquals(20, BattlePhysicalDefense.playerAcMaximum(0, armorClass));
        assertEquals(30, BattlePhysicalDefense.playerAcMaximum(1, armorClass));
        assertEquals(20, BattlePhysicalDefense.playerAcMaximum(2, armorClass));
        assertEquals(12, BattlePhysicalDefense.playerAcMaximum(3, armorClass));
        assertEquals(15, BattlePhysicalDefense.playerAcMaximum(4, armorClass));
        assertEquals(20, BattlePhysicalDefense.playerAcMaximum(5, armorClass));
        assertEquals(15, BattlePhysicalDefense.playerAcMaximum(6, armorClass));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDefense.playerAcMaximum(7, armorClass));
    }

    @Test void reconstructsTheOriginal381BaseAndEquipmentArmorClass() {
        assertEquals(3, BattlePhysicalDefense.playerBaseArmorClass(54, 12));
        assertEquals(2, BattlePhysicalDefense.playerBaseArmorClass(51, 13));
        assertEquals(3, BattlePhysicalDefense.playerBaseArmorClass(60, 7));
        assertEquals(-46, BattlePhysicalDefense.playerArmorClass(54, 12, -49));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDefense.playerBaseArmorClass(-1, 12));
    }

    @Test void appliesNativeAcThenRetainedIgnoreAndDirectionalReduction() {
        BattlePhysicalDefense.PlayerOutcome outcome =
                BattlePhysicalDefense.againstPlayer(
                        40, 1, -10, 4,
                        15, 2, 5, 3);
        assertEquals(10, outcome.acMaximum());
        assertEquals(5, outcome.acReduction());
        assertEquals(13, outcome.armorReduction());
        assertEquals(8, outcome.effectiveArmorReduction());
        assertEquals(3, outcome.directionalReduction());
        assertEquals(24, outcome.damage());
    }

    @Test void validatesPlayerDefenseRollBandsAndNeverReturnsNegativeDamage() {
        assertEquals(11, BattlePhysicalDefense.armorReduction(15, 0));
        assertEquals(15, BattlePhysicalDefense.armorReduction(15, 4));
        assertEquals(8, BattlePhysicalDefense.armorReduction(8, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDefense.armorReduction(15, 5));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDefense.againstPlayer(
                        20, 5, -20, 10, 0, 0, 0, 0));
        assertEquals(0, BattlePhysicalDefense.againstPlayer(
                3, 6, -10, 0, 20, 9, 0, 50).damage());
    }
}
