// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeNpcVitalState;

class NativeDeVitalState
extends NativeDeNpcVitalState {
    private final AtomicLong _deathGeneration = new AtomicLong();

    NativeDeVitalState(int n, int n2) {
        super(n, n2);
    }

    NativeDeVitalState(int n, NativeDeCharacterFields.Snapshot snapshot) throws IOException {
        super(n, snapshot);
    }

    private long commitDamageState(int n) {
        return this.withVitalMutation(() -> {
            if (this.isDead()) {
                return 0L;
            }
            int n2 = this.getCurrentHp();
            long l = (long)n2 - (long)n;
            if (n2 <= 0 || l <= 0L) {
                this.setCurrentHpDirect(0);
                this.setDead(true);
                this.setStatus(8);
                long l2 = this._deathGeneration.incrementAndGet();
                this.markAsyncDeathPending(l2);
                return l2;
            }
            int n3 = (int)Math.min((long)this.getMaxHp(), l);
            if (n3 <= 0) {
                this.setCurrentHpDirect(0);
                this.setDead(true);
                this.setStatus(8);
                long l3 = this._deathGeneration.incrementAndGet();
                this.markAsyncDeathPending(l3);
                return l3;
            }
            if (n3 == n2) {
                return 0L;
            }
            this.setCurrentHpDirect(n3);
            return 0L;
        });
    }

    private boolean isCurrentDeathGeneration(long l) {
        return l != 0L && this._deathGeneration.get() == l && this.isDead() && !this.destroyed();
    }

    @Override
    public void setDead(boolean bl) {
        this.withVitalMutation(() -> {
            boolean bl2 = !bl && this.isDead();
            super.setDead(bl);
            if (bl2 && !this.isDead()) {
                this._deathGeneration.incrementAndGet();
            }
        });
    }

    long commitDamage(int n) {
        return this.commitDamageState(n);
    }

    boolean currentDeath(long l) {
        return this.isCurrentDeathGeneration(l);
    }

    long deathGeneration() {
        return this._deathGeneration.get();
    }
}
