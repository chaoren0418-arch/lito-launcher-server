package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

final class GameStateGuardianTest {
    private static final long NOW = 1_800_000_000L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.ofEpochSecond(NOW), ZoneOffset.UTC);

    @TempDir Path temp;

    @Test
    void reconcilesReadOnlySourceThenPersistsKillAndGuardianAbsolution()
            throws Exception {
        Path file = temp.resolve("state.properties");
        long sourceLast = NOW - 60;
        GameStateStore state = new GameStateStore(file, true, CLOCK);

        GameStateStore.ElfPkBalance initial =
                state.reconcileElfPk("Elf", 2, sourceLast);
        assertEquals(new GameStateStore.ElfPkState(2, sourceLast),
                initial.observedSource());
        assertEquals(initial.observedSource(), initial.effective());
        assertTrue(initial.effective().wantedAt(NOW));

        GameStateStore.ElfPkState killed = state.recordElfPkKill("Elf", true);
        assertEquals(3, killed.count());
        assertEquals(NOW, killed.lastPkEpochSeconds());
        assertTrue(Files.readString(file).contains("format.version=43"));

        GameStateStore reloaded = new GameStateStore(file, true, CLOCK);
        GameStateStore.ElfPkBalance retained =
                reloaded.reconcileElfPk("Elf", 2, sourceLast);
        assertEquals(new GameStateStore.ElfPkState(2, sourceLast),
                retained.observedSource());
        assertEquals(killed, retained.effective());

        GameStateStore.ElfPkState absolved =
                reloaded.clearElfPkAfterGuardianDeath("Elf");
        assertEquals(2, absolved.count());
        assertEquals(0, absolved.lastPkEpochSeconds());
        assertFalse(absolved.wantedAt(NOW));
        GameStateStore.ElfPkBalance afterRestart =
                new GameStateStore(file, true, CLOCK)
                        .reconcileElfPk("Elf", 2, sourceLast);
        assertEquals(absolved, afterRestart.effective());
    }

    @Test
    void wantedWindowIsInclusiveAtTwentyFourHoursAndRejectsFutureTimestamp() {
        GameStateStore.ElfPkState exact = new GameStateStore.ElfPkState(
                1, NOW - ClassicWorldGuardianCatalog.WANTED_SECONDS);
        assertTrue(exact.wantedAt(NOW));
        assertFalse(exact.wantedAt(NOW + 1));
        assertFalse(new GameStateStore.ElfPkState(1, NOW + 1).wantedAt(NOW));
        assertFalse(new GameStateStore.ElfPkState(1, 0).wantedAt(NOW));
    }

    @Test
    void failedKillSaveRestoresPresenceAndAllEffectiveFields() throws Exception {
        Path blocker = temp.resolve("not-a-directory");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        Files.delete(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class,
                () -> state.recordElfPkKill("Elf", true));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        GameStateStore.ElfPkBalance recovered = state.reconcileElfPk("Elf", 0, 0);
        assertEquals(new GameStateStore.ElfPkState(0, 0), recovered.effective());
        assertTrue(Files.isRegularFile(file),
                "presence rollback must force the source reconciliation save");
    }

    @Test
    void failedGuardianClearSaveAlsoRestoresPresenceAndEffectiveFields()
            throws Exception {
        Path blocker = temp.resolve("not-a-directory-clear");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        Files.delete(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class,
                () -> state.clearElfPkAfterGuardianDeath("Elf"));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        GameStateStore.ElfPkBalance recovered = state.reconcileElfPk("Elf", 0, 0);
        assertEquals(new GameStateStore.ElfPkState(0, 0), recovered.effective());
        assertTrue(Files.isRegularFile(file),
                "presence rollback must force the source reconciliation save");
    }
}
