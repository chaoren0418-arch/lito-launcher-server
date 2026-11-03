package local.lito381.server;

import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** One transport operation's watchdog. Completion, cancellation and expiration
 * have one atomic winner; a dispatched old callback cannot abort a later frame.
 * The monotonic clock also enforces the bound when the scheduler runs late. */
final class ModernTransportDeadline implements AutoCloseable {
    @FunctionalInterface interface Cancellation { void cancel(); }
    @FunctionalInterface interface Scheduler { Cancellation schedule(Runnable expiration, int milliseconds); }
    private enum Outcome { ARMED, COMPLETED, CANCELLED, EXPIRED }
    private final AtomicReference<Outcome> outcome = new AtomicReference<>(Outcome.ARMED);
    private final LongSupplier clock;
    private final long deadlineNanos;
    private final BooleanSupplier ownerClosed;
    private final Runnable onExpiry;
    private final Cancellation cancellation;

    ModernTransportDeadline(int milliseconds, LongSupplier clock, Scheduler scheduler,
                            Runnable onExpiry, BooleanSupplier ownerClosed) {
        if (milliseconds < 50 || milliseconds > 30_000) throw new IllegalArgumentException("invalid transport deadline");
        this.clock = Objects.requireNonNull(clock);
        this.ownerClosed = Objects.requireNonNull(ownerClosed);
        this.onExpiry = Objects.requireNonNull(onExpiry);
        deadlineNanos = clock.getAsLong() + TimeUnit.MILLISECONDS.toNanos(milliseconds);
        cancellation = Objects.requireNonNull(Objects.requireNonNull(scheduler).schedule(this::expire, milliseconds));
    }

    void complete() throws SocketTimeoutException {
        if (outcome.get() == Outcome.ARMED) {
            if (clock.getAsLong() - deadlineNanos >= 0) expire();
            else if (!ownerClosed.getAsBoolean() && outcome.compareAndSet(Outcome.ARMED, Outcome.COMPLETED)) {
                cancellation.cancel();
            }
        }
        if (outcome.get() != Outcome.COMPLETED || ownerClosed.getAsBoolean()) {
            throw new SocketTimeoutException("modern transport deadline exceeded");
        }
    }

    private void expire() {
        if (outcome.compareAndSet(Outcome.ARMED, Outcome.EXPIRED)) onExpiry.run();
    }

    @Override public void close() {
        // Cancelling the Future is only queue cleanup: a running callback may
        // already have passed the executor's cancellation check.
        outcome.compareAndSet(Outcome.ARMED, Outcome.CANCELLED);
        cancellation.cancel();
    }
}
