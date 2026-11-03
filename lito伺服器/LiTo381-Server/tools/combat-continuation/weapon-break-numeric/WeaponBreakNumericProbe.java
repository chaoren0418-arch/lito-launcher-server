package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Reference is extracted verbatim from the selected Source NPC damage block. */
public final class WeaponBreakNumericProbe {
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[0]).toRealPath();if(!Path.of(BattleWeaponBreak.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("helper overlay");
  var values=new LinkedHashSet<Integer>();for(int i=0;i<=1000;i++)values.add(i);Collections.addAll(values,Integer.MAX_VALUE,Integer.MAX_VALUE-1,1<<30,999999,9999999,21474836,21474837,42949672,42949673);
  var random=new Random(270019);for(int i=0;i<10000;i++)values.add(random.nextInt()&Integer.MAX_VALUE);
  int cases=0,gaps=0;for(int damage:values)for(int stacks=0;stacks<=10;stacks++){
   int expected=(int)SourceWeaponBreakNumeric.damage(damage,stacks),actual=BattleWeaponBreak.damageAfterStacks(damage,stacks);if(actual!=expected)gaps++;cases++;
  }
  if(args[2].equals("after")&&gaps!=0||args[2].equals("before")&&gaps==0)throw new AssertionError("source numeric parity gaps="+gaps);
  Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"numeric_parity_gaps\":"+gaps+",\"native_consumers_qualified\":false,\"full_goal_achieved\":false}\n");
  System.out.println(args[2]+" cases="+cases+" numeric-parity-gaps="+gaps);
 }
}
