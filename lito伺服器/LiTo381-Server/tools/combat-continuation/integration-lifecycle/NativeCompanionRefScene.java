package local.lito381.server;
/** Stateless WORLD companion resolution. Player action bindings and BATTLE retain their original meaning. */
final class NativeCompanionRefScene {
 static boolean worldRef(NativeCombatAttacker.Ref ref){return ref!=null&&ref.kind()==2&&ref.master()!=null&&ref.master().phase().equals("WORLD");}
 static GameHub hub(ClientSession pc){return NativeCompanionPlacement.hub(pc);}
 static boolean binding(NativePlayerCommit.Binding binding,ClientSession owner){return binding.phase().equals("WORLD")?owner!=null&&NativeCompanionRoleIdentity.actorBinding(binding,owner):binding.matches(owner);}
 static NativeCombatAttacker.Ref capture(ClientSession owner,BattleCompanion pet){
  var ref=NativeCombatAttacker.nativeRefOriginalCompanion(owner,pet);if(!worldRef(ref))return ref;
  var h=hub(owner);synchronized(h){synchronized(pet){
   if(!binding(ref.master(),owner)||CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id())!=pet)return null;
   var timed=(BattleTimedStatus)CombatAccess.field(pet,"timedStatus");var snapshot=pet.snapshot();
   if(!snapshot.active()||snapshot.hp()<=0||NativeCombatAttacker.token(timed,false)!=ref.owner())return null;
   var state=NativeCompanionPlacement.state(pet);int map,instance;
   if(state==null){map=NativeCompanionPlacement.map(owner);instance=NativeCompanionPlacement.instance(owner);}
   else {if(!state.role(owner)||!state.scene.alive(h))return null;map=state.scene.map;instance=state.scene.instance;}
   return new NativeCombatAttacker.Ref(2,ref.id(),ref.owner(),ref.epoch(),ref.master(),ref.room(),map,instance);
  }}
 }
 /** Called under the original pet monitor and, for WORLD companions, the outer hub monitor. */
 static boolean liveSame(NativeCombatAttacker.Ref ref,Object actor,BattleTimedStatus timed,ClientSession owner){
  if(ref.owner()!=NativeCombatAttacker.token(timed,false))return false;if(!worldRef(ref))return true;
  if(!(actor instanceof BattleCompanion pet)||!binding(ref.master(),owner)||CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id())!=pet)return false;
  var h=hub(owner);if(!Thread.holdsLock(h))throw new IllegalStateException("WORLD Ref requires hub before pet");
  var state=NativeCompanionPlacement.state(pet);
  return state==null?NativeCompanionPlacement.map(owner)==ref.map()&&NativeCompanionPlacement.instance(owner)==ref.instance()&&!(boolean)CombatAccess.field(h,"nativeStaticMotionClosed"):state.role(owner)&&state.scene.at(ref.map(),ref.instance())&&state.scene.alive(h);
 }
 private NativeCompanionRefScene(){}
}
