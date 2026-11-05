package local.lito381.server;
/** One existing completion receipt is reserved before HP mutation, under the real coordinator fence. */
final class NativeHpCompletionAdmission {
 static NativePlayerVitalCompletions.Receipt reserve(ClientSession pc,NativePlayerCommit.Queue queue){
  if(!queue.vitalLock.isHeldByCurrentThread())throw new IllegalStateException("HP admission requires original actor lock");
  synchronized(queue.life.admission){if(queue.closed||queue.retired||!queue.life.processes())return null;return NativePlayerVitalCompletions.reserve(pc);}
 }
 static void unlock(java.util.concurrent.locks.ReentrantLock lock,NativePlayerVitalCompletions.Receipt receipt){try{if(receipt!=null&&!receipt.released.get())receipt.close();}finally{lock.unlock();}}
 private NativeHpCompletionAdmission(){}
}
