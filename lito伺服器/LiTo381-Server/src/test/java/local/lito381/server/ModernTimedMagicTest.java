package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Accounts INT+SP -> ClientSession timed SP -> BattleElementalResistance subtraction/clamp. */
class ModernTimedMagicTest {
    static ModernMagicCombat.Values values(long intelligence,long sp,long term) {
        return new ModernMagicCombat.Values(true,1000,new ModernMagicCombat.Offense(intelligence,sp,term,0,0,0,0),
                new ModernMagicCombat.Defense(0,0,0,0,0,0,0));
    }
    static long damage(long intelligence,long sp,long term,int timedSp) {
        return ModernMagicCombat.resolveValues(values(intelligence,sp,term),values(12,0,1),1000,bound->bound-1,timedSp).appliedDamage();
    }
    @Test void lowIntelligenceAndNegativeEquipmentSpAreClampedAfterWisdomNotBefore() {
        for(long intelligence:new long[]{0,5,10,11})assertEquals(damage(intelligence,0,1,0),damage(intelligence,0,1,2));
        assertEquals(damage(20,-20,1,0),damage(20,-20,1,2));
        assertEquals(16*38/32,damage(12,0,1,2));
        assertEquals(16*44/32,damage(20,-6,2,2));
        assertTrue(damage(12,0,1,2)>damage(12,0,1,0));
    }
    @Test void timedZeroRetainsQualifiedLegacyTermAndAllAddedArithmeticFailsClosed() {
        assertEquals(16*35/32,damage(10,0,1,0));
        for(var offensive:new ModernMagicCombat.Values[]{values(Long.MAX_VALUE,0,1),values(Long.MAX_VALUE,-1,1),values(0,Long.MIN_VALUE,1)}) {
            var exception=assertThrows(ModernMagicCombat.CombatException.class,()->ModernMagicCombat.resolveValues(offensive,values(12,0,1),1000,bound->bound-1,2));
            assertEquals(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,exception.failure());
        }
        assertThrows(IllegalArgumentException.class,()->ModernMagicCombat.resolveValues(values(12,0,1),values(12,0,1),1000,bound->bound-1,1));
    }
}
