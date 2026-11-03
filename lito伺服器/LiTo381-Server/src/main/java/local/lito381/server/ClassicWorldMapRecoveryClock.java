package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Per-player independent retained map-environment cadence. */
final class ClassicWorldMapRecoveryClock {
    private int mapId = Integer.MIN_VALUE;
    private int ruleId = -1;
    private long deadlineNanos;

    synchronized boolean poll(int candidateMapId,
                              ClassicWorldMapRecoveryCatalog.Rule rule,
                              long nowNanos) {
        if (candidateMapId < 0 || nowNanos < 0) {
            throw new IllegalArgumentException("invalid map-recovery clock input");
        }
        if (rule == null) {
            reset();
            mapId = candidateMapId;
            return false;
        }
        if (rule.mapId() != candidateMapId && rule.mapId() != -1) {
            throw new IllegalArgumentException("map-recovery rule mismatch");
        }
        if (mapId != candidateMapId || ruleId != rule.id() || deadlineNanos == 0) {
            mapId = candidateMapId;
            ruleId = rule.id();
            deadlineNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(rule.intervalSeconds()));
            return false;
        }
        if (nowNanos < deadlineNanos) return false;
        deadlineNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(rule.intervalSeconds()));
        return true;
    }

    synchronized void reset() {
        mapId = Integer.MIN_VALUE;
        ruleId = -1;
        deadlineNanos = 0;
    }
}
