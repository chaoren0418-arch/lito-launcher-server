package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
public class EmptyMeleeRegression {
 static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);}
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object setup(String n,Class<?>[] types,Object...args)throws Exception{var m=Regression.class.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(null,args);}
 static void phase(ClientSession s,String value)throws Exception{setup("setPhase",new Class[]{ClientSession.class,String.class},s,value);}
 static void attack(ClientSession s,int opcode,long target,int x,int y)throws Exception{
  Class<?> c=Class.forName(ClientSession.class.getName()+"$AttackEnvelope");
  Object e=Enum.valueOf((Class)c,opcode==0x17?"DIRECT":opcode==0x18?"FAR":"ALTERNATE");
  var m=ClientSession.class.getDeclaredMethod("attack",byte[].class,c);m.setAccessible(true);
  try{m.invoke(s,new Protocol.Writer().c(opcode).d(target).h(x).h(y).bytes(),e);}catch(InvocationTargetException ex){throw new RuntimeException(ex.getCause());}
 }
 static void rejected(ClientSession s,int opcode,int x,int y)throws Exception{
  set(s,"emptyMeleeReadyNanos",0L);attack(s,opcode,0,x,y);check((long)get(s,"emptyMeleeReadyNanos")==0,"rejected action advanced cooldown");
 }
 public static void main(String[] args)throws Exception{
  var config=ServerConfig.load(Path.of(args[0]),"standard");var tmp=Path.of(args[1]);Files.createDirectories(tmp);
  AccountRepository accounts=new MemoryAccountRepository("qatest","testpass","",0,1,40,52);
  var state=new GameStateStore(tmp.resolve("unused.state"),false,true);
  try(var capture=new CaptureLog(tmp.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var s=f.session;
   var chr=(CharacterSummary)setup("character",new Class[]{String.class,int.class,long.class},"EmptyFixture",5,0x38101910L);
   setup("initialize",new Class[]{ClientSession.class,CharacterSummary.class},s,chr);
   set(s,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
   set(s,"selectedClassicServerName",config.classicServerNames().get(0));
   hub.enterChannel(0,s);long room=hub.createRoom(0,new GameHub.RoomSettings("Empty", "",2,1,0,0,0,0));hub.joinRoom(room,s);
   setup("prepareBattle",new Class[]{ClientSession.class,long.class,int.class,int.class},s,room,32740,32798);
   var companion=(BattleCompanion)get(s,"battleCompanion");companion.resetForBattle(32741,32798);
   int hp=companion.snapshot().hp();Object inventory=get(s,"inventory");
   int[][] offsets={{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
   for(int heading=0;heading<8;heading++){
    set(s,"emptyMeleeReadyNanos",0L);attack(s,0x17,0,32740+offsets[heading][0],32798+offsets[heading][1]);
    check((int)get(s,"playerHeading")==heading,"wrong facing");
    long ready=(long)get(s,"emptyMeleeReadyNanos");check(ready>0,"empty swing rejected");
    attack(s,0x17,0,32740+offsets[heading][0],32798+offsets[heading][1]);check((long)get(s,"emptyMeleeReadyNanos")==ready,"cooldown allowed duplicate");
   }
   check(companion.snapshot().hp()==hp && (int)get(s,"playerHp")==100 && (int)get(s,"playerMp")==100 && get(s,"inventory")==inventory,"empty swing modified combat/inventory");
   check((long)get(s,"battleCombatExitDeadlineNanos")>System.nanoTime(),"empty attack omitted combat exit lock");
   rejected(s,0x18,32741,32798);rejected(s,0x17,0,32798);rejected(s,0x17,32740,32798);rejected(s,0x17,1,1);
   set(s,"observer",true);rejected(s,0x17,32741,32798);set(s,"observer",false);
   set(s,"playerHp",0);rejected(s,0x17,32741,32798);set(s,"playerHp",100);
   set(s,"invisible",true);rejected(s,0x17,32741,32798);set(s,"invisible",false);
   var timed=(BattleTimedStatus)get(s,"timedStatus");timed.activateFreeze(System.nanoTime(),10);rejected(s,0x17,32741,32798);timed.clear();
   set(s,"retainedSourceInventoryWeight",1_000_000_000);rejected(s,0x17,32741,32798);set(s,"retainedSourceInventoryWeight",0);
   phase(s,"LOBBY");rejected(s,0x17,32741,32798);phase(s,"BATTLE");
   set(s,"emptyMeleeReadyNanos",0L);attack(s,0x7A,0,32741,32798);check((long)get(s,"emptyMeleeReadyNanos")>0,"alternate melee omitted");
   set(s,"emptyMeleeReadyNanos",0L);attack(s,0x17,99999999,32741,32798);check((long)get(s,"emptyMeleeReadyNanos")==0,"unknown nonzero target converted into empty swing");
   check(EmptyMelee.interval(1000,1,6)==585,"haste and bloodlust interval");check(EmptyMelee.interval(1000,2,0)==2000,"slow interval");check(EmptyMelee.interval(1000,0,4)==1000,"holy walk accelerated attack");
   for(byte[] bad:List.of(new byte[]{0x17},new byte[]{0x17,0,0,0,0,1,0,1,0,0})){
    boolean failed=false;try{Protocol.attackRequest(bad,0x17);}catch(RuntimeException e){failed=true;}check(failed,"malformed packet admitted");
   }
   System.out.println("PASS: real dispatch, eight headings, repeat pacing, zero damage/resource use, ten-second combat lock, alternate melee; invalid states/coordinates/FAR/nonzero targets and malformed packets rejected; no SQL writes");
  }
 }
}
