package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ModernBackendExecutorTest {
    @Test void timedOutRunningOperationRetainsWorkerAndSessionUntilActualExit() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var gate = executor.openSession();
        try {
            var request = clients.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> gate.call(180, scope -> { entered.countDown(); await(release); return "late"; })));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertEquals(ModernBackendExecutor.Failure.DEADLINE, request.get(2, TimeUnit.SECONDS).failure());
            assertTrue(gate.isBusy()); assertEquals(1, executor.activeCount()); assertFalse(executor.isQuiescent());
            assertEquals(ModernBackendExecutor.Failure.BUSY,
                    assertThrows(ModernBackendExecutor.BackendException.class, () -> gate.call(100, scope -> "no")).failure());
            release.countDown();
            until(executor::isQuiescent);
            assertFalse(gate.isBusy());
            assertEquals("fresh", gate.call(1000, scope -> "fresh"));
        } finally { release.countDown(); stop(executor, clients); }
    }

    @Test void boundedQueueRemovalAndRejectionDiscardOwnedSecretsExactlyOnce() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newFixedThreadPool(2);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var first = executor.openSession(); var queued = executor.openSession();
        var queuedWork = new SecretWork(); var rejectedWork = new SecretWork();
        try {
            var running = clients.submit(() -> first.call(2_000, scope -> { entered.countDown(); await(release); return "first"; }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var waiting = clients.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> queued.call(2_000, queuedWork)));
            until(() -> executor.queuedCount() == 1);
            assertEquals(ModernBackendExecutor.Failure.OVERLOADED,
                    assertThrows(ModernBackendExecutor.BackendException.class,
                            () -> executor.openSession().call(100, rejectedWork)).failure());
            assertEquals(1, rejectedWork.closed.get()); assertEquals(0, rejectedWork.ran.get());
            assertArrayEquals(new char[rejectedWork.secret.length], rejectedWork.secret);
            queued.close();
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, waiting.get(2, TimeUnit.SECONDS).failure());
            assertEquals(0, executor.queuedCount()); assertFalse(queued.isBusy());
            assertEquals(1, queuedWork.closed.get()); assertEquals(0, queuedWork.ran.get());
            assertArrayEquals(new char[queuedWork.secret.length], queuedWork.secret);
            release.countDown(); assertEquals("first", running.get(2, TimeUnit.SECONDS));
        } finally { release.countDown(); stop(executor, clients); }
    }

    @Test void cancellationAtQueueStartDoesNotDoubleCleanOrReleaseALiveWorker() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 20; iteration++) {
                var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
                var first = executor.openSession(); var next = executor.openSession();
                var work = new SecretWork();
                var running = clients.submit(() -> first.call(2_000, scope -> { entered.countDown(); await(release); return 1; }));
                try {
                    assertTrue(entered.await(2, TimeUnit.SECONDS));
                    var queued = clients.submit(() -> {
                        try { return next.call(2_000, work); }
                        catch (ModernBackendExecutor.BackendException expected) { return "cancelled"; }
                    });
                    until(() -> executor.queuedCount() == 1);
                    release.countDown(); next.close();
                    running.get(2, TimeUnit.SECONDS); queued.get(2, TimeUnit.SECONDS);
                    until(executor::isQuiescent);
                    assertEquals(1, work.closed.get()); assertTrue(work.ran.get() <= 1);
                    assertArrayEquals(new char[work.secret.length], work.secret);
                } finally { release.countDown(); first.close(); next.close(); }
            }
        } finally { stop(executor, clients); }
    }

    @Test void oneMonotonicBudgetSpansSourcesAndThreadLocalDoesNotCarryOver() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newSingleThreadExecutor();
        var firstScope = new AtomicReference<ModernBackendScope>();
        try {
            assertEquals(ModernBackendExecutor.Failure.DEADLINE,
                    assertThrows(ModernBackendExecutor.BackendException.class,
                            () -> executor.openSession().call(180, scope -> {
                                assertSame(scope, ModernBackendScope.current()); firstScope.set(scope);
                                int before = scope.remainingMillis(); Thread.sleep(80);
                                assertTrue(scope.remainingMillis() < before);
                                Thread.sleep(150); // Source two does not get a fresh 180 ms.
                                return "late";
                            })).failure());
            until(executor::isQuiescent);
            executor.openSession().call(1_000, scope -> {
                assertSame(scope, ModernBackendScope.current()); assertNotSame(firstScope.get(), scope); return 1;
            });
            assertThrows(IllegalStateException.class, ModernBackendScope::current);
        } finally { stop(executor, clients); }
    }

    @Test void closeRacingAdmissionNeverLeavesARetiredJobRegisteredOrSecretOwned() throws Exception {
        var executor = new ModernBackendExecutor(2, 2);
        var clients = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 100; iteration++) {
                var start = new CountDownLatch(1);
                var gate = executor.openSession(); var work = new SecretWork();
                var request = clients.submit(() -> {
                    await(start);
                    try { gate.call(1_000, work); }
                    catch (ModernBackendExecutor.BackendException expected) {
                        assertTrue(expected.failure() == ModernBackendExecutor.Failure.CLOSED
                                || expected.failure() == ModernBackendExecutor.Failure.CANCELLED);
                    }
                    return null;
                });
                var close = clients.submit(() -> { await(start); gate.close(); return null; });
                start.countDown(); request.get(2, TimeUnit.SECONDS); close.get(2, TimeUnit.SECONDS);
                until(executor::isQuiescent);
                assertFalse(gate.isBusy()); assertEquals(1, work.closed.get());
                assertArrayEquals(new char[work.secret.length], work.secret);
            }
        } finally { stop(executor, clients); }
    }

    @Test void shutdownCannotClaimTerminationWhileCancelledBackendStillRuns() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try {
            var request = clients.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> executor.openSession().call(2_000, scope -> { entered.countDown(); await(release); return "late"; })));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            executor.close(); request.get(2, TimeUnit.SECONDS);
            assertFalse(executor.awaitTermination(30)); assertFalse(executor.isQuiescent());
            assertEquals(1, executor.activeCount());
            release.countDown(); assertTrue(executor.awaitTermination(3_000)); assertTrue(executor.isQuiescent());
        } finally { release.countDown(); stop(executor, clients); }
    }

    @Test void failuresAreSanitizedAndCleanupAlsoRunsForInvalidOrClosedAdmission() throws Exception {
        var executor = new ModernBackendExecutor(1, 1);
        var clients = Executors.newSingleThreadExecutor();
        try {
            var failure = assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> executor.openSession().call(1_000, scope -> { throw new Exception("private password or JDBC URL"); }));
            assertEquals(ModernBackendExecutor.Failure.BACKEND, failure.failure()); assertNull(failure.getCause());
            assertFalse(failure.toString().contains("private"));
            var invalid = new SecretWork();
            assertThrows(IllegalArgumentException.class, () -> executor.openSession().call(0, invalid));
            assertEquals(1, invalid.closed.get());
            var gate = executor.openSession(); gate.close(); var closed = new SecretWork();
            assertEquals(ModernBackendExecutor.Failure.CLOSED,
                    assertThrows(ModernBackendExecutor.BackendException.class, () -> gate.call(100, closed)).failure());
            assertEquals(1, closed.closed.get()); assertArrayEquals(new char[closed.secret.length], closed.secret);
        } finally { stop(executor, clients); }
    }

    private static final class SecretWork implements ModernBackendExecutor.Work<String> {
        final char[] secret = "owned-fixture-secret".toCharArray();
        final AtomicInteger ran = new AtomicInteger(), closed = new AtomicInteger();
        @Override public String run(ModernBackendScope scope) { ran.incrementAndGet(); return "ok"; }
        @Override public void close() { closed.incrementAndGet(); Arrays.fill(secret, '\0'); }
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(3, TimeUnit.SECONDS)) throw new AssertionError("fixture release deadline");
    }
    private static void until(java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < end, "fixture state deadline"); Thread.sleep(1);
        }
    }
    private static void stop(ModernBackendExecutor executor, ExecutorService clients) throws Exception {
        executor.close(); clients.shutdownNow();
        assertTrue(clients.awaitTermination(3, TimeUnit.SECONDS));
        assertTrue(executor.awaitTermination(3_000), "backend workers cannot be detached at test exit");
    }
}
