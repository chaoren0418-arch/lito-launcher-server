package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** Fault inside the real memory save: no canonical item/HP/MP/status mutation may escape. */
public final class WeightFaultProbe extends WeightMaterialProbe {
 static int expectedCost,material,amount,skill,faults,gaps;static boolean armed;static GameStateStore memory;static ClientSession active;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed)return;armed=false;faults++;String key=(String)call(active,"stateCharacter",new Class[]{});
  need(memory.economy(key).inventory().get((long)material)==2,"actual memory staging reached");
  need(active.nativeDeInventory(0).countCatalog(material)==amount+2,"canonical inventory unchanged before memory commit");
  need((int)get(active,"playerMp")==1000&&(int)get(active,"playerHp")==4000,"HP and MP unchanged before memory commit");
  need(!((BattleWeightStatus)get(active,"weightStatus")).snapshot(System.nanoTime()).decreaseWeightActive(),"weight effect waits for material commit");
  throw new IOException("fixture actual weight material memory flush failure");
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int selected)throws Exception{
  skill=selected;setup(pc,peer,hub,world);set(pc,"learnedSkills",new HashSet<>(Set.of(14)));var weight=(BattleWeightStatus)get(pc,"weightStatus");weight.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var old=(CombatDatabase.Snapshot)field.get(null);material=old.skills().get(44).reagent();amount=2;
  var skills=new HashMap<>(old.skills());skills.put(14,copy(skills.get(14),Map.of("reagent",material,"reagentCount",amount)));field.set(null,old.withSkills(skills));
  try{
   NativeDeItemPublication.grant(pc,material,amount+2);var row=spell(skill);expectedCost=cost(pc,skill,row);var inventory=pc.nativeDeInventory(0);String key=(String)call(pc,"stateCharacter",new Class[]{});var ledger=state.economy(key);memory=state;active=pc;
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    armed=true;try{cast(pc,skill,row);}finally{armed=false;}
    need(state.economy(key).equals(ledger),"memory staging rolled back");
    need(pc.nativeDeInventory(0)==inventory&&inventory.countCatalog(material)==amount+2,"failed cast retains canonical identity and material");
    need((int)get(pc,"playerMp")==1000&&(int)get(pc,"playerHp")==4000&&!weight.snapshot(System.nanoTime()).decreaseWeightActive(),"failed material cannot change resources or weight");
    need(!((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill),"failed material cannot commit reuse");
    var self=collect(wire);var peers=collect(other);need(wire.locked+other.locked==0&&peers.isEmpty(),"failure has no peer effect and no locked output");
    need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299)))),"bounded failure message exactly299");
    need(pc.nativeCastTimelineState().timeline.canReserveSkillAction()&&!pc.nativeCastTimelineState().reuseActor.isSkillDelay(),"provisional action and reuse canceled");
    need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"failed receipt drained");
   }
   cast(pc,skill,row);need(pc.nativeDeInventory(0).countCatalog(material)==2&&(int)get(pc,"playerMp")==1000-expectedCost&&(int)get(pc,"playerHp")==4000-(int)call(row,"hpCost",new Class[]{})&&weight.snapshot(System.nanoTime()).decreaseWeightActive(),"immediate retry exactly once");
   need(state.economy(key).inventory().get((long)material)==2,"retry canonical and memory ledger agree");
  }finally{field.set(null,old);}cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeDeItemTransaction.class,NativeCastTimeline.class,BattleTimedStatus.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual requested native PC/item/timeline/effects without replacement");need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"only verified inverse memory observation hook is overlaid");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int selectedSkill:new int[]{14}){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+selectedSkill);Files.createDirectories(r);var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated native clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,selectedSkill);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==8&&NativePlayerLifetime.allIdle(),"all actual memory faults reached and lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("item-weapon-fault-wire.txt"),String.join("\n",wires)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);
 }
}
