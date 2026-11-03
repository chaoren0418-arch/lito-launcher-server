package local.lito381.server;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Actual candidate consumers under altered immutable inputs; no SQL modifications. */
public class CombatRegression {
    static int checks;
    static void check(boolean ok, String name) {
        checks++; if (!ok) throw new AssertionError(name);
    }
    static Field current(Class<?> type) throws Exception {
        Field f = type.getDeclaredField("current"); f.setAccessible(true); return f;
    }
    public static void main(String[] args) throws Exception {
        ResourceDatabase.load(ServerConfig.load(Path.of(args[0]), "standard"));
        Field rf=current(CombatRules.class), df=current(CombatDatabase.class);
        var rules=(CombatRules.Snapshot)rf.get(null);
        var data=(CombatDatabase.Snapshot)df.get(null);
        check(rules.values().size()==CombatRules.SKILL_KEYS.size()+CombatRules.DAMAGE_KEYS.size()+5,"installed source and bot controls");
        check(CombatRules.extraAcReduction(-100)==10,"source AC setting");
        check(CombatRules.extraAcReduction(0)==0 && CombatRules.extraAcReduction(10)==0,"AC only negative");
        var defense=BattlePhysicalDefense.againstPlayer(500,1,-100,0,20,0,5,3);
        check(defense.damage()==500-defense.acReduction()-defense.effectiveArmorReduction()
                -defense.directionalReduction()-10,"source defense order, ignore does not cancel AC");
        check(BattlePhysicalDefense.againstPlayer(1,1,-100,0,0,0,0,0).damage()==0,"floor zero");
        var blessing=BattleSpellCatalog.naturesBlessingSpell(164);
        check(blessing.applyLawfulRevision(10,0)==10,"Nature lawful result is pure roll");
        check(blessing.applyLawfulRevision(100,32767)==199,"Nature lawful result excludes configured extra");
        check(CombatHealing.blessingPlayer(BattleHealingRecovery.adjustedAmount(10*2,false,false,false))==20,"Nature actual source field remains0 after Water Life doubling");
        check(CombatHealing.blessingPlayer(BattleHealingRecovery.adjustedAmount(15,true,false,false))==8,"Nature actual source field remains0 after signed odd halving");
        var protection=new BattleAbilityStatus.Protection(2,30);
        check(protection.applyTo(101)==50,"default protection truncation");
        check(new BattleAbilityStatus.Protection(0,0).applyTo(101)==101,"expired protection inactive");
        check(BattleSpellCatalog.shockStunSpell(87).randomStunSeconds()==3,"source shock random timer");
        check(BattleSpellCatalog.shockStunSpell(87).rollStunSeconds(sides->sides)==5,"source shock inclusive upper bound");
        Map<String,Double> changed=new HashMap<>(rules.values());
        changed.put("NATURES_BLESSING_HEAL",7.0); changed.put("AC_REDUCTION_DIVISOR",5.0);
        changed.put("IMMUNE_TO_HARM",2.5); changed.put("BURNING_CHANCE",100.0);
        changed.put("BURNING_DMG",2.25); changed.put("DOUBLE_BREAK_CHANCE",0.0);
        changed.put("ELEMENTAL_FIRE_RND",100.0); changed.put("ELEMENTAL_FIRE",1.75);
        changed.put("SOUL_OF_FLAME_DAMAGE",2.75); changed.put("STRIKER_DMG2",1.8);
        changed.put("PET_TO_PC_DAMAGE_MULTIPLIER",0.6);
        changed.put("JOY_OF_PAIN_COUNTDMG",2.0);changed.put("JOY_OF_PAIN_DMG",17.0);
        changed.put("COUNTER_BARRIER_DMG",1.75);changed.put("BLOODY_SOULADDMP",37.0);
        changed.put("DISINTEGRATE_CRITICAL_RND",67.0);changed.put("CRITICAL_SPR",11749.0);
        changed.put("CURSE_READY_TIME",3.0);changed.put("CURSE_TIME",7.0);
        changed.put("SHOCK_STUN_TIMER.base",3.0);changed.put("SHOCK_STUN_TIMER.random",1.0);
        changed.put("SLAY_BREAK_TIMER.base",4.0);changed.put("SLAY_BREAK_TIMER.random",3.0);
        changed.put("MORTAL_BODY_DMG",7.0);
        try {
            rf.set(null,new CombatRules.Snapshot(changed,rules.skillFile(),rules.damageFile()));
            check(CombatHealing.blessingPlayer(blessing.applyLawfulRevision(10,0))==17,"altered actual blessing consumer after target modifiers");
            check(BattlePhysicalDefense.againstPlayer(500,1,-100,0,20,0,5,3).damage()
                    ==defense.damage()-10,"altered actual defense consumer");
            check(protection.applyTo(101)==40,"fractional divisor remains fractional");
            check(BattlePhysicalProcBuff.burningSpirit(true,true,100).multiplier()==2.25,"changed burning proc");
            check(!BattlePhysicalProcBuff.doubleBrake(true,11,true,1).triggered(),"zero double brake probability");
            check(BattlePhysicalProcBuff.elementalFire(true,true,100).multiplier()==1.75,"changed elemental proc");
            check(BattlePhysicalProcBuff.soulOfFlameMultiplier(true)==2.75,"changed flame multiplier");
            check(BattleStrikerGale.damageAfterStatus(100,true,true)==180,"changed striker consumer");
            check(BattleJoyOfPain.feedbackDamage(1000,900,0)==17,"changed joy divisor/cap");
            check(BattleJoyOfPain.feedbackDamage(1000,970,0)==15,"changed joy divisor below cap");
            check(BattleSpellCatalog.counterBarrierSpell(91).damageMultiplier()==1.75,"changed barrier multiplier");
            check(BattleSpellCatalog.bodyToMindSpell(146).manaGain()==37,"changed bloody soul");
            var mortal=BattleSpellCatalog.mortalBodySpell(191);
            check(mortal.baseReflectionDamage()==7,"changed mortal body base damage consumer");
            check(BattleMortalBody.decide(mortal,true,50,50,false,1,false,0,0,0,0).reflectedDamage()==7,"changed mortal body actual feedback consumer");
            check(BattleSpellCatalog.attackSpell(77).specialCriticalChancePercent()==67,"changed critical chance");
            check(BattleSpellCatalog.attackSpell(77).specialCriticalGfx()==11749,"changed critical gfx");
            check(BattleSpellCatalog.paralysisSpell(33).warningDelayMillis()==3000
                    && BattleSpellCatalog.paralysisSpell(33).paralysisDurationMillis()==7000,"changed mummy timing");
            check(BattleSpellCatalog.shockStunSpell(87).rollStunSeconds(sides->sides)==4,"changed actual shock random consumer");
            check(BattleSpellCatalog.thunderGrabSpell(192).minimumBindSeconds()==4
                    && BattleSpellCatalog.thunderGrabSpell(192).maximumBindSeconds()==7,"changed nested thunder timers");
            check(BattleSpellCatalog.thunderGrabSpell(192).bindDurationSeconds(3)==7,"changed nested thunder duration consumer");
            var pet=ClientSession.class.getDeclaredMethod("companionPhysicalDamage",
                    int.class,int.class,int.class,int.class,int.class,int.class,int.class);
            pet.setAccessible(true);
            check((int)pet.invoke(null,20,18,1,10,10,99,0)==16,"changed actual pet damage consumer");
            changed.put("AC_REDUCTION_DIVISOR",0.0);
            rf.set(null,new CombatRules.Snapshot(changed,rules.skillFile(),rules.damageFile()));
            check(CombatRules.extraAcReduction(-100)==0,"AC switch off");
            check(CombatRules.extraAcReduction(Integer.MIN_VALUE)==0,"safe negative int boundary");
            changed.put("MORTAL_BODY_DMG",0.0);rf.set(null,new CombatRules.Snapshot(changed,rules.skillFile(),rules.damageFile()));
            var zero=BattleMortalBody.decide(mortal,true,50,50,false,1,false,0,0,0,0);
            check(zero.baseDamage()==0&&zero.reflectedDamage()==1,"zero source mortal base retains one damage floor without record rejection");
            var altered=new HashMap<>(data.skills());
            var original=altered.get(1); altered.put(1,original.withBasics(9,0,111,0));
            df.set(null,data.withSkills(altered));
            var heal=BattleSpellCatalog.healingSpell(1);
            check(heal.mpCost()==9&&heal.reuseDelayMillis()==111,"altered SQL basic getters");
            check(heal.effectiveMpCost(8,0,false)==9,"altered SQL internal GETFIELD consumer");
            altered.put(1,new CombatDatabase.Skill(9,0,111,0,3,6,0,0,0));
            df.set(null,data.withSkills(altered));
            check(heal.diceCount(11)==3&&heal.dieSides()==6,"changed healing dice SQL binding");
            var group=altered.get(BattleSpellCatalog.HEAL_ALL);
            altered.put(BattleSpellCatalog.HEAL_ALL,new CombatDatabase.Skill(group.mp(),group.hp(),group.reuse(),group.duration(),3,6,0,0,0));
            df.set(null,data.withSkills(altered));
            check(BattleSpellCatalog.groupHealingSpell(BattleSpellCatalog.HEAL_ALL).rollHealing(11,sides->sides)==18,"changed group healing dice actual consumer");
            var arrow=altered.get(4);
            altered.put(4,new CombatDatabase.Skill(arrow.mp(),arrow.hp(),arrow.reuse(),arrow.duration(),20,6,2,0,0));
            var reagent=altered.get(68);
            altered.put(68,new CombatDatabase.Skill(reagent.mp(),reagent.hp(),reagent.reuse(),reagent.duration(),
                    reagent.damage(),reagent.die(),reagent.dice(),40321,3));
            df.set(null,data.withSkills(altered));
            check(BattleSpellCatalog.attackSpell(4).baseDamage()==20
                    &&BattleSpellCatalog.attackSpell(4).diceCount()==2
                    &&BattleSpellCatalog.attackSpell(4).dieSides()==6,"changed attack dice SQL binding");
            check(BattleSpellCatalog.damageProtectionSpell(68).itemConsumeId()==40321
                    &&BattleSpellCatalog.damageProtectionSpell(68).itemConsumeCount()==3,"changed material SQL binding");
            altered.put(11,altered.get(11).withProbability(3,0));df.set(null,data.withSkills(altered));
            check(BattleSpellCatalog.poisonSpell(11).probabilityDice()==3,"changed legacy probability die SQL binding");
            check(BattleSpellCatalog.poisonSpell(11).probability(3,50,11,0,0,0,false,sides->sides)==33,
                    "changed legacy probability die actual consumer with source wizard type");
            check(BattleSpellCatalog.boneBreakSpell(208).durationSeconds()==2,"source executor exception");
            altered.put(1,original);
        } finally { rf.set(null,rules); df.set(null,data); }
        Properties skill=CombatRules.read(rules.skillFile()), damage=CombatRules.read(rules.damageFile());
        check(rules.defaultedKeys().equals(java.util.Set.of("MORTAL_BODY_ROM","NATURES_BLESSING_HEAL"))&&rules.values().get("MORTAL_BODY_ROM")==23.0,
                "absent mortal base chance uses documented source ConfigSkill default23");
        var explicit=new Properties();explicit.putAll(skill);explicit.setProperty("MORTAL_BODY_ROM","65");
        var explicitRules=CombatRules.snapshot(explicit,damage,rules.skillFile(),rules.damageFile());
        check(explicitRules.values().get("MORTAL_BODY_ROM")==65.0&&explicitRules.defaultedKeys().equals(java.util.Set.of("NATURES_BLESSING_HEAL")),"explicit mortal chance overrides source default");
        for (String bad : new String[]{"NaN","Infinity","-1","101","1.2","garbage"}) {
            var copy=new Properties();copy.putAll(skill);copy.setProperty("BURNING_CHANCE",bad);
            boolean rejected=false;
            try { CombatRules.snapshot(copy,damage,rules.skillFile(),rules.damageFile()); }
            catch(java.io.IOException expected) { rejected=true; }
            check(rejected,"invalid probability rejected "+bad);
        }
        boolean immutable=false;
        try { data.skills().clear(); } catch(UnsupportedOperationException expected) { immutable=true; }
        check(immutable,"immutable SQL snapshot");
        System.out.println("PASS combat consumer differential checks="+checks+"; SQL SELECT only; no deployment changes");
    }
}
