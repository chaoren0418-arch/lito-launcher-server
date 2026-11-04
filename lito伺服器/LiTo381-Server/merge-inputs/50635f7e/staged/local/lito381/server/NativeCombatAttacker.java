// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.BattleCompanion
 *  local.lito381.server.BattleCompanion$Damage
 *  local.lito381.server.BattleCompanion$Snapshot
 *  local.lito381.server.BattleHitReactionSuppressCatalog
 *  local.lito381.server.BattleJoyOfPain
 *  local.lito381.server.BattleJoyOfPainStatus
 *  local.lito381.server.BattleJoyOfPainStatus$Lease
 *  local.lito381.server.BattleMortalBody$Decision
 *  local.lito381.server.BattleQuestEncounter$Hit
 *  local.lito381.server.BattleQuestEncounter$ManaDrain
 *  local.lito381.server.BattleRules$Mode
 *  local.lito381.server.BattleTimedStatus
 *  local.lito381.server.ClientSession
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.CombatMonsterCurseLifecycle
 *  local.lito381.server.CombatMonsterCurseLifecycle$Actor
 *  local.lito381.server.CombatMonsterCurseLifecycle$Context
 *  local.lito381.server.CombatMonsterCurseLifecycle$Recipient
 *  local.lito381.server.CombatPoisonLifecycle
 *  local.lito381.server.CombatPoisonLifecycle$Scope
 *  local.lito381.server.CombatPoisonLifecycle$State
 *  local.lito381.server.CombatProbability
 *  local.lito381.server.CombatRules
 *  local.lito381.server.CombatWorldCommit$Plan
 *  local.lito381.server.CombatWorldPhysical
 *  local.lito381.server.GameHub
 *  local.lito381.server.GameHub$MonsterAiTick
 *  local.lito381.server.GameHub$TimeAttackActorHit
 *  local.lito381.server.NativeCompanionCombatScene
 *  local.lito381.server.NativeCompanionRefScene
 *  local.lito381.server.NativeCompanionReflectionScene
 *  local.lito381.server.NativeDeAiTimers
 *  local.lito381.server.NativeProcHealingWorld
 *  local.lito381.server.NativeSourceCompanionClock
 *  local.lito381.server.NativeSourceCompanionFirstActor
 *  local.lito381.server.NativeSourceCubeNpc
 *  local.lito381.server.NativeSourceCubePlayer
 *  local.lito381.server.NativeSourceNpcPlayer
 *  local.lito381.server.NativeSourceNpcTickView
 *  local.lito381.server.NativeSourceRawDamage
 *  local.lito381.server.NativeSourceRefRawNpc
 *  local.lito381.server.Protocol
 */
package local.lito381.server;

import java.util.List;
import java.util.Map;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleHitReactionSuppressCatalog;
import local.lito381.server.BattleJoyOfPain;
import local.lito381.server.BattleJoyOfPainStatus;
import local.lito381.server.BattleMortalBody;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.CombatProbability;
import local.lito381.server.CombatRules;
import local.lito381.server.CombatWorldCommit;
import local.lito381.server.CombatWorldPhysical;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatOrigin;
import local.lito381.server.NativeCompanionCombatScene;
import local.lito381.server.NativeCompanionRefScene;
import local.lito381.server.NativeCompanionReflectionScene;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcHealingEncounter;
import local.lito381.server.NativeProcHealingWorld;
import local.lito381.server.NativeSourceCompanionClock;
import local.lito381.server.NativeSourceCompanionFirstActor;
import local.lito381.server.NativeSourceCubeNpc;
import local.lito381.server.NativeSourceCubePlayer;
import local.lito381.server.NativeSourceNpcPlayer;
import local.lito381.server.NativeSourceNpcTickView;
import local.lito381.server.NativeSourceRawDamage;
import local.lito381.server.NativeSourceRefRawNpc;
import local.lito381.server.Protocol;

final class NativeCombatAttacker {
    private static final String P = "local/lito381/server/";
    static final int WORLD = 1;
    static final int COMPANION = 2;
    static final int QUEST = 3;
    static final int TIME = 4;
    private static final ThreadLocal<Ingress> ingress = new ThreadLocal();
    private static final ThreadLocal<Ref> reflecting = new ThreadLocal();

    NativeCombatAttacker() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Object token(BattleTimedStatus battleTimedStatus, boolean bl) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)bl);
            if (state == null) {
                return null;
            }
            Object object = CombatAccess.field((Object)state, (String)"sourceNpcOwner");
            if (object == null && bl) {
                object = new Object();
                CombatAccess.set((Object)state, (String)"sourceNpcOwner", (Object)object);
            }
            return object;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void advance(Object object) {
        BattleTimedStatus battleTimedStatus;
        if (!CombatRules.loaded()) {
            return;
        }
        String string = object instanceof BattleCompanion ? "timedStatus" : "damagePoison";
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)object, (String)string);
        synchronized (battleTimedStatus2) {
            NativeDeAiTimers.retireStatus((BattleTimedStatus)battleTimedStatus);
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)true);
            CombatAccess.set((Object)state, (String)"sourceNpcOwner", (Object)new Object());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void resetWorld(BattleTimedStatus battleTimedStatus) {
        if (!CombatRules.loaded()) {
            return;
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)false);
            if (state != null && CombatAccess.field((Object)state, (String)"sourceNpcOwner") != null) {
                CombatAccess.set((Object)state, (String)"sourceNpcOwner", (Object)new Object());
            }
        }
    }

    static Ingress enterCompanion(ClientSession clientSession, BattleCompanion battleCompanion) {
        if (!CombatRules.loaded()) {
            return (Ingress)NativeSourceNpcPlayer.attach(null, (ClientSession)clientSession, null, (BattleCompanion)battleCompanion);
        }
        return (Ingress)NativeSourceNpcPlayer.attach((Object)NativeCombatAttacker.enter(NativeCombatAttacker.companion(clientSession, battleCompanion)), (ClientSession)clientSession, null, (BattleCompanion)battleCompanion);
    }

    static Ingress enterMonster(ClientSession clientSession, GameHub.MonsterAiTick monsterAiTick) {
        if (!CombatRules.loaded()) {
            return (Ingress)NativeSourceNpcPlayer.attach(null, (ClientSession)clientSession, (GameHub.MonsterAiTick)monsterAiTick, null);
        }
        return (Ingress)NativeSourceNpcPlayer.attach((Object)NativeCombatAttacker.enter(NativeCombatOrigin.tickAttacker(monsterAiTick)), (ClientSession)clientSession, (GameHub.MonsterAiTick)monsterAiTick, null);
    }

    static Ingress enterWorld(ClientSession clientSession, long l) {
        if (!CombatRules.loaded()) {
            return null;
        }
        Ingress ingress = NativeCombatAttacker.ingress.get();
        return ingress != null && ingress.blocked ? NativeCombatAttacker.enter(null) : (ingress != null && ingress.attacker != null && ingress.attacker.kind == 1 && ingress.attacker.id == l ? NativeCombatAttacker.enter(ingress.attacker) : NativeCombatAttacker.enter(NativeCombatAttacker.world(clientSession, l)));
    }

    static ClassicWorldMonsterPopulation.Snapshot capturedSnapshot(long l) {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        while (ingress != null) {
            if (ingress.snapshot != null && ingress.snapshot.objectId() == l) {
                return ingress.snapshot;
            }
            ingress = ingress.previous;
        }
        return null;
    }

    static Ingress enterKnown(Ref ref) {
        return NativeCombatAttacker.enter(ref);
    }

    static boolean blocked() {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        return ingress != null && ingress.blocked;
    }

    static ClassicWorldMonsterPopulation.Snapshot worldSnapshot(ClientSession clientSession, Ref ref) {
        Live live = NativeCombatAttacker.live(clientSession, ref);
        return live == null ? null : (ClassicWorldMonsterPopulation.Snapshot)live.snapshot;
    }

    private static Ingress enter(Ref ref) {
        Ingress ingress = new Ingress(ref, NativeCombatAttacker.ingress.get(), NativeCombatAttacker.ingress.get() != null && NativeCombatAttacker.ingress.get().blocked);
        NativeCombatAttacker.ingress.set(ingress);
        return ingress;
    }

    static Ingress enterPoison() {
        if (!CombatRules.loaded()) {
            return null;
        }
        Ingress ingress = new Ingress(null, NativeCombatAttacker.ingress.get(), true);
        NativeCombatAttacker.ingress.set(ingress);
        return ingress;
    }

    static void leave(Ingress ingress) {
        NativeSourceNpcPlayer.clear((Object)ingress);
        if (ingress == null) {
            return;
        }
        if (NativeCombatAttacker.ingress.get() != ingress) {
            throw new IllegalStateException("native NPC ingress scope mismatch");
        }
        if (ingress.previous == null) {
            NativeCombatAttacker.ingress.remove();
        } else {
            NativeCombatAttacker.ingress.set(ingress.previous);
        }
    }

    static boolean idle() {
        return ingress.get() == null && reflecting.get() == null;
    }

    static Ref current() {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        return ingress == null ? null : ingress.attacker;
    }

    static void calculated(NativePlayerCommit.Binding binding) {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        if (ingress != null && ingress.attacker != null) {
            Ingress ingress2 = ingress;
            while (ingress2 != null && ingress.attacker.equals(ingress2.attacker)) {
                ingress2.lastTarget = binding;
                ingress2 = ingress2.previous;
            }
        }
    }

    static boolean relocated(ClientSession clientSession) {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        return CombatRules.loaded() && ingress != null && ingress.attacker != null && ingress.lastTarget != null && ingress.lastTarget.matches(clientSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Ref nativeRefOriginalCompanion(ClientSession clientSession, BattleCompanion battleCompanion) {
        if (clientSession == null || battleCompanion == null) {
            return null;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            BattleCompanion.Snapshot snapshot = battleCompanion.snapshot();
            if (!snapshot.active() || snapshot.hp() <= 0) {
                return null;
            }
            BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)battleCompanion, (String)"timedStatus");
            return new Ref(2, snapshot.objectId(), NativeCombatAttacker.token(battleTimedStatus, true), 0L, binding, binding.room(), binding.map(), binding.instance());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Ref world(ClientSession clientSession, long l) {
        if (l <= 0L || !CombatWorldPhysical.world((ClientSession)clientSession)) {
            return null;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        int n = NativePlayerCommit.integer(clientSession, "classicWorldMapId");
        int n2 = NativePlayerCommit.integer(clientSession, "classicWorldInstanceId");
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            CombatMonsterCurseLifecycle.Actor actor = CombatMonsterCurseLifecycle.actor((GameHub)gameHub, (int)n, (int)n2, (long)l);
            if (actor == null) {
                return null;
            }
            ClassicWorldMonsterPopulation classicWorldMonsterPopulation = actor.population();
            synchronized (classicWorldMonsterPopulation) {
                if (!((Boolean)CombatAccess.field((Object)actor.actor(), (String)"alive")).booleanValue() || !((Boolean)CombatAccess.field((Object)actor.actor(), (String)"present")).booleanValue()) {
                    return null;
                }
                return new Ref(1, l, NativeCombatAttacker.token(actor.timed(), true), CombatPoisonLifecycle.monsterEpoch((BattleTimedStatus)actor.timed(), (boolean)false), null, 0L, n, n2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Ref encounter(ClientSession clientSession, GameHub.MonsterAiTick monsterAiTick) {
        GameHub gameHub;
        int n;
        if (clientSession == null || monsterAiTick == null) {
            return null;
        }
        n = monsterAiTick.mode() == BattleRules.Mode.QUEST_BATTLE ? 3 : (monsterAiTick.mode() == BattleRules.Mode.TIME_ATTACK ? 4 : 0);
        if (n == 0) {
            return null;
        }
        GameHub gameHub2 = gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        synchronized (gameHub2) {
            Object v = ((Map)CombatAccess.field((Object)gameHub, (String)"rooms")).get(monsterAiTick.roomId());
            if (v == null) {
                return null;
            }
            Object object = CombatAccess.field(v, (String)(n == 3 ? "questEncounter" : "timeAttackEncounter"));
            if (object == null) {
                return null;
            }
            Object object2 = object;
            synchronized (object2) {
                Object object3 = CombatAccess.call((Object)object, (String)"monster", (String)("(J)Llocal/lito381/server/" + (n == 3 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter") + "$Monster;"), (Object[])new Object[]{monsterAiTick.objectId()});
                if (!NativeSourceNpcTickView.encounter((Object)object3, (GameHub.MonsterAiTick)monsterAiTick, (Object)object)) {
                    return null;
                }
                BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)object3, (String)"damagePoison");
                return new Ref(n, monsterAiTick.objectId(), NativeCombatAttacker.token(battleTimedStatus, true), 0L, null, monsterAiTick.roomId(), 0, -1, null, monsterAiTick.nativeSourceNpcTickView);
            }
        }
    }

    private static boolean same(Ref ref, Object object, BattleTimedStatus battleTimedStatus) {
        return ref.owner == NativeCombatAttacker.token(battleTimedStatus, false) && (ref.kind != 1 || ref.epoch == CombatPoisonLifecycle.monsterEpoch((BattleTimedStatus)battleTimedStatus, (boolean)false));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Live nativeRefOriginalLive(ClientSession clientSession, Ref ref) {
        if (ref == null) {
            return null;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        if (ref.kind == 2) {
            ClientSession clientSession2 = NativePlayerCommit.nativeRefCompanionCaster(clientSession, ref.master);
            if (clientSession2 == null) {
                return null;
            }
            BattleCompanion battleCompanion = (BattleCompanion)CombatAccess.call((Object)clientSession2, (String)"ownedCompanion", (String)"(J)Llocal/lito381/server/BattleCompanion;", (Object[])new Object[]{ref.id});
            if (battleCompanion == null) {
                return null;
            }
            BattleCompanion battleCompanion2 = battleCompanion;
            synchronized (battleCompanion2) {
                BattleCompanion.Snapshot snapshot = battleCompanion.snapshot();
                BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)battleCompanion, (String)"timedStatus");
                if (!snapshot.active()) return null;
                if (snapshot.hp() <= 0) return null;
                if (!NativeSourceCompanionFirstActor.live((boolean)NativeCompanionRefScene.liveSame((Ref)ref, (Object)battleCompanion, (BattleTimedStatus)battleTimedStatus, (ClientSession)clientSession2), (Ref)ref, (Object)battleCompanion)) return null;
                Live live = new Live(ref, battleCompanion, battleCompanion, battleTimedStatus, snapshot, clientSession2);
                return live;
            }
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            if (ref.kind == 1) {
                CombatMonsterCurseLifecycle.Actor actor = CombatMonsterCurseLifecycle.actor((GameHub)gameHub, (int)ref.map, (int)ref.instance, (long)ref.id);
                if (actor == null) {
                    return null;
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = actor.population();
                synchronized (classicWorldMonsterPopulation) {
                    if ((Boolean)CombatAccess.field((Object)actor.actor(), (String)"alive") == false) return null;
                    if ((Boolean)CombatAccess.field((Object)actor.actor(), (String)"present") == false) return null;
                    if (!NativeCombatAttacker.same(ref, actor.actor(), actor.timed())) {
                        return null;
                    }
                    ClassicWorldMonsterPopulation.Snapshot snapshot = actor.population().actor(ref.map, ref.id);
                    if (snapshot == null) {
                        return null;
                    }
                    Live live = new Live(ref, actor.population(), actor.actor(), actor.timed(), snapshot, null);
                    return live;
                }
            }
            Object v = ((Map)CombatAccess.field((Object)gameHub, (String)"rooms")).get(ref.room);
            if (v == null) {
                return null;
            }
            Object object = CombatAccess.field(v, (String)(ref.kind == 3 ? "questEncounter" : "timeAttackEncounter"));
            if (object == null) {
                return null;
            }
            Object object2 = object;
            synchronized (object2) {
                Object object3 = CombatAccess.call((Object)object, (String)"monster", (String)("(J)Llocal/lito381/server/" + (ref.kind == 3 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter") + "$Monster;"), (Object[])new Object[]{ref.id});
                if (object3 == null) {
                    return null;
                }
                BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)object3, (String)"damagePoison");
                if (!NativeSourceNpcTickView.live((boolean)NativeCombatAttacker.same(ref, object3, battleTimedStatus), (Ref)ref, (Object)object, (Object)object3)) {
                    return null;
                }
                Object object4 = CombatAccess.call((Object)object3, (String)"snapshot", (String)("()Llocal/lito381/server/" + (ref.kind == 3 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter") + "$Snapshot;"), (Object[])new Object[0]);
                return new Live(ref, object, object3, battleTimedStatus, object4, null);
            }
        }
    }

    static boolean alive(ClientSession clientSession, Ref ref) {
        if (NativeSourceCubePlayer.effect((Ref)ref)) {
            return NativeSourceCubePlayer.alive((ClientSession)clientSession, (Ref)ref);
        }
        return NativeCombatAttacker.live(clientSession, ref) != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int nativeCombatSceneOriginalDebit(ClientSession clientSession, Ref ref, int n) {
        if (n <= 0) {
            return 0;
        }
        Live live = NativeCombatAttacker.live(clientSession, ref);
        if (live == null) {
            return 0;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        if (ref.kind == 2) {
            Object object = live.actor;
            synchronized (object) {
                BattleCompanion battleCompanion = (BattleCompanion)live.actor;
                BattleCompanion.Snapshot snapshot = battleCompanion.snapshot();
                if (!snapshot.active() || snapshot.hp() <= 0 || !NativeCombatAttacker.same(ref, battleCompanion, live.timed)) {
                    return 0;
                }
                int n2 = Math.min(Math.max(0, snapshot.mp()), n);
                if (n2 > 0) {
                    CombatAccess.set((Object)battleCompanion, (String)"mp", (Object)(snapshot.mp() - n2));
                }
                return n2;
            }
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Live live2 = NativeCombatAttacker.live(clientSession, ref);
            if (live2 == null) {
                return 0;
            }
            Object object = live2.container;
            synchronized (object) {
                if (!NativeCombatAttacker.same(ref, live2.actor, live2.timed)) {
                    return 0;
                }
                if (ref.kind == 1) {
                    ClassicWorldMonsterPopulation.ManaDrain manaDrain = ((ClassicWorldMonsterPopulation)live2.container).drainMana(ref.map, ref.id, n);
                    int n3 = manaDrain == null ? 0 : manaDrain.actualDrain();
                    return n3;
                }
                int n4 = ((Number)CombatAccess.field((Object)live2.snapshot, (String)"maximumMp")).intValue();
                Object manaDrain = ref.kind == 3 ? (Object)gameHub.drainQuestMonsterMana(ref.room, clientSession, ref.id, n4, n) : (Object)gameHub.drainTimeAttackActorMana(ref.room, clientSession, ref.id, n4, n);
                return manaDrain == null ? 0 : ((Number)CombatAccess.field((Object)manaDrain, (String)"drained")).intValue();
            }
        }
    }

    private static List<ClientSession> viewers(ClientSession clientSession, Ref ref) {
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        return ref.kind == 1 ? gameHub.classicWorldMembers(ref.map, ref.instance) : (ref.master != null && ref.master.phase().equals("WORLD") ? gameHub.classicWorldMembers(ref.map, ref.instance) : gameHub.roomMembers(ref.room));
    }

    static void effect(ClientSession clientSession, Ref ref, int n, String string) {
        Live live = NativeCombatAttacker.live(clientSession, ref);
        if (live == null) {
            return;
        }
        byte[] byArray = Protocol.effectObject((long)ref.id, (int)n);
        for (ClientSession clientSession2 : NativeCombatAttacker.viewers(clientSession, ref)) {
            if (!NativePlayerCommit.validFrame(clientSession) || !NativeCombatAttacker.alive(clientSession, ref)) break;
            NativePlayerCommit.safeSend(clientSession2, byArray, string + " npc=" + ref.id);
        }
    }

    static Feedback feedback(ClientSession clientSession, Ref ref) {
        if (NativeSourceCubePlayer.effect((Ref)ref)) {
            return null;
        }
        Live live = NativeCombatAttacker.live(clientSession, ref);
        if (live == null) {
            return null;
        }
        int n = ref.kind == 1 ? ((ClassicWorldMonsterPopulation.Snapshot)live.snapshot).template().level() : ((Number)CombatAccess.field((Object)live.snapshot, (String)"level")).intValue();
        boolean bl = ref.kind == 1 && ((ClassicWorldMonsterPopulation.Snapshot)live.snapshot).template().boss();
        BattleMortalBody.Decision decision = CombatProbability.npcMortalDecision((ClientSession)clientSession, (int)n, (boolean)bl, (Object[])new Object[]{live.master, live.actor, live.snapshot});
        BattleJoyOfPainStatus battleJoyOfPainStatus = (BattleJoyOfPainStatus)CombatAccess.field((Object)live.actor, (String)"joyOfPainStatus");
        BattleJoyOfPainStatus.Lease lease = battleJoyOfPainStatus.capture(System.nanoTime());
        int n2 = lease == null ? 0 : BattleJoyOfPain.feedbackDamage((int)NativePlayerCommit.integer(clientSession, "playerMaxHp"), (int)NativePlayerCommit.integer(clientSession, "playerHp"), (int)CombatAccess.integerCall((Object)clientSession, (String)"joyOfPainEquipmentAddedHp", (String)"()I", (Object[])new Object[0]));
        return new Feedback(decision, lease, n2);
    }

    static void reaction(ClientSession clientSession, Ref ref, String string) {
        int n;
        if (NativeSourceCubePlayer.effect((Ref)ref)) {
            return;
        }
        Live live = NativeCombatAttacker.live(clientSession, ref);
        if (live == null) {
            return;
        }
        int n2 = n = ref.kind == 1 ? ((ClassicWorldMonsterPopulation.Snapshot)live.snapshot).gfxId() : ((Number)CombatAccess.field((Object)live.snapshot, (String)"gfx")).intValue();
        if (!BattleHitReactionSuppressCatalog.shouldSendDamageAction((int)n)) {
            return;
        }
        for (ClientSession clientSession2 : NativeCombatAttacker.viewers(clientSession, ref)) {
            if (!NativePlayerCommit.validFrame(clientSession) || !NativeCombatAttacker.alive(clientSession, ref)) break;
            NativePlayerCommit.safeSend(clientSession2, Protocol.objectAction((long)ref.id, (int)2), "S_ACTION " + string + " NPC=" + ref.id);
        }
    }

    static void nativeRawOriginalFeedback(ClientSession clientSession, Ref ref, Feedback feedback) {
        if (feedback == null || !NativePlayerCommit.validFrame(clientSession) || !NativeCombatAttacker.alive(clientSession, ref)) {
            return;
        }
        if (feedback.mortal != null && feedback.mortal.triggered()) {
            try {
                NativeCombatAttacker.reaction(clientSession, ref, "mortal-body");
                NativeCombatAttacker.damage(clientSession, ref, feedback.mortal.reflectedDamage(), "mortal-body");
            }
            catch (RuntimeException runtimeException) {
                NativePlayerCommit.secondaryFailure("NPC Mortal", runtimeException);
            }
        }
        if (feedback.joy != null && NativePlayerCommit.validFrame(clientSession)) {
            Live live = NativeCombatAttacker.live(clientSession, ref);
            if (live == null) {
                return;
            }
            BattleJoyOfPainStatus battleJoyOfPainStatus = (BattleJoyOfPainStatus)CombatAccess.field((Object)live.actor, (String)"joyOfPainStatus");
            if (battleJoyOfPainStatus.consume(feedback.joy) && feedback.joyDamage > 0) {
                try {
                    NativeCombatAttacker.reaction(clientSession, ref, "joy-of-pain");
                    NativeCombatAttacker.damage(clientSession, ref, feedback.joyDamage, "joy-of-pain");
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.secondaryFailure("NPC Joy", runtimeException);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void nativeReflectionOriginalDamage(ClientSession clientSession, Ref ref, int n, String string) {
        if (n <= 0 || !NativePlayerCommit.validFrame(clientSession)) {
            return;
        }
        Live live = NativeCombatAttacker.live(clientSession, ref);
        if (live == null) {
            return;
        }
        if (ref.kind == 1) {
            GameHub gameHub;
            GameHub gameHub2 = gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
            synchronized (gameHub2) {
                CombatMonsterCurseLifecycle.Actor actor = CombatMonsterCurseLifecycle.actor((GameHub)gameHub, (int)ref.map, (int)ref.instance, (long)ref.id);
                if (actor == null) {
                    return;
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = actor.population();
                synchronized (classicWorldMonsterPopulation) {
                    BattleTimedStatus battleTimedStatus = actor.timed();
                    synchronized (battleTimedStatus) {
                        if (!(NativeCombatAttacker.same(ref, actor.actor(), actor.timed()) && actor.valid(ref.epoch) && NativePlayerCommit.validFrame(clientSession))) {
                            return;
                        }
                        int n2 = NativePlayerCommit.integer(actor.actor(), "hiddenStatus");
                        if (n2 == 1 || n2 == 2) {
                            return;
                        }
                        NativePlayerCommit.Binding binding = NativePlayerCommit.frameBinding(clientSession);
                        CombatMonsterCurseLifecycle.enqueue((CombatMonsterCurseLifecycle.Actor)actor, (CombatMonsterCurseLifecycle.Recipient)new CombatMonsterCurseLifecycle.Recipient(binding.object(), binding.source(), binding.account(), binding.selected()), (long)ref.epoch, (int)n, (CombatWorldCommit.Plan)new Reflection(ref, binding, string));
                    }
                }
            }
            return;
        }
        Ref ref2 = reflecting.get();
        reflecting.set(ref);
        try {
            if (ref.kind == 2) {
                CombatAccess.call((Object)clientSession, (String)"applyCompanionDefenseFeedbackDamage", (String)"(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)V", (Object[])new Object[]{live.master, live.actor, n, NativeCombatAttacker.viewers(clientSession, ref), string});
            } else {
                NativeSourceRefRawNpc.call((Object)clientSession, (String)(ref.kind == 3 ? "applyQuestMonsterDamage" : "applyTimeAttackActorDamage"), (String)"(JILjava/lang/String;)I", (Object[])new Object[]{ref.id, n, string}, (Ref)ref, (Object)live);
            }
        }
        finally {
            if (ref2 == null) {
                reflecting.remove();
            } else {
                reflecting.set(ref2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static BattleCompanion.Damage nativeReflectionOriginalCompanionDamage(BattleCompanion battleCompanion, int n) {
        Ref ref = reflecting.get();
        if (ref == null) {
            return battleCompanion.applyDamage(n);
        }
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)battleCompanion, (String)"timedStatus");
            return ref.kind == 2 && ref.id == battleCompanion.snapshot().objectId() && NativeCombatAttacker.same(ref, battleCompanion, battleTimedStatus) ? battleCompanion.applyDamage(n) : new BattleCompanion.Damage(battleCompanion.snapshot(), battleCompanion.snapshot(), 0, false);
        }
    }

    static boolean companionEarth(BattleCompanion battleCompanion, long l) {
        Ref ref = reflecting.get();
        if (ref != null && ref.kind == 2 && ref.id == battleCompanion.snapshot().objectId() && NativeCombatAttacker.same(ref, battleCompanion, (BattleTimedStatus)CombatAccess.field((Object)battleCompanion, (String)"timedStatus"))) {
            return false;
        }
        return battleCompanion.earthBindActive(l);
    }

    static boolean earth(boolean bl, Object object) {
        if (NativeSourceCubeNpc.rawEffectTarget((Object)object)) {
            return false;
        }
        if (NativeProcHealingEncounter.direct(object)) {
            return false;
        }
        Ref ref = reflecting.get();
        if (ref == null || ref.kind != 3 && ref.kind != 4) {
            return bl;
        }
        BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field((Object)object, (String)"damagePoison");
        return NativeCombatAttacker.same(ref, object, battleTimedStatus) ? false : bl;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static BattleQuestEncounter.Hit questDamage(GameHub gameHub, long l, ClientSession clientSession, long l2, int n) {
        Ref ref = reflecting.get();
        if (ref == null) {
            return gameHub.damageQuestMonster(l, clientSession, l2, n);
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Live live = NativeCombatAttacker.live(clientSession, ref);
            if (ref.kind != 3 || ref.id != l2 || ref.room != l || live == null || !NativePlayerCommit.validFrame(clientSession)) {
                return null;
            }
            Object object = live.container;
            synchronized (object) {
                return NativeCombatAttacker.same(ref, live.actor, live.timed) ? gameHub.damageQuestMonster(l, clientSession, l2, n) : null;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static GameHub.TimeAttackActorHit timeDamage(GameHub gameHub, long l, ClientSession clientSession, long l2, int n) {
        Ref ref = reflecting.get();
        if (ref == null) {
            return gameHub.damageTimeAttackActor(l, clientSession, l2, n);
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Live live = NativeCombatAttacker.live(clientSession, ref);
            if (ref.kind != 4 || ref.id != l2 || ref.room != l || live == null || !NativePlayerCommit.validFrame(clientSession)) {
                return null;
            }
            Object object = live.container;
            synchronized (object) {
                return NativeCombatAttacker.same(ref, live.actor, live.timed) ? gameHub.damageTimeAttackActor(l, clientSession, l2, n) : null;
            }
        }
    }

    static ClientSession owner(ClientSession clientSession, CombatMonsterCurseLifecycle.Context context) {
        CombatWorldCommit.Plan plan;
        if (NativeProcHealingWorld.handles((CombatMonsterCurseLifecycle.Context)context)) {
            return NativeProcHealingWorld.owner((ClientSession)clientSession, (CombatMonsterCurseLifecycle.Context)context);
        }
        if (context != null && context.event != null && (plan = context.event.physical()) instanceof Reflection) {
            Reflection reflection = (Reflection)plan;
            return clientSession != null && reflection.owner.matches(clientSession) ? clientSession : null;
        }
        return clientSession;
    }

    static void publish(ClientSession clientSession, ClassicWorldMonsterPopulation.PoisonUpdate poisonUpdate, CombatPoisonLifecycle.Scope scope, Reflection reflection) {
        CombatAccess.call((Object)clientSession, (String)"sourceWorldDollCommitted", (String)"(Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;Ljava/lang/String;JLlocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V", (Object[])new Object[]{poisonUpdate.hits().get(0), reflection.source, poisonUpdate.target().objectId(), poisonUpdate, scope});
    }

    static Ref companion(ClientSession clientSession, BattleCompanion battleCompanion) {
        return NativeCompanionRefScene.capture((ClientSession)clientSession, (BattleCompanion)battleCompanion);
    }

    private static Live live(ClientSession clientSession, Ref ref) {
        if (NativeCompanionRefScene.worldRef((Ref)ref)) {
            GameHub gameHub = NativeCompanionRefScene.hub((ClientSession)clientSession);
            synchronized (gameHub) {
                Live live = NativeCombatAttacker.nativeRefOriginalLive(clientSession, ref);
                return live;
            }
        }
        return NativeCombatAttacker.nativeRefOriginalLive(clientSession, ref);
    }

    static int debit(ClientSession clientSession, Ref ref, int n) {
        return NativeCompanionCombatScene.debit((ClientSession)clientSession, (Ref)ref, (int)n);
    }

    static void damage(ClientSession clientSession, Ref ref, int n, String string) {
        NativeCompanionReflectionScene.damage((ClientSession)clientSession, (Ref)ref, (int)n, (String)string);
    }

    static BattleCompanion.Damage companionDamage(BattleCompanion battleCompanion, int n) {
        return NativeCompanionReflectionScene.companionDamage((BattleCompanion)battleCompanion, (int)n);
    }

    static Ref nativeReflectionRef() {
        return reflecting.get();
    }

    static void acceptedFeedback(ClientSession clientSession, Ref ref, Feedback feedback) {
        if (NativeSourceCubePlayer.effect((Ref)ref)) {
            return;
        }
        NativeSourceRawDamage.feedback((ClientSession)clientSession, (Ref)ref, (Feedback)feedback);
    }

    static Ref nativeSourceCubeCurrent() {
        return reflecting.get();
    }

    static Object nativeSourceNpcReactionLive(ClientSession clientSession, Ref ref) {
        return NativeCombatAttacker.live(clientSession, ref);
    }

    static Object nativeSourceNpcPlayerCurrent() {
        Ingress ingress = NativeCombatAttacker.ingress.get();
        if (ingress != null) {
            return ingress.nativeSourceNpcPlayerScope;
        }
        return null;
    }

    static Ingress nativeSourceCompanionClockEnter(ClientSession clientSession, BattleCompanion battleCompanion, Object object) {
        if (!CombatRules.loaded()) {
            return (Ingress)NativeSourceCompanionClock.attach(null, (ClientSession)clientSession, null, (BattleCompanion)battleCompanion, (Object)object);
        }
        return (Ingress)NativeSourceCompanionClock.attach((Object)NativeCombatAttacker.enter(NativeSourceCompanionClock.ref((ClientSession)clientSession, (BattleCompanion)battleCompanion, (Object)object)), (ClientSession)clientSession, null, (BattleCompanion)battleCompanion, (Object)object);
    }

    static Ingress nativeSourceCompanionFirstEnter(ClientSession clientSession, BattleCompanion battleCompanion, Object object, Object object2) {
        if (!CombatRules.loaded()) {
            return (Ingress)NativeSourceCompanionClock.attach(null, (ClientSession)clientSession, null, (BattleCompanion)battleCompanion, (Object)object);
        }
        return (Ingress)NativeSourceCompanionClock.attach((Object)NativeCombatAttacker.enter(NativeSourceCompanionFirstActor.ref((ClientSession)clientSession, (BattleCompanion)battleCompanion, (Object)object, (Object)object2)), (ClientSession)clientSession, null, (BattleCompanion)battleCompanion, (Object)object);
    }

    record Ref(int kind, long id, Object owner, long epoch, NativePlayerCommit.Binding master, long room, int map, int instance) {
        Ref(int n, long l, Object object, long l2, NativePlayerCommit.Binding binding, long l3, int n2, int n3, Object object2) {
            this(n, l, object, l2, binding, l3, n2, n3);
        }

        Ref(int n, long l, Object object, long l2, NativePlayerCommit.Binding binding, long l3, int n2, int n3, Object object2, Object object3) {
            this(n, l, object, l2, binding, l3, n2, n3);
        }
    }

    static final class Ingress {
        final Ref attacker;
        final Ingress previous;
        final boolean blocked;
        NativePlayerCommit.Binding lastTarget;
        ClassicWorldMonsterPopulation.Snapshot snapshot;
        Object nativeSourceNpcPlayerScope;

        Ingress(Ref ref, Ingress ingress, boolean bl) {
            this.attacker = ref;
            this.previous = ingress;
            this.blocked = bl;
        }
    }

    private record Live(Ref ref, Object container, Object actor, BattleTimedStatus timed, Object snapshot, ClientSession master) {
    }

    record Feedback(BattleMortalBody.Decision mortal, BattleJoyOfPainStatus.Lease joy, int joyDamage) {
    }

    record Reflection(Ref target, NativePlayerCommit.Binding owner, String source) implements CombatWorldCommit.Plan
    {
    }
}

