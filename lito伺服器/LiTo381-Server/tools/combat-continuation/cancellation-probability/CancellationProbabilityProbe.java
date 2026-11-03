package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual Weapon Break packets, target speed cancellation and caster material ownership. NPC/companion lifetime is a separate gate. */
public class CancellationProbabilityProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={44};static int selectedSkill,selectedMode;static long targetId;static int gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.cancellationSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception {
  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).bytes();
  call(pc,"handle",new Class[]{byte[].class},packet);
 }
 static void probability(int skill)throws Exception{
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);
  var rows=new HashMap<>(data.controls().probabilities());int base=selectedMode==15?-1000:selectedMode==16?1000:50;rows.put(skill,new SourceSkillControls.Probability(skill,base,base,base,0,0,0,0,false,false,0,0,0));
  f.set(null,copy(data,Map.of("controls",copy(data.controls(),Map.of("probabilities",rows)))));
 }
 static boolean active(ClientSession pc,int skill)throws Exception{return ((BattleTimedStatus)get(pc,"timedStatus")).speed(System.nanoTime()).type()==BattleTimedStatus.NORMAL;}
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception {
  reset(pc,peer,hub,world);
  var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
  set(pc,"characters",List.of(copy(c,Map.of("type",3,"originalIntelligence",10))));
  for(var p:List.of(pc,peer)){
   set(p,"playerHp",4000);set(p,"playerMaxHp",5000);set(p,"playerMp",1000);set(p,"playerMaxMp",1000);set(p,"observer",false);
   call(p,"clearInvisibilityStateSilently",new Class[]{});((BattleTimedStatus)get(p,"timedStatus")).clear();((BattleEraseMagicStatus)get(p,"eraseMagicStatus")).clear();
  }
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,44)));targetId=peer.playerObjectId();
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  for(var equipped:List.of(pc,peer))((BattleTimedStatus)get(equipped,"timedStatus")).activateHaste(System.nanoTime(),60,43);
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));

 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(peer,"invisible",true);
  if(mode==9)targetId=pc.playerObjectId();if(mode==10)((BattleTimedStatus)get(peer,"timedStatus")).activateCounterMagic(System.nanoTime(),60);
  if(mode==11)((BattleTimedStatus)get(peer,"timedStatus")).activateSlow(System.nanoTime(),60,29);
  if(mode==12){hub.registerOriginalClan(pc.sourceCharacterIdentity(),"CancellationClan");hub.registerOriginalClan(peer.sourceCharacterIdentity(),"CancellationClan");}
  if(mode==13)hub.joinClassicParty(pc.sourceCharacterIdentity(),peer.sourceCharacterIdentity(),8);
  if(mode==14){NativeCancellationProbability.prepare(peer);NativeSourcePlayerActionLock.run(peer,()->{var actor=NativeReuseGate.actor(peer);var policy=actor.materialPolicy;actor.materialPolicy=new NativeCastMaterialPolicy.State(policy.owner(),policy.repository(),policy.account(),policy.id(),policy.name(),200);});}
  if(mode==17){set(peer,"playerLawful",30000);need(NativeCancellationProbability.targetMr(peer,System.nanoTime())==CombatAccess.integerCall(peer,"effectiveBaseMagicResistance","(J)I",System.nanoTime())+9,"actual lawful guardian MR");}
  if(mode==18||mode==19||mode==21)NativeCancellationSleep.activate((BattleTimedStatus)get(peer,"timedStatus"),System.nanoTime(),60,mode==19?212:66);
  if(mode==20||mode==21)((BattleEraseMagicStatus)get(peer,"eraseMagicStatus")).activate(System.nanoTime(),60);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=2&&mode!=21)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode>=9&&mode!=21||mode==4&&before;
   var trace=new java.io.ByteArrayOutputStream();var oldOut=System.out;
   try(var out=new java.io.PrintStream(trace,true,java.nio.charset.StandardCharsets.UTF_8)){System.setOut(out);cast(pc,skill,row);}finally{System.setOut(oldOut);}
   String log=trace.toString(java.nio.charset.StandardCharsets.UTF_8);System.out.print(log);
   var matcher=java.util.regex.Pattern.compile("Source Cancellation probability target=([0-9]+) chance=([0-9]+) roll=([0-9]+) success=(true|false)").matcher(log);
   boolean effect=accepted;
   if(accepted){need(matcher.find(),"one actual probability evaluation after material admission");int chance=Integer.parseInt(matcher.group(2)),roll=Integer.parseInt(matcher.group(3));effect=Boolean.parseBoolean(matcher.group(4));need(effect==(chance==100||chance>0&&roll<=chance),"actual RNG decision");need(!matcher.find(),"one roll per cast");if(mode==9||mode==12||mode==13)need(chance==100&&roll==0,"self/clan/party bypass without RNG");if(mode==14||mode==15)need(chance==1,"GM target or lower cap");if(mode==16)need(chance==90,"upper cap");if(!effect)gaps++;}
   else need(!matcher.find(),"invalid/rejected material does not roll or consume target status");
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"Source resources mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2||mode==21?0:count+3-(accepted?count:0)),"material once after valid target and resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"reuse follows accepted material");
   need(active(peer,skill)==((effect&&mode!=9)||mode==11)&&active(pc,skill)==(effect&&mode==9),"actual cancellation removes target speed; self is an explicit valid target mode="+mode);
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   if(mode==18||mode==21)need(((BattleTimedStatus)get(peer,"timedStatus")).sleepActive(System.nanoTime())==!accepted,"only accepted cast removes fog even on failed probability");
   if(mode==19)need(((BattleTimedStatus)get(peer,"timedStatus")).sleepActive(System.nanoTime())==!effect,"failed probability preserves Phantasm identity");
   if(mode==20||mode==21)need(((BattleEraseMagicStatus)get(peer,"eraseMagicStatus")).active(System.nanoTime())==!accepted,"erase magic consumed only after accepted probability");
   var self=collect(wire);var peers=collect(other);
   if(accepted&&!effect){String fx=HexFormat.of().formatHex(Protocol.effectObject(targetId,((BattleSpellCatalog.CancellationSpell)row).effectGfx()));need(!self.contains(fx)&&!peers.contains(fx),"failed probability emits no successful cancellation effect");need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(((BattleSpellCatalog.CancellationSpell)row).failureMessage()))),"visible failure response");}
   if(mode==2||mode==21)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(mode==0||mode==1||mode==10||mode==11)goldens.add("skill="+skill+" world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=false;var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class,NativeCancellationProbability.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<22;mode++){selectedSkill=skill;selectedMode=mode;
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
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"probability_failures\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
