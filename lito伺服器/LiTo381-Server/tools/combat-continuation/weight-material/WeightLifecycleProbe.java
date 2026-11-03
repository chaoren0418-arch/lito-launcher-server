package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;import java.net.Socket;
/** Self actor ownership, accepted STOPPED receipt and deferred packet lifetime. */
public final class WeightLifecycleProbe extends WeightMaterialProbe {
 static void send(ClientSession pc)throws Exception{cast(pc,14,spell(14));}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);set(pc,"learnedSkills",new HashSet<>(Set.of(14)));var weight=(BattleWeightStatus)get(pc,"weightStatus");weight.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var old=(CombatDatabase.Snapshot)field.get(null);int item=old.skills().get(44).reagent(),count=2;
  var skills=new HashMap<>(old.skills());skills.put(14,copy(skills.get(14),Map.of("reagent",item,"reagentCount",count)));field.set(null,old.withSkills(skills));
  try{
   var row=spell(14);NativeDeItemPublication.grant(pc,item,count+3);int mp=cost(pc,14,row),hp=(int)call(row,"hpCost",new Class[]{});var error=new AtomicReference<Throwable>();var life=NativePlayerLifetime.of(pc);
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    if(mode<=2){
     var lock=pc.nativePlayerActionLock();var clean=new AtomicBoolean();var worker=new Thread(()->{try{send(pc);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"weight-self-lifetime");
     lock.lock();try{worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
      need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"real self ingress owns one queued receipt");
      need((int)get(pc,"playerMp")==1000&&(int)get(pc,"playerHp")==4000&&!weight.snapshot(System.nanoTime()).decreaseWeightActive()&&wire.raw.size()==0,"no mutation before lock");
      if(mode==0)NativePlayerCommit.selected(pc,pc.selectedCharacterName());else if(mode==1)NativePlayerCommit.scene(pc,(int)get(pc,"classicWorldMapId")+1,"classicWorldMapId");else NativePlayerLifetime.shutdown(hub,false,0);
     }finally{lock.unlock();worker.join(3000);}
     if(error.get()!=null)throw new AssertionError("queued weight cast",error.get());need(!worker.isAlive()&&clean.get(),"worker and contexts drained");boolean accepted=mode==2;
     need((int)get(pc,"playerMp")==1000-(accepted?mp:0)&&(int)get(pc,"playerHp")==4000-(accepted?hp:0)&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"only preaccepted STOPPED receipt pays once");
     need(weight.snapshot(System.nanoTime()).decreaseWeightActive()==accepted,"effect follows actor binding");
     if(!accepted)need(wire.raw.size()==0&&other.raw.size()==0,"retired actor emits nothing");else{int bytes=wire.raw.size();send(pc);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mp,"new STOPPED work denied");}
    }else if(mode==3){
     NativeSourcePlayerActionLock.run(pc,()->{try{send(pc);need(weight.snapshot(System.nanoTime()).decreaseWeightActive()&&wire.raw.size()==0&&other.raw.size()==0,"state commit before deferred publication");NativePlayerCommit.selected(pc,pc.selectedCharacterName());}catch(Throwable e){error.set(e);}});
     if(error.get()!=null)throw new AssertionError(error.get());need(wire.raw.size()==0&&other.raw.size()==0,"all old-role self and peer output suppressed");
    }else{
     var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){
      public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true))throw new IOException("weight-output-failure");out.write(b,o,n);}public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
     try{try{send(pc);}catch(Throwable e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();need(cause instanceof IOException&&"weight-output-failure".equals(cause.getMessage()),"exact socket failure");}}finally{set(pc,"socket",original);}
     need(fired.get()&&weight.snapshot(System.nanoTime()).decreaseWeightActive()&&(int)get(pc,"playerMp")==1000-mp&&(int)get(pc,"playerHp")==4000-hp&&pc.nativeDeInventory(0).countCatalog(item)==3,"socket failure never repeats accepted resource debit");
     int bytes=wire.raw.size();send(pc);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mp,"failed output does not reopen action window");
    }
    collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&NativeLightStoppedProbe.idleContext(),"unlocked output and no retained receipts or context");
   }
  }finally{field.set(null,old);}cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native producer");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{14})for(int mode=0;mode<5;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
