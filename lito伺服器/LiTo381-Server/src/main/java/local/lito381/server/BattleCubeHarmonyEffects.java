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
            if (l < 0x75000000L || l > 0x75FFFFFFL || l2 <= 0L || l2 > 0xFFFFFFFFL || n < 0 || n > 65535 || n2 < 0 || n2 > 65535 || l3 <= 0L) {
                throw new IllegalArgumentException("invalid Cube: Harmony object");
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
                throw new IllegalArgumentException("invalid Cube: Harmony event");
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
                        TimeUnit.SECONDS.toNanos(DURATION_SECONDS)), this);
        cubes.add(new CubeLease(cube, nowNanos));
        return Placement.accepted(cube);
    }


    synchronized Placement nativeMaterialPlace(long l, int n, int n2, long l2, ClientSession clientSession) {
        if (l <= 0L || l > 0xFFFFFFFFL || n < 0 || n > 65535 || n2 < 0 || n2 > 65535 || l2 < 0L) {
            return Placement.rejected();
        }
        for (CubeLease cubeLease : this.cubes) {
            if (l2 >= cubeLease.cube.expiresAtNanos()) continue;
            if (BattleCubeHarmonyEffects.distance(n, n2, cubeLease.cube.x(), cubeLease.cube.y()) <= 3) {
                return Placement.rejected();
            }
        }
        if (!NativeCubeCastAction.admit(clientSession, 220)) {
            return Placement.rejected();
        }
        l2 = System.nanoTime();
        Cube cube = new Cube(BattleCubeHarmonyEffects.nextObjectId(), l, n, n2, Math.addExact(l2, TimeUnit.SECONDS.toNanos(20L)), this);
        this.cubes.add(new CubeLease(cube, l2));
        return Placement.accepted(cube);
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
                            status.targetObjectId, status.targetKind, this, status));
                }
                if (status.tick % HP_EVERY_TICKS == 0) {
                    events.add(new Event(EventKind.HP_DAMAGE, status.cube,
                            status.targetObjectId, status.targetKind, this, status));
                }
                status.nextTickAtNanos = Math.addExact(
                        status.nextTickAtNanos,
                        TimeUnit.MILLISECONDS.toNanos(TICK_MILLIS));
            }
            if (nowNanos < status.expiresAtNanos) continue;
            iterator.remove();
            events.add(new Event(EventKind.STATUS_EXPIRED, status.cube,
                    status.targetObjectId, status.targetKind, this, status));
        }

        long scanInterval = TimeUnit.MILLISECONDS.toNanos(
                NativeSourceCubePlacement.scanInterval(this));
        for (CubeLease lease : cubes) {
            if (NativeSourceCubeClock.compare(nowNanos, lease.nextScanAtNanos, this) < 0) continue;
            do {
                lease.nextScanAtNanos = Math.addExact(
                        lease.nextScanAtNanos, scanInterval);
            } while (lease.nextScanAtNanos <= nowNanos);
            for (Actor actor : liveActors.values()) {
                if (distance(actor.x(), actor.y(), lease.cube.x(),
                        lease.cube.y()) > EFFECT_RADIUS) continue;
                Relation relation = NativeSourceCubeRelation.compare(actor.objectId(), lease.cube.casterObjectId(), lease.cube) == 0
                        ? Relation.ALLY : NativeSourceCubeRelation.relation(alliance,
                                lease.cube.casterObjectId(), actor.objectId(), lease.cube, actor);
                if (relation == null || relation == Relation.IGNORE) continue;
                StatusLease existing = statuses.get(actor.objectId());
                if (existing != null && nowNanos < existing.expiresAtNanos) {
                    continue;
                }
                long expiresAt = Math.addExact(nowNanos,
                        TimeUnit.SECONDS.toNanos(STATUS_SECONDS));
                long firstTick = Math.addExact(nowNanos,
                        TimeUnit.MILLISECONDS.toNanos(INITIAL_DELAY_MILLIS));
                Map<Long, StatusLease> targetStatuses = statuses;
                Long targetId = actor.objectId();
                StatusLease created = new StatusLease(
                        lease.cube, actor.objectId(), actor.kind(),
                        expiresAt, firstTick, this);
                created.nativeSourceCubePresentationRelation = relation;
                targetStatuses.put(targetId, created);
                events.add(new Event(EventKind.STATUS_APPLIED, lease.cube,
                        actor.objectId(), actor.kind(), this, null));
            }
        }
        List<Event> result = List.copyOf(events);
        NativeSourceCubeRemainder.seal(this);
        return result;
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
        if (!NativeSourceCubeRemainder.allowRead(this, targetObjectId)) {
            return false;
        }
        if (targetObjectId <= 0L || nowNanos < 0L) {
            return false;
        }
        StatusLease statusLease = this.statuses.get(targetObjectId);
        return statusLease != null && nowNanos < statusLease.expiresAtNanos;
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

    @AsmFinal({"nativeSourceCubeOrigin"})
    private static final class StatusLease {
        final Cube cube;
        final long targetObjectId;
        final ActorKind targetKind;
        final long expiresAtNanos;
        long nextTickAtNanos;
        int tick;
        Object nativeSourceCubeOrigin;
        volatile Object nativeSourceCubePresentationRelation;

        StatusLease(Cube cube, long l, ActorKind actorKind, long l2, long l3) {
            this.cube = cube;
            this.targetObjectId = l;
            this.targetKind = actorKind;
            this.expiresAtNanos = l2;
            this.nextTickAtNanos = l3;
        }

        StatusLease(Cube cube, long l, ActorKind actorKind, long l2, long l3, Object object) {
            this(cube, l, actorKind, l2, l3);
            this.nativeSourceCubeOrigin = NativeSourceCubeOrigin.lease(this, object);
        }
    }
}
