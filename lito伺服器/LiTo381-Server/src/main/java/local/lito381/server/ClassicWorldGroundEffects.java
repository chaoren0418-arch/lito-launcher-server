package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Map/instance-isolated retained leases for ordinary-WORLD coordinate spells. */
final class ClassicWorldGroundEffects {
    enum ActorKind { PLAYER, MONSTER }

    record Actor(long objectId, int x, int y, boolean alive, ActorKind kind) {
        Actor {
            if (objectId <= 0 || x <= 0 || y <= 0 || kind == null) {
                throw new IllegalArgumentException("invalid WORLD ground actor");
            }
        }
    }

    record Pulse(int skillId, long casterObjectId, long targetObjectId,
                 ActorKind targetKind) {}

    record Point(int x, int y) {
        Point {
            if (x <= 0 || y <= 0) {
                throw new IllegalArgumentException("invalid WORLD ground point");
            }
        }
    }

    record CastResult(boolean accepted, List<Point> points) {
        CastResult {
            points = points == null ? List.of() : List.copyOf(points);
        }

        static CastResult rejected() {
            return new CastResult(false, List.of());
        }
    }

    record Active(int skillId, long casterObjectId, int x, int y, int radius) {}

    private static final int MAX_FIRE_WALL_SEGMENTS_PER_CASTER = 24;
    private final List<Lease> leases = new ArrayList<>();

    CastResult cast(BattleSpellCatalog.GroundSpell spell, long casterObjectId,
                    int sourceX, int sourceY, int targetX, int targetY,
                    ClassicWorldMapCatalog.ClassicMap map, long nowNanos) {
        if (spell == null || casterObjectId <= 0 || map == null || nowNanos < 0
                || !map.contains(sourceX, sourceY)
                || !map.contains(targetX, targetY)) {
            return CastResult.rejected();
        }
        List<Point> points = spell.skillId() == BattleSpellCatalog.FIRE_WALL
                ? fireWallPoints(spell, casterObjectId, sourceX, sourceY,
                        targetX, targetY, map, nowNanos)
                : List.of(new Point(targetX, targetY));
        if (points.isEmpty()) return CastResult.rejected();

        long expiresAt = Math.addExact(nowNanos,
                Math.multiplyExact(spell.durationSeconds(), 1_000_000_000L));
        long firstPulse = spell.pulseIntervalMillis() <= 0 ? Long.MAX_VALUE
                : Math.addExact(nowNanos,
                        Math.multiplyExact(spell.pulseIntervalMillis(), 1_000_000L));
        for (Point point : points) {
            leases.add(new Lease(spell.skillId(), casterObjectId,
                    point.x(), point.y(), spell.effectRadius(), expiresAt,
                    firstPulse, spell.pulseIntervalMillis()));
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
                lease.nextPulseNanos = Math.addExact(
                        lease.nextPulseNanos, intervalNanos);
            } while (lease.nextPulseNanos <= nowNanos);
            if (actors == null) continue;
            for (Actor actor : actors) {
                if (actor == null || !actor.alive()
                        || actor.objectId() == lease.casterObjectId) continue;
                int distance = Math.max(Math.abs(actor.x() - lease.x),
                        Math.abs(actor.y() - lease.y));
                if (distance <= lease.radius) {
                    pulses.add(new Pulse(lease.skillId, lease.casterObjectId,
                            actor.objectId(), actor.kind()));
                }
            }
        }
        return List.copyOf(pulses);
    }

    boolean contains(int skillId, int x, int y, long nowNanos) {
        if (nowNanos < 0) return false;
        expire(nowNanos);
        for (Lease lease : leases) {
            if (lease.skillId != skillId) continue;
            int distance = Math.max(Math.abs(x - lease.x), Math.abs(y - lease.y));
            if (distance <= lease.radius) return true;
        }
        return false;
    }

    List<Active> active(long nowNanos) {
        expire(nowNanos);
        return leases.stream().map(lease -> new Active(
                lease.skillId, lease.casterObjectId, lease.x, lease.y,
                lease.radius)).toList();
    }

    int activeCount(long nowNanos) {
        expire(nowNanos);
        return leases.size();
    }

    private void expire(long nowNanos) {
        leases.removeIf(lease -> nowNanos >= lease.expiresAtNanos);
    }

    private List<Point> fireWallPoints(
            BattleSpellCatalog.GroundSpell spell, long casterObjectId,
            int sourceX, int sourceY, int targetX, int targetY,
            ClassicWorldMapCatalog.ClassicMap map, long nowNanos) {
        int dx = Integer.compare(targetX, sourceX);
        int dy = Integer.compare(targetY, sourceY);
        if (dx == 0 && dy == 0) return List.of();
        expire(nowNanos);
        // Retained L1SpawnUtil counts the caster's L1EffectInstance objects
        // returned by World.getVisibleObjects(caster), not every object on the
        // map. Ground effects of either supported kind share that native cap.
        int existingCount = (int) leases.stream().filter(lease ->
                lease.casterObjectId == casterObjectId
                        && BattleSpellCatalog.isInNativeScreen(
                                sourceX, sourceY, lease.x, lease.y)).count();
        int available = MAX_FIRE_WALL_SEGMENTS_PER_CASTER - existingCount;
        if (available <= 0) return List.of();

        List<Point> result = new ArrayList<>();
        int previousX = sourceX;
        int previousY = sourceY;
        int count = Math.min(spell.segmentCount(), available);
        for (int index = 1; index <= count; index++) {
            int x = sourceX + dx * index;
            int y = sourceY + dy * index;
            if (!map.contains(x, y)
                    || !map.attackPosition(previousX, previousY, x, y, 1)
                    || !map.arrowPassable(x, y)) break;
            previousX = x;
            previousY = y;
            boolean duplicate = leases.stream().anyMatch(lease ->
                    lease.skillId == BattleSpellCatalog.FIRE_WALL
                            && lease.x == x && lease.y == y);
            if (!duplicate) result.add(new Point(x, y));
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
