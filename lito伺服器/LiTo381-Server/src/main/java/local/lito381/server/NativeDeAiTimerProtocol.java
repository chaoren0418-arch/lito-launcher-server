// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;

final class NativeDeAiTimerProtocol {
   private NativeDeAiTimerProtocol() {
   }

   static Object schedule(NativeDeAiTimeline.State var0, NativeDeAiTimerProtocol.Port var1, long var2, int var4, boolean var5) {
      if (var1.shutdown()) {
         NativeDeAiTimeline.stop(var0, var1, var2);
         return null;
      } else {
         long var6;
         synchronized (var0.lock) {
            if (!NativeDeAiTimeline.owns(var0, var2) || var0.chain != var2 || var0.stop || !var0.pending || var0.pendingBackground != var5) {
               return null;
            }

            if (var0.activeTimer != 0L) {
               return var0.future;
            }

            var6 = NativeDeAiTimeline.nextTimer(var0);
            var0.activeTimer = var6;
         }

         try {
            Object var16 = var1.install(var2, var6, var5, Math.max(0, var4));
            if (var16 != null) {
               boolean var9 = false;
               synchronized (var0.lock) {
                  if (NativeDeAiTimeline.owns(var0, var2) && var0.chain == var2 && !var0.stop && var0.activeTimer == var6) {
                     var0.future = var16;
                     var9 = true;
                  }
               }

               if (!var9) {
                  var1.cancel(var16);
                  return null;
               }

               return var16;
            }

            var1.failure(NativeDeAiTimerProtocol.Failure.NULL_FUTURE);
         } catch (RejectedExecutionException var13) {
            var1.failure(NativeDeAiTimerProtocol.Failure.REJECTED);
         } catch (RuntimeException var14) {
            var1.failure(NativeDeAiTimerProtocol.Failure.ERROR);
         }

         NativeDeAiTimeline.failTimer(var0, var1, var2, var6);
         return null;
      }
   }

   static enum Failure {
      NULL_FUTURE,
      REJECTED,
      ERROR;
   }

   interface Port extends NativeDeAiTimeline.Port {
      Object install(long var1, long var3, boolean var5, int var6);

      void failure(NativeDeAiTimerProtocol.Failure var1);
   }
}
