package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.*;
import static local.lito381.server.ModernBattleEngineVipSuppliesTest.actor;
import static local.lito381.server.ModernBattleEngineVipArmorTest.armor;
import static local.lito381.server.ModernTimedEffects.Kind.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Internal full-VIP status composition. No public permission, packets, native
 * renderer or resource availability is established by these synthetic sources. */
class ModernBattleEngineVipSpeedTest {
    @TempDir Path directory;

    @Test void allFourPotionsUseQualifiedClassStockWeightAndIndependentLeases()throws Exception {
        var draws=new AtomicInteger();
        try(var f=fixture(4,5,0,b->{draws.incrementAndGet();return 0;})) {
            var before=f.active();
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,use(f,0,1,ELF_BRAVE).reason());
            assertEquals(actor(before,0).supplies().stocks(),actor(f.view(),0).supplies().stocks());
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,use(f,1,1,WISDOM).reason());
            for(var kind:List.of(HASTE,BRAVE,WISDOM))assertEquals(ModernItemProtocol.Reason.NONE,use(f,0,actor(f.view(),0).supplies().requestSequence()+1,kind).reason());
            for(var kind:List.of(HASTE,ELF_BRAVE))assertEquals(ModernItemProtocol.Reason.NONE,use(f,1,actor(f.view(),1).supplies().requestSequence()+1,kind).reason());
            var used=f.view();assertEquals(0,draws.get());assertEquals(2,actor(used,0).buffs().spBonus());assertEquals(3,actor(used,1).buffs().braveKind());
            assertEquals(300*SECOND,actor(used,0).buffs().hasteUntilNanos());assertEquals(480*SECOND,actor(used,1).buffs().braveUntilNanos());
            for(int i=0;i<2;i++) {
                assertEquals(actor(before,i).resources().vitals(),actor(used,i).resources().vitals());
                assertTrue(actor(used,i).weight().view().suppliesWeight()<actor(before,i).weight().view().suppliesWeight());
            }
            elapsed(f,6);var expiredGear=f.view();
            for(int i=0;i<2;i++){assertEquals(2,actor(expiredGear,i).resources().equipmentRevision());assertEquals(actor(used,i).buffs(),actor(expiredGear,i).buffs());}
            elapsed(f,300);var shortExpired=f.view();assertFalse(actor(shortExpired,0).buffs().hasEffects());
            assertTrue(actor(shortExpired,1).buffs().holds(ELF_BRAVE));assertFalse(actor(shortExpired,1).buffs().holds(HASTE));
            elapsed(f,480);assertFalse(actor(f.view(),1).buffs().hasEffects());
            assertEquals(actor(used,0).supplies().history().last(),actor(f.view(),0).supplies().history().last());
        }
    }

    @Test void bothSpellsRepriceAfterVipExpiryButDoNotRewriteOldCostOrTargetProof()throws Exception {
        for(int skill:List.of(43,54))try(var f=fixture(4,5,0,b->0)) {
            f.active();var cast=speed(f,0,1,skill,1);assertEquals("NONE",cast.reason());var before=actor(cast.observation(),0);
            var receipt=before.runtime().receipts().get(speedLane(skill));long spent=before.resources().flows().spent();
            int target=skill==43?1:0;var lease=actor(cast.observation(),target).buffs();
            elapsed(f,6);var after=f.view();assertEquals(2,actor(after,0).resources().equipmentRevision());
            assertEquals(0,actor(after,0).loadout().fighter().vip().state().ledger().equipmentRevision());
            assertEquals(receipt,actor(after,0).runtime().receipts().get(speedLane(skill)));assertEquals(lease,actor(after,target).buffs());
            assertEquals(spent,actor(after,0).resources().flows().spent());
            assertTrue(actor(after,0).histories().get(speedLane(skill)).current().cost()>before.histories().get(speedLane(skill)).current().cost());
            assertEquals("NONE",speed(f,0,2,skill,1).reason());var latest=actor(f.view(),0);
            assertEquals(spent+latest.histories().get(speedLane(skill)).current().cost(),latest.resources().flows().spent());
            assertEquals(2,latest.runtime().receipts().get(speedLane(skill)).batch().use().before().equipmentRevision());
            if(skill==43){var r=(ModernHasteReceipt)receipt.detail();assertEquals(42,((ModernHasteReceipt.Player)r.target()).participant().identity().sourceId());}
        }
    }

    @Test void stalePreExpirySpeedCommandsRejectWithoutSpendingOrGranting()throws Exception {
        for(int skill:List.of(43,54))try(var f=fixture(4,5,0,b->0)) {
            var before=f.active();var h=hasteRequest(before,0,1,1);var g=greaterRequest(before,0,1);elapsed(f,4);
            Reply out=skill==43?reply(f.engine.haste(f.players.get(0).owner(),h)):reply(f.engine.greaterHaste(f.players.get(0).owner(),g));
            assertEquals("STALE_STATE",out.reason());assertEquals(0,actor(out.observation(),0).resources().flows().spent());
            assertFalse(actor(out.observation(),0).buffs().hasEffects());assertFalse(actor(out.observation(),1).buffs().hasEffects());
            assertEquals(1,actor(out.observation(),0).runtime().sequence());assertEquals("NONE",speed(f,0,2,skill,1).reason());
        }
    }

    @Test void potionAndSkillOverwriteKeepOriginalSpendingAndOtherEffects()throws Exception {
        try(var f=fixture(-1,-1,0,b->0)) {
            f.active();assertEquals("NONE",speed(f,0,1,54,0).reason());var receipt=actor(f.view(),0).runtime().receipts().get(speedLane(54));
            use(f,0,1,WISDOM);assertEquals(ModernTimedEffects.HasteOrigin.GREATER_HASTE,actor(f.view(),0).buffs().hasteOrigin());
            elapsed(f,2);assertEquals("NONE",speed(f,1,1,43,0).reason());var c=f.view();
            assertEquals(ModernTimedEffects.HasteOrigin.HASTE,actor(c,0).buffs().hasteOrigin());assertEquals(1202*SECOND,actor(c,0).buffs().hasteUntilNanos());
            assertEquals(1,c.hasteTargets().targets().size());assertEquals(2,actor(c,0).buffs().spBonus());
            use(f,0,2,HASTE);var potion=f.view();assertEquals(ModernTimedEffects.HasteOrigin.POTION,actor(potion,0).buffs().hasteOrigin());
            assertEquals(302*SECOND,actor(potion,0).buffs().hasteUntilNanos());assertTrue(potion.hasteTargets().targets().isEmpty());
            assertEquals(receipt,actor(potion,0).runtime().receipts().get(speedLane(54)));assertEquals(receipt.spent(),actor(potion,0).resources().flows().spent());
        }
    }

    @Test void movementAttackAndSharedSpellPaceUseBothBuffsWithoutMutatingBaseIntervals()throws Exception {
        try(var f=fixture(-1,-1,0,b->0)) {
            f.active();use(f,0,1,HASTE);use(f,0,2,BRAVE);var buff=actor(f.view(),0).buffs();
            long step=ModernTimedEffects.intervalNanos(200_000_000,ModernTimedEffects.Action.MOVE,buff);
            assertEquals(114_000_000,step);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,0,1,0).status());
            elapsedNanos(f,step-1);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,move(f,0,2,0).status());
            elapsedNanos(f,step);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,0,3,4).status());
            elapsed(f,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            long attack=ModernTimedEffects.intervalNanos(700_000_000,ModernTimedEffects.Action.ATTACK,buff);assertEquals(398_000_000,attack);
            elapsedNanos(f,SECOND+attack-1);assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,2,2).status());
            elapsedNanos(f,SECOND+attack);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,3,2).status());
            elapsed(f,2);assertEquals("NONE",cast(f,0,1,4).reason());
            long spell=ModernTimedEffects.intervalNanos(1_200_000_000,ModernTimedEffects.Action.SPELL,buff);
            elapsedNanos(f,2*SECOND+spell-1);assertEquals("TOO_FAST",speed(f,0,2,54,0).reason());
            elapsedNanos(f,2*SECOND+spell);assertEquals("NONE",speed(f,0,3,54,0).reason());
            assertEquals(200_000_000,actor(f.view(),0).pose().stepIntervalNanos());assertEquals(700_000_000,actor(f.view(),0).vital().attackIntervalNanos());
        }
    }

    @Test void wisdomCombinesWithFullVipMagicButDoesNotChangeSavedIntOrSp()throws Exception {
        for(int skill:List.of(4,70,77))try(var f=fixture(-1,-1,0,b->0);var control=fixture(-1,-1,0,b->0)) {
            var initial=f.active();control.active();use(f,0,1,WISDOM);
            var boosted=cast(f,0,1,skill);var plain=cast(control,0,1,skill);assertEquals("NONE",boosted.reason());assertEquals("NONE",plain.reason());
            long full=actor(initial,1).resources().vitals().hp();assertTrue(full-actor(boosted.observation(),1).resources().vitals().hp()>full-actor(plain.observation(),1).resources().vitals().hp());
            assertEquals(actor(initial,0).loadout().fighter().profile().effective(),actor(boosted.observation(),0).loadout().fighter().profile().effective());
            assertEquals(actor(plain.observation(),0).resources().flows().spent(),actor(boosted.observation(),0).resources().flows().spent());
        }
    }

    @Test void slowNaturalRecoveryCannotPublishExpiredPotionOrTargetedHaste()throws Exception {
        for(int duration:List.of(300,1200))for(boolean expires:List.of(false,true)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(expires?4:-1,expires?5:-1,225,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                f.active();if(duration==300)use(f,0,1,HASTE);else assertEquals("NONE",speed(f,1,1,43,0).reason());
                assertEquals("NONE",cast(f,0,1,4).reason());
                elapsed(f,expires?3:duration-1);action.set(()->elapsed(f,duration+1));var after=f.view();assertNull(action.get());
                assertEquals((duration+1)*SECOND,after.clock().activeElapsedNanos());assertFalse(actor(after,0).buffs().hasEffects());
                assertTrue(after.hasteTargets().targets().isEmpty());assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());
                if(expires) {
                    assertEquals(0,actor(after,0).resources().equipmentRevision());
                    assertEquals(0,actor(after,0).regeneration().mp().gain());
                    var caughtUp=f.view();assertEquals(2,actor(caughtUp,0).resources().equipmentRevision());
                    assertFalse(actor(caughtUp,0).buffs().hasEffects());
                }
            }
        }
    }

    @Test void speedGrantDuringRealDurableVipRemovalSurvivesWithoutRepricingItsReceipt()throws Exception {
        for(int skill:List.of(43,54))try(var f=fixture(-1,-1,0,b->0)) {
            f.active();assertEquals("NONE",cast(f,0,1,4).reason());elapsed(f,2);assertEquals("NONE",speed(f,0,2,54,0).reason());
            try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,f.epoch.get());change.prepareVip(candidate,f.epoch.get());
                var during=new AtomicReference<ModernBattleObservation.Capture>();
                try(var store=new ModernEquipmentOverrideStore(Files.createDirectory(directory.resolve("rename"+skill)),new ModernEquipmentOverrideStore.FileOps(){
                    @Override public void move(Path from,Path to)throws IOException {
                        var grant=speed(f,1,1,skill,0);assertEquals("NONE",grant.reason());during.set(grant.observation());
                        ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    }
                })) {
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                    var after=f.view();for(int i=0;i<2;i++) {
                        assertEquals(actor(during.get(),i).buffs(),actor(after,i).buffs());assertEquals(armor(during.get(),i),armor(after,i));
                        assertEquals(actor(during.get(),i).runtime(),actor(after,i).runtime());
                        assertEquals(actor(during.get(),i).resources().flows().spent(),actor(after,i).resources().flows().spent());
                    }
                    assertEquals(1,actor(after,0).resources().equipmentRevision());assertEquals(0,actor(after,1).resources().equipmentRevision());
                    assertEquals(during.get().hasteTargets(),after.hasteTargets());
                }
            }
        }
    }

    @Test void independentTargetGrantsKeepTheirOwnOldVersionsAfterExpiryAndArmorSaving()throws Exception {
        try(var f=fixture(4,-1,0,b->0);var store=new ModernEquipmentOverrideStore(directory)) {
            f.active();assertEquals("NONE",speed(f,0,1,43,1).reason());elapsed(f,2);assertEquals("NONE",speed(f,0,2,43,0).reason());
            var before=f.view();assertEquals(2,before.hasteTargets().targets().size());
            elapsed(f,4);var expired=f.view();assertEquals(before.hasteTargets(),expired.hasteTargets());
            ModernBattleEngineVipArmorTest.publishArmor(f,1,false,store);var saved=f.view();
            assertEquals(before.hasteTargets(),saved.hasteTargets());assertEquals(2,actor(saved,1).resources().equipmentRevision());
            assertEquals(1,actor(saved,1).loadout().fighter().vip().state().ledger().equipmentRevision());
            assertEquals(actor(before,1).buffs(),actor(saved,1).buffs());
            elapsed(f,1200);assertEquals(1,f.view().hasteTargets().targets().size());
            elapsed(f,1202);assertTrue(f.view().hasteTargets().targets().isEmpty());
        }
    }

    @Test void targetedHasteDuringDamageCalculationInvalidatesOldCasterSpeedWithoutPartialDamage()throws Exception {
        for(int skill:List.of(0,4,70,77)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(-1,-1,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                var before=f.active();action.set(()->assertEquals("NONE",speed(f,1,1,43,0).reason()));
                var out=ModernBattleEngineVipArmorTest.hit(f,0,1,skill);assertEquals("STALE_EFFECT",out.reason());assertNull(action.get());
                assertEquals(actor(before,1).resources().vitals().hp(),actor(out.observation(),1).resources().vitals().hp());
                assertEquals(0,actor(out.observation(),0).resources().flows().spent());assertTrue(actor(out.observation(),1).resources().flows().spent()>0);
                assertEquals(ModernTimedEffects.HasteOrigin.HASTE,actor(out.observation(),0).buffs().hasteOrigin());
                assertEquals(armor(before,1),armor(out.observation(),1));assertEquals(1,out.observation().hasteTargets().targets().size());
            }
        }
    }

    @Test void wisdomExpiringDuringAnyMagicCalculationCannotCommitItsOldSpBonus()throws Exception {
        for(int skill:List.of(4,70,77)) {
            var action=new AtomicReference<Runnable>();
            try(var f=fixture(-1,-1,0,b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;})) {
                f.active();use(f,0,1,WISDOM);elapsed(f,299);var before=f.view();action.set(()->elapsed(f,301));
                var out=cast(f,0,1,skill);assertEquals("STALE_EFFECT",out.reason());assertNull(action.get());
                assertEquals(actor(before,1).resources(),actor(out.observation(),1).resources());assertEquals(armor(before,1),armor(out.observation(),1));
                assertEquals(0,actor(out.observation(),0).resources().flows().spent());assertEquals(0,actor(out.observation(),0).buffs().spBonus());
                assertEquals(actor(before,0).supplies(),actor(out.observation(),0).supplies());
            }
        }
    }

    @Test void worldTimeoutAndCancellationClearSpeedAndArmorWithoutRefundingCostsOrStock()throws Exception {
        for(boolean timeout:List.of(false,true))try(var f=fixture(-1,-1,0,b->0)) {
            f.active();speed(f,0,1,54,0);elapsed(f,timeout?1799:2);use(f,1,1,HASTE);
            assertEquals("NONE",cast(f,0,2,4).reason());var before=f.view();assertTrue(armor(before,1).state().untilNanos()>0);
            if(timeout)elapsed(f,1800);else f.rooms.rooms.releasePreparation(f.ticket);
            var after=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(timeout?ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT:ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,after.abortReason());
            for(int i=0;i<2;i++) {
                assertFalse(actor(after,i).buffs().hasEffects());assertEquals(0,armor(after,i).state().untilNanos());
                assertEquals(actor(before,i).resources(),actor(after,i).resources());assertEquals(actor(before,i).supplies(),actor(after,i).supplies());
            }
            assertTrue(after.hasteTargets().targets().isEmpty());assertEquals(after,f.view());
        }
    }

    @Test void fullVipCannotInventLearnedHasteOrAcceptUnsupportedSourceTemplates()throws Exception {
        for(var status:List.of(ModernHasteCatalog.Status.UNLEARNED,ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE))
            try(var f=fixture(-1,-1,0,b->0,status)) {
                var before=f.active();for(int skill:List.of(43,54))assertEquals(status==ModernHasteCatalog.Status.UNLEARNED?"UNLEARNED":"UNSUPPORTED",speed(f,0,skill==43?1:2,skill,1).reason());
                for(int i=0;i<2;i++){assertEquals(actor(before,i).resources(),actor(f.view(),i).resources());assertFalse(actor(f.view(),i).buffs().hasEffects());}
                assertTrue(f.view().hasteTargets().targets().isEmpty());assertEquals(2,actor(f.view(),0).runtime().sequence());
            }
    }

    @Test void insufficientMpAfterRepricingCannotRefreshAnExistingLease()throws Exception {
        for(int skill:List.of(43,54))try(var f=fixture(4,-1,0,b->0)) {
            f.active();boolean denied=false;
            for(int seq=1;seq<=80;seq++) {
                elapsed(f,(seq-1)*2);var before=f.view();var out=speed(f,0,seq,skill,0);
                if(out.reason().equals("NO_MP")) {
                    assertEquals(actor(before,0).resources(),actor(out.observation(),0).resources());assertEquals(actor(before,0).buffs(),actor(out.observation(),0).buffs());
                    assertEquals(actor(before,0).histories(),actor(out.observation(),0).histories());assertEquals(before.hasteTargets(),out.observation().hasteTargets());
                    denied=true;break;
                }
                assertEquals("NONE",out.reason());
            }
            assertTrue(denied);assertEquals(1,actor(f.view(),0).resources().equipmentRevision());
        }
    }

    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,long food,ModernPhysicalCombat.BoundedRandom random)throws Exception {
        return fixture(vip,gear,food,random,ModernHasteCatalog.Status.QUALIFIED);
    }
    static ModernBattleEngineVipTest.Fixture fixture(int vip,int gear,long food,ModernPhysicalCombat.BoundedRandom random,ModernHasteCatalog.Status status)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(false,ModernBattleEngineVipArmorTest.ABILITIES,new AtomicLong(1000),random,
                s->ModernBattleEngineVipArmorTest.sourceWithArmor(s,vip,gear,10000,food,100),p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),p->{
                    var s=p.fighter().vip().source();var fighter=ModernPhysicalCombat.qualifyVipProtection(p.fighter().vip().state(),1000,ModernArmorProtectionLoadoutTest.bound(s));
                    return enable(advanced(p.withVip(fighter)),status);
                },SECOND,0,2,false,new int[]{3,2},1_800_000);
    }
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,ModernHasteCatalog.Status status) {
        var skills=new ArrayList<>(p.skills().skills());
        if(status!=ModernHasteCatalog.Status.UNLEARNED) {
            skills.add(new ModernSkills.Skill(43,"$1463",40,10,ModernSkills.Support.UNIMPLEMENTED));
            skills.add(new ModernSkills.Skill(54,"$1976",60,10,ModernSkills.Support.UNIMPLEMENTED));
        }
        skills.sort(Comparator.comparingInt(ModernSkills.Skill::skillId));
        var stocks=ModernShopTimedTest.catalog(p.character().characterClass()).offers().stream().sorted(Comparator.comparingLong(ModernShopCatalog.Offer::itemId)).map(o->new ModernShopAuthority.Stack(o,4)).toList();
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),p.castIntervalNanos(),true,stocks,ModernInventoryWeight.qualify(p.fighter().profile(),100),
                true,false,true,true,p.disintegrate(),null,new ModernGreaterHasteCatalog.Snapshot(p.fighter().key(),ModernGreaterHasteCatalog.Status.valueOf(status.name())),
                new ModernHasteCatalog.Snapshot(p.fighter().key(),status),null,false,true);
    }
    static ModernBattleEngine.ItemResult use(ModernBattleEngineVipTest.Fixture f,int who,long seq,ModernTimedEffects.Kind kind) {
        var p=f.players.get(who);return f.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),seq,kind.itemId()));
    }
    static ModernBattleEngine.MoveResult move(ModernBattleEngineVipTest.Fixture f,int who,long seq,int heading) {
        var p=f.players.get(who);var at=actor(f.view(),who).pose().position();return f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),seq,at.x(),at.y(),heading);
    }
    static ModernUseHistory.Lane speedLane(int skill){return skill==43?ModernUseHistory.Lane.HASTE:ModernUseHistory.Lane.GREATER_HASTE;}
    static ModernGreaterHasteCast.Request greaterRequest(ModernBattleObservation.Capture c,int who,long seq) {
        var a=actor(c,who);return new ModernGreaterHasteCast.Request(a.scope(),c.identity().matchId(),c.identity().generation(),seq,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position().x(),a.pose().position().y(),a.buffs().revision());
    }
    static ModernHasteCast.Request hasteRequest(ModernBattleObservation.Capture c,int who,long seq,int target) {return ModernBattleHasteTest.request(c,who,seq,ModernBattleHasteTest.target(c,target));}
    static Reply reply(ModernBattleEngine.HasteResult result){return new Reply(result.reason().name(),result.observation());}
    static Reply reply(ModernBattleEngine.GreaterHasteResult result){return new Reply(result.reason().name(),result.observation());}
    static Reply speed(ModernBattleEngineVipTest.Fixture f,int who,long seq,int skill,int target) {
        var c=f.view();return skill==43?reply(f.engine.haste(f.players.get(who).owner(),hasteRequest(c,who,seq,target))):reply(f.engine.greaterHaste(f.players.get(who).owner(),greaterRequest(c,who,seq)));
    }
}
