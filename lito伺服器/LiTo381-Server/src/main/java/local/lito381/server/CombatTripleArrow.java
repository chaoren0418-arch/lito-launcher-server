// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/triple-control/CombatTripleArrow.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
/** Per-attempt scope; never survives the synchronous calculation or applies to a normal attack. */
final class CombatTripleArrow {
 static final class Scope {
  final ClientSession caster;final NativePlayerCommit.Binding binding;final int index;final Scope previous;boolean consumed;
  Scope(ClientSession pc,int hit,Scope old){caster=pc;binding=NativePlayerCommit.Binding.capture(pc);index=hit;previous=old;}
 }
 private static final ThreadLocal<Scope> current=new ThreadLocal<>();
 static Scope enter(ClientSession pc,int index){var s=new Scope(pc,index,current.get());current.set(s);return s;}
 static void leave(Scope s){if(current.get()!=s)throw new IllegalStateException("triple-arrow scope mismatch");if(s.previous==null)current.remove();else current.set(s.previous);}
 static boolean idle(){return current.get()==null;}
 static BattleWeaponCritical.DamageOutcome apply(SourceWeaponPhysicalStats weapon,int subtotal,int chance,double existing,ClientSession pc){
  var scope=current.get();
  if(scope!=null&&scope.caster==pc&&!scope.consumed&&scope.index>=0&&scope.index<3&&scope.binding.matches(pc)&&weapon.present()&&(weapon.weaponType()==4||weapon.weaponType()==13)){
   scope.consumed=true;existing=Math.max(existing,CombatRules.number("Triple_Arrow_Dmg",1.0));
  }
  return BattleWeaponCritical.apply(weapon,subtotal,chance,existing);
 }
}
