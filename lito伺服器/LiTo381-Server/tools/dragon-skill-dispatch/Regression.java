package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.lang.reflect.*;import java.util.*;
public class Regression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object call(Object s,String n,Class<?>[] types,Object...args)throws Exception{var m=s.getClass().getDeclaredMethod(n,types);m.setAccessible(true);try{return m.invoke(s,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}
 static void cast(ClientSession s,int id,long target)throws Exception {
  ((Map<?,?>)field(s,"spellReadyNanos")).clear();
  var w=new Protocol.Writer().c(0x14).c((id-1)>>3).c((id-1)&7);
  if(target!=0)w.d(target).h(32741).h(32798);
  call(s,"useSpell",new Class[]{byte[].class},w.bytes());
  check(DragonSkillRequests.current()==null,"request scope leaked");
 }
 public static void main(String[]args)throws Exception {
  var config=ServerConfig.load(Path.of(args[0]),"standard");Path tmp=Path.of(args[1]);Files.createDirectories(tmp);
  int[] commits={0};
  AccountRepository base=new MemoryAccountRepository("qatest","testpass","",0,1,40,52);
  AccountRepository accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
   if(m.getName().equals("repositoryForClassicSource"))return p;
   if(m.getName().equals("persistTournamentProfile")) {
    var expected=(RepositoryTournamentProfile)a[2];var after=(GameStateStore.TournamentProfileState)a[3];
    check(expected.state().wins()==3 && expected.state().losses()==2,"wrong disconnect baseline");
    check(after.wins()==3 && after.losses()==3 && after.classicServerName().equals(config.classicServerNames().get(0)),"lost source/results after cleanup");
    commits[0]++;return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.SUCCESS,new RepositoryTournamentProfile((long)a[0],(String)a[1],2,after));
   }
   try{return m.invoke(base,a);}catch(InvocationTargetException e){throw e.getCause();}
  });
  var state=new GameStateStore(tmp.resolve("unused.state"),false,true);
  try(var capture=new CaptureLog(tmp.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var fixture=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
   var s=fixture.session;initialize(s,character("DragonFixture",5,0x38101910L));
   setField(s,"learnedSkills",Set.of(181,182,183,184,185,186,187,188,189,190,191,192,193,194,195));
   setField(s,"selectedClassicServerName",config.classicServerNames().get(0));
   hub.enterChannel(0,s);long room=hub.createRoom(0,new GameHub.RoomSettings("Dragon", "", 2,1,0,0,0,0));
   hub.joinRoom(room,s);prepareBattle(s,room,32740,32798);setField(s,"playerHp",1000);setField(s,"playerMaxHp",1000);
   cast(s,181,0);
   var skin=(BattleDragonSkinStatus)field(s,"dragonSkinStatus");check(skin.snapshot(System.nanoTime()).active(),"skin did not activate");
   int physical=(int)call(s,"combatDamageAfterCooking",new Class[]{int.class,String.class,boolean.class,int.class,boolean.class,boolean.class},50,"fixture",true,1,false,true);
   int magic=(int)call(s,"combatDamageAfterCooking",new Class[]{int.class,String.class,boolean.class,int.class,boolean.class,boolean.class},50,"fixture",false,0,false,false);
   check(physical==40 && magic==50,"skin actual damage path");
   cast(s,182,0);var slash=(BattleBurningSlashStatus)field(s,"burningSlashStatus");
   check(slash.snapshot(System.nanoTime()).active(),"slash did not activate");
   check((boolean)call(s,"consumeBurningSlash",new Class[]{boolean.class,long.class,long.class,int.class,int.class,List.class,String.class},true,System.nanoTime(),2L,32741,32798,List.of(s),"fixture"),"slash not consumed");
   check(!slash.snapshot(System.nanoTime()).active(),"slash remained active after hit");
   var companion=(BattleCompanion)field(s,"battleCompanion");companion.resetForBattle(32741,32798);
   cast(s,188,companion.snapshot().objectId());
   check(((int)field(s,"playerHp"))==945,"fear companion cost / no exception");
   int before=(int)field(s,"playerHp");cast(s,188,9999999L);check((int)field(s,"playerHp")==before,"invalid explicit target consumed HP");
   cast(s,188,0);check((int)field(s,"playerHp")==before,"selector-only request guessed target");
   cast(s,192,companion.snapshot().objectId());
   check(companion.snapshot().hp()<BattleCompanion.MAX_HP,"Thunder Grab did not damage selected companion");
   var row=new SharedDatabase.Route(config.classicServerNames().get(0),"fixture","fixture",new Properties()).profile(0x38101910L,"DragonFixture",3,2,1);
   ((Map<Long,RepositoryTournamentProfile>)field(s,"sqlTournamentProfiles")).put(row.characterObjectId(),row);
   setField(s,"sqlTournamentAuthoritative",true);
   String identity=s.sourceCharacterIdentity();state.reconcileTournamentProfileStates(Map.of(identity,row.state()));
   check(state.releaseSourceCharacter(identity).released(),"fixture profile not released");
   var remaining=Set.copyOf(((Map<?,?>)field(state,"profiles")).keySet());
   call(s,"serverRecordBattleDisconnect",new Class[]{});
   check(commits[0]==1 && (boolean)field(s,"resultRecorded"),"disconnect result failed after working-set release");
   call(s,"serverRecordBattleDisconnect",new Class[]{});check(commits[0]==1,"duplicate disconnect result");
   check(((Map<?,?>)field(state,"profiles")).keySet().equals(remaining),"disconnect recreated live working set");
   System.out.println("PASS: delayed disconnect result after memory release; exact one loss; idempotency; no live-state recreation");
   System.out.println("PASS: real spell dispatch; physical reduction only; slash consumed once; fear companion no crash; invalid/missing targets rejected; Thunder Grab companion damage; request scope cleared; no SQL writes");
  }
 }
    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static void initialize(
            ClientSession session, CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static void prepareBattle(
            ClientSession session, long roomId, int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
