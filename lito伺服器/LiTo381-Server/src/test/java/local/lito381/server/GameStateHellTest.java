package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateHellTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.ofEpochSecond(1_800_000_000L), ZoneOffset.UTC);

    @TempDir Path temp;

    @Test
    void readOnlySourceReconcilesAndSidecarRetainsElapsedHellTime()
            throws Exception {
        Path file = temp.resolve("hell.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);

        GameStateStore.HellBalance initial = state.reconcileHell("Knight", 15);
        assertEquals(new GameStateStore.HellState(15), initial.observedSource());
        assertEquals(initial.observedSource(), initial.effective());

        assertEquals(new GameStateStore.HellState(27_300),
                state.startHell("Knight", 100));
        assertEquals(new GameStateStore.HellState(27_293),
                state.elapseHell("Knight", 7));
        String durable = Files.readString(file);
        assertTrue(durable.contains("hell.present=true"));
        assertTrue(durable.contains("hell.observedTicks=15"));
        assertTrue(durable.contains("hell.effectiveTicks=27293"));

        GameStateStore reloaded = new GameStateStore(file, true, CLOCK);
        GameStateStore.HellBalance retained =
                reloaded.reconcileHell("Knight", 15);
        assertEquals(new GameStateStore.HellState(15), retained.observedSource());
        assertEquals(new GameStateStore.HellState(27_293), retained.effective());
        assertEquals(new GameStateStore.HellState(0),
                reloaded.elapseHell("Knight", Integer.MAX_VALUE));
    }

    @Test
    void changedReadOnlySourceBecomesNewEffectiveTruth() throws Exception {
        Path file = temp.resolve("changed-source.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        state.reconcileHell("Elf", 20);
        state.elapseHell("Elf", 5);

        GameStateStore.HellBalance changed = state.reconcileHell("Elf", 30);
        assertEquals(new GameStateStore.HellState(30), changed.observedSource());
        assertEquals(new GameStateStore.HellState(30), changed.effective());
    }

    @Test
    void failedHellWritesRestoreInMemoryState() throws Exception {
        Path blocker = temp.resolve("not-a-directory");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        state.reconcileHell("Knight", 9);
        Files.deleteIfExists(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class, () -> state.startHell("Knight", 100));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        assertEquals(new GameStateStore.HellState(9),
                state.reconcileHell("Knight", 9).effective());

        Files.deleteIfExists(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");
        assertThrows(IOException.class, () -> state.elapseHell("Knight", 3));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        assertEquals(new GameStateStore.HellState(9),
                state.reconcileHell("Knight", 9).effective());
    }
}
