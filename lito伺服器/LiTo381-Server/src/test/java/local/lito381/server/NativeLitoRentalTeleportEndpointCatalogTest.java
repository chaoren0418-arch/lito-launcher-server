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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeLitoRentalTeleportEndpointCatalogTest {
    @TempDir Path temporary;

    @Test void parserKeepsPageQualifiedActionsAndNativeOrdinals() {
        List<NativeLitoRentalTeleportEndpointCatalog.Endpoint> endpoints =
                NativeLitoRentalTeleportEndpointCatalog.parse(
                        "TeleportMe|LT_TELEPORT_B|Blessed|127.0.0.2|32100,"
                        + "TeleportMe|LT_TELEPORT_C|Brave|127.0.0.3|32101,"
                        + "TeleportMe_C|LT_TELEPORT_B|Trial One|127.0.0.4|32102");
        assertEquals(3, endpoints.size());
        assertEquals(1, endpoints.get(0).serverIndex());
        assertEquals(0x0200_007FL, endpoints.get(0).ipv4());
        assertEquals(2, endpoints.get(1).serverIndex());
        assertEquals("Trial One", NativeLitoRentalTeleportEndpointCatalog.find(
                endpoints, "TeleportMe_C", "LT_TELEPORT_B").name());

        Protocol.LitoVsServerAvailability[] statuses =
                NativeLitoRentalTeleportEndpointCatalog.statusTable(
                        endpoints, "TeleportMe");
        assertArrayEquals(new Protocol.LitoVsServerAvailability[] {
                Protocol.LitoVsServerAvailability.UNAVAILABLE,
                Protocol.LitoVsServerAvailability.AVAILABLE,
                Protocol.LitoVsServerAvailability.AVAILABLE}, statuses);
        assertEquals(0, NativeLitoRentalTeleportEndpointCatalog.parse("").size());
    }

    @Test void parserRejectsUnknownDuplicateAndNonIpv4Rows() {
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoRentalTeleportEndpointCatalog.parse(
                        "TeleportMe|LT_TELEPORT_A|Wrong|127.0.0.2|32100"));
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoRentalTeleportEndpointCatalog.parse(
                        "TeleportMe|LT_TELEPORT_B|One|127.0.0.2|32100,"
                        + "TeleportMe|LT_TELEPORT_B|Two|127.0.0.3|32101"));
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoRentalTeleportEndpointCatalog.parse(
                        "TeleportMe|LT_TELEPORT_B|Wrong|::1|32100"));
    }

    @Test void exactTeleporterActionSendsStatusThenDirectEndpoint() throws Exception {
        String base = Files.readString(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path properties = temporary.resolve("rental-teleport.properties");
        Files.writeString(properties, base + "\n"
                + "lito.rental.teleport.routes="
                + "TeleportMe|LT_TELEPORT_B|Blessed|127.0.0.2|32100,"
                + "TeleportMe|LT_TELEPORT_C|Brave|127.0.0.3|32101\n");
        ServerConfig config = ServerConfig.load(properties);
        assertEquals(2, config.rentalTeleportEndpoints().size());
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(200);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "account", "test");
            setField(session, "selectedCharacter", "EndpointVisitor");
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            RuntimeInventory rental = new RuntimeInventory();
            assertTrue(rental.add(
                    InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
            setField(session, "rentalTownInventory", rental);
            NativeLitoRentalTownNpcCatalog.Npc teleporter =
                    NativeLitoRentalTownNpcCatalog.forMap(500).stream()
                            .filter(npc -> npc.resourceIndex() == 92)
                            .findFirst().orElseThrow();
            setField(session, "positionX", teleporter.x());
            setField(session, "positionY", teleporter.y());
            int seed = 0x3810_0808;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            handle(session, action(teleporter.objectId(),
                    NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_B, ""));
            assertArrayEquals(Protocol.vsServerStatus(
                            Protocol.LitoVsServerAvailability.UNAVAILABLE,
                            Protocol.LitoVsServerAvailability.AVAILABLE,
                            Protocol.LitoVsServerAvailability.AVAILABLE),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoVsServerIp(1, 0x0200_007FL, 32100),
                    receive(client, receiver));
            assertTrue((boolean) field(session, "rentalTownTransferPending"));

            invoke(session, "quitFight", new Class<?>[0]);
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    receive(client, receiver));
            assertEquals("LOBBY", field(session, "phase").toString());
            assertFalse((boolean) field(session, "rentalTownRoomState"));
            assertTrue(hub.hasRentalBattleHandoff(
                    "test", sessionStateCharacter(session)));

            assertTrue((boolean) invoke(session,
                    "activatePendingRentalBattleHandoff",
                    new Class<?>[] {String.class}, "same-endpoint-test"));
            assertEquals(1, ((RuntimeInventory) field(session, "inventory")).size());
            assertTrue((boolean) field(session, "rentalBattleLoadoutActive"));
            assertFalse(hub.hasRentalBattleHandoff(
                    "test", sessionStateCharacter(session)));

            invoke(session, "resetNativeTournamentEconomy",
                    new Class<?>[] {String.class}, "test-result");
            assertEquals(0, ((RuntimeInventory) field(session, "inventory")).size());
            assertFalse((boolean) field(session, "rentalBattleLoadoutActive"));

            setField(session, "rentalTownTransferPending", false);
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            handle(session, action(teleporter.objectId(),
                    NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_B, "forged"));
            assertThrows(SocketTimeoutException.class,
                    () -> receive(client, receiver));
            assertFalse((boolean) field(session, "rentalTownTransferPending"));
        }
    }

    @Test void rentalBattleHandoffIsCharacterBoundCopiedAndOneShot() {
        ServerConfig config;
        try {
            config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
        GameHub hub = new GameHub(config);
        try {
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(
                    InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
            assertTrue(hub.stageRentalBattleHandoff(
                    "Account", "source::Knight", inventory));
            inventory = inventory.withoutObjects(inventory.items().stream()
                    .map(RuntimeInventory.Item::objectId)
                    .collect(java.util.stream.Collectors.toSet()));
            assertEquals(0, inventory.size());
            assertTrue(hub.hasRentalBattleHandoff(
                    "account", "SOURCE::KNIGHT"));
            assertEquals(null, hub.consumeRentalBattleHandoff(
                    "account", "source::Other"));
            GameHub.RentalBattleHandoff accepted =
                    hub.consumeRentalBattleHandoff("ACCOUNT", "source::Knight");
            assertEquals(1, accepted.inventory().size());
            assertEquals(null, hub.consumeRentalBattleHandoff(
                    "account", "source::Knight"));
        } finally {
            hub.close();
        }
    }

    private static byte[] action(long objectId, String action, String parameter) {
        return new Protocol.Writer().c(Protocol.C_HACTION).d(objectId)
                .s(action).s(parameter).bytes();
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void handle(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static Object invoke(ClientSession session, String name,
                                 Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(session, arguments);
    }

    private static String sessionStateCharacter(ClientSession session) throws Exception {
        return (String) invoke(session, "stateCharacter", new Class<?>[0]);
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
