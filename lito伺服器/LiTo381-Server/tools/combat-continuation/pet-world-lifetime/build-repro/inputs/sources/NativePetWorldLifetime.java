package local.lito381.server;
import java.io.IOException;import java.util.*;
/** WORLD-only native NPC binding. Battle encounter lifetime rules are separate. */
final class NativePetWorldLifetime {
 private static final String P="local/lito381/server/";
 private static final ThreadLocal<Target> ingress=new ThreadLocal<>(),current=new ThreadLocal<>();
 private static ClassicWorldMonsterPopulation population(GameHub hub,int map,int instance){return (ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)L"+P+"ClassicWorldMonsterPopulation;",map,instance);}
 private static Object actor(ClassicWorldMonsterPopulation population,long id){return population==null?null:((Map<?,?>)CombatAccess.field(population,"actors")).get(id);}
 private static Object epoch(Object actor){return actor==null?null:NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoisonStatus"),false);}
 private static Object bindEpoch(Object actor){return actor==null?null:NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoisonStatus"),true);}
 private static final class Target {
  final ClientSession pc;final GameHub hub;final NativePlayerCommit.Binding binding;final int map,instance;final long id;final Object epoch;final ClassicWorldMonsterPopulation population;final Object actor;
  Object publicationEpoch;
  Target(ClientSession pc,GameHub hub,int map,int instance,long id,ClassicWorldMonsterPopulation population,Object actor){this.pc=pc;this.hub=hub;this.map=map;this.instance=instance;this.id=id;this.population=population;this.actor=actor;binding=NativePlayerCommit.Binding.capture(pc);epoch=bindEpoch(actor);publicationEpoch=epoch;}
  boolean scene(){return binding.matches(pc)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)&&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")&&(int)CombatAccess.field(pc,"classicWorldMapId")==map&&(int)CombatAccess.field(pc,"classicWorldInstanceId")==instance;}
  boolean valid(){return scene()&&population(hub,map,instance)==population&&actor(population,id)==actor&&epoch(actor)==epoch;}
  boolean publication(){synchronized(hub){if(!scene()||population(hub,map,instance)!=population)return false;if(population==null)return true;synchronized(population){var live=actor(population,id);return (live==actor||live==null)&&epoch(actor)==publicationEpoch;}}}
 }
 private static Target take(ClientSession pc,byte[]packet){
  var hub=(GameHub)CombatAccess.field(pc,"hub");int map=(int)CombatAccess.field(pc,"classicWorldMapId"),instance=(int)CombatAccess.field(pc,"classicWorldInstanceId");
  long id=Protocol.spellSelector(packet).skillId()==36?Protocol.attackSpellRequest(packet).targetObjectId():Protocol.objectSpellRequest(packet).targetObjectId();
  synchronized(hub){var population=population(hub,map,instance);if(population==null)return new Target(pc,hub,map,instance,id,null,null);synchronized(population){return new Target(pc,hub,map,instance,id,population,actor(population,id));}}
 }
 static void dispatch(ClientSession pc,byte[]packet,NativeSelfCastAction.Task body)throws IOException{
  int skill=Protocol.spellSelector(packet).skillId();if((skill!=36&&skill!=41)||!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")||pc.selectedCharacterName().isBlank()){body.run();return;}
  var prior=ingress.get();ingress.set(take(pc,packet));try{body.run();}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 static void run(ClientSession pc,byte[]packet,NativeSelfCastAction.Task body)throws IOException{
  if(!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")){NativeLightAction.commitPair(pc,pc,body);return;}
  var inherited=ingress.get();var target=inherited!=null&&inherited.pc==pc?inherited:take(pc,packet);ingress.remove();
  try{NativeLightAction.commitPair(pc,pc,()->{
   // Preserve the established player -> hub -> population order. The native
   // Light batch queues network publication until all these monitors exit.
   synchronized(target.hub){if(target.population==null){if(target.valid())body.run();return;}synchronized(target.population){
    if(!target.valid())return;var prior=current.get();current.set(target);
    try{body.run();}finally{target.publicationEpoch=bindEpoch(target.actor);if(prior==null)current.remove();else current.set(prior);}
   }}
  });}finally{if(inherited==null)ingress.remove();else ingress.set(inherited);}
 }
 static boolean captureOutput(ClientSession receiver,byte[]plain,String note){
  var target=current.get();if(target==null){current.remove();return false;}
  boolean effect=note.startsWith("S_EFFECT taming target=")||note.startsWith("S_EFFECT create-zombie corpse=");
  boolean removal=note.startsWith("S_REMOVE_OBJECT ")&&(note.endsWith(" reason=tamed")||note.endsWith(" reason=create-zombie"));
  if(!effect&&!removal)return false;byte[]snapshot=plain.clone();NativeLightAction.deferScoped(()->{if(target.publication())receiver.nativeSelfOriginalSend(snapshot,note);});return true;
 }
 private NativePetWorldLifetime(){}
}
