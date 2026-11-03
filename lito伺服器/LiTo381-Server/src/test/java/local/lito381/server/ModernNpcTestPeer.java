package local.lito381.server;

import static local.lito381.server.ModernNpcCatalog.Field.HP;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Test-owned, single loopback TLS codec/engine peer; NOT a production/authentication provider. */
public final class ModernNpcTestPeer {
    private ModernNpcTestPeer() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!args[2].equals("npc-v17"))
            throw new IllegalArgumentException("serve <prepared-tls-dir> npc-v17 <bounded-hold>");
        int hold=Integer.parseInt(args[3]);if(hold<50||hold>10000)throw new IllegalArgumentException("hold bound");
        var context=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        try(var listener=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(10000);listener.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            try(var socket=(SSLSocket)listener.accept();
                var f=new ModernBattleEngineNpcTest.Fixture(2,1,false,ModernNpcProtocolTest.HUGE,0,UnaryOperator.identity(),
                    ModernNpcMeleeTest.catalog(Map.of(HP,ModernNpcProtocolTest.HUGE),true,0),true)) {
                socket.setSoTimeout(hold);socket.startHandshake();
                var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,
                        ModernSessionProtocol.CAP_EXPLICIT_I64_STATS|ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES,17));
                byte[] ack=boundary.acceptHello(ModernSessionProtocol.readFrame(socket.getInputStream()));
                var selection=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ack);
                if(selection.version()!=17||(selection.capabilities()&ModernSessionProtocol.CAP_NPC)==0)
                    throw new IllegalArgumentException("explicit test NPC capability required");
                socket.getOutputStream().write(ack);socket.getOutputStream().flush();
                f.active();var first=f.spawn(0,11,10);var second=f.spawn(0,31,10);f.elapsed(100);f.step(first);f.step(second);
                for(int i=0;i<4;i++) {
                    var decoded=ModernApplicationProtocol.decode(ModernApplicationProtocol.readFrame(socket.getInputStream(),17),17);
                    if(!(decoded instanceof ModernApplicationProtocol.NpcRequest m))throw new IllegalArgumentException("NPC request only");
                    var expected=ModernNpcProtocolTest.request(f,i==1?1:i==2?2:0,i==1||i==2?1:0,i==1||i==2?1:0);
                    if(!m.request().equals(expected))throw new IllegalArgumentException("fixture scope/action mismatch");
                    ModernNpcBattle.Frame state;
                    var status=ModernNpcProtocol.Status.OK;var reason=ModernNpcProtocol.Reason.NONE;
                    if(i==1||i==2) {
                        var result=f.engine.attackNpc(f.players.get(0).owner(),m.request().player(),
                            m.request().world().matchId(),m.request().world().generation(),m.request().sequence(),m.request().target(),m.request().life());
                        if(result.reason()!=(i==1?ModernNpcBattle.Reason.ATTACKED:ModernNpcBattle.Reason.TOO_FAST))
                            throw new AssertionError("unexpected engine attack "+result.reason());
                        if(i==2){status=ModernNpcProtocol.Status.DENIED;reason=ModernNpcProtocol.Reason.TOO_FAST;}
                        state=result.frame();
                    } else {
                        if(i==3)f.engine.abort();state=f.view();
                    }
                    var message=new ModernApplicationProtocol.NpcResult(m.requestId(),m.request(),
                        new ModernNpcProtocol.Reply(status,reason,ModernNpcProtocol.Frame.from(state,ModernNpcProtocolTest.descriptor(f))));
                    byte[] response=ModernApplicationProtocol.encode(message,17);
                    // Exercise TLS record/stream fragmentation without adding artificial sleeps.
                    for(int offset=0;offset<response.length;offset+=37) {
                        socket.getOutputStream().write(response,offset,Math.min(37,response.length-offset));socket.getOutputStream().flush();
                    }
                }
                if(socket.getInputStream().read()!=-1)throw new AssertionError("unexpected trailing request");
            }
        }
    }
}
