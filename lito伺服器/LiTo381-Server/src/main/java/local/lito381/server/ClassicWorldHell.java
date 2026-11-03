package local.lito381.server;

import java.util.List;

/** Exact retained PK-punishment lifecycle constants from L1PcInstance/PcHellTimer. */
final class ClassicWorldHell {
    static final String PC_SOURCE_SHA256 =
            "D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F";
    static final String TIMER_SOURCE_SHA256 =
            "6627C8D2B5F2433920519F1ED43C99F82144C8056EFEAE220B1348B92903F913";
    static final String RESTART_SOURCE_SHA256 =
            "69E421ABEB5FE0BF168D179EA9C3719AFEE6FC8E41D04CF9696DA14AFCBAB7B8";
    static final String LOGIN_SOURCE_SHA256 =
            "C02DFC9571980226F9B49D518C8C6CD8072AFE4690A9319B34C98D110E0999E5";
    static final String TOWN_SOURCE_SHA256 =
            "DAEF00602E793678BFD1C9BC0E210D2E9856F2A7C45FC38D9D322A5A22A5EDE3";
    static final String DATABASE_SOURCE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String MAP_SOURCE_SHA256 =
            "2DF903D97C556E5ECBA2A9527C6D3CCC1B371B474A4FC69E7599570F1EF8E90C";
    static final int MAP_ID = 666;
    static final int ENTRY_X = 32701;
    static final int ENTRY_Y = 32777;
    static final int HEADING = 5;
    static final long TICK_NANOS = 1_100_000_000L;
    static final int PK_THRESHOLD = 100;

    record Destination(int mapId, int x, int y, int heading) {}

    private static final List<Destination> ORCISH_FOREST_RETURNS = List.of(
            new Destination(4, 32750, 32435, HEADING),
            new Destination(4, 32745, 32447, HEADING),
            new Destination(4, 32738, 32452, HEADING),
            new Destination(4, 32741, 32436, HEADING),
            new Destination(4, 32749, 32446, HEADING));

    private ClassicWorldHell() {}

    static int durationTicks(int pkCount) {
        if (pkCount < 0) throw new IllegalArgumentException("negative PK count");
        if (pkCount <= 10) return 300;
        return Math.addExact(Math.multiplyExact(300, pkCount - 10), 300);
    }

    static Destination entry() {
        return new Destination(MAP_ID, ENTRY_X, ENTRY_Y, HEADING);
    }

    static Destination returnDestination(int index) {
        return ORCISH_FOREST_RETURNS.get(Math.floorMod(index,
                ORCISH_FOREST_RETURNS.size()));
    }

    static List<Destination> returnDestinations() {
        return ORCISH_FOREST_RETURNS;
    }

    /** Monotonic, fixed-rate 1100 ms clock matching retained PcHellTimer. */
    static final class Clock {
        private long nextTickNanos;

        void reset(long nowNanos) {
            nextTickNanos = saturatingAdd(nowNanos, TICK_NANOS);
        }

        void clear() {
            nextTickNanos = 0;
        }

        int pollDue(long nowNanos) {
            if (nextTickNanos == 0) {
                reset(nowNanos);
                return 0;
            }
            if (nowNanos < nextTickNanos) return 0;
            long elapsed = nowNanos - nextTickNanos;
            long due = 1L + elapsed / TICK_NANOS;
            long advance = due > Long.MAX_VALUE / TICK_NANOS
                    ? Long.MAX_VALUE : due * TICK_NANOS;
            nextTickNanos = saturatingAdd(nextTickNanos, advance);
            return (int) Math.min(Integer.MAX_VALUE, due);
        }

        private static long saturatingAdd(long left, long right) {
            if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
            return left + right;
        }
    }
}
