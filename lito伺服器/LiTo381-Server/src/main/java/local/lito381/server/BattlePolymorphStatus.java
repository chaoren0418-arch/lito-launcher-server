package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative timed appearance selected through a polymorph item. */
final class BattlePolymorphStatus {
    private BattlePolymorphCatalog.Form form;
    private long expiresAtNanos;

    synchronized void activate(long nowNanos, BattlePolymorphCatalog.Form selected,
                               int durationSeconds) {
        if (nowNanos < 0 || selected == null
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native polymorph timing");
        }
        form = selected;
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized BattlePolymorphCatalog.Form active(long nowNanos) {
        if (form == null || nowNanos >= expiresAtNanos) return null;
        return form;
    }

    synchronized BattlePolymorphCatalog.Form expire(long nowNanos) {
        if (form == null || nowNanos < expiresAtNanos) return null;
        BattlePolymorphCatalog.Form expired = form;
        clear();
        return expired;
    }

    synchronized BattlePolymorphCatalog.Form clear() {
        BattlePolymorphCatalog.Form previous = form;
        form = null;
        expiresAtNanos = 0;
        return previous;
    }
}
