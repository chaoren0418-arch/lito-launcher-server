// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.*;
/** Source doll29 only. Uses the original actors, locks, timer scalars and expiry/clear paths. */
final class NativeSourceDollSlow {
 static final long PERMANENT=Long.MAX_VALUE;
 /** Source performs int seconds*1000 before choosing a finite or permanent skill timer. */
 static long expires(long now,int seconds){
  if(now<0||seconds<0)throw new IllegalArgumentException("invalid Source slow time");
  int millis=seconds*1000;
  return millis<=0?PERMANENT:Math.addExact(now,Math.max(1,millis/1000)*1000000000L);
 }
 static boolean active(long expiry,long now){return expiry!=0&&now<expiry;}
 static int remaining(long expiry,long now){
  if(expiry==PERMANENT)return 0;
  long left=expiry-now;return left<=0?0:(int)((1+(left-1)/1000000000L)&65535L);
 }
 /** Caller owns the original status/pet/Hub monitor; no object is retained. */
 static BattleTimedStatus.SpeedChange change(Object state,long now,int duration,int mode,boolean ids,boolean movement){
  long expiry=expires(now,duration);
  if(mode==BattleTimedStatus.SLOW||mode!=BattleTimedStatus.NORMAL&&mode!=BattleTimedStatus.HASTE)return null;
  if(mode==BattleTimedStatus.HASTE){
   long haste=(long)CombatAccess.field(state,"hasteExpiresAtNanos");
   int skill=ids?(int)CombatAccess.field(state,"hasteSkillId"):43;
   if(!active(haste,now)||ids&&skill!=0&&skill!=43&&skill!=54&&skill!=1001)return null;
   CombatAccess.set(state,"nativeSourceDollSlowEpoch",new Object());
   CombatAccess.set(state,"hasteExpiresAtNanos",0L);if(ids)CombatAccess.set(state,"hasteSkillId",0);
   if(movement)CombatAccess.set(state,"moveSpeed",BattleTimedStatus.NORMAL);
   return new BattleTimedStatus.SpeedChange(BattleTimedStatus.NORMAL,0,skill);
  }
  CombatAccess.set(state,"nativeSourceDollSlowEpoch",new Object());
  CombatAccess.set(state,"hasteExpiresAtNanos",0L);if(ids)CombatAccess.set(state,"hasteSkillId",0);
  CombatAccess.set(state,"slowExpiresAtNanos",expiry);if(ids)CombatAccess.set(state,"slowSkillId",29);
  if(movement)CombatAccess.set(state,"moveSpeed",BattleTimedStatus.SLOW);
  return new BattleTimedStatus.SpeedChange(BattleTimedStatus.SLOW,duration&65535,0);
 }
 static Object epoch(NativeSourceDollOutput.Speed speed){return CombatAccess.field(speed.state(),"nativeSourceDollSlowEpoch");}
 static boolean sameEpoch(NativeSourceDollOutput.Speed speed){return speed.nativeSourceDollSlowEpoch==epoch(speed);}
 static BattleTimedStatus.SpeedChange pcChange(BattleTimedStatus state,long now,int duration){
  synchronized(state){return change(state,now,duration,state.speed(now).type(),true,false);}
 }
 static void slow(NativeSourceDollSkills.View view,int duration)throws IOException{
  if(duration<0||!view.alive())return;long now=System.nanoTime();BattleTimedStatus.SpeedChange result;
  if(view.player!=null)result=pcChange((BattleTimedStatus)CombatAccess.field(view.player,"timedStatus"),now,duration);
  else if(view.npc.controlled!=null){var pet=view.npc.controlled.pet;synchronized(pet){if(!view.alive())return;result=change(pet,now,duration,(int)CombatAccess.field(pet,"moveSpeed"),true,true);}}
  else{synchronized(view.npc.c.hub){if(!view.alive())return;var actor=view.npc.aim.npc().actor();int mode=active((long)CombatAccess.field(actor,"slowExpiresAtNanos"),now)?2:active((long)CombatAccess.field(actor,"hasteExpiresAtNanos"),now)?1:0;result=change(actor,now,duration,mode,false,false);}}
  if(result==null)return;
  for(var receiver:view.viewers){int seconds=receiver==view.player&&result.type()==BattleTimedStatus.SLOW?duration&65535:0;
   NativeSourceDollOutput.send(receiver,NativeSourceDollSlowWire.packet(view.id,Protocol.SpeedMode.fromWire(result.type()),seconds,duration),"S_SPEED Source doll slow",view.caster,view,2);
  }
 }
 /** Called inside the original synchronized speed() query; avoid sentinel overflow and preserve writeH. */
 static BattleTimedStatus.Speed extendedSpeed(BattleTimedStatus state,long now){
  long expiry=(long)CombatAccess.field(state,"slowExpiresAtNanos");
  if(!active(expiry,now))return null;
  return expiry==PERMANENT||expiry-now>65535000000000L?new BattleTimedStatus.Speed(BattleTimedStatus.SLOW,remaining(expiry,now)):null;
 }
 /** Original final-output proofs stay authoritative; only the Source timer/wire encoding changes. */
 static boolean nativeSourceDollSlowWireOriginalFrame(ClientSession receiver,byte[] packet){
  var context=NativeAreaMaterialAction.current.get();if(context==null||!(context.nativeSourceDollOutput instanceof NativeSourceDollOutput.Output out))return true;
  if(out.receiver().pc()!=receiver||!NativeSourceDollOutput.current(out))return false;
  if(out.kind()==2&&out.proof().target()!=null&&out.proof().target().pc()==receiver&&out.proof().speed().mode()==2&&packet.length==8&&(packet[0]&255)==Protocol.S_SPEED){
   long expiry=out.proof().speed().slow(),now=System.nanoTime();if(!active(expiry,now))return false;
   int seconds=remaining(expiry,now);packet[6]=(byte)seconds;packet[7]=(byte)(seconds>>>8);
  }return true;
 }
 static boolean allowFrame(ClientSession receiver,byte[] packet){return NativeSourceDollSlowWire.allowFrame(receiver,packet);}
 private NativeSourceDollSlow(){}
}
