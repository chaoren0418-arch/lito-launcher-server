package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real world population actor and original spell handler; isolated population replaces no production class. */
public final class CallNatureWorldNpcProbe extends CallNatureMaterialProbe {
 static ClassicWorldMonsterPopulation.Snapshot monster(ClientSession pc,GameHub hub)throws Exception{
  int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
  var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&ClassicWorldResurrectionCatalog.allowsSelectedTemplate(t.npcId(),true)).findFirst().orElseThrow();
  var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"negative-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);
  var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);
  var actors=population.inScreen(map,x,y);need(actors.size()==1&&actors.get(0).alive(),"one real isolated Source population actor");return actors.get(0);
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);int skill=selectedSkill;Object row=spell(skill);var npc=monster(pc,hub);long object=npc.objectId();targetId=object;
  int map=(int)get(pc,"classicWorldMapId");var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");
  if(mode!=1){population.damage(map,object,Integer.MAX_VALUE);need(!population.actor(map,object).alive(),"real NPC corpse before resurrection");}
  if(mode==2)set(pc,"playerMp",0);if(mode==3)set(pc,"positionX",(int)get(pc,"positionX")-40);if(mode==6)targetId=0x7feeeeeeL;if(mode==7)set(pc,"observer",true);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0&&mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==4&&item==0;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"world NPC resurrection Source MP mode="+mode+" skill="+skill);
   if(item>0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"NPC resurrection material debit once");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"NPC resurrection reuse follows admission");
   var after=population.actor(map,object);need(after.alive()==(accepted||mode==1),"actual population resurrection or unchanged live NPC");
   if(accepted)need(after.currentHp()==(int)call(row,"restoredNpcHp",new Class[]{int.class},npc.maximumHp()),"Source NPC restored HP rule");
   need(get(pc,"pendingResurrection")==null&&get(peer,"pendingResurrection")==null&&get(pc,"nativeResurrectionAnswerLease")==null,"NPC resurrection creates no player question or lease");
   if(mode==5)need(wire.raw.size()==bytes,"NPC shared Source action window");
   var self=collect(wire);var peers=collect(other);
   if(accepted)need(self.contains(HexFormat.of().formatHex(Protocol.resurrect(object,0,object,npc.gfxId())))&&peers.contains(HexFormat.of().formatHex(Protocol.resurrect(object,0,object,npc.gfxId()))),"actual NPC resurrection frame reaches both observers");
   if(mode==4&&item>0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"NPC missing material299 exactly once");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"NPC outputs unlocked and receipt drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("decayPotionFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
