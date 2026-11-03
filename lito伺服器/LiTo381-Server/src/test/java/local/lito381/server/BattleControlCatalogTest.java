package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleControlCatalogTest {
    @Test
    void builtInPlayerSpawnSeedCoversEveryNativeBattleMap() {
        var rows = BattleControlCatalog.defaultPlayerSpawns();

        assertEquals(380, rows.size());
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int mapId : mode.maps()) {
                var selected = rows.stream()
                        .filter(row -> row.modeId() == mode.wireId()
                                && row.mapId() == mapId)
                        .toList();
                assertFalse(selected.isEmpty(), () -> "missing " + mode + "/" + mapId);
                for (int index = 0; index < selected.size(); index++) {
                    assertEquals(index, selected.get(index).spawnIndex());
                    assertTrue(ArenaMapCatalog.forMap(mapId).nativeStandingFloor(
                            selected.get(index).x(), selected.get(index).y()));
                }
            }
        }
    }

    @Test
    void databaseRuleOverridesTimerGoalAndMinimumPlayers() {
        BattleControlCatalog controls = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(List.of(
                        new RepositoryBattleRule(1, 564, true, 42, 7, 3)),
                        List.of(), List.of()));
        BattleRules.RuleSet rules = BattleRules.decode(
                new GameHub.RoomSettings("DB", "", 8, 1, 0, 0, 0, 0),
                564, controls);
        assertEquals(42, rules.timeLimitSeconds());
        assertEquals(7, rules.goal());
        assertEquals(3, rules.minimumPlayers());
    }

    @Test
    void databaseCanDisableOneNativeModeMapPair() {
        BattleControlCatalog controls = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(List.of(
                        new RepositoryBattleRule(1, 564, false, 180, 1, 2)),
                        List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> BattleRules.decode(
                new GameHub.RoomSettings("DB", "", 8, 1, 0, 0, 0, 0),
                564, controls));
    }

    @Test
    void databaseObjectiveControlsNativeFloorCoordinateGfxAndHp() {
        BattleControlCatalog controls = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(List.of(), List.of(
                        new RepositoryBattleObject(5, 510, "honor-red",
                                32799, 32796, BattleGuardianTower.GFX_EARTH, 4321)),
                        List.of()));
        BattleRules.RuleSet rules = BattleRules.decode(
                new GameHub.RoomSettings("DB", "", 8, 5, 0, 0, 0, 0),
                510, controls);
        BattleGuardianTowers towers = new BattleGuardianTowers();
        towers.reset(rules, ArenaMapCatalog.forMap(510), -1, controls);
        BattleGuardianTower.Snapshot red = towers.snapshots().stream()
                .filter(tower -> tower.force() == 0).findFirst().orElseThrow();
        assertEquals(32799, red.x());
        assertEquals(32796, red.y());
        assertEquals(BattleGuardianTower.GFX_EARTH, red.gfx());
        assertEquals(4321, red.maxHp());
        assertEquals("lito_static.lito_battle_object", red.placementSource());
    }

    @Test
    void databaseQuestRowsControlEveryWaveAndSpawnPublication() {
        List<RepositoryBattleSpawn> rows = List.of(
                spawn(1, 45262), spawn(2, 45207),
                spawn(3, 45259), spawn(4, 45390));
        BattleControlCatalog controls = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(List.of(), List.of(), rows));
        BattleRules.RuleSet rules = BattleRules.decode(
                new GameHub.RoomSettings("DB", "", 8, 2, 0, 0, 0, 0),
                513, controls);
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules, ArenaMapCatalog.forMap(513), controls);
        assertEquals(2, encounter.snapshots().size());
        assertTrue(encounter.snapshots().stream().allMatch(actor ->
                actor.npcId() == 45262
                        && actor.placementSource().equals(
                                BattleQuestEncounter.DATABASE_SPAWN_SOURCE)));
    }

    @Test
    void rejectsBlockedObjectiveAndIncompleteQuestWaveSet() {
        assertThrows(IllegalArgumentException.class, () ->
                BattleControlCatalog.fromRepository(new RepositoryBattleConfiguration(
                        List.of(), List.of(new RepositoryBattleObject(
                                5, 510, "honor-red", 1, 1, 2665, 3000)), List.of())));
        BattleControlCatalog incomplete = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(List.of(), List.of(),
                        List.of(spawn(1, 45262))));
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        BattleRules.RuleSet rules = BattleRules.decode(
                new GameHub.RoomSettings("DB", "", 8, 2, 0, 0, 0, 0), 513);
        assertThrows(IllegalArgumentException.class, () -> encounter.reset(
                rules, ArenaMapCatalog.forMap(513), incomplete));
    }

    @Test
    void databasePlayerSpawnsOverrideBuiltInDeploymentAndWrapByIndex() {
        var first = new RepositoryBattlePlayerSpawn(
                1, 564, 0, 32729, 32797);
        var second = new RepositoryBattlePlayerSpawn(
                1, 564, 1, 32741, 32801);
        BattleControlCatalog controls = BattleControlCatalog.fromRepository(
                new RepositoryBattleConfiguration(
                        List.of(), List.of(), List.of(), List.of(first, second)));
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(564);

        assertEquals(new ArenaMapCatalog.Point(first.x(), first.y()),
                controls.playerSpawn(BattleRules.Mode.DEATHMATCH, arena, 0));
        assertEquals(new ArenaMapCatalog.Point(second.x(), second.y()),
                controls.playerSpawn(BattleRules.Mode.DEATHMATCH, arena, 1));
        assertEquals(new ArenaMapCatalog.Point(first.x(), first.y()),
                controls.playerSpawn(BattleRules.Mode.DEATHMATCH, arena, 2));
        assertEquals(2, controls.configuredPlayerSpawnRowCount());
    }

    @Test
    void rejectsPlayerSpawnGapsBlockedCellsAndDuplicateCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                BattleControlCatalog.fromRepository(
                        new RepositoryBattleConfiguration(
                                List.of(), List.of(), List.of(), List.of(
                                new RepositoryBattlePlayerSpawn(
                                        1, 564, 1, 32729, 32797)))));
        assertThrows(IllegalArgumentException.class, () ->
                BattleControlCatalog.fromRepository(
                        new RepositoryBattleConfiguration(
                                List.of(), List.of(), List.of(), List.of(
                                new RepositoryBattlePlayerSpawn(1, 564, 0, 1, 1)))));
        assertThrows(IllegalArgumentException.class, () ->
                BattleControlCatalog.fromRepository(
                        new RepositoryBattleConfiguration(
                                List.of(), List.of(), List.of(), List.of(
                                new RepositoryBattlePlayerSpawn(
                                        1, 564, 0, 32729, 32797),
                                new RepositoryBattlePlayerSpawn(
                                        1, 564, 1, 32729, 32797)))));
    }

    private static RepositoryBattleSpawn spawn(int wave, int npcId) {
        return new RepositoryBattleSpawn(2, 0, wave, 0, npcId, 1, null, null);
    }
}
