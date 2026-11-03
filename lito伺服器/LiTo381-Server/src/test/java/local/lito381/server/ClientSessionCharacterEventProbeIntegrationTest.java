package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionCharacterEventProbeIntegrationTest {
    private static final int SEED = 0x0404_C8C8;
    private static final long LOOPBACK_127_0_0_2 = 0x0200_007FL;

    @TempDir Path temporary;

    @Test void emitsS04AfterSourceDirectoryAndWaitsForItsCc8Callback()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-event-probe.properties"));
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog capture = new CaptureLog(temporary.resolve("captures"))) {
            client.setSoTimeout(100);
            GameStateStore state = new GameStateStore(
                    temporary.resolve("state.properties"), false);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.LOGIN, config,
                    accounts, state, capture, hub);
            setField(session, "outgoing", new Protocol.CipherState(SEED));
            setEnumField(session, "phase", "VERSION_OK");
            hub.connect(session);
            try {
                invoke(session, "login", new Protocol.Writer()
                        .c(Protocol.C_LOGIN).s("test").s("test")
                        .d(0x0100_007FL).bytes());
                Protocol.CipherState receiver = new Protocol.CipherState(SEED);
                List<byte[]> bootstrap = packets(client, receiver);

                assertEquals(3, bootstrap.size());
                assertEquals(Protocol.S_LOGIN_CHECK, bootstrap.get(0)[0] & 0xFF);
                assertEquals(Protocol.S_SERVER_LIST, bootstrap.get(1)[0] & 0xFF);
                assertArrayEquals(Protocol.specialCharacterServerEvent(), bootstrap.get(2));
                assertEquals("AUTHENTICATED", field(session, "phase").toString());

                invoke(session, "selectClassicServer",
                        new byte[] {(byte) Protocol.C_SERVER_SELECT});
                assertTrue(packets(client, receiver).isEmpty(),
                        "fixture-only C35 deferral must not redirect before C_C8");
                assertEquals("AUTHENTICATED", field(session, "phase").toString());

                String sourceName = config.classicServerNames().get(0);
                invoke(session, "requestLitoCharacterInfo", new Protocol.Writer()
                        .c(Protocol.C_LITO_LIN_CHARACTER_INFO).s(sourceName).bytes());
                List<byte[]> redirect = packets(client, receiver);
                assertEquals(1, redirect.size());
                assertArrayEquals(Protocol.litoVsServerIp(
                        0, LOOPBACK_127_0_0_2, 32100), redirect.get(0));
                assertEquals(sourceName, hub.pendingClassicSourceHandoff("test"));
                assertEquals("LOBBY", field(session, "phase").toString());
            } finally {
                hub.disconnect(session);
            }
        }
    }

    @Test void sourceUiFixtureRefreshesTheDirectoryOnceWithoutS04OrRedirect()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.native-source-ui-probe.properties"));
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("source-ui-captures"))) {
            client.setSoTimeout(100);
            GameStateStore state = new GameStateStore(
                    temporary.resolve("source-ui-state.properties"), false);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.LOGIN, config,
                    accounts, state, capture, hub);
            setField(session, "outgoing", new Protocol.CipherState(SEED));
            setField(session, "account", "test");
            setEnumField(session, "phase", "AUTHENTICATED");
            hub.connect(session);
            try {
                Protocol.CipherState receiver = new Protocol.CipherState(SEED);
                invoke(session, "selectClassicServer",
                        new byte[] {(byte) Protocol.C_SERVER_SELECT});
                List<byte[]> refresh = packets(client, receiver);
                assertEquals(1, refresh.size());
                assertEquals(Protocol.S_SERVER_LIST, refresh.get(0)[0] & 0xFF);
                assertEquals("AUTHENTICATED", field(session, "phase").toString());

                invoke(session, "selectClassicServer",
                        new byte[] {(byte) Protocol.C_SERVER_SELECT});
                List<byte[]> fallback = packets(client, receiver);
                assertEquals(1, fallback.size());
                assertEquals(Protocol.S_LITO_VS_SERVER_IP,
                        fallback.get(0)[0] & 0xFF);
                assertEquals("LOBBY", field(session, "phase").toString());
            } finally {
                hub.disconnect(session);
            }
        }
    }

    private static void invoke(ClientSession session, String methodName, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(methodName, byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) payload);
    }

    private static Object field(ClientSession session, String name) throws Exception {
        Field field = ClientSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(session);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }

    private static List<byte[]> packets(Socket client, Protocol.CipherState receiver)
            throws Exception {
        List<byte[]> result = new ArrayList<>();
        while (true) {
            try {
                byte[] encrypted = Protocol.readFrame(client.getInputStream(), 8192);
                if (encrypted == null) break;
                result.add(receiver.decrypt(encrypted));
            } catch (SocketTimeoutException timeout) {
                break;
            }
        }
        return result;
    }
}
