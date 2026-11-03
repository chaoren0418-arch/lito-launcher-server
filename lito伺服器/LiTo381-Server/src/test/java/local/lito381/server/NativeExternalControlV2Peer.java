package local.lito381.server;
import java.net.*;import java.net.http.*;import java.nio.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.regex.*;import javax.net.ssl.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
public final class NativeExternalControlV2Peer {
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

 static Map<String,Object> correlation(String h,String job,String terminal)throws Exception {
  String[] strings={"job_id","selected_epoch_hex","private_registration_generation","work_operation_id","correlation_origin"};
  for(String key:strings)require(string(h,key).equals(string(terminal,key)),"same held and terminal "+key);
  require(string(h,"job_id").equals(job),"sole actual FRESH_READ job");
  for(String key:List.of("source","object","channel_id","effective_gear_revision_at_entry","work_deadline_monotonic_nanos"))require(number(h,key)==number(terminal,key),"same held and terminal "+key);
  require(string(h,"private_registration_generation").matches("[0-9a-f]{16}"),"opaque u64 generation");
  return Map.of("hold",h,"terminal",terminal);
 }
 static String heldView(String id)throws Exception {String value=status();var m=Pattern.compile("\"hold\":\\{([^}]*)\\}").matcher(value);require(m.find(),"held view");String v="{"+m.group(1)+"}";require(string(v,"hold_id").equals(id),"same hold");return v;}
 public static void main(String[] args)throws Exception {
  settings=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]))){settings.load(r);}Path output=Path.of(args[1]);
  var checks=new LinkedHashMap<String,Object>();var evidence=new ArrayList<Object>();boolean success=false;String failure=null;
  var context=ModernTlsTestMaterial.clientContext(Path.of(settings.getProperty("tls_directory")));int port=Integer.parseInt(settings.getProperty("tls_port"));
  try(var a=new Client(context,7,port)) {
   a.login();require(number(control("PING",Map.of(),false),"schema")==2,"explicit schema2");
   a.setGear(10304,false);String held=hold(a),pending=fresh(a,8000);entered(held);String observed=heldView(held);
   require(number(observed,"effective_gear_revision_at_entry")==1,"same Work real TLS CAS rev1");release(held);
   String completed=job(pending,false);require(number(completed,"weapon_object")==0&&number(completed,"revision")==1,"same Work qualified unarmed rev1");evidence.add(correlation(observed,pending,completed));
   a.setGear(10304,true);held=hold(a);pending=fresh(a,8000);entered(held);observed=heldView(held);
   require(number(observed,"effective_gear_revision_at_entry")==2,"same Work real TLS CAS rev2");a.back();
   var blocked=(ModernApplicationProtocol.CharacterResult)a.exchange(new ModernApplicationProtocol.CharacterRequest(a.next(),7,Long.MIN_VALUE+1));
   require(blocked.status()==2&&blocked.characters().isEmpty(),"BACK held Work intentionally BackendUnavailable and no owner admitted");
   checks.put("held_BACK_roster_status",blocked.status());release(held);String rejected=job(pending,true);evidence.add(correlation(observed,pending,rejected));
   control("FRESH_READ",scope(a,8000),true);a.roster(7,Long.MIN_VALUE+1);a.select(7,10163,Long.MIN_VALUE+1);
   String freshNew=job(fresh(a,8000),false);require(number(freshNew,"weapon_object")==10252,"sequential new owner actual weapon");
   require(!string(freshNew,"private_registration_generation").equals(string(observed,"private_registration_generation")),"private registration generation changed");
   checks.put("new_owner_terminal",freshNew);
   held=hold(a);pending=fresh(a,500);entered(held);observed=heldView(held);Thread.sleep(650);release(held);
   String late=heldView(held);require(number(late,"backend_remaining_nanos_at_release")<=0,"same Work expired backend deadline");evidence.add(correlation(observed,pending,job(pending,true)));
   require(number(job(fresh(a,8000),false),"weapon_object")==10252,"deadline Work retired and reader recovered");
   checks.put("limited_new_cases_only",true);checks.put("same_Work_rev1_rev2_hold_terminal",true);checks.put("overlap_intentionally_unavailable",true);checks.put("sequential_reselect_passed",true);checks.put("backend_deadline_correlation_passed",true);success=true;
  }catch(Exception e){failure=e.getClass().getName();throw e;}
  finally {
   try{control("SHUTDOWN",Map.of(),false);}catch(Exception ignored){}
   capture.write(output,"control-v2-tls");Files.writeString(output.resolve("control-v2-exchanges.json"),NativeTerrainProbeMain.json(controls));
   checks.put("correlations",evidence);checks.put("state",success?"PASSED_CONTROL_V2_ACTUAL_WORK_LIMITED_CASES":"FAILED");checks.put("failure_type",failure);checks.put("renderer_LOADED",false);checks.put("Rust_live_connected",false);checks.put("authentication_capture",false);
   Files.writeString(output.resolve("control-v2-peer-probe.json"),NativeTerrainProbeMain.json(checks));
  }
 }
}
