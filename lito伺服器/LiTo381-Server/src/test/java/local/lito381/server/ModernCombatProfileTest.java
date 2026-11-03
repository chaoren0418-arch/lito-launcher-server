package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModernCombatProfileTest {
    static final ModernEquipmentOverrideStore.Fingerprint BINDING = new ModernEquipmentOverrideStore.Fingerprint(new byte[32]);

    @Test void baseMaximaAndBaseSixAreAugmentedExactlyOnceAndOriginalBirthStatsAreNotUsed() {
        var item=simpleArmor(1,1,bonus(MAX_HP,1000049,MAX_MP,123,STR,1,DEX,2,CON,3,INT,4,WIS,5,CHA,6,AC,-7));
        var profile=profile(qualified(List.of(item)),Optional.empty());
        assertEquals(1000149,profile.effective().maximumHp()); assertEquals(123,profile.effective().maximumMp());
        assertEquals(201,profile.effective().strength()); assertEquals(203,profile.effective().dexterity());
        assertEquals(205,profile.effective().constitution()); assertEquals(207,profile.effective().intelligence());
        assertEquals(209,profile.effective().wisdom()); assertEquals(211,profile.effective().charisma());
        assertEquals(-900,profile.effective().armorClass()); // Saved TOTAL AC, not saved + (-7).
        assertEquals(1000149,profile.effective().initialHp());assertFalse(profile.attackImplemented());
    }

    @Test void overrideAcIsOnlyCurrentMinusSourceBaselineAndSavedAcRemainsAnOpaqueLayer() {
        var first=simpleArmor(1,1,bonus(AC,-7,MAX_HP,50));
        var second=instance(2,2,ModernCharacterData.ItemKind.ARMOR,false,0,"$2",List.of(),bonus(AC,-11,MAX_HP,80),
                Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var q=qualified(List.of(first,second));
        var stored=stored(q,8,Map.of(1L,false,2L,true));
        var profile=profile(q,stored);
        assertEquals(-904,profile.effective().armorClass());assertEquals(180,profile.effective().maximumHp());
        assertEquals(8,profile.equipmentRevision());
        assertEquals(List.of(new ModernEquipmentAuthority.Equipped(1,false),new ModernEquipmentAuthority.Equipped(2,true)),profile.equipped());
    }

    @Test void retainedSavedVitalsNeverAddEquipmentTwiceOrHealToMax() {
        var q=withVitals(qualified(List.of(simpleArmor(1,1,bonus(MAX_HP,900,MAX_MP,700)))),100,50,333,123,-80);
        var result=ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q),binding(q),Optional.empty(),12,
                ModernCombatProfile.VitalPolicy.RETAINED_SAVED);
        assertEquals(1000,result.effective().maximumHp());assertEquals(750,result.effective().maximumMp());
        assertEquals(333,result.effective().initialHp());assertEquals(123,result.effective().initialMp());
        assertEquals(ModernCombatProfile.VitalPolicy.RETAINED_SAVED,result.vitalPolicy());
    }

    @Test void retainedSavedAboveEffectiveMaximumFailsRatherThanClipsWhileNewMatchIsExplicitlyFull() {
        var q=qualified(List.of()); // Saved HP 1000 > base/effective 100.
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q),binding(q),
                Optional.empty(),0,ModernCombatProfile.VitalPolicy.RETAINED_SAVED));
        assertEquals(100,profile(q,Optional.empty()).effective().initialHp());
        var mp=withVitals(q,100,2,50,3,-900);
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(mp),binding(mp),
                Optional.empty(),0,ModernCombatProfile.VitalPolicy.RETAINED_SAVED));
    }

    @Test void savedZeroHpIsRetainedDeadNotAutomaticallyResurrected() {
        var q=withVitals(qualified(List.of()),100,50,0,0,-900);
        var profile=ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q),binding(q),Optional.empty(),0,
                ModernCombatProfile.VitalPolicy.RETAINED_SAVED);
        assertEquals(0,profile.effective().initialHp());assertEquals(0,profile.effective().initialMp());
    }

    @Test void i64MaximaStatsAndSignedAcAreNeverNativeIntClamped() {
        var q=withVitals(qualified(List.of(simpleArmor(1,1,bonus(MAX_HP,1,MAX_MP,1,STR,0x1_0000_0000L)))),
                Long.MAX_VALUE-1,Long.MAX_VALUE-1,1,1,Long.MIN_VALUE);
        var result=profile(q,Optional.empty());
        assertEquals(Long.MAX_VALUE,result.effective().maximumHp());assertEquals(Long.MAX_VALUE,result.effective().maximumMp());
        assertEquals(0x1_0000_00c8L,result.effective().strength());assertEquals(Long.MIN_VALUE,result.effective().armorClass());
    }

    @Test void maximumAndSixStatOverflowFailBeforeReturningAnyProfile() {
        for(Stat stat:List.of(MAX_HP,STR,DEX,CON,INT,WIS,CHA)) {
            var q=qualified(List.of(simpleArmor(1,1,bonus(stat,Long.MAX_VALUE))));
            assertThrows(ArithmeticException.class,()->profile(q,Optional.empty()));
        }
        var q=withVitals(qualified(List.of(simpleArmor(1,1,bonus(MAX_MP,Long.MAX_VALUE)))),100,1,0,0,-900);
        assertThrows(ArithmeticException.class,()->profile(q,Optional.empty()));
    }

    @Test void negativeEffectiveRangesFailClosedWithoutZeroFallback() {
        for(Stat stat:List.of(MAX_HP,MAX_MP,STR,DEX,CON,INT,WIS,CHA)) {
            var q=qualified(List.of(simpleArmor(1,1,bonus(stat,-10000))));
            assertThrows(IllegalArgumentException.class,()->profile(q,Optional.empty()));
        }
    }

    @Test void acDeltaIntermediateOverflowIsRejectedEvenWhenFinalMathematicsCouldFit() {
        var a=simpleArmor(1,1,bonus(AC,Long.MIN_VALUE));
        var b=instance(2,2,ModernCharacterData.ItemKind.ARMOR,false,0,"$2",List.of(),bonus(AC,Long.MAX_VALUE),
                Ability.zero(),emptyCards(),Stone.none(),TemplatePhysical.zero());
        var q=qualified(List.of(a,b));
        assertThrows(ArithmeticException.class,()->profile(q,stored(q,1,Map.of(1L,false,2L,true))));
    }

    @Test void staleOverrideIsNotAppliedAndDoesNotEraseSourceBaseline() {
        var original=qualified(List.of(simpleArmor(1,1,bonus(AC,-7,MAX_HP,50))));
        var saved=stored(original,6,Map.of(1L,false));
        var current=qualified(List.of(simpleArmor(1,1,bonus(AC,-11,MAX_HP,90))));
        var result=profile(current,saved);
        assertEquals(-900,result.effective().armorClass());assertEquals(190,result.effective().maximumHp());
        assertTrue(result.equipped().get(0).equipped());assertEquals(6,result.equipmentRevision());
    }

    @Test void equalObjectIdsAndIdenticalItemsFromDifferentSourcesNeverShareReconciliation() {
        var q=qualified(List.of(simpleArmor(1,1,Ability.zero())));
        var other=qualified(42,0,List.copyOf(q.items().values()),defaults(List.copyOf(q.items().values()),false),false,List.of(),List.of());
        assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,()->ModernCombatProfile.qualify(
                ModernCombatProfile.Input.unqualified(other),binding(q),Optional.empty(),0,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH));
        assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,()->ModernCombatProfile.qualify(
                ModernCombatProfile.Input.unqualified(other),binding(other),stored(q,1,Map.of(1L,false)),0,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH));
        byte[] changed=new byte[32];changed[0]=1;
        assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,()->ModernCombatProfile.qualify(
                ModernCombatProfile.Input.unqualified(q),new ModernEquipmentReconciliation.ExpectedBinding(binding(q).key(),
                new ModernEquipmentOverrideStore.Fingerprint(changed)),stored(q,1,Map.of(1L,false)),0,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH));
    }

    @Test void oldTemplateFixtureIsExplicitlyUnqualifiedNotZeroWeaponDamage() {
        var q=qualified(List.of(weapon(1,1,true)));
        var p=profile(q,Optional.empty());
        assertEquals(ModernCombatProfile.WeaponState.UNQUALIFIED,p.weapon().state());assertNull(p.weapon().facts());
        assertEquals(1,p.weapon().objectId());assertFalse(p.attackImplemented());
        assertEquals(ModernCombatProfile.WeaponState.UNARMED,profile(qualified(List.of()),Optional.empty()).weapon().state());
    }

    @Test void qualifiedWeaponKeepsSmallLargeRangeMaterialProcAndLargeIds() {
        long id=0x1_0000_0000L,catalog=0x2_0000_0000L;
        var q=qualified(List.of(weapon(id,catalog,true)));
        var facts=new ModernCombatProfile.WeaponFacts(id,catalog,1,Long.MAX_VALUE,Long.MAX_VALUE-1,-1,17,2,100,37,1.5,8,99);
        var p=ModernCombatProfile.qualify(new ModernCombatProfile.Input(q,true,Map.of(id,facts)),binding(q),Optional.empty(),7,
                ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertEquals(facts,p.weapon().facts());assertEquals(ModernCombatProfile.WeaponState.QUALIFIED,p.weapon().state());
        assertEquals(-5,p.weapon().enchantment());assertEquals(7,p.evaluatedAtMillis());assertFalse(p.attackImplemented());
        assertFalse(p.toString().contains("fixture"));assertFalse(p.weapon().toString().contains(Long.toString(id)));
    }

    @Test void fullQualificationIncludesUnequippedWeaponAndRejectsMissingExtraWrongTypeOrCatalog() {
        var q=qualified(List.of(weapon(1,1,true),weapon(2,2,false)));
        var first=facts(1,1,1);var second=facts(2,2,1);
        var map=new java.util.HashMap<>(Map.of(1L,first,2L,second));
        var input=new ModernCombatProfile.Input(q,true,map);map.clear();assertEquals(2,input.weapons().size());
        assertThrows(UnsupportedOperationException.class,()->input.weapons().clear());
        for(var invalid:List.of(Map.of(1L,first),Map.of(1L,first,2L,second,3L,facts(3,3,1)),
                Map.of(1L,facts(1,99,1),2L,second),Map.of(1L,facts(1,1,2),2L,second))) {
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.Input(q,true,invalid));
        }
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.Input(q,false,Map.of(1L,first)));
    }

    @Test void weaponReplacementUsesNewWeaponFactsButDoesNotAlterStoredTemplateFingerprint() {
        var q=qualified(List.of(weapon(1,1,true),weapon(2,2,false)));
        var input=new ModernCombatProfile.Input(q,true,Map.of(1L,facts(1,1,1),2L,facts(2,2,1)));
        var result=ModernCombatProfile.qualify(input,binding(q),stored(q,1,Map.of(1L,false,2L,true)),0,
                ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertEquals(2,result.weapon().objectId());assertEquals(2,result.weapon().facts().catalogId());
        assertEquals(5,result.weapon().templateModifiers().meleeDamage());
        assertEquals(3,result.weapon().itemModifiers().get(MELEE_DAMAGE));
        assertEquals(3,result.equipmentEvaluation().total(MELEE_DAMAGE)); // Template is separate, not lost or double-added.
    }

    @Test void full512CandidatesRemainBoundedImmutableWithoutNativeObjectIdTruncation() {
        var items=new ArrayList<Instance>();
        for(int i=0;i<512;i++)items.add(weapon(0x1_0000_0000L+i,0x2_0000_0000L+i,i==0));
        var q=qualified(items);var weapons=new java.util.HashMap<Long,ModernCombatProfile.WeaponFacts>();
        for(var item:items)weapons.put(item.source().objectId(),facts(item.source().objectId(),item.source().template().catalogId(),1));
        var input=new ModernCombatProfile.Input(q,true,weapons);
        var result=ModernCombatProfile.qualify(input,binding(q),Optional.empty(),0,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertEquals(512,result.equipped().size());assertEquals(0x1_0000_0000L,result.weapon().objectId());
        assertThrows(UnsupportedOperationException.class,()->result.equipped().clear());
    }

    @Test void malformedWeaponFactsAndNonfiniteMultipliersCannotImplyQualification() {
        for(double value:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,1,11,1,0,0,value,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,-2,11,1,0,0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,1,11,1,101,0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,1,11,1,0,101,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,1,23,1,0,0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProfile.WeaponFacts(1,1,1,1,1,1,11,1,0,0,1,1,0));
        assertTrue(facts(1,1,4).ranged());assertTrue(facts(1,1,10).ranged());assertTrue(facts(1,1,13).ranged());assertFalse(facts(1,1,1).ranged());
    }

    static Instance weapon(long object,long catalog,boolean equipped) {
        return instance(object,catalog,ModernCharacterData.ItemKind.WEAPON,equipped,-5,"$"+catalog,List.of(),bonus(MELEE_DAMAGE,3),
                Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(4,5,0,0,0,0));
    }
    static ModernCombatProfile.WeaponFacts facts(long object,long catalog,long type) {
        return new ModernCombatProfile.WeaponFacts(object,catalog,type,8,12,1,11,1,10,20,1.5,0,0);
    }
    static ModernEquipmentReconciliation.ExpectedBinding binding(Qualified q) {
        return new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(q.source().sourceId(),q.source().character().objectId()),BINDING);
    }
    static Optional<ModernEquipmentOverrideStore.Stored> stored(Qualified q,long revision,Map<Long,Boolean> desired) {
        var rows=desired.entrySet().stream().map(e->new ModernEquipmentOverrideStore.ItemOverride(e.getKey(),q.items().get(e.getKey()).source().template().catalogId(),
                ModernEquipmentReconciliation.fingerprint(q.items().get(e.getKey()),q.lifetimes().items().get(e.getKey())),e.getValue())).toList();
        return Optional.of(new ModernEquipmentOverrideStore.Stored(binding(q).key(),revision,new ModernEquipmentOverrideStore.Replacement(BINDING,rows)));
    }
    static ModernCombatProfile profile(Qualified q,Optional<ModernEquipmentOverrideStore.Stored> stored) {
        return ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q),binding(q),stored,1000,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
    }
    static Qualified withVitals(Qualified q,long maxHp,long maxMp,long hp,long mp,long ac) {
        var p=q.source().persisted();
        var saved=new ModernCharacterData.PersistedValues(maxHp,maxMp,hp,mp,ac,p.strength(),p.dexterity(),p.constitution(),p.intelligence(),p.wisdom(),p.charisma(),
                p.experience(),p.lawful(),p.originalStrength(),p.originalDexterity(),p.originalConstitution(),p.originalIntelligence(),p.originalWisdom(),p.food225());
        return new Qualified(new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),saved,q.source().inventory()),
                q.items(),q.catalog(),q.lifetimes());
    }
}
