package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual runtime actor, packet and Source idle-order checks; all repositories are memory fixtures. */
public final class StaticMotionProbe extends ReturnNatureMaterialProbe {
 static int checked,kernel;static final List<String> rows=new ArrayList<>();
 static ClassicWorldNpcCatalog.StaticNpc npc(int id)throws Exception{
  var f=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");f.setAccessible(true);var maps=(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)f.get(null);
  return maps.values().stream().flatMap(List::stream).filter(n->n.npcId()==id).findFirst().orElseThrow();
 }
 static void kernel()throws Exception{
  var npc=npc(71092);int[] order={0,7,1,2,6,5,3,4};
  for(int direction=0;direction<8;direction++)for(int pass=0;pass<256;pass++)for(int blocked=0;blocked<16;blocked++)for(int door=-1;door<8;door++){
   var m=new NativeStaticNpcMotion.Motion(npc);m.distance=1;m.direction=direction;int bits=pass,occupied=blocked*17,closed=door,original=direction;
   var terrain=new NativeStaticNpcMotion.Terrain(){int h(int x,int y){return NativeStaticNpcMotion.direction(npc.x(),npc.y(),x,y);}public boolean step(int sx,int sy,int x,int y){return (bits&(1<<h(x,y)))!=0;}public boolean occupied(int x,int y){return (occupied&(1<<h(x,y)))!=0;}public boolean door(int x,int y){return h(x,y)==closed;}public int home(int x,int y,int hx,int hy,int heading){return original;}};
   int wanted=-1;for(int offset:order){int h=(direction+offset)&7;if((pass&(1<<h))!=0&&(occupied&(1<<h))==0){if(h!=door)wanted=h;break;}}
   NativeStaticNpcMotion.idle(m,100,true,1280,terrain,b->{throw new AssertionError("existing random run must not reroll");});
   if(wanted<0)need(m.x==npc.x()&&m.y==npc.y()&&m.due==300_000_100L,"Source checkObject then openDoor blocked");
   else need(m.x==npc.x()+NativeStaticNpcMotion.DX[wanted]&&m.y==npc.y()+NativeStaticNpcMotion.DY[wanted]&&m.heading==wanted&&m.due==1_280_000_100L,"Source eight-way footprint order and movement interval");kernel++;
  }
  for(int id:NativeStaticNpcMotion.NO_AI){var m=new NativeStaticNpcMotion.Motion(copy(npc,Map.of("npcId",id)));NativeStaticNpcMotion.idle(m,100,true,1,null,b->{throw new AssertionError("no-AI must never roll");});need(m.x==npc.x()&&m.y==npc.y()&&m.due==0,"four Source no-AI IDs stay idle at positive speed");kernel++;}
  var zero=new NativeStaticNpcMotion.Motion(copy(npc,Map.of("hp",0)));NativeStaticNpcMotion.idle(zero,100,true,1,null,b->{throw new AssertionError("Source startAI rejects zero HP");});need(zero.due==0&&zero.x==npc.x(),"positive speed cannot start Source zero-HP actor AI");kernel++;
 }
 static void place(ClientSession pc,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance,int x,int y)throws Exception{
  set(pc,"classicWorldMapId",npc.mapId());set(pc,"classicWorldInstanceId",instance);set(pc,"positionX",x);set(pc,"positionY",y);hub.transitionClassicWorld(pc,npc.mapId(),x,y,0,instance);
 }
 static void perceive(ClientSession pc,ClassicWorldNpcCatalog.StaticNpc npc)throws Exception{call(pc,"sendClassicWorldStaticNpc",new Class[]{ClassicWorldNpcCatalog.StaticNpc.class,String.class},npc,"static-motion-fixture");}
 static final Map<SourceActionPairProbe.Wire,Integer> readPositions=new WeakHashMap<>();
 static List<byte[]> packets(SourceActionPairProbe.Wire wire)throws Exception{var all=collect(wire);int previous=readPositions.getOrDefault(wire,0);readPositions.put(wire,all.size());return all.subList(previous,all.size()).stream().map(HexFormat.of()::parseHex).toList();}
 static boolean has(List<byte[]> packets,int op,long id){return packets.stream().anyMatch(p->(p[0]&255)==op&&p.length>=(op==Protocol.S_PUT_OBJECT?9:5)&&NativeStaticNpcMotion.id(p,op==Protocol.S_PUT_OBJECT?5:1)==id);}
 static int word(byte[] p,int offset){return (p[offset]&255)|(p[offset+1]&255)<<8;}
 static int free(ClientSession pc,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,NativeStaticNpcMotion.Motion m){var map=ClassicWorldMapCatalog.forMap(npc.mapId());for(int h=0;h<8;h++){int x=m.x+NativeStaticNpcMotion.DX[h],y=m.y+NativeStaticNpcMotion.DY[h];if(map.canStep(m.x,m.y,x,y)&&!NativeStaticNpcMotion.occupies(npc.mapId(),x,y,pc)&&!hub.classicWorldMonsterOccupies(npc.mapId(),NativeStaticNpcScene.instance(pc),x,y)&&hub.classicWorldMembers(npc.mapId(),NativeStaticNpcScene.instance(pc)).stream().noneMatch(p->(int)CombatAccess.field(p,"positionX")==x&&(int)CombatAccess.field(p,"positionY")==y))return h;}throw new AssertionError("real NPC has no free neighboring cell");}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int mode)throws Exception{
  setup(pc,peer,hub,true);var npc=npc(71092);for(var member:List.of(pc,peer))place(member,hub,npc,-1,npc.x()+2,npc.y()+2);
  try(var a=new SourceActionPairProbe.Wire(pc,peer);var b=new SourceActionPairProbe.Wire(peer,pc)){
   perceive(pc,npc);perceive(peer,npc);var initial=packets(a);packets(b);need(has(initial,Protocol.S_PUT_OBJECT,npc.objectId()),"actual initial PUT");
   var state=NativeStaticNpcMotion.state(hub,false);need(state!=null,"initial perception establishes owned motion");var key=new NativeStaticNpcScene.Key(npc.mapId(),-1,npc.objectId());var m=state.actors.get(key);need(m!=null,"actual L1Quest motion identity");int h=free(pc,hub,npc,m),oldX=m.x,oldY=m.y;
   m.distance=1;m.direction=h;m.due=0;
   if(mode==1){set(peer,"positionX",npc.x()+50);set(peer,"positionY",npc.y()+50);}
   if(mode==2){NativePlayerCommit.advance(peer);set(peer,"positionX",npc.x()+50);set(peer,"positionY",npc.y()+50);}
   NativeStaticNpcMotion.tick(hub);var own=packets(a);var other=packets(b);need(m.x==oldX+NativeStaticNpcMotion.DX[h]&&m.y==oldY+NativeStaticNpcMotion.DY[h],"actual existing clock tick lands in free Source heading");
   var move=own.stream().filter(p->(p[0]&255)==Protocol.S_MOVE_OBJECT&&NativeStaticNpcMotion.id(p,1)==npc.objectId()).findFirst().orElseThrow(()->new AssertionError("expected native Source movement, got "+own.stream().map(HexFormat.of()::formatHex).toList()));need(word(move,5)==oldX&&word(move,7)==oldY&&(move[9]&255)==h,"MOVE wire carries before coordinates");
   need(!has(own,Protocol.S_PUT_OBJECT,npc.objectId()),"clock and existing perception do not duplicate first PUT");
   if(mode==1)need(has(other,Protocol.S_REMOVE_OBJECT,npc.objectId()),"old viewer leaves current screen");
   if(mode==2)need(!has(other,Protocol.S_MOVE_OBJECT,npc.objectId())&&!has(other,Protocol.S_REMOVE_OBJECT,npc.objectId()),"role retirement discards old viewer knowledge");
   need(NativeStaticNpcMotion.x(npc,pc)==m.x&&NativeStaticNpcMotion.y(npc,pc)==m.y&&new NativeCancellationNpcStatic.Target(hub,npc,pc).getX()==m.x,"Source target and runtime coordinates agree");need(NativeStaticNpcMotion.occupies(npc.mapId(),m.x,m.y,pc)&&!NativeStaticNpcMotion.occupies(npc.mapId(),oldX,oldY,pc),"collision leaves old spawn and owns current tile");
   if(mode==3){call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_DIALOG).d(npc.objectId()).bytes());need(m.rest==10,"Source talk rest is ten seconds/count");m.due=0;NativeStaticNpcMotion.tick(hub);need(m.x==oldX+NativeStaticNpcMotion.DX[h]&&m.y==oldY+NativeStaticNpcMotion.DY[h],"rest prevents idle move");state.nextRest=System.nanoTime()-1;NativeStaticNpcMotion.tick(hub);need(m.rest==5,"shared five-second timer subtracts five");state.nextRest=System.nanoTime()-1;NativeStaticNpcMotion.tick(hub);need(m.rest==0,"second timer clears rest");}
   need(a.locked+b.locked==0,"every NPC frame is published after Source locks");
   for(var member:new ArrayList<>(hub.classicWorldMembers(npc.mapId(),-1)))hub.leaveClassicWorld(member);need(get(hub,NativeStaticNpcScene.FIELD)==null,"last scene membership releases motion and presentation");need(state.poses.isEmpty()&&state.closed,"retired pose snapshot cannot be reused");rows.add("mode="+mode+" from="+oldX+","+oldY+" to="+m.x+","+m.y);checked++;
  }finally{readPositions.clear();}
 }
 public static void main(String[] args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,NativeStaticNpcMotion.class,NativeStaticNpcScene.class,ClassicWorldNpcCatalog.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"single candidate class authority");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);kernel();var root=Path.of(args[1]);
  try{for(boolean flag:new boolean[]{false,true})for(int mode=0;mode<4;mode++){
   var r=root.resolve(flag+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);exercise(p.session,m.session,hub,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"persistence setting cannot write memory fixture");
  }}finally{NativePlayerCommit.closeFixture();}need(NativePlayerLifetime.allIdle(),"all runtime lifetimes idle");Files.write(Path.of(args[2]).resolveSibling("cases.txt"),rows);Files.writeString(Path.of(args[2]),"{\"cases\":"+checked+",\"kernel_cases\":"+kernel+",\"checks\":"+ownChecks+",\"full_goal_achieved\":false}\n");
 }
}
