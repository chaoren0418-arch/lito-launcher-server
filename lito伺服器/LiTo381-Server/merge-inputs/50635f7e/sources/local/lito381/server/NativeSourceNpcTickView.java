package local.lito381.server;
/** The admitted original weak view survives tick/Ref creation; post-action pose may legitimately change. */
final class NativeSourceNpcTickView {
 static boolean same(NativeSourceNpcFirstView.View v,Object encounter,Object actor){
  return actor!=null&&Thread.holdsLock(encounter)&&v.owner().get()==encounter&&v.actor().get()==actor&&v.generation()==CombatAccess.field(encounter,"sourceProcEncounterGeneration")&&NativeSourceNpcFactoryType.stamp(actor)==v.stamp()&&NativeSourceMonsterHp.life(actor)==v.life()&&(long)CombatAccess.field(actor,"objectId")==v.id()&&NativeSourceNpcFactoryMemory.npc(actor)==v.npc()&&(v.family()!=1||(int)CombatAccess.field(actor,"force")==v.force());
 }
 static boolean encounter(Object actor,GameHub.MonsterAiTick tick,Object encounter){
  if(actor==null)return false;Object raw=tick.nativeSourceNpcTickView;if(raw==null)return true;
  if(!(raw instanceof NativeSourceNpcFirstView.View v))return false;
  int family=tick.mode()==BattleRules.Mode.QUEST_BATTLE?1:tick.mode()==BattleRules.Mode.TIME_ATTACK?2:0;
  return family==v.family()&&tick.objectId()==v.id()&&tick.force()==v.force()&&tick.sourceX()==v.x()&&tick.sourceY()==v.y()&&same(v,encounter,actor);
 }
 static boolean live(boolean original,NativeCombatAttacker.Ref ref,Object encounter,Object actor){
  if(!original)return false;Object raw=ref.nativeSourceNpcTickView;if(raw==null)return true;
  return raw instanceof NativeSourceNpcFirstView.View v&&ref.kind()==v.family()+2&&ref.id()==v.id()&&ref.owner()==v.life()&&ref.master()==null&&same(v,encounter,actor);
 }
 private NativeSourceNpcTickView(){}
}
