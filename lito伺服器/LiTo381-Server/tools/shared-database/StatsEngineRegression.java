package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.lang.reflect.*;

/** Real MySQL engine checks. Every write targets a connection-local TEMPORARY table, never a durable table. */
public class StatsEngineRegression {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class TemporaryDriver implements Driver {
  final Properties admin;String engine="InnoDB";boolean existing,race;int calls;long finalWins,finalVersion;long character;String name;
  TemporaryDriver(Properties admin){this.admin=admin;}
  public boolean acceptsURL(String u){return u.startsWith("jdbc:lito-temp:");}
  public Connection connect(String u,Properties ignored)throws SQLException {
   if(!acceptsURL(u))return null;
   Connection real=DriverManager.getConnection(u.replace("jdbc:lito-temp:","jdbc:mysql:"),admin.getProperty("Login_LOGIN",admin.getProperty("Login")),admin.getProperty("Password_LOGIN",admin.getProperty("Password","")));
   try(Statement s=real.createStatement()) {
    try(ResultSet r=s.executeQuery("SELECT objid,char_name FROM characters WHERE Banned=0 ORDER BY objid LIMIT 1")){check(r.next(),"fixture source character exists");character=r.getLong(1);name=r.getString(2);}
    s.executeUpdate("CREATE TEMPORARY TABLE character_lito (char_obj_id INT UNSIGNED PRIMARY KEY,char_name VARCHAR(45),wins BIGINT UNSIGNED,losses BIGINT UNSIGNED,version BIGINT UNSIGNED,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE="+engine);
   } catch(Throwable e){real.close();throw e;}
   if(existing)seed(real,0,0);
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(proxy,m,a)->{
    if(m.getName().equals("close")) {
     try(Statement s=real.createStatement();ResultSet r=s.executeQuery("SELECT wins,version FROM character_lito WHERE char_obj_id="+character)) {if(r.next()){finalWins=r.getLong(1);finalVersion=r.getLong(2);}}
    }
    Object result;try{result=m.invoke(real,a);}catch(InvocationTargetException e){throw e.getCause();}
    if(m.getName().equals("prepareStatement") && a[0] instanceof String sql && (sql.startsWith("INSERT")||sql.startsWith("UPDATE"))) {
     check(sql.startsWith("INSERT INTO character_lito(")||sql.startsWith("UPDATE character_lito SET "),"only temporary results may be written");
     PreparedStatement statement=(PreparedStatement)result;
     return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(p,method,values)->{
      if(method.getName().equals("executeUpdate")){calls++;if(race){if(existing)try(Statement q=real.createStatement()){q.executeUpdate("UPDATE character_lito SET wins=9,version=1");}else seed(real,9,1);real.commit();}}
      try{return method.invoke(statement,values);}catch(InvocationTargetException e){throw e.getCause();}
     });
    }
    return result;
   });
  }
  void seed(Connection c,long wins,long version)throws SQLException {
   try(PreparedStatement s=c.prepareStatement("INSERT INTO character_lito(char_obj_id,char_name,wins,losses,version) VALUES(?,?,?,0,?)")){s.setLong(1,character);s.setString(2,name);s.setLong(3,wins);s.setLong(4,version);s.executeUpdate();}
  }
  public DriverPropertyInfo[] getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}public java.util.logging.Logger getParentLogger(){return java.util.logging.Logger.getGlobal();}
 }
 static GameStateStore.TournamentProfileState win(GameStateStore.TournamentProfileState s)throws Exception {
  var parts=s.getClass().getRecordComponents();Object[] values=new Object[parts.length];Class<?>[] types=new Class[parts.length];
  for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=parts[i].getAccessor().invoke(s);if(parts[i].getName().equals("wins"))values[i]=s.wins()+1;}
  return (GameStateStore.TournamentProfileState)s.getClass().getDeclaredConstructor(types).newInstance(values);
 }
 public static void main(String[] args)throws Exception {
  ServerConfig config=ServerConfig.load(Path.of(args[0]));TemporaryDriver driver=new TemporaryDriver(SharedDatabase.properties(Path.of(args[1])));DriverManager.registerDriver(driver);
  try(AccountRepository all=AccountRepository.open(config)) {
   all.healthCheck();
   LegacyMySqlRepository repository=(LegacyMySqlRepository)all.repositoryForClassicSource(config.classicServerNames().get(0));
   var original=SharedDatabase.ROUTES.get(repository);Properties p=SharedDatabase.properties(config.legacySqlProperties());p.setProperty("URL1_LOGIN",p.getProperty("URL1_LOGIN").replace("jdbc:mysql:","jdbc:lito-temp:"));
   try {
    for(String engine:List.of("InnoDB","MyISAM"))for(boolean exists:List.of(false,true))for(boolean race:List.of(false,true)) {
     driver.engine=engine;driver.existing=exists;driver.race=race;driver.calls=0;driver.finalWins=-1;
     var route=new SharedDatabase.Route(original.name,original.database,original.controls,p);SharedDatabase.ROUTES.put(repository,route);
     var before=repository.tournamentProfiles().profiles().get(0);
     try { repository.persistTournamentProfile(before.characterObjectId(),before.characterName(),before,win(before.state())); throw new AssertionError("non-settlement write accepted"); }
     catch(IllegalStateException expected) { check(driver.calls==0,"write gate before JDBC"); }
     try { SharedDatabase.commitResult(repository,before.characterObjectId(),before.characterName(),before,win(win(before.state()))); throw new AssertionError("multi-result write accepted"); }
     catch(SQLException expected) { check(driver.calls==0,"invalid delta rejected before write"); }
     var result=SharedDatabase.commitResult(repository,before.characterObjectId(),before.characterName(),before,win(before.state()));
     try { repository.persistTournamentProfile(before.characterObjectId(),before.characterName(),before,win(before.state())); throw new AssertionError("settlement capability leaked"); }
     catch(IllegalStateException expected) {}
     check(result.status()==(race?RepositoryTournamentMutationStatus.CONFLICT:RepositoryTournamentMutationStatus.SUCCESS),"CAS result "+engine);
     check(driver.calls==1,"exactly one application write");
     check(driver.finalWins==(race?9:1)&&driver.finalVersion==1,"temporary row preserved, race never overwritten");
     System.out.println("PASS "+engine+" "+(exists?"UPDATE":"INSERT")+" concurrent-race="+race+"; temporary-table-only");
    }
   }finally{SharedDatabase.ROUTES.put(repository,original);}
  }finally{DriverManager.deregisterDriver(driver);}
 }
}
