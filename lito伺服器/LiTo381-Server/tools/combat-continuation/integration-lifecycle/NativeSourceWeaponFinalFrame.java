package local.lito381.server;
import java.io.IOException;import java.util.*;
/** First participants carried by an existing output task; no Context/Frame/TLS survives producing scope. */
final class NativeSourceWeaponFinalFrame {
 record Participant(ClientSession pc,NativePlayerCommit.Binding binding,Object timed,Object life,int x,int y,boolean observer,boolean position){}
 record Npc(int kind,Object owner,Object actor,Object timed,Object life,Object generation){}
 record Proof(Participant source,Participant target,Npc npc,Participant receiver,NativePlayerLifetime lifetime,GameHub hub){}
 record Output(Proof proof,byte[] packet,String note){Output{packet=packet.clone();}}
 static Object state(Object status){return status==null?null:CombatAccess.field(status,"sourcePoisonLifecycle");}
 static Participant participant(NativeSourceRawDamage.Member m,Object timed,boolean observer,boolean position){return m==null?null:new Participant(m.pc,m.binding,timed,m.life,m.x,m.y,observer,position);}
 static boolean member(Participant p,GameHub hub){
  if(p==null||p.binding()==null||p.binding().owner()==null||p.life()==null)return false;var pc=p.pc();Object status=CombatAccess.field(pc,"timedStatus");if(status==null||p.timed()!=null&&p.timed()!=status)return false;Object s=state(status);
  return s!=null&&CombatAccess.field(s,"sourcePlayerOwner")==p.binding().owner()&&CombatAccess.field(s,"sourceNpcOwner")==p.life()&&p.binding().matchesFast(pc)&&CombatAccess.field(pc,"hub")==hub&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&(boolean)CombatAccess.field(pc,"observer")==p.observer()&&(!p.position()||NativeSourceRawDamage.number(pc,"positionX")==p.x()&&NativeSourceRawDamage.number(pc,"positionY")==p.y()&&NativeSourceRawDamage.number(pc,"playerHp")>0);
 }
 static boolean npc(Npc n,Participant target){
  if(n==null)return true;Object status=CombatAccess.field(n.actor(),n.kind()==3?"timedStatus":"damagePoison");if(status==null||status!=n.timed()||n.life()==null)return false;Object s=state(status);if(s==null||CombatAccess.field(s,"sourceNpcOwner")!=n.life()||NativeSourceRawDamage.number(n.actor(),"hp")<=0)return false;
  if(n.kind()==3){Object tamed=CombatAccess.field(target.pc(),"tamedCompanions");return (boolean)CombatAccess.field(n.actor(),"active")&&(CombatAccess.field(target.pc(),"battleCompanion")==n.actor()||tamed instanceof List<?> pets&&pets.contains(n.actor()));}
  return (n.kind()==1||n.kind()==2)&&CombatAccess.field(n.owner(),"sourceProcEncounterGeneration")==n.generation();
 }
 static boolean ownImpact(ClientSession receiver,byte[] bytes,Proof p){if(receiver!=p.source().pc()||bytes==null||bytes.length==0)return false;int op=bytes[0]&255;return op==Protocol.S_HIT_POINT||op==Protocol.S_MANA_POINT||op==Protocol.S_CHANGE_DIRECTION||op==Protocol.S_CHANGE_ITEM_USE||op==Protocol.S_CHANGE_ITEM_DESC||op==Protocol.S_CHANGE_ITEM_DESC_EX||op==Protocol.S_CHANGE_COUNT||op==Protocol.S_REMOVE_INVENTORY;}
 static boolean current(Output out){
  if(out==null)return false;var p=out.proof();return p!=null&&CombatRules.loaded()&&p.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&CombatAccess.field(p.hub(),"sourcePlayerCommitLifetime")==p.lifetime()&&!(boolean)CombatAccess.field(p.hub(),"nativeStaticMotionClosed")&&member(p.source(),p.hub())&&(ownImpact(p.receiver().pc(),out.packet(),p)||member(p.target(),p.hub())&&npc(p.npc(),p.target()))&&member(p.receiver(),p.hub());
 }
 static Output output(NativeAreaMaterialAction.Context c,ClientSession receiver,byte[] bytes,String note){
  var first=NativeSourceTargetNegativeLife.proof(c);if(first==null||!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot))return null;var m=snapshot.members().get(receiver);if(m==null||m.nativeSourceNpcHpObserver==null||!m.binding.equals(c.bindings.get(receiver)))return null;
  var source=participant(first.source(),first.sourceTimed(),false,true);var target=participant(first.member(),first.peerTimed(),false,true);var audience=participant(m,receiver==c.pc?first.sourceTimed():receiver==first.peer()?first.peerTimed():null,m.nativeSourceNpcHpObserver,false);var n=first.npc();var npc=n==null?null:new Npc(n.kind(),n.owner(),n.actor(),first.actorTimed(),first.actorLife(),first.generation());var out=new Output(new Proof(source,target,npc,audience,first.life(),c.hub),bytes,note);return current(out)?out:null;
 }
 static boolean capture(ClientSession receiver,byte[] bytes,String note)throws IOException{
  var c=NativeAreaMaterialAction.current.get();if(!NativeSourceWeaponTargetLife.marked(c))return false;var out=output(c,receiver,bytes,note);if(out!=null&&!NativeLightAction.nativeSourceNpcHpDefer(()->send(out)))send(out);return true;
 }
 static void send(Output out)throws IOException{if(current(out))out.proof().receiver().pc().nativeSourceWeaponSelfSend(out.packet(),out.note(),out);}
 /** Called only by the complete original static-motion sibling, preserving any original packet transformation. */
 static void frame(ClientSession receiver,byte[] bytes,String note,Object value)throws IOException{if(value instanceof Output out&&out.proof().receiver().pc()==receiver){var actual=new Output(out.proof(),bytes,note);receiver.nativeSourceWeaponStaticFrame(actual.packet(),actual.note(),actual);}}
 static boolean allow(ClientSession receiver,byte[] bytes,Object value){if(!Thread.holdsLock(receiver))throw new IllegalStateException("weapon final guard lost original receiver monitor");return value instanceof Output out&&out.proof().receiver().pc()==receiver&&Arrays.equals(bytes,out.packet())&&current(out);}
 private NativeSourceWeaponFinalFrame(){}
}
