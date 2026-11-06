package local.lito381.server;
import java.util.*;
/** First audience identity travels in existing View/Snapshot/Receiver values; no global state. */
final class NativeSourceDollAudience {
 record Life(GameHub hub,Object life,boolean observer){}
 static final class Audience extends AbstractList<ClientSession> implements RandomAccess {
  final List<NativeSourceDollOutput.Receiver> receivers;
  Audience(List<NativeSourceDollOutput.Receiver> values){receivers=List.copyOf(values);}
  public ClientSession get(int i){return receivers.get(i).pc();}public int size(){return receivers.size();}
 }
 static boolean enabled(ClientSession pc){return CombatRules.loaded()&&"BATTLE".equals(String.valueOf(CombatAccess.field(pc,"phase")));}
 /** Runs at the existing Context first-discovery call while its original Hub monitor is held. */
 static NativePlayerCommit.Binding capture(ClientSession pc,NativeAreaMaterialAction.Context c){
  boolean observer=(boolean)CombatAccess.field(pc,"observer");var binding=NativeSourceFirstPlayers.capture(pc,c);
  if(CombatRules.loaded()&&"BATTLE".equals(c.phase)&&c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Builder b){var m=b.members.get(pc);if(m!=null&&m.nativeSourceNpcHpObserver==null)b.members.put(pc,new NativeSourceRawDamage.Member(pc,m.binding,m.life,m.x,m.y,observer));}
  return binding;
 }
 static NativeSourceDollOutput.Receiver receiver(ClientSession pc,NativePlayerCommit.Binding binding,Object life,boolean observer,GameHub hub){return new NativeSourceDollOutput.Receiver(pc,binding,new Life(hub,life,observer));}
 /** Keeps the original roomMembers lock through observer/life-before-binding capture, without another lock domain. */
 static List<ClientSession> members(GameHub hub,long room,ClientSession caster){
  if(!enabled(caster))return hub.roomMembers(room);
  synchronized(hub){var original=hub.roomMembers(room);var values=new ArrayList<NativeSourceDollOutput.Receiver>();for(var pc:original){var m=NativeSourceNpcHpOutput.ingress(pc);values.add(receiver(pc,m.binding,m.life,Boolean.TRUE.equals(m.nativeSourceNpcHpObserver),hub));}return new Audience(values);}
 }
 static Object acceptedLife(ClientSession pc,NativePlayerCommit.Binding binding,Object original,boolean observer){
  if(observer)return original;var f=NativePlayerCommit.frame();
  if(f==null||!f.accepted||f.pc!=pc||f.event==null||f.event.world()||!Objects.equals(f.event.target(),binding)||!(f.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof p)||p.target()==null)return original;
  var t=p.target();var plan=f.event.sourceDisplay();if(t.pc()!=pc||t.life()==null||!Objects.equals(t.binding(),binding)||plan==null)return original;
  var a=plan.target;var g=plan.nativeRawGuard;boolean same=a!=null&&a.pc()==pc&&Objects.equals(a.binding(),binding)&&a.life()==original;
  if(!same&&g!=null){var m=g.target();same=m!=null&&m.pc==pc&&Objects.equals(m.binding,binding)&&m.life==original;}
  return same?t.life():original;
 }
 /** A Context with published first members never falls through to a later life/binding capture. */
 static List<ClientSession> copy(Collection<ClientSession> original,NativeAreaMaterialAction.Context c,ClientSession caster){
  if(original instanceof Audience a)return a;
  var list=List.copyOf(original);if(!enabled(caster)||c==null||c.pc!=caster||c.nativeSourceFirstPlayers==null)return list;
  var values=new ArrayList<NativeSourceDollOutput.Receiver>();GameHub hub=(GameHub)CombatAccess.field(caster,"hub");
  for(var pc:list){var m=c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot s?s.members().get(pc):null;
   if(m==null||m.nativeSourceNpcHpObserver==null){values.add(receiver(pc,m==null?null:m.binding,null,false,hub));continue;}
   boolean observer=m.nativeSourceNpcHpObserver;values.add(receiver(pc,m.binding,acceptedLife(pc,m.binding,m.life,observer),observer,hub));
  }return new Audience(values);
 }
 /** The later Context construction/binding reads cannot replace an earlier selected receiver. */
 static NativeSourceDollOutput.Receiver select(ClientSession pc,NativePlayerCommit.Binding later,NativeSourceDollSkills.View view){
  if(view.viewers instanceof Audience a){for(var r:a.receivers)if(r.pc()==pc)return r;throw new IllegalStateException("doll original audience member missing");}
  return new NativeSourceDollOutput.Receiver(pc,later);
 }
 static boolean current(NativeSourceDollOutput.Output out){
  var r=out.receiver();Object value=r.nativeSourceDollAudienceProof;if(value==null)return true;
  return value instanceof Life p&&p.life()!=null&&r.binding()!=null&&out.proof().hub()==p.hub()&&CombatAccess.field(r.pc(),"hub")==p.hub()&&NativeSourceControlledDisplay.rawLife(r.pc())==p.life()&&(boolean)CombatAccess.field(r.pc(),"observer")==p.observer();
 }
 private NativeSourceDollAudience(){}
}
