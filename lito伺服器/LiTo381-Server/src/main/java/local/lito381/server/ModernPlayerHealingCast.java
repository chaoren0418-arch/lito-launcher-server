package local.lito381.server;

import java.util.Objects;

/** Typed engine intent only. No wire opcode, client amount, MP cost or clock. */
final class ModernPlayerHealingCast {
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP, TOO_HEAVY,
        STALE_STATE, NUMERIC, TARGET_DEAD, STALE_TARGET, WRONG_INSTANCE, OUT_OF_RANGE, BLOCKED }
    record Target(ModernTargetHistory.Participant participant,long life,long equipmentRevision,ModernWorldMap.Cell cell) {
        Target {Objects.requireNonNull(participant);Objects.requireNonNull(cell);if(life<1||equipmentRevision<0)throw bad();}
    }
    record Request(ModernRooms.Scope scope,long matchId,long generation,long sequence,
            long equipmentRevision,long life,ModernWorldMap.Cell cell,Target target) {
        Request {
            Objects.requireNonNull(scope);Objects.requireNonNull(cell);Objects.requireNonNull(target);
            if(matchId<1||generation<1||sequence<1||equipmentRevision<0||life<1)throw bad();
        }
        @Override public String toString(){return "PlayerHealingRequest[redacted]";}
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid player healing intent");}
    private ModernPlayerHealingCast(){}
}
