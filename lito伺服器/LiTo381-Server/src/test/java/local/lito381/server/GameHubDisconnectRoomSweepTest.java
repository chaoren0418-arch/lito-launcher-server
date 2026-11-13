package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T124 (#355): found by {@code BattleDisconnectStressMySqlIntegrationTest}. ClientSession creates a
 * room with {@code hub.createRoom} and joins it afterwards; a disconnect of an unrelated player in
 * between used to sweep every empty room and delete the new one ("unknown room" on join).
 */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class GameHubDisconnectRoomSweepTest {
    @TempDir Path temporary;
    private final List<Socket> sockets = new ArrayList<>();

    @Test
    void unrelatedDisconnectKeepsAJustCreatedEmptyRoomButRemovesItsOwnRoom() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(temporary.resolve("sweep.properties"), false);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state, accounts)) {
            ClientSession creator = session(config, accounts, state, hub, "creator");
            ClientSession bystander = session(config, accounts, state, hub, "bystander");
            ClientSession leaver = session(config, accounts, state, hub, "leaver");

            // The bystander sits in its own room; the leaver is only in the channel.
            long bystanderRoom = hub.createRoom(0, new GameHub.RoomSettings("Bystander", "", 2,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
            assertTrue(hub.tryJoinRoom(bystanderRoom, bystander).joined());

            // creator: hub.createRoom(...) — the room has no member yet ...
            long created = hub.createRoom(0, new GameHub.RoomSettings("Fresh", "", 2,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
            // ... an unrelated session closes its socket at this moment ...
            hub.disconnect(leaver);
            assertTrue(rooms(hub).containsKey(created), "an unrelated disconnect deleted the new room");
            // ... and the creator's synthetic C_LITO_JOIN still succeeds.
            assertTrue(hub.tryJoinRoom(created, creator).joined());

            // The departing member's own room is still cleaned up as before.
            hub.disconnect(bystander);
            assertFalse(rooms(hub).containsKey(bystanderRoom), "the departed member's empty room is removed");
            assertTrue(rooms(hub).containsKey(created));
            hub.disconnect(creator);
            assertFalse(rooms(hub).containsKey(created));
        } finally {
            for (Socket s : sockets) s.close();
        }
    }

    private ClientSession session(ServerConfig config, AccountRepository accounts, GameStateStore state,
            GameHub hub, String name) throws Exception {
        Socket client;
        Socket server;
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            client = new Socket("127.0.0.1", listener.getLocalPort());
            server = listener.accept();
        }
        sockets.add(client);
        sockets.add(server);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME, config, accounts, state,
                new CaptureLog(temporary.resolve("capture-" + name)), hub);
        hub.connect(session);
        hub.enterChannel(0, session);
        return session;
    }

    private static Map<?, ?> rooms(GameHub hub) throws Exception {
        Field field = GameHub.class.getDeclaredField("rooms");
        field.setAccessible(true);
        synchronized (hub) {
            return Map.copyOf((Map<?, ?>) field.get(hub));
        }
    }
}
