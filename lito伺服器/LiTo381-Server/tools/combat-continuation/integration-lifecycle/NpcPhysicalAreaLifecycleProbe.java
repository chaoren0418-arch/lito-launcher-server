package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.net.*;import java.io.*;
/** Real box skill with a second player and companion, held locks and lifecycle replacement. */
public final class NpcPhysicalAreaLifecycleProbe extends ReturnNatureMaterialProbe {
 static int mode,areaCases,areaFaults;static final List<String> rows=new ArrayList<>();
 static void captured(Thread worker)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
  while(worker.isAlive()&&System.nanoTime()<end){for(var frame:worker.getStackTrace())if(frame.getClassName().endsWith("NativeNpcWeaponBreakAreaActions")&&frame.getMethodName().equals("run")&&worker.getState()==Thread.State.TIMED_WAITING)return;Thread.yield();}
  throw new AssertionError("physical area did not reach bounded lock acquisition");
 }
 @SuppressWarnings("unchecked") static void area(NpcWeaponBreakHpProbe.Scenario s)throws Exception{
  var pc=s.pc();var peer=s.peer();var chars=get(peer,"characters");int peerMap=(int)get(peer,"classicWorldMapId");
  var pet=BattleCompanion.tamed(0x5f0000a1L,45065,3989,"area-old",1,10000,10000,0,10,20,1,s.attack().targetX(),s.attack().targetY());pet.qualifyNpcDefense(new SourceNpcPhysicalStats(45065,10,false,0,false));
  var pets=(List<BattleCompanion>)get(peer,"tamedCompanions");pets.add(pet);BattleCompanion replacement=null;
  need(peer.classicWorldCompanionSnapshots().stream().anyMatch(n->n.objectId()==pet.snapshot().objectId()),"owned tamed companion is an actual WORLD area candidate: "+pet.snapshot());
  var skill=new ClassicWorldMonsterPhysicalSkillCatalog.Definition(s.npc().npcId(),0,100,-2,0,2,1,2,15,0,30);
  need(skill.containsBoxTarget(s.npc().x(),s.npc().y(),s.npc().heading(),s.attack().targetX(),s.attack().targetY()),"actual rotated Source box contains both players and pet");
  need(skill.containsBoxTarget(s.npc().x(),s.npc().y(),s.npc().heading(),pet.snapshot().x(),pet.snapshot().y()),"pet coordinates in rotated box: "+pet.snapshot());
  var attack=new ClassicWorldMonsterPopulation.PhysicalSkillAttack(s.npc(),pc.playerObjectId(),s.attack().targetX(),s.attack().targetY(),skill,0,false);
  if(mode==0){var context=new NativeAreaMaterialAction.Context(pc);NativeAreaMaterialAction.owned(context,()->{
   need(NativeNpcWeaponBreakAreaActions.companion(peer,pet.snapshot().objectId())==pet,"captured exact pet is eligible before the physical body");
   need(CombatAccess.call(peer,"classicWorldMonsterPhysicalTarget","(J)Llocal/lito381/server/ClientSession$ClassicWorldMonsterPhysicalTarget;",pet.snapshot().objectId())!=null,"real physical target resolves captured pet");
   var targets=(List<?>)CombatAccess.call(pc,"classicWorldMonsterPhysicalAreaTargets","(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPhysicalSkillCatalog$Definition;)Ljava/util/List;",s.npc(),skill);
   need(targets.stream().anyMatch(n->((Number)CombatAccess.field(n,"objectId")).longValue()==pet.snapshot().objectId()),"native box includes pet: "+targets+" snapshot="+pet.snapshot());
  });}
  int initial=(int)get(pc,"playerHp"),otherInitial=(int)get(peer,"playerHp");var failure=new AtomicReference<Throwable>();var clean=new AtomicBoolean();var fired=new AtomicBoolean();var logged=new AtomicInteger();
  var logger=java.util.logging.Logger.getLogger("");var handler=new java.util.logging.Handler(){public void publish(java.util.logging.LogRecord r){var e=r.getThrown();while(e!=null&&e.getCause()!=null)e=e.getCause();if(e instanceof IOException&&String.valueOf(e.getMessage()).contains("fixture area output failure"))logged.incrementAndGet();}public void flush(){}public void close(){}};
  if(mode==9)logger.addHandler(handler);
  try(var wa=new SourceActionPairProbe.Wire(pc,peer);var wb=new SourceActionPairProbe.Wire(peer,pc)){
   Socket original=(Socket)get(pc,"socket");if(mode==9||mode==12)set(pc,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(original.getOutputStream()){public void write(byte[] b,int o,int n)throws IOException{if(fired.compareAndSet(false,true)){if(mode==9)throw new IOException("fixture area output failure");NativePlayerCommit.advance(peer);}out.write(b,o,n);}public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});
   var worker=new Thread(()->{try{pc.serverClassicWorldMonsterPhysicalSkillAttack(attack);}catch(Throwable e){failure.set(e);}finally{try{clean.set(NpcWeaponActionLifecycleProbe.contexts()&&NativeAreaMaterialAction.current.get()==null);}catch(Exception e){failure.compareAndSet(null,e);}}},"NPC-physical-area-life");
   var lock=peer.nativePlayerActionLock();boolean waiting=mode>=1&&mode<=8||mode==10||mode==11;if(waiting)lock.lock();
   try{
    worker.start();if(waiting){captured(worker);
     if(mode==1)NativePlayerCommit.advance(peer);
     if(mode==2)NativePlayerCommit.advance(pc);
     if(mode==3)s.pop().damage(s.npc().mapId(),s.npc().objectId(),Integer.MAX_VALUE);
     if(mode==4){s.pop().damage(s.npc().mapId(),s.npc().objectId(),Integer.MAX_VALUE);s.pop().removeCorpse(s.npc().mapId(),s.npc().objectId());need(s.pop().respawn(s.npc().mapId(),s.npc().objectId())!=null,"same NPC ID has a new generation");}
     if(mode==5){worker.join(2000);need(!worker.isAlive(),"bounded retry aborts while peer lock stays busy");}
     if(mode==6)worker.interrupt();
     if(mode==7){var old=(List<CharacterSummary>)chars;set(peer,"characters",List.of(copy(old.get(0),Map.of("objectId",old.get(0).objectId()+13))));}
     if(mode==8)set(peer,"classicWorldMapId",400);
     if(mode==10){pet.deactivate();pets.remove(pet);replacement=BattleCompanion.tamed(pet.snapshot().objectId(),45065,3989,"area-new",1,10000,10000,0,10,20,1,s.attack().targetX(),s.attack().targetY());replacement.qualifyNpcDefense(new SourceNpcPhysicalStats(45065,10,false,0,false));pets.add(replacement);}
     if(mode==11)NativeCompanionCallbacks.advance(pet);
    }
   }finally{if(waiting)lock.unlock();worker.join(5000);if(mode==9||mode==12)set(pc,"socket",original);}
   need(!worker.isAlive()&&clean.get(),"area producer releases every existing context");NpcWeaponActionLifecycleProbe.await(s);
   if(mode==9){need(fired.get()&&(failure.get() instanceof IOException||logged.get()>0),"actual area I/O fault reaches native error handler");areaFaults++;}else if(failure.get()!=null)throw new AssertionError("area callback",failure.get());
   boolean cancelled=mode>=2&&mode<=6;
   need(((int)get(pc,"playerHp")==initial)==cancelled,"primary HP follows original caster/NPC lifecycle mode="+mode);
   boolean peerExcluded=cancelled||mode==1||mode==7||mode==8;
   if(mode!=12)need(((int)get(peer,"playerHp")==otherInitial)==peerExcluded,"cross-player HP uses frozen role/scene mode="+mode);
   boolean petExcluded=peerExcluded||mode==10||mode==11;
   need((pet.snapshot().hp()==10000)==petExcluded,"captured companion lifecycle mode="+mode);
   if(replacement!=null)need(replacement.snapshot().hp()==10000,"same-ID replacement companion never inherits old area hit");
   if(cancelled)need(wa.raw.size()==0&&wb.raw.size()==0,"retired action has no packets");
   // Source publishes current visible players' overhead HP independently after accepted HP.
   // A newly bound viewer may receive that live primary-player meter, never the frozen area animation or its own old HP.
   if(mode==1||mode==7||mode==8){var expected=HexFormat.of().formatHex(Protocol.hitRatio(pc.playerObjectId(),(int)get(pc,"playerHp"),(int)get(pc,"playerMaxHp")));var output=collect(wb);need(output.stream().allMatch(expected::equals),"stale peer receives no old area/self packets; only live primary HP visibility mode="+mode+" frames="+output);}
   need(wa.locked+wb.locked==0&&NativePlayerCommit.pending()==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"all paired locks released before output and all receipts drained");
   rows.add("mode="+mode+" primary="+get(pc,"playerHp")+" peer="+get(peer,"playerHp")+" pet="+pet.snapshot().hp()+" bytes="+wa.raw.size()+","+wb.raw.size());areaCases++;
  }finally{if(mode==9)logger.removeHandler(handler);set(peer,"characters",chars);set(peer,"classicWorldMapId",peerMap);NativePlayerCommit.advance(peer);pet.deactivate();pets.remove(pet);if(replacement!=null){replacement.deactivate();pets.remove(replacement);}}
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var c:List.of(ClientSession.class,NativeNpcWeaponBreakAreaActions.class,NativeAreaMaterialAction.Context.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual area candidate classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);NpcWeaponBreakHpProbe.scenarioHook=NpcPhysicalAreaLifecycleProbe::area;
  try{for(boolean app:new boolean[]{false,true}){
   var r=root.resolve(""+app);Files.createDirectories(r);var changes=new HashMap<String,Object>();changes.put("battlePhysicalHitEnabled",false);changes.put("uncannyDodgeHitFixtureD20",20);changes.put("uncannyDodgeHitFixtureD100",1);changes.put("classicWorldMonsterPhysicalSkillFixtureLevelRoll",0);changes.put("classicWorldMonsterPhysicalSkillFixtureCriticalRoll",99);changes.put("battleNaturalHpRegenEnabled",false);changes.put("battleNaturalMpRegenEnabled",false);changes.put("statePersistenceEnabled",true);changes.put("stateFile",r.resolve("must-not-write.properties"));var cfg=copy(base,changes);
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(NpcPhysicalAreaLifecycleProbe.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats"))return new SourceNpcPhysicalStats((int)a[0],10,false,0,false);try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);for(mode=0;mode<13;mode++)NpcWeaponBreakHpProbe.exercise(p.session,m.session,hub,capture,1,NpcWeaponBreakHpProbe.fractionalWitness());
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only fixture");
  }}finally{NpcWeaponBreakHpProbe.scenarioHook=null;NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"all role lifetimes idle");Files.write(Path.of(args[2]).resolveSibling("cases.txt"),rows);Files.writeString(Path.of(args[2]),"{\"cases\":"+areaCases+",\"checks\":"+ownChecks+",\"IO_faults\":"+areaFaults+",\"full_goal_achieved\":false}\n");
 }
}
