package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Own/other live PET and SUMMON through actual C_USE_SPELL44. */
public class CancellationCompanionProbe extends CancellationMaterialProbe {
 static boolean petKind;
 static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception{
  BattleCompanion actor;
  if(petKind){var state=new GameStateStore.PetState("fixture-pet",0x70001652L,0x90001652L,(int)InventoryCatalog.PET_COLLAR,45034,"CancellationPet",12,78,17,21375,0);actor=BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),x,y);}
  else{actor=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());actor.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));}
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(actor);actor.activateHaste(System.nanoTime(),60,43);return actor;
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);ClientSession owner=mode==0?pc:peer;var actor=companion(owner,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);targetId=actor.snapshot().objectId();var row=BattleSpellCatalog.cancellationSpell(44);
  if(mode==2)actor.applyDamage(Integer.MAX_VALUE);if(mode==3)actor.deactivate();if(mode==6)actor.relocate((int)get(pc,"positionX")+40,(int)get(pc,"positionY"));if(mode==7)set(pc,"playerMp",0);
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"actual prior action window");}
   int mp=(int)get(pc,"playerMp"),speed=actor.snapshot().moveSpeed();boolean accepted=mode<=1;cast(pc,44,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,44,row):0)&&pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"companion cost exactly once after admission mode="+mode);
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(44)==accepted,"companion reuse follows admission");
   need(actor.snapshot().moveSpeed()==(accepted?BattleTimedStatus.NORMAL:speed),"actual companion status follows material commit");
   var self=collect(wire);var peers=collect(other);if(mode==4)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"one visible reagent rejection");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and no receipt leak");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(boolean kind:new boolean[]{false,true})for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=0;petKind=kind;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+kind+"-"+skill+"-"+mode);Files.createDirectories(r);
   var cfg=copy(base,Map.of("weaponBreakFixtureD100",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
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
