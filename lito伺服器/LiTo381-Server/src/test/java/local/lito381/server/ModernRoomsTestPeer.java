package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Rust interoperability peer: real production listener, navigation and waiting-room authority; no DB. */
public final class ModernRoomsTestPeer {
    private ModernRoomsTestPeer() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("serve") || !Set.of("normal","equipment").contains(args[2]))
            throw new IllegalArgumentException("serve <fixture-dir> <normal|equipment> <lifetime-ms>");
        int lifetime=Integer.parseInt(args[3]);
        if(lifetime<1000 || lifetime>30000)throw new IllegalArgumentException("bounded lifetime required");
        boolean equipment=args[2].equals("equipment");
        Path directory=equipment?Files.createTempDirectory("modern-rooms-equipment-peer-"):null;
        boolean retired=false;
        try {
            run(args,lifetime,directory);
            retired=true;
        } finally {
            // Only our newly-created flat fixture directory, after owned workers and store have retired.
            // On failure preserve it for diagnosis rather than delete an uncertain live namespace.
            if(directory!=null && retired) {
                try(var children=Files.list(directory)) {
                    for(Path child:children.toList()) {
                        if(!Files.isRegularFile(child,java.nio.file.LinkOption.NOFOLLOW_LINKS))throw new IOException("unexpected test child");
                        Files.delete(child);
                    }
                }
                Files.delete(directory);
            }
        }
    }

    private static void run(String[] args,int lifetime,Path directory) throws Exception {
        var backend=new ModernApplicationTestBackend("auth");var presence=new TournamentPresence();var rooms=new ModernRooms();
        var reads=new AtomicInteger();
        var store=directory==null?null:new ModernEquipmentOverrideStore(directory);
        boolean transferred=false;
        try {
            var authority=store==null?null:new ModernEquipmentAuthority((principal,source,object)->{
                if(!principal.authorizesSource(source)||object!=3333
                        || !(source==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT)
                                .equals(principal.accountForSource(source)))throw new SecurityException("fixture scope required");
                reads.incrementAndGet();byte[] binding=new byte[32];Arrays.fill(binding,(byte)source);
                return new ModernEquipmentAuthority.Loaded(ModernEquipmentTestPeer.qualified(source),new ModernEquipmentOverrideStore.Fingerprint(binding));
            },store,()->1000L);
            var limits=new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,5000);
            var server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),
                    ModernTlsTestMaterial.serverContext(Path.of(args[1]),false),limits,backend.verifier,backend.characters,presence,
                    (principal,source,object)->principal.authorizesSource(source)&&object==3333
                            ?Optional.of(ModernEquipmentTestPeer.qualified(source).source()):Optional.empty(),authority,store,rooms);
            transferred=true;
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                for(;;) {
                    var state=server.snapshot();
                    if(state.accepted()==2 && state.sessions()==0)break;
                    if(server.listenerFailed() || System.nanoTime()>=deadline)throw new IllegalStateException("rooms peer incomplete");
                    Thread.sleep(10);
                }
                if(backend.authenticationCalls.get()!=4 || backend.characterCalls.get()<4 || server.snapshot().failed()!=0
                        || store!=null && reads.get()<2)throw new IllegalStateException("rooms peer flow failed");
            } finally {
                server.close();
                if(!server.awaitTermination(3000) || presence.ownerCount()!=0 || presence.userCount()!=0
                        || rooms.ownerCount()!=0 || rooms.roomCount()!=0 || rooms.memberCount()!=0
                        || store!=null && !store.isQuiescent())throw new IllegalStateException("rooms ownership did not retire");
            }
            if(directory!=null)try(var reopened=new ModernEquipmentOverrideStore(directory);var executor=new ModernBackendExecutor(1,0);var gate=executor.openSession()) {
                var saved=gate.call(2000,scope->reopened.read(new TournamentPresence.CharacterKey(7,3333),scope));
                if(saved.isEmpty() || saved.orElseThrow().revision()<1)throw new IllegalStateException("room equipment SET not persisted");
                executor.close();if(!executor.awaitTermination(3000))throw new IllegalStateException("reopen read did not retire");
            }
        } finally {if(!transferred && store!=null)store.close();}
    }
}
