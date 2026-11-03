package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Independent cancelable AC lease for selected skill168 coexist mode. */
final class BattleIronSkinStatus {
    record Snapshot(boolean active, int armorClass, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int armorClass;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int armorClassModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || armorClassModifier >= 0 || armorClassModifier < -127) {
            throw new IllegalArgumentException("invalid native Iron Skin lease");
        }
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        armorClass = armorClassModifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Iron Skin time");
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, armorClass, remaining);
    }

    synchronized int armorClass(long nowNanos) {
        return snapshot(nowNanos).armorClass();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Iron Skin time");
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
        expiresAtNanos = 0;
        armorClass = 0;
    }
}
