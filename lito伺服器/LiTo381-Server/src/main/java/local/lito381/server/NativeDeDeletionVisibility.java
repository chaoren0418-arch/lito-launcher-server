// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.TimeUnit;
import local.lito381.server.NativeDeDeletionScope;

public final class NativeDeDeletionVisibility {
    private volatile Marker _marker = new Marker(Phase.ACTIVE, 0L, 0L);

    public void beginDeletion() {
        long now = NativeDeDeletionScope.nanoTime();
        this._marker = new Marker(Phase.DELETE_CLAIMED, now, now);
    }

    public void transition(Phase phase) {
        if (phase == null || phase == Phase.ACTIVE || phase == Phase.DELETE_CLAIMED) {
            throw new IllegalArgumentException("invalid deletion phase transition: " + String.valueOf((Object)phase));
        }
        long now = NativeDeDeletionScope.nanoTime();
        Marker current = this._marker;
        long deleteStarted = current.deleteStartedNanos > 0L ? current.deleteStartedNanos : now;
        this._marker = new Marker(phase, deleteStarted, now);
    }

    public void resetActive() {
        this._marker = new Marker(Phase.ACTIVE, 0L, 0L);
    }

    public void registrationAborted() {
        long now = NativeDeDeletionScope.nanoTime();
        this._marker = new Marker(Phase.REGISTRATION_ABORTED, now, now);
    }

    public Snapshot snapshot() {
        return this.snapshot(NativeDeDeletionScope.nanoTime());
    }

    Snapshot snapshot(long nowNanos) {
        Marker marker = this._marker;
        return new Snapshot(marker.phase, NativeDeDeletionVisibility.elapsedMillis(marker.deleteStartedNanos, nowNanos), NativeDeDeletionVisibility.elapsedMillis(marker.phaseStartedNanos, nowNanos));
    }

    public static RepairKind classifyRepair(Snapshot snapshot, boolean deletionStarted, long stallMillis) {
        if (snapshot == null) {
            return RepairKind.LEAK;
        }
        return switch (snapshot.phase()) {
            default -> throw new IncompatibleClassChangeError();
            case DELETE_CLAIMED, DESTROYED_MARKED, VIEWERS_CAPTURED, WORLD_REMOVED -> {
                if (!deletionStarted) {
                    yield RepairKind.LEAK;
                }
                if (snapshot.deleteAgeMillis() >= Math.max(1L, stallMillis)) {
                    yield RepairKind.STALL;
                }
                yield RepairKind.NORMAL_RACE;
            }
            case ACTIVE -> {
                if (deletionStarted) {
                    yield RepairKind.NORMAL_RACE;
                }
                yield RepairKind.LEAK;
            }
            case REMOVE_PUBLISHED, TERMINAL, FAILED, REGISTRATION_ABORTED -> RepairKind.LEAK;
        };
    }

    private static long elapsedMillis(long startedNanos, long nowNanos) {
        if (startedNanos <= 0L || nowNanos <= startedNanos) {
            return 0L;
        }
        return TimeUnit.NANOSECONDS.toMillis(nowNanos - startedNanos);
    }

    private static final class Marker {
        private final Phase phase;
        private final long deleteStartedNanos;
        private final long phaseStartedNanos;

        private Marker(Phase phase, long deleteStartedNanos, long phaseStartedNanos) {
            this.phase = phase;
            this.deleteStartedNanos = deleteStartedNanos;
            this.phaseStartedNanos = phaseStartedNanos;
        }
    }

    public static enum Phase {
        ACTIVE,
        DELETE_CLAIMED,
        DESTROYED_MARKED,
        VIEWERS_CAPTURED,
        WORLD_REMOVED,
        REMOVE_PUBLISHED,
        TERMINAL,
        FAILED,
        REGISTRATION_ABORTED;

    }

    public static final class Snapshot {
        private final Phase _phase;
        private final long _deleteAgeMillis;
        private final long _phaseAgeMillis;

        public Snapshot(Phase phase, long deleteAgeMillis, long phaseAgeMillis) {
            this._phase = phase == null ? Phase.ACTIVE : phase;
            this._deleteAgeMillis = Math.max(0L, deleteAgeMillis);
            this._phaseAgeMillis = Math.max(0L, phaseAgeMillis);
        }

        public Phase phase() {
            return this._phase;
        }

        public long deleteAgeMillis() {
            return this._deleteAgeMillis;
        }

        public long phaseAgeMillis() {
            return this._phaseAgeMillis;
        }

        public boolean deletionInProgress() {
            return switch (this._phase) {
                case DELETE_CLAIMED, DESTROYED_MARKED, VIEWERS_CAPTURED, WORLD_REMOVED, REMOVE_PUBLISHED -> true;
                default -> false;
            };
        }
    }

    public static enum RepairKind {
        NORMAL_RACE,
        LEAK,
        STALL;

    }
}
