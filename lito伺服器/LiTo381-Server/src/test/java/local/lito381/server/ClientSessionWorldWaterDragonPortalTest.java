package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldWaterDragonPortalTest {
    @TempDir Path temporary;

    @Test
    void route464PreservesShowAndStartsUnconfiguredTransparentGateChain()
            throws Exception {
        Fixture fixture = fixture(32941, 32671, 0x1234_5678);
        try {
            fixture.enterPortal(32941, 32671);
            assertArrayEquals(Protocol.portal(1011, 32993, 32841),
                    fixture.onlyPacket());

            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 32993, 32841, 6, 0x1234_5678);
            ClassicWorldWaterDragonInstances.Snapshot snapshot =
                    fixture.hub().classicWorldWaterDragonInstances()
                            .snapshot(0x1234_5678);
            assertTrue(snapshot.bossSequenceStarted());
            assertEquals(-1, snapshot.activeBossNpcId());

            ClassicWorldWaterDragonInstances.GateTalk repeated =
                    fixture.hub().talkClassicWorldWaterDragonBossGate(
                            fixture.session(), System.nanoTime());
            assertTrue(repeated.accepted());
            assertFalse(repeated.scheduled());

            fixture.enterPortal(32993, 32841);
            assertArrayEquals(Protocol.portal(1011, 32941, 32671),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();
            assertLocation(fixture, 32941, 32671, 5, 0x1234_5678);
            assertTrue(fixture.hub().classicWorldWaterDragonInstances()
                    .snapshot(0x1234_5678).bossSequenceStarted());
        } finally {
            fixture.close();
        }
    }

    @Test
    void saellDialogAppliesAndExpiresEveryNativeAdlv80Modifier()
            throws Exception {
        Fixture fixture = fixture(32859, 32664, 0x1234_5679);
        try {
            ClassicWorldNpcCatalog.StaticNpc saell =
                    ClassicWorldNpcCatalog.all(1011).stream()
                            .filter(npc -> npc.npcId()
                                    == ClassicWorldWaterDragonCatalog.SAELL_NPC_ID)
                            .findFirst().orElseThrow();
            assertEquals(300, intField(fixture.session(), "playerMaxHp"));
            assertEquals(100, intField(fixture.session(), "playerMaxMp"));
            assertEquals(0, invokeInt(fixture.session(),
                    "effectiveSpellElementalResistance",
                    BattleSpellCatalog.ATTRIBUTE_WATER));

            fixture.open(saell.objectId());
            List<byte[]> applied = fixture.packets();
            assertTrue(applied.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.effectObject(
                            fixture.session().playerObjectId(),
                            ClassicWorldWaterDragonCatalog.WORLD_BLESSING_GFX))));
            assertEquals(380, intField(fixture.session(), "playerMaxHp"));
            assertEquals(110, intField(fixture.session(), "playerMaxMp"));
            assertEquals(-8, fixture.session().battleArmorClass());
            assertEquals(30, invokeInt(fixture.session(),
                    "effectiveSpellElementalResistance",
                    BattleSpellCatalog.ATTRIBUTE_WATER));
            assertTrue(fixture.session().activeStorySkillEffect(
                    ClassicWorldWaterDragonCatalog.WORLD_BLESSING_SKILL_ID,
                    System.nanoTime()));
            assertEquals(8, invokeInt(fixture.session(),
                    "waterDragonBlessingHpRegen", System.nanoTime()));
            assertEquals(1, invokeInt(fixture.session(),
                    "waterDragonBlessingMpRegen", System.nanoTime()));

            fixture.open(saell.objectId());
            assertTrue(fixture.packets().isEmpty(),
                    "native Npc_Saell does nothing while skill4010 is active");

            @SuppressWarnings("unchecked")
            Map<Integer, Long> deadlines = (Map<Integer, Long>) field(
                    fixture.session(), "storySkillEffectDeadlines");
            long expiredAt = System.nanoTime() - 1;
            deadlines.put(
                    ClassicWorldWaterDragonCatalog.WORLD_BLESSING_SKILL_ID,
                    expiredAt);
            fixture.session().serverExpireTimedStatuses(System.nanoTime());
            fixture.packets();
            assertEquals(300, intField(fixture.session(), "playerMaxHp"));
            assertEquals(100, intField(fixture.session(), "playerMaxMp"));
            assertEquals(0, fixture.session().battleArmorClass());
            assertEquals(0, invokeInt(fixture.session(),
                    "effectiveSpellElementalResistance",
                    BattleSpellCatalog.ATTRIBUTE_WATER));
            assertFalse(fixture.session().activeStorySkillEffect(
                    ClassicWorldWaterDragonCatalog.WORLD_BLESSING_SKILL_ID,
                    System.nanoTime()));
        } finally {
            fixture.close();
        }
    }

    @Test
    void gateRentalDebitsNativePriceAndPassObjectBecomesEntryShowId()
            throws Exception {
        Fixture fixture = gateFixture();
        try {
            ClassicWorldNpcCatalog.StaticNpc gate =
                    ClassicWorldNpcCatalog.all(613).stream()
                            .filter(npc -> npc.npcId()
                                    == ClassicWorldWaterDragonCatalog.GATE_NPC_ID)
                            .findFirst().orElseThrow();
            fixture.open(gate.objectId());
            assertArrayEquals(Protocol.hypertext(
                            gate.objectId(), "y_n_dragon1", "",
                            List.of("法利昂棲息地")),
                    fixture.onlyPacket());

            fixture.action(new Protocol.HypertextActionRequest(
                    gate.objectId(), "1", "",
                    Protocol.HypertextActionKind.GENERIC));
            fixture.packets();
            RuntimeInventory inventory = (RuntimeInventory) field(
                    fixture.session(), "inventory");
            assertEquals(90_400,
                    inventory.catalogCounts().get(40308L).intValue());
            RuntimeInventory.Item pass = inventory.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == ClassicWorldWaterDragonCatalog.PASS_ITEM_ID)
                    .findFirst().orElseThrow();
            assertEquals(32, pass.quantity());
            assertTrue(pass.expiresAtEpochMillis() > System.currentTimeMillis());
            assertEquals(1, fixture.hub()
                    .classicWorldWaterDragonRentedRooms(
                            "LiTo 3.81C Isolated Self-Test", System.nanoTime()));

            fixture.action(new Protocol.HypertextActionRequest(
                    gate.objectId(), "0", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertArrayEquals(Protocol.portal(1011, 32957, 32743),
                    fixture.onlyPacket());
            fixture.completePortal();
            fixture.packets();

            int showId = Math.toIntExact(pass.objectId());
            assertLocation(fixture, 32957, 32743, 1, showId);
            assertTrue(fixture.hub().classicWorldWaterDragonInstances()
                    .snapshot(showId).joinedIdentities().contains(
                            fixture.session().sourceCharacterIdentity()
                                    .toLowerCase(java.util.Locale.ROOT)));
            assertEquals(ClassicWorldWaterDragonMonsterCatalog.INITIAL_POPULATION,
                    fixture.hub()
                    .classicWorldWaterDragonMonsterPopulationSize(showId));
        } finally {
            fixture.close();
        }
    }

    @Test
    void completionGrantsBloodAndPortalHandshakeLeavesTheInstance()
            throws Exception {
        int showId = 0x1234_5680;
        Fixture fixture = fixture(32990, 32842, showId);
        try {
            ClassicWorldWaterDragonInstances.Ended ended =
                    fixture.hub().classicWorldWaterDragonInstances()
                            .end(showId, "complete-test");
            assertEquals(showId, ended.showId());
            fixture.session().serverClassicWorldWaterDragonEnded(
                    showId, "complete",
                    ClassicWorldWaterDragonCatalog.COMPLETION_EXIT, true);
            List<byte[]> ending = fixture.packets();
            assertTrue(ending.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.effectObject(
                            fixture.session().playerObjectId(),
                            ClassicWorldWaterDragonCatalog.COMPLETION_GFX))));
            assertTrue(ending.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.portal(
                            4, 33718, 32506))));
            assertTrue(fixture.session().activeStorySkillEffect(
                    ClassicWorldWaterDragonCatalog.WATER_DRAGON_BLOOD_SKILL_ID,
                    System.nanoTime()));

            fixture.completePortal();
            fixture.packets();
            assertEquals(4, intField(fixture.session(), "classicWorldMapId"));
            assertEquals(-1,
                    intField(fixture.session(), "classicWorldInstanceId"));
            assertEquals(33718, intField(fixture.session(), "positionX"));
            assertEquals(32506, intField(fixture.session(), "positionY"));
            GameHub.ClassicWorldLocation location = fixture.hub()
                    .classicWorldLocation(fixture.session());
            assertEquals(List.of(4, 33718, 32506, -1), List.of(
                    location.mapId(), location.x(), location.y(),
                    location.instanceId()));
        } finally {
            fixture.close();
        }
    }

    private Fixture fixture(int x, int y, int showId) throws Exception {
        return fixture(1011, x, y, showId, true, Map.of());
    }

    private Fixture gateFixture() throws Exception {
        return fixture(613, 32809, 32896, -1, false,
                Map.of(40308L, 100_000));
    }

    private Fixture fixture(int mapId, int x, int y, int showId,
                            boolean admitWaterDragon,
                            Map<Long, Integer> inventoryCounts) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("water-dragon-" + mapId + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(temporary.resolve("capture"));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x1011_1170;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "water-dragon");
        setField(session, "selectedCharacter", "FafurionVerifier");
        CharacterSummary selected = character(mapId, x, y);
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
        AtomicLong objectIds = new AtomicLong(0x7117_0000L);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventoryCounts, objectIds::incrementAndGet));
        setField(session, "playerHp", 300);
        setField(session, "playerMaxHp", 300);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 80);
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", showId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        hub.connect(session);
        if (admitWaterDragon) {
            ClassicWorldWaterDragonInstances.Admission admission =
                    hub.admitClassicWorldWaterDragon(
                            session, showId, true, false, System.nanoTime());
            assertTrue(admission.admitted());
            assertTrue(admission.created());
        }
        hub.enterClassicWorld(session, mapId, x, y, 0, showId);
        return new Fixture(hub, capture, client, server, session, receiver);
    }

    private static CharacterSummary character(int mapId, int x, int y) {
        return new CharacterSummary("FafurionVerifier", "", 1, 0, 80,
                300, 100, 0, 14, 14, 12, 12, 12, 12, 14, 14,
                0x7117_0117L, 0, 0, 0, 2, x, y, mapId,
                8, 40, 1, 8, 8, 0);
    }

    private static void assertLocation(
            Fixture fixture, int x, int y, int heading, int showId)
            throws Exception {
        assertEquals(1011, intField(fixture.session(), "classicWorldMapId"));
        assertEquals(showId,
                intField(fixture.session(), "classicWorldInstanceId"));
        assertEquals(x, intField(fixture.session(), "positionX"));
        assertEquals(y, intField(fixture.session(), "positionY"));
        assertEquals(heading, intField(fixture.session(), "playerHeading"));
        GameHub.ClassicWorldLocation location =
                fixture.hub().classicWorldLocation(fixture.session());
        assertEquals(List.of(1011, x, y, heading, showId), List.of(
                location.mapId(), location.x(), location.y(),
                location.heading(), location.instanceId()));
    }

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int invokeInt(Object target, String name, int argument)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, int.class);
        method.setAccessible(true);
        return (int) method.invoke(target, argument);
    }

    private static int invokeInt(Object target, String name, long argument)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, long.class);
        method.setAccessible(true);
        return (int) method.invoke(target, argument);
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

    private record Fixture(GameHub hub, CaptureLog capture,
                           Socket client, Socket server,
                           ClientSession session,
                           Protocol.CipherState receiver) {
        void open(long objectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, objectId);
        }

        void action(Protocol.HypertextActionRequest request) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        void enterPortal(int x, int y) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "enterPortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new Protocol.Writer()
                    .c(Protocol.C_ENTER_PORTAL).h(x).h(y).bytes());
        }

        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session,
                    (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        byte[] onlyPacket() throws Exception {
            List<byte[]> packets = packets();
            assertEquals(1, packets.size());
            return packets.get(0);
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
