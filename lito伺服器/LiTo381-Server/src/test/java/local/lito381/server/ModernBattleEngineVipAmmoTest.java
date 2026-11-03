package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Private full-VIP bow composition. No public wire or visual admission. */
class ModernBattleEngineVipAmmoTest {
    @TempDir Path directory;
    @Test void arrowSelectionDuringDamageCannotChargeTheNewArrowForOldDamage()throws Exception {
        for(boolean armor:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,-1,armor,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                var before=f.active();action.set(()->assertEquals(ModernAmmoProtocol.Reason.NONE,select(f,0,1,40746).reason()));
                var result=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,result.status());
                var c=result.observation();assertEquals(40746,actor(c,0).ammo().selectedItemId());
                assertEquals(actor(before,0).ammo().stocks(),actor(c,0).ammo().stocks());
                assertEquals(actor(before,1).resources(),actor(c,1).resources());assertNull(actor(c,0).presentation().shot());
                assertEquals(0,actor(c,0).ammo().shotCount());consistent(c);
            }
        }
    }

    @Test void nestedAttacksCannotRewindSequenceOrDebitTwiceOnTheOuterReturn()throws Exception {
        for(boolean armor:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,-1,armor,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                f.active();var inside=new AtomicReference<ModernBattleObservation.Capture>();action.set(()->{
                    assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
                    var second=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,second.status());inside.set(second.observation());
                });
                assertThrows(SecurityException.class,()->f.attack(0,1,2));
                var c=f.view();assertEquals(2,actor(c,0).vital().attackSequence());assertEquals(1,actor(c,0).ammo().shotCount());
                assertEquals(actor(inside.get(),0).ammo(),actor(c,0).ammo());assertEquals(actor(inside.get(),1).resources(),actor(c,1).resources());consistent(c);
            }
        }
    }

    @Test void hitAtomicallyPublishesArrowWeightHpAndSourceQualifiedShotWithOptionalProtection()throws Exception {
        for(boolean armor:List.of(false,true))try(var f=fixture(-1,-1,-1,armor,0,b->b-1)) {
            var before=f.active();var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());
            var c=out.observation();var a=actor(c,0);var t=actor(c,1);var shot=a.presentation().shot();
            assertEquals(before.revision()+1,c.revision());assertEquals(c.revision(),a.ammo().shotRevision());assertEquals(c.revision(),a.vital().attackRevision());
            assertEquals(c.revision(),t.vital().hurtRevision());assertEquals(3,a.ammo().selected().remaining());
            assertEquals(actor(before,0).weight().view().suppliesWeight()-2,a.weight().view().suppliesWeight());
            assertEquals(actor(before,0).supplies(),a.supplies());assertEquals(actor(before,0).materials(),a.materials());
            assertEquals(42,((ModernTargetHistory.Participant)shot.target()).identity().sourceId());assertEquals(3333,((ModernTargetHistory.Participant)shot.target()).identity().id());
            assertEquals(c.revision(),shot.revision());assertEquals(0,shot.equipmentRevision());
            long full=actor(before,1).resources().vitals().hp()-t.resources().vitals().hp();assertTrue(full>0);consistent(c);
            elapsed(f,2);var second=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,second.status());
            assertEquals(armor?full/2:full,t.resources().vitals().hp()-actor(second.observation(),1).resources().vitals().hp());consistent(second.observation());
        }
    }

    @Test void evadedArrowStillConsumesOneButCannotCreateHurtOrArmorTrigger()throws Exception {
        for(boolean armor:List.of(false,true))try(var f=fixture(-1,-1,-1,armor,0,b->b==100?0:b-1)) {
            var before=f.active();var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());var c=out.observation();
            assertFalse(actor(c,0).ammo().hit());assertEquals(1,actor(c,0).ammo().shotCount());assertEquals(3,actor(c,0).ammo().selected().remaining());
            // A paid zero-damage attempt still advances the resource ledger,
            // but never changes vitals/flows or emits a hurt/armor effect.
            assertEquals(actor(before,1).resources().vitals(),actor(c,1).resources().vitals());
            assertEquals(actor(before,1).resources().flows(),actor(c,1).resources().flows());assertEquals(actor(before,1).vital(),actor(c,1).vital());
            if(armor){assertTrue(actor(c,1).armorProtection().triggers().isEmpty());assertEquals(0,actor(c,1).armorProtection().state().untilNanos());}consistent(c);
        }
    }

    @Test void vipAndArmorExpiryKeepSpentArrowsMaterialsPotionsAndOldEventVersions()throws Exception {
        var high=new AtomicBoolean();try(var f=fixture(4,5,-1,true,0,b->high.get()?b-1:0)) {
            f.active();assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,0,1,1).reason());high.set(true);
            elapsed(f,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            assertEquals(ModernItemProtocol.Reason.NONE,ModernBattleEngineVipSpeedTest.use(f,0,1,ModernTimedEffects.Kind.HASTE).reason());
            var before=f.view();elapsed(f,6);var c=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
            var a=actor(c,0);var old=actor(before,0);assertEquals(2,a.resources().equipmentRevision());
            assertEquals(old.ammo(),a.ammo());assertEquals(old.materials(),a.materials());assertEquals(old.supplies().stocks(),a.supplies().stocks());
            assertEquals(old.presentation(),a.presentation());assertEquals(old.weight().view().suppliesWeight(),a.weight().view().suppliesWeight());
            assertEquals(25,old.loadout().fighter().ranged().values().offense().damageBonus()-a.loadout().fighter().ranged().values().offense().damageBonus());
            assertEquals(before.slowTargets(),c.slowTargets());assertEquals(old.buffs(),a.buffs());
            var next=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,next.status());
            assertEquals(2,actor(next.observation(),0).presentation().shot().equipmentRevision());
            assertEquals(2,actor(next.observation(),0).ammo().selected().remaining());consistent(next.observation());
        }
    }

    @Test void weaponExpirySwitchesToUnarmedWithoutErasingArrowHistoryOrChargingMelee()throws Exception {
        try(var f=fixture(-1,-1,4,false,0,b->b-1)) {
            f.active();var shot=f.attack(0,1,2).observation();elapsed(f,4);var expired=f.view();
            assertFalse(actor(expired,0).loadout().combatant().bow());assertTrue(expired.features().ammo());
            assertEquals(actor(shot,0).ammo(),actor(expired,0).ammo());assertEquals(actor(shot,0).presentation(),actor(expired,0).presentation());
            assertEquals(1,actor(expired,0).resources().equipmentRevision());var melee=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,melee.status());
            assertEquals(actor(shot,0).ammo(),actor(melee.observation(),0).ammo());assertEquals(actor(shot,0).presentation(),actor(melee.observation(),0).presentation());consistent(melee.observation());
        }
    }

    @Test void movementMaterialsTargetDamageAndSelectionAbaAllInvalidateOldShotInputs()throws Exception {
        for(String change:List.of("caster-move","target-move","material","target-damage","selection-aba")) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,-1,false,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                var before=f.active();var changed=new AtomicReference<ModernBattleObservation.Capture>();action.set(()->{
                    switch(change) {
                        case "caster-move","target-move"->assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,change.equals("caster-move")?0:1,1,4).status());
                        case "material"->assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,0,1,1).reason());
                        case "target-damage"->assertEquals("NONE",cast(f,1,1,4).reason());
                        case "selection-aba"->{assertEquals(ModernAmmoProtocol.Reason.NONE,select(f,0,1,40746).reason());assertEquals(ModernAmmoProtocol.Reason.NONE,select(f,0,2,40744).reason());}
                        default->fail("Unknown test action");
                    }
                    changed.set(f.view());
                });
                var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,out.status());var c=out.observation();
                assertEquals(actor(before,0).ammo().stocks(),actor(c,0).ammo().stocks());assertEquals(actor(changed.get(),1).resources(),actor(c,1).resources());
                assertEquals(actor(changed.get(),0).materials(),actor(c,0).materials());assertEquals(0,actor(c,0).ammo().shotCount());consistent(c);
            }
        }
    }

    @Test void expiredAbilityOrInvalidRandomNeverPartiallyCommitsShotAndRetryUsesCurrentProfile()throws Exception {
        for(boolean armor:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(4,5,-1,armor,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                var before=f.active();action.set(()->elapsed(f,6));var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,out.status());
                assertEquals(actor(before,0).ammo(),actor(out.observation(),0).ammo());assertEquals(0,actor(out.observation(),0).vital().attackSequence());
                assertEquals(0,actor(out.observation(),1).resources().flows().damaged());
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());assertEquals(2,actor(f.view(),0).presentation().shot().equipmentRevision());consistent(f.view());
            }
        }
        for(boolean high:List.of(false,true))try(var f=fixture(-1,-1,-1,false,0,b->high?b:-1)) {
            var before=f.active();var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,out.status());
            assertEquals(actor(before,0).ammo(),actor(out.observation(),0).ammo());assertEquals(actor(before,1).resources(),actor(out.observation(),1).resources());consistent(out.observation());
        }
    }

    @Test void exhaustionRequiresExplicitSelectionAndDenialsCannotRewriteEarlierShots()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(4,5,-1,false,0,b->{draws.incrementAndGet();return b-1;})) {
            f.active();for(int i=1;i<=4;i++){elapsed(f,(i-1)*2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,i,2).status());}
            elapsed(f,8);var before=f.view();int count=draws.get();assertEquals(ModernBattleEngine.AttackStatus.NO_AMMO,f.attack(0,5,2).status());assertEquals(count,draws.get());
            assertEquals(actor(before,0).ammo(),actor(f.view(),0).ammo());assertEquals(ModernAmmoProtocol.Reason.EMPTY,select(f,0,1,40744).reason());
            assertEquals(ModernAmmoProtocol.Reason.UNSUPPORTED,select(f,0,2,40010).reason());assertThrows(SecurityException.class,()->select(f,0,2,40746));
            var denied=actor(f.view(),0).ammo();assertEquals(actor(before,0).ammo().shotRevision(),denied.shotRevision());assertEquals(actor(before,0).ammo().stocks(),denied.stocks());
            assertEquals(ModernAmmoProtocol.Reason.NONE,select(f,0,3,40746).reason());assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,6,2).status());
            assertEquals(40746,actor(f.view(),0).ammo().shotItemId());assertEquals(3,actor(f.view(),0).ammo().selected().remaining());consistent(f.view());
        }
    }

    @Test void sourceRangeRejectsWithoutRandomAndReentryWithinRangeCanShoot()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(-1,-1,-1,false,0,b->{draws.incrementAndGet();return b-1;})) {
            f.active();for(int i=1;i<=9;i++){elapsed(f,i);assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,1,i,4).status());}
            assertEquals(ModernBattleEngine.AttackStatus.OUT_OF_RANGE,f.attack(0,1,2).status());assertEquals(0,draws.get());assertEquals(0,actor(f.view(),0).ammo().shotCount());
            elapsed(f,10);assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,1,10,0).status());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());consistent(f.view());
        }
    }

    @Test void durableBowSaveKeepsInterveningShotAndReequipDoesNotRefillAmmo()throws Exception {
        for(boolean armor:List.of(false,true))try(var f=fixture(4,-1,-1,armor,0,b->b-1)) {
            f.active();var during=new AtomicReference<ModernBattleObservation.Capture>();
            try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                var candidate=bowCandidate(f,change,false);change.prepareVip(candidate,f.epoch.get());
                try(var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("bow"+armor)),new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        if(during.get()==null){var r=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,r.status());during.set(r.observation());}
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                    assertFalse(actor(f.view(),0).loadout().combatant().bow());assertEquals(actor(during.get(),0).ammo(),actor(f.view(),0).ammo());
                    elapsed(f,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());
                    assertEquals(actor(during.get(),0).presentation().shot(),actor(f.view(),0).presentation().shot());
                    try(var equip=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                        var next=bowCandidate(f,equip,true);equip.prepareVip(next,f.epoch.get());
                        assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,equip.publish(ModernBattleEngineVipTest.commit(store,equip,next)).disposition());
                    }
                    elapsed(f,4);assertEquals(3,actor(f.view(),0).resources().equipmentRevision());assertEquals(2,actor(f.view(),0).loadout().fighter().vip().state().ledger().equipmentRevision());
                    assertEquals(3,actor(f.view(),0).ammo().selected().remaining());assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,3,2).status());
                    assertEquals(2,actor(f.view(),0).ammo().selected().remaining());assertEquals(3,actor(f.view(),0).presentation().shot().equipmentRevision());consistent(f.view());
                }
            }
        }
    }

    @Test void reentrantArrowSelectionDuringRecoveryCannotConsumeHalfPlannedState()throws Exception {
        var action=new AtomicReference<Runnable>();try(var f=fixture(4,5,-1,false,225,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
            f.active();f.attack(0,1,2);var before=f.view();elapsed(f,3);
            action.set(()->assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,select(f,0,1,40746).reason()));
            var recovered=f.view();assertNull(action.get());assertEquals(actor(before,0).ammo(),actor(recovered,0).ammo());
            elapsed(f,6);assertEquals(ModernAmmoProtocol.Reason.NONE,select(f,0,1,40746).reason());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());consistent(f.view());
        }
    }

    @Test void hasteAndSlowChangeActualShotPaceWithoutGrantingFreeOrExtraArrows()throws Exception {
        var high=new AtomicBoolean(true);try(var f=fixture(-1,-1,-1,false,0,b->high.get()?b-1:0)) {
            f.active();ModernBattleEngineVipSpeedTest.use(f,0,1,ModernTimedEffects.Kind.HASTE);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());elapsedNanos(f,527_000_000);
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,2,2).status());elapsedNanos(f,528_000_000);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,3,2).status());
            elapsed(f,2);high.set(false);assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,1,1,0).reason());assertFalse(actor(f.view(),0).buffs().hasEffects());
            elapsed(f,4);assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,1,2,0).reason());high.set(true);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,4,2).status());elapsedNanos(f,5_399_000_000L);
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,5,2).status());elapsedNanos(f,5_400_000_000L);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,6,2).status());assertEquals(4,actor(f.view(),0).ammo().shotCount());
            assertEquals(2,actor(f.view(),1).materials().uses());consistent(f.view());
        }
    }

    @Test void concurrentDuplicateAttacksCommitExactlyOneShot()throws Exception {
        try(var f=fixture(-1,-1,-1,false,0,b->b-1)) {
            f.active();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var jobs=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{start.await();try{return f.attack(0,1,2).status()==ModernBattleEngine.AttackStatus.ATTACKED;}catch(SecurityException duplicate){return false;}}));
                start.countDown();int wins=0;for(var job:jobs)if(job.get(5,TimeUnit.SECONDS))wins++;
                assertEquals(1,wins);assertEquals(1,actor(f.view(),0).ammo().shotCount());assertEquals(1,actor(f.view(),0).vital().attackSequence());consistent(f.view());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }

    @Test void terminalArrowKillKeepsTypedSourceResultsAndCannotSpendAfterFinish()throws Exception {
        try(var f=fixture(-1,-1,-1,true,0,b->b-1,true,1_000_000)) {
            f.active();var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());var c=out.observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,c.phase());assertEquals(1,actor(c,0).ammo().shotCount());assertEquals(0,actor(c,1).resources().vitals().hp());
            assertEquals(ModernMatchSettlement.Verdict.WIN,c.result().participants().get(0).verdict());assertEquals(ModernMatchSettlement.Verdict.LOSS,c.result().participants().get(1).verdict());
            assertEquals(7,c.result().participants().get(0).identity().sourceId());assertEquals(42,c.result().participants().get(1).identity().sourceId());
            assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,select(f,0,1,40746).reason());assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,f.attack(0,2,2).status());
            assertEquals(c,f.view());consistent(c);
        }
    }

    @Test void timeoutDuringRandomCannotPublishEitherAmmoOrDamage()throws Exception {
        var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,-1,true,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
            var before=f.active();action.set(()->elapsed(f,1800));var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,out.status());
            var c=out.observation();assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,c.abortReason());
            for(int i=0;i<2;i++){assertEquals(actor(before,i).ammo(),actor(c,i).ammo());assertEquals(actor(before,i).resources(),actor(c,i).resources());}consistent(c);
        }
    }

    static ModernVipCharacterState.Candidate bowCandidate(ModernBattleEngineVipTest.Fixture f,ModernBattleEngine.EquipmentChange change,boolean equipped) {
        var s=f.sources.get(0);var q=s.input().equipment();var state=change.vipState();long now=f.epoch.get();
        var fresh=ModernVipLoadout.qualifyForRebuild(s,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,state.ledger().equipmentRevision()+1,Map.of(5L,equipped)),now,0);
        return ModernVipCharacterState.prepareOrdinaryEquipment(state,fresh,now);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,int bow,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random)throws Exception {
        return fixture(vip,gear,bow,armor,food,random,false,25);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,int bow,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random,boolean settlement,long damage)throws Exception {
        var fields=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);fields.putAll(ModernBattleEngineVipArmorTest.ABILITIES);
        fields.put(ModernVipEquipment.Field.RANGED_DAMAGE,damage);fields.put(ModernVipEquipment.Field.RANGED_HIT,40L);fields.put(ModernVipEquipment.Field.DEX,5L);
        return new ModernBattleEngineVipTest.Fixture(false,fields,new AtomicLong(1000),random,s->{
            var source=withBow(ModernBattleEngineVipArmorTest.sourceWithArmor(s,vip,gear,10000,food,100),bow);
            if(!armor)source=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),Set.of(),source.magicGrowth(),
                    source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth(),source.vipCatalog(),null,Set.of());
            return ModernBattleEngineVipSlowTest.withHasteFacts(source,0);
        },p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->{
            var s=p.fighter().vip().source();var fighter=armor?ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(s)):p.fighter();
            var timed=ModernBattleEngineVipSpeedTest.enable(advanced(p.withVip(fighter)),ModernHasteCatalog.Status.QUALIFIED);
            return enable(ModernBattleEngineVipSlowTest.enable(timed,4,ModernSlowCatalog.Status.QUALIFIED));
        },SECOND,0,2,settlement,new int[]{2,3},1_800_000);
    }
    static ModernPhysicalCombat.Source withBow(ModernPhysicalCombat.Source s,int expires) {
        var q=s.input().equipment();var seed=ModernPhysicalCombatTest.weapon(5,1,true,0,ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.TemplatePhysical.zero());
        var t=seed.template();var weapon=new ModernEquipmentFacts.Instance(seed.source(),new ModernEquipmentFacts.Template(t.metadata(),t.rules(),4,
                t.safeEnchant(),t.setIds(),t.base(),t.physical()),seed.greater(),seed.dark(),seed.cards(),seed.stone());
        var items=new HashMap<>(q.items());items.put(5L,weapon);
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),q.source().persisted(),
                items.values().stream().map(ModernEquipmentFacts.Instance::source).sorted(Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId)).toList());
        var rules=new HashMap<>(q.catalog().lookups());
        for(var key:ModernEquipmentFacts.requiredLookups(List.copyOf(items.values())))if(key.kind()!=ModernEquipmentFacts.RuleKind.VIP)rules.putIfAbsent(key,ModernEquipmentFacts.RuleValue.absent());
        var catalog=new ModernEquipmentFacts.Catalog(row.sourceId(),q.catalog().resourceDatabase(),rules,q.catalog().verifiedSetIds(),q.catalog().sets(),q.catalog().combos(),q.catalog().vipTableState());
        var life=q.lifetimes();var facts=new HashMap<>(life.items());
        facts.put(5L,new ModernItemLifetime.Fact(5,1,ModernCharacterData.ItemKind.WEAPON,true,OptionalLong.of(0),expires>=0,
                expires<0?OptionalLong.empty():OptionalLong.of(1),expires<0?Optional.empty():Optional.of(String.format("2026-10-01 00:00:%02d",expires)),0,false));
        var equipment=new ModernEquipmentFacts.Qualified(row,items,catalog,new ModernItemLifetime.Observation(row.sourceId(),row.character().objectId(),
                life.dbNowLocal(),life.queryStartedEpochMillis(),life.queryStartedNanos(),facts,life.dbClockUncertaintyMillis()));
        var policies=new HashMap<>(s.templates());policies.put(1L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.WEAPON,"0",new ModernPhysicalCombat.Rational(1,1),OptionalLong.of(0)));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(equipment,true,Map.of(5L,new ModernCombatProfile.WeaponFacts(5,1,4,8,12,8,11,0,0,0,1,0,0))),
                s.growth(),policies,s.procCatalogIds(),s.magicGrowth(),Map.of(1L,0L),Map.of(1L,-1L),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),s.weaponProcCatalogIds());
    }
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p) {
        var stocks=new ArrayList<>(p.matchItems());
        for(long id:List.of(40744L,40746L)) {
            var old=ModernCombatTestPeer.arrow(id,id==40744?0:1,20);var t=old.template();
            var offer=ModernShopCatalog.Offer.fromSource(id,old.sortOrder(),20,20,new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),2000,t.etc(),t.arrow()));
            stocks.add(new ModernShopAuthority.Stack(offer,4));
        }
        stocks.sort(Comparator.comparingLong(x->x.offer().itemId()));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                p.skills(),p.castIntervalNanos(),p.damageMagic(),stocks,p.weightBasis(),true,true,p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),
                p.npcDisintegrate(),p.greaterHaste(),p.haste(),p.slow(),p.playerHealing(),true);
    }
    static ModernBattleEngine.AmmoResult select(ModernBattleEngineVipTest.Fixture f,int who,long seq,long id) {
        var p=f.players.get(who);return f.engine.ammo(p.owner(),new ModernAmmoProtocol.Request(p.scope(),ModernAmmoProtocol.Action.SELECT,f.ticket.matchId(),f.ticket.generation(),seq,id));
    }
    static void consistent(ModernBattleObservation.Capture c) {
        ModernBattleEngineVipSlowTest.consistent(c);
        for(var a:c.actors()){assertDoesNotThrow(()->ModernBattlePresentation.capture(a));assertTrue(a.ammo().attackSequence()<=a.vital().attackSequence());}
    }
}
