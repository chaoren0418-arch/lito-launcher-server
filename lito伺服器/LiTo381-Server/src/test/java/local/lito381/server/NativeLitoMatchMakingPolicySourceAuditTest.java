package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pins the contemporary similar-win-rate source and the unrecovered details. */
final class NativeLitoMatchMakingPolicySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/analysis/native-lito-matchmaking-historical-source-20031110.json"));
    }

    private static final Path SOURCE = Path.of("..", "LiTo381-開發", "analysis",
            "native-lito-matchmaking-historical-source-20031110.json")
            .toAbsolutePath().normalize();

    @Test
    void contemporaryNcsoftAnnouncementPinsSimilarWinRateAsTheSelectionGoal()
            throws Exception {
        assertEquals("6C173839BB594B24B24AB85B525A28F8607B147ACEBF127FD01F19249C279AF4",
                sha256(SOURCE));
        String source = LocalResources.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("2003-11-10T19:19:00+09:00"));
        assertTrue(source.contains("2003-11-19"));
        assertTrue(source.contains("비슷한 승률을 가진 게이머"));
        assertTrue(source.contains("77C20FB307B076BC356382DAD677E88DF"
                + "BC9FBFFE1C97329E3293F170CA11E7E"));
        assertEquals("similar-player-win-rates",
                NativeLitoMatchMakingPolicy.HISTORICAL_SELECTION_GOAL);
        assertEquals("https://www.gamemeca.com/view.php?gid=45989",
                NativeLitoMatchMakingPolicy.HISTORICAL_SOURCE_URL);
    }

    @Test
    void productionOrdersCompatibleCandidatesByWinRateProximityThenServerFifo() {
        GameStateStore.TournamentStatistics requester = statistics(10, 8, 2);
        GameStateStore.TournamentStatistics nearby = statistics(10, 7, 3);
        GameStateStore.TournamentStatistics distant = statistics(10, 2, 8);
        GameStateStore.TournamentStatistics sameRate = statistics(20, 14, 6);

        assertTrue(NativeLitoMatchMakingPolicy.compareCandidates(
                requester, nearby, 8, distant, 1) < 0);
        assertTrue(NativeLitoMatchMakingPolicy.compareCandidates(
                requester, nearby, 8, sameRate, 3) > 0);
        assertEquals(0.1, NativeLitoMatchMakingPolicy.distance(requester, nearby),
                0.0000001);
        assertEquals("absolute-wins-over-wins-plus-losses-distance",
                NativeLitoMatchMakingPolicy.CURRENT_DISTANCE_POLICY);
        assertEquals("fifo-sequence",
                NativeLitoMatchMakingPolicy.CURRENT_TIE_BREAK_POLICY);
    }

    @Test
    void unknownHistoricalThresholdAndZeroHistoryRulesRemainExplicit() {
        GameStateStore.TournamentStatistics requester = statistics(10, 8, 2);
        GameStateStore.TournamentStatistics empty = statistics(0, 0, 0);
        assertFalse(NativeLitoMatchMakingPolicy.maximumDistanceRecovered());
        assertFalse(NativeLitoMatchMakingPolicy.zeroHistoryTreatmentRecovered());
        assertFalse(NativeLitoMatchMakingPolicy.equalDistanceTieBreakRecovered());
        assertEquals(1.0, NativeLitoMatchMakingPolicy.distance(requester, empty));
        assertEquals("both-zero-equal-one-zero-maximally-distant",
                NativeLitoMatchMakingPolicy.CURRENT_ZERO_HISTORY_POLICY);
    }

    private static GameStateStore.TournamentStatistics statistics(
            int games, int wins, int losses) {
        return new GameStateStore.TournamentStatistics(games, wins, losses, 0);
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing retained source: " + path);
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(LocalResources.readAllBytes(path)));
    }
}
