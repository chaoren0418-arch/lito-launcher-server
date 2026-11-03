package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual native/direct replies queued across role, scene, disconnect and STOPPED transitions. */
public final class ResurrectionAnswerRaceProbe extends ResurrectionMaterialProbe {
 static int nativeGaps,directGaps;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);int change=mode/2;boolean nativeEntry=mode%2==0;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   cast(pc,skill,row);need(active(peer,skill),"actual cast installs question before queued reply");
   int question=(int)call(row,"questionId",new Class[]{});int priorSelf=wire.raw.size(),priorPeer=other.raw.size(),mp=(int)get(pc,"playerMp");
   // Hold only the actor whose transition is injected. Holding the target lock
   // while selecting the caster would itself invert the Source pair lock order.
   var lock=(change==1||change==3?pc:peer).nativePlayerActionLock();var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var clean=new java.util.concurrent.atomic.AtomicBoolean();
   Thread worker=new Thread(()->{try{if(nativeEntry)answer(peer,question,1);else call(peer,"answerResurrection",new Class[]{Protocol.AskAnswer.class},new Protocol.AskAnswer(question,1,""));clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"resurrection-answer-role-race");
   lock.lock();try{
    worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
    while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
    need(lock.hasQueuedThread(worker),"actual native/direct reply waits for ordered target lock");
    need((int)get(peer,"playerHp")==0&&active(peer,skill)&&wire.raw.size()==priorSelf&&other.raw.size()==priorPeer,"queued reply has no premature HP or output");
    if(change==1)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
    else if(change==2)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    else if(change==3)set(pc,NativeDeItemApplication.DISCONNECTED,true);
    else if(change==4)NativePlayerCommit.scene(peer,(int)get(peer,"classicWorldMapId")+1,"classicWorldMapId");
    else if(change==5)NativePlayerLifetime.shutdown(hub,false,0);
    else if(change==6)peer.nativeRejectResurrectionQuestion();
   }finally{lock.unlock();worker.join(3000);}
   if(error.get()!=null)throw new AssertionError("queued original reply",error.get());
   need(!worker.isAlive()&&clean.get(),"reply finished and thread-local contexts clean");
   boolean accepted=change==0||change==5;
   need(((int)get(peer,"playerHp")>0)==accepted,"role/scene/disconnect/cleared reply rejected; already owned STOPPED receipt commits");
   need(!active(peer,skill)&&get(peer,"nativeResurrectionAnswerLease")==null,"completed or rejected pending question and lease released");
   need((int)get(pc,"playerMp")==mp,"answer never repeats caster debit");
   int bytes=other.raw.size();answer(peer,question,1);need(other.raw.size()==bytes,"repeat answer cannot resurrect or publish twice");
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty(),"both lifetime receipts, deferred outputs and queues drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<14;mode++){selectedSkill=skill;selectedMode=mode;
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
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"stale_native_acceptances\":"+nativeGaps+",\"stale_direct_acceptances\":"+directGaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
