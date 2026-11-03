package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Scene changes while pair ownership is pending, and actual delayed packet publication. */
public final class StaticNpcSceneLifecycleProbe extends StaticCancellationProbe {
 static void move(ClientSession pc,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance)throws Exception{StaticNpcSceneProbe.scene(pc,hub,npc,instance);}
 static void exerciseLifetime(ClientSession pc,ClientSession peer,GameHub hub,int mode)throws Exception{
  setup(pc,peer,hub,true);var npc=StaticNpcSceneProbe.fixtures.get(selectedNpc);targetId=npc.objectId();move(pc,hub,npc,11);move(peer,hub,npc,11);
  var rule=NativeCancellationNpcRules.lookup(selectedNpc);var row=BattleSpellCatalog.cancellationSpell(44);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,count+3);int mp=(int)get(pc,"playerMp");
  var failure=new AtomicReference<Throwable>();
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode<3){
    byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(5).c(3).d(targetId).bytes();var lock=pc.nativePlayerActionLock();
    var worker=new Thread(()->{try{need(NativeCancellationNpcStatic.route(pc,packet,row),"actual static route recognized");}catch(Throwable e){failure.set(e);}},"private-static-scene-wait");
    lock.lock();try{
     worker.start();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(worker.isAlive()&&!lock.hasQueuedThread(worker)&&System.nanoTime()<until)Thread.sleep(1);
     need(lock.hasQueuedThread(worker),"route captured actual scene before owner wait");
     if(mode<2){move(pc,hub,npc,22);if(mode==1)move(pc,hub,npc,11);}else set(pc,"playerHp",0);
    }finally{lock.unlock();worker.join(4000);}
    if(failure.get()!=null)throw new AssertionError(failure.get());need(!worker.isAlive(),"route wait completed");
    need((int)get(pc,"playerMp")==mp&&pc.nativeDeInventory(0).countCatalog(item)==count+3,"stale scene or dead caster spends nothing");
    need(NativeStaticNpcScene.gfx(hub,npc,peer)==npc.gfxId()&&wire.raw.size()==0&&other.raw.size()==0,"stale scene cannot transform or publish");
   }else{
    NativeSourcePlayerActionLock.run(pc,()->{try{
     cast(pc,44,row);need(NativeStaticNpcScene.gfx(hub,npc,pc)==rule.targetId()&&wire.raw.size()==0&&other.raw.size()==0,"committed before deferred output");
     if(mode==3)move(peer,hub,npc,22);
     else {
      move(pc,hub,npc,22);move(peer,hub,npc,22);need(get(hub,NativeStaticNpcScene.FIELD)==null,"old instance destroyed before output");
      move(pc,hub,npc,11);move(peer,hub,npc,11);
      need(NativeStaticNpcScene.transform(hub,npc,npc.gfxId(),rule.targetId(),pc),"new actor coincidentally same graphic");
      NativeStaticNpcScene.appearance(hub,npc,11,new NativeCancellationNpcStatic.Appearance(rule.nameId().isEmpty()?npc.nameId():rule.nameId(),rule.passispeed()>0?rule.passispeed():npc.passispeed()));
     }
    }catch(Throwable e){failure.set(e);}});
    if(failure.get()!=null)throw new AssertionError(failure.get());var mine=collect(wire);var peers=collect(other);String fx=HexFormat.of().formatHex(Protocol.effectObject(npc.objectId(),row.effectGfx()));
    need(!peers.contains(fx)&&(mode==3||!mine.contains(fx)),"no old scene effect delivered to moved recipient or recreated actor");
    if(mode==3)need(peers.isEmpty()&&mine.contains(fx),"other-instance recipient receives no old actor packets");
    need((int)get(pc,"playerMp")==mp-cost(pc,44,row)&&pc.nativeDeInventory(0).countCatalog(item)==3,"committed cast paid once despite recipient retirement");
   }
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeLightStoppedProbe.idleContext(),"no locked output or pending scope");
  }
  hub.leaveClassicWorld(pc);hub.leaveClassicWorld(peer);need(get(hub,NativeStaticNpcScene.FIELD)==null,"private lifecycle state drained");cases++;
 }
 public static void main(String[]args)throws Exception {
  before=false;var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class,NativeStaticNpcScene.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);StaticNpcSceneProbe.prepareNpcs();var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{71092,80013,81209})for(int mode=0;mode<5;mode++){selectedNpc=skill;
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
    exerciseLifetime(p.session,m.session,hub,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
