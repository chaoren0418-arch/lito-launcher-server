package local.lito381.server;
import java.net.*;import java.net.http.*;import java.nio.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.regex.*;import javax.net.ssl.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
public final class NativeExternalNavigationWirePeer {
 static final String ZERO="0".repeat(64);static final int CAPS=NativePlayerNavigationSqlProbe.CAPS;
 static final NativeWireCapture capture=new NativeWireCapture();
    static final class Client implements AutoCloseable {
        final SSLSocket socket; final long session=capture.session();
        ModernRooms.Scope scope; int id=1; String phase="navigation";
        Client(SSLContext context,int source,int port)throws Exception {
            scope=new ModernRooms.Scope(source,10275,Long.MIN_VALUE,0);
            var raw=new Socket();raw.connect(new InetSocketAddress("127.0.0.1",port),2000);
            socket=(SSLSocket)context.getSocketFactory().createSocket(raw,"localhost",port,true);
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
            socket.setSoTimeout(12000);socket.startHandshake();
            byte[] hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,26,26,CAPS,CAPS,1));send(hello);
            byte[] response=ModernSessionProtocol.readFrame(socket.getInputStream());
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(response);require(ack.version()==26&&ack.capabilities()==CAPS,"TLS melee ACK");
            capture.append(session,phase,scope,"HELLO",hello,response,"COMPLETE_EXCHANGE");
        }
        int next(){return ++id;}
        void send(byte[] bytes)throws Exception {socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message message)throws Exception {
            byte[] request=ModernApplicationProtocol.encode(message,26);send(request);
            byte[] header=socket.getInputStream().readNBytes(16);require(header.length==16,"full TLS application header");
            var h=ByteBuffer.wrap(header);require(h.getInt()==0x4c544d31,"TLS application magic");int length=h.getInt();require(length>=16&&length<=1024*1024,"bounded TLS application frame");
            byte[] body=socket.getInputStream().readNBytes(length-16);require(body.length==length-16,"full TLS application body");
            byte[] response=ByteBuffer.allocate(length).put(header).put(body).array();
            var reply=ModernApplicationProtocol.decode(response,26);require(reply.requestId()==message.requestId(),"TLS echoed request");
            // Never persist authentication requests or their secret buffers.
            if(!(message instanceof ModernApplicationProtocol.Auth))capture.append(session,phase,scope,message.getClass().getSimpleName(),request,response,"COMPLETE_EXCHANGE");
            Arrays.fill(request,(byte)0);return reply;
        }
        List<ModernApplicationProtocol.CharacterRow> roster(int source,long epoch)throws Exception {
            scope=new ModernRooms.Scope(source,scope.objectId(),epoch,0);
            var r=(ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch));require(r.status()==0,"SQL roster");return r.characters();
        }
        void select(int source,long object,long epoch)throws Exception {
            scope=new ModernRooms.Scope(source,object,epoch,0);
            var r=(ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,object,epoch));require(r.status()==0&&r.phase()==1,"actual TLS selected");
            r=(ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,object,epoch,0));require(r.status()==0&&r.phase()==2,"actual TLS lobby");
        }
        void login()throws Exception {
            try(var auth=new ModernApplicationProtocol.Auth(next(),"3333","3333".toCharArray())){require(((ModernApplicationProtocol.AuthResult)exchange(auth)).status()==0,"actual TLS SQL auth");}
            var rows=roster(scope.sourceId(),scope.epoch());require(rows.stream().anyMatch(r->r.objectId()==scope.objectId()),"actual object in authorized SQL roster");select(scope.sourceId(),scope.objectId(),scope.epoch());
        }
        void back()throws Exception {
            var r=(ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()));require(r.status()==0&&r.phase()==0,"actual BACK");
        }
        ModernEquipmentAuthority.Result gear(ModernEquipmentAuthority.Request q)throws Exception {
            return ((ModernApplicationProtocol.EquipmentResult)exchange(new ModernApplicationProtocol.EquipmentRequest(next(),q))).result();
        }
        ModernEquipmentAuthority.Result readGear()throws Exception{return gear(new ModernEquipmentAuthority.Request(scope.sourceId(),scope.objectId(),scope.epoch(),0,false,0,0,ModernEquipmentAuthority.ZERO));}
        void setGear(long item,boolean equipped)throws Exception {
            var current=readGear();require(current.status()==0,"current gear read");
            var r=gear(new ModernEquipmentAuthority.Request(scope.sourceId(),scope.objectId(),scope.epoch(),1,equipped,item,current.snapshot().revision(),current.snapshot().viewToken()));require(r.status()==0&&r.changed(),"TLS current CAS");
        }
        ModernRooms.RoomSnapshot room(ModernRooms.Action action,long room,long revision,int value,ModernRooms.Draft draft)throws Exception {
            var r=(ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next(),new ModernRooms.Command(scope,action,room,revision,value,draft,"")));
            require(r.result().status()==ModernRooms.Status.OK,"room "+action);return r.result().view().joined();
        }
        ModernWorldProtocol.Reply world(ModernWorldProtocol.Action action,ModernWorldProtocol.Frame frame,long room,long revision,String hash)throws Exception {
            var q=new ModernWorldProtocol.Request(scope,action,0,room,revision,frame==null?0:frame.matchId(),frame==null?0:frame.generation(),0,0,0,hash);
            return ((ModernApplicationProtocol.WorldResult)exchange(new ModernApplicationProtocol.WorldRequest(next(),q))).reply();
        }
        void reject(ModernApplicationProtocol.Message message,String reason)throws Exception {
            byte[] request=ModernApplicationProtocol.encode(message,26);send(request);require(socket.getInputStream().read()==-1,"TLS reject "+reason);
            capture.append(session,phase,scope,reason,request,null,"EOF_REJECTED");
        }
        public void close()throws Exception{socket.close();}
    }
 static final HttpClient http=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
 static Properties settings;static final List<Map<String,Object>> controls=new ArrayList<>();
 static String control(String command,Map<String,String> fields,boolean reject)throws Exception {
  String request=UUID.randomUUID().toString();StringBuilder body=new StringBuilder("fixture_id="+settings.getProperty("fixture_id")+"\nrequest_id="+request+"\ncommand="+command+"\n");
  fields.forEach((k,v)->body.append(k).append('=').append(v).append('\n'));
  var q=HttpRequest.newBuilder(URI.create(settings.getProperty("control_url"))).timeout(java.time.Duration.ofSeconds(5)).header("Authorization","Bearer "+settings.getProperty("control_token")).header("Content-Type","text/plain; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
  var r=http.send(q,HttpResponse.BodyHandlers.ofString());require(reject?r.statusCode()==400:r.statusCode()==200,"external control "+command);
  controls.add(Map.of("request_id",request,"command",command,"fields",fields,"http_status",r.statusCode(),"response",r.body()));return r.body();
 }
 static String status()throws Exception {
  var q=HttpRequest.newBuilder(URI.create(settings.getProperty("control_url").replace("/control","/status"))).timeout(java.time.Duration.ofSeconds(5)).header("Authorization","Bearer "+settings.getProperty("control_token")).GET().build();
  var r=http.send(q,HttpResponse.BodyHandlers.ofString());require(r.statusCode()==200,"external status");return r.body();
 }
 static String string(String json,String field)throws Exception {var m=Pattern.compile("\""+Pattern.quote(field)+"\":\"([^\"]*)\"").matcher(json);require(m.find(),"control field "+field);return m.group(1);}
 static long number(String json,String field)throws Exception {var m=Pattern.compile("\""+Pattern.quote(field)+"\":(-?[0-9]+)").matcher(json);require(m.find(),"control number "+field);return Long.parseLong(m.group(1));}
 static Map<String,String> scope(Client c,long deadline){return Map.of("source_id",Integer.toString(c.scope.sourceId()),"object_id",Long.toString(c.scope.objectId()),"epoch_hex",String.format("%016x",c.scope.epoch()),"channel_id","0","deadline_ms",Long.toString(deadline));}
 static String fresh(Client c,long deadline)throws Exception{return string(control("FRESH_READ",scope(c,deadline),false),"job_id");}
 static String job(String id,boolean rejected)throws Exception {
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
  while(System.nanoTime()<end){String value=status();var m=Pattern.compile("\""+Pattern.quote(id)+"\":\\{([^}]*)\\}").matcher(value);if(m.find()){String result="{"+m.group(1)+"}";String state=string(result,"state");if(!state.equals("RUNNING")){require(state.equals(rejected?"REJECTED":"COMPLETE"),"external actual reader result");return result;}}Thread.sleep(50);}
  throw new IllegalStateException("external job timeout");
 }
 static String hold(Client c)throws Exception{return string(control("ARM_HOLD",Map.of("source_id",Integer.toString(c.scope.sourceId()),"object_id",Long.toString(c.scope.objectId())),false),"hold_id");}
 static void entered(String id)throws Exception {
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<end){String s=status();if(s.contains("\"hold_id\":\""+id+"\"")&&s.contains("\"entered\":true"))return;Thread.sleep(25);}throw new IllegalStateException("external actual SQL hold not entered");
 }
 static void release(String id)throws Exception{control("RELEASE",Map.of("hold_id",id),false);}
 public static void main(String[] args)throws Exception {
  settings=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]))){settings.load(r);}Path output=Path.of(args[1]);
  var checks=new LinkedHashMap<String,Object>();boolean success=false;String failure=null;
  var context=ModernTlsTestMaterial.clientContext(Path.of(settings.getProperty("tls_directory")));int port=Integer.parseInt(settings.getProperty("tls_port"));
  try(var a=new Client(context,7,port);var b=new Client(context,42,port)) {
   a.login();b.login();control("PING",Map.of(),false);
   require(number(job(fresh(a,8000),false),"weapon_object")==10304,"external source7 actual weapon");
   require(number(job(fresh(b,8000),false),"weapon_object")==10304,"external source42 actual weapon");
   a.setGear(10304,false);String bare=job(fresh(a,8000),false);require(number(bare,"weapon_object")==0&&number(bare,"revision")==1,"external effective CAS reread");
   require(number(job(fresh(b,8000),false),"weapon_object")==10304,"external other-source CAS isolation");a.setGear(10304,true);
   var room=a.room(ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("external real source melee","",2,1,564,0));room=b.room(ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null);
   room=a.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);room=b.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);
   var initial=a.world(ModernWorldProtocol.Action.PREPARE,null,room.summary().id(),room.summary().revision(),ZERO);require(initial.status()==ModernWorldProtocol.Status.OK,"external actual PREPARE");
   var frame=initial.frame();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while(frame.phase()==ModernWorldProtocol.Phase.PREPARING&&System.nanoTime()<until){Thread.sleep(25);frame=a.world(ModernWorldProtocol.Action.READ,frame,0,0,ZERO).frame();}
   require(frame.phase()==ModernWorldProtocol.Phase.LOADING&&frame.descriptor().features()==1,"external melee DATA");
   String digest=settings.getProperty("map_digest");frame=a.world(ModernWorldProtocol.Action.LOADED,frame,0,0,digest).frame();frame=b.world(ModernWorldProtocol.Action.LOADED,frame,0,0,digest).frame();require(frame.phase()==ModernWorldProtocol.Phase.ACTIVE,"external DTO ACTIVE");
   var combat=(ModernApplicationProtocol.CombatResult)a.exchange(new ModernApplicationProtocol.CombatRequest(a.next(),new ModernCombatProtocol.Request(a.scope,ModernCombatProtocol.Action.READ,frame.matchId(),frame.generation(),0,0)));
   require(combat.reply().status()==ModernCombatProtocol.Status.OK&&combat.reply().frame().vitals().size()==2&&combat.reply().frame().vitals().stream().allMatch(v->v.weaponType()==3&&v.maxHp()==544),"external actual source vitals");
   a.world(ModernWorldProtocol.Action.LEAVE,frame,0,0,ZERO);b.world(ModernWorldProtocol.Action.LEAVE,frame,0,0,ZERO);
   room=a.room(ModernRooms.Action.READ,0,0,0,null);a.room(ModernRooms.Action.LEAVE,room.summary().id(),room.summary().revision(),0,null);room=b.room(ModernRooms.Action.READ,0,0,0,null);b.room(ModernRooms.Action.LEAVE,room.summary().id(),room.summary().revision(),0,null);
   String held=hold(a),pending=fresh(a,8000);entered(held);a.back();release(held);job(pending,true);control("FRESH_READ",scope(a,8000),true);
   a.roster(7,Long.MIN_VALUE+1);a.select(7,10163,Long.MIN_VALUE+1);require(number(job(fresh(a,8000),false),"weapon_object")==10252,"external reselect actual weapon");
   a.back();a.roster(7,Long.MIN_VALUE+2);a.select(7,10275,Long.MIN_VALUE+2);
   held=hold(a);pending=fresh(a,400);entered(held);Thread.sleep(600);release(held);job(pending,true);
   long cutoff=number(control("TIMER_ARM",Map.of("source_id","7","object_id","10275","item_object_id","10304","seconds","8"),false),"cutoff_epoch_millis");
   held=hold(a);pending=fresh(a,10000);entered(held);while(System.currentTimeMillis()<cutoff)Thread.sleep(Math.min(20,Math.max(1,cutoff-System.currentTimeMillis())));release(held);job(pending,true);control("TIMER_CLEAR",Map.of(),false);
   held=hold(a);pending=fresh(a,8000);entered(held);a.close();Thread.sleep(100);release(held);job(pending,true);
   checks.put("SQL_TLS_auth_roster_select_channel",true);checks.put("actual_effective_CAS_and_source_isolation",true);checks.put("real_PREPARE_melee_DATA_DTO_ACTIVE_and_vitals",true);checks.put("held_BACK_reselect_deadline_expiry_disconnect_rejections",true);
   checks.put("actual_SQL_cutoff_exclusive_epoch_millis",cutoff);success=true;
  }catch(Exception e){failure=e.getClass().getName();throw e;}
  finally {
   try{control("TIMER_CLEAR",Map.of(),false);}catch(Exception ignored){}
   try{control("SHUTDOWN",Map.of(),false);}catch(Exception ignored){}
   capture.write(output,"external-tls");Files.writeString(output.resolve("external-control-exchanges.json"),NativeTerrainProbeMain.json(controls));
   checks.put("state",success?"PASSED_EXTERNAL_PROCESS_SQL_TLS_CONTROL_SMOKE":"FAILED");checks.put("failure_type",failure);checks.put("renderer_LOADED",false);checks.put("Rust_live_connected",false);checks.put("authentication_capture",false);
   Files.writeString(output.resolve("external-peer-probe.json"),NativeTerrainProbeMain.json(checks));
  }
  System.out.println("External process SQL/TLS/control smoke passed; DTO acknowledgement only, Rust live pending");
 }
}
