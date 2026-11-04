package local.lito381.server;
/** Explicit NPC cancellation extends the existing bounded status and control list. */
final class NativeCompanionForestCancellation {
 static void cancel(BattleCompanion pet){
  CombatNpcDebuffs.cancel(pet);
  var controls=CombatDatabase.controlsOrNull();
  if(controls!=null&&!controls.cancellationImmune().contains(161))
   ((BattleTimedStatus)CombatAccess.field(pet,"timedStatus")).expireAreaSilence(Long.MAX_VALUE);
 }
 private NativeCompanionForestCancellation(){}
}
