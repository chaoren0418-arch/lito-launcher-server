package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** Observe a real owner's memory-save failure after the caster has paid skill145. */
public final class ReturnNatureTransferFaultProbe extends ReturnNatureMaterialProbe {
 static GameStateStore memory;static ClientSession owner;static BattleCompanion actor;
 static String ownerKey;static long potions;static boolean armed;static int faults,divergences;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed||memory.economy(ownerKey).inventory().getOrDefault(40010L,0)!=potions+2)return;
  armed=false;faults++;throw new IOException("fixture companion owner memory failure");
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world)throws Exception{
  setup(pc,peer,hub,world);owner=peer;actor=companion(owner,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2,false);targetId=actor.snapshot().objectId();
  need(actor.receiveItem(InventoryCatalog.require(40010),2),"real carried items");potions=owner.nativeDeInventory(0).countCatalog(40010);
  ownerKey=(String)call(owner,"stateCharacter",new Class[]{});memory=state;var ledger=state.economy(ownerKey);
  var row=BattleSpellCatalog.returnToNatureSpell(145);int item=Math.toIntExact(row.itemConsumeId()),amount=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,amount+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   armed=true;Throwable failure=null;try{cast(pc);}catch(Throwable error){failure=error;while(failure.getCause()!=null)failure=failure.getCause();}finally{armed=false;}
   need(before?failure instanceof IOException&&failure.getMessage().equals("fixture companion owner memory failure"):failure==null,"original exception versus bounded failed release");
   need(state.economy(ownerKey).equals(ledger),"memory ledger rolls back failed owner save");
   need(owner.nativeDeInventory(0).countCatalog(40010)==potions+(before?2:0)&&actor.carriedItems().isEmpty()==before&&actor.snapshot().active(),"failed return preserves owner inventory and companion bag on candidate");if(before)divergences++;
   need((int)get(pc,"playerMp")==1000-cost(pc,145,row)&&pc.nativeDeInventory(0).countCatalog(item)==3,"caster was already charged by accepted spell");
   var self=collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"failure leaves no output under lock or outstanding receipt");
   if(!before){
    need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(row.failureMessage()))),"failed return retains native visible failure message");
    long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
    while((!pc.nativeCastTimelineState().timeline.canReserveSkillAction()||pc.nativeCastTimelineState().reuseActor.isSkillDelay())&&System.nanoTime()<until)Thread.sleep(1);
    need(pc.nativeCastTimelineState().timeline.canReserveSkillAction()&&!pc.nativeCastTimelineState().reuseActor.isSkillDelay(),"source reuse expires before retry");
    cast(pc);need(owner.nativeDeInventory(0).countCatalog(40010)==potions+2&&actor.carriedItems().isEmpty()&&!actor.snapshot().active(),"retry transfers once then retires summon");
    need(state.economy(ownerKey).inventory().getOrDefault(40010L,0)==potions+2,"successful retry ledger equals canonical inventory");
   }
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args.length<5||args[4].equals("before");var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native transfer body");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true}){
   var r=root.resolve(flag+"-"+world+"-"+app);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"no source or fixture disk state writes");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==8&&divergences==(before?8:0)&&NativePlayerLifetime.allIdle(),"all owner failures reached and candidate has no partial transfer");
  Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"owner_save_faults\":"+faults+",\"partial_owner_transfers\":"+divergences+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
