package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.*;

/** Owned loopback TLS relay to a real ModernServer. Only the explicitly chosen
 * reply is damaged/withheld AFTER real authority execution. Never logs frames,
 * credentials or keys; no production socket, trust store or SQL is touched. */
public final class ModernVipTransportFaultTestPeer {
    static final Set<String> MODES=Set.of("world-id","world-source","world-length","world-old-kind","world-truncated",
            "world-replay","world-timeout","world-cancel","gear-id","gear-timeout","gear-cancel");
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!MODES.contains(args[2])||!args[3].equals("30000"))throw new IllegalArgumentException("fixture arguments");
        var tls=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        var client=ModernTlsTestMaterial.clientContext(Path.of(args[1]));ModernPlayerHealingTlsTest.serverContext=tls;
        Path root=Files.createTempDirectory("lito-vip-transport-fault-").toAbsolutePath().normalize();
        var workers=Executors.newFixedThreadPool(2);var timer=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r);t.setDaemon(true);return t;});
        var timeout=timer.schedule(()->System.exit(3),30,TimeUnit.SECONDS);var faults=new AtomicInteger();
        try(var f=new ModernVipWorldTlsTest.Fixture(root);
            var listener=(SSLServerSocket)tls.getServerSocketFactory().createServerSocket(0,2,InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(5000);System.out.println("READY "+listener.getLocalPort());System.out.flush();
            var tasks=new ArrayList<Future<?>>();
            for(int i=0;i<2;i++){var socket=(SSLSocket)listener.accept();tasks.add(workers.submit(()->{
                try{relay(socket,client,f.server.address().getPort(),args[2],faults);}catch(Exception e){throw new RuntimeException(e);}
            }));}
            for(var task:tasks)task.get(15,TimeUnit.SECONDS);
            ModernVipWorldServiceTest.await(()->f.server.snapshot().sessions()==0);
            assertEquals(1,faults.get());assertEquals(4,f.backend.authenticationCalls.get());assertEquals(0,f.server.snapshot().failed());
            assertEquals(args[2].startsWith("gear")?4:2,f.vipReads.get());
            var scope=ModernEquipmentOverrideStoreTest.scope();
            try {
                var saved=f.store.read(new TournamentPresence.CharacterKey(7,3333),scope);
                assertEquals(args[2].startsWith("gear"),saved.isPresent());
                if(saved.isPresent())assertEquals(1,saved.orElseThrow().revision(),"uncertain SET is committed once, never retried");
                assertTrue(f.store.read(new TournamentPresence.CharacterKey(42,3333),scope).isEmpty());
            } finally {scope.finish();}
            assertEquals(0,f.worlds.registrationCount());ModernWorldServiceTest.awaitNoMatches(f.worlds);
        } finally {
            workers.shutdownNow();assertTrue(workers.awaitTermination(3000,TimeUnit.MILLISECONDS));timeout.cancel(false);timer.shutdownNow();
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()){
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED "+args[2]);System.out.flush();
    }
    static void relay(SSLSocket outer,SSLContext client,int port,String mode,AtomicInteger faults)throws Exception {
        try(outer;var inner=(SSLSocket)client.getSocketFactory().createSocket("localhost",port)) {
            outer.setSoTimeout(5000);inner.setSoTimeout(5000);
            var parameters=inner.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");inner.setSSLParameters(parameters);
            outer.startHandshake();inner.startHandshake();byte[] old=null;
            for(;;){byte[] q=read(outer.getInputStream());if(q==null)return;inner.getOutputStream().write(q);inner.getOutputStream().flush();
                var r=read(inner.getInputStream());assertNotNull(r);var b=ByteBuffer.wrap(q);int kind=Short.toUnsignedInt(b.getShort(12));
                boolean sun=q.length>=20&&b.getInt(16)==7;
                if(sun&&kind==84&&r.length>168&&r[148]==ModernWorldProtocol.Phase.LOADING.ordinal()&&old==null)old=r.clone();
                boolean target=sun&&(mode.startsWith("world")&&kind==84&&q[40]==0&&r.length>168&&r[148]==ModernWorldProtocol.Phase.ACTIVE.ordinal()
                    ||mode.startsWith("gear")&&kind==82&&q[112]==1);
                if(target) {
                    assertEquals(1,faults.incrementAndGet());
                    if(mode.endsWith("id"))r[8]^=0x40;
                    if(mode.equals("world-source"))r[19]^=1;
                    if(mode.equals("world-length")){r=Arrays.copyOf(r,16);ByteBuffer.wrap(r).putInt(4,Integer.MAX_VALUE);}
                    if(mode.equals("world-old-kind"))ByteBuffer.wrap(r).putShort(12,(short)67);
                    if(mode.equals("world-replay")){assertNotNull(old);r=old.clone();System.arraycopy(q,8,r,8,4);System.arraycopy(q,16,r,16,96);}
                    if(mode.equals("world-truncated")){outer.getOutputStream().write(Arrays.copyOf(r,170));outer.getOutputStream().flush();return;}
                    if(mode.endsWith("timeout")||mode.endsWith("cancel")) {
                        System.out.println("HELD "+mode);System.out.flush();
                    }else{outer.getOutputStream().write(r);outer.getOutputStream().flush();}
                    // No application frame after an uncertain receipt. A retry
                    // is a test failure even if the server could reject it.
                    try{assertEquals(-1,outer.getInputStream().read());}catch(SocketException|SSLException closed){/* abort may reset TLS */}
                    return;
                }
                outer.getOutputStream().write(r);outer.getOutputStream().flush();
            }
        }
    }
    static byte[] read(InputStream input)throws Exception {
        int first=input.read();if(first<0)return null;byte[] header=new byte[16];header[0]=(byte)first;
        var rest=input.readNBytes(15);assertEquals(15,rest.length);System.arraycopy(rest,0,header,1,15);
        int size=ByteBuffer.wrap(header).getInt(4);assertTrue(size>=16&&size<=ModernVipEquipmentProtocol.MAX_RESULT_BYTES);
        byte[] data=Arrays.copyOf(header,size);rest=input.readNBytes(size-16);assertEquals(size-16,rest.length);System.arraycopy(rest,0,data,16,rest.length);return data;
    }
}
