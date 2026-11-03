package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real world population actor and original spell handler; isolated population replaces no production class. */
public final class WeaknessWorldNpcProbe extends WeaknessMaterialProbe {
 static ClassicWorldMonsterPopulation.Snapshot monster(ClientSession pc,GameHub hub)throws Exception{
  int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
  var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).eraseEligible()).findFirst().orElseThrow();
  var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"negative-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);
  var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);
  var actors=population.inScreen(map,x,y);need(actors.size()==1&&actors.get(0).alive(),"one real isolated Source population actor");return actors.get(0);
 }
 static boolean active(GameHub hub,ClientSession pc,long object,int skill)throws Exception{
  int map=(int)get(pc,"classicWorldMapId"),instance=(int)get(pc,"classicWorldInstanceId");
  return skill==56?hub.classicWorldMonsterDiseaseActive(map,instance,object,System.nanoTime()):hub.classicWorldMonsterWeaknessActive(map,instance,object,System.nanoTime());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);int skill=selectedSkill;Object row=spell(skill);probability(skill);var npc=monster(pc,hub);long object=npc.objectId();targetId=object;
  if(mode==2){var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");population.damage((int)get(pc,"classicWorldMapId"),object,Integer.MAX_VALUE);need(!population.actor((int)get(pc,"classicWorldMapId"),object).alive(),"real NPC death transition");}
  if(mode==3)set(pc,"positionX",(int)get(pc,"positionX")-40);if(mode==6)targetId=0x7feeeeeeL;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==7||mode==5&&before;
   selectedMode=mode==7?12:mode==1?1:0;cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"world NPC Source MP mode="+mode+" skill="+skill);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"world NPC material once after admission");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"world NPC reuse");
   need(active(hub,pc,object,skill)==(accepted&&mode!=1),"actual population effect follows Source roll");
   if(mode==5){need((wire.raw.size()>bytes)==before,"NPC shared Source window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==4)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"world NPC missing material299");
   if(mode<=1)goldens.add("skill="+skill+" mode="+mode+" npc="+npc.npcId()+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"world NPC output unlocked and receipt drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("weaknessFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
    exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
