package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-owned course progression for the four native LiTo pet-race maps.
 *
 * <p>The selected LTmap resources pair 565/569 and 566/570 as normal/reverse
 * versions.  Their Layer8 direction markers identify the bottom start
 * straight, while the native collision graph identifies the corridor gates.
 * Progress is deliberately sequential: crossing the finish straight without
 * traversing the course cannot manufacture a lap.</p>
 */
final class BattlePetRaceCourseCatalog {
    static final int LAPS = 4;
    static final int MAXIMUM_PLAYERS = 10;
    static final long FINISH_WINDOW_NANOS = 10_000_000_000L;

    record Zone(int minX, int maxX, int minY, int maxY) {
        Zone {
            if (minX > maxX || minY > maxY) {
                throw new IllegalArgumentException("invalid race zone");
            }
        }

        boolean contains(int x, int y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }

    enum PanelType { NONE, TRANSFORM, SPEED }

    record Panel(PanelType type, ArenaMapCatalog.Point point) {
        Panel {
            if (type == null || type == PanelType.NONE || point == null) {
                throw new IllegalArgumentException("invalid pet-race panel");
            }
        }
    }

    record PanelTransition(PanelType type, BattlePetRaceFormCatalog.Form form,
                           int durationSeconds, boolean trailing) {
        PanelTransition {
            if (type == null || durationSeconds < 0
                    || (type == PanelType.NONE && (form != null || durationSeconds != 0))
                    || (type == PanelType.TRANSFORM && form == null)
                    || (type == PanelType.SPEED && form != null)) {
                throw new IllegalArgumentException("invalid pet-race panel transition");
            }
        }

        static PanelTransition none() {
            return new PanelTransition(PanelType.NONE, null, 0, false);
        }

        boolean activated() { return type != PanelType.NONE; }
    }

    record Course(int mapId, int heading, List<ArenaMapCatalog.Point> spawns,
                  List<Zone> gates, List<Panel> panels) {
        Course {
            spawns = List.copyOf(spawns);
            gates = List.copyOf(gates);
            panels = List.copyOf(panels);
            if (heading < 0 || heading > 7 || spawns.size() != MAXIMUM_PLAYERS
                    || gates.size() < 2) {
                throw new IllegalArgumentException("invalid pet-race course map=" + mapId);
            }
            long distinctPanels = panels.stream().map(Panel::point).distinct().count();
            if (distinctPanels != panels.size()) {
                throw new IllegalArgumentException("duplicate pet-race panel map=" + mapId);
            }
        }

        ArenaMapCatalog.Point spawn(int index) {
            return spawns.get(Math.floorMod(index, spawns.size()));
        }
    }

    record Transition(boolean gateAdvanced, boolean lapCompleted,
                      int completedLaps, int currentLap, boolean finished,
                      int placement, long elapsedMilliseconds,
                      long finishDeadlineNanos) {
        static Transition ignored(int completedLaps) {
            return new Transition(false, false, completedLaps,
                    Math.min(LAPS, completedLaps + 1), false, 0, 0, 0);
        }
    }

    static final class State {
        private Course course;
        private long startedNanos;
        private long raceDeadlineNanos;
        private Long firstFinisher;
        private long finishDeadlineNanos;
        private int finishedCount;
        private BattlePetRaceFormCatalog.Form baseForm;
        private final Map<Long, Runner> runners = new LinkedHashMap<>();

        void begin(int mapId, List<Long> participants, long nowNanos,
                   long deadlineNanos) {
            begin(mapId, participants, nowNanos, deadlineNanos, 0);
        }

        void begin(int mapId, List<Long> participants, long nowNanos,
                   long deadlineNanos, long raceGeneration) {
            if (deadlineNanos < nowNanos) {
                throw new IllegalArgumentException("race deadline precedes start");
            }
            course = forMap(mapId);
            startedNanos = nowNanos;
            raceDeadlineNanos = deadlineNanos;
            firstFinisher = null;
            finishDeadlineNanos = 0;
            finishedCount = 0;
            baseForm = BattlePetRaceFormCatalog.baseForm(mapId, raceGeneration);
            runners.clear();
            for (long participant : participants) runners.put(participant, new Runner());
        }

        void reset() {
            course = null;
            startedNanos = 0;
            raceDeadlineNanos = 0;
            firstFinisher = null;
            finishDeadlineNanos = 0;
            finishedCount = 0;
            baseForm = null;
            runners.clear();
        }

        Transition moved(long player, int fromX, int fromY,
                         int toX, int toY, long nowNanos) {
            Runner runner = runners.get(player);
            if (course == null || runner == null || runner.finished) {
                return Transition.ignored(runner == null ? 0 : runner.completedLaps);
            }
            Zone next = course.gates().get(runner.nextGate);
            if (next.contains(fromX, fromY) || !next.contains(toX, toY)) {
                return Transition.ignored(runner.completedLaps);
            }
            runner.nextGate++;
            if (runner.nextGate < course.gates().size()) {
                return new Transition(true, false, runner.completedLaps,
                        runner.completedLaps + 1, false, 0, 0, finishDeadlineNanos);
            }

            runner.nextGate = 0;
            runner.completedLaps++;
            runner.finished = runner.completedLaps >= LAPS;
            long elapsed = Math.max(0, (nowNanos - startedNanos) / 1_000_000L);
            int placement = 0;
            if (runner.finished) {
                placement = ++finishedCount;
                runner.placement = placement;
                if (firstFinisher == null) {
                    firstFinisher = player;
                    long requestedDeadline = nowNanos > Long.MAX_VALUE - FINISH_WINDOW_NANOS
                            ? Long.MAX_VALUE : nowNanos + FINISH_WINDOW_NANOS;
                    finishDeadlineNanos = Math.min(raceDeadlineNanos, requestedDeadline);
                }
            }
            return new Transition(true, true, runner.completedLaps,
                    Math.min(LAPS, runner.completedLaps + 1), runner.finished,
                    placement, elapsed, finishDeadlineNanos);
        }

        int currentLap(long player) {
            Runner runner = runners.get(player);
            return runner == null ? 0 : Math.min(LAPS, runner.completedLaps + 1);
        }

        BattlePetRaceFormCatalog.Form baseForm() {
            if (baseForm == null) throw new IllegalStateException("pet race has not begun");
            return baseForm;
        }

        PanelTransition panelMoved(long player, int fromX, int fromY,
                                   int toX, int toY, long nowNanos) {
            Runner runner = runners.get(player);
            if (course == null || runner == null || runner.finished) {
                return PanelTransition.none();
            }
            Panel panel = course.panels().stream()
                    .filter(candidate -> candidate.point().x() == toX
                            && candidate.point().y() == toY)
                    .filter(candidate -> candidate.point().x() != fromX
                            || candidate.point().y() != fromY)
                    .findFirst().orElse(null);
            if (panel == null) return PanelTransition.none();

            if (panel.type() == PanelType.TRANSFORM) {
                if (nowNanos < runner.transformPanelReadyNanos) {
                    return PanelTransition.none();
                }
                runner.transformPanelReadyNanos = saturatingAdd(
                        nowNanos, BattlePetRaceFormCatalog.PANEL_COOLDOWN_NANOS);
                runner.transformActivations++;
                boolean trailing = progress(runner) < leadingProgress();
                BattlePetRaceFormCatalog.Form form =
                        BattlePetRaceFormCatalog.temporaryForm(
                                player, runner.transformActivations, trailing);
                return new PanelTransition(PanelType.TRANSFORM, form,
                        BattlePetRaceFormCatalog.TRANSFORM_DURATION_SECONDS, trailing);
            }
            if (nowNanos < runner.speedPanelReadyNanos) {
                return PanelTransition.none();
            }
            runner.speedPanelReadyNanos = saturatingAdd(
                    nowNanos, BattlePetRaceFormCatalog.PANEL_COOLDOWN_NANOS);
            return new PanelTransition(PanelType.SPEED, null,
                    BattlePetRaceFormCatalog.SPEED_DURATION_SECONDS,
                    progress(runner) < leadingProgress());
        }

        Long firstFinisher() {
            return firstFinisher;
        }

        int finishedCount() {
            return finishedCount;
        }

        /**
         * Native reward boundary recovered from the contemporary official
         * race rules: 2-4 entrants reward first place, 5-7 reward the first
         * two, and 8-10 reward the first three.  Only runners that actually
         * completed four laps inside the shared finish window are returned.
         */
        Set<Long> rewardWinners() {
            // Production PET_RACE enforces the native minimum of two racers.
            // Keep the isolated one-player protocol fixture usable without
            // weakening that admission rule or changing the public boundary.
            int placements = runners.size() == 1
                    ? 1 : rewardPlacementLimit(runners.size());
            LinkedHashSet<Long> winners = new LinkedHashSet<>();
            for (Map.Entry<Long, Runner> entry : runners.entrySet()) {
                Runner runner = entry.getValue();
                if (runner.finished && runner.placement > 0
                        && runner.placement <= placements) {
                    winners.add(entry.getKey());
                }
            }
            return Set.copyOf(winners);
        }

        long finishDeadlineNanos() {
            return finishDeadlineNanos;
        }

        boolean finishWindowExpired(long nowNanos) {
            return firstFinisher != null && nowNanos >= finishDeadlineNanos;
        }

        private int leadingProgress() {
            int leading = 0;
            for (Runner runner : runners.values()) leading = Math.max(leading, progress(runner));
            return leading;
        }

        private int progress(Runner runner) {
            return runner.completedLaps * course.gates().size() + runner.nextGate;
        }
    }

    private static final class Runner {
        int nextGate;
        int completedLaps;
        int placement;
        boolean finished;
        long transformPanelReadyNanos;
        long speedPanelReadyNanos;
        long transformActivations;
    }

    private static final Map<Integer, Course> COURSES = Map.of(
            565, course(565, 2,
                    grid(32765, 32767, 32846, 32850),
                    List.of(),
                    new Zone(32793, 32810, 32840, 32858),
                    new Zone(32793, 32810, 32808, 32821),
                    new Zone(32734, 32749, 32808, 32821),
                    new Zone(32732, 32743, 32840, 32858),
                    new Zone(32774, 32782, 32845, 32853)),
            569, course(569, 6,
                    grid(32767, 32769, 32846, 32850),
                    List.of(),
                    new Zone(32732, 32743, 32840, 32858),
                    new Zone(32734, 32749, 32808, 32821),
                    new Zone(32793, 32810, 32808, 32821),
                    new Zone(32793, 32810, 32840, 32858),
                    new Zone(32756, 32764, 32845, 32853)),
            566, course(566, 2,
                    grid(32698, 32700, 32866, 32870),
                    panels(
                            transform(32661, 32807), transform(32680, 32849),
                            transform(32683, 32849), transform(32704, 32807),
                            transform(32716, 32831), transform(32723, 32864),
                            transform(32743, 32836),
                            speed(32671, 32806), speed(32723, 32812),
                            speed(32680, 32866), speed(32699, 32819),
                            speed(32682, 32865), speed(32682, 32868),
                            speed(32682, 32871), speed(32681, 32869),
                            speed(32736, 32838), speed(32748, 32787)),
                    new Zone(32735, 32752, 32812, 32825),
                    new Zone(32740, 32755, 32782, 32794),
                    new Zone(32658, 32670, 32786, 32800),
                    new Zone(32670, 32682, 32866, 32877),
                    new Zone(32707, 32717, 32865, 32874)),
            570, course(570, 6,
                    grid(32696, 32698, 32866, 32870),
                    panels(
                            transform(32732, 32849),
                            speed(32743, 32837), speed(32710, 32865),
                            speed(32710, 32868), speed(32710, 32871),
                            speed(32699, 32819), speed(32744, 32786),
                            speed(32662, 32806), speed(32676, 32861)),
                    new Zone(32670, 32682, 32866, 32877),
                    new Zone(32658, 32670, 32786, 32800),
                    new Zone(32740, 32755, 32782, 32794),
                    new Zone(32735, 32752, 32812, 32825),
                    new Zone(32684, 32692, 32865, 32874))
    );

    private BattlePetRaceCourseCatalog() {}

    static Course forMap(int mapId) {
        Course course = COURSES.get(mapId);
        if (course == null) {
            throw new IllegalArgumentException("unsupported native pet-race map: " + mapId);
        }
        return course;
    }

    static boolean supports(int mapId) {
        return COURSES.containsKey(mapId);
    }

    static int rewardPlacementLimit(int entrants) {
        if (entrants < 2 || entrants > MAXIMUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "native pet-race entrants must be between 2 and "
                            + MAXIMUM_PLAYERS + ": " + entrants);
        }
        if (entrants >= 8) return 3;
        if (entrants >= 5) return 2;
        return 1;
    }

    private static Course course(int mapId, int heading,
                                 List<ArenaMapCatalog.Point> spawns,
                                 List<Panel> panels,
                                 Zone... gates) {
        return new Course(mapId, heading, spawns, List.of(gates), panels);
    }

    private static List<Panel> panels(Panel... panels) { return List.of(panels); }
    private static Panel transform(int x, int y) {
        return new Panel(PanelType.TRANSFORM, new ArenaMapCatalog.Point(x, y));
    }
    private static Panel speed(int x, int y) {
        return new Panel(PanelType.SPEED, new ArenaMapCatalog.Point(x, y));
    }

    private static long saturatingAdd(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    private static List<ArenaMapCatalog.Point> grid(int firstX, int secondX,
                                                    int minY, int maxY) {
        java.util.ArrayList<ArenaMapCatalog.Point> points = new java.util.ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            points.add(new ArenaMapCatalog.Point(firstX, y));
            points.add(new ArenaMapCatalog.Point(secondX, y));
        }
        return List.copyOf(points);
    }
}
