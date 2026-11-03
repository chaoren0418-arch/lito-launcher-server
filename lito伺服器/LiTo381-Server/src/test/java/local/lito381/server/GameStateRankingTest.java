package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateRankingTest {
    @TempDir Path temporary;

    @Test void officialEligibilityScoreOrderAndNativePartitionsStayDistinct()
            throws Exception {
        GameStateStore state = new GameStateStore(temporary.resolve("ranking.properties"), false);
        state.bindRankingIdentity("Alpha", "Sun", 0, 20, 1);
        state.bindRankingIdentity("Beta", "Moon", 0, 20, 2);
        state.bindRankingIdentity("Gamma", "Sun", 1, 21, 1);
        state.bindRankingIdentity("Delta", "Sun", 0, 20, 1);

        record(state, "Alpha", 20, 0, 10);
        record(state, "Beta", 20, 0, 5);
        record(state, "Gamma", 19, 0, 100);
        record(state, "Delta", 10, 10, 20);

        GameStateStore.RankingQuery alpha = new GameStateStore.RankingQuery("Sun", 0, 20, 1);
        assertEquals(List.of("Delta(Sun)", "Alpha(Sun)", "Beta(Moon)"), names(state.ranking(
                Protocol.LitoRankingScope.OVERALL, alpha, 100)));
        assertEquals(List.of(1, 1, 2), state.ranking(
                Protocol.LitoRankingScope.OVERALL, alpha, 100).stream()
                .map(GameStateStore.Ranking::classIcon).toList());
        assertEquals(List.of("Delta(Sun)", "Alpha(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.CLASSIC_SERVER, alpha, 100)));
        assertEquals(List.of("Delta(Sun)", "Alpha(Sun)", "Beta(Moon)"), names(state.ranking(
                Protocol.LitoRankingScope.CHANNEL, alpha, 100)));
        assertEquals(List.of("Delta(Sun)", "Alpha(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.RACE, alpha, 100)));
        assertEquals(List.of("Delta(Sun)", "Alpha(Sun)", "Beta(Moon)"),
                names(state.ranking(
                        Protocol.LitoRankingScope.MONTHLY, alpha, 100)));

        Protocol.LitoCharacterRankings ranks = state.characterRankings("Alpha");
        assertEquals(2, ranks.overall());
        assertEquals(2, ranks.classicServer());
        assertEquals(2, ranks.channel());
        assertEquals(2, ranks.level());
        assertEquals(2, ranks.race());
        assertEquals(0, ranks.overallMovement());
    }

    @Test void retainsDragonKnightAndIllusionistRacePartitions() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("381-races.properties"), false);
        state.bindRankingIdentity("DragonKnight", "Sun", 0, 52, 5);
        state.bindRankingIdentity("Illusionist", "Sun", 0, 52, 6);
        record(state, "DragonKnight", 20, 0, 100);
        record(state, "Illusionist", 20, 0, 100);
        assertEquals(List.of("DragonKnight(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.RACE,
                new GameStateStore.RankingQuery("Sun", 0, 52, 5), 100)));
        assertEquals(List.of("Illusionist(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.RACE,
                new GameStateStore.RankingQuery("Sun", 0, 52, 6), 100)));
    }

    @Test void rankingScoreIsSeparateFromShopMoneyAndDisconnectsLoseScore()
            throws Exception {
        GameStateStore state = new GameStateStore(temporary.resolve("loss.properties"), false);
        GameStateStore.Progress afterWin = state.recordResult("Score", true, 250);
        GameStateStore.Progress afterLoss = state.recordResult("Score", false, 50);
        GameStateStore.Progress afterDisconnect =
                state.recordResult("Score", false, true, 50);
        assertEquals(5_000, afterWin.shopPoints());
        assertEquals(5_000, afterLoss.shopPoints());
        assertEquals(250, afterWin.rankingPoints());
        assertEquals(300, afterLoss.rankingPoints());
        assertEquals(250, afterDisconnect.rankingPoints());
    }

    @Test void cookingDessertBonusAppliesOnlyToTheExperienceAward()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("cooking-exp.properties"), false);
        GameStateStore.Progress win = state.recordResult(
                "Cook", true, false, 250, 1);
        GameStateStore.Progress loss = state.recordResult(
                "Cook", false, false, 50, 1);
        assertEquals(101, win.experience());
        // The source multiplier is applied before integer storage: 25 * 1%
        // does not cross the next whole experience point.
        assertEquals(126, loss.experience());
        assertEquals(300, loss.rankingPoints());
    }

    @Test void levelFourCookingExperienceUsesItsExactTwoAndFourPercentRates()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("cooking-level4-exp.properties"), false);
        GameStateStore.Progress mainCourse = state.recordResult(
                "LevelFourCook", true, false, 250,
                BattleCookingCatalog.require(49825).experiencePercent());
        GameStateStore.Progress dessert = state.recordResult(
                "LevelFourCook", true, false, 250,
                BattleCookingCatalog.require(49828).experiencePercent());
        assertEquals(102, mainCourse.experience());
        assertEquals(206, dessert.experience());
        assertEquals(500, dessert.rankingPoints());
    }

    @Test void rankingRemainsFrozenUntilTheNextOfficialFiveAmBoundary()
            throws Exception {
        MutableClock clock = new MutableClock("2026-08-12T21:01:00Z");
        Path file = temporary.resolve("daily-snapshot.properties");
        GameStateStore state = new GameStateStore(file, true, clock);
        state.bindRankingIdentity("Alpha", "Sun", 0, 20, 1);
        state.bindRankingIdentity("Beta", "Sun", 0, 20, 1);
        record(state, "Alpha", 20, 0, 10);
        record(state, "Beta", 20, 0, 5);
        GameStateStore.RankingQuery query = new GameStateStore.RankingQuery("Sun", 0, 20, 1);

        assertEquals(List.of("Alpha(Sun)", "Beta(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.OVERALL, query, 100)));
        state.recordResult("Beta", true, 1_000);
        assertEquals(List.of("Alpha(Sun)", "Beta(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.OVERALL, query, 100)));

        // Restarting before the next boundary must retain the same frozen table.
        state = new GameStateStore(file, true, clock);
        assertEquals(List.of("Alpha(Sun)", "Beta(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.OVERALL, query, 100)));

        clock.set("2026-08-13T21:01:00Z");
        List<GameStateStore.Ranking> moved = state.ranking(
                Protocol.LitoRankingScope.OVERALL, query, 100);
        assertEquals(List.of("Beta(Sun)", "Alpha(Sun)"), names(moved));
        assertEquals(1, moved.get(0).rankMovement());
        assertEquals(-1, moved.get(1).rankMovement());
        assertEquals(1, state.characterRankings("Beta").overallMovement());
        assertEquals(-1, state.characterRankings("Alpha").overallMovement());
    }

    @Test void monthlyRankingUsesAnIndependentCalendarAccumulatorAndDailyFreeze()
            throws Exception {
        MutableClock clock = new MutableClock("2026-08-30T21:01:00Z");
        Path file = temporary.resolve("monthly-ranking.properties");
        GameStateStore state = new GameStateStore(file, true, clock);
        state.bindRankingIdentity("Alpha", "Sun", 0, 20, 1);
        state.bindRankingIdentity("Beta", "Moon", 1, 20, 2);
        record(state, "Alpha", 20, 0, 10);
        record(state, "Beta", 20, 0, 5);
        GameStateStore.RankingQuery query =
                new GameStateStore.RankingQuery("Sun", 0, 20, 1);

        List<GameStateStore.Ranking> august = state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100);
        assertEquals(List.of("Alpha(Sun)", "Beta(Moon)"), names(august));
        assertEquals(List.of(20, 20), august.stream()
                .map(GameStateStore.Ranking::wins).toList());

        // The current-month accumulator changes immediately, but the native
        // page remains frozen at the most recent daily 05:00 boundary.
        state.recordResult("Beta", true, 1_000);
        assertEquals(List.of("Alpha(Sun)", "Beta(Moon)"), names(state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100)));
        state = new GameStateStore(file, true, clock);
        assertEquals(List.of("Alpha(Sun)", "Beta(Moon)"), names(state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100)));

        // At the first September 05:00 boundary both monthly rows reset to
        // 0/0, while lifetime qualification and the lifetime ranking remain.
        clock.set("2026-08-31T21:01:00Z");
        List<GameStateStore.Ranking> september = state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100);
        assertEquals(List.of("Beta(Moon)", "Alpha(Sun)"), names(september));
        assertEquals(List.of(0, 0), september.stream()
                .map(GameStateStore.Ranking::wins).toList());
        assertEquals(List.of(0, 0), september.stream()
                .map(GameStateStore.Ranking::losses).toList());
        assertEquals(List.of(0, 0), september.stream()
                .map(GameStateStore.Ranking::rankMovement).toList());

        state.recordResult("Alpha", true, 2_000);
        assertEquals(List.of("Beta(Moon)", "Alpha(Sun)"), names(state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100)));
        clock.set("2026-09-01T21:01:00Z");
        List<GameStateStore.Ranking> moved = state.ranking(
                Protocol.LitoRankingScope.MONTHLY, query, 100);
        assertEquals(List.of("Alpha(Sun)", "Beta(Moon)"), names(moved));
        assertEquals(List.of(1, 0), moved.stream()
                .map(GameStateStore.Ranking::wins).toList());
        assertEquals(List.of(1, -1), moved.stream()
                .map(GameStateStore.Ranking::rankMovement).toList());
    }

    @Test void sourceQualifiedSameNameProfilesStayIndependentAndMigrateLegacyState()
            throws Exception {
        Path file = temporary.resolve("source-qualified.properties");
        GameStateStore state = new GameStateStore(file, true);
        String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
        String moon = GameStateStore.sourceCharacter("Moon", "SameHero");
        state.bindRankingIdentity(sun, "Sun", 0, 20, 1);
        state.bindRankingIdentity(moon, "Moon", 1, 21, 2);
        state.recordResult(sun, true, 100);
        state.recordResult(moon, false, 25);
        state.saveEconomy(sun, 111, java.util.Map.of(2001L, 1));
        state.saveEconomy(moon, 222, java.util.Map.of(2001L, 2));

        assertEquals(1, state.tournamentStatistics(sun).wins());
        assertEquals(0, state.tournamentStatistics(moon).wins());
        assertEquals(111, state.economy(sun).points());
        assertEquals(222, state.economy(moon).points());

        state = new GameStateStore(file, true);
        assertEquals(1, state.tournamentStatistics(sun).wins());
        assertEquals(1, state.tournamentStatistics(moon).losses());
        assertEquals(111, state.economy(sun).points());
        assertEquals(222, state.economy(moon).points());

        state.recordResult("LegacyHero", true, 77);
        String migrated = GameStateStore.sourceCharacter("Sun", "LegacyHero");
        state.bindRankingIdentity(migrated, "Sun", 0, 20, 1);
        assertEquals(1, state.tournamentStatistics(migrated).wins());
        String independent = GameStateStore.sourceCharacter("Moon", "LegacyHero");
        state.bindRankingIdentity(independent, "Moon", 0, 20, 1);
        assertEquals(0, state.tournamentStatistics(independent).wins());
    }

    @Test void sqlTournamentHookSeesWholeCandidateAndFailureRestoresMemory()
            throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("sql-hook.properties"), true);
        GameStateStore.TournamentProfileState before =
                state.tournamentProfileState("SqlHero");
        IOException refused = assertThrows(IOException.class, () -> state.recordResult(
                "SqlHero", true, false, 90, 0, (expected, replacement) -> {
                    assertEquals(before, expected);
                    assertEquals(1, replacement.wins());
                    assertEquals(90, replacement.rankingPoints());
                    throw new IOException("SQL CAS conflict");
                }));
        assertEquals("SQL CAS conflict", refused.getMessage());
        assertEquals(before, state.tournamentProfileState("SqlHero"));

        state.recordResult("SqlHero", true, false, 90, 0,
                (expected, replacement) -> {
                    assertEquals(before, expected);
                    assertEquals(1, replacement.wins());
                });
        assertEquals(1, state.tournamentStatistics("SqlHero").wins());
    }

    private static void record(GameStateStore state, String name, int wins, int losses,
                               int magnitude) throws Exception {
        for (int index = 0; index < wins; index++) {
            state.recordResult(name, true, magnitude);
        }
        for (int index = 0; index < losses; index++) {
            state.recordResult(name, false, magnitude);
        }
    }

    private static List<String> names(List<GameStateStore.Ranking> rows) {
        return rows.stream().map(GameStateStore.Ranking::name).toList();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(String instant) {
            this.instant = Instant.parse(instant);
        }

        void set(String value) {
            instant = Instant.parse(value);
        }

        @Override public ZoneId getZone() {
            return GameStateStore.RANKING_ZONE;
        }

        @Override public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override public Instant instant() {
            return instant;
        }
    }
}
