package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Retained L1NpcInstance.noTarget idle movement and home-return semantics. */
final class ClassicWorldMonsterIdleMovementTest {
    private static final int MAP_ID = 4;
    private static final long OBJECT_BASE = 0x6800_0000L;

    @Test
    void sourceRunUsesSelectionTickPlusCountdownAndPauseRollConsumesDefaultDelay() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 2, 0, false);
        long objectId = OBJECT_BASE + 1;

        ClassicWorldMonsterPopulation.IdleMovement first = population.idleMove(
                MAP_ID, objectId, Set.of(), 1L, 1, 0);
        assertNotNull(first);
        assertEquals(home[0] + 1, first.after().x());
        assertEquals(home[1], first.after().y());
        assertEquals(2, first.after().heading());
        assertFalse(first.teleportedHome());

        // A source run length of one still performs this second tick after the
        // decrement reaches zero, returning toward the retained home.
        ClassicWorldMonsterPopulation.IdleMovement second = population.idleMove(
                MAP_ID, objectId, Set.of(), 10_000_000_000L, 3, 39);
        assertNotNull(second);
        assertEquals(home[0], second.after().x());
        assertEquals(home[1], second.after().y());

        // The following tick opens a new cycle. 32/40 source rolls are an
        // intentional idle pause and retain the default 300 ms AI delay.
        long pauseNow = 20_000_000_000L;
        assertNull(population.idleMove(
                MAP_ID, objectId, Set.of(), pauseNow, 3, 39));
        assertFalse(population.actionReady(MAP_ID, objectId,
                pauseNow + 299_999_999L));
        assertTrue(population.actionReady(MAP_ID, objectId,
                pauseNow + 300_000_000L));
    }

    @Test
    void movementDistanceBroadcastsStepThenLeashesToActualSpawnLanding() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 2, 1, false);
        long objectId = OBJECT_BASE + 1;
        assertNotNull(population.idleMove(
                MAP_ID, objectId, Set.of(), 1L, 1, 0));

        Set<ClassicWorldMonsterPopulation.GridPoint> blockers = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(home[0], home[1]),
                new ClassicWorldMonsterPopulation.GridPoint(home[0], home[1] + 1),
                new ClassicWorldMonsterPopulation.GridPoint(home[0], home[1] - 1));
        ClassicWorldMonsterPopulation.IdleMovement leash = population.idleMove(
                MAP_ID, objectId, blockers, 10_000_000_000L, 0, -1);
        assertNotNull(leash);
        assertEquals(home[0] + 1, leash.moved().x());
        assertEquals(home[1] - 1, leash.moved().y());
        assertTrue(leash.teleportedHome());
        assertEquals(home[0], leash.after().x());
        assertEquals(home[1], leash.after().y());
    }

    @Test
    void noPlayerWithinTwentyTilesResetsWanderedActorButBoundaryObserverKeepsIt() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 2, 0, false);
        long objectId = OBJECT_BASE + 1;
        ClassicWorldMonsterPopulation.IdleMovement moved = population.idleMove(
                MAP_ID, objectId, Set.of(), 1L, 1, 0);
        assertNotNull(moved);

        Set<ClassicWorldMonsterPopulation.GridPoint> boundary = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(
                        moved.after().x() + 20, moved.after().y()));
        assertTrue(population.resetUnobservedHome(
                MAP_ID, boundary, 2L).isEmpty());

        Set<ClassicWorldMonsterPopulation.GridPoint> outside = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(
                        moved.after().x() + 21, moved.after().y()));
        List<ClassicWorldMonsterPopulation.HomeReset> resets =
                population.resetUnobservedHome(MAP_ID, outside, 3L);
        assertEquals(1, resets.size());
        assertEquals(home[0], resets.get(0).after().x());
        assertEquals(home[1], resets.get(0).after().y());
    }

    @Test
    void restAndZeroPassiveSpeedRemainStationary() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation resting = population(home, 2, 0, true);
        assertNull(resting.idleMove(
                MAP_ID, OBJECT_BASE + 1, Set.of(), 1L, 1, 0));

        ClassicWorldMonsterPopulation zeroSpeed = population(home, 2, 0, false, 0);
        assertNull(zeroSpeed.idleMove(
                MAP_ID, OBJECT_BASE + 1, Set.of(), 1L, 1, 0));
    }

    @Test
    void activationRangeIncludesTwentyAndExcludesTwentyOne() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 2, 0, false);
        assertEquals(1, population.withinTileDistance(
                MAP_ID, home[0] + 20, home[1], 20).size());
        assertTrue(population.withinTileDistance(
                MAP_ID, home[0] + 21, home[1], 20).isEmpty());
    }

    @Test
    void sourceMobGroupMinionFollowsLeaderOnlyBeyondTwoTiles() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot leader =
                population.mobGroupLeaders(MAP_ID, 8).get(0);
        long leaderObjectId = leader.objectId();
        List<ClassicWorldMonsterPopulation.Snapshot> minions =
                population.mobGroupMembers(MAP_ID, leaderObjectId).stream()
                        .filter(actor -> actor.objectId() != leaderObjectId)
                        .toList();
        long playerObjectId = 0x4200_0769L;
        assertTrue(population.provoke(MAP_ID, leaderObjectId, playerObjectId));
        int targetX = leader.x() + 12;
        int targetY = leader.y();
        Set<ClassicWorldMonsterPopulation.GridPoint> targetCell = Set.of(
                new ClassicWorldMonsterPopulation.GridPoint(targetX, targetY));
        for (int step = 0; step < 8; step++) {
            population.moveToward(MAP_ID, leaderObjectId, playerObjectId,
                    targetX, targetY, targetCell,
                    10_000_000_000L * (step + 1));
        }
        population.clearTarget(MAP_ID, leaderObjectId, playerObjectId);
        leader = population.actor(MAP_ID, leaderObjectId);
        ClassicWorldMonsterPopulation.Snapshot distant = null;
        for (ClassicWorldMonsterPopulation.Snapshot source : minions) {
            ClassicWorldMonsterPopulation.Snapshot current = population.actor(
                    MAP_ID, source.objectId());
            if (chebyshev(current, leader) > 2) {
                distant = current;
                break;
            }
        }
        assertNotNull(distant, "leader fixture did not leave a minion behind");
        int beforeDistance = chebyshev(distant, leader);
        ClassicWorldMonsterPopulation.IdleMovement follow = population.idleMove(
                MAP_ID, distant.objectId(), Set.of(), 100_000_000_000L);
        assertNotNull(follow);
        assertTrue(chebyshev(follow.after(), leader) < beforeDistance);
    }

    private static ClassicWorldMonsterPopulation population(
            int[] home, int heading, int movementDistance, boolean rest) {
        return population(home, heading, movementDistance, rest, 600);
    }

    private static ClassicWorldMonsterPopulation population(
            int[] home, int heading, int movementDistance,
            boolean rest, int passiveSpeed) {
        ClassicWorldMonsterCatalog.MonsterTemplate source =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).get(0).template();
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        source.npcId(), source.nameId(), "L1Monster",
                        source.gfxId(), source.level(), source.hp(), source.mp(),
                        source.armorClass(), source.strength(), source.constitution(),
                        source.dexterity(), source.wisdom(), source.intelligence(),
                        source.magicResistance(), source.experience(), source.lawful(),
                        source.size(), source.weakAttribute(), source.ranged(),
                        passiveSpeed, source.atkspeed(), source.bowActionId(),
                        source.undead(), source.poisonAttack(),
                        source.paralysisAttack(), false, source.damageReduction(),
                        source.hard(), source.doppel(), false,
                        source.lightSize(), source.changeHead());
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 990_769, "native-noTarget-fixture", 1, 0,
                        home[0], home[1], 0, 0, 0, 0, 0, 0, heading,
                        0, 0, false, movementDistance, rest, 0, template);
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                769L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                List.of(definition));
    }

    private static int[] openCenter() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int x = map.startX() + 2; x <= map.endX() - 2; x++) {
            for (int y = map.startY() + 2; y <= map.endY() - 2; y++) {
                boolean open = map.standingPassable(x, y)
                        && !ClassicWorldNpcCatalog.occupies(MAP_ID, x, y);
                for (int heading = 0; open && heading < 8; heading++) {
                    int nx = x + dx[heading];
                    int ny = y + dy[heading];
                    open = map.canStep(x, y, nx, ny)
                            && !ClassicWorldNpcCatalog.occupies(MAP_ID, nx, ny);
                }
                if (open) return new int[] {x, y};
            }
        }
        throw new AssertionError("missing open noTarget fixture cell");
    }

    private static int chebyshev(
            ClassicWorldMonsterPopulation.Snapshot left,
            ClassicWorldMonsterPopulation.Snapshot right) {
        return Math.max(Math.abs(left.x() - right.x()),
                Math.abs(left.y() - right.y()));
    }
}
