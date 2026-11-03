package local.lito381.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Test-owned TLS + actual navigation/queue authority. Synthetic map and records;
 * not a battle/assets/SQL fixture. Faults withhold only a genuinely committed reply. */
public final class ModernMatchmakingTestPeer {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("serve") || !Set.of("normal","lost-start","lost-match","cancel-start","cancel-match").contains(args[2]))
            throw new IllegalArgumentException("matchmaking fixture arguments");
        int lifetime=Integer.parseInt(args[3]);
        if(lifetime<1000 || lifetime>30000)throw new IllegalArgumentException("bounded lifetime required");
        String mode=args[2];boolean normal=mode.equals("normal");
        Path directory=Files.createTempDirectory("modern-matchmaking-peer-");boolean retired=false;
        var worker=Executors.newFixedThreadPool(2);var watchdog=Executors.newSingleThreadScheduledExecutor();
        var fired=new AtomicBoolean();var starts=new AtomicInteger();var faults=new AtomicInteger();
        try(var services=new ModernMatchmakingNavigationTest.Services(directory);
            var executor=new ModernBackendExecutor(2,2);
            var listener=(SSLServerSocket)ModernTlsTestMaterial.serverContext(Path.of(args[1]),false).getServerSocketFactory()
                .createServerSocket(0,2,InetAddress.getByAddress(new byte[]{127,0,0,1}))) {
            listener.setSoTimeout(5000);
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            var futures=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<(normal?2:1);i++) {
                var socket=(SSLSocket)listener.accept();
                futures.add(worker.submit(()->{
                    try(socket;var transport=new ModernTlsSessionTransport(socket,
                        new ModernSessionBoundary.Policy(ModernMatchmakingProtocolTest.CAPS,7,34),
                        new ModernTlsSessionTransport.Limits(2000,2000),services.fixture.verifier);
                        var gate=executor.openSession()) {
                        transport.attachBackend(gate,2000);
                        var stop=watchdog.schedule(()->{fired.set(true);transport.failClosed();},lifetime,TimeUnit.MILLISECONDS);
                        TournamentPresence.Owner synthetic=null;
                        try(var navigation=new ModernNavigationSession(transport,services.fixture.characters,gate,2000,
                            services.presence,services.rooms,services.worlds,services.equipment,ModernSkillsRepository.unavailable(),
                            (p,s,o)->{ModernMatchmakingNavigationTest.principal(p,s,o);return ModernMatchmakingNavigationTest.standing(s);})) {
                            var negotiated=transport.negotiate();
                            if(!ModernMatchmakingProtocol.negotiated(negotiated))throw new IOException("matchmaking not negotiated");
                            navigation.negotiateRooms(true);navigation.negotiateWorlds(true);
                            navigation.negotiateRecords(true);navigation.negotiateMatchProtocol(negotiated);
                            int previous=negotiated.requestId();
                            for(;;)try(var message=transport.readApplication(5000)) {
                                if(message==null)break;
                                if(Integer.compareUnsigned(message.requestId(),previous)<=0)throw new IOException("request sequence");
                                previous=message.requestId();
                                if(message instanceof ModernApplicationProtocol.Auth auth) {
                                    var p=transport.authenticateForApplication(auth.account(),auth.password());
                                    var sources=p.sources().stream().sorted(Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                                        .map(s->new ModernApplicationProtocol.Source(s.id(),s.name())).toList();
                                    transport.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(),0,p.account(),sources));
                                } else if(message instanceof ModernApplicationProtocol.Ping ping) {
                                    transport.writeApplication(new ModernApplicationProtocol.Pong(ping.requestId()));
                                } else if(message instanceof ModernApplicationProtocol.CharacterRequest r) {
                                    navigation.beforeCharacterRequest();var p=transport.authenticatedPrincipal().orElseThrow();
                                    var rows=gate.call(2000,scope->services.fixture.characters.characters(p,r.sourceId()));
                                    transport.writeApplication(new ModernApplicationProtocol.CharacterResult(r.requestId(),r.sourceId(),r.selectionEpoch(),0,rows));
                                    navigation.recordRoster(r.sourceId(),r.selectionEpoch(),rows);
                                } else if(message instanceof ModernApplicationProtocol.CharacterDataRequest r) {
                                    transport.writeApplication(navigation.characterData(r,ModernCharacterDataRepository.unavailable()));
                                } else if(message instanceof ModernApplicationProtocol.EquipmentRequest r) {
                                    if(r.request().action()!=ModernEquipmentAuthority.READ)throw new IOException("fixture only supports equipment READ");
                                    transport.writeApplication(new ModernApplicationProtocol.EquipmentResult(r.requestId(),r.request(),
                                        ModernEquipmentAuthority.Result.unavailable(ModernEquipmentAuthority.BUSY)));
                                } else if(message instanceof ModernApplicationProtocol.RecordRequest r) {
                                    transport.writeApplication(navigation.record(r));
                                } else if(message instanceof ModernApplicationProtocol.RoomRequest r) {
                                    transport.writeApplication(navigation.room(r));
                                } else if(message instanceof ModernApplicationProtocol.WorldRequest r) {
                                    transport.writeApplication(new ModernApplicationProtocol.WorldResult(r.requestId(),r.request(),navigation.world(r.request())));
                                } else if(message instanceof ModernApplicationProtocol.MatchmakingRequest r) {
                                    var result=navigation.matchmaking(r);
                                    if(r.request().action()==ModernMatchmakingProtocol.Action.START) {
                                        starts.incrementAndGet();
                                        if(result.reply().result().state()!=ModernMatchmaking.State.WAITING)throw new IOException("START did not queue");
                                        if(!normal && mode.endsWith("-match")) {
                                            synthetic=services.presence.connect();
                                            var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(42,3333),"月亮","測試配對者",80,1,0);
                                            if(services.presence.select(synthetic,selected)!=TournamentPresence.SelectStatus.ACCEPTED)throw new IOException("synthetic selection failed");
                                            services.rooms.enterLobby(synthetic,selected,1,0,true);
                                            services.rooms.startMatchmaking(synthetic,new ModernRooms.Scope(42,3333,1,0),ModernMatchmakingTest.pair(),ModernMatchmakingNavigationTest.standing(42));
                                        }
                                    }
                                    boolean fault=!normal && (mode.endsWith("-start") && r.request().action()==ModernMatchmakingProtocol.Action.START
                                        || mode.endsWith("-match") && result.reply().result().state()==ModernMatchmaking.State.MATCHED);
                                    if(fault) {
                                        boolean matched=mode.endsWith("-match");
                                        if(starts.get()!=1 || services.rooms.queuedMatchmakingCount()!=(matched?0:1)
                                            || services.rooms.roomCount()!=(matched?1:0) || services.rooms.memberCount()!=(matched?2:0))
                                            throw new IOException("fault did not follow exact authority commit");
                                        faults.incrementAndGet();
                                        System.out.println(matched?"COMMITTED MATCH":"COMMITTED START");System.out.flush();
                                        long start=System.nanoTime();
                                        try(var unexpected=transport.readApplication(2500)) {
                                            if(unexpected!=null)throw new IllegalStateException("uncertain matchmaking was replayed or continued");
                                        } catch(IOException closed) { /* Close/reset is required before the fixture timeout. */ }
                                        if(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)>=2200)throw new IOException("uncertain peer did not close promptly");
                                        break;
                                    }
                                    transport.writeApplication(result);
                                } else if(message instanceof ModernApplicationProtocol.CharacterSelection || message instanceof ModernApplicationProtocol.ChannelEnter
                                    || message instanceof ModernApplicationProtocol.LobbyLeave || message instanceof ModernApplicationProtocol.CharacterReturn) {
                                    transport.writeApplication(navigation.handle(message));
                                } else throw new IOException("unexpected matchmaking fixture operation");
                            }
                        } finally {
                            stop.cancel(false);
                            if(synthetic!=null){services.rooms.disconnect(synthetic);services.presence.disconnect(synthetic);}
                        }
                    }
                    return null;
                }));
            }
            for(var future:futures)future.get(lifetime+1000L,TimeUnit.MILLISECONDS);
            listener.setSoTimeout(300);
            try(var unexpected=listener.accept()){throw new IOException("automatic reconnect");}
            catch(SocketTimeoutException expected) { }
            if(fired.get() || starts.get()<(normal?2:1) || faults.get()!=(normal?0:1))throw new IOException("fixture incomplete");
            executor.close();if(!executor.awaitTermination(3000))throw new IOException("backend ownership");
            retired=true;
        } finally {
            worker.shutdownNow();watchdog.shutdownNow();
            if(!worker.awaitTermination(3000,TimeUnit.MILLISECONDS)||!watchdog.awaitTermination(3000,TimeUnit.MILLISECONDS))throw new IOException("peer workers still live");
            if(retired) {
                try(var children=Files.list(directory)) {
                    for(var child:children.toList()){
                        if(!Files.isRegularFile(child,java.nio.file.LinkOption.NOFOLLOW_LINKS))throw new IOException("unexpected test child");
                        Files.delete(child);
                    }
                }
                Files.delete(directory);
            }
        }
    }
}
