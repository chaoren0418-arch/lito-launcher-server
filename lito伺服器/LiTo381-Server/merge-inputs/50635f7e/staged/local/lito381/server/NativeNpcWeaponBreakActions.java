package local.lito381.server;
import java.io.IOException;import java.util.*;
/** Existing ordinary WORLD NPC producers use the already-qualified role/output boundary. */
final class NativeNpcWeaponBreakActions {
 static void normal(ClientSession pc,ClassicWorldMonsterPopulation.Attack attack)throws IOException{
  if(!CombatRules.loaded()){pc.nativeNpcWeaponMonsterBody(attack);return;}if(attack==null)return;
  NativeLightAction.commitPair(pc,pc,()->{if(!NativeCombatOrigin.validWorldAction(pc,attack))return;var origin=NativeCombatOrigin.enterWorldAction(pc,attack);try{pc.nativeNpcWeaponMonsterBody(attack);}finally{NativeCombatOrigin.leaveWorldAction(origin);}});
 }
 static void companion(ClientSession pc,ClassicWorldMonsterPopulation.Attack attack,long id)throws IOException{
  if(!CombatRules.loaded()){pc.nativeNpcWeaponCompanionBody(attack,id);return;}if(attack==null)return;
  var master=NativePlayerCommit.Binding.capture(pc);var pet=(BattleCompanion)CombatAccess.call(pc,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",id);var stamp=NativeCompanionCallbacks.capture(master,pet);if(stamp==null)return;
  NativeLightAction.commitPair(pc,pc,()->{if(!master.matches(pc)||!sameCompanion(pc,pet,stamp)||!NativeCombatOrigin.validWorldAction(pc,attack))return;var origin=NativeCombatOrigin.enterWorldAction(pc,attack);try{pc.nativeNpcWeaponCompanionBody(attack,id);}finally{NativeCombatOrigin.leaveWorldAction(origin);}});
 }
 private static boolean sameCompanion(ClientSession pc,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  if(CombatAccess.call(pc,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",stamp.companion)!=pet)return false;
  synchronized(pet){return pet.snapshot().active()&&pet.snapshot().hp()>0&&stamp.life==NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);}
 }
 static void dspy(ClientSession pc,ClassicWorldMonsterPopulation.Attack attack,long id)throws IOException{
  if(!CombatRules.loaded()){pc.nativeNpcWeaponDspyBody(attack,id);return;}if(attack==null)return;
  var master=NativePlayerCommit.Binding.capture(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");var escorts=hub.classicWorldDspyEscorts();final Object follower;synchronized(escorts){follower=((Map<?,?>)CombatAccess.field(escorts,"followers")).get(id);}if(follower==null)return;
  NativeLightAction.commitPair(pc,pc,()->{if(!master.matches(pc)||!NativeCombatOrigin.validWorldAction(pc,attack))return;synchronized(escorts){if(((Map<?,?>)CombatAccess.field(escorts,"followers")).get(id)!=follower)return;}var origin=NativeCombatOrigin.enterWorldAction(pc,attack);try{pc.nativeNpcWeaponDspyBody(attack,id);}finally{NativeCombatOrigin.leaveWorldAction(origin);}});
 }
 private NativeNpcWeaponBreakActions(){}
}
