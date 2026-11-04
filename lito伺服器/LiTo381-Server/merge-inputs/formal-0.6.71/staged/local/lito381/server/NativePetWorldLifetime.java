// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.io.IOException;
import java.util.Map;

final class NativePetWorldLifetime {
   private static final String P = "local/lito381/server/";
   private static final ThreadLocal<NativePetWorldLifetime.Target> ingress = new ThreadLocal<>();
   private static final ThreadLocal<NativePetWorldLifetime.Target> current = new ThreadLocal<>();

   private static ClassicWorldMonsterPopulation population(GameHub var0, int var1, int var2) {
      return (ClassicWorldMonsterPopulation)CombatAccess.call(
         var0, "classicWorldMonsterPopulation", "(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;", var1, var2
      );
   }

   private static Object actor(ClassicWorldMonsterPopulation var0, long var1) {
      return var0 == null ? null : ((Map)CombatAccess.field(var0, "actors")).get(var1);
   }

   private static Object epoch(Object var0) {
      return var0 == null ? null : NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(var0, "damagePoisonStatus"), false);
   }

   private static Object bindEpoch(Object var0) {
      return var0 == null ? null : NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(var0, "damagePoisonStatus"), true);
   }

   private static NativePetWorldLifetime.Target take(ClientSession var0, byte[] var1) {
      GameHub var2 = (GameHub)CombatAccess.field(var0, "hub");
      int var3 = (Integer)CombatAccess.field(var0, "classicWorldMapId");
      int var4 = (Integer)CombatAccess.field(var0, "classicWorldInstanceId");
      long var5 = Protocol.spellSelector(var1).skillId() == 36
         ? Protocol.attackSpellRequest(var1).targetObjectId()
         : Protocol.objectSpellRequest(var1).targetObjectId();
      synchronized (var2) {
         ClassicWorldMonsterPopulation var8 = population(var2, var3, var4);
         if (var8 == null) {
            return new NativePetWorldLifetime.Target(var0, var2, var3, var4, var5, null, null);
         } else {
            NativePetWorldLifetime.Target var10000;
            synchronized (var8) {
               var10000 = new NativePetWorldLifetime.Target(var0, var2, var3, var4, var5, var8, actor(var8, var5));
            }

            return var10000;
         }
      }
   }

   static void dispatch(ClientSession var0, byte[] var1, NativeSelfCastAction.Task var2) throws IOException {
      int var3 = Protocol.spellSelector(var1).skillId();
      if ((var3 == 36 || var3 == 41) && String.valueOf(CombatAccess.field(var0, "phase")).equals("WORLD") && !var0.selectedCharacterName().isBlank()) {
         NativePetWorldLifetime.Target var4 = ingress.get();
         ingress.set(take(var0, var1));

         try {
            var2.run();
         } finally {
            if (var4 == null) {
               ingress.remove();
            } else {
               ingress.set(var4);
            }
         }
      } else {
         var2.run();
      }
   }

   static void run(ClientSession var0, byte[] var1, NativeSelfCastAction.Task var2) throws IOException {
      if (!String.valueOf(CombatAccess.field(var0, "phase")).equals("WORLD")) {
         NativeLightAction.commitPair(var0, var0, var2);
      } else {
         NativePetWorldLifetime.Target var3 = ingress.get();
         NativePetWorldLifetime.Target var4 = var3 != null && var3.pc == var0 ? var3 : take(var0, var1);
         ingress.remove();

         try {
            NativeLightAction.commitPair(
               var0, var0, () -> NativeWorldMaterialPolicy.run(var0, var4.actor != null, Protocol.spellSelector(var1).skillId(), () -> {
                  synchronized (var4.hub) {
                     if (var4.population == null) {
                        if (var4.valid()) {
                           var2.run();
                        }
                     } else {
                        synchronized (var4.population) {
                           if (!var4.valid()) {
                              return;
                           }

                           NativePetWorldLifetime.Target var4x = current.get();
                           current.set(var4);

                           try {
                              var2.run();
                           } finally {
                              var4.publicationEpoch = bindEpoch(var4.actor);
                              var4.publicationPresent = var4.actor != null && (Boolean)CombatAccess.field(var4.actor, "present");
                              if (var4x == null) {
                                 current.remove();
                              } else {
                                 current.set(var4x);
                              }
                           }
                        }
                     }
                  }
               })
            );
         } finally {
            if (var3 == null) {
               ingress.remove();
            } else {
               ingress.set(var3);
            }
         }
      }
   }

   static boolean captureOutput(ClientSession var0, byte[] var1, String var2) {
      NativePetWorldLifetime.Target var3 = current.get();
      if (var3 == null) {
         current.remove();
         return false;
      } else {
         boolean var4 = var2.startsWith("S_EFFECT taming target=") || var2.startsWith("S_EFFECT create-zombie corpse=");
         boolean var5 = var2.startsWith("S_REMOVE_OBJECT ") && (var2.endsWith(" reason=tamed") || var2.endsWith(" reason=create-zombie"));
         if (!var4 && !var5) {
            return false;
         } else {
            byte[] var6 = (byte[])var1.clone();
            NativeLightAction.deferScoped(() -> {
               if (var3.publication()) {
                  var0.nativeSelfOriginalSend(var6, var2);
               }
            });
            return true;
         }
      }
   }

   private NativePetWorldLifetime() {
   }

   private static final class Target {
      final ClientSession pc;
      final GameHub hub;
      final NativePlayerCommit.Binding binding;
      final int map;
      final int instance;
      final long id;
      final Object epoch;
      final ClassicWorldMonsterPopulation population;
      final Object actor;
      Object publicationEpoch;
      boolean publicationPresent;

      Target(ClientSession var1, GameHub var2, int var3, int var4, long var5, ClassicWorldMonsterPopulation var7, Object var8) {
         this.pc = var1;
         this.hub = var2;
         this.map = var3;
         this.instance = var4;
         this.id = var5;
         this.population = var7;
         this.actor = var8;
         this.binding = NativePlayerCommit.Binding.capture(var1);
         this.epoch = NativePetWorldLifetime.bindEpoch(var8);
         this.publicationEpoch = this.epoch;
      }

      boolean scene() {
         return this.binding.matches(this.pc)
            && !(Boolean)CombatAccess.field(this.pc, "sourceItemDisconnected")
            && String.valueOf(CombatAccess.field(this.pc, "phase")).equals("WORLD")
            && (Integer)CombatAccess.field(this.pc, "classicWorldMapId") == this.map
            && (Integer)CombatAccess.field(this.pc, "classicWorldInstanceId") == this.instance;
      }

      boolean valid() {
         return this.scene()
            && NativePetWorldLifetime.population(this.hub, this.map, this.instance) == this.population
            && NativePetWorldLifetime.actor(this.population, this.id) == this.actor
            && NativePetWorldLifetime.epoch(this.actor) == this.epoch;
      }

      boolean publication() {
         synchronized (this.hub) {
            if (!this.scene() || NativePetWorldLifetime.population(this.hub, this.map, this.instance) != this.population) {
               return false;
            } else if (this.population == null) {
               return true;
            } else {
               boolean var10000;
               synchronized (this.population) {
                  Object var3 = NativePetWorldLifetime.actor(this.population, this.id);
                  var10000 = (var3 == this.actor || var3 == null)
                     && NativePetWorldLifetime.epoch(this.actor) == this.publicationEpoch
                     && (var3 == null || (Boolean)CombatAccess.field(var3, "present") == this.publicationPresent);
               }

               return var10000;
            }
         }
      }
   }
}
