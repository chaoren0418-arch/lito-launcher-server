package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
/** One observational GameStateStore hook; PC, effects, item transaction and admission remain actual JAR classes. */
public final class MassTeleportFaultProbe extends MassTeleportProbe {
 static Object spell(int skill){return BattleSpellCatalog.massTeleportSpell(skill);}
 static boolean active(ClientSession pc,int skill)throws Exception{return get(pc,"pendingMassTeleport")!=null;}
 static void castArea(ClientSession pc,ClientSession peer,int skill,Object row,int roll,boolean nativeEntry)throws Exception{
  var hub=(GameHub)get(pc,"hub");int map=String.valueOf(get(pc,"phase")).equals("WORLD")?(int)get(pc,"classicWorldMapId"):hub.battleMapId((long)get(pc,"currentRoomId"),pc);
  call(pc,"handle",new Class[]{byte[].class},packet(map,0,0));
 }
 static int selectedNpc;
 static int expectedCost,material,amount,skill;static long object;static boolean armed;static int faults,gaps;static GameStateStore memory;static ClientSession active,recipient;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed)return;armed=false;faults++;String key=(String)call(active,"stateCharacter",new Class[]{});need(memory.economy(key).inventory().get((long)material)==2,"fault after actual economy staging before original save");
  need(active.nativeDeInventory(0).countCatalog(material)==(before?2:amount+2),"candidate canonical inventory remains unchanged until memory save succeeds");
  need((int)get(active,"playerMp")== (before?(1000-expectedCost):1000),"candidate MP remains unchanged until material save succeeds");
  need(active(recipient,skill)==before,"candidate cancellation follows successful material save");need((int)get(active,"playerHp")==4000-(before?0 /* CancellationSpell has no HP debit in either native body */:0),"HP unchanged until accepted material save");
  throw new IOException("fixture actual material memory flush failure");
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int selectedSkill)throws Exception{
  skill=selectedSkill;
  CancellationMaterialProbe.setup(pc,peer,hub,world);set(pc,"learnedSkills",new HashSet<>(Set.of(skill)));set(pc,"elfAttribute",31);set(pc,"pendingPortal",null);set(peer,"pendingPortal",null);set(peer,"pendingMassTeleport",null);((Map<?,?>)get(hub,"clanMembership")).clear();((Map<?,?>)get(hub,"clanProfiles")).clear();hub.registerOriginalClan(pc.sourceCharacterIdentity(),"FaultClan");hub.registerOriginalClan(peer.sourceCharacterIdentity(),"FaultClan");recipient=peer;material=CombatDatabase.value(0,skill,"MagicalWeaponFault","itemConsumeId");amount=CombatDatabase.value(0,skill,"MagicalWeaponFault","itemConsumeCount");NativeDeItemPublication.grant(pc,material,amount+2);expectedCost=cost(pc,skill,spell(skill));
  var inventory=pc.nativeDeInventory(0);String key=(String)call(pc,"stateCharacter",new Class[]{});var ledger=state.economy(key);memory=state;active=pc;
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   armed=true;Throwable failure=null;try{castArea(pc,peer,skill,spell(skill),1,false);}catch(Throwable error){failure=error;while(failure instanceof RuntimeException&&failure.getCause()!=null)failure=failure.getCause();}finally{armed=false;}
   if(before?!(failure instanceof IOException&&failure.getMessage().equals("fixture actual material memory flush failure")):failure!=null)throw new AssertionError("original material storage exception vs bounded Source rejection; before="+before,failure);need(state.economy(key).equals(ledger),"actual memory store rolls its staged economy back");
   need(pc.nativeDeInventory(0).countCatalog(material)==(before?2:amount+2),"failed cast cannot consume canonical material");need((int)get(pc,"playerMp")== (before?(1000-expectedCost):1000),"failed material cannot debit MP");need(active(peer,skill)==before,"candidate failed material cannot remove target status");need((int)get(pc,"playerHp")==4000-(before?0 /* CancellationSpell has no HP debit in either native body */:0),"material failure cannot debit HP");need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==before,"failed material cannot commit legacy reuse");
   var self=collect(wire);var peers=collect(other);need(wire.locked+other.locked==0,"failure output unlocked");
   if(before){
    need(!self.isEmpty(),"original cancellation inventory delta precedes failing memory save");
    need(peers.contains(HexFormat.of().formatHex(Protocol.effectObject(peer.playerObjectId(),value(spell(skill),"effectGfx")))),"original cancellation publishes target effect before failing material save");gaps++;
   }else{need(peers.isEmpty(),"candidate material failure publishes no peer effect");need(pc.nativeDeInventory(0)==inventory&&new HashSet<>(self).equals(Set.of(HexFormat.of().formatHex(Protocol.messageCode(299)),HexFormat.of().formatHex(Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK))))&&self.size()==2,"candidate failure preserves canonical identity and outputs exactly299");need(pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"material failure cancels Source provisional action");need(!pc.nativeCastTimelineState().reuseActor.isSkillDelay(),"material failure cancels Source provisional reuse");}
   need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"failed actual cast receipt released");
  }
  if(!before){castArea(pc,peer,skill,spell(skill),1,false);need(pc.nativeDeInventory(0).countCatalog(material)==2&&(int)get(pc,"playerMp")==(1000-expectedCost),"immediate actual retry succeeds after failed material cancellation");need(state.economy(key).inventory().get((long)material)==2,"retry memory ledger matches canonical inventory");}
  cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeDeItemTransaction.class,NativeCastTimeline.class,BattleTimedStatus.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual requested native PC/item/timeline/effects without replacement");need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"only verified inverse memory observation hook is overlaid");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int selectedSkill:new int[]{69}){selectedNpc=selectedSkill;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+selectedNpc);Files.createDirectories(r);var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated native clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,selectedSkill);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(faults==8&&NativePlayerLifetime.allIdle(),"all actual memory faults reached and lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("item-weapon-fault-wire.txt"),String.join("\n",wires)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);
 }
}
