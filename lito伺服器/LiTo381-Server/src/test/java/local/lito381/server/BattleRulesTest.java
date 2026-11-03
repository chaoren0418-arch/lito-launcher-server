package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class BattleRulesTest {
    @Test void battleBootstrapResolvesWorldBeforeStartingNativeArena() {
        Protocol.BattleBootstrap packets = Protocol.battleBootstrap(564, 180);
        assertArrayEquals(new byte[] {0x28, 0x34, 0x02, 0x00}, packets.world());
        assertArrayEquals(new byte[] {(byte) 0xD2}, packets.start());
        assertArrayEquals(new byte[] {(byte) 0xE9, (byte) 0xB4, 0x00, 0x00, 0x00},
                packets.timeLimit());
    }

    @Test void readyStateAlwaysNamesAnExistingRoomParticipant() {
        assertArrayEquals(new byte[] {(byte) 0xD1, 'T', 'e', 's', 't', 0x00, 0x01},
                Protocol.readyState("Test", true));
        assertThrows(IllegalArgumentException.class, () -> Protocol.readyState("", true));
    }

    @Test void decodesEveryNativeModeWithAnIndependentMapSelection() {
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            GameHub.RoomSettings settings = new GameHub.RoomSettings(
                    "mode", "", 8, mode.wireId(), 3, 2, 7, 31, List.of(1));
            int selectedMap = mode.maps().get(Math.min(1, mode.maps().size() - 1));
            BattleRules.RuleSet rules = BattleRules.decode(settings, selectedMap);
            assertEquals(mode, rules.mode());
            assertEquals(selectedMap, rules.mapId());
            assertEquals(mode.maps(), rules.mapPool());
            assertFalse(rules.objective().isBlank());
        }
    }

    @Test void eventWinnerAndServerFieldsNeverOverrideMapOrBattleDuration() {
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "event", "", 8, BattleRules.Mode.DEATHMATCH.wireId(),
                7, 2, 127, 1, List.of(1, 128, 255));
        BattleRules.RuleSet rules = BattleRules.decode(settings, 534);
        assertEquals(534, rules.mapId());
        assertEquals(180, rules.timeLimitSeconds());
        assertEquals(127, settings.winnerCount());
        assertEquals(List.of(1, 128, 255), settings.allowedServerIds());
        assertThrows(IllegalArgumentException.class,
                () -> BattleRules.decode(settings, 565));
    }

    @Test void nativeModeOrderMatchesTheProtected381cRoomUi() {
        assertEquals(BattleRules.Mode.PET_RACE, BattleRules.Mode.fromWire(0));
        assertEquals(BattleRules.Mode.DEATHMATCH, BattleRules.Mode.fromWire(1));
        assertEquals(BattleRules.Mode.QUEST_BATTLE, BattleRules.Mode.fromWire(2));
        assertEquals(BattleRules.Mode.SIEGE_WAR, BattleRules.Mode.fromWire(3));
        assertEquals(BattleRules.Mode.TIME_ATTACK, BattleRules.Mode.fromWire(4));
        assertEquals(BattleRules.Mode.HONOR_MATCH, BattleRules.Mode.fromWire(5));
        assertEquals(BattleRules.Mode.TOWN, BattleRules.Mode.fromWire(6));
        assertEquals(BattleRules.Mode.QUEST_BATTLE, BattleRules.Mode.stepNative(
                5, Protocol.LitoModeStepDirection.NEXT));
        assertEquals(BattleRules.Mode.SIEGE_WAR, BattleRules.Mode.stepNative(
                0, Protocol.LitoModeStepDirection.PREVIOUS));
        assertThrows(IllegalArgumentException.class,
                () -> BattleRules.Mode.stepNative(
                        6, Protocol.LitoModeStepDirection.NEXT));
        assertThrows(IllegalArgumentException.class,
                () -> BattleRules.Mode.fromWire(7));
        assertEquals(420, BattleRules.decode(new GameHub.RoomSettings(
                "time", "", 20, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of())).timeLimitSeconds());
    }

    @Test void nativeMapFamiliesMatchLtmapstrCategoryParser() {
        assertEquals(List.of(554, 553, 513, 527, 518, 519, 520, 521, 522),
                BattleRules.Mode.QUEST_BATTLE.maps());
        assertEquals(List.of(573, 563, 547), BattleRules.Mode.SIEGE_WAR.maps());
        assertEquals(List.of(510, 528, 532, 512, 517, 524, 530, 535),
                BattleRules.Mode.HONOR_MATCH.maps());
        assertFalse(BattleRules.Mode.SIEGE_WAR.maps().contains(549));
        assertFalse(BattleRules.Mode.SIEGE_WAR.maps().contains(550));
        assertFalse(BattleRules.Mode.SIEGE_WAR.maps().contains(562));
        assertFalse(BattleRules.Mode.SIEGE_WAR.maps().contains(574));
    }

    @Test void questObjectiveTextTracksTheSelectedHistoricalMapFamily() {
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "quest", "", 8, BattleRules.Mode.QUEST_BATTLE.wireId(),
                0, 0, 0, 0, List.of());
        assertEquals("clear-each-monster-room+destroy-each-room-symbol",
                BattleRules.decode(settings, 513).objective());
        assertEquals("clear-each-monster-room+destroy-each-room-symbol",
                BattleRules.decode(settings, 527).objective());
        assertEquals("clear-four-monster-rooms+destroy-final-guardian-tower",
                BattleRules.decode(settings, 554).objective());
    }

    @Test void selectedTaiwanRoomCapacityIsTwoThroughOneHundredPlayers() {
        assertFalse(GameHub.nativeRoomCapacity(1));
        assertTrue(GameHub.nativeRoomCapacity(2));
        assertTrue(GameHub.nativeRoomCapacity(30));
        assertTrue(GameHub.nativeRoomCapacity(100));
        assertFalse(GameHub.nativeRoomCapacity(101));
        GameHub.RoomSettings deathmatch = new GameHub.RoomSettings(
                "dm", "", 100, BattleRules.Mode.DEATHMATCH.wireId(),
                0, 0, 0, 0, List.of());
        GameHub.RoomSettings petRace = new GameHub.RoomSettings(
                "race", "", 100, BattleRules.Mode.PET_RACE.wireId(),
                0, 0, 0, 0, List.of());
        GameHub.RoomSettings timeAttack = new GameHub.RoomSettings(
                "ta", "", 100, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of());
        GameHub.RoomSettings siege = new GameHub.RoomSettings(
                "siege", "", 100, BattleRules.Mode.SIEGE_WAR.wireId(),
                0, 0, 0, 0, List.of());
        assertEquals(100, GameHub.nativeCombatantCapacity(deathmatch));
        assertEquals(10, GameHub.nativeCombatantCapacity(petRace));
        assertEquals(60, GameHub.nativeCombatantCapacity(timeAttack));
        assertEquals(100, GameHub.nativeCombatantCapacity(siege));
        assertEquals(30, BattleRules.decode(siege).minimumPlayers());
        assertEquals(15, BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE);
        assertEquals(50, BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE);
    }

    @Test void deathmatchEliminatesAnEntireForceWithoutRespawn() {
        BattleRules.MatchState match = state(BattleRules.Mode.DEATHMATCH);
        match.begin(List.of(1L, 2L, 3L, 4L), Map.of(1L, 0, 2L, 0, 3L, 1, 4L, 1), 0);
        BattleRules.Resolution first = match.playerDefeated(1, 3);
        assertFalse(first.ended());
        assertFalse(first.respawn());
        BattleRules.Resolution result = match.playerDefeated(2, 4);
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(1L, 2L), result.winners());
        assertEquals("deathmatch-opposing-team-eliminated", result.reason());
        BattleRules.Snapshot snapshot = match.snapshot(0);
        assertEquals(Map.of(1L, 1, 2L, 1), snapshot.playerScores());
        assertEquals(Map.of(3L, 1, 4L, 1), snapshot.playerDeaths());
    }

    @Test void duplicateFatalTransitionCannotInflateNativeResultStatistics() {
        BattleRules.MatchState match = state(BattleRules.Mode.HONOR_MATCH);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);

        assertTrue(match.playerDefeated(1L, 2L).respawn());
        BattleRules.Resolution duplicate = match.playerDefeated(1L, 2L);
        assertFalse(duplicate.ended());
        assertFalse(duplicate.respawn());
        assertEquals("ignored-duplicate-player-death", duplicate.reason());
        assertEquals(Map.of(1L, 1), match.snapshot(0).playerScores());
        assertEquals(Map.of(2L, 1), match.snapshot(0).playerDeaths());

        match.respawn(2L);
        assertTrue(match.playerDefeated(1L, 2L).respawn());
        assertEquals(Map.of(1L, 2), match.snapshot(0).playerScores());
        assertEquals(Map.of(2L, 2), match.snapshot(0).playerDeaths());
    }

    @Test void honorMatchRespawnsPlayersAndEndsAtGuardianTower() {
        BattleRules.MatchState match = state(BattleRules.Mode.HONOR_MATCH);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        BattleRules.Resolution death = match.playerDefeated(1, 2);
        assertFalse(death.ended());
        assertTrue(death.respawn());
        BattleRules.Resolution result = match.guardianTowerDestroyed(0);
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(1L), result.winners());
        assertEquals("honormatch-guardian-tower-destroyed", result.reason());
    }

    @Test void questRoomsDoNotReplaceTheFinalSymbolObjective() {
        BattleRules.MatchState quest = state(BattleRules.Mode.QUEST_BATTLE);
        quest.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        assertFalse(quest.waveCleared(0).ended());
        assertFalse(quest.waveCleared(0).ended());
        BattleRules.Resolution result = quest.guardianTowerDestroyed(0);
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(1L), result.winners());
        assertEquals("questbattle-final-guardian-tower-destroyed", result.reason());
    }

    @Test void questDeathsRespawnEvenWhenEveryParticipantIsTemporarilyDown() {
        BattleRules.MatchState quest = state(BattleRules.Mode.QUEST_BATTLE);
        quest.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);

        BattleRules.Resolution first = quest.playerDefeated(0, 1L);
        assertFalse(first.ended());
        assertTrue(first.respawn());
        assertEquals("questbattle-respawn", first.reason());

        BattleRules.Resolution second = quest.playerDefeated(0, 2L);
        assertFalse(second.ended());
        assertTrue(second.respawn());
        assertEquals("questbattle-respawn", second.reason());

        quest.respawn(1L);
        quest.respawn(2L);
        assertEquals(java.util.Set.of(1L, 2L),
                quest.snapshot(0).alivePlayers());
    }

    @Test void timeAttackEndsOnlyAfterTheFinalBossEvent() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L), Map.of(), 0);
        assertFalse(match.waveCleared(0).ended());
        BattleRules.Resolution result = match.timeAttackCompleted();
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(1L, 2L), result.winners());
        assertEquals("timeattack-final-boss-defeated", result.reason());
    }

    @Test void timeAttackDragonTerminalUsesItsOwnServerReason() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L), Map.of(), 0);
        BattleRules.Resolution result = match.timeAttackCompleted(true);
        assertTrue(result.ended());
        assertEquals("timeattack-dragon-defeated", result.reason());
    }

    @Test void timeAttackLeaderDeathFailsTheCooperativeParty() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L, 3L), Map.of(), 2L, 0);
        assertEquals(2L, match.timeAttackLeader());
        assertFalse(match.playerDefeated(3L, 1L).ended());

        BattleRules.Resolution result = match.playerDefeated(3L, 2L);
        assertTrue(result.ended());
        assertTrue(result.winners().isEmpty());
        assertEquals(java.util.Set.of(1L, 2L, 3L), result.losers());
        assertEquals("timeattack-leader-defeated", result.reason());
    }

    @Test void timeAttackDisconnectIsAnIndividualLossAndTransfersLeader() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L), Map.of(), 1L, 0);

        BattleRules.Resolution disconnect = match.timeAttackPlayerDisconnected(1L);
        assertFalse(disconnect.ended());
        assertEquals(java.util.Set.of(1L), disconnect.losers());
        assertTrue(match.transferTimeAttackLeader(2L));
        assertEquals(2L, match.timeAttackLeader());

        BattleRules.Resolution completed = match.timeAttackCompleted();
        assertTrue(completed.ended());
        assertEquals(java.util.Set.of(2L), completed.winners());
        assertEquals(java.util.Set.of(1L), completed.losers());
    }

    @Test void timeAttackDeadlineBeginsAtTheOwnerStartTower() {
        BattleRules.MatchState match = state(BattleRules.Mode.TIME_ATTACK);
        match.begin(List.of(1L, 2L), Map.of(), 100L);
        long muchLater = 10_000_000_000_000L;
        assertFalse(match.timeout(muchLater).ended());
        assertEquals(match.rules().timeLimitSeconds(),
                match.snapshot(muchLater).secondsRemaining());

        assertTrue(match.startTimeAttack(muchLater));
        assertFalse(match.startTimeAttack(muchLater + 1));
        long deadline = muchLater
                + match.rules().timeLimitSeconds() * 1_000_000_000L;
        assertFalse(match.timeout(deadline - 1).ended());
        BattleRules.Resolution timeout = match.timeout(deadline);
        assertTrue(timeout.ended());
        assertEquals("timeattack-timeout", timeout.reason());
    }

    @Test void siegeWarEndsAtStrongholdCapture() {
        BattleRules.MatchState match = state(BattleRules.Mode.SIEGE_WAR);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        assertTrue(match.assignSiegeAttackerForce(1));
        assertFalse(match.assignSiegeAttackerForce(0));
        assertFalse(match.strongholdCaptured(0).ended());
        BattleRules.Resolution result = match.strongholdCaptured(1);
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(2L), result.winners());
    }

    @Test void siegeUsesFixedTwentyMinutesAndTimeoutAwardsDefenders() {
        GameHub.RoomSettings configured = new GameHub.RoomSettings(
                "siege", "", 8, BattleRules.Mode.SIEGE_WAR.wireId(),
                0, 0, 0, 1, List.of());
        BattleRules.MatchState match = new BattleRules.MatchState(
                BattleRules.decode(configured));
        assertEquals(1_200, match.rules().timeLimitSeconds());
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        assertTrue(match.assignSiegeAttackerForce(0));
        BattleRules.Resolution timeout = match.timeout(1_200_000_000_000L);
        assertTrue(timeout.ended());
        assertEquals(java.util.Set.of(2L), timeout.winners());
        assertEquals("siegewar-timeout", timeout.reason());
    }

    @Test void petRaceKeepsIndependentCheckpointState() {
        BattleRules.MatchState race = state(BattleRules.Mode.PET_RACE);
        assertEquals(4, race.rules().goal());
        assertEquals(300, race.rules().timeLimitSeconds());
        assertEquals(2, race.rules().minimumPlayers());
        race.begin(List.of(1L, 2L), Map.of(), 0);
        assertEquals("ignored-race-checkpoint-order", race.checkpoint(2, 2).reason());
        assertFalse(race.checkpoint(2, 1).ended());
        assertEquals("ignored-race-checkpoint-order", race.checkpoint(2, 3).reason());
        assertFalse(race.checkpoint(2, 2).ended());
        assertFalse(race.checkpoint(2, 3).ended());
        assertFalse(race.checkpoint(2, 4).ended());
        assertEquals("ignored-pet-race-finish-window", race.finishPetRace(1).reason());
        BattleRules.Resolution result = race.finishPetRace(2);
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(2L), result.winners());
        assertEquals(java.util.Set.of(1L), result.losers());
    }

    @Test void petRaceCanResolveMultipleOfficialRewardPlacements() {
        BattleRules.MatchState race = state(BattleRules.Mode.PET_RACE);
        race.begin(List.of(1L, 2L, 3L, 4L, 5L), Map.of(), 0);
        for (long player : List.of(1L, 2L)) {
            for (int checkpoint = 1; checkpoint <= 4; checkpoint++) {
                assertFalse(race.checkpoint(player, checkpoint).ended());
            }
        }
        BattleRules.Resolution result = race.finishPetRace(
                java.util.Set.of(1L, 2L));
        assertTrue(result.ended());
        assertEquals(java.util.Set.of(1L, 2L), result.winners());
        assertEquals(java.util.Set.of(3L, 4L, 5L), result.losers());
    }

    @Test void petRaceDisconnectRecordsOnlyTheDepartedRunnerAndKeepsRaceOpen() {
        BattleRules.MatchState race = state(BattleRules.Mode.PET_RACE);
        race.begin(List.of(1L, 2L), Map.of(), 0);
        BattleRules.Resolution disconnected = race.petRacePlayerDisconnected(1L);
        assertFalse(disconnected.ended());
        assertEquals(java.util.Set.of(1L), disconnected.losers());
        assertFalse(race.checkpoint(2L, 1).ended());
        assertEquals("ignored-pet-race-disconnect-outside-running-match",
                race.petRacePlayerDisconnected(1L).reason());
    }

    @Test void pveModesRejectPlayerAttacksAndTeamModesRejectFriendlyFire() {
        BattleRules.MatchState quest = state(BattleRules.Mode.QUEST_BATTLE);
        quest.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        assertFalse(quest.canAttack(1, 2));

        BattleRules.MatchState honor = state(BattleRules.Mode.HONOR_MATCH);
        honor.begin(List.of(1L, 2L, 3L), Map.of(1L, 0, 2L, 0, 3L, 1), 0);
        assertFalse(honor.canAttack(1, 2));
        assertTrue(honor.canAttack(1, 3));
    }

    @Test void nativeTownWireValueCannotEnterTheBattleStateMachine() {
        BattleRules.MatchState town = state(BattleRules.Mode.TOWN);
        assertEquals(List.of(500, 560), town.rules().mapPool());
        assertEquals(500, town.rules().mapId());
        assertEquals(0, town.rules().minimumPlayers());
        assertEquals(0, town.rules().timeLimitSeconds());
        assertFalse(town.rules().mode().isBattleScene());
        assertThrows(IllegalStateException.class,
                () -> town.begin(List.of(1L), Map.of(), 0));
    }

    @Test void timeoutDoesNotFabricateADeathmatchWinner() {
        BattleRules.MatchState match = state(BattleRules.Mode.DEATHMATCH);
        match.begin(List.of(1L, 2L), Map.of(1L, 0, 2L, 1), 0);
        BattleRules.Resolution result = match.timeout(
                match.rules().timeLimitSeconds() * 1_000_000_000L);
        assertTrue(result.ended());
        assertTrue(result.winners().isEmpty());
        assertEquals("deathmatch-timeout", result.reason());
    }

    @Test void everyNativeMapProducesLegalDistinctSpawns() {
        Set<Integer> activeBattleMaps = new LinkedHashSet<>();
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int mapId : mode.maps()) {
                assertTrue(activeBattleMaps.add(mapId),
                        () -> "map appears in multiple native modes: " + mapId);
                ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
                ArenaMapCatalog.Point first = arena.spawn(0);
                ArenaMapCatalog.Point second = arena.spawn(1);
                assertTrue(arena.contains(first.x(), first.y()));
                assertTrue(arena.contains(second.x(), second.y()));
                assertFalse(first.equals(second));
                assertEquals(8, arena.spawns().size());
                assertEquals(8, new java.util.HashSet<>(arena.spawns()).size());
                assertTrue(arena.contains(arena.featurePoint().x(),
                        arena.featurePoint().y()));
                assertFalse(arena.spawns().contains(arena.featurePoint()));
                for (ArenaMapCatalog.Point spawn : arena.spawns()) {
                    assertTrue(arena.nativeStandingFloor(spawn.x(), spawn.y()),
                            () -> "spawn is not a native standing floor: "
                                    + mapId + " " + spawn);
                    assertTrue(hasNativeExit(arena, spawn),
                            () -> "spawn has no legal native movement edge: "
                                    + mapId + " " + spawn);
                }
            }
        }
        assertEquals(40, activeBattleMaps.size());
        assertTrue(ArenaMapCatalog.mapIds().containsAll(activeBattleMaps));
        assertEquals(List.of(500, 560), BattleRules.Mode.TOWN.maps());
    }

    @Test void geometryComesFromNativeLitoS32SectorsInsteadOfLegacyMapIds() {
        ArenaMapCatalog.Arena map509 = ArenaMapCatalog.forMap(509);
        assertEquals(32768, map509.minX());
        assertEquals(32831, map509.maxX());
        assertEquals(32768, map509.minY());
        assertEquals(32831, map509.maxY());

        ArenaMapCatalog.Arena map508 = ArenaMapCatalog.forMap(508);
        assertEquals(32768, map508.minX());
        assertEquals(32831, map508.maxX());
        assertEquals(32768, map508.minY());
        assertEquals(32831, map508.maxY());
    }

    @Test void rectangularObserverEnvelopeDoesNotMakeMissingS32SectorsLegal() {
        ArenaMapCatalog.Arena map504 = ArenaMapCatalog.forMap(504);
        assertEquals(32704, map504.minX());
        assertEquals(32895, map504.maxX());
        assertEquals(32704, map504.minY());
        assertEquals(32895, map504.maxY());
        assertFalse(map504.contains(32704, 32704));
        assertTrue(map504.contains(32768, 32768));
    }

    @Test void unknownArenaIdsCannotSilentlyReuseMap509Geometry() {
        assertThrows(IllegalArgumentException.class,
                () -> ArenaMapCatalog.forMap(999));
    }

    private static BattleRules.MatchState state(BattleRules.Mode mode) {
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "mode", "", 8, mode.wireId(), 0, 0, 0, 0, List.of());
        return new BattleRules.MatchState(BattleRules.decode(settings));
    }

    private static boolean hasNativeExit(ArenaMapCatalog.Arena arena,
                                         ArenaMapCatalog.Point spawn) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx == 0 && dy == 0)
                        || (dx != 0 && dy != 0)) continue;
                if (arena.nativeCanStep(spawn.x(), spawn.y(),
                        spawn.x() + dx, spawn.y() + dy)) return true;
            }
        }
        return false;
    }
}
