package local.lito381.server;
import java.io.*;import java.util.*;
/** Explicit proc-frame provenance, reusing only the existing bounded publication scope. */
final class NativeSourceDollOutput {
 record Receiver(ClientSession pc,NativePlayerCommit.Binding binding){}
 /** Synchronous producer carrier only; never stored in an HP/display Plan or detached output proof. */
 record Snapshot(List<Receiver> receivers,NativeAreaMaterialAction.Context carrier){}
 record Speed(Object actor,Object state,long haste,long slow,int mode){
  boolean current(){return (long)CombatAccess.field(state,"hasteExpiresAtNanos")==haste&&(long)CombatAccess.field(state,"slowExpiresAtNanos")==slow&&speedMode(haste,slow,System.nanoTime())==mode;}
 }
 record Proof(NativeSourceDollAction.Stamp summon,NativeSourceDamageDisplay.Actor target,Object targetLife,NativeSourceNpcDamageDisplay.Proof npc,NativePlayerLifetime lifetime,GameHub hub,Speed speed){
  boolean current(){
   if(lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return false;
   if(speed!=null)return statusTargetCurrent(this)&&speed.current();
   return summon.current()&&NativeSourceDamageDisplay.n(summon.caster(),"playerHp")>0&&(target==null||target.valid(targetLife))&&(npc==null||npc.current());
  }
 }
 record Output(Proof proof,Receiver receiver,byte[] packet,String note,int kind,int x,int y){}
 static Object snapshot(NativeSourceDollSkills.View view){
  var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=view.caster)c=new NativeAreaMaterialAction.Context(view.caster);
  var receivers=new ArrayList<Receiver>();for(var pc:view.viewers){var binding=c.bindings.get(pc);receivers.add(new Receiver(pc,binding!=null?binding:NativePlayerCommit.Binding.capture(pc)));}return new Snapshot(List.copyOf(receivers),c);
 }
 static void send(ClientSession receiver,byte[] packet,String note,ClientSession caster,NativeSourceDollSkills.View view,int kind){
  try{
   if(caster!=view.caster||kind!=2&&!view.current()||!(view.nativeSourceDollOutputAudience instanceof Snapshot snapshot))return;
   Receiver selected=null;for(var candidate:snapshot.receivers)if(candidate.pc==receiver){selected=candidate;break;}if(selected==null)return;
   var npc=view.npc==null?null:view.npc.proof();var life=NativePlayerLifetime.of(caster);Speed speed=null;
   if(kind==2){if(packet.length!=8||(packet[0]&255)!=Protocol.S_SPEED)return;Object actor=view.target!=null?view.target.pc():npc.actor();Object state=view.target!=null?CombatAccess.field(actor,"timedStatus"):actor;speed=new Speed(actor,state,(long)CombatAccess.field(state,"hasteExpiresAtNanos"),(long)CombatAccess.field(state,"slowExpiresAtNanos"),packet[5]&255);}
   var proof=new Proof(kind==2?null:view.stamp,view.target,view.target==null?null:view.target.life(),npc,life,(GameHub)CombatAccess.field(caster,"hub"),speed);
   int x=view.target!=null?view.target.x():view.npc.controlled!=null?view.npc.controlled.x:view.npc.aim.x(),y=view.target!=null?view.target.y():view.npc.controlled!=null?view.npc.controlled.y:view.npc.aim.y();
   var out=new Output(proof,selected,packet.clone(),note,kind,x,y);var c=snapshot.carrier;
   NativeLightAction.deferScoped(NativeSourceDollDetachedOutput.task(c,out,receiver,caster));
  }catch(RuntimeException failure){NativeSourceDollSkills.failed(caster);}
 }
 /** Runs at the original receiver frame monitor: read-only identities, no action/Hub/status/inventory lock. */
 static boolean current(Output out){
  if(!NativeSourceDollAudience.current(out))return false;
  var receiver=out.receiver.pc;if(!out.proof.current()||!NativeAcceptedFreezeMutation.roleFrame(out.receiver.binding,receiver)||(boolean)CombatAccess.field(receiver,"sourceItemDisconnected"))return false;
  if(out.kind!=2&&!receiver.nativeSourceShowProcSpr)return false;
  int x=out.x,y=out.y;if(out.kind==1){var doll=(NativeMagicDoll)CombatAccess.field(out.proof.summon.caster(),"activeMagicDoll");if(doll==null)return false;x=doll.x();y=doll.y();}
  if(out.kind==2){var actor=out.proof.speed.actor;x=(int)CombatAccess.field(actor,out.proof.target!=null?"positionX":"x");y=(int)CombatAccess.field(actor,out.proof.target!=null?"positionY":"y");}
  int rx=NativeSourceDamageDisplay.n(receiver,"positionX"),ry=NativeSourceDamageDisplay.n(receiver,"positionY");return out.kind==0?NativeSourceDollSkills.procVisible(true,rx,ry,x,y):BattleSpellCatalog.isInNativeScreen(rx,ry,x,y);
 }
 static int speedMode(long haste,long slow,long now){return slow!=0&&now<slow?2:haste!=0&&now<haste?1:0;}
 static int remaining(long expires,long now){long left=expires-now;return left<=0?0:(int)Math.min(65535,1+(left-1)/1000000000L);}
 static boolean statusTargetCurrent(Proof p){
  if(p.target!=null){var pc=p.target.pc();return NativeAcceptedFreezeMutation.roleFrame(p.target.binding(),pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&NativeSourceControlledDisplay.rawLife(pc)==p.targetLife;}
  var n=p.npc;if(n==null)return false;var c=n.controlled();
  if(c!=null){if(!NativeAcceptedFreezeMutation.roleFrame(c.binding(),c.owner())||(boolean)CombatAccess.field(c.owner(),"sourceItemDisconnected")||NativeSourceControlledDisplay.rawLife(c.owner())!=c.ownerLife()||NativeSourceControlledDisplay.rawLife(c.pet())!=c.life()||CombatAccess.field(c.pet(),"carriedItems")!=c.bag()||(int)CombatAccess.field(c.pet(),"hp")<=0||!(boolean)CombatAccess.field(c.pet(),"active")||!NativeSourceControlledDisplay.attached(c.owner(),c.pet()))return false;var state=NativeCompanionPlacement.state(c.pet());return state==c.placement()&&(state==null||state.owner.get()==c.owner()&&state.scene==c.scene()&&state.master==c.master());}
  var state=CombatAccess.field(CombatAccess.field(n.actor(),"damagePoison"),"sourcePoisonLifecycle");return state!=null&&CombatAccess.field(state,"sourceNpcOwner")==n.life()&&CombatAccess.field(n.owner(),"sourceProcEncounterGeneration")==n.generation()&&(int)CombatAccess.field(n.actor(),"hp")>0;
 }
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null)child.nativeSourceDollOutput=parent.nativeSourceDollOutput;}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 static boolean allowFrame(ClientSession receiver,byte[] packet){var c=NativeAreaMaterialAction.current.get();if(c==null||!(c.nativeSourceDollOutput instanceof Output out))return true;if(out.receiver.pc!=receiver||!current(out))return false;if(out.kind==2&&out.proof.target!=null&&out.proof.target.pc()==receiver&&out.proof.speed.mode==2&&packet.length==8&&(packet[0]&255)==Protocol.S_SPEED){int seconds=remaining(out.proof.speed.slow,System.nanoTime());if(seconds==0)return false;packet[6]=(byte)seconds;packet[7]=(byte)(seconds>>>8);}return true;}
 private NativeSourceDollOutput(){}
}
