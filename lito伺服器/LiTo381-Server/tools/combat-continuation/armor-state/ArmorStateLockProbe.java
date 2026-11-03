package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Actual WORLD state methods: shared action exclusion and absence of the old PC-monitor inversion. */
public final class ArmorStateLockProbe extends ArmorMaterialProbe {
 static int bypasses,monitorBlocks;
 static void exerciseState(ClientSession pc,ClientSession peer,GameHub hub,int mode,boolean monitor)throws Exception{
  setup(pc,peer,hub,true);long now=System.nanoTime(),initial=mode==0?0:mode==1?1:now+TimeUnit.SECONDS.toNanos(8);
  set(pc,"classicWorldArmorBreakExpiresAtNanos",initial);var failure=new AtomicReference<Throwable>();var returned=new AtomicReference<Boolean>();
  var worker=new Thread(()->{try{returned.set((boolean)(mode==0?call(pc,"activateClassicWorldArmorBreak",new Class[]{long.class,int.class},now,8):mode==1?call(pc,"classicWorldArmorBreakActive",new Class[]{long.class},now):call(pc,"cancelClassicWorldArmorBreak",new Class[]{})));}catch(Throwable e){failure.set(e);}},"armor-state-lock");
  var lock=pc.nativePlayerActionLock();
  if(monitor){synchronized(pc){worker.start();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(worker.isAlive()&&worker.getState()!=Thread.State.BLOCKED&&System.nanoTime()<end)Thread.sleep(1);need(before?worker.getState()==Thread.State.BLOCKED:!worker.isAlive(),"state helper retains Source lock but no old PC monitor");if(before)monitorBlocks++;}}
  else{lock.lock();try{worker.start();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(worker.isAlive()&&!lock.hasQueuedThread(worker)&&System.nanoTime()<end)Thread.sleep(1);need(before?!worker.isAlive():lock.hasQueuedThread(worker),"actual state helper participates in canonical action lock");if(before)bypasses++;else need((long)get(pc,"classicWorldArmorBreakExpiresAtNanos")==initial,"blocked state transition makes no early mutation");}finally{lock.unlock();}}
  worker.join(3000);need(!worker.isAlive(),"state worker finishes after release");if(failure.get()!=null)throw new AssertionError(failure.get());
  need(returned.get()==(mode!=1),"original activation/expiry/cancellation return preserved");need((long)get(pc,"classicWorldArmorBreakExpiresAtNanos")== (mode==0?now+TimeUnit.SECONDS.toNanos(8):0),"exact original deadline or clear preserved");
  need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeLightStoppedProbe.idleContext(),"state access creates no receipt or retained context");cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual pinned state methods");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean monitor:new boolean[]{false,true})for(int mode=0;mode<3;mode++){
   var r=root.resolve(flag+"-"+monitor+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);exerciseState(p.session,m.session,hub,mode,monitor);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory only for both flags");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"state fixture lifetimes drained");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_lock_bypasses\":"+bypasses+",\"old_PC_monitor_blocks\":"+monitorBlocks+",\"full_goal_achieved\":false}";Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
