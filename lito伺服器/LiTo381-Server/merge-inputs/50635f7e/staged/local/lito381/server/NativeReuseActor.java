// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.ClientSession
 *  local.lito381.server.NativeCastMaterialPolicy$State
 *  local.lito381.server.NativeDeItemApplication$Owner
 *  local.lito381.server.NativeRetiredReuseTimer
 */
package local.lito381.server;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeRetiredReuseTimer;

final class NativeReuseActor {
    final ClientSession pc;
    final NativeDeItemApplication.Owner owner;
    final NativePlayerLifetime life;
    final int characterId;
    NativeCastMaterialPolicy.State materialPolicy;
    private static final Object LEGACY_SKILL_DELAY_TOKEN = new Object();
    private final AtomicReference<Object> _skillDelayToken = new AtomicReference();

    NativeReuseActor(ClientSession clientSession, NativeDeItemApplication.Owner owner) {
        this.pc = clientSession;
        this.owner = owner;
        this.life = NativePlayerLifetime.of(clientSession);
        this.characterId = (int)clientSession.playerObjectId();
    }

    public int getId() {
        return this.characterId;
    }

    public ScheduledFuture<?> pcSchedule(Runnable runnable, long l) {
        return NativeRetiredReuseTimer.schedule((NativeReuseActor)this, (Runnable)runnable, (long)l);
    }

    public void setSkillDelay(boolean bl) {
        if (bl) {
            this._skillDelayToken.compareAndSet(null, LEGACY_SKILL_DELAY_TOKEN);
        } else {
            this._skillDelayToken.compareAndSet(LEGACY_SKILL_DELAY_TOKEN, null);
        }
    }

    public boolean trySetSkillDelay() {
        return this._skillDelayToken.compareAndSet(null, LEGACY_SKILL_DELAY_TOKEN);
    }

    public boolean trySetSkillDelay(Object object) {
        return object != null && this._skillDelayToken.compareAndSet(null, object);
    }

    public boolean clearSkillDelay(Object object) {
        return object != null && this._skillDelayToken.compareAndSet(object, null);
    }

    public boolean isSkillDelay() {
        return this._skillDelayToken.get() != null;
    }
}

