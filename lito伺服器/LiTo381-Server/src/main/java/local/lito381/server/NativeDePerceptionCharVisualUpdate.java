// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;

public class NativeDePerceptionCharVisualUpdate
extends NativeDePerceptionPacket {
    private byte[] _byte = null;

    public NativeDePerceptionCharVisualUpdate(int objid, int weaponType) {
        this.writeC(NativeDePerceptionOpcodes.S_CharVisualUpdate());
        this.writeD(objid);
        this.writeC(weaponType);
        this.debugPacket();
    }

    @Override
    public byte[] getContent() {
        if (this._byte == null) {
            this._byte = this.getBytes();
        }
        return this._byte;
    }
}
