// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeCorpseClock;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeathLifecycleGate;

class NativeDeNpcVitalState
extends NativeDeCharacterFields {
    private final NativeDeathLifecycleGate _asyncDeathLifecycle = new NativeDeathLifecycleGate();
    private int _weaponBreakStack;
    public volatile boolean _destroyed = false;
    private static final AtomicLong CORPSE_RESURRECTION_REJECTED_COUNT = new AtomicLong();
    private final AtomicBoolean _deleteStarted = new AtomicBoolean();
    private int _deadTimerTemp = -1;
    private long _corpseDeleteDeadlineNanos = -1L;
    private boolean _corpseDeletionClaimed;
    protected boolean _hprRunning = false;
    private boolean _mprRunning = false;
    private static final AtomicLong CORPSE_TIMER_INVALID_START_COUNT = new AtomicLong();

    NativeDeNpcVitalState(int n, int n2) {
        super(n, n2);
    }

    NativeDeNpcVitalState(int n, NativeDeCharacterFields.Snapshot snapshot) throws IOException {
        super(n, snapshot);
    }

    protected final boolean markAsyncDeathPending(long l) {
        return this._asyncDeathLifecycle.markDeadPending(l);
    }

    protected final NativeDeathLifecycleGate.Ownership asyncDeathOwnership(long l) {
        return this._asyncDeathLifecycle.ownership(l);
    }

    protected final boolean isAsyncDeathPending(long l) {
        return this._asyncDeathLifecycle.isPending(l);
    }

    protected final boolean isAsyncDeathGeneration(long l) {
        return this._asyncDeathLifecycle.isDeathGeneration(l);
    }

    public int getWeaponBreakStack() {
        return this._weaponBreakStack;
    }

    public void addWeaponBreakStack() {
        if (this._weaponBreakStack < 10) {
            ++this._weaponBreakStack;
        }
    }

    public void setWeaponBreakStack(int n) {
        this._weaponBreakStack = Math.max(0, Math.min(10, n));
    }

    public void setDead(boolean bl) {
        this.withVitalMutation(() -> {
            if (!this._asyncDeathLifecycle.isManaged()) {
                this.setCharacterDead(bl);
                if (bl) {
                    this.setWeaponBreakStack(0);
                }
                return;
            }
            NativeDeathLifecycleGate.State state = this._asyncDeathLifecycle.snapshot();
            if (bl) {
                if (state.phase() != NativeDeathLifecycleGate.Phase.RESURRECTING && state.phase() != NativeDeathLifecycleGate.Phase.TERMINATED) {
                    this.setCharacterDead(true);
                    this.setWeaponBreakStack(0);
                }
                return;
            }
            switch (state.phase()) {
                case ACTIVE: 
                case RESURRECTING: {
                    this.setCharacterDead(false);
                    break;
                }
                case DEAD_PENDING: 
                case DEAD_SETTLED: 
                case DEATH_DROPPED: {
                    if (!this._asyncDeathLifecycle.reviveDirect(state)) break;
                    this.setCharacterDead(false);
                    break;
                }
            }
        });
    }

    public boolean destroyed() {
        return this._destroyed;
    }

    NativeDeathLifecycleGate.State lifecycleSnapshot() {
        return this._asyncDeathLifecycle.snapshot();
    }

    NativeDeathLifecycleGate deathGate() {
        return this._asyncDeathLifecycle;
    }

    @Override
    protected void setDeadForResurrection(boolean bl) {
        this.setDead(bl);
    }

    protected int getNpcId() {
        return this.resurrectionEffects().npcId(this);
    }

    @Override
    public int getX() {
        return this.resurrectionEffects().x(this);
    }

    @Override
    public int getY() {
        return this.resurrectionEffects().y(this);
    }

    @Override
    public synchronized void resurrect(int n) {
        if (this._destroyed) {
            return;
        }
        if (this._corpseDeletionClaimed) {
            long l = CORPSE_RESURRECTION_REJECTED_COUNT.incrementAndGet();
            this.resurrectionEffects().corpseRejected(this.getId(), this.getNpcId(), l);
            return;
        }
        boolean bl = this.withVitalMutation(() -> {
            if (!this._asyncDeathLifecycle.isManaged()) {
                super.resurrect(n);
                return !this.isDead();
            }
            NativeDeathLifecycleGate.ResurrectionClaim resurrectionClaim = this._asyncDeathLifecycle.tryReserveResurrection();
            if (resurrectionClaim == null) {
                return false;
            }
            try {
                super.resurrect(n);
                if (this.isDead()) {
                    this._asyncDeathLifecycle.abortResurrection(resurrectionClaim);
                    return false;
                }
                if (!this._asyncDeathLifecycle.completeResurrection(resurrectionClaim)) {
                    this.setCharacterDead(true);
                    return false;
                }
                return true;
            }
            catch (Error | RuntimeException throwable) {
                this._asyncDeathLifecycle.abortResurrection(resurrectionClaim);
                throw throwable;
            }
        });
        if (!bl) {
            return;
        }
        this.setWeaponBreakStack(0);
        this.startHpRegeneration();
        this.startMpRegeneration();
        this.resurrectionEffects().broadcastResurrection(this, this, 0);
        NativeDeResurrectionEffects.LoginSkill loginSkill = this.resurrectionEffects().newSkillUse();
        loginSkill.handleCommands(null, 44, this.getId(), this.getX(), this.getY(), 0, 1, this);
        this.clearCorpseDeleteTimerLocked();
    }

    public final void startHpRegeneration() {
        if (this.destroyed() && this.isDead()) {
            return;
        }
        if (this.getCurrentHp() <= 0) {
            return;
        }
        if (!this._hprRunning) {
            this._hprRunning = true;
        }
    }

    public final void startMpRegeneration() {
        if (this.destroyed() && this.isDead()) {
            return;
        }
        if (this.getCurrentHp() <= 0) {
            return;
        }
        if (!this._mprRunning) {
            this._mprRunning = true;
        }
    }

    public final boolean isHpRegenerationX() {
        return this._hprRunning;
    }

    public final boolean isMpRegenerationX() {
        return this._mprRunning;
    }

    public synchronized boolean claimExpiredCorpseDeletion(long l) {
        if (this._destroyed || this._deleteStarted.get() || this._corpseDeletionClaimed || this._corpseDeleteDeadlineNanos < 0L || !this.isDead()) {
            return false;
        }
        if (l - this._corpseDeleteDeadlineNanos < 0L) {
            return false;
        }
        this._corpseDeletionClaimed = true;
        this._deadTimerTemp = -1;
        this._corpseDeleteDeadlineNanos = -1L;
        return true;
    }

    private void clearCorpseDeleteTimerLocked() {
        this._deadTimerTemp = -1;
        this._corpseDeleteDeadlineNanos = -1L;
        this.resurrectionEffects().untrackCorpse(this);
    }

    protected long corpseNanoTime() {
        return System.nanoTime();
    }

    protected NativeDeCorpseClock corpseClock() {
        return NativeDeCorpseClock.UNBOUND;
    }

    public final boolean deletionStarted() {
        return this._deleteStarted.get();
    }

    protected synchronized void startDeleteTimer(int n) {
        if (this._destroyed || this._deleteStarted.get()) {
            long l = CORPSE_TIMER_INVALID_START_COUNT.incrementAndGet();
            this.corpseClock().invalidStart(this.getId(), this.getNpcId(), n, l);
            return;
        }
        if (this._corpseDeleteDeadlineNanos >= 0L || this._corpseDeletionClaimed) {
            return;
        }
        this._deadTimerTemp = Math.max(0, n);
        this._corpseDeleteDeadlineNanos = this.corpseNanoTime() + TimeUnit.SECONDS.toNanos(this._deadTimerTemp);
        this.corpseClock().track(this);
    }

    public synchronized void set_deadTimerTemp(int n) {
        if (n < 0) {
            this.clearCorpseDeleteTimerLocked();
            this._corpseDeletionClaimed = false;
            return;
        }
        this._deadTimerTemp = n;
        this._corpseDeleteDeadlineNanos = this.corpseNanoTime() + TimeUnit.SECONDS.toNanos(n);
        this._corpseDeletionClaimed = false;
        this.corpseClock().track(this);
    }

    public synchronized int get_deadTimerTemp() {
        if (this._corpseDeleteDeadlineNanos < 0L) {
            return this._deadTimerTemp;
        }
        long l = this._corpseDeleteDeadlineNanos - this.corpseNanoTime();
        if (l <= 0L) {
            return 0;
        }
        return (int)Math.min(Integer.MAX_VALUE, TimeUnit.NANOSECONDS.toSeconds(l) + 1L);
    }

    private synchronized void releaseCorpseDeletionClaimForPairedResurrection() {
        this._corpseDeletionClaimed = false;
        this.clearCorpseDeleteTimerLocked();
    }

    protected final AtomicBoolean deletionClaimFlag() {
        return this._deleteStarted;
    }

    protected final void releasePairedCorpseClaim() {
        this.releaseCorpseDeletionClaimForPairedResurrection();
    }

    public final void sourceNpcSetDead(boolean bl) {
        this.withVitalMutation(() -> {
            if (!this._asyncDeathLifecycle.isManaged()) {
                this.setCharacterDead(bl);
                if (bl) {
                    this.setWeaponBreakStack(0);
                }
                return;
            }
            NativeDeathLifecycleGate.State state = this._asyncDeathLifecycle.snapshot();
            if (bl) {
                if (state.phase() != NativeDeathLifecycleGate.Phase.RESURRECTING && state.phase() != NativeDeathLifecycleGate.Phase.TERMINATED) {
                    this.setCharacterDead(true);
                    this.setWeaponBreakStack(0);
                }
                return;
            }
            switch (state.phase()) {
                case ACTIVE: 
                case RESURRECTING: {
                    this.setCharacterDead(false);
                    break;
                }
                case DEAD_PENDING: 
                case DEAD_SETTLED: 
                case DEATH_DROPPED: {
                    if (!this._asyncDeathLifecycle.reviveDirect(state)) break;
                    this.setCharacterDead(false);
                    break;
                }
            }
        });
    }
}
