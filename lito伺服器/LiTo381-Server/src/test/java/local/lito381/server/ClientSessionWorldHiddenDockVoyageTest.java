package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldHiddenDockVoyageTest {
    @TempDir Path temporary;

    @Test
    void encryptedRoundTripBuys40303BoardsMap446AndConsumesItAtDeparture()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc roxanne = ClassicWorldNpcCatalog.npc(
                440, ClassicWorldNpcCatalog.objectId(71054));
        ArenaMapCatalog.Point starting = nearbyPassable(roxanne);
        Fixture fixture = fixture("HiddenDockOutbound", 440,
                starting.x(), starting.y(), Map.of(InventoryCatalog.ADENA, 690));
        try {
            fixture.action(roxanne, "buy");
            ClassicWorldMerchantCatalog.Merchant merchant =
                    ClassicWorldMerchantCatalog.find(roxanne);
            assertContains(fixture.packets(), Protocol.classicBuyList(
                    roxanne.objectId(), List.of(merchant.offers().get(0).wireRow())));
            fixture.buy(roxanne.objectId(), 0, 1);
            fixture.packets();
            assertFalse(fixture.inventory().catalogCounts().containsKey(
                    InventoryCatalog.ADENA));
            assertEquals(1, fixture.inventory().catalogCounts().get(
                    InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK));

            fixture.relocate(440, 32297, 33087);
            fixture.enterPortal(32297, 33087);
            assertContains(fixture.packets(), Protocol.portal(446, 32735, 32795));
            assertEquals(1, fixture.inventory().catalogCounts().get(
                    InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK));
            fixture.completePortal();
            fixture.packets();
            fixture.assertLocation(446, 32735, 32795, 4);

            fixture.session().serverClassicWorldShipDeparture(5_399);
            assertTrue(fixture.packets().isEmpty());
            fixture.session().serverClassicWorldShipDeparture(5_400);
            List<byte[]> departure = fixture.packets();
            assertContains(departure, Protocol.portal(445, 32751, 32873));
            assertFalse(fixture.inventory().catalogCounts().containsKey(
                    InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK));

            fixture.completePortal();
            fixture.packets();
            fixture.assertLocation(445, 32751, 32873, 0);
        } finally {
            fixture.close();
        }
    }

    @Test
    void encryptedReturnDepartureConsumes40302AndLandsOnMap440()
            throws Exception {
        Fixture fixture = fixture("PirateReturn", 447, 32732, 32797,
                Map.of(InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND, 1));
        try {
            fixture.session().serverClassicWorldShipDeparture(10_799);
            assertTrue(fixture.packets().isEmpty());
            fixture.session().serverClassicWorldShipDeparture(10_800);
            assertContains(fixture.packets(), Protocol.portal(440, 32297, 33086));
            assertFalse(fixture.inventory().catalogCounts().containsKey(
                    InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND));
            fixture.completePortal();
            fixture.packets();
            fixture.assertLocation(440, 32297, 33086, 0);
        } finally {
            fixture.close();
        }
    }

    @Test
    void departureWithoutItsExactTicketStaysFailClosed() throws Exception {
        Fixture fixture = fixture("MissingTicket", 446, 32735, 32795, Map.of());
        try {
            fixture.session().serverClassicWorldShipDeparture(5_400);
            assertTrue(fixture.packets().isEmpty());
            fixture.assertLocation(446, 32735, 32795, 0);
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(String name, int mapId, int x, int y,
                            Map<Long, Integer> inventoryCounts) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.world-mainland-ship-selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve(name + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + name));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x4464_4700 + name.hashCode();
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "hidden-dock-voyage");
        setField(session, "selectedCharacter", name);
        CharacterSummary selected = character(name, mapId, x, y);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7460_0000L + mapId * 32L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventoryCounts, objectIds::incrementAndGet));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 50);
        setField(session, "playerMaxMp", 50);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, x, y, 0);
        return new Fixture(hub, capture, client, server, session, receiver);
    }

    private static CharacterSummary character(
            String name, int mapId, int x, int y) {
        return new CharacterSummary(name, "", 1, 0, 50, 100, 50,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7464_4700L + mapId, 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc npc) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(npc.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = npc.x() - radius; x <= npc.x() + radius; x++) {
                for (int y = npc.y() - radius; y <= npc.y() + radius; y++) {
                    if (map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(npc.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new IllegalStateException("no passable point near Roxanne");
    }

    private static void assertContains(List<byte[]> packets, byte[] expected) {
        assertTrue(packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected)));
    }

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static Object objectField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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

    private record Fixture(GameHub hub, CaptureLog capture,
                           Socket client, Socket server,
                           ClientSession session,
                           Protocol.CipherState receiver) {
        RuntimeInventory inventory() throws Exception {
            return (RuntimeInventory) objectField(session, "inventory");
        }

        void action(ClassicWorldNpcCatalog.StaticNpc npc, String action)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, new Protocol.HypertextActionRequest(
                    npc.objectId(), action, "",
                    Protocol.HypertextActionKind.GENERIC));
        }

        void buy(long npcObjectId, long orderId, long quantity) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "classicBuySell", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new Protocol.Writer()
                    .c(Protocol.C_BUY_SELL).d(npcObjectId).c(0).c(1).c(0)
                    .d(orderId).d(quantity).bytes());
        }

        void enterPortal(int x, int y) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "enterPortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new Protocol.Writer()
                    .c(Protocol.C_ENTER_PORTAL).h(x).h(y).bytes());
        }

        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        void relocate(int mapId, int x, int y) throws Exception {
            setField(session, "classicWorldMapId", mapId);
            setField(session, "positionX", x);
            setField(session, "positionY", y);
            hub.enterClassicWorld(session, mapId, x, y, 0);
            packets();
        }

        void assertLocation(int mapId, int x, int y, int heading) throws Exception {
            assertEquals(List.of(mapId, x, y, heading), List.of(
                    intField(session, "classicWorldMapId"),
                    intField(session, "positionX"),
                    intField(session, "positionY"),
                    intField(session, "playerHeading")));
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }
}
