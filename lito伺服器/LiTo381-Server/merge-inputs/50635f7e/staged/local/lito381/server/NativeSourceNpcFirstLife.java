package local.lito381.server;
/** Initial target metadata borrows original NPC discovery; accepted death seals remain original. */
final class NativeSourceNpcFirstLife {
 private static final Object denied=new Object();
 static boolean original(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){return n!=null&&actor==n.actor()&&c.npcs.get(n.id())==n;}
 static Object first(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){
  if(!original(actor,n,c))return denied;
  if(n.kind()==1||n.kind()==2)return n.epoch()==null?denied:n.epoch();
  if(n.kind()==3&&n.owner() instanceof ClientSession owner&&n.epoch() instanceof NativeCombatAttacker.Ref ref&&ref.kind()==2&&ref.id()==n.id()&&ref.owner()!=null&&ref.master()!=null&&ref.master().equals(c.bindings.get(owner)))return ref.owner();
  return denied;
 }
 static Object controlled(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){return NativeSourcePlayerFactories.scoped(c)?first(actor,n,c):NativeSourceControlledDisplay.token(actor);}
 static Object monster(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){return NativeSourcePlayerFactories.scoped(c)?first(actor,n,c):NativeSourceMonsterHp.life(actor);}
 static Object material(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){return n!=null&&n.kind()!=0&&NativeSourcePlayerFactories.scoped(c)?first(actor,n,c):NativeSourceRemainingMaterials.token(actor);}
 static Object owner(Object actor,NativeAreaMaterialAction.Npc n,NativeAreaMaterialAction.Context c){
  if(!NativeSourcePlayerFactories.scoped(c))return NativeSourceControlledDisplay.token(actor);
  if(n==null||n.kind()!=3||n.owner()!=actor||!original(n.actor(),n,c)||first(n.actor(),n,c)==denied||!(actor instanceof ClientSession pc))return denied;
  var binding=c.bindings.get(pc);return binding==null?denied:NativeSourcePlayerFactories.retained(pc,binding,c).acceptedLife;
 }
 private NativeSourceNpcFirstLife(){}
}
