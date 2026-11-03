package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSlowCast.Reason.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import static local.lito381.server.ModernBattleEngineVipSpeedTest.speed;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Private full-VIP composition; not public admission or a visual/resource test. */
class ModernBattleEngineVipSlowTest {
    @TempDir Path directory;
    @Test void paidMaterialWeightAndOldReceiptSurviveBothVipAndArmorExpiry()throws Exception {
        try(var f=fixture(4,5,0,b->0,4)) {
            var initial=f.active();var first=slow(f,0,1,1);assertEquals(NONE,first.reason());
            var before=actor(first.observation(),0);var receipt=before.runtime().receipts().get(ModernUseHistory.Lane.SLOW);
            assertEquals(3,before.materials().stocks().get(0).remaining());
            assertEquals(actor(initial,0).weight().view().suppliesWeight()-2,before.weight().view().suppliesWeight());
            elapsed(f,6);var after=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());
            var a=actor(after,0);assertEquals(2,a.resources().equipmentRevision());
            assertEquals(before.materials(),a.materials());assertEquals(before.weight().view().suppliesWeight(),a.weight().view().suppliesWeight());
            assertEquals(receipt,a.runtime().receipts().get(ModernUseHistory.Lane.SLOW));
            assertEquals(17,before.histories().get(ModernUseHistory.Lane.SLOW).current().cost());
            assertEquals(20,a.histories().get(ModernUseHistory.Lane.SLOW).current().cost());
            assertEquals(actor(first.observation(),1).buffs(),actor(after,1).buffs());
            assertEquals(NONE,slow(f,0,2,1).reason());a=actor(f.view(),0);
            assertEquals(37,a.resources().flows().spent());assertEquals(2,a.materials().uses());
            assertEquals(2,a.materials().lastEquipmentRevision());assertEquals(0,a.loadout().fighter().vip().state().ledger().equipmentRevision());
            consistent(f.view());
        }
    }

    @Test void probabilityMissIsPaidButReplayCooldownAndEmptyStockCannotPayAgain()throws Exception {
        try(var f=fixture(4,5,0,b->b-1,1)) {
            f.active();var first=slow(f,0,1,1);assertEquals(NONE,first.reason());
            assertEquals(ModernSlowReceipt.Outcome.MISS,detail(first.observation(),0).outcome());
            assertEquals(17,actor(first.observation(),0).resources().flows().spent());
            assertThrows(SecurityException.class,()->slow(f,0,1,1));assertEquals(TOO_FAST,slow(f,0,2,1).reason());
            elapsed(f,6);var before=f.view();assertEquals(NO_MATERIAL,slow(f,0,3,1).reason());
            assertEquals(1,actor(before,0).materials().uses());assertEquals(0,actor(before,0).materials().stocks().get(0).remaining());
            assertEquals(actor(before,0).resources(),actor(f.view(),0).resources());consistent(f.view());
        }
    }

    @Test void oldEquipmentProofRejectsBeforeRandomOrMaterialAndCanRetryNextSequence()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(4,5,0,b->{draws.incrementAndGet();return 0;},3)) {
            var c=f.active();var req=request(c,0,1,1);elapsed(f,6);
            var out=f.engine.slow(f.players.get(0).owner(),req);assertEquals(STALE_STATE,out.reason());
            assertEquals(0,draws.get());assertEquals(0,actor(out.observation(),0).materials().uses());
            assertEquals(0,actor(out.observation(),0).resources().flows().spent());
            assertEquals(NONE,slow(f,0,2,1).reason());assertEquals(20,actor(f.view(),0).resources().flows().spent());consistent(f.view());
        }
    }

    @Test void slowAndBothHasteSkillsNeutralizeWithoutRefundingOldSpendingAfterExpiry()throws Exception {
        for(int skill:List.of(43,54))try(var f=fixture(4,5,0,b->0,4)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());elapsed(f,6);
            assertEquals("NONE",speed(f,1,1,skill,1).reason());assertFalse(actor(f.view(),1).buffs().hasEffects());
            assertTrue(f.view().slowTargets().targets().isEmpty());
            elapsed(f,8);assertEquals("NONE",speed(f,1,2,skill,1).reason());assertEquals(NONE,slow(f,0,2,1).reason());
            var c=f.view();assertEquals(ModernSlowReceipt.Outcome.NEUTRALIZED,detail(c,0).outcome());
            assertFalse(actor(c,1).buffs().hasEffects());assertEquals(2,actor(c,0).materials().uses());
            assertEquals(37,actor(c,0).resources().flows().spent());consistent(c);
        }
    }

    @Test void exactMovementPaceAndSlowDeadlineRemainIndependentOfVipLease()throws Exception {
        try(var f=fixture(4,5,0,b->0,4)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,1,1,0).status());
            elapsedNanos(f,399_000_000);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,ModernBattleEngineVipSpeedTest.move(f,1,2,0).status());
            elapsedNanos(f,400_000_000);assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,1,3,0).status());
            elapsed(f,6);assertEquals(64*SECOND,actor(f.view(),1).buffs().slowUntilNanos());
            elapsed(f,64);var c=f.view();assertFalse(actor(c,1).buffs().hasEffects());assertTrue(c.slowTargets().targets().isEmpty());
            assertEquals(1,actor(c,0).materials().uses());consistent(c);
        }
    }

    @Test void expiryDuringSlowRandomPublishesNoDebitOrGrantAndSequenceIsRetryable()throws Exception {
        var action=new AtomicReference<Runnable>();try(var f=fixture(4,5,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;},4)) {
            f.active();action.set(()->elapsed(f,6));var denied=slow(f,0,1,1);assertEquals(NOT_ACTIVE,denied.reason());
            var a=actor(denied.observation(),0);assertEquals(0,a.materials().uses());assertEquals(0,a.runtime().sequence());
            assertEquals(0,a.resources().flows().spent());assertFalse(actor(denied.observation(),1).buffs().hasEffects());
            assertEquals(NONE,slow(f,0,1,1).reason());assertEquals(20,actor(f.view(),0).resources().flows().spent());consistent(f.view());
        }
    }

    @Test void nestedSameSequencePaysOnlyOnceAndChangedTargetCancelsOuterCast()throws Exception {
        for(boolean same:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;},4)) {
                f.active();action.set(()->{if(same)assertEquals(NONE,slow(f,0,1,1).reason());else assertEquals("NONE",speed(f,1,1,54,1).reason());});
                if(same)assertThrows(SecurityException.class,()->slow(f,0,1,1));else assertEquals(STALE_TARGET,slow(f,0,1,1).reason());
                assertEquals(same?1:0,actor(f.view(),0).materials().uses());assertEquals(same?17:0,actor(f.view(),0).resources().flows().spent());consistent(f.view());
            }
        }
    }

    @Test void reentrantSlowDuringRecoveryCannotSpendHalfPlannedInventory()throws Exception {
        var action=new AtomicReference<Runnable>();try(var f=fixture(4,5,225,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;},4)) {
            var first=f.active();assertEquals("NONE",cast(f,1,1,4).reason());
            var req=request(first,0,1,1);elapsed(f,3);
            action.set(()->assertEquals(NOT_ACTIVE,f.engine.slow(f.players.get(0).owner(),req).reason()));
            var recovered=f.view();assertNull(action.get());assertEquals(0,actor(recovered,0).materials().uses());assertEquals(0,actor(recovered,0).runtime().sequence());
            elapsed(f,6);assertEquals(NONE,slow(f,0,1,1).reason());assertEquals(1,actor(f.view(),0).materials().uses());consistent(f.view());
        }
    }

    @Test void recoveryCrossingSlowDeadlineClearsGrantEvenWhenVipCandidateIsStale()throws Exception {
        for(boolean expiry:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(expiry?4:-1,expiry?5:-1,225,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;},4)) {
                f.active();assertEquals(NONE,slow(f,0,1,1).reason());assertEquals("NONE",cast(f,1,1,4).reason());
                var paid=actor(f.view(),0).materials();elapsed(f,expiry?3:63);action.set(()->elapsed(f,65));
                var c=f.view();assertNull(action.get());assertFalse(actor(c,1).buffs().hasEffects());assertTrue(c.slowTargets().targets().isEmpty());
                assertEquals(paid,actor(c,0).materials());assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
                if(expiry){assertEquals(0,actor(c,0).resources().equipmentRevision());assertEquals(2,actor(f.view(),0).resources().equipmentRevision());}
                consistent(f.view());
            }
        }
    }

    @Test void realDurableRenamePreservesInterveningSlowConsumptionAndTargetGrant()throws Exception {
        try(var f=fixture(-1,-1,0,b->0,4)) {
            f.active();try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,f.epoch.get());change.prepareVip(candidate,f.epoch.get());
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var r=slow(f,0,1,1);assertEquals(NONE,r.reason());during.set(r.observation());ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                    var c=f.view();assertEquals(actor(during.get(),0).materials(),actor(c,0).materials());assertEquals(actor(during.get(),0).runtime(),actor(c,0).runtime());
                    assertEquals(during.get().slowTargets(),c.slowTargets());assertEquals(actor(during.get(),1).buffs(),actor(c,1).buffs());
                    assertEquals(17,actor(c,0).resources().flows().spent());assertEquals(1,actor(c,0).resources().equipmentRevision());consistent(c);
                }
            }
        }
    }

    @Test void separateTargetsRetainTheirOwnOldVersionsThroughExpiryAndArmorSave()throws Exception {
        try(var f=fixture(4,-1,0,b->0,4);var store=new ModernEquipmentOverrideStore(directory)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());elapsed(f,2);assertEquals(NONE,slow(f,0,2,0).reason());
            var grants=f.view().slowTargets();assertEquals(2,grants.targets().size());elapsed(f,6);
            ModernBattleEngineVipArmorTest.publishArmor(f,0,false,store);var c=f.view();assertEquals(grants,c.slowTargets());
            assertEquals(2,actor(c,0).materials().uses());assertEquals(34,actor(c,0).resources().flows().spent());
            assertEquals(2,actor(c,0).resources().equipmentRevision());assertEquals(1,actor(c,0).loadout().fighter().vip().state().ledger().equipmentRevision());consistent(c);
        }
    }

    @Test void cancellationAndTimeoutClearEffectsWithoutRefundingMaterialsOrMp()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=fixture(-1,-1,0,b->0,4)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());var before=f.view();
            if(timeout)elapsed(f,1800);else f.rooms.rooms.releasePreparation(f.ticket);
            var c=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,c.phase());assertFalse(actor(c,1).buffs().hasEffects());assertTrue(c.slowTargets().targets().isEmpty());
            assertEquals(actor(before,0).materials(),actor(c,0).materials());assertEquals(actor(before,0).resources(),actor(c,0).resources());consistent(c);
        }
    }

    @Test void unlearnedOrUnsupportedTemplateCannotGainPermissionFromVip()throws Exception {
        for(var status:List.of(ModernSlowCatalog.Status.UNLEARNED,ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE))
            try(var f=fixture(4,5,0,b->{fail("No draw for unsupported skill");return 0;},4,status)) {
                f.active();elapsed(f,6);assertEquals(status==ModernSlowCatalog.Status.UNLEARNED?UNLEARNED:UNSUPPORTED,slow(f,0,1,1).reason());
                assertEquals(0,actor(f.view(),0).materials().uses());assertEquals(0,actor(f.view(),0).resources().flows().spent());consistent(f.view());
            }
    }

    @Test void wideSignedEquipmentHasteImmunityIsRequalifiedAfterArmorExpires()throws Exception {
        for(long flag:List.of(1L,-1L,1L<<32))try(var f=fixture(4,5,0,b->0,4,ModernSlowCatalog.Status.QUALIFIED,flag)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());var immune=detail(f.view(),0);
            assertEquals(ModernSlowReceipt.Outcome.EQUIPMENT_IMMUNE,immune.outcome());assertEquals(1,immune.equipmentHasteItems());
            assertFalse(actor(f.view(),1).buffs().hasEffects());elapsed(f,6);assertEquals(0,actor(f.view(),1).loadout().fighter().hasteItemCount().orElseThrow());
            assertEquals(NONE,slow(f,0,2,1).reason());assertEquals(ModernSlowReceipt.Outcome.APPLIED,detail(f.view(),0).outcome());
            assertEquals(2,actor(f.view(),0).materials().uses());assertEquals(37,actor(f.view(),0).resources().flows().spent());consistent(f.view());
        }
    }

    static ModernSlowReceipt detail(ModernBattleObservation.Capture c,int who) {return (ModernSlowReceipt)actor(c,who).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();}
    @Test void timelineCarriesRemainingArrowsAndMaterialsButDoesNotEnableRangedAdmission()throws Exception {
        try(var f=fixture(4,5,0,b->0,4)) {
            f.active();assertEquals(NONE,slow(f,0,1,1).reason());var c=f.view();var a=actor(c,0);
            // Arithmetic candidate only: no ranged engine operation is enabled.
            var arrows=new ArrayList<>(List.of(new ModernAmmo.Stock(new ModernAmmo.Facts(40744,6,5,66,true,0,0),4,2)));
            var materials=new ArrayList<>(a.materials().stocks());var items=new ArrayList<>(a.supplies().stocks());
            var units=new ArrayList<>(a.weight().units());units.add(new ModernInventoryWeight.Unit(40744,1000));
            var weight=a.loadout().reweight(items,arrows,units,materials);
            var history=new ModernEquipmentHistory.State(a.runtime().identity(),c.revision(),a.resources(),a.histories(),a.supplies().history(),a.targets(),a.playerHealing());
            var state=new ModernVipRegenTimeline.State(a.loadout(),history,ModernRegenProtocol.State.initial(a.pose().entityId()),a.regenClock(),weight,items,arrows,materials);
            assertThrows(IllegalArgumentException.class,()->new ModernVipRegenTimeline.State(a.loadout(),history,state.regen(),a.regenClock(),weight,items,List.of(),materials));
            assertThrows(IllegalArgumentException.class,()->new ModernVipRegenTimeline.State(a.loadout(),history,state.regen(),a.regenClock(),weight,items,arrows,List.of()));
            items.clear();arrows.clear();materials.clear(); // defensive immutable copies
            var after=ModernVipRegenTimeline.advance(state,SECOND,7*SECOND,7000,true,false,b->{fail("Unfed recovery draws no random");return 0;});
            assertEquals(2,after.history().resources().equipmentRevision());assertEquals(state.ammo(),after.ammo());assertEquals(state.materials(),after.materials());
            assertEquals(2,after.ammo().get(0).remaining());assertEquals(3,after.materials().get(0).remaining());
            assertEquals(state.weight().view().suppliesWeight(),after.weight().view().suppliesWeight());assertEquals(17,after.history().resources().flows().spent());
            assertFalse(c.features().ammo());assertFalse(a.loadout().combatant().bow());
        }
    }

    @Test void invalidRandomCannotConsumeMaterialEvenAfterSuccessfulExpiryRequalification()throws Exception {
        for(long draw:List.of(-1L,100L))try(var f=fixture(4,5,0,b->draw,4)) {
            f.active();elapsed(f,6);var before=f.view();assertEquals(NUMERIC,slow(f,0,1,1).reason());
            var after=f.view();assertEquals(actor(before,0).materials(),actor(after,0).materials());assertEquals(actor(before,0).resources(),actor(after,0).resources());
            assertFalse(actor(after,1).buffs().hasEffects());assertEquals(1,actor(after,0).runtime().sequence());consistent(after);
        }
    }

    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,long food,ModernPhysicalCombat.BoundedRandom random,int materials)throws Exception {
        return fixture(vip,gear,food,random,materials,ModernSlowCatalog.Status.QUALIFIED);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,long food,ModernPhysicalCombat.BoundedRandom random,int materials,ModernSlowCatalog.Status status)throws Exception {
        return fixture(vip,gear,food,random,materials,status,0);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,long food,ModernPhysicalCombat.BoundedRandom random,int materials,ModernSlowCatalog.Status status,long armorFlag)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,ModernBattleEngineVipArmorTest.ABILITIES,new AtomicLong(1000),random,
                s->withHasteFacts(ModernBattleEngineVipArmorTest.sourceWithArmor(s,vip,gear,10000,food,100),armorFlag),p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->{
                    var s=p.fighter().vip().source();var fighter=ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(s));
                    return enable(ModernBattleEngineVipSpeedTest.enable(advanced(p.withVip(fighter)),ModernHasteCatalog.Status.QUALIFIED),materials,status);
                },SECOND,0,2,false,new int[]{3,2},1_800_000);
    }
    static ModernPhysicalCombat.Source withHasteFacts(ModernPhysicalCombat.Source s,long armorFlag) {
        var policies=new HashMap<>(s.templates());policies.replaceAll((id,p)->p.kind()==ModernCharacterData.ItemKind.ETC?p:new ModernPhysicalCombat.TemplatePolicy(
                p.kind(),p.className(),p.criticalMultiplier(),OptionalLong.of(p.kind()==ModernCharacterData.ItemKind.ARMOR?armorFlag:0)));
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,s.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),s.weaponProcCatalogIds());
    }
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,int materials,ModernSlowCatalog.Status status) {
        var skills=new ArrayList<>(p.skills().skills());
        if(status!=ModernSlowCatalog.Status.UNLEARNED)skills.add(new ModernSkills.Skill(29,"$1455",20,500,ModernSkills.Support.UNIMPLEMENTED));
        skills.sort(Comparator.comparingInt(ModernSkills.Skill::skillId));var stocks=new ArrayList<>(p.matchItems());
        if(materials>0)stocks.add(new ModernShopAuthority.Stack(ModernSpellMaterialsTest.offer(2000),materials));
        stocks.sort(Comparator.comparingLong(x->x.offer().itemId()));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),p.castIntervalNanos(),p.damageMagic(),stocks,p.weightBasis(),
                p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),p.greaterHaste(),p.haste(),
                new ModernSlowCatalog.Snapshot(p.fighter().key(),status,OptionalLong.of(0)),p.playerHealing(),true);
    }
    static ModernSlowCast.Request request(ModernBattleObservation.Capture c,int who,long seq,int target) {
        return ModernBattleSlowTest.request(c,who,seq,ModernBattleSlowTest.target(c,target));
    }
    static ModernBattleEngine.SlowResult slow(ModernBattleEngineVipTest.Fixture f,int who,long seq,int target) {
        return f.engine.slow(f.players.get(who).owner(),request(f.view(),who,seq,target));
    }
    static void consistent(ModernBattleObservation.Capture c) {
        for(var a:c.actors()) {
            assertEquals(a.weight(),a.loadout().reweight(a.supplies().stocks(),a.ammo()==null?List.of():a.ammo().stocks(),a.weight().units(),a.materials().stocks()));
            var h=new ModernEquipmentHistory.State(a.runtime().identity(),c.revision(),a.resources(),a.histories(),a.supplies().history(),a.targets(),a.playerHealing());
            assertDoesNotThrow(()->new ModernSlowTransaction.State(h,a.runtime(),a.materials()));
        }
        assertThrows(IllegalArgumentException.class,c::world);
    }
}
