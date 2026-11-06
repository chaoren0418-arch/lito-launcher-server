package local.lito381.server;
import java.io.IOException;import java.util.*;
/** First self-cast values survive the original transport without retaining producing scopes. */
final class NativeSourceSelfFinalFrame {
 record Participant(ClientSession pc,NativePlayerCommit.Binding binding,Object timed,Object life,boolean observer,boolean dead){}
 record First(Participant source,NativePlayerLifetime lifetime,GameHub hub,Object application,NativeServerLifetime serverApplication){}
 record SelfTask(First first,NativeSelfCastAction.Task original) implements NativeSelfCastAction.Task {public void run()throws IOException{if(current(first)&&first.source().pc().nativePlayerActionLock().isHeldByCurrentThread())original.run();}}
 static final class Scope {final First first;final IdentityHashMap<ClientSession,Participant> receivers=new IdentityHashMap<>();Scope(First f){first=f;}}
 record Output(First first,Participant receiver,byte[] packet,String note){Output{packet=packet.clone();}}
 static Object state(Object timed){return timed==null?null:CombatAccess.field(timed,"sourcePoisonLifecycle");}
 static Object life(Object timed){Object state=state(timed);return state==null?null:CombatAccess.field(state,"sourceNpcOwner");}
 static boolean member(Participant p,GameHub hub){
  if(p==null||p.binding()==null||p.binding().owner()==null||p.timed()==null)return false;Object timed=CombatAccess.field(p.pc(),"timedStatus"),state=state(timed);
  return timed==p.timed()&&state!=null&&CombatAccess.field(state,"sourcePlayerOwner")==p.binding().owner()&&CombatAccess.field(state,"sourceNpcOwner")==p.life()&&p.binding().matchesFast(p.pc())&&CombatAccess.field(p.pc(),"hub")==hub&&!(boolean)CombatAccess.field(p.pc(),"sourceItemDisconnected")&&(boolean)CombatAccess.field(p.pc(),"observer")==p.observer()&&(!p.dead()||NativeSourceRawDamage.number(p.pc(),"playerHp")==0);
 }
 static boolean current(First first){return first!=null&&first.lifetime()!=null&&CombatAccess.field(first.hub(),"sourcePlayerCommitLifetime")==first.lifetime()&&first.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&first.lifetime().application==first.serverApplication()&&CombatAccess.field(first.hub(),"sourceItemApplication")==first.application()&&(first.application()==null||!(boolean)CombatAccess.field(first.application(),"closed"))&&!(boolean)CombatAccess.field(first.hub(),"nativeStaticMotionClosed")&&member(first.source(),first.hub());}
 static First initial(ClientSession pc,Object ingress){
  if(ingress==null||CombatAccess.field(ingress,"pc")!=pc)return null;var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");var receipt=(NativePlayerVitalCompletions.Receipt)CombatAccess.field(ingress,"receipt");var hub=(GameHub)CombatAccess.field(ingress,"hub");Object timed=CombatAccess.field(pc,"timedStatus");
  if(binding==null||binding.owner()==null||timed==null||receipt==null||receipt.pc!=pc||CombatAccess.field(pc,"hub")!=hub||CombatAccess.field(hub,"sourcePlayerCommitLifetime")!=receipt.life||!NativeSourceMaterialMain.locked(ingress,pc)||receipt.life.application!=receipt.app)return null;
  Object application=CombatAccess.field(ingress,"application");if(CombatAccess.field(hub,"sourceItemApplication")!=application||application!=null&&(boolean)CombatAccess.field(application,"closed"))return null;
  Object token;boolean dead=false;var frame=NativePlayerCommit.frame();
  if(frame!=null&&frame.accepted&&!frame.event.world()){
   if(!(frame.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof accepted))return null;
   var p=accepted.target()!=null&&accepted.target().pc()==pc?accepted.target():accepted.source()!=null&&accepted.source().pc()==pc?accepted.source():null;if(p==null||!p.binding().equals(binding))return null;token=p.life();dead=p.dead();
  }else if(NativeAreaMaterialAction.current.get()!=null&&"BATTLE".equals(NativeAreaMaterialAction.current.get().phase)){
   var c=NativeAreaMaterialAction.current.get();
   if(!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot))return null;var m=snapshot.members().get(pc);if(m==null||!m.binding.equals(binding))return null;token=m.acceptedLife;dead=m.dead;
   var known=NativeSourceTargetNegativeLife.proof(c);if(known!=null){if(known.source()!=null&&known.source().pc==pc)timed=known.sourceTimed();else if(known.peer()==pc)timed=known.peerTimed();}
  }else{
   // First ingress only: initialize the canonical actor token under its existing status monitor.
   // A prior accepted participant never falls through to this initialization path.
   synchronized(timed){Object s=state(timed);if(CombatAccess.field(pc,"timedStatus")!=timed||s==null||CombatAccess.field(s,"sourcePlayerOwner")!=binding.owner()||!binding.matchesFast(pc)||!NativeSourceMaterialMain.locked(ingress,pc))return null;token=NativeCombatAttacker.token((BattleTimedStatus)timed,true);}
  }
  var first=new First(new Participant(pc,binding,timed,token,(boolean)CombatAccess.field(pc,"observer"),dead),receipt.life,hub,CombatAccess.field(ingress,"application"),receipt.app);return current(first)?first:null;
 }
 static void initialize(Object ingress){var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");if(binding!=null&&binding.phase().equals("BATTLE")){var first=initial((ClientSession)CombatAccess.field(ingress,"pc"),ingress);CombatAccess.set(ingress,"nativeSourceSelfIngressFirst",first==null?Boolean.FALSE:first);}}
 static First first(ClientSession pc,Object ingress){Object value=CombatAccess.field(ingress,"nativeSourceSelfIngressFirst");return value instanceof First first&&first.source().pc()==pc&&current(first)?first:null;}
 static void retire(Object ingress){if(ingress!=null)CombatAccess.set(ingress,"nativeSourceSelfIngressFirst",Boolean.FALSE);}
 static boolean entry(ClientSession pc,Object ingress){if(ingress==null||CombatAccess.field(ingress,"pc")!=pc)return true;var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");return binding==null||!binding.phase().equals("BATTLE")||first(pc,ingress)!=null;}
 static void run(Runnable action,ClientSession pc){if(NativeLightAction.nativeSourceSelfIngressValid(pc))action.run();}
 static NativeSelfCastAction.Task task(ClientSession pc,NativeSelfCastAction.Task original,Object ingress){var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");if(binding==null||!binding.phase().equals("BATTLE")||NativeAreaMaterialAction.current.get()!=null)return original;return new SelfTask(first(pc,ingress),original);}
 static boolean matches(NativePlayerCommit.Binding binding,ClientSession pc,NativeSelfCastAction.Task task){if(!(task instanceof SelfTask self))return binding.matches(pc);return current(self.first())&&self.first().source().pc()==pc&&self.first().source().binding().equals(binding);}
 static void install(Object batch,NativeSelfCastAction.Task task){if(task instanceof SelfTask self&&self.first()!=null)CombatAccess.set(batch,"nativeSourceSelfFinalFirst",new Scope(self.first()));}
 static Participant receiver(ClientSession pc){
  Object timed=CombatAccess.field(pc,"timedStatus"),s=state(timed);if(s==null)return null;Object owner=CombatAccess.field(s,"sourcePlayerOwner");if(owner==null)return null;
  // The canonical Binding fields, read without owner(), capture(), token() or status creation.
  var binding=new NativePlayerCommit.Binding(owner,pc.playerObjectId(),pc.sourceCharacterIdentity(),pc.sourceAccountIdentity(),pc.selectedCharacterName(),String.valueOf(CombatAccess.field(pc,"phase")),(long)CombatAccess.field(pc,"currentRoomId"),(int)CombatAccess.field(pc,"classicWorldMapId"),(int)CombatAccess.field(pc,"classicWorldInstanceId"));
  return new Participant(pc,binding,timed,life(timed),(boolean)CombatAccess.field(pc,"observer"),false);
 }
 static boolean capture(Object batch,ClientSession pc,byte[] bytes,String note){
  if(batch==null||!(CombatAccess.field(batch,"nativeSourceSelfFinalFirst")instanceof Scope scope))return false;
  if(!(boolean)CombatAccess.field(batch,"captureSelf"))return false;if(!scope.first.source().pc().nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("self output requires original source action lock");
  if(!scope.receivers.containsKey(pc))scope.receivers.put(pc,pc==scope.first.source().pc()?scope.first.source():receiver(pc));
  var out=new Output(scope.first,scope.receivers.get(pc),bytes,note);if(current(out)&&!NativeLightAction.nativeSourceNpcHpDefer(()->send(out)))throw new IllegalStateException("self output lost existing Batch transport");return true;
 }
 static boolean current(Output out){return out!=null&&current(out.first())&&member(out.receiver(),out.first().hub());}
 static void send(Output out)throws IOException{if(current(out))out.receiver().pc().nativeSourceWeaponSelfSend(out.packet(),out.note(),out);}
 static boolean kind(Object value){return value instanceof Output;}
 static boolean frame(ClientSession pc,byte[] bytes,String note,Object value)throws IOException{if(!(value instanceof Output out))return false;if(out.receiver()!=null&&out.receiver().pc()==pc){var actual=new Output(out.first(),out.receiver(),bytes,note);pc.nativeSourceWeaponStaticFrame(actual.packet(),actual.note(),actual);}return true;}
 static boolean allow(ClientSession pc,byte[] bytes,Object value){if(!Thread.holdsLock(pc))throw new IllegalStateException("self final proof lost original receiver monitor");return value instanceof Output out&&out.receiver()!=null&&out.receiver().pc()==pc&&Arrays.equals(bytes,out.packet())&&current(out);}
 static void produced(Object batch){Object scope=CombatAccess.field(batch,"nativeSourceSelfFinalFirst");if(scope instanceof Scope s)s.receivers.clear();CombatAccess.set(batch,"nativeSourceSelfFinalFirst",null);}
 static Throwable release(Object batch,Throwable primary,Throwable first){if(batch!=null&&batch.getClass().getName().equals("local.lito381.server.NativeLightAction$Batch")){Object scope=CombatAccess.field(batch,"nativeSourceSelfFinalFirst");if(scope instanceof Scope s)s.receivers.clear();return NativeSourceBatchTerminal.slot(batch,"nativeSourceSelfFinalFirst",primary,first);}return first;}
 private NativeSourceSelfFinalFrame(){}
}
