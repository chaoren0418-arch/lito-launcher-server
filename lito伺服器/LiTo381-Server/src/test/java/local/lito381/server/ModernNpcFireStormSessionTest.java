package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real loopback TLS -> authentication -> navigation -> room -> world dispatch.
 * Flat geometry and source-qualified in-memory repositories, not SQL or a renderer. */
class ModernNpcFireStormSessionTest {
    @TempDir Path temporary;
    static final class Fixture implements AutoCloseable {
        final ModernNpcFireStormServiceTest.Fixture world;
        final ModernServerConfig config;
        final ModernEquipmentOverrideStore store;
        final ModernServer server;
        final AtomicInteger reads=new AtomicInteger();
        Fixture(Path path)throws Exception {
            var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
            String base=ModernNpcConfigurationTest.base(path).replace("source.7.npc.blessingConvention=INTERNAL_ONE_BLESSED\n","");
            String second=base.lines().filter(l->l.startsWith("source.7.")).map(l->l.replace("source.7.","source.42.").replace("name=Configured","name=Moon").replace("not_a_live_database","second_fixture_database")).collect(java.util.stream.Collectors.joining("\n"));
            config=ModernServerConfig.parse((base.replace("sources=7\n","sources=7,42\n")+"\n"+second+"\n").getBytes(StandardCharsets.UTF_8),key->"fixture-only");
            store=new ModernEquipmentOverrideStore(path);
            world=new ModernNpcFireStormServiceTest.Fixture(true,0,false);
            var backend=new ModernApplicationTestBackend("auth",80);
            ModernEquipmentAuthority.PhysicalProvider physical=(principal,source,object)->{
                if(!principal.authorizesSource(source)||object!=3333||!(source==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT).equals(principal.accountForSource(source)))throw new SecurityException();
                var row=backend.characters.characters(principal,source).stream().filter(c->c.objectId()==object).findFirst().orElseThrow();
                var binding=ModernEquipmentBinding.ownerBinding(principal,config.sources().stream().filter(s->s.id()==source).findFirst().orElseThrow());
                return new ModernEquipmentAuthority.PhysicalLoaded(ModernBattleEngineNpcDisintegrateTest.source(ModernBattleEngineHonorTest.source(source,row,10000,1000),1000,500),binding);
            };
            var authority=new ModernEquipmentAuthority((p,s,o)->{var v=physical.read(p,s,o);return new ModernEquipmentAuthority.Loaded(v.source().input().equipment(),v.ownerBinding());},null,physical,store);
            ModernSkillsRepository skills=(principal,source,object)->{
                reads.incrementAndGet();var row=physical.read(principal,source,object).source().input().equipment().source().character();
                return Optional.of(new ModernSkills.Snapshot(source,row,source==7?List.of(ModernSkillsTest.teleport(),new ModernSkills.Skill(70,"$1478",48,2000,ModernSkills.Support.FIRE_STORM)):List.of(ModernSkillsTest.teleport())));
            };
            ModernShopCatalogRepository shop=(p,s,o)->Optional.of(new ModernShopCatalog.Snapshot(s,p.accountForSource(s),physical.read(p,s,o).source().input().equipment().source().character(),List.of()));
            server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(tls,false),
                    new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),backend.verifier,backend.characters,world.presence,
                    (p,s,o)->Optional.of(physical.read(p,s,o).source().input().equipment().source()),authority,store,world.rooms,world.service,31,
                    skills,ModernCharacterRecordRepository.unavailable(),null,ModernGmCommandRepository.unavailable(),shop,20000);
        }
        public void close()throws Exception {
            server.close();assertTrue(server.awaitTermination(5000));world.close();store.close();config.close();
            assertEquals(0,world.service.registrationCount());assertTrue(store.isQuiescent());
            assertEquals(0,server.snapshot().backendActive());assertEquals(0,server.snapshot().backendQueued());
        }
    }
    static final class Client implements AutoCloseable {
        final SSLSocket socket;final ModernSessionProtocol.Ack ack;int id=1;ModernRooms.Scope scope;
        Client(Fixture f,int source,boolean capability)throws Exception {
            socket=(SSLSocket)ModernTlsTestMaterial.clientContext(ModernTlsTestMaterial.defaultDirectory()).getSocketFactory().createSocket();
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
            socket.setSoTimeout(4000);socket.connect(f.server.address(),1000);socket.startHandshake();
            int caps=ModernNpcFireStormAdmissionTest.CAPS&~(capability?0:ModernSessionProtocol.CAP_NPC_FIRE_STORM);
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(id,31,31,caps,3,1)));socket.getOutputStream().flush();
            ack=assertInstanceOf(ModernSessionProtocol.Ack.class,ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
            assertEquals(capability,(ack.capabilities()&ModernSessionProtocol.CAP_NPC_FIRE_STORM)!=0);
            var auth=assertInstanceOf(ModernApplicationProtocol.AuthResult.class,exchange(new ModernApplicationProtocol.Auth(++id,ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())));
            assertEquals(0,auth.status());select(source,1);
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request)throws Exception {
            byte[] bytes=ModernApplicationProtocol.encode(request,31);
            try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);request.close();}
            byte[] response=ModernApplicationProtocol.readFrame(socket.getInputStream(),31);
            if(response==null)throw new java.io.EOFException();
            var result=ModernApplicationProtocol.decode(response,31);assertEquals(request.requestId(),result.requestId());return result;
        }
        void select(int source,long epoch)throws Exception {
            var roster=assertInstanceOf(ModernApplicationProtocol.CharacterResult.class,exchange(new ModernApplicationProtocol.CharacterRequest(++id,source,epoch)));
            assertEquals(3333,roster.characters().get(0).objectId());
            var selection=assertInstanceOf(ModernApplicationProtocol.NavigationResult.class,exchange(new ModernApplicationProtocol.CharacterSelection(++id,source,3333,epoch)));
            assertEquals(0,selection.status());assertEquals(1,selection.phase());
            var lobby=assertInstanceOf(ModernApplicationProtocol.NavigationResult.class,exchange(new ModernApplicationProtocol.ChannelEnter(++id,source,3333,epoch,0)));
            assertEquals(0,lobby.status());assertEquals(2,lobby.phase());scope=new ModernRooms.Scope(source,3333,epoch,0);
        }
        ModernSpellProtocol.SkillsReply skills()throws Exception{return assertInstanceOf(ModernApplicationProtocol.SkillsResult.class,exchange(new ModernApplicationProtocol.SkillsRequest(++id,new ModernSpellProtocol.Context(scope.sourceId(),scope.objectId(),scope.epoch())))).reply();}
        ModernRooms.Result room(ModernRooms.Action action,ModernRooms.RoomSnapshot joined,int value,ModernRooms.Draft draft)throws Exception {
            return assertInstanceOf(ModernApplicationProtocol.RoomResult.class,exchange(new ModernApplicationProtocol.RoomRequest(++id,new ModernRooms.Command(scope,action,joined==null?0:joined.summary().id(),joined==null?0:joined.summary().revision(),value,draft,"")))).result();
        }
        ModernRooms.RoomSnapshot room()throws Exception{return room(ModernRooms.Action.READ,null,0,null).view().joined();}
        ModernWorldProtocol.Reply world(ModernWorldProtocol.Action action,ModernWorldProtocol.Frame frame,ModernRooms.RoomSnapshot room)throws Exception {
            var r=new ModernWorldProtocol.Request(scope,action,0,room==null?0:room.summary().id(),room==null?0:room.summary().revision(),
                    frame==null?0:frame.matchId(),frame==null?0:frame.generation(),0,0,0,action==ModernWorldProtocol.Action.LOADED?frame.manifest().digest():ModernWorldProtocol.ZERO_DIGEST);
            return assertInstanceOf(ModernApplicationProtocol.WorldResult.class,exchange(new ModernApplicationProtocol.WorldRequest(++id,r))).reply();
        }
        ModernNpcProtocol.Frame read(ModernWorldProtocol.Frame frame)throws Exception {
            var r=new ModernNpcProtocol.Request(scope,ModernNpcProtocol.Action.READ,new ModernNpcRoster.Scope(frame.room().summary().id(),frame.matchId(),frame.generation()),0,0,0);
            return assertInstanceOf(ModernApplicationProtocol.NpcResult.class,exchange(new ModernApplicationProtocol.NpcRequest(++id,r))).reply().frame();
        }
        ModernNpcFireStormProtocol.Reply cast(ModernNpcProtocol.Frame frame,long sequence)throws Exception {
            return assertInstanceOf(ModernApplicationProtocol.NpcFireStormResult.class,exchange(new ModernApplicationProtocol.NpcFireStormRequest(++id,new ModernNpcFireStormState.Request(scope,frame.scope(),sequence,1)))).reply();
        }
        void returnToRoster()throws Exception {
            var r=assertInstanceOf(ModernApplicationProtocol.NavigationResult.class,exchange(new ModernApplicationProtocol.CharacterReturn(++id,scope.sourceId(),scope.objectId(),scope.epoch())));
            assertEquals(0,r.status());assertEquals(0,r.phase());assertNull(r.selected());
        }
        public void close()throws Exception{socket.close();}
    }
    static ModernNpcProtocol.Frame active(Client a,Client b)throws Exception {
        var create=a.room(ModernRooms.Action.CREATE,null,0,ModernRoomsTest.draft(2,2,554,0));ModernRoomsTest.accepted(create);
        ModernRoomsTest.accepted(b.room(ModernRooms.Action.JOIN,create.view().joined(),0,null));
        ModernRoomsTest.accepted(a.room(ModernRooms.Action.READY,a.room(),1,null));ModernRoomsTest.accepted(b.room(ModernRooms.Action.READY,b.room(),1,null));
        assertEquals(ModernWorldProtocol.Status.OK,a.world(ModernWorldProtocol.Action.PREPARE,null,a.room()).status());
        long until=System.nanoTime()+3_000_000_000L;ModernWorldProtocol.Frame loading;
        do{loading=a.world(ModernWorldProtocol.Action.READ,null,null).frame();if(loading.phase()!=ModernWorldProtocol.Phase.PREPARING)break;assertTrue(System.nanoTime()<until);Thread.sleep(2);}while(true);
        assertEquals(ModernWorldProtocol.Phase.LOADING,loading.phase());assertTrue(loading.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM));
        // Protocol barrier only: no renderer or shared SPR is installed here.
        assertEquals(ModernWorldProtocol.Status.OK,a.world(ModernWorldProtocol.Action.LOADED,loading,null).status());
        assertEquals(ModernWorldProtocol.Status.OK,b.world(ModernWorldProtocol.Action.LOADED,loading,null).status());
        var frame=a.read(loading);assertEquals(ModernWorldProtocol.Phase.ACTIVE,frame.combat().world().phase());return frame;
    }
    @Test void actualTlsDispatchHonorsEachSourceLearnedSkillsCooldownAndReturn()throws Exception {
        try(var f=new Fixture(temporary);var a=new Client(f,7,true);var b=new Client(f,42,true)){
            assertEquals(ModernSkills.Support.FIRE_STORM,a.skills().skills().stream().filter(s->s.skillId()==70).findFirst().orElseThrow().support());
            assertFalse(b.skills().skills().stream().anyMatch(s->s.skillId()==70));
            var frame=active(a,b);int captured=f.reads.get();
            var hit=a.cast(frame,1);assertEquals(NONE,hit.reason());assertEquals(1,hit.frame().combat().npcFireStorms().get(0).castCount());
            assertEquals(TOO_FAST,a.cast(hit.frame(),2).reason());assertEquals(UNLEARNED,b.cast(frame,1).reason());assertEquals(captured,f.reads.get());
            a.world(ModernWorldProtocol.Action.LEAVE,hit.frame().combat().world(),null);
            b.world(ModernWorldProtocol.Action.LEAVE,hit.frame().combat().world(),null);
            ModernRoomsTest.accepted(b.room(ModernRooms.Action.LEAVE,b.room(),0,null));
            ModernRoomsTest.accepted(a.room(ModernRooms.Action.LEAVE,a.room(),0,null));
            a.returnToRoster();b.returnToRoster();
            a.select(42,2);assertFalse(a.skills().skills().stream().anyMatch(s->s.skillId()==70));a.returnToRoster();
            assertEquals(0,f.server.snapshot().failed());
        }
    }
    @Test void foreignSelectedSourceOverTlsClosesOnlyOffenderAndCannotCast()throws Exception {
        try(var f=new Fixture(temporary);var a=new Client(f,7,true);var b=new Client(f,42,true)){
            var frame=active(a,b);
            var forged=new ModernNpcFireStormState.Request(b.scope,frame.scope(),1,1);
            assertThrows(java.io.IOException.class,()->a.exchange(new ModernApplicationProtocol.NpcFireStormRequest(++a.id,forged)));
            ModernNpcServiceTest.await(()->f.world.service.registrationCount()==1);
            var closed=b.read(frame.combat().world());assertTrue(closed.closed());
            assertTrue(closed.combat().npcFireStorms().stream().allMatch(s->s.castCount()==0));
            b.world(ModernWorldProtocol.Action.LEAVE,frame.combat().world(),null);
            ModernRoomsTest.accepted(b.room(ModernRooms.Action.LEAVE,b.room(),0,null));b.returnToRoster();
            assertEquals(1,f.server.snapshot().failed());
        }
    }
    @Test void unnegotiatedCastIsRejectedAtApplicationBoundaryAndSkillIsMasked()throws Exception {
        try(var f=new Fixture(temporary);var a=new Client(f,7,false)){
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,a.skills().skills().stream().filter(s->s.skillId()==70).findFirst().orElseThrow().support());
            var r=new ModernNpcFireStormState.Request(a.scope,new ModernNpcRoster.Scope(1,1,1),1,1);
            assertThrows(java.io.IOException.class,()->a.exchange(new ModernApplicationProtocol.NpcFireStormRequest(++a.id,r)));
        }
    }
}
