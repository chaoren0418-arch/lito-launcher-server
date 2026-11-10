package local.lito381.server;
/** Context-bound factories borrow earlier proofs. Missing discoveries are explicitly stale, never new authority. */
final class NativeSourcePlayerFactories {
 private static final Object denied=new Object();
 static boolean scoped(NativeAreaMaterialAction.Context c){return c!=null&&"BATTLE".equals(c.phase)&&(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot||c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Builder);}
 static NativePlayerCommit.Binding caster(ClientSession pc,NativeAreaMaterialAction.Context c){
  if(!CombatRules.loaded()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE"))return NativePlayerCommit.Binding.capture(pc);
  Object life=NativeSourceControlledDisplay.token(pc);var binding=NativePlayerCommit.Binding.capture(pc);NativeSourceFirstPlayers.remember(c,pc,binding,life);return binding;
 }
 static NativeSourceRawDamage.Member retained(ClientSession pc,NativePlayerCommit.Binding binding,NativeAreaMaterialAction.Context c){
  if(!scoped(c))return new NativeSourceRawDamage.Member(pc,binding);
  var m=NativeSourceSignedRecovery.member(pc,binding,c);return m!=null?m:new NativeSourceRawDamage.Member(pc,binding,denied);
 }
 static NativeSourceRawDamage.Member retained(ClientSession pc,NativePlayerCommit.Binding binding,Object fallback,NativeAreaMaterialAction.Context c){return scoped(c)?retained(pc,binding,c):new NativeSourceRawDamage.Member(pc,binding,fallback);}
 static NativeSourceDamageDisplay.Actor from(NativeSourceRawDamage.Member m){return new NativeSourceDamageDisplay.Actor(m.pc,m.binding,m.acceptedLife,m.x,m.y);}
 static NativeSourceDamageDisplay.Actor actor(ClientSession pc,NativePlayerCommit.Binding binding,Object fallback,int x,int y,NativeAreaMaterialAction.Context c){
  if(!scoped(c))return new NativeSourceDamageDisplay.Actor(pc,binding,fallback,x,y);
  var m=retained(pc,binding,c);
  if(!NativeSourceHealingCast.scoped(c)&&!(c.nativeSourceRawAction instanceof NativeSourceRawDamage.Action a&&a.members.get(pc)!=null)){
   NativeSourceDamageDisplay.Actor old=null;
   if(c.nativeSourceDollAction instanceof NativeSourceDollAction.Action a&&a.stamp.caster()==pc)old=a.stamp.source();
   else if(c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot s&&s.source().pc()==pc)old=s.source();
   if(old!=null&&old.binding().equals(binding)&&old.life()==m.acceptedLife)return old;
  }
  return from(m);
 }
 static NativeSourceDamageDisplay.Actor capture(ClientSession pc,NativeAreaMaterialAction.Context c){
  if(!scoped(c))return NativeSourceDamageDisplay.Actor.capture(pc);
  var binding=pc==c.pc?c.caster:c.bindings.get(pc);return actor(pc,binding,denied,0,0,c);
 }
 static NativeSourceDamageDisplay.Actor contextActor(ClientSession pc){var c=NativeAreaMaterialAction.current.get();return pc!=null&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&scoped(c)?capture(pc,c):null;}
 static Object targetToken(Object fallback,Object actor,NativeAreaMaterialAction.Context c){if(actor instanceof ClientSession pc&&scoped(c)){var binding=pc==c.pc?c.caster:c.bindings.get(pc);return retained(pc,binding,c).acceptedLife;}return fallback;}
 private NativeSourcePlayerFactories(){}
}
