package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual Taming packet and real WORLD NPC conversion, scoped to capacity controls. */
public final class PetTamingProbe extends ElementalMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int type,int setting)throws Exception{
  reset(pc,peer,hub,true);set(pc,"observer",false);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(36)));((BattleTimedStatus)get(pc,"timedStatus")).clear();
  var original=((List<CharacterSummary>)get(pc,"characters")).get(0);var character=copy(original,Map.of("type",type,"charisma",1,"originalIntelligence",10));set(pc,"characters",List.of(character));
  var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var previous=(CombatRules.Snapshot)field.get(null);var values=new HashMap<>(previous.values());var properties=new Properties();
  for(int i=0;i<7;i++)if(i==type&&setting>0)properties.setProperty(NativePetControls.KEYS.get(i),setting==1?"-100":"100");
  SourcePetReference.configure(properties);var bonuses=NativePetControls.parse(properties);for(int i=0;i<7;i++)values.put(NativePetControls.KEYS.get(i),bonuses.get(i).doubleValue());field.set(null,copy(previous,Map.of("values",values)));
  var db=CombatDatabase.class.getDeclaredField("current");db.setAccessible(true);var priorDb=(CombatDatabase.Snapshot)db.get(null);var probabilities=new HashMap<>(priorDb.controls().probabilities());probabilities.put(36,new SourceSkillControls.Probability(36,50,50,50,0,0,0,0,false,false,0,0,0));db.set(null,copy(priorDb,Map.of("controls",copy(priorDb.controls(),Map.of("probabilities",probabilities)))));
  var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");pets.clear();
  try{
   int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
   var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).tamable()).findFirst().orElseThrow();
   var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"pet-capacity-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);
   var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);var actors=population.inScreen(map,x,y);need(actors.size()==1&&actors.get(0).alive(),"one real tamable population actor");var target=actors.get(0);
   var row=BattleSpellCatalog.tamingSpell(36);int material=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,material,count+3);
   int effective=Math.max(1,Math.min(254,(int)call(pc,"effectiveCharisma",new Class[]{CharacterSummary.class},character)));boolean accepted=new SourcePetReference(type,effective,0,0).canTakePet(6);int mpCost=cost(pc,36,row);
   byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((36-1)>>3).c((36-1)&7).d(target.objectId()).h(x).h(y).bytes();
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    call(pc,"handle",new Class[]{byte[].class},packet);
    need(pets.size()==(accepted?1:0),"actual Taming capacity and factory type="+type+" setting="+setting);
    need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000-mpCost,1000))&&(int)get(pc,"playerHp")==4000-row.hpCost(),"Source cost precedes Taming capacity decision");
    need(pc.nativeDeInventory(0).countCatalog(material)==3,"accepted Taming consumes material even when capacity prevents conversion");
    var now=population.actor(map,target.objectId());need((now==null||!now.present())==accepted,"only successful conversion removes original population actor");
    if(accepted){var pet=pets.get(0).snapshot();need(pet.active()&&pet.kind()==BattleCompanion.Kind.TAMED&&pet.petCost()==6&&pet.npcId()==template.npcId(),"actual factory preserves NPC identity and Source cost");}
    var self=collect(wire);collect(other);if(!accepted)need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(319))),"capacity rejection visible as original319");
    // This checks capacity wiring, not the still-pending skill36 material/lifetime migration.
    need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"no retained receipt after Taming returns");
   }
  }finally{pets.clear();field.set(null,previous);db.set(null,priorDb);}cases++;
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var cls:List.of(ClientSession.class,NativePetControls.class,CombatRules.class,ClassicWorldMonsterPopulation.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes without consumer overlays");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean app:new boolean[]{false,true})for(int type=0;type<7;type++)for(int setting=0;setting<3;setting++){
   var r=root.resolve(app+"-"+type+"-"+setting);Files.createDirectories(r);var cfg=copy(base,Map.of("tamingMonsterFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,type,setting);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"all converted NPC state remains memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all actor lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes_observed\":"+lockedWrites+",\"skill36_material_and_lifetime_still_pending\":true,\"full_goal_achieved\":false}\n");
 }
}
