package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Pins the selected-client per-match economy reset reasons.
 *
 * <p>#138 B-land: the RESULT ordering audit was replaced by
 * {@link NativeTournamentMatchEndEconomyBehaviorTest}. The remaining check only asserts that the
 * three reset call sites still exist in the candidate source (the reason literals survived
 * decompilation unchanged); it is kept pending 主控 review of the room-exit/disconnect paths.
 */
final class NativeTournamentMatchEndEconomySourceAuditTest {
    @Test
    void roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        for (String reason : new String[] {
                "battle-result", "room-exit", "disconnect"
        }) {
            assertTrue(source.contains(
                    "resetNativeTournamentEconomy(\"" + reason + "\")"), reason);
        }
    }
}
