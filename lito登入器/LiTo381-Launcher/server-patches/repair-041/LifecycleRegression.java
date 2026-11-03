package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.util.*;
public class LifecycleRegression {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void set(Object s,String n,Object v){RuntimeLifecycle.set(s,n,v);}
 static Object get(Object s,String n){return LiveBattleStats.get(s,n);}
 public static void main(String[] args)throws Exception {
  var config=ServerConfig.load(Path.of(args[0]));Path root=Path.of(args[1]);Files.createDirectories(root);
  check(config.databaseReadOnly()&&!config.statePersistenceEnabled()&&config.ephemeralSourceProfiles(),"memory-only config");
  var state=new GameStateStore(root.resolve("must-not-exist.state"),false,true);
  try(var accounts=new MemoryAccountRepository("qatest","testpass","",0,1.0,40,52);var hub=new GameHub(config);var listen=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var peer=new Socket(InetAddress.getLoopbackAddress(),listen.getLocalPort());var socket=listen.accept();var capture=new CaptureLog(root.resolve("capture"))) {
   var s=new ClientSession(socket,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);
   String source=config.classicServerNames().get(0), identity=GameStateStore.sourceCharacter(source,"LifecycleTester");
   set(s,"selectedClassicServerName",source);set(s,"selectedCharacter","LifecycleTester");set(s,"account","qatest");
   set(s,"nativeTournamentEconomyInitialized",true);LiveBattleStats.enable(s);
   for(int i=0;i<100;i++) {
    var inv=new RuntimeInventory();check(inv.add(InventoryCatalog.require(40743),100),"purchase stack");set(s,"inventory",inv);
    for(String name:List.of("spellReadyNanos","itemReadyNanos","storySkillEffectDeadlines"))((Map)get(s,name)).put(i,Long.MAX_VALUE);
    ((Collection)get(s,"tamedCompanions")).add(get(s,"battleCompanion"));set(s,"lastAttacker",s);set(s,"lastCombatTargetObjectId",5L);
    state.recordResult(identity,true,250);
    LiveBattleStats.call(s,"resetNativeTournamentEconomy",new Class[]{String.class},"battle-result");
    for(String name:List.of("spellReadyNanos","itemReadyNanos","storySkillEffectDeadlines"))check(((Map)get(s,name)).isEmpty(),"cleared "+name);
    check(((Collection)get(s,"tamedCompanions")).isEmpty()&&get(s,"lastAttacker")==null,"actors and session links released");
    check(((RuntimeInventory)get(s,"inventory")).size()==0&&(long)get(s,"shopPoints")==0,"result clears shop items and coins");
    check(state.tournamentStatistics(identity).wins()==i+1,"cleanup retains result");
    LiveBattleStats.call(s,"resetNativeTournamentEconomy",new Class[]{String.class},"room-exit");
    check((long)get(s,"shopPoints")==config.nativeTournamentAllowance(),"next lobby fixed allowance");
   }
   set(s,"resultRecorded",false);s.serverRecordBattleResult(true);s.serverRecordBattleResult(true);
   check(state.tournamentStatistics(identity).wins()==101,"duplicate settlement callback awarded once");
   set(s,"nativeTournamentEconomyInitialized",false);((Map)get(s,"spellReadyNanos")).put(1,1L);
   LiveBattleStats.call(s,"resetNativeTournamentEconomy",new Class[]{String.class},"disconnect");
   check(((Map)get(s,"spellReadyNanos")).isEmpty(),"uninitialized economy still cleans runtime");
   state.releaseSourceCharacter(identity);check(!((Map)get(state,"profiles")).containsKey(identity.toLowerCase(Locale.ROOT)),"logout releases working profile");
   check(!Files.exists(root.resolve("must-not-exist.state")),"no sidecar player writes");
  }
  // Real production roster load; SELECT only. Per-session CAS retains only owned characters.
  try(var accounts=AccountRepository.open(config);var hub=new GameHub(config);var listen=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var peer=new Socket(InetAddress.getLoopbackAddress(),listen.getLocalPort());var socket=listen.accept();var capture=new CaptureLog(root.resolve("roster-capture"))) {
   var s=new ClientSession(socket,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);String source=config.classicServerNames().get(0);set(s,"selectedClassicServerName",source);
   var repo=(LegacyMySqlRepository)accounts.repositoryForClassicSource(source);var rows=repo.tournamentProfiles().profiles();check(!rows.isEmpty(),"source roster present");
   check(SharedDatabase.ROUTES.get(repo).runtime.isEmpty(),"read does not retain full roster cache");
   SharedDatabase.ROUTES.get(repo).runtime.put(Long.MAX_VALUE,rows.get(0));repo.tournamentProfiles();
   check(SharedDatabase.ROUTES.get(repo).runtime.isEmpty(),"removed source identities pruned");
   var own=rows.get(0);set(s,"characters",List.of(new CharacterSummary(own.characterName(),"",1,0,52,500,100,10,13,10,10,9,8,11,8,11,own.characterObjectId(),0,0,0,0,0,0,0)));
   LiveBattleStats.call(s,"loadSqlTournamentProfiles",new Class[0]);
   check(((Map)get(s,"sqlTournamentProfiles")).keySet().equals(Set.of(own.characterObjectId())),"actual roster hook retains only account characters");
   check(((Map)get(state,"profiles")).size()>=rows.size(),"shared leaderboard roster preserved");
  }
  System.out.println("PASS 100 match/shop reset cycles; cooldowns/companions/session links released; results retained; logout cleanup; account-only CAS cache; read-only roster; no state file");
 }
}
