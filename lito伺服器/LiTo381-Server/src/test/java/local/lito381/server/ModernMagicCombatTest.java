package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernMagicCombatTest {
    @Test void realLightArrowDieIntelligenceAndCriticalAreNotFixedDamage() {
        var a=fighter(7,0,List.of(),12,0,0,0);var b=fighter(42,1,List.of(),12,0,0,0);
        var low=ModernMagicCombat.resolve(a,b,100,rolls(0,99));
        var high=ModernMagicCombat.resolve(a,b,100,rolls(9,99));
        var critical=ModernMagicCombat.resolve(a,b,100,rolls(9,0));
        assertEquals(7,low.rawDamage());assertEquals(17,high.rawDamage());assertEquals(25,critical.rawDamage());
        assertFalse(low.critical());assertTrue(critical.critical());assertEquals(75,critical.remainingHp());
        assertEquals(100,b.profile().effective().initialHp());assertEquals(3,a.mpCost());assertEquals(10,a.range());
    }
    @Test void effectiveIntelligenceItemSpOriginalAndEquipmentDamageAreSeparatedExactlyOnce() {
        var armor=simpleArmor(2,2,bonus(INT,2,SP,3,MAGIC_DAMAGE,7,MAGIC_HIT,999));
        var a=fighter(7,3,List.of(armor),12,0,13,0);var b=fighter(42,1,List.of(),12,0,0,0);
        assertEquals(14,a.offense().effectiveIntelligence());assertEquals(3,a.offense().itemSpellPower());
        assertEquals(5,a.offense().intelligenceTerm());assertEquals(7,a.offense().equipmentDamage());
        assertEquals(0,a.offense().originalHit());assertEquals(1,a.offense().originalDamage());
        // floor(16*(32+5*3)/32)=23, then original1 + equipment7. No MAGIC_HIT roll.
        var result=ModernMagicCombat.resolve(a,b,100,rolls(9,99));assertEquals(31,result.rawDamage());
    }
    @Test void sevenClassesRetainOriginalThresholdsAndCircleOneIgnoresCurrentIntMpReduction() {
        long[] costs={1,1,3,3,1,3,1};
        for(int type=0;type<7;type++) {
            var a=fighter(7,type,List.of(),200,0,18,0);
            assertEquals(costs[type],a.mpCost());assertEquals(BattleMagicResistance.classBase(type),a.defense().classBase());
            for(int original=0;original<=100;original++) {
                assertEquals(BattleMagicResistance.originalMrBonus(type,original),ModernMagicCombat.originalMr(type,original));
                assertEquals(BattleMagicResistance.originalMagicHit(type,original),ModernMagicCombat.originalHit(type,original));
                assertEquals(BattleMagicResistance.originalMagicCritical(type,original),ModernMagicCombat.originalCritical(type,original));
                assertEquals(BattleMagicResistance.originalMagicDamage(type,original),ModernMagicCombat.originalDamage(type,original));
                assertEquals(BattleSpellCatalog.originalMagicConsumeReduction(type,original),ModernMagicCombat.originalMpReduction(type,original));
            }
        }
        assertEquals(2,ModernMagicCombat.originalHit(3,Long.MAX_VALUE));
        assertEquals(0,ModernMagicCombat.originalCritical(3,Long.MAX_VALUE));
        assertEquals(2,fighter(7,0,List.of(),12,0,11,0).mpCost());
        assertEquals(2,fighter(7,0,List.of(),0,0,11,0).mpCost());
    }
    @Test void mrUsesEffectiveWisdomSourceTableOriginalAndEquipmentNotSavedAcOrSpellPower() {
        var armor=simpleArmor(2,2,bonus(WIS,3,MR,7,SP,999,AC,-99));
        var s=stats(ModernPhysicalCombatTest.source(7,3,81,List.of(armor)),12,17,0,15,50);
        var m=ModernPhysicalCombatTest.fighter(enable(s)).magic();
        assertEquals(20,m.profile().effective().wisdom());assertEquals(21,m.defense().wisdomBonus());
        assertEquals(15,m.defense().classBase());assertEquals(40,m.defense().levelBonus());
        assertEquals(1,m.defense().originalBonus());assertEquals(7,m.defense().equipmentMagicResistance());
        assertEquals(84,m.defense().magicResistance());
    }
    @Test void sourceWisdomZeroAndAboveFiftyHaveExplicitLookupRulesWithoutI64AbilityClamp() {
        var g=growth(7);assertEquals(0,g.bonus(0));assertEquals(66,g.bonus(50));assertEquals(66,g.bonus(Long.MAX_VALUE));
        var a=fighter(7,1,List.of(),12,0,0,0);assertEquals(0,a.defense().magicResistance());
        a=fighter(7,1,List.of(),12,0x1_0000_0000L,0,0);
        assertEquals(0x1_0000_0000L,a.profile().effective().wisdom());assertEquals(66,a.defense().wisdomBonus());
        var rows=new ArrayList<>(g.wisdom());rows.set(49,new ModernMagicCombat.WisdomRow(50,123));
        assertEquals(123,new ModernMagicCombat.Growth(7,"catalog",rows).bonus(Long.MAX_VALUE));
    }
    @Test void mrBandsPreserveNativeDiscontinuitiesFloorAndNonpositiveDamageFloor() {
        long[][] samples={{0,100},{50,75},{51,73},{100,57},{101,65},{120,60},{121,56},{140,52},
                {141,57},{160,54},{161,47},{180,45},{181,48},{200,45},{201,38},{220,36},{221,38},{240,36},{241,29},{550,0},{Long.MAX_VALUE,0}};
        for(var sample:samples)assertEquals(sample[1],ModernMagicCombat.afterResistance(100,sample[0],0));
        assertEquals(104,ModernMagicCombat.afterResistance(100,0,4));
        assertEquals(101,ModernMagicCombat.afterResistance(100,1,2)); // floor(-1/2), not integer truncation.
        assertEquals(3,ModernMagicCombat.afterResistance(7,100,0));
    }
    @Test void finalRepresentableMrScaledDamageMustNotFailOnTemporaryPercentProduct() {
        assertEquals(Long.MAX_VALUE,ModernMagicCombat.afterResistance(Long.MAX_VALUE,0,0));
        assertEquals(java.math.BigInteger.valueOf(Long.MAX_VALUE).multiply(java.math.BigInteger.valueOf(57))
                .divide(java.math.BigInteger.valueOf(100)).longValueExact(),ModernMagicCombat.afterResistance(Long.MAX_VALUE,100,0));
        assertEquals(0,ModernMagicCombat.afterResistance(Long.MAX_VALUE,Long.MAX_VALUE,0));
        assertThrows(ArithmeticException.class,()->ModernMagicCombat.afterResistance(Long.MAX_VALUE,0,4));
    }
    @Test void armorReductionUsesExistingQualifiedTemplateSetBaseNotDirectReductionAgain() {
        var armor=instance(2,2,ModernCharacterData.ItemKind.ARMOR,true,0,"$2",List.of(),bonus(REDUCTION,999),
                Ability.zero(),emptyCards(),Stone.none(),new TemplatePhysical(0,0,0,0,12,0));
        var a=fighter(7,0,List.of(),12,0,0,0);var b=fighter(42,1,List.of(armor),12,0,0,0);
        assertEquals(12,b.defense().armorReduction());assertEquals(999,b.profile().equipmentEvaluation().total(REDUCTION));
        assertEquals(6,ModernMagicCombat.resolve(a,b,100,rolls(9,99,0)).rawDamage());
        assertEquals(5,ModernMagicCombat.resolve(a,b,100,rolls(9,99,1)).rawDamage());
    }
    @Test void noOverkillHpCreditOrSourceMutationAndMpRemainsEngineOwned() {
        var a=fighter(7,0,List.of(),12,0,0,0);var b=fighter(42,1,List.of(),12,0,0,0);
        var hit=ModernMagicCombat.resolve(a,b,5,rolls(9,99));
        assertEquals(17,hit.rawDamage());assertEquals(5,hit.appliedDamage());assertEquals(0,hit.remainingHp());assertTrue(hit.death());
        assertEquals(50,a.profile().effective().initialMp());assertEquals(100,b.profile().effective().initialHp());
        for(long hp:new long[]{0,-1,101,Long.MAX_VALUE})assertFailure(ModernMagicCombat.Failure.INVALID_STATE,
                ()->ModernMagicCombat.resolve(a,b,hp,rolls()));
        assertFailure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernMagicCombat.resolve(a,a,1,rolls()));
    }
    @Test void legacySourceHasNoMagicProofAndSourceOrCatalogMismatchCannotQualify() {
        var s=ModernPhysicalCombatTest.source(7,0,1,List.of());assertNull(ModernPhysicalCombatTest.fighter(s).magic());
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),growth(42)));
        var other=new ModernMagicCombat.Growth(7,"other",growth(7).wisdom());
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),other));
        assertThrows(IllegalArgumentException.class,()->ModernMagicCombat.qualify(enable(s),fighter(42,0,List.of(),12,0,0,0).profile()));
    }
    @Test void bowAndInvalidMeleeRangeDoNotBlockOtherwiseQualifiedMagic() {
        var s=ModernPhysicalCombatTest.source(7,0,1,List.of(ModernPhysicalCombatTest.weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        s=ModernPhysicalCombatTest.withType(s,4); // Retained bow.
        var physical=ModernPhysicalCombatTest.fighter(enable(s));
        assertEquals(ModernPhysicalCombat.UnsupportedReason.RANGED,physical.unsupportedReason());assertTrue(physical.magic().supported());
        s=ModernPhysicalCombatTest.weaponFacts(s,new ModernCombatProfile.WeaponFacts(1,1,4,8,8,-1,11,1,0,0,0,0,0));
        assertTrue(ModernPhysicalCombatTest.fighter(enable(s)).magic().supported());
    }
    @Test void unknownExecutorProcAndUnqualifiedWeaponStillBlockMagicWithoutMeleeGate() {
        var s=ModernPhysicalCombatTest.source(7,0,1,List.of(simpleArmor(2,2,Ability.zero())));
        var policies=new HashMap<>(s.templates());policies.put(2L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ARMOR,"custom",null));
        var custom=new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,Set.of(),growth(7));
        assertEquals(ModernMagicCombat.UnsupportedReason.CUSTOM_EXECUTOR,ModernPhysicalCombatTest.fighter(custom).magic().unsupportedReason());
        var proc=new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(2L),growth(7));
        assertEquals(ModernMagicCombat.UnsupportedReason.EQUIPMENT_PROC,ModernPhysicalCombatTest.fighter(proc).magic().unsupportedReason());
        var weapon=ModernPhysicalCombatTest.source(7,0,1,List.of(ModernPhysicalCombatTest.weapon(1,1,true,0,Ability.zero(),TemplatePhysical.zero())));
        var unknown=new ModernPhysicalCombat.Source(ModernCombatProfile.Input.unqualified(weapon.input().equipment()),weapon.growth(),weapon.templates(),Set.of(),growth(7));
        var blocked=ModernPhysicalCombatTest.fighter(unknown).magic();assertEquals(ModernMagicCombat.UnsupportedReason.WEAPON_UNQUALIFIED,blocked.unsupportedReason());
        assertFailure(ModernMagicCombat.Failure.UNSUPPORTED,()->ModernMagicCombat.resolve(blocked,fighter(42,1,List.of(),12,0,0,0),100,rolls()));
    }
    @Test void growthMustBeCompleteOrderedImmutableAndNonnegativeWithoutDefaultRows() {
        var original=growth(7);var rows=new ArrayList<>(original.wisdom());
        var copy=new ModernMagicCombat.Growth(7,"catalog",rows);rows.clear();assertEquals(50,copy.wisdom().size());
        assertThrows(UnsupportedOperationException.class,()->copy.wisdom().clear());
        for(int mode=0;mode<3;mode++) {
            var invalid=new ArrayList<>(original.wisdom());
            if(mode==0)invalid.remove(49);else if(mode==1)invalid.set(1,invalid.get(0));else invalid.add(invalid.get(0));
            assertThrows(IllegalArgumentException.class,()->new ModernMagicCombat.Growth(7,"catalog",invalid));
        }
        assertThrows(IllegalArgumentException.class,()->new ModernMagicCombat.WisdomRow(0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernMagicCombat.WisdomRow(50,-1));
    }
    @Test void unresolvedLifetimeAndExpiredCustomSourceBaselineRemainExplicitlyUnsupported() {
        var s=ModernPhysicalCombatTest.source(7,0,1,List.of(simpleArmor(2,2,bonus(SP,20))));
        var q=s.input().equipment();var source=q.source();
        var fact=new ModernItemLifetime.Fact(2,2,ModernCharacterData.ItemKind.ARMOR,true,
                OptionalLong.empty(),false,OptionalLong.empty(),Optional.empty(),0,false);
        var life=new ModernItemLifetime.Observation(7,source.character().objectId(),"1970-01-01 00:00:00.000000",0,0,Map.of(2L,fact));
        var fresh=new Qualified(source,q.items(),q.catalog(),life);
        var unknown=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(fresh,true,s.input().weapons()),s.growth(),s.templates(),Set.of(),growth(7));
        assertEquals(ModernMagicCombat.UnsupportedReason.LIFETIME_UNQUALIFIED,ModernPhysicalCombatTest.fighter(unknown).magic().unsupportedReason());
        var expired=new ModernItemLifetime.Fact(2,2,ModernCharacterData.ItemKind.ARMOR,true,
                OptionalLong.of(0),true,OptionalLong.of(1),Optional.of("1970-01-01 00:00:00.000000"),0,false);
        life=new ModernItemLifetime.Observation(7,source.character().objectId(),"1970-01-01 00:00:00.000000",0,0,Map.of(2L,expired));
        fresh=new Qualified(source,q.items(),q.catalog(),life);
        var policies=Map.of(2L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ARMOR,"add.VIP",null));
        var stale=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(fresh,true,s.input().weapons()),s.growth(),policies,Set.of(),growth(7));
        var fighter=ModernPhysicalCombatTest.fighter(stale).magic();
        assertEquals(ModernMagicCombat.UnsupportedReason.CUSTOM_EXECUTOR,fighter.unsupportedReason());
        assertEquals(0,fighter.offense().itemSpellPower());assertFalse(fighter.profile().equipped().get(0).equipped());
    }
    @Test void invalidRandomAndCheckedOverflowNeverReturnPartialDamage() {
        var a=fighter(7,0,List.of(),12,0,0,0);var b=fighter(42,1,List.of(),12,0,0,0);
        assertFailure(ModernMagicCombat.Failure.INVALID_RANDOM,()->ModernMagicCombat.resolve(a,b,100,bound->bound));
        assertFailure(ModernMagicCombat.Failure.INVALID_RANDOM,()->ModernMagicCombat.resolve(a,b,100,bound->-1));
        assertFailure(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,()->fighter(7,0,List.of(),Long.MAX_VALUE,0,0,0));
        var huge=fighter(7,0,List.of(),Long.MAX_VALUE/4,0,0,0);
        assertFailure(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,()->ModernMagicCombat.resolve(huge,b,100,rolls(9)));
        var damage=fighter(7,0,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,Long.MAX_VALUE))),12,0,0,0);
        assertFailure(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,()->ModernMagicCombat.resolve(damage,b,100,rolls(0,99)));
        assertEquals(100,b.profile().effective().initialHp());assertEquals(50,a.profile().effective().initialMp());
    }
    @Test void signedNegativeSpAndMagicDamageAreNotTurnedIntoUnsignedBonuses() {
        var a=fighter(7,0,List.of(simpleArmor(2,2,bonus(SP,-99,MAGIC_DAMAGE,-100))),12,0,0,0);
        var b=fighter(42,1,List.of(),12,0,0,0);
        assertEquals(1,a.offense().intelligenceTerm());var hit=ModernMagicCombat.resolve(a,b,100,rolls(9,99));
        assertEquals(0,hit.rawDamage());assertEquals(100,hit.remainingHp());assertFalse(hit.death());
    }
    @Test void helperAndDiagnosticsKeepExactSharedProfileAndDoNotPrintPrivateNames() {
        var row=new ModernApplicationProtocol.CharacterRow(0x1_0000_0001L,"private-character",1,0,1);
        var p=createFighter(42,row,100,50,12,0);
        assertSame(p.profile(),p.magic().profile());assertEquals(new TournamentPresence.CharacterKey(42,row.objectId()),p.magic().key());
        assertFalse(p.magic().toString().contains(row.name()));assertFalse(growth(7).toString().contains("catalog"));
    }

    /** Shared test-only trusted provider fixture; production must read real growth and skill grants. */
    static ModernPhysicalCombat.Fighter createFighter(int sourceId,ModernApplicationProtocol.CharacterRow row,
            long hp,long mp,long intelligence,long wisdom) {
        var s=ModernPhysicalCombatTest.fixtureSource(sourceId,row,hp,10,0);
        return ModernPhysicalCombatTest.fighter(enable(stats(s,intelligence,wisdom,0,0,mp)));
    }
    static ModernMagicCombat.Growth growth(int source) {
        var rows=new ArrayList<ModernMagicCombat.WisdomRow>();
        for(int id=1;id<=50;id++)rows.add(new ModernMagicCombat.WisdomRow(id,BattleMagicResistance.wisdomMagicResistance(id)));
        return new ModernMagicCombat.Growth(source,"catalog",rows);
    }
    static ModernPhysicalCombat.Source enable(ModernPhysicalCombat.Source source) {
        return new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),growth(source.input().equipment().source().sourceId()));
    }
    static ModernPhysicalCombat.Source stats(ModernPhysicalCombat.Source s,long intelligence,long wisdom,long originalInt,long originalWis,long mp) {
        var q=s.input().equipment();var p=q.source().persisted();
        var values=new ModernCharacterData.PersistedValues(p.maxHp(),mp,p.savedHp(),mp,p.armorClass(),p.strength(),p.dexterity(),p.constitution(),
                intelligence,wisdom,p.charisma(),p.experience(),p.lawful(),p.originalStrength(),p.originalDexterity(),p.originalConstitution(),originalInt,originalWis,p.food225());
        var snapshot=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),values,q.source().inventory());
        var qualified=new Qualified(snapshot,q.items(),q.catalog(),q.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,s.input().weaponsQualified(),s.input().weapons()),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth());
    }
    private static ModernMagicCombat.Fighter fighter(int source,int type,List<Instance> items,long intelligence,long wisdom,long originalInt,long originalWis) {
        return ModernPhysicalCombatTest.fighter(enable(stats(ModernPhysicalCombatTest.source(source,type,1,items),intelligence,wisdom,originalInt,originalWis,50))).magic();
    }
    private static ModernPhysicalCombat.BoundedRandom rolls(long... rolls) {
        int[] cursor={0};return bound->{assertTrue(cursor[0]<rolls.length,"unexpected magic RNG");long value=rolls[cursor[0]++];assertTrue(value>=0&&value<bound);return value;};
    }
    private static void assertFailure(ModernMagicCombat.Failure expected,org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected,assertThrows(ModernMagicCombat.CombatException.class,action).failure());
    }
}
