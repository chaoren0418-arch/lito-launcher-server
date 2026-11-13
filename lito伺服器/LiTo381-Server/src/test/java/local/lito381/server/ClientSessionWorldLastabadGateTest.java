package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
final class ClientSessionWorldLastabadGateTest {
    @TempDir Path temporary;

    @Test
    void map400AndGateRoundTripUsesNativePortalHandshakeOverEncryptedSocket()
            throws Exception {
        Fixture fixture = fixture(400, 33005, 32763);
        try {
            fixture.enterPortal(33005, 32763);
            assertArrayEquals(Protocol.portal(450, 32656, 32853),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 450, 32656, 32853, 2);

            fixture.enterPortal(32655, 32853);
            assertArrayEquals(Protocol.portal(400, 33004, 32763),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 400, 33004, 32763, 6);
        } finally {
            fixture.close();
        }
    }

    @Test
    void knownMap451SourceRowStaysFailClosedWithoutSendingPortal() throws Exception {
        Fixture fixture = fixture(450, 32656, 32853);
        try {
            // Isolate route lookup from collision reachability: these coordinates
            // survive in the DB, but selected-client Layer3 proves they are dead.
            setField(fixture.session(), "positionX", 32770);
            setField(fixture.session(), "positionY", 32849);
            fixture.enterPortal(32770, 32849);
            assertTrue(fixture.packets().isEmpty());
            assertNull(field(fixture.session(), "pendingPortal"));
            assertNull(field(fixture.session(), "pendingClassicWorldPortal"));
            assertEquals(32770, intField(fixture.session(), "positionX"));
            assertEquals(32849, intField(fixture.session(), "positionY"));
            GameHub.ClassicWorldLocation location =
                    fixture.hub().classicWorldLocation(fixture.session());
            assertEquals(List.of(450, 32656, 32853), List.of(
                    location.mapId(), location.x(), location.y()));
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(int mapId, int x, int y) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("gate-" + mapId + "-" + x + "-" + y
                        + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + mapId));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x4504_5000 + mapId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "lastabad-gate");
        setField(session, "selectedCharacter", "GateVerifier");
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
        AtomicLong objectIds = new AtomicLong(0x7450_0000L + mapId);
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
        return new CharacterSummary("GateVerifier", "", 1, 0, 50, 100, 50,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7450_4500L, 0, 0, 0, 2, x, y, mapId,
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

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static Object field(Object target, String name) throws Exception {
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
