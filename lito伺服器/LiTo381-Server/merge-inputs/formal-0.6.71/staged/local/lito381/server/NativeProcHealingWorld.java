// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import local.lito381.server.BattleInvisibilityStatus;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterCatalog;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.CombatRules;
import local.lito381.server.CombatWorldCommit;
import local.lito381.server.GameHub;
import local.lito381.server.NativeArmorProc;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcHealBonus;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeProcHealingPlayer;
import local.lito381.server.NativeProcNpcState;
import local.lito381.server.NativeProcPlayerState;
import local.lito381.server.NativeProcSkillCatalog;
import local.lito381.server.Protocol;

final class NativeProcHealingWorld {
    NativeProcHealingWorld() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean apply(ClientSession clientSession, NativeCombatAttacker.Ref ref, NativeArmorProc.Rule rule) {
        if (ref == null || ref.kind() != 1 || rule.skill() <= 0) {
            return false;
        }
        NativeProcSkillCatalog.Catalog catalog = NativeProcSkillCatalog.catalog();
        if (!catalog.healing(rule.skill())) {
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.frameBinding(clientSession);
        if (!(binding != null && binding.matches(clientSession) && NativePlayerCommit.validFrame(clientSession) && NativeCombatAttacker.alive(clientSession, ref))) {
            return true;
        }
        NativeProcSkillCatalog.Skill skill = catalog.find(rule.skill());
        if (skill == null) {
            throw new IllegalStateException("missing NPC healing metadata=" + rule.skill());
        }
        long l = System.nanoTime();
        int n2 = NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(clientSession, l));
        int n3 = NativeProcHealingKernel.calculate(skill.damageDice(), skill.damageValue(), n2, NativePlayerCommit.integer(clientSession, "playerLawful"), 10, n -> ThreadLocalRandom.current().nextInt(n));
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        boolean bl;
        synchronized (gameHub) {
            CombatMonsterCurseLifecycle.Actor actor = CombatMonsterCurseLifecycle.actor(gameHub, ref.map(), ref.instance(), ref.id());
            if (actor == null) {
                return true;
            }
            synchronized (actor.population()) {
                synchronized (actor.timed()) {
                    if (!NativePlayerCommit.validFrame(clientSession) || ref.owner() != NativeCombatAttacker.token(actor.timed(), false) || !actor.valid(ref.epoch())) {
                        return true;
                    }
                    ClassicWorldMonsterPopulation.Snapshot snapshot = actor.population().actor(ref.map(), ref.id());
                    if (snapshot == null || snapshot.currentHp() <= 0) {
                        return true;
                    }
                    Target target = new Target(clientSession, binding, ref, actor, snapshot.template(), l, rule.skill());
                    NativeProcHealingKernel.apply(target, () -> n3, (int)CombatRules.number("NATURES_BLESSING_HEAL", 0.0), () -> {
                        target.graphics = true;
                    });
                    bl = target.graphics;
                }
            }
        }
        if (bl) {
            NativeProcHealingWorld.graphics(clientSession, binding, ref, rule.skill(), skill.castGraphics());
        }
        return true;
    }

    private static void graphics(ClientSession clientSession, NativePlayerCommit.Binding binding, NativeCombatAttacker.Ref ref, int n, int n2) {
        if (!(n2 > 0 && NativePlayerCommit.validFrame(clientSession) && binding.matches(clientSession) && NativeCombatAttacker.alive(clientSession, ref) && NativeProcPlayerState.show(clientSession))) {
            return;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        arrayList.add(clientSession);
        if (!((BattleInvisibilityStatus)CombatAccess.field(clientSession, "invisibilityStatus")).active()) {
            var iterator = gameHub.classicWorldMembers(binding.map(), binding.instance()).iterator();
            while (iterator.hasNext()) {
                ClientSession clientSession2 = iterator.next();
                if (clientSession2 != clientSession && Math.max(Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionX") - (long)NativePlayerCommit.integer(clientSession, "positionX")), Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionY") - (long)NativePlayerCommit.integer(clientSession, "positionY"))) <= 10L) { arrayList.add(clientSession2); }
            }
        }
        byte[] object = Protocol.effectObject(ref.id(), n2);
        for (ClientSession clientSession3 : arrayList) {
            NativePlayerCommit.Binding binding2 = NativePlayerCommit.Binding.capture(clientSession3);
            if (!NativePlayerCommit.validFrame(clientSession) || !binding.matches(clientSession) || !NativeCombatAttacker.alive(clientSession, ref)) break;
            if (binding2.matches(clientSession3)) NativePlayerCommit.safeSend(clientSession3, (byte[])object, "S_EFFECT equipment-NPC-healing skill=" + n + " target=" + ref.id());
        }
    }

    static boolean handles(CombatMonsterCurseLifecycle.Context context) {
        return context != null && context.event != null && context.event.physical() instanceof Plan;
    }

    static ClientSession owner(ClientSession clientSession, CombatMonsterCurseLifecycle.Context context) {
        Plan plan = (Plan)context.event.physical();
        return clientSession != null && plan.owner().matches(clientSession) ? clientSession : null;
    }

    static boolean valid(boolean bl, CombatMonsterCurseLifecycle.Context context) {
        if (!bl || !NativeProcHealingWorld.handles(context)) {
            return bl;
        }
        Plan plan = (Plan)context.event.physical();
        NativeCombatAttacker.Ref ref = plan.target();
        CombatMonsterCurseLifecycle.Actor actor = context.actor;
        if (ref.kind() != 1 || ref.id() != actor.id() || ref.map() != actor.map() || ref.instance() != actor.instance()) {
            return false;
        }
        return context.dead || context.epoch == ref.epoch() && NativeCombatAttacker.token(actor.timed(), false) == ref.owner();
    }

    static boolean allow(CombatMonsterCurseLifecycle.Actor actor, CombatNpcDamageQueue.Event event) {
        if (event.physical() instanceof Plan plan2) { } else { return true; }
        if (plan2.guardian() && event.damage() <= 0) {
            return false;
        }
        if (plan2.target().owner() != NativeCombatAttacker.token(actor.timed(), false) || plan2.target().epoch() != CombatPoisonLifecycle.monsterEpoch(actor.timed(), false)) {
            return false;
        }
        for (ClientSession clientSession : actor.hub().connectedSessionsSnapshot()) {
            if (!plan2.owner().matchesFast(clientSession)) continue;
            Object object = CombatAccess.field(CombatPoisonAtomic.status(clientSession), "sourcePoisonLifecycle");
            if (object != null && CombatAccess.field(object, "sourcePlayerOwner") == plan2.owner().owner()) return true;
        }
        return false;
    }

    static void publish(ClientSession clientSession, ClassicWorldMonsterPopulation.PoisonUpdate poisonUpdate, CombatPoisonLifecycle.Scope scope, Plan plan) {
        if (clientSession == null) {
            return;
        }
        CombatAccess.call(clientSession, "sourceWorldDollCommitted", "(Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;Ljava/lang/String;JLlocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V", poisonUpdate.hits().get(0), "equipment-proc-healing skill=" + plan.skill(), plan.target().id(), poisonUpdate, scope);
    }

    private static final class Target
    implements NativeProcHealingKernel.Target {
        final ClientSession pc;
        final NativePlayerCommit.Binding owner;
        final NativeCombatAttacker.Ref ref;
        final CombatMonsterCurseLifecycle.Actor actor;
        final ClassicWorldMonsterCatalog.MonsterTemplate template;
        final long now;
        final int skill;
        boolean graphics;

        Target(ClientSession clientSession, NativePlayerCommit.Binding binding, NativeCombatAttacker.Ref ref, CombatMonsterCurseLifecycle.Actor actor, ClassicWorldMonsterCatalog.MonsterTemplate monsterTemplate, long l, int n) {
            this.pc = clientSession;
            this.owner = binding;
            this.ref = ref;
            this.actor = actor;
            this.template = monsterTemplate;
            this.now = l;
            this.skill = n;
        }

        @Override
        public boolean dead() {
            return NativePlayerCommit.integer(this.actor.actor(), "currentHp") <= 0 || (Boolean)CombatAccess.field(this.actor.actor(), "alive") == false;
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
        public boolean hasEffect(int n) {
            BattleTimedStatus battleTimedStatus = this.actor.timed();
            return switch (n) {
                case 164, 170 -> NativeProcNpcState.active(battleTimedStatus, n, this.now);
                case 173 -> {
                    if (this.actor.population().polluteWaterActive(this.ref.map(), this.ref.id(), this.now) || battleTimedStatus.polluteWaterActive(this.now) || NativeProcNpcState.active(battleTimedStatus, n, this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 4012 -> {
                    if (battleTimedStatus.waterDragonDirtyCurrentActive(this.now) || NativeProcNpcState.active(battleTimedStatus, n, this.now)) {
                        yield true;
                    }
                    yield false;
                }
                default -> throw new AssertionError((Object)("unexpected NPC Proc modifier=" + n));
            };
        }

        @Override
        public void removeEffect(int n) {
            throw new AssertionError((Object)"NPC healing must not consume PC-only Water Life");
        }

        @Override
        public void commit(int n) {
            CombatMonsterCurseLifecycle.enqueue(this.actor, new CombatMonsterCurseLifecycle.Recipient(this.owner.object(), this.owner.source(), this.owner.account(), this.owner.selected()), this.ref.epoch(), n, new Plan(this.ref, this.owner, this.skill, this.template.implementation().equals("L1Guardian")));
        }
    }

    record Plan(NativeCombatAttacker.Ref target, NativePlayerCommit.Binding owner, int skill, boolean guardian) implements CombatWorldCommit.Plan
    {
    }
}
