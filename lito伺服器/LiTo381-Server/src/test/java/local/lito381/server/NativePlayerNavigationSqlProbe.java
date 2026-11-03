package local.lito381.server;

import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;

/** Actual TLS selection issues every tested lease. Reflection only retains the
 * already registered reader to exercise delayed callbacks after invalidation. */
final class NativePlayerNavigationSqlProbe {
    static final String ZERO="0".repeat(64);
    static final int CAPS=255|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
    static final NativeWireCapture capture=new NativeWireCapture();
    static final class Client implements AutoCloseable {
        final SSLSocket socket; final long session=capture.session();
        ModernRooms.Scope scope; int id=1; String phase="navigation";
        Client(ModernServer server,SSLContext context,int source)throws Exception {
            scope=new ModernRooms.Scope(source,NativePlayerNavigationSqlMain.objectId,Long.MIN_VALUE,0);
            var raw=new Socket();raw.connect(server.address(),2000);
            socket=(SSLSocket)context.getSocketFactory().createSocket(raw,"localhost",server.address().getPort(),true);
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
    static final class Provider implements ModernEquipmentAuthority.PhysicalProvider {
        final ModernEquipmentAuthority.PhysicalProvider jdbc; final AtomicInteger calls=new AtomicInteger();
        final Map<Integer,ModernEquipmentAuthority.PhysicalLoaded> last=new ConcurrentHashMap<>();
        final List<Map<String,Object>> reads=Collections.synchronizedList(new ArrayList<>());
        volatile CountDownLatch entered,release;volatile String mode="normal";
        volatile Map<String,Object> expiryObservation;
        Provider(ModernEquipmentAuthority.PhysicalProvider jdbc){this.jdbc=jdbc;}
        void block(String value){mode=value;entered=new CountDownLatch(1);release=new CountDownLatch(1);}
        public ModernEquipmentAuthority.PhysicalLoaded read(ModernAccountAuthentication.Principal p,int source,long object)throws Exception {
            var work=ModernBackendScope.current();work.checkActive();calls.incrementAndGet();var loaded=jdbc.read(p,source,object);last.put(source,loaded);
            reads.add(Map.of("source",source,"object",object,"canonical_account",p.accountForSource(source),"backend_scope_bound",true,"source_items",loaded.source().input().equipment().source().inventory().size(),"thread",Thread.currentThread().getName()));
            String selected=mode;if(!selected.equals("normal")) {
                mode="normal";entered.countDown();
                if(selected.equals("expiry")) {
                    long cutoff=loaded.source().input().equipment().lifetimes().view(10304,System.currentTimeMillis()).expiresAtMillis();
                    require(cutoff>System.currentTimeMillis(),"live SQL deadline begins active");
                    while(System.currentTimeMillis()<cutoff)Thread.sleep(Math.min(20,Math.max(1,cutoff-System.currentTimeMillis())));
                    expiryObservation=Map.of("source",source,"object",object,"item",10304,"cutoff_exclusive",cutoff,
                            "provider_finished_at_millis",System.currentTimeMillis(),"backend_remaining_nanos",work.deadlineNanos()-System.nanoTime(),"actual_wall_clock",true);
                    require(work.deadlineNanos()-System.nanoTime()>TimeUnit.MILLISECONDS.toNanos(500),"SQL expiry precedes backend deadline");
                } else require(release.await(8,TimeUnit.SECONDS),"bounded blocked provider released");
            }
            work.checkActive();return loaded;
        }
    }
    static Object field(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    @SuppressWarnings("unchecked") static ModernWorldService.PhysicalReader registered(ModernWorldService worlds,ModernRooms.Scope scope)throws Exception {
        Object gate=field(worlds,"gate");synchronized(gate){var registrations=(Map<Object,Object>)field(worlds,"registrations");for(var r:registrations.values())if(scope.equals(field(r,"scope")))return (ModernWorldService.PhysicalReader)field(r,"physical");}
        throw new IllegalStateException("actual navigation registration absent");
    }
    static ModernPhysicalCombat.Fighter fresh(ModernWorldService.PhysicalReader reader)throws Exception{return reader.read(System.nanoTime()+TimeUnit.SECONDS.toNanos(8));}
    static void rejectBeforeRead(ModernWorldService.PhysicalReader reader,Provider provider)throws Exception {
        int before=provider.calls.get();NativePlayerLoadoutSqlProbe.rejected(()->fresh(reader));require(provider.calls.get()==before,"retired attachment rejected before SQL");
    }
    static void save(Path output,String name,Client client,Provider provider,ModernPhysicalCombat.Fighter fighter)throws Exception {
        var loaded=provider.last.get(client.scope.sourceId());
        require(loaded.source().input().equipment().source().equals(fighter.profile().source()),"projection from exact real Work SQL bundle");
        // Evidence-only serialization of the actual returned fighter and its read
        // bundle; this constructed record is never passed into navigation.
        NativePlayerLoadoutSqlProbe.save(output,name,new NativePlayerLoadoutAdapter.Captured(client.scope,loaded.ownerBinding(),loaded.source(),fighter));
    }
    static Future<Boolean> pending(ExecutorService threads,ModernWorldService.PhysicalReader reader,long millis){return threads.submit(()->{try{reader.read(System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis));return false;}catch(Exception expected){return true;}});}
    static void waitZero(ModernServer server)throws Exception{NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==0&&server.snapshot().backendActive()==0&&server.snapshot().backendQueued()==0);}
    static void run(ModernSettlementMysqlFixture mysql,ModernServerConfig config,Map<Integer,ModernJdbcBackend> backends,Map<String,Object> report,Path output)throws Exception {
        Path candidatePath=Path.of(System.getProperty("native.navigation.candidate"));
        var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(candidatePath),()->false);
        var binding=candidate.binding();var mapping=new ModernWorldService.Mapping(binding,candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100),TimeUnit.MILLISECONDS.toNanos(100));
        var limits=new ModernWorldService.Limits(1,1,1,10000,10,new ModernBattleEngine.Limits(15000,0,30000));
        var auth=new LinkedHashMap<Integer,ModernAccountAuthentication.SourceBinding>();var sources=new LinkedHashMap<Integer,ModernEquipmentRepository.Source>();var data=new LinkedHashMap<Integer,ModernCharacterDataRepository.Source>();
        for(var c:config.sources()){var jdbc=backends.get(c.id());auth.put(c.id(),ModernAccountAuthentication.SourceBinding.direct(c.name(),jdbc::authenticate));sources.put(c.id(),new ModernEquipmentRepository.Source(c,jdbc));data.put(c.id(),new ModernCharacterDataRepository.Source(jdbc,c.catalogDatabase()));}
        var checks=new LinkedHashMap<String,Object>();var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(binding.key()));
        var store=new ModernEquipmentOverrideStore(config.equipmentStoreDirectory());var provider=new Provider(ModernEquipmentRepository.physicalJdbc(sources));
        var adapter=new NativePlayerLoadoutAdapter(provider,store,System::currentTimeMillis);
        var equipment=new ModernEquipmentAuthority(ModernEquipmentRepository.jdbc(sources),ModernEquipmentRepository.combatJdbc(sources),ModernEquipmentRepository.physicalJdbc(sources),store,System::currentTimeMillis);
        try(var unchanged=new ModernWorldService(new ModernRooms(Set.of(binding.key())),Map.of(binding.key(),mapping),limits)){require(unchanged.nativePlayerLoadoutAdapter()==null,"ordinary factory remains default off");}
        boolean invalid=false;try{ModernWorldService.withNativeSourceMelee(rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100),TimeUnit.MILLISECONDS.toNanos(100),TimeUnit.MILLISECONDS.toNanos(100))),limits,adapter);}catch(IllegalArgumentException expected){invalid=true;}require(invalid,"unqualified spell mapping refused");
        var worlds=ModernWorldService.withNativeSourceMelee(rooms,Map.of(binding.key(),mapping),limits,adapter);
        var tls=output.resolve("configuration-tls");var server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(tls,false),new ModernServer.Limits(16,16,1000,16,2,4,10000,2000,15000,30000),ModernAccountAuthentication.repositories(auth,ignored->false),ModernJdbcBackend.characters(backends),presence,ModernCharacterDataRepository.jdbc(data),equipment,store,rooms,worlds,26);
        report.put("owned_TLS_listener",Map.of("host","127.0.0.1","port",server.address().getPort(),"owner_pid",ProcessHandle.current().pid()));NativePlayerNavigationSqlMain.write(output,report);
        var context=ModernTlsTestMaterial.clientContext(tls);var threads=Executors.newSingleThreadExecutor();int port=server.address().getPort();
        try {
            try(var a=new Client(server,context,7);var b=new Client(server,context,42)) {
                a.login();b.login();var ra=registered(worlds,a.scope);var rb=registered(worlds,b.scope);
                var first=fresh(ra);var other=fresh(rb);save(output,"source7-initial",a,provider,first);save(output,"source42-initial",b,provider,other);
                require(first.profile().weapon().objectId()==10304&&other.profile().weapon().objectId()==10304,"real equipped weapon both sources");
                a.setGear(10304,false);var bare=fresh(ra);var untouched=fresh(rb);require(bare.profile().weapon().objectId()==0&&untouched.profile().weapon().objectId()==10304,"TLS CAS fresh reread source isolation");save(output,"source7-after-CAS",a,provider,bare);save(output,"source42-after-other-CAS",b,provider,untouched);
                a.setGear(10305,true);var armor=fresh(ra);var armorOther=fresh(rb);require(armor.profile().effective().armorClass()==-3&&armorOther.profile().effective().armorClass()==2,"TLS real armor effective isolation");save(output,"source7-after-real-armor-CAS",a,provider,armor);save(output,"source42-after-other-armor-CAS",b,provider,armorOther);
                a.setGear(10305,false);a.setGear(10304,true);
                var room=a.room(ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("source melee SQL TLS","",2,1,564,0));room=b.room(ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null);room=a.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);room=b.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);
                var initial=a.world(ModernWorldProtocol.Action.PREPARE,null,room.summary().id(),room.summary().revision(),ZERO);require(initial.status()==ModernWorldProtocol.Status.OK,"actual TLS world prepare admitted");
                ModernWorldProtocol.Frame frame=initial.frame();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
                while(frame.phase()==ModernWorldProtocol.Phase.PREPARING&&System.nanoTime()<until){Thread.sleep(20);frame=a.world(ModernWorldProtocol.Action.READ,frame,0,0,ZERO).frame();}
                require(frame.phase()==ModernWorldProtocol.Phase.LOADING&&frame.descriptor().features()==ModernMatchDescriptor.COMBAT&&frame.descriptor().ruleset()==ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,"actual melee descriptor no extras");
                frame=a.world(ModernWorldProtocol.Action.LOADED,frame,0,0,binding.map().digest()).frame();frame=b.world(ModernWorldProtocol.Action.LOADED,frame,0,0,binding.map().digest()).frame();
                require(frame.phase()==ModernWorldProtocol.Phase.ACTIVE,"actual TLS engine active (DTO acknowledgement only)");
                var combat=(ModernApplicationProtocol.CombatResult)a.exchange(new ModernApplicationProtocol.CombatRequest(a.next(),new ModernCombatProtocol.Request(a.scope,ModernCombatProtocol.Action.READ,frame.matchId(),frame.generation(),0,0)));
                require(combat.reply().status()==ModernCombatProtocol.Status.OK&&combat.reply().frame().vitals().size()==2&&combat.reply().frame().vitals().stream().allMatch(v->v.weaponType()==3&&v.maxHp()==544),"actual TLS source-derived melee vitals");
                checks.put("actual_TLS_world_prepare_to_SQL_and_melee_publication",true);checks.put("DATA_descriptor",Map.of("ruleset","PVP_SKIRMISH","features",1,"numeric_ruleset",ModernPhysicalCombat.RULESET));
                a.world(ModernWorldProtocol.Action.LEAVE,frame,0,0,ZERO);b.world(ModernWorldProtocol.Action.LEAVE,frame,0,0,ZERO);
                room=a.room(ModernRooms.Action.READ,0,0,0,null);a.room(ModernRooms.Action.LEAVE,room.summary().id(),room.summary().revision(),0,null);
                room=b.room(ModernRooms.Action.READ,0,0,0,null);b.room(ModernRooms.Action.LEAVE,room.summary().id(),room.summary().revision(),0,null);
                provider.block("hold");var backPending=pending(threads,ra,8000);require(provider.entered.await(5,TimeUnit.SECONDS),"actual selected Work read reached");a.back();provider.release.countDown();require(backPending.get(5,TimeUnit.SECONDS),"inflight BACK refuses publication");rejectBeforeRead(ra,provider);
                var roster=a.roster(7,Long.MIN_VALUE+1);var next=roster.stream().filter(r->r.objectId()!=NativePlayerNavigationSqlMain.objectId).findFirst().orElseThrow();a.select(7,next.objectId(),Long.MIN_VALUE+1);var replacement=registered(worlds,a.scope);save(output,"source7-reselected-actual-character",a,provider,fresh(replacement));rejectBeforeRead(ra,provider);
                a.back();a.roster(7,Long.MIN_VALUE+2);a.select(7,NativePlayerNavigationSqlMain.objectId,Long.MIN_VALUE+2);ra=registered(worlds,a.scope);rejectBeforeRead(replacement,provider);
                provider.block("hold");var expired=pending(threads,ra,400);require(provider.entered.await(5,TimeUnit.SECONDS),"deadline Work reached SQL");Thread.sleep(600);provider.release.countDown();require(expired.get(5,TimeUnit.SECONDS),"actual backend deadline rejects result");
                NativeCanonicalServiceProbeMain.await(()->server.snapshot().backendActive()==0&&server.snapshot().backendQueued()==0);
                try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()){try(var r=s.executeQuery("SELECT COUNT(*) FROM `380_ac_1`.character_items_time WHERE itemr_obj_id=10304")){r.next();require(r.getInt(1)==0,"original item time row absent");}s.executeUpdate("INSERT INTO `380_ac_1`.character_items_time VALUES (10304,CURRENT_TIMESTAMP + INTERVAL 8 SECOND)");}
                try{save(output,"source7-actual-SQL-timed-fixture",a,provider,fresh(ra));provider.block("expiry");var stale=pending(threads,ra,10000);require(provider.entered.await(5,TimeUnit.SECONDS),"timed actual SQL read");require(stale.get(10,TimeUnit.SECONDS),"real wall-clock SQL deadline rejects late result");}
                finally{try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()){require(s.executeUpdate("DELETE FROM `380_ac_1`.character_items_time WHERE itemr_obj_id=10304")==1,"timed SQL fixture restored");}}
                provider.block("hold");var disconnected=pending(threads,ra,8000);require(provider.entered.await(5,TimeUnit.SECONDS),"disconnect Work reached SQL");a.close();NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==1);provider.release.countDown();require(disconnected.get(5,TimeUnit.SECONDS),"actual disconnect rejects pending publication");rejectBeforeRead(ra,provider);
                checks.put("BACK_reselection_disconnect_and_actual_deadline_rejected",true);checks.put("all_retired_readers_rejected_before_SQL",true);checks.put("actual_reselected_object",next.objectId());checks.put("expiry_between_SQL_read_and_publication_real_wall_clock",true);
                checks.put("actual_SQL_deadline_observation",provider.expiryObservation);
            }
            waitZero(server);
            for(String test:List.of("wrong_source","stale_epoch"))try(var client=new Client(server,context,7)) {
                client.login();int count=provider.calls.get();var s=client.scope;
                var bad=new ModernApplicationProtocol.EquipmentRequest(client.next(),new ModernEquipmentAuthority.Request(test.equals("wrong_source")?42:7,s.objectId(),test.equals("stale_epoch")?s.epoch()+1:s.epoch(),0,false,0,0,ModernEquipmentAuthority.ZERO));
                client.reject(bad,test);NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==0);require(provider.calls.get()==count,"wire scope rejection before physical SQL");
            }
            checks.put("TLS_cross_source_and_stale_epoch_EOF",true);checks.put("selection_issuer", "actual ModernNavigationSession.select after TLS SQL authentication and roster reread");checks.put("test_selection_issuer",false);
        } finally {
            if(provider.release!=null)provider.release.countDown();threads.shutdownNow();require(threads.awaitTermination(5,TimeUnit.SECONDS),"test callback thread retired");server.close();require(server.awaitTermination(5000),"TLS server/backend workers retired");worlds.close();
            require(worlds.isQuiescent()&&store.isQuiescent(),"world/store retired");
            try(var listener=new java.net.ServerSocket(port,1,InetAddress.getByName("127.0.0.1"))){checks.put("TLS_listener_released",true);}
            capture.write(output,"navigation-tls");Files.writeString(output.resolve("navigation-provider-reads.json"),NativeTerrainProbeMain.json(provider.reads));
        }
        checks.put("state","PASSED_ACTUAL_SELECTED_OWNER_NAVIGATION_SQL_TLS");checks.put("SQL",true);checks.put("TLS",true);checks.put("projection_count",8);checks.put("projection_origin","evidence-only record from actual registered reader returned Fighter plus exact same Work provider bundle");checks.put("old92_93_SCENE_ONLY_unchanged",true);checks.put("renderer_LOADED",false);checks.put("GUI",false);checks.put("full_combat",false);checks.put("new_game_wire_extension",false);checks.put("offline_LPL1_is_session_authority",false);checks.put("server_retired",true);checks.put("provider_calls",provider.calls.get());
        report.put("navigation",checks);Files.writeString(output.resolve("navigation-work-probe.json"),NativeTerrainProbeMain.json(checks));
    }
}
