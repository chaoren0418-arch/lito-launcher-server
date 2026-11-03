package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClassicWorldCastleWarClockTest {
    private static final long START = 1_000_000L;

    @Test
    void preservesStrictSourceStartAndEndBoundaries() {
        ClassicWorldCastleWarClock clock = clock();

        assertEquals(List.of(), clock.tick(START));
        assertFalse(clock.active(5));

        List<ClassicWorldCastleWarClock.Event> began = clock.tick(START + 1);
        assertEquals(1, began.size());
        assertEquals(ClassicWorldCastleWarClock.EventKind.BEGIN, began.get(0).kind());
        assertEquals(5, began.get(0).castleId());
        assertTrue(clock.active(5));

        long end = START + 60L * 60_000L;
        assertEquals(List.of(), clock.tick(end));
        assertTrue(clock.active(5));

        List<ClassicWorldCastleWarClock.Event> ended = clock.tick(end + 1);
        assertEquals(1, ended.size());
        assertEquals(ClassicWorldCastleWarClock.EventKind.END, ended.get(0).kind());
        assertFalse(clock.active(5));
    }

    @Test
    void advancesExactlyOneIntervalOnlyAfterAnObservedActiveWarEnds() {
        ClassicWorldCastleWarClock clock = clock();
        long end = START + 60L * 60_000L;
        long interval = 10_080L * 60_000L;

        // The selected source does not catch up an expired inactive row.
        assertEquals(List.of(), clock.tick(end + interval * 3));
        assertEquals(START, clock.snapshot(5).startMillis());

        clock.register(List.of(schedule(START)));
        clock.tick(START + 1);
        clock.tick(end + 1);
        assertEquals(START + interval, clock.snapshot(5).startMillis());
        assertEquals(end + interval, clock.snapshot(5).endMillis());
    }

    @Test
    void identicalRegistrationDoesNotEraseLiveRuntimeProgress() {
        ClassicWorldCastleWarClock clock = clock();
        clock.tick(START + 1);
        assertTrue(clock.active(5));

        clock.register(List.of(schedule(START)));
        assertTrue(clock.active(5));
    }

    @Test
    void rejectsDuplicateSchedules() {
        ClassicWorldCastleWarClock clock = new ClassicWorldCastleWarClock();
        assertThrows(IllegalArgumentException.class,
                () -> clock.register(List.of(schedule(START), schedule(START))));
    }

    @Test
    void emitsExactNativeWarPacketBoxLayouts() {
        assertEquals("7B0005000000", hex(Protocol.castleWarEvent(
                Protocol.ServerEventType.CASTLE_WAR_BEGIN, 5)));
        assertEquals("7B0105000000", hex(Protocol.castleWarEvent(
                Protocol.ServerEventType.CASTLE_WAR_END, 5)));
        assertEquals("7B0205000000", hex(Protocol.castleWarEvent(
                Protocol.ServerEventType.CASTLE_WAR_GOING, 5)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarEvent(Protocol.ServerEventType.SMS_RESULT, 5));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.castleWarEvent(
                        Protocol.ServerEventType.CASTLE_WAR_BEGIN, 9));
        assertThrows(NullPointerException.class,
                () -> Protocol.castleWarEvent(null, 5));
    }

    private static ClassicWorldCastleWarClock clock() {
        ClassicWorldCastleWarClock clock = new ClassicWorldCastleWarClock();
        clock.register(List.of(schedule(START)));
        return clock;
    }

    private static CastleWarSchedule schedule(long start) {
        return new CastleWarSchedule(5, "海音城", start, 60, 10_080);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02X", value & 0xFF));
        return result.toString();
    }
}
