package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewInstancesTest {
    private static final long START = 7_000_000_000L;
    private static final List<ClassicWorldCkewInstances.Member> PARTY = List.of(
            member("S:Crown", 0), member("S:Knight", 1),
            member("S:Elf", 2), member("S:Wizard", 3));

    @Test
    void firstMemberAllocates100AndLaterMembersReuseTheSameShowId() {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission first =
                instances.admit("s:crown", PARTY, START);
        assertTrue(first.admitted());
        assertTrue(first.created());
        assertEquals(100, first.showId());
        assertEquals(START + 1_800_000_000_000L, first.deadlineNanos());
        assertTrue(instances.snapshot(100).joinedIdentities().isEmpty());
        assertTrue(instances.commitAdmission("s:crown", first));

        ClassicWorldCkewInstances.Admission second =
                instances.admit("S:Knight", PARTY, START + 10);
        assertTrue(second.admitted());
        assertFalse(second.created());
        assertEquals(100, second.showId());
        assertTrue(instances.commitAdmission("s:knight", second));
        assertEquals(100, instances.showId("s:elf"));
        assertEquals(Set.of("s:crown", "s:knight"),
                instances.snapshot(100).joinedIdentities());

        ClassicWorldCkewInstances.Admission repeat =
                instances.admit("S:CROWN", PARTY, START + 20);
        assertTrue(repeat.admitted());
        assertTrue(repeat.alreadyJoined());
    }

    @Test
    void invalidOrChangedPartyCannotBorrowAnExistingInstance() {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        assertEquals(ClassicWorldCkewInstances.AdmissionStatus.INVALID_PARTY,
                instances.admit("s:crown", PARTY.subList(0, 3), START).status());
        assertEquals(ClassicWorldCkewInstances.AdmissionStatus.INVALID_PARTY,
                instances.admit("s:crown", List.of(
                        member("S:Crown", 0), member("S:Knight", 1),
                        member("S:Elf", 2), member("S:Elf2", 2)), START).status());

        ClassicWorldCkewInstances.Admission admitted =
                instances.admit("s:crown", PARTY, START);
        assertTrue(admitted.admitted());
        assertTrue(instances.commitAdmission("s:crown", admitted));
        List<ClassicWorldCkewInstances.Member> changed = List.of(
                member("S:Crown", 0), member("S:Knight", 1),
                member("S:Elf", 2), member("S:OtherWizard", 3));
        assertEquals(ClassicWorldCkewInstances.AdmissionStatus.PARTY_CHANGED,
                instances.admit("s:knight", changed, START + 1).status());
    }

    @Test
    void outStopEndsAllJoinedMembersAndReleasesExpectedPartyMapping() {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission crown =
                instances.admit("s:crown", PARTY, START);
        ClassicWorldCkewInstances.Admission knight =
                instances.admit("s:knight", PARTY, START + 1);
        assertTrue(instances.commitAdmission("s:crown", crown));
        assertTrue(instances.commitAdmission("s:knight", knight));
        ClassicWorldCkewInstances.Ended ended =
                instances.leave("S:Knight", "map-leave");
        assertEquals(100, ended.showId());
        assertEquals(List.of("s:crown", "s:knight"), ended.joinedIdentities());
        assertEquals("map-leave", ended.reason());
        assertNull(instances.snapshot(100));
        assertEquals(-1, instances.showId("s:elf"));
        assertEquals(101, instances.admit("s:elf", PARTY, START + 2).showId());
    }

    @Test
    void timeoutUsesOneSharedCreationDeadlineAndRollbackIsAtomic() {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission first =
                instances.admit("s:crown", PARTY, START);
        instances.rollbackAdmission("s:crown", first);
        assertNull(instances.snapshot(100));
        assertEquals(-1, instances.showId("s:crown"));

        ClassicWorldCkewInstances.Admission next =
                instances.admit("s:crown", PARTY, START + 100);
        assertEquals(101, next.showId());
        assertTrue(instances.expire(next.deadlineNanos() - 1).isEmpty());
        List<ClassicWorldCkewInstances.Ended> expired =
                instances.expire(next.deadlineNanos());
        assertEquals(1, expired.size());
        assertEquals("timeout", expired.get(0).reason());
        assertNull(instances.snapshot(101));
    }

    @Test
    void invalidatedReservationNeverPersistsEnteredQuestState() throws Exception {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admission =
                instances.admit("s:crown", PARTY, START);
        assertTrue(admission.admitted());
        assertTrue(instances.end(admission.showId(), "concurrent-timeout") != null);
        boolean[] persisted = {false};

        assertFalse(instances.commitAdmission(
                "s:crown", admission, () -> persisted[0] = true));
        assertFalse(persisted[0],
                "durable quest mutation must run only for a live reservation");
        assertNull(instances.snapshot(admission.showId()));
    }

    @Test
    void durableFailureLeavesReservationRollbackableAndNoJoinedMember()
            throws Exception {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admission =
                instances.admit("s:crown", PARTY, START);

        IOException failure = assertThrows(IOException.class,
                () -> instances.commitAdmission(
                        "s:crown", admission,
                        () -> { throw new IOException("fixture persistence failure"); }));
        assertEquals("fixture persistence failure", failure.getMessage());
        assertTrue(instances.snapshot(admission.showId())
                .joinedIdentities().isEmpty());
        instances.rollbackAdmission("s:crown", admission);
        assertNull(instances.snapshot(admission.showId()));
        assertEquals(-1, instances.showId("s:crown"));
    }

    @Test
    void timeoutCannotSplitDurableQuestMutationFromMembershipCommit()
            throws Exception {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admission =
                instances.admit("s:crown", PARTY, START);
        CountDownLatch durableStarted = new CountDownLatch(1);
        CountDownLatch releaseDurable = new CountDownLatch(1);
        CountDownLatch expiryAttempted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> committed = workers.submit(() ->
                    instances.commitAdmission("s:crown", admission, () -> {
                        durableStarted.countDown();
                        try {
                            if (!releaseDurable.await(5, TimeUnit.SECONDS)) {
                                throw new IOException("durable fixture timed out");
                            }
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IOException(
                                    "durable fixture interrupted", interrupted);
                        }
                    }));
            assertTrue(durableStarted.await(5, TimeUnit.SECONDS));
            Future<List<ClassicWorldCkewInstances.Ended>> expired =
                    workers.submit(() -> {
                        expiryAttempted.countDown();
                        return instances.expire(admission.deadlineNanos());
                    });
            assertTrue(expiryAttempted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> expired.get(100, TimeUnit.MILLISECONDS),
                    "expiry must wait until durable save and join are indivisible");

            releaseDurable.countDown();
            assertTrue(committed.get(5, TimeUnit.SECONDS));
            List<ClassicWorldCkewInstances.Ended> ended =
                    expired.get(5, TimeUnit.SECONDS);
            assertEquals(1, ended.size());
            assertEquals(List.of("s:crown"), ended.get(0).joinedIdentities());
            assertNull(instances.snapshot(admission.showId()));
        } finally {
            releaseDurable.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void invalidatedCommittedAdmissionCannotPublishPortal() throws Exception {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admission =
                instances.admit("s:crown", PARTY, START);
        assertTrue(instances.commitAdmission("s:crown", admission));
        assertTrue(instances.end(admission.showId(), "concurrent-timeout") != null);
        boolean[] published = {false};

        assertFalse(instances.publishAdmission(
                "s:crown", admission, () -> published[0] = true));
        assertFalse(published[0],
                "an ended show must suppress effect and S_PORTAL publication");
    }

    @Test
    void timeoutCannotSplitLiveCheckFromPortalPublication() throws Exception {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admission =
                instances.admit("s:crown", PARTY, START);
        assertTrue(instances.commitAdmission("s:crown", admission));
        CountDownLatch publicationStarted = new CountDownLatch(1);
        CountDownLatch releasePublication = new CountDownLatch(1);
        CountDownLatch expiryAttempted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> published = workers.submit(() ->
                    instances.publishAdmission("s:crown", admission, () -> {
                        publicationStarted.countDown();
                        try {
                            if (!releasePublication.await(5, TimeUnit.SECONDS)) {
                                throw new IOException("publication fixture timed out");
                            }
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IOException(
                                    "publication fixture interrupted", interrupted);
                        }
                    }));
            assertTrue(publicationStarted.await(5, TimeUnit.SECONDS));
            Future<List<ClassicWorldCkewInstances.Ended>> expired =
                    workers.submit(() -> {
                        expiryAttempted.countDown();
                        return instances.expire(admission.deadlineNanos());
                    });
            assertTrue(expiryAttempted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> expired.get(100, TimeUnit.MILLISECONDS),
                    "expiry must wait for the complete effect plus S_PORTAL publish");

            releasePublication.countDown();
            assertTrue(published.get(5, TimeUnit.SECONDS));
            List<ClassicWorldCkewInstances.Ended> ended =
                    expired.get(5, TimeUnit.SECONDS);
            assertEquals(1, ended.size());
            assertEquals(List.of("s:crown"), ended.get(0).joinedIdentities());
            assertNull(instances.snapshot(admission.showId()));
        } finally {
            releasePublication.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void emitsEveryRetainedCountdownThresholdOnceToCurrentMembers() {
        ClassicWorldCkewInstances instances = new ClassicWorldCkewInstances();
        ClassicWorldCkewInstances.Admission admitted =
                instances.admit("s:crown", PARTY, START);
        assertTrue(instances.commitAdmission("s:crown", admitted));
        int[] thresholds = {
                1800, 900, 600, 300, 240, 180, 120, 60,
                30, 15, 10, 5, 4, 3, 2, 1
        };
        long deadline = START + 1_800_000_000_000L;
        for (int threshold : thresholds) {
            List<ClassicWorldCkewInstances.Countdown> notices =
                    instances.countdowns(
                            deadline - threshold * 1_000_000_000L);
            assertEquals(1, notices.size());
            assertEquals(threshold, notices.get(0).remainingSeconds());
            assertEquals(Set.of("s:crown"),
                    notices.get(0).joinedIdentities());
            assertTrue(instances.countdowns(
                    deadline - threshold * 1_000_000_000L).isEmpty());
        }
    }

    private static ClassicWorldCkewInstances.Member member(String id, int type) {
        return new ClassicWorldCkewInstances.Member(id, type);
    }
}
