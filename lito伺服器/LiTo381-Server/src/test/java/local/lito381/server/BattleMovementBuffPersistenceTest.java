package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleMovementBuffPersistenceTest {
    @TempDir Path temporary;

    @Test void sidecarRoundTripsWindWalkIdentityAndRemainingTime()
            throws Exception {
        Path file = temporary.resolve("movement-buff.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedMovementBuff("WindElf",
                new GameStateStore.TimedMovementBuff(
                        BattleSpellCatalog.WIND_WALK, 731));

        GameStateStore.TimedMovementBuff restored =
                new GameStateStore(file, true).timedMovementBuff("WindElf");
        assertTrue(restored.active());
        assertEquals(BattleSpellCatalog.WIND_WALK, restored.skillId());
        assertEquals(731, restored.remainingSeconds());

        state.saveTimedMovementBuff(
                "WindElf", GameStateStore.TimedMovementBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedMovementBuff("WindElf").active());
    }

    @Test void durableContractRejectsUnknownOrImpossibleLeases() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedMovementBuff(149, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedMovementBuff(
                        BattleSpellCatalog.WIND_WALK, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedMovementBuff(
                        BattleSpellCatalog.WIND_WALK, 961));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedMovementBuff(0, 1));
    }
}
