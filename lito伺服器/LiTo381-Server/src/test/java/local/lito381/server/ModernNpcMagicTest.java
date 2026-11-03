package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.MAGIC_DAMAGE;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNpcMagicTest {
    static ModernMagicCombat.Fighter caster(int source,int type,long originalInt,List<Instance> items) {
        return ModernPhysicalCombatTest.fighter(ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(
                ModernPhysicalCombatTest.source(source,type,1,items),12,0,originalInt,0,50))).magic();
    }
    static ModernMagicCombat.Fighter caster(){return caster(7,0,0,List.of());}
    static ModernMagicCombat.Outcome hit(ModernMagicCombat.Fighter pc,Map<ModernNpcCatalog.Field,Long> fields,long... rolls) {
        var c=ModernNpcMeleeTest.catalog(fields,true,0);var random=new ModernNpcMeleeTest.Rolls(rolls);
        var result=ModernNpcMagic.againstNpc(pc,c,ModernNpcMeleeTest.actor(c),random);random.consumed(10,100);return result;
    }
    static void failure(ModernMagicCombat.Failure why,org.junit.jupiter.api.function.Executable action) {
        assertEquals(why,assertThrows(ModernMagicCombat.CombatException.class,action).failure());
    }
    @Test void lightArrowUsesQualifiedDiceIntAndCriticalNotNpcPhysicalHitOrAc() {
        assertEquals(7,hit(caster(),Map.of(MR,0L),0,99).rawDamage());
        assertEquals(17,hit(caster(),Map.of(MR,0L),9,99).rawDamage());
        var critical=hit(caster(),Map.of(MR,0L),9,0);assertEquals(25,critical.rawDamage());assertTrue(critical.critical());
        assertEquals(255,critical.remainingHp());assertFalse(critical.death());
    }
    @Test void attr16AndMagicDefenseDoNotImportPhysicalReductionHardSkinOrWeakAttribute() {
        for(long weak:new long[]{-8,-4,-2,-1,0,1,2,4,8}) {
            var fields=Map.of(MR,0L,WEAK_ATTRIBUTE,weak,AC,Long.MIN_VALUE,REDUCTION,Long.MAX_VALUE,HARD,1L,UNDEAD,3L);
            assertEquals(17,hit(caster(),fields,9,99).rawDamage());
            assertEquals(0,ClassicWorldMonsterMagicDamage.npcElementalResistance((int)weak,16,0));
        }
    }
    @Test void npcMrKeepsEveryRetainedBandWithoutPlayerClassLevelWisdomOrArmorTerms() {
        var pc=caster(7,0,0,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,83))));
        long[][] expected={{0,100},{50,75},{51,73},{100,57},{101,65},{120,60},{121,56},{140,52},
                {141,57},{160,54},{161,47},{180,45},{181,48},{200,45},{201,38},{220,36},{221,38},{240,36},{241,29},{550,0},{Long.MAX_VALUE,0}};
        for(var row:expected)assertEquals(row[1],hit(pc,Map.of(MR,row[0],LEVEL,Long.MAX_VALUE,INT,Long.MAX_VALUE),9,99).rawDamage(),"MR "+row[0]);
    }
    @Test void originalHitCanIncreaseLowMrDamageAndMpCostRemainsSourceClassQualified() {
        var pc=caster(7,5,17,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,80)))); // original damage3 at INT17: 17+80+3=100.
        assertEquals(4,pc.offense().originalHit());assertEquals(104,hit(pc,Map.of(MR,0L),9,99).rawDamage());
        long[] costs={1,1,3,3,1,3,1};
        for(int type=0;type<7;type++)assertEquals(costs[type],caster(7,type,18,List.of()).mpCost());
    }
    @Test void exactI64HpAndDamageDoNotPassThroughNativeIntOrBinary64() {
        long hp=9_007_199_254_740_993L;
        var hit=hit(caster(),Map.of(HP,hp,MR,0L),9,99);assertEquals(hp-17,hit.remainingHp());
        var pc=caster(7,0,0,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,hp))));
        var huge=hit(pc,Map.of(HP,Long.MAX_VALUE,MR,0L),9,99);assertEquals(hp+17,huge.rawDamage());assertEquals(Long.MAX_VALUE-hp-17,huge.remainingHp());
        var overkill=hit(pc,Map.of(HP,5L,MR,0L),9,99);assertEquals(hp+17,overkill.rawDamage());assertEquals(5,overkill.appliedDamage());assertTrue(overkill.death());
        assertEquals(50,pc.profile().effective().initialMp(),"calculator never consumes player MP");
    }
    @Test void maximumRepresentableNpcDamageSurvivesPercentScalingAndCapsOnlyAtVictimHp() {
        var pc=caster(7,0,0,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,Long.MAX_VALUE-17))));
        var result=hit(pc,Map.of(HP,Long.MAX_VALUE,MR,0L),9,99);
        assertEquals(Long.MAX_VALUE,result.rawDamage());assertEquals(Long.MAX_VALUE,result.appliedDamage());assertTrue(result.death());
        var small=hit(pc,Map.of(HP,1L,MR,0L),9,99);assertEquals(1,small.appliedDamage());assertEquals(Long.MAX_VALUE,small.rawDamage());
    }
    @Test void overflowAndInvalidRandomFailBeforeAnyVictimMutation() {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L),true,0);var npc=ModernNpcMeleeTest.actor(c);
        var pc=caster(7,0,0,List.of(simpleArmor(2,2,bonus(MAGIC_DAMAGE,Long.MAX_VALUE))));
        failure(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,()->ModernNpcMagic.againstNpc(pc,c,npc,new ModernNpcMeleeTest.Rolls(9,99)));
        for(long invalid:new long[]{-1,10,Long.MAX_VALUE})failure(ModernMagicCombat.Failure.INVALID_RANDOM,()->ModernNpcMagic.againstNpc(caster(),c,npc,b->invalid));
        failure(ModernMagicCombat.Failure.INVALID_RANDOM,()->ModernNpcMagic.againstNpc(caster(),c,npc,new ModernNpcMeleeTest.Rolls(0,100)));
        assertEquals(280,npc.hp());
    }
    @Test void unsupportedBehaviorsAndNegativeMrAreRejectedWithoutDiceOrDefaults() {
        for(var fields:List.of(Map.of(BOSS,1L),Map.of(POISON,1L),Map.of(TRANSFORM_ID,1L))) {
            var c=ModernNpcMeleeTest.catalog(fields,true,0);
            failure(ModernMagicCombat.Failure.UNSUPPORTED,()->ModernNpcMagic.againstNpc(caster(),c,ModernNpcMeleeTest.actor(c),b->{fail("unqualified NPC drew RNG");return 0;}));
        }
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,-1L),true,0);
        failure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernNpcMagic.againstNpc(caster(),c,ModernNpcMeleeTest.actor(c),b->{fail("negative MR cannot default to zero");return 0;}));
    }
    @Test void customCasterOrEquipmentProcCannotGainNpcMagicPermission() {
        var raw=ModernPhysicalCombatTest.source(7,0,1,List.of(simpleArmor(2,2,Ability.zero())));
        var proc=new ModernPhysicalCombat.Source(raw.input(),raw.growth(),raw.templates(),Set.of(2L),ModernMagicCombatTest.growth(7));
        var pc=ModernPhysicalCombatTest.fighter(proc).magic();assertFalse(pc.supported());
        var c=ModernNpcMeleeTest.catalog();
        failure(ModernMagicCombat.Failure.UNSUPPORTED,()->ModernNpcMagic.againstNpc(pc,c,ModernNpcMeleeTest.actor(c),b->{fail("unsupported caster drew RNG");return 0;}));
    }
    @Test void sameNpcIdInDifferentCatalogOrChangedTemplateCannotAlias() {
        var c=ModernNpcMeleeTest.catalog();var before=ModernNpcMeleeTest.actor(c);var row=c.npcs().get(ModernNpcMeleeTest.NPC);
        var wrong=new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(89,"npc_other",row.key().npcId()),row.name(),row.nameId(),row.implementation(),row.large(),row.facts(),false);
        var other=new ModernNpcCatalog.Catalog(89,"npc_other",Map.of(row.key().npcId(),wrong),
                new ModernPhysicalCombat.Growth(89,"npc_other",c.growth().strength(),c.growth().dexterity()));
        failure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernNpcMagic.againstNpc(caster(),other,before,b->0));
        var changed=ModernNpcMeleeTest.catalog(Map.of(HP,281L),true,0);
        failure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernNpcMagic.againstNpc(caster(),changed,before,b->0));
    }
}
