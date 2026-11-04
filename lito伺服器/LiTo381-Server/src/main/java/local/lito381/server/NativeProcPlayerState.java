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
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.NativePlayerCommit;

final class NativeProcPlayerState {
    private Identity identity;
    private final Map<Integer, Long> modifiers = new HashMap<Integer, Long>();
    private boolean show = true;

    NativeProcPlayerState() {
    }

    private static NativeProcPlayerState owned(ClientSession clientSession) {
        Identity identity;
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        if (!Thread.holdsLock(battleTimedStatus)) {
            throw new IllegalStateException("Proc state requires player status monitor");
        }
        NativeProcPlayerState nativeProcPlayerState = (NativeProcPlayerState)CombatAccess.field(battleTimedStatus, "sourceProcPlayerState");
        if (nativeProcPlayerState == null) {
            nativeProcPlayerState = new NativeProcPlayerState();
            CombatAccess.set(battleTimedStatus, "sourceProcPlayerState", nativeProcPlayerState);
        }
        if (!(identity = new Identity(clientSession.sourceCharacterIdentity(), clientSession.sourceAccountIdentity(), clientSession.selectedCharacterName())).equals(nativeProcPlayerState.identity)) {
            nativeProcPlayerState.modifiers.clear();
            nativeProcPlayerState.show = true;
            nativeProcPlayerState.identity = identity;
        }
        return nativeProcPlayerState;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean active(ClientSession clientSession, int n, long l) {
        BattleTimedStatus battleTimedStatus;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus2) {
            NativeProcPlayerState nativeProcPlayerState = NativeProcPlayerState.owned(clientSession);
            nativeProcPlayerState.modifiers.values().removeIf(l2 -> l >= l2);
            return nativeProcPlayerState.modifiers.containsKey(n);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean remove(ClientSession clientSession, int n) {
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus) {
            return NativeProcPlayerState.owned((ClientSession)clientSession).modifiers.remove(n) != null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean activate(ClientSession clientSession, NativePlayerCommit.Binding binding, int n, long l, int n2) {
        if (!Set.of(Integer.valueOf(164), Integer.valueOf(170), Integer.valueOf(173), Integer.valueOf(4012)).contains(n) || l < 0L || n2 <= 0) {
            throw new IllegalArgumentException("unsupported Proc healing modifier");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n2));
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus) {
            if (binding == null || !binding.matches(clientSession)) {
                return false;
            }
            NativeProcPlayerState.owned((ClientSession)clientSession).modifiers.put(n, l2);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void show(ClientSession clientSession, boolean bl) {
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus) {
            NativeProcPlayerState.owned((ClientSession)clientSession).show = bl;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean show(ClientSession clientSession) {
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus) {
            return NativeProcPlayerState.owned((ClientSession)clientSession).show;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clear(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeProcPlayerState nativeProcPlayerState = (NativeProcPlayerState)CombatAccess.field(battleTimedStatus, "sourceProcPlayerState");
            if (nativeProcPlayerState != null) {
                nativeProcPlayerState.modifiers.clear();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void selected(ClientSession clientSession) {
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        if (battleTimedStatus == null) {
            return;
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            CombatAccess.set(battleTimedStatus, "sourceProcPlayerState", null);
        }
    }

    static void selectedField(ClientSession clientSession, String string) {
        if (string.equals("selectedCharacter")) {
            NativeProcPlayerState.selected(clientSession);
        }
    }

    private record Identity(String source, String account, String character) {
    }
}
