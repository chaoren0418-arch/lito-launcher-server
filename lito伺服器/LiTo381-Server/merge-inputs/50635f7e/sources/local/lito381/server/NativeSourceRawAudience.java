package local.lito381.server;
import java.util.Arrays;
/** Raw packets borrow the existing first audience; no later actor/token capture. */
final class NativeSourceRawAudience {
 record Proof(Object life,boolean observer){}
 static Object first(ClientSession pc,NativePlayerCommit.Binding binding){
  var a=NativeSourceRawDamage.current();
  if(a==null||!CombatRules.loaded()||!"BATTLE".equals(a.c.phase)||a.c.nativeSourceFirstPlayers==null)return null;
  if(!(a.c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot s))return new Proof(null,false);
  var m=s.members().get(pc);
  if(m==null||binding==null||!binding.equals(m.binding)||m.nativeSourceNpcHpObserver==null)return new Proof(null,false);
  boolean observer=m.nativeSourceNpcHpObserver;
  return new Proof(NativeSourceDollAudience.acceptedLife(pc,binding,m.life,observer),observer);
 }
 static NativeSourceRawDamage.Output output(ClientSession pc,NativePlayerCommit.Binding binding,byte[] bytes,String note){return new NativeSourceRawDamage.Output(pc,binding,bytes,note,first(pc,binding));}
 static boolean allow(ClientSession pc,byte[] bytes){
  var a=NativeSourceRawDamage.current();var out=a==null?null:a.outbound;
  if(out==null||out.nativeSourceRawAudience==null)return true;
  return out.nativeSourceRawAudience instanceof Proof p&&p.life()!=null&&out.pc()==pc&&bytes!=null&&Arrays.equals(bytes,out.packet())&&(boolean)CombatAccess.field(pc,"observer")==p.observer()&&NativeSourceControlledDisplay.rawLife(pc)==p.life();
 }
 private NativeSourceRawAudience(){}
}
