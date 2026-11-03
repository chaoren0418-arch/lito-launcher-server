package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Transient cancelable flat physical-damage reduction for native skill181. */
final class BattleDragonSkinStatus {
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

    private ClientSession iconOwner;
    void bind(ClientSession owner) { iconOwner = owner; }
    private void icon(int seconds) {
        if (iconOwner != null) DragonCombat.sendSkinIcon(iconOwner, seconds);
    }
    private long expiresAtNanos;
    private int damageReduction;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int reduction) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || reduction <= 0 || reduction > 127) {
            throw new IllegalArgumentException("invalid native Dragon Skin lease");
        }
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        damageReduction = reduction;
        icon(durationSeconds);
        return refreshed;
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
        boolean wasActive = expiresAtNanos != 0;
        expiresAtNanos = 0;
        damageReduction = 0;
        if (wasActive) icon(0);
    }
}
