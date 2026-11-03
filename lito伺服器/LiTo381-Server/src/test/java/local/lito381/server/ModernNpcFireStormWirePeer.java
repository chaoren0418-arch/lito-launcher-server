package local.lito381.server;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HexFormat;

/** Bounded Rust -> engine -> Rust section fixture. Not a server listener, SQL
 * session, atomic application envelope or player deliverable. */
public final class ModernNpcFireStormWirePeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("one scenario required");
        switch(args[0]) {
            case "time" -> {try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
                f.active();var player=f.players.get(0);var initial=f.view();greeting(player.scope(),initial);
                for(int i=0;i<7;i++) {var request=request();if(i>=3)f.elapsed((i-2)*2000L);String reason;
                    try{reason=f.engine.fireStormNpc(player.owner(),request).reason().name();}catch(SecurityException e){reason="SECURITY";}
                    reply(reason,f.view());}
            }}
            case "quest-symbols","quest-guardian" -> {int map=args[0].equals("quest-symbols")?513:554;int casts=map==513?8:5;
                try(var f=ModernNpcFireStormStructuresTest.quest(map,1)) {
                    f.active();var player=f.players.get(0);greeting(player.scope(),f.view());
                    for(int i=0;i<casts+2;i++){var request=request();if(i>=3)f.elapsed((i-2)*2000L);String reason;
                        try{reason=f.engine.fireStormNpc(player.owner(),request).reason().name();}catch(SecurityException e){reason="SECURITY";}
                        reply(reason,f.view());}
                }
            }
            default -> throw new IllegalArgumentException("unknown scenario");
        }
        System.out.println("CLOSED");System.out.flush();
    }
    private static void greeting(ModernRooms.Scope player,ModernNpcBattle.Frame frame) {
        var b=ByteBuffer.allocate(64);ModernNpcFireStormWire.putRequest(b,new ModernNpcFireStormState.Request(player,frame.npcs().scope(),1,1),31);
        System.out.println("READY:"+HexFormat.of().formatHex(b.array())+":"+Math.incrementExact(frame.world().revision())+":"+section(frame));System.out.flush();
    }
    private static String section(ModernNpcBattle.Frame frame) {
        var states=frame.world().npcFireStorms().stream().map(ModernNpcFireStormState.State::wireRevision).toList();var scope=frame.npcs().scope();var b=ByteBuffer.allocate(ModernNpcFireStormWire.bytes(scope,states,states.size()));
        ModernNpcFireStormWire.putStates(b,scope,states,states.size(),31);return HexFormat.of().formatHex(b.array());
    }
    private static void reply(String reason,ModernNpcBattle.Frame frame) {
        var w=frame.world();System.out.println(reason+":"+Math.incrementExact(w.revision())+":"+w.vitals().get(0).mp()+":"+w.vitals().get(1).mp()+":"+w.phase()+":"+section(frame));System.out.flush();
    }
    private static ModernNpcFireStormState.Request request()throws Exception {
        var b=ByteBuffer.wrap(HexFormat.of().parseHex(line(System.in)));var r=ModernNpcFireStormWire.getRequest(b,31);if(b.hasRemaining())throw new IllegalArgumentException("trailing bytes");return r;
    }
    private static String line(InputStream in)throws Exception {
        var s=new StringBuilder(128);for(int i=0;i<=128;i++) {
            int b=in.read();if(b=='\n'&&i==128)return s.toString();
            if(b<0||i==128||Character.digit((char)b,16)<0)throw new IllegalArgumentException("invalid request line");s.append((char)b);
        }throw new IllegalArgumentException("oversized request");
    }
    private ModernNpcFireStormWirePeer(){}
}
