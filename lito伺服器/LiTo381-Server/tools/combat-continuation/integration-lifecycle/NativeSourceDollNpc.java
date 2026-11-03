package local.lito381.server;
import java.io.*;import java.util.*;
/** Accepted natural/owned-NPC doll proc routing. The synchronous scope is never stored in an HP/display event. */
final class NativeSourceDollNpc {
 static final String P="local/lito381/server/",FIELD="nativeSourceDollNpcScope";
 static final class Scope {
  final NativeAreaMaterialAction.Context c;final NativeSourceDamageDisplay.Actor caster;final NativeSourceMonsterHp.Aim aim;final NativeSourceControlledDisplay.Target controlled;final SourceNpcPhysicalStats stats;
  NativeSourceNpcDamageDisplay.Proof natural;
  NativeSourceNpcDamageDisplay.Receipt baseReceipt;NativeMagicDollEffects.AttackProc pendingProc;boolean baseAccepted;
  Scope(NativeSourceMonsterHp.Aim aim,Object life,SourceNpcPhysicalStats stats){this.c=aim.c();this.caster=aim.caster();this.aim=aim;this.controlled=null;this.stats=stats;natural=new NativeSourceNpcDamageDisplay.Proof(aim.npc().actor(),aim.npc().owner(),aim.generation(),life,aim.x(),aim.y(),true);}
  Scope(NativeSourceControlledDisplay.Target t,SourceNpcPhysicalStats stats){c=t.action.c;caster=t.action.caster;aim=null;controlled=t;this.stats=stats;}
  long id(){return controlled==null?aim.npc().id():controlled.npc.id();}
  boolean current(){return NativePlayerLifetime.of(c.hub).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")&&c.scene()&&caster.valid(caster.life())&&NativeSourceDamageDisplay.n(c.pc,"playerHp")>0&&(controlled==null?natural.current():controlled.output().current());}
  boolean alive(){return current()&&(int)CombatAccess.field(controlled==null?aim.npc().actor():controlled.pet,"hp")>0;}
  NativeSourceNpcDamageDisplay.Proof proof(){return controlled==null?natural:controlled.display(controlled.output());}
  NativeSourceMonsterHp.Aim nextAim(){var n=aim.npc();return new NativeSourceMonsterHp.Aim(c,new NativeAreaMaterialAction.Npc(n.kind(),n.id(),n.owner(),n.actor(),natural.life()),aim.cache(),caster,aim.generation(),aim.x(),aim.y());}
 }
 static Scope current(ClientSession caster){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.pc==caster&&c.nativeSourceDollNpcScope instanceof Scope s?s:null;}
 static void owned(Scope s,NativeSelfCastAction.Task task)throws IOException{var prior=s.c.nativeSourceDollNpcScope;s.c.nativeSourceDollNpcScope=s;try{NativeAreaMaterialAction.owned(s.c,task);}finally{s.c.nativeSourceDollNpcScope=prior;}}
 static void invoke(Scope s,NativeMagicDollEffects.AttackProc proc)throws IOException{
  if(proc==null||!s.current())return;var active=s.alive()?proc:new NativeMagicDollEffects.AttackProc(proc.additionalDamage(),proc.damageTriggers(),List.of());
  Object q=null,t=null;if(s.controlled==null){var n=s.aim.npc();var snapshot=CombatAccess.call(n.actor(),"snapshot","()L"+P+(n.kind()==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;");if(n.kind()==1)q=snapshot;else t=snapshot;}
  final Object quest=q,time=t;owned(s,()->{if(s.current())CombatAccess.call(s.c.pc,"applyNativeMagicDollAttackProcs","(L"+P+"NativeMagicDollEffects$AttackProc;JL"+P+"ClientSession;L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;L"+P+"SourceNpcPhysicalStats;)V",active,s.id(),null,quest,time,s.stats);});
 }
 static void failed(ClientSession caster){NativeSourceNpcDamageDisplay.failed(caster);}
 static int natural(ClientSession caster,long id,int damage,String source,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome maximum,BattleWeaponCritical.DamageOutcome multiplier,NativeSourceMonsterHp.Aim aim,NativeMagicDollEffects.AttackProc proc,SourceNpcPhysicalStats stats,boolean quest)throws IOException{
  if(!NativeSourceDollCommit.enabled(caster))return quest?NativeSourceNpcDamageDisplay.physicalQuest(caster,id,damage,source,weapon,maximum,multiplier,aim):NativeSourceNpcDamageDisplay.physicalTime(caster,id,damage,source,weapon,maximum,multiplier,aim);
  if(aim==null||aim.npc().id()!=id||aim.npc().kind()!=(quest?1:2))return 0;
  final int[] result={0};var lifetime=NativePlayerLifetime.of(aim.c().hub);
  try{NativeSourcePlayerActionLock.runPairOrdered(caster,caster,()->{synchronized(lifetime.admission){if(lifetime.mode!=NativePlayerLifetime.Mode.OPEN||!aim.caster().valid(aim.caster().life()))return;try{NativeLightAction.commitSelf(caster,()->{
   var receipt=new NativeSourceNpcDamageDisplay.Receipt(aim,caster,damage,1,NativeSourceDamageDisplay.critical(weapon.weaponType(),maximum.effectEnabled(),multiplier.effectEnabled()));if(!receipt.before())return;
   var scope=new Scope(aim,aim.npc().epoch(),stats);scope.baseReceipt=receipt;scope.pendingProc=proc;
   try{owned(scope,()->result[0]=(int)CombatAccess.call(caster,quest?"nativeSourceHpOriginalQuestDamage":"nativeSourceHpOriginalTimeDamage","(JILjava/lang/String;L"+P+"NativeSourceNpcDamageDisplay$Receipt;)I",id,damage,source,receipt));}
   finally{if(receipt.accepted&&receipt.current()){
    if(scope.baseAccepted){var acceptedProc=scope.pendingProc;scope.pendingProc=null;try{invoke(scope,acceptedProc);}catch(IOException|RuntimeException failure){failed(caster);}finally{if(receipt.visual!=null)receipt.visual.npc=scope.natural;}}
    try{NativeSourceMonsterHp.publish(aim);}catch(RuntimeException failure){failed(caster);}
    try{NativeSourceDamageDisplay.publish(receipt.visual);}catch(RuntimeException failure){failed(caster);}
   }}
  });}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}return result[0];
 }
 static int physicalQuest(ClientSession c,long id,int d,String src,SourceWeaponPhysicalStats w,BattleWeaponCritical.RollOutcome max,BattleWeaponCritical.DamageOutcome mult,NativeSourceMonsterHp.Aim aim,NativeMagicDollEffects.AttackProc proc,SourceNpcPhysicalStats stats)throws IOException{return natural(c,id,d,src,w,max,mult,aim,proc,stats,true);}
 static int physicalTime(ClientSession c,long id,int d,String src,SourceWeaponPhysicalStats w,BattleWeaponCritical.RollOutcome max,BattleWeaponCritical.DamageOutcome mult,NativeSourceMonsterHp.Aim aim,NativeMagicDollEffects.AttackProc proc,SourceNpcPhysicalStats stats)throws IOException{return natural(c,id,d,src,w,max,mult,aim,proc,stats,false);}
 /** Seal provenance immediately after Hub unlock; side effects wait for the original hit publisher to finish. */
 static void acceptedNatural(NativeSourceNpcDamageDisplay.Receipt receipt){
  if(receipt==null||!receipt.accepted)return;var scope=current(receipt.aim.c().pc);if(scope==null||scope.baseReceipt!=receipt)return;
  scope.baseReceipt=null;scope.natural=receipt.proof(true);scope.baseAccepted=true;
 }
 static void submitted(ClientSession c,NativeMagicDollEffects.AttackProc proc,long id,ClientSession target,BattleQuestEncounter.Snapshot q,BattleTimeAttackEncounter.Snapshot t,SourceNpcPhysicalStats stats,NativeSourceKiringkuAdmission.Aim aim)throws IOException{
  if(NativeSourceDollCommit.enabled(c)&&(q!=null||t!=null))return;NativeSourceDollAccepted.submitted(c,proc,id,target,q,t,stats,aim);
 }
 static BattleCompanion.Damage controlled(ClientSession owner,BattleCompanion pet,int damage,List<ClientSession> members,String source,ClientSession caster,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome max,BattleWeaponCritical.DamageOutcome mult,NativeMagicDollEffects.AttackProc proc,SourceNpcPhysicalStats stats)throws IOException{
  if(!NativeSourceDollCommit.enabled(caster))return NativeSourceControlledDisplay.physical(owner,pet,damage,members,source,caster,weapon,max,mult);
  return NativeSourceControlledDisplay.nativeSourceDollPhysical(owner,pet,damage,members,source,caster,weapon,max,mult,proc,stats);
 }
 static void controlledAccepted(NativeSourceControlledDisplay.Action a,NativeSourceControlledDisplay.Target t,NativeSourceDamageDisplay.Plan physical,NativeMagicDollEffects.AttackProc proc,SourceNpcPhysicalStats stats){
  try{invoke(new Scope(t,stats),proc);}catch(IOException|RuntimeException failure){failed(a.c.pc);}finally{NativeSourceControlledDisplay.publish(a,t,physical);}
 }
 static int skillDamage(ClientSession caster,SourceMagicDollPower power,ClientSession player,SourceNpcPhysicalStats stats){
  var s=current(caster);if(s!=null&&!s.alive())return 0;
  if(s==null||s.controlled==null)return(int)CombatAccess.call(caster,"nativeMagicDollTriggeredSkillDamage","(L"+P+"SourceMagicDollPower;L"+P+"ClientSession;L"+P+"SourceNpcPhysicalStats;)I",power,player,stats);
  var spell=BattleSpellCatalog.areaAttackSpell(power.triggerSkillId());if(spell==null)return 0;
  int source=(int)CombatAccess.call(caster,"rollAreaSpellSourceDamage","(L"+P+"BattleSpellCatalog$AreaAttackSpell;)I",spell);int mr=s.stats==null?0:s.stats.magicResistance(),armor=s.stats==null?0:s.stats.damageReduction();
  return CombatCompanionDamage.area(caster,spell,source,0,mr,armor,"magic-doll-proc",s.controlled.pet);
 }
 static void commit(ClientSession caster,int damage,long id,ClientSession player,BattleQuestEncounter.Snapshot q,BattleTimeAttackEncounter.Snapshot t,String reason)throws IOException{
  var s=current(caster);if(s==null){NativeSourceDollCommit.commit(caster,damage,id,player,q,t,reason);return;}
  if(damage<=0||id!=s.id()||player!=null||!s.current())return;if(!NativeSourceDollCommit.skill(reason)&&!"active-damage".equals(reason))throw new IllegalArgumentException("unreviewed doll proc kind");
  if(s.controlled==null)naturalCommit(s,damage,reason);else controlledCommit(s,damage,reason);
 }
 static void naturalCommit(Scope s,int damage,String reason)throws IOException{
  boolean skill=NativeSourceDollCommit.skill(reason);var aim=s.nextAim();NativeSourceDamageDisplay.Plan pre=skill?null:NativeSourceDamageDisplay.captureNpc(s.c.pc,s.proof(),s.id(),damage,false,false);if(!skill)NativeSourceDamageDisplay.publish(pre);
  if(!s.alive())return;var receipt=new NativeSourceNpcDamageDisplay.Receipt(aim,s.c.pc,damage,skill?2:0,false);if(!receipt.before())return;
  try{CombatAccess.call(s.c.pc,aim.npc().kind()==1?"nativeSourceHpOriginalQuestDamage":"nativeSourceHpOriginalTimeDamage","(JILjava/lang/String;L"+P+"NativeSourceNpcDamageDisplay$Receipt;)I",s.id(),damage,"magic-doll:"+reason,receipt);}
  finally{if(receipt.accepted){s.natural=receipt.proof(true);if(pre!=null)pre.npc=s.natural;if(receipt.current()){try{NativeSourceMonsterHp.publish(aim);}catch(RuntimeException failure){failed(s.c.pc);}try{NativeSourceDamageDisplay.publish(receipt.visual);}catch(RuntimeException failure){failed(s.c.pc);}}}}
 }
 static void controlledCommit(Scope s,int damage,String reason)throws IOException{
  boolean skill=NativeSourceDollCommit.skill(reason);var t=s.controlled;var a=t.action;var visual=NativeSourceDamageDisplay.captureNpc(s.c.pc,s.proof(),s.id(),damage,false,false);if(!skill)NativeSourceDamageDisplay.publish(visual);
  if(!s.alive()||!t.before())return;var previous=a.currentTarget;a.currentTarget=t;
  try{var result=NativeSourceControlledDisplay.feedback(t.owner,t.pet,damage,s.c.players,"magic-doll:"+reason);if(result!=null&&result.appliedDamage()>0){t.seal();if(visual!=null)visual.npc=s.proof();CombatAccess.call(s.c.pc,"markBattleCombatWith","(L"+P+"ClientSession;)V",t.owner);if(skill)NativeSourceControlledDisplay.publish(a,t,visual);}}
  finally{a.currentTarget=previous;}
 }
 private NativeSourceDollNpc(){}
}
