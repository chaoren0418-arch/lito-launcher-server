// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationBattleAction;
import local.lito381.server.NativeCancellationProbability;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.Protocol;

final class NativeCancellationMaterialAction {
    static void battle(ClientSession clientSession2, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        if (NativeCancellationBattleAction.cast(clientSession2, byArray, cancellationSpell, scope)) {
            return;
        }
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        ClientSession clientSession3 = gameHub.roomCombatants((Long)CombatAccess.field(clientSession2, "currentRoomId")).stream().filter(clientSession -> clientSession.playerObjectId() == objectSpellRequest.targetObjectId()).findFirst().orElse(clientSession2);
        NativeCancellationMaterialAction.prepared(clientSession2, clientSession3, () -> clientSession2.nativeMaterialCancellationBody(byArray, cancellationSpell, scope));
    }

    static void world(ClientSession clientSession, Protocol.ObjectSpellRequest objectSpellRequest, ClientSession clientSession2, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeCancellationMaterialAction.prepared(clientSession, clientSession2, () -> clientSession.nativeMaterialWorldCancellationBody(objectSpellRequest, clientSession2, cancellationSpell, scope));
    }

    private static void prepared(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task task) throws IOException {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativePlayerCommit.Binding binding2 = NativePlayerCommit.Binding.capture(clientSession2);
        NativeCancellationProbability.prepare(clientSession2);
        NativeLightAction.commitPair(clientSession, clientSession2, () -> {
            if (binding.matches(clientSession) && binding2.matches(clientSession2)) {
                task.run();
            }
        });
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 44) {
            throw new IllegalStateException("unreviewed cancellation material skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, n, n2, n3);
    }

    private NativeCancellationMaterialAction() {
    }
}
