package local.lito381.server;
/** Lazy actor-owned lease. The original nextActionNanos remains the only AI clock. */
final class NativeCompanionWindState {
 static final String FIELD="nativeCompanionWindStatus";
 static BattleWindShackleStatus state(BattleCompanion pet,boolean create){if(!Thread.holdsLock(pet))throw new IllegalStateException("NPC wind state requires actor ownership");var s=(BattleWindShackleStatus)CombatAccess.field(pet,FIELD);if(s==null&&create){s=new BattleWindShackleStatus();CombatAccess.set(pet,FIELD,s);}return s;}
 static boolean activate(BattleCompanion pet,long now,int seconds){return state(pet,true).activate(now,seconds);}
 static boolean active(BattleCompanion pet,long now){synchronized(pet){var s=state(pet,false);return s!=null&&s.snapshot(now).active();}}
 static int interval(int original,BattleCompanion pet,long now){var s=state(pet,false);return s!=null&&s.snapshot(now).active()?Math.max(1,(int)(original*1.25d)):original;}
 static void clear(BattleCompanion pet){var s=state(pet,false);if(s!=null)s.cancel();CombatAccess.set(pet,FIELD,null);}
 static void cancel(BattleCompanion pet){var controls=CombatDatabase.controlsOrNull();if(controls!=null&&!controls.cancellationImmune().contains(167))clear(pet);}
 private NativeCompanionWindState(){}
}
