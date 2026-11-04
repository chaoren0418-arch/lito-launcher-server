package local.lito381.server;
import java.io.*;import java.util.*;
/** Explicit arena-monster visual receipt is captured before output and sealed at the existing Hub HP mutation. */
final class NativeSourceNpcDamageDisplay {
 static final String P="local/lito381/server/";
 record Proof(Object actor,Object owner,Object generation,Object life,int x,int y,boolean accepted,NativeSourceControlledDisplay.Proof controlled){
  Proof(Object actor,Object owner,Object generation,Object life,int x,int y,boolean accepted){this(actor,owner,generation,life,x,y,accepted,null);}
  boolean current(){if(controlled!=null)return accepted&&controlled.current();var state=CombatAccess.field(CombatAccess.field(actor,"damagePoison"),"sourcePoisonLifecycle");return accepted&&state!=null&&CombatAccess.field(state,"sourceNpcOwner")==life&&CombatAccess.field(owner,"sourceProcEncounterGeneration")==generation&&(int)CombatAccess.field(actor,"x")==x&&(int)CombatAccess.field(actor,"y")==y;}
 }
 static final class Receipt {
  final NativeSourceMonsterHp.Aim aim;final NativeSourceDamageDisplay.Plan visual;volatile boolean accepted;volatile Object life;
  Receipt(NativeSourceMonsterHp.Aim aim,ClientSession caster,int damage,int mode,boolean critical){this.aim=aim;visual=aim==null||mode==0?null:NativeSourceDamageDisplay.captureNpc(caster,proof(false),aim.npc().id(),damage,mode==1,critical);}
  boolean before(){return aim!=null&&NativePlayerLifetime.of(aim.c().hub).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(aim.c().hub,"nativeStaticMotionClosed")&&aim.c().scene()&&aim.c().npc(aim.npc().id())&&aim.caster().valid(aim.caster().life())&&NativeSourceMonsterHp.life(aim.npc().actor())==aim.npc().epoch();}
  Proof proof(boolean committed){return new Proof(aim.npc().actor(),aim.npc().owner(),aim.generation(),committed?life:aim.npc().epoch(),aim.x(),aim.y(),committed);}
  boolean current(){return aim!=null&&accepted&&proof(true).current();}
  void seal(){life=NativeSourceMonsterHp.life(aim.npc().actor());if(visual!=null)visual.npc=proof(true);accepted=true;}
 }
 static BattleQuestEncounter.Hit questDamage(GameHub hub,long room,ClientSession caster,long id,int damage,Receipt receipt){synchronized(hub){if(receipt!=null&&receipt.aim!=null&&!receipt.before())return null;var hit=NativeCombatAttacker.questDamage(hub,room,caster,id,damage);if(hit!=null&&receipt!=null&&receipt.aim!=null)receipt.seal();return hit;}}
 static GameHub.TimeAttackActorHit timeDamage(GameHub hub,long room,ClientSession caster,long id,int damage,Receipt receipt){synchronized(hub){if(receipt!=null&&receipt.aim!=null&&!receipt.before())return null;var hit=NativeCombatAttacker.timeDamage(hub,room,caster,id,damage);if(hit!=null&&receipt!=null&&receipt.aim!=null)receipt.seal();return hit;}}
 static NativeSourceMonsterHp.Aim capture(ClientSession caster,Protocol.AttackRequest request){
  if(!CombatRules.loaded()||!CombatAccess.field(caster,"phase").toString().equals("BATTLE"))return null;var c=new NativeAreaMaterialAction.Context(caster);var npc=c.npcs.get(request.targetObjectId());if(npc==null||npc.kind()!=1&&npc.kind()!=2)return null;return new NativeSourceMonsterHp.Aim(c,npc,(NativeSourceMonsterHp.Cache)CombatAccess.field(npc.actor(),NativeSourceMonsterHp.CACHE),NativeSourceDamageDisplay.Actor.capture(caster),NativeProcHealingEncounter.generation(npc.owner()),(int)CombatAccess.field(npc.actor(),"x"),(int)CombatAccess.field(npc.actor(),"y"));
 }
 static void kiringku(ClientSession caster,List<ClientSession> members,boolean hit,BattleKiringkuDamage.Outcome old,ClientSession target,NativeSourceMonsterHp.Aim aim)throws IOException{
  if(aim==null){NativeSourceKiringkuPresentation.broadcast(caster,members,hit,old,target);return;}var settings=NativeSourceOtherControls.settings();if(!hit||!settings.kiringku()||settings.kiringkuSprite()<=0)return;var audience=new ArrayList<NativeSourceKiringkuPresentation.Viewer>();for(var member:members)audience.add(new NativeSourceKiringkuPresentation.Viewer(member,NativePlayerCommit.Binding.capture(member)));var packet=Protocol.effectObject(caster.playerObjectId(),settings.kiringkuSprite());for(var viewer:audience)synchronized(viewer.pc()){if(fxCurrent(aim,viewer))viewer.pc().nativeSelfOriginalSend(packet,"S_EFFECT Source arena monster kiringku");}
 }
 static boolean fxCurrent(NativeSourceMonsterHp.Aim aim,NativeSourceKiringkuPresentation.Viewer viewer){return NativePlayerLifetime.of(aim.c().hub).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(aim.c().hub,"nativeStaticMotionClosed")&&aim.caster().valid(aim.caster().life())&&NativeSourceDamageDisplay.n(aim.c().pc,"playerHp")>0&&NativeAcceptedFreezeMutation.roleFrame(viewer.binding(),viewer.pc())&&!(boolean)CombatAccess.field(viewer.pc(),"sourceItemDisconnected")&&(int)CombatAccess.field(aim.npc().actor(),"hp")>0&&new Proof(aim.npc().actor(),aim.npc().owner(),aim.generation(),aim.npc().epoch(),aim.x(),aim.y(),true).current()&&BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(viewer.pc(),"positionX"),NativeSourceDamageDisplay.n(viewer.pc(),"positionY"),aim.caster().x(),aim.caster().y());}
 static int run(ClientSession caster,long id,int damage,String source,boolean quest,int mode,boolean critical)throws IOException{return run(caster,id,damage,source,quest,mode,critical,NativeSourceMonsterHp.capture(caster,id,quest?1:2));}
 static int run(ClientSession caster,long id,int damage,String source,boolean quest,int mode,boolean critical,NativeSourceMonsterHp.Aim aim)throws IOException{
  var receipt=new Receipt(aim,caster,damage,mode,critical);int result=(int)CombatAccess.call(caster,quest?"nativeSourceHpOriginalQuestDamage":"nativeSourceHpOriginalTimeDamage","(JILjava/lang/String;L"+P+"NativeSourceNpcDamageDisplay$Receipt;)I",id,damage,source,receipt);
  if(receipt.accepted&&receipt.current()){try{NativeSourceMonsterHp.publish(aim);}catch(RuntimeException failure){failed(caster);}try{NativeSourceDamageDisplay.publish(receipt.visual);}catch(RuntimeException failure){failed(caster);}}return result;
 }
 static void failed(ClientSession caster){var life=NativePlayerLifetime.of(caster);synchronized(life.admission){life.nativeSourceDisplayJobs.failed++;}}
 static int quest(ClientSession caster,long id,int damage,String source)throws IOException{return run(caster,id,damage,source,true,0,false);}
 static int time(ClientSession caster,long id,int damage,String source)throws IOException{return run(caster,id,damage,source,false,0,false);}
 static int magicQuest(ClientSession caster,long id,int damage,String source)throws IOException{return run(caster,id,damage,source,true,2,false);}
 static int magicTime(ClientSession caster,long id,int damage,String source)throws IOException{return run(caster,id,damage,source,false,2,false);}
 static int physicalQuest(ClientSession caster,long id,int damage,String source,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome maximum,BattleWeaponCritical.DamageOutcome multiplier,NativeSourceMonsterHp.Aim aim)throws IOException{if(CombatRules.loaded()&&aim==null)return 0;return run(caster,id,damage,source,true,1,NativeSourceDamageDisplay.critical(weapon.weaponType(),maximum.effectEnabled(),multiplier.effectEnabled()),aim);}
 static int physicalTime(ClientSession caster,long id,int damage,String source,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.RollOutcome maximum,BattleWeaponCritical.DamageOutcome multiplier,NativeSourceMonsterHp.Aim aim)throws IOException{if(CombatRules.loaded()&&aim==null)return 0;return run(caster,id,damage,source,false,1,NativeSourceDamageDisplay.critical(weapon.weaponType(),maximum.effectEnabled(),multiplier.effectEnabled()),aim);}
 private NativeSourceNpcDamageDisplay(){}
}
