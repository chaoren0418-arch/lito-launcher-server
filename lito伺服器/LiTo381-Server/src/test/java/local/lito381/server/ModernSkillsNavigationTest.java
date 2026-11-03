package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/** Synthetic SELECT providers behind real TLS/APP/navigation/lease; no database or deployed listener. */
class ModernSkillsNavigationTest {
    static SSLContext serverTls,clientTls;
    @TempDir Path directory;
    private int fixtureNumber;
    @BeforeAll static void certificates()throws Exception {
        Path fixtures=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixtures);
        serverTls=ModernTlsTestMaterial.serverContext(fixtures,false);clientTls=ModernTlsTestMaterial.clientContext(fixtures);
    }
    static ModernApplicationProtocol.CharacterRow row(int source) {
        return new ModernApplicationProtocol.CharacterRow(3333,source==7?"Sun Knight":"Moon Mage",source==7?1:3,source==7?0:1,source==7?80:120000);
    }
    static ModernSkills.Snapshot snapshot(int source) {
        return new ModernSkills.Snapshot(source,row(source),source==7?List.of(ModernSkillsTest.teleport())
                :List.of(new ModernSkills.Skill(6,"$ice",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNIMPLEMENTED)));
    }
    private Peer peer(ModernSkillsRepository skills)throws Exception{return new Peer(new Services(nextDirectory()),skills);}
    private Path nextDirectory()throws Exception{return Files.createDirectory(directory.resolve("fixture-"+(++fixtureNumber)));}
    private static void principal(ModernAccountAuthentication.Principal principal,int source)throws IOException {
        assertTrue(principal.authorizesSource(source));assertEquals(source==7?"sun-owner":"moon-owner",principal.accountForSource(source));
        ModernBackendScope.current().checkActive();
    }

    @Test void v4SkillReadsPreserveChannelsLobbyRoleAndExactCanonicalAcrossSourceSwitch()throws Exception {
        var calls=new AtomicInteger();
        try(var peer=peer((p,s,o)->{principal(p,s);calls.incrementAndGet();return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255)) {
            client.login();client.select(7,1);
            assertEquals(snapshot(7).skills(),client.skills(7,3333,1).reply().skills());assertEquals(0,peer.services.presence.userCount());
            assertRoleHeld(peer.services.presence,7);
            assertEquals(2,client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,2)).phase());
            var channels=peer.services.presence.snapshots();assertEquals(snapshot(7).skills(),client.skills(7,3333,1).reply().skills());
            assertEquals(channels,peer.services.presence.snapshots());assertEquals(1,peer.services.worlds.registrationCount());
            assertEquals(1,client.nav(new ModernApplicationProtocol.LobbyLeave(client.next(),7,3333,1)).phase());
            assertEquals(snapshot(7).skills(),client.skills(7,3333,1).reply().skills());assertEquals(0,peer.services.worlds.registrationCount());
            client.nav(new ModernApplicationProtocol.CharacterReturn(client.next(),7,3333,1));client.select(42,2);
            assertEquals(snapshot(42).skills(),client.skills(42,3333,2).reply().skills());assertEquals(4,calls.get());
            client.ping();client.close();peer.success();assertEquals(0,peer.services.presence.ownerCount());
        }
    }
    @Test void versionAndNegotiatedCombatCapabilityGateSkillRequestsBeforeRepository()throws Exception {
        for(int scenario=0;scenario<2;scenario++) {
            var calls=new AtomicInteger();
            try(var peer=peer((p,s,o)->{calls.incrementAndGet();return Optional.of(snapshot(s));});
                    var client=new Client(peer.port(),scenario==0?3:4,scenario==0?255:111)) {
                client.login();client.select(7,1);
                client.send(new ModernApplicationProtocol.SkillsRequest(client.next(),new ModernSpellProtocol.Context(7,3333,1)),4);
                assertInstanceOf(IOException.class,peer.failure());assertEquals(0,calls.get());
            }
        }
    }
    @Test void unauthenticatedUnselectedWrongSourceObjectEpochAndReturnedRequestsNeverCallRepository()throws Exception {
        for(int scenario=0;scenario<7;scenario++) {
            var calls=new AtomicInteger();
            try(var peer=peer((p,s,o)->{calls.incrementAndGet();return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255)) {
                if(scenario!=0)client.login();
                if(scenario>=2)client.select(7,1);
                if(scenario==6)client.nav(new ModernApplicationProtocol.CharacterReturn(client.next(),7,3333,1));
                var context=new ModernSpellProtocol.Context(scenario==2?42:7,scenario==3?3334:3333,scenario==4?2:1);
                if(scenario==5)context=new ModernSpellProtocol.Context(99,3333,1);
                client.send(new ModernApplicationProtocol.SkillsRequest(client.next(),context),4);
                assertInstanceOf(IOException.class,peer.failure());assertEquals(0,calls.get());
                assertEquals(0,peer.services.presence.ownerCount());
            }
        }
    }
    @Test void missingBusyUnavailableWrongSourceAndChangedCharacterReturnNoSkillsAndKeepSelection()throws Exception {
        for(int scenario=0;scenario<7;scenario++) {
            final int selected=scenario;
            try(var peer=peer((p,s,o)->{
                principal(p,s);
                return switch(selected) {
                    case 0->Optional.empty();
                    case 1->throw new IOException("synthetic secret");
                    case 2->throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.BUSY);
                    case 3->Optional.of(snapshot(42));
                    case 4->Optional.of(new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3333,"changed",1,0,80),List.of()));
                    case 5->Optional.of(new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3334,"Sun Knight",1,0,80),List.of()));
                    default->null;
                };
            });var client=new Client(peer.port(),4,255)) {
                client.login();client.select(7,1);var reply=client.skills(7,3333,1).reply();
                assertEquals(scenario==0?ModernSpellProtocol.Status.DENIED:scenario==2?ModernSpellProtocol.Status.BUSY:ModernSpellProtocol.Status.UNAVAILABLE,reply.status());
                assertTrue(reply.skills().isEmpty());assertRoleHeld(peer.services.presence,7);client.ping();
                assertEquals(0,client.nav(new ModernApplicationProtocol.CharacterReturn(client.next(),7,3333,1)).phase());
                client.close();peer.success();
            }
        }
    }
    @Test void realBackendQueueSaturationIsBusyAndRetryUsesSameSelectedOwner()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var callers=Executors.newFixedThreadPool(2);var calls=new AtomicInteger();
        try(var peer=peer((p,s,o)->{calls.incrementAndGet();return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255);
                var first=peer.backend.openSession();var second=peer.backend.openSession()) {
            try {
                client.login();client.select(7,1);
                var active=callers.submit(()->first.call(5000,scope->{entered.countDown();await(release);return 1;}));
                assertTrue(entered.await(2,TimeUnit.SECONDS));
                var queued=callers.submit(()->second.call(5000,scope->{await(release);return 2;}));
                waitFor(()->peer.backend.queuedCount()==1);
                assertEquals(ModernSpellProtocol.Status.BUSY,client.skills(7,3333,1).reply().status());assertEquals(0,calls.get());
                release.countDown();assertEquals(1,active.get(3,TimeUnit.SECONDS));assertEquals(2,queued.get(3,TimeUnit.SECONDS));
                assertEquals(ModernSpellProtocol.Status.OK,client.skills(7,3333,1).reply().status());assertEquals(1,calls.get());
                client.close();peer.success();
            }finally{release.countDown();}
        }finally{callers.shutdownNow();assertTrue(callers.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void explicitCloseCancelsReadButCountsWorkerUntilFinallyAndCannotPublishLateSuccess()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var peer=peer((p,s,o)->{entered.countDown();await(release);return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255)) {
            try {
                client.login();client.select(7,1);client.send(new ModernApplicationProtocol.SkillsRequest(client.next(),new ModernSpellProtocol.Context(7,3333,1)),4);
                assertTrue(entered.await(2,TimeUnit.SECONDS));peer.transport.get().failClosed();
                assertEquals(1,peer.backend.activeCount());release.countDown();assertInstanceOf(IOException.class,peer.failure());
                waitFor(()->peer.backend.activeCount()==0);assertEquals(0,peer.services.presence.ownerCount());
            }finally{release.countDown();}
        }
    }
    @Test void backInvalidatesInFlightDirectReadEvenWhenRepositoryLaterReturnsGenericFailure()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var caller=Executors.newSingleThreadExecutor();
        try(var peer=peer((p,s,o)->{entered.countDown();await(release);throw new IOException("synthetic");});var client=new Client(peer.port(),4,255)) {
            try {
                client.login();client.select(7,1);
                var pending=caller.submit(()->peer.navigation().skills(new ModernApplicationProtocol.SkillsRequest(90,new ModernSpellProtocol.Context(7,3333,1))));
                assertTrue(entered.await(2,TimeUnit.SECONDS));
                assertEquals(0,client.nav(new ModernApplicationProtocol.CharacterReturn(client.next(),7,3333,1)).phase());
                release.countDown();assertInstanceOf(IOException.class,assertThrows(ExecutionException.class,()->pending.get(3,TimeUnit.SECONDS)).getCause());
                client.close();peer.success();
            }finally{release.countDown();}
        }finally{caller.shutdownNow();assertTrue(caller.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void registeredFreshReaderUsesActualPrincipalScopeAndRejectsRetiredAttachmentBeforeRead()throws Exception {
        var calls=new AtomicInteger();
        try(var peer=peer((p,s,o)->{principal(p,s);calls.incrementAndGet();return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255)) {
            client.login();client.select(7,1);client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,1));
            var reader=peer.skillsReader();assertNotNull(reader);
            assertEquals(snapshot(7),reader.read(System.nanoTime()+TimeUnit.SECONDS.toNanos(1)));assertEquals(1,calls.get());
            assertThrows(IOException.class,()->reader.read(System.nanoTime()));assertEquals(1,calls.get());
            client.nav(new ModernApplicationProtocol.LobbyLeave(client.next(),7,3333,1));
            assertThrows(IOException.class,()->reader.read(System.nanoTime()+TimeUnit.SECONDS.toNanos(1)));assertEquals(1,calls.get());
            client.nav(new ModernApplicationProtocol.CharacterReturn(client.next(),7,3333,1));client.select(7,1);
            client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,1));
            assertThrows(IOException.class,()->reader.read(System.nanoTime()+TimeUnit.SECONDS.toNanos(1)));assertEquals(1,calls.get());
            assertEquals(snapshot(7),peer.skillsReader().read(System.nanoTime()+TimeUnit.SECONDS.toNanos(1)));
            client.close();peer.success();
        }
    }
    @Test void freshReaderRejectsChangedCharacterAndMissingQualificationNotFakeEmpty()throws Exception {
        for(boolean missing:List.of(false,true)) {
            try(var peer=peer((p,s,o)->missing?Optional.empty():Optional.of(snapshot(42)));var client=new Client(peer.port(),4,255)) {
                client.login();client.select(7,1);client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,0));
                assertThrows(IOException.class,()->peer.skillsReader().read(System.nanoTime()+TimeUnit.SECONDS.toNanos(1)));
                client.ping();client.close();peer.success();
            }
        }
    }
    @Test void registeredFreshReaderAbsoluteDeadlineRejectsLateResultWithoutPretendingWorkerRetired()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var caller=Executors.newSingleThreadExecutor();
        try(var peer=peer((p,s,o)->{entered.countDown();await(release);return Optional.of(snapshot(s));});var client=new Client(peer.port(),4,255)) {
            try {
                client.login();client.select(7,1);client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,0));
                var reader=peer.skillsReader();var pending=caller.submit(()->reader.read(System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(300)));
                assertTrue(entered.await(2,TimeUnit.SECONDS));
                var cause=assertThrows(ExecutionException.class,()->pending.get(2,TimeUnit.SECONDS)).getCause();
                assertEquals(ModernBackendExecutor.Failure.DEADLINE,assertInstanceOf(ModernBackendExecutor.BackendException.class,cause).failure());
                assertEquals(1,peer.backend.activeCount());release.countDown();waitFor(()->peer.backend.activeCount()==0);
                client.ping();client.close();peer.success();
            }finally{release.countDown();}
        }finally{caller.shutdownNow();assertTrue(caller.awaitTermination(3,TimeUnit.SECONDS));}
    }
    @Test void legacyCombatRegistersNoSkillsClosureAndCannotEnableFeatureTwice()throws Exception {
        try(var peer=peer(ModernSkillsRepository.unavailable());var client=new Client(peer.port(),3,255)) {
            client.login();client.select(7,1);client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,0));
            assertNull(peer.skillsReader());assertThrows(IOException.class,()->peer.navigation().negotiateSkills(true));
            client.ping();client.close();peer.success();
        }
    }
    @Test void castRejectsWrongChannelBeforeWorldAndDispatchesCurrentScopeToRealService()throws Exception {
        try(var peer=peer((p,s,o)->Optional.of(snapshot(s)));var client=new Client(peer.port(),4,255)) {
            client.login();client.select(7,1);client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(),7,3333,1,0));
            assertThrows(IOException.class,()->peer.navigation().cast(new ModernSpellProtocol.Request(new ModernRooms.Scope(7,3333,1,1),1,1,1,5)));
            // A current selected lobby has no match. The real WorldService rejects
            // its unknown match instead of accepting client identity as authority.
            assertThrows(SecurityException.class,()->peer.navigation().cast(new ModernSpellProtocol.Request(new ModernRooms.Scope(7,3333,1,0),1,1,1,5)));
            client.ping();client.close();peer.success();
        }
    }
    @Test void productionServerLongestConstructorActuallyForwardsSkillRepository()throws Exception {
        var calls=new AtomicInteger();
        try(var services=new Services(nextDirectory())) {
            ModernServer server=null;
            try {
                server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),serverTls,
                        new ModernServer.Limits(2,2,60,2,1,1,2000,2000,2000,10000),services.fixture.verifier,services.fixture.characters,
                        services.presence,ModernCharacterDataRepository.unavailable(),services.equipment,services.store,services.rooms,services.worlds,4,
                        (p,s,o)->{principal(p,s);calls.incrementAndGet();return Optional.of(snapshot(s));});
                try(var client=new Client(server.address().getPort(),4,255)){
                    client.login();client.select(7,1);assertEquals(snapshot(7).skills(),client.skills(7,3333,1).reply().skills());client.ping();
                }
            }finally{if(server!=null){server.close();assertTrue(server.awaitTermination(5000));}}
            assertEquals(1,calls.get());assertEquals(0,services.presence.ownerCount());
        }
    }

    private static void assertRoleHeld(TournamentPresence presence,int source){
        var contender=presence.connect();try{assertEquals(TournamentPresence.SelectStatus.BUSY,presence.select(contender,
                new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),source==7?"太陽神":"月亮",row(source).name(),row(source).level(),row(source).characterClass(),row(source).sex())));}
        finally{presence.disconnect(contender);}
    }
    private static void await(CountDownLatch latch)throws IOException {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean interrupted=false;
        try{for(;;){long left=deadline-System.nanoTime();if(left<=0)throw new IOException("fixture barrier deadline");
            try{if(!latch.await(left,TimeUnit.NANOSECONDS))throw new IOException("fixture barrier deadline");return;}
            catch(InterruptedException failure){interrupted=true;}}}finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void waitFor(java.util.function.BooleanSupplier ready)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!ready.getAsBoolean()){assertTrue(System.nanoTime()<deadline);Thread.sleep(1);}
    }
    private static Object field(Object object,String name)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    private static final class Services implements AutoCloseable {
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms;final ModernWorldService worlds;final ModernEquipmentOverrideStore store;final ModernEquipmentAuthority equipment;
        Services(Path directory)throws Exception {
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()));
            store=new ModernEquipmentOverrideStore(directory);equipment=new ModernEquipmentAuthority((p,s,o)->{throw new IOException("unused equipment provider");},store);
            worlds=new ModernWorldService(rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,new ModernWorldMap.Cell(32724,32784),100000000L,100000000L)),
                    new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,10000)));
        }
        @Override public void close()throws Exception{worlds.close();assertTrue(worlds.awaitTermination(5000));store.close();assertTrue(store.isQuiescent());}
    }
    private static final class Peer implements AutoCloseable {
        final Services services;final ModernBackendExecutor backend=new ModernBackendExecutor(1,1);
        final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();final SSLServerSocket listener;
        final java.util.concurrent.Future<?> future;final AtomicReference<ModernTlsSessionTransport> transport=new AtomicReference<>();
        final AtomicReference<ModernApplicationSession> session=new AtomicReference<>();
        Peer(Services services,ModernSkillsRepository skills)throws Exception {
            this.services=services;listener=(SSLServerSocket)serverTls.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}));listener.setSoTimeout(3000);
            future=worker.submit(()->{try(var socket=(SSLSocket)listener.accept()){
                var peer=new ModernTlsSessionTransport(socket,ModernSessionBoundary.Policy.world(true,true),new ModernTlsSessionTransport.Limits(2000,2000),services.fixture.verifier);transport.set(peer);
                var app=new ModernApplicationSession(peer,services.fixture.characters,backend,10000,2000,services.presence,
                        ModernCharacterDataRepository.unavailable(),services.equipment,services.rooms,services.worlds,skills);session.set(app);app.run();return null;
            }});
        }
        int port(){return listener.getLocalPort();}
        ModernNavigationSession navigation()throws Exception{return (ModernNavigationSession)field(session.get(),"navigation");}
        ModernWorldService.SkillsReader skillsReader()throws Exception {
            Object owner=field(navigation(),"owner"),gate=field(services.worlds,"gate");
            synchronized(gate){var registrations=(Map<?,?>)field(services.worlds,"registrations");return (ModernWorldService.SkillsReader)field(registrations.get(owner),"skills");}
        }
        void success()throws Exception{future.get(5,TimeUnit.SECONDS);}
        Throwable failure()throws Exception{return assertThrows(ExecutionException.class,()->future.get(5,TimeUnit.SECONDS)).getCause();}
        @Override public void close()throws Exception {
            listener.close();if(transport.get()!=null)transport.get().failClosed();worker.shutdownNow();backend.close();
            assertTrue(worker.awaitTermination(5000,TimeUnit.MILLISECONDS));assertTrue(backend.awaitTermination(5000));services.close();
        }
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;final int version;int request=2;
        Client(int port,int version,int capabilities)throws Exception {
            this.version=version;socket=(SSLSocket)clientTls.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.setSoTimeout(5000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,capabilities,7,1)));socket.getOutputStream().flush();
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(version,ack.version());assertEquals(capabilities,ack.capabilities());
        }
        int next(){return request++;}
        void login()throws Exception{try(var auth=new ModernApplicationProtocol.Auth(next(),ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())){
            assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());}}
        void select(int source,long epoch)throws Exception{assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,nav(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch)).phase());}
        ModernApplicationProtocol.NavigationResult nav(ModernApplicationProtocol.Message request)throws Exception{return (ModernApplicationProtocol.NavigationResult)exchange(request);}
        ModernApplicationProtocol.SkillsResult skills(int source,long object,long epoch)throws Exception{
            var request=new ModernApplicationProtocol.SkillsRequest(next(),new ModernSpellProtocol.Context(source,object,epoch));
            var result=(ModernApplicationProtocol.SkillsResult)exchange(request);assertEquals(request.requestId(),result.requestId());assertEquals(request.context(),result.echo());return result;
        }
        void ping()throws Exception{int id=next();assertEquals(new ModernApplicationProtocol.Pong(id),exchange(new ModernApplicationProtocol.Ping(id)));}
        void send(ModernApplicationProtocol.Message request,int version)throws Exception{byte[] bytes=ModernApplicationProtocol.encode(request,version);
            try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);}}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request)throws Exception{send(request,version);byte[] bytes=ModernApplicationProtocol.readFrame(socket.getInputStream());
            try{return ModernApplicationProtocol.decode(bytes,version);}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}}
        @Override public void close()throws IOException{socket.close();}
    }
}
