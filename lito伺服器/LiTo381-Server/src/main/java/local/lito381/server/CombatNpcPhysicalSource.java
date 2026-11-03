package local.lito381.server;

/** Current source L1AttackNpc arithmetic; no retained actors or mutable tables. */
final class CombatNpcPhysicalSource {
    static boolean night(long millis) {
        long elapsed=millis-1_735_689_600_000L;
        if(elapsed<0)throw new IllegalArgumentException("time predates source game epoch");
        int seconds=(int)(elapsed*6/1000L);
        seconds-=seconds%3;
        int hour=Math.floorMod(seconds,86_400)/3_600;
        return hour<6||hour>17;
    }
    static int strengthDamage(int strength) {
        if (strength < 0 || strength > 254)
            throw new IllegalArgumentException("source NPC STRD has entries 0..254");
        if(strength<=22)return -6+(strength+1)/2;
        int damage=5+(Math.min(strength,28)-23)/3+1;
        if(strength>=29)damage+=(Math.min(strength,32)-29)/2+1;
        if(strength>=33)damage+=Math.min(strength,34)-32;
        if(strength>=35)damage+=(strength-33)/4;
        return damage;
    }
    static double base(int level,int strength,int levelRoll,boolean pet,int petWeaponDamage) {
        if(level<=0||levelRoll<0||levelRoll>=level)
            throw new IllegalArgumentException("invalid source NPC level roll");
        double damage=levelRoll+strength*.8+strengthDamage(strength);
        if(pet){damage+=level/7;damage+=petWeaponDamage;}
        return damage;
    }
    static double mode(double damage,int criticalRoll,int damageUp,boolean undeadNight,
                       boolean boss,int leverage,int weaponBreakStacks) {
        if(criticalRoll<0||criticalRoll>=100||weaponBreakStacks<0||weaponBreakStacks>10)
            throw new IllegalArgumentException("invalid source NPC damage mode");
        if(criticalRoll<15)damage*=1.80;
        damage+=damageUp;
        if(undeadNight)damage*=1.20;
        damage=(int)(damage*(leverage/10D));
        if(boss)damage*=1.80;
        if(weaponBreakStacks>0) {
            damage=damage*(100-weaponBreakStacks*5)/100.0;
            if(damage<1)damage=1;
        }
        return damage;
    }
    static double defense(double damage,int type,int ac,int roll) {
        int max=BattlePhysicalDefense.playerAcMaximum(type,ac);
        if(roll<0||(max==0?roll!=0:roll>=max))
            throw new IllegalArgumentException("invalid source NPC PC defense roll");
        return max==0?damage:damage-roll-Math.max(1,max>>3);
    }
}
