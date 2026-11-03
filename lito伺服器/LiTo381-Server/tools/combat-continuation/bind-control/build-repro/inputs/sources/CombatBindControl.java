package local.lito381.server;
/** PCs remain able to act while Thunder Grab binds movement; NPC paralysis is unchanged. */
final class CombatBindControl {
 static boolean actionLocked(BattleTimedStatus state,long now){
  if(!CombatRules.loaded())return state.actionLocked(now);
  synchronized(state){return state.sleepActive(now)||state.freezeVisualLockActive(now)||state.paralysisActive(now)||state.shockStunActive(now)||state.boneBreakActive(now);}
 }
 static boolean blocked(ClientSession pc){return CombatRules.number("SLAY_BREAK_NOT_TELEPORT",1)!=0&&((BattleTimedStatus)CombatAccess.field(pc,"timedStatus")).thunderGrabActive(System.nanoTime());}
 static void rejected(ClientSession pc){
  send(pc,Protocol.manaPoint((int)CombatAccess.field(pc,"playerMp"),(int)CombatAccess.field(pc,"playerMaxMp")),"S_MANA_POINT Thunder-Grab accepted teleport cost");
  send(pc,Protocol.systemMessage(0,"\\fY身上有奪命之雷的效果無法瞬移"),"S_MESSAGE Thunder-Grab teleport disabled");
  send(pc,Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK),"S_PARALYSE Thunder-Grab teleport unlock");
 }
 private static void send(ClientSession pc,byte[] data,String note){CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",data,note);}
}
