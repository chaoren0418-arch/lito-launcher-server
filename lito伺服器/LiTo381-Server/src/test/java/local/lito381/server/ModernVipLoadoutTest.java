package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernVipLoadoutTest {
    @Test void all31FieldsAreCompleteAndOnlyExplicitCurrentShapeIsAppliedOnce() {
        var source=source(true,true);var load=qualify(source,5642);
        assertEquals(31,ModernVipEquipment.Field.values().length);
        for(var field:ModernVipEquipment.Field.values()) {
            long vip=(field.ordinal()+1)*3L;
            long poly=field.polyColumn==null?0:(field.ordinal()+1)*20L;
            assertEquals(vip,load.vipAbilities().get(field),field.name());
            assertEquals(poly,load.polymorphAbilities().get(field),field.name());
            assertEquals(vip+poly,load.total().get(field),field.name());
        }
        assertEquals(List.of(1L,2L),load.items().stream().map(ModernVipLoadout.Applied::objectId).toList());
        assertEquals(Set.of(505L),load.icons());assertEquals(5642,load.shape().orElseThrow().graphicsId());
        assertTrue(load.shape().orElseThrow().noDeathExpLoss());assertTrue(load.shape().orElseThrow().retainsShapeOnCancellation());
        assertEquals(3,load.legacyAlreadyEvaluated().get(Stat.STR));
        assertEquals(0,load.legacyAlreadyEvaluated().get(Stat.CHA));
        assertEquals(0,load.legacyAlreadyEvaluated().get(Stat.AC));
        assertEquals(0,load.legacyAlreadyEvaluated().get(Stat.MAX_HP));
        assertEquals(0,load.legacyAlreadyEvaluated().get(Stat.REDUCTION));
    }

    @Test void explicitNoShapeDoesNotInferOneFromEquippedItemsAndInactiveShapeIsRejected() {
        var source=source(true,false);var plain=qualify(source,0);
        assertTrue(plain.shape().isEmpty());assertEquals(plain.vipAbilities(),plain.total());
        for(long shape:new long[]{-1,5642,9999})assertThrows(IllegalArgumentException.class,()->qualify(source,shape));
        var shaped=qualify(source,5641);assertEquals(11,shaped.total().get(STR));
    }

    @Test void sameExclusiveTypeIncludingZeroRejectsBeforePublishingCandidate() {
        for(long type:new long[]{0,1,Long.MAX_VALUE}) {
            var source=source(true,true);var facts=source.vipCatalog();
            var vips=new HashMap<Long,ModernVipEquipment.Vip>();
            for(var vip:facts.vips().values())vips.put(vip.catalogId(),new ModernVipEquipment.Vip(vip.catalogId(),type,vip.iconId(),vip.polymorphId(),vip.abilities()));
            var changed=withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",vips,facts.polymorphs()));
            assertThrows(IllegalArgumentException.class,()->qualify(changed,0));
        }
    }

    @Test void carriedVipCatalogIsRequiredAndSourceBoundEvenBeforeEquip() {
        var source=source(false,false);var facts=source.vipCatalog();
        assertTrue(qualify(source,0).items().isEmpty());
        var incomplete=new ModernVipEquipment.Catalog(7,"catalog",Map.of(300086L,facts.vips().get(300086L)),Map.of(5641L,facts.polymorphs().get(5641L)));
        assertThrows(IllegalArgumentException.class,()->withCatalog(source,incomplete));
        assertThrows(IllegalArgumentException.class,()->qualify(withCatalog(source,null),0));
        assertThrows(IllegalArgumentException.class,()->withCatalog(source,new ModernVipEquipment.Catalog(42,"catalog",facts.vips(),facts.polymorphs())));
        assertThrows(IllegalArgumentException.class,()->withCatalog(source,new ModernVipEquipment.Catalog(7,"other",facts.vips(),facts.polymorphs())));
    }

    @Test void ownerRevisionAndExactInstanceOverrideAreReconciledWithoutCommitting() {
        var source=source(false,false);var q=source.input().equipment();
        var stored=ModernCombatProfileTest.stored(q,8,Map.of(2L,true));
        var load=ModernVipLoadout.qualify(source,ModernCombatProfileTest.binding(q),stored,1000,5642);
        assertEquals(8,load.equipmentRevision());assertEquals(1000,load.evaluatedAtMillis());
        assertEquals(new TournamentPresence.CharacterKey(7,q.source().character().objectId()),load.owner());
        assertEquals("catalog",load.database());assertEquals(-1,load.validUntilMillis());
        assertEquals(List.of(2L),load.items().stream().map(ModernVipLoadout.Applied::objectId).toList());
        assertTrue(q.source().inventory().stream().noneMatch(ModernCharacterData.InventoryItem::equipped));
        var wrong=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(42,q.source().character().objectId()),ModernCombatProfileTest.binding(q).fingerprint());
        assertThrows(IllegalArgumentException.class,()->ModernVipLoadout.qualify(source,wrong,stored,1000,5642));
    }

    @Test void overlappingLegacyAndFullQueriesCannotMixConflictingGenerations() {
        var source=source(true,false);var facts=source.vipCatalog();var original=facts.vips().get(300086L);
        var values=new EnumMap<ModernVipEquipment.Field,Long>(original.abilities().values());values.put(STR,900L);
        var vips=new HashMap<>(facts.vips());vips.put(300086L,new ModernVipEquipment.Vip(300086,1,505,5641,new ModernVipEquipment.Abilities(values)));
        var changed=withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",vips,facts.polymorphs()));
        assertThrows(IllegalArgumentException.class,()->qualify(changed,5641));
    }

    @Test void fullContributionDoesNotRewriteHistoricalSavedAcOrEnableCombat() {
        var source=source(true,false);long saved=source.input().equipment().source().persisted().armorClass();
        var load=qualify(source,5641);assertTrue(load.total().get(AC)>0);
        var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
        assertEquals(saved,fighter.profile().effective().armorClass());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.CUSTOM_EXECUTOR,fighter.unsupportedReason());
        assertFalse(fighter.supported());
    }

    @Test void expiredAndUnknownLifetimesDoNotBecomeAnActiveVipOrShape() {
        var expired=ModernVipEquipmentCombatTest.source("expired",0,0,true);
        assertTrue(qualify(expired,0).items().isEmpty());
        assertThrows(IllegalArgumentException.class,()->qualify(expired,5641));
        assertThrows(IllegalArgumentException.class,()->qualify(ModernVipEquipmentCombatTest.source("unknown",0,0,true),0));
        var active=source(true,false);var q=active.input().equipment();
        var facts=new HashMap<>(q.lifetimes().items());var item=q.source().inventory().get(0);
        facts.put(item.objectId(),new ModernItemLifetime.Fact(item.objectId(),item.template().catalogId(),item.template().kind(),true,
                java.util.OptionalLong.of(0),true,java.util.OptionalLong.of(1),Optional.of("2026-10-01 00:00:01"),0,false));
        var life=new ModernItemLifetime.Observation(7,q.source().character().objectId(),"2026-10-01 00:00:00",1000,0,facts);
        var timed=withEquipment(active,new Qualified(q.source(),q.items(),q.catalog(),life));
        assertEquals(2000,qualify(timed,5641).validUntilMillis());
        assertTrue(ModernVipLoadout.qualify(timed,ModernCombatProfileTest.binding(q),Optional.empty(),2000,0).items().isEmpty());
        assertThrows(IllegalArgumentException.class,()->ModernVipLoadout.qualify(timed,ModernCombatProfileTest.binding(q),Optional.empty(),999,0));
    }

    @Test void absentPolyAbilityIsVerifiedAbsenceNotMissingCatalogOrMissingShapeResource() {
        var source=source(true,false);var facts=source.vipCatalog();var polys=new HashMap<>(facts.polymorphs());
        polys.put(5641L,ModernVipEquipment.Polymorph.absent(5641));
        var load=qualify(withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",facts.vips(),polys)),5641);
        assertEquals(5641,load.shape().orElseThrow().graphicsId());assertFalse(load.shape().orElseThrow().present());
        assertEquals(load.vipAbilities(),load.total());
    }

    @Test void immutableFullWidthFactsAndEveryAdditionRejectOverflowWithoutWrapping() {
        var source=source(true,false);var load=qualify(source,5641);
        assertThrows(UnsupportedOperationException.class,()->load.items().clear());
        assertThrows(UnsupportedOperationException.class,()->load.icons().clear());
        assertThrows(UnsupportedOperationException.class,()->load.total().values().clear());
        assertFalse(load.toString().contains("catalog"));assertFalse(load.items().get(0).toString().contains("300086"));
        for(var field:ModernVipEquipment.Field.values()) {
            var a=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Abilities.zero().values());
            var b=new EnumMap<ModernVipEquipment.Field,Long>(a);
            a.put(field,Long.MAX_VALUE);b.put(field,1L);
            assertThrows(ArithmeticException.class,()->new ModernVipEquipment.Abilities(a).plus(new ModernVipEquipment.Abilities(b)));
            a.put(field,Long.MIN_VALUE);b.put(field,-1L);
            assertThrows(ArithmeticException.class,()->new ModernVipEquipment.Abilities(a).plus(new ModernVipEquipment.Abilities(b)));
            a.put(field,4_294_967_299L);b.put(field,7L);
            assertEquals(4_294_967_306L,new ModernVipEquipment.Abilities(a).plus(new ModernVipEquipment.Abilities(b)).get(field));
        }
        var facts=source.vipCatalog();var vip=facts.vips().get(300086L);var polys=new HashMap<>(facts.polymorphs());
        var overflow=new EnumMap<ModernVipEquipment.Field,Long>(facts.polymorphs().get(5641L).abilities().values());overflow.put(ADENA_RATE,Long.MAX_VALUE);
        polys.put(5641L,new ModernVipEquipment.Polymorph(5641,true,false,false,new ModernVipEquipment.Abilities(overflow)));
        var bad=withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",facts.vips(),polys));
        assertThrows(ArithmeticException.class,()->qualify(bad,5641));
        assertEquals(vip.abilities(),source.vipCatalog().vips().get(300086L).abilities());
    }

    static ModernVipLoadout qualify(ModernPhysicalCombat.Source source,long shape) {
        return ModernVipLoadout.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000,shape);
    }
    static ModernPhysicalCombat.Source source(boolean first,boolean second) {
        var items=new ArrayList<Instance>();var vips=new HashMap<Long,ModernVipEquipment.Vip>();var polys=new HashMap<Long,ModernVipEquipment.Polymorph>();
        var policies=new HashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
        for(int i=1;i<=2;i++) {
            long id=300085L+i,shape=5640L+i;
            items.add(ModernEquipmentEvaluatorTest.instance(i,id,ModernCharacterData.ItemKind.ETC,i==1?first:second,0,"$26",List.of(),Ability.zero(),Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero()));
            var direct=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);var poly=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);
            for(var f:ModernVipEquipment.Field.values()) {direct.put(f,(f.ordinal()+1L)*i);poly.put(f,f.polyColumn==null?0:(f.ordinal()+1L)*i*10);}
            vips.put(id,new ModernVipEquipment.Vip(id,i,505,shape,new ModernVipEquipment.Abilities(direct)));
            polys.put(shape,new ModernVipEquipment.Polymorph(shape,true,true,true,new ModernVipEquipment.Abilities(poly)));
            policies.put(id,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ETC,"add.VIP",null));
        }
        var base=ModernPhysicalCombatTest.source(7,0,30,items);var q=base.input().equipment();var rules=new HashMap<>(q.catalog().lookups());
        for(var vip:vips.values()) {
            var raw=Ability.zero();
            for(var f:ModernVipEquipment.Field.values()) {
                String name=f==HPR?"HP_REGEN":f==MPR?"MP_REGEN":f.name();
                if(Set.of(CHA,AC,MAX_HP,MAX_MP,DAMAGE_REDUCTION,MAGIC_REDUCTION,EXP_RATE,ADENA_RATE).contains(f))continue;
                raw=raw.with(Stat.valueOf(name),vip.abilities().get(f));
            }
            rules.put(new RuleKey(RuleKind.VIP,ModernCharacterData.ItemKind.ETC,vip.catalogId(),0),RuleValue.present(raw));
        }
        var c=new Catalog(7,"catalog",rules,q.catalog().verifiedSetIds(),q.catalog().sets(),q.catalog().combos(),VipTableState.VERIFIED_PRESENT);
        q=new Qualified(q.source(),q.items(),c,q.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of()),base.growth(),policies,Set.of(),null,null,null,null,null,
                new ModernVipEquipment.Catalog(7,"catalog",vips,polys));
    }
    static ModernPhysicalCombat.Source withCatalog(ModernPhysicalCombat.Source s,ModernVipEquipment.Catalog c) {
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),c);
    }
    static ModernPhysicalCombat.Source withEquipment(ModernPhysicalCombat.Source s,Qualified q) {
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of()),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog());
    }
}
