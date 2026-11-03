package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
/** Actual skill145 packets and owned SUMMON/PET targets, without actor overlays. */
public class ReturnNatureMaterialProbe extends ElementalMaterialProbe {
 static long targetId;static int gaps;static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.returnToNatureSpell(skill);}
 static void cast(ClientSession pc)throws Exception{
  call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(18).c(0).d(targetId).h((int)get(pc,"positionX")+2).h((int)get(pc,"positionY")+2).bytes());
 }
 static void probability()throws Exception{
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);var rows=new HashMap<>(data.controls().probabilities());rows.put(145,new SourceSkillControls.Probability(145,50,50,50,0,0,0,0,false,false,0,0,0));f.set(null,copy(data,Map.of("controls",copy(data.controls(),Map.of("probabilities",rows)))));
 }
 static BattleCompanion companion(ClientSession owner,int x,int y,boolean pet)throws Exception{
  BattleCompanion actor;
  if(pet){var state=new GameStateStore.PetState("fixture-pet",0x70001652L,0x90001652L,(int)InventoryCatalog.PET_COLLAR,45034,"NaturePet",12,78,17,21375,0);actor=BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),x,y);}
  else{actor=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());actor.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));actor.cancelHaste();}
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(actor);return actor;
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception{
  reset(pc,peer,hub,world);probability();var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("type",2,"level",99,"originalIntelligence",10))));set(pc,"elfAttribute",8);
  for(var p:List.of(pc,peer)){set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleTimedStatus)get(p,"timedStatus")).clear();}
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,145)));
  if(!world){var point=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc)).spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var owner=mode==10?pc:peer;var pet=companion(owner,(int)get(pc,"positionX")+(mode==7?40:2),(int)get(pc,"positionY")+2,mode==9);targetId=pet.snapshot().objectId();
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)pet.applyDamage(Integer.MAX_VALUE);if(mode==14)targetId=peer.playerObjectId();if(mode==15)set(pc,"observer",true);
  if(mode==8)pet.activateEarthBind(System.nanoTime(),60);if(mode==11)pet.activateEraseMagic(System.nanoTime(),60);if(mode==12)pet.activateSleep(System.nanoTime(),60);
  long ownerPotions=owner.nativeDeInventory(0).countCatalog(40010);if(mode==16)need(pet.receiveItem(InventoryCatalog.require(40010),2),"real carried potion fixture");
  var row=BattleSpellCatalog.returnToNatureSpell(145);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"prior Light action remains reserved");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode>=8&&mode<=13||mode==16||mode==4&&before;boolean removed=accepted&&mode!=1&&mode!=8&&mode!=9;
   cast(pc);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,145,row):0)&&(int)get(pc,"playerHp")==hp,"Source resources mode="+mode+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"single material consumption");need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(145)==accepted,"reuse only on admission");
   need((!pet.snapshot().active())==(removed||mode==6),"SUMMON release follows probability and target kind mode="+mode);
   if(removed)need(!((List<BattleCompanion>)get(owner,"tamedCompanions")).contains(pet),"released companion removed from owner list");
   if(mode==16)need(pet.carriedItems().isEmpty()&&owner.nativeDeInventory(0).countCatalog(40010)==ownerPotions+2,"carried items return once to real owner rather than spell caster");
   if(mode==11)need(!pet.captureEraseMagic(System.nanoTime()).present(),"accepted cast consumes captured erase-magic lease");if(mode==12)need(!pet.sleepActive(System.nanoTime()),"accepted cast cancels target sleep");
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared source time window");if(before)gaps++;}
   if(mode==13){cast(pc);need((int)get(pc,"playerMp")==mp-cost(pc,145,row)&&pc.nativeDeInventory(0).countCatalog(item)==3,"repeat packet does not charge removed actor again");}
   var self=collect(wire);var peers=collect(other);
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(mode==9)need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(row.invalidTargetMessage()))),"PET invalid kind message after accepted source cost");
   if(removed)need(self.contains(HexFormat.of().formatHex(Protocol.removeObject(pet.snapshot().objectId()))),"actual removal packet");
   if(mode==0||mode==1||mode==8||mode==9||mode==10)goldens.add("world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,BattleCompanion.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native classes");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<17;mode++){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("returnToNatureFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only fixture");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output,"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
