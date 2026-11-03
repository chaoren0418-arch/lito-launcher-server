package local.lito381.server;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** New process generations, same live MySQL/datadir, genuine wire selection. */
public final class NativeLetterRestartWireMain extends NativeLetterAuthenticatedWireMain {
 final List<Object> generations=new ArrayList<>(),snapshots=new ArrayList<>();
 Process console;Path phase,profile;int port,generation;String datadirKey;long sqlPid;
 NativeLetterRestartWireMain(Path root,Path work){super(root,work);}
 void waitFile(Path file,int seconds,String why)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);while(!Files.exists(file)){require(console==null||console.isAlive(),"console launcher exited: "+why);require(System.nanoTime()<deadline,why);Thread.sleep(50);}}
 void start()throws Exception{
  generation++;phase=root.resolve("generation-"+generation);Files.createDirectory(phase);evidence.put("generation",generation);stage="start-generation-"+generation;save();
  Path python=Path.of("C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe");
  console=new ProcessBuilder(python.toString(),"-B","-X","utf8",work.resolve("tools/NativeLetterOwnedConsoleServer.py").toString(),phase.toString(),jar.toString(),profile.toString()).directory(root.toFile()).redirectOutput(phase.resolve("launcher.stdout.log").toFile()).redirectError(phase.resolve("launcher.stderr.log").toFile()).start();
  evidence.put("launcher",process(console.toHandle()));waitFile(phase.resolve("server.pid"),10,"actual owned Java PID");long pid=Long.parseLong(Files.readString(phase.resolve("server.pid")).trim());evidence.put("server",process(ProcessHandle.of(pid).orElseThrow()));save();
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);while(true){require(console.isAlive()&&ProcessHandle.of(pid).orElseThrow().isAlive(),"actual candidate startup");if(Files.exists(phase.resolve("server.stdout.log"))&&Files.readString(phase.resolve("server.stdout.log")).contains("[LiTo] ready"))break;require(System.nanoTime()<deadline,"actual candidate readiness");Thread.sleep(100);}
  evidence.put("state","READY_NORMAL_RESTART_WIRE");save();waitFile(phase.resolve("owned-live.ok"),20,"four owned process capture");
  generations.add(Map.of("generation",generation,"server",evidence.get("server"),"launcher",evidence.get("launcher"),"same_profile",profile.toString(),"same_candidate_SHA",evidence.get("candidate_SHA")));evidence.put("generations",generations);save();
 }
 void stop()throws Exception{
  if(console==null)return;stage="normal-stop-generation-"+generation;evidence.put("state","STOPPING_NORMAL_RESTART_WIRE");save();Files.writeString(phase.resolve("normal-stop.request"),"All encrypted peers closed; observe ClientSession quiescence then exclusive console Ctrl+C.",StandardCharsets.UTF_8);
  require(console.waitFor(40,TimeUnit.SECONDS),"normal console launcher stop deadline");require(console.exitValue()==0&&Files.exists(phase.resolve("normal-stop-result.json")),"normal JVM stop only, no forced fallback qualification");
  evidence.put("state","WAIT_RESTART_GENERATION_RETIRED");save();console=null;waitFile(phase.resolve("owned-generation-retired.ok"),20,"generation server/launcher retirement and persistent DB identity");checks.add(Map.of("case","normal Java Ctrl+C shutdown after observed client quiescence","generation",generation,"same_MySQL_PID",sqlPid,"datadir_file_key",datadirKey,"crash_qualified",false));save();
 }
 List<Object> rows(ModernSettlementMysqlFixture mysql,String db,String query)throws Exception{
  try(var c=connection(mysql,db);var s=c.createStatement();var r=s.executeQuery(query)){List<Object> out=new ArrayList<>();var m=r.getMetaData();while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=m.getColumnCount();i++)row.put(m.getColumnLabel(i),r.getString(i));out.add(row);}return out;}
 }
 String nativeDatadirIdentity(ModernSettlementMysqlFixture mysql)throws Exception{
  var q=new ProcessBuilder("C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe","-B","-X","utf8",work.resolve("tools/NativeLetterOwnedConsoleServer.py").toString(),"--datadir-id",mysql.dataDirectory().toRealPath().toString()).start();String identity=new String(q.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();String error=new String(q.getErrorStream().readAllBytes(),StandardCharsets.UTF_8);require(q.waitFor(5,TimeUnit.SECONDS)&&q.exitValue()==0&&identity.matches("[A-F0-9]{8}:[A-F0-9]{16}:[0-9]+"),"native existing datadir identity "+error);return identity;
 }
 Map<String,Object> snapshot(ModernSettlementMysqlFixture mysql,String label)throws Exception{
  require(mysql.processId()==sqlPid&&ProcessHandle.of(sqlPid).orElseThrow().isAlive(),"same MySQL remains live across generations");String key=nativeDatadirIdentity(mysql);require(key.equals(datadirKey),"same existing native volume/file-ID/creation-time datadir identity, never recreated");
  Map<String,Object> core=new LinkedHashMap<>();for(String db:List.of(A,B)){Map<String,Object> source=new LinkedHashMap<>();source.put("body",rows(mysql,db,"SELECT item_object_id,code,sender,receiver,date,template_id,HEX(subject) AS subject_bytes,HEX(content) AS content_bytes,isCheck FROM character_letter ORDER BY item_object_id"));source.put("items",rows(mysql,db,"SELECT id,item_id,char_id,count,is_id FROM character_items WHERE id>=11001 AND id<=11003 ORDER BY id"));source.put("origin",rows(mysql,db,"SELECT item_obj_id,char_obj_id,catalog_id,tournament_reward,created_at FROM lito_item_origin WHERE item_obj_id>=11001 AND item_obj_id<=11003 ORDER BY item_obj_id"));source.put("allocator",rows(mysql,db,"SELECT id,minid,maxid FROM server_info ORDER BY id"));core.put(db,source);}
  Path folder=root.resolve("snapshots");Files.createDirectories(folder);Map<String,Object> record=new LinkedHashMap<>();record.put("label",label);record.put("generation",generation);record.put("same_mysql_pid",sqlPid);record.put("datadir",mysql.dataDirectory().toRealPath().toString());record.put("datadir_file_key",key);record.put("core",core);Path state=root.resolve("native.state");record.put("sidecar_present",Files.exists(state));if(Files.exists(state)){record.put("sidecar_SHA",hash(state));record.put("sidecar_bytes",Files.size(state));}Files.writeString(folder.resolve(label+".json"),json(record),StandardCharsets.UTF_8);snapshots.add(record);evidence.put("SQL_snapshots",snapshots);save();return core;
 }
 RepositoryLetter letter(Path profile,String source,long id)throws Exception{try(var routes=AccountRepository.open(ServerConfig.load(profile))){var loaded=routes.repositoryForClassicSource(source).letters(RECEIVER);require(loaded.authoritative(),"authoritative source letters");return loaded.letters().stream().filter(l->l.itemObjectId()==id).findFirst().orElseThrow(()->new AssertionError("actual letter missing source="+source+" id="+id));}}
 RepositoryLetter sendLetter(ModernSettlementMysqlFixture mysql,Peer sender,Peer recipient,String source,String db,String name,String subject,String body,int expectedPaper)throws Exception{
  long paper=sender.item(464);sender.send(new Protocol.Writer().c(Protocol.C_USE_ITEM).d(paper).h(7).s(name).S(subject).S(body).bytes());var p=new Protocol.Reader(recipient.await(Protocol.S_ADD_INVENTORY));p.c();long id=p.d();require(Arrays.equals(recipient.await(Protocol.S_MESSAGE_CODE),Protocol.messageCode(428)),"wire receipt428");var result=letter(profile,source,id);require(!result.opened()&&result.subject().equals(subject)&&result.content().equals(body),"wire-created durable body exact selected source");require(scalar(mysql,db,"SELECT count FROM character_items WHERE id="+PAPER)==expectedPaper,"durable SQL paper decrement");checks.add(Map.of("case","actual wire send for restart fixture","generation",generation,"source",source,"letter_id",id,"paper_qty_after",expectedPaper,"subject",subject));return result;
 }
 void assertPaper(Peer p){Item item=p.inventory.get(PAPER);require(item!=null&&item.icon()==464&&item.quantity()==1,"fresh selected source packet paper quantity1");}
 @Override void battle(Peer sender,Peer recipient,String name)throws Exception{
  super.battle(sender,recipient,name);
  // START precedes the actual ROOM -> BATTLE assignment in the retained
  // producer. Observe real post-phase world vitals before sending item input.
  for(Peer peer:List.of(sender,recipient)){var hp=new Protocol.Reader(peer.await(Protocol.S_HIT_POINT));hp.c();require(hp.h()>0&&hp.h()==200&&hp.remaining()==0,"actual post-START battle vitality bootstrap");}
  checks.add(Map.of("case","real post-START battle HP packet before item input","generation",generation,"private_phase_override",false));save();
 }
 void assertItem(Peer p,long id,int icon){Item item=p.inventory.get(id);require(item!=null&&item.icon()==icon&&item.quantity()==1,"restarted actual inventory physical ID/catalog state id="+id+" icon="+icon);}
 void absent(Peer p,long id){require(!p.inventory.containsKey(id),"deleted actual physical ID absent after process restart "+id);}
 void deleted(ModernSettlementMysqlFixture mysql,Peer p,String db,long id)throws Exception{p.destroy(id);require(Arrays.equals(p.await(Protocol.S_REMOVE_INVENTORY),Protocol.removeInventory(id)),"actual deletion inventory packet");for(String table:List.of("character_letter","character_items","lito_item_origin")){String col=table.equals("character_letter")?"item_object_id":table.equals("character_items")?"id":"item_obj_id";require(scalar(mysql,db,"SELECT COUNT(*) FROM "+table+" WHERE "+col+"="+id)==0,"actual selected SQL delete core/origin");}checks.add(Map.of("case","actual encrypted delete for cross-process absence","generation",generation,"source_database",db,"physical_letter_id",id));}
 void runRestart(ModernSettlementMysqlFixture mysql)throws Exception{
  RepositoryLetter sunUnread,sunOpened,moonOpened,moonDeleted;
  start();snapshot(mysql,"G1-after-one-time-seed-before-wire");
  try(Peer s=select("G1SunSender",port,"WireSend",SUN,"SenderA");Peer r=select("G1SunRecipient",port,"WireRecv",SUN,"RecvA")){
   battle(s,r,"RestartSun");sunUnread=sendLetter(mysql,s,r,SUN,A,"RecvA","SunUnread","Sun first source unread",2);sunOpened=sendLetter(mysql,s,r,SUN,A,"RecvA","SunOpened","Sun second source opened",1);read(r,sunOpened);require(letter(profile,SUN,sunOpened.itemObjectId()).opened(),"Sun second durable opened");
  }
  try(Peer s=select("G1MoonSender",port,"WireSend",MOON,"SenderB");Peer r=select("G1MoonRecipient",port,"WireRecv",MOON,"RecvB")){
   battle(s,r,"RestartMoon");moonOpened=sendLetter(mysql,s,r,MOON,B,"RecvB","MoonOpened","Moon first source opened",2);moonDeleted=sendLetter(mysql,s,r,MOON,B,"RecvB","MoonDeleted","Moon second source deleted",1);read(r,moonOpened);deleted(mysql,r,B,moonDeleted.itemObjectId());
  }
  require(sunUnread.itemObjectId()==moonOpened.itemObjectId()&&sunOpened.itemObjectId()==moonDeleted.itemObjectId(),"two physical letter IDs same across sources with different durable states");
  Map<String,Object> before1=snapshot(mysql,"G1-before-normal-stop");stop();require(before1.equals(snapshot(mysql,"G1-after-normal-stop-same-live-datadir")),"normal JVM stop did not change letter/paper core");
  start();require(before1.equals(snapshot(mysql,"G2-after-actual-new-JVM-start-before-login")),"restart uses exact existing SQL state, no reseed");
  try(Peer s=select("G2SunSender",port,"WireSend",SUN,"SenderA");Peer r=select("G2SunRecipient",port,"WireRecv",SUN,"RecvA")){
   assertPaper(s);assertItem(r,sunUnread.itemObjectId(),465);assertItem(r,sunOpened.itemObjectId(),466);require(!letter(profile,SUN,sunUnread.itemObjectId()).opened()&&letter(profile,SUN,sunOpened.itemObjectId()).opened(),"unread/opened SQL recovered");battle(s,r,"RestartSunRead");read(r,sunUnread);read(r,sunOpened);deleted(mysql,r,A,sunOpened.itemObjectId());checks.add(Map.of("case","new JVM genuinely selected unread+opened source and reads exact respective bodies","generation",generation,"source",SUN,"paper_qty",1));
  }
  try(Peer s=select("G2MoonSender",port,"WireSend",MOON,"SenderB");Peer r=select("G2MoonRecipient",port,"WireRecv",MOON,"RecvB")){
   assertPaper(s);assertItem(r,moonOpened.itemObjectId(),466);absent(r,moonDeleted.itemObjectId());battle(s,r,"RestartMoonRead");read(r,moonOpened);deleted(mysql,r,B,moonOpened.itemObjectId());checks.add(Map.of("case","new JVM source-qualified opened letter+same-ID deleted absence","generation",generation,"source",MOON,"paper_qty",1));
  }
  Map<String,Object> before2=snapshot(mysql,"G2-before-normal-stop");stop();require(before2.equals(snapshot(mysql,"G2-after-normal-stop-same-live-datadir")),"second normal stop preserved core");
  start();require(before2.equals(snapshot(mysql,"G3-after-actual-new-JVM-start-before-login")),"second restart existing SQL exact");
  try(Peer s=select("G3SunSender",port,"WireSend",SUN,"SenderA");Peer r=select("G3SunRecipient",port,"WireRecv",SUN,"RecvA")){
   assertPaper(s);assertItem(r,sunUnread.itemObjectId(),466);absent(r,sunOpened.itemObjectId());battle(s,r,"RestartSunAgain");read(r,sunUnread);checks.add(Map.of("case","previously unread becomes durable opened across next JVM while deleted same-source ID stays absent","generation",generation,"source",SUN));
  }
  try(Peer s=select("G3MoonSender",port,"WireSend",MOON,"SenderB");Peer r=select("G3MoonRecipient",port,"WireRecv",MOON,"RecvB")){
   assertPaper(s);absent(r,moonOpened.itemObjectId());absent(r,moonDeleted.itemObjectId());require(r.inventory.values().stream().noneMatch(i->i.icon()==465||i.icon()==466),"both Moon deleted letters remain absent across second process restart");checks.add(Map.of("case","third JVM both deleted foreign-source same physical IDs stay absent","generation",generation,"source",MOON));
  }
  Map<String,Object> before3=snapshot(mysql,"G3-before-final-normal-stop");stop();require(before3.equals(snapshot(mysql,"G3-after-final-normal-stop")),"final normal stop preserved core");
  evidence.put("physical_letter_ids",List.of(sunUnread.itemObjectId(),sunOpened.itemObjectId()));evidence.put("one_time_schema_and_seed",true);evidence.put("Java_generations",3);evidence.put("same_MySQL_process_and_datadir",true);evidence.put("normal_restart_only",true);evidence.put("process_local_pending_receipts_or_debts_injected",false);evidence.put("crash_or_ambiguous_SQL_qualified",false);save();
 }
 @Override void run()throws Exception{
  require(root.toRealPath().toString().matches("[\\x20-\\x7e]+")&&root.getFileName().toString().matches("nw-[a-f0-9]{32}"),"unique ASCII owned root");String sha=hash(jar);require(sha.equals("AD368F49CF47E37A76A954AAFAAE1962AB2AB761057AAD4EE3145F7BBFD71219"),"fixed cumulative candidate");require(Path.of(AccountRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar.toRealPath()),"actual DAO codeSource");evidence.put("candidate_SHA",sha);evidence.put("helper",process(ProcessHandle.current()));evidence.put("GUI",0);evidence.put("authenticated_selection",false);evidence.put("reflection_selection",false);evidence.put("state","STARTING");save();
  try(var mysql=ModernSettlementMysqlFixture.open(Path.of("C:/Program Files/MySQL/MySQL Server 5.5"))){
   sqlPid=mysql.processId();evidence.put("mysql",process(ProcessHandle.of(sqlPid).orElseThrow()));evidence.put("mysql_port",mysql.port());evidence.put("datadir",mysql.dataDirectory().toRealPath().toString());datadirKey=nativeDatadirIdentity(mysql);evidence.put("datadir_file_key",datadirKey);evidence.put("datadir_identity_kind","Win32 volume serial + 64-bit directory file ID + creation FILETIME");save();schema(mysql);
   try(var one=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));var two=new ServerSocket(one.getLocalPort(),1,InetAddress.getByName("127.0.0.2"))){port=one.getLocalPort();}profile=profile(mysql,port);seed(mysql,profile);evidence.put("native_port",port);evidence.put("profile",profile.toString());
   try{runRestart(mysql);evidence.put("state","PASSED_NORMAL_JAVA_RESTART_ENCRYPTED_LETTER_SQL");save();mysql.successful();}finally{if(console!=null&&console.isAlive()){try{stop();}catch(Throwable cleanup){evidence.put("cleanup_failure",cleanup.toString());save();}}}
  }catch(Throwable failure){evidence.put("state","FAILED");evidence.put("failure",failure.toString());save();throw failure;}finally{evidence.put("mysql_retired",ProcessHandle.of(sqlPid).map(p->!p.isAlive()).orElse(true));evidence.put("all_server_generations_retired",generations.stream().allMatch(g->{long pid=((Number)((Map<?,?>)((Map<?,?>)g).get("server")).get("pid")).longValue();return ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true);}));save();}
 }
 public static void main(String[] args)throws Exception{new NativeLetterRestartWireMain(Path.of(args[0]),Path.of(args[1])).run();}
}
