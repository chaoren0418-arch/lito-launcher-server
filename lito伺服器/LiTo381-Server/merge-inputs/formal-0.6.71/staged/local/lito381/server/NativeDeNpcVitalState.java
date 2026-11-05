// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeCharacterFields.Snapshot;
import local.lito381.server.NativeDeResurrectionEffects.LoginSkill;
import local.lito381.server.NativeDeathLifecycleGate.Ownership;
import local.lito381.server.NativeDeathLifecycleGate.Phase;
import local.lito381.server.NativeDeathLifecycleGate.ResurrectionClaim;
import local.lito381.server.NativeDeathLifecycleGate.State;

class NativeDeNpcVitalState extends NativeDeCharacterFields {
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

   NativeDeNpcVitalState(int var1, int var2) {
      super(var1, var2);
   }

   NativeDeNpcVitalState(int var1, Snapshot var2) throws IOException {
      super(var1, var2);
   }

   protected final boolean markAsyncDeathPending(long var1) {
      return this._asyncDeathLifecycle.markDeadPending(var1);
   }

   protected final Ownership asyncDeathOwnership(long var1) {
      return this._asyncDeathLifecycle.ownership(var1);
   }

   protected final boolean isAsyncDeathPending(long var1) {
      return this._asyncDeathLifecycle.isPending(var1);
   }

   protected final boolean isAsyncDeathGeneration(long var1) {
      return this._asyncDeathLifecycle.isDeathGeneration(var1);
   }

   public int getWeaponBreakStack() {
      return this._weaponBreakStack;
   }

   public void addWeaponBreakStack() {
      if (this._weaponBreakStack < 10) {
         this._weaponBreakStack++;
      }
   }

   public void setWeaponBreakStack(int var1) {
      this._weaponBreakStack = Math.max(0, Math.min(10, var1));
   }

   public void setDead(boolean var1) {
      this.withVitalMutation(() -> {
         if (!this._asyncDeathLifecycle.isManaged()) {
            this.setCharacterDead(var1);
            if (var1) {
               this.setWeaponBreakStack(0);
            }
            return;
         }
         {
            State var2 = this._asyncDeathLifecycle.snapshot();
            if (var1) {
               if (var2.phase() != Phase.RESURRECTING && var2.phase() != Phase.TERMINATED) {
                  this.setCharacterDead(true);
                  this.setWeaponBreakStack(0);
               }
               return;
            }
            {
               switch (var2.phase()) {
                  case ACTIVE:
                  case RESURRECTING:
                     this.setCharacterDead(false);
                     break;
                  case DEAD_PENDING:
                  case DEAD_SETTLED:
                  case DEATH_DROPPED:
                     if (this._asyncDeathLifecycle.reviveDirect(var2)) {
                        this.setCharacterDead(false);
                     }
                     break;
                  case FINALIZING:
                  case TERMINATED:
               }
            }
         }
      });
   }

   public boolean destroyed() {
      return this._destroyed;
   }

   State lifecycleSnapshot() {
      return this._asyncDeathLifecycle.snapshot();
   }

   NativeDeathLifecycleGate deathGate() {
      return this._asyncDeathLifecycle;
   }

   @Override
   protected void setDeadForResurrection(boolean var1) {
      this.setDead(var1);
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
   public synchronized void resurrect(int var1) {
      if (!this._destroyed) {
         if (this._corpseDeletionClaimed) {
            long var4 = CORPSE_RESURRECTION_REJECTED_COUNT.incrementAndGet();
            this.resurrectionEffects().corpseRejected(this.getId(), this.getNpcId(), var4);
         } else {
            boolean var2 = this.withVitalMutation(() -> {
               if (!this._asyncDeathLifecycle.isManaged()) {
                  super.resurrect(var1);
                  return !this.isDead();
               } else {
                  ResurrectionClaim var2x = this._asyncDeathLifecycle.tryReserveResurrection();
                  if (var2x == null) {
                     return false;
                  } else {
                     try {
                        super.resurrect(var1);
                        if (this.isDead()) {
                           this._asyncDeathLifecycle.abortResurrection(var2x);
                           return false;
                        } else if (!this._asyncDeathLifecycle.completeResurrection(var2x)) {
                           this.setCharacterDead(true);
                           return false;
                        } else {
                           return true;
                        }
                     } catch (RuntimeException | Error var4x) {
                        this._asyncDeathLifecycle.abortResurrection(var2x);
                        throw var4x;
                     }
                  }
               }
            });
            if (var2) {
               this.setWeaponBreakStack(0);
               this.startHpRegeneration();
               this.startMpRegeneration();
               this.resurrectionEffects().broadcastResurrection(this, this, 0);
               LoginSkill var3 = this.resurrectionEffects().newSkillUse();
               var3.handleCommands(null, 44, this.getId(), this.getX(), this.getY(), 0, 1, this);
               this.clearCorpseDeleteTimerLocked();
            }
         }
      }
   }

   public final void startHpRegeneration() {
      if (!this.destroyed() || !this.isDead()) {
         if (this.getCurrentHp() > 0) {
            if (!this._hprRunning) {
               this._hprRunning = true;
            }
         }
      }
   }

   public final void startMpRegeneration() {
      if (!this.destroyed() || !this.isDead()) {
         if (this.getCurrentHp() > 0) {
            if (!this._mprRunning) {
               this._mprRunning = true;
            }
         }
      }
   }

   public final boolean isHpRegenerationX() {
      return this._hprRunning;
   }

   public final boolean isMpRegenerationX() {
      return this._mprRunning;
   }

   public synchronized boolean claimExpiredCorpseDeletion(long var1) {
      if (!this._destroyed && !this._deleteStarted.get() && !this._corpseDeletionClaimed && this._corpseDeleteDeadlineNanos >= 0L && this.isDead()) {
         if (var1 - this._corpseDeleteDeadlineNanos < 0L) {
            return false;
         } else {
            this._corpseDeletionClaimed = true;
            this._deadTimerTemp = -1;
            this._corpseDeleteDeadlineNanos = -1L;
            return true;
         }
      } else {
         return false;
      }
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

   protected synchronized void startDeleteTimer(int var1) {
      if (this._destroyed || this._deleteStarted.get()) {
         long var2 = CORPSE_TIMER_INVALID_START_COUNT.incrementAndGet();
         this.corpseClock().invalidStart(this.getId(), this.getNpcId(), var1, var2);
      } else if (this._corpseDeleteDeadlineNanos < 0L && !this._corpseDeletionClaimed) {
         this._deadTimerTemp = Math.max(0, var1);
         this._corpseDeleteDeadlineNanos = this.corpseNanoTime() + TimeUnit.SECONDS.toNanos(this._deadTimerTemp);
         this.corpseClock().track(this);
      }
   }

   public synchronized void set_deadTimerTemp(int var1) {
      if (var1 < 0) {
         this.clearCorpseDeleteTimerLocked();
         this._corpseDeletionClaimed = false;
      } else {
         this._deadTimerTemp = var1;
         this._corpseDeleteDeadlineNanos = this.corpseNanoTime() + TimeUnit.SECONDS.toNanos(var1);
         this._corpseDeletionClaimed = false;
         this.corpseClock().track(this);
      }
   }

   public synchronized int get_deadTimerTemp() {
      if (this._corpseDeleteDeadlineNanos < 0L) {
         return this._deadTimerTemp;
      } else {
         long var1 = this._corpseDeleteDeadlineNanos - this.corpseNanoTime();
         return var1 <= 0L ? 0 : (int)Math.min(2147483647L, TimeUnit.NANOSECONDS.toSeconds(var1) + 1L);
      }
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

   public final void sourceNpcSetDead(boolean var1) {
      this.withVitalMutation(() -> {
         if (!this._asyncDeathLifecycle.isManaged()) {
            this.setCharacterDead(var1);
            if (var1) {
               this.setWeaponBreakStack(0);
            }
            return;
         }
         {
            State var2 = this._asyncDeathLifecycle.snapshot();
            if (var1) {
               if (var2.phase() != Phase.RESURRECTING && var2.phase() != Phase.TERMINATED) {
                  this.setCharacterDead(true);
                  this.setWeaponBreakStack(0);
               }
               return;
            }
            {
               switch (var2.phase()) {
                  case ACTIVE:
                  case RESURRECTING:
                     this.setCharacterDead(false);
                     break;
                  case DEAD_PENDING:
                  case DEAD_SETTLED:
                  case DEATH_DROPPED:
                     if (this._asyncDeathLifecycle.reviveDirect(var2)) {
                        this.setCharacterDead(false);
                     }
                     break;
                  case FINALIZING:
                  case TERMINATED:
               }
            }
         }
      });
   }
}
