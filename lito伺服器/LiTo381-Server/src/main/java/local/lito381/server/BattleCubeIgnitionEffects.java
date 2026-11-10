// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authoritative NPC 80149 / GFX 6706 lifecycle for skill205 Cube: Ignition.
 *
 * <p>The retained server scans every live cube once per second.  A character
 * inside three tiles receives one non-stacking eight-second ally or enemy
 * lease.  Enemy leases run their own 900/1000ms clock and deal ten damage on
 * every fourth callback, at approximately 3.9 and 7.9 seconds.  Keeping those
 * clocks separate is important: leaving the cube does not cancel an already
 * applied eight-second lease.</p>
 */
final class BattleCubeIgnitionEffects {
    static final int SKILL_ID = 205;
    static final int NPC_ID = 80149;
    static final int CUBE_GFX = 6706;
    static final int ALLY_GFX = 6708;
    static final int ENEMY_GFX = 6709;
    static final int DURATION_SECONDS = 20;
    static final int EFFECT_RADIUS = 3;
    static final int STATUS_SECONDS = 8;
    static final int ALLY_FIRE_RESISTANCE = 30;
    static final int ENEMY_DAMAGE = 10;
    static final int SCAN_INTERVAL_MILLIS = 1_000;
    static final int ENEMY_INITIAL_DELAY_MILLIS = 900;
    static final int ENEMY_TICK_MILLIS = 1_000;
    static final int ENEMY_DAMAGE_EVERY_TICKS = 4;

    private static final long FIRST_OBJECT_ID = 0x7200_0000L;
    private static final long LAST_OBJECT_ID = 0x72FF_FFFFL;
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
        ENEMY_DAMAGE
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
                throw new IllegalArgumentException("invalid Cube: Ignition actor");
            }
        }
    }

    @AsRecord(components = {"objectId", "casterObjectId", "x", "y", "expiresAtNanos"}, finalFields = {"nativeSourceCubeEntity", "nativeSourceCubeFaction", "nativeSourceCubeVisibility", "nativeSourceCubePresentation"})
    static final class Cube {
        private final long objectId;
        private final long casterObjectId;
        private final int x;
        private final int y;
        private final long expiresAtNanos;
        Object nativeSourceCubeEntity;
        Object nativeSourceCubeFaction;
        Object nativeSourceCubeVisibility;
        Object nativeSourceCubePresentation;

        Cube(long l, long l2, int n, int n2, long l3) {
            if (l < 0x72000000L || l > 0x72FFFFFFL || l2 <= 0L || l2 > 0xFFFFFFFFL || n < 0 || n > 65535 || n2 < 0 || n2 > 65535 || l3 <= 0L) {
                throw new IllegalArgumentException("invalid Cube: Ignition object");
            }
            this.objectId = l;
            this.casterObjectId = l2;
            this.x = n;
            this.y = n2;
            this.expiresAtNanos = l3;
        }




        public long objectId() {
            return this.objectId;
        }

        public long casterObjectId() {
            return this.casterObjectId;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public long expiresAtNanos() {
            return this.expiresAtNanos;
        }

        Cube(long l, long l2, int n, int n2, long l3, Object object) {
            this(l, l2, n, n2, l3);
            this.nativeSourceCubeEntity = NativeSourceCubeOrigin.entity(this, object);
            this.nativeSourceCubeFaction = NativeSourceCubeNpc.captureFaction(this.nativeSourceCubeEntity);
            this.nativeSourceCubeVisibility = NativeSourceCubeVisibility.capture(this);
            this.nativeSourceCubePresentation = NativeSourceCubeTemplate.capture(this);
        }
    }

    record Placement(boolean accepted, Cube cube) {
        static Placement rejected() { return new Placement(false, null); }
        static Placement accepted(Cube cube) { return new Placement(true, cube); }
    }

    @AsRecord(components = {"kind", "cube", "targetObjectId", "targetKind"}, finalFields = {"nativeSourceCubeOrigin", "nativeSourceCubeRemainderProof"})
    static final class Event {
        private final EventKind kind;
        private final Cube cube;
        private final long targetObjectId;
        private final ActorKind targetKind;
        Object nativeSourceCubeOrigin;
        Object nativeSourceCubeRemainderProof;

        Event(EventKind eventKind, Cube cube, long l, ActorKind actorKind) {
            if (eventKind == null || cube == null || (eventKind == EventKind.CUBE_EXPIRED ? l != 0L || actorKind != null : l <= 0L || actorKind == null)) {
                throw new IllegalArgumentException("invalid Cube: Ignition event");
            }
            this.kind = eventKind;
            this.cube = cube;
            this.targetObjectId = l;
            this.targetKind = actorKind;
            this.nativeSourceCubeRemainderProof = NativeSourceCubeRemainder.baseEvent(this);
        }

        static Event cubeExpired(Cube cube) {
            return new Event(EventKind.CUBE_EXPIRED, cube, 0, null);
        }




        public EventKind kind() {
            return this.kind;
        }

        public Cube cube() {
            return this.cube;
        }

        public long targetObjectId() {
            return this.targetObjectId;
        }

        public ActorKind targetKind() {
            return this.targetKind;
        }

        Event(EventKind eventKind, Cube cube, long l, ActorKind actorKind, Object object, Object object2) {
            this(eventKind, cube, l, actorKind);
            this.nativeSourceCubeOrigin = NativeSourceCubeOrigin.event(this, object, object2);
            this.nativeSourceCubeRemainderProof = NativeSourceCubeRemainder.capture(this);
        }
    }

    private final List<CubeLease> cubes = new ArrayList<>();
    private final Map<StatusKey, StatusLease> statuses = new LinkedHashMap<>();


    volatile Object nativeSourceCubeEpoch;
    volatile Object nativeSourceCubeScene;
    volatile Object nativeSourceCubeInstalling;
    volatile Object nativeSourceCubeScan;
    volatile Object nativeSourceCubeIndex;

    BattleCubeIgnitionEffects() {
        NativeSourceCubeRemainder.reset(this);
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
                        TimeUnit.SECONDS.toNanos(DURATION_SECONDS)), this);
        // The retained global executor starts at delay zero.  The first server
        // clock pass therefore applies an in-range status immediately.
        cubes.add(new CubeLease(cube, nowNanos));
        return Placement.accepted(cube);
    }


    synchronized Placement nativeMaterialPlace(long l, int n, int n2, long l2, ClientSession clientSession) {
        if (l <= 0L || l > 0xFFFFFFFFL || n < 0 || n > 65535 || n2 < 0 || n2 > 65535 || l2 < 0L) {
            return Placement.rejected();
        }
        for (CubeLease cubeLease : this.cubes) {
            if (l2 >= cubeLease.cube.expiresAtNanos()) continue;
            if (BattleCubeIgnitionEffects.distance(n, n2, cubeLease.cube.x(), cubeLease.cube.y()) <= 3) {
                return Placement.rejected();
            }
        }
        if (!NativeCubeCastAction.admit(clientSession, 205)) {
            return Placement.rejected();
        }
        l2 = System.nanoTime();
        Cube cube = new Cube(BattleCubeIgnitionEffects.nextObjectId(), l, n, n2, Math.addExact(l2, TimeUnit.SECONDS.toNanos(20L)), this);
        this.cubes.add(new CubeLease(cube, l2));
        return Placement.accepted(cube);
    }
    synchronized List<Event> advance(long nowNanos, List<Actor> actors,
                                     Alliance alliance) {
        NativeSourceCubePlayer.retire(this);
        if (nowNanos < 0 || alliance == null) {
            throw new IllegalArgumentException("invalid Cube: Ignition clock");
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

        for (Iterator<Map.Entry<StatusKey, StatusLease>> iterator =
                statuses.entrySet().iterator(); iterator.hasNext();) {
            StatusLease status = iterator.next().getValue();
            if (status.kind == StatusKind.ENEMY) {
                while (status.nextDamageAtNanos < status.expiresAtNanos
                        && nowNanos >= status.nextDamageAtNanos) {
                    status.tick++;
                    if (status.tick % ENEMY_DAMAGE_EVERY_TICKS == 0) {
                        events.add(new Event(EventKind.ENEMY_DAMAGE, status.cube,
                                status.targetObjectId, status.targetKind, this, status));
                    }
                    status.nextDamageAtNanos = Math.addExact(
                            status.nextDamageAtNanos,
                            TimeUnit.MILLISECONDS.toNanos(ENEMY_TICK_MILLIS));
                }
            }
            if (nowNanos < status.expiresAtNanos) continue;
            iterator.remove();
            events.add(new Event(status.kind == StatusKind.ALLY
                    ? EventKind.ALLY_EXPIRED : EventKind.ENEMY_EXPIRED,
                    status.cube, status.targetObjectId, status.targetKind, this, status));
        }

        long scanInterval = TimeUnit.MILLISECONDS.toNanos(NativeSourceCubePlacement.scanInterval(this));
        for (CubeLease lease : cubes) {
            if (NativeSourceCubeClock.compare(nowNanos, lease.nextScanAtNanos, this) < 0) continue;
            do {
                lease.nextScanAtNanos = Math.addExact(
                        lease.nextScanAtNanos, scanInterval);
            } while (lease.nextScanAtNanos <= nowNanos);
            for (Actor actor : liveActors.values()) {
                if (distance(actor.x(), actor.y(), lease.cube.x(), lease.cube.y())
                        > EFFECT_RADIUS) continue;
                Relation relation = NativeSourceCubeRelation.compare(actor.objectId(), lease.cube.casterObjectId(), lease.cube) == 0
                        ? Relation.ALLY : NativeSourceCubeRelation.relation(alliance,
                                lease.cube.casterObjectId(), actor.objectId(), lease.cube, actor);
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
                        expiresAt, firstEnemyTick, this));
                events.add(new Event(ally ? EventKind.ALLY_APPLIED
                        : EventKind.ENEMY_APPLIED, lease.cube,
                        actor.objectId(), actor.kind(), this, null));
            }
        }
        List<Event> result = List.copyOf(events);
        NativeSourceCubeRemainder.seal(this);
        return result;
    }

    synchronized int fireResistance(long targetObjectId, long nowNanos) {
        if (!NativeSourceCubeRemainder.allowRead(this, targetObjectId)) {
            return 0;
        }
        if (targetObjectId <= 0L || nowNanos < 0L) {
            return 0;
        }
        StatusLease statusLease = this.statuses.get(new StatusKey(targetObjectId, StatusKind.ALLY));
        return statusLease != null && nowNanos < statusLease.expiresAtNanos ? 30 : 0;
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

    synchronized void clear() {
        NativeSourceCubeRemainder.reset(this);
        this.cubes.clear();
        this.statuses.clear();
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

    @AsmFinal({"nativeSourceCubeOrigin"})
    private static final class StatusLease {
        final Cube cube;
        final long targetObjectId;
        final ActorKind targetKind;
        final StatusKind kind;
        final long expiresAtNanos;
        long nextDamageAtNanos;
        int tick;
        Object nativeSourceCubeOrigin;

        StatusLease(Cube cube, long l, ActorKind actorKind, StatusKind statusKind, long l2, long l3) {
            this.cube = cube;
            this.targetObjectId = l;
            this.targetKind = actorKind;
            this.kind = statusKind;
            this.expiresAtNanos = l2;
            this.nextDamageAtNanos = l3;
        }

        StatusLease(Cube cube, long l, ActorKind actorKind, StatusKind statusKind, long l2, long l3, Object object) {
            this(cube, l, actorKind, statusKind, l2, l3);
            this.nativeSourceCubeOrigin = NativeSourceCubeOrigin.lease(this, object);
        }
    }
}
