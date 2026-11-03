package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative cancelable lease for native Elf skill147 Elemental Protection. */
final class BattleElementalProtectionStatus {
    record Snapshot(boolean active, int attributeMask, int resistanceModifier,
                    int remainingSeconds) {}

    record Cancellation(boolean present, int attributeMask) {}

    private long expiresAtNanos;
    private int attributeMask;
    private int resistanceModifier;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int sourceElfAttribute,
            int modifier) {
        validate(nowNanos, durationSeconds, sourceElfAttribute, modifier);
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        // Retain the cast-time ElfAttr mask.  The original NPC executor removes
        // this effect before changing ElfAttr, so a later attribute mutation
        // cannot redirect or corrupt the stop path.
        attributeMask = sourceElfAttribute;
        resistanceModifier = modifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException(
                    "invalid Elemental Protection time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return new Snapshot(false, 0, 0, 0);
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, attributeMask, resistanceModifier, remaining);
    }

    synchronized int modifier(int requestedAttribute, long nowNanos) {
        if (!isSingleAttribute(requestedAttribute)) return 0;
        Snapshot current = snapshot(nowNanos);
        return current.active()
                && (current.attributeMask() & requestedAttribute) != 0
                ? current.resistanceModifier() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException(
                    "invalid Elemental Protection time");
        }
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) return false;
        clear();
        return true;
    }

    synchronized Cancellation cancel() {
        if (expiresAtNanos == 0) return new Cancellation(false, 0);
        Cancellation result = new Cancellation(true, attributeMask);
        clear();
        return result;
    }

    synchronized void clear() {
        expiresAtNanos = 0;
        attributeMask = 0;
        resistanceModifier = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int sourceElfAttribute,
            int modifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || sourceElfAttribute <= 0
                || (sourceElfAttribute & ~0x0F) != 0
                || modifier <= 0 || modifier > 127) {
            throw new IllegalArgumentException(
                    "invalid native Elemental Protection lease");
        }
    }

    private static boolean isSingleAttribute(int attribute) {
        return attribute == BattleSpellCatalog.ATTRIBUTE_EARTH
                || attribute == BattleSpellCatalog.ATTRIBUTE_FIRE
                || attribute == BattleSpellCatalog.ATTRIBUTE_WATER
                || attribute == BattleSpellCatalog.ATTRIBUTE_WIND;
    }
}
