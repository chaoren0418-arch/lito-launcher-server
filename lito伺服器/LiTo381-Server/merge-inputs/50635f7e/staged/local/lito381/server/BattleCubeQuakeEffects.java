package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authoritative NPC 80150 / GFX 6712 lifecycle for skill210 Cube: Quake.
 *
 * <p>The retained executor gives targets inside radius three one non-stacking
 * eight-second lease. Allies receive earth resistance +30. Enemy leases own a
 * 900/1000ms clock and attempt a one-second movement-only bind on every fourth
 * callback (about 3.9 and 7.9 seconds). A bind survives both the enemy lease
 * and cube object when their clocks overlap, matching the retained
 * {@code L1Cube}/{@code MOVE_STOP} lifecycle.</p>
 */
final class BattleCubeQuakeEffects {
    static final int SKILL_ID = 210;
    static final int NPC_ID = 80150;
    static final int CUBE_GFX = 6712;
    static final int ALLY_GFX = 6714;
    static final int ENEMY_GFX = 6715;
    static final int DURATION_SECONDS = 20;
    static final int EFFECT_RADIUS = 3;
    static final int STATUS_SECONDS = 8;
    static final int ALLY_EARTH_RESISTANCE = 30;
    static final int SCAN_INTERVAL_MILLIS = 1_000;
    static final int ENEMY_INITIAL_DELAY_MILLIS = 900;
    static final int ENEMY_TICK_MILLIS = 1_000;
    static final int ENEMY_BIND_EVERY_TICKS = 4;
    static final int ENEMY_BIND_MILLIS = 1_000;

    private static final long FIRST_OBJECT_ID = 0x7300_0000L;
    private static final long LAST_OBJECT_ID = 0x73FF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    enum ActorKind { PLAYER, BOT, MONSTER }
    enum StatusKind { ALLY, ENEMY }
    enum Relation { ALLY, ENEMY, IGNORE }
    enum EventKind {
        CUBE_EXPIRED,
        ALLY_APPLIED,
        ENEMY_APPLIED,
        ALLY_EXPIRED,
        ENEMY_EXPIRED,
        ENEMY_BIND_STARTED,
        ENEMY_BIND_EXPIRED
    }

    @FunctionalInterface
    interface Alliance {
        Relation relation(long casterObjectId, long targetObjectId);
    }

    /** Resolves the retained freeze/barrier/earth-bind suppression at callback time. */
    @FunctionalInterface
    interface BindProtection {
        boolean protectedTarget(long targetObjectId, ActorKind targetKind,
                                long nowNanos);
    }

    record Actor(long objectId, int x, int y, boolean alive, ActorKind kind) {
        Actor {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || kind == null) {
                throw new IllegalArgumentException("invalid Cube: Quake actor");
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
                throw new IllegalArgumentException("invalid Cube: Quake object");
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
                throw new IllegalArgumentException("invalid Cube: Quake event");
            }
        }

        static Event cubeExpired(Cube cube) {
            return new Event(EventKind.CUBE_EXPIRED, cube, 0, null);
        }
    }

    private final List<CubeLease> cubes = new ArrayList<>();
    private final Map<StatusKey, StatusLease> statuses = new LinkedHashMap<>();
    private final Map<Long, BindLease> binds = new LinkedHashMap<>();


    volatile Object nativeSourceCubeEpoch;
    volatile Object nativeSourceCubeScene;
    volatile Object nativeSourceCubeInstalling;
    volatile Object nativeSourceCubeScan;
    volatile Object nativeSourceCubeIndex;

    BattleCubeQuakeEffects() {
        NativeSourceCubeStatus.reset(this);
    }
    synchronized Placement place(long casterObjectId, int x, int y, long nowNanos) {
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
        if (!NativeCubeCastAction.admit(clientSession, 210)) {
            cubes.removeIf(lease -> lease.cube == placed.cube());
            return Placement.rejected();
        }
        return placed;
    }
    synchronized List<Event> advance(long nowNanos, List<Actor> actors,
                                     Alliance alliance,
                                     BindProtection protection) {
        NativeSourceCubeStatus.retire(this);
        if (nowNanos < 0 || alliance == null || protection == null) {
            throw new IllegalArgumentException("invalid Cube: Quake clock");
        }
        List<Event> events = new ArrayList<>();
        Map<Long, Actor> liveActors = new LinkedHashMap<>();
        if (actors != null) {
            for (Actor actor : actors) {
                if (actor != null && actor.alive()) liveActors.put(actor.objectId(), actor);
            }
        }

        for (Iterator<CubeLease> iterator = cubes.iterator(); iterator.hasNext();) {
            CubeLease lease = iterator.next();
            if (nowNanos < lease.cube.expiresAtNanos()) continue;
            iterator.remove();
            events.add(Event.cubeExpired(lease.cube));
        }

        for (Iterator<Map.Entry<Long, BindLease>> iterator =
                binds.entrySet().iterator(); iterator.hasNext();) {
            BindLease bind = iterator.next().getValue();
            if (nowNanos < bind.expiresAtNanos) continue;
            iterator.remove();
            events.add(new Event(EventKind.ENEMY_BIND_EXPIRED, bind.cube,
                    bind.targetObjectId, bind.targetKind));
        }

        for (Iterator<Map.Entry<StatusKey, StatusLease>> iterator =
                statuses.entrySet().iterator(); iterator.hasNext();) {
            StatusLease status = iterator.next().getValue();
            if (status.kind == StatusKind.ENEMY) {
                while (status.nextBindAtNanos < status.expiresAtNanos
                        && nowNanos >= status.nextBindAtNanos) {
                    status.tick++;
                    if (status.tick % ENEMY_BIND_EVERY_TICKS == 0
                            && !protection.protectedTarget(
                                status.targetObjectId, status.targetKind,
                                status.nextBindAtNanos)) {
                        long expiresAt = Math.addExact(nowNanos,
                                TimeUnit.MILLISECONDS.toNanos(ENEMY_BIND_MILLIS));
                        BindLease existing = binds.get(status.targetObjectId);
                        if (existing == null || nowNanos >= existing.expiresAtNanos) {
                            binds.put(status.targetObjectId, new BindLease(
                                    status.cube, status.targetObjectId,
                                    status.targetKind, expiresAt));
                            events.add(new Event(EventKind.ENEMY_BIND_STARTED,
                                    status.cube, status.targetObjectId,
                                    status.targetKind));
                        } else {
                            existing.expiresAtNanos = Math.max(
                                    existing.expiresAtNanos, expiresAt);
                        }
                    }
                    status.nextBindAtNanos = Math.addExact(
                            status.nextBindAtNanos,
                            TimeUnit.MILLISECONDS.toNanos(ENEMY_TICK_MILLIS));
                }
            }
            if (nowNanos < status.expiresAtNanos) continue;
            iterator.remove();
            events.add(new Event(status.kind == StatusKind.ALLY
                    ? EventKind.ALLY_EXPIRED : EventKind.ENEMY_EXPIRED,
                    status.cube, status.targetObjectId, status.targetKind));
        }

        long scanInterval = TimeUnit.MILLISECONDS.toNanos(SCAN_INTERVAL_MILLIS);
        for (CubeLease lease : cubes) {
            if (nowNanos < lease.nextScanAtNanos) continue;
            do {
                lease.nextScanAtNanos = Math.addExact(
                        lease.nextScanAtNanos, scanInterval);
            } while (lease.nextScanAtNanos <= nowNanos);
            for (Actor actor : liveActors.values()) {
                if (distance(actor.x(), actor.y(), lease.cube.x(), lease.cube.y())
                        > EFFECT_RADIUS) continue;
                Relation relation = actor.objectId() == lease.cube.casterObjectId()
                        ? Relation.ALLY : alliance.relation(
                                lease.cube.casterObjectId(), actor.objectId());
                if (relation == null || relation == Relation.IGNORE) continue;
                boolean ally = relation == Relation.ALLY;
                StatusKind kind = ally ? StatusKind.ALLY : StatusKind.ENEMY;
                StatusKey key = new StatusKey(actor.objectId(), kind);
                StatusLease existing = statuses.get(key);
                if (existing != null && nowNanos < existing.expiresAtNanos) continue;
                long expiresAt = Math.addExact(nowNanos,
                        TimeUnit.SECONDS.toNanos(STATUS_SECONDS));
                long firstEnemyTick = ally ? Long.MAX_VALUE
                        : Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(
                                ENEMY_INITIAL_DELAY_MILLIS));
                statuses.put(key, new StatusLease(
                        lease.cube, actor.objectId(), actor.kind(), kind,
                        expiresAt, firstEnemyTick));
                events.add(new Event(ally ? EventKind.ALLY_APPLIED
                        : EventKind.ENEMY_APPLIED, lease.cube,
                        actor.objectId(), actor.kind()));
            }
        }
        return List.copyOf(events);
    }

    synchronized int earthResistance(long targetObjectId, long nowNanos) {
        if (targetObjectId <= 0 || nowNanos < 0) return 0;
        StatusLease status = statuses.get(new StatusKey(
                targetObjectId, StatusKind.ALLY));
        return status != null && nowNanos < status.expiresAtNanos
                ? ALLY_EARTH_RESISTANCE : 0;
    }

    synchronized boolean movementStopped(long targetObjectId, long nowNanos) {
        if (targetObjectId <= 0 || nowNanos < 0) return false;
        BindLease bind = binds.get(targetObjectId);
        return bind != null && nowNanos < bind.expiresAtNanos;
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

    synchronized int bindCount(long nowNanos) {
        if (nowNanos < 0) return 0;
        return (int) binds.values().stream()
                .filter(bind -> nowNanos < bind.expiresAtNanos).count();
    }

    synchronized void clear() {
        cubes.clear();
        statuses.clear();
        binds.clear();
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

    private record StatusKey(long targetObjectId, StatusKind kind) {}

    private static final class StatusLease {
        final Cube cube;
        final long targetObjectId;
        final ActorKind targetKind;
        final StatusKind kind;
        final long expiresAtNanos;
        long nextBindAtNanos;
        int tick;
        final Object nativeSourceCubeOrigin;

        StatusLease(Cube cube, long targetObjectId, ActorKind targetKind,
                    StatusKind kind, long expiresAtNanos,
                    long nextBindAtNanos) {
            this.cube = cube;
            this.targetObjectId = targetObjectId;
            this.targetKind = targetKind;
            this.kind = kind;
            this.expiresAtNanos = expiresAtNanos;
            this.nextBindAtNanos = nextBindAtNanos;
            this.nativeSourceCubeOrigin = null;
        }
    }

    private static final class BindLease {
        final Cube cube;
        final long targetObjectId;
        final ActorKind targetKind;
        long expiresAtNanos;
        volatile Object nativeSourceCubeOrigin;

        BindLease(Cube cube, long targetObjectId, ActorKind targetKind,
                  long expiresAtNanos) {
            this.cube = cube;
            this.targetObjectId = targetObjectId;
            this.targetKind = targetKind;
            this.expiresAtNanos = expiresAtNanos;
        }
    }
}
