// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/call-nature-material/NativeCallNatureMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;import java.io.UncheckedIOException;import java.util.*;
/** Skill165: preserve native resurrection semantics; debit only the captured corpse generation. */
final class NativeCallNatureMaterialAction {
 static final String P="local/lito381/server/";static final ThreadLocal<Target> current=new ThreadLocal<>();
 static final class Target {
  final NativeAreaMaterialAction.Context scene;final long id;int kind;Object owner,actor,epoch,publicationEpoch,publicationActor;ClientSession player;NativePlayerCommit.Binding playerBinding;boolean publicationDead;
  Target(ClientSession pc,long id){this.id=id;scene=new NativeAreaMaterialAction.Context(pc);
   synchronized(scene.hub){
    for(var member:scene.players){
     if(member.playerObjectId()==id){kind=1;player=member;playerBinding=scene.bindings.get(member);actor=member;owner=member;break;}
     var pet=(BattleCompanion)CombatAccess.call(member,"ownedCompanion","(J)L"+P+"BattleCompanion;",id);
     if(pet!=null){kind=2;player=member;playerBinding=scene.bindings.get(member);owner=member;actor=pet;break;}
    }
    if(kind==0&&scene.phase.equals("WORLD")&&scene.population!=null)synchronized(scene.population){actor=NativeCancellationNpcMaterialAction.actor(scene.population,id);if(actor!=null){kind=3;owner=scene.population;}}
    if(kind==0&&scene.phase.equals("BATTLE"))for(int k:new int[]{1,2}){var encounter=scene.encounter(k);var candidate=corpse(encounter,id);if(candidate!=null){kind=k+3;owner=encounter;actor=candidate;break;}}
    epoch=life(true);publicationEpoch=epoch;publicationActor=actor;
   }
  }
  static Object corpse(Object encounter,long id){if(encounter==null)return null;synchronized(encounter){for(var c:(List<?>)CombatAccess.field(encounter,"corpses")){var snap=CombatAccess.field(c,"snapshot");if((long)CombatAccess.call(snap,"objectId","()J")==id)return c;}return null;}}
  Object life(boolean create){
   if(kind==1)return NativePlayerCommit.Binding.capture(player).owner();
   if(kind==2)return NativeCompanionCallbacks.capture(playerBinding,(BattleCompanion)actor).life;
   if(kind==3)return NativeCancellationNpcMaterialAction.epoch(actor,create);
   if(kind==4||kind==5)return actor;
   return null;
  }
  boolean canonical(){
   if(kind==1)return playerBinding.matches(player)&&scene.player(player)&&!(boolean)CombatAccess.field(player,NativeDeItemApplication.DISCONNECTED);
   if(kind==2)return playerBinding.matches(player)&&scene.player(player)&&!(boolean)CombatAccess.field(player,NativeDeItemApplication.DISCONNECTED)&&CombatAccess.call(player,"ownedCompanion","(J)L"+P+"BattleCompanion;",id)==actor;
   if(kind==3)return NativeCancellationNpcMaterialAction.population(scene.hub,scene.map,scene.instance)==owner&&NativeCancellationNpcMaterialAction.actor((ClassicWorldMonsterPopulation)owner,id)==actor&&(boolean)CombatAccess.field(actor,"present");
   if(kind==4||kind==5)return scene.encounter(kind-3)==owner&&corpse(owner,id)==actor&&System.nanoTime()-(long)CombatAccess.field(actor,"expiresAtNanos")<0;
   return false;
  }
  boolean dead(){if(kind==1)return(int)CombatAccess.field(player,"playerHp")==0;if(kind==2){var s=((BattleCompanion)actor).snapshot();return s.active()&&s.hp()==0;}if(kind==3)return!(boolean)CombatAccess.field(actor,"alive");return kind==4||kind==5;}
  boolean valid(){return scene.scene()&&canonical()&&life(false)==epoch&&dead();}
  boolean publication(){synchronized(scene.hub){if(scene.population!=null)synchronized(scene.population){return publicationOwned();}return publicationOwned();}}
  void finish(){publicationActor=actor;publicationEpoch=life(true);publicationDead=kind!=0&&dead();
   if(kind==4||kind==5){var live=scene.monster(owner,kind-3,id);if(live!=null){publicationActor=live;publicationEpoch=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(live,"damagePoison"),true);publicationDead=false;}}
  }
  boolean publicationOwned(){
   if((kind==4||kind==5)&&!publicationDead)return scene.encounter(kind-3)==owner&&scene.monster(owner,kind-3,id)==publicationActor&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(publicationActor,"damagePoison"),false)==publicationEpoch&&(int)CombatAccess.field(publicationActor,"hp")>0;
   return canonical()&&life(false)==publicationEpoch&&dead()==publicationDead;
  }
 }
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  if(spell.skillId()!=165)throw new IllegalArgumentException("Call of Nature selector");
  var target=new Target(pc,Protocol.objectSpellRequest(packet).targetObjectId());
  NativeAreaMaterialAction.run(pc,165,()->{
   if(!target.scene.scene())return;
   if(target.kind==2)synchronized(target.actor){owned(target,pc,packet,spell);}else if(target.kind==4||target.kind==5)synchronized(target.owner){owned(target,pc,packet,spell);}else owned(target,pc,packet,spell);
  });
 }
 static void owned(Target target,ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  if(target.kind!=0&&!target.valid())return;
  var prior=current.get();current.set(target);
  try{pc.nativeMaterialCallNatureBody(packet,spell);}finally{target.finish();if(prior==null)current.remove();else current.set(prior);}
 }
 static boolean admit(ClientSession pc,int skill,int action,int reuse){var target=current.get();if(skill!=165||target==null||target.scene.pc!=pc||!target.valid())return false;return NativeLightAction.admitPair(pc,pc,skill,action,reuse);}
 static List<ClientSession> battleRecipients(GameHub hub,long room,int force){return NativeAreaMaterialAction.filter(hub.battleRecipients(room,force));}
 static void publish(ClientSession receiver,byte[] plain,String note){
  var target=current.get();if(target==null)throw new IllegalStateException("Call of Nature output outside owned cast");
  var binding=NativePlayerCommit.Binding.capture(receiver);byte[] frozen=plain.clone();long id=frozen.length<5?-1:((long)(frozen[1]&255)|((long)(frozen[2]&255)<<8)|((long)(frozen[3]&255)<<16)|((long)(frozen[4]&255)<<24));
  var output=NativeRolePublication.completion(receiver,()->{if(binding.matches(receiver)&&(id!=target.id||target.publication()))try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException e){throw new UncheckedIOException(e);}});
  NativeLightAction.finishScoped((accepted,failure)->output.run());
 }
 private NativeCallNatureMaterialAction(){}
}
