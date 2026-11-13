package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionClassicWorldBowTest {
    @TempDir Path temporary;

    @Test
    void invisibleApertureDamagesLanePlayerAndStillFiresWithNoTarget()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("bow-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc playerBow =
                ClassicWorldBowCatalog.all().stream()
                        .filter(bow -> bow.spawnId() == 679)
                        .findFirst().orElseThrow();
        int playerX = playerBow.x();
        int playerY = playerBow.y() + 1;

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(temporary.resolve("bow-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5690);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary character = accounts.characters("test18").get(0);
            initialize(session, config.classicServerNames().get(0), character,
                    accounts.equipmentMagicStats(character.objectId()));
            setField(session, "outgoing", new Protocol.CipherState(0x3810_5690));
            prepareWorld(session, playerX, playerY);
            hub.connect(session);
            hub.enterClassicWorld(session, ClassicWorldBowCatalog.MAP_ID,
                    playerX, playerY, -1);
            drain(client, receiver);

            invokeStaticNpcRebuild(session);
            List<byte[]> staticPackets = drain(client, receiver);
            assertTrue(staticPackets.stream().noneMatch(packet ->
                    putObjectId(packet) == playerBow.objectId()),
                    "L1Bow.onPerceive must not emit S_PUT_OBJECT");

            Map<Long, Integer> monsterHpBefore = laneMonsterHp(hub);
            hub.classicWorldBowTick();
            List<byte[]> hitPackets = drain(client, receiver);
            BowPacket targeted = findBowPacket(hitPackets, playerBow.objectId());
            assertNotNull(targeted);
            assertEquals(session.playerObjectId(), targeted.targetObjectId());
            assertEquals(ClassicWorldBowCatalog.DAMAGE, targeted.damage());
            assertEquals(playerBow.heading(), targeted.heading());
            assertEquals(ClassicWorldBowCatalog.NATIVE_PROJECTILE_SEQUENCE,
                    targeted.sequence());
            assertEquals(ClassicWorldBowCatalog.PROJECTILE_GFX,
                    targeted.projectileGfx());
            assertEquals(0x7f, targeted.flightType());
            assertEquals(List.of(playerBow.x(), playerBow.y(), playerX, playerY),
                    List.of(targeted.sourceX(), targeted.sourceY(),
                            targeted.targetX(), targeted.targetY()));
            assertEquals(9_985, getIntField(session, "playerHp"));
            assertTrue(hasHp(hitPackets, 9_985, 10_000));
            assertTrue(monsterDamageAfter(hub, monsterHpBefore) >= 15,
                    "null-attacker L1Monster branch must be exercised");
            assertTrue(hitPackets.stream().noneMatch(packet -> packet.length > 0
                    && (packet[0] & 0xFF) == Protocol.S_EXP));

            EmptyLane empty = emptyLane(hub);
            setField(session, "positionX", empty.playerX());
            setField(session, "positionY", empty.playerY());
            setField(session, "playerHp", 10_000);
            assertTrue(hub.updateClassicWorld(
                    session, ClassicWorldBowCatalog.MAP_ID,
                    empty.playerX(), empty.playerY(), -1));
            hub.classicWorldBowTick();
            List<byte[]> emptyPackets = drain(client, receiver);
            BowPacket noTarget = findBowPacket(
                    emptyPackets, empty.bow().objectId());
            assertNotNull(noTarget);
            assertEquals(0, noTarget.targetObjectId());
            assertEquals(ClassicWorldBowCatalog.DAMAGE, noTarget.damage());
            assertEquals(List.of(
                            empty.lane().endpointX(), empty.lane().endpointY()),
                    List.of(noTarget.targetX(), noTarget.targetY()));
            assertEquals(10_000, getIntField(session, "playerHp"));

            BattleCompanion companion = BattleCompanion.tamed(
                    0x6000_5690L, 45034, 936, "Bow Target", 5,
                    30, 30, 5, 9, 15, 15,
                    empty.companionX(), empty.companionY());
            addTamedCompanion(session, companion);
            hub.classicWorldBowTick();
            List<byte[]> companionPackets = drain(client, receiver);
            BowPacket companionAttack = findBowPacket(
                    companionPackets, empty.bow().objectId());
            assertNotNull(companionAttack);
            assertEquals(companion.snapshot().objectId(),
                    companionAttack.targetObjectId());
            assertEquals(15, companion.snapshot().hp());
            assertEquals(List.of(empty.companionX(), empty.companionY()),
                    List.of(companionAttack.targetX(), companionAttack.targetY()));
        }
    }

    private record EmptyLane(
            ClassicWorldNpcCatalog.StaticNpc bow,
            ClassicWorldBowCatalog.Lane lane,
            int playerX, int playerY,
            int companionX, int companionY) {}

    private record BowPacket(
            long targetObjectId, int damage, int heading, long sequence,
            int projectileGfx, int flightType,
            int sourceX, int sourceY, int targetX, int targetY) {}

    private static EmptyLane emptyLane(GameHub hub) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldBowCatalog.MAP_ID);
        List<ClassicWorldBowCatalog.Lane> allLanes =
                ClassicWorldBowCatalog.all().stream()
                        .map(ClassicWorldBowCatalog::lane).toList();
        for (ClassicWorldNpcCatalog.StaticNpc bow : ClassicWorldBowCatalog.all()) {
            ClassicWorldBowCatalog.Lane lane = ClassicWorldBowCatalog.lane(bow);
            boolean monsterInLane = hub.classicWorldMonstersInScreen(
                            bow.mapId(), -1, bow.x(), bow.y()).stream()
                    .anyMatch(monster -> monster.present()
                            && lane.contains(monster.x(), monster.y()));
            if (monsterInLane) continue;
            int headingX = bow.heading() == 2 ? 1 : bow.heading() == 6 ? -1 : 0;
            int headingY = bow.heading() == 4 ? 1 : bow.heading() == 0 ? -1 : 0;
            int companionX = 0;
            int companionY = 0;
            for (int step = 1; step <= Math.max(map.width(), map.height()); step++) {
                int candidateX = bow.x() + headingX * step;
                int candidateY = bow.y() + headingY * step;
                if (!lane.contains(candidateX, candidateY)) break;
                if (map.standingPassable(candidateX, candidateY)
                        && !ClassicWorldNpcCatalog.occupies(
                                bow.mapId(), candidateX, candidateY)) {
                    companionX = candidateX;
                    companionY = candidateY;
                    break;
                }
            }
            if (companionX == 0) continue;
            for (int dx = -10; dx <= 10; dx++) {
                for (int dy = -10; dy <= 10; dy++) {
                    int x = bow.x() + dx;
                    int y = bow.y() + dy;
                    if (!map.standingPassable(x, y)
                            || !BattleSpellCatalog.isInNativeScreen(
                                    bow.x(), bow.y(), x, y)
                            || !BattleSpellCatalog.isInNativeScreen(
                                    x, y, bow.x(), bow.y())
                            || ClassicWorldNpcCatalog.occupies(bow.mapId(), x, y)
                            || allLanes.stream().anyMatch(
                                    candidate -> candidate.contains(x, y))) continue;
                    return new EmptyLane(
                            bow, lane, x, y, companionX, companionY);
                }
            }
        }
        throw new AssertionError("no retained empty bow lane fixture");
    }

    private static Map<Long, Integer> laneMonsterHp(GameHub hub) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (ClassicWorldNpcCatalog.StaticNpc bow : ClassicWorldBowCatalog.all()) {
            ClassicWorldBowCatalog.Lane lane = ClassicWorldBowCatalog.lane(bow);
            for (ClassicWorldMonsterPopulation.Snapshot monster
                    : hub.classicWorldMonstersInScreen(
                            bow.mapId(), -1, bow.x(), bow.y())) {
                if (monster.present() && monster.alive()
                        && lane.contains(monster.x(), monster.y())) {
                    result.putIfAbsent(monster.objectId(), monster.currentHp());
                }
            }
        }
        return result;
    }

    private static int monsterDamageAfter(
            GameHub hub, Map<Long, Integer> before) {
        int damage = 0;
        for (Map.Entry<Long, Integer> entry : before.entrySet()) {
            ClassicWorldMonsterPopulation.Snapshot after =
                    hub.classicWorldMonster(
                            ClassicWorldBowCatalog.MAP_ID, -1, entry.getKey());
            if (after != null) damage += Math.max(
                    0, entry.getValue() - after.currentHp());
        }
        return damage;
    }

    private static BowPacket findBowPacket(
            List<byte[]> packets, long bowObjectId) {
        for (byte[] packet : packets) {
            if (packet.length != 29
                    || (packet[0] & 0xFF) != Protocol.S_ATTACK) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.c() != BattleActionCodes.ATTACK
                    || reader.d() != bowObjectId) continue;
            long target = reader.d();
            int damage = reader.c();
            int heading = reader.c();
            long sequence = reader.d();
            int gfx = reader.h();
            int flight = reader.c();
            int sourceX = reader.h();
            int sourceY = reader.h();
            int targetX = reader.h();
            int targetY = reader.h();
            return new BowPacket(target, damage, heading, sequence, gfx,
                    flight, sourceX, sourceY, targetX, targetY);
        }
        return null;
    }

    private static long putObjectId(byte[] packet) {
        if (packet.length < 13
                || (packet[0] & 0xFF) != Protocol.S_PUT_OBJECT) return 0;
        Protocol.Reader reader = new Protocol.Reader(packet);
        reader.c();
        reader.h();
        reader.h();
        return reader.d();
    }

    private static boolean hasHp(List<byte[]> packets, int hp, int maximum) {
        for (byte[] packet : packets) {
            if (packet.length != 5
                    || (packet[0] & 0xFF) != Protocol.S_HIT_POINT) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.h() == hp && reader.h() == maximum) return true;
        }
        return false;
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character,
            SourceEquipmentMagicStats equipment) throws Exception {
        setField(session, "account", "test18");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7569_0001L));
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats", equipment);
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static void prepareWorld(
            ClientSession session, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", ClassicWorldBowCatalog.MAP_ID);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 10_000);
        setField(session, "playerMaxHp", 10_000);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void invokeStaticNpcRebuild(ClientSession session)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "sendClassicWorldStaticNpcs", String.class);
        method.setAccessible(true);
        method.invoke(session, "bow-test");
    }

    @SuppressWarnings("unchecked")
    private static void addTamedCompanion(
            ClientSession session, BattleCompanion companion) throws Exception {
        Field field = ClientSession.class.getDeclaredField("tamedCompanions");
        field.setAccessible(true);
        List<BattleCompanion> companions =
                (List<BattleCompanion>) field.get(session);
        companions.add(companion);
    }

    private static List<byte[]> drain(
            Socket client, Protocol.CipherState receiver) throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                packets.add(receiver.decrypt(Protocol.readFrame(
                        client.getInputStream(), 65_535)));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
    }

    private static int getIntField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
