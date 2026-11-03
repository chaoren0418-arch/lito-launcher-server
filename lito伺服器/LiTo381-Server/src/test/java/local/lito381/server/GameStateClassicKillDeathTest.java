package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateClassicKillDeathTest {
    @TempDir Path temporary;

    @Test void reconcilesSourceAndAtomicallyPersistsBothPlayerCounters()
            throws Exception {
        Path file = temporary.resolve("kill-death.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(new GameStateStore.ClassicKillDeathState(7, 3),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Winner", new SourceKillDeathCounts(7, 3))
                        .effective());
        assertEquals(new GameStateStore.ClassicKillDeathState(2, 9),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Loser", new SourceKillDeathCounts(2, 9))
                        .effective());

        GameStateStore.ClassicPlayerKillMutation mutation =
                state.recordClassicPlayerKill(
                        "Perfect-1:Winner", "Perfect-1:Loser");
        assertEquals(new GameStateStore.ClassicKillDeathState(7, 3),
                mutation.attackerBefore());
        assertEquals(new GameStateStore.ClassicKillDeathState(8, 3),
                mutation.attackerAfter());
        assertEquals(new GameStateStore.ClassicKillDeathState(2, 9),
                mutation.victimBefore());
        assertEquals(new GameStateStore.ClassicKillDeathState(2, 10),
                mutation.victimAfter());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(mutation.attackerAfter(), reloaded.reconcileClassicKillDeath(
                "Perfect-1:Winner", new SourceKillDeathCounts(7, 3)).effective());
        assertEquals(mutation.victimAfter(), reloaded.reconcileClassicKillDeath(
                "Perfect-1:Loser", new SourceKillDeathCounts(2, 9)).effective());
    }

    @Test void changedReadOnlySourceRebasesTheSidecarOverlay() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("source-drift.properties"), true);
        state.reconcileClassicKillDeath(
                "Perfect-1:Winner", new SourceKillDeathCounts(1, 2));
        state.reconcileClassicKillDeath(
                "Perfect-1:Loser", new SourceKillDeathCounts(3, 4));
        state.recordClassicPlayerKill("Perfect-1:Winner", "Perfect-1:Loser");

        assertEquals(new GameStateStore.ClassicKillDeathState(10, 20),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Winner", new SourceKillDeathCounts(10, 20))
                        .effective());
    }

    @Test void failedSaveRollsBackWinnerAndLoserTogether() throws Exception {
        Path blocker = temporary.resolve("not-a-directory");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileClassicKillDeath(
                "Perfect-1:Winner", new SourceKillDeathCounts(4, 1));
        state.reconcileClassicKillDeath(
                "Perfect-1:Loser", new SourceKillDeathCounts(2, 6));
        Files.delete(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class, () -> state.recordClassicPlayerKill(
                "Perfect-1:Winner", "Perfect-1:Loser"));

        Files.delete(blocker);
        Files.createDirectory(blocker);
        assertEquals(new GameStateStore.ClassicKillDeathState(4, 1),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Winner", new SourceKillDeathCounts(4, 1))
                        .effective());
        assertEquals(new GameStateStore.ClassicKillDeathState(2, 6),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Loser", new SourceKillDeathCounts(2, 6))
                        .effective());
    }

    @Test void rejectsSelfKillWithoutMutatingState() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("self.properties"), true);
        state.reconcileClassicKillDeath(
                "Perfect-1:Same", new SourceKillDeathCounts(5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> state.recordClassicPlayerKill(
                        "Perfect-1:Same", "perfect-1:same"));
        assertEquals(new GameStateStore.ClassicKillDeathState(5, 5),
                state.reconcileClassicKillDeath(
                        "Perfect-1:Same", new SourceKillDeathCounts(5, 5))
                        .effective());
    }
}
