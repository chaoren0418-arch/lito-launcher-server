package local.lito381.server;

/** Authoritative retained skill-133 elemental-resistance lease. */
final class BattleElementalFallDownStatus {
    static final int MODIFIER = -50;
    private long expiresAtNanos;
    private int attributeKind;
    volatile transient long nativeSourceIcons1Version;

    BattleElementalFallDownStatus() {
    }

    synchronized Activation activate(long l, int n, int n2) {
        boolean bl;
        if (l < 0L || n <= 0 || n2 < 0 || n2 > 15) {
            throw new IllegalArgumentException("invalid Elemental Fall Down activation");
        }
        boolean bl2 = this.expiresAtNanos > l;
        boolean bl3 = bl = !bl2;
        if (!bl2) {
            this.attributeKind = BattleElementalFallDownStatus.retainedAttribute(n2);
        }
        long l2 = Math.addExact(l, Math.multiplyExact((long)n, 1000000000L));
        BattleElementalFallDownStatus battleElementalFallDownStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleElementalFallDownStatus);
        return new Activation(bl, bl2, this.attributeKind, n);
    }

    synchronized int modifier(int n, long l) {
        if (this.expiresAtNanos <= l) {
            return 0;
        }
        return this.attributeKind != 0 && this.attributeKind == n ? -50 : 0;
    }

    synchronized Snapshot snapshot(long l) {
        if (this.expiresAtNanos <= l) {
            return new Snapshot(false, 0, 0, 0);
        }
        long l2 = this.expiresAtNanos - l;
        int n = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, (l2 + 999999999L) / 1000000000L));
        return new Snapshot(true, this.attributeKind, this.attributeKind == 0 ? 0 : -50, n);
    }

    synchronized boolean expire(long l) {
        if (this.expiresAtNanos == 0L || this.expiresAtNanos > l) {
            return false;
        }
        this.clear();
        return true;
    }

    synchronized Cancellation cancel() {
        if (this.expiresAtNanos == 0L) {
            return new Cancellation(false, 0);
        }
        Cancellation cancellation = new Cancellation(true, this.attributeKind);
        this.clear();
        return cancellation;
    }

    synchronized void clear() {
        long l = 0L;
        BattleElementalFallDownStatus battleElementalFallDownStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleElementalFallDownStatus);
        this.attributeKind = 0;
    }

    static int retainedAttribute(int n) {
        if (n < 0 || n > 15) {
            throw new IllegalArgumentException("invalid ElfAttr bitmask");
        }
        for (int n2 : new int[]{1, 2, 4, 8}) {
            if ((n & n2) == 0) continue;
            return n2;
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
