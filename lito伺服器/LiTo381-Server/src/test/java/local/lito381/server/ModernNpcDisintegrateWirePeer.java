package local.lito381.server;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HexFormat;

/** Bounded stdin/stdout test fixture, not a listener or player deliverable.
 * Rust supplies actual request payloads; the room-authorized engine executes
 * them. No database, native process, socket or production account is opened. */
public final class ModernNpcDisintegrateWirePeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("no arguments");
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);
            var request=ModernBattleEngineNpcDisintegrateTest.request(f,0,1,npc);
            var start=ByteBuffer.allocate(80);ModernNpcDisintegrateWire.putRequest(start,request,29);
            System.out.println("READY:"+HexFormat.of().formatHex(start.array()));System.out.flush();
            for(int i=0;i<4;i++) {
                var incoming=ByteBuffer.wrap(HexFormat.of().parseHex(line(System.in)));
                var r=ModernNpcDisintegrateWire.getRequest(incoming,29);if(incoming.hasRemaining())throw new IllegalArgumentException("trailing request bytes");
                if(i==2)f.elapsed(5000);
                String reason;
                try {reason=f.engine.disintegrateNpc(f.players.get(0).owner(),r).reason().name();}
                catch(SecurityException denied){reason="SECURITY";}
                var frame=f.view();var w=frame.world();var states=w.npcDisintegrations();
                var data=ByteBuffer.allocate(8+states.size()*144);ModernNpcDisintegrateWire.putStates(data,f.scope,states,states.size(),29);
                var target=frame.npcs().actors().stream().filter(n->n.id().equals(npc.id())).findFirst().orElseThrow();
                System.out.println(reason+":"+w.revision()+":"+w.vitals().get(0).mp()+":"+target.hp()+":"+HexFormat.of().formatHex(data.array()));System.out.flush();
            }
        }
        System.out.println("CLOSED");System.out.flush();
    }
    private static String line(InputStream in)throws Exception {
        var s=new StringBuilder(160);for(int i=0;i<=160;i++) {
            int b=in.read();if(b=='\n'&&i==160)return s.toString();
            if(b<0||Character.digit((char)b,16)<0||i==160)throw new IllegalArgumentException("invalid fixed request line");s.append((char)b);
        }throw new IllegalArgumentException("oversized request line");
    }
    private ModernNpcDisintegrateWirePeer(){}
}
