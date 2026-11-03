package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual skill14 packets; Source SQL remains read-only, only fixture JVM row14 is varied. */
public class WeightMaterialProbe extends CancellationMaterialProbe {
 static Object spell(int skill){return BattleSpellCatalog.weightSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception{
  call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).bytes());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);set(pc,"learnedSkills",new HashSet<>(Set.of(2,14)));var status=(BattleWeightStatus)get(pc,"weightStatus");status.clear();
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var old=(CombatDatabase.Snapshot)field.get(null);
  int item=old.skills().get(44).reagent();need(item>0,"real loaded material catalog");int count=2;
  var skills=new HashMap<>(old.skills());skills.put(14,copy(skills.get(14),Map.of("reagent",mode==0?0:item,"reagentCount",mode==0?0:count)));field.set(null,old.withSkills(skills));
  try{
   Object row=spell(14);int granted=mode==0||mode==3?0:mode==2?2:7;if(granted>0)NativeDeItemPublication.grant(pc,item,granted);
   if(mode==4)set(pc,"playerMp",0);if(mode==5)set(pc,"playerHp",(int)call(row,"hpCost",new Class[]{}));
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    if(mode==6){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light action window");}
    int hp=(int)get(pc,"playerHp"),mp=(int)get(pc,"playerMp");boolean accepted=mode<=2,activated=accepted&&(!before||mode==0);
    boolean refused=false;
    try{cast(pc,14,row);}catch(RuntimeException failure){Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();if(!before||!(cause instanceof IllegalStateException)||!"self cast requires reviewed no-material metadata".equals(cause.getMessage()))throw failure;refused=true;}
    need(refused==(before&&(mode==1||mode==2||mode==3||mode==6)),"exact legacy metadata guard, no unrelated exception accepted");
    long expected=granted-(mode==0||!accepted||before?0:count);
    need(pc.nativeDeInventory(0).countCatalog(item)==expected,"canonical debit matches observed path mode="+mode);
    need(status.snapshot(System.nanoTime()).decreaseWeightActive()==activated,"real status activation mode="+mode);
    need((int)get(pc,"playerMp")==mp-(activated?cost(pc,14,row):0),"MP only on accepted effect mode="+mode);
    need((int)get(pc,"playerHp")==hp-(activated?(int)call(row,"hpCost",new Class[]{}):0),"HP only on accepted effect");
    need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(14)==activated,"reuse exactly once after successful material");
    if(refused)gaps++;
    if(!before&&accepted)need(expected==granted-(mode==0?0:count)&&activated,"configured material never double-debited or partially applied");
    var self=collect(wire);var peers=collect(other);
    if(mode==3&&!before)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material exactly299");
    if(mode==0)goldens.add("world="+world+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
    need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
   }
  }finally{field.set(null,old);}cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{14})for(int mode=0;mode<7;mode++){selectedSkill=skill;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("weaponBreakFixtureD100",mode==1?100:1,"weaponBreakFixtureDamageRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");need(gaps==(before?32:0),"all configured material faults counted");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"configured_material_gaps\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
