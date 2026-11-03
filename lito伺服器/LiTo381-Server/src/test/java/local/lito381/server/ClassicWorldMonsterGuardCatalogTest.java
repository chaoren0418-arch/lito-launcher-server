package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterGuardCatalogTest {
    @Test
    void preservesElevenOrcCastleArrowTowerPointsAndSelectedTemplate() {
        List<ClassicWorldNpcCatalog.StaticNpc> rows =
                ClassicWorldMonsterGuardCatalog.all();
        assertEquals(11, rows.size());
        assertEquals(11, new HashSet<>(rows.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::objectId).toList()).size());
        assertTrue(rows.stream().allMatch(row ->
                row.mapId() == 4 && row.npcId() == 60520
                        && row.implementation().equals("L1MonsterGuard")
                        && row.gfxId() == 816 && row.level() == 25
                        && row.hp() == 10 && row.mp() == 10
                        && row.experience() == 1970 && row.heading() == 4
                        && row.movementDistance() == 0
                        && row.atkspeed() == 1880
                        && ClassicWorldNpcCatalog.npc(
                                row.mapId(), row.objectId()) == row));
        ClassicWorldNpcCatalog.StaticNpc first = rows.get(0);
        assertEquals(356, first.spawnId());
        assertEquals(32784, first.x());
        assertEquals(32321, first.y());
        assertEquals(366, rows.get(rows.size() - 1).spawnId());
        assertEquals(32764, rows.get(rows.size() - 1).x());
        assertEquals(32297, rows.get(rows.size() - 1).y());
        assertNull(ClassicWorldMonsterGuardCatalog.target(4, 0x6400_0001L));
    }

    @Test
    void combatProjectionRetainsAttackMathButCannotEnterMonsterLifecycle() {
        ClassicWorldNpcCatalog.StaticNpc tower =
                ClassicWorldMonsterGuardCatalog.all().get(0);
        ClassicWorldMonsterPopulation.Snapshot actor =
                ClassicWorldMonsterGuardCatalog.combatProjection(tower, 7);
        assertEquals(tower.objectId(), actor.objectId());
        assertEquals(7, actor.heading());
        assertEquals(10, actor.currentHp());
        assertEquals(10, actor.currentMp());
        assertEquals(-50, actor.template().armorClass());
        assertEquals(60, actor.template().strength());
        assertEquals(60, actor.template().dexterity());
        assertEquals(80, actor.template().magicResistance());
        assertEquals(10, actor.template().ranged());
        assertEquals(66, actor.template().bowActionId());
        assertEquals(1880, actor.template().atkspeed());
        assertTrue(actor.template().aggressive());
        assertFalse(actor.template().large());
        assertEquals("L1Monster", actor.template().implementation());
        assertEquals("L1MonsterGuard", tower.implementation());
        assertNull(new ClassicWorldMonsterPopulation()
                .actor(4, tower.objectId()));
    }

    @Test
    void memoryPhysicalRowMatchesSelectedDatabase() throws Exception {
        ServerConfig config = ServerConfig.load(
                java.nio.file.Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config)) {
            SourceNpcPhysicalStats stats = accounts.npcPhysicalStats(60520);
            assertNotNull(stats);
            assertEquals(-50, stats.armorClass());
            assertEquals(80, stats.magicResistance());
            assertEquals(60, stats.strength());
            assertEquals(60, stats.dexterity());
            assertEquals(10, stats.maxMp());
            assertEquals(10, stats.ranged());
            assertEquals(66, stats.bowActionId());
            assertFalse(stats.large());
            assertFalse(stats.boss());
        }
    }

    @Test
    void historicalAdjacentGlanceBoundaryIsScopedToArrowTowers() {
        ClassicWorldNpcCatalog.StaticNpc tower =
                ClassicWorldMonsterGuardCatalog.all().get(0);
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(tower.mapId());
        assertEquals(0, map.rawCell(tower.x(), tower.y()));
        assertTrue(ClassicWorldMonsterGuardCatalog.attackPosition(
                map, tower.x(), tower.y(), tower.x() + 1, tower.y(), 10));
        assertTrue(ClassicWorldMonsterGuardCatalog.attackPosition(
                map, tower.x() + 1, tower.y(), tower.x(), tower.y(), 15));
        assertFalse(ClassicWorldMonsterGuardCatalog.attackPosition(
                map, tower.x(), tower.y(), tower.x() + 2, tower.y(), 10));
        assertFalse(map.attackPosition(
                tower.x(), tower.y(), tower.x() + 1, tower.y(), 10));
    }
}
