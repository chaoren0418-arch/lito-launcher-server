// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.BattleCompanion
 *  local.lito381.server.BattleCompanion$Kind
 *  local.lito381.server.BattleCompanion$Snapshot
 *  local.lito381.server.BattleInvisibilityStatus
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
 *  local.lito381.server.NativeCompanionProcScene
 *  local.lito381.server.NativeCompanionRefScene
 *  local.lito381.server.NativeProcHealBonus
 *  local.lito381.server.NativeProcHealingKernel
 *  local.lito381.server.NativeProcHealingKernel$Kind
 *  local.lito381.server.NativeProcHealingKernel$Target
 *  local.lito381.server.NativeProcNpcCatalog
 *  local.lito381.server.NativeProcNpcState
 *  local.lito381.server.NativeProcPlayerState
 *  local.lito381.server.Protocol
 *  local.lito381.server.Protocol$Writer
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleInvisibilityStatus;
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
import local.lito381.server.NativeCompanionProcScene;
import local.lito381.server.NativeCompanionRefScene;
import local.lito381.server.NativeNpcLifetime;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcHealBonus;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeProcHealingPlayer;
import local.lito381.server.NativeProcNpcCatalog;
import local.lito381.server.NativeProcNpcState;
import local.lito381.server.NativeProcPlayerState;
import local.lito381.server.NativeProcSkillCatalog;
import local.lito381.server.Protocol;

final class NativeProcHealingCompanion {
    NativeProcHealingCompanion() {
    }

    private static ClientSession find(GameHub gameHub, NativePlayerCommit.Binding binding) {
        if (binding == null) {
            return null;
        }
        for (ClientSession clientSession : gameHub.connectedSessionsSnapshot()) {
            if (!binding.matchesFast(clientSession)) continue;
            return clientSession;
        }
        return null;
    }

    private static boolean contains(List<ClientSession> list, ClientSession clientSession) {
        for (ClientSession clientSession2 : list) {
            if (clientSession2 != clientSession) continue;
            return true;
        }
        return false;
    }

    private static boolean registered(GameHub gameHub, ClientSession clientSession, NativePlayerCommit.Binding binding) {
        return switch (binding.phase()) {
            case "WORLD" -> NativeProcHealingCompanion.contains(gameHub.classicWorldMembers(binding.map(), binding.instance()), clientSession);
            case "BATTLE" -> NativeProcHealingCompanion.contains(gameHub.roomMembers(binding.room()), clientSession);
            default -> false;
        };
    }

    private static boolean sameScene(NativePlayerCommit.Binding binding, NativePlayerCommit.Binding binding2) {
        return binding.phase().equals(binding2.phase()) && (binding.phase().equals("WORLD") ? binding.map() == binding2.map() && binding.instance() == binding2.instance() : binding.room() == binding2.room());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static <T> T nativeProcSceneOriginalWithTarget(GameHub gameHub, Plan plan, boolean bl, Function<Live, T> function) {
        NativeCombatAttacker.Ref ref = plan.target();
        if (ref == null || ref.kind() != 2 || ref.master() == null) {
            return null;
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            BattleTimedStatus battleTimedStatus;
            ClientSession clientSession;
            ClientSession clientSession2 = NativeProcHealingCompanion.find(gameHub, ref.master());
            ClientSession clientSession3 = clientSession = bl ? NativeProcHealingCompanion.find(gameHub, plan.caster()) : null;
            if (clientSession2 == null || bl && clientSession == null) {
                return null;
            }
            BattleTimedStatus battleTimedStatus2 = CombatPoisonAtomic.status((ClientSession)clientSession2);
            BattleTimedStatus battleTimedStatus3 = battleTimedStatus = bl ? CombatPoisonAtomic.status((ClientSession)clientSession) : battleTimedStatus2;
            synchronized (battleTimedStatus3) {
                BattleTimedStatus battleTimedStatus4 = battleTimedStatus2;
                synchronized (battleTimedStatus4) {
                    List list;
                    if (!ref.master().matches(clientSession2) || !NativeProcHealingCompanion.registered(gameHub, clientSession2, ref.master())) {
                        return null;
                    }
                    if (!(!bl || plan.caster().matches(clientSession) && NativeProcHealingCompanion.registered(gameHub, clientSession, plan.caster()) && NativeProcHealingCompanion.sameScene(plan.caster(), ref.master()))) {
                        return null;
                    }
                    List list2 = list = (List)CombatAccess.field((Object)clientSession2, (String)"tamedCompanions");
                    synchronized (list2) {
                        Object object;
                        BattleCompanion object2 = (BattleCompanion)CombatAccess.field((Object)clientSession2, (String)"battleCompanion");
                        if (object2.snapshot().objectId() != ref.id()) {
                            object2 = null;
                            for (Object object3 : list) {
                                if (!(object3 instanceof BattleCompanion) || ((BattleCompanion)(object = (BattleCompanion)object3)).snapshot().objectId() != ref.id()) continue;
                                object2 = (BattleCompanion)object;
                                break;
                            }
                        }
                        if (object2 == null) {
                            return null;
                        }
                        BattleCompanion battleCompanion = object2;
                        synchronized (battleCompanion) {
                            Object object3;
                            object = object3 = (BattleTimedStatus)CombatAccess.field((Object)object2, (String)"timedStatus");
                            synchronized (object) {
                                BattleCompanion.Snapshot snapshot = object2.snapshot();
                                if (!snapshot.active() || snapshot.hp() <= 0 || snapshot.objectId() != ref.id() || NativeCombatAttacker.token((BattleTimedStatus)object3, false) != ref.owner()) {
                                    return null;
                                }
                                return function.apply(new Live(clientSession, clientSession2, (BattleCompanion)object2, (BattleTimedStatus)object3, snapshot));
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean apply(ClientSession clientSession, NativeCombatAttacker.Ref ref, NativeArmorProc.Rule rule) {
        Plan plan;
        if (ref == null || ref.kind() != 2 || rule.skill() <= 0) {
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
            throw new IllegalStateException("missing companion Proc skill=" + rule.skill());
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        Boolean bl = (Boolean)NativeProcHealingCompanion.withTarget(gameHub, plan = new Plan(ref, binding, rule.skill()), true, live -> {
            if (!NativePlayerCommit.validFrame(clientSession)) {
                return false;
            }
            long l = System.nanoTime();
            Target target = new Target(gameHub, plan, (Live)live, l);
            NativeProcHealingKernel.apply((NativeProcHealingKernel.Target)target, () -> {
                int n2 = NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(clientSession, l));
                return NativeProcHealingKernel.calculate((int)skill.damageDice(), (int)skill.damageValue(), (int)n2, (int)NativePlayerCommit.integer(clientSession, "playerLawful"), (int)10, n -> ThreadLocalRandom.current().nextInt(n));
            }, (int)((int)CombatRules.number((String)"NATURES_BLESSING_HEAL", (double)0.0)), () -> {
                target.graphics = true;
            });
            return target.graphics;
        });
        if (Boolean.TRUE.equals(bl)) {
            NativeProcHealingCompanion.graphics(gameHub, plan, skill.castGraphics());
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean enqueue(GameHub gameHub, BattleTimedStatus battleTimedStatus, Plan plan, int n) {
        if (n >= 0) {
            throw new IllegalArgumentException("companion Proc recovery requires negative damage");
        }
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
        return Boolean.TRUE.equals(NativeProcHealingCompanion.withTarget(gameHub, plan, true, live -> NativeProcHealingCompanion.enqueue(gameHub, live.timed(), plan, n)));
    }

    private static void publish(GameHub gameHub, Receipt receipt) {
        Meter meter = (Meter)NativeProcHealingCompanion.withTarget(gameHub, receipt.plan(), false, live -> {
            int n = receipt.pet() ? live.snapshot().hp() : receipt.hp();
            int n2 = live.snapshot().maxHp();
            if (n2 <= 0) {
                return null;
            }
            byte[] byArray = new Protocol.Writer().c(104).d(receipt.plan().target().id()).c(100 * n / n2).bytes();
            return new Meter(live.master(), receipt.plan().target().master(), byArray);
        });
        if (meter == null || !NativeCompanionProcScene.meterBinding((NativePlayerCommit.Binding)meter.binding(), (ClientSession)meter.master())) {
            return;
        }
        if (Boolean.TRUE.equals(NativeProcHealingCompanion.withTarget(gameHub, receipt.plan(), false, live -> live.master() == meter.master()))) {
            NativeCompanionProcScene.sendMeter((ClientSession)meter.master(), (byte[])meter.packet(), (String)("S_HIT_RATIO equipment-companion-healing skill=" + receipt.plan().skill() + " target=" + receipt.plan().target().id()), (GameHub)gameHub, (NativeCombatAttacker.Ref)receipt.plan().target());
        }
    }

    private static void graphics(GameHub gameHub, Plan plan, int n) {
        if (n <= 0) {
            return;
        }
        ClientSession clientSession = (ClientSession)NativeProcHealingCompanion.withTarget(gameHub, plan, true, live -> live.caster());
        if (clientSession == null || !NativePlayerCommit.validFrame(clientSession) || !NativeProcPlayerState.show((ClientSession)clientSession)) {
            return;
        }
        NativePlayerCommit.Binding binding = plan.caster();
        List<ClientSession> list = binding.phase().equals("WORLD") ? gameHub.classicWorldMembers(binding.map(), binding.instance()) : gameHub.roomMembers(binding.room());
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        arrayList.add(clientSession);
        if (!((BattleInvisibilityStatus)CombatAccess.field((Object)clientSession, (String)"invisibilityStatus")).active()) {
            for (ClientSession clientSession2 : list) {
                if (clientSession2 == clientSession || Math.max(Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionX") - (long)NativePlayerCommit.integer(clientSession, "positionX")), Math.abs((long)NativePlayerCommit.integer(clientSession2, "positionY") - (long)NativePlayerCommit.integer(clientSession, "positionY"))) > 10L) continue;
                arrayList.add(clientSession2);
            }
        }
        Object object = Protocol.effectObject((long)plan.target().id(), (int)n);
        for (ClientSession clientSession3 : arrayList) {
            NativePlayerCommit.Binding binding2 = NativePlayerCommit.Binding.capture(clientSession3);
            if (!NativePlayerCommit.validFrame(clientSession) || !Boolean.TRUE.equals(NativeProcHealingCompanion.withTarget(gameHub, plan, true, live -> true))) break;
            if (!binding2.matches(clientSession3)) continue;
            NativePlayerCommit.safeSend(clientSession3, (byte[])object, "S_EFFECT equipment-companion-healing skill=" + plan.skill() + " target=" + plan.target().id());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T withTarget(GameHub gameHub, Plan plan, boolean bl, Function<Live, T> function) {
        if (NativeCompanionRefScene.worldRef((NativeCombatAttacker.Ref)plan.target())) {
            return (T)NativeCompanionProcScene.withTarget((GameHub)gameHub, (Plan)plan, (boolean)bl, (Function)function);
        }
        return NativeProcHealingCompanion.nativeProcSceneOriginalWithTarget(gameHub, plan, bl, function);
    }

    static Object nativeProcSceneLive(ClientSession clientSession, ClientSession clientSession2, BattleCompanion battleCompanion, BattleTimedStatus battleTimedStatus, BattleCompanion.Snapshot snapshot) {
        return new Live(clientSession, clientSession2, battleCompanion, battleTimedStatus, snapshot);
    }

    record Plan(NativeCombatAttacker.Ref target, NativePlayerCommit.Binding caster, int skill) implements CombatWorldCommit.Plan
    {
    }

    private record Live(ClientSession caster, ClientSession master, BattleCompanion actor, BattleTimedStatus timed, BattleCompanion.Snapshot snapshot) {
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
                    Object object = event.physical();
                    if (!(object instanceof Plan)) {
                        throw new IllegalStateException("foreign companion HP queue plan");
                    }
                    Plan plan = (Plan)object;
                    object = (Receipt)NativeProcHealingCompanion.withTarget(this.hub, plan, true, live -> {
                        if (event.damage() >= 0) {
                            throw new IllegalStateException("invalid companion recovery event");
                        }
                        BattleCompanion.Snapshot snapshot = live.snapshot();
                        int n = (int)Math.min((long)snapshot.maxHp(), (long)snapshot.hp() - (long)event.damage());
                        if (n <= snapshot.hp()) {
                            return null;
                        }
                        int n2 = live.actor().heal(n - snapshot.hp());
                        return n2 == snapshot.hp() ? null : new Receipt(plan, n2, snapshot.kind() == BattleCompanion.Kind.PET);
                    });
                    if (object == null) continue;
                    NativeProcHealingCompanion.publish(this.hub, (Receipt)object);
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

    private record Receipt(Plan plan, int hp, boolean pet) {
    }

    private record Meter(ClientSession master, NativePlayerCommit.Binding binding, byte[] packet) {
    }

    private static final class Target
    implements NativeProcHealingKernel.Target {
        final GameHub hub;
        final Plan plan;
        final Live live;
        final long now;
        boolean graphics;

        Target(GameHub gameHub, Plan plan, Live live, long l) {
            this.hub = gameHub;
            this.plan = plan;
            this.live = live;
            this.now = l;
        }

        public boolean dead() {
            return !this.live.actor().snapshot().active() || this.live.actor().snapshot().hp() <= 0;
        }

        public NativeProcHealingKernel.Kind kind() {
            return NativeProcNpcCatalog.companionKind((BattleCompanion.Kind)this.live.snapshot().kind());
        }

        public int undead() {
            return this.live.actor().undead();
        }

        public boolean hasEffect(int n) {
            BattleTimedStatus battleTimedStatus = this.live.timed();
            return switch (n) {
                case 164, 170 -> NativeProcNpcState.active((BattleTimedStatus)battleTimedStatus, (int)n, (long)this.now);
                case 173 -> {
                    if (battleTimedStatus.polluteWaterActive(this.now) || NativeProcNpcState.active((BattleTimedStatus)battleTimedStatus, (int)n, (long)this.now)) {
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
                default -> throw new AssertionError((Object)("unexpected companion Proc modifier=" + n));
            };
        }

        public void removeEffect(int n) {
            throw new AssertionError((Object)"NPC Proc healing must retain Water Life");
        }

        public void commit(int n) {
            NativeProcHealingCompanion.enqueue(this.hub, this.live.timed(), this.plan, n);
        }
    }
}

