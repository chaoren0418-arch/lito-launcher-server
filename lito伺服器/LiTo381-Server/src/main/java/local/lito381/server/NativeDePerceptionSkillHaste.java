// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;

public class NativeDePerceptionSkillHaste
extends NativeDePerceptionPacket {
    private byte[] _byte = null;

    public NativeDePerceptionSkillHaste(int objid, int mode, int time) {
        this.writeC(NativeDePerceptionOpcodes.S_SkillHaste());
        this.writeD(objid);
        this.writeC(mode);
        this.writeH(time);
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
