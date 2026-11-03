// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;

public class NativeDePerceptionDoActionShop
extends NativeDePerceptionPacket {
    public NativeDePerceptionDoActionShop(int object, String message) {
        this.writeC(NativeDePerceptionOpcodes.S_DoActionShop());
        this.writeD(object);
        this.writeC(70);
        this.writeS(message);
        this.debugPacket();
    }

    @Override
    public byte[] getContent() {
        return this.getBytes();
    }
}
