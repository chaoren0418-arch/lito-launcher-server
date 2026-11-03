// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record CastleWarSchedule(int castleId, String castleName, long warTimeMillis, int warDurationMinutes, int warIntervalMinutes) {
    CastleWarSchedule {
        if (castleId < 1 || castleId > 8 || castleName == null || castleName.isBlank() || warTimeMillis < 0L || warDurationMinutes <= 0 || warIntervalMinutes <= warDurationMinutes) {
            throw new IllegalArgumentException("invalid legacy castle-war schedule");
        }
        try {
            Math.addExact(warTimeMillis, Math.multiplyExact((long)warDurationMinutes, 60000L));
            Math.multiplyExact((long)warIntervalMinutes, 60000L);
        }
        catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("castle-war schedule exceeds millisecond range", overflow);
        }
    }
}
