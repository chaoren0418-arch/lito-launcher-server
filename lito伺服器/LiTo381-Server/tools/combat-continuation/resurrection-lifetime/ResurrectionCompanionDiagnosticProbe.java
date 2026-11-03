package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual own/other companion corpse, native resurrection packet and original filters. */
public final class ResurrectionCompanionDiagnosticProbe extends ResurrectionMaterialProbe {
 static BattleCompanion summoned(ClientSession owner,int x,int y)throws Exception {
  var pet=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());
  pet.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));pet.cancelHaste();
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception {
  var state=new GameStateStore.PetState("fixture-pet",0x70001652L,0x90001652L,(int)InventoryCatalog.PET_COLLAR,45034,"ResurrectionPet",12,78,17,21375,0);
  var pet=BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),x,y);
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);set(peer,"playerHp",4000);int skill=selectedSkill;Object row=spell(skill);
  ClientSession owner=mode==0?pc:peer;int x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");var pet=mode==8?summoned(owner,x,y):companion(owner,x,y);var original=pet.snapshot();targetId=original.objectId();
  if(mode!=2){pet.applyDamage(Integer.MAX_VALUE);need(pet.snapshot().hp()==0,"actual companion corpse before resurrection");}
  if(mode==3)pet.deactivate();if(mode==6)targetId=0x7feeeeeeL;if(mode==7)pet.relocate((int)get(pc,"positionX")+40,(int)get(pc,"positionY"));
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0&&mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==4&&item==0;
   cast(pc,skill,row);
   System.out.println("DIAGNOSTIC native mp="+get(pc,"playerMp")+" pet="+pet.snapshot()+" frames="+collect(wire));
   byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).bytes();
   call(pc,"useClassicWorldResurrectionSpell",new Class[]{byte[].class,row.getClass()},packet,row);
   System.out.println("DIAGNOSTIC root mp="+get(pc,"playerMp")+" pet="+pet.snapshot()+" frames="+collect(wire));
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"companion resurrection MP mode="+mode+" skill="+skill);
   if(item>0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"companion resurrection material exactly once");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"companion reuse follows admission");
   var after=pet.snapshot();need((after.hp()>0)==(accepted||mode==2),"actual companion revived or unchanged live companion");
   if(accepted)need(after.hp()==(int)call(row,"restoredNpcHp",new Class[]{int.class},original.maxHp()),"restored companion HP rule");
   need(get(pc,"pendingResurrection")==null&&get(peer,"pendingResurrection")==null&&get(pc,"nativeResurrectionAnswerLease")==null&&get(peer,"nativeResurrectionAnswerLease")==null,"companion does not install player confirmation lease");
   if(mode==5)need(wire.raw.size()==bytes,"companion shared Source action window");
   var self=collect(wire);var peers=collect(other);
   if(mode==4&&item>0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"companion missing material299 exactly once");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"companion outputs unlocked and receipt drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false})for(int skill:new int[]{61})for(int mode=0;mode<1;mode++){selectedSkill=skill;selectedMode=0;
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
