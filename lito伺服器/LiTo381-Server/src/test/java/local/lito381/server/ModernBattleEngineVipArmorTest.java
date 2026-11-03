package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Internal VIP/proc composition; synthetic catalogs do not qualify shared SPR
 * availability, player protocol, SQL writes, permissions or native UI. */
class ModernBattleEngineVipArmorTest {
    static final Map<ModernVipEquipment.Field,Long> ABILITIES=Map.of(INT,30L,SP,5L,HPR,4L,MPR,3L,
            MAX_HP,100L,MAX_MP,80L,DAMAGE_REDUCTION,0L,MAGIC_REDUCTION,1L);
    @TempDir Path directory;

    @Test void everyDamageLaneUsesOldProtectionAndNeverRefreshesItOnAnotherProc()throws Exception {
        for(int skill:List.of(0,4,70,77))try(var protectedWorld=fixture(-1,-1,2,false,10000,0,100,b->0);
                var control=fixture(-1,-1,2,false,10000,0,0,b->0)) {
            var initial=protectedWorld.active();control.active();
            assertTrue(initial.hasVip());assertThrows(IllegalArgumentException.class,initial::world);
            var first=hit(protectedWorld,0,1,0);var plain=hit(control,0,1,0);
            assertEquals("ATTACKED",first.reason());assertEquals("ATTACKED",plain.reason());
            assertEquals(hp(plain.observation(),1),hp(first.observation(),1));
            var lease=armor(first.observation(),1);assertEquals(3*SECOND,lease.state().untilNanos());
            assertFalse(lease.triggers().isEmpty());assertEquals(first.observation().revision(),lease.revision());
            assertEquals(actor(first.observation(),1).pose().position(),lease.anchor().cell());
            elapsed(protectedWorld,1);elapsed(control,1);
            long protectedHp=hp(protectedWorld.view(),1),plainHp=hp(control.view(),1);
            var reduced=hit(protectedWorld,0,skill==0?2:1,skill);var full=hit(control,0,skill==0?2:1,skill);
            assertEquals(skill==0?"ATTACKED":"NONE",reduced.reason());assertEquals(reduced.reason(),full.reason());
            long damage=plainHp-hp(full.observation(),1);assertTrue(damage>0);
            assertEquals(damage/2,protectedHp-hp(reduced.observation(),1));
            assertEquals(lease.state().untilNanos(),armor(reduced.observation(),1).state().untilNanos());
            assertEquals(3333,armor(reduced.observation(),1).state().actor().owner().objectId());
            assertEquals(42,armor(reduced.observation(),1).state().actor().owner().sourceId());
            assertEquals(actor(full.observation(),0).resources().flows().spent(),actor(reduced.observation(),0).resources().flows().spent());
        }
    }

    @Test void separateVipAndArmorExpiryRetainTheGrantedSpellButRemoveFutureTriggers()throws Exception {
        try(var f=fixture(1,2,2,false,10000,0,100,b->0)) {
            f.active();assertEquals("ATTACKED",hit(f,0,1,0).reason());var lease=armor(f.view(),1);
            elapsed(f,1);var vipExpired=f.view();assertEquals(1,actor(vipExpired,1).resources().equipmentRevision());
            assertEquals(lease,armor(vipExpired,1));assertEquals(1,rules(vipExpired,1));
            elapsed(f,2);var gearExpired=f.view();assertEquals(2,actor(gearExpired,1).resources().equipmentRevision());
            assertEquals(0,actor(gearExpired,1).loadout().fighter().vip().state().ledger().equipmentRevision());
            assertEquals(0,rules(gearExpired,1));assertEquals(lease,armor(gearExpired,1));
            assertEquals("ATTACKED",hit(f,0,2,0).reason());assertTrue(armor(f.view(),1).triggers().isEmpty());
            assertEquals(3*SECOND,armor(f.view(),1).state().untilNanos());
            elapsed(f,3);assertEquals(0,armor(f.view(),1).state().untilNanos());
            assertEquals("ATTACKED",hit(f,0,3,0).reason());assertEquals(0,armor(f.view(),1).state().untilNanos());
            assertTrue(actor(f.view(),1).loadout().fighter().vip().state().stored().isEmpty());
        }
    }

    @Test void realDurableUnequipAndReequipPreserveTheActiveLeaseWithoutHealing()throws Exception {
        try(var f=fixture(-1,-1,2,false,10000,0,100,b->0);var store=new ModernEquipmentOverrideStore(directory)) {
            f.active();hit(f,0,1,0);var before=f.view();var lease=armor(before,1);long hp=hp(before,1);
            publishArmor(f,1,false,store);var off=f.view();assertEquals(0,rules(off,1));assertEquals(lease,armor(off,1));
            long clipped=Math.min(hp,actor(off,1).resources().vitals().maximumHp());
            assertEquals(clipped,hp(off,1));assertEquals(hp-clipped,actor(off,1).resources().flows().hpClipped());
            assertEquals(1,actor(off,1).resources().equipmentRevision());
            assertEquals(0,actor(off,0).resources().equipmentRevision());
            elapsed(f,1);publishArmor(f,1,true,store);var on=f.view();assertEquals(1,rules(on,1));
            assertEquals(lease,armor(on,1));assertEquals(clipped,hp(on,1));assertEquals(2,actor(on,1).resources().equipmentRevision());
            assertEquals(2,actor(on,1).loadout().fighter().vip().state().ledger().equipmentRevision());
            hit(f,0,2,0);assertEquals(lease.state().untilNanos(),armor(f.view(),1).state().untilNanos());
            elapsed(f,3);assertEquals(0,armor(f.view(),1).state().untilNanos());
            hit(f,0,3,0);assertEquals(6*SECOND,armor(f.view(),1).state().untilNanos());
            try(var call=new ModernEquipmentOverrideStoreTest.Call(lease.state().actor().owner())) {
                var saved=store.read(lease.state().actor().owner(),call.scope).orElseThrow();
                assertEquals(2,saved.revision());assertTrue(saved.value().overrides().get(0).equipped());
            }
        }
    }

    @Test void aProcDuringDurableRenameSurvivesPublicationOfTheUnequippedArmor()throws Exception {
        for(int skill:List.of(0,4,70,77))try(var f=fixture(-1,-1,2,false,10000,0,100,b->0)) {
            f.active();try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,1)) {
                var candidate=armorCandidate(f,1,change,false);change.prepareVip(candidate,f.epoch.get());
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("rename"+skill)),new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var result=hit(f,0,1,skill);assertEquals(skill==0?"ATTACKED":"NONE",result.reason());during.set(result.observation());
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    var result=change.publish(ModernBattleEngineVipTest.commit(store,change,candidate));
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,result.disposition());var after=f.view();
                    assertEquals(armor(during.get(),1),armor(after,1));
                    assertEquals(Math.min(hp(during.get(),1),actor(after,1).resources().vitals().maximumHp()),hp(after,1));
                    assertEquals(0,rules(after,1));assertEquals(actor(during.get(),0),actor(after,0));
                }
            }
        }
    }

    @Test void gearExpiryDuringAnyDamageCalculationRejectsBothDamageAndProc()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(1,2,2,false,10000,0,100,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                var before=f.active();action.set(()->elapsed(f,2));var result=hit(f,0,1,skill);
                assertEquals("NOT_ACTIVE",result.reason());assertNull(action.get());var after=result.observation();
                assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());assertEquals(2,actor(after,1).resources().equipmentRevision());
                for(int i=0;i<2;i++) {
                    assertEquals(0,actor(after,i).resources().flows().damaged());assertEquals(0,actor(after,i).resources().flows().spent());
                    assertEquals(armor(before,i),armor(after,i));
                }
                assertEquals(skill==0?"ATTACKED":"NONE",hit(f,0,1,skill).reason());assertEquals(0,rules(f.view(),1));
            }
        }
    }

    @Test void existingProtectionExpiryDuringCalculationDoesNotRollAgainOrCommitOldHalfDamage()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var action=new AtomicReference<Runnable>();var draws=new AtomicInteger();
            try(var f=fixture(-1,-1,2,false,10000,0,100,b->{draws.incrementAndGet();var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                f.active();hit(f,0,1,0);elapsed(f,1);var before=f.view();draws.set(0);action.set(()->elapsed(f,3));
                var out=hit(f,0,skill==0?2:1,skill);assertEquals("STALE_EFFECT",out.reason());assertTrue(draws.get()>0);
                assertEquals(hp(before,1),hp(out.observation(),1));assertEquals(0,armor(out.observation(),1).state().untilNanos());
                assertEquals(0,actor(out.observation(),0).resources().flows().spent());
            }
        }
    }

    @Test void fourPlayerFireBatchPublishesOnlyEnemyProcsAndOneSourceScopedCost()throws Exception {
        try(var f=fixture(-1,-1,4,false,10000,0,100,b->0)) {
            var before=f.active();var out=hit(f,0,1,70);assertEquals("NONE",out.reason());var c=out.observation();
            for(int i:List.of(1,3)) {
                assertEquals(3*SECOND,armor(c,i).state().untilNanos());assertEquals(c.revision(),armor(c,i).revision());
                assertEquals(actor(c,i).pose().position(),armor(c,i).anchor().cell());
                assertEquals(f.players.get(i).scope().sourceId(),armor(c,i).state().actor().owner().sourceId());
            }
            for(int i:List.of(0,2))assertEquals(armor(before,i),armor(c,i));
            assertEquals(actor(before,2).resources(),actor(c,2).resources());
            assertEquals(actor(c,0).histories().get(lane(70)).current().cost(),actor(c,0).resources().flows().spent());
        }
    }

    @Test void everyBadRandomIncludingTheLastEnemyProcRejectsTheWholeBatch()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var count=new AtomicInteger();int calls;
            try(var f=fixture(-1,-1,skill==70?4:2,false,10000,0,100,b->{count.incrementAndGet();return 0;})) {
                f.active();assertEquals(skill==0?"ATTACKED":"NONE",hit(f,0,1,skill).reason());calls=count.get();assertTrue(calls>1);
            }
            for(int bad=1;bad<=calls;bad++) {
                int failureAt=bad;count.set(0);
                try(var f=fixture(-1,-1,skill==70?4:2,false,10000,0,100,b->count.incrementAndGet()==failureAt?-1:0)) {
                    var before=f.active();var out=hit(f,0,1,skill);assertEquals("NUMERIC",out.reason());assertEquals(bad,count.get());
                    for(int i=0;i<before.actors().size();i++) {
                        assertEquals(actor(before,i).resources(),actor(out.observation(),i).resources());
                        assertEquals(armor(before,i),armor(out.observation(),i));
                        assertEquals(actor(before,i).regeneration(),actor(out.observation(),i).regeneration());
                    }
                }
            }
        }
    }

    @Test void deathSettlementAndCancellationNeverLeaveAnActiveProtectionLease()throws Exception {
        for(int skill:List.of(0,4,70,77))try(var f=fixture(-1,-1,skill==70?4:2,true,1,0,100,b->0,Map.of(INT,30L,SP,5L,MAX_HP,-40L))) {
            f.active();var out=hit(f,0,1,skill);assertEquals(skill==0?"ATTACKED":"NONE",out.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,out.observation().phase());assertNotNull(out.observation().result());
            for(var a:out.observation().actors()) {assertEquals(0,a.armorProtection().state().untilNanos());assertTrue(a.armorProtection().triggers().isEmpty());}
            elapsed(f,10);assertEquals(out.observation(),f.view());
        }
        try(var f=fixture(-1,-1,2,false,10000,0,100,b->0)) {
            f.active();hit(f,0,1,0);assertTrue(armor(f.view(),1).state().untilNanos()>0);
            f.rooms.rooms.releasePreparation(f.ticket);var out=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,out.phase());
            assertEquals(0,armor(out,1).state().untilNanos());assertTrue(armor(out,1).triggers().isEmpty());
        }
    }

    @Test void vipReductionToZeroDoesNotInventDamageOrAnOnHurtProc()throws Exception {
        var fields=new HashMap<>(ABILITIES);fields.put(DAMAGE_REDUCTION,1L);
        try(var f=fixture(-1,-1,2,false,10000,0,100,b->0,fields)) {
            var before=f.active();var out=hit(f,0,1,0);assertEquals("ATTACKED",out.reason());
            assertEquals(hp(before,1),hp(out.observation(),1));assertEquals(0,armor(out.observation(),1).state().untilNanos());
            assertTrue(armor(out.observation(),1).triggers().isEmpty());
        }
    }

    @Test void durableArmorRemovalInsideEveryDamageLaneInvalidatesTheOldSelection()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(-1,-1,2,false,10000,0,100,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;});
                    var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("during"+skill)))) {
                var before=f.active();action.set(()->{try{publishArmor(f,1,false,store);}catch(Exception e){throw new AssertionError(e);}});
                var out=hit(f,0,1,skill);assertEquals("NOT_ACTIVE",out.reason());assertNull(action.get());
                assertEquals(0,actor(out.observation(),1).resources().flows().damaged());
                assertEquals(0,actor(out.observation(),0).resources().flows().spent());
                assertEquals(armor(before,1),armor(out.observation(),1));assertEquals(0,rules(out.observation(),1));
                assertEquals(skill==0?"ATTACKED":"NONE",hit(f,0,1,skill).reason());
                assertEquals(0,armor(f.view(),1).state().untilNanos());
            }
        }
    }

    @Test void secondEnemyGearChangeCannotPublishFirstEnemyDamageOrProc()throws Exception {
        var count=new AtomicInteger();int firstTargetDraws;
        try(var f=fixture(-1,-1,4,false,10000,0,100,b->{count.incrementAndGet();return 0;})) {
            f.active();assertEquals("NONE",hit(f,0,1,70).reason());assertEquals(0,count.get()%2);firstTargetDraws=count.get()/2;
        }
        count.set(0);var action=new AtomicReference<Runnable>();
        try(var f=fixture(-1,-1,4,false,10000,0,100,b->{if(count.incrementAndGet()==firstTargetDraws+1)action.getAndSet(null).run();return 0;});
                var store=new ModernEquipmentOverrideStore(directory)) {
            var before=f.active();action.set(()->{try{publishArmor(f,3,false,store);}catch(Exception e){throw new AssertionError(e);}});
            var out=hit(f,0,1,70);assertEquals("NOT_ACTIVE",out.reason());assertNull(action.get());
            for(int i=0;i<4;i++) {
                assertEquals(armor(before,i),armor(out.observation(),i));assertEquals(0,actor(out.observation(),i).resources().flows().damaged());
                assertEquals(0,actor(out.observation(),i).resources().flows().spent());
            }
            assertEquals(0,rules(out.observation(),3));assertEquals(1,rules(out.observation(),1));
            assertEquals("NONE",hit(f,0,1,70).reason());assertTrue(armor(f.view(),1).state().untilNanos()>0);
            assertEquals(0,armor(f.view(),3).state().untilNanos());
        }
    }

    @Test void naturalRecoveryAndPotionsKeepTheirOwnLedgersAndDoNotRefreshProtection()throws Exception {
        try(var f=fixture(4,5,2,false,10000,225,100,b->0)) {
            f.active();assertEquals("NONE",hit(f,0,1,4).reason());var lease=armor(f.view(),1);
            assertEquals(3*SECOND,lease.state().untilNanos());
            var used=ModernBattleEngineVipSuppliesTest.use(f,1,1);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var before=actor(used.observation(),1);assertEquals(lease,armor(used.observation(),1));
            elapsed(f,2);assertEquals(lease,armor(f.view(),1));
            elapsed(f,6);var after=actor(f.view(),1);assertEquals(2,after.resources().equipmentRevision());
            assertTrue(after.regeneration().hp().gain()>0);assertEquals(0,after.armorProtection().state().untilNanos());
            assertEquals(before.supplies().history().last(),after.supplies().history().last());
            assertEquals(before.resources().flows().restored(),after.resources().flows().restored());
            assertEquals(before.supplies().stocks(),after.supplies().stocks());assertEquals(0,rules(f.view(),1));
        }
    }

    @Test void aSlowFirstProcStartsAtAcceptanceWithoutBackdatingItsThreeSecondLease()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(-1,-1,2,false,10000,0,100,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                f.active();action.set(()->elapsed(f,1));var out=hit(f,0,1,skill);
                assertEquals(skill==0?"ATTACKED":"NONE",out.reason());
                assertEquals(4*SECOND,armor(out.observation(),1).state().untilNanos());
                assertEquals(SECOND,armor(out.observation(),1).state().observedNanos());
                elapsed(f,3);assertTrue(armor(f.view(),1).state().active(3*SECOND));
                elapsed(f,4);assertEquals(0,armor(f.view(),1).state().untilNanos());
            }
        }
    }

    @Test void meleeOnlyVipProtectionAlsoRebindsExpiryWithoutRequiringMagicOrRegen()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,ABILITIES,new AtomicLong(1000),b->0,
                s->sourceWithArmor(s,1,2,10000,0,100),p->p,p->{
                    var s=p.fighter().vip().source();return p.withVip(ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(s)));
                },SECOND)) {
            f.active();assertEquals("ATTACKED",hit(f,0,1,0).reason());var lease=armor(f.view(),1);
            elapsed(f,2);var after=f.view();assertEquals(1,actor(after,1).resources().equipmentRevision());
            assertEquals(lease,armor(after,1));assertEquals(0,rules(after,1));
            assertEquals("ATTACKED",hit(f,0,2,0).reason());elapsed(f,3);assertEquals(0,armor(f.view(),1).state().untilNanos());
        }
    }

    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,int humans,boolean settlement,long hp,long food,long chance,
            ModernPhysicalCombat.BoundedRandom random)throws Exception {return fixture(vip,gear,humans,settlement,hp,food,chance,random,ABILITIES);}
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,int humans,boolean settlement,long hp,long food,long chance,
            ModernPhysicalCombat.BoundedRandom random,Map<ModernVipEquipment.Field,Long> fields)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,fields,new AtomicLong(1000),random,
                s->sourceWithArmor(s,vip,gear,hp,food,chance),p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->{
                    var source=p.fighter().vip().source();
                    var fighter=ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(source));
                    return advanced(p.withVip(fighter));
                },SECOND,0,humans,settlement);
    }
    static ModernPhysicalCombat.Source sourceWithArmor(ModernPhysicalCombat.Source s,int vip,int gear,long hp,long food,long chance) {
        s=source(s,vip,hp,1000,500,food);var q=s.input().equipment();var items=new HashMap<>(q.items());
        var entry=items.get(3L);var old=entry.source();
        var item=new ModernCharacterData.InventoryItem(old.objectId(),old.template(),old.quantity(),true,old.enchantment(),old.durability(),old.identified(),old.blessing(),old.sealed(),old.chargeCount());
        items.put(3L,new ModernEquipmentFacts.Instance(item,entry.template(),entry.greater(),entry.dark(),entry.cards(),entry.stone()));
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),q.source().persisted(),
                items.values().stream().map(ModernEquipmentFacts.Instance::source).sorted(Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId)).toList());
        var life=q.lifetimes();var facts=new HashMap<>(life.items());var prior=facts.get(3L);
        facts.put(3L,new ModernItemLifetime.Fact(3,item.template().catalogId(),item.template().kind(),true,prior.remainingSeconds(),gear>=0,
                gear<0?OptionalLong.empty():OptionalLong.of(1),gear<0?Optional.empty():Optional.of(String.format("2026-10-01 00:00:%02d",gear)),prior.templateMaxUseSeconds(),prior.lighting()));
        var qualified=new ModernEquipmentFacts.Qualified(row,items,q.catalog(),new ModernItemLifetime.Observation(row.sourceId(),row.character().objectId(),
                life.dbNowLocal(),life.queryStartedEpochMillis(),life.queryStartedNanos(),facts,life.dbClockUncertaintyMillis()));
        var changed=ModernVipLoadoutTest.withEquipment(s,qualified);long catalog=item.template().catalogId();
        return ModernArmorProtectionLoadoutTest.withRules(changed,Set.of(),Map.of(catalog,List.of(ModernArmorProcsTest.rule(catalog,catalog,-1,88,0,chance))));
    }
    static Reply hit(ModernBattleEngineVipTest.Fixture f,int who,long sequence,int skill) {
        if(skill!=0)return cast(f,who,sequence,skill);
        var out=f.attack(who,sequence,who==0?2:1);return new Reply(out.status().name(),out.observation());
    }
    static long hp(ModernBattleObservation.Capture c,int who){return actor(c,who).resources().vitals().hp();}
    static ModernBattleEngine.ArmorProtectionView armor(ModernBattleObservation.Capture c,int who){return actor(c,who).armorProtection();}
    static int rules(ModernBattleObservation.Capture c,int who){return actor(c,who).loadout().fighter().armorProtection().select(armor(c,who).state().actor()).rules().size();}
    static ModernVipCharacterState.Candidate armorCandidate(ModernBattleEngineVipTest.Fixture f,int who,ModernBattleEngine.EquipmentChange change,boolean on) {
        var s=f.sources.get(who);var q=s.input().equipment();var state=change.vipState();long now=f.epoch.get();
        var fresh=ModernVipLoadout.qualifyForRebuild(s,ModernCombatProfileTest.binding(q),
                ModernCombatProfileTest.stored(q,state.ledger().equipmentRevision()+1,Map.of(3L,on)),now,0);
        return ModernVipCharacterState.prepareOrdinaryEquipment(state,fresh,now);
    }
    static void publishArmor(ModernBattleEngineVipTest.Fixture f,int who,boolean on,ModernEquipmentOverrideStore store)throws Exception {
        try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,who)) {
            var candidate=armorCandidate(f,who,change,on);change.prepareVip(candidate,f.epoch.get());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
        }
    }
}
