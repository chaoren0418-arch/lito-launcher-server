package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernDisintegrateTest {
    static ModernMagicCombat.Values values(long intelligence,long sp,long damage,long mr,long armor) {
        return new ModernMagicCombat.Values(true,Long.MAX_VALUE,new ModernMagicCombat.Offense(intelligence,sp,1,99,0,0,damage),
                new ModernMagicCombat.Defense(0,0,0,0,mr,mr,armor));
    }
    private static long oracle(long beforeMr,long mr,long hit,boolean critical) {
        long[] max={0,50,100,120,140,160,180,200,220,240,Long.MAX_VALUE};
        int[] divisors={1,2,3,4,5,6,7,8,9,10,11},percent={100,100,90,90,80,80,70,70,60,60,50};
        int i=0;while(mr>max[i])i++;long multiple=Math.max(0,percent[i]-Math.floorDiv(mr-hit,divisors[i]));
        return BigInteger.valueOf(beforeMr).multiply(BigInteger.valueOf(multiple)).divide(BigInteger.valueOf(100))
                .multiply(BigInteger.valueOf(critical?2:1)).longValueExact();
    }
    @Test void nativeSourceDiceAndPostMrSpecialCriticalAreIndependentFromOrdinaryCritical() {
        for(int intelligence:new int[]{1,10,12,13,19,21,127,254})for(int die:new int[]{0,7,14})
            for(int mr:new int[]{0,1,50,51,100,101,120,121,140,141,160,161,180,181,200,201,220,221,240,241,999})
                for(int criticalRoll:new int[]{0,39,40,99}) {
                    int base=BattleElementalResistance.magicDamage(190+8*(die+1),intelligence,0);
                    var bounds=new ArrayList<Long>();
                    var out=ModernDisintegrate.resolve(values(intelligence,0,0,0,0),values(12,0,0,mr,0),Long.MAX_VALUE,b->{
                        bounds.add(b);return b==15?die:bounds.size()==9?criticalRoll:0;},0);
                    assertEquals(oracle(base,mr,0,criticalRoll<40),out.rawDamage());assertEquals(criticalRoll<40,out.critical());
                    assertEquals(criticalRoll<40?11748:11746,out.effect());
                    assertEquals(List.of(15L,15L,15L,15L,15L,15L,15L,15L,100L,100L),bounds);
                }
        // A pre-MR multiplier would incorrectly yield315 rather than314.
        assertEquals(314,ModernDisintegrate.resolve(values(12,0,0,0,0),values(12,0,0,51,0),1000,b->0,0).rawDamage());
    }
    @Test void wisdomOriginalEquipmentArmorAndCriticalOrderRemainExplicit() {
        var a=new ModernMagicCombat.Values(true,1000,new ModernMagicCombat.Offense(10,-1,1,99,4,3,20),new ModernMagicCombat.Defense(0,0,0,0,0,0,0));
        var bounds=new ArrayList<Long>();
        var out=ModernDisintegrate.resolve(a,values(12,0,0,51,20),90,b->{bounds.add(b);return bounds.size()==9?39:b-1;},2);
        // INT10-1+2 clamps to term1: floor310*35/32=339; +4+20-20=343;
        // MR51/originalHit3 =>74%; floor253.82=253, then special x2=506.
        assertEquals(506,out.rawDamage());assertEquals(90,out.appliedDamage());assertEquals(0,out.remainingHp());assertTrue(out.death());
        assertEquals(10L,bounds.get(10));assertEquals(11,bounds.size());
    }
    @Test void wideFinalValuesNeverNarrowAndRepresentableIntermediateOverflowIsAccepted() {
        var d=values(12,0,0,0,216);
        // Last armor offset is0 => armor11; use base10 so216+MAX-10 would
        // exceed i64 at MR0, but high MR can yield a representable final value.
        var a=values(12,0,Long.MAX_VALUE,0,0);var defender=values(12,0,0,200,0);
        var out=ModernDisintegrate.resolve(a,defender,Long.MAX_VALUE,b->b==15?0:99,0);
        long expected=BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(216)).multiply(BigInteger.valueOf(45)).divide(BigInteger.valueOf(100)).longValueExact();
        assertEquals(expected,out.rawDamage());assertTrue(out.rawDamage()>Integer.MAX_VALUE);
        assertEquals(ModernDisintegrate.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernDisintegrate.CombatException.class,
                ()->ModernDisintegrate.resolve(a,d,100,b->0,0)).failure());
        assertEquals(0,ModernDisintegrate.resolve(values(Long.MAX_VALUE,Long.MAX_VALUE,0,0,0),values(12,0,0,Long.MAX_VALUE,0),100,b->0,2).rawDamage());
        assertEquals(236,ModernDisintegrate.resolve(values(12,0,0,0,0),values(12,0,0,0,-20),1000,b->b==15?0:99,0).rawDamage());
        assertEquals(0,ModernDisintegrate.resolve(values(12,0,Long.MIN_VALUE,0,0),values(12,0,0,0,0),100,b->0,0).rawDamage());
    }
    @Test void invalidStateAndEveryRandomPositionFailBeforeReturningAnyOutcome() {
        var a=values(12,0,0,0,0);var d=values(12,0,0,0,20);
        for(int position=0;position<11;position++)for(boolean high:new boolean[]{false,true}) {
            int rejected=position;int[] call={0};
            var e=assertThrows(ModernDisintegrate.CombatException.class,()->ModernDisintegrate.resolve(a,d,100,b->call[0]++==rejected?(high?b:-1):0,0));
            assertEquals(ModernDisintegrate.Failure.INVALID_RANDOM,e.failure());assertEquals(position+1,call[0]);
        }
        for(long hp:new long[]{0,-1})assertEquals(ModernDisintegrate.Failure.INVALID_STATE,assertThrows(ModernDisintegrate.CombatException.class,
                ()->ModernDisintegrate.resolve(a,d,hp,b->{fail("no RNG on bad state");return 0;},0)).failure());
        assertThrows(ModernDisintegrate.CombatException.class,()->ModernDisintegrate.resolve(a,d,1,b->0,1));
        var unsupported=new ModernMagicCombat.Values(false,100,a.offense(),a.defense());
        assertEquals(ModernDisintegrate.Failure.UNSUPPORTED,assertThrows(ModernDisintegrate.CombatException.class,
                ()->ModernDisintegrate.resolve(unsupported,d,1,b->0,0)).failure());
    }
    @Test void mpUsesNineThresholdsOriginalClassReductionAndI64IntWithoutGrantingPermission() {
        var nativeSpell=BattleSpellCatalog.attackSpell(77);
        for(int clazz=0;clazz<=6;clazz++)for(long intelligence:new long[]{1,12,13,14,15,16,17,18,19,20,21,127,254,1L<<32}) {
            var row=new ModernApplicationProtocol.CharacterRow(3333,"synthetic",clazz,0,80);
            var source=ModernMagicCombatTest.stats(ModernBattleEngineHonorTest.source(7,row,100,300),intelligence,0,0,0,300);
            var fighter=ModernPhysicalCombatTest.fighter(source);var profile=ModernDisintegrate.qualify(fighter);
            long original=ModernMagicCombat.originalMpReduction(clazz,fighter.profile().source().persisted().originalIntelligence());
            assertEquals(nativeSpell.effectiveMpCost((int)Math.min(254,intelligence),(int)original),profile.mpCost());
            assertEquals(fighter.key(),profile.key());
            assertTrue(new ModernDisintegrateCatalog.Snapshot(fighter.key(),ModernDisintegrateCatalog.Status.QUALIFIED).accepts(profile));
            assertFalse(new ModernDisintegrateCatalog.Snapshot(new TournamentPresence.CharacterKey(8,3333),ModernDisintegrateCatalog.Status.QUALIFIED).accepts(profile));
            assertFalse(new ModernDisintegrateCatalog.Snapshot(fighter.key(),ModernDisintegrateCatalog.Status.UNLEARNED).accepts(profile));
        }
    }
    @Test void lawfulGateAndSelectedPlusOneDeltaAreSeparateAndNeverWordClamped() {
        for(long lawful:new long[]{Long.MIN_VALUE,-1,0,499}){assertFalse(ModernDisintegrate.lawfulAllows(lawful));assertThrows(ModernDisintegrate.CombatException.class,()->ModernDisintegrate.lawfulAfterCast(lawful));}
        for(long lawful:new long[]{500,32767,1L<<40,Long.MAX_VALUE-1}){assertTrue(ModernDisintegrate.lawfulAllows(lawful));assertEquals(lawful+1,ModernDisintegrate.lawfulAfterCast(lawful));}
        assertEquals(ModernDisintegrate.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernDisintegrate.CombatException.class,()->ModernDisintegrate.lawfulAfterCast(Long.MAX_VALUE)).failure());
    }
}
