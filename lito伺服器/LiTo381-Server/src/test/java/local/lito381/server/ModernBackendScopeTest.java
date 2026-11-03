package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernBackendScopeTest {
    @Test void retiredConnectionLeaseCannotAbortTheNextBorrowerEvenIfOldCallbackWasCaptured() throws Exception {
        var scheduler = new CapturedAborts(); var aborts = new AtomicInteger();
        Connection sharedPhysical = connection(() -> aborts.incrementAndGet());
        var oldScope = scope(scheduler); var oldLease = oldScope.register(sharedPhysical);
        oldScope.cancel(ModernBackendExecutor.Failure.CANCELLED);
        Runnable alreadyCaptured = scheduler.actions.get(0);
        oldLease.close();
        var newScope = scope(scheduler); var newLease = newScope.register(sharedPhysical);
        alreadyCaptured.run();
        assertEquals(0, aborts.get()); newScope.checkActive();
        newScope.cancel(ModernBackendExecutor.Failure.CANCELLED);
        scheduler.actions.get(scheduler.actions.size() - 1).run();
        assertEquals(1, aborts.get());
        newLease.close(); oldScope.finish(); newScope.finish();
    }

    @Test void leaseRetirementWaitsForAnAbortThatReallyStarted() throws Exception {
        var scheduler = new CapturedAborts(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var scope = scope(scheduler);
        var lease = scope.register(connection(() -> {
            entered.countDown();
            try { assertTrue(release.await(3, TimeUnit.SECONDS)); }
            catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
        }));
        var threads = Executors.newFixedThreadPool(2);
        try {
            scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            var abort = threads.submit(scheduler.actions.get(0));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var retired = threads.submit(lease::close);
            assertThrows(java.util.concurrent.TimeoutException.class, () -> retired.get(40, TimeUnit.MILLISECONDS));
            release.countDown(); abort.get(2, TimeUnit.SECONDS); retired.get(2, TimeUnit.SECONDS);
            scope.finish();
        } finally { release.countDown(); threads.shutdownNow(); assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void cancellationClosesRawSocketBeforeItSchedulesJdbcAbort() throws Exception {
        var events = new ArrayList<String>();
        var scheduler = new CapturedAborts() {
            @Override public boolean execute(Runnable action) { events.add("queue-abort"); return super.execute(action); }
        };
        var scope = scope(scheduler);
        Socket raw = new Socket() { @Override public synchronized void close() { events.add("close-raw"); } };
        var socketLease = scope.registerSocket(raw);
        var connectionLease = scope.register(connection(() -> events.add("abort-jdbc")));
        scope.cancel(ModernBackendExecutor.Failure.DEADLINE);
        assertEquals(List.of("close-raw", "queue-abort"), events);
        scheduler.actions.get(0).run(); assertEquals("abort-jdbc", events.get(2));
        connectionLease.close(); socketLease.close(); scope.finish();
    }

    @Test void rawSocketLeaseRetirementAndUnreleasedLeaseCleanupAreBoundedByOwnership() throws Exception {
        var scheduler = new CapturedAborts(); var oldCloses = new AtomicInteger();
        var scope = scope(scheduler);
        var old = scope.registerSocket(new Socket() { @Override public void close() { oldCloses.incrementAndGet(); } });
        old.close();
        var aborts = new AtomicInteger(); var currentCloses = new AtomicInteger();
        scope.registerSocket(new Socket() { @Override public void close() { currentCloses.incrementAndGet(); } });
        scope.register(connection(() -> aborts.incrementAndGet()));
        assertThrows(IllegalStateException.class, () -> scope.register(connection(() -> {})));
        scope.finish(); // Forgot resources: force owned cleanup and invalidate success.
        assertEquals(0, oldCloses.get()); assertTrue(currentCloses.get() >= 1); assertEquals(1, aborts.get());
        assertEquals(ModernBackendExecutor.Failure.BACKEND, scope.cancellation());
        assertThrows(ModernBackendExecutor.BackendException.class, scope::checkActive);
        for (Runnable action : scheduler.actions) action.run();
        assertEquals(1, aborts.get(), "captured callbacks cannot re-abort retired ownership");
    }

    @Test void expiredScopeCannotRegisterNewBorrowersAndNeverReturnsInfiniteZeroTimeout() throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() - 1, new CapturedAborts());
        assertEquals(ModernBackendExecutor.Failure.DEADLINE,
                assertThrows(ModernBackendExecutor.BackendException.class, scope::remainingMillis).failure());
        assertThrows(ModernBackendExecutor.BackendException.class, () -> scope.registerSocket(new Socket()));
        assertThrows(ModernBackendExecutor.BackendException.class, () -> scope.register(connection(() -> {})));
        assertTrue(scope(new CapturedAborts()).remainingMillis() > 0);
    }
    private static ModernBackendScope scope(ModernBackendScope.AbortScheduler scheduler) {
        return new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(5), scheduler);
    }
    private static class CapturedAborts implements ModernBackendScope.AbortScheduler {
        final List<Runnable> actions = new ArrayList<>();
        @Override public boolean execute(Runnable action) { actions.add(action); return true; }
        // Keep a captured runnable deliberately: it might already have left a real queue.
        @Override public void remove(Runnable action) {}
    }
    private static Connection connection(Runnable abort) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("abort")) { ((java.util.concurrent.Executor) arguments[0]).execute(abort); return null; }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException("unexpected fixture JDBC operation");
                });
    }
}
