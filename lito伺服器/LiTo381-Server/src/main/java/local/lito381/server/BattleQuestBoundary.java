package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Quarantined resource-derived boundary experiment for maps 573/574.
 *
 * <p>The wire mechanism is exact: LiTo.bin's {@code S_CHANGE_ATTR} receiver
 * updates one Layer3 directional edge, and {@code sub_48B640} consumes those
 * edges for cardinal and diagonal movement.  The three edge sets below are a
 * deterministic reconstruction from the selected client's unmodified 573 and
 * 574 S32 Layer3 resources: four repeated red-marker room centres are ordered
 * from the native Layer7 entrance, then each boundary is the complete minimum
 * cardinal cut between the marker groups.  Exact LTmapstr parsing now proves
 * these resources belong to Siege War, not Quest Battle.  Production Quest
 * maps therefore never activate this experiment; it remains only as a
 * reproducible Layer3/S_CHANGE_ATTR geometry diagnostic and is not presented
 * as an original-server boundary contract.</p>
 */
final class BattleQuestBoundary {
    static final String WIRE_SOURCE =
            "LiTo.bin-sub_4A5E70+sub_48B9A0+sub_48B640-S_CHANGE_ATTR";
    static final String PLACEMENT_SOURCE =
            "LTmap-573+574-Layer1-red-markers+Layer3-symmetric-mincut-reconstruction";

    record Attribute(int x, int y, int direction, int passable,
                     int unlockAfterRoom, String placementSource) {
        Attribute {
            if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || (direction != 0 && direction != 1)
                    || (passable != BattleDoor.PASSABLE
                        && passable != BattleDoor.NOT_PASSABLE)
                    || unlockAfterRoom < 1 || unlockAfterRoom > 3
                    || placementSource == null || placementSource.isBlank()) {
                throw new IllegalArgumentException("invalid Quest boundary attribute");
            }
        }
    }

    private record Edge(int x, int y, int direction, int unlockAfterRoom) {}

    private static final List<Edge> EDGES = List.of(
            // Clear room 1: leave the north-west entrance room.
            edge(32743, 32805, 1, 1), edge(32743, 32806, 1, 1),
            edge(32743, 32807, 1, 1), edge(32749, 32784, 1, 1),
            edge(32749, 32785, 1, 1), edge(32749, 32786, 1, 1),
            edge(32756, 32821, 1, 1), edge(32756, 32822, 1, 1),
            edge(32756, 32823, 1, 1), edge(32756, 32824, 1, 1),
            edge(32756, 32825, 1, 1), edge(32756, 32826, 1, 1),
            edge(32756, 32827, 1, 1), edge(32756, 32828, 1, 1),
            edge(32756, 32829, 1, 1), edge(32756, 32830, 1, 1),
            edge(32756, 32831, 1, 1), edge(32756, 32832, 1, 1),
            // Clear room 2: enter the south-east bone-wall room.
            edge(32763, 32835, 0, 2), edge(32767, 32835, 0, 2),
            edge(32768, 32835, 0, 2), edge(32769, 32835, 0, 2),
            edge(32773, 32835, 0, 2),
            // Clear room 3: enter the south-west final monster room.
            edge(32747, 32842, 1, 3), edge(32747, 32843, 1, 3),
            edge(32749, 32863, 1, 3), edge(32749, 32864, 1, 3),
            edge(32749, 32865, 1, 3), edge(32750, 32852, 1, 3),
            edge(32750, 32853, 1, 3), edge(32750, 32854, 1, 3));

    private final int[] mapIds = new int[2];
    private boolean active;

    synchronized void reset(BattleRules.RuleSet rules) {
        deactivate();
        if (rules == null || rules.mode() != BattleRules.Mode.QUEST_BATTLE
                || !BattleRules.Mode.QUEST_BATTLE.maps().contains(rules.mapId())) return;
        mapIds[0] = BattleRules.arenaMapForForce(rules, 0);
        mapIds[1] = BattleRules.arenaMapForForce(rules, 1);
        active = supported(mapIds[0]) && supported(mapIds[1]);
    }

    synchronized void deactivate() {
        active = false;
        mapIds[0] = 0;
        mapIds[1] = 0;
    }

    synchronized boolean active() {
        return active;
    }

    synchronized List<Attribute> attributes(int force, int roomNumber) {
        if (!validLane(force, roomNumber)) return List.of();
        List<Attribute> result = new ArrayList<>(EDGES.size());
        for (Edge edge : EDGES) {
            result.add(attribute(edge,
                    edge.unlockAfterRoom() < roomNumber
                            ? BattleDoor.PASSABLE : BattleDoor.NOT_PASSABLE));
        }
        return List.copyOf(result);
    }

    synchronized List<Attribute> openedAfter(int force, int clearedRoom) {
        if (!active || force < 0 || force > 1
                || clearedRoom < 1 || clearedRoom > 3) return List.of();
        return EDGES.stream()
                .filter(edge -> edge.unlockAfterRoom() == clearedRoom)
                .map(edge -> attribute(edge, BattleDoor.PASSABLE))
                .toList();
    }

    synchronized boolean blocks(int force, int roomNumber,
                                int fromX, int fromY, int toX, int toY) {
        return validLane(force, roomNumber)
                && blocksMap(mapIds[force], roomNumber, fromX, fromY, toX, toY);
    }

    static boolean blocksMap(int mapId, int roomNumber,
                             int fromX, int fromY, int toX, int toY) {
        if (!supported(mapId) || roomNumber < 1 || roomNumber > 4) return false;
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || (dx == 0 && dy == 0)) {
            return true;
        }
        if (dx == 0 && dy == -1) {
            return blockedEdge(mapId, fromX, fromY, 0, roomNumber);
        }
        if (dx == 1 && dy == 0) {
            return blockedEdge(mapId, fromX, fromY, 1, roomNumber);
        }
        if (dx == 0 && dy == 1) {
            return blockedEdge(mapId, fromX, fromY + 1, 0, roomNumber);
        }
        if (dx == -1 && dy == 0) {
            return blockedEdge(mapId, fromX - 1, fromY, 1, roomNumber);
        }

        // Exact LiTo.bin sub_48B640 formula over the merged Layer3 state.
        // A diagonal may be blocked by one native edge plus one S_CHANGE_ATTR
        // edge, so testing static and dynamic graphs independently is wrong.
        if (dx == 1 && dy == -1) {
            return (blockedEdge(mapId, fromX, fromY, 0, roomNumber)
                    || blockedEdge(mapId, fromX, fromY - 1, 1, roomNumber))
                    && (blockedEdge(mapId, fromX, fromY, 1, roomNumber)
                    || blockedEdge(mapId, fromX + 1, fromY, 0, roomNumber));
        }
        if (dx == 1 && dy == 1) {
            return (blockedEdge(mapId, fromX, fromY, 1, roomNumber)
                    || blockedEdge(mapId, fromX + 1, fromY + 1, 0, roomNumber))
                    && (blockedEdge(mapId, fromX, fromY + 1, 0, roomNumber)
                    || blockedEdge(mapId, fromX, fromY + 1, 1, roomNumber));
        }
        if (dx == -1 && dy == 1) {
            return (blockedEdge(mapId, fromX, fromY + 1, 0, roomNumber)
                    || blockedEdge(mapId, fromX - 1, fromY + 1, 1, roomNumber))
                    && (blockedEdge(mapId, fromX - 1, fromY, 1, roomNumber)
                    || blockedEdge(mapId, fromX - 1, fromY + 1, 0, roomNumber));
        }
        return (blockedEdge(mapId, fromX - 1, fromY, 1, roomNumber)
                || blockedEdge(mapId, fromX - 1, fromY, 0, roomNumber))
                && (blockedEdge(mapId, fromX - 1, fromY - 1, 1, roomNumber)
                || blockedEdge(mapId, fromX, fromY, 0, roomNumber));
    }

    static ArenaMapCatalog.Point roomAnchor(int mapId, int roomNumber) {
        if (!supported(mapId) || roomNumber < 1 || roomNumber > 4) return null;
        return switch (roomNumber) {
            case 1 -> new ArenaMapCatalog.Point(32729, 32824);
            case 2 -> new ArenaMapCatalog.Point(32800, 32824);
            case 3 -> new ArenaMapCatalog.Point(32800, 32860);
            case 4 -> new ArenaMapCatalog.Point(32730, 32860);
            default -> throw new IllegalStateException();
        };
    }

    static List<Integer> cutSizes() {
        return List.of(18, 5, 8);
    }

    private boolean validLane(int force, int roomNumber) {
        return active && force >= 0 && force <= 1
                && roomNumber >= 1 && roomNumber <= 4;
    }

    private static boolean supported(int mapId) {
        return mapId == 573 || mapId == 574;
    }

    private static boolean closed(int x, int y, int direction, int roomNumber) {
        return EDGES.stream().anyMatch(edge -> edge.x() == x && edge.y() == y
                && edge.direction() == direction
                && edge.unlockAfterRoom() >= roomNumber);
    }

    private static boolean blockedEdge(int mapId, int x, int y, int direction,
                                       int roomNumber) {
        return ArenaPassabilityCatalog.get().edgeBlocked(mapId, x, y, direction)
                || closed(x, y, direction, roomNumber);
    }

    private static Attribute attribute(Edge edge, int passable) {
        return new Attribute(edge.x(), edge.y(), edge.direction(), passable,
                edge.unlockAfterRoom(), PLACEMENT_SOURCE);
    }

    private static Edge edge(int x, int y, int direction, int unlockAfterRoom) {
        return new Edge(x, y, direction, unlockAfterRoom);
    }
}
