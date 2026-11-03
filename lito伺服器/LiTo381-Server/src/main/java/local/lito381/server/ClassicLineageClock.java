package local.lito381.server;

import java.time.Instant;
import java.time.ZoneId;

/** Exact retained six-times Lineage clock consumed by native S_TIME/localtime. */
final class ClassicLineageClock {
    static final long REAL_BASE_EPOCH_MILLIS = 1_735_689_600_000L;
    static final int SPEED = 6;
    static final int ALIGNMENT_SECONDS = 3;
    static final ZoneId NATIVE_CLIENT_ZONE = ZoneId.of("Asia/Taipei");

    private ClassicLineageClock() {}

    static int now() {
        return atEpochMillis(System.currentTimeMillis());
    }

    static int atEpochMillis(long epochMillis) {
        long elapsedMillis = epochMillis - REAL_BASE_EPOCH_MILLIS;
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("time predates retained Lineage epoch");
        }
        long accelerated = Math.multiplyExact(elapsedMillis, SPEED) / 1_000L;
        accelerated -= Math.floorMod(accelerated, ALIGNMENT_SECONDS);
        if (accelerated > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("retained Lineage clock exceeds native time_t");
        }
        return (int) accelerated;
    }

    /**
     * LiTo.bin passes the S_TIME dword to 32-bit {@code localtime}; ship
     * animation and C_GOTO_MAP production therefore use this local clock view.
     */
    static int nativeClientSecondOfDay(int gameSeconds) {
        if (gameSeconds < 0) {
            throw new IllegalArgumentException("negative retained Lineage clock");
        }
        return Instant.ofEpochSecond(gameSeconds).atZone(NATIVE_CLIENT_ZONE)
                .toLocalTime().toSecondOfDay();
    }
}
