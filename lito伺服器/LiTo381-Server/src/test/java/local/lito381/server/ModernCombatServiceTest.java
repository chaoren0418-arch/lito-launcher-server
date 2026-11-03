package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class ModernCombatServiceTest {
    private static final String ZERO = "0".repeat(64);
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture room = new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a = room.player(7, 3333, 80, 0), b = room.player(42, 3333, 80, 0);
        final ModernWorldService service;
        final AtomicInteger physicalReads = new AtomicInteger(), rosterReads = new AtomicInteger();
        Fixture() throws Exception {
            var map = ModernWorldMap.fromSectorBytes(514, Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),
                    ModernWorldMapTest.openSectorBytes()), () -> false);
            var binding = new ModernWorldBinding(1,514,map);
            service = new ModernWorldService(room.rooms, Map.of(binding.key(), new ModernWorldService.Mapping(binding,
                    new ModernWorldMap.Cell(32778,32778), 1_000_000,1_000_000)),
                    new ModernWorldService.Limits(2,1,1,2000,5,new ModernBattleEngine.Limits(2000,0,10000)));
            ModernRoomsTest.accepted(room.create(a,ModernRoomsTest.draft(100,1,514,0)));
            ModernRoomsTest.accepted(room.join(b,room.view(a).joined().summary(),""));
            for (var player : List.of(a,b)) {
                service.register(player.owner(),player.scope(), deadline -> { rosterReads.incrementAndGet(); return row(player); },
                        deadline -> { assertTrue(deadline-System.nanoTime()>0); physicalReads.incrementAndGet(); return fighter(player); });
                ModernRoomsTest.accepted(room.act(player,ModernRooms.Action.READY,1));
            }
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p) {
            var s=room.presence.selected(p.owner());
            return new ModernApplicationProtocol.CharacterRow(s.key().objectId(),s.characterName(),s.characterClass(),s.sex(),s.level());
        }
        ModernPhysicalCombat.Fighter fighter(ModernRoomsTest.Player p) { return ModernPhysicalCombatTest.createFighter(p.scope().sourceId(),row(p),20,10,0); }
        ModernWorldProtocol.Reply prepare() {
            var s=room.view(a).joined().summary();
            return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO));
        }
        ModernWorldProtocol.Frame read() { return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame(); }
        ModernWorldProtocol.Frame active() throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,prepare().status());
            await(() -> read().phase()!=ModernWorldProtocol.Phase.PREPARING);
            var f=read(); assertEquals(ModernWorldProtocol.Phase.LOADING,f.phase());
            for(var p:List.of(a,b)) service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,f.matchId(),f.generation(),0,0,0,f.manifest().digest()));
            assertEquals(ModernWorldProtocol.Phase.ACTIVE,read().phase());return read();
        }
        ModernCombatProtocol.Request request(ModernRoomsTest.Player p,ModernWorldProtocol.Frame f,long seq,long target) {
            return new ModernCombatProtocol.Request(p.scope(),seq==0?ModernCombatProtocol.Action.READ:ModernCombatProtocol.Action.ATTACK,f.matchId(),f.generation(),seq,target);
        }
        @Override public void close() throws Exception {
            service.close();assertTrue(service.awaitTermination(4000));
            for(var p:List.of(a,b)){room.rooms.disconnect(p.owner());room.presence.disconnect(p.owner());}
        }
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!condition.getAsBoolean()){assertTrue(System.nanoTime()-deadline<0);Thread.sleep(2);}
    }
    private static void wire(ModernCombatProtocol.Request request,ModernCombatProtocol.Reply reply) throws Exception {
        var message=new ModernApplicationProtocol.CombatResult(88,request,reply);
        byte[] encoded=ModernApplicationProtocol.encode(message);
        var legacy=ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,ModernSessionProtocol.VERSION,1,255));
        try(var decoded=ModernApplicationProtocol.decode(encoded)){
            assertEquals(legacy,decoded);
            var frame=((ModernApplicationProtocol.CombatResult)decoded).reply().frame();
            if(frame!=null)assertNull(frame.world().descriptor());
        }
        finally{java.util.Arrays.fill(encoded,(byte)0);}
        for(int version:new int[]{2,15}) {
            var projected=ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,version,1,
                    255|(version==15?ModernSessionProtocol.CAP_MATCH_DESCRIPTOR:0)));
            byte[] versioned=ModernApplicationProtocol.encode(projected,version);
            try(var decoded=ModernApplicationProtocol.decode(versioned,version)) {
                assertEquals(version==15?message:legacy,decoded);
                var frame=((ModernApplicationProtocol.CombatResult)decoded).reply().frame();
                if(frame!=null) {
                    if(version==15){assertNotNull(frame.world().descriptor());assertEquals(reply.frame().world().descriptor(),frame.world().descriptor());}
                    else assertNull(frame.world().descriptor());
                }
            }finally{java.util.Arrays.fill(versioned,(byte)0);}
        }
    }
    @Test void freshQualifiedProfileAndAtomicDamageReachWireAndLeaveReleasesRoom() throws Exception {
        try(var f=new Fixture()) {
            var world=f.active(); assertTrue(f.service.combatEnabled());assertEquals(2,f.physicalReads.get());assertEquals(0,f.rosterReads.get());
            var read=f.request(f.a,world,0,0);var initial=f.service.combat(f.a.owner(),read);wire(read,initial);
            assertEquals(world,initial.frame().world());assertEquals(20,initial.frame().vitals().get(1).hp());
            ModernCombatProtocol.Reply last=initial;
            for(long seq=1;seq<=40 && last.frame().vitals().get(1).hp()>0;seq++) {
                Thread.sleep(2);var request=f.request(f.a,world,seq,2);last=f.service.combat(f.a.owner(),request);wire(request,last);
                assertEquals(seq,last.frame().vitals().get(0).attackSequence());
                assertEquals(ModernCombatProtocol.Status.OK,last.status());
                assertEquals(last.frame().world().revision(),last.frame().vitals().get(0).attackRevision());
            }
            assertEquals(0,last.frame().vitals().get(1).hp());assertTrue(last.frame().vitals().get(1).deathRevision()>0);
            var denied=f.request(f.b,world,1,1);assertEquals(ModernCombatProtocol.Reason.DEAD,f.service.combat(f.b.owner(),denied).reason());
            var before=f.service.combat(f.a.owner(),read).frame();
            var actor=before.world().actors().get(1);
            var moved=f.service.handle(f.b.owner(),new ModernWorldProtocol.Request(f.b.scope(),ModernWorldProtocol.Action.MOVE,2,0,0,world.matchId(),world.generation(),1,actor.position().x(),actor.position().y(),ZERO));
            assertEquals(ModernWorldProtocol.Reason.NOT_ACTIVE,moved.reason());
            var after=f.service.combat(f.a.owner(),read).frame();assertEquals(moved.frame(),after.world());assertEquals(0,after.vitals().get(1).hp());
            var left=f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,world.matchId(),world.generation(),0,0,0,ZERO));
            assertNull(left.frame());assertFalse(f.room.view(f.a).joined().summary().readySatisfied());
            var terminal=f.service.combat(f.b.owner(),f.request(f.b,world,0,0));
            assertEquals(ModernWorldProtocol.Phase.ABORTED,terminal.frame().world().phase());assertEquals(0,terminal.frame().vitals().get(1).hp());
            assertThrows(SecurityException.class,()->f.service.combat(f.a.owner(),read));
        }
    }
    @Test void legacyParticipantCannotSilentlyEnterACombatMapping() throws Exception {
        try(var f=new Fixture()) {
            f.service.register(f.b.owner(),f.b.scope(),deadline->{fail("must not fallback to legacy roster");return null;});
            var result=f.prepare();assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,result.status());assertNull(result.frame());
            assertEquals(0,f.physicalReads.get());assertFalse(f.room.view(f.a).joined().summary().readySatisfied());
        }
    }
    @Test void blockedPhysicalReadPublishesNoFakeVitalsAndCancellationCannotPublishLate() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture()) {
            f.service.register(f.a.owner(),f.a.scope(),deadline->{fail("no roster fallback");return null;},deadline->{
                entered.countDown();boolean interrupted=false;
                for(;;)try{assertTrue(release.await(3,TimeUnit.SECONDS));break;}catch(InterruptedException e){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();return f.fighter(f.a);
            });
            var preparing=f.prepare().frame();assertTrue(entered.await(1,TimeUnit.SECONDS));
            var request=f.request(f.a,preparing,0,0);var reply=f.service.combat(f.a.owner(),request);
            assertEquals(ModernCombatProtocol.Status.UNAVAILABLE,reply.status());assertNull(reply.frame());wire(request,reply);
            f.service.detach(f.a.owner());assertEquals(1,f.service.active());release.countDown();
            await(()->f.service.active()==0);assertThrows(SecurityException.class,()->f.service.combat(f.a.owner(),request));
            var remaining=f.service.combat(f.b.owner(),f.request(f.b,preparing,0,0));assertNull(remaining.frame());
        }finally{release.countDown();}
    }
    @Test void wrongSourceEpochOwnerAndGenerationCannotReadOrMutateAnotherWorld() throws Exception {
        try(var f=new Fixture()) {
            var world=f.active();var read=f.request(f.a,world,0,0);var before=f.service.combat(f.a.owner(),read);
            assertThrows(SecurityException.class,()->f.service.combat(f.b.owner(),read));
            var wrong=new ModernRooms.Scope(7,3333,99,0);
            assertThrows(SecurityException.class,()->f.service.combat(f.a.owner(),new ModernCombatProtocol.Request(wrong,ModernCombatProtocol.Action.ATTACK,world.matchId(),world.generation(),1,2)));
            assertThrows(SecurityException.class,()->f.service.combat(f.a.owner(),new ModernCombatProtocol.Request(f.a.scope(),ModernCombatProtocol.Action.ATTACK,world.matchId(),world.generation()+1,1,2)));
            assertEquals(before,f.service.combat(f.a.owner(),read));
        }
    }
}
