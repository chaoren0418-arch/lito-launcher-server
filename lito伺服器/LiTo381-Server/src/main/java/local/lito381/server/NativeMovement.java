package local.lito381.server;

/** S_12 sub_4A2C10 advances the supplied origin by one heading step. */
final class NativeMovement {
    private static final int[] DX={0,1,1,1,0,-1,-1,-1}, DY={-1,-1,0,1,1,1,0,-1};
    static byte[] destination(long objectId, int x, int y, int heading) {
        if (heading<0 || heading>7) throw new IllegalArgumentException("movement heading");
        int sourceX=x-DX[heading],sourceY=y-DY[heading];
        if(sourceX<0 || sourceX>65535 || sourceY<0 || sourceY>65535)
            throw new IllegalArgumentException("movement source outside word range");
        return new Protocol.Writer().c(Protocol.S_MOVE_OBJECT).d(objectId)
                .h(sourceX).h(sourceY).c(heading).bytes();
    }
    /** Scoped binary migration for legacy player methods that wrote destinations.
     * Never apply globally: NPC and companion producers already write origins. */
    static byte[] legacyDestinationPacket(byte[] packet) {
        if(packet.length==0 || (packet[0]&255)!=Protocol.S_MOVE_OBJECT) return packet;
        if(packet.length!=10) throw new IllegalArgumentException("legacy movement length");
        var r=new Protocol.Reader(packet);r.c();return destination(r.d(),r.h(),r.h(),r.c());
    }
}
