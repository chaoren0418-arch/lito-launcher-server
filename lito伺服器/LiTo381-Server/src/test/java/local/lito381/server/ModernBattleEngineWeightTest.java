package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real captured rooms/qualified profiles; no client capacity or fabricated engine combatant. */
class ModernBattleEngineWeightTest {
    @Test void loadingRequiresEveryHumanButWeightsAlreadyDescribeExactOwnedStock()throws Exception {
        try(var f=new Fixture(0,true)) {
            var before=f.engine.tick();assertEquals(2,before.weights().size());
            assertEquals(210,before.weights().get(0).snapshot().view().stage());
            assertEquals(7,before.weights().get(0).snapshot().view().suppliesWeight());
            assertEquals(List.of(new ModernInventoryWeight.Unit(40010,3900)),before.weights().get(0).snapshot().units());
            assertEquals(ModernBattleEngine.Phase.LOADING,f.ack(0).phase());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.ack(1).phase());
            assertEquals(before.weights(),f.engine.tick().weights());
        }
    }
    @Test void heavyMeleeConsumesOnlyDeniedSequenceWithoutRandomHpMpCooldownOrAttackEvent()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();var before=f.engine.tick();var denied=f.attack(1);
            assertEquals(ModernBattleEngine.AttackStatus.TOO_HEAVY,denied.status());assertEquals(0,f.draws.get());
            assertEquals(before.vitals().get(0).hp(),denied.snapshot().vitals().get(0).hp());
            assertEquals(before.vitals().get(0).mp(),denied.snapshot().vitals().get(0).mp());
            assertEquals(before.vitals().get(1),denied.snapshot().vitals().get(1));
            assertEquals(1,denied.snapshot().vitals().get(0).attackSequence());assertEquals(0,denied.snapshot().vitals().get(0).attackRevision());
            assertEquals(before.weights(),denied.snapshot().weights());assertEquals(before.actors(),denied.snapshot().actors());
            assertThrows(SecurityException.class,()->f.attack(1));
        }
    }
    @Test void heavyTeleportAndLightArrowShareDenialSequenceWithoutMpOrPositionChange()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();var before=f.engine.tick();var tp=f.cast(1,5,0);var magic=f.cast(2,4,2);
            assertEquals(ModernSpellProtocol.Reason.TOO_HEAVY,tp.reason());assertEquals(ModernSpellProtocol.Reason.TOO_HEAVY,magic.reason());
            assertEquals(0,f.draws.get());assertEquals(before.vitals(),magic.snapshot().vitals());
            assertEquals(before.actors(),magic.snapshot().actors());assertEquals(before.damages(),magic.snapshot().damages());
            assertEquals(2,magic.snapshot().casts().get(0).requestSequence());assertEquals(0,magic.snapshot().casts().get(0).castCount());
        }
    }
    @Test void movementIsNotBlockedByHeavyStage()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();var before=f.engine.tick();var p=f.prepared.get(0);var at=before.actors().get(0).position();
            var moved=f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,at.x(),at.y(),0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,moved.status());
            assertEquals(before.weights(),moved.snapshot().weights());assertEquals(new ModernWorldMap.Cell(at.x(),at.y()-1),moved.snapshot().actors().get(0).position());
        }
    }
    @Test void healingAtomicallyLowersWeightAndImmediatelyRestoresPhysicalAdmission()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.TOO_HEAVY,f.attack(1).status());
            var healed=f.use(1);assertEquals(ModernItemProtocol.Reason.NONE,healed.reason());
            var weight=healed.snapshot().weights().get(0).snapshot();assertEquals(3,weight.view().suppliesWeight());
            assertEquals(90,weight.view().stage());assertEquals(1,weight.view().hitPenalty());
            assertEquals(1,healed.snapshot().items().get(0).stocks().get(0).remaining());
            assertEquals(healed.snapshot().revision(),healed.snapshot().items().get(0).useRevision());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(2).status(),"weight denial must not consume attack cooldown");
        }
    }
    @Test void depletionKeepsOriginalUnitAndTerminalSnapshotsDoNotRestoreUsedItems()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();var first=f.use(1);f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500));var last=f.use(2);
            assertEquals(0,last.snapshot().weights().get(0).snapshot().view().suppliesWeight());
            assertEquals(first.snapshot().weights().get(0).snapshot().units(),last.snapshot().weights().get(0).snapshot().units());
            assertEquals(0,last.snapshot().items().get(0).stocks().get(0).remaining());
            var p=f.prepared.get(1);f.engine.disconnect(p.owner());var terminal=f.engine.tick();
            assertEquals(ModernBattleEngine.Phase.ABORTED,terminal.phase());assertEquals(last.snapshot().weights(),terminal.weights());
            assertEquals(terminal,f.use(3).snapshot());
        }
    }
    @Test void botsAreExplicitlyNotApplicableAndNeverBorrowHumanSourceWeights()throws Exception {
        try(var f=new Fixture(1,true)) {
            var before=f.engine.tick();assertEquals(2,before.weights().size());
            assertEquals(ModernInventoryWeight.notApplicable(),before.weights().get(1).snapshot());
            assertTrue(before.actors().get(1).loaded());assertFalse(before.actors().get(0).loaded());
            f.active();f.use(1);assertEquals(ModernInventoryWeight.notApplicable(),f.engine.tick().weights().get(1).snapshot());
        }
    }
    @Test void legacyItemEnabledEngineKeepsWeightEntirelyAbsent()throws Exception {
        try(var f=new Fixture(0,false)) {
            f.active();assertTrue(f.engine.tick().weights().isEmpty());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());assertTrue(f.use(1).snapshot().weights().isEmpty());
        }
    }
    @Test void deniedRngDoesNotRemoveStockOrChangeWeight()throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();var before=f.engine.tick();f.badRandom=true;var failure=f.use(1);
            assertEquals(ModernItemProtocol.Reason.NUMERIC,failure.reason());assertEquals(before.weights(),failure.snapshot().weights());
            assertEquals(before.vitals(),failure.snapshot().vitals());assertEquals(before.items().get(0).stocks(),failure.snapshot().items().get(0).stocks());
        }
    }
    @Test void classicWeightHitPenaltyChangesHitSubtotalWithoutChangingQualifiedFighter() {
        var a=ModernPhysicalCombatTest.createFighter(7,ModernShopAuthorityTest.row(),100,10,0);
        var b=ModernPhysicalCombatTest.createFighter(42,ModernShopAuthorityTest.row(),100,10,0);
        var original=a.offense();
        for(int penalty:List.of(0,1,3,5)) {
            var result=ModernPhysicalCombat.resolveValues(a.values(),b.values(),100,bound->bound-1,penalty);
            assertNotNull(result);assertEquals(original,a.offense());
        }
        assertThrows(ModernPhysicalCombat.CombatException.class,()->ModernPhysicalCombat.resolveValues(a.values(),b.values(),100,bound->0,2));
    }

    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;final ModernRooms.FrozenPreparation ticket;
        final List<ModernBattleEngine.PreparedMember> prepared;final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(1_000_000_000L);final AtomicInteger draws=new AtomicInteger();boolean badRandom;
        Fixture(int bots,boolean weighted)throws Exception {
            room=new ModernBattleEngineBotTest.RoomFixture(bots==0?2:1,bots);ticket=room.capture();
            var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)) {
                boolean self=p.scope().sourceId()==7;
                var stocks=self?List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),2)):List.<ModernShopAuthority.Stack>of();
                var defaultBasis=ModernInventoryWeight.qualify(p.fighter().profile(),1);
                var basis=weighted?ModernInventoryWeight.qualify(p.fighter().profile(),self?8.0/defaultBasis.maximumWeight():1):null;
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),
                        p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),stocks,basis));
            }
            prepared=List.copyOf(members);
            engine=new ModernBattleEngine(ticket,room.binding,prepared,room.bots(ticket),ModernBattleEngineBotTest.LIMITS,
                    clock::get,()->1000,bound->{draws.incrementAndGet();return badRandom?-1:bound-1;},false);
        }
        ModernBattleEngine.Snapshot ack(int index){var p=prepared.get(index);return engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());}
        void active(){for(int i=0;i<prepared.size();i++)ack(i);assertEquals(ModernBattleEngine.Phase.ACTIVE,engine.tick().phase());}
        ModernBattleEngine.AttackResult attack(long sequence){var p=prepared.get(0);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,2);}
        ModernBattleEngine.CastResult cast(long sequence,int skill,long target){var p=prepared.get(0);return engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),sequence,skill,target));}
        ModernBattleEngine.ItemResult use(long sequence){var p=prepared.get(0);return engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,ticket.matchId(),ticket.generation(),sequence,40010));}
        @Override public void close(){engine.close();room.rooms.releasePreparation(ticket);room.close();}
    }
}
