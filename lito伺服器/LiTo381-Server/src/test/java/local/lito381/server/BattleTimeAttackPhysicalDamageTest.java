package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleTimeAttackPhysicalDamageTest {
    private static final long RETAINED_DAYLIGHT =
            1_735_689_600_000L + 3_600_000L;

    @Test
    void ordinaryFallbackUsesDefaultLeverageAndExactSelectedStrength() {
        BattleTimeAttackPhysicalDamage.Outcome hit = hit(
                45182, 18, 10, 1, 99, 1, 10, 0);
        assertEquals(51, hit.sourceDamage());
        assertEquals(0, hit.acMaximum());
        assertEquals(0, hit.acReduction());
        assertEquals(51, hit.afterAcDamage());
    }

    @Test
    void selectedBossPhysicalRowsApplyLeverageBeforeBossMultiplier() {
        BattleTimeAttackPhysicalDamage.Outcome minotaur = hit(
                45584, 53, 30, 1, 99, 1, 10, 0);
        BattleTimeAttackPhysicalDamage.Outcome ancient = hit(
                45610, 56, 30, 1, 99, 1, 10, 0);
        assertEquals(423, minotaur.sourceDamage());
        assertEquals(126, ancient.sourceDamage());
    }

    @Test
    void playerClassAcReductionKeepsRetainedRandomBand() {
        BattleTimeAttackPhysicalDamage.Outcome hit = hit(
                45182, 18, 10, 1, 99, 1, -10, 3);
        assertEquals(10, hit.acMaximum());
        assertEquals(4, hit.acReduction());
        assertEquals(47, hit.afterAcDamage());
    }

    @Test
    void weaponBreakStacksApplyBeforePlayerAcReduction() {
        BattleTimeAttackPhysicalDamage.Outcome hit =
                BattleTimeAttackPhysicalDamage.againstPlayer(
                        18, SourceNpcPhysicalStats.memoryFixture(45182),
                        SourcePhysicalGrowthStats.classic381(), 10, 2,
                        1, -10, 1, 99, 3, RETAINED_DAYLIGHT);
        assertEquals(45, hit.sourceDamage());
        assertEquals(4, hit.acReduction());
        assertEquals(41, hit.afterAcDamage());
    }

    @Test
    void invalidPerActorFixtureFailsInsteadOfClampingSourceRolls() {
        assertThrows(IllegalArgumentException.class, () -> hit(
                45182, 18, 10, 18, 99, 1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> hit(
                45182, 18, 10, 1, 100, 1, 10, 0));
    }

    private static BattleTimeAttackPhysicalDamage.Outcome hit(
            int npcId, int level, int leverage,
            int levelRoll, int criticalRoll, int characterType,
            int armorClass, int acRoll) {
        return BattleTimeAttackPhysicalDamage.againstPlayer(
                level, SourceNpcPhysicalStats.memoryFixture(npcId),
                SourcePhysicalGrowthStats.classic381(), leverage,
                0,
                characterType, armorClass, levelRoll, criticalRoll,
                acRoll, RETAINED_DAYLIGHT);
    }
}
