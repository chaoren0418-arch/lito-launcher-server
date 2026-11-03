package local.lito381.server;

/** Pure packet construction; does not open SQL, sockets, or a game server. */
public final class ClassIconWireProbe {
    static byte[] packet(int job,int sex,Protocol.LitoParticipantState state) {
        return Protocol.litoRoomParticipant(123,"IconProbe",Protocol.LitoForce.FORCE_0,job,sex,
            new GameStateStore.TournamentStatistics(14,9,5,2),"",0,321,state,0,
            GameStateStore.TournamentAwards.none());
    }
    public static void main(String[] args) {
        int cases=0;
        for(int job=0;job<7;job++)for(int sex=0;sex<2;sex++)
            for(var state:Protocol.LitoParticipantState.values()) {
                byte[] p=packet(job,sex,state),base=packet(0,sex,state);
                var r=new Protocol.Reader(p);
                if(r.c()!=Protocol.S_LITO_ROOM_USER || r.d()!=123 || !r.s().equals("IconProbe")
                    || r.c()!=0 || r.c()!=job || r.c()!=sex)throw new AssertionError("wire class mismatch");
                // Name is nine ASCII bytes; job byte follows opcode, room, name+NUL, force.
                int offset=1+4+10+1;
                for(int i=0;i<p.length;i++)if(i!=offset&&p[i]!=base[i])throw new AssertionError("unrelated field changed");
                cases++;
            }
        for(int job:new int[]{-1,7,255}) {
            try {packet(job,0,Protocol.LitoParticipantState.READY);throw new AssertionError("invalid accepted");}
            catch(IllegalArgumentException expected) {cases++;}
        }
        System.out.println("PASS "+cases+" seven-class participant packet cases; actual candidate JAR; no SQL");
    }
}
