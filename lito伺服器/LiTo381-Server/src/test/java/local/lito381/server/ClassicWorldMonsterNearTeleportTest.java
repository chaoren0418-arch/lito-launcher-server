package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterNearTeleportTest {
    private static final int MAP_ID = 33;
    private static final long OBJECT_BASE = 0x68A0_0000L;
    private static final long TARGET = 0x4200_0777L;

    @Test
    void firstFoundTeleportsAtFourToNineteenWithoutMinimumMp() {
        Fixture fixture = fixture(45393);
        ClassicWorldMonsterPopulation population = fixture.population();
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, TARGET));
        ClassicWorldMonsterPopulation.Snapshot full =
                population.actor(MAP_ID, objectId);
        assertNotNull(population.drainMana(
                MAP_ID, objectId, full.currentMp() - 5));

        ClassicWorldMonsterPopulation.NearTeleport teleport =
                population.nearTeleport(
                        MAP_ID, objectId, TARGET,
                        fixture.target().x(), fixture.target().y(),
                        Set.of(new ClassicWorldMonsterPopulation.GridPoint(
                                fixture.target().x(), fixture.target().y())),
                        1L, 100, 1);

        assertNotNull(teleport);
        assertEquals(ClassicWorldMonsterPopulation.NearTeleportKind.FIRST_FOUND,
                teleport.kind());
        assertEquals(5, teleport.mpDebit());
        assertEquals(0, teleport.after().currentMp());
        assertEquals(1, chebyshev(teleport.after().x(), teleport.after().y(),
                fixture.target().x(), fixture.target().y()));
        assertFalse(population.firstFound(MAP_ID, objectId));
    }

    @Test
    void laterPursuitRequiresMpDistanceAndTwentyPercentRoll() {
        Fixture fixture = fixture(45393);
        ClassicWorldMonsterPopulation population = fixture.population();
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, TARGET));
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(
                        fixture.target().x(), fixture.target().y()));
        ClassicWorldMonsterPopulation.NearTeleport first =
                population.nearTeleport(
                        MAP_ID, objectId, TARGET,
                        fixture.target().x(), fixture.target().y(),
                        occupied, 1L, 100, 1);
        assertNotNull(first);
        Point secondTarget = targetFor(population, first.after(), 10);
        Set<ClassicWorldMonsterPopulation.GridPoint> secondOccupied = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(
                        secondTarget.x(), secondTarget.y()));

        assertNull(population.nearTeleport(
                MAP_ID, objectId, TARGET, secondTarget.x(), secondTarget.y(),
                secondOccupied, 2L, 21, 1));
        ClassicWorldMonsterPopulation.NearTeleport random =
                population.nearTeleport(
                        MAP_ID, objectId, TARGET,
                        secondTarget.x(), secondTarget.y(), secondOccupied,
                        3L, 20, 1);

        assertNotNull(random);
        assertEquals(ClassicWorldMonsterPopulation.NearTeleportKind.RANDOM_PURSUIT,
                random.kind());
        assertEquals(19, random.randomRoll());
        assertEquals(10, random.mpDebit());
        assertFalse(population.firstFound(MAP_ID, objectId));
    }

    @Test
    void unavailableEightTileRingKeepsFirstFoundAndMana() {
        Fixture fixture = fixture(45451);
        ClassicWorldMonsterPopulation population = fixture.population();
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, TARGET));
        ClassicWorldMonsterPopulation.Snapshot before =
                population.actor(MAP_ID, objectId);
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied =
                new LinkedHashSet<>();
        occupied.add(new ClassicWorldMonsterPopulation.GridPoint(
                fixture.target().x(), fixture.target().y()));
        for (int y = fixture.target().y() - 1;
             y <= fixture.target().y() + 1; y++) {
            for (int x = fixture.target().x() - 1;
                 x <= fixture.target().x() + 1; x++) {
                if (x != fixture.target().x() || y != fixture.target().y()) {
                    occupied.add(new ClassicWorldMonsterPopulation.GridPoint(x, y));
                }
            }
        }

        assertNull(population.nearTeleport(
                MAP_ID, objectId, TARGET,
                fixture.target().x(), fixture.target().y(),
                Set.copyOf(occupied), 1L, 100, 1));
        assertTrue(population.firstFound(MAP_ID, objectId));
        assertEquals(before.currentMp(),
                population.actor(MAP_ID, objectId).currentMp());
    }

    @Test
    void successfulFirstFoundFlagSurvivesRespawnLikeRetainedObject() {
        Fixture fixture = fixture(45400);
        ClassicWorldMonsterPopulation population = fixture.population();
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, TARGET));
        assertNotNull(population.nearTeleport(
                MAP_ID, objectId, TARGET,
                fixture.target().x(), fixture.target().y(),
                Set.of(new ClassicWorldMonsterPopulation.GridPoint(
                        fixture.target().x(), fixture.target().y())),
                1L, 100, 1));
        ClassicWorldMonsterPopulation.Snapshot alive =
                population.actor(MAP_ID, objectId);
        assertTrue(population.damage(
                MAP_ID, objectId, TARGET, alive.currentHp()).diedNow());
        assertNotNull(population.removeCorpse(MAP_ID, objectId));
        assertNotNull(population.respawn(MAP_ID, objectId));
        assertFalse(population.firstFound(MAP_ID, objectId));
    }

    @Test
    void databaseDisabledTemplateCannotInheritTeleport() {
        Fixture fixture = fixture(45278);
        ClassicWorldMonsterPopulation population = fixture.population();
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, TARGET));
        assertNull(population.nearTeleport(
                MAP_ID, objectId, TARGET,
                fixture.target().x(), fixture.target().y(),
                Set.of(), 1L, 1, 1));
        assertTrue(population.firstFound(MAP_ID, objectId));
    }

    private static Fixture fixture(int npcId) {
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldMapCatalog.supportedMapIds().stream()
                        .flatMap(mapId -> ClassicWorldMonsterCatalog
                                .definitions(mapId).stream())
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(candidate -> candidate.npcId() == npcId)
                        .findFirst().orElseThrow();
        int[] cells = openPair();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 997_777, "near-teleport-fixture", 1, 0,
                        cells[0], cells[1], 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.retainedSourceSlice(
                        777L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                        List.of(definition));
        return new Fixture(population, new Point(cells[2], cells[3]));
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
        throw new AssertionError("missing near-teleport fixture cells");
    }

    private static Point targetFor(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot actor, int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int y = map.startY() + 1; y <= map.endY() - 1; y++) {
            for (int x = map.startX() + 1; x <= map.endX() - 1; x++) {
                if (chebyshev(actor.x(), actor.y(), x, y) != distance
                        || !map.standingPassable(x, y)) continue;
                for (int landingY = y - 1; landingY <= y + 1; landingY++) {
                    for (int landingX = x - 1; landingX <= x + 1; landingX++) {
                        if (landingX == x && landingY == y) continue;
                        if (map.standingPassable(landingX, landingY)
                                && !population.occupies(
                                        MAP_ID, landingX, landingY)
                                && !ClassicWorldNpcCatalog.occupies(
                                        MAP_ID, landingX, landingY)) {
                            return new Point(x, y);
                        }
                    }
                }
            }
        }
        throw new AssertionError("missing second near-teleport target");
    }

    private static int chebyshev(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private record Point(int x, int y) {}
    private record Fixture(
            ClassicWorldMonsterPopulation population, Point target) {}
}
