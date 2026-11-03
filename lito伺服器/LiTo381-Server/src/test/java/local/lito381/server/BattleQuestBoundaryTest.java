package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class BattleQuestBoundaryTest {
    @Test
    void nativeQuestMapFamilyNeverActivatesQuarantinedSiegeGeometry() {
        assertEquals(List.of(18, 5, 8), BattleQuestBoundary.cutSizes());
        for (int selected : BattleRules.Mode.QUEST_BATTLE.maps()) {
            BattleQuestBoundary boundary = new BattleQuestBoundary();
            boundary.reset(new BattleRules.RuleSet(
                    BattleRules.Mode.QUEST_BATTLE, selected, List.of(selected),
                    600, 1, 2, true, false,
                    "clear-four-monster-rooms+destroy-final-guardian-tower"));
            assertFalse(boundary.active());
            assertTrue(boundary.attributes(0, 1).isEmpty());
            assertTrue(boundary.openedAfter(0, 1).isEmpty());
        }
    }

    @Test
    void markerCentresAreStandingFloorOnBothSiegeResources() {
        for (int mapId : List.of(573, 574)) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            for (int room = 1; room <= 4; room++) {
                ArenaMapCatalog.Point anchor =
                        BattleQuestBoundary.roomAnchor(mapId, room);
                assertTrue(arena.nativeStandingFloor(anchor.x(), anchor.y()),
                        "room centre is not floor map=" + mapId
                                + " room=" + room + " " + anchor);
            }
        }
    }

    @Test
    void quarantinedGeometryRemainsReproducibleInEightDirections() {
        for (int mapId : List.of(573, 574)) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            List<ArenaMapCatalog.Point> anchors = List.of(
                    BattleQuestBoundary.roomAnchor(mapId, 1),
                    BattleQuestBoundary.roomAnchor(mapId, 2),
                    BattleQuestBoundary.roomAnchor(mapId, 3),
                    BattleQuestBoundary.roomAnchor(mapId, 4));
            for (int room = 1; room <= 4; room++) {
                Set<ArenaMapCatalog.Point> reachable = reachable(
                        arena, anchors.get(0), room);
                for (int index = 0; index < anchors.size(); index++) {
                    assertEquals(index < room, reachable.contains(anchors.get(index)),
                            "unexpected marker reachability map=" + mapId
                                    + " activeRoom=" + room + " anchor=" + (index + 1));
                }
            }
        }
    }

    @Test
    void exactDoorAttributeWireCarriesEveryFieldIndependentlyOfModeSemantics() {
        BattleQuestBoundary.Attribute edge = new BattleQuestBoundary.Attribute(
                32743, 32805, 1, BattleDoor.NOT_PASSABLE, 1,
                BattleQuestBoundary.PLACEMENT_SOURCE);
        Protocol.Reader packet = new Protocol.Reader(Protocol.doorAttribute(
                edge.x(), edge.y(), edge.direction(), edge.passable()));
        assertEquals(Protocol.S_CHANGE_ATTR, packet.c());
        assertEquals(edge.x(), packet.h());
        assertEquals(edge.y(), packet.h());
        assertEquals(edge.direction(), packet.c());
        assertEquals(edge.passable(), packet.c());
        assertEquals(0, packet.remaining());
    }

    @Test
    void siegeMapsCannotBeInjectedIntoQuestToActivateTheExperiment() {
        for (int selected : List.of(573, 563, 547)) {
            BattleQuestBoundary boundary = new BattleQuestBoundary();
            boundary.reset(new BattleRules.RuleSet(
                    BattleRules.Mode.QUEST_BATTLE, selected, List.of(selected),
                    600, 1, 2, true, false,
                    "clear-four-monster-rooms+destroy-final-guardian-tower"));
            assertFalse(boundary.active(),
                    "invalid Quest map activated quarantined geometry: " + selected);
            assertTrue(boundary.attributes(0, 1).isEmpty());
            assertTrue(boundary.openedAfter(0, 1).isEmpty());
        }
    }

    private static Set<ArenaMapCatalog.Point> reachable(
            ArenaMapCatalog.Arena arena, ArenaMapCatalog.Point start,
            int roomNumber) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        Set<ArenaMapCatalog.Point> seen = new HashSet<>();
        ArrayDeque<ArenaMapCatalog.Point> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            ArenaMapCatalog.Point point = queue.removeFirst();
            for (int heading = 0; heading < dx.length; heading++) {
                ArenaMapCatalog.Point next = new ArenaMapCatalog.Point(
                        point.x() + dx[heading], point.y() + dy[heading]);
                if (!seen.contains(next)
                        && arena.nativeStandingFloor(next.x(), next.y())
                        && !BattleQuestBoundary.blocksMap(arena.mapId(), roomNumber,
                                point.x(), point.y(), next.x(), next.y())) {
                    seen.add(next);
                    queue.addLast(next);
                }
            }
        }
        return seen;
    }
}
