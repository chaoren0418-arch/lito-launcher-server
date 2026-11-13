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
final class ClientSessionWorldRescueFollowerTest {
    @TempDir Path temporary;

    @Test
    void restoredSearcherStartCreatesFollowerAndDefersRewardUntilArrival()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc searcher = source(71092);
        try (Fixture fixture = fixtureNear(searcher, 1, "TestKnight", "searcher-start")) {
            fixture.state.saveStoryQuestSteps(fixture.identity(),
                    Map.of(10801, 1, 10804, 0));
            assertTrue(fixture.hub.transformClassicWorldNpcGfx(
                    searcher, ClassicWorldRescueFollowers.SEARCHER_CURSED_GFX,
                    ClassicWorldRescueFollowers.SEARCHER_RESTORED_GFX));

            fixture.invokeNpcAction(new Protocol.HypertextActionRequest(
                    searcher.objectId(), "start", "",
                    Protocol.HypertextActionKind.GENERIC));

            ClassicWorldRescueFollowers.Snapshot follower = fixture.hub
                    .classicWorldRescueFollowers().owned(fixture.identity());
            assertNotNull(follower);
            assertEquals(ClassicWorldRescueFollowers.Kind.SEARCHER,
                    follower.definition().kind());
            assertEquals(0, fixture.state.storyQuests(fixture.identity()).step(10804));
            assertEquals(0, fixture.inventory().catalogCounts()
                    .getOrDefault(40593L, 0));
            assertEquals(ClassicWorldRescueFollowers.SEARCHER_CURSED_GFX,
                    fixture.hub.classicWorldNpcGfx(searcher));

            List<byte[]> packets = fixture.packets();
            assertTrue(contains(packets, Protocol.removeObject(searcher.objectId())));
            assertTrue(contains(packets, Protocol.classicWorldRescueFollower(follower)));
            assertTrue(contains(packets, Protocol.classicStaticNpc(
                    searcher, fixture.hub.classicWorldNpcHeading(searcher),
                    ClassicWorldRescueFollowers.SEARCHER_CURSED_GFX)));
            assertTrue(contains(packets, Protocol.hypertext(
                    searcher.objectId(), "searcherk2", "", List.of())));
        }
    }

    @Test
    void abandonedSearcherUsesFollowerNpcEventAndCanBeReclaimed()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc searcher = source(71092);
        try (Fixture fixture = fixtureNear(searcher, 1, "TestKnight", "searcher-reclaim")) {
            fixture.state.saveStoryQuestSteps(fixture.identity(),
                    Map.of(10801, 1, 10804, 0));
            ClassicWorldRescueFollowers.Snapshot first = fixture.hub
                    .classicWorldRescueFollowers().claimStatic(
                            ClassicWorldRescueFollowers.SEARCHER,
                            "departed-owner", fixture.x, fixture.y, 4, 0);
            assertNotNull(first);
            ClassicWorldRescueFollowers.Transition abandoned = fixture.hub
                    .classicWorldRescueFollowers().tick(
                            "departed-owner", 5, fixture.x, fixture.y,
                            true, 2_000_000_000L, (a, b, c, d, e) -> true);
            assertEquals(ClassicWorldRescueFollowers.TransitionKind.ABANDONED,
                    abandoned.kind());

            fixture.invokeNpcAction(new Protocol.HypertextActionRequest(
                    abandoned.after().objectId(), "start", "",
                    Protocol.HypertextActionKind.GENERIC));

            ClassicWorldRescueFollowers.Snapshot reclaimed = fixture.hub
                    .classicWorldRescueFollowers().owned(fixture.identity());
            assertNotNull(reclaimed);
            assertEquals(71093, reclaimed.definition().followerNpcId());
            assertEquals(0, fixture.state.storyQuests(fixture.identity()).step(10804));
            List<byte[]> packets = fixture.packets();
            assertTrue(contains(packets, Protocol.removeObject(
                    abandoned.after().objectId())));
            assertTrue(contains(packets,
                    Protocol.classicWorldRescueFollower(reclaimed)));
        }
    }

    @Test
    void twoEncryptedSessionsReceiveClaimAndNativeOwnerStrangerPages()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc searcher = source(71092);
        try (PairFixture fixture = pairNear(searcher)) {
            fixture.state.saveStoryQuestSteps(fixture.owner.identity(),
                    Map.of(10801, 1, 10804, 0));
            assertTrue(fixture.hub.transformClassicWorldNpcGfx(
                    searcher, ClassicWorldRescueFollowers.SEARCHER_CURSED_GFX,
                    ClassicWorldRescueFollowers.SEARCHER_RESTORED_GFX));

            fixture.owner.invokeNpcAction(new Protocol.HypertextActionRequest(
                    searcher.objectId(), "start", "",
                    Protocol.HypertextActionKind.GENERIC));
            ClassicWorldRescueFollowers.Snapshot follower = fixture.hub
                    .classicWorldRescueFollowers().owned(fixture.owner.identity());
            assertNotNull(follower);

            List<byte[]> ownerPackets = fixture.owner.packets();
            List<byte[]> observerPackets = fixture.observer.packets();
            byte[] followerPacket = Protocol.classicWorldRescueFollower(follower);
            byte[] removeStatic = Protocol.removeObject(searcher.objectId());
            assertTrue(contains(ownerPackets, removeStatic));
            assertTrue(contains(observerPackets, removeStatic));
            assertTrue(contains(ownerPackets, followerPacket));
            assertTrue(contains(observerPackets, followerPacket));

            fixture.owner.openHypertext(follower.objectId());
            fixture.observer.openHypertext(follower.objectId());
            assertTrue(contains(fixture.owner.packets(), Protocol.hypertext(
                    follower.objectId(), "searcherk2", "", List.of())));
            assertTrue(contains(fixture.observer.packets(), Protocol.hypertext(
                    follower.objectId(), "searcherk4", "", List.of())));
        }
    }

    @Test
    void searcherArrivalCompletesStageStartsNextAndAwardsMissingPage()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc soldier = source(70740);
        try (Fixture fixture = fixtureNear(soldier, 1, "TestKnight", "searcher-end")) {
            fixture.state.saveStoryQuestSteps(fixture.identity(),
                    Map.of(10801, 1, 10804, 0));
            ClassicWorldRescueFollowers.Snapshot follower = fixture.hub
                    .classicWorldRescueFollowers().claimStatic(
                            ClassicWorldRescueFollowers.SEARCHER,
                            fixture.identity(), fixture.x, fixture.y, 4, 0);
            assertNotNull(follower);

            fixture.session.serverClassicWorldCompanionTick(4, 2_000_000_000L);

            Map<Integer, Integer> quests = fixture.state
                    .storyQuests(fixture.identity()).effective();
            assertEquals(1, quests.get(10804));
            assertEquals(0, quests.get(10805));
            assertEquals(1, fixture.inventory().catalogCounts().get(40593L));
            assertNull(fixture.hub.classicWorldRescueFollowers()
                    .owned(fixture.identity()));
            assertTrue(contains(fixture.packets(),
                    Protocol.removeObject(follower.objectId())));
        }
    }

    @Test
    void endiaArrivalCompletesStageStartsLetterAndAwardsPouch()
            throws Exception {
        ClassicWorldNpcCatalog.StaticNpc lyra = source(70811);
        try (Fixture fixture = fixtureNear(lyra, 4, "TestDarkElf", "endia-end")) {
            fixture.state.saveStoryQuestSteps(fixture.identity(),
                    Map.of(12605, 1, 12701, 0));
            ClassicWorldRescueFollowers.Snapshot follower = fixture.hub
                    .classicWorldRescueFollowers().claimStatic(
                            ClassicWorldRescueFollowers.ENDIA,
                            fixture.identity(), fixture.x, fixture.y, 5, 0);
            assertNotNull(follower);

            fixture.session.serverClassicWorldCompanionTick(4, 2_000_000_000L);

            Map<Integer, Integer> quests = fixture.state
                    .storyQuests(fixture.identity()).effective();
            assertEquals(1, quests.get(12701));
            assertEquals(0, quests.get(12702));
            assertEquals(1, fixture.inventory().catalogCounts().get(40582L));
            assertNull(fixture.hub.classicWorldRescueFollowers()
                    .owned(fixture.identity()));
            RuntimeInventory.Item pouch = fixture.inventory().items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.ENDIA_POUCH)
                    .findFirst().orElseThrow();
            fixture.openEndiaPouch(pouch);
            assertEquals(0, fixture.inventory().catalogCounts()
                    .getOrDefault(InventoryCatalog.ENDIA_POUCH, 0));
            assertEquals(1, fixture.inventory().catalogCounts()
                    .get(InventoryCatalog.ENDIA_LETTER));
            assertTrue(contains(fixture.packets(),
                    Protocol.removeObject(follower.objectId())));
        }
    }

    private Fixture fixtureNear(ClassicWorldNpcCatalog.StaticNpc target,
                                int characterType, String characterName,
                                String suffix) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-" + suffix + ".properties"), false);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-" + suffix));
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
        setField(session, "selectedCharacter", characterName);
        setField(session, "characters", List.of(
                character(characterType, characterName, target)));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7100_2001L));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        ArenaMapCatalog.Point point = nearbyPassable(target);
        setField(session, "classicWorldMapId", 4);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 4, point.x(), point.y(), 0);
        return new Fixture(hub, state, capture, client, server,
                session, receiver, point.x(), point.y());
    }

    private PairFixture pairNear(ClassicWorldNpcCatalog.StaticNpc target)
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state-pair.properties"), false);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture-pair"));
        ArenaMapCatalog.Point point = nearbyPassable(target);
        Peer owner = peer(config, hub, state, capture, point,
                1, "TestKnight", 0x4567_89AB, 0x7100_3001L);
        Peer observer = peer(config, hub, state, capture, point,
                1, "TestObserver", 0x5678_9ABC, 0x7100_4001L);
        return new PairFixture(hub, state, capture, owner, observer);
    }

    private static Peer peer(
            ServerConfig config, GameHub hub, GameStateStore state,
            CaptureLog capture, ArenaMapCatalog.Point point,
            int characterType, String characterName, int seed,
            long inventoryObjectId) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "test2");
        setField(session, "selectedCharacter", characterName);
        setField(session, "characters", List.of(new CharacterSummary(
                characterName, "", characterType, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, characterType, point.x(), point.y(), 4,
                8, 40, 1, 8, 8, 0)));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
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
        setField(session, "classicWorldMapId", 4);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 4, point.x(), point.y(), 0);
        return new Peer(client, server, session, receiver);
    }

    private static ClassicWorldNpcCatalog.StaticNpc source(int npcId) {
        return ClassicWorldNpcCatalog.all(4).stream()
                .filter(npc -> npc.npcId() == npcId)
                .findFirst().orElseThrow();
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        for (int radius = 1; radius <= 2; radius++) {
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
        throw new AssertionError("no passable rescue interaction tile");
    }

    private static CharacterSummary character(
            int type, String name, ClassicWorldNpcCatalog.StaticNpc target) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                3, 0, 0, 0, type, target.x(), target.y(), 4,
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
        String identity() { return session.sourceCharacterIdentity(); }

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

        void openEndiaPouch(RuntimeInventory.Item pouch) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "useEndiaPouch", RuntimeInventory.Item.class);
            method.setAccessible(true);
            method.invoke(session, pouch);
        }

        @Override public void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
            hub.close();
        }
    }

    private record Peer(Socket client, Socket server, ClientSession session,
                        Protocol.CipherState receiver) {
        String identity() { return session.sourceCharacterIdentity(); }

        void invokeNpcAction(Protocol.HypertextActionRequest request) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction", Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        void openHypertext(long objectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, objectId);
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
    }

    private record PairFixture(GameHub hub, GameStateStore state, CaptureLog capture,
                               Peer owner, Peer observer) implements AutoCloseable {
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
