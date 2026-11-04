// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import local.lito381.server.NativeDeCharacterControls;
import local.lito381.server.NativeDeConstructionPorts;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDePerceptionPorts;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeVisibilityAccess;
import local.lito381.server.NativeDeVisibilityHp;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeVisibilityScope;

class NativeDeCharacterFields
extends NativeDeObjectFields {
    static final int MAX_HP_MP_RO = 999999;
    static final int MAX_HP_MP_STANDARD = Short.MAX_VALUE;
    static final HashMap<Integer, Integer> SKU1 = new HashMap<Integer, Integer>();
    static final HashMap<Integer, Integer> SKU2 = new HashMap<Integer, Integer>();
    static final HashMap<Integer, Integer> SKD1 = new HashMap<Integer, Integer>();
    static final HashMap<Integer, Integer> SKD2 = new HashMap<Integer, Integer>();
    static final HashMap<Integer, Integer> SKD3 = new HashMap<Integer, Integer>();
    private final int clientType;
    private final int objectId;
    private final ReentrantLock _npcVitalMutationLock;
    private final ConcurrentLinkedQueue<Runnable> _vitalCompletions;
    private final AtomicBoolean _vitalCompletionDraining;
    private static final ThreadLocal<ArrayDeque<DamageMutationReceipt>> DAMAGE_MUTATION_RECEIPTS = new ThreadLocal();
    private volatile int _currentHp;
    private volatile int _currentMp;
    private volatile int _maxHp = 0;
    private volatile int _trueMaxHp = 0;
    private volatile int _maxMp = 0;
    private volatile int _trueMaxMp = 0;
    private volatile int _status;
    private volatile boolean _isDead;
    private int _level = 1;
    private int _ac = 10;
    private int _trueAc = 0;
    private int _lawful;
    private int _dmgup = 0;
    private int _trueDmgup = 0;
    private int _hitup = 0;
    private int _trueHitup = 0;
    private int _sp = 0;
    private int _tempCharGfx;
    private int _gfxid;
    private short _str = 0;
    private short _trueStr = 0;
    private short _con = 0;
    private short _trueCon = 0;
    private short _dex = 0;
    private short _trueDex = 0;
    private short _cha = 0;
    private short _trueCha = 0;
    private short _int = 0;
    private short _trueInt = 0;
    private short _wis = 0;
    private short _trueWis = 0;
    private static final ErrorLog _log = new ErrorLog();
    private volatile NativeDePerceptionPorts.Poison _poison = null;
    private volatile boolean _sleeped;
    final Map<Integer, NativeDeDeletionState> pets = new ConcurrentHashMap<Integer, NativeDeDeletionState>();
    private final ConcurrentHashMap<Integer, NativeDeConstructionPorts.SkillTimer> _skillEffect = new ConcurrentHashMap();
    private final ReentrantLock _skillEffectLock = new ReentrantLock();
    private volatile SkillEffectSnapshot _skillEffectSnapshot = SkillEffectSnapshot.EMPTY;
    @Deprecated
    protected volatile int _combatMeleeHitBonus = 0;
    @Deprecated
    protected volatile int _combatRangedHitBonus = 0;
    @Deprecated
    protected volatile int _combatMeleeCookingDmgBonus = 0;
    @Deprecated
    protected volatile int _combatRangedCookingDmgBonus = 0;
    @Deprecated
    protected volatile int _combatDmgBonus = 0;
    @Deprecated
    protected volatile boolean _combatDmgHalve = false;
    private final Map<Integer, NativeDeConstructionPorts.ItemDelayTimer> _itemdelay = new ConcurrentHashMap<Integer, NativeDeConstructionPorts.ItemDelayTimer>();
    private final ConcurrentHashMap<Integer, NativeDeConstructionPorts.CubeLease> _cubeLeases = new ConcurrentHashMap();
    private final Map<Integer, NativeDeConstructionPorts.Follower> _followerlist = new ConcurrentHashMap<Integer, NativeDeConstructionPorts.Follower>();
    private final ConcurrentHashMap<Integer, ViewerHpState> _secHpByViewer = new ConcurrentHashMap();
    private volatile NativeDeConstructionPorts.DamageQueue _damageCommitQueue;
    private final ReentrantLock _damageCommitQueueLock;
    private volatile boolean _paralyzed;
    private volatile NativeDeConstructionPorts.Paralysis _paralysis;
    private final AtomicReference<Object> _skillDelayToken;
    private final AtomicLong _skillActionReadyAt;
    final Map<Integer, NativeDeDeletionPorts.Doll> dolls;
    private long _exp;
    private final CopyOnWriteArrayList<NativeDeDeletionPorts.ObjectRef> _knownObjects;
    private final CopyOnWriteArrayList<NativeDeDeletionPorts.Viewer> _knownPlayer;
    private final ReentrantLock _knownObjectLock;
    String name;
    private int _wind = 0;
    private int _trueWind = 0;
    private int _water = 0;
    private int _trueWater = 0;
    private int _fire = 0;
    private int _trueFire = 0;
    private int _earth = 0;
    private int _trueEarth = 0;
    private int _addAttrKind;
    private int _registStun = 0;
    private int _trueRegistStun = 0;
    private int _registStone = 0;
    private int _trueRegistStone = 0;
    private int _registSleep = 0;
    private int _trueRegistSleep = 0;
    private int _registFreeze = 0;
    private int _trueRegistFreeze = 0;
    private int _registSustain = 0;
    private int _trueRegistSustain = 0;
    private int _registBlind = 0;
    private int _trueRegistBlind = 0;
    private int _bowDmgup = 0;
    private int _trueBowDmgup = 0;
    private int _bowHitup = 0;
    private int _trueBowHitup = 0;
    private int _stunSuccess = 0;
    private int _mr = 0;
    private int _trueMr = 0;
    private String _title;
    private volatile int _heading;
    private int _moveSpeed;
    private int _braveSpeed;
    private int _karma;
    private int _chaLightSize;
    private int _ownLightSize;
    private int _tmp;
    private int _tmp_mr;
    private int _dodge_up = 0;
    private int _dodge_down = 0;
    private boolean _decay_potion = false;

    NativeDeCharacterFields(int n, int n2) {
        this._npcVitalMutationLock = new ReentrantLock();
        this._vitalCompletions = new ConcurrentLinkedQueue();
        this._vitalCompletionDraining = new AtomicBoolean();
        this._damageCommitQueueLock = new ReentrantLock();
        this._skillDelayToken = new AtomicReference();
        this._skillActionReadyAt = new AtomicLong(0L);
        this.dolls = new ConcurrentHashMap<Integer, NativeDeDeletionPorts.Doll>();
        this._knownObjects = new CopyOnWriteArrayList();
        this._knownPlayer = new CopyOnWriteArrayList();
        this._knownObjectLock = new ReentrantLock();
        this.clientType = n;
        this.objectId = n2;
    }

    private boolean isROClient() {
        return this.clientType == 2;
    }

    @Override
    protected int getId() {
        return this.objectId;
    }

    private static int ensure(int n, int n2, int n3) {
        int n4 = n2 <= n ? n : n2;
        return n4 <= n3 ? n4 : n3;
    }

    public int getCurrentHp() {
        return this._currentHp;
    }

    private ReentrantLock vitalMutationLock() {
        return this._npcVitalMutationLock;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected final <T> T withVitalMutation(Supplier<T> supplier) {
        if (supplier == null) {
            throw new NullPointerException("mutation");
        }
        ReentrantLock reentrantLock = this.vitalMutationLock();
        reentrantLock.lock();
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            reentrantLock.unlock();
            if (!reentrantLock.isHeldByCurrentThread()) {
                this.drainVitalCompletionsAfterActionUnlock();
            }
        }
    }

    protected final void withVitalMutation(Runnable runnable) {
        this.withVitalMutation(() -> {
            runnable.run();
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected final DamageMutationReceipt traceDamageMutations(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("mutation");
        }
        ArrayDeque<DamageMutationReceipt> arrayDeque = DAMAGE_MUTATION_RECEIPTS.get();
        if (arrayDeque == null) {
            arrayDeque = new ArrayDeque();
            DAMAGE_MUTATION_RECEIPTS.set(arrayDeque);
        }
        DamageMutationReceipt damageMutationReceipt = new DamageMutationReceipt(this);
        arrayDeque.push(damageMutationReceipt);
        try {
            runnable.run();
        }
        finally {
            if (arrayDeque.peek() == damageMutationReceipt) {
                arrayDeque.pop();
            } else {
                arrayDeque.remove(damageMutationReceipt);
            }
            if (arrayDeque.isEmpty()) {
                DAMAGE_MUTATION_RECEIPTS.remove();
            }
        }
        return damageMutationReceipt;
    }

    private void recordDamageHpMutation(int n, int n2) {
        if (n == n2) {
            return;
        }
        ArrayDeque<DamageMutationReceipt> arrayDeque = DAMAGE_MUTATION_RECEIPTS.get();
        if (arrayDeque == null) {
            return;
        }
        for (DamageMutationReceipt damageMutationReceipt : arrayDeque) {
            if (damageMutationReceipt._target != this) continue;
            damageMutationReceipt.recordHp(n, n2);
            return;
        }
    }

    private void recordDamageDeathMutation(boolean bl, boolean bl2) {
        if (bl == bl2) {
            return;
        }
        ArrayDeque<DamageMutationReceipt> arrayDeque = DAMAGE_MUTATION_RECEIPTS.get();
        if (arrayDeque == null) {
            return;
        }
        for (DamageMutationReceipt damageMutationReceipt : arrayDeque) {
            if (damageMutationReceipt._target != this) continue;
            damageMutationReceipt.recordDead(bl, bl2, this._currentHp);
            return;
        }
    }

    private static int boundedVitalValue(int n, long l, int n2, int n3) {
        long l2;
        int n4 = Math.max(0, n3);
        int n5 = Math.max(0, Math.min(n2, n4));
        try {
            l2 = Math.addExact((long)n, l);
        }
        catch (ArithmeticException arithmeticException) {
            return l >= 0L ? n4 : n5;
        }
        return (int)Math.max((long)n5, Math.min((long)n4, l2));
    }

    protected CurrentHpMutation commitCurrentHpLocked(int n) {
        int n2 = this._currentHp;
        if (!this.isCurrentHpMutationAllowed()) {
            return new CurrentHpMutation(n2, n2);
        }
        int n3 = Math.max(0, Math.min(n, Math.max(0, this.getMaxHp())));
        if (this.isDead() && n3 > 0) {
            return new CurrentHpMutation(n2, n2);
        }
        this._currentHp = n3;
        this.recordDamageHpMutation(n2, n3);
        return new CurrentHpMutation(n2, n3);
    }

    protected CurrentMpMutation commitCurrentMpLocked(int n) {
        int n2;
        int n3 = this._currentMp;
        if (!this.isCurrentMpMutationAllowed()) {
            return new CurrentMpMutation(n3, n3);
        }
        this._currentMp = n2 = Math.max(0, Math.min(n, Math.max(0, this.getMaxMp())));
        return new CurrentMpMutation(n3, n2);
    }

    protected boolean isCurrentHpMutationAllowed() {
        return true;
    }

    protected boolean isCurrentMpMutationAllowed() {
        return true;
    }

    protected void afterCurrentHpMutation(CurrentHpMutation currentHpMutation) {
    }

    protected void afterCurrentMpMutation(CurrentMpMutation currentMpMutation) {
    }

    private CurrentHpMutation enqueueCurrentHpCompletionLocked(CurrentHpMutation currentHpMutation) {
        if (currentHpMutation.completionRequired()) {
            this._vitalCompletions.add(() -> this.afterCurrentHpMutation(currentHpMutation));
        }
        return currentHpMutation;
    }

    private CurrentMpMutation enqueueCurrentMpCompletionLocked(CurrentMpMutation currentMpMutation) {
        if (currentMpMutation.completionRequired()) {
            this._vitalCompletions.add(() -> this.afterCurrentMpMutation(currentMpMutation));
        }
        return currentMpMutation;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    final void drainVitalCompletionsAfterActionUnlock() {
        ReentrantLock reentrantLock = this.vitalMutationLock();
        block8: do {
            if (reentrantLock.isHeldByCurrentThread() || !this._vitalCompletionDraining.compareAndSet(false, true)) {
                return;
            }
            try {
                block9: while (true) {
                    Runnable runnable;
                    ArrayDeque<Runnable> arrayDeque = new ArrayDeque<Runnable>();
                    reentrantLock.lock();
                    try {
                        while ((runnable = this._vitalCompletions.poll()) != null) {
                            arrayDeque.addLast(runnable);
                        }
                    }
                    finally {
                        reentrantLock.unlock();
                    }
                    if (arrayDeque.isEmpty()) {
                        continue block8;
                    }
                    while (true) {
                        if ((runnable = (Runnable)arrayDeque.pollFirst()) == null) continue block9;
                        try {
                            runnable.run();
                        }
                        catch (Throwable throwable) {
                            this.reportCompletionFailure(throwable);
                        }
                    }
                }
            }
            finally {
                this._vitalCompletionDraining.set(false);
            }
        } while (!this._vitalCompletions.isEmpty());
    }

    public final int adjustCurrentHp(long l) {
        return this.adjustCurrentHp(l, 0);
    }

    public final int adjustCurrentHp(long l, int n) {
        CurrentHpMutation currentHpMutation = this.withVitalMutation(() -> {
            int n2 = NativeDeCharacterFields.boundedVitalValue(this._currentHp, l, n, this.getMaxHp());
            return this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(n2));
        });
        return currentHpMutation.currentHp();
    }

    public final int adjustCurrentHpIfAlive(long l, int n) {
        CurrentHpMutation currentHpMutation = this.withVitalMutation(() -> {
            if (this._currentHp <= 0 || this.isDead()) {
                return this.enqueueCurrentHpCompletionLocked(new CurrentHpMutation(this._currentHp, this._currentHp));
            }
            int n2 = NativeDeCharacterFields.boundedVitalValue(this._currentHp, l, Math.min(this._currentHp, n), this.getMaxHp());
            return this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(n2));
        });
        return currentHpMutation.currentHp();
    }

    public final int adjustCurrentMp(long l) {
        CurrentMpMutation currentMpMutation = this.withVitalMutation(() -> {
            int n = NativeDeCharacterFields.boundedVitalValue(this._currentMp, l, 0, this.getMaxMp());
            return this.enqueueCurrentMpCompletionLocked(this.commitCurrentMpLocked(n));
        });
        return currentMpMutation.currentMp();
    }

    public final int debitCurrentMpUpTo(long l) {
        if (l <= 0L) {
            return 0;
        }
        MpDebitMutation mpDebitMutation = this.withVitalMutation(() -> {
            MpDebitMutation debit = this.debitCurrentMpLocked(l);
            this.enqueueCurrentMpCompletionLocked(debit._mutation);
            return debit;
        });
        return mpDebitMutation._debited;
    }

    public final int debitCurrentMpUpToIfAlive(long l) {
        if (l <= 0L) {
            return 0;
        }
        MpDebitMutation mpDebitMutation = this.withVitalMutation(() -> {
            MpDebitMutation debit = this.isDead() ? new MpDebitMutation(0, new CurrentMpMutation(this._currentMp, this._currentMp)) : this.debitCurrentMpLocked(l);
            this.enqueueCurrentMpCompletionLocked(debit._mutation);
            return debit;
        });
        return mpDebitMutation._debited;
    }

    private MpDebitMutation debitCurrentMpLocked(long l) {
        int n = Math.max(0, this._currentMp);
        int n2 = (int)Math.min((long)n, l);
        CurrentMpMutation currentMpMutation = n2 > 0 ? this.commitCurrentMpLocked(n - n2) : new CurrentMpMutation(this._currentMp, this._currentMp);
        return new MpDebitMutation(n2, currentMpMutation);
    }

    public final void setCurrentHp(int n) {
        this.withVitalMutation(() -> this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(n)));
    }

    public final void setCurrentHpDirect(int n) {
        this.withVitalMutation(() -> {
            int n2 = this._currentHp;
            this._currentHp = n;
            this.recordDamageHpMutation(n2, n);
        });
    }

    public int getCurrentMp() {
        return this._currentMp;
    }

    public final void setCurrentMp(int n) {
        this.withVitalMutation(() -> this.enqueueCurrentMpCompletionLocked(this.commitCurrentMpLocked(n)));
    }

    public final void setCurrentMpDirect(int n) {
        this.withVitalMutation(() -> {
            this._currentMp = n;
            return this._currentMp;
        });
    }

    public synchronized int getLevel() {
        return this._level;
    }

    public synchronized void setLevel(int n) {
        this._level = n;
    }

    public int getMaxHp() {
        return this._maxHp;
    }

    public void setMaxHp(int n) {
        this.withVitalMutation(() -> {
            int n2 = this._currentHp;
            this._trueMaxHp = n;
            int n3 = this.isROClient() ? 999999 : Short.MAX_VALUE;
            this._maxHp = NativeDeCharacterFields.ensure(this._trueMaxHp, 1, n3);
            this._currentHp = Math.min(this._currentHp, this._maxHp);
            this.enqueueCurrentHpCompletionLocked(new CurrentHpMutation(n2, this._currentHp));
        });
    }

    public void addMaxHp(int n) {
        this.withVitalMutation(() -> this.setMaxHp(this._trueMaxHp + n));
    }

    public int getMaxMp() {
        return this._maxMp;
    }

    public void setMaxMp(int n) {
        this.withVitalMutation(() -> {
            int n2 = this._currentMp;
            this._trueMaxMp = n;
            int n3 = this.isROClient() ? 999999 : Short.MAX_VALUE;
            this._maxMp = NativeDeCharacterFields.ensure(this._trueMaxMp, 0, n3);
            this._currentMp = Math.min(this._currentMp, this._maxMp);
            this.enqueueCurrentMpCompletionLocked(new CurrentMpMutation(n2, this._currentMp));
        });
    }

    public void addMaxMp(int n) {
        this.withVitalMutation(() -> this.setMaxMp(this._trueMaxMp + n));
    }

    public int getAc() {
        return NativeDeCharacterFields.ensure(this._ac, this.getMinAcLimit(), 44);
    }

    public void setAc(int n) {
        this._trueAc = n;
        this._ac = NativeDeCharacterFields.ensure(n, this.getMinAcLimit(), 44);
    }

    private int getMinAcLimit() {
        return this.isROClient() ? Integer.MIN_VALUE : -211;
    }

    public void addAc(int n) {
        this.setAc(this._trueAc + n);
    }

    public short getStr() {
        return this._str;
    }

    public void setStr(int n) {
        this._trueStr = (short)n;
        this._str = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addStr(int n) {
        this.setStr(this._trueStr + n);
    }

    public short getCon() {
        return this._con;
    }

    public void setCon(int n) {
        this._trueCon = (short)n;
        this._con = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addCon(int n) {
        this.setCon(this._trueCon + n);
    }

    public short getDex() {
        return this._dex;
    }

    public void setDex(int n) {
        this._trueDex = (short)n;
        this._dex = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addDex(int n) {
        this.setDex(this._trueDex + n);
    }

    public short getCha() {
        return this._cha;
    }

    public void setCha(int n) {
        this._trueCha = (short)n;
        this._cha = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addCha(int n) {
        this.setCha(this._trueCha + n);
    }

    public short getInt() {
        return this._int;
    }

    public void setInt(int n) {
        this._trueInt = (short)n;
        this._int = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addInt(int n) {
        this.setInt(this._trueInt + n);
    }

    public short getWis() {
        return this._wis;
    }

    public void setWis(int n) {
        this._trueWis = (short)n;
        this._wis = (short)NativeDeCharacterFields.ensure(n, 1, 254);
    }

    public void addWis(int n) {
        this.setWis(this._trueWis + n);
    }

    public int getDmgup() {
        return this._dmgup;
    }

    public void addDmgup(int n) {
        this._trueDmgup += n;
        this._dmgup = this._trueDmgup >= 127 ? 127 : (this._trueDmgup <= -128 ? -128 : this._trueDmgup);
    }

    public int getHitup() {
        return this._hitup;
    }

    public void addHitup(int n) {
        this._trueHitup += n;
        this._hitup = this._trueHitup >= 127 ? 127 : (this._trueHitup <= -128 ? -128 : this._trueHitup);
    }

    public int getSp() {
        return this.getTrueSp() + this._sp;
    }

    public int getTrueSp() {
        return this.getMagicLevel() + this.getMagicBonus();
    }

    public void addSp(int n) {
        this._sp += n;
    }

    public boolean isDead() {
        return this._isDead;
    }

    public boolean destroyed() {
        return false;
    }

    protected final void setCharacterDead(boolean bl) {
        this.withVitalMutation(() -> {
            boolean bl2 = this._isDead;
            this._isDead = bl;
            this.recordDamageDeathMutation(bl2, bl);
        });
    }

    public int getStatus() {
        return this._status;
    }

    public void setStatus(int n) {
        this.withVitalMutation(() -> {
            this._status = n;
        });
    }

    public int getLawful() {
        return this._lawful;
    }

    public void setLawful(int n) {
        this._lawful = n;
    }

    public int getTempCharGfx() {
        return this._tempCharGfx;
    }

    public void setTempCharGfx(int n) {
        this._tempCharGfx = n;
    }

    public int getGfxId() {
        return this._gfxid;
    }

    public void setGfxId(int n) {
        this._gfxid = n;
    }

    public int getMagicLevel() {
        return Math.max(this.getLevel(), 1);
    }

    public int getMagicBonus() {
        int n = this.getInt() / 2;
        return Math.max(n, 1);
    }

    protected void reportCompletionFailure(Throwable throwable) {
        _log.error("Vital completion failed: objectId={} type={}", this.getId(), this.getClass().getName(), throwable);
    }

    static NativeDeCharacterFields fromInitializedNpc(int n, Snapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot);
        if (snapshot.clientType() != NativeDeCharacterControls.clientType()) {
            throw new IllegalArgumentException("NPC initial facts use a different source client bound policy");
        }
        if (!snapshot.fields().keySet().equals(Set.of("_currentHp", "_currentMp", "_maxHp", "_trueMaxHp", "_maxMp", "_trueMaxMp", "_status", "_level", "_ac", "_trueAc", "_lawful", "_dmgup", "_trueDmgup", "_hitup", "_trueHitup", "_sp", "_tempCharGfx", "_gfxid", "_str", "_trueStr", "_con", "_trueCon", "_dex", "_trueDex", "_cha", "_trueCha", "_int", "_trueInt", "_wis", "_trueWis"))) {
            throw new IllegalArgumentException("complete explicit NPC initial Character fields required");
        }
        for (Map.Entry<String, Number> entry : snapshot.fields().entrySet()) {
            boolean bl = Set.of("_cha", "_con", "_dex", "_int", "_str", "_trueCha", "_trueCon", "_trueDex", "_trueInt", "_trueStr", "_trueWis", "_wis").contains(entry.getKey());
            if (entry.getValue().getClass() == (bl ? Short.class : Integer.class)) continue;
            throw new IllegalArgumentException("exact source primitive field type required: " + entry.getKey());
        }
        NativeDeCharacterFields nativeDeCharacterFields = new NativeDeCharacterFields(snapshot.clientType(), n);
        nativeDeCharacterFields._currentHp = snapshot.fields().get("_currentHp").intValue();
        nativeDeCharacterFields._currentMp = snapshot.fields().get("_currentMp").intValue();
        nativeDeCharacterFields._maxHp = snapshot.fields().get("_maxHp").intValue();
        nativeDeCharacterFields._trueMaxHp = snapshot.fields().get("_trueMaxHp").intValue();
        nativeDeCharacterFields._maxMp = snapshot.fields().get("_maxMp").intValue();
        nativeDeCharacterFields._trueMaxMp = snapshot.fields().get("_trueMaxMp").intValue();
        nativeDeCharacterFields._status = snapshot.fields().get("_status").intValue();
        nativeDeCharacterFields._level = snapshot.fields().get("_level").intValue();
        nativeDeCharacterFields._ac = snapshot.fields().get("_ac").intValue();
        nativeDeCharacterFields._trueAc = snapshot.fields().get("_trueAc").intValue();
        nativeDeCharacterFields._lawful = snapshot.fields().get("_lawful").intValue();
        nativeDeCharacterFields._dmgup = snapshot.fields().get("_dmgup").intValue();
        nativeDeCharacterFields._trueDmgup = snapshot.fields().get("_trueDmgup").intValue();
        nativeDeCharacterFields._hitup = snapshot.fields().get("_hitup").intValue();
        nativeDeCharacterFields._trueHitup = snapshot.fields().get("_trueHitup").intValue();
        nativeDeCharacterFields._sp = snapshot.fields().get("_sp").intValue();
        nativeDeCharacterFields._tempCharGfx = snapshot.fields().get("_tempCharGfx").intValue();
        nativeDeCharacterFields._gfxid = snapshot.fields().get("_gfxid").intValue();
        nativeDeCharacterFields._str = snapshot.fields().get("_str").shortValue();
        nativeDeCharacterFields._trueStr = snapshot.fields().get("_trueStr").shortValue();
        nativeDeCharacterFields._con = snapshot.fields().get("_con").shortValue();
        nativeDeCharacterFields._trueCon = snapshot.fields().get("_trueCon").shortValue();
        nativeDeCharacterFields._dex = snapshot.fields().get("_dex").shortValue();
        nativeDeCharacterFields._trueDex = snapshot.fields().get("_trueDex").shortValue();
        nativeDeCharacterFields._cha = snapshot.fields().get("_cha").shortValue();
        nativeDeCharacterFields._trueCha = snapshot.fields().get("_trueCha").shortValue();
        nativeDeCharacterFields._int = snapshot.fields().get("_int").shortValue();
        nativeDeCharacterFields._trueInt = snapshot.fields().get("_trueInt").shortValue();
        nativeDeCharacterFields._wis = snapshot.fields().get("_wis").shortValue();
        nativeDeCharacterFields._trueWis = snapshot.fields().get("_trueWis").shortValue();
        nativeDeCharacterFields._isDead = snapshot.dead();
        return nativeDeCharacterFields;
    }

    Snapshot snapshot() {
        TreeMap<String, Number> treeMap = new TreeMap<String, Number>();
        this._npcVitalMutationLock.lock();
        try {
            treeMap.put("_level", this.getLevel());
            treeMap.put("_currentHp", this._currentHp);
            treeMap.put("_currentMp", this._currentMp);
            treeMap.put("_maxHp", this._maxHp);
            treeMap.put("_trueMaxHp", this._trueMaxHp);
            treeMap.put("_maxMp", this._maxMp);
            treeMap.put("_trueMaxMp", this._trueMaxMp);
            treeMap.put("_status", this._status);
            treeMap.put("_ac", this._ac);
            treeMap.put("_trueAc", this._trueAc);
            treeMap.put("_lawful", this._lawful);
            treeMap.put("_dmgup", this._dmgup);
            treeMap.put("_trueDmgup", this._trueDmgup);
            treeMap.put("_hitup", this._hitup);
            treeMap.put("_trueHitup", this._trueHitup);
            treeMap.put("_sp", this._sp);
            treeMap.put("_tempCharGfx", this._tempCharGfx);
            treeMap.put("_gfxid", this._gfxid);
            treeMap.put("_str", this._str);
            treeMap.put("_trueStr", this._trueStr);
            treeMap.put("_con", this._con);
            treeMap.put("_trueCon", this._trueCon);
            treeMap.put("_dex", this._dex);
            treeMap.put("_trueDex", this._trueDex);
            treeMap.put("_cha", this._cha);
            treeMap.put("_trueCha", this._trueCha);
            treeMap.put("_int", this._int);
            treeMap.put("_trueInt", this._trueInt);
            treeMap.put("_wis", this._wis);
            treeMap.put("_trueWis", this._trueWis);
            Snapshot snapshot = new Snapshot(this.clientType, treeMap, this._isDead);
            return snapshot;
        }
        finally {
            this._npcVitalMutationLock.unlock();
        }
    }

    NativeDeCharacterFields(int n, Snapshot snapshot) throws IOException {
        this(snapshot.clientType(), n);
        NativeDeCharacterFields nativeDeCharacterFields = NativeDeCharacterFields.fromInitializedNpc(n, snapshot);
        this._currentHp = nativeDeCharacterFields._currentHp;
        this._currentMp = nativeDeCharacterFields._currentMp;
        this._maxHp = nativeDeCharacterFields._maxHp;
        this._trueMaxHp = nativeDeCharacterFields._trueMaxHp;
        this._maxMp = nativeDeCharacterFields._maxMp;
        this._trueMaxMp = nativeDeCharacterFields._trueMaxMp;
        this._status = nativeDeCharacterFields._status;
        this._level = nativeDeCharacterFields._level;
        this._ac = nativeDeCharacterFields._ac;
        this._trueAc = nativeDeCharacterFields._trueAc;
        this._lawful = nativeDeCharacterFields._lawful;
        this._dmgup = nativeDeCharacterFields._dmgup;
        this._trueDmgup = nativeDeCharacterFields._trueDmgup;
        this._hitup = nativeDeCharacterFields._hitup;
        this._trueHitup = nativeDeCharacterFields._trueHitup;
        this._sp = nativeDeCharacterFields._sp;
        this._tempCharGfx = nativeDeCharacterFields._tempCharGfx;
        this._gfxid = nativeDeCharacterFields._gfxid;
        this._str = nativeDeCharacterFields._str;
        this._trueStr = nativeDeCharacterFields._trueStr;
        this._con = nativeDeCharacterFields._con;
        this._trueCon = nativeDeCharacterFields._trueCon;
        this._dex = nativeDeCharacterFields._dex;
        this._trueDex = nativeDeCharacterFields._trueDex;
        this._cha = nativeDeCharacterFields._cha;
        this._trueCha = nativeDeCharacterFields._trueCha;
        this._int = nativeDeCharacterFields._int;
        this._trueInt = nativeDeCharacterFields._trueInt;
        this._wis = nativeDeCharacterFields._wis;
        this._trueWis = nativeDeCharacterFields._trueWis;
        this._isDead = nativeDeCharacterFields._isDead;
    }

    protected NativeDeResurrectionEffects resurrectionEffects() {
        return NativeDeResurrectionEffects.UNBOUND;
    }

    protected void setDeadForResurrection(boolean bl) {
        this.setCharacterDead(bl);
    }

    public void resurrect(int n) {
        try {
            int n2 = Math.max(1, n);
            CurrentHpMutation currentHpMutation = this.withVitalMutation(() -> {
                if (!this.isDead()) {
                    return null;
                }
                this.setDeadForResurrection(false);
                CurrentHpMutation mutation = this.commitCurrentHpLocked(n2);
                this.setStatus(0);
                return this.enqueueCurrentHpCompletionLocked(mutation);
            });
            if (currentHpMutation == null) {
                return;
            }
            this.resurrectionEffects().undoPoly(this);
            for (NativeDeResurrectionEffects.Viewer viewer : this.resurrectionEffects().recognizePlayers(this)) {
                viewer.sendRemoveObject(this.resurrectionEffects().newRemoveObject(this.getId()));
                viewer.removeKnownObject(this);
                viewer.updateObject();
            }
        }
        catch (Exception exception) {
            this.resurrectionEffects().characterFailure(exception.getLocalizedMessage(), exception);
        }
    }

    protected final <T> T withKnownLock(Supplier<T> supplier) {
        this._knownObjectLock.lock();
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            this._knownObjectLock.unlock();
        }
    }

    public boolean hasSkillEffect(int n) {
        return this._skillEffectSnapshot.activeSkillIds.contains(n);
    }

    public Map<Integer, NativeDeDeletionState> getPetList() {
        return this.pets;
    }

    public Map<Integer, NativeDeDeletionPorts.Doll> getDolls() {
        return this.dolls;
    }

    public NativeDePerceptionPorts.Poison getPoison() {
        NativeDePerceptionPorts.Poison poison = this._poison;
        return poison instanceof NativeDePerceptionPorts.PoisonTransition ? null : poison;
    }

    public long getExp() {
        return this._exp;
    }

    public void setExp(long l) {
        this._exp = l;
    }

    public boolean knownsObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        return this._knownObjects.contains(objectRef);
    }

    public List<NativeDeDeletionPorts.ObjectRef> getKnownObjects() {
        return this._knownObjects;
    }

    public List<NativeDeDeletionPorts.Viewer> getKnownPlayers() {
        return this._knownPlayer;
    }

    public void addKnownObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.tryAddKnownObject(objectRef);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean tryAddKnownObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef == null) {
            return false;
        }
        this._knownObjectLock.lock();
        try {
            NativeDeCharacterFields nativeDeCharacterFields = this;
            if (nativeDeCharacterFields instanceof NativeDeDeletionPorts.Viewer) {
                NativeDeDeletionState nativeDeDeletionState;
                NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
                if (objectRef instanceof NativeDeDeletionState && !(nativeDeDeletionState = (NativeDeDeletionState)objectRef).registerViewer(viewer)) {
                    boolean bl = false;
                    return bl;
                }
            }
            if (this._knownObjects.addIfAbsent(objectRef) && objectRef instanceof NativeDeDeletionPorts.Viewer) {
                this._knownPlayer.addIfAbsent((NativeDeDeletionPorts.Viewer)objectRef);
            }
            boolean bl = true;
            return bl;
        }
        finally {
            this._knownObjectLock.unlock();
        }
    }

    public void removeKnownObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.removeKnownObjectIfPresent(objectRef);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean removeKnownObjectIfPresent(NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef == null) {
            return false;
        }
        this._knownObjectLock.lock();
        try {
            NativeDeCharacterFields nativeDeCharacterFields;
            boolean bl = this._knownObjects.remove(objectRef);
            if (objectRef instanceof NativeDeDeletionPorts.Viewer) {
                this._knownPlayer.remove(objectRef);
            }
            if ((nativeDeCharacterFields = this) instanceof NativeDeDeletionPorts.Viewer) {
                NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
                if (objectRef instanceof NativeDeDeletionState) {
                    NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)objectRef;
                    nativeDeDeletionState.unregisterViewer(viewer);
                }
            }
            boolean bl2 = bl;
            return bl2;
        }
        finally {
            this._knownObjectLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isKnownNpcViewerConsistent(NativeDeDeletionState nativeDeDeletionState) {
        NativeDeDeletionPorts.Viewer viewer;
        block6: {
            block5: {
                NativeDeCharacterFields nativeDeCharacterFields = this;
                if (!(nativeDeCharacterFields instanceof NativeDeDeletionPorts.Viewer)) break block5;
                viewer = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
                if (nativeDeDeletionState != null) break block6;
            }
            return true;
        }
        this._knownObjectLock.lock();
        try {
            boolean bl = this._knownObjects.contains(nativeDeDeletionState) == nativeDeDeletionState.isViewerRegistered(viewer);
            return bl;
        }
        finally {
            this._knownObjectLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeAllKnownObjects() {
        this._knownObjectLock.lock();
        try {
            NativeDeCharacterFields nativeDeCharacterFields = this;
            if (nativeDeCharacterFields instanceof NativeDeDeletionPorts.Viewer) {
                NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
                for (NativeDeDeletionPorts.ObjectRef objectRef : this._knownObjects) {
                    if (!(objectRef instanceof NativeDeDeletionState)) continue;
                    NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)objectRef;
                    nativeDeDeletionState.unregisterViewer(viewer);
                }
            }
            this._knownObjects.clear();
            this._knownPlayer.clear();
        }
        finally {
            this._knownObjectLock.unlock();
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public int getWind() {
        return this._wind;
    }

    public void addWind(int n) {
        this._trueWind += n;
        this._wind = this._trueWind >= 127 ? 127 : (this._trueWind <= -128 ? -128 : this._trueWind);
    }

    public int getWater() {
        return this._water;
    }

    public void addWater(int n) {
        this._trueWater += n;
        this._water = this._trueWater >= 127 ? 127 : (this._trueWater <= -128 ? -128 : this._trueWater);
    }

    public int getFire() {
        return this._fire;
    }

    public void addFire(int n) {
        this._trueFire += n;
        this._fire = this._trueFire >= 127 ? 127 : (this._trueFire <= -128 ? -128 : this._trueFire);
    }

    public int getEarth() {
        return this._earth;
    }

    public void addEarth(int n) {
        this._trueEarth += n;
        this._earth = this._trueEarth >= 127 ? 127 : (this._trueEarth <= -128 ? -128 : this._trueEarth);
    }

    public int getAddAttrKind() {
        return this._addAttrKind;
    }

    public void setAddAttrKind(int n) {
        this._addAttrKind = n;
    }

    public int getRegistStun() {
        return this._registStun;
    }

    public void addRegistStun(int n) {
        this._trueRegistStun += n;
        this._registStun = this._trueRegistStun > 127 ? 127 : (this._trueRegistStun < -128 ? -128 : this._trueRegistStun);
    }

    public int getRegistStone() {
        return this._registStone;
    }

    public void addRegistStone(int n) {
        this._trueRegistStone += n;
        this._registStone = this._trueRegistStone > 127 ? 127 : (this._trueRegistStone < -128 ? -128 : this._trueRegistStone);
    }

    public int getRegistSleep() {
        return this._registSleep;
    }

    public void addRegistSleep(int n) {
        this._trueRegistSleep += n;
        this._registSleep = this._trueRegistSleep > 127 ? 127 : (this._trueRegistSleep < -128 ? -128 : this._trueRegistSleep);
    }

    public int getRegistFreeze() {
        return this._registFreeze;
    }

    public void add_regist_freeze(int n) {
        this._trueRegistFreeze += n;
        this._registFreeze = this._trueRegistFreeze > 127 ? 127 : (this._trueRegistFreeze < -128 ? -128 : this._trueRegistFreeze);
    }

    public int getRegistSustain() {
        return this._registSustain;
    }

    public void addRegistSustain(int n) {
        this._trueRegistSustain += n;
        this._registSustain = this._trueRegistSustain > 127 ? 127 : (this._trueRegistSustain < -128 ? -128 : this._trueRegistSustain);
    }

    public int getRegistBlind() {
        return this._registBlind;
    }

    public void addRegistBlind(int n) {
        this._trueRegistBlind += n;
        this._registBlind = this._trueRegistBlind > 127 ? 127 : (this._trueRegistBlind < -128 ? -128 : this._trueRegistBlind);
    }

    public int getBowDmgup() {
        return this._bowDmgup;
    }

    public void addBowDmgup(int n) {
        this._trueBowDmgup += n;
        this._bowDmgup = this._trueBowDmgup >= 127 ? 127 : (this._trueBowDmgup <= -128 ? -128 : this._trueBowDmgup);
    }

    public int getBowHitup() {
        return this._bowHitup;
    }

    public void addBowHitup(int n) {
        this._trueBowHitup += n;
        this._bowHitup = this._trueBowHitup >= 127 ? 127 : (this._trueBowHitup <= -128 ? -128 : this._trueBowHitup);
    }

    public int getStunSuccess() {
        return this._stunSuccess;
    }

    public void addStunSuccess(int n) {
        this._stunSuccess += n;
    }

    public int getMr() {
        if (this.hasSkillEffect(153)) {
            return this._mr >> 2;
        }
        return this._mr;
    }

    public int getTrueMr() {
        return this._trueMr;
    }

    public void addMr(int n) {
        this._trueMr += n;
        this._mr = this._trueMr <= 0 ? 0 : this._trueMr;
    }

    public String getTitle() {
        return this._title;
    }

    public void setTitle(String string) {
        this._title = string;
    }

    public int getHeading() {
        return this._heading;
    }

    public void setHeading(int n) {
        this._heading = n;
    }

    public int getMoveSpeed() {
        return this._moveSpeed;
    }

    public void setMoveSpeed(int n) {
        this._moveSpeed = n;
    }

    public int getBraveSpeed() {
        return this._braveSpeed;
    }

    public void setBraveSpeed(int n) {
        this._braveSpeed = n;
    }

    public boolean isInvisble() {
        return this.hasSkillEffect(60) || this.hasSkillEffect(97);
    }

    public int getKarma() {
        return this._karma;
    }

    public void setKarma(int n) {
        this._karma = n;
    }

    public void setMr(int n) {
        this._trueMr = n;
        this._mr = this._trueMr <= 0 ? 0 : this._trueMr;
    }

    public int getChaLightSize() {
        if (this.isInvisble()) {
            return 0;
        }
        return this._chaLightSize;
    }

    public void setChaLightSize(int n) {
        this._chaLightSize = n;
    }

    public int getOwnLightSize() {
        if (this.isInvisble()) {
            return 0;
        }
        return this._ownLightSize;
    }

    public void setOwnLightSize(int n) {
        this._ownLightSize = n;
    }

    public boolean isSleeped() {
        return this._sleeped;
    }

    public boolean isParalyzed() {
        return this._paralyzed;
    }

    private int hpMeterRatio() {
        int n = NativeDeVisibilityAccess.L1Character$getMaxHp(this);
        if (n <= 0) {
            return 100;
        }
        return 100 * NativeDeVisibilityAccess.L1Character$getCurrentHp(this) / n;
    }

    private final void visibilitySource$broadcastPacketHP(NativeDeDeletionPorts.Viewer viewer) {
        try {
            int n2 = this.hpMeterRatio();
            int n3 = NativeDeVisibilityAccess.L1PcInstance$getId(viewer);
            if (this._secHpByViewer.size() >= 1024 && !this._secHpByViewer.containsKey(n3)) {
                this._secHpByViewer.clear();
            }
            ViewerHpState viewerHpState2 = this._secHpByViewer.compute(n3, (n, viewerHpState) -> viewerHpState != null && viewerHpState.belongsTo(viewer) ? viewerHpState : new ViewerHpState(viewer));
            if (viewerHpState2.lastHpRatio.getAndSet(n2) != n2) {
                NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeVisibilityHp(NativeDeVisibilityAccess.L1Character$getId(this), n2));
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error(exception.getLocalizedMessage(), exception);
        }
    }

    public void broadcastPacketHP(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityRuntime.broadcastPacketHP(this, viewer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private final void visibilitySource$hideTrackedHpBars() {
        try {
            for (ViewerHpState viewerHpState : this._secHpByViewer.values()) {
                NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)viewerHpState.viewer.get();
                try {
                    if (viewer == null) continue;
                    NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeVisibilityHp(NativeDeVisibilityAccess.L1Character$getId(this), 255));
                }
                catch (Exception exception) {
                    NativeDeVisibilityScope.log().error("HP meter hide failed: objectId={} viewerId={}", (Object)NativeDeVisibilityAccess.L1Character$getId(this), (Object)(viewer != null ? NativeDeVisibilityAccess.L1PcInstance$getId(viewer) : -1), (Object)exception);
                }
            }
        }
        finally {
            this._secHpByViewer.clear();
        }
    }

    protected final void hideTrackedHpBars() {
        NativeDeVisibilityRuntime.hideTrackedHpBars(this);
    }

    protected static final class DamageMutationReceipt {
        private final NativeDeCharacterFields _target;
        private final int _startHp;
        private int _hpBefore;
        private int _hpAfter;
        private boolean _hpSeen;
        private boolean _hpReduced;
        private boolean _deadAfter;
        private boolean _deathSeen;

        private DamageMutationReceipt(NativeDeCharacterFields nativeDeCharacterFields) {
            this._target = nativeDeCharacterFields;
            this._hpBefore = this._startHp = nativeDeCharacterFields.getCurrentHp();
            this._hpAfter = this._startHp;
            this._deadAfter = nativeDeCharacterFields.isDead();
        }

        private void recordHp(int n, int n2) {
            if (n == n2) {
                return;
            }
            if (!this._hpSeen) {
                this._hpBefore = n;
                this._hpSeen = true;
            }
            this._hpAfter = n2;
            this._hpReduced |= n2 < n;
        }

        private void recordDead(boolean bl, boolean bl2, int n) {
            if (bl == bl2) {
                return;
            }
            this._deathSeen = true;
            this._deadAfter = bl2;
            if (!this._hpSeen) {
                this._hpBefore = n;
                this._hpAfter = n;
            }
        }

        public boolean damageCommitted() {
            return this._hpReduced || this._deathSeen && this._deadAfter;
        }

        public int hpBefore() {
            return this._hpBefore;
        }

        public int hpAfter() {
            return this._hpAfter;
        }

        public boolean terminal() {
            return this._deadAfter || this.damageCommitted() && this._hpAfter <= 0;
        }
    }

    protected static class CurrentHpMutation {
        private final int _previousHp;
        private final int _currentHp;

        protected CurrentHpMutation(int n, int n2) {
            this._previousHp = n;
            this._currentHp = n2;
        }

        public final int previousHp() {
            return this._previousHp;
        }

        public final int currentHp() {
            return this._currentHp;
        }

        public final boolean changed() {
            return this._previousHp != this._currentHp;
        }

        protected boolean completionRequired() {
            return this.changed();
        }
    }

    protected static class CurrentMpMutation {
        private final int _previousMp;
        private final int _currentMp;

        protected CurrentMpMutation(int n, int n2) {
            this._previousMp = n;
            this._currentMp = n2;
        }

        public final int previousMp() {
            return this._previousMp;
        }

        public final int currentMp() {
            return this._currentMp;
        }

        public final boolean changed() {
            return this._previousMp != this._currentMp;
        }

        protected boolean completionRequired() {
            return this.changed();
        }
    }

    private static final class MpDebitMutation {
        private final int _debited;
        private final CurrentMpMutation _mutation;

        private MpDebitMutation(int n, CurrentMpMutation currentMpMutation) {
            this._debited = n;
            this._mutation = currentMpMutation;
        }
    }

    private static final class ErrorLog {
        private ErrorLog() {
        }

        void error(String string, Object object, Object object2, Object object3) {
            System.getLogger(NativeDeCharacterFields.class.getName()).log(System.Logger.Level.ERROR, "Factory vital completion failed id=" + String.valueOf(object) + " type=" + String.valueOf(object2), (Throwable)object3);
        }
    }

    record Snapshot(int clientType, Map<String, Number> fields, boolean dead) {Snapshot {
            fields = Map.copyOf(fields);
        }
    }

    private static final class SkillEffectSnapshot {
        private static final CombatSkillDerived EMPTY_DERIVED = new CombatSkillDerived(0, 0, 0, 0, 0, false);
        private static final SkillEffectSnapshot EMPTY = new SkillEffectSnapshot(Set.of(), EMPTY_DERIVED);
        private final Set<Integer> activeSkillIds;
        private final CombatSkillDerived combat;

        private SkillEffectSnapshot(Set<Integer> set, CombatSkillDerived combatSkillDerived) {
            this.activeSkillIds = set;
            this.combat = combatSkillDerived;
        }
    }

    private static final class ViewerHpState {
        private final WeakReference<NativeDeDeletionPorts.Viewer> viewer;
        private final AtomicInteger lastHpRatio = new AtomicInteger(-1);

        private ViewerHpState(NativeDeDeletionPorts.Viewer viewer) {
            this.viewer = new WeakReference<NativeDeDeletionPorts.Viewer>(viewer);
        }

        private boolean belongsTo(NativeDeDeletionPorts.Viewer viewer) {
            return this.viewer.get() == viewer;
        }
    }

    public record CombatSkillDerived(int meleeHitBonus, int rangedHitBonus, int meleeCookingDmgBonus, int rangedCookingDmgBonus, int skd3FlatModifier, boolean skd3HalveDamage) {
        public static CombatSkillDerived compute(Iterable<Integer> iterable) {
            int n = 0;
            int n2 = 0;
            int n3 = 0;
            int n4 = 0;
            int n5 = 0;
            boolean bl = false;
            for (Integer n6 : iterable) {
                Integer n7;
                Integer n8;
                Integer n9;
                Integer n10;
                if (n6 == null) continue;
                Integer n11 = (Integer)SKU1.get(n6);
                if (n11 != null) {
                    n += n11.intValue();
                }
                if ((n10 = (Integer)SKU2.get(n6)) != null) {
                    n2 += n10.intValue();
                }
                if ((n9 = (Integer)SKD1.get(n6)) != null) {
                    n3 += n9.intValue();
                }
                if ((n8 = (Integer)SKD2.get(n6)) != null) {
                    n4 += n8.intValue();
                }
                if ((n7 = (Integer)SKD3.get(n6)) == null) continue;
                if (n7.equals(n6)) {
                    bl = true;
                    continue;
                }
                n5 += n7.intValue();
            }
            return new CombatSkillDerived(n, n2, n3, n4, n5, bl);
        }
    }
}
