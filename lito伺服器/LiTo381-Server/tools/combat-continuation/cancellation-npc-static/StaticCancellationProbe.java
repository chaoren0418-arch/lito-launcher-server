package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual C_USE_SPELL44 into all three configured static NPC rows. */
public class StaticCancellationProbe extends CancellationMaterialProbe {
 static int selectedNpc;
 static ClassicWorldNpcCatalog.StaticNpc npc(int id)throws Exception{
  var f=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");f.setAccessible(true);var map=(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)f.get(null);
  return map.values().stream().flatMap(List::stream).filter(n->n.npcId()==id).findFirst().orElseThrow(()->new AssertionError("configured static NPC absent "+id));
 }
 static ClassicWorldNpcCatalog.StaticNpc setupStatic(ClientSession pc,ClientSession peer,GameHub hub,int id)throws Exception{
  setup(pc,peer,hub,true);var npc=npc(id);for(var p:List.of(pc,peer)){set(p,"classicWorldMapId",npc.mapId());set(p,"positionX",npc.x());set(p,"positionY",npc.y()+1);hub.transitionClassicWorld(p,npc.mapId(),npc.x(),npc.y()+1,0,(int)get(p,"classicWorldInstanceId"));}need(hub.classicWorldMembers(npc.mapId(),(int)get(pc,"classicWorldInstanceId")).containsAll(List.of(pc,peer)),"actual WORLD membership matches fixture scene");targetId=npc.objectId();return npc;
 }
 static void exerciseStatic(ClientSession pc,ClientSession peer,GameHub hub,int mode)throws Exception{
  var npc=setupStatic(pc,peer,hub,selectedNpc);var rule=NativeCancellationNpcRules.lookup(selectedNpc);need(rule!=null&&rule.type()==1,"actual SQL transform rule");
  for(var p:List.of(pc,peer)){set(p,"classicWorldMapId",npc.mapId());set(p,"positionX",npc.x());set(p,"positionY",npc.y()+1);}
  targetId=npc.objectId();var row=BattleSpellCatalog.cancellationSpell(44);
  if(mode==2)set(pc,"playerMp",0);if(mode==3)set(pc,"learnedSkills",new HashSet<Integer>());if(mode==4)set(pc,"positionX",npc.x()+40);
  int initial=mode==5?100:mode==6?rule.targetId():npc.gfxId();if(mode==5||mode==6)need(hub.transformClassicWorldNpcGfx(npc,npc.gfxId(),initial),"fixture pre-existing form");
  if(mode==7)set(pc,"observer",true);if(mode==8)((BattleTimedStatus)get(pc,"timedStatus")).activateSilence(System.nanoTime(),60);
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=1)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==9){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"actual prior action interval");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp");boolean accepted=mode==0||mode==5||mode==6;
   cast(pc,44,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,44,row):0)&&(int)get(pc,"playerHp")==hp,"resource cost once mode="+mode);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==1?0:count+3-(accepted?count:0)),"material once mode="+mode);
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(44)==accepted,"reuse only admitted cast mode="+mode);
   int wanted=mode==0?rule.targetId():initial;need(hub.classicWorldNpcGfx(npc)==wanted,"actual static gfx mode="+mode);
   var appearance=NativeCancellationNpcStatic.appearance(hub,npc);String name=mode==0&&!rule.nameId().isEmpty()?rule.nameId():npc.nameId();int speed=mode==0&&rule.passispeed()>0?rule.passispeed():npc.passispeed();
   need(appearance.name().equals(name)&&appearance.speed()==speed,"stored Source name/speed");
   var framesBefore=collect(wire);var peerFrames=collect(other);
   if(accepted){String fx=HexFormat.of().formatHex(Protocol.effectObject(npc.objectId(),row.effectGfx()));need(framesBefore.contains(fx)&&peerFrames.contains(fx),"actual effect delivered to both admitted viewers");}
   if(mode==1)need(framesBefore.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299)))),"visible reagent rejection");
   if(mode==0){
    var expected=copy(npc,Map.of("nameId",name,"passispeed",speed));
    try(var late=new SourceActionPairProbe.Wire(pc,peer)){
     call(pc,"sendClassicWorldStaticNpc",new Class[]{ClassicWorldNpcCatalog.StaticNpc.class,String.class},npc,"fixture-late-viewer");
     need(collect(late).equals(List.of(HexFormat.of().formatHex(Protocol.classicStaticNpc(expected,hub.classicWorldNpcHeading(npc),wanted)))),"native late-viewer packet keeps new name and shape");
    }
    need(hub.restoreClassicWorldNpcGfx(npc,wanted),"native quest restoration");
    var restored=NativeCancellationNpcStatic.appearance(hub,npc);need(restored.name().equals(npc.nameId())&&restored.speed()==npc.passispeed()&&get(hub,NativeCancellationNpcStatic.FIELD)==null,"quest restore releases appearance state");
   }
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
 public static void main(String[]args)throws Exception {
  before=false;var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class,NativeCancellationNpcStatic.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{71092,80013,81209})for(int mode=0;mode<10;mode++){selectedNpc=skill;
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
    exerciseStatic(p.session,m.session,hub,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
