package local.lito381.server;
import java.nio.file.*;import java.util.*;
public class MovementRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void fields(ClientSession s,int x,int y)throws Exception {NativeDirectoryRegression.set(s,"positionX",x);NativeDirectoryRegression.set(s,"positionY",y);}
 static void battle(ClientSession s)throws Exception {Object p=NativeDirectoryRegression.get(s,"phase");NativeDirectoryRegression.set(s,"phase",Enum.valueOf((Class)p.getClass(),"BATTLE"));}
 static long puts(List<byte[]> packets){return packets.stream().filter(b->(b[0]&255)==0x0b).count();}
 public static void main(String[] args)throws Exception {
  int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
  for(int h=0;h<8;h++) {
   int targetX=32700+dx[h],targetY=32700+dy[h];byte[] b=NativeMovement.destination(42,targetX,targetY,h);
   var r=new Protocol.Reader(b);check(r.c()==0x12 && r.d()==42,"identity");int sx=r.h(),sy=r.h();check(r.c()==h && r.remaining()==0,"layout");
   check(sx==32700 && sy==32700,"wire must carry origin");check(sx+dx[h]==targetX && sy+dy[h]==targetY,"native end tile");
   byte[] legacy=new Protocol.Writer().c(0x12).d(42).h(targetX).h(targetY).c(h).bytes();check(Arrays.equals(b,NativeMovement.legacyDestinationPacket(legacy)),"scoped binary/source parity");
  }
  var config=ServerConfig.load(Path.of(args[0]),"standard");var root=Path.of(args[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var a=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var b=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME)) {
   NativeDirectoryRegression.init(a,"Owner",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(b,"Peer",0,config.classicServerNames().get(1));
   var ac=new Protocol.CipherState(0x37110001);var bc=new Protocol.CipherState(0x37110001);
   NativeDirectoryRegression.call(a.session,"createRoom",new Class[]{byte[].class},NativeDirectoryRegression.settings(0xcc,"Visibility"));long room=(long)NativeDirectoryRegression.get(a.session,"currentRoomId");
   NativeDirectoryRegression.call(b.session,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(0xcf).d(room).s("").bytes(),"fixture");
   battle(a.session);battle(b.session);fields(b.session,32750,32797);
   int oldX=32720;while(!BattleSpellCatalog.isInNativeScreen(oldX+1,32797,32750,32797))oldX++;
   check(!BattleSpellCatalog.isInNativeScreen(oldX,32797,32750,32797),"entry boundary");
   fields(a.session,oldX+1,32797);NativeDirectoryRegression.drain(a,ac);NativeDirectoryRegression.drain(b,bc);
   byte[] entry=new Protocol.Writer().c(Protocol.C_MOVE).h(oldX).h(32797).c(2).bytes();NativeBattleVisibility.afterMove(a.session,entry);
   check(puts(NativeDirectoryRegression.drain(a,ac))==1 && puts(NativeDirectoryRegression.drain(b,bc))==1,"bilateral reentry actor rebuild");
   fields(a.session,oldX+2,32797);NativeBattleVisibility.afterMove(a.session,new Protocol.Writer().c(Protocol.C_MOVE).h(oldX+1).h(32797).c(2).bytes());
   check(puts(NativeDirectoryRegression.drain(a,ac))==0 && puts(NativeDirectoryRegression.drain(b,bc))==0,"continuous movement must not rebuild actors");
   fields(a.session,oldX,32797);NativeBattleVisibility.afterMove(a.session,new Protocol.Writer().c(Protocol.C_MOVE).h(oldX+1).h(32797).c(6).bytes());
   check(NativeDirectoryRegression.drain(a,ac).stream().anyMatch(p->(p[0]&255)==Protocol.S_REMOVE_OBJECT),"local exit removal");check(NativeDirectoryRegression.drain(b,bc).stream().anyMatch(p->(p[0]&255)==Protocol.S_REMOVE_OBJECT),"peer exit removal");
   NativeDirectoryRegression.set(b.session,"invisible",true);fields(a.session,oldX+1,32797);NativeBattleVisibility.afterMove(a.session,entry);
   check(puts(NativeDirectoryRegression.drain(a,ac))==0,"invisible peer disclosed");check(puts(NativeDirectoryRegression.drain(b,bc))==1,"invisible viewer lost visible actor");
   System.out.println("PASS: all eight native origin/destination directions; binary/source parity; bilateral arena view reentry/removal; no repeated spawn during ordinary step; invisible target preserved");
  }
 }
}
