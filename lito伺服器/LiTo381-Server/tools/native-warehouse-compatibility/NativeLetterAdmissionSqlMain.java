package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import com.zaxxer.hikari.HikariDataSource;

/** Owned real MySQL evidence: actual DAO + private delivery boundary.
 * Selection remains a controlled memory Hub fixture, not authenticated login.
 */
public final class NativeLetterAdmissionSqlMain {
 static final String DATABASE="native_letter_admission";
 static void write(Path root,Map<String,Object> result)throws Exception{
  Path temp=root.resolve("sql-result.tmp");Files.writeString(temp,json(result),StandardCharsets.UTF_8);
  Files.move(temp,root.resolve("sql-result.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
 }
 static String json(Object o){
  if(o==null)return "null";if(o instanceof Number||o instanceof Boolean)return o.toString();
  if(o instanceof Map<?,?> m)return "{"+String.join(",",m.entrySet().stream().map(e->json(e.getKey().toString())+":"+json(e.getValue())).toList())+"}";
  if(o instanceof Collection<?> c)return "["+String.join(",",c.stream().map(NativeLetterAdmissionSqlMain::json).toList())+"]";
  return "\""+o.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";
 }
 static Connection connection(ModernSettlementMysqlFixture mysql)throws SQLException{
  Connection c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));c.setCatalog(DATABASE);return c;
 }
 static LegacyMySqlRepository repository(ModernSettlementMysqlFixture mysql)throws Exception{
  Class<?> u=Class.forName("sun.misc.Unsafe");Field singleton=u.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);
  LegacyMySqlRepository repository=(LegacyMySqlRepository)u.getMethod("allocateInstance",Class.class).invoke(unsafe,LegacyMySqlRepository.class);
  for(var v:Map.of("accountUrl","jdbc:owned-real-native-letter","accountLogin","fixture","accountPassword","fixture","repositoryMode","lito_mysql").entrySet())NativeLetterSqlFaultTest.set(repository,v.getKey(),v.getValue());
  NativeLetterSqlFaultTest.set(repository,"readOnly",false);NativeLetterSqlFaultTest.set(repository,"dataSource",new HikariDataSource(){@Override public Connection getConnection()throws SQLException{return connection(mysql);}});return repository;
 }
 static void schema(ModernSettlementMysqlFixture mysql)throws Exception{
  try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()){
   s.execute("CREATE DATABASE "+DATABASE+" CHARACTER SET utf8");s.execute("USE "+DATABASE);
   for(String ddl:List.of(
    "CREATE TABLE server_info(id INT PRIMARY KEY,minid BIGINT,maxid BIGINT)",
    "CREATE TABLE characters(objid BIGINT PRIMARY KEY,char_name VARCHAR(255))",
    "CREATE TABLE character_pets(objid BIGINT PRIMARY KEY)",
    "CREATE TABLE character_items(id BIGINT PRIMARY KEY,item_id BIGINT,char_id BIGINT,item_name VARCHAR(255),count INT,is_equipped INT,is_sealed INT,enchantlvl INT,is_id INT,durability INT,charge_count INT,bless INT)",
    "CREATE TABLE character_items_time(itemr_obj_id BIGINT PRIMARY KEY)",
    "CREATE TABLE lito_item_origin(item_obj_id BIGINT PRIMARY KEY,char_obj_id BIGINT,catalog_id BIGINT,tournament_reward INT,created_at DATETIME)",
    "CREATE TABLE character_letter(item_object_id BIGINT PRIMARY KEY,code INT,sender VARCHAR(255),receiver VARCHAR(255),date VARCHAR(255),template_id INT,subject BLOB,content BLOB,isCheck INT)"))s.execute(ddl+" ENGINE=MyISAM");
   s.execute("INSERT INTO server_info VALUES(1,10000,10000)");
  }
 }
 static void seed(ModernSettlementMysqlFixture mysql,ClientSession sender,ClientSession recipient)throws Exception{
  try(var c=connection(mysql);var s=c.createStatement()){
   for(String table:List.of("characters","character_items","character_letter","lito_item_origin","character_items_time"))s.execute("DELETE FROM "+table);
   try(var p=c.prepareStatement("INSERT INTO characters VALUES(?,?)")){
    for(ClientSession session:sender==recipient?List.of(sender):List.of(sender,recipient)){
     p.setLong(1,session.nativeLetterSelectedObjectId());p.setString(2,session.playerName());p.executeUpdate();
    }
   }
   long paper=NativeLetterAdmissionTest.paper(sender).objectId();
   s.execute("INSERT INTO character_items(id,item_id,char_id,item_name,count) VALUES("+paper+","+InventoryCatalog.LETTER_PAPER+","+sender.nativeLetterSelectedObjectId()+",'owned paper',1)");
  }
 }
 static int count(ModernSettlementMysqlFixture mysql,String sql)throws Exception{
  try(var c=connection(mysql);var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getInt(1);}
 }
 public static void main(String[] args)throws Exception{
  Path output=Path.of(args[0]).toRealPath();assertTrue(output.toString().matches("[\\x20-\\x7e]+"));assertTrue(output.getFileName().toString().startsWith("native-letter-admission-sql-"));
  Map<String,Object> result=new LinkedHashMap<>();result.put("state","STARTING");result.put("helper_pid",ProcessHandle.current().pid());result.put("helper_start_utc",ProcessHandle.current().info().startInstant().orElseThrow().toString());result.put("GUI",0);result.put("authenticated_selection",false);write(output,result);
  long pid=0;int port=0;
  try(var mysql=ModernSettlementMysqlFixture.open(Path.of("C:/Program Files/MySQL/MySQL Server 5.5"))){
   pid=mysql.processId();port=mysql.port();result.put("mysql_pid",pid);result.put("mysql_port",port);result.put("mysql_start_utc",ProcessHandle.of(pid).orElseThrow().info().startInstant().orElseThrow().toString());result.put("datadir",mysql.dataDirectory().toRealPath().toString());write(output,result);
   schema(mysql);
   List<String> checks=new ArrayList<>();
   try(var pair=new NativePlayerTradeBoundaryTest.Pair(output.resolve("selected"));var dao=repository(mysql)){
    var f=pair.f;NativeLetterAdmissionTest.ready(f.session);NativeLetterAdmissionTest.ready(pair.other);
    NativeLetterAdmissionTest.set(f.session,"inventory",NativeLetterAdmissionTest.inventory(1,0));NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,255));seed(mysql,f.session,pair.other);
    AccountRepository routed=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
     if(m.getName().equals("repositoryForClassicSource"))return p;
     if(m.getName().equals("deliverLetter")){
      RepositoryLetterDelivery delivery=dao.deliverLetter((Long)a[0],(Long)a[1],(String)a[2],(Integer)a[3],(String)a[4],(String)a[5],(String)a[6]);
      NativeLetterAdmissionTest.ids.accumulateAndGet(delivery.letter().itemObjectId()+4096,Math::max);
      // The real DB has already consumed paper and persisted body+origin.
      // A replacement ledger deliberately invalidates runtime slot admission.
      NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,256));return delivery;
     }
     if(m.getName().equals("letters"))return dao.letters((Long)a[0]);
     return m.invoke(f.accounts,a);
    });
    NativeLetterAdmissionTest.set(f.session,"accounts",routed);NativeLetterAdmissionTest.set(pair.other,"accounts",routed);
    long paper=NativeLetterAdmissionTest.paper(f.session).objectId();NativeLetterAdmissionTest.send(f.session,pair.other);
    RepositoryLetter letter=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);
    assertEquals(0,count(mysql,"SELECT COUNT(*) FROM character_items WHERE id="+paper));
    assertEquals(1,count(mysql,"SELECT COUNT(*) FROM character_letter WHERE item_object_id="+letter.itemObjectId()));
    assertEquals(1,count(mysql,"SELECT COUNT(*) FROM lito_item_origin WHERE item_obj_id="+letter.itemObjectId()));
    assertNull(NativeLetterAdmissionTest.inv(pair.other).item(letter.itemObjectId()));assertTrue(f.hub.nativeLetterReconcilePending(pair.other));assertTrue(pair.drain().isEmpty());checks.add("real DAO DELIVERED survives runtime full; paper absent and item/body/origin durable");
    NativeLetterAdmissionTest.inv(pair.other).removeEntire(NativeLetterAdmissionTest.inv(pair.other).items().get(0).objectId());NativeLetterAdmissionTest.reconcile(pair.other);
    assertNotNull(NativeLetterAdmissionTest.inv(pair.other).item(letter.itemObjectId()));assertEquals(256,NativeLetterAdmissionTest.inv(pair.other).size());assertFalse(f.hub.nativeLetterReconcilePending(pair.other));assertEquals(2,pair.drain().size());checks.add("authoritative real DAO letters reread admits exact stable ID after space");
    try(var reopened=repository(mysql)){assertEquals(List.of(letter),reopened.letters(pair.other.nativeLetterSelectedObjectId()).letters());}checks.add("independent repository+connection reread same durable body and stable ID");
    NativeLetterAdmissionTest.set(f.session,"inventory",NativeLetterAdmissionTest.inventory(1,0));NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,0));seed(mysql,f.session,pair.other);
    NativeLetterAdmissionTest.send(f.session,pair.other);letter=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);
    assertTrue(f.hub.nativeLetterReconcilePending(pair.other));
    // An external authoritative delete before runtime reconcile. The retained
    // deleteLetter method has a separately recorded item_obj_id/itemr_obj_id
    // mismatch in character_items_time; do not change fixture DDL to hide it.
    try(var c=connection(mysql);var s=c.createStatement()){
     s.executeUpdate("DELETE FROM character_letter WHERE item_object_id="+letter.itemObjectId());
     s.executeUpdate("DELETE FROM character_items WHERE id="+letter.itemObjectId());
     s.executeUpdate("DELETE FROM lito_item_origin WHERE item_obj_id="+letter.itemObjectId());
    }
    NativeLetterAdmissionTest.reconcile(pair.other);assertFalse(f.hub.nativeLetterReconcilePending(pair.other));assertNull(NativeLetterAdmissionTest.inv(pair.other).item(letter.itemObjectId()));checks.add("external real SQL delete before deferred authoritative DAO reread does not resurrect obsolete letter");
   }
   result.put("checks",checks);result.put("real_SQL",true);result.put("source","minimal owned MyISAM schema; actual retained candidate deliverLetter/letters, external SQL delete; controlled registered memory selection and runtime replacement callback");result.put("state","PASSED_REAL_SQL_LETTER_DURABILITY");write(output,result);
   mysql.successful();
  }finally{
   result.put("mysql_process_retired",pid!=0&&ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true));
   if(port!=0)try(var socket=new java.net.ServerSocket(port,1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("mysql_listener_released",true);}catch(Exception e){result.put("mysql_listener_released",false);}write(output,result);
  }
  System.out.println(json(result));
 }
}
