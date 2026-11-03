package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real preparation/provider/engine/publication path, not a player UI test. */
class ModernGreaterHasteServiceTest {
    static final int CAPS=ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withGreaterHaste().supportedCapabilities();
    @Test void explicitCapabilityDowngradesWithoutLeakingIntoOldPeers()throws Exception {
        var policy=ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withGreaterHaste();
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,42,CAPS,7,1));
        var current=new ModernSessionBoundary(policy);current.acceptHello(hello);
        assertTrue(ModernGreaterHasteProtocol.negotiated(current.negotiated().orElseThrow()));
        var old=new ModernSessionBoundary(policy.withMaxVersion(41));old.acceptHello(hello);
        assertEquals(0,old.negotiated().orElseThrow().capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE);
        assertFalse(ModernGreaterHasteProtocol.negotiated(old.negotiated().orElseThrow()));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,41,1,CAPS)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,41,CAPS,7,1)));
        for(int bit:new int[]{ModernSessionProtocol.CAP_TIMED,ModernSessionProtocol.CAP_BATTLE_DATA,ModernSessionProtocol.CAP_ACTION_LIFE,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR})
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(CAPS&~bit,0));
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_GREATER_HASTE);
    }
    @Test void preparesBothSourcesThenPublishesSuccessfulAndDeniedCastsWithoutLegacyProjection()throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(2,f.reads.get());var q=f.intent(0,1);
            var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.GreaterHasteRequest(q),42);
            var decoded=(ModernApplicationProtocol.GreaterHasteRequest)ModernApplicationProtocol.decode(bytes,42);
            var success=f.service.greaterHaste(f.player(0).owner(),decoded.request());
            assertEquals(ModernGreaterHasteCast.Reason.NONE,success.reason());assertEquals(240,success.view().world().actors().get(0).vital().values().mp());
            assertEquals(300,success.view().world().actors().get(1).vital().values().mp());
            var reply=ModernApplicationProtocol.encode(new ModernApplicationProtocol.GreaterHasteResult(success),42);
            assertEquals(3,ByteBuffer.wrap(reply).getShort(156));
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.GreaterHasteResult(success),41));
            var denied=f.service.greaterHaste(f.player(0).owner(),f.intent(0,2));
            assertEquals(ModernGreaterHasteCast.Reason.TOO_FAST,denied.reason());
            assertEquals(success.view().personal().history().resources(),denied.view().personal().history().resources());
            var foreign=f.service.greaterHaste(f.player(1).owner(),f.intent(1,1));
            assertEquals(ModernGreaterHasteCast.Reason.UNLEARNED,foreign.reason());
            assertEquals(300,foreign.view().world().actors().get(1).vital().values().mp());
            assertEquals(success.view().personal().scope().objectId(),foreign.view().personal().scope().objectId());
            assertNotEquals(success.view().personal().scope().sourceId(),foreign.view().personal().scope().sourceId());
            var read=f.read(0);var encoded=ModernBattleWorldProtocol.encodeResult(new ModernApplicationProtocol.BattleWorldResult(5,f.worldRequest(0,ModernBattleWorldProtocol.Action.READ),read),42);
            assertEquals(3,ByteBuffer.wrap(encoded).getShort(ModernBattleWorldProtocol.MIN_RESULT_BYTES+4));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWorldProtocol.encodeResult(new ModernApplicationProtocol.BattleWorldResult(5,f.worldRequest(0,ModernBattleWorldProtocol.Action.READ),read),41));
            f.leave(0);assertEquals(ModernWorldProtocol.Phase.ABORTED,f.read(1).lifecycle().phase());
            assertEquals(ModernGreaterHasteCast.Reason.NOT_ACTIVE,f.service.greaterHaste(f.player(1).owner(),f.intent(1,2)).reason());
            f.leave(1);assertEquals(0,f.service.matchCount());
        }
    }
    @Test void ownerEntityRoomFutureRevisionAndStaleGearAreNotBorrowed()throws Exception {
        try(var f=new Fixture()) {
            f.active();var q=f.intent(0,1);var p=q.proof();var before=f.read(0).view();
            assertThrows(SecurityException.class,()->f.service.greaterHaste(f.player(1).owner(),q));
            for(var proof:List.of(new ModernBattleCommandLife.Proof(p.player(),p.world(),2,p.life(),p.revision()),
                    new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life(),Long.MAX_VALUE),
                    new ModernBattleCommandLife.Proof(p.player(),new ModernTargetHistory.World(p.world().roomId()+1,p.world().matchId(),p.world().generation()),p.entity(),p.life(),p.revision()))) {
                var bad=new ModernGreaterHasteProtocol.Request(1,proof,1,q.equipmentRevision(),q.x(),q.y(),q.buffRevision());
                assertThrows(SecurityException.class,()->f.service.greaterHaste(f.player(0).owner(),bad));
            }
            var stale=new ModernGreaterHasteProtocol.Request(1,p,1,q.equipmentRevision()+1,q.x(),q.y(),q.buffRevision());
            var result=f.service.greaterHaste(f.player(0).owner(),stale);assertEquals(ModernGreaterHasteCast.Reason.STALE_STATE,result.reason());
            assertEquals(before.personal().history().resources(),result.view().personal().history().resources());
            assertEquals(1,result.view().world().actors().get(0).spells().sequence());
            assertEquals(ModernGreaterHasteCast.Reason.NONE,f.service.greaterHaste(f.player(0).owner(),f.intent(0,2)).reason());
        }
    }
    @Test void oldPeerCannotPrepareTheNewBindingOrReadItsCatalog()throws Exception {
        try(var f=new Fixture()) {
            f.register(1,41,null);var reply=f.prepare();assertNotEquals(ModernWorldProtocol.Status.OK,reply.status());
            assertEquals(0,f.reads.get());assertEquals(0,f.service.matchCount());
        }
    }
    @Test void blockedCatalogDoesNotPublishAfterDepartureAndProviderFailureAborts()throws Exception {
        for(boolean fail:List.of(false,true))try(var f=new Fixture()) {
            var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
            f.register(0,42,deadline->{
                entered.countDown();
                if(fail)throw new java.io.IOException("fixture failure");
                boolean interrupted=false;
                for(;;)try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("provider release timeout");break;}catch(InterruptedException e){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();
                return new ModernGreaterHasteCatalog.Snapshot(new TournamentPresence.CharacterKey(7,3333),ModernGreaterHasteCatalog.Status.QUALIFIED);
            });
            try {
                assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());assertTrue(entered.await(2,TimeUnit.SECONDS));
                if(!fail)f.leave(0);
                release.countDown();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while(f.read(1).lifecycle().phase()!=ModernWorldProtocol.Phase.ABORTED){assertTrue(System.nanoTime()<until);Thread.sleep(5);}
                assertNull(f.read(1).view());assertEquals(0,f.reads.get(),"next source must not be captured after retirement");
                f.leave(1);if(fail)f.leave(0);
            } finally {release.countDown();}
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room=new ModernBattleEngineBotTest.RoomFixture(2,0);
        final ModernWorldService service;final AtomicInteger reads=new AtomicInteger();ModernTargetHistory.World world;
        Fixture()throws Exception {
            var mapping=new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,2_000_000_000L,false,false,true,true,true).withGreaterHaste(true);
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),mapping),new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(4000,0,30000)),bound->bound-1,null,Map.of(7,100.0,42,100.0));
            for(int i=0;i<2;i++){final int index=i;register(i,42,d->{assertTrue(d-System.nanoTime()>0);reads.incrementAndGet();return new ModernGreaterHasteCatalog.Snapshot(new TournamentPresence.CharacterKey(player(index).scope().sourceId(),player(index).row().objectId()),index==0?ModernGreaterHasteCatalog.Status.QUALIFIED:ModernGreaterHasteCatalog.Status.UNLEARNED);});}
        }
        ModernBattleEngineBotTest.Player player(int i){return room.players.get(i);}
        void register(int i,int version,ModernWorldService.GreaterHasteReader reader){
            var p=player(i);var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.row(),1000,300,12,0);
            service.register(p.owner(),p.scope(),d->p.row(),d->fighter,true,d->new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),i==0?List.of(new ModernSkills.Skill(54,"$1976",60,10,ModernSkills.Support.UNIMPLEMENTED)):List.of()),false,
                    new ModernWorldService.MatchItems(){public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}public void terminal(long m,long g){}},
                    true,true,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,version>=42?CAPS:CAPS&~ModernSessionProtocol.CAP_GREATER_HASTE),null,reader);
        }
        ModernBattleWorldProtocol.Reply prepare(){var s=room.view().summary();var r=service.battleWorld(player(0).owner(),new ModernBattleWorldProtocol.Request(player(0).scope(),ModernBattleWorldProtocol.Action.PREPARE,s.id(),s.revision(),0,0,ModernWorldProtocol.ZERO_DIGEST));if(r.world()!=null)world=r.world();return r;}
        ModernBattleWorldProtocol.Request worldRequest(int i,ModernBattleWorldProtocol.Action action){return new ModernBattleWorldProtocol.Request(player(i).scope(),action,world.roomId(),0,world.matchId(),world.generation(),action==ModernBattleWorldProtocol.Action.LOADED?room.map.digest():ModernWorldProtocol.ZERO_DIGEST);}
        ModernBattleWorldProtocol.Reply read(int i){return service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.READ));}
        void active()throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,prepare().status());long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(read(0).view()==null){assertNotEquals(ModernWorldProtocol.Phase.ABORTED,read(0).lifecycle().phase());assertTrue(System.nanoTime()<until);Thread.sleep(5);}
            for(int i=0;i<2;i++)assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.LOADED)).status());
            assertEquals(ModernWorldProtocol.Phase.ACTIVE,read(0).lifecycle().phase());
        }
        ModernGreaterHasteProtocol.Request intent(int i,long sequence){var v=read(i).view();var a=v.world().actors().get(i);return new ModernGreaterHasteProtocol.Request(1,new ModernBattleCommandLife.Proof(player(i).scope(),world,a.pose().entityId(),a.resources().life()+1,v.world().revision()),sequence,a.resources().equipmentRevision(),a.pose().position().x(),a.pose().position().y(),a.buffs().revision());}
        void leave(int i){assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.LEAVE)).status());}
        public void close()throws Exception{service.close();assertTrue(service.awaitTermination(5000));room.close();}
    }
}
