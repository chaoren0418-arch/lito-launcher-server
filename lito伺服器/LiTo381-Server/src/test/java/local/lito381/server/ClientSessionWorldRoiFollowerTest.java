package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldRoiFollowerTest {
    @TempDir Path temporary;

    @Test
    void restoredStaticStartCreatesFollowerRestoresZeroDelaySpawnAndPersistsStep()
            throws Exception {
        try (Fixture fixture = fixtureNear(fixtureRoi())) {
            assertTrue(fixture.hub.transformClassicWorldNpcGfx(
                    fixtureRoi(), ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
            fixture.invokeNpcAction(new Protocol.HypertextActionRequest(
                    fixtureRoi().objectId(), "start", "",
                    Protocol.HypertextActionKind.GENERIC));

            ClassicWorldRoiFollowers.Snapshot follower = fixture.hub
                    .classicWorldRoiFollowers().owned(fixture.identity());
            assertNotNull(follower);
            assertEquals(ClassicWorldRoiFollowers.QUEST_STARTED,
                    fixture.state.storyQuests(fixture.identity())
                            .step(ClassicWorldRoiFollowers.QUEST_ID));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    fixture.hub.classicWorldNpcGfx(fixtureRoi()));

            List<byte[]> packets = fixture.packets();
            assertTrue(contains(packets, Protocol.closeHypertext(fixtureRoi().objectId())));
            assertTrue(contains(packets, Protocol.removeObject(fixtureRoi().objectId())));
            assertTrue(contains(packets, Protocol.classicWorldRoiFollower(follower)));
            assertTrue(contains(packets, Protocol.classicStaticNpc(
                    fixtureRoi(), fixture.hub.classicWorldNpcHeading(fixtureRoi()),
                    ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX)));
        }
    }

    @Test
    void arrivalAtBaschCompletesQuestAwards41003AndDeletesFollower()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc basch = ClassicWorldNpcCatalog
                .all(ClassicWorldRoiFollowers.MAP_ID).stream()
                .filter(ClassicWorldDemonTempleCatalog::isBasch)
                .findFirst().orElseThrow();
        try (Fixture fixture = fixtureNear(basch)) {
            fixture.state.saveStoryQuestSteps(
                    fixture.identity(), Map.of(ClassicWorldRoiFollowers.QUEST_ID,
                            ClassicWorldRoiFollowers.QUEST_STARTED));
            ClassicWorldRoiFollowers.Snapshot follower = fixture.hub
                    .classicWorldRoiFollowers().claimStatic(
                            fixture.identity(), fixture.x, fixture.y, 4, 0);
            assertNotNull(follower);

            fixture.session.serverClassicWorldCompanionTick(
                    ClassicWorldRoiFollowers.MAP_ID, 1_000_000_000L);

            assertEquals(ClassicWorldRoiFollowers.QUEST_COMPLETE,
                    fixture.state.storyQuests(fixture.identity())
                            .step(ClassicWorldRoiFollowers.QUEST_ID));
            assertEquals(1, fixture.inventory().catalogCounts()
                    .get(InventoryCatalog.ROI_POUCH));
            assertNull(fixture.hub.classicWorldRoiFollowers()
                    .owned(fixture.identity()));
            assertTrue(contains(fixture.packets(),
                    Protocol.removeObject(follower.objectId())));
        }
    }

    private Fixture fixtureNear(ClassicWorldNpcCatalog.StaticNpc target) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-" + target.spawnId() + ".properties"), false);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-" + target.spawnId()));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(0x3456_789A);
        setField(session, "outgoing", new Protocol.CipherState(0x3456_789A));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "test2");
        setField(session, "selectedCharacter", "TestMage");
        setField(session, "characters", List.of(character()));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7100_1001L));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        ArenaMapCatalog.Point point = nearbyPassable(target);
        setField(session, "classicWorldMapId", ClassicWorldRoiFollowers.MAP_ID);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, ClassicWorldRoiFollowers.MAP_ID,
                point.x(), point.y(), 0);
        return new Fixture(hub, state, capture, client, server,
                session, receiver, point.x(), point.y());
    }

    private static ClassicWorldNpcCatalog.StaticNpc fixtureRoi() {
        return ClassicWorldNpcCatalog.npc(
                ClassicWorldRoiFollowers.MAP_ID,
                ClassicWorldNpcCatalog.objectId(
                        ClassicWorldDemonTempleCatalog.ROI_SPAWN_ID));
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 2; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()), Math.abs(y - target.y())) == radius
                            && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(target.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no passable interaction tile");
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TestMage", "", 3, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, 3, 32823, 32892, 410,
                8, 40, 1, 8, 8, 0);
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> java.util.Arrays.equals(packet, expected));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(GameHub hub, GameStateStore state, CaptureLog capture,
                           Socket client, Socket server, ClientSession session,
                           Protocol.CipherState receiver, int x, int y)
            implements AutoCloseable {
        String identity() {
            return session.sourceCharacterIdentity();
        }

        void invokeNpcAction(Protocol.HypertextActionRequest request) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction", Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
                    if (encrypted == null) break;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
            return result;
        }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
        }

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }
}
