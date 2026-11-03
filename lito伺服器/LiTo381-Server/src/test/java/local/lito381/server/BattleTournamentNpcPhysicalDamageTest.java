package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleTournamentNpcPhysicalDamageTest {
    private static final long RETAINED_DAYLIGHT =
            1_735_689_600_000L + 3_600_000L;

    @Test
    void questActorsUseTheirSelectedStrengthLevelAndBossRows() {
        assertEquals(15, hit(45262, 26).sourceDamage());
        assertEquals(21, hit(45278, 28).sourceDamage());
        assertEquals(151, hit(45601, 55).sourceDamage());
    }

    @Test
    void questWeaponBreakPanicAndPlayerAcApplyInTheRetainedOrder() {
        BattleTournamentNpcPhysicalDamage.Outcome hit =
                BattleTournamentNpcPhysicalDamage.againstPlayer(
                        28, SourceNpcPhysicalStats.memoryFixture(45278),
                        SourcePhysicalGrowthStats.classic381(), 10, 2,
                        1, -10, 1, 99, 3, RETAINED_DAYLIGHT, -1);
        assertEquals(18, hit.sourceDamage());
        assertEquals(10, hit.acMaximum());
        assertEquals(4, hit.acReduction());
        assertEquals(14, hit.afterAcDamage());
    }

    @Test
    void missingOrOutOfRangeSourceRollsFailClosed() {
        assertThrows(IllegalArgumentException.class, () ->
                BattleTournamentNpcPhysicalDamage.againstPlayer(
                        26, null, SourcePhysicalGrowthStats.classic381(),
                        10, 0, 1, 10, 1, 99, 0,
                        RETAINED_DAYLIGHT));
        assertThrows(IllegalArgumentException.class, () ->
                BattleTournamentNpcPhysicalDamage.againstPlayer(
                        26, SourceNpcPhysicalStats.memoryFixture(45262),
                        SourcePhysicalGrowthStats.classic381(),
                        10, 0, 1, 10, 26, 99, 0,
                        RETAINED_DAYLIGHT));
    }

    private static BattleTournamentNpcPhysicalDamage.Outcome hit(
            int npcId, int level) {
        return BattleTournamentNpcPhysicalDamage.againstPlayer(
                level, SourceNpcPhysicalStats.memoryFixture(npcId),
                SourcePhysicalGrowthStats.classic381(), 10, 0,
                1, 10, 1, 99, 0, RETAINED_DAYLIGHT);
    }
}
