package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Server-authoritative lease, group exclusion and per-item delay for Dragon Eyes. */
final class BattleDragonEyeStatus {
    enum Outcome { APPLIED, ITEM_COOLDOWN, GROUP_CONFLICT }

    record Activation(Outcome outcome, int remainingSeconds, int remainingCooldownMillis) {
        Activation {
            if (outcome == null || remainingSeconds < 0 || remainingCooldownMillis < 0) {
                throw new IllegalArgumentException("invalid Dragon Eye activation result");
            }
        }

        boolean applied() {
            return outcome == Outcome.APPLIED;
        }
    }

    record Snapshot(BattleDragonEyeCatalog.Eye eye, int remainingSeconds) {
        boolean active() {
            return eye != null && remainingSeconds > 0;
        }

        int magicHit() {
            return active() ? eye.magicHit() : 0;
        }

        int meleeDamage() {
            return active() ? eye.meleeDamage() : 0;
        }

        int damageReduction() {
            return active() ? eye.damageReduction() : 0;
        }

        int evasionRate() {
            return active() ? eye.evasionRate() : 0;
        }

        int stoneResistance() {
            return active() ? eye.stoneResistance() : 0;
        }

        int sleepResistance() {
            return active() ? eye.sleepResistance() : 0;
        }

        int freezeResistance() {
            return active() ? eye.freezeResistance() : 0;
        }

        int poisonResistance() {
            return active() ? eye.poisonResistance() : 0;
        }
    }

    private final Map<Long, Long> itemReadyAtNanos = new HashMap<>();
    private BattleDragonEyeCatalog.Eye activeEye;
    private long expiresAtNanos;
    private final Map<Integer, Long> sharedReadyAtNanos = new HashMap<>();

    synchronized Activation activate(
            long itemObjectId, BattleDragonEyeCatalog.Eye eye, long nowNanos) {
        if (itemObjectId <= 0 || itemObjectId > 0xFFFF_FFFFL
                || eye == null || nowNanos < 0) {
            throw new IllegalArgumentException("invalid Dragon Eye activation");
        }
        long itemReady = itemReadyAtNanos.getOrDefault(itemObjectId, 0L);
        long sharedReady = sharedReadyAtNanos.getOrDefault(eye.sharedDelayId(), 0L);
        long readyAt = Math.max(sharedReady, itemReady);
        if (nowNanos < readyAt) {
            long remaining = readyAt - nowNanos;
            return new Activation(Outcome.ITEM_COOLDOWN, 0,
                    (int) Math.min(Integer.MAX_VALUE,
                            (remaining + 999_999L) / 1_000_000L));
        }
        clearExpired(nowNanos);
        if (activeEye != null && nowNanos < expiresAtNanos && !eye.canOverwrite()) {
            return new Activation(Outcome.GROUP_CONFLICT,
                    remainingSeconds(expiresAtNanos, nowNanos), 0);
        }
        activeEye = eye;
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(eye.durationSeconds()));
        sharedReadyAtNanos.put(eye.sharedDelayId(), Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(eye.sharedDelayMillis())));
        itemReadyAtNanos.put(itemObjectId, Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(eye.itemDelayMillis())));
        return new Activation(Outcome.APPLIED, eye.durationSeconds(), 0);
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Dragon Eye clock");
        if (activeEye == null || nowNanos >= expiresAtNanos) return new Snapshot(null, 0);
        return new Snapshot(activeEye, remainingSeconds(expiresAtNanos, nowNanos));
    }

    synchronized BattleDragonEyeCatalog.Eye expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Dragon Eye clock");
        if (activeEye == null || nowNanos < expiresAtNanos) return null;
        BattleDragonEyeCatalog.Eye expired = activeEye;
        activeEye = null;
        expiresAtNanos = 0;
        return expired;
    }

    synchronized void clear() {
        activeEye = null;
        expiresAtNanos = 0;
        sharedReadyAtNanos.clear();
        itemReadyAtNanos.clear();
    }

    private void clearExpired(long nowNanos) {
        if (activeEye != null && nowNanos >= expiresAtNanos) {
            activeEye = null;
            expiresAtNanos = 0;
        }
    }

    private static int remainingSeconds(long deadline, long nowNanos) {
        long remaining = deadline - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        return (int) Math.min(0xFFFFL, Math.max(1L, (remaining + second - 1) / second));
    }
}
