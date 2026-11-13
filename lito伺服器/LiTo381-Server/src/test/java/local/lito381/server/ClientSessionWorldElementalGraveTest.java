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

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldElementalGraveTest {
    @TempDir Path temporary;

    @Test
    void map400ToAbyssLakeGroundRouteUsesNativeEncryptedPortalHandshake()
            throws Exception {
        Fixture fixture = fixture(400, 32930, 32971);
        try {
            fixture.enterPortal(32930, 32971);
            assertArrayEquals(Protocol.portal(420, 32777, 32854),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 420, 32777, 32854, 2);
        } finally {
            fixture.close();
        }
    }

    @Test
    void abyssLakeToElementalGraveGroundRouteUsesNativeCoordinates()
            throws Exception {
        Fixture fixture = fixture(420, 32807, 33047);
        try {
            fixture.enterPortal(32807, 33047);
            assertArrayEquals(Protocol.portal(430, 32934, 32786),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 430, 32934, 32786, 2);
        } finally {
            fixture.close();
        }
    }

    @Test
    void namedTeleportsReachAbyssLakeAndPirateIslandThroughEncryptedFrames()
            throws Exception {
        Fixture fixture = fixture(430, 32922, 32798);
        try {
            ClassicWorldNpcCatalog.StaticNpc grave = ClassicWorldNpcCatalog.npc(
                    430, ClassicWorldNpcCatalog.objectId(115));
            fixture.action(new Protocol.HypertextActionRequest(
                    grave.objectId(), "teleport elegrave-abysslake", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertContains(fixture.packets(), Protocol.portal(420, 32800, 33051));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 420, 32800, 33051, 5);

            setField(fixture.session(), "classicWorldMapId", 430);
            setField(fixture.session(), "positionX", 32922);
            setField(fixture.session(), "positionY", 32798);
            fixture.hub().enterClassicWorld(fixture.session(), 430, 32922, 32798, 5);
            fixture.packets();
            fixture.action(new Protocol.HypertextActionRequest(
                    grave.objectId(), "teleport elegrave-drakeIsland", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertContains(fixture.packets(), Protocol.portal(440, 32669, 32982));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 440, 32669, 32982, 5);
            assertTrue(ClassicWorldMapCatalog.forMap(440)
                    .standingPassable(32669, 32982));
        } finally {
            fixture.close();
        }
    }

    @Test
    void abyssLakeTeleporterReachesElementalGraveThroughEncryptedFrames()
            throws Exception {
        Fixture fixture = fixture(420, 32804, 33047);
        try {
            ClassicWorldNpcCatalog.StaticNpc abyss = ClassicWorldNpcCatalog.npc(
                    420, ClassicWorldNpcCatalog.objectId(116));
            fixture.action(new Protocol.HypertextActionRequest(
                    abyss.objectId(), "teleport abysslake-elegrave", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertContains(fixture.packets(), Protocol.portal(430, 32924, 32801));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 430, 32924, 32801, 5);
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(int mapId, int x, int y) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("elemental-grave-" + mapId + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + mapId));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x4204_3044 + mapId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "elemental-grave");
        setField(session, "selectedCharacter", "ElementalVerifier");
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
        AtomicLong objectIds = new AtomicLong(0x7443_0000L + mapId);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), objectIds::incrementAndGet));
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
        return new CharacterSummary("ElementalVerifier", "", 1, 0, 50, 100, 50,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7443_0440L, 0, 0, 0, 2, x, y, mapId,
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
