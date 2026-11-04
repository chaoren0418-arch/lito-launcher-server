// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class NativeSourcePacketExecutionContext {
    private static final ThreadLocal<Long> ARRIVAL_NANOS = new ThreadLocal();
    private static final ThreadLocal<Long> ARRIVAL_EPOCH_MILLIS = new ThreadLocal();
    private static final ThreadLocal<Lane> PACKET_LANE = new ThreadLocal();

    private NativeSourcePacketExecutionContext() {
    }

    public static long getArrivalNanos() {
        Long l = ARRIVAL_NANOS.get();
        return l != null ? l : 0L;
    }

    public static boolean hasArrivalNanos() {
        return ARRIVAL_NANOS.get() != null;
    }

    public static long getArrivalEpochMillis() {
        Long l = ARRIVAL_EPOCH_MILLIS.get();
        return l != null ? l : 0L;
    }

    public static boolean hasArrivalEpochMillis() {
        return ARRIVAL_EPOCH_MILLIS.get() != null;
    }

    public static boolean isNonCombatSessionLane() {
        Lane lane = PACKET_LANE.get();
        return lane == Lane.FAST_NONCOMBAT || lane == Lane.BLOCKING_NONCOMBAT;
    }

    public static boolean isBlockingNonCombatLane() {
        return PACKET_LANE.get() == Lane.BLOCKING_NONCOMBAT;
    }

    public static boolean isFastNonCombatLane() {
        return PACKET_LANE.get() == Lane.FAST_NONCOMBAT;
    }

    static void runWithArrival(long l, boolean bl, Runnable runnable) {
        NativeSourcePacketExecutionContext.runWithArrival(l, NativeSourcePacketExecutionContext.estimateEpochMillis(l), bl ? Lane.COMBAT : Lane.BLOCKING_NONCOMBAT, runnable);
    }

    static void runWithArrival(long l, Lane lane, Runnable runnable) {
        NativeSourcePacketExecutionContext.runWithArrival(l, NativeSourcePacketExecutionContext.estimateEpochMillis(l), lane, runnable);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void runWithArrival(long l, long l2, Lane lane, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Long l3 = ARRIVAL_NANOS.get();
        Long l4 = ARRIVAL_EPOCH_MILLIS.get();
        Lane lane2 = PACKET_LANE.get();
        ARRIVAL_NANOS.set(l);
        ARRIVAL_EPOCH_MILLIS.set(l2);
        PACKET_LANE.set(Objects.requireNonNull(lane, "lane"));
        try {
            runnable.run();
        }
        finally {
            if (l3 != null) {
                ARRIVAL_NANOS.set(l3);
            } else {
                ARRIVAL_NANOS.remove();
            }
            if (l4 != null) {
                ARRIVAL_EPOCH_MILLIS.set(l4);
            } else {
                ARRIVAL_EPOCH_MILLIS.remove();
            }
            if (lane2 != null) {
                PACKET_LANE.set(lane2);
            } else {
                PACKET_LANE.remove();
            }
        }
    }

    static long estimateEpochMillis(long l) {
        long l2 = System.nanoTime();
        long l3 = TimeUnit.NANOSECONDS.toMillis(l - l2);
        return System.currentTimeMillis() + l3;
    }

    static enum Lane {
        FAST_NONCOMBAT,
        BLOCKING_NONCOMBAT,
        COMBAT;

    }
}
