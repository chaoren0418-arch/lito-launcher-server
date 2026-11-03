package local.lito381.server;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
import static local.lito381.server.ModernNativeCanonicalWorldProtocol.*;

/** Actual TLS/authentication/navigation/catalog 90-91 DATA. Limited geometry DTO peer, not Rust/renderer acceptance. */
public final class NativeActorSqlTlsPeer {
    static ModernNativeCatalogWorldProtocol.Catalog catalog;
    static ModernNativeActorWorldProtocol.Identity identity;
    static final NativeWireCapture capture=new NativeWireCapture();
    static final String ZERO="0".repeat(64);
    static final int CAPS=ModernSessionProtocol.CAP_EQUIPMENT|ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;
    static final class Client implements AutoCloseable {
        final long session=capture.session();String phase="main";final SSLSocket socket;final ModernSessionBoundary.Negotiated peer;final ModernRooms.Scope scope;int id=1;
        Client(ModernServer server,SSLContext context,int source)throws Exception {
            scope=new ModernRooms.Scope(source,NativeActorSqlProbeMain.objectId,Long.MIN_VALUE,0);int port=server.address().getPort();var raw=new Socket();raw.connect(new InetSocketAddress("127.0.0.1",port),2000);
            socket=(SSLSocket)context.getSocketFactory().createSocket(raw,"localhost",port,true);var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.setSoTimeout(5000);socket.startHandshake();
            send(ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(new ModernSessionProtocol.Hello(1,47,47,CAPS,CAPS,1),new ModernSessionExtensions.Offer(1792,1792))));
            var response=ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));require(response instanceof ModernSessionProtocol.ExtendedAck,"explicit Native extension ACK");var ack=(ModernSessionProtocol.ExtendedAck)response;require(ack.extensions()==1792&&ack.base().capabilities()==CAPS,"selected capabilities unchanged");peer=new ModernSessionBoundary.Negotiated(1,47,1,CAPS,1792);
        }
        int next(){return ++id;}
        void reject(ModernApplicationProtocol.Message message,String action)throws Exception{byte[] bytes=ModernApplicationProtocol.encode(message,peer);send(bytes.clone());require(socket.getInputStream().read()==-1,"actual TLS rejection "+action);capture.append(session,phase,scope,action,bytes,null,"EOF_REJECTED");}
        void send(byte[] bytes)throws Exception{try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);}}
        byte[] exchange(ModernApplicationProtocol.Message message,int kind)throws Exception {
            byte[] sent=ModernApplicationProtocol.encode(message,peer);send(sent.clone());var header=socket.getInputStream().readNBytes(16);require(header.length==16,"complete TLS header");var b=ByteBuffer.wrap(header);require(b.getInt()==0x4c544d31,"magic");int size=b.getInt();require(size>=16&&size<=ModernNativeActorWorldProtocol.maximumResult(peer),"bounded TLS frame");require(b.getInt()==message.requestId(),"echo request ID");int actual=Short.toUnsignedInt(b.getShort());require(kind<0||actual==kind,"selected kind");require(b.getShort()==0,"reserved");var body=socket.getInputStream().readNBytes(size-16);require(body.length==size-16,"complete TLS body");byte[] frame=ByteBuffer.allocate(size).put(header).put(body).array();if(message instanceof ModernApplicationProtocol.NativeActorWorldRequest r)capture.append(session,phase,scope,r.request().catalogRequest().nativeRequest().world().action().name(),sent,frame,"COMPLETE_EXCHANGE");
            else if(message instanceof ModernApplicationProtocol.CharacterDataRequest||message instanceof ModernApplicationProtocol.EquipmentRequest||message instanceof ModernApplicationProtocol.BattleReadRequest||message instanceof ModernApplicationProtocol.BattleOperationRequest)capture.append(session,phase,scope,"PUBLICATION_OR_OPERATION",sent,frame,"COMPLETE_EXCHANGE");return frame;
        }
        ModernApplicationProtocol.Message old(ModernApplicationProtocol.Message message)throws Exception{return ModernApplicationProtocol.decode(exchange(message,-1),47);}
        void login()throws Exception {
            try(var auth=new ModernApplicationProtocol.Auth(next(),"3333","3333".toCharArray())){require(((ModernApplicationProtocol.AuthResult)old(auth)).status()==0,"authenticated");}
            var roster=(ModernApplicationProtocol.CharacterResult)old(new ModernApplicationProtocol.CharacterRequest(next(),scope.sourceId(),scope.epoch()));
            require(roster.status()==0&&roster.characters().stream().anyMatch(row->row.objectId()==scope.objectId()&&row.name().equals("3332")),"actual SQL scoped roster");
            require(((ModernApplicationProtocol.NavigationResult)old(new ModernApplicationProtocol.CharacterSelection(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase()==1,"selected character");
            require(((ModernApplicationProtocol.NavigationResult)old(new ModernApplicationProtocol.ChannelEnter(next(),scope.sourceId(),scope.objectId(),scope.epoch(),scope.channelId()))).phase()==2,"channel enter");
        }
        ModernRooms.RoomSnapshot room(ModernRooms.Action action,long room,long revision,int value,ModernRooms.Draft draft)throws Exception {
            var result=(ModernApplicationProtocol.RoomResult)old(new ModernApplicationProtocol.RoomRequest(next(),new ModernRooms.Command(scope,action,room,revision,value,draft,"")));require(result.result().status()==ModernRooms.Status.OK,"room command "+action);return result.result().view().joined();
        }
        byte[] world(ModernBattleWorldProtocol.Action action,ModernTargetHistory.World world,long room,long revision,SourceAttachment source)throws Exception {
            var q=new ModernBattleWorldProtocol.Request(scope,action,world==null?room:world.roomId(),revision,world==null?0:world.matchId(),world==null?0:world.generation(),action==ModernBattleWorldProtocol.Action.LOADED?source.manifest().digest():ZERO);
            var nativeQ=new Request(next(),2,source==null?ZERO:source.plan().digest(),action==ModernBattleWorldProtocol.Action.LOADED?loadProof(source,q):ZERO,q);
            var envelope=NativeActorIdentityProbeMain.envelope(nativeQ,identity,source);
            byte[] full=exchange(new ModernApplicationProtocol.NativeActorWorldRequest(envelope),93);
            require(ByteBuffer.wrap(full).getInt(84)==952&&Arrays.equals(identity.bytes(),Arrays.copyOfRange(full,92,1044)),"SQL TLS actor dependency identity");
            require(ByteBuffer.wrap(full).getInt(1128)==168&&Arrays.equals(catalog.bytes(),Arrays.copyOfRange(full,1136,1304)),"SQL TLS nested catalog identity");
            return Arrays.copyOfRange(full,1304,full.length);
        }
        public void close()throws Exception{socket.close();}
    }
    static ModernEquipmentAuthority.Result gear(Client c,ModernEquipmentAuthority.Request request)throws Exception {
        return ((ModernApplicationProtocol.EquipmentResult)c.old(new ModernApplicationProtocol.EquipmentRequest(c.next(),request))).result();
    }
    static void equipmentChecks(Client a,Client b,Map<String,Object> out)throws Exception {
        var zero=ModernEquipmentAuthority.ZERO;
        var ra=gear(a,new ModernEquipmentAuthority.Request(7,a.scope.objectId(),a.scope.epoch(),0,false,0,0,zero));
        var rb=gear(b,new ModernEquipmentAuthority.Request(42,b.scope.objectId(),b.scope.epoch(),0,false,0,0,zero));
        out.put("equipment_read_status",List.of(ra.status(),rb.status()));
        if(ra.status()!=0||rb.status()!=0){out.put("equipment_gap","Exact locked legacy SQL equipment qualification unavailable; no fabricated/stripped fallback");return;}
        var sa=ra.snapshot();var sb=rb.snapshot();require(!sa.viewToken().equals(sb.viewToken()),"gear owner binding distinguishes same object source");
        var item=sa.equipped().stream().filter(ModernEquipmentAuthority.Equipped::equipped).findFirst().orElseThrow();
        var change=gear(a,new ModernEquipmentAuthority.Request(7,a.scope.objectId(),a.scope.epoch(),1,false,item.objectId(),sa.revision(),sa.viewToken()));require(change.status()==0&&change.changed(),"actual source equipment durable CAS");
        var stale=gear(a,new ModernEquipmentAuthority.Request(7,a.scope.objectId(),a.scope.epoch(),1,true,item.objectId(),sa.revision(),sa.viewToken()));require(stale.status()==4,"equipment stale CAS refused");
        var other=gear(b,new ModernEquipmentAuthority.Request(42,b.scope.objectId(),b.scope.epoch(),1,false,item.objectId(),sa.revision(),sa.viewToken()));require(other.status()==4||other.status()==5,"cross-source gear CAS refused");
        var again=gear(b,new ModernEquipmentAuthority.Request(42,b.scope.objectId(),b.scope.epoch(),0,false,0,0,zero));require(again.snapshot().revision()==sb.revision()&&again.snapshot().equipped().equals(sb.equipped()),"other source equipment unchanged");
        out.put("equipment_source_durable_CAS",true);out.put("equipment_expected_override_revision",change.snapshot().revision());out.put("equipment_override_item",item.objectId());
    }
    record Geometry(ModernTargetHistory.World world,long revision,int phase,List<ModernWorldMap.Cell> positions,
                    long firstEntity,int firstX,int firstY,boolean firstLoaded) {}
    static ByteBuffer section(byte[] frame,int offset,int wanted) {
        var b=ByteBuffer.wrap(frame,offset,frame.length-offset).slice();require(b.getInt()==ModernBattleWire.MAGIC,"canonical DATA magic");int schema=Short.toUnsignedInt(b.getShort());require(schema>=1&&schema<=6&&b.getShort()==0,"selected schema");require(b.getInt()==frame.length-offset,"DATA length");int count=Short.toUnsignedInt(b.getShort());require(count>=12&&count<=14&&b.getShort()==0,"section count");ByteBuffer found=null;
        for(int i=1;i<=count;i++){require(Short.toUnsignedInt(b.getShort())==i&&b.getShort()==0,"ordered canonical sections");int length=b.getInt();require(length>=0&&length<=b.remaining(),"section bound");var value=b.slice();value.limit(length);if(i==wanted)found=value;b.position(b.position()+length);}require(!b.hasRemaining()&&found!=null,"exact section consumption");return found;
    }
    static Geometry data(byte[] frame,int offset,SourceAttachment source) {
        var manifest=section(frame,offset,3);require(manifest.getInt()==source.manifest().mapId(),"DATA map ID");byte[] hash=new byte[32];manifest.get(hash);require(HexFormat.of().formatHex(hash).equals(source.manifest().digest()),"DATA canonical SHA");require(manifest.getInt()==source.manifest().sectors().size(),"DATA sectors");
        for(var pin:source.manifest().sectors()){require(manifest.getInt()==pin.key().x()&&manifest.getInt()==pin.key().y()&&manifest.getInt()==pin.byteLength(),"full ordered DATA sector fields");manifest.get(hash);require(HexFormat.of().formatHex(hash).equals(pin.rawSha256()),"DATA raw sector SHA");}require(!manifest.hasRemaining(),"exact DATA manifest");
        var w=section(frame,offset,1);require(w.get(38)==ModernMatchDescriptor.Ruleset.SCENE_ONLY.ordinal()&&w.getInt(39)==0,"actual DATA descriptor SCENE_ONLY/features0");var identity=new ModernTargetHistory.World(w.getLong(),w.getLong(),w.getLong());long revision=w.getLong();int phase=Byte.toUnsignedInt(w.get());var actors=section(frame,offset,4);require(actors.getInt()==2,"two genuine source scoped actors");var positions=new ArrayList<ModernWorldMap.Cell>();long entity=0;int x=0,y=0;boolean loaded=false;
        for(int i=0;i<2;i++){long id=actors.getLong();int ax=actors.getInt(),ay=actors.getInt();positions.add(new ModernWorldMap.Cell(ax,ay));actors.getInt();byte state=actors.get();require(state==0||state==1,"Loaded flag");if(i==0){entity=id;x=ax;y=ay;loaded=state==1;}actors.getLong();actors.getLong();require(actors.get()==0&&actors.get()==0,"geometry-only actor has no combat vitals/resources");actors.position(actors.position()+32);require(actors.getInt()==0&&actors.get()==0,"geometry-only spell lanes");actors.getLong();require(actors.get()==0&&actors.get()==0,"geometry-only buff/regen");}require(!actors.hasRemaining(),"exact actor consumption");return new Geometry(identity,revision,phase,List.copyOf(positions),entity,x,y,loaded);
    }
    static int metadata(byte[] bytes){return 92+ByteBuffer.wrap(bytes).getInt(84)+96;}
    static Geometry nativeData(byte[] bytes,SourceAttachment source,byte[] attachment) {
        var b=ByteBuffer.wrap(bytes);require(b.getInt(84)==attachment.length&&b.getInt(88)==0,"attachment length");require(Arrays.equals(attachment,Arrays.copyOfRange(bytes,92,92+attachment.length)),"stable full SourceAttachment");int at=metadata(bytes);require(bytes[at]==0&&bytes[at+2]==2,"canonical DATA success");int length=b.getInt(at+52);require(length==bytes.length-at-56,"canonical DATA advertised length");var value=data(bytes,at+56,source);require(value.world().roomId()==b.getLong(at+4)&&value.world().matchId()==b.getLong(at+12)&&value.world().generation()==b.getLong(at+20),"DATA lifecycle world identity");return value;
    }

    static void negativeCatalogTls(ModernServer server,SSLContext context,ModernWorldService worlds,SourceAttachment source,Map<String,Object> out)throws Exception {
        var rejected=new ArrayList<String>();
        for(String test:List.of("wrong_catalog","old_v1_outer_proof","cross_source_scope","source7_proof_for_source42","old88_bypass","old90_bypass","old_catalog_actor_proof")) {
            NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==0&&worlds.isQuiescent());
            try(var a=new Client(server,context,7);var b=new Client(server,context,42)) {
                a.phase="negative_"+test;b.phase="negative_"+test;a.login();b.login();
                var room=a.room(ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("catalog SQL negative","",2,1,564,0));room=b.room(ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null);room=a.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);room=b.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);
                a.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);b.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);
                var world=decodeResultWithoutData(a.world(ModernBattleWorldProtocol.Action.PREPARE,null,room.summary().id(),room.summary().revision(),source),a.peer).reply().world();
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);boolean ready=false;
                while(System.nanoTime()<deadline){var bytes=a.world(ModernBattleWorldProtocol.Action.READ,world,0,0,source);if(bytes[metadata(bytes)+2]==2){ready=true;break;}Thread.sleep(5);}require(ready,"negative actual SQL world ready");
                var own=new ModernBattleWorldProtocol.Request(a.scope,ModernBattleWorldProtocol.Action.LOADED,world.roomId(),0,world.matchId(),world.generation(),source.manifest().digest());
                var nativeQ=new Request(a.next(),2,source.plan().digest(),loadProof(source,own),own);
                var envelope=NativeCatalogProbeMain.envelope(nativeQ,catalog,source);Client attacked=a;
                if(test.equals("wrong_catalog"))envelope=new ModernNativeCatalogWorldProtocol.Request("1".repeat(64),envelope.catalogProof(),nativeQ);
                if(test.equals("old_v1_outer_proof"))envelope=new ModernNativeCatalogWorldProtocol.Request(catalog.digest(),nativeQ.loadProof(),nativeQ);
                if(test.equals("cross_source_scope")){attacked=b;nativeQ=new Request(b.next(),2,source.plan().digest(),loadProof(source,own),own);envelope=NativeCatalogProbeMain.envelope(nativeQ,catalog,source);}
                if(test.equals("source7_proof_for_source42")){attacked=b;var other=new ModernBattleWorldProtocol.Request(b.scope,ModernBattleWorldProtocol.Action.LOADED,world.roomId(),0,world.matchId(),world.generation(),source.manifest().digest());var otherQ=new Request(b.next(),2,source.plan().digest(),loadProof(source,other),other);envelope=new ModernNativeCatalogWorldProtocol.Request(catalog.digest(),envelope.catalogProof(),otherQ);}
                var actorQ=new ModernNativeActorWorldProtocol.Request(identity.digest(),ModernNativeActorWorldProtocol.loadProof(identity,source,envelope.nativeRequest().world()),envelope);
                if(test.equals("old_catalog_actor_proof"))actorQ=new ModernNativeActorWorldProtocol.Request(identity.digest(),envelope.catalogProof(),envelope);
                byte[] bytes=test.equals("old88_bypass")?ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCanonicalWorldRequest(nativeQ),attacked.peer):test.equals("old90_bypass")?ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCatalogWorldRequest(envelope),attacked.peer):ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeActorWorldRequest(actorQ),attacked.peer);
                attacked.send(bytes.clone());require(attacked.socket.getInputStream().read()==-1,"actual TLS refuses "+test);capture.append(attacked.session,attacked.phase,attacked.scope,"LOADED_OR_BYPASS",bytes,null,"EOF_REJECTED");rejected.add(test);
                var survivor=attacked==a?b:a;survivor.world(ModernBattleWorldProtocol.Action.LEAVE,world,0,0,source);
            }
        }
        NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==0&&worlds.isQuiescent());out.put("catalog_TLS_negatives",rejected);
    }
    static void run(String[] args,ModernServerConfig config,Map<Integer,ModernJdbcBackend> backends,Map<String,Object> result)throws Exception {
        var output=Path.of(args[1]);var certificateDir=output.resolve("configuration-tls");
        identity=new ModernNativeActorWorldProtocol.Identity(Files.readAllBytes(output.resolve("resources/actor-identity.bin")));ModernNativeActorWorldProtocol.verifyMembers(output.resolve("resources"),identity);catalog=NativeCatalogProbeMain.catalog(output.resolve("catalog"));var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(Path.of(args[0])),()->false);var plan=NativeCanonicalServiceProbeMain.plan(candidate);var source=new SourceAttachment(plan,candidate.binding().map().manifest());var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(candidate.binding().key()));
        var worlds=ModernWorldService.withNativeActorCanonicalResources(rooms,Map.of(candidate.binding().key(),new ModernWorldService.Mapping(candidate.binding(),candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100))),new ModernWorldService.Limits(1,1,1,4000,5,new ModernBattleEngine.Limits(15000,0,45000)),Map.of(candidate.binding().key(),plan),identity);
        var authSources=new LinkedHashMap<Integer,ModernAccountAuthentication.SourceBinding>();
        var dataSources=new LinkedHashMap<Integer,ModernCharacterDataRepository.Source>();
        var gearSources=new LinkedHashMap<Integer,ModernEquipmentRepository.Source>();
        for(var configured:config.sources()){var dao=backends.get(configured.id());authSources.put(configured.id(),ModernAccountAuthentication.SourceBinding.direct(configured.name(),dao::authenticate));dataSources.put(configured.id(),new ModernCharacterDataRepository.Source(dao,configured.catalogDatabase()));gearSources.put(configured.id(),new ModernEquipmentRepository.Source(configured,dao));}
        var store=new ModernEquipmentOverrideStore(config.equipmentStoreDirectory());
        var provider=ModernEquipmentRepository.jdbc(gearSources);var equipment=new ModernEquipmentAuthority(provider,store);
        var verifier=ModernAccountAuthentication.repositories(authSources,ignored->false);var server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(certificateDir,false),new ModernServer.Limits(16,16,600,16,2,4,2000,2000,5000,10000),verifier,ModernJdbcBackend.characters(backends),presence,ModernCharacterDataRepository.jdbc(dataSources),equipment,store,rooms,worlds,47);
        require(worlds.nativeCatalogEnabled(),"explicit catalog factory active");result.put("catalog_factory","withNativeActorCanonicalResources");result.put("catalog_digest",catalog.digest());result.put("catalog_bytes",HexFormat.of().formatHex(catalog.bytes()));
        result.put("owned_TLS_listener",Map.of("host",server.address().getAddress().getHostAddress(),"port",server.address().getPort(),"owner_pid",ProcessHandle.current().pid()));NativeActorSqlProbeMain.write(output,result);
        var clientContext=ModernTlsTestMaterial.clientContext(certificateDir);var vectors=new LinkedHashMap<String,Object>();boolean retired=false;
        try {
            try(var unauthenticated=new Client(server,clientContext,7)) {unauthenticated.phase="unauthenticated";var q=new ModernBattleWorldProtocol.Request(unauthenticated.scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,ZERO);unauthenticated.send(ModernNativeActorWorldProtocol.encodeRequest(NativeActorIdentityProbeMain.envelope(new Request(2,2,ZERO,ZERO,q),identity,null),unauthenticated.peer));require(unauthenticated.socket.getInputStream().read()==-1,"unauthenticated Native request refused");}
            try(var a=new Client(server,clientContext,7);var b=new Client(server,clientContext,42)) {
                a.login();b.login();
                var charA=(ModernApplicationProtocol.CharacterDataResult)a.old(new ModernApplicationProtocol.CharacterDataRequest(a.next(),7,a.scope.objectId(),a.scope.epoch()));
                var charB=(ModernApplicationProtocol.CharacterDataResult)b.old(new ModernApplicationProtocol.CharacterDataRequest(b.next(),42,b.scope.objectId(),b.scope.epoch()));
                require(charA.status()==0&&charB.status()==0,"actual SQL character publication");
                require(charA.snapshot().sourceId()==7&&charB.snapshot().sourceId()==42&&charA.snapshot().character().objectId()==charB.snapshot().character().objectId(),"same object different sources");
                result.put("character_inventory_counts",List.of(charA.snapshot().inventory().size(),charB.snapshot().inventory().size()));
                result.put("character_class_sex",List.of(List.of(charA.snapshot().character().characterClass(),charA.snapshot().character().sex()),List.of(charB.snapshot().character().characterClass(),charB.snapshot().character().sex())));result.put("DATA_descriptor",Map.of("ruleset","SCENE_ONLY","features",0,"capabilities",CAPS,"extensions",1792,"combat_loadout",false));
                equipmentChecks(a,b,result);
                var room=a.room(ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("native TLS","",2,1,564,0));room=b.room(ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null);room=a.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);room=b.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);
                var discovery=a.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);var discovered=decodeResultWithoutData(discovery,a.peer);require(source.equals(discovered.source()),"actual TLS pinned discovery");byte[] attachment=Arrays.copyOfRange(discovery,92,92+ByteBuffer.wrap(discovery).getInt(84));b.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);
                var prepared=a.world(ModernBattleWorldProtocol.Action.PREPARE,null,room.summary().id(),room.summary().revision(),source);var before=decodeResultWithoutData(prepared,a.peer);require(before.reply().state()==ModernBattleWorldProtocol.State.BEFORE_ENGINE,"real before-engine lifecycle");var world=before.reply().world();
                byte[] loading=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(System.nanoTime()<deadline){loading=a.world(ModernBattleWorldProtocol.Action.READ,world,0,0,source);if(loading[metadata(loading)+2]==2)break;Thread.sleep(5);}var initial=nativeData(loading,source,attachment);require(!initial.firstLoaded(),"actual DATA begins unloaded");
                var one=a.world(ModernBattleWorldProtocol.Action.LOADED,world,0,0,source);require(nativeData(one,source,attachment).firstLoaded(),"actual LOADED publishes self Loaded");var both=b.world(ModernBattleWorldProtocol.Action.LOADED,world,0,0,source);var active=nativeData(both,source,attachment);require(active.phase()==ModernBattleEngine.Phase.ACTIVE.ordinal(),"both authenticated owners loaded");
                var read=a.exchange(new ModernApplicationProtocol.BattleReadRequest(a.next(),new ModernBattleReadProtocol.Request(a.scope,world)),61);require(read[64]==ModernBattleReadProtocol.State.DATA.ordinal(),"proof-gated 60 DATA");var current=data(read,72,source);
                var personal=section(read,72,11);var ownerScope=new ModernRooms.Scope(personal.getInt(),personal.getLong(),personal.getLong(),personal.getInt());long ownerEntity=personal.getLong();require(personal.get()==0,"SCENE_ONLY PERSONAL has no combat loadout equipment");result.put("PERSONAL_equipment_presence",0);require(ownerScope.equals(a.scope)&&ownerEntity==current.firstEntity(),"MOVE guard source/object/epoch/channel/entity are source7 READ61 PERSONAL");
                result.put("MOVE_guard_READ61_PERSONAL",Map.of("source_id",ownerScope.sourceId(),"object_id",ownerScope.objectId(),"epoch",ownerScope.epoch(),"channel",ownerScope.channelId(),"entity",ownerEntity,"incarnation",1,"world_revision",current.revision()));
                int heading=-1;int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};for(int h=0;h<8;h++)if(candidate.binding().map().canStep(new ModernWorldMap.Cell(current.firstX(),current.firstY()),new ModernWorldMap.Cell(current.firstX()+dx[h],current.firstY()+dy[h]))&&!current.positions().contains(new ModernWorldMap.Cell(current.firstX()+dx[h],current.firstY()+dy[h]))){heading=h;break;}require(heading>=0,"wire-derived move cell");
                int id=a.next();var command=new ModernApplicationProtocol.WorldRequest(id,new ModernWorldProtocol.Request(a.scope,ModernWorldProtocol.Action.MOVE,heading,0,0,world.matchId(),world.generation(),1,current.firstX(),current.firstY(),ZERO));var proof=new ModernBattleCommandLife.Proof(a.scope,world,current.firstEntity(),1,current.revision());var staleProof=new ModernBattleCommandLife.Proof(a.scope,world,current.firstEntity(),2,current.revision());
                var stale=a.exchange(new ModernApplicationProtocol.BattleOperationRequest(id,new ModernBattleActionProtocol.Request(staleProof,ModernBattleActionProtocol.NO_TARGET,command)),65);
                int staleMeta=116+ByteBuffer.wrap(stale).getInt(112);require(stale[staleMeta]==ModernBattleCommandLife.Rejection.STALE_LIFE.ordinal()&&stale[staleMeta+1]==ModernBattleOperationProtocol.Disposition.REJECTED.ordinal(),"stale incarnation refused before MOVE");
                id=a.next();command=new ModernApplicationProtocol.WorldRequest(id,new ModernWorldProtocol.Request(a.scope,ModernWorldProtocol.Action.MOVE,heading,0,0,world.matchId(),world.generation(),1,current.firstX(),current.firstY(),ZERO));
                var operation=a.exchange(new ModernApplicationProtocol.BattleOperationRequest(id,new ModernBattleActionProtocol.Request(proof,ModernBattleActionProtocol.NO_TARGET,command)),65);int meta=116+ByteBuffer.wrap(operation).getInt(112);require(operation[meta]==0&&operation[meta+1]==ModernBattleOperationProtocol.Disposition.APPLIED.ordinal(),"source7 READ61-owned geometry incarnation MOVE applied");var moved=data(operation,meta+32,source);require(moved.firstX()==current.firstX()+dx[heading]&&moved.firstY()==current.firstY()+dy[heading],"MOVE output derives from source7 exact READ61 publication");
                vectors.put("discovery89_hex",HexFormat.of().formatHex(discovery));vectors.put("preparing89_hex",HexFormat.of().formatHex(prepared));vectors.put("loading89_hex",HexFormat.of().formatHex(loading));vectors.put("loaded89_hex",HexFormat.of().formatHex(one));vectors.put("active89_hex",HexFormat.of().formatHex(both));vectors.put("read61_source7_before_move_hex",HexFormat.of().formatHex(read));vectors.put("move65_source7_hex",HexFormat.of().formatHex(operation));vectors.put("stale_life65_source7_hex",HexFormat.of().formatHex(stale));
                require(decodeResultWithoutData(a.world(ModernBattleWorldProtocol.Action.LEAVE,world,0,0,source),a.peer).source().equals(source),"leave SourceAttachment retained");b.world(ModernBattleWorldProtocol.Action.LEAVE,world,0,0,source);
                NativeCanonicalServiceProbeMain.await(worlds::isQuiescent);
                a.phase="main_reprepare";b.phase="main_reprepare";room=a.room(ModernRooms.Action.READ,0,0,0,null);room=a.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);room=b.room(ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null);
                a.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);b.world(ModernBattleWorldProtocol.Action.READ,null,0,0,null);
                var nextPrepare=decodeResultWithoutData(a.world(ModernBattleWorldProtocol.Action.PREPARE,null,room.summary().id(),room.summary().revision(),source),a.peer);var nextWorld=nextPrepare.reply().world();require(nextWorld.matchId()>world.matchId()&&nextWorld.generation()>world.generation(),"new generation after reprepare");
                byte[] nextLoading=null;long readyDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(System.nanoTime()<readyDeadline){nextLoading=a.world(ModernBattleWorldProtocol.Action.READ,nextWorld,0,0,source);if(nextLoading[metadata(nextLoading)+2]==2)break;Thread.sleep(5);}require(!nativeData(nextLoading,source,attachment).firstLoaded(),"reprepare clears loaded authority");
                a.world(ModernBattleWorldProtocol.Action.LOADED,nextWorld,0,0,source);b.world(ModernBattleWorldProtocol.Action.LOADED,nextWorld,0,0,source);
                int ping=a.next();require(a.old(new ModernApplicationProtocol.Ping(ping)).equals(new ModernApplicationProtocol.Pong(ping)),"selected connection survives full lifecycle");
                var oldLoaded=new ModernBattleWorldProtocol.Request(a.scope,ModernBattleWorldProtocol.Action.LOADED,world.roomId(),0,world.matchId(),world.generation(),source.manifest().digest());
                a.reject(new ModernApplicationProtocol.NativeActorWorldRequest(NativeActorIdentityProbeMain.envelope(new Request(a.next(),2,source.plan().digest(),loadProof(source,oldLoaded),oldLoaded),identity,source)),"OLD_GENERATION_LOADED");
                int oldId=b.next();var oldCommand=new ModernApplicationProtocol.WorldRequest(oldId,new ModernWorldProtocol.Request(b.scope,ModernWorldProtocol.Action.MOVE,heading,0,0,world.matchId(),world.generation(),1,current.firstX(),current.firstY(),ZERO));var oldProof=new ModernBattleCommandLife.Proof(b.scope,world,2,1,current.revision());b.reject(new ModernApplicationProtocol.BattleOperationRequest(oldId,new ModernBattleActionProtocol.Request(oldProof,ModernBattleActionProtocol.NO_TARGET,oldCommand)),"OLD_GENERATION_OPERATION");
                result.put("SQL_flow_reprepare_old_LOADED_and_operation_refused",true);
                result.put("Native_scene_MOVE_service_verified",true);result.put("Native_scene_incarnation","geometry-only participant incarnation is 1 by domain rule; source7 owner/entity/world/revision derive from PERSONAL/WORLD in its READ61, never source42 active89");
            }
            NativeCanonicalServiceProbeMain.await(()->server.snapshot().sessions()==0);negativeCatalogTls(server,clientContext,worlds,source,result);require(worlds.matchCount()==0&&worlds.registrationCount()==0,"all TLS attachments retired");
        }finally{server.close();retired=server.awaitTermination(5000);worlds.close();require(worlds.awaitTermination(5000),"world workers retired");require(retired&&presence.ownerCount()==0&&rooms.ownerCount()==0&&rooms.roomCount()==0,"owned TLS/backend resources retired");try(var released=new java.net.ServerSocket(server.address().getPort(),1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("owned_TLS_listener_released",true);}}
        var reloadExecutor=new ModernBackendExecutor(1,1);
        try(var reopened=new ModernEquipmentOverrideStore(config.equipmentStoreDirectory());var session=reloadExecutor.openSession()) {
            session.call(5000,scope->{var stored=reopened.read(new TournamentPresence.CharacterKey(7,NativeActorSqlProbeMain.objectId),scope).orElseThrow();require(stored.revision()==((Number)result.get("equipment_expected_override_revision")).longValue(),"force-confirmed override persists after server/store reopen");require(stored.value().overrides().stream().anyMatch(item->item.itemObjectId()==((Number)result.get("equipment_override_item")).longValue()&&!item.equipped()),"persisted desired equipment matches ACK");require(reopened.read(new TournamentPresence.CharacterKey(42,NativeActorSqlProbeMain.objectId),scope).isEmpty(),"other source has no durable override");return null;});
            result.put("equipment_reopen_persistence_and_other_source_absent",true);
        }finally{reloadExecutor.close();require(reloadExecutor.awaitTermination(5000),"reopen worker retired");}
        capture.write(output,"actor-sql-wire");Files.writeString(output.resolve("actor-sql-tls-vectors.json"),NativeTerrainProbeMain.json(vectors));var report=new LinkedHashMap<String,Object>();report.put("state","PASSED_NATIVE_CATALOG_REAL_TLS_LOCKED_SQL_SUBSET");report.put("TLS",true);report.put("hostname_validated",true);report.put("AUTH_SELECTION_CHANNEL",true);report.put("source_same_object",true);report.put("opaque_high_bit_epoch",true);report.put("Native92_93_DATA",true);report.put("catalog_proof_consumed",true);report.put("append_only_capture",true);report.put("capture_records",capture.count());report.put("catalog_bytes_verified",168);report.put("catalog_digest",catalog.digest());report.put("actor_identity_digest",identity.digest());report.put("negative_actual_TLS_cases",result.get("catalog_TLS_negatives"));report.put("Native60_61",true);report.put("Native64_65",true);report.put("MOVE_service_verified",true);report.put("unauthenticated_request_refused",true);report.put("workers_and_listeners_retired",retired);report.put("SQL",true);report.put("Rust_consumer_verified",false);report.put("renderer_loaded_qualified",false);report.put("GUI",false);report.put("formal_player_enabled",false);report.put("test_certificate_directory",certificateDir.toString());Files.writeString(output.resolve("actor-sql-tls-probe.json"),NativeTerrainProbeMain.json(report));result.put("canonical_TLS",report);System.out.println("Actual Native canonical locked SQL TLS subset passed; owned resources retired");
    }
}



