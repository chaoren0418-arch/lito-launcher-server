package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ModernEquipmentEvaluatorTest {
    @Test void everyTracedAbilityLayerHasExactOrderingSignsAndSeparatePhysicalProvenance() {
        var armor = instance(1, 1000, ModernCharacterData.ItemKind.ARMOR, true, 7, "$187", List.of(1L),
                bonus(STR,5, AC,-3, MAX_HP,10, SP,1, FIRE,2), bonus(STR,7, AC,-6, MAX_HP,20, REDUCTION,555),
                List.of(100L,100L,0L,0L,0L), new Stone(200,3,OptionalLong.of(2000)),
                new TemplatePhysical(2,3,4,5,4,10));
        var etc = instance(2, 3000, ModernCharacterData.ItemKind.ETC, true, 0, "$3000", List.of(), Ability.zero(),
                bonus(STR,999, MAX_HP,999), List.of(101L,0L,0L,0L,0L), new Stone(201,1,OptionalLong.empty()), TemplatePhysical.zero());
        var rules = defaults(List.of(armor,etc), true);
        put(rules, armor, RuleKind.GREATER_TYPE, 2,0, bonus(STR,2,AC,1,FIRE,3,WEIGHT_REDUCTION,50));
        put(rules, armor, RuleKind.BLESS_TYPE, 2,1, bonus(STR,3,AC,2,FIRE,100));
        put(rules, armor, RuleKind.BLESS_ITEM, 1000,1, bonus(STR,4,AC,3,FIRE,7));
        put(rules, armor, RuleKind.ENCHANT_ITEM, 1000,7, bonus(STR,5,AC,4,FIRE,100));
        put(rules, armor, RuleKind.ENCHANT_TYPE, 2,1, bonus(STR,6,AC,5,FIRE,100));
        rules.put(key(RuleKind.CARD,100,0), RuleValue.present(bonus(STR,8,AC,2,FIRE,11,WEIGHT_REDUCTION,3)));
        rules.put(key(RuleKind.STONE,200,3), RuleValue.present(bonus(STR,9,MAX_HP,30,SP,2,AC,999)));
        rules.put(key(RuleKind.VIP,3000,0), RuleValue.present(bonus(STR,30,SP,4,MAX_HP,999)));
        var qualified = qualified(7, 2, List.of(armor,etc), rules, true,
                List.of(new SetRule(1,1,bonus(STR,10,AC,4,MAX_HP,40,SP,999,REDUCTION,2,WEIGHT_REDUCTION,20)),
                        new SetRule(1,2,bonus(STR,999))),
                List.of(new ComboRule(1,Set.of(100L,101L),bonus(STR,20,AC,3,MAX_HP,50,SP,999,REDUCTION,3,WEIGHT_REDUCTION,30))));
        var evaluated = ModernEquipmentEvaluator.evaluate(qualified, Map.of(), 1000);
        assertEquals(117, evaluated.total(STR));
        assertEquals(-36, evaluated.armorClassModifier());
        assertEquals(150, evaluated.total(MAX_HP));
        assertEquals(7, evaluated.total(SP));
        assertEquals(5, evaluated.total(MR)); // precise elf/$187 retained exception
        assertEquals(34, evaluated.total(FIRE));
        assertEquals(66, evaluated.total(WEIGHT_REDUCTION));
        assertEquals(9, evaluated.magicReductionBase()); // never generic item reduction 555
        assertEquals(555, evaluated.direct().get(REDUCTION));
        assertEquals(new TemplatePhysical(2,3,4,5,4,10), evaluated.armorTemplatePhysical());
        assertEquals(2, evaluated.equippedRows()); assertEquals(1, evaluated.armorRows());
        assertEquals(1, evaluated.vipRows()); assertEquals(2, evaluated.cardRows());
        assertEquals(1, evaluated.activeSets()); assertEquals(1, evaluated.activeCombos());
        assertEquals(OptionalLong.of(2000), evaluated.nextStoneExpiry());
        var removed = ModernEquipmentEvaluator.evaluate(qualified, Map.of(1L,false), 1000);
        assertEquals(30, removed.total(STR)); assertEquals(0, removed.activeSets()); assertEquals(0, removed.activeCombos());
        assertEquals(0, removed.total(MAX_HP)); // etc dark/stone and VIP MAX_HP are unread, not applied
        assertEquals(OptionalLong.empty(), removed.nextStoneExpiry());
    }

    @Test void signedI64ValuesSurviveAndDoNotDependOnTheNativeGlobalSchema() {
        long high = 4_294_967_299L;
        var armor = simpleArmor(Long.MAX_VALUE, high, bonus(MAX_HP, high, STR,high, AC,-high, FIRE,high));
        var qualified = qualified(List.of(armor));
        String old = System.getProperty("lito.numeric.breakthrough");
        try {
            System.setProperty("lito.numeric.breakthrough", "0");
            var nativeDisabled = ModernEquipmentEvaluator.evaluate(qualified, Map.of(), 0);
            System.setProperty("lito.numeric.breakthrough", "4");
            assertEquals(nativeDisabled, ModernEquipmentEvaluator.evaluate(qualified, Map.of(), 0));
            assertEquals(high, nativeDisabled.total(MAX_HP)); assertEquals(high, nativeDisabled.total(STR));
            assertEquals(-high, nativeDisabled.armorClassModifier()); assertEquals(high, nativeDisabled.total(FIRE));
        } finally { if (old == null) System.clearProperty("lito.numeric.breakthrough"); else System.setProperty("lito.numeric.breakthrough",old); }
    }

    @Test void effectiveEquipmentDoesNotRewriteSavedAcOrTreatOriginalStatsAsBase() {
        var item = simpleArmor(1, 1, bonus(STR,3,MAX_HP,20,AC,-4));
        var qualified = qualified(List.of(item));
        var saved = qualified.source().persisted();
        var evaluation = ModernEquipmentEvaluator.evaluate(qualified, Map.of(), 0);
        assertEquals(3, evaluation.total(STR)); assertEquals(-4, evaluation.armorClassModifier());
        assertEquals(20, evaluation.total(MAX_HP));
        assertEquals(1000, saved.savedHp()); assertEquals(100, saved.maxHp());
        assertEquals(-900, saved.armorClass()); assertEquals(200, saved.strength()); assertEquals(11, saved.originalStrength());
        assertEquals(saved, qualified.source().persisted());
    }

    @Test void itemAndAggregateOverflowAndNegationFailRatherThanClampOrWrap() {
        var base = simpleArmor(1, 1, bonus(STR,Long.MAX_VALUE));
        var rules = defaults(List.of(base), false);
        put(rules,base,RuleKind.GREATER_TYPE,2,0,bonus(STR,1));
        var overflow = qualified(7,0,List.of(base),rules,false,List.of(),List.of());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(overflow,Map.of(),0));
        var second = simpleArmor(2, 2, bonus(STR,1));
        var sum = qualified(List.of(base,second));
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(sum,Map.of(),0));
        var negate = defaults(List.of(base),false);
        put(negate,base,RuleKind.BLESS_ITEM,1,1,bonus(AC,Long.MIN_VALUE));
        var badAc = qualified(7,0,List.of(base),negate,false,List.of(),List.of());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(badAc,Map.of(),0));
    }

    @Test void derivedTotalWeightAcAndMagicReductionOverflowFailBeforeEvaluationIsReturned() {
        var stat = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(5L),
                bonus(STR,Long.MAX_VALUE),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var statQualified = qualified(7,0,List.of(stat),defaults(List.of(stat),false),false,
                List.of(new SetRule(5,1,bonus(STR,1))),List.of());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(statQualified,Map.of(),0));
        var weight = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(),
                bonus(WEIGHT_REDUCTION,Long.MAX_VALUE),Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(0,0,0,0,0,1));
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(qualified(List.of(weight)),Map.of(),0));
        var ac = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(5L),
                bonus(AC,Long.MIN_VALUE),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var acQualified = qualified(7,0,List.of(ac),defaults(List.of(ac),false),false,
                List.of(new SetRule(5,1,bonus(AC,1))),List.of());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(acQualified,Map.of(),0));
        var magic = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(5L),
                Ability.zero(),Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(0,0,0,0,Long.MAX_VALUE,0));
        var magicQualified = qualified(7,0,List.of(magic),defaults(List.of(magic),false),false,
                List.of(new SetRule(5,1,bonus(REDUCTION,1))),List.of());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(magicQualified,Map.of(),0));
    }

    @Test void negativeArmorEnchantIsAddedWithoutByteWrapping() {
        var armor = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,-999,"$1",List.of(),bonus(AC,-5),
                Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        assertEquals(994, ModernEquipmentEvaluator.evaluate(qualified(List.of(armor)),Map.of(),0).armorClassModifier());
        var minimum = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,Long.MIN_VALUE,"$1",List.of(),Ability.zero(),
                Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        assertThrows(ArithmeticException.class, () -> ModernEquipmentEvaluator.evaluate(qualified(List.of(minimum)),Map.of(),0));
    }

    @Test void weaponExtensionsDoNotInventAcAndWeaponItemLayerIsNotDoubleCounted() {
        var weapon = instance(Long.MAX_VALUE,41,ModernCharacterData.ItemKind.WEAPON,true,0,"$16",List.of(),
                bonus(MELEE_HIT,8,MELEE_DAMAGE,9,STR,3),Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(1,2,0,0,0,0));
        var armor = simpleArmor(2,2,bonus(MELEE_HIT,4,MELEE_DAMAGE,5));
        var rules = defaults(List.of(weapon,armor),false);
        put(rules,weapon,RuleKind.GREATER_TYPE,1,0,bonus(MELEE_HIT,6,AC,999));
        var evaluation = ModernEquipmentEvaluator.evaluate(qualified(7,0,List.of(weapon,armor),rules,false,List.of(),List.of()),Map.of(),0);
        assertEquals(18,evaluation.direct().get(MELEE_HIT)); assertEquals(14,evaluation.weaponItem().get(MELEE_HIT));
        assertEquals(4,evaluation.nonWeaponItem(MELEE_HIT)); assertEquals(0,evaluation.armorClassModifier());
        assertEquals(new TemplatePhysical(1,2,0,0,0,0),evaluation.weaponTemplatePhysical());
        assertEquals(Long.MAX_VALUE,evaluation.weaponObjectId());
    }

    @Test void highestEligibleSetThresholdAndEveryQualifiedComboAreAppliedOnce() {
        var one = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(5L),Ability.zero(),Ability.zero(),
                List.of(10L,10L,0L,0L,0L),Stone.none(),TemplatePhysical.zero());
        var two = instance(2,2,ModernCharacterData.ItemKind.ARMOR,true,0,"$2",List.of(5L),Ability.zero(),Ability.zero(),
                List.of(11L,0L,0L,0L,0L),Stone.none(),TemplatePhysical.zero());
        var q = qualified(7,0,List.of(one,two),defaults(List.of(one,two),false),false,
                List.of(new SetRule(5,1,bonus(STR,1)),new SetRule(5,2,bonus(STR,20)),new SetRule(5,3,bonus(STR,300))),
                List.of(new ComboRule(1,Set.of(10L),bonus(STR,2)),new ComboRule(2,Set.of(10L,11L),bonus(STR,3))));
        var e = ModernEquipmentEvaluator.evaluate(q,Map.of(),0);
        assertEquals(25,e.total(STR)); assertEquals(1,e.activeSets()); assertEquals(2,e.activeCombos());
        assertEquals(3,ModernEquipmentEvaluator.evaluate(q,Map.of(2L,false),0).total(STR));
    }

    @Test void repeatedSetTokensRetainSourceCountingRatherThanInventingDeduplication() {
        var one = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(5L,5L),Ability.zero(),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var q = qualified(7,0,List.of(one),defaults(List.of(one),false),false,List.of(new SetRule(5,2,bonus(STR,20))),List.of());
        assertEquals(20,ModernEquipmentEvaluator.evaluate(q,Map.of(),0).total(STR));
    }

    @Test void stoneExpiryIsExclusiveAndIndefiniteStoneIsExplicit() {
        var timed = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(),Ability.zero(),Ability.zero(),emptyCards(),new Stone(3,0,OptionalLong.of(100)),TemplatePhysical.zero());
        var rules = defaults(List.of(timed),false); rules.put(key(RuleKind.STONE,3,0),RuleValue.present(bonus(MAX_MP,55)));
        var q = qualified(7,0,List.of(timed),rules,false,List.of(),List.of());
        assertEquals(55,ModernEquipmentEvaluator.evaluate(q,Map.of(),99).total(MAX_MP));
        assertEquals(0,ModernEquipmentEvaluator.evaluate(q,Map.of(),100).total(MAX_MP));
        assertEquals(OptionalLong.empty(),ModernEquipmentEvaluator.evaluate(q,Map.of(),100).nextStoneExpiry());
        var indefinite = new Instance(timed.source(),timed.template(),0,timed.dark(),timed.cards(),new Stone(3,0,OptionalLong.empty()));
        assertEquals(55,ModernEquipmentEvaluator.evaluate(qualified(7,0,List.of(indefinite),rules,false,List.of(),List.of()),Map.of(),Long.MAX_VALUE).total(MAX_MP));
    }

    @Test void lookupAbsenceIsQualifiedButUnqueriedIncludingUnequippedCandidatesIsRejected() {
        var armor = simpleArmor(1,1,Ability.zero());
        var empty = new Catalog(7,"catalog",Map.of(),Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT);
        var armorSource = snapshot(7,0,List.of(armor));
        assertThrows(IllegalArgumentException.class, () -> new Qualified(armorSource,Map.of(1L,armor),empty,
                ModernItemLifetime.Observation.verifiedPermanent(armorSource)));
        assertEquals(Ability.zero(),ModernEquipmentEvaluator.evaluate(qualified(List.of(armor)),Map.of(),0).direct());
        var source = armor.source();
        var unwear = new ModernCharacterData.InventoryItem(source.objectId(),source.template(),source.quantity(),false,source.enchantment(),
                source.durability(),source.identified(),source.blessing(),source.sealed(),source.chargeCount());
        var unworn = new Instance(unwear,armor.template(),0,armor.dark(),armor.cards(),armor.stone());
        var unwornSource = snapshot(7,0,List.of(unworn));
        assertThrows(IllegalArgumentException.class, () -> new Qualified(unwornSource,Map.of(1L,unworn),empty,
                ModernItemLifetime.Observation.verifiedPermanent(unwornSource)));
    }

    @Test void sourceItemTemplateAndSetQualificationAreExactNotPrimaryFallback() {
        var item = simpleArmor(1,1,Ability.zero()); var q = qualified(List.of(item));
        var wrongSource = snapshot(42,0,List.of(item));
        assertThrows(IllegalArgumentException.class, () -> new Qualified(wrongSource,Map.of(1L,item),q.catalog(),
                ModernItemLifetime.Observation.verifiedPermanent(wrongSource)));
        assertThrows(IllegalArgumentException.class, () -> new Qualified(q.source(),Map.of(2L,item),q.catalog(),q.lifetimes()));
        var inSet = instance(1,1,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(999L),Ability.zero(),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var inSetSource = snapshot(7,0,List.of(inSet));
        assertThrows(IllegalArgumentException.class, () -> new Qualified(inSetSource,Map.of(1L,inSet),q.catalog(),
                ModernItemLifetime.Observation.verifiedPermanent(inSetSource)));
        assertThrows(IllegalArgumentException.class, () -> ModernEquipmentEvaluator.evaluate(q,Map.of(99L,true),0));
        assertThrows(IllegalArgumentException.class, () -> ModernEquipmentEvaluator.evaluate(q,Map.of(),-1));
    }

    @Test void full512AndI64ObjectCatalogRuleKeysAreNotNarrowed() {
        var items = new ArrayList<Instance>();
        for (int i=0;i<512;i++) items.add(simpleArmor(0x1_0000_0000L+i,0x2_0000_0000L+i,bonus(MAX_HP,1)));
        var q=qualified(items); var e=ModernEquipmentEvaluator.evaluate(q,Map.of(),0);
        assertEquals(512,e.equippedRows()); assertEquals(512,e.total(MAX_HP));
        assertEquals(512,q.ruleLoadout().items().size());
        assertEquals(0x1_0000_01ffL,q.ruleLoadout().items().get(511).objectId());
        // This tests ability accumulation only; EquipmentRules would reject 512 equipped body slots.
    }

    @Test void immutableCompleteAbilityAndCatalogRejectMalformedOrAmbiguousFacts() {
        var values=new EnumMap<Stat,Long>(Stat.class); values.putAll(Ability.zero().values()); values.put(STR,99L);
        var ability=new Ability(values); values.put(STR,0L); assertEquals(99,ability.get(STR));
        assertThrows(UnsupportedOperationException.class,()->ability.values().clear());
        values.remove(DEX); assertThrows(IllegalArgumentException.class,()->new Ability(values));
        assertThrows(IllegalArgumentException.class,()->new RuleValue(false,bonus(STR,1)));
        assertThrows(IllegalArgumentException.class,()->new Catalog(7,"catalog",Map.of(),Set.of(1L),
                List.of(new SetRule(1,1,Ability.zero()),new SetRule(1,1,Ability.zero())),List.of(),VipTableState.VERIFIED_ABSENT));
        assertThrows(IllegalArgumentException.class,()->new Catalog(7,"catalog",Map.of(),Set.of(),List.of(),
                List.of(new ComboRule(1,Set.of(10L),Ability.zero()),new ComboRule(1,Set.of(11L),Ability.zero())),VipTableState.VERIFIED_ABSENT));
        assertThrows(IllegalArgumentException.class,()->new Catalog(7,"catalog",Map.of(key(RuleKind.VIP,1,0),RuleValue.absent()),Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT));
        assertThrows(IllegalArgumentException.class,()->new Stone(0,1,OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class,()->new RuleKey(RuleKind.CARD,ModernCharacterData.ItemKind.WEAPON,10,0));
    }

    @Test void unknownBlessingNeverBecomesEquipPermissionAndMultipleWeaponsAreRejected() {
        var original=simpleArmor(1,1,Ability.zero()); var src=original.source();
        var unknown=new ModernCharacterData.InventoryItem(1,src.template(),1,true,0,0,true,255,false,0);
        var instance=new Instance(unknown,original.template(),0,RuleValue.absent(),emptyCards(),Stone.none());
        assertEquals(EquipmentRules.CurseState.UNKNOWN,qualified(List.of(instance)).ruleLoadout().item(1).curse());
        var first=instance(1,1,ModernCharacterData.ItemKind.WEAPON,true,0,"$1",List.of(),Ability.zero(),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var second=instance(2,2,ModernCharacterData.ItemKind.WEAPON,true,0,"$2",List.of(),Ability.zero(),Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentEvaluator.evaluate(qualified(List.of(first,second)),Map.of(),0));
    }

    static Ability bonus(Object... pairs) {
        Ability result=Ability.zero();
        for(int i=0;i<pairs.length;i+=2) result=result.with((Stat)pairs[i],((Number)pairs[i+1]).longValue());
        return result;
    }
    static Instance simpleArmor(long id,long catalog,Ability base) {
        return instance(id,catalog,ModernCharacterData.ItemKind.ARMOR,true,0,"$"+catalog,List.of(),base,Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
    }
    static Instance instance(long id,long catalog,ModernCharacterData.ItemKind kind,boolean equipped,long enchant,String name,
                             List<Long> sets,Ability base,Ability dark,List<Long> cards,Stone stone,TemplatePhysical physical) {
        var metadata=new ModernCharacterData.ItemTemplate(catalog,kind,name,1,2,1000);
        var source=new ModernCharacterData.InventoryItem(id,metadata,1,equipped,enchant,0,true,1,false,0);
        var range=new EquipmentRules.LevelRange(1,Long.MAX_VALUE);
        EquipmentRules.Facts rules=switch(kind) {
            case WEAPON->new EquipmentRules.WeaponFacts(127,range,false);
            case ARMOR->new EquipmentRules.ArmorFacts(127,range,EquipmentRules.ArmorSlot.BODY);
            case ETC->EquipmentRules.NonEquipmentFacts.INSTANCE;
        };
        return new Instance(source,new Template(metadata,rules,kind==ModernCharacterData.ItemKind.ARMOR?2:kind==ModernCharacterData.ItemKind.WEAPON?1:0,
                kind==ModernCharacterData.ItemKind.ETC?0:6,sets,base,physical),0,RuleValue.present(dark),cards,stone);
    }
    static List<Long> emptyCards(){return List.of(0L,0L,0L,0L,0L);}
    static RuleKey key(RuleKind kind,long first,long second){return new RuleKey(kind,ModernCharacterData.ItemKind.ETC,first,second);}
    static void put(Map<RuleKey,RuleValue> rules,Instance item,RuleKind kind,long first,long second,Ability ability){
        rules.put(new RuleKey(kind,item.template().metadata().kind(),first,second),RuleValue.present(ability));
    }
    static Map<RuleKey,RuleValue> defaults(List<Instance> items,boolean vip){
        Map<RuleKey,RuleValue> result=new HashMap<>();
        for(RuleKey key:ModernEquipmentFacts.requiredLookups(items)) if(vip||key.kind()!=RuleKind.VIP) result.put(key,RuleValue.absent());
        return result;
    }
    static Qualified qualified(List<Instance> items){return qualified(7,0,items,defaults(items,false),false,List.of(),List.of());}
    static Qualified qualified(int sourceId,int type,List<Instance> items,Map<RuleKey,RuleValue> rules,boolean vip,List<SetRule> sets,List<ComboRule> combos){
        Map<Long,Instance> map=new HashMap<>(); for(var item:items)map.put(item.source().objectId(),item);
        var source=snapshot(sourceId,type,items);
        return new Qualified(source,map,new Catalog(sourceId,"catalog",rules,ModernEquipmentFacts.requiredSetIds(items),sets,combos,
                vip?VipTableState.VERIFIED_PRESENT:VipTableState.VERIFIED_ABSENT),
                ModernItemLifetime.Observation.verifiedPermanent(source));
    }
    static ModernCharacterData.Snapshot snapshot(int sourceId,int type,List<Instance> items){
        return new ModernCharacterData.Snapshot(sourceId,new ModernApplicationProtocol.CharacterRow(3333,"fixture",type,0,Long.MAX_VALUE),
                new ModernCharacterData.PersistedValues(100,0,1000,500,-900,200,201,202,203,204,205,0,0,11,12,13,14,15,225),
                items.stream().map(Instance::source).sorted(java.util.Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId)).toList());
    }
}
