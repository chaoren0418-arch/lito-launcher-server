// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;

public class NativeDePerceptionSkillBrave
extends NativeDePerceptionPacket {
    public NativeDePerceptionSkillBrave(int objid, int mode, int time) {
        this.writeC(NativeDePerceptionOpcodes.S_SkillBrave());
        this.writeD(objid);
        this.writeC(mode);
        this.writeH(time);
        this.debugPacket();
    }

    @Override
    public byte[] getContent() {
        return this.getBytes();
    }
}
