package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentAuthority.OK;
import static local.lito381.server.ModernEquipmentAuthority.READ;
import static local.lito381.server.ModernEquipmentAuthority.SET;
import static local.lito381.server.ModernEquipmentAuthority.ZERO;
import local.lito381.server.ModernEquipmentAuthority.Loaded;
import local.lito381.server.ModernEquipmentAuthority.PhysicalLoaded;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static local.lito381.server.ModernWorldProtocol.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real WorldService/engine/gear store. SET still uses the trusted internal
 * transaction seam: this is not a network SET or player-UI acceptance test. */
class ModernBattleLifecycleServiceTest {
    @TempDir Path root;

    @Test void dynamicPriceCanPublishClockReadAndLeaveWithoutLegacyFrames()throws Exception {
        try(var f=new Fixture(root,null)) {
            f.reprice();var before=f.read(0);var old=before.view();
            assertEquals(1,old.personal().equipment().revision());
            assertThrows(IllegalArgumentException.class,()->f.service.combat(f.player(0).owner(),
                    new ModernCombatProtocol.Request(f.player(0).scope(),ModernCombatProtocol.Action.READ,f.world.matchId(),f.world.generation(),0,0)));
            Thread.sleep(3);var after=f.read(0);
            assertEquals(old.world().revision(),after.view().world().revision());
            assertTrue(after.view().world().clock().loadingElapsedNanos()>old.world().clock().loadingElapsedNanos());
            assertEquals(0,f.read(1).view().personal().equipment().revision());
            assertTrue(f.lifecycle().result()==null);
            f.leave(0);var terminal=f.read(1);
            assertEquals(ModernBattleEngine.Phase.ABORTED,terminal.view().world().phase());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,old.world().phase());
            assertEquals(Phase.ABORTED,f.lifecycle().phase());assertFalse(f.lifecycle().resultOutstanding());
            assertThrows(SecurityException.class,()->f.read(0));f.leave(1);
            ModernWorldServiceTest.awaitNoMatches(f.service);assertEquals(0,f.persisted.get());assertEquals(2,f.terminals.get());
        }
    }

    @Test void dynamicHistoryReachesRealVictoryWriterAndSavedReceiptBeforeBothLeave()throws Exception {
        try(var f=new Fixture(root,null)) {
            f.reprice();var old=f.read(0).view();f.win();var won=f.read(0);
            assertEquals(ModernBattleEngine.Phase.FINISHED,won.view().world().phase());
            ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==SettlementState.SAVED);
            assertEquals(1,f.persisted.get());assertEquals(f.event.get(),f.lifecycle().result());
            assertEquals(won.view().world().result(),f.event.get());assertEquals(2,f.event.get().participants().size());
            assertEquals(List.of(7,42),f.event.get().participants().stream().map(p->p.key().sourceId()).toList());
            assertTrue(f.lifecycle().revision()>won.view().world().revision()+1);
            assertEquals(ModernBattleEngine.Phase.ACTIVE,old.world().phase());
            assertEquals(won.view().world().result(),f.read(1).view().world().result());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(f.engine.serviceObservation()));
            f.leave(0);assertEquals(ModernBattleEngine.Phase.FINISHED,f.read(1).view().world().phase());
            f.leave(1);ModernWorldServiceTest.awaitNoMatches(f.service);assertEquals(1,f.persisted.get());
        }
    }

    @Test void actualJournalReservationReceivesDynamicTerminalWithoutLegacyConversion()throws Exception {
        var journal=new Journal();
        try(var f=new Fixture(root,journal)) {
            f.reprice();f.win();var won=f.read(0);
            assertEquals(ModernBattleEngine.Phase.FINISHED,won.view().world().phase());
            assertEquals(1,journal.records.get());assertEquals(SettlementState.SAVED,f.lifecycle().settlement());
            assertEquals(journal.event,f.lifecycle().result());f.leave(0);f.leave(1);
        }
        assertEquals(1,journal.released.get());assertTrue(journal.closed);
    }

    static final class Journal implements ModernMatchSettlement.JournaledStore {
        final AtomicInteger records=new AtomicInteger(),released=new AtomicInteger();
        ModernMatchSettlement.Event event;boolean closed;
        public ModernMatchSettlement.Reservation reserve(long match,long generation) {
            return new ModernMatchSettlement.Reservation(){boolean freed;
                public void record(ModernMatchSettlement.Event e,long deadline){assertEquals(match,e.matchId());assertEquals(generation,e.generation());assertEquals(1,e.mode());event=e;records.incrementAndGet();}
                public ModernMatchSettlement.ReceiptState state(){return event==null?ModernMatchSettlement.ReceiptState.PENDING:ModernMatchSettlement.ReceiptState.SAVED;}
                public void close(){if(!freed){freed=true;released.incrementAndGet();}}
            };
        }
        public boolean accepting(){return !closed;}
        public void persist(ModernMatchSettlement.Event e,long deadline){fail("reserved journal must own the result");}
        public void close(){closed=true;}
        public boolean awaitTermination(long millis){return closed;}
        public boolean isQuiescent(){return closed;}
    }

    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room=new ModernBattleEngineBotTest.RoomFixture(2,0);
        final ModernWorldService service;final ModernEquipmentOverrideStore store;final ModernEquipmentAuthority gear;
        final ModernPhysicalCombat.Source source;
        final AtomicInteger persisted=new AtomicInteger(),terminals=new AtomicInteger();
        final AtomicReference<ModernMatchSettlement.Event> event=new AtomicReference<>();
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("canonical-service-test",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
        final Frame world;final Object entry;final ModernBattleEngine engine;
        long moves;
        Fixture(Path root,ModernMatchSettlement.Store journal)throws Exception {this(root,journal,37);}
        Fixture(Path root,ModernMatchSettlement.Store journal,int version)throws Exception {
            this(root,journal,version,new ModernEquipmentOverrideStore.FileOps(){});
        }
        Fixture(Path root,ModernMatchSettlement.Store journal,int version,ModernEquipmentOverrideStore.FileOps files)throws Exception {
            store=new ModernEquipmentOverrideStore(root,files);source=source(player(0));
            var mapping=new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,10_000_000,true,false,true,true,false,false,true,null,true);
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),mapping),
                    new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(4000,0,30000)),bound->bound-1,
                    journal==null?(e,d)->{event.set(e);persisted.incrementAndGet();}:journal,Map.of(7,100.0,42,100.0));
            gear=new ModernEquipmentAuthority((p,s,o)->new Loaded(source.input().equipment(),binding(source.input().equipment()).fingerprint()),null,
                    (p,s,o)->new PhysicalLoaded(source,binding(source.input().equipment()).fingerprint()),store,System::currentTimeMillis);
            for(var p:room.players) {
                var full=p==player(0)?source:source(p);var fighter=ModernPhysicalCombatTest.fighter(full);
                service.register(p.owner(),p.scope(),d->p.row(),d->fighter,true,
                        d->new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM))),true,
                        new ModernWorldService.MatchItems(){
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError("weight basis required");}
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}
                            public void terminal(long m,long g){terminals.incrementAndGet();}
                        },true,false,false,true,false,true,new ModernSessionBoundary.Negotiated(1,version,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE));
            }
            var summary=room.view().summary();assertEquals(Status.OK,service.handle(player(0).owner(),new Request(player(0).scope(),Action.PREPARE,0,summary.id(),summary.revision(),0,0,0,0,0,ZERO_DIGEST)).status());
            ModernWorldServiceItemTestAwait.until(()->legacyRead().phase()==Phase.LOADING);world=legacyRead();
            for(var p:room.players)assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LOADED,0,0,0,world.matchId(),world.generation(),0,0,0,world.manifest().digest())).status());
            ModernBattlePublicationTest.stopScheduler(service);entry=ModernBattlePublicationTest.entry(service,player(0).owner());
            engine=(ModernBattleEngine)ModernBattlePublicationTest.field(entry,"engine");assertEquals(ModernBattleEngine.Phase.ACTIVE,read(0).view().world().phase());
        }
        static ModernPhysicalCombat.Source source(ModernBattleEngineBotTest.Player p) {
            var base=ModernBattleLoadoutTest.source(p.scope().sourceId(),true);var q=base.input().equipment();
            var data=new ModernCharacterData.Snapshot(q.source().sourceId(),p.row(),q.source().persisted(),q.source().inventory());
            var qualified=new ModernEquipmentFacts.Qualified(data,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(data));
            return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,base.input().weapons()),base.growth(),base.templates(),base.procCatalogIds(),base.magicGrowth(),base.magicBows(),base.weaponArrowSprites(),base.regenGrowth(),base.healingGrowth());
        }
        ModernBattleEngineBotTest.Player player(int i){return room.players.get(i);}
        Frame legacyRead(){return service.handle(player(0).owner(),new Request(player(0).scope(),Action.READ,0,0,0,0,0,0,0,0,ZERO_DIGEST)).frame();}
        ModernBattleReadProtocol.Reply read(int i){return service.readBattle(player(i).owner(),ModernBattleReadProtocolTest.request(player(i).scope(),world));}
        ModernBattleLifecycle lifecycle()throws Exception {synchronized(ModernBattlePublicationTest.field(service,"gate")){return (ModernBattleLifecycle)ModernBattlePublicationTest.field(ModernBattlePublicationTest.field(entry,"output"),"lifecycle");}}
        void move(int heading)throws Exception {
            Thread.sleep(2);var a=engine.serviceObservation().actors().get(0).pose();
            var request=new ModernApplicationProtocol.WorldRequest(9,new Request(player(0).scope(),Action.MOVE,heading,0,0,world.matchId(),world.generation(),++moves,a.position().x(),a.position().y(),ZERO_DIGEST));
            var receipt=action(request,ModernBattleActionProtocol.NO_TARGET);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,((ModernBattleEngine.MoveResult)receipt.outcome()).status());
        }
        ModernBattleActionReceipt action(ModernApplicationProtocol.Message command,ModernBattleActionProtocol.Target target) {
            var request=new ModernApplicationProtocol.BattleActionRequest(command.requestId(),new ModernBattleActionProtocol.Request(ModernBattleCommandLifeTest.proof(engine,0),target,command));
            var receipt=service.canonicalAction(player(0).owner(),request);
            assertEquals(ModernBattleCommandLife.Rejection.NONE,receipt.rejection());return receipt;
        }
        void reprice()throws Exception {
            // The service's deterministic second spawn is north of the first.
            // Walk south into empty terrain; do not bypass collision admission.
            for(int i=0;i<8;i++)move(4);
            Thread.sleep(2); // The final MOVE also owns the shared action clock.
            var first=engine.cast(player(0).owner(),new ModernSpellProtocol.Request(player(0).scope(),world.matchId(),world.generation(),1,70,0));
            assertEquals(ModernSpellProtocol.Reason.NONE,first.reason());
            var scope=ModernEquipmentOverrideStoreTest.scope();var lease=new ModernEquipmentLease.Issuer().open(player(0).owner(),binding(source.input().equipment()).key(),1,()->{});
            try(var op=lease.operation(scope)) {
                ModernBackendScope.bind(scope);
                try {
                    var before=gear.execute(principal,player(0).row(),op,new ModernEquipmentAuthority.Request(7,1,1,READ,false,0,0,ZERO)).snapshot();
                    var proof=ModernBattleCommandLifeTest.proof(engine,0);
                    var changed=gear.executeInMatch(principal,player(0).row(),op,new ModernEquipmentAuthority.Request(7,1,1,SET,false,1,before.revision(),before.viewToken()),engine.beginCanonicalEquipmentChange(player(0).owner(),proof));
                    assertEquals(OK,changed.equipment().status());assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
                    assertEquals(1,store.read(op.key(),op.scope()).orElseThrow().revision());
                } finally {ModernBackendScope.unbind();}
            } finally {lease.invalidate();scope.finish();}
            Thread.sleep(2050);var receipt=action(new ModernApplicationProtocol.CastRequest(10,new ModernSpellProtocol.Request(player(0).scope(),world.matchId(),world.generation(),2,70,0)),ModernBattleActionProtocol.NO_TARGET);
            var second=(ModernBattleEngine.CastResult)receipt.outcome();
            assertEquals(ModernSpellProtocol.Reason.NONE,second.reason());assertThrows(IllegalArgumentException.class,second::snapshot);
            var actual=read(0);assertEquals(2,actual.view().world().actors().get(0).spells().histories().get(ModernUseHistory.Lane.FIRE_STORM).current().casts());
            Thread.sleep(2050); // Fire Storm's real cooldown also blocks movement.
            for(int i=0;i<8;i++)move(0);
        }
        void win()throws Exception {
            for(int sequence=1;sequence<=100;sequence++) {
                Thread.sleep(2);var receipt=action(new ModernApplicationProtocol.CombatRequest(11,new ModernCombatProtocol.Request(player(0).scope(),ModernCombatProtocol.Action.ATTACK,world.matchId(),world.generation(),sequence,2)),new ModernBattleActionProtocol.Target(ModernBattleActionProtocol.Kind.PLAYER,2,1));
                var hit=(ModernBattleEngine.AttackResult)receipt.outcome();
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,hit.status());if(hit.observation().phase()==ModernBattleEngine.Phase.FINISHED)return;
            }
            fail("actual qualified attacks did not finish the match");
        }
        void leave(int i){var p=player(i);assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LEAVE,0,0,0,world.matchId(),world.generation(),0,0,0,ZERO_DIGEST)).status());}
        public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());store.close();assertTrue(store.isQuiescent());room.close();}
    }
    @FunctionalInterface interface Check {boolean ready()throws Exception;}
    static final class ModernWorldServiceItemTestAwait {
        static void until(Check check)throws Exception {long end=System.nanoTime()+4_000_000_000L;while(!check.ready()){assertTrue(System.nanoTime()<end);Thread.sleep(2);}}
    }
}
