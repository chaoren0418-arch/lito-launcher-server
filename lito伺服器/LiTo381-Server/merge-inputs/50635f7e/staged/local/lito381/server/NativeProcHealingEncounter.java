// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.BattleInvisibilityStatus
 *  local.lito381.server.BattleQuestEncounter$Damage
 *  local.lito381.server.BattleQuestEncounter$Healing
 *  local.lito381.server.BattleQuestEncounter$Hit
 *  local.lito381.server.BattleRules$MatchState
 *  local.lito381.server.BattleRules$Mode
 *  local.lito381.server.BattleRules$Resolution
 *  local.lito381.server.BattleRules$RuleSet
 *  local.lito381.server.BattleTimeAttackEncounter
 *  local.lito381.server.BattleTimeAttackEncounter$Damage
 *  local.lito381.server.BattleTimeAttackEncounter$Healing
 *  local.lito381.server.BattleTimeAttackEncounter$Hit
 *  local.lito381.server.BattleTimedStatus
 *  local.lito381.server.ClientSession
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.CombatNpcDamageQueue
 *  local.lito381.server.CombatNpcDamageQueue$Event
 *  local.lito381.server.CombatNpcDamageQueue$Queue
 *  local.lito381.server.CombatPoisonAtomic
 *  local.lito381.server.CombatPoisonLifecycle
 *  local.lito381.server.CombatRules
 *  local.lito381.server.CombatWorldCommit$Plan
 *  local.lito381.server.GameHub
 *  local.lito381.server.GameHub$TimeAttackActorHit
 *  local.lito381.server.NativeDeAiTimers
 *  local.lito381.server.NativeProcEncounterCatalog
 *  local.lito381.server.NativeProcEncounterCatalog$Npc
 *  local.lito381.server.NativeProcEncounterCatalog$Receiver
 *  local.lito381.server.NativeProcEncounterPublication
 *  local.lito381.server.NativeProcEncounterPublication$Context
 *  local.lito381.server.NativeProcFollowerReceiver
 *  local.lito381.server.NativeProcHealBonus
 *  local.lito381.server.NativeProcHealingKernel
 *  local.lito381.server.NativeProcHealingKernel$Kind
 *  local.lito381.server.NativeProcHealingKernel$Target
 *  local.lito381.server.NativeProcNpcState
 *  local.lito381.server.NativeProcPlayerState
 *  local.lito381.server.NativeSourceCubeNpcQueue
 *  local.lito381.server.NativeSourcePhysicalNpc
 *  local.lito381.server.NativeSourcePoisonNpc
 *  local.lito381.server.Protocol
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import local.lito381.server.BattleInvisibilityStatus;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.CombatRules;
import local.lito381.server.CombatWorldCommit;
import local.lito381.server.GameHub;
import local.lito381.server.NativeArmorProc;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativeNpcLifetime;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcEncounterCatalog;
import local.lito381.server.NativeProcEncounterPublication;
import local.lito381.server.NativeProcFollowerReceiver;
import local.lito381.server.NativeProcHealBonus;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeProcHealingPlayer;
import local.lito381.server.NativeProcNpcState;
import local.lito381.server.NativeProcPlayerState;
import local.lito381.server.NativeProcSkillCatalog;
import local.lito381.server.NativeSourceCubeNpcQueue;
import local.lito381.server.NativeSourcePhysicalNpc;
import local.lito381.server.NativeSourcePoisonNpc;
import local.lito381.server.Protocol;

final class NativeProcHealingEncounter {
    static final String P = "local/lito381/server/";
    static final String GEN = "sourceProcEncounterGeneration";
    private static final ThreadLocal<Object> direct = new ThreadLocal();

    NativeProcHealingEncounter() {
    }

    static boolean direct(Object object) {
        return direct.get() == object;
    }

    static boolean idle() {
        return direct.get() == null;
    }

    static void retire(Object object) {
        NativeDeAiTimers.retireContainer((Object)object);
        CombatAccess.set((Object)object, (String)GEN, (Object)new Object());
    }

    static Object generation(Object object) {
        Object object2 = CombatAccess.field((Object)object, (String)GEN);
        if (object2 == null) {
            object2 = new Object();
            CombatAccess.set((Object)object, (String)GEN, (Object)object2);
        }
        return object2;
    }

    static String type(NativeCombatAttacker.Ref ref) {
        return ref.kind() == 3 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter";
    }

    static Object room(GameHub gameHub, long l) {
        return ((Map)CombatAccess.field((Object)gameHub, (String)"rooms")).get(l);
    }

    static Object container(Object object, NativeCombatAttacker.Ref ref) {
        return CombatAccess.field((Object)object, (String)(ref.kind() == 3 ? "questEncounter" : "timeAttackEncounter"));
    }

    static Object actor(Object object, NativeCombatAttacker.Ref ref) {
        return CombatAccess.call((Object)object, (String)"monster", (String)("(J)Llocal/lito381/server/" + NativeProcHealingEncounter.type(ref) + "$Monster;"), (Object[])new Object[]{ref.id()});
    }

    static Object snapshot(Object object, NativeCombatAttacker.Ref ref) {
        return CombatAccess.call((Object)object, (String)"snapshot", (String)("()Llocal/lito381/server/" + NativeProcHealingEncounter.type(ref) + "$Snapshot;"), (Object[])new Object[0]);
    }

    static boolean mode(Object object, NativeCombatAttacker.Ref ref) {
        if (object == null || !((Boolean)CombatAccess.field((Object)object, (String)"battleStarted")).booleanValue()) {
            return false;
        }
        BattleRules.RuleSet ruleSet = ((BattleRules.MatchState)CombatAccess.field((Object)object, (String)"match")).rules();
        return ruleSet.mode() == (ref.kind() == 3 ? BattleRules.Mode.QUEST_BATTLE : BattleRules.Mode.TIME_ATTACK);
    }

    static ClientSession find(GameHub gameHub, NativePlayerCommit.Binding binding) {
        for (ClientSession clientSession : gameHub.connectedSessionsSnapshot()) {
            if (!binding.matchesFast(clientSession)) continue;
            return clientSession;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T withLive(GameHub gameHub, NativeCombatAttacker.Ref ref, NativePlayerCommit.Binding binding, Object object, Function<Live, T> function) {
        if (ref == null || ref.kind() != 3 && ref.kind() != 4 || binding == null) {
            return null;
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Object object2 = NativeProcHealingEncounter.room(gameHub, ref.room());
            ClientSession clientSession = NativeProcHealingEncounter.find(gameHub, binding);
            if (!NativeProcHealingEncounter.mode(object2, ref) || clientSession == null || !binding.phase().equals("BATTLE") || binding.room() != ref.room()) {
                return null;
            }
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
            synchronized (battleTimedStatus) {
                Object object3;
                if (!(binding.matches(clientSession) && ((Set)CombatAccess.field((Object)object2, (String)"members")).contains(clientSession) && ((Set)CombatAccess.field((Object)object2, (String)"battleRoster")).contains(clientSession))) {
                    return null;
                }
                Object object4 = object3 = NativeProcHealingEncounter.container(object2, ref);
                synchronized (object4) {
                    BattleTimedStatus battleTimedStatus2;
                    if (object != null && NativeProcHealingEncounter.generation(object3) != object) {
                        return null;
                    }
                    Object object5 = NativeProcHealingEncounter.actor(object3, ref);
                    if (object5 == null) {
                        return null;
                    }
                    BattleTimedStatus battleTimedStatus3 = battleTimedStatus2 = (BattleTimedStatus)CombatAccess.field((Object)object5, (String)"damagePoison");
                    synchronized (battleTimedStatus3) {
                        Object object6 = NativeProcHealingEncounter.snapshot(object5, ref);
                        if (NativeCombatAttacker.token(battleTimedStatus2, false) != ref.owner() || NativePlayerCommit.integer(object6, "hp") <= 0) {
                            return null;
                        }
                        return function.apply(new Live(gameHub, object2, object3, object5, battleTimedStatus2, object6, clientSession));
                    }
                }
            }
        }
    }

    static boolean apply(ClientSession clientSession, NativeCombatAttacker.Ref ref, NativeArmorProc.Rule rule) {
        if (ref == null || ref.kind() != 3 && ref.kind() != 4 || rule.skill() <= 0) {
            return false;
        }
        NativeProcSkillCatalog.Catalog catalog = NativeProcSkillCatalog.catalog();
        if (!catalog.healing(rule.skill())) {
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.frameBinding(clientSession);
        if (binding == null || !binding.matches(clientSession) || !NativePlayerCommit.validFrame(clientSession)) {
            return true;
        }
        NativeProcSkillCatalog.Skill skill = catalog.find(rule.skill());
        if (skill == null) {
            throw new IllegalStateException("missing encounter Proc skill=" + rule.skill());
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        Plan plan = NativeProcHealingEncounter.withLive(gameHub, ref, binding, null, live -> {
            if (!NativePlayerCommit.validFrame(clientSession)) {
                return null;
            }
            NativeProcEncounterCatalog.Npc npc = NativeProcEncounterCatalog.catalog().require(NativePlayerCommit.integer(live.snapshot(), "npcId"));
            Plan applied = new Plan(ref, binding, rule.skill(), NativeProcHealingEncounter.generation(live.container()), npc);
            long l = System.nanoTime();
            Target target = new Target((Live)live, applied, l);
            NativeProcHealingKernel.apply((NativeProcHealingKernel.Target)target, () -> NativeProcHealingKernel.calculate((int)skill.damageDice(), (int)skill.damageValue(), (int)NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(clientSession, l)), (int)NativePlayerCommit.integer(clientSession, "playerLawful"), (int)10, n -> ThreadLocalRandom.current().nextInt(n)), (int)((int)CombatRules.number((String)"NATURES_BLESSING_HEAL", (double)0.0)), () -> {
                target.graphics = true;
            });
            return target.graphics ? applied : null;
        });
        if (plan != null) {
            NativeProcHealingEncounter.graphics(gameHub, plan, skill.castGraphics());
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean enqueue(GameHub gameHub, BattleTimedStatus battleTimedStatus, Plan plan, int n) {
        CombatNpcDamageQueue.Queue queue = CombatPoisonLifecycle.monsterDamage((BattleTimedStatus)battleTimedStatus, (boolean)true);
        NativePlayerCommit.Binding binding = plan.caster();
        CombatNpcDamageQueue.Event event = new CombatNpcDamageQueue.Event(plan.target().epoch(), System.nanoTime(), ClassicWorldMonsterPopulation.ContributorKind.PLAYER, binding.object(), binding.object(), n, binding.object(), binding.source(), binding.account(), binding.selected(), (CombatWorldCommit.Plan)plan);
        CombatNpcDamageQueue.Queue queue2 = queue;
        synchronized (queue2) {
            if (!NativeNpcLifetime.offer(queue, event, gameHub)) {
                return false;
            }
            if (queue.start()) {
                NativeNpcLifetime.Lane lane = NativeNpcLifetime.lane(queue, gameHub);
                Dispatch dispatch = new Dispatch(gameHub, queue);
                lane.task = dispatch;
                lane.life.npcs.add(lane);
                NativeNpcLifetime.dispatch(lane, dispatch, 0);
            }
        }
        return true;
    }

    static boolean offer(GameHub gameHub, Plan plan, int n) {
        return Boolean.TRUE.equals(NativeProcHealingEncounter.withLive(gameHub, plan.target(), plan.caster(), plan.generation(), live -> NativeProcHealingEncounter.enqueue(gameHub, live.timed(), plan, n)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static NativeProcEncounterPublication.Context commit(Live live, Plan plan, int n) {
        Record hit;
        int n2;
        if (NativeProcFollowerReceiver.blocked((NativeProcEncounterCatalog.Npc)plan.npc(), (Object)live.snapshot(), (int)n)) {
            return null;
        }
        NativeProcEncounterCatalog.Receiver receiver = plan.npc().receiver();
        if (receiver == NativeProcEncounterCatalog.Receiver.NONE || receiver == NativeProcEncounterCatalog.Receiver.POSITIVE_ONLY && n <= 0) {
            return null;
        }
        if (receiver == NativeProcEncounterCatalog.Receiver.UNSUPPORTED) {
            throw new IllegalStateException("unqualified encounter receive owner");
        }
        NativeCombatAttacker.Ref ref = plan.target();
        int n3 = n2 = ref.kind() == 3 ? live.hub().questMonsterPoisonPalette(ref.room(), ref.id(), System.nanoTime()) : live.hub().timeAttackActorPoisonPalette(ref.room(), ref.id(), System.nanoTime());
        if (n > 0) {
            Object object = direct.get();
            direct.set(live.actor());
            try {
                hit = ref.kind() == 3 ? live.hub().damageQuestMonster(ref.room(), live.caster(), ref.id(), n) : live.hub().damageTimeAttackActor(ref.room(), live.caster(), ref.id(), n);
            }
            finally {
                if (object == null) {
                    direct.remove();
                } else {
                    direct.set(object);
                }
            }
            if (hit == null) {
                return null;
            }
        } else {
            int n4 = NativePlayerCommit.integer(live.actor(), "hp");
            int n5 = NativePlayerCommit.integer(live.snapshot(), "maxHp");
            int n6 = (int)Math.min((long)n5, (long)n4 - (long)n);
            if (n6 == n4) {
                return null;
            }
            Object object = CombatAccess.call((Object)live.actor(), (String)"heal", (String)("(I)Llocal/lito381/server/" + NativeProcHealingEncounter.type(ref) + "$Healing;"), (Object[])new Object[]{n6 - n4});
            if (object == null) {
                return null;
            }
            if (ref.kind() == 3) {
                BattleQuestEncounter.Healing healing = (BattleQuestEncounter.Healing)object;
                hit = new BattleQuestEncounter.Hit(new BattleQuestEncounter.Damage(healing.before(), healing.after(), false), false, 0, List.of(), false);
            } else {
                BattleTimeAttackEncounter.Healing healing = (BattleTimeAttackEncounter.Healing)object;
                int n7 = ((BattleTimeAttackEncounter)live.container()).snapshots().size();
                hit = new GameHub.TimeAttackActorHit(new BattleTimeAttackEncounter.Hit(new BattleTimeAttackEncounter.Damage(healing.before(), healing.after(), false), false, n7, List.of()), BattleRules.Resolution.continuing((boolean)false, (String)"equipment-proc-healing"));
            }
        }
        return new NativeProcEncounterPublication.Context(live, plan, (Object)hit, n2, n, NativeCombatAttacker.token(live.timed(), false));
    }

    private static void graphics(GameHub gameHub, Plan plan, int n) {
        Object object;
        if (n <= 0) {
            return;
        }
        ClientSession clientSession = NativeProcHealingEncounter.withLive(gameHub, plan.target(), plan.caster(), plan.generation(), Live::caster);
        if (clientSession == null || !NativePlayerCommit.validFrame(clientSession) || !NativeProcPlayerState.show((ClientSession)clientSession)) {
            return;
        }
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        arrayList.add(clientSession);
        if (!((BattleInvisibilityStatus)CombatAccess.field((Object)clientSession, (String)"invisibilityStatus")).active()) {
            java.util.Iterator<ClientSession> members = gameHub.roomMembers(plan.target().room()).iterator();
            object = members;
            while (members.hasNext()) {
                ClientSession clientSession2 = members.next();
                if (clientSession2 == clientSession || Math.max(Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionX") - (long)NativePlayerCommit.integer(clientSession, "positionX")), Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionY") - (long)NativePlayerCommit.integer(clientSession, "positionY"))) > 10L) continue;
                arrayList.add(clientSession2);
            }
        }
        object = Protocol.effectObject((long)plan.target().id(), (int)n);
        for (ClientSession clientSession3 : arrayList) {
            NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession3);
            if (!NativePlayerCommit.validFrame(clientSession) || !Boolean.TRUE.equals(NativeProcHealingEncounter.withLive(gameHub, plan.target(), plan.caster(), plan.generation(), live -> true))) break;
            if (!binding.matches(clientSession3)) continue;
            NativePlayerCommit.safeSend(clientSession3, (byte[])object, "S_EFFECT equipment-encounter-healing skill=" + plan.skill() + " target=" + plan.target().id());
        }
    }

    static Runnable nativeSourceCubeDrain(GameHub gameHub, CombatNpcDamageQueue.Queue queue) {
        return new Dispatch(gameHub, queue);
    }

    record Live(GameHub hub, Object room, Object container, Object actor, BattleTimedStatus timed, Object snapshot, ClientSession caster) {
    }

    record Plan(NativeCombatAttacker.Ref target, NativePlayerCommit.Binding caster, int skill, Object generation, NativeProcEncounterCatalog.Npc npc) implements CombatWorldCommit.Plan
    {
    }

    private static final class Dispatch
    implements Runnable {
        final GameHub hub;
        final CombatNpcDamageQueue.Queue queue;

        Dispatch(GameHub gameHub, CombatNpcDamageQueue.Queue queue) {
            this.hub = gameHub;
            this.queue = queue;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            for (int i = 0; i < 256; ++i) {
                CombatNpcDamageQueue.Event event = this.queue.claim();
                if (event == null) {
                    return;
                }
                try {
                    if (NativeSourcePoisonNpc.consume((GameHub)this.hub, (CombatNpcDamageQueue.Queue)this.queue, (CombatNpcDamageQueue.Event)event)) {
                        continue;
                    }
                    if (NativeSourcePhysicalNpc.consume((GameHub)this.hub, (CombatNpcDamageQueue.Queue)this.queue, (CombatNpcDamageQueue.Event)event)) {
                        continue;
                    }
                    if (NativeSourceCubeNpcQueue.consume((GameHub)this.hub, (CombatNpcDamageQueue.Queue)this.queue, (CombatNpcDamageQueue.Event)event)) {
                        continue;
                    }
                    Object object = event.physical();
                    if (!(object instanceof Plan)) {
                        throw new IllegalStateException("foreign encounter Proc queue plan");
                    }
                    Plan plan2 = (Plan)object;
                    object = NativeProcHealingEncounter.withLive(this.hub, plan2.target(), plan2.caster(), plan2.generation(), live -> NativeProcHealingEncounter.commit(live, plan2, event.damage()));
                    if (object == null) continue;
                    NativeProcEncounterPublication.publish((NativeProcEncounterPublication.Context)object);
                    continue;
                }
                catch (Throwable throwable) {
                    CombatNpcDamageQueue.failed((Throwable)throwable);
                    continue;
                }
                finally {
                    this.queue.complete(event);
                }
            }
        }
    }

    private static final class Target
    implements NativeProcHealingKernel.Target {
        final Live live;
        final Plan plan;
        final long now;
        boolean graphics;

        Target(Live live, Plan plan, long l) {
            this.live = live;
            this.plan = plan;
            this.now = l;
        }

        public boolean dead() {
            return NativePlayerCommit.integer(this.live.actor(), "hp") <= 0;
        }

        public NativeProcHealingKernel.Kind kind() {
            return this.plan.npc().kind();
        }

        public int undead() {
            return this.plan.npc().undead();
        }

        public boolean hasEffect(int n) {
            BattleTimedStatus battleTimedStatus = this.live.timed();
            return switch (n) {
                case 164, 170 -> NativeProcNpcState.active((BattleTimedStatus)battleTimedStatus, (int)n, (long)this.now);
                case 173 -> {
                    if (((Boolean)CombatAccess.call((Object)this.live.container(), (String)"polluteWaterActive", (String)"(JJ)Z", (Object[])new Object[]{this.plan.target().id(), this.now})).booleanValue() || battleTimedStatus.polluteWaterActive(this.now) || NativeProcNpcState.active((BattleTimedStatus)battleTimedStatus, (int)n, (long)this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 4012 -> {
                    if (battleTimedStatus.waterDragonDirtyCurrentActive(this.now) || NativeProcNpcState.active((BattleTimedStatus)battleTimedStatus, (int)n, (long)this.now)) {
                        yield true;
                    }
                    yield false;
                }
                default -> throw new AssertionError((Object)("unexpected encounter Proc modifier=" + n));
            };
        }

        public void removeEffect(int n) {
            throw new AssertionError((Object)"NPC Proc healing must retain Water Life");
        }

        public void commit(int n) {
            NativeProcHealingEncounter.enqueue(this.live.hub(), this.live.timed(), this.plan, n);
        }
    }
}

