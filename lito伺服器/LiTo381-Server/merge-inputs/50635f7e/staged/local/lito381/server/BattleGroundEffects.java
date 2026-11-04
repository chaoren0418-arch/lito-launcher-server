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
    record Pulse(int skillId, long casterObjectId, long targetObjectId, boolean bot) {}
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

    CastResult cast(BattleSpellCatalog.GroundSpell spell, long casterObjectId,
                    int sourceX, int sourceY, int targetX, int targetY,
                    ArenaMapCatalog.Arena arena, long nowNanos) {
        if (spell == null || casterObjectId <= 0 || arena == null || nowNanos < 0
                || !arena.contains(targetX, targetY)) {
            return CastResult.rejected();
        }

        List<ArenaMapCatalog.Point> points = spell.skillId() == BattleSpellCatalog.FIRE_WALL
                ? fireWallPoints(spell, casterObjectId, sourceX, sourceY,
                        targetX, targetY, arena)
                : List.of(new ArenaMapCatalog.Point(targetX, targetY));
        if (points.isEmpty()) return CastResult.rejected();

        long expiresAt = Math.addExact(nowNanos,
                Math.multiplyExact(spell.durationSeconds(), 1_000_000_000L));
        long firstPulse = spell.pulseIntervalMillis() <= 0 ? Long.MAX_VALUE
                : Math.addExact(nowNanos,
                        Math.multiplyExact(spell.pulseIntervalMillis(), 1_000_000L));
        for (ArenaMapCatalog.Point point : points) {
            leases.add(new Lease(spell.skillId(), casterObjectId,
                    point.x(), point.y(), spell.effectRadius(), expiresAt,
                    firstPulse, spell.pulseIntervalMillis()));
        }
        return new CastResult(true, points);
    }

    CastResult castPattern(int skillId, long casterObjectId,
                           List<ArenaMapCatalog.Point> points, int radius,
                           int durationMillis, int pulseIntervalMillis,
                           ArenaMapCatalog.Arena arena, long nowNanos) {
        if (skillId <= 0 || casterObjectId <= 0 || points == null || points.isEmpty()
                || radius < 0 || durationMillis <= 0 || pulseIntervalMillis <= 0
                || arena == null || nowNanos < 0
                || points.stream().anyMatch(point -> point == null
                    || !arena.contains(point.x(), point.y()))) {
            return CastResult.rejected();
        }
        long expiresAt = Math.addExact(nowNanos,
                Math.multiplyExact(durationMillis, 1_000_000L));
        long firstPulse = Math.addExact(nowNanos,
                Math.multiplyExact(pulseIntervalMillis, 1_000_000L));
        for (ArenaMapCatalog.Point point : points) {
            leases.add(new Lease(skillId, casterObjectId, point.x(), point.y(),
                    radius, expiresAt, firstPulse, pulseIntervalMillis));
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
                            actor.objectId(), actor.bot()));
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
        leases.clear();
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

    private static final class Lease {
        final int skillId;
        final long casterObjectId;
        final int x;
        final int y;
        final int radius;
        final long expiresAtNanos;
        final int pulseIntervalMillis;
        long nextPulseNanos;

        Lease(int skillId, long casterObjectId, int x, int y, int radius,
              long expiresAtNanos, long nextPulseNanos, int pulseIntervalMillis) {
            this.skillId = skillId;
            this.casterObjectId = casterObjectId;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.expiresAtNanos = expiresAtNanos;
            this.nextPulseNanos = nextPulseNanos;
            this.pulseIntervalMillis = pulseIntervalMillis;
        }
    }
}
