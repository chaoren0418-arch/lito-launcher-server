package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Encrypted-wire proof that append-only WORLD maps participate in the AI clock. */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionWorldExpandedMapMonsterAiIntegrationTest {
    private static final int MAP_ID = 304;
    private static final long PLAYER_OBJECT_ID = 0x4200_0304L;

    @TempDir Path temporary;

    @Test
    void silentCavernAggressiveMonsterAttacksThroughScheduledEncryptedWire()
            throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.packets();
            fixture.tick();
            List<byte[]> packets = fixture.packets();

            byte[] attack = packets.stream()
                    .filter(packet -> packet.length >= 10
                            && (packet[0] & 0xFF) == Protocol.S_ATTACK
                            && unsignedInt(packet, 2)
                                == fixture.candidate.actor().objectId()
                            && unsignedInt(packet, 6) == PLAYER_OBJECT_ID)
                    .findFirst().orElse(null);
            assertNotNull(attack,
                    "map304 must execute the production AI scheduler and emit S_ATTACK");
            assertEquals(16, attack.length,
                    "native melee S_ATTACK layout including the encoded damage result");
            assertEquals(BattleActionCodes.ATTACK, attack[1] & 0xFF);
        }
    }

    private Fixture fixture() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        ClassicWorldMonsterPopulation population = population(hub);
        Candidate candidate = candidate(population);
        assertEquals(45030, candidate.actor().npcId());
        assertEquals(0x6500_31D7L, candidate.actor().objectId(),
                "append-only runtime identity for the selected map304 attacker");

        GameStateStore state = new GameStateStore(
                temporary.resolve("expanded-map-ai.properties"), false);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-expanded-map-ai"));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(200);

        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x3040_0304;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "expanded-map-ai");
        setField(session, "selectedCharacter", "SilentCavernAi");
        setField(session, "characters", List.of(character(candidate.target())));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7400_0304L));
        setField(session, "playerHp", 5_000);
        setField(session, "playerMaxHp", 5_000);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", MAP_ID);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", candidate.target().x());
        setField(session, "positionY", candidate.target().y());
        setField(session, "playerHeading", 0);

        hub.connect(session);
        hub.enterClassicWorld(session, MAP_ID,
                candidate.target().x(), candidate.target().y(), 0);
        return new Fixture(hub, capture, client, server, session, receiver, candidate);
    }

    private static Candidate candidate(ClassicWorldMonsterPopulation population) {
        List<ClassicWorldMonsterPopulation.Snapshot> actors = new ArrayList<>();
        for (long ordinal = 1; ordinal <= population.size(); ordinal++) {
            ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                    MAP_ID, ClassicWorldMonsterPopulation.OBJECT_BASE + ordinal);
            if (actor != null && actor.present() && actor.alive()) actors.add(actor);
        }
        assertEquals(ClassicWorldMonsterCatalog.SILENT_CAVERN_POPULATION,
                actors.size());
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied = new HashSet<>();
        for (ClassicWorldMonsterPopulation.Snapshot actor : actors) {
            occupied.add(new ClassicWorldMonsterPopulation.GridPoint(
                    actor.x(), actor.y()));
        }

        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (ClassicWorldMonsterPopulation.Snapshot actor : actors) {
            ClassicWorldMonsterCatalog.MonsterTemplate template = actor.template();
            if (!template.aggressive() || template.atkspeed() <= 0
                    || template.attackRange() != 1
                    || !ClassicWorldMonsterPhysicalSkillCatalog
                        .forNpc(actor.npcId()).isEmpty()
                    || !ClassicWorldMonsterMagicSkillCatalog
                        .forNpcAll(actor.npcId()).isEmpty()) {
                continue;
            }
            for (int x = actor.x() - 1; x <= actor.x() + 1; x++) {
                for (int y = actor.y() - 1; y <= actor.y() + 1; y++) {
                    ClassicWorldMonsterPopulation.GridPoint target =
                            new ClassicWorldMonsterPopulation.GridPoint(x, y);
                    if ((x == actor.x() && y == actor.y())
                            || occupied.contains(target)
                            || ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)
                            || !map.standingPassable(x, y)
                            || !map.attackPosition(
                                actor.x(), actor.y(), x, y, 1)) {
                        continue;
                    }
                    return new Candidate(actor,
                            new ArenaMapCatalog.Point(x, y));
                }
            }
        }
        throw new AssertionError(
                "no source-backed map304 aggressive melee monster has an open target cell");
    }

    private static CharacterSummary character(ArenaMapCatalog.Point target) {
        return new CharacterSummary(
                "SilentCavernAi", "", 1, 0, 50, 5_000, 100,
                0, 12, 12, 12, 8, 8, 8, 8, 8,
                PLAYER_OBJECT_ID, 0, 0, 0, 1,
                target.x(), target.y(), MAP_ID,
                12, 40, 1, 12, 12, 0);
    }

    private static ClassicWorldMonsterPopulation population(GameHub hub)
            throws Exception {
        Field field = GameHub.class.getDeclaredField("classicWorldMonsters");
        field.setAccessible(true);
        return (ClassicWorldMonsterPopulation) field.get(hub);
    }

    private static long unsignedInt(byte[] packet, int offset) {
        return (packet[offset] & 0xFFL)
                | ((packet[offset + 1] & 0xFFL) << 8)
                | ((packet[offset + 2] & 0xFFL) << 16)
                | ((packet[offset + 3] & 0xFFL) << 24);
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

    private record Candidate(ClassicWorldMonsterPopulation.Snapshot actor,
                             ArenaMapCatalog.Point target) {}

    private record Fixture(
            GameHub hub, CaptureLog capture, Socket client, Socket server,
            ClientSession session, Protocol.CipherState receiver,
            Candidate candidate) implements AutoCloseable {
        void tick() throws Exception {
            Method method = GameHub.class.getDeclaredMethod(
                    "classicWorldMonsterTickSafely");
            method.setAccessible(true);
            method.invoke(hub);
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
