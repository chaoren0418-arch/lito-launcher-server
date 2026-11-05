// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeChatAccess;
import local.lito381.server.NativeDeChatWire;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeVisibilityPacket;

public class NativeDeChatChatGlobal
extends NativeDeVisibilityPacket {
    private byte[] _byte = null;
    final NativeDeDeletionPorts.ObjectRef nativeSubject;

    public NativeDeChatChatGlobal(NativeDeNpcConstructionState nativeDeNpcConstructionState, String string) {
        this.buildPacket(nativeDeNpcConstructionState, string);
        this.debugPacket();
        this.nativeSubject = nativeDeNpcConstructionState;
    }

    private void buildPacket(NativeDeNpcConstructionState nativeDeNpcConstructionState, String string) {
        this.writeC(161);
        this.writeC(3);
        this.writeD(NativeDeChatAccess.L1NpcInstance$getId(nativeDeNpcConstructionState));
        this.writeS("[" + NativeDeChatAccess.L1NpcInstance$getNameId(nativeDeNpcConstructionState) + "] " + string);
    }

    private byte[] sourceChat$getContent() {
        if (this._byte == null) {
            this._byte = this.getBytes();
        }
        return this._byte;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public byte[] getContent() {
        return NativeDeChatWire.content(this, this.nativeSubject);
    }
}
