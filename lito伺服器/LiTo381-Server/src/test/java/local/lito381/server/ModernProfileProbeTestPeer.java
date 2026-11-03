package local.lito381.server;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Set;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import static local.lito381.server.ModernSessionProtocol.*;
import static local.lito381.server.ModernSessionExtensions.*;

/** One owned loopback connection. Full advertised test contract, no SQL/account/game providers. */
public final class ModernProfileProbeTestPeer {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("serve"))throw new IllegalArgumentException("explicit fixture/mode/lifetime required");
        String mode=args[2];
        if(!Set.of("full","tls12","legacy","no-extensions","stall-ack").contains(mode))throw new IllegalArgumentException("mode");
        int lifetime=Integer.parseInt(args[3]);
        if(lifetime<100 || lifetime>10000)throw new IllegalArgumentException("lifetime");
        var policy=new ModernSessionBoundary.Policy(KNOWN_CAPABILITIES,
                CAP_EXPLICIT_I64_STATS|CAP_SOURCE_SCOPED_IDENTITIES,mode.equals("legacy")?37:47);
        var extensions=mode.equals("no-extensions")||mode.equals("legacy")?NONE:new Offer(15,0);
        var context=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        try(var listener=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(10000);
            listener.setEnabledProtocols(mode.equals("tls12")?new String[]{"TLSv1.2"}:new String[]{"TLSv1.3","TLSv1.2"});
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            try(var socket=(SSLSocket)listener.accept()) {
                socket.setSoTimeout(lifetime);
                if(mode.equals("stall-ack")) {
                    socket.startHandshake();
                    new ModernSessionBoundary(policy,extensions).acceptHello(readFrame(socket.getInputStream()));
                    Thread.sleep(lifetime);
                    if(socket.getInputStream().read()!=-1)throw new AssertionError("probe sent account/game data after HELLO");
                } else {
                    try(var transport=new ModernTlsSessionTransport(socket,policy,extensions,
                            ModernTlsSessionTransport.Limits.defaults(),
                            (account,password)->{throw new AssertionError("probe must never authenticate");})) {
                        try {
                            transport.negotiate();
                            if(transport.authenticatedPrincipal().isPresent())throw new AssertionError("probe authenticated");
                            if(socket.getInputStream().read()!=-1)throw new AssertionError("probe must close after HELLO/ACK");
                        } catch(ProtocolException denied) {
                            if(!mode.equals("legacy")&&!mode.equals("no-extensions"))throw denied;
                            if(transport.state()!=ModernTlsSessionTransport.State.CLOSED)throw new AssertionError("denial did not close");
                        }
                    }
                }
            }
        }
        System.out.println("CLOSED "+mode+" no-auth-or-game-data");System.out.flush();
    }
}
