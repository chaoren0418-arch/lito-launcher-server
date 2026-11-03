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

final class ClientSessionWorldCkewMedicineTest {
    @TempDir Path temporary;

    @Test
    void nonCrownClearsFourEffectsUnequipsThenApplies4007AndConsumesLast()
            throws Exception {
        try (Fixture fixture = fixture(1, true, "knight")) {
            seedEffects(fixture.session);
            fixture.packets();
            RuntimeInventory.Item medicine = fixture.item(49168);
            long before = System.nanoTime();
            assertTrue(fixture.session.classicWorldCkewAttackAllowed(87000, before));
            assertTrue(fixture.session.classicWorldCkewAttackAllowed(87001, before));
            fixture.use(medicine.objectId());
            long after = System.nanoTime();

            RuntimeInventory inventory = fixture.inventory();
            assertEquals(1, inventory.catalogCounts().get(49168L));
            assertFalse(inventory.hasEquippedWeapon());
            assertFalse(fixture.session.activeStorySkillEffect(4003, after));
            assertFalse(fixture.session.activeStorySkillEffect(4006, after));
            assertFalse(fixture.session.activeStorySkillEffect(4005, after));
            assertTrue(fixture.session.activeStorySkillEffect(4007, after));
            assertTrue(fixture.session.classicWorldCkewAttackAllowed(87000, after));
            long deadline = effectDeadlines(fixture.session).get(4007);
            long duration = 1_500_000_000_000L;
            assertTrue(deadline >= before + duration);
            assertTrue(deadline <= after + duration);

            List<byte[]> packets = fixture.packets();
            int unequip = indexOf(packets, Protocol.messageCode(1027));
            int effect = indexOf(packets, Protocol.effectObject(
                    fixture.playerObjectId(), 7248));
            int active = indexOf(packets, Protocol.messageCode(1300));
            int consumed = indexOf(packets, Protocol.changeInventoryCount(
                    medicine.objectId(), 1));
            assertTrue(unequip >= 0);
            assertTrue(effect > unequip);
            assertTrue(active > effect);
            assertTrue(consumed > active,
                    "retained executor consumes item after effect and message1300");
        }
    }

    @Test
    void altarAttackGateRequiresEffect4007OnlyForNpc87000() throws Exception {
        try (Fixture fixture = fixture(1, true, "altar-gate")) {
            long now = System.nanoTime();
            assertFalse(fixture.session.classicWorldCkewAttackAllowed(87000, now));
            assertTrue(fixture.session.classicWorldCkewAttackAllowed(87001, now));
            effectDeadlines(fixture.session).put(
                    4007, now + 1_000_000_000L);
            assertTrue(fixture.session.classicWorldCkewAttackAllowed(87000, now));
        }
    }

    @Test
    void crownKeepsEquippedWeaponButStillReplacesEffectAndConsumesOne()
            throws Exception {
        try (Fixture fixture = fixture(0, true, "crown")) {
            seedEffects(fixture.session);
            fixture.packets();
            RuntimeInventory.Item medicine = fixture.item(49168);
            fixture.use(medicine.objectId());

            assertTrue(fixture.inventory().hasEquippedWeapon());
            assertEquals(1, fixture.inventory().catalogCounts().get(49168L));
            assertTrue(fixture.session.activeStorySkillEffect(
                    4007, System.nanoTime()));
            List<byte[]> packets = fixture.packets();
            assertEquals(-1, indexOf(packets, Protocol.messageCode(1027)));
            int effect = indexOf(packets, Protocol.effectObject(
                    fixture.playerObjectId(), 7248));
            int active = indexOf(packets, Protocol.messageCode(1300));
            int consumed = indexOf(packets, Protocol.changeInventoryCount(
                    medicine.objectId(), 1));
            assertTrue(effect >= 0 && active > effect && consumed > active);
        }
    }

    @Test
    void outsideMap2000Sends79WithoutMutatingWeaponEffectsOrItem()
            throws Exception {
        try (Fixture fixture = fixture(1, false, "outside")) {
            seedEffects(fixture.session);
            Map<Integer, Long> beforeEffects = Map.copyOf(
                    effectDeadlines(fixture.session));
            fixture.packets();
            fixture.use(fixture.item(49168).objectId());

            assertEquals(2, fixture.inventory().catalogCounts().get(49168L));
            assertTrue(fixture.inventory().hasEquippedWeapon());
            assertEquals(beforeEffects, effectDeadlines(fixture.session));
            assertTrue(fixture.packets().stream().anyMatch(packet ->
                    Arrays.equals(packet, Protocol.messageCode(79))));
        }
    }

    private Fixture fixture(int type, boolean inside, String suffix)
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("medicine-" + suffix + ".properties"), false);
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-medicine-" + suffix));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x2500_0000 + type;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        String name = "CkewMedicine" + suffix;
        setField(session, "account", "ckew-medicine-" + suffix);
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(character(name, type, 401 + type)));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7400_0000L + type * 16L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(49168L, 2, InventoryCatalog.SHORT_SWORD, 1),
                objectIds::getAndIncrement);
        RuntimeInventory.Item weapon = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.SHORT_SWORD)
                .findFirst().orElseThrow();
        assertTrue(!inventory.toggleWeapon(weapon.objectId()).isEmpty());
        setField(session, "inventory", inventory);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);

        ClassicWorldCkewCatalog.CharacterClass nativeClass =
                ClassicWorldCkewCatalog.characterClass(type);
        ClassicWorldCkewCatalog.Location location;
        if (inside) {
            location = nativeClass.firstFloor();
        } else {
            ClassicWorldNpcCatalog.StaticNpc entrance =
                    ClassicWorldNpcCatalog.all(410).stream()
                            .filter(npc -> npc.npcId()
                                    == nativeClass.entranceNpcId())
                            .findFirst().orElseThrow();
            ArenaMapCatalog.Point point = nearbyPassable(entrance);
            location = new ClassicWorldCkewCatalog.Location(
                    410, point.x(), point.y(), 0);
        }
        setField(session, "classicWorldMapId", location.mapId());
        setField(session, "classicWorldInstanceId", inside ? 100 : -1);
        setField(session, "positionX", location.x());
        setField(session, "positionY", location.y());
        setField(session, "playerHeading", location.heading());
        hub.connect(session);
        hub.enterClassicWorld(session, location.mapId(), location.x(), location.y(),
                location.heading(), inside ? 100 : -1);
        return new Fixture(hub, capture, client, server, session, receiver);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 3; radius++) {
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
        throw new AssertionError("no CKEW entrance interaction tile");
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                objectId, 0, 0, 0, 3, 32823, 32892, 410,
                8, 40, 1, 8, 8, 0);
    }

    private static void seedEffects(ClientSession session) throws Exception {
        long deadline = System.nanoTime() + 9_000_000_000_000L;
        Map<Integer, Long> effects = effectDeadlines(session);
        effects.put(4003, deadline);
        effects.put(4006, deadline);
        effects.put(4005, deadline);
        effects.put(4007, deadline);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Long> effectDeadlines(ClientSession session)
            throws Exception {
        Field field = ClientSession.class.getDeclaredField(
                "storySkillEffectDeadlines");
        field.setAccessible(true);
        return (Map<Integer, Long>) field.get(session);
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (Arrays.equals(packets.get(index), expected)) return index;
        }
        return -1;
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
                           Protocol.CipherState receiver)
            implements AutoCloseable {
        long playerObjectId() { return session.playerObjectId(); }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
        }

        RuntimeInventory.Item item(long catalogId) throws Exception {
            return inventory().items().stream()
                    .filter(value -> value.definition().catalogId() == catalogId)
                    .findFirst().orElseThrow();
        }

        void use(long objectId) throws Exception {
            byte[] payload = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(objectId).s("").bytes();
            Method method = ClientSession.class.getDeclaredMethod(
                    "useInventoryItem", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) payload);
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
