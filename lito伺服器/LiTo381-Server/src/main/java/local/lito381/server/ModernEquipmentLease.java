package local.lito381.server;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Opaque, authority-issued selection capability for modern equipment commits.
 *
 * <p>This class does not authenticate a player or attach itself to navigation or
 * {@link TournamentPresence}. The equipment authority must keep its Issuer and
 * owner capability private, mint only after server-confirmed selection, and
 * invalidate on BACK/close. A matching numeric key/epoch alone cannot mint a
 * lease or replace that authority-owned object.</p>
 *
 * <p>Commit admission and invalidate linearize under one short monitor. There
 * is never filesystem/JDBC work under that monitor. A permit admitted before
 * invalidate remains admitted: cancellation may therefore precede the actual
 * atomic rename without undoing it. Read the store revision after an uncertain
 * ACK; do not infer rollback from a cancelled backend Future.</p>
 *
 * <p>The backend scope's cancellation flag is a separate atomic domain. Its
 * check immediately before admission is useful, but is NOT an atomic guarantee
 * against scope cancellation between that check and this monitor acquisition.
 * The authority must also invalidate this lease on session cancellation.</p>
 */
final class ModernEquipmentLease implements AutoCloseable {
    /** Held privately by one trusted equipment authority, never supplied by wire data. */
    static final class Issuer {
        ModernEquipmentLease open(Object ownerCapability, TournamentPresence.CharacterKey key,
                                  long selectionEpoch, Runnable onRetired) {
            return new ModernEquipmentLease(this, ownerCapability, key, selectionEpoch, onRetired);
        }

        boolean owns(ModernEquipmentLease lease) {
            return lease != null && lease.issuer == this;
        }

        @Override public String toString() { return "ModernEquipmentLeaseIssuer[redacted]"; }
    }

    private final Issuer issuer;
    private final Object ownerCapability;
    private final TournamentPresence.CharacterKey key;
    private final long selectionEpoch;
    private final Runnable onRetired;
    private boolean invalidated;
    private boolean retirementClaimed;
    private boolean retired;
    private boolean retirementFailed;
    private Operation operation;

    private ModernEquipmentLease(Issuer issuer, Object ownerCapability,
                                 TournamentPresence.CharacterKey key, long selectionEpoch,
                                 Runnable onRetired) {
        this.issuer = Objects.requireNonNull(issuer);
        this.ownerCapability = Objects.requireNonNull(ownerCapability);
        this.key = Objects.requireNonNull(key);
        // The wire epoch is an opaque nonzero u64 bit pattern, not a signed
        // positive revision. Preserve Long.MIN_VALUE/-1 exactly.
        if (selectionEpoch == 0) throw new IllegalArgumentException("invalid equipment selection epoch");
        this.selectionEpoch = selectionEpoch;
        this.onRetired = Objects.requireNonNull(onRetired);
    }

    TournamentPresence.CharacterKey key() { return key; }
    long selectionEpoch() { return selectionEpoch; }

    synchronized boolean matches(Object owner, TournamentPresence.CharacterKey expectedKey, long epoch) {
        return !invalidated && owner == ownerCapability && key.equals(expectedKey) && selectionEpoch == epoch;
    }

    /**
     * One operation holder per lease. Create inside the actual backend Work.run
     * and retain it in that Work until Work.close (its actual finally cleanup).
     * Queued work that never starts must not manufacture a holder.
     */
    Operation operation(ModernBackendScope scope) throws ModernBackendExecutor.BackendException {
        Objects.requireNonNull(scope).checkActive();
        synchronized (this) {
            if (invalidated || operation != null) throw new IllegalStateException("equipment lease operation unavailable");
            operation = new Operation(scope);
            return operation;
        }
    }

    /**
     * No waiting for disk or for an admitted operation. Before admission,
     * invalidate retires immediately; after admission, the holder's finally
     * close delivers retirement. The callback executes outside the monitor and
     * must be a short, nonblocking authority release with NO disk/network work.
     */
    void invalidate() {
        boolean notify;
        synchronized (this) {
            invalidated = true;
            notify = claimRetirement();
        }
        if (notify) notifyRetired();
    }

    /**
     * Atomic BACK admission, never a hasActivePermit()/invalidate() check-then-act.
     * An active permit returns false without changing this lease at all. With
     * no permit, invalidate prevents any late preparation from admitting; true
     * means the retirement callback has actually finished. Concurrent callback
     * execution or a failed callback is a fail-closed exception, not a wait or
     * a false claim that the role was released. Unconditional close still uses
     * invalidate(), allowing an already admitted commit to finish later.
     */
    boolean tryInvalidateIfNoPermit() {
        boolean notify;
        synchronized (this) {
            if (hasActivePermit()) return false;
            if (retirementFailed || (retirementClaimed && !retired)) {
                throw new IllegalStateException("equipment lease retirement unavailable");
            }
            invalidated = true;
            notify = claimRetirement();
        }
        if (notify) notifyRetired();
        synchronized (this) {
            if (!retired) throw new IllegalStateException("equipment lease retirement unavailable");
            return true;
        }
    }

    @Override public void close() { invalidate(); }
    synchronized boolean isInvalidated() { return invalidated; }
    /** True only after the authority's retirement callback actually returned. */
    synchronized boolean isRetired() { return retired; }
    synchronized boolean retirementFailed() { return retirementFailed; }
    synchronized boolean hasActivePermit() {
        return operation != null && operation.admitted && !operation.closed;
    }

    private boolean claimRetirement() {
        // Called only with this monitor held. An unadmitted preparation can
        // finish its owned temporary-file cleanup after authority retirement,
        // but cannot acquire permission to change the durable target anymore.
        if (!invalidated || retirementClaimed || hasActivePermit()) return false;
        retirementClaimed = true;
        return true;
    }

    private void notifyRetired() {
        try {
            onRetired.run();
            synchronized (this) { retired = true; }
        } catch (RuntimeException | Error failure) {
            synchronized (this) { retirementFailed = true; }
            // Never claim that failed authority cleanup actually released a
            // character. Do not retry an unknown partially completed callback.
            // No callback cause/object/account is included in diagnostics.
            throw new IllegalStateException("equipment lease retirement failed");
        }
    }

    /**
     * Operation-owned permit holder, not a store-local try-with-resources.
     * The store validates matchesKey and scope identity, prepares/forces its
     * temporary file, then calls tryCommit immediately before atomic rename.
     * Success OR failure after admission leaves this holder live until the
     * owning BackendExecutor.Work.close runs; the store must not close it.
     */
    final class Operation implements AutoCloseable {
        private final ModernBackendScope scope;
        private boolean admitted;
        private boolean closed;
        private LongSupplier validityClock;
        private long notBefore;
        private long expiresExclusive = -1;

        private Operation(ModernBackendScope scope) { this.scope = scope; }
        ModernBackendScope scope() { return scope; }
        TournamentPresence.CharacterKey key() { return key; }
        boolean matchesKey(TournamentPresence.CharacterKey expected) { return key.equals(expected); }
        boolean matchesSelection(Object owner, long epoch) {
            synchronized (ModernEquipmentLease.this) {
                return !closed && !invalidated && operation == this
                        && ownerCapability == owner && selectionEpoch == epoch;
            }
        }

        /** Trusted, constant-time clock only: never JDBC/filesystem/user callback.
         * This guard is checked at the actual store admission after force(). */
        void restrictCommit(long notBefore, long expiresExclusive, LongSupplier clock) {
            if (notBefore < 0 || expiresExclusive < -1 || expiresExclusive >= 0 && expiresExclusive <= notBefore)
                throw new IllegalArgumentException("invalid equipment validity window");
            Objects.requireNonNull(clock);
            synchronized (ModernEquipmentLease.this) {
                if (closed || admitted || validityClock != null) throw new IllegalStateException("equipment validity already fixed");
                this.notBefore = notBefore; this.expiresExclusive = expiresExclusive; this.validityClock = clock;
            }
        }

        boolean tryCommit() throws ModernBackendExecutor.BackendException {
            synchronized (ModernEquipmentLease.this) {
                if (closed || admitted || invalidated || operation != this) return false;
            }
            // Outside the lease monitor: checkActive can trigger JDBC/raw-socket
            // cancellation and must not do that while holding the commit gate.
            scope.checkActive();
            synchronized (ModernEquipmentLease.this) {
                if (closed || admitted || invalidated || operation != this) return false;
                if (validityClock != null) {
                    long now = validityClock.getAsLong();
                    if (now < notBefore || expiresExclusive >= 0 && now >= expiresExclusive) return false;
                }
                admitted = true;
                return true;
            }
        }

        boolean isCommitAdmitted() {
            synchronized (ModernEquipmentLease.this) { return admitted && !closed; }
        }

        /** Caller-owned worker finally only; idempotent and never checks cancellation. */
        @Override public void close() {
            boolean notify;
            synchronized (ModernEquipmentLease.this) {
                if (closed) return;
                closed = true;
                if (operation == this) operation = null;
                notify = claimRetirement();
            }
            if (notify) notifyRetired();
        }

        @Override public String toString() { return "ModernEquipmentOperation[redacted]"; }
    }

    @Override public String toString() { return "ModernEquipmentLease[redacted]"; }
}
