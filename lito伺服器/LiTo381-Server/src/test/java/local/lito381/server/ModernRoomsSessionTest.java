package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real TLS tests over the production server, including malformed navigation and disconnect cleanup. */
class ModernRoomsSessionTest {
    @TempDir Path directory;
    private static SSLContext serverContext,clientContext;
    @BeforeAll static void certificates() throws Exception {
        Path fixture=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(fixture);
        serverContext=ModernTlsTestMaterial.serverContext(fixture,false);clientContext=ModernTlsTestMaterial.clientContext(fixture);
    }

    @Test void twoSourcesWithSameObjectJoinReadyConfigureTransferOwnershipAndReturn() throws Exception {
        try(var harness=new Harness(null);var first=harness.client(7,47);var second=harness.client(42,47)) {
            first.enter(0);second.enter(0);
            var created=first.command(Action.CREATE,0,0,0,new Draft("測試房","password",4,1,564,48),"");
            assertEquals(Status.OK,created.status());assertEquals(1,created.view().joined().summary().users());
            assertEquals(new TournamentPresence.CharacterKey(7,3333),created.view().joined().owner());
            assertEquals("太陽神",created.view().joined().members().get(0).sourceName());
            var room=created.view().joined().summary();
            var denied=second.command(Action.JOIN,room.id(),room.revision(),0,null,"wrong");
            assertEquals(Reason.BAD_PASSWORD,denied.reason());assertNull(denied.view().joined());
            assertEquals(created.view().listRevision(),denied.view().listRevision());
            var joined=second.command(Action.JOIN,room.id(),room.revision(),0,null,"password");
            assertEquals(Status.OK,joined.status());assertEquals(2,joined.view().joined().members().size());
            assertEquals(List.of("太陽神","月亮"),joined.view().joined().members().stream().map(Member::sourceName).toList());
            var stale=first.command(Action.READY,room.id(),room.revision(),1,null,"");
            assertEquals(Status.CONFLICT,stale.status());assertEquals(0,stale.view().joined().summary().ready());
            var firstReady=first.inRoom(Action.READY,1,null);assertEquals(1,firstReady.view().joined().summary().ready());
            var allReady=second.inRoom(Action.READY,1,null);assertTrue(allReady.view().joined().summary().readySatisfied());
            assertTrue(allReady.view().options().stream().noneMatch(Option::worldAvailable));
            var noOp=second.inRoom(Action.READY,1,null);assertEquals(allReady.view().listRevision(),noOp.view().listRevision());
            assertEquals(Reason.NOT_OWNER,second.inRoom(Action.CONFIGURE,0,new Draft("not owner","",4,1,564,16)).reason());
            var configured=first.inRoom(Action.CONFIGURE,0,new Draft("new","",4,4,540,16));
            assertEquals(0,configured.view().joined().summary().ready());assertEquals(4,configured.view().joined().summary().config().mode());
            first.close();await(()->harness.presence.ownerCount()==1);
            var transferred=second.read();assertEquals(new TournamentPresence.CharacterKey(42,3333),transferred.view().joined().owner());
            assertEquals(1,transferred.view().joined().summary().users());
            assertEquals(Status.OK,second.inRoom(Action.LEAVE,0,null).status());assertTrue(second.read().view().rooms().isEmpty());
            for(int channel=0;channel<4;channel++) {
                assertEquals(1,((ModernApplicationProtocol.NavigationResult)second.exchange(new ModernApplicationProtocol.LobbyLeave(second.next++,42,3333,1))).phase());
                second.enter(channel);assertNull(second.read().view().joined());
            }
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)second.exchange(new ModernApplicationProtocol.CharacterReturn(second.next++,42,3333,1))).phase());
        }
    }

    @Test void roomMembershipCannotBeBypassedWithLobbyLeaveBackOrChannelEnter() throws Exception {
        for(int action:List.of(11,12,13))try(var harness=new Harness(null);var client=harness.client(7,47)) {
            client.enter(0);client.command(Action.CREATE,0,0,0,new Draft("room","",8,1,564,16),"");
            ModernApplicationProtocol.Message request=switch(action) {
                case 11->new ModernApplicationProtocol.ChannelEnter(client.next++,7,3333,1,1);
                case 12->new ModernApplicationProtocol.CharacterReturn(client.next++,7,3333,1);
                default->new ModernApplicationProtocol.LobbyLeave(client.next++,7,3333,1);
            };
            client.expectClosed(request);await(()->harness.server.snapshot().sessions()==0);
            assertEquals(1,harness.server.snapshot().failed());assertEquals(0,harness.rooms.roomCount());
        }
    }

    @Test void capabilitySelectionAndExactScopeAreRequiredBeforeAnyRoomAction() throws Exception {
        for(int scenario=0;scenario<6;scenario++)try(var harness=new Harness(null);var client=harness.client(7,scenario==0?15:47)) {
            if(scenario!=1)client.enter(0);
            var scope=new Scope(scenario==2?42:7,scenario==3?3334:3333,scenario==4?2:1,scenario==5?1:0);
            client.expectClosed(new ModernApplicationProtocol.RoomRequest(client.next++,new Command(scope,Action.READ,0,0,0,null,"")));
            await(()->harness.server.snapshot().sessions()==0);assertEquals(1,harness.server.snapshot().failed());
            assertEquals(0,harness.rooms.ownerCount());assertEquals(0,harness.presence.userCount());
        }
    }

    @Test void peerWithoutOptionalRoomCapabilityKeepsLegacyWideClassReadOnlyNavigation() throws Exception {
        try(var harness=new Harness(null,true);var client=harness.client(7,15)) {
            for(int channel=0;channel<4;channel++) {
                client.enter(channel);assertEquals(1,harness.presence.userCount());assertEquals(0,harness.rooms.ownerCount());
                var back=(ModernApplicationProtocol.NavigationResult)client.exchange(new ModernApplicationProtocol.LobbyLeave(client.next++,7,3333,1));
                assertEquals(1,back.phase());assertEquals(65535,back.selected().characterClass());
            }
            assertEquals(0,harness.server.snapshot().failed());
        }
    }

    @Test void equipmentReadKeepsReadyButActualSetClearsReadyBeforePublishingNewLoadout() throws Exception {
        try(var harness=new Harness(Files.createDirectory(directory.resolve("gear")));var client=harness.client(7,63)) {
            client.enter(0);client.command(Action.CREATE,0,0,0,new Draft("room","",8,4,540,16),"");
            assertTrue(client.inRoom(Action.READY,1,null).view().joined().summary().readySatisfied());
            var read=new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO);
            var equipment=(ModernApplicationProtocol.EquipmentResult)client.exchange(new ModernApplicationProtocol.EquipmentRequest(client.next++,read));
            assertEquals(ModernEquipmentAuthority.OK,equipment.result().status());
            assertTrue(client.read().view().joined().summary().readySatisfied());
            var before=equipment.result().snapshot();
            var set=new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.SET,true,5_000_000_002L,before.revision(),before.viewToken());
            var changed=(ModernApplicationProtocol.EquipmentResult)client.exchange(new ModernApplicationProtocol.EquipmentRequest(client.next++,set));
            assertEquals(ModernEquipmentAuthority.OK,changed.result().status());assertTrue(changed.result().changed());
            assertEquals(1,changed.result().snapshot().revision());assertEquals(0,client.read().view().joined().summary().ready());
            assertFalse(client.read().view().joined().summary().readySatisfied());
        }
    }

    @Test void createdRoomWithoutConsumedAckIsRemovedOnDisconnectAndReconnectDoesNotReuseItsId() throws Exception {
        try(var harness=new Harness(null)) {
            try(var client=harness.client(7,47)) {
                client.enter(0);
                client.send(new ModernApplicationProtocol.RoomRequest(client.next++,new Command(new Scope(7,3333,1,0),
                        Action.CREATE,0,0,0,new Draft("unconfirmed","",8,1,564,16),"")));
                // The production authority really committed membership, but this client never reads its ACK.
                await(()->harness.rooms.roomCount()==1 && harness.rooms.memberCount()==1);
            }
            await(()->harness.server.snapshot().sessions()==0 && harness.rooms.roomCount()==0 && harness.presence.ownerCount()==0);
            try(var reconnected=harness.client(7,47)) {
                reconnected.enter(0);assertTrue(reconnected.read().view().rooms().isEmpty());
                var created=reconnected.command(Action.CREATE,0,0,0,new Draft("confirmed","",8,1,564,16),"");
                assertEquals(Status.OK,created.status());assertEquals(2,created.view().joined().summary().id());
                assertEquals(1,created.view().joined().summary().users());
            }
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!condition.getAsBoolean()){assertTrue(System.nanoTime()<deadline,"owned operation did not retire");Thread.sleep(5);}
    }
    private static final class Harness implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms=new ModernRooms();
        final ModernApplicationTestBackend backend=new ModernApplicationTestBackend("auth");final ModernServer server;
        Harness(Path storage) throws Exception {
            this(storage,false);
        }
        Harness(Path storage,boolean legacyWideClass) throws Exception {
            ModernEquipmentOverrideStore store=storage==null?null:new ModernEquipmentOverrideStore(storage);
            ModernEquipmentAuthority authority=store==null?null:new ModernEquipmentAuthority((principal,source,object)->{
                if(!principal.authorizesSource(source)||object!=3333)throw new SecurityException("fixture scope required");
                byte[] binding=new byte[32];Arrays.fill(binding,(byte)source);
                return new ModernEquipmentAuthority.Loaded(ModernEquipmentTestPeer.qualified(source),new ModernEquipmentOverrideStore.Fingerprint(binding));
            },store,()->1000L);
            ModernCharacterRepository characters=legacyWideClass?(principal,source)->List.of(
                    new ModernApplicationProtocol.CharacterRow(3333,"Legacy unsupported class",65535,0,80)):backend.characters;
            server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),serverContext,
                    new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,5000),backend.verifier,characters,presence,
                    (principal,source,object)->Optional.of(ModernEquipmentTestPeer.qualified(source).source()),authority,store,rooms);
        }
        Client client(int source,int capabilities) throws Exception{return new Client(this,source,capabilities);}
        @Override public void close() throws Exception {
            server.close();assertTrue(server.awaitTermination(3000));assertEquals(0,presence.ownerCount());
            assertEquals(0,presence.userCount());assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.roomCount());assertEquals(0,rooms.memberCount());
        }
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;final int source;int channel=255,next=2;
        Client(Harness harness,int source,int capabilities) throws Exception {
            this.source=source;socket=(SSLSocket)clientContext.getSocketFactory().createSocket();socket.setSoTimeout(3000);
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
            socket.connect(harness.server.address(),1000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,1,capabilities,7,1)));
            socket.getOutputStream().flush();assertEquals(new ModernSessionProtocol.Ack(1,1,1,capabilities),ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
            var auth=(ModernApplicationProtocol.AuthResult)exchange(new ModernApplicationProtocol.Auth(next++,ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray()));assertEquals(0,auth.status());
            var roster=(ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next++,source,1));assertEquals(0,roster.status());
            var selected=(ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next++,source,3333,1));assertEquals(0,selected.status());
        }
        void enter(int channel) throws Exception {
            var response=(ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next++,source,3333,1,channel));
            assertEquals(0,response.status());assertEquals(2,response.phase());this.channel=channel;
        }
        Result read() throws Exception{return command(Action.READ,0,0,0,null,"");}
        Result inRoom(Action action,int value,Draft draft) throws Exception {
            var current=read().view().joined().summary();return command(action,current.id(),current.revision(),value,draft,"");
        }
        Result command(Action action,long room,long revision,int value,Draft draft,String password) throws Exception {
            var command=new Command(new Scope(source,3333,1,channel),action,room,revision,value,draft,password);
            var response=(ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next++,command));
            assertEquals(ModernRoomProtocol.Echo.of(command),response.echo());return response.result();
        }
        void send(ModernApplicationProtocol.Message request) throws Exception {
            byte[] encoded=ModernApplicationProtocol.encode(request);
            try{socket.getOutputStream().write(encoded);socket.getOutputStream().flush();}
            finally{Arrays.fill(encoded,(byte)0);request.close();}
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            send(request);byte[] frame=ModernApplicationProtocol.readFrame(socket.getInputStream());
            assertNotNull(frame);var response=ModernApplicationProtocol.decode(frame);assertEquals(request.requestId(),response.requestId());return response;
        }
        void expectClosed(ModernApplicationProtocol.Message request) throws Exception {
            send(request);
            try{assertNull(ModernApplicationProtocol.readFrame(socket.getInputStream()));}
            catch(java.io.EOFException|java.net.SocketException closed) { /* Actual close, never accept a read timeout. */ }
        }
        @Override public void close() throws Exception{socket.close();}
    }
}
