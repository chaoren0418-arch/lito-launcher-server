package local.lito381.server;
/** Existing Source scopes/locks only. A nested contended request is rejected before cast admission. */
final class NativePairedCastSafety {
 static void run(ClientSession caster,ClientSession target,Runnable action){
  try(var scope=NativePlayerActionScope.enter(caster,target)){
   var a=caster.nativePlayerActionLock();var b=target.nativePlayerActionLock();ClientSession acquired=null;
   if(a.isHeldByCurrentThread()&&!b.isHeldByCurrentThread()){if(!b.tryLock())return;acquired=target;}
   else if(b.isHeldByCurrentThread()&&!a.isHeldByCurrentThread()){if(!a.tryLock())return;acquired=caster;}
   // Both locks are already owned for nested calls. Avoid even an identity-hash tie monitor wait.
   try{if(a.isHeldByCurrentThread()&&b.isHeldByCurrentThread())action.run();else NativeSourcePlayerActionLock.runPairOrdered(caster,target,action);}
   finally{if(acquired!=null){acquired.nativePlayerActionLock().unlock();NativePlayerActionScope.unlocked(acquired);}}
  }
 }
 static void transfer(ClientSession caster,ClientSession target,NativePlayerVitalCompletions.Receipt receipt){
  if(receipt==null)return;if(!caster.nativePlayerActionLock().isHeldByCurrentThread()||!target.nativePlayerActionLock().isHeldByCurrentThread()||receipt.pc!=target||receipt.transferred||receipt.released.get())throw new IllegalStateException("pair receipt requires both actual locks");receipt.transferred=true;
 }
 static void abandon(NativePlayerVitalCompletions.Receipt receipt){if(receipt!=null)receipt.run(()->{});}
 private NativePairedCastSafety(){}
}
