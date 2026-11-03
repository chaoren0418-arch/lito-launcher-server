package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;import java.net.*;import java.util.concurrent.atomic.*;
/** Audit actual socket delay against the existing curse warning deadline; no producer overlay. */
public class PoisonScopeCurseConsumerProbe extends PoisonScopeMaterialProbe {
 static int early;
 static final List<Long> remaining=new ArrayList<>();
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var control=CombatRules.class.getDeclaredField("current");control.setAccessible(true);var originalControl=(CombatRules.Snapshot)control.get(null);var settings=new HashMap<>(originalControl.values());if(mode==1){settings.put("CURSE_READY_TIME",0.0);settings.put("CURSE_TIME",1.0);}if(mode==2){settings.put("CURSE_READY_TIME",2.0);settings.put("CURSE_TIME",3.0);}control.set(null,copy(originalControl,Map.of("values",settings)));var row=spell(33);probability(33);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});
  NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();var delayed=new AtomicLong();
   set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){
    public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true)){long start=System.nanoTime();try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}delayed.set(System.nanoTime()-start);}out.write(b,o,n);}
    public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
   try{cast(pc,33,row);}finally{set(pc,"socket",original);}
   need(fired.get()&&delayed.get()>=TimeUnit.MILLISECONDS.toNanos(350),"actual first socket write delayed");
   need(active(peer,33),"actual curse installed");
   var status=(BattleTimedStatus)get(peer,"timedStatus");long left=(long)get(status,"paralysisActivatesAtNanos")-System.nanoTime();
   long expected=TimeUnit.MILLISECONDS.toNanos((int)call(row,"warningDelayMillis",new Class[]{}));
   remaining.add(TimeUnit.NANOSECONDS.toMillis(left));need(left>=expected-TimeUnit.MILLISECONDS.toNanos(150),"configured warning begins after palette output");
   need((int)call(row,"warningDelayMillis",new Class[]{})==Math.round(settings.get("CURSE_READY_TIME")*1000),"warning consumer uses configured control");
   need((int)call(row,"paralysisDurationMillis",new Class[]{})==Math.round(settings.get("CURSE_TIME")*1000),"active consumer uses configured control");
   long starts=(long)get(status,"paralysisActivatesAtNanos");peer.serverExpireTimedStatuses(starts-1);need(!status.paralysisActive(starts-1),"not locked before warning deadline");
   peer.serverExpireTimedStatuses(starts);need(status.paralysisActive(starts),"actual clock locks at exact deadline");
   long expires=(long)get(status,"paralysisExpiresAtNanos");long activeNs=TimeUnit.MILLISECONDS.toNanos((int)call(row,"paralysisDurationMillis",new Class[]{}));
   need(expires>=starts+activeNs&&expires-starts-activeNs<TimeUnit.MILLISECONDS.toNanos(150),"configured active duration after lock packet");
   peer.serverExpireTimedStatuses(expires-1);need(status.paralysisActive(expires-1),"still locked before active expiry");
   peer.serverExpireTimedStatuses(expires);need(!status.paralysisVisualActive(expires)&&!status.paralysisActive(expires),"real clock releases at active expiry");
   PoisonScopeTimerProbe.awaitIdle();
   var packets=collect(other);String apply=HexFormat.of().formatHex(Protocol.paralysis(Protocol.ParalysisMode.PARALYSIS_APPLY)),release=HexFormat.of().formatHex(Protocol.paralysis(Protocol.ParalysisMode.PARALYSIS_RELEASE));
   need(Collections.frequency(packets,apply)==1&&Collections.frequency(packets,release)==1&&packets.indexOf(apply)<packets.indexOf(release),"one native lock then one native release");
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"no locked output or retained receipt");
  }finally{control.set(null,originalControl);}cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{33})for(int mode=0;mode<3;mode++){selectedSkill=skill;selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("cursePoisonFixturePercentileRoll",1,"curseParalysisFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"early_warning_clocks\":"+early+",\"remaining_millis\":"+remaining+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
