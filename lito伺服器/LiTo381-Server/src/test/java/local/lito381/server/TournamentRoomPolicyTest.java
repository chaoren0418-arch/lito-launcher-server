package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TournamentRoomPolicyTest {
    @Test void capacityAndLevelRulesPreserveNativeBoundariesWithoutNarrowingModernLevels() {
        for (int capacity : new int[]{Integer.MIN_VALUE, 0, 1, 101, Integer.MAX_VALUE}) {
            assertFalse(TournamentRoomPolicy.nativeRoomCapacity(capacity));
            assertFalse(GameHub.nativeRoomCapacity(capacity));
        }
        for (int capacity : new int[]{2, 10, 100}) assertTrue(GameHub.nativeRoomCapacity(capacity));
        long[] levels = {0, 14, 15, 39, 40, 47, 48, 51, 52, 80, Long.MAX_VALUE};
        for (long level : levels) for (int channel = -1; channel <= 4; channel++) {
            var tier = TournamentPresence.recommendedChannel(level);
            boolean exists = tier != null && channel >= 0 && channel < 4;
            assertEquals(exists && channel <= tier.id(), TournamentRoomPolicy.canCreate(channel, level));
            assertEquals(exists && channel <= tier.id(), TournamentRoomPolicy.canJoin(channel, level, false));
            assertEquals(exists, TournamentRoomPolicy.canJoin(channel, level, true));
        }
        assertFalse(TournamentRoomPolicy.canCreate(3, Long.MAX_VALUE));
        assertTrue(TournamentRoomPolicy.canJoin(3, Long.MAX_VALUE, true));
    }

    @Test void nativeSettingsMaskAndLazyNullBehaviorAreUnchanged() {
        var settings = new GameHub.RoomSettings("room", "", 100, 1, 0, 0, 0, 0);
        for (int level : new int[]{14, 15, 39, 40, 48, 52, Integer.MAX_VALUE})
            for (int channel = -1; channel <= 4; channel++)
                assertEquals(TournamentRoomPolicy.canJoin(channel, level, settings.allowsUpperTier()),
                        GameHub.canJoinRoom(channel, level, settings));
        assertFalse(GameHub.canJoinRoom(-1, 80, null));
        assertFalse(GameHub.canJoinRoom(0, 14, null));
        assertTrue(GameHub.canJoinRoom(0, 80, null));
        assertThrows(NullPointerException.class, () -> GameHub.canJoinRoom(3, 80, null));
        var masked = new GameHub.RoomSettings("room", "", 100, 0x81, 0, 0, 0, 0);
        assertEquals(BattleRules.decode(BattleRules.Mode.DEATHMATCH, 564, BattleControlCatalog.defaults()),
                BattleRules.decode(masked, 564, BattleControlCatalog.defaults()));
    }

    @Test void allFortyOptionsUseTheExistingRulesAndNotTownOrInventedMaps() {
        var seen = new HashSet<String>();
        for (var mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int map : mode.maps()) {
                var actual = TournamentRoomPolicy.rules(mode.wireId(), map);
                assertEquals(BattleRules.decode(mode, map, BattleControlCatalog.defaults()), actual);
                assertTrue(seen.add(mode.wireId() + ":" + map));
                assertTrue(actual.minimumPlayers() >= 1 && actual.minimumPlayers() <= 100);
                assertTrue(actual.timeLimitSeconds() >= 1 && actual.timeLimitSeconds() <= 86400);
            }
        }
        assertEquals(40, seen.size());
        assertThrows(IllegalArgumentException.class, () -> TournamentRoomPolicy.rules(6, 500));
        assertThrows(IllegalArgumentException.class, () -> TournamentRoomPolicy.rules(1, 540));
        assertEquals(10, TournamentRoomPolicy.combatantCapacity(100, BattleRules.Mode.PET_RACE));
        assertEquals(60, TournamentRoomPolicy.combatantCapacity(100, BattleRules.Mode.TIME_ATTACK));
        assertEquals(100, TournamentRoomPolicy.combatantCapacity(100, BattleRules.Mode.SIEGE_WAR));
    }

    @Test void readinessRequiresActualModeMinimumBothTeamsAndOptionalEquality() {
        var death = TournamentRoomPolicy.rules(1, 564);
        assertFalse(TournamentRoomPolicy.readySatisfied(death, 1, 1, 1, 0, false));
        assertFalse(TournamentRoomPolicy.readySatisfied(death, 2, 1, 1, 1, false));
        assertFalse(TournamentRoomPolicy.readySatisfied(death, 2, 2, 2, 0, false));
        assertTrue(TournamentRoomPolicy.readySatisfied(death, 2, 2, 1, 1, true));
        assertTrue(TournamentRoomPolicy.readySatisfied(death, 3, 3, 2, 1, false));
        assertFalse(TournamentRoomPolicy.readySatisfied(death, 3, 3, 2, 1, true));
        assertTrue(TournamentRoomPolicy.readySatisfied(TournamentRoomPolicy.rules(4, 540), 1, 1, 1, 0, false));
        assertFalse(TournamentRoomPolicy.readySatisfied(TournamentRoomPolicy.rules(0, 565), 1, 1, 1, 0, false));
        assertTrue(TournamentRoomPolicy.readySatisfied(TournamentRoomPolicy.rules(0, 565), 2, 2, 2, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> TournamentRoomPolicy.readySatisfied(death, 2, 2, 1, 0, false));
    }

    @Test void siegeReadinessRequiresFifteenToFiftyOnEachSide() {
        var siege = TournamentRoomPolicy.rules(3, 573);
        assertFalse(TournamentRoomPolicy.readySatisfied(siege, 29, 29, 15, 14, false));
        assertFalse(TournamentRoomPolicy.readySatisfied(siege, 30, 30, 16, 14, false));
        assertTrue(TournamentRoomPolicy.readySatisfied(siege, 30, 30, 15, 15, true));
        assertFalse(TournamentRoomPolicy.readySatisfied(siege, 66, 66, 51, 15, false));
        assertTrue(TournamentRoomPolicy.readySatisfied(siege, 100, 100, 50, 50, true));
    }
}
