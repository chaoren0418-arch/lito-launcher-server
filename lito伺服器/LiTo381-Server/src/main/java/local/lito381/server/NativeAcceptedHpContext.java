// DECOMPILED from base-0.6.71.jar, TODO review (Vineflower 1.11.2)
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

final class NativeAcceptedHpContext implements AutoCloseable {
   static final List<String> FIELDS = List.of(
      "CombatMonsterCurseLifecycle.poisonDamageTarget",
      "CombatWorldCommit.effects",
      "CombatWorldFreeze.visualSeconds",
      "DragonSkillRequests.REQUEST",
      "ForcedCombat.CURRENT",
      "IllusionSkillRequests.REQUEST",
      "NativeAcceptedState.publication",
      "NativeCombatAttacker.ingress",
      "NativeCombatAttacker.reflecting",
      "NativeCombatOrigin.clock",
      "NativeCombatOrigin.installing",
      "NativePlayerCommit.calculation",
      "NativePlayerCommit.worldCalculation",
      "NativePlayerLifetime.minimumDeadline",
      "NativePlayerVitals.minimum",
      "NativeProcHealingEncounter.direct"
   );
   private static final ThreadLocal<Object>[] locals = resolve();
   private NativeCastTimeline.Arrival packetBefore;
   private final Object[] previous = new Object[locals.length];
   private boolean closed;

   private static ThreadLocal<Object>[] resolve() {
      ThreadLocal[] var0 = new ThreadLocal[FIELDS.size()];

      try {
         for (int var1 = 0; var1 < FIELDS.size(); var1++) {
            String var2 = FIELDS.get(var1);
            int var3 = var2.indexOf(46);
            Class var4 = Class.forName("local.lito381.server." + var2.substring(0, var3), true, NativeAcceptedHpContext.class.getClassLoader());
            Field var5 = var4.getDeclaredField(var2.substring(var3 + 1));
            if (var5.getType() != ThreadLocal.class || !Modifier.isStatic(var5.getModifiers())) {
               throw new IllegalStateException("invalid native combat context field " + var2);
            }

            var5.setAccessible(true);
            var0[var1] = (ThreadLocal)var5.get(null);
            if (var0[var1] == null) {
               throw new IllegalStateException("uninitialized native combat context " + var2);
            }
         }

      } catch (ReflectiveOperationException var6) {
         throw new ExceptionInInitializerError(var6);
      }
      return var0;
   }

   private NativeAcceptedHpContext() {
      for (int var1 = 0; var1 < locals.length; var1++) {
         this.previous[var1] = locals[var1].get();
      }

      for (ThreadLocal var4 : locals) {
         var4.remove();
      }

      this.packetBefore = NativeCastTimeline.detach();
   }

   static NativeAcceptedHpContext detach() {
      return new NativeAcceptedHpContext();
   }

   @Override
   public void close() {
      if (this.closed) {
         throw new IllegalStateException("native HP context restored twice");
      } else {
         this.closed = true;
         NativeCastTimeline.restore(this.packetBefore);

         for (int var1 = locals.length - 1; var1 >= 0; var1--) {
            if (this.previous[var1] == null) {
               locals[var1].remove();
            } else {
               locals[var1].set(this.previous[var1]);
            }
         }
      }
   }
}
