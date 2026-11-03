package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Native map-4 escort state for {@code Npc_Searcher} and {@code Npc_Endia}.
 *
 * <p>The retained {@code L1FollowerInstance} removes the clicked quest actor,
 * creates one owner-bound follower in the same tile, and lets the zero-delay
 * static spawn return.  Owner death, map separation, or tile-line distance
 * greater than thirteen replaces the owned follower with a free quest actor
 * at its final tile.  The free actor can be claimed by another eligible
 * player, matching the retained follower constructor and respawn path.</p>
 */
final class ClassicWorldRescueFollowers {
    static final int MAP_ID = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
    static final int FOLLOW_DISTANCE = 2;
    static final int ABANDON_DISTANCE = 13;
    static final int ARRIVAL_DISTANCE_EXCLUSIVE = 3;

    static final int SEARCHER_SOURCE_NPC_ID = 71092;
    static final int SEARCHER_FOLLOWER_NPC_ID = 71093;
    static final int SEARCHER_CURSED_GFX = 1569;
    static final int SEARCHER_RESTORED_GFX = 1314;
    static final int SEARCHER_QUEST_ID = 10804;
    static final int SEARCHER_COMPLETION_EVENT_ID = 40000140;
    static final int SEARCHER_FREE_START_EVENT_ID = 40000141;
    static final int SEARCHER_FREE_COMPAT_START_EVENT_ID = 40000142;
    static final long SEARCHER_REWARD_ITEM_ID = 40593;

    static final int ENDIA_NPC_ID = 71094;
    static final int ENDIA_GFX = 3097;
    static final int ENDIA_QUEST_ID = 12701;
    static final int ENDIA_COMPLETION_EVENT_ID = 40000362;
    static final long ENDIA_REWARD_ITEM_ID = 40582;

    /** Source-documented target missing from selected spawnlist_npc. */
    static final int DUKE_SOLDIER_NPC_ID = 70740;
    static final int DUKE_SOLDIER_X = 34043;
    static final int DUKE_SOLDIER_Y = 32234;
    static final int LYRA_NPC_ID = 70811;

    private static final long FIRST_OBJECT_ID = 0x6F00_0000L;
    private static final long LAST_OBJECT_ID = 0x6FFF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);
    private static final int[] STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] STEP_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    enum Kind { SEARCHER, ENDIA }

    record Definition(
            Kind kind, int sourceNpcId, int followerNpcId, int gfxId,
            String nameId, int questId, int completionEventId,
            long rewardItemId, int targetNpcId, long moveIntervalNanos,
            String ownerHtml, String strangerHtml) {
        Definition {
            if (kind == null || sourceNpcId <= 0 || followerNpcId <= 0
                    || gfxId <= 0 || gfxId > 0xFFFF || nameId == null
                    || nameId.isBlank() || questId <= 0 || completionEventId <= 0
                    || rewardItemId <= 0 || targetNpcId <= 0
                    || moveIntervalNanos <= 0 || ownerHtml == null
                    || ownerHtml.isBlank() || strangerHtml == null
                    || strangerHtml.isBlank()) {
                throw new IllegalArgumentException("invalid rescue follower definition");
            }
        }
    }

    static final Definition SEARCHER = new Definition(
            Kind.SEARCHER, SEARCHER_SOURCE_NPC_ID, SEARCHER_FOLLOWER_NPC_ID,
            SEARCHER_RESTORED_GFX, "$2257", SEARCHER_QUEST_ID,
            SEARCHER_COMPLETION_EVENT_ID, SEARCHER_REWARD_ITEM_ID,
            DUKE_SOLDIER_NPC_ID, 1_280_000_000L, "searcherk2", "searcherk4");
    static final Definition ENDIA = new Definition(
            Kind.ENDIA, ENDIA_NPC_ID, ENDIA_NPC_ID, ENDIA_GFX, "$2659",
            ENDIA_QUEST_ID, ENDIA_COMPLETION_EVENT_ID, ENDIA_REWARD_ITEM_ID,
            LYRA_NPC_ID, 1_440_000_000L, "endiaq2", "endiaq4");

    @FunctionalInterface
    interface StepAdmission {
        boolean canStep(int fromX, int fromY, int toX, int toY, long movingObjectId);
    }

    enum TransitionKind { NONE, MOVED, ABANDONED }

    record Snapshot(long objectId, Definition definition, int mapId,
                    int x, int y, int heading, String ownerIdentity,
                    long nextMoveNanos) {
        Snapshot {
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || definition == null || mapId != MAP_ID
                    || x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                    || heading < 0 || heading > 7 || nextMoveNanos < 0) {
                throw new IllegalArgumentException("invalid rescue follower snapshot");
            }
            ownerIdentity = normalizeIdentity(ownerIdentity);
        }

        boolean owned() { return !ownerIdentity.isEmpty(); }
    }

    record Transition(TransitionKind kind, Snapshot before, Snapshot after) {
        Transition {
            if (kind == null) throw new IllegalArgumentException("transition kind is required");
            if (kind == TransitionKind.NONE && (before != null || after != null)) {
                throw new IllegalArgumentException("empty transition carries actors");
            }
            if (kind == TransitionKind.MOVED
                    && (before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.definition() != after.definition())) {
                throw new IllegalArgumentException("movement must retain one rescue actor");
            }
            if (kind == TransitionKind.ABANDONED
                    && (before == null || after == null || !before.owned()
                    || after.owned() || before.objectId() == after.objectId()
                    || before.definition() != after.definition())) {
                throw new IllegalArgumentException("abandonment must replace one rescue actor");
            }
        }

        static Transition none() { return new Transition(TransitionKind.NONE, null, null); }
    }

    private static final class MutableFollower {
        final long objectId;
        final Definition definition;
        int x;
        int y;
        int heading;
        String ownerIdentity;
        long nextMoveNanos;

        MutableFollower(long objectId, Definition definition, int x, int y,
                        int heading, String ownerIdentity, long nextMoveNanos) {
            this.objectId = objectId;
            this.definition = definition;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.ownerIdentity = normalizeIdentity(ownerIdentity);
            this.nextMoveNanos = nextMoveNanos;
        }

        Snapshot snapshot() {
            return new Snapshot(objectId, definition, MAP_ID, x, y, heading,
                    ownerIdentity, nextMoveNanos);
        }
    }

    private final Map<Long, MutableFollower> followers = new LinkedHashMap<>();

    static Definition sourceDefinition(ClassicWorldNpcCatalog.StaticNpc npc) {
        if (npc == null || npc.mapId() != MAP_ID) return null;
        return npc.npcId() == SEARCHER.sourceNpcId() ? SEARCHER
                : npc.npcId() == ENDIA.sourceNpcId() ? ENDIA : null;
    }

    static boolean admitsStartEvent(
            Definition definition, int actionNpcId, int eventId) {
        if (definition == null || eventId <= 0) return false;
        if (actionNpcId == definition.sourceNpcId()) {
            return eventId == definition.completionEventId();
        }
        return definition.kind() == Kind.SEARCHER
                && actionNpcId == SEARCHER_FOLLOWER_NPC_ID
                && (eventId == SEARCHER_FREE_START_EVENT_ID
                    || eventId == SEARCHER_FREE_COMPAT_START_EVENT_ID);
    }

    synchronized Snapshot claimStatic(Definition definition, String ownerIdentity,
                                      int x, int y, int heading, long nowNanos) {
        String owner = requireIdentity(ownerIdentity);
        if (definition == null || ownedMutable(owner) != null || nowNanos < 0) return null;
        MutableFollower follower = new MutableFollower(
                nextObjectId(), definition, x, y, heading, owner,
                saturatedAdd(nowNanos, definition.moveIntervalNanos()));
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
                nextObjectId(), source.definition, source.x, source.y,
                source.heading, owner,
                saturatedAdd(nowNanos, source.definition.moveIntervalNanos()));
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
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null) return Transition.none();
        Snapshot before = follower.snapshot();
        int range = tileLineDistance(follower.x, follower.y, ownerX, ownerY);
        if (!ownerAlive || ownerMapId != MAP_ID || range > ABANDON_DISTANCE) {
            followers.remove(follower.objectId);
            MutableFollower free = new MutableFollower(
                    nextObjectId(), follower.definition, follower.x, follower.y,
                    follower.heading, "", 0);
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
            if (tileLineDistance(nextX, nextY, ownerX, ownerY) > range) continue;
            follower.x = nextX;
            follower.y = nextY;
            follower.heading = candidate;
            follower.nextMoveNanos = saturatedAdd(
                    nowNanos, follower.definition.moveIntervalNanos());
            return new Transition(TransitionKind.MOVED, before, follower.snapshot());
        }
        follower.nextMoveNanos = saturatedAdd(nowNanos, 100_000_000L);
        return Transition.none();
    }

    synchronized Snapshot completeIfNear(String ownerIdentity, int ownerX, int ownerY,
                                         int targetX, int targetY) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null
                || tileLineDistance(follower.x, follower.y, ownerX, ownerY)
                    >= ARRIVAL_DISTANCE_EXCLUSIVE
                || tileLineDistance(targetX, targetY, ownerX, ownerY)
                    >= ARRIVAL_DISTANCE_EXCLUSIVE) {
            return null;
        }
        followers.remove(follower.objectId);
        return follower.snapshot();
    }

    synchronized Snapshot removeOwned(String ownerIdentity) {
        MutableFollower follower = ownedMutable(normalizeIdentity(ownerIdentity));
        if (follower == null) return null;
        followers.remove(follower.objectId);
        return follower.snapshot();
    }

    synchronized void restoreAfterFailedCompletion(Snapshot snapshot) {
        if (snapshot == null || !snapshot.owned()
                || followers.containsKey(snapshot.objectId())
                || ownedMutable(snapshot.ownerIdentity()) != null) {
            throw new IllegalStateException("cannot restore rescue follower completion");
        }
        followers.put(snapshot.objectId(), new MutableFollower(
                snapshot.objectId(), snapshot.definition(), snapshot.x(), snapshot.y(),
                snapshot.heading(), snapshot.ownerIdentity(), snapshot.nextMoveNanos()));
    }

    synchronized int size() { return followers.size(); }

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
        if (normalized.isEmpty()) throw new IllegalArgumentException("owner identity is required");
        return normalized;
    }

    private static String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
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
        if (left < 0 || right < 0) throw new IllegalArgumentException("negative deadline input");
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }
}
