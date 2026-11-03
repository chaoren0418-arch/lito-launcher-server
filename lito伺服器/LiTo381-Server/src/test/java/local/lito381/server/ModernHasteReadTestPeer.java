package local.lito381.server;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Authenticated loopback transport/admission fixture, not a room/lifecycle or
 * source-qualified cast service. Real immutable engine publications, two scopes.
 * Deliberately sends a valid older publication and another owner's publication
 * to prove that client state admission, not TLS alone, rejects those updates. */
public final class ModernHasteReadTestPeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!args[2].equals("normal"))throw new IllegalArgumentException();
        var traces=new ArrayList<ModernBattlePublication>();
        try(var f=ModernBattleHasteTest.fixture()) {
            traces.add(ModernHasteEffectsTest.publish(f.engine.serviceObservation()));
            traces.add(ModernHasteEffectsTest.publish(ModernBattleHasteTest.active(f)));
            traces.add(ModernHasteEffectsTest.publish(ModernBattleHasteTest.cast(f,0,1,1).observation()));
            f.elapsed(2_000_000_000L);
            traces.add(ModernHasteEffectsTest.publish(ModernBattleHasteTest.cast(f,0,2,0).observation()));
            traces.add(ModernHasteEffectsTest.publish(ModernBattleHasteTest.cast(f,1,1,1).observation()));
            f.elapsed(1_202_000_000_000L);traces.add(ModernHasteEffectsTest.publish(f.engine.serviceObservation()));
            traces.add(ModernHasteEffectsTest.publish(f.engine.abortObservation()));
            var backend=new ModernApplicationTestBackend("auth");
            var ctx=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
            try(var executor=new ModernBackendExecutor(2,2);
                var listener=(SSLServerSocket)ctx.getServerSocketFactory().createServerSocket(0,2,InetAddress.getByName("127.0.0.1"))) {
                listener.setSoTimeout(10000);listener.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});
                System.out.println("READY "+listener.getLocalPort());System.out.flush();
                for(int owner=0;owner<2;owner++) {
                    var scope=f.players.get(owner).scope();var world=traces.get(0).capture().identity();
                    System.out.println("SCOPE "+scope.sourceId()+" "+scope.objectId()+" "+scope.epoch()+" "+scope.channelId()+" "+world.roomId()+" "+world.matchId()+" "+world.generation());System.out.flush();
                    try(var socket=(SSLSocket)listener.accept();
                        var t=new ModernTlsSessionTransport(socket,new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,7,44),
                                ModernTlsSessionTransport.Limits.defaults(),backend.verifier)) {
                        var gate=executor.openSession();t.attachBackend(gate,5000);t.negotiate();
                        try(var message=t.readApplication(5000)) {
                            if(!(message instanceof ModernApplicationProtocol.Auth auth))throw new IllegalStateException("AUTH required");
                            var principal=t.authenticateForApplication(auth.account(),auth.password());
                            if(!principal.authorizesSource(scope.sourceId()))throw new SecurityException();
                            var sources=principal.sources().stream().sorted(java.util.Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                                    .map(s->new ModernApplicationProtocol.Source(s.id(),s.name())).toList();
                            t.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(),0,principal.account(),sources));
                        }
                        int[] order={0,0,2,2,4,3,4,5,6,6};
                        for(int step=0;step<order.length;step++) {
                            try(var message=t.readApplication(5000)) {
                                if(!(message instanceof ModernApplicationProtocol.BattleReadRequest q)
                                        ||!q.request().scope().equals(scope)||!q.request().world().equals(world))throw new SecurityException();
                                var selected=step==6?f.players.get(1-owner).scope():scope;
                                var v=traces.get(order[step]).audience(selected);
                                t.writeApplication(new ModernApplicationProtocol.BattleReadResult(q.requestId(),
                                        new ModernBattleReadProtocol.Request(selected,world),
                                        new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v)));
                            }
                        }
                        if(t.readApplication(5000)!=null)throw new IllegalStateException("expected orderly close");
                    }
                }
            }
            if(backend.authenticationCalls.get()!=4)throw new IllegalStateException("two authenticated owners required");
        }
    }
}
