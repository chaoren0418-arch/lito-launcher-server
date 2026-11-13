package local.lito381.server;
import java.util.*;import java.lang.ref.WeakReference;
/** Original NPC roster ownership and a weak, immutable master proof for new companion ingress. */
final class NativeSourceNpcOrigins {
 private static final Object denied=new Object();
 record Master(WeakReference<ClientSession> pc,NativePlayerCommit.Binding binding,Object life,int x,int y){}
 static NativeSourceNpcPlayer.Origin origin(ClientSession pc,NativeSourceRawDamage.Member original,GameHub hub,long room){if(!NativeSourceDollCommit.enabled(pc))return NativeSourceNpcPlayer.nativeSourceNpcOriginOriginalCapture(pc,original,hub,room);synchronized(hub){return NativeSourceNpcPlayer.nativeSourceNpcOriginFrozenCapture(pc,original,hub,room);}}
 static NativePlayerCommit.Binding binding(ClientSession pc,Map<ClientSession,NativeSourceRawDamage.Member> members,ClientSession primary,NativeSourceRawDamage.Member original,GameHub hub){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("NPC discovery lost original Hub ownership");var kept=pc==primary?original:members.get(pc);if(kept!=null){var binding=NativePlayerCommit.Binding.capture(pc);members.putIfAbsent(pc,kept);return binding;}
  Object life=NativeSourceControlledDisplay.token(pc);int x=NativeSourceRawDamage.number(pc,"positionX"),y=NativeSourceRawDamage.number(pc,"positionY");var binding=NativePlayerCommit.Binding.capture(pc);members.put(pc,new NativeSourceRawDamage.Member(pc,binding,life,x,y));return binding;
 }
 static NativeSourceRawDamage.Member member(ClientSession pc,NativePlayerCommit.Binding binding,Map<ClientSession,NativeSourceRawDamage.Member> members){var m=members.get(pc);return m!=null&&m.binding.equals(binding)?m:new NativeSourceRawDamage.Member(pc,binding,denied,0,0);}
 static NativeCombatAttacker.Ref ref(ClientSession pc,BattleCompanion pet){if(!NativeSourceDollCommit.enabled(pc))return NativeCompanionRefScene.nativeSourceNpcOriginOriginalCapture(pc,pet);var original=NativeSourceAcceptedPlayers.ingress(pc);return stamp(NativeCompanionRefScene.nativeSourceNpcOriginOriginalCapture(pc,pet),original);}
 static NativeCombatAttacker.Ref stamp(NativeCombatAttacker.Ref ref,NativeSourceRawDamage.Member original){if(ref==null)return null;if(original==null||!original.binding.phase().equals("BATTLE"))return ref;if(ref.kind()!=2||ref.master()==null||!ref.master().equals(original.binding)||!original.current(false))return null;var proof=new Master(new WeakReference<>(original.pc),original.binding,original.life,original.x,original.y);return new NativeCombatAttacker.Ref(ref.kind(),ref.id(),ref.owner(),ref.epoch(),ref.master(),ref.room(),ref.map(),ref.instance(),proof);}
 static NativeSourceRawDamage.Member master(ClientSession pc,NativePlayerCommit.Binding binding,NativeCombatAttacker.Ref ref){if(ref!=null&&ref.nativeSourceNpcMasterProof instanceof Master proof){if(proof.pc().get()!=pc||!proof.binding().equals(binding)||!proof.binding().equals(ref.master()))return new NativeSourceRawDamage.Member(pc,binding,denied,proof.x(),proof.y());return new NativeSourceRawDamage.Member(pc,binding,proof.life(),proof.x(),proof.y());}return new NativeSourceRawDamage.Member(pc,binding);}
 private NativeSourceNpcOrigins(){}
}
