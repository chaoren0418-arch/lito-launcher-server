// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;

public class NativeDePerceptionEmblem
extends NativeDePerceptionPacket {
    private byte[] _byte = null;

    public NativeDePerceptionEmblem(NativeDePerceptionEmblemData emblemIcon) {
        this.writeC(NativeDePerceptionOpcodes.S_Emblem());
        this.writeD(emblemIcon.get_clanid());
        this.writeByte(emblemIcon.get_clanIcon());
        this.debugPacket();
    }

    @Override
    public byte[] getContent() {
        if (this._byte == null) {
            this._byte = this.getBytes();
        }
        return this._byte;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }
}
