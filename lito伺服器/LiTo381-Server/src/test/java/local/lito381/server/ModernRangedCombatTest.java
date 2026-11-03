package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernRangedCombatTest {
    static ModernPhysicalCombat.Source source(int type,long weapon) {
        var old=ModernPhysicalCombatTest.fixtureSource(7,new ModernApplicationProtocol.CharacterRow(0x1_0000_0001L,"ranged",type,0,80),100,10,weapon);
        return new ModernPhysicalCombat.Source(old.input(),old.growth(),old.templates(),old.procCatalogIds(),null,
                weapon==0?Map.of():Map.of(1L,0L),weapon==0?Map.of():Map.of(1L,-1L));
    }
    static ModernRangedCombat.Fighter fighter(ModernPhysicalCombat.Source source){return ModernPhysicalCombatTest.fighter(source).ranged();}
    @Test void bothOrdinaryBowTypesHaveIndependentAttackAndDefenseQualification() {
        for(int type:new int[]{4,13}){var source=source(1,type);var melee=ModernPhysicalCombatTest.fighter(source);
            assertFalse(melee.supported());assertTrue(melee.ranged().supported());assertTrue(melee.ranged().defenseSupported());
            assertEquals(type,melee.ranged().values().weapon().retainedType());assertEquals(0x1_0000_0001L,melee.profile().source().character().objectId());}
        var unarmed=fighter(source(1,0));assertFalse(unarmed.supported());assertTrue(unarmed.defenseSupported());assertEquals(ModernRangedCombat.Unsupported.NOT_BOW,unarmed.unsupported());
        assertNull(ModernPhysicalCombatTest.createFighter(7,new ModernApplicationProtocol.CharacterRow(1,"old",1,0,80),100,10,4).ranged());
    }
    @Test void unsupportedAttackPropertiesDoNotInventMeleeOrInvalidateKnownDefenderFacts() {
        var good=source(1,4);
        var magic=new ModernPhysicalCombat.Source(good.input(),good.growth(),good.templates(),Set.of(),null,Map.of(1L,1L),Map.of(1L,-1L));
        var sprite=new ModernPhysicalCombat.Source(good.input(),good.growth(),good.templates(),Set.of(),null,Map.of(1L,0L),Map.of(1L,99L));
        assertEquals(ModernRangedCombat.Unsupported.MAGIC_BOW,fighter(magic).unsupported());assertTrue(fighter(magic).defenseSupported());
        assertEquals(ModernRangedCombat.Unsupported.PROJECTILE_SPRITE,fighter(sprite).unsupported());assertTrue(fighter(sprite).defenseSupported());
        assertEquals(ModernRangedCombat.Unsupported.APPEARANCE,fighter(source(5,4)).unsupported());
        var old=good.input().weapons().get(1L);
        for(long range:new long[]{-1,17}){
            var w=new ModernCombatProfile.WeaponFacts(old.objectId(),old.catalogId(),old.retainedType(),old.damageSmall(),old.damageLarge(),range,
                    old.material(),old.templateBlessing(),old.maximumDamageChance(),old.criticalChance(),old.criticalMultiplier(),old.attributeKind(),old.attributeLevel());
            var changed=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(good.input().equipment(),true,Map.of(1L,w)),good.growth(),good.templates(),Set.of(),null,good.magicBows(),good.weaponArrowSprites());
            assertEquals(ModernRangedCombat.Unsupported.RANGE,fighter(changed).unsupported());assertTrue(fighter(changed).defenseSupported());
        }
    }
    @Test void unknownCustomOrProcBaselineRejectsAttackAndDefenseRatherThanDroppingEffects() {
        var good=source(1,4);
        var custom=new ModernPhysicalCombat.Source(good.input(),good.growth(),Map.of(1L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.WEAPON,"custom.Executor",new ModernPhysicalCombat.Rational(0,1))),Set.of(),null,good.magicBows(),good.weaponArrowSprites());
        var proc=new ModernPhysicalCombat.Source(good.input(),good.growth(),good.templates(),Set.of(1L),null,good.magicBows(),good.weaponArrowSprites());
        assertEquals(ModernRangedCombat.Unsupported.CUSTOM_EXECUTOR,fighter(custom).unsupported());assertFalse(fighter(custom).defenseSupported());
        assertEquals(ModernRangedCombat.Unsupported.EQUIPMENT_PROC,fighter(proc).unsupported());assertFalse(fighter(proc).defenseSupported());
    }
    @Test void completeCatalogKeysAndSeparateExplicitSpriteEvidenceAreRequired() {
        var s=source(1,4);
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),null,Map.of(),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),null,Map.of(1L,0L),Map.of(2L,-1L)));
        var incomplete=new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),null,s.magicBows());
        assertEquals(ModernRangedCombat.Unsupported.UNQUALIFIED,fighter(incomplete).unsupported());
    }
    @Test void ordinaryBowUsesSourceDexGrowthNotOriginalOrWeaponDice() {
        var s=source(1,4);var growth=ModernPhysicalCombatTest.growth(7,7,9);
        s=new ModernPhysicalCombat.Source(s.input(),growth,s.templates(),Set.of(),null,s.magicBows(),s.weaponArrowSprites());
        var f=fighter(s);assertEquals(80+8+7,f.values().offense().totalHit());assertEquals(9,f.values().offense().damageBonus());
        var d=fighter(source(1,0));var out=ModernRangedCombat.resolve(f.values(),d.values(),ModernAmmoTest.facts(40744),100,bound->bound-1,0);
        assertTrue(out.hit());assertEquals(7+9,out.rawDamage());
        var w=s.input().weapons().get(1L);var huge=new ModernCombatProfile.WeaponFacts(1,1,4,Long.MAX_VALUE,Long.MAX_VALUE,w.range(),w.material(),w.templateBlessing(),0,0,0,0,0);
        var changed=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(s.input().equipment(),true,Map.of(1L,huge)),growth,s.templates(),Set.of(),null,s.magicBows(),s.weaponArrowSprites());
        assertEquals(out,ModernRangedCombat.resolve(fighter(changed).values(),d.values(),ModernAmmoTest.facts(40744),100,bound->bound-1,0));
    }
}
