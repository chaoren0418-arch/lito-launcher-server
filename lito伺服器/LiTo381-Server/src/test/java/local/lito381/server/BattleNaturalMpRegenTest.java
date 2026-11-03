package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BattleNaturalMpRegenTest {
    @Test void wizardAndOtherClassesUseTheNativeFortyAndSixtyFourCounters() {
        assertEquals(40, BattleNaturalMpRegen.threshold(3));
        assertEquals(64, BattleNaturalMpRegen.threshold(0));
        assertEquals(64, BattleNaturalMpRegen.threshold(6));
    }

    @Test void stationaryMovementAndAttackStatesDriveTheOneSecondCounter() {
        BattleNaturalMpRegen regen = new BattleNaturalMpRegen();
        long start = 1_000_000_000L;
        regen.reset(start);

        assertEquals(0, regen.poll(start + 9_000_000_000L, true, 3));
        assertEquals(1, regen.poll(start + 10_000_000_000L, true, 3));

        regen.noteMove();
        assertEquals(0, regen.poll(start + 11_000_000_000L, true, 3));
        regen.noteAttack();
        assertEquals(0, regen.poll(start + 12_000_000_000L, true, 3));
        assertEquals(0, regen.poll(start + 20_000_000_000L, true, 3));
        assertEquals(1, regen.poll(start + 22_000_000_000L, true, 3));
    }

    @Test void inactiveSecondsDoNotAccumulateOrClearActivity() {
        BattleNaturalMpRegen regen = new BattleNaturalMpRegen();
        long start = 2_000_000_000L;
        regen.reset(start);
        regen.noteAttack();
        assertEquals(0, regen.poll(start + 20_000_000_000L, false, 3));
        assertEquals(0, regen.poll(start + 29_000_000_000L, true, 3));
        assertEquals(0, regen.poll(start + 30_000_000_000L, true, 3));
        assertEquals(1, regen.poll(start + 31_000_000_000L, true, 3));
    }

    @Test void reproducesWisdomBaseAndEveryOriginalClassBranch() {
        assertEquals(1, BattleNaturalMpRegen.wisdomBase(14));
        assertEquals(2, BattleNaturalMpRegen.wisdomBase(15));
        assertEquals(3, BattleNaturalMpRegen.wisdomBase(17));
        assertEquals(2, BattleNaturalMpRegen.originalMpRegen(0, 15));
        assertEquals(2, BattleNaturalMpRegen.originalMpRegen(1, 13));
        assertEquals(2, BattleNaturalMpRegen.originalMpRegen(2, 18));
        assertEquals(3, BattleNaturalMpRegen.originalMpRegen(3, 18));
        assertEquals(1, BattleNaturalMpRegen.originalMpRegen(4, 13));
        assertEquals(2, BattleNaturalMpRegen.originalMpRegen(5, 17));
        assertEquals(2, BattleNaturalMpRegen.originalMpRegen(6, 17));
    }

    @Test void amountAddsOnlyQualifiedBaseOriginalAndEquipmentMpr() {
        BattleNaturalMpRegen.Profile profile = new BattleNaturalMpRegen.Profile(
                3, 17, 18, 4);
        assertEquals(10, BattleNaturalMpRegen.amount(profile));
    }

    @Test void bluePotionAddsTheExactWisdomDerivedBonus() {
        assertEquals(1, BattleNaturalMpRegen.bluePotionBonus(10));
        assertEquals(1, BattleNaturalMpRegen.bluePotionBonus(11));
        assertEquals(7, BattleNaturalMpRegen.bluePotionBonus(17));
        BattleNaturalMpRegen.Profile profile = new BattleNaturalMpRegen.Profile(
                3, 17, 18, 4);
        assertEquals(17, BattleNaturalMpRegen.amount(profile, true));
        assertEquals(10, BattleNaturalMpRegen.amount(profile, false));
    }

    @Test void meditationAndConcentrationAddExactIndependentModifiers() {
        BattleNaturalMpRegen.Profile profile = new BattleNaturalMpRegen.Profile(
                1, 11, 11, 0);
        assertEquals(2, BattleNaturalMpRegen.amount(profile, false, false, false));
        assertEquals(7, BattleNaturalMpRegen.amount(profile, false, true, false));
        assertEquals(4, BattleNaturalMpRegen.amount(profile, false, false, true));
        assertEquals(9, BattleNaturalMpRegen.amount(profile, false, true, true));
        assertEquals(10, BattleNaturalMpRegen.amount(profile, true, true, true));
        assertEquals(12, BattleNaturalMpRegen.amount(
                profile, true, true, true, 2));
    }
}
