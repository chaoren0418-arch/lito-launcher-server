package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;import java.net.Socket;
/** Capture owner/entity before the earliest native dispatcher wait; guard deferred removals. */
public class ReturnNatureLifecycleProbe extends ReturnNatureMaterialProbe {
 static boolean clean()throws Exception{
  for(String name:List.of("ingress","current")){var f=NativeReturnNatureMaterialAction.class.getDeclaredField(name);f.setAccessible(true);if(((ThreadLocal<?>)f.get(null)).get()!=null)return false;}
  return NativeLightStoppedProbe.idleContext();
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var pet=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2,false);targetId=pet.snapshot().objectId();var row=BattleSpellCatalog.returnToNatureSpell(145);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount(),mpCost=cost(pc,145,row);NativeDeItemPublication.grant(pc,item,count+3);
  var failure=new AtomicReference<Throwable>();var done=new AtomicBoolean();var life=NativePlayerLifetime.of(pc);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode<=5){
    var lock=pc.nativePlayerActionLock();Thread worker=new Thread(()->{try{cast(pc);done.set(clean());}catch(Throwable e){failure.set(e);}},"return-nature-queued");BattleCompanion replacement=null;
    lock.lock();try{
     worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
     need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"native ingress reserves before waiting");need((int)get(pc,"playerMp")==1000&&pet.snapshot().active()&&wire.raw.size()==0,"no mutation while queued");
     if(mode==1)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
     if(mode==2){((List<BattleCompanion>)get(peer,"tamedCompanions")).remove(pet);replacement=companion(peer,pet.snapshot().x(),pet.snapshot().y(),false);}
     if(mode==3)set(peer,NativeDeItemApplication.DISCONNECTED,true);
     if(mode==4)pet.applyDamage(Integer.MAX_VALUE);
     if(mode==5)NativePlayerLifetime.shutdown(hub,false,0);
    }finally{lock.unlock();worker.join(3000);}
    if(failure.get()!=null)throw new AssertionError("queued original ingress",failure.get());need(!worker.isAlive()&&done.get(),"worker contexts drained");boolean accepted=mode==0||mode==5;
    need((int)get(pc,"playerMp")==1000-(accepted?mpCost:0)&&pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"only valid original target or owned STOPPED cast pays");
    need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(145)==accepted,"reuse follows acceptance");
    if(replacement!=null)need(replacement.snapshot().active(),"same-ID replacement remains active");
    if(!accepted)need(wire.raw.size()==0&&other.raw.size()==0,"stale target produces no packets");else need(!pet.snapshot().active(),"accepted release completes");
    if(mode==5){int bytes=wire.raw.size();cast(pc);need(wire.raw.size()==bytes&&(int)get(pc,"playerMp")==1000-mpCost,"new STOPPED producer denied");}
   }else if(mode==6||mode==7){
    NativeSourcePlayerActionLock.runPairOrdered(pc,peer,()->{try{
     cast(pc);need(!pet.snapshot().active()&&wire.raw.size()==0&&other.raw.size()==0,"accepted retirement before deferred publication");
     if(mode==6)companion(peer,pet.snapshot().x(),pet.snapshot().y(),false);else NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    }catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new AssertionError(failure.get());
    var self=collect(wire);var peers=collect(other);String remove=HexFormat.of().formatHex(Protocol.removeObject(pet.snapshot().objectId()));String effect=HexFormat.of().formatHex(Protocol.effectObject(pet.snapshot().objectId(),row.effectGfx()));
    need(!self.contains(remove)&&!peers.contains(remove)&&!self.contains(effect)&&!peers.contains(effect),"old removal cannot erase replacement or retired owner view");
    need(self.contains(HexFormat.of().formatHex(Protocol.manaPoint(1000-mpCost,1000))),"caster receives committed MP");
   }else{
    var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true))throw new IOException("return-nature-output-failure");out.write(b,o,n);}public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
    try{try{cast(pc);}catch(Throwable error){while(error.getCause()!=null)error=error.getCause();need(error instanceof IOException&&error.getMessage().equals("return-nature-output-failure"),"original socket failure");}}finally{set(pc,"socket",original);}
    need(fired.get()&&!pet.snapshot().active()&&(int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"send failure does not undo accepted retirement or cost");
   }
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty()&&clean(),"unlocked output; both queues/receipts/contexts drain");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<9;mode++){
   var r=root.resolve(world+"-"+app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only fixture");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
