package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authoritative NPC 80152 / GFX 6724 lifecycle for skill220 Cube: Harmony.
 *
 * <p>The retained server scans every live cube once per second. A living PC or
 * monster inside radius three receives the same non-stacking status1025 and
 * GFX6727 for eight seconds, irrespective of ally/enemy relation. Each status
 * owns a 900/1000ms clock: callbacks four and eight restore five MP, while
 * callback five deals 25 HP damage. Leaving the cube does not cancel an
 * already installed status lease.</p>
 */
final class BattleCubeHarmonyEffects {
    static final int SKILL_ID = 220;
    static final int NPC_ID = 80152;
    static final int CUBE_GFX = 6724;
    static final int EFFECT_GFX = 6727;
    static final int DURATION_SECONDS = 20;
    static final int EFFECT_RADIUS = 3;
    static final int STATUS_SECONDS = 8;
    static final int MP_RECOVERY = 5;
    static final int HP_DAMAGE = 25;
    static final int SCAN_INTERVAL_MILLIS = 1_000;
    static final int INITIAL_DELAY_MILLIS = 900;
    static final int TICK_MILLIS = 1_000;
    static final int MP_EVERY_TICKS = 4;
    static final int HP_EVERY_TICKS = 5;

    private static final long FIRST_OBJECT_ID = 0x7500_0000L;
    private static final long LAST_OBJECT_ID = 0x75FF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID =
            new AtomicLong(FIRST_OBJECT_ID);

    enum ActorKind { PLAYER, BOT, MONSTER }
    enum Relation { ALLY, ENEMY, IGNORE }
    enum EventKind {
        CUBE_EXPIRED,
        STATUS_APPLIED,
        STATUS_EXPIRED,
        MP_RECOVERY,
        HP_DAMAGE
    }

    @FunctionalInterface
    interface Alliance {
        Relation relation(long casterObjectId, long targetObjectId);
    }

    record Actor(long objectId, int x, int y, boolean alive, ActorKind kind) {
        Actor {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || kind == null) {
                throw new IllegalArgumentException("invalid Cube: Harmony actor");
            }
        }
    }

    record Cube(long objectId, long casterObjectId, int x, int y,
                long expiresAtNanos) {
        Cube {
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || casterObjectId <= 0 || casterObjectId > 0xFFFF_FFFFL
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || expiresAtNanos <= 0) {
                throw new IllegalArgumentException("invalid Cube: Harmony object");
            }
        }
    }

    record Placement(boolean accepted, Cube cube) {
        static Placement rejected() { return new Placement(false, null); }
        static Placement accepted(Cube cube) { return new Placement(true, cube); }
    }

    record Event(EventKind kind, Cube cube, long targetObjectId,
                 ActorKind targetKind) {
        Event {
            if (kind == null || cube == null
                    || (kind == EventKind.CUBE_EXPIRED
                        ? targetObjectId != 0 || targetKind != null
                        : targetObjectId <= 0 || targetKind == null)) {
                throw new IllegalArgumentException("invalid Cube: Harmony event");
            }
        }

        static Event cubeExpired(Cube cube) {
            return new Event(EventKind.CUBE_EXPIRED, cube, 0, null);
        }
    }

    private final List<CubeLease> cubes = new ArrayList<>();
    /** Retained STATUS_CUBE_BALANCE is shared by ally/enemy and all cubes. */
    private final Map<Long, StatusLease> statuses = new LinkedHashMap<>();


    volatile Object nativeSourceCubeEpoch;
    volatile Object nativeSourceCubeScene;
    volatile Object nativeSourceCubeInstalling;
    volatile Object nativeSourceCubeScan;
    volatile Object nativeSourceCubeIndex;

    BattleCubeHarmonyEffects() {
        NativeSourceCubeRemainder.reset(this);
    }
    synchronized Placement place(long casterObjectId, int x, int y,
                                 long nowNanos) {
        if (casterObjectId <= 0 || casterObjectId > 0xFFFF_FFFFL
                || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                || nowNanos < 0) return Placement.rejected();
        for (CubeLease existing : cubes) {
            if (nowNanos >= existing.cube.expiresAtNanos()) continue;
            if (distance(x, y, existing.cube.x(), existing.cube.y())
                    <= EFFECT_RADIUS) return Placement.rejected();
        }
        Cube cube = new Cube(nextObjectId(), casterObjectId, x, y,
                Math.addExact(nowNanos,
                        TimeUnit.SECONDS.toNanos(DURATION_SECONDS)));
        cubes.add(new CubeLease(cube, nowNanos));
        return Placement.accepted(cube);
    }


    synchronized Placement nativeMaterialPlace(long casterObjectId, int x, int y,
            long nowNanos, ClientSession clientSession) {
        Placement placed = place(casterObjectId, x, y, nowNanos);
        if (!placed.accepted()) {
            return placed;
        }
        if (!NativeCubeCastAction.admit(clientSession, 220)) {
            cubes.removeIf(lease -> lease.cube == placed.cube());
            return Placement.rejected();
        }
        return placed;
    }
    synchronized List<Event> advance(long nowNanos, List<Actor> actors,
                                     Alliance alliance) {
        NativeSourceCubePlayer.retire(this);
        if (nowNanos < 0 || alliance == null) {
            throw new IllegalArgumentException("invalid Cube: Harmony clock");
        }
        List<Event> events = new ArrayList<>();
        Map<Long, Actor> liveActors = new LinkedHashMap<>();
        if (actors != null) {
            for (Actor actor : actors) {
                if (actor != null && actor.alive()) {
                    liveActors.put(actor.objectId(), actor);
                }
            }
        }

        for (Iterator<CubeLease> iterator = cubes.iterator();
             iterator.hasNext();) {
            CubeLease lease = iterator.next();
            if (nowNanos < lease.cube.expiresAtNanos()) continue;
            iterator.remove();
            events.add(Event.cubeExpired(lease.cube));
        }

        for (Iterator<Map.Entry<Long, StatusLease>> iterator =
                statuses.entrySet().iterator(); iterator.hasNext();) {
            StatusLease status = iterator.next().getValue();
            while (status.nextTickAtNanos < status.expiresAtNanos
                    && nowNanos >= status.nextTickAtNanos) {
                status.tick++;
                if (status.tick % MP_EVERY_TICKS == 0) {
                    events.add(new Event(EventKind.MP_RECOVERY, status.cube,
                            status.targetObjectId, status.targetKind));
                }
                if (status.tick % HP_EVERY_TICKS == 0) {
                    events.add(new Event(EventKind.HP_DAMAGE, status.cube,
                            status.targetObjectId, status.targetKind));
                }
                status.nextTickAtNanos = Math.addExact(
                        status.nextTickAtNanos,
                        TimeUnit.MILLISECONDS.toNanos(TICK_MILLIS));
            }
            if (nowNanos < status.expiresAtNanos) continue;
            iterator.remove();
            events.add(new Event(EventKind.STATUS_EXPIRED, status.cube,
                    status.targetObjectId, status.targetKind));
        }

        long scanInterval = TimeUnit.MILLISECONDS.toNanos(
                SCAN_INTERVAL_MILLIS);
        for (CubeLease lease : cubes) {
            if (nowNanos < lease.nextScanAtNanos) continue;
            do {
                lease.nextScanAtNanos = Math.addExact(
                        lease.nextScanAtNanos, scanInterval);
            } while (lease.nextScanAtNanos <= nowNanos);
            for (Actor actor : liveActors.values()) {
                if (distance(actor.x(), actor.y(), lease.cube.x(),
                        lease.cube.y()) > EFFECT_RADIUS) continue;
                Relation relation = actor.objectId()
                        == lease.cube.casterObjectId()
                        ? Relation.ALLY : alliance.relation(
                                lease.cube.casterObjectId(), actor.objectId());
                if (relation == null || relation == Relation.IGNORE) continue;
                StatusLease existing = statuses.get(actor.objectId());
                if (existing != null && nowNanos < existing.expiresAtNanos) {
                    continue;
                }
                long expiresAt = Math.addExact(nowNanos,
                        TimeUnit.SECONDS.toNanos(STATUS_SECONDS));
                long firstTick = Math.addExact(nowNanos,
                        TimeUnit.MILLISECONDS.toNanos(INITIAL_DELAY_MILLIS));
                statuses.put(actor.objectId(), new StatusLease(
                        lease.cube, actor.objectId(), actor.kind(),
                        expiresAt, firstTick));
                events.add(new Event(EventKind.STATUS_APPLIED, lease.cube,
                        actor.objectId(), actor.kind()));
            }
        }
        return List.copyOf(events);
    }

    synchronized List<Cube> active(long nowNanos) {
        if (nowNanos < 0) return List.of();
        return cubes.stream().map(lease -> lease.cube)
                .filter(cube -> nowNanos < cube.expiresAtNanos()).toList();
    }

    synchronized int statusCount(long nowNanos) {
        if (nowNanos < 0) return 0;
        return (int) statuses.values().stream()
                .filter(status -> nowNanos < status.expiresAtNanos).count();
    }

    synchronized boolean hasStatus(long targetObjectId, long nowNanos) {
        if (targetObjectId <= 0 || nowNanos < 0) return false;
        StatusLease status = statuses.get(targetObjectId);
        return status != null && nowNanos < status.expiresAtNanos;
    }

    synchronized void clear() {
        cubes.clear();
        statuses.clear();
    }

    private static int distance(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }

    private static final class CubeLease {
        final Cube cube;
        long nextScanAtNanos;

        CubeLease(Cube cube, long nextScanAtNanos) {
            this.cube = cube;
            this.nextScanAtNanos = nextScanAtNanos;
        }
    }

    private static final class StatusLease {
        final Cube cube;
        final long targetObjectId;
        final ActorKind targetKind;
        final long expiresAtNanos;
        long nextTickAtNanos;
        int tick;
        final Object nativeSourceCubeOrigin;
        volatile Object nativeSourceCubePresentationRelation;

        StatusLease(Cube cube, long targetObjectId, ActorKind targetKind,
                    long expiresAtNanos, long nextTickAtNanos) {
            this.cube = cube;
            this.targetObjectId = targetObjectId;
            this.targetKind = targetKind;
            this.expiresAtNanos = expiresAtNanos;
            this.nextTickAtNanos = nextTickAtNanos;
            this.nativeSourceCubeOrigin = null;
        }
    }
}
