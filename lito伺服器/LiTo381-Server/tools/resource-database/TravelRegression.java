package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
public class TravelRegression {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object get(ClientSession s,String n)throws Exception{return GmTravel.get(s,n);}
 static void set(ClientSession s,String n,Object v)throws Exception{GmTravel.set(s,n,v);}
 static Object call(ClientSession s,String n,Class<?>[] t,Object...v)throws Exception{return GmTravel.call(s,n,t,v);}
 static void cmd(ClientSession s,String text)throws Exception{check((boolean)call(s,"handleGmChatCommand",new Class[]{String.class},text),"command dispatch");}
 static void use(ClientSession s,byte[] p)throws Exception{call(s,"useInventoryItem",new Class[]{byte[].class},p);}
 static void finish(ClientSession s)throws Exception{call(s,"completePortal",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());}
 static ArenaMapCatalog.Point free(ClientSession s,int offset)throws Exception {
  var hub=(GameHub)get(s,"hub");long room=(long)get(s,"currentRoomId");int map=hub.battleMapId(room,s);var arena=ArenaMapCatalog.forMap(map);
  for(int dx=offset;dx<offset+20;dx++)for(int dy=-10;dy<10;dy++){int x=(int)get(s,"positionX")+dx,y=(int)get(s,"positionY")+dy;if(arena.nativeStandingFloor(x,y)&&arena.nativeCanStep(x,y,x+1,y)&&!GmTravel.occupied(s,hub,room,map,x,y)&&!GmTravel.occupied(s,hub,room,map,x+1,y))return new ArenaMapCatalog.Point(x,y);}
  throw new AssertionError("no fixture floor");
 }
 public static void main(String[] args)throws Exception {
  var config=ServerConfig.load(Path.of(args[0]),"standard");var root=Path.of(args[1]);Files.createDirectories(root);GmTravel.load(config);
  InventoryCatalog.Definition wand;
  try(var sources=SharedDatabase.open(config)) {
   var repo=sources.repositoryForClassicSource(config.classicServerNames().get(0));
   try(var c=LiToGmCommands.connection(repo)){wand=(InventoryCatalog.Definition)LiToGmCommands.call(repo,"rewardItemDefinition",new Class[]{java.sql.Connection.class,int.class},c,42501);}
  }
  check(wand!=null&&wand.useType()==5,"SQL spell_long native cursor");
  var level=new AtomicInteger(200);boolean[] enabled={true},fail={false};
  AccountRepository base=new MemoryAccountRepository("travel","travel","",0,1,40,52);
  AccountRepository accounts=(AccountRepository)Proxy.newProxyInstance(TravelRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(proxy,m,a)->{
   if(m.getName().equals("repositoryForClassicSource"))return proxy;
   if(m.getName().equals("accessLevel"))return level.get();
   if(m.getName().equals("gmCommand")){if(fail[0])throw new java.sql.SQLException("fixture");if(!enabled[0])return null;String n=(String)a[0];return new GmCommandDefinition(n,200,n.equals("loc")?"L1Loc":"L1Move","",false);}
   try{return m.invoke(base,a);}catch(InvocationTargetException e){throw e.getCause();}
  });
  var state=new GameStateStore(root.resolve("unused"),false,true);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   NativeDirectoryRegression.init(f,"GmA",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(peer,"GmB",0,config.classicServerNames().get(0));set(f.session,"account","travel");
   long room=hub.createRoom(0,new GameHub.RoomSettings("Travel","",8,1,0,0,0,0));hub.joinRoom(room,f.session);hub.joinRoom(room,peer.session);
   var prepare=Regression.class.getDeclaredMethod("prepareBattle",ClientSession.class,long.class,int.class,int.class);prepare.setAccessible(true);
   var arena=ArenaMapCatalog.forMap(hub.battleMapId(room,f.session));var origin=arena.spawn(0);prepare.invoke(null,f.session,room,origin.x(),origin.y());prepare.invoke(null,peer.session,room,origin.x()+1,origin.y());
   var cipher=new Protocol.CipherState(0x37110001);var otherCipher=new Protocol.CipherState(0x37110001);f.client.setSoTimeout(30);peer.client.setSoTimeout(30);
   NativeDirectoryRegression.drain(f,cipher);NativeDirectoryRegression.drain(peer,otherCipher);
   cmd(f.session,".loc");check(NativeDirectoryRegression.drain(f,cipher).stream().anyMatch(p->new String(p,Protocol.LEGACY_TEXT).contains("地圖="+arena.mapId())),"loc visible map response");
   var target=free(f.session,3);String move=".move "+target.x()+" "+target.y();
   level.set(0);SourceAdministration.clear(f.session);cmd(f.session,move);check(get(f.session,"pendingPortal")==null,"non-GM moved");
   level.set(200);SourceAdministration.clear(f.session);enabled[0]=false;cmd(f.session,move);check(get(f.session,"pendingPortal")==null,"disabled command moved");enabled[0]=true;
   fail[0]=true;cmd(f.session,move);check(get(f.session,"pendingPortal")==null,"SQL failure moved");fail[0]=false;
   for(String invalid:List.of(".move 1 1",".move 999999 1",".move a b",move+" 4", ".move "+get(peer.session,"positionX")+" "+get(peer.session,"positionY"))) {cmd(f.session,invalid);check(get(f.session,"pendingPortal")==null,"invalid/occupied/cross-map moved: "+invalid);}
   cmd(f.session,move);check(get(f.session,"pendingPortal")!=null,"move not admitted");check((int)get(f.session,"positionX")==origin.x(),"position changed before acknowledgement");
   finish(f.session);check((int)get(f.session,"positionX")==target.x()&&(int)get(f.session,"positionY")==target.y(),"move completion position");check(get(f.session,"pendingPortal")==null,"pending portal leaked");
   var frames=NativeDirectoryRegression.drain(f,cipher);check(frames.stream().anyMatch(p->(p[0]&255)==0x0b),"self actor not rebuilt");check(!NativeDirectoryRegression.drain(peer,otherCipher).isEmpty(),"peer not notified");
   var inv=new RuntimeInventory();check(inv.add(wand,1),"item fixture");set(f.session,"inventory",inv);
   long id=inv.items().get(0).objectId();var next=free(f.session,3);int hp=(int)get(f.session,"playerHp"),mp=(int)get(f.session,"playerMp");
   byte[] packet=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(id).d(0).h(next.x()).h(next.y()).bytes();
   set(f.session,"observer",true);use(f.session,packet);check(get(f.session,"pendingPortal")==null,"observer traveled");set(f.session,"observer",false);
   use(f.session,Arrays.copyOf(packet,packet.length-1));check(get(f.session,"pendingPortal")==null,"malformed traveled");
   use(f.session,packet);check(get(f.session,"pendingPortal")!=null,"Storm_Walk not admitted");use(f.session,packet);finish(f.session);
   check((int)get(f.session,"positionX")==next.x()&&(int)get(f.session,"positionY")==next.y(),"Storm_Walk completion");check(inv.countCatalog(42501)==1&&hp==(int)get(f.session,"playerHp")&&mp==(int)get(f.session,"playerMp"),"unexpected item/HP/MP consumption");
   check(inv.item(id).chargeCount()==0,"unexpected wand charge");
   var raw=((Map<?,?>)NativeDirectoryRegression.get(hub,"rooms")).get(room);NativeDirectoryRegression.set(raw,"battleStarted",true);
   call(f.session,"move",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_MOVE).h(next.x()).h(next.y()).c(2).bytes());
   check((int)get(f.session,"positionX")==next.x()+1,"movement did not resume after teleport");NativeDirectoryRegression.set(raw,"battleStarted",false);
   NativeDirectoryRegression.drain(f,cipher);NativeDirectoryRegression.drain(peer,otherCipher);
   System.out.println("PASS SQL classname/use_type; loc/move permissions and SQL denial; same-map terrain/occupied/bounds; portal acknowledgement and actor rebuild; peer notification; Storm_Walk coordinate item, observer/malformed/repeated request gates, no item/HP/MP consumption; no source SQL writes.");
  }
 }
}
