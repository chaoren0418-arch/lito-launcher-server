package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NativeCastleSecuritySettlementSourceAuditTest {
    @Test
    void officialPreSiegeChargeHasDurableRouteScopedExecutionGate()
            throws Exception {
        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8)
                // #138 B-land: LegacyMySqlRepository now lives in its own file.
                + Files.readString(Path.of(
                        "src/main/java/local/lito381/server/LegacyMySqlRepository.java"),
                        StandardCharsets.UTF_8);
        String state = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameStateStore.java"),
                StandardCharsets.UTF_8);
        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);

        assertTrue(accounts.contains(
                "lito_castle_security_settlement WRITE"));
        assertTrue(accounts.contains(
                "PRIMARY KEY (castle_id,war_start_millis)"));
        assertTrue(accounts.contains("treasury_before"));
        assertTrue(accounts.contains("prepared=1"));
        assertTrue(accounts.contains("completed=1"));
        assertTrue(state.contains("format.version\", \"43"));
        assertTrue(state.contains("castle.security.settlement.count"));
        assertTrue(hub.contains(
                "static final long CASTLE_SECURITY_CHARGE_ADENA = 100_000L"));
        assertTrue(hub.contains("settleClassicCastleSecurity("));
        assertTrue(hub.contains("serverClassicCastleWarExpel(event, destination)"));
        assertTrue(session.contains("cost=100000-at-pre-siege"));
    }
}
