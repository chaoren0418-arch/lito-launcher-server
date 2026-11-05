package local.lito381.server;
import java.util.*;
/** Action generations remain strict; WORLD actors additionally retain a bounded character-role identity. */
final class NativeCompanionRoleIdentity {
 static final class Role {volatile boolean active=true;}
 static final class Epoch {final Role role;Epoch(Role role){this.role=role;}}
 static Object nextRole(ClientSession pc){var status=CombatPoisonAtomic.status(pc);if(status!=null){var state=CombatPoisonLifecycle.monsterPoisonCallbacks(status,false);var old=state==null?null:CombatAccess.field(state,"sourcePlayerOwner");if(old instanceof Epoch epoch)epoch.role.active=false;}return new Epoch(new Role());}
 static void advanceScene(ClientSession pc,Object value,String field){Object current=NativePlayerCommit.owner(pc);boolean preserve=NativeCompanionPlacement.world(pc)&&Set.of("phase","classicWorldMapId","classicWorldInstanceId","currentRoomId").contains(field)&&(!field.equals("phase")||String.valueOf(value).equals("WORLD"));Object next=preserve&&current instanceof Epoch epoch&&epoch.role.active?new Epoch(epoch.role):nextRole(pc);NativePlayerCommit.nativeCompanionSceneOriginalAdvance(pc,next);}
 static boolean sameRole(NativePlayerCommit.Binding before,NativePlayerCommit.Binding after){return before!=null&&after!=null&&before.owner() instanceof Epoch a&&after.owner() instanceof Epoch b&&a.role==b.role&&a.role.active&&before.object()==after.object()&&before.source().equals(after.source())&&before.account().equals(after.account())&&before.selected().equals(after.selected())&&before.phase().equals("WORLD")&&after.phase().equals("WORLD");}
 static Object currentOwner(ClientSession pc){var status=CombatPoisonAtomic.status(pc);var state=status==null?null:CombatAccess.field(status,"sourcePoisonLifecycle");return state==null?null:CombatAccess.field(state,"sourcePlayerOwner");}
 static boolean actorBinding(NativePlayerCommit.Binding binding,ClientSession owner){var current=currentOwner(owner);return binding!=null&&binding.owner() instanceof Epoch before&&current instanceof Epoch after&&before.role==after.role&&before.role.active&&binding.phase().equals("WORLD")&&NativeCompanionPlacement.world(owner)&&binding.object()==owner.playerObjectId()&&binding.source().equals(owner.sourceCharacterIdentity())&&binding.account().equals(owner.sourceAccountIdentity())&&binding.selected().equals(owner.selectedCharacterName())&&!(boolean)CombatAccess.field(owner,"sourceItemDisconnected");}
 static boolean stateRole(NativeCompanionPlacement.State state,ClientSession pc){return NativeCompanionPlacement.world(pc)?state.owned(pc)&&actorBinding(state.master,pc):state.nativeRoleOriginalRole(pc);}
 static boolean transitionCharacter(NativeCompanionPlacement.Transition transition,ClientSession pc){return transition.nativeRoleOriginalCharacter(pc)&&actorBinding(transition.before,pc);}
 static boolean npc(NativeAreaMaterialAction.Context c,long id){
  var n=c.npcs.get(id);if(n==null||!c.phase.equals("WORLD")||n.kind()!=3||!(n.epoch() instanceof Object[] stamp))return NativeCompanionTargets.nativeRoleOriginalNpc(c,id);
  if(stamp.length!=3||!(stamp[0] instanceof NativeCombatAttacker.Ref ref)||ref.kind()!=2||ref.id()!=id)return false;
  var owner=(ClientSession)n.owner();var pet=(BattleCompanion)n.actor();var binding=c.bindings.get(owner);
  if(!sameRole(binding,ref.master())||!actorBinding(binding,owner))return false;
  synchronized(c.hub){synchronized(pet){var s=NativeCompanionPlacement.state(pet);var snap=pet.snapshot();return actorBinding(binding,owner)&&s!=null&&s.owned(owner)&&sameRole(s.master,binding)&&actorBinding(s.master,owner)&&s.scene==stamp[2]&&s.pose==stamp[1]&&s.scene.at(c.map,c.instance)&&s.scene.alive(c.hub)&&snap.objectId()==id&&snap.active()&&snap.hp()>0&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false)==ref.owner();}}
 }
 static boolean owns(NativeCompanionTargets.Scope scope,NativeAreaMaterialAction.Context c,ClientSession owner){
  if(scope.nativeRoleOriginalOwns(c,owner))return true;var receipt=scope.receipts.get(owner);var frozen=scope.bindings.get(owner);
  if(scope.closed||!scope.valid||c==null||!c.phase.equals("WORLD")||c.pc!=scope.caster.get()||!scope.binding.matches(c.pc)||receipt==null||receipt.pc!=owner||receipt.released.get()||frozen==null||!frozen.equals(c.bindings.get(owner))||!c.pc.nativePlayerActionLock().isHeldByCurrentThread()||!owner.nativePlayerActionLock().isHeldByCurrentThread())return false;
  for(var n:c.npcs.values())if(n.kind()==3&&n.owner()==owner&&n.epoch() instanceof Object[]&&npc(c,n.id()))return true;return false;
 }
 private NativeCompanionRoleIdentity(){}
}
