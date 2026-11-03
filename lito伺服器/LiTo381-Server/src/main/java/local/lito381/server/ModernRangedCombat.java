package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import java.util.Objects;

/**
 * NEW_MATCH_BOW_V1: the LiTo ordinary PC bow pipeline, not current classic's
 * DEX*2.5/original-stat policy. No weapon die, original damage or class damage.
 * RNG remains server-owned; even a MISS rolls arrow maximum/die/critical before
 * the engine atomically debits one arrow. No inventory, HP, clock or I/O here.
 */
final class ModernRangedCombat {
    static final String RULESET="NEW_MATCH_BOW_V1";
    enum Unsupported { NONE, NOT_BOW, UNQUALIFIED, MAGIC_BOW, ATTRIBUTE, RANGE, CUSTOM_EXECUTOR, EQUIPMENT_PROC, LIFETIME, PROJECTILE_SPRITE, APPEARANCE }
    record Offense(long totalHit,long damageBonus,long ignoreReduction) {}
    record Values(boolean supported,boolean defenseSupported,long maximumHp,long range,long evasion,
                  Offense offense,ModernPhysicalCombat.Defense defense,ModernPhysicalCombat.Defense meleeDefense,
                  ModernCombatProfile.WeaponFacts weapon,ModernPhysicalCombat.Rational multiplier) {
        Values {Objects.requireNonNull(offense);Objects.requireNonNull(defense);Objects.requireNonNull(meleeDefense);Objects.requireNonNull(multiplier);
            if(maximumHp<1||range<1||supported&&(weapon==null||weapon.retainedType()!=4&&weapon.retainedType()!=13))throw bad();}
    }
    record Fighter(ModernCombatProfile profile,Unsupported unsupported,Values values) {
        Fighter {Objects.requireNonNull(profile);Objects.requireNonNull(unsupported);Objects.requireNonNull(values);if(values.supported()!=(unsupported==Unsupported.NONE))throw bad();}
        boolean supported(){return values.supported();}boolean defenseSupported(){return values.defenseSupported();}long range(){return values.range();}
    }
    private ModernRangedCombat(){}
    static Fighter qualify(ModernPhysicalCombat.Source source,ModernCombatProfile profile,ModernPhysicalCombat.Defense meleeDefense,long now) {
        return qualify(source,profile,meleeDefense,now,null);
    }
    static Fighter qualify(ModernPhysicalCombat.Source source,ModernCombatProfile profile,ModernPhysicalCombat.Defense meleeDefense,long now,ModernArmorProtectionLoadout protection) {
        return qualify(source,profile,meleeDefense,now,protection,null);
    }
    static Fighter qualify(ModernPhysicalCombat.Source source,ModernCombatProfile profile,ModernPhysicalCombat.Defense meleeDefense,long now,ModernArmorProtectionLoadout protection,ModernVipCombatProfile vip) {
        if(vip!=null)vip.require(source,profile);
        if((profile.armorPolicy()==ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP)!=(vip!=null))throw bad();
        if(protection!=null)protection.require(source,profile);
        if(source.magicBows()==null)return null;
        if(!source.input().equipment().source().equals(profile.source()))throw bad();
        try {
            var row=profile.source().character();var e=profile.equipmentEvaluation();var w=profile.weapon();var facts=w.facts();
            Unsupported common=Unsupported.NONE;
            for(var flag:profile.equipped()) {
                var item=source.input().equipment().items().get(flag.objectId()).source();if(!flag.equipped()&&!item.equipped())continue;
                var life=source.input().equipment().lifetimes().view(item.objectId(),now);
                if(!life.usable()&&life.state()!=ModernItemLifetime.State.EXPIRED)common=Unsupported.LIFETIME;
                if((vip==null||!vip.permits(item,flag.equipped()))&&ModernVipEquipment.unresolvedCustom(source,item,flag.equipped()))common=Unsupported.CUSTOM_EXECUTOR;
                if(source.procCatalogIds().contains(item.template().catalogId())&&(protection==null||!protection.permits(item.objectId())))common=Unsupported.EQUIPMENT_PROC;
            }
            if(w.state()==ModernCombatProfile.WeaponState.UNQUALIFIED)common=Unsupported.UNQUALIFIED;
            boolean defenseSupported=common==Unsupported.NONE;
            Unsupported reason=common;
            if(reason==Unsupported.NONE) {
                if(facts==null||facts.retainedType()!=4&&facts.retainedType()!=13)reason=Unsupported.NOT_BOW;
                else if(source.weaponArrowSprites()==null)reason=Unsupported.UNQUALIFIED;
                else if(source.magicBows().get(facts.catalogId())!=0)reason=Unsupported.MAGIC_BOW;
                else if(source.weaponArrowSprites().get(facts.catalogId())!=-1&&source.weaponArrowSprites().get(facts.catalogId())!=66)reason=Unsupported.PROJECTILE_SPRITE;
                else if(row.characterClass()==5)reason=Unsupported.APPEARANCE;
                else if(facts.attributeKind()!=0)reason=Unsupported.ATTRIBUTE;
                else if(facts.range()<0||facts.range()>16)reason=Unsupported.RANGE;
            }
            var growth=source.growth().at(true,profile.effective().dexterity());
            long hit=add(row.level(),row.level()/HIT_DIVISORS[row.characterClass()],growth.hit(),e.total(RANGED_HIT),
                    e.armorTemplatePhysical().rangedHit(),e.weaponTemplatePhysical().meleeHit(),facts==null?0:threeFifths(w.enchantment()));
            long damage=add(growth.damage(),e.total(RANGED_DAMAGE),e.armorTemplatePhysical().rangedDamage(),
                    e.weaponTemplatePhysical().meleeDamage(),facts==null?0:w.enchantment());
            var defense=new ModernPhysicalCombat.Defense(meleeDefense.armorClass(),meleeDefense.hitDefenseMaximum(),meleeDefense.acMaximum(),
                    meleeDefense.armorReduction(),Math.max(0,e.total(RANGED_REDUCTION)),meleeDefense.pvpReduction());
            long er=evasion(row.characterClass(),row.level(),profile.effective().dexterity(),profile.source().persisted().originalDexterity(),e.total(EVASION));
            var multiplier=facts==null?new ModernPhysicalCombat.Rational(1,1):source.templates().get(facts.catalogId()).criticalMultiplier();
            return new Fighter(profile,reason,new Values(reason==Unsupported.NONE,defenseSupported,profile.effective().maximumHp(),
                    facts==null?1:Math.max(1,facts.range()),er,new Offense(hit,damage,e.total(IGNORE_REDUCTION)),defense,meleeDefense,facts,multiplier));
        }catch(ArithmeticException failure){throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW);}
    }
    static ModernPhysicalCombat.Outcome resolve(Values attacker,Values defender,ModernAmmo.Facts arrow,long hp,
                                               ModernPhysicalCombat.BoundedRandom random,int penalty) {
        return resolveProtected(attacker,defender,arrow,hp,random,penalty,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<ModernPhysicalCombat.Outcome> resolveProtected(Values attacker,Values defender,
            ModernAmmo.Facts arrow,long hp,ModernPhysicalCombat.BoundedRandom random,int penalty,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(arrow);Objects.requireNonNull(random);
        Objects.requireNonNull(protection);
        if(!attacker.supported()||!defender.defenseSupported())throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
        if(hp<1||hp>defender.maximumHp()||penalty!=0&&penalty!=1&&penalty!=3&&penalty!=5)throw bad();
        try {
            var d=defender.defense();long rate=Math.subtractExact(attacker.offense().totalHit(),penalty);
            long attack=add(draw(random,20),2,rate,-10);
            long defend=d.armorClass()>=0?Math.subtractExact(10,d.armorClass()):add(10,one(random,d.hitDefenseMaximum()));
            int chance=attack<=Math.subtractExact(rate,9)?15:attack>=Math.addExact(rate,10)?100:attack>defend?100:15;
            long first=one(random,100);boolean hit=first<=chance;
            if(hit&&chance>first)hit=defender.evasion()<one(random,100);
            var weapon=attacker.weapon();
            boolean maximum=weapon.maximumDamageChance()>0&&one(random,100)<=weapon.maximumDamageChance();
            long die=maximum?arrow.small():one(random,arrow.small());
            long raw=Math.max(0,add(die,attacker.offense().damageBonus()));
            boolean critical=weapon.criticalChance()>0&&one(random,100)<=weapon.criticalChance();
            if(critical)raw=attacker.multiplier().applyAtLeastOne(raw);
            if(!hit)return new ModernIncomingDamage.Resolved<>(new ModernPhysicalCombat.Outcome(false,0,0,hp,false,false,false),protection.apply(0,hp));
            long ac=d.acMaximum()==0?0:add(draw(random,d.acMaximum()),Math.max(1,d.acMaximum()>>3));
            long armor=d.armorReduction()<=10?d.armorReduction():add(11,draw(random,Math.subtractExact(d.armorReduction(),10)));
            long reduction=add(ac,Math.max(0,Math.subtractExact(armor,attacker.offense().ignoreReduction())),d.directionalReduction(),d.pvpReduction());
            long damage=Math.max(0,Math.subtractExact(raw,reduction));var impact=protection.apply(damage,hp);
            return new ModernIncomingDamage.Resolved<>(new ModernPhysicalCombat.Outcome(true,raw,impact.applied(),impact.remaining(),impact.death(),maximum,critical),impact);
        }catch(ArithmeticException failure){throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.ARITHMETIC_OVERFLOW);}
    }
    static long evasion(int type,long level,long dex,long original,long equipment) {
        if(type<0||type>6||level<1||dex<0||original<0)throw bad();
        long classLevel=level/ER_DIVISORS[type];long correction=switch(type) {
            case 0->original==18?3:original>=16?2:original>=14?1:0;
            case 1->original==16?3:original>=14&&original<=15?1:0;
            case 2->0;case 3->original==11?2:original>=9&&original<=10?1:0;
            case 4->original>=16?2:0;case 5->original>=15?2:original>=13?1:0;
            case 6->original>=14?2:original>=12?1:0;default->throw bad();};
        return add(classLevel,Math.subtractExact(dex,8)>>1,correction,equipment);
    }
    private static final int[] HIT_DIVISORS={15,10,30,40,30,25,40},ER_DIVISORS={8,4,8,10,6,7,9};
    private static long draw(ModernPhysicalCombat.BoundedRandom random,long bound){if(bound<1)throw randomBad();long v=random.nextLong(bound);if(v<0||v>=bound)throw randomBad();return v;}
    private static long one(ModernPhysicalCombat.BoundedRandom random,long bound){return Math.incrementExact(draw(random,bound));}
    private static long add(long... values){long sum=0;for(long v:values)sum=Math.addExact(sum,v);return sum;}
    private static long threeFifths(long v){return Math.addExact(Math.multiplyExact(v/5,3),Math.multiplyExact(v%5,3)/5);}
    private static ModernPhysicalCombat.CombatException randomBad(){return new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.INVALID_RANDOM);}
    private static ModernPhysicalCombat.CombatException bad(){return new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.INVALID_STATE);}
}
