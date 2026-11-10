package local.lito381.server;
/** Consume the original first view under the original synchronized AI/move entry monitor. */
final class NativeSourceNpcViewAction {
 static boolean allowed(Object encounter,long id,Object snapshot){
  Object raw=NativeSourceNpcFirstView.raw(snapshot);
  if(raw==null)return true; // Old canonical constructors are explicitly unqualified fallback.
  if(!(raw instanceof NativeSourceNpcFirstView.View v)||!Thread.holdsLock(encounter))return false;
  boolean quest=encounter instanceof BattleQuestEncounter;
  Object actor=CombatAccess.call(encounter,"monster","(J)Llocal/lito381/server/"+(quest?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);
  return NativeSourceNpcFirstView.accepted(snapshot,actor,encounter,quest?BattleRules.Mode.QUEST_BATTLE:BattleRules.Mode.TIME_ATTACK,id,v.force(),v.x(),v.y());
 }
 private NativeSourceNpcViewAction(){}
}
