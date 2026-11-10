package local.lito381.server;
import java.util.*;
/** Source visual packet planning; no HP mutation or scheduling authority. */
final class NativeSourceDisplayPlan {
 record Effect(long object,int sprite){}
 record Settings(boolean hpbar,boolean numbers,int miss,int critical,List<Integer> physical,List<Integer> magic,boolean kiringku,int kiringkuSprite){
  Settings{physical=List.copyOf(physical);magic=List.copyOf(magic);if(physical.size()!=5||magic.size()!=5)throw new IllegalArgumentException("five digit bases required");}
  static Settings read(Properties p){
   var places=List.of("Unit","Ten","Hundred","Thousand","TenThousand");var physical=new ArrayList<Integer>();var magic=new ArrayList<Integer>();
   for(int i=0;i<5;i++){physical.add(Integer.parseInt(p.getProperty("DmgSprPhysical"+places.get(i),String.valueOf(25001+10*i))));magic.add(Integer.parseInt(p.getProperty("DmgSprMagic"+places.get(i),String.valueOf(25051+10*i))));}
   return new Settings(Boolean.parseBoolean(p.getProperty("hpbar","false")),Boolean.parseBoolean(p.getProperty("DmgDisplayPhysicalEnable","false")),Integer.parseInt(p.getProperty("DmgSprPhysicalMiss","0")),Integer.parseInt(p.getProperty("DmgSprPhysicalCrit","0")),physical,magic,Boolean.parseBoolean(p.getProperty("KiringkuEffectEnable","true")),Integer.parseInt(p.getProperty("KiringkuEffectSpr","7049")));
  }
 }
 static List<Effect> physical(Settings s,Long attacker,Long target,boolean show,boolean dead,int damage,boolean critical,boolean miss){
  if(!s.numbers||attacker==null||!show||dead)return List.of();var effects=new ArrayList<Effect>(7);
  if(miss){if(s.miss>0)effects.add(new Effect(attacker,s.miss));return List.copyOf(effects);}
  if(critical&&s.critical>0)effects.add(new Effect(attacker,s.critical));
  if(target!=null&&damage>0)effects.addAll(digits(target,damage,s.physical));return List.copyOf(effects);
 }
 static List<Effect> magic(Settings s,Long attacker,Long target,boolean show,boolean dead,int damage){return !s.numbers||attacker==null||!show||dead||target==null||damage<=0?List.of():digits(target,damage,s.magic);}
 private static List<Effect> digits(long target,int damage,List<Integer> bases){var out=new ArrayList<Effect>(5);int bounded=Math.min(damage,99999),places=damage<10?1:damage<100?2:damage<1000?3:damage<10000?4:5;int divisor=1;for(int i=1;i<places;i++)divisor*=10;for(int p=places-1;p>=0;p--,divisor/=10){int base=bases.get(p);if(base>0)out.add(new Effect(target,base+(bounded/divisor)%10));}return List.copyOf(out);}
 private NativeSourceDisplayPlan(){}
}
