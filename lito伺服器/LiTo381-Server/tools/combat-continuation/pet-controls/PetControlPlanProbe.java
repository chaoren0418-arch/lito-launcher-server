package local.lito381.server;
import java.nio.file.*;import java.util.*;
public final class PetControlPlanProbe {
 static int checks,cases;
 static void need(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
 public static void main(String[]args)throws Exception{
  Properties installed=new Properties();try(var in=Files.newBufferedReader(Path.of(args[0]))){installed.load(in);}
  List<Properties> configs=new ArrayList<>();configs.add(new Properties());configs.add(installed);
  for(int[] bonuses:new int[][]{{1,2,3,4,5,6,7},{-7,0,30,100,Integer.MAX_VALUE,Integer.MIN_VALUE,9}}){var p=new Properties();for(int i=0;i<7;i++)p.setProperty(NativePetControls.KEYS.get(i),""+bonuses[i]);configs.add(p);}
  for(var p:configs){SourcePetReference.configure(p);var bonuses=NativePetControls.parse(p);
   for(int type=-1;type<=7;type++)for(int cha:new int[]{1,6,9,18,100,254})for(int count:new int[]{0,1,7,8,9})for(int used:new int[]{0,6,48,1000})for(int cost:new int[]{0,1,6,12,1000}){
    if(count==0&&used!=0)continue;
    var ref=new SourcePetReference(type,cha,count,used);int bonus=type<0||type>=7?0:bonuses.get(type);
    need(NativePetControls.available(cha,bonus,used)==ref.getChaForPet()-used,"Source effective charisma/class bonus/signed-int order");
    need(NativePetControls.canTake(count,cha,bonus,used,cost)==ref.canTakePet(cost),"Source live count and cost boundary");cases++;
   }
   var values=new HashMap<String,Double>();var missing=new HashSet<String>();NativePetControls.loadInto(values,missing,p,Path.of("isolated-character-controls"));need(values.size()==7,"seven controls loaded");for(int i=0;i<7;i++){String key=NativePetControls.KEYS.get(i);need(values.get(key)==bonuses.get(i).doubleValue()&&missing.contains(key)==!p.containsKey(key),"exact key/default provenance");}
  }
  for(String bad:new String[]{""," 6","6 ","1.5","2147483648","-2147483649","NaN"})for(String key:NativePetControls.KEYS){
   var p=new Properties();p.setProperty(key,bad);boolean a=false,b=false;try{SourcePetReference.configure(p);}catch(NumberFormatException e){a=true;}try{NativePetControls.parse(p);}catch(NumberFormatException e){b=true;}need(a&&b,"same source parser rejects malformed property");
   var values=new HashMap<String,Double>();values.put("retained",12.0);var missing=new HashSet<String>();try{NativePetControls.loadInto(values,missing,p,Path.of("invalid-fixture"));throw new AssertionError("loader should reject");}catch(java.io.IOException expected){need(values.equals(Map.of("retained",12.0))&&missing.isEmpty(),"failed parse cannot partially update unpublished snapshot");}
  }
  Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"native_consumers_tested\":false}\n");
 }
}
