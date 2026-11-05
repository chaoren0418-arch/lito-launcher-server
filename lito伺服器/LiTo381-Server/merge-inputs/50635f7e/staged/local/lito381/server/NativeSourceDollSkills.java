// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** Source SKILL loop: no normal cast permission, material, MP or probability pipeline is invoked. */
final class NativeSourceDollSkills {
 static final String P="local/lito381/server/",PRE="(IIIIIILjava/lang/String;I)L"+P+"CombatMagicDamage$Attempt;";
 static final class View {
  final ClientSession caster,player;final long id;final NativeSourceDollAction.Stamp stamp;final NativeSourceDamageDisplay.Actor target;final NativeSourceDollNpc.Scope npc;final List<ClientSession> viewers;final Object nativeSourceDollOutputAudience;
  View(ClientSession c,long id,ClientSession p){caster=c;player=p;this.id=id;var old=NativeSourceDollAction.stamp(c);stamp=old instanceof NativeSourceDollAction.Stamp s?s:NativeSourceDollAction.capture(c);target=p==null?null:NativeSourceDamageDisplay.Actor.capture(p);npc=NativeSourceDollNpc.current(c);var context=NativeAreaMaterialAction.current.get();viewers=context!=null&&context.pc==c?NativeSourceDollAudience.copy(context.players,context,c):NativeSourceDollAudience.copy(NativeSourceDollAudience.members((GameHub)CombatAccess.field(c,"hub"),(long)CombatAccess.field(c,"currentRoomId"),c),context,c);nativeSourceDollOutputAudience=NativeSourceDollOutput.snapshot(this);}
  boolean current(){var lifetime=NativePlayerLifetime.of(caster);return lifetime.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(CombatAccess.field(caster,"hub"),"nativeStaticMotionClosed")&&stamp.before()&&NativeSourceDamageDisplay.n(caster,"playerHp")>0&&(target==null?npc!=null&&npc.id()==id&&npc.current():target.valid(target.life()));}
  boolean alive(){return current()&&(player!=null?NativeSourceDamageDisplay.n(player,"playerHp")>0:npc.alive());}
 }
 static void apply(ClientSession caster,NativeMagicDollEffects.AttackProc proc,long id,ClientSession player,BattleQuestEncounter.Snapshot q,BattleTimeAttackEncounter.Snapshot t,SourceNpcPhysicalStats stats)throws IOException{
  if(!NativeSourceDollCommit.enabled(caster)){caster.nativeSourceDollOriginalOrderedAttackProcs(proc,id,player,q,t,stats);return;}
  if(proc==null||!proc.triggered())return;var view=new View(caster,id,player);if(!view.current())return;
  for(var power:proc.damageTriggers())try{if(!view.current())continue;if(power.gfxId()>0){if(view.alive())targetEffect(view,power.gfxId());}else dollAttack(view);}catch(IOException|RuntimeException failure){failed(caster);}
  for(var power:proc.skillTriggers())try{
   if(!view.alive())continue;var row=NativeSourceDollSkillData.row(power.triggerSkillId());int damage=damage(view,row,stats);
   if(!view.current())continue;if(CombatPlayerDamage.dollGate(damage,player,caster)>0)NativeSourceDollNpc.commit(caster,damage,id,player,q,t,"skill-"+row.id());
   targetEffect(view,row.gfx());dollAttack(view);if(row.id()==29)NativeSourceDollSlow.slow(view,row.duration());
  }catch(IOException|RuntimeException failure){failed(caster);}
  if(proc.additionalDamage()>0&&view.current())try{NativeSourceDollNpc.commit(caster,proc.additionalDamage(),id,player,q,t,"active-damage");}catch(IOException|RuntimeException failure){failed(caster);}
 }
 static int damage(View v,NativeSourceDollSkillData.Row row,SourceNpcPhysicalStats stats){
  int skill=row.id();long now=System.nanoTime();if(v.player!=null&&CombatPlayerMagic.protectedZero(v.player,now))return 0;
  if(v.player==null&&v.npc!=null){if(v.npc.controlled!=null?v.npc.controlled.pet.freezeActive(now):(boolean)CombatAccess.call(v.npc.aim.npc().actor(),"freezeActive","(J)Z",now))return 0;}
  int source;if(skill==BattleSpellCatalog.FINAL_BURN)source=NativeSourceDamageDisplay.n(v.caster,"playerMp");else{source=row.damage();for(int i=0;i<row.dice();i++)source+=ThreadLocalRandom.current().nextInt(row.die())+1;}
  int elemental=v.player==null?0:CombatAccess.integerCall(v.player,"effectiveSpellElementalResistance","(I)I",row.attribute());
  int mr=v.player==null?(stats==null?0:stats.magicResistance()):CombatAccess.integerCall(v.player,"effectiveBaseMagicResistance","()I");
  if(v.player==null&&v.npc!=null)mr=v.npc.controlled!=null?v.npc.controlled.pet.effectiveMagicResistance(mr,now):((BattleEraseMagicStatus)CombatAccess.field(v.npc.aim.npc().actor(),"eraseMagicStatus")).effectiveMagicResistance(mr,now);
  int armor=v.player==null?0:CombatAccess.integerCall(v.player,"effectiveArmorMagicReductionBase","()I");
  CombatMagicDamage.Attempt raw;
  if(skill==BattleSpellCatalog.FINAL_BURN){int offset=armor>10?ThreadLocalRandom.current().nextInt(armor-10):0;int reduced=source-BattleMagicResistance.armorDamageReduction(armor,offset);raw=CombatMagicDamage.attempt(reduced,mr,CombatAccess.integerCall(v.caster,"originalMagicHit","()I"));}
  else raw=(CombatMagicDamage.Attempt)CombatAccess.call(v.caster,v.player==null?"nativeSourceDollNpcMagic":"nativeSourceDollPcMagic",PRE,skill,row.attribute(),source,elemental,mr,armor,"magic-doll-proc",Math.max(1,row.level()));
  int damage=raw.damage();
  if(v.player!=null){damage=CombatPlayerMagic.reductions(v.player,damage,"source-magic-doll");damage=CombatPlayerMagic.protect(v.player,damage,"source-magic-doll");var mirror=(BattleCounterMirrorStatus)CombatAccess.field(v.player,"counterMirrorStatus");if(mirror.active(System.nanoTime())){var decision=CombatPlayerMirror.decide(v.player,damage,true,ThreadLocalRandom.current().nextInt(100),System.nanoTime(),raw);if(decision.triggered()){CombatPlayerMirror.publish(v.caster,v.player,decision,"magic-doll");damage=0;}}
   return CombatMagicDamage.finish(damage,raw,v.caster,specialCritical(skill));
  }
  if(v.npc!=null&&v.npc.controlled!=null)damage=(int)(damage*CombatCompanionDamage.multiplier());
  if(specialCritical(skill))damage*=2;
  return CombatNpcMagic.resistance(damage,mr,raw.originalMagicHit());
 }
 static boolean specialCritical(int skill){if(skill!=BattleSpellCatalog.DISINTEGRATE)return false;double chance=CombatRules.number("DISINTEGRATE_CRITICAL_RND",0);return chance>0&&ThreadLocalRandom.current().nextInt(100)+1<=chance;}
 static boolean procVisible(boolean enabled,int x,int y,int centerX,int centerY){return enabled&&Math.max(Math.abs((long)x-centerX),Math.abs((long)y-centerY))<=10;}
 static void send(ClientSession receiver,byte[] packet,String note,ClientSession caster){try{CombatAccess.call(receiver,"sendEncrypted","([BLjava/lang/String;)V",packet,note);}catch(RuntimeException failure){failed(caster);}}
 static void targetEffect(View v,int gfx)throws IOException{if(gfx<=0||!v.current())return;int x=v.target!=null?v.target.x():v.npc.controlled!=null?v.npc.controlled.x:v.npc.aim.x(),y=v.target!=null?v.target.y():v.npc.controlled!=null?v.npc.controlled.y:v.npc.aim.y();var packet=Protocol.effectObject(v.id,gfx);for(var receiver:v.viewers)if(procVisible(receiver.nativeSourceShowProcSpr,NativeSourceDamageDisplay.n(receiver,"positionX"),NativeSourceDamageDisplay.n(receiver,"positionY"),x,y))NativeSourceDollOutput.send(receiver,packet,"S_EFFECT Source doll skill target",v.caster,v,0);}
 static void dollAttack(View v)throws IOException{if(!v.current()||v.stamp.doll()==null)return;var doll=(NativeMagicDoll)CombatAccess.field(v.caster,"activeMagicDoll");if(doll==null)return;var packet=Protocol.objectAction(doll.objectId(),67);for(var receiver:v.viewers)if(receiver.nativeSourceShowProcSpr&&BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(receiver,"positionX"),NativeSourceDamageDisplay.n(receiver,"positionY"),doll.x(),doll.y()))NativeSourceDollOutput.send(receiver,packet,"S_ACTION Source doll skill aggression",v.caster,v,1);}
 static void slow(View v,int duration)throws IOException{
  if(duration<=0||duration>65535||!v.alive())return;long now=System.nanoTime();int speed;
  if(v.player!=null){var timed=(BattleTimedStatus)CombatAccess.field(v.player,"timedStatus");var change=slowChange(timed,now,duration);if(change==null)return;speed=change.type();}
  else if(v.npc.controlled!=null){var pet=v.npc.controlled.pet;if(pet.snapshot().moveSpeed()==BattleTimedStatus.SLOW)return;speed=pet.activateSlow(now,duration,29).type();}
  else{synchronized(v.npc.c.hub){if(!v.alive())return;var actor=v.npc.aim.npc().actor();if((boolean)CombatAccess.call(actor,"slowActive","(J)Z",now))return;if(!(boolean)CombatAccess.call(actor,"activateSlow","(JI)Z",now,duration))return;speed=(boolean)CombatAccess.call(actor,"slowActive","(J)Z",now)?BattleTimedStatus.SLOW:BattleTimedStatus.NORMAL;}}
  for(var receiver:v.viewers){int seconds=receiver==v.player&&speed==BattleTimedStatus.SLOW?duration:0;NativeSourceDollOutput.send(receiver,Protocol.speed(v.id,Protocol.SpeedMode.fromWire(speed),seconds),"S_SPEED Source doll slow",v.caster,v,2);}
 }
 static BattleTimedStatus.SpeedChange slowChange(BattleTimedStatus status,long now,int duration){synchronized(status){if(status.speed(now).type()==BattleTimedStatus.SLOW)return null;return status.activateSlow(now,duration,29);}}
 static boolean toggle(ClientSession pc,String action)throws IOException{if(!NativeSourceDollCommit.enabled(pc)||!"proc_spr".equalsIgnoreCase(action))return false;NativeLightAction.commitSelf(pc,()->{pc.nativeSourceShowProcSpr=!pc.nativeSourceShowProcSpr;CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",Protocol.messageCode(166,"特效顯示: "+(pc.nativeSourceShowProcSpr?"開啟":"關閉")),"S_MESSAGE_CODE Source proc_spr");});return true;}
 static void failed(ClientSession pc){NativeSourceNpcDamageDisplay.failed(pc);}
 private NativeSourceDollSkills(){}
}
