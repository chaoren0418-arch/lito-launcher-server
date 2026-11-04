package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameHubClassicWorldInstanceTest {
    @Test
    void sameMapMembershipIsStrictlyPartitionedByRetainedShowId()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession publicWorld = session(config, hub);
            ClientSession show100a = session(config, hub);
            ClientSession show100b = session(config, hub);
            ClientSession show101 = session(config, hub);
            hub.connect(publicWorld);
            hub.connect(show100a);
            hub.connect(show100b);
            hub.connect(show101);

            assertEquals(List.of(), hub.enterClassicWorld(
                    publicWorld, 2000, 32720, 32900, 2));
            assertEquals(List.of(), hub.enterClassicWorld(
                    show100a, 2000, 32721, 32853, 3, 100));
            assertEquals(List.of(show100a), hub.enterClassicWorld(
                    show100b, 2000, 32725, 32940, 1, 100));
            assertEquals(List.of(), hub.enterClassicWorld(
                    show101, 2000, 32810, 32941, 7, 101));

            assertEquals(List.of(publicWorld), hub.classicWorldMembers(2000));
            assertEquals(List.of(show100a, show100b),
                    hub.classicWorldMembers(2000, 100));
            assertEquals(List.of(show101), hub.classicWorldMembers(2000, 101));
            assertEquals(100, hub.classicWorldLocation(show100a).instanceId());
            assertEquals(101, hub.classicWorldLocation(show101).instanceId());

            hub.updateClassicWorld(show100a, 2000, 32720, 32900, 4);
            assertEquals(100, hub.classicWorldLocation(show100a).instanceId());

            assertEquals(List.of(show100b), hub.transitionClassicWorld(
                    show100a, 2000, 32877, 32901, 5, 100));
            assertEquals(new GameHub.ClassicWorldLocation(
                            2000, 32877, 32901, 5, 100),
                    hub.classicWorldLocation(show100a));
            assertEquals(List.of(show100a, show100b),
                    hub.classicWorldMembers(2000, 100));
        }
    }

    @Test
    void invalidPreNativeShowIdsAreRejectedWithoutMembershipMutation()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession session = session(config, hub);
            hub.connect(session);
            assertThrows(IllegalArgumentException.class, () ->
                    hub.enterClassicWorld(session, 2000,
                            32720, 32900, 2, 99));
            assertEquals(List.of(), hub.classicWorldMembers(2000, 100));
        }
    }

    private static ClientSession session(ServerConfig config, GameHub hub) {
        return new ClientSession(null, ClientSession.Endpoint.GAME,
                config, null, null, LocalResources.discardCapture(), hub);
    }
}
