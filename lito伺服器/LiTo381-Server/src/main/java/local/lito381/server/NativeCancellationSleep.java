// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-probability/NativeCancellationSleep.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
/** Tag only actual PC skill producers; the existing lease remains the publication identity. */
final class NativeCancellationSleep {
 private record Origin(int skill,Object generation){}
 static boolean activate(BattleTimedStatus status,long now,int seconds,int skill){
  if(skill!=66&&skill!=212)throw new IllegalArgumentException("unreviewed sleep producer");
  synchronized(status){boolean active=status.activateSleep(now,seconds);if(active&&CombatRules.loaded())CombatAccess.set(status,"sourceSleepLease",new Origin(skill,CombatAccess.field(status,"sourceSleepLease")));return active;}
 }
 static void removeFog(ClientSession pc){
  var status=(BattleTimedStatus)CombatAccess.field(pc,"timedStatus");Object released;
  synchronized(status){var token=CombatAccess.field(status,"sourceSleepLease");if(!(token instanceof Origin origin)||origin.skill()!=66||!status.cancelSleep())return;released=CombatAccess.field(status,"sourceSleepLease");}
  NativeLightAction.deferScoped(()->{synchronized(status){if(CombatAccess.field(status,"sourceSleepLease")!=released)return;}NativeCancellationProbability.send(pc,Protocol.paralysis(Protocol.ParalysisMode.SLEEP_RELEASE),"S_PARALYSE Cancellation probability fog-release");});
 }
 private NativeCancellationSleep(){}
}
