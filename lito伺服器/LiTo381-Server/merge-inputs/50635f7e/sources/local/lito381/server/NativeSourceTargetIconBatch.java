package local.lito381.server;
import java.io.IOException;import java.io.UncheckedIOException;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Per-original-Batch target presentation tasks; no global output lane or renewed acceptance. */
final class NativeSourceTargetIconBatch {
 static final class Work {
  final Object output;final NativeSelfCastAction.Task task;final Runnable publication;final AtomicBoolean claimed=new AtomicBoolean();
  Work(Object output,NativeSelfCastAction.Task task,NativeSourceProc68Icon.Proof proof){this.output=output;this.task=task;publication=NativeRolePublication.nativeSourceTargetIconCompletion(proof.target().pc(),()->{try{task.run();}catch(IOException e){throw new UncheckedIOException(e);}});}
  void run()throws IOException{if(claimed.compareAndSet(false,true))try{publication.run();}catch(UncheckedIOException e){throw e.getCause();}}
 }
 static NativeDeItemApplication.Owner existing(ClientSession pc){Object owner=CombatAccess.field(pc,"sourceItemOwner");return owner instanceof NativeDeItemApplication.Owner value?value:null;}
 @SuppressWarnings("unchecked") static List<Work> works(Object batch){var value=CombatAccess.field(batch,"nativeSourceTargetIconOutputs");return value instanceof List<?> list?(List<Work>)list:List.of();}
 static void defer(NativeSelfCastAction.Task task,NativeSourceProc68Icon.Output icon){
  // Original admission, action-lock check and original output wrapper stay authoritative.
  NativeLightAction.deferScoped(task);
  Object batch=NativeLightAction.nativeSourceGroundPlacementBatch();
  var outputs=(List<?>)CombatAccess.field(batch,"outputs");Object output=outputs.get(outputs.size()-1);
  if(CombatAccess.field(batch,"nativeSourceTargetIconOutputs")==null)CombatAccess.set(batch,"nativeSourceTargetIconOutputs",new ArrayList<Work>());
  works(batch).add(new Work(output,task,icon.proof()));
 }
 static boolean independent(Object output,Object batch)throws IOException{
  for(var work:works(batch))if(work.output==output){work.run();return true;}
  return NativeSourceCubeVisibility.independent(output);
 }
 static void rescue(Object batch)throws IOException{NativeSourceCubeVisibility.rescue(batch);for(var work:works(batch))work.run();}
 static void finish(Object batch,Throwable primary)throws Throwable{
  try{NativeSourceBatchTerminal.finish(batch,primary);}finally{Object value=CombatAccess.field(batch,"nativeSourceTargetIconOutputs");if(value instanceof List<?> list)list.clear();CombatAccess.set(batch,"nativeSourceTargetIconOutputs",null);}
 }
 private NativeSourceTargetIconBatch(){}
}
