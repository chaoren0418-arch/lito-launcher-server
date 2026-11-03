package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateProjectileTest {
    @TempDir Path temporary;

    @Test void persistsReadOnlySourceDebitsAndReconcilesExternalCountChanges()
            throws Exception {
        Path file = temporary.resolve("source-projectiles.properties");
        String character = GameStateStore.sourceCharacter("Sun", "Archer");
        SourceProjectilePhysicalStats twoArrows = new SourceProjectilePhysicalStats(
                0x1234_5678L, 40743, -2, 5, 7, 66, true, 2);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceProjectileReservation first =
                state.reserveSourceProjectile(character, twoArrows);
        assertTrue(first.accepted());
        assertEquals(1, first.balance().remainingCount());

        state = new GameStateStore(file, true);
        GameStateStore.SourceProjectileReservation second =
                state.reserveSourceProjectile(character, twoArrows);
        assertTrue(second.accepted());
        assertEquals(0, second.balance().remainingCount());
        assertFalse(state.reserveSourceProjectile(character, twoArrows).accepted());

        SourceProjectilePhysicalStats externallyRestocked =
                new SourceProjectilePhysicalStats(
                        0x1234_5678L, 40743, -2, 5, 7, 66, true, 5);
        assertEquals(5, state.sourceProjectileBalance(
                character, externallyRestocked).remainingCount());
    }

    @Test void nonConsumableProjectileRemainsAvailable() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("non-consuming.properties"), true);
        SourceProjectilePhysicalStats projectile = new SourceProjectilePhysicalStats(
                0x1234_5679L, 40744, -3, 3, 4, -1, false, 1);
        GameStateStore.SourceProjectileReservation reservation =
                state.reserveSourceProjectile(
                        GameStateStore.sourceCharacter("Sun", "Thrower"), projectile);
        assertTrue(reservation.accepted());
        assertEquals(1, reservation.balance().remainingCount());
        assertEquals(2989, projectile.effectGfx());
    }
}
