package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;import java.net.*;import java.util.concurrent.atomic.*;
/** Audit actual socket delay against the existing first damage-poison tick deadline; no producer overlay. */
public class PoisonScopeTickProbe extends PoisonScopeMaterialProbe {
 static int early;
 static final List<Long> remaining=new ArrayList<>();
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=spell(11);probability(11);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});
  NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();var delayed=new AtomicLong();
   set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){
    public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true)){long start=System.nanoTime();try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}delayed.set(System.nanoTime()-start);}out.write(b,o,n);}
    public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
   try{cast(pc,11,row);}finally{set(pc,"socket",original);}
   need(fired.get()&&delayed.get()>=TimeUnit.MILLISECONDS.toNanos(350),"actual first socket write delayed");
   need(active(peer,11),"actual damage poison installed");
   var status=(BattleTimedStatus)get(peer,"timedStatus");long left=(long)get(status,"poisonNextTickAtNanos")-System.nanoTime();
   long expected=TimeUnit.MILLISECONDS.toNanos((int)call(row,"tickIntervalMillis",new Class[]{}));
   remaining.add(TimeUnit.NANOSECONDS.toMillis(left));if(left<expected-TimeUnit.MILLISECONDS.toNanos(150))early++;
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"no locked output or retained receipt");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{11})for(int mode=0;mode<1;mode++){selectedSkill=skill;selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("cursePoisonFixturePercentileRoll",mode==1?100:1,"curseParalysisFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"early_first_ticks\":"+early+",\"remaining_millis\":"+remaining+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
