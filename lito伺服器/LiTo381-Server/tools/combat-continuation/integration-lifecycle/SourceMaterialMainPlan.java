package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** All58 initial closed ingress plus actual material73 after-lock lifetime/Main waits. */
public final class SourceMaterialMainPlan {
 static int checks,waits;static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 static final class Gate extends java.util.concurrent.locks.ReentrantLock{final CountDownLatch tried=new CountDownLatch(1);public void lock(){if(isLocked()&&!isHeldByCurrentThread())tried.countDown();super.lock();}}
 static void wait73(String mode)throws Throwable{
  var f=SourceWeaponPlayerFullPathPlan.fixture("normal");var b=f.base().first().base().base();var pc=b.caster();var oldLock=pc.nativePlayerActionLock();var field=Arrays.stream(ClientSession.class.getDeclaredFields()).filter(x->{try{x.setAccessible(true);return x.get(pc)==oldLock;}catch(Exception e){throw new AssertionError(e);}}).findFirst().orElseThrow();var gate=new Gate();field.set(pc,gate);var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object oldRules=rules.get(null);
  var inventory=RuntimeInventory.fromDurableState(Map.of(24L,1,40318L,30),Map.of(24L,List.of(new RuntimeInventory.PersistentItem(99993L,1,0,0,true,1,false,true))));SourceWeaponFullPathPlan.put(pc,"inventory",inventory);SourceWeaponFullPathPlan.put(pc,"learnedSkills",Set.of(73));SourceWeaponFullPathPlan.put(pc,"playerMp",2000);SourceWeaponFullPathPlan.put(pc,"playerMaxMp",2000);
  var state=(GameStateStore)CombatAccess.field(pc,"state");String key=(String)CombatAccess.call(pc,"stateCharacter","()Ljava/lang/String;");state.saveEconomyState(key,0L,inventory.catalogCounts(),inventory.catalogItemStates());
  var failure=new AtomicReference<Throwable>();var worker=new Thread(()->{try{NativeLightAction.dispatch(pc,new Protocol.Writer().c(Protocol.C_USE_SPELL).c(9).c(0).d(99993L).bytes());}catch(Throwable e){failure.set(e);}},"material73-original-self-action-lock");
  try{
   gate.lock();try{worker.start();check(gate.tried.await(3,TimeUnit.SECONDS),"actual73 waits for original source action lock after ingress Receipt");
    if(mode.equals("unloaded-Main")){rules.set(null,null);SourceWeaponFullPathPlan.put(b.hub(),"nativeStaticMotionClosed",true);}else SourceTargetNegativeReservePlan.mutate(f.base(),mode);
   }finally{gate.unlock();}
   worker.join(3000);check(!worker.isAlive()&&failure.get()==null,"actual73 resumes without exceptions or identity repair "+mode+" "+failure.get());
   var now=(RuntimeInventory)CombatAccess.field(pc,"inventory");int mp=(int)CombatAccess.field(pc,"playerMp");int bytes=f.sockets().values().stream().mapToInt(s->s.bytes.size()).sum();boolean admitted=Set.of("normal","DRAINING").contains(mode);
   if(admitted)check(mp==1806&&now.countCatalog(40318)==20&&now.item(99993L).enchantLevel()==1&&bytes>0,"original73 accepted costs/enchant/output once, including DRAINING "+mode);
   else check(mp==2000&&now==inventory&&now.countCatalog(40318)==30&&now.item(99993L).enchantLevel()==0&&bytes==0,"closed/stale original73 has no cost/equipment mutation/output "+mode);
   SourceTargetNegativeReservePlan.noInit(f.base(),mode);waits++;
  }finally{if(gate.isHeldByCurrentThread())gate.unlock();worker.join(3000);rules.set(null,oldRules);field.set(pc,oldLock);SourceWeaponFullPathPlan.cleanup(f);}
 }
 public static void main(String[]args)throws Throwable{
  SourceMaterialBroadIngressPlan.main(new String[]{args[0],args[1]});var lines=Files.readAllLines(Path.of(args[1]));int closed=0,normal73=0;
  for(String line:lines.subList(1,lines.size())){String[] s=line.split("\t",-1);if(Set.of("Main","STOPPED").contains(s[1])){check(s[2].equals("rejected-or-no-output")&&s[3].equals("2000")&&s[5].equals("0")&&s[6].isEmpty()&&s[9].isEmpty(),"all58 original closed native ingress rejects before cost/packet/exception "+s[0]+"/"+s[1]);closed++;}else if(s[0].equals("73")){check(s[2].equals("cost-observed")&&s[3].equals("1806")&&s[4].equals("20")&&Integer.parseInt(s[5])>0&&s[6].isEmpty(),"real native73 positive control still pays exact original reduced MP/material");normal73++;}}
  check(closed==116&&normal73==2,"all58 Main/STOPPED and both native73 positive controls exercised");
  SourceWeaponOverlayPlan.environment(()->{BattleSpellCatalog.magicalWeaponSpell(73);var db=CombatDatabase.class.getDeclaredField("current");db.setAccessible(true);Object old=db.get(null);try{db.set(null,new CombatDatabase.Snapshot(Map.of(27,new CombatDatabase.Skill(32,0,3000,0,0,0,0,40318,1),73,new CombatDatabase.Skill(200,0,0,0,0,0,0,40318,10)),new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));for(String mode:List.of("normal","DRAINING","Main","STOPPED","lifetime","foreign-life","source-role","unloaded-Main"))wait73(mode);}finally{db.set(null,old);}});
  check(NativeSourceMaterialMain.class.getDeclaredFields().length==0,"no added TLS/registry/queue/worker/lifecycle owner");System.out.println("PASS"+checks+" material Main closed-native-ingress/original73-self-lock/first-lifetime contracts; closed-material-ingress=116, original73-positive-controls=2, original73-self-lock-waits="+waits+"; other normal material errors/Source semantics/final/backend/MySQL/cursor unqualified");
 }
}
