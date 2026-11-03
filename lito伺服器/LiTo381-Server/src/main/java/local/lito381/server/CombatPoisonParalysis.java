package local.lito381.server;

/** Independent poison paralysis, owned only by the existing transient status container. */
final class CombatPoisonParalysis {
 private static final String FIELD="sourcePoisonParalysis";
 private static BattleTimedStatus slot(BattleTimedStatus t,boolean create){
  var value=(BattleTimedStatus)CombatAccess.field(t,FIELD);
  if(value==null&&create){value=new BattleTimedStatus();CombatAccess.set(t,FIELD,value);}return value;
 }
 static void discard(BattleTimedStatus t){CombatAccess.set(t,FIELD,null);CombatMonsterCurseLifecycle.reset(t);}
 private static long time(BattleTimedStatus t,String name){return ((Number)CombatAccess.field(t,name)).longValue();}
 static boolean curseVisual(BattleTimedStatus t,long now){return CombatCurseLifecycle.visual(t,now);}
 static boolean curseActive(BattleTimedStatus t,long now){return CombatCurseLifecycle.active(t,now);}
 static boolean otherVisual(boolean legacy,BattleTimedStatus t,long now){if(!CombatRules.loaded())return legacy;synchronized(t){var s=slot(t,false);return CombatMonsterCurseLifecycle.poisonParalysisVisual(t)||CombatMonsterParalysis.curseVisual(t,now)||curseVisual(t,now)||s!=null&&CombatPoisonLifecycle.paralysisVisual(t,s,now);}}
 static boolean otherActive(boolean legacy,BattleTimedStatus t,long now){if(!CombatRules.loaded())return legacy;synchronized(t){var s=slot(t,false);return CombatMonsterParalysis.curseActive(t,now)||!CombatMonsterCurseLifecycle.managedPoisonParalysis(t)&&curseActive(t,now)||s!=null&&CombatPoisonLifecycle.paralysisActive(t,s,now);}}
 static boolean poisonVisual(BattleTimedStatus t,long now){if(!CombatRules.loaded())return t.paralysisVisualActive(now);synchronized(t){var s=slot(t,false);return s!=null&&CombatPoisonLifecycle.paralysisVisual(t,s,now);}}
 static boolean activate(BattleTimedStatus t,long now,int warning,int duration){if(!CombatRules.loaded())return t.activateParalysis(now,warning,duration);synchronized(t){return slot(t,true).activateParalysis(now,warning,duration);}}
 static boolean note(BattleTimedStatus t,long now){if(!CombatRules.loaded())return t.noteParalysisLockSent(now);synchronized(t){boolean own=curseActive(t,now)&&t.noteParalysisLockSent(now);var s=slot(t,false);boolean poison=s!=null&&CombatPoisonLifecycle.paralysisActive(t,s,now);if(poison)CombatAccess.set(s,"paralysisLockEmitted",true);return poison|own;}}
 static boolean note(BattleTimedStatus t,long now,ClientSession pc,CombatPoisonLifecycle.Scope scope){if(!CombatRules.loaded())return t.noteParalysisLockSent(now);boolean own=CombatCurseLifecycle.note(t,now,pc,scope.curses());synchronized(t){var s=slot(t,false);boolean poison=s!=null&&CombatPoisonLifecycle.paralysisActive(t,s,now);if(poison)CombatAccess.set(s,"paralysisLockEmitted",true);return own|poison;}}
 static BattleTimedStatus.ParalysisPoll poll(BattleTimedStatus t,long now,ClientSession pc){
  return CombatPoisonLifecycle.paralysisPoll(t,now,pc,null);
 }
 static BattleTimedStatus.ParalysisPoll nativePoll(BattleTimedStatus t,long now,ClientSession pc){
  return CombatRules.loaded()?CombatPoisonLifecycle.paralysisPoll(t,now,pc,null):t.pollParalysis(now);
 }
 static BattleTimedStatus.ParalysisPoll nativePoll(BattleTimedStatus t,long now,ClientSession pc,CombatPoisonLifecycle.Scope scope){
  if(!CombatRules.loaded())return t.pollParalysis(now);
  synchronized(t){var own=CombatCurseLifecycle.poll(t,now,pc,scope.curses());var s=slot(t,false);var poison=s==null?new BattleTimedStatus.ParalysisPoll(false,false,false):CombatPoisonLifecycle.paralysisStep(t,s,now);if(poison.expired()){discard(t);CombatPoisonAtomic.clear(pc);}
   return new BattleTimedStatus.ParalysisPoll(own.activated()||poison.activated(),own.expired()||poison.expired(),(own.unlockRequired()||poison.unlockRequired())&&!t.paralysisActive(now));}
 }
 static BattleTimedStatus.ParalysisCancellation cancelPoison(BattleTimedStatus t){
  synchronized(t){var s=slot(t,false);if(s==null)return new BattleTimedStatus.ParalysisCancellation(false,false);var result=s.cancelParalysis();discard(t);return result;}
 }
 static BattleTimedStatus.ParalysisCancellation merge(BattleTimedStatus.ParalysisCancellation a,BattleTimedStatus.ParalysisCancellation b){return new BattleTimedStatus.ParalysisCancellation(a.present()||b.present(),a.unlockRequired()||b.unlockRequired());}
 static BattleTimedStatus.ParalysisCancellation cancelAll(BattleTimedStatus t){synchronized(t){var a=t.cancelParalysis();return CombatRules.loaded()?merge(a,cancelPoison(t)):a;}}
 static BattleTimedStatus.ParalysisCancellation cancelNpc(BattleTimedStatus t,ClientSession pc){return CombatRules.loaded()?CombatNpcPoison.cureNpcParalysis(pc):CombatNpcPoison.cancelParalysis(t,pc);}
 static BattleTimedStatus.MagicCancellation magic(BattleTimedStatus.MagicCancellation a,BattleTimedStatus.ParalysisCancellation b){
  if(!b.present())return a;
  return new BattleTimedStatus.MagicCancellation(a.haste(),a.slow(),a.brave(),a.blind(),a.weakness(),a.disease(),a.silence(),a.decayPotion(),a.polluteWater(),a.strikerGale(),a.light(),a.bluePotion(),a.wisdomPotion(),a.meditation(),a.concentration(),a.counterMagic(),a.breath(),a.poison(),a.sleep(),a.freeze(),a.earthBind(),a.burningSpirit(),a.elementalFire(),a.soulOfFlame(),a.venomResist(),a.doubleBrake(),a.horrorOfDeath(),a.thunderGrab(),a.boneBreak(),merge(a.paralysisCancellation(),b));
 }
 static boolean paletteFirst(boolean freeze,BattleTimedStatus t,long now){return freeze||CombatRules.loaded()&&(curseVisual(t,now)||CombatPoisonLifecycle.paralysisGray(t));}
 static void release(ClientSession pc,byte[] packet,String reason){
  releaseAt(pc,packet,reason,System.nanoTime());
 }
 static void releaseAt(ClientSession pc,byte[] packet,String reason,long now){
  CombatPoisonLifecycle.paralysisRelease(pc,packet,reason,now);
 }
}
