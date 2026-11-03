package local.lito381.server;
/** Pure poison state transactions share the existing status monitor; no owned cache. */
final class CombatPoisonAtomic {
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;";
 record NpcState(ClassicWorldMonsterPoisonAttack.Kind kind,int roll,boolean resistance,boolean existing,boolean applied){}
 static BattleTimedStatus status(ClientSession pc){return (BattleTimedStatus)CombatAccess.field(pc,"timedStatus");}
 static boolean occupied(ClientSession pc,long now){var t=status(pc);return CombatPoisonLifecycle.reserved(t)||t.poisonActive(now)||t.poisonSilenceActive()||CombatAccess.field(pc,"classicWorldPoisonKind")==ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS&&CombatPoisonParalysis.poisonVisual(t,now);}
 static void clear(ClientSession pc){CombatAccess.set(pc,"poisonAttacker",null);CombatAccess.set(pc,"classicWorldPoisonAttackerObjectId",0L);CombatAccess.set(pc,"classicWorldPoisonKind",ClassicWorldMonsterPoisonAttack.Kind.NONE);}
 private static NpcState nativeState(ClientSession pc,long object,int npc,int poison,int paralysis,long now,String source){return (NpcState)CombatAccess.call(pc,"sourceNpcPoisonState","(JIIIJLjava/lang/String;)L"+P+"CombatPoisonAtomic$NpcState;",object,npc,poison,paralysis,now,source);}
 static NpcState npc(ClientSession pc,long object,int npc,int poison,int paralysis,long now,String source){
  return npc(pc,object,npc,poison,paralysis,now,source,null);
 }
 static NpcState npc(ClientSession pc,long object,int npc,int poison,int paralysis,long now,String source,CombatPoisonLifecycle.Scope scope){
  if(!CombatRules.loaded())return nativeState(pc,object,npc,poison,paralysis,now,source);
  synchronized(status(pc)){var result=nativeState(pc,object,npc,poison,paralysis,now,source);if(result.applied()){CombatAccess.set(pc,"poisonAttacker",null);CombatPoisonLifecycle.installed(pc,scope,now);}return result;}
 }
 static boolean player(BattleTimedStatus t,long now,int seconds,int interval,ClientSession pc,ClientSession caster){
  return player(t,now,seconds,interval,pc,caster,null);
 }
 static boolean player(BattleTimedStatus t,long now,int seconds,int interval,ClientSession pc,ClientSession caster,CombatPoisonLifecycle.Scope scope){
  if(!CombatRules.loaded())return t.activatePoison(now,seconds,interval);
  synchronized(t){if(occupied(pc,now))return false;boolean applied=t.activatePoison(now,seconds,interval);if(applied){clear(pc);CombatAccess.set(pc,"poisonAttacker",caster);CombatPoisonLifecycle.installed(pc,scope,now);}return applied;}
 }
 static BattleTimedStatus.PoisonPoll poll(BattleTimedStatus t,long now,int interval,ClientSession pc){return CombatPoisonLifecycle.poll(t,now,interval,pc,null);}
 static BattleTimedStatus.PoisonPoll nativePoll(BattleTimedStatus t,long now,int interval,ClientSession pc){
  if(!CombatRules.loaded())return t.pollPoison(now,interval);
  synchronized(t){var result=t.pollPoison(now,interval);if(result.expired())clear(pc);return result;}
 }
 // The original producers/curatives still contain assignment opcodes after the
 // transaction. Keep legacy writes when unloaded; never let late clears erase
 // the provenance of a newly installed poison.
 static void attacker(ClientSession pc,ClientSession value){if(!CombatRules.loaded()){CombatAccess.set(pc,"poisonAttacker",value);return;}synchronized(status(pc)){if(value==null&&!occupied(pc,System.nanoTime()))CombatAccess.set(pc,"poisonAttacker",null);}}
 static void object(ClientSession pc,long value){if(!CombatRules.loaded()){CombatAccess.set(pc,"classicWorldPoisonAttackerObjectId",value);return;}synchronized(status(pc)){if(value!=0||!occupied(pc,System.nanoTime()))CombatAccess.set(pc,"classicWorldPoisonAttackerObjectId",value);}}
 static void kind(ClientSession pc,ClassicWorldMonsterPoisonAttack.Kind value){if(!CombatRules.loaded()){CombatAccess.set(pc,"classicWorldPoisonKind",value);return;}synchronized(status(pc)){if(value!=ClassicWorldMonsterPoisonAttack.Kind.NONE||!occupied(pc,System.nanoTime()))CombatAccess.set(pc,"classicWorldPoisonKind",value);}}
}
