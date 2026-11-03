package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;

/** Actual verified TLS -> App dispatcher -> bounded source read -> selected lease -> room/shop commit. */
class ModernShopNavigationTest {
    @TempDir Path directory;
    static SSLContext serverTls,clientTls;
    @BeforeAll static void material()throws Exception {
        var fixture=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixture);
        serverTls=ModernTlsTestMaterial.serverContext(fixture,false);clientTls=ModernTlsTestMaterial.clientContext(fixture);
    }
    static ModernShopCatalog.Snapshot fresh(ModernAccountAuthentication.Principal principal,int source,long object)throws Exception {
        assertTrue(principal.authorizesSource(source));assertEquals(3333,object);ModernBackendScope.current().checkActive();
        var fixture=new ModernApplicationTestBackend("auth");var row=fixture.characters.characters(principal,source).stream().filter(c->c.objectId()==object).findFirst().orElseThrow();
        return new ModernShopCatalog.Snapshot(source,principal.accountForSource(source),row,List.of(ModernShopAuthorityTest.offer(40,40)));
    }
    static ModernShopProtocol.Request readRequest(ModernRooms.Scope scope,ModernRooms.Summary room) {
        return new ModernShopProtocol.Request(scope,Action.READ,room==null?0:room.id(),room==null?0:room.revision(),0,0,new Fingerprint(new byte[32]),0,0);
    }
    @Test void realDispatcherBuysSellsInLobbyAndRoomReadCancelsReadyWithConfirmedRoomAck()throws Exception {
        var reads=new AtomicInteger();
        try(var peer=new Peer((p,s,o)->{reads.incrementAndGet();return Optional.of(fresh(p,s,o));},2000,directory);var c=new Client(peer.port(),8)) {
            c.enter(7,1);var opened=c.shop(readRequest(c.scope,null));assertEquals(20000,opened.outcome().snapshot().balance());
            var bought=c.mutate(Action.BUY,opened,3);assertEquals(19880,bought.outcome().snapshot().balance());
            var sold=c.mutate(Action.SELL,bought,1);assertEquals(19920,sold.outcome().snapshot().balance());
            c.create();c.act(ModernRooms.Action.READY,1);var room=c.view().joined().summary();assertEquals(1,room.ready());
            var inRoom=c.shop(readRequest(c.scope,room));assertEquals(0,inRoom.rooms().view().joined().summary().ready());
            assertEquals(room.revision()+1,inRoom.rooms().view().joined().summary().revision());assertEquals(sold.outcome().snapshot(),inRoom.outcome().snapshot());
            c.act(ModernRooms.Action.LEAVE,0);c.back();c.enter(42,2);var moon=c.shop(readRequest(c.scope,null));
            assertEquals(20000,moon.outcome().snapshot().balance());assertTrue(moon.outcome().snapshot().stacks().isEmpty());assertEquals(5,reads.get());
            c.close();peer.success();assertEquals(0,peer.presence.ownerCount());assertEquals(0,peer.rooms.ownerCount());
        }
    }
    @Test void wrongContextAndLegacyVersionCannotReachTheSourceShop()throws Exception {
        var reads=new AtomicInteger();
        try(var peer=new Peer((p,s,o)->{reads.incrementAndGet();return Optional.of(fresh(p,s,o));},2000,directory);var c=new Client(peer.port(),8)) {
            c.enter(7,1);
            for(var scope:List.of(new ModernRooms.Scope(42,3333,1,0),new ModernRooms.Scope(7,3334,1,0),new ModernRooms.Scope(7,3333,2,0),new ModernRooms.Scope(7,3333,1,1)))
                assertThrows(IOException.class,()->peer.navigation().shop(readRequest(scope,null)));
            assertEquals(0,reads.get());c.close();peer.success();
        }
        try(var peer=new Peer((p,s,o)->{reads.incrementAndGet();return Optional.of(fresh(p,s,o));},2000,directory);var c=new Client(peer.port(),7)) {
            c.enter(7,1);assertThrows(IOException.class,()->peer.navigation().shop(readRequest(c.scope,null)));assertEquals(0,reads.get());c.close();peer.success();
        }
    }
    @Test void actualFreshReadFailureOrWrongOwnerIsNotAnEmptyFreeShop()throws Exception {
        for(int scenario=0;scenario<4;scenario++) {
            int kind=scenario;
            try(var peer=new Peer((p,s,o)->{if(kind==0)throw new IOException("synthetic-private-error");if(kind==1)return Optional.empty();
                if(kind==2)return Optional.of(fresh(p,42,o));var f=fresh(p,s,o);return Optional.of(new ModernShopCatalog.Snapshot(s,"wrong",f.character(),f.offers()));},2000,directory);
                var c=new Client(peer.port(),8)) {
                c.enter(7,1);var result=c.shop(readRequest(c.scope,null));assertEquals(Status.UNAVAILABLE,result.outcome().status());assertNull(result.outcome().snapshot());assertNull(result.rooms());c.close();peer.success();
            }
        }
    }
    @Test void roomRevisionChangeDuringReadReturnsConflictWithoutCancellingNewReady()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var workers=Executors.newSingleThreadExecutor();
        try(var peer=new Peer((p,s,o)->{var f=fresh(p,s,o);entered.countDown();await(release);return Optional.of(f);},2000,directory);var c=new Client(peer.port(),8)) {
            try{c.enter(7,1);var room=c.create();var request=readRequest(c.scope,room);var pending=workers.submit(()->peer.navigation().shop(request));
                assertTrue(entered.await(2,TimeUnit.SECONDS));var after=c.act(ModernRooms.Action.READY,1);release.countDown();var reply=pending.get(3,TimeUnit.SECONDS);
                assertEquals(Reason.ROOM_CHANGED,reply.outcome().reason());assertEquals(after.view(),reply.rooms().view());assertEquals(20000,reply.outcome().snapshot().balance());
                c.close();peer.success();}finally{release.countDown();}
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void returnedSelectionRejectsLateReadAndNewSourceGetsIndependentAllowance()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var workers=Executors.newSingleThreadExecutor();var calls=new AtomicInteger();
        try(var peer=new Peer((p,s,o)->{var f=fresh(p,s,o);if(calls.getAndIncrement()==0){entered.countDown();await(release);}return Optional.of(f);},2000,directory);var c=new Client(peer.port(),8)) {
            try{c.enter(7,1);var request=readRequest(c.scope,null);var pending=workers.submit(()->peer.navigation().shop(request));
                assertTrue(entered.await(2,TimeUnit.SECONDS));c.back();release.countDown();assertInstanceOf(IOException.class,assertThrows(ExecutionException.class,()->pending.get(3,TimeUnit.SECONDS)).getCause());
                c.enter(42,2);assertEquals(20000,c.shop(readRequest(c.scope,null)).outcome().snapshot().balance());c.close();peer.success();}
            finally{release.countDown();}
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void closedConnectionCancelsReadButCapacityOnlyRetiresAfterActualWorkerExit()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var peer=new Peer((p,s,o)->{var f=fresh(p,s,o);entered.countDown();await(release);return Optional.of(f);},2000,directory);var c=new Client(peer.port(),8)) {
            try{c.enter(7,1);c.send(new ModernApplicationProtocol.ShopRequest(c.next(),readRequest(c.scope,null)),8);
                assertTrue(entered.await(2,TimeUnit.SECONDS));peer.transport.get().failClosed();assertEquals(1,peer.backend.activeCount());release.countDown();
                assertInstanceOf(IOException.class,peer.failure());waitFor(()->peer.backend.activeCount()==0);assertEquals(0,peer.rooms.ownerCount());}
            finally{release.countDown();}
        }
    }
    @Test void expiredReadCannotCommitAndStillOwnsInterruptIgnoringBackend()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var peer=new Peer((p,s,o)->{var f=fresh(p,s,o);entered.countDown();await(release);return Optional.of(f);},300,directory);var c=new Client(peer.port(),8)) {
            try{c.enter(7,1);c.send(new ModernApplicationProtocol.ShopRequest(c.next(),readRequest(c.scope,null)),8);assertTrue(entered.await(2,TimeUnit.SECONDS));
                assertInstanceOf(IOException.class,peer.failure());assertEquals(1,peer.backend.activeCount());release.countDown();waitFor(()->peer.backend.activeCount()==0);}
            finally{release.countDown();}
        }
    }
    static void await(CountDownLatch latch)throws IOException {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean interrupted=false;
        try{for(;;){long left=deadline-System.nanoTime();if(left<=0)throw new IOException("fixture barrier expired");try{if(!latch.await(left,TimeUnit.NANOSECONDS))throw new IOException("fixture barrier expired");return;}catch(InterruptedException ignored){interrupted=true;}}}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static void waitFor(java.util.function.BooleanSupplier predicate)throws Exception {long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!predicate.getAsBoolean()){assertTrue(System.nanoTime()<deadline);Thread.sleep(1);}}
    static Object field(Object value,String name)throws Exception {var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    static final class Peer implements AutoCloseable {
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms;final ModernWorldService worlds;final ModernEquipmentOverrideStore store;
        final ModernBackendExecutor backend=new ModernBackendExecutor(1,1);final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
        final SSLServerSocket listener;final java.util.concurrent.Future<?> future;
        final AtomicReference<ModernTlsSessionTransport> transport=new AtomicReference<>();final AtomicReference<ModernApplicationSession> app=new AtomicReference<>();
        Peer(ModernShopCatalogRepository repo,int millis,Path directory)throws Exception {
            store=new ModernEquipmentOverrideStore(directory);var authority=new ModernEquipmentAuthority((p,s,o)->{throw new IOException("unused fixture equipment read");},store);
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()));
            worlds=new ModernWorldService(rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,new ModernWorldMap.Cell(32724,32784),50_000_000,1_000_000_000L,0,false,false,true)),
                    new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,10000)));
            listener=(SSLServerSocket)serverTls.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}));listener.setSoTimeout(3000);
            future=worker.submit(()->{try(var socket=(SSLSocket)listener.accept()) {
                var tls=new ModernTlsSessionTransport(socket,ModernSessionBoundary.Policy.world(true,true,true),new ModernTlsSessionTransport.Limits(2000,2000),fixture.verifier);
                transport.set(tls);var session=new ModernApplicationSession(tls,fixture.characters,backend,10000,millis,presence,ModernCharacterDataRepository.unavailable(),authority,rooms,worlds,
                        ModernSkillsRepository.unavailable(),ModernCharacterRecordRepository.unavailable(),ModernGmCommandRepository.unavailable(),repo,20000);
                app.set(session);session.run();return null;
            }});
        }
        int port(){return listener.getLocalPort();}ModernNavigationSession navigation()throws Exception{return (ModernNavigationSession)field(app.get(),"navigation");}
        void success()throws Exception{future.get(5,TimeUnit.SECONDS);}Throwable failure()throws Exception{return assertThrows(ExecutionException.class,()->future.get(5,TimeUnit.SECONDS)).getCause();}
        @Override public void close()throws Exception {listener.close();if(transport.get()!=null)transport.get().failClosed();worker.shutdownNow();backend.close();worlds.close();
            assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS));assertTrue(backend.awaitTermination(5000));assertTrue(worlds.awaitTermination(5000));store.close();}
    }
    static final class Client implements AutoCloseable {
        final SSLSocket socket;final int version;int id=2;boolean authenticated;ModernRooms.Scope scope;
        Client(int port,int version)throws Exception {
            this.version=version;socket=(SSLSocket)clientTls.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(5000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,1023,7,1)));socket.getOutputStream().flush();
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(version,ack.version());assertEquals(version==8?1023:511,ack.capabilities());
        }
        int next(){return id++;}
        void enter(int source,long epoch)throws Exception {
            if(!authenticated){try(var auth=new ModernApplicationProtocol.Auth(next(),ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())){assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());}authenticated=true;}
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,3333,epoch,0))).phase());scope=new ModernRooms.Scope(source,3333,epoch,0);
        }
        ModernRooms.Result room(ModernRooms.Command command)throws Exception{return ((ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next(),command))).result();}
        ModernRooms.View view()throws Exception{return room(new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view();}
        ModernRooms.Summary create()throws Exception{return room(new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("shop room","",4,1,514,ModernRooms.ALLOW_UPPER_TIER),"")).view().joined().summary();}
        ModernRooms.Result act(ModernRooms.Action action,int value)throws Exception {var room=view().joined().summary();return room(new ModernRooms.Command(scope,action,room.id(),room.revision(),value,null,""));}
        ModernShopProtocol.Reply shop(ModernShopProtocol.Request request)throws Exception {var reply=(ModernApplicationProtocol.ShopResult)exchange(new ModernApplicationProtocol.ShopRequest(next(),request));assertEquals(request,reply.echo());return reply.reply();}
        ModernShopProtocol.Reply mutate(Action action,ModernShopProtocol.Reply previous,long quantity)throws Exception {var s=previous.outcome().snapshot();var room=previous.rooms().view().joined();return shop(new ModernShopProtocol.Request(scope,action,room==null?0:room.summary().id(),room==null?0:room.summary().revision(),s.roundId(),s.revision(),s.token(),40010,quantity));}
        void back()throws Exception {assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());}
        void send(ModernApplicationProtocol.Message request,int version)throws Exception {byte[] bytes=ModernApplicationProtocol.encode(request,version);try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{java.util.Arrays.fill(bytes,(byte)0);}}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request)throws Exception {send(request,version);byte[] bytes=ModernApplicationProtocol.readFrame(socket.getInputStream());try{return ModernApplicationProtocol.decode(bytes,version);}finally{if(bytes!=null)java.util.Arrays.fill(bytes,(byte)0);}}
        @Override public void close()throws IOException{socket.close();}
    }
}
