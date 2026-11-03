package local.lito381.server;

import java.util.Objects;

/** Engine skill43 intent; no client cost/duration/time or implicit authority. */
final class ModernHasteCast {
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP, TOO_HEAVY,
        STALE_STATE, NUMERIC, TARGET_DEAD, STALE_TARGET, WRONG_INSTANCE, OUT_OF_RANGE }
    sealed interface Target permits Player, Npc { long life(); ModernWorldMap.Cell cell(); }
    record Player(ModernTargetHistory.Participant participant, long life, long equipmentRevision,
                  ModernWorldMap.Cell cell, long buffRevision) implements Target {
        Player {
            Objects.requireNonNull(participant); Objects.requireNonNull(cell);
            if (life < 1 || equipmentRevision < 0 || buffRevision < 0) throw bad();
        }
    }
    record Npc(ModernNpcRoster.Id id, long life, long changedRevision, ModernWorldMap.Cell cell) implements Target {
        Npc { Objects.requireNonNull(id); Objects.requireNonNull(cell); if (life < 1 || changedRevision < 1) throw bad(); }
    }
    record Request(ModernRooms.Scope scope, long matchId, long generation, long sequence,
                   long equipmentRevision, long life, ModernWorldMap.Cell cell, long buffRevision, Target target) {
        Request {
            Objects.requireNonNull(scope); Objects.requireNonNull(cell); Objects.requireNonNull(target);
            if (matchId < 1 || generation < 1 || sequence < 1 || equipmentRevision < 0 || life < 1 || buffRevision < 0) throw bad();
        }
        @Override public String toString() { return "HasteRequest[redacted]"; }
    }
    /** Exact native Point.isInScreen shape, with wide subtraction at int edges.
     * Native skill43 uses this shape, not an attack radius or line-of-sight gate. */
    static boolean inScreen(ModernWorldMap.Cell from, ModernWorldMap.Cell to) {
        long dx = (long) to.x() - from.x(), dy = (long) to.y() - from.y();
        long distance = Math.abs(dx) + Math.abs(dy);
        if (distance > 19) return false;
        if (distance <= 18) return true;
        long edge = Math.abs(dx + 18) + Math.abs(dy + 18);
        return edge >= 19 && edge <= 52;
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern haste intent"); }
    private ModernHasteCast() {}
}
