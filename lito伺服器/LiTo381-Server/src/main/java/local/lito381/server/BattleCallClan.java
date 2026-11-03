package local.lito381.server;

/** Server-owned destination geometry for native skill 116 Call Clan. */
final class BattleCallClan {
    static final int QUESTION_ID = 729;
    static final int INVITE_TIMEOUT_SECONDS = 30;

    private static final int[] HEADING_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    @FunctionalInterface
    interface TileProbe {
        boolean test(int x, int y);
    }

    record Destination(int mapId, int x, int y, int heading) {
        Destination {
            if (mapId < 0 || mapId > 0xFFFF || x <= 0 || x > 0xFFFF
                    || y <= 0 || y > 0xFFFF || heading < 0 || heading > 7) {
                throw new IllegalArgumentException("invalid Call Clan destination");
            }
        }
    }

    private BattleCallClan() {}

    /**
     * Retained C_Attr geometry: one tile in the caller's captured heading;
     * the arriving clan member faces back toward the caller.
     */
    static Destination adjacentDestination(
            int mapId, int callerX, int callerY, int callerHeading,
            TileProbe standingFloor, TileProbe occupied) {
        if (standingFloor == null || occupied == null || mapId < 0 || mapId > 0xFFFF
                || callerX <= 0 || callerX > 0xFFFF
                || callerY <= 0 || callerY > 0xFFFF) {
            throw new IllegalArgumentException("invalid Call Clan geometry inputs");
        }
        int heading = Math.floorMod(callerHeading, 8);
        int x = callerX + HEADING_X[heading];
        int y = callerY + HEADING_Y[heading];
        if (x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                || !standingFloor.test(x, y) || occupied.test(x, y)) {
            return null;
        }
        return new Destination(mapId, x, y, (heading + 4) & 7);
    }
}
