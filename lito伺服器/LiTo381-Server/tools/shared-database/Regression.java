package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.sql.*;import java.lang.reflect.*;
/** Live reads and rollback-only result writes. Never commits a test result. */
public class Regression {
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 static GameStateStore.TournamentProfileState win(GameStateStore.TournamentProfileState before)throws Exception{
  RecordComponent[] parts=before.getClass().getRecordComponents();Object[] values=new Object[parts.length];Class<?>[] types=new Class<?>[parts.length];
  for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=parts[i].getAccessor().invoke(before);if(parts[i].getName().equals("wins"))values[i]=before.wins()+1;}
  return (GameStateStore.TournamentProfileState)before.getClass().getDeclaredConstructor(types).newInstance(values);
 }
 static final class RollbackDriver implements Driver {
  final List<Connection> opened=new ArrayList<>();
  boolean seedExistingZeroVersion;
  public boolean acceptsURL(String u){return u.startsWith("jdbc:lito-rollback:");}
  public Connection connect(String u,Properties p)throws SQLException{
   if(!acceptsURL(u))return null;
   Connection real=DriverManager.getConnection(u.replace("jdbc:lito-rollback:","jdbc:mysql:"),p);opened.add(real);
   try(Statement s=real.createStatement();ResultSet r=s.executeQuery("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_lito'")){
    if(!r.next()||!"InnoDB".equalsIgnoreCase(r.getString(1))){real.close();throw new SQLException("Rollback-only test refuses nontransactional tables; use StatsEngineRegression temporary tables.");}
   }
   if(seedExistingZeroVersion){
    real.setAutoCommit(false);
    try(Statement s=real.createStatement()){
     s.executeUpdate("INSERT INTO character_lito(char_obj_id,char_name,wins,losses,version) SELECT c.objid,c.char_name,0,0,0 FROM characters c LEFT JOIN character_lito t ON t.char_obj_id=c.objid WHERE c.Banned=0 AND t.char_obj_id IS NULL ORDER BY c.objid LIMIT 1");
    }
   }
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(proxy,m,args)->{
    if(m.getName().equals("commit"))return null;
    if(m.getName().equals("close")){if(!real.getAutoCommit())real.rollback();real.close();return null;}
    try{return m.invoke(real,args);}catch(InvocationTargetException e){throw e.getCause();}
   });
  }
  public DriverPropertyInfo[] getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}
  public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}
  public java.util.logging.Logger getParentLogger(){return java.util.logging.Logger.getGlobal();}
 }
 public static void main(String[] args)throws Exception{
  ServerConfig config=ServerConfig.load(Path.of(args[0]));
  require(config.classicServerIds().equals(List.of(1,2,3)),"three routes");
  require(!config.statePersistenceEnabled()&&config.databaseReadOnly(),"immutable source mode");
  Properties p=SharedDatabase.properties(config.legacySqlProperties());RollbackDriver driver=new RollbackDriver();DriverManager.registerDriver(driver);
  try(AccountRepository all=AccountRepository.open(config)){
   all.healthCheck();all.migrateLitoExtensions();
   for(String name:config.classicServerNames()){
    LegacyMySqlRepository repository=(LegacyMySqlRepository)all.repositoryForClassicSource(name);
    SharedDatabase.Route original=SharedDatabase.ROUTES.get(repository);
    var roster=repository.tournamentProfiles();require(roster.authoritative()&&!roster.profiles().isEmpty(),"SQL result roster");
    require(repository.tournamentShopOffers().size()==16,"shop route");
    var battle=repository.battleConfiguration();require(battle.rules().size()==40&&battle.playerSpawns().size()==380,"control route");
    long id=roster.profiles().get(0).characterObjectId();
    repository.sourceInventory(id);repository.learnedSkills(id);repository.pets(id);
    require(repository.createAccount("lito_readonly_probe_unused","not-a-real-password")==AccountMutationResult.READ_ONLY,"account create blocked");
    try(Connection c=original.connection();Statement s=c.createStatement()){
     for(String table:List.of("characters","character_items","accounts")){
      try{s.executeUpdate("DELETE FROM `"+table+"` WHERE 1=0");throw new AssertionError("Source DELETE privilege exists");}
      catch(SQLException denied){require(denied.getErrorCode()==1142,"expected privilege denial");}
     }
     try{s.executeUpdate("UPDATE characters SET OnlineStatus=OnlineStatus WHERE 1=0");throw new AssertionError("Source UPDATE privilege exists");}
     catch(SQLException denied){require(denied.getErrorCode()==1142,"source UPDATE denied");}
    }
    Properties rollback=new Properties();rollback.putAll(p);rollback.setProperty("URL1_LOGIN",p.getProperty("URL1_LOGIN").replace("jdbc:mysql:","jdbc:lito-rollback:"));
    SharedDatabase.Route test=new SharedDatabase.Route(name,original.database,original.controls,rollback);
    SharedDatabase.ROUTES.put(repository,test);
    try{
     RepositoryTournamentProfile before=repository.tournamentProfiles().profiles().get(0);
     var after=win(before.state());
     var saved=SharedDatabase.commitResult(repository,before.characterObjectId(),before.characterName(),before,after);
     require(saved.status()==RepositoryTournamentMutationStatus.SUCCESS,"result insert/update path");
     require(saved.profile().snapshotVersion()==before.snapshotVersion()+1,"result version increment");
     var wrong=new RepositoryTournamentProfile(before.characterObjectId(),before.characterName(),before.snapshotVersion()+12,before.state());
     require(SharedDatabase.commitResult(repository,before.characterObjectId(),before.characterName(),wrong,after).status()==RepositoryTournamentMutationStatus.CONFLICT,"stale result rejected");
     var unchanged=repository.tournamentProfiles().profiles().get(0);
     require(unchanged.state().wins()==before.state().wins()&&unchanged.snapshotVersion()==before.snapshotVersion(),"rollback left no test result");
     driver.seedExistingZeroVersion=true;
     try{
      test.runtime.clear();
      var existing=repository.tournamentProfiles().profiles().get(0);
      var updated=SharedDatabase.commitResult(repository,existing.characterObjectId(),existing.characterName(),existing,win(existing.state()));
      require(updated.status()==RepositoryTournamentMutationStatus.SUCCESS,"existing zero-version row UPDATE");
     }finally{driver.seedExistingZeroVersion=false;}
    }finally{SharedDatabase.ROUTES.put(repository,original);}
    System.out.println("PASS "+original.database+": reads, SQL write restrictions, result write/rollback and CAS conflict");
   }
  }
  require(driver.opened.stream().allMatch(c->{try{return c.isClosed();}catch(SQLException e){return false;}}),"all rollback connections closed");
  // Verify a disabled middle slot keeps stable IDs and does not open its database.
  Path dir=Files.createTempDirectory("lito-source-slots-");
  try{
   Path file=dir.resolve("sources.properties");
   Files.writeString(file,"source.slots=8\nsource.1.enabled=true\nsource.1.name=A\nsource.1.id=1\nsource.1.database=380_ac\nsource.3.enabled=true\nsource.3.name=C\nsource.3.id=3\nsource.3.database=380_ac_2\n");
   Properties t=new Properties();t.setProperty("source.servers.file",file.toString());SharedDatabase.applySources(t,dir);
   require(t.getProperty("login.classic.server.ids").equals("1,3"),"disabled slot preserves IDs");
   require(t.getProperty("database.dynamic.databases").equals("380_ac,380_ac_2"),"disabled databases excluded");
  }finally{Files.deleteIfExists(dir.resolve("sources.properties"));Files.delete(dir);}
  System.out.println("PASS eight-slot enable switches; no source rows or battle results committed by tests.");
 }
}
