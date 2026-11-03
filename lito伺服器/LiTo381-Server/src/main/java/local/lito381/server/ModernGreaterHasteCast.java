package local.lito381.server;

import java.util.Objects;

/** Internal self-only command. No opcode, client clock, price or duration.
 * Must not be registered in a service until its new wire/client capability exists. */
final class ModernGreaterHasteCast {
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP, TOO_HEAVY, STALE_STATE, NUMERIC }
    record Request(ModernRooms.Scope scope,long matchId,long generation,long sequence,
            long equipmentRevision,long life,int x,int y,long buffRevision) {
        Request {
            Objects.requireNonNull(scope);
            if(matchId<1||generation<1||sequence<1||equipmentRevision<0||life<1||buffRevision<0)
                throw new IllegalArgumentException("invalid greater haste command");
        }
        @Override public String toString(){return "GreaterHasteRequest[redacted]";}
    }
    private ModernGreaterHasteCast(){}
}
