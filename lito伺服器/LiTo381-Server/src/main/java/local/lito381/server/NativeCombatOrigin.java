// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPoisonAttack;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.CombatRules;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativePlayerCommit;

final class NativeCombatOrigin {
    private static final String FIELD = "sourceCombatOrigin";
    private static final MethodHandle NPC_STATE = NativeCombatOrigin.npcStateHandle();
    private static final ThreadLocal<Install> installing = new ThreadLocal();
    private static final ThreadLocal<Clock> clock = new ThreadLocal();

    NativeCombatOrigin() {
    }

    private static MethodHandle npcStateHandle() {
        try {
            return MethodHandles.privateLookupIn(CombatPoisonAtomic.class, MethodHandles.lookup()).findStatic(CombatPoisonAtomic.class, "nativeState", MethodType.methodType(CombatPoisonAtomic.NpcState.class, ClientSession.class, Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Long.TYPE, String.class));
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
    }

    static void tickCreated(GameHub.MonsterAiTick monsterAiTick) {
        if (!CombatRules.loaded()) {
            return;
        }
        ClientSession clientSession = monsterAiTick.target();
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativeCombatAttacker.Ref ref = clientSession == null ? null : NativeCombatAttacker.encounter(clientSession, monsterAiTick);
        CombatAccess.set(monsterAiTick, FIELD, new Tick(ref, binding));
    }

    static Tick tick(GameHub.MonsterAiTick monsterAiTick) {
        return monsterAiTick == null ? null : (Tick)CombatAccess.field(monsterAiTick, FIELD);
    }

    static boolean validTick(ClientSession clientSession, GameHub.MonsterAiTick monsterAiTick) {
        if (!CombatRules.loaded() || monsterAiTick == null) {
            return true;
        }
        Tick tick = NativeCombatOrigin.tick(monsterAiTick);
        return tick != null && tick.target != null && tick.target.matches(monsterAiTick.target()) && tick.attacker != null && NativeCombatAttacker.alive(clientSession, tick.attacker);
    }

    static NativeCombatAttacker.Ref tickAttacker(GameHub.MonsterAiTick monsterAiTick) {
        Tick tick = NativeCombatOrigin.tick(monsterAiTick);
        return tick == null ? null : tick.attacker;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static ClassicWorldMonsterPopulation.Snapshot worldSnapshot(ClassicWorldMonsterPopulation.Snapshot snapshot, Object object) {
        if (CombatRules.loaded()) {
            BattleTimedStatus battleTimedStatus;
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus = (BattleTimedStatus)CombatAccess.field(object, "damagePoisonStatus");
            synchronized (battleTimedStatus2) {
                CombatAccess.set(snapshot, FIELD, new Life(NativeCombatAttacker.token(battleTimedStatus, true), CombatPoisonLifecycle.monsterEpoch(battleTimedStatus, false)));
            }
        }
        return snapshot;
    }

    static NativeCombatAttacker.Ref worldRef(ClientSession clientSession, Object object) {
        if (object == null) {
            return null;
        }
        ClassicWorldMonsterPopulation.Snapshot snapshot = (ClassicWorldMonsterPopulation.Snapshot)CombatAccess.field(object, "attacker");
        Life life = (Life)CombatAccess.field(snapshot, FIELD);
        if (life == null) {
            return null;
        }
        return new NativeCombatAttacker.Ref(1, snapshot.objectId(), life.owner, life.epoch, null, 0L, snapshot.mapId(), NativePlayerCommit.integer(clientSession, "classicWorldInstanceId"));
    }

    static boolean validWorldAction(ClientSession clientSession, Object object) {
        if (!CombatRules.loaded() || object == null) {
            return true;
        }
        NativeCombatAttacker.Ref ref = NativeCombatOrigin.worldRef(clientSession, object);
        if (ref != null) {
            return NativeCombatAttacker.alive(clientSession, ref);
        }
        ClassicWorldMonsterPopulation.Snapshot snapshot = (ClassicWorldMonsterPopulation.Snapshot)CombatAccess.field(object, "attacker");
        return NativeCombatAttacker.world(clientSession, snapshot.objectId()) == null;
    }

    static NativeCombatAttacker.Ingress enterWorldAction(ClientSession clientSession, Object object) {
        NativeCombatAttacker.Ingress ingress;
        if (!CombatRules.loaded()) {
            return null;
        }
        NativeCombatAttacker.Ref ref = NativeCombatOrigin.worldRef(clientSession, object);
        NativeCombatAttacker.Ingress ingress2 = ingress = ref == null ? NativeCombatAttacker.enterPoison() : NativeCombatAttacker.enterKnown(ref);
        if (object != null) {
            ingress.snapshot = (ClassicWorldMonsterPopulation.Snapshot)CombatAccess.field(object, "attacker");
        }
        return ingress;
    }

    static void leaveWorldAction(NativeCombatAttacker.Ingress ingress) {
        NativeCombatAttacker.leave(ingress);
    }

    static Install enterNpc(ClientSession clientSession, long l) {
        ClassicWorldMonsterPopulation.Snapshot snapshot;
        if (!CombatRules.loaded()) {
            return null;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativeCombatAttacker.Ref ref = NativeCombatAttacker.current();
        if (ref != null && ref.id() != l) {
            ref = null;
        }
        if (ref == null && !NativeCombatAttacker.blocked()) {
            ref = NativeCombatAttacker.world(clientSession, l);
        }
        if ((snapshot = NativeCombatAttacker.capturedSnapshot(l)) == null && ref != null && ref.kind() == 1) {
            snapshot = NativeCombatAttacker.worldSnapshot(clientSession, ref);
        }
        return NativeCombatOrigin.install(new Poison(binding, null, ref, snapshot));
    }

    static Install enterPlayer(ClientSession clientSession, ClientSession clientSession2) {
        if (!CombatRules.loaded()) {
            return null;
        }
        return NativeCombatOrigin.install(new Poison(NativePlayerCommit.Binding.capture(clientSession), NativePlayerCommit.Binding.capture(clientSession2), null, null));
    }

    private static Install install(Poison poison) {
        Install install = new Install(poison, installing.get());
        installing.set(install);
        return install;
    }

    static void leaveInstall(Install install) {
        if (install == null) {
            return;
        }
        if (installing.get() != install) {
            throw new IllegalStateException("poison installation scope mismatch");
        }
        if (install.previous == null) {
            installing.remove();
        } else {
            installing.set(install.previous);
        }
    }

    static boolean validInstall(ClientSession clientSession) {
        Install install = installing.get();
        return install == null || install.value.target.matches(clientSession);
    }

    static CombatPoisonAtomic.NpcState npcState(ClientSession clientSession, long l, int n, int n2, int n3, long l2, String string) {
        if (!NativeCombatOrigin.validInstall(clientSession)) {
            return new CombatPoisonAtomic.NpcState(ClassicWorldMonsterPoisonAttack.Kind.NONE, 0, false, false, false);
        }
        try {
            return (CombatPoisonAtomic.NpcState) NPC_STATE.invokeExact(clientSession, l, n, n2, n3, l2, string);
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("native NPC poison state failed", throwable);
        }
    }

    static boolean activatePlayer(BattleTimedStatus battleTimedStatus, long l, int n, int n2, ClientSession clientSession) {
        return NativeCombatOrigin.validInstall(clientSession) && battleTimedStatus.activatePoison(l, n, n2);
    }

    static void leaseCreated(Object object) {
        if (!CombatRules.loaded()) {
            return;
        }
        Install install = installing.get();
        if (install != null) {
            CombatAccess.set(object, FIELD, install.value);
        }
    }

    static Poison lease(Object object) {
        return object == null ? null : (Poison)CombatAccess.field(object, FIELD);
    }

    static boolean contextValid(boolean bl, Object object) {
        Poison poison;
        if (!bl || !CombatRules.loaded()) {
            return bl;
        }
        Object object2 = CombatAccess.field(object, "clock");
        if (object2 == null) {
            object2 = CombatAccess.field(object, "producer");
        }
        return (poison = NativeCombatOrigin.lease(object2)) == null || poison.target.matches((ClientSession)CombatAccess.field(object, "pc"));
    }

    static ClientSession caster(ClientSession clientSession, Object object) {
        Poison poison = NativeCombatOrigin.lease(CombatAccess.field(object, "clock"));
        return poison == null || poison.player == null ? clientSession : (clientSession != null && poison.player.matches(clientSession) ? clientSession : null);
    }

    static Clock enterClock(ClientSession clientSession, CombatPoisonLifecycle.Scope scope) {
        if (!CombatRules.loaded()) {
            return null;
        }
        Object object = scope == null ? null : CombatAccess.call(scope, "find", "(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/CombatPoisonLifecycle$Context;", clientSession);
        Poison poison = object == null ? null : NativeCombatOrigin.lease(CombatAccess.field(object, "clock"));
        Clock clock = new Clock(poison, NativeCombatOrigin.clock.get(), NativeCombatAttacker.enterPoison());
        NativeCombatOrigin.clock.set(clock);
        return clock;
    }

    static void leaveClock(Clock clock) {
        if (clock == null) {
            return;
        }
        if (NativeCombatOrigin.clock.get() != clock) {
            throw new IllegalStateException("poison clock scope mismatch");
        }
        try {
            if (clock.previous == null) {
                NativeCombatOrigin.clock.remove();
            } else {
                NativeCombatOrigin.clock.set(clock.previous);
            }
        }
        finally {
            NativeCombatAttacker.leave(clock.ingress);
        }
    }

    static Poison poison() {
        Clock clock = NativeCombatOrigin.clock.get();
        return clock == null ? null : clock.value;
    }

    static boolean poisonClock() {
        return clock.get() != null;
    }

    static boolean idle() {
        return installing.get() == null && clock.get() == null;
    }

    record Tick(NativeCombatAttacker.Ref attacker, NativePlayerCommit.Binding target) {
    }

    record Life(Object owner, long epoch) {
    }

    record Poison(NativePlayerCommit.Binding target, NativePlayerCommit.Binding player, NativeCombatAttacker.Ref npc, ClassicWorldMonsterPopulation.Snapshot snapshot) {
    }

    static final class Install {
        final Poison value;
        final Install previous;

        Install(Poison poison, Install install) {
            this.value = poison;
            this.previous = install;
        }
    }

    static final class Clock {
        final Poison value;
        final Clock previous;
        final NativeCombatAttacker.Ingress ingress;

        Clock(Poison poison, Clock clock, NativeCombatAttacker.Ingress ingress) {
            this.value = poison;
            this.previous = clock;
            this.ingress = ingress;
        }
    }
}
