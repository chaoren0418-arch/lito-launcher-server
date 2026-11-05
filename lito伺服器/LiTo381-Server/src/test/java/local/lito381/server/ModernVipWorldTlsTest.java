package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual listener/TLS/AUTH/Navigation/backend lease/WorldService/file CAS.
 * Source rows are controlled; map514 is the existing shared map. Not UI/SQL. */
class ModernVipWorldTlsTest {
    @TempDir Path root;
    static final int ALL=112;
    @BeforeAll static void certificates()throws Exception{ModernPlayerHealingTlsTest.certificates();}
    static final class Fixture implements AutoCloseable {
        final ModernApplicationTestBackend backend=new ModernApplicationTestBackend("auth",80);
        final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms;final ModernWorldMap map;final ModernWorldService worlds;final ModernEquipmentOverrideStore store;final ModernServer server;
        final AtomicInteger vipReads=new AtomicInteger();
        Fixture(Path root)throws Exception {
            map=ModernWorldTestPeer.sharedMap(LocalResources.assumeSharedResourceRoot());
            var binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()));
            store=new ModernEquipmentOverrideStore(root);
            ModernEquipmentAuthority.PhysicalProvider physical=new ModernEquipmentAuthority.PhysicalProvider() {
                public boolean fullVipSource(int source){return source==7||source==42;}
                public ModernEquipmentAuthority.PhysicalLoaded read(ModernAccountAuthentication.Principal p,int source,long id)throws Exception {
                    if(!fullVipSource(source)||!p.authorizesSource(source)||id!=3333)throw new SecurityException("fixture source");
                    var row=backend.characters.characters(p,source).stream().filter(r->r.objectId()==id).findFirst().orElseThrow();
                    var data=ModernBattleEngineVipTest.source(source,row,Map.of(ModernVipEquipment.Field.MAX_HP,4_294_967_299L),false);
                    return new ModernEquipmentAuthority.PhysicalLoaded(data,ModernCombatProfileTest.binding(data.input().equipment()).fingerprint());
                }
                public ModernEquipmentAuthority.PhysicalLoaded readForWorld(ModernAccountAuthentication.Principal p,int s,long id,boolean armor)throws Exception {
                    vipReads.incrementAndGet();assertFalse(armor);return read(p,s,id);
                }
            };
            var equipment=new ModernEquipmentAuthority((p,s,id)->{var v=physical.read(p,s,id);return new ModernEquipmentAuthority.Loaded(v.source().input().equipment(),v.ownerBinding());},null,physical,store);
            var vip=new ModernVipEquipmentAuthority(physical,store,System::currentTimeMillis);
            var mapping=new ModernWorldService.Mapping(binding,ModernWorldTestPeer.ANCHOR,1_000_000,1_000_000,0,false,false,true,true,true,false,true,null,false).withFullVip(true);
            worlds=new ModernWorldService(rooms,Map.of(binding.key(),mapping),new ModernWorldService.Limits(2,2,0,4000,10,new ModernBattleEngine.Limits(10000,0,60000)),b->0,null,Map.of(7,100.0,42,100.0));
            ModernShopCatalogRepository shop=(p,s,id)->Optional.of(new ModernShopCatalog.Snapshot(s,p.accountForSource(s),physical.read(p,s,id).source().input().equipment().source().character(),List.of()));
            server=new ModernServer(new java.net.InetSocketAddress("127.0.0.1",0),ModernPlayerHealingTlsTest.serverContext,
                    new ModernServer.Limits(4,4,600,4,2,4,3000,2000,2000,10000),backend.verifier,backend.characters,presence,
                    (p,s,id)->Optional.of(physical.read(p,s,id).source().input().equipment().source()),equipment,store,rooms,worlds,47,
                    ModernSkillsRepository.unavailable(),ModernCharacterRecordRepository.unavailable(),null,ModernGmCommandRepository.unavailable(),shop,20000,null,false,null,null,null,vip);
        }
        public void close()throws Exception {
            server.close();assertTrue(server.awaitTermination(5000));worlds.close();assertTrue(worlds.awaitTermination(5000));
            assertTrue(worlds.isQuiescent());assertEquals(0,presence.ownerCount());assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.roomCount());
            assertTrue(store.isQuiescent());store.close();
        }
    }
    static byte[] world(ModernPlayerHealingTlsTest.Client c,ModernBattleWorldProtocol.Action action,
            long room,long revision,ModernTargetHistory.World w,String digest)throws Exception {
        return c.exchange(new ModernApplicationProtocol.VipWorldRequest(c.next(),new ModernBattleWorldProtocol.Request(c.scope,action,
                w==null?room:w.roomId(),revision,w==null?0:w.matchId(),w==null?0:w.generation(),digest)),85);
    }
    static ModernTargetHistory.World identity(byte[] frame){var b=ByteBuffer.wrap(frame);return new ModernTargetHistory.World(b.getLong(116),b.getLong(124),b.getLong(132));}
    static byte[] read(ModernPlayerHealingTlsTest.Client c,ModernTargetHistory.World w)throws Exception{return world(c,ModernBattleWorldProtocol.Action.READ,0,0,w,ModernWorldProtocol.ZERO_DIGEST);}
    static ModernRooms.RoomSnapshot ready(ModernPlayerHealingTlsTest.Client a,ModernPlayerHealingTlsTest.Client b)throws Exception {
        var room=a.room();var result=(ModernApplicationProtocol.RoomResult)b.old(new ModernApplicationProtocol.RoomRequest(b.next(),new ModernRooms.Command(b.scope,ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null,"")));
        assertEquals(ModernRooms.Status.OK,result.result().status());room=result.result().view().joined();
        for(var c:List.of(a,b)) {
            var fresh=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,ModernRooms.Action.READ,0,0,0,null,"")));
            room=fresh.result().view().joined();
            var reply=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null,"")));
            room=reply.result().view().joined();
        }
        assertTrue(room.summary().readySatisfied());return room;
    }
    static ModernTargetHistory.World active(Fixture f,ModernPlayerHealingTlsTest.Client a,ModernPlayerHealingTlsTest.Client b)throws Exception {
        a.login();b.login();var room=ready(a,b);
        var prepared=world(a,ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
        assertEquals(168,prepared.length);assertEquals(1,prepared[114]);var w=identity(prepared);
        byte[] loading=null;long end=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<end){loading=read(a,w);if(loading[114]==2)break;assertEquals(ModernWorldProtocol.Phase.PREPARING.ordinal(),loading[148]);Thread.sleep(5);}
        assertNotNull(loading);assertEquals(2,loading[114]);assertEquals(ModernWorldProtocol.Phase.LOADING.ordinal(),loading[148]);
        world(a,ModernBattleWorldProtocol.Action.LOADED,0,0,w,f.map.digest());
        var live=world(b,ModernBattleWorldProtocol.Action.LOADED,0,0,w,f.map.digest());assertEquals(ModernWorldProtocol.Phase.ACTIVE.ordinal(),live[148]);
        assertEquals(2,f.vipReads.get());return w;
    }
    /** A bounded independent field probe only. Complete cross-end decoding is
     * separately verified by Rust; no server object/reflection supplies intent. */
    static ModernBattleVipCommandState.Proof proof(byte[] frame,int offset) {
        var b=ByteBuffer.wrap(frame,offset,frame.length-offset).slice();assertEquals(0x4c544231,b.getInt());assertEquals(10,b.getShort());assertEquals(0,b.getShort());
        assertEquals(frame.length-offset,b.getInt());assertEquals(18,b.getShort());assertEquals(0,b.getShort());ByteBuffer owner=null;
        for(int s=1;s<=18;s++){assertEquals(s,b.getShort());assertEquals(0,b.getShort());int n=b.getInt();assertTrue(n>=0&&n<=b.remaining());if(s==18){owner=b.slice();owner.limit(n);}b.position(b.position()+n);}
        assertFalse(b.hasRemaining());assertNotNull(owner);var w=new ModernTargetHistory.World(owner.getLong(),owner.getLong(),owner.getLong());long rev=owner.getLong();
        var scope=new ModernRooms.Scope(owner.getInt(),owner.getLong(),owner.getLong(),owner.getInt());long entity=owner.getLong(),life=owner.getLong()+1;
        var versions=new ModernBattleVipObservation.Versions(owner.getLong(),owner.getLong(),owner.getLong());
        return new ModernBattleVipCommandState.Proof(new ModernBattleCommandLife.Proof(scope,w,entity,life,rev),versions);
    }
    @Test void twoAuthenticatedSourcesLoadInspectSaveReadLeaveAndReenter()throws Exception {
        try(var f=new Fixture(root);var a=new ModernPlayerHealingTlsTest.Client(f.server,ALL,7);var b=new ModernPlayerHealingTlsTest.Client(f.server,ALL,42)) {
            var w=active(f,a,b);var initial=proof(read(a,w),168);assertEquals(7,initial.life().player().sourceId());assertEquals(0,initial.versions().savedEquipmentRevision());
            var inspected=a.exchange(new ModernApplicationProtocol.VipEquipmentRequest(ModernVipEquipmentProtocol.Request.inspect(a.next(),initial)),83);
            assertEquals(ModernVipEquipmentProtocol.Outcome.INSPECTED.ordinal(),inspected[157]);
            var token=new ModernEquipmentOverrideStore.Fingerprint(Arrays.copyOfRange(inspected,176,208));
            var q=new ModernVipEquipmentProtocol.Request(a.next(),proof(inspected,236),ModernVipEquipmentProtocol.Action.SET,1,false,token);
            var saved=a.exchange(new ModernApplicationProtocol.VipEquipmentRequest(q),83);
            assertEquals(ModernVipEquipmentProtocol.Outcome.APPLIED.ordinal(),saved[157]);assertEquals(1,ByteBuffer.wrap(saved).getLong(168));
            assertEquals(1,proof(read(a,w),168).versions().savedEquipmentRevision());assertEquals(0,proof(read(b,w),168).versions().savedEquipmentRevision());
            assertEquals(168,world(a,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST).length);
            var aborted=read(b,w);assertEquals(ModernWorldProtocol.Phase.ABORTED.ordinal(),aborted[148]);
            world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);
            assertEquals(0,read(a,null)[114]);ModernWorldServiceTest.awaitNoMatches(f.worlds);
            var again=readyAfterLeave(a,b);var next=world(a,ModernBattleWorldProtocol.Action.PREPARE,again.summary().id(),again.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
            assertTrue(identity(next).matchId()>w.matchId());
            world(a,ModernBattleWorldProtocol.Action.LEAVE,0,0,identity(next),ModernWorldProtocol.ZERO_DIGEST);
            world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,identity(next),ModernWorldProtocol.ZERO_DIGEST);
            a.close();b.close();ModernVipWorldServiceTest.await(()->f.server.snapshot().sessions()==0);assertEquals(0,f.server.snapshot().failed());
        }
    }
    static ModernRooms.RoomSnapshot readyAfterLeave(ModernPlayerHealingTlsTest.Client a,ModernPlayerHealingTlsTest.Client b)throws Exception {
        ModernRooms.RoomSnapshot room=null;
        for(var c:List.of(a,b)) {
            var fresh=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,ModernRooms.Action.READ,0,0,0,null,"")));
            room=fresh.result().view().joined();
            room=((ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null,"")))).result().view().joined();
        }
        return room;
    }
    @Test void negotiatedWorldStillRequiresAuthentication()throws Exception {
        try(var f=new Fixture(root);var c=new ModernPlayerHealingTlsTest.Client(f.server,ALL)) {
            var q=new ModernApplicationProtocol.VipWorldRequest(c.next(),new ModernBattleWorldProtocol.Request(c.scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
            c.send(ModernApplicationProtocol.encode(q,c.peer));assertEquals(-1,c.socket.getInputStream().read());
            ModernVipWorldServiceTest.await(()->f.server.snapshot().sessions()==0);assertEquals(0,f.backend.authenticationCalls.get());assertEquals(0,f.vipReads.get());assertEquals(0,f.worlds.registrationCount());
        }
    }
    @Test void dataAndEquipmentSelectionCannotSendWorldLifecycleBeforeBodyRead()throws Exception {
        try(var f=new Fixture(root);var c=new ModernPlayerHealingTlsTest.Client(f.server,48)) {
            c.login();var selected=new ModernSessionBoundary.Negotiated(1,47,1,c.peer.capabilities(),ALL);
            var q=new ModernApplicationProtocol.VipWorldRequest(c.next(),new ModernBattleWorldProtocol.Request(c.scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
            c.send(Arrays.copyOf(ModernApplicationProtocol.encode(q,selected),16));
            assertEquals(-1,c.socket.getInputStream().read());ModernVipWorldServiceTest.await(()->f.server.snapshot().sessions()==0);assertEquals(0,f.vipReads.get());
        }
    }
    @Test void worldOnlySelectionCannotSendGearAndDisconnectAbortsOtherOwner()throws Exception {
        try(var f=new Fixture(root);var a=new ModernPlayerHealingTlsTest.Client(f.server,80,7);var b=new ModernPlayerHealingTlsTest.Client(f.server,80,42)) {
            var w=active(f,a,b);var selected=new ModernSessionBoundary.Negotiated(1,47,1,a.peer.capabilities(),ALL);
            var q=ModernVipEquipmentProtocol.Request.inspect(a.next(),proof(read(a,w),168));
            a.send(Arrays.copyOf(ModernVipEquipmentProtocol.encodeRequest(q,selected),16));assertEquals(-1,a.socket.getInputStream().read());
            ModernVipWorldServiceTest.await(()->f.worlds.registrationCount()==1);var aborted=read(b,w);
            assertEquals(ModernWorldProtocol.Phase.ABORTED.ordinal(),aborted[148]);assertEquals(ModernWorldProtocol.AbortReason.DISCONNECTED.ordinal(),aborted[149]);
            world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);
        }
    }
    @Test void foreignSelectedSourceCannotReadAnotherOwnersVipWorld()throws Exception {
        try(var f=new Fixture(root);var a=new ModernPlayerHealingTlsTest.Client(f.server,ALL,7);var b=new ModernPlayerHealingTlsTest.Client(f.server,ALL,42)) {
            var w=active(f,a,b);var q=new ModernApplicationProtocol.VipWorldRequest(a.next(),new ModernBattleWorldProtocol.Request(b.scope,ModernBattleWorldProtocol.Action.READ,w.roomId(),0,w.matchId(),w.generation(),ModernWorldProtocol.ZERO_DIGEST));
            a.send(ModernApplicationProtocol.encode(q,a.peer));assertEquals(-1,a.socket.getInputStream().read());
            ModernVipWorldServiceTest.await(()->f.worlds.registrationCount()==1);assertEquals(ModernWorldProtocol.Phase.ABORTED.ordinal(),read(b,w)[148]);
            world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);
        }
    }
}
