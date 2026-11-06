// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Server-side leases for coordinate-targeted spell effects.
 *
 * <p>{@code S_EFFECT_LOC} contains only x/y/gfx and therefore cannot identify
 * an effect for a later {@code S_REMOVE_OBJECT}.  The native visual and the
 * authoritative gameplay lease are deliberately kept separate here.</p>
 */
final class BattleGroundEffects {
    record Actor(long objectId, int x, int y, boolean alive, boolean bot) {}
    @AsRecord(components = {"skillId", "casterObjectId", "targetObjectId", "bot"}, finalFields = {"nativeSourceGroundProof"})
    static final class Pulse {
        private final int skillId;
        private final long casterObjectId;
        private final long targetObjectId;
        private final boolean bot;
        Object nativeSourceGroundProof;

        Pulse(int n, long l, long l2, boolean bl) {
            this.skillId = n;
            this.casterObjectId = l;
            this.targetObjectId = l2;
            this.bot = bl;
        }




        public int skillId() {
            return this.skillId;
        }

        public long casterObjectId() {
            return this.casterObjectId;
        }

        public long targetObjectId() {
            return this.targetObjectId;
        }

        public boolean bot() {
            return this.bot;
        }

        Pulse(int n, long l, long l2, boolean bl, BattleGroundEffects battleGroundEffects, Object object) {
            this(n, l, l2, bl);
            this.nativeSourceGroundProof = NativeSourceGroundOrigin.pulse(this, battleGroundEffects, object);
        }
    }
    record CastResult(boolean accepted, List<ArenaMapCatalog.Point> points) {
        CastResult {
            points = points == null ? List.of() : List.copyOf(points);
        }

        static CastResult rejected() {
            return new CastResult(false, List.of());
        }
    }

    private static final int MAX_FIRE_WALL_SEGMENTS_PER_CASTER = 24;
    private final List<Lease> leases = new ArrayList<>();
    volatile Object nativeSourceGroundEpoch;
    volatile Object nativeSourceGroundScene;
    volatile Object nativeSourceGroundInstalling;
    volatile Object nativeSourceGroundScan;

    BattleGroundEffects() {
        NativeSourceGroundOrigin.reset(this);
    }

    CastResult cast(BattleSpellCatalog.GroundSpell spell, long casterObjectId, int sourceX, int sourceY, int targetX, int targetY, ArenaMapCatalog.Arena arena, long nowNanos) {
        List<ArenaMapCatalog.Point> list;
        if (spell == null || casterObjectId <= 0L || arena == null || nowNanos < 0L || !arena.contains(targetX, targetY)) {
            return CastResult.rejected();
        }
        List<ArenaMapCatalog.Point> list2 = list = spell.skillId() == 58 ? this.fireWallPoints(spell, casterObjectId, sourceX, sourceY, targetX, targetY, arena) : List.of(new ArenaMapCatalog.Point(targetX, targetY));
        if (list.isEmpty()) {
            return CastResult.rejected();
        }
        long l3 = Math.addExact(nowNanos, Math.multiplyExact((long)spell.durationSeconds(), 1000000000L));
        long l4 = spell.pulseIntervalMillis() <= 0 ? Long.MAX_VALUE : Math.addExact(nowNanos, Math.multiplyExact((long)spell.pulseIntervalMillis(), 1000000L));
        for (ArenaMapCatalog.Point point : list) {
            this.leases.add(new Lease(spell.skillId(), casterObjectId, point.x(), point.y(), spell.effectRadius(), l3, l4, spell.pulseIntervalMillis(), this));
        }
        return new CastResult(true, list);
    }

    CastResult castPattern(int skillId, long casterObjectId, List<ArenaMapCatalog.Point> points, int radius, int durationMillis, int pulseIntervalMillis, ArenaMapCatalog.Arena arena, long nowNanos) {
        if (skillId <= 0 || casterObjectId <= 0L || points == null || points.isEmpty() || radius < 0 || durationMillis <= 0 || pulseIntervalMillis <= 0 || arena == null || nowNanos < 0L || points.stream().anyMatch(point -> point == null || !arena.contains(point.x(), point.y()))) {
            return CastResult.rejected();
        }
        long l3 = Math.addExact(nowNanos, Math.multiplyExact((long)durationMillis, 1000000L));
        long l4 = Math.addExact(nowNanos, Math.multiplyExact((long)pulseIntervalMillis, 1000000L));
        for (ArenaMapCatalog.Point point2 : points) {
            this.leases.add(new Lease(skillId, casterObjectId, point2.x(), point2.y(), radius, l3, l4, pulseIntervalMillis, this));
        }
        return new CastResult(true, points);
    }

    List<Pulse> advance(long nowNanos, List<Actor> actors) {
        List<Pulse> pulses = new ArrayList<>();
        for (Iterator<Lease> iterator = leases.iterator(); iterator.hasNext();) {
            Lease lease = iterator.next();
            if (nowNanos >= lease.expiresAtNanos) {
                iterator.remove();
                continue;
            }
            if (lease.pulseIntervalMillis <= 0 || nowNanos < lease.nextPulseNanos) {
                continue;
            }
            long intervalNanos = lease.pulseIntervalMillis * 1_000_000L;
            do {
                lease.nextPulseNanos = Math.addExact(lease.nextPulseNanos, intervalNanos);
            } while (lease.nextPulseNanos <= nowNanos);

            if (actors == null) continue;
            for (Actor actor : actors) {
                if (actor == null || !actor.alive() || actor.objectId() == lease.casterObjectId) {
                    continue;
                }
                int distance = Math.max(Math.abs(actor.x() - lease.x),
                        Math.abs(actor.y() - lease.y));
                if (distance <= lease.radius) {
                    pulses.add(new Pulse(lease.skillId, lease.casterObjectId,
                            actor.objectId(), actor.bot(), this, lease));
                }
            }
        }
        return List.copyOf(pulses);
    }

    int activeCount() {
        return leases.size();
    }

    boolean contains(int skillId, int x, int y, long nowNanos) {
        if (nowNanos < 0) return false;
        for (Lease lease : leases) {
            if (lease.skillId != skillId || nowNanos >= lease.expiresAtNanos) continue;
            int distance = Math.max(Math.abs(x - lease.x), Math.abs(y - lease.y));
            if (distance <= lease.radius) return true;
        }
        return false;
    }

    void clear() {
        NativeSourceGroundOrigin.reset(this);
        this.leases.clear();
    }

    private List<ArenaMapCatalog.Point> fireWallPoints(
            BattleSpellCatalog.GroundSpell spell, long casterObjectId,
            int sourceX, int sourceY, int targetX, int targetY,
            ArenaMapCatalog.Arena arena) {
        int dx = Integer.compare(targetX, sourceX);
        int dy = Integer.compare(targetY, sourceY);
        if (dx == 0 && dy == 0) return List.of();

        int available = MAX_FIRE_WALL_SEGMENTS_PER_CASTER;
        for (Lease lease : leases) {
            if (lease.skillId == BattleSpellCatalog.FIRE_WALL
                    && lease.casterObjectId == casterObjectId) {
                available--;
            }
        }
        if (available <= 0) return List.of();

        List<ArenaMapCatalog.Point> result = new ArrayList<>();
        int count = Math.min(spell.segmentCount(), available);
        int previousX = sourceX;
        int previousY = sourceY;
        for (int index = 1; index <= count; index++) {
            int x = sourceX + dx * index;
            int y = sourceY + dy * index;
            // The classic Fire Wall placement stops at an arrow-blocked edge.
            // Tournament maps expose their authoritative directional boundary
            // through the same Layer3 data consumed by LiTo.bin sub_48B640.
            if (!arena.contains(x, y)
                    || !arena.nativeCanStep(previousX, previousY, x, y)) {
                break;
            }
            previousX = x;
            previousY = y;
            boolean duplicate = leases.stream().anyMatch(existing ->
                    existing.skillId == BattleSpellCatalog.FIRE_WALL
                            && existing.x == x && existing.y == y);
            if (!duplicate) result.add(new ArenaMapCatalog.Point(x, y));
        }
        return result;
    }

    @AsmFinal({"nativeSourceGroundEntity"})
    private static final class Lease {
        final int skillId;
        final long casterObjectId;
        final int x;
        final int y;
        final int radius;
        final long expiresAtNanos;
        final int pulseIntervalMillis;
        long nextPulseNanos;
        Object nativeSourceGroundEntity;

        Lease(int n, long l, int n2, int n3, int n4, long l2, long l3, int n5) {
            this.skillId = n;
            this.casterObjectId = l;
            this.x = n2;
            this.y = n3;
            this.radius = n4;
            this.expiresAtNanos = l2;
            this.nextPulseNanos = l3;
            this.pulseIntervalMillis = n5;
        }

        Lease(int n, long l, int n2, int n3, int n4, long l2, long l3, int n5, BattleGroundEffects battleGroundEffects) {
            this(n, l, n2, n3, n4, l2, l3, n5);
            this.nativeSourceGroundEntity = NativeSourceGroundOrigin.entity(this, battleGroundEffects);
        }
    }
}
