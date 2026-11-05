package local.lito381.server;
import java.io.IOException;import java.util.*;
/** Controlled29/152/153 final values use the existing complete sender, with no producer in the queue. */
final class NativeSourceNegativeFinalFrame {
 static final class Scope {
  final NativeSourceSelfFinalFrame.First first;final NativeSourceWeaponFinalFrame.Participant source,target;final NativeSourceWeaponFinalFrame.Npc npc;
  final IdentityHashMap<ClientSession,NativeSourceSelfFinalFrame.Participant> receivers=new IdentityHashMap<>();
  final int x,y;final NativeCompanionPlacement.State placement;final Object pose,scene,master;
  Scope(NativeSourceNegativeTargets.Aim a,NativeSourceNegativePairFirst.First value){
   first=value==null?null:value.source();var p=value==null?null:value.proof();source=p==null?null:NativeSourceWeaponFinalFrame.participant(p.source(),p.sourceTimed(),false,false);target=p==null?null:NativeSourceWeaponFinalFrame.participant(p.member(),p.peerTimed(),false,false);
   var n=p==null?null:p.npc();npc=n==null?null:new NativeSourceWeaponFinalFrame.Npc(n.kind(),n.owner(),n.actor(),p.actorTimed(),p.actorLife(),p.generation());x=a.x;y=a.y;placement=a.placement;pose=a.pose;scene=a.scene;master=a.master;
   if(a.c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot)for(var entry:snapshot.members().entrySet()){
    var pc=entry.getKey();var member=entry.getValue();var receiver=NativeSourceSelfFinalFrame.receiver(pc);
    if(receiver!=null&&receiver.binding().equals(member.binding)&&member.binding.equals(a.c.bindings.get(pc))&&receiver.life()==member.acceptedLife&&member.life==member.acceptedLife&&member.nativeSourceNpcHpObserver!=null&&receiver.observer()==member.nativeSourceNpcHpObserver)receivers.put(pc,new NativeSourceSelfFinalFrame.Participant(pc,member.binding,receiver.timed(),member.acceptedLife,receiver.observer(),member.dead));
   }
  }
 }
 record Value(NativeSourceSelfFinalFrame.First first,NativeSourceWeaponFinalFrame.Participant source,NativeSourceWeaponFinalFrame.Participant target,NativeSourceWeaponFinalFrame.Npc npc,NativeSourceSelfFinalFrame.Participant receiver,NativeSourceSelfFinalFrame.Participant addressed,int x,int y,NativeCompanionPlacement.State placement,Object pose,Object scene,Object master,byte[] packet,String note){Value{packet=packet.clone();}}
 static void install(NativeSourceNegativeTargets.Aim a){if(NativeSourceNegativePairFirst.scoped(a))a.nativeSourceNegativeFinal=new Scope(a,a.nativeSourceNegativePairFirst instanceof NativeSourceNegativePairFirst.First first?first:null);}
 static void close(NativeSourceNegativeTargets.Aim a){if(a.nativeSourceNegativeFinal instanceof Scope scope)scope.receivers.clear();a.nativeSourceNegativeFinal=null;}
 static boolean register(NativeSourceNegativeTargets.Aim a){if(!NativeSourceNegativePairFirst.scoped(a))return false;a.registered=true;return true;}
 static boolean ownImpact(Value v){if(v.receiver().pc()!=v.source().pc()||v.packet().length==0)return false;int op=v.packet()[0]&255;return op==Protocol.S_MANA_POINT||op==Protocol.S_HIT_POINT||op==Protocol.S_CHANGE_DIRECTION;}
 static boolean current(Value v){
  if(v==null||v.source()==null||v.target()==null||v.npc()==null||v.receiver()==null||!NativeSourceSelfFinalFrame.current(v.first()))return false;var hub=v.first().hub();
  if(!NativeSourceWeaponFinalFrame.member(v.source(),hub)||!NativeSourceSelfFinalFrame.member(v.receiver(),hub)||v.addressed()!=null&&!NativeSourceSelfFinalFrame.member(v.addressed(),hub))return false;
  if(ownImpact(v))return true;
  if(!NativeSourceWeaponFinalFrame.member(v.target(),hub)||!NativeSourceWeaponFinalFrame.npc(v.npc(),v.target())||NativeSourceRawDamage.number(v.source().pc(),"positionX")!=v.source().x()||NativeSourceRawDamage.number(v.source().pc(),"positionY")!=v.source().y()||NativeSourceRawDamage.number(v.npc().actor(),"x")!=v.x()||NativeSourceRawDamage.number(v.npc().actor(),"y")!=v.y())return false;
  var placement=NativeCompanionPlacement.state((BattleCompanion)v.npc().actor());return placement==v.placement()&&(placement==null||placement.owner.get()==v.target().pc()&&placement.pose==v.pose()&&placement.scene==v.scene()&&placement.master==v.master());
 }
 static boolean capture(ClientSession receiver,byte[] bytes,String note){
  var a=NativeSourceNegativeTargets.aim();if(!NativeSourceNegativePairFirst.scoped(a))return false;
  if(!(a.nativeSourceNegativeFinal instanceof Scope scope))return true;var participant=scope.receivers.get(receiver);NativeSourceSelfFinalFrame.Participant addressed=null;
  if(bytes.length>=5&&Set.of(Protocol.S_EFFECT,Protocol.S_SPEED,Protocol.S_ACTION,Protocol.S_POISON).contains(bytes[0]&255)){long id=NativeStaticNpcMotion.id(bytes,1);for(var p:scope.receivers.values())if(p.binding().object()==id){addressed=p;break;}}
  var value=new Value(scope.first,scope.source,scope.target,scope.npc,participant,addressed,scope.x,scope.y,scope.placement,scope.pose,scope.scene,scope.master,bytes,note);
  if(current(value))NativeLightAction.deferScoped(()->send(value));return true;
 }
 static void send(Value value)throws IOException{if(current(value))value.receiver().pc().nativeSourceWeaponSelfSend(value.packet(),value.note(),value);}
 static boolean kind(Object value){return value instanceof Value;}
 static boolean frame(ClientSession pc,byte[] bytes,String note,Object value)throws IOException{
  if(!(value instanceof Value v))return false;if(v.receiver()!=null&&v.receiver().pc()==pc){var actual=new Value(v.first(),v.source(),v.target(),v.npc(),v.receiver(),v.addressed(),v.x(),v.y(),v.placement(),v.pose(),v.scene(),v.master(),bytes,note);pc.nativeSourceWeaponStaticFrame(actual.packet(),actual.note(),actual);}return true;
 }
 static boolean allow(ClientSession pc,byte[] bytes,Object value){if(!Thread.holdsLock(pc))throw new IllegalStateException("negative final proof requires original receiver monitor");return value instanceof Value v&&v.receiver()!=null&&v.receiver().pc()==pc&&Arrays.equals(bytes,v.packet())&&current(v);}
 static Throwable collect(Throwable first,Throwable next){if(first==null)return next;if(first!=next)first.addSuppressed(next);return first;}
 static void cleanup(NativeSourceNegativeTargets.Aim a,NativeCompanionTargets.Scope scope,Throwable primary)throws IOException{
  Throwable failure=null;try{a.close();}catch(RuntimeException|Error e){failure=e;}
  try{scope.close();}catch(RuntimeException|Error e){failure=collect(failure,e);}
  // The original close marks itself closed before visiting receipts; finish all remaining releases even on failure.
  try{for(var receipt:new ArrayList<>(scope.receipts.values()))if(!receipt.released.get())try{receipt.close();}catch(RuntimeException|Error e){failure=collect(failure,e);}}
  finally{scope.receipts.clear();scope.bindings.clear();scope.caster.clear();}
  if(failure!=null){if(primary!=null){collect(primary,failure);return;}if(failure instanceof RuntimeException e)throw e;if(failure instanceof Error e)throw e;throw new IOException(failure);}
 }
 static void produce(NativeSourceNegativeTargets.Aim a,NativeAreaMaterialAction.Context c,NativeCompanionTargets.Scope scope,Object spell,ClientSession pc,byte[] packet,int fixture)throws IOException{
  if(!NativeSourceNegativePairFirst.scoped(a)){NativeSourceNegativeTargets.nativeSourceNegativeFinalOriginalProducer(a,c,scope,spell,pc,packet,fixture);return;}
  Throwable primary=null;try{NativeSourceNegativeTargets.nativeSourceNegativeFinalOriginalProducer(a,c,scope,spell,pc,packet,fixture);}catch(IOException|RuntimeException|Error e){primary=e;throw e;}finally{cleanup(a,scope,primary);}
 }
 private NativeSourceNegativeFinalFrame(){}
}
