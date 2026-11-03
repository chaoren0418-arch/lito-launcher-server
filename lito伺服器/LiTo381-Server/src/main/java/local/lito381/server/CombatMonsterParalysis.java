package local.lito381.server;

/** WORLD monster curse uses a separate transient status, never the poison slot. */
final class CombatMonsterParalysis {
 record Poll(BattleTimedStatus.ParalysisPoll poison,BattleTimedStatus.ParalysisPoll curse){
  boolean activated(){return poison.activated();}
  boolean expired(){return poison.expired()||curse.expired();}
  boolean poisonExpired(){return poison.expired();}
 }
 private static BattleTimedStatus slot(BattleTimedStatus t,boolean create){return CombatPoisonLifecycle.monsterCurse(t,create);}
 static boolean activate(BattleTimedStatus t,long now,int warning,int duration){if(!CombatRules.loaded())return t.activateParalysis(now,warning,duration);synchronized(t){if(CombatMonsterCurseLifecycle.reserved(t))return false;return slot(t,true).activateParalysis(now,warning,duration);}}
 static boolean curseVisual(BattleTimedStatus t,long now){if(!CombatRules.loaded())return t.paralysisVisualActive(now);synchronized(t){var s=slot(t,false);return s!=null&&CombatMonsterCurseLifecycle.visual(t,s,now);}}
 static boolean curseActive(BattleTimedStatus t,long now){if(!CombatRules.loaded())return false;synchronized(t){var s=slot(t,false);return s!=null&&CombatMonsterCurseLifecycle.active(t,s,now);}}
 static boolean poisonVisual(BattleTimedStatus t,long now){if(!CombatRules.loaded())return t.paralysisVisualActive(now);synchronized(t){return CombatMonsterCurseLifecycle.poisonParalysisVisual(t)||((Number)CombatAccess.field(t,"paralysisActivatesAtNanos")).longValue()!=0&&now<((Number)CombatAccess.field(t,"paralysisExpiresAtNanos")).longValue();}}
 static boolean paletteFirst(boolean freeze,BattleTimedStatus t,long now){return freeze||CombatRules.loaded()&&curseVisual(t,now);}
 static Poll poll(BattleTimedStatus t,long now){synchronized(t){var s=CombatRules.loaded()?slot(t,false):null;var curse=s==null?new BattleTimedStatus.ParalysisPoll(false,false,false):CombatMonsterCurseLifecycle.poll(t,s,now);if(curse.expired()&&!CombatMonsterCurseLifecycle.reserved(t))CombatPoisonLifecycle.discardMonsterCurse(t);return new Poll(CombatMonsterCurseLifecycle.poisonParalysisPoll(t,now),curse);}}
 static boolean poisonEvent(boolean original,Poll result){return original&&(result.poison.activated()||result.poison.expired());}
 static BattleTimedStatus.ParalysisCancellation cancelCurse(BattleTimedStatus t){if(!CombatRules.loaded())return t.cancelParalysis();synchronized(t){if(CombatMonsterCurseLifecycle.reserved(t))return CombatMonsterCurseLifecycle.cancel(t);var s=slot(t,false);if(s==null)return new BattleTimedStatus.ParalysisCancellation(false,false);var result=s.cancelParalysis();CombatPoisonLifecycle.discardMonsterCurse(t);return result;}}
 static BattleTimedStatus.MagicCancellation cancelMagic(BattleTimedStatus t){synchronized(t){boolean repeated=CombatMonsterCurseLifecycle.poisonClosing(t);var result=t.cancelMagicEffects();CombatMonsterCurseLifecycle.cancelPoisonMagic(t);return CombatRules.loaded()?CombatPoisonParalysis.magic(CombatMonsterCurseLifecycle.magicResult(result,repeated),cancelCurse(t)):result;}}
 static BattleTimedStatus.MagicCancellation resetMagic(BattleTimedStatus t){var result=cancelMagic(t);CombatMonsterCurseLifecycle.reset(t);return result;}
 static boolean legacyPoisonCancellation(BattleTimedStatus.ParalysisCancellation result){return !CombatRules.loaded()&&result.present();}
}
