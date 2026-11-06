// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

public final class NativeSourceActionIntervalCalculator {
   private NativeSourceActionIntervalCalculator() {
   }

   public static int calculate(int var0, NativeSourceActionIntervalCalculator.ActionType var1, NativeSourceActionIntervalCalculator.SpeedState var2) {
      int var3 = Math.max(1, var0);
      NativeSourceActionIntervalCalculator.ActionType var4 = var1 != null ? var1 : NativeSourceActionIntervalCalculator.ActionType.SPELL;
      if (var2.slowed()) {
         var3 = multiply(var3, 2.0);
      } else if (var2.haste()) {
         var3 = multiply(var3, 0.755);
      }

      if (var4 == NativeSourceActionIntervalCalculator.ActionType.MOVE && var2.fastMovable()) {
         var3 = multiply(var3, 0.665);
      }

      if (var4 == NativeSourceActionIntervalCalculator.ActionType.ATTACK && var2.fastAttackable()) {
         var3 = multiply(var3, 0.775);
      }

      if (var2.brave()) {
         var3 = multiply(var3, 0.755);
      }

      if (var2.braveX()) {
         var3 = multiply(var3, 0.755);
      }

      if (var2.elfBrave()) {
         var3 = multiply(var3, 0.855);
         if (var4 == NativeSourceActionIntervalCalculator.ActionType.ATTACK) {
            var3 = multiply(var3, 0.9);
         }
      }

      if (var2.windShackle()
         && (var4 == NativeSourceActionIntervalCalculator.ActionType.ATTACK || var4 == NativeSourceActionIntervalCalculator.ActionType.SPELL)) {
         var3 = multiply(var3, 1.25);
      }

      return Math.max(1, var3);
   }

   private static int multiply(int var0, double var1) {
      return (int)(var0 * var1);
   }

   public static enum ActionType {
      MOVE,
      ATTACK,
      SPELL;
   }

   public record SpeedState(
      boolean haste, boolean slowed, boolean fastMovable, boolean fastAttackable, boolean brave, boolean braveX, boolean elfBrave, boolean windShackle
   ) {
   }
}
