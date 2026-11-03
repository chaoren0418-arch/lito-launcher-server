package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernWorldClockServiceTest {
    static final String ZERO="0".repeat(64);
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=rooms.player(7,3333,80,0),b=rooms.player(42,3333,80,0);
        final AtomicInteger reads=new AtomicInteger();final AtomicLong sourceDeadline=new AtomicLong();
        final ModernWorldService service;final int preparation;
        Fixture(int preparation,ModernWorldService.ClockOverrides first,ModernWorldService.ClockOverrides second)throws Exception{
            this.preparation=preparation;var initial=ModernWorldClockTest.mapping();
            var other=new ModernWorldService.Mapping(new ModernWorldBinding(1,509,initial.binding().map()),initial.anchor(),initial.stepIntervalNanos());
            service=new ModernWorldService(rooms.rooms,Map.of(initial.binding().key(),initial.withClockOverrides(first),other.binding().key(),other.withClockOverrides(second)),
                    new ModernWorldService.Limits(4,1,1,preparation,5,new ModernBattleEngine.Limits(2000,3_000_000_000L,45000)),bound->{throw new AssertionError("Scene clock cannot consume RNG");});
            for(var p:List.of(a,b))service.register(p.owner(),p.scope(),deadline->{
                reads.incrementAndGet();long previous=sourceDeadline.get();if(previous==0)sourceDeadline.set(deadline);else assertEquals(previous,deadline,"one original deadline per preparation");
                return row(p);
            },null,false,null,false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,15,1,65903));
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p){var s=rooms.presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(3333,s.characterName(),s.characterClass(),s.sex(),80);}
        Reply prepare(){sourceDeadline.set(0);for(var p:List.of(a,b))ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));var s=rooms.view(a).joined().summary();return service.handle(a.owner(),new Request(a.scope(),Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO));}
        Frame read(){return service.handle(a.owner(),new Request(a.scope(),Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();}
        Frame waitPhase(Phase target)throws Exception{long deadline=System.nanoTime()+3_000_000_000L;for(;;){var frame=read();if(frame.phase()==target)return frame;assertTrue(System.nanoTime()-deadline<0,"phase="+frame.phase()+" abort="+frame.abortReason());Thread.sleep(2);}}
        Reply ack(ModernRoomsTest.Player p,Frame f){return service.handle(p.owner(),new Request(p.scope(),Action.LOADED,0,0,0,f.matchId(),f.generation(),0,0,0,f.manifest().digest()));}
        void leave(Frame f){for(var p:List.of(a,b))assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LEAVE,0,0,0,f.matchId(),f.generation(),0,0,0,ZERO)).status());}
        public void close()throws Exception{service.close();assertTrue(service.awaitTermination(4000));for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}assertTrue(service.isQuiescent());assertEquals(0,service.registrationCount());}
    }
    @Test void sameOwnersUseDifferentFrozenClocksThroughActualEngineTimeoutsWithoutResults()throws Exception{
        try(var f=new Fixture(2000,new ModernWorldService.ClockOverrides(null,100_000_001L,150),new ModernWorldService.ClockOverrides(1500,200_000_001L,250))){
            long previousMatch=0,previousGeneration=0;
            for(int map:new int[]{514,509}){
                if(map==509)ModernRoomsTest.accepted(f.rooms.act(f.a,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(2,1,509,0)));
                var initial=f.prepare();assertEquals(Status.OK,initial.status());assertEquals(Phase.PREPARING,initial.frame().phase());
                var descriptor=initial.frame().descriptor();assertEquals(2000,descriptor.preparationTimeoutMillis());assertEquals(0,descriptor.features());
                assertEquals(map==514?2000:1500,descriptor.loadTimeoutMillis());assertEquals(map==514?100_000_001L:200_000_001L,descriptor.countdownNanos());
                assertEquals(map==514?150:250,descriptor.activeTimeoutMillis());assertFalse(descriptor.settlementEnabled());
                assertTrue(initial.frame().matchId()>previousMatch);assertNotEquals(previousGeneration,initial.frame().generation());
                var loading=f.waitPhase(Phase.LOADING);assertEquals(descriptor,loading.descriptor());assertEquals(Phase.LOADING,f.ack(f.a,loading).frame().phase());
                long beforeAck=System.nanoTime();var ack=f.ack(f.b,loading);assertEquals(Status.OK,ack.status());
                assertEquals(descriptor,ack.frame().descriptor());
                var active=f.waitPhase(Phase.ACTIVE);assertEquals(descriptor,active.descriptor());
                var terminal=f.waitPhase(Phase.ABORTED);assertEquals(AbortReason.ACTIVE_TIMEOUT,terminal.abortReason());assertNull(terminal.completion());assertEquals(descriptor,terminal.descriptor());
                assertTrue(System.nanoTime()-beforeAck>=descriptor.countdownNanos()+TimeUnit.MILLISECONDS.toNanos(descriptor.activeTimeoutMillis()));
                assertEquals(terminal,f.read());previousMatch=terminal.matchId();previousGeneration=terminal.generation();f.leave(terminal);
            }
            assertEquals(4,f.reads.get());
        }
    }
    @Test void localLoadTimeoutIsEnforcedWithoutWaitingForGlobalFallback()throws Exception{
        try(var f=new Fixture(2000,new ModernWorldService.ClockOverrides(250,0L,50),null)){
            long started=System.nanoTime();var initial=f.prepare();assertEquals(Status.OK,initial.status());var terminal=f.waitPhase(Phase.ABORTED);
            long elapsed=System.nanoTime()-started;
            assertTrue(elapsed>=TimeUnit.MILLISECONDS.toNanos(250),"local load duration must not fire early");
            assertTrue(elapsed<TimeUnit.MILLISECONDS.toNanos(2000),"terminal must precede the global load duration, not merely describe an override");
            assertEquals(AbortReason.LOAD_TIMEOUT,terminal.abortReason());assertEquals(250,terminal.descriptor().loadTimeoutMillis());assertNull(terminal.completion());
            assertTrue(terminal.actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));f.leave(terminal);
        }
    }
    @Test void longLocalPhaseClocksNeverExtendGlobalPreparationOrPublishAfterCancellation()throws Exception{
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture(200,new ModernWorldService.ClockOverrides(30000,30_000_000_000L,1800000),null)){
            f.service.register(f.a.owner(),f.a.scope(),deadline->{entered.countDown();boolean interrupted=false;
                try{while(release.getCount()!=0)try{if(!release.await(3,TimeUnit.SECONDS))throw new IllegalStateException("bounded test reader");}catch(InterruptedException e){interrupted=true;}
                    return f.row(f.a);
                }finally{if(interrupted)Thread.currentThread().interrupt();}
            });
            var reply=f.prepare();assertEquals(Status.OK,reply.status());assertEquals(200,reply.frame().descriptor().preparationTimeoutMillis());assertTrue(entered.await(1,TimeUnit.SECONDS));
            var terminal=f.waitPhase(Phase.ABORTED);assertEquals(AbortReason.LOAD_TIMEOUT,terminal.abortReason());assertNull(terminal.completion());
            release.countDown();assertEquals(terminal,f.read());f.leave(terminal);
        }finally{release.countDown();}
    }
}
