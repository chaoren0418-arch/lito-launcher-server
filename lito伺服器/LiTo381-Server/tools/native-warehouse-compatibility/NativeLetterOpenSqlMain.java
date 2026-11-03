package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import com.zaxxer.hikari.HikariDataSource;

/** Actual owned MySQL5.5, retained full MyISAM DDL, actual open DAO. */
public final class NativeLetterOpenSqlMain {
 static final String A=NativeLetterDeleteSqlMain.A;
 static Connection connection(ModernSettlementMysqlFixture mysql)throws SQLException{return NativeLetterDeleteSqlMain.connection(mysql,A);}
 static int value(ModernSettlementMysqlFixture mysql,String sql)throws Exception{return NativeLetterDeleteSqlMain.count(mysql,A,sql);}
 static Map<String,Object> state(ModernSettlementMysqlFixture mysql,RepositoryLetter row)throws Exception{
  long id=row.itemObjectId();Map<String,Object> r=new LinkedHashMap<>(NativeLetterDeleteSqlMain.poststate(mysql,row));
  r.put("backing_catalog",(long)value(mysql,"SELECT item_id FROM character_items WHERE id="+id));r.put("body_opened",value(mysql,"SELECT isCheck FROM character_letter WHERE item_object_id="+id));r.put("origin_catalog",(long)value(mysql,"SELECT catalog_id FROM lito_item_origin WHERE item_obj_id="+id));return r;
 }
 static void core(Map<String,Object> r,boolean opened){assertEquals(1,r.get("body"));assertEquals(1,r.get("item"));assertEquals(opened?1:0,r.get("body_opened"));assertEquals(opened?InventoryCatalog.LETTER_OPEN:InventoryCatalog.LETTER_CLOSED,r.get("backing_catalog"));}
 static void fault(ModernSettlementMysqlFixture mysql,String table,boolean restore)throws Exception{
  try(var c=connection(mysql);var s=c.createStatement()){
   s.execute("CREATE TRIGGER "+(restore?"owned_open_restore_fault":"owned_open_fault")+" BEFORE UPDATE ON "+table+" FOR EACH ROW "+(restore?"BEGIN IF NEW.item_id="+InventoryCatalog.LETTER_CLOSED+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='owned open compensation fault'; END IF; END":"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='owned open "+table+" fault'"));
  }
 }
 static void drop(ModernSettlementMysqlFixture mysql)throws Exception{try(var c=connection(mysql);var s=c.createStatement()){s.execute("DROP TRIGGER IF EXISTS owned_open_fault");s.execute("DROP TRIGGER IF EXISTS owned_open_restore_fault");}}
 static void closeFault(LegacyMySqlRepository dao,ModernSettlementMysqlFixture mysql,String table)throws Exception{jdbcFault(dao,mysql,table,false);}
 static void jdbcFault(LegacyMySqlRepository dao,ModernSettlementMysqlFixture mysql,String table,boolean loseUpdateReply)throws Exception{
  var fired=new java.util.concurrent.atomic.AtomicBoolean();
  NativeLetterSqlFaultTest.set(dao,"dataSource",new HikariDataSource(){@Override public Connection getConnection()throws SQLException{
   Connection real=connection(mysql);return NativeLetterSqlFaultTest.proxy(Connection.class,(p,m,a)->{
    try{
     if(m.getName().equals("prepareStatement")&&a[0] instanceof String sql&&sql.startsWith("UPDATE "+table+" SET")){
      PreparedStatement statement=(PreparedStatement)m.invoke(real,a);var executed=new java.util.concurrent.atomic.AtomicBoolean();
      return NativeLetterSqlFaultTest.proxy(PreparedStatement.class,(pp,mm,aa)->{try{
       if(mm.getName().equals("close")&&executed.get()&&fired.compareAndSet(false,true)){statement.close();throw new SQLException("owned after real open SQL JDBC close fault");}
       Object v=mm.invoke(statement,aa);if(mm.getName().equals("executeUpdate")){executed.set(true);if(loseUpdateReply&&fired.compareAndSet(false,true))throw new SQLException("owned missing UPDATE reply after real SQL execution");}return v;
      }catch(InvocationTargetException e){throw e.getCause();}});
     }
     return m.invoke(real,a);
    }catch(InvocationTargetException e){throw e.getCause();}
   });
  }});
 }
 static void player(ModernSettlementMysqlFixture mysql,LegacyMySqlRepository dao,Path output,List<Object> checks)throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(output.resolve("player"))){
   var f=pair.f;NativeLetterAdmissionTest.ready(f.session);NativeLetterAdmissionTest.ready(pair.other);
   NativeLetterAdmissionTest.set(f.session,"inventory",NativeLetterAdmissionTest.inventory(1,0));NativeLetterAdmissionTest.set(pair.other,"inventory",NativeLetterAdmissionTest.inventory(0,0));
   NativeLetterDeleteSqlMain.seed(mysql,A,f.session.nativeLetterSelectedObjectId(),pair.other.nativeLetterSelectedObjectId(),f.session.playerName(),pair.other.playerName(),NativeLetterAdmissionTest.paper(f.session).objectId());
   AccountRepository routed=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return p;
    if(m.getName().equals("deliverLetter")){var r=dao.deliverLetter((Long)a[0],(Long)a[1],(String)a[2],(Integer)a[3],(String)a[4],(String)a[5],(String)a[6]);NativeLetterAdmissionTest.ids.accumulateAndGet(r.letter().itemObjectId()+4096,Math::max);return r;}
    if(m.getName().equals("letters"))return dao.letters((Long)a[0]);if(m.getName().equals("openLetter"))return dao.openLetter((Long)a[0],(RepositoryLetter)a[1]);if(m.getName().equals("deleteLetter"))return dao.deleteLetter((Long)a[0],(RepositoryLetter)a[1]);return m.invoke(f.accounts,a);
   });NativeLetterAdmissionTest.set(f.session,"accounts",routed);NativeLetterAdmissionTest.set(pair.other,"accounts",routed);
   NativeLetterAdmissionTest.send(f.session,pair.other);RepositoryLetter row=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);pair.drain();
   Method read=ClientSession.class.getDeclaredMethod("useSqlPhysicalLetter",RuntimeInventory.Item.class);read.setAccessible(true);
   fault(mysql,"character_letter",false);InvocationTargetException refusal=assertThrows(InvocationTargetException.class,()->read.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId())));assertInstanceOf(java.io.IOException.class,refusal.getCause());core(state(mysql,row),false);assertEquals(InventoryCatalog.LETTER_CLOSED,NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId()).definition().catalogId());assertTrue(pair.drain().isEmpty());drop(mysql);
   fault(mysql,"lito_item_origin",false);read.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId()));core(state(mysql,row),true);assertEquals(InventoryCatalog.LETTER_OPEN,NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId()).definition().catalogId());var opened=dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().get(0);var packets=pair.drain();assertEquals(1,packets.size());assertArrayEquals(Protocol.readMail(opened.itemObjectId(),InventoryCatalog.LETTER_OPEN_INVENTORY_GFX,opened.code(),opened.sender(),opened.receiver(),opened.subject(),opened.content(),opened.templateId(),opened.date()),packets.get(0));drop(mysql);
   checks.add(Map.of("case","actual private player read: body refusal stays closed/no packet; retry with origin fault succeeds core+runtime+exact content packet","poststate",state(mysql,row),"authenticated_selection",false));
   Method destroy=ClientSession.class.getDeclaredMethod("destroySqlPhysicalLetter",RuntimeInventory.Item.class);destroy.setAccessible(true);destroy.invoke(pair.other,NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId()));assertTrue(dao.letters(pair.other.nativeLetterSelectedObjectId()).letters().isEmpty());assertNull(NativeLetterAdmissionTest.inv(pair.other).item(row.itemObjectId()));var removed=pair.drain();assertEquals(1,removed.size());assertArrayEquals(Protocol.removeInventory(row.itemObjectId()),removed.get(0));
   checks.add(Map.of("case","new open failure boundary followed by retained actual private delete; no resurrection","poststate",NativeLetterDeleteSqlMain.poststate(mysql,row)));
  }
 }
 public static void main(String[] args)throws Exception{
  Path output=Path.of(args[0]).toRealPath();assertTrue(output.toString().matches("[\\x20-\\x7e]+"));assertTrue(output.getFileName().toString().startsWith("native-letter-open-sql-"));boolean baseline=args[1].equals("baseline");
  Map<String,Object> result=new LinkedHashMap<>();result.put("state","STARTING");result.put("mode",args[1]);result.put("helper_pid",ProcessHandle.current().pid());result.put("helper_start_utc",ProcessHandle.current().info().startInstant().orElseThrow().toString());result.put("GUI",0);result.put("authenticated_selection",false);
  Path artifact=Path.of(LegacyMySqlRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();String hash=java.util.HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));assertEquals(System.getProperty("lito.open.expectedSHA"),hash);result.put("actual_DAO_artifact",artifact.toString());result.put("actual_DAO_artifact_SHA",hash);NativeLetterDeleteSqlMain.write(output,result);long pid=0;int port=0;
  try(var mysql=ModernSettlementMysqlFixture.open(Path.of("C:/Program Files/MySQL/MySQL Server 5.5"))){
   pid=mysql.processId();port=mysql.port();result.put("mysql_pid",pid);result.put("mysql_port",port);result.put("mysql_start_utc",ProcessHandle.of(pid).orElseThrow().info().startInstant().orElseThrow().toString());result.put("datadir",mysql.dataDirectory().toRealPath().toString());NativeLetterDeleteSqlMain.write(output,result);NativeLetterDeleteSqlMain.schema(mysql,output);
   List<Object> engines=new ArrayList<>();try(var c=connection(mysql);var s=c.createStatement();var rows=s.executeQuery("SELECT TABLE_SCHEMA,TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA IN ('"+A+"','"+NativeLetterDeleteSqlMain.B+"') ORDER BY TABLE_SCHEMA,TABLE_NAME")){while(rows.next()){assertEquals("MyISAM",rows.getString(3));engines.add(Map.of("schema",rows.getString(1),"table",rows.getString(2),"engine",rows.getString(3)));}}assertEquals(14,engines.size());result.put("actual_table_engines",engines);
   List<Object> checks=new ArrayList<>();try(var dao=NativeLetterDeleteSqlMain.repository(mysql,A);var foreign=NativeLetterDeleteSqlMain.repository(mysql,NativeLetterDeleteSqlMain.B)){
    for(boolean metadata:List.of(false,true)){var row=NativeLetterDeleteSqlMain.letter(mysql,dao,metadata);var opened=dao.openLetter(20000,row);assertEquals(RepositoryLetterMutationStatus.SUCCESS,opened.status());core(state(mysql,row),true);assertEquals(List.of(opened.letter()),dao.letters(20000).letters());assertEquals(RepositoryLetterMutationStatus.ALREADY_OPEN,dao.openLetter(20000,row).status());checks.add(Map.of("case","normal+repeat open metadata="+metadata,"poststate",state(mysql,row)));}
    var gated=NativeLetterDeleteSqlMain.letter(mysql,dao,true);var before=state(mysql,gated);assertEquals(RepositoryLetterMutationStatus.NOT_FOUND,dao.openLetter(10000,gated).status());NativeLetterDeleteSqlMain.seed(mysql,NativeLetterDeleteSqlMain.B,10000,20000,"Sender","Receiver",10001);assertEquals(RepositoryLetterMutationStatus.NOT_FOUND,foreign.openLetter(20000,gated).status());
    try(var c=NativeLetterDeleteSqlMain.connection(mysql,NativeLetterDeleteSqlMain.B);var s=c.createStatement()){s.executeUpdate("UPDATE server_info SET maxid="+(gated.itemObjectId()-1)+" WHERE id=1");}
    var foreignRow=foreign.deliverLetter(10000,10001,"Receiver",1,"26/10/04","foreign","different body");assertEquals(gated.itemObjectId(),foreignRow.letter().itemObjectId());assertEquals(RepositoryLetterMutationStatus.CONFLICT,foreign.openLetter(20000,gated).status());assertEquals(List.of(foreignRow.letter()),foreign.letters(20000).letters());NativeLetterSqlFaultTest.set(dao,"readOnly",true);assertEquals(RepositoryLetterMutationStatus.READ_ONLY,dao.openLetter(20000,gated).status());NativeLetterSqlFaultTest.set(dao,"readOnly",false);assertEquals(before,state(mysql,gated));checks.add("wrong physical owner+foreign absent/conflicting same-ID source+read-only preserve closed rows");
    for(String table:List.of("character_items","character_letter","lito_item_origin")){
     var row=NativeLetterDeleteSqlMain.letter(mysql,dao,true);byte[] subject=NativeLetterDeleteSqlMain.fieldBytes(mysql,row.itemObjectId(),"subject"),content=NativeLetterDeleteSqlMain.fieldBytes(mysql,row.itemObjectId(),"content");fault(mysql,table,false);RepositoryLetterMutation success=null;SQLException failure=null;try{success=dao.openLetter(20000,row);}catch(SQLException e){failure=e;}var post=state(mysql,row);Map<String,Object> entry=new LinkedHashMap<>();entry.put("case","real BEFORE UPDATE SIGNAL "+table);entry.put("poststate",post);entry.put("exception",failure!=null);entry.put("status",success==null?null:success.status().name());
     if(baseline){assertNotNull(failure);if(table.equals("character_items"))core(post,false);else if(table.equals("character_letter")){assertEquals(0,post.get("body_opened"));assertEquals(InventoryCatalog.LETTER_OPEN,post.get("backing_catalog"));assertThrows(SQLException.class,()->dao.letters(20000));entry.put("authoritative_reread_backing_mismatch",true);}else core(post,true);}
     else if(table.equals("lito_item_origin")){assertNull(failure);assertEquals(RepositoryLetterMutationStatus.SUCCESS,success.status());core(post,true);assertEquals(List.of(success.letter()),dao.letters(20000).letters());assertEquals(InventoryCatalog.LETTER_CLOSED,post.get("origin_catalog"));}
     else{assertNotNull(failure);core(post,false);assertEquals(List.of(row),dao.letters(20000).letters());assertArrayEquals(subject,NativeLetterDeleteSqlMain.fieldBytes(mysql,row.itemObjectId(),"subject"));assertArrayEquals(content,NativeLetterDeleteSqlMain.fieldBytes(mysql,row.itemObjectId(),"content"));entry.put("raw_subject_content_bytes_preserved",true);}
     drop(mysql);if(!baseline&&failure!=null){assertEquals(RepositoryLetterMutationStatus.SUCCESS,dao.openLetter(20000,row).status());core(state(mysql,row),true);entry.put("retry_after_fault_removed","SUCCESS");}checks.add(entry);
    }
    if(!baseline){
     var row=NativeLetterDeleteSqlMain.letter(mysql,dao,true);fault(mysql,"character_letter",false);fault(mysql,"character_items",true);var failed=assertThrows(SQLException.class,()->dao.openLetter(20000,row));assertTrue(failed.getSuppressed().length>0);var post=state(mysql,row);assertEquals(0,post.get("body_opened"));assertEquals(InventoryCatalog.LETTER_OPEN,post.get("backing_catalog"));assertThrows(SQLException.class,()->dao.letters(20000));checks.add(Map.of("case","body UPDATE and backing compensation UPDATE both fail; NOT a rollback","poststate",post,"suppressed_compensation_failure",true));drop(mysql);
     for(String table:List.of("character_items","character_letter")){
      var closing=NativeLetterDeleteSqlMain.letter(mysql,dao,true);closeFault(dao,mysql,table);if(table.equals("character_items")){assertThrows(SQLException.class,()->dao.openLetter(20000,closing));core(state(mysql,closing),false);assertEquals(List.of(closing),dao.letters(20000).letters());}else{var opened=dao.openLetter(20000,closing);assertEquals(RepositoryLetterMutationStatus.SUCCESS,opened.status());core(state(mysql,closing),true);assertEquals(List.of(opened.letter()),dao.letters(20000).letters());}checks.add(Map.of("case","owned JDBC close fault after real open UPDATE "+table,"poststate",state(mysql,closing),"JDBC_fault_wrapper",true));
     }
     var ambiguous=NativeLetterDeleteSqlMain.letter(mysql,dao,true);jdbcFault(dao,mysql,"character_letter",true);var replyFailure=assertThrows(SQLException.class,()->dao.openLetter(20000,ambiguous));assertTrue(replyFailure.getSuppressed().length>0);core(state(mysql,ambiguous),true);assertTrue(dao.letters(20000).letters().get(0).opened());checks.add(Map.of("case","owned missing body UPDATE reply: compensation guard preserves already-open body/backing; caller still fails","poststate",state(mysql,ambiguous),"JDBC_fault_wrapper",true,"success_or_rollback_claim",false));
     player(mysql,dao,output,checks);
    }
   }
   result.put("checks",checks);result.put("real_SQL",true);result.put("transaction_rollback_claim",false);result.put("state",baseline?"PASSED_BASELINE_REAL_DDL_OPEN_PARTIAL_STATE":"PASSED_CANDIDATE_REAL_DDL_OPEN_BOUNDARY");NativeLetterDeleteSqlMain.write(output,result);mysql.successful();
  }finally{result.put("mysql_process_retired",pid!=0&&ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true));if(port!=0)try(var socket=new java.net.ServerSocket(port,1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("mysql_listener_released",true);}catch(Exception e){result.put("mysql_listener_released",false);}NativeLetterDeleteSqlMain.write(output,result);}
  System.out.println(NativeLetterDeleteSqlMain.json(result));
 }
}
