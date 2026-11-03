package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionLitoBuddyPresenceIntegrationTest {
    @TempDir Path temporary;

    @Test
    void listPresenceAndActivityUseNativeFieldsAcrossChannels() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 2, InetAddress.getByName("127.0.0.1"));
             Socket recipientClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket recipientServer = listener.accept();
             Socket senderClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket senderServer = listener.accept()) {
            recipientClient.setSoTimeout(1_000);
            senderClient.setSoTimeout(1_000);
            ClientSession recipient = new ClientSession(
                    recipientServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("recipient-capture")), hub);
            ClientSession sender = new ClientSession(
                    senderServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("sender-capture")), hub);
            configureCharacter(recipient, "TestKnight");
            configureCharacter(sender, "TestMage");
            hub.connect(recipient);
            hub.connect(sender);
            setField(recipient, "currentChannelId", 1);
            setField(recipient, "hubChannelId", 1);
            setField(sender, "currentChannelId", 0);
            setField(sender, "hubChannelId", 0);
            hub.enterChannel(1, recipient);
            hub.enterChannel(0, sender);

            assertEquals(GameHub.BuddyAddResult.ADDED, hub.addBuddy(
                    recipient.sourceCharacterIdentity(), "TestMage", "Local", false));

            int seed = 0x3810_0695;
            setField(recipient, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invokeBuddyList(recipient,
                    hub.buddyData(recipient.sourceCharacterIdentity(), 1));
            assertOfflineRow(receive(recipientClient, receiver), "TestMage");

            invokePresence(sender, Protocol.LitoBuddyPresence.ONLINE);
            assertArrayEquals(Protocol.buddyLogin(
                            "TestMage", Protocol.LitoBuddyPresence.ONLINE),
                    receive(recipientClient, receiver));
            assertArrayEquals(Protocol.buddyStatus(
                            "TestMage", Protocol.LitoBuddyActivity.WAITING),
                    receive(recipientClient, receiver));

            invokeActivity(sender, Protocol.LitoBuddyActivity.FIGHTING);
            assertArrayEquals(Protocol.buddyStatus(
                            "TestMage", Protocol.LitoBuddyActivity.FIGHTING),
                    receive(recipientClient, receiver));

            invokePresence(sender, Protocol.LitoBuddyPresence.OFFLINE);
            assertArrayEquals(Protocol.buddyLogin(
                            "TestMage", Protocol.LitoBuddyPresence.OFFLINE),
                    receive(recipientClient, receiver));
            assertArrayEquals(Protocol.buddyStatus(
                            "TestMage", Protocol.LitoBuddyActivity.OFFLINE),
                    receive(recipientClient, receiver));
        }
    }

    private static void assertOfflineRow(byte[] payload, String expectedName) {
        Protocol.Reader in = new Protocol.Reader(payload);
        assertEquals(Protocol.S_LITO_BUDDY_LIST, in.c());
        int count = in.c();
        boolean found = false;
        for (int index = 0; index < count; index++) {
            String name = in.s();
            in.h();
            in.c();
            int paletteOffset = in.c();
            in.s();
            int activity = in.c();
            if (name.equals(expectedName)) {
                assertEquals(Protocol.LitoBuddyActivity.OFFLINE.wireValue(), activity);
                assertEquals(1, paletteOffset);
                found = true;
            }
        }
        assertEquals(true, found);
        assertEquals(0, in.remaining());
    }

    private static void configureCharacter(ClientSession session, String name) throws Exception {
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(new CharacterSummary(
                name, "", 1, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0)));
    }

    private static void invokePresence(
            ClientSession session, Protocol.LitoBuddyPresence presence) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "notifyBuddyPresence", Protocol.LitoBuddyPresence.class);
        method.setAccessible(true);
        method.invoke(session, presence);
    }

    private static void invokeActivity(
            ClientSession session, Protocol.LitoBuddyActivity activity) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "notifyBuddyActivity", Protocol.LitoBuddyActivity.class);
        method.setAccessible(true);
        method.invoke(session, activity);
    }

    private static void invokeBuddyList(
            ClientSession session, GameHub.BuddyData data) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "sendBuddyList", GameHub.BuddyData.class);
        method.setAccessible(true);
        method.invoke(session, data);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
