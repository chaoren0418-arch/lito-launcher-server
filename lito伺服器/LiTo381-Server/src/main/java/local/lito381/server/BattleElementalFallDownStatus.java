package local.lito381.server;

/** Authoritative retained skill-133 elemental-resistance lease. */
final class BattleElementalFallDownStatus {
    static final int MODIFIER = -50;

    record Activation(boolean applied, boolean refreshed, int attributeKind,
                      int remainingSeconds) {}

    record Cancellation(boolean present, int attributeKind) {}

    record Snapshot(boolean active, int attributeKind, int modifier,
                    int remainingSeconds) {}

    private long expiresAtNanos;
    private int attributeKind;

    synchronized Activation activate(
            long nowNanos, int durationSeconds, int sourceElfAttribute) {
        if (nowNanos < 0 || durationSeconds <= 0
                || sourceElfAttribute < 0 || sourceElfAttribute > 0x0F) {
            throw new IllegalArgumentException(
                    "invalid Elemental Fall Down activation");
        }
        boolean active = expiresAtNanos > nowNanos;
        boolean applied = !active;
        if (!active) {
            attributeKind = retainedAttribute(sourceElfAttribute);
        }
        expiresAtNanos = Math.addExact(nowNanos,
                Math.multiplyExact((long) durationSeconds, 1_000_000_000L));
        return new Activation(applied, active, attributeKind, durationSeconds);
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
        expiresAtNanos = 0;
        attributeKind = 0;
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
}
