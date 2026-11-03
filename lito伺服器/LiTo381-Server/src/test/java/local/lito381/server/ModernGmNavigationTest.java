package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Actual TLS and bounded session worker; only the read-only source provider is synthetic. */
class ModernGmNavigationTest {
    static SSLContext serverTls,clientTls;
    static final int CAPS=303; // stats/auth/data/navigation/rooms + GM; no invented world capability.
    @BeforeAll static void certificates() throws Exception {
        Path fixtures=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixtures);
        serverTls=ModernTlsTestMaterial.serverContext(fixtures,false);clientTls=ModernTlsTestMaterial.clientContext(fixtures);
    }
    static ModernApplicationProtocol.CharacterRow row(int source) {return ModernSkillsNavigationTest.row(source);}
    static ModernGmCommands.Snapshot grant(int source,boolean allowed) {
        return new ModernGmCommands.Snapshot(source,source==7?"sun-owner":"moon-owner",row(source),allowed?200:0,false,false,
                new ModernGmCommands.Definition("de",200,"L1SpawnDe",false));
    }
    static void principal(ModernAccountAuthentication.Principal principal,int source,long object) throws Exception {
        assertTrue(principal.authorizesSource(source));assertEquals(3333,object);
        assertEquals(source==7?"sun-owner":"moon-owner",principal.accountForSource(source));ModernBackendScope.current().checkActive();
    }
    @Test void everyCommandUsesFreshExactSourceGrantAndSuccessfulReplyReflectsActualMembership() throws Exception {
        var calls=new AtomicInteger();var allowed=new AtomicBoolean(true);
        try(var peer=new Peer((p,s,o)->{principal(p,s,o);calls.incrementAndGet();return Optional.of(grant(s,allowed.get()));},2000);
                var client=new Client(peer.port(),7,CAPS)) {
            client.login();client.enter(7,1);var room=client.create(4);
            var first=client.gm(room,20);assertEquals(ModernGmCommandProtocol.Delivery.RESULT,first.delivery());
            assertEquals(3,first.result().added());assertEquals(4,first.result().result().view().joined().members().size());
            assertTrue(first.result().result().view().joined().members().stream().filter(m->!m.identity().isHuman())
                    .allMatch(m->m.identity().sourceId()==7 && m.sourceName().equals("太陽神") && m.ready()));
            allowed.set(false);room=first.result().result().view().joined().summary();var denied=client.gm(room,0);
            assertEquals(ModernRooms.Reason.GM_NOT_AUTHORIZED,denied.result().result().reason());
            assertEquals(4,denied.result().result().view().joined().members().size());assertEquals(room.revision(),denied.result().result().view().joined().summary().revision());
            allowed.set(true);var clear=client.gm(room,0);assertEquals(3,clear.result().removed());
            client.leaveRoom();client.back();client.enter(42,2);room=client.create(3);
            var other=client.gm(room,1);assertEquals(1,other.result().added());
            assertTrue(other.result().result().view().joined().members().stream().filter(m->!m.identity().isHuman()).allMatch(m->m.identity().sourceId()==42));
            assertEquals(4,calls.get());client.close();peer.success();assertEquals(0,peer.presence.ownerCount());assertEquals(0,peer.rooms.roomCount());
        }
    }
    @Test void oldVersionMissingCapabilityAndWrongSelectedScopeNeverReadAuthority() throws Exception {
        for(int scenario=0;scenario<8;scenario++) {
            var calls=new AtomicInteger();
            try(var peer=new Peer((p,s,o)->{calls.incrementAndGet();return Optional.of(grant(s,true));},2000);
                    var client=new Client(peer.port(),scenario==0?6:7,scenario<2?47:CAPS)) {
                client.login();client.enter(7,1);var room=client.create(3);
                if(scenario==6)client.leaveRoom();
                if(scenario==7){client.leaveRoom();client.back();}
                var scope=new ModernRooms.Scope(scenario==2?42:7,scenario==3?3334:3333,scenario==4?2:1,scenario==5?1:0);
                client.send(new ModernApplicationProtocol.GmRequest(client.next(),new ModernGmCommandProtocol.Request(scope,room.id(),room.revision(),1)),7);
                assertInstanceOf(IOException.class,peer.failure());assertEquals(0,calls.get());
            }
        }
    }
    @Test void missingInvalidOrFailedSourceNeverBecomesAnAllowedGrant() throws Exception {
        for(int scenario=0;scenario<7;scenario++) {
            int selected=scenario;
            try(var peer=new Peer((p,s,o)->switch(selected) {
                case 0->Optional.empty();case 1->throw new IOException("synthetic sensitive backend message");
                case 2->Optional.of(grant(42,true));case 3->Optional.of(new ModernGmCommands.Snapshot(7,"other-owner",row(7),200,false,false,grant(7,true).definition()));
                case 4->Optional.of(new ModernGmCommands.Snapshot(7,"sun-owner",new ModernApplicationProtocol.CharacterRow(3333,"changed",1,0,80),200,false,false,grant(7,true).definition()));
                case 5->throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.BUSY);default->null;
            },2000);var client=new Client(peer.port(),7,CAPS)) {
                client.login();client.enter(7,1);var room=client.create(3);var reply=client.gm(room,1);
                assertEquals(scenario==0?ModernGmCommandProtocol.Delivery.RESULT:scenario==5?ModernGmCommandProtocol.Delivery.BUSY:ModernGmCommandProtocol.Delivery.UNAVAILABLE,reply.delivery());
                if(scenario==0)assertEquals(ModernRooms.Reason.GM_NOT_AUTHORIZED,reply.result().result().reason());else assertNull(reply.result());
                assertEquals(room,client.view().joined().summary());client.ping();client.close();peer.success();
            }
        }
    }
    @Test void roomRevisionIsRecheckedAfterBlockingPermissionReadUnderSameCommitMonitor() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var worker=Executors.newSingleThreadExecutor();
        try(var peer=new Peer((p,s,o)->{entered.countDown();await(release);return Optional.of(grant(s,true));},2000);
                var client=new Client(peer.port(),7,CAPS)) {
            try {
                client.login();client.enter(7,1);var room=client.create(3);var request=client.request(room,1);
                var pending=worker.submit(()->peer.navigation().gm(request));assertTrue(entered.await(2,TimeUnit.SECONDS));
                var after=client.act(ModernRooms.Action.READY,1);release.countDown();var reply=pending.get(3,TimeUnit.SECONDS);
                assertEquals(ModernRooms.Status.CONFLICT,reply.result().result().status());assertEquals(after.view(),reply.result().result().view());
                assertEquals(1,reply.result().result().view().joined().members().size());client.close();peer.success();
            }finally{release.countDown();}
        }finally{worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void leaveReturnAndReselectCannotReuseAnOldInFlightPermissionGrant() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var worker=Executors.newSingleThreadExecutor();
        try(var peer=new Peer((p,s,o)->{entered.countDown();await(release);return Optional.of(grant(s,true));},2000);
                var client=new Client(peer.port(),7,CAPS)) {
            try {
                client.login();client.enter(7,1);var room=client.create(3);var request=client.request(room,1);
                var pending=worker.submit(()->peer.navigation().gm(request));assertTrue(entered.await(2,TimeUnit.SECONDS));
                client.leaveRoom();client.back();release.countDown();
                assertInstanceOf(IOException.class,assertThrows(ExecutionException.class,()->pending.get(3,TimeUnit.SECONDS)).getCause());
                client.enter(42,2);assertTrue(client.view().rooms().isEmpty());assertEquals(1L,field(peer.rooms,"nextBotId"));
                client.close();peer.success();
            }finally{release.countDown();}
        }finally{worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void explicitCloseRejectsLateAllowedResultAndWorkerRemainsOwnedUntilActualExit() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var peer=new Peer((p,s,o)->{entered.countDown();await(release);return Optional.of(grant(s,true));},2000);
                var client=new Client(peer.port(),7,CAPS)) {
            try {
                client.login();client.enter(7,1);var room=client.create(3);client.send(new ModernApplicationProtocol.GmRequest(client.next(),client.request(room,1)),7);
                assertTrue(entered.await(2,TimeUnit.SECONDS));peer.transport.get().failClosed();assertEquals(1,peer.backend.activeCount());
                release.countDown();assertInstanceOf(IOException.class,peer.failure());waitFor(()->peer.backend.activeCount()==0);
                assertEquals(1L,field(peer.rooms,"nextBotId"));assertEquals(0,peer.rooms.roomCount());assertEquals(0,peer.presence.ownerCount());
            }finally{release.countDown();}
        }
    }
    @Test void absoluteBackendDeadlineCannotGrantAfterAnInterruptIgnoringRepositoryReturns() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var peer=new Peer((p,s,o)->{entered.countDown();await(release);return Optional.of(grant(s,true));},300);
                var client=new Client(peer.port(),7,CAPS)) {
            try {
                client.login();client.enter(7,1);var room=client.create(3);client.send(new ModernApplicationProtocol.GmRequest(client.next(),client.request(room,1)),7);
                assertTrue(entered.await(2,TimeUnit.SECONDS));assertInstanceOf(IOException.class,peer.failure());assertEquals(1,peer.backend.activeCount());
                release.countDown();waitFor(()->peer.backend.activeCount()==0);assertEquals(1L,field(peer.rooms,"nextBotId"));assertEquals(0,peer.rooms.roomCount());
            }finally{release.countDown();}
        }
    }
    @Test void realQueueSaturationDoesNotReadGrantAndRetryRemainsBoundToSelectedRoom() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var callers=Executors.newFixedThreadPool(2);var reads=new AtomicInteger();
        try(var peer=new Peer((p,s,o)->{reads.incrementAndGet();return Optional.of(grant(s,true));},2000);var client=new Client(peer.port(),7,CAPS);
                var first=peer.backend.openSession();var second=peer.backend.openSession()) {
            try {
                client.login();client.enter(7,1);var room=client.create(3);
                var a=callers.submit(()->first.call(5000,scope->{entered.countDown();await(release);return 1;}));assertTrue(entered.await(2,TimeUnit.SECONDS));
                var b=callers.submit(()->second.call(5000,scope->{await(release);return 2;}));waitFor(()->peer.backend.queuedCount()==1);
                assertEquals(ModernGmCommandProtocol.Delivery.BUSY,client.gm(room,1).delivery());assertEquals(0,reads.get());
                release.countDown();assertEquals(1,a.get(3,TimeUnit.SECONDS));assertEquals(2,b.get(3,TimeUnit.SECONDS));
                assertEquals(1,client.gm(room,1).result().added());assertEquals(1,reads.get());client.close();peer.success();
            }finally{release.countDown();}
        }finally{callers.shutdownNow();assertTrue(callers.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void productionServerLongestConstructorForwardsOnlyExplicitGmProvider() throws Exception {
        var fixture=new ModernApplicationTestBackend("auth");var presence=new TournamentPresence();var rooms=rooms();var reads=new AtomicInteger();
        ModernServer server=null;
        try {
            server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),serverTls,
                    new ModernServer.Limits(2,2,60,2,1,1,2000,2000,2000,10000),fixture.verifier,fixture.characters,presence,
                    ModernCharacterDataRepository.unavailable(),null,null,rooms,null,7,ModernSkillsRepository.unavailable(),
                    ModernCharacterRecordRepository.unavailable(),null,(p,s,o)->{principal(p,s,o);reads.incrementAndGet();return Optional.of(grant(s,true));});
            try(var client=new Client(server.address().getPort(),7,CAPS)) {
                client.login();client.enter(7,1);assertEquals(1,client.gm(client.create(3),1).result().added());client.ping();
            }
        }finally{if(server!=null){server.close();assertTrue(server.awaitTermination(5000));}}
        assertEquals(1,reads.get());assertEquals(0,presence.ownerCount());assertEquals(0,rooms.roomCount());
    }
    static ModernRooms rooms() {var key=new ModernWorldBinding.Key(1,514);return new ModernRooms(Set.of(key),Set.of(key));}
    private static Object field(Object object,String name) throws Exception {Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void await(CountDownLatch latch) throws IOException {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean interrupted=false;
        try{for(;;){long left=deadline-System.nanoTime();if(left<=0)throw new IOException("fixture barrier deadline");
            try{if(!latch.await(left,TimeUnit.NANOSECONDS))throw new IOException("fixture barrier deadline");return;}
            catch(InterruptedException ignored){interrupted=true;}}}finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void waitFor(java.util.function.BooleanSupplier predicate) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!predicate.getAsBoolean()){assertTrue(System.nanoTime()<deadline);Thread.sleep(1);}
    }
    private static final class Peer implements AutoCloseable {
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms=rooms();final ModernBackendExecutor backend=new ModernBackendExecutor(1,1);
        final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();final SSLServerSocket listener;
        final java.util.concurrent.Future<?> future;final AtomicReference<ModernTlsSessionTransport> transport=new AtomicReference<>();
        final AtomicReference<ModernApplicationSession> session=new AtomicReference<>();
        Peer(ModernGmCommandRepository repository,int backendMillis) throws Exception {
            listener=(SSLServerSocket)serverTls.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}));listener.setSoTimeout(3000);
            future=worker.submit(()->{try(var socket=(SSLSocket)listener.accept()) {
                var current=new ModernTlsSessionTransport(socket,ModernSessionBoundary.Policy.rooms(false),new ModernTlsSessionTransport.Limits(2000,2000),fixture.verifier);
                transport.set(current);var app=new ModernApplicationSession(current,fixture.characters,backend,10000,backendMillis,presence,
                        ModernCharacterDataRepository.unavailable(),null,rooms,null,ModernSkillsRepository.unavailable(),ModernCharacterRecordRepository.unavailable(),repository);
                session.set(app);app.run();return null;
            }});
        }
        int port(){return listener.getLocalPort();}
        ModernNavigationSession navigation() throws Exception{return (ModernNavigationSession)field(session.get(),"navigation");}
        void success() throws Exception{future.get(5,TimeUnit.SECONDS);}
        Throwable failure() throws Exception{return assertThrows(ExecutionException.class,()->future.get(5,TimeUnit.SECONDS)).getCause();}
        @Override public void close() throws Exception {
            listener.close();if(transport.get()!=null)transport.get().failClosed();worker.shutdownNow();backend.close();
            assertTrue(worker.awaitTermination(5000,TimeUnit.MILLISECONDS));assertTrue(backend.awaitTermination(5000));
        }
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;final int version;int requestId=2;ModernRooms.Scope scope;
        Client(int port,int version,int capabilities) throws Exception {
            this.version=version;socket=(SSLSocket)clientTls.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(5000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,capabilities,7,1)));socket.getOutputStream().flush();
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(version,ack.version());assertEquals(capabilities,ack.capabilities());
        }
        int next(){return requestId++;}
        void login() throws Exception {try(var auth=new ModernApplicationProtocol.Auth(next(),ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())){
            assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());}}
        void enter(int source,long epoch) throws Exception {
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,3333,epoch,0))).phase());scope=new ModernRooms.Scope(source,3333,epoch,0);
        }
        ModernRooms.Summary create(int capacity) throws Exception {return room(new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,
                new ModernRooms.Draft("GM training","",capacity,1,514,ModernRooms.ALLOW_UPPER_TIER),"")).view().joined().summary();}
        ModernRooms.Result room(ModernRooms.Command command) throws Exception{return ((ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next(),command))).result();}
        ModernRooms.View view() throws Exception{return room(new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view();}
        ModernRooms.Result act(ModernRooms.Action action,int value) throws Exception {var room=view().joined().summary();return room(new ModernRooms.Command(scope,action,room.id(),room.revision(),value,null,""));}
        void leaveRoom() throws Exception {assertEquals(ModernRooms.Status.OK,act(ModernRooms.Action.LEAVE,0).status());}
        void back() throws Exception {assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());}
        ModernGmCommandProtocol.Request request(ModernRooms.Summary room,int count){return new ModernGmCommandProtocol.Request(scope,room.id(),room.revision(),count);}
        ModernGmCommandProtocol.Reply gm(ModernRooms.Summary room,int count) throws Exception {
            var request=request(room,count);int id=next();var result=(ModernApplicationProtocol.GmResult)exchange(new ModernApplicationProtocol.GmRequest(id,request));
            assertEquals(id,result.requestId());assertEquals(request,result.echo());return result.reply();
        }
        void ping() throws Exception {int id=next();assertEquals(new ModernApplicationProtocol.Pong(id),exchange(new ModernApplicationProtocol.Ping(id)));}
        void send(ModernApplicationProtocol.Message request,int version) throws Exception {byte[] bytes=ModernApplicationProtocol.encode(request,version);
            try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);}}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {send(request,version);byte[] bytes=ModernApplicationProtocol.readFrame(socket.getInputStream());
            try{return ModernApplicationProtocol.decode(bytes,version);}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}}
        @Override public void close() throws IOException {socket.close();}
    }
}
