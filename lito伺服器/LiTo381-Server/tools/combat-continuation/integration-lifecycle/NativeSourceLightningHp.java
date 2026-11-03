package local.lito381.server;
import java.util.*;
/** Source17 accepted HP output keeps its original event and existing completion/worker authority. */
final class NativeSourceLightningHp {
 static final class Linked extends AbstractList<Object> implements RandomAccess {
  final List<?> values;final NativeAreaMaterialAction.Context c;final ClientSession target;final Object casterLife;final int cx,cy,x,y;volatile Object targetLife;
  Linked(List<?> values,NativeAreaMaterialAction.Context c,ClientSession target){this.values=values;this.c=c;this.target=target;cx=n(c.pc,"positionX");cy=n(c.pc,"positionY");x=n(target,"positionX");y=n(target,"positionY");casterLife=life(c.pc);targetLife=life(target);}
  public Object get(int n){return values.get(n);}public int size(){return values.size();}
 }
 static int n(ClientSession p,String key){return(int)CombatAccess.field(p,key);}
 static Object life(ClientSession p){return NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(p,"timedStatus"),true);}
 static List<?> link(List<?> values,NativePlayerCommit.Binding target,NativePlayerCommit.Binding caster){var original=NativeAcceptedFreezeMutation.link(values,target,caster);var a=NativeSourceLightningLifetime.aim();if(a==null||caster==null||!caster.equals(a.c.caster))return original;for(var p:a.c.players)if(target.equals(a.c.bindings.get(p)))return new Linked(original,a.c,p);return original;}
 static NativeAcceptedHpCompletion enqueue(NativePlayerCommit.Frame frame,NativePlayerVitalCompletions.Receipt receipt){if((Object)frame.event.defense()instanceof Linked linked&&frame.accepted&&frame.fatal)linked.targetLife=life(frame.pc);return NativeAcceptedHpCompletion.enqueue(frame,receipt);}
 /** Executed inside the existing receiver monitor; read existing role/life/pose tokens only. */
 static boolean allowFrame(ClientSession receiver,byte[] packet){var frame=NativePlayerCommit.frame();if(frame==null||!((Object)frame.event.defense()instanceof Linked l))return true;var c=l.c;var binding=c.bindings.get(receiver);if(!frame.accepted||binding==null||NativePlayerLifetime.of(c.hub).mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")||!NativeAcceptedFreezeMutation.roleFrame(binding,receiver)||!NativeAcceptedFreezeMutation.roleFrame(c.caster,c.pc)||!NativeAcceptedFreezeMutation.roleFrame(frame.event.target(),l.target)||(boolean)CombatAccess.field(c.pc,"sourceItemDisconnected")||(boolean)CombatAccess.field(l.target,"sourceItemDisconnected")||n(c.pc,"positionX")!=l.cx||n(c.pc,"positionY")!=l.cy||n(l.target,"positionX")!=l.x||n(l.target,"positionY")!=l.y)return false;var caster=CombatAccess.field(CombatAccess.field(c.pc,"timedStatus"),"sourcePoisonLifecycle");var target=CombatAccess.field(CombatAccess.field(l.target,"timedStatus"),"sourcePoisonLifecycle");return caster!=null&&target!=null&&CombatAccess.field(caster,"sourceNpcOwner")==l.casterLife&&CombatAccess.field(target,"sourceNpcOwner")==l.targetLife;}
 private NativeSourceLightningHp(){}
}
