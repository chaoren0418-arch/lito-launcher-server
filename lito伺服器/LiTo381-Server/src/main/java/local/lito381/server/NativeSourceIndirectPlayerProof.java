package local.lito381.server;
/** Indirect audience/selection proofs retain original participants and exact accepted freeze target values. */
final class NativeSourceIndirectPlayerProof {
 static NativeAcceptedFreezeMutation.Batch batch(NativeAreaMaterialAction.Context c,ClientSession pc){var state=CombatAccess.field(c,NativeAcceptedFreezeMutation.FIELD);return state instanceof NativeAcceptedFreezeMutation.Batch b?b.target==pc?b:null:state instanceof NativeAcceptedAreaFreezeMutation.Group g?g.targets.get(pc):null;}
 static NativeSourceAcceptedPlayers.Participant accepted(NativeSourceAcceptedPlayers.Participant original,NativePlayerCommit.Frame f){
  if(original!=null||f==null||!f.accepted||f.queue==null||f.event.world()||!f.event.target().phase().equals("BATTLE")||!((Object)f.event.defense() instanceof NativeAcceptedFreezeMutation.Linked linked))return original;
  var lock=CombatAccess.field(f.queue,"vitalLock");if(!(lock instanceof java.util.concurrent.locks.ReentrantLock vital)||!vital.isHeldByCurrentThread())return original;
  var b=linked.batch;if(b.target!=f.pc||!NativeSourcePlayerFactories.scoped(b.c)||!f.event.target().equals(b.c.bindings.get(f.pc))||!NativeAcceptedFreezeMutation.roleFrame(f.event.target(),f.pc))return original;
  Object life=NativeSourceControlledDisplay.rawLife(f.pc);return life==null?original:new NativeSourceAcceptedPlayers.Participant(f.pc,f.event.target(),life,NativeSourceRawDamage.number(f.pc,"positionX"),NativeSourceRawDamage.number(f.pc,"positionY"),f.fatal);
 }
 static boolean player(NativeAreaMaterialAction.Context c,ClientSession pc){
  if(!c.player(pc))return false;if(!NativeSourcePlayerFactories.scoped(c))return true;
  var b=batch(c,pc);if(b==null)return NativeSourceAreaPlayerProof.player(c,pc);
  var binding=c.bindings.get(pc);if(binding==null||!binding.equals(b.c.bindings.get(pc))||!NativeAcceptedFreezeMutation.roleFrame(binding,pc)||(boolean)CombatAccess.field(pc,"sourceItemDisconnected"))return false;
  var frame=b.frame;Object life=b.life;
  if(frame!=null&&frame.accepted){if(frame.pc!=pc||!frame.event.target().equals(binding)||!(frame.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof p)||p.target()==null)return false;var accepted=p.target();if(accepted.pc()!=pc||!accepted.binding().equals(binding)||accepted.dead()!=frame.fatal||accepted.life()!=(frame.fatal?b.deathLife:b.life))return false;life=accepted.life();}
  return life!=null&&NativeSourceControlledDisplay.rawLife(pc)==life;
 }
 static Object petLife(Object original,NativeAreaMaterialAction.Context c,BattleCompanion pet){if(!NativeSourcePlayerFactories.scoped(c))return original;var n=c.npcs.get((long)CombatAccess.field(pet,"objectId"));return n==null?null:NativeSourceNpcFirstLife.first(pet,n,c);}
 static boolean cancellationFrame(){var s=NativeBattleControlledCancellation.scope();return s==null||s.outbound==null||!NativeSourcePlayerFactories.scoped(s.c)||!s.closed&&NativeSourceMaterialFirstLife.proof(s.c,null,s.c.npcs.get(s.id),s.life)&&NativeSourceMaterialFirstLife.receiver(s.outbound.receiver(),s.c);}
 static boolean attached(java.util.List<?> original,Object actor){var s=NativeBattleControlledCancellation.scope();return s==null||!NativeSourcePlayerFactories.scoped(s.c)?original.contains(actor):actor==s.pet&&NativeSourceControlledDisplay.attached(s.owner,s.pet);}
 private NativeSourceIndirectPlayerProof(){}
}
