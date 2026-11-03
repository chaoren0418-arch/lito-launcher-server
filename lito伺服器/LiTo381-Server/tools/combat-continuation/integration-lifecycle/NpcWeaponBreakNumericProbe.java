package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Independent retained Source method oracle, including fractional BOSS and saturation cases. */
public final class NpcWeaponBreakNumericProbe {
 public static void main(String[]args)throws Exception{
  boolean before=args[2].equals("before");var jar=Path.of(args[0]).toRealPath();for(var c:List.of(ClassicWorldNpcDamage.class,BattleWeaponBreak.class,BattleTournamentNpcPhysicalDamage.class))if(!Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("production class overlay");
  if(!before&&!Path.of(NativeNpcWeaponBreakDamage.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("prefix helper overlay");
  var oracle=new SourceNpcDmgModeOracle();long cases=0,gaps=0,bossFractionGaps=0;var examples=new ArrayList<String>();
  for(int level:new int[]{1,3,52,99,255,Integer.MAX_VALUE})for(int strength:new int[]{0,1,9,20,35,254})for(int growth:new int[]{-10,0,5,127})for(int roll:new int[]{0,level-1})for(int critical:new int[]{0,14,15,99})for(boolean night:new boolean[]{false,true})for(boolean boss:new boolean[]{false,true})for(int leverage:new int[]{1,3,7,10,11,15,1000,1000000,Integer.MAX_VALUE})for(int stacks:new int[]{0,1,2,5,9,10}){
   int expected=Math.max(0,(int)oracle.calculate(roll,strength,growth,critical,night,boss,leverage,stacks));int actual=before?BattleWeaponBreak.damageAfterStacks(ClassicWorldNpcDamage.sourceDamage(level,strength,growth,roll,critical,night,boss,leverage),stacks):NativeNpcWeaponBreakDamage.afterStacks(NativeNpcWeaponBreakDamage.sourceExact(level,strength,growth,roll,critical,night,boss,leverage),stacks);
   if(actual!=expected){gaps++;if(boss)bossFractionGaps++;if(examples.size()<8)examples.add("level="+level+" strength="+strength+" growth="+growth+" roll="+roll+" critical="+critical+" night="+night+" boss="+boss+" leverage="+leverage+" stacks="+stacks+" Source="+expected+" native="+actual);}cases++;
  }
  long consumerCases=0,consumerGaps=0;var growth=SourcePhysicalGrowthStats.classic381();long noon=1735689600000L+7200000L;
  for(boolean boss:new boolean[]{false,true})for(int level:new int[]{1,52,255,Integer.MAX_VALUE})for(int leverage:new int[]{3,7,10,11,1000,1000000,Integer.MAX_VALUE})for(int type=0;type<7;type++)for(int ac:new int[]{0,-100})for(int critical:new int[]{0,14,15,99})for(int stacks=0;stacks<=10;stacks++){
   var attacker=new SourceNpcPhysicalStats(45065,0,false,0,false,0,0,0,false,0,true,boss,20,20,20);
   var actual=BattleTournamentNpcPhysicalDamage.againstPlayer(level,attacker,growth,leverage,stacks,type,ac,level-1,critical,0,noon);
   int expected=Math.max(0,(int)oracle.calculate(level-1,20,growth.damage(false,20),critical,false,boss,leverage,stacks));
   if(actual.sourceDamage()!=expected||actual.afterAcDamage()!=Math.max(0,expected-actual.acReduction()))consumerGaps++;consumerCases++;
  }
  if(before?(gaps==0||consumerGaps==0):(gaps!=0||consumerGaps!=0))throw new AssertionError("Source mismatches helper="+gaps+" consumer="+consumerGaps+" "+examples);
  Files.writeString(Path.of(args[1]),"{\"prefix_cases\":"+cases+",\"prefix_gaps\":"+gaps+",\"boss_gaps\":"+bossFractionGaps+",\"consumer_cases\":"+consumerCases+",\"consumer_gaps\":"+consumerGaps+",\"native_four_HP_commit_paths_qualified\":false,\"full_goal_achieved\":false}\n");Files.write(Path.of(args[1]).resolveSibling("mismatch-examples.txt"),examples);
 }
}
