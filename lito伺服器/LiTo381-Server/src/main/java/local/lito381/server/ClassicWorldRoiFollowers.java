package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime state for the retained EWLv40_1 Roi follower lifecycle.
 *
 * <p>The selected spawn row has a zero respawn delay.  Claiming the static
 * 81209 actor therefore creates one private 70957 follower while the cursed
 * spawn is immediately available again for another player.  An owner death,
 * map separation, or native tile-line distance greater than thirteen replaces
 * that follower with an unowned 70957 at its last tile.</p>
 */
final class ClassicWorldRoiFollowers {
    static final int MAP_ID = ClassicWorldDemonTempleCatalog.MAP_ID;
    static final int NPC_ID = 70957;
    static final int GFX_ID = ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX;
    static final String NAME_ID = "$3420";
    static final int QUEST_ID = 138;
    static final int QUEST_STARTED = 1;
    static final int QUEST_COMPLETE = 255;
    static final int FOLLOW_DISTANCE = 2;
    static final int ABANDON_DISTANCE = 13;
    static final long MOVE_INTERVAL_NANOS = 640_000_000L;

    private static final long FIRST_OBJECT_ID = 0x6E00_0000L;
    private static final long LAST_OBJECT_ID = 0x6EFF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);
    private static final int[] STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] STEP_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    @FunctionalInterface
    interface StepAdmission {
        boolean canStep(int fromX, int fromY, int toX, int toY, long movingObjectId);
    }

    enum TransitionKind { NONE, MOVED, ABANDONED }

    record Snapshot(long objectId, int mapId, int x, int y, int heading,
                    String ownerIdentity, long nextMoveNanos) {
        Snapshot {
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || mapId != MAP_ID || x < 0 || x > 0xFFFF
                    || y < 0 || y > 0xFFFF || heading < 0 || heading > 7
                    || nextMoveNanos < 0) {
                throw new IllegalArgumentException("invalid Roi follower snapshot");
            }
            ownerIdentity = normalizeIdentity(ownerIdentity);
        }

        boolean owned() {
            return !ownerIdentity.isEmpty();
        }
    }

    record Transition(TransitionKind kind, Snapshot before, Snapshot after) {
        Transition {
            if (kind == null) throw new IllegalArgumentException("transition kind is required");
            if (kind == TransitionKind.NONE && (before != null || after != null)) {
                throw new IllegalArgumentException("empty transition carries actors");
            }
            if (kind == TransitionKind.MOVED
                    && (before == null || after == null
                    || before.objectId() != after.objectId())) {
                throw new IllegalArgumentException("movement must retain one object id");
            }
            if (kind == TransitionKind.ABANDONED
                    && (before == null || after == null || !before.owned()
                    || after.owned() || before.objectId() == after.objectId())) {
                throw new IllegalArgumentException("abandonment must replace the follower");
            }
        }

        static Transition none() {
            return new Transition(TransitionKind.NONE, null, null);
        }
    }

    private static final class MutableFollower {
        final long objectId;
        int x;
        int y;
        int heading;
        String ownerIdentity;
        long nextMoveNanos;

        MutableFollower(long objectId, int x, int y, int heading,
                        String ownerIdentity, long nextMoveNanos) {
            this.objectId = objectId;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.ownerIdentity = normalizeIdentity(ownerIdentity);
            this.nextMoveNanos = nextMoveNanos;
        }

        Snapshot snapshot() {
            return new Snapshot(objectId, MAP_ID, x, y, heading,
                    ownerIdentity, nextMoveNanos);
        }
    }

    private final Map<Long, MutableFollower> followers = new LinkedHashMap<>();

    synchronized Snapshot claimStatic(String ownerIdentity, int x, int y,
                                      int heading, long nowNanos) {
        String owner = requireIdentity(ownerIdentity);
        if (ownedMutable(owner) != null || nowNanos < 0) return null;
        MutableFollower follower = new MutableFollower(
                nextObjectId(), x, y, heading, owner,
                saturatedAdd(nowNanos, MOVE_INTERVAL_NANOS));
        followers.put(follower.objectId, follower);
        return follower.snapshot();
    }

    synchronized Snapshot claimFree(String ownerIdentity, long sourceObjectId,
                                    long nowNanos) {
        String owner = requireIdentity(ownerIdentity);
        MutableFollower source = followers.get(sourceObjectId);
        if (source == null || !source.ownerIdentity.isEmpty()
                || ownedMutable(owner) != null || nowNanos < 0) return null;
        followers.remove(sourceObjectId);
        MutableFollower follower = new MutableFollower(
                nextObjectId(), source.x, source.y, source.heading, owner,
                saturatedAdd(nowNanos, MOVE_INTERVAL_NANOS));
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
        if (mapId != MAP_ID) return List.of();
        List<Snapshot> result = new ArrayList<>();
        for (MutableFollower follower : followers.values()) {
            if (BattleSpellCatalog.isInNativeScreen(x, y, follower.x, follower.y)) {
                result.add(follower.snapshot());
            }
        }
        return List.copyOf(result);
    }

    synchronized Transition tick(String ownerIdentity, int ownerMapId,
                                 int ownerX, int ownerY, boolean ownerAlive,
                                 long nowNanos, StepAdmission admission) {
        String owner = normalizeIdentity(ownerIdentity);
        MutableFollower follower = ownedMutable(owner);
        if (follower == null) return Transition.none();
        Snapshot before = follower.snapshot();
        int range = tileLineDistance(follower.x, follower.y, ownerX, ownerY);
        if (!ownerAlive || ownerMapId != MAP_ID || range > ABANDON_DISTANCE) {
            followers.remove(follower.objectId);
            MutableFollower free = new MutableFollower(
                    nextObjectId(), follower.x, follower.y, follower.heading,
                    "", 0);
            followers.put(free.objectId, free);
            return new Transition(TransitionKind.ABANDONED, before, free.snapshot());
        }
        if (range <= FOLLOW_DISTANCE || nowNanos < follower.nextMoveNanos) {
            return Transition.none();
        }
        if (admission == null) throw new IllegalArgumentException("step admission is required");
        int preferred = heading(follower.x, follower.y, ownerX, ownerY);
        int[] order = {
                preferred, (preferred + 1) & 7, (preferred + 7) & 7,
                (preferred + 2) & 7, (preferred + 6) & 7,
                (preferred + 3) & 7, (preferred + 5) & 7, (preferred + 4) & 7
        };
        for (int candidate : order) {
            int nextX = follower.x + STEP_X[candidate];
            int nextY = follower.y + STEP_Y[candidate];
            if (!admission.canStep(follower.x, follower.y, nextX, nextY,
                    follower.objectId)) continue;
            int nextRange = tileLineDistance(nextX, nextY, ownerX, ownerY);
            if (nextRange > range) continue;
            follower.x = nextX;
            follower.y = nextY;
            follower.heading = candidate;
            follower.nextMoveNanos = saturatedAdd(nowNanos, MOVE_INTERVAL_NANOS);
            return new Transition(TransitionKind.MOVED, before, follower.snapshot());
        }
        follower.nextMoveNanos = saturatedAdd(nowNanos, 100_000_000L);
        return Transition.none();
    }

    synchronized Snapshot complete(String ownerIdentity) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null) return null;
        followers.remove(follower.objectId);
        return follower.snapshot();
    }

    synchronized boolean canComplete(String ownerIdentity, int ownerX, int ownerY,
                                     int baschX, int baschY) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        return follower != null
                && tileLineDistance(follower.x, follower.y, ownerX, ownerY) < 3
                && tileLineDistance(baschX, baschY, ownerX, ownerY) < 3;
    }

    /** Atomically admits and removes one arrival so concurrent clocks cannot reward twice. */
    synchronized Snapshot completeIfNear(String ownerIdentity, int ownerX, int ownerY,
                                         int baschX, int baschY) {
        if (!canComplete(ownerIdentity, ownerX, ownerY, baschX, baschY)) return null;
        return complete(ownerIdentity);
    }

    synchronized void restoreAfterFailedCompletion(Snapshot snapshot) {
        if (snapshot == null || !snapshot.owned()
                || followers.containsKey(snapshot.objectId())
                || ownedMutable(snapshot.ownerIdentity()) != null) {
            throw new IllegalStateException("cannot restore Roi follower completion");
        }
        followers.put(snapshot.objectId(), new MutableFollower(
                snapshot.objectId(), snapshot.x(), snapshot.y(), snapshot.heading(),
                snapshot.ownerIdentity(), snapshot.nextMoveNanos()));
    }

    synchronized int size() {
        return followers.size();
    }

    private MutableFollower ownedMutable(String ownerIdentity) {
        if (ownerIdentity.isEmpty()) return null;
        for (MutableFollower follower : followers.values()) {
            if (follower.ownerIdentity.equals(ownerIdentity)) return follower;
        }
        return null;
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

    private static String requireIdentity(String identity) {
        String normalized = normalizeIdentity(identity);
        if (normalized.isEmpty()) throw new IllegalArgumentException("owner identity is required");
        return normalized;
    }

    private static String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(java.util.Locale.ROOT);
    }

    static String normalizeIdentityForWire(String identity) {
        return normalizeIdentity(identity);
    }

    private static long saturatedAdd(long left, long right) {
        if (left < 0 || right < 0) throw new IllegalArgumentException("negative deadline input");
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }
}
