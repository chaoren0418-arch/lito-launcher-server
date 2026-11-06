// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NativeSourceSkillDelay {
   private static final NativeReuseLog.Logger _log = NativeReuseLog.getLogger(NativeSourceSkillDelay.class);
   private static final AtomicLong USE_COUNT = new AtomicLong();
   private static final AtomicLong SCHEDULED_COUNT = new AtomicLong();
   private static final AtomicLong ACTIVE_COUNT = new AtomicLong();
   private static final AtomicLong COMPLETED_COUNT = new AtomicLong();
   private static final AtomicLong TIMER_RUN_COUNT = new AtomicLong();
   private static final AtomicLong DELAY_SET_COUNT = new AtomicLong();
   private static final AtomicLong DELAY_CLEAR_COUNT = new AtomicLong();
   private static final AtomicLong SKIP_COUNT = new AtomicLong();
   private static final AtomicLong REJECTED_COUNT = new AtomicLong();
   private static final AtomicLong ERROR_COUNT = new AtomicLong();

   private NativeSourceSkillDelay() {
   }

   private static ScheduledFuture<?> scheduleDelayTimer(NativeSourceSkillDelay.Lease var0, int var1) {
      if (var0 == null || !var0.isOwned()) {
         SKIP_COUNT.incrementAndGet();
         return null;
      }
         try {
            SCHEDULED_COUNT.incrementAndGet();
            ScheduledFuture var2 = var0._cha.pcSchedule(() -> {
               ACTIVE_COUNT.incrementAndGet();

               try {
                  var0.expire();
                  TIMER_RUN_COUNT.incrementAndGet();
               } catch (RuntimeException var5) {
                  ERROR_COUNT.incrementAndGet();
                  _log.error("L1SkillDelay timer failed: chaId={}", var0.characterId(), var5);
                  throw var5;
               } finally {
                  COMPLETED_COUNT.incrementAndGet();
                  ACTIVE_COUNT.decrementAndGet();
               }
            }, var1);
            if (var2 == null) {
               REJECTED_COUNT.incrementAndGet();
               ERROR_COUNT.incrementAndGet();
               _log.warn("L1SkillDelay timer schedule returned null: chaId={} time={}", var0.characterId(), var1);
               return null;
            } else {
               return var2;
            }
         } catch (RejectedExecutionException var3) {
            REJECTED_COUNT.incrementAndGet();
            ERROR_COUNT.incrementAndGet();
            _log.warn("L1SkillDelay timer rejected: chaId={} time={}", var0.characterId(), var1, var3);
            return null;
         } catch (RuntimeException var4) {
            REJECTED_COUNT.incrementAndGet();
            ERROR_COUNT.incrementAndGet();
            _log.warn("L1SkillDelay timer submit failed: chaId={} time={}", var0.characterId(), var1, var4);
            return null;
         }
   }

   public static void onSkillUse(NativeReuseActor var0, int var1) {
      tryOnSkillUse(var0, var1);
   }

   public static boolean tryOnSkillUse(NativeReuseActor var0, int var1) {
      NativeSourceSkillDelay.Lease var2 = tryReserveSkillUse(var0, var1);
      if (var2 == null) {
         return false;
      } else {
         var2.commit();
         return true;
      }
   }

   public static NativeSourceSkillDelay.Lease tryReserveSkillUse(NativeReuseActor var0, int var1) {
      USE_COUNT.incrementAndGet();
      if (var0 == null) {
         SKIP_COUNT.incrementAndGet();
         return null;
      } else if (var1 <= 0) {
         return NativeSourceSkillDelay.Lease.noDelay();
      } else {
         NativeSourceSkillDelay.Lease var2 = new NativeSourceSkillDelay.Lease(var0, new Object(), true, var1);

         try {
            if (!var0.trySetSkillDelay(var2._token)) {
               SKIP_COUNT.incrementAndGet();
               return null;
            } else {
               DELAY_SET_COUNT.incrementAndGet();
               ScheduledFuture var3 = scheduleDelayTimer(var2, var1);
               if (var3 == null) {
                  var2.cancel();
                  return null;
               } else {
                  var2.installFuture(var3);
                  return var2;
               }
            }
         } catch (Exception var4) {
            ERROR_COUNT.incrementAndGet();
            _log.error(var4.getLocalizedMessage(), var4);
            var2.cancel();
            return null;
         }
      }
   }

   public static String getMonitorLine() {
      return String.format(
         "L1SkillDelay use=%d scheduled=%d active=%d done=%d timerRun=%d delaySet=%d delayClear=%d skip=%d rejected=%d error=%d",
         USE_COUNT.get(),
         SCHEDULED_COUNT.get(),
         ACTIVE_COUNT.get(),
         COMPLETED_COUNT.get(),
         TIMER_RUN_COUNT.get(),
         DELAY_SET_COUNT.get(),
         DELAY_CLEAR_COUNT.get(),
         SKIP_COUNT.get(),
         REJECTED_COUNT.get(),
         ERROR_COUNT.get()
      );
   }

   public static final class Lease {
      private final NativeReuseActor _cha;
      private final Object _token;
      private final boolean _owned;
      private final int _delayMillis;
      private final AtomicBoolean _cancelOpen = new AtomicBoolean(true);
      private final AtomicBoolean _committed = new AtomicBoolean(false);
      private final AtomicBoolean _terminal = new AtomicBoolean(false);
      private volatile long _deadlineNanos;
      private volatile ScheduledFuture<?> _future;

      private Lease(NativeReuseActor var1, Object var2, boolean var3, int var4) {
         this._cha = var1;
         this._token = var2;
         this._owned = var3;
         this._delayMillis = Math.max(0, var4);
         this._deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(this._delayMillis);
      }

      private static NativeSourceSkillDelay.Lease noDelay() {
         return new NativeSourceSkillDelay.Lease(null, null, false, 0);
      }

      private boolean isOwned() {
         return this._owned;
      }

      private int characterId() {
         return this._cha != null ? this._cha.getId() : 0;
      }

      private void installFuture(ScheduledFuture<?> var1) {
         this._future = var1;
         if (this._terminal.get()) {
            var1.cancel(false);
         }
      }

      public void commit() {
         if (this._owned && this._cancelOpen.compareAndSet(true, false)) {
            this._deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(this._delayMillis);
            this._committed.set(true);
         }
      }

      public boolean cancel() {
         if (!this._owned) {
            return true;
         } else if (!this._cancelOpen.compareAndSet(true, false)) {
            return false;
         } else {
            this._terminal.set(true);
            ScheduledFuture var1 = this._future;
            if (var1 != null) {
               var1.cancel(false);
            }

            if (this._cha.clearSkillDelay(this._token)) {
               NativeSourceSkillDelay.DELAY_CLEAR_COUNT.incrementAndGet();
            }

            return true;
         }
      }

      private void expire() {
         if (!this._owned || this._terminal.get()) return;
         if (!this._committed.get()) {
            this.reschedule(Math.max(1L, (long)this._delayMillis));
            return;
         }
         long remaining = this._deadlineNanos - System.nanoTime();
         if (remaining > 0L) {
            long millis = TimeUnit.NANOSECONDS.toMillis(remaining - 1L) + 1L;
            this.reschedule(millis);
            return;
         }
         if (this._terminal.compareAndSet(false, true) && this._cha.clearSkillDelay(this._token)) {
            NativeSourceSkillDelay.DELAY_CLEAR_COUNT.incrementAndGet();
         }
      }

      private void reschedule(long var1) {
         int var3 = (int)Math.min(2147483647L, Math.max(1L, var1));
         ScheduledFuture var4 = NativeSourceSkillDelay.scheduleDelayTimer(this, var3);
         if (var4 == null) {
            NativeSourceSkillDelay._log.error("L1SkillDelay rebase timer unavailable; keeping gate closed: chaId={} delay={}", this.characterId(), var3);
            return;
         }
         this.installFuture(var4);
      }
   }
}
