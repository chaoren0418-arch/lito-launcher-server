package local.lito381.server;
import java.util.Map;
/** A synchronous read of the existing parent scope; new-action pairs never wait while holding its third actor. */
final class NativeSourcePairThird {
 static boolean other(Object root,ClientSession first,ClientSession second){if(root==null)return false;Object fence=CombatAccess.field(root,"fence");synchronized(fence){if((boolean)CombatAccess.field(root,"closed")||CombatAccess.field(root,"owner")!=Thread.currentThread())return false;for(Object value:((Map<?,?>)CombatAccess.field(root,"members")).keySet()){var pc=(ClientSession)value;if(pc!=first&&pc!=second&&pc.nativePlayerActionLock().isHeldByCurrentThread())return true;}return false;}}
 static boolean registered(Object root,ClientSession pc){synchronized(CombatAccess.field(root,"fence")){return !(boolean)CombatAccess.field(root,"closed")&&((Map<?,?>)CombatAccess.field(root,"members")).containsKey(pc);}}
 static void original(ClientSession first,ClientSession second,Runnable action,int route){switch(route){case 0->NativeSourcePairedScope.nativeSourcePairThirdOriginalRun(first,second,action);case 1->NativeSourceOuterPair.nativeSourcePairThirdOriginalRun(first,second,action);case 2->NativeSourcePairAdmission.nativeSourcePairThirdOriginalAdmit(first,second,action);default->throw new IllegalArgumentException("unreviewed third-actor pair route");}}
 static void run(ClientSession first,ClientSession second,Runnable action,int route){
  if(action==null||first==null||second==null||!NativeSourcePairedScope.enabled(first)){original(first,second,action,route);return;}
  if(route!=2){var frame=NativePlayerCommit.frame();if(frame!=null&&frame.accepted){original(first,second,action,route);return;}}
  Object parent=NativePlayerActionScope.nativeSourcePairScopeRoot();
  if(!other(parent,first,second)){original(first,second,action,route);return;}
  var a=first.nativePlayerActionLock();var b=second.nativePlayerActionLock();boolean acquiredA=false,acquiredB=false,entered=false;
  if(!a.isHeldByCurrentThread()){if(!a.tryLock())return;acquiredA=true;}
  try{if(b!=a&&!b.isHeldByCurrentThread()){if(!b.tryLock())return;acquiredB=true;}entered=true;original(first,second,action,route);}
  finally{if(acquiredB)b.unlock();if(acquiredA)a.unlock();
   // Rejected partial acquisition has registered no peer. Keep original release ordering only for actual members.
   if(entered&&(route==0||!a.isHeldByCurrentThread()&&!b.isHeldByCurrentThread())&&NativePlayerActionScope.nativeSourcePairScopeRoot()==parent){if(acquiredA&&registered(parent,first))NativePlayerActionScope.unlocked(first);if(acquiredB&&registered(parent,second))NativePlayerActionScope.unlocked(second);}
  }
 }
 private NativeSourcePairThird(){}
}
