// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class NativeProcPlayerState {
   private NativeProcPlayerState.Identity identity;
   private final Map<Integer, Long> modifiers = new HashMap<>();
   private boolean show = true;

   private static NativeProcPlayerState owned(ClientSession var0) {
      BattleTimedStatus var1 = CombatPoisonAtomic.status(var0);
      if (!Thread.holdsLock(var1)) {
         throw new IllegalStateException("Proc state requires player status monitor");
      } else {
         NativeProcPlayerState var2 = (NativeProcPlayerState)CombatAccess.field(var1, "sourceProcPlayerState");
         if (var2 == null) {
            var2 = new NativeProcPlayerState();
            CombatAccess.set(var1, "sourceProcPlayerState", var2);
         }

         NativeProcPlayerState.Identity var3 = new NativeProcPlayerState.Identity(
            var0.sourceCharacterIdentity(), var0.sourceAccountIdentity(), var0.selectedCharacterName()
         );
         if (!var3.equals(var2.identity)) {
            var2.modifiers.clear();
            var2.show = true;
            var2.identity = var3;
         }

         return var2;
      }
   }

   static boolean active(ClientSession var0, int var1, long var2) {
      BattleTimedStatus var4 = CombatPoisonAtomic.status(var0);
      synchronized (var4) {
         NativeProcPlayerState var6 = owned(var0);
         var6.modifiers.values().removeIf(var2x -> var2 >= var2x);
         return var6.modifiers.containsKey(var1);
      }
   }

   static boolean remove(ClientSession var0, int var1) {
      synchronized (CombatPoisonAtomic.status(var0)) {
         return owned(var0).modifiers.remove(var1) != null;
      }
   }

   static boolean activate(ClientSession var0, NativePlayerCommit.Binding var1, int var2, long var3, int var5) {
      if (Set.of(164, 170, 173, 4012).contains(var2) && var3 >= 0L && var5 > 0) {
         long var6 = Math.addExact(var3, TimeUnit.SECONDS.toNanos(var5));
         synchronized (CombatPoisonAtomic.status(var0)) {
            if (var1 != null && var1.matches(var0)) {
               owned(var0).modifiers.put(var2, var6);
               return true;
            } else {
               return false;
            }
         }
      } else {
         throw new IllegalArgumentException("unsupported Proc healing modifier");
      }
   }

   static void show(ClientSession var0, boolean var1) {
      synchronized (CombatPoisonAtomic.status(var0)) {
         owned(var0).show = var1;
      }
   }

   static boolean show(ClientSession var0) {
      synchronized (CombatPoisonAtomic.status(var0)) {
         return owned(var0).show;
      }
   }

   static void clear(BattleTimedStatus var0) {
      synchronized (var0) {
         NativeProcPlayerState var2 = (NativeProcPlayerState)CombatAccess.field(var0, "sourceProcPlayerState");
         if (var2 != null) {
            var2.modifiers.clear();
         }
      }
   }

   static void selected(ClientSession var0) {
      BattleTimedStatus var1 = CombatPoisonAtomic.status(var0);
      if (var1 != null) {
         synchronized (var1) {
            CombatAccess.set(var1, "sourceProcPlayerState", null);
         }
      }
   }

   static void selectedField(ClientSession var0, String var1) {
      if (var1.equals("selectedCharacter")) {
         selected(var0);
      }
   }

   private record Identity(String source, String account, String character) {
   }
}
