package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T103 (#310): rules decided in docs/design/gameplay.md section 7 (D1, D2, D4, D8). */
final class GameplayDecisionRulesTest {
    @TempDir Path temporary;

    private static final long LONG_AFTER = 10_000_000_000_000L;

    // ---- D4: timeout without a winner is a draw (nobody ranked) ----

    @Test void deathmatchHonorAndQuestTimeoutsAreDraws() {
        for (BattleRules.Mode mode : List.of(BattleRules.Mode.DEATHMATCH,
                BattleRules.Mode.HONOR_MATCH, BattleRules.Mode.QUEST_BATTLE)) {
            BattleRules.MatchState match = state(mode);
            match.begin(List.of(1L, 2L, 3L, 4L), Map.of(1L, 0, 2L, 0, 3L, 1, 4L, 1), 0);
            BattleRules.Resolution timeout = match.timeout(LONG_AFTER);
            assertDraw(timeout, mode.key() + "-timeout");
            assertEquals(BattleRules.Phase.FINISHED, match.phase());
        }
    }

    @Test void petRaceTimeoutWithoutFinisherIsADraw() {
        BattleRules.MatchState race = state(BattleRules.Mode.PET_RACE);
        race.begin(List.of(1L, 2L), Map.of(), 0);
        assertDraw(race.timeout(LONG_AFTER), "petrace-timeout");
    }

    @Test void timeAttackTimeoutIsADrawEvenAfterAMemberFell() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L, 3L), Map.of(), 1L, 0);
        assertTrue(match.startTimeAttack(1_000L));
        assertFalse(match.playerDefeated(0L, 3L).ended());
        assertDraw(match.timeout(LONG_AFTER), "timeattack-timeout");
    }

    @Test void siegeTimeoutStillAwardsTheDefenders() {
        BattleRules.MatchState match = state(BattleRules.Mode.SIEGE_WAR);
        match.begin(List.of(1L, 2L, 3L, 4L), Map.of(1L, 0, 2L, 0, 3L, 1, 4L, 1), 0);
        assertTrue(match.assignSiegeAttackerForce(1));
        BattleRules.Resolution timeout = match.timeout(LONG_AFTER);
        assertTrue(timeout.ended());
        assertFalse(timeout.isDraw());
        assertEquals(Set.of(1L, 2L), timeout.winners());
        assertEquals(Set.of(3L, 4L), timeout.losers());
    }

    @Test void siegeTimeoutWithoutAnAssignedAttackerIsADraw() {
        BattleRules.MatchState match = state(BattleRules.Mode.SIEGE_WAR);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        assertDraw(match.timeout(LONG_AFTER), "siegewar-timeout");
    }

    @Test void decisiveResultsAreNotDraws() {
        BattleRules.MatchState match = state(BattleRules.Mode.DEATHMATCH);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        BattleRules.Resolution result = match.playerDefeated(1L, 2L);
        assertTrue(result.ended());
        assertFalse(result.isDraw());
        assertFalse(BattleRules.Resolution.continuing(false, "running").isDraw());
    }

    @Test void drawProducesNoDurableSettlementRow() {
        String battle = UUID.randomUUID().toString();
        BattleRules.Resolution draw = BattleRules.Resolution.draw("deathmatch-timeout");
        assertNull(DurableBattleSettlement.entry(battle, 7L, "380_ac_1", "Sun",
                "380_lito", 99L, "Hero", draw, Set.of()));

        BattleRules.Resolution loss = new BattleRules.Resolution(
                true, false, Set.of(8L), Set.of(7L), "deathmatch-opposing-team-eliminated");
        SettlementJournal.Entry row = DurableBattleSettlement.entry(battle, 7L, "380_ac_1",
                "Sun", "380_lito", 99L, "Hero", loss, Set.of());
        assertNotNull(row);
        assertFalse(row.won());
    }

    // ---- D8: Mode table agrees with the death handler ----

    @Test void petRaceModeTableRespawnMatchesDeathHandling() {
        assertTrue(BattleRules.Mode.PET_RACE.respawn());
        BattleRules.MatchState race = state(BattleRules.Mode.PET_RACE);
        assertTrue(race.rules().respawn());
        race.begin(List.of(1L, 2L), Map.of(), 0);
        assertTrue(race.playerDefeated(1L, 2L).respawn());
    }

    @Test void respawnColumnMatchesEveryBattleModeDeathHandler() {
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            BattleRules.MatchState match = state(mode);
            if (mode == BattleRules.Mode.TIME_ATTACK) {
                match.begin(List.of(1L, 2L, 3L), Map.of(), 1L, 0);
                assertEquals(mode.respawn(), match.playerDefeated(0L, 3L).respawn(), mode.key());
            } else {
                match.begin(List.of(1L, 2L, 3L, 4L),
                        Map.of(1L, 0, 2L, 0, 3L, 1, 4L, 1), 0);
                assertEquals(mode.respawn(), match.playerDefeated(3L, 1L).respawn(), mode.key());
            }
        }
    }

    // ---- D1: score formula (defaults documented in docs/design/gameplay.md section 4.3) ----

    @Test void scoreFormulaWinLossAndDisconnectFloorAtZero() throws Exception {
        GameStateStore state = new GameStateStore(temporary.resolve("d1.properties"), false);
        assertEquals(0, state.recordResult("Fresh", false, true, 50).rankingPoints(),
                "a forced disconnect never drives the score below zero");
        assertEquals(250, state.recordResult("Fresh", true, 250).rankingPoints());
        assertEquals(300, state.recordResult("Fresh", false, 50).rankingPoints());
        assertEquals(250, state.recordResult("Fresh", false, true, 50).rankingPoints());
    }

    private static void assertDraw(BattleRules.Resolution resolution, String reason) {
        assertTrue(resolution.ended(), reason);
        assertTrue(resolution.isDraw(), reason);
        assertTrue(resolution.winners().isEmpty(), reason);
        assertTrue(resolution.losers().isEmpty(), reason + ": a draw records no loss");
        assertEquals(reason, resolution.reason());
    }

    private static BattleRules.MatchState state(BattleRules.Mode mode) {
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "mode", "", 8, mode.wireId(), 0, 0, 0, 0, List.of());
        return new BattleRules.MatchState(BattleRules.decode(settings));
    }
}
