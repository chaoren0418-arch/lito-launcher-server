package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernBattleEngineVipRegenTest {
    @TempDir Path directory;
    static final long SECOND=1_000_000_000L;
    static final Map<ModernVipEquipment.Field,Long> FIELDS=Map.of(HPR,10L,MPR,20L);

    @Test void exactExpiryTickUsesNewHprWhileEarlierTicksRetainTheOldAmount()throws Exception {
        for(int expires:List.of(3,4,7))try(var f=fixture(expires,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=actor(f.view(),0);long amount=ModernNaturalRegen.hpAmount(before.loadout().regen().values(),1);
            elapsed(f,6);var after=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());var a=actor(after,0);
            long plain=amount-10;long expected=expires<=3?plain*2:expires<=6?amount+plain:amount*2;
            assertEquals(expected,a.regeneration().hp().gain());
            assertEquals(expires<=6?1:0,a.resources().equipmentRevision());
            assertEquals(0,a.loadout().fighter().vip().state().ledger().equipmentRevision());assertEquals(after,f.view());
        }
    }
    @Test void mpTickAtSixteenSecondsDoesNotUseExpiredBonusOrResetItsCounter()throws Exception {
        for(int expires:List.of(16,17))try(var f=fixture(expires,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1,5).reason());
            elapsed(f,2);assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,2,5).reason());
            var before=actor(f.view(),0);assertEquals(30,before.resources().vitals().mp());
            elapsed(f,16);var a=actor(f.view(),0);
            assertEquals(expires==16?4:20,a.regeneration().mp().gain());
            assertEquals(before.runtime(),a.runtime());assertEquals(20,a.resources().flows().spent());
            assertEquals(17*SECOND,a.regenClock().nextSecondElapsedNanos());
        }
    }
    @Test void multipleDeadlinesAndLongCatchupMatchFrequentPolling()throws Exception {
        try(var late=fixture(4,8,FIELDS,SECOND,b->0,new AtomicLong(1000));
                var frequent=fixture(4,8,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            for(var f:List.of(late,frequent)){f.active();cast(f,1,1,4);cast(f,0,1,5);}
            for(int second=1;second<=32;second++){elapsed(frequent,second);frequent.view();}
            elapsed(late,32);var a=actor(late.view(),0);var b=actor(frequent.view(),0);
            assertEquals(a.resources().vitals(),b.resources().vitals());assertEquals(a.resources().flows(),b.resources().flows());
            assertEquals(a.regenClock(),b.regenClock());assertEquals(2,a.resources().equipmentRevision());
            assertEquals(0,a.weight().view().sourceGearWeight());assertEquals(a.regeneration().hp().gain(),b.regeneration().hp().gain());
        }
    }
    @Test void fullVitalsAdvanceCountersWithoutRandomRecoveryOrWorldRevision()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(-1,-1,FIELDS,SECOND,b->{draws.incrementAndGet();return 0;},new AtomicLong(1000))) {
            var before=f.active();elapsed(f,30);var after=f.view();assertEquals(before.revision(),after.revision());assertEquals(0,draws.get());
            assertEquals(before.actors().get(0).resources(),after.actors().get(0).resources());assertEquals(31*SECOND,actor(after,0).regenClock().nextSecondElapsedNanos());
        }
    }
    @Test void deathStaysDeadAcrossExpiryAndNeverGetsNaturalRecovery()throws Exception {
        try(var f=fixture(4,-1,Map.of(MAX_HP,-99L,HPR,100L,MPR,100L),SECOND,b->0,new AtomicLong(1000))) {
            f.active();f.attack(1,1,1);assertEquals(0,actor(f.view(),0).resources().vitals().hp());
            elapsed(f,20);var a=actor(f.view(),0);assertEquals(0,a.resources().vitals().hp());assertEquals(0,a.regeneration().hp().gain());
            assertEquals(1,a.resources().equipmentRevision());
        }
    }
    @Test void nanoTimeWrapUsesSourcePairAndWallRollbackStillAborts()throws Exception {
        long origin=Long.MAX_VALUE-SECOND;
        try(var f=fixture(4,-1,FIELDS,origin,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);f.clock.set(origin+6*SECOND);f.epoch.set(7000);
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().phase());assertEquals(1,actor(f.view(),0).resources().equipmentRevision());
            f.epoch.decrementAndGet();assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.view().abortReason());
        }
    }
    @Test void stalledWallCannotRenewSourceMonotonicLease()throws Exception {
        try(var f=fixture(4,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);f.clock.addAndGet(6*SECOND);
            var a=actor(f.view(),0);assertEquals(1,a.resources().equipmentRevision());assertEquals(11+1,a.regeneration().hp().gain());
        }
    }
    @Test void invalidSecondActorRandomPublishesNoFirstActorOrExpiryCandidate()throws Exception {
        var draws=new AtomicInteger();var fail=new AtomicInteger(-1);
        try(var f=fixture(4,-1,FIELDS,SECOND,b->draws.incrementAndGet()==fail.get()?-1:0,new AtomicLong(1000))) {
            f.active();cast(f,0,1,4);cast(f,1,1,4);var before=f.view();fail.set(draws.get()+3);elapsed(f,6);
            var after=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(before.actors(),after.actors());
        }
    }
    @Test void slowRandomPastMatchTimeoutCannotCommitEarlierSegments()throws Exception {
        var action=new AtomicReference<Runnable>();
        try(var f=fixture(4,-1,FIELDS,SECOND,b->{var a=action.getAndSet(null);if(a!=null)a.run();return 0;},new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=f.view();elapsed(f,6);action.set(()->elapsed(f,60));
            var after=f.view();assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,after.abortReason());assertEquals(before.actors(),after.actors());
        }
    }
    @Test void reentrantCommandDuringRegenCannotConsumeAnOperationFromHalfPlannedState()throws Exception {
        var action=new AtomicReference<Runnable>();
        try(var f=fixture(4,-1,FIELDS,SECOND,b->{var a=action.getAndSet(null);if(a!=null)a.run();return 0;},new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=actor(f.view(),0);action.set(()->cast(f,0,1,5));elapsed(f,6);
            var after=actor(f.view(),0);assertEquals(before.runtime(),after.runtime());assertEquals(0,after.resources().flows().spent());
            assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1,5).reason());
        }
    }
    @Test void recoveryDuringDurableSaveIsRetainedAndLaterTicksUseUnequippedAbilities()throws Exception {
        try(var f=fixture(-1,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=actor(f.view(),0);
            try(var change=reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        elapsed(f,3);during.set(f.view());ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    var out=change.publish(ModernBattleEngineVipTest.commit(store,change,candidate));
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,out.disposition());var published=actor(f.view(),0);
                    assertEquals(actor(during.get(),0).resources().vitals(),published.resources().vitals());
                    assertEquals(11,published.regeneration().hp().gain());assertEquals(1,published.resources().equipmentRevision());
                    elapsed(f,6);var after=actor(f.view(),0);assertEquals(before.resources().vitals().hp()+12,after.resources().vitals().hp());
                    assertEquals(12,after.regeneration().hp().gain());assertEquals(1,after.loadout().fighter().vip().state().ledger().equipmentRevision());
                }
            }
        }
    }
    @Test void expiryDuringRecoveryRandomDiscardsWholeCandidateThenCatchesUpOnRetry()throws Exception {
        var action=new AtomicReference<Runnable>();
        try(var f=fixture(4,-1,FIELDS,SECOND,b->{var a=action.getAndSet(null);if(a!=null)a.run();return 0;},new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=f.view();elapsed(f,3);action.set(()->elapsed(f,6));
            var deferred=f.view();assertEquals(before.actors(),deferred.actors());assertEquals(ModernBattleEngine.Phase.ACTIVE,deferred.phase());
            var after=actor(f.view(),0);assertEquals(12,after.regeneration().hp().gain());assertEquals(1,after.resources().equipmentRevision());
        }
    }
    @Test void signedNegativeBonusExpiresWithoutDeathOrRewritingLossTotals()throws Exception {
        try(var f=fixture(4,-1,Map.of(HPR,-10L,MPR,-20L),SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var before=actor(f.view(),0);elapsed(f,6);var a=actor(f.view(),0);
            assertEquals(before.resources().vitals().hp()-8,a.resources().vitals().hp());
            assertEquals(9,a.regeneration().hp().loss());assertEquals(1,a.regeneration().hp().gain());assertEquals(0,a.vital().deathRevision());
            assertEquals(before.vital().hurtRevision(),a.vital().hurtRevision());
        }
    }
    @Test void forwardWallJumpShortensLeaseAtObservationWithoutExtendingMonotonicBoundary()throws Exception {
        try(var f=fixture(16,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);elapsed(f,3);f.epoch.set(17000);
            var a=actor(f.view(),0);assertEquals(1,a.resources().equipmentRevision());assertEquals(1,a.regeneration().hp().gain());
            f.clock.addAndGet(3*SECOND);var after=actor(f.view(),0);assertEquals(2,after.regeneration().hp().gain());
        }
    }
    @Test void vipCapacityLossPausesCountersAndCarriedGearExpiryResumesThemWithoutRetroactiveTicks()throws Exception {
        var fields=Map.of(STR,1000L,HPR,10L);
        try(var f=new ModernBattleEngineVipTest.Fixture(false,fields,new AtomicLong(1000),b->0,s->timed(s,4,8,SECOND,10000),p->{
            var v=supplies(p,-1);var saved=p.fighter().profile().source().persisted();
            double plain=ModernInventoryWeight.maximumWeight(new ModernInventoryWeight.Parameters(p.character().characterClass(),saved.strength(),
                    saved.constitution(),saved.originalStrength(),saved.originalConstitution(),0,1));
            var basis=ModernInventoryWeight.qualify(p.fighter().profile(),1.5/plain);
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    v.skills(),v.castIntervalNanos(),true,List.of(),basis);
        },ModernBattleEngineVipRegenTest::withRegen,SECOND)) {
            f.active();cast(f,1,1,4);elapsed(f,4);var paused=actor(f.view(),0);
            assertFalse(paused.weight().view().permitsNaturalRegen());assertEquals(11,paused.regeneration().hp().gain());
            elapsed(f,7);assertEquals(11,actor(f.view(),0).regeneration().hp().gain());
            elapsed(f,8);var resumed=actor(f.view(),0);assertTrue(resumed.weight().view().permitsNaturalRegen());
            assertEquals(4,resumed.regenClock().hpCounter());assertEquals(11,resumed.regeneration().hp().gain());
            elapsed(f,10);var after=actor(f.view(),0);assertEquals(12,after.regeneration().hp().gain());assertEquals(2,after.resources().equipmentRevision());
        }
    }
    @Test void externalWeightDeadlineCannotBeExtendedByTheRegenTimeline()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,FIELDS,new AtomicLong(1000),b->0,s->timed(s,8,-1,SECOND,10000),
                p->supplies(p,5000),ModernBattleEngineVipRegenTest::withRegen,SECOND)) {
            f.active();cast(f,1,1,4);var before=f.view();elapsed(f,6);var after=f.view();
            assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(before.actors(),after.actors());
        }
    }
    @Test void delayedCountdownUsesPlannedActiveStartAndExpiryDoesNotGrantPreBattleRecovery()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,FIELDS,new AtomicLong(1000),b->0,s->timed(s,16,-1,SECOND,10000),
                p->supplies(p,-1),ModernBattleEngineVipRegenTest::withRegen,SECOND,5*SECOND)) {
            assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.active().phase());elapsed(f,15);
            var active=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,active.phase());assertEquals(10*SECOND,active.clock().activeElapsedNanos());
            assertEquals(0,actor(active,0).regeneration().hp().gain());assertEquals(0,actor(active,0).resources().equipmentRevision());
            cast(f,1,1,4);elapsed(f,18);var after=actor(f.view(),0);assertEquals(1,after.regeneration().hp().gain());assertEquals(1,after.resources().equipmentRevision());
        }
    }
    @Test void ordinaryPotionAndRegenRemainSeparateAcrossTheSameExpiry()throws Exception {
        try(var f=fixture(4,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var used=use(f,0,1);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var before=actor(used.observation(),0);elapsed(f,6);var after=actor(f.view(),0);
            assertEquals(before.supplies().stocks(),after.supplies().stocks());
            assertEquals(before.supplies().history().last(),after.supplies().history().last());
            assertEquals(before.supplies().requestSequence(),after.supplies().requestSequence());
            assertEquals(1,after.supplies().history().equipmentRevision());
            assertEquals(before.resources().flows().restored(),after.resources().flows().restored());
            assertEquals(before.regeneration().itemGain(),after.regeneration().itemGain());assertEquals(12,after.regeneration().hp().gain());
        }
    }
    @Test void slowRecoveryCannotBackdateTheFollowingMovementCooldown()throws Exception {
        var action=new AtomicReference<Runnable>();
        try(var f=fixture(-1,-1,FIELDS,SECOND,b->{var a=action.getAndSet(null);if(a!=null)a.run();return 0;},new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);var p=f.players.get(0);var cell=actor(f.view(),0).pose().position();
            elapsed(f,3);action.set(()->elapsed(f,5));
            var first=f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,cell.x(),cell.y(),0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,first.status());var moved=actor(first.observation(),0).pose().position();
            var second=f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,moved.x(),moved.y(),0);
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,second.status());assertEquals(moved,actor(second.observation(),0).pose().position());
        }
    }
    @Test void recoveryBeforeCommandCalculationDoesNotFalselyRetireItsFirstAttempt()throws Exception {
        try(var f=fixture(4,-1,FIELDS,SECOND,b->0,new AtomicLong(1000))) {
            f.active();cast(f,1,1,4);elapsed(f,6);
            var out=cast(f,0,1,5);assertEquals(ModernSpellProtocol.Reason.NONE,out.reason());
            var a=actor(out.observation(),0);assertEquals(12,a.regeneration().hp().gain());assertEquals(10,a.resources().flows().spent());
            assertEquals(1,a.runtime().sequence());assertEquals(1,a.resources().equipmentRevision());
        }
    }
    static void elapsed(ModernBattleEngineVipTest.Fixture f,long seconds){f.clock.set(SECOND+seconds*SECOND);f.epoch.set(1000+seconds*1000);}
    static ModernBattleEngineVipTest.Fixture fixture(int vipSeconds,int armorSeconds,Map<ModernVipEquipment.Field,Long> fields,
            long origin,ModernPhysicalCombat.BoundedRandom random,AtomicLong epoch)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,fields,epoch,random,s->timed(s,vipSeconds,armorSeconds,origin,fields.getOrDefault(MAX_HP,0L)<0?100:10000),
                p->supplies(p,-1),ModernBattleEngineVipRegenTest::withRegen,origin);
    }
    static ModernBattleEngine.PreparedMember withRegen(ModernBattleEngine.PreparedMember p) {
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),
                p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),
                false,false,true,false,null,null,null,null,null,false,true);
    }
    static ModernPhysicalCombat.Source timed(ModernPhysicalCombat.Source source,int vip,int armor,long origin,long hp) {
        var q=ModernCombatProfileTest.withVitals(source.input().equipment(),hp,50,hp,50,10);var facts=new HashMap<>(q.lifetimes().items());
        for(var e:Map.of(1L,vip,3L,armor).entrySet())if(e.getValue()>=0) {
            var item=q.items().get(e.getKey()).source();
            facts.put(e.getKey(),new ModernItemLifetime.Fact(e.getKey(),item.template().catalogId(),item.template().kind(),item.equipped(),
                    OptionalLong.of(0),true,OptionalLong.of(1),Optional.of(String.format("2026-10-01 00:00:%02d",e.getValue())),0,false));
        }
        return ModernVipLoadoutTest.withEquipment(source,new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),
                new ModernItemLifetime.Observation(q.source().sourceId(),q.source().character().objectId(),"2026-10-01 00:00:00",1000,origin,facts)));
    }
}
