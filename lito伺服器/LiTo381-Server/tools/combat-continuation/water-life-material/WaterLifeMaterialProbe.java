package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real player-target skill170 packets and effects, independently derived Source cost. */
public class WaterLifeMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={170};static long targetId;static int gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.waterLifeSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception {
  call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).bytes());
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception {
  reset(pc,peer,hub,world);
  var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
  set(pc,"characters",List.of(copy(c,Map.of("type",2,"originalIntelligence",10))));
  for(var p:List.of(pc,peer)){
   set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);
   call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleWaterLifeStatus)get(p,"waterLifeStatus")).cancel();
  }
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,170)));targetId=peer.playerObjectId();
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.waterLifeSpell(170);
  if(mode==0)targetId=pc.playerObjectId();if(mode==3)set(pc,"playerMp",0);
  if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);
    while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light action interval");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode==4&&before;
   cast(pc,170,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,170,row):0)&&(int)get(pc,"playerHp")==hp,"Source resources and no HP change");
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material commits once after valid target and admission");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(170)==accepted,"reuse matches accepted cast");
   need(((BattleWaterLifeStatus)get(mode==0?pc:peer,"waterLifeStatus")).active(System.nanoTime())==accepted,"real one-shot recovery status on selected player only");
   need(!((BattleWaterLifeStatus)get(mode==0?peer:pc,"waterLifeStatus")).active(System.nanoTime()),"other player receives no state");
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source action window gates target cast");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material reports299 only");
   if(mode==0||mode==1)goldens.add("world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked publication and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<8;mode++){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
