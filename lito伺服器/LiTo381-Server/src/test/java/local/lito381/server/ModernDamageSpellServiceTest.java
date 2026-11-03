package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Actual service/preparation/receipt ownership, synthetic trusted source facts; no SQL or sockets. */
class ModernDamageSpellServiceTest {
    private static final String ZERO="0".repeat(64);
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=rooms.player(7,3333,80,0),b=rooms.player(42,3333,80,0);
        final AtomicInteger reads=new AtomicInteger();
        final ModernWorldService service;
        final long hp;
        Fixture(boolean bV6,long hp,ModernMatchSettlement.Store store) throws Exception {
            this.hp=hp;
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);
            service=new ModernWorldService(rooms.rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,
                    new ModernWorldMap.Cell(32778,32778),1_000_000,1_000_000,10_000_000,true)),
                    new ModernWorldService.Limits(2,1,1,2000,5,new ModernBattleEngine.Limits(2000,0,10000)),bound->0,store);
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));
            ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
            register(a,true,true);register(b,bV6,true);
            for(var p:List.of(a,b))ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p) {
            var s=rooms.presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(s.key().objectId(),s.characterName(),s.characterClass(),s.sex(),s.level());
        }
        void register(ModernRoomsTest.Player p,boolean v6,boolean magicFacts) {
            service.register(p.owner(),p.scope(),deadline->{fail("physical preparation must share its exact profile");return null;},
                    deadline->{reads.incrementAndGet();return magicFacts?ModernMagicCombatTest.createFighter(p.scope().sourceId(),row(p),hp,30,12,0)
                            :ModernPhysicalCombatTest.createFighter(p.scope().sourceId(),row(p),hp,10,0);},true,
                    deadline->new ModernSkills.Snapshot(p.scope().sourceId(),row(p),List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW))),v6);
        }
        ModernWorldProtocol.Reply prepare() {
            var r=rooms.view(a).joined().summary();return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,r.id(),r.revision(),0,0,0,0,0,ZERO));
        }
        ModernWorldProtocol.Frame world(ModernRoomsTest.Player p) {
            return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();
        }
        ModernWorldProtocol.Frame active() throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,prepare().status());
            await(()->world(a).phase()!=ModernWorldProtocol.Phase.PREPARING);var frame=world(a);
            assertEquals(ModernWorldProtocol.Phase.LOADING,frame.phase());
            for(var p:List.of(a,b))service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,
                    frame.matchId(),frame.generation(),0,0,0,frame.manifest().digest()));
            return world(a);
        }
        ModernCombatProtocol.Frame combat(ModernRoomsTest.Player p,ModernWorldProtocol.Frame f) {
            return service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,f.matchId(),f.generation(),0,0)).frame();
        }
        ModernSpellProtocol.Request request(ModernWorldProtocol.Frame f) {return new ModernSpellProtocol.Request(a.scope(),f.matchId(),f.generation(),1,4,2);}
        @Override public void close() throws Exception {
            service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());
            for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}
            assertEquals(0,rooms.rooms.memberCount());assertEquals(0,rooms.presence.ownerCount());
        }
    }
    private static void await(BooleanSupplier ready) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!ready.getAsBoolean()){assertTrue(System.nanoTime()-end<0,"bounded service state");Thread.sleep(2);}
    }
    private static void wire(ModernApplicationProtocol.Message message) throws Exception {
        for(int version:new int[]{6,15}) {
            var projected=ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,version,1,
                    255|(version==15?ModernSessionProtocol.CAP_MATCH_DESCRIPTOR:0)));
            byte[] encoded=ModernApplicationProtocol.encode(message,version);
            try(var decoded=ModernApplicationProtocol.decode(encoded,version)) {
                assertEquals(projected,decoded);
                var frame=decoded instanceof ModernApplicationProtocol.CastResult cast?cast.reply().frame():
                        ((ModernApplicationProtocol.CombatResult)decoded).reply().frame();
                if(version==15){assertEquals(message,decoded);assertNotNull(frame.world().descriptor());}
                else assertNull(frame.world().descriptor());
            }finally{java.util.Arrays.fill(encoded,(byte)0);}
        }
    }
    @Test void everyParticipantMustNegotiateV6BeforeAnySourcePreparation() throws Exception {
        try(var f=new Fixture(false,100,null)) {
            var denied=f.prepare();assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,denied.status());assertNull(denied.frame());
            assertEquals(0,f.reads.get());assertFalse(f.rooms.rooms.isPreparing(f.a.owner()));
        }
    }
    @Test void v6WithoutQualifiedGrowthCannotPublishLoadingOrInventMagicStats() throws Exception {
        try(var f=new Fixture(true,100,null)) {
            f.register(f.b,true,false);assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());
            await(()->f.world(f.a).phase()!=ModernWorldProtocol.Phase.PREPARING);
            assertEquals(ModernWorldProtocol.Phase.ABORTED,f.world(f.a).phase());
            assertNull(f.combat(f.a,f.world(f.a)));
        }
    }
    @Test void lethalCastExactResponseAndAsynchronousSavedReceiptKeepDamageAndTeleportHistories() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var written=new AtomicReference<ModernMatchSettlement.Event>();
        try(var f=new Fixture(true,5,(event,deadline)->{
            assertTrue(written.compareAndSet(null,event));entered.countDown();
            if(!release.await(3,TimeUnit.SECONDS))throw new IllegalStateException("bounded writer barrier");
        })) {
            var active=f.active();assertEquals(ModernWorldProtocol.Phase.ACTIVE,active.phase());
            var initial=f.combat(f.a,active);assertEquals(2,initial.damages().size());assertEquals(0,initial.damages().get(0).castCount());
            var request=f.request(active);var reply=f.service.cast(f.a.owner(),request);
            assertEquals(ModernSpellProtocol.Status.OK,reply.status());wire(new ModernApplicationProtocol.CastResult(17,request,reply));
            var killed=reply.frame();assertEquals(ModernWorldProtocol.Phase.FINISHED,killed.world().phase());
            assertEquals(0,killed.vitals().get(1).hp());assertEquals(27,killed.vitals().get(0).mp());
            assertEquals(0,killed.vitals().get(0).attackRevision());assertEquals(1,killed.damages().get(0).castCount());
            assertEquals(killed.world().revision(),killed.damages().get(0).castRevision());
            assertTrue(entered.await(1,TimeUnit.SECONDS));assertEquals(ModernWorldProtocol.SettlementState.PENDING,killed.world().completion().state());
            var pending=f.combat(f.b,killed.world());assertEquals(killed.damages(),pending.damages());
            release.countDown();await(()->f.world(f.a).completion().state()==ModernWorldProtocol.SettlementState.SAVED);
            var saved=f.combat(f.a,killed.world());
            assertEquals(killed.vitals(),saved.vitals());assertEquals(killed.damages(),saved.damages());assertEquals(killed.world().casts(),saved.world().casts());
            assertEquals(written.get(),saved.world().completion().event());assertTrue(saved.world().revision()>killed.world().revision());
            var denied=f.service.cast(f.a.owner(),new ModernSpellProtocol.Request(f.a.scope(),active.matchId(),active.generation(),2,4,2));
            assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,denied.reason());assertEquals(saved,denied.frame());
            var rows=written.get().participants();assertEquals(List.of(7,42),rows.stream().map(p->p.key().sourceId()).toList());
            assertEquals(List.of(3333L,3333L),rows.stream().map(p->p.key().objectId()).toList());
        } finally {release.countDown();}
    }
    @Test void detachAfterNonlethalMagicPreservesConfirmedMpAndHistoryForSurvivingPeer() throws Exception {
        try(var f=new Fixture(true,100,null)) {
            var active=f.active();var cast=f.service.cast(f.a.owner(),f.request(active));assertEquals(ModernSpellProtocol.Status.OK,cast.status());
            f.service.detach(f.a.owner());var terminal=f.combat(f.b,active);
            assertEquals(ModernWorldProtocol.Phase.ABORTED,terminal.world().phase());assertEquals(cast.frame().vitals(),terminal.vitals());
            assertEquals(cast.frame().damages(),terminal.damages());assertEquals(cast.frame().world().casts(),terminal.world().casts());
            wire(new ModernApplicationProtocol.CombatResult(17,new ModernCombatProtocol.Request(f.b.scope(),ModernCombatProtocol.Action.READ,
                    active.matchId(),active.generation(),0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,terminal)));
        }
    }
}
