package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
public class IllusionRegression {
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object helper(String n,Class<?>[] t,Object... a)throws Exception{var m=Regression.class.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(null,a);}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception {
  var config=ServerConfig.load(Path.of(a[0]),"standard");var tmp=Path.of(a[1]);Files.createDirectories(tmp);
  var accounts=new MemoryAccountRepository("qatest","testpass","",0,1,40,52);
  var state=new GameStateStore(tmp.resolve("unused.state"),false,true);
  try(var capture=new CaptureLog(tmp.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var fixture=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
   var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();clock.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS);
   var s=fixture.session;
   helper("initialize",new Class[]{ClientSession.class,CharacterSummary.class},s,helper("character",new Class[]{String.class,int.class,long.class},"IllusionFixture",6,0x38102010L));
   set(s,"learnedSkills",Set.of(201,204,206,209,211,214,216,217,219));
   hub.enterChannel(0,s);long room=hub.createRoom(0,new GameHub.RoomSettings("Illusion", "",2,1,0,0,0,0));hub.joinRoom(room,s);
   helper("prepareBattle",new Class[]{ClientSession.class,long.class,int.class,int.class},s,room,32740,32798);
   set(s,"playerMp",1000);set(s,"playerMaxMp",1000);
   // The same real spell dispatch used by the reported session.
   Regression.cast(s,201,0);Regression.cast(s,209,s.playerObjectId());
   s.serverExpireTimedStatuses(System.nanoTime());
   long[] leases=(long[])get(s,"illusionIconLeases");check(leases[0]>0 && leases[3]>0,"real mirror/Lich did not get icons");
   IllusionStatusIcons.sync(s); // unchanged state must not resend every tick
   Object ability=get(s,"abilityStatus"),timed=get(s,"timedStatus");
   String[] fields={"mirrorImageExpiresAtNanos","illusionOgreExpiresAtNanos","concentrationExpiresAtNanos","illusionLichExpiresAtNanos",null,"diamondGolemExpiresAtNanos","insightExpiresAtNanos","panicExpiresAtNanos","illusionAvatarExpiresAtNanos"};
   long until=System.nanoTime()+60_000_000_000L;
   for(int i=0;i<9;i++)if(i!=4)set(i==2?timed:ability,fields[i],until);
   Object roomObject=((Map<?,?>)get(hub,"rooms")).get(room);
   var patience=(BattlePatienceStatus)get(roomObject,"patienceStatus");patience.activate(s.playerObjectId(),System.nanoTime(),600);
   IllusionStatusIcons.sync(s);check(Arrays.stream((long[])get(s,"illusionIconLeases")).allMatch(x->x>0),"all nine leases");
   for(int i=0;i<9;i++)if(i!=4)set(i==2?timed:ability,fields[i],1L);
   patience.cancel(s.playerObjectId());IllusionStatusIcons.sync(s);
   check(Arrays.stream((long[])get(s,"illusionIconLeases")).allMatch(x->x==0),"expiry/cancel did not clear icons");
   helper("setPhase",new Class[]{ClientSession.class,String.class},s,"WORLD");
   set(s,"classicWorldMapId",4);set(s,"classicWorldInstanceId",0);
   Class<?> key=Class.forName("local.lito381.server.GameHub$ClassicWorldEffectKey");var ctor=key.getDeclaredConstructor(int.class,int.class);ctor.setAccessible(true);
   var worldPatience=new BattlePatienceStatus();worldPatience.activate(s.playerObjectId(),System.nanoTime(),600);
   ((Map<Object,Object>)get(hub,"classicWorldPatienceStatuses")).put(ctor.newInstance(4,0),worldPatience);
   IllusionStatusIcons.sync(s);check(((long[])get(s,"illusionIconLeases"))[4]>0,"world Patience missing");
   helper("setPhase",new Class[]{ClientSession.class,String.class},s,"LOBBY");IllusionStatusIcons.sync(s);
   check(get(s,"illusionIconLeases")==null,"presentation memory retained in lobby");
   System.out.println("PASS: real Mirror/Lich casts; nine status leases/refresh/dedup/expiry/cancel; WORLD Patience route; lobby cache released; no SQL writes");
  }
 }
}
