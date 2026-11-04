package local.lito381.server;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Encrypted C/S acceptance for retained Hell entry, expiry and death restart. */
final class ClientSessionClassicWorldHellIntegrationTest {
    private static final AtomicInteger NEXT = new AtomicInteger();
    @TempDir Path temporary;

    @Test
    void firstEntryEmits552AndPortalThenExpiryReturnsThroughPortalHandshake()
            throws Exception {
        LocalResources.assumeNativeCatalogPins();
        Fixture fixture = fixture("HellFirst", 4, 33051, 32337);
        try {
            setInt(fixture.session(), "generalPkCount", 100);
            fixture.beginHell(true);
            List<byte[]> entry = fixture.packets();
            assertContains(entry, Protocol.effectObject(
                    fixture.session().playerObjectId(),
                    ClassicWorldTeleportCatalog.TELEPORT_EFFECT_GFX));
            assertContains(entry, Protocol.portal(666, 32701, 32777));
            assertContains(entry, Protocol.messageCode(552, "100", "455"));
            assertLocation(fixture, 4, 33051, 32337, -1);

            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 666, 32701, 32777, 5);
            assertEquals(27_300, intField(
                    fixture.session(), "hellRemainingTicks"));

            fixture.state().elapseHell("HellFirst", 27_299);
            setInt(fixture.session(), "hellRemainingTicks", 1);
            setInt(fixture.session(), "playerHp", 0);
            ClassicWorldHell.Clock hellClock = (ClassicWorldHell.Clock)
                    field(fixture.session(), "hellClock");
            long tickOrigin = 10_000L;
            hellClock.reset(tickOrigin);
            fixture.session().serverExpireTimedStatuses(
                    tickOrigin + ClassicWorldHell.TICK_NANOS);
            List<byte[]> completion = fixture.packets();
            assertTrue(ClassicWorldHell.returnDestinations().stream()
                    .anyMatch(destination -> contains(completion,
                            Protocol.portal(destination.mapId(),
                                    destination.x(), destination.y()))),
                    () -> "completion packets=" + completion.stream()
                            .map(Protocol::hex).toList());
            assertLocation(fixture, 666, 32701, 32777, 5);

            fixture.completePortal();
            fixture.packets();
            GameHub.ClassicWorldLocation returned = fixture.hub()
                    .classicWorldLocation(fixture.session());
            assertEquals(4, returned.mapId());
            assertEquals(5, returned.heading());
            assertTrue(ClassicWorldHell.returnDestinations().stream()
                    .anyMatch(destination -> destination.x() == returned.x()
                            && destination.y() == returned.y()));
            assertEquals(0, fixture.state().reconcileHell(
                    "HellFirst", 0).effective().remainingTicks());
        } finally {
            fixture.close();
        }
    }

    @Test
    void reconnectResumeEmits637AndQueuesExactMap666Entry() throws Exception {
        Fixture fixture = fixture("HellResume", 4, 33051, 32337);
        try {
            fixture.state().reconcileHell("HellResume", 77);
            setInt(fixture.session(), "hellRemainingTicks", 77);
            fixture.beginHell(false);
            List<byte[]> packets = fixture.packets();
            assertContains(packets, Protocol.portal(666, 32701, 32777));
            assertContains(packets, Protocol.messageCode(637, "77"));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 666, 32701, 32777, 5);
        } finally {
            fixture.close();
        }
    }

    @Test
    void deathRestartWithRemainingHellTimeReturnsDirectlyToMap666()
            throws Exception {
        Fixture fixture = fixture("HellRestart", 4, 33051, 32337);
        try {
            fixture.state().reconcileHell("HellRestart", 42);
            setInt(fixture.session(), "hellRemainingTicks", 42);
            setInt(fixture.session(), "playerHp", 0);
            fixture.restart();
            fixture.packets();
            assertLocation(fixture, 666, 32701, 32777, 5);
            assertTrue(intField(fixture.session(), "playerHp") > 0);
            assertEquals(42, intField(fixture.session(), "hellRemainingTicks"));
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(String name, int mapId, int x, int y)
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("hell-" + NEXT.incrementAndGet() + ".properties"),
                false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(500);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-" + NEXT.incrementAndGet()));
        ClientSession session = new ClientSession(server,
                ClientSession.Endpoint.GAME, config, null, state, capture, hub);
        int seed = 0x6660_0000 | NEXT.incrementAndGet();
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "hell-account");
        setField(session, "selectedCharacter", name);
        CharacterSummary selected = character(name, mapId, x, y);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7666_0000L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), objectIds::incrementAndGet));
        setInt(session, "playerHp", 300);
        setInt(session, "playerMaxHp", 300);
        setInt(session, "playerMp", 100);
        setInt(session, "playerMaxMp", 100);
        setInt(session, "playerClassicLevel", 80);
        setInt(session, "classicWorldMapId", mapId);
        setInt(session, "classicWorldInstanceId", -1);
        setInt(session, "positionX", x);
        setInt(session, "positionY", y);
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, x, y, 0);
        return new Fixture(hub, state, capture, client, server, session, receiver);
    }

    private static CharacterSummary character(
            String name, int mapId, int x, int y) {
        return new CharacterSummary(name, "", 1, 0, 80,
                300, 100, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                0x7666_0117L + NEXT.incrementAndGet(), 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static void assertLocation(
            Fixture fixture, int mapId, int x, int y, int heading) {
        GameHub.ClassicWorldLocation location = fixture.hub()
                .classicWorldLocation(fixture.session());
        assertEquals(List.of(mapId, x, y, heading), List.of(
                location.mapId(), location.x(), location.y(),
                heading < 0 ? -1 : location.heading()));
    }

    private static void assertContains(List<byte[]> packets, byte[] expected) {
        assertTrue(contains(packets, expected),
                "missing packet " + Protocol.hex(expected));
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> Arrays.equals(packet, expected));
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

    private static void setInt(Object target, String name, int value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
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

    private record Fixture(GameHub hub, GameStateStore state, CaptureLog capture,
                           Socket client, Socket server, ClientSession session,
                           Protocol.CipherState receiver) {
        void beginHell(boolean firstEntry) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "beginClassicWorldHell", boolean.class);
            method.setAccessible(true);
            method.invoke(session, firstEntry);
        }

        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        void restart() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "requestRestart", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_RESTART});
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
