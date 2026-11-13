package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionClassicWorldMonsterGuardTest {
    @TempDir Path temporary;

    @Test
    void playerDamageIsIgnoredWhileAutonomousTowerUsesRetainedCooldown()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("monster-guard-state.properties"), false);
        TowerPoint fixture = rangedAttackPoint();
        ClassicWorldNpcCatalog.StaticNpc tower = fixture.tower();
        int[] playerPoint = {fixture.x(), fixture.y()};

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("monster-guard-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5680);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary character = accounts.characters("test18").get(0);
            initialize(session, config.classicServerNames().get(0), character,
                    accounts.equipmentMagicStats(character.objectId()));
            setField(session, "outgoing", new Protocol.CipherState(0x3810_5680));
            prepareWorld(session, tower.mapId(), playerPoint[0], playerPoint[1]);
            hub.connect(session);
            hub.enterClassicWorld(
                    session, tower.mapId(), playerPoint[0], playerPoint[1], -1);
            drain(client, receiver);

            invokeDirectAttack(session, tower);
            List<byte[]> receiverPackets = drain(client, receiver);
            assertTrue(hasPlayerAttack(receiverPackets,
                    session.playerObjectId(), tower));
            assertFalse(containsOpcode(receiverPackets, Protocol.S_HIT_RATIO));
            assertFalse(containsOpcode(receiverPackets, Protocol.S_REMOVE_OBJECT));
            assertFalse(containsOpcode(receiverPackets, Protocol.S_EXP));
            assertEquals(4, hub.classicWorldNpcHeading(tower));
            assertNull(hub.classicWorldMonster(4, -1, tower.objectId()));

            setField(session, "invisible", true);
            invokeTowerTick(hub, 1L);
            List<byte[]> firstVolley = drain(client, receiver);
            assertTrue(hasAttack(firstVolley,
                    tower.objectId(), session.playerObjectId()));
            assertFalse(hasHitRatioFor(firstVolley, tower.objectId()));
            assertFalse(containsOpcode(firstVolley, Protocol.S_REMOVE_OBJECT));
            assertNull(hub.classicWorldMonster(4, -1, tower.objectId()));

            invokeTowerTick(hub, TimeUnit.SECONDS.toNanos(1));
            assertFalse(hasAttack(drain(client, receiver),
                    tower.objectId(), session.playerObjectId()),
                    "1880ms retained attack clock must reject early reuse");

            invokeTowerTick(hub, Math.addExact(1L,
                    TimeUnit.MILLISECONDS.toNanos(1880)));
            assertTrue(hasAttack(drain(client, receiver),
                    tower.objectId(), session.playerObjectId()));
        }
    }

    private record TowerPoint(
            ClassicWorldNpcCatalog.StaticNpc tower, int x, int y) {}

    private static TowerPoint rangedAttackPoint() {
        int towerToPlayer = 0;
        int playerToTower = 0;
        int standing = 0;
        for (ClassicWorldNpcCatalog.StaticNpc target
                : ClassicWorldMonsterGuardCatalog.all()) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int distance = 1; distance <= 10; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != distance) continue;
                int x = target.x() + dx;
                int y = target.y() + dy;
                if (!map.standingPassable(x, y)
                        || ClassicWorldNpcCatalog.occupies(target.mapId(), x, y)) {
                    continue;
                }
                standing++;
                boolean playerAttack = ClassicWorldMonsterGuardCatalog.attackPosition(
                        map, x, y, target.x(), target.y(), 15);
                boolean towerAttack = ClassicWorldMonsterGuardCatalog.attackPosition(
                        map, target.x(), target.y(), x, y, 10);
                if (playerAttack) playerToTower++;
                if (towerAttack) towerToPlayer++;
                if (playerAttack && towerAttack) {
                    return new TowerPoint(target, x, y);
                }
            }
            }
        }
        }
        throw new AssertionError("no ranged monster-guard attack tile; standing="
                + standing + " tower-to-player=" + towerToPlayer
                + " player-to-tower=" + playerToTower);
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character,
            SourceEquipmentMagicStats equipment)
            throws Exception {
        setField(session, "account", "test18");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7568_0001L));
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats",
                equipment);
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
            ClientSession session, int mapId, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 10_000);
        setField(session, "playerMaxHp", 10_000);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeDirectAttack(
            ClientSession session, ClassicWorldNpcCatalog.StaticNpc target)
            throws Exception {
        Class envelopeType = Class.forName(
                ClientSession.class.getName() + "$AttackEnvelope");
        Object direct = Enum.valueOf(envelopeType, "FAR");
        Method method = ClientSession.class.getDeclaredMethod(
                "attackClassicWorld", Protocol.AttackRequest.class, envelopeType);
        method.setAccessible(true);
        method.invoke(session, new Protocol.AttackRequest(
                target.objectId(), target.x(), target.y()), direct);
    }

    private static void invokeTowerTick(GameHub hub, long nowNanos)
            throws Exception {
        Method method = GameHub.class.getDeclaredMethod(
                "classicWorldMonsterGuardTick", long.class);
        method.setAccessible(true);
        method.invoke(hub, nowNanos);
    }

    private static boolean hasAttack(
            List<byte[]> packets, long attacker, long target) {
        for (byte[] packet : packets) {
            if (packet.length < 10 || (packet[0] & 0xFF) != Protocol.S_ATTACK) {
                continue;
            }
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            if (reader.d() == attacker && reader.d() == target) return true;
        }
        return false;
    }

    private static boolean hasPlayerAttack(
            List<byte[]> packets, long attacker,
            ClassicWorldNpcCatalog.StaticNpc target) {
        for (byte[] packet : packets) {
            if (packet.length < 25 || (packet[0] & 0xFF) != Protocol.S_ATTACK) {
                continue;
            }
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            if (reader.d() != attacker) continue;
            long targetMarker = reader.d();
            reader.c();
            reader.c();
            reader.d();
            reader.h();
            reader.c();
            reader.h();
            reader.h();
            int targetX = reader.h();
            int targetY = reader.h();
            if ((targetMarker == 0 || targetMarker == target.objectId())
                    && targetX == target.x() && targetY == target.y()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOpcode(List<byte[]> packets, int opcode) {
        return packets.stream().anyMatch(packet -> packet.length > 0
                && (packet[0] & 0xFF) == opcode);
    }

    private static boolean hasHitRatioFor(List<byte[]> packets, long objectId) {
        for (byte[] packet : packets) {
            if (packet.length < 5
                    || (packet[0] & 0xFF) != Protocol.S_HIT_RATIO) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.d() == objectId) return true;
        }
        return false;
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
