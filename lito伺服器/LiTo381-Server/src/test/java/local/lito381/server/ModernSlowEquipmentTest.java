package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static local.lito381.server.ModernCombatProfileTest.stored;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernSlowEquipmentTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/datatables/ItemTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
    }

    @Test void retainedFlagAndEquippedCountDoNotUseBooleanOneOrNativeIntegerNarrowing() throws Exception {
        String itemTable=LocalResources.readString(Path.of("../1-伺服器/src/com/lineage/server/datatables/ItemTable.java"));
        assertTrue(itemTable.contains("weapon.setHasteItem(rs.getInt(\"haste_item\") == 0 ? false : true);"));
        assertTrue(itemTable.contains("armor.setHasteItem(rs.getInt(\"haste_item\") == 0 ? false : true);"));
        String use=LocalResources.readString(Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("getHasteItemEquipped() > 0"));
        for(long flag:new long[]{Long.MIN_VALUE,-1,0,1,2,1L<<32,Long.MAX_VALUE}) {
            var source=flags(ModernHasteTest.source(7,1,15,20023),Map.of(20023L,flag));
            var fighter=ModernPhysicalCombatTest.fighter(source);
            assertEquals(flag==0?0:1,fighter.hasteItemCount().orElseThrow());
            assertEquals(flag!=0,ModernSlow.equipmentImmune(fighter));
            assertEquals(17,ModernSlow.qualify(fighter).mpCost());
        }
    }

    @Test void liveUnequipReequipAndSourceReloadRequalifyWithoutRewritingOldFactsOrVitals() {
        var source=flags(ModernHasteTest.source(7,1,15,20023),Map.of(20023L,2L));
        var before=ModernPhysicalCombatTest.fighter(source);var q=source.input().equipment();
        var removed=ModernPhysicalCombat.qualifyEquipmentChange(source,binding(q),stored(q,1,Map.of(1L,false)),1,before,23,7);
        assertFalse(ModernSlow.equipmentImmune(removed));assertEquals(1,removed.profile().equipmentRevision());
        assertEquals(23,removed.profile().effective().initialHp());assertEquals(7,removed.profile().effective().initialMp());
        var returned=ModernPhysicalCombat.qualifyEquipmentChange(source,binding(q),stored(q,2,Map.of(1L,true)),2,removed,23,7);
        assertTrue(ModernSlow.equipmentImmune(returned));assertEquals(2,returned.profile().equipmentRevision());
        assertTrue(ModernSlow.equipmentImmune(before));assertEquals(0,before.profile().equipmentRevision());
        var other=ModernPhysicalCombatTest.fighter(flags(ModernHasteTest.source(42,1,15,20023),Map.of(20023L,0L)));
        assertEquals(before.key().objectId(),other.key().objectId());assertNotEquals(before.key(),other.key());
        assertFalse(ModernSlow.equipmentImmune(other));
        var reloaded=ModernPhysicalCombatTest.fighter(flags(source,Map.of(20023L,0L)));
        assertFalse(ModernSlow.equipmentImmune(reloaded));assertTrue(ModernSlow.equipmentImmune(before));
    }

    @Test void unknownEvenOnUnequippedGearIsNotFalseAndNoGearIsProvenEmpty() {
        var unknown=ModernHasteTest.source(7,1,15,20023);
        var before=ModernPhysicalCombatTest.fighter(unknown);assertTrue(before.hasteItemCount().isEmpty());
        assertThrows(IllegalArgumentException.class,()->ModernSlow.equipmentImmune(before));
        var q=unknown.input().equipment();
        var removed=ModernPhysicalCombat.qualifyEquipmentChange(unknown,binding(q),stored(q,1,Map.of(1L,false)),1,before,23,7);
        assertTrue(removed.hasteItemCount().isEmpty());
        assertThrows(IllegalArgumentException.class,()->ModernSlow.equipmentImmune(removed));
        var empty=ModernPhysicalCombatTest.fighter(ModernMagicCombatTest.enable(ModernPhysicalCombatTest.source(7,1,80,List.of())));
        assertEquals(0,empty.hasteItemCount().orElseThrow());assertFalse(ModernSlow.equipmentImmune(empty));
        var noMagic=ModernPhysicalCombatTest.fighter(ModernPhysicalCombatTest.source(7,1,80,List.of()));
        assertThrows(IllegalArgumentException.class,()->ModernSlow.equipmentImmune(noMagic));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.TemplatePolicy(
                ModernCharacterData.ItemKind.ETC,"0",null,OptionalLong.of(0)));
    }

    @Test void twoGearInstancesCountIndividuallyButExpiredGearCannotKeepImmunity() {
        var weapon=ModernPhysicalCombatTest.weapon(1,1,true,0,ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.TemplatePhysical.zero());
        var armor=ModernEquipmentEvaluatorTest.simpleArmor(2,2,ModernEquipmentFacts.Ability.zero());
        var source=flags(ModernMagicCombatTest.enable(ModernPhysicalCombatTest.source(7,1,80,List.of(weapon,armor))),Map.of(1L,1L,2L,2L));
        var before=ModernPhysicalCombatTest.fighter(source);
        assertEquals(2,before.hasteItemCount().orElseThrow());
        var q=source.input().equipment();var facts=new HashMap<>(q.lifetimes().items());
        facts.put(2L,new ModernItemLifetime.Fact(2,2,ModernCharacterData.ItemKind.ARMOR,true,OptionalLong.of(0),
                true,OptionalLong.of(1),Optional.of("1970-01-01 00:00:00.001000"),0,false));
        var lifetime=new ModernItemLifetime.Observation(7,q.source().character().objectId(),"1970-01-01 00:00:00.000000",0,0,facts);
        var nextQ=new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),lifetime);
        var next=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(nextQ,true,source.input().weapons()),
                source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth());
        var expired=ModernPhysicalCombat.qualify(next,binding(nextQ),Optional.empty(),1);
        assertFalse(expired.profile().equipped().stream().filter(i->i.objectId()==2).findFirst().orElseThrow().equipped());
        assertEquals(1,expired.hasteItemCount().orElseThrow());
        assertEquals(2,before.hasteItemCount().orElseThrow());
    }

    private static ModernPhysicalCombat.Source flags(ModernPhysicalCombat.Source source,Map<Long,Long> flags) {
        var policies=new HashMap<>(source.templates());
        flags.forEach((id,value)->{
            var prior=policies.get(id);
            policies.put(id,new ModernPhysicalCombat.TemplatePolicy(prior.kind(),prior.className(),prior.criticalMultiplier(),OptionalLong.of(value)));
        });
        return new ModernPhysicalCombat.Source(source.input(),source.growth(),policies,source.procCatalogIds(),
                source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth());
    }
}

