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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldPirateBackHalfTest {
    @TempDir Path temporary;

    @Test
    void kamylaQuestStartsAndEntersFrancuMazeOverEncryptedFrames()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc kamyla = ClassicWorldNpcCatalog.all(481).get(0);
        Fixture fixture = fixture("KamylaVerifier", kamyla,
                Map.of(40621L, 1));
        try {
            fixture.state().saveStoryQuestSteps(
                    fixture.identity(), Map.of(31, 255));
            fixture.open();
            assertContains(fixture.packets(), Protocol.hypertext(
                    kamyla.objectId(), "kamyla1", "", List.of()));

            fixture.action("a");
            assertContains(fixture.packets(), Protocol.hypertext(
                    kamyla.objectId(), "kamyla7", "", List.of()));
            assertEquals(1, fixture.state().storyQuests(
                    fixture.identity()).step(32));

            fixture.action("b");
            assertContains(fixture.packets(), Protocol.portal(482, 32679, 32742));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 482, 32679, 32742, 5);
        } finally {
            fixture.close();
        }
    }

    @Test
    void francuAwardsMazeMapAndReturnsToKamylaOverEncryptedFrames()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc francu = ClassicWorldNpcCatalog.npc(
                482, ClassicWorldNpcCatalog.objectId(71089));
        Fixture fixture = fixture("FrancuVerifier", francu, Map.of());
        try {
            fixture.state().saveStoryQuestSteps(
                    fixture.identity(), Map.of(32, 1));
            fixture.action("a");
            List<byte[]> rewardPackets = fixture.packets();
            assertContains(rewardPackets, Protocol.hypertext(
                    francu.objectId(), "francu10", "", List.of()));
            assertEquals(1, fixture.inventory().catalogCounts().get(40644L));
            assertEquals(2, fixture.state().storyQuests(
                    fixture.identity()).step(32));

            fixture.action("b");
            assertContains(fixture.packets(), Protocol.portal(481, 32735, 32845));
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 481, 32735, 32845, 5);
        } finally {
            fixture.close();
        }
    }

    @Test
    void kamylaFiveChoiceRewardsPersistExactlyOnceOverEncryptedFrames()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc kamyla = ClassicWorldNpcCatalog.all(481).get(0);
        for (Map.Entry<String, Integer> reward
                : ClassicWorldKamylaQuestCatalog.finalRewards().entrySet()) {
            Fixture fixture = fixture("KamylaReward" + reward.getKey(), kamyla,
                    Map.of(40717L, 1));
            try {
                fixture.state().saveStoryQuestSteps(
                        fixture.identity(), Map.of(32, 4));
                fixture.action(reward.getKey());
                List<byte[]> packets = fixture.packets();
                assertContains(packets, Protocol.hypertext(
                        kamyla.objectId(), "kamyla25", "", List.of()));
                assertEquals(1, fixture.inventory().catalogCounts()
                        .get((long) reward.getValue()));
                assertFalse(fixture.inventory().catalogCounts().containsKey(40717L));
                assertEquals(255, fixture.state().storyQuests(
                        fixture.identity()).step(32));

                fixture.action(reward.getKey());
                fixture.packets();
                assertEquals(1, fixture.inventory().catalogCounts()
                        .get((long) reward.getValue()));
                assertEquals(255, fixture.state().storyQuests(
                        fixture.identity()).step(32));
            } finally {
                fixture.close();
            }
        }
    }

    @Test
    void weakSoulConvertsToOneCrewCorpseWithoutPortal() throws Exception {
        Fixture fixture = fixture("WeakSoulVerifier", 550, 32844, 32693,
                Map.of(41208L, 1));
        try {
            RuntimeInventory.Item soul = fixture.inventory().items().get(0);
            fixture.use(soul.objectId());
            List<byte[]> packets = fixture.packets();
            assertFalse(packets.stream().anyMatch(packet ->
                    packet.length > 0 && Byte.toUnsignedInt(packet[0])
                            == Protocol.S_PORTAL));
            assertEquals(Map.of(41207L, 1),
                    fixture.inventory().catalogCounts());
            assertEquals(Map.of(41207L, 1), fixture.state().economy(
                    fixture.identity()).inventory());
        } finally {
            fixture.close();
        }
    }

    @Test
    void voyagerRewardIsOneOfTheTwoRetainedEncryptedBranches()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc voyager = ClassicWorldNpcCatalog.all(550)
                .stream().filter(ClassicWorldPirateShipGraveCatalog::isVoyager)
                .findFirst().orElseThrow();
        Fixture fixture = fixture("VoyagerVerifier", voyager, Map.of());
        try {
            fixture.open();
            assertContains(fixture.packets(), Protocol.hypertext(
                    voyager.objectId(), "voyager1", "", List.of()));
            fixture.action("a");
            List<byte[]> packets = fixture.packets();
            Map<Long, Integer> counts = fixture.inventory().catalogCounts();
            int itemId = counts.containsKey(49082L) ? 49082 : 49083;
            assertEquals(Map.of((long) itemId, 1), counts);
            assertContains(packets, Protocol.hypertext(voyager.objectId(),
                    ClassicWorldPirateShipGraveCatalog.voyagerRewardHtml(itemId),
                    "", List.of()));
        } finally {
            fixture.close();
        }
    }

    @Test
    void aldranConsumesOneCorpseAndUsesAllFiveExactEncryptedPortals()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc aldran = ClassicWorldNpcCatalog.all(550)
                .stream().filter(ClassicWorldPirateShipGraveCatalog::isAldran)
                .findFirst().orElseThrow();
        for (String action : List.of("a", "b", "c", "d", "e")) {
            ClassicWorldPirateShipGraveCatalog.AldranRoute route =
                    ClassicWorldPirateShipGraveCatalog.aldranRoute(action);
            Fixture fixture = fixture("AldranVerifier" + action, aldran,
                    Map.of(41207L, 2));
            try {
                fixture.action(action);
                assertContains(fixture.packets(), Protocol.portal(
                        route.mapId(), route.x(), route.y()));
                assertEquals(1, fixture.inventory().catalogCounts().get(41207L));

                // A replay while C_GOTO_PORTAL is pending cannot consume again.
                fixture.action(action);
                fixture.packets();
                assertEquals(1, fixture.inventory().catalogCounts().get(41207L));

                fixture.completePortal();
                fixture.packets();
                assertLocation(fixture, route.mapId(), route.x(), route.y(),
                        route.heading());
                assertEquals(1, fixture.state().economy(
                        fixture.identity()).inventory().get(41207L));
            } finally {
                fixture.close();
            }
        }
    }

    @Test
    void aldranWithoutCorpseReturnsNativeMissingHtmlWithoutTeleport()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc aldran = ClassicWorldNpcCatalog.all(550)
                .stream().filter(ClassicWorldPirateShipGraveCatalog::isAldran)
                .findFirst().orElseThrow();
        Fixture fixture = fixture("AldranMissingCorpse", aldran, Map.of());
        try {
            fixture.action("a");
            List<byte[]> packets = fixture.packets();
            assertContains(packets, Protocol.hypertext(aldran.objectId(),
                    "aldran9", "", List.of()));
            assertFalse(packets.stream().anyMatch(packet ->
                    packet.length > 0 && Byte.toUnsignedInt(packet[0])
                            == Protocol.S_PORTAL));
            assertTrue(fixture.inventory().catalogCounts().isEmpty());
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(String name, ClassicWorldNpcCatalog.StaticNpc npc,
                            Map<Long, Integer> inventory) throws Exception {
        ArenaMapCatalog.Point point = nearbyPassable(npc);
        return fixture(name, npc.mapId(), point.x(), point.y(), inventory, npc);
    }

    private Fixture fixture(String name, int mapId, int x, int y,
                            Map<Long, Integer> inventory) throws Exception {
        return fixture(name, mapId, x, y, inventory, null);
    }

    private Fixture fixture(String name, int mapId, int x, int y,
                            Map<Long, Integer> inventory,
                            ClassicWorldNpcCatalog.StaticNpc npc) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve(name + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + name));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x3780_0000 + mapId + name.hashCode();
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "pirate-back-half");
        setField(session, "selectedCharacter", name);
        CharacterSummary selected = character(name, mapId, x, y);
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
        AtomicLong ids = new AtomicLong(0x7378_0000L + mapId * 32L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventory, ids::incrementAndGet));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        hub.connect(session);
        hub.enterClassicWorld(session, mapId, x, y, 0);
        return new Fixture(hub, state, capture, client, server,
                session, receiver, npc);
    }

    private static CharacterSummary character(String name, int mapId, int x, int y) {
        return new CharacterSummary(name, "", 1, 0, 50, 100, 100,
                0, 12, 12, 8, 8, 8, 8, 12, 12,
                0x7378_0000L + mapId, 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc npc) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(npc.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = npc.x() - radius; x <= npc.x() + radius; x++) {
                for (int y = npc.y() - radius; y <= npc.y() + radius; y++) {
                    if (map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(npc.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new IllegalStateException("no passable point near NPC " + npc.npcId());
    }

    private static void assertLocation(
            Fixture fixture, int mapId, int x, int y, int heading) throws Exception {
        assertEquals(List.of(mapId, x, y, heading), List.of(
                intField(fixture.session(), "classicWorldMapId"),
                intField(fixture.session(), "positionX"),
                intField(fixture.session(), "positionY"),
                intField(fixture.session(), "playerHeading")));
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

    private static Object objectField(Object target, String name) throws Exception {
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
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(GameHub hub, GameStateStore state, CaptureLog capture,
                           Socket client, Socket server, ClientSession session,
                           Protocol.CipherState receiver,
                           ClassicWorldNpcCatalog.StaticNpc npc) {
        String identity() { return session.sourceCharacterIdentity(); }

        RuntimeInventory inventory() throws Exception {
            return (RuntimeInventory) objectField(session, "inventory");
        }

        void open() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, npc.objectId());
        }

        void action(String action) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, new Protocol.HypertextActionRequest(
                    npc.objectId(), action, "",
                    Protocol.HypertextActionKind.GENERIC));
        }

        void use(long objectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "useInventoryItem", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new Protocol.Writer()
                    .c(Protocol.C_USE_ITEM).d(objectId).s("").bytes());
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
                    byte[] encrypted = Protocol.readFrame(client.getInputStream(), 65_535);
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
