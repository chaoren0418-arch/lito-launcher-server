// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative retained skill-133 elemental-resistance lease. */
final class BattleElementalFallDownStatus {
    static final int MODIFIER = -50;
    private long expiresAtNanos;
    private int attributeKind;
    volatile transient long nativeSourceIcons1Version;

    BattleElementalFallDownStatus() {
    }

    synchronized Activation activate(long nowNanos, int durationSeconds, int sourceElfAttribute) {
        boolean bl;
        if (nowNanos < 0L || durationSeconds <= 0 || sourceElfAttribute < 0 || sourceElfAttribute > 15) {
            throw new IllegalArgumentException("invalid Elemental Fall Down activation");
        }
        boolean bl2 = this.expiresAtNanos > nowNanos;
        boolean bl3 = bl = !bl2;
        if (!bl2) {
            this.attributeKind = BattleElementalFallDownStatus.retainedAttribute(sourceElfAttribute);
        }
        BattleElementalFallDownStatus battleElementalFallDownStatus;
        (battleElementalFallDownStatus = this).expiresAtNanos = Math.addExact(nowNanos, Math.multiplyExact((long)durationSeconds, 1000000000L));
        NativeSourceIcons1Epoch.bump(battleElementalFallDownStatus);
        return new Activation(bl, bl2, this.attributeKind, durationSeconds);
    }

    synchronized int modifier(int requestedAttribute, long nowNanos) {
        if (expiresAtNanos <= nowNanos) return 0;
        return attributeKind != 0 && attributeKind == requestedAttribute
                ? MODIFIER : 0;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (expiresAtNanos <= nowNanos) {
            return new Snapshot(false, 0, 0, 0);
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        int remainingSeconds = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, (remainingNanos + 999_999_999L) / 1_000_000_000L));
        return new Snapshot(true, attributeKind,
                attributeKind == 0 ? 0 : MODIFIER, remainingSeconds);
    }

    synchronized boolean expire(long nowNanos) {
        if (expiresAtNanos == 0 || expiresAtNanos > nowNanos) return false;
        clear();
        return true;
    }

    synchronized Cancellation cancel() {
        if (expiresAtNanos == 0) return new Cancellation(false, 0);
        Cancellation result = new Cancellation(true, attributeKind);
        clear();
        return result;
    }

    synchronized void clear() {
        BattleElementalFallDownStatus battleElementalFallDownStatus;
        (battleElementalFallDownStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleElementalFallDownStatus);
        this.attributeKind = 0;
    }

    static int retainedAttribute(int sourceElfAttribute) {
        if (sourceElfAttribute < 0 || sourceElfAttribute > 0x0F) {
            throw new IllegalArgumentException("invalid ElfAttr bitmask");
        }
        for (int attribute : new int[] {1, 2, 4, 8}) {
            if ((sourceElfAttribute & attribute) != 0) return attribute;
        }
        return 0;
    }

    record Activation(boolean applied, boolean refreshed, int attributeKind, int remainingSeconds) {
    }

    record Snapshot(boolean active, int attributeKind, int modifier, int remainingSeconds) {
    }

    record Cancellation(boolean present, int attributeKind) {
    }
}
