package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

final class ClientSessionLitoChannelSelectionIntegrationTest {
    @TempDir Path temporary;

    @Test void encryptedMalformedAndUnproducibleValuesStayAlignedBeforeValidEntry()
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
            hub.connect(session);
            setPhase(session, "LOBBY");
            setField(session, "selectedCharacter", "ChannelUser");
            setField(session, "characters", List.of(character("ChannelUser")));

            int seed = 0x3810_0694;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invokeHandle(session, bytes(0xCA, 4));
            assertArrayEquals(Protocol.litoMalformedEnterChannelFailure(),
                    receive(client, receiver));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));

            invokeHandle(session, bytes(0xCA, 0, 0));
            assertArrayEquals(Protocol.litoMalformedEnterChannelFailure(),
                    receive(client, receiver));
            assertEquals("LOBBY", field(session, "phase").toString());

            invokeHandle(session, bytes(0xCA, 0));
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoChannelInfo(
                            Protocol.LitoChannelSelection.CHANNEL_0, 1, 100),
                    receive(client, receiver));
            assertEquals("CHANNEL", field(session, "phase").toString());
            assertEquals(0, field(session, "currentChannelId"));

            invokeHandle(session, bytes(Protocol.C_LITO_LEAVE_CHANNEL));
            assertEquals("LOBBY", field(session, "phase").toString());
            invokeHandle(session, bytes(Protocol.C_LITO_ENTER_CHANNEL, 100));
            assertArrayEquals(Protocol.allowPostTransition(),
                    receive(client, receiver));
            assertEquals("RESTART_AWAIT_SOURCE_REFRESH",
                    field(session, "pendingTransition").toString());
        }
    }

    @Test void nativeLeaveChannelAndQuitRequestsMutateOnlyTheirOwnedState()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("navigation-state.properties"), false);
        try (GameHub hub = new GameHub(config)) {
            ClientSession session = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, state,
                    null, hub);
            hub.connect(session);
            setPhase(session, "CHANNEL");
            setField(session, "account", "NavigationUser");
            setField(session, "selectedCharacter", "NavigationUser");
            setField(session, "characters", List.of(character("NavigationUser")));
            setField(session, "currentChannelId", 0);
            setField(session, "hubChannelId", 0);
            hub.enterChannel(0, session);

            // C_CB leaves the lobby. The subsequent C_CA100 continuation is
            // covered on an encrypted socket above because it must now answer
            // with native S_F5 and cannot be exercised by this socketless unit.
            invokeHandle(session, bytes(Protocol.C_LITO_LEAVE_CHANNEL));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));
            assertEquals(-1, field(session, "hubChannelId"));
            assertEquals(0, hub.channelSnapshot(0).users());

            // The native return-to-character/account path owns its local UI
            // reset and emits opcode-only C_QUIT.  The server must arm a clean
            // connection close without changing that packet into a room exit.
            invokeHandle(session, bytes(Protocol.C_QUIT));
            assertEquals(true, field(session, "quitRequested"));
        }
    }

    @Test void nativeChannelButtonSelectsAnIndependentLobbyOnTheSharedGamePort()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("channel-port-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession source = new ClientSession(
                    server, ClientSession.Endpoint.GAME, 0,
                    config, null, state,
                    new CaptureLog(temporary.resolve("channel-port-capture")), hub);
            hub.connect(source);
            setPhase(source, "LOBBY");
            setField(source, "account", "PortUser");
            setField(source, "selectedCharacter", "PortUser");
            setField(source, "characters", List.of(character("PortUser")));
            int seed = 0x3810_0701;
            setField(source, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invokeHandle(source, bytes(Protocol.C_LITO_ENTER_CHANNEL, 2));
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_2),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoChannelInfo(
                            Protocol.LitoChannelSelection.CHANNEL_2, 1, 100),
                    receive(client, receiver));
            assertEquals("CHANNEL", field(source, "phase").toString());
            assertEquals(2, field(source, "currentChannelId"));
            assertEquals(1, hub.channelSnapshot(2).users());
            assertEquals(0, hub.channelSnapshot(0).users());
            assertEquals(null, hub.pendingChannelPortHandoff("PortUser", 2));
        }
    }

    @Test void nativeLobbyExitQuitRehydratesCachedCharacterBeforeChannelReentry()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("lobby-exit-resume-state.properties"), false);
        String sourceName = config.classicServerNames().get(0);
        try (GameHub hub = new GameHub(config);
             AccountRepository accounts = AccountRepository.open(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession departing = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, state,
                    null, hub);
            hub.connect(departing);
            setPhase(departing, "CHANNEL");
            setField(departing, "account", "test");
            setField(departing, "selectedClassicServerName", sourceName);
            setField(departing, "selectedCharacter", "TestKnight");
            setField(departing, "currentChannelId", 0);
            setField(departing, "hubChannelId", 0);
            hub.enterChannel(0, departing);

            // Exact live trace: lobby Exit emits C_D0 then C_QUIT and the next
            // GAME connection reaches C_CA without replaying C_C9.
            assertEquals(null, hub.pendingChannelPortHandoff("test", 0));
            invokeHandle(departing, bytes(Protocol.C_LITO_EXIT_ROOM));
            // The captured 3.81C client took 6.296 seconds between these two
            // frames while replacing the lobby with its channel page.  Keep
            // that real transition in the regression so a five-second gate
            // cannot be reintroduced unnoticed.
            Thread.sleep(6_500L);
            assertTrue((long) field(departing,
                    "nativeLobbyExitResumeDeadlineNanos") - System.nanoTime() > 0);
            invokeHandle(departing, bytes(Protocol.C_QUIT));
            assertTrue((boolean) field(departing, "quitRequested"));
            GameHub.ChannelPortHandoff resume =
                    hub.pendingChannelPortHandoff("test", 0);
            assertNotNull(resume);
            assertEquals("TestKnight", resume.character());
            assertEquals(sourceName, resume.source());
            assertNotNull(hub.pendingChannelPortHandoff("test", -1));
            hub.disconnect(departing);
            assertEquals(0, hub.channelSnapshot(0).users());

            ClientSession resumed = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("lobby-exit-resume-capture")), hub);
            hub.connect(resumed);
            setPhase(resumed, "LOBBY");
            setField(resumed, "account", "test");
            setField(resumed, "selectedClassicServerName", sourceName);
            setField(resumed, "characters",
                    accounts.repositoryForClassicSource(sourceName).characters("test"));
            GameHub.ChannelPortHandoff sharedResume =
                    hub.pendingChannelPortHandoff("test", -1);
            assertNotNull(sharedResume);
            setField(resumed, "channelPortHandoffTarget", sharedResume.channelId());
            setField(resumed, "channelPortHandoffCharacter", sharedResume.character());
            int resumeSeed = 0x3810_0703;
            setField(resumed, "outgoing", new Protocol.CipherState(resumeSeed));
            Protocol.CipherState receiver = new Protocol.CipherState(resumeSeed);

            assertTrue((boolean) invokeNoArg(
                    resumed, "autoSelectPendingChannelPortHandoff"));
            assertEquals("TestKnight", field(resumed, "selectedCharacter"));
            assertTrue((boolean) field(resumed, "litoCharacterSelectionConsumed"));
            byte[] silentRestore = receive(client, receiver);
            assertEquals(Protocol.S_ADD_INVENTORY_BATCH,
                    Byte.toUnsignedInt(silentRestore[0]));

            invokeHandle(resumed, bytes(Protocol.C_LITO_ENTER_CHANNEL, 0));
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoChannelInfo(
                            Protocol.LitoChannelSelection.CHANNEL_0, 1, 100),
                    receive(client, receiver));
            CharacterSummary restoredCharacter = accounts
                    .repositoryForClassicSource(sourceName).characters("test")
                    .stream().filter(row -> row.name().equals("TestKnight"))
                    .findFirst().orElseThrow();
            assertLobbyStatusFoodOrder(
                    receive(client, receiver), restoredCharacter);
            assertEquals("CHANNEL", field(resumed, "phase").toString());
            assertEquals(0, field(resumed, "currentChannelId"));
            assertEquals(1, hub.channelSnapshot(0).users());
            assertEquals(null, hub.pendingChannelPortHandoff("test", 0));
        }
    }

    @Test void characterSelectionPublishesEquippedSourceItemsToLobbyInventory()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/"
                        + "server.blessed-armor-native-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("lobby-inventory-state.properties"), false);
        String sourceName = config.classicServerNames().get(0);
        try (GameHub hub = new GameHub(config);
             AccountRepository accounts = AccountRepository.open(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("lobby-inventory-capture")), hub);
            hub.connect(session);
            List<CharacterSummary> characters = accounts
                    .repositoryForClassicSource(sourceName).characters("test7");
            assertEquals(1, characters.size());
            assertEquals(8L, characters.get(0).objectId());
            setPhase(session, "LOBBY");
            setField(session, "account", "test7");
            setField(session, "selectedClassicServerName", sourceName);
            setField(session, "characters", characters);
            setField(session, "originalCharacterCount", 1);

            int seed = 0x3810_0859;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            invokeHandle(session, new Protocol.Writer()
                    .c(Protocol.C_LITO_SELECT_CHAR)
                    .s(characters.get(0).name()).bytes());

            assertArrayEquals(Protocol.litoCharacterSelectResult(
                            Protocol.LitoCharacterSelectResult.ACCEPTED),
                    receive(client, receiver));
            byte[] inventoryBatch = receive(client, receiver);
            assertEquals(Protocol.S_ADD_INVENTORY_BATCH,
                    Byte.toUnsignedInt(inventoryBatch[0]));
            RuntimeInventory inventory = (RuntimeInventory) field(session, "inventory");
            @SuppressWarnings("unchecked")
            java.util.Map<Long, Long> sourceEquipmentObjects =
                    (java.util.Map<Long, Long>) field(session,
                            "sourceEquipmentObjects");
            assertTrue(sourceEquipmentObjects.containsValue(0x0F00_2008L));
            assertTrue(inventory.items().stream()
                    .anyMatch(item -> sourceEquipmentObjects.containsKey(
                            item.objectId())));
            assertEquals(Math.min(0xFF, inventory.size()),
                    Byte.toUnsignedInt(inventoryBatch[1]));
            assertTrue(Byte.toUnsignedInt(inventoryBatch[1]) > 0);
            assertEquals(true, field(session, "inventoryBootstrapSent"));
        }
    }

    @Test void directWindowQuitDoesNotCreateLobbyExitResumeCapability()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("direct-quit-state.properties"), false);
        try (GameHub hub = new GameHub(config)) {
            ClientSession session = new ClientSession(
                    null, ClientSession.Endpoint.GAME, config, null, state,
                    null, hub);
            hub.connect(session);
            setPhase(session, "CHANNEL");
            setField(session, "account", "test");
            setField(session, "selectedClassicServerName",
                    config.classicServerNames().get(0));
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "currentChannelId", 0);

            invokeHandle(session, bytes(Protocol.C_QUIT));

            assertTrue((boolean) field(session, "quitRequested"));
            assertEquals(null, hub.pendingChannelPortHandoff("test", 0));
        }
    }

    private static CharacterSummary character(String name) {
        return new CharacterSummary(name, "", 0, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0);
    }

    private static void invokeHandle(ClientSession session, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static Object invokeNoArg(ClientSession session, String methodName)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(session);
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

    private static void assertLobbyStatusFoodOrder(
            byte[] packet, CharacterSummary character) {
        Protocol.Reader status = new Protocol.Reader(packet);
        assertEquals(Protocol.S_STATUS, status.c());
        assertEquals(character.objectId(), status.d());
        status.c();
        status.d();
        for (int index = 0; index < 6; index++) {
            if (NumericBreakthrough.levelAbilityEnabled()) status.d();
            else status.c();
        }
        if (NumericBreakthrough.enabled()) {
            status.d();
            status.d();
            status.d();
            status.d();
            status.d();
        } else {
            status.h();
            status.h();
            status.h();
            status.h();
            status.c();
        }
        assertEquals(0L, status.d());
        assertEquals(Protocol.nativeFoodGauge(character.food()), status.c());
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
