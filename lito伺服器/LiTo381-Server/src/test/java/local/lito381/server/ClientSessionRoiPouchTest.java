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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionRoiPouchTest {
    @TempDir Path temporary;

    @Test
    void encryptedSocketPairCommitsAllEightExecutorBranchesIncludingTwoSourceDefects()
            throws Exception {
        try (Fixture fixture = fixture()) {
            long pouchObjectId = fixture.inventory().items().get(0).objectId();
            for (int roll : new int[] {0, 10, 20, 30, 40, 48, 49, 50}) {
                fixture.open(roll);
            }

            Map<Long, Integer> counts = fixture.inventory().catalogCounts();
            assertEquals(0, counts.getOrDefault(InventoryCatalog.ROI_POUCH, 0));
            assertEquals(1_000, counts.get(InventoryCatalog.ADENA));
            assertEquals(1, counts.get(40016L));
            assertEquals(1, counts.get(InventoryCatalog.BLUE_POTION));
            assertEquals(1, counts.get(40089L));
            assertEquals(1, counts.get(InventoryCatalog.POLYMORPH_SCROLL));
            assertEquals(1, counts.get(40074L));
            assertFalse(counts.containsKey(
                    (long) RoiPouchCatalog.ELVEN_SILVER_ARMOR_MISSING_TEMPLATE));
            assertFalse(counts.containsKey(
                    (long) RoiPouchCatalog.BLESSED_OGRE_BELT_MISSING_TEMPLATE));

            List<byte[]> packets = fixture.packets();
            assertTrue(packets.size() >= 12);
            assertTrue(packets.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet,
                            Protocol.removeInventory(pouchObjectId))));
            assertEquals(counts,
                    fixture.state.economy(fixture.identity()).inventory());
        }
    }

    private Fixture fixture() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture"));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(0x4567_89AB);
        setField(session, "outgoing", new Protocol.CipherState(0x4567_89AB));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "test2");
        setField(session, "selectedCharacter", "TestMage");
        setField(session, "characters", List.of(character()));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.ROI_POUCH, 8), () -> 0x7100_2001L));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", ClassicWorldRoiFollowers.MAP_ID);
        ArenaMapCatalog.Point point = passableMap410Point();
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, ClassicWorldRoiFollowers.MAP_ID,
                point.x(), point.y(), 0);
        return new Fixture(hub, state, capture, client, server, session, receiver);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TestMage", "", 3, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, 3, 32823, 32892, 410,
                8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point passableMap410Point() {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldRoiFollowers.MAP_ID);
        for (int x = 32818; x <= 32828; x++) {
            for (int y = 32888; y <= 32900; y++) {
                if (map.standingPassable(x, y)
                        && !ClassicWorldNpcCatalog.occupies(
                                ClassicWorldRoiFollowers.MAP_ID, x, y)) {
                    return new ArenaMapCatalog.Point(x, y);
                }
            }
        }
        throw new AssertionError("no passable map410 test tile");
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
                           Protocol.CipherState receiver) implements AutoCloseable {
        String identity() {
            return session.sourceCharacterIdentity();
        }

        void open(int roll) throws Exception {
            RuntimeInventory.Item pouch = inventory().items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.ROI_POUCH)
                    .findFirst().orElseThrow();
            Method method = ClientSession.class.getDeclaredMethod(
                    "useRoiPouch", RuntimeInventory.Item.class, int.class);
            method.setAccessible(true);
            method.invoke(session, pouch, roll);
        }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
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

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }
}
