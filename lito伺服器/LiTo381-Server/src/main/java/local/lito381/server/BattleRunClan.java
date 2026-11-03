package local.lito381.server;

/** Server-owned destination snapshot for native skill 118 Run Clan. */
final class BattleRunClan {
    static final int ARRIVAL_HEADING = 5;
    static final int PORTAL_TIMEOUT_SECONDS = 30;

    record Destination(int mapId, int x, int y, int heading) {
        Destination {
            if (mapId < 0 || mapId > 0xFFFF || x <= 0 || x > 0xFFFF
                    || y <= 0 || y > 0xFFFF || heading != ARRIVAL_HEADING) {
                throw new IllegalArgumentException("invalid Run Clan destination");
            }
        }
    }

    private BattleRunClan() {}

    /**
     * Retained RUN_CLAN teleports the caster to the selected clan member's
     * exact X/Y and uses heading 5.  The target's authoritative occupied tile
     * is therefore intentional and must not be replaced by guessed adjacency.
     */
    static Destination exactDestination(int mapId, int targetX, int targetY) {
        return new Destination(mapId, targetX, targetY, ARRIVAL_HEADING);
    }
}
