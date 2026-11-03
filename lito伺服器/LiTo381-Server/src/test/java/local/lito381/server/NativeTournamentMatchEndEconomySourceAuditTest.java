package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Pins the selected-client per-match economy boundary to RESULT creation. */
final class NativeTournamentMatchEndEconomySourceAuditTest {
    @Test
    void resultPublishesPrizeBeforeRemovingBoughtMatchItems() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        int start = source.indexOf(
                "void serverEndBattle(BattleRules.Resolution resolution)");
        int end = source.indexOf("private boolean isPetRaceBattle()", start);
        assertTrue(start >= 0 && end > start);
        String handler = source.substring(start, end);

        int result = handler.indexOf("phase = Phase.RESULT;");
        int record = handler.indexOf("recordBattleResult(", result);
        int prize = handler.indexOf("awardPetRacePrize();", record);
        int reset = handler.indexOf(
                "resetNativeTournamentEconomy(\"battle-result\")", prize);
        assertTrue(result >= 0 && record > result && prize > record && reset > prize,
                "RESULT must record outcome and publish any prize before economy reset");
    }

    @Test
    void roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        for (String reason : new String[] {
                "battle-result", "room-exit", "battle-quit", "disconnect"
        }) {
            assertTrue(source.contains(
                    "resetNativeTournamentEconomy(\"" + reason + "\")"), reason);
        }
    }
}
