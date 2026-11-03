package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual engine histories and independent save CAS, not a public VIP wire. */
class ModernBattleEngineVipSuppliesTest {
    @TempDir Path directory;

    @Test void derivedWeightDeadlineExpiresWithVipButExplicitDeadlineCannotBeExtended()throws Exception {
        for(boolean cap:List.of(false,true))try(var f=fixture(true,Map.of(STR,50L,CON,10L,MAX_HP,100L),UnaryOperator.identity(),cap?2000:-1)) {
            var before=f.active();var b=actor(before,0).loadout().weightBasis();assertEquals(2000,b.validUntilMillis());
            assertEquals(cap?2000:-1,b.retainedUntilMillis());f.epoch.set(2000);var after=f.view();
            if(cap) {assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(before.actors(),after.actors());}
            else {
                assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());var a=actor(after,0);
                assertEquals(-1,a.loadout().weightBasis().validUntilMillis());assertEquals(-1,a.loadout().weightBasis().retainedUntilMillis());
                assertTrue(a.weight().view().maximumWeight()<actor(before,0).weight().view().maximumWeight());
                assertEquals(actor(before,0).weight().view().totalWeight(),a.weight().view().totalWeight());
                assertEquals(actor(before,0).weight().units(),a.weight().units());assertEquals(1,a.resources().equipmentRevision());
            }
        }
    }
    @Test void earlierUnattributedWeightLeaseAbortsWithoutPretendingVipExpired()throws Exception {
        try(var f=fixture(true,Map.of(STR,50L),UnaryOperator.identity(),1500)) {
            var before=f.active();f.epoch.set(1500);var after=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(before.actors(),after.actors());assertEquals(0,actor(after,0).resources().equipmentRevision());
        }
    }
    @Test void expiredUnequippedSourceArmorRemovesCarriedWeightWithoutRemovingPermanentVip()throws Exception {
        try(var f=fixture(false,Map.of(STR,50L),s->ModernVipLifecycleTest.timed(s,1000,"2026-10-01 00:00:00",3),-1)) {
            var before=f.active();var old=actor(before,0);assertEquals(1,old.weight().view().sourceGearWeight());
            assertEquals(-1,old.loadout().fighter().profile().validUntilMillis());assertEquals(2000,old.loadout().weightBasis().validUntilMillis());
            f.epoch.set(2000);var after=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());var a=actor(after,0);
            assertEquals(0,a.weight().view().sourceGearWeight());assertEquals(old.weight().view().suppliesWeight(),a.weight().view().suppliesWeight());
            assertEquals(old.weight().view().maximumWeight(),a.weight().view().maximumWeight());
            assertEquals(50,a.loadout().fighter().vip().state().ledger().total().get(STR));
            assertEquals(0,a.loadout().fighter().vip().state().ledger().equipmentRevision());assertEquals(1,a.resources().equipmentRevision());
            assertEquals(after,f.view());
        }
    }
    @Test void independentVipAndCarriedGearDeadlinesAdvanceOnceEachInOrder()throws Exception {
        // Anchor the armor's one-second lease one second later: deadline 3000.
        try(var f=fixture(true,Map.of(STR,50L),s->laterArmor(s),-1)) {
            var first=f.active();assertEquals(2000,actor(first,0).loadout().weightBasis().validUntilMillis());
            f.epoch.set(2000);var middle=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,middle.phase());
            assertEquals(3000,actor(middle,0).loadout().weightBasis().validUntilMillis());assertEquals(1,actor(middle,0).weight().view().sourceGearWeight());
            f.epoch.set(3000);var last=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,last.phase());
            assertEquals(2,actor(last,0).resources().equipmentRevision());assertEquals(0,actor(last,0).weight().view().sourceGearWeight());
            assertEquals(0,actor(last,0).loadout().fighter().vip().state().ledger().equipmentRevision());assertEquals(last,f.view());
        }
    }
    @Test void potionConsumptionAndItsReceiptSurviveExpiryWithoutRestockingOrHealingAgain()throws Exception {
        try(var f=fixture(true,Map.of(MAX_HP,100L,STR,50L),UnaryOperator.identity(),-1)) {
            f.active();f.attack(1,1,1);var used=use(f,0,1);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var old=actor(used.observation(),0);assertEquals(2,old.supplies().stocks().get(0).remaining());
            f.epoch.set(2000);var a=actor(f.view(),0);
            assertEquals(old.supplies().stocks(),a.supplies().stocks());assertEquals(old.supplies().history().last(),a.supplies().history().last());
            assertEquals(old.resources().flows().restored(),a.resources().flows().restored());assertEquals(100,a.resources().vitals().hp());
            assertEquals(old.weight().view().suppliesWeight(),a.weight().view().suppliesWeight());
            assertEquals(old.weight().units(),a.weight().units());
            f.clock.addAndGet(2_000_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,use(f,0,2).reason());
            assertEquals(1,actor(f.view(),0).supplies().stocks().get(0).remaining());
        }
    }
    @Test void spellExpiryRetainsHistoricalMpAndFirstCirclePriceWhileChangingAbilityVersion()throws Exception {
        try(var f=fixture(true,Map.of(INT,40L,MAX_HP,10000L,MAX_MP,100L),UnaryOperator.identity(),-1)) {
            f.active();var cast=cast(f,0,1,4);assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());
            var old=actor(cast.observation(),0);var lane=ModernUseHistory.Lane.LIGHT_ARROW;var oldUse=old.histories().get(lane).last();
            f.epoch.set(2000);var expired=actor(f.view(),0);var history=expired.histories().get(lane);
            assertEquals(oldUse,history.last());assertEquals(old.histories().get(lane).current().cost(),history.current().cost());
            // Circle one uses original-stat reduction, not current INT.
            assertEquals(40,old.loadout().fighter().profile().effective().intelligence()-expired.loadout().fighter().profile().effective().intelligence());
            assertEquals(old.runtime(),expired.runtime());assertEquals(old.resources().flows().spent(),expired.resources().flows().spent());
            f.clock.addAndGet(2_000_000_000L);var next=cast(f,0,2,4);assertEquals(ModernSpellProtocol.Reason.NONE,next.reason());
            var after=actor(next.observation(),0);assertEquals(old.resources().flows().spent()+history.current().cost(),after.resources().flows().spent());
            assertEquals(2,after.histories().get(lane).current().casts());assertEquals(1,after.histories().get(lane).last().before().equipmentRevision());
        }
    }
    @Test void itemUseDuringDurableRenameIsRetainedByWeightedEquipmentPublication()throws Exception {
        try(var f=fixture(false,Map.of(MAX_HP,100L,STR,50L),UnaryOperator.identity(),-1)) {
            f.active();f.attack(1,1,1);
            try(var change=reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var out=use(f,0,1);assertEquals(ModernItemProtocol.Reason.NONE,out.reason());during.set(out.observation());
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    var committed=ModernBattleEngineVipTest.commit(store,change,candidate);var result=change.publish(committed);
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,result.disposition());
                    var a=actor(f.view(),0);var b=actor(during.get(),0);assertEquals(b.supplies().stocks(),a.supplies().stocks());
                    assertEquals(b.resources().flows().restored(),a.resources().flows().restored());assertEquals(100,a.resources().vitals().hp());
                    assertEquals(b.weight().view().suppliesWeight(),a.weight().view().suppliesWeight());
                    assertEquals(result.history().items().last(),a.supplies().history().last());assertNotNull(change.observation());
                }
            }
        }
    }
    @Test void expiryDuringArrowTeleportOrPotionRandomDrawCommitsNeitherObsoleteEffectNorSequence()throws Exception {
        for(int skill:List.of(0,4,5)) {
            boolean item=skill==0;
            var epoch=new AtomicLong(1000);var expire=new AtomicReference<Runnable>();
            try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,10000L,INT,40L),epoch,b->{var action=expire.getAndSet(null);if(action!=null)action.run();return 0;},
                    UnaryOperator.identity(),p->supplies(p,-1))) {
                f.active();if(item)f.attack(1,1,1);var before=f.view();expire.set(()->epoch.set(2000));
                var after=item?use(f,0,1).observation():cast(f,0,1,skill).observation();assertNull(expire.get());
                assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());
                var a=actor(after,0);var old=actor(before,0);
                assertEquals(old.resources().flows().spent(),a.resources().flows().spent());assertEquals(old.resources().flows().restored(),a.resources().flows().restored());
                assertEquals(old.runtime().sequence(),a.runtime().sequence());assertEquals(old.supplies().requestSequence(),a.supplies().requestSequence());
                assertEquals(old.supplies().stocks(),a.supplies().stocks());
                assertEquals(old.pose(),a.pose());
                if(item)assertEquals(ModernItemProtocol.Reason.NONE,use(f,0,1).reason());
                else assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1,skill).reason());
            }
        }
    }
    @Test void oldExplicitWeightCapAndNoVipFreezeRemainConservative()throws Exception {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"same",6,0,30);
        var source=ModernBattleEngineVipTest.source(7,row,Map.of(STR,50L),true);
        var old=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
        var basis=ModernInventoryWeight.qualify(old.profile(),1);assertEquals(-1,basis.retainedUntilMillis());
        var legacy=new ModernInventoryWeight.Basis(basis.source(),basis.equipmentRevision(),basis.sourceGearWeight(),basis.maximumWeight(),basis.sourceRate(),2000);
        assertEquals(2000,legacy.retainedUntilMillis());assertEquals(1500,legacy.retaining(1500).retainedUntilMillis());
        assertEquals(1500,legacy.retaining(1500).retaining(3000).validUntilMillis());
        assertThrows(IllegalArgumentException.class,()->basis.retaining(-2));
        assertThrows(IllegalArgumentException.class,()->new ModernInventoryWeight.Basis(basis.source(),0,1,100,1,3000,2000));
    }
    @Test void spellDuringRenamePublishesLatestMpAndPreservesItsOldAbilityAnchor()throws Exception {
        try(var f=fixture(false,Map.of(MAX_HP,10000L,MAX_MP,100L,INT,40L),UnaryOperator.identity(),-1)) {
            f.active();try(var change=reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var out=cast(f,0,1,4);assertEquals(ModernSpellProtocol.Reason.NONE,out.reason());during.set(out.observation());
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    var out=change.publish(ModernBattleEngineVipTest.commit(store,change,candidate));assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,out.disposition());
                    var before=actor(during.get(),0);var after=actor(f.view(),0);
                    assertEquals(before.histories().get(ModernUseHistory.Lane.LIGHT_ARROW).last(),after.histories().get(ModernUseHistory.Lane.LIGHT_ARROW).last());
                    assertEquals(before.runtime(),after.runtime());assertEquals(before.resources().flows().spent(),after.resources().flows().spent());
                    assertEquals(Math.min(50,before.resources().vitals().mp()),after.resources().vitals().mp());
                    assertEquals(1,after.resources().equipmentRevision());assertEquals(0,after.histories().get(ModernUseHistory.Lane.LIGHT_ARROW).last().before().equipmentRevision());
                    assertEquals(after.resources().vitals().mp(),after.loadout().fighter().profile().effective().initialMp());
                }
            }
        }
    }
    @Test void decreasedCapacityImmediatelyRechecksAttackAndSpellWeightGates()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(STR,1000L),new AtomicLong(1000),b->0,UnaryOperator.identity(),p->{
            var v=supplies(p,-1);var basis=v.weightBasis();var raw=ModernInventoryWeight.fromShop(basis,v.matchItems()).view().totalWeight();
            // This fixture has only VIP STR and unequipped armor; the ordinary
            // profile already includes legacy VIP STR, so it is not a baseline.
            var saved=p.fighter().profile().source().persisted();
            var unboosted=ModernInventoryWeight.maximumWeight(new ModernInventoryWeight.Parameters(p.character().characterClass(),
                    saved.strength(),saved.constitution(),saved.originalStrength(),saved.originalConstitution(),0,1));
            var tight=ModernInventoryWeight.qualify(p.fighter().profile(),raw*0.9/unboosted);
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    v.skills(),v.castIntervalNanos(),true,v.matchItems(),tight);
        })) {
            var before=f.active();assertTrue(actor(before,0).weight().view().permitsAttackCast());
            f.epoch.set(2000);var after=f.view();assertFalse(actor(after,0).weight().view().permitsAttackCast());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_HEAVY,f.attack(0,1,2).status());
            assertEquals(ModernSpellProtocol.Reason.TOO_HEAVY,cast(f,0,1,4).reason());
            assertEquals(0,actor(f.view(),0).resources().flows().spent());
            assertEquals(ModernItemProtocol.Reason.NONE,use(f,0,1).reason());
        }
    }
    @Test void twoSuccessiveExpiriesKeepEarlierReceiptsAndDoNotGrantMana()throws Exception {
        try(var f=fixture(true,Map.of(MAX_HP,10000L,MAX_MP,100L),ModernBattleEngineVipSuppliesTest::laterArmor,-1)) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1,4).reason());
            var before=actor(f.view(),0);f.epoch.set(2000);var first=actor(f.view(),0);f.epoch.set(3000);var second=actor(f.view(),0);
            assertEquals(2,second.resources().equipmentRevision());assertEquals(first.resources().vitals(),second.resources().vitals());
            assertEquals(before.runtime(),second.runtime());assertEquals(before.histories().get(ModernUseHistory.Lane.LIGHT_ARROW).last(),second.histories().get(ModernUseHistory.Lane.LIGHT_ARROW).last());
            assertEquals(before.resources().flows().spent(),second.resources().flows().spent());assertEquals(first.resources().flows().mpClipped(),second.resources().flows().mpClipped());
        }
    }
    static ModernBattleEngineVipTest.Fixture fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,
            UnaryOperator<ModernPhysicalCombat.Source> source,long retained)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(timed,fields,new AtomicLong(1000),b->0,source,p->supplies(p,retained));
    }
    static ModernBattleEngine.PreparedMember supplies(ModernBattleEngine.PreparedMember p,long cap) {
        var stocks=List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),3));
        var basis=ModernInventoryWeight.qualify(p.fighter().profile(),1);
        if(cap>=0)basis=basis.retaining(cap);
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),ModernBattleEngineBotTest.learned()),1_200_000_000,true,stocks,basis);
    }
    static ModernBattleEngine.EquipmentChange reserve(ModernBattleEngineVipTest.Fixture f,int index) {
        var p=f.players.get(index);var view=ModernBattleVipObservation.from(f.view()).forOwner(p.scope());
        var admission=f.engine.admitVipEquipmentChange(p.owner(),ModernBattleVipCommandState.from(view));
        assertEquals(ModernBattleVipCommandState.Rejection.NONE,admission.rejection());
        return admission.change();
    }
    static ModernBattleObservation.CapturedActor actor(ModernBattleObservation.Capture c,int index){return c.actors().get(index);}
    static ModernBattleEngine.ItemResult use(ModernBattleEngineVipTest.Fixture f,int index,long seq) {
        var p=f.players.get(index);return f.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),seq,40010));
    }
    static ModernBattleEngine.CastResult cast(ModernBattleEngineVipTest.Fixture f,int index,long seq,int skill) {
        var p=f.players.get(index);return f.engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),seq,skill,skill==5?0:index==0?2:1));
    }
    private static ModernPhysicalCombat.Source laterArmor(ModernPhysicalCombat.Source source) {
        var q=source.input().equipment();var facts=new HashMap<>(q.lifetimes().items());var item=q.items().get(3L).source();
        facts.put(3L,new ModernItemLifetime.Fact(3,item.template().catalogId(),item.template().kind(),false,OptionalLong.of(0),true,
                OptionalLong.of(2),Optional.of("2026-10-01 00:00:02"),0,false));
        return ModernVipLoadoutTest.withEquipment(source,new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),
                new ModernItemLifetime.Observation(q.source().sourceId(),q.source().character().objectId(),"2026-10-01 00:00:00",1000,0,facts)));
    }
}
