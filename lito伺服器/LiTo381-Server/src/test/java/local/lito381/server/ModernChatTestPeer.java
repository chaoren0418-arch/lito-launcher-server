package local.lito381.server;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Two actual TLS application sessions sharing production navigation/rooms/chat.
 * Only authentication/character rows and the chat rate clock are fixtures. */
public final class ModernChatTestPeer {
    public static void main(String[] args) throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!Set.of("normal","stale","unnegotiated","direct-stale","direct-unnegotiated").contains(args[2]))throw new IllegalArgumentException("chat peer arguments");
        int lifetime=Integer.parseInt(args[3]);if(lifetime<1000||lifetime>30000)throw new IllegalArgumentException("bounded chat peer lifetime");
        var backend=new ModernApplicationTestBackend("auth");var presence=new TournamentPresence();
        var rooms=new ModernRooms(Set.of(),Set.of(),java.util.Map.of(),new ModernChat(()->0));
        if(args[2].equals("normal")) {runListener(args,lifetime,backend,presence,rooms);return;}
        var executor=new ModernBackendExecutor(2,4);var workers=Executors.newFixedThreadPool(2);
        var transports=new java.util.concurrent.CopyOnWriteArrayList<ModernTlsSessionTransport>();
        var watchdog=Executors.newSingleThreadScheduledExecutor();
        var timedOut=new java.util.concurrent.atomic.AtomicBoolean();
        var stop=watchdog.schedule(()->{timedOut.set(true);for(var t:transports)t.failClosed();},lifetime,TimeUnit.MILLISECONDS);
        var tasks=new ArrayList<Future<Boolean>>();
        try(executor;var listener=(SSLServerSocket)ModernTlsTestMaterial.serverContext(Path.of(args[1]),false)
                .getServerSocketFactory().createServerSocket(0,2,InetAddress.getByAddress(new byte[]{127,0,0,1}))) {
            listener.setSoTimeout(5000);listener.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            for(int i=0;i<2;i++) {
                var socket=(SSLSocket)listener.accept();
                tasks.add(workers.submit(()->{
                    var policy=ModernSessionBoundary.Policy.rooms(false);
                    if(!args[2].equals("unnegotiated"))policy=policy.withChat();
                    if(args[2].equals("direct-stale"))policy=policy.withDirectChat();
                    try(var transport=new ModernTlsSessionTransport(socket,policy,new ModernTlsSessionTransport.Limits(2000,2000),backend.verifier)) {
                        transports.add(transport);
                        try {new ModernApplicationSession(transport,backend.characters,executor,10000,2000,presence,
                                ModernCharacterDataRepository.unavailable(),null,rooms).run();return true;}
                        catch(ModernApplicationProtocol.ProtocolException rejected) {if(!Set.of("stale","direct-stale","direct-unnegotiated").contains(args[2]))throw rejected;return false;}
                        finally {transports.remove(transport);}
                    }
                }));
            }
            int rejected=0;for(var task:tasks)if(!task.get(lifetime,TimeUnit.MILLISECONDS))rejected++;
            if(rejected!=(Set.of("stale","direct-stale","direct-unnegotiated").contains(args[2])?1:0)||backend.authenticationCalls.get()!=4||timedOut.get())throw new IllegalStateException("chat peer flow incomplete");
        } finally {
            stop.cancel(false);watchdog.shutdownNow();for(var t:transports)t.failClosed();workers.shutdownNow();executor.close();
            if(!workers.awaitTermination(3000,TimeUnit.MILLISECONDS)||!executor.awaitTermination(3000)
                    ||presence.ownerCount()!=0||presence.userCount()!=0||rooms.ownerCount()!=0
                    ||rooms.roomCount()!=0||rooms.memberCount()!=0||rooms.chatOwnerCount()!=0||rooms.directChatOwnerCount()!=0)throw new IllegalStateException("chat peer ownership did not retire");
        }
    }
    private static void runListener(String[] args,int lifetime,ModernApplicationTestBackend backend,
                                    TournamentPresence presence,ModernRooms rooms) throws Exception {
        var limits=new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000);
        var server=new ModernServer(new java.net.InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),
                ModernTlsTestMaterial.serverContext(Path.of(args[1]),false),limits,backend.verifier,backend.characters,presence,
                ModernCharacterDataRepository.unavailable(),null,null,rooms);
        try {
            System.out.println("READY "+server.address().getPort());System.out.flush();
            long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
            for(;;) {
                var s=server.snapshot();if(s.accepted()==2&&s.sessions()==0)break;
                if(server.listenerFailed()||System.nanoTime()>=deadline)throw new IllegalStateException("chat listener incomplete");
                Thread.sleep(10);
            }
            if(backend.authenticationCalls.get()!=4||server.snapshot().failed()!=0)throw new IllegalStateException("chat listener flow failed");
        } finally {
            server.close();
            if(!server.awaitTermination(3000)||presence.ownerCount()!=0||presence.userCount()!=0||rooms.ownerCount()!=0
                    ||rooms.roomCount()!=0||rooms.memberCount()!=0||rooms.chatOwnerCount()!=0||rooms.directChatOwnerCount()!=0)throw new IllegalStateException("chat listener ownership did not retire");
        }
    }
}
