package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** Actual skill145 packet, existing owner entrance, native population and bag identities. */
public final class TamedWorldLifecycleProbe extends ReturnNatureMaterialProbe {
 static final class Future extends FutureTask<Void> implements ScheduledFuture<Void>{Future(Runnable r){super(r,null);}public long getDelay(TimeUnit u){return 0;}public int compareTo(Delayed d){return 0;}}
 static final class Clock extends ScheduledThreadPoolExecutor {boolean reject;final List<Future> tasks=new ArrayList<>();Clock(boolean r){super(1);reject=r;}@Override public ScheduledFuture<?> schedule(Runnable task,long delay,TimeUnit unit){if(reject)throw new RejectedExecutionException("explicit fixture OPEN rejection");var f=new Future(task);tasks.add(f);return f;}}
 static final class ThrowMap extends HashMap<Long,Object>{final long id;ThrowMap(Map<Long,Object> old,long n){super(old);id=n;}@Override public Object put(Long k,Object v){var old=super.put(k,v);if(k==id)throw new IllegalStateException("fixture actor publication failure after insert");return old;}}
 @SuppressWarnings("unchecked") static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean app,int mode)throws Exception {
  setup(pc,peer,hub,true);var owner=mode==1?pc:peer;int map=(int)get(owner,"classicWorldMapId"),instance=(int)get(owner,"classicWorldInstanceId");
  var pop=(ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;",map,instance);
  var actors=(Map<Long,Object>)get(pop,"actors");Object originActor=null;ClassicWorldMonsterPopulation.Snapshot origin=null;
  for(var actor:actors.values()){var snap=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");if(snap.alive()&&snap.present()&&snap.template().implementation().equals("L1Monster")&&snap.maximumMp()>0){originActor=actor;origin=snap;break;}}
  need(origin!=null,"native ordinary actor with MP");var originalBag=(RuntimeInventory)get(originActor,"releasedItems");need(originalBag.add(InventoryCatalog.require(40010),2),"picked-up NPC inventory before capture");set(originActor,"currentMp",Math.max(0,origin.maximumMp()-1));set(originActor,"heading",7);origin=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(originActor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");var captured=pop.captureTamed(map,origin.objectId());need(captured!=null,"actual WORLD capture entrance");
  var stats=new SourceNpcPhysicalStats(origin.npcId(),origin.template().armorClass(),origin.template().large(),0,false);
  BattleCompanion pet;var action=owner.nativePlayerActionLock();action.lock();try{pet=(BattleCompanion)call(owner,"createTamedCompanion",new Class[]{ClassicWorldMonsterPopulation.Snapshot.class,int.class,SourceNpcPhysicalStats.class},captured.captured(),instance,stats);}finally{action.unlock();}targetId=pet.snapshot().objectId();
  need(get(pet,"carriedItems")==originalBag&&get(originActor,"releasedItems")!=originalBag&&pet.snapshot().mp()==captured.captured().currentMp()&&pet.snapshot().heading()==7,"existing WORLD factory transfers exact captured bag, direct MP and heading");
  for(var p:List.of(pc,peer)){set(p,"positionX",origin.x());set(p,"positionY",origin.y());}
  need(pet.receiveItem(InventoryCatalog.require(40010),3),"carried source potions");need(pet.receiveItem(InventoryCatalog.require(20281),2),"two separate carried rings");var bag=get(pet,"carriedItems");var bagItems=pet.carriedItems();
  if(mode==4){set(pet,"hp",origin.maximumHp()+17);set(pet,"mp",origin.maximumMp()+11);}
  var expected=pet.snapshot();long next=(long)get(pop,"nextQuestDynamicObjectId");if(mode==5){actors=new ThrowMap(actors,next);set(pop,"actors",actors);}
  var oldclock=(ScheduledExecutorService)get(hub,"battleClock");oldclock.shutdownNow();need(oldclock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var clock=new Clock(mode==2);set(hub,"battleClock",clock);
  var row=BattleSpellCatalog.returnToNatureSpell(145);NativeDeItemPublication.grant(pc,Math.toIntExact(row.itemConsumeId()),row.itemConsumeCount()+3);
  long ownerPotion=owner.nativeDeInventory(0).countCatalog(40010);var ledger=state.economy((String)call(owner,"stateCharacter",new Class[]{}));var ownerInv=owner.nativeDeInventory(0);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   Throwable fault=null;try{cast(pc);}catch(Throwable e){fault=e;}
   if(mode==5){need(fault!=null,"controlled publication fault reaches retained source error path");need(pet.snapshot().active()&&get(pet,"carriedItems")==bag&&pet.carriedItems().equals(bagItems),"failed publication preserves exact bag and controlled actor");need(!actors.containsKey(next)&&get(originActor,"alive").equals(true)&&get(originActor,"present").equals(false),"failed inserted actor removed and origin hidden");need(clock.tasks.size()==1&&clock.tasks.get(0).isCancelled(),"failed publication cancels reserved respawn");}
   else if(mode==2){need(fault==null&&pet.snapshot().active()&&get(pet,"carriedItems")==bag&&pet.carriedItems().equals(bagItems),"OPEN scheduler rejection is atomic");need(actors.size()>0&&!actors.containsKey(next)&&get(originActor,"alive").equals(true)&&clock.tasks.isEmpty(),"scheduler rejection has no monster or origin mutation");}
   else {
    if(fault!=null)throw new AssertionError("native release",fault);
    need(!pet.snapshot().active()&&pet.carriedItems().isEmpty()&&get(pet,"carriedItems")!=bag,"controlled actor loses ownership of bag");
    var newActor=actors.get(next);need(newActor!=null&&get(newActor,"releasedItems")==bag,"new monster receives exact bag reference");
    var released=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(newActor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");
    need(released.objectId()!=origin.objectId()&&released.objectId()!=expected.objectId()&&released.template().experience()==0,"fresh native ID and zero XP");
    need(released.x()==expected.x()&&released.y()==expected.y()&&released.heading()==expected.heading()&&released.currentHp()==expected.hp()&&released.currentMp()==expected.mp(),"Source direct position, heading and vitals including overcap");
    need(released.maximumHp()==origin.maximumHp()&&released.maximumMp()==origin.maximumMp(),"template maxima remain Source values");
    need(!get(originActor,"alive").equals(true)&&!get(originActor,"present").equals(true)&&clock.tasks.size()==1,"ordinary origin becomes separate respawn candidate");
    need(NativeTamedWorldLifecycle.initializedDrop(released),"initialized loot marker");
    need((owner==pc||owner.nativeDeInventory(0)==ownerInv)&&owner.nativeDeInventory(0).countCatalog(40010)==ownerPotion,"liberated inventory never enters owner economy");
    if(owner!=pc)need(state.economy((String)call(owner,"stateCharacter",new Class[]{})).equals(ledger),"no owner persistence mutation");
    need(collect(wire).contains(HexFormat.of().formatHex(Protocol.removeObject(expected.objectId()))),"old controlled identity removed on wire");
    if(mode==3){actors.remove(origin.objectId());clock.tasks.get(0).run();need(!get(originActor,"alive").equals(true),"stale actor timer cannot revive removed origin");}
    if(mode==0){clock.tasks.get(0).run();need(get(originActor,"alive").equals(true)&&get(originActor,"present").equals(true)&&get(newActor,"releasedItems")==bag,"separate original spawn returns without duplicating liberated bag");}
    if(mode==0){
     var grounds=hub.classicWorldGroundItemsInScreen(map,instance,released.x(),released.y());long oldQuantity=grounds.stream().mapToLong(g->g.quantity()).sum();
     var dead=pop.damage(map,next,Integer.MAX_VALUE).after();var contributions=List.of(new ClassicWorldMonsterPopulation.Contribution(pc.playerObjectId(),1));
     call(pc,"distributeClassicWorldMonsterDrops",new Class[]{ClassicWorldMonsterPopulation.Snapshot.class,List.class,Map.class},dead,contributions,Map.of(pc.playerObjectId(),pc));
     long dropped=hub.classicWorldGroundItemsInScreen(map,instance,released.x(),released.y()).stream().mapToLong(g->g.quantity()).sum();
     need(dropped-oldQuantity==7&&((RuntimeInventory)bag).items().isEmpty(),"death releases exactly captured and carried items; initialized monster does not roll fresh loot");
     call(pc,"distributeClassicWorldMonsterDrops",new Class[]{ClassicWorldMonsterPopulation.Snapshot.class,List.class,Map.class},dead,contributions,Map.of(pc.playerObjectId(),pc));
     need(hub.classicWorldGroundItemsInScreen(map,instance,released.x(),released.y()).stream().mapToLong(g->g.quantity()).sum()==dropped,"repeated drop consumer cannot duplicate carried or random loot");
    }
   }
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(owner))==0,"both role receipts drained and output unlocked");
  }finally{clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"fixture clock terminated");}cases++;
 }
 public static void main(String[]args)throws Exception {
  var jar=Path.of(args[3]).toRealPath();for(var t:List.of(ClientSession.class,BattleCompanion.class,NativeTamedWorldLifecycle.class,ClassicWorldMonsterPopulation.class))need(Path.of(t.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual integrated native classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);
  try{for(boolean app:new boolean[]{false,true})for(int mode=0;mode<6;mode++){
   var r=root.resolve(app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties"),"classicWorldDropAutoLoot",0,"classicWorldDropItemRate",1000000.0,"classicWorldDropAdenaRate",1000000.0));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped before scene setup");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,app,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only state");
  }}finally{NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
