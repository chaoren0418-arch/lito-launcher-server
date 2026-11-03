package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static local.lito381.server.ModernCombatProfileTest.stored;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernHasteTest {
    @Test void exactNativeContractAndAllNativeCostsKeep43DistinctFrom54AndPotion() {
        var nativeSpell = BattleSpellCatalog.hasteSpell(43);
        assertEquals(nativeSpell.skillId(), ModernHaste.SKILL_ID);
        assertEquals(nativeSpell.actionId(), ModernHaste.ACTION);
        assertEquals(nativeSpell.effectGfx(), ModernHaste.EFFECT);
        assertEquals(nativeSpell.mpCost(), ModernHaste.BASE_MP);
        assertEquals(nativeSpell.reuseDelayMillis(), ModernHaste.REUSE_MILLIS);
        assertEquals(nativeSpell.durationSeconds(), ModernHaste.DURATION_SECONDS);
        assertEquals(300, ModernTimedEffects.Kind.HASTE.durationSeconds);
        assertEquals(2400, ModernGreaterHaste.DURATION_SECONDS);
        for (int intelligence=1; intelligence<=254; intelligence++)
            for (int reduction=0; reduction<=2; reduction++) for (boolean helm:new boolean[]{false,true})
                assertEquals(nativeSpell.effectiveMpCost(intelligence,reduction,helm),
                        ModernHaste.effectiveMpCost(intelligence,reduction,helm));
        assertNotEquals(ModernHaste.RULESET,ModernGreaterHaste.RULESET);
    }
    @Test void wideIntAndOddHalvingNeverNarrowOrUseSkill54Thresholds() {
        for (long intelligence:new long[]{17,18,254,255,65536,1L<<32,Long.MAX_VALUE}) {
            assertEquals(35,ModernHaste.effectiveMpCost(intelligence,0,false));
            assertEquals(33,ModernHaste.effectiveMpCost(intelligence,2,false));
            assertEquals(17,ModernHaste.effectiveMpCost(intelligence,0,true));
            assertEquals(15,ModernHaste.effectiveMpCost(intelligence,2,true));
        }
        assertEquals(18,ModernHaste.effectiveMpCost(12,2,true));
        assertEquals(17,ModernHaste.effectiveMpCost(13,2,true)); // 39/2 - 2, not (39-2)/2
        for (long invalid:new long[]{Long.MIN_VALUE,-1,0})
            assertThrows(IllegalArgumentException.class,()->ModernHaste.effectiveMpCost(invalid,0,false));
        for (long invalid:new long[]{Long.MIN_VALUE,-1,3,Long.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->ModernHaste.effectiveMpCost(12,invalid,true));
    }
    static ModernPhysicalCombat.Source source(int id,int clazz,long intelligence,long armor) {
        var item=simpleArmor(1,armor,ModernEquipmentFacts.Ability.zero());
        var base=ModernPhysicalCombatTest.source(id,clazz,80,List.of(item));
        return ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(base,intelligence,17,0,15,100));
    }
    @Test void exactTwoHelmetsUseLiveEquipmentAndFullSourceIdentityWithoutMutatingVitals() {
        for (int clazz=0;clazz<=6;clazz++) for (long intelligence:new long[]{1,12,13,16,17,18,1L<<40})
            for (long armor:new long[]{20008,20013,20014,20023,20024}) {
                var fighter=ModernPhysicalCombatTest.fighter(source(7,clazz,intelligence,armor));
                var p=ModernHaste.qualify(fighter);
                assertEquals(fighter.key(),p.key());assertEquals(0,p.equipmentRevision());
                assertEquals(ModernHaste.effectiveMpCost(intelligence,0,armor==20008||armor==20013),p.mpCost());
                assertEquals(armor==20008||armor==20013,LegacyEquipmentMagicLoader.halvesHasteCost(armor));
            }
        for (long armor:new long[]{20008,20013}) {
            var source=source(7,1,13,armor);var before=ModernPhysicalCombatTest.fighter(source);
            var original=ModernHaste.qualify(before);var q=source.input().equipment();
            var changed=ModernPhysicalCombat.qualifyEquipmentChange(source,binding(q),stored(q,1,Map.of(1L,false)),1,before,23,7);
            var next=ModernHaste.qualify(changed);
            assertTrue(q.source().inventory().get(0).equipped());
            assertEquals(19,original.mpCost());assertEquals(39,next.mpCost());assertEquals(1,next.equipmentRevision());
            assertEquals(23,changed.profile().effective().initialHp());assertEquals(7,changed.profile().effective().initialMp());
            assertEquals(original,ModernHaste.qualify(before));
            assertNotEquals(original.key(),ModernHaste.qualify(ModernPhysicalCombatTest.fighter(source(42,1,13,armor))).key());
            assertFalse(original.toString().contains("3333"));
        }
    }
    @Test void missingMagicAndMalformedProfilesCannotGrantTheUnimplementedSkill() {
        var source=ModernPhysicalCombatTest.source(7,1,80,List.of());
        assertThrows(IllegalArgumentException.class,()->ModernHaste.qualify(ModernPhysicalCombatTest.fighter(source)));
        var key=new TournamentPresence.CharacterKey(7,3333);
        assertThrows(IllegalArgumentException.class,()->new ModernHaste.Profile(key,-1,40));
        for(int cost:new int[]{-1,0,14,41,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->new ModernHaste.Profile(key,0,cost));
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,
                new ModernSkills.Skill(43,"$1463",40,10,ModernSkills.Support.UNIMPLEMENTED).support());
    }
}
