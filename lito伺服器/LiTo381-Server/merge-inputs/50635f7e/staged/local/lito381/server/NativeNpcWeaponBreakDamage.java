package local.lito381.server;
/** Retained npcDmgMode ordering, including the post-leverage BOSS fraction. */
final class NativeNpcWeaponBreakDamage {
 static double sourceExact(int level,int strength,int strengthDamage,int levelRoll,int criticalRoll,boolean undeadNight,boolean boss){return sourceExact(level,strength,strengthDamage,levelRoll,criticalRoll,undeadNight,boss,10);}
 static double sourceExact(int level,int strength,int strengthDamage,int levelRoll,int criticalRoll,boolean undeadNight,boolean boss,int leverage){
  if(level<=0||strength<0||levelRoll<0||levelRoll>=level||criticalRoll<0||criticalRoll>=100||leverage<=0)throw new IllegalArgumentException("invalid Source NPC damage prefix");
  double damage=levelRoll+strength*0.8+strengthDamage;
  if(criticalRoll<15)damage*=1.80;
  if(undeadNight)damage*=1.20;
  damage=(int)(damage*(leverage/10D));
  if(boss)damage*=1.80;
  return damage;
 }
 static int afterStacks(double damage,int stacks){
  if(!Double.isFinite(damage)||stacks<0||stacks>BattleWeaponBreak.MAX_STACKS)throw new IllegalArgumentException("invalid Source NPC Weapon Break input");
  if(stacks>0){damage=damage*(100-stacks*5)/100.0;if(damage<1)damage=1;}
  return Math.max(0,(int)damage);
 }
 private NativeNpcWeaponBreakDamage(){}
}
