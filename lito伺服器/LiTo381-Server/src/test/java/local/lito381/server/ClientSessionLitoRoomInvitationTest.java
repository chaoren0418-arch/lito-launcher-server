package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientSessionLitoRoomInvitationTest {
    @TempDir Path temporary;

    @Test void nativeS_D7D6CfChainUsesOneShotServerCapability() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket inviterListener = listener();
             Socket inviterClient = new Socket("127.0.0.1", inviterListener.getLocalPort());
             Socket inviterServer = inviterListener.accept();
             ServerSocket targetListener = listener();
             Socket targetClient = new Socket("127.0.0.1", targetListener.getLocalPort());
             Socket targetServer = targetListener.accept()) {
            inviterClient.setSoTimeout(150);
            targetClient.setSoTimeout(1_000);
            ClientSession inviter = session(
                    inviterServer, config, state, hub, "inviter-capture");
            ClientSession target = session(
                    targetServer, config, state, hub, "target-capture");
            prepareChannelMember(inviter, "RoomOwner", 0);
            prepareChannelMember(target, "Invitee", 0);
            hub.connect(inviter);
            hub.connect(target);
            hub.enterChannel(0, inviter);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Private Arena", "381", 4,
                    GameHub.RoomSettings.ALLOW_UPPER_TIER,
                    0, 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, inviter).joined());
            setPhase(inviter, "ROOM");
            setField(inviter, "currentRoomId", roomId);

            int inviterSeed = 0x3810_0612;
            int targetSeed = 0x3810_0613;
            setField(inviter, "outgoing", new Protocol.CipherState(inviterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState inviterReceiver = new Protocol.CipherState(inviterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            byte[] staleAccept = new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                    .s("RoomOwner").c(1).bytes();
            invoke(target, "inviteReply", new Class<?>[] {byte[].class}, staleAccept);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(inviterClient.getInputStream(), 8_192));
            assertNull(field(target, "pendingLitoRoomInvitation"));
            assertNull(field(target, "acceptedLitoRoomInvitation"));

            byte[] request = new Protocol.Writer().c(Protocol.C_LITO_INVITE_USER)
                    .s("Invitee").bytes();
            invoke(inviter, "inviteUser", new Class<?>[] {byte[].class}, request);
            assertArrayEquals(Protocol.roomInvitation(
                            inviter.litoDisplayName(), roomId, "381"),
                    receive(targetClient, targetReceiver));
            assertNotNull(field(target, "pendingLitoRoomInvitation"));

            byte[] accept = new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                    .s(inviter.litoDisplayName()).c(1).bytes();
            invoke(target, "inviteReply", new Class<?>[] {byte[].class}, accept);
            assertArrayEquals(Protocol.roomInvitationReply(
                            target.litoDisplayName(),
                            Protocol.LitoInvitationReply.ACCEPTED),
                    receive(inviterClient, inviterReceiver));
            assertNull(field(target, "pendingLitoRoomInvitation"));
            assertNotNull(field(target, "acceptedLitoRoomInvitation"));

            // A replay has no matching S_D7 and cannot notify the owner again
            // or consume the already accepted CF capability.
            invoke(target, "inviteReply", new Class<?>[] {byte[].class}, accept);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(inviterClient.getInputStream(), 8_192));
            assertNotNull(field(target, "acceptedLitoRoomInvitation"));

            byte[] join = new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(roomId).s("381").bytes();
            invoke(target, "joinRoom", new Class<?>[] {byte[].class, String.class},
                    join, "direct");
            Protocol.Reader result = new Protocol.Reader(receive(targetClient, targetReceiver));
            assertEquals(Protocol.S_LITO_JOIN_RESULT, result.c());
            assertEquals(0, result.c());
            assertEquals(roomId, result.d());
            assertEquals("ROOM", field(target, "phase").toString());
            assertNull(field(target, "acceptedLitoRoomInvitation"));
            assertTrue(hub.roomMembers(roomId).contains(target));
            CaptureLog targetCapture = (CaptureLog) field(target, "capture");
            assertTrue(Files.readString(targetCapture.path()).contains(
                    "S_LITO_JOIN_RESULT accepted room=" + roomId
                            + " trigger=invite-accept"));
        }
    }

    @Test void mismatchedCfConsumesAcceptedCapabilityWithoutGrantingPasswordBypass()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("mismatch-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket inviterListener = listener();
             Socket inviterClient = new Socket("127.0.0.1", inviterListener.getLocalPort());
             Socket inviterServer = inviterListener.accept();
             ServerSocket targetListener = listener();
             Socket targetClient = new Socket("127.0.0.1", targetListener.getLocalPort());
             Socket targetServer = targetListener.accept()) {
            inviterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession inviter = session(
                    inviterServer, config, state, hub, "mismatch-inviter-capture");
            ClientSession target = session(
                    targetServer, config, state, hub, "mismatch-target-capture");
            prepareChannelMember(inviter, "RoomOwner", 0);
            prepareChannelMember(target, "Invitee", 0);
            hub.connect(inviter);
            hub.connect(target);
            hub.enterChannel(0, inviter);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Private Arena", "381", 4,
                    GameHub.RoomSettings.ALLOW_UPPER_TIER,
                    0, 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, inviter).joined());
            setPhase(inviter, "ROOM");
            setField(inviter, "currentRoomId", roomId);

            int inviterSeed = 0x3810_0614;
            int targetSeed = 0x3810_0615;
            setField(inviter, "outgoing", new Protocol.CipherState(inviterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState inviterReceiver = new Protocol.CipherState(inviterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            invoke(inviter, "inviteUser", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_INVITE_USER)
                            .s("Invitee").bytes());
            receive(targetClient, targetReceiver);
            invoke(target, "inviteReply", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_INVITE_REPLY)
                            .s(inviter.litoDisplayName()).c(1).bytes());
            receive(inviterClient, inviterReceiver);
            assertNotNull(field(target, "acceptedLitoRoomInvitation"));

            byte[] wrongPassword = new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(roomId).s("wrong").bytes();
            invoke(target, "joinRoom", new Class<?>[] {byte[].class, String.class},
                    wrongPassword, "direct");
            assertArrayEquals(
                    Protocol.joinRejected(Protocol.LitoJoinFailure.BAD_PASSWORD),
                    receive(targetClient, targetReceiver));
            assertNull(field(target, "acceptedLitoRoomInvitation"));
            assertEquals("CHANNEL", field(target, "phase").toString());
        }
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub,
                                  String captureDirectory) throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config, null, state,
                new CaptureLog(temporary.resolve(captureDirectory)), hub);
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static void prepareChannelMember(
            ClientSession session, String name, int channelId) throws Exception {
        setPhase(session, "CHANNEL");
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(new CharacterSummary(
                name, "", 1, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0)));
        setField(session, "currentChannelId", channelId);
        setField(session, "hubChannelId", channelId);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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
