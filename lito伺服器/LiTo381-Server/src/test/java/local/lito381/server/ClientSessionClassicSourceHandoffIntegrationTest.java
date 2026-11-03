package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicSourceHandoffIntegrationTest {
    private static final int LOGIN_SEED = 0x3711_0001;
    private static final int GAME_SEED = 0x3711_0002;
    private static final long LOOPBACK_127_0_0_2 = 0x0200_007FL;

    @TempDir Path temporary;

    @Test
    void interactiveCc8RedirectsOnceAndGameLoginClaimsTheSelectedNonFirstSource()
            throws Exception {
        ServerConfig config = twoSourceConfig();
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config)) {
            GameStateStore state = new GameStateStore(
                    temporary.resolve("state.properties"), false);
            try (Fixture login = fixture(ClientSession.Endpoint.LOGIN, LOGIN_SEED,
                    config, accounts, state, hub, "login-captures")) {
                login.authenticate();
                login.packets();

                login.invoke("requestLitoCharacterInfo", new Protocol.Writer()
                        .c(Protocol.C_LITO_LIN_CHARACTER_INFO).s("Moon").bytes());

                List<byte[]> redirect = login.packets();
                assertEquals(1, redirect.size(),
                        "LOGIN C_C8 must not leak a character directory on the old socket");
                assertArrayEquals(Protocol.litoVsServerIp(
                        0, LOOPBACK_127_0_0_2, config.advertisedPort()), redirect.get(0));
                assertEquals("Moon", hub.pendingClassicSourceHandoff("test"));
                assertEquals("LOBBY", login.field("phase").toString());

                try (Fixture game = fixture(ClientSession.Endpoint.GAME, GAME_SEED,
                        config, accounts, state, hub, "game-captures")) {
                    game.authenticate();
                    List<byte[]> bootstrap = game.packets();

                    assertEquals("Moon", game.field("selectedClassicServerName"));
                    assertEquals("AUTHENTICATED", game.field("phase").toString());
                    assertNull(hub.pendingClassicSourceHandoff("test"),
                            "GAME authentication must consume the one-shot handoff");
                    assertEquals(1, ((List<?>) game.field("characters")).size(),
                            "the selected source must load its one-character memory fixture");
                    assertEquals(1, bootstrap.size(),
                            "GAME authentication must stop at the native S_E6 readiness barrier");
                    assertArrayEquals(Protocol.litoNewsTrigger(), bootstrap.get(0));

                    game.invoke("acknowledgeLitoNews",
                            new byte[] {(byte) Protocol.C_LITO_READ_NEWS});
                    List<byte[]> directory = game.packets();
                    assertEquals(2, directory.size(),
                            "C_E2 releases S_03 followed by the selected source's S_CB row");
                    assertEquals(Protocol.S_NUM_CHARACTER, directory.get(0)[0] & 0xFF);
                    assertEquals(Protocol.S_LITO_CHARACTER_INFO,
                            directory.get(1)[0] & 0xFF);
                    assertEquals("LOBBY", game.field("phase").toString());
                }
            }
        }
    }

    @Test
    void sameCharacterPageSourceSwitchReloadsEachSelectedDatabaseProjection()
            throws Exception {
        ServerConfig config = twoSourceConfig();
        Map<String, AccountRepository> routes = new LinkedHashMap<>();
        routes.put("Sun", new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52));
        routes.put("Moon", new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 190, 88));
        try (AccountRepository accounts = new SourceRoutedAccountRepository(routes);
             GameHub hub = new GameHub(config)) {
            GameStateStore state = new GameStateStore(
                    temporary.resolve("same-page-source-switch-state.properties"), false);
            try (Fixture game = fixture(ClientSession.Endpoint.GAME, GAME_SEED,
                    config, accounts, state, hub, "same-page-source-switch-captures")) {
                game.authenticate();
                game.packets();
                game.invoke("acknowledgeLitoNews",
                        new byte[] {(byte) Protocol.C_LITO_READ_NEWS});
                game.packets();

                game.invoke("requestLitoCharacterInfo", new Protocol.Writer()
                        .c(Protocol.C_LITO_LIN_CHARACTER_INFO).s("Moon").bytes());
                List<byte[]> moonPackets = game.packets();
                assertEquals(2, moonPackets.size());
                CharacterSummary moon = ((List<CharacterSummary>) game.field("characters")).get(0);
                assertEquals("Moon", game.field("selectedClassicServerName"));
                assertEquals(88, moon.level());
                assertEquals(190, moon.food());

                game.invoke("requestLitoCharacterInfo", new Protocol.Writer()
                        .c(Protocol.C_LITO_LIN_CHARACTER_INFO).s("Sun").bytes());
                List<byte[]> sunPackets = game.packets();
                assertEquals(2, sunPackets.size());
                CharacterSummary sun = ((List<CharacterSummary>) game.field("characters")).get(0);
                assertEquals("Sun", game.field("selectedClassicServerName"));
                assertEquals(52, sun.level());
                assertEquals(40, sun.food());
                assertEquals(Protocol.S_NUM_CHARACTER, sunPackets.get(0)[0] & 0xFF);
                assertEquals(Protocol.S_LITO_CHARACTER_INFO, sunPackets.get(1)[0] & 0xFF);
            }
        }
    }

    private ServerConfig twoSourceConfig() throws Exception {
        String source = Files.readString(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"), StandardCharsets.UTF_8);
        source = source.replace(
                "login.classic.server.names=LiTo 3.81C Isolated Self-Test",
                "login.classic.server.names=Sun,Moon");
        Path path = temporary.resolve("server.two-source.properties");
        Files.writeString(path, source, StandardCharsets.UTF_8);
        return ServerConfig.load(path);
    }

    private Fixture fixture(ClientSession.Endpoint endpoint, int seed,
                            ServerConfig config, AccountRepository accounts,
                            GameStateStore state, GameHub hub, String captureName)
            throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve(captureName));
        ClientSession session = new ClientSession(
                server, endpoint, config, accounts, state, capture, hub);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "VERSION_OK");
        hub.connect(session);
        return new Fixture(client, server, session,
                new Protocol.CipherState(seed), capture, hub);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(Socket client, Socket server, ClientSession session,
                           Protocol.CipherState receiver, CaptureLog capture,
                           GameHub hub) implements AutoCloseable {
        void authenticate() throws Exception {
            invoke("login", new Protocol.Writer().c(Protocol.C_LOGIN)
                    .s("test").s("test").d(0x0100_007FL).bytes());
        }

        void invoke(String methodName, byte[] payload) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    methodName, byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) payload);
        }

        Object field(String name) throws Exception {
            Field field = ClientSession.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(session);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 8192);
                    if (encrypted == null) break;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException timeout) {
                    break;
                }
            }
            return result;
        }

        @Override public void close() throws Exception {
            hub.disconnect(session);
            client.close();
            server.close();
            capture.close();
        }
    }
}
