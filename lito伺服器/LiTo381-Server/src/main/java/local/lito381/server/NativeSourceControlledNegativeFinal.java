package local.lito381.server;
import java.io.IOException;import java.util.*;
/** Controlled negative outputs carry first values through the existing complete sender siblings. */
final class NativeSourceControlledNegativeFinal {
 static final class Scope {
  final NativeSourceSelfFinalFrame.First first;final IdentityHashMap<ClientSession,Object> receiverTimed=new IdentityHashMap<>();
  Scope(NativeSourceSelfFinalFrame.First first){this.first=first;}
 }
 record Value(NativeSourceWeaponFinalFrame.Output output,NativeSourceSelfFinalFrame.First first,Object receiverTimed){}
 static void install(NativeAreaMaterialAction.Context c){
  var p=NativeSourceTargetNegativeLife.proof(c);if(p==null||!"BATTLE".equals(c.phase)||p.npc()==null||p.npc().kind()!=3)return;
  var first=NativeSourceMassTeleportExit.capture(c.pc,c.caster).first();
  if(first!=null&&(first.lifetime()!=p.life()||!first.source().binding().equals(p.source().binding)))first=null;
  var scope=new Scope(first);
  if(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot)for(var entry:snapshot.members().entrySet()){
   var pc=entry.getKey();var member=entry.getValue();Object timed=CombatAccess.field(pc,"timedStatus");
   if(member.binding.equals(c.bindings.get(pc))&&timed!=null&&NativeSourceSelfFinalFrame.life(timed)==member.acceptedLife)scope.receiverTimed.put(pc,timed);
  }
  c.nativeSourceControlledNegativeFinal=scope;
 }
 static void close(NativeAreaMaterialAction.Context c){if(c==null)return;Object value=c.nativeSourceControlledNegativeFinal;if(value instanceof Scope scope)scope.receiverTimed.clear();c.nativeSourceControlledNegativeFinal=null;}
 static void produced(NativeAreaMaterialAction.Context c){if(c==null)return;Object value=c.nativeSourceControlledNegativeFinal;if(value instanceof Scope scope){scope.receiverTimed.clear();c.nativeSourceControlledNegativeFinal=Boolean.FALSE;}}
 static boolean produce(ClientSession pc,byte[] packet,BattleSpellCatalog.TargetNegativeStatusSpell spell,int fixture,CombatNpcDebuffs.Focus focus,boolean administrator)throws IOException {
  var c=NativeAreaMaterialAction.current.get();try{return CombatNpcDebuffs.nativeMaterialControlledNegativeBody(pc,packet,spell,fixture,focus,administrator);}finally{produced(c);}
 }
 static NativeSourceWeaponFinalFrame.Participant identity(NativeSourceWeaponFinalFrame.Participant p){return new NativeSourceWeaponFinalFrame.Participant(p.pc(),p.binding(),p.timed(),p.life(),p.x(),p.y(),p.observer(),false);}
 static boolean current(Value value){return value!=null&&value.receiverTimed()!=null&&NativeSourceSelfFinalFrame.current(value.first())&&CombatAccess.field(value.output().proof().receiver().pc(),"timedStatus")==value.receiverTimed()&&NativeSourceWeaponFinalFrame.current(value.output());}
 static boolean capture(ClientSession receiver,byte[] bytes,String note)throws IOException {
  var c=NativeAreaMaterialAction.current.get();if(c==null||NativeSourceWeaponTargetLife.marked(c)||(int)CombatAccess.field(c,"nativeWeaponBreakTarget")!=2)return false;
  if(c.nativeSourceControlledNegativeFinal==Boolean.FALSE)return true;
  if(!(c.nativeSourceControlledNegativeFinal instanceof Scope scope))return false;
  var p=NativeSourceTargetNegativeLife.proof(c);if(p==null||p.npc()==null||p.npc().kind()!=3)return false;
  var original=NativeSourceWeaponFinalFrame.output(c,receiver,bytes,note);
  if(original!=null){var proof=original.proof();var output=new NativeSourceWeaponFinalFrame.Output(new NativeSourceWeaponFinalFrame.Proof(identity(proof.source()),identity(proof.target()),proof.npc(),proof.receiver(),proof.lifetime(),proof.hub()),bytes,note);var value=new Value(output,scope.first,scope.receiverTimed.get(receiver));
   if(current(value)&&!NativeLightAction.nativeSourceNpcHpDefer(()->send(value)))send(value);
  }
  return true;
 }
 static void send(Value value)throws IOException{if(current(value)){var out=value.output();out.proof().receiver().pc().nativeSourceWeaponSelfSend(out.packet(),out.note(),value);}}
 static boolean kind(Object value){return value instanceof Value;}
 static boolean frame(ClientSession receiver,byte[] bytes,String note,Object value)throws IOException {
  if(!(value instanceof Value v))return false;
  if(v.output().proof().receiver().pc()==receiver){var actual=new Value(new NativeSourceWeaponFinalFrame.Output(v.output().proof(),bytes,note),v.first(),v.receiverTimed());receiver.nativeSourceWeaponStaticFrame(actual.output().packet(),actual.output().note(),actual);}return true;
 }
 static boolean allow(ClientSession receiver,byte[] bytes,Object value){if(!Thread.holdsLock(receiver))throw new IllegalStateException("controlled negative final check requires original receiver monitor");return value instanceof Value v&&v.output().proof().receiver().pc()==receiver&&Arrays.equals(bytes,v.output().packet())&&current(v);}
 private NativeSourceControlledNegativeFinal(){}
}
