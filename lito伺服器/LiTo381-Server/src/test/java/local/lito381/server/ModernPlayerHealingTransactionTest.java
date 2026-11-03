package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernPlayerHealingTransactionTest {
    static final ModernTargetHistory.World WORLD=new ModernTargetHistory.World(1,2,3);
    static ModernPlayerHealingTransaction.Actor actor(long entity,int source,long hp) {
        return new ModernPlayerHealingTransaction.Actor(WORLD,
                new ModernTargetHistory.Participant(entity,ModernParticipantId.human(source,3333)),
                ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(hp,100,100,100)),
                new ModernWorldMap.Cell(10,10),0,ModernPlayerHealing.Effects.NONE);
    }
    static ModernPlayerHealingTransaction.State state(boolean self) {
        var p=ModernPlayerHealingTest.profile();var caster=actor(1,7,40);
        return new ModernPlayerHealingTransaction.State(1,new ModernRooms.Scope(7,3333,1,0),caster,self?caster:actor(2,42,40),
                p,ModernPlayerHealingTest.skills(p),ModernSkillSpending.Ledger.initial(0,1,p.mpCost()),0,0);
    }
    static ModernPlayerHealingTransaction.State target(ModernPlayerHealingTransaction.State state,ModernPlayerHealingTransaction.Actor target) {
        return new ModernPlayerHealingTransaction.State(state.worldRevision(),state.scope(),state.caster(),target,state.profile(),state.skills(),state.spending(),state.sequence(),state.lawful());
    }
    static ModernPlayerHealingTransaction.Actor effects(ModernPlayerHealingTransaction.Actor actor,ModernPlayerHealing.Effects effects) {
        return new ModernPlayerHealingTransaction.Actor(actor.world(),actor.participant(),actor.resources(),actor.cell(),actor.effectRevision(),effects);
    }
    @Test void selfHealHasOneCombinedBalanceAndAnExactCasterAndTargetReceipt() {
        var before=state(true);var plan=ModernPlayerHealingTransaction.prepare(before,2,1,b->3);
        assertSame(plan.caster(),plan.target());assertEquals(48,plan.caster().vitals().hp());assertEquals(96,plan.caster().vitals().mp());
        assertEquals(8,plan.caster().flows().restored());assertEquals(4,plan.caster().flows().spent());
        assertEquals(1,plan.spending().casts());assertEquals(4,plan.spending().spent());assertEquals(1,plan.receipt().use().sequence());
        assertEquals(40,plan.receipt().use().before().vitals().hp());assertEquals(40,plan.receipt().target().before().hp());
        assertEquals(48,plan.receipt().target().hpAfter());assertTrue(plan.matches(before));
        assertEquals(40,before.caster().resources().vitals().hp());assertEquals(100,before.caster().resources().vitals().mp());
    }
    @Test void publicationOnlyRevisionAdvanceReusesTheExactRollAndOneCharge() {
        for(boolean self:new boolean[]{false,true}) {
            var draws=new AtomicInteger();var before=state(self);
            var original=ModernPlayerHealingTransaction.prepare(before,2,1,b->{draws.incrementAndGet();return 3;});
            int once=draws.get();var rebased=ModernPlayerHealingTransaction.atWorldRevision(original,50);
            assertEquals(once,draws.get());assertEquals(50,rebased.before().worldRevision());
            assertEquals(51,rebased.receipt().use().revision());assertEquals(51,rebased.caster().revision());
            assertEquals(original.caster().vitals(),rebased.caster().vitals());assertEquals(original.target().vitals(),rebased.target().vitals());
            assertEquals(original.receipt().baseAmount(),rebased.receipt().baseAmount());assertEquals(original.receipt().target(),rebased.receipt().target());
            assertEquals(1,rebased.spending().casts());assertEquals(4,rebased.spending().spent());
            assertEquals(2,original.receipt().use().revision());assertEquals(1,before.worldRevision());
            assertSame(rebased,ModernPlayerHealingTransaction.atWorldRevision(rebased,50));
            assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.atWorldRevision(rebased,49));
            assertThrows(ArithmeticException.class,()->ModernPlayerHealingTransaction.atWorldRevision(rebased,Long.MAX_VALUE));
        }
    }
    @Test void sameObjidAcrossSourcesIsNotSelfAndOnlyCasterPaysAndTargetRecovers() {
        var before=state(false);var plan=ModernPlayerHealingTransaction.prepare(before,2,1,b->3);
        assertEquals(40,plan.caster().vitals().hp());assertEquals(96,plan.caster().vitals().mp());
        assertEquals(48,plan.target().vitals().hp());assertEquals(100,plan.target().vitals().mp());
        assertEquals(0,plan.caster().flows().restored());assertEquals(0,plan.target().flows().spent());
        assertEquals(42,((ModernTargetHistory.Participant)plan.receipt().target().before().target()).identity().sourceId());
        assertFalse(plan.toString().contains("3333"));assertFalse(plan.receipt().toString().contains("3333"));
    }
    @Test void serverBotTargetRetainsItsNamespaceRatherThanBecomingADatabaseCharacter() {
        var before=state(false);var t=before.target();
        t=new ModernPlayerHealingTransaction.Actor(t.world(),new ModernTargetHistory.Participant(2,ModernParticipantId.bot(7,3333)),
                t.resources(),t.cell(),t.effectRevision(),t.effects());
        var result=ModernPlayerHealingTransaction.prepare(target(before,t),2,1,b->3);
        assertFalse(((ModernTargetHistory.Participant)result.receipt().target().before().target()).identity().isHuman());
        assertEquals(48,result.target().vitals().hp());
    }
    @Test void fullHealthAndErosionStillPayAndConsumeTargetWaterLifeButNeverDamage() {
        var before=state(false);
        for(boolean erosion:new boolean[]{false,true}) {
            var t=effects(actor(2,42,erosion?40:100),new ModernPlayerHealing.Effects(true,false,false,erosion));
            var plan=ModernPlayerHealingTransaction.prepare(target(before,t),2,1,b->3);
            assertEquals(t.resources().vitals().hp(),plan.target().vitals().hp());assertEquals(96,plan.caster().vitals().mp());
            assertEquals(0,plan.target().flows().damaged());assertFalse(plan.targetEffects().waterLife());
            assertEquals(erosion,plan.targetEffects().healingErosion());assertTrue(t.effects().waterLife());
        }
    }
    @Test void aliasesForeignWorldAndUnlearnedOrWrongSourceCasterAreRejected() {
        var before=state(false);var t=before.target();
        assertThrows(IllegalArgumentException.class,()->target(before,actor(1,42,40)));
        assertThrows(IllegalArgumentException.class,()->target(before,actor(2,7,40)));
        assertThrows(IllegalArgumentException.class,()->target(state(true),actor(1,7,41)));
        assertThrows(IllegalArgumentException.class,()->target(before,new ModernPlayerHealingTransaction.Actor(
                new ModernTargetHistory.World(1,2,4),t.participant(),t.resources(),t.cell(),0,t.effects())));
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.State(1,
                new ModernRooms.Scope(42,3333,1,0),before.caster(),t,before.profile(),before.skills(),before.spending(),0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.State(1,before.scope(),before.caster(),t,before.profile(),
                new ModernSkills.Snapshot(7,before.profile().character(),java.util.List.of()),before.spending(),0,0));
    }
    @Test void sequenceRevisionAndScreenChecksFailBeforeRngIncludingWideCoordinates() {
        var before=state(false);ModernPhysicalCombat.BoundedRandom forbidden=b->{throw new AssertionError("invalid admission used RNG");};
        for(long[] values:new long[][]{{1,1},{3,1},{2,0},{2,2}})
            assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.prepare(before,values[0],values[1],forbidden));
        var t=before.target();
        var far=new ModernPlayerHealingTransaction.Actor(t.world(),t.participant(),t.resources(),new ModernWorldMap.Cell(Integer.MAX_VALUE,Integer.MIN_VALUE),0,t.effects());
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.prepare(target(before,far),2,1,forbidden));
        assertEquals(0,before.spending().casts());
    }
    @Test void exhaustedMpAndDeadTargetAreRejectedWithoutPartialCostOrHeal() {
        var before=state(false);var a=before.caster();
        var dry=new ModernPlayerHealingTransaction.Actor(a.world(),a.participant(),
                ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(40,100,3,100)),a.cell(),0,a.effects());
        var current=new ModernPlayerHealingTransaction.State(1,before.scope(),dry,before.target(),before.profile(),before.skills(),before.spending(),0,0);
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.prepare(current,2,1,b->{throw new AssertionError();}));
        var t=before.target();
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.State(2,before.scope(),a,
                new ModernPlayerHealingTransaction.Actor(t.world(),t.participant(),t.resources().damage(2,40),t.cell(),0,t.effects()),
                before.profile(),before.skills(),before.spending(),0,0));
        assertEquals(3,current.caster().resources().vitals().mp());assertEquals(0,current.spending().spent());
    }
    @Test void staleTargetLifeGearCellHealthStatusSourceEpochOrSequenceCannotMatchPreparedState() {
        var before=state(false);var plan=ModernPlayerHealingTransaction.prepare(before,2,1,b->3);var t=before.target();
        var changed=java.util.List.of(
                new ModernPlayerHealingTransaction.Actor(t.world(),t.participant(),t.resources(),new ModernWorldMap.Cell(11,10),0,t.effects()),
                new ModernPlayerHealingTransaction.Actor(t.world(),t.participant(),t.resources(),t.cell(),1,t.effects()),
                effects(t,new ModernPlayerHealing.Effects(true,false,false,false)),
                actor(2,43,40));
        for(var candidate:changed)assertFalse(plan.matches(target(before,candidate)));
        var histories=java.util.List.of(t.resources().restore(2,1),t.resources().equip(2,1,100,100),t.resources().damage(2,40).respawn(3,40,0));
        for(var resources:histories)assertFalse(plan.matches(new ModernPlayerHealingTransaction.State(resources.revision(),before.scope(),before.caster(),
                new ModernPlayerHealingTransaction.Actor(t.world(),t.participant(),resources,t.cell(),0,t.effects()),before.profile(),before.skills(),before.spending(),0,0)));
        assertFalse(plan.matches(new ModernPlayerHealingTransaction.State(1,new ModernRooms.Scope(7,3333,2,0),before.caster(),t,
                before.profile(),before.skills(),before.spending(),0,0)));
        assertFalse(plan.matches(new ModernPlayerHealingTransaction.State(2,before.scope(),before.caster(),t,before.profile(),before.skills(),before.spending(),1,0)));
        var changedLawful=new ModernPlayerHealingTransaction.State(1,before.scope(),before.caster(),t,before.profile(),before.skills(),before.spending(),0,32767);
        assertFalse(plan.matches(changedLawful));
        assertEquals(15,ModernPlayerHealingTransaction.prepare(changedLawful,2,1,b->3).receipt().baseAmount());
    }
    @Test void failedRandomOrReceiptFabricationDoesNotPublishAnyCandidate() {
        var before=state(false);var draws=new AtomicInteger();
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.prepare(before,2,1,b->draws.incrementAndGet()==1?3:4));
        assertEquals(2,draws.get());assertEquals(40,before.target().resources().vitals().hp());assertEquals(100,before.caster().resources().vitals().mp());
        assertEquals(0,before.spending().casts());
        var receipt=ModernPlayerHealingTransaction.prepare(state(true),2,1,b->3).receipt();
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.Receipt(receipt.world(),receipt.source(),receipt.caster(),
                receipt.use(),0,receipt.effects(),receipt.baseAmount(),receipt.adjustedAmount()+1,receipt.target()));
    }
    @Test void qualifiedFreeCastStillHasASequenceAndReceiptAndOldHonorAndPotionHistoriesRejectTheNewEffect() {
        var before=state(false);var p=before.profile();
        var free=new ModernPlayerHealing.Profile(p.key(),p.character(),p.equipmentRevision(),p.maximumHp(),p.maximumMp(),
                new ModernHonorHealing.Profile(true,p.calculation().diceCount(),0,p.calculation().lawful()));
        var current=new ModernPlayerHealingTransaction.State(1,before.scope(),before.caster(),before.target(),free,before.skills(),
                ModernSkillSpending.Ledger.initial(0,1,0),0,0);
        var plan=ModernPlayerHealingTransaction.prepare(current,2,1,b->3);var receipt=plan.receipt();
        assertEquals(100,plan.caster().vitals().mp());assertEquals(1,plan.spending().casts());assertEquals(0,plan.spending().spent());
        assertEquals(1,receipt.use().sequence());assertEquals(48,plan.target().vitals().hp());
        assertThrows(IllegalArgumentException.class,()->new ModernTargetHistory.Batch(receipt.world(),receipt.source(),receipt.caster(),
                ModernUseHistory.Lane.HEALING,receipt.use(),java.util.List.of(receipt.target())));
        var identity=new ModernEquipmentHistory.Identity(WORLD,new ModernRooms.Scope(42,3333,1,0),before.target().participant());
        assertThrows(IllegalArgumentException.class,()->new ModernEquipmentHistory.State(identity,2,plan.target(),java.util.Map.of(),null,java.util.Map.of()));
    }
}
