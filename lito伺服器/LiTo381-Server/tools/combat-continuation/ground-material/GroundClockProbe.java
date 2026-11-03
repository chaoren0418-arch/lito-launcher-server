package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;import java.io.*;
/** Real Source policy interface plus reversible memory-store observer, without SQL writes. */
public final class GroundClockProbe extends GroundMaterialProbe {
 static boolean armed,worldCase;static int mode,reads,flushes;static long policyDone,saveDone;static ClientSession pcCase;static GameHub hubCase;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed||mode!=1)return;flushes++;need(active(pcCase,hubCase,worldCase)==0,"no live lease while material save is pending");need((int)get(pcCase,"playerMp")==1000,"MP waits for material save");Thread.sleep(250);saveDone=System.nanoTime();
 }
 static AccountRepository accounts(){var base=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  return (AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class[]{AccountRepository.class,NativeCastMaterialPolicy.Source.class},(proxy,method,args)->{
   if(method.getName().equals("repositoryForClassicSource"))return proxy;
   if(method.getName().equals("characterAccessLevel")){
    if(armed){reads++;need(!Thread.holdsLock(hubCase)&&pcCase.nativePlayerActionLock().isHeldByCurrentThread(),"Source policy lookup outside hub monitor and inside own action lock");need(active(pcCase,hubCase,worldCase)==0,"Source policy precedes live placement");if(mode==0)Thread.sleep(250);policyDone=System.nanoTime();if(mode==2)throw new IOException("ground-source-policy-unavailable");}
    return mode==3||mode==5?200:mode==4?199:0;
   }
   try{method.setAccessible(true);return method.invoke(base,args);}catch(InvocationTargetException e){throw e.getCause();}
  });
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int scenario)throws Exception{
  mode=scenario;selectedSkill=63;setup(pc,peer,hub,world);pcCase=pc;hubCase=hub;worldCase=world;reads=flushes=0;policyDone=saveDone=0;var row=BattleSpellCatalog.groundSpell(63);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();boolean grant=mode!=3&&mode!=4;if(grant)NativeDeItemPublication.grant(pc,item,count+3);int mpCost=cost(pc,63,row);boolean accept=mode!=2&&mode!=4;
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   armed=true;try{cast(pc,63,row);}finally{armed=false;}long finished=System.nanoTime();need(reads==1,"Source policy read exactly once through native policy cache");need((int)get(pc,"playerMp")==1000-(accept?mpCost:0),"policy failure or missing material does not debit MP");int expected=grant?count+3-(accept&&mode!=5?count:0):0;need(pc.nativeDeInventory(0).countCatalog(item)==expected,"GM200 skip and nonGM199 enforce actual material ownership");need((active(pc,hub,world)>0)==accept,"live placement follows policy/material acceptance");
   if(accept){var effect=GroundLifecycleProbe.effects(pc,hub,world);var leases=GroundLifecycleProbe.leases(effect);long start=(long)get(leases.get(0),"expiresAtNanos")-TimeUnit.SECONDS.toNanos(row.durationSeconds());need(start>=Math.max(policyDone,saveDone)&&start<=finished,"full effect lifetime begins after slow policy and material commit");need(flushes==(mode==1?1:0),"actual delayed memory commit reached exactly once");GroundLifecycleProbe.expireAndPulse(pc,peer,hub,world,Math.max(policyDone,saveDone),finished);}
   else{need(!((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(63)&&pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"failed ground policy/material cancels action and reuse");if(world)need(((Map<?,?>)get(hub,"classicWorldGroundEffects")).isEmpty(),"failed WORLD placement retains no empty map entry");}
   var self=collect(wire);collect(other);if(!accept)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&other.raw.size()==0,"visible policy/material rejection299 only");need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked output and drained receipt");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,GameHub.class,NativeCastMaterialPolicy.class,BattleGroundEffects.class,ClassicWorldGroundEffects.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native material/policy/ground classes");need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"only inverse-verified memory observer overlaid");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int scenario=0;scenario<6;scenario++){mode=scenario;armed=false;var r=root.resolve(flag+"-"+world+"-"+app+"-"+scenario);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=accounts();
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,scenario);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory only with either state flag");
  }}catch(Throwable e){primary=e;throw e;}finally{armed=false;try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
