package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual skill36 and145 packets exercise initialized loot and original-spawn timing. */
public final class TamedCaptureLifecycleProbe extends ReturnNatureMaterialProbe {
 interface CapturedHook{void run(ClientSession pc,ClientSession peer,GameHub hub,BattleCompanion pet,SourceActionPairProbe.Wire a,SourceActionPairProbe.Wire b)throws Exception;}static CapturedHook capturedHook;
 static void tame(ClientSession pc,long id,int x,int y)throws Exception{call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(4).c(3).d(id).h(x).h(y).bytes());}
 @SuppressWarnings("unchecked") static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean app,int mode)throws Exception {
  setup(pc,peer,hub,true);set(pc,"learnedSkills",new HashSet<>(Set.of(2,36,145)));int map=(int)get(pc,"classicWorldMapId"),instance=(int)get(pc,"classicWorldInstanceId");
  var pop=(ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;",map,instance);var actors=(Map<Long,Object>)get(pop,"actors");
  Object originActor=null;ClassicWorldMonsterPopulation.Snapshot origin=null;var world=ClassicWorldMapCatalog.forMap(map);
  for(var actor:actors.values()){
   var snap=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");var stats=SourceNpcPhysicalStats.memoryFixture(snap.npcId());
   if(snap.mapId()==map&&snap.present()&&snap.alive()&&snap.template().implementation().equals("L1Monster")&&!snap.template().boss()&&stats.tamable()&&!stats.boss()&&!world.isSafetyZone(snap.x(),snap.y())&&!ClassicWorldDropCatalog.roll(map,snap.npcId(),1000000,1000000,1,new Random(7)).isEmpty()){originActor=actor;origin=snap;break;}
  }
  need(origin!=null,"ordinary, tamable, unsafe NPC with guaranteed Source loot");var bag=(RuntimeInventory)get(originActor,"releasedItems");need(bag.add(InventoryCatalog.require(20281),mode==2?RuntimeInventory.MAX_SLOTS:2),"existing NPC bag");var originalItems=bag.items();
  if(mode==3)set(originActor,NativeTamedCaptureLifecycle.DROP,true);
  set(originActor,"currentMp",Math.max(0,origin.maximumMp()-1));set(originActor,"heading",6);origin=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(originActor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");
  for(var p:List.of(pc,peer)){set(p,"positionX",origin.x());set(p,"positionY",origin.y());}
  var clock=new TamedWorldLifecycleProbe.Clock(mode==1);set(hub,"battleClock",clock);var row=BattleSpellCatalog.tamingSpell(36);NativeDeItemPublication.grant(pc,Math.toIntExact(row.itemConsumeId()),row.itemConsumeCount()+3);int mp=(int)get(pc,"playerMp");
  if(mode==4){var lock=pc.nativePlayerActionLock();lock.lock();try{
   var prep=NativeTamedCaptureLifecycle.capture(hub,map,instance,origin.objectId(),pc);need(prep!=null,"prepared capture for Source template mismatch");boolean failed=false;
   try{NativeTamedCaptureLifecycle.create(0x5f000073L,prep.captured(),instance,new SourceNpcPhysicalStats(origin.npcId()+1,0,false,0,false),pc);}catch(IllegalArgumentException expected){failed=true;}
   need(failed&&get(originActor,"alive").equals(true)&&get(originActor,"present").equals(true)&&clock.tasks.size()==1&&clock.tasks.get(0).isCancelled(),"failed native factory restores original visibility and cancels its timer");
   var restored=(RuntimeInventory)get(originActor,"releasedItems");for(var item:originalItems)need(restored.items().contains(item),"factory rollback preserves every original item alongside initialized loot");
   need(NativeTamedCaptureLifecycle.initializedDrop(pc,origin)&&get(prep.captured(),NativeTamedCaptureLifecycle.PREP)==null,"initialized loot cannot roll twice after rollback; preparation released");originalItems=restored.items();clock.tasks.clear();
  }finally{lock.unlock();}}
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   tame(pc,origin.objectId(),origin.x(),origin.y());var companions=(List<BattleCompanion>)get(pc,"tamedCompanions");
   need((int)get(pc,"playerMp")<mp,"real Source cast charged MP");
   if(mode==1||mode==2){need(companions.isEmpty()&&get(originActor,"alive").equals(true)&&get(originActor,"present").equals(true),"capture admission failure preserves visible living original");need(get(originActor,"releasedItems")==bag&&bag.items().equals(originalItems)&&clock.tasks.isEmpty(),"OPEN rejection/capacity failure keeps complete exact original bag and no timer");}
   else{
    need(companions.size()==1,"actual tame packet publishes one controlled actor");var pet=companions.get(0);var carried=(RuntimeInventory)get(pet,"carriedItems");
    need(pet.snapshot().mp()==origin.currentMp()&&pet.snapshot().heading()==origin.heading()&&pet.snapshot().hp()==origin.currentHp(),"Source direct capture HP/MP/heading");
    for(var item:originalItems)need(carried.items().stream().anyMatch(i->i.equals(item)),"existing NPC object ID, state and count retained");
    need(mode==3||mode==4?carried.items().equals(originalItems):carried.items().size()>originalItems.size(),"only uninitialized actor generates initial loot");
    need(get(originActor,"releasedItems")!=carried&&((RuntimeInventory)get(originActor,"releasedItems")).items().isEmpty(),"bag has one owner");
    need(clock.tasks.size()==1&&!get(originActor,"alive").equals(true)&&!get(originActor,"present").equals(true),"capture itself retires ordinary spawn and admits its timer");
    if(capturedHook!=null){capturedHook.run(pc,peer,hub,pet,wire,other);return;}
    if(mode==5){
     need(pop.respawn(map,origin.objectId())!=null,"external lifecycle owner respawns original slot early");var lock=pc.nativePlayerActionLock();lock.lock();try{
      var again=NativeTamedCaptureLifecycle.capture(hub,map,instance,origin.objectId(),pc);need(again!=null,"new capture of same physical NPC slot");call(pc,"createTamedCompanion",new Class[]{ClassicWorldMonsterPopulation.Snapshot.class,int.class,SourceNpcPhysicalStats.class},again.captured(),instance,SourceNpcPhysicalStats.memoryFixture(origin.npcId()));
     }finally{lock.unlock();}
     need(clock.tasks.size()==2&&!get(originActor,"alive").equals(true),"new generation owns separate respawn timer");clock.tasks.get(0).run();need(!get(originActor,"alive").equals(true)&&!get(originActor,"present").equals(true),"obsolete timer cannot revive later capture of same physical actor");clock.tasks.get(1).run();
    }else clock.tasks.get(0).run();
    need(get(originActor,"alive").equals(true)&&get(originActor,"present").equals(true)&&!(boolean)get(originActor,NativeTamedCaptureLifecycle.DROP),"original spawn returns before liberation, fresh loot state reset");
    long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(!pc.nativeCastTimelineState().timeline.canReserveSkillAction()&&System.nanoTime()<until)Thread.sleep(1);need(pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"original Source action window completed");
    targetId=pet.snapshot().objectId();var nature=BattleSpellCatalog.returnToNatureSpell(145);NativeDeItemPublication.grant(pc,Math.toIntExact(nature.itemConsumeId()),nature.itemConsumeCount()+3);long next=(long)get(pop,"nextQuestDynamicObjectId");cast(pc);
    need(!pet.snapshot().active()&&pet.carriedItems().isEmpty()&&actors.containsKey(next)&&get(actors.get(next),"releasedItems")==carried,"existing145 entrance liberates exact bag after old spawn has already returned");
    need(get(originActor,"alive").equals(true)&&get(originActor,"present").equals(true)&&clock.tasks.size()==(mode==5?2:1),"liberation does not retire original respawn a second time or admit duplicate timer");
   }
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked outputs and no receipt retention");
  }finally{clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"controlled fixture clock terminated");}cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var t:List.of(ClientSession.class,BattleCompanion.class,NativeTamedCaptureLifecycle.class,ClassicWorldMonsterPopulation.class))need(Path.of(t.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual integrated native classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);
  try{for(boolean app:new boolean[]{false,true})for(int mode=0;mode<6;mode++){
   var r=root.resolve(app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",1,"tamingMonsterFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties"),"classicWorldDropItemRate",1000000.0,"classicWorldDropAdenaRate",1000000.0));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,app,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only state");
  }}finally{NativePlayerCommit.closeFixture();}need(NativePlayerLifetime.allIdle(),"all lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
