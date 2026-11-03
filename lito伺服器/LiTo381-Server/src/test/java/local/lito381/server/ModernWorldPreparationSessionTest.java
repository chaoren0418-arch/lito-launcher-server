package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real authenticated TLS application, with a test-only trusted server preparation trigger. */
class ModernWorldPreparationSessionTest {
    private static SSLContext serverContext,clientContext;
    private static final Scope SCOPE=new Scope(7,3333,1,0);
    @TempDir Path directory;
    @BeforeAll static void certificates() throws Exception {
        Path fixture=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixture);
        serverContext=ModernTlsTestMaterial.serverContext(fixture,false);clientContext=ModernTlsTestMaterial.clientContext(fixture);
    }

    @Test void actualPreparationRejectsGearSetBeforeProviderAndReleaseRestoresAuthority() throws Exception {
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.login(63);client.select();client.enter();var room=client.readyRoom();
            var before=client.gear(new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO));
            assertEquals(ModernEquipmentAuthority.OK,before.status());assertEquals(1,peer.providerCalls.get());
            var prepared=peer.navigation.get().prepareWorld(SCOPE,room.id(),room.revision());
            assertEquals(Status.OK,prepared.result().status());assertTrue(prepared.ticket().isLive());
            var snapshot=before.snapshot();
            var set=new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.SET,true,
                    5_000_000_002L,snapshot.revision(),snapshot.viewToken());
            var busy=client.gear(set);
            assertEquals(ModernEquipmentAuthority.BUSY,busy.status());assertEquals(0,busy.reason());
            assertFalse(busy.changed());assertNull(busy.snapshot());assertEquals(1,peer.providerCalls.get());
            assertEquals(prepared.result().view(),client.room(Action.READ,0,0,0,null).view());
            var allowedRead=client.gear(new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO));
            assertEquals(ModernEquipmentAuthority.OK,allowedRead.status());assertEquals(snapshot.revision(),allowedRead.snapshot().revision());
            assertTrue(peer.rooms.releasePreparation(prepared.ticket()));assertFalse(prepared.ticket().isLive());
            var changed=client.gear(set);assertEquals(ModernEquipmentAuthority.OK,changed.status());assertTrue(changed.changed());
            assertEquals(1,changed.snapshot().revision());assertEquals(0,client.room(Action.READ,0,0,0,null).view().joined().summary().ready());
            client.close();peer.success();assertEquals(0,peer.rooms.roomCount());assertEquals(0,peer.presence.ownerCount());
        }
    }

    @Test void trustedPreparationStillRequiresAuthenticationSelectionCapabilityAndExactScope() throws Exception {
        for(int capabilities:new int[]{15,63})try(var peer=new Peer(directory.resolve("cap"+capabilities));var client=new Client(peer)) {
            assertTrue(peer.created.await(2,TimeUnit.SECONDS));
            var navigation=peer.navigation.get();
            assertThrows(IOException.class,()->navigation.prepareWorld(SCOPE,1,1));
            client.login(capabilities);assertThrows(IOException.class,()->navigation.prepareWorld(SCOPE,1,1));
            client.select();assertThrows(IOException.class,()->navigation.prepareWorld(SCOPE,1,1));
            client.enter();
            if(capabilities==15)assertThrows(IOException.class,()->navigation.prepareWorld(SCOPE,1,1));
            else {
                var room=client.readyRoom();
                for(var wrong:new Scope[]{new Scope(42,3333,1,0),new Scope(7,3334,1,0),new Scope(7,3333,2,0),new Scope(7,3333,1,1)})
                    assertThrows(IOException.class,()->navigation.prepareWorld(wrong,room.id(),room.revision()));
                assertEquals(Reason.REVISION_MISMATCH,navigation.prepareWorld(SCOPE,room.id(),room.revision()-1).result().reason());
                assertEquals(room,client.room(Action.READ,0,0,0,null).view().joined().summary());
                var prepared=navigation.prepareWorld(SCOPE,room.id(),room.revision());assertNotNull(prepared.ticket());
                peer.rooms.releasePreparation(prepared.ticket());
            }
            client.close();peer.success();
        }
    }

    @Test void actualTlsEofCancelsFrozenCapabilityAndRemovesRoomWithoutAClientAck() throws Exception {
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.login(63);client.select();client.enter();var room=client.readyRoom();
            var prepared=peer.navigation.get().prepareWorld(SCOPE,room.id(),room.revision());
            assertTrue(prepared.ticket().isLive());client.close();peer.success();
            assertFalse(prepared.ticket().isLive());assertThrows(IllegalStateException.class,prepared.ticket()::guard);
            assertFalse(peer.rooms.releasePreparation(prepared.ticket()));
            assertEquals(0,peer.rooms.roomCount());assertEquals(0,peer.rooms.ownerCount());assertEquals(0,peer.presence.ownerCount());
        }
    }

    @Test void authenticatedCaptureAndFreshQualifiedRowDriveRealSharedGeometryBeforeDisconnect() throws Exception {
        String sharedRoot=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sharedRoot!=null && !sharedRoot.isBlank(),"explicit shared resource root required");
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.login(63);client.select();client.enter();var room=client.readyRoom();
            var ticket=peer.navigation.get().prepareWorld(SCOPE,room.id(),room.revision()).ticket();
            try {
                // Actual shared bytes and a fresh authenticated source read; no IO under the room/ticket monitor.
                var map=ModernWorldMap.load(Path.of(sharedRoot),514,Set.of(new ModernWorldMap.SectorKey(0x7fff,0x8000)),()->false);
                int priorReads=peer.fixture.characterCalls.get();
                var fresh=peer.navigation.get().equipment(7,3333,1,(principal,selected,operation)->
                        peer.fixture.characters.characters(principal,7).stream().filter(row->row.objectId()==3333).findFirst().orElseThrow());
                assertEquals(priorReads+1,peer.fixture.characterCalls.get());
                var captured=ticket.participants().get(0);var spawn=new ModernWorldMap.Cell(32724,32784);
                var prepared=new ModernBattleEngine.PreparedMember(captured.owner(),captured.scope(),fresh,spawn,200_000_000L);
                var now=new AtomicLong();
                // Logical solo-mode 540 -> physical shared 514 is deliberate, not a same-number fallback.
                try(var engine=new ModernBattleEngine(ticket,new ModernWorldBinding(4,540,map),List.of(prepared),
                        new ModernBattleEngine.Limits(10000,3_000_000_000L,60000),now::get)) {
                    assertEquals(ModernBattleEngine.Phase.LOADING,engine.tick().phase());
                    assertEquals(ModernBattleEngine.Phase.COUNTDOWN,engine.ackLoaded(captured.owner(),SCOPE,ticket.matchId(),ticket.generation(),map.digest()).phase());
                    now.set(2_999_999_999L);assertEquals(ModernBattleEngine.Phase.COUNTDOWN,engine.tick().phase());
                    now.incrementAndGet();assertEquals(ModernBattleEngine.Phase.ACTIVE,engine.tick().phase());
                    var moved=engine.move(captured.owner(),SCOPE,ticket.matchId(),ticket.generation(),1,spawn.x(),spawn.y(),2);
                    assertEquals(ModernBattleEngine.MoveStatus.MOVED,moved.status());
                    assertEquals(new ModernWorldMap.Cell(32725,32784),moved.snapshot().actors().get(0).position());
                    assertEquals("太陽神",moved.snapshot().actors().get(0).member().sourceName());
                    assertTrue(client.room(Action.READ,0,0,0,null).view().options().stream().noneMatch(Option::worldAvailable));
                    client.close();peer.success();assertFalse(ticket.isLive());
                    assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,engine.tick().abortReason());
                    assertEquals(ModernBattleEngine.MoveStatus.ABORTED,engine.move(captured.owner(),SCOPE,ticket.matchId(),ticket.generation(),2,32725,32784,2).status());
                }
            } finally {peer.rooms.releasePreparation(ticket);}
        }
    }

    private static final class Peer implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms=new ModernRooms();
        final ModernBackendExecutor backend=new ModernBackendExecutor(2,2);
        final AtomicReference<ModernTlsSessionTransport> transport=new AtomicReference<>();
        final AtomicReference<ModernNavigationSession> navigation=new AtomicReference<>();
        final AtomicInteger providerCalls=new AtomicInteger();final CountDownLatch created=new CountDownLatch(1);
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");
        final ModernEquipmentOverrideStore store;final SSLServerSocket listener;
        final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();final java.util.concurrent.Future<?> future;
        Peer(Path storage) throws Exception {
            Files.createDirectories(storage);store=new ModernEquipmentOverrideStore(storage);
            var equipment=new ModernEquipmentAuthority((principal,source,object)->{
                if(!principal.authorizesSource(source)||object!=3333)throw new SecurityException("fixture scope required");
                providerCalls.incrementAndGet();byte[] binding=new byte[32];Arrays.fill(binding,(byte)source);
                return new ModernEquipmentAuthority.Loaded(ModernEquipmentTestPeer.qualified(source),new ModernEquipmentOverrideStore.Fingerprint(binding));
            },store,()->1000L);
            listener=(SSLServerSocket)serverContext.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));listener.setSoTimeout(3000);
            future=worker.submit(()->{
                try(var socket=(SSLSocket)listener.accept()) {
                    var connection=new ModernTlsSessionTransport(socket,ModernSessionBoundary.Policy.rooms(true),
                            new ModernTlsSessionTransport.Limits(2000,2000),fixture.verifier);transport.set(connection);
                    var application=new ModernApplicationSession(connection,fixture.characters,backend,10000,3000,presence,
                            (principal,source,object)->Optional.of(ModernEquipmentTestPeer.qualified(source).source()),equipment,rooms);
                    // Capture an actual production session only in test; never manufacture its principal, owner or lease.
                    Field field=ModernApplicationSession.class.getDeclaredField("navigation");field.setAccessible(true);
                    navigation.set((ModernNavigationSession)field.get(application));created.countDown();application.run();
                }
                return null;
            });
        }
        void success() throws Exception{future.get(4,TimeUnit.SECONDS);}
        @Override public void close() throws Exception {
            listener.close();if(transport.get()!=null)transport.get().failClosed();worker.shutdown();backend.close();
            assertTrue(worker.awaitTermination(4,TimeUnit.SECONDS));assertTrue(backend.awaitTermination(4000));store.close();
            assertEquals(0,presence.ownerCount());assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.roomCount());
        }
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;int next=2;
        Client(Peer peer) throws Exception {
            socket=(SSLSocket)clientContext.getSocketFactory().createSocket("localhost",peer.listener.getLocalPort());socket.setSoTimeout(3000);
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
        }
        void login(int caps) throws Exception {
            socket.startHandshake();socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,1,caps,7,1)));socket.getOutputStream().flush();
            assertEquals(new ModernSessionProtocol.Ack(1,1,1,caps),ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
            var auth=(ModernApplicationProtocol.AuthResult)exchange(new ModernApplicationProtocol.Auth(next++,ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray()));assertEquals(0,auth.status());
        }
        void select() throws Exception {
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next++,7,1))).status());
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next++,7,3333,1))).status());
        }
        void enter() throws Exception{assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next++,7,3333,1,0))).status());}
        Summary readyRoom() throws Exception {
            var created=room(Action.CREATE,0,0,0,new Draft("load world","",8,4,540,16)).view().joined().summary();
            var ready=room(Action.READY,created.id(),created.revision(),1,null).view().joined().summary();assertTrue(ready.readySatisfied());return ready;
        }
        Result room(Action action,long id,long revision,int value,Draft draft) throws Exception {
            return ((ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next++,new Command(SCOPE,action,id,revision,value,draft,"")))).result();
        }
        ModernEquipmentAuthority.Result gear(ModernEquipmentAuthority.Request request) throws Exception {
            return ((ModernApplicationProtocol.EquipmentResult)exchange(new ModernApplicationProtocol.EquipmentRequest(next++,request))).result();
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            byte[] frame=ModernApplicationProtocol.encode(request);
            try{socket.getOutputStream().write(frame);socket.getOutputStream().flush();}finally{Arrays.fill(frame,(byte)0);request.close();}
            byte[] response=ModernApplicationProtocol.readFrame(socket.getInputStream());assertNotNull(response);
            var decoded=ModernApplicationProtocol.decode(response);assertEquals(request.requestId(),decoded.requestId());return decoded;
        }
        @Override public void close() throws Exception{socket.close();}
    }
}
