package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionLitoObserverStateIntegrationTest {
    @TempDir Path temporary;

    @Test void encryptedAllowedPhasesRoundTripTypedStatesAndBattleRejectsWithoutE5()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("capture")), hub);

            int seed = 0x3810_0693;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            setPhase(session, "LOBBY");
            invokeSetObserver(session, bytes(0xE1, 1));
            assertTrue(session.isObserver());
            assertArrayEquals(bytes(0xE5, 1), receive(client, receiver));

            setPhase(session, "CHANNEL");
            invokeSetObserver(session, bytes(0xE1, 0));
            assertFalse(session.isObserver());
            assertArrayEquals(bytes(0xE5, 0), receive(client, receiver));

            setPhase(session, "ROOM");
            invokeSetObserver(session, bytes(0xE1, 1));
            assertTrue(session.isObserver());
            assertArrayEquals(bytes(0xE5, 1), receive(client, receiver));
            invokeSetObserver(session, bytes(0xE1, 0));
            assertFalse(session.isObserver());
            assertArrayEquals(bytes(0xE5, 0), receive(client, receiver));

            setPhase(session, "BATTLE");
            invokeSetObserver(session, bytes(0xE1, 1));
            assertFalse(session.isObserver());
            client.setSoTimeout(100);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        }
    }

    private static void invokeSetObserver(ClientSession session, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("setObserver", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
