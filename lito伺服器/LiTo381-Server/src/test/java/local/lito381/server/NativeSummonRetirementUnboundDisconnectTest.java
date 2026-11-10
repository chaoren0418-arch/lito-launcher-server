package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * #138 approved deviation (issuecomment-6777167802): candidate 50635f7e called
 * {@code NativeSummonRetirement.beforeDisconnect} for every session, which threw
 * {@code invalid source-qualified character identity} for sessions that never bound a
 * source account and skipped the rest of {@code GameHub.disconnect}. 0.6.71 has no such
 * step, so an unbound session must disconnect cleanly and release everything it held.
 */
final class NativeSummonRetirementUnboundDisconnectTest {
    private static final Path CONFIG =
            Path.of("src/test/resources/server-config/config/server.selftest.properties");

    @Test
    void sessionThatNeverLoggedInDisconnectsWithoutThrowingAndLeavesTheHub() throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        try (GameHub hub = new GameHub(config)) {
            ClientSession anonymous = session(ClientSession.Endpoint.GAME, config, hub);
            hub.connect(anonymous);

            assertDoesNotThrow(() -> NativeSummonRetirement.beforeDisconnect(hub, anonymous));
            assertDoesNotThrow(() -> hub.disconnect(anonymous));

            // Removed from connectedSessions: a disconnected session can no longer claim an account.
            assertFalse(hub.acceptAuthenticatedAccount(
                    anonymous, ClientSession.Endpoint.GAME, "test"));
        }
    }

    @Test
    void authenticatedSessionWithoutSourceSelectionReleasesItsAccountOnDisconnect() throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        try (GameHub hub = new GameHub(config)) {
            ClientSession game = session(ClientSession.Endpoint.GAME, config, hub);
            ClientSession contender = session(ClientSession.Endpoint.GAME, config, hub);
            hub.connect(game);
            hub.connect(contender);
            assertTrue(hub.acceptAuthenticatedAccount(game, ClientSession.Endpoint.GAME, "test"));
            assertFalse(hub.acceptAuthenticatedAccount(contender, ClientSession.Endpoint.GAME, "TEST"));

            assertDoesNotThrow(() -> hub.disconnect(game));

            // The full disconnect cleanup ran: the account ownership was released and the
            // departed session is no longer connected.
            assertTrue(hub.acceptAuthenticatedAccount(contender, ClientSession.Endpoint.GAME, "test"));
            assertFalse(hub.acceptAuthenticatedAccount(game, ClientSession.Endpoint.GAME, "test"));
        }
    }

    private static ClientSession session(ClientSession.Endpoint endpoint,
                                         ServerConfig config, GameHub hub) {
        return new ClientSession(null, endpoint, config, null, null, LocalResources.discardCapture(), hub);
    }
}
