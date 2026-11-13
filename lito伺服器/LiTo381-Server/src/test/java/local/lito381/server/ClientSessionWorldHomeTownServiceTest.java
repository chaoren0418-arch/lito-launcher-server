package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldHomeTownServiceTest {
    @TempDir Path temporary;

    @Test
    void silveriaDragonKnightTutorUsesNativeHtmlListAndPrice() throws Exception {
        ClassicWorldNpcCatalog.StaticNpc tutor = npc(1000, 1060);
        try (Fixture fixture = fixture(1000, tutor.x(), tutor.y(), 5, 45)) {
            fixture.dialogue(tutor.objectId());
            assertArrayEquals(Protocol.hypertext(
                    tutor.objectId(), "y_skill_01", "", List.of()),
                    fixture.onlyPacket());

            fixture.buyableSpells(tutor.objectId());
            assertArrayEquals(Protocol.buyableSpellList(620,
                            SpellShopCatalog.availableNativeTeacherWireSkillIds(
                                    fixture.character(), Set.of()), tutor.objectId()),
                    fixture.onlyPacket());
        }
    }

    @Test
    void behemothRepairerArmsNativeOpcodeOnlyRepairList() throws Exception {
        ClassicWorldNpcCatalog.StaticNpc repairer = npc(1001, 1061);
        try (Fixture fixture = fixture(1001, repairer.x(), repairer.y(), 6, 40)) {
            fixture.dialogue(repairer.objectId());
            List<byte[]> dialogue = fixture.packets();
            assertEquals(2, dialogue.size());
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION)
                            .d(repairer.objectId()).c(0).bytes(),
                    dialogue.get(0));
            assertArrayEquals(Protocol.hypertext(
                    repairer.objectId(), "pual1", "", List.of()),
                    dialogue.get(1));

            fixture.fixableItems();
            assertArrayEquals(Protocol.fixableItemList(200, List.of()),
                    fixture.onlyPacket());
        }
    }

    private Fixture fixture(int mapId, int x, int y, int type, int level)
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("home-town-" + mapId + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + mapId));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x5100_1000 + mapId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "home-town");
        setField(session, "selectedCharacter", "HomeTownVerifier");
        CharacterSummary selected = character(mapId, x, y, type, level);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats", SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7510_0000L + mapId);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), objectIds::incrementAndGet));
        setField(session, "learnedSkills", Set.of());
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 50);
        setField(session, "playerMaxMp", 50);
        setField(session, "playerClassicLevel", level);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, x, y, 0);
        return new Fixture(hub, capture, client, server, session, receiver, selected);
    }

    private static CharacterSummary character(
            int mapId, int x, int y, int type, int level) {
        return new CharacterSummary("HomeTownVerifier", "", type, 0, level, 100, 50,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7510_1000L + mapId, 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int mapId, int spawnId) {
        return ClassicWorldNpcCatalog.npc(mapId, ClassicWorldNpcCatalog.objectId(spawnId));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(GameHub hub, CaptureLog capture, Socket client, Socket server,
                           ClientSession session, Protocol.CipherState receiver,
                           CharacterSummary character) implements AutoCloseable {
        void dialogue(long objectId) throws Exception {
            invoke("openClassicWorldHypertext", new Class<?>[] {long.class}, objectId);
        }

        void buyableSpells(long objectId) throws Exception {
            invoke("listBuyableSpells", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_BUYABLE_SPELL).d(objectId).bytes());
        }

        void fixableItems() throws Exception {
            invoke("listFixableItems", new Class<?>[] {byte[].class},
                    new byte[] {(byte) Protocol.C_FIXABLE_ITEM});
        }

        private void invoke(String name, Class<?>[] types, Object argument) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(session, argument);
        }

        byte[] onlyPacket() throws Exception {
            List<byte[]> packets = packets();
            assertEquals(1, packets.size(), () -> "plain opcodes=" + packets.stream()
                    .map(packet -> packet.length == 0 ? "empty"
                            : String.format("0x%02X", packet[0] & 0xFF))
                    .toList());
            return packets.get(0);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
                    if (encrypted == null) return result;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
        }

        @Override
        public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }
}
