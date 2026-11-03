package local.lito381.server;

import java.util.Objects;

/** Trusted service receipt, NOT a wire DTO. The command retains private engine
 * data; only view() is owner-filtered for the negotiated operation codec.
 * Equipment SET still needs a room-wide capability gate and its own receipt. */
record ModernBattleActionReceipt(ModernBattleCommandLife.Rejection rejection,
        boolean available, ModernBattleCommand outcome, ModernBattleAudience.View view,
        ModernBattleLifecycle lifecycle) {
    ModernBattleActionReceipt {
        Objects.requireNonNull(rejection);Objects.requireNonNull(view);Objects.requireNonNull(lifecycle);
        if((available&&rejection==ModernBattleCommandLife.Rejection.NONE)!=(outcome!=null)
                ||!available&&rejection!=ModernBattleCommandLife.Rejection.NONE
                ||lifecycle.revision()<=view.world().revision()
                ||!Objects.equals(lifecycle.result(),view.world().result())
                ||outcome!=null&&(outcome.observation().revision()!=view.world().revision()
                    ||!outcome.observation().world().identity().equals(view.world().identity())))
            throw new IllegalArgumentException("invalid canonical action receipt");
    }
    @Override public String toString(){return "BattleActionReceipt[redacted]";}
}
