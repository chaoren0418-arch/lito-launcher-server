// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceInitialization;
import local.lito381.server.NativeDeInstanceNames;
import local.lito381.server.NativeDeInstancePorts;
import local.lito381.server.NativeDeLiveAiJobs;
import local.lito381.server.NativeDeLiveAiPorts;
import local.lito381.server.NativeDeLiveAiScope;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDeWorldKinds;

public class NativeDeInstanceState
extends NativeDeNpcConstructionState
implements NativeDeInitializerKernel.Actor,
Runnable {
    final NativeDeInstancePorts.Construction construction;
    private NativeDeInstancePorts.Combat _combatBehavior;
    private final Object _deAiScheduleLock;
    private long _deAiChainToken;
    private boolean _deAiTickPending;
    private boolean _deAiTickExecuting;
    private boolean _deAiStopRequested;
    private boolean _deAiWakeupRequested;
    private boolean _deAiRestartRequested;
    private long _deAiTimerSequence;
    private long _deAiActiveTimerToken;
    private boolean _deAiPendingBackground;
    private int _deAiWorkerRetryAttempt;
    private volatile long _lastWakeupTime;
    private volatile ScheduledFuture<?> _runFuture;
    private long _delayedMoveGenerationSequence;
    private long _delayedMoveGeneration;
    private long _delayedMoveTimerSequence;
    private long _delayedMoveActiveTimerToken;
    private ScheduledFuture<?> _delayedMoveFuture;
    private volatile long _deNextActionUntilMs;
    public final DeRepathThrottle deRepathThrottle;
    private volatile boolean _deFootprintBlockAggroApplied;
    private boolean _spawnLocationChecked;
    private volatile boolean _deathMatchArenaCombatEnabled;
    private NativeDePerceptionEmblemData _emblem;
    private boolean _isMorphEnable;
    private boolean _isDollEnable;
    private String _classRestrict;
    private boolean _initFailed;
    private int _classId;
    private int _currentWeapon;
    private boolean _isAutoBuffEnabled;
    private int _autoBuffInterval;
    private int _autoBuffChance;
    private long _lastAutoBuffTime;
    private final AtomicLong _deathGeneration;
    private volatile Object sourceDeLiveAiBinding;

    @Override
    public void set_emblem(NativeDeInitializerKernel.Emblem emblem) {
        this.set_emblem(emblem == null ? null : this.construction.scene().emblem(emblem));
    }

    @Override
    public void broadcastPacketAll(NativeDeInitializerKernel.Packet packet) {
        this.construction.scene().publish(this, packet);
    }

    public void setCombatBehavior(NativeDeInstancePorts.Combat combat) {
        this._combatBehavior = combat;
    }

    public NativeDeInstancePorts.Combat getCombatBehavior() {
        return this._combatBehavior;
    }

    public void markFootprintBlockAggroApplied() {
        this._deFootprintBlockAggroApplied = true;
    }

    public boolean consumeFootprintBlockAggroApplied() {
        if (!this._deFootprintBlockAggroApplied) {
            return false;
        }
        this._deFootprintBlockAggroApplied = false;
        return true;
    }

    public void clearFootprintBlockAggroApplied() {
        this._deFootprintBlockAggroApplied = false;
    }

    public boolean isInitFailed() {
        return this._initFailed;
    }

    @Override
    public void setInitFailed(boolean bl) {
        this._initFailed = bl;
    }

    @Override
    public boolean isMorphEnable() {
        return this._isMorphEnable;
    }

    public void setMorphEnable(boolean bl) {
        this._isMorphEnable = bl;
    }

    @Override
    public int getCurrentWeapon() {
        return this._currentWeapon;
    }

    @Override
    public void setCurrentWeapon(int n) {
        this._currentWeapon = n;
    }

    public boolean isAutoBuffEnabled() {
        return this._isAutoBuffEnabled;
    }

    public void setAutoBuffEnabled(boolean bl) {
        this._isAutoBuffEnabled = bl;
    }

    public int getAutoBuffInterval() {
        return this._autoBuffInterval;
    }

    public void setAutoBuffInterval(int n) {
        this._autoBuffInterval = n;
    }

    public int getAutoBuffChance() {
        return this._autoBuffChance;
    }

    public void setAutoBuffChance(int n) {
        this._autoBuffChance = n;
    }

    public long getLastAutoBuffTime() {
        return this._lastAutoBuffTime;
    }

    public void setLastAutoBuffTime(long l) {
        this._lastAutoBuffTime = l;
    }

    public void executeAutoBuff() {
        if (!this._isAutoBuffEnabled) {
            return;
        }
        if (this._combatBehavior != null) {
            this._combatBehavior.checkAutoState();
        }
    }

    // #72 approved ctor-prologue-order: Java 17 assigns construction after super.
    // Name validation must still happen before any superclass constructor effects.
    public NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeNpcTemplate nativeDeNpcTemplate) {
        super(construction.id(), construction.effects(), construction.owner(), construction.perception(), construction.template(), construction.npc(), nativeDeNpcTemplate);
        this.construction = construction;
        this._deAiScheduleLock = new Object();
        this._deAiChainToken = 0L;
        this._deAiTickPending = false;
        this._deAiTickExecuting = false;
        this._deAiStopRequested = false;
        this._deAiWakeupRequested = false;
        this._deAiRestartRequested = false;
        this._deAiTimerSequence = 0L;
        this._deAiActiveTimerToken = 0L;
        this._deAiPendingBackground = false;
        this._deAiWorkerRetryAttempt = 0;
        this._isBackgroundMode = false;
        this._lastWakeupTime = System.currentTimeMillis();
        this._runFuture = null;
        this._delayedMoveGenerationSequence = 0L;
        this._delayedMoveGeneration = 0L;
        this._delayedMoveTimerSequence = 0L;
        this._delayedMoveActiveTimerToken = 0L;
        this._delayedMoveFuture = null;
        this._deNextActionUntilMs = 0L;
        this.deRepathThrottle = new DeRepathThrottle();
        this._spawnLocationChecked = false;
        this._clanid = 0;
        this._aiType = 0;
        this._clan_name = null;
        this._emblem = null;
        this.deName = null;
        this._isMorphEnable = false;
        this._isDollEnable = false;
        this._classRestrict = null;
        this._initFailed = false;
        this._currentWeapon = 0;
        this._isAutoBuffEnabled = false;
        this._autoBuffInterval = 0;
        this._autoBuffChance = 0;
        this._lastAutoBuffTime = 0L;
        this._deathGeneration = new AtomicLong();
        this.startNpc();
    }

    public NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeNpcTemplate nativeDeNpcTemplate, NativeDeInitializerKernel.Name name) {
        this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate);
        this.startNpc();
    }

    public NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeNpcTemplate nativeDeNpcTemplate, NativeDeInitializerKernel.Name name, boolean bl) {
        this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate);
        this._isMorphEnable = bl;
        this.startNpc();
    }

    public NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeNpcTemplate nativeDeNpcTemplate, NativeDeInitializerKernel.Name name, boolean bl, String string) {
        this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate);
        this._isMorphEnable = bl;
        this._isDollEnable = false;
        this._classRestrict = string;
        this.startNpc();
    }

    public NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeNpcTemplate nativeDeNpcTemplate, NativeDeInitializerKernel.Name name, boolean bl, boolean bl2, String string) {
        this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate);
        this._isMorphEnable = bl;
        this._isDollEnable = bl2;
        this._classRestrict = string;
        this.startNpc();
    }

    private NativeDeInstanceState(NativeDeInstancePorts.Construction construction, NativeDeInitializerKernel.Name name, NativeDeNpcTemplate nativeDeNpcTemplate) {
        super(construction.id(), construction.effects(), construction.owner(), construction.perception(), construction.template(), construction.npc(), nativeDeNpcTemplate);
        this.construction = construction;
        this._deAiScheduleLock = new Object();
        this._deAiChainToken = 0L;
        this._deAiTickPending = false;
        this._deAiTickExecuting = false;
        this._deAiStopRequested = false;
        this._deAiWakeupRequested = false;
        this._deAiRestartRequested = false;
        this._deAiTimerSequence = 0L;
        this._deAiActiveTimerToken = 0L;
        this._deAiPendingBackground = false;
        this._deAiWorkerRetryAttempt = 0;
        this._isBackgroundMode = false;
        this._lastWakeupTime = System.currentTimeMillis();
        this._runFuture = null;
        this._delayedMoveGenerationSequence = 0L;
        this._delayedMoveGeneration = 0L;
        this._delayedMoveTimerSequence = 0L;
        this._delayedMoveActiveTimerToken = 0L;
        this._delayedMoveFuture = null;
        this._deNextActionUntilMs = 0L;
        this.deRepathThrottle = new DeRepathThrottle();
        this._spawnLocationChecked = false;
        this._clanid = 0;
        this._aiType = 0;
        this._clan_name = null;
        this._emblem = null;
        this.deName = null;
        this._isMorphEnable = false;
        this._isDollEnable = false;
        this._classRestrict = null;
        this._initFailed = false;
        this._currentWeapon = 0;
        this._isAutoBuffEnabled = false;
        this._autoBuffInterval = 0;
        this._autoBuffChance = 0;
        this._lastAutoBuffTime = 0L;
        this._deathGeneration = new AtomicLong();
        this.deName = name;
    }
    private void startNpc() {
        NativeDeInstanceInitialization.init(this, this.deName, this._isMorphEnable, this._classRestrict);
        if (this.getLevel() <= 0) {
            this.setLevel(1);
        }
        if (this.getInt() <= 0) {
            this.setInt(10);
        }
    }

    public void startCompanions() {
        this.startCompanions(true);
    }

    public void startCompanions(boolean bl) {
        if (this._isDollEnable) {
            NativeDeInstanceInitialization.initDoll(this);
        }
        if (bl) {
            NativeDeInstanceInitialization.initPet(this);
        }
    }

    @Override
    public int getClassId() {
        return this._classId;
    }

    @Override
    public void setClassId(int n) {
        this._classId = n;
    }

    public void setAiType(int n) {
        this._aiType = n;
    }

    public int getAiType() {
        return this._aiType;
    }

    public void setDeathMatchArenaCombatEnabled(boolean bl) {
        this._deathMatchArenaCombatEnabled = bl;
    }

    public boolean isDeathMatchArenaCombatEnabled() {
        return this._deathMatchArenaCombatEnabled;
    }

    @Override
    public NativeDeInitializerKernel.Name get_deName() {
        return this.deName;
    }

    @Override
    public void set_deName(NativeDeInitializerKernel.Name name) {
        name = NativeDeInstanceNames.name(this.construction, name);
        this.deName = name;
    }

    @Override
    public void set_clanid(int n) {
        this._clanid = n;
    }

    @Override
    public void set_clanname(String string) {
        this._clan_name = string;
    }

    public void set_emblem(NativeDePerceptionEmblemData nativeDePerceptionEmblemData) {
        this._emblem = nativeDePerceptionEmblemData;
    }

    public void setClanid(int n) {
        this.set_clanid(n);
    }

    public void setClanname(String string) {
        this.set_clanname(string);
    }

    @Override
    public boolean isPickupItem() {
        return true;
    }

    public void setTargetItem(NativeDeWorldKinds.Item item) {
        this._targetItem = item;
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
            ((NativeDeNpcVitalState)this).sourceNpcSetDead(bl);
            if (bl2 && !this.isDead()) {
                this._deathGeneration.incrementAndGet();
            }
        });
    }

    @Override
    public int getClanid() {
        return this._clanid;
    }

    @Override
    public String getClanname() {
        return this._clan_name;
    }

    public NativeDePerceptionEmblemData getEmblem() {
        return this._emblem;
    }

    @Override
    public void removeFromDeNameList() {
        if (this.deName != null) {
            NativeDeInstanceInitialization.namesLock(this).lock();
            try {
                NativeDeInstanceInitialization.names(this).remove(this.deName);
            }
            finally {
                NativeDeInstanceInitialization.namesLock(this).unlock();
            }
        }
    }

    @Override
    public int getMagicLevel() {
        return Math.max(this.getLevel(), 1);
    }

    @Override
    public int getMagicBonus() {
        int n = this.getInt() / 2;
        return Math.max(n, 1);
    }

    @Override
    long commitDamage(int n) {
        return this.commitDamageState(n);
    }

    @Override
    boolean currentDeath(long l) {
        return this.isCurrentDeathGeneration(l);
    }

    @Override
    long deathGeneration() {
        return this._deathGeneration.get();
    }

    private String sourceAi$getName() {
        if (this.getNameId() != null && !this.getNameId().isEmpty()) {
            return this.getNameId();
        }
        if (this.deName != null) {
            return this.deName.get_name();
        }
        return super.getName();
    }

    @Override
    public String getName() {
        NativeDeInstanceState actor = this;
        return (String)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$getName());
    }

    private void sourceAi$updateNextActionTime(int n) {
        long l = System.currentTimeMillis() + (long)Math.max(0, n);
        this._deNextActionUntilMs = l;
    }

    public void updateNextActionTime(int n) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$updateNextActionTime(n);
            return null;
        });
    }

    private int sourceAi$calcSleepTime(int n, int n2) {
        int n3 = super.calcSleepTime(n, n2);
        if (n2 == 0) {
            return Math.max(100, n3);
        }
        return n3;
    }

    @Override
    public int calcSleepTime(int n, int n2) {
        NativeDeInstanceState actor = this;
        return (Integer)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$calcSleepTime(n, n2));
    }

    private void sourceAi$wakeup() {
        if (System.currentTimeMillis() < this._deNextActionUntilMs) {
            return;
        }
        long l = this.currentAiOwnerToken();
        if (l == 0L) {
            this.startAI();
            return;
        }
        this.wakeup(l);
    }

    @Override
    public void wakeup() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$wakeup();
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$wakeup(long l) {
        boolean bl = false;
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested) {
                return;
            }
            this._isBackgroundMode = false;
            NativeDeLiveAiScope.Manager.get().remove(this);
            if (this._deAiTickExecuting) {
                this._deAiWakeupRequested = true;
                return;
            }
            if (this._deAiTickPending) {
                if (this._deAiPendingBackground) {
                    this._deAiWakeupRequested = true;
                }
                return;
            }
            this._deAiTickPending = true;
            this._deAiPendingBackground = false;
            bl = true;
        }
        if (bl) {
            this.setSleepTime(300);
            this.scheduleAiTick(l, this.getSleepTime());
        }
    }

    private void wakeup(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$wakeup(l);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$startAI() {
        long l;
        if (this.isDead() || this.destroyed() || this.getCurrentHp() <= 0) {
            return;
        }
        synchronized (this._deAiScheduleLock) {
            l = this.tryAcquireAiOwner();
            if (l == 0L) {
                if (this._deAiStopRequested && this.currentAiOwnerToken() == this._deAiChainToken) {
                    this._deAiRestartRequested = true;
                }
                return;
            }
            this._deAiChainToken = l;
            this._deAiTickPending = true;
            this._deAiTickExecuting = false;
            this._deAiStopRequested = false;
            this._deAiWakeupRequested = false;
            this._deAiRestartRequested = false;
            this._deAiActiveTimerToken = 0L;
            this._deAiPendingBackground = false;
            this._deAiWorkerRetryAttempt = 0;
            this._runFuture = null;
            this._isBackgroundMode = false;
        }
        this.submitAiTick(l);
    }

    @Override
    public void startAI() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$startAI();
            return null;
        });
    }

    private boolean sourceAi$checkNeedActive() {
        if (this._aiType == 7 && !this._deathMatchArenaCombatEnabled) {
            return false;
        }
        long l = System.currentTimeMillis();
        boolean bl = !NativeDeLiveAiScope.World.get().getVisiblePlayer(this).isEmpty();
        boolean bl2 = this.isBusy();
        boolean bl3 = this._aiType == 2 || this._aiType == 5 || this._aiType == 6 || this._aiType == 7 && this._deathMatchArenaCombatEnabled;
        boolean bl4 = !NativeDeLiveAiScope.wakeupEnable();
        boolean bl5 = false;
        if (NativeDeLiveAiScope.wakeupEnable()) {
            long l2 = (long)NativeDeLiveAiScope.wakeupInterval() * 1000L;
            long l3 = (long)NativeDeLiveAiScope.wakeupDuration() * 1000L;
            if (l - this._lastWakeupTime < l3) {
                bl5 = true;
            } else if (l - this._lastWakeupTime > l2) {
                this._lastWakeupTime = l;
                bl5 = true;
            }
        }
        return bl4 || bl || bl2 || bl3 || bl5;
    }

    private boolean checkNeedActive() {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$checkNeedActive());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$onBackgroundTick() {
        long l = this.currentAiOwnerToken();
        boolean bl = false;
        synchronized (this._deAiScheduleLock) {
            if (this.isAiOwner(l) && this._deAiChainToken == l && !this._deAiStopRequested && this._isBackgroundMode && !this._deAiTickPending && !this._deAiTickExecuting) {
                this._deAiTickPending = true;
                this._deAiPendingBackground = true;
                bl = true;
            }
        }
        if (bl) {
            this.dispatchAiWork(l, true);
        }
    }

    public void onBackgroundTick() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$onBackgroundTick();
            return null;
        });
    }

    private void sourceAi$runBackgroundTick(long l) {
        if (!this.beginBackgroundTick(l)) {
            return;
        }
        try {
            if (!this._isBackgroundMode || this.checkNeedActive()) {
                this.wakeup(l);
                return;
            }
            if (this.isDeAiChain(l) && (NativeDeLiveAiPorts.Combat)this._combatBehavior != null) {
                ((NativeDeLiveAiPorts.Combat)this._combatBehavior).executeBackground();
            }
        }
        catch (Exception exception) {
            NativeDeLiveAiScope.log().error("onBackgroundTick \u7570\u5e38: " + this.getName(), exception);
        }
        catch (Error error) {
            this.stopAiOwner(l);
            throw error;
        }
        finally {
            this.finishAiTick(l);
        }
    }

    private void runBackgroundTick(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$runBackgroundTick(l);
            return null;
        });
    }

    private void sourceAi$run() {
        this.ensureAiScheduled();
    }

    @Override
    public void run() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$run();
            return null;
        });
    }

    private void sourceAi$runAiTick(long l) {
        if (!this.beginAiTick(l)) {
            return;
        }
        try {
            if (!this._spawnLocationChecked) {
                this.checkSpawnLocation();
                this._spawnLocationChecked = true;
            }
            if (this.isDead() || this.destroyed() || this.getCurrentHp() <= 0) {
                this.stopAiOwner(l);
                return;
            }
            if (this.isParalyzed() || this.isSleeped()) {
                this.setSleepTime(200);
                this.scheduleNextAiTick(l);
                return;
            }
            boolean bl = this.checkNeedActive();
            if (bl) {
                if (this._isBackgroundMode) {
                    this._isBackgroundMode = false;
                    NativeDeLiveAiScope.Manager.get().remove(this);
                }
                this.setNpcSpeed();
                this.setSleepTime(300);
                long l2 = this.getSleepTime();
                if ((NativeDeInstancePorts.Shop)this._shopBehavior != null) {
                    ((NativeDeInstancePorts.Shop)this._shopBehavior).start();
                } else if ((NativeDeLiveAiPorts.Fishing)this._fishingBehavior != null) {
                    l2 = ((NativeDeLiveAiPorts.Fishing)this._fishingBehavior).execute();
                } else if ((NativeDeLiveAiPorts.Combat)this._combatBehavior != null) {
                    l2 = ((NativeDeLiveAiPorts.Combat)this._combatBehavior).execute();
                }
                this.setSleepTime(this.normalizeDeSleepTime(l2));
                this.scheduleNextAiTick(l);
            } else {
                this.enterBackgroundMode(l);
            }
        }
        catch (Exception exception) {
            NativeDeLiveAiScope.log().error("AI \u7570\u5e38: " + this.getName(), exception);
            this.stopAiOwner(l);
        }
        catch (Error error) {
            this.stopAiOwner(l);
            throw error;
        }
        finally {
            this.finishAiTick(l);
        }
    }

    private void runAiTick(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$runAiTick(l);
            return null;
        });
    }

    private int sourceAi$normalizeDeSleepTime(long l) {
        if (l <= 0L) {
            return 300;
        }
        if (l > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)l;
    }

    private int normalizeDeSleepTime(long l) {
        NativeDeInstanceState actor = this;
        return (Integer)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$normalizeDeSleepTime(l));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean sourceAi$isDeAiChain(long l) {
        if (!this.isAiOwner(l)) {
            return false;
        }
        synchronized (this._deAiScheduleLock) {
            return this.isAiOwner(l) && this._deAiChainToken == l && !this._deAiStopRequested;
        }
    }

    private boolean isDeAiChain(long l) {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$isDeAiChain(l));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean sourceAi$beginAiTick(long l) {
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || !this._deAiTickPending || this._deAiPendingBackground || this._deAiTickExecuting || this._deAiStopRequested || this._deAiActiveTimerToken != 0L) {
                return false;
            }
            this._deAiTickPending = false;
            this._deAiTickExecuting = true;
            this._deAiWorkerRetryAttempt = 0;
            return true;
        }
    }

    private boolean beginAiTick(long l) {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$beginAiTick(l));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean sourceAi$beginBackgroundTick(long l) {
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || !this._deAiTickPending || !this._deAiPendingBackground || this._deAiTickExecuting || this._deAiStopRequested || this._deAiActiveTimerToken != 0L) {
                return false;
            }
            this._deAiTickPending = false;
            this._deAiTickExecuting = true;
            this._deAiWorkerRetryAttempt = 0;
            return true;
        }
    }

    private boolean beginBackgroundTick(long l) {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$beginBackgroundTick(l));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$finishAiTick(long l) {
        boolean bl = false;
        boolean bl2 = false;
        synchronized (this._deAiScheduleLock) {
            if (this._deAiChainToken != l) {
                return;
            }
            this._deAiTickExecuting = false;
            if (this._deAiStopRequested) {
                bl2 = this.completeAiStopLocked(l);
            } else if (this._deAiWakeupRequested) {
                this._deAiWakeupRequested = false;
                if (!this._deAiTickPending) {
                    this._isBackgroundMode = false;
                    NativeDeLiveAiScope.Manager.get().remove(this);
                    this._deAiTickPending = true;
                    this._deAiPendingBackground = false;
                    bl = true;
                }
            }
        }
        if (bl2) {
            this.startAI();
        } else if (bl) {
            this.dispatchAiWork(l, false);
        }
    }

    private void finishAiTick(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$finishAiTick(l);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$enterBackgroundMode(long l) {
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested) {
                return;
            }
            this._isBackgroundMode = true;
            this._deAiTickPending = false;
            this._deAiPendingBackground = false;
            this._deAiActiveTimerToken = 0L;
            this._runFuture = null;
            NativeDeLiveAiScope.Manager.get().add(this);
        }
    }

    private void enterBackgroundMode(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$enterBackgroundMode(l);
            return null;
        });
    }

    private void sourceAi$submitAiTick(long l) {
        this.dispatchAiWork(l, false);
    }

    private void submitAiTick(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$submitAiTick(l);
            return null;
        });
    }

    private ScheduledFuture<?> sourceAi$scheduleAiTick(long l, int n) {
        return this.scheduleAiTick(l, n, false);
    }

    private ScheduledFuture<?> scheduleAiTick(long l, int n) {
        NativeDeInstanceState actor = this;
        return (ScheduledFuture)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$scheduleAiTick(l, n));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ScheduledFuture<?> sourceAi$scheduleAiTick(long l, int n, boolean bl) {
        long l2;
        if (NativeDeLiveAiScope.Pool.get().isShutdown()) {
            this.stopAiOwner(l);
            return null;
        }
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested || !this._deAiTickPending || this._deAiPendingBackground != bl) {
                return null;
            }
            if (this._deAiActiveTimerToken != 0L) {
                return this._runFuture;
            }
            l2 = this.nextAiTimerTokenLocked();
            this._deAiActiveTimerToken = l2;
        }
        try {
            ScheduledFuture<?> scheduledFuture = NativeDeLiveAiScope.Clock.get().aiSchedule(() -> {
                NativeDeInstanceState actor = this;
                NativeDeLiveAiScope.call(this, () -> {
                    actor.sourceAi$lambda$scheduleAiTick$0(l, l2, bl);
                    return null;
                });
            }, Math.max(0, n), l);
            if (scheduledFuture != null) {
                boolean bl2 = false;
                synchronized (this._deAiScheduleLock) {
                    if (this.isAiOwner(l) && this._deAiChainToken == l && !this._deAiStopRequested && this._deAiActiveTimerToken == l2) {
                        this._runFuture = scheduledFuture;
                        bl2 = true;
                    }
                }
                if (!bl2) {
                    scheduledFuture.cancel(false);
                    return null;
                }
                return scheduledFuture;
            }
            NativeDeLiveAiScope.counter("AI_SCHEDULE_ERROR_COUNT").incrementAndGet();
            NativeDeLiveAiScope.log().warn("[L1DeInstance] AI schedule returned null: name={} delayMs={}", (Object)this.getName(), (Object)n);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            NativeDeLiveAiScope.counter("AI_SCHEDULE_REJECTED_COUNT").incrementAndGet();
            NativeDeLiveAiScope.log().warn("[L1DeInstance] AI schedule rejected: name={} delayMs={}", (Object)this.getName(), (Object)n, (Object)rejectedExecutionException);
        }
        catch (RuntimeException runtimeException) {
            NativeDeLiveAiScope.counter("AI_SCHEDULE_ERROR_COUNT").incrementAndGet();
            NativeDeLiveAiScope.log().warn("[L1DeInstance] AI schedule failed: name={} delayMs={}", (Object)this.getName(), (Object)n, (Object)runtimeException);
        }
        this.failAiTimer(l, l2);
        return null;
    }

    private ScheduledFuture<?> scheduleAiTick(long l, int n, boolean bl) {
        NativeDeInstanceState actor = this;
        return (ScheduledFuture)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$scheduleAiTick(l, n, bl));
    }

    private long sourceAi$nextAiTimerTokenLocked() {
        long l = ++this._deAiTimerSequence;
        if (l == 0L) {
            l = ++this._deAiTimerSequence;
        }
        return l;
    }

    private long nextAiTimerTokenLocked() {
        NativeDeInstanceState actor = this;
        return (Long)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$nextAiTimerTokenLocked());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$onAiTimer(long l, long l2, boolean bl) {
        boolean bl2 = false;
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested || this._deAiActiveTimerToken != l2 || !this._deAiTickPending || this._deAiPendingBackground != bl) {
                return;
            }
            this._deAiActiveTimerToken = 0L;
            this._runFuture = null;
            if (this._deAiTickExecuting) {
                this._deAiTickPending = false;
                this._deAiWakeupRequested = true;
                return;
            }
            bl2 = true;
        }
        if (bl2) {
            this.dispatchAiWork(l, bl);
        }
    }

    private void onAiTimer(long l, long l2, boolean bl) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$onAiTimer(l, l2, bl);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$failAiTimer(long l, long l2) {
        boolean bl = false;
        synchronized (this._deAiScheduleLock) {
            if (this.isAiOwner(l) && this._deAiChainToken == l && this._deAiActiveTimerToken == l2) {
                this._deAiActiveTimerToken = 0L;
                this._runFuture = null;
                bl = true;
            }
        }
        if (bl) {
            this.stopAiOwner(l);
        }
    }

    private void failAiTimer(long l, long l2) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$failAiTimer(l, l2);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$dispatchAiWork(long l, boolean bl) {
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested || !this._deAiTickPending || this._deAiPendingBackground != bl || this._deAiTickExecuting || this._deAiActiveTimerToken != 0L) {
                return;
            }
        }
        if (NativeDeLiveAiScope.Pool.get().tryExecute(new DeAiWorkerTask(l, bl))) {
            return;
        }
        long l2 = NativeDeLiveAiScope.counter("AI_SUBMIT_REJECTED_COUNT").incrementAndGet();
        if (NativeDeLiveAiScope.Pool.get().isShutdown()) {
            this.stopAiOwner(l);
            return;
        }
        int n = this.nextAiRetryDelay(l, bl);
        if (n < 0) {
            return;
        }
        NativeDeLiveAiScope.counter("AI_WORKER_RETRY_COUNT").incrementAndGet();
        if (l2 == 1L || (l2 & l2 - 1L) == 0L) {
            NativeDeLiveAiScope.log().warn("[L1DeInstance] NPC AI worker saturated; retrying: name={} delayMs={} rejected={}", (Object)this.getName(), (Object)n, (Object)l2);
        }
        this.scheduleAiTick(l, n, bl);
    }

    private void dispatchAiWork(long l, boolean bl) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$dispatchAiWork(l, bl);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private int sourceAi$nextAiRetryDelay(long l, boolean bl) {
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested || !this._deAiTickPending || this._deAiPendingBackground != bl) {
                return -1;
            }
            this._deAiWorkerRetryAttempt = Math.min(this._deAiWorkerRetryAttempt + 1, 31);
            int n = Math.min(5, this._deAiWorkerRetryAttempt - 1);
            return Math.min(1000, 25 << n);
        }
    }

    private int nextAiRetryDelay(long l, boolean bl) {
        NativeDeInstanceState actor = this;
        return (Integer)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$nextAiRetryDelay(l, bl));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$scheduleNextAiTick(long l) {
        if (NativeDeLiveAiScope.Pool.get().isShutdown()) {
            this.stopAiOwner(l);
            return;
        }
        boolean bl = false;
        synchronized (this._deAiScheduleLock) {
            if (this.isAiOwner(l) && this._deAiChainToken == l && !this._deAiStopRequested && !this._deAiTickPending && this._deAiActiveTimerToken == 0L) {
                this._deAiTickPending = true;
                this._deAiPendingBackground = false;
                bl = true;
            }
        }
        if (bl) {
            this.scheduleAiTick(l, this.getSleepTime());
        }
    }

    private void scheduleNextAiTick(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$scheduleNextAiTick(l);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$ensureAiScheduled() {
        long l = this.currentAiOwnerToken();
        if (l == 0L) {
            this.startAI();
            return;
        }
        boolean bl = false;
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l || this._deAiStopRequested) {
                return;
            }
            if (this._deAiTickExecuting) {
                this._deAiWakeupRequested = true;
                return;
            }
            if (!this._deAiTickPending) {
                this._deAiTickPending = true;
                this._deAiPendingBackground = false;
                bl = true;
            }
        }
        if (bl) {
            this.setSleepTime(300);
            this.scheduleAiTick(l, this.getSleepTime());
        }
    }

    private void ensureAiScheduled() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$ensureAiScheduled();
            return null;
        });
    }

    private void sourceAi$stopAI() {
        this.stopAiOwner(this.currentAiOwnerToken());
    }

    @Override
    public void stopAI() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$stopAI();
            return null;
        });
    }

    @Deprecated
    private void sourceAi$setAiRunning(boolean bl) {
        if (bl) {
            this.startAI();
        } else {
            this.stopAI();
        }
    }

    @Deprecated
    public void setAiRunning(boolean bl) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$setAiRunning(bl);
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sourceAi$stopAiOwner(long l) {
        if (l == 0L) {
            return;
        }
        synchronized (this._deAiScheduleLock) {
            if (!this.isAiOwner(l) || this._deAiChainToken != l) {
                return;
            }
            this._deAiStopRequested = true;
            this._deAiWakeupRequested = false;
            this._deAiTickPending = false;
            this._deAiPendingBackground = false;
            this._deAiActiveTimerToken = 0L;
            this._deAiWorkerRetryAttempt = 0;
            ScheduledFuture<?> scheduledFuture = this._runFuture;
            this._runFuture = null;
            if (scheduledFuture != null && !scheduledFuture.isDone()) {
                scheduledFuture.cancel(false);
            }
            if (!this._deAiTickExecuting) {
                this.completeAiStopLocked(l);
            }
        }
    }

    private void stopAiOwner(long l) {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$stopAiOwner(l);
            return null;
        });
    }

    private boolean sourceAi$completeAiStopLocked(long l) {
        if (!this.isAiOwner(l) || this._deAiChainToken != l) {
            return false;
        }
        boolean bl = this._deAiRestartRequested;
        this._runFuture = null;
        this._deAiTickPending = false;
        this._deAiTickExecuting = false;
        this._deAiPendingBackground = false;
        this._deAiActiveTimerToken = 0L;
        this._deAiWorkerRetryAttempt = 0;
        this._deAiStopRequested = false;
        this._deAiWakeupRequested = false;
        this._deAiRestartRequested = false;
        this._deAiChainToken = 0L;
        this._isBackgroundMode = false;
        this._deNextActionUntilMs = 0L;
        NativeDeLiveAiScope.Manager.get().remove(this);
        this.releaseAiOwner(l);
        return bl;
    }

    private boolean completeAiStopLocked(long l) {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$completeAiStopLocked(l));
    }

    private void sourceAi$setNpcSpeed() {
        try {
            int n = NativeDeLiveAiScope.Sprites.get().getAttackSpeed(this.getTempCharGfx(), this.getCurrentWeapon() + 1);
            this.setAtkspeed(n);
            int n2 = NativeDeLiveAiScope.Sprites.get().getMoveSpeed(this.getTempCharGfx(), this.getCurrentWeapon());
            this.setPassispeed(n2);
            if (this.getPassispeed() <= 0) {
                this.setPassispeed(640);
                NativeDeLiveAiScope.log().warn("L1DeInstance.setNpcSpeed: \u5047\u4eba " + this.getName() + " \u79fb\u52d5\u901f\u5ea6\u7570\u5e38\uff0c\u5f37\u5236\u8a2d\u5b9a\u70ba 640");
            }
            if (this.getAtkspeed() <= 0) {
                this.setAtkspeed(900);
                NativeDeLiveAiScope.log().warn("L1DeInstance.setNpcSpeed: \u5047\u4eba " + this.getName() + " \u653b\u64ca\u901f\u5ea6\u7570\u5e38\uff0c\u5f37\u5236\u8a2d\u5b9a\u70ba 900");
            }
            if (!this.getDolls().isEmpty()) {
                for (Object object : this.getDolls().values().toArray()) {
                    NativeDeLiveAiPorts.Doll doll = (NativeDeLiveAiPorts.Doll)object;
                    if (doll != null) {
                        doll.setNpcMoveSpeed();
                    }
                }
            }
        }
        catch (Exception exception) {
            NativeDeLiveAiScope.log().error(exception.getLocalizedMessage(), exception);
        }
    }

    public void setNpcSpeed() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$setNpcSpeed();
            return null;
        });
    }

    private boolean sourceAi$isBusy() {
        if ((NativeDeLiveAiPorts.Combat)this._combatBehavior != null && ((NativeDeLiveAiPorts.Combat)this._combatBehavior).hasTarget()) {
            return true;
        }
        return false;
    }

    public boolean isBusy() {
        NativeDeInstanceState actor = this;
        return (Boolean)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$isBusy());
    }

    private int sourceAi$getSleepTime() {
        return super.getSleepTime();
    }

    @Override
    public int getSleepTime() {
        NativeDeInstanceState actor = this;
        return (Integer)NativeDeLiveAiScope.call(this, () -> actor.sourceAi$getSleepTime());
    }

    private void sourceAi$checkSpawnLocation() {
        try {
            if (NativeDeLiveAiScope.isPassable(this.getMap(), this.getX(), this.getY(), null)) {
                return;
            }
            for (int i = 1; i <= 3; ++i) {
                for (int j = 0; j < 8; ++j) {
                    int n = this.getX() + NativeDeLiveAiScope.HEADING_TABLE_X[j] * i;
                    int n2 = this.getY() + NativeDeLiveAiScope.HEADING_TABLE_Y[j] * i;
                    if (NativeDeLiveAiScope.isPassable(this.getMap(), n, n2, null)) {
                    java.util.Collection<NativeDeDeletionPorts.Viewer> arrayList = NativeDeLiveAiScope.World.get().getRecognizePlayer(this);
                    if (!NativeDeLiveAiScope.World.get().relocateVisibleObject(this, n, n2, this.getMapId())) {
                        NativeDeLiveAiScope.log().warn("\u5047\u4eba\u51fa\u751f\u9ede\u4fee\u6b63\u5931\u6557 name={} target=({}, {}, map={})", (Object)this.getName(), (Object)n, (Object)n2, (Object)this.getMapId());
                        return;
                    }
                    for (NativeDeDeletionPorts.Viewer viewer : arrayList) {
                        NativeDeLiveAiScope.send(viewer, new NativeDeDeletionPorts.RemoveObject(this));
                        NativeDeLiveAiScope.removeKnown(viewer, this);
                        NativeDeLiveAiScope.update(viewer);
                    }
                    return;
                    }
                }
            }
            if (this.getHomeX() != 0 && this.getHomeY() != 0 && !NativeDeLiveAiScope.World.get().relocateVisibleObject(this, this.getHomeX(), this.getHomeY(), this.getMapId())) {
                NativeDeLiveAiScope.log().warn("\u5047\u4eba\u51fa\u751f\u9ede\u56de\u5fa9\u5931\u6557 name={} home=({}, {}, map={})", (Object)this.getName(), (Object)this.getHomeX(), (Object)this.getHomeY(), (Object)this.getMapId());
            }
        }
        catch (Exception exception) {
            NativeDeLiveAiScope.log().error("\u6aa2\u67e5\u51fa\u751f\u9ede\u7570\u5e38", exception);
        }
    }

    private void checkSpawnLocation() {
        NativeDeInstanceState actor = this;
        NativeDeLiveAiScope.call(this, () -> {
            actor.sourceAi$checkSpawnLocation();
            return null;
        });
    }

    private /* synthetic */ void sourceAi$lambda$scheduleAiTick$0(long l, long l2, boolean bl) {
        this.onAiTimer(l, l2, bl);
    }

    @Override
    public void deleteMe() {
        try {
            super.deleteMe();
            return;
        }
        finally {
            NativeDeLiveAiJobs.retireDeleted(this);
        }
    }

    public static final class DeRepathThrottle {
        public long lastRepathTimeMs;
        public int lastRepathTargetId;
        public int lastRepathGoalX;
        public int lastRepathGoalY;
        public int cachedChaseDir = -1;
        public int consecutiveMoveFail;
        public long repathCooldownUntilMs;
    }

    private final class DeAiWorkerTask
    implements NativeDeLiveAiScope.Pool.ShutdownDiscardable {
        private final long _ownerToken;
        private final boolean _background;

        private DeAiWorkerTask(long l, boolean bl) {
            this._ownerToken = l;
            this._background = bl;
        }

        @Override
        public void run() {
            if (NativeDeLiveAiScope.Pool.get().isShutdown()) {
                NativeDeInstanceState.this.stopAiOwner(this._ownerToken);
                return;
            }
            if (this._background) {
                NativeDeInstanceState.this.runBackgroundTick(this._ownerToken);
            } else {
                NativeDeInstanceState.this.runAiTick(this._ownerToken);
            }
        }

        @Override
        public void discardOnShutdown() {
            NativeDeInstanceState.this.stopAiOwner(this._ownerToken);
        }
    }
}
