package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleArmorProtectionPersistenceTest {
    @TempDir Path temporary;

    @Test void sidecarRoundTripsEarthSkinIdentityAndRemainingTime()
            throws Exception {
        Path file = temporary.resolve("armor-protection.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedArmorProtection("EarthElf",
                new GameStateStore.TimedArmorProtection(
                        BattleSpellCatalog.EARTH_SKIN, 731));

        GameStateStore.TimedArmorProtection restored =
                new GameStateStore(file, true)
                        .timedArmorProtection("EarthElf");
        assertTrue(restored.active());
        assertEquals(BattleSpellCatalog.EARTH_SKIN, restored.skillId());
        assertEquals(731, restored.remainingSeconds());

        state.saveTimedArmorProtection(
                "EarthElf", GameStateStore.TimedArmorProtection.none());
        assertFalse(new GameStateStore(file, true)
                .timedArmorProtection("EarthElf").active());
    }

    @Test void durableContractRejectsUnknownOrImpossibleLeases() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedArmorProtection(150, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedArmorProtection(
                        BattleSpellCatalog.EARTH_SKIN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedArmorProtection(
                        BattleSpellCatalog.EARTH_SKIN, 961));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedArmorProtection(0, 1));
    }
}
