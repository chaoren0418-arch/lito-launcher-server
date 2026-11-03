package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

final class ClassicWorldRewardDistributionTest {
    @Test
    void noPartyRetainsDirectHateShares() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        100, -20, 1, false, false, List.of(
                                player(1, 45, 3, false, true),
                                player(2, 52, 1, false, true)));
        assertEquals(2, awards.size());
        assertEquals(75, awards.get(0).baseExperience());
        assertEquals(-15, awards.get(0).sourceLawfulShare());
        assertEquals(25, awards.get(1).baseExperience());
        assertEquals(-5, awards.get(1).sourceLawfulShare());
        assertFalse(awards.get(0).partyAward());
    }

    @Test
    void partyUsesContributionPoolVisiblePeerBonusAndSquaredLevels() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        1_000, -100, 1, true, false, List.of(
                                player(1, 45, 60, true, true),
                                player(2, 52, 20, true, true),
                                player(3, 50, 20, false, true)));
        assertEquals(3, awards.size());
        assertEquals(356, awards.get(0).baseExperience());
        assertEquals(-34, awards.get(0).sourceLawfulShare());
        assertEquals(475, awards.get(1).baseExperience());
        assertEquals(-45, awards.get(1).sourceLawfulShare());
        assertEquals(200, awards.get(2).baseExperience());
        assertEquals(-20, awards.get(2).sourceLawfulShare());
        assertTrue(awards.get(0).partyAward());
        assertTrue(awards.get(1).partyAward());
        assertFalse(awards.get(2).partyAward());
    }

    @Test
    void nonContributorReceivesPartyShareButInvisibleMemberDoesNot() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        1_000, 100, 1, true, false, List.of(
                                player(1, 45, 100, true, true),
                                player(2, 45, 0, true, true),
                                player(3, 45, 0, true, false)));
        assertEquals(2, awards.size());
        assertEquals(520, awards.get(0).baseExperience());
        assertEquals(520, awards.get(1).baseExperience());
        assertEquals(50, awards.get(0).sourceLawfulShare());
        assertEquals(50, awards.get(1).sourceLawfulShare());
    }

    @Test
    void visibleCrownLeaderAddsOriginalFivePointNinePercent() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        1_000, 0, 1, true, true, List.of(
                                player(1, 50, 100, true, true),
                                player(2, 50, 0, true, true)));
        assertEquals(2, awards.size());
        assertEquals(549, awards.get(0).baseExperience());
        assertEquals(549, awards.get(1).baseExperience());
    }

    @Test
    void summonHateDilutesDirectExperienceButSummonReceivesNothing() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        100, -20, 1, false, false,
                        List.of(player(1, 45, 50, false, true)),
                        List.of(companion(10, 1,
                                ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                                50)));
        assertEquals(1, awards.size());
        assertEquals(ClassicWorldRewardDistribution.RecipientKind.PLAYER,
                awards.get(0).recipientKind());
        assertEquals(50, awards.get(0).baseExperience());
        assertEquals(-20, awards.get(0).sourceLawfulShare());
    }

    @Test
    void partyOwnerLocalSplitLosesSummonShareExactlyLikeCalcExp() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        1_000, -100, 1, true, false,
                        List.of(
                                player(1, 45, 20, true, true),
                                player(2, 45, 0, true, true)),
                        List.of(companion(10, 1,
                                ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                                80)));
        assertEquals(2, awards.size());
        assertEquals(104, awards.get(0).baseExperience());
        assertEquals(-50, awards.get(0).sourceLawfulShare());
        assertEquals(520, awards.get(1).baseExperience());
        assertEquals(-50, awards.get(1).sourceLawfulShare());
    }

    @Test
    void petReceivesItsDirectAndOwnerLocalExperienceSharesWithoutLawful() {
        List<ClassicWorldRewardDistribution.Award> awards =
                ClassicWorldRewardDistribution.distribute(
                        1_000, -100, 1, true, false,
                        List.of(
                                player(1, 45, 20, true, true),
                                player(2, 45, 0, true, true)),
                        List.of(companion(10, 1,
                                ClassicWorldMonsterPopulation.ContributorKind.PET,
                                80)));
        assertEquals(3, awards.size());
        assertEquals(104, awards.get(0).baseExperience());
        assertEquals(-50, awards.get(0).sourceLawfulShare());
        assertEquals(ClassicWorldRewardDistribution.RecipientKind.PET,
                awards.get(1).recipientKind());
        assertEquals(416, awards.get(1).baseExperience());
        assertEquals(0, awards.get(1).sourceLawfulShare());
        assertEquals(520, awards.get(2).baseExperience());
    }

    private static ClassicWorldRewardDistribution.Player player(
            long objectId, int level, long hate, boolean party, boolean visible) {
        return new ClassicWorldRewardDistribution.Player(
                objectId, level, hate, party, visible);
    }

    private static ClassicWorldRewardDistribution.Companion companion(
            long objectId, long ownerObjectId,
            ClassicWorldMonsterPopulation.ContributorKind kind, long hate) {
        return new ClassicWorldRewardDistribution.Companion(
                objectId, ownerObjectId, kind, hate);
    }
}
