package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Observational audit, not a claim that the current answer lifetime is qualified. */
public final class ResurrectionAnswerLifetimeProbe extends ResurrectionMaterialProbe {
 static int nativeGaps,directGaps;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);int change=mode/2;boolean nativeEntry=mode%2==0;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   cast(pc,skill,row);need(active(peer,skill),"actual native cast installed question before transition");
   if(change==1)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
   else if(change==2)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
   else if(change==3)set(pc,"phase",Enum.valueOf((Class)get(pc,"phase").getClass(),"LOBBY"));
   int question=(int)call(row,"questionId",new Class[]{});
   if(nativeEntry)answer(peer,question,1);else call(peer,"answerResurrection",new Class[]{Protocol.AskAnswer.class},new Protocol.AskAnswer(question,1,""));
   boolean revived=(int)get(peer,"playerHp")>0;
   if(change==0)need(revived,"ordinary native/direct answer baseline");
   if(change==3)need(!revived,"existing phase rejection control");
   if((change==1||change==2)&&revived){if(nativeEntry)nativeGaps++;else directGaps++;}
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"audit receipts drained and output unlocked");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=mode;
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
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"stale_native_acceptances\":"+nativeGaps+",\"stale_direct_acceptances\":"+directGaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
