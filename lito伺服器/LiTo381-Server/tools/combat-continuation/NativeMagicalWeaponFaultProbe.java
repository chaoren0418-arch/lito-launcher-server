package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** One observational GameStateStore hook; PC, effects, item transaction and admission remain actual JAR classes. */
public final class NativeMagicalWeaponFaultProbe extends NativeMagicalWeaponProbe {
 static int expectedCost,material,amount;static long object;static boolean armed;static int faults,gaps;static GameStateStore memory;static ClientSession active;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed)return;armed=false;faults++;String key=(String)call(active,"stateCharacter",new Class[]{});need(memory.economy(key).inventory().get((long)material)==2,"fault after actual economy staging before original save");
  need(active.nativeDeInventory(0).countCatalog(material)==(before?2:amount+2),"candidate canonical inventory remains unchanged until memory save succeeds");
  need((int)get(active,"playerMp")== (before?(1000-expectedCost):1000),"candidate MP remains unchanged until material save succeeds");
  need(active.nativeDeInventory(0).item(object).enchantLevel()==(before?1:0),"candidate original timed effect not yet activated at fault");
  throw new IOException("fixture actual material memory flush failure");
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world)throws Exception{
  reset(pc,peer,hub,world);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(73)));((Map<?,?>)get(pc,"spellReadyNanos")).clear();call(pc,"clearInvisibilityStateSilently",new Class[]{});material=CombatDatabase.value(0,73,"MagicalWeaponFault","itemConsumeId");amount=CombatDatabase.value(0,73,"MagicalWeaponFault","itemConsumeCount");NativeDeItemPublication.grant(pc,material,amount+2);object=weapon(pc,true);expectedCost=cost(pc,73,spell(73));
  var inventory=pc.nativeDeInventory(0);String key=(String)call(pc,"stateCharacter",new Class[]{});var ledger=state.economy(key);memory=state;active=pc;
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   armed=true;Throwable failure=null;try{NativeMagicalWeaponProbe.cast(pc,73,object);}catch(Throwable error){failure=error;while(failure instanceof RuntimeException&&failure.getCause()!=null)failure=failure.getCause();}finally{armed=false;}
   if(before?!(failure instanceof IOException&&failure.getMessage().equals("fixture actual material memory flush failure")):failure!=null)throw new AssertionError("original material storage exception vs bounded Source rejection; before="+before,failure);need(state.economy(key).equals(ledger),"actual memory store rolls its staged economy back");
   need(pc.nativeDeInventory(0).countCatalog(material)==(before?2:amount+2),"failed cast cannot consume canonical material");need((int)get(pc,"playerMp")== (before?(1000-expectedCost):1000),"failed material cannot debit MP");need(pc.nativeDeInventory(0).item(object).enchantLevel()==(before?1:0),"failed material cannot install timed effect");need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(73)==before,"failed material cannot commit legacy reuse");
   var self=collect(wire);var peers=collect(other);need(peers.isEmpty()&&wire.locked+other.locked==0,"failure output unlocked and no peer effect");
   if(before){need(self.isEmpty(),"original item-weapon save precedes its entire output tail");gaps++;}else{need(pc.nativeDeInventory(0)==inventory&&self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299)))),"candidate failure preserves canonical identity and outputs exactly299");need(pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"material failure cancels Source provisional action");need(!pc.nativeCastTimelineState().reuseActor.isSkillDelay(),"material failure cancels Source provisional reuse");}
   need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"failed actual cast receipt released");
  }
  if(!before){NativeMagicalWeaponProbe.cast(pc,73,object);need(pc.nativeDeInventory(0).countCatalog(material)==2&&(int)get(pc,"playerMp")==(1000-expectedCost)&&pc.nativeDeInventory(0).item(object).enchantLevel()==1,"immediate actual retry succeeds after failed material cancellation");need(state.economy(key).inventory().get((long)material)==2,"retry memory ledger matches canonical inventory");}
  cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeDeItemTransaction.class,NativeCastTimeline.class,BattleTimedStatus.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual requested native PC/item/timeline/effects without replacement");need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"only verified inverse memory observation hook is overlaid");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true}){
   var r=root.resolve(flag+"-"+world+"-"+app);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated native clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==8&&NativePlayerLifetime.allIdle(),"all actual memory faults reached and lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("item-weapon-fault-wire.txt"),String.join("\n",wires)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);
 }
}
