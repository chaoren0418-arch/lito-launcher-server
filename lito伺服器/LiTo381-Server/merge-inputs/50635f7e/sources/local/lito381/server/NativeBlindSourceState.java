package local.lito381.server;
/** CURSE_BLIND, DARKNESS and DARK_BLIND install the Source DARKNESS(40) lease. */
final class NativeBlindSourceState {
 static boolean protectedLease(){var controls=CombatDatabase.controlsOrNull();return controls!=null&&controls.cancellationImmune().contains(40);}
 static boolean removed(boolean active){return active&&!protectedLease();}
 static long cleared(long original){return protectedLease()?original:0L;}
 static void cancel(BattleCompanion pet){if(!Thread.holdsLock(pet))throw new IllegalStateException("blind cancellation requires actor ownership");if(!protectedLease())((BattleTimedStatus)CombatAccess.field(pet,"timedStatus")).expireBlind(Long.MAX_VALUE);}
 private NativeBlindSourceState(){}
}
