package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static local.lito381.server.ModernCombatProfileTest.stored;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernBattleLoadoutTest {
    static final ModernBattleLoadout.Features ALL=new ModernBattleLoadout.Features(true,true,true,true,true,true);
    static final ModernBattleLoadout.Features BASE=new ModernBattleLoadout.Features(false,false,false,false,false,false);
    static ModernPhysicalCombat.Source source(int id) {
        return source(id,false);
    }
    static ModernPhysicalCombat.Source source(int id,boolean armed) {
        var armor=simpleArmor(1,20014,bonus(MAX_HP,90,MAX_MP,70,AC,-7,STR,4,CON,5,INT,20,SP,3,MR,11,
                HP_REGEN,4,MP_REGEN,5,POTION_RECOVERY,8,WEIGHT_REDUCTION,10,MELEE_DAMAGE,40));
        var items=new ArrayList<ModernEquipmentFacts.Instance>();items.add(armor);
        if(armed)items.add(ModernPhysicalCombatTest.weapon(2,2,true,0,ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.TemplatePhysical.zero()));
        var base=ModernPhysicalCombatTest.source(id,1,30,items);
        var magic=ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(base,12,17,0,15,50));
        var rows=new ArrayList<ModernHonorHealing.Row>();
        for(int i=1;i<=127;i++)rows.add(new ModernHonorHealing.Row(i,i>20?5:0));
        return new ModernPhysicalCombat.Source(magic.input(),magic.growth(),magic.templates(),magic.procCatalogIds(),
                magic.magicGrowth(),armed?Map.of(2L,0L):Map.of(),armed?Map.of(2L,-1L):Map.of(),ModernNaturalRegenTest.growth(id,"catalog",1),
                new ModernHonorHealing.Growth(id,"catalog",rows));
    }
    static ModernBattleLoadout initial(ModernPhysicalCombat.Source source) {
        var fighter=ModernPhysicalCombatTest.fighter(source);
        return ModernBattleLoadout.initial(fighter,ALL,ModernInventoryWeight.qualify(fighter.profile(),1.25));
    }
    static ModernPhysicalCombat.Fighter candidate(ModernPhysicalCombat.Source source,ModernBattleLoadout before,long revision,boolean equipped,long now) {
        var q=source.input().equipment();
        return ModernPhysicalCombat.qualifyEquipmentChange(source,binding(q),stored(q,revision,Map.of(1L,equipped)),now,before.fighter(),23,7);
    }
    @Test void oneReplacementRebuildsAllDerivedAbilitiesFromOneFighterWithoutRewritingBirthProfile() {
        var source=source(7);var before=initial(source);var fighter=candidate(source,before,1,false,1);
        var after=before.replacement(fighter);
        assertSame(fighter,after.fighter());assertSame(fighter.magic(),after.magic());assertSame(fighter.regen(),after.regen());
        assertSame(fighter.healing(),after.healing());assertSame(fighter.profile(),after.magic().profile());
        assertSame(fighter.profile(),after.regen().combat());
        assertEquals(100,after.combatant().maxHp());assertEquals(50,after.combatant().maxMp());
        assertEquals(23,after.combatant().initialHp());assertEquals(7,after.combatant().initialMp());
        assertEquals(190,before.combatant().maxHp());assertEquals(120,before.combatant().maxMp());
        assertEquals(3,before.magic().offense().itemSpellPower());assertEquals(0,after.magic().offense().itemSpellPower());
        assertEquals(11,before.magic().defense().equipmentMagicResistance());assertEquals(0,after.magic().defense().equipmentMagicResistance());
        assertEquals(4,before.regen().values().equipmentHpRegen());assertEquals(0,after.regen().values().equipmentHpRegen());
        assertEquals(5,before.regen().values().equipmentMpRegen());assertEquals(0,after.regen().values().equipmentMpRegen());
        assertEquals(8,before.potionRecovery());assertEquals(0,after.potionRecovery());
        assertEquals(ModernFireStorm.qualify(fighter),after.fireStorm());assertEquals(ModernDisintegrate.qualify(fighter),after.disintegrate());
        assertSame(after.fireStorm(),after.fireStorm());assertSame(after.disintegrate(),after.disintegrate());
        assertTrue(after.fireStorm().mpCost()>before.fireStorm().mpCost());assertTrue(after.disintegrate().mpCost()>before.disintegrate().mpCost());
        assertEquals(7,before.healing().diceCount());assertEquals(2,after.healing().diceCount());
        assertEquals(2,before.healing().mpCost());assertEquals(4,after.healing().mpCost());
        assertEquals(ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH,before.fighter().profile().vitalPolicy());
    }
    @Test void reweightUsesRemainingPotionAndAmmoUnitsNotOriginalShopQuantities() {
        var source=source(7);var before=initial(source);var after=before.replacement(candidate(source,before,1,false,1));
        var items=List.of(new ModernItemProtocol.Stock(40010,10,3));
        var ammo=List.of(new ModernAmmo.Stock(ModernAmmoTest.facts(40744),2,0));
        var units=List.of(new ModernInventoryWeight.Unit(40010,2000),new ModernInventoryWeight.Unit(40744,1000));
        var oldWeight=before.reweight(items,ammo,units);var newWeight=after.reweight(items,ammo,units);
        assertEquals(6,newWeight.view().suppliesWeight());assertEquals(oldWeight.units(),newWeight.units());
        assertEquals(oldWeight.view().totalWeight(),newWeight.view().totalWeight());
        assertEquals(before.weightBasis().sourceGearWeight(),after.weightBasis().sourceGearWeight());
        assertTrue(before.weightBasis().maximumWeight()>after.weightBasis().maximumWeight());
        assertEquals(1,after.weightBasis().equipmentRevision());assertEquals(1.25,after.weightBasis().sourceRate());
        assertEquals(3,items.get(0).remaining());assertEquals(0,ammo.get(0).remaining());
        assertThrows(IllegalArgumentException.class,()->after.reweight(items,ammo,List.of(units.get(0))));
        assertEquals(newWeight,after.reweight(items,ammo,units));
    }
    @Test void capturedWeightLifetimeCanTightenButCannotBeExtendedByAReplacement() {
        var source=source(7);var fighter=ModernPhysicalCombatTest.fighter(source);var full=ModernInventoryWeight.qualify(fighter.profile(),1);
        var tight=new ModernInventoryWeight.Basis(full.source(),0,full.sourceGearWeight(),full.maximumWeight(),full.sourceRate(),5);
        var before=ModernBattleLoadout.initial(fighter,ALL,tight);
        var after=before.replacement(candidate(source,before,1,false,4));
        assertEquals(5,after.weightBasis().validUntilMillis());assertEquals(5,before.weightBasis().validUntilMillis());
        assertFalse(before.currentAt(-1));assertTrue(before.currentAt(0));assertTrue(before.currentAt(4));assertFalse(before.currentAt(5));
        assertFalse(after.currentAt(3));assertTrue(after.currentAt(4));assertFalse(after.currentAt(5));
        assertThrows(IllegalArgumentException.class,()->before.replacement(candidate(source,before,1,false,5)));
    }
    @Test void npcOffenseAndDefenseBelongToSameReplacementAndKeepExplicitBlessingBinding() {
        var source=source(7,true);var before=initial(source).withNpc(ModernNpcMeleeTest.INTERNAL);
        var fighter=candidate(source,before,1,false,1);var after=before.replacement(fighter);
        assertSame(fighter,after.npcPlayer().fighter());assertNotSame(before.npcVictim(),after.npcVictim());
        assertEquals(ModernParticipantId.human(fighter.key()),after.npcVictim().identity());
        var catalog=ModernNpcMeleeTest.catalog();var npc=ModernNpcMeleeTest.actor(catalog);
        var fresh=ModernNpcMelee.player(fighter,ModernNpcMeleeTest.INTERNAL);
        var oldHit=ModernNpcMelee.againstNpc(before.npcPlayer(),catalog,npc,0,b->0);
        var newHit=ModernNpcMelee.againstNpc(after.npcPlayer(),catalog,npc,0,b->0);
        assertEquals(ModernNpcMelee.againstNpc(fresh,catalog,npc,0,b->0),newHit);assertTrue(oldHit.rawDamage()>newHit.rawDamage());
        assertEquals(ModernNpcMelee.againstParticipant(catalog,npc,ModernNpcMelee.victim(fresh),100,false,b->0),
                ModernNpcMelee.againstParticipant(catalog,npc,after.npcVictim(),100,false,b->0));
        assertThrows(IllegalArgumentException.class,()->before.withNpc(ModernNpcMeleeTest.INTERNAL));
        assertThrows(IllegalArgumentException.class,()->initial(source).withNpc(new ModernNpcMelee.BlessingBinding(42,"catalog",ModernNpcMeleeTest.INTERNAL.convention())));
    }
    @Test void replacementRejectsForeignSourceSkippedOrRepeatedGearAndFullSpawnProfiles() {
        var source=source(7);var before=initial(source);var first=candidate(source,before,1,false,1);var after=before.replacement(first);
        assertThrows(IllegalArgumentException.class,()->after.replacement(first));
        var second=candidate(source,after,2,true,2);assertThrows(IllegalArgumentException.class,()->before.replacement(second));
        var foreign=source(42);var other=initial(foreign);
        assertThrows(IllegalArgumentException.class,()->before.replacement(candidate(foreign,other,1,false,1)));
        assertThrows(IllegalArgumentException.class,()->before.replacement(ModernPhysicalCombatTest.fighter(source)));
        assertThrows(IllegalArgumentException.class,()->ModernBattleLoadout.initial(first,ALL,after.weightBasis()));
        assertThrows(IllegalArgumentException.class,()->after.withNpc(ModernNpcMeleeTest.INTERNAL));
        assertSame(before.fighter().profile(),before.magic().profile());
    }
    @Test void aMissingPreviouslyQualifiedGrowthProjectionCannotBecomeAnImplicitZero() {
        var source=source(7);var fighter=ModernPhysicalCombatTest.fighter(source);
        var before=ModernBattleLoadout.initial(fighter,BASE,null);
        var missing=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds());
        var next=candidate(missing,before,1,false,1);
        assertThrows(IllegalArgumentException.class,()->before.replacement(next));
        assertNotNull(before.magic());assertNotNull(before.regen());assertNotNull(before.healing());
    }
    @Test void unnegotiatedSkillAndItemProjectionsStayDisabledEvenWhenSourceContainsTheirFacts() {
        var source=source(7);var before=ModernBattleLoadout.initial(ModernPhysicalCombatTest.fighter(source),BASE,null);
        var after=before.replacement(candidate(source,before,1,false,1));
        for(var value:List.of(before,after)) {
            assertNull(value.fireStorm());assertNull(value.disintegrate());assertNull(value.weightBasis());
            assertNull(value.npcPlayer());assertNull(value.npcVictim());assertEquals(0,value.potionRecovery());
            assertThrows(IllegalArgumentException.class,()->value.reweight(List.of(),List.of(),List.of()));
        }
    }
    @Test void botProjectionNeverSynthesizesHumanEquipmentMagicOrNpcOffense() {
        var id=ModernParticipantId.bot(7,3333);var bot=ModernBattleLoadout.bot(id,ModernBotTemplate.roomTestDummy(1,0));
        assertNull(bot.fighter());assertNull(bot.magic());assertNull(bot.regen());assertNull(bot.healing());
        assertNull(bot.fireStorm());assertNull(bot.disintegrate());assertNull(bot.npcPlayer());
        assertEquals(id,bot.npcVictim().identity());assertEquals(id,bot.combatant().identity());
        assertTrue(bot.combatant().passive());assertEquals(40,bot.combatant().maxHp());
        assertThrows(IllegalArgumentException.class,()->bot.replacement(ModernPhysicalCombatTest.fighter(source(7))));
        assertThrows(IllegalArgumentException.class,()->bot.withNpc(ModernNpcMeleeTest.INTERNAL));
    }
    @Test void arbitraryWeightBasisCannotBorrowAnotherEquipmentRevisionOrCapacity() {
        var source=source(7);var fighter=ModernPhysicalCombatTest.fighter(source);var full=ModernInventoryWeight.qualify(fighter.profile(),1);
        var wrong=new ModernInventoryWeight.Basis(full.source(),1,full.sourceGearWeight(),full.maximumWeight(),1,-1);
        assertThrows(IllegalArgumentException.class,()->ModernBattleLoadout.initial(fighter,ALL,wrong));
        var forged=new ModernInventoryWeight.Basis(full.source(),0,full.sourceGearWeight(),full.maximumWeight()+1,1,-1);
        assertThrows(IllegalArgumentException.class,()->ModernBattleLoadout.initial(fighter,ALL,forged));
    }
    @Test void runtimeRoutingCannotReintroducePreparedFighterWeightOrSeparateNpcCaches()throws Exception {
        String text=Files.readString(Path.of("src/main/java/local/lito381/server/ModernBattleEngine.java"));
        assertFalse(text.contains(".prepared.fighter()"));assertFalse(text.contains(".prepared.weightBasis()"));
        assertFalse(text.contains("npcPlayers"));assertFalse(text.contains("npcVictims"));
        assertFalse(text.contains("ModernCombatant combatant;"));assertFalse(text.contains("ModernDisintegrate.Profile disintegrateProfile;"));
        assertTrue(text.contains("actor.loadout = candidateLoadout;"));
        assertTrue(text.contains("actor.loadout.regen().values()"));assertTrue(text.contains("actor.loadout.healing()"));
        assertTrue(text.contains("actor.loadout.npcPlayer()"));assertTrue(text.contains("target.loadout.npcVictim()"));
    }
    @Test void sourcePinDetectsChangedDerivedGrowthRatherThanJustMatchingSqlAndPhysicalDamage() {
        var source=source(7);var before=initial(source);
        assertTrue(before.matchesDerivedFacts(ModernPhysicalCombatTest.fighter(source)));
        var changed=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),
                ModernBattleEngineHonorTest.healing(7,"catalog",99));
        var current=ModernPhysicalCombatTest.fighter(changed);
        assertEquals(before.fighter().profile().source(),current.profile().source());
        assertEquals(before.fighter().values(),current.values());assertFalse(before.matchesDerivedFacts(current));
        assertFalse(before.matchesDerivedFacts(null));
    }
    @Test void sourcePinAcceptsFreshFullVitalsForSameRetainedProjectionButRejectsChangedRegenTable() {
        var source=source(7);var before=initial(source);var after=before.replacement(candidate(source,before,1,false,1));
        var q=source.input().equipment();var saved=stored(q,1,Map.of(1L,false));
        var fresh=ModernPhysicalCombat.qualify(source,binding(q),saved,2);
        assertNotEquals(after.fighter().profile().effective().initialHp(),fresh.profile().effective().initialHp());
        assertTrue(after.matchesDerivedFacts(fresh));
        var changed=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),ModernNaturalRegenTest.growth(7,"catalog",99),source.healingGrowth());
        assertFalse(after.matchesDerivedFacts(ModernPhysicalCombat.qualify(changed,binding(q),saved,2)));
    }
}
