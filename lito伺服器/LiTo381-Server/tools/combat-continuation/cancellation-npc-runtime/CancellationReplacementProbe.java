package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real C_USE_SPELL, population and deferred wire. Only the scheduler is controllable. */
public class CancellationReplacementProbe extends CancellationWorldNpcProbe {
 static final class Clock extends ScheduledThreadPoolExecutor {
  Runnable tick;ScheduledFuture<?> scheduled;
  final boolean real;Clock(){this(false);}Clock(boolean real){super(1);this.real=real;setRemoveOnCancelPolicy(true);}
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,long initial,long delay,TimeUnit unit){
   need(initial==1&&delay==1&&unit==TimeUnit.SECONDS,"bounded real cleanup cadence");tick=command;
   scheduled=super.scheduleWithFixedDelay(real?command:()->{},real?initial:1,real?delay:1,real?unit:TimeUnit.DAYS);return scheduled;
  }
  void fire(){if(tick!=null)tick.run();}
 }
 static ClassicWorldMonsterCatalog.MonsterTemplate rawTemplate(NativeDeNpcTemplate n){
  return new ClassicWorldMonsterCatalog.MonsterTemplate(n.get_npcId(),n.get_nameid(),n.getImpl(),n.get_gfxid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_ac(),n.get_str(),n.get_con(),n.get_dex(),n.get_wis(),n.get_int(),n.get_mr(),n.get_exp(),n.get_lawful(),n.get_size(),n.get_weakAttr(),n.get_ranged(),n.get_passispeed(),n.get_atkspeed(),n.getBowActId(),n.get_undead(),n.get_poisonatk(),n.get_paralysisatk(),n.is_agro(),n.get_damagereduction(),n.is_hard(),n.is_doppel(),n.isBoss(),n.getLightSize(),n.getChangeHead());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);var row=BattleSpellCatalog.cancellationSpell(44);int map=(int)get(pc,"classicWorldMapId"),instance=(int)get(pc,"classicWorldInstanceId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
  var source=NativeDeNpcTemplateResources.snapshot().npcs().get(91123);var target=NativeDeNpcTemplateResources.snapshot().npcs().get(45641);
  need(NativeCancellationNpcRuntime.template(45641)!=null,"Source replacement factory supported: classname="+target.get_classname()+" random="+target.get_randomlevel());
  var def=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"replacement-fixture",1,0,x,y,0,0,0,0,0,0,3,10,10,false,30,false,0,rawTemplate(source));
  var pop=mode==14?ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e000fffL,List.of(def)):ClassicWorldMonsterPopulation.questInstance(7,0x5e000000L,0x5e000fffL,List.of(def));set(hub,"classicWorldMonsters",pop);
  var old=pop.inScreen(map,x,y).get(0);targetId=old.objectId();long next=(long)get(pop,"nextQuestDynamicObjectId");
  var clock=new Clock(mode==17);set(hub,"battleClock",clock);var rules=(NativeCancellationNpcRules.Snapshot)getStatic(NativeCancellationNpcRules.class,"current");
  if(mode==1)set(pc,"playerMp",0);if(mode==3)set(pc,"positionX",x-50);if(mode==4)pop.damage(map,targetId,Integer.MAX_VALUE);
  if(mode==5||mode==9||mode==17){var rows=new HashMap<>(rules.rows());var rule=rows.get(91123);rows.put(91123,new NativeCancellationNpcRules.Rule(91123,2,mode==5?999999:45641,mode==9?0:mode==17?1:300,"",0));NativeCancellationNpcRules.publish(new NativeCancellationNpcRules.Snapshot(rules.table(),rows,rules.invalid()));}
  if(mode==6)target.set_classname("unqualified.fixture.Executor");if(mode==7)set(pop,"nextQuestDynamicObjectId",(long)get(pop,"questDynamicObjectEnd")+1);if(mode==8)clock.shutdownNow();
  pop.haste(map,targetId,System.nanoTime(),60);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   boolean accepted=mode==0||mode>=5,converted=accepted&&(mode<5||mode>=9);int mp=(int)get(pc,"playerMp");
   if(mode==15||mode==16)NativeSourcePlayerActionLock.run(pc,()->{try{cast(pc,44,row);NativePlayerCommit.selected(mode==16?pc:peer,(mode==16?pc:peer).selectedCharacterName());}catch(Exception e){throw new RuntimeException(e);}});else if(mode==18){
    set(peer,"learnedSkills",new HashSet<>(Set.of(44)));NativeDeItemPublication.grant(peer,item,count+3);
    var gate=new CountDownLatch(1);var failures=new java.util.concurrent.atomic.AtomicReference<Throwable>();var threads=new ArrayList<Thread>();
    for(var caster:List.of(pc,peer)){var worker=new Thread(()->{try{gate.await();cast(caster,44,row);}catch(Throwable failure){failures.set(failure);}});threads.add(worker);worker.start();}
    gate.countDown();for(var worker:threads){worker.join(4000);need(!worker.isAlive(),"concurrent replacement cannot deadlock");}if(failures.get()!=null)throw new AssertionError(failures.get());
    accepted=(int)get(pc,"playerMp")!=mp;need((pc.nativeDeInventory(0).countCatalog(item)+peer.nativeDeInventory(0).countCatalog(item))==count+6,"two casters convert and pay exactly once");
    need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"peer receipt released");
   }else cast(pc,44,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,44,row):0),"cost once mode="+mode);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once mode="+mode);
   var replacement=pop.actor(map,next);
   need((replacement!=null)==converted,"actual packet transformation mode="+mode);
   if(converted){
    need(replacement.npcId()==45641&&replacement.x()==old.x()&&replacement.y()==old.y()&&replacement.heading()==old.heading(),"Source replacement identity/tile/heading");
    var removed=pop.actor(map,old.objectId());need(removed==null||!removed.present()&&!removed.alive(),"delete after successful spawn");
    var state=hub.nativeCancellationNpcRuntime;need(state.leases.size()==(mode==14?2:1),"bounded leases, original respawn ownership");
    if(mode==9){need(state.leases.get(0).deadline==0,"zero duration means unlimited");clock.fire();need(pop.actor(map,next).alive(),"no accidental expiry");}
    if(mode==10){set(state.leases.get(0),"deadline",System.nanoTime()-1);clock.fire();need(pop.actor(map,next)==null&&state.leases.isEmpty()&&clock.scheduled.isCancelled(),"expiry removes object and timer");}
    if(mode==11){pop.damage(map,next,Integer.MAX_VALUE);clock.fire();need(pop.actor(map,next)!=null&&!pop.actor(map,next).alive()&&pop.actor(map,next).present()&&state.leases.isEmpty(),"death keeps corpse, releases deadline");pop.removeCorpse(map,next);}
    if(mode==12){set(hub,"classicWorldMonsters",ClassicWorldMonsterPopulation.questInstance(8,0x5d000000L,0x5d000fffL,List.of(def)));clock.fire();need(state.leases.isEmpty()&&clock.scheduled.isCancelled(),"population retirement releases lease");}
    if(mode==13){hub.close();need(state.closed&&state.leases.isEmpty()&&pop.actor(map,next)==null&&clock.scheduled.isCancelled(),"shutdown cleanup idempotent");NativeCancellationNpcRuntime.close(hub);clock.fire();}
    if(mode==17){long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(pop.actor(map,next)!=null&&System.nanoTime()<limit)Thread.sleep(10);synchronized(hub){need(pop.actor(map,next)==null&&state.leases.isEmpty()&&clock.scheduled.isCancelled(),"actual scheduled deadline fires and drains");}}
    if(mode==14){for(var lease:state.leases)if(lease.respawn)set(lease,"deadline",System.nanoTime()-1);clock.fire();need(pop.actor(map,old.objectId()).alive()&&state.leases.size()==1,"old non-one-shot respawns through existing lifecycle");}
    if(mode==16){clock.fire();need(state.pending.isEmpty(),"hub-owned output survives caster retirement");}
    var self=collect(wire);var peers=collect(other);String remove=HexFormat.of().formatHex(Protocol.removeObject(old.objectId()));
    need(mode==16?self.isEmpty():self.contains(remove),"actual old-NPC remove packet or retired caster");need(mode==15?peers.isEmpty():peers.contains(remove),"viewer lifetime guard");
   }else if(accepted){need(pop.actor(map,old.objectId()).alive()&&!pop.hasteActive(map,old.objectId(),System.nanoTime()),"failed spawn keeps old actor and ordinary cancellation fallback");need(hub.nativeCancellationNpcRuntime==null||hub.nativeCancellationNpcRuntime.leases.isEmpty(),"failure has no orphan lease");}
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"no locked writes or pending receipts");
   collect(wire);collect(other);
  }finally{NativeCancellationNpcRules.publish(rules);if(mode==6)target.set_classname("0");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"fixture clock released");}
  cases++;
 }
 static Object getStatic(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<19;mode++){selectedSkill=skill;selectedMode=0;
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
