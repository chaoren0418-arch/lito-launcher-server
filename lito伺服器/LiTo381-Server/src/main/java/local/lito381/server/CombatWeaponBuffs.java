package local.lito381.server;

/** Source player-owned weapon/armor BUFFs. No actor or item cache. */
final class CombatWeaponBuffs {
    private static final int[] NEGATIVE_PHYSICAL_SKILLS={47,56};
    private CombatWeaponBuffs() { }
    static CombatBuffLeases leases(ClientSession player) {
        return CombatBuffLeases.owned((BattleAbilityStatus)CombatAccess.field(player,"abilityStatus"));
    }
    static int activateWeapon(BattleWeaponBuffStatus old,long now,int duration,int skill,int hit,int damage,int bowHit,int bowDamage,ClientSession target) {
        return CombatBuffLeases.loaded()?leases(target).put(skill,now,duration):old.activate(now,duration,skill,hit,damage,bowHit,bowDamage);
    }
    static int activateItemWeapon(BattleItemWeaponEnchantStatus old,long now,int duration,int skill,long item,String name,int damage,int bowDamage,ClientSession caster) {
        return CombatBuffLeases.loaded()?leases(caster).put(skill,now,duration):old.activate(now,duration,skill,item,name,damage,bowDamage);
    }
    static int activateArmor(BattleItemArmorEnchantStatus old,long now,int duration,int skill,long item,String name,int ac,ClientSession caster) {
        return CombatBuffLeases.loaded()?leases(caster).put(skill,now,duration):old.activate(now,duration,skill,item,name,ac);
    }
    static BattleWeaponBuffStatus.Modifier weaponModifier(BattleWeaponBuffStatus old,long now,ClientSession caster) {
        if(!CombatBuffLeases.loaded())return old.modifier(now);
        var result=CombatSpecialBuffs.weapon(leases(caster).weapon(now),caster,now);var timed=(BattleTimedStatus)CombatAccess.field(caster,"timedStatus");
        if(!timed.weaknessActive(now)&&!timed.diseaseActive(now))return result;
        int hit=result.meleeHit(),damage=result.meleeDamage();var source=CombatDatabase.controls();
        for(int id:NEGATIVE_PHYSICAL_SKILLS)if(id==47?timed.weaknessActive(now):timed.diseaseActive(now)) {
            hit=Math.addExact(hit,SourceSkillControls.ability(source,id,"HIT"));
            damage=Math.addExact(damage,SourceSkillControls.ability(source,id,"DMG"));
        }
        return new BattleWeaponBuffStatus.Modifier(result.skillId(),hit,damage,result.rangedHit(),result.rangedDamage(),result.remainingSeconds());
    }
    static int legacyWeakness(int old){return CombatBuffLeases.loaded()?0:old;}
    static int diseaseArmor(int old){return CombatBuffLeases.value(old,56,"AC");}
    static BattleWeaponBuffStatus.Modifier autoWeaponModifier(BattleWeaponBuffStatus old,long now,ClientSession caster) {
        var result=weaponModifier(old,now,caster);
        return CombatBuffLeases.loaded()&&leases(caster).contains(8,now)
                ?new BattleWeaponBuffStatus.Modifier(8,result.meleeHit(),result.meleeDamage(),result.rangedHit(),result.rangedDamage(),result.remainingSeconds()):result;
    }
    static BattleItemWeaponEnchantStatus.Modifier itemWeaponModifier(BattleItemWeaponEnchantStatus old,long now,long item,ClientSession caster) {
        return CombatBuffLeases.loaded()?BattleItemWeaponEnchantStatus.Modifier.none(item):old.modifier(now,item);
    }
    static BattleItemArmorEnchantStatus.Modifier itemArmorModifier(BattleItemArmorEnchantStatus old,long now,long item,ClientSession caster) {
        return CombatBuffLeases.loaded()?leases(caster).armorModifier(now,item):old.modifier(now,item);
    }
    static int armor(int old,ClientSession caster,long now) {return CombatBuffLeases.loaded()?leases(caster).armor(now):old;}
    static BattleWeaponBuffStatus.Cancellation keepWeaponBuffs(BattleWeaponBuffStatus old) {
        return CombatBuffLeases.loaded()?new BattleWeaponBuffStatus.Cancellation(0):old.cancelMagicEffects();
    }
    static int keepItemBuff(BattleItemWeaponEnchantStatus old,long item) {return CombatBuffLeases.loaded()?0:old.remove(item);}
    static boolean removeWeaponSkill(BattleWeaponBuffStatus old,int skill,ClientSession caster) {
        return CombatBuffLeases.loaded()?leases(caster).remove(false,skill):old.removeSkill(skill);
    }
    static boolean removeArmorSkill(BattleItemArmorEnchantStatus old,int skill,ClientSession caster) {
        return CombatBuffLeases.loaded()?leases(caster).remove(false,skill):old.removeSkill(skill);
    }
    static boolean equippedWeapon(boolean old,ClientSession caster) {
        return !CombatBuffLeases.loaded()?old:((SourceWeaponPhysicalStats)CombatAccess.call(caster,"activePhysicalWeapon","()Llocal/lito381/server/SourceWeaponPhysicalStats;")).present();
    }
    static long itemObjectId(RuntimeInventory.Item item) {return item==null?0:item.objectId();}
    static String itemLogName(RuntimeInventory.Item item,ClientSession caster) {
        if(!CombatBuffLeases.loaded())return item.nativeLogName();
        var weapon=(SourceWeaponPhysicalStats)CombatAccess.call(caster,"activePhysicalWeapon","()Llocal/lito381/server/SourceWeaponPhysicalStats;");
        return (String)CombatAccess.call(caster,"activeWeaponLogName","(Llocal/lito381/server/SourceWeaponPhysicalStats;)Ljava/lang/String;",weapon);
    }
    static RuntimeInventory.Item bodyArmor(RuntimeInventory inventory,long oldTarget,ClientSession caster) {
        if(!CombatBuffLeases.loaded())return inventory.item(oldTarget);
        return inventory.items().stream().filter(item->item.equipped()&&item.definition().sourceArmor()
                &&item.definition().sourceArmorStatus()!=null&&item.definition().sourceArmorStatus().armorType()==2).findFirst().orElse(null);
    }
}
