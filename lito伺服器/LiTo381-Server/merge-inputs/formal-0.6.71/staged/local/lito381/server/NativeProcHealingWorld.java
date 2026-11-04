// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

final class NativeProcHealingWorld {
   static boolean apply(ClientSession var0, NativeCombatAttacker.Ref var1, NativeArmorProc.Rule var2) {
      if (var1 != null && var1.kind() == 1 && var2.skill() > 0) {
         NativeProcSkillCatalog.Catalog var3 = NativeProcSkillCatalog.catalog();
         if (!var3.healing(var2.skill())) {
            return false;
         } else {
            NativePlayerCommit.Binding var4 = NativePlayerCommit.frameBinding(var0);
            if (var4 != null && var4.matches(var0) && NativePlayerCommit.validFrame(var0) && NativeCombatAttacker.alive(var0, var1)) {
               NativeProcSkillCatalog.Skill var5 = var3.find(var2.skill());
               if (var5 == null) {
                  throw new IllegalStateException("missing NPC healing metadata=" + var2.skill());
               } else {
                  long var6 = System.nanoTime();
                  int var8 = NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(var0, var6));
                  int var9 = NativeProcHealingKernel.calculate(
                     var5.damageDice(),
                     var5.damageValue(),
                     var8,
                     NativePlayerCommit.integer(var0, "playerLawful"),
                     10,
                     var0x -> ThreadLocalRandom.current().nextInt(var0x)
                  );
                  GameHub var10 = (GameHub)CombatAccess.field(var0, "hub");
                  boolean var11;
                  synchronized (var10) {
                     label123: {
                        CombatMonsterCurseLifecycle.Actor var13 = CombatMonsterCurseLifecycle.actor(var10, var1.map(), var1.instance(), var1.id());
                        if (var13 == null) {
                           return true;
                        }

                        boolean var10000;
                        synchronized (var13.population()) {
                           synchronized (var13.timed()) {
                              if (!NativePlayerCommit.validFrame(var0)
                                 || var1.owner() != NativeCombatAttacker.token(var13.timed(), false)
                                 || !var13.valid(var1.epoch())) {
                                 return true;
                              }

                              ClassicWorldMonsterPopulation.Snapshot var16 = var13.population().actor(var1.map(), var1.id());
                              if (var16 == null || var16.currentHp() <= 0) {
                                 var10000 = true;
                              } else {
                                 NativeProcHealingWorld.Target var17 = new NativeProcHealingWorld.Target(
                                    var0, var4, var1, var13, var16.template(), var6, var2.skill()
                                 );
                                 NativeProcHealingKernel.apply(
                                    var17, () -> var9, (int)CombatRules.number("NATURES_BLESSING_HEAL", 0.0), () -> var17.graphics = true
                                 );
                                 var11 = var17.graphics;
                                 break label123;
                              }
                           }
                        }

                        return var10000;
                     }
                  }

                  if (var11) {
                     graphics(var0, var4, var1, var2.skill(), var5.castGraphics());
                  }

                  return true;
               }
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static void graphics(ClientSession var0, NativePlayerCommit.Binding var1, NativeCombatAttacker.Ref var2, int var3, int var4) {
      if (var4 > 0 && NativePlayerCommit.validFrame(var0) && var1.matches(var0) && NativeCombatAttacker.alive(var0, var2) && NativeProcPlayerState.show(var0)) {
         GameHub var5 = (GameHub)CombatAccess.field(var0, "hub");
         ArrayList<ClientSession> var6 = new ArrayList<>();
         var6.add(var0);
         if (!((BattleInvisibilityStatus)CombatAccess.field(var0, "invisibilityStatus")).active()) {
            for (ClientSession var8 : var5.classicWorldMembers(var1.map(), var1.instance())) {
               if (var8 != var0
                  && Math.max(
                        Math.abs((long)NativePlayerCommit.integer(var8, "positionX") - NativePlayerCommit.integer(var0, "positionX")),
                        Math.abs((long)NativePlayerCommit.integer(var8, "positionY") - NativePlayerCommit.integer(var0, "positionY"))
                     )
                     <= 10L) {
                  var6.add(var8);
               }
            }
         }

         byte[] var11 = Protocol.effectObject(var2.id(), var4);

         for (ClientSession var9 : var6) {
            NativePlayerCommit.Binding var10 = NativePlayerCommit.Binding.capture(var9);
            if (!NativePlayerCommit.validFrame(var0) || !var1.matches(var0) || !NativeCombatAttacker.alive(var0, var2)) {
               break;
            }

            if (var10.matches(var9)) {
               NativePlayerCommit.safeSend(var9, var11, "S_EFFECT equipment-NPC-healing skill=" + var3 + " target=" + var2.id());
            }
         }
      }
   }

   static boolean handles(CombatMonsterCurseLifecycle.Context var0) {
      return var0 != null && var0.event != null && var0.event.physical() instanceof NativeProcHealingWorld.Plan;
   }

   static ClientSession owner(ClientSession var0, CombatMonsterCurseLifecycle.Context var1) {
      NativeProcHealingWorld.Plan var2 = (NativeProcHealingWorld.Plan)var1.event.physical();
      return var0 != null && var2.owner().matches(var0) ? var0 : null;
   }

   static boolean valid(boolean var0, CombatMonsterCurseLifecycle.Context var1) {
      if (var0 && handles(var1)) {
         NativeProcHealingWorld.Plan var2 = (NativeProcHealingWorld.Plan)var1.event.physical();
         NativeCombatAttacker.Ref var3 = var2.target();
         CombatMonsterCurseLifecycle.Actor var4 = var1.actor;
         return var3.kind() == 1 && var3.id() == var4.id() && var3.map() == var4.map() && var3.instance() == var4.instance()
            ? var1.dead || var1.epoch == var3.epoch() && NativeCombatAttacker.token(var4.timed(), false) == var3.owner()
            : false;
      } else {
         return var0;
      }
   }

   static boolean allow(CombatMonsterCurseLifecycle.Actor var0, CombatNpcDamageQueue.Event var1) {
      if (var1.physical() instanceof NativeProcHealingWorld.Plan var2) {
         if (var2.guardian() && var1.damage() <= 0) {
            return false;
         } else if (var2.target().owner() == NativeCombatAttacker.token(var0.timed(), false)
            && var2.target().epoch() == CombatPoisonLifecycle.monsterEpoch(var0.timed(), false)) {
            for (ClientSession var4 : var0.hub().connectedSessionsSnapshot()) {
               if (var2.owner().matchesFast(var4)) {
                  Object var5 = CombatAccess.field(CombatPoisonAtomic.status(var4), "sourcePoisonLifecycle");
                  if (var5 != null && CombatAccess.field(var5, "sourcePlayerOwner") == var2.owner().owner()) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   static void publish(ClientSession var0, ClassicWorldMonsterPopulation.PoisonUpdate var1, CombatPoisonLifecycle.Scope var2, NativeProcHealingWorld.Plan var3) {
      if (var0 != null) {
         CombatAccess.call(
            var0,
            "sourceWorldDollCommitted",
            "(Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;Ljava/lang/String;JLlocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V",
            var1.hits().get(0),
            "equipment-proc-healing skill=" + var3.skill(),
            var3.target().id(),
            var1,
            var2
         );
      }
   }

   record Plan(NativeCombatAttacker.Ref target, NativePlayerCommit.Binding owner, int skill, boolean guardian) implements CombatWorldCommit.Plan {
   }

   private static final class Target implements NativeProcHealingKernel.Target {
      final ClientSession pc;
      final NativePlayerCommit.Binding owner;
      final NativeCombatAttacker.Ref ref;
      final CombatMonsterCurseLifecycle.Actor actor;
      final ClassicWorldMonsterCatalog.MonsterTemplate template;
      final long now;
      final int skill;
      boolean graphics;

      Target(
         ClientSession var1,
         NativePlayerCommit.Binding var2,
         NativeCombatAttacker.Ref var3,
         CombatMonsterCurseLifecycle.Actor var4,
         ClassicWorldMonsterCatalog.MonsterTemplate var5,
         long var6,
         int var8
      ) {
         this.pc = var1;
         this.owner = var2;
         this.ref = var3;
         this.actor = var4;
         this.template = var5;
         this.now = var6;
         this.skill = var8;
      }

      @Override
      public boolean dead() {
         return NativePlayerCommit.integer(this.actor.actor(), "currentHp") <= 0 || !(Boolean)CombatAccess.field(this.actor.actor(), "alive");
      }

      @Override
      public NativeProcHealingKernel.Kind kind() {
         return this.template.implementation().equals("L1Monster") ? NativeProcHealingKernel.Kind.MONSTER : NativeProcHealingKernel.Kind.NPC;
      }

      @Override
      public int undead() {
         return this.template.undead();
      }

      @Override
      public boolean hasEffect(int var1) {
         BattleTimedStatus var2 = this.actor.timed();

         return switch (var1) {
            case 164, 170 -> NativeProcNpcState.active(var2, var1, this.now);
            case 173 -> this.actor.population().polluteWaterActive(this.ref.map(), this.ref.id(), this.now)
               || var2.polluteWaterActive(this.now)
               || NativeProcNpcState.active(var2, var1, this.now);
            case 4012 -> var2.waterDragonDirtyCurrentActive(this.now) || NativeProcNpcState.active(var2, var1, this.now);
            default -> throw new AssertionError("unexpected NPC Proc modifier=" + var1);
         };
      }

      @Override
      public void removeEffect(int var1) {
         throw new AssertionError("NPC healing must not consume PC-only Water Life");
      }

      @Override
      public void commit(int var1) {
         CombatMonsterCurseLifecycle.enqueue(
            this.actor,
            new CombatMonsterCurseLifecycle.Recipient(this.owner.object(), this.owner.source(), this.owner.account(), this.owner.selected()),
            this.ref.epoch(),
            var1,
            new NativeProcHealingWorld.Plan(this.ref, this.owner, this.skill, this.template.implementation().equals("L1Guardian"))
         );
      }
   }
}
