package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative, refreshable STATUS_DRAGON_GEM_EXP lease. */
final class BattleExperienceGemStatus {
    record Snapshot(BattleExperienceGemCatalog.Gem gem, int remainingSeconds) {
        Snapshot {
            if (remainingSeconds < 0 || (gem == null && remainingSeconds != 0)
                    || (gem != null && remainingSeconds == 0)) {
                throw new IllegalArgumentException("invalid Dragon Gem status snapshot");
            }
        }

        boolean active() {
            return gem != null;
        }

        int bonusExperience() {
            return active() ? gem.bonusExperience() : 0;
        }
    }

    private BattleExperienceGemCatalog.Gem activeGem;
    private long expiresAtNanos;

    synchronized Snapshot activate(BattleExperienceGemCatalog.Gem gem, long nowNanos) {
        if (gem == null || nowNanos < 0) {
            throw new IllegalArgumentException("invalid Dragon Gem activation");
        }
        activeGem = gem;
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(gem.durationSeconds()));
        return snapshot(nowNanos);
    }

    synchronized Snapshot restore(int bonusExperience, int remainingSeconds,
                                  long nowNanos) {
        BattleExperienceGemCatalog.Gem gem =
                BattleExperienceGemCatalog.findByBonus(bonusExperience);
        if (gem == null || remainingSeconds <= 0
                || remainingSeconds > BattleExperienceGemCatalog.DURATION_SECONDS
                || nowNanos < 0) {
            clear();
            return new Snapshot(null, 0);
        }
        activeGem = gem;
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(remainingSeconds));
        return snapshot(nowNanos);
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Dragon Gem clock");
        if (activeGem == null || nowNanos >= expiresAtNanos) {
            return new Snapshot(null, 0);
        }
        return new Snapshot(activeGem, remainingSeconds(expiresAtNanos, nowNanos));
    }

    synchronized BattleExperienceGemCatalog.Gem expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Dragon Gem clock");
        if (activeGem == null || nowNanos < expiresAtNanos) return null;
        BattleExperienceGemCatalog.Gem expired = activeGem;
        clear();
        return expired;
    }

    synchronized void clear() {
        activeGem = null;
        expiresAtNanos = 0;
    }

    private static int remainingSeconds(long deadline, long nowNanos) {
        long second = TimeUnit.SECONDS.toNanos(1);
        long remaining = Math.max(1L, deadline - nowNanos);
        return (int) Math.min(0xFFFFL, (remaining + second - 1L) / second);
    }
}
