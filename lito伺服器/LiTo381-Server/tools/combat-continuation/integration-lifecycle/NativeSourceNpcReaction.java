package local.lito381.server;
/** NPC Counter Barrier/Mirror reaction only; no HP or retained role/actor state. */
final class NativeSourceNpcReaction {
 static int scope(NativeCombatAttacker.Ref ref,long id){
  if(ref==null||ref.kind()<2||ref.kind()>4)return -1;
  return id>0&&ref.id()==id?1:0;
 }
 static Integer gfx(int kind,Object snapshot){
  if(kind==2&&snapshot instanceof BattleCompanion.Snapshot s)return s.gfx();
  if(kind==3&&snapshot instanceof BattleQuestEncounter.Snapshot s)return s.gfx();
  if(kind==4&&snapshot instanceof BattleTimeAttackEncounter.Snapshot s)return s.gfx();
  return null;
 }
 static boolean allow(ClientSession defender,long id){
  if(!CombatRules.loaded()||!"BATTLE".equals(String.valueOf(CombatAccess.field(defender,"phase"))))return true;
  var ref=NativeCombatAttacker.current();int scope=scope(ref,id);if(scope<0)return true;if(scope==0||!NativePlayerCommit.validFrame(defender))return false;
  var validated=NativeCombatAttacker.nativeSourceNpcReactionLive(defender,ref);if(validated==null)return false;
  var gfx=gfx(ref.kind(),CombatAccess.field(validated,"snapshot"));return gfx!=null&&BattleHitReactionSuppressCatalog.shouldSendDamageAction(gfx);
 }
 private NativeSourceNpcReaction(){}
}
