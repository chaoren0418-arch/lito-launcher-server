// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class NativeProcNpcState {
   private final Object life;
   private final Map<Integer, Long> deadlines = new HashMap<>();

   private NativeProcNpcState(Object var1) {
      this.life = var1;
   }

   private static NativeProcNpcState owned(BattleTimedStatus var0) {
      if (!Thread.holdsLock(var0)) {
         throw new IllegalStateException("NPC Proc state requires status monitor");
      } else {
         Object var1 = NativeCombatAttacker.token(var0, true);
         NativeProcNpcState var2 = (NativeProcNpcState)CombatAccess.field(var0, "sourceProcNpcState");
         if (var2 == null || var2.life != var1) {
            var2 = new NativeProcNpcState(var1);
            CombatAccess.set(var0, "sourceProcNpcState", var2);
         }

         return var2;
      }
   }

   static boolean active(BattleTimedStatus var0, int var1, long var2) {
      synchronized (var0) {
         NativeProcNpcState var5 = owned(var0);
         var5.deadlines.values().removeIf(var2x -> var2 >= var2x);
         return var5.deadlines.containsKey(var1);
      }
   }

   static void activate(BattleTimedStatus var0, int var1, long var2, int var4) {
      if (!Set.of(164, 170, 173, 4012).contains(var1) || var2 < 0L || var4 <= 0) {
         throw new IllegalArgumentException("unsupported NPC Proc modifier");
      }
      long var5 = Math.addExact(var2, TimeUnit.SECONDS.toNanos(var4));
      synchronized (var0) {
         owned(var0).deadlines.put(var1, var5);
      }
   }

   static void clear(BattleTimedStatus var0) {
      synchronized (var0) {
         CombatAccess.set(var0, "sourceProcNpcState", null);
      }
   }
}
