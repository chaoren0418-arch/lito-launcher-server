package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ModernBattleEngineItemTest {
    @Test void matchStacksAreSeparateFromSourceObjectsAndStillRequireEveryHumanLoaded() throws Exception {
        try(var f=new Fixture(0,false)) {
            var before=f.engine.tick();assertEquals(2,before.items().size());
            assertEquals(3,before.items().get(0).stocks().size());assertTrue(before.items().get(1).stocks().isEmpty());
            assertEquals(ModernBattleEngine.Phase.LOADING,f.ack(0).phase());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.ack(1).phase());
            assertTrue(f.prepared.get(0).fighter().profile().source().inventory().isEmpty());
            assertEquals(before.items(),f.engine.tick().items());
        }
    }
    @Test void realAttackThenPotionAtomicallyRestoresHpConsumesExactlyOneAndKeepsCombatHistory() throws Exception {
        try(var f=new Fixture(0,false)) {
            f.active();var attacked=f.hurt(1);long hp=attacked.vitals().get(0).hp();assertTrue(hp<100);
            var result=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NONE,result.reason());var after=result.snapshot();
            var state=after.items().get(0);assertEquals(Math.min(100,hp+20),after.vitals().get(0).hp());
            assertEquals(hp,state.hpBefore());assertEquals(after.vitals().get(0).hp(),state.hpAfter());
            assertEquals(state.hpAfter()-hp,state.restored());assertEquals(1,state.useCount());
            assertEquals(1,state.requestSequence());assertEquals(after.revision(),state.useRevision());assertEquals(189,state.effect());
            assertEquals(1,state.stocks().get(0).remaining());assertEquals(attacked.casts(),after.casts());assertEquals(attacked.damages(),after.damages());
            assertEquals(attacked.vitals().get(0).hurtRevision(),after.vitals().get(0).hurtRevision());
            assertEquals(attacked.vitals().get(1),after.vitals().get(1));
            assertEquals(attacked.actors(),after.actors());
        }
    }
    @Test void sourcePotionGroupSharesThePreviousItemsDelayAndFullHpStillConsumes() throws Exception {
        try(var f=new Fixture(0,false)) {
            f.active();var red=f.use(1,40010);assertEquals(0,red.snapshot().items().get(0).restored());
            var fast=f.use(2,40011);assertEquals(ModernItemProtocol.Reason.TOO_FAST,fast.reason());
            assertEquals(red.snapshot().items().get(0).stocks(),fast.snapshot().items().get(0).stocks());
            assertEquals(red.snapshot().items().get(0).useRevision(),fast.snapshot().items().get(0).useRevision());
            f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500));assertEquals(ModernItemProtocol.Reason.NONE,f.use(3,40011).reason());
            f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500));assertEquals(ModernItemProtocol.Reason.TOO_FAST,f.use(4,40010).reason());
            f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500));assertEquals(ModernItemProtocol.Reason.NONE,f.use(5,40010).reason());
            f.clock.addAndGet(TimeUnit.SECONDS.toNanos(1));var empty=f.use(6,40010);assertEquals(ModernItemProtocol.Reason.EMPTY,empty.reason());
            assertEquals(3,empty.snapshot().items().get(0).useCount());assertEquals(0,empty.snapshot().items().get(0).stocks().get(0).remaining());
            assertEquals(3,empty.snapshot().items().get(0).stocks().size(),"depleted initial stocks remain auditable");
        }
    }
    @Test void notActiveUnknownAndUnownedRequestsCannotMintItemsOrEffects() throws Exception {
        try(var f=new Fixture(0,false)) {
            var denied=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NOT_ACTIVE,denied.reason());
            assertEquals(1,denied.snapshot().items().get(0).requestSequence());assertEquals(0,denied.snapshot().items().get(0).useCount());
            f.active();assertEquals(ModernItemProtocol.Reason.UNSUPPORTED,f.use(2,9999).reason());
            var other=f.room.players.get(1);var request=new ModernItemProtocol.Request(other.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40010);
            assertEquals(ModernItemProtocol.Reason.EMPTY,f.engine.item(other.owner(),request).reason());
            assertEquals(0,f.engine.tick().items().get(1).useCount());
        }
    }
    @Test void exactOwnerEpochAndNonreplayableSequenceAreRequired() throws Exception {
        try(var f=new Fixture(0,false)) {
            f.active();var p=f.room.players.get(0);var scope=p.scope();
            assertThrows(SecurityException.class,()->f.engine.item(p.owner(),new ModernItemProtocol.Request(
                    new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch()+1,scope.channelId()),ModernItemProtocol.Action.USE,
                    f.ticket.matchId(),f.ticket.generation(),1,40010)));
            var first=f.use(1,40010).snapshot();
            assertThrows(SecurityException.class,()->f.use(1,40010));assertThrows(SecurityException.class,()->f.use(3,40010));
            assertEquals(first,f.engine.tick());
        }
    }
    @Test void deadPlayersCannotUseOwnedSuppliesOrRevive() throws Exception {
        try(var f=new Fixture(0,false)) {
            f.active();for(int i=1;f.engine.tick().vitals().get(0).hp()>0;i++){assertTrue(i<20);f.hurt(i);f.clock.addAndGet(ModernBattleEngineBotTest.ATTACK);}
            var before=f.engine.tick();var denied=f.use(1,40012);assertEquals(ModernItemProtocol.Reason.DEAD,denied.reason());
            assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
            assertEquals(0,denied.snapshot().items().get(0).useCount());
        }
    }
    @Test void expiredOrRegressedClockWinsEvenWhenSlowRandomReturnsMalformedValue() throws Exception {
        for(boolean regressed:new boolean[]{false,true})for(int random:new int[]{0,1,2})try(var f=new Fixture(0,false)) {
            f.active();var before=f.engine.tick();
            f.random.set(bound->{f.clock.addAndGet(regressed?-1:TimeUnit.SECONDS.toNanos(60));return random==0?0:random==1?-1:bound;});
            var denied=f.use(1,40010);var after=denied.snapshot();assertEquals(ModernItemProtocol.Reason.NOT_ACTIVE,denied.reason());
            assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(regressed?ModernBattleEngine.AbortReason.CLOCK_REGRESSED:ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,after.abortReason());
            assertEquals(before.items(),after.items());assertEquals(before.vitals(),after.vitals());assertEquals(before.casts(),after.casts());
            assertEquals(before.damages(),after.damages());assertEquals(before.actors(),after.actors());
        }
    }
    @Test void invalidRandomAndOverflowDoNotCommitPartialHealingInventoryOrCooldown() throws Exception {
        for(long value:new long[]{-1,11})try(var f=new Fixture(0,false)) {
            f.active();var before=f.engine.tick();f.random.set(bound->value);
            var denied=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NUMERIC,denied.reason());
            assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
            f.random.set(bound->bound-1);assertEquals(ModernItemProtocol.Reason.NONE,f.use(2,40010).reason(),"denied RNG cannot start cooldown");
        }
        try(var f=new Fixture(Long.MAX_VALUE,false)) {
            f.active();var before=f.engine.tick();var denied=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NUMERIC,denied.reason());
            assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
            assertEquals(Long.MAX_VALUE,denied.snapshot().items().get(0).fixedRecovery());
        }
        // The actual low roll still fits i64 even when the theoretical upper
        // bound does not. Frame validation must not overflow after the heal.
        try(var f=new Fixture(Long.MAX_VALUE-15,false)) {
            f.active();f.random.set(bound->0);
            var used=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            assertEquals(0,used.snapshot().items().get(0).restored());
            assertEquals(1,used.snapshot().items().get(0).stocks().get(0).remaining());
            f.clock.addAndGet(TimeUnit.SECONDS.toNanos(1));f.random.set(bound->bound-1);
            var denied=f.use(2,40010);assertEquals(ModernItemProtocol.Reason.NUMERIC,denied.reason());
            assertEquals(used.snapshot().items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
        }
    }
    @Test void equipmentRecoveryStaysI64AndNegativeSourceBonusIsNotApplied() throws Exception {
        for(long fixed:new long[]{-1,0,3_000_000_000L})try(var f=new Fixture(fixed,false)) {
            f.active();var injured=f.hurt(1);long hp=injured.vitals().get(0).hp();var healed=f.use(1,40010).snapshot();
            assertEquals(Math.max(0,fixed),healed.items().get(0).fixedRecovery());
            assertEquals(Math.min(100,hp+20+Math.max(0,fixed)),healed.vitals().get(0).hp());
            assertEquals(healed.vitals().get(0).hp()-hp,healed.items().get(0).restored());
        }
    }
    @Test void committedFinalResultCannotBeChangedByPotionUseOrRead() throws Exception {
        try(var f=new Fixture(0,true)) {
            f.active();for(int i=1;f.engine.tick().phase()!=ModernBattleEngine.Phase.FINISHED;i++){assertTrue(i<20);f.hurt(i);f.clock.addAndGet(ModernBattleEngineBotTest.ATTACK);}
            var finished=f.engine.tick();assertNotNull(finished.result());assertEquals(finished,f.use(1,40012).snapshot());
            var p=f.room.players.get(0);assertEquals(finished,f.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.READ,
                    f.ticket.matchId(),f.ticket.generation(),0,0)).snapshot());
        }
    }

    @Test void suppliesRemainHumanOwnedWithSameNumberedBotAndMagicDeathSettlement() throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(1,1)) {
            var ticket=room.capture();var p=room.prepared(ticket).get(0);var clock=new AtomicLong(1_000_000_000L);
            var supplied=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),
                    p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),
                    List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),2)));
            try(var engine=new ModernBattleEngine(ticket,room.binding,List.of(supplied),room.bots(ticket),ModernBattleEngineBotTest.LIMITS,
                    clock::get,()->1000,bound->bound-1,true)) {
                var loading=engine.tick();assertEquals(2,loading.items().size());
                assertFalse(loading.actors().get(0).loaded());assertTrue(loading.actors().get(1).loaded());
                assertTrue(loading.items().get(1).stocks().isEmpty());assertEquals(0,loading.items().get(1).fixedRecovery());
                assertEquals(ModernBattleEngine.Phase.ACTIVE,engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest()).phase());
                var use=new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,ticket.matchId(),ticket.generation(),1,40010);
                var used=engine.item(p.owner(),use);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
                assertEquals(1,used.snapshot().items().get(0).stocks().get(0).remaining());
                assertEquals(loading.items().get(1),used.snapshot().items().get(1));
                for(int sequence=1;sequence<=4;sequence++) {
                    var cast=engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),sequence,4,2));
                    assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());clock.addAndGet(ModernBattleEngineBotTest.CAST);
                }
                var finished=engine.tick();assertEquals(ModernBattleEngine.Phase.FINISHED,finished.phase());assertTrue(finished.result().hasBots());
                assertEquals(used.snapshot().items(),finished.items());assertEquals(18,finished.vitals().get(0).mp());
                assertEquals(0,finished.vitals().get(1).hp());assertEquals(0,finished.items().get(1).requestSequence());
                var frame=ModernItemProtocolTest.frame(finished);
                var read=new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.READ,ticket.matchId(),ticket.generation(),0,0);
                var message=new ModernApplicationProtocol.ItemResult(91,read,new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,frame));
                assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,8),8));
                assertEquals(finished,engine.item(p.owner(),use).snapshot());
            }finally{room.rooms.releasePreparation(ticket);}
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;
        final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;
        final List<ModernBattleEngine.PreparedMember> prepared;
        final AtomicLong clock=new AtomicLong(1_000_000_000L);
        final AtomicReference<ModernPhysicalCombat.BoundedRandom> random=new AtomicReference<>(bound->bound-1);
        Fixture(long fixed,boolean settlement)throws Exception {
            room=new ModernBattleEngineBotTest.RoomFixture(2,0);ticket=room.capture();
            var rows=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)) {
                boolean self=p.scope().sourceId()==7;
                var fighter=self?fighter(p.scope().sourceId(),p.character(),fixed):p.fighter();
                var stocks=self?List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),2),
                        new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40011,1),2),new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40012,2),2))
                        :List.<ModernShopAuthority.Stack>of();
                rows.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),stocks));
            }
            prepared=List.copyOf(rows);
            engine=new ModernBattleEngine(ticket,room.binding,prepared,ModernBattleEngineBotTest.LIMITS,clock::get,()->1000,bound->random.get().nextLong(bound),settlement);
        }
        ModernBattleEngine.Snapshot ack(int index) {var p=room.players.get(index);return engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());}
        void active(){ack(0);assertEquals(ModernBattleEngine.Phase.ACTIVE,ack(1).phase());}
        ModernBattleEngine.ItemResult use(long seq,long item){var p=room.players.get(0);return engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,ticket.matchId(),ticket.generation(),seq,item));}
        ModernBattleEngine.Snapshot hurt(long seq){var p=room.players.get(1);var result=engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,result.status());return result.snapshot();}
        @Override public void close(){engine.close();room.rooms.releasePreparation(ticket);room.close();}
    }
    static ModernPhysicalCombat.Fighter fighter(int source,ModernApplicationProtocol.CharacterRow row,long recovery) {
        var items=recovery==0?List.<Instance>of():List.of(simpleArmor(2,2,bonus(Stat.POTION_RECOVERY,recovery)));
        var original=ModernPhysicalCombatTest.source(source,row.characterClass(),row.level(),items);var q=original.input().equipment();
        var snapshot=new ModernCharacterData.Snapshot(source,row,q.source().persisted(),q.source().inventory());
        var qualified=new Qualified(snapshot,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        var changed=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,original.input().weapons()),original.growth(),original.templates(),original.procCatalogIds());
        return ModernPhysicalCombatTest.fighter(ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(changed,12,0,0,0,30)));
    }
}
