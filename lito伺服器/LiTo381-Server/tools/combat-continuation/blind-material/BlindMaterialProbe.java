package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Native skills40/103 packets with existing percentile fixture; original two-phase target, counter and probability rules. */
public class BlindMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={40,103};static int selectedSkill,selectedMode;static long targetId;static int gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.blindSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception {
  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).h((int)get(pc,"positionX")+1).h((int)get(pc,"positionY")).bytes();
  call(pc,"handle",new Class[]{byte[].class},packet);
 }
 static void probability(int skill)throws Exception{
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);
  var rows=new HashMap<>(data.controls().probabilities());rows.put(skill,new SourceSkillControls.Probability(skill,50,50,50,0,0,0,0,false,false,0,0,0));
  f.set(null,copy(data,Map.of("controls",copy(data.controls(),Map.of("probabilities",rows)))));
 }
 static boolean active(ClientSession pc,int skill)throws Exception{
  return ((BattleTimedStatus)get(pc,"timedStatus")).blindActive(System.nanoTime());
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception {
  reset(pc,peer,hub,world);
  var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
  set(pc,"characters",List.of(copy(c,Map.of("type",selectedSkill==103?4:3,"originalIntelligence",10))));
  for(var p:List.of(pc,peer)){
   set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);
   call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleTimedStatus)get(p,"timedStatus")).clear();((BattleEraseMagicStatus)get(p,"eraseMagicStatus")).clear();
  }
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,40,103)));targetId=peer.playerObjectId();
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));

 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(peer,"invisible",true);
  if(mode==9)targetId=pc.playerObjectId();if(mode==10)((BattleTimedStatus)get(peer,"timedStatus")).activateCounterMagic(System.nanoTime(),60);
  if(mode==11)((BattleTimedStatus)get(peer,"timedStatus")).activateHaste(System.nanoTime(),60,43);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode>=10||mode==4&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"Source resources mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once after valid target and resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"reuse follows accepted material");
   {boolean applied=accepted&&mode!=1&&mode!=10;need(active(peer,skill)==applied&&!active(pc,skill),"actual target status follows Source roll/counter/haste rules mode="+mode+" skill="+skill);}
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   if(mode==12){
    var status=(BattleTimedStatus)get(peer,"timedStatus");long end=(long)get(status,"blindExpiresAtNanos");
    need(!status.expireBlind(end-1)&&active(peer,skill),"blind retained before stored deadline");
    need(status.expireBlind(end)&&!active(peer,skill),"blind expires at stored deadline");
   }
   var self=collect(wire);var peers=collect(other);
   if(mode==12)need(peers.contains(HexFormat.of().formatHex(Protocol.blind(1))),"actual blind mode1 packet reaches affected player");
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(mode==0||mode==1||mode==10||mode==11)goldens.add("skill="+skill+" world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<13;mode++){selectedSkill=skill;selectedMode=mode;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("darkBlindFixtureRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
