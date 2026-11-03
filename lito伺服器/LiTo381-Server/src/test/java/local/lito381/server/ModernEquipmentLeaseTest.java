package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ModernEquipmentLeaseTest {
    private static final TournamentPresence.CharacterKey KEY =
            new TournamentPresence.CharacterKey(42, 5_000_003_333L);

    @Test void issuerAndOwnerAreOpaqueCapabilitiesNotNumericEpochCredentials() {
        var issuer = new ModernEquipmentLease.Issuer();
        var otherIssuer = new ModernEquipmentLease.Issuer();
        Object owner = new String("equal-but-not-identical");
        var lease = issuer.open(owner, KEY, -1, () -> {});
        assertTrue(issuer.owns(lease)); assertFalse(otherIssuer.owns(lease)); assertFalse(issuer.owns(null));
        assertTrue(lease.matches(owner, KEY, -1));
        assertFalse(lease.matches(new String("equal-but-not-identical"), KEY, -1));
        assertFalse(lease.matches(owner, new TournamentPresence.CharacterKey(7, KEY.objectId()), -1));
        assertFalse(lease.matches(owner, new TournamentPresence.CharacterKey(42, 3333), -1));
        assertFalse(lease.matches(owner, KEY, 1));
        assertEquals(KEY, lease.key()); assertEquals(-1, lease.selectionEpoch());
        var wide = issuer.open(new Object(), new TournamentPresence.CharacterKey(Integer.MAX_VALUE, Long.MAX_VALUE),
                Long.MIN_VALUE, () -> {});
        assertEquals(Long.MAX_VALUE, wide.key().objectId());
        assertEquals(Long.MIN_VALUE, wide.selectionEpoch());
        assertThrows(IllegalArgumentException.class, () -> issuer.open(owner, KEY, 0, () -> {}));
        assertThrows(NullPointerException.class, () -> issuer.open(null, KEY, 1, () -> {}));
        assertThrows(NullPointerException.class, () -> issuer.open(owner, null, 1, () -> {}));
        assertThrows(NullPointerException.class, () -> issuer.open(owner, KEY, 1, null));
        assertEquals("ModernEquipmentLease[redacted]", lease.toString());
        lease.close(); wide.close();
    }

    @Test void oneOperationAndOneAdmissionWithStaleCloseUnableToReleaseItsSuccessor() throws Exception {
        var lease = lease(new AtomicInteger());
        var firstScope = scope(); var secondScope = scope();
        var first = lease.operation(firstScope);
        assertSame(firstScope, first.scope()); assertEquals(KEY, first.key());
        assertTrue(first.matchesKey(KEY));
        assertFalse(first.matchesKey(new TournamentPresence.CharacterKey(7, KEY.objectId())));
        assertFalse(first.matchesKey(null));
        assertThrows(IllegalStateException.class, () -> lease.operation(secondScope));
        assertTrue(first.tryCommit()); assertFalse(first.tryCommit());
        first.close(); assertFalse(first.isCommitAdmitted()); assertFalse(first.tryCommit());
        var second = lease.operation(secondScope);
        assertTrue(second.tryCommit()); first.close();
        assertTrue(second.isCommitAdmitted()); assertTrue(lease.hasActivePermit());
        assertEquals("ModernEquipmentOperation[redacted]", second.toString());
        second.close(); lease.close();
        assertTrue(lease.isRetired());
    }

    @Test void invalidateBeforeAdmissionRetiresImmediatelyAndCannotCommitLater() throws Exception {
        var retired = new AtomicInteger(); var lease = lease(retired);
        var operation = lease.operation(scope());
        lease.invalidate();
        assertTrue(lease.isInvalidated()); assertTrue(lease.isRetired());
        assertEquals(1, retired.get()); assertFalse(operation.tryCommit());
        assertThrows(IllegalStateException.class, () -> lease.operation(scope()));
        operation.close(); operation.close(); lease.close();
        assertEquals(1, retired.get());
    }

    @Test void admittedPermitSurvivesInvalidateUntilActualOperationFinally() throws Exception {
        var retired = new AtomicInteger(); var lease = lease(retired);
        var operation = lease.operation(scope());
        assertTrue(operation.tryCommit());
        lease.invalidate(); lease.close();
        assertTrue(operation.isCommitAdmitted()); assertTrue(lease.hasActivePermit());
        assertFalse(lease.isRetired()); assertEquals(0, retired.get());
        // A successful rename OR an exception after admission uses the same
        // worker-finally close, not an eager store-local permit release.
        operation.close();
        assertFalse(lease.hasActivePermit()); assertTrue(lease.isRetired());
        assertEquals(1, retired.get());
    }

    @Test void conditionalBackWithAdmittedPermitLeavesTheLeaseEntirelyUsable() throws Exception {
        var retired = new AtomicInteger(); var owner = new Object();
        var lease = new ModernEquipmentLease.Issuer().open(owner, KEY, 9, retired::incrementAndGet);
        var operation = lease.operation(scope()); assertTrue(operation.tryCommit());
        assertFalse(lease.tryInvalidateIfNoPermit()); assertFalse(lease.tryInvalidateIfNoPermit());
        assertFalse(lease.isInvalidated()); assertFalse(lease.isRetired()); assertEquals(0, retired.get());
        assertTrue(lease.matches(owner, KEY, 9)); assertTrue(operation.isCommitAdmitted());
        operation.close();
        // Busy BACK is not a latent invalidation: another real operation may
        // run before a later BACK is accepted.
        var next = lease.operation(scope()); assertTrue(next.tryCommit()); next.close();
        assertTrue(lease.tryInvalidateIfNoPermit()); assertTrue(lease.tryInvalidateIfNoPermit());
        assertTrue(lease.isInvalidated()); assertTrue(lease.isRetired()); assertEquals(1, retired.get());
    }

    @Test void conditionalBackBeforeAdmissionRetiresOnceAndDeniesLatePreparation() throws Exception {
        var retired = new AtomicInteger(); var lease = lease(retired);
        var preparing = lease.operation(scope());
        assertTrue(lease.tryInvalidateIfNoPermit()); assertTrue(lease.isRetired());
        assertFalse(preparing.tryCommit()); assertEquals(1, retired.get());
        preparing.close(); lease.invalidate(); assertTrue(lease.tryInvalidateIfNoPermit());
        assertEquals(1, retired.get());
    }

    @Test void conditionalBackAndCommitHaveBothDeterministicNonblockingLinearizations() throws Exception {
        for (boolean admitFirst : new boolean[] {false, true}) {
            var retired = new AtomicInteger(); var lease = lease(retired);
            var prepared = new CountDownLatch(1); var proceed = new CountDownLatch(1);
            var threads = Executors.newFixedThreadPool(2);
            try {
                var worker = threads.submit(() -> {
                    var operation = lease.operation(scope());
                    try {
                        boolean admitted = admitFirst && operation.tryCommit();
                        prepared.countDown(); await(proceed);
                        return admitFirst ? admitted : operation.tryCommit();
                    } finally { operation.close(); }
                });
                assertTrue(prepared.await(2, TimeUnit.SECONDS));
                assertEquals(!admitFirst, threads.submit(lease::tryInvalidateIfNoPermit).get(1, TimeUnit.SECONDS));
                assertEquals(!admitFirst, lease.isInvalidated());
                assertEquals(admitFirst ? 0 : 1, retired.get());
                proceed.countDown(); assertEquals(admitFirst, worker.get(2, TimeUnit.SECONDS));
                assertTrue(lease.tryInvalidateIfNoPermit()); assertEquals(1, retired.get());
            } finally {
                proceed.countDown(); lease.invalidate(); threads.shutdown();
                assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS));
            }
        }
    }

    @Test void conditionalBackCannotClaimRetirementWhileCallbackIsRunningOrAfterItFailed() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var calls = new AtomicInteger(); var threads = Executors.newSingleThreadExecutor();
        var lease = new ModernEquipmentLease.Issuer().open(new Object(), KEY, 9, () -> {
            calls.incrementAndGet(); entered.countDown();
            try { await(finish); }
            catch (InterruptedException failed) { Thread.currentThread().interrupt(); throw new AssertionError(failed); }
        });
        try {
            var closing = threads.submit(lease::invalidate);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var failure = assertThrows(IllegalStateException.class, lease::tryInvalidateIfNoPermit);
            assertEquals("equipment lease retirement unavailable", failure.getMessage()); assertNull(failure.getCause());
            assertFalse(lease.isRetired()); finish.countDown(); closing.get(2, TimeUnit.SECONDS);
            assertTrue(lease.tryInvalidateIfNoPermit()); assertEquals(1, calls.get());
        } finally {
            finish.countDown(); threads.shutdown(); assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS));
        }
        var failed = new ModernEquipmentLease.Issuer().open(new Object(), KEY, 9,
                () -> { throw new IllegalArgumentException("private callback data"); });
        assertThrows(IllegalStateException.class, failed::tryInvalidateIfNoPermit);
        assertTrue(failed.isInvalidated()); assertTrue(failed.retirementFailed()); assertFalse(failed.isRetired());
        var repeated = assertThrows(IllegalStateException.class, failed::tryInvalidateIfNoPermit);
        assertEquals("equipment lease retirement unavailable", repeated.getMessage()); assertNull(repeated.getCause());
    }

    @Test void bothLatchOrderedInvalidateAndAdmissionOutcomesAreLinearized() throws Exception {
        for (boolean admitFirst : new boolean[] {false, true}) {
            var retired = new AtomicInteger(); var lease = lease(retired);
            var prepared = new CountDownLatch(1); var proceed = new CountDownLatch(1);
            var threads = Executors.newFixedThreadPool(2);
            try {
                var running = threads.submit(() -> {
                    var operation = lease.operation(scope());
                    try {
                        boolean admitted = admitFirst && operation.tryCommit();
                        prepared.countDown(); await(proceed);
                        return admitFirst ? admitted : operation.tryCommit();
                    } finally { operation.close(); }
                });
                assertTrue(prepared.await(2, TimeUnit.SECONDS));
                // invalidate must return without waiting for the simulated disk
                // worker, which cannot proceed until this caller releases it.
                threads.submit(lease::invalidate).get(1, TimeUnit.SECONDS);
                assertEquals(admitFirst ? 0 : 1, retired.get());
                assertEquals(!admitFirst, lease.isRetired());
                proceed.countDown(); assertEquals(admitFirst, running.get(2, TimeUnit.SECONDS));
                assertTrue(lease.isRetired()); assertEquals(1, retired.get());
            } finally {
                proceed.countDown(); lease.invalidate(); threads.shutdown();
                assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS));
            }
        }
    }

    @Test void scopeCancellationIsSeparateAndDoesNotPretendToUndoAdmittedCommit() throws Exception {
        var retired = new AtomicInteger(); var lease = lease(retired);
        var cancelled = scope(); cancelled.cancel(ModernBackendExecutor.Failure.CANCELLED);
        assertThrows(ModernBackendExecutor.BackendException.class, () -> lease.operation(cancelled));
        var before = scope(); var preparation = lease.operation(before);
        before.cancel(ModernBackendExecutor.Failure.DEADLINE);
        assertEquals(ModernBackendExecutor.Failure.DEADLINE,
                assertThrows(ModernBackendExecutor.BackendException.class, preparation::tryCommit).failure());
        assertFalse(preparation.isCommitAdmitted()); preparation.close();
        var after = scope(); var admitted = lease.operation(after);
        assertTrue(admitted.tryCommit()); after.cancel(ModernBackendExecutor.Failure.CANCELLED);
        // Scope cancellation alone does not call invalidate or release an owner.
        assertFalse(lease.isInvalidated()); assertTrue(admitted.isCommitAdmitted());
        lease.invalidate(); assertEquals(0, retired.get());
        admitted.close(); assertEquals(1, retired.get());
    }

    @Test void retirementCallbackIsOutsideMonitorReentrantAndRunsOnce() throws Exception {
        var holder = new AtomicReference<ModernEquipmentLease>();
        var retired = new AtomicInteger();
        var lease = new ModernEquipmentLease.Issuer().open(new Object(), KEY, 1, () -> {
            var selected = holder.get();
            assertFalse(Thread.holdsLock(selected));
            assertFalse(selected.isRetired(), "callback has not completed yet");
            selected.invalidate(); retired.incrementAndGet();
        });
        holder.set(lease);
        var operation = lease.operation(scope()); assertTrue(operation.tryCommit());
        lease.invalidate(); operation.close(); lease.invalidate(); operation.close();
        assertTrue(lease.isRetired()); assertEquals(1, retired.get());
    }

    @Test void failedRetirementIsNotReportedAsReleasedOrRetriedWithPrivateCause() {
        var calls = new AtomicInteger();
        var lease = new ModernEquipmentLease.Issuer().open(new Object(), KEY, 1, () -> {
            calls.incrementAndGet(); throw new IllegalArgumentException("private callback data");
        });
        var failure = assertThrows(IllegalStateException.class, lease::invalidate);
        assertEquals("equipment lease retirement failed", failure.getMessage()); assertNull(failure.getCause());
        assertTrue(lease.retirementFailed()); assertFalse(lease.isRetired());
        lease.invalidate(); lease.close(); assertEquals(1, calls.get());
    }

    @Test void cancelledBackendCallerRetainsPermitUntilOwnedWorkCloseActuallyRuns() throws Exception {
        var executor = new ModernBackendExecutor(1, 0);
        var callers = Executors.newSingleThreadExecutor();
        var gate = executor.openSession();
        var retired = new AtomicInteger(); var lease = lease(retired);
        var admitted = new CountDownLatch(1); var release = new CountDownLatch(1);
        var holder = new AtomicReference<ModernEquipmentLease.Operation>();
        var cleanupCalls = new AtomicInteger();
        try {
            var result = callers.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> gate.call(2_500, new ModernBackendExecutor.Work<Integer>() {
                        @Override public Integer run(ModernBackendScope scope) throws Exception {
                            var operation = lease.operation(scope); holder.set(operation);
                            assertTrue(operation.tryCommit()); admitted.countDown();
                            await(release); return 1;
                        }
                        @Override public void close() {
                            cleanupCalls.incrementAndGet();
                            var operation = holder.get(); if (operation != null) operation.close();
                        }
                    })));
            assertTrue(admitted.await(2, TimeUnit.SECONDS));
            gate.close(); lease.invalidate();
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, result.get(1, TimeUnit.SECONDS).failure());
            assertEquals(0, cleanupCalls.get()); assertEquals(0, retired.get());
            assertTrue(lease.hasActivePermit()); assertFalse(lease.isRetired()); assertTrue(gate.isBusy());
            release.countDown(); executor.close();
            assertTrue(executor.awaitTermination(3_000));
            assertEquals(1, cleanupCalls.get()); assertEquals(1, retired.get()); assertTrue(lease.isRetired());
            assertFalse(gate.isBusy()); assertTrue(executor.isQuiescent());
        } finally {
            release.countDown(); gate.close(); lease.invalidate(); executor.close(); callers.shutdown();
            assertTrue(executor.awaitTermination(3_000)); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    private static ModernEquipmentLease lease(AtomicInteger retired) {
        return new ModernEquipmentLease.Issuer().open(new Object(), KEY, 9, retired::incrementAndGet);
    }
    private static ModernBackendScope scope() {
        return new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10),
                new ModernBackendScope.AbortScheduler() {
                    @Override public boolean execute(Runnable action) { return false; }
                    @Override public void remove(Runnable action) {}
                });
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(3, TimeUnit.SECONDS)) throw new AssertionError("equipment lease test barrier timeout");
    }
}
