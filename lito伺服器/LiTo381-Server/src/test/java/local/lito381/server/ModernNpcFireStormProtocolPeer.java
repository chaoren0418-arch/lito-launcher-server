package local.lito381.server;

import java.io.InputStream;
import java.util.HexFormat;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;

/** Test-classpath only: actual bounded application requests into room authority,
 * complete application responses back. No production listener/TLS/UI claim. */
public final class ModernNpcFireStormProtocolPeer {
    static String wire(ModernApplicationProtocol.Message m)throws Exception {
        return HexFormat.of().formatHex(ModernNpcFireStormProtocolTest.roundtrip(m));
    }
    static void send(String tag,ModernApplicationProtocol.Message m)throws Exception {System.out.println(tag+":"+wire(m));System.out.flush();}
    static ModernApplicationProtocol.NpcFireStormRequest request()throws Exception {
        var bytes=HexFormat.of().parseHex(line(System.in));
        var framed=ModernApplicationProtocol.readFrame(new java.io.ByteArrayInputStream(bytes),31);
        if(!(ModernApplicationProtocol.decode(framed,31) instanceof ModernApplicationProtocol.NpcFireStormRequest r))throw new IllegalArgumentException("expected NPC70 request");return r;
    }
    static void greet(ModernRooms.Scope player,ModernNpcProtocol.Frame frame)throws Exception {
        var q=new ModernApplicationProtocol.NpcFireStormRequest(91,new ModernNpcFireStormState.Request(player,frame.scope(),1,1));
        System.out.println("READY:"+wire(q)+":"+wire(ModernNpcFireStormProtocolTest.read(new ModernNpcFireStormFramePeer.Row("before",player,frame))));System.out.flush();
    }
    static void reply(ModernApplicationProtocol.NpcFireStormRequest q,ModernNpcFireStormState.Reason reason,ModernNpcProtocol.Frame frame)throws Exception {
        send("RESULT",new ModernApplicationProtocol.NpcFireStormResult(q.requestId(),q.request(),new ModernNpcFireStormProtocol.Reply(reason==NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,frame)));
    }
    static void rejected(ModernRooms.Scope player,ModernNpcProtocol.Frame frame)throws Exception {
        send("REJECTED",ModernNpcFireStormProtocolTest.read(new ModernNpcFireStormFramePeer.Row("unchanged",player,frame)));
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("one scenario required");
        if(args[0].equals("large")) {
            for(var m:ModernNpcFireStormProtocolTest.largeMessages())send("LARGE",m);
        } else if(args[0].equals("time"))try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();var p=f.players.get(0);greet(p.scope(),ModernNpcFireStormFrameTest.frame(f));
            for(int i=0;i<8;i++) {
                var q=request();if(i>=3)f.elapsed((i-2)*2000L);
                try{var reason=f.engine.fireStormNpc(p.owner(),q.request()).reason();reply(q,reason,ModernNpcFireStormFrameTest.frame(f));}
                catch(SecurityException e){rejected(p.scope(),ModernNpcFireStormFrameTest.frame(f));}
            }
        } else if(args[0].equals("normal")||args[0].equals("symbols")||args[0].equals("guardian")) {
            boolean normal=args[0].equals("normal");int map=args[0].equals("symbols")?513:554;int casts=normal?2:map==513?8:5;
            try(var f=normal?ModernBattleEngineNpcFireStormTest.fixture():ModernNpcFireStormStructuresTest.quest(map,1)) {
                f.active();if(normal){f.spawn(0,11,10);f.spawn(0,10,11);}var p=f.players.get(0);greet(p.scope(),ModernNpcFireStormFrameTest.frame(f,0));
                for(int i=0;i<casts+3;i++) {
                    var q=request();if(i>=3)f.elapsed((i-2)*2000L);if(normal&&i==casts+2)f.rooms.releasePreparation(f.ticket);
                    try{var reason=f.engine.fireStormNpc(p.owner(),q.request()).reason();reply(q,reason,ModernNpcFireStormFrameTest.frame(f,0));}
                    catch(SecurityException e){rejected(p.scope(),ModernNpcFireStormFrameTest.frame(f,0));}
                }
            }
        } else throw new IllegalArgumentException("unknown scenario");
        System.out.println("CLOSED");System.out.flush();
    }
    static String line(InputStream in)throws Exception {
        var s=new StringBuilder(160);for(int i=0;i<=160;i++) {
            int b=in.read();if(b=='\n'&&i==160)return s.toString();
            if(b<0||i==160||Character.digit((char)b,16)<0)throw new IllegalArgumentException("invalid bounded request");s.append((char)b);
        }throw new IllegalArgumentException("oversized request");
    }
    private ModernNpcFireStormProtocolPeer(){}
}
