package local.lito381.server;
import java.io.*;import java.util.*;
/** Source healing uses the original signed HP FIFO; native previews cannot claim acceptance. */
final class NativeSourceHealingHp {
 static final String P="local/lito381/server/";
 record Recovery(NativeSourceRawDamage.Member target){}
 static void mark(NativeSourceDamageDisplay.Plan plan,int raw){if(plan==null)return;var guard=plan.nativeRawGuard;plan.nativeSourceHealingRecovery=raw<0&&guard!=null&&guard.target()!=null&&guard.target().binding.phase().equals("BATTLE")?new Recovery(guard.target()):null;}
 static Recovery proof(NativePlayerCommit.Frame frame,ClientSession pc){if(frame==null||frame.pc!=pc||frame.fatal||frame.event.world()||frame.event.damage()>0)return null;var plan=frame.event.sourceDisplay();if(plan==null||!(plan.nativeSourceHealingRecovery instanceof Recovery proof)||plan.nativeRawGuard==null||plan.nativeRawGuard.target()!=proof.target()||proof.target().pc!=pc||!Objects.equals(frame.event.target(),proof.target().binding))return null;return proof;}
 static boolean recovery(ClientSession pc){var frame=NativePlayerCommit.frame();if(proof(frame,pc)==null)return false;if(frame.accepted&&frame.before!=frame.after){NativeAcceptedState.requireCurrent();NativePlayerCommit.safeSend(pc,new Protocol.Writer().c(Protocol.S_HIT_POINT).h(frame.after).h(frame.maximum).bytes(),"S_HIT_POINT Source accepted recovery="+frame.after+"/"+frame.maximum);NativePlayerCommit.safeRatio(pc,"Source accepted recovery");}return true;}
 static NativeSourceDamageDisplay.Plan plan(NativeSourceHealingCast.Cast cast,ClientSession pc,int damage){
  if(cast==null||!Thread.holdsLock(cast.lifetime.admission)||!pc.nativePlayerActionLock().isHeldByCurrentThread()||!cast.source.pc.nativePlayerActionLock().isHeldByCurrentThread()||!cast.allowed(pc)||NativeSourceDamageDisplay.n(pc,"playerHp")<=0)return null;
  var member=cast.members.get(pc);var plan=damage>0?NativeSourceDamageDisplay.capture(cast.source.pc,pc,damage,false,false,false):null;if(plan==null){var actor=new NativeSourceDamageDisplay.Actor(pc,member.binding,member.life,member.x,member.y);plan=new NativeSourceDamageDisplay.Plan(actor,actor,List.of());}plan.nativeRawGuard=new NativeSourceRawDamage.Guard(member,null,false,cast.lifetime);return plan;
 }
 static boolean handled(ClientSession pc,int healing)throws IOException{var cast=NativeSourceHealingCast.current();if(cast==null)return false;if(healing==0)return true;var plan=plan(cast,pc,-healing);if(plan!=null)NativeSourceDamageDisplay.damage(pc,-healing,cast.source.pc,true,false,0,plan);return true;}
 static void damage(ClientSession pc,int amount,ClientSession caster,boolean originalMagic)throws IOException{var cast=NativeSourceHealingCast.current();if(cast==null){CombatAccess.call(pc,"takePlayerDamage","(IL"+P+"ClientSession;Z)V",amount,caster,originalMagic);return;}if(caster!=cast.source.pc)return;var plan=plan(cast,pc,amount);if(plan!=null&&amount!=0)NativeSourceDamageDisplay.damage(pc,amount,caster,true,false,0,plan);}
 static boolean preview(byte[]packet,String note){return packet!=null&&packet.length>0&&(packet[0]&255)==Protocol.S_HIT_POINT&&note!=null&&(note.startsWith("S_HIT_POINT healing=")||note.startsWith("S_HIT_POINT group-healing=")||note.startsWith("S_HIT_POINT Natures-Blessing="));}
 static void send(ClientSession pc,byte[]packet,String note)throws IOException{if(NativeSourceHealingCast.current()!=null&&preview(packet,note))return;CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,note);}
 static void ratio(ClientSession pc,long id,int hp,int max,String note)throws IOException{if(NativeSourceHealingCast.current()!=null)return;CombatAccess.call(pc,"broadcastHitRatio","(JIILjava/lang/String;)V",id,hp,max,note);}
 private NativeSourceHealingHp(){}
}
