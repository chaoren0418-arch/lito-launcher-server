package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class ModernWorldServiceItemTest {
    @Test void captureFreezesOnlyMatchSuppliesAndSourceInventoryIsUntouched() throws Exception {
        try(var f=new Fixture()) {
            var frame=f.loading();assertEquals(2,f.captures.get());
            for(var shop:f.shops)assertTrue(shop.frozen());
            var combat=f.combat(0);assertEquals(2,combat.items().size());assertEquals(2,combat.items().get(0).stocks().get(0).remaining());
            assertTrue(combat.items().get(1).stocks().isEmpty());assertTrue(f.fighter(0).profile().source().inventory().isEmpty());
            f.loaded(0,frame);assertEquals(Phase.LOADING,f.read().phase());f.loaded(1,frame);assertEquals(Phase.ACTIVE,f.read().phase());
            var p=f.room.players.get(0);var result=f.service.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,frame.matchId(),frame.generation(),1,40010));
            assertEquals(ModernItemProtocol.Status.OK,result.status());assertEquals(1,result.frame().items().get(0).stocks().get(0).remaining());
            assertEquals(100,result.frame().vitals().get(0).hp());assertEquals(0,result.frame().items().get(0).restored());
            f.leave(0,frame);await(()->f.terminals.get()==2);
            for(int i=0;i<2;i++) {var reset=f.shops.get(i).apply(ModernShopAuthority.Action.READ,0,0,null,0,0,f.catalog(i));assertEquals(2,reset.snapshot().roundId());assertEquals(20000,reset.snapshot().balance());assertTrue(reset.snapshot().stacks().isEmpty());}
        }
    }
    @Test void deathResultResetsOnceBeforeReturnAndSavedReceiptRetainsItemHistory() throws Exception {
        try(var f=new Fixture()) {
            var frame=f.loading();f.loaded(0,frame);f.loaded(1,frame);
            var p=f.room.players.get(0);var used=f.service.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,frame.matchId(),frame.generation(),1,40010));
            assertEquals(ModernItemProtocol.Status.OK,used.status());
            ModernCombatProtocol.Frame last=null;
            for(int i=1;i<20;i++) {
                var hit=f.service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,frame.matchId(),frame.generation(),i,2));
                assertEquals(ModernCombatProtocol.Status.OK,hit.status());last=hit.frame();
                if(last.world().phase()==Phase.FINISHED)break;Thread.sleep(2);
            }
            assertNotNull(last);assertEquals(Phase.FINISHED,last.world().phase());
            await(()->f.read().completion().state()==SettlementState.SAVED);assertEquals(2,f.terminals.get());assertEquals(1,f.persisted.get());
            var saved=f.combat(0);assertEquals(last.items(),saved.items());
            assertEquals(1,saved.items().get(0).useCount());assertEquals(1,saved.items().get(0).stocks().get(0).remaining());
            for(var shop:f.shops)assertFalse(shop.frozen());
            f.leave(0,frame);f.leave(1,frame);assertEquals(2,f.terminals.get());
        }
    }
    @Test void oldRegistrationCannotEnterItemBindingAndDoesNotCaptureAnyInventory() throws Exception {
        try(var f=new Fixture()) {
            var p=f.room.players.get(1);
            f.service.register(p.owner(),p.scope(),deadline->p.row(),deadline->f.fighter(1),true);
            var denied=f.prepare();assertEquals(Status.UNAVAILABLE,denied.status());assertEquals(Reason.INVALID_STATE,denied.reason());
            assertEquals(0,f.captures.get());assertEquals(0,f.service.matchCount());assertFalse(f.room.rooms.isPreparing(f.room.players.get(0).owner()));
            assertFalse(f.shops.get(0).frozen());
        }
    }
    @Test void terminalTombstoneWinsOverLateCaptureAndActualPreparationRemainsOwnedUntilReturn() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture()) {
            var p=f.room.players.get(0);var shop=f.shops.get(0);
            f.service.register(p.owner(),p.scope(),deadline->p.row(),deadline->f.fighter(0),true,null,false,new ModernWorldService.MatchItems() {
                public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline)throws Exception {
                    entered.countDown();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                    while(release.getCount()!=0 && System.nanoTime()<until)try{release.await(10,TimeUnit.MILLISECONDS);}catch(InterruptedException ignored){}
                    if(release.getCount()!=0)throw new IllegalStateException("fixture release timeout");
                    return shop.capture(match,generation,deadline,f.catalog(0));
                }
                public void terminal(long match,long generation){f.terminals.incrementAndGet();shop.terminal(match,generation);}
            });
            var preparing=f.prepare();assertEquals(Status.OK,preparing.status());assertTrue(entered.await(1,TimeUnit.SECONDS));
            f.service.close();assertFalse(f.service.awaitTermination(10));assertEquals(1,f.service.active());
            assertEquals(2,f.terminals.get());release.countDown();assertTrue(f.service.awaitTermination(3000));
            assertFalse(shop.frozen());assertEquals(0,f.service.active());assertEquals(0,f.service.registrationCount());
            var state=shop.apply(ModernShopAuthority.Action.READ,0,0,null,0,0,f.catalog(0));assertEquals(2,state.snapshot().roundId());assertTrue(state.snapshot().stacks().isEmpty());
        }finally{release.countDown();}
    }
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room=new ModernBattleEngineBotTest.RoomFixture(2,0);
        final ModernWorldService service;
        final List<ModernShopAuthority> shops;
        final AtomicInteger captures=new AtomicInteger(),terminals=new AtomicInteger(),persisted=new AtomicInteger();
        Fixture()throws Exception {
            shops=room.players.stream().map(p->new ModernShopAuthority(20000,p.scope().sourceId(),p.row(),"fixture-owner")).toList();
            var first=shops.get(0).apply(ModernShopAuthority.Action.READ,0,0,null,0,0,catalog(0)).snapshot();
            assertEquals(ModernShopAuthority.Status.OK,shops.get(0).apply(ModernShopAuthority.Action.BUY,first.roundId(),first.revision(),first.token(),40010,2,catalog(0)).status());
            var mapping=new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,0,false,false,true);
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),mapping),new ModernWorldService.Limits(2,1,0,2000,5,
                    new ModernBattleEngine.Limits(2000,0,10000)),bound->bound-1,(event,deadline)->persisted.incrementAndGet());
            for(int i=0;i<2;i++){final int index=i;var p=room.players.get(i);var shop=shops.get(i);
                service.register(p.owner(),p.scope(),deadline->p.row(),deadline->fighter(index),true,null,false,new ModernWorldService.MatchItems(){
                    public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline){captures.incrementAndGet();return shop.capture(match,generation,deadline,catalog(index));}
                    public void terminal(long match,long generation){terminals.incrementAndGet();shop.terminal(match,generation);}
                });
            }
        }
        ModernShopCatalog.Snapshot catalog(int i){var p=room.players.get(i);return new ModernShopCatalog.Snapshot(p.scope().sourceId(),"fixture-owner",p.row(),List.of(ModernShopCatalogTest.offer(40010,0)));}
        ModernPhysicalCombat.Fighter fighter(int i){var p=room.players.get(i);return ModernPhysicalCombatTest.createFighter(p.scope().sourceId(),p.row(),100,10,0);}
        Reply prepare(){var p=room.players.get(0);var s=room.view().summary();return service.handle(p.owner(),new Request(p.scope(),Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,"0".repeat(64)));}
        Frame read(){var p=room.players.get(0);return service.handle(p.owner(),new Request(p.scope(),Action.READ,0,0,0,0,0,0,0,0,"0".repeat(64))).frame();}
        Frame loading()throws Exception {assertEquals(Status.OK,prepare().status());await(()->{var v=read();assertNotEquals(Phase.ABORTED,v.phase());return v.phase()==Phase.LOADING;});return read();}
        ModernCombatProtocol.Frame combat(int i){var p=room.players.get(i);var frame=read();return service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,frame.matchId(),frame.generation(),0,0)).frame();}
        void loaded(int i,Frame frame){var p=room.players.get(i);assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LOADED,0,0,0,frame.matchId(),frame.generation(),0,0,0,frame.manifest().digest())).status());}
        void leave(int i,Frame frame){var p=room.players.get(i);assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LEAVE,0,0,0,frame.matchId(),frame.generation(),0,0,0,"0".repeat(64))).status());}
        @Override public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));for(var s:shops)s.close();room.close();}
    }
    private static void await(BooleanSupplier condition)throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!condition.getAsBoolean()){assertTrue(System.nanoTime()<end);Thread.sleep(5);}}
}
