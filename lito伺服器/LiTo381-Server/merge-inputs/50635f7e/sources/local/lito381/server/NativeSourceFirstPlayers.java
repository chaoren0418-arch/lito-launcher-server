package local.lito381.server;
import java.util.*;
/** Freeze player life at the original Hub discovery, before binding capture and later target waits. */
final class NativeSourceFirstPlayers {
 static final class Builder{final IdentityHashMap<ClientSession,NativeSourceRawDamage.Member> members=new IdentityHashMap<>();}
 record Snapshot(Map<ClientSession,NativeSourceRawDamage.Member> members){Snapshot{members=Collections.unmodifiableMap(new IdentityHashMap<>(members));}}
 static NativePlayerCommit.Binding capture(ClientSession pc,NativeAreaMaterialAction.Context c){
  if(!CombatRules.loaded()||!c.phase.equals("BATTLE"))return NativePlayerCommit.Binding.capture(pc);
  if(!Thread.holdsLock(c.hub))throw new IllegalStateException("first player discovery lost original Hub ownership");
  // Read life BEFORE the existing binding capture. A death/revival between them cannot be adopted.
  Object life=NativeSourceControlledDisplay.token(pc);var binding=NativePlayerCommit.Binding.capture(pc);remember(c,pc,binding,life);return binding;
 }
 static void remember(NativeAreaMaterialAction.Context c,ClientSession pc,NativePlayerCommit.Binding binding,Object life){
  Builder builder;if(c.nativeSourceFirstPlayers==null){builder=new Builder();c.nativeSourceFirstPlayers=builder;}else if(c.nativeSourceFirstPlayers instanceof Builder b)builder=b;else throw new IllegalStateException("first player snapshot already sealed");
  if(!builder.members.containsKey(pc))builder.members.put(pc,new NativeSourceRawDamage.Member(pc,binding,life));
 }
 static void seal(NativeAreaMaterialAction.Context c){if(c.nativeSourceFirstPlayers instanceof Builder b)c.nativeSourceFirstPlayers=new Snapshot(b.members);else if(c.nativeSourceFirstPlayers==null&&CombatRules.loaded()&&c.phase.equals("BATTLE"))c.nativeSourceFirstPlayers=new Snapshot(Map.of());}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){child.nativeSourceFirstPlayers=parent.nativeSourceFirstPlayers instanceof Snapshot s?s:parent.nativeSourceFirstPlayers instanceof Builder?new Snapshot(Map.of()):null;}
 static NativeSourceRawDamage.Member member(ClientSession pc,NativePlayerCommit.Binding binding,NativeAreaMaterialAction.Context c){
  if(c!=null&&c.nativeSourceFirstPlayers instanceof Snapshot s){var m=s.members().get(pc);return m!=null&&m.binding.equals(binding)?m:null;}
  if(c!=null&&c.nativeSourceFirstPlayers instanceof Builder)return null;
  return new NativeSourceRawDamage.Member(pc,binding);
 }
 private NativeSourceFirstPlayers(){}
}
