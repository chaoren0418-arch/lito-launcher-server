package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class AreaSleepSlowLifecycleProbe extends AreaSleepSlowProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int skill,int mode)throws Exception{
  CancellationMaterialProbe.setup(pc,peer,hub,world);probability(skill,100);((BattleTimedStatus)get(peer,"timedStatus")).clear();
  set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));set(pc,"elfAttribute",31);var row=spell(skill);int item=value(row,"itemConsumeId"),count=value(row,"itemConsumeCount"),expected=cost(pc,skill,row);
  NativeDeItemPublication.grant(pc,item,count+3);var failure=new AtomicReference<Throwable>();
  try(var self=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   boolean accepted=mode!=0&&!(mode==1||mode==2);
   var applied=new AtomicBoolean();if(mode<=2){
    var lock=peer.nativePlayerActionLock();var clean=new AtomicBoolean();var worker=new Thread(()->{try{castArea(pc,peer,skill,row,1,false);clean.set(NativeAreaMaterialAction.current.get()==null&&NativeLightStoppedProbe.idleContext());}catch(Throwable e){failure.set(e);}},"area-lifetime-wait");
    lock.lock();try{
     worker.start();long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(worker.isAlive()&&worker.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<limit)Thread.onSpinWait();
     need(worker.getState()==Thread.State.TIMED_WAITING,"actual all-player acquisition waits without holding partial locks");
     need((int)get(pc,"playerMp")==1000&&self.raw.size()==0&&other.raw.size()==0,"no speculative material/effect/output");
     if(mode==0)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
     if(mode==1)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
     if(mode==2){if(world)NativePlayerCommit.scene(peer,(int)get(peer,"classicWorldMapId")+1,"classicWorldMapId");else NativePlayerCommit.scene(peer,(long)get(peer,"currentRoomId")+1,"currentRoomId");}
    }finally{lock.unlock();worker.join(4000);}
    if(failure.get()!=null)throw new AssertionError(failure.get());need(!worker.isAlive()&&clean.get(),"retired acquisition releases contexts");need(!active(peer,skill),"retired target cannot receive old area spell");
   }else if(mode<=4){
    NativeSourcePlayerActionLock.run(pc,()->{try{castArea(pc,peer,skill,row,1,false);applied.set(active(peer,skill));need(self.raw.size()==0&&other.raw.size()==0,"area effects precede unlocked publication");var retired=mode==3?peer:pc;NativePlayerCommit.selected(retired,retired.selectedCharacterName());}catch(Throwable e){failure.set(e);}});
    if(failure.get()!=null)throw new AssertionError(failure.get());var a=collect(self);var b=collect(other);
    String effect=HexFormat.of().formatHex(Protocol.effectObject(peer.playerObjectId(),value(row,"effectGfx")));
    if(mode==3)need(b.isEmpty()&&!a.contains(effect),"retired recipient and retired target receive no stale effect");
    else need(a.isEmpty()&&b.contains(effect)==applied.get(),"target-owned output survives caster retirement");
   }else{
    set(peer,"learnedSkills",new HashSet<>(Set.of(skill)));set(peer,"elfAttribute",31);NativeDeItemPublication.grant(peer,item,count+3);var start=new CountDownLatch(1);var workers=new ArrayList<Thread>();
    for(var caster:List.of(pc,peer)){var target=caster==pc?peer:pc;var worker=new Thread(()->{try{start.await();castArea(caster,target,skill,row,1,false);}catch(Throwable e){failure.set(e);}});workers.add(worker);worker.start();}
    start.countDown();for(var worker:workers){worker.join(4000);need(!worker.isAlive(),"opposite area casts do not invert player locks");}if(failure.get()!=null)throw new AssertionError(failure.get());
    accepted=(int)get(pc,"playerMp")!=1000;need(accepted||(int)get(peer,"playerMp")!=1000,"at least one contested cast completes");
    long left=peer.nativeDeInventory(0).countCatalog(item);need(left==count+3||left==3,"contested peer material charged at most once");
   }
   need((int)get(pc,"playerMp")==1000-(accepted?expected:0)&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"admission and cost follow valid frozen membership mode="+mode);
   collect(self);collect(other);need(self.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"no locked publication or retained area receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception{before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeCastMaterial.class,NativeDeItemTransaction.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native handlers and material authority from requested JAR");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<6;mode++){var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("sleepFixturePercentileRoll",1,"massSlowFixturePercentileRoll",1,"massSlowFixtureDieRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,skill,mode);}need(!Files.exists(r.resolve("must-not-write.properties")),"both flags remain memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}need(NativePlayerLifetime.allIdle(),"native lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("self-material-wire.txt"),String.join("\n",wires)+"\n");Files.writeString(output.resolveSibling("self-material-goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output.resolveSibling("self-material-notes.txt"),String.join("\n",notes)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"self_skills\":2,\"material_skills\":2,\"Source_window_bypasses\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);}
}
