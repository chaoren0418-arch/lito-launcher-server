package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Runtime state for retained {@code Npc_Dspy}/{@code L1FollowerInstance}. */
final class ClassicWorldDspyEscorts {
    static final int FOLLOW_DISTANCE = 2;
    static final int ABANDON_DISTANCE = 13;
    static final long IDLE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
    static final long MOVE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(
            ClassicWorldDspyCatalog.DSPY_PASSISPEED_MILLIS);
    static final long CORPSE_MILLIS = 10_000L;
    static final long CORPSE_NANOS = TimeUnit.MILLISECONDS.toNanos(CORPSE_MILLIS);

    private static final long FIRST_OBJECT_ID = 0x6D00_0000L;
    private static final long LAST_OBJECT_ID = 0x6DFF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);
    private static final int[] STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] STEP_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    @FunctionalInterface
    interface StepAdmission {
        boolean canStep(int fromX, int fromY, int toX, int toY,
                        long movingObjectId);
    }

    @FunctionalInterface
    interface SpawnAdmission {
        boolean canSpawn(int x, int y);
    }

    enum TransitionKind { NONE, MOVED, ABANDONED, ARRIVED, DEAD_REMOVED }

    record Snapshot(long objectId, int mapId, int x, int y, int heading,
                    String ownerIdentity, int currentHp, int maximumHp,
                    boolean alive, long nextAiNanos, long removeAtNanos) {
        Snapshot {
            ownerIdentity = normalizeIdentity(ownerIdentity);
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || mapId != ClassicWorldDspyCatalog.MAP_ID
                    || x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                    || heading < 0 || heading > 7 || currentHp < 0
                    || maximumHp != ClassicWorldDspyCatalog.DSPY_MAX_HP
                    || currentHp > maximumHp || alive != (currentHp > 0)
                    || nextAiNanos < 0 || removeAtNanos < 0
                    || alive && removeAtNanos != 0
                    || !alive && removeAtNanos == 0) {
                throw new IllegalArgumentException("invalid Dspy follower snapshot");
            }
        }

        boolean owned() { return !ownerIdentity.isEmpty(); }
    }

    record AssassinSpawn(int mapId, int x, int y, long linkedObjectId) {
        AssassinSpawn {
            if (mapId != ClassicWorldDspyCatalog.MAP_ID
                    || x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                    || linkedObjectId < FIRST_OBJECT_ID
                    || linkedObjectId > LAST_OBJECT_ID) {
                throw new IllegalArgumentException("invalid Dspy assassin spawn");
            }
        }
    }

    record Transition(TransitionKind kind, Snapshot before, Snapshot after) {
        Transition {
            if (kind == null) throw new IllegalArgumentException("transition kind required");
            if (kind == TransitionKind.NONE && (before != null || after != null)) {
                throw new IllegalArgumentException("empty Dspy transition carries actors");
            }
            if (kind == TransitionKind.MOVED
                    && (before == null || after == null
                    || before.objectId() != after.objectId()
                    || !before.alive() || !after.alive())) {
                throw new IllegalArgumentException("invalid Dspy movement");
            }
            if (kind == TransitionKind.ABANDONED
                    && (before == null || after == null || !before.owned()
                    || after.owned() || before.objectId() == after.objectId()
                    || !before.alive() || !after.alive())) {
                throw new IllegalArgumentException("invalid Dspy abandonment");
            }
            if ((kind == TransitionKind.ARRIVED
                    || kind == TransitionKind.DEAD_REMOVED)
                    && (before == null || after != null)) {
                throw new IllegalArgumentException("invalid Dspy removal");
            }
        }

        static Transition none() {
            return new Transition(TransitionKind.NONE, null, null);
        }
    }

    record Cycle(Transition transition, AssassinSpawn assassin) {
        Cycle {
            if (transition == null) throw new IllegalArgumentException("transition required");
            if (transition.kind() == TransitionKind.ARRIVED && assassin != null) {
                throw new IllegalArgumentException("arrival suppresses assassin spawn");
            }
        }

        static Cycle none() { return new Cycle(Transition.none(), null); }
    }

    record Damage(Snapshot before, Snapshot after, int appliedDamage,
                  boolean diedNow) {
        Damage {
            if (before == null || after == null || appliedDamage < 0
                    || before.objectId() != after.objectId()
                    || diedNow != (before.alive() && !after.alive())) {
                throw new IllegalArgumentException("invalid Dspy damage transition");
            }
        }
    }

    private static final class MutableFollower {
        final long objectId;
        int x;
        int y;
        int heading;
        String ownerIdentity;
        int currentHp = ClassicWorldDspyCatalog.DSPY_MAX_HP;
        long nextAiNanos;
        long removeAtNanos;

        MutableFollower(long objectId, int x, int y, int heading,
                        String ownerIdentity, long nextAiNanos) {
            this.objectId = objectId;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.ownerIdentity = normalizeIdentity(ownerIdentity);
            this.nextAiNanos = nextAiNanos;
        }

        Snapshot snapshot() {
            return new Snapshot(objectId, ClassicWorldDspyCatalog.MAP_ID,
                    x, y, heading, ownerIdentity, currentHp,
                    ClassicWorldDspyCatalog.DSPY_MAX_HP, currentHp > 0,
                    nextAiNanos, removeAtNanos);
        }
    }

    private final Map<Long, MutableFollower> followers = new LinkedHashMap<>();
    private final Random random;

    ClassicWorldDspyEscorts() {
        this(new Random());
    }

    ClassicWorldDspyEscorts(long seed) {
        this(new Random(seed));
    }

    private ClassicWorldDspyEscorts(Random random) {
        this.random = random;
    }

    synchronized Snapshot claimStatic(String ownerIdentity, int x, int y,
                                      int heading, long nowNanos) {
        String owner = requireIdentity(ownerIdentity);
        if (ownedMutable(owner) != null || nowNanos < 0) return null;
        MutableFollower follower = new MutableFollower(
                nextObjectId(), x, y, heading, owner, nowNanos);
        followers.put(follower.objectId, follower);
        return follower.snapshot();
    }

    synchronized Snapshot claimFree(String ownerIdentity, long sourceObjectId,
                                    long nowNanos) {
        String owner = requireIdentity(ownerIdentity);
        MutableFollower source = followers.get(sourceObjectId);
        if (source == null || source.currentHp <= 0
                || !source.ownerIdentity.isEmpty() || ownedMutable(owner) != null
                || nowNanos < 0) return null;
        followers.remove(sourceObjectId);
        MutableFollower follower = new MutableFollower(
                nextObjectId(), source.x, source.y, source.heading, owner, nowNanos);
        followers.put(follower.objectId, follower);
        return follower.snapshot();
    }

    synchronized Snapshot find(long objectId) {
        MutableFollower follower = followers.get(objectId);
        return follower == null ? null : follower.snapshot();
    }

    synchronized Snapshot owned(String ownerIdentity) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        return follower == null ? null : follower.snapshot();
    }

    synchronized List<Snapshot> inScreen(int mapId, int x, int y) {
        if (mapId != ClassicWorldDspyCatalog.MAP_ID) return List.of();
        List<Snapshot> result = new ArrayList<>();
        for (MutableFollower follower : followers.values()) {
            if (BattleSpellCatalog.isInNativeScreen(x, y, follower.x, follower.y)) {
                result.add(follower.snapshot());
            }
        }
        return List.copyOf(result);
    }

    /**
     * Executes one retained no-target pass.  Arrival and the assassin roll
     * precede owner-loss and follow movement, matching L1FollowerInstance.
     */
    synchronized Cycle cycle(String ownerIdentity, int ownerMapId,
                             int ownerX, int ownerY, boolean ownerAlive,
                             long nowNanos, StepAdmission stepAdmission,
                             SpawnAdmission spawnAdmission) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null || follower.currentHp <= 0
                || nowNanos < follower.nextAiNanos) return Cycle.none();
        Snapshot before = follower.snapshot();
        if (ClassicWorldDspyCatalog.arrival(ownerMapId, ownerX, ownerY)) {
            followers.remove(follower.objectId);
            return new Cycle(new Transition(
                    TransitionKind.ARRIVED, before, null), null);
        }

        AssassinSpawn assassin = null;
        if (random.nextInt(ClassicWorldDspyCatalog.ASSASSIN_ROLL_BOUND)
                <= ClassicWorldDspyCatalog.ASSASSIN_ROLL_MAX_INCLUSIVE) {
            int[] location = randomLocation(follower, spawnAdmission);
            assassin = new AssassinSpawn(ClassicWorldDspyCatalog.MAP_ID,
                    location[0], location[1], follower.objectId);
        }

        int range = tileLineDistance(follower.x, follower.y, ownerX, ownerY);
        if (!ownerAlive || ownerMapId != ClassicWorldDspyCatalog.MAP_ID
                || range > ABANDON_DISTANCE) {
            followers.remove(follower.objectId);
            MutableFollower free = new MutableFollower(
                    nextObjectId(), follower.x, follower.y, follower.heading, "", 0);
            followers.put(free.objectId, free);
            return new Cycle(new Transition(
                    TransitionKind.ABANDONED, before, free.snapshot()), assassin);
        }

        if (range > FOLLOW_DISTANCE) {
            if (stepAdmission == null) {
                throw new IllegalArgumentException("step admission is required");
            }
            int preferred = heading(follower.x, follower.y, ownerX, ownerY);
            int[] order = {
                    preferred, (preferred + 1) & 7, (preferred + 7) & 7,
                    (preferred + 2) & 7, (preferred + 6) & 7,
                    (preferred + 3) & 7, (preferred + 5) & 7,
                    (preferred + 4) & 7
            };
            for (int candidate : order) {
                int nextX = follower.x + STEP_X[candidate];
                int nextY = follower.y + STEP_Y[candidate];
                if (!stepAdmission.canStep(
                        follower.x, follower.y, nextX, nextY,
                        follower.objectId)) continue;
                if (tileLineDistance(nextX, nextY, ownerX, ownerY) > range) continue;
                follower.x = nextX;
                follower.y = nextY;
                follower.heading = candidate;
                follower.nextAiNanos = saturatedAdd(nowNanos, MOVE_INTERVAL_NANOS);
                return new Cycle(new Transition(
                        TransitionKind.MOVED, before, follower.snapshot()), assassin);
            }
        }
        follower.nextAiNanos = saturatedAdd(nowNanos, IDLE_INTERVAL_NANOS);
        return new Cycle(Transition.none(), assassin);
    }

    synchronized Damage damage(long objectId, int damage, long nowNanos) {
        MutableFollower follower = followers.get(objectId);
        if (follower == null || follower.currentHp <= 0 || damage <= 0
                || nowNanos < 0) return null;
        Snapshot before = follower.snapshot();
        int applied = Math.min(follower.currentHp, damage);
        follower.currentHp -= applied;
        boolean died = follower.currentHp == 0;
        if (died) {
            follower.nextAiNanos = Long.MAX_VALUE;
            follower.removeAtNanos = saturatedAdd(nowNanos, CORPSE_NANOS);
        }
        return new Damage(before, follower.snapshot(), applied, died);
    }

    synchronized List<Snapshot> expireCorpses(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative Dspy clock");
        List<Snapshot> removed = new ArrayList<>();
        var iterator = followers.entrySet().iterator();
        while (iterator.hasNext()) {
            MutableFollower follower = iterator.next().getValue();
            if (follower.currentHp == 0 && nowNanos >= follower.removeAtNanos) {
                removed.add(follower.snapshot());
                iterator.remove();
            }
        }
        return List.copyOf(removed);
    }

    synchronized Transition ownerLost(String ownerIdentity) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null) return Transition.none();
        Snapshot before = follower.snapshot();
        followers.remove(follower.objectId);
        if (!before.alive()) {
            return new Transition(TransitionKind.DEAD_REMOVED, before, null);
        }
        MutableFollower free = new MutableFollower(
                nextObjectId(), follower.x, follower.y, follower.heading, "", 0);
        followers.put(free.objectId, free);
        return new Transition(TransitionKind.ABANDONED, before, free.snapshot());
    }

    synchronized void rollbackClaim(Snapshot claimed, Snapshot originalFree) {
        if (claimed == null || !claimed.owned()) {
            throw new IllegalArgumentException("claimed Dspy follower is required");
        }
        MutableFollower current = followers.get(claimed.objectId());
        if (current == null || !current.ownerIdentity.equals(claimed.ownerIdentity())) {
            throw new IllegalStateException("Dspy claim no longer owns runtime actor");
        }
        followers.remove(claimed.objectId());
        if (originalFree == null) return;
        if (originalFree.owned() || !originalFree.alive()
                || followers.containsKey(originalFree.objectId())) {
            throw new IllegalStateException("cannot restore original free Dspy actor");
        }
        MutableFollower restored = new MutableFollower(
                originalFree.objectId(), originalFree.x(), originalFree.y(),
                originalFree.heading(), "", originalFree.nextAiNanos());
        followers.put(restored.objectId, restored);
    }

    synchronized void restoreAfterFailedArrival(Snapshot snapshot) {
        if (snapshot == null || !snapshot.owned() || !snapshot.alive()
                || followers.containsKey(snapshot.objectId())
                || ownedMutable(snapshot.ownerIdentity()) != null) {
            throw new IllegalStateException("cannot restore Dspy arrival");
        }
        MutableFollower restored = new MutableFollower(
                snapshot.objectId(), snapshot.x(), snapshot.y(), snapshot.heading(),
                snapshot.ownerIdentity(), snapshot.nextAiNanos());
        restored.currentHp = snapshot.currentHp();
        restored.removeAtNanos = snapshot.removeAtNanos();
        followers.put(restored.objectId, restored);
    }

    synchronized int size() { return followers.size(); }

    private int[] randomLocation(MutableFollower follower,
                                 SpawnAdmission admission) {
        int radius = ClassicWorldDspyCatalog.ASSASSIN_RADIUS;
        int cells = 1 + radius * 2;
        int attempts = 40 * cells * cells;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = follower.x - radius + random.nextInt(cells);
            int y = follower.y - radius + random.nextInt(cells);
            if (admission == null || admission.canSpawn(x, y)) {
                return new int[]{x, y};
            }
        }
        return new int[]{follower.x, follower.y};
    }

    private MutableFollower ownedMutable(String ownerIdentity) {
        if (ownerIdentity.isEmpty()) return null;
        for (MutableFollower follower : followers.values()) {
            if (follower.ownerIdentity.equals(ownerIdentity)) return follower;
        }
        return null;
    }

    static String normalizeIdentityForWire(String identity) {
        return normalizeIdentity(identity);
    }

    private static String requireIdentity(String identity) {
        String normalized = normalizeIdentity(identity);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("owner identity is required");
        }
        return normalized;
    }

    private static String normalizeIdentity(String identity) {
        return identity == null ? ""
                : identity.trim().toLowerCase(Locale.ROOT);
    }

    private static int tileLineDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private static int heading(int x, int y, int targetX, int targetY) {
        int dx = Integer.compare(targetX, x);
        int dy = Integer.compare(targetY, y);
        if (dx == 0 && dy < 0) return 0;
        if (dx > 0 && dy < 0) return 1;
        if (dx > 0 && dy == 0) return 2;
        if (dx > 0) return 3;
        if (dx == 0 && dy > 0) return 4;
        if (dx < 0 && dy > 0) return 5;
        if (dx < 0 && dy == 0) return 6;
        return 7;
    }

    private static long saturatedAdd(long left, long right) {
        if (left < 0 || right < 0) {
            throw new IllegalArgumentException("negative deadline input");
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }
}
