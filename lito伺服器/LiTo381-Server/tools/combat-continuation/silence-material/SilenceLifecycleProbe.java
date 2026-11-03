package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.net.Socket;

/** Pair-ordered admission, role lifetime, stopped receipts and original socket failure. */
public final class SilenceLifecycleProbe extends SilenceMaterialProbe {
 static void send(ClientSession pc,ClientSession target)throws Exception{
  var row=spell(selectedSkill);byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((selectedSkill-1)>>3).c((selectedSkill-1)&7).d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY")).bytes();
  call(pc,"handle",new Class[]{byte[].class},packet);
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=spell(selectedSkill);probability(selectedSkill);int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});
  NativeDeItemPublication.grant(pc,item,count+3);
  int mpCost=cost(pc,selectedSkill,row);var error=new AtomicReference<Throwable>();var life=NativePlayerLifetime.of(pc);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode<=2||mode==4){
    var lock=(mode==0||mode==1?peer:pc).nativePlayerActionLock();var clean=new AtomicBoolean();
    var worker=new Thread(()->{try{send(pc,peer);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"water-life-pair-lifetime");
    lock.lock();try{
     worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
     while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
     need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"actual target producer reserves and waits for ordered player lock");
     need((int)get(pc,"playerMp")==1000&&!active(peer,selectedSkill)&&wire.raw.size()==0,"no mutation before pair acquisition");
     if(mode==0)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
     else if(mode==1)NativePlayerCommit.scene(peer,(int)get(peer,"classicWorldMapId")+1,"classicWorldMapId");
     else if(mode==2)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
     else NativePlayerLifetime.shutdown(hub,false,0);
    }finally{lock.unlock();worker.join(3000);}
    if(error.get()!=null)throw new AssertionError("actual queued target cast",error.get());need(!worker.isAlive()&&clean.get(),"worker completes with clean contexts");
    boolean accepted=mode==4;
    need((int)get(pc,"playerMp")==1000-(accepted?mpCost:0)&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"stale role/scene rejected; owned STOPPED receipt commits once");
    need(active(peer,selectedSkill)==accepted,"target effect follows retained or retired admission");
    if(!accepted)need(wire.raw.size()==0&&other.raw.size()==0,"retired cast emits no old-role packets");
    else {int bytes=wire.raw.size();send(pc,peer);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mpCost,"new unowned cast denied after STOPPED");}
   }else if(mode==3){
    NativeSourcePlayerActionLock.runPairOrdered(pc,peer,()->{try{
     send(pc,peer);need(active(peer,selectedSkill)&&wire.raw.size()==0&&other.raw.size()==0,"accepted target state before deferred output");
     NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    }catch(Throwable e){error.set(e);}});
    if(error.get()!=null)throw new AssertionError(error.get());
    var self=collect(wire);String effect=HexFormat.of().formatHex(Protocol.effectObject(peer.playerObjectId(),(int)call(row,"effectGfx",new Class[]{})));
    need(!self.contains(effect)&&other.raw.size()==0,"no target effect or delivery to retired recipient");
    need(self.contains(HexFormat.of().formatHex(Protocol.manaPoint(1000-mpCost,1000))),"caster still receives committed MP after target retirement");
   }else if(mode==5){
    var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();
    set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){
     public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true))throw new IOException("water-life-output-failure");out.write(b,o,n);}
     public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
    try{try{send(pc,peer);}catch(Throwable e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();need(cause instanceof IOException&&"water-life-output-failure".equals(cause.getMessage()),"original socket error retained");}}finally{set(pc,"socket",original);}
    need(fired.get()&&active(peer,selectedSkill)&&(int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"socket failure preserves accepted state and cost once");
    int bytes=wire.raw.size();send(pc,peer);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mpCost,"failed output cannot reopen action window");
   }else{
    set(peer,"learnedSkills",new HashSet<>(Set.of(selectedSkill)));NativeDeItemPublication.grant(peer,item,count+3);
    int peerCost=cost(peer,selectedSkill,row);var start=new CountDownLatch(1);var done=new CountDownLatch(2);
    Thread a=new Thread(()->{try{start.await();send(pc,peer);}catch(Throwable e){error.set(e);}finally{done.countDown();}},"water-life-forward");
    Thread b=new Thread(()->{try{start.await();send(peer,pc);}catch(Throwable e){error.set(e);}finally{done.countDown();}},"water-life-reverse");
    a.start();b.start();start.countDown();need(done.await(4,TimeUnit.SECONDS),"opposite target casts finish without lock inversion");
    if(error.get()!=null)throw new AssertionError(error.get());a.join();b.join();
    boolean aApplied=active(peer,selectedSkill),bApplied=active(pc,selectedSkill);need(aApplied||bApplied,"at least one concurrent Silence is admitted; later caster may already be silenced");
    // Source L1Character.commitCurrentMpLocked bounds signed consumption to [0,maxMp].
    need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000-(aApplied?mpCost:0),1000))&&(int)get(peer,"playerMp")==Math.max(0,Math.min(1000-(bApplied?peerCost:0),1000))&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(aApplied?count:0)&&peer.nativeDeInventory(0).countCatalog(item)==count+3-(bApplied?count:0),"each actor pays independent Source cost with original MP bounds");
   }
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty()&&NativeLightStoppedProbe.idleContext(),"pair outputs unlocked and both queues, receipts, contexts drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native producer");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<7;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("silenceFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
