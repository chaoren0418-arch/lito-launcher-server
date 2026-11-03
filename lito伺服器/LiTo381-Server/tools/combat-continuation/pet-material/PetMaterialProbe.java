package local.lito381.server;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real WORLD Taming/Create Zombie packets. Covers caster materials, not all NPC lifetime races. */
public final class PetMaterialProbe extends ElementalMaterialProbe {
 static int gaps,faults,partial;static boolean armed;static ClientSession active;static GameStateStore memory;static int reagent,quantity,expectedCost;static final List<String>goldens=new ArrayList<>();
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{
  if(!armed)return;armed=false;faults++;
  String key=(String)call(active,"stateCharacter",new Class[]{});
  need(memory.economy(key).inventory().get((long)reagent)==3,"fault after actual memory staging");
  need(active.nativeDeInventory(0).countCatalog(reagent)==(before?3:quantity+3),"canonical reagent commit ordering");
  need((int)get(active,"playerMp")==1000-(before?expectedCost:0),"MP must follow successful material memory commit");
  throw new IOException("fixture pet material memory commit failure");
 }
 static Object row(int skill){return skill==36?BattleSpellCatalog.tamingSpell(skill):BattleSpellCatalog.createZombieSpell(skill);}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,int skill,int mode)throws Exception{
  reset(pc,peer,hub,true);set(pc,"observer",mode==5);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",mode==2?0:1000);set(pc,"playerMaxMp",1000);set(pc,"learnedSkills",new HashSet<>(Set.of(2,skill)));((BattleTimedStatus)get(pc,"timedStatus")).clear();
  var character=((List<CharacterSummary>)get(pc,"characters")).get(0);character=copy(character,Map.of("type",3,"charisma",25,"originalIntelligence",10));set(pc,"characters",List.of(character));
  var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");pets.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var prior=(CombatDatabase.Snapshot)field.get(null);
  var probabilities=new HashMap<>(prior.controls().probabilities());probabilities.put(36,new SourceSkillControls.Probability(36,50,50,50,0,0,0,0,false,false,0,0,0));field.set(null,copy(prior,Map.of("controls",copy(prior.controls(),Map.of("probabilities",probabilities)))));
  try{
   int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
   var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).tamable()).findFirst().orElseThrow();
   var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"pet-material-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);
   definition=copy(definition,Map.of("minRespawnSeconds",60,"maxRespawnSeconds",60));
   var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);
   var target=population.inScreen(map,x,y).get(0);boolean corpse=(skill==41)!=(mode==7);if(corpse)population.damage(map,target.objectId(),Integer.MAX_VALUE);
   var spell=row(skill);int material=value(spell,"itemConsumeId"),count=value(spell,"itemConsumeCount");if(mode!=1)NativeDeItemPublication.grant(pc,material,count+3);
   int mpCost=cost(pc,skill,spell);long targetId=mode==4?0x7feeeeeeL:target.objectId();var writer=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId);if(skill==36)writer.h(x).h(y);byte[] packet=writer.bytes();
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    if(mode==3){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"actual Light leaves shared action window reserved");}
    int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp");var inventory=pc.nativeDeInventory(0);String key=(String)call(pc,"stateCharacter",new Class[]{});var ledger=state.economy(key);
    active=pc;memory=state;reagent=material;quantity=count;expectedCost=mpCost;armed=mode==8;Throwable failure=null;
    try{call(pc,"handle",new Class[]{byte[].class},packet);}catch(Exception error){if(mode!=8)throw error;failure=error;while(failure instanceof RuntimeException&&failure.getCause()!=null)failure=failure.getCause();}finally{armed=false;}
    boolean accepted=mode==0||mode==6||mode==3&&before;boolean consumed=accepted||mode==8&&before;
    need((int)get(pc,"playerMp")==mp-(consumed?mpCost:0)&&(int)get(pc,"playerHp")==hp-(consumed?value(spell,"hpCost"):0),"native cost skill="+skill+" mode="+mode);
    need(pc.nativeDeInventory(0).countCatalog(material)==(mode==1?0:count+3-(consumed?count:0)),"material exact count");
    need(pets.size()==(accepted?1:0),"factory runs only after accepted materials");
    var now=population.actor(map,target.objectId());need((now==null||!now.present())==accepted,"rejected cast preserves original live actor/corpse");
    if(accepted)need(pets.get(0).snapshot().kind()==(skill==36?BattleCompanion.Kind.TAMED:BattleCompanion.Kind.SUMMON),"actual factory kind");
    if(mode==3&&before)gaps++;
    if(mode==6){int output=wire.raw.size();call(pc,"handle",new Class[]{byte[].class},packet);need(wire.raw.size()==output&&pc.nativeDeInventory(0).countCatalog(material)==3&&pets.size()==1,"duplicate packet cannot create or consume twice");}
    if(mode==8){
     need(before?failure instanceof IOException&&failure.getMessage().equals("fixture pet material memory commit failure"):failure==null,"original storage exception versus bounded Source admission failure");
     need(state.economy(key).equals(ledger),"actual memory error restores ledger");
     if(before){partial++;}else{need(pc.nativeDeInventory(0)==inventory&&get(pc,"playerMp").equals(mp),"failed material retains canonical state");call(pc,"handle",new Class[]{byte[].class},packet);need(pets.size()==1&&pc.nativeDeInventory(0).countCatalog(material)==3,"immediate retry accepted once after rollback");}
    }
    var self=collect(wire);var peers=collect(other);if(mode==1)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"original missing material299");
    if(mode==0)goldens.add("skill="+skill+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
    need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked outputs and released receipts");
   }
  }finally{armed=false;pets.clear();field.set(null,prior);}cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var cls:List.of(ClientSession.class,NativeCastMaterial.class,NativePetControls.class,ClassicWorldMonsterPopulation.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer/factory without overlays");
  need(!Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"verified inverse memory hook only");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{36,41})for(int mode=0;mode<9;mode++){
   var r=root.resolve(flag+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("tamingMonsterFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    var petClock=new ScheduledThreadPoolExecutor(1,task->{var t=new Thread(task,"pet-material-fixture");t.setDaemon(true);return t;});petClock.setRemoveOnCancelPolicy(true);set(hub,"battleClock",petClock);try{exercise(p.session,m.session,hub,state,skill,mode);}finally{petClock.shutdownNow();need(petClock.awaitTermination(2,TimeUnit.SECONDS),"fixture corpse clock drained");}
   }need(!Files.exists(r.resolve("must-not-write.properties")),"no source or file persistence");
  }}catch(Throwable error){primary=error;throw error;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable error){if(primary!=null)primary.addSuppressed(error);else throw error;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output,"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":"+partial+",\"locked_writes\":"+lockedWrites+",\"frames\":"+frames+",\"npc_lifetime_and_battle_scenes_qualified\":false,\"full_goal_achieved\":false}\n");
 }
}
