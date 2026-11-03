package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ModernFireStormTest {
    static ModernFireStorm.Values values(long intelligence,long sp,long damage,long fire,long mr,long armor){
        return new ModernFireStorm.Values(new ModernMagicCombat.Values(true,Long.MAX_VALUE,
                new ModernMagicCombat.Offense(intelligence,sp,Math.max(1,intelligence+sp-12),0,0,0,damage),
                new ModernMagicCombat.Defense(0,0,0,0,mr,mr,armor)),fire);
    }
    private static long checkedMrOracle(long damage,int mr){
        int[] ceilings={0,50,100,120,140,160,180,200,220,240,Integer.MAX_VALUE};
        int[] divisors={1,2,3,4,5,6,7,8,9,10,11},percent={100,100,90,90,80,80,70,70,60,60,50};
        int band=0;while(mr>ceilings[band])band++;long numerator=Math.max(0,percent[band]-mr/divisors[band]);
        return java.math.BigInteger.valueOf(damage).multiply(java.math.BigInteger.valueOf(numerator)).divide(java.math.BigInteger.valueOf(100)).longValueExact();
    }
    @Test void retainedSignedElementIsExactAndModernMrUsesIndependentCheckedIntegerPolicyWithoutCircleNineCritical(){
        for(int intelligence:new int[]{1,10,12,13,19,30,127})for(int fire:new int[]{-128,-127,-100,-4,-1,0,1,4,100,127})
            for(int die:new int[]{0,7,14})for(int mr:new int[]{0,1,50,51,100,101,200}){
                int source=24+8*(die+1),element=BattleElementalResistance.magicDamage(source,intelligence,fire);
                long expected=checkedMrOracle(element,mr);
                var a=values(intelligence,0,0,0,0,0);var d=values(12,0,0,fire,mr,0);
                var bounds=new ArrayList<Long>();var outcome=ModernFireStorm.resolve(a,d,Long.MAX_VALUE,b->{bounds.add(b);return b==15?die:0;},0);
                assertEquals(expected,outcome.rawDamage(),"INT/FIRE/die/MR="+intelligence+"/"+fire+"/"+die+"/"+mr);
                assertEquals(java.util.List.of(15L,15L,15L,15L,15L,15L,15L,15L,100L),bounds);
            }
    }
    @Test void existingModernMrPolicyExplicitlyDiffersAtNativeBinary64TruncationBoundary(){
        int elemental=BattleElementalResistance.magicDamage(144,127,-100);assertEquals(1840,elemental);
        assertEquals(827,BattleMagicResistance.damageAfterResistance(elemental,200,0));
        assertEquals(828,checkedMrOracle(elemental,200));
        assertEquals(828,ModernFireStorm.resolve(values(127,0,0,0,0,0),values(12,0,0,-100,200,0),Long.MAX_VALUE,b->b-1,0).rawDamage());
    }
    @Test void wisdomIsAddedBeforeClampAndArmorDrawRemainsAfterIgnoredCriticalDie(){
        var a=values(10,-1,20,0,0,0);var d=values(12,0,0,0,0,20);var bounds=new ArrayList<Long>();
        var out=ModernFireStorm.resolve(a,d,100,b->{bounds.add(b);return b-1;},2);
        assertEquals(157,out.rawDamage());assertEquals(100,out.appliedDamage());assertEquals(0,out.remainingHp());assertTrue(out.death());
        assertEquals(java.util.List.of(15L,15L,15L,15L,15L,15L,15L,15L,100L,10L),bounds);
    }
    @Test void wideI64DamageIsNotNarrowedAndEveryOverflowOrInvalidRandomIsTyped(){
        var a=values(12,0,1L<<40,0,0,0);var d=values(12,0,0,0,0,0);
        assertEquals((1L<<40)+35,ModernFireStorm.resolve(a,d,Long.MAX_VALUE,b->0,0).rawDamage());
        assertEquals(ModernFireStorm.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernFireStorm.CombatException.class,
                ()->ModernFireStorm.resolve(values(Long.MAX_VALUE,0,0,0,0,0),d,100,b->0,2)).failure());
        assertEquals(ModernFireStorm.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernFireStorm.CombatException.class,
                ()->ModernFireStorm.resolve(a,values(12,0,0,Long.MIN_VALUE,0,0),100,b->0,0)).failure());
        for(boolean upper:new boolean[]{false,true})assertEquals(ModernFireStorm.Failure.INVALID_RANDOM,assertThrows(ModernFireStorm.CombatException.class,
                ()->ModernFireStorm.resolve(a,d,100,b->upper?b:-1,0)).failure());
    }
    @Test void mpQualificationKeepsSourceOriginalClassBonusAndDoesNotUseAnIntNarrowingHelper(){
        for(int clazz=0;clazz<=6;clazz++)for(long intelligence:new long[]{1,12,13,18,19,127,1L<<32}){
            var row=new ModernApplicationProtocol.CharacterRow(3333,"synthetic",clazz,0,80);
            var s=ModernBattleEngineHonorTest.source(7,row,100,300);
            s=ModernMagicCombatTest.stats(s,intelligence,0,0,0,300);
            var f=ModernPhysicalCombatTest.fighter(s);var q=ModernFireStorm.qualify(f);
            int reduction=0;for(int n=12;n<=18;n++)if(intelligence>n)reduction++;
            long original=ModernMagicCombat.originalMpReduction(clazz,f.profile().source().persisted().originalIntelligence());
            assertEquals(48-reduction-original,q.mpCost());assertTrue(q.mpCost()>=39&&q.mpCost()<=48);
        }
    }
}
