package local.lito381.server;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;

/** Actual native material173/174 ingress at the shared source/target lock boundary. */
public final class SourceRemainingMaterialPairPlan {
 static final AtomicInteger checks=new AtomicInteger();static int attempts,waits;
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks.incrementAndGet();}
 static void put(Object o,String k,Object v){SourceWeaponFullPathPlan.put(o,k,v);}
 static BattleSpellCatalog.TargetNegativeStatusSpell model(int id){return id==173?BattleSpellCatalog.polluteWaterSpell(id):BattleSpellCatalog.strikerGaleSpell(id);}
 static SourceWeaponFullPathPlan.Fixture fixture(int id)throws Exception{
  var f=SourceWeaponPlayerFullPathPlan.fixture("normal");var b=f.base().first().base().base();var spell=model(id);
  var old=(CharacterSummary)CombatAccess.call(b.caster(),"selectedCharacterSummary","()Llocal/lito381/server/CharacterSummary;");
  put(b.caster(),"characters",List.of(SourceWeaponOverlayPlan.copy(old,Map.of("type",2))));put(b.caster(),"learnedSkills",Set.of(id));put(b.caster(),"elfAttribute",spell.attribute());
  var inventory=RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.SPIRIT_CRYSTAL,15));put(b.caster(),"inventory",inventory);
  var state=(GameStateStore)CombatAccess.field(b.caster(),"state");String key=(String)CombatAccess.call(b.caster(),"stateCharacter","()Ljava/lang/String;");state.saveEconomyState(key,0L,inventory.catalogCounts(),inventory.catalogItemStates());
  return f;
 }
 static final class Gate extends java.util.concurrent.locks.ReentrantLock{
  final CountDownLatch tried=new CountDownLatch(1);boolean captured;NativeAreaMaterialAction.Context context;
  public void lock(){if(isLocked()&&!isHeldByCurrentThread()&&!captured){captured=true;context=NativeAreaMaterialAction.current.get();check(context!=null&&NativeSourceTargetNegativeLife.proof(context)!=null,"actual remaining material pair retains original first target proof");tried.countDown();}super.lock();}
 }
 static void attempt(int id,String mode)throws Throwable{
  var f=fixture(id);var b=f.base().first().base().base();var peer=b.target();var original=peer.nativePlayerActionLock();var field=Arrays.stream(ClientSession.class.getDeclaredFields()).filter(x->{try{x.setAccessible(true);return x.get(peer)==original;}catch(Exception e){throw new AssertionError(e);}}).findFirst().orElseThrow();var gate=new Gate();field.set(peer,gate);
  var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object priorRules=rules.get(null);
  var error=new AtomicReference<Throwable>();var worker=new Thread(()->{try{NativeLightAction.dispatch(b.caster(),SourceTargetNegativeLifePlan.packet(id,peer.playerObjectId()));}catch(Throwable e){error.set(e);}},"remaining-material-original-pair-"+id+"-"+mode);
  try{
   gate.lock();try{worker.start();check(gate.tried.await(3,TimeUnit.SECONDS),"actual material pair waits "+id+" "+mode);
    if(mode.equals("closed-proof"))NativeSourceTargetNegativeLife.close(gate.context);
    else if(mode.equals("unloaded"))rules.set(null,null);
    else if(mode.equals("source-life"))CombatAccess.set(CombatAccess.field(CombatAccess.field(b.caster(),"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());
    else if(mode.endsWith("status-replaced")){var pc=mode.startsWith("source")?b.caster():peer;Object state=CombatAccess.field(CombatAccess.field(pc,"timedStatus"),"sourcePoisonLifecycle");var status=new BattleTimedStatus();CombatAccess.set(status,"sourcePoisonLifecycle",state);put(pc,"timedStatus",status);}
    else SourceTargetNegativeReservePlan.mutate(f.base(),mode);
   }finally{gate.unlock();}
   worker.join(3000);if(error.get()!=null)error.get().printStackTrace();check(!worker.isAlive()&&error.get()==null,"shared material pair rejects stale first status without recapture "+id+"/"+mode+" error="+error.get());
   int mp=(int)CombatAccess.field(b.caster(),"playerMp");long gems=((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(InventoryCatalog.SPIRIT_CRYSTAL);
   if(mode.equals("normal"))check(mp==100-(id==173?50:10)&&gems==15-model(id).itemConsumeCount(),"actual remaining material normal path pays original MP/material once "+id+" mp="+mp+" gems="+gems);
   else check(mp==100&&gems==15&&f.sockets().values().stream().allMatch(s->s.bytes.size()==0),"stale remaining material never charges or emits "+id+"/"+mode);
   SourceTargetNegativeReservePlan.noInit(f.base(),mode);attempts++;waits++;
  }finally{if(gate.isHeldByCurrentThread())gate.unlock();worker.join(3000);rules.set(null,priorRules);check(!worker.isAlive(),"remaining material worker terminal");field.set(peer,original);SourceWeaponFullPathPlan.cleanup(f);}
 }
 public static void main(String[]args)throws Throwable{
  SourceWeaponOverlayPlan.environment(()->{
   BattleSpellCatalog.polluteWaterSpell(173);BattleSpellCatalog.strikerGaleSpell(174);
   var database=CombatDatabase.class.getDeclaredField("current");database.setAccessible(true);Object old=database.get(null);
   try{database.set(null,new CombatDatabase.Snapshot(Map.of(27,new CombatDatabase.Skill(32,0,3000,0,0,0,0,40318,1),173,new CombatDatabase.Skill(50,0,1000,192,0,0,0,40319,1),174,new CombatDatabase.Skill(10,0,3000,192,0,0,0,40319,5)),new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));
    for(int id:List.of(173,174))for(String mode:args.length==0?new String[]{"normal","Main","STOPPED","lifetime","foreign-life","source-role","owner-role","source-status","owner-status","source-status-replaced","owner-status-replaced","source-life","owner-life","closed-proof","unloaded"}:args)attempt(id,mode);
   }finally{database.set(null,old);}
  });
  System.out.println("PASS"+checks.get()+" remaining material actual-ingress/shared-pair/first-status/resource contracts; original-attempts="+attempts+", original-pair-waits="+waits+"; other materials/RNG/status/final/Source/game unqualified");
 }
}
