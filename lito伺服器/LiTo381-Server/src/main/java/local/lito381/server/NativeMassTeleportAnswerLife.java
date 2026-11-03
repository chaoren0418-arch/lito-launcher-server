// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.Protocol;

final class NativeMassTeleportAnswerLife {
    static Object token(ClientSession clientSession) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        reentrantLock.lock();
        try {
            if (clientSession.nativeMassTeleportRoleToken == null) {
                clientSession.nativeMassTeleportRoleToken = new Object();
            }
            Object object = clientSession.nativeMassTeleportRoleToken;
            return object;
        }
        finally {
            reentrantLock.unlock();
        }
    }

    static void retireRole(ClientSession clientSession) {
        clientSession.nativeMassTeleportRoleToken = new Object();
        clientSession.nativeRejectMassTeleportQuestion();
    }

    static void capture(ClientSession clientSession, Object object) {
        clientSession.nativeMassTeleportAnswerLease = object == null ? null : new Lease(clientSession, object);
    }

    private static Object pending(ClientSession clientSession) {
        return CombatAccess.field(clientSession, "pendingMassTeleport");
    }

    private static void clear(ClientSession clientSession, Object object) {
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        reentrantLock.lock();
        try {
            if (NativeMassTeleportAnswerLife.pending(clientSession) == object) {
                clientSession.nativeRejectMassTeleportQuestion();
            }
        }
        finally {
            reentrantLock.unlock();
        }
    }

    static void answer(ClientSession clientSession, Protocol.AskAnswer askAnswer) throws IOException {
        Object object = NativeMassTeleportAnswerLife.pending(clientSession);
        Lease lease = clientSession.nativeMassTeleportAnswerLease;
        if (object == null) {
            NativeLightAction.commitPair(clientSession, clientSession, () -> {
                if (NativeMassTeleportAnswerLife.pending(clientSession) == null) {
                    clientSession.nativeMassTeleportAnswerBody(askAnswer);
                }
            });
            return;
        }
        if (lease == null || lease.pending != object || !lease.valid(clientSession)) {
            NativeMassTeleportAnswerLife.clear(clientSession, object);
            return;
        }
        boolean[] blArray = new boolean[]{false};
        NativeLightAction.commitPair(clientSession, lease.caster, () -> {
            blArray[0] = true;
            if (NativeMassTeleportAnswerLife.pending(clientSession) != object) {
                return;
            }
            if (!lease.valid(clientSession)) {
                clientSession.nativeRejectMassTeleportQuestion();
                return;
            }
            clientSession.nativeMassTeleportAnswerBody(askAnswer);
        });
        if (!blArray[0]) {
            NativeMassTeleportAnswerLife.clear(clientSession, object);
        }
    }

    static void publish(ClientSession clientSession, byte[] byArray, String string) {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.finishScoped((bl, throwable) -> {
            try {
                NativeLightAction.commitPair(clientSession, clientSession, () -> {
                    if (!binding.matches(clientSession)) {
                        return;
                    }
                    Runnable runnable = NativeRolePublication.completion(clientSession, () -> {
                        if (binding.matches(clientSession)) {
                            try {
                                clientSession.nativeSelfOriginalSend(byArray2, string);
                            }
                            catch (IOException iOException) {
                                throw new UncheckedIOException(iOException);
                            }
                        }
                    });
                    NativeLightAction.finishScoped((bl, throwable) -> runnable.run());
                });
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        });
    }

    private NativeMassTeleportAnswerLife() {
    }

    static final class Lease {
        final Object pending;
        final Object casterRole;
        final Object targetRole;
        final ClientSession caster;
        final NativePlayerCommit.Binding casterBinding;
        final NativePlayerCommit.Binding targetBinding;

        Lease(ClientSession clientSession, Object object) {
            this.pending = object;
            this.caster = (ClientSession)CombatAccess.field(object, "caster");
            this.casterRole = NativeMassTeleportAnswerLife.token(this.caster);
            this.targetRole = NativeMassTeleportAnswerLife.token(clientSession);
            this.casterBinding = NativePlayerCommit.Binding.capture(this.caster);
            this.targetBinding = NativePlayerCommit.Binding.capture(clientSession);
        }

        boolean valid(ClientSession clientSession) {
            boolean bl;
            if (this.casterRole != this.caster.nativeMassTeleportRoleToken || this.targetRole != clientSession.nativeMassTeleportRoleToken || !this.targetBinding.matches(clientSession) || ((Boolean)CombatAccess.field(this.caster, "sourceItemDisconnected")).booleanValue() || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue()) {
                return false;
            }
            if (!String.valueOf(CombatAccess.field(this.pending, "originPhase")).equals("WORLD")) {
                return this.casterBinding.matches(this.caster);
            }
            NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(this.caster);
            Protocol.PortalDestination portalDestination = (Protocol.PortalDestination)CombatAccess.field(this.pending, "destination");
            boolean bl2 = binding.object() == this.casterBinding.object() && binding.source().equals(this.casterBinding.source()) && binding.account().equals(this.casterBinding.account()) && binding.selected().equals(this.casterBinding.selected()) && binding.phase().equals("WORLD") && binding.room() == this.casterBinding.room();
            boolean bl3 = bl = binding.map() == this.casterBinding.map() && binding.instance() == this.casterBinding.instance();
            boolean bl4 = portalDestination != null && binding.map() == portalDestination.mapId() && binding.instance() == (portalDestination.mapId() == this.casterBinding.map() ? this.casterBinding.instance() : -1);
            return bl2 && (bl || bl4);
        }
    }
}
