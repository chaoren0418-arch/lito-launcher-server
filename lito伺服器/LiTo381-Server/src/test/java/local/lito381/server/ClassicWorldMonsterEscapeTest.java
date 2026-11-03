package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Exact retained L1NpcInstance.ascape state and movement semantics. */
final class ClassicWorldMonsterEscapeTest {
    private static final int[] HEADING_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    @Test
    void selectedSourceHasExactEscapeActorInventory() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> rows =
                ClassicWorldMapCatalog.supportedMapIds().stream()
                        .flatMap(map -> ClassicWorldMonsterCatalog
                                .definitions(map).stream())
                        .filter(row -> row.template().atkspeed() == 0
                                && row.template().passispeed() > 0)
                        .toList();
        assertEquals(85, rows.size());
        assertEquals(754, rows.stream().mapToInt(
                ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(Set.of(0, 4, 63), rows.stream().map(
                ClassicWorldMonsterCatalog.SpawnDefinition::mapId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new)));
        assertEquals(Set.of(
                45010, 45012, 45015, 45021, 45025, 45077, 45154,
                46048, 46049, 46050, 46051, 46052, 70984),
                rows.stream().map(row -> row.template().npcId())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void escapeActorUsesExactReverseHeadingAndKeepsTargetInsideSeventeenTiles() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        EscapeStep step = directEscapeStep(population, false);
        assertEquals((step.towardHeading() + 4) & 7,
                step.movement().after().heading());
        assertTrue(distance(step.movement().after(), step.targetX(), step.targetY())
                > distance(step.movement().before(), step.targetX(), step.targetY()));
        assertEquals(step.playerObjectId(), population.targetObjectId(
                step.movement().after().mapId(),
                step.movement().after().objectId()));
    }

    @Test
    void escapeActorClearsTargetBeyondSeventeenTiles() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot actor = escapeActor(population);
        long playerObjectId = 0x4200_1760L;
        assertTrue(population.provoke(
                actor.mapId(), actor.objectId(), playerObjectId));
        assertNull(population.moveAway(
                actor.mapId(), actor.objectId(), playerObjectId,
                actor.x() + 18, actor.y(), Set.of(), 1L));
        assertEquals(0, population.targetObjectId(
                actor.mapId(), actor.objectId()));
    }

    @Test
    void darknessShrinksEscapeDistanceToOneButDoesNotPreventAdjacentStep() {
        ClassicWorldMonsterPopulation distantPopulation =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot distant =
                escapeActor(distantPopulation);
        long now = 10L;
        long distantPlayer = 0x4200_0200L;
        assertTrue(distantPopulation.blind(
                distant.mapId(), distant.objectId(), now, 60));
        assertTrue(distantPopulation.provoke(
                distant.mapId(), distant.objectId(), distantPlayer));
        assertNull(distantPopulation.moveAway(
                distant.mapId(), distant.objectId(), distantPlayer,
                distant.x() + 2, distant.y(), Set.of(), now));
        assertEquals(0, distantPopulation.targetObjectId(
                distant.mapId(), distant.objectId()));

        ClassicWorldMonsterPopulation adjacentPopulation =
                new ClassicWorldMonsterPopulation(21L);
        EscapeStep adjacent = directEscapeStep(adjacentPopulation, true);
        assertNotNull(adjacent.movement());
        assertTrue(adjacentPopulation.blindActive(
                adjacent.movement().after().mapId(),
                adjacent.movement().after().objectId(), adjacent.nowNanos()));
        assertEquals(adjacent.playerObjectId(), adjacentPopulation.targetObjectId(
                adjacent.movement().after().mapId(),
                adjacent.movement().after().objectId()));
    }

    @Test
    void zeroAttackAndZeroPassiveSpeedActorRemainsInert() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                if (definition.template().atkspeed() != 0
                        || definition.template().passispeed() != 0) continue;
                ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                        mapId, ClassicWorldMonsterPopulation.OBJECT_BASE
                                + ordinal(mapId, definition.spawnId(), 1));
                if (actor == null || !actor.present() || !actor.alive()) continue;
                long playerObjectId = 0x4200_0003L;
                assertTrue(population.provoke(
                        mapId, actor.objectId(), playerObjectId));
                assertNull(population.moveAway(
                        mapId, actor.objectId(), playerObjectId,
                        actor.x() + 1, actor.y(), Set.of(), 1L));
                ClassicWorldMonsterPopulation.Snapshot unchanged =
                        population.actor(mapId, actor.objectId());
                assertEquals(actor.x(), unchanged.x());
                assertEquals(actor.y(), unchanged.y());
                return;
            }
        }
        throw new AssertionError("missing inert atkspeed/passispeed zero actor");
    }

    private static EscapeStep directEscapeStep(
            ClassicWorldMonsterPopulation population, boolean blind) {
        long now = 1_000_000_000L;
        long playerObjectId = blind ? 0x4200_0002L : 0x4200_0001L;
        for (int mapId : List.of(0, 4, 63)) {
            Set<ClassicWorldMonsterPopulation.GridPoint> occupied =
                    occupied(population, mapId);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                if (definition.template().atkspeed() != 0
                        || definition.template().passispeed() <= 0) continue;
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId,
                                    ClassicWorldMonsterPopulation.OBJECT_BASE
                                            + ordinal(mapId,
                                                definition.spawnId(), slot));
                    if (actor == null || !actor.present() || !actor.alive()) continue;
                    for (int toward = 0; toward < 8; toward++) {
                        int targetX = actor.x() + HEADING_X[toward];
                        int targetY = actor.y() + HEADING_Y[toward];
                        int reverse = (toward + 4) & 7;
                        int escapeX = actor.x() + HEADING_X[reverse];
                        int escapeY = actor.y() + HEADING_Y[reverse];
                        if (!map.canStep(actor.x(), actor.y(), escapeX, escapeY)
                                || occupied.contains(
                                    new ClassicWorldMonsterPopulation.GridPoint(
                                            targetX, targetY))
                                || occupied.contains(
                                    new ClassicWorldMonsterPopulation.GridPoint(
                                            escapeX, escapeY))
                                || ClassicWorldNpcCatalog.occupies(
                                    mapId, targetX, targetY)
                                || ClassicWorldNpcCatalog.occupies(
                                    mapId, escapeX, escapeY)) continue;
                        if (blind) {
                            assertTrue(population.blind(
                                    mapId, actor.objectId(), now, 60));
                        }
                        assertTrue(population.provoke(
                                mapId, actor.objectId(), playerObjectId));
                        ClassicWorldMonsterPopulation.Movement movement =
                                population.moveAway(
                                        mapId, actor.objectId(), playerObjectId,
                                        targetX, targetY,
                                        Set.of(new ClassicWorldMonsterPopulation.GridPoint(
                                                targetX, targetY)), now);
                        if (movement != null
                                && movement.after().heading() == reverse) {
                            return new EscapeStep(movement, toward,
                                    targetX, targetY, playerObjectId, now);
                        }
                    }
                }
            }
        }
        throw new AssertionError("no direct retained escape step available");
    }

    private static ClassicWorldMonsterPopulation.Snapshot escapeActor(
            ClassicWorldMonsterPopulation population) {
        for (int mapId : List.of(0, 4, 63)) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                if (definition.template().atkspeed() != 0
                        || definition.template().passispeed() <= 0) continue;
                ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                        mapId, ClassicWorldMonsterPopulation.OBJECT_BASE
                                + ordinal(mapId, definition.spawnId(), 1));
                if (actor != null && actor.present() && actor.alive()) return actor;
            }
        }
        throw new AssertionError("missing retained escape actor");
    }

    private static Set<ClassicWorldMonsterPopulation.GridPoint> occupied(
            ClassicWorldMonsterPopulation population, int mapId) {
        Set<ClassicWorldMonsterPopulation.GridPoint> result = new HashSet<>();
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(mapId)) {
            for (int slot = 1; slot <= definition.count(); slot++) {
                ClassicWorldMonsterPopulation.Snapshot actor = population.actor(
                        mapId, ClassicWorldMonsterPopulation.OBJECT_BASE
                                + ordinal(mapId, definition.spawnId(), slot));
                if (actor != null && actor.present()) {
                    result.add(new ClassicWorldMonsterPopulation.GridPoint(
                            actor.x(), actor.y()));
                }
            }
        }
        return result;
    }

    private static int ordinal(int mapId, int spawnId, int slot) {
        int result = slot;
        for (int candidateMapId : ClassicWorldMapCatalog.supportedMapIds()) {
            if (candidateMapId == mapId) {
                for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                        : ClassicWorldMonsterCatalog.definitions(candidateMapId)) {
                    if (definition.spawnId() == spawnId) return result;
                    result += definition.count();
                }
                break;
            }
            result += ClassicWorldMonsterCatalog.definitions(candidateMapId).stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
        }
        throw new IllegalArgumentException(
                "unknown map/spawn " + mapId + ":" + spawnId);
    }

    private static int distance(
            ClassicWorldMonsterPopulation.Snapshot actor, int x, int y) {
        return Math.max(Math.abs(actor.x() - x), Math.abs(actor.y() - y));
    }

    private record EscapeStep(
            ClassicWorldMonsterPopulation.Movement movement,
            int towardHeading, int targetX, int targetY,
            long playerObjectId, long nowNanos) {}
}
