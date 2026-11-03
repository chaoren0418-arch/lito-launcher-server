package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
public class TargetRegression {
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object helper(String n,Class<?>[] t,Object... a)throws Exception{var m=Regression.class.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(null,a);}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void cast(ClientSession s,int id,long target)throws Exception {
  ((Map<?,?>)get(s,"spellReadyNanos")).clear();
  var w=new Protocol.Writer().c(0x14).c((id-1)>>3).c((id-1)&7);if(target!=0)w.d(target).h(32741).h(32798);
  Regression.call(s,"useSpell",new Class[]{byte[].class},w.bytes());
  check(IllusionSkillRequests.current()==null,"request scope leaked");
 }
 static void init(ClientSession s,String name,long id,long room)throws Exception {
  helper("initialize",new Class[]{ClientSession.class,CharacterSummary.class},s,helper("character",new Class[]{String.class,int.class,long.class},name,6,id));
  helper("prepareBattle",new Class[]{ClientSession.class,long.class,int.class,int.class},s,room,32740,32798);
  set(s,"playerMp",10000);set(s,"playerMaxMp",10000);set(s,"playerHp",10000);set(s,"playerMaxHp",10000);
  Set<Integer> skills=new HashSet<>();for(int i=201;i<=220;i++)skills.add(i);set(s,"learnedSkills",Set.copyOf(skills));
 }
 public static void main(String[] a)throws Exception {
  var config=ServerConfig.load(Path.of(a[0]),"standard");var tmp=Path.of(a[1]);Files.createDirectories(tmp);
  var accounts=new MemoryAccountRepository("qatest","testpass","",0,1,40,52);var state=new GameStateStore(tmp.resolve("unused.state"),false,true);
  try(var capture=new CaptureLog(tmp.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var f2=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
   var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();clock.awaitTermination(5,TimeUnit.SECONDS);
   var s=f.session;var other=f2.session;
   long room=hub.createRoom(0,new GameHub.RoomSettings("Illusion", "",4,1,0,0,0,0));
   init(s,"IllusionCaster",0x38102010L,room);init(other,"ChosenPlayer",0x38102011L,room);
   hub.enterChannel(0,s);hub.enterChannel(0,other);hub.joinRoom(room,s);hub.joinRoom(room,other);
   set(other,"positionX",32741);set(s,"lastCombatTargetObjectId",2L);
   int[] pcs={204,209,214,216,219};String[] fields={"illusionOgreExpiresAtNanos","illusionLichExpiresAtNanos","diamondGolemExpiresAtNanos","insightExpiresAtNanos","illusionAvatarExpiresAtNanos"};
   for(int i=0;i<pcs.length;i++){
    cast(s,pcs[i],other.playerObjectId());check((long)get(get(other,"abilityStatus"),fields[i])>System.nanoTime(),"chosen PC buff "+pcs[i]);
    check((long)get(get(s,"abilityStatus"),fields[i])==0,"PC buff retargeted to self "+pcs[i]);
    cast(s,pcs[i],s.playerObjectId());check((long)get(get(s,"abilityStatus"),fields[i])>System.nanoTime(),"explicit self buff "+pcs[i]);
   }
   Object roomObject=((Map<?,?>)get(hub,"rooms")).get(room);set(roomObject,"battleStarted",true);
   Regression.call(get(roomObject,"match"),"begin",new Class[]{List.class,Map.class,long.class},List.of(s.playerObjectId(),other.playerObjectId()),Map.of(s.playerObjectId(),0,other.playerObjectId(),1),System.nanoTime());
   var companion=(BattleCompanion)get(s,"battleCompanion");companion.resetForBattle(32741,32798);
   ((BattleCompanion)get(other,"battleCompanion")).deactivate();
   cast(s,211,companion.snapshot().objectId());
   check(((BattlePatienceStatus)get(roomObject,"patienceStatus")).active(companion.snapshot().objectId(),System.nanoTime()),"chosen companion Patience");
   cast(s,218,companion.snapshot().objectId());
   for(int id:new int[]{202,203,207,208,213}) {
    companion.resetForBattle(32741,32798);int before=(int)get(s,"playerMp");
    cast(s,id,companion.snapshot().objectId());check((int)get(s,"playerMp")<before,"explicit attack accepted "+id);
   }
   for(int id:new int[]{212,217}) {int before=(int)get(s,"playerMp");cast(s,id,other.playerObjectId());check((int)get(s,"playerMp")<before,"explicit curse accepted "+id);}
   // Unknown, missing and out-of-range targets must never fall back to self/last/unique.
   for(int id:new int[]{202,203,204,207,208,209,211,212,213,214,216,217,218,219}) {
    int before=(int)get(s,"playerMp");cast(s,id,99999999L);check((int)get(s,"playerMp")==before,"unknown target consumed MP "+id);
    cast(s,id,0);check((int)get(s,"playerMp")==before,"missing target guessed "+id);
    set(other,"positionX",32760);cast(s,id,other.playerObjectId());check((int)get(s,"playerMp")==before,"far target accepted "+id);set(other,"positionX",32741);
   }
   set(other,"playerHp",0);int mp=(int)get(s,"playerMp");cast(s,204,other.playerObjectId());check((int)get(s,"playerMp")==mp,"dead target accepted");
   ((BattleRules.MatchState)get(roomObject,"match")).reset();set(roomObject,"battleStarted",false);
   hub.leaveRoom(room,other);set(roomObject,"battleStarted",true);
   ((BattleRules.MatchState)get(roomObject,"match")).begin(List.of(s.playerObjectId()),Map.of(s.playerObjectId(),0),System.nanoTime());
   var bot=hub.botPosition(room);set(s,"positionX",bot.x()-1);set(s,"positionY",bot.y());
   int botHp=hub.botHp(room);cast(s,208,2L);check(hub.botHp(room)<botHp,"selected ArenaBot Bone Break did not damage");
   ((BattleRules.MatchState)get(roomObject,"match")).reset();set(roomObject,"battleStarted",false);
   System.out.println("PASS: ArenaBot Bone Break damage; 14 explicit illusion skills; five PC buffs on selected PC/self; companion attack/Patience/Joy; curse target; no unknown/missing/far/dead fallback; scopes released; no SQL writes");
  }
 }
}
