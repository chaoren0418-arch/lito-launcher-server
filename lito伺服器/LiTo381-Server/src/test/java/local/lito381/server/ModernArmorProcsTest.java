package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernArmorProcsTest {
    static ModernArmorProcs.Rule rule(long id,long catalog,long enchant,long group,long attack,long hurt) {
        return new ModernArmorProcs.Rule(id,catalog,enchant,group,attack,hurt,0,0,0,0,0,0,0,68,3,1,11101,1);
    }
    @Test void allExactRowsWinOverAllWildcardGroupsAndZeroChanceDoesNotFallBack() {
        var wild=rule(1,88889,-1,88,0,10);var exactA=rule(3,88889,5,90,0,0);var exactB=rule(2,88889,5,89,0,20);
        var catalog=new ModernArmorProcs.Catalog(7,"catalog",Map.of(88889L,List.of(exactA,wild,exactB),9L,List.of()));
        assertEquals(List.of(exactB,exactA),catalog.selected(88889,5));
        assertEquals(List.of(wild),catalog.selected(88889,0));
        assertEquals(List.of(wild),catalog.selected(88889,Long.MIN_VALUE));
        assertEquals(List.of(wild),catalog.selected(88889,5L+(1L<<32)));
        assertEquals(List.of(),catalog.selected(9,5));
        assertThrows(IllegalArgumentException.class,()->catalog.selected(10,5));
        assertEquals(List.of(exactA),new ModernArmorProcs.Catalog(7,"catalog",Map.of(88889L,List.of(wild,exactA))).selected(88889,5));
    }
    @Test void catalogIsImmutableBoundedAndRejectsForeignOrDuplicateIdentities() {
        var row=rule(1,2,-1,0,0,10);var list=new ArrayList<>(List.of(row));
        var catalog=new ModernArmorProcs.Catalog(7,"catalog",Map.of(2L,list));list.clear();
        assertEquals(1,catalog.byItem().get(2L).size());
        assertThrows(UnsupportedOperationException.class,()->catalog.byItem().clear());
        assertThrows(UnsupportedOperationException.class,()->catalog.byItem().get(2L).clear());
        assertThrows(IllegalArgumentException.class,()->new ModernArmorProcs.Catalog(7,"catalog",Map.of(3L,List.of(row))));
        assertThrows(IllegalArgumentException.class,()->new ModernArmorProcs.Catalog(7,"catalog",Map.of(2L,List.of(row,row))));
        assertThrows(IllegalArgumentException.class,()->rule(0,2,-1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->rule(1,2,-2,0,0,0));
        assertFalse(catalog.toString().contains("88889"));
    }
    @Test void probabilitiesAreClampedWithoutNativeNarrowingAndConsumeExactlyOneDraw() {
        var calls=new AtomicInteger();ModernPhysicalCombat.BoundedRandom rng=bound->{assertEquals(100,bound);calls.incrementAndGet();return 9;};
        assertTrue(ModernArmorProcs.roll(rule(1,2,-1,0,0,10),true,rng));
        assertFalse(ModernArmorProcs.roll(rule(1,2,-1,0,0,9),true,rng));
        assertFalse(ModernArmorProcs.roll(rule(1,2,-1,0,0,Long.MIN_VALUE),true,rng));
        assertTrue(ModernArmorProcs.roll(rule(1,2,-1,0,0,Long.MAX_VALUE),true,rng));assertEquals(3,calls.get());
        for(long invalid:new long[]{-1,100,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,
                ()->ModernArmorProcs.roll(rule(1,2,-1,0,0,10),true,bound->invalid));
    }
    @Test void amountsAreInclusiveWideAndRejectWholeRangeOverflowBeforeDrawing() {
        assertEquals(5_000_000_000L,ModernArmorProcs.amount(5_000_000_000L,0,b->{fail();return 0;}));
        assertEquals(17,ModernArmorProcs.amount(10,7,b->{assertEquals(8,b);return 7;}));
        assertEquals(0,ModernArmorProcs.amount(-100,-1,b->{fail();return 0;}));
        assertEquals(Long.MAX_VALUE,ModernArmorProcs.amount(Long.MAX_VALUE,0,b->0));
        assertThrows(ArithmeticException.class,()->ModernArmorProcs.amount(Long.MAX_VALUE,1,b->{fail();return 0;}));
        assertThrows(ArithmeticException.class,()->ModernArmorProcs.amount(0,Long.MAX_VALUE,b->{fail();return 0;}));
        assertThrows(IllegalArgumentException.class,()->ModernArmorProcs.amount(0,7,b->8));
    }
}
