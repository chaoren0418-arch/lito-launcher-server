package local.lito381.server;

import java.nio.file.Path;
import java.net.InetAddress;
import java.util.Set;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSessionProtocol.*;
import static local.lito381.server.ModernSessionExtensions.*;

/** One owned loopback TLS connection; no account/database/gameplay authority. */
public final class ModernExtendedSessionTestPeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve"))throw new IllegalArgumentException("explicit fixture/mode/lifetime required");
        String mode=args[2];if(!Set.of("commands","tls12","data","fragmented","denied","legacy-ack","unoffered").contains(mode))throw new IllegalArgumentException("mode");
        int lifetime=Integer.parseInt(args[3]);if(lifetime<100||lifetime>10000)throw new IllegalArgumentException("lifetime");
        var context=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        try(var listener=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(10000);listener.setEnabledProtocols(mode.equals("tls12")?new String[]{"TLSv1.2"}:new String[]{"TLSv1.3","TLSv1.2"});
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            try(var socket=(SSLSocket)listener.accept()) {
                socket.setSoTimeout(lifetime);
                if(Set.of("fragmented","legacy-ack","unoffered").contains(mode)) {
                    socket.startHandshake();var hello=readFrame(socket.getInputStream());
                    var b=new ModernSessionBoundary(ModernSessionExtensionsTest.policy(),new Offer(3,0));
                    var selected=(ExtendedAck)decode(b.acceptHello(hello));
                    var answer=mode.equals("legacy-ack")?encode(selected.base()):mode.equals("unoffered")?encode(new ExtendedAck(selected.base(),3)):encode(selected);
                    if(mode.equals("fragmented")){for(byte value:answer){socket.getOutputStream().write(value&255);socket.getOutputStream().flush();Thread.sleep(5);}}
                    else {socket.getOutputStream().write(answer);socket.getOutputStream().flush();}
                    assertEquals(-1,socket.getInputStream().read());
                } else {
                    var offer=mode.equals("denied")?NONE:mode.equals("data")?new Offer(1,1):HEALING;
                    try(var transport=new ModernTlsSessionTransport(socket,ModernSessionExtensionsTest.policy(),offer,ModernTlsSessionTransport.Limits.defaults(),
                            (account,password)->{throw new AssertionError("negotiation must not authenticate");})) {
                        if(mode.equals("denied")) {
                            assertThrows(ProtocolException.class,transport::negotiate);assertEquals(ModernTlsSessionTransport.State.CLOSED,transport.state());
                        } else {
                            var selected=transport.negotiate();assertEquals(mode.equals("data")?1:3,selected.extensions());
                            assertEquals(ModernTlsSessionTransport.State.NEGOTIATED,transport.state());assertTrue(transport.authenticatedPrincipal().isEmpty());
                            assertEquals(-1,socket.getInputStream().read());
                        }
                    }
                }
            }
        }
        System.out.println("CLOSED "+mode);System.out.flush();
    }
}
