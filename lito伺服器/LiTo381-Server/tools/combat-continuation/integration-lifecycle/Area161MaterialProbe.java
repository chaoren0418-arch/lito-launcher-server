package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public class Area161MaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={161};
 static Object spell(int skill){return skill==161?BattleSpellCatalog.areaSilenceSpell(skill):BattleSpellCatalog.windShackleSpell(skill);}
 static byte[] packet(int skill,long target){var w=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7);if(skill==167)w.d(target).h(0).h(0);return w.bytes();}
 static boolean active(ClientSession target,int skill)throws Exception{var status=(BattleTimedStatus)get(target,"timedStatus");return skill==161?status.areaSilenceActive(System.nanoTime()):((BattleWindShackleStatus)get(target,"windShackleStatus")).snapshot(System.nanoTime()).active();}
 static void castArea(ClientSession pc,ClientSession target,int skill,Object row,int roll,boolean packet)throws Exception{
  byte[] payload=packet(skill,target.playerObjectId());
  if(packet)call(pc,"handle",new Class[]{byte[].class},payload);
  else call(pc,skill==161?"useAreaSilenceSpell":"useWindShackleSpell",new Class[]{byte[].class,row.getClass(),int.class},payload,row,roll);
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int skill,int mode)throws Exception{
  CancellationMaterialProbe.setup(pc,peer,hub,world);CancellationMaterialProbe.probability(skill);
  set(pc,"learnedSkills",new HashSet<>(Set.of(2,skill)));set(pc,"elfAttribute",31);
  var members=world?hub.classicWorldMembers((int)get(pc,"classicWorldMapId"),(int)get(pc,"classicWorldInstanceId")):hub.roomMembers((long)get(pc,"currentRoomId"));
  var third=members.stream().filter(p->p!=pc&&p!=peer).findFirst().orElseThrow();
  set(third,"positionX",(int)get(peer,"positionX")+1);set(third,"positionY",(int)get(peer,"positionY"));set(third,"playerHp",4000);set(third,"observer",false);((BattleTimedStatus)get(third,"timedStatus")).clear();call(third,"clearInvisibilityStateSilently",new Class[]{});
  if(mode==2)set(pc,"playerMp",0);if(mode==4)set(peer,"positionX",(int)get(pc,"positionX")+60);if(mode==5)set(third,"observer",true);
  if(mode==6)((BattleTimedStatus)get(peer,"timedStatus")).activateAbsoluteBarrier(System.nanoTime(),60);
  var row=spell(skill);need(row!=null,"area catalog row");int item=value(row,"itemConsumeId"),count=value(row,"itemConsumeCount");need(item>0&&count>0,"actual material skill");if(mode!=1)NativeDeItemPublication.grant(pc,item,count+3);
  try(var self=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc);var thirdWire=new SourceActionPairProbe.Wire(third,pc)){
   if(mode==3){NativeCastMaterialProbe.cast(pc,2);long until=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-until<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"prior Source action window");}
   int mp=(int)get(pc,"playerMp"),expected=cost(pc,skill,row);boolean accepted=mode!=1&&mode!=2&&mode!=3&&!(mode==4&&skill==167);
   castArea(pc,peer,skill,row,mode==8?100:1,mode==7);
   need((int)get(pc,"playerMp")==mp-(accepted?expected:0),"area MP once skill="+skill+" world="+world+" mode="+mode);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==1?0:count+3-(accepted?count:0)),"area material once for all targets");
   if(mode!=7){need(active(peer,skill)==(accepted&&mode!=4&&mode!=6&&mode!=8),"primary per-target probability/immunity skill="+skill+" mode="+mode);need(active(third,skill)==(accepted&&mode!=5&&mode!=8),"third target independent result skill="+skill+" mode="+mode);}
   var a=collect(self);var b=collect(other);collect(thirdWire);
   if(mode==1)need(a.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&b.isEmpty(),"area missing material 299 only");
   need(self.locked+other.locked+thirdWire.locked==0&&NativeAreaMaterialAction.current.get()==null,"all three player writes outside locks; area context restored");
   for(var member:members)need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(member))==0&&SourceActionPairProbe.state(member).queue.isEmpty(),"all member receipts drained");
  }
  cases++;
 }
 public static void main(String[]args)throws Exception{before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeCastMaterial.class,NativeDeItemTransaction.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native handlers and material authority from requested JAR");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<9;mode++){var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,skill,mode);}need(!Files.exists(r.resolve("must-not-write.properties")),"both flags remain memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}need(NativePlayerLifetime.allIdle(),"native lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("self-material-wire.txt"),String.join("\n",wires)+"\n");Files.writeString(output.resolveSibling("self-material-goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output.resolveSibling("self-material-notes.txt"),String.join("\n",notes)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"self_skills\":2,\"material_skills\":2,\"Source_window_bypasses\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);}
}
