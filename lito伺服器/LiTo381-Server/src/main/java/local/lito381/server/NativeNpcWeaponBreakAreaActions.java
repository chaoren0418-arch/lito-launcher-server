package local.lito381.server;
import java.io.IOException;import java.util.*;import java.util.concurrent.locks.LockSupport;
/** NPC physical-area producer reuses the existing frozen membership, locks and output context. */
final class NativeNpcWeaponBreakAreaActions {
 static void run(ClientSession pc,ClassicWorldMonsterPopulation.PhysicalSkillAttack attack)throws IOException{
  if(!CombatRules.loaded()){pc.nativeNpcWeaponPhysicalBody(attack);return;}if(attack==null)return;
  var context=new NativeAreaMaterialAction.Context(pc);if(!context.phase.equals("WORLD")||attack.attacker().mapId()!=context.map)return;
  var locks=context.players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();int acquired=0;
  for(int attempt=0;attempt<32;attempt++){
   if(Thread.currentThread().isInterrupted()||!context.caster.matches(pc))return;
   for(;acquired<locks.size();acquired++)if(!locks.get(acquired).tryLock())break;
   if(acquired==locks.size())break;
   while(acquired>0)locks.get(--acquired).unlock();
   if(Thread.currentThread().isInterrupted()||!context.caster.matches(pc))return;
   LockSupport.parkNanos(1_000_000L);
  }
  if(acquired!=locks.size())return;
  var scope=NativePlayerActionScope.enter(context.players.toArray(ClientSession[]::new));
  try{
   if(Thread.currentThread().isInterrupted()||!context.scene())return;
   NativeLightAction.commitPair(pc,pc,()->{
    synchronized(context.hub){
     if(!context.scene()||!NativeCombatOrigin.validWorldAction(pc,attack))return;
     if(context.population==null)return;
     synchronized(context.population){
      if(!context.scene()||!NativeCombatOrigin.validWorldAction(pc,attack))return;
      var origin=NativeCombatOrigin.enterWorldAction(pc,attack);
      try{NativeAreaMaterialAction.owned(context,()->pc.nativeNpcWeaponPhysicalBody(attack));}
      finally{NativeCombatOrigin.leaveWorldAction(origin);}
     }
    }
   });
  }finally{
   while(acquired>0)locks.get(--acquired).unlock();
   for(var member:context.players)NativePlayerActionScope.unlocked(member);
   scope.close();
  }
 }
 @SuppressWarnings("unchecked") static List<ClientSession> members(ClientSession pc,int map){
  var live=(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",map);
  var context=NativeAreaMaterialAction.current.get();if(context==null){NativeAreaMaterialAction.current.remove();return live;}
  return live.stream().filter(context::player).toList();
 }
 static BattleCompanion companion(ClientSession pc,long id){
  var live=(BattleCompanion)CombatAccess.call(pc,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",id);
  var context=NativeAreaMaterialAction.current.get();if(context==null){NativeAreaMaterialAction.current.remove();return live;}
  var captured=context.npcs.get(id);return captured!=null&&captured.kind()==3&&captured.owner()==pc&&captured.actor()==live&&context.npc(id)?live:null;
 }
 private NativeNpcWeaponBreakAreaActions(){}
}
