// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
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

    void bind(ClientSession owner) { iconOwner = owner; }

    private void icon(int seconds) {
        if (iconOwner != null) DragonCombat.sendSkinIcon(iconOwner, seconds);
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int reduction) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || reduction <= 0 || reduction > 127) {
            throw new IllegalArgumentException("invalid native Dragon Skin lease");
        }
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleDragonSkinStatus battleDragonSkinStatus;
        (battleDragonSkinStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleDragonSkinStatus);
        this.damageReduction = reduction;
        this.icon(durationSeconds);
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Dragon Skin time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, damageReduction, remaining);
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Dragon Skin time");
        }
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) return false;
        clear();
        return true;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        clear();
        return true;
    }

    synchronized void clear() {
        boolean bl = this.expiresAtNanos != 0L;
        BattleDragonSkinStatus battleDragonSkinStatus;
        (battleDragonSkinStatus = this).expiresAtNanos = 0L;
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

        int applyTo(int damage) {
            if (damage < 0) {
                throw new IllegalArgumentException("negative physical damage");
            }
            return active ? Math.max(0, damage - damageReduction) : damage;
        }
    }
}
