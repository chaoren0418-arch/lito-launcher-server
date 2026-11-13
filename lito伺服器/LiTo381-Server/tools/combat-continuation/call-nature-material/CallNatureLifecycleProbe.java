package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public class CallNatureLifecycleProbe extends CallNatureMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int kind,int mode)throws Exception{
  setup(pc,peer,hub,true);int change=mode/2;boolean nativeEntry=mode%2==0;var row=BattleSpellCatalog.resurrectionSpell(165);
  BattleCompanion pet=null;ClassicWorldMonsterPopulation population=null;ClassicWorldMonsterPopulation.Snapshot npc=null;
  if(kind!=0)set(peer,"playerHp",4000);
  if(kind==1){pet=CallNatureCompanionProbe.companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);pet.applyDamage(Integer.MAX_VALUE);targetId=pet.snapshot().objectId();}
  if(kind==2){npc=CallNatureWorldNpcProbe.monster(pc,hub);population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");population.damage((int)get(pc,"classicWorldMapId"),npc.objectId(),Integer.MAX_VALUE);targetId=npc.objectId();}
  long id=targetId;int item=(int)row.itemConsumeId(),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,count+3);
  try(var self=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var clean=new java.util.concurrent.atomic.AtomicBoolean();
   Thread worker=new Thread(()->{try{byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(20).c(4).d(id).bytes();if(nativeEntry)call(pc,"handle",new Class[]{byte[].class},g16Intent(pc,id,packet));else call(pc,"useCallOfNatureSpell",new Class[]{byte[].class,row.getClass()},packet,row);clean.set(NativeCallNatureMaterialAction.current.get()==null&&NativeAreaMaterialAction.current.get()==null);}catch(Throwable e){error.set(e);}},"call-nature-corpse-race");
   var lock=peer.nativePlayerActionLock();lock.lock();try{
    worker.start();boolean waiting=false;long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
    while(worker.isAlive()&&System.nanoTime()<limit){if(Arrays.stream(worker.getStackTrace()).anyMatch(f->f.getClassName().endsWith("NativeAreaMaterialAction")&&f.getMethodName().equals("run"))){waiting=true;break;}Thread.sleep(1);}
    need(waiting,"actual packet captured target before area ownership wait");need((int)get(pc,"playerMp")==1000&&pc.nativeDeInventory(0).countCatalog(item)==count+3,"queued corpse cast has no premature debit");
    if(change==1)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
    if(change==2)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    if(change==3){if(kind==0)set(peer,"playerHp",4000);if(kind==1)pet.resurrect(pet.snapshot().maxHp());if(kind==2)hub.resurrectClassicWorldMonster((int)get(pc,"classicWorldMapId"),(int)get(pc,"classicWorldInstanceId"),id,npc.maximumHp());}
    if(change==4){if(kind==0)set(peer,"phase",Enum.valueOf((Class)get(peer,"phase").getClass(),"LOBBY"));if(kind==1)((List<?>)get(peer,"tamedCompanions")).remove(pet);if(kind==2)CallNatureWorldNpcProbe.monster(pc,hub);}
    if(change==5)NativePlayerCommit.scene(pc,65530,"classicWorldMapId");
    if(change==6)set(pc,NativeDeItemApplication.DISCONNECTED,true);
    if(change==7)set(peer,NativeDeItemApplication.DISCONNECTED,true);
   }finally{lock.unlock();worker.join(3000);}
   if(error.get()!=null)throw new AssertionError("queued corpse cast",error.get());need(!worker.isAlive()&&clean.get(),"bounded cast finished and scoped state restored");
   boolean accepted=change==0||kind==2&&(change==2||change==7);
   need((int)get(pc,"playerMp")==1000-(accepted?cost(pc,165,row):0),"queued corpse admission kind="+kind+" mode="+mode);
   need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"queued corpse material once");
   if(kind==0)need((get(peer,"pendingResurrection")!=null)==accepted,"stale role cannot receive resurrection question");
   collect(self);collect(other);need(self.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"queued output unlocked and receipt drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int kind=0;kind<3;kind++)for(int mode=0;mode<16;mode++){int skill=165;selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+kind+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("decayPotionFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    exercise(p.session,m.session,hub,kind,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
