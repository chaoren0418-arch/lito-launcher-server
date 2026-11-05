package local.lito381.server;
/** Original binding-match status lock, without creating a status container or a role token. */
final class NativeSourceControlledRoleFrame {
 static boolean current(NativePlayerCommit.Binding binding,ClientSession pc){
  if(binding==null||binding.owner()==null||pc==null)return false;
  Object status=CombatAccess.field(pc,"timedStatus");if(status==null)return false;
  synchronized(status){return CombatAccess.field(pc,"timedStatus")==status&&NativeAcceptedFreezeMutation.roleFrame(binding,pc);}
 }
 private NativeSourceControlledRoleFrame(){}
}
