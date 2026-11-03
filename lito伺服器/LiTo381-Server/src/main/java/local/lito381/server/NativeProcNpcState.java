// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeCombatAttacker;

final class NativeProcNpcState {
    private final Object life;
    private final Map<Integer, Long> deadlines = new HashMap<Integer, Long>();

    private NativeProcNpcState(Object object) {
        this.life = object;
    }

    private static NativeProcNpcState owned(BattleTimedStatus battleTimedStatus) {
        if (!Thread.holdsLock(battleTimedStatus)) {
            throw new IllegalStateException("NPC Proc state requires status monitor");
        }
        Object object = NativeCombatAttacker.token(battleTimedStatus, true);
        NativeProcNpcState nativeProcNpcState = (NativeProcNpcState)CombatAccess.field(battleTimedStatus, "sourceProcNpcState");
        if (nativeProcNpcState == null || nativeProcNpcState.life != object) {
            nativeProcNpcState = new NativeProcNpcState(object);
            CombatAccess.set(battleTimedStatus, "sourceProcNpcState", nativeProcNpcState);
        }
        return nativeProcNpcState;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean active(BattleTimedStatus battleTimedStatus, int n, long l) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeProcNpcState nativeProcNpcState = NativeProcNpcState.owned(battleTimedStatus);
            nativeProcNpcState.deadlines.values().removeIf(l2 -> l >= l2);
            return nativeProcNpcState.deadlines.containsKey(n);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void activate(BattleTimedStatus battleTimedStatus, int n, long l, int n2) {
        if (!Set.of(Integer.valueOf(164), Integer.valueOf(170), Integer.valueOf(173), Integer.valueOf(4012)).contains(n) || l < 0L || n2 <= 0) {
            throw new IllegalArgumentException("unsupported NPC Proc modifier");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n2));
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeProcNpcState.owned((BattleTimedStatus)battleTimedStatus).deadlines.put(n, l2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clear(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            CombatAccess.set(battleTimedStatus, "sourceProcNpcState", null);
        }
    }
}
