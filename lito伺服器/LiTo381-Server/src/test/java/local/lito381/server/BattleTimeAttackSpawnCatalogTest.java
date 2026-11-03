package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

class BattleTimeAttackSpawnCatalogTest {
    @Test
    void deploysAllSixtyPlayersInDistinctCellsAroundTheCentralStartTower() {
        var feature = BattleTimeAttackSpawnCatalog.featurePoint();
        var spawns = BattleTimeAttackSpawnCatalog.spawns();

        assertEquals(540, BattleTimeAttackSpawnCatalog.MAP_ID);
        assertEquals(BattleTimeAttackEncounter.MAXIMUM_PLAYERS, spawns.size());
        assertEquals(spawns.size(), new HashSet<>(spawns).size());
        assertFalse(spawns.contains(feature));
        assertTrue(spawns.stream().allMatch(point -> {
            int dx = point.x() - feature.x();
            int dy = point.y() - feature.y();
            return dx * dx + dy * dy <= 18;
        }));
        assertEquals(new ArenaMapCatalog.Point(32737, 32799),
                BattleTimeAttackSpawnCatalog.spawn(0));
        assertEquals(BattleTimeAttackSpawnCatalog.spawn(0),
                BattleTimeAttackSpawnCatalog.spawn(60));
    }

    @Test
    void recordsEvidenceAndDoesNotInventCentralCircleServerRestrictions() {
        assertEquals(
                "GameMeca-LWC-2005-center-circle-start+"
                        + "LTmap-540-Layer3-nearest-passable-provisional-exact-server-coordinates-unrecovered",
                BattleTimeAttackSpawnCatalog.DEPLOYMENT_SOURCE);
        assertEquals(
                "GameMeca-LWC-2005-player-rule+dragon-rumor-not-proven-server-predicate",
                BattleTimeAttackSpawnCatalog.CENTRAL_ACTION_BOUNDARY_SOURCE);
        assertTrue(BattleTimeAttackSpawnCatalog.supports(540));
        assertFalse(BattleTimeAttackSpawnCatalog.supports(539));
    }

    @Test
    void keepsEncounterActorsOnDistinctSafeAnchorsOutsideTheCentralFormation() {
        var arena = ArenaMapCatalog.forMap(540);
        var playerSpawns = new HashSet<>(BattleTimeAttackSpawnCatalog.spawns());
        var actorSpawns = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> BattleTimeAttackSpawnCatalog.encounterSpawn(
                        arena, index))
                .toList();

        assertEquals(4, new HashSet<>(actorSpawns).size());
        assertTrue(actorSpawns.stream().noneMatch(playerSpawns::contains));
        assertEquals(new ArenaMapCatalog.Point(32736, 32772), actorSpawns.get(0));
        assertEquals(new ArenaMapCatalog.Point(32764, 32828), actorSpawns.get(1));
        assertEquals(new ArenaMapCatalog.Point(32737, 32829), actorSpawns.get(2));
        assertEquals(new ArenaMapCatalog.Point(32709, 32802), actorSpawns.get(3));
        assertEquals(
                "LTmap-540-S32-cardinal-safe-anchor-provisional-outside-central-deployment",
                BattleTimeAttackSpawnCatalog.ENCOUNTER_PLACEMENT_SOURCE);
    }
}
