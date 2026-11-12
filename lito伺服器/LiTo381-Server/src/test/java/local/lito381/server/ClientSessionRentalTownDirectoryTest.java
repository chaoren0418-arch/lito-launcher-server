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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientSessionRentalTownDirectoryTest {
    @TempDir Path temporary;

    @Test void gameBootstrapRearmsDirectoryCapabilityAcrossLoginRedirect()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("handoff-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("handoff-capture")), hub);
            setField(session, "outgoing", new Protocol.CipherState(0x3810_0449));
            setField(session, "rentalTownDirectoryAdvertised", false);
            setField(session, "rentalTownJoinPending", true);

            invoke(session, "beginGameBootstrap", new Class<?>[0]);

            assertTrue((boolean) field(session, "rentalTownDirectoryAdvertised"));
            assertFalse((boolean) field(session, "rentalTownJoinPending"));
        }
    }

    @Test void rentalItemSetSelectionIsClassBoundAndNeverEntersGameHubChannel()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("item-set-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("item-set-capture")), hub);
            setPhase(session, "LOBBY");
            setField(session, "selectedCharacter", "TestElf");
            setField(session, "characters", List.of(character("TestElf", 2)));
            setField(session, "rentalTownDirectoryAdvertised", true);
            int seed = 0x3810_0450;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] unavailableThird = new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(2).bytes();
            invoke(session, "enterChannel", new Class<?>[] {byte[].class},
                    unavailableThird);
            assertArrayEquals(Protocol.litoEnterChannelFailure(
                            Protocol.LitoEnterChannelFailure.CHANNEL_NOT_IN_SERVICE,
                            Protocol.LitoChannelSelection.CHANNEL_2),
                    receive(client, receiver));
            assertEquals(-1, field(session, "rentalItemSetSelection"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));

            byte[] nearSet = new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(1).bytes();
            invoke(session, "enterChannel", new Class<?>[] {byte[].class}, nearSet);
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_1),
                    receive(client, receiver));
            assertEquals(1, field(session, "rentalItemSetSelection"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));
            assertEquals(0,
                    ((RuntimeInventory) field(session, "rentalTownInventory")).size());
            assertEquals(null, field(session, "rentalTownLoadout"));
            assertTrue(hub.channelMembers(0).isEmpty());
            assertTrue(hub.channelMembers(1).isEmpty());

            invoke(session, "handle", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_ROOM_LIST).bytes());
            Protocol.Reader directory = new Protocol.Reader(receive(client, receiver));
            assertEquals(Protocol.S_LITO_SERVER_LIST, directory.c());
            assertEquals(1, directory.d());
            assertEquals(1, directory.d());
            assertEquals("Rental Village Probe", directory.s());
            assertEquals(Protocol.S_LITO_VS_SERVER_STATUS,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals("LOBBY", field(session, "phase").toString());
            assertFalse((boolean) field(session, "rentalTownJoinPending"));
        }
    }

    @Test void encryptedGroupOneChainBootstrapsResourceDerivedNativeTownScene()
            throws Exception {
        ServerConfig config = rentalConfig(560);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameHub hub = new GameHub(config);
        try (MemoryAccountRepository accounts =
                    new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            hub.connect(session);
            setPhase(session, "LOBBY");
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "characters", List.of(character("TestKnight", 1)));
            int seed = 0x3810_0444;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invoke(session, "sendLitoServerList", new Class<?>[0]);
            byte[] directory = receive(client, receiver);
            Protocol.Reader list = new Protocol.Reader(directory);
            assertEquals(Protocol.S_LITO_SERVER_LIST, list.c());
            assertEquals(1, list.d());
            assertEquals(1, list.d());
            assertEquals("Rental Village Probe", list.s());
            assertEquals(0x0100_007FL, list.d());
            assertEquals(32000, list.h());
            assertEquals(0x5000_0001L, list.d());
            assertEquals(0, list.d());
            assertEquals(config.serverName(), list.s());
            list.d();
            list.h();
            assertEquals(0, list.d());
            assertEquals(100, list.d());
            assertEquals(0, list.remaining());
            assertEquals(Protocol.S_LITO_VS_SERVER_STATUS,
                    receive(client, receiver)[0] & 0xFF);

            byte[] itemSet = new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes();
            invoke(session, "enterChannel", new Class<?>[] {byte[].class}, itemSet);
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receive(client, receiver));
            assertEquals(0, field(session, "rentalItemSetSelection"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(-1, field(session, "currentChannelId"));

            invoke(session, "quitFight", new Class<?>[0]);
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    receive(client, receiver));
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_VS_SERVER_ALLOW_JOIN).bytes(),
                    receive(client, receiver));
            assertTrue((boolean) field(session, "rentalTownJoinPending"));

            byte[] join = new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(0x5000_0001L).s("").bytes();
            invoke(session, "joinRoom",
                    new Class<?>[] {byte[].class, String.class}, join, "test");
            Protocol.Reader accepted = new Protocol.Reader(receive(client, receiver));
            assertEquals(Protocol.S_LITO_JOIN_RESULT, accepted.c());
            assertEquals(0, accepted.c());
            assertEquals(0x5000_0001L, accepted.d());
            assertEquals("Rental Village Probe", accepted.s());
            assertEquals(0, accepted.c());
            assertEquals("", accepted.s());
            assertEquals(1, accepted.h());
            assertEquals(30, accepted.h());
            assertEquals(6, accepted.c());
            assertEquals(560, accepted.h());
            for (int index = 0; index < 5; index++) assertEquals(0, accepted.c());
            assertEquals("TestKnight", accepted.s());
            for (int index = 0; index < 5; index++) assertEquals(0, accepted.c());
            assertEquals(0, accepted.remaining());
            assertEquals(Protocol.S_LITO_ROOM_USER,
                    receive(client, receiver)[0] & 0xFF);
            assertArrayEquals(Protocol.readyState(
                            session.litoDisplayName(),
                            Protocol.LitoParticipantState.READY),
                    receive(client, receiver));
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_WORLD)
                            .h(560).c(0).bytes(), receive(client, receiver));
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_START_GAME).bytes(),
                    receive(client, receiver));
            assertEquals(Protocol.S_LITO_TIMELIMIT, receive(client, receiver)[0] & 0xFF);
            NativeLitoTownCatalog.Town town = NativeLitoTownCatalog.forMap(560);
            assertArrayEquals(Protocol.observerBox(
                            town.minX(), town.minY(), town.maxX() + 1,
                            town.maxY() + 1),
                    receive(client, receiver));
            assertEquals(Protocol.S_TIME, receive(client, receiver)[0] & 0xFF);
            RuntimeInventory rentalInventory =
                    (RuntimeInventory) field(session, "rentalTownInventory");
            List<Protocol.InventoryWireItem> townItems = new java.util.ArrayList<>(
                    rentalInventory.items().stream()
                            .map(RuntimeInventory.Item::wireItem).toList());
            townItems.add(Protocol.litoRentalLightGem(0x6C00_0002L, 20));
            townItems.add(Protocol.litoRentalTownMap(0x6C00_0003L));
            townItems.add(Protocol.litoTransferStone(0x6C00_0001L));
            assertArrayEquals(Protocol.inventoryBatch(townItems),
                    receive(client, receiver));
            assertEquals(Protocol.S_LITO_POINT,
                    receive(client, receiver)[0] & 0xFF);
            for (int opcode : new int[] {
                    Protocol.S_STATUS, Protocol.S_MANA_POINT,
                    Protocol.S_ABILITY_SCORES, Protocol.S_EXP,
                    Protocol.S_AC, Protocol.S_MAGIC_STATUS,
                    Protocol.S_PUT_OBJECT, Protocol.S_LITO_MINIMAP,
                    Protocol.S_HIT_POINT}) {
                assertEquals(opcode, receive(client, receiver)[0] & 0xFF);
            }
            assertTrue((boolean) field(session, "rentalTownTransferStoneGranted"));
            assertFalse((boolean) field(session, "rentalTownJoinPending"));
            assertEquals("BATTLE", field(session, "phase").toString());
            assertEquals(0x5000_0001L, field(session, "currentRoomId"));
            assertFalse((boolean) field(session, "ready"));
            assertTrue((boolean) field(session, "rentalTownRoomState"));
            assertTrue(field(session, "rentalTownLoadout") != null);
            assertEquals(560, field(session, "rentalTownMapId"));
            assertEquals(32328, field(session, "positionX"));
            assertEquals(32768, field(session, "positionY"));
            assertTrue(rentalInventory.size() > 0);
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());

            byte[] say = new Protocol.Writer().c(Protocol.C_SAY)
                    .c(0).s("town hello").bytes();
            invoke(session, "handle", new Class<?>[] {byte[].class}, say);
            assertArrayEquals(Protocol.say(0, session.playerObjectId(),
                            session.litoDisplayName() + ": town hello", 32328, 32768),
                    receive(client, receiver));

            byte[] alternateChat = new Protocol.Writer().c(Protocol.C_CHAT)
                    .c(12).s("town trade").bytes();
            invoke(session, "handle", new Class<?>[] {byte[].class}, alternateChat);
            assertArrayEquals(Protocol.say(12, session.playerObjectId(),
                            session.litoDisplayName() + ": town trade", 32328, 32768),
                    receive(client, receiver));

            byte[] who = new Protocol.Writer().c(Protocol.C_WHO).s("").bytes();
            invoke(session, "handle", new Class<?>[] {byte[].class}, who);
            assertArrayEquals(Protocol.messageCode(0x51, "1"),
                    receive(client, receiver));

            NativeLitoTownPassabilityCatalog passability =
                    NativeLitoTownPassabilityCatalog.get();
            int startX = (int) field(session, "positionX");
            int startY = (int) field(session, "positionY");
            int stepX = 0;
            int stepY = 0;
            int heading = 0;
            int[][] steps = {{0, -1}, {1, -1}, {1, 0}, {1, 1},
                    {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
            for (int index = 0; index < steps.length; index++) {
                int x = startX + steps[index][0];
                int y = startY + steps[index][1];
                if (passability.canStep(560, startX, startY, x, y)) {
                    stepX = x;
                    stepY = y;
                    heading = index;
                    break;
                }
            }
            assertTrue(stepX != 0);
            invoke(session, "move", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_MOVE)
                            .h(startX).h(startY).c(heading).bytes());
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(stepX, field(session, "positionX"));
            assertEquals(stepY, field(session, "positionY"));

            NativeLitoTownPortalCatalog.Route route =
                    NativeLitoTownPortalCatalog.routesForMap(560).get(0);
            setField(session, "positionX", route.source().x());
            setField(session, "positionY", route.source().y());
            invoke(session, "enterPortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_ENTER_PORTAL)
                            .h(route.source().x()).h(route.source().y()).bytes());
            assertArrayEquals(Protocol.portal(route.targetMapId(),
                            route.target().x(), route.target().y()),
                    receive(client, receiver));
            invoke(session, "completePortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());
            for (int opcode : new int[] {
                    Protocol.S_STATUS, Protocol.S_MANA_POINT,
                    Protocol.S_ABILITY_SCORES, Protocol.S_EXP,
                    Protocol.S_AC, Protocol.S_MAGIC_STATUS,
                    Protocol.S_PUT_OBJECT, Protocol.S_LITO_MINIMAP,
                    Protocol.S_HIT_POINT}) {
                assertEquals(opcode, receive(client, receiver)[0] & 0xFF);
            }
            assertEquals(route.target().x(), field(session, "positionX"));
            assertEquals(route.target().y(), field(session, "positionY"));

            List<Long> rentalObjectIds = rentalInventory.items().stream()
                    .map(RuntimeInventory.Item::objectId).toList();
            // #138 B-land: in 0.6.71 and candidate 50635f7e exitRoomBeforePolyMe tests
            // phase == BATTLE first (T029 method, SAME_EFFECTS vs 0.6.71), so C_D0 inside the
            // BATTLE-phase rental-town scene is an ordinary battle withdrawal. With no GameHub
            // room behind the synthetic town room id nothing is sent and the scene stays.
            // Issue #368 only admits a staged restart return (RESTART_AWAIT_RETURN); see
            // ClientSessionRentalTownReturnTest. Without one this stays unchanged.
            invoke(session, "exitRoom", new Class<?>[0]);
            assertThrows(SocketTimeoutException.class,
                    () -> receive(client, receiver));
            assertEquals("BATTLE", field(session, "phase").toString());
            assertTrue((boolean) field(session, "rentalTownRoomState"));
            assertEquals(0x5000_0001L, field(session, "currentRoomId"));
            // The rental-town directory-return branch runs once the scene is outside BATTLE.
            setPhase(session, "ROOM");
            invoke(session, "exitRoom", new Class<?>[0]);
            for (long objectId : rentalObjectIds) {
                assertArrayEquals(Protocol.removeInventory(objectId),
                        receive(client, receiver));
            }
            assertArrayEquals(Protocol.removeInventory(0x6C00_0002L),
                    receive(client, receiver));
            assertArrayEquals(Protocol.removeInventory(0x6C00_0003L),
                    receive(client, receiver));
            assertArrayEquals(Protocol.removeInventory(0x6C00_0001L),
                    receive(client, receiver));
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                            .d(0x5000_0001L).s(session.litoDisplayName()).bytes(),
                    receive(client, receiver));
            assertEquals(Protocol.S_LITO_SERVER_LIST,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(Protocol.S_LITO_VS_SERVER_STATUS,
                    receive(client, receiver)[0] & 0xFF);
            assertFalse((boolean) field(session, "rentalTownRoomState"));
            assertFalse((boolean) field(session, "rentalTownTransferStoneGranted"));
            assertFalse((boolean) field(session, "rentalTownTransferPending"));
            assertEquals(0, field(session, "rentalTownMapId"));
            assertEquals(0, field(session, "rentalItemSetSelection"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(0L, field(session, "currentRoomId"));
            assertEquals(0,
                    ((RuntimeInventory) field(session, "rentalTownInventory")).size());
            assertEquals(null, field(session, "rentalTownLoadout"));
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());
        }
    }

    @Test void rentalTownLightGemAndMapRetainTheirNativeDirectUsePolicies()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("town-service-item-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-service-item-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "characters", List.of(character("TestKnight", 1)));
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownTransferStoneGranted", true);
            setField(session, "rentalTownMapId", 500);
            int seed = 0x3810_0454;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] lightGem = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0002L).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, lightGem);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));

            byte[] townMap = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0003L).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, townMap);
            assertArrayEquals(Protocol.showMap(0x6C00_0003L, 1),
                    receive(client, receiver));

            setField(session, "rentalTownMapId", 560);
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, townMap);
            assertArrayEquals(Protocol.showMap(0x6C00_0003L, 2),
                    receive(client, receiver));

            setField(session, "rentalTownMapId", 501);
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, townMap);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));

            byte[] forgedMap = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0003L).s("forged").bytes();
            setField(session, "rentalTownMapId", 500);
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, forgedMap);
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        } finally {
            hub.close();
        }
    }

    @Test void transferStoneAdvertisesConfiguredBattleEndpointAndStagesHandoff()
            throws Exception {
        ServerConfig config = rentalConfigWithBattleEndpoint(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("town-transfer-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-transfer-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "account", "test");
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "characters", List.of(character("TestKnight", 1)));
            setField(session, "currentRoomId", 0x5000_0001L);
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownTransferStoneGranted", true);
            setField(session, "rentalItemSetSelection", 0);
            setField(session, "rentalTownMapId", 500);
            RuntimeInventory rentalInventory = new RuntimeInventory();
            assertTrue(rentalInventory.add(
                    InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
            setField(session, "rentalTownInventory", rentalInventory);
            int seed = 0x3810_0451;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(0x6C00_0001L).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);
            Protocol.Reader directory = new Protocol.Reader(receive(client, receiver));
            assertEquals(Protocol.S_LITO_SERVER_LIST, directory.c());
            assertEquals(0, directory.d());
            assertEquals(1, directory.d());
            assertEquals("Rental Battle", directory.s());
            assertEquals(0x0300_007FL, directory.d());
            assertEquals(32001, directory.h());
            assertEquals(0, directory.d());
            assertEquals(100, directory.d());
            assertEquals(0, directory.remaining());

            NativeLitoRentalTeleportEndpointCatalog.Endpoint destination =
                    config.rentalTeleportEndpoints().get(0);
            assertEquals("127.0.0.3", destination.host());
            assertTrue((boolean) field(session, "rentalTownTransferPending"));
            assertEquals(destination, field(session, "pendingRentalTeleportEndpoint"));
            assertTrue(hub.hasRentalBattleHandoff(
                    "test", session.sourceCharacterIdentity()));
        } finally {
            hub.close();
        }
    }

    @Test void restartReturnClearsEveryIsolatedTownFieldWithoutGameHubMembership()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("restart-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config,
                    new MemoryAccountRepository("test", "test"), state,
                    new CaptureLog(temporary.resolve("restart-capture")), hub);
            setPhase(session, "ROOM");
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "account", "test");
            setField(session, "currentRoomId", 0x5000_0001L);
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownJoinPending", true);
            setField(session, "rentalItemSetSelection", 1);
            setField(session, "rentalTownMapId", 500);
            setField(session, "ready", true);
            int seed = 0x3810_0446;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invoke(session, "toggleRoomSilence", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_SILENCE).bytes());
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());

            invoke(session, "requestRestart", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_RESTART).bytes());
            assertArrayEquals(Protocol.restart(), receive(client, receiver));
            assertEquals("RESTART_AWAIT_QUIT_FIGHT",
                    field(session, "pendingTransition").toString());

            invoke(session, "quitFight", new Class<?>[0]);
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    receive(client, receiver));
            assertEquals("RESTART_AWAIT_RETURN",
                    field(session, "pendingTransition").toString());

            invoke(session, "leaveChannel", new Class<?>[0]);
            assertArrayEquals(Protocol.allowPostTransition(), receive(client, receiver));
            assertFalse((boolean) field(session, "rentalTownRoomState"));
            assertFalse((boolean) field(session, "rentalTownJoinPending"));
            assertEquals(-1, field(session, "rentalItemSetSelection"));
            assertEquals(0, field(session, "rentalTownMapId"));
            assertEquals(0L, field(session, "currentRoomId"));
            assertFalse((boolean) field(session, "ready"));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals("RESTART_AWAIT_SOURCE_REFRESH",
                    field(session, "pendingTransition").toString());
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());
        }
    }

    @Test void rentalTownTellUsesSelectedCharacterDirectoryWithoutGameHubRoom()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("town-tell-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket senderListener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket senderClient = new Socket(
                     "127.0.0.1", senderListener.getLocalPort());
             Socket senderServer = senderListener.accept();
             ServerSocket targetListener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket targetClient = new Socket(
                     "127.0.0.1", targetListener.getLocalPort());
             Socket targetServer = targetListener.accept()) {
            senderClient.setSoTimeout(150);
            targetClient.setSoTimeout(1_000);
            ClientSession sender = new ClientSession(
                    senderServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-tell-sender-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-tell-target-capture")), hub);
            hub.connect(sender);
            hub.connect(target);
            for (ClientSession session : List.of(sender, target)) {
                setPhase(session, "BATTLE");
                setField(session, "currentRoomId", 0x5000_0001L);
                setField(session, "rentalTownRoomState", true);
                setField(session, "rentalTownMapId", 500);
            }
            setField(sender, "selectedCharacter", "TownSender");
            setField(sender, "characters", List.of(character("TownSender", 1)));
            setField(target, "selectedCharacter", "TownTarget");
            setField(target, "characters", List.of(character("TownTarget", 1)));
            assertTrue(hub.selectCharacter(sender, "TownSender"));
            assertTrue(hub.selectCharacter(target, "TownTarget"));

            int senderSeed = 0x3810_0452;
            int targetSeed = 0x3810_0453;
            setField(sender, "outgoing", new Protocol.CipherState(senderSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            invoke(sender, "handle", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_TELL)
                            .s("TownTarget").s("private town hello").bytes());

            assertArrayEquals(Protocol.tell(
                            sender.litoDisplayName(), "private town hello"),
                    receive(targetClient, targetReceiver));
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(senderClient.getInputStream(), 8_192));
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());
        }
    }

    @Test void rentalTownPlayersShareRosterActorsMovementDirectionActionAndDeparture()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("town-actors-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket firstListener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket firstClient = new Socket(
                     "127.0.0.1", firstListener.getLocalPort());
             Socket firstServer = firstListener.accept();
             ServerSocket secondListener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket secondClient = new Socket(
                     "127.0.0.1", secondListener.getLocalPort());
             Socket secondServer = secondListener.accept()) {
            firstClient.setSoTimeout(1_000);
            secondClient.setSoTimeout(1_000);
            ClientSession first = new ClientSession(
                    firstServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-actors-first-capture")), hub);
            ClientSession second = new ClientSession(
                    secondServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("town-actors-second-capture")), hub);
            hub.connect(first);
            hub.connect(second);

            int firstSeed = 0x3810_0454;
            int secondSeed = 0x3810_0455;
            setField(first, "outgoing", new Protocol.CipherState(firstSeed));
            setField(second, "outgoing", new Protocol.CipherState(secondSeed));
            Protocol.CipherState firstReceiver = new Protocol.CipherState(firstSeed);
            Protocol.CipherState secondReceiver = new Protocol.CipherState(secondSeed);

            prepareTownScene(first, "TownFirst", 0x101L);
            invoke(first, "startRentalTownScene", new Class<?>[0]);
            assertArrayEquals(Protocol.readyState(
                            first.litoDisplayName(),
                            Protocol.LitoParticipantState.READY),
                    receive(firstClient, firstReceiver));
            for (int index = 0; index < 14; index++) receive(firstClient, firstReceiver);
            for (NativeLitoRentalTownNpcCatalog.Npc npc
                    : NativeLitoRentalTownNpcCatalog.forMap(500)) {
                assertRentalTownNpc(receive(firstClient, firstReceiver), npc);
            }
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(firstClient, firstReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_HIT_POINT,
                    receive(firstClient, firstReceiver)[0] & 0xFF);

            prepareTownScene(second, "TownSecond", 0x102L);
            invoke(second, "synchronizeRentalTownParticipantRows", new Class<?>[0]);
            assertEquals(Protocol.S_LITO_ROOM_USER,
                    receive(secondClient, secondReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_LITO_ROOM_USER,
                    receive(firstClient, firstReceiver)[0] & 0xFF);

            invoke(second, "startRentalTownScene", new Class<?>[0]);
            assertArrayEquals(Protocol.readyState(
                            second.litoDisplayName(),
                            Protocol.LitoParticipantState.READY),
                    receive(secondClient, secondReceiver));
            for (int index = 0; index < 14; index++) receive(secondClient, secondReceiver);
            for (NativeLitoRentalTownNpcCatalog.Npc npc
                    : NativeLitoRentalTownNpcCatalog.forMap(500)) {
                assertRentalTownNpc(receive(secondClient, secondReceiver), npc);
            }
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(secondClient, secondReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_HIT_POINT,
                    receive(secondClient, secondReceiver)[0] & 0xFF);
            assertTownActor(receive(secondClient, secondReceiver),
                    first.playerObjectId(), 32770, 32818, 4);
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(secondClient, secondReceiver)[0] & 0xFF);
            assertTownActor(receive(firstClient, firstReceiver),
                    second.playerObjectId(), 32770, 32818, 4);
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(firstClient, firstReceiver)[0] & 0xFF);

            NativeLitoTownPassabilityCatalog passability =
                    NativeLitoTownPassabilityCatalog.get();
            int startX = (int) field(second, "positionX");
            int startY = (int) field(second, "positionY");
            int stepX = 0;
            int stepY = 0;
            int heading = 0;
            int[][] steps = {{0, -1}, {1, -1}, {1, 0}, {1, 1},
                    {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
            for (int index = 0; index < steps.length; index++) {
                int candidateX = startX + steps[index][0];
                int candidateY = startY + steps[index][1];
                if (passability.canStep(500, startX, startY,
                        candidateX, candidateY)) {
                    stepX = candidateX;
                    stepY = candidateY;
                    heading = index;
                    break;
                }
            }
            assertTrue(stepX != 0);
            invoke(second, "move", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_MOVE)
                            .h(startX).h(startY).c(heading).bytes());
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(secondClient, secondReceiver)[0] & 0xFF);
            // S_12 carries the origin; the client advances one heading step.
            assertArrayEquals(NativeMovement.destination(
                            second.playerObjectId(), stepX, stepY, heading),
                    receive(firstClient, firstReceiver));
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(firstClient, firstReceiver)[0] & 0xFF);

            invoke(second, "changeDirection", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_CHANGE_DIRECTION).c(6).bytes());
            byte[] direction = new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION)
                    .d(second.playerObjectId()).c(6).bytes();
            assertArrayEquals(direction, receive(firstClient, firstReceiver));
            assertArrayEquals(direction, receive(secondClient, secondReceiver));

            invoke(second, "action", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_ACTION)
                            .c(BattleActionCodes.IDLE).bytes());
            byte[] action = Protocol.objectAction(
                    second.playerObjectId(), BattleActionCodes.IDLE);
            assertArrayEquals(action, receive(firstClient, firstReceiver));
            assertArrayEquals(action, receive(secondClient, secondReceiver));

            // #138 B-land: 0.6.71 / candidate 50635f7e treat C_D0 inside the BATTLE-phase
            // rental-town scene as an ordinary battle withdrawal (exitRoomBeforePolyMe tests
            // BATTLE first). No GameHub room exists, so neither player sees a departure.
            invoke(second, "exitRoom", new Class<?>[0]);
            assertThrows(SocketTimeoutException.class,
                    () -> receive(firstClient, firstReceiver));
            assertEquals("BATTLE", field(second, "phase").toString());
            assertTrue((boolean) field(second, "rentalTownRoomState"));
            // Outside BATTLE the rental-town departure branch publishes removal to the peer.
            setPhase(second, "ROOM");
            invoke(second, "exitRoom", new Class<?>[0]);
            assertArrayEquals(Protocol.removeObject(second.playerObjectId()),
                    receive(firstClient, firstReceiver));
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                            .d(0x5000_0001L).s(second.litoDisplayName()).bytes(),
                    receive(firstClient, firstReceiver));
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());
            assertEquals("BATTLE", field(first, "phase").toString());
        }
    }

    @Test void invitationDoesNotLeakSyntheticTownRoomIdToAnotherChannelMember()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("invite-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket inviterListener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket inviterClient = new Socket("127.0.0.1", inviterListener.getLocalPort());
             Socket inviterServer = inviterListener.accept();
             ServerSocket targetListener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket targetClient = new Socket("127.0.0.1", targetListener.getLocalPort());
             Socket targetServer = targetListener.accept()) {
            inviterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(150);
            ClientSession inviter = new ClientSession(
                    inviterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("invite-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("target-capture")), hub);
            setPhase(inviter, "ROOM");
            setField(inviter, "selectedCharacter", "TownInviter");
            setField(inviter, "currentChannelId", 0);
            setField(inviter, "hubChannelId", 0);
            setField(inviter, "currentRoomId", 0x5000_0001L);
            setField(inviter, "rentalTownRoomState", true);
            setField(inviter, "rentalTownMapId", 500);
            setPhase(target, "CHANNEL");
            setField(target, "selectedCharacter", "ChannelTarget");
            setField(target, "currentChannelId", 0);
            setField(target, "hubChannelId", 0);
            hub.enterChannel(0, inviter);
            hub.enterChannel(0, target);

            int inviterSeed = 0x3810_0447;
            int targetSeed = 0x3810_0448;
            setField(inviter, "outgoing", new Protocol.CipherState(inviterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState inviterReceiver = new Protocol.CipherState(inviterSeed);

            byte[] invitation = new Protocol.Writer().c(Protocol.C_LITO_INVITE_USER)
                    .s("ChannelTarget").bytes();
            invoke(inviter, "inviteUser", new Class<?>[] {byte[].class}, invitation);

            assertArrayEquals(Protocol.roomInvitationReply(
                            "ChannelTarget", Protocol.LitoInvitationReply.NOT_ACCEPTED),
                    receive(inviterClient, inviterReceiver));
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(targetClient.getInputStream(), 8_192));
            assertEquals("CHANNEL", field(target, "phase").toString());
            assertEquals(0L, field(target, "currentRoomId"));
            assertTrue(hub.roomMembers(0x5000_0001L).isEmpty());
        }
    }

    @Test void wrongCachedRoomIdConsumesCapabilityAndReplayCannotJoin()
            throws Exception {
        ServerConfig config = rentalConfig(500);
        GameStateStore state = new GameStateStore(
                temporary.resolve("negative-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("negative-capture")), hub);
            setPhase(session, "LOBBY");
            setField(session, "selectedCharacter", "TestKnight");
            setField(session, "characters", List.of(character("TestKnight", 1)));
            int seed = 0x3810_0445;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invoke(session, "sendLitoServerList", new Class<?>[0]);
            receive(client, receiver);
            receive(client, receiver);
            invoke(session, "enterChannel", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());
            assertArrayEquals(Protocol.litoEnterChannelSuccess(
                            Protocol.LitoChannelSelection.CHANNEL_0),
                    receive(client, receiver));
            invoke(session, "quitFight", new Class<?>[0]);
            receive(client, receiver);
            receive(client, receiver);

            byte[] wrong = new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(0x5000_0002L).s("").bytes();
            invoke(session, "joinRoom",
                    new Class<?>[] {byte[].class, String.class}, wrong, "negative");
            assertArrayEquals(Protocol.joinRejected(), receive(client, receiver));
            assertFalse((boolean) field(session, "rentalTownJoinPending"));

            byte[] replay = new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(0x5000_0001L).s("").bytes();
            invoke(session, "joinRoom",
                    new Class<?>[] {byte[].class, String.class}, replay, "replay");
            assertArrayEquals(Protocol.joinRejected(), receive(client, receiver));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertEquals(0L, field(session, "currentRoomId"));
        }
    }

    private static CharacterSummary character(String name, int type) {
        return character(name, type, 1);
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                objectId, 0, 0, 0, 0, 0, 0, 0);
    }

    private static void prepareTownScene(
            ClientSession session, String name, long objectId) throws Exception {
        setPhase(session, "ROOM");
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(character(name, 1, objectId)));
        setField(session, "currentRoomId", 0x5000_0001L);
        setField(session, "rentalTownRoomState", true);
        setField(session, "rentalTownMapId", 500);
        setField(session, "rentalItemSetSelection", 0);
    }

    private static void assertTownActor(
            byte[] payload, long objectId, int x, int y, int heading) {
        Protocol.Reader actor = new Protocol.Reader(payload);
        assertEquals(Protocol.S_PUT_OBJECT, actor.c());
        assertEquals(x, actor.h());
        assertEquals(y, actor.h());
        assertEquals(objectId, actor.d());
        actor.h();
        actor.c();
        assertEquals(heading, actor.c());
    }

    private static void assertRentalTownNpc(
            byte[] payload, NativeLitoRentalTownNpcCatalog.Npc expected) {
        Protocol.Reader actor = new Protocol.Reader(payload);
        assertEquals(Protocol.S_PUT_OBJECT, actor.c());
        assertEquals(expected.x(), actor.h());
        assertEquals(expected.y(), actor.h());
        assertEquals(expected.objectId(), actor.d());
        assertEquals(expected.gfxId(), actor.h());
        actor.c();
        assertEquals(expected.heading(), actor.c());
    }

    private ServerConfig rentalConfig(int mapId) throws Exception {
        String base = Files.readString(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path path = temporary.resolve("rental.properties");
        Files.writeString(path, base + "\n"
                + "lito.rental.town.directory.enabled=true\n"
                + "lito.rental.town.room.id=1342177281\n"
                + "lito.rental.town.directory.name=Rental Village Probe\n"
                + "lito.rental.town.directory.host=127.0.0.1\n"
                + "lito.rental.town.directory.port=32000\n"
                + "lito.rental.town.map=" + mapId + "\n");
        return ServerConfig.load(path);
    }

    private ServerConfig rentalConfigWithBattleEndpoint(int mapId) throws Exception {
        String base = Files.readString(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path path = temporary.resolve("rental-transfer.properties");
        Files.writeString(path, base + "\n"
                + "lito.rental.town.directory.enabled=true\n"
                + "lito.rental.town.room.id=1342177281\n"
                + "lito.rental.town.directory.name=Rental Village Probe\n"
                + "lito.rental.town.directory.host=127.0.0.1\n"
                + "lito.rental.town.directory.port=32000\n"
                + "lito.rental.town.map=" + mapId + "\n"
                + "lito.rental.teleport.routes="
                + "TeleportMe|LT_TELEPORT_B|Rental Battle|127.0.0.3|32001,"
                + "TeleportMe|LT_TELEPORT_C|Rental Battle|127.0.0.3|32001\n");
        return ServerConfig.load(path);
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
