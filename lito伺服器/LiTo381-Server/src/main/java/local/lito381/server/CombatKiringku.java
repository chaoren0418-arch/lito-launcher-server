package local.lito381.server;
import java.util.concurrent.ThreadLocalRandom;

/** Current source Kiringku arithmetic; defense projections contain values only. */
final class CombatKiringku {
    static final String MR_KEY="MR_REDUCTION_PER_10";
    private static final String P="local/lito381/server/";
    record Defense(int reduction,boolean player) {}
    static boolean enabled(SourceWeaponPhysicalStats weapon){return CombatRules.loaded()&&weapon.present()&&weapon.weaponType()==17;}
    static boolean hit(boolean previous,SourceWeaponPhysicalStats weapon){return enabled(weapon)||previous;}
    static Defense player(BattlePhysicalDefense.PlayerOutcome outcome,int ac,SourceWeaponPhysicalStats weapon) {
        return enabled(weapon)?new Defense(outcome.acReduction()+outcome.effectiveArmorReduction()+outcome.directionalReduction()+CombatRules.extraAcReduction(ac),true):null;
    }
    static Defense npc(BattlePhysicalDefense.NpcOutcome outcome,SourceWeaponPhysicalStats weapon){return enabled(weapon)?new Defense(outcome.totalReduction(),false):null;}
    static int magicBonus(int intelligence){return intelligence>43?intelligence-28:BattleTurnUndead.magicBonus(intelligence);}
    static double prefix(int intelligence,int spellPower,int magicHit,int weaponRoll,int enchant,int mr,boolean player,double perTen) {
        int value=(int)((intelligence+spellPower+magicHit)*1.8);value+=weaponRoll;value+=enchant;
        double damage=value*0.9;
        if(player)damage=Math.max(0,damage*(1-Math.min(1.0,(mr/10.0)*perTen/100.0)));
        return damage;
    }
    static int rawMr(ClientSession target,long now) {
        var base=(SourceBaseMagicResistance)CombatAccess.field(target,"sourceBaseMagicResistance");
        var equipment=equipment(target);var dolls=(NativeMagicDollPowers)CombatAccess.call(target,"activeMagicDollPowers","()L"+P+"NativeMagicDollPowers;");
        int wis=CombatAccess.integerCall(target,"effectiveWisdom","(J)I",now);
        return Math.max(0,base.classBase()+base.levelBonus()+base.originalBonus()+BattleMagicResistance.wisdomMagicResistance(wis)
                +equipment.totalDirectMagicResistance()+dolls.value("PASSIVE_MR")+CombatAccess.integerCall(target,"magicResistanceModifier","(J)I",now));
    }
    private static SourceEquipmentMagicStats equipment(ClientSession actor){return (SourceEquipmentMagicStats)CombatAccess.call(actor,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");}
    static int finish(int legacy,ClientSession caster,ClientSession target,SourceWeaponPhysicalStats weapon,
                      BattleWeaponCritical.DamageOutcome critical,Defense defense,boolean hit,boolean armorBreak) {
        return finish(legacy,caster,target,weapon,critical,defense,hit,armorBreak,1.0);
    }
    static int finish(int legacy,ClientSession caster,ClientSession target,SourceWeaponPhysicalStats weapon,
                      BattleWeaponCritical.DamageOutcome critical,Defense defense,boolean hit,boolean armorBreak,double companionMultiplier) {
        if(!enabled(weapon))return CombatClassDamage.physical(legacy,caster,target);
        if(!hit)return 0;
        long now=System.nanoTime();var equipment=equipment(caster);
        var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");
        int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","(J)I",now));
        int spellPower=CombatRules.magicLevel(summary.type(),summary.level())+magicBonus(intelligence)+equipment.itemSpellPower()+CombatAccess.integerCall(caster,"spellPowerModifier","(J)I",now);
        var eye=(BattleDragonEyeStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
        int magicHit=equipment.totalMagicHit()+eye.magicHit();
        if(weapon.damageSmall()<1)throw new IllegalArgumentException("source Kiringku small-weapon die must be positive");
        int roll=ThreadLocalRandom.current().nextInt(1,weapon.damageSmall()+1);
        double damage=prefix(intelligence,spellPower,magicHit,roll,weapon.enchantLevel(),target==null?0:rawMr(target,now),target!=null,CombatRules.number(MR_KEY,3));
        var cooking=(BattleCookingStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"cookingStatus"),"active","(J)L"+P+"BattleCookingStatus$Snapshot;",now);
        // Current L1Weapon.get_add_dmg min/max remain zero: no source setter caller exists.
        damage+=BattleKiringkuDamage.classAttackLevel(summary.type(),summary.level());damage+=cooking.physicalDamage(false);damage*=critical.multiplier();
        if(defense!=null)damage-=defense.reduction();
        if(target!=null) {
            var targetCooking=(BattleCookingStatus.Snapshot)CombatAccess.call(CombatAccess.field(target,"cookingStatus"),"active","(J)L"+P+"BattleCookingStatus$Snapshot;",now);
            damage-=targetCooking.damageReduction();var abilities=(BattleAbilityStatus)CombatAccess.field(target,"abilityStatus");
            if(abilities.reductionArmor(now).remainingSeconds()!=0)damage-=BattleSpellCatalog.reductionArmorSpell(BattleSpellCatalog.REDUCTION_ARMOR).reduction(CombatAccess.integerCall(target,"battleLevel","()I"),true);
            boolean world=((Enum<?>)CombatAccess.field(target,"phase")).name().equals("WORLD");
            boolean broken=world?(boolean)CombatAccess.call(target,"classicWorldArmorBreakActive","(J)Z",now):armorBreak;
            if(broken)damage*=1.58;
            var skin=(BattleDragonSkinStatus.Snapshot)CombatAccess.call(CombatAccess.field(target,"dragonSkinStatus"),"snapshot","(J)L"+P+"BattleDragonSkinStatus$Snapshot;",now);
            if(skin.active())damage-=skin.damageReduction();damage-=CombatAccess.integerCall(target,"effectivePatiencePhysicalReduction","(J)I",now);
            if(abilities.immuneToHarm(now).divisor()!=0)damage/=CombatRules.number("IMMUNE_TO_HARM",2);
        } else {damage*=companionMultiplier;}
        String key=CombatClassDamage.key(summary.type(),target!=null);if(key!=null)damage*=CombatRules.number(key,1);
        return Math.max(0,(int)damage);
    }
}
