package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionLitoClanRosterIntegrationTest {
    @TempDir Path temporary;

    @Test
    void encryptedRosterKeepsPaletteAndFinalActivityInNativeFields() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 2, InetAddress.getByName("127.0.0.1"));
             Socket ownerClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket ownerServer = listener.accept();
             Socket memberClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket memberServer = listener.accept();
             Socket preauthClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket preauthServer = listener.accept()) {
            ownerClient.setSoTimeout(1_000);
            memberClient.setSoTimeout(1_000);
            preauthClient.setSoTimeout(1_000);
            ClientSession owner = new ClientSession(
                    ownerServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("owner-capture")), hub);
            ClientSession member = new ClientSession(
                    memberServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("member-capture")), hub);
            ClientSession preauth = new ClientSession(
                    preauthServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("preauth-capture")), hub);
            configureCharacter(owner, "TestKnight", 1);
            configureCharacter(member, "TestMage", 3);
            hub.connect(owner);
            hub.connect(member);
            hub.connect(preauth);
            hub.registerOriginalClan(owner.sourceCharacterIdentity(), "Raiders");
            hub.registerOriginalClan(member.sourceCharacterIdentity(), "Raiders");
            hub.setClanRosterState(owner.sourceCharacterIdentity(), 0, 0, "Crown",
                    Protocol.LitoClanActivity.WAITING);
            hub.setClanRosterState(member.sourceCharacterIdentity(), 4, 5, "MageTitle",
                    Protocol.LitoClanActivity.OFFLINE);

            int seed = 0x3810_0697;
            setField(owner, "outgoing", new Protocol.CipherState(seed));
            setField(preauth, "outgoing", new Protocol.CipherState(seed + 1));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            assertTrue(owner.acceptsLitoRosterBroadcast());
            assertTrue(member.acceptsLitoRosterBroadcast());
            assertFalse(preauth.acceptsLitoRosterBroadcast());
            assertEquals(List.of(owner, member),
                    hub.connectedClanSessions(member.sourceCharacterIdentity()));

            invokeSendClanInfo(owner);
            assertMember(receive(ownerClient, receiver), "TestMage", 2,
                    4, 5, "MageTitle", Protocol.LitoClanActivity.OFFLINE);

            invokePublish(member, Protocol.LitoClanActivity.WAITING);
            assertMember(receive(ownerClient, receiver), "TestMage", 2,
                    0, 0, "", Protocol.LitoClanActivity.WAITING);

            invokePublish(member, Protocol.LitoClanActivity.FIGHTING);
            assertMember(receive(ownerClient, receiver), "TestMage", 2,
                    0, 0, "", Protocol.LitoClanActivity.FIGHTING);

            invokePublish(member, Protocol.LitoClanActivity.OFFLINE);
            assertMember(receive(ownerClient, receiver), "TestMage", 2,
                    0, 0, "", Protocol.LitoClanActivity.OFFLINE);
            assertEquals(0, preauthClient.getInputStream().available(),
                    "pre-version socket must not receive S_F6");
        }
    }

    @Test
    void roomOwnerTransferIgnoresAClosedSuccessorSocket() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("room-owner-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 2, InetAddress.getByName("127.0.0.1"));
             Socket ownerClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket ownerServer = listener.accept();
             Socket successorClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket successorServer = listener.accept()) {
            ClientSession owner = new ClientSession(
                    ownerServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("room-owner-capture")), hub);
            ClientSession successor = new ClientSession(
                    successorServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("room-successor-capture")), hub);
            configureCharacter(owner, "TestKnight", 1);
            configureCharacter(successor, "TestMage", 3);
            setField(successor, "outgoing", new Protocol.CipherState(0x3810_0700));
            hub.connect(owner);
            hub.connect(successor);
            hub.enterChannel(0, owner);
            hub.enterChannel(0, successor);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Raiders", "", 2,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
            hub.joinRoom(roomId, owner);
            hub.joinRoom(roomId, successor);

            successorServer.close();
            assertFalse(hub.leaveRoom(roomId, owner));
            assertEquals("TestMage", hub.roomOwnerName(roomId));
            assertDoesNotThrow(() -> invokeRoomOwnerBroadcast(owner, roomId));
        }
    }

    private static void assertMember(byte[] payload, String expectedName, int expectedRole,
                                     int expectedStyle, int expectedPalette,
                                     String expectedTitle,
                                     Protocol.LitoClanActivity expectedActivity) {
        Protocol.Reader in = new Protocol.Reader(payload);
        assertEquals(Protocol.S_LITO_CLAN_INFO, in.c());
        assertEquals("Raiders", in.s());
        int count = in.c();
        boolean found = false;
        for (int index = 0; index < count; index++) {
            String name = in.s();
            int role = in.h();
            int style = in.c();
            int palette = in.c();
            String title = in.s();
            int activity = in.c();
            if (!name.equals(expectedName)) continue;
            assertEquals(expectedRole, role);
            assertEquals(expectedStyle, style);
            assertEquals(expectedPalette, palette);
            assertEquals(expectedTitle, title);
            assertEquals(expectedActivity.wireValue(), activity);
            found = true;
        }
        assertEquals(true, found);
        assertEquals(0, in.remaining());
    }

    private static void configureCharacter(
            ClientSession session, String name, int type) throws Exception {
        setField(session, "selectedCharacter", name);
        setField(session, "currentChannelId", 0);
        setPhase(session, "CHANNEL");
        setField(session, "characters", List.of(new CharacterSummary(
                name, "Raiders", type, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0)));
    }

    private static void invokeSendClanInfo(ClientSession session) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("sendClanInfo");
        method.setAccessible(true);
        method.invoke(session);
    }

    private static void invokePublish(
            ClientSession session, Protocol.LitoClanActivity activity) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "publishClanRosterState", Protocol.LitoClanActivity.class, String.class);
        method.setAccessible(true);
        method.invoke(session, activity, "integration");
    }

    private static void invokeRoomOwnerBroadcast(ClientSession session, long roomId)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "broadcastRoomOwner", long.class, String.class);
        method.setAccessible(true);
        method.invoke(session, roomId, "closed-successor-test");
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setPhase(ClientSession session, String phase) throws Exception {
        Field field = ClientSession.class.getDeclaredField("phase");
        field.setAccessible(true);
        Class<? extends Enum> phaseType = (Class<? extends Enum>) field.getType();
        field.set(session, Enum.valueOf(phaseType, phase));
    }
}
