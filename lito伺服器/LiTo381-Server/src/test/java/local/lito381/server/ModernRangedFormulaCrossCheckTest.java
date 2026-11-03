package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

/** Independent comparison with retained LiTo helpers, not duplicate new-formula expectations. */
class ModernRangedFormulaCrossCheckTest {
    static final ModernAmmo.Facts SILVER=new ModernAmmo.Facts(40744,7,6,-1,true,0,0);
    static ModernRangedCombat.Values values(long hp,long rate,long bonus,long ac,long er,long maximum,long critical) {
        var weapon=new ModernCombatProfile.WeaponFacts(1,1,4,12345,23456,10,11,0,maximum,critical,2,0,0);
        var defense=new ModernPhysicalCombat.Defense(ac,ac<0?BattlePhysicalAttack.negativeAcRollMaximum((int)ac):0,0,0,0,0);
        return new ModernRangedCombat.Values(true,true,hp,10,er,new ModernRangedCombat.Offense(rate,bonus,0),defense,defense,weapon,new ModernPhysicalCombat.Rational(2,1));
    }
    static final class Dice implements ModernPhysicalCombat.BoundedRandom {
        final ArrayDeque<long[]> expected=new ArrayDeque<>();
        Dice add(long bound,long value){expected.add(new long[]{bound,value});return this;}
        public long nextLong(long bound){var e=expected.poll();assertNotNull(e,"unexpected RNG draw");assertEquals(e[0],bound);assertTrue(e[1]>=0&&e[1]<bound);return e[1];}
        void done(){assertTrue(expected.isEmpty(),"required native RNG draw was skipped");}
    }
    @Test void evasionMatchesRetainedClassAndOriginalDexterityTablesIncludingNegativeDexCorrection() {
        int cases=0;
        for(int type=0;type<7;type++)for(int level:new int[]{1,7,8,9,40,80,127,10000})
            for(int dex:new int[]{1,2,7,8,9,18,35,127})for(int original=0;original<=25;original++)
                for(int equipment:new int[]{-50,0,1,99,200}) {
                    assertEquals(BattleRangedEvasion.total(type,level,dex,original,equipment,0),
                            ModernRangedCombat.evasion(type,level,dex,original,equipment));cases++;
                }
        assertEquals(58240,cases);
    }
    @Test void hitEnvelopeAndConditionalEvasionDrawMatchExistingLiToAcrossExactThresholds() {
        int cases=0;
        for(int type=0;type<7;type++)for(int level:new int[]{1,40,80})for(int stat:new int[]{-5,0,15})
            for(int ac:new int[]{-20,0,20})for(int d20:new int[]{1,2,10,19,20})
                for(int d100:new int[]{1,15,16,99,100})for(int er:new int[]{0,10,100}) {
                    int negative=ac<0?BattlePhysicalAttack.negativeAcRollMaximum(ac):0;
                    int chance=BattlePhysicalAttack.hitChanceWithStatBonus(type,level,stat,ac,0,0,d20,negative);
                    long rate=level+BattlePhysicalAttack.classHitCorrection(type,level)+stat;
                    var dice=new Dice().add(20,d20-1);
                    if(ac<0)dice.add(negative,negative-1);
                    dice.add(100,d100-1);
                    boolean hit=BattlePhysicalAttack.hits(chance,d100);
                    if(hit&&chance>d100){dice.add(100,49);hit=BattleRangedEvasion.hitsAfterEvasion(er,50);}
                    dice.add(7,6); // ordinary arrows include their maximum face even after a MISS.
                    var actual=ModernRangedCombat.resolve(values(100,rate,3,0,0,0,0),values(100,1,0,ac,er,0,0),SILVER,100,dice,0);
                    assertEquals(hit,actual.hit());assertEquals(hit?10:0,actual.appliedDamage());
                    assertEquals(hit?90:100,actual.remainingHp());dice.done();cases++;
                }
        assertEquals(14175,cases);
    }
    @Test void missStillRollsMaximumAndCriticalButDoesNotReportDamageOrProcFlags() {
        var dice=new Dice().add(20,0).add(100,99).add(100,49).add(100,39);
        var result=ModernRangedCombat.resolve(values(100,10,3,0,0,50,40),values(100,1,0,0,0,0,0),SILVER,100,dice,0);
        assertFalse(result.hit());assertEquals(0,result.rawDamage());assertEquals(0,result.appliedDamage());
        assertFalse(result.death());assertFalse(result.maximumTriggered());assertFalse(result.criticalTriggered());dice.done();
    }
    @Test void wideArrowDamageIsNotTruncatedAndOverflowIsAnExplicitNumericFailure() {
        var arrow=new ModernAmmo.Facts(40746,4_294_967_301L,4_294_967_302L,66,true,1,0);
        var dice=new Dice().add(20,19).add(100,99).add(arrow.small(),arrow.small()-1);
        var result=ModernRangedCombat.resolve(values(Long.MAX_VALUE,100,9,0,0,0,0),values(Long.MAX_VALUE,1,0,0,0,0,0),arrow,9_000_000_000L,dice,0);
        assertEquals(4_294_967_310L,result.appliedDamage());assertEquals(4_705_032_690L,result.remainingHp());dice.done();
        var huge=new ModernAmmo.Facts(40746,Long.MAX_VALUE,Long.MAX_VALUE,-1,true,1,0);
        var overflow=new Dice().add(20,19).add(100,99).add(Long.MAX_VALUE,Long.MAX_VALUE-1);
        var error=assertThrows(ModernPhysicalCombat.CombatException.class,()->ModernRangedCombat.resolve(
                values(Long.MAX_VALUE,100,1,0,0,0,0),values(Long.MAX_VALUE,1,0,0,0,0,0),huge,Long.MAX_VALUE,overflow,0));
        assertEquals(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW,error.failure());overflow.done();
        assertEquals(5_000_000_000L/8+((6_000_000_000L-8)>>1)+2+7,ModernRangedCombat.evasion(0,5_000_000_000L,6_000_000_000L,16,7));
        assertThrows(ArithmeticException.class,()->ModernRangedCombat.evasion(0,Long.MAX_VALUE,Long.MAX_VALUE,16,Long.MAX_VALUE));
    }
}
