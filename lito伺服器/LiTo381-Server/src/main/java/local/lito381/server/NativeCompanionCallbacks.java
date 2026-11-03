// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeCompanionCommands;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.Protocol;

final class NativeCompanionCallbacks {
    private static final VarHandle slave = NativeCompanionCallbacks.handle("pendingSlaveTarget");
    private static final VarHandle rename = NativeCompanionCallbacks.handle("pendingPetRename");

    NativeCompanionCallbacks() {
    }

    private static VarHandle handle(String string) {
        try {
            Field field = ClientSession.class.getDeclaredField(string);
            return MethodHandles.privateLookupIn(ClientSession.class, MethodHandles.lookup()).findVarHandle(ClientSession.class, string, field.getType());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Stamp capture(NativePlayerCommit.Binding binding, BattleCompanion battleCompanion) {
        if (binding == null || battleCompanion == null) {
            return null;
        }
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            return new Stamp(binding, battleCompanion.snapshot().objectId(), NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(battleCompanion, "timedStatus"), true));
        }
    }

    static void attach(Object object, Stamp stamp) {
        CombatAccess.set(object, "sourceCompanionCallback", stamp);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void advance(Object object) {
        BattleTimedStatus battleTimedStatus;
        BattleCompanion battleCompanion = (BattleCompanion)object;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = (BattleTimedStatus)CombatAccess.field(battleCompanion, "timedStatus");
        synchronized (battleTimedStatus2) {
            CombatPoisonLifecycle.State state = CombatPoisonLifecycle.monsterPoisonCallbacks(battleTimedStatus, true);
            CombatAccess.set(state, "sourceNpcOwner", new Object());
        }
    }

    private static Stamp stamp(Object object) {
        return object == null ? null : (Stamp)CombatAccess.field(object, "sourceCompanionCallback");
    }

    private static boolean master(ClientSession clientSession, Stamp stamp) {
        if (stamp == null || !stamp.master.matches(clientSession)) {
            return false;
        }
        if (stamp.master.phase().equals("WORLD")) {
            return NativeCompanionCommands.registered(clientSession);
        }
        if (!stamp.master.phase().equals("BATTLE")) {
            return false;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        for (ClientSession clientSession2 : gameHub.roomMembers(stamp.master.room())) {
            if (clientSession2 != clientSession) continue;
            return true;
        }
        return false;
    }

    private static boolean claim(ClientSession clientSession, Object object, VarHandle varHandle) {
        if (object == null || varHandle.getVolatile(clientSession) != object) {
            return false;
        }
        Stamp stamp = NativeCompanionCallbacks.stamp(object);
        long l = ((Number)CombatAccess.field(object, "expiresAtNanos")).longValue();
        if (!NativeCompanionCallbacks.master(clientSession, stamp) || System.nanoTime() - l >= 0L) {
            varHandle.compareAndSet(clientSession, object, null);
            return false;
        }
        return stamp.claimed.compareAndSet(false, true) && varHandle.compareAndSet(clientSession, object, object);
    }

    static boolean claimSlave(ClientSession clientSession, Object object, Protocol.SlaveControlRequest slaveControlRequest) {
        if (object == null || ((Number)CombatAccess.field(object, "controllerObjectId")).longValue() != slaveControlRequest.controllerObjectId() || ((Number)CombatAccess.field(object, "mode")).intValue() != slaveControlRequest.mode()) {
            return false;
        }
        return NativeCompanionCallbacks.claim(clientSession, object, slave);
    }

    static boolean claimRename(ClientSession clientSession, Object object, Protocol.AskAnswer askAnswer) {
        return askAnswer.questionId() == 325 && NativeCompanionCallbacks.claim(clientSession, object, rename);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean ready(ClientSession clientSession, Object object, BattleCompanion battleCompanion, VarHandle varHandle) {
        Object object2;
        boolean bl;
        Stamp stamp = NativeCompanionCallbacks.stamp(object);
        if (battleCompanion == null || stamp == null || !stamp.claimed.get() || varHandle.getVolatile(clientSession) != object) {
            return false;
        }
        boolean bl2 = bl = CombatAccess.field(clientSession, "battleCompanion") == battleCompanion;
        if (!bl) {
            object2 = (List)CombatAccess.field(clientSession, "tamedCompanions");
            List list = object2;
            synchronized (list) {
                for (int i = 0; i < object2.size(); ++i) {
                    if (object2.get(i) != battleCompanion) continue;
                    bl = true;
                    break;
                }
            }
        }
        if (!bl || !NativeCompanionCallbacks.master(clientSession, stamp)) {
            return false;
        }
        object2 = battleCompanion;
        synchronized (object2) {
            return battleCompanion.snapshot().objectId() == stamp.companion && stamp.life == NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(battleCompanion, "timedStatus"), false) && varHandle.getVolatile(clientSession) == object;
        }
    }

    static boolean readySlave(ClientSession clientSession, Object object, BattleCompanion battleCompanion) {
        return NativeCompanionCallbacks.ready(clientSession, object, battleCompanion, slave);
    }

    static boolean readyRename(ClientSession clientSession, Object object, BattleCompanion battleCompanion) {
        return NativeCompanionCallbacks.ready(clientSession, object, battleCompanion, rename);
    }

    static void finishSlave(ClientSession clientSession, Object object, boolean bl) {
        if (bl) {
            slave.compareAndSet(clientSession, object, null);
        }
    }

    static void finishRename(ClientSession clientSession, Object object, boolean bl) {
        if (bl) {
            rename.compareAndSet(clientSession, object, null);
        }
    }

    static final class Stamp {
        final NativePlayerCommit.Binding master;
        final long companion;
        final Object life;
        final AtomicBoolean claimed = new AtomicBoolean();

        Stamp(NativePlayerCommit.Binding binding, long l, Object object) {
            this.master = binding;
            this.companion = l;
            this.life = object;
        }
    }
}
