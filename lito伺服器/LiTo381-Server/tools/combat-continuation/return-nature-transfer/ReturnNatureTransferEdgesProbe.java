package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.io.*;
/** Real SUMMON bags: same owner, retained shutdown, full inventory and nested ground rollback. */
public final class ReturnNatureTransferEdgesProbe extends ReturnNatureMaterialProbe {
 static GameStateStore memory;static String key;static int faults;static long count;static boolean armed;
 public static void beforeSave(){}
 public static void beforeFlush()throws IOException{
  if(armed&&memory.economy(key).inventory().getOrDefault(40010L,0)==count+2){armed=false;faults++;throw new IOException("nested ground owner save failure");}
 }
 static List<?> ground(GameHub hub,ClientSession pc,boolean world)throws Exception{
  return world?hub.classicWorldGroundItemsInScreen((int)get(pc,"classicWorldMapId"),(int)get(pc,"classicWorldInstanceId"),(int)get(pc,"positionX"),(int)get(pc,"positionY")):hub.groundItems((long)get(pc,"currentRoomId"));
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var owner=mode==0?pc:peer;var actor=companion(owner,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2,false);targetId=actor.snapshot().objectId();
  var row=BattleSpellCatalog.returnToNatureSpell(145);int item=Math.toIntExact(row.itemConsumeId()),amount=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,amount+3);
  if(mode>=2){
   var chars=(List<CharacterSummary>)get(owner,"characters");set(owner,"characters",List.of(copy(chars.get(0),Map.of("strength",254,"constitution",254))));
   if(mode!=4)NativeDeItemPublication.grant(owner,40010,1);
   NativeDeItemPublication.grant(owner,20281,RuntimeInventory.MAX_SLOTS-owner.nativeDeInventory(0).size());
   need(owner.nativeDeInventory(0).size()==RuntimeInventory.MAX_SLOTS,"real full inventory with nonstackable source rings");
   if(mode!=4)need(owner.permitsInventoryAddition(owner.nativeDeInventory(0),InventoryCatalog.require(40010),2),"stackable potion still within real weight limit");
   need(actor.receiveItem(InventoryCatalog.require(20281),2),"two distinct carried rings for nested ground reservations");
  }
  need(actor.receiveItem(InventoryCatalog.require(40010),2),"real carried potion");count=owner.nativeDeInventory(0).countCatalog(40010);key=(String)call(owner,"stateCharacter",new Class[]{});memory=state;
  var ledger=state.economy(key);var inventory=owner.nativeDeInventory(0);var bag=actor.carriedItems();var oldGround=ground(hub,owner,world);armed=mode==3;
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==1){
    var failure=new AtomicReference<Throwable>();var lock=pc.nativePlayerActionLock();var worker=new Thread(()->{try{cast(pc);}catch(Throwable e){failure.set(e);}},"retained-summon-inventory-return");
    lock.lock();try{worker.start();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<until)Thread.sleep(1);need(lock.hasQueuedThread(worker)&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==1,"cast admitted before shutdown");NativePlayerLifetime.shutdown(hub,false,0);}finally{lock.unlock();worker.join(3000);}
    if(failure.get()!=null)throw new AssertionError("retained owner transaction",failure.get());need(!worker.isAlive(),"retained transaction finishes");
   }else cast(pc);
   armed=false;var nowGround=ground(hub,owner,world);var self=collect(wire);var peers=collect(other);
   if(mode==3){
    need(owner.nativeDeInventory(0)==inventory&&state.economy(key).equals(ledger)&&actor.carriedItems().equals(bag)&&actor.snapshot().active(),"owner failure preserves canonical identity, ledger and full summon bag");
    need(nowGround.equals(oldGround),"all nested ground reservations rolled back");
    need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(row.failureMessage()))),"visible failed release");
   }else{
    need(!actor.snapshot().active()&&actor.carriedItems().isEmpty(),"accepted return retires actor and drains bag");
    need(owner.nativeDeInventory(0).countCatalog(40010)==count+(mode==4?0:2),"potion goes to owner when its stack fits");
    need(nowGround.size()==oldGround.size()+(mode==4?3:mode==2?2:0),"exact number of overflow objects, no duplicate ground publication");
    if(mode>=2){long quantity=0;for(Object entry:nowGround){var def=(InventoryCatalog.Definition)call(entry,"definition",new Class[]{});int qty=(int)call(entry,"quantity",new Class[]{});need(def.catalogId()==20281||mode==4&&def.catalogId()==40010,"only returned source items on ground");quantity+=qty;}need(quantity==(mode==4?4:2),"overflow quantity retained");}
    if(mode!=4)need(state.economy(key).inventory().getOrDefault(40010L,0)==count+2,"committed memory ledger matches owner inventory");
   }
   need((int)get(pc,"playerMp")==1000-cost(pc,145,row)&&pc.nativeDeInventory(0).countCatalog(item)==3,"accepted caster cost charged exactly once");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeReturnNatureTransfer.material(pc)==null&&NativeReturnNatureTransfer.material(owner)==null,"no locked output, leaked receipt or retained owner capability");
   if(mode==1){int bytes=wire.raw.size();cast(pc);need(wire.raw.size()==bytes,"shutdown admits no new producer");}
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var t:List.of(ClientSession.class,BattleCompanion.class,NativeReturnNatureTransfer.class))need(Path.of(t.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<5;mode++){
   var r=root.resolve(world+"-"+app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"no source or fixture disk writes");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==4&&NativePlayerLifetime.allIdle(),"all mixed transfers exercised owner-save failure");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"owner_save_faults\":"+faults+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
