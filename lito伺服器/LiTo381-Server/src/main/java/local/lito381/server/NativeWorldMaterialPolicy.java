// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/pet-world-policy/NativeWorldMaterialPolicy.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
/** Prepare cold role policy outside shared WORLD monitors; surface failure only at original material admission. */
final class NativeWorldMaterialPolicy {
 private record Prepared(ClientSession pc,NativePlayerCommit.Binding binding,Object owner,boolean gm,IOException failure){}
 private static final ThreadLocal<Prepared> current=new ThreadLocal<>();
 static void run(ClientSession pc,boolean hasActor,int skill,NativeSelfCastAction.Task body)throws IOException{
  if(!hasActor||CombatDatabase.value(0,skill,"NativeWorldMaterialPolicy","itemConsumeId")==0){body.run();return;}
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("world material policy requires owned player lock");
  var actor=NativeReuseGate.actor(pc);var binding=NativePlayerCommit.Binding.capture(pc);Object owner=actor==null?null:actor.owner;
  boolean gm=false;IOException failed=null;
  try{gm=NativeCastMaterialPolicy.nativeOriginalGm(pc);}catch(IOException failure){failed=failure;}
  var previous=current.get();current.set(new Prepared(pc,binding,owner,gm,failed));
  try{body.run();}finally{if(previous==null)current.remove();else current.set(previous);}
 }
 static boolean gm(ClientSession pc)throws IOException{
  var prepared=current.get();if(prepared==null){current.remove();return NativeCastMaterialPolicy.nativeOriginalGm(pc);}
  if(prepared.pc!=pc)return NativeCastMaterialPolicy.nativeOriginalGm(pc);
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("prepared policy lost player lock");
  var actor=NativeReuseGate.actor(pc);
  if(!prepared.binding.matches(pc)||actor==null||actor.owner!=prepared.owner)throw new IOException("prepared material character policy role changed");
  if(prepared.failure!=null)throw prepared.failure;return prepared.gm;
 }
 private NativeWorldMaterialPolicy(){}
}
