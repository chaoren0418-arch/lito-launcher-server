package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTimedEffects.*;
import static local.lito381.server.ModernTimedEffectsTest.sec;
import org.junit.jupiter.api.Test;

class ModernGreaterHasteLeaseTest {
    @Test void potionAndSkillHaveDifferentMaximumsAndNoSourceTagCanSmuggleALongPotion() {
        var skill=applyGreaterHaste(initial(1),0,1);assertEquals(sec(2400),skill.hasteUntilNanos());assertEquals(HasteOrigin.GREATER_HASTE,skill.hasteOrigin());
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,sec(2400),0,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,sec(2400),0,0,0,HasteOrigin.POTION));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,sec(2400)+1,0,0,0,HasteOrigin.GREATER_HASTE));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,0,0,0,0,HasteOrigin.GREATER_HASTE));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,sec(1),0,0,0,HasteOrigin.NONE));
        assertEquals(HasteOrigin.GREATER_HASTE,skill.wireRevision().hasteOrigin());
    }
    @Test void unrelatedLeaseExpiryRetainsTheSkillOriginAndClearRemovesItAtTheUnchanged30MinuteBound() {
        var wisdom=apply(initial(1),Kind.WISDOM,0,1,3);var both=applyGreaterHaste(wisdom,sec(1),2);
        var expired=expire(both,sec(300),3);assertEquals(0,expired.wisdomUntilNanos());assertEquals(HasteOrigin.GREATER_HASTE,expired.hasteOrigin());
        assertEquals(sec(2401),expired.hasteUntilNanos());assertSame(expired,expire(expired,sec(1800),4));
        var cleared=clear(expired,sec(1800),4);assertEquals(HasteOrigin.NONE,cleared.hasteOrigin());assertFalse(cleared.hasEffects());
        // The current match ends at 30min; do not pretend a 40min world timer was tested.
        assertThrows(IllegalArgumentException.class,()->expire(expired,sec(2401),5));
    }
    @Test void refreshOverwritesThePreviousDeadlineAndBraveKeepsItsIndependentDuration() {
        var brave=apply(initial(1),Kind.BRAVE,0,1,1);var first=applyGreaterHaste(brave,sec(10),2);
        var refreshed=applyGreaterHaste(first,sec(20),3);assertEquals(sec(2420),refreshed.hasteUntilNanos());assertEquals(sec(300),refreshed.braveUntilNanos());
        assertEquals(114_000_000L,intervalNanos(200_000_000L,Action.MOVE,refreshed));
        var potion=apply(refreshed,Kind.HASTE,sec(21),4,1);assertEquals(sec(321),potion.hasteUntilNanos());assertEquals(HasteOrigin.POTION,potion.hasteOrigin());
        assertEquals(sec(300),potion.braveUntilNanos());
    }
    @Test void spellMetadataRequiresExactDurationPriceAndAnEmptyTargetBatch() {
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.GreaterHaste(0,sec(300)));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.GreaterHaste(-1,sec(2400)-1));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.GreaterHaste(sec(1800)+1,sec(4200)+1));
        var id=ModernEquipmentHistoryTest.ID;var lane=ModernUseHistory.Lane.GREATER_HASTE;
        for(int cost:new int[]{24,25,60,61}) {
            var resource=ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(1000,1000,1000,1000));
            var h=new ModernEquipmentHistory.State(id,1,resource,java.util.Map.of(lane,ModernUseHistory.Spell.initial(0,1,cost)),null,java.util.Map.of());
            var runtime=ModernSpellRuntime.State.initial(id,h.spells().keySet(),1,0);
            var next=h.cast(lane,2,1,ModernTargetHistoryTest.CELL,java.util.List.of());
            var detail=new ModernSpellRuntime.GreaterHaste(0,sec(2400));
            if(cost==25||cost==60)assertDoesNotThrow(()->runtime.cast(h,next,lane,0,detail));
            else assertThrows(IllegalArgumentException.class,()->runtime.cast(h,next,lane,0,detail));
            assertThrows(IllegalArgumentException.class,()->runtime.cast(h,next,lane,0,ModernSpellRuntime.Ordinary.INSTANCE));
            assertThrows(IllegalArgumentException.class,()->h.cast(lane,2,1,ModernTargetHistoryTest.CELL,
                    ModernEquipmentHistoryTest.impacts(ModernUseHistory.Lane.LIGHT_ARROW)));
        }
    }
}
