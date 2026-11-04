// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

final class NativePcActionAuthority {
   static <T> T item(ClientSession var0, NativeDeItemTransaction.Operation<T> var1) throws IOException {
      Object var6;
      try (NativePlayerActionScope.Ticket var2 = NativePlayerActionScope.enter(var0)) {
         BattleTimedStatus var3 = CombatPoisonAtomic.status(var0);
         ReentrantLock var4 = var0.nativePlayerActionLock();
         var4.lock();

         try {
            synchronized (var3) {
               var6 = NativeDeItemTransaction.nativeCall(var0, var1);
            }
         } finally {
            var4.unlock();
            NativePlayerActionScope.unlocked(var0);
         }
      }

      return (T)var6;
   }

   private NativePcActionAuthority() {
   }
}
