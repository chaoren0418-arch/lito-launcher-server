package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real TLS -> AUTH -> Navigation -> room/GM -> WorldService -> healing.
 * Shared map bytes; explicit synthetic source rows, not SQL or native UI proof.
 * Client command facts come only from received frames, never server reflection.
 * The small test probe reads required fields, not a replacement scene decoder. */
class ModernPlayerHealingTlsTest {
    @TempDir Path root;
    static SSLContext serverContext,clientContext;
    @BeforeAll static void certificates()throws Exception {
        LocalResources.assumeSharedResourceRoot();
        var directory=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(directory);
        serverContext=ModernTlsTestMaterial.serverContext(directory,false);clientContext=ModernTlsTestMaterial.clientContext(directory);
    }
    @Test void authenticatedSelectedSessionUsesSharedMapAndReturnsHealingThroughRealTransport()throws Exception {
        try(var f=new Fixture(root);var c=new Client(f.server,3)) {
            c.login();var room=c.room();
            var gm=(ModernApplicationProtocol.GmResult)c.old(new ModernApplicationProtocol.GmRequest(c.next(),
                    new ModernGmCommandProtocol.Request(c.scope,room.summary().id(),room.summary().revision(),20)));
            assertEquals(ModernGmCommandProtocol.Delivery.RESULT,gm.reply().delivery());
            assertEquals(1,gm.reply().result().added(),"capacity two clamps twenty requested bots");
            room=gm.reply().result().result().view().joined();
            var bot=room.members().stream().filter(m->!m.identity().isHuman()).findFirst().orElseThrow();
            assertTrue(bot.ready());assertEquals("太陽神",bot.sourceName());
            var ready=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),
                    new ModernRooms.Command(c.scope,ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null,"")));
            assertEquals(ModernRooms.Status.OK,ready.result().status());room=ready.result().view().joined();assertTrue(room.summary().readySatisfied());
            var prepared=c.world(ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
            assertEquals(0,prepared[112]);var world=new ModernTargetHistory.World(ByteBuffer.wrap(prepared).getLong(116),ByteBuffer.wrap(prepared).getLong(124),ByteBuffer.wrap(prepared).getLong(132));
            byte[] loading=null;long deadline=System.nanoTime()+4_000_000_000L;
            while(System.nanoTime()<deadline){loading=c.world(ModernBattleWorldProtocol.Action.READ,0,0,world,ModernWorldProtocol.ZERO_DIGEST);if(loading.length>168)break;Thread.sleep(10);}
            assertNotNull(loading);assertTrue(loading.length>168,"world preparation produced complete data");
            var initial=Probe.from(loading,168);assertEquals(world,initial.world);
            var loaded=c.world(ModernBattleWorldProtocol.Action.LOADED,0,0,world,f.map.digest());
            var active=Probe.from(loaded,168);assertEquals(ModernBattleEngine.Phase.ACTIVE.ordinal(),active.phase);
            var command=active.self(c.next(),c.scope,1);var frame=c.exchange(new ModernApplicationProtocol.PlayerHealingRequest(command),79);
            assertEquals(ModernPlayerHealingCast.Reason.NONE.ordinal(),Byte.toUnsignedInt(frame[160]));
            var healed=Probe.from(frame,192);assertEquals(active.mp-4,healed.mp);assertEquals(active.life,healed.life);assertTrue(healed.revision>active.revision);
            var read=c.exchange(new ModernApplicationProtocol.BattleReadRequest(c.next(),new ModernBattleReadProtocol.Request(c.scope,world)),61);
            var observed=Probe.from(read,ModernBattleReadProtocol.MIN_RESULT_BYTES);assertEquals(healed.mp,observed.mp);assertTrue(observed.revision>=healed.revision);
            var leave=c.world(ModernBattleWorldProtocol.Action.LEAVE,0,0,world,ModernWorldProtocol.ZERO_DIGEST);assertEquals(168,leave.length);assertEquals(0,leave[112]);
            int ping=c.next();assertEquals(new ModernApplicationProtocol.Pong(ping),c.old(new ModernApplicationProtocol.Ping(ping)));
            c.close();ModernNpcServiceTest.await(()->f.server.snapshot().sessions()==0);assertEquals(0,f.server.snapshot().failed());
        }
    }
    @Test void negotiatedCommandsStillRequireAuthenticationAndLegacyHandshakeKeepsZeroExtensions()throws Exception {
        try(var f=new Fixture(root);var c=new Client(f.server,3)) {
            var world=new ModernTargetHistory.World(1,1,1);var proof=new ModernBattleCommandLife.Proof(c.scope,world,1,1,1);
            var q=new ModernPlayerHealingProtocol.Request(c.next(),proof,1,0,32724,32784,
                    new ModernPlayerHealingCast.Target(new ModernTargetHistory.Participant(1,ModernParticipantId.human(7,3333)),1,0,ModernWorldTestPeer.ANCHOR));
            c.send(ModernApplicationProtocol.encode(new ModernApplicationProtocol.PlayerHealingRequest(q),c.peer));
            assertEquals(-1,c.socket.getInputStream().read());ModernNpcServiceTest.await(()->f.server.snapshot().sessions()==0);
            assertEquals(0,f.backend.authenticationCalls.get());assertEquals(0,f.worlds.registrationCount());assertEquals(0,f.worlds.matchCount());
            try(var legacy=new Client(f.server,0)){legacy.login();assertEquals(0,legacy.peer.extensions());int ping=legacy.next();assertEquals(new ModernApplicationProtocol.Pong(ping),legacy.old(new ModernApplicationProtocol.Ping(ping)));}
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernApplicationTestBackend backend=new ModernApplicationTestBackend("auth",80);
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms;
        final ModernEquipmentOverrideStore store;final ModernWorldMap map;final ModernWorldService worlds;final ModernServer server;
        Fixture(Path root)throws Exception {
            this(root,4000);
        }
        Fixture(Path root,int loadTimeoutMillis)throws Exception {
            this(root,loadTimeoutMillis,b->b-1);
        }
        Fixture(Path root,int loadTimeoutMillis,ModernPhysicalCombat.BoundedRandom random)throws Exception {
            map=ModernWorldTestPeer.sharedMap(LocalResources.sharedResourceRoot());
            var binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()),Set.of(binding.key()));
            store=new ModernEquipmentOverrideStore(root);
            ModernEquipmentAuthority.PhysicalProvider physical=new ModernEquipmentAuthority.PhysicalProvider(){
                public boolean playerHealingSource(int source){return source==7||source==42;}
                public ModernEquipmentAuthority.PhysicalLoaded read(ModernAccountAuthentication.Principal p,int source,long id)throws Exception {
                    if(!p.authorizesSource(source)||!playerHealingSource(source)||id!=3333)throw new SecurityException();
                    var row=backend.characters.characters(p,source).stream().filter(r->r.objectId()==id).findFirst().orElseThrow();
                    var data=ModernBattleEngineHonorTest.source(source,row,1000,300);byte[] token=new byte[32];token[0]=(byte)source;
                    return new ModernEquipmentAuthority.PhysicalLoaded(data,new ModernEquipmentOverrideStore.Fingerprint(token));
                }
            };
            var equipment=new ModernEquipmentAuthority((p,s,id)->{var v=physical.read(p,s,id);return new ModernEquipmentAuthority.Loaded(v.source().input().equipment(),v.ownerBinding());},null,physical,store);
            ModernSkillsRepository learned=(p,s,id)->Optional.of(new ModernSkills.Snapshot(s,physical.read(p,s,id).source().input().equipment().source().character(),List.of(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL))));
            ModernShopCatalogRepository shop=(p,s,id)->Optional.of(new ModernShopCatalog.Snapshot(s,p.accountForSource(s),physical.read(p,s,id).source().input().equipment().source().character(),List.of()));
            ModernGmCommandRepository gm=(p,s,id)->Optional.of(new ModernGmCommands.Snapshot(s,p.accountForSource(s),physical.read(p,s,id).source().input().equipment().source().character(),200,false,false,new ModernGmCommands.Definition("de",200,"L1SpawnDe",false)));
            var mapping=new ModernWorldService.Mapping(binding,ModernWorldTestPeer.ANCHOR,1_000_000,1_000_000,2_000_000_000L,false,true,true,true,true,false,true,null,false).withPlayerHealing(true);
            worlds=new ModernWorldService(rooms,Map.of(binding.key(),mapping),new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(loadTimeoutMillis,0,30000)),random,null,Map.of(7,100.0,42,100.0));
            server=new ModernServer(new InetSocketAddress("127.0.0.1",0),serverContext,new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),backend.verifier,backend.characters,presence,
                    (p,s,id)->Optional.of(physical.read(p,s,id).source().input().equipment().source()),equipment,store,rooms,worlds,47,learned,ModernCharacterRecordRepository.unavailable(),null,gm,shop,20000);
        }
        public void close()throws Exception {
            server.close();assertTrue(server.awaitTermination(5000));worlds.close();assertTrue(worlds.awaitTermination(5000));
            assertEquals(0,worlds.registrationCount());assertEquals(0,worlds.matchCount());assertEquals(0,presence.ownerCount());assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.roomCount());assertTrue(store.isQuiescent());store.close();
        }
    }
    static final class Client implements AutoCloseable {
        final SSLSocket socket;final ModernSessionBoundary.Negotiated peer;int id=1;
        final ModernRooms.Scope scope;
        Client(ModernServer server,int extensions)throws Exception {
            this(server,extensions,7);
        }
        Client(ModernServer server,int extensions,int source)throws Exception {
            scope=new ModernRooms.Scope(source,3333,1,0);
            var raw=new Socket();int port=server.address().getPort();raw.connect(new InetSocketAddress("127.0.0.1",port),2000);
            socket=(SSLSocket)clientContext.getSocketFactory().createSocket(raw,"localhost",port,true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(5000);socket.startHandshake();
            var hello=new ModernSessionProtocol.Hello(1,47,47,-1,ModernSessionExtensions.DEPENDENCIES,1);
            send(ModernSessionProtocol.encode(extensions==0?hello:new ModernSessionProtocol.ExtendedHello(hello,new ModernSessionExtensions.Offer(extensions,extensions))));
            var response=ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));
            ModernSessionProtocol.Ack ack;
            if(extensions==0)ack=assertInstanceOf(ModernSessionProtocol.Ack.class,response);
            else {var selected=assertInstanceOf(ModernSessionProtocol.ExtendedAck.class,response);assertEquals(extensions,selected.extensions());ack=selected.base();}
            peer=new ModernSessionBoundary.Negotiated(ack.requestId(),ack.version(),ack.numericSchema(),ack.capabilities(),extensions);
        }
        int next(){return ++id;}
        void send(byte[] bytes)throws Exception {try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);}}
        byte[] receive(int request,int kind)throws Exception {
            var head=socket.getInputStream().readNBytes(16);assertEquals(16,head.length);var b=ByteBuffer.wrap(head);assertEquals(0x4c544d31,b.getInt());int size=b.getInt();
            assertTrue(size>=16&&size<=ModernBattleWire.Encoding.selected(peer).maximum()+1024);assertEquals(request,b.getInt());if(kind>=0)assertEquals(kind,Short.toUnsignedInt(b.getShort()));else b.getShort();assertEquals(0,b.getShort());
            var body=socket.getInputStream().readNBytes(size-16);assertEquals(size-16,body.length);return ByteBuffer.allocate(size).put(head).put(body).array();
        }
        byte[] exchange(ModernApplicationProtocol.Message m,int kind)throws Exception {send(ModernApplicationProtocol.encode(m,peer));return receive(m.requestId(),kind);}
        ModernApplicationProtocol.Message old(ModernApplicationProtocol.Message m)throws Exception {return ModernApplicationProtocol.decode(exchange(m,-1),47);}
        void login()throws Exception {
            login(ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD);
        }
        void login(String account,String password)throws Exception {
            try(var auth=new ModernApplicationProtocol.Auth(next(),account,password.toCharArray())){assertEquals(0,((ModernApplicationProtocol.AuthResult)old(auth)).status());}
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)old(new ModernApplicationProtocol.CharacterRequest(next(),scope.sourceId(),scope.epoch()))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)old(new ModernApplicationProtocol.CharacterSelection(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());
            var entered=(ModernApplicationProtocol.NavigationResult)old(new ModernApplicationProtocol.ChannelEnter(next(),scope.sourceId(),scope.objectId(),scope.epoch(),scope.channelId()));assertEquals(0,entered.status());assertEquals(2,entered.phase());
        }
        ModernRooms.RoomSnapshot room()throws Exception {
            var result=(ModernApplicationProtocol.RoomResult)old(new ModernApplicationProtocol.RoomRequest(next(),new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("healing TLS fixture","",2,1,514,16),"")));
            assertEquals(ModernRooms.Status.OK,result.result().status(),result.result().reason().name());return result.result().view().joined();
        }
        byte[] world(ModernBattleWorldProtocol.Action action,long room,long revision,ModernTargetHistory.World world,String digest)throws Exception {
            return exchange(new ModernApplicationProtocol.BattleWorldRequest(next(),new ModernBattleWorldProtocol.Request(scope,action,world==null?room:world.roomId(),revision,world==null?0:world.matchId(),world==null?0:world.generation(),digest)),67);
        }
        public void close()throws Exception {socket.close();}
    }
    record Probe(ModernTargetHistory.World world,long revision,int phase,long entity,int x,int y,long life,long gear,long mp) {
        static Probe from(byte[] frame,int offset) {
            return from(frame,offset,7);
        }
        static Probe from(byte[] frame,int offset,int schema) {
            assertTrue(schema==7||schema==8);int sections=schema==8?16:15;
            var b=ByteBuffer.wrap(frame,offset,frame.length-offset).slice();assertEquals(0x4c544231,b.getInt());assertEquals(schema,b.getShort());assertEquals(0,b.getShort());assertEquals(frame.length-offset,b.getInt());assertEquals(sections,b.getShort());assertEquals(0,b.getShort());
            ByteBuffer world=null,actors=null;
            for(int section=1;section<=sections;section++){assertEquals(section,b.getShort());assertEquals(0,b.getShort());int size=b.getInt();assertTrue(size>=0&&size<=b.remaining());var data=b.slice();data.limit(size);if(section==1)world=data;if(section==4)actors=data;b.position(b.position()+size);}
            assertFalse(b.hasRemaining());assertNotNull(world);assertNotNull(actors);
            var identity=new ModernTargetHistory.World(world.getLong(),world.getLong(),world.getLong());long revision=world.getLong();int phase=Byte.toUnsignedInt(world.get());
            assertEquals(2,actors.getInt());long entity=actors.getLong();int x=actors.getInt(),y=actors.getInt();actors.getInt();actors.get();actors.getLong();actors.getLong();
            assertEquals(1,actors.get());actors.position(actors.position()+32+8+4+6*8);
            assertEquals(1,actors.get());actors.getLong();long gear=actors.getLong(),life=actors.getLong();actors.getLong();actors.getLong();actors.getLong();actors.getLong();long mp=actors.getLong();
            return new Probe(identity,revision,phase,entity,x,y,life,gear,mp);
        }
        ModernPlayerHealingProtocol.Request self(int id,ModernRooms.Scope scope,long sequence) {
            return new ModernPlayerHealingProtocol.Request(id,new ModernBattleCommandLife.Proof(scope,world,entity,life+1,revision),sequence,gear,x,y,
                    new ModernPlayerHealingCast.Target(new ModernTargetHistory.Participant(entity,ModernParticipantId.human(scope.sourceId(),scope.objectId())),life+1,gear,new ModernWorldMap.Cell(x,y)));
        }
    }
}
