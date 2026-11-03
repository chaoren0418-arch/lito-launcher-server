package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

final class ClientSessionWorldDspyEscortTest {
    @TempDir Path temporary;

    @Test
    void twoEncryptedSessionsSeeClaimAndNativeOwnerStrangerPages()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc source = source();
        try (PairFixture fixture = pairNear(source, "claim")) {
            fixture.state.saveStoryQuestSteps(fixture.owner.identity(),
                    Map.of(11601, 1, 11603, 0));

            fixture.owner.invokeNpcAction(new Protocol.HypertextActionRequest(
                    source.objectId(), "start", "",
                    Protocol.HypertextActionKind.GENERIC));
            ClassicWorldDspyEscorts.Snapshot follower = fixture.hub
                    .classicWorldDspyEscorts().owned(fixture.owner.identity());
            assertNotNull(follower);
            assertEquals(2500, follower.currentHp());
            assertEquals(0, fixture.state.storyQuests(
                    fixture.owner.identity()).step(11603));
            assertEquals(0, fixture.owner.inventory().catalogCounts()
                    .getOrDefault(49163L, 0));

            byte[] put = Protocol.classicWorldDspyFollower(follower);
            byte[] remove = Protocol.removeObject(source.objectId());
            List<byte[]> ownerPackets = fixture.owner.packets();
            List<byte[]> observerPackets = fixture.observer.packets();
            assertTrue(contains(ownerPackets, remove));
            assertTrue(contains(observerPackets, remove));
            assertTrue(contains(ownerPackets, put));
            assertTrue(contains(observerPackets, put));
            assertTrue(contains(ownerPackets, Protocol.classicStaticNpc(
                    source, fixture.hub.classicWorldNpcHeading(source),
                    source.gfxId())));

            fixture.owner.openHypertext(follower.objectId());
            fixture.observer.openHypertext(follower.objectId());
            assertTrue(contains(fixture.owner.packets(), Protocol.hypertext(
                    follower.objectId(), "dspy3", "", List.of())));
            assertTrue(contains(fixture.observer.packets(), Protocol.hypertext(
                    follower.objectId(), "dspy1", "", List.of())));
        }
    }

    @Test
    void ownerRectangleCompletesQuestAndAwardsSealedBookEvenWhenFollowerIsFar()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc source = source();
        try (PairFixture fixture = pairNear(source, "arrival")) {
            fixture.state.saveStoryQuestSteps(fixture.owner.identity(),
                    Map.of(11601, 1, 11603, 0));
            ClassicWorldDspyEscorts.Snapshot follower = fixture.hub
                    .classicWorldDspyEscorts().claimStatic(
                            fixture.owner.identity(), source.x(), source.y(),
                            source.heading(), 0);
            assertNotNull(follower);

            fixture.owner.moveAuthoritatively(
                    ClassicWorldDspyCatalog.ARRIVAL_MIN_X,
                    ClassicWorldDspyCatalog.ARRIVAL_MIN_Y);
            fixture.owner.session.serverClassicWorldCompanionTick(
                    400, Long.MAX_VALUE - 1);

            Map<Integer, Integer> quests = fixture.state.storyQuests(
                    fixture.owner.identity()).effective();
            assertEquals(1, quests.get(11603));
            assertEquals(0, quests.get(11604));
            assertEquals(1, fixture.owner.inventory().catalogCounts().get(49163L));
            assertNull(fixture.hub.classicWorldDspyEscorts()
                    .owned(fixture.owner.identity()));
            assertTrue(contains(fixture.observer.packets(),
                    Protocol.removeObject(follower.objectId())));
        }
    }

    @Test
    void encryptedElfReportAtomicallyStartsSelectedAndNativeCkewQuests()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc source = source();
        try (PairFixture fixture = pairNear(source, "report-bridge")) {
            fixture.state.saveStoryQuestSteps(fixture.owner.identity(),
                    Map.of(11601, 1, 11603, 0));
            ClassicWorldDspyEscorts.Snapshot follower = fixture.hub
                    .classicWorldDspyEscorts().claimStatic(
                            fixture.owner.identity(), source.x(), source.y(),
                            source.heading(), 0);
            assertNotNull(follower);
            fixture.owner.moveAuthoritatively(
                    ClassicWorldDspyCatalog.ARRIVAL_MIN_X,
                    ClassicWorldDspyCatalog.ARRIVAL_MIN_Y);
            fixture.owner.session.serverClassicWorldCompanionTick(
                    400, Long.MAX_VALUE - 1);
            assertEquals(1, fixture.owner.inventory().catalogCounts().get(49163L));

            ClassicWorldNpcCatalog.StaticNpc dicarding =
                    ClassicWorldNpcCatalog.all(77).stream()
                            .filter(npc -> npc.npcId()
                                    == ClassicWorldCkewCompletionCatalog.NPC_ID)
                            .findFirst().orElseThrow();
            fixture.owner.relocateNear(dicarding);
            fixture.owner.packets();
            fixture.owner.invokeNpcAction(new Protocol.HypertextActionRequest(
                    dicarding.objectId(), "p", "",
                    Protocol.HypertextActionKind.GENERIC));

            Map<Integer, Integer> quests = fixture.state.storyQuests(
                    fixture.owner.identity()).effective();
            assertEquals(1, quests.get(11604));
            assertEquals(0, quests.get(
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
            assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                    quests.get(ClassicWorldCkewCatalog.QUEST_ID));
            assertFalse(fixture.owner.inventory().catalogCounts().containsKey(49163L));
            assertTrue(contains(fixture.owner.packets(), Protocol.hypertext(
                    dicarding.objectId(), "dicardinge10", "", List.of())));
        }
    }

    @Test
    void linkedAssassinPublishesEffectMonsterAndAttackAgainstFollower()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc source = source();
        try (PairFixture fixture = pairNear(source, "assassin")) {
            ClassicWorldDspyEscorts.Snapshot follower = fixture.hub
                    .classicWorldDspyEscorts().claimStatic(
                            fixture.owner.identity(), source.x(), source.y(),
                            source.heading(), Long.MAX_VALUE);
            assertNotNull(follower);
            ArenaMapCatalog.Point spawn = nearbyAttackTile(source);
            ClassicWorldDspyEscorts.AssassinSpawn request =
                    new ClassicWorldDspyEscorts.AssassinSpawn(
                            400, spawn.x(), spawn.y(), follower.objectId());

            fixture.owner.spawnAssassin(request);
            ClassicWorldMonsterPopulation population = fixture.population();
            ClassicWorldMonsterPopulation.Snapshot assassin =
                    population.inScreen(400, spawn.x(), spawn.y()).stream()
                            .filter(actor -> actor.npcId() == 80011)
                            .findFirst().orElseThrow();
            assertEquals(follower.objectId(),
                    population.targetObjectId(400, assassin.objectId()));
            List<byte[]> spawnedOwner = fixture.owner.packets();
            List<byte[]> spawnedObserver = fixture.observer.packets();
            assertTrue(contains(spawnedOwner, Protocol.effectLocation(
                    spawn.x(), spawn.y(), 3992)));
            assertTrue(contains(spawnedObserver, Protocol.effectLocation(
                    spawn.x(), spawn.y(), 3992)));
            assertTrue(contains(spawnedOwner, Protocol.classicWorldMonster(assassin)));
            assertTrue(contains(spawnedObserver, Protocol.classicWorldMonster(assassin)));

            fixture.hubMonsterTick(400, population, 0);
            assertTrue(hasAttackTarget(
                    fixture.owner.packets(), follower.objectId()));
            assertTrue(hasAttackTarget(
                    fixture.observer.packets(), follower.objectId()));
        }
    }

    private PairFixture pairNear(
            ClassicWorldNpcCatalog.StaticNpc target, String suffix)
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-" + suffix + ".properties"), false);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-" + suffix));
        ArenaMapCatalog.Point point = nearbyPassable(target);
        Peer owner = peer(config, hub, state, capture, point,
                "TestElf", 0x4567_89AB, 0x7100_5001L);
        Peer observer = peer(config, hub, state, capture, point,
                "TestObserver", 0x5678_9ABC, 0x7100_6001L);
        return new PairFixture(hub, state, capture, owner, observer);
    }

    private static Peer peer(
            ServerConfig config, GameHub hub, GameStateStore state,
            CaptureLog capture, ArenaMapCatalog.Point point,
            String characterName, int seed, long inventoryObjectId)
            throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "test2");
        setField(session, "selectedCharacter", characterName);
        setField(session, "characters", List.of(new CharacterSummary(
                characterName, "", 2, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, 2, point.x(), point.y(), 400,
                8, 40, 1, 8, 8, 0)));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> inventoryObjectId));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", 400);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 400, point.x(), point.y(), 0);
        return new Peer(client, server, session, receiver, hub);
    }

    private static ClassicWorldNpcCatalog.StaticNpc source() {
        return ClassicWorldNpcCatalog.all(400).stream()
                .filter(npc -> npc.spawnId() == 1182 && npc.npcId() == 80012)
                .findFirst().orElseThrow();
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 4; radius++) {
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
        throw new AssertionError("no passable Dspy interaction tile");
    }

    private static ArenaMapCatalog.Point nearbyAttackTile(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(400);
        for (int radius = 1; radius <= 2; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()),
                            Math.abs(y - target.y())) == radius
                            && map.standingPassable(x, y)
                            && map.attackPosition(x, y,
                                target.x(), target.y(), 2)
                            && !ClassicWorldNpcCatalog.occupies(400, x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no passable Dspy assassin tile");
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
    }

    private static boolean hasAttackTarget(List<byte[]> packets, long target) {
        for (byte[] packet : packets) {
            try {
                Protocol.Reader in = new Protocol.Reader(packet);
                if (in.c() != Protocol.S_ATTACK) continue;
                in.c();
                in.d();
                if (in.d() == target) return true;
            } catch (RuntimeException ignored) {
                // Other native frames are intentionally skipped.
            }
        }
        return false;
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

    private record Peer(Socket client, Socket server, ClientSession session,
                        Protocol.CipherState receiver, GameHub hub) {
        String identity() { return session.sourceCharacterIdentity(); }

        void invokeNpcAction(Protocol.HypertextActionRequest request)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        void openHypertext(long objectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, objectId);
        }

        void spawnAssassin(ClassicWorldDspyEscorts.AssassinSpawn request)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "spawnClassicWorldDspyAssassin",
                    ClassicWorldDspyEscorts.AssassinSpawn.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        void moveAuthoritatively(int x, int y) throws Exception {
            setField(session, "positionX", x);
            setField(session, "positionY", y);
            assertTrue(hub.updateClassicWorld(session, 400, x, y, 0));
        }

        void relocateNear(ClassicWorldNpcCatalog.StaticNpc target) throws Exception {
            hub.leaveClassicWorld(session);
            ArenaMapCatalog.Point point = nearbyPassable(target);
            setField(session, "classicWorldMapId", target.mapId());
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", point.x());
            setField(session, "positionY", point.y());
            hub.enterClassicWorld(session, target.mapId(), point.x(), point.y(), 0);
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
    }

    private record PairFixture(GameHub hub, GameStateStore state,
                               CaptureLog capture, Peer owner, Peer observer)
            implements AutoCloseable {
        ClassicWorldMonsterPopulation population() throws Exception {
            Field field = GameHub.class.getDeclaredField("classicWorldMonsters");
            field.setAccessible(true);
            return (ClassicWorldMonsterPopulation) field.get(hub);
        }

        void hubMonsterTick(int mapId,
                            ClassicWorldMonsterPopulation population,
                            long nowNanos) throws Exception {
            Method method = GameHub.class.getDeclaredMethod(
                    "classicWorldMonsterTick", int.class, int.class,
                    ClassicWorldMonsterPopulation.class, long.class);
            method.setAccessible(true);
            method.invoke(hub, mapId, -1, population, nowNanos);
        }

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(observer.session());
            hub.leaveClassicWorld(owner.session());
            observer.server().close();
            observer.client().close();
            owner.server().close();
            owner.client().close();
            capture.close();
            hub.close();
        }
    }
}
