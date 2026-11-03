package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Internal full-VIP PvP composition. No permission to export legacy frames. */
class ModernBattleEngineVipAdvancedTest {
    static final long SECOND=1_000_000_000L;
    static final Map<ModernVipEquipment.Field,Long> FIELDS=Map.of(INT,30L,SP,5L,FIRE,30L,
            DAMAGE_REDUCTION,23L,MAGIC_REDUCTION,29L);
    @TempDir Path directory;
    record Reply(String reason,ModernBattleObservation.Capture observation) {}

    @Test void bothDamageSkillsKeepHistoricalPricesAndLawfulWhenVipExpires()throws Exception {
        for(int skill:List.of(70,77))try(var f=fixture(4,2,false,10000,1000,500,0,b->0)) {
            f.active();var cast=cast(f,0,1,skill);assertEquals("NONE",cast.reason());
            var before=actor(cast.observation(),0);var old=before.histories().get(lane(skill));
            elapsed(f,4);var after=actor(f.view(),0);var current=after.histories().get(lane(skill));
            assertEquals(old.last(),current.last());assertEquals(old.current().spent(),current.current().spent());
            assertEquals(old.current().cost()+(skill==70?7:9),current.current().cost());
            assertEquals(1,current.current().equipmentRevision());assertEquals(before.runtime(),after.runtime());
            assertEquals(before.resources().flows().spent(),after.resources().flows().spent());
            if(skill==77) {
                assertEquals("TOO_FAST",cast(f,0,2,skill).reason());elapsed(f,5);
                assertEquals("NONE",cast(f,0,3,skill).reason());
            }else assertEquals("NONE",cast(f,0,2,skill).reason());
            var last=actor(f.view(),0);assertEquals(old.current().cost()+current.current().cost(),last.resources().flows().spent());
            assertEquals(2,last.histories().get(lane(skill)).current().casts());
            assertEquals(skill==77?502:500,last.runtime().lawful());
            assertEquals(500,last.loadout().fighter().profile().source().persisted().lawful());
        }
    }
    @Test void fireBatchContainsBothEnemiesWithFullVipResistanceAndNoFriendlyDamage()throws Exception {
        try(var f=fixture(-1,4,false,10000,1000,500,0,b->0)) {
            var before=f.active();var caster=actor(before,0);var target=actor(before,1);
            var expected=ModernFireStorm.resolve(caster.loadout().fireStorm().values(),target.loadout().fireStorm().values(),10000,b->0,0);
            assertEquals(30,target.loadout().fireStorm().values().fireResistance());
            var out=cast(f,0,1,70);assertEquals("NONE",out.reason());var after=out.observation();
            var use=actor(after,0).runtime().receipts().get(lane(70));
            assertEquals(List.of(2L,4L),use.batch().targets().stream().map(e->((ModernTargetHistory.Participant)e.before().target()).entityId()).toList());
            assertEquals(List.of(42,63),use.batch().targets().stream().map(e->((ModernTargetHistory.Participant)e.before().target()).identity().sourceId()).toList());
            for(var e:use.batch().targets())assertEquals(expected.appliedDamage(),e.applied());
            assertEquals(actor(before,2).resources(),actor(after,2).resources());
            assertEquals(use.batch().use().cost(),caster.resources().vitals().mp()-actor(after,0).resources().vitals().mp());
        }
    }
    @Test void expiryDuringEitherSpellDiscardsEffectsLawfulAndSequenceThenSameSequenceCanRetry()throws Exception {
        for(int skill:List.of(70,77))for(boolean wallOnly:List.of(false,true)) {
            var trigger=new AtomicReference<Runnable>();
            try(var f=fixture(4,2,false,10000,1000,500,0,b->{var r=trigger.getAndSet(null);if(r!=null)r.run();return 0;})) {
                var before=f.active();trigger.set(()->{if(wallOnly)f.epoch.set(5000);else elapsed(f,4);});
                var rejected=cast(f,0,1,skill);assertEquals("NOT_ACTIVE",rejected.reason());assertNull(trigger.get());
                var after=rejected.observation();assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());
                assertEquals(actor(before,0).runtime(),actor(after,0).runtime());
                assertEquals(0,actor(after,0).resources().flows().spent());assertEquals(0,actor(after,1).regeneration().damageLoss());
                assertEquals(1,actor(after,0).resources().equipmentRevision());
                assertEquals("NONE",cast(f,0,1,skill).reason());
            }
        }
    }
    @Test void secondTargetExpiryDoesNotPublishTheFirstFireStormHit()throws Exception {
        var count=new AtomicInteger();var trigger=new AtomicReference<Runnable>();
        try(var f=fixture(4,4,false,10000,1000,500,0,b->{if(count.incrementAndGet()==11)trigger.get().run();return 0;})) {
            f.active();trigger.set(()->elapsed(f,4));var out=cast(f,0,1,70);assertEquals("NOT_ACTIVE",out.reason());
            for(var a:out.observation().actors()) {assertEquals(0,a.regeneration().damageLoss());assertEquals(0,a.resources().flows().spent());}
            assertEquals(0,actor(out.observation(),0).runtime().sequence());
            assertEquals("NONE",cast(f,0,1,70).reason());
        }
    }
    @Test void everyInvalidSpellDrawRejectsTheWholeBatchWithoutSpendingOrChangingLawful()throws Exception {
        for(int skill:List.of(70,77))for(int invalidAt=1;invalidAt<=(skill==70?20:11);invalidAt++) {
            var count=new AtomicInteger();int bad=invalidAt;
            try(var f=fixture(-1,skill==70?4:2,false,10000,1000,500,0,b->count.incrementAndGet()==bad?-1:0)) {
                var before=f.active();var out=cast(f,0,1,skill);assertEquals("NUMERIC",out.reason());assertEquals(bad,count.get());
                for(int i=0;i<before.actors().size();i++) {
                    var a=actor(out.observation(),i);assertEquals(actor(before,i).resources(),a.resources());
                    assertEquals(actor(before,i).regeneration(),a.regeneration());assertEquals(500,a.runtime().lawful());
                }
                assertEquals(1,actor(out.observation(),0).runtime().sequence());
                assertThrows(SecurityException.class,()->cast(f,0,1,skill));
            }
        }
    }
    @Test void killsCommitOneSourceScopedSettlementAndNeverRecoverTheDeadAfterFinish()throws Exception {
        for(int skill:List.of(70,77))try(var f=fixture(4,skill==70?4:2,true,1,1000,500,225,b->0)) {
            f.active();var out=cast(f,0,1,skill);assertEquals("NONE",out.reason());var c=out.observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,c.phase());assertNotNull(c.result());
            assertEquals(c.revision(),c.result().engineRevision());assertEquals(c.actors().size(),c.result().participants().size());
            for(int i=1;i<c.actors().size();i+=2){assertEquals(0,actor(c,i).resources().vitals().hp());assertEquals(c.revision(),actor(c,i).vital().deathRevision());}
            elapsed(f,10);assertEquals(c,f.view());assertEquals(c,cast(f,0,2,skill).observation());
        }
    }
    @Test void oldReceiptAndSpentMpSurviveRealDurableUnequipAfterEitherSpell()throws Exception {
        for(int skill:List.of(70,77))try(var f=fixture(-1,2,false,10000,1000,500,0,b->0)) {
            f.active();try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(java.nio.file.Files.createDirectory(directory.resolve("spell"+skill)),new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var cast=cast(f,0,1,skill);assertEquals("NONE",cast.reason());during.set(cast.observation());
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    var out=change.publish(ModernBattleEngineVipTest.commit(store,change,candidate));assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,out.disposition());
                    var before=actor(during.get(),0);var after=actor(f.view(),0);
                    assertEquals(before.runtime(),after.runtime());assertEquals(before.histories().get(lane(skill)).last(),after.histories().get(lane(skill)).last());
                    assertEquals(before.resources().vitals().mp(),after.resources().vitals().mp());assertEquals(before.resources().flows().spent(),after.resources().flows().spent());
                    assertEquals(1,after.resources().equipmentRevision());assertEquals(1,after.loadout().fighter().vip().state().ledger().equipmentRevision());
                    assertEquals(actor(during.get(),1),actor(f.view(),1));
                }
            }
        }
    }
    @Test void independentRegenDuringSpellCalculationRejectsOldDamageWithoutUndoingRecovery()throws Exception {
        for(int skill:List.of(70,77)) {
            var trigger=new AtomicReference<Runnable>();
            try(var f=fixture(-1,2,false,10000,1000,500,225,b->{var r=trigger.getAndSet(null);if(r!=null)r.run();return 0;})) {
                f.active();assertEquals("NONE",cast(f,0,1,4).reason());elapsedNanos(f,3*SECOND-1);var before=f.view();
                trigger.set(()->elapsed(f,3));var out=cast(f,0,2,skill);assertEquals("STALE_REGEN",out.reason());
                assertEquals(actor(before,0).resources().flows().spent(),actor(out.observation(),0).resources().flows().spent());
                assertEquals(actor(before,1).regeneration().damageLoss(),actor(out.observation(),1).regeneration().damageLoss());
                assertTrue(actor(out.observation(),1).resources().vitals().hp()>actor(before,1).resources().vitals().hp());
                assertEquals(2,actor(out.observation(),0).runtime().sequence());
            }
        }
    }
    @Test void lawGateAndOverflowDoNotBorrowVipStatsOrConsumeRandomness()throws Exception {
        for(long lawful:List.of(499L,Long.MAX_VALUE)) {
            var count=new AtomicInteger();try(var f=fixture(-1,2,false,10000,1000,lawful,0,b->{count.incrementAndGet();return 0;})) {
                var before=f.active();var out=cast(f,0,1,77);assertEquals(lawful==499?"LAWFUL":"NUMERIC",out.reason());
                assertEquals(0,count.get());assertEquals(actor(before,0).resources(),actor(out.observation(),0).resources());
                assertEquals(lawful,actor(out.observation(),0).runtime().lawful());
            }
        }
    }
    @Test void fullI64VipAbilitiesReachBothDamageSkillsWithoutNativeNarrowing()throws Exception {
        for(int skill:List.of(70,77))try(var f=fixture(-1,2,false,1L<<45,1L<<34,500,0,b->0,Map.of(INT,1L<<33))) {
            var before=f.active();var out=cast(f,0,1,skill);assertEquals("NONE",out.reason());
            var receipt=actor(out.observation(),0).runtime().receipts().get(lane(skill));var hit=receipt.batch().targets().get(0);
            assertTrue(hit.applied()>Integer.MAX_VALUE);assertEquals(hit.applied(),actor(before,1).resources().vitals().hp()-actor(out.observation(),1).resources().vitals().hp());
            assertTrue(actor(out.observation(),0).resources().vitals().mp()>Integer.MAX_VALUE);
        }
    }
    @Test void sourceScopeRemainsIndependentOfIdenticalObjectIds()throws Exception {
        try(var f=fixture(-1,2,false,10000,1000,500,0,b->0)) {
            var before=f.active();var p=f.players.get(0);var other=f.players.get(1);
            var request=new ModernDisintegrateState.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),1,1,2,1);
            assertThrows(SecurityException.class,()->f.engine.disintegrate(other.owner(),request));
            assertThrows(SecurityException.class,()->f.engine.disintegrate(p.owner(),new ModernDisintegrateState.Request(other.scope(),f.ticket.matchId(),f.ticket.generation(),1,1,2,1)));
            assertEquals(before,f.view());assertThrows(IllegalArgumentException.class,()->f.engine.tick());
        }
    }
    @Test void casterMovementDuringFireCalculationCannotTurnOldRadiusIntoNewOrigin()throws Exception {
        var trigger=new AtomicReference<Runnable>();
        try(var f=fixture(-1,2,false,10000,1000,500,0,b->{var r=trigger.getAndSet(null);if(r!=null)r.run();return 0;})) {
            var before=f.active();trigger.set(()->{
                var p=f.players.get(0);var cell=actor(before,0).pose().position();
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,cell.x(),cell.y(),0).status());
            });
            var out=cast(f,0,1,70);assertEquals("STALE_REGEN",out.reason());
            assertEquals(actor(before,1).resources(),actor(out.observation(),1).resources());
            assertEquals(0,actor(out.observation(),0).resources().flows().spent());
        }
    }

    @Test void durableUnequipInsideEitherSpellRandomCannotCommitObsoleteDamageOrPrice()throws Exception {
        for(int skill:List.of(70,77)) {
            var trigger=new AtomicReference<Runnable>();
            try(var f=fixture(-1,2,false,10000,1000,500,0,b->{var r=trigger.getAndSet(null);if(r!=null)r.run();return 0;})) {
                var before=f.active();try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0);
                        var store=new ModernEquipmentOverrideStore(java.nio.file.Files.createDirectory(directory.resolve("random"+skill)))) {
                    var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                    trigger.set(()->{try {
                        var out=change.publish(ModernBattleEngineVipTest.commit(store,change,candidate));
                        assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,out.disposition());
                    }catch(Exception failure){throw new RuntimeException(failure);}});
                    var out=cast(f,0,1,skill);assertEquals("NOT_ACTIVE",out.reason());assertNull(trigger.get());
                    var after=actor(out.observation(),0);assertEquals(1,after.resources().equipmentRevision());
                    assertEquals(1,after.loadout().fighter().vip().state().ledger().equipmentRevision());
                    assertEquals(actor(before,0).runtime(),after.runtime());assertEquals(0,after.resources().flows().spent());
                    assertEquals(actor(before,1),actor(out.observation(),1));assertEquals("NONE",cast(f,0,1,skill).reason());
                }
            }
        }
    }
    @Test void lateRandomOrCancelledRoomCannotPublishASpellOrSettlement()throws Exception {
        for(int skill:List.of(70,77))for(boolean cancel:List.of(false,true)) {
            var trigger=new AtomicReference<Runnable>();
            try(var f=fixture(-1,2,true,1,1000,500,0,b->{var r=trigger.getAndSet(null);if(r!=null)r.run();return 0;})) {
                var before=f.active();trigger.set(()->{if(cancel)f.rooms.rooms.releasePreparation(f.ticket);else elapsed(f,60);});
                var out=cast(f,0,1,skill);assertEquals("NOT_ACTIVE",out.reason());var after=out.observation();
                assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertNull(after.result());
                assertEquals(before.actors(),after.actors());
            }
        }
    }
    @Test void emptyFireStormStillCostsMpAndUsesSharedSequenceAndTwoSecondReuse()throws Exception {
        var count=new AtomicInteger();try(var f=fixture(-1,2,false,10000,1000,500,0,b->{count.incrementAndGet();return 0;})) {
            f.active();for(int i=1;i<=4;i++) {
                elapsed(f,i);var p=f.players.get(0);var cell=actor(f.view(),0).pose().position();
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),i,cell.x(),cell.y(),4).status());
            }
            elapsed(f,5);var out=cast(f,0,1,70);assertEquals("NONE",out.reason());
            var a=actor(out.observation(),0);assertTrue(a.runtime().receipts().get(lane(70)).batch().targets().isEmpty());
            assertEquals(a.histories().get(lane(70)).current().cost(),a.resources().flows().spent());assertEquals(0,count.get());
            assertEquals("TOO_FAST",cast(f,0,2,70).reason());elapsed(f,7);assertEquals("NONE",cast(f,0,3,70).reason());
            assertEquals(2,actor(f.view(),0).histories().get(lane(70)).current().casts());
        }
    }

    @Test void expiryRechecksInsufficientMpBeforeDrawingOrSpendingEitherSpell()throws Exception {
        for(int skill:List.of(70,77)) {
            var count=new AtomicInteger();try(var f=fixture(4,2,false,10000,skill==70?45:65,500,0,b->{count.incrementAndGet();return 0;})) {
                var before=f.active();var a=actor(before,0);assertTrue(a.resources().vitals().mp()>=a.histories().get(lane(skill)).current().cost());
                elapsed(f,4);var out=cast(f,0,1,skill);assertEquals("NO_MP",out.reason());
                var after=actor(out.observation(),0);assertTrue(after.resources().vitals().mp()<after.histories().get(lane(skill)).current().cost());
                assertEquals(0,count.get());assertEquals(0,after.resources().flows().spent());assertEquals(500,after.runtime().lawful());
            }
        }
    }
    @Test void completeVipDoesNotInventLearnedSkillsOrAcceptAnUnsupportedSkillTemplate()throws Exception {
        for(var status:List.of(ModernDisintegrateCatalog.Status.UNLEARNED,ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE)) {
            var count=new AtomicInteger();
            try(var f=new ModernBattleEngineVipTest.Fixture(false,FIELDS,new AtomicLong(1000),b->{count.incrementAndGet();return 0;},
                    s->source(s,-1,10000,1000,500,0),p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->advanced(p,status,false),SECOND)) {
                var before=f.active();assertEquals("UNLEARNED",cast(f,0,1,70).reason());
                assertEquals(status==ModernDisintegrateCatalog.Status.UNLEARNED?"UNLEARNED":"UNSUPPORTED",cast(f,0,2,77).reason());
                assertEquals(0,count.get());assertEquals(actor(before,0).resources(),actor(f.view(),0).resources());
                assertEquals(2,actor(f.view(),0).runtime().sequence());
            }
        }
    }

    static ModernBattleEngineVipTest.Fixture fixture(int expiry,int humans,boolean settlement,long hp,long mp,long lawful,long food,
            ModernPhysicalCombat.BoundedRandom random)throws Exception {return fixture(expiry,humans,settlement,hp,mp,lawful,food,random,FIELDS);}
    static ModernBattleEngineVipTest.Fixture fixture(int expiry,int humans,boolean settlement,long hp,long mp,long lawful,long food,
            ModernPhysicalCombat.BoundedRandom random,Map<ModernVipEquipment.Field,Long> fields)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,fields,new AtomicLong(1000),random,s->source(s,expiry,hp,mp,lawful,food),
                p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),ModernBattleEngineVipAdvancedTest::advanced,SECOND,0,humans,settlement);
    }
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source s,int expiry,long hp,long mp,long lawful,long food) {
        s=ModernBattleEngineVipRegenTest.timed(s,expiry,-1,SECOND,hp);var q=s.input().equipment();var p=q.source().persisted();
        var v=new ModernCharacterData.PersistedValues(hp,mp,hp,mp,p.armorClass(),p.strength(),p.dexterity(),p.constitution(),12,p.wisdom(),p.charisma(),p.experience(),lawful,
                p.originalStrength(),p.originalDexterity(),p.originalConstitution(),p.originalIntelligence(),p.originalWisdom(),food);
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),v,q.source().inventory());
        return ModernVipLoadoutTest.withEquipment(s,new ModernEquipmentFacts.Qualified(row,q.items(),q.catalog(),q.lifetimes()));
    }
    static ModernBattleEngine.PreparedMember advanced(ModernBattleEngine.PreparedMember p) {
        return advanced(p,ModernDisintegrateCatalog.Status.QUALIFIED,true);
    }
    static ModernBattleEngine.PreparedMember advanced(ModernBattleEngine.PreparedMember p,ModernDisintegrateCatalog.Status status,boolean fireLearned) {
        var learned=new ArrayList<>(p.skills().skills());if(fireLearned)learned.add(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),learned),p.castIntervalNanos(),true,p.matchItems(),p.weightBasis(),false,false,true,true,
                new ModernDisintegrateCatalog.Snapshot(p.fighter().key(),status),null,null,null,null,false,true);
    }
    static ModernUseHistory.Lane lane(int skill){return skill==70?ModernUseHistory.Lane.FIRE_STORM:ModernUseHistory.Lane.DISINTEGRATE;}
    static Reply cast(ModernBattleEngineVipTest.Fixture f,int who,long sequence,int skill) {
        var p=f.players.get(who);long target=who==0?2:1;
        if(skill==77){var r=f.engine.disintegrate(p.owner(),new ModernDisintegrateState.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,1,target,1));return new Reply(r.reason().name(),r.observation());}
        var r=f.engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,skill,skill==4?target:0));return new Reply(r.reason().name(),r.observation());
    }
    static void elapsed(ModernBattleEngineVipTest.Fixture f,long seconds){elapsedNanos(f,seconds*SECOND);}
    static void elapsedNanos(ModernBattleEngineVipTest.Fixture f,long nanos){f.clock.set(SECOND+nanos);f.epoch.set(1000+nanos/1_000_000);}
}
