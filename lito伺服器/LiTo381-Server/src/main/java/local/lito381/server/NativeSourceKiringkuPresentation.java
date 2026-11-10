package local.lito381.server;
import java.io.*;import java.util.*;
/** Hit-only Source visual. Ephemeral arguments own its lifetime; no worker, TLS, registry or HP authority. */
final class NativeSourceKiringkuPresentation {
 record Viewer(ClientSession pc,NativePlayerCommit.Binding binding){}
 record Output(ClientSession caster,NativePlayerCommit.Binding binding,GameHub hub,NativePlayerLifetime lifetime,Object life,int x,int y,byte[] packet,List<Viewer> viewers,ClientSession target,NativePlayerCommit.Binding targetBinding,Object targetLife,int tx,int ty){}
 static int n(ClientSession p,String field){return(int)CombatAccess.field(p,field);}
 static Object life(ClientSession p){return NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(p,"timedStatus"),true);}
 static void broadcast(ClientSession caster,List<ClientSession> members,boolean hit,BattleKiringkuDamage.Outcome old,ClientSession target)throws IOException{
  if(!CombatRules.loaded()){byte[] packet=Protocol.effectObject(caster.playerObjectId(),old.effectGfx());for(var pc:members)pc.nativeSelfOriginalSend(packet,"S_EFFECT retained non-Source kiringku");return;}
  var settings=NativeSourceOtherControls.settings();if(!hit||target!=null&&n(target,"playerHp")<=0||!settings.kiringku()||settings.kiringkuSprite()<=0)return;
  var hub=(GameHub)CombatAccess.field(caster,"hub");var viewers=new ArrayList<Viewer>();for(var pc:members)viewers.add(new Viewer(pc,NativePlayerCommit.Binding.capture(pc)));
  var output=new Output(caster,NativePlayerCommit.Binding.capture(caster),hub,NativePlayerLifetime.of(hub),life(caster),n(caster,"positionX"),n(caster,"positionY"),Protocol.effectObject(caster.playerObjectId(),settings.kiringkuSprite()),List.copyOf(viewers),target,target==null?null:NativePlayerCommit.Binding.capture(target),target==null?null:life(target),target==null?0:n(target,"positionX"),target==null?0:n(target,"positionY"));
  for(var viewer:output.viewers)send(output,viewer);
 }
 /** Check at the actual receiver monitor; only existing volatile/role/life/pose reads. */
 static boolean valid(Output out,Viewer viewer){
  if(out.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(out.hub,"nativeStaticMotionClosed")||!NativeAcceptedFreezeMutation.roleFrame(out.binding,out.caster)||!NativeAcceptedFreezeMutation.roleFrame(viewer.binding,viewer.pc)||(boolean)CombatAccess.field(out.caster,"sourceItemDisconnected")||(boolean)CombatAccess.field(viewer.pc,"sourceItemDisconnected")||n(out.caster,"playerHp")<=0||n(out.caster,"positionX")!=out.x||n(out.caster,"positionY")!=out.y||!BattleSpellCatalog.isInNativeScreen(n(viewer.pc,"positionX"),n(viewer.pc,"positionY"),out.x,out.y))return false;
  if(out.target!=null){if(!NativeAcceptedFreezeMutation.roleFrame(out.targetBinding,out.target)||(boolean)CombatAccess.field(out.target,"sourceItemDisconnected")||n(out.target,"playerHp")<=0||n(out.target,"positionX")!=out.tx||n(out.target,"positionY")!=out.ty)return false;var targetState=CombatAccess.field(CombatAccess.field(out.target,"timedStatus"),"sourcePoisonLifecycle");if(targetState==null||CombatAccess.field(targetState,"sourceNpcOwner")!=out.targetLife)return false;}
  var state=CombatAccess.field(CombatAccess.field(out.caster,"timedStatus"),"sourcePoisonLifecycle");return state!=null&&CombatAccess.field(state,"sourceNpcOwner")==out.life;
 }
 static void send(Output out,Viewer viewer)throws IOException{synchronized(viewer.pc){if(valid(out,viewer))viewer.pc.nativeSelfOriginalSend(out.packet,"S_EFFECT Source other.properties kiringku");}}
 private NativeSourceKiringkuPresentation(){}
}
