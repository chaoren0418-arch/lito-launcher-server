package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionTransferStoneProbeTest {
    @TempDir Path temporary;

    @Test void exactUseReturnsOnlyTheCurrentAuthoritativeLoopbackDirectory()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(200);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("capture")), new GameHub(config));
            int seed = 0x3810_0591;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, request);

            byte[] expected = Protocol.litoServerDirectory(List.of(), List.of(
                    new Protocol.LitoVsServerDirectoryEntry(
                            config.serverName(),
                            ipv4LittleEndian(config.advertisedHost()),
                            config.advertisedPort(), 0, 100)));
            assertArrayEquals(expected, receiver.decrypt(
                    Protocol.readFrame(client.getInputStream(), 8_192)));
            assertEquals(Protocol.S_LITO_SERVER_LIST, expected[0] & 0xFF);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        }
    }

    @Test void nonEmptyArgumentDoesNotOpenTheProbeDirectory() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("rejected-state.properties"), false);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(200);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("rejected-capture")),
                    new GameHub(config));
            setField(session, "outgoing", new Protocol.CipherState(0x3810_0592));

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("unexpected").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, request);

            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        }
    }

    @Test void nativeD9ReleasesOnlyTheExplicitProbeHandoffIdentity()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("handoff-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(500);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("handoff-capture")), hub);
            ClientSession contender = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, null, null, hub);
            hub.connect(session);
            hub.connect(contender);
            setField(session, "account", "test");
            assertTrue(hub.acceptAuthenticatedAccount(
                    session, ClientSession.Endpoint.GAME, "test"));
            assertTrue(hub.selectCharacter(session, "TestKnight"));
            int seed = 0x3810_0593;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, request);
            assertEquals(Protocol.S_LITO_SERVER_LIST,
                    receiver.decrypt(Protocol.readFrame(
                            client.getInputStream(), 8_192))[0] & 0xFF);

            invoke(session, "quitFight", new Class<?>[0]);
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192)));
            assertTrue(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "TEST"));
            assertTrue(hub.selectCharacter(contender, "TestKnight"));
        }
    }

    @Test void newEndpointAtomicallyClaimsAfterDirectoryWasOfferedEvenIfD9IsLost()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("atomic-handoff-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(500);
            ClientSession active = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("atomic-handoff-capture")), hub);
            ClientSession contender = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, null, null, hub);
            hub.connect(active);
            hub.connect(contender);
            setField(active, "account", "test");
            assertTrue(hub.acceptAuthenticatedAccount(
                    active, ClientSession.Endpoint.GAME, "test"));
            assertTrue(hub.selectCharacter(active, "TestKnight"));
            org.junit.jupiter.api.Assertions.assertFalse(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "test"));
            int seed = 0x3810_0594;
            setField(active, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("").bytes();
            invoke(active, "useInventoryItem", new Class<?>[] {byte[].class}, request);
            assertEquals(Protocol.S_LITO_SERVER_LIST,
                    receiver.decrypt(Protocol.readFrame(
                            client.getInputStream(), 8_192))[0] & 0xFF);

            assertTrue(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "TEST"));
            assertTrue(hub.selectCharacter(contender, "TestKnight"));
        }
    }

    @Test void nativeD9ReleasesIdentityEvenWhenOldEndpointResetBeforeReply()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.transfer-stone-probe.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("reset-handoff-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession active = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("reset-handoff-capture")), hub);
            ClientSession contender = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, null, null, hub);
            hub.connect(active);
            hub.connect(contender);
            setField(active, "account", "test");
            assertTrue(hub.acceptAuthenticatedAccount(
                    active, ClientSession.Endpoint.GAME, "test"));
            assertTrue(hub.selectCharacter(active, "TestKnight"));
            setField(active, "outgoing", new Protocol.CipherState(0x3810_0595));

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("").bytes();
            invoke(active, "useInventoryItem", new Class<?>[] {byte[].class}, request);
            server.close();

            invoke(active, "quitFight", new Class<?>[0]);
            assertTrue(hub.acceptAuthenticatedAccount(
                    contender, ClientSession.Endpoint.GAME, "TEST"));
            assertTrue(hub.selectCharacter(contender, "TestKnight"));
        }
    }

    private static long ipv4LittleEndian(String host) throws Exception {
        byte[] address = InetAddress.getByName(host).getAddress();
        return Integer.toUnsignedLong((address[0] & 0xFF)
                | ((address[1] & 0xFF) << 8)
                | ((address[2] & 0xFF) << 16)
                | ((address[3] & 0xFF) << 24));
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
