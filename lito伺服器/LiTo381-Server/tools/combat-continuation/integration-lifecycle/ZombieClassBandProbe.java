package local.lito381.server;
import java.nio.file.*;
/** Source L1SummonInstance wizard/elf bands against the actual native method. */
public final class ZombieClassBandProbe {
 static int expected(int type,int level){if(type==3){if(level>=52)return 81188;if(level>=48)return 81187;if(level>=44)return 81186;if(level>=40)return 81185;if(level>=32)return 81184;if(level>=24)return 81183;}else if(type==2&&level>=48)return 81183;return 45065;}
 public static void main(String[]a)throws Exception{
  var jar=Path.of(a[0]).toRealPath();if(!Path.of(BattleSpellCatalog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("actual candidate catalog");int mismatches=0,cases=0;var rows=new StringBuilder();
  for(int type=0;type<7;type++)for(int level=1;level<=127;level++){var actual=BattleSpellCatalog.zombieTemplate(type,level);int expected=expected(type,level);if(actual==null)throw new AssertionError("legal class and level has no template");if(actual.npcId()!=expected){mismatches++;rows.append(type+"\t"+level+"\t"+expected+"\t"+actual.npcId()+"\n");}cases++;}
  if(cases!=889||mismatches!=(a[2].equals("before")?208:0))throw new AssertionError("Source class/level mismatches="+mismatches);Files.writeString(Path.of(a[1]).resolveSibling("differences.tsv"),"type\tlevel\tSource-npc\tnative-npc\n"+rows);Files.writeString(Path.of(a[1]),"{\"cases\":"+cases+",\"mismatches\":"+mismatches+",\"full_goal_achieved\":false}\n");
 }
}
