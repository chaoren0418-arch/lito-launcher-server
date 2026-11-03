// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.Protocol;

final class NativeResurrectionAnswerLife {
    static void capture(ClientSession clientSession, Object object) {
        clientSession.nativeResurrectionAnswerLease = object == null ? null : new Lease(clientSession, object);
    }

    private static Object pending(ClientSession clientSession) {
        return CombatAccess.field(clientSession, "pendingResurrection");
    }

    private static void clear(ClientSession clientSession, Object object) throws IOException {
        NativeLightAction.commitPair(clientSession, clientSession, () -> {
            if (NativeResurrectionAnswerLife.pending(clientSession) == object) {
                clientSession.nativeRejectResurrectionQuestion();
            }
        });
    }

    static void answer(ClientSession clientSession, Protocol.AskAnswer askAnswer) throws IOException {
        Object object = NativeResurrectionAnswerLife.pending(clientSession);
        Lease lease = clientSession.nativeResurrectionAnswerLease;
        if (object == null) {
            NativeLightAction.commitPair(clientSession, clientSession, () -> {
                if (NativeResurrectionAnswerLife.pending(clientSession) == null) {
                    clientSession.nativeResurrectionAnswerBody(askAnswer);
                }
            });
            return;
        }
        if (lease == null || lease.pending != object || !lease.valid(clientSession)) {
            NativeResurrectionAnswerLife.clear(clientSession, object);
            return;
        }
        boolean[] blArray = new boolean[]{false};
        NativeLightAction.commitPair(clientSession, lease.caster, () -> {
            blArray[0] = true;
            if (NativeResurrectionAnswerLife.pending(clientSession) != object) {
                return;
            }
            if (!lease.valid(clientSession)) {
                clientSession.nativeRejectResurrectionQuestion();
                return;
            }
            clientSession.nativeResurrectionAnswerBody(askAnswer);
        });
        if (!blArray[0]) {
            NativeResurrectionAnswerLife.clear(clientSession, object);
        }
    }

    private NativeResurrectionAnswerLife() {
    }

    static final class Lease {
        final Object pending;
        final ClientSession caster;
        final NativePlayerCommit.Binding casterBinding;
        final NativePlayerCommit.Binding targetBinding;

        Lease(ClientSession clientSession, Object object) {
            this.pending = object;
            this.caster = (ClientSession)CombatAccess.field(object, "caster");
            this.casterBinding = NativePlayerCommit.Binding.capture(this.caster);
            this.targetBinding = NativePlayerCommit.Binding.capture(clientSession);
        }

        boolean valid(ClientSession clientSession) {
            return this.casterBinding.matches(this.caster) && this.targetBinding.matches(clientSession) && (Boolean)CombatAccess.field(this.caster, "sourceItemDisconnected") == false && (Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected") == false;
        }
    }
}
