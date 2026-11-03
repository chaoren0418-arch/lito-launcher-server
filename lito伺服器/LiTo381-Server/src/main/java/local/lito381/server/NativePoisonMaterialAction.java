// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeLightAction;
import local.lito381.server.Protocol;

final class NativePoisonMaterialAction {
    private static ClientSession target(ClientSession clientSession2, byte[] byArray, boolean bl) {
        Protocol.AttackSpellRequest attackSpellRequest = Protocol.attackSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        List list = bl ? (List)CombatAccess.call(clientSession2, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession2, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession2, "currentRoomId"));
        return list.stream().filter(clientSession -> clientSession.playerObjectId() == attackSpellRequest.targetObjectId()).findFirst().orElse(clientSession2);
    }

    static void poisonBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PoisonSpell poisonSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, false), () -> {
            CombatPoisonLifecycle.Scope scope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialPoisonScopeBody(byArray, poisonSpell, scope);
            }
            catch (IOException | Error | RuntimeException throwable) {
                CombatPoisonLifecycle.failed(scope, throwable);
                throw throwable;
            }
        });
    }

    static void poisonWorld(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PoisonSpell poisonSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, true), () -> {
            CombatPoisonLifecycle.Scope scope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialWorldPoisonScopeBody(byArray, poisonSpell, scope);
            }
            catch (IOException | Error | RuntimeException throwable) {
                CombatPoisonLifecycle.failed(scope, throwable);
                throw throwable;
            }
        });
    }

    static void paralysisBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ParalysisSpell paralysisSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, false), () -> {
            CombatPoisonLifecycle.Scope scope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialParalysisScopeBody(byArray, paralysisSpell, scope);
            }
            catch (IOException | Error | RuntimeException throwable) {
                CombatPoisonLifecycle.failed(scope, throwable);
                throw throwable;
            }
        });
    }

    static void paralysisWorld(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ParalysisSpell paralysisSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, true), () -> {
            CombatPoisonLifecycle.Scope scope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialWorldParalysisScopeBody(byArray, paralysisSpell, scope);
            }
            catch (IOException | Error | RuntimeException throwable) {
                CombatPoisonLifecycle.failed(scope, throwable);
                throw throwable;
            }
        });
    }

    private static CombatPoisonLifecycle.Scope open() {
        CombatPoisonLifecycle.Scope scope = CombatPoisonLifecycle.open();
        NativeLightAction.finishScoped((bl, throwable) -> {
            if (bl && throwable == null) {
                CombatPoisonLifecycle.finish(scope);
            } else {
                CombatPoisonLifecycle.failed(scope, throwable != null ? throwable : new CancellationException("cast publication retired"));
            }
        });
        return scope;
    }

    private static boolean open(CombatPoisonLifecycle.Scope scope) {
        return scope == null || (Boolean)CombatAccess.field(scope, "closed") == false;
    }

    static void palette(ClientSession clientSession, int n, String string, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatPoisonLifecycle.palette(clientSession, n, string, scope);
            }
        });
    }

    static void monsterSend(ClientSession clientSession, byte[] byArray, String string, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatMonsterCurseLifecycle.send(clientSession, byArray, string, scope);
            }
        });
    }

    static void monsterPaletteFinished(byte[] byArray, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatMonsterCurseLifecycle.paletteFinished(byArray, scope);
            }
        });
    }

    static void poisonPaletteFinished(ClientSession clientSession, CombatPoisonLifecycle.Scope scope, boolean bl, int n) {
        if (clientSession == null || scope == null || !bl) {
            return;
        }
        NativeLightAction.deferScoped(() -> {
            BattleTimedStatus battleTimedStatus;
            if (!NativePoisonMaterialAction.open(scope)) {
                return;
            }
            CombatPoisonLifecycle.Context context = scope.find(clientSession);
            if (context == null) {
                return;
            }
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            synchronized (battleTimedStatus2) {
                CombatPoisonLifecycle.Lease lease = (CombatPoisonLifecycle.Lease)CombatAccess.field(context, "producer");
                CombatPoisonLifecycle.State state = (CombatPoisonLifecycle.State)CombatAccess.field(context, "state");
                long l = System.nanoTime();
                if (CombatAccess.field(context, "timed") == battleTimedStatus && ((Boolean)CombatAccess.field(context, "producerActive")).booleanValue() && state.isCurrent(lease) && battleTimedStatus.poisonActive(l)) {
                    CombatAccess.set(battleTimedStatus, "poisonNextTickAtNanos", Math.addExact(l, TimeUnit.MILLISECONDS.toNanos(n)));
                    lease.started = true;
                }
            }
        });
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 11 && n != 33) {
            throw new IllegalStateException("unreviewed poison/paralysis material skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, n, n2, n3);
    }

    private NativePoisonMaterialAction() {
    }
}
