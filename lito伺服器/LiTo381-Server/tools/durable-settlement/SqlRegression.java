package local.lito381.server;
import java.sql.*;import java.util.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.logging.Logger;
public class SqlRegression {
 static volatile boolean unavailable;static final AtomicBoolean failAfterCounter=new AtomicBoolean(),failAck=new AtomicBoolean();
 static Object invoke(Object o,Method m,Object[] a)throws Throwable{try{return m.invoke(o,a);}catch(InvocationTargetException e){throw e.getCause();}}
 static class FaultDriver implements Driver {
  public boolean acceptsURL(String u){return u.startsWith("jdbc:lito-fault:");}
  public Connection connect(String u,Properties p)throws SQLException{if(!acceptsURL(u))return null;if(unavailable)throw new SQLException("injected database outage");Connection real=DriverManager.getConnection("jdbc:"+u.substring("jdbc:lito-fault:".length()),p);
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(proxy,m,a)->{
    Object r=invoke(real,m,a);if(m.getName().equals("prepareStatement")&&r instanceof PreparedStatement ps){String sql=(String)a[0];return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(pp,mm,aa)->{
     if(mm.getName().equals("executeUpdate")&&sql.contains("SET applied=1")&&failAck.compareAndSet(true,false))throw new SQLException("injected receipt acknowledgement failure");
     Object v=invoke(ps,mm,aa);if(mm.getName().equals("executeUpdate")&&((sql.startsWith("UPDATE character_lito")||sql.startsWith("INSERT INTO character_lito")))&&failAfterCounter.compareAndSet(true,false))throw new SQLException("injected lost counter-write response");return v;
    });}return r;
   });
  }
  public DriverPropertyInfo[] getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}public Logger getParentLogger(){return Logger.getGlobal();}
 }
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static SettlementJournal.Entry entry(String battle,String db,String controls,String name,boolean won){return new SettlementJournal.Entry(SettlementJournal.Entry.key(battle,db,42),battle,db,db,controls,42,name,won,!won);}
 static long[] counts(Connection c,String db)throws Exception{try(var s=c.createStatement();var r=s.executeQuery("SELECT wins,losses,version FROM `"+db+"`.character_lito WHERE char_obj_id=42")){return r.next()?new long[]{r.getLong(1),r.getLong(2),r.getLong(3)}:new long[]{0,0,0};}}
 static void expected(Connection c,String db,long w,long l)throws Exception{check(Arrays.equals(counts(c,db),new long[]{w,l,w+l}),"unexpected counters "+db+" "+Arrays.toString(counts(c,db)));}
 public static void main(String[] args)throws Exception{
  var config=UnifiedConfiguration.read(Path.of(args[0]));if(args.length>2){Properties admin=new Properties();try(var in=Files.newBufferedReader(Path.of(args[2]))){admin.load(in);}config.setProperty("database.user",admin.getProperty("Login_LOGIN"));config.setProperty("database.password",admin.getProperty("Password_LOGIN",""));}Path root=Path.of(args[1]);Files.createDirectories(root);String prefix="lito_settle_test_"+Long.toUnsignedString(System.nanoTime(),36);String control=prefix+"_c",a=prefix+"_a",b=prefix+"_b";List<String> dbs=List.of(control,a,b);List<LegacyMySqlRepository> repos=new ArrayList<>();Path credentials=root.resolve("credentials.tmp");
  DriverManager.registerDriver(new FaultDriver());
  try(Connection admin=SettlementSchema.connect(config,config.getProperty("database.lito.database"))){
   try{
    for(String db:dbs)try(var s=admin.createStatement()){s.executeUpdate("CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4");}
    for(String db:List.of(a,b))try(var s=admin.createStatement()){s.executeUpdate("CREATE TABLE `"+db+"`.characters(objid BIGINT PRIMARY KEY,char_name VARCHAR(100) NOT NULL) ENGINE=MyISAM");s.executeUpdate("INSERT INTO `"+db+"`.characters VALUES(42,'SameName')");s.executeUpdate("CREATE TABLE `"+db+"`.character_lito(char_obj_id BIGINT PRIMARY KEY,char_name VARCHAR(100),wins INT UNSIGNED NOT NULL,losses INT UNSIGNED NOT NULL,version INT UNSIGNED NOT NULL) ENGINE=MyISAM");}
    SettlementSchema.apply(admin,control,List.of(a,b));
    Properties p=new Properties();p.setProperty("URL1_LOGIN",config.getProperty("database.jdbc.base.url"));p.setProperty("URL3_LOGIN",config.getProperty("database.jdbc.options",""));p.setProperty("Login_LOGIN",config.getProperty("database.user"));p.setProperty("Password_LOGIN",config.getProperty("database.password"));p.setProperty("Login",p.getProperty("Login_LOGIN"));p.setProperty("Password",p.getProperty("Password_LOGIN"));try(var out=Files.newBufferedWriter(credentials)){p.store(out,"temporary fixture credentials");}
    Properties faulty=new Properties();faulty.putAll(p);faulty.setProperty("URL1_LOGIN",p.getProperty("URL1_LOGIN").replace("jdbc:","jdbc:lito-fault:"));
    for(String db:List.of(a,b)){var repo=new LegacyMySqlRepository(credentials,db,db,db,100,true,"legacy_mysql");repos.add(repo);SharedDatabase.ROUTES.put(repo,new SharedDatabase.Route(db,db,control,faulty));}
    var first=entry(UUID.randomUUID().toString(),a,control,"SameName",true);var second=entry(first.battle(),b,control,"SameName",false);
    try(var journal=new SettlementJournal(root.resolve("journal"))){journal.save(List.of(first,second));unavailable=true;try{journal.replay(SettlementSql::apply);throw new AssertionError("outage ignored");}catch(SQLException expected){}finally{unavailable=false;}check(journal.pending()==1,"outage lost pending results");}
    // Restart/reopen the journal; the write succeeds but its acknowledgement is lost.
    try(var journal=new SettlementJournal(root.resolve("journal"))){failAfterCounter.set(true);try{journal.replay(SettlementSql::apply);throw new AssertionError("lost reply ignored");}catch(SQLException expected){}check(journal.pending()==1,"unknown outcome lost pending result");journal.replay(SettlementSql::apply);check(journal.pending()==0,"replay did not finish");}
    expected(admin,a,1,0);expected(admin,b,0,1);SettlementSql.apply(List.of(first,second));expected(admin,a,1,0);expected(admin,b,0,1);
    var third=entry(UUID.randomUUID().toString(),a,control,"SameName",false);failAck.set(true);try{SettlementSql.apply(List.of(third));throw new AssertionError("ack failure ignored");}catch(SQLException expected){}SettlementSql.drainAll();expected(admin,a,1,1);
    var pool=Executors.newFixedThreadPool(4);try{List<Future<?>> jobs=new ArrayList<>();for(int i=0;i<8;i++){var event=entry(UUID.randomUUID().toString(),a,control,"SameName",true);jobs.add(pool.submit(()->{try{SettlementSql.apply(List.of(event));SettlementSql.apply(List.of(event));}catch(Exception e){throw new RuntimeException(e);}}));}for(var job:jobs)job.get(30,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
    expected(admin,a,9,1);SettlementSql.apply(List.of(first));expected(admin,a,9,1);
    var conflicting=new SettlementJournal.Entry(first.id(),first.battle(),a,a,control,42,"SameName",false,true);try{SettlementSql.apply(List.of(conflicting));throw new AssertionError("changed verdict accepted");}catch(SQLException expected){}expected(admin,a,9,1);
    try(var s=admin.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM `"+control+"`.lito_battle_settlement WHERE applied=0")){r.next();check(r.getInt(1)==0,"pending receipts left");}
    // Exercise the actual ClientSession -> DisconnectedBattleResult -> durable SQL path.
    var serverConfig=ServerConfig.load(Path.of(args[0]),"standard");var live=new GameStateStore(root.resolve("memory-only"),false,true);var memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
    try(var capture=new CaptureLog(root.resolve("session-capture"));var hub=new GameHub(serverConfig,live,memory);var fixture=new SourceLoginRegression.Fixture(serverConfig,memory,live,capture,hub,0,ClientSession.Endpoint.GAME);var sessionJournal=new SettlementJournal(root.resolve("session-journal"))){
     var jf=DurableBattleSettlement.class.getDeclaredField("journal");jf.setAccessible(true);jf.set(null,sessionJournal);
     NativeDirectoryRegression.init(fixture,"SameName",0,serverConfig.classicServerNames().get(0));
     var create=Regression.class.getDeclaredMethod("character",String.class,int.class,long.class);create.setAccessible(true);var character=(CharacterSummary)create.invoke(null,"SameName",0,42L);
     NativeDirectoryRegression.set(fixture.session,"characters",List.of(character));NativeDirectoryRegression.set(fixture.session,"accounts",repos.get(0));NativeDirectoryRegression.set(fixture.session,"sqlTournamentAuthoritative",true);
     SharedDatabase.ROUTES.put(repos.get(0),new SharedDatabase.Route(serverConfig.classicServerNames().get(0),a,control,faulty));
     long rid=FlowRegression.setup(hub,fixture.session);String identity=FlowRegression.key(fixture.session);
     var win=new BattleRules.Resolution(true,false,Set.of(fixture.session.playerObjectId()),Set.of(),"sql-session-win");
     DurableBattleSettlement.prepare(fixture.session,win);check((boolean)NativeDirectoryRegression.get(fixture.session,"resultRecorded"),"actual SQL record hook failed");expected(admin,a,10,1);check(live.tournamentProfileState(identity).wins()==10,"live profile not synchronized");
     fixture.session.serverRecordBattleResult(true);expected(admin,a,10,1);
     Object rawRoom=FlowRegression.room(hub,rid);NativeDirectoryRegression.set(rawRoom,"battleGeneration",2L);NativeDirectoryRegression.set(fixture.session,"resultRecorded",false);DurableBattleSettlement.begin(fixture.session);
     live.reconcileTournamentProfileStates(Map.of(identity,DurableBattleSettlement.counters(live.tournamentProfileState(identity),9,1,serverConfig.classicServerNames().get(0))));
     DurableBattleSettlement.reconcileRecovered(List.of(first));check(live.tournamentProfileState(identity).wins()==10,"recovered earlier battle did not refresh new battle session");check(!(boolean)NativeDirectoryRegression.get(fixture.session,"resultRecorded"),"older receipt marked new battle complete");
     unavailable=true;var loss=new BattleRules.Resolution(true,false,Set.of(),Set.of(fixture.session.playerObjectId()),"sql-session-loss");DurableBattleSettlement.prepare(fixture.session,loss);check(!(boolean)NativeDirectoryRegression.get(fixture.session,"resultRecorded"),"failed SQL marked complete");check(sessionJournal.pending()>0,"pending SQL result not journaled");
     unavailable=false;var other=(CharacterSummary)create.invoke(null,"OtherCharacter",0,43L);NativeDirectoryRegression.set(fixture.session,"characters",List.of(other));NativeDirectoryRegression.set(fixture.session,"selectedCharacter","OtherCharacter");fixture.session.serverRecordBattleResult(true);check(!(boolean)NativeDirectoryRegression.get(fixture.session,"resultRecorded"),"old outcome completed new character");expected(admin,a,10,1);
     NativeDirectoryRegression.set(fixture.session,"characters",List.of(character));NativeDirectoryRegression.set(fixture.session,"selectedCharacter","SameName");
     fixture.server.close();live.releaseSourceCharacter(identity);unavailable=false;sessionJournal.replay(SettlementSql::apply);fixture.session.serverRecordBattleResult(true);expected(admin,a,10,2);
     check((boolean)NativeDirectoryRegression.get(fixture.session,"resultRecorded"),"detached session retry failed");check(!DurableBattleSettlement.hasProfile(live,identity),"retry recreated released player state");
     NativeDirectoryRegression.set(rawRoom,"battleGeneration",3L);NativeDirectoryRegression.set(fixture.session,"resultRecorded",false);DurableBattleSettlement.begin(fixture.session);
     var closedJournal=new SettlementJournal(root.resolve("closed-journal"));closedJournal.close();jf.set(null,closedJournal);
     try{DurableBattleSettlement.prepare(fixture.session,win);throw new AssertionError("failed journal write ignored");}catch(java.io.IOException expected){}
     check(!DurableBattleSettlement.cleanupAllowed(hub,rid),"room cleanup permitted before durable save");
     try{hub.endBattle(rid);throw new AssertionError("battle cleared after disk failure");}catch(IllegalStateException expected){}
     ((Set<?>)NativeDirectoryRegression.get(rawRoom,"members")).clear();NativeDirectoryRegression.call(hub,"resetOrRemove",new Class[]{rawRoom.getClass()},rawRoom);check(FlowRegression.room(hub,rid)!=null,"empty room removed before durable save");expected(admin,a,10,2);
     jf.set(null,sessionJournal);DurableBattleSettlement.prepare(fixture.session,win);check(DurableBattleSettlement.cleanupAllowed(hub,rid),"recovered journal still blocks cleanup");expected(admin,a,11,2);hub.endBattle(rid);NativeDirectoryRegression.call(hub,"resetOrRemove",new Class[]{rawRoom.getClass()},rawRoom);check(FlowRegression.room(hub,rid)==null,"recovered empty room retained");jf.set(null,null);
    }
    System.out.println("PASS: actual ClientSession SQL hook, live profile counters, outage queues before notification, detached-session replay preserves frozen loss and does not recreate released profile; journal failure blocks battle and empty-room cleanup, recovery saves once then permits cleanup");
    System.out.println("PASS: real MyISAM source tables; independent same-ID sources; SQL outage WAL retained/restart replay; counter-write response loss; receipt-ack failure; concurrent duplicate replay; immutable outcome; exact counters; no real player data changed");
   }finally{
    SharedDatabase.ROUTES.clear();for(var repo:repos)repo.close();Files.deleteIfExists(credentials);
    for(String db:dbs){if(!db.startsWith(prefix+"_")||!db.matches("lito_settle_test_[a-z0-9]+_[cab]"))throw new IllegalStateException("unsafe fixture database cleanup");try(var s=admin.createStatement()){s.executeUpdate("DROP DATABASE IF EXISTS `"+db+"`");}}
   }
  }
 }
}
