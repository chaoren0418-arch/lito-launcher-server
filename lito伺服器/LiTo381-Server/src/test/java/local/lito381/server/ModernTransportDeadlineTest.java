package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernTransportDeadlineTest {
    private static final long MILLIS = 1_000_000L;
    private static final class Fixture {
        final AtomicLong clock = new AtomicLong();
        final AtomicInteger closes = new AtomicInteger(), cancellations = new AtomicInteger();
        Runnable scheduled;
        ModernTransportDeadline open() {
            return new ModernTransportDeadline(100, clock::get, (task, milliseconds) -> {
                assertEquals(100, milliseconds);
                scheduled = task;
                return () -> cancellations.incrementAndGet();
            }, closes::incrementAndGet, () -> false);
        }
    }

    @Test void alreadyDispatchedWatchdogCannotCloseACompletedOperationOrItsNextOperation() throws Exception {
        var f = new Fixture();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var worker = Executors.newSingleThreadExecutor();
        try (var deadline = f.open()) {
            Runnable dispatched = f.scheduled;
            f.clock.set(99 * MILLIS);
            deadline.complete();
            // The old implementation only checked a flag here. The task can
            // become due between that successful check and scope close().
            f.clock.set(100 * MILLIS);
            var result = worker.submit(() -> {
                // Represents a scheduled task already running, paused before
                // the watchdog body. Future.cancel(false) cannot stop it.
                entered.countDown();
                assertTrue(release.await(2, TimeUnit.SECONDS));
                dispatched.run();
                return null;
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            deadline.close();
            try (var next = f.open()) {
                f.clock.set(101 * MILLIS);
                release.countDown();
                result.get(2, TimeUnit.SECONDS);
                assertEquals(0, f.closes.get(), "a retired operation cannot close the next TLS exchange");
                next.complete();
            }
        } finally {
            release.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test void scopeUnwindingDisarmsAnAlreadyDispatchedWatchdog() {
        var f = new Fixture();
        var deadline = f.open();
        deadline.close();
        f.clock.set(100 * MILLIS);
        f.scheduled.run();
        assertEquals(0, f.closes.get());
        assertTrue(f.cancellations.get() > 0);
    }

    @Test void lateSchedulerCannotTurnAnOverdueOperationIntoSuccess() {
        var f = new Fixture();
        try (var deadline = f.open()) {
            f.clock.set(100 * MILLIS);
            assertThrows(SocketTimeoutException.class, deadline::complete);
            assertEquals(1, f.closes.get());
            f.scheduled.run();
            assertEquals(1, f.closes.get(), "expiration owns exactly one abort");
        }
    }

    @Test void watchdogWinnerCannotBeChangedIntoSuccessByCompletionOrClose() {
        var f = new Fixture();
        try (var deadline = f.open()) {
            f.clock.set(100 * MILLIS);
            f.scheduled.run();
            assertThrows(SocketTimeoutException.class, deadline::complete);
            deadline.close();
            assertThrows(SocketTimeoutException.class, deadline::complete);
            assertEquals(1, f.closes.get());
        }
    }

    @Test void timelyCompletionAndUnwindingAreDifferentOutcomes() throws Exception {
        var f = new Fixture();
        try (var deadline = f.open()) {
            f.clock.set(99 * MILLIS);
            deadline.complete();
            f.clock.set(150 * MILLIS);
            deadline.complete();
            assertEquals(0, f.closes.get());
        }
        try (var deadline = f.open()) {
            deadline.close();
            assertThrows(SocketTimeoutException.class, deadline::complete,
                    "scope disposal is not proof that an operation succeeded");
        }
    }

    @Test void absoluteDeadlineUsesMonotonicDifferenceAcrossSignedWrap() {
        var f = new Fixture();
        f.clock.set(Long.MAX_VALUE - 50 * MILLIS);
        try (var deadline = f.open()) {
            f.clock.addAndGet(100 * MILLIS);
            assertThrows(SocketTimeoutException.class, deadline::complete);
            assertEquals(1, f.closes.get());
        }
    }
}
