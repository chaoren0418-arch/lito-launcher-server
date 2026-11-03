package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Deterministic commit-admission clock tests: no disk, sleeping or unowned worker. */
final class ModernEquipmentLeaseLifetimeTest {
    private static final TournamentPresence.CharacterKey KEY =
            new TournamentPresence.CharacterKey(42, 5_000_003_333L);

    @Test void validityWindowIsInclusiveAtStartAndExclusiveAtExpiry() throws Exception {
        for (long now : new long[]{9, 10, 19, 20, 21}) {
            try (var h = new Harness()) {
                h.operation.restrictCommit(10, 20, () -> now);
                boolean expected = now >= 10 && now < 20;
                assertEquals(expected, h.operation.tryCommit(), "admission at " + now);
                assertEquals(expected, h.operation.isCommitAdmitted());
                assertEquals(expected, h.lease.hasActivePermit());
                assertFalse(h.lease.isInvalidated());
                assertEquals(0, h.retired.get());
            }
        }
    }

    @Test void explicitPermanentWindowStillRejectsBeforeStartAndPreservesI64Maximum() throws Exception {
        for (long now : new long[]{-1, 0, Long.MAX_VALUE}) {
            try (var h = new Harness()) {
                h.operation.restrictCommit(0, -1, () -> now);
                assertEquals(now >= 0, h.operation.tryCommit());
            }
        }
        try (var h = new Harness()) {
            h.operation.restrictCommit(Long.MAX_VALUE - 1, Long.MAX_VALUE, () -> Long.MAX_VALUE - 1);
            assertTrue(h.operation.tryCommit());
        }
        try (var h = new Harness()) {
            h.operation.restrictCommit(Long.MAX_VALUE - 1, Long.MAX_VALUE, () -> Long.MAX_VALUE);
            assertFalse(h.operation.tryCommit());
        }
    }

    @Test void clockIsSampledAtAdmissionRatherThanWhenWindowIsRegistered() throws Exception {
        var now = new AtomicLong(19);
        var calls = new AtomicInteger();
        try (var h = new Harness()) {
            h.operation.restrictCommit(10, 20, () -> {
                calls.incrementAndGet();
                return now.get();
            });
            assertEquals(0, calls.get());
            // Represents time elapsed while the caller prepares and forces its file.
            now.set(20);
            assertFalse(h.operation.tryCommit());
            assertEquals(1, calls.get());
            assertFalse(h.operation.isCommitAdmitted());
            assertTrue(h.lease.tryInvalidateIfNoPermit());
            assertEquals(1, h.retired.get());
            assertFalse(h.operation.tryCommit());
            assertEquals(1, calls.get());
        }
    }

    @Test void clockRegressionBeforeObservationStartCannotAdmitPreviouslyPreparedIntent() throws Exception {
        var now = new AtomicLong(19);
        try (var h = new Harness()) {
            h.operation.restrictCommit(10, 20, now::get);
            now.set(9);
            assertFalse(h.operation.tryCommit());
            assertFalse(h.operation.isCommitAdmitted());
            assertTrue(h.lease.tryInvalidateIfNoPermit());
            assertTrue(h.lease.isRetired());
        }
    }

    @Test void alreadyAdmittedPermitIsNotPretendedRolledBackWhenExpiryOrCancellationFollows() throws Exception {
        var now = new AtomicLong(19);
        var h = new Harness();
        try {
            h.operation.restrictCommit(10, 20, now::get);
            assertTrue(h.operation.tryCommit());
            now.set(20);
            h.scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            h.lease.invalidate();
            assertTrue(h.operation.isCommitAdmitted());
            assertTrue(h.lease.hasActivePermit());
            assertFalse(h.lease.isRetired());
            assertEquals(0, h.retired.get());
            assertFalse(h.operation.tryCommit());
            // Actual owned Work.close, not the caller's timeout, releases the holder.
            h.operation.close();
            assertFalse(h.operation.isCommitAdmitted());
            assertFalse(h.lease.hasActivePermit());
            assertTrue(h.lease.isRetired());
            assertEquals(1, h.retired.get());
        } finally {
            h.close();
        }
        assertEquals(1, h.retired.get());
    }

    @Test void restrictionIsSingleUseAndRejectedReplacementCannotWidenOriginalWindow() throws Exception {
        try (var h = new Harness()) {
            h.operation.restrictCommit(10, 20, () -> 20);
            assertThrows(IllegalStateException.class, () -> h.operation.restrictCommit(0, -1, () -> 0));
            assertThrows(IllegalStateException.class, () -> h.operation.restrictCommit(11, 19, () -> 12));
            assertThrows(IllegalStateException.class, () -> h.operation.restrictCommit(10, 20, () -> 19));
            assertFalse(h.operation.tryCommit());
            assertFalse(h.operation.isCommitAdmitted());
        }
    }

    @Test void registrationAfterAdmissionOrOperationCloseIsRejected() throws Exception {
        try (var h = new Harness()) {
            assertTrue(h.operation.tryCommit()); // Existing unrestricted lease contract remains available.
            assertThrows(IllegalStateException.class, () -> h.operation.restrictCommit(10, 20, () -> 19));
            assertTrue(h.operation.isCommitAdmitted());
        }
        try (var h = new Harness()) {
            h.operation.close();
            assertThrows(IllegalStateException.class, () -> h.operation.restrictCommit(10, 20, () -> 19));
            assertFalse(h.operation.tryCommit());
        }
    }

    @Test void invalidWindowOrNullClockDoesNotConsumeTheSingleRegistration() throws Exception {
        for (long[] bounds : new long[][]{{-1, -1}, {0, -2}, {10, 10}, {11, 10}}) {
            try (var h = new Harness()) {
                assertThrows(IllegalArgumentException.class,
                        () -> h.operation.restrictCommit(bounds[0], bounds[1], () -> 19));
                h.operation.restrictCommit(10, 20, () -> 19);
                assertTrue(h.operation.tryCommit());
            }
        }
        try (var h = new Harness()) {
            assertThrows(NullPointerException.class, () -> h.operation.restrictCommit(10, 20, null));
            h.operation.restrictCommit(10, 20, () -> 19);
            assertTrue(h.operation.tryCommit());
        }
    }

    @Test void cancelledScopeCannotReachClockOrCommitGateEvenInsideValidWindow() throws Exception {
        var calls = new AtomicInteger();
        try (var h = new Harness()) {
            h.operation.restrictCommit(10, 20, () -> {
                calls.incrementAndGet();
                return 19;
            });
            h.scope.cancel(ModernBackendExecutor.Failure.DEADLINE);
            assertEquals(ModernBackendExecutor.Failure.DEADLINE,
                    assertThrows(ModernBackendExecutor.BackendException.class, h.operation::tryCommit).failure());
            assertEquals(0, calls.get());
            assertFalse(h.operation.isCommitAdmitted());
            assertTrue(h.lease.tryInvalidateIfNoPermit());
            assertEquals(1, h.retired.get());
        }
    }

    @Test void failedClockCannotLeaveAnAdmittedPermitOrBlockRetirement() throws Exception {
        try (var h = new Harness()) {
            h.operation.restrictCommit(10, 20, () -> { throw new IllegalStateException("fixture clock unavailable"); });
            assertThrows(IllegalStateException.class, h.operation::tryCommit);
            assertFalse(h.operation.isCommitAdmitted());
            assertFalse(h.lease.hasActivePermit());
            assertTrue(h.lease.tryInvalidateIfNoPermit());
            assertTrue(h.lease.isRetired());
            assertEquals(1, h.retired.get());
        }
    }

    private static final class Harness implements AutoCloseable {
        final AtomicInteger retired = new AtomicInteger();
        final ModernEquipmentLease lease = new ModernEquipmentLease.Issuer().open(new Object(), KEY, -1, retired::incrementAndGet);
        final ModernBackendScope scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10),
                new ModernBackendScope.AbortScheduler() {
                    @Override public boolean execute(Runnable action) { return false; }
                    @Override public void remove(Runnable action) {}
                });
        final ModernEquipmentLease.Operation operation;

        Harness() throws ModernBackendExecutor.BackendException {
            operation = lease.operation(scope);
        }

        @Override public void close() {
            try {
                operation.close();
            } finally {
                try { lease.close(); }
                finally { scope.finish(); }
            }
        }
    }
}
