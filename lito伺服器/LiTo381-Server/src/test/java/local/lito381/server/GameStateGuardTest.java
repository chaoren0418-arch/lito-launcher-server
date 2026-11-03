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

final class GameStateGuardTest {
    private static final long NOW = 1_800_000_000L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.ofEpochSecond(NOW), ZoneOffset.UTC);

    @TempDir Path temp;

    @Test
    void reconcilesReadOnlySourcePersistsKillAndAppliesGuardAbsolution()
            throws Exception {
        Path file = temp.resolve("state.properties");
        long sourceLast = NOW - 60;
        GameStateStore state = new GameStateStore(file, true, CLOCK);

        GameStateStore.GeneralPkBalance initial =
                state.reconcileGeneralPk("Knight", 2, sourceLast);
        assertEquals(new GameStateStore.GeneralPkState(2, sourceLast),
                initial.observedSource());
        assertEquals(initial.observedSource(), initial.effective());
        assertTrue(initial.effective().wantedAt(NOW));

        GameStateStore.GeneralPkState killed =
                state.recordGeneralPkKill("Knight", true);
        assertEquals(3, killed.count());
        assertEquals(NOW, killed.lastPkEpochSeconds());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("generalPk.effectiveCount=3"));

        GameStateStore reloaded = new GameStateStore(file, true, CLOCK);
        GameStateStore.GeneralPkBalance retained =
                reloaded.reconcileGeneralPk("Knight", 2, sourceLast);
        assertEquals(killed, retained.effective());

        GameStateStore.GeneralPkState absolved =
                reloaded.clearGeneralPkAfterGuardDeath("Knight");
        assertEquals(2, absolved.count());
        assertEquals(0, absolved.lastPkEpochSeconds());
        assertFalse(absolved.wantedAt(NOW));
        assertEquals(absolved, new GameStateStore(file, true, CLOCK)
                .reconcileGeneralPk("Knight", 2, sourceLast).effective());
    }

    @Test
    void highLawfulKillRefreshesWantedLeaseWithoutIncrementingCount()
            throws Exception {
        GameStateStore state =
                new GameStateStore(temp.resolve("high-lawful.properties"), true, CLOCK);
        state.reconcileGeneralPk("Royal", 4, NOW - 100);
        GameStateStore.GeneralPkState killed =
                state.recordGeneralPkKill("Royal", false);
        assertEquals(4, killed.count());
        assertEquals(NOW, killed.lastPkEpochSeconds());
        assertTrue(killed.wantedAt(NOW));
    }

    @Test
    void wantedWindowIsInclusiveAtOneHourAndRejectsFutureTimestamp() {
        GameStateStore.GeneralPkState exact = new GameStateStore.GeneralPkState(
                1, NOW - ClassicWorldGuardCatalog.WANTED_SECONDS);
        assertTrue(exact.wantedAt(NOW));
        assertFalse(exact.wantedAt(NOW + 1));
        assertFalse(new GameStateStore.GeneralPkState(1, NOW + 1).wantedAt(NOW));
        assertFalse(new GameStateStore.GeneralPkState(1, 0).wantedAt(NOW));
    }

    @Test
    void failedKillSaveRestoresPresenceAndEffectiveFields() throws Exception {
        Path blocker = temp.resolve("not-a-directory");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        Files.delete(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class,
                () -> state.recordGeneralPkKill("Knight", true));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        assertEquals(new GameStateStore.GeneralPkState(0, 0),
                state.reconcileGeneralPk("Knight", 0, 0).effective());
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void failedGuardClearSaveAlsoRestoresPresenceAndEffectiveFields()
            throws Exception {
        Path blocker = temp.resolve("not-a-directory-clear");
        Files.createDirectory(blocker);
        Path file = blocker.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true, CLOCK);
        Files.delete(file);
        Files.delete(blocker);
        Files.writeString(blocker, "block");

        assertThrows(IOException.class,
                () -> state.clearGeneralPkAfterGuardDeath("Knight"));
        Files.delete(blocker);
        Files.createDirectory(blocker);
        assertEquals(new GameStateStore.GeneralPkState(0, 0),
                state.reconcileGeneralPk("Knight", 0, 0).effective());
        assertTrue(Files.isRegularFile(file));
    }
}
