// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-npc-material/NativeCancellationNpcMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;import java.util.*;
/** Ordered player ownership precedes hub, population and NPC state ownership. */
final class NativeCancellationNpcMaterialAction {
 static final String P="local/lito381/server/";
 private static final ThreadLocal<Target> current=new ThreadLocal<>(),ingress=new ThreadLocal<>();
 static ClassicWorldMonsterPopulation population(GameHub hub,int map,int instance){return(ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)L"+P+"ClassicWorldMonsterPopulation;",map,instance);}
 static Object actor(ClassicWorldMonsterPopulation pop,long id){return pop==null?null:((Map<?,?>)CombatAccess.field(pop,"actors")).get(id);}
 static Object epoch(Object actor,boolean create){return actor==null?null:NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoisonStatus"),create);}
 static final class Target {
  final ClientSession pc,owner;final NativePlayerCommit.Binding casterBinding,ownerBinding;final GameHub hub;
  final int map,instance;final long id;final ClassicWorldMonsterPopulation population;final Object actor,epoch;final BattleCompanion companion;
  Object publicationEpoch;
  Target(ClientSession pc,ClientSession owner,BattleCompanion companion,long id){
   this.pc=pc;this.owner=owner==null?pc:owner;this.companion=companion;this.id=id;casterBinding=NativePlayerCommit.Binding.capture(pc);ownerBinding=NativePlayerCommit.Binding.capture(this.owner);
   hub=(GameHub)CombatAccess.field(pc,"hub");map=(int)CombatAccess.field(pc,"classicWorldMapId");instance=(int)CombatAccess.field(pc,"classicWorldInstanceId");
   synchronized(hub){population=companion==null?population(hub,map,instance):null;if(population==null){actor=null;epoch=null;}else synchronized(population){actor=actor(population,id);epoch=epoch(actor,true);}}
   publicationEpoch=epoch;
  }
  boolean scene(){return casterBinding.matches(pc)&&ownerBinding.matches(owner)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)&&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")&&(int)CombatAccess.field(pc,"classicWorldMapId")==map&&(int)CombatAccess.field(pc,"classicWorldInstanceId")==instance;}
  boolean valid(){
   if(!scene())return false;
   if(companion!=null)return CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",id)==companion;
   return population!=null&&population(hub,map,instance)==population&&actor(population,id)==actor&&actor!=null&&epoch(actor,false)==epoch;
  }
  boolean publication(){synchronized(hub){if(!scene())return false;if(companion!=null){synchronized(companion){var snapshot=companion.snapshot();return valid()&&snapshot.active()&&snapshot.hp()>0;}}
   if(population==null||population(hub,map,instance)!=population)return false;
   synchronized(population){return actor(population,id)==actor&&epoch(actor,false)==publicationEpoch&&(boolean)CombatAccess.field(actor,"alive")&&(boolean)CombatAccess.field(actor,"present");}
  }}
 }
 static void dispatch(ClientSession pc,byte[]packet,NativeSelfCastAction.Task body)throws IOException{
  if(Protocol.spellSelector(packet).skillId()!=44||!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")||pc.selectedCharacterName().isBlank()){NativePetWorldLifetime.dispatch(pc,packet,body);return;}
  long id=Protocol.objectSpellRequest(packet).targetObjectId();var hub=(GameHub)CombatAccess.field(pc,"hub");int map=(int)CombatAccess.field(pc,"classicWorldMapId"),instance=(int)CombatAccess.field(pc,"classicWorldInstanceId");
  ClientSession owner=null;BattleCompanion companion=null;
  for(var candidate:hub.classicWorldMembers(map,instance)){var owned=(BattleCompanion)CombatAccess.call(candidate,"ownedCompanion","(J)L"+P+"BattleCompanion;",id);if(owned!=null){owner=candidate;companion=owned;break;}}
  var prior=ingress.get();ingress.set(new Target(pc,owner,companion,id));
  try{NativePetWorldLifetime.dispatch(pc,packet,body);}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 static void worldNpc(ClientSession pc,Protocol.ObjectSpellRequest request,ClientSession owner,BattleCompanion companion,BattleCompanion.Snapshot companionSnapshot,ClassicWorldMonsterPopulation.Snapshot monster,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope inherited)throws IOException{
  var captured=ingress.get();var target=captured!=null&&captured.pc==pc&&captured.id==request.targetObjectId()?captured:new Target(pc,owner,companion,request.targetObjectId());
  NativeLightAction.commitPair(pc,target.owner,()->NativeWorldMaterialPolicy.run(pc,true,44,()->{
   synchronized(target.hub){if(companion!=null){synchronized(companion){if(target.valid())owned(target,pc,request,owner,companion,companionSnapshot,monster,spell);}}
    else if(target.population!=null)synchronized(target.population){if(target.valid())owned(target,pc,request,owner,companion,companionSnapshot,monster,spell);}
   }
  }));
 }
 private static void owned(Target target,ClientSession pc,Protocol.ObjectSpellRequest request,ClientSession owner,BattleCompanion companion,BattleCompanion.Snapshot companionSnapshot,ClassicWorldMonsterPopulation.Snapshot monster,BattleSpellCatalog.CancellationSpell spell)throws IOException{
  var prior=current.get();current.set(target);try{body(pc,request,owner,companion,companionSnapshot,monster,spell);}finally{target.publicationEpoch=epoch(target.actor,true);if(prior==null)current.remove();else current.set(prior);}
 }
 static boolean captureOutput(ClientSession receiver,byte[]plain,String note){
  var target=current.get();if(target==null){current.remove();return false;}
  var binding=NativePlayerCommit.Binding.capture(receiver);byte[]frozen=plain.clone();
  long id=frozen.length<5?-1:((long)(frozen[1]&255)|((long)(frozen[2]&255)<<8)|((long)(frozen[3]&255)<<16)|((long)(frozen[4]&255)<<24));
  boolean npcPacket=id==target.id;
  NativeLightAction.deferScoped(()->{if(binding.matches(receiver)&&(!npcPacket||target.publication()))receiver.nativeSelfOriginalSend(frozen,note);});return true;
 }
 private static void body(ClientSession pc,Protocol.ObjectSpellRequest request,ClientSession owner,BattleCompanion companion,BattleCompanion.Snapshot companionSnapshot,ClassicWorldMonsterPopulation.Snapshot monster,BattleSpellCatalog.CancellationSpell spell)throws IOException{
  var scope=CombatPoisonLifecycle.open();NativeLightAction.finishScoped((published,failure)->{if(published&&failure==null)CombatPoisonLifecycle.finish(scope);else CombatPoisonLifecycle.failed(scope,failure!=null?failure:new java.util.concurrent.CancellationException("NPC cancellation publication retired"));});
  try{pc.nativeMaterialWorldNpcCancellationBody(request,owner,companion,companionSnapshot,monster,spell,scope);}catch(IOException|RuntimeException|Error failure){CombatPoisonLifecycle.failed(scope,failure);throw failure;}
 }
 static boolean admit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){if(skill!=44)throw new IllegalStateException("unexpected NPC cancellation skill");return NativeLightAction.admitPair(pc,owner==null?pc:owner,skill,action,reuse);}
 private NativeCancellationNpcMaterialAction(){}
}
