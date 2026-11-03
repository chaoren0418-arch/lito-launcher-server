package local.lito381.server;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Production protocol/hub with synthetic source accounts. No player SQL writes. */
public class NativeBattleFlowFixture {
 static volatile int job=5,sex=0; static String configuredFirstSource;
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object invoke(Object o,String n,Class<?>[] t,Object...a)throws Exception{var m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(o,a);}

 static CharacterSummary variant(CharacterSummary old, String source) throws Exception {
  var fields=CharacterSummary.class.getRecordComponents();
  Class<?>[] types=new Class<?>[fields.length];Object[] values=new Object[fields.length];
  for(int i=0;i<fields.length;i++) {
   var f=fields[i];types[i]=f.getType();values[i]=switch(f.getName()) {
    case "objectId" -> old.objectId() + (source.equals(configuredFirstSource) ? 0L : 100000L); case "type" -> job; case "sex" -> sex;
    case "classId" -> (long)new int[][]{{0,1},{61,48},{138,37},{734,1186},{2786,2796},{6658,6661},{6671,6650}}[job][sex];
    default -> f.getAccessor().invoke(old);
   };
  }
  return CharacterSummary.class.getDeclaredConstructor(types).newInstance(values);
 }
 public static void main(String[] args) throws Exception {
  var config=ServerConfig.load(Path.of(args[0]));
  configuredFirstSource=config.classicServerNames().get(0);
  var routes=new LinkedHashMap<String,AccountRepository>();
  for(String name:config.classicServerNames()) {
   var base=new MemoryAccountRepository("qatest","testpass","",0,1.0,40,52);
   var proxy=(AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class<?>[]{AccountRepository.class},(p,m,a)->{
    try {
     if(m.getName().equals("repositoryForClassicSource")) return p;
     if(m.getName().equals("learnedSkills")) {
      var ids=new LinkedHashSet<Integer>();int start=job<5?1:job==5?181:201,count=job<5?8:job==5?15:20;
      for(int i=0;i<count;i++)ids.add(start+i);
      return Set.copyOf(ids);
     }
     Object[] mapped=a==null?null:a.clone();
     if(mapped!=null && mapped.length>0 && mapped[0] instanceof Long id && id==100001L)mapped[0]=1L;
     Object result=m.invoke(base,mapped);
     if(m.getName().equals("characters")) {
      var converted=new ArrayList<CharacterSummary>();
      for(Object c:(List<?>)result)converted.add(variant((CharacterSummary)c,name));
      System.out.println("FIXTURE_CHARACTERS type="+job+" sex="+sex+" count="+converted.size());System.out.flush();
      return converted;
     }
     return result;
    } catch(InvocationTargetException e){throw e.getCause();}
   });
   routes.put(name,proxy);
  }
  var clients=Executors.newCachedThreadPool();
  try(var accounts=new SourceRoutedAccountRepository(routes);
      var capture=new CaptureLog(Path.of(args[1]));
      var hub=new GameHub(config,new GameStateStore(Path.of(args[1]).resolve("unused.state"),false,true),accounts)) {
   SourceLogin.start(config,accounts,new GameStateStore(Path.of(args[1]).resolve("unused2.state"),false,true),capture,hub,clients);
   System.out.println("NATIVE_FIXTURE_READY no_player_sql=true");System.out.flush();
   var input=new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
   for(String line;(line=input.readLine())!=null && !line.equals("stop");) {
    if(line.equals("positions") || line.startsWith("step ")) {
     var members=hub.connectedSessionsSnapshot().stream().filter(x->{try{return get(x,"phase").toString().equals("BATTLE");}catch(Exception e){throw new RuntimeException(e);}}).sorted(Comparator.comparing(x->{try{return get(x,"selectedClassicServerName").toString();}catch(Exception e){throw new RuntimeException(e);}})).toList();
     if(line.startsWith("step ")) {
      var mover=members.get(0);int heading=Integer.parseInt(line.split(" ")[1]),x=(int)get(mover,"positionX"),y=(int)get(mover,"positionY");
      invoke(mover,"move",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_MOVE).h(x).h(y).c(heading).bytes());
      // This fixture invokes the real server handler without native prediction;
      // synchronize its mover explicitly, while the witness uses the ordinary broadcast.
      long id=(long)invoke(mover,"playerObjectId",new Class[]{});
      invoke(mover,"sendEncrypted",new Class[]{byte[].class,String.class},NativeMovement.destination(id,(int)get(mover,"positionX"),(int)get(mover,"positionY"),(int)get(mover,"playerHeading")),"fixture mover synchronization");
     }
     for(var member:members)System.out.println("POSITION "+invoke(member,"playerObjectId",new Class[]{})+" "+get(member,"positionX")+" "+get(member,"positionY"));
     System.out.println("MOVEMENT_DONE "+line);System.out.flush();
    } else if(line.startsWith("directory ")) {
     var fields=line.split(" ",4);String action=fields[1];int index=Integer.parseInt(fields[2]);
     var members=hub.connectedSessionsSnapshot().stream().sorted(Comparator.comparing(x->{try{return get(x,"selectedClassicServerName").toString();}catch(Exception e){throw new RuntimeException(e);}})).toList();
     var session=members.get(index);
     if(action.equals("create") || action.equals("rename")) {
      byte[] payload=new Protocol.Writer().c(action.equals("create")?0xcc:0xdd).s(fields[3]).c(8).s("").h(action.equals("create")?4:6).bytes();
      invoke(session,action.equals("create")?"createRoom":"editRoom",new Class[]{byte[].class},payload);
     } else if(action.equals("leave")) invoke(session,"exitRoom",new Class[]{});
     System.out.println("DIRECTORY_DONE "+line);System.out.flush();
    } else if(line.equals("pair")) {
     var members=hub.connectedSessionsSnapshot().stream().filter(x->{try{return Set.of("CHANNEL","LOBBY").contains(get(x,"phase").toString());}catch(Exception e){throw new RuntimeException(e);}}).toList();
     if(members.size()!=2)throw new IllegalStateException("pair size="+members.size());
     long room=hub.createRoom(0,new GameHub.RoomSettings("NativePair","",2,1,0,0,0,0));
     int force=0;
     for(var session:members) {
      invoke(session,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(Protocol.C_LITO_JOIN).d(room).s("").bytes(),"fixture-pair");
      invoke(session,"setForce",new Class[]{byte[].class},(Object)new byte[]{(byte)0xd3,(byte)force++});
     }
     System.out.println("PAIR_READY");System.out.flush();
    } else if(line.equals("startpair")) {
     var members=hub.connectedSessionsSnapshot().stream().filter(x->{try{return get(x,"phase").toString().equals("ROOM");}catch(Exception e){throw new RuntimeException(e);}}).toList();
     for(var session:members) {
      invoke(session,"setReady",new Class[]{byte[].class},(Object)new byte[]{(byte)0xd2,1});
      Thread.sleep(300);
      invoke(session,"setReady",new Class[]{byte[].class},(Object)new byte[]{(byte)0xd2,0});
     }
     for(var session:members)invoke(session,"setReady",new Class[]{byte[].class},(Object)new byte[]{(byte)0xd2,1});
    } else if(line.equals("draw")) {
     var members=hub.connectedSessionsSnapshot().stream().filter(x->{try{return get(x,"phase").toString().equals("BATTLE");}catch(Exception e){throw new RuntimeException(e);}}).toList();
     long room=(long)get(members.get(0),"currentRoomId");
     var result=hub.forceWinner(room,-1);
     for(var session:members)session.serverEndBattle(result);
     hub.endBattle(room);
     System.out.println("DRAW_SENT");System.out.flush();
    } else if(line.equals("world") || line.equals("room")) {
     if(line.equals("world")) for(var session:hub.connectedSessionsSnapshot()) if(get(session,"phase").toString().equals("ROOM")) {
      invoke(session,"setReady",new Class[]{byte[].class},(Object)new byte[]{(byte)0xD2});
     }
     for(var session:hub.connectedSessionsSnapshot()) if(get(session,"phase").toString().equals("CHANNEL")) {
      long room=hub.createRoom(0,new GameHub.RoomSettings("NativeFixture","",2,1,0,0,0,0));
      invoke(session,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(Protocol.C_LITO_JOIN).d(room).s("").bytes(),"fixture");
      if(line.equals("world")){Thread.sleep(1000);invoke(session,"setReady",new Class[]{byte[].class},(Object)new byte[]{(byte)0xD2});}
      System.out.println("WORLD_READY");System.out.flush();
     }
    } else if(line.equals("summaryone")) {
     for(var session:hub.connectedSessionsSnapshot()) if(get(session,"phase").toString().equals("RESULT")) {
      invoke(session,"quitFight",new Class[]{});break;
     }
     System.out.println("ONE_RESULT_RETURNED");System.out.flush();
    } else if(line.equals("result") || line.equals("summary")) {
     for(var session:hub.connectedSessionsSnapshot()) {
      String phase=get(session,"phase").toString();
      if(line.equals("result") && phase.equals("BATTLE")) {
       set(session,"battleCombatExitDeadlineNanos",0L);
       invoke(session,"quitFight",new Class[]{});
      } else if(line.equals("summary") && phase.equals("RESULT")) {
       invoke(session,"quitFight",new Class[]{});
      }
     }
     System.out.println("FIXTURE_TRANSITION "+line);System.out.flush();
    } else if(line.startsWith("empty ")) {
     int walk=Integer.parseInt(line.split(" ")[1]);
     for(var session:hub.connectedSessionsSnapshot()) if(get(session,"phase").toString().equals("BATTLE")) {
      invoke(session,"broadcastWieldState",new Class[]{int.class,String.class},walk,"fixture");
      Thread.sleep(500);
      int x=(int)get(session,"positionX"),y=(int)get(session,"positionY");
      EmptyMelee.handle(session,new Protocol.AttackRequest(0,x+1,y),"NORMAL");
      System.out.println("EMPTY_SENT "+walk);System.out.flush();
     }
    } else if(line.startsWith("class ")) {
     var tokens=line.split(" ");int j=Integer.parseInt(tokens[1]),s=Integer.parseInt(tokens[2]);
     if(j<0 || j>6 || s<0 || s>1)throw new IllegalArgumentException();
     job=j;sex=s;System.out.println("CLASS_READY "+job+" "+sex);System.out.flush();
    }
   }
  } finally {clients.shutdownNow();clients.awaitTermination(5,TimeUnit.SECONDS);}
 }
}
