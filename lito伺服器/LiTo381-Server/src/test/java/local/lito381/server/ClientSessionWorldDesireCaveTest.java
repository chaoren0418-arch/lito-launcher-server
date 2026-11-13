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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldDesireCaveTest {
    @TempDir Path temporary;

    @Test void openingElementDoorAtomicallyCreatesRulerAndNativePage()
            throws Exception {
        Fixture fixture = fixture(601, 80059, "EarthRuler",
                Map.of(40909L, 1, 40913L, 10), 0);
        try {
            fixture.open();
            Map<Long, Integer> counts = fixture.inventory().catalogCounts();
            assertEquals(1, counts.get(40917L));
            assertFalse(counts.containsKey(40909L));
            assertFalse(counts.containsKey(40913L));
            assertTrue(contains(fixture.packets(), Protocol.hypertext(
                    fixture.npc().objectId(), "wpass06", "", List.of())));

            GameStateStore reloaded = new GameStateStore(
                    fixture.stateFile(), true);
            assertEquals(1, reloaded.economy(fixture.character())
                    .inventory().get(40917L));
        } finally {
            fixture.close();
        }
    }

    @Test void stewardOfferingConsumesFragmentsAndPersistsKarma()
            throws Exception {
        Fixture fixture = fixture(601, 80064, "KarmaOffer",
                Map.of(40678L, 10), 0);
        try {
            fixture.action("b");
            assertFalse(fixture.inventory().catalogCounts().containsKey(40678L));
            assertEquals(1_000,
                    fixture.state().factionKarma(fixture.character()).value());
            List<byte[]> packets = fixture.packets();
            assertTrue(contains(packets, Protocol.messageCode(1078)));
            assertTrue(contains(packets, Protocol.hypertext(
                    fixture.npc().objectId(), "meet008", "", List.of())));
        } finally {
            fixture.close();
        }
    }

    @Test void desireSpyMissionUsesNative6034MorphAndQuestItems()
            throws Exception {
        Fixture fixture = fixture(778, 80067, "DesireSpy",
                Map.of(InventoryCatalog.ADENA, 100), 0);
        try {
            fixture.action("n");
            Map<Long, Integer> counts = fixture.inventory().catalogCounts();
            assertFalse(counts.containsKey(InventoryCatalog.ADENA));
            assertEquals(1, counts.get(41132L));
            assertEquals(1, counts.get(41133L));
            assertEquals(1, counts.get(41134L));
            assertEquals(1, fixture.state().storyQuests(
                    fixture.character()).step(36));
            assertEquals(6034, fixture.session().battleGfx());
            assertTrue(fixture.packets().stream().anyMatch(packet ->
                    packet.length >= 8 && (packet[0] & 0xFF) == Protocol.S_POLYMORPH));
        } finally {
            fixture.close();
        }
    }

    @Test void barlogSpyReturnRequiresKarmaLevelThreeAndUsesPortalHandshake()
            throws Exception {
        Fixture rejected = fixture(601, 80065, "LowKarmaSpy", Map.of(), 20_000);
        try {
            rejected.action("4");
            assertTrue(rejected.packets().isEmpty());
        } finally {
            rejected.close();
        }
        Fixture accepted = fixture(601, 80065, "RankThreeSpy", Map.of(), 100_000);
        try {
            accepted.action("4");
            assertTrue(contains(accepted.packets(), Protocol.portal(410, 32747, 32813)));
        } finally {
            accepted.close();
        }
    }

    private Fixture fixture(
            int mapId, int npcId, String name,
            Map<Long, Integer> items, int karma) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        Path stateFile = temporary.resolve(name + "-desire.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        String character = GameStateStore.sourceCharacter("SelfTest", name);
        state.reconcileStoryQuests(character, Map.of(36, 0));
        state.setFactionKarma(character, karma);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(
                temporary.resolve(name + "-capture"));
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME, config,
                null, state, capture, hub);
        int seed = 0x3810_8800 + npcId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", name.toLowerCase());
        setField(session, "selectedCharacter", name);
        setField(session, "selectedClassicServerName", "SelfTest");
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.all(mapId)
                .stream().filter(candidate -> candidate.npcId() == npcId)
                .findFirst().orElseThrow();
        ArenaMapCatalog.Point start = passableNear(mapId, npc.x(), npc.y());
        CharacterSummary selected = character(name, start.x(), start.y());
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
        AtomicLong ids = new AtomicLong(0x7280_0000L + npcId * 0x100L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                items, ids::incrementAndGet));
        setField(session, "playerHp", 300);
        setField(session, "playerMaxHp", 300);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 80);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", start.x());
        setField(session, "positionY", start.y());
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, start.x(), start.y(), 0, -1);
        assertEquals(character, session.sourceCharacterIdentity());
        return new Fixture(hub, state, stateFile, capture, client, server,
                session, receiver, npc, character);
    }

    private static CharacterSummary character(String name, int x, int y) {
        return new CharacterSummary(name, "", 1, 0, 80,
                300, 100, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                0x7280_0001L, 0, 0, 0, 0,
                x, y, 0, 8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point passableNear(
            int mapId, int centerX, int centerY) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        for (int radius = 1; radius <= 8; radius++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    if (map.standingPassable(x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new IllegalStateException("Desire NPC has no passable interaction tile");
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> Arrays.equals(packet, expected));
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
    private static void setEnumField(
            Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(
            GameHub hub, GameStateStore state, Path stateFile,
            CaptureLog capture, Socket client, Socket server,
            ClientSession session, Protocol.CipherState receiver,
            ClassicWorldNpcCatalog.StaticNpc npc, String character) {
        RuntimeInventory inventory() throws Exception {
            return (RuntimeInventory) field(session, "inventory");
        }

        void open() throws Exception {
            invoke("openClassicWorldHypertext", new Class<?>[] {long.class},
                    npc.objectId());
        }

        void action(String action) throws Exception {
            invoke("handleClassicWorldNpcAction",
                    new Class<?>[] {Protocol.HypertextActionRequest.class},
                    new Protocol.HypertextActionRequest(
                            npc.objectId(), action, "",
                            Protocol.HypertextActionKind.GENERIC));
        }

        private void invoke(String methodName, Class<?>[] types, Object argument)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            method.invoke(session, argument);
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
