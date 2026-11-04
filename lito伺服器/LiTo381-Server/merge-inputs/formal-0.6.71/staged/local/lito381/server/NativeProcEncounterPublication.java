// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class NativeProcEncounterPublication {
   static void publish(NativeProcEncounterPublication.Context var0) {
      if (var0.valid()) {
         ClientSession var1 = var0.driver;
         String var2 = var0.plan.target().kind() == 3 ? "sourceProcQuestCommitted" : "sourceProcTimeCommitted";
         String var3 = var0.plan.target().kind() == 3 ? "BattleQuestEncounter$Hit" : "GameHub$TimeAttackActorHit";

         try {
            CombatAccess.call(
               var1,
               var2,
               "(Llocal/lito381/server/" + var3 + ";ILjava/lang/String;ILlocal/lito381/server/NativeProcEncounterPublication$Context;)I",
               var0.outcome,
               var0.palette,
               "equipment-proc-healing skill=" + var0.plan.skill(),
               var0.damage,
               var0
            );
         } catch (RuntimeException var5) {
            if (!hasStale(var5)) {
               throw var5;
            }
         }
      }
   }

   private static boolean hasStale(Throwable var0) {
      for (Throwable var1 = var0; var1 != null; var1 = var1.getCause()) {
         if (var1 instanceof NativeProcEncounterPublication.Stale) {
            return true;
         }
      }

      return false;
   }

   static long room(ClientSession var0, NativeProcEncounterPublication.Context var1) {
      return var1.plan.target().room();
   }

   static String name(ClientSession var0, NativeProcEncounterPublication.Context var1) {
      return var1.plan.caster().selected();
   }

   private static List<ClientSession> capture(List<ClientSession> var0, NativeProcEncounterPublication.Context var1) {
      if (!var1.valid()) {
         throw new NativeProcEncounterPublication.Stale();
      } else {
         ArrayList var2 = new ArrayList();
         synchronized (var1.hub) {
            for (ClientSession var5 : var0) {
               NativePlayerCommit.Binding var6 = NativePlayerCommit.Binding.capture(var5);
               if (var6.room() == var1.plan.target().room() && var6.phase().equals("BATTLE")) {
                  NativeProcEncounterPublication.Viewer var7 = var1.viewers.get(var5);
                  if (var7 == null || var7.binding().equals(var6)) {
                     int var8 = var1.hub.forceOf(var6.room(), var5);
                     var1.viewers.putIfAbsent(var5, new NativeProcEncounterPublication.Viewer(var6, var8));
                     var2.add(var5);
                  }
               }
            }

            return var2;
         }
      }
   }

   static List<ClientSession> members(GameHub var0, long var1, NativeProcEncounterPublication.Context var3) {
      return capture(var0.roomMembers(var3.plan.target().room()), var3);
   }

   static List<ClientSession> recipients(GameHub var0, long var1, int var3, NativeProcEncounterPublication.Context var4) {
      return capture(var0.battleRecipients(var4.plan.target().room(), var3), var4);
   }

   private static boolean current(ClientSession var0, NativeProcEncounterPublication.Context var1) {
      NativeProcEncounterPublication.Viewer var2 = var1.viewers.get(var0);
      if (var2 != null && var2.binding().matches(var0) && var1.valid()) {
         synchronized (var1.hub) {
            return var1.hub.roomMembers(var1.plan.target().room()).contains(var0) && var1.hub.forceOf(var1.plan.target().room(), var0) == var2.force();
         }
      } else {
         return false;
      }
   }

   static void send(ClientSession var0, byte[] var1, String var2, NativeProcEncounterPublication.Context var3) {
      if (current(var0, var3)) {
         NativePlayerCommit.safeSend(var0, var1, var2);
      }
   }

   static List<BattleGuardianTower.Snapshot> visible(GameHub var0, long var1, ClientSession var3, NativeProcEncounterPublication.Context var4) {
      if (!var4.valid()) {
         throw new NativeProcEncounterPublication.Stale();
      } else {
         synchronized (var0) {
            Object var6 = CombatAccess.field(var4.room, "guardianTowers");
            return ((List<BattleGuardianTower.Snapshot>)CombatAccess.call(var6, "snapshots", "()Ljava/util/List;")).stream().filter(var1x -> var1x.force() == var4.force).toList();
         }
      }
   }

   static List<BattleQuestBoundary.Attribute> opened(GameHub var0, long var1, int var3, int var4, NativeProcEncounterPublication.Context var5) {
      if (!var5.valid()) {
         throw new NativeProcEncounterPublication.Stale();
      } else {
         return var0.openedQuestBoundary(var5.plan.target().room(), var3, var4);
      }
   }

   static void arm(GameHub var0, long var1, NativeProcEncounterPublication.Context var3) {
      if (var3.valid()) {
         var0.armPveAi(var3.plan.target().room());
      }
   }

   static boolean cancel(GameHub var0, long var1, long var3, NativeProcEncounterPublication.Context var5) {
      return var5.valid() && var0.cancelArmorBreak(var5.plan.target().room(), var3);
   }

   static void end(ClientSession var0, BattleRules.Resolution var1, NativeProcEncounterPublication.Context var2) {
      if (current(var0, var2)) {
         try {
            var0.serverEndBattle(var1);
         } catch (Exception var4) {
            NativePlayerCommit.secondaryFailure("encounter end publication", new IllegalStateException(var4));
         }
      }
   }

   static void finish(GameHub var0, long var1, NativeProcEncounterPublication.Context var3) {
      if (var3.valid()) {
         var0.endBattle(var3.plan.target().room());
      }
   }

   static void lawful(ClientSession var0, int var1, String var2, NativeProcEncounterPublication.Context var3) {
      int var4 = var3.plan.npc().lawful();
      if (var4 != 0 && var3.valid()) {
         ClientSession var5 = NativeProcHealingEncounter.find(var3.hub, var3.plan.caster());
         if (var5 != null) {
            GameStateStore.LawfulMutation var6;
            synchronized (var3.hub) {
               label100: {
                  synchronized (CombatPoisonAtomic.status(var5)) {
                     synchronized (var3.container) {
                        synchronized (var3.timed) {
                           if (var3.plan.caster().matches(var5)
                              && var3.valid()
                              && ((Set)CombatAccess.field(var3.room, "members")).contains(var5)
                              && ((Set)CombatAccess.field(var3.room, "battleRoster")).contains(var5)) {
                              CharacterSummary var11 = (CharacterSummary)CombatAccess.call(
                                 var5, "selectedCharacterSummary", "()Llocal/lito381/server/CharacterSummary;"
                              );
                              int var12 = var11 == null ? 0 : (int)Math.max(-32768L, Math.min(32767L, var11.lawful()));
                              GameStateStore var13 = (GameStateStore)CombatAccess.field(var5, "state");
                              String var14 = (String)CombatAccess.call(var5, "stateCharacter", "()Ljava/lang/String;");

                              try {
                                 var6 = var13.addLawful(var14, var12, -var4);
                              } catch (Exception var20) {
                                 throw new IllegalStateException("encounter lawful mutation failed", var20);
                              }

                              CombatAccess.set(var5, "playerLawful", var6.after());
                              break label100;
                           }
                        }
                     }
                  }

                  return;
               }
            }

            if (var6.changed() && var3.plan.caster().matches(var5)) {
               byte[] var25 = Protocol.changeAlignment(var3.plan.caster().object(), var6.after());

               for (ClientSession var26 : members(var3.hub, var3.plan.target().room(), var3)) {
                  if (!var3.plan.caster().matches(var5)) {
                     break;
                  }

                  send(
                     var26,
                     var25,
                     "S_CHANGE_ALIGNMENT equipment-encounter-healing npc=" + var1 + " lawful=" + var6.before() + "->" + var6.after() + " source=" + var2,
                     var3
                  );
               }
            }
         }
      }
   }

   static final class Context {
      final GameHub hub;
      final ClientSession driver;
      final NativeProcHealingEncounter.Plan plan;
      final Object room;
      final Object container;
      final Object actor;
      final BattleTimedStatus timed;
      final Object afterToken;
      final Object outcome;
      final int palette;
      final int damage;
      final int force;
      final boolean dead;
      final IdentityHashMap<ClientSession, NativeProcEncounterPublication.Viewer> viewers = new IdentityHashMap<>();

      Context(NativeProcHealingEncounter.Live var1, NativeProcHealingEncounter.Plan var2, Object var3, int var4, int var5, Object var6) {
         this.hub = var1.hub();
         this.driver = var1.caster();
         this.room = var1.room();
         this.container = var1.container();
         this.actor = var1.actor();
         this.timed = var1.timed();
         this.plan = var2;
         this.outcome = var3;
         this.palette = var4;
         this.damage = var5;
         this.afterToken = var6;
         if (var3 instanceof BattleQuestEncounter.Hit var7) {
            this.force = var7.damage().after().force();
            this.dead = var7.damage().diedNow();
         } else {
            this.force = -1;
            this.dead = ((GameHub.TimeAttackActorHit)var3).hit().damage().diedNow();
         }
      }

      boolean valid() {
         NativeCombatAttacker.Ref var1 = this.plan.target();
         synchronized (this.hub) {
            if (NativeProcHealingEncounter.room(this.hub, var1.room()) == this.room
               && NativeProcHealingEncounter.mode(this.room, var1)
               && NativeProcHealingEncounter.container(this.room, var1) == this.container) {
               boolean var12;
               synchronized (this.container) {
                  synchronized (this.timed) {
                     if (NativeProcHealingEncounter.generation(this.container) == this.plan.generation()
                        && NativeCombatAttacker.token(this.timed, false) == this.afterToken) {
                        Object var5 = NativeProcHealingEncounter.actor(this.container, var1);
                        var12 = !this.dead
                           ? var5 == this.actor && NativePlayerCommit.integer(this.actor, "hp") > 0
                           : NativePlayerCommit.integer(this.actor, "hp") == 0 && (var5 == null || var5 == this.actor);
                     } else {
                        return false;
                     }
                  }
               }

               return var12;
            } else {
               return false;
            }
         }
      }
   }

   private static final class Stale extends RuntimeException {
   }

   record Viewer(NativePlayerCommit.Binding binding, int force) {
   }
}
