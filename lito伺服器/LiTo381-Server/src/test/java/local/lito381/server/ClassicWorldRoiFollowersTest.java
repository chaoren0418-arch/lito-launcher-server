package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldRoiFollowersTest {
    @Test
    void zeroDelaySourceSupportsIndependentPerOwnerFollowers() {
        ClassicWorldRoiFollowers followers = new ClassicWorldRoiFollowers();
        ClassicWorldRoiFollowers.Snapshot first = followers.claimStatic(
                "Server\0Alice", 32823, 32895, 4, 1);
        ClassicWorldRoiFollowers.Snapshot second = followers.claimStatic(
                "Server\0Bob", 32823, 32895, 4, 1);

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.objectId(), second.objectId());
        assertEquals(2, followers.size());
        assertNull(followers.claimStatic("server\0alice", 1, 1, 0, 2));
    }

    @Test
    void followsAfter640MillisecondsAndRetainsObjectId() {
        ClassicWorldRoiFollowers followers = new ClassicWorldRoiFollowers();
        ClassicWorldRoiFollowers.Snapshot source = followers.claimStatic(
                "Alice", 100, 100, 4, 0);

        assertEquals(ClassicWorldRoiFollowers.TransitionKind.NONE,
                followers.tick("Alice", 410, 105, 100, true,
                        639_999_999L, (a, b, c, d, id) -> true).kind());
        ClassicWorldRoiFollowers.Transition moved = followers.tick(
                "Alice", 410, 105, 100, true, 640_000_000L,
                (a, b, c, d, id) -> true);

        assertEquals(ClassicWorldRoiFollowers.TransitionKind.MOVED, moved.kind());
        assertEquals(source.objectId(), moved.after().objectId());
        assertEquals(101, moved.after().x());
        assertEquals(100, moved.after().y());
        assertEquals(2, moved.after().heading());
    }

    @Test
    void ownerLossReplacesFollowerWithClaimableFreeRoi() {
        ClassicWorldRoiFollowers followers = new ClassicWorldRoiFollowers();
        ClassicWorldRoiFollowers.Snapshot source = followers.claimStatic(
                "Alice", 100, 100, 4, 0);
        ClassicWorldRoiFollowers.Transition abandoned = followers.tick(
                "Alice", 410, 114, 100, true, 1,
                (a, b, c, d, id) -> true);

        assertEquals(ClassicWorldRoiFollowers.TransitionKind.ABANDONED,
                abandoned.kind());
        assertEquals(source.objectId(), abandoned.before().objectId());
        assertNotEquals(source.objectId(), abandoned.after().objectId());
        assertFalse(abandoned.after().owned());
        assertNull(followers.owned("Alice"));

        ClassicWorldRoiFollowers.Snapshot reclaimed = followers.claimFree(
                "Bob", abandoned.after().objectId(), 10);
        assertNotNull(reclaimed);
        assertTrue(reclaimed.owned());
        assertNotEquals(abandoned.after().objectId(), reclaimed.objectId());
        assertEquals("bob", reclaimed.ownerIdentity());
    }

    @Test
    void completionUsesRetainedStrictLessThanThreeDistances() {
        ClassicWorldRoiFollowers followers = new ClassicWorldRoiFollowers();
        followers.claimStatic("Alice", 102, 100, 4, 0);

        assertTrue(followers.canComplete("Alice", 100, 100, 98, 100));
        assertFalse(followers.canComplete("Alice", 99, 100, 96, 100));
        assertNotNull(followers.complete("Alice"));
        assertEquals(0, followers.size());
    }

    @Test
    void nativeFollowerPacketMatchesRetainedActorTail() throws Exception {
        ClassicWorldRoiFollowers followers = new ClassicWorldRoiFollowers();
        ClassicWorldRoiFollowers.Snapshot follower = followers.claimStatic(
                "Alice", 32823, 32895, 4, 0);
        Protocol.Reader in = new Protocol.Reader(
                Protocol.classicWorldRoiFollower(follower));

        assertEquals(Protocol.S_PUT_OBJECT, in.c());
        assertEquals(32823, in.h());
        assertEquals(32895, in.h());
        assertEquals(follower.objectId(), in.d());
        assertEquals(4310, in.h());
        assertEquals(0, in.c());
        assertEquals(4, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals(0, in.h());
        assertEquals("$3420", in.s());
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
