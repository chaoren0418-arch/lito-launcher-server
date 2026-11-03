package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** One observational GameStateStore hook; PC, effects, item transaction and admission remain actual JAR classes. */
public final class WeaponBreakTargetFaultProbe extends WeaponBreakMaterialProbe {
 static int writes;static int expectedCost,material,amount,skill;static long object;static boolean armed;static int faults,gaps;static GameStateStore memory;static ClientSession active,recipient;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed||++writes!=2)return;armed=false;faults++;
  need((int)get(active,"playerMp")==1000-expectedCost,"caster resources already accepted before target durability save");
  throw new IOException("fixture target durability memory failure");
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int selectedSkill)throws Exception{
  skill=selectedSkill;WeaponBreakMaterialProbe.selectedSkill=skill;WeaponBreakMaterialProbe.selectedMode=0;probability(skill);
  setup(pc,peer,hub,world);recipient=peer;material=CombatDatabase.value(0,skill,"MagicalWeaponFault","itemConsumeId");amount=CombatDatabase.value(0,skill,"MagicalWeaponFault","itemConsumeCount");NativeDeItemPublication.grant(pc,material,amount+2);expectedCost=cost(pc,skill,spell(skill));
  var inventory=peer.nativeDeInventory(0);var weapon=inventory.equippedWeaponItem();String key=(String)call(peer,"stateCharacter",new Class[]{});var ledger=state.economy(key);memory=state;active=pc;
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   armed=true;writes=0;Throwable failure=null;try{WeaponBreakMaterialProbe.cast(pc,skill,spell(skill));}catch(Throwable e){failure=e;while(failure.getCause()!=null)failure=failure.getCause();}finally{armed=false;}
   need(failure instanceof IOException&&failure.getMessage().equals("fixture target durability memory failure"),"actual target memory failure propagated");
   need(state.economy(key).equals(ledger),"target memory rolls back");
   var actual=peer.nativeDeInventory(0).equippedWeaponItem();
   need(actual!=null&&actual.objectId()==weapon.objectId()&&actual.durability()==0,"failed target save restores exact weapon durability");
   need(pc.nativeDeInventory(0).countCatalog(material)==2&&(int)get(pc,"playerMp")==1000-expectedCost,"accepted caster cost is retained once on target failure");
   var self=collect(wire);var peers=collect(other);need(wire.locked+other.locked==0,"target fault output unlocked");
   need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty(),"target fault drains queues and receipts");
  }
  cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeDeItemTransaction.class,NativeCastTimeline.class,BattleTimedStatus.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual requested native PC/item/timeline/effects without replacement");need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"only verified inverse memory observation hook is overlaid");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int selectedSkill:SKILLS){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+selectedSkill);Files.createDirectories(r);var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated native clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,selectedSkill);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==8&&NativePlayerLifetime.allIdle(),"all actual memory faults reached and lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("item-weapon-fault-wire.txt"),String.join("\n",wires)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);
 }
}
