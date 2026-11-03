package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Synthetic source qualification only; no grant for the missing shared 11101 SPR. */
class ModernArmorProtectionLoadoutTest {
    static ModernPhysicalCombat.Source withRules(ModernPhysicalCombat.Source s,Set<Long> weaponProcs,
            Map<Long,List<ModernArmorProcs.Rule>> rules) {
        var ids=new HashSet<>(weaponProcs==null?Set.<Long>of():weaponProcs);ids.addAll(rules.keySet());
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),ids,s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),
                new ModernArmorProcs.Catalog(s.growth().sourceId(),s.growth().database(),rules),weaponProcs);
    }
    static ModernArmorProtectionCatalog.BoundSource bound(ModernPhysicalCombat.Source source) {
        try {
            var skill=ModernArmorProtectionCatalogTest.read(new ModernDisintegrateCatalogTest.Jdbc(List.of(ModernArmorProtectionCatalogTest.row())),
                    source.growth().sourceId(),source.growth().database(),true);
            return new ModernArmorProtectionCatalog.BoundSource(source,skill);
        }catch(Exception e){throw new AssertionError(e);}
    }
    static ModernPhysicalCombat.Fighter qualify(ModernPhysicalCombat.Source source) {
        return ModernPhysicalCombat.qualifyProtection(bound(source),ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
    }
    static ModernArmorProtectionCatalog.BoundSource boundInScope(ModernPhysicalCombat.Source source)throws Exception {
        return new ModernArmorProtectionCatalog.BoundSource(source,ModernArmorProtectionCatalogTest.readInScope(
                new ModernDisintegrateCatalogTest.Jdbc(List.of(ModernArmorProtectionCatalogTest.row())),
                source.growth().sourceId(),source.growth().database(),true));
    }
    static ModernPhysicalCombat.Source protection(ModernPhysicalCombat.Source s) {
        var rules=new HashMap<Long,List<ModernArmorProcs.Rule>>();
        for(var item:s.input().equipment().source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR)
            rules.put(item.template().catalogId(),List.of(ModernArmorProcsTest.rule(item.template().catalogId(),item.template().catalogId(),-1,88,0,100)));
        return withRules(s,Set.of(),rules);
    }
    @Test void certifiedArmorDoesNotOpenTheLegacyPhysicalOrRegenRoute() {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);
        var source=protection(ModernBattleEngineHonorTest.source(7,row,1000,1000,0));
        var old=ModernPhysicalCombatTest.fighter(source);
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,old.unsupportedReason());
        assertEquals(ModernNaturalRegen.UnsupportedReason.EQUIPMENT_PROC,old.regen().unsupportedReason());
        var next=qualify(source);
        assertTrue(next.supported());assertTrue(next.magic().supported());assertTrue(next.regen().supported());
        assertNotNull(next.armorProtection());
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyEquipmentChange(source,
                ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000,next,100,50));
        assertThrows(IllegalArgumentException.class,()->ModernNaturalRegen.qualify(source,old.profile(),source.regenGrowth(),next.armorProtection()));
    }
    @Test void unknownWeaponProvenanceOrSameCatalogWeaponProcCannotBeHiddenByArmorRules() {
        var seed=ModernArmorProtectionCatalogTest.source(7);var rules=seed.armorProcs().byItem();
        for(Set<Long> proof:Arrays.asList(null,Set.of(88889L))) {
            var s=withRules(seed,proof,rules);assertThrows(IllegalArgumentException.class,()->qualify(s));
        }
        assertTrue(qualify(withRules(seed,Set.of(),rules)).supported());
    }
    @Test void unsupportedSiblingAndMissingSkillAreRejectedRatherThanDropped() {
        var seed=ModernArmorProtectionCatalogTest.source(7);var good=seed.armorProcs().byItem().get(88889L).get(0);
        var bad=new ModernArmorProcs.Rule(51,88889,-1,89,0,10,1,0,0,0,0,0,0,68,3,1,11101,1);
        assertThrows(IllegalArgumentException.class,()->qualify(withRules(seed,Set.of(),Map.of(88889L,List.of(good,bad)))));
        try {
            var s=withRules(seed,Set.of(),Map.of(88889L,List.of(good)));
            var missing=ModernArmorProtectionCatalogTest.read(new ModernDisintegrateCatalogTest.Jdbc(List.of()));
            var b=new ModernArmorProtectionCatalog.BoundSource(s,missing);
            assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyProtection(b,
                    ModernCombatProfileTest.binding(s.input().equipment()),Optional.empty(),1000));
        }catch(Exception e){throw new AssertionError(e);}
    }
    @Test void differentSourcesWithSameCharacterIdNeverShareSelections() {
        var seed=ModernArmorProtectionCatalogTest.source(7);
        var fighter=qualify(withRules(seed,Set.of(),seed.armorProcs().byItem()));
        var actor=new ModernArmorProcProtection.Actor(UUID.randomUUID(),fighter.key(),1,0);
        assertEquals(1,fighter.armorProtection().select(actor).rules().size());
        assertThrows(IllegalArgumentException.class,()->fighter.armorProtection().select(new ModernArmorProcProtection.Actor(
                actor.match(),new TournamentPresence.CharacterKey(42,fighter.key().objectId()),1,0)));
    }
}
