package local.lito381.server;
import java.util.*;import java.nio.file.*;
public final class DisplayPlanProbe {
 static int cases,checks;
 static void need(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static List<NativeCombatDisplayPlan.Effect> effects(List<SourceDisplayReference.ServerBasePacket> values){return values.stream().map(p->new NativeCombatDisplayPlan.Effect(p.object,p.sprite)).toList();}
 public static void main(String[]args)throws Exception{
  var live=new Properties();try(var input=Files.newInputStream(Path.of(args[0]))){live.load(input);}
  var defaults=new Properties();var enabled=new Properties();enabled.setProperty("DmgDisplayPhysicalEnable","true");enabled.setProperty("DmgSprPhysicalMiss","25302");enabled.setProperty("DmgSprPhysicalCrit","7000");
  var sparse=new Properties();sparse.putAll(enabled);sparse.setProperty("DmgSprPhysicalUnit","0");sparse.setProperty("DmgSprMagicTen","-1");sparse.setProperty("DmgSprPhysicalTenThousand","0");
  var liveEnabled=new Properties();liveEnabled.putAll(live);liveEnabled.setProperty("DmgDisplayPhysicalEnable","true");
  for(var props:List.of(defaults,enabled,sparse,live,liveEnabled)){
   var settings=NativeCombatDisplayPlan.Settings.read(props);SourceDisplayReference.configure(props);
   need(settings.hpbar()==SourceDisplayReference.ConfigOther.HPBAR&&settings.kiringku()==SourceDisplayReference.ConfigOther.KIRINGKU_EFFECT_ENABLE&&settings.kiringkuSprite()==SourceDisplayReference.ConfigOther.KIRINGKU_EFFECT_SPR,"Source non-digit control defaults and configured values");
   for(boolean attackerPresent:new boolean[]{false,true})for(boolean targetPresent:new boolean[]{false,true})for(boolean show:new boolean[]{false,true})for(boolean dead:new boolean[]{false,true})for(int damage:new int[]{-1,0,1,9,10,99,100,101,999,1000,9999,10000,99999,100000,Integer.MAX_VALUE})for(boolean critical:new boolean[]{false,true})for(boolean miss:new boolean[]{false,true}){
    var attacker=attackerPresent?new SourceDisplayReference.L1PcInstance(123,show,dead):null;var target=targetPresent?new SourceDisplayReference.L1Character(456):null;Long a=attacker==null?null:123L,t=target==null?null:456L;
    need(effects(SourceDisplayReference.buildPhysicalDamagePackets(attacker,target,damage,critical,miss)).equals(NativeCombatDisplayPlan.physical(settings,a,t,show,dead,damage,critical,miss)),"unchanged Source physical packet plan");
    need(effects(SourceDisplayReference.buildMagicDamagePackets(attacker,target,damage)).equals(NativeCombatDisplayPlan.magic(settings,a,t,show,dead,damage)),"unchanged Source magic packet plan");cases++;
   }
  }
  for(String raw:List.of("x"," ","1 ","2147483648","-1","0","65535")){var props=new Properties();props.setProperty("DmgSprPhysicalMiss",raw);boolean sourceError=false,nativeError=false;try{SourceDisplayReference.configure(props);}catch(NumberFormatException e){sourceError=true;}try{NativeCombatDisplayPlan.Settings.read(props);}catch(NumberFormatException e){nativeError=true;}need(sourceError==nativeError,"Source numeric parser parity: "+raw);}
  String result="{\"cases\":"+cases+",\"checks\":"+checks+",\"status\":\"passed-pure-planning-only\",\"native_consumers_installed\":false,\"scheduler_or_native_UI_tested\":false}";Files.writeString(Path.of(args[1]),result+"\n");System.out.println(result);
 }
}
