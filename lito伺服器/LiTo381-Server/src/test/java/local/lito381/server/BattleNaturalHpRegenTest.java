package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class BattleNaturalHpRegenTest {
    @Test void reproducesLevelAndKnightCounterThresholds() {
        assertEquals(120, BattleNaturalHpRegen.threshold(1, 3));
        assertEquals(12, BattleNaturalHpRegen.threshold(50, 3));
        assertEquals(8, BattleNaturalHpRegen.threshold(30, 1));
    }

    @Test void stationaryMovementAndAttackStatesDriveTheOneSecondCounter() {
        BattleNaturalHpRegen regen = new BattleNaturalHpRegen();
        long start = 1_000_000_000L;
        regen.reset(start);

        assertEquals(0, regen.poll(start + 1_000_000_000L, true, 50, 3));
        assertEquals(0, regen.poll(start + 2_000_000_000L, true, 50, 3));
        assertEquals(1, regen.poll(start + 3_000_000_000L, true, 50, 3));

        regen.noteMove();
        assertEquals(0, regen.poll(start + 4_000_000_000L, true, 50, 3));
        regen.noteAttack();
        assertEquals(0, regen.poll(start + 5_000_000_000L, true, 50, 3));
        assertEquals(0, regen.poll(start + 6_000_000_000L, true, 50, 3));
        assertEquals(0, regen.poll(start + 7_000_000_000L, true, 50, 3));
        assertEquals(1, regen.poll(start + 8_000_000_000L, true, 50, 3));
    }

    @Test void inactiveSecondsDoNotAccumulateOrClearActivity() {
        BattleNaturalHpRegen regen = new BattleNaturalHpRegen();
        long start = 2_000_000_000L;
        regen.reset(start);
        regen.noteAttack();
        assertEquals(0, regen.poll(start + 20_000_000_000L, false, 50, 3));
        assertEquals(0, regen.poll(start + 21_000_000_000L, true, 50, 3));
        assertEquals(0, regen.poll(start + 22_000_000_000L, true, 50, 3));
        assertEquals(0, regen.poll(start + 23_000_000_000L, true, 50, 3));
        assertEquals(1, regen.poll(start + 24_000_000_000L, true, 50, 3));
    }

    @Test void lifeStreamAddsThreeToTheSameNaturalRegenEvent() {
        BattleNaturalHpRegen.Profile profile = new BattleNaturalHpRegen.Profile(
                50, 3, 18, 18, 40, 3, 2);
        assertEquals(5, BattleNaturalHpRegen.amount(profile, false, 1));
        assertEquals(8, BattleNaturalHpRegen.amount(profile, true, 1));
        assertEquals(10, BattleNaturalHpRegen.amount(profile, true, 3));
        assertEquals(12, BattleNaturalHpRegen.amount(profile, true, 3, 2));
        assertEquals(27, BattleNaturalHpRegen.amount(profile, true, 3, 2,
                BattleNaturesTouchStatus.NATIVE_HPR_BONUS));
    }

    @Test void reproducesOriginalConHprBranches() {
        assertEquals(4, BattleNaturalHpRegen.originalHpRegen(0, 18));
        assertEquals(4, BattleNaturalHpRegen.originalHpRegen(1, 18));
        assertEquals(3, BattleNaturalHpRegen.originalHpRegen(2, 17));
        assertEquals(2, BattleNaturalHpRegen.originalHpRegen(3, 18));
        assertEquals(2, BattleNaturalHpRegen.originalHpRegen(4, 13));
        assertEquals(3, BattleNaturalHpRegen.originalHpRegen(5, 18));
        assertEquals(2, BattleNaturalHpRegen.originalHpRegen(6, 16));
    }
}
