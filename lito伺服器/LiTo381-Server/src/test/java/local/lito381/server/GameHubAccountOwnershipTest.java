package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class GameHubAccountOwnershipTest {
    @Test
    void loginHandoffDoesNotClaimButAnActiveGameSessionDoes() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession login = session(ClientSession.Endpoint.LOGIN, config, hub);
            ClientSession game = session(ClientSession.Endpoint.GAME, config, hub);
            ClientSession contender = session(ClientSession.Endpoint.GAME, config, hub);
            hub.connect(login);
            hub.connect(game);
            hub.connect(contender);

            assertTrue(hub.acceptAuthenticatedAccount(
                    login, ClientSession.Endpoint.LOGIN, "Test"));
            assertTrue(hub.acceptAuthenticatedAccount(
                    game, ClientSession.Endpoint.GAME, "test"));
            assertFalse(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "TEST"));

            hub.disconnect(game);
            assertTrue(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "test"));
        }
    }

    @Test
    void explicitEndpointTransferReleasesIdentityButNotOrdinaryDuplicateLogin()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession game = session(ClientSession.Endpoint.GAME, config, hub);
            ClientSession contender = session(ClientSession.Endpoint.GAME, config, hub);
            hub.connect(game);
            hub.connect(contender);
            assertTrue(hub.acceptAuthenticatedAccount(
                    game, ClientSession.Endpoint.GAME, "Test"));
            assertTrue(hub.selectCharacter(game, "TestKnight"));
            assertFalse(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "test"));

            assertFalse(hub.releaseAuthenticatedAccountForTransfer(game, "other"));
            assertTrue(hub.releaseAuthenticatedAccountForTransfer(game, "TEST"));
            assertTrue(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "test"));
            assertTrue(hub.selectCharacter(contender, "TestKnight"));
        }
    }

    private static ClientSession session(ClientSession.Endpoint endpoint,
                                         ServerConfig config, GameHub hub) {
        return new ClientSession(null, endpoint, config, null, null, LocalResources.discardCapture(), hub);
    }
}
