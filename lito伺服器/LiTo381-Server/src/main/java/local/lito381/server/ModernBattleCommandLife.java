package local.lito381.server;

import java.util.Objects;

/** Atomic admission used by the explicitly negotiated v37 mutation envelope.
 * Bare legacy request opcodes do not gain this protection by version alone.
 * A proof comes from the actor/life the client saw, never from a later read.
 */
final class ModernBattleCommandLife {
    record Proof(ModernRooms.Scope player, ModernTargetHistory.World world,
                 long entity, long life, long revision) {
        Proof {
            Objects.requireNonNull(player); Objects.requireNonNull(world);
            if (entity < 1 || entity > 100 || life < 1 || revision < 1) throw bad();
        }
        @Override public String toString() { return "BattleCommandLife[redacted]"; }
    }
    enum Rejection { NONE, NOT_ACTIVE, STALE_LIFE, DEAD, STALE_TARGET }

    /** Exactly one of a typed result or an admission rejection. Capture is
     * PRIVATE engine data; WorldService owner-scopes the recovery response.
     * A rejected admission consumes no action sequence and must never be
     * passed off as a normal typed command denial/ACK.
     */
    record Result<T extends ModernBattleCommand>(Rejection rejection,
            ModernBattleEngine.Observed<T> accepted, ModernBattleObservation.Capture observation) {
        Result {
            Objects.requireNonNull(rejection); Objects.requireNonNull(observation);
            if ((rejection == Rejection.NONE) != (accepted != null)
                    || accepted != null && accepted.observation() != observation) throw bad();
        }
        static <T extends ModernBattleCommand> Result<T> accepted(ModernBattleEngine.Observed<T> outcome) {
            return new Result<>(Rejection.NONE, outcome, outcome.observation());
        }
        static <T extends ModernBattleCommand> Result<T> rejected(Rejection reason, ModernBattleObservation.Capture observation) {
            if (reason == Rejection.NONE) throw bad();
            return new Result<>(reason, null, observation);
        }
        @Override public String toString() { return "BattleCommandLifeResult[redacted]"; }
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid battle command life"); }
    private ModernBattleCommandLife() {}
}
