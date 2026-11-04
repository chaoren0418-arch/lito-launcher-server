// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-material/NativeCancellationBattleAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;

/** Capture a battle NPC before waiting; player cancellation retains its qualified path. */
final class NativeCancellationBattleAction {
 static final ThreadLocal<Target> current=new ThreadLocal<>();
 record Target(NativeAreaMaterialAction.Context scene,long id,NativeAreaMaterialAction.Npc npc) {
  boolean valid(){return scene.scene()&&scene.npc(id);}
  void refresh(){
   var owner=NativeAreaMaterialAction.current.get();
   if(owner!=null&&scene.encounter(npc.kind())==npc.owner()&&scene.monster(npc.owner(),npc.kind(),id)==npc.actor()&&(int)CombatAccess.field(npc.actor(),"hp")>0){
    var epoch=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(npc.actor(),"damagePoison"),true);
    owner.npcs.put(id,new NativeAreaMaterialAction.Npc(npc.kind(),id,npc.owner(),npc.actor(),epoch));
   }
  }
 }
 static boolean cast(ClientSession pc,byte[] packet,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException {
  if(spell.skillId()!=44||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE"))return false;
  long id=Protocol.objectSpellRequest(packet).targetObjectId();var scene=new NativeAreaMaterialAction.Context(pc);
  if(scene.players.stream().anyMatch(p->p.playerObjectId()==id))return false;
  var npc=scene.npcs.get(id);if(npc==null||(npc.kind()!=1&&npc.kind()!=2))return false;
  var target=new Target(scene,id,npc);
  NativeAreaMaterialAction.run(pc,44,()->{synchronized(npc.owner()){
   if(!target.valid())return;
   var previous=current.get();current.set(target);
   try{pc.nativeMaterialCancellationBody(packet,spell,scope);}finally{target.refresh();if(previous==null)current.remove();else current.set(previous);}
  }});
  return true;
 }
 static List<ClientSession> roomCombatants(GameHub hub,long room){return current.get()==null?hub.roomCombatants(room):NativeAreaMaterialAction.roomCombatants(hub,room);}
 static List<ClientSession> roomMembers(GameHub hub,long room){return current.get()==null?hub.roomMembers(room):NativeAreaMaterialAction.battleMembers(hub,room);}
 static List<ClientSession> battleRecipients(GameHub hub,long room,int force){var members=hub.battleRecipients(room,force);return current.get()==null?members:NativeAreaMaterialAction.filter(members);}
 static List<BattleQuestEncounter.Snapshot> visibleQuestMonsters(GameHub hub,long room,ClientSession pc){return current.get()==null?hub.visibleQuestMonsters(room,pc):NativeAreaMaterialAction.questNpcs(hub,room,pc);}
 static List<BattleTimeAttackEncounter.Snapshot> visibleTimeAttackActors(GameHub hub,long room,ClientSession pc){return current.get()==null?hub.visibleTimeAttackActors(room,pc):NativeAreaMaterialAction.timeNpcs(hub,room,pc);}
 private NativeCancellationBattleAction(){}
}
