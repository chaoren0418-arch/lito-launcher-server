// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/pet-controls/NativePetControls.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Source collar/taming controls; not installed until the native consumer patch is qualified. */
final class NativePetControls {
 static final List<String> KEYS=List.of("PrinceChaForPet","KnightChaForPet","ElfChaForPet","WizardChaForPet","DarkelfChaForPet","DragonKnightChaForPet","IllusionistChaForPet");
 static final List<Integer> DEFAULTS=List.of(6,0,12,6,6,6,6);
 static List<Integer> parse(Properties properties){
  var result=new ArrayList<Integer>(7);
  for(int i=0;i<KEYS.size();i++)result.add(Integer.parseInt(properties.getProperty(KEYS.get(i),DEFAULTS.get(i).toString())));
  return List.copyOf(result);
 }
 static void loadInto(Map<String,Double> values,Set<String> defaulted,Properties properties,Path file)throws IOException{
  final List<Integer> parsed;
  try{parsed=parse(properties);}catch(NumberFormatException error){throw new IOException(file+": invalid source pet charisma control",error);}
  // Parse everything before modifying the unpublished snapshot.
  for(int i=0;i<KEYS.size();i++){String key=KEYS.get(i);values.put(key,parsed.get(i).doubleValue());if(!properties.containsKey(key))defaulted.add(key);}
 }
 static int configuredBonus(int type){return type<0||type>=KEYS.size()?0:(int)CombatRules.number(KEYS.get(type),DEFAULTS.get(type));}
 static int configuredBonus(CharacterSummary character){return configuredBonus(character==null?0:character.type());}
 static void loadFromProperties(Properties properties,Map<String,Double> values,Set<String> defaults,Path file)throws IOException{loadInto(values,defaults,properties,file);}
 static int effectiveCharisma(ClientSession pc,CharacterSummary character){
  int value=CombatAccess.integerCall(pc,"effectiveCharisma","(Llocal/lito381/server/CharacterSummary;)I",character);
  return Math.max(1,Math.min(254,value)); // Source L1PcInstance.getCha contract.
 }
 static int available(int charisma,int bonus,int used){return charisma+bonus-used;}
 static boolean canTake(int count,int charisma,int bonus,int used,int requested){return count<8&&available(charisma,bonus,used)>=requested;}
 private NativePetControls(){}
}
