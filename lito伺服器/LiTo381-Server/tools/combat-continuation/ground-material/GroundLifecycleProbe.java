package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;import java.net.Socket;
/** Native coordinate ingress, owned admission lifetime and actual effect expiry/pulses. */
public final class GroundLifecycleProbe extends GroundMaterialProbe {
 static Object effects(ClientSession pc,GameHub hub,boolean world)throws Exception{
  if(!world)return get(room(pc,hub),"groundEffects");
  var values=((Map<?,?>)get(hub,"classicWorldGroundEffects")).values();need(values.size()==1,"one actual WORLD ground owner");return values.iterator().next();
 }
 static List<?> leases(Object effects)throws Exception{return (List<?>)get(effects,"leases");}
 static void expireAndPulse(ClientSession pc,ClientSession peer,GameHub hub,boolean world,long begin,long end)throws Exception{
  var effect=effects(pc,hub,world);var list=leases(effect);need(!list.isEmpty(),"actual placed lease available");var row=BattleSpellCatalog.groundSpell(selectedSkill);
  long duration=TimeUnit.SECONDS.toNanos(row.durationSeconds()),expiry=(long)get(list.get(0),"expiresAtNanos"),pulse=(long)get(list.get(0),"nextPulseNanos");
  need(expiry>=begin+duration&&expiry<=end+duration,"full original duration starts at committed placement");
  for(Object lease:list)need((long)get(lease,"expiresAtNanos")==expiry&&(int)get(lease,"skillId")==selectedSkill&&(long)get(lease,"casterObjectId")==pc.playerObjectId(),"all segments preserve caster, spell and expiry");
  int x=(int)get(list.get(0),"x"),y=(int)get(list.get(0),"y");
  if(world){var e=(ClassicWorldGroundEffects)effect;var actors=List.of(new ClassicWorldGroundEffects.Actor(peer.playerObjectId(),x,y,true,ClassicWorldGroundEffects.ActorKind.PLAYER),new ClassicWorldGroundEffects.Actor(pc.playerObjectId(),x,y,true,ClassicWorldGroundEffects.ActorKind.PLAYER));
   need(e.contains(selectedSkill,x,y,expiry-1),"WORLD lease exists before expiry");
   if(row.pulseIntervalMillis()>0&&pulse<expiry){need(e.advance(pulse-1,actors).isEmpty(),"no WORLD early pulse");var out=e.advance(pulse,actors);need(!out.isEmpty()&&out.stream().allMatch(p->p.targetObjectId()==peer.playerObjectId()&&p.casterObjectId()==pc.playerObjectId()),"WORLD exact pulse boundary preserves targets");need(e.advance(pulse,actors).isEmpty(),"no WORLD repeated pulse at same time");}
   need(e.advance(expiry,actors).isEmpty()&&e.activeCount(expiry)==0,"WORLD expires without last extra pulse");
  }else{var e=(BattleGroundEffects)effect;var actors=List.of(new BattleGroundEffects.Actor(peer.playerObjectId(),x,y,true,false),new BattleGroundEffects.Actor(pc.playerObjectId(),x,y,true,false));
   need(e.contains(selectedSkill,x,y,expiry-1),"battle lease exists before expiry");
   if(row.pulseIntervalMillis()>0&&pulse<expiry){need(e.advance(pulse-1,actors).isEmpty(),"no battle early pulse");var out=e.advance(pulse,actors);need(!out.isEmpty()&&out.stream().allMatch(p->p.targetObjectId()==peer.playerObjectId()&&p.casterObjectId()==pc.playerObjectId()),"battle exact pulse boundary preserves targets");need(e.advance(pulse,actors).isEmpty(),"no repeated battle pulse");}
   need(e.advance(expiry,actors).isEmpty()&&e.activeCount()==0,"battle expires without extra pulse");
  }
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.groundSpell(selectedSkill);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(item!=0)NativeDeItemPublication.grant(pc,item,count+3);
  int mpCost=cost(pc,selectedSkill,row);var error=new AtomicReference<Throwable>();var life=NativePlayerLifetime.of(pc);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode<=2){var lock=pc.nativePlayerActionLock();var clean=new AtomicBoolean();var worker=new Thread(()->{try{cast(pc,selectedSkill,row);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"ground-owned-cast");
    lock.lock();try{worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
     need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(life)==1,"native ground reserves receipt before own lock");need((int)get(pc,"playerMp")==1000&&active(pc,hub,world)==0&&wire.raw.size()==0,"no ground mutation before actor lock");
     if(mode==0)NativePlayerCommit.selected(pc,pc.selectedCharacterName());else if(mode==1)NativePlayerCommit.scene(pc,(int)get(pc,"classicWorldMapId")+1,"classicWorldMapId");else NativePlayerLifetime.shutdown(hub,false,0);
    }finally{lock.unlock();worker.join(3000);}if(error.get()!=null)throw new AssertionError(error.get());need(!worker.isAlive()&&clean.get(),"owned ground finishes cleanly");boolean accept=mode==2;
    need((int)get(pc,"playerMp")==1000-(accept?mpCost:0)&&(active(pc,hub,world)>0)==accept,"stale caster rejects; owned STOPPED receipt finishes once");if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accept?count:0),"stale or STOPPED material semantics");
    if(!accept)need(wire.raw.size()+other.raw.size()==0,"no retired actor output");else{int sent=wire.raw.size(),n=active(pc,hub,world);cast(pc,selectedSkill,row);need(wire.raw.size()==sent&&active(pc,hub,world)==n,"STOPPED blocks new unowned cast");}
   }else if(mode==3){var original=(Socket)get(pc,"socket");var fired=new AtomicBoolean();set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){
     public void write(byte[]b,int o,int n)throws IOException{if(fired.compareAndSet(false,true))throw new IOException("ground-output-failure");out.write(b,o,n);}public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
    try{try{cast(pc,selectedSkill,row);}catch(Throwable e){while(e.getCause()!=null)e=e.getCause();need(e instanceof IOException&&"ground-output-failure".equals(e.getMessage()),"original output failure retained");}}finally{set(pc,"socket",original);}
    need(fired.get()&&active(pc,hub,world)>0&&(int)get(pc,"playerMp")==1000-mpCost,"output failure retains committed ground and cost");int n=active(pc,hub,world),sent=wire.raw.size();cast(pc,selectedSkill,row);need(active(pc,hub,world)==n&&wire.raw.size()==sent,"output failure cannot reopen admission");
   }else if(mode==4){long begin=System.nanoTime();cast(pc,selectedSkill,row);long end=System.nanoTime();expireAndPulse(pc,peer,hub,world,begin,end);
   }else{var start=new CountDownLatch(1);var done=new CountDownLatch(2);Runnable run=()->{try{start.await();cast(pc,selectedSkill,row);}catch(Throwable e){error.set(e);}finally{done.countDown();}};var a=new Thread(run,"ground-duplicate-A");var b=new Thread(run,"ground-duplicate-B");a.start();b.start();start.countDown();need(done.await(4,TimeUnit.SECONDS),"concurrent ground packets finish");a.join();b.join();if(error.get()!=null)throw new AssertionError(error.get());need((int)get(pc,"playerMp")==1000-mpCost,"concurrent native packets pay exactly once");if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==3,"concurrent packets consume one material");int expected=world?new ClassicWorldGroundEffects().cast(row,pc.playerObjectId(),(int)get(pc,"positionX"),(int)get(pc,"positionY"),targetX,targetY,ClassicWorldMapCatalog.forMap((int)get(pc,"classicWorldMapId")),System.nanoTime()).points().size():new BattleGroundEffects().cast(row,pc.playerObjectId(),(int)get(pc,"positionX"),(int)get(pc,"positionY"),targetX,targetY,ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc)),System.nanoTime()).points().size();need(active(pc,hub,world)==expected,"one original ground pattern from two concurrent packets");}
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(life)==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&NativeLightStoppedProbe.idleContext(),"outputs unlocked, receipts and contexts drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,GameHub.class,BattleGroundEffects.class,ClassicWorldGroundEffects.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native producer and effect classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<6;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"full_goal_achieved\":false}";Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
