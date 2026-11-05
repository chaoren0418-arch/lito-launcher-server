// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import local.lito381.server.NativeDeConstructionPorts.CubeLease;
import local.lito381.server.NativeDeConstructionPorts.DamageQueue;
import local.lito381.server.NativeDeConstructionPorts.Follower;
import local.lito381.server.NativeDeConstructionPorts.ItemDelayTimer;
import local.lito381.server.NativeDeConstructionPorts.Paralysis;
import local.lito381.server.NativeDeConstructionPorts.SkillTimer;
import local.lito381.server.NativeDePerceptionPorts.Poison;
import local.lito381.server.NativeDePerceptionPorts.PoisonTransition;
import local.lito381.server.NativeDeResurrectionEffects.Viewer;

class NativeDeCharacterFields extends NativeDeObjectFields {
   static final int MAX_HP_MP_RO = 999999;
   static final int MAX_HP_MP_STANDARD = 32767;
   static final java.util.HashMap<Integer,Integer> SKU1 = new java.util.HashMap<>();
   static final java.util.HashMap<Integer,Integer> SKU2 = new java.util.HashMap<>();
   static final java.util.HashMap<Integer,Integer> SKD1 = new java.util.HashMap<>();
   static final java.util.HashMap<Integer,Integer> SKD2 = new java.util.HashMap<>();
   static final java.util.HashMap<Integer,Integer> SKD3 = new java.util.HashMap<>();
   private final int clientType;
   private final int objectId;
   private final ReentrantLock _npcVitalMutationLock;
   private final ConcurrentLinkedQueue<Runnable> _vitalCompletions;
   private final AtomicBoolean _vitalCompletionDraining;
   private static final ThreadLocal<ArrayDeque<NativeDeCharacterFields.DamageMutationReceipt>> DAMAGE_MUTATION_RECEIPTS = new ThreadLocal<>();
   private volatile int _currentHp;
   private volatile int _currentMp;
   private volatile int _maxHp;
   private volatile int _trueMaxHp;
   private volatile int _maxMp;
   private volatile int _trueMaxMp;
   private volatile int _status;
   private volatile boolean _isDead;
   private int _level;
   private int _ac;
   private int _trueAc;
   private int _lawful;
   private int _dmgup;
   private int _trueDmgup;
   private int _hitup;
   private int _trueHitup;
   private int _sp;
   private int _tempCharGfx;
   private int _gfxid;
   private short _str;
   private short _trueStr;
   private short _con;
   private short _trueCon;
   private short _dex;
   private short _trueDex;
   private short _cha;
   private short _trueCha;
   private short _int;
   private short _trueInt;
   private short _wis;
   private short _trueWis;
   private static final NativeDeCharacterFields.ErrorLog _log = new NativeDeCharacterFields.ErrorLog();
   private volatile Poison _poison = null;
   private volatile boolean _sleeped;
   final Map<Integer, NativeDeDeletionState> pets = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Integer, SkillTimer> _skillEffect = new ConcurrentHashMap<>();
   private final ReentrantLock _skillEffectLock = new ReentrantLock();
   private volatile NativeDeCharacterFields.SkillEffectSnapshot _skillEffectSnapshot = NativeDeCharacterFields.SkillEffectSnapshot.EMPTY;
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
   private final Map<Integer, ItemDelayTimer> _itemdelay = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Integer, CubeLease> _cubeLeases = new ConcurrentHashMap<>();
   private final Map<Integer, Follower> _followerlist = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Integer, NativeDeCharacterFields.ViewerHpState> _secHpByViewer = new ConcurrentHashMap<>();
   private volatile DamageQueue _damageCommitQueue;
   private final ReentrantLock _damageCommitQueueLock;
   private volatile boolean _paralyzed;
   private volatile Paralysis _paralysis;
   private final AtomicReference<Object> _skillDelayToken;
   private final AtomicLong _skillActionReadyAt;
   final Map<Integer, NativeDeDeletionPorts.Doll> dolls;
   private long _exp;
   private final CopyOnWriteArrayList<NativeDeDeletionPorts.ObjectRef> _knownObjects;
   private final CopyOnWriteArrayList<NativeDeDeletionPorts.Viewer> _knownPlayer;
   private final ReentrantLock _knownObjectLock;
   String name;
   private int _wind;
   private int _trueWind;
   private int _water;
   private int _trueWater;
   private int _fire;
   private int _trueFire;
   private int _earth;
   private int _trueEarth;
   private int _addAttrKind;
   private int _registStun;
   private int _trueRegistStun;
   private int _registStone;
   private int _trueRegistStone;
   private int _registSleep;
   private int _trueRegistSleep;
   private int _registFreeze;
   private int _trueRegistFreeze;
   private int _registSustain;
   private int _trueRegistSustain;
   private int _registBlind;
   private int _trueRegistBlind;
   private int _bowDmgup;
   private int _trueBowDmgup;
   private int _bowHitup;
   private int _trueBowHitup;
   private int _stunSuccess;
   private int _mr;
   private int _trueMr;
   private String _title;
   private volatile int _heading;
   private int _moveSpeed;
   private int _braveSpeed;
   private int _karma;
   private int _chaLightSize;
   private int _ownLightSize;
   private int _tmp;
   private int _tmp_mr;
   private int _dodge_up;
   private int _dodge_down;
   private boolean _decay_potion;

   NativeDeCharacterFields(int var1, int var2) {
      this._npcVitalMutationLock = new ReentrantLock();
      this._vitalCompletions = new ConcurrentLinkedQueue<>();
      this._vitalCompletionDraining = new AtomicBoolean();
      this._damageCommitQueueLock = new ReentrantLock();
      this._skillDelayToken = new AtomicReference<>();
      this._skillActionReadyAt = new AtomicLong(0L);
      this.dolls = new ConcurrentHashMap<>();
      this._knownObjects = new CopyOnWriteArrayList<>();
      this._knownPlayer = new CopyOnWriteArrayList<>();
      this._knownObjectLock = new ReentrantLock();
      this._maxHp = 0;
      this._trueMaxHp = 0;
      this._maxMp = 0;
      this._trueMaxMp = 0;
      this._ac = 10;
      this._trueAc = 0;
      this._str = 0;
      this._trueStr = 0;
      this._con = 0;
      this._trueCon = 0;
      this._dex = 0;
      this._trueDex = 0;
      this._cha = 0;
      this._trueCha = 0;
      this._int = 0;
      this._trueInt = 0;
      this._wis = 0;
      this._trueWis = 0;
      this._wind = 0;
      this._trueWind = 0;
      this._water = 0;
      this._trueWater = 0;
      this._fire = 0;
      this._trueFire = 0;
      this._earth = 0;
      this._trueEarth = 0;
      this._registStun = 0;
      this._trueRegistStun = 0;
      this._registStone = 0;
      this._trueRegistStone = 0;
      this._registSleep = 0;
      this._trueRegistSleep = 0;
      this._registFreeze = 0;
      this._trueRegistFreeze = 0;
      this._registSustain = 0;
      this._trueRegistSustain = 0;
      this._registBlind = 0;
      this._trueRegistBlind = 0;
      this._dmgup = 0;
      this._trueDmgup = 0;
      this._bowDmgup = 0;
      this._trueBowDmgup = 0;
      this._hitup = 0;
      this._trueHitup = 0;
      this._bowHitup = 0;
      this._trueBowHitup = 0;
      this._stunSuccess = 0;
      this._mr = 0;
      this._trueMr = 0;
      this._sp = 0;
      this._dodge_up = 0;
      this._dodge_down = 0;
      this._decay_potion = false;
      this._level = 1;
      this.clientType = var1;
      this.objectId = var2;
   }

   private boolean isROClient() {
      return this.clientType == 2;
   }

   @Override
   protected int getId() {
      return this.objectId;
   }

   private static int ensure(int var0, int var1, int var2) {
      int var3 = var1 <= var0 ? var0 : var1;
      return var3 <= var2 ? var3 : var2;
   }

   public int getCurrentHp() {
      return this._currentHp;
   }

   private ReentrantLock vitalMutationLock() {
      return this._npcVitalMutationLock;
   }

   protected final <T> T withVitalMutation(Supplier<T> var1) {
      if (var1 == null) {
         throw new NullPointerException("mutation");
      } else {
         ReentrantLock var2 = this.vitalMutationLock();
         var2.lock();

         try {
            return var1.get();
         } finally {
            var2.unlock();
            if (!var2.isHeldByCurrentThread()) {
               this.drainVitalCompletionsAfterActionUnlock();
            }
         }

      }
   }

   protected final void withVitalMutation(Runnable var1) {
      this.withVitalMutation(() -> {
         var1.run();
         return null;
      });
   }

   protected final NativeDeCharacterFields.DamageMutationReceipt traceDamageMutations(Runnable var1) {
      if (var1 == null) {
         throw new NullPointerException("mutation");
      } else {
         ArrayDeque var2 = DAMAGE_MUTATION_RECEIPTS.get();
         if (var2 == null) {
            var2 = new ArrayDeque();
            DAMAGE_MUTATION_RECEIPTS.set(var2);
         }

         NativeDeCharacterFields.DamageMutationReceipt var3 = new NativeDeCharacterFields.DamageMutationReceipt(this);
         var2.push(var3);

         try {
            var1.run();
         } finally {
            if (var2.peek() == var3) {
               var2.pop();
            } else {
               var2.remove(var3);
            }

            if (var2.isEmpty()) {
               DAMAGE_MUTATION_RECEIPTS.remove();
            }
         }

         return var3;
      }
   }

   private void recordDamageHpMutation(int var1, int var2) {
      if (var1 == var2) { return; }
      ArrayDeque<NativeDeCharacterFields.DamageMutationReceipt> var3 = DAMAGE_MUTATION_RECEIPTS.get();
      if (var3 == null) { return; }
      for (NativeDeCharacterFields.DamageMutationReceipt var5 : var3) {
         if (var5._target == this) {
            var5.recordHp(var1, var2);
            return;
         }
      }
   }

   private void recordDamageDeathMutation(boolean var1, boolean var2) {
      if (var1 == var2) { return; }
      ArrayDeque<NativeDeCharacterFields.DamageMutationReceipt> var3 = DAMAGE_MUTATION_RECEIPTS.get();
      if (var3 == null) { return; }
      for (NativeDeCharacterFields.DamageMutationReceipt var5 : var3) {
         if (var5._target == this) {
            var5.recordDead(var1, var2, this._currentHp);
            return;
         }
      }
   }

   private static int boundedVitalValue(int var0, long var1, int var3, int var4) {
      int var5 = Math.max(0, var4);
      int var6 = Math.max(0, Math.min(var3, var5));

      long var7;
      try {
         var7 = Math.addExact((long)var0, var1);
      } catch (ArithmeticException var10) {
         return var1 >= 0L ? var5 : var6;
      }

      return (int)Math.max((long)var6, Math.min((long)var5, var7));
   }

   protected NativeDeCharacterFields.CurrentHpMutation commitCurrentHpLocked(int var1) {
      int var2 = this._currentHp;
      if (!this.isCurrentHpMutationAllowed()) {
         return new NativeDeCharacterFields.CurrentHpMutation(var2, var2);
      } else {
         int var3 = Math.max(0, Math.min(var1, Math.max(0, this.getMaxHp())));
         if (this.isDead() && var3 > 0) {
            return new NativeDeCharacterFields.CurrentHpMutation(var2, var2);
         } else {
            this._currentHp = var3;
            this.recordDamageHpMutation(var2, var3);
            return new NativeDeCharacterFields.CurrentHpMutation(var2, var3);
         }
      }
   }

   protected NativeDeCharacterFields.CurrentMpMutation commitCurrentMpLocked(int var1) {
      int var2 = this._currentMp;
      if (!this.isCurrentMpMutationAllowed()) {
         return new NativeDeCharacterFields.CurrentMpMutation(var2, var2);
      } else {
         int var3 = Math.max(0, Math.min(var1, Math.max(0, this.getMaxMp())));
         this._currentMp = var3;
         return new NativeDeCharacterFields.CurrentMpMutation(var2, var3);
      }
   }

   protected boolean isCurrentHpMutationAllowed() {
      return true;
   }

   protected boolean isCurrentMpMutationAllowed() {
      return true;
   }

   protected void afterCurrentHpMutation(NativeDeCharacterFields.CurrentHpMutation var1) {
   }

   protected void afterCurrentMpMutation(NativeDeCharacterFields.CurrentMpMutation var1) {
   }

   private NativeDeCharacterFields.CurrentHpMutation enqueueCurrentHpCompletionLocked(NativeDeCharacterFields.CurrentHpMutation var1) {
      if (var1.completionRequired()) {
         this._vitalCompletions.add(() -> this.afterCurrentHpMutation(var1));
      }

      return var1;
   }

   private NativeDeCharacterFields.CurrentMpMutation enqueueCurrentMpCompletionLocked(NativeDeCharacterFields.CurrentMpMutation var1) {
      if (var1.completionRequired()) {
         this._vitalCompletions.add(() -> this.afterCurrentMpMutation(var1));
      }

      return var1;
   }

   final void drainVitalCompletionsAfterActionUnlock() {
      ReentrantLock var1 = this.vitalMutationLock();
      do {
         if (var1.isHeldByCurrentThread() || !this._vitalCompletionDraining.compareAndSet(false, true)) {
            return;
         }
         try {
            while (true) {
               ArrayDeque<Runnable> var2 = new ArrayDeque<>();
               var1.lock();
               Runnable var3;
               try {
                  while ((var3 = this._vitalCompletions.poll()) != null) {
                     var2.addLast(var3);
                  }
               } finally {
                  var1.unlock();
               }
               if (var2.isEmpty()) { break; }
               while ((var3 = var2.pollFirst()) != null) {
                  try {
                     var3.run();
                  } catch (Throwable var12) {
                     this.reportCompletionFailure(var12);
                  }
               }
            }
         } finally {
            this._vitalCompletionDraining.set(false);
         }
      } while (!this._vitalCompletions.isEmpty());
   }

   public final int adjustCurrentHp(long var1) {
      return this.adjustCurrentHp(var1, 0);
   }

   public final int adjustCurrentHp(long var1, int var3) {
      NativeDeCharacterFields.CurrentHpMutation var4 = this.withVitalMutation(() -> {
         int var4x = boundedVitalValue(this._currentHp, var1, var3, this.getMaxHp());
         return this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(var4x));
      });
      return var4.currentHp();
   }

   public final int adjustCurrentHpIfAlive(long var1, int var3) {
      NativeDeCharacterFields.CurrentHpMutation var4 = this.withVitalMutation(() -> {
         if (this._currentHp <= 0 || this.isDead()) {
            return this.enqueueCurrentHpCompletionLocked(new NativeDeCharacterFields.CurrentHpMutation(this._currentHp, this._currentHp));
         }
         int var4x = boundedVitalValue(this._currentHp, var1, Math.min(this._currentHp, var3), this.getMaxHp());
         return this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(var4x));
      });
      return var4.currentHp();
   }

   public final int adjustCurrentMp(long var1) {
      NativeDeCharacterFields.CurrentMpMutation var3 = this.withVitalMutation(() -> {
         int var3x = boundedVitalValue(this._currentMp, var1, 0, this.getMaxMp());
         return this.enqueueCurrentMpCompletionLocked(this.commitCurrentMpLocked(var3x));
      });
      return var3.currentMp();
   }

   public final int debitCurrentMpUpTo(long var1) {
      if (var1 <= 0L) {
         return 0;
      } else {
         NativeDeCharacterFields.MpDebitMutation var3 = this.withVitalMutation(() -> {
            NativeDeCharacterFields.MpDebitMutation var3x = this.debitCurrentMpLocked(var1);
            this.enqueueCurrentMpCompletionLocked(var3x._mutation);
            return var3x;
         });
         return var3._debited;
      }
   }

   public final int debitCurrentMpUpToIfAlive(long var1) {
      if (var1 <= 0L) {
         return 0;
      } else {
         NativeDeCharacterFields.MpDebitMutation var3 = this.withVitalMutation(
            () -> {
               NativeDeCharacterFields.MpDebitMutation var3x = this.isDead()
                  ? new NativeDeCharacterFields.MpDebitMutation(0, new NativeDeCharacterFields.CurrentMpMutation(this._currentMp, this._currentMp))
                  : this.debitCurrentMpLocked(var1);
               this.enqueueCurrentMpCompletionLocked(var3x._mutation);
               return var3x;
            }
         );
         return var3._debited;
      }
   }

   private NativeDeCharacterFields.MpDebitMutation debitCurrentMpLocked(long var1) {
      int var3 = Math.max(0, this._currentMp);
      int var4 = (int)Math.min((long)var3, var1);
      NativeDeCharacterFields.CurrentMpMutation var5 = var4 > 0
         ? this.commitCurrentMpLocked(var3 - var4)
         : new NativeDeCharacterFields.CurrentMpMutation(this._currentMp, this._currentMp);
      return new NativeDeCharacterFields.MpDebitMutation(var4, var5);
   }

   public final void setCurrentHp(int var1) {
      this.withVitalMutation(() -> this.enqueueCurrentHpCompletionLocked(this.commitCurrentHpLocked(var1)));
   }

   public final void setCurrentHpDirect(int var1) {
      this.withVitalMutation(() -> {
         int var2 = this._currentHp;
         this._currentHp = var1;
         this.recordDamageHpMutation(var2, var1);
      });
   }

   public int getCurrentMp() {
      return this._currentMp;
   }

   public final void setCurrentMp(int var1) {
      this.withVitalMutation(() -> this.enqueueCurrentMpCompletionLocked(this.commitCurrentMpLocked(var1)));
   }

   public final void setCurrentMpDirect(int var1) {
      this.withVitalMutation(() -> this._currentMp = var1);
   }

   public synchronized int getLevel() {
      return this._level;
   }

   public synchronized void setLevel(int var1) {
      this._level = var1;
   }

   public int getMaxHp() {
      return this._maxHp;
   }

   public void setMaxHp(int var1) {
      this.withVitalMutation(() -> {
         int var2 = this._currentHp;
         this._trueMaxHp = var1;
         int var3 = this.isROClient() ? 999999 : 32767;
         this._maxHp = ensure(this._trueMaxHp, 1, var3);
         this._currentHp = Math.min(this._currentHp, this._maxHp);
         this.enqueueCurrentHpCompletionLocked(new NativeDeCharacterFields.CurrentHpMutation(var2, this._currentHp));
      });
   }

   public void addMaxHp(int var1) {
      this.withVitalMutation(() -> this.setMaxHp(this._trueMaxHp + var1));
   }

   public int getMaxMp() {
      return this._maxMp;
   }

   public void setMaxMp(int var1) {
      this.withVitalMutation(() -> {
         int var2 = this._currentMp;
         this._trueMaxMp = var1;
         int var3 = this.isROClient() ? 999999 : 32767;
         this._maxMp = ensure(this._trueMaxMp, 0, var3);
         this._currentMp = Math.min(this._currentMp, this._maxMp);
         this.enqueueCurrentMpCompletionLocked(new NativeDeCharacterFields.CurrentMpMutation(var2, this._currentMp));
      });
   }

   public void addMaxMp(int var1) {
      this.withVitalMutation(() -> this.setMaxMp(this._trueMaxMp + var1));
   }

   public int getAc() {
      return ensure(this._ac, this.getMinAcLimit(), 44);
   }

   public void setAc(int var1) {
      this._trueAc = var1;
      this._ac = ensure(var1, this.getMinAcLimit(), 44);
   }

   private int getMinAcLimit() {
      return this.isROClient() ? Integer.MIN_VALUE : -211;
   }

   public void addAc(int var1) {
      this.setAc(this._trueAc + var1);
   }

   public short getStr() {
      return this._str;
   }

   public void setStr(int var1) {
      this._trueStr = (short)var1;
      this._str = (short)ensure(var1, 1, 254);
   }

   public void addStr(int var1) {
      this.setStr(this._trueStr + var1);
   }

   public short getCon() {
      return this._con;
   }

   public void setCon(int var1) {
      this._trueCon = (short)var1;
      this._con = (short)ensure(var1, 1, 254);
   }

   public void addCon(int var1) {
      this.setCon(this._trueCon + var1);
   }

   public short getDex() {
      return this._dex;
   }

   public void setDex(int var1) {
      this._trueDex = (short)var1;
      this._dex = (short)ensure(var1, 1, 254);
   }

   public void addDex(int var1) {
      this.setDex(this._trueDex + var1);
   }

   public short getCha() {
      return this._cha;
   }

   public void setCha(int var1) {
      this._trueCha = (short)var1;
      this._cha = (short)ensure(var1, 1, 254);
   }

   public void addCha(int var1) {
      this.setCha(this._trueCha + var1);
   }

   public short getInt() {
      return this._int;
   }

   public void setInt(int var1) {
      this._trueInt = (short)var1;
      this._int = (short)ensure(var1, 1, 254);
   }

   public void addInt(int var1) {
      this.setInt(this._trueInt + var1);
   }

   public short getWis() {
      return this._wis;
   }

   public void setWis(int var1) {
      this._trueWis = (short)var1;
      this._wis = (short)ensure(var1, 1, 254);
   }

   public void addWis(int var1) {
      this.setWis(this._trueWis + var1);
   }

   public int getDmgup() {
      return this._dmgup;
   }

   public void addDmgup(int var1) {
      this._trueDmgup += var1;
      if (this._trueDmgup >= 127) {
         this._dmgup = 127;
      } else if (this._trueDmgup <= -128) {
         this._dmgup = -128;
      } else {
         this._dmgup = this._trueDmgup;
      }
   }

   public int getHitup() {
      return this._hitup;
   }

   public void addHitup(int var1) {
      this._trueHitup += var1;
      if (this._trueHitup >= 127) {
         this._hitup = 127;
      } else if (this._trueHitup <= -128) {
         this._hitup = -128;
      } else {
         this._hitup = this._trueHitup;
      }
   }

   public int getSp() {
      return this.getTrueSp() + this._sp;
   }

   public int getTrueSp() {
      return this.getMagicLevel() + this.getMagicBonus();
   }

   public void addSp(int var1) {
      this._sp += var1;
   }

   public boolean isDead() {
      return this._isDead;
   }

   protected final void setCharacterDead(boolean var1) {
      this.withVitalMutation(() -> {
         boolean var2 = this._isDead;
         this._isDead = var1;
         this.recordDamageDeathMutation(var2, var1);
      });
   }

   public int getStatus() {
      return this._status;
   }

   public void setStatus(int var1) {
      this.withVitalMutation(() -> { this._status = var1; });
   }

   public int getLawful() {
      return this._lawful;
   }

   public void setLawful(int var1) {
      this._lawful = var1;
   }

   public int getTempCharGfx() {
      return this._tempCharGfx;
   }

   public void setTempCharGfx(int var1) {
      this._tempCharGfx = var1;
   }

   public int getGfxId() {
      return this._gfxid;
   }

   public void setGfxId(int var1) {
      this._gfxid = var1;
   }

   public int getMagicLevel() {
      return Math.max(this.getLevel(), 1);
   }

   public int getMagicBonus() {
      int var1 = this.getInt() / 2;
      return Math.max(var1, 1);
   }

   protected void reportCompletionFailure(Throwable var1) {
      _log.error("Vital completion failed: objectId={} type={}", this.getId(), this.getClass().getName(), var1);
   }

   static NativeDeCharacterFields fromInitializedNpc(int var0, NativeDeCharacterFields.Snapshot var1) throws IOException {
      Objects.requireNonNull(var1);
      if (var1.clientType() != NativeDeCharacterControls.clientType()) {
         throw new IllegalArgumentException("NPC initial facts use a different source client bound policy");
      } else if (!var1.fields()
         .keySet()
         .equals(
            Set.of(
               "_currentHp",
               "_currentMp",
               "_maxHp",
               "_trueMaxHp",
               "_maxMp",
               "_trueMaxMp",
               "_status",
               "_level",
               "_ac",
               "_trueAc",
               "_lawful",
               "_dmgup",
               "_trueDmgup",
               "_hitup",
               "_trueHitup",
               "_sp",
               "_tempCharGfx",
               "_gfxid",
               "_str",
               "_trueStr",
               "_con",
               "_trueCon",
               "_dex",
               "_trueDex",
               "_cha",
               "_trueCha",
               "_int",
               "_trueInt",
               "_wis",
               "_trueWis"
            )
         )) {
         throw new IllegalArgumentException("complete explicit NPC initial Character fields required");
      } else {
         for (Entry var3 : var1.fields().entrySet()) {
            boolean var4 = Set.of("_cha", "_con", "_dex", "_int", "_str", "_trueCha", "_trueCon", "_trueDex", "_trueInt", "_trueStr", "_trueWis", "_wis")
               .contains(var3.getKey());
            if (((Number)var3.getValue()).getClass() != (var4 ? Short.class : Integer.class)) {
               throw new IllegalArgumentException("exact source primitive field type required: " + (String)var3.getKey());
            }
         }

         NativeDeCharacterFields var5 = new NativeDeCharacterFields(var1.clientType(), var0);
         var5._currentHp = var1.fields().get("_currentHp").intValue();
         var5._currentMp = var1.fields().get("_currentMp").intValue();
         var5._maxHp = var1.fields().get("_maxHp").intValue();
         var5._trueMaxHp = var1.fields().get("_trueMaxHp").intValue();
         var5._maxMp = var1.fields().get("_maxMp").intValue();
         var5._trueMaxMp = var1.fields().get("_trueMaxMp").intValue();
         var5._status = var1.fields().get("_status").intValue();
         var5._level = var1.fields().get("_level").intValue();
         var5._ac = var1.fields().get("_ac").intValue();
         var5._trueAc = var1.fields().get("_trueAc").intValue();
         var5._lawful = var1.fields().get("_lawful").intValue();
         var5._dmgup = var1.fields().get("_dmgup").intValue();
         var5._trueDmgup = var1.fields().get("_trueDmgup").intValue();
         var5._hitup = var1.fields().get("_hitup").intValue();
         var5._trueHitup = var1.fields().get("_trueHitup").intValue();
         var5._sp = var1.fields().get("_sp").intValue();
         var5._tempCharGfx = var1.fields().get("_tempCharGfx").intValue();
         var5._gfxid = var1.fields().get("_gfxid").intValue();
         var5._str = var1.fields().get("_str").shortValue();
         var5._trueStr = var1.fields().get("_trueStr").shortValue();
         var5._con = var1.fields().get("_con").shortValue();
         var5._trueCon = var1.fields().get("_trueCon").shortValue();
         var5._dex = var1.fields().get("_dex").shortValue();
         var5._trueDex = var1.fields().get("_trueDex").shortValue();
         var5._cha = var1.fields().get("_cha").shortValue();
         var5._trueCha = var1.fields().get("_trueCha").shortValue();
         var5._int = var1.fields().get("_int").shortValue();
         var5._trueInt = var1.fields().get("_trueInt").shortValue();
         var5._wis = var1.fields().get("_wis").shortValue();
         var5._trueWis = var1.fields().get("_trueWis").shortValue();
         var5._isDead = var1.dead();
         return var5;
      }
   }

   NativeDeCharacterFields.Snapshot snapshot() {
      TreeMap var1 = new TreeMap();
      this._npcVitalMutationLock.lock();

      try {
         var1.put("_level", this.getLevel());
         var1.put("_currentHp", this._currentHp);
         var1.put("_currentMp", this._currentMp);
         var1.put("_maxHp", this._maxHp);
         var1.put("_trueMaxHp", this._trueMaxHp);
         var1.put("_maxMp", this._maxMp);
         var1.put("_trueMaxMp", this._trueMaxMp);
         var1.put("_status", this._status);
         var1.put("_ac", this._ac);
         var1.put("_trueAc", this._trueAc);
         var1.put("_lawful", this._lawful);
         var1.put("_dmgup", this._dmgup);
         var1.put("_trueDmgup", this._trueDmgup);
         var1.put("_hitup", this._hitup);
         var1.put("_trueHitup", this._trueHitup);
         var1.put("_sp", this._sp);
         var1.put("_tempCharGfx", this._tempCharGfx);
         var1.put("_gfxid", this._gfxid);
         var1.put("_str", this._str);
         var1.put("_trueStr", this._trueStr);
         var1.put("_con", this._con);
         var1.put("_trueCon", this._trueCon);
         var1.put("_dex", this._dex);
         var1.put("_trueDex", this._trueDex);
         var1.put("_cha", this._cha);
         var1.put("_trueCha", this._trueCha);
         var1.put("_int", this._int);
         var1.put("_trueInt", this._trueInt);
         var1.put("_wis", this._wis);
         var1.put("_trueWis", this._trueWis);
         return new NativeDeCharacterFields.Snapshot(this.clientType, var1, this._isDead);
      } finally {
         this._npcVitalMutationLock.unlock();
      }

   }

   NativeDeCharacterFields(int var1, NativeDeCharacterFields.Snapshot var2) throws IOException {
      this(var2.clientType(), var1);
      NativeDeCharacterFields var3 = fromInitializedNpc(var1, var2);
      this._currentHp = var3._currentHp;
      this._currentMp = var3._currentMp;
      this._maxHp = var3._maxHp;
      this._trueMaxHp = var3._trueMaxHp;
      this._maxMp = var3._maxMp;
      this._trueMaxMp = var3._trueMaxMp;
      this._status = var3._status;
      this._level = var3._level;
      this._ac = var3._ac;
      this._trueAc = var3._trueAc;
      this._lawful = var3._lawful;
      this._dmgup = var3._dmgup;
      this._trueDmgup = var3._trueDmgup;
      this._hitup = var3._hitup;
      this._trueHitup = var3._trueHitup;
      this._sp = var3._sp;
      this._tempCharGfx = var3._tempCharGfx;
      this._gfxid = var3._gfxid;
      this._str = var3._str;
      this._trueStr = var3._trueStr;
      this._con = var3._con;
      this._trueCon = var3._trueCon;
      this._dex = var3._dex;
      this._trueDex = var3._trueDex;
      this._cha = var3._cha;
      this._trueCha = var3._trueCha;
      this._int = var3._int;
      this._trueInt = var3._trueInt;
      this._wis = var3._wis;
      this._trueWis = var3._trueWis;
      this._isDead = var3._isDead;
   }

   protected NativeDeResurrectionEffects resurrectionEffects() {
      return NativeDeResurrectionEffects.UNBOUND;
   }

   protected void setDeadForResurrection(boolean var1) {
      this.setCharacterDead(var1);
   }

   public void resurrect(int var1) {
      try {
         int var2 = Math.max(1, var1);
         NativeDeCharacterFields.CurrentHpMutation var3 = this.withVitalMutation(() -> {
            if (!this.isDead()) {
               return null;
            } else {
               this.setDeadForResurrection(false);
               NativeDeCharacterFields.CurrentHpMutation var2x = this.commitCurrentHpLocked(var2);
               this.setStatus(0);
               return this.enqueueCurrentHpCompletionLocked(var2x);
            }
         });
         if (var3 == null) {
            return;
         }

         this.resurrectionEffects().undoPoly(this);

         for (Viewer var5 : this.resurrectionEffects().recognizePlayers(this)) {
            var5.sendRemoveObject(this.resurrectionEffects().newRemoveObject(this.getId()));
            var5.removeKnownObject(this);
            var5.updateObject();
         }
      } catch (Exception var6) {
         this.resurrectionEffects().characterFailure(var6.getLocalizedMessage(), var6);
      }
   }

   protected final <T> T withKnownLock(Supplier<T> var1) {
      this._knownObjectLock.lock();

      try {
         return var1.get();
      } finally {
         this._knownObjectLock.unlock();
      }

   }

   public boolean hasSkillEffect(int var1) {
      return this._skillEffectSnapshot.activeSkillIds.contains(var1);
   }

   public Map<Integer, NativeDeDeletionState> getPetList() {
      return this.pets;
   }

   public Map<Integer, NativeDeDeletionPorts.Doll> getDolls() {
      return this.dolls;
   }

   public Poison getPoison() {
      Poison var1 = this._poison;
      return var1 instanceof PoisonTransition ? null : var1;
   }

   public long getExp() {
      return this._exp;
   }

   public void setExp(long var1) {
      this._exp = var1;
   }

   public boolean knownsObject(NativeDeDeletionPorts.ObjectRef var1) {
      return this._knownObjects.contains(var1);
   }

   public List<NativeDeDeletionPorts.ObjectRef> getKnownObjects() {
      return this._knownObjects;
   }

   public List<NativeDeDeletionPorts.Viewer> getKnownPlayers() {
      return this._knownPlayer;
   }

   public void addKnownObject(NativeDeDeletionPorts.ObjectRef var1) {
      this.tryAddKnownObject(var1);
   }

   public boolean tryAddKnownObject(NativeDeDeletionPorts.ObjectRef var1) {
      if (var1 == null) { return false; }
      this._knownObjectLock.lock();
      try {
         if (this instanceof NativeDeDeletionPorts.Viewer var2 && var1 instanceof NativeDeDeletionState var3 && !var3.registerViewer(var2)) {
            return false;
         }
         if (this._knownObjects.addIfAbsent(var1) && var1 instanceof NativeDeDeletionPorts.Viewer) {
            this._knownPlayer.addIfAbsent((NativeDeDeletionPorts.Viewer)var1);
         }
         return true;
      } finally {
         this._knownObjectLock.unlock();
      }
   }

   public void removeKnownObject(NativeDeDeletionPorts.ObjectRef var1) {
      this.removeKnownObjectIfPresent(var1);
   }

   public boolean removeKnownObjectIfPresent(NativeDeDeletionPorts.ObjectRef var1) {
      if (var1 == null) {
         return false;
      } else {
         this._knownObjectLock.lock();

         try {
            boolean var2 = this._knownObjects.remove(var1);
            if (var1 instanceof NativeDeDeletionPorts.Viewer) {
               this._knownPlayer.remove(var1);
            }

            if (this instanceof NativeDeDeletionPorts.Viewer var3 && var1 instanceof NativeDeDeletionState var4) {
               var4.unregisterViewer(var3);
            }

            return var2;
         } finally {
            this._knownObjectLock.unlock();
         }

      }
   }

   public boolean isKnownNpcViewerConsistent(NativeDeDeletionState var1) {
      if (!(this instanceof NativeDeDeletionPorts.Viewer var2) || var1 == null) { return true; }
      this._knownObjectLock.lock();
      try {
         return this._knownObjects.contains(var1) == var1.isViewerRegistered(var2);
      } finally {
         this._knownObjectLock.unlock();
      }
   }

   public void removeAllKnownObjects() {
      this._knownObjectLock.lock();

      try {
         if (this instanceof NativeDeDeletionPorts.Viewer var1) {
            for (NativeDeDeletionPorts.ObjectRef var3 : this._knownObjects) {
               if (var3 instanceof NativeDeDeletionState var4) {
                  var4.unregisterViewer(var1);
               }
            }
         }

         this._knownObjects.clear();
         this._knownPlayer.clear();
      } finally {
         this._knownObjectLock.unlock();
      }
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public int getWind() {
      return this._wind;
   }

   public void addWind(int var1) {
      this._trueWind += var1;
      if (this._trueWind >= 127) {
         this._wind = 127;
      } else if (this._trueWind <= -128) {
         this._wind = -128;
      } else {
         this._wind = this._trueWind;
      }
   }

   public int getWater() {
      return this._water;
   }

   public void addWater(int var1) {
      this._trueWater += var1;
      if (this._trueWater >= 127) {
         this._water = 127;
      } else if (this._trueWater <= -128) {
         this._water = -128;
      } else {
         this._water = this._trueWater;
      }
   }

   public int getFire() {
      return this._fire;
   }

   public void addFire(int var1) {
      this._trueFire += var1;
      if (this._trueFire >= 127) {
         this._fire = 127;
      } else if (this._trueFire <= -128) {
         this._fire = -128;
      } else {
         this._fire = this._trueFire;
      }
   }

   public int getEarth() {
      return this._earth;
   }

   public void addEarth(int var1) {
      this._trueEarth += var1;
      if (this._trueEarth >= 127) {
         this._earth = 127;
      } else if (this._trueEarth <= -128) {
         this._earth = -128;
      } else {
         this._earth = this._trueEarth;
      }
   }

   public int getAddAttrKind() {
      return this._addAttrKind;
   }

   public void setAddAttrKind(int var1) {
      this._addAttrKind = var1;
   }

   public int getRegistStun() {
      return this._registStun;
   }

   public void addRegistStun(int var1) {
      this._trueRegistStun += var1;
      if (this._trueRegistStun > 127) {
         this._registStun = 127;
      } else if (this._trueRegistStun < -128) {
         this._registStun = -128;
      } else {
         this._registStun = this._trueRegistStun;
      }
   }

   public int getRegistStone() {
      return this._registStone;
   }

   public void addRegistStone(int var1) {
      this._trueRegistStone += var1;
      if (this._trueRegistStone > 127) {
         this._registStone = 127;
      } else if (this._trueRegistStone < -128) {
         this._registStone = -128;
      } else {
         this._registStone = this._trueRegistStone;
      }
   }

   public int getRegistSleep() {
      return this._registSleep;
   }

   public void addRegistSleep(int var1) {
      this._trueRegistSleep += var1;
      if (this._trueRegistSleep > 127) {
         this._registSleep = 127;
      } else if (this._trueRegistSleep < -128) {
         this._registSleep = -128;
      } else {
         this._registSleep = this._trueRegistSleep;
      }
   }

   public int getRegistFreeze() {
      return this._registFreeze;
   }

   public void add_regist_freeze(int var1) {
      this._trueRegistFreeze += var1;
      if (this._trueRegistFreeze > 127) {
         this._registFreeze = 127;
      } else if (this._trueRegistFreeze < -128) {
         this._registFreeze = -128;
      } else {
         this._registFreeze = this._trueRegistFreeze;
      }
   }

   public int getRegistSustain() {
      return this._registSustain;
   }

   public void addRegistSustain(int var1) {
      this._trueRegistSustain += var1;
      if (this._trueRegistSustain > 127) {
         this._registSustain = 127;
      } else if (this._trueRegistSustain < -128) {
         this._registSustain = -128;
      } else {
         this._registSustain = this._trueRegistSustain;
      }
   }

   public int getRegistBlind() {
      return this._registBlind;
   }

   public void addRegistBlind(int var1) {
      this._trueRegistBlind += var1;
      if (this._trueRegistBlind > 127) {
         this._registBlind = 127;
      } else if (this._trueRegistBlind < -128) {
         this._registBlind = -128;
      } else {
         this._registBlind = this._trueRegistBlind;
      }
   }

   public int getBowDmgup() {
      return this._bowDmgup;
   }

   public void addBowDmgup(int var1) {
      this._trueBowDmgup += var1;
      if (this._trueBowDmgup >= 127) {
         this._bowDmgup = 127;
      } else if (this._trueBowDmgup <= -128) {
         this._bowDmgup = -128;
      } else {
         this._bowDmgup = this._trueBowDmgup;
      }
   }

   public int getBowHitup() {
      return this._bowHitup;
   }

   public void addBowHitup(int var1) {
      this._trueBowHitup += var1;
      if (this._trueBowHitup >= 127) {
         this._bowHitup = 127;
      } else if (this._trueBowHitup <= -128) {
         this._bowHitup = -128;
      } else {
         this._bowHitup = this._trueBowHitup;
      }
   }

   public int getStunSuccess() {
      return this._stunSuccess;
   }

   public void addStunSuccess(int var1) {
      this._stunSuccess += var1;
   }

   public int getMr() {
      if (this.hasSkillEffect(153) == true) {
         return this._mr >> 2;
      }
      return this._mr;
   }

   public int getTrueMr() {
      return this._trueMr;
   }

   public void addMr(int var1) {
      this._trueMr += var1;
      if (this._trueMr <= 0) {
         this._mr = 0;
      } else {
         this._mr = this._trueMr;
      }
   }

   public String getTitle() {
      return this._title;
   }

   public void setTitle(String var1) {
      this._title = var1;
   }

   public int getHeading() {
      return this._heading;
   }

   public void setHeading(int var1) {
      this._heading = var1;
   }

   public int getMoveSpeed() {
      return this._moveSpeed;
   }

   public void setMoveSpeed(int var1) {
      this._moveSpeed = var1;
   }

   public int getBraveSpeed() {
      return this._braveSpeed;
   }

   public void setBraveSpeed(int var1) {
      this._braveSpeed = var1;
   }

   public boolean isInvisble() {
      return this.hasSkillEffect(60) || this.hasSkillEffect(97);
   }

   public int getKarma() {
      return this._karma;
   }

   public void setKarma(int var1) {
      this._karma = var1;
   }

   public void setMr(int var1) {
      this._trueMr = var1;
      if (this._trueMr <= 0) {
         this._mr = 0;
      } else {
         this._mr = this._trueMr;
      }
   }

   public int getChaLightSize() {
      if (this.isInvisble()) {
         return 0;
      }
      return this._chaLightSize;
   }

   public void setChaLightSize(int var1) {
      this._chaLightSize = var1;
   }

   public int getOwnLightSize() {
      if (this.isInvisble()) {
         return 0;
      }
      return this._ownLightSize;
   }

   public void setOwnLightSize(int var1) {
      this._ownLightSize = var1;
   }

   public boolean isSleeped() {
      return this._sleeped;
   }

   public boolean isParalyzed() {
      return this._paralyzed;
   }

   private int hpMeterRatio() {
      int var1 = NativeDeVisibilityAccess.L1Character$getMaxHp(this);
      if (var1 <= 0) {
         return 100;
      }
      return 100 * NativeDeVisibilityAccess.L1Character$getCurrentHp(this) / var1;
   }

   private final void visibilitySource$broadcastPacketHP(NativeDeDeletionPorts.Viewer var1) {
      try {
         int var2 = this.hpMeterRatio();
         int var3 = NativeDeVisibilityAccess.L1PcInstance$getId(var1);
         if (this._secHpByViewer.size() >= 1024 && !this._secHpByViewer.containsKey(var3)) {
            this._secHpByViewer.clear();
         }

         NativeDeCharacterFields.ViewerHpState var4 = this._secHpByViewer
            .compute(
               var3,
               (var1x, var2x) -> (NativeDeCharacterFields.ViewerHpState)(var2x != null && var2x.belongsTo(var1)
                  ? var2x
                  : new NativeDeCharacterFields.ViewerHpState(var1))
            );
         if (var4.lastHpRatio.getAndSet(var2) != var2) {
            NativeDeVisibilityAccess.L1PcInstance$sendPackets(var1, new NativeDeVisibilityHp(NativeDeVisibilityAccess.L1Character$getId(this), var2));
         }
      } catch (Exception var5) {
         NativeDeVisibilityScope.log().error(var5.getLocalizedMessage(), (Throwable)var5);
      }
   }

   public void broadcastPacketHP(NativeDeDeletionPorts.Viewer var1) {
      NativeDeVisibilityRuntime.broadcastPacketHP(this, var1);
   }

   private final void visibilitySource$hideTrackedHpBars() {
      try {
         for (NativeDeCharacterFields.ViewerHpState var2 : this._secHpByViewer.values()) {
            NativeDeDeletionPorts.Viewer var3 = var2.viewer.get();

            try {
               if (var3 != null) {
                  NativeDeVisibilityAccess.L1PcInstance$sendPackets(var3, new NativeDeVisibilityHp(NativeDeVisibilityAccess.L1Character$getId(this), 255));
               }
            } catch (Exception var8) {
               NativeDeVisibilityScope.log()
                  .error(
                     "HP meter hide failed: objectId={} viewerId={}",
                     NativeDeVisibilityAccess.L1Character$getId(this),
                     var3 != null ? NativeDeVisibilityAccess.L1PcInstance$getId(var3) : -1,
                     var8
                  );
            }
         }
      } finally {
         this._secHpByViewer.clear();
      }
   }

   protected final void hideTrackedHpBars() {
      NativeDeVisibilityRuntime.hideTrackedHpBars(this);
   }

   public record CombatSkillDerived(
      int meleeHitBonus, int rangedHitBonus, int meleeCookingDmgBonus, int rangedCookingDmgBonus, int skd3FlatModifier, boolean skd3HalveDamage
   ) {
      public static NativeDeCharacterFields.CombatSkillDerived compute(Iterable<Integer> var0) {
         int var1 = 0;
         int var2 = 0;
         int var3 = 0;
         int var4 = 0;
         int var5 = 0;
         boolean var6 = false;

         for (Integer var8 : var0) {
            if (var8 == null) {
               continue;
            }
            {
               Integer var9 = (Integer)NativeDeCharacterFields.SKU1.get(var8);
               if (var9 != null) {
                  var1 += var9;
               }

               Integer var10 = (Integer)NativeDeCharacterFields.SKU2.get(var8);
               if (var10 != null) {
                  var2 += var10;
               }

               Integer var11 = (Integer)NativeDeCharacterFields.SKD1.get(var8);
               if (var11 != null) {
                  var3 += var11;
               }

               Integer var12 = (Integer)NativeDeCharacterFields.SKD2.get(var8);
               if (var12 != null) {
                  var4 += var12;
               }

               Integer var13 = (Integer)NativeDeCharacterFields.SKD3.get(var8);
               if (var13 != null) {
                  if (var13.equals(var8)) {
                     var6 = true;
                  } else {
                     var5 += var13;
                  }
               }
            }
         }

         return new NativeDeCharacterFields.CombatSkillDerived(var1, var2, var3, var4, var5, var6);
      }
   }

   protected static class CurrentHpMutation {
      private final int _previousHp;
      private final int _currentHp;

      protected CurrentHpMutation(int var1, int var2) {
         this._previousHp = var1;
         this._currentHp = var2;
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

      protected CurrentMpMutation(int var1, int var2) {
         this._previousMp = var1;
         this._currentMp = var2;
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

   protected static final class DamageMutationReceipt {
      private final NativeDeCharacterFields _target;
      private final int _startHp;
      private int _hpBefore;
      private int _hpAfter;
      private boolean _hpSeen;
      private boolean _hpReduced;
      private boolean _deadAfter;
      private boolean _deathSeen;

      private DamageMutationReceipt(NativeDeCharacterFields var1) {
         this._target = var1;
         this._startHp = var1.getCurrentHp();
         this._hpBefore = this._startHp;
         this._hpAfter = this._startHp;
         this._deadAfter = var1.isDead();
      }

      private void recordHp(int var1, int var2) {
         if (var1 == var2) {
            return;
         }
         {
            if (!this._hpSeen) {
               this._hpBefore = var1;
               this._hpSeen = true;
            }

            this._hpAfter = var2;
            this._hpReduced |= var2 < var1;
         }
      }

      private void recordDead(boolean var1, boolean var2, int var3) {
         if (var1 == var2) {
            return;
         }
         {
            this._deathSeen = true;
            this._deadAfter = var2;
            if (!this._hpSeen) {
               this._hpBefore = var3;
               this._hpAfter = var3;
            }
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

   private static final class ErrorLog {
      void error(String var1, Object var2, Object var3, Object var4) {
         System.getLogger(NativeDeCharacterFields.class.getName())
            .log(Level.ERROR, "Factory vital completion failed id=" + var2 + " type=" + var3, (Throwable)var4);
      }
   }

   private static final class MpDebitMutation {
      private final int _debited;
      private final NativeDeCharacterFields.CurrentMpMutation _mutation;

      private MpDebitMutation(int var1, NativeDeCharacterFields.CurrentMpMutation var2) {
         this._debited = var1;
         this._mutation = var2;
      }
   }

   private static final class SkillEffectSnapshot {
      private static final NativeDeCharacterFields.CombatSkillDerived EMPTY_DERIVED = new NativeDeCharacterFields.CombatSkillDerived(0, 0, 0, 0, 0, false);
      private static final NativeDeCharacterFields.SkillEffectSnapshot EMPTY = new NativeDeCharacterFields.SkillEffectSnapshot(Set.of(), EMPTY_DERIVED);
      private final Set<Integer> activeSkillIds;
      private final NativeDeCharacterFields.CombatSkillDerived combat;

      private SkillEffectSnapshot(Set<Integer> var1, NativeDeCharacterFields.CombatSkillDerived var2) {
         this.activeSkillIds = var1;
         this.combat = var2;
      }
   }

   record Snapshot(int clientType, Map<String, Number> fields, boolean dead) {
      Snapshot(int clientType, Map<String, Number> fields, boolean dead) {
         fields = Map.copyOf(fields);
         this.clientType = clientType;
         this.fields = fields;
         this.dead = dead;
      }
   }

   private static final class ViewerHpState {
      private final WeakReference<NativeDeDeletionPorts.Viewer> viewer;
      private final AtomicInteger lastHpRatio = new AtomicInteger(-1);

      private ViewerHpState(NativeDeDeletionPorts.Viewer var1) {
         this.viewer = new WeakReference<>(var1);
      }

      private boolean belongsTo(NativeDeDeletionPorts.Viewer var1) {
         return this.viewer.get() == var1;
      }
   }
}
