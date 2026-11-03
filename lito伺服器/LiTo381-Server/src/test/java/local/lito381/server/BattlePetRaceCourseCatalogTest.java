package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

final class BattlePetRaceCourseCatalogTest {
    @Test
    void allNativeCoursesPublishTenUniquePassableStartingCells() {
        for (int mapId : new int[] {565, 566, 569, 570}) {
            BattlePetRaceCourseCatalog.Course course =
                    BattlePetRaceCourseCatalog.forMap(mapId);
            assertEquals(mapId, course.mapId());
            assertEquals(BattlePetRaceCourseCatalog.MAXIMUM_PLAYERS,
                    new HashSet<>(course.spawns()).size());
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
            int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
            for (ArenaMapCatalog.Point spawn : course.spawns()) {
                assertTrue(arena.nativeStandingFloor(spawn.x(), spawn.y()),
                        () -> "non-passable pet-race spawn " + mapId + ":" + spawn);
                assertTrue(arena.nativeCanStep(spawn.x(), spawn.y(),
                                spawn.x() + dx[course.heading()],
                                spawn.y() + dy[course.heading()]),
                        () -> "blocked starting direction " + mapId + ":" + spawn);
            }
        }
    }

    @Test
    void gatesMustBeCrossedInOrderAndFinishExactlyFourLaps() {
        BattlePetRaceCourseCatalog.Course course =
                BattlePetRaceCourseCatalog.forMap(565);
        BattlePetRaceCourseCatalog.State state = new BattlePetRaceCourseCatalog.State();
        long start = 5_000_000_000L;
        long raceDeadline = start + 300_000_000_000L;
        state.begin(565, List.of(11L, 22L), start, raceDeadline);

        BattlePetRaceCourseCatalog.Zone second = course.gates().get(1);
        assertFalse(enter(state, 11, second, start + 1).gateAdvanced());
        assertEquals(1, state.currentLap(11));

        BattlePetRaceCourseCatalog.Transition transition = null;
        for (int lap = 1; lap <= BattlePetRaceCourseCatalog.LAPS; lap++) {
            for (BattlePetRaceCourseCatalog.Zone gate : course.gates()) {
                transition = enter(state, 11, gate,
                        start + lap * 1_000_000_000L);
                assertTrue(transition.gateAdvanced());
            }
            assertTrue(transition.lapCompleted());
            assertEquals(lap, transition.completedLaps());
            assertEquals(Math.min(BattlePetRaceCourseCatalog.LAPS, lap + 1),
                    transition.currentLap());
            assertEquals(lap == BattlePetRaceCourseCatalog.LAPS,
                    transition.finished());
        }
        assertEquals(4_000, transition.elapsedMilliseconds());
        assertEquals(1, transition.placement());
        assertEquals(11L, state.firstFinisher());
        assertEquals(start + 14_000_000_000L, state.finishDeadlineNanos());
        assertFalse(state.finishWindowExpired(state.finishDeadlineNanos() - 1));
        assertTrue(state.finishWindowExpired(state.finishDeadlineNanos()));
        assertFalse(enter(state, 11, course.gates().get(0),
                start + 5_000_000_000L).gateAdvanced());
        assertEquals(1, state.currentLap(22));

        for (int lap = 1; lap <= BattlePetRaceCourseCatalog.LAPS; lap++) {
            for (BattlePetRaceCourseCatalog.Zone gate : course.gates()) {
                transition = enter(state, 22, gate,
                        start + (5L + lap) * 1_000_000_000L);
            }
        }
        assertTrue(transition.finished());
        assertEquals(2, transition.placement());
        assertEquals(2, state.finishedCount());
        assertEquals(start + 14_000_000_000L, state.finishDeadlineNanos(),
                "later finishers must not extend the ten-second window");
    }

    @Test
    void reverseCourseRetainsIndependentSequentialProgress() {
        BattlePetRaceCourseCatalog.Course reverse =
                BattlePetRaceCourseCatalog.forMap(570);
        BattlePetRaceCourseCatalog.State state = new BattlePetRaceCourseCatalog.State();
        state.begin(570, List.of(1L, 2L), 0, 300_000_000_000L);
        for (BattlePetRaceCourseCatalog.Zone gate : reverse.gates()) {
            assertTrue(enter(state, 2, gate, 500_000_000L).gateAdvanced());
        }
        assertEquals(2, state.currentLap(2));
        assertEquals(1, state.currentLap(1));
    }

    @Test
    void officialEntrantBandsRewardOnlyCompletedEligiblePlacements() {
        assertEquals(1, BattlePetRaceCourseCatalog.rewardPlacementLimit(2));
        assertEquals(1, BattlePetRaceCourseCatalog.rewardPlacementLimit(4));
        assertEquals(2, BattlePetRaceCourseCatalog.rewardPlacementLimit(5));
        assertEquals(2, BattlePetRaceCourseCatalog.rewardPlacementLimit(7));
        assertEquals(3, BattlePetRaceCourseCatalog.rewardPlacementLimit(8));
        assertEquals(3, BattlePetRaceCourseCatalog.rewardPlacementLimit(10));

        assertRewardWinners(4, 3, java.util.Set.of(1L));
        assertRewardWinners(5, 3, java.util.Set.of(1L, 2L));
        assertRewardWinners(8, 3, java.util.Set.of(1L, 2L, 3L));
        assertRewardWinners(8, 2, java.util.Set.of(1L, 2L));
    }

    private static void assertRewardWinners(
            int entrants, int finishers, java.util.Set<Long> expected) {
        BattlePetRaceCourseCatalog.State state = new BattlePetRaceCourseCatalog.State();
        List<Long> players = LongStream.rangeClosed(1, entrants).boxed().toList();
        long start = 1_000_000_000L;
        state.begin(565, players, start, start + 300_000_000_000L);
        BattlePetRaceCourseCatalog.Course course =
                BattlePetRaceCourseCatalog.forMap(565);
        for (long player = 1; player <= finishers; player++) {
            for (int lap = 0; lap < BattlePetRaceCourseCatalog.LAPS; lap++) {
                for (BattlePetRaceCourseCatalog.Zone gate : course.gates()) {
                    enter(state, player, gate, start + player * 1_000_000L + lap);
                }
            }
        }
        assertEquals(expected, state.rewardWinners());
    }

    private static BattlePetRaceCourseCatalog.Transition enter(
            BattlePetRaceCourseCatalog.State state, long player,
            BattlePetRaceCourseCatalog.Zone zone, long nowNanos) {
        return state.moved(player, zone.minX() - 1, zone.minY(),
                zone.minX(), zone.minY(), nowNanos);
    }
}
