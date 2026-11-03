package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernNpcDisintegrateTest {
    static ModernPhysicalCombat.Fighter caster() {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",3,0,80);
        return ModernPhysicalCombatTest.fighter(ModernDisintegrateTestPeer.lawfulSource(
                ModernMagicCombatTest.stats(ModernBattleEngineHonorTest.source(7,row,100,300),12,0,0,0,300)));
    }
    private static ModernDisintegrate.Profile values(long intelligence,long sp,long damage) {
        return new ModernDisintegrate.Profile(new TournamentPresence.CharacterKey(7,3333),
                ModernDisintegrateTest.values(intelligence,sp,damage,0,0),70);
    }
    static ModernDisintegrate.Outcome hit(ModernDisintegrate.Profile caster,Map<ModernNpcCatalog.Field,Long> fields,
                                        ModernPhysicalCombat.BoundedRandom random,int timedSp) {
        var c=ModernNpcMeleeTest.catalog(fields,true,0);
        return ModernNpcDisintegrate.againstNpc(caster,c,ModernNpcMeleeTest.actor(c),random,timedSp);
    }
    private static long multiplier(long mr,int hit) {
        long[] maxima={0,50,100,120,140,160,180,200,220,240,Long.MAX_VALUE};
        int[] divisors={1,2,3,4,5,6,7,8,9,10,11},percent={100,100,90,90,80,80,70,70,60,60,50};
        int i=0;while(mr>maxima[i])i++;
        return Math.max(0,percent[i]-Math.floorDiv(mr-hit,divisors[i]));
    }
    static void failure(ModernDisintegrate.Failure why,org.junit.jupiter.api.function.Executable action) {
        assertEquals(why,assertThrows(ModernDisintegrate.CombatException.class,action).failure());
    }
    @Test void npcMrUsesNeutralSkill77DiceThenPostMrCriticalWithNoPlayerArmorDraw() {
        for(long mr:new long[]{0,1,50,51,100,101,120,121,140,141,160,161,180,181,200,201,220,221,240,241,550,Long.MAX_VALUE})
            for(int die:new int[]{0,7,14})for(int critical:new int[]{0,39,40,99}) {
                var bounds=new ArrayList<Long>();
                var out=hit(values(12,0,0),Map.of(HP,10000L,MR,mr),b->{
                    bounds.add(b);return bounds.size()<=8?die:bounds.size()==9?critical:0;
                },0);
                long elemental=(190+8*(die+1))*35/32;
                long expected=elemental*multiplier(mr,0)/100*(critical<40?2:1);
                assertEquals(expected,out.rawDamage());assertEquals(10000-expected,out.remainingHp());
                assertEquals(critical<40,out.critical());assertEquals(critical<40?11748:11746,out.effect());
                assertEquals(List.of(15L,15L,15L,15L,15L,15L,15L,15L,100L,100L),bounds);
            }
        assertEquals(314,hit(values(12,0,0),Map.of(MR,51L,HP,1000L),b->0,0).rawDamage(),"special critical after MR, not before rounding");
    }
    @Test void physicalNpcFactsAndWeaknessDoNotLeakIntoNeutralMagic() {
        for(long weak:new long[]{-8,-4,-2,-1,0,1,2,4,8}) {
            var out=hit(values(12,0,0),Map.of(HP,1000L,MR,0L,AC,Long.MIN_VALUE,REDUCTION,Long.MAX_VALUE,
                    WEAK_ATTRIBUTE,weak,HARD,1L,UNDEAD,5L,LEVEL,Long.MAX_VALUE,INT,Long.MAX_VALUE),b->b==15?0:99,0);
            assertEquals(216,out.rawDamage());
            assertEquals(0,ClassicWorldMonsterMagicDamage.npcElementalResistance((int)weak,BattleSpellCatalog.ATTRIBUTE_NONE,0));
        }
    }
    @Test void fullWidthHpAndFinalDamageStayExactAndOverkillOnlyCapsAppliedDamage() {
        long hp=9_007_199_254_740_993L;
        var result=hit(values(12,0,0),Map.of(HP,hp,MR,0L),b->b==15?0:99,0);
        assertEquals(hp-216,result.remainingHp());
        var huge=hit(values(12,0,Long.MAX_VALUE-216),Map.of(HP,Long.MAX_VALUE,MR,0L),b->b==15?0:99,0);
        assertEquals(Long.MAX_VALUE,huge.rawDamage());assertEquals(Long.MAX_VALUE,huge.appliedDamage());assertTrue(huge.death());
        var small=hit(values(12,0,Long.MAX_VALUE-216),Map.of(HP,1L,MR,0L),b->b==15?0:99,0);
        assertEquals(1,small.appliedDamage());assertEquals(Long.MAX_VALUE,small.rawDamage());assertTrue(small.death());
        failure(ModernDisintegrate.Failure.ARITHMETIC_OVERFLOW,()->hit(values(12,0,Long.MAX_VALUE),Map.of(MR,0L),b->0,0));
        assertEquals(0,hit(values(Long.MAX_VALUE,Long.MAX_VALUE,0),Map.of(MR,Long.MAX_VALUE),b->0,2).rawDamage());
    }
    @Test void originalHitDamageEquipmentAndTimedSpellPowerStayAttackerSide() {
        var offense=new ModernMagicCombat.Offense(10,-1,1,99,4,3,20);
        var a=new ModernDisintegrate.Profile(new TournamentPresence.CharacterKey(42,3333),
                new ModernMagicCombat.Values(true,100,offense,new ModernMagicCombat.Defense(0,0,0,0,0,0,0)),70);
        long elemental=198*35/32,preMr=elemental+4+20;
        assertEquals(preMr*multiplier(51,3)/100*2,hit(a,Map.of(HP,1000L,MR,51L),b->0,2).rawDamage());
        assertEquals(70,a.mpCost());
    }
    @Test void invalidCatalogTargetBehaviorAndRandomNeverProduceAnOutcomeOrMutateNpc() {
        var a=values(12,0,0);
        for(var facts:List.of(Map.of(BOSS,1L),Map.of(POISON,1L),Map.of(TRANSFORM_ID,1L)))
            failure(ModernDisintegrate.Failure.UNSUPPORTED,()->hit(a,facts,b->{fail("unqualified target used RNG");return 0;},0));
        failure(ModernDisintegrate.Failure.INVALID_STATE,()->hit(a,Map.of(MR,-1L),b->{fail("negative MR cannot default to zero");return 0;},0));
        var c=ModernNpcMeleeTest.catalog();var npc=ModernNpcMeleeTest.actor(c);
        for(int position=0;position<10;position++)for(boolean high:new boolean[]{false,true}) {
            int where=position;int[] calls={0};
            failure(ModernDisintegrate.Failure.INVALID_RANDOM,()->ModernNpcDisintegrate.againstNpc(a,c,npc,b->calls[0]++==where?(high?b:-1):0,0));
            assertEquals(position+1,calls[0]);assertEquals(280,npc.hp());
        }
        failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.againstNpc(a,c,npc,b->{fail("invalid timed SP before RNG");return 0;},1));
    }
    @Test void targetCatalogSourceAndTemplateCannotAliasEqualNpcNumbers() {
        var c=ModernNpcMeleeTest.catalog();var npc=ModernNpcMeleeTest.actor(c);var row=c.npcs().get(ModernNpcMeleeTest.NPC);
        var other=new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(89,"other_npc",row.key().npcId()),row.name(),row.nameId(),row.implementation(),row.large(),row.facts(),false);
        var wrong=new ModernNpcCatalog.Catalog(89,"other_npc",Map.of(row.key().npcId(),other),new ModernPhysicalCombat.Growth(89,"other_npc",c.growth().strength(),c.growth().dexterity()));
        failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.againstNpc(values(12,0,0),wrong,npc,b->0,0));
        var changed=ModernNpcMeleeTest.catalog(Map.of(HP,281L),true,0);
        failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.againstNpc(values(12,0,0),changed,npc,b->0,0));
    }
}
