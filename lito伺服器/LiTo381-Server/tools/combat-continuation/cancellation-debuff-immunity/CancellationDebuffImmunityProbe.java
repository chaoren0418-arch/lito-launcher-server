package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real skill-44 packets; replace only this fixture JVM's read-only loaded immune set. */
public final class CancellationDebuffImmunityProbe extends CancellationMaterialProbe {
 static int selectedStatus;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);boolean self=mode>=3,immune=mode%3==1;var target=self?pc:peer;targetId=target.playerObjectId();
  var timed=(BattleTimedStatus)get(target,"timedStatus");timed.clear();long now=System.nanoTime();
  String status=switch(selectedStatus){case 47->"weakness";case 56->"disease";case 71->"decayPotion";case 173->"polluteWater";case 174->"strikerGale";default->throw new AssertionError();};
  String method="activate"+Character.toUpperCase(status.charAt(0))+status.substring(1);
  call(timed,method,new Class[]{long.class,int.class},now,60);
  timed.activateLight(now,60);String deadline=status+"ExpiresAtNanos";
  long originalDeadline=(long)get(timed,deadline);
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var old=(CombatDatabase.Snapshot)field.get(null);
  Set<Integer> ids=mode%3==0?Set.of():Set.of(immune?selectedStatus:(selectedStatus==47?56:47));
  field.set(null,copy(old,Map.of("controls",copy(old.controls(),Map.of("cancellationImmune",ids)))));
  try{
   var row=spell(44);int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{}),mp=cost(pc,44,row);NativeDeItemPublication.grant(pc,item,count+3);
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    cast(pc,44,row);
    need((int)get(pc,"playerMp")==1000-mp&&pc.nativeDeInventory(0).countCatalog(item)==3,"immune targets still consume material and MP exactly once");
    boolean remains=(long)get(timed,deadline)==originalDeadline;
    if(remains!=immune)gaps++;
    if(!before){need(remains==immune,"only exact configured debuff skill survives");need((long)get(timed,deadline)==(immune?originalDeadline:0),"deadline retained exactly, never refreshed");}
    need((long)get(timed,"lightExpiresAtNanos")==0,"unprotected light still removed");
    collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked output and no retained completion");
    // This is the raw clear used by disposal, not a skill-44 call site.
    if(immune&&!before){timed.cancelMagicEffects();need((long)get(timed,deadline)==0,"raw cleanup bypasses skill immunity");}
   }
  }finally{field.set(null,old);}cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate dispatcher");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int speed:new int[]{47,56,71,173,174})for(int mode=0;mode<6;mode++){
   selectedStatus=speed;var dir=root.resolve(flag+"-"+world+"-"+app+"-"+speed+"-"+mode);Files.createDirectories(dir);
   var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",dir.resolve("must-not-write.properties")));
   var state=new GameStateStore(dir.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(dir.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(dir.resolve("must-not-write.properties")),"both persistence flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all native lifetimes idle");need(gaps==(before?80:0),"all exact immune cases accounted for");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"source_immunity_gaps\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
