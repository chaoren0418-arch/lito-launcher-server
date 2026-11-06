// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDePerceptionOpcodes;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDePerceptionSpeed;
import local.lito381.server.NativeDePerceptionState;

public class NativeDePerceptionNPCPack_De
extends NativeDePerceptionPacket {
    private static final int STATUS_POISON = 1;
    private static final int STATUS_INVISIBLE = 2;
    private static final int STATUS_PC = 4;
    private static final int STATUS_BRAVE = 16;
    private static final int STATUS_ELFBRAVE = 32;
    private static final int STATUS_FASTMOVABLE = 64;
    private byte[] _byte = null;

    public NativeDePerceptionNPCPack_De(NativeDePerceptionState de) {
        int status = 4;
        NativeDePerceptionSpeed.SpeedState speedState = NativeDePerceptionSpeed.stateOf(de);
        if (de.getPoison() != null && de.getPoison().getEffectId() == 1) {
            status |= 1;
        }
        if (de.isInvisble()) {
            status |= 2;
        }
        if (speedState.brave()) {
            status |= 0x10;
        }
        if (speedState.elfBrave()) {
            status |= 0x10;
            status |= 0x20;
        }
        if (speedState.fastMovable()) {
            status |= 0x40;
        }
        this.writeC(NativeDePerceptionOpcodes.S_NPCPack_De());
        this.writeH(de.getX());
        this.writeH(de.getY());
        this.writeD(de.getId());
        this.writeH(de.getTempCharGfx());
        this.writeC(de.getStatus());
        this.writeC(de.getHeading());
        this.writeC(de.getChaLightSize());
        this.writeC(de.getMoveSpeed());
        this.writeD(0);
        this.writeH(de.getLawful());
        this.writeS(de.getNameId());
        this.writeS(de.getTitle());
        this.writeC(status);
        this.writeD(de.getClanid());
        this.writeS(de.getClanname());
        this.writeS(null);
        this.writeC(0);
        this.writeC(255);
        this.writeC(de.hasSkillEffect(998) ? 8 : 0);
        this.writeC(0);
        this.writeC(0);
        this.writeC(255);
        this.writeC(255);
        this.writeC(255);
        this.writeD(0);
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
