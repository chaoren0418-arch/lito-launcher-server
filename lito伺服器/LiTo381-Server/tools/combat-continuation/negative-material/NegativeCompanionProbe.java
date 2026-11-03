package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real constructed source elemental actor; retained handler resolves its owner in both game phases. */
public final class NegativeCompanionProbe extends NegativeMaterialProbe {
 static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception {
  var pet=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());
  pet.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));pet.cancelHaste();
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static boolean active(BattleCompanion pet,int skill)throws Exception{return skill==153?((BattleEraseMagicStatus)get(pet,"eraseMagicStatus")).active(System.nanoTime()):pet.snapshot().moveSpeed()==BattleTimedStatus.SLOW;}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  ClientSession owner=mode==0?pc:peer;var pet=companion(owner,(int)get(pc,"positionX")+1,(int)get(pc,"positionY"));targetId=pet.snapshot().objectId();
  if(mode==2)pet.applyDamage(Integer.MAX_VALUE);if(mode==3)pet.deactivate();if(mode==4)pet.relocate((int)get(pc,"positionX")+40,(int)get(pc,"positionY"));
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=5)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==6){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==7||mode==6&&before;
   selectedMode=mode==7?12:0;cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"companion Source MP mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==5?0:count+3-(accepted?count:0)),"companion material consumed once after admission");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"companion cast reuse");
   if(mode!=7)need(active(pet,skill)==accepted,"actual companion receives accepted negative state");
   need(!active(pc,skill)&&!active(peer,skill),"neither caster nor owner receives companion effect");
   if(mode==6){need((wire.raw.size()>bytes)==before,"companion shared Source window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==5)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"companion missing material299");
   if(mode<=1)goldens.add("skill="+skill+" world="+world+" own="+(mode==0)+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"companion output unlocked and cast receipt drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);
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
