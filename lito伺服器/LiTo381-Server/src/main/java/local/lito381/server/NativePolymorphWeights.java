// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/polymorph-source/NativePolymorphWeights.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.util.List;
/** WandPolyTable's fixed-million choice, in the exact SQL result order. */
final class NativePolymorphWeights {
 static final int DENOMINATOR=1_000_000;
 record Row(int polyId,int chance){Row{if(chance<=0)throw new IllegalArgumentException("SQL filters non-positive chance");}}
 static int select(List<Row> rows,int roll){
  if(roll<1||roll>DENOMINATOR)throw new IllegalArgumentException("polymorph roll outside 1..1000000");
  int sum=0;
  for(var row:rows){sum+=row.chance();if(roll<=sum)return row.polyId();}
  return 29;
 }
 private NativePolymorphWeights(){}
}
