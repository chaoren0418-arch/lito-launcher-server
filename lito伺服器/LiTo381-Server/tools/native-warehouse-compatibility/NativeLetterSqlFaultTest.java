package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeLetterSqlFaultTest {
 @TempDir Path temporary;
 @Test void postcommitOnlineRuntimeAdmissionFailureHasConcreteUnfixedPoststate()throws Exception {
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;AtomicLong ids=new AtomicLong(0x7A000000L);
   NativeWarehouseCompatibilityTest.set(f.session,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.LETTER_PAPER,1),ids::incrementAndGet));
   RuntimeInventory.Item paper=f.item(InventoryCatalog.LETTER_PAPER);boolean[] sourceDelivered={false};
   AccountRepository original=f.accounts;
   AccountRepository mocked=proxy(AccountRepository.class,(p,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return p;
    if(m.getName().equals("deliverLetter")){
     sourceDelivered[0]=true;
     NativeWarehouseCompatibilityTest.set(pair.other,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.LETTER_CLOSED,RuntimeInventory.MAX_SLOTS),ids::incrementAndGet));
     return new RepositoryLetterDelivery(RepositoryLetterDeliveryStatus.DELIVERED,new RepositoryLetter(30000,1,f.session.playerName(),pair.other.playerName(),"26/10/04",0,"Title","Body",false));
    }
    return m.invoke(original,a);
   });
   NativeWarehouseCompatibilityTest.set(f.session,"accounts",mocked);
   Method send=ClientSession.class.getDeclaredMethod("useSqlLetterPaper",RuntimeInventory.Item.class,Protocol.LetterComposeInventoryUse.class,ClientSession.class);send.setAccessible(true);
   var error=assertThrows(InvocationTargetException.class,()->send.invoke(f.session,paper,new Protocol.LetterComposeInventoryUse(paper.objectId(),1,pair.other.playerName(),"Title","Body"),pair.other));
   assertInstanceOf(IllegalStateException.class,error.getCause());assertEquals("SQL physical-letter delivery cannot admit online receiver item",error.getCause().getMessage());
   assertTrue(sourceDelivered[0]);assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(RuntimeInventory.MAX_SLOTS,((RuntimeInventory)NativeWarehouseCompatibilityTest.get(pair.other,"inventory")).size());
   assertFalse(((RuntimeInventory)NativeWarehouseCompatibilityTest.get(pair.other,"inventory")).items().stream().anyMatch(i->i.objectId()==30000));
   assertTrue(f.drain().isEmpty());assertTrue(pair.drain().isEmpty());
  }
 }
 @Test void metadataFailureAfterPaperCommitRetainsRecipientAndReturnsDelivered()throws Exception {
  for(String point:List.of("timeCleanup","originCleanup")){
   Db db=new Db(point);try(var repository=repository(db)){
    var result=repository.deliverLetter(10000,10001,"Receiver",1,"26/10/04","Title","Body");
    assertTrue(result.delivered());assertEquals(30000,result.letter().itemObjectId());
    assertFalse(db.paper);assertTrue(db.recipientItem);assertTrue(db.body);assertTrue(db.recipientOrigin);
    assertTrue(db.unlocked);assertTrue(db.closed);assertEquals(1,db.consumeCalls);
    assertTrue(db.cleanupTimeAttempted);assertTrue(db.cleanupOriginAttempted);
    if(point.equals("timeCleanup")){assertTrue(db.paperTime);assertFalse(db.paperOrigin);}else{assertFalse(db.paperTime);assertTrue(db.paperOrigin);}
   }
  }
 }
 @Test void failureBeforePaperConsumptionCompensatesReceiverRowsAndKeepsPaper()throws Exception {
  for(String point:List.of("bodyInsert","originInsert","paperDelete")){
   Db db=new Db(point);try(var repository=repository(db)){
    assertThrows(SQLException.class,()->repository.deliverLetter(10000,10001,"Receiver",1,"26/10/04","Title","Body"));
    assertTrue(db.paper);assertFalse(db.recipientItem);assertFalse(db.body);assertFalse(db.recipientOrigin);assertTrue(db.paperTime);assertTrue(db.paperOrigin);assertTrue(db.unlocked);
   }
  }
 }
 @Test void baselineConcreteLossPoststate()throws Exception {
  for(String point:List.of("timeCleanup","originCleanup")){
   Db db=new Db(point);try(var repository=repository(db)){
    assertThrows(SQLException.class,()->repository.deliverLetter(10000,10001,"Receiver",1,"26/10/04","Title","Body"));
    assertFalse(db.paper);assertFalse(db.recipientItem);assertFalse(db.body);assertFalse(db.recipientOrigin);assertTrue(db.unlocked);assertEquals(1,db.consumeCalls);
   }
  }
 }
 // Test-only allocation avoids repository startup/driver/network/config paths.
 // Actual deliverLetter SQL-control-flow executes against strict in-process JDBC proxies.
 static LegacyMySqlRepository repository(Db db)throws Exception {
  Class<?> u=Class.forName("sun.misc.Unsafe");Field singleton=u.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);
  LegacyMySqlRepository repository=(LegacyMySqlRepository)u.getMethod("allocateInstance",Class.class).invoke(unsafe,LegacyMySqlRepository.class);
  for(var value:Map.of("accountUrl","jdbc:owned-test:no-network","accountLogin","fixture","accountPassword","fixture","repositoryMode","lito_mysql").entrySet())set(repository,value.getKey(),value.getValue());
  set(repository,"readOnly",false);set(repository,"dataSource",new HikariDataSource(){@Override public Connection getConnection(){return db.connection();}});return repository;
 }
 static void set(Object value,String name,Object replacement)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,replacement);}
 static final class Db {
  final String fail;boolean paper=true,paperTime=true,paperOrigin=true,recipientItem,body,recipientOrigin,unlocked,closed,cleanupTimeAttempted,cleanupOriginAttempted;int consumeCalls;
  Db(String fail){this.fail=fail;}
  Connection connection(){return proxy(Connection.class,(p,m,a)->switch(m.getName()){
   case "createStatement"->statement();case "prepareStatement"->prepared((String)a[0]);case "close"->{closed=true;yield null;}default->unexpected(m);
  });}
  Statement statement(){return proxy(Statement.class,(p,m,a)->switch(m.getName()){
   case "execute"->{String sql=(String)a[0];assertTrue(sql.startsWith("LOCK TABLES")||sql.equals("UNLOCK TABLES"));if(sql.equals("UNLOCK TABLES"))unlocked=true;yield true;}
   case "executeUpdate"->{assertTrue(((String)a[0]).startsWith("UPDATE server_info"));yield 1;}
   case "executeQuery"->{assertEquals("SELECT LAST_INSERT_ID()",a[0]);yield row(30000L);}
   case "close"->null;default->unexpected(m);
  });}
  PreparedStatement prepared(String sql){Map<Integer,Object> args=new HashMap<>();return proxy(PreparedStatement.class,(p,m,a)->{
   if(m.getName().startsWith("set")){args.put((Integer)a[0],a[1]);return null;}
   return switch(m.getName()){
    case "executeQuery"->{
     if(sql.startsWith("SELECT char_name"))yield row("Sender");
     if(sql.startsWith("SELECT objid,char_name"))yield row(20000L,"Receiver");
     if(sql.startsWith("SELECT count FROM character_items"))yield row(paper?1:0);
     if(sql.startsWith("SELECT COUNT(*)"))yield row(0);
     throw new AssertionError(sql);
    }
    case "executeUpdate"->update(sql,args);case "close"->null;default->unexpected(m);
   };
  });}
  int update(String sql,Map<Integer,Object> args)throws SQLException {
   if(sql.startsWith("INSERT INTO character_items ")){recipientItem=true;return 1;}
   if(sql.startsWith("INSERT INTO character_letter")){fault("bodyInsert");body=true;return 1;}
   if(sql.startsWith("INSERT INTO lito_item_origin")){fault("originInsert");recipientOrigin=true;return 1;}
   if(sql.startsWith("DELETE FROM character_items WHERE")){
    if(((Number)args.get(1)).longValue()==10001){fault("paperDelete");paper=false;consumeCalls++;}else recipientItem=false;return 1;
   }
   if(sql.startsWith("DELETE FROM character_items_time")){cleanupTimeAttempted=true;fault("timeCleanup");paperTime=false;return 1;}
   if(sql.startsWith("DELETE FROM lito_item_origin")){
    if(((Number)args.get(1)).longValue()==10001){cleanupOriginAttempted=true;fault("originCleanup");paperOrigin=false;}else recipientOrigin=false;return 1;
   }
   if(sql.startsWith("DELETE FROM character_letter")){body=false;return 1;}
   throw new AssertionError(sql);
  }
  void fault(String point)throws SQLException{if(fail.equals(point))throw new SQLException("owned JDBC injected "+point);}
 }
 static ResultSet row(Object... values){boolean[] next={false};return proxy(ResultSet.class,(p,m,a)->switch(m.getName()){
  case "next"->{boolean first=!next[0];next[0]=true;yield first;}
  case "getLong"->((Number)values[(Integer)a[0]-1]).longValue();case "getInt"->((Number)values[(Integer)a[0]-1]).intValue();case "getString"->(String)values[(Integer)a[0]-1];case "close"->null;default->unexpected(m);
 });}
 static Object unexpected(Method m){throw new AssertionError("unexpected JDBC method "+m);}
 static <T>T proxy(Class<T> type,InvocationHandler h){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h));}
}
