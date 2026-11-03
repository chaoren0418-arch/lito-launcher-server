package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernBattleEngineRegenTest {
    static final long SECOND=1_000_000_000L;
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;final ModernRooms.FrozenPreparation ticket;
        final List<ModernBattleEngine.PreparedMember> prepared;final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(SECOND),epoch=new AtomicLong(1000);final AtomicInteger draws=new AtomicInteger();
        long jump;boolean invalid;int failAtDraw=-1;final long[] itemSeq=new long[2];
        Runnable duringRandom;
        final long origin;
        Fixture()throws Exception{this(0,4,225,100,0,1000,false);}
        Fixture(long hpr,long mpr,long food,double rate,int bots,long targetHp,boolean settle)throws Exception {
            this(hpr,mpr,food,rate,bots,targetHp,settle,SECOND);
        }
        Fixture(long hpr,long mpr,long food,double rate,int bots,long targetHp,boolean settle,long origin)throws Exception {
            this.origin=origin;clock.set(origin);
            room=new ModernBattleEngineBotTest.RoomFixture(bots==0?2:1,bots);ticket=room.capture();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.players) {
                var source=ModernNaturalRegenTest.source(p.scope().sourceId(),p.row(),members.isEmpty()?1000:targetHp,30,hpr,mpr,food);
                var fighter=ModernPhysicalCombatTest.fighter(source);
                var stocks=ModernShopTimedTest.catalog(p.row().characterClass()).offers().stream().filter(o->o.support()==ModernShopCatalog.Support.HEALING)
                        .sorted(java.util.Comparator.comparingLong(ModernShopCatalog.Offer::itemId)).map(o->new ModernShopAuthority.Stack(o,4)).toList();
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.row(),ModernBattleEngineBotTest.cell(10+members.size(),10),200_000_000,
                        fighter,700_000_000,new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),ModernBattleEngineBotTest.learned()),1_200_000_000,
                        true,stocks,ModernInventoryWeight.qualify(fighter.profile(),rate),true,false,true));
            }
            prepared=List.copyOf(members);engine=new ModernBattleEngine(ticket,room.binding,prepared,room.bots(ticket),
                    new ModernBattleEngine.Limits(2000,0,1_800_000),clock::get,epoch::get,b->{int draw=draws.incrementAndGet();
                        if(duringRandom!=null){var action=duringRandom;duringRandom=null;action.run();}
                        if(jump!=0){clock.addAndGet(jump);jump=0;}return invalid||draw==failAtDraw?-1:b-1;},settle);
        }
        ModernBattleEngine.Snapshot active(){for(var p:prepared)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());return engine.tick();}
        void elapsed(long elapsed){clock.set(origin+elapsed);}
        ModernBattleEngine.AttackResult attack(int who,long sequence){var p=prepared.get(who);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,who==0?2:1);}
        ModernBattleEngine.CastResult cast(int who,long sequence,int skill){var p=prepared.get(who);return engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),sequence,skill,skill==5?0:who==0?2:1));}
        ModernBattleEngine.ItemResult heal(int who){var p=prepared.get(who);return engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,ticket.matchId(),ticket.generation(),++itemSeq[who],40010));}
        @Override public void close(){engine.close();room.rooms.releasePreparation(ticket);room.close();}
    }
    @Test void loadingAndFullVitalsNeverRegenerateOrConsumeRandomOrProduceRevision()throws Exception {
        try(var f=new Fixture()){var loading=f.engine.tick();assertEquals(List.of(ModernRegenProtocol.State.initial(1),ModernRegenProtocol.State.initial(2)),loading.regen());
            var active=f.active();f.elapsed(100*SECOND);assertEquals(active,f.engine.tick());assertEquals(0,f.draws.get());}
    }
    @Test void exactTwoSecondKnightHpAndSixteenSecondMpUseSeparateLedgersAndNoFakeEvents()throws Exception {
        try(var f=new Fixture()){f.active();var cast=f.cast(0,1,4).snapshot();long hp=cast.vitals().get(1).hp();assertTrue(hp<1000);
            f.elapsed(2*SECOND-1);assertEquals(cast,f.engine.tick());f.elapsed(2*SECOND);var hpTick=f.engine.tick();
            assertEquals(hp+1,hpTick.vitals().get(1).hp());assertEquals(1,hpTick.regen().get(1).hpGain());assertEquals(cast.vitals().get(1).hurtRevision(),hpTick.vitals().get(1).hurtRevision());
            f.elapsed(16*SECOND);var mpTick=f.engine.tick();assertEquals(30,mpTick.vitals().get(0).mp());assertEquals(3,mpTick.regen().get(0).mpGain());
            assertEquals(cast.casts(),mpTick.casts());assertEquals(cast.damages(),mpTick.damages());assertEquals(cast.items(),mpTick.items());assertEquals(cast.weights(),mpTick.weights());
            assertEquals(mpTick,f.engine.tick());}
    }
    @Test void signedNegativeRegenFloorsAliveHpAtOneAndMpAtZeroWithoutDeathOrHurtEvents()throws Exception {
        try(var f=new Fixture(-4,-4,225,100,0,1000,false)){f.active();var hit=f.cast(0,1,4).snapshot();f.elapsed(1000*SECOND);var after=f.engine.tick();
            assertEquals(1,after.vitals().get(1).hp());assertEquals(0,after.vitals().get(0).mp());assertTrue(after.regen().get(1).hpLoss()>0);assertEquals(27,after.regen().get(0).mpLoss());
            assertEquals(hit.vitals().get(1).hurtRevision(),after.vitals().get(1).hurtRevision());assertEquals(0,after.vitals().get(1).deathRevision());assertNull(after.result());}
    }
    @Test void actualHealingAndLaterNaturalHpBothConserveAndPreserveTheLastItemReceipt()throws Exception {
        try(var f=new Fixture()){f.active();for(int n=0;n<3;n++){f.elapsed(n*1_200_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,n+1,4).reason());}
            var healed=f.heal(0).snapshot();assertEquals(healed.items().get(0).restored(),healed.regen().get(0).itemGain());assertTrue(healed.vitals().get(0).hp()<1000);
            f.elapsed(4*SECOND);var later=f.engine.tick();assertEquals(healed.items(),later.items());assertEquals(healed.regen().get(0).itemGain(),later.regen().get(0).itemGain());assertTrue(later.regen().get(0).hpGain()>healed.regen().get(0).hpGain());
            ModernRegenProtocol.validateTotals(later.regen().get(0),later.vitals().get(0),0,0);}
    }
    @Test void rawFoodTwoAndWeightAtTheNativeLimitSuppressWithoutSpuriousHpOrMpEvents()throws Exception {
        for(int mode=0;mode<2;mode++)try(var f=new Fixture(0,4,mode==0?2:225,mode==0?100:0.01,0,1000,false)) {
            f.active();if(mode==0)f.cast(0,1,4);var before=f.engine.tick();f.elapsed(100*SECOND);var after=f.engine.tick();
            assertEquals(before,after);assertTrue(after.regen().stream().allMatch(r->r.hpRevision()==0&&r.mpRevision()==0));
            if(mode==1)assertFalse(after.weights().get(0).snapshot().view().permitsNaturalRegen());}
    }
    @Test void successfulMovementUsesMoveCounterButTeleportAndCastingDoNotInventPhysicalActivity()throws Exception {
        try(var f=new Fixture()){f.active();f.attack(0,1);var p=f.prepared.get(1);var pos=f.engine.tick().actors().get(1).position();
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,pos.x(),pos.y(),0).status());
            f.elapsed(2*SECOND);assertEquals(0,f.engine.tick().regen().get(1).hpRevision());f.elapsed(3*SECOND);assertEquals(1,f.engine.tick().regen().get(1).hpGain());}
    }
    @Test void lateMeleeAndHealingOutcomesCannotOverwriteANewHpTickOrConsumeSupplies()throws Exception {
        for(int action=0;action<2;action++)try(var f=new Fixture()){f.active();f.attack(action==0?0:1,1);f.elapsed(2*SECOND-1);var before=f.engine.tick();f.jump=1;
            ModernBattleEngine.Snapshot after;
            if(action==0){var result=f.attack(0,2);assertEquals(ModernBattleEngine.AttackStatus.STALE_REGEN,result.status());after=result.snapshot();
                assertEquals(before.vitals().get(0).attackRevision(),after.vitals().get(0).attackRevision());}
            else{var result=f.heal(0);assertEquals(ModernItemProtocol.Reason.STALE_REGEN,result.reason());after=result.snapshot();assertEquals(before.items().get(0).stocks(),after.items().get(0).stocks());assertEquals(0,after.items().get(0).useCount());}
            int target=action==0?1:0;assertEquals(before.vitals().get(target).hp()+1,after.vitals().get(target).hp());assertEquals(before.revision()+2,after.revision());}
    }
    @Test void lateTeleportAndLightArrowUseTheIndependentMpAndTargetHpRevisionFences()throws Exception {
        for(int skill:new int[]{5,4})try(var f=new Fixture()){f.active();f.cast(0,1,4);f.elapsed(16*SECOND-1);var before=f.engine.tick();f.jump=1;
            var result=f.cast(0,2,skill);var after=result.snapshot();assertEquals(ModernSpellProtocol.Reason.STALE_REGEN,result.reason());
            assertEquals(30,after.vitals().get(0).mp());assertEquals(before.casts().get(0).castCount(),after.casts().get(0).castCount());assertEquals(before.damages(),after.damages());
            assertEquals(before.actors(),after.actors());assertEquals(2,after.casts().get(0).requestSequence());}
    }
    @Test void repeatedNaturalMpAllowsLegitimateCumulativeSpellSpendAboveInitialMaximum()throws Exception {
        try(var f=new Fixture()){f.active();ModernBattleEngine.Snapshot last=null;
            for(int n=1;n<=11;n++){f.elapsed((n-1)*16*SECOND);var result=f.cast(0,n,4);assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());last=result.snapshot();}
            assertEquals(33,last.damages().get(0).mpSpent());assertTrue(last.damages().get(0).mpSpent()>last.vitals().get(0).maxMp());
            ModernRegenProtocol.validateTotals(last.regen().get(0),last.vitals().get(0),0,33);}
    }
    @Test void slowOrBadRegenRandomAbortsWithoutPublishingPartialClockHpMpOrLedger()throws Exception {
        for(boolean invalid:new boolean[]{false,true})try(var f=new Fixture()){f.active();f.attack(0,1);var before=f.engine.tick();f.elapsed(2*SECOND);f.jump=1800*SECOND;f.invalid=invalid;
            var after=f.engine.tick();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,after.abortReason());
            assertEquals(before.vitals(),after.vitals());assertEquals(before.regen(),after.regen());assertEquals(before.items(),after.items());}
    }
    @Test void secondActorInvalidRandomBeforeDeadlineDoesNotCommitTheFirstActorsCandidateRecovery()throws Exception {
        try(var f=new Fixture()){f.active();f.attack(0,1);f.attack(1,1);var before=f.engine.tick();
            assertTrue(before.vitals().stream().allMatch(v->v.hp()>0&&v.hp()<v.maxHp()));
            int priorDraws=f.draws.get();f.failAtDraw=priorDraws+2;f.elapsed(3*SECOND);var after=f.engine.tick();
            assertEquals(priorDraws+2,f.draws.get());assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,after.abortReason());
            assertEquals(before.vitals(),after.vitals());assertEquals(before.regen(),after.regen());
            assertEquals(before.items(),after.items());assertEquals(before.casts(),after.casts());assertEquals(before.damages(),after.damages());}
    }
    @Test void signedNanoOriginWrapIsHarmlessAndLogicalRegressionStillAborts()throws Exception {
        try(var f=new Fixture(0,4,225,100,0,1000,false,Long.MAX_VALUE-SECOND)){f.active();f.cast(0,1,4);f.elapsed(16*SECOND);
            var wrapped=f.engine.tick();assertTrue(f.clock.get()<0);assertEquals(ModernBattleEngine.Phase.ACTIVE,wrapped.phase());assertEquals(3,wrapped.regen().get(0).mpGain());
            f.clock.addAndGet(-1);assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.engine.tick().abortReason());}
    }
    @Test void botHasNoSourceRecoveryAndDeathStopsAllFurtherRegenWithoutChangingSettlement()throws Exception {
        try(var f=new Fixture(0,4,225,100,1,1,false)){f.active();f.cast(0,1,4);f.elapsed(16*SECOND);var after=f.engine.tick();var bot=after.regen().get(1);
            assertEquals(ModernRegenProtocol.State.initial(2).damaged(bot.damageLoss()),bot);assertTrue(bot.damageLoss()>0);}
        try(var f=new Fixture(0,4,225,100,0,1,true)){f.active();var done=f.cast(0,1,4).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());f.elapsed(1600*SECOND);
            assertEquals(done,f.engine.tick());assertEquals(2,done.result().participants().size());}
    }
}
