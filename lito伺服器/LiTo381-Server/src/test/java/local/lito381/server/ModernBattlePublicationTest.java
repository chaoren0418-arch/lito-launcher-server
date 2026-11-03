package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual service publication, command serialization and journal fences. No UI
 * or new negotiated wire acceptance is inferred from these server tests. */
class ModernBattlePublicationTest {
    @TempDir Path temporary;
    static Object field(Object o,String name)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static Object entry(ModernWorldService s,TournamentPresence.Owner owner)throws Exception {
        synchronized(field(s,"gate")){return field(((Map<?,?>)field(s,"registrations")).get(owner),"entry");}
    }
    static void stopScheduler(ModernWorldService s)throws Exception {var scheduler=(ScheduledThreadPoolExecutor)field(s,"scheduler");scheduler.shutdownNow();assertTrue(scheduler.awaitTermination(3,TimeUnit.SECONDS));}
    record Cache(long revision,ModernWorldProtocol.Frame world,ModernCombatProtocol.Frame combat,ModernNpcProtocol.Frame npc,ModernBattlePublication authority) {}
    static Object legacy(Object entry,String name)throws Exception {var method=entry.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(entry);}
    static Cache cache(ModernWorldService s,Object entry)throws Exception {
        synchronized(field(s,"gate")){return new Cache((long)field(entry,"engineRevision"),(ModernWorldProtocol.Frame)legacy(entry,"frame"),
                (ModernCombatProtocol.Frame)legacy(entry,"combat"),(ModernNpcProtocol.Frame)legacy(entry,"npc"),(ModernBattlePublication)field(entry,"publication"));}
    }
    static void unchanged(Cache before,Cache after) {
        assertEquals(before.revision(),after.revision());assertSame(before.world(),after.world());assertSame(before.combat(),after.combat());
        assertSame(before.npc(),after.npc());assertSame(before.authority(),after.authority());
    }
    static void consistent(Cache c) {
        assertNotNull(c.authority());assertEquals(c.revision(),c.authority().world().revision());assertTrue(c.world().revision()>c.revision());
        assertEquals(c.world().matchId(),c.authority().world().identity().matchId());assertEquals(c.world().generation(),c.authority().world().identity().generation());
        if(c.combat()!=null)assertSame(c.world(),c.combat().world());if(c.npc()!=null)assertEquals(c.combat(),c.npc().combat());
        for(var actor:c.authority().world().actors())if(actor.vital()!=null) {
            var v=c.combat().vitals().get(Math.toIntExact(actor.pose().entityId()-1));assertEquals(actor.vital().values().hp(),v.hp());assertEquals(actor.vital().values().mp(),v.mp());
        }
    }
    static void publish(ModernWorldService s,Object entry,ModernBattleEngine e,ModernBattleObservation.Capture c)throws Exception {
        var m=ModernWorldService.class.getDeclaredMethod("publish",entry.getClass(),ModernBattleEngine.class,ModernBattleObservation.Capture.class);m.setAccessible(true);
        try{m.invoke(s,entry,e,c);}catch(java.lang.reflect.InvocationTargetException failure){if(failure.getCause() instanceof RuntimeException r)throw r;throw failure;}
    }
    static ModernMatchDescriptor honor(ModernBattleObservation.Capture c) {
        var l=c.limits();return new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.HONOR_TOWERS,
                COMBAT|SPELLS|LIGHT_ARROW|ITEMS|WEIGHT|TIMED|REGEN|HONOR,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
    }
    @Test void commandCaptureDoesNotAdvanceClockAgainAfterItsExactResult()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var observed=f.engine.observeCommand(()->{var result=f.cast(0,1,1,3);f.elapsed(5_000_000_000L);return result;});
            assertEquals(ModernSpellProtocol.Reason.NONE,observed.outcome().reason());assertEquals(0,observed.observation().clock().loadingElapsedNanos());
            assertEquals(observed.outcome().snapshot(),ModernLegacyBattleProjection.from(observed.observation()));
            assertEquals(5_000_000_000L,f.engine.serviceObservation().clock().loadingElapsedNanos());
        }
    }
    @Test void anotherEngineOperationCannotInterleaveBetweenCommandAndCapture()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var contender=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var first=pool.submit(()->f.engine.observeCommand(()->{var result=f.cast(0,1,1,3);entered.countDown();
                    try{if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("release timed out");}catch(InterruptedException e){throw new AssertionError(e);}return result;}));
                assertTrue(entered.await(2,TimeUnit.SECONDS));var second=pool.submit(()->{contender.countDown();return f.engine.serviceObservation();});
                assertTrue(contender.await(2,TimeUnit.SECONDS));assertThrows(java.util.concurrent.TimeoutException.class,()->second.get(30,TimeUnit.MILLISECONDS));
                release.countDown();var observed=first.get(2,TimeUnit.SECONDS);assertEquals(observed.outcome().snapshot(),ModernLegacyBattleProjection.from(observed.observation()));
                assertEquals(observed.observation().world(),second.get(2,TimeUnit.SECONDS).world());
            } finally {release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(3,TimeUnit.SECONDS));}
        }
    }
    @Test void trustedTerminalCaptureSurvivesCancellationWithoutReopeningOwnerRead()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.rooms.releasePreparation(f.ticket);var c=f.engine.serviceObservation();
            assertEquals(ModernBattleEngine.Phase.ABORTED,c.phase());assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,c.abortReason());
            assertEquals(f.engine.tick(),ModernLegacyBattleProjection.from(c));var p=f.players.get(0);
            assertThrows(IllegalStateException.class,()->f.engine.observation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void policyAndPrivateEnvelopesArePinnedToOneRealCapture()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.cast(0,1,1,3);var c=f.engine.serviceObservation();var d=honor(c);var p=new ModernBattlePublication(c,d,null,null);
            var a=p.owner(f.players.get(0).scope());var b=p.owner(f.players.get(1).scope());assertSame(a.world(),b.world());assertSame(p.world(),a.world());
            assertEquals(c.limits(),d.engineLimits());assertNotEquals(a.owner().scope(),b.owner().scope());
            assertThrows(SecurityException.class,()->p.owner(new ModernRooms.Scope(7,3333,2,0)));
            var wrongClock=new ModernMatchDescriptor(d.ruleset(),d.features(),2000,3000,d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c,wrongClock,null,null));
            var wrongFlags=new ModernMatchDescriptor(d.ruleset(),d.features()&~TIMED,2000,d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c,wrongFlags,null,null));
        }
    }
    @Test void realServicePublishesLoadedItemAndPrivateStockInTheSameRevision()throws Exception {
        try(var f=new ModernWorldServiceItemTest.Fixture()) {
            var w=f.loading();f.loaded(0,w);f.loaded(1,w);stopScheduler(f.service);var p=f.room.players.get(0);var e=entry(f.service,p.owner());
            var before=cache(f.service,e);consistent(before);
            var result=f.service.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,w.matchId(),w.generation(),1,40010));
            assertEquals(ModernItemProtocol.Status.OK,result.status());var after=cache(f.service,e);consistent(after);
            assertTrue(after.revision()>before.revision());assertEquals(1,after.authority().owner(p.scope()).owner().supplies().stocks().get(0).remaining());
            assertEquals(result.frame().items().get(0).stocks(),after.authority().owner(p.scope()).owner().supplies().stocks());
            assertEquals(2,before.authority().owner(p.scope()).owner().supplies().stocks().get(0).remaining());
        }
    }
    @Test void rejectedCompletePrivateCandidateCannotAdvanceAnyPublishedField()throws Exception {
        try(var f=new ModernWorldServiceItemTest.Fixture()) {
            var w=f.loading();f.loaded(0,w);f.loaded(1,w);stopScheduler(f.service);var p=f.room.players.get(0);var e=entry(f.service,p.owner());
            var engine=(ModernBattleEngine)field(e,"engine");var before=cache(f.service,e);
            var observed=engine.observeCommand(()->engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,w.matchId(),w.generation(),1,40010)));
            var c=observed.observation();assertTrue(c.revision()>before.revision());var a=c.actors().get(0);var foreign=new ModernRooms.Scope(a.scope().sourceId(),a.scope().objectId(),a.scope().epoch()+1,a.scope().channelId());
            var wrong=new ModernBattleObservation.CapturedActor(a.pose(),foreign,a.loadout(),a.skills(),a.vital(),a.resources(),a.castIntervalNanos(),
                    a.histories(),a.targets(),a.runtime(),a.supplies(),a.weight(),a.buffs(),a.ammo(),a.regeneration(),a.regenClock(),a.presentation());
            var invalid=ModernBattleObservationTest.withActor(c,0,wrong,c.revision());
            assertThrows(IllegalArgumentException.class,()->publish(f.service,e,engine,invalid));unchanged(before,cache(f.service,e));
            publish(f.service,e,engine,c);var after=cache(f.service,e);consistent(after);assertEquals(c.revision(),after.revision());
        }
    }
    @Test void newerPublicationWinsAndRetiredEntryCannotBeRevived()throws Exception {
        try(var f=new ModernWorldServiceItemTest.Fixture()) {
            var w=f.loading();f.loaded(0,w);f.loaded(1,w);stopScheduler(f.service);var p=f.room.players.get(0);var e=entry(f.service,p.owner());var engine=(ModernBattleEngine)field(e,"engine");
            var first=engine.observeCommand(()->engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,w.matchId(),w.generation(),1,40010)));
            var next=engine.observeCommand(()->engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,w.matchId(),w.generation(),2,99999)));
            assertTrue(next.observation().revision()>first.observation().revision());publish(f.service,e,engine,next.observation());var newer=cache(f.service,e);
            publish(f.service,e,engine,first.observation());unchanged(newer,cache(f.service,e));
            f.leave(0,w);f.leave(1,w);var retired=cache(f.service,e);publish(f.service,e,engine,next.observation());unchanged(retired,cache(f.service,e));
        }
    }
    @Test void npcPublicationPinsActualRosterAndSelectedCatalogNamesWithoutSecondRead()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture()) {
            f.active();stopScheduler(f.service);var e=entry(f.service,f.a.owner());var c=cache(f.service,e);consistent(c);
            assertNotNull(c.authority().world().npcs());assertEquals(c.npc().resources(),c.authority().resources());assertEquals(c.npc().names(),c.authority().names());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c.authority().capture(),c.authority().descriptor(),null,c.authority().names()));
            var badNames=new ModernNpcNames.Table(c.authority().names().rows().stream().map(n->new ModernNpcNames.Name(n.sourceId(),n.npcId()+1,n.graphic(),n.name(),n.nameId())).toList());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c.authority().capture(),c.authority().descriptor(),c.authority().resources(),badNames));
        }
    }
    @Test void cooperativeJournalAndSavedReceiptRetainOneTerminalAuthority()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernTimeAttackServiceTest.Fixture(temporary,1,journal)) {
            var p=f.players.get(0);var w=f.active();stopScheduler(f.service);var e=entry(f.service,p.owner());f.tower(p,w);for(int n=0;n<3;n++)f.hit(p,w,n+2);
            var terminal=cache(f.service,e);consistent(terminal);assertEquals(journal.event,terminal.authority().world().modes().timeAttackResult());
            journal.state=ModernMatchSettlement.ReceiptState.SAVED;var saved=f.world(p);var after=cache(f.service,e);consistent(after);
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,saved.timeAttack().receipt().state());assertSame(terminal.authority(),after.authority());
            assertEquals(terminal.revision(),after.revision());assertEquals(terminal.world().revision()+1,after.world().revision());
        }
    }
    @Test void siegeTimeoutSavedReceiptKeepsThirtyTypedParticipantsAndOneResult()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernSiegeServiceTest.Fixture(0,500,journal)) {
            f.active();stopScheduler(f.service);var e=entry(f.service,f.players.get(0).owner());ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);
            var terminal=cache(f.service,e);consistent(terminal);assertEquals(30,terminal.authority().world().actors().size());assertEquals(journal.event,terminal.authority().world().modes().siegeResult());
            journal.state=ModernMatchSettlement.ReceiptState.SAVED;f.world();var saved=cache(f.service,e);consistent(saved);assertSame(terminal.authority(),saved.authority());
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,saved.world().siege().receipt().state());assertEquals(journal.event,saved.world().siege().event(saved.world()));
        }
    }
    @Test void racePublicationHasNoInventedPrivateInventoryOrNpcMetadata()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();var c=f.engine.serviceObservation();var l=c.limits();var d=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PET_RACE,PET_RACE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
            var p=new ModernBattlePublication(c,d,null,null);assertNull(p.owner(f.a.scope()).owner().loadout());assertNull(p.owner(f.b.scope()).owner().supplies());assertNull(p.names());
        }
    }
    @Test void allServiceCacheAssignmentsAreConfinedToTheNonfalliblePublisher()throws Exception {
        var source=Files.readString(Path.of("src/main/java/local/lito381/server/ModernWorldService.java"));
        for(var name:List.of("output","publication","engineRevision","cooperativeResult","siegeResult")) {
            var matches=java.util.regex.Pattern.compile("entry\\."+name+"\\s*=(?!=)").matcher(source);int count=0;
            while(matches.find()){count++;assertTrue(matches.start()>source.indexOf("private static void assignPublication("));assertTrue(matches.start()<source.indexOf("private final Object gate"));}
            assertEquals(1,count,name);
        }
        assertFalse(source.contains("engine.tick()"));assertFalse(source.contains("engine.serviceNpcSnapshot()"));
        assertFalse(source.contains("engine.timeAttackResult()"));assertFalse(source.contains("engine.siegeResult()"));
        var canonical=source.substring(source.indexOf("private static Publication publication("),source.indexOf("private final Object gate"));
        var lifecycle=source.substring(source.indexOf("private void publish(Entry"),source.indexOf("private static final class JournalPublicationException"));
        for(var area:List.of(canonical,lifecycle))for(var forbidden:List.of("ModernLegacyBattleProjection", "entry.frame()", "entry.combat()", "entry.npc()"))
            assertFalse(area.contains(forbidden),forbidden);
    }
}
