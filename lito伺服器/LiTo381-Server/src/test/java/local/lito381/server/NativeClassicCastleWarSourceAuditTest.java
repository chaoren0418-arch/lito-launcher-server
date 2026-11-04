package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the selected-source war clock and gate restriction to their consumers. */
final class NativeClassicCastleWarSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    void sourceAndAllRetainedGatekeepersUseTheExactDestructibleDoorRule()
            throws Exception {
        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_NPCAction.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains(
                "finalIsNowWar && (door.getMaxHp() > 1)"));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int authenticate = session.indexOf("account = auth.account();");
        int loginAccepted = session.indexOf(
                "hub.acceptAuthenticatedAccount(", authenticate);
        assertTrue(authenticate >= 0 && loginAccepted > authenticate);
        String defaultSourceLoad = session.substring(authenticate, loginAccepted);
        assertTrue(defaultSourceLoad.contains(
                "repositoryForClassicSource(selectedClassicServerName)"));
        assertTrue(defaultSourceLoad.contains(".castleWarSchedules()"));
        assertTrue(defaultSourceLoad.contains(
                "hub.registerClassicCastleWarSchedules("));

        int explicitSource = session.indexOf(
                "private void requestLitoCharacterInfo(");
        int explicitEnd = session.indexOf(
                "private SourceCharacterDirectory loadCharacterDirectory(",
                explicitSource);
        assertTrue(explicitSource >= 0 && explicitEnd > explicitSource);
        String explicitSourceLoad = session.substring(explicitSource, explicitEnd);
        assertTrue(explicitSourceLoad.contains(".castleWarSchedules()"));
        assertTrue(explicitSourceLoad.contains(
                "hub.registerClassicCastleWarSchedules("));

        assertGatekeeperConnected(session,
                "private void handleAdenCastleGatekeeperAction(",
                "private void handleHeineCastleGatekeeperAction(");
        assertGatekeeperConnected(session,
                "private void handleHeineCastleGatekeeperAction(",
                "private void handleHeineCastleGuardAction(");
        assertGatekeeperConnected(session,
                "private void handleDwarfCastleGuardAction(",
                "private void broadcastDwarfCastleDoors(");
    }

    private static void assertGatekeeperConnected(
            String session, String startMarker, String endMarker) {
        int start = session.indexOf(startMarker);
        int end = session.indexOf(endMarker, start);
        assertTrue(start >= 0 && end > start);
        String method = session.substring(start, end);
        assertTrue(method.contains("hub.classicCastleWarActive("));
        assertTrue(method.contains(
                "ClassicWorldCastleGateRules.blocksKeeperToggle("));
        assertTrue(method.indexOf("blocksKeeperToggle(")
                < method.indexOf("CastleDoorsOpen("));
    }
}
