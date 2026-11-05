// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeChatAccess;
import local.lito381.server.NativeDeChatData;
import local.lito381.server.NativeDeChatRuntime;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeChatTable;
import local.lito381.server.NativeDeChatTimer;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionPorts;
import local.lito381.server.NativeDeNpcHateList;
import local.lito381.server.NativeDeNpcInventory;
import local.lito381.server.NativeDeNpcMoveExecutor;
import local.lito381.server.NativeDeNpcMoveState;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplatePorts;
import local.lito381.server.NativeDeNpcTemplateState;
import local.lito381.server.NativeDeObjectBindings;
import local.lito381.server.NativeDePerceptionPorts;
import local.lito381.server.NativeDePerceptionSpeed;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeWorldKinds;

public class NativeDeNpcConstructionState
extends NativeDeNpcTemplateState {
    private int _petcost;
    private boolean firstFound;
    private int _pathFailCount;
    private NativeDeCharacterFields _pendingFamilyTarget;
    private int _randomMoveDistance;
    private int _randomMoveDirection;
    private volatile int _prevX;
    private volatile int _prevY;
    private volatile long _lastMoveTimeMs;
    protected boolean ISASCAPE;
    private final AtomicLong _aiGeneration;
    private boolean _actived;
    private boolean _firstAttack;
    private int _sleep_time;
    protected NativeDeCharacterFields _target;
    protected NativeDeWorldKinds.Item _targetItem;
    private volatile boolean _deathProcessing;
    protected NativeDeNpcMoveExecutor _npcMove;
    private int _homeX;
    private int _homeY;
    private int _hiddenStatus;
    private int _movementDistance;
    private volatile ScheduledFuture<?> _monitorFuture;
    private boolean _rest;
    private boolean _isResurrect;
    private int _mobGroupId;
    private int _bowActId;
    private int _ranged;
    private long _existDeleteDeadlineNanos;
    private boolean _existenceDeletionClaimed;
    private boolean _isremovearmor;
    private int _stop_time;
    private int _work_time;
    final NativeDeNpcConstructionPorts.Services nativeServices;

    public NativeDeNpcConstructionState(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner, NativeDePerceptionPorts.Services services, NativeDeNpcTemplatePorts.Services services2, NativeDeNpcConstructionPorts.Services services3, NativeDeNpcTemplate nativeDeNpcTemplate) {
        super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner, services, services2, null);
        this.nativeServices = Objects.requireNonNull(services3);
        this.setNameId(null);
        this.inventory = new NativeDeNpcInventory(this.nativeServices);
        this.firstFound = true;
        this._pathFailCount = 0;
        this._pendingFamilyTarget = null;
        this._randomMoveDistance = 0;
        this._randomMoveDirection = 0;
        this._prevX = 0;
        this._prevY = 0;
        this._lastMoveTimeMs = 0L;
        this.ISASCAPE = false;
        this._aiGeneration = new AtomicLong(0L);
        this._actived = false;
        this._firstAttack = false;
        this._hateList = new NativeDeNpcHateList();
        this._dropHateList = new NativeDeNpcHateList();
        this._target = null;
        this._targetItem = null;
        this._deathProcessing = false;
        this._npcMove = null;
        this._movementDistance = 0;
        this._rest = false;
        this._isResurrect = false;
        this._mobGroupId = 0;
        this._bowActId = -1;
        this._ranged = -1;
        this._existDeleteDeadlineNanos = -1L;
        this._isremovearmor = false;
        this._stop_time = -1;
        this._work_time = -1;
        this.setStatus(0);
        this.setMoveSpeed(0);
        this.setDead(false);
        this.setStatus(0);
        this.setreSpawn(false);
        this._npcMove = new NativeDeNpcMoveState(this, this.nativeServices);
        if (nativeDeNpcTemplate != null) {
            this.setting_template(nativeDeNpcTemplate);
        }
    }

    protected final void resetPathFailureState() {
        this._pathFailCount = 0;
        this._pendingFamilyTarget = null;
    }

    public int getPrevX() {
        return this._prevX;
    }

    public int getPrevY() {
        return this._prevY;
    }

    public long getLastMoveTimeMs() {
        return this._lastMoveTimeMs;
    }

    public void onItemUse() {
    }

    public void searchTarget() {
    }

    public void setLink(NativeDeCharacterFields nativeDeCharacterFields) {
    }

    protected void deferFamilyLinkFromPcAttacker(NativeDeDeletionPorts.Viewer viewer) {
        if (viewer != null) {
            this._pendingFamilyTarget = viewer;
        }
    }

    public void onFinalAction(NativeDeDeletionPorts.Viewer viewer, String string) {
    }

    @Override
    public void allTargetClear() {
        if (this._npcMove != null) {
            this._npcMove.clear();
        }
        this._pathFailCount = 0;
        this._pendingFamilyTarget = null;
        ((NativeDeNpcHateList)this._hateList).clear();
        ((NativeDeNpcHateList)this._dropHateList).clear();
        this._target = null;
        this._targetItemLock.lock();
        try {
            this._targetItemList.clear();
            this._targetItem = null;
        }
        finally {
            this._targetItemLock.unlock();
        }
    }

    public void onNpcAI() {
    }

    private String newName(int n) {
        String string = "";
        return string;
    }

    @Override
    public NativeDeNpcInventory getInventory() {
        return (NativeDeNpcInventory)this.inventory;
    }

    public void setInventory(NativeDeNpcInventory nativeDeNpcInventory) {
        this.inventory = nativeDeNpcInventory;
    }

    @Override
    public int getNpcId() {
        return ((NativeDeNpcTemplate)this.template).get_npcId();
    }

    public void setPetcost(int n) {
        this._petcost = n;
    }

    public int getPetcost() {
        return this._petcost;
    }

    @Override
    protected void onDeleteClaimed() {
    }

    public int commitManaDamage(NativeDeCharacterFields nativeDeCharacterFields, int n) {
        return 0;
    }

    public void receiveDamage(NativeDeCharacterFields nativeDeCharacterFields, int n) {
    }

    public NativeDeCharacterFields is_now_target() {
        return this._target;
    }

    public NativeDeWorldKinds.Item is_now_targetItem() {
        return this._targetItem;
    }

    public void set_now_targetItem(NativeDeWorldKinds.Item item) {
        this._targetItem = item;
    }

    public NativeDeNpcMoveExecutor getMove() {
        return this._npcMove;
    }

    public int getHomeX() {
        return this._homeX;
    }

    public void setHomeX(int n) {
        this._homeX = n;
    }

    public int getHomeY() {
        return this._homeY;
    }

    public void setHomeY(int n) {
        this._homeY = n;
    }

    public void setreSpawn(boolean bl) {
        this.respawn = bl;
    }

    public int getHiddenStatus() {
        return this._hiddenStatus;
    }

    public void setHiddenStatus(int n) {
        this._hiddenStatus = n;
    }

    public int getMovementDistance() {
        return this._movementDistance;
    }

    public void setMovementDistance(int n) {
        this._movementDistance = n;
    }

    protected void setActived(boolean bl) {
        this._actived = bl;
    }

    protected boolean isActived() {
        return this._actived;
    }

    protected void setFirstAttack(boolean bl) {
        this._firstAttack = bl;
    }

    protected boolean isFirstAttack() {
        return this._firstAttack;
    }

    public void setSleepTime(int n) {
        this._sleep_time = n;
    }

    protected int getSleepTime() {
        return this._sleep_time;
    }

    protected boolean isDeathProcessing() {
        return this._deathProcessing;
    }

    public void setRest(boolean bl) {
        this._rest = bl;
    }

    public boolean isRest() {
        return this._rest;
    }

    public boolean isResurrect() {
        return this._isResurrect;
    }

    public void setResurrect(boolean bl) {
        this._isResurrect = bl;
    }

    public int getMobGroupId() {
        return this._mobGroupId;
    }

    public void setMobGroupId(int n) {
        this._mobGroupId = n;
    }

    public void setBowActId(int n) {
        this._bowActId = n;
    }

    public void set_ranged(int n) {
        this._ranged = n;
    }

    public boolean isremovearmor() {
        return this._isremovearmor;
    }

    public void set_removearmor(boolean bl) {
        this._isremovearmor = bl;
    }

    public int get_stop_time() {
        return this._stop_time;
    }

    public void set_work_time(int n) {
        this._work_time = n;
    }

    public int get_work_time() {
        return this._work_time;
    }

    private /* synthetic */ void lambda$setDeathProcessing$3(boolean bl) {
        this._deathProcessing = bl;
    }

    @Override
    public int getX() {
        return NativeDeObjectBindings.x(this);
    }

    @Override
    public int getY() {
        return NativeDeObjectBindings.y(this);
    }

    @Override
    public short getMapId() {
        return NativeDeObjectBindings.mapId(this);
    }

    public int calcSleepTime(int n, int n2) {
        if (n <= 0) {
            n = 960;
        }
        NativeDePerceptionSpeed.ActionType action;
        switch (n2) {
            case 0: action = NativeDePerceptionSpeed.ActionType.MOVE; break;
            case 1: action = NativeDePerceptionSpeed.ActionType.ATTACK; break;
            case 2:
            default: action = NativeDePerceptionSpeed.ActionType.SPELL;
        }
        return NativeDePerceptionSpeed.calculate(this, n, action);
    }

    protected final long tryAcquireAiOwner() {
        if (this._aiOwnerToken.get() != 0L) {
            return 0L;
        }
        long l = this._aiGeneration.incrementAndGet();
        if (l == 0L) {
            l = this._aiGeneration.incrementAndGet();
        }
        return this._aiOwnerToken.compareAndSet(0L, l) ? l : 0L;
    }

    protected final boolean isAiOwner(long l) {
        return l != 0L && this._aiOwnerToken.get() == l;
    }

    protected final boolean releaseAiOwner(long l) {
        return l != 0L && this._aiOwnerToken.compareAndSet(l, 0L);
    }

    protected final long currentAiOwnerToken() {
        return this._aiOwnerToken.get();
    }

    public void startChat(int n) {
        NativeDeChatRuntime.start(this, n);
    }

    private void sourceChat$startChat(int n) {
        if (n == 0 && NativeDeChatAccess.L1NpcInstance$isDead(this)) {
            return;
        }
        if (n == 1 && !NativeDeChatAccess.L1NpcInstance$isDead(this)) {
            return;
        }
        if (n == 2 && NativeDeChatAccess.L1NpcInstance$isDead(this)) {
            return;
        }
        if (n == 3 && NativeDeChatAccess.L1NpcInstance$isDead(this)) {
            return;
        }
        int n2 = NativeDeChatAccess.L1NpcInstance$getNpcTemplate(this).get_npcId();
        NativeDeChatData nativeDeChatData = null;
        switch (n) {
            case 0: {
                nativeDeChatData = NativeDeChatTable.get().getTemplateAppearance(n2);
                break;
            }
            case 1: {
                nativeDeChatData = NativeDeChatTable.get().getTemplateDead(n2);
                break;
            }
            case 2: {
                nativeDeChatData = NativeDeChatTable.get().getTemplateHide(n2);
                break;
            }
            case 3: {
                nativeDeChatData = NativeDeChatTable.get().getTemplateGameTime(n2);
            }
        }
        if (nativeDeChatData == null) {
            return;
        }
        this.cancelNpcChatTimer();
        NativeDeChatTimer nativeDeChatTimer = new NativeDeChatTimer(this, nativeDeChatData);
        this._monitorFuture = !nativeDeChatData.isRepeat() ? this.scheduleNpcChatTimer(nativeDeChatTimer, nativeDeChatData.getStartDelayTime(), 0L) : this.scheduleNpcChatTimer(nativeDeChatTimer, nativeDeChatData.getStartDelayTime(), nativeDeChatData.getRepeatInterval());
    }

    private ScheduledFuture<?> scheduleNpcChatTimer(NativeDeChatTimer nativeDeChatTimer, long l, long l2) {
        try {
            if (l2 <= 0L && l <= 0L) {
                NativeDeChatScope.Clock.get().execute(nativeDeChatTimer);
                return null;
            }
            ScheduledFuture<?> scheduledFuture = l2 <= 0L ? NativeDeChatScope.Clock.get().aiSchedule(nativeDeChatTimer, l) : NativeDeChatScope.Clock.get().aiScheduleAtFixedRate(nativeDeChatTimer, l, l2);
            if (scheduledFuture != null) {
                return scheduledFuture;
            }
            NativeDeChatScope.counter("com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("[L1NpcInstance] npc chat schedule returned null: npc={} delay={} repeat={}", (Object)NativeDeChatAccess.L1NpcInstance$getNameId(this), (Object)l, (Object)l2);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            NativeDeChatScope.counter("com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_REJECTED_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("[L1NpcInstance] npc chat schedule rejected: npc={} delay={} repeat={}", (Object)NativeDeChatAccess.L1NpcInstance$getNameId(this), (Object)l, (Object)l2, (Object)rejectedExecutionException);
        }
        catch (RuntimeException runtimeException) {
            NativeDeChatScope.counter("com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("[L1NpcInstance] npc chat schedule failed: npc={} delay={} repeat={}", (Object)NativeDeChatAccess.L1NpcInstance$getNameId(this), (Object)l, (Object)l2, (Object)runtimeException);
        }
        return null;
    }

    @Override
    // 正式為 private，但父類同名 package 方法使 Java 無法編譯；此差異待 #73 裁定。
    void cancelNpcChatTimer() {
        NativeDeChatRuntime.cancel(this);
    }

    private void sourceChat$cancelNpcChatTimer() {
        if (this._monitorFuture != null) {
            NativeDeChatScope.counter("com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_CANCEL_COUNT").incrementAndGet();
            NativeDeChatScope.Clock.get().cancel(this._monitorFuture, false);
            this._monitorFuture = null;
        }
    }
}
