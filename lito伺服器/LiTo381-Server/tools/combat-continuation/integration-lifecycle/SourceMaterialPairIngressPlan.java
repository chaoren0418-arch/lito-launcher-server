package local.lito381.server;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Existing first Context followed by original commitPair before its reservations and locks. */
public final class SourceMaterialPairIngressPlan {
 static int checks,attempts,fenceWaits,receipts,rollbacks;static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 record Scoped(SourceWeaponFullPathPlan.Fixture f,NativeAreaMaterialAction.Context c,NativeSelfCastAction.Task task,NativeServerLifetime app){}
 static Scoped scoped(boolean application)throws Exception{
  var f=SourceRemainingMaterialPairPlan.fixture(173);var b=f.base().first().base().base();NativeServerLifetime app=null;
  if(application){app=(NativeServerLifetime)SourceSignedRecoveryPlan.unsafe().allocateInstance(NativeServerLifetime.class);SourceWeaponFullPathPlan.put(app,"hub",b.hub());SourceWeaponFullPathPlan.put(app,"fence",new Object());SourceWeaponFullPathPlan.put(b.life(),"application",app);}
  var c=new NativeAreaMaterialAction.Context(b.caster());check(NativeSourceTargetNegativeLife.capture(c,b.target().playerObjectId()),"original Context target discovery before original reserve entry");var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);NativeSelfCastAction.Task task;
  try{task=NativeSourceMaterialPairIngress.prepare(b.caster(),b.target(),()->{throw new AssertionError("receipt fixture must not invoke body");});}finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);}
  check(NativeSourceMaterialPairIngress.enter(task),"prepared first Task values are usable before locks without granting execution authority");return new Scoped(f,c,task,app);
 }
 static void release(Scoped f,ClientSession pc,NativePlayerVitalCompletions.Receipt r,long before){var life=f.f().base().first().base().base().life();check(r!=null&&r.pc==pc&&r.life==life&&r.app==f.app(),"original Receipt retains exact first PC/lifetime/application");check(NativePlayerVitalCompletions.pending(life)==before+1&&(f.app()==null||(int)CombatAccess.field(f.app(),"handlers")==1),"original native counters register exactly once");r.close();check(NativePlayerVitalCompletions.pending(life)==before&&(f.app()==null||(int)CombatAccess.field(f.app(),"handlers")==0),"original receipt closes pending/app handler exactly once");receipts++;}
 static void cleanup(Scoped f)throws Exception{NativeSourceTargetNegativeLife.close(f.c());SourceWeaponFullPathPlan.cleanup(f.f());}
 static void fence(boolean source,String mode,boolean packet)throws Throwable{
  var f=scoped(true);var b=f.f().base().first().base().base();var pc=source?b.caster():b.target();var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object old=rules.get(null);var result=new AtomicReference<NativePlayerVitalCompletions.Receipt>();var failure=new AtomicReference<Throwable>();
  var worker=new Thread(()->{var stack=NativeServerLifetime.packets.get();if(packet)stack.addLast(f.app());try{result.set(NativeSourceMaterialPairIngress.reserve(pc,f.task()));}catch(Throwable e){failure.set(e);}finally{if(packet)stack.removeLast();NativeServerLifetime.packets.remove();}},"material-pair-original-receipt-fence");
  try{
   synchronized(f.app().fence){worker.start();SourceElementAudiencePlan.blocked(worker,"actual shared material original receipt app fence "+source+"/"+mode);check(Arrays.stream(worker.getStackTrace()).anyMatch(x->x.getMethodName().equals("nativeSourceMaterialPairReserve")),"wait occurs inside complete original native Receipt sibling");
    if(mode.equals("unloaded"))rules.set(null,null);else if(mode.equals("application"))SourceWeaponFullPathPlan.put(b.life(),"application",null);else if(mode.equals("app-closing"))SourceWeaponFullPathPlan.put(f.app(),"closing",true);else SourceTargetNegativeReservePlan.mutate(f.f().base(),mode);
   }
   worker.join(3000);check(!worker.isAlive()&&failure.get()==null,"original app-fence wait terminates without replacement capture or exception "+mode);
   if(Set.of("normal","DRAINING").contains(mode)||packet&&mode.equals("app-closing"))release(f,pc,result.get(),0);else check(result.get()==null&&NativePlayerVitalCompletions.pending(b.life())==0&&(int)CombatAccess.field(f.app(),"handlers")==0,"stale first pair rejected under original app fence before counters "+mode);
   SourceTargetNegativeReservePlan.noInit(f.f().base(),mode);fenceWaits++;
  }finally{worker.join(3000);rules.set(null,old);cleanup(f);}
 }
 static void noApplication(String mode)throws Exception{var f=scoped(false);var b=f.f().base().first().base().base();try{SourceTargetNegativeReservePlan.mutate(f.f().base(),mode);var r=NativeSourceMaterialPairIngress.reserve(b.target(),f.task());if(Set.of("normal","DRAINING").contains(mode))release(f,b.target(),r,0);else check(r==null&&NativePlayerVitalCompletions.pending(b.life())==0,"original no-app processes branch rejects stale first lifetime "+mode);SourceTargetNegativeReservePlan.noInit(f.f().base(),mode);}finally{cleanup(f);}}
 static void constructorFailure()throws Exception{
  var f=scoped(true);var b=f.f().base().first().base().base();var field=NativePlayerVitalCompletions.class.getDeclaredField("pending");field.setAccessible(true);Object old=field.get(null);var unsafe=SourceSignedRecoveryPlan.unsafe();Object base=unsafe.staticFieldBase(field);long offset=unsafe.staticFieldOffset(field);var map=new SourceTargetNegativeReservePlan.FailingMap();
  try{unsafe.putObjectVolatile(base,offset,map);try{NativeSourceMaterialPairIngress.reserve(b.target(),f.task());throw new AssertionError("original Receipt constructor failure swallowed");}catch(RuntimeException expected){check(expected==map.failure&&(int)CombatAccess.field(f.app(),"handlers")==0,"complete original constructor catch rolls app handler back and preserves exact failure");}check(!Thread.holdsLock(f.app().fence),"original failure handler releases application monitor");rollbacks++;}finally{unsafe.putObjectVolatile(base,offset,old);cleanup(f);}
 }
 static void missingWithoutContext(boolean source)throws Throwable{var f=SourceRemainingMaterialPairPlan.fixture(173);var b=f.base().first().base().base();var pc=source?b.caster():b.target();try{check(NativeAreaMaterialAction.current.get()==null,"no prior Context in initial missing-status fixture");SourceWeaponFullPathPlan.put(pc,"timedStatus",null);NativeLightAction.commitPair(b.caster(),b.target(),()->{throw new AssertionError("missing initial status cannot grant first Task authority");});check(CombatAccess.field(pc,"timedStatus")==null&&NativePlayerVitalCompletions.pending(b.life())==0,"missing initial status never captures/initializes status or reserves a Receipt");}finally{SourceWeaponFullPathPlan.cleanup(f);}}
 static void exercise(String mode)throws Throwable{
  var f=SourceRemainingMaterialPairPlan.fixture(173);var b=f.base().first().base().base();var c=new NativeAreaMaterialAction.Context(b.caster());var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object old=rules.get(null);var calls=new AtomicInteger();
  check(NativeSourceTargetNegativeLife.capture(c,b.target().playerObjectId()),"actual Context captures first target before original pair entry");
  try{
   if(mode.equals("unloaded"))rules.set(null,null);else SourceTargetNegativeReservePlan.mutate(f.base(),mode);
   Throwable failure=null;try{NativeAreaMaterialAction.owned(c,()->NativeLightAction.commitPair(b.caster(),b.target(),()->calls.incrementAndGet()));}catch(Throwable e){failure=e;}
   if(failure!=null)failure.printStackTrace();check(failure==null,"stale first material identity rejected before original recapture/receipt, mode="+mode+" error="+failure);
   check(calls.get()==(mode.equals("normal")?1:0),"original body only for still-current original first pair "+mode);
   SourceTargetNegativeReservePlan.noInit(f.base(),mode);check(f.sockets().values().stream().allMatch(s->s.bytes.size()==0),"entry fixture never emits game frames");attempts++;
  }finally{rules.set(null,old);NativeSourceTargetNegativeLife.close(c);SourceWeaponFullPathPlan.cleanup(f);}
 }
 public static void main(String[]args)throws Throwable{
  SourceWeaponOverlayPlan.environment(()->{
   BattleSpellCatalog.polluteWaterSpell(173);var db=CombatDatabase.class.getDeclaredField("current");db.setAccessible(true);Object old=db.get(null);
   try{db.set(null,new CombatDatabase.Snapshot(Map.of(27,new CombatDatabase.Skill(32,0,3000,0,0,0,0,40318,1),173,new CombatDatabase.Skill(50,0,1000,192,0,0,0,40319,1)),new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));
    for(String mode:args.length==0?new String[]{"normal","source-status","owner-status","lifetime","foreign-life","Main","STOPPED","source-role","owner-role","owner-life","unloaded"}:args)exercise(mode);
    if(args.length==0){for(boolean source:List.of(false,true)){for(String mode:List.of("normal","DRAINING","STOPPED","Main","lifetime","foreign-life","source-role","owner-role","source-status","owner-status","owner-life","application","app-closing","unloaded"))fence(source,mode,false);for(String mode:List.of("app-closing","Main","STOPPED","owner-role"))fence(source,mode,true);missingWithoutContext(source);}for(String mode:List.of("normal","DRAINING","STOPPED","Main","lifetime","owner-status"))noApplication(mode);constructorFailure();check(NativeSourceMaterialPairIngress.class.getDeclaredFields().length==0,"no new registry/TLS/worker or retained Context fields");}
   }finally{db.set(null,old);}
  });System.out.println("PASS"+checks+" material first pair ingress original-capture/receipt/no-initialization contracts; original-pair-entries="+attempts+", original-app-fence-waits="+fenceWaits+", original-receipts="+receipts+", constructor-rollbacks="+rollbacks+"; full material and final transport unqualified");
 }
}
