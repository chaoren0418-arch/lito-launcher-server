package local.lito381.server;
import java.sql.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import com.zaxxer.hikari.HikariDataSource;
/** Shared database is SELECT-only; fault views never change its contents. */
public final class NpcPhysicalSourceLoadProbe {
 static int checks,cases,lookups;
 static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Object field(Object o,Class<?> c,String name)throws Exception{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static Object invoke(Object o,Method m,Object[]a)throws Throwable{try{return m.invoke(o,a);}catch(InvocationTargetException e){throw e.getCause();}}
 static Connection view(Connection real,int mode){return(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
  if(m.getName().equals("prepareStatement")){
   need(((String)a[0]).startsWith("SELECT "),"SELECT only");if(mode==1)throw new SQLException("fixture query failure");
   var st=(PreparedStatement)invoke(real,m,a);
   return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(sp,sm,sa)->{
    if(sm.getName().equals("executeQuery")){
     var rs=(ResultSet)invoke(st,sm,sa);int[]seen={0};
     return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),new Class[]{ResultSet.class},(rp,rm,ra)->{
      if(rm.getName().equals("next")){if(mode==2)return false;if(mode==4&&seen[0]++==1)throw new SQLException("fixture partial result failure");}
      if(rm.getName().equals("getInt")&&"mp".equals(ra[0])&&mode==3)return -1;
      if(rm.getName().equals("getInt")&&"npcid".equals(ra[0])&&mode==5)return 1;
      return invoke(rs,rm,ra);
     });
    }return invoke(st,sm,sa);
   });
  }return invoke(real,m,a);
 });}
 static void rejects(String table,int id)throws Exception{boolean rejected=false;try{NativeNpcPhysicalSource.lookup(table,id);}catch(SQLException expected){rejected=true;}need(rejected,"missing/invalid/source mismatch rejected");}
 static ServerConfig mismatchedSource(ServerConfig original)throws Exception{
  var parts=ServerConfig.class.getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];
  for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=parts[i].getName().equals("legacySourceResourceDatabases")?List.of("fixture_wrong_static"):parts[i].getAccessor().invoke(original);}
  var ctor=ServerConfig.class.getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(values);
 }
 public static void main(String[]args)throws Exception{
  Path jar=Path.of(args[2]).toRealPath();for(var c:List.of(CombatDatabase.class,LegacyMySqlRepository.class,NativeNpcPhysicalSource.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate classes");
  need(NativeNpcPhysicalSource.lookup("unused.npc",1)==null,"legacy fallback before startup");
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(cfg);
  var original=(NativeNpcPhysicalSource.Snapshot)field(null,NativeNpcPhysicalSource.class,"current");need(original!=null&&!original.rows().isEmpty(),"startup snapshot populated");ResourceDatabase.load(cfg);need(field(null,NativeNpcPhysicalSource.class,"current")==original,"repeat startup stable");
  var p=UnifiedConfiguration.read(cfg.source());String db=SharedDatabase.identifier(p.getProperty("database.static.database"));need(original.table().equals(db+".npc"),"qualified static database");
  for(String source:cfg.legacySourceResourceDatabases())need(source.equals(db),"enabled resource database matches shared snapshot");
  for(int mode=0;mode<6;mode++){
   NativeNpcPhysicalSource.Snapshot prepared=null;boolean failed=false;
   try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);try{prepared=NativeNpcPhysicalSource.read(view(c,mode));}catch(SQLException e){failed=true;}finally{c.rollback();}}
   need(failed==(mode==1||mode==4||mode==5),"expected fault mode "+mode);need(field(null,NativeNpcPhysicalSource.class,"current")==original,"no partial publication");
   if(!failed){try{NativeNpcPhysicalSource.publish(prepared);if(mode==0)need(prepared.equals(original),"all SQL fields stable");if(mode==2){need(prepared.rows().isEmpty(),"empty table view");rejects(prepared.table(),original.rows().keySet().iterator().next());}if(mode==3){need(prepared.rows().isEmpty()&&!prepared.invalid().isEmpty(),"invalid rows retained as rejected IDs");rejects(prepared.table(),prepared.invalid().iterator().next());}}finally{NativeNpcPhysicalSource.publish(original);}}cases++;
  }
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){c.setReadOnly(true);c.setAutoCommit(false);try{
   var observed=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,m,a)->{if(m.getName().equals("prepareStatement"))throw new AssertionError("source mismatch must fail before SELECT");return invoke(c,m,a);});
   boolean rejected=false;try{NativeNpcPhysicalSource.read(observed,mismatchedSource(cfg));}catch(SQLException expected){rejected=true;}need(rejected,"startup rejects mismatched static source before query");need(field(null,NativeNpcPhysicalSource.class,"current")==original,"failed source validation cannot publish");cases++;
  }finally{c.rollback();}}
  Path fixture=Path.of(args[1]).resolveSibling("unreachable-legacy.properties");Files.writeString(fixture,"URL1_LOGIN=jdbc:mysql://127.0.0.1:1/\nURL2_LOGIN=fixture\nURL3_LOGIN=?connectTimeout=100\nLogin=fixture\nLogin_LOGIN=fixture\nPassword_LOGIN=fixture\n");
  for(String database:List.of(db,"wrong_static_source")){
   var repo=new LegacyMySqlRepository(fixture,"snapshot-fixture",0,"fixture",database,1.0,true,"legacy_mysql");var pool=(HikariDataSource)field(repo,LegacyMySqlRepository.class,"dataSource");
   try{if(database.equals(db)){for(var e:original.rows().entrySet()){need(repo.npcPhysicalStats(e.getKey())==e.getValue(),"native repository uses exact immutable record");lookups++;}for(int id:original.invalid()){boolean rejected=false;try{repo.npcPhysicalStats(id);}catch(SQLException expected){rejected=true;}need(rejected,"invalid row has no SQL fallback");}boolean rejected=false;try{repo.npcPhysicalStats(Integer.MAX_VALUE);}catch(SQLException expected){rejected=true;}need(rejected,"missing row no SQL fallback");}else{boolean rejected=false;try{repo.npcPhysicalStats(original.rows().keySet().iterator().next());}catch(SQLException expected){rejected=true;}need(rejected,"mismatched static source no SQL fallback");}
    need(pool.getHikariPoolMXBean().getTotalConnections()==0,"no connection opened during native repository reads");need(((Map<?,?>)field(repo,LegacyMySqlRepository.class,"npcPhysicalCache")).isEmpty(),"no growing per-repository NPC cache");
   }finally{pool.close();}need(pool.isClosed(),"fixture pool closed");cases++;
  }
  int empty=0,all=0;
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){c.setReadOnly(true);c.setAutoCommit(false);try(var st=c.prepareStatement("SELECT npcid,size FROM npc");var rs=st.executeQuery()){
   while(rs.next()){all++;var size=rs.getString("size");if("".equals(size)){empty++;var row=NativeNpcPhysicalSource.lookup(db+".npc",rs.getInt("npcid"));need(!row.large(),"empty Source size has non-large physical damage semantics");}}
  }finally{c.rollback();}}
  need(empty>0&&original.rows().size()==all&&original.invalid().isEmpty(),"all current static rows load without empty-size rejection");cases++;
  need(field(null,NativeNpcPhysicalSource.class,"current")==original,"original snapshot retained");Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"native_repository_lookups\":"+lookups+",\"valid_sql_rows\":"+original.rows().size()+",\"invalid_sql_rows\":"+original.invalid().size()+",\"source_database_mutated\":false,\"full_goal_achieved\":false}\n");
 }
}
