// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

final class NativePlayerVitals {
   private static final ThreadLocal<Integer> minimum = new ThreadLocal<>();

   static int minimum() {
      Integer var0 = minimum.get();
      return var0 == null ? 0 : var0;
   }

   static NativePlayerVitals.Result calculate(int var0, int var1, int var2, int var3, boolean var4) {
      long var5 = Math.min((long)var0 - (long)var2, (long)var1);
      int var7 = Math.max(0, Math.min(var0, Math.min(Math.max(0, var3), var1)));
      if (var5 <= (long)var7) {
         if (var7 > 0) {
            return new NativePlayerVitals.Result(var7, false);
         }
         if (var4) {
            return new NativePlayerVitals.Result(var1, false);
         }
         return new NativePlayerVitals.Result(0, true);
      }
      return new NativePlayerVitals.Result((int)Math.min(2147483647L, var5), false);
   }

   static boolean gm(ClientSession var0) {
      AccountRepository var1 = (AccountRepository)CombatAccess.call(var0, "selectedSourceAccounts", "()Llocal/lito381/server/AccountRepository;");
      return SourceAdministration.accessLevel(var1, var0.accountName(), var0) >= 200;
   }

   static void nonlethal(ClientSession var0, int var1, ClientSession var2, boolean var3) {
      Integer var4 = minimum.get();
      minimum.set(1);

      try {
         if (var3) {
            CombatAccess.call(var0, "takeClassicWorldPlayerDamage", "(ILlocal/lito381/server/ClientSession;ZZ)V", var1, var2, false, false);
         } else {
            CombatAccess.call(var0, "takePlayerDamage", "(ILlocal/lito381/server/ClientSession;ZZJ)V", var1, var2, false, false, 0L);
         }
      } finally {
         if (var4 == null) {
            minimum.remove();
         } else {
            minimum.set(var4);
         }
      }
   }

   static boolean idle() {
      return minimum.get() == null;
   }

   record Result(int after, boolean fatal) {
   }
}
