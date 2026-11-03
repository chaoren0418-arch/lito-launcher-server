package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import org.junit.jupiter.api.Test;

/** Public projections cannot drop the private runtime's cross-skill guards. */
class ModernPlayerHealingObservationTest {
    @Test void publicSpendingRejectsImpossiblePriceTotalsEvenWithNoCast()throws Exception {
        try(var f=fixture(0,100,100)) {
            var actor=active(f).world().actors().get(0);var h=actor.playerHealing();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.PlayerHealing(h.incoming(),ModernUseHistory.Spell.initial(0,1,5),null));
            var cast=heal(f,0,1,1).observation().world().actors().get(0).playerHealing().receipt();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.PlayerHealingReceipt(cast.impact(),1,5,cast.loadingElapsedNanos()));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.PlayerHealingReceipt(cast.impact(),2,8,cast.loadingElapsedNanos()));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.PlayerHealingReceipt(cast.impact(),1,3,cast.loadingElapsedNanos()));
        }
    }
    @Test void publicHealingCannotReuseAnotherSkillSequenceOrReverseItsTime()throws Exception {
        try(var f=fixture(0,100,100)) {
            active(f);f.elapsed(2*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            f.elapsed(4*SECOND);var world=heal(f,0,2,1).observation().world();var actor=world.actors().get(0);var h=actor.playerHealing();
            var r=h.receipt();var use=r.impact().use();var impact=r.impact();var previous=actor.spells().receipts().get(ModernUseHistory.Lane.LIGHT_ARROW);
            var earlier=new ModernBattleObservation.PlayerHealing(h.incoming(),h.outgoing(),
                    new ModernBattleObservation.PlayerHealingReceipt(impact,r.casts(),r.spent(),previous.loadingElapsedNanos()-1));
            assertThrows(IllegalArgumentException.class,()->earlier.validate(world.identity(),impact.caster(),actor.resources(),actor.spells(),world.clock()));
            var duplicateUse=new ModernUseHistory.Cast(use.revision(),1,use.before(),use.cost(),use.mpAfter());
            var duplicateImpact=new ModernPlayerHealingHistory.Impact(impact.world(),impact.caster(),duplicateUse,impact.targetEffectRevision(),
                    impact.effects(),impact.baseAmount(),impact.adjustedAmount(),impact.target());
            var original=h.outgoing().current();var duplicateSpending=new ModernUseHistory.Spell(new ModernSkillSpending.Ledger(original.equipmentRevision(),
                    original.profileRevision(),original.cost(),original.priorCasts(),original.priorSpent(),original.casts(),original.spent(),use.revision(),1),duplicateUse);
            var duplicate=new ModernBattleObservation.PlayerHealing(h.incoming(),duplicateSpending,new ModernBattleObservation.PlayerHealingReceipt(duplicateImpact,r.casts(),r.spent(),r.loadingElapsedNanos()));
            assertThrows(IllegalArgumentException.class,()->duplicate.validate(world.identity(),impact.caster(),actor.resources(),actor.spells(),world.clock()));
        }
    }
}
