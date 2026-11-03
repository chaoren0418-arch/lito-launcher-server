package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual C_USE_SPELL with read-only loaded controls replaced in this JVM only. */
public final class CancellationImmunityProbe extends CancellationMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=44;var row=spell(skill);boolean immune=(mode&1)!=0,self=mode>=2;var target=self?pc:peer;targetId=target.playerObjectId();
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var original=(CombatDatabase.Snapshot)f.get(null);
  f.set(null,copy(original,Map.of("controls",copy(original.controls(),Map.of("cancellationImmune",immune?Set.of(43):Set.of())))));
  try{
   int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),amount=(int)call(row,"itemConsumeCount",new Class[]{});NativeDeItemPublication.grant(pc,item,amount+3);int mpCost=cost(pc,skill,row);
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    need(!active(target,skill),"target haste active before cancellation");cast(pc,skill,row);
    need((int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"valid cancellation consumes source resources once even when buff protected");
    boolean removed=active(target,skill);if(removed!=!immune)gaps++;
    if(!before)need(removed==!immune,"DB immunity controls actual haste retention");
    collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked output and drained cast");
   }
  }finally{f.set(null,original);}cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<4;mode++){selectedSkill=skill;selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("weaponBreakFixtureD100",mode==1?100:1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
  need(NativePlayerLifetime.allIdle(),"all native lifetimes idle");
  if(!before)need(gaps==0,"all configured immunity preserved by actual cancellation");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"source_immunity_gaps\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
