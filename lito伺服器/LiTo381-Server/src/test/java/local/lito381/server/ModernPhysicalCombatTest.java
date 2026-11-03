package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernPhysicalCombatTest {
    @Test void ordinaryMeleeUsesSourceDieAndClassDamageNotFixedTen() {
        var attacker=fighter(source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero()))));
        var target=fighter(source(42,0,30,List.of()));
        var low=ModernPhysicalCombat.resolve(attacker,target,100,rolls(0,0,0));
        var high=ModernPhysicalCombat.resolve(attacker,target,100,rolls(19,0,7));
        assertTrue(low.hit());assertEquals(4,low.rawDamage());assertEquals(96,low.remainingHp());
        assertEquals(11,high.rawDamage());assertEquals(89,high.remainingHp());
        assertEquals(100,attacker.profile().effective().initialHp());assertEquals(100,target.profile().effective().initialHp());
    }

    @Test void deathIsDerivedFromAppliedDamageAndNeverAllowsNegativeHpOrOverkill() {
        var a=fighter(source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero()))));
        var b=fighter(source(42,0,30,List.of()));
        var hit=ModernPhysicalCombat.resolve(a,b,5,rolls(19,0,7));
        assertEquals(11,hit.rawDamage());assertEquals(5,hit.appliedDamage());assertEquals(0,hit.remainingHp());assertTrue(hit.death());
        for(long hp:new long[]{0,-1,101,Long.MAX_VALUE})assertFailure(ModernPhysicalCombat.Failure.INVALID_STATE,
                ()->ModernPhysicalCombat.resolve(a,b,hp,bound->0));
        assertFailure(ModernPhysicalCombat.Failure.INVALID_STATE,()->ModernPhysicalCombat.resolve(a,a,1,bound->0));
    }

    @Test void failedAttackDiceUsesFifteenPercentAndMissNeverConsumesWeaponDamageRng() {
        var a=fighter(source(7,0,30,List.of()));var b=fighter(source(42,0,30,List.of()));
        // d20=1 yields fumble only after an explicit -1? Retained fumble is
        // hitRate-9; d20+1+hitRate-10 is hitRate-8. A strong negative target
        // defense instead reaches the same retained 15% chance branch.
        var q=withVitals(b.profile().source().sourceId(),source(42,0,30,List.of()),100,-1000);
        b=fighter(q);var target=b;
        var miss=ModernPhysicalCombat.resolve(a,target,100,rolls(0,1499,99));
        assertFalse(miss.hit());assertEquals(0,miss.appliedDamage());assertEquals(100,miss.remainingHp());assertFalse(miss.death());
    }

    @Test void growthOriginalClassAndEquipmentLayersAreSeparateAndAddedOnce() {
        var item=weapon(1,1,true,6,bonus(MELEE_HIT,2,MELEE_DAMAGE,3),new TemplatePhysical(4,5,0,0,0,0));
        var s=source(7,0,30,List.of(item));
        s=new ModernPhysicalCombat.Source(s.input(),growth(7,7,9),s.templates(),s.procCatalogIds());
        var a=fighter(s);var o=a.offense();
        assertEquals(2,o.classHit());assertEquals(7,o.growthHit());assertEquals(0,o.originalHit());
        assertEquals(6,o.equipmentHit());assertEquals(3,o.enchantHit());assertEquals(48,o.totalHit());
        assertEquals(3,o.classDamage());assertEquals(9,o.growthDamage());assertEquals(3,o.itemDamage());
        assertEquals(5,o.templateDamage());assertEquals(6,o.enchantDamage());
        var hit=ModernPhysicalCombat.resolve(a,fighter(source(42,0,30,List.of())),100,rolls(19,0,0));
        assertEquals(27,hit.rawDamage());
    }

    @Test void negativeEnchantRetainsThreeFifthsTowardZeroAndDurabilityDamageLayer() {
        var a=fighter(source(7,0,30,List.of(weapon(1,1,true,-6,Ability.zero(),TemplatePhysical.zero()))));
        assertEquals(-3,a.offense().enchantHit());assertEquals(-6,a.offense().enchantDamage());
        var hit=ModernPhysicalCombat.resolve(a,fighter(source(42,0,30,List.of())),100,rolls(19,0,0));
        assertEquals(0,hit.rawDamage());assertEquals(100,hit.remainingHp());assertFalse(hit.death());
    }

    @Test void completeGrowthUsesSaturatedIndexOnlyAndNeverClampsEffectiveI64Strength() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,bonus(STR,0x1_0000_0000L),TemplatePhysical.zero())));
        var table=new ArrayList<ModernPhysicalCombat.GrowthRow>();
        for(int i=1;i<=127;i++)table.add(new ModernPhysicalCombat.GrowthRow(i,i,i));
        var g=new ModernPhysicalCombat.Growth(7,"catalog",table,table);
        var a=fighter(new ModernPhysicalCombat.Source(s.input(),g,s.templates(),Set.of()));
        assertEquals(0x1_0000_00c8L,a.profile().effective().strength());assertEquals(127,a.offense().growthHit());
        assertEquals(1,g.at(false,0).id());assertEquals(127,g.at(true,Long.MAX_VALUE).id());
        table.clear();assertEquals(127,g.strength().size());assertThrows(UnsupportedOperationException.class,()->g.strength().clear());
    }

    @Test void missingDuplicateUnorderedOrWrongSourceGrowthIsNotZeroFallback() {
        var rows=new ArrayList<>(growth(7,0,0).strength());rows.remove(126);
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Growth(7,"catalog",rows,rows));
        rows.add(new ModernPhysicalCombat.GrowthRow(126,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Growth(7,"catalog",rows,rows));
        var s=source(7,0,30,List.of());
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),growth(42,0,0),Map.of(),Set.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Growth(7,"catalog;DROP",List.of(),List.of()));
    }

    @Test void allSevenClassesUseRetainedDistinctLevelDivisorsAndOriginalThresholds() {
        long[] hit={8,12,4,3,4,4,3},damage={12,20,10,6,7,9,7},ac={10,15,10,6,7,10,7};
        for(int c=0;c<7;c++) {
            var a=fighter(withVitals(7,source(7,c,120,List.of()),100,-20));
            assertEquals(hit[c],a.offense().classHit());assertEquals(damage[c],a.offense().classDamage());assertEquals(ac[c],a.defense().acMaximum());
        }
        assertEquals(0,ModernPhysicalCombat.originalHit(1,16));assertEquals(2,ModernPhysicalCombat.originalHit(1,18));assertEquals(4,ModernPhysicalCombat.originalHit(1,19));
        assertEquals(0,ModernPhysicalCombat.originalDamage(5,14));assertEquals(1,ModernPhysicalCombat.originalDamage(5,17));assertEquals(3,ModernPhysicalCombat.originalDamage(5,18));
        assertEquals(3,ModernPhysicalCombat.originalHit(6,16));assertEquals(4,ModernPhysicalCombat.originalHit(6,17));
        assertEquals(2,ModernPhysicalCombat.originalDamage(0,0)); // Existing switch default, not guessed zero.
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.originalHit(7,10));
    }

    @Test void unarmedDamageDeliberatelyReplacesEarlierWeaponStatEquipmentSubtotal() {
        var armor=simpleArmor(1,1,bonus(MELEE_DAMAGE,999));
        var a=fighter(source(7,0,30,List.of(armor)));var b=fighter(source(42,0,30,List.of()));
        var low=ModernPhysicalCombat.resolve(a,b,100,rolls(19,0,0));
        var high=ModernPhysicalCombat.resolve(a,b,100,rolls(19,0,4));
        assertEquals(4,low.rawDamage());assertEquals(5,high.rawDamage());assertEquals(999,a.offense().itemDamage());
    }

    @Test void maximumDamageAndCriticalMultiplierApplyBeforeDefenseWithExactDecimalFloor() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        s=weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,8,12,1,11,1,100,100,1.5,0,0));
        var outcome=ModernPhysicalCombat.resolve(fighter(s),fighter(source(42,0,30,List.of())),100,rolls(19,0,0,0));
        assertEquals(16,outcome.rawDamage());assertTrue(outcome.maximumTriggered());assertTrue(outcome.criticalTriggered());
        assertEquals(7,ModernPhysicalCombat.Rational.decimal(new BigDecimal("1.5")).applyAtLeastOne(5));
        assertEquals(5,ModernPhysicalCombat.Rational.decimal(new BigDecimal("0.9")).applyAtLeastOne(5));
    }

    @Test void untriggeredMaximumChanceExcludesMaximumDieButZeroChanceIncludesIt() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        s=weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,8,12,1,11,1,10,0,0,0,0));
        var outcome=ModernPhysicalCombat.resolve(fighter(s),fighter(source(42,0,30,List.of())),100,rolls(19,0,99,6));
        assertEquals(10,outcome.rawDamage());assertFalse(outcome.maximumTriggered());
    }

    @Test void armorAcDirectionalAndQualifiedPvpReductionAreIndependentAndIgnoreOnlyArmor() {
        var a=fighter(source(7,0,30,List.of(weapon(1,1,true,0,bonus(MELEE_DAMAGE,100,IGNORE_REDUCTION,3),TemplatePhysical.zero()))));
        var armor=instance(2,2,ModernCharacterData.ItemKind.ARMOR,true,0,"$2",List.of(),bonus(MELEE_REDUCTION,4,PVP_REDUCTION,5,REDUCTION,999),
                Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(0,0,0,0,12,0));
        var b=fighter(withVitals(42,source(42,0,30,List.of(armor)),100,-20));
        var hit=ModernPhysicalCombat.resolve(a,b,100,rolls(19,0,0,7,0,0));
        assertEquals(111,hit.rawDamage()); // AC 1, armor (11-3)=8, near4, PvP5 -> 93.
        assertEquals(93,hit.appliedDamage());assertEquals(7,hit.remainingHp());assertEquals(12,b.defense().armorReduction());
        assertEquals(999,b.profile().equipmentEvaluation().total(REDUCTION)); // NOT silently armor or outgoing damage.
    }

    @Test void sourceSavedAcIsNotDoubleAppliedAndOverrideChangesOnlyDelta() {
        var armor=simpleArmor(2,2,bonus(AC,-7));var s=withVitals(7,source(7,0,30,List.of(armor)),100,-30);
        assertEquals(-30,fighter(s).defense().armorClass());
        var q=s.input().equipment();var changed=ModernPhysicalCombat.qualify(s,ModernCombatProfileTest.binding(q),
                ModernCombatProfileTest.stored(q,1,Map.of(2L,false)),0);
        assertEquals(-23,changed.defense().armorClass());
    }

    @Test void supportedMeleeTypesAndUnarmedAreExplicitNotAllWeaponTypes() {
        for(long type=1;type<=18;type++) {
            var s=withType(source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero()))),type);
            assertEquals(ModernPhysicalCombat.MELEE_TYPES.contains(type),fighter(s).supported(),"type "+type);
        }
        assertTrue(fighter(source(7,0,30,List.of())).supported());
    }

    @Test void attributesCustomExecutorsAndProcPresenceOnlyBlockCurrentlyEquippedItems() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero()),weapon(2,2,false,0,Ability.zero(),TemplatePhysical.zero())));
        var policies=new HashMap<>(s.templates());policies.put(2L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.WEAPON,"custom.Executor",new ModernPhysicalCombat.Rational(0,1)));
        var special=new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,Set.of(2L));
        assertTrue(fighter(special).supported());
        var q=s.input().equipment();var changed=ModernPhysicalCombat.qualify(special,ModernCombatProfileTest.binding(q),
                ModernCombatProfileTest.stored(q,1,Map.of(1L,false,2L,true)),0);
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,changed.unsupportedReason());
        assertFailure(ModernPhysicalCombat.Failure.UNSUPPORTED,()->ModernPhysicalCombat.resolve(fighter(s),changed,100,bound->0));
        var attributed=weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,8,8,1,11,1,0,0,0,1,1));
        assertEquals(ModernPhysicalCombat.UnsupportedReason.ATTRIBUTE_ENCHANT,fighter(attributed).unsupportedReason());
    }

    @Test void unqualifiedZeroDieInvalidMeleeRangeAndTrialWeaponAreNotPlayableFallbacks() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        var unqualified=new ModernPhysicalCombat.Source(ModernCombatProfile.Input.unqualified(s.input().equipment()),s.growth(),s.templates(),Set.of());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.WEAPON_UNQUALIFIED,fighter(unqualified).unsupportedReason());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.ZERO_WEAPON_DIE,fighter(weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,0,0,1,11,1,0,0,0,0,0))).unsupportedReason());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.INVALID_MELEE_RANGE,fighter(weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,8,8,-1,11,1,0,0,0,0,0))).unsupportedReason());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.INVALID_MELEE_RANGE,fighter(weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,8,8,17,11,1,0,0,0,0,0))).unsupportedReason());
        for(long id:new long[]{247,248,249})assertEquals(ModernPhysicalCombat.UnsupportedReason.SPECIAL_WEAPON,
                fighter(source(7,0,30,List.of(weapon(id,id,true,0,Ability.zero(),TemplatePhysical.zero())))).unsupportedReason());
    }

    @Test void invalidServerRngIsRejectedBeforeAnyHpMutation() {
        var a=fighter(source(7,0,30,List.of()));var b=fighter(source(42,0,30,List.of()));
        assertFailure(ModernPhysicalCombat.Failure.INVALID_RANDOM,()->ModernPhysicalCombat.resolve(a,b,100,bound->bound));
        assertFailure(ModernPhysicalCombat.Failure.INVALID_RANDOM,()->ModernPhysicalCombat.resolve(a,b,100,bound->-1));
        assertEquals(100,b.profile().effective().initialHp());
    }

    @Test void overflowIsFailClosedBothDuringQualificationAndBeforeOutcome() {
        assertFailure(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW,()->fighter(source(7,0,Long.MAX_VALUE,List.of())));
        assertFailure(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW,()->fighter(withVitals(7,source(7,0,30,List.of()),100,Long.MIN_VALUE)));
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        s=weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,1,Long.MAX_VALUE,1,1,11,1,100,0,0,0,0));
        var a=fighter(s);var b=fighter(source(42,0,30,List.of()));
        assertFailure(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW,()->ModernPhysicalCombat.resolve(a,b,100,rolls(19,0,0)));
    }

    @Test void full512InventoryAndLargeNumericIdsRemainIntactAndSourceCollisionIsNotSelfAttack() {
        var items=new ArrayList<Instance>();for(int i=0;i<512;i++)items.add(weapon(0x1_0000_0000L+i,0x2_0000_0000L+i,i==0,0,Ability.zero(),TemplatePhysical.zero()));
        var a=fighter(source(7,0,30,items));var b=fighter(source(42,0,30,List.of()));
        assertEquals(512,a.profile().equipped().size());assertEquals(0x1_0000_0000L,a.profile().weapon().objectId());
        assertEquals(a.key().objectId(),b.key().objectId());assertNotEquals(a.key(),b.key());
        assertTrue(ModernPhysicalCombat.resolve(a,b,100,rolls(19,0,7)).hit());
    }

    @Test void sourceCoverageAndExactMultiplierMustMatchTheOriginalQualifiedBundle() {
        var s=source(7,0,30,List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),Map.of(),Set.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(2L)));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),Map.of(1L,
                new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.WEAPON,"0",new ModernPhysicalCombat.Rational(2,1))),Set.of()));
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.Rational.decimal(new BigDecimal("1.0000001")));
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.Rational.decimal(new BigDecimal("-1")));
    }

    static Instance weapon(long object,long catalog,boolean equipped,long enchant,Ability bonus,TemplatePhysical physical) {
        return instance(object,catalog,ModernCharacterData.ItemKind.WEAPON,equipped,enchant,"$"+catalog,List.of(),bonus,
                Ability.zero(),emptyCards(),Stone.none(),physical);
    }
    static ModernPhysicalCombat.Source source(int id,int type,long level,List<Instance> items) {
        var old=qualified(id,type,items,defaults(items,false),false,List.of(),List.of());var row=old.source().character();
        var snapshot=new ModernCharacterData.Snapshot(id,new ModernApplicationProtocol.CharacterRow(row.objectId(),row.name(),type,row.sex(),level),
                old.source().persisted(),old.source().inventory());
        var q=ModernCombatProfileTest.withVitals(new Qualified(snapshot,old.items(),old.catalog(),old.lifetimes()),100,50,999,999,10);
        var facts=new HashMap<Long,ModernCombatProfile.WeaponFacts>();var policies=new HashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
        for(var item:items) {
            long object=item.source().objectId(),catalog=item.source().template().catalogId();var kind=item.source().template().kind();
            policies.put(catalog,new ModernPhysicalCombat.TemplatePolicy(kind,"0",kind==ModernCharacterData.ItemKind.WEAPON?new ModernPhysicalCombat.Rational(0,1):null));
            if(kind==ModernCharacterData.ItemKind.WEAPON)facts.put(object,new ModernCombatProfile.WeaponFacts(object,catalog,item.template().retainedType(),8,12,1,11,1,0,0,0,0,0));
        }
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,facts),growth(id,0,0),policies,Set.of());
    }
    static ModernPhysicalCombat.Growth growth(int source,long hit,long damage) {
        var rows=new ArrayList<ModernPhysicalCombat.GrowthRow>();for(int i=1;i<=127;i++)rows.add(new ModernPhysicalCombat.GrowthRow(i,hit,damage));
        return new ModernPhysicalCombat.Growth(source,"catalog",rows,rows);
    }
    static ModernPhysicalCombat.Source withVitals(int source,ModernPhysicalCombat.Source s,long maxHp,long ac) {
        if (source != s.input().equipment().source().sourceId()) throw new IllegalArgumentException("fixture source mismatch");
        var q=ModernCombatProfileTest.withVitals(s.input().equipment(),maxHp,50,999,999,ac);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,s.input().weapons()),s.growth(),s.templates(),s.procCatalogIds());
    }
    static ModernPhysicalCombat.Source weaponFacts(ModernPhysicalCombat.Source s,ModernCombatProfile.WeaponFacts fact) {
        var facts=new HashMap<>(s.input().weapons());facts.put(fact.objectId(),fact);
        var policies=new HashMap<>(s.templates());var prior=policies.get(fact.catalogId());
        policies.put(fact.catalogId(),new ModernPhysicalCombat.TemplatePolicy(prior.kind(),prior.className(),ModernPhysicalCombat.Rational.decimal(BigDecimal.valueOf(fact.criticalMultiplier()))));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(s.input().equipment(),true,facts),s.growth(),policies,s.procCatalogIds());
    }
    static ModernPhysicalCombat.Source withType(ModernPhysicalCombat.Source s,long type) {
        var q=s.input().equipment();var i=q.items().get(1L);var t=i.template();
        var changed=new Instance(i.source(),new Template(t.metadata(),t.rules(),type,t.safeEnchant(),t.setIds(),t.base(),t.physical()),i.greater(),i.dark(),i.cards(),i.stone());
        var catalog=q.catalog();
        q=new Qualified(q.source(),Map.of(1L,changed),new Catalog(catalog.sourceId(),catalog.resourceDatabase(),defaults(List.of(changed),false),
                requiredSetIds(List.of(changed)),List.of(),List.of(),catalog.vipTableState()),q.lifetimes());
        var fact=new ModernCombatProfile.WeaponFacts(1,1,type,8,12,1,11,1,0,0,0,0,0);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of(1L,fact)),s.growth(),s.templates(),Set.of());
    }
    static ModernPhysicalCombat.Fighter fighter(ModernPhysicalCombat.Source s) {
        return ModernPhysicalCombat.qualify(s,ModernCombatProfileTest.binding(s.input().equipment()),Optional.empty(),0);
    }
    /** Test-only trusted provider fixture; no production constructor bypass. Type 0 means truly unarmed. */
    static ModernPhysicalCombat.Fighter createFighter(int sourceId,ModernApplicationProtocol.CharacterRow row,long hp,long ac,long weaponType) {
        return fighter(fixtureSource(sourceId,row,hp,ac,weaponType));
    }
    static ModernPhysicalCombat.Source fixtureSource(int sourceId,ModernApplicationProtocol.CharacterRow row,long hp,long ac,long weaponType) {
        var s=source(sourceId,row.characterClass(),row.level(),weaponType==0?List.of():List.of(weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        if(weaponType!=0)s=withType(s,weaponType);
        s=withVitals(sourceId,s,hp,ac);var q=s.input().equipment();
        var snapshot=new ModernCharacterData.Snapshot(sourceId,row,q.source().persisted(),q.source().inventory());
        q=new Qualified(snapshot,q.items(),q.catalog(),
                row.objectId()==q.source().character().objectId()?q.lifetimes():ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,s.input().weapons()),s.growth(),s.templates(),s.procCatalogIds());
    }
    private static ModernPhysicalCombat.BoundedRandom rolls(long... values) {
        int[] at={0};return bound->{assertTrue(at[0]<values.length,"unexpected extra RNG draw");long v=values[at[0]++];assertTrue(v>=0&&v<bound,"fixture roll outside requested bound");return v;};
    }
    private static void assertFailure(ModernPhysicalCombat.Failure failure,org.junit.jupiter.api.function.Executable run) {
        assertEquals(failure,assertThrows(ModernPhysicalCombat.CombatException.class,run).failure());
    }
}
