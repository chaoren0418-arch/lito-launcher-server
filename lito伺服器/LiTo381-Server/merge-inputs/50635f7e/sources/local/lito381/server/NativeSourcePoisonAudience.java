package local.lito381.server;
import java.util.*;
/** Reuses the first immutable receiver value without retaining the temporary full audience. */
final class NativeSourcePoisonAudience {
 static List<ClientSession> members(GameHub hub,long room){return NativeSourceElementAudience.members(hub,room);}
 static Object first(List<ClientSession> members,ClientSession pc){if(!(members instanceof NativeSourceElementAudience.Audience a))return null;Object r=a.first.get(pc);return r==null?Boolean.FALSE:r;}
 static boolean value(Object first,ClientSession pc,NativePlayerCommit.Binding binding){
  if(first==null)return true;if(!(first instanceof NativeSourceElementAudience.Receiver r))return false;
  Object status=r.status().get();return status!=null&&status==CombatAccess.field(pc,"timedStatus")&&r.binding()!=null&&r.binding().equals(binding)&&(boolean)CombatAccess.field(pc,"observer")==r.observer()&&NativeSourceControlledDisplay.rawLife(pc)==r.life();
 }
 static boolean current(NativeSourceArenaPoison.Proof proof,ClientSession pc,NativePlayerCommit.Binding binding){boolean marked=false;for(var v:proof.viewers()){if(v.pc().get()==pc)return value(v.nativeSourcePoisonAudience,pc,binding);if(v.nativeSourcePoisonAudience!=null)marked=true;}return !marked;}
 static boolean current(NativeSourceArenaPoison.Output out){return current(out.proof(),out.receiver(),out.binding());}
 static boolean current(NativeSourcePoisonNpc.Status out){return current(out.proof(),out.receiver(),out.binding());}
 private NativeSourcePoisonAudience(){}
}
