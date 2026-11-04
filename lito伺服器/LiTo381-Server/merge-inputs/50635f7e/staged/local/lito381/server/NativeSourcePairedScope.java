package local.lito381.server;
import java.util.concurrent.locks.ReentrantLock;
/** Reject nested Source pair contention before the original scope can register its peer. */
final class NativeSourcePairedScope {
 static boolean enabled(ClientSession caster){return caster!=null&&CombatRules.loaded()&&"BATTLE".equals(String.valueOf(CombatAccess.field(caster,"phase")));}
 static void nativeSourcePairThirdOriginalRun(ClientSession caster,ClientSession target,Runnable action){
  if(!enabled(caster)||target==null){NativePairedCastSafety.nativeSourcePairedScopeOriginalRun(caster,target,action);return;}
  var a=caster.nativePlayerActionLock();var b=target.nativePlayerActionLock();
  ReentrantLock acquired=null;ClientSession owner=null;
  if(a.isHeldByCurrentThread()&&!b.isHeldByCurrentThread()){if(!b.tryLock())return;acquired=b;owner=target;}
  else if(b.isHeldByCurrentThread()&&!a.isHeldByCurrentThread()){if(!a.tryLock())return;acquired=a;owner=caster;}
  try{NativePairedCastSafety.nativeSourcePairedScopeOriginalRun(caster,target,action);}
  finally{if(acquired!=null){acquired.unlock();NativePlayerActionScope.unlocked(owner);}}
 }
 static void run(ClientSession caster,ClientSession target,Runnable action){NativeSourcePairThird.run(caster,target,action,0);}
 private NativeSourcePairedScope(){}
}
