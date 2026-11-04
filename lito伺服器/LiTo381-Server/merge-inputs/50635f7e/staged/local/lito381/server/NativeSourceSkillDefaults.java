package local.lito381.server;
import java.io.IOException;import java.nio.file.Path;import java.util.*;
/** Installed ConfigSkill keys use Source defaults for omissions/blanks before original validation. */
final class NativeSourceSkillDefaults {
 static String value(String key){return switch(key){
  case "ILLUSION_AVATAR_DAMAGE_RATE","COUNTER_BARRIER_DMG","SOUL_OF_FLAME_DAMAGE","ELEMENTAL_FIRE","BURNING_DMG","DOUBLE_BREAK_DMG"->"1.5";
  case "JOY_OF_PAIN_COUNTDMG"->"3";case "JOY_OF_PAIN_DMG"->"1500";
  case "SHOCK_STUN_TIMER"->"2,2";case "SOLID_CARRIAGE_MODE"->"1";
  case "DISINTEGRATE_CRITICAL_RND"->"40";case "CRITICAL_SPR"->"11748";
  case "CURSE_READY_TIME"->"5";case "CURSE_TIME"->"10";case "IMMUNE_TO_HARM"->"2.0";
  case "INVIS_CAST_BREAK"->"false";case "Triple_Arrow_Dmg"->"1.0";case "STRIKER_DMG2"->"1.2";case "BLOODY_SOULADDMP"->"12";
  case "ELEMENTAL_FIRE_RND","BURNING_CHANCE"->"33";case "DOUBLE_BREAK_CHANCE"->"15";
  case "FOE_SLAYER_DMG","VULNERABILITY_1"->"10";case "VULNERABILITY_ROM"->"90";case "VULNERABILITY_2"->"20";case "VULNERABILITY_3"->"30";
  case "SLAY_BREAK_TIMER"->"4,0";case "SLAY_BREAK_NOT_TELEPORT"->"true";case "MORTAL_BODY_ROM"->"23";case "MORTAL_BODY_DMG"->"30";
  default->null;
 };}
 static CombatRules.Snapshot snapshot(Properties skill,Properties damage,Path skillFile,Path damageFile)throws IOException{
  Properties patched=null;var defaults=new HashSet<String>();
  for(var key:CombatRules.SKILL_KEYS){String fallback=value(key);if(fallback==null)continue;String raw=skill.getProperty(key);if(raw!=null&&!raw.isBlank())continue;
   if(patched==null){patched=new Properties(skill);patched.putAll(skill);}patched.setProperty(key,fallback);defaults.add(key);
  }
  var original=CombatRules.nativeSourceSkillDefaultsOriginalSnapshot(patched==null?skill:patched,damage,skillFile,damageFile);
  if(defaults.isEmpty())return original;defaults.addAll(original.defaultedKeys());
  return new CombatRules.Snapshot(original.values(),original.skillFile(),original.damageFile(),original.characterFile(),defaults,original.mapDirectory());
 }
 private NativeSourceSkillDefaults(){}
}
