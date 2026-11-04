// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDePerceptionEmblem;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDePerceptionState;
import local.lito381.server.NativePlayerLifetime;

final class NativeDePerceptionOutput {
    NativeDePerceptionOutput() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void send(NativeDeClientViewer nativeDeClientViewer, NativeDePerceptionPacket nativeDePerceptionPacket) {
        NativeDePerceptionState nativeDePerceptionState = nativeDePerceptionPacket.subject;
        if (nativeDePerceptionState.deletionOwner.timer.life != nativeDeClientViewer.life) {
            throw new IllegalArgumentException("foreign perception application");
        }
        ClientSession clientSession = (ClientSession)CombatAccess.call(nativeDeClientViewer, "current", "()Llocal/lito381/server/ClientSession;", new Object[0]);
        if (clientSession == null) {
            throw new IllegalStateException("stale perception viewer");
        }
        byte[] byArray = NativeDePerceptionScope.call(nativeDePerceptionState, () -> {
            try {
                return nativeDePerceptionPacket.prepareForWrite();
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        });
        if (byArray == null) {
            throw new IllegalStateException("null perception payload");
        }
        if (nativeDePerceptionState.getId() == 0) {
            throw new IllegalArgumentException("native perception object ID is zero");
        }
        if (nativeDePerceptionPacket instanceof NativeDePerceptionEmblem && byArray.length < 389) {
            throw new IllegalArgumentException("native emblem requires 384 pixel bytes");
        }
        ClientSession clientSession2 = clientSession;
        synchronized (clientSession2) {
            if (!NativeDePerceptionOutput.current(nativeDeClientViewer, clientSession, nativeDePerceptionState) || !nativeDePerceptionPacket.isValidForWrite()) {
                nativeDePerceptionPacket.onInvalidForWrite();
                throw new IllegalStateException("stale perception publication");
            }
            CombatAccess.call(clientSession, "sendEncryptedFrame", "([BLjava/lang/String;)V", byArray, "source-De perception " + nativeDePerceptionPacket.getType() + " id=" + Integer.toUnsignedLong(nativeDePerceptionState.getId()));
        }
    }

    private static boolean current(NativeDeClientViewer nativeDeClientViewer, ClientSession clientSession, NativeDePerceptionState nativeDePerceptionState) {
        return !nativeDeClientViewer.retired && nativeDeClientViewer.life.mode != NativePlayerLifetime.Mode.STOPPED && nativeDeClientViewer.binding.matchesFast(clientSession) && CombatAccess.field(CombatPoisonAtomic.status(clientSession), "sourceDeKnownViewer") == nativeDeClientViewer && !nativeDePerceptionState.destroyed() && !nativeDePerceptionState.deletionStarted() && !nativeDePerceptionState.context().closed && nativeDeClientViewer.knownsObject(nativeDePerceptionState) && nativeDePerceptionState._registeredViewers.get(nativeDeClientViewer.getId()) == nativeDeClientViewer;
    }
}
