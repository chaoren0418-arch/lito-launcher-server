package local.lito381.server;
import java.util.*;import java.nio.file.*;
public final class PolymorphWeightsProbe {
 static int cases,checks;
 static void need(boolean ok){checks++;if(!ok)throw new AssertionError("Source WandPolyTable parity case "+cases);}
 static void compare(List<NativePolymorphWeights.Row> rows,int roll){
  int actual=NativePolymorphWeights.select(rows,roll);int source=SourceWandPolyReference.select(rows,roll);need(actual==source);cases++;
 }
 public static void main(String[]args)throws Exception{
  var random=new Random(0x145067);
  List<List<NativePolymorphWeights.Row>> sets=List.of(List.of(),List.of(new NativePolymorphWeights.Row(123,1)),List.of(new NativePolymorphWeights.Row(124,999999)),List.of(new NativePolymorphWeights.Row(125,1000000)),List.of(new NativePolymorphWeights.Row(126,1000001)),List.of(new NativePolymorphWeights.Row(1,500000),new NativePolymorphWeights.Row(2,500000)),List.of(new NativePolymorphWeights.Row(7,1),new NativePolymorphWeights.Row(7,2),new NativePolymorphWeights.Row(8,3)),List.of(new NativePolymorphWeights.Row(8,Integer.MAX_VALUE),new NativePolymorphWeights.Row(9,Integer.MAX_VALUE)));
  for(var rows:sets)for(int roll:new int[]{1,2,3,4,6,7,499999,500000,500001,999999,1000000})compare(rows,roll);
  for(int iteration=0;iteration<5000;iteration++){
   var rows=new ArrayList<NativePolymorphWeights.Row>();for(int i=0,n=random.nextInt(30);i<n;i++)rows.add(new NativePolymorphWeights.Row(random.nextInt(30000),1+random.nextInt(100000)));
   rows.sort(Comparator.comparingInt(NativePolymorphWeights.Row::chance));compare(rows,1+random.nextInt(1000000));
  }
  need(NativePolymorphWeights.select(List.of(),1)==29);need(NativePolymorphWeights.select(List.of(new NativePolymorphWeights.Row(1,1)),2)==29);
  for(int roll:new int[]{Integer.MIN_VALUE,0,1000001,Integer.MAX_VALUE}){boolean rejected=false;try{NativePolymorphWeights.select(List.of(),roll);}catch(IllegalArgumentException expected){rejected=true;}need(rejected);}
  for(int chance:new int[]{-1,0,Integer.MIN_VALUE}){boolean rejected=false;try{new NativePolymorphWeights.Row(1,chance);}catch(IllegalArgumentException expected){rejected=true;}need(rejected);}
  Files.writeString(Path.of(args[0]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"full_goal_achieved\":false}\n");
 }
}
