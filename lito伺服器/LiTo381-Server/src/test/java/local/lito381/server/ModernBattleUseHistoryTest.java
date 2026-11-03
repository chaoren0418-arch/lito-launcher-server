package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernBattleUseHistoryTest {
    @Test void reentrantPotionCannotOverwriteAnAlreadyCommittedUseOrItsHistory() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();f.random.set(bound->{f.random.set(b->b-1);assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40011).reason());return 0;});
            assertThrows(SecurityException.class,()->f.use(1,40010));
            var s=f.engine.tick();var h=read(f.engine,f.ticket,f.room.players.get(0)).items();
            assertEquals(1,h.uses());assertEquals(40011,h.last().itemId());assertEquals(1,s.items().get(0).requestSequence());
            assertEquals(2,s.items().get(0).stocks().get(0).remaining());assertEquals(1,s.items().get(0).stocks().get(1).remaining());
        }
    }
    @Test void reentrantSpellCannotOverwriteAnotherLaneOrRollBackSharedSequence() throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.randomOverride=bound->{f.randomOverride=null;assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());return 0;};
            assertThrows(SecurityException.class,()->f.cast(0,1,5,0));
            var s=f.engine.tick();var h=read(f.engine,f.ticket,f.players.get(0)).spells();
            assertEquals(1,s.casts().get(0).requestSequence());assertEquals(96,s.vitals().get(0).mp());
            assertEquals(1,h.get(ModernUseHistory.Lane.HEALING).current().casts());assertNull(h.get(ModernUseHistory.Lane.TELEPORT).last());
        }
    }
    static ModernBattleEngine.UseHistories read(ModernBattleEngine engine,ModernRooms.FrozenPreparation ticket,ModernBattleEngineBotTest.Player player) {
        return engine.useHistories(player.owner(),player.scope(),ticket.matchId(),ticket.generation());
    }
    @Test void realPotionUseCapturesRollClippingAndBonusAndDeniedUsePreservesIt() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(3_000_000_000L,false)) {
            f.active();var before=f.hurt(1);var player=f.room.players.get(0);
            var used=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var history=read(f.engine,f.ticket,player);var last=history.items().last();
            assertEquals(before.vitals().get(0).hp(),last.before().vitals().hp());
            assertEquals(20,last.rolledBase());assertEquals(3_000_000_000L,last.fixedRecovery());
            assertEquals(used.snapshot().items().get(0).restored(),history.items().restored());
            assertEquals(used.snapshot().revision(),last.revision());assertEquals(100,last.hpAfter());
            assertEquals(ModernItemProtocol.Reason.TOO_FAST,f.use(2,40011).reason());
            assertEquals(history.items(),read(f.engine,f.ticket,player).items());
            assertThrows(UnsupportedOperationException.class,()->history.spells().clear());
            assertThrows(SecurityException.class,()->f.engine.useHistories(f.room.players.get(1).owner(),player.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.useHistories(player.owner(),player.scope(),f.ticket.matchId(),f.ticket.generation()+1));
        }
    }
    @Test void potionOverflowAndInvalidEntropyNeverPublishHistoryOrConsumeStocks() throws Exception {
        for(long fixed:new long[]{0,Long.MAX_VALUE})try(var f=new ModernBattleEngineItemTest.Fixture(fixed,false)) {
            f.active();var player=f.room.players.get(0);var before=read(f.engine,f.ticket,player);var stocks=f.engine.tick().items().get(0).stocks();
            if(fixed==0)f.random.set(bound->-1);
            var denied=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NUMERIC,denied.reason());
            assertEquals(before.items(),read(f.engine,f.ticket,player).items());assertEquals(stocks,denied.snapshot().items().get(0).stocks());
        }
    }
    @Test void historyPlannerOverflowRejectsWholeUseBeforeHpStockAndCooldown() throws Exception {
        try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();f.hurt(1);var player=f.room.players.get(0);
            var actors=ModernBattleEngine.class.getDeclaredField("actors");actors.setAccessible(true);
            var actor=((java.util.List<?>)actors.get(f.engine)).get(0);
            var field=actor.getClass().getDeclaredField("itemHistory");field.setAccessible(true);
            // A valid accumulated history at exhaustion, paired with the same
            // legacy stock/use count. The next restoration cannot be committed.
            f.use(1,40010);var old=(ModernUseHistory.Items)field.get(actor);
            f.clock.addAndGet(1_000_000_000L);f.hurt(2);
            // Two prior uses allow a cumulative total above this last event.
            f.use(2,40011);old=(ModernUseHistory.Items)field.get(actor);
            field.set(actor,new ModernUseHistory.Items(old.equipmentRevision(),old.profileRevision(),old.fixedRecovery(),old.uses(),Long.MAX_VALUE,old.last()));
            f.clock.addAndGet(1_000_000_000L);f.hurt(3);var before=f.engine.tick();
            var denied=f.use(3,40010);assertEquals(ModernItemProtocol.Reason.NUMERIC,denied.reason());
            assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
            assertEquals(Long.MAX_VALUE,read(f.engine,f.ticket,player).items().restored());
        }
    }
    @Test void actualTeleportAndHonorHealKeepIndependentLanesAndDeniedAttemptsDoNotReplaceReceipt() throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());
            var first=read(f.engine,f.ticket,p).spells().get(ModernUseHistory.Lane.HEALING);
            assertEquals(4,first.last().cost());assertEquals(96,first.last().mpAfter());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,1,3).reason());
            assertEquals(first,read(f.engine,f.ticket,p).spells().get(ModernUseHistory.Lane.HEALING));
            f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,5,0).reason());
            var histories=read(f.engine,f.ticket,p).spells();assertEquals(first,histories.get(ModernUseHistory.Lane.HEALING));
            var teleport=histories.get(ModernUseHistory.Lane.TELEPORT);
            assertEquals(96,teleport.last().before().vitals().mp());assertEquals(86,teleport.last().mpAfter());assertEquals(3,teleport.last().sequence());
        }
    }
    @Test void realDeathAndRespawnRetainReceiptThenNextUseHasNewLife() throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();f.cast(1,1,1,4);var p=f.players.get(1);var first=read(f.engine,f.ticket,p).spells().get(ModernUseHistory.Lane.HEALING);
            f.cast(0,1,4,2);f.elapsed(5_000_000_000L);var revived=f.engine.tick();assertEquals(1,revived.vitals().get(1).hp());
            assertEquals(first,read(f.engine,f.ticket,p).spells().get(ModernUseHistory.Lane.HEALING));
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,2,1,4).reason());
            var next=read(f.engine,f.ticket,p).spells().get(ModernUseHistory.Lane.HEALING);
            assertEquals(1,first.last().before().life());assertEquals(2,next.last().before().life());assertEquals(8,next.current().spent());
        }
    }
    @Test void actualDisintegrateKillCommitsHistoricalMpWithLegacyDeathAndSettlement() throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,100,300,500,false)) {
            f.active();f.randomOverride=b->b==100?99:0;
            var reply=ModernBattleEngineDisintegrateTest.cast(f,1,2);assertEquals(ModernDisintegrateState.Reason.NONE,reply.reason());
            var h=read(f.engine,f.ticket,f.players.get(0)).spells().get(ModernUseHistory.Lane.DISINTEGRATE);
            assertEquals(reply.snapshot().revision(),h.last().revision());assertEquals(300,h.last().before().vitals().mp());
            assertEquals(reply.snapshot().vitals().get(0).mp(),h.last().mpAfter());assertEquals(reply.snapshot().disintegrations().get(0).mpSpent(),h.current().spent());
            assertEquals(ModernBattleEngine.Phase.FINISHED,reply.snapshot().phase());assertNotNull(reply.snapshot().result());
        }
    }
}
