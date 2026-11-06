// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class NativeSourceSkillCastAdmission {
   private NativeSourceSkillCastAdmission() {
   }

   static NativeSourceSkillCastAdmission.Result admit(
      BooleanSupplier var0, Supplier<? extends NativeSourceSkillCastAdmission.Reservation> var1, BooleanSupplier var2
   ) {
      Objects.requireNonNull(var0, "preflight");
      Objects.requireNonNull(var1, "reservationFactory");
      Objects.requireNonNull(var2, "materialCommit");
      if (!var0.getAsBoolean()) {
         return NativeSourceSkillCastAdmission.Result.PREFLIGHT_REJECTED;
      } else {
         NativeSourceSkillCastAdmission.Reservation var3 = (NativeSourceSkillCastAdmission.Reservation)var1.get();
         if (var3 == null) {
            return NativeSourceSkillCastAdmission.Result.RESERVATION_REJECTED;
         } else {
            try {
               if (!var2.getAsBoolean()) {
                  var3.cancel();
                  return NativeSourceSkillCastAdmission.Result.MATERIAL_REJECTED;
               } else {
                  var3.commit();
                  return NativeSourceSkillCastAdmission.Result.ACCEPTED;
               }
            } catch (RuntimeException | Error var5) {
               var3.cancel();
               throw var5;
            }
         }
      }
   }

   interface Reservation {
      void commit();

      void cancel();
   }

   static enum Result {
      PREFLIGHT_REJECTED,
      RESERVATION_REJECTED,
      MATERIAL_REJECTED,
      ACCEPTED;
   }
}
