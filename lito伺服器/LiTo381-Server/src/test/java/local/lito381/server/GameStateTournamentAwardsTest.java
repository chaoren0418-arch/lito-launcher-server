package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateTournamentAwardsTest {
    @TempDir Path temporary;

    @Test void explicitNativeAwardsPersistAndOrdinaryResultsDoNotMutateThem()
            throws Exception {
        Path file = temporary.resolve("tournament-awards.properties");
        GameStateStore.TournamentAwards awarded =
                new GameStateStore.TournamentAwards(
                        List.of(0x31, 0x72, 0xA4, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(2, 4, 6, 8, 10), true);

        GameStateStore state = new GameStateStore(file, true);
        assertEquals(GameStateStore.TournamentAwards.none(),
                state.tournamentAwards("AwardKnight"));
        state.replaceTournamentAwards("AwardKnight", awarded);
        assertEquals(awarded, state.tournamentAwards("AwardKnight"));

        state.recordResult("AwardKnight", true, 250);
        state.recordResult("AwardKnight", false, 50);
        assertEquals(awarded, state.tournamentAwards("AwardKnight"));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(awarded, reloaded.tournamentAwards("AwardKnight"));
        assertEquals(new GameStateStore.TournamentStatistics(2, 1, 1, 0),
                reloaded.tournamentStatistics("AwardKnight"));
    }
}
