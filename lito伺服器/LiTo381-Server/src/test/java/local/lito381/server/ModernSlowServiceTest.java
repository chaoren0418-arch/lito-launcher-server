package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real Rooms -> source readers -> shop freeze -> engine -> canonical publication.
 * No Slow command opcode, network listener or player UI is enabled here. */
class ModernSlowServiceTest {
    static final int CAPS=ModernSessionBoundary.Policy.world(true,true,true,true,true).withBattleActions().withSlowData().supportedCapabilities();
    @Test void sourceQualifiedMaterialsLoadThroughTheActualPreparationAndReturnPath()throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(2,f.reads.get());assertEquals(2,f.captures.get());
            for(int i=0;i<2;i++) {
                var p=f.player(i);var view=f.read(i).view();
                assertTrue(view.world().descriptor().has(ModernMatchDescriptor.SLOW));
                assertEquals(p.scope(),view.personal().scope());assertEquals(f.player(0).scope().objectId(),p.scope().objectId());
                assertEquals(4,view.personal().materials().stocks().get(0).remaining());
                assertEquals(0,view.personal().materials().uses());assertTrue(f.shops.get(i).frozen());
                var request=new ModernBattleReadProtocol.Request(p.scope(),f.world);
                var read=f.service.readBattle(p.owner(),request);assertEquals(view.world().identity(),read.view().world().identity());
                var result=new ModernApplicationProtocol.BattleReadResult(1,request,read);
                assertSame(result,ModernMatchDescriptor.forPeer(result,new ModernSessionBoundary.Negotiated(1,46,1,CAPS)));
                var bytes=ModernApplicationProtocol.encode(result,46);
                assertEquals(6,ByteBuffer.wrap(bytes).getShort(ModernBattleReadProtocol.MIN_RESULT_BYTES+4));
                assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,45));
            }
            var request=new ModernBattleReadProtocol.Request(f.player(0).scope(),f.world);
            assertThrows(SecurityException.class,()->f.service.readBattle(f.player(1).owner(),request));
            f.leave(0);assertEquals(ModernWorldProtocol.Phase.ABORTED,f.read(1).lifecycle().phase());
            f.leave(1);ModernWorldServiceTest.awaitNoMatches(f.service);
            ModernNpcServiceTest.await(()->f.terminals.get()==2);
            for(int i=0;i<2;i++){assertFalse(f.shops.get(i).frozen());assertTrue(f.shopRead(i).stacks().isEmpty());}
        }
    }
    @Test void readerAndVersionAreRequiredBeforeAnyPreparationOrShopCapture()throws Exception {
        try(var f=new Fixture()) {
            assertThrows(IllegalArgumentException.class,()->f.register(0,46,CAPS,null));
            assertThrows(IllegalArgumentException.class,()->f.register(0,45,CAPS,f.reader(0)));
            assertThrows(IllegalArgumentException.class,()->f.register(0,46,CAPS&~ModernSessionProtocol.CAP_SLOW_DATA,f.reader(0)));
            f.register(1,45,CAPS&~ModernSessionProtocol.CAP_SLOW_DATA,null);
            assertNotEquals(ModernWorldProtocol.Status.OK,f.prepare().status());
            assertEquals(0,f.reads.get());assertEquals(0,f.captures.get());assertEquals(0,f.service.matchCount());
        }
    }
    @Test void wrongSourceFailsBeforeInventoryFreezeAndCannotPublish()throws Exception {
        try(var f=new Fixture()) {
            f.register(0,46,CAPS,d->new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(42,f.player(0).scope().objectId()),ModernSlowCatalog.Status.QUALIFIED,OptionalLong.of(0)));
            assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());
            ModernNpcServiceTest.await(()->f.read(1).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
            assertNull(f.read(1).view());assertEquals(0,f.captures.get());
            f.leave(0);f.leave(1);ModernWorldServiceTest.awaitNoMatches(f.service);
        }
    }
    @Test void providerFailureOrLateReturnCannotFreezeInventoryAfterDeparture()throws Exception {
        for(boolean fail:List.of(false,true))try(var f=new Fixture()) {
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            f.register(0,46,CAPS,deadline->{
                entered.countDown();if(fail)throw new java.io.IOException("fixture source failure");
                boolean interrupted=false;for(;;)try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("source release timeout");break;}catch(InterruptedException e){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();return f.snapshot(0);
            });
            try {
                assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());assertTrue(entered.await(2,TimeUnit.SECONDS));
                if(!fail)f.leave(0);release.countDown();
                ModernNpcServiceTest.await(()->f.read(1).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
                assertNull(f.read(1).view());assertEquals(0,f.captures.get());assertEquals(0,f.reads.get());
                f.leave(1);if(fail)f.leave(0);
            }finally{release.countDown();}
        }
    }
    @Test void mappingCopiesKeepTheOptInAndCannotEnableItWithoutRequiredTimedFacts()throws Exception {
        try(var f=new Fixture()) {
            assertTrue(f.mapping.withClockOverrides(null).slowEnabled());
            assertTrue(f.mapping.withDisintegrate(false).slowEnabled());
            assertTrue(f.mapping.withHaste(true).slowEnabled());
            assertTrue(f.mapping.withGreaterHaste(true).slowEnabled());
            assertFalse(f.mapping.withSlow(false).slowEnabled());
            var old=new ModernWorldService.Mapping(f.room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,2_000_000_000L,false,false,true,true);
            assertThrows(IllegalArgumentException.class,()->old.withSlow(true));
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room=new ModernBattleEngineBotTest.RoomFixture(2,0);
        final ModernWorldService.Mapping mapping;
        final ModernWorldService service;
        final List<ModernPhysicalCombat.Fighter> fighters=new ArrayList<>();
        final List<ModernShopAuthority> shops=new ArrayList<>();
        final List<ModernShopCatalog.Snapshot> catalogs=new ArrayList<>();
        final AtomicInteger reads=new AtomicInteger(),captures=new AtomicInteger(),terminals=new AtomicInteger();
        ModernTargetHistory.World world;
        Fixture()throws Exception {
            mapping=new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,2_000_000_000L,false,false,true,true,true).withSlow(true);
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),mapping),new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(4000,0,30000)),bound->0,null,Map.of(7,100.0,42,100.0));
            for(int i=0;i<2;i++){
                var p=player(i);fighters.add(ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.row(),1000,300,12,0));
                catalogs.add(new ModernShopCatalog.Snapshot(p.scope().sourceId(),"fixture-owner",p.row(),List.of(ModernSpellMaterialsTest.offer(2000))));
                var shop=new ModernShopAuthority(2000,p.scope().sourceId(),p.row(),"fixture-owner",true,false,true);shops.add(shop);
                var read=shopRead(i);var bought=shop.apply(ModernShopAuthority.Action.BUY,read.roundId(),read.revision(),read.token(),40318,4,catalogs.get(i),basis(i));
                assertEquals(ModernShopAuthority.Status.OK,bought.status());register(i,46,CAPS,reader(i));
            }
        }
        ModernBattleEngineBotTest.Player player(int i){return room.players.get(i);}
        ModernInventoryWeight.Basis basis(int i){return ModernInventoryWeight.qualify(fighters.get(i).profile(),100);}
        ModernShopAuthority.Snapshot shopRead(int i){return shops.get(i).apply(ModernShopAuthority.Action.READ,0,0,null,0,0,catalogs.get(i),basis(i)).snapshot();}
        ModernSlowCatalog.Snapshot snapshot(int i){return new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(player(i).scope().sourceId(),player(i).scope().objectId()),i==0?ModernSlowCatalog.Status.QUALIFIED:ModernSlowCatalog.Status.UNLEARNED,OptionalLong.of(0));}
        ModernWorldService.SlowReader reader(int i){return deadline->{assertTrue(deadline-System.nanoTime()>0);reads.incrementAndGet();return snapshot(i);};}
        void register(int i,int version,int caps,ModernWorldService.SlowReader reader) {
            var p=player(i);service.register(p.owner(),p.scope(),d->p.row(),d->fighters.get(i),true,
                d->new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),i==0?List.of(new ModernSkills.Skill(29,"$1455",20,500,ModernSkills.Support.UNIMPLEMENTED)):List.of()),false,
                new ModernWorldService.MatchItems(){
                    public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError("unbound capture");}
                    public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b,ModernMatchDescriptor descriptor){
                        assertTrue(descriptor.has(ModernMatchDescriptor.SLOW));captures.incrementAndGet();return shops.get(i).capture(m,g,d,catalogs.get(i),b,descriptor);
                    }
                    public void terminal(long m,long g){shops.get(i).terminal(m,g);terminals.incrementAndGet();}
                },true,true,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,caps),null,null,null,reader);
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
        void leave(int i){assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.LEAVE)).status());}
        public void close()throws Exception{service.close();assertTrue(service.awaitTermination(5000));for(var shop:shops)shop.close();room.close();}
    }
}
