package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldRescueFollowersTest {
    @Test
    void definitionsPinBothRetainedFollowerTemplatesAndCompletionRows() {
        assertEquals(71092, ClassicWorldRescueFollowers.SEARCHER.sourceNpcId());
        assertEquals(71093, ClassicWorldRescueFollowers.SEARCHER.followerNpcId());
        assertEquals(1314, ClassicWorldRescueFollowers.SEARCHER.gfxId());
        assertEquals(40000140,
                ClassicWorldRescueFollowers.SEARCHER.completionEventId());
        assertTrue(ClassicWorldRescueFollowers.admitsStartEvent(
                ClassicWorldRescueFollowers.SEARCHER, 71092, 40000140));
        assertTrue(ClassicWorldRescueFollowers.admitsStartEvent(
                ClassicWorldRescueFollowers.SEARCHER, 71093, 40000141));
        assertTrue(ClassicWorldRescueFollowers.admitsStartEvent(
                ClassicWorldRescueFollowers.SEARCHER, 71093, 40000142));
        assertFalse(ClassicWorldRescueFollowers.admitsStartEvent(
                ClassicWorldRescueFollowers.SEARCHER, 71093, 40000140));
        assertEquals(71094, ClassicWorldRescueFollowers.ENDIA.sourceNpcId());
        assertEquals(71094, ClassicWorldRescueFollowers.ENDIA.followerNpcId());
        assertEquals(3097, ClassicWorldRescueFollowers.ENDIA.gfxId());
        assertEquals(40000362,
                ClassicWorldRescueFollowers.ENDIA.completionEventId());

        InventoryCatalog.Definition pouch = InventoryCatalog.require(40582);
        assertEquals(1321, pouch.icon());
        assertEquals(3374, pouch.groundGfx());
        assertEquals(InventoryCatalog.Effect.ENDIA_POUCH, pouch.effect());
        assertEquals("$2654", InventoryCatalog.require(40583).name());
        assertEquals(1800, InventoryCatalog.require(40593).icon());
        assertEquals(3963, InventoryCatalog.require(40593).groundGfx());
    }

    @Test
    void zeroDelaySourcesGiveIndependentOwnersButOneFollowerPerOwner() {
        ClassicWorldRescueFollowers followers = new ClassicWorldRescueFollowers();
        ClassicWorldRescueFollowers.Snapshot alice = followers.claimStatic(
                ClassicWorldRescueFollowers.SEARCHER,
                "Server\0Alice", 34242, 33356, 0, 1);
        ClassicWorldRescueFollowers.Snapshot bob = followers.claimStatic(
                ClassicWorldRescueFollowers.ENDIA,
                "Server\0Bob", 32944, 32280, 5, 1);

        assertNotNull(alice);
        assertNotNull(bob);
        assertNotEquals(alice.objectId(), bob.objectId());
        assertNull(followers.claimStatic(ClassicWorldRescueFollowers.ENDIA,
                "server\0alice", 1, 1, 0, 2));
        assertEquals(2, followers.size());
    }

    @Test
    void eachTemplateUsesItsRetainedPassispeedAsMovementClock() {
        ClassicWorldRescueFollowers followers = new ClassicWorldRescueFollowers();
        ClassicWorldRescueFollowers.Snapshot searcher = followers.claimStatic(
                ClassicWorldRescueFollowers.SEARCHER,
                "Alice", 100, 100, 4, 0);

        assertEquals(ClassicWorldRescueFollowers.TransitionKind.NONE,
                followers.tick("Alice", 4, 105, 100, true,
                        1_279_999_999L, (a, b, c, d, id) -> true).kind());
        ClassicWorldRescueFollowers.Transition moved = followers.tick(
                "Alice", 4, 105, 100, true, 1_280_000_000L,
                (a, b, c, d, id) -> true);
        assertEquals(ClassicWorldRescueFollowers.TransitionKind.MOVED, moved.kind());
        assertEquals(searcher.objectId(), moved.after().objectId());
        assertEquals(101, moved.after().x());
        assertEquals(2, moved.after().heading());

        followers.removeOwned("Alice");
        followers.claimStatic(ClassicWorldRescueFollowers.ENDIA,
                "Alice", 100, 100, 4, 0);
        assertEquals(ClassicWorldRescueFollowers.TransitionKind.NONE,
                followers.tick("Alice", 4, 105, 100, true,
                        1_439_999_999L, (a, b, c, d, id) -> true).kind());
        assertEquals(ClassicWorldRescueFollowers.TransitionKind.MOVED,
                followers.tick("Alice", 4, 105, 100, true,
                        1_440_000_000L, (a, b, c, d, id) -> true).kind());
    }

    @Test
    void ownerLossReplacesFollowerWithClaimableFreeActorOfSameKind() {
        ClassicWorldRescueFollowers followers = new ClassicWorldRescueFollowers();
        ClassicWorldRescueFollowers.Snapshot source = followers.claimStatic(
                ClassicWorldRescueFollowers.ENDIA,
                "Alice", 100, 100, 4, 0);
        ClassicWorldRescueFollowers.Transition abandoned = followers.tick(
                "Alice", 4, 114, 100, true, 1,
                (a, b, c, d, id) -> true);

        assertEquals(ClassicWorldRescueFollowers.TransitionKind.ABANDONED,
                abandoned.kind());
        assertEquals(source.objectId(), abandoned.before().objectId());
        assertEquals(ClassicWorldRescueFollowers.Kind.ENDIA,
                abandoned.after().definition().kind());
        assertFalse(abandoned.after().owned());
        assertNull(followers.owned("Alice"));

        ClassicWorldRescueFollowers.Snapshot reclaimed = followers.claimFree(
                "Bob", abandoned.after().objectId(), 10);
        assertNotNull(reclaimed);
        assertTrue(reclaimed.owned());
        assertEquals("bob", reclaimed.ownerIdentity());
        assertEquals(ClassicWorldRescueFollowers.Kind.ENDIA,
                reclaimed.definition().kind());
    }

    @Test
    void completionRequiresBothRetainedStrictLessThanThreeDistances() {
        ClassicWorldRescueFollowers followers = new ClassicWorldRescueFollowers();
        followers.claimStatic(ClassicWorldRescueFollowers.SEARCHER,
                "Alice", 102, 100, 4, 0);

        assertNull(followers.completeIfNear("Alice", 99, 100, 96, 100));
        assertNotNull(followers.completeIfNear("Alice", 100, 100, 98, 100));
        assertEquals(0, followers.size());
    }

    @Test
    void nativeFollowerPacketMatchesRetainedFollowerPackTail() {
        ClassicWorldRescueFollowers followers = new ClassicWorldRescueFollowers();
        ClassicWorldRescueFollowers.Snapshot follower = followers.claimStatic(
                ClassicWorldRescueFollowers.SEARCHER,
                "Alice", 34242, 33356, 0, 0);
        Protocol.Reader in = new Protocol.Reader(
                Protocol.classicWorldRescueFollower(follower));

        assertEquals(Protocol.S_PUT_OBJECT, in.c());
        assertEquals(34242, in.h());
        assertEquals(33356, in.h());
        assertEquals(follower.objectId(), in.d());
        assertEquals(1314, in.h());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals(0, in.h());
        assertEquals("$2257", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals("", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0, in.remaining());
    }
}
