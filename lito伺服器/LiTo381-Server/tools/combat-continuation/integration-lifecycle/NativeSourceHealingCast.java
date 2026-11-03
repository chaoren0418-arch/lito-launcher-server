package local.lito381.server;
import java.io.*;import java.util.*;
/** Freeze native healing participants before the original target waits, cost and RNG. */
final class NativeSourceHealingCast {
 static final String P="local/lito381/server/";
 static final class Cast {
  final NativeSourceRawDamage.Member source;final GameHub hub;final NativePlayerLifetime lifetime;final IdentityHashMap<ClientSession,NativeSourceRawDamage.Member> members=new IdentityHashMap<>();volatile boolean closed;boolean registered;
  Cast(NativeSourceRawDamage.Member source,GameHub hub,NativePlayerLifetime lifetime,Map<ClientSession,NativeSourceRawDamage.Member> members){this.source=source;this.hub=hub;this.lifetime=lifetime;this.members.putAll(members);}
  boolean before(){return !closed&&lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")&&source.current(true)&&source.binding.phase().equals("BATTLE")&&CombatAccess.field(source.pc,"hub")==hub&&NativeSourceDamageDisplay.n(source.pc,"playerHp")>0;}
  boolean allowed(ClientSession pc){var member=members.get(pc);return before()&&member!=null&&member.current(true)&&member.binding.room()==source.binding.room()&&CombatAccess.field(pc,"hub")==hub;}
  void close(){closed=true;members.clear();}
 }
 static Cast current(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceHealingCast instanceof Cast cast?cast:null;}
 static boolean scoped(NativeAreaMaterialAction.Context c){return c!=null&&c.nativeSourceHealingCast instanceof Cast;}
 static NativeSourceRawDamage.Member member(ClientSession pc,NativePlayerCommit.Binding binding,NativeAreaMaterialAction.Context c){if(!(c.nativeSourceHealingCast instanceof Cast cast)||cast.closed)return null;var m=cast.members.get(pc);return m!=null&&m.binding.equals(binding)?m:null;}
 static List<ClientSession> targets(List<ClientSession> original){var cast=current();return cast==null?original:original.stream().filter(cast::allowed).toList();}
 static boolean selected(ClientSession pc){var cast=current();return pc==null||cast==null||cast.allowed(pc);}
 static void original(ClientSession pc,Object[] args,int kind){String type=kind==0?"HealingSpell":kind==1?"GroupHealingSpell":"NaturesBlessingSpell";String method=kind==0?"nativeSourceHealingOriginalSingle":kind==1?"nativeSourceHealingOriginalGroup":"nativeSourceHealingOriginalNature";CombatAccess.call(pc,method,"([BL"+P+"BattleSpellCatalog$"+type+";"+(kind==2?"I":"")+")V",args);}
 static void run(ClientSession pc,Object[] args,int kind)throws IOException{
  if(!CombatRules.loaded()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")){original(pc,args,kind);return;}
  var parent=NativeAreaMaterialAction.current.get();if(parent!=null&&parent.pc!=pc)return;var active=current();if(active!=null){if(active.before())original(pc,args,kind);return;}
  var binding=parent==null?NativePlayerCommit.Binding.capture(pc):parent.caster;var source=NativeSourceSignedRecovery.member(pc,binding,parent);if(source==null||!source.current(true))return;
  var c=parent==null?new NativeAreaMaterialAction.Context(pc):parent;if(!c.caster.equals(binding)||!source.current(true))return;var members=new IdentityHashMap<ClientSession,NativeSourceRawDamage.Member>();for(var target:c.players){var b=c.bindings.get(target);if(b!=null){var m=target==pc?source:NativeSourceSignedRecovery.member(target,b,c);if(m!=null)members.put(target,m);}}members.put(pc,source);var cast=new Cast(source,c.hub,NativePlayerLifetime.of(c.hub),members);Object previous=c.nativeSourceHealingCast;c.nativeSourceHealingCast=cast;
  var participants=new ArrayList<>(c.players);if(!participants.contains(pc))participants.add(pc);
  try{owned(participants,cast::before,()->NativeLightAction.commitPair(pc,pc,()->{synchronized(cast.lifetime.admission){if(!cast.before())return;synchronized(c.hub){if(!c.scene())return;}NativeLightAction.finishScoped((published,failure)->cast.close());cast.registered=true;NativeAreaMaterialAction.owned(c,()->original(pc,args,kind));}}));}finally{c.nativeSourceHealingCast=previous;if(!cast.registered)cast.close();}
 }
 static void owned(List<ClientSession> players,java.util.function.BooleanSupplier ready,NativeSelfCastAction.Task body)throws IOException{
  var locks=players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();int held=0;AutoCloseable ticket=null;
  try{for(int attempt=0;attempt<32;attempt++){while(held<locks.size()&&locks.get(held).tryLock())held++;if(held==locks.size())break;while(held>0)locks.get(--held).unlock();if(Thread.currentThread().isInterrupted()||!ready.getAsBoolean())return;java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);}if(held!=locks.size())return;ticket=NativePlayerActionScope.enter(players.toArray(ClientSession[]::new));if(ready.getAsBoolean())body.run();}
  finally{while(held>0)locks.get(--held).unlock();if(ticket!=null){try{for(var pc:players)NativePlayerActionScope.unlocked(pc);}finally{try{ticket.close();}catch(RuntimeException failure){throw failure;}catch(Exception failure){throw new IOException(failure);}}}}
 }
 private NativeSourceHealingCast(){}
}
