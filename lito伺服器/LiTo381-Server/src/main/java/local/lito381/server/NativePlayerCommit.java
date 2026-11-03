// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
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
import local.lito381.server.NativeAcceptedHpCompletion;
import local.lito381.server.NativeAcceptedState;
import local.lito381.server.NativeArmorProc;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeCombatOrigin;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeEffectCompletions;
import local.lito381.server.NativeEquipmentDefense;
import local.lito381.server.NativeMagicDoll;
import local.lito381.server.NativeMagicDollEffects;
import local.lito381.server.NativeMassTeleportAnswerLife;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativePlayerVitals;
import local.lito381.server.NativeProcPlayerState;
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
        return ((Number)CombatAccess.field(object, string)).intValue();
    }

    static long number(Object object, String string) {
        return ((Number)CombatAccess.field(object, string)).longValue();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Object owner(ClientSession clientSession) {
        BattleTimedStatus battleTimedStatus;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus2) {
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks(battleTimedStatus, true);
            Object object = CombatAccess.field(state, "sourcePlayerOwner");
            if (object == null) {
                object = new Object();
                CombatAccess.set(state, "sourcePlayerOwner", object);
            }
            return object;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void advance(ClientSession clientSession) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        reentrantLock.lock();
        try {
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            if (battleTimedStatus == null) {
                return;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            synchronized (battleTimedStatus2) {
                CombatPoisonLifecycle.State state;
                block15: {
                    NativeDeClientViewer.retire(battleTimedStatus);
                    state = CombatPoisonLifecycle.monsterPoisonCallbacks(battleTimedStatus, false);
                    if (state != null) break block15;
                    return;
                }
                Queue queue = (Queue)CombatAccess.field(state, "sourcePlayerQueue");
                if (queue != null) {
                    queue.vitalLock.lock();
                    try {
                        queue.drop(true);
                        CombatAccess.set(state, "sourcePlayerQueue", null);
                        CombatAccess.set(state, "sourcePlayerOwner", new Object());
                    }
                    finally {
                        queue.vitalLock.unlock();
                    }
                } else {
                    CombatAccess.set(state, "sourcePlayerOwner", new Object());
                }
            }
        }
        finally {
            reentrantLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void assign(ClientSession clientSession, Object object, String string) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        reentrantLock.lock();
        try {
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            if (battleTimedStatus == null) {
                CombatAccess.set(clientSession, string, object);
                return;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            synchronized (battleTimedStatus2) {
                NativePlayerCommit.advance(clientSession);
                NativeProcPlayerState.selectedField(clientSession, string);
                CombatAccess.set(clientSession, string, object);
            }
        }
        finally {
            reentrantLock.unlock();
        }
    }

    static void selected(ClientSession clientSession, String string) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        reentrantLock.lock();
        try {
            NativeDeItemApplication.retireClient(clientSession);
            NativeMassTeleportAnswerLife.retireRole(clientSession);
            NativePlayerCommit.assign(clientSession, string, "selectedCharacter");
        }
        finally {
            reentrantLock.unlock();
        }
    }

    static void scene(ClientSession clientSession, Object object, String string) {
        NativePlayerCommit.assign(clientSession, object, string);
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
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus2) {
            if (!binding.matches(clientSession)) {
                return null;
            }
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks(battleTimedStatus, true);
            Queue queue = (Queue)CombatAccess.field(state, "sourcePlayerQueue");
            if (queue == null || !queue.owner.equals(binding)) {
                if (queue != null) {
                    queue.drop(true);
                }
                queue = new Queue(binding, NativePlayerLifetime.of(clientSession), clientSession.nativePlayerActionLock());
                CombatAccess.set(state, "sourcePlayerQueue", queue);
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
        if (calcTicket == null || !String.valueOf(CombatAccess.field(clientSession, "phase")).equals(bl ? "WORLD" : "BATTLE")) {
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
            if (!bl && n <= 0 && ((Boolean)CombatAccess.field(clientSession, "deathConfirmationPending")).booleanValue()) {
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
                snapshot = ((GameHub)CombatAccess.field(clientSession, "hub")).classicWorldMonster(NativePlayerCommit.integer(clientSession, "classicWorldMapId"), NativePlayerCommit.integer(clientSession, "classicWorldInstanceId"), l2);
            }
        }
        NativePlayerCommit.offer(clientSession, new Event(calcTicket.target, binding, n, false, true, 0L, null, 0, bl, l2, snapshot, worldPlan, List.of(), false, List.of(), calcTicket.attacker, null, NativeAcceptedState.Incoming.empty(), 0, NativePlayerLifetime.deadline()));
        return true;
    }

    static void markCombat(ClientSession clientSession, ClientSession clientSession2) {
        Frame frame = current.get();
        if (frame == null || frame.accepted) {
            CombatAccess.call(clientSession, "markBattleCombatWith", "(Llocal/lito381/server/ClientSession;)V", clientSession2);
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
        NativePlayerCommit.offer(clientSession, new Event(binding, binding2, n, bl, bl2 || !bl3, l, bl3 ? lease : null, bl3 ? n2 : 0, false, binding2 == null ? 0L : binding2.object, null, null, calcTicket == null ? List.of() : calcTicket.defense, bl3, calcTicket == null ? List.of() : calcTicket.equipment, calcTicket == null ? null : calcTicket.attacker, calcTicket == null ? null : calcTicket.npcFeedback, calcTicket == null ? NativeAcceptedState.Incoming.empty() : calcTicket.incoming, NativePlayerVitals.minimum(), NativePlayerLifetime.deadline()));
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
        return n <= 0 ? n : CombatAccess.integerCall(clientSession, "earthBindProtectedCombatDamage", "(ILjava/lang/String;)I", n, string);
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
            CombatAccess.call(clientSession, "sendEncrypted", "([BLjava/lang/String;)V", byArray, string);
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
                        CombatAccess.set(clientSession, "playerMp", n3);
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
                        CombatAccess.set(clientSession, "playerHp", n3);
                    }
                    if (bl2) {
                        CombatAccess.set(clientSession, "playerMp", n5);
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
        Object object = CombatAccess.field(clientSession, "activeMagicDollObjectIds");
        synchronized (object) {
            nativeMagicDoll = (NativeMagicDoll)CombatAccess.field(clientSession, "activeMagicDoll");
        }
        int n = 0;
        ArrayList<SourceMagicDollPower> arrayList = new ArrayList<SourceMagicDollPower>();
        if (nativeMagicDoll != null) {
            for (SourceMagicDollPower object2 : nativeMagicDoll.type().powerDefinitions()) {
                if (object2 == null) continue;
                try {
                    if (!object2.className().equals("ACTIVE_REDUX") && (!object2.className().equals("ACTIVE_DMG") || object2.mode() != 2) || object2.probability() <= 0) continue;
                    defenseRolls.incrementAndGet();
                    if (ThreadLocalRandom.current().nextInt(1000) >= Math.min(1000, object2.probability())) continue;
                    n = (int)Math.min(Integer.MAX_VALUE, (long)n + (long)Math.max(0, object2.valueSub()));
                    arrayList.add(object2);
                }
                catch (RuntimeException sourceMagicDollPower) {
                    NativePlayerCommit.defenseCalcFailure(sourceMagicDollPower);
                }
            }
        }
        NativeMagicDollEffects.DefenseProc defenseProc = new NativeMagicDollEffects.DefenseProc(n, arrayList);
        if (nativeMagicDoll != null) {
            for (SourceMagicDollPower sourceMagicDollPower : arrayList) {
                calcTicket.defense.add(new DefenseVisual(nativeMagicDoll, sourceMagicDollPower.id(), 0, calcTicket.target.object, string + "-defense"));
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
        if (!CombatRules.loaded()) {
            return CombatAccess.integerCall(clientSession, "nativeMagicDollDamageAfterActiveReduction", "(IZLjava/lang/String;)I", n, bl, string);
        }
        NativePlayerCommit.calculation.get().incoming = NativeAcceptedState.capture(clientSession, n > 0);
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
            return CombatIncomingSource.world(clientSession, n, bl, string, d, illusionAvatar);
        }
        if (!(d > 0.0)) {
            NativePlayerCommit.calculation.get().feedbackPositive = false;
            return (int)(d * CombatIncomingSource.weapon(clientSession) * CombatIncomingSource.rate(clientSession));
        }
        WorldPlan worldPlan = worldCalculation.get();
        NativePlayerCommit.calculation.get().incoming = NativeAcceptedState.capture(clientSession, d > 0.0);
        NativePlayerCommit.prepareEquipment(clientSession, worldPlan != null && worldPlan.kind != 2 && worldPlan.suppress);
        NativeMagicDollEffects.DefenseProc defenseProc = NativePlayerCommit.defenseOrEmpty(clientSession, string);
        double d2 = Math.max(0.0, d - (double)defenseProc.damageReduction());
        boolean bl2 = NativePlayerCommit.calculation.get().feedbackPositive = d2 > 0.0;
        if (d2 > 0.0 && (worldPlan == null || worldPlan.kind == 2 || !worldPlan.suppress)) {
            NativePlayerCommit.captureNpcFeedback(clientSession);
        }
        return (int)(d2 * CombatIncomingSource.weapon(clientSession) * CombatIncomingSource.rate(clientSession));
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
        return String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") && clientSession2 != null && (!bl || NativePlayerCommit.sameWorld(clientSession, clientSession2));
    }

    private static boolean sameWorld(ClientSession clientSession, ClientSession clientSession2) {
        return (Boolean)CombatAccess.call(clientSession, "sameClassicWorldMap", "(Llocal/lito381/server/ClientSession;)Z", clientSession2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void calculateWorld(ClientSession clientSession, int n, long l, WorldPlan worldPlan) {
        WorldPlan worldPlan2 = worldCalculation.get();
        worldCalculation.set(worldPlan);
        try {
            CombatAccess.call(clientSession, "takeClassicWorldDirectDamage", "(DJ)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", n, l);
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
        if (bl && n > 0 && (n = CombatAccess.integerCall(clientSession, "earthBindProtectedCombatDamage", "(ILjava/lang/String;)I", n, "classic-world-player-" + clientSession2.playerName())) <= 0) {
            return;
        }
        BattleJoyOfPainStatus battleJoyOfPainStatus = (BattleJoyOfPainStatus)CombatAccess.field(clientSession2, "joyOfPainStatus");
        BattleJoyOfPainStatus.Lease lease = n > 0 && !bl2 && clientSession2 != clientSession ? battleJoyOfPainStatus.capture(System.nanoTime()) : null;
        int n2 = lease == null ? 0 : BattleJoyOfPain.feedbackDamage(NativePlayerCommit.integer(clientSession, "playerMaxHp"), NativePlayerCommit.integer(clientSession, "playerHp"), CombatAccess.integerCall(clientSession, "joyOfPainEquipmentAddedHp", "()I", new Object[0]));
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
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        if (binding.phase.equals("WORLD")) {
            for (ClientSession clientSession2 : (List)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", binding.map)) {
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
                        boolean bl3 = NativePlayerVitals.gm(clientSession);
                        NativeEquipmentDefense.Resolved resolved = !queue.closed && !queue.terminal && event.target.matchesFast(clientSession) && NativePlayerCommit.integer(clientSession, "playerHp") > 0 ? NativeEquipmentDefense.resolve(clientSession, clientSession2, event.equipment, event.attacker) : NativeEquipmentDefense.Resolved.empty();
                        queue.vitalLock.lock();
                        try {
                            int n2;
                            NativePlayerVitals.Result result;
                            if (queue.closed || queue.terminal || !event.target.matchesFast(clientSession) || !String.valueOf(CombatAccess.field(clientSession, "phase")).equals(event.world ? "WORLD" : "BATTLE")) {
                                dropped.incrementAndGet();
                                break block21;
                            }
                            int n3 = NativePlayerCommit.integer(clientSession, "playerMaxHp");
                            int n4 = NativePlayerCommit.integer(clientSession, "playerHp");
                            boolean bl4 = bl2 = n4 > 0;
                            if (bl2) {
                                result = NativePlayerVitals.calculate(n4, n3, event.damage, event.minimumHp, bl3);
                                n2 = result.after();
                            } else {
                                n2 = bl3 ? n4 : 0;
                                result = new NativePlayerVitals.Result(n2, true);
                                repairs.incrementAndGet();
                            }
                            queue.terminal = result.fatal();
                            if (n2 != n4) {
                                CombatAccess.set(clientSession, "playerHp", n2);
                            }
                            nativeAcceptedHpCompletion = NativeAcceptedHpCompletion.enqueue(new Frame(clientSession, clientSession2, queue, event, n4, n2, n3, result.fatal(), bl2, resolved));
                        }
                        finally {
                            queue.vitalLock.unlock();
                        }
                        NativePlayerVitalCompletions.drain(clientSession);
                        if (nativeAcceptedHpCompletion.suspend()) {
                            bl = true;
                            return;
                        }
                    }
                    catch (Throwable throwable) {
                        if (NativeAcceptedState.isStale(throwable)) {
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
                CombatPoisonLifecycle.lastDamage(clientSession, clientSession2);
            }
            Object object = CombatAccess.call(clientSession, "sourcePlayerWorldCommit", "(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;", 0.0, event.attackerObject, scope, event.npc, null, event.damage);
            WorldPlan worldPlan = event.plan;
            if (worldPlan != null && clientSession2 != null && NativePlayerCommit.validFrame(clientSession) && worldPlan.caster.matches(clientSession2)) {
                if (worldPlan.kind == 1 || worldPlan.kind == 2) {
                    NativePlayerCommit.accepted(clientSession, clientSession2, "liability", () -> CombatAccess.call(clientSession, "applyClassicWorldPlayerLiability", "(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;)V", clientSession2, object));
                }
                if (worldPlan.kind == 1 && event.feedbackPositive) {
                    if (!worldPlan.suppress && NativePlayerCommit.integer(clientSession2, "playerHp") > 0) {
                        NativePlayerCommit.safeMortal(clientSession, clientSession2, "classic-world-accepted-player-damage");
                    }
                    NativePlayerCommit.safeJoy(clientSession, clientSession2, worldPlan.joy, worldPlan.joyDamage, "classic-world-accepted-player-damage");
                } else if (worldPlan.kind == 3 && NativePlayerCommit.sameWorld(clientSession, clientSession2)) {
                    NativePlayerCommit.accepted(clientSession, clientSession2, "pink", () -> CombatAccess.call(clientSession2, "publishClassicWorldPinkName", "(Llocal/lito381/server/ClientSession;)V", clientSession));
                }
            }
            CombatPoisonLifecycle.finish(scope);
        }
        catch (Throwable throwable) {
            CombatPoisonLifecycle.failed(scope, throwable);
            throw throwable;
        }
    }

    static byte[] hitRatio(long l, int n, int n2) {
        Frame frame = current.get();
        return frame != null && (!frame.accepted || frame.before == frame.after) ? new byte[]{} : Protocol.hitRatio(l, n, n2);
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
        NativePlayerCommit.accepted(clientSession, clientSession2, "Mortal", () -> CombatAccess.call(clientSession, "tryMortalBodyPlayerReflection", "(Llocal/lito381/server/ClientSession;Ljava/lang/String;)V", clientSession2, string));
    }

    static void safeJoy(ClientSession clientSession, ClientSession clientSession2, BattleJoyOfPainStatus.Lease lease, int n, String string) {
        if (lease == null) {
            return;
        }
        NativePlayerCommit.accepted(clientSession, clientSession2, "Joy", () -> CombatAccess.call(clientSession, "applyJoyOfPainPlayerFeedback", "(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleJoyOfPainStatus$Lease;ILjava/lang/String;)V", clientSession2, lease, n, string));
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
            CombatAccess.call(clientSession, "sendEncrypted", "([BLjava/lang/String;)V", byArray, string);
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
            CombatAccess.call(clientSession, "broadcastOwnHitRatio", "(Ljava/lang/String;)V", string);
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
                byte[] byArray = defenseVisual.gfx > 0 ? Protocol.effectObject(defenseVisual.target, defenseVisual.gfx) : Protocol.objectAction(defenseVisual.doll.objectId(), 67);
                String string = defenseVisual.gfx > 0 ? "S_EFFECT magic-doll-proc=" + defenseVisual.power + " target=" + defenseVisual.target + " gfx=" + defenseVisual.gfx : "S_ACTION magic-doll-proc=" + defenseVisual.power + " doll=" + defenseVisual.doll.objectId() + " action=67";
                try {
                    for (ClientSession clientSession2 : (List)CombatAccess.call(clientSession, "nativeMagicDollRecipients", "(Llocal/lito381/server/NativeMagicDoll;)Ljava/util/List;", defenseVisual.doll)) {
                        if (!NativePlayerCommit.validFrame(clientSession)) continue block10;
                        NativePlayerCommit.safeSend(clientSession2, byArray, string + " defense=true reason=" + defenseVisual.reason);
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
        return frame != null && (frame.defenseActive || NativeEffectCompletions.defensePublishing(frame));
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
                CombatAccess.call(clientSession, "sourcePlayerBattleCommit", "(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V", event.damage, clientSession2, event.earth, event.suppress, event.terminal, event.joy, event.joyDamage);
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
        if (NativeAcceptedState.isStale(throwable)) {
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
        synchronized boolean offer(Event event) {
            Object object = this.life.admission;
            synchronized (object) {
                if (this.closed || this.retired || !this.life.admits() || this.events.size() >= TARGET || this.life.pending.get() >= GLOBAL) {
                    rejected.incrementAndGet();
                    return false;
                }
                this.events.add(event);
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
            this.events.clear();
            pending.addAndGet(-n);
            this.life.pending.addAndGet(-n);
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

    record Event(Binding target, Binding caster, int damage, boolean earth, boolean suppress, long terminal, BattleJoyOfPainStatus.Lease joy, int joyDamage, boolean world, long attackerObject, ClassicWorldMonsterPopulation.Snapshot npc, WorldPlan plan, List<DefenseVisual> defense, boolean feedbackPositive, List<NativeArmorProc.Rule> equipment, NativeCombatAttacker.Ref attacker, NativeCombatAttacker.Feedback npcFeedback, NativeAcceptedState.Incoming incoming, int minimumHp, long deadline) {
        Event {
            list = List.copyOf(list);
            list2 = List.copyOf(list2);
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
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            synchronized (battleTimedStatus2) {
                return new Binding(NativePlayerCommit.owner(clientSession), clientSession.playerObjectId(), clientSession.sourceCharacterIdentity(), clientSession.sourceAccountIdentity(), clientSession.selectedCharacterName(), String.valueOf(CombatAccess.field(clientSession, "phase")), NativePlayerCommit.number(clientSession, "currentRoomId"), NativePlayerCommit.integer(clientSession, "classicWorldMapId"), NativePlayerCommit.integer(clientSession, "classicWorldInstanceId"));
            }
        }

        boolean matches(ClientSession clientSession) {
            return this.equals(Binding.capture(clientSession));
        }

        boolean matchesFast(ClientSession clientSession) {
            return this.object == clientSession.playerObjectId() && this.source.equals(clientSession.sourceCharacterIdentity()) && this.account.equals(clientSession.sourceAccountIdentity()) && this.selected.equals(clientSession.selectedCharacterName()) && this.phase.equals(String.valueOf(CombatAccess.field(clientSession, "phase"))) && this.room == NativePlayerCommit.number(clientSession, "currentRoomId") && this.map == NativePlayerCommit.integer(clientSession, "classicWorldMapId") && this.instance == NativePlayerCommit.integer(clientSession, "classicWorldInstanceId");
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

        CalcTicket(Binding binding, Binding binding2, NativeCombatAttacker.Ref ref, CalcTicket calcTicket) {
            this.target = binding;
            this.caster = binding2;
            this.attacker = ref;
            this.previous = calcTicket;
        }
    }

    record DefenseVisual(NativeMagicDoll doll, int power, int gfx, long target, String reason) {
    }
}
