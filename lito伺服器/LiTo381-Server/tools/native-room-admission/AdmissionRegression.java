package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class AdmissionRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void noBot(List<byte[]> packets){for(var p:packets)check(!new String(p,StandardCharsets.ISO_8859_1).contains("ArenaBot"),"automatic ArenaBot packet");}
 static void ready(ClientSession s)throws Exception{NativeDirectoryRegression.call(s,"setReady",new Class[]{byte[].class},new byte[]{(byte)0xd2,1});}
 static String phase(ClientSession s)throws Exception{return NativeDirectoryRegression.get(s,"phase").toString();}
 static void battle(ClientSession... sessions)throws Exception{
  long end=System.nanoTime()+20_000_000_000L;while(System.nanoTime()<end){boolean ok=true;for(var s:sessions)ok &= phase(s).equals("BATTLE");if(ok){Thread.sleep(500);return;}Thread.sleep(100);}throw new AssertionError("battle did not start");
 }
 public static void main(String[] args)throws Exception {
  var config=ServerConfig.load(Path.of(args[0]),"standard");var root=Path.of(args[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var a=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var b=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME);
      var lobby=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   NativeDirectoryRegression.init(a,"Owner",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(b,"Peer",0,config.classicServerNames().get(1));NativeDirectoryRegression.init(lobby,"Waiting",0,config.classicServerNames().get(0));
   var ac=new Protocol.CipherState(0x37110001);var bc=new Protocol.CipherState(0x37110001);var lc=new Protocol.CipherState(0x37110001);
   NativeDirectoryRegression.call(a.session,"createRoom",new Class[]{byte[].class},NativeDirectoryRegression.settings(0xcc,"PlayersOnly"));long room=(long)NativeDirectoryRegression.get(a.session,"currentRoomId");
   noBot(NativeDirectoryRegression.drain(a,ac));noBot(NativeDirectoryRegression.drain(b,bc));NativeDirectoryRegression.drain(lobby,lc);
   check(NativeDirectoryRegression.lists.get(lobby).get(room).users()==1,"only creator should occupy room");check(hub.botHp(room)==0,"bot enabled on creation");
   NativeDirectoryRegression.call(b.session,"sendRoomUser",new Class[]{byte[].class},new Protocol.Writer().c(0xcd).d(room).bytes());noBot(NativeDirectoryRegression.drain(b,bc));
   NativeDirectoryRegression.call(b.session,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(0xcf).d(room).s("").bytes(),"admission-test");
   noBot(NativeDirectoryRegression.drain(a,ac));noBot(NativeDirectoryRegression.drain(b,bc));NativeDirectoryRegression.drain(lobby,lc);
   ready(a.session);ready(b.session);battle(a.session,b.session);
   var deleted=NativeDirectoryRegression.drain(lobby,lc);check(deleted.stream().anyMatch(p->{var r=new Protocol.Reader(p);return r.c()==0xcf&&r.d()==room;}),"waiting client not notified at battle start");
   check(hub.roomMembers(room).size()==2,"room destroyed before settlement");check(!NativeDirectoryRegression.lists.get(lobby).containsKey(room),"running room still listed");
   NativeDirectoryRegression.call(lobby.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(lobby,lc);check(!NativeDirectoryRegression.lists.get(lobby).containsKey(room),"refresh restored running room");
   noBot(NativeDirectoryRegression.drain(a,ac));noBot(NativeDirectoryRegression.drain(b,bc));
   var result=hub.forceWinner(room,-1);a.session.serverEndBattle(result);b.session.serverEndBattle(result);hub.endBattle(room);
   NativeDirectoryRegression.call(a.session,"quitFight",new Class[]{});NativeDirectoryRegression.call(a.session,"exitRoom",new Class[]{});NativeDirectoryRegression.call(a.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(a,ac);check(phase(a.session).equals("CHANNEL"),"owner not returned");
   check(hub.roomMembers(room).size()==1,"result member not retained");check(!NativeDirectoryRegression.lists.get(a).containsKey(room),"returning A sees B result room");
   NativeDirectoryRegression.call(lobby.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(lobby,lc);check(!NativeDirectoryRegression.lists.get(lobby).containsKey(room),"result room reappeared");
   NativeDirectoryRegression.call(b.session,"quitFight",new Class[]{});NativeDirectoryRegression.call(b.session,"exitRoom",new Class[]{});NativeDirectoryRegression.call(b.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(b,bc);NativeDirectoryRegression.drain(lobby,lc);check(hub.roomSnapshots(0).stream().noneMatch(r->r.id()==room),"final member cleanup failed");
   // A solo battle must not silently spawn or tick a hidden bot either.
   NativeDirectoryRegression.call(a.session,"createRoom",new Class[]{byte[].class},NativeDirectoryRegression.settings(0xcc,"SoloNoBot"));long solo=(long)NativeDirectoryRegression.get(a.session,"currentRoomId");noBot(NativeDirectoryRegression.drain(a,ac));ready(a.session);battle(a.session);
   var packets=NativeDirectoryRegression.drain(a,ac);noBot(packets);check(hub.botHp(solo)==0,"bot reset at battle start");
   check(((List<?>)NativeDirectoryRegression.call(hub,"botTicks",new Class[]{long.class},System.nanoTime())).isEmpty(),"hidden bot AI still running");
   check(!hub.forceWinner(solo,1).winners().contains(2L),"phantom bot winner");
   System.out.println("PASS: no automatic bot roster/ready/world/AI/winner; start deletes waiting list immediately; running/result refresh cannot restore row; A returns while B result retained; final cleanup; no SQL writes");
  }
 }
}
