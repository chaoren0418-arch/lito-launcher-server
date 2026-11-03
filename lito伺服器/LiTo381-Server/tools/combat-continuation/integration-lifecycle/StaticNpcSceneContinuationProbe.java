package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual spell ingress in independent instances; source templates and public world remain unchanged. */
public final class StaticNpcSceneContinuationProbe extends StaticCancellationProbe {
 static final Map<Integer,ClassicWorldNpcCatalog.StaticNpc> fixtures=new HashMap<>();
 static boolean privateGone(GameHub hub,int instance){synchronized(hub){var scene=NativeStaticNpcScene.state(hub,false);var motion=NativeStaticNpcMotion.state(hub,false);return (scene==null||scene.actors.keySet().stream().noneMatch(k->k.instance()==instance))&&(motion==null||motion.actors.keySet().stream().noneMatch(k->k.instance()==instance));}}
 static void prepareNpcs()throws Exception{
  var field=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");field.setAccessible(true);var maps=(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)field.get(null);
  var map=ClassicWorldMapCatalog.forMap(1011);int x=0,y=0;
  outer:for(int sx=map.startX();sx<=map.endX();sx++)for(int sy=map.startY();sy<map.endY();sy++)if(map.standingPassable(sx,sy)&&map.standingPassable(sx,sy+1)){x=sx;y=sy;break outer;}
  need(x!=0,"native instanced map has passable adjacent cells");
  var rows=new ArrayList<>(maps.getOrDefault(1011,List.of()));
  for(int id:new int[]{71092,80013,81209}){var source=npc(id);var placed=copy(source,Map.of("mapId",1011,"x",x,"y",y));fixtures.put(id,placed);rows.add(placed);}
  maps.put(1011,List.copyOf(rows));
 }
 static void scene(ClientSession pc,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance)throws Exception{
  set(pc,"classicWorldMapId",npc.mapId());set(pc,"classicWorldInstanceId",instance);set(pc,"positionX",npc.x());set(pc,"positionY",npc.y()+1);
  hub.transitionClassicWorld(pc,npc.mapId(),npc.x(),npc.y()+1,0,instance);
 }
 static void perceive(ClientSession pc,ClassicWorldNpcCatalog.StaticNpc npc)throws Exception{
  call(pc,"sendClassicWorldStaticNpc",new Class[]{ClassicWorldNpcCatalog.StaticNpc.class,String.class},npc,"fixture-scene");
 }
 static void exerciseScene(ClientSession pc,ClientSession peer,ClientSession remote,ClientSession publicPc,GameHub hub,int mode)throws Exception{
  setup(remote,publicPc,hub,true);setup(pc,peer,hub,true);var npc=fixtures.get(selectedNpc);targetId=npc.objectId();var rule=NativeCancellationNpcRules.lookup(selectedNpc);
  for(var p:List.of(remote,publicPc)){set(p,"classicWorldMapId",npc.mapId());set(p,"positionX",npc.x());set(p,"positionY",npc.y()+1);}
  scene(pc,hub,npc,11);scene(peer,hub,npc,11);scene(remote,hub,npc,22);scene(publicPc,hub,npc,-1);
  var original=NativeCancellationNpcStatic.appearance(hub,npc);
  if(mode==1){need(hub.transformClassicWorldNpcGfx(npc,npc.gfxId(),100),"public pre-existing transformation");need(NativeStaticNpcScene.gfx(hub,npc,pc)==npc.gfxId(),"public transform cannot enter private instance");}
  var row=BattleSpellCatalog.cancellationSpell(44);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();
  if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);if(mode==3)set(pc,"playerMp",0);
  int mp=(int)get(pc,"playerMp");boolean accepted=mode!=2&&mode!=3;
  try(var w=new SourceActionPairProbe.Wire(pc,peer);var p=new SourceActionPairProbe.Wire(peer,pc);var r=new SourceActionPairProbe.Wire(remote,pc);var u=new SourceActionPairProbe.Wire(publicPc,pc)){
   cast(pc,44,row);
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,44,row):0),"private cast MP once");
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"private cast material once");
   need(NativeStaticNpcScene.gfx(hub,npc,pc)==(accepted?rule.targetId():npc.gfxId()),"caster instance owns transformation");
   need(NativeStaticNpcScene.gfx(hub,npc,peer)==NativeStaticNpcScene.gfx(hub,npc,pc),"same instance sees transformation");
   need(NativeStaticNpcScene.gfx(hub,npc,remote)==npc.gfxId(),"other private instance remains original");
   need(hub.classicWorldNpcGfx(npc)==(mode==1?100:npc.gfxId())&&NativeCancellationNpcStatic.appearance(hub,npc).equals(original),"public world remains intact");
   var own=collect(w);var same=collect(p);need(collect(r).isEmpty()&&collect(u).isEmpty(),"no cross-instance effect/action/name/shape packets");
   String fx=HexFormat.of().formatHex(Protocol.effectObject(npc.objectId(),row.effectGfx()));need(own.contains(fx)==accepted&&same.contains(fx)==accepted,"real effect only accepted scene");
   if(mode==2)need(own.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299)))),"missing material visible");
   perceive(remote,npc);var plain=collect(r);need(plain.equals(List.of(HexFormat.of().formatHex(Protocol.classicStaticNpc(npc,npc.heading(),npc.gfxId())))),"late viewer receives isolated native snapshot");
   if(accepted){
    var appearance=NativeStaticNpcScene.appearance(hub,npc,11);need(appearance.name().equals(rule.nameId().isEmpty()?npc.nameId():rule.nameId())&&appearance.speed()==(rule.passispeed()>0?rule.passispeed():npc.passispeed()),"private name and speed projection");
    if(mode==4){
     NativeDeItemPublication.grant(remote,item,count+3);cast(remote,44,row);
     need(NativeStaticNpcScene.gfx(hub,npc,remote)==rule.targetId(),"second instance transforms independently");
     need(NativeStaticNpcScene.restore(hub,npc,rule.targetId(),pc),"private restore accepted");
     need(NativeStaticNpcScene.gfx(hub,npc,pc)==npc.gfxId()&&NativeStaticNpcScene.gfx(hub,npc,remote)==rule.targetId(),"restoration isolated");
    }
    if(mode==5){
     hub.leaveClassicWorld(pc);need(NativeStaticNpcScene.gfx(hub,npc,peer)==rule.targetId(),"other member retains instance state");
     hub.leaveClassicWorld(peer);need(privateGone(hub,11),"last member releases private state");
    }
    if(mode==6){
     scene(pc,hub,npc,33);scene(peer,hub,npc,33);need(privateGone(hub,11),"map transition prunes prior empty scene");
     scene(pc,hub,npc,11);need(NativeStaticNpcScene.gfx(hub,npc,pc)==npc.gfxId(),"reused instance id starts without old form");
    }
    if(mode==8){hub.close();need(get(hub,NativeStaticNpcScene.FIELD)==null&&get(hub,NativeCancellationNpcStatic.FIELD)==null,"actual hub close clears private and public state");}
    if(mode==7){
     int nextHeading=(npc.heading()+1)%8;need(NativeStaticNpcScene.turn(hub,npc,nextHeading,pc),"owned heading change");
     need(NativeStaticNpcScene.heading(hub,npc,peer)==nextHeading&&NativeStaticNpcScene.heading(hub,npc,remote)==npc.heading(),"heading isolation");
     need(NativeStaticNpcScene.restore(hub,npc,rule.targetId(),pc),"restore keeps independent heading");
     need(NativeStaticNpcScene.appearance(hub,npc,11).equals(original)&&NativeStaticNpcScene.heading(hub,npc,pc)==nextHeading,"restore resets name/speed only");
    }
   }
   need(w.locked+p.locked+r.locked+u.locked==0,"no network publication under player ownership");
  }
  hub.leaveClassicWorld(pc);hub.leaveClassicWorld(peer);hub.leaveClassicWorld(remote);hub.leaveClassicWorld(publicPc);need(get(hub,NativeStaticNpcScene.FIELD)==null,"all private state released");cases++;
 }
 public static void main(String[]args)throws Exception {
  before=false;var jar=Path.of(args[3]).toRealPath();
  for(var type:List.of(ClientSession.class,NativeCastMaterial.class,NativeStaticNpcScene.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate producer and material authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);prepareNpcs();var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{true})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{71092,80013,81209})for(int mode=0;mode<9;mode++){selectedNpc=skill;
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
    exerciseScene(p.session,m.session,f.session,o.session,hub,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
  String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(output,result+"\n");System.out.println(result);
 }
}
