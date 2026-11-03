package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real authenticated TLS + bounded backend worker; map/record providers here
 * are synthetic. These tests do not claim live SQL or battle asset coverage. */
class ModernMatchmakingNavigationTest {
    @TempDir Path temp;
    static SSLContext serverTls,clientTls;
    static final int CAPS=ModernMatchmakingProtocolTest.CAPS;
    @BeforeAll static void certificates()throws Exception{
        var directory=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(directory);
        serverTls=ModernTlsTestMaterial.serverContext(directory,false);clientTls=ModernTlsTestMaterial.clientContext(directory);
    }
    static ModernCharacterRecord.Snapshot standing(int source){return ModernMatchmakingTest.standing(new TournamentPresence.CharacterKey(source,3333),3,7);}
    static void principal(ModernAccountAuthentication.Principal p,int source,long object)throws Exception{
        assertEquals(3333,object);assertEquals(source==7?"sun-owner":"moon-owner",p.accountForSource(source));
        assertTrue(p.authorizesSource(source));ModernBackendScope.current().checkActive();
    }
    @Test void exactSourceRecordOnlyOnStartAndLobbyReturnRetiresAdmission()throws Exception{
        var reads=new AtomicInteger();
        try(var services=new Services(temp);var peer=new Peer(services,(p,s,o)->{principal(p,s,o);reads.incrementAndGet();return standing(s);},2000);
                var client=new Client(peer,34,CAPS)){
            client.login();client.enter(7,1);assertEquals(ModernMatchmaking.State.IDLE,client.read(0).result().state());assertEquals(0,reads.get());
            var queued=client.start();assertEquals(ModernMatchmaking.State.WAITING,queued.result().state());assertEquals(1,reads.get());
            assertEquals(ModernMatchmaking.State.WAITING,client.read(queued.result().ticketId()).result().state());assertEquals(1,reads.get());
            assertEquals(ModernMatchmaking.State.IDLE,client.cancel(queued.result().ticketId()).result().state());assertEquals(1,reads.get());
            client.start();assertEquals(2,reads.get());client.back();assertEquals(0,services.rooms.queuedMatchmakingCount());
            client.enter(42,2);var next=client.start();assertEquals(3,reads.get());assertEquals(ModernMatchmaking.State.WAITING,next.result().state());
            client.close();peer.success();assertEquals(0,services.rooms.queuedMatchmakingCount());assertEquals(0,services.presence.ownerCount());
        }
    }
    @Test void twoAuthenticatedSourcesMatchIntoOneTruthfulUnreadyRosterAndCancelDoesNotLeave()throws Exception{
        try(var services=new Services(temp);var first=new Peer(services,(p,s,o)->{principal(p,s,o);return standing(s);},2000);
                var second=new Peer(services,(p,s,o)->{principal(p,s,o);return standing(s);},2000);
                var a=new Client(first,34,CAPS);var b=new Client(second,34,CAPS)){
            a.login();b.login();a.enter(7,1);b.enter(42,1);var at=a.start().result();var bt=b.start().result();
            var matched=a.read(at.ticketId()).result();assertEquals(ModernMatchmaking.State.MATCHED,matched.state());
            var joined=matched.rooms().view().joined();assertEquals(2,joined.members().size());assertFalse(joined.summary().readySatisfied());
            assertEquals(Set.of("太陽神","月亮"),joined.members().stream().map(ModernRooms.Member::sourceName).collect(java.util.stream.Collectors.toSet()));
            assertTrue(joined.members().stream().noneMatch(ModernRooms.Member::ready));
            assertEquals(joined,b.read(bt.ticketId()).result().rooms().view().joined());
            assertEquals(ModernMatchmaking.State.MATCHED,a.cancel(at.ticketId()).result().state());assertEquals(2,services.rooms.memberCount());
            a.close();first.success();b.close();second.success();assertEquals(0,services.rooms.roomCount());
        }
    }
    @Test void unavailableNullWrongSourceAndBusyRecordsNeverEnqueueAndCanRecover()throws Exception{
        for(int scenario=0;scenario<5;scenario++){
            final int which=scenario;var reads=new AtomicInteger();
            try(var services=new Services(temp.resolve("bad-"+scenario));var peer=new Peer(services,(p,s,o)->{
                if(reads.incrementAndGet()>1)return standing(s);
                return switch(which){case 0->throw new IOException("sensitive repository detail");case 1->null;
                    case 2->standing(42);case 3->throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.BUSY);
                    default->throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.OVERLOADED);};
            },2000);var client=new Client(peer,34,CAPS)){
                client.login();client.enter(7,1);var denied=client.start();
                assertEquals(which>=3?ModernMatchmakingProtocol.Status.BUSY:ModernMatchmakingProtocol.Status.UNAVAILABLE,denied.status());assertNull(denied.result());
                assertEquals(0,services.rooms.queuedMatchmakingCount());assertEquals(ModernMatchmaking.State.IDLE,client.read(0).result().state());
                assertEquals(ModernMatchmaking.State.WAITING,client.start().result().state());client.close();peer.success();
            }
        }
    }
    @Test void oldVersionMissingCapabilityAndWrongSelectionNeverQueryRecords()throws Exception{
        for(int scenario=0;scenario<7;scenario++){
            var reads=new AtomicInteger();
            try(var services=new Services(temp.resolve("scope-"+scenario));var peer=new Peer(services,(p,s,o)->{reads.incrementAndGet();return standing(s);},2000);
                    var client=new Client(peer,scenario==0?33:34,scenario<2?ModernMatchmakingProtocolTest.BASE_CAPS:CAPS)){
                client.login();client.enter(7,1);if(scenario==6)client.leaveLobby();
                var scope=new ModernRooms.Scope(scenario==2?42:7,scenario==3?3334:3333,scenario==4?2:1,scenario==5?1:0);
                client.send(new ModernApplicationProtocol.MatchmakingRequest(client.next(),ModernMatchmakingProtocolTest.request(scope,ModernMatchmakingProtocol.Action.START,0)),34);
                assertInstanceOf(IOException.class,peer.failure());assertEquals(0,reads.get());assertEquals(0,services.rooms.queuedMatchmakingCount());
            }
        }
    }
    @Test void staleCancelCannotRemoveNewAdmissionAndManualCreateRetiresQueue()throws Exception{
        try(var services=new Services(temp);var peer=new Peer(services,(p,s,o)->standing(s),2000);var client=new Client(peer,34,CAPS)){
            client.login();client.enter(7,1);long old=client.start().result().ticketId();client.cancel(old);long current=client.start().result().ticketId();
            var stale=client.cancel(old).result();assertEquals(ModernRooms.Status.CONFLICT,stale.rooms().status());assertEquals(current,stale.ticketId());
            var room=(ModernApplicationProtocol.RoomResult)client.exchange(new ModernApplicationProtocol.RoomRequest(client.next(),
                    new ModernRooms.Command(client.scope,ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("手動房","",2,1,514,0),"")));
            assertEquals(ModernRooms.Status.OK,room.result().status());assertEquals(0,services.rooms.queuedMatchmakingCount());
            var view=client.read(0).result();assertEquals(ModernMatchmaking.State.IDLE,view.state());assertNotNull(view.rooms().view().joined());
            client.close();peer.success();
        }
    }
    @Test void sourceReturnOrSameLobbyReentryInvalidatesBlockedStartEvenWithSameNumericScope()throws Exception{
        for(int scenario=0;scenario<2;scenario++){
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var worker=Executors.newSingleThreadExecutor();
            try(var services=new Services(temp.resolve("return-"+scenario));var peer=new Peer(services,(p,s,o)->{entered.countDown();await(release);return standing(s);},2000);
                    var client=new Client(peer,34,CAPS)){
                try {
                    client.login();client.enter(7,1);var request=new ModernApplicationProtocol.MatchmakingRequest(90,ModernMatchmakingProtocolTest.request(client.scope,ModernMatchmakingProtocol.Action.START,0));
                    var pending=worker.submit(()->peer.navigation().matchmaking(request));assertTrue(entered.await(2,TimeUnit.SECONDS));
                    if(scenario==0){client.back();}else{client.leaveLobby();client.channel();}
                    release.countDown();assertInstanceOf(IOException.class,assertThrows(ExecutionException.class,()->pending.get(3,TimeUnit.SECONDS)).getCause());
                    if(scenario==0)client.enter(42,2); // Source reload uses the same bounded worker after it actually retires.
                    assertEquals(0,services.rooms.queuedMatchmakingCount());assertEquals(ModernMatchmaking.State.IDLE,client.read(0).result().state());
                    client.close();peer.success();
                }finally{release.countDown();}
            }finally{worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));}
        }
    }
    @Test void closeOrDeadlineBeforeBlockedSqlReturnsCannotAdmitAndWorkerRemainsOwned()throws Exception{
        for(boolean explicitClose:new boolean[]{false,true}){
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            try(var services=new Services(temp.resolve("closed-"+explicitClose));var peer=new Peer(services,(p,s,o)->{entered.countDown();await(release);return standing(s);},explicitClose?2000:300);
                    var client=new Client(peer,34,CAPS)){
                try {
                    client.login();client.enter(7,1);client.send(new ModernApplicationProtocol.MatchmakingRequest(client.next(),ModernMatchmakingProtocolTest.request(client.scope,ModernMatchmakingProtocol.Action.START,0)),34);
                    assertTrue(entered.await(2,TimeUnit.SECONDS));if(explicitClose)peer.transport.get().failClosed();
                    assertInstanceOf(IOException.class,peer.failure());assertEquals(1,peer.backend.activeCount());
                    release.countDown();waitFor(()->peer.backend.activeCount()==0);
                    assertEquals(0,services.rooms.queuedMatchmakingCount());assertEquals(0,services.rooms.roomCount());
                }finally{release.countDown();}
            }
        }
    }
    @Test void boundedWorkerSaturationReturnsBusyWithoutReadingScores()throws Exception{
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var workers=Executors.newFixedThreadPool(2);var reads=new AtomicInteger();
        try(var services=new Services(temp);var peer=new Peer(services,(p,s,o)->{reads.incrementAndGet();return standing(s);},2000);
                var client=new Client(peer,34,CAPS);var first=peer.backend.openSession();var second=peer.backend.openSession()){
            try{
                client.login();client.enter(7,1);
                var a=workers.submit(()->first.call(5000,s->{entered.countDown();await(release);return 1;}));assertTrue(entered.await(2,TimeUnit.SECONDS));
                var b=workers.submit(()->second.call(5000,s->{await(release);return 2;}));waitFor(()->peer.backend.queuedCount()==1);
                assertEquals(ModernMatchmakingProtocol.Status.BUSY,client.start().status());assertEquals(0,reads.get());assertEquals(0,services.rooms.queuedMatchmakingCount());
                release.countDown();assertEquals(1,a.get(3,TimeUnit.SECONDS));assertEquals(2,b.get(3,TimeUnit.SECONDS));
                assertEquals(ModernMatchmaking.State.WAITING,client.start().result().state());client.close();peer.success();
            }finally{release.countDown();}
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
    }
    static void await(CountDownLatch latch)throws IOException{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean interrupted=false;
        try{while(true){long left=end-System.nanoTime();if(left<=0)throw new IOException("fixture barrier timeout");
            try{if(!latch.await(left,TimeUnit.NANOSECONDS))throw new IOException("fixture barrier timeout");return;}catch(InterruptedException ignored){interrupted=true;}}}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static void waitFor(java.util.function.BooleanSupplier condition)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!condition.getAsBoolean()){assertTrue(System.nanoTime()<end);Thread.sleep(1);}
    }
    static final class Services implements AutoCloseable{
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms;final ModernWorldService worlds;final ModernEquipmentOverrideStore store;final ModernEquipmentAuthority equipment;
        Services(Path directory)throws Exception{
            java.nio.file.Files.createDirectories(directory);
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()));
            store=new ModernEquipmentOverrideStore(directory);equipment=new ModernEquipmentAuthority((p,s,o)->{throw new IOException("not a battle fixture");},store);
            worlds=new ModernWorldService(rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,new ModernWorldMap.Cell(32724,32784),100000000L,100000000L)),
                    new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,10000)));
        }
        public void close()throws Exception{worlds.close();assertTrue(worlds.awaitTermination(5000));store.close();assertTrue(store.isQuiescent());
            assertEquals(0,rooms.queuedMatchmakingCount());assertEquals(0,rooms.roomCount());assertEquals(0,presence.ownerCount());}
    }
    static final class Peer implements AutoCloseable{
        final ModernBackendExecutor backend=new ModernBackendExecutor(1,1);final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
        final SSLServerSocket listener;final java.util.concurrent.Future<?> future;final AtomicReference<ModernTlsSessionTransport> transport=new AtomicReference<>();
        final AtomicReference<ModernApplicationSession> session=new AtomicReference<>();
        Peer(Services services,ModernCharacterRecordRepository records,int millis)throws Exception{
            listener=(SSLServerSocket)serverTls.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}));listener.setSoTimeout(3000);
            future=worker.submit(()->{try(var socket=(SSLSocket)listener.accept()){
                var current=new ModernTlsSessionTransport(socket,new ModernSessionBoundary.Policy(CAPS,7,34),new ModernTlsSessionTransport.Limits(2000,2000),services.fixture.verifier);
                transport.set(current);var app=new ModernApplicationSession(current,services.fixture.characters,backend,10000,millis,services.presence,
                        ModernCharacterDataRepository.unavailable(),services.equipment,services.rooms,services.worlds,ModernSkillsRepository.unavailable(),records);
                session.set(app);app.run();return null;
            }});
        }
        int port(){return listener.getLocalPort();}
        ModernNavigationSession navigation()throws Exception{var field=ModernApplicationSession.class.getDeclaredField("navigation");field.setAccessible(true);return(ModernNavigationSession)field.get(session.get());}
        void success()throws Exception{future.get(5,TimeUnit.SECONDS);}
        Throwable failure()throws Exception{return assertThrows(ExecutionException.class,()->future.get(5,TimeUnit.SECONDS)).getCause();}
        public void close()throws Exception{listener.close();if(transport.get()!=null)transport.get().failClosed();worker.shutdownNow();backend.close();
            assertTrue(worker.awaitTermination(5000,TimeUnit.MILLISECONDS));assertTrue(backend.awaitTermination(5000));}
    }
    static final class Client implements AutoCloseable{
        final SSLSocket socket;final int version;int id=2;ModernRooms.Scope scope;
        Client(Peer peer,int version,int caps)throws Exception{
            this.version=version;socket=(SSLSocket)clientTls.getSocketFactory().createSocket(new Socket("127.0.0.1",peer.port()),"localhost",peer.port(),true);
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.setSoTimeout(5000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,caps,7,1)));socket.getOutputStream().flush();
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(version,ack.version());assertEquals(caps,ack.capabilities());
        }
        int next(){return id++;}
        void login()throws Exception{try(var auth=new ModernApplicationProtocol.Auth(next(),ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())){assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());}}
        void enter(int source,long epoch)throws Exception{
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            scope=new ModernRooms.Scope(source,3333,epoch,0);channel();
        }
        void channel()throws Exception{assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),scope.sourceId(),scope.objectId(),scope.epoch(),scope.channelId()))).phase());}
        void leaveLobby()throws Exception{assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());}
        void back()throws Exception{leaveLobby();assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());}
        ModernMatchmakingProtocol.Reply start()throws Exception{return match(ModernMatchmakingProtocol.Action.START,0);}
        ModernMatchmakingProtocol.Reply read(long ticket)throws Exception{return match(ModernMatchmakingProtocol.Action.READ,ticket);}
        ModernMatchmakingProtocol.Reply cancel(long ticket)throws Exception{return match(ModernMatchmakingProtocol.Action.CANCEL,ticket);}
        ModernMatchmakingProtocol.Reply match(ModernMatchmakingProtocol.Action action,long ticket)throws Exception{
            var request=new ModernApplicationProtocol.MatchmakingRequest(next(),ModernMatchmakingProtocolTest.request(scope,action,ticket));
            var result=(ModernApplicationProtocol.MatchmakingResult)exchange(request);assertEquals(request.requestId(),result.requestId());assertEquals(request.request(),result.echo());return result.reply();
        }
        void send(ModernApplicationProtocol.Message request,int version)throws Exception{socket.getOutputStream().write(ModernApplicationProtocol.encode(request,version));socket.getOutputStream().flush();}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request)throws Exception{send(request,version);return ModernApplicationProtocol.decode(ModernApplicationProtocol.readFrame(socket.getInputStream(),version),version);}
        public void close()throws IOException{socket.close();}
    }
}
