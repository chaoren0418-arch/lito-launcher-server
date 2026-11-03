package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-local mirror of the selected 3.81C {@code ServerWarExecutor}.
 *
 * <p>The source clock uses strict Calendar boundaries: a war begins only when
 * {@code start < now < end}; it ends only after {@code end < now}.  The next
 * start/end pair is advanced by one database {@code war_interval} only after
 * an observed active war ends.  In particular, this class deliberately does
 * not invent catch-up recurrence for an already-expired inactive DB row.</p>
 */
final class ClassicWorldCastleWarClock {
    enum EventKind { BEGIN, END }

    record Event(EventKind kind, int castleId, String castleName,
                 long startMillis, long endMillis) {
        Event {
            if (kind == null || castleId < 1 || castleId > 8
                    || castleName == null || castleName.isBlank()
                    || startMillis < 0 || endMillis <= startMillis) {
                throw new IllegalArgumentException("invalid castle-war clock event");
            }
        }
    }

    record Snapshot(int castleId, String castleName, long startMillis,
                    long endMillis, int intervalMinutes, boolean active) {}

    private final Map<Integer, MutableSchedule> schedules = new LinkedHashMap<>();

    synchronized void register(List<CastleWarSchedule> sourceSchedules) {
        if (sourceSchedules == null || sourceSchedules.isEmpty()) {
            throw new IllegalArgumentException("castle-war schedules are required");
        }
        Map<Integer, CastleWarSchedule> incoming = new LinkedHashMap<>();
        for (CastleWarSchedule schedule : sourceSchedules) {
            if (schedule == null || incoming.put(schedule.castleId(), schedule) != null) {
                throw new IllegalArgumentException("duplicate castle-war schedule");
            }
        }
        schedules.keySet().removeIf(id -> !incoming.containsKey(id));
        for (CastleWarSchedule schedule : incoming.values()) {
            MutableSchedule current = schedules.get(schedule.castleId());
            if (current == null || !current.source.equals(schedule)) {
                schedules.put(schedule.castleId(), new MutableSchedule(schedule));
            }
        }
    }

    synchronized List<Event> tick(long nowMillis) {
        if (nowMillis < 0) throw new IllegalArgumentException("negative castle-war clock time");
        List<Event> events = new ArrayList<>();
        for (MutableSchedule schedule : schedules.values()) {
            if (schedule.startMillis < nowMillis && schedule.endMillis > nowMillis) {
                if (!schedule.active) {
                    schedule.active = true;
                    events.add(schedule.event(EventKind.BEGIN));
                }
            } else if (schedule.endMillis < nowMillis && schedule.active) {
                Event ended = schedule.event(EventKind.END);
                schedule.active = false;
                long intervalMillis = Math.multiplyExact(
                        (long) schedule.source.warIntervalMinutes(), 60_000L);
                schedule.startMillis = Math.addExact(schedule.startMillis, intervalMillis);
                schedule.endMillis = Math.addExact(schedule.endMillis, intervalMillis);
                events.add(ended);
            }
        }
        return List.copyOf(events);
    }

    synchronized boolean active(int castleId) {
        MutableSchedule schedule = schedules.get(castleId);
        return schedule != null && schedule.active;
    }

    synchronized List<Integer> activeCastleIds() {
        return schedules.values().stream()
                .filter(schedule -> schedule.active)
                .map(schedule -> schedule.source.castleId())
                .toList();
    }

    synchronized Snapshot snapshot(int castleId) {
        MutableSchedule schedule = schedules.get(castleId);
        return schedule == null ? null : schedule.snapshot();
    }

    private static final class MutableSchedule {
        final CastleWarSchedule source;
        long startMillis;
        long endMillis;
        boolean active;

        MutableSchedule(CastleWarSchedule source) {
            this.source = source;
            startMillis = source.warTimeMillis();
            endMillis = Math.addExact(startMillis,
                    Math.multiplyExact((long) source.warDurationMinutes(), 60_000L));
        }

        Event event(EventKind kind) {
            return new Event(kind, source.castleId(), source.castleName(),
                    startMillis, endMillis);
        }

        Snapshot snapshot() {
            return new Snapshot(source.castleId(), source.castleName(),
                    startMillis, endMillis, source.warIntervalMinutes(), active);
        }
    }
}
