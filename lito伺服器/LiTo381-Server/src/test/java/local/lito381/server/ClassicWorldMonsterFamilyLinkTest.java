package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterFamilyLinkTest {
    private static final int MAP_ID = 4;
    private static final long PLAYER = 0x6600_0001L;

    @Test
    void directPlayerHitDefersThenLinksIdleSameFamilyMonsterAtAttackPosition() {
        ClassicWorldMonsterCatalog.SpawnDefinition source =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .filter(definition -> definition.groupId() == 8)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.MonsterTemplate allyTemplate =
                ClassicWorldMobGroupCatalog.forId(8).minions().stream()
                        .filter(minion -> minion.npcId() == 45009)
                        .findFirst().orElseThrow().template();
        int[] cells = threeOpenHorizontalCells();
        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.retainedSourceSlice(
                        21L, 0x6700_0000L, 0x6700_1000L,
                        List.of(spawn(900001, cells[0], cells[1], source.template()),
                                spawn(900002, cells[0] + 2, cells[1], allyTemplate)));
        ClassicWorldMonsterPopulation.Snapshot attacked =
                population.actor(MAP_ID, 0x6700_0001L);
        ClassicWorldMonsterPopulation.Snapshot ally =
                population.actor(MAP_ID, 0x6700_0002L);
        assertNotNull(attacked);
        assertNotNull(ally);

        assertNotNull(population.damage(MAP_ID, attacked.objectId(), PLAYER, 1));
        assertEquals(PLAYER, population.pendingFamilyTargetObjectId(
                MAP_ID, attacked.objectId()));
        assertEquals(0, population.targetObjectId(MAP_ID, ally.objectId()));

        assertNotNull(population.attack(
                MAP_ID, attacked.objectId(), PLAYER,
                cells[0] + 1, cells[1], 0));
        assertEquals(0, population.pendingFamilyTargetObjectId(
                MAP_ID, attacked.objectId()));
        assertEquals(PLAYER, population.targetObjectId(MAP_ID, ally.objectId()));
    }

    @Test
    void groupSupportWaitsForSuccessfulPathStepAndNeverOverwritesExistingTarget() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot leader =
                population.mobGroupLeaders(MAP_ID, 8).get(0);
        List<ClassicWorldMonsterPopulation.Snapshot> minions = population
                .mobGroupMembers(MAP_ID, leader.objectId()).stream()
                .filter(member -> member.objectId() != leader.objectId()).toList();
        ClassicWorldMonsterPopulation.Snapshot reserved = minions.get(0);
        long existingTarget = 0x6600_0002L;
        population.provoke(MAP_ID, reserved.objectId(), existingTarget);

        assertNotNull(population.damage(MAP_ID, leader.objectId(), PLAYER, 1));
        assertEquals(0, minions.stream()
                .filter(member -> member.objectId() != reserved.objectId())
                .mapToLong(member -> population.targetObjectId(
                        MAP_ID, member.objectId())).sum());

        ClassicWorldMonsterPopulation.Movement movement = null;
        for (int radius = 3; radius <= 12 && movement == null; radius++) {
            for (int dx = -radius; dx <= radius && movement == null; dx++) {
                for (int dy = -radius; dy <= radius && movement == null; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    movement = population.moveToward(
                            MAP_ID, leader.objectId(), PLAYER,
                            leader.x() + dx, leader.y() + dy,
                            java.util.Set.of(), 0);
                }
            }
        }
        assertNotNull(movement, "selected leader must have one retained path step");
        assertEquals(existingTarget,
                population.targetObjectId(MAP_ID, reserved.objectId()));
        for (ClassicWorldMonsterPopulation.Snapshot minion : minions) {
            if (minion.objectId() != reserved.objectId()) {
                assertEquals(PLAYER,
                        population.targetObjectId(MAP_ID, minion.objectId()));
            }
        }
        assertNull(population.moveToward(
                MAP_ID, leader.objectId(), PLAYER,
                leader.x(), leader.y(), java.util.Set.of(), 0));
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition spawn(
            int spawnId, int x, int y,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                MAP_ID, spawnId, "family-link-fixture", 1, 0,
                x, y, 0, 0, 0, 0, 0, 0, 0,
                0, 0, true, 0, false, 0, template);
    }

    private static int[] threeOpenHorizontalCells() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int x = map.startX(); x <= map.endX() - 2; x++) {
            for (int y = map.startY(); y <= map.endY(); y++) {
                    if (map.standingPassable(x, y)
                            && map.standingPassable(x + 1, y)
                            && map.standingPassable(x + 2, y)
                            && map.attackPosition(x, y, x + 1, y, 1)
                            && map.hasLineOfSight(x + 2, y, x + 1, y)) {
                        return new int[] {x, y};
                    }
            }
        }
        throw new AssertionError("no three-cell family-link fixture near source");
    }
}
