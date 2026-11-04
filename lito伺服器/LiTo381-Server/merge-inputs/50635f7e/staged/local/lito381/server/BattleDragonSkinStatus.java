package local.lito381.server;

/** Transient cancelable flat physical-damage reduction for native skill181. */
import java.util.concurrent.TimeUnit;

final class BattleDragonSkinStatus {
    private ClientSession iconOwner;
    private long expiresAtNanos;
    private int damageReduction;
    volatile transient long nativeSourceIcons1Version;

    BattleDragonSkinStatus() {
    }

    void bind(ClientSession clientSession) {
        this.iconOwner = clientSession;
    }

    private void icon(int n) {
        if (this.iconOwner != null) {
            DragonCombat.sendSkinIcon(this.iconOwner, n);
        }
    }

    synchronized boolean activate(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Dragon Skin lease");
        }
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleDragonSkinStatus battleDragonSkinStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleDragonSkinStatus);
        this.damageReduction = n2;
        this.icon(n);
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Dragon Skin time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, this.damageReduction, n);
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Dragon Skin time");
        }
        if (this.expiresAtNanos == 0L || l < this.expiresAtNanos) {
            return false;
        }
        this.clear();
        return true;
    }

    synchronized boolean cancel() {
        if (this.expiresAtNanos == 0L) {
            return false;
        }
        this.clear();
        return true;
    }

    synchronized void clear() {
        boolean bl = this.expiresAtNanos != 0L;
        long l = 0L;
        BattleDragonSkinStatus battleDragonSkinStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleDragonSkinStatus);
        this.damageReduction = 0;
        if (bl) {
            this.icon(0);
        }
    }

    record Snapshot(boolean active, int damageReduction, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }

        int applyTo(int n) {
            if (n < 0) {
                throw new IllegalArgumentException("negative physical damage");
            }
            return this.active ? Math.max(0, n - this.damageReduction) : n;
        }
    }
}
