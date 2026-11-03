package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateModerationTest {
    @TempDir Path temporary;

    @Test void persistsCaseInsensitiveAdministrativeAccountBansInSidecarState()
            throws Exception {
        Path file = temporary.resolve("moderation-state.properties");
        GameStateStore first = new GameStateStore(file, true);
        assertFalse(first.isAccountBanned("test2"));
        assertTrue(first.banAccount("TeSt2"));
        assertFalse(first.banAccount("test2"));
        assertTrue(first.isAccountBanned("TEST2"));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertTrue(reloaded.isAccountBanned("test2"));
        assertFalse(reloaded.isAccountBanned("test3"));
    }

    @Test void keepsNonPersistentModerationStateInMemoryOnly() throws Exception {
        Path file = temporary.resolve("unused.properties");
        GameStateStore state = new GameStateStore(file, false);
        assertTrue(state.banAccount("test3"));
        assertTrue(state.isAccountBanned("TEST3"));
        assertFalse(file.toFile().exists());
    }
}
