package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.function.UnaryOperator;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Complete VIP healing inside the engine, not a public wire admission. */
class ModernBattleEngineVipHealingTest {
    @TempDir Path directory;
    @Test void expiryKeepsPaidHealingAndIncomingReceiptsWithoutAborting()throws Exception {
        for(boolean armor:List.of(false,true))try(var f=fixture(4,5,armor,0,b->b-1)) {
            var start=f.active();assertEquals(ModernBattleEngine.Phase.ACTIVE,start.phase());
            assertEquals(NONE,heal(f,0,1,0).reason());var before=f.view();
            elapsed(f,6);var c=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
            var a=actor(c,0);var old=actor(before,0);assertEquals(2,a.resources().equipmentRevision());
            assertEquals(old.playerHealing().incoming(),a.playerHealing().incoming());
            assertEquals(old.playerHealing().outgoing().last(),a.playerHealing().outgoing().last());
            assertEquals(old.resources().flows().spent(),a.resources().flows().spent());
            assertEquals(2,a.playerHealing().outgoing().spending().current().equipmentRevision());
            assertEquals(0,a.loadout().fighter().vip().state().ledger().equipmentRevision());consistent(c);
            assertEquals(NONE,heal(f,0,2,1).reason());consistent(f.view());
        }
    }
    @Test void crossSourceHealRestoresOnlyTargetAndChargesOnlyCaster()throws Exception {
        try(var f=fixture(-1,-1,true,0,b->b-1)) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            elapsed(f,2);var before=f.view();var result=heal(f,0,1,1);assertEquals(NONE,result.reason());
            var c=result.observation();var a=actor(c,0);var b=actor(c,1);var impact=a.playerHealing().outgoing().last().impact();
            assertEquals(7,impact.caster().identity().sourceId());assertEquals(42,b.playerHealing().incoming().recipient().identity().sourceId());
            assertEquals(3333,impact.caster().identity().id());assertEquals(3333,b.playerHealing().incoming().recipient().identity().id());
            assertEquals(impact,b.playerHealing().incoming().last());assertTrue(b.playerHealing().incoming().restored()>0);
            assertEquals(actor(before,0).resources().vitals().hp(),a.resources().vitals().hp());
            assertEquals(actor(before,0).resources().vitals().mp()-ModernPlayerHealing.qualify(a.loadout().fighter()).mpCost(),a.resources().vitals().mp());
            assertEquals(actor(before,1).resources().vitals().mp(),b.resources().vitals().mp());
            assertEquals(actor(before,1).resources().vitals().hp()+b.playerHealing().incoming().restored(),b.resources().vitals().hp());
            assertEquals(actor(before,0).ammo(),a.ammo());assertEquals(actor(before,0).materials(),a.materials());assertEquals(actor(before,0).supplies(),a.supplies());consistent(c);
        }
    }
    @Test void vipIntelligenceChangesLaterHealingNotThePaidReceipt()throws Exception {
        try(var f=fixture(4,5,true,0,b->b-1)) {
            f.active();assertEquals(NONE,heal(f,0,1,0).reason());var before=actor(f.view(),0);var first=before.playerHealing().outgoing().last();
            assertEquals(20,first.baseAmount());assertEquals(4,first.use().cost());
            elapsed(f,6);var expired=actor(f.view(),0);assertEquals(first,expired.playerHealing().outgoing().last());
            assertEquals(2,expired.playerHealing().outgoing().spending().current().equipmentRevision());
            assertEquals(NONE,heal(f,0,2,1).reason());var after=actor(f.view(),0);var second=after.playerHealing().outgoing().last();
            assertEquals(8,second.baseAmount());assertEquals(2,second.use().before().equipmentRevision());
            assertEquals(8,after.playerHealing().outgoing().spending().current().spent());consistent(f.view());
        }
    }
    @Test void selfHealingPotionsAndNaturalRecoveryKeepSeparateBalancesAcrossExpiry()throws Exception {
        try(var f=fixture(4,5,true,225,b->b-1)) {
            f.active();assertEquals("NONE",cast(f,1,1,77).reason());
            assertEquals(NONE,heal(f,0,1,0).reason());var healed=actor(f.view(),0).playerHealing();assertTrue(healed.incoming().restored()>0);
            assertEquals(ModernItemProtocol.Reason.NONE,ModernBattleEngineVipSuppliesTest.use(f,0,1).reason());var used=actor(f.view(),0);assertTrue(used.regeneration().itemGain()>0);
            elapsed(f,6);assertEquals(NONE,heal(f,0,2,0).reason());healed=actor(f.view(),0).playerHealing();
            elapsed(f,60);var c=f.view();var a=actor(c,0);assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
            assertEquals(healed.incoming(),a.playerHealing().incoming());assertEquals(healed.outgoing().last(),a.playerHealing().outgoing().last());
            assertEquals(used.regeneration().itemGain()+healed.incoming().restored(),a.resources().flows().restored());
            assertTrue(a.resources().flows().mpGain()>0);assertEquals(used.supplies().stocks(),a.supplies().stocks());consistent(c);
        }
    }
    @Test void expiryDuringHealingRandomCannotSpendOrRestoreOldCandidateAndCanRetry()throws Exception {
        for(boolean armor:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(4,5,armor,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                var before=f.active();action.set(()->elapsed(f,6));var out=heal(f,0,1,1);assertEquals(NOT_ACTIVE,out.reason());
                for(int i=0;i<2;i++){assertEquals(actor(before,i).playerHealing().incoming(),actor(out.observation(),i).playerHealing().incoming());assertEquals(0,actor(out.observation(),i).resources().flows().spent());}
                assertEquals(0,actor(out.observation(),0).runtime().sequence());assertEquals(NONE,heal(f,0,1,1).reason());
                assertEquals(8,actor(f.view(),0).playerHealing().outgoing().last().baseAmount());consistent(f.view());
            }
        }
    }
    @Test void movementDamageAndOtherHealingDuringRandomCannotBeOverwritten()throws Exception {
        for(String change:List.of("caster-move","target-move","caster-hurt","target-heal")) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,false,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                f.active();var inner=new AtomicReference<ModernBattleObservation.Capture>();action.set(()->{
                    switch(change) {
                        case "caster-move","target-move"->assertEquals(ModernBattleEngine.MoveStatus.MOVED,ModernBattleEngineVipSpeedTest.move(f,change.equals("caster-move")?0:1,1,4).status());
                        case "caster-hurt"->assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1,1,1).status());
                        case "target-heal"->assertEquals(NONE,heal(f,1,1,1).reason());
                        default->fail("unknown change");
                    }
                    inner.set(f.view());
                });
                var out=heal(f,0,1,1);assertEquals(change.equals("target-move")?STALE_TARGET:STALE_STATE,out.reason());
                for(int i=0;i<2;i++){assertEquals(actor(inner.get(),i).resources(),actor(out.observation(),i).resources());assertEquals(actor(inner.get(),i).playerHealing(),actor(out.observation(),i).playerHealing());}
                assertEquals(1,actor(out.observation(),0).runtime().sequence());consistent(out.observation());
            }
        }
    }
    @Test void nestedAndConcurrentDuplicatesCanCommitOnlyOneHeal()throws Exception {
        var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,false,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
            f.active();action.set(()->assertEquals(NONE,heal(f,0,1,1).reason()));assertThrows(SecurityException.class,()->heal(f,0,1,1));
            var c=f.view();assertEquals(1,actor(c,0).runtime().sequence());assertEquals(1,actor(c,0).runtime().playerHealing().casts());assertEquals(4,actor(c,0).resources().flows().spent());consistent(c);
        }
        try(var f=fixture(-1,-1,true,0,b->b-1)) {
            var q=ModernBattlePlayerHealingTest.request(f.active(),0,1,1);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
            try {
                Callable<Boolean> task=()->{start.await();try{return f.engine.playerHeal(f.players.get(0).owner(),q).reason()==NONE;}catch(SecurityException duplicate){return false;}};
                var a=pool.submit(task);var b=pool.submit(task);start.countDown();assertEquals(1,(a.get(5,TimeUnit.SECONDS)?1:0)+(b.get(5,TimeUnit.SECONDS)?1:0));
                var c=f.view();assertEquals(1,actor(c,1).playerHealing().incoming().received());assertEquals(4,actor(c,0).resources().flows().spent());consistent(c);
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void durableSaveRetainsHealingCommittedDuringAtomicRename()throws Exception {
        for(int target:List.of(0,1))try(var f=fixture(4,-1,true,0,b->b-1)) {
            f.active();var during=new AtomicReference<ModernBattleObservation.Capture>();
            try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                var candidate=ModernBattleEngineVipArmorTest.armorCandidate(f,0,change,false);change.prepareVip(candidate,f.epoch.get());
                try(var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("rename"+target)),new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        if(during.get()==null){var out=heal(f,0,1,target);assertEquals(NONE,out.reason());during.set(out.observation());}
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                }
            }
            var after=f.view();for(int i=0;i<2;i++){assertEquals(actor(during.get(),i).playerHealing().incoming(),actor(after,i).playerHealing().incoming());assertEquals(actor(during.get(),i).playerHealing().outgoing().last(),actor(after,i).playerHealing().outgoing().last());}
            assertEquals(1,actor(after,0).resources().equipmentRevision());assertEquals(4,actor(after,0).resources().flows().spent());
            elapsed(f,4);assertEquals(NONE,heal(f,0,2,target).reason());assertEquals(2,actor(f.view(),0).playerHealing().outgoing().last().use().before().equipmentRevision());consistent(f.view());
        }
    }
    @Test void durableCasterOrTargetGearPublicationDuringRandomRejectsOldHealing()throws Exception {
        for(int who:List.of(0,1)) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,true,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return b-1;})) {
                var before=f.active();try(var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("rng"+who)))) {
                    action.set(()->assertDoesNotThrow(()->ModernBattleEngineVipArmorTest.publishArmor(f,who,false,store)));
                    var out=heal(f,0,1,1);assertEquals(NOT_ACTIVE,out.reason());assertEquals(1,actor(out.observation(),who).resources().equipmentRevision());
                    for(int i=0;i<2;i++){assertEquals(actor(before,i).playerHealing().incoming(),actor(out.observation(),i).playerHealing().incoming());assertEquals(0,actor(out.observation(),i).resources().flows().spent());}
                    assertEquals(0,actor(out.observation(),0).runtime().sequence());assertEquals(NONE,heal(f,0,1,1).reason());consistent(f.view());
                }
            }
        }
    }
    @Test void expiringHealingHelmetRepricesFutureCostAndNoRegenRouteAlsoPreservesHistory()throws Exception {
        for(boolean regen:List.of(false,true))try(var f=fixture(4,5,false,0,b->b-1,ModernBattleEngineVipHealingTest::helmet,p->options(p,regen,true))) {
            f.active();assertEquals(NONE,heal(f,0,1,0).reason());var before=actor(f.view(),0);assertEquals(2,before.playerHealing().outgoing().last().use().cost());
            elapsed(f,6);var c=f.view();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());var a=actor(c,0);
            assertEquals(4,a.playerHealing().outgoing().spending().current().cost());assertEquals(before.playerHealing().outgoing().last(),a.playerHealing().outgoing().last());
            assertEquals(2,a.resources().flows().spent());assertEquals(NONE,heal(f,0,2,1).reason());assertEquals(6,actor(f.view(),0).resources().flows().spent());consistent(f.view());
        }
    }
    @Test void healingPriceCannotBeDroppedFromAnOtherwiseValidTimelineCandidate()throws Exception {
        try(var f=fixture(-1,-1,false,0,b->b-1)) {
            var c=f.active();var a=actor(c,0);var h=history(c,0);var incoming=h.healing().incoming();
            var wrong=new ModernEquipmentHistory.State(h.identity(),h.worldRevision(),h.resources(),h.spells(),h.items(),h.targets(),
                    new ModernPlayerHealingHistory.Entry(incoming,ModernPlayerHealingHistory.Outgoing.initial(a.resources().equipmentRevision(),h.healing().outgoing().spending().current().profileRevision(),3)));
            assertThrows(IllegalArgumentException.class,()->new ModernVipRegenTimeline.State(a.loadout(),wrong,ModernRegenProtocol.State.initial(a.pose().entityId()),a.regenClock(),a.weight(),a.supplies().stocks(),a.ammo().stocks(),a.materials().stocks()));
            assertThrows(IllegalArgumentException.class,()->h.reprofile(c.revision()+1,new ModernEquipmentHistory.Profile(1,h.profile().maximumHp(),h.profile().maximumMp(),h.profile().costs(),h.profile().fixedRecovery())));
        }
    }
    @Test void invalidRandomTimeoutAndCancellationNeverPartiallyHealOrSpend()throws Exception {
        for(String mode:List.of("negative","bound","timeout","cancel")) {
            var action=new AtomicReference<Runnable>();try(var f=fixture(-1,-1,false,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return mode.equals("negative")?-1:mode.equals("bound")?b:b-1;})) {
                var before=f.active();if(mode.equals("timeout"))action.set(()->elapsed(f,1800));if(mode.equals("cancel"))action.set(()->f.rooms.rooms.releasePreparation(f.ticket));
                var out=heal(f,0,1,1);assertEquals(mode.equals("timeout")||mode.equals("cancel")?NOT_ACTIVE:NUMERIC,out.reason());
                for(int i=0;i<2;i++){assertEquals(actor(before,i).resources(),actor(out.observation(),i).resources());assertEquals(actor(before,i).playerHealing(),actor(out.observation(),i).playerHealing());}consistent(out.observation());
            }
        }
    }
    @Test void unlearnedOrInsufficientCurrentMpCannotAcquireHealingFromVip()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(-1,-1,false,0,b->{draws.incrementAndGet();return b-1;},UnaryOperator.identity(),p->options(p,true,false))) {
            f.active();assertEquals(UNLEARNED,heal(f,0,1,1).reason());assertEquals(0,draws.get());assertEquals(0,actor(f.view(),0).resources().flows().spent());
        }
        try(var f=fixture(4,5,false,0,b->{draws.incrementAndGet();return b-1;},s->vitals(s,10000,3,0),UnaryOperator.identity())) {
            f.active();elapsed(f,6);assertEquals(NO_MP,heal(f,0,1,1).reason());assertEquals(0,draws.get());assertEquals(3,actor(f.view(),0).resources().vitals().mp());consistent(f.view());
        }
    }
    @Test void sourceQualifiedZeroCostAndLargeHpDoNotNarrowToNativeWidths()throws Exception {
        try(var f=fixture(4,-1,false,0,b->b-1,s->helmet(vitals(s,5_000_000_000L,0,5_000_000_000L)),UnaryOperator.identity(),new int[]{1,0})) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());elapsed(f,2);
            assertEquals(NONE,heal(f,1,1,1).reason());var a=actor(f.view(),1);assertEquals(0,a.playerHealing().outgoing().last().use().cost());assertTrue(a.resources().vitals().hp()>Integer.MAX_VALUE);
            assertTrue(a.playerHealing().incoming().restored()>0);elapsed(f,6);assertEquals(NONE,heal(f,1,2,1).reason());
            assertEquals(0,actor(f.view(),1).resources().flows().spent());assertEquals(0,actor(f.view(),1).resources().vitals().mp());consistent(f.view());
        }
    }
    @Test void oldAbilityTargetAndForeignSourceProofsRejectBeforeRandom()throws Exception {
        var draws=new AtomicInteger();try(var f=fixture(4,5,false,0,b->{draws.incrementAndGet();return b-1;})) {
            var c=f.active();var old=ModernBattlePlayerHealingTest.request(c,0,1,1);elapsed(f,6);
            assertEquals(STALE_STATE,f.engine.playerHeal(f.players.get(0).owner(),old).reason());assertEquals(0,draws.get());
            var q=ModernBattlePlayerHealingTest.request(f.view(),0,2,1);var t=q.target();
            var stale=new ModernPlayerHealingCast.Request(q.scope(),q.matchId(),q.generation(),q.sequence(),q.equipmentRevision(),q.life(),q.cell(),
                    new ModernPlayerHealingCast.Target(t.participant(),t.life(),0,t.cell()));
            assertEquals(STALE_TARGET,f.engine.playerHeal(f.players.get(0).owner(),stale).reason());assertEquals(0,draws.get());
            q=ModernBattlePlayerHealingTest.request(f.view(),0,3,1);var alias=new ModernPlayerHealingCast.Request(q.scope(),q.matchId(),q.generation(),q.sequence(),q.equipmentRevision(),q.life(),q.cell(),
                    new ModernPlayerHealingCast.Target(new ModernTargetHistory.Participant(t.participant().entityId(),ModernParticipantId.human(7,3333)),t.life(),t.equipmentRevision(),t.cell()));
            assertThrows(SecurityException.class,()->f.engine.playerHeal(f.players.get(0).owner(),alias));assertEquals(0,draws.get());assertEquals(NONE,heal(f,0,3,1).reason());consistent(f.view());
        }
    }
    @Test void healingUsesTimedCastPaceAndRetainsOtherSpellMaterialsAndReceipts()throws Exception {
        try(var f=fixture(-1,-1,true,0,b->b-1)) {
            f.active();assertEquals(ModernItemProtocol.Reason.NONE,ModernBattleEngineVipSpeedTest.use(f,0,1,ModernTimedEffects.Kind.HASTE).reason());
            assertEquals(NONE,heal(f,0,1,0).reason());elapsedNanos(f,905_000_000);assertEquals(TOO_FAST,heal(f,0,2,1).reason());
            elapsedNanos(f,906_000_000);assertEquals(NONE,heal(f,0,3,1).reason());elapsed(f,3);
            assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,0,4,0).reason());var slow=f.view();elapsed(f,6);
            assertEquals(NONE,heal(f,0,5,1).reason());var c=f.view();assertEquals(actor(slow,0).materials(),actor(c,0).materials());
            assertEquals(slow.slowTargets(),c.slowTargets());assertEquals(actor(slow,0).targets(),actor(c,0).targets());assertEquals(3,actor(c,0).runtime().playerHealing().casts());consistent(c);
        }
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random)throws Exception {
        return fixture(vip,gear,armor,food,random,UnaryOperator.identity(),UnaryOperator.identity());
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random,
            UnaryOperator<ModernPhysicalCombat.Source> transform,UnaryOperator<ModernBattleEngine.PreparedMember> adjust)throws Exception {
        return fixture(vip,gear,armor,food,random,transform,adjust,new int[]{2,3});
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random,
            UnaryOperator<ModernPhysicalCombat.Source> transform,UnaryOperator<ModernBattleEngine.PreparedMember> adjust,int[] classes)throws Exception {
        return fixture(vip,gear,armor,food,random,transform,adjust,classes,s->0);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,boolean armor,long food,ModernPhysicalCombat.BoundedRandom random,
            UnaryOperator<ModernPhysicalCombat.Source> transform,UnaryOperator<ModernBattleEngine.PreparedMember> adjust,int[] classes,
            java.util.function.ToLongFunction<ModernPhysicalCombat.Source> initialShape)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,ModernBattleEngineVipArmorTest.ABILITIES,new AtomicLong(1000),random,s->{
            var source=ModernBattleEngineVipAmmoTest.withBow(ModernBattleEngineVipArmorTest.sourceWithArmor(s,vip,gear,10000,food,100),-1);
            var rows=new ArrayList<ModernHonorHealing.Row>();for(int i=1;i<=127;i++)rows.add(new ModernHonorHealing.Row(i,i>=20?3:0));
            source=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),armor?source.procCatalogIds():Set.of(),
                    source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),new ModernHonorHealing.Growth(source.growth().sourceId(),source.growth().database(),rows),
                    source.vipCatalog(),armor?source.armorProcs():null,armor?source.weaponProcCatalogIds():Set.of());
            return ModernBattleEngineVipSlowTest.withHasteFacts(transform.apply(source),0);
        },p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->{
            var s=p.fighter().vip().source();var fighter=armor?ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(s)):p.fighter();
            var ready=ModernBattleEngineVipAmmoTest.enable(ModernBattleEngineVipSlowTest.enable(
                    ModernBattleEngineVipSpeedTest.enable(advanced(p.withVip(fighter)),ModernHasteCatalog.Status.QUALIFIED),4,ModernSlowCatalog.Status.QUALIFIED));
            var skills=new ArrayList<>(ready.skills().skills());skills.add(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL));
            skills.sort(Comparator.comparingInt(ModernSkills.Skill::skillId));
            return adjust.apply(new ModernBattleEngine.PreparedMember(ready.owner(),ready.scope(),ready.character(),ready.spawn(),ready.stepIntervalNanos(),ready.fighter(),ready.attackIntervalNanos(),
                    new ModernSkills.Snapshot(ready.scope().sourceId(),ready.character(),skills),ready.castIntervalNanos(),ready.damageMagic(),ready.matchItems(),ready.weightBasis(),
                    ready.timedEnabled(),ready.bowEnabled(),ready.regenEnabled(),ready.fireStormEnabled(),ready.disintegrate(),ready.npcDisintegrate(),ready.greaterHaste(),ready.haste(),ready.slow(),true,true));
        },SECOND,0,2,false,classes,1_800_000,initialShape);
    }
    static ModernBattleEngine.PlayerHealingResult heal(ModernBattleEngineVipTest.Fixture f,int who,long sequence,int target) {
        return f.engine.playerHeal(f.players.get(who).owner(),ModernBattlePlayerHealingTest.request(f.view(),who,sequence,target));
    }
    static void consistent(ModernBattleObservation.Capture c) {
        ModernBattleEngineVipAmmoTest.consistent(c);assertTrue(c.features().playerHealing());
        for(var a:c.actors()) {
            assertNotNull(a.playerHealing());assertEquals(a.resources().equipmentRevision(),a.playerHealing().outgoing().spending().current().equipmentRevision());
            assertEquals(ModernPlayerHealing.qualify(a.loadout().fighter()).mpCost(),a.playerHealing().outgoing().spending().current().cost());
        }
    }
    static ModernEquipmentHistory.State history(ModernBattleObservation.Capture c,int who) {
        var a=actor(c,who);return new ModernEquipmentHistory.State(a.runtime().identity(),c.revision(),a.resources(),a.histories(),a.supplies().history(),a.targets(),a.playerHealing());
    }
    static ModernBattleEngine.PreparedMember options(ModernBattleEngine.PreparedMember p,boolean regen,boolean learned) {
        var skills=learned?p.skills():new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),p.skills().skills().stream().filter(s->s.skillId()!=1).toList());
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),skills,p.castIntervalNanos(),p.damageMagic(),
                p.matchItems(),p.weightBasis(),p.timedEnabled(),p.bowEnabled(),regen,regen&&p.fireStormEnabled(),regen?p.disintegrate():null,null,p.greaterHaste(),p.haste(),p.slow(),true,true);
    }
    static ModernPhysicalCombat.Source vitals(ModernPhysicalCombat.Source s,long hp,long mp,long originalInt) {
        var q=s.input().equipment();var p=q.source().persisted();
        var values=new ModernCharacterData.PersistedValues(hp,mp,hp,mp,p.armorClass(),p.strength(),p.dexterity(),p.constitution(),p.intelligence(),p.wisdom(),p.charisma(),p.experience(),p.lawful(),
                p.originalStrength(),p.originalDexterity(),p.originalConstitution(),originalInt,p.originalWisdom(),p.food225());
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),values,q.source().inventory());
        return source(s,new ModernEquipmentFacts.Qualified(row,q.items(),q.catalog(),q.lifetimes()),s.templates());
    }
    static ModernPhysicalCombat.Source helmet(ModernPhysicalCombat.Source s) {
        if(s.armorProcs()!=null)throw new IllegalArgumentException("helmet fixture has no proc catalog");
        var q=s.input().equipment();var items=new HashMap<>(q.items());var old=items.get(3L);var t=old.template();var m=t.metadata();
        var metadata=new ModernCharacterData.ItemTemplate(20014,m.kind(),m.nameToken(),m.inventoryGraphic(),m.groundGraphic(),m.unitWeight());
        var i=old.source();var item=new ModernCharacterData.InventoryItem(i.objectId(),metadata,i.quantity(),i.equipped(),i.enchantment(),i.durability(),i.identified(),i.blessing(),i.sealed(),i.chargeCount());
        var oldRules=(EquipmentRules.ArmorFacts)t.rules();var helmetRules=new EquipmentRules.ArmorFacts(oldRules.classMask(),oldRules.levelRange(),EquipmentRules.ArmorSlot.HELMET);
        items.put(3L,new ModernEquipmentFacts.Instance(item,new ModernEquipmentFacts.Template(metadata,helmetRules,1,t.safeEnchant(),t.setIds(),t.base(),t.physical()),old.greater(),old.dark(),old.cards(),old.stone()));
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),q.source().persisted(),items.values().stream().map(ModernEquipmentFacts.Instance::source).sorted(Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId)).toList());
        var rules=new HashMap<>(q.catalog().lookups());for(var key:ModernEquipmentFacts.requiredLookups(List.copyOf(items.values())))rules.putIfAbsent(key,ModernEquipmentFacts.RuleValue.absent());
        var cat=q.catalog();var catalog=new ModernEquipmentFacts.Catalog(cat.sourceId(),cat.resourceDatabase(),rules,cat.verifiedSetIds(),cat.sets(),cat.combos(),cat.vipTableState());
        var life=q.lifetimes();var facts=new HashMap<>(life.items());var fact=facts.get(3L);
        facts.put(3L,new ModernItemLifetime.Fact(3,20014,fact.kind(),fact.sourceEquipped(),fact.remainingSeconds(),fact.timeRulePresent(),fact.timeRuleHours(),fact.absoluteLocalTime(),fact.templateMaxUseSeconds(),fact.lighting()));
        var qualified=new ModernEquipmentFacts.Qualified(row,items,catalog,new ModernItemLifetime.Observation(row.sourceId(),row.character().objectId(),life.dbNowLocal(),life.queryStartedEpochMillis(),life.queryStartedNanos(),facts,life.dbClockUncertaintyMillis()));
        var policies=new HashMap<>(s.templates());policies.put(20014L,policies.remove(m.catalogId()));return source(s,qualified,policies);
    }
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source s,ModernEquipmentFacts.Qualified q,Map<Long,ModernPhysicalCombat.TemplatePolicy> templates) {
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,s.input().weaponsQualified(),s.input().weapons()),s.growth(),templates,s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),s.weaponProcCatalogIds());
    }
}
