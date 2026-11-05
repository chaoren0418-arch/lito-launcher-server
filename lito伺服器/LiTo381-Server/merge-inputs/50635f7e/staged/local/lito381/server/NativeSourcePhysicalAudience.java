package local.lito381.server;
import java.util.*;
/** The original natural-NPC Aim supplies the first audience; the later Commit cannot resample it. */
final class NativeSourcePhysicalAudience {
 record Receiver(NativePlayerCommit.Binding binding,Object life,Boolean observer){}
 record Audience(Map<ClientSession,Receiver> receivers){Audience{receivers=Collections.unmodifiableMap(new IdentityHashMap<>(receivers));}}
 static Object first(NativeSourceMonsterHp.Aim aim){
  if(aim==null||!CombatRules.loaded()||!"BATTLE".equals(aim.c().phase)||aim.c().nativeSourceFirstPlayers==null)return null;
  var values=new IdentityHashMap<ClientSession,Receiver>();
  if(aim.c().nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot)for(var m:snapshot.members().values())values.put(m.pc,new Receiver(m.binding,m.life,m.nativeSourceNpcHpObserver));
  return new Audience(values);
 }
 static boolean current(NativeSourcePhysicalNpc.Commit commit,ClientSession pc,NativePlayerCommit.Binding binding){
  Object value=commit.nativeSourcePhysicalAudience;if(value==null)return true;
  if(!(value instanceof Audience a))return false;var r=a.receivers().get(pc);
  return r!=null&&r.binding()!=null&&r.binding().equals(binding)&&r.life()!=null&&r.observer()!=null&&(boolean)CombatAccess.field(pc,"observer")==r.observer()&&NativeSourceControlledDisplay.rawLife(pc)==r.life();
 }
 private NativeSourcePhysicalAudience(){}
}
