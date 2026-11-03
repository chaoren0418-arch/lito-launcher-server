package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;import java.net.Socket;
/** Actual WORLD actor generation, role retirement and scoped output. */
public class CancellationWorldNpcLifecycleProbe extends CancellationWorldNpcProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,true);var npc=monster(pc,hub);targetId=npc.objectId();var pop=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");var row=BattleSpellCatalog.cancellationSpell(44);
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,count+3);int mpCost=cost(pc,44,row);
  var error=new AtomicReference<Throwable>();var life=NativePlayerLifetime.of(pc);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)) {
   if(mode<=2||mode==4) {
    var lock=pc.nativePlayerActionLock();var clean=new AtomicBoolean();var worker=new Thread(()->{try{cast(pc,44,row);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"NPC-cancellation-lifetime");
    lock.lock();try {
     worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
     need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"actual cast waits with owned receipt");
     need((int)get(pc,"playerMp")==1000&&!active(hub,pc,npc.objectId(),44)&&wire.raw.size()==0,"no speculative mutation");
     if(mode==0)NativePlayerCommit.selected(pc,pc.selectedCharacterName());if(mode==1)NativePlayerCommit.scene(pc,npc.mapId()+1,"classicWorldMapId");if(mode==2)NativePlayerLifetime.shutdown(hub,false,0);
     if(mode==4){pop.damage(npc.mapId(),npc.objectId(),Integer.MAX_VALUE);need(pop.removeCorpse(npc.mapId(),npc.objectId())!=null&&pop.respawn(npc.mapId(),npc.objectId())!=null,"actual actor death/retirement/respawn");need(pop.haste(npc.mapId(),npc.objectId(),System.nanoTime(),60),"new actor lifetime haste");}
    }finally{lock.unlock();worker.join(3000);}
    if(error.get()!=null)throw new AssertionError("queued actual NPC cast",error.get());need(!worker.isAlive()&&clean.get(),"queued contexts released");boolean accepted=mode==2;
    need((int)get(pc,"playerMp")==1000-(accepted?mpCost:0)&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"only retained same-generation admission costs material mode="+mode);
    need(!pop.hasteActive(npc.mapId(),npc.objectId(),System.nanoTime())==accepted,"new NPC lifetime cannot receive stale cancellation");
    if(!accepted)need(wire.raw.size()==0&&other.raw.size()==0,"retired cast emits no packets");
    if(mode==2){int bytes=wire.raw.size();cast(pc,44,row);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mpCost,"STOPPED refuses new receipt");}
   }else if(mode==3){
    NativeSourcePlayerActionLock.run(pc,()->{try{cast(pc,44,row);need(active(hub,pc,npc.objectId(),44)&&wire.raw.size()==0&&other.raw.size()==0,"committed NPC before deferred output");need(pop.nativeCancellationDelete(npc.mapId(),npc.objectId())!=null,"retire NPC before publication");NativePlayerCommit.selected(peer,peer.selectedCharacterName());}catch(Throwable e){error.set(e);}});
    if(error.get()!=null)throw new AssertionError(error.get());var frames=collect(wire);String fx=HexFormat.of().formatHex(Protocol.effectObject(npc.objectId(),row.effectGfx()));
    need(!frames.contains(fx)&&other.raw.size()==0,"no retired NPC effect or retired viewer output");need(frames.contains(HexFormat.of().formatHex(Protocol.manaPoint(1000-mpCost,1000))),"caster still receives committed MP");
   }else if(mode==5){
    var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){public void write(byte[]b,int off,int len)throws IOException{if(fired.compareAndSet(false,true))throw new IOException("NPC-output-failure");out.write(b,off,len);}public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
    try{try{cast(pc,44,row);}catch(Throwable e){while(e.getCause()!=null)e=e.getCause();need(e instanceof IOException&&"NPC-output-failure".equals(e.getMessage()),"original socket failure");}}finally{set(pc,"socket",original);}
    need(fired.get()&&active(hub,pc,npc.objectId(),44)&&(int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"output failure preserves committed NPC and cost");int bytes=wire.raw.size();cast(pc,44,row);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mpCost,"no replay after socket failure");
   }else{
    set(peer,"learnedSkills",new HashSet<>(Set.of(44)));NativeDeItemPublication.grant(peer,item,count+3);int peerCost=cost(peer,44,row);var start=new CountDownLatch(1);var done=new CountDownLatch(2);var workers=new ArrayList<Thread>();
    for(var caster:List.of(pc,peer)){var worker=new Thread(()->{try{start.await();cast(caster,44,row);}catch(Throwable e){error.set(e);}finally{done.countDown();}},"NPC-competing-caster");workers.add(worker);worker.start();}
    start.countDown();need(done.await(4,TimeUnit.SECONDS),"shared NPC casts finish without inversion");for(var worker:workers)worker.join();if(error.get()!=null)throw new AssertionError(error.get());
    need(active(hub,pc,npc.objectId(),44)&&(int)get(pc,"playerMp")==1000-mpCost&&(int)get(peer,"playerMp")==1000-peerCost&&pc.nativeDeInventory(0).countCatalog(item)==3&&peer.nativeDeInventory(0).countCatalog(item)==3,"both valid casts pay once, existing status clears once");
   }
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty()&&NativeLightStoppedProbe.idleContext(),"unlocked publication and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<7;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }

}
