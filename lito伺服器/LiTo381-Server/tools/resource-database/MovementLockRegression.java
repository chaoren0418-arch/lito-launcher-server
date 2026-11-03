package local.lito381.server;
import java.nio.file.*;import java.util.*;
public class MovementLockRegression {
 static Object get(Object o,String n)throws Exception{return NativeDirectoryRegression.get(o,n);}
 static void set(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
 static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{return NativeDirectoryRegression.call(o,n,t,a);}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void move(ClientSession s,int x,int y,int h)throws Exception{call(s,"move",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_MOVE).h(x).h(y).c(h).bytes());}
 static void ack(ClientSession s)throws Exception{call(s,"movementUnlock",new Class[]{byte[].class},new byte[]{0x5a});}
 static boolean packet(List<byte[]> rows,byte...p){return rows.stream().anyMatch(b->Arrays.equals(b,p));}
 static void complete(SourceLoginRegression.Fixture f,Protocol.CipherState cipher,String reason)throws Exception{
  var rows=NativeDirectoryRegression.drain(f,cipher);check(packet(rows,(byte)0x63),reason+" did not issue S_BLINK");check((boolean)get(f.session,"movementLockPending"),"not pending");
  ack(f.session);rows=NativeDirectoryRegression.drain(f,cipher);check(rows.size()==1&&packet(rows,(byte)0x25,(byte)17),"missing exact blink release, or unrelated unlock emitted");check(!(boolean)get(f.session,"movementLockPending"),"pending retained");
  ack(f.session);check(NativeDirectoryRegression.drain(f,cipher).isEmpty(),"duplicate acknowledgement sent another unlock");
 }
 public static void main(String[] args)throws Exception{
  var config=ServerConfig.load(Path.of(args[0]),"standard");var root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   NativeDirectoryRegression.init(f,"Owner",0,config.classicServerNames().get(0));var s=f.session;var cipher=new Protocol.CipherState(0x37110001);
   call(s,"createRoom",new Class[]{byte[].class},NativeDirectoryRegression.settings(0xcc,"MovementLockFixture"));long room=(long)get(s,"currentRoomId");NativeDirectoryRegression.drain(f,cipher);MovementRegression.battle(s);
   var arena=ArenaMapCatalog.forMap(hub.battleMapId(room,s));int x=arena.spawn(0).x(),y=arena.spawn(0).y();
   // Find an eastward walkable pair, independent of current SQL map selection.
   boolean found=false;outer:for(int ox=-12;ox<=12;ox++)for(int oy=-12;oy<=12;oy++)if(arena.contains(x+ox+1,y+oy)&&arena.nativeCanStep(x+ox,y+oy,x+ox+1,y+oy)){x+=ox;y+=oy;found=true;break outer;}check(found,"no walkable fixture edge");
   var raw=((Map<?,?>)get(hub,"rooms")).get(room);set(raw,"battleStarted",true);var door=(BattleDoor)get(raw,"door");door.reset(arena,new ArenaMapCatalog.Point(x+1,y));MovementRegression.fields(s,x,y);
   move(s,x,y,2);complete(f,cipher,"closed door");check((int)get(s,"positionX")==x&&(int)get(s,"positionY")==y,"collision moved authoritative position");
   door.deactivate();move(s,x,y,2);NativeDirectoryRegression.drain(f,cipher);check((int)get(s,"positionX")==x+1,"movement not resumed after door correction");
   move(s,x-1,y,2);complete(f,cipher,"stale source");
   MovementRegression.fields(s,arena.maxX(),arena.spawn(0).y());move(s,arena.maxX(),arena.spawn(0).y(),2);complete(f,cipher,"outside map");
   // Coalescing, unsolicited/malformed acknowledgements, and negative statuses.
   call(s,"issueMovementLock",new Class[]{String.class},"fixture");NativeDirectoryRegression.drain(f,cipher);call(s,"issueMovementLock",new Class[]{String.class},"fixture duplicate");check(NativeDirectoryRegression.drain(f,cipher).isEmpty(),"pending lock not coalesced");
   try{call(s,"movementUnlock",new Class[]{byte[].class},new byte[]{0x5a,0});throw new AssertionError("malformed ack accepted");}catch(RuntimeException expected){}check((boolean)get(s,"movementLockPending"),"malformed ack cleared lock");
   var status=(BattleTimedStatus)get(s,"timedStatus");status.activateShockStun(System.nanoTime(),30,30);status.activateParalysis(System.nanoTime(),0,30000);ack(s);var reply=NativeDirectoryRegression.drain(f,cipher);check(reply.stream().filter(b->b[0]==0x25&&(b[1]&1)==1).count()==1&&packet(reply,(byte)0x25,(byte)17),"wrong status release packet");check(status.actionLocked(System.nanoTime()),"real negative status was cleared");
   set(raw,"battleStarted",false);
   System.out.println("PASS: real move rejected closed-door/stale-source/outside-map; exact encrypted 25 11 acknowledgement; subsequent valid move; duplicate/malformed acknowledgement; lock coalescing; stun/paralysis retained; no SQL writes");
  }
 }
}
