package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModernVipCharacterProfileTest {
    @Test void completeVipAndExactlyOneShapeAugmentBaseOnceWithDistinctReductionAndRates() {
        var source=ModernVipLoadoutTest.source(true,true);var result=qualify(source,5642);var p=result.character();var saved=p.source().persisted();
        assertEquals(saved.maxHp()+184,p.effective().maximumHp());assertEquals(saved.maxMp()+207,p.effective().maximumMp());
        assertEquals(saved.strength()+23,p.effective().strength());assertEquals(saved.dexterity()+46,p.effective().dexterity());
        assertEquals(saved.constitution()+69,p.effective().constitution());assertEquals(saved.intelligence()+92,p.effective().intelligence());
        assertEquals(saved.wisdom()+115,p.effective().wisdom());assertEquals(saved.charisma()+138,p.effective().charisma());
        long intrinsic=ModernCharacterArmor.intrinsic(p.source().character().characterClass(),p.source().character().level(),saved.originalDexterity());
        assertEquals(intrinsic+161,p.effective().armorClass());assertEquals(414,result.genericDamageReduction());
        assertEquals(437,result.magicOnlyReduction());assertEquals(690,result.experienceRate());assertEquals(93,result.adenaRate());
        assertEquals(322,p.equipmentEvaluation().total(ModernEquipmentFacts.Stat.MELEE_DAMAGE));
        assertEquals(0,p.equipmentEvaluation().total(ModernEquipmentFacts.Stat.REDUCTION));
        assertEquals(0,p.equipmentEvaluation().total(ModernEquipmentFacts.Stat.MAGIC_DAMAGE));
        assertEquals(ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP,p.armorPolicy());
        assertThrows(IllegalStateException.class,p::sourceBaseline);assertFalse(p.attackImplemented());
        assertEquals(p.effective().maximumHp(),p.effective().initialHp());
    }
    @Test void savedTotalArmorAndSavedLiveHpDoNotContaminateExplicitRebuild() {
        var s=ModernVipLoadoutTest.source(true,false);var q=s.input().equipment();
        var a=ModernVipLoadoutTest.withEquipment(s,ModernCombatProfileTest.withVitals(q,100,50,999999,888888,Long.MIN_VALUE));
        var b=ModernVipLoadoutTest.withEquipment(s,ModernCombatProfileTest.withVitals(q,100,50,1,0,Long.MAX_VALUE));
        assertEquals(qualify(a,5641).character().effective(),qualify(b,5641).character().effective());
        var old=ModernCombatProfile.qualify(a.input(),ModernCombatProfileTest.binding(a.input().equipment()),Optional.empty(),1000,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertEquals(Long.MIN_VALUE,old.effective().armorClass());assertEquals(100,old.effective().maximumHp());
        assertEquals(ModernCombatProfile.ArmorPolicy.PRESERVED_SOURCE_TOTAL,old.armorPolicy());
    }
    @Test void missingShapeIsExplicitAndNeverGuessedFromTheItemOrder() {
        var s=ModernVipLoadoutTest.source(true,true);var p=qualify(s,0);
        assertEquals(s.input().equipment().source().persisted().strength()+3,p.character().effective().strength());
        assertTrue(p.vip().shape().isEmpty());assertThrows(IllegalArgumentException.class,()->qualify(s,999));
    }
    @Test void negativePartialOldVipIsNotAppliedBeforeCompensatingShapeAndCompleteValue() {
        var changed=ModernVipLifecycleTest.change(ModernVipLoadoutTest.source(true,false),300086,STR,-300,600,null);
        var q=changed.input().equipment();var c=q.catalog();var rules=new HashMap<>(c.lookups());
        var key=new ModernEquipmentFacts.RuleKey(ModernEquipmentFacts.RuleKind.VIP,ModernCharacterData.ItemKind.ETC,300086,0);
        rules.put(key,ModernEquipmentFacts.RuleValue.present(rules.get(key).raw().with(ModernEquipmentFacts.Stat.STR,-300)));
        var s=ModernVipLoadoutTest.withEquipment(changed,new ModernEquipmentFacts.Qualified(q.source(),q.items(),
                new ModernEquipmentFacts.Catalog(c.sourceId(),c.resourceDatabase(),rules,c.verifiedSetIds(),c.sets(),c.combos(),c.vipTableState()),q.lifetimes()));
        assertEquals(s.input().equipment().source().persisted().strength()+300,qualify(s,5641).character().effective().strength());
        assertThrows(IllegalArgumentException.class,()->qualify(s,0));
    }
    @Test void foreignBundleBindingRevisionAndTimeCannotReuseCompleteVipProof() {
        var s=ModernVipLoadoutTest.source(true,false);var q=s.input().equipment();var binding=ModernCombatProfileTest.binding(q);
        var vip=ModernVipLoadout.qualifyForRebuild(s,binding,Optional.empty(),1000,5641);
        var clone=ModernVipLoadoutTest.withEquipment(s,new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),q.lifetimes()));
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualifyRebuiltVip(clone.input(),binding,Optional.empty(),1000,vip));
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualifyRebuiltVip(s.input(),binding,Optional.empty(),1001,vip));
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualifyRebuiltVip(s.input(),binding,ModernCombatProfileTest.stored(q,1,Map.of()),1000,vip));
        var other=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(42,binding.key().objectId()),binding.fingerprint());
        assertThrows(IllegalArgumentException.class,()->ModernCombatProfile.qualifyRebuiltVip(s.input(),other,Optional.empty(),1000,vip));
    }
    @Test void expiredVipDoesNotRemainInReconstructedArmorOrAttributes() {
        var s=ModernVipLifecycleTest.timed(ModernVipLoadoutTest.source(true,false),1000,"2026-10-01 00:00:00",1);
        var q=s.input().equipment();var p=ModernVipCharacterProfile.qualify(s,ModernCombatProfileTest.binding(q),Optional.empty(),2000,0);
        assertTrue(p.vip().items().isEmpty());assertEquals(q.source().persisted().strength(),p.character().effective().strength());
        assertEquals(ModernCharacterArmor.intrinsic(q.source().character().characterClass(),q.source().character().level(),q.source().persisted().originalDexterity()),p.character().effective().armorClass());
        assertEquals(-1,p.character().validUntilMillis());
    }
    @Test void unclassifiedEtcVipRuleCannotBeSilentlyLostByCompleteReplacement() {
        var s=ModernVipLoadoutTest.source(true,true);var policies=new HashMap<>(s.templates());
        policies.put(300087L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ETC,"other.Custom",null));
        var facts=s.vipCatalog();var vips=new HashMap<>(facts.vips());vips.remove(300087L);var polys=new HashMap<>(facts.polymorphs());polys.remove(5642L);
        var altered=new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),
                new ModernVipEquipment.Catalog(7,"catalog",vips,polys));
        assertThrows(IllegalArgumentException.class,()->qualify(altered,5641));
    }
    @Test void fullI64OverflowFailsAndOldMatchCannotCrossIntoRebuiltArmorPolicy() {
        var overflow=ModernVipLifecycleTest.change(ModernVipLoadoutTest.source(true,false),300086,MAX_HP,Long.MAX_VALUE,0,null);
        assertThrows(ArithmeticException.class,()->qualify(overflow,0));
        var s=ModernVipLoadoutTest.source(true,false);var q=s.input().equipment();var b=ModernCombatProfileTest.binding(q);
        var old=ModernCombatProfile.qualify(s.input(),b,Optional.empty(),1000,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        var next=ModernVipCharacterProfile.qualify(s,b,ModernCombatProfileTest.stored(q,1,Map.of()),1100,5641).character();
        assertThrows(IllegalArgumentException.class,()->next.retainMatchVitals(old,50,0));
    }
    static ModernVipCharacterProfile qualify(ModernPhysicalCombat.Source s,long shape) {
        return ModernVipCharacterProfile.qualify(s,ModernCombatProfileTest.binding(s.input().equipment()),Optional.empty(),1000,shape);
    }
}
