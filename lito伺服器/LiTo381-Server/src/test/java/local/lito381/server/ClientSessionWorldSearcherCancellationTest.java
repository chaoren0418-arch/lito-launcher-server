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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldSearcherCancellationTest {
    @TempDir Path temporary;

    @Test
    void exactWorldSkill44ConsumesResourcesAndPublishesSearcherShape()
            throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.cast();
            List<byte[]> packets = fixture.packets();
            long objectId = fixture.searcher.objectId();

            assertTrue(contains(packets, Protocol.objectAction(3, 19)));
            assertTrue(contains(packets, Protocol.effectObject(objectId, 870)));
            assertTrue(contains(packets, Protocol.classicStaticNpcShape(
                    objectId, ClassicWorldRescueFollowers.SEARCHER_RESTORED_GFX,
                    0)));
            assertTrue(contains(packets, Protocol.manaPoint(60, 100)));
            assertEquals(1, fixture.inventory().catalogCounts()
                    .get(InventoryCatalog.MAGIC_GEM));
            assertEquals(60, integerField(fixture.session, "playerMp"));
            assertEquals(ClassicWorldRescueFollowers.SEARCHER_RESTORED_GFX,
                    fixture.hub.classicWorldNpcGfx(fixture.searcher));

            ClassicWorldStoryQuestCatalog.Dialogue dialogue =
                    ClassicWorldStoryQuestCatalog.dialogue(
                            ClassicWorldRescueFollowers.SEARCHER_SOURCE_NPC_ID,
                            1, 50, 0, 0,
                            fixture.hub.classicWorldNpcGfx(fixture.searcher),
                            Map.of(10801, 1, 10804, 0), Map.of());
            assertEquals("searcherk1", dialogue.html());
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
        Protocol.CipherState receiver = new Protocol.CipherState(0x2345_6789);
        setField(session, "outgoing", new Protocol.CipherState(0x2345_6789));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "test2");
        setField(session, "selectedCharacter", "TestMage");
        setField(session, "characters", List.of(character()));
        setField(session, "learnedSkills", Set.of(BattleSpellCatalog.CANCELLATION));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.MAGIC_GEM, 3), () -> 0x7100_3001L));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);

        ClassicWorldNpcCatalog.StaticNpc searcher = ClassicWorldNpcCatalog.all(4)
                .stream().filter(npc -> npc.npcId() == 71092)
                .findFirst().orElseThrow();
        ArenaMapCatalog.Point caster = nearbyPassable(searcher);
        setField(session, "classicWorldMapId", 4);
        setField(session, "positionX", caster.x());
        setField(session, "positionY", caster.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 4, caster.x(), caster.y(), 0);
        return new Fixture(hub, capture, client, server,
                session, receiver, searcher);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()),
                            Math.abs(y - target.y())) == radius
                            && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(4, x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no passable Searcher casting tile");
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TestMage", "", 3, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, 3, 34242, 33355, 4,
                8, 40, 1, 8, 8, 0);
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> java.util.Arrays.equals(packet, expected));
    }

    private static int integerField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
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

    private record Fixture(GameHub hub, CaptureLog capture, Socket client,
                           Socket server, ClientSession session,
                           Protocol.CipherState receiver,
                           ClassicWorldNpcCatalog.StaticNpc searcher)
            implements AutoCloseable {
        void cast() throws Exception {
            Method useSpell = ClientSession.class.getDeclaredMethod("useSpell", byte[].class);
            useSpell.setAccessible(true);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(5).c(3).d(searcher.objectId()).bytes();
            useSpell.invoke(session, (Object) request);
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
