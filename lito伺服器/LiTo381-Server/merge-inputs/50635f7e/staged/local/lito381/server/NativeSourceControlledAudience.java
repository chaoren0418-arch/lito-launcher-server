package local.lito381.server;
import java.util.Arrays;
/** Receiver proof borrows only the already sealed first Context members. */
final class NativeSourceControlledAudience {
 record Proof(Object life,Boolean observer){}
 static Object first(NativeSourceControlledDisplay.Action a,ClientSession pc,NativePlayerCommit.Binding binding){
  if(!CombatRules.loaded()||!"BATTLE".equals(a.c.phase)||a.c.nativeSourceFirstPlayers==null)return null;
  if(!(a.c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot s))return new Proof(null,null);
  var m=s.members().get(pc);if(m==null||binding==null||!binding.equals(m.binding)||m.nativeSourceNpcHpObserver==null)return new Proof(null,null);
  return new Proof(m.life,m.nativeSourceNpcHpObserver);
 }
 static boolean allow(ClientSession pc,byte[] packet){var a=NativeSourceControlledDisplay.action();var out=a==null?null:a.outbound;if(out==null||out.nativeSourceControlledAudience==null)return true;return out.nativeSourceControlledAudience instanceof Proof p&&p.observer()!=null&&(p.life()!=null||p.observer())&&out.pc()==pc&&packet!=null&&Arrays.equals(packet,out.packet())&&(boolean)CombatAccess.field(pc,"observer")==p.observer()&&NativeSourceControlledDisplay.rawLife(pc)==p.life();}
 private NativeSourceControlledAudience(){}
}
