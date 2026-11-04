// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeChatChat;
import local.lito381.server.NativeDeChatChatGlobal;
import local.lito381.server.NativeDeChatChatShouting;
import local.lito381.server.NativeDeChatJobs;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeNpcVitalState;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.Protocol;

final class NativeDeChatWire {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static byte[] content(NativeDeVisibilityPacket nativeDeVisibilityPacket, NativeDeDeletionPorts.ObjectRef objectRef) {
        synchronized (nativeDeVisibilityPacket) {
            byte[] byArray = (byte[])CombatAccess.call(nativeDeVisibilityPacket, "sourceChat$getContent", "()[B", new Object[0]);
            if (byArray.length < 7 || (byArray[0] & 0xFF) != 161) {
                throw new IllegalArgumentException("source NPC chat layout");
            }
            Protocol.Reader reader = new Protocol.Reader(byArray);
            reader.c();
            int n = reader.c();
            long l = reader.d();
            String string = reader.s();
            if (l != Integer.toUnsignedLong(((NativeDeCharacterFields)objectRef).getId())) {
                throw new IllegalArgumentException("source NPC speaker identity");
            }
            while (reader.remaining() > 0) {
                if (reader.c() == 0) continue;
                throw new IllegalArgumentException("source NPC chat trailing bytes");
            }
            NativeDeCharacterFields nativeDeCharacterFields = (NativeDeCharacterFields)objectRef;
            byte[] byArray2 = Protocol.sayCode(n, l, string, nativeDeCharacterFields.getX(), nativeDeCharacterFields.getY());
            CombatAccess.set(nativeDeVisibilityPacket, "_byteCache", byArray2);
            return byArray2;
        }
    }

    static NativeDeDeletionPorts.ObjectRef subject(NativeDeDeletionPorts.Packet packet) {
        NativeDeDeletionPorts.ObjectRef objectRef;
        if (packet instanceof NativeDeChatChat) {
            NativeDeChatChat nativeDeChatChat = (NativeDeChatChat)packet;
            objectRef = nativeDeChatChat.nativeSubject;
        } else if (packet instanceof NativeDeChatChatGlobal) {
            NativeDeChatChatGlobal nativeDeChatChatGlobal = (NativeDeChatChatGlobal)packet;
            objectRef = nativeDeChatChatGlobal.nativeSubject;
        } else if (packet instanceof NativeDeChatChatShouting) {
            NativeDeChatChatShouting nativeDeChatChatShouting = (NativeDeChatChatShouting)packet;
            objectRef = nativeDeChatChatShouting.nativeSubject;
        } else {
            objectRef = null;
        }
        return objectRef;
    }

    static void requirePublication(NativeDeClientViewer nativeDeClientViewer, NativeDeDeletionPorts.Packet packet) {
        Object object;
        NativeDeDeletionPorts.ObjectRef objectRef = NativeDeChatWire.subject(packet);
        if (objectRef == null) {
            return;
        }
        NativeDeWorldContext.Entry entry = NativeDeWorldContext.entry(objectRef);
        if (entry.context.closed || NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeClientViewer).context != entry.context || objectRef instanceof NativeDeDeletionState && (((NativeDeNpcVitalState)(object = (NativeDeDeletionState)objectRef)).destroyed() || ((NativeDeNpcVitalState)object).deletionStarted())) {
            throw new RejectedExecutionException("NPC chat publication retired");
        }
        object = NativeDeChatJobs.find(entry.context);
        if (object == null || ((NativeDeChatJobs.Owner)object).closed) {
            throw new RejectedExecutionException("NPC chat owner retired");
        }
    }

    private NativeDeChatWire() {
    }
}
