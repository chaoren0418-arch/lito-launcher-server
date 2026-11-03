package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Production scheduler and encrypted-wire proof for retained nearTeleport. */
final class ClientSessionClassicWorldMonsterNearTeleportIntegrationTest {
    private static final int MAP_ID = 33;
    private static final long OBJECT_BASE = 0x68B0_0000L;
    private static final long PLAYER_OBJECT_ID = 0x4200_0777L;

    @TempDir Path temporary;

    @Test
    void worldSchedulerEmitsEffectRemoveThenPutForFirstFoundTeleport()
            throws Exception {
        int[] cells = openPair();
        ClassicWorldMonsterPopulation population = population(cells[0], cells[1]);
        long monsterId = OBJECT_BASE + 1;
        ClassicWorldMonsterPopulation.Snapshot before =
                population.actor(MAP_ID, monsterId);
        assertNotNull(before);
        assertTrue(population.provoke(MAP_ID, monsterId, PLAYER_OBJECT_ID));

        try (Fixture fixture = fixture(cells[2], cells[3])) {
            fixture.tick(population, 1L);

            ClassicWorldMonsterPopulation.Snapshot after =
                    population.actor(MAP_ID, monsterId);
            assertEquals(1, Math.max(Math.abs(after.x() - cells[2]),
                    Math.abs(after.y() - cells[3])));
            assertEquals(before.currentMp() - 10, after.currentMp());
            assertFalse(population.firstFound(MAP_ID, monsterId));

            String frames = Files.readString(fixture.capture().path());
            int effect = frames.indexOf(
                    "S_EFFECT classic-world-monster-near=" + monsterId
                            + " gfx=169 reason=first-found");
            int remove = frames.indexOf(
                    "S_REMOVE_OBJECT classic-world-monster-near=" + monsterId
                            + " reason=first-found", effect + 1);
            int put = frames.indexOf(
                    "S_PUT_OBJECT classic-world-monster=" + monsterId,
                    remove + 1);
            assertTrue(effect >= 0 && effect < remove && remove < put);
            assertTrue(frames.substring(put).contains(
                    "reason=monster-near-teleport-first-found"));
        }
    }

    private Fixture fixture(int targetX, int targetY) throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("near-teleport-state.properties"), false);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-near-teleport"));
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();

        ClientSession session = new ClientSession(server,
                ClientSession.Endpoint.GAME, config, null, state, capture, hub);
        setField(session, "outgoing", new Protocol.CipherState(0x4077_0777));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "monster-near-teleport");
        setField(session, "selectedCharacter", "TeleportTarget");
        setField(session, "characters", List.of(character(targetX, targetY)));
        setField(session, "playerLawful", 0);
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
                Map.of(), () -> 0x7477_0001L));
        setField(session, "playerHp", 5_000);
        setField(session, "playerMaxHp", 5_000);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);
        setField(session, "classicWorldMapId", MAP_ID);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", targetX);
        setField(session, "positionY", targetY);
        setField(session, "playerHeading", 0);
        hub.connect(session);
        hub.enterClassicWorld(session, MAP_ID, targetX, targetY, 0);
        return new Fixture(hub, capture, client, server, session);
    }

    private static ClassicWorldMonsterPopulation population(int x, int y) {
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(candidate -> candidate.npcId() == 45393)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 998_777, "near-teleport-wire-fixture", 1, 0,
                        x, y, 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                777L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                List.of(definition));
    }

    private static int[] openPair() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int y = map.startY() + 2; y <= map.endY() - 2; y++) {
            for (int x = map.startX() + 2; x <= map.endX() - 12; x++) {
                int targetX = x + 10;
                if (!map.standingPassable(x, y)
                        || ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)
                        || !map.standingPassable(targetX, y)) continue;
                for (int landingY = y - 1; landingY <= y + 1; landingY++) {
                    for (int landingX = targetX - 1;
                         landingX <= targetX + 1; landingX++) {
                        if (landingX == targetX && landingY == y) continue;
                        if (map.standingPassable(landingX, landingY)
                                && !ClassicWorldNpcCatalog.occupies(
                                        MAP_ID, landingX, landingY)) {
                            return new int[] {x, y, targetX, y};
                        }
                    }
                }
            }
        }
        throw new AssertionError("missing near-teleport wire cells");
    }

    private static CharacterSummary character(int x, int y) {
        return new CharacterSummary(
                "TeleportTarget", "", 1, 0, 50, 5_000, 100,
                0, 12, 12, 12, 8, 8, 8, 8, 8,
                PLAYER_OBJECT_ID, 0, 0, 0, 1,
                x, y, MAP_ID, 12, 40, 1, 12, 12, 0);
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

    private record Fixture(
            GameHub hub, CaptureLog capture, Socket client,
            Socket server, ClientSession session) implements AutoCloseable {
        void tick(ClassicWorldMonsterPopulation population, long nowNanos)
                throws Exception {
            Method method = GameHub.class.getDeclaredMethod(
                    "classicWorldMonsterTick", int.class, int.class,
                    ClassicWorldMonsterPopulation.class, long.class);
            method.setAccessible(true);
            method.invoke(hub, MAP_ID, -1, population, nowNanos);
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
