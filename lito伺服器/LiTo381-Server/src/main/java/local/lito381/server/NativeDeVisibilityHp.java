// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeVisibilityAccess;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeVisibilityScope;

public class NativeDeVisibilityHp
extends NativeDeVisibilityPacket {
    private byte[] _byte = null;

    public NativeDeVisibilityHp(int n, int n2) {
        this.buildPacket(n, n2);
        this.debugPacket();
    }

    public NativeDeVisibilityHp(NativeDeCharacterFields nativeDeCharacterFields) {
        int n = NativeDeVisibilityAccess.L1Character$getId(nativeDeCharacterFields);
        int n2 = 100;
        if (0 < NativeDeVisibilityAccess.L1Character$getMaxHp(nativeDeCharacterFields)) {
            n2 = 100 * NativeDeVisibilityAccess.L1Character$getCurrentHp(nativeDeCharacterFields) / NativeDeVisibilityAccess.L1Character$getMaxHp(nativeDeCharacterFields);
        }
        this.buildPacket(n, n2);
        this.debugPacket();
    }

    private void buildPacket(int n, int n2) {
        this.writeC(NativeDeVisibilityScope.HP_OPCODE());
        this.writeD(n);
        this.writeC(n2);
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
