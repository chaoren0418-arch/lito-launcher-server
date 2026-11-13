package local.lito381.server;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldCkewStageItemTest {
    @TempDir Path temporary;

    @Test
    void secondFloorKeyMovesFourSocketSessionsAndRemovesWholeStacks()
            throws Exception {
        assertStageTeleport(ClassicWorldCkewCatalog.SECOND_FLOOR_KEY);
    }

    @Test
    void thirdFloorKeyMovesFourSocketSessionsAndRemovesWholeStacks()
            throws Exception {
        assertStageTeleport(ClassicWorldCkewCatalog.THIRD_FLOOR_KEY);
    }

    @Test
    void willOfDestructionMovesFourSocketSessionsToClassAltarLandings()
            throws Exception {
        assertStageTeleport(ClassicWorldCkewCatalog.WILL_OF_DESTRUCTION);
    }

    @Test
    void missingSameShowClassSends79ThenEndsTheWholeInstance() throws Exception {
        try (PartyRig rig = party(ClassicWorldCkewCatalog.SECOND_FLOOR_KEY, 3,
                "invalid")) {
            Fixture caller = rig.fixtures.get(0);
            for (Fixture fixture : rig.fixtures) fixture.packets();
            caller.use(ClassicWorldCkewCatalog.SECOND_FLOOR_KEY);

            List<byte[]> callerPackets = caller.packets();
            int message = indexOf(callerPackets, Protocol.messageCode(79));
            int exit = indexOf(callerPackets, Protocol.portal(4, 33430, 32814));
            assertTrue(message >= 0);
            assertTrue(exit > message,
                    "native invalid-party message must precede quest end exit");
            assertEquals(2, caller.inventory().catalogCounts()
                    .get((long) ClassicWorldCkewCatalog.SECOND_FLOOR_KEY));
            assertNull(rig.hub.classicWorldCkewInstances().snapshot(100));

            for (int index = 0; index < 3; index++) {
                Fixture fixture = rig.fixtures.get(index);
                if (fixture != caller) {
                    assertTrue(fixture.packets().stream().anyMatch(packet ->
                            Arrays.equals(packet,
                                    Protocol.portal(4, 33430, 32814))));
                }
                fixture.completePortal();
                fixture.packets();
                GameHub.ClassicWorldLocation location =
                        rig.hub.classicWorldLocation(fixture.session);
                assertEquals(4, location.mapId());
                assertEquals(-1, location.instanceId());
            }
            assertEquals(410, rig.hub.classicWorldLocation(
                    rig.fixtures.get(3).session).mapId());
        }
    }

    @Test
    void stageItemOutsideMap2000OnlySends79AndKeepsTheItem() throws Exception {
        try (PartyRig rig = party(ClassicWorldCkewCatalog.SECOND_FLOOR_KEY, 0,
                "outside")) {
            Fixture caller = rig.fixtures.get(0);
            caller.packets();
            caller.use(ClassicWorldCkewCatalog.SECOND_FLOOR_KEY);
            assertTrue(caller.packets().stream().anyMatch(packet ->
                    Arrays.equals(packet, Protocol.messageCode(79))));
            assertEquals(2, caller.inventory().catalogCounts()
                    .get((long) ClassicWorldCkewCatalog.SECOND_FLOOR_KEY));
            assertTrue(rig.hub.classicWorldCkewInstances().snapshot(100) != null,
                    "outside-map executor rejection must not end the instance");
        }
    }

    private void assertStageTeleport(long itemId) throws Exception {
        try (PartyRig rig = party(itemId, 4, Long.toString(itemId))) {
            for (Fixture fixture : rig.fixtures) fixture.packets();
            rig.fixtures.get(0).use(itemId);

            for (Fixture fixture : rig.fixtures) {
                ClassicWorldCkewCatalog.Location landing =
                        ClassicWorldCkewCatalog.stageDestination(itemId, fixture.type);
                InventoryCatalog.Definition definition = InventoryCatalog.find(itemId);
                List<byte[]> packets = fixture.packets();
                int removedMessage = indexOf(packets,
                        Protocol.messageCode(165, definition.name()));
                int removedObject = indexOfOpcode(packets, Protocol.S_REMOVE_INVENTORY);
                int portal = indexOf(packets, Protocol.portal(
                        2000, landing.x(), landing.y()));
                assertTrue(removedMessage >= 0);
                assertTrue(removedObject > removedMessage,
                        "retained executor sends message165 before inventory removal");
                assertTrue(portal > removedObject);
                assertEquals(0, fixture.inventory().catalogCounts()
                        .getOrDefault(itemId, 0));

                fixture.completePortal();
                fixture.packets();
                GameHub.ClassicWorldLocation location =
                        rig.hub.classicWorldLocation(fixture.session);
                assertEquals(2000, location.mapId());
                assertEquals(100, location.instanceId());
                assertEquals(landing.x(), location.x());
                assertEquals(landing.y(), location.y());
                assertEquals(landing.heading(), location.heading());
            }
            assertTrue(rig.hub.classicWorldCkewInstances().snapshot(100).active());
        }
    }

    private PartyRig party(long itemId, int insideCount, String suffix)
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-" + suffix + ".properties"), false);
        List<Fixture> fixtures = new ArrayList<>();
        try {
            for (int type = 0; type < 4; type++) {
                fixtures.add(fixture(config, hub, state, itemId, type));
            }
            String leader = fixtures.get(0).identity();
            for (int index = 1; index < fixtures.size(); index++) {
                assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                        hub.joinClassicParty(leader,
                                fixtures.get(index).identity(), 8).status());
            }
            for (Fixture fixture : fixtures) {
                ClassicWorldCkewInstances.Admission admission =
                        hub.admitClassicWorldCkew(fixture.session, System.nanoTime());
                assertTrue(admission.admitted());
                assertEquals(100, admission.showId());
                assertTrue(hub.commitClassicWorldCkewAdmission(
                        fixture.session, admission));
            }
            for (int index = 0; index < insideCount; index++) {
                Fixture fixture = fixtures.get(index);
                ClassicWorldCkewCatalog.Location first =
                        ClassicWorldCkewCatalog.characterClass(fixture.type).firstFloor();
                hub.leaveClassicWorld(fixture.session);
                setField(fixture.session, "classicWorldMapId", 2000);
                setField(fixture.session, "classicWorldInstanceId", 100);
                setField(fixture.session, "positionX", first.x());
                setField(fixture.session, "positionY", first.y());
                setField(fixture.session, "playerHeading", first.heading());
                hub.enterClassicWorld(fixture.session, 2000,
                        first.x(), first.y(), first.heading(), 100);
            }
            return new PartyRig(hub, fixtures);
        } catch (Throwable failure) {
            for (Fixture fixture : fixtures) fixture.close();
            hub.close();
            throw failure;
        }
    }

    private Fixture fixture(ServerConfig config, GameHub hub,
                            GameStateStore state, long itemId, int type)
            throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-stage-" + itemId + "-" + type));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x2400_0000 + type;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        String name = "CkewStage" + itemId + "T" + type;
        setField(session, "account", "ckew-stage-" + type);
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(character(name, type, 301 + type)));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(itemId, 2), () -> 0x7300_0000L + type));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        ClassicWorldCkewCatalog.CharacterClass nativeClass =
                ClassicWorldCkewCatalog.characterClass(type);
        ClassicWorldNpcCatalog.StaticNpc entrance = ClassicWorldNpcCatalog.all(410)
                .stream().filter(npc -> npc.npcId() == nativeClass.entranceNpcId())
                .findFirst().orElseThrow();
        ArenaMapCatalog.Point start = nearbyPassable(entrance);
        setField(session, "classicWorldMapId", 410);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", start.x());
        setField(session, "positionY", start.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 410, start.x(), start.y(), 0);
        return new Fixture(hub, capture, client, server, session, receiver, type);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()),
                            Math.abs(y - target.y())) == radius
                            && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(
                                    target.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no CKEW entrance interaction tile");
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                objectId, 0, 0, 0, 3, 32823, 32892, 410,
                8, 40, 1, 8, 8, 0);
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (Arrays.equals(packets.get(index), expected)) return index;
        }
        return -1;
    }

    private static int indexOfOpcode(List<byte[]> packets, int opcode) {
        for (int index = 0; index < packets.size(); index++) {
            if (packets.get(index).length > 0
                    && (packets.get(index)[0] & 0xFF) == opcode) return index;
        }
        return -1;
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

    private static final class PartyRig implements AutoCloseable {
        private final GameHub hub;
        private final List<Fixture> fixtures;

        private PartyRig(GameHub hub, List<Fixture> fixtures) {
            this.hub = hub;
            this.fixtures = List.copyOf(fixtures);
        }

        @Override
        public void close() throws Exception {
            Exception first = null;
            for (Fixture fixture : fixtures) {
                try {
                    fixture.close();
                } catch (Exception failure) {
                    if (first == null) first = failure;
                }
            }
            hub.close();
            if (first != null) throw first;
        }
    }

    private record Fixture(GameHub hub, CaptureLog capture,
                           Socket client, Socket server,
                           ClientSession session,
                           Protocol.CipherState receiver, int type) {
        String identity() { return session.sourceCharacterIdentity(); }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
        }

        void use(long itemId) throws Exception {
            RuntimeInventory.Item item = inventory().items().stream()
                    .filter(value -> value.definition().catalogId() == itemId)
                    .findFirst().orElseThrow();
            byte[] payload = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(item.objectId()).s("").bytes();
            Method method = ClientSession.class.getDeclaredMethod(
                    "useInventoryItem", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) payload);
        }

        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 65_535);
                    if (encrypted == null) break;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
            return result;
        }

        void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
        }
    }
}
