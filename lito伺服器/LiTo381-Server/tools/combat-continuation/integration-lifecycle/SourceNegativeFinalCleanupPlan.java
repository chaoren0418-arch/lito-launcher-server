package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Strict memory-only cleanup fault cases; no production fault class or changed candidate byte. */
public final class SourceNegativeFinalCleanupPlan {
 static int checks;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
 static void run(boolean broken,boolean withPrimary)throws Throwable{
  var f=SourceMaterialElementalIngressPlan.fixture();var b=f.base().first().base().base();var c=new NativeAreaMaterialAction.Context(b.caster());var a=new NativeSourceNegativeTargets.Aim(c,c.npcs.get(777L),null,29);var baseline=b.life().pending.get();var scope=new NativeCompanionTargets.Scope(c);var good=new ArrayList<>(scope.receipts.values());NativePlayerVitalCompletions.Receipt bad=null;var primary=withPrimary?new IOException("original producer fixture failure"):null;Throwable caught=null;
  try{
   check(scope.valid&&!good.isEmpty(),"actual original companion receipt reserved on strict memory lifetime");
   if(broken){bad=(NativePlayerVitalCompletions.Receipt)SourceSignedRecoveryPlan.unsafe().allocateInstance(NativePlayerVitalCompletions.Receipt.class);SourceWeaponFullPathPlan.put(bad,"released",new AtomicBoolean());var dummy=SourceSignedRecoveryPlan.shell(b.hub(),"cleanup-fault-receipt");SourceWeaponFullPathPlan.put(bad,"pc",dummy);scope.receipts.put(dummy,bad);}
   try{NativeSourceNegativeFinalFrame.cleanup(a,scope,primary);}catch(Throwable e){caught=e;}
   check(a.closed&&a.nativeSourceNegativeFinal==null&&a.nativeSourceNegativePairFirst==null&&c.nativeSourceNegativeAim==null,"Aim and both bounded producing first/scope fields cleared");
   check(scope.closed&&scope.receipts.isEmpty()&&scope.bindings.isEmpty()&&scope.caster.get()==null,"original companion scope emptied even if a receipt close throws");
   check(good.stream().allMatch(r->r.released.get())&&b.life().pending.get()==baseline,"all real original receipts released and pending count restored");
   if(broken){check(bad.released.get(),"injected invalid receipt claims its one release");check(withPrimary?caught==null&&primary.getSuppressed().length==1&&primary.getSuppressed()[0]instanceof NullPointerException:caught instanceof NullPointerException,"original primary preserved and cleanup failure suppressed, or original cleanup failure propagated");}
   else check(caught==null,"normal original cleanup unchanged");
  }finally{a.close();scope.close();SourceWeaponFullPathPlan.cleanup(f);}
 }
 public static void main(String[]args)throws Throwable{SourceWeaponOverlayPlan.environment(()->{run(false,false);run(true,false);run(true,true);});System.out.println("PASS"+checks+" negative final cleanup memory-only faults;3 cases,original primary/suppression,pending and all receipt/Scope/Aim releases; runtime unchanged");}
}
