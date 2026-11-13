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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldRoiCancellationTest {
    @TempDir Path temporary;

    @Test
    void exactWorldSkill44ConsumesNativeResourcesAndPublishesRoiShape()
            throws Exception {
        try (Fixture fixture = fixture(3)) {
            fixture.cast();
            List<byte[]> packets = fixture.packets();
            long roiObject = fixture.roi.objectId();

            assertTrue(contains(packets, Protocol.objectAction(3, 19)));
            assertTrue(contains(packets, Protocol.effectObject(roiObject, 870)));
            assertTrue(contains(packets, Protocol.polymorph(
                    roiObject, ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX,
                    BattleActionCodes.WALK)));
            assertTrue(contains(packets, Protocol.manaPoint(60, 100)));
            assertEquals(1, fixture.inventory().catalogCounts()
                    .get(InventoryCatalog.MAGIC_GEM));
            assertEquals(60, integerField(fixture.session, "playerMp"));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX,
                    fixture.hub.classicWorldNpcGfx(fixture.roi));
            assertEquals("roi1", ClassicWorldDemonTempleCatalog.dialogueHtml(
                    fixture.roi, 50, fixture.hub.classicWorldNpcGfx(fixture.roi)));
        }
    }

    @Test
    void missingTwoMagicGemsEmits299WithoutMpOrWorldMutation() throws Exception {
        try (Fixture fixture = fixture(1)) {
            fixture.cast();
            List<byte[]> packets = fixture.packets();

            assertTrue(contains(packets, Protocol.messageCode(299)));
            assertFalse(packets.stream().anyMatch(packet -> packet.length > 0
                    && (packet[0] & 0xFF) == Protocol.S_ACTION));
            assertFalse(packets.stream().anyMatch(packet -> packet.length > 0
                    && (packet[0] & 0xFF) == Protocol.S_POLYMORPH));
            assertEquals(1, fixture.inventory().catalogCounts()
                    .get(InventoryCatalog.MAGIC_GEM));
            assertEquals(100, integerField(fixture.session, "playerMp"));
            assertEquals(ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX,
                    fixture.hub.classicWorldNpcGfx(fixture.roi));
        }
    }

    private Fixture fixture(int gems) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-" + gems + ".properties"), false);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + gems));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(0x1234_5678);
        setField(session, "outgoing", new Protocol.CipherState(0x1234_5678));
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
                Map.of(InventoryCatalog.MAGIC_GEM, gems), () -> 0x7100_0001L));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);

        ClassicWorldNpcCatalog.StaticNpc roi = ClassicWorldNpcCatalog.npc(
                ClassicWorldDemonTempleCatalog.MAP_ID,
                ClassicWorldNpcCatalog.objectId(ClassicWorldDemonTempleCatalog.ROI_SPAWN_ID));
        ArenaMapCatalog.Point caster = nearbyPassable(roi);
        setField(session, "classicWorldMapId", ClassicWorldDemonTempleCatalog.MAP_ID);
        setField(session, "positionX", caster.x());
        setField(session, "positionY", caster.y());
        hub.connect(session);
        hub.enterClassicWorld(session, ClassicWorldDemonTempleCatalog.MAP_ID,
                caster.x(), caster.y(), 0);
        return new Fixture(hub, capture, client, server, session, receiver, roi);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc roi) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(roi.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = roi.x() - radius; x <= roi.x() + radius; x++) {
                for (int y = roi.y() - radius; y <= roi.y() + radius; y++) {
                    if (Math.max(Math.abs(x - roi.x()), Math.abs(y - roi.y())) == radius
                            && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(roi.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no passable Roi casting tile");
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

    private record Fixture(GameHub hub, CaptureLog capture, Socket client, Socket server,
                           ClientSession session, Protocol.CipherState receiver,
                           ClassicWorldNpcCatalog.StaticNpc roi) implements AutoCloseable {
        void cast() throws Exception {
            Method useSpell = ClientSession.class.getDeclaredMethod("useSpell", byte[].class);
            useSpell.setAccessible(true);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(5).c(3).d(roi.objectId()).bytes();
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
