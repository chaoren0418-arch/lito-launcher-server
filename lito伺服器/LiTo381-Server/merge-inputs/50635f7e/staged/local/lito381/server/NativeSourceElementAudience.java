package local.lito381.server;
import java.lang.ref.WeakReference;import java.util.*;
/** First cube/poison HP audience, captured in the original Hub/status lock domains. */
final class NativeSourceElementAudience {
 record Receiver(NativePlayerCommit.Binding binding,Object life,boolean observer,WeakReference<Object> status){}
 static final class Audience extends AbstractList<ClientSession> implements RandomAccess {
  final List<ClientSession> members;final Map<ClientSession,Receiver> first;
  Audience(Collection<ClientSession> members,Map<ClientSession,Receiver> first){this.members=List.copyOf(members);this.first=Collections.unmodifiableMap(new IdentityHashMap<>(first));}
  public int size(){return members.size();}public ClientSession get(int i){return members.get(i);}
 }
 static Receiver capture(ClientSession pc){var status=CombatPoisonAtomic.status(pc);synchronized(status){boolean observer=(boolean)CombatAccess.field(pc,"observer");Object life=observer?NativeSourceControlledDisplay.rawLife(pc):NativeSourceControlledDisplay.token(pc);return new Receiver(NativePlayerCommit.Binding.capture(pc),life,observer,new WeakReference<>(status));}}
 static Audience freeze(Collection<ClientSession> original,Audience prior){var values=new IdentityHashMap<ClientSession,Receiver>();if(prior!=null)values.putAll(prior.first);for(var pc:original)if(!values.containsKey(pc))values.put(pc,capture(pc));return new Audience(original,values);}
 static List<ClientSession> members(GameHub hub,long room){synchronized(hub){var original=hub.roomMembers(room);return CombatRules.loaded()?freeze(original,null):original;}}
 static List<ClientSession> recipients(GameHub hub,long room,int force,List<ClientSession> members){synchronized(hub){var original=hub.battleRecipients(room,force);return members instanceof Audience a?freeze(original,a):original;}}
 static List<ClientSession> copy(Collection<ClientSession> original){return original instanceof Audience a?a:List.copyOf(original);}
 static boolean current(List<ClientSession> members,List<ClientSession> recipients,ClientSession pc,NativePlayerCommit.Binding binding){
  if(!(members instanceof Audience)&&!(recipients instanceof Audience))return true;
  Receiver r=members instanceof Audience a?a.first.get(pc):null;if(r==null&&recipients instanceof Audience a)r=a.first.get(pc);
  return r!=null&&r.status().get()!=null&&r.binding()!=null&&r.binding().equals(binding)&&r.status().get()==CombatAccess.field(pc,"timedStatus")&&(boolean)CombatAccess.field(pc,"observer")==r.observer()&&NativeSourceControlledDisplay.rawLife(pc)==r.life();
 }
 static boolean current(NativeSourceCubeNpc.Commit p,ClientSession pc,NativePlayerCommit.Binding binding){return current(p.members,p.recipients,pc,binding);}
 static boolean current(NativeSourcePoisonNpc.Commit p,ClientSession pc,NativePlayerCommit.Binding binding){return current(p.members,p.recipients,pc,binding);}
 private NativeSourceElementAudience(){}
}
