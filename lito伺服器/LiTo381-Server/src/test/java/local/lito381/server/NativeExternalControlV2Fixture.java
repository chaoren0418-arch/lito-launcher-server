package local.lito381.server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;

/** Explicit test-only external-client bridge. Selection is issued by real TLS navigation. */
final class NativeExternalControlV2Fixture implements AutoCloseable {
    final String id=UUID.randomUUID().toString(),token=UUID.randomUUID().toString()+UUID.randomUUID();
    final ModernSettlementMysqlFixture mysql;
    final Path output;
    final ModernEquipmentOverrideStore store;
    final ModernWorldService worlds;
    final ModernServer server;
    final Provider provider;
    final HttpServer control;
    final ExecutorService httpThreads=Executors.newFixedThreadPool(2),jobsThread=Executors.newSingleThreadExecutor();
    final AtomicBoolean shutdown=new AtomicBoolean();
    final Map<String,Map<String,Object>> jobs=new ConcurrentHashMap<>();
    final Set<String> seen=new HashSet<>();
    final long expires;
    final Map<String,Object> ready=new LinkedHashMap<>();
    volatile boolean timerOwned;
    volatile String stopReason="SHUTDOWN";

    final AtomicReference<ReadContext> activeRead=new AtomicReference<>();
    static Object field(Object o,String name)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static String hex(long value){return String.format("%016x",value);}
    static final class ReadContext {
        final String job;final ModernRooms.Scope requested;final Object session,attachment;final long generation;
        volatile Map<String,Object> witness=Map.of();
        ReadContext(String job,ModernRooms.Scope scope,Object reader)throws Exception {
            this.job=job;requested=scope;session=field(reader,"this$0");attachment=field(reader,"val$attachment");generation=(Long)field(reader,"val$expectedGeneration");
            require(scope.equals(field(reader,"val$scope")),"actual registration scope");
        }
        Map<String,Object> view(String state){var m=new LinkedHashMap<String,Object>();m.put("job_id",job);m.put("state",state);m.put("source",requested.sourceId());m.put("object",requested.objectId());m.put("epoch_hex",hex(requested.epoch()));m.put("channel_id",requested.channelId());m.put("work_observed",!witness.isEmpty());m.putAll(witness);return m;}
        void observe(ModernRooms.Scope scope,Object capability,ModernEquipmentLease.Operation operation,long revision)throws Exception {
            var backendScope=ModernBackendScope.current();require(backendScope==operation.scope(),"same actual operation scope");
            Object gate=field(session,"backend"),backendJob=((AtomicReference<?>)field(gate,"current")).get();
            require(backendJob!=null&&field(backendJob,"scope")==backendScope,"actual current backend job");
            Object work=field(backendJob,"work");
            require(field(work,"this$0")==session&&field(work,"operation")==operation&&field(work,"val$attachment")==attachment,"same actual registered Work and operation");
            long actualGeneration=(Long)field(work,"val$expectedGeneration");var actualScope=(ModernRooms.Scope)field(work,"val$scope");
            require(actualGeneration==generation&&scope.equals(actualScope)&&requested.equals(actualScope),"actual Work generation and owner");
            Object lease=field(attachment,"lease");
            require(field(lease,"operation")==operation&&field(lease,"ownerCapability")==capability&&field(session,"owner")==capability,"private owner/lease identity");
            require(operation.matchesSelection(capability,actualScope.epoch())&&operation.matchesKey(new TournamentPresence.CharacterKey(actualScope.sourceId(),actualScope.objectId())),"live selection at observation");
            var m=new LinkedHashMap<String,Object>();m.put("job_id",job);m.put("source",actualScope.sourceId());m.put("object",actualScope.objectId());m.put("selected_epoch_hex",hex(actualScope.epoch()));m.put("channel_id",actualScope.channelId());m.put("private_registration_generation",hex(actualGeneration));m.put("effective_gear_revision_at_entry",revision);m.put("work_deadline_monotonic_nanos",backendScope.deadlineNanos());m.put("work_operation_id",UUID.randomUUID().toString());m.put("correlation_origin","ACTUAL_REGISTERED_WORK_OPERATION_SQL_AND_RECONCILED_STORE");
            require(witness.isEmpty(),"one observation per fresh Work");witness=Collections.unmodifiableMap(m);
        }
    }
    void observeEntry(ModernRooms.Scope scope,Object capability,ModernEquipmentLease.Operation operation,ModernEquipmentAuthority.PhysicalLoaded loaded,long revision)throws Exception {
        ReadContext context=activeRead.get();if(context==null||!context.requested.equals(scope))return;context.observe(scope,capability,operation,revision);
        provider.pause(loaded,context,operation.scope());
    }
    static final class Hold {
        final String id=UUID.randomUUID().toString();final int source;final long object;
        volatile Map<String,Object> witness=Map.of();
        final CountDownLatch release=new CountDownLatch(1);final AtomicBoolean released=new AtomicBoolean();
        volatile boolean entered,finished;volatile long remainingAtEntry,deadlineNanos,releaseAtMillis,remainingAtRelease,sqlQueryStarted;volatile long sqlCutoff=-1;volatile String result="ARMED";
        Hold(int source,long object){this.source=source;this.object=object;}
        Map<String,Object> view(){var v=new LinkedHashMap<String,Object>();v.putAll(witness);v.put("hold_id",id);v.put("source",source);v.put("object",object);v.put("entered",entered);v.put("released",released.get());v.put("finished",finished);v.put("remaining_nanos_at_entry",remainingAtEntry);v.put("result",result);v.put("sql_query_started_epoch_millis",sqlQueryStarted);v.put("sql_cutoff_exclusive_epoch_millis",sqlCutoff);v.put("release_at_epoch_millis",releaseAtMillis);v.put("backend_remaining_nanos_at_release",remainingAtRelease);return v;}
    }
    static final class Provider implements ModernEquipmentAuthority.PhysicalProvider {
        final ModernEquipmentAuthority.PhysicalProvider jdbc;final AtomicReference<Hold> next=new AtomicReference<>();
        final AtomicInteger calls=new AtomicInteger();volatile Hold hold;
        final List<Map<String,Object>> reads=Collections.synchronizedList(new ArrayList<>());
        Provider(ModernEquipmentAuthority.PhysicalProvider jdbc){this.jdbc=jdbc;}
        public ModernEquipmentAuthority.PhysicalLoaded read(ModernAccountAuthentication.Principal principal,int source,long object)throws Exception {
            var work=ModernBackendScope.current();work.checkActive();calls.incrementAndGet();
            var loaded=jdbc.read(principal,source,object);
            reads.add(Map.of("source",source,"object",object,"backend_scope_bound",true,"items",loaded.source().input().equipment().source().inventory().size(),"query_finished_at_millis",System.currentTimeMillis()));
            work.checkActive();return loaded;
        }
        void pause(ModernEquipmentAuthority.PhysicalLoaded loaded,ReadContext context,ModernBackendScope work)throws Exception {
            Hold h=next.get();var actual=context.witness;
            if(h!=null&&h.source==((Number)actual.get("source")).intValue()&&h.object==((Number)actual.get("object")).longValue()&&next.compareAndSet(h,null)) {
                h.witness=actual;h.deadlineNanos=work.deadlineNanos();h.remainingAtEntry=h.deadlineNanos-System.nanoTime();
                var observation=loaded.source().input().equipment().lifetimes();h.sqlQueryStarted=observation.queryStartedEpochMillis();
                if(h.object==10275)h.sqlCutoff=observation.view(10304,System.currentTimeMillis()).expiresAtMillis();
                h.result="HOLDING_AFTER_REAL_SQL_AND_RECONCILED_STORE";h.entered=true;
                try {long wait=Math.min(TimeUnit.MILLISECONDS.toNanos(8000),h.remainingAtEntry);if(wait<=0||!h.release.await(wait,TimeUnit.NANOSECONDS))throw new IOException("bounded hold deadline");work.checkActive();h.result="RELEASED";}
                catch(Exception e){h.result="REJECTED";throw e;}finally{h.finished=true;}
            }
            work.checkActive();
        }
    }
    NativeExternalControlV2Fixture(ModernSettlementMysqlFixture mysql,ModernServerConfig config,Map<Integer,ModernJdbcBackend> backends,Path output,int lifetime)throws Exception {
        this.mysql=mysql;this.output=output;expires=System.currentTimeMillis()+lifetime*1000L;
        var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(output.resolve("native-dm564-candidate.properties")),()->false);
        var binding=candidate.binding();var mapping=new ModernWorldService.Mapping(binding,candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100),TimeUnit.MILLISECONDS.toNanos(100));
        var limits=new ModernWorldService.Limits(1,1,1,10000,10,new ModernBattleEngine.Limits(15000,0,30000));
        var auth=new LinkedHashMap<Integer,ModernAccountAuthentication.SourceBinding>();
        var sources=new LinkedHashMap<Integer,ModernEquipmentRepository.Source>();var data=new LinkedHashMap<Integer,ModernCharacterDataRepository.Source>();
        for(var c:config.sources()) {
            var jdbc=backends.get(c.id());auth.put(c.id(),ModernAccountAuthentication.SourceBinding.direct(c.name(),jdbc::authenticate));
            sources.put(c.id(),new ModernEquipmentRepository.Source(c,jdbc));data.put(c.id(),new ModernCharacterDataRepository.Source(jdbc,c.catalogDatabase()));
        }
        store=new ModernEquipmentOverrideStore(config.equipmentStoreDirectory());provider=new Provider(ModernEquipmentRepository.physicalJdbc(sources));
        var adapter=new NativePlayerLoadoutAdapter(provider,store,System::currentTimeMillis);
        var equipment=new ModernEquipmentAuthority(ModernEquipmentRepository.jdbc(sources),ModernEquipmentRepository.combatJdbc(sources),ModernEquipmentRepository.physicalJdbc(sources),store,System::currentTimeMillis);
        var rooms=new ModernRooms(Set.of(binding.key()));
        worlds=ModernWorldService.withNativeSourceMelee(rooms,Map.of(binding.key(),mapping),limits,adapter);
        server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(output.resolve("configuration-tls"),false),new ModernServer.Limits(16,16,1000,16,2,4,10000,2000,15000,30000),ModernAccountAuthentication.repositories(auth,ignored->false),ModernJdbcBackend.characters(backends),new TournamentPresence(),ModernCharacterDataRepository.jdbc(data),equipment,store,rooms,worlds,26);
        control=HttpServer.create(new InetSocketAddress("127.0.0.1",0),8);control.setExecutor(httpThreads);
        try {
        control.createContext("/control",this::handle);control.createContext("/status",this::handle);
        ready.put("schema",2);ready.put("control_contract","NATIVE_SELECTED_WORK_CONTROL_V2");ready.put("hold_point","AFTER_REAL_SQL_AND_SAME_WORK_RECONCILED_STORE_BEFORE_COMMIT");ready.put("overlap_policy","SAME_SESSION_BUSY_UNTIL_ACTUAL_WORK_RETIRES");ready.put("state","READY");ready.put("fixture_id",id);ready.put("expires_at_epoch_millis",expires);
        ready.put("owner",nativeOwner(ProcessHandle.current(),output.resolve("helper-identity.properties")));
        ready.put("candidate_manifest_sha256",System.getProperty("native.external.manifest"));
        ready.put("runtime_namespace",System.getProperty("native.external.namespace"));
        ready.put("runtime_namespace_root",output.getParent().toString());
        ready.put("navigation_baseline_manifest_sha256","B20D6C58A8CC52376164FCFCC7059CB52E9C76EFFAC408CA9039534543A9055A");
        ready.put("tls",Map.of("host","127.0.0.1","port",server.address().getPort(),"server_name","localhost","root_der_path",output.resolve("configuration-tls/root.der").toString(),"root_der_sha256",NativePlayerNavigationSqlMain.sha(Files.readAllBytes(output.resolve("configuration-tls/root.der"))),"server_der_sha256",NativePlayerNavigationSqlMain.sha(Files.readAllBytes(output.resolve("configuration-tls/server.der")))));
        ready.put("control",Map.of("host","127.0.0.1","port",control.getAddress().getPort(),"path","/control","token",token));
        var mysqlOwner=new LinkedHashMap<>(nativeOwner(ProcessHandle.of(mysql.processId()).orElseThrow(),output.resolve("mysql-identity.properties")));
        mysqlOwner.put("port",mysql.port());mysqlOwner.put("datadir",mysql.dataDirectory().toRealPath().toString());ready.put("mysql",mysqlOwner);
        ready.put("protocol",Map.of("version",26,"capabilities",NativePlayerNavigationSqlProbe.CAPS));
        ready.put("sources",List.of(Map.of("id",7,"database","380_ac_1"),Map.of("id",42,"database","380_ac_2")));
        ready.put("selection",Map.of("initial_object",10275,"reselect_object",10163,"epoch_is_opaque_u64",true));
        ready.put("resources",Map.of("map_id",564,"source_root",output.resolve("resources/LiTo/LTmap").toString(),"map_digest",binding.map().digest(),"sector_sha256","B68EE758F17448316C70ED5D49AD65EB6432AF483F0591A5F8AA7987F9AF6865"));
        ready.put("limits",Map.of("fixture_lifetime_seconds",lifetime,"max_hold_millis",8000,"max_fresh_read_deadline_millis",10000));
        control.start();write(output.resolve("ready.json"),ready);
        }catch(Exception e){try{close();}catch(Exception cleanup){e.addSuppressed(cleanup);}throw e;}
    }
    static String filetime(ProcessHandle p){var t=p.info().startInstant().orElseThrow();return Long.toString(Math.addExact(Math.multiplyExact(t.getEpochSecond()+11644473600L,10000000L),t.getNano()/100));}
    static Map<String,Object> owner(ProcessHandle p){return Map.of("pid",p.pid(),"start_utc",p.info().startInstant().orElseThrow().toString(),"creation_filetime_utc",filetime(p));}
    static Map<String,Object> nativeOwner(ProcessHandle process,Path witness)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!Files.isRegularFile(witness)&&System.nanoTime()<end)Thread.sleep(10);
        require(Files.isRegularFile(witness)&&!Files.isSymbolicLink(witness)&&Files.size(witness)<=512,"native process witness");
        var p=new Properties();try(var r=Files.newBufferedReader(witness,StandardCharsets.UTF_8)){p.load(r);}
        require(p.size()==2&&Long.parseLong(p.getProperty("pid"))==process.pid(),"native PID witness matches");
        String ft=p.getProperty("creation_filetime_utc");require(ft.matches("[0-9]{17,18}"),"native FILETIME witness");
        long exact=Long.parseLong(ft);require(exact/10000==Long.parseLong(filetime(process))/10000,"native and JDK millisecond start match");
        long unix=exact-116444736000000000L;var instant=java.time.Instant.ofEpochSecond(unix/10000000L,(unix%10000000L)*100L);
        return Map.of("pid",process.pid(),"start_utc",instant.toString(),"creation_filetime_utc",ft,"jdk_start_utc",process.info().startInstant().orElseThrow().toString(),"creation_time_source","launcher Get-Process native FILETIME; helper independently checks PID and millisecond start");
    }
    static void write(Path p,Object v)throws Exception {Path tmp=p.resolveSibling(p.getFileName()+".tmp");Files.writeString(tmp,NativeTerrainProbeMain.json(v),StandardCharsets.UTF_8);long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(true){try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);break;}catch(AccessDeniedException busy){if(System.nanoTime()-until>=0)throw busy;Thread.sleep(10);}}}
    Map<String,Object> status(){var v=new LinkedHashMap<String,Object>();v.put("schema",2);v.put("fixture_id",id);v.put("state",shutdown.get()?"STOPPING":"READY");v.put("expires_at_epoch_millis",expires);v.put("server",server.snapshot());v.put("provider_calls",provider.calls.get());v.put("hold",provider.hold==null?null:provider.hold.view());v.put("jobs",new TreeMap<>(jobs));v.put("timer_owned",timerOwned);return v;}
    void handle(HttpExchange exchange)throws IOException {
        int code=200;Object response;
        try {
            if(!exchange.getRemoteAddress().getAddress().isLoopbackAddress()||!Objects.equals(exchange.getRequestHeaders().getFirst("Authorization"),"Bearer "+token))throw new SecurityException();
            if(exchange.getRequestURI().getPath().equals("/status")) {
                if(!exchange.getRequestMethod().equals("GET"))throw new IllegalArgumentException();response=status();
            } else {
                if(!exchange.getRequestURI().getPath().equals("/control")||!exchange.getRequestMethod().equals("POST")||exchange.getRequestURI().getRawQuery()!=null)throw new IllegalArgumentException();
                String length=exchange.getRequestHeaders().getFirst("Content-Length");if(length==null||!length.matches("[0-9]{1,4}"))throw new IllegalArgumentException();
                int count=Integer.parseInt(length);if(count<1||count>4096)throw new IllegalArgumentException();
                byte[] bytes=exchange.getRequestBody().readNBytes(count);if(bytes.length!=count)throw new IllegalArgumentException();
                var q=new HashMap<String,String>();String body=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                for(String line:body.split("\n")) {
                    if(line.isEmpty())continue;int at=line.indexOf('=');if(at<1||line.indexOf('\r')>=0)throw new IllegalArgumentException();
                    if(q.put(line.substring(0,at),line.substring(at+1))!=null)throw new IllegalArgumentException();
                }
                String request=q.get("request_id");if(!id.equals(q.get("fixture_id"))||request==null||!UUID.fromString(request).toString().equals(request))throw new SecurityException();
                synchronized(seen){if(seen.size()>=256||!seen.add(request))throw new IllegalArgumentException();}
                response=Map.of("schema",2,"fixture_id",id,"request_id",request,"status","OK","result",command(q));
            }
        }catch(SecurityException e){code=403;response=Map.of("schema",2,"status","REJECTED","reason","CONTROL_SCOPE");}
        catch(Exception e){code=400;response=Map.of("schema",2,"status","REJECTED","reason","CONTROL_ARGUMENT_OR_STATE");}
        try{byte[] bytes=NativeTerrainProbeMain.json(response).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception e){throw new IOException("control response failed");}finally{exchange.close();}
    }
    void keys(Map<String,String> q,String...extra){var allowed=new HashSet<>(List.of("fixture_id","request_id","command"));allowed.addAll(List.of(extra));if(!q.keySet().equals(allowed))throw new IllegalArgumentException();}
    static long number(Map<String,String> q,String name,long min,long max){String s=q.get(name);if(s==null||!s.matches("[0-9]{1,19}"))throw new IllegalArgumentException();long n=Long.parseLong(s);if(n<min||n>max)throw new IllegalArgumentException();return n;}
    synchronized Object command(Map<String,String> q)throws Exception {
        if(shutdown.get()||System.currentTimeMillis()>=expires)throw new IllegalArgumentException();
        switch(q.getOrDefault("command","")) {
            case "PING":keys(q);return status();
            case "ARM_HOLD":{
                keys(q,"source_id","object_id");int source=(int)number(q,"source_id",7,42);if(source!=7&&source!=42)throw new IllegalArgumentException();long object=number(q,"object_id",1,Long.MAX_VALUE);
                if(provider.next.get()!=null||provider.hold!=null&&!provider.hold.finished)throw new IllegalArgumentException();
                Hold h=new Hold(source,object);provider.hold=h;provider.next.set(h);return h.view();
            }
            case "RELEASE":{
                keys(q,"hold_id");Hold h=provider.hold;if(h==null||!h.id.equals(q.get("hold_id"))||!h.released.compareAndSet(false,true))throw new IllegalArgumentException();h.releaseAtMillis=System.currentTimeMillis();h.remainingAtRelease=h.entered?h.deadlineNanos-System.nanoTime():0;h.release.countDown();return h.view();
            }
            case "FRESH_READ":{
                keys(q,"source_id","object_id","epoch_hex","channel_id","deadline_ms");int source=(int)number(q,"source_id",7,42);if(source!=7&&source!=42)throw new IllegalArgumentException();long object=number(q,"object_id",1,Long.MAX_VALUE);
                String epoch=q.get("epoch_hex");if(epoch==null||!epoch.matches("[0-9a-f]{16}"))throw new IllegalArgumentException();
                int channel=(int)number(q,"channel_id",0,0);long deadline=number(q,"deadline_ms",1,10000);
                var scope=new ModernRooms.Scope(source,object,Long.parseUnsignedLong(epoch,16),channel);
                var reader=NativePlayerNavigationSqlProbe.registered(worlds,scope);
                if(jobs.size()>=32||jobs.values().stream().anyMatch(v->v.get("state").equals("RUNNING")))throw new IllegalArgumentException();
                String job=UUID.randomUUID().toString();ReadContext context=new ReadContext(job,scope,reader);
                if(!activeRead.compareAndSet(null,context))throw new IllegalArgumentException();jobs.put(job,context.view("RUNNING"));
                jobsThread.submit(()->{try{
                    var f=reader.read(System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(deadline));var p=f.profile();
                    require(!context.witness.isEmpty()&&p.equipmentRevision()==((Number)context.witness.get("effective_gear_revision_at_entry")).longValue(),"returned same Work revision");
                    var result=context.view("COMPLETE");result.put("revision",p.equipmentRevision());result.put("weapon_object",p.weapon().objectId());result.put("max_hp",p.effective().maximumHp());result.put("evaluated_at_millis",p.evaluatedAtMillis());result.put("valid_until_millis",p.validUntilMillis());jobs.put(job,result);
                }catch(Exception e){var result=context.view("REJECTED");result.put("reason","ACTUAL_READER_REJECTED");jobs.put(job,result);}finally{activeRead.compareAndSet(context,null);}});
                return Map.of("job_id",job);
            }
            case "TIMER_ARM":{
                keys(q,"source_id","object_id","item_object_id","seconds");require(number(q,"source_id",7,7)==7&&number(q,"object_id",10275,10275)==10275&&number(q,"item_object_id",10304,10304)==10304,"timer scope");long seconds=number(q,"seconds",2,30);
                if(timerOwned)throw new IllegalArgumentException();
                try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                    try(var r=s.executeQuery("SELECT COUNT(*) FROM `380_ac_1`.character_items_time WHERE itemr_obj_id=10304")){r.next();require(r.getInt(1)==0,"timer original absence");}
                    require(s.executeUpdate("INSERT INTO `380_ac_1`.character_items_time VALUES (10304,CURRENT_TIMESTAMP + INTERVAL "+seconds+" SECOND)")==1,"owned timer insert");timerOwned=true;
                    try(var r=s.executeQuery("SELECT usertime FROM `380_ac_1`.character_items_time WHERE itemr_obj_id=10304")){require(r.next(),"owned timer read");return Map.of("item",10304,"cutoff_epoch_millis",r.getTimestamp(1).getTime());}
                }
            }
            case "TIMER_CLEAR":keys(q);return Map.of("changed",clearTimer());
            case "SHUTDOWN":keys(q);shutdown.set(true);return Map.of("accepted",true);
            default:throw new IllegalArgumentException();
        }
    }
    synchronized boolean clearTimer()throws Exception {
        if(!timerOwned)return false;
        try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()){require(s.executeUpdate("DELETE FROM `380_ac_1`.character_items_time WHERE itemr_obj_id=10304")==1,"owned timer restore");}
        timerOwned=false;return true;
    }
    void waitForShutdown()throws Exception {while(!shutdown.get()&&System.currentTimeMillis()<expires){Thread.sleep(100);}if(!shutdown.get()){stopReason="HARD_DEADLINE";shutdown.set(true);}}
    @Override public void close()throws Exception {
        shutdown.set(true);if(provider.hold!=null)provider.hold.release.countDown();
        Exception failure=null;boolean quiet=false;
        jobsThread.shutdownNow();
        try{server.close();require(server.awaitTermination(5000),"external TLS workers retired");require(jobsThread.awaitTermination(5,TimeUnit.SECONDS),"external callback retired");}
        catch(Exception e){failure=e;}
        try{worlds.close();quiet=worlds.isQuiescent()&&store.isQuiescent();require(quiet,"external world/store retired");}
        catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}
        try{clearTimer();}catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}
        finally{control.stop(0);httpThreads.shutdownNow();}
        try{require(httpThreads.awaitTermination(5,TimeUnit.SECONDS),"control workers retired");}
        catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}
        write(output.resolve("provider-reads.json"),provider.reads);write(output.resolve("control-final-status.json"),status());
        var retired=new LinkedHashMap<String,Object>();retired.put("schema",2);retired.put("state",failure==null?"TLS_CONTROL_AND_BACKEND_RETIRED":"FAILED_RETIREMENT");retired.put("fixture_id",id);retired.put("reason",stopReason);retired.put("timer_restored",!timerOwned);retired.put("TLS_port",server.address().getPort());retired.put("control_port",control.getAddress().getPort());retired.put("world_store_quiescent",quiet);write(output.resolve("retirement.json"),retired);
        var ended=new LinkedHashMap<>(ready);ended.put("state",failure==null?"RETIRED":"FAILED");write(output.resolve("ready.json"),ended);
        if(failure!=null)throw failure;
    }
}
