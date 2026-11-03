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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldPirateDungeonTest {
    @TempDir Path temporary;

    @Test
    void pirateIslandToFirstFloorUsesNativeEncryptedPortalHandshake()
            throws Exception {
        Fixture fixture = fixture(440, 32524, 33134);
        try {
            fixture.enterPortal(32524, 33134);
            assertArrayEquals(Protocol.portal(441, 32679, 32862),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 441, 32679, 32862, 2);
        } finally {
            fixture.close();
        }
    }

    @Test
    void firstThroughFourthFloorChainUsesExactEncryptedGroundRows()
            throws Exception {
        Fixture fixture = fixture(441, 32805, 32802);
        try {
            fixture.enterPortal(32805, 32802);
            assertArrayEquals(Protocol.portal(442, 32862, 32730),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 442, 32862, 32730, 2);

            fixture.relocate(442, 32860, 32799);
            fixture.enterPortal(32860, 32799);
            assertArrayEquals(Protocol.portal(443, 32864, 32781),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 443, 32864, 32781, 2);

            fixture.relocate(443, 32752, 32745);
            fixture.enterPortal(32752, 32745);
            assertArrayEquals(Protocol.portal(444, 32745, 32809),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 444, 32745, 32809, 2);
        } finally {
            fixture.close();
        }
    }

    @Test
    void fourthFloorCrystalReachesElementalGraveThroughEncryptedFrames()
            throws Exception {
        Fixture fixture = fixture(444, 32736, 32807);
        try {
            ClassicWorldNpcCatalog.StaticNpc crystal = ClassicWorldNpcCatalog.npc(
                    444, ClassicWorldNpcCatalog.objectId(117));
            fixture.action(new Protocol.HypertextActionRequest(
                    crystal.objectId(), "teleportURL", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertArrayEquals(Protocol.hypertext(
                            crystal.objectId(), "picrystal1", "", List.of()),
                    fixture.onlyPacket());

            fixture.action(new Protocol.HypertextActionRequest(
                    crystal.objectId(), "teleport piratedungeon-elementaltomb", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertContains(fixture.packets(),
                    Protocol.portal(430, 32924, 32801));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 430, 32924, 32801, 5);
            assertTrue(ClassicWorldMapCatalog.forMap(430)
                    .standingPassable(32924, 32801));
        } finally {
            fixture.close();
        }
    }

    @Test
    void fourthFloorElementalCraftConsumesNativeMaterialsAndReturnsSoulCrystal()
            throws Exception {
        Fixture fixture = fixture(444, 32731, 32804,
                Map.of(40471L, 20, 40318L, 3));
        try {
            ClassicWorldNpcCatalog.StaticNpc elemental = ClassicWorldNpcCatalog.npc(
                    444, ClassicWorldNpcCatalog.objectId(1015));
            fixture.action(new Protocol.HypertextActionRequest(
                    elemental.objectId(), "request crystal of soul", "",
                    Protocol.HypertextActionKind.GENERIC));

            List<byte[]> packets = fixture.packets();
            InventoryCatalog.Definition soulCrystal =
                    ClassicWorldCraftCatalog.inventoryDefinition(40417);
            assertContains(packets,
                    Protocol.messageCode(403, soulCrystal.name()));
            RuntimeInventory inventory = (RuntimeInventory) objectField(
                    fixture.session(), "inventory");
            assertEquals(Map.of(40417L, 1), inventory.catalogCounts());
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(int mapId, int x, int y) throws Exception {
        return fixture(mapId, x, y, Map.of());
    }

    private Fixture fixture(
            int mapId, int x, int y, Map<Long, Integer> inventoryCounts)
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("pirate-dungeon-" + mapId + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + mapId));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x4414_4440 + mapId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "pirate-dungeon");
        setField(session, "selectedCharacter", "PirateVerifier");
        CharacterSummary selected = character(mapId, x, y);
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
        AtomicLong objectIds = new AtomicLong(0x7444_0000L + mapId);
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

    private static CharacterSummary character(int mapId, int x, int y) {
        return new CharacterSummary("PirateVerifier", "", 1, 0, 50, 100, 50,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7444_0444L, 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static void assertLocation(
            Fixture fixture, int mapId, int x, int y, int heading) throws Exception {
        assertEquals(mapId, intField(fixture.session(), "classicWorldMapId"));
        assertEquals(x, intField(fixture.session(), "positionX"));
        assertEquals(y, intField(fixture.session(), "positionY"));
        assertEquals(heading, intField(fixture.session(), "playerHeading"));
        GameHub.ClassicWorldLocation location =
                fixture.hub().classicWorldLocation(fixture.session());
        assertEquals(List.of(mapId, x, y, heading), List.of(
                location.mapId(), location.x(), location.y(), location.heading()));
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
        void action(Protocol.HypertextActionRequest request) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
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

        byte[] onlyPacket() throws Exception {
            List<byte[]> packets = packets();
            assertEquals(1, packets.size());
            return packets.get(0);
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
