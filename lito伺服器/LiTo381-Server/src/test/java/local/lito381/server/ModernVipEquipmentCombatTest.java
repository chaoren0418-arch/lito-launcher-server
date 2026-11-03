package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernVipEquipmentCombatTest {
    @Test void expiredKnownVipKeepsSavedArmorAndBaseStatsWithoutApplyingItsPolymorph() {
        var source=source("expired",0,0,true);
        var before=source.input().equipment().source();var fighter=qualify(source);
        assertTrue(fighter.supported());assertTrue(fighter.magic().supported());
        assertTrue(fighter.regen().supported());assertTrue(fighter.ranged().defenseSupported());
        assertEquals(before.persisted().armorClass(),fighter.profile().effective().armorClass());
        assertEquals(before.persisted().strength(),fighter.profile().effective().strength());
        assertEquals(before.persisted().maxHp(),fighter.profile().effective().maximumHp());
        assertFalse(fighter.profile().equipped().get(0).equipped());
        assertSame(before,source.input().equipment().source());assertTrue(before.inventory().get(0).equipped());
        // The actual calculator can use the qualified input; this is not a DB attack.
        var target=ModernPhysicalCombatTest.createFighter(42,new ModernApplicationProtocol.CharacterRow(3333,"other",0,0,30),100,0,0);
        var result=ModernPhysicalCombat.resolve(fighter,target,100,bound->0);
        assertTrue(result.remainingHp()>=0);
    }

    @Test void activeVipIsNotMistakenForImplementedAttributesVisualsAndTransitions() {
        var fighter=qualify(source("active",0,0,true));
        assertCustom(fighter);
        assertTrue(fighter.profile().equipped().get(0).equipped());
    }

    @Test void independentlyNonzeroVipOrPolyAcRemainsUnsupportedEvenWhenTheyCancel() {
        for(long[] pair:new long[][]{{1,0},{0,-1},{1,-1},{Long.MAX_VALUE,Long.MIN_VALUE}})
            assertCustom(qualify(source("expired",pair[0],pair[1],true)));
    }

    @Test void nameAloneMissingFactsOrUnknownLifetimeNeverGrantsSupport() {
        assertCustom(qualify(source("expired",0,0,false)));
        var unknown=qualify(source("unknown",0,0,true));
        assertEquals(ModernPhysicalCombat.UnsupportedReason.LIFETIME_UNQUALIFIED,unknown.unsupportedReason());
        assertEquals(ModernMagicCombat.UnsupportedReason.LIFETIME_UNQUALIFIED,unknown.magic().unsupportedReason());
        assertFalse(unknown.regen().supported());assertFalse(unknown.ranged().defenseSupported());
    }

    @Test void procGuardRemainsIndependentOfKnownInactiveVip() {
        var base=source("expired",0,0,true);
        var changed=new ModernPhysicalCombat.Source(base.input(),base.growth(),base.templates(),Set.of(300086L),
                base.magicGrowth(),base.magicBows(),base.weaponArrowSprites(),base.regenGrowth(),base.healingGrowth(),base.vipCatalog());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,qualify(changed).unsupportedReason());
    }

    @Test void vipFactsCannotCrossSourceDatabaseOrCatalogIdentity() {
        var base=source("expired",0,0,true);var facts=base.vipCatalog();
        for(var wrong:List.of(new ModernVipEquipment.Catalog(42,"catalog",facts.vips(),facts.polymorphs()),
                new ModernVipEquipment.Catalog(7,"other",facts.vips(),facts.polymorphs()),
                new ModernVipEquipment.Catalog(7,"catalog",Map.of(),Map.of()))) {
            assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(base.input(),base.growth(),base.templates(),Set.of(),
                    base.magicGrowth(),base.magicBows(),base.weaponArrowSprites(),base.regenGrowth(),null,wrong));
        }
    }

    private static void assertCustom(ModernPhysicalCombat.Fighter fighter) {
        assertEquals(ModernPhysicalCombat.UnsupportedReason.CUSTOM_EXECUTOR,fighter.unsupportedReason());
        assertFalse(fighter.magic().supported());assertFalse(fighter.regen().supported());assertFalse(fighter.ranged().defenseSupported());
    }
    private static ModernPhysicalCombat.Fighter qualify(ModernPhysicalCombat.Source source) {
        return ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
    }
    static ModernPhysicalCombat.Source source(String life,long vipAc,long polyAc,boolean includeFacts) {
        var item=ModernEquipmentEvaluatorTest.instance(1,300086,ModernCharacterData.ItemKind.ETC,true,0,"vip-private",List.of(),
                Ability.zero(),Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        var base=ModernPhysicalCombatTest.source(7,0,30,List.of(item));var q=base.input().equipment();
        var ruleMap=new HashMap<>(q.catalog().lookups());
        ruleMap.put(new RuleKey(RuleKind.VIP,ModernCharacterData.ItemKind.ETC,300086,0),RuleValue.present(Ability.zero().with(Stat.STR,30)));
        var catalog=new Catalog(7,"catalog",ruleMap,q.catalog().verifiedSetIds(),q.catalog().sets(),q.catalog().combos(),VipTableState.VERIFIED_PRESENT);
        var lifetime=new ModernItemLifetime.Observation(7,q.source().character().objectId(),"2026-09-30 00:00:00",1000,0,
                Map.of(1L,new ModernItemLifetime.Fact(1,300086,ModernCharacterData.ItemKind.ETC,true,
                        life.equals("unknown")?OptionalLong.empty():OptionalLong.of(0),true,OptionalLong.of(720),
                        life.equals("unknown")?Optional.empty():Optional.of(life.equals("active")?"2026-10-01 00:00:00":"2026-09-29 00:00:00"),0,false)));
        q=new Qualified(q.source(),q.items(),catalog,lifetime);
        var policies=Map.of(300086L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ETC,"add.VIP",null));
        var vipValues=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);
        vipValues.putAll(ModernVipEquipment.Abilities.zero().values());vipValues.put(ModernVipEquipment.Field.AC,vipAc);
        var polyValues=new EnumMap<ModernVipEquipment.Field,Long>(vipValues);polyValues.put(ModernVipEquipment.Field.AC,polyAc);
        polyValues.put(ModernVipEquipment.Field.STR,4_294_967_299L);
        var facts=new ModernVipEquipment.Catalog(7,"catalog",
                Map.of(300086L,new ModernVipEquipment.Vip(300086,0,505,5641,new ModernVipEquipment.Abilities(vipValues))),
                Map.of(5641L,new ModernVipEquipment.Polymorph(5641,true,false,false,new ModernVipEquipment.Abilities(polyValues))));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of()),base.growth(),policies,Set.of(),
                ModernMagicCombatTest.growth(7),Map.of(),Map.of(),ModernNaturalRegenTest.growth(7,"catalog",5),null,includeFacts?facts:null);
    }
}
