package local.lito381.server;
/** Original material primaries retain first participant/actor life across locks and final IO. */
final class NativeSourceMaterialFirstLife {
 private static final Object denied=new Object();
 static NativeSourceRawDamage.Member member(ClientSession pc,NativeAreaMaterialAction.Context c){var binding=pc==c.pc?c.caster:c.bindings.get(pc);return binding==null?null:NativeSourcePlayerFactories.retained(pc,binding,c);}
 static Object playerLife(Object fallback,ClientSession pc,NativeAreaMaterialAction.Context c){if(!NativeSourcePlayerFactories.scoped(c))return fallback;var m=member(pc,c);return m==null?denied:m.life;}
 static Object initial(Object fallback,NativeAreaMaterialAction.Context c,ClientSession player,NativeAreaMaterialAction.Npc n){if(!NativeSourcePlayerFactories.scoped(c))return fallback;return player!=null?playerLife(fallback,player,c):n==null?denied:NativeSourceNpcFirstLife.first(n.actor(),n,c);}
 static boolean player(ClientSession pc,NativeAreaMaterialAction.Context c,boolean position){var m=member(pc,c);return m!=null&&m.current(position)&&NativeSourceControlledDisplay.rawLife(pc)==m.life;}
 static boolean proof(NativeAreaMaterialAction.Context c,ClientSession player,NativeAreaMaterialAction.Npc n,Object life){
  if(!NativeSourcePlayerFactories.scoped(c))return true;
  if(!player(c.pc,c,true))return false;
  if(player!=null)return player(player,c,true)&&playerLife(denied,player,c)==life;
  return n!=null&&n.kind()==3&&n.owner() instanceof ClientSession owner&&player(owner,c,false)&&NativeSourceNpcFirstLife.first(n.actor(),n,c)==life&&NativeSourceControlledDisplay.rawLife(n.actor())==life;
 }
 static boolean valid(NativeSourceNegativeTargets.Aim a){return !NativeSourcePlayerFactories.scoped(a.c)||!a.closed&&proof(a.c,a.player,a.target,a.life);}
 static boolean valid(NativeSourceMassSlowTargets.Aim a){return !NativeSourcePlayerFactories.scoped(a.c)||!a.closed&&proof(a.c,a.player,a.target,a.life);}
 static boolean valid(NativeSourceWindTargets.Aim a){return !NativeSourcePlayerFactories.scoped(a.c)||!a.closed&&proof(a.c,a.player,a.target,a.life);}
 static boolean valid(NativeSourceWeaponBuffTargets.Aim a){return !NativeSourcePlayerFactories.scoped(a.c)||!a.closed&&proof(a.c,a.target,null,a.targetLife)&&playerLife(denied,a.c.pc,a.c)==a.casterLife;}
 static boolean receiver(ClientSession pc,NativeAreaMaterialAction.Context c){var m=member(pc,c);return m!=null&&NativeAcceptedFreezeMutation.roleFrame(m.binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&NativeSourceControlledDisplay.rawLife(pc)==m.life;}
 static boolean negativeFrame(){var a=NativeSourceNegativeTargets.aim();return a==null||a.outbound==null||valid(a)&&(!NativeSourcePlayerFactories.scoped(a.c)||receiver(a.outbound.receiver(),a.c));}
 static boolean slowFrame(){var a=NativeSourceMassSlowTargets.aim();return a==null||a.outbound==null||valid(a)&&(!NativeSourcePlayerFactories.scoped(a.c)||receiver(a.outbound.receiver(),a.c));}
 static boolean windFrame(){var a=NativeSourceWindTargets.aim();return a==null||a.outbound==null||valid(a)&&(!NativeSourcePlayerFactories.scoped(a.c)||receiver(a.outbound.receiver(),a.c));}
 static boolean buffFrame(){var a=NativeSourceWeaponBuffTargets.aim();return a==null||a.outbound==null||valid(a)&&(!NativeSourcePlayerFactories.scoped(a.c)||receiver(a.outbound.receiver(),a.c));}
 static boolean negativeAttached(java.util.List<?> original,Object actor){var a=NativeSourceNegativeTargets.aim();return a==null||!NativeSourcePlayerFactories.scoped(a.c)?original.contains(actor):actor==a.pet&&NativeSourceControlledDisplay.attached(a.owner,a.pet);}
 static boolean slowAttached(java.util.List<?> original,Object actor){var a=NativeSourceMassSlowTargets.aim();return a==null||!NativeSourcePlayerFactories.scoped(a.c)?original.contains(actor):actor==a.pet&&NativeSourceControlledDisplay.attached(a.owner,a.pet);}
 static boolean windAttached(java.util.List<?> original,Object actor){var a=NativeSourceWindTargets.aim();return a==null||!NativeSourcePlayerFactories.scoped(a.c)?original.contains(actor):actor==a.pet&&NativeSourceControlledDisplay.attached(a.owner,a.pet);}
 private NativeSourceMaterialFirstLife(){}
}
