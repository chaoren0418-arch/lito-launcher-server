package local.lito381.server;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** External real MySQL lock -> actual recipient C_QUIT -> committed deferred receipt. */
public final class NativeLetterDeferredWireMain extends NativeLetterRestartWireMain {
 NativeLetterDeferredWireMain(Path root,Path work){super(root,work);}
 @Override Path profile(ModernSettlementMysqlFixture mysql,int port)throws Exception{
  Path p=super.profile(mysql,port);Path jdbc=root.resolve("native-database.properties");
  // writeNativeJdbcSettings returns a fixture-owned file; locate by its real
  // configured path, never load production credentials or change SQL semantics.
  Properties conf=new Properties();try(var in=Files.newInputStream(p)){conf.load(in);}jdbc=Path.of(conf.getProperty("database.connection.properties"));
  Properties db=new Properties();try(var in=Files.newInputStream(jdbc)){db.load(in);}String suffix=db.getProperty("URL3_LOGIN");require(suffix.contains("socketTimeout=5000"),"owned fixture timeout expected");db.setProperty("URL3_LOGIN",suffix.replace("socketTimeout=5000","socketTimeout=15000"));try(var out=Files.newOutputStream(jdbc)){db.store(out,"Owned deferred SQL-lock timing only");}evidence.put("owned_JDBC_socketTimeout_ms",15000);evidence.put("product_SQL_logic_or_context_hook_changed",false);return p;
 }
 int occurrences(String value,String needle){int n=0,at=0;while((at=value.indexOf(needle,at))>=0){n++;at+=needle.length();}return n;}
 String serverLog()throws Exception{return Files.readString(phase.resolve("server.stdout.log"));}
 void waitLog(String needle,int before,int seconds)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);while(occurrences(serverLog(),needle)<=before){require(System.nanoTime()<deadline,"actual producer log "+needle);Thread.sleep(30);}}
 void awaitActualBlockedDao(ModernSettlementMysqlFixture mysql)throws Exception{
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(true){List<Object> blocked=rows(mysql,A,"SELECT ID,DB,COMMAND,TIME,STATE,INFO FROM information_schema.PROCESSLIST WHERE DB='nw_a' AND INFO LIKE 'LOCK TABLES server_info WRITE,%' AND INFO LIKE '%character_letter WRITE%' ORDER BY ID");
   if(!blocked.isEmpty()){Map<?,?> thread=(Map<?,?>)blocked.get(0);String state=String.valueOf(thread.get("STATE"));require(state.toLowerCase(Locale.ROOT).contains("lock"),"actual DAO wait state");evidence.put("actual_blocked_deliverLetter_SQL_threads",blocked);checks.add(Map.of("case","real production deliverLetter LOCK TABLES observed blocked by fixture server_info WRITE lock","SQL_threads",blocked));save();return;}require(System.nanoTime()<deadline,"actual sender DAO lock wait");Thread.sleep(25);
  }
 }
 void paper(Peer sender,long quantity){Item p=sender.inventory.get(PAPER);require(p!=null&&p.icon()==464&&p.quantity()==quantity,"actual source-selected paper quantity "+quantity);}
 void checkCoreCount(ModernSettlementMysqlFixture mysql,String db,long id,long bodies,long paper)throws Exception{
  require(scalar(mysql,db,"SELECT COUNT(*) FROM character_letter WHERE item_object_id="+id)==bodies,"source-owned body row count");require(scalar(mysql,db,"SELECT count FROM character_items WHERE id="+PAPER)==paper,"source SQL paper quantity exact no duplicate debit");
 }
 void deferred(ModernSettlementMysqlFixture mysql)throws Exception{
  start();snapshot(mysql,"before-actual-wire-deferred-send");RepositoryLetter sun;
  try(Peer s=select("DeferredSunSender",port,"WireSend",SUN,"SenderA");Peer r=select("RevokedSunRecipient",port,"WireRecv",SUN,"RecvA")){
   battle(s,r,"DeferredSun");long actualPaper=s.item(464);int guardBefore=occurrences(serverLog(),"MySQL character disconnect persistence failed: IOException: SQL-letter source inventory reconcile pending");int deliveredBefore=occurrences(serverLog(),"SQL physical-letter durable delivered object=");
   try(var hold=connection(mysql,A);var lock=hold.createStatement()){
    lock.execute("LOCK TABLES server_info WRITE");long acquired=System.nanoTime();try{
     stage="real-DAO-block-recipient-C_QUIT";save();s.send(new Protocol.Writer().c(Protocol.C_USE_ITEM).d(actualPaper).h(7).s("RecvA").S("DeferredSun").S("Sun committed after recipient revocation").bytes());awaitActualBlockedDao(mysql);
     r.close();waitLog("MySQL character disconnect persistence failed: IOException: SQL-letter source inventory reconcile pending",guardBefore,5);
     require(r.socket.isClosed(),"old actual recipient socket exited before SQL released");require(r.inventory.values().stream().noneMatch(i->i.icon()==465||i.icon()==466),"old socket has no deferred arrival inventory");evidence.put("old_recipient_wire_frames_at_revocation",r.received.size());evidence.put("actual_disconnect_guard_seen_while_SQL_blocked",true);checks.add(Map.of("case","actual C_QUIT revoked selected recipient while real deliverLetter blocked; stale disconnect inventory SQL refused by pending guard","hook",false,"old_source",SUN));
    }finally{lock.execute("UNLOCK TABLES");evidence.put("external_SQL_lock_hold_ms",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-acquired));}
   }
   stage="SQL-committed-after-real-recipient-revocation";save();waitLog("SQL physical-letter durable delivered object=",deliveredBefore,10);sun=letter(profile,SUN,11002);require(!sun.opened()&&sun.subject().equals("DeferredSun")&&sun.content().equals("Sun committed after recipient revocation"),"exact deferred SQL body");checkCoreCount(mysql,A,sun.itemObjectId(),1,2);
   s.close();boolean oldSenderCount2=s.received.stream().anyMatch(packet->Arrays.equals(packet,Protocol.changeInventoryCount(actualPaper,2)));evidence.put("old_sender_count2_packet_observed",oldSenderCount2);evidence.put("sender_paper_runtime_recovery_qualified_at_genuine_fresh_selection",true);
   snapshot(mysql,"after-revoked-recipient-durable-SQL-commit");checks.add(Map.of("case","durable success with recipient context gone and no old arrival; selected-source pending receipt inferred from retained production commit/current-capability branches","physical_letter_id",sun.itemObjectId(),"direct_private_Hub_map_inspection",false,"SQL_paper_qty",2));
  }
  RepositoryLetter moon;
  try(Peer s=select("ForeignMoonSender",port,"WireSend",MOON,"SenderB");Peer r=select("ForeignMoonRecipient",port,"WireRecv",MOON,"RecvB")){
   require(r.inventory.values().stream().noneMatch(i->i.icon()==465||i.icon()==466),"foreign source did not install pending Sun receipt");paper(s,3);battle(s,r,"DeferredMoon");moon=sendLetter(mysql,s,r,MOON,B,"RecvB","LiveMoon","Moon same physical ID stays private",2);require(moon.itemObjectId()==sun.itemObjectId(),"real wire same physical letterID in two SQL sources");read(r,moon);checkCoreCount(mysql,A,sun.itemObjectId(),1,2);checkCoreCount(mysql,B,moon.itemObjectId(),1,2);checks.add(Map.of("case","genuine other-source selection and own same-ID delivery/read cannot consume or overwrite revoked-source receipt","physical_letter_id",sun.itemObjectId(),"foreign_exact_content",true));snapshot(mysql,"foreign-source-same-ID-while-Sun-pending");
  }
  try(Peer s=select("RebuiltSunSender",port,"WireSend",SUN,"SenderA");Peer r=select("RebuiltSunRecipient",port,"WireRecv",SUN,"RecvA")){
   paper(s,2);assertItem(r,sun.itemObjectId(),465);require(r.inventory.values().stream().filter(i->i.id()==sun.itemObjectId()).count()==1,"one reconstructed physical letter");battle(s,r,"DeferredSunRebuilt");read(r,sun);read(r,sun);require(letter(profile,SUN,sun.itemObjectId()).opened(),"reconstructed letter actual open persisted");checkCoreCount(mysql,A,sun.itemObjectId(),1,2);deleted(mysql,r,A,sun.itemObjectId());checkCoreCount(mysql,B,moon.itemObjectId(),1,2);require(letter(profile,MOON,moon.itemObjectId()).opened()&&letter(profile,MOON,moon.itemObjectId()).subject().equals("LiveMoon"),"foreign same-ID opened letter unchanged by rebuilt Sun delete");checks.add(Map.of("case","genuine revoked-source reselect reconstructs one closed SQL letter, exact open/re-read/delete and stale private inventory does not restore old paper/letter","paper_qty",2,"same_ID_foreign_body_retained",true));snapshot(mysql,"after-rebuilt-Sun-actual-open-repeat-delete");
  }
  try(Peer s=select("AfterDeleteSunSender",port,"WireSend",SUN,"SenderA");Peer r=select("AfterDeleteSunRecipient",port,"WireRecv",SUN,"RecvA")){
   paper(s,2);absent(r,sun.itemObjectId());require(r.inventory.values().stream().noneMatch(i->i.icon()==465||i.icon()==466),"deleted deferred Sun letter cannot resurrect on genuine reselect");checkCoreCount(mysql,A,sun.itemObjectId(),0,2);checks.add(Map.of("case","genuine reconnect after deferred letter delete has no resurrection or duplicate paper debit","source",SUN,"SQL_paper_qty",2));
  }
  try(Peer s=select("FinalMoonSender",port,"WireSend",MOON,"SenderB");Peer r=select("FinalMoonRecipient",port,"WireRecv",MOON,"RecvB")){
   paper(s,2);assertItem(r,moon.itemObjectId(),466);battle(s,r,"DeferredMoonFinal");read(r,moon);deleted(mysql,r,B,moon.itemObjectId());checkCoreCount(mysql,A,sun.itemObjectId(),0,2);checkCoreCount(mysql,B,moon.itemObjectId(),0,2);
  }
  try(Peer r=select("AfterDeleteMoonRecipient",port,"WireRecv",MOON,"RecvB")){absent(r,moon.itemObjectId());require(r.inventory.values().stream().noneMatch(i->i.icon()==465||i.icon()==466),"Moon no deleted same-ID resurrection");}
  snapshot(mysql,"final-both-source-core-absent-paper2");stop();snapshot(mysql,"after-normal-owned-server-retirement");
  evidence.put("normal_restart_reexecuted",false);evidence.put("Java_generations",1);evidence.put("one_time_schema_and_seed",true);evidence.put("external_real_SQL_lock_timing_fixture",true);evidence.put("controlled_product_context_or_SQL_hook",false);evidence.put("pending_recipient_receipt_inferred_from_retained_real_flow",true);evidence.put("private_Hub_pending_map_directly_observed",false);evidence.put("physical_letter_id_both_sources",sun.itemObjectId());evidence.put("old_disconnect_inventory_guard_refusal_while_inflight",true);evidence.put("same_ID_source_separation_and_deleted_reconnect_absence",true);evidence.put("exactly_one_paper_debit_per_source",true);save();
 }
 @Override void run()throws Exception{
  require(root.toRealPath().toString().matches("[\\x20-\\x7e]+")&&root.getFileName().toString().matches("nw-[a-f0-9]{32}"),"unique ASCII owned root");require(hash(jar).equals("AD368F49CF47E37A76A954AAFAAE1962AB2AB761057AAD4EE3145F7BBFD71219"),"unchanged cumulative candidate");require(Path.of(AccountRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar.toRealPath()),"actual DAO codeSource");evidence.put("candidate_SHA",hash(jar));evidence.put("helper",process(ProcessHandle.current()));evidence.put("GUI",0);evidence.put("authenticated_selection",false);evidence.put("reflection_selection",false);evidence.put("state","STARTING");save();
  try(var mysql=ModernSettlementMysqlFixture.open(Path.of("C:/Program Files/MySQL/MySQL Server 5.5"))){
   sqlPid=mysql.processId();evidence.put("mysql",process(ProcessHandle.of(sqlPid).orElseThrow()));evidence.put("mysql_port",mysql.port());evidence.put("datadir",mysql.dataDirectory().toRealPath().toString());datadirKey=nativeDatadirIdentity(mysql);evidence.put("datadir_file_key",datadirKey);evidence.put("datadir_identity_kind","Win32 volume serial + 64-bit directory file ID + creation FILETIME");save();schema(mysql);
   try(var one=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));var two=new ServerSocket(one.getLocalPort(),1,InetAddress.getByName("127.0.0.2"))){port=one.getLocalPort();}profile=profile(mysql,port);seed(mysql,profile);evidence.put("native_port",port);evidence.put("profile",profile.toString());
   try{deferred(mysql);evidence.put("state","PASSED_REAL_SQL_LOCK_RECIPIENT_REVOCATION_ENCRYPTED_RESELECT_NO_OVERWRITE");save();mysql.successful();}finally{if(console!=null&&console.isAlive()){try{stop();}catch(Throwable cleanup){evidence.put("cleanup_failure",cleanup.toString());save();}}}
  }catch(Throwable failure){evidence.put("state","FAILED");evidence.put("failure",failure.toString());save();throw failure;}finally{evidence.put("mysql_retired",ProcessHandle.of(sqlPid).map(p->!p.isAlive()).orElse(true));evidence.put("all_server_generations_retired",generations.stream().allMatch(g->{long pid=((Number)((Map<?,?>)((Map<?,?>)g).get("server")).get("pid")).longValue();return ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true);}));save();}
 }
 public static void main(String[] args)throws Exception{new NativeLetterDeferredWireMain(Path.of(args[0]),Path.of(args[1])).run();}
}
