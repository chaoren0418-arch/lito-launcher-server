package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual own/other companion corpse, native resurrection packet and original filters. */
public final class ResurrectionCompanionAuditProbe extends ResurrectionMaterialProbe {
 static BattleCompanion summoned(ClientSession owner,int x,int y)throws Exception {
  var pet=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());
  pet.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));pet.cancelHaste();
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception {
  var state=new GameStateStore.PetState("fixture-pet",0x70001652L,0x90001652L,(int)InventoryCatalog.PET_COLLAR,45034,"ResurrectionPet",12,78,17,21375,0);
  var pet=BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),x,y);
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);set(peer,"playerHp",4000);int skill=selectedSkill;Object row=spell(skill);
  var pet=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);targetId=pet.snapshot().objectId();pet.applyDamage(Integer.MAX_VALUE);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var clean=new java.util.concurrent.atomic.AtomicBoolean();
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   Thread worker=new Thread(()->{try{cast(pc,skill,row);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"resurrection-companion-audit");
   var lock=pc.nativePlayerActionLock();BattleCompanion replacement=null;lock.lock();try{
    worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
    while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
    need(lock.hasQueuedThread(worker)&&(int)get(pc,"playerMp")==1000&&pet.snapshot().hp()==0,"actual native companion cast queued before mutations");
    if(mode==1)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    else if(mode==2){((List<BattleCompanion>)get(peer,"tamedCompanions")).remove(pet);replacement=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);replacement.applyDamage(Integer.MAX_VALUE);}
    else if(mode==3)set(peer,NativeDeItemApplication.DISCONNECTED,true);
   }finally{lock.unlock();worker.join(3000);}
   if(error.get()!=null)throw new AssertionError("native companion audit",error.get());need(!worker.isAlive()&&clean.get(),"audit worker and contexts drain");
   boolean revived=(mode==2?replacement:pet).snapshot().hp()>0;
   if(mode==0)need(revived,"actual companion resurrection control");
   else if(revived)gaps++;
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"audit output and receipts drain");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<4;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("decayPotionFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"stale_companion_acceptances\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
