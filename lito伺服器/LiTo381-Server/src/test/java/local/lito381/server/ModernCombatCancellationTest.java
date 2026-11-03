package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Deliberately holds the real Engine Guard inside trusted server RNG, never a guessed sleep race. */
class ModernCombatCancellationTest {
    @Test void guestLeaveWaitsForAdmittedAttackAndPublishesLaterTerminalWithItsTrueHp() throws Exception { race(false); }
    @Test void guestDetachWaitsForAdmittedAttackAndPublishesLaterTerminalWithItsTrueHp() throws Exception { race(true); }

    private static void race(boolean detach) throws Exception {
        var random=new BlockingRandom();var threads=Executors.newFixedThreadPool(3);
        try(var f=new Fixture(random)) {
            var world=f.active();var read=f.combatRequest(0,0);var before=f.service.combat(f.a.owner(),read).frame();
            Future<ModernCombatProtocol.Reply> attack=threads.submit(()->f.service.combat(f.a.owner(),f.combatRequest(1,2)));
            assertTrue(random.entered.await(3,TimeUnit.SECONDS),"attack did not acquire the real Engine Guard");
            Future<?> cancellation=threads.submit(()->{
                if(detach)f.service.detach(f.b.owner());
                else {
                    var reply=f.service.handle(f.b.owner(),f.request(f.b,ModernWorldProtocol.Action.LEAVE,world));
                    assertEquals(ModernWorldProtocol.Status.OK,reply.status());assertNull(reply.frame());
                }
            });
            // Private fields are ONLY observed under the actual service gate.
            // We do not replace an engine, mutate its state, or fabricate a ticket.
            await(()->f.terminationAdmissionFenced());
            assertFalse(cancellation.isDone(),"LEAVE/detach acknowledged while an admitted Guard was still live");
            var readerEntered=new CountDownLatch(1);
            Future<ModernCombatProtocol.Reply> terminalRead=threads.submit(()->{readerEntered.countDown();return f.service.combat(f.a.owner(),read);});
            assertTrue(readerEntered.await(2,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->terminalRead.get(100,TimeUnit.MILLISECONDS),
                    "a terminal reader must not return the cached pre-attack HP while retirement waits");
            random.release.countDown();
            var acknowledgement=attack.get(3,TimeUnit.SECONDS);cancellation.get(3,TimeUnit.SECONDS);
            var terminal=terminalRead.get(3,TimeUnit.SECONDS);
            assertEquals(ModernCombatProtocol.Status.OK,acknowledgement.status());
            assertEquals(ModernWorldProtocol.Phase.ACTIVE,acknowledgement.frame().world().phase());
            assertEquals(ModernCombatProtocol.Status.OK,terminal.status());
            assertEquals(ModernWorldProtocol.Phase.ABORTED,terminal.frame().world().phase());
            assertEquals(detach?ModernWorldProtocol.AbortReason.DISCONNECTED:ModernWorldProtocol.AbortReason.SERVER_ABORT,
                    terminal.frame().world().abortReason());
            assertTrue(acknowledgement.frame().world().revision()>before.world().revision());
            assertTrue(terminal.frame().world().revision()>acknowledgement.frame().world().revision(),
                    "ACTIVE ACK and terminal must never have the same revision with different HP/state");
            assertTrue(acknowledgement.frame().vitals().get(1).hp()<before.vitals().get(1).hp());
            assertEquals(acknowledgement.frame().vitals(),terminal.frame().vitals(),"terminal must include the admitted hit, not cached HP");
            assertEquals(acknowledgement.frame().world().actors(),terminal.frame().world().actors());
            assertEquals(terminal,f.service.combat(f.a.owner(),read));
            assertFalse(f.room.view(f.a).joined().summary().readySatisfied());
            wire(f.combatRequest(1,2),acknowledgement);wire(read,terminal);
        } finally {
            random.release.countDown();threads.shutdownNow();assertTrue(threads.awaitTermination(5,TimeUnit.SECONDS));
        }
    }
    private static void wire(ModernCombatProtocol.Request request,ModernCombatProtocol.Reply reply)throws Exception {
        var message=new ModernApplicationProtocol.CombatResult(333,request,reply);byte[] encoded=ModernApplicationProtocol.encode(message);
        var legacy=ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,ModernSessionProtocol.VERSION,1,255));
        try(var decoded=ModernApplicationProtocol.decode(encoded)){
            assertEquals(legacy,decoded);
            assertNull(((ModernApplicationProtocol.CombatResult)decoded).reply().frame().world().descriptor());
        }
        finally{java.util.Arrays.fill(encoded,(byte)0);}
        for(int version:new int[]{2,15}) {
            var projected=ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,version,1,
                    255|(version==15?ModernSessionProtocol.CAP_MATCH_DESCRIPTOR:0)));
            byte[] versioned=ModernApplicationProtocol.encode(projected,version);
            try(var decoded=ModernApplicationProtocol.decode(versioned,version)) {
                assertEquals(version==15?message:legacy,decoded);
                var descriptor=((ModernApplicationProtocol.CombatResult)decoded).reply().frame().world().descriptor();
                if(version==15){assertNotNull(descriptor);assertEquals(reply.frame().world().descriptor(),descriptor);}
                else assertNull(descriptor);
            }finally{java.util.Arrays.fill(versioned,(byte)0);}
        }
    }
    private static void await(BooleanSupplier condition)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!condition.getAsBoolean()){assertTrue(System.nanoTime()-deadline<0,"bounded barrier was not reached");Thread.sleep(2);}
    }
    private static final class BlockingRandom implements ModernPhysicalCombat.BoundedRandom {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        boolean first=true;
        @Override public long nextLong(long bound) {
            if(first) {
                first=false;entered.countDown();boolean interrupted=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                try {
                    while(release.getCount()!=0) {
                        long remaining=deadline-System.nanoTime();assertTrue(remaining>0,"test RNG barrier expired");
                        try{assertTrue(release.await(remaining,TimeUnit.NANOSECONDS),"test RNG barrier expired");}
                        catch(InterruptedException ignored){interrupted=true;}
                    }
                }finally{if(interrupted)Thread.currentThread().interrupt();}
            }
            return 0; // Legal nonconstant damage still derives from the qualified unarmed/class formula.
        }
    }
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture room=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=room.player(7,3333,30,0),b=room.player(42,3333,30,0);
        final ModernWorldService service;final BlockingRandom random;ModernWorldProtocol.Frame world;
        Fixture(BlockingRandom random)throws Exception {
            this.random=random;
            var key=new ModernWorldMap.SectorKey(0x8000,0x8000);
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(key,ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);
            service=new ModernWorldService(room.rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,
                    new ModernWorldMap.Cell(32778,32778),200_000_000,700_000_000)),
                    new ModernWorldService.Limits(2,1,1,3000,5,new ModernBattleEngine.Limits(10_000,0,60_000)),random);
            ModernRoomsTest.accepted(room.create(a,ModernRoomsTest.draft(100,1,514,0)));
            ModernRoomsTest.accepted(room.join(b,room.view(a).joined().summary(),""));
            for(var p:List.of(a,b)) {
                var selected=room.presence.selected(p.owner());
                var row=new ModernApplicationProtocol.CharacterRow(selected.key().objectId(),selected.characterName(),selected.characterClass(),selected.sex(),selected.level());
                service.register(p.owner(),p.scope(),deadline->row,
                        deadline->{assertTrue(deadline-System.nanoTime()>0);return ModernPhysicalCombatTest.createFighter(p.scope().sourceId(),row,100,10,0);});
                ModernRoomsTest.accepted(room.act(p,ModernRooms.Action.READY,1));
            }
        }
        ModernWorldProtocol.Frame active()throws Exception {
            var summary=room.view(a).joined().summary();
            var preparing=service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,
                    summary.id(),summary.revision(),0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
            assertEquals(ModernWorldProtocol.Status.OK,preparing.status());world=preparing.frame();
            await(()->{world=service.handle(a.owner(),request(a,ModernWorldProtocol.Action.READ,world)).frame();return world.phase()!=ModernWorldProtocol.Phase.PREPARING;});
            assertEquals(ModernWorldProtocol.Phase.LOADING,world.phase());
            for(var p:List.of(a,b))service.handle(p.owner(),request(p,ModernWorldProtocol.Action.LOADED,world));
            world=service.handle(a.owner(),request(a,ModernWorldProtocol.Action.READ,world)).frame();assertEquals(ModernWorldProtocol.Phase.ACTIVE,world.phase());
            return world;
        }
        ModernWorldProtocol.Request request(ModernRoomsTest.Player p,ModernWorldProtocol.Action action,ModernWorldProtocol.Frame frame) {
            return new ModernWorldProtocol.Request(p.scope(),action,0,0,0,frame.matchId(),frame.generation(),0,0,0,
                    action==ModernWorldProtocol.Action.LOADED?frame.manifest().digest():ModernWorldProtocol.ZERO_DIGEST);
        }
        ModernCombatProtocol.Request combatRequest(long sequence,long target) {
            return new ModernCombatProtocol.Request(a.scope(),sequence==0?ModernCombatProtocol.Action.READ:ModernCombatProtocol.Action.ATTACK,
                    world.matchId(),world.generation(),sequence,target);
        }
        boolean terminationAdmissionFenced() {
            try {
                Field gateField=ModernWorldService.class.getDeclaredField("gate");gateField.setAccessible(true);Object gate=gateField.get(service);
                synchronized(gate) {
                    Field entriesField=ModernWorldService.class.getDeclaredField("entries");entriesField.setAccessible(true);
                    var entries=(Set<?>)entriesField.get(service);assertEquals(1,entries.size());Object entry=entries.iterator().next();
                    Field terminal=entry.getClass().getDeclaredField("terminal");terminal.setAccessible(true);return terminal.getBoolean(entry);
                }
            }catch(ReflectiveOperationException failure){throw new AssertionError("test-only observation failed",failure);}
        }
        @Override public void close()throws Exception {
            random.release.countDown();service.close();assertTrue(service.awaitTermination(5000));
            for(var p:List.of(a,b)){room.rooms.disconnect(p.owner());room.presence.disconnect(p.owner());}
            assertTrue(service.isQuiescent());
        }
    }
}
