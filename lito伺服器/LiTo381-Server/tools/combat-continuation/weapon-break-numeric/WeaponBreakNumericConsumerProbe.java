package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Actual Tournament prefix integration; does not claim full NPC HP publication. */
public final class WeaponBreakNumericConsumerProbe {
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[0]).toRealPath();for(var cls:List.of(BattleWeaponBreak.class,BattleTournamentNpcPhysicalDamage.class,ClassicWorldNpcDamage.class))if(!Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("consumer overlay");
  var attacker=SourceNpcPhysicalStats.memoryFixture(45065);var growth=SourcePhysicalGrowthStats.classic381();long noon=1735689600000L+7200000L;int cases=0,gaps=0;
  for(int level:new int[]{1,52,255,Integer.MAX_VALUE})for(int leverage:new int[]{10,1000,1000000})for(int type=0;type<7;type++)for(int ac:new int[]{0,-100})for(int critical:new int[]{0,99})for(int stacks=0;stacks<=10;stacks++){
   var base=BattleTournamentNpcPhysicalDamage.againstPlayer(level,attacker,growth,leverage,0,type,ac,level-1,critical,0,noon);
   var actual=BattleTournamentNpcPhysicalDamage.againstPlayer(level,attacker,growth,leverage,stacks,type,ac,level-1,critical,0,noon);
   int expected=(int)SourceWeaponBreakNumeric.damage(base.sourceDamage(),stacks);
   if(actual.sourceDamage()!=expected||actual.afterAcDamage()!=Math.max(0,expected-base.acReduction()))gaps++;
   if(actual.acReduction()!=base.acReduction()||actual.acMaximum()!=base.acMaximum()||actual.acRoll()!=base.acRoll())throw new AssertionError("unrelated AC phase changed");cases++;
  }
  if(args[2].equals("after")&&gaps!=0||args[2].equals("before")&&gaps==0)throw new AssertionError("consumer gaps="+gaps);
  Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"numeric_consumer_gaps\":"+gaps+",\"actual_consumer\":\"BattleTournamentNpcPhysicalDamage.againstPlayer\",\"NPC_HP_publication_qualified\":false,\"full_goal_achieved\":false}\n");System.out.println(args[2]+" native calculation cases="+cases+" gaps="+gaps);
 }
}
