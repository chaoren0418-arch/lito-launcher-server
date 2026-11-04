// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts.Packet;
import local.lito381.server.NativeDeDeletionPorts.RemoveObject;
import local.lito381.server.NativePlayerLifetime.Mode;

import java.io.IOException;
import java.io.UncheckedIOException;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.NativeDeChatWire;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeVisibilityHp;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.Protocol;

final class NativeDeVisibilityOutput {
    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    static boolean accepts(Packet var0) {
      return var0 instanceof NativeDeVisibilityPacket || var0 instanceof RemoveObject var1 && !(var1.actor instanceof NativeDeDeletionState);
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void send(NativeDeClientViewer nativeDeClientViewer, NativeDeDeletionPorts.Packet packet) {
        ClientSession clientSession = NativeDeVisibilityRuntime.client(nativeDeClientViewer);
        if (clientSession == null) {
            throw new IllegalStateException("stale visibility viewer");
        }
        NativeDeWorldContext.Entry entry = NativeDeWorldContext.entry(nativeDeClientViewer);
        byte[] byArray;
        NativeDeVisibilityPacket nativeDeVisibilityPacket = null;
        NativeDeDeletionPorts.RemoveObject removeObject;
        if (packet instanceof NativeDeVisibilityPacket object) {
            nativeDeVisibilityPacket = object;
            try {
                byArray = object.prepareForWrite();
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
            if (object instanceof NativeDeVisibilityHp) {
                if (byArray.length < 6) {
                    throw new IllegalArgumentException("source HP packet truncated");
                }
                long l = (long)byArray[1] & 0xFFL | ((long)byArray[2] & 0xFFL) << 8 | ((long)byArray[3] & 0xFFL) << 16 | ((long)byArray[4] & 0xFFL) << 24;
                if (l == 0L) {
                    throw new IllegalArgumentException("native HP object ID zero");
                }
            }
        } else if (packet instanceof NativeDeDeletionPorts.RemoveObject) {
            removeObject = (NativeDeDeletionPorts.RemoveObject)packet;
            if (NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)removeObject.actor).context != entry.context) {
                throw new IllegalArgumentException("foreign generic source remove");
            }
            byArray = Protocol.removeObject(Integer.toUnsignedLong(((NativeDeObjectFields)removeObject.actor).getId()));
        } else {
            throw new IllegalArgumentException("unsupported source visibility packet");
        }
        synchronized (clientSession) {
            if (nativeDeClientViewer.retired || entry.context.closed || nativeDeClientViewer.life.mode == NativePlayerLifetime.Mode.STOPPED || !nativeDeClientViewer.binding.matchesFast(clientSession) || CombatAccess.field(CombatPoisonAtomic.status(clientSession), "sourceDeKnownViewer") != nativeDeClientViewer || nativeDeVisibilityPacket != null && !nativeDeVisibilityPacket.isValidForWrite()) {
                throw new IllegalStateException("retired visibility publication");
            }
            NativeDeVisibilityRuntime.requirePublication(nativeDeClientViewer);
            NativeDeChatWire.requirePublication(nativeDeClientViewer, packet);
            CombatAccess.call(clientSession, "sendEncryptedFrame", "([BLjava/lang/String;)V", byArray, "source visibility " + packet.getClass().getSimpleName());
        }
    }

    private NativeDeVisibilityOutput() {
    }
}

