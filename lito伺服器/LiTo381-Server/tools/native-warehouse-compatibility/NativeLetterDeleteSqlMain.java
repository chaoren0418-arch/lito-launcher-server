package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import com.zaxxer.hikari.HikariDataSource;

/** Real MySQL 5.5 + exact selected locked-source DDL. No formal config/store. */
public final class NativeLetterDeleteSqlMain {
 static final String A="native_letter_delete_a",B="native_letter_delete_b";
 static String json(Object o){return NativeLetterAdmissionSqlMain.json(o);}
 static void write(Path root,Map<String,Object> result)throws Exception{
  Path temp=root.resolve("sql-result.tmp");Files.writeString(temp,json(result),StandardCharsets.UTF_8);
  Files.move(temp,root.resolve("sql-result.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
 }
 static Connection connection(ModernSettlementMysqlFixture mysql,String schema)throws SQLException{
  Connection c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));c.setCatalog(schema);return c;
 }
 static LegacyMySqlRepository repository(ModernSettlementMysqlFixture mysql,String schema)throws Exception{
  Class<?> u=Class.forName("sun.misc.Unsafe");Field singleton=u.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);
  LegacyMySqlRepository repository=(LegacyMySqlRepository)u.getMethod("allocateInstance",Class.class).invoke(unsafe,LegacyMySqlRepository.class);
  for(var v:Map.of("accountUrl","jdbc:owned-real-delete:"+schema,"accountLogin","fixture","accountPassword","fixture","repositoryMode","lito_mysql").entrySet())NativeLetterSqlFaultTest.set(repository,v.getKey(),v.getValue());
  NativeLetterSqlFaultTest.set(repository,"readOnly",false);NativeLetterSqlFaultTest.set(repository,"dataSource",new HikariDataSource(){@Override public Connection getConnection()throws SQLException{return connection(mysql,schema);}});return repository;
 }
 static void schema(ModernSettlementMysqlFixture mysql,Path root)throws Exception{
  for(var entry:Map.of(A,"locked-380_ac_1-selected-ddl.sql",B,"locked-380_ac_2-selected-ddl.sql").entrySet())try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()){
   s.execute("CREATE DATABASE "+entry.getKey()+" CHARACTER SET utf8");s.execute("USE "+entry.getKey());
   String ddl=Files.readString(root.resolve(entry.getValue()),StandardCharsets.UTF_8);
   for(String statement:ddl.split(";"))if(!statement.isBlank()){assertTrue(statement.trim().startsWith("CREATE TABLE"));s.execute(statement);}
   s.execute("INSERT INTO server_info(id,minid,maxid) VALUES(1,10000,10000)");
  }
 }
 static void seed(ModernSettlementMysqlFixture mysql,String schema,long sender,long receiver,String senderName,String receiverName,long paper)throws Exception{
  try(var c=connection(mysql,schema);var s=c.createStatement()){
   s.execute("DROP TRIGGER IF EXISTS owned_delete_fault");s.execute("DROP TRIGGER IF EXISTS owned_restore_fault");
   for(String table:List.of("characters","character_items","character_letter","lito_item_origin","character_items_time","character_pets"))s.execute("DELETE FROM "+table);
   try(var p=c.prepareStatement("INSERT INTO characters(objid,char_name) VALUES(?,?)")){
    for(var row:Map.of(sender,senderName,receiver,receiverName).entrySet()){p.setLong(1,row.getKey());p.setString(2,row.getValue());p.executeUpdate();}
   }
   s.execute("INSERT INTO character_items(id,item_id,char_id,item_name,count) VALUES("+paper+","+InventoryCatalog.LETTER_PAPER+","+sender+",'owned paper',1)");
  }
 }
 static RepositoryLetter letter(ModernSettlementMysqlFixture mysql,LegacyMySqlRepository dao,boolean metadata)throws Exception{
  seed(mysql,A,10000,20000,"Sender","Receiver",10001);
  RepositoryLetterDelivery delivered=dao.deliverLetter(10000,10001,"Receiver",7,"26/10/04","刪除主旨","actual persisted body");assertTrue(delivered.delivered());
  if(metadata)try(var c=connection(mysql,A);var s=c.createStatement()){
   s.execute("INSERT INTO character_items_time(itemr_obj_id,usertime) VALUES("+delivered.letter().itemObjectId()+",'2030-01-01 00:00:00')");
   // Owned orphan pet metadata exercises the retained cleanup; not a gameplay
   // claim that physical letter catalog can be a legitimate linked pet collar.
   s.execute("INSERT INTO character_pets(item_obj_id,objid) VALUES("+delivered.letter().itemObjectId()+",50000)");
  }
  return delivered.letter();
 }
 static int count(ModernSettlementMysqlFixture mysql,String schema,String sql)throws Exception{
  try(var c=connection(mysql,schema);var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getInt(1);}
 }
 static Map<String,Object> poststate(ModernSettlementMysqlFixture mysql,RepositoryLetter letter)throws Exception{
  Map<String,Object> state=new LinkedHashMap<>();long id=letter.itemObjectId();
  for(var row:Map.of("body","character_letter WHERE item_object_id=","item","character_items WHERE id=","time","character_items_time WHERE itemr_obj_id=","pet","character_pets WHERE item_obj_id=","origin","lito_item_origin WHERE item_obj_id=").entrySet())state.put(row.getKey(),count(mysql,A,"SELECT COUNT(*) FROM "+row.getValue()+id));return state;
 }
 static void fault(ModernSettlementMysqlFixture mysql,String table,boolean restore)throws Exception{
  try(var c=connection(mysql,A);var s=c.createStatement()){
   s.execute("CREATE TRIGGER "+(restore?"owned_restore_fault":"owned_delete_fault")+" BEFORE "+(restore?"INSERT":"DELETE")+" ON "+table+" FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='owned "+table+" fault'");
  }
 }
 static void dropFault(ModernSettlementMysqlFixture mysql)throws Exception{
  try(var c=connection(mysql,A);var s=c.createStatement()){s.execute("DROP TRIGGER IF EXISTS owned_delete_fault");s.execute("DROP TRIGGER IF EXISTS owned_restore_fault");}
 }
 static void expectCore(Map<String,Object> state,int body,int item){assertEquals(body,state.get("body"));assertEquals(item,state.get("item"));}
 static byte[] fieldBytes(ModernSettlementMysqlFixture mysql,long id,String field)throws Exception{
  assertTrue(List.of("subject","content").contains(field));try(var c=connection(mysql,A);var s=c.prepareStatement("SELECT "+field+" FROM character_letter WHERE item_object_id=?")){s.setLong(1,id);try(var r=s.executeQuery()){assertTrue(r.next());return r.getBytes(1);}}
 }
 static void injectCoreStatementCloseFailure(LegacyMySqlRepository dao,ModernSettlementMysqlFixture mysql,String table)throws Exception{
  java.util.concurrent.atomic.AtomicBoolean fired=new java.util.concurrent.atomic.AtomicBoolean();
  NativeLetterSqlFaultTest.set(dao,"dataSource",new HikariDataSource(){@Override public Connection getConnection()throws SQLException{
   Connection raw=connection(mysql,A);
   return NativeLetterSqlFaultTest.proxy(Connection.class,(p,m,a)->{
    try{
     if(m.getName().equals("prepareStatement")&&a[0] instanceof String sql&&sql.startsWith("DELETE FROM "+table+" WHERE")){
      PreparedStatement real=(PreparedStatement)m.invoke(raw,a);java.util.concurrent.atomic.AtomicBoolean executed=new java.util.concurrent.atomic.AtomicBoolean();
      return NativeLetterSqlFaultTest.proxy(PreparedStatement.class,(pp,mm,aa)->{
       try{
        if(mm.getName().equals("close")&&executed.get()&&fired.compareAndSet(false,true)){real.close();throw new SQLException("owned JDBC close failure after real SQL execution");}
        Object result=mm.invoke(real,aa);if(mm.getName().equals("executeUpdate"))executed.set(true);return result;
       }catch(InvocationTargetException failure){throw failure.getCause();}
      });
     }
     return m.invoke(raw,a);
    }catch(InvocationTargetException failure){throw failure.getCause();}
   });
  }});
 }
 static void pendingReceipt(ModernSettlementMysqlFixture mysql,LegacyMySqlRepository dao,Path output,List<Object> checks)throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(output.resolve("pending"))){
   var f=pair.f;NativeLetterAdmissionTest.ready(f.session);NativeLetterAdmissionTest.ready(pair.other);
   NativeLetterAdmissionTest.set(f.session,"inventory",NativeLetterAdmissionTest.inventory(1,0));NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,255));
   seed(mysql,A,f.session.nativeLetterSelectedObjectId(),pair.other.nativeLetterSelectedObjectId(),f.session.playerName(),pair.other.playerName(),NativeLetterAdmissionTest.paper(f.session).objectId());
   java.util.concurrent.atomic.AtomicBoolean defer=new java.util.concurrent.atomic.AtomicBoolean(true);
   AccountRepository routed=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return p;
    if(m.getName().equals("deliverLetter")){
     var delivered=dao.deliverLetter((Long)a[0],(Long)a[1],(String)a[2],(Integer)a[3],(String)a[4],(String)a[5],(String)a[6]);
     NativeLetterAdmissionTest.ids.accumulateAndGet(delivered.letter().itemObjectId()+4096,Math::max);if(defer.get())NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,256));return delivered;
    }
    if(m.getName().equals("letters"))return dao.letters((Long)a[0]);if(m.getName().equals("openLetter"))return dao.openLetter((Long)a[0],(RepositoryLetter)a[1]);if(m.getName().equals("deleteLetter"))return dao.deleteLetter((Long)a[0],(RepositoryLetter)a[1]);return m.invoke(f.accounts,a);
   });NativeLetterAdmissionTest.set(f.session,"accounts",routed);NativeLetterAdmissionTest.set(pair.other,"accounts",routed);
   NativeLetterAdmissionTest.send(f.session,pair.other);RepositoryLetter delivered=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);assertTrue(f.hub.nativeLetterReconcilePending(pair.other));assertNull(NativeLetterAdmissionTest.inv(pair.other).item(delivered.itemObjectId()));
   assertEquals(RepositoryLetterMutationStatus.SUCCESS,dao.deleteLetter(pair.other.nativeLetterSelectedObjectId(),delivered).status());
   NativeLetterAdmissionTest.inv(pair.other).removeEntire(NativeLetterAdmissionTest.inv(pair.other).items().get(0).objectId());NativeLetterAdmissionTest.reconcile(pair.other);
   assertFalse(f.hub.nativeLetterReconcilePending(pair.other));assertNull(NativeLetterAdmissionTest.inv(pair.other).item(delivered.itemObjectId()));assertTrue(pair.drain().isEmpty());expectCore(poststate(mysql,delivered),0,0);
   checks.add(Map.of("case","actual DAO deletion precedes T16 pending receipt reconcile; not external SQL DELETE","poststate",poststate(mysql,delivered),"runtime_resurrected",false));
   defer.set(false);NativeLetterAdmissionTest.set(f.session,"inventory",NativeLetterAdmissionTest.inventory(1,0));NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,0));
   seed(mysql,A,f.session.nativeLetterSelectedObjectId(),pair.other.nativeLetterSelectedObjectId(),f.session.playerName(),pair.other.playerName(),NativeLetterAdmissionTest.paper(f.session).objectId());
   NativeLetterAdmissionTest.send(f.session,pair.other);RepositoryLetter active=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);assertNotNull(NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()));pair.drain();
   Method read=ClientSession.class.getDeclaredMethod("useSqlPhysicalLetter",RuntimeInventory.Item.class);read.setAccessible(true);read.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()));
   RepositoryLetter opened=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);assertTrue(opened.opened());assertEquals(InventoryCatalog.LETTER_OPEN,NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()).definition().catalogId());
   assertEquals(1,count(mysql,A,"SELECT COUNT(*) FROM character_items WHERE id="+active.itemObjectId()+" AND item_id="+InventoryCatalog.LETTER_OPEN));
   read.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()));var reads=pair.drain();assertEquals(2,reads.size());
   byte[] expectedRead=Protocol.readMail(opened.itemObjectId(),InventoryCatalog.LETTER_OPEN_INVENTORY_GFX,opened.code(),opened.sender(),opened.receiver(),opened.subject(),opened.content(),opened.templateId(),opened.date());for(var packet:reads)assertArrayEquals(expectedRead,packet);
   checks.add(Map.of("case","actual private player open and repeat read execute real selected-source DAO with exact content packets","poststate",poststate(mysql,opened),"real_opened_backing_catalog",InventoryCatalog.LETTER_OPEN,"authenticated_selection",false));
   Method destroy=ClientSession.class.getDeclaredMethod("destroySqlPhysicalLetter",RuntimeInventory.Item.class);destroy.setAccessible(true);destroy.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()));
   assertNull(NativeLetterAdmissionTest.inv(pair.other).item(active.itemObjectId()));assertTrue(dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().isEmpty());expectCore(poststate(mysql,active),0,0);var removed=pair.drain();
   // Parent baseline additionally refreshes status; the candidate publishes
   // exactly the captured physical removal under the transport monitor.
   assertFalse(removed.isEmpty());assertArrayEquals(Protocol.removeInventory(active.itemObjectId()),removed.get(0));checks.add(Map.of("case","actual private player delete handler executes real DAO and publishes physical removal","poststate",poststate(mysql,active),"authenticated_selection",false));
  }
 }
 public static void main(String[] args)throws Exception{
  Path output=Path.of(args[0]).toRealPath();assertTrue(output.toString().matches("[\\x20-\\x7e]+"));assertTrue(output.getFileName().toString().startsWith("native-letter-delete-sql-"));boolean baseline=args[1].equals("baseline");
  Map<String,Object> result=new LinkedHashMap<>();result.put("state","STARTING");result.put("mode",args[1]);result.put("helper_pid",ProcessHandle.current().pid());result.put("helper_start_utc",ProcessHandle.current().info().startInstant().orElseThrow().toString());result.put("GUI",0);result.put("authenticated_selection",false);write(output,result);long pid=0;int port=0;
  Path artifact=Path.of(LegacyMySqlRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
  String artifactSHA=java.util.HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));assertEquals(System.getProperty("lito.delete.expectedSHA"),artifactSHA);result.put("actual_DAO_artifact",artifact.toString());result.put("actual_DAO_artifact_SHA",artifactSHA);write(output,result);
  try(var mysql=ModernSettlementMysqlFixture.open(Path.of("C:/Program Files/MySQL/MySQL Server 5.5"))){
   pid=mysql.processId();port=mysql.port();result.put("mysql_pid",pid);result.put("mysql_port",port);result.put("mysql_start_utc",ProcessHandle.of(pid).orElseThrow().info().startInstant().orElseThrow().toString());result.put("datadir",mysql.dataDirectory().toRealPath().toString());write(output,result);schema(mysql,output);List<Object> checks=new ArrayList<>();
   try(var c=connection(mysql,A);var s=c.createStatement();var rows=s.executeQuery("SELECT TABLE_SCHEMA,TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA IN ('"+A+"','"+B+"') ORDER BY TABLE_SCHEMA,TABLE_NAME")){
    List<Object> engines=new ArrayList<>();while(rows.next()){assertEquals("MyISAM",rows.getString(3));engines.add(Map.of("schema",rows.getString(1),"table",rows.getString(2),"engine",rows.getString(3)));}assertEquals(14,engines.size());result.put("actual_table_engines",engines);
   }
   try(var dao=repository(mysql,A);var foreign=repository(mysql,B)){
    for(boolean metadata:List.of(false,true))for(boolean opened:List.of(false,true)){
     RepositoryLetter letter=letter(mysql,dao,metadata);if(opened){var open=dao.openLetter(20000,letter);assertEquals(RepositoryLetterMutationStatus.SUCCESS,open.status());letter=open.letter();assertTrue(letter.opened());}
     assertEquals(RepositoryLetterMutationStatus.SUCCESS,dao.deleteLetter(20000,letter).status());var state=poststate(mysql,letter);for(Object value:state.values())assertEquals(0,value);assertTrue(dao.letters(20000).letters().isEmpty());assertEquals(RepositoryLetterMutationStatus.NOT_FOUND,dao.deleteLetter(20000,letter).status());checks.add(Map.of("case","normal+repeat deletion metadata="+metadata+" opened="+opened,"poststate",state));
    }
    RepositoryLetter letter=letter(mysql,dao,true);var before=poststate(mysql,letter);
    assertEquals(RepositoryLetterMutationStatus.NOT_FOUND,dao.deleteLetter(10000,letter).status());assertEquals(before,poststate(mysql,letter));
    seed(mysql,B,10000,20000,"Sender","Receiver",10001);assertEquals(RepositoryLetterMutationStatus.NOT_FOUND,foreign.deleteLetter(20000,letter).status());assertEquals(before,poststate(mysql,letter));
    try(var c=connection(mysql,B);var s=c.createStatement()){s.executeUpdate("UPDATE server_info SET maxid="+(letter.itemObjectId()-1)+" WHERE id=1");}
    var other=foreign.deliverLetter(10000,10001,"Receiver",1,"26/10/04","foreign","different body");assertTrue(other.delivered());assertEquals(letter.itemObjectId(),other.letter().itemObjectId());assertEquals(RepositoryLetterMutationStatus.CONFLICT,foreign.deleteLetter(20000,letter).status());assertEquals(List.of(other.letter()),foreign.letters(20000).letters());checks.add("wrong physical recipient+foreign source absent/conflicting same-ID body refuse without mutating owned letter");
    NativeLetterSqlFaultTest.set(dao,"readOnly",true);assertEquals(RepositoryLetterMutationStatus.READ_ONLY,dao.deleteLetter(20000,letter).status());NativeLetterSqlFaultTest.set(dao,"readOnly",false);assertEquals(before,poststate(mysql,letter));checks.add("read-only delete preserves all rows");
    for(String table:List.of("character_letter","character_items","character_pets","character_items_time","lito_item_origin")){
     letter=letter(mysql,dao,true);if(table.equals("character_items")){var opened=dao.openLetter(20000,letter);assertEquals(RepositoryLetterMutationStatus.SUCCESS,opened.status());letter=opened.letter();}
     byte[] subjectBefore=fieldBytes(mysql,letter.itemObjectId(),"subject"),contentBefore=fieldBytes(mysql,letter.itemObjectId(),"content");fault(mysql,table,false);RepositoryLetter original=letter;RepositoryLetterMutation success=null;SQLException failure=null;
     try{success=dao.deleteLetter(20000,original);}catch(SQLException e){failure=e;}
     Map<String,Object> state=poststate(mysql,letter);Map<String,Object> entry=new LinkedHashMap<>();entry.put("case","real BEFORE DELETE SIGNAL "+table);entry.put("poststate",state);entry.put("exception",failure!=null);entry.put("status",success==null?null:success.status().name());
     if(baseline){assertNotNull(failure);if(table.equals("character_letter"))expectCore(state,1,1);else expectCore(state,0,table.equals("lito_item_origin")?0:1);}
     else if(table.equals("character_letter")||table.equals("character_items")){assertNotNull(failure);expectCore(state,1,1);assertEquals(1,state.get("time"));assertEquals(1,state.get("pet"));assertEquals(1,state.get("origin"));assertEquals(List.of(letter),dao.letters(20000).letters());assertArrayEquals(subjectBefore,fieldBytes(mysql,letter.itemObjectId(),"subject"));assertArrayEquals(contentBefore,fieldBytes(mysql,letter.itemObjectId(),"content"));entry.put("raw_subject_content_bytes_preserved",true);entry.put("opened",letter.opened());}
     else{assertNull(failure);assertEquals(RepositoryLetterMutationStatus.SUCCESS,success.status());expectCore(state,0,0);assertTrue(dao.letters(20000).letters().isEmpty());assertEquals(1,state.get(table.equals("character_pets")?"pet":table.equals("character_items_time")?"time":"origin"));}
     dropFault(mysql);if(!baseline&&failure!=null){assertEquals(RepositoryLetterMutationStatus.SUCCESS,dao.deleteLetter(20000,letter).status());expectCore(poststate(mysql,letter),0,0);entry.put("retry_after_fault_removed","SUCCESS");}checks.add(entry);
    }
    if(!baseline){
     letter=letter(mysql,dao,true);fault(mysql,"character_items",false);fault(mysql,"character_letter",true);RepositoryLetter original=letter;SQLException failed=assertThrows(SQLException.class,()->dao.deleteLetter(20000,original));assertTrue(failed.getSuppressed().length>0);var state=poststate(mysql,letter);expectCore(state,0,1);assertEquals(1,state.get("time"));assertEquals(1,state.get("pet"));assertEquals(1,state.get("origin"));checks.add(Map.of("case","item deletion AND body compensation insertion fail; NOT a rollback","poststate",state,"suppressed_compensation_failure",true));dropFault(mysql);
     for(String table:List.of("character_letter","character_items")){
      letter=letter(mysql,dao,true);injectCoreStatementCloseFailure(dao,mysql,table);RepositoryLetter closing=letter;
      if(table.equals("character_letter")){assertThrows(SQLException.class,()->dao.deleteLetter(20000,closing));expectCore(poststate(mysql,letter),1,1);assertEquals(List.of(letter),dao.letters(20000).letters());}
      else{assertEquals(RepositoryLetterMutationStatus.SUCCESS,dao.deleteLetter(20000,closing).status());expectCore(poststate(mysql,letter),0,0);assertTrue(dao.letters(20000).letters().isEmpty());}
      checks.add(Map.of("case","owned JDBC statement-close injection after real SQL "+table,"poststate",poststate(mysql,letter),"real_SQL_rows",true,"JDBC_fault_wrapper",true));
     }
    }
    pendingReceipt(mysql,dao,output,checks);
   }
   result.put("checks",checks);result.put("real_SQL",true);result.put("transaction_rollback_claim",false);result.put("state",baseline?"PASSED_BASELINE_REAL_DDL_PARTIAL_STATE_REPRODUCTION":"PASSED_CANDIDATE_REAL_DDL_DELETE_BOUNDARY");write(output,result);mysql.successful();
  }finally{result.put("mysql_process_retired",pid!=0&&ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true));if(port!=0)try(var socket=new java.net.ServerSocket(port,1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("mysql_listener_released",true);}catch(Exception e){result.put("mysql_listener_released",false);}write(output,result);}
  System.out.println(json(result));
 }
}
