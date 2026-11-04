// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.BattleAbilityStatus
 *  local.lito381.server.BattleAbilityStatus$IllusionAvatar
 *  local.lito381.server.BattleCompanion
 *  local.lito381.server.BattleCompanion$Snapshot
 *  local.lito381.server.BattleJoyOfPain
 *  local.lito381.server.BattleJoyOfPainStatus
 *  local.lito381.server.BattleJoyOfPainStatus$Lease
 *  local.lito381.server.BattleTimedStatus
 *  local.lito381.server.ClientSession
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.CombatIncomingSource
 *  local.lito381.server.CombatPoisonAtomic
 *  local.lito381.server.CombatPoisonLifecycle
 *  local.lito381.server.CombatPoisonLifecycle$Scope
 *  local.lito381.server.CombatPoisonLifecycle$State
 *  local.lito381.server.CombatRules
 *  local.lito381.server.GameHub
 *  local.lito381.server.NativeAcceptedFreezeMutation
 *  local.lito381.server.NativeAcceptedState
 *  local.lito381.server.NativeAcceptedState$Incoming
 *  local.lito381.server.NativeAcceptedState$Stale
 *  local.lito381.server.NativeCompanionManaTransfer
 *  local.lito381.server.NativeCompanionRefScene
 *  local.lito381.server.NativeCompanionRoleIdentity
 *  local.lito381.server.NativeDeClientViewer
 *  local.lito381.server.NativeDeItemApplication
 *  local.lito381.server.NativeEffectCompletions
 *  local.lito381.server.NativeHpCompletionAdmission
 *  local.lito381.server.NativeMagicDoll
 *  local.lito381.server.NativeMagicDollEffects$DefenseProc
 *  local.lito381.server.NativePlayerVitals
 *  local.lito381.server.NativePlayerVitals$Result
 *  local.lito381.server.NativeProcPlayerState
 *  local.lito381.server.NativeSourceCubePlayer
 *  local.lito381.server.NativeSourceDamageCapacity
 *  local.lito381.server.NativeSourceDamageDisplay
 *  local.lito381.server.NativeSourceDamageDisplay$Plan
 *  local.lito381.server.NativeSourceDollDefense
 *  local.lito381.server.NativeSourceEquipmentProcLife$Proof
 *  local.lito381.server.NativeSourceFatalityDamage
 *  local.lito381.server.NativeSourceGroundQueued
 *  local.lito381.server.NativeSourceLightningHp
 *  local.lito381.server.NativeSourceManualScope
 *  local.lito381.server.NativeSourcePendingAdmission
 *  local.lito381.server.NativeSourceProc68FirstQueue
 *  local.lito381.server.NativeSourceProcScalarSkills
 *  local.lito381.server.NativeSourceRawDamage
 *  local.lito381.server.NativeStaticNpcMotion
 *  local.lito381.server.Protocol
 *  local.lito381.server.SourceMagicDollPower
 */
package local.lito381.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import local.lito381.server.BattleAbilityStatus;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleJoyOfPain;
import local.lito381.server.BattleJoyOfPainStatus;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatIncomingSource;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.CombatRules;
import local.lito381.server.GameHub;
import local.lito381.server.NativeAcceptedFreezeMutation;
import local.lito381.server.NativeAcceptedHpCompletion;
import local.lito381.server.NativeAcceptedState;
import local.lito381.server.NativeArmorProc;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeCombatOrigin;
import local.lito381.server.NativeCompanionManaTransfer;
import local.lito381.server.NativeCompanionRefScene;
import local.lito381.server.NativeCompanionRoleIdentity;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeEffectCompletions;
import local.lito381.server.NativeEquipmentDefense;
import local.lito381.server.NativeHpCompletionAdmission;
import local.lito381.server.NativeMagicDoll;
import local.lito381.server.NativeMagicDollEffects;
import local.lito381.server.NativeMassTeleportAnswerLife;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativePlayerVitals;
import local.lito381.server.NativeProcPlayerState;
import local.lito381.server.NativeProcSkillCatalog;
import local.lito381.server.NativeSourceCubePlayer;
import local.lito381.server.NativeSourceDamageCapacity;
import local.lito381.server.NativeSourceDamageDisplay;
import local.lito381.server.NativeSourceDollDefense;
import local.lito381.server.NativeSourceEquipmentProcLife;
import local.lito381.server.NativeSourceFatalityDamage;
import local.lito381.server.NativeSourceGroundQueued;
import local.lito381.server.NativeSourceLightningHp;
import local.lito381.server.NativeSourceManualScope;
import local.lito381.server.NativeSourcePendingAdmission;
import local.lito381.server.NativeSourceProc68FirstQueue;
import local.lito381.server.NativeSourceProcScalarSkills;
import local.lito381.server.NativeSourceRawDamage;
import local.lito381.server.NativeStaticNpcMotion;
import local.lito381.server.Protocol;
import local.lito381.server.SourceMagicDollPower;

final class NativePlayerCommit {
    private static final String P = "local/lito381/server/";
    private static final String C = "Llocal/lito381/server/ClientSession;";
    private static final String JOY = "Llocal/lito381/server/BattleJoyOfPainStatus$Lease;";
    static final int TARGET = Math.max(64, Math.min(65536, Integer.getInteger("lineage.damage.maxTargetPending", 4096)));
    static final int CORES = Math.max(1, Runtime.getRuntime().availableProcessors());
    static final int WORKERS = Math.max(1, Math.min(Math.min(4, CORES), Integer.getInteger("lineage.damage.commitWorkers", Math.max(1, Math.min(4, CORES / 2)))));
    static final int CAPACITY = Math.max(256, Math.min(65536, Integer.getInteger("lineage.damage.commitDispatchCapacity", 8192)));
    static final long GLOBAL = Math.max((long)TARGET, Math.min(0x100000L, Long.getLong("lineage.damage.maxGlobalPending", 65536L)));
    private static final AtomicLong pending = new AtomicLong();
    private static final AtomicLong committed = new AtomicLong();
    private static final AtomicLong dropped = new AtomicLong();
    private static final AtomicLong rejected = new AtomicLong();
    private static final AtomicLong failed = new AtomicLong();
    private static final AtomicLong retries = new AtomicLong();
    private static final AtomicLong repairs = new AtomicLong();
    private static final AtomicLong defenseRolls = new AtomicLong();
    private static final AtomicLong defenseAttempts = new AtomicLong();
    private static final AtomicLong effectFailures = new AtomicLong();
    private static final AtomicLong defenseCalcFailures = new AtomicLong();
    private static final ThreadLocal<Frame> current = new ThreadLocal();
    private static final ThreadLocal<WorldPlan> worldCalculation = new ThreadLocal();
    private static final ThreadLocal<CalcTicket> calculation = new ThreadLocal();

    NativePlayerCommit() {
    }

    static int integer(Object object, String string) {
        return ((Number)CombatAccess.field((Object)object, (String)string)).intValue();
    }

    static long number(Object object, String string) {
        return ((Number)CombatAccess.field((Object)object, (String)string)).longValue();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Object owner(ClientSession clientSession) {
        BattleTimedStatus battleTimedStatus;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
        synchronized (battleTimedStatus2) {
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)true);
            Object object = CombatAccess.field((Object)state, (String)"sourcePlayerOwner");
            if (object == null) {
                object = NativeCompanionRoleIdentity.nextRole((ClientSession)clientSession);
                CombatAccess.set((Object)state, (String)"sourcePlayerOwner", (Object)object);
            }
            return object;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    static void nativeStaticOriginalAdvance(ClientSession clientSession) {
        CombatPoisonLifecycle.State state;
        NativePlayerActionScope.Ticket ticket;
        ReentrantLock reentrantLock;
        block13: {
            BattleTimedStatus battleTimedStatus;
            block12: {
                reentrantLock = clientSession.nativePlayerActionLock();
                ticket = NativeSourceManualScope.lock((ReentrantLock)reentrantLock, (ClientSession)clientSession);
                try {
                    battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
                    if (battleTimedStatus != null) break block12;
                }
                catch (Throwable throwable) {
                    NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, (Throwable)throwable);
                    throw throwable;
                }
                NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
                return;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            // MONITORENTER : battleTimedStatus2
            NativeDeClientViewer.retire((BattleTimedStatus)battleTimedStatus);
            state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)false);
            if (state != null) break block13;
            // MONITOREXIT : battleTimedStatus2
            NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
            return;
        }
        Queue queue = (Queue)CombatAccess.field((Object)state, (String)"sourcePlayerQueue");
        if (queue != null) {
            queue.vitalLock.lock();
            try {
                queue.drop(true);
                CombatAccess.set((Object)state, (String)"sourcePlayerQueue", null);
                CombatAccess.set((Object)state, (String)"sourcePlayerOwner", (Object)NativeCompanionRoleIdentity.nextRole((ClientSession)clientSession));
            }
            finally {
                queue.vitalLock.unlock();
            }
        } else {
            CombatAccess.set((Object)state, (String)"sourcePlayerOwner", (Object)NativeCompanionRoleIdentity.nextRole((ClientSession)clientSession));
        }
        // MONITOREXIT : battleTimedStatus2
        NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void assign(ClientSession clientSession, Object object, String string) {
        BattleTimedStatus battleTimedStatus;
        NativePlayerActionScope.Ticket ticket;
        ReentrantLock reentrantLock;
        block6: {
            reentrantLock = clientSession.nativePlayerActionLock();
            ticket = NativeSourceManualScope.lock((ReentrantLock)reentrantLock, (ClientSession)clientSession);
            try {
                battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
                if (battleTimedStatus != null) break block6;
                CombatAccess.set((Object)clientSession, (String)string, (Object)object);
            }
            catch (Throwable throwable) {
                NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, (Throwable)throwable);
                throw throwable;
            }
            NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
            return;
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativePlayerCommit.advance(clientSession);
            NativeProcPlayerState.selectedField((ClientSession)clientSession, (String)string);
            CombatAccess.set((Object)clientSession, (String)string, (Object)object);
        }
        NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
    }

    static void selected(ClientSession clientSession, String string) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.lock((ReentrantLock)reentrantLock, (ClientSession)clientSession);
        try {
            NativeDeItemApplication.retireClient((ClientSession)clientSession);
            NativeMassTeleportAnswerLife.retireRole(clientSession);
            NativePlayerCommit.assign(clientSession, string, "selectedCharacter");
            clientSession.nativeSourceShowDamage = true;
            clientSession.nativeSourceShowProcSpr = true;
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, (Throwable)throwable);
            throw throwable;
        }
        NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
    }

    static void scene(ClientSession clientSession, Object object, String string) {
        NativePlayerCommit.nativeCompanionSceneAssign(clientSession, object, string);
    }

    static Frame frame() {
        return current.get();
    }

    static Binding combatBinding(ClientSession clientSession) {
        Frame frame = current.get();
        return clientSession == null ? null : (frame != null && clientSession == frame.pc ? frame.event.target : (frame != null && clientSession == frame.casterSession ? frame.event.caster : Binding.capture(clientSession)));
    }

    static CalcTicket beginCalc(ClientSession clientSession, ClientSession clientSession2, boolean bl) {
        if (!CombatRules.loaded()) {
            return null;
        }
        WorldPlan worldPlan = bl ? worldCalculation.get() : null;
        NativeCombatOrigin.Poison poison = NativeCombatOrigin.poison();
        CalcTicket calcTicket = new CalcTicket(poison != null ? poison.target() : (worldPlan == null ? NativePlayerCommit.combatBinding(clientSession) : worldPlan.target), poison != null ? poison.player() : (worldPlan == null ? NativePlayerCommit.combatBinding(clientSession2) : worldPlan.caster), worldPlan == null ? NativeCombatAttacker.current() : worldPlan.attacker, calculation.get());
        calculation.set(calcTicket);
        NativeCombatAttacker.calculated(calcTicket.target);
        return calcTicket;
    }

    static void endCalc(CalcTicket calcTicket) {
        if (calcTicket == null) {
            return;
        }
        if (calculation.get() != calcTicket) {
            throw new IllegalStateException("native Calc scope ownership");
        }
        if (calcTicket.previous == null) {
            calculation.remove();
        } else {
            calculation.set(calcTicket.previous);
        }
    }

    static boolean calculationIdle() {
        return calculation.get() == null && worldCalculation.get() == null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Queue queue(ClientSession clientSession, Binding binding) {
        BattleTimedStatus battleTimedStatus;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
        synchronized (battleTimedStatus2) {
            if (!binding.matches(clientSession)) {
                return null;
            }
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)true);
            Queue queue = (Queue)CombatAccess.field((Object)state, (String)"sourcePlayerQueue");
            if (queue == null || !queue.owner.equals(binding)) {
                if (queue != null) {
                    queue.drop(true);
                }
                queue = new Queue(binding, NativePlayerLifetime.of(clientSession), clientSession.nativePlayerActionLock());
                CombatAccess.set((Object)state, (String)"sourcePlayerQueue", (Object)queue);
            }
            return queue;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean earlyZero(ClientSession clientSession, ClientSession clientSession2, boolean bl, long l) {
        WorldPlan worldPlan;
        int n;
        CalcTicket calcTicket = calculation.get();
        if (calcTicket == null || !String.valueOf(CombatAccess.field((Object)clientSession, (String)"phase")).equals(bl ? "WORLD" : "BATTLE")) {
            return false;
        }
        Queue queue = NativePlayerCommit.queue(clientSession, calcTicket.target);
        if (queue == null) {
            return true;
        }
        queue.vitalLock.lock();
        try {
            n = NativePlayerCommit.integer(clientSession, "playerHp");
            if (queue.closed || !calcTicket.target.matchesFast(clientSession)) {
                boolean bl2 = true;
                return bl2;
            }
            if (queue.terminal) {
                boolean bl3 = true;
                return bl3;
            }
            if (!bl && n <= 0 && ((Boolean)CombatAccess.field((Object)clientSession, (String)"deathConfirmationPending")).booleanValue()) {
                queue.terminal = true;
                boolean bl4 = true;
                return bl4;
            }
            if (n > 0) {
                boolean bl5 = false;
                return bl5;
            }
        }
        finally {
            queue.vitalLock.unlock();
        }
        Binding binding = calcTicket.caster;
        WorldPlan worldPlan2 = worldPlan = bl ? worldCalculation.get() : null;
        long l2 = l != 0L ? l : (binding != null ? binding.object : (calcTicket.attacker == null ? 0L : calcTicket.attacker.id()));
        ClassicWorldMonsterPopulation.Snapshot snapshot = null;
        if (bl) {
            NativeCombatOrigin.Poison poison = NativeCombatOrigin.poison();
            ClassicWorldMonsterPopulation.Snapshot snapshot2 = snapshot = poison != null ? poison.snapshot() : NativeCombatAttacker.capturedSnapshot(l2);
            if (snapshot == null && calcTicket.attacker != null && calcTicket.attacker.kind() == 1) {
                snapshot = NativeCombatAttacker.worldSnapshot(clientSession, calcTicket.attacker);
            }
            if (snapshot == null && l2 != 0L) {
                snapshot = ((GameHub)CombatAccess.field((Object)clientSession, (String)"hub")).classicWorldMonster(NativePlayerCommit.integer(clientSession, "classicWorldMapId"), NativePlayerCommit.integer(clientSession, "classicWorldInstanceId"), l2);
            }
        }
        NativePlayerCommit.offer(clientSession, new Event(calcTicket.target, binding, n, false, true, 0L, null, 0, bl, l2, snapshot, worldPlan, List.of(), false, List.of(), calcTicket.attacker, null, NativeAcceptedState.Incoming.empty(), 0, NativePlayerLifetime.deadline()));
        return true;
    }

    static void markCombat(ClientSession clientSession, ClientSession clientSession2) {
        Frame frame = current.get();
        if (frame == null || frame.accepted) {
            CombatAccess.call((Object)clientSession, (String)"markBattleCombatWith", (String)"(Llocal/lito381/server/ClientSession;)V", (Object[])new Object[]{clientSession2});
        }
    }

    private static boolean needsZero(ClientSession clientSession, Binding binding) {
        Queue queue = NativePlayerCommit.queue(clientSession, binding);
        return queue == null || queue.terminal || NativePlayerCommit.integer(clientSession, "playerHp") <= 0;
    }

    static void enqueueBattle(ClientSession clientSession, int n, ClientSession clientSession2, boolean bl, boolean bl2, long l, BattleJoyOfPainStatus.Lease lease, int n2) {
        CalcTicket calcTicket = calculation.get();
        Binding binding = calcTicket == null ? Binding.capture(clientSession) : calcTicket.target;
        Binding binding2 = calcTicket == null ? Binding.capture(clientSession2) : calcTicket.caster;
        boolean bl3 = calcTicket == null || calcTicket.feedbackPositive;
        NativePlayerCommit.offer(clientSession, new Event(binding, binding2, n, bl, bl2 || !bl3, l, (BattleJoyOfPainStatus.Lease)(bl3 ? lease : null), bl3 ? n2 : 0, false, binding2 == null ? 0L : binding2.object, null, null, calcTicket == null ? List.of() : calcTicket.defense, bl3, calcTicket == null ? List.of() : calcTicket.equipment, calcTicket == null ? null : calcTicket.attacker, calcTicket == null ? null : calcTicket.npcFeedback, calcTicket == null ? NativeAcceptedState.Incoming.empty() : calcTicket.incoming, NativePlayerVitals.minimum(), NativePlayerLifetime.deadline(), calcTicket != null ? calcTicket.sourceDisplay : null));
    }

    static void enqueueWorld(ClientSession clientSession, int n, long l, ClassicWorldMonsterPopulation.Snapshot snapshot) {
        NativeCombatOrigin.Poison poison = NativeCombatOrigin.poison();
        if (poison != null) {
            snapshot = poison.snapshot();
        }
        WorldPlan worldPlan = worldCalculation.get();
        CalcTicket calcTicket = calculation.get();
        NativePlayerCommit.offer(clientSession, new Event(calcTicket == null ? Binding.capture(clientSession) : calcTicket.target, (Binding)(calcTicket == null ? (worldPlan == null ? null : worldPlan.caster) : calcTicket.caster), n, false, worldPlan != null && worldPlan.suppress, 0L, null, 0, true, l, snapshot, worldPlan, calcTicket == null ? List.of() : calcTicket.defense, calcTicket == null || calcTicket.feedbackPositive, calcTicket == null ? List.of() : calcTicket.equipment, calcTicket == null ? null : calcTicket.attacker, calcTicket == null ? null : calcTicket.npcFeedback, calcTicket == null ? NativeAcceptedState.Incoming.empty() : calcTicket.incoming, NativePlayerVitals.minimum(), NativePlayerLifetime.deadline()));
    }

    static boolean admitRaw(int n) {
        CalcTicket calcTicket = calculation.get();
        if (calcTicket == null) {
            return n > 0;
        }
        calcTicket.rawNonpositive = n <= 0;
        return true;
    }

    static int signedEarth(ClientSession clientSession, int n, String string) {
        if (NativeSourceFatalityDamage.skipAttackerCalc((ClientSession)clientSession)) {
            return n;
        }
        return n <= 0 ? n : CombatAccess.integerCall((Object)clientSession, (String)"earthBindProtectedCombatDamage", (String)"(ILjava/lang/String;)I", (Object[])new Object[]{n, string});
    }

    static boolean admitEarth(int n) {
        CalcTicket calcTicket = calculation.get();
        return n > 0 || calcTicket != null && calcTicket.rawNonpositive;
    }

    static boolean admitWorld(double d) {
        return calculation.get() != null || d > 0.0;
    }

    static boolean allowDefenseValue(int n) {
        return calculation.get() != null || n > 0;
    }

    private static void prepareEquipment(ClientSession clientSession, boolean bl) {
        CalcTicket calcTicket = calculation.get();
        if (NativeCombatOrigin.poisonClock() || calcTicket == null || calcTicket.caster == null && calcTicket.attacker == null) {
            return;
        }
        try {
            calcTicket.equipment.addAll(NativeEquipmentDefense.prepare(clientSession, bl));
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.defenseCalcFailure(runtimeException);
        }
    }

    static void secondaryFailure(String string, RuntimeException runtimeException) {
        NativePlayerCommit.effectFailure(string, runtimeException);
    }

    static Binding frameBinding(ClientSession clientSession) {
        Frame frame = current.get();
        return frame == null ? null : (clientSession == frame.pc ? frame.event.target : frame.event.caster);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean mutateOwned(ClientSession clientSession, Binding binding, BooleanSupplier booleanSupplier) {
        Frame frame = current.get();
        if (binding == null || frame == null || !NativePlayerCommit.validFrame(frame.pc)) {
            return false;
        }
        Queue queue = NativePlayerCommit.queue(clientSession, binding);
        if (queue == null) {
            return false;
        }
        queue.vitalLock.lock();
        try {
            boolean bl = !queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0 && booleanSupplier.getAsBoolean();
            return bl;
        }
        finally {
            queue.vitalLock.unlock();
        }
    }

    static void safeAuxSend(ClientSession clientSession, byte[] byArray, String string) {
        Frame frame = current.get();
        if (!(frame == null || NativePlayerCommit.validFrame(frame.pc) && (clientSession == frame.pc || frame.event.caster != null && frame.event.caster.matches(clientSession)))) {
            return;
        }
        try {
            CombatAccess.call((Object)clientSession, (String)"sendEncrypted", (String)"([BLjava/lang/String;)V", (Object[])new Object[]{byArray, string});
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.effectFailure("aux packet", runtimeException);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int debitMp(ClientSession clientSession, Binding binding, int n) {
        Queue queue = NativePlayerCommit.queue(clientSession, binding);
        if (queue == null) {
            return 0;
        }
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        if (receipt == null) {
            return 0;
        }
        try {
            int n2 = 0;
            int n3 = 0;
            int n4 = 0;
            queue.vitalLock.lock();
            try {
                if (!queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0) {
                    n2 = Math.min(Math.max(0, n), Math.max(0, NativePlayerCommit.integer(clientSession, "playerMp")));
                    n3 = NativePlayerCommit.integer(clientSession, "playerMp") - n2;
                    n4 = NativePlayerCommit.integer(clientSession, "playerMaxMp");
                    if (n2 > 0) {
                        CombatAccess.set((Object)clientSession, (String)"playerMp", (Object)n3);
                        NativePlayerVitalCompletions.debit(receipt, n3, n4, n2);
                    }
                }
            }
            finally {
                queue.vitalLock.unlock();
            }
            int n5 = n2;
            return n5;
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void recover(ClientSession clientSession, Binding binding, int n, int n2, String string) {
        if (n <= 0 && n2 <= 0) {
            return;
        }
        Queue queue = NativePlayerCommit.queue(clientSession, binding);
        if (queue == null) {
            return;
        }
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        if (receipt == null) {
            return;
        }
        try {
            int n3 = 0;
            int n4 = 0;
            int n5 = 0;
            int n6 = 0;
            boolean bl = false;
            boolean bl2 = false;
            queue.vitalLock.lock();
            try {
                if (!queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0) {
                    int n7 = NativePlayerCommit.integer(clientSession, "playerHp");
                    int n8 = NativePlayerCommit.integer(clientSession, "playerMp");
                    n4 = NativePlayerCommit.integer(clientSession, "playerMaxHp");
                    n6 = NativePlayerCommit.integer(clientSession, "playerMaxMp");
                    n3 = n > 0 ? (int)Math.min((long)n4, (long)n7 + (long)n) : n7;
                    n5 = n2 > 0 ? (int)Math.min((long)n6, (long)n8 + (long)n2) : n8;
                    bl = n3 != n7;
                    boolean bl3 = bl2 = n5 != n8;
                    if (bl) {
                        CombatAccess.set((Object)clientSession, (String)"playerHp", (Object)n3);
                    }
                    if (bl2) {
                        CombatAccess.set((Object)clientSession, (String)"playerMp", (Object)n5);
                    }
                    if (bl || bl2) {
                        NativePlayerVitalCompletions.recovery(receipt, binding, n3, n4, n5, n6, bl, bl2, string);
                    }
                }
            }
            finally {
                queue.vitalLock.unlock();
            }
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession);
        }
    }

    private static void defenseCalcFailure(RuntimeException runtimeException) {
        long l = defenseCalcFailures.incrementAndGet();
        if (l <= 1L || (l & l - 1L) == 0L) {
            System.err.println("[LiTo development] defense Calc failed; incoming damage continues: " + String.valueOf(runtimeException));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static NativeMagicDollEffects.DefenseProc drawDefense(ClientSession clientSession, String string) {
        NativeMagicDoll nativeMagicDoll;
        CalcTicket calcTicket = calculation.get();
        if (calcTicket == null) {
            throw new IllegalStateException("defense Calc without owner ticket");
        }
        Object object = CombatAccess.field((Object)clientSession, (String)"activeMagicDollObjectIds");
        synchronized (object) {
            nativeMagicDoll = (NativeMagicDoll)NativeSourceDollDefense.dollField((Object)clientSession, (String)"activeMagicDoll");
        }
        int n = 0;
        ArrayList<SourceMagicDollPower> arrayList = new ArrayList<SourceMagicDollPower>();
        if (nativeMagicDoll != null) {
            for (SourceMagicDollPower sourceMagicDollPower : nativeMagicDoll.type().powerDefinitions()) {
                if (sourceMagicDollPower == null) continue;
                try {
                    if (!sourceMagicDollPower.className().equals("ACTIVE_REDUX") && (!sourceMagicDollPower.className().equals("ACTIVE_DMG") || sourceMagicDollPower.mode() != 2) || sourceMagicDollPower.probability() <= 0) continue;
                    defenseRolls.incrementAndGet();
                    if (ThreadLocalRandom.current().nextInt(1000) >= Math.min(1000, sourceMagicDollPower.probability())) continue;
                    n = (int)Math.min(Integer.MAX_VALUE, (long)n + (long)Math.max(0, sourceMagicDollPower.valueSub()));
                    arrayList.add(sourceMagicDollPower);
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.defenseCalcFailure(runtimeException);
                }
            }
        }
        NativeMagicDollEffects.DefenseProc defenseProc = new NativeMagicDollEffects.DefenseProc(n, arrayList);
        if (nativeMagicDoll != null) {
            for (SourceMagicDollPower sourceMagicDollPower : arrayList) {
                calcTicket.defense.add(new DefenseVisual(nativeMagicDoll, sourceMagicDollPower.id(), 0, calcTicket.target.object, string + "-defense", clientSession));
            }
        }
        return defenseProc;
    }

    private static NativeMagicDollEffects.DefenseProc defenseOrEmpty(ClientSession clientSession, String string) {
        try {
            return NativePlayerCommit.drawDefense(clientSession, string);
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.defenseCalcFailure(runtimeException);
            return NativeMagicDollEffects.DefenseProc.empty();
        }
    }

    static int captureBattleDefense(ClientSession clientSession, int n, boolean bl, String string) {
        if (NativeSourceFatalityDamage.skipAttackerCalc((ClientSession)clientSession)) {
            return n;
        }
        if (!CombatRules.loaded()) {
            return CombatAccess.integerCall((Object)clientSession, (String)"nativeMagicDollDamageAfterActiveReduction", (String)"(IZLjava/lang/String;)I", (Object[])new Object[]{n, bl, string});
        }
        NativePlayerCommit.calculation.get().incoming = NativeAcceptedState.capture((ClientSession)clientSession, (n > 0 ? 1 : 0) != 0);
        if (n <= 0) {
            NativePlayerCommit.calculation.get().feedbackPositive = false;
            return n;
        }
        NativePlayerCommit.prepareEquipment(clientSession, !bl);
        NativeMagicDollEffects.DefenseProc defenseProc = NativePlayerCommit.defenseOrEmpty(clientSession, string);
        int n2 = Math.max(0, n - defenseProc.damageReduction());
        boolean bl2 = NativePlayerCommit.calculation.get().feedbackPositive = n2 > 0;
        if (n2 > 0 && bl) {
            NativePlayerCommit.captureNpcFeedback(clientSession);
        }
        return n2;
    }

    static int captureWorldDefense(ClientSession clientSession, int n, boolean bl, String string, double d, BattleAbilityStatus.IllusionAvatar illusionAvatar) {
        if (!CombatRules.loaded()) {
            return CombatIncomingSource.world((ClientSession)clientSession, (int)n, (boolean)bl, (String)string, (double)d, (BattleAbilityStatus.IllusionAvatar)illusionAvatar);
        }
        if (!(d > 0.0)) {
            NativePlayerCommit.calculation.get().feedbackPositive = false;
            return (int)(d * CombatIncomingSource.weapon((ClientSession)clientSession) * CombatIncomingSource.rate((ClientSession)clientSession));
        }
        WorldPlan worldPlan = worldCalculation.get();
        NativePlayerCommit.calculation.get().incoming = NativeAcceptedState.capture((ClientSession)clientSession, (d > 0.0 ? 1 : 0) != 0);
        NativePlayerCommit.prepareEquipment(clientSession, worldPlan != null && worldPlan.kind != 2 && worldPlan.suppress);
        NativeMagicDollEffects.DefenseProc defenseProc = NativePlayerCommit.defenseOrEmpty(clientSession, string);
        double d2 = Math.max(0.0, d - (double)defenseProc.damageReduction());
        boolean bl2 = NativePlayerCommit.calculation.get().feedbackPositive = d2 > 0.0;
        if (d2 > 0.0 && (worldPlan == null || worldPlan.kind == 2 || !worldPlan.suppress)) {
            NativePlayerCommit.captureNpcFeedback(clientSession);
        }
        return (int)(d2 * CombatIncomingSource.weapon((ClientSession)clientSession) * CombatIncomingSource.rate((ClientSession)clientSession));
    }

    private static void captureNpcFeedback(ClientSession clientSession) {
        CalcTicket calcTicket = calculation.get();
        if (!NativeCombatOrigin.poisonClock() && calcTicket.attacker != null) {
            try {
                calcTicket.npcFeedback = NativeCombatAttacker.feedback(clientSession, calcTicket.attacker);
            }
            catch (RuntimeException runtimeException) {
                NativePlayerCommit.defenseCalcFailure(runtimeException);
            }
        }
    }

    private static void offer(ClientSession clientSession, Event event) {
        Queue queue = NativePlayerCommit.queue(clientSession, event.target);
        if (queue == null) {
            rejected.incrementAndGet();
            return;
        }
        if (queue.offer(event) && queue.start()) {
            NativePlayerCommit.dispatch(clientSession, queue, 0);
        }
    }

    private static boolean worldTarget(ClientSession clientSession, ClientSession clientSession2, boolean bl) {
        return String.valueOf(CombatAccess.field((Object)clientSession, (String)"phase")).equals("WORLD") && clientSession2 != null && (!bl || NativePlayerCommit.sameWorld(clientSession, clientSession2));
    }

    private static boolean sameWorld(ClientSession clientSession, ClientSession clientSession2) {
        return (Boolean)CombatAccess.call((Object)clientSession, (String)"sameClassicWorldMap", (String)"(Llocal/lito381/server/ClientSession;)Z", (Object[])new Object[]{clientSession2});
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void calculateWorld(ClientSession clientSession, int n, long l, WorldPlan worldPlan) {
        WorldPlan worldPlan2 = worldCalculation.get();
        worldCalculation.set(worldPlan);
        try {
            CombatAccess.call((Object)clientSession, (String)"takeClassicWorldDirectDamage", (String)"(DJ)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", (Object[])new Object[]{(double)n, l});
        }
        finally {
            if (worldPlan2 == null) {
                worldCalculation.remove();
            } else {
                worldCalculation.set(worldPlan2);
            }
        }
    }

    static void worldPlayer(ClientSession clientSession, int n, ClientSession clientSession2, boolean bl, boolean bl2) {
        Binding binding = NativePlayerCommit.combatBinding(clientSession);
        Binding binding2 = NativePlayerCommit.combatBinding(clientSession2);
        if (!NativePlayerCommit.worldTarget(clientSession, clientSession2, true)) {
            return;
        }
        if (NativePlayerCommit.needsZero(clientSession, binding)) {
            NativePlayerCommit.calculateWorld(clientSession, n, binding2.object, new WorldPlan(1, binding, binding2, true, null, 0, null));
            return;
        }
        if (bl && n > 0 && (n = CombatAccess.integerCall((Object)clientSession, (String)"earthBindProtectedCombatDamage", (String)"(ILjava/lang/String;)I", (Object[])new Object[]{n, "classic-world-player-" + clientSession2.playerName()})) <= 0) {
            return;
        }
        BattleJoyOfPainStatus battleJoyOfPainStatus = (BattleJoyOfPainStatus)CombatAccess.field((Object)clientSession2, (String)"joyOfPainStatus");
        BattleJoyOfPainStatus.Lease lease = n > 0 && !bl2 && clientSession2 != clientSession ? battleJoyOfPainStatus.capture(System.nanoTime()) : null;
        int n2 = lease == null ? 0 : BattleJoyOfPain.feedbackDamage((int)NativePlayerCommit.integer(clientSession, "playerMaxHp"), (int)NativePlayerCommit.integer(clientSession, "playerHp"), (int)CombatAccess.integerCall((Object)clientSession, (String)"joyOfPainEquipmentAddedHp", (String)"()I", (Object[])new Object[0]));
        NativePlayerCommit.calculateWorld(clientSession, n, binding2.object, new WorldPlan(1, binding, binding2, bl2, lease, n2, null));
    }

    static void worldCompanion(ClientSession clientSession, int n, ClientSession clientSession2, BattleCompanion battleCompanion) {
        Binding binding = NativePlayerCommit.combatBinding(clientSession);
        Binding binding2 = NativePlayerCommit.combatBinding(clientSession2);
        if (clientSession == clientSession2 || battleCompanion == null || !NativePlayerCommit.worldTarget(clientSession, clientSession2, true)) {
            return;
        }
        BattleCompanion.Snapshot snapshot = battleCompanion.snapshot();
        if (!snapshot.active() || !snapshot.kind().classicWorldEligible()) {
            return;
        }
        NativePlayerCommit.calculateWorld(clientSession, n, snapshot.objectId(), new WorldPlan(2, binding, binding2, true, null, 0, NativeCombatAttacker.companion(clientSession2, battleCompanion)));
    }

    static void worldPoison(ClientSession clientSession, int n, ClientSession clientSession2) {
        Binding binding = NativePlayerCommit.combatBinding(clientSession);
        Binding binding2 = NativePlayerCommit.combatBinding(clientSession2);
        if (!NativePlayerCommit.worldTarget(clientSession, clientSession2, false)) {
            return;
        }
        NativePlayerCommit.calculateWorld(clientSession, n, binding2.object, new WorldPlan(3, binding, binding2, true, null, 0, null));
    }

    static void dispatch(ClientSession clientSession, Queue queue, int n) {
        NativePlayerLifetime.dispatch(clientSession, queue, n);
    }

    static void retried() {
        retries.incrementAndGet();
    }

    static void withMinimumCommitDelay(long l, Runnable runnable) {
        NativePlayerLifetime.withMinimumCommitDelay(l, runnable);
    }

    static ClientSession caster(ClientSession clientSession, Binding binding) {
        if (binding == null) {
            return null;
        }
        if (binding.matches(clientSession)) {
            return clientSession;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        if (binding.phase.equals("WORLD")) {
            for (ClientSession clientSession2 : (List<ClientSession>)CombatAccess.call((Object)clientSession, (String)"classicWorldMembers", (String)"(I)Ljava/util/List;", (Object[])new Object[]{binding.map})) {
                if (!binding.matches(clientSession2)) continue;
                return clientSession2;
            }
        }
        for (ClientSession clientSession2 : gameHub.roomMembers(binding.room)) {
            if (!binding.matches(clientSession2)) continue;
            return clientSession2;
        }
        for (ClientSession clientSession2 : gameHub.connectedSessionsSnapshot()) {
            if (!binding.matches(clientSession2)) continue;
            return clientSession2;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static void drain(ClientSession clientSession, Queue queue) {
        NativePlayerVitalCompletions.Receipt receipt = null;
        boolean bl = false;
        try {
            int n = 0;
            while (n < 256) {
                block21: {
                    long l = queue.waitNanos();
                    if (l > 0L) {
                        NativePlayerLifetime.deadline(clientSession, queue, l);
                        return;
                    }
                    Event event = queue.claim();
                    if (event == null) {
                        return;
                    }
                    boolean bl2 = false;
                    try {
                        NativeAcceptedHpCompletion nativeAcceptedHpCompletion;
                        ClientSession clientSession2 = NativePlayerCommit.caster(clientSession, event.caster);
                        boolean bl3 = NativeSourceGroundQueued.early((ClientSession)clientSession, (Event)event, (ClientSession)clientSession2);
                        boolean bl4 = NativePlayerVitals.gm((ClientSession)clientSession);
                        NativeEquipmentDefense.Resolved resolved = bl3 && !queue.closed && !queue.terminal && event.target.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0 ? NativeEquipmentDefense.resolve(clientSession, clientSession2, event.equipment, event.attacker) : NativeEquipmentDefense.Resolved.empty();
                        queue.vitalLock.lock();
                        try {
                            int n2;
                            NativePlayerVitals.Result result;
                            int n3;
                            int n4;
                            receipt = null;
                            receipt = NativeHpCompletionAdmission.reserve((ClientSession)clientSession, (Queue)queue);
                            if (receipt != null && !queue.closed && !queue.terminal && event.target.matchesFast(clientSession) && String.valueOf(CombatAccess.field((Object)clientSession, (String)"phase")).equals(event.world ? "WORLD" : "BATTLE") && NativeSourceGroundQueued.beforeEvent((ClientSession)clientSession, (Event)event, (ClientSession)clientSession2, (boolean)bl3)) {
                                n4 = NativePlayerCommit.integer(clientSession, "playerMaxHp");
                                n3 = NativePlayerCommit.integer(clientSession, "playerHp");
                                bl2 = n3 > 0;
                                if (bl2) {
                                    result = NativePlayerVitals.calculate((int)n3, (int)n4, (int)event.damage, (int)event.minimumHp, (boolean)bl4);
                                    n2 = result.after();
                                } else {
                                    n2 = bl4 ? n3 : 0;
                                    result = new NativePlayerVitals.Result(n2, true);
                                    repairs.incrementAndGet();
                                }
                                queue.terminal = result.fatal();
                                if (n2 != n3) {
                                    CombatAccess.set((Object)clientSession, (String)"playerHp", (Object)n2);
                                }
                                nativeAcceptedHpCompletion = NativeSourceDamageDisplay.enqueue((Frame)new Frame(clientSession, clientSession2, queue, event, n3, n2, n4, result.fatal(), bl2, resolved), (NativePlayerVitalCompletions.Receipt)receipt);
                            } else {
                                NativeAcceptedFreezeMutation.abandon((Event)event);
                                dropped.incrementAndGet();
                                break block21;
                            }
                        }
                        finally {
                            NativeHpCompletionAdmission.unlock((ReentrantLock)queue.vitalLock, (NativePlayerVitalCompletions.Receipt)receipt);
                        }
                        NativePlayerVitalCompletions.drain(clientSession);
                        if (nativeAcceptedHpCompletion.suspend()) {
                            bl = true;
                            return;
                        }
                    }
                    catch (Throwable throwable) {
                        NativeAcceptedFreezeMutation.abandon((Event)event);
                        if (NativeAcceptedState.isStale((Throwable)throwable)) {
                            if (bl2) {
                                committed.incrementAndGet();
                            }
                        }
                        failed.incrementAndGet();
                        System.err.println("[LiTo development] PC committed tail failed, no HP replay: " + String.valueOf(throwable));
                        throwable.printStackTrace();
                    }
                }
                ++n;
            }
            return;
        }
        finally {
            if (!bl) {
                NativePlayerCommit.resumeAccepted(clientSession, queue);
            }
        }
    }

    private static void worldCommit(ClientSession clientSession, ClientSession clientSession2, Event event) {
        CombatPoisonLifecycle.Scope scope = CombatPoisonLifecycle.open();
        try {
            if (event.plan != null && NativePlayerCommit.current.get().accepted) {
                CombatPoisonLifecycle.lastDamage((ClientSession)clientSession, (ClientSession)clientSession2);
            }
            Object object = CombatAccess.call((Object)clientSession, (String)"sourcePlayerWorldCommit", (String)"(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", (Object[])new Object[]{0.0, event.attackerObject, scope, event.npc, null, event.damage});
            WorldPlan worldPlan = event.plan;
            if (worldPlan != null && clientSession2 != null && NativePlayerCommit.validFrame(clientSession) && worldPlan.caster.matches(clientSession2)) {
                if (worldPlan.kind == 1 || worldPlan.kind == 2) {
                    NativePlayerCommit.accepted(clientSession, clientSession2, "liability", () -> CombatAccess.call((Object)clientSession, (String)"applyClassicWorldPlayerLiability", (String)"(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;)V", (Object[])new Object[]{clientSession2, object}));
                }
                if (worldPlan.kind == 1 && event.feedbackPositive) {
                    if (!worldPlan.suppress && NativePlayerCommit.integer(clientSession2, "playerHp") > 0) {
                        NativePlayerCommit.safeMortal(clientSession, clientSession2, "classic-world-accepted-player-damage");
                    }
                    NativePlayerCommit.safeJoy(clientSession, clientSession2, worldPlan.joy, worldPlan.joyDamage, "classic-world-accepted-player-damage");
                } else if (worldPlan.kind == 3 && NativePlayerCommit.sameWorld(clientSession, clientSession2)) {
                    NativePlayerCommit.accepted(clientSession, clientSession2, "pink", () -> CombatAccess.call((Object)clientSession2, (String)"publishClassicWorldPinkName", (String)"(Llocal/lito381/server/ClientSession;)V", (Object[])new Object[]{clientSession}));
                }
            }
            CombatPoisonLifecycle.finish((CombatPoisonLifecycle.Scope)scope);
        }
        catch (Throwable throwable) {
            CombatPoisonLifecycle.failed((CombatPoisonLifecycle.Scope)scope, (Throwable)throwable);
            throw throwable;
        }
    }

    static byte[] hitRatio(long l, int n, int n2) {
        Frame frame = current.get();
        return frame != null && (!frame.accepted || frame.before == frame.after) ? new byte[]{} : Protocol.hitRatio((long)l, (int)n, (int)n2);
    }

    static int deathHp(ClientSession clientSession) {
        Frame frame = current.get();
        return frame != null && frame.pc == clientSession ? (frame.fatal ? 0 : 1) : NativePlayerCommit.integer(clientSession, "playerHp");
    }

    static int hp(ClientSession clientSession) {
        Frame frame = current.get();
        return frame != null && frame.pc == clientSession ? frame.after : NativePlayerCommit.integer(clientSession, "playerHp");
    }

    static int max(ClientSession clientSession) {
        Frame frame = current.get();
        return frame != null && frame.pc == clientSession ? frame.maximum : NativePlayerCommit.integer(clientSession, "playerMaxHp");
    }

    private static void effectFailure(String string, RuntimeException runtimeException) {
        long l = effectFailures.incrementAndGet();
        if (l <= 1L || (l & l - 1L) == 0L) {
            System.err.println("[LiTo development] accepted PC " + string + " failed, HP remains committed: " + String.valueOf(runtimeException));
        }
    }

    private static void accepted(ClientSession clientSession, ClientSession clientSession2, String string, Runnable runnable) {
        Frame frame = current.get();
        if (!(frame == null || frame.accepted && NativePlayerCommit.validFrame(clientSession) && frame.event.caster != null && frame.event.caster.matches(clientSession2))) {
            return;
        }
        try {
            runnable.run();
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.effectFailure(string, runtimeException);
        }
    }

    static void safeMortal(ClientSession clientSession, ClientSession clientSession2, String string) {
        NativePlayerCommit.accepted(clientSession, clientSession2, "Mortal", () -> CombatAccess.call((Object)clientSession, (String)"tryMortalBodyPlayerReflection", (String)"(Llocal/lito381/server/ClientSession;Ljava/lang/String;)V", (Object[])new Object[]{clientSession2, string}));
    }

    static void safeJoy(ClientSession clientSession, ClientSession clientSession2, BattleJoyOfPainStatus.Lease lease, int n, String string) {
        if (lease == null) {
            return;
        }
        NativePlayerCommit.accepted(clientSession, clientSession2, "Joy", () -> CombatAccess.call((Object)clientSession, (String)"applyJoyOfPainPlayerFeedback", (String)"(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleJoyOfPainStatus$Lease;ILjava/lang/String;)V", (Object[])new Object[]{clientSession2, lease, n, string}));
    }

    static void safeSend(ClientSession clientSession, byte[] byArray, String string) {
        Frame frame = current.get();
        if (frame != null) {
            int n;
            if (!NativePlayerCommit.validFrame(frame.pc) || byArray.length == 0) {
                return;
            }
            int n2 = n = byArray.length == 0 ? -1 : byArray[0] & 0xFF;
            if (!(frame.accepted && frame.before != frame.after || n != 13 && n != 104)) {
                return;
            }
        }
        try {
            CombatAccess.call((Object)clientSession, (String)"sendEncrypted", (String)"([BLjava/lang/String;)V", (Object[])new Object[]{byArray, string});
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.effectFailure("packet", runtimeException);
        }
    }

    static void safeRatio(ClientSession clientSession, String string) {
        Frame frame = current.get();
        if (!(frame == null || frame.accepted && NativePlayerCommit.validFrame(clientSession) && frame.before != frame.after)) {
            return;
        }
        try {
            CombatAccess.call((Object)clientSession, (String)"broadcastOwnHitRatio", (String)"(Ljava/lang/String;)V", (Object[])new Object[]{string});
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.effectFailure("HP ratio", runtimeException);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void publishDefense(ClientSession clientSession) {
        Frame frame = current.get();
        if (frame == null || !frame.accepted || frame.pc != clientSession || frame.defensePublished) {
            return;
        }
        frame.defensePublished = true;
        frame.defenseActive = true;
        try {
            block10: for (DefenseVisual defenseVisual : frame.event.defense) {
                if (!NativePlayerCommit.validFrame(clientSession)) {
                    break;
                }
                defenseAttempts.incrementAndGet();
                byte[] byArray = defenseVisual.gfx > 0 ? Protocol.effectObject((long)defenseVisual.target, (int)defenseVisual.gfx) : Protocol.objectAction((long)defenseVisual.doll.objectId(), (int)67);
                String string = defenseVisual.gfx > 0 ? "S_EFFECT magic-doll-proc=" + defenseVisual.power + " target=" + defenseVisual.target + " gfx=" + defenseVisual.gfx : "S_ACTION magic-doll-proc=" + defenseVisual.power + " doll=" + defenseVisual.doll.objectId() + " action=67";
                try {
                    for (ClientSession clientSession2 : (List<ClientSession>)NativeSourceDollDefense.recipients((Object)clientSession, (String)"nativeMagicDollRecipients", (String)"(Llocal/lito381/server/NativeMagicDoll;)Ljava/util/List;", (Object[])new Object[]{defenseVisual.doll}, (DefenseVisual)defenseVisual)) {
                        if (!NativePlayerCommit.validFrame(clientSession)) continue block10;
                        NativeSourceDollDefense.send((ClientSession)clientSession2, (byte[])byArray, (String)(string + " defense=true reason=" + defenseVisual.reason), (DefenseVisual)defenseVisual, (ClientSession)clientSession);
                    }
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.effectFailure("defense Proc", runtimeException);
                }
            }
        }
        finally {
            try {
                if (!frame.event.equipment.isEmpty() && NativePlayerCommit.validFrame(clientSession)) {
                    NativeEquipmentDefense.apply(clientSession, NativePlayerCommit.caster(clientSession, frame.event.caster), frame.event.target, frame.event.caster, frame.equipment, frame.event.attacker);
                }
            }
            finally {
                frame.defenseActive = false;
            }
        }
    }

    static long defenseRolls() {
        return defenseRolls.get();
    }

    static long defenseAttempts() {
        return defenseAttempts.get();
    }

    static long effectFailures() {
        return effectFailures.get();
    }

    static long defenseCalcFailures() {
        return defenseCalcFailures.get();
    }

    static boolean defensePublishing() {
        Frame frame = current.get();
        return frame != null && (frame.defenseActive || NativeEffectCompletions.defensePublishing((Frame)frame));
    }

    static long repairs() {
        return repairs.get();
    }

    static long pending() {
        return pending.get();
    }

    static long committed() {
        return committed.get();
    }

    static long dropped() {
        return dropped.get();
    }

    static long failed() {
        return failed.get();
    }

    static long rejected() {
        return rejected.get();
    }

    static boolean vitalLockHeld() {
        Frame frame = current.get();
        return frame != null && frame.queue.vitalLock.isHeldByCurrentThread();
    }

    static boolean ownsFrame(ClientSession clientSession) {
        Frame frame = current.get();
        return frame != null && frame.pc == clientSession;
    }

    static boolean validFrame(ClientSession clientSession) {
        if (!NativeSourceGroundQueued.validFrame((ClientSession)clientSession)) {
            return false;
        }
        if (!NativeSourceCubePlayer.validFrame((ClientSession)clientSession)) {
            return false;
        }
        if (!NativeSourceRawDamage.validFrame((ClientSession)clientSession)) {
            return false;
        }
        Frame frame = current.get();
        return frame != null && frame.pc == clientSession && !frame.queue.closed && frame.event.target.matchesFast(clientSession);
    }

    static void executeFixture(Runnable runnable) {
        NativePlayerLifetime.executeFixture(runnable);
    }

    static void executeFixture(GameHub gameHub, Runnable runnable) {
        NativePlayerLifetime.executeFixture(gameHub, runnable);
    }

    static boolean idle() {
        return pending.get() == 0L && NativePlayerLifetime.allIdle();
    }

    static void closeFixture() {
        NativePlayerLifetime.closeFixture();
    }

    static void completionFrame(Frame frame, Runnable runnable) {
        Frame frame2 = current.get();
        if (frame == null) {
            current.remove();
        } else {
            current.set(frame);
        }
        try {
            runnable.run();
        }
        finally {
            if (frame2 == null) {
                current.remove();
            } else {
                current.set(frame2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void acceptedTail(Frame frame) {
        Frame frame2 = current.get();
        ClientSession clientSession = frame.pc;
        ClientSession clientSession2 = frame.casterSession;
        Queue queue = frame.queue;
        Event event = frame.event;
        boolean bl = frame.accepted;
        current.set(frame);
        try {
            if (event.world) {
                NativePlayerCommit.worldCommit(clientSession, clientSession2, event);
            } else {
                CombatAccess.call((Object)clientSession, (String)"sourcePlayerBattleCommit", (String)"(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V", (Object[])new Object[]{event.damage, clientSession2, event.earth, event.suppress, event.terminal, event.joy, event.joyDamage});
            }
            if (bl) {
                committed.incrementAndGet();
            }
        }
        catch (NativeAcceptedState.Stale stale) {
            if (bl) {
                committed.incrementAndGet();
            }
        }
        finally {
            block26: {
                try {
                    if (!bl) break block26;
                    try {
                        NativePlayerCommit.publishDefense(clientSession);
                        NativeSourceDamageDisplay.accepted((Frame)frame);
                    }
                    finally {
                        NativeCombatAttacker.acceptedFeedback(clientSession, event.attacker, event.npcFeedback);
                    }
                }
                finally {
                    if (frame2 == null) {
                        current.remove();
                    } else {
                        current.set(frame2);
                    }
                }
            }
        }
    }

    static void acceptedTailFailure(Frame frame, Throwable throwable) {
        if (NativeAcceptedState.isStale((Throwable)throwable)) {
            if (frame.accepted) {
                committed.incrementAndGet();
            }
            return;
        }
        failed.incrementAndGet();
        System.err.println("[LiTo development] PC committed tail failed, no HP replay: " + String.valueOf(throwable));
        throwable.printStackTrace();
    }

    static void resumeAccepted(ClientSession clientSession, Queue queue) {
        if (queue.finish() && queue.start()) {
            NativePlayerCommit.dispatch(clientSession, queue, 0);
        }
    }

    static void advance(ClientSession clientSession) {
        NativeStaticNpcMotion.advance((ClientSession)clientSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    static void nativeCompanionSceneOriginalAdvance(ClientSession clientSession, Object object) {
        CombatPoisonLifecycle.State state;
        NativePlayerActionScope.Ticket ticket;
        ReentrantLock reentrantLock;
        block13: {
            BattleTimedStatus battleTimedStatus;
            block12: {
                reentrantLock = clientSession.nativePlayerActionLock();
                ticket = NativeSourceManualScope.lock((ReentrantLock)reentrantLock, (ClientSession)clientSession);
                try {
                    battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
                    if (battleTimedStatus != null) break block12;
                }
                catch (Throwable throwable) {
                    NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, (Throwable)throwable);
                    throw throwable;
                }
                NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
                return;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            // MONITORENTER : battleTimedStatus2
            NativeDeClientViewer.retire((BattleTimedStatus)battleTimedStatus);
            state = CombatPoisonLifecycle.monsterPoisonCallbacks((BattleTimedStatus)battleTimedStatus, (boolean)false);
            if (state != null) break block13;
            // MONITOREXIT : battleTimedStatus2
            NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
            return;
        }
        Queue queue = (Queue)CombatAccess.field((Object)state, (String)"sourcePlayerQueue");
        if (queue != null) {
            queue.vitalLock.lock();
            try {
                queue.drop(true);
                CombatAccess.set((Object)state, (String)"sourcePlayerQueue", null);
                CombatAccess.set((Object)state, (String)"sourcePlayerOwner", (Object)object);
            }
            finally {
                queue.vitalLock.unlock();
            }
        } else {
            CombatAccess.set((Object)state, (String)"sourcePlayerOwner", (Object)object);
        }
        // MONITOREXIT : battleTimedStatus2
        NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void nativeCompanionSceneAssign(ClientSession clientSession, Object object, String string) {
        BattleTimedStatus battleTimedStatus;
        NativePlayerActionScope.Ticket ticket;
        ReentrantLock reentrantLock;
        block6: {
            reentrantLock = clientSession.nativePlayerActionLock();
            ticket = NativeSourceManualScope.lock((ReentrantLock)reentrantLock, (ClientSession)clientSession);
            try {
                battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
                if (battleTimedStatus != null) break block6;
                CombatAccess.set((Object)clientSession, (String)string, (Object)object);
            }
            catch (Throwable throwable) {
                NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, (Throwable)throwable);
                throw throwable;
            }
            NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
            return;
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeStaticNpcMotion.nativeCompanionSceneAdvance((ClientSession)clientSession, (Object)object, (String)string);
            NativeProcPlayerState.selectedField((ClientSession)clientSession, (String)string);
            CombatAccess.set((Object)clientSession, (String)string, (Object)object);
        }
        NativeSourceManualScope.unlock((ReentrantLock)reentrantLock, (ClientSession)clientSession, (NativePlayerActionScope.Ticket)ticket, null);
    }

    static ClientSession nativeRefCompanionCaster(ClientSession clientSession, Binding binding) {
        if (binding == null) {
            return null;
        }
        if (NativeCompanionRefScene.binding((Binding)binding, (ClientSession)clientSession)) {
            return clientSession;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        if (binding.phase.equals("WORLD")) {
            for (ClientSession clientSession2 : (List<ClientSession>)CombatAccess.call((Object)clientSession, (String)"classicWorldMembers", (String)"(I)Ljava/util/List;", (Object[])new Object[]{binding.map})) {
                if (!NativeCompanionRefScene.binding((Binding)binding, (ClientSession)clientSession2)) continue;
                return clientSession2;
            }
        }
        for (ClientSession clientSession2 : gameHub.roomMembers(binding.room)) {
            if (!NativeCompanionRefScene.binding((Binding)binding, (ClientSession)clientSession2)) continue;
            return clientSession2;
        }
        for (ClientSession clientSession2 : gameHub.connectedSessionsSnapshot()) {
            if (!NativeCompanionRefScene.binding((Binding)binding, (ClientSession)clientSession2)) continue;
            return clientSession2;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void nativeManaTransferRecover(NativePlayerVitalCompletions.Receipt receipt, ClientSession clientSession, Binding binding, int n, int n2, String string) {
        if (n <= 0 && n2 <= 0) {
            return;
        }
        Queue queue = NativeCompanionManaTransfer.queue((ClientSession)clientSession, (Binding)binding);
        if (queue == null) {
            return;
        }
        NativePlayerVitalCompletions.Receipt receipt2 = receipt;
        if (receipt2 == null) {
            return;
        }
        try {
            int n3 = 0;
            int n4 = 0;
            int n5 = 0;
            int n6 = 0;
            boolean bl = false;
            boolean bl2 = false;
            queue.vitalLock.lock();
            try {
                if (!queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0) {
                    int n7 = NativePlayerCommit.integer(clientSession, "playerHp");
                    int n8 = NativePlayerCommit.integer(clientSession, "playerMp");
                    n4 = NativePlayerCommit.integer(clientSession, "playerMaxHp");
                    n6 = NativePlayerCommit.integer(clientSession, "playerMaxMp");
                    n3 = n > 0 ? (int)Math.min((long)n4, (long)n7 + (long)n) : n7;
                    n5 = n2 > 0 ? (int)Math.min((long)n6, (long)n8 + (long)n2) : n8;
                    bl = n3 != n7;
                    boolean bl3 = bl2 = n5 != n8;
                    if (bl) {
                        CombatAccess.set((Object)clientSession, (String)"playerHp", (Object)n3);
                    }
                    if (bl2) {
                        CombatAccess.set((Object)clientSession, (String)"playerMp", (Object)n5);
                    }
                    if (bl || bl2) {
                        NativePlayerVitalCompletions.recovery(receipt2, binding, n3, n4, n5, n6, bl, bl2, string);
                    }
                }
            }
            finally {
                queue.vitalLock.unlock();
            }
        }
        finally {
            receipt2.close();
            NativePlayerVitalCompletions.drain(clientSession);
        }
    }

    static CalcTicket nativeSourceFatalityCalculation() {
        return calculation.get();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Queue nativeSourceScalarQueue(ClientSession clientSession, Binding binding, BattleTimedStatus battleTimedStatus, NativeSourceEquipmentProcLife.Proof proof, NativeProcSkillCatalog.Catalog catalog) {
        BattleTimedStatus battleTimedStatus2;
        BattleTimedStatus battleTimedStatus3 = battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus3) {
            block6: {
                if (NativeSourceProcScalarSkills.queueCurrent((Binding)binding, (ClientSession)clientSession, (BattleTimedStatus)battleTimedStatus, (NativeSourceEquipmentProcLife.Proof)proof, (NativeProcSkillCatalog.Catalog)catalog)) break block6;
                return (Queue)NativeSourceProcScalarSkills.queueValue(null, (NativeSourceEquipmentProcLife.Proof)proof);
            }
            Object object = NativeSourceProcScalarSkills.state((BattleTimedStatus)battleTimedStatus2);
            Queue queue = (Queue)CombatAccess.field((Object)object, (String)"sourcePlayerQueue");
            if (queue == null || !queue.owner.equals(binding)) {
                if (queue != null) {
                    queue.drop(true);
                }
                queue = new Queue(binding, proof.lifetime(), clientSession.nativePlayerActionLock());
                CombatAccess.set((Object)object, (String)"sourcePlayerQueue", (Object)queue);
            }
            return (Queue)NativeSourceProcScalarSkills.queueValue((Object)queue, (NativeSourceEquipmentProcLife.Proof)proof);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean nativeSourceScalarMutateOwned(ClientSession clientSession, Binding binding, BooleanSupplier booleanSupplier, BattleTimedStatus battleTimedStatus, NativeSourceEquipmentProcLife.Proof proof, NativeProcSkillCatalog.Catalog catalog) {
        Frame frame = current.get();
        if (binding == null || frame == null || !NativePlayerCommit.validFrame(frame.pc)) {
            return false;
        }
        Queue queue = NativePlayerCommit.nativeSourceScalarQueue(clientSession, binding, battleTimedStatus, proof, catalog);
        if (queue == null) {
            return false;
        }
        queue.vitalLock.lock();
        try {
            boolean bl = !queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0 && booleanSupplier.getAsBoolean();
            return bl;
        }
        finally {
            queue.vitalLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Queue nativeSourceProc68Queue(ClientSession clientSession, Binding binding, BattleTimedStatus battleTimedStatus, NativeSourceEquipmentProcLife.Proof proof, NativeProcSkillCatalog.Catalog catalog, BattleAbilityStatus battleAbilityStatus) {
        BattleTimedStatus battleTimedStatus2;
        BattleTimedStatus battleTimedStatus3 = battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus3) {
            block6: {
                if (NativeSourceProc68FirstQueue.current((Binding)binding, (ClientSession)clientSession, (BattleTimedStatus)battleTimedStatus, (NativeSourceEquipmentProcLife.Proof)proof, (NativeProcSkillCatalog.Catalog)catalog, (BattleAbilityStatus)battleAbilityStatus)) break block6;
                return (Queue)NativeSourceProcScalarSkills.queueValue(null, (NativeSourceEquipmentProcLife.Proof)proof);
            }
            Object object = NativeSourceProcScalarSkills.state((BattleTimedStatus)battleTimedStatus2);
            Queue queue = (Queue)CombatAccess.field((Object)object, (String)"sourcePlayerQueue");
            if (queue == null || !queue.owner.equals(binding)) {
                if (queue != null) {
                    queue.drop(true);
                }
                queue = new Queue(binding, proof.lifetime(), clientSession.nativePlayerActionLock());
                CombatAccess.set((Object)object, (String)"sourcePlayerQueue", (Object)queue);
            }
            return (Queue)NativeSourceProcScalarSkills.queueValue((Object)queue, (NativeSourceEquipmentProcLife.Proof)proof);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean nativeSourceProc68MutateOwned(ClientSession clientSession, Binding binding, BooleanSupplier booleanSupplier, BattleTimedStatus battleTimedStatus, NativeSourceEquipmentProcLife.Proof proof, NativeProcSkillCatalog.Catalog catalog, BattleAbilityStatus battleAbilityStatus) {
        Frame frame = current.get();
        if (binding == null || frame == null || !NativePlayerCommit.validFrame(frame.pc)) {
            return false;
        }
        Queue queue = NativePlayerCommit.nativeSourceProc68Queue(clientSession, binding, battleTimedStatus, proof, catalog, battleAbilityStatus);
        if (queue == null) {
            return false;
        }
        queue.vitalLock.lock();
        try {
            boolean bl = !queue.closed && binding.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0 && booleanSupplier.getAsBoolean();
            return bl;
        }
        finally {
            queue.vitalLock.unlock();
        }
    }

    static final class Queue {
        final Binding owner;
        final NativePlayerLifetime life;
        final ArrayDeque<Event> events = new ArrayDeque();
        final ReentrantLock vitalLock;
        volatile boolean closed;
        volatile boolean terminal;
        volatile boolean retired;
        boolean draining;
        boolean running;
        NativePlayerLifetime.Registration waiting;

        Queue(Binding binding, NativePlayerLifetime nativePlayerLifetime) {
            this.vitalLock = new ReentrantLock();
            this.owner = binding;
            this.life = nativePlayerLifetime;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        synchronized boolean nativeFreezeAcceptedOriginalOffer(Event event) {
            Object object = this.life.admission;
            synchronized (object) {
                if (this.closed || this.retired || !this.life.admits() || this.events.size() >= TARGET || !NativeSourcePendingAdmission.reserve((AtomicLong)this.life.nativeSourceDamagePending, (long)GLOBAL)) {
                    rejected.incrementAndGet();
                    return false;
                }
                NativeSourcePendingAdmission.add(this.events, (Object)event, (AtomicLong)this.life.nativeSourceDamagePending);
                this.life.pending.incrementAndGet();
                pending.incrementAndGet();
                this.life.active.add(this);
                return true;
            }
        }

        synchronized boolean start() {
            if (this.closed || this.retired || this.draining || this.running || !this.life.processes() || this.events.isEmpty()) {
                return false;
            }
            this.draining = true;
            return true;
        }

        synchronized boolean beginRun() {
            if (this.closed || this.retired || !this.life.processes()) {
                return false;
            }
            if (this.running) {
                throw new IllegalStateException("overlapping native PC dispatch");
            }
            this.running = true;
            return true;
        }

        synchronized long waitNanos() {
            Event event = this.events.peek();
            return event == null || this.life.mode != NativePlayerLifetime.Mode.OPEN || event.deadline == 0L ? 0L : Math.max(0L, event.deadline - System.nanoTime());
        }

        synchronized Event claim() {
            Event event;
            Event event2 = event = this.closed || this.retired || !this.life.processes() ? null : this.events.poll();
            if (event != null) {
                pending.decrementAndGet();
                this.life.pending.decrementAndGet();
                NativeSourceDamageCapacity.release((NativePlayerLifetime)this.life, (long)1L);
            }
            return event;
        }

        synchronized boolean waitFor(NativePlayerLifetime.Registration registration) {
            if (this.closed || this.retired || !this.life.processes() || registration.attempt == 0 && this.life.mode == NativePlayerLifetime.Mode.DRAINING) {
                return false;
            }
            if (this.waiting != null) {
                throw new IllegalStateException("multiple native PC timers");
            }
            this.waiting = registration;
            return true;
        }

        synchronized boolean removeWait(NativePlayerLifetime.Registration registration) {
            if (this.waiting != registration) {
                return false;
            }
            this.waiting = null;
            registration.cancel();
            this.life.cancelledTimers.incrementAndGet();
            return true;
        }

        synchronized boolean wake(NativePlayerLifetime.Registration registration) {
            if (this.waiting != registration || registration.cancelled) {
                return false;
            }
            this.waiting = null;
            this.draining = false;
            return this.start();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void requestDrain() {
            boolean bl;
            NativePlayerLifetime.Registration registration;
            Queue queue = this;
            synchronized (queue) {
                registration = this.waiting;
                if (registration == null) {
                    return;
                }
                this.removeWait(registration);
                this.draining = false;
                bl = this.start();
            }
            if (bl) {
                NativePlayerLifetime.dispatch(registration.pc, this, 0);
            }
        }

        synchronized void drop(boolean bl) {
            this.closed |= bl;
            if (this.waiting != null) {
                this.removeWait(this.waiting);
            }
            int n = this.events.size();
            ArrayDeque<Event> arrayDeque = this.events;
            NativeAcceptedFreezeMutation.clearEvents(arrayDeque);
            arrayDeque.clear();
            pending.addAndGet(-n);
            this.life.pending.addAndGet(-n);
            NativeSourceDamageCapacity.release((NativePlayerLifetime)this.life, (long)n);
            dropped.addAndGet(n);
            this.life.dropped.addAndGet(n);
            if (!this.running) {
                this.draining = false;
                this.life.active.remove(this);
            }
        }

        synchronized void stop() {
            this.retired = true;
            this.drop(false);
        }

        synchronized boolean finish() {
            this.running = false;
            if (this.closed || this.retired || !this.life.processes()) {
                this.drop(false);
                this.draining = false;
                this.life.active.remove(this);
                return false;
            }
            if (this.waiting != null) {
                return false;
            }
            this.draining = false;
            if (this.events.isEmpty()) {
                this.life.active.remove(this);
                return false;
            }
            return true;
        }

        Queue(Binding binding, NativePlayerLifetime nativePlayerLifetime, ReentrantLock reentrantLock) {
            this.vitalLock = reentrantLock;
            this.owner = binding;
            this.life = nativePlayerLifetime;
        }

        synchronized boolean offer(Event event) {
            return NativeAcceptedFreezeMutation.offer((Queue)this, (Event)event);
        }
    }

    static final class Frame {
        final ClientSession pc;
        final ClientSession casterSession;
        final Queue queue;
        final Event event;
        final int before;
        final int after;
        final int maximum;
        final boolean fatal;
        final boolean accepted;
        final NativeEquipmentDefense.Resolved equipment;
        boolean defensePublished;
        boolean defenseActive;
        volatile Object nativeSourceDollDefenseOutput;
        volatile Object nativeSourceAcceptedPlayers;
        volatile Object nativeSourceEquipmentOutputScope;

        Frame(ClientSession clientSession, ClientSession clientSession2, Queue queue, Event event, int n, int n2, int n3, boolean bl, boolean bl2, NativeEquipmentDefense.Resolved resolved) {
            this.pc = clientSession;
            this.casterSession = clientSession2;
            this.queue = queue;
            this.event = event;
            this.before = n;
            this.after = n2;
            this.maximum = n3;
            this.fatal = bl;
            this.accepted = bl2;
            this.equipment = resolved;
        }
    }

    record Event(Binding target, Binding caster, int damage, boolean earth, boolean suppress, long terminal, BattleJoyOfPainStatus.Lease joy, int joyDamage, boolean world, long attackerObject, ClassicWorldMonsterPopulation.Snapshot npc, WorldPlan plan, List<DefenseVisual> defense, boolean feedbackPositive, List<NativeArmorProc.Rule> equipment, NativeCombatAttacker.Ref attacker, NativeCombatAttacker.Feedback npcFeedback, NativeAcceptedState.Incoming incoming, int minimumHp, long deadline, NativeSourceDamageDisplay.Plan sourceDisplay) {
        @SuppressWarnings("unchecked")
        Event {
            defense = (List<DefenseVisual>)NativeSourceLightningHp.link(List.copyOf(defense), target, caster);
            equipment = List.copyOf(equipment);
        }

        Event(Binding target, Binding caster, int damage, boolean earth, boolean suppress, long terminal, BattleJoyOfPainStatus.Lease joy, int joyDamage, boolean world, long attackerObject, ClassicWorldMonsterPopulation.Snapshot npc, WorldPlan plan, List<DefenseVisual> defense, boolean feedbackPositive, List<NativeArmorProc.Rule> equipment, NativeCombatAttacker.Ref attacker, NativeCombatAttacker.Feedback npcFeedback, NativeAcceptedState.Incoming incoming, int minimumHp, long deadline) {
            this(target, caster, damage, earth, suppress, terminal, joy, joyDamage, world, attackerObject, npc, plan, defense, feedbackPositive, equipment, attacker, npcFeedback, incoming, minimumHp, deadline, null);
        }
    }

    record Binding(Object owner, long object, String source, String account, String selected, String phase, long room, int map, int instance) {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        static Binding capture(ClientSession clientSession) {
            BattleTimedStatus battleTimedStatus;
            if (clientSession == null) {
                return null;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession);
            synchronized (battleTimedStatus2) {
                return new Binding(NativePlayerCommit.owner(clientSession), clientSession.playerObjectId(), clientSession.sourceCharacterIdentity(), clientSession.sourceAccountIdentity(), clientSession.selectedCharacterName(), String.valueOf(CombatAccess.field((Object)clientSession, (String)"phase")), NativePlayerCommit.number(clientSession, "currentRoomId"), NativePlayerCommit.integer(clientSession, "classicWorldMapId"), NativePlayerCommit.integer(clientSession, "classicWorldInstanceId"));
            }
        }

        boolean matches(ClientSession clientSession) {
            return this.equals(Binding.capture(clientSession));
        }

        boolean matchesFast(ClientSession clientSession) {
            return this.object == clientSession.playerObjectId() && this.source.equals(clientSession.sourceCharacterIdentity()) && this.account.equals(clientSession.sourceAccountIdentity()) && this.selected.equals(clientSession.selectedCharacterName()) && this.phase.equals(String.valueOf(CombatAccess.field((Object)clientSession, (String)"phase"))) && this.room == NativePlayerCommit.number(clientSession, "currentRoomId") && this.map == NativePlayerCommit.integer(clientSession, "classicWorldMapId") && this.instance == NativePlayerCommit.integer(clientSession, "classicWorldInstanceId");
        }
    }

    record WorldPlan(int kind, Binding target, Binding caster, boolean suppress, BattleJoyOfPainStatus.Lease joy, int joyDamage, NativeCombatAttacker.Ref attacker) {
    }

    static final class CalcTicket {
        final Binding target;
        final Binding caster;
        final NativeCombatAttacker.Ref attacker;
        NativeCombatAttacker.Feedback npcFeedback;
        NativeAcceptedState.Incoming incoming = NativeAcceptedState.Incoming.empty();
        final CalcTicket previous;
        final List<DefenseVisual> defense = new ArrayList<DefenseVisual>();
        final List<NativeArmorProc.Rule> equipment = new ArrayList<NativeArmorProc.Rule>();
        boolean rawNonpositive;
        boolean feedbackPositive = true;
        NativeSourceDamageDisplay.Plan sourceDisplay;

        CalcTicket(Binding binding, Binding binding2, NativeCombatAttacker.Ref ref, CalcTicket calcTicket) {
            this.target = binding;
            this.caster = binding2;
            this.attacker = ref;
            this.previous = calcTicket;
        }
    }

    record DefenseVisual(NativeMagicDoll doll, int power, int gfx, long target, String reason) {
        DefenseVisual(NativeMagicDoll doll, int power, int gfx, long target, String reason, ClientSession clientSession) {
            this(doll, power, gfx, target, reason);
            NativeSourceDollDefense.proof((DefenseVisual)this, (ClientSession)clientSession);
        }
    }
}

