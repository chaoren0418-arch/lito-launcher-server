package local.lito381.server;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import static local.lito381.server.ModernWorldProtocol.*;

/** Targeted exact-artifact codec/negotiation/service gates; no live SQL, GUI provider or network. */
public final class NativeResourceWireFixtureMain {
    static final String ZERO="0".repeat(64);
    static final int CAPS=ModernSessionExtensions.NATIVE_DEPENDENCIES,EXT=ModernSessionExtensions.NATIVE_WORLD_RESOURCES_V1;
    static final ModernSessionBoundary.Negotiated PEER=new ModernSessionBoundary.Negotiated(1,47,1,CAPS,EXT);
    static final ModernSessionBoundary.Negotiated LEGACY=new ModernSessionBoundary.Negotiated(1,47,1,CAPS,0);
    final List<Object> vectors=new ArrayList<>(),checks=new ArrayList<>();int requestId=100;
    @FunctionalInterface interface Attempt {void run()throws Exception;}
    void reject(String name,byte[] bytes,String expectation,Attempt attempt)throws Exception {
        try{attempt.run();throw new AssertionError(name+" unexpectedly admitted");}catch(IOException|IllegalArgumentException|SecurityException expected){checks.add(Map.of("name",name,"rejected",true,"exception",expected.getClass().getSimpleName()));}
        if(bytes!=null)vectors.add(Map.of("name",name,"positive",false,"hex",HexFormat.of().formatHex(bytes),"expectation",expectation));
    }
    byte[] positive(String name,ModernWorldResourceProtocol.Request r)throws Exception {
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeWorldRequest(r),PEER);
        require(((ModernApplicationProtocol.NativeWorldRequest)ModernApplicationProtocol.decode(bytes,PEER)).request().equals(r),"request roundtrip");
        require(Arrays.equals(ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),PEER),bytes),"bounded request read");
        vectors.add(Map.of("name",name,"positive",true,"kind",86,"hex",HexFormat.of().formatHex(bytes)));return bytes;
    }
    void result(String name,ModernWorldResourceProtocol.Result r)throws Exception {
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeWorldResult(r),PEER);
        var decoded=((ModernApplicationProtocol.NativeWorldResult)ModernApplicationProtocol.decode(bytes,PEER)).result();
        require(decoded.equals(r),"result roundtrip: "+name);require(Arrays.equals(ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),PEER),bytes),"bounded result read");
        vectors.add(Map.of("name",name,"positive",true,"kind",87,"hex",HexFormat.of().formatHex(bytes)));
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("geometry candidate properties, Native archive plan properties, owned output directory required");
        var root=Path.of(args[2]).toRealPath();var test=new NativeResourceWireFixtureMain();var props=NativeWorldResourceCandidate.config(Path.of(args[0]));
        var candidate=NativeWorldResourceCandidate.load(props,()->false);var p=NativeWorldResourceCandidate.config(Path.of(args[1]));var archives=new ArrayList<ModernWorldResourceProtocol.ArchivePin>();
        for(int i=1;i<=4;i++)archives.add(new ModernWorldResourceProtocol.ArchivePin(i,1,Long.parseLong(p.getProperty("archive."+i+".bytes")),p.getProperty("archive."+i+".sha256")));
        var plan=new ModernWorldResourceProtocol.Plan(2,564,candidate.binding().map().digest(),archives);
        test.negotiation();test.codec(plan);var service=test.service(candidate,plan);
        var evidence=new LinkedHashMap<String,Object>();evidence.put("status","passed");evidence.put("scope","artifact-only exact HELLO/ACK, Native envelope, SourcePlan and authoritative navigation/service rejection checks; memory character rows; no TLS/Rust consumer/SQL/GUI acceptance");
        evidence.put("candidate_jar_sha256",NativeTerrainProbeMain.hash(Files.readAllBytes(Path.of(ModernWorldService.class.getProtectionDomain().getCodeSource().getLocation().toURI()))));
        evidence.put("resource_plan",plan);evidence.put("resource_set_digest",plan.digest());evidence.put("canonical_identity_hex",HexFormat.of().formatHex(ModernWorldResourceProtocol.identityBytes(plan)));
        evidence.put("checks",test.checks);evidence.put("service",service);evidence.put("positive_vectors",test.vectors.stream().filter(v->Boolean.TRUE.equals(((Map<?,?>)v).get("positive"))).count());evidence.put("negative_vectors",test.vectors.stream().filter(v->Boolean.FALSE.equals(((Map<?,?>)v).get("positive"))).count());evidence.put("mode_additions",0);evidence.put("gui_acceptance",false);
        Files.writeString(root.resolve("wire-fixture.json"),NativeTerrainProbeMain.json(evidence));Files.writeString(root.resolve("wire-hex-vectors.json"),NativeTerrainProbeMain.json(Map.of("wire_schema",1,"base_version",47,"extension_bit",EXT,"context",Map.of("capabilities",CAPS,"extensions",EXT,"numeric_schema",1),"vectors",test.vectors)));
        System.out.println("[native-resource-wire] targeted negotiation/codec/gates passed; vectors="+test.vectors.size()+"; workers stopped");
    }
    void negotiation()throws Exception {
        var policy=new ModernSessionBoundary.Policy(CAPS,CAPS,47);var offer=new ModernSessionExtensions.Offer(EXT,EXT);
        var hello=new ModernSessionProtocol.ExtendedHello(new ModernSessionProtocol.Hello(1,47,47,CAPS,CAPS,1),offer);
        var bytes=ModernSessionProtocol.encode(hello);var boundary=new ModernSessionBoundary(policy,offer);var ack=boundary.acceptHello(bytes);
        require(((ModernSessionProtocol.ExtendedAck)ModernSessionProtocol.decode(ack)).extensions()==EXT,"Native mutually selected");
        vectors.add(Map.of("name","native_extension_hello","positive",true,"kind",80,"hex",HexFormat.of().formatHex(bytes)));
        vectors.add(Map.of("name","native_extension_ack","positive",true,"kind",81,"hex",HexFormat.of().formatHex(ack)));
        var plain=ModernSessionProtocol.encode(hello.base());var required=new ModernSessionBoundary(policy,offer);reject("required_native_missing_plain_hello",plain,"required extension absent -> close",()->required.acceptHello(plain));require(required.state()==ModernSessionBoundary.State.CLOSED,"failed boundary closed");
        var unsupported=bytes.clone();ByteBuffer.wrap(unsupported).putInt(32,512).putInt(36,512);reject("unknown_extension",unsupported,"unknown extension bit -> close",()->new ModernSessionBoundary(policy,offer).acceptHello(unsupported));
        var older=bytes.clone();ByteBuffer.wrap(older).putShort(16,(short)46).putShort(18,(short)46);reject("native_extension_non_v47",older,"exact base version required",()->ModernSessionProtocol.decode(older));
        var deps=bytes.clone();ByteBuffer.wrap(deps).putInt(20,CAPS&~64).putInt(24,CAPS&~64);reject("native_missing_world_dependency",deps,"WORLD dependency absent",()->ModernSessionProtocol.decode(deps));
        var optional=new ModernSessionBoundary(policy,new ModernSessionExtensions.Offer(EXT,0));var legacyAck=optional.acceptHello(plain);require(ModernSessionProtocol.decode(legacyAck) instanceof ModernSessionProtocol.Ack,"plain Shared handshake keeps old ACK");
        checks.add(Map.of("name","plain_shared_hello_on_optional_native_listener","extensions",optional.negotiated().orElseThrow().extensions(),"plain_ack",true));
    }
    void codec(ModernWorldResourceProtocol.Plan plan)throws Exception {
        var scope=new ModernRooms.Scope(7,Long.MAX_VALUE,1,0);var read=new Request(scope,Action.READ,0,0,0,0,0,0,0,0,ZERO);
        var r=new ModernWorldResourceProtocol.Request(10,2,ZERO,read);var good=positive("native_bootstrap_read",r);
        var response=new ModernWorldResourceProtocol.Result(r,plan,new Reply(Status.OK,Reason.NONE,null));result("native_bootstrap_source_plan",response);
        var resultBytes=ModernWorldResourceProtocol.encodeResult(response,PEER);
        for(var patch:List.of(new int[]{16,2},new int[]{18,99},new int[]{14,1})) {
            var bad=good.clone();ByteBuffer.wrap(bad).putShort(patch[0],(short)patch[1]);reject("request_bad_u16_at_"+patch[0],bad,"reject unknown schema/namespace or reserved",()->ModernWorldResourceProtocol.decodeRequest(bad,PEER));
        }
        var id=good.clone();ByteBuffer.wrap(id).putInt(8,0);reject("zero_request_id",id,"reject zero ID",()->ModernWorldResourceProtocol.decodeRequest(id,PEER));
        var length=good.clone();ByteBuffer.wrap(length).putInt(4,155);reject("wrong_declared_length",length,"reject length",()->ModernWorldResourceProtocol.decodeRequest(length,PEER));
        var trailing=Arrays.copyOf(good,good.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);reject("trailing_request_byte",trailing,"exact156 bytes required",()->ModernWorldResourceProtocol.decodeRequest(trailing,PEER));
        reject("unnegotiated_native_decode",good,"extension must be selected",()->ModernWorldResourceProtocol.decodeRequest(good,LEGACY));
        reject("version_only_native_decode",good,"full selected context mandatory",()->ModernApplicationProtocol.decode(good,47));
        reject("version_only_native_encode",null,"full selected context mandatory",()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeWorldRequest(r),47));
        var deniedStream=new CountingInput(good);reject("unnegotiated_stream_header",good,"reject after16-byte header before body allocation/read",()->ModernApplicationProtocol.readFrame(deniedStream,LEGACY));require(deniedStream.read==16,"unnegotiated header budget");
        var huge=Arrays.copyOf(good,16);ByteBuffer.wrap(huge).putInt(4,Integer.MAX_VALUE);var hugeStream=new CountingInput(huge);reject("oversized_stream_header",huge,"reject after16-byte header",()->ModernApplicationProtocol.readFrame(hugeStream,PEER));require(hugeStream.read==16,"oversized header budget");
        for(var patch:List.of(new int[]{52,2},new int[]{54,99},new int[]{124,3},new int[]{126,1},new int[]{128,2},new int[]{130,9})) {
            var bad=resultBytes.clone();ByteBuffer.wrap(bad).putShort(patch[0],(short)patch[1]);reject("result_bad_u16_at_"+patch[0],bad,"reject SourcePlan schema/namespace/count/reserved/role/profile",()->ModernWorldResourceProtocol.decodeResult(bad,PEER));
        }
        var archive=resultBytes.clone();archive[140]^=1;reject("archive_pin_digest_mismatch",archive,"resource-set checksum mismatch",()->ModernWorldResourceProtocol.decodeResult(archive,PEER));
        var negativeSize=resultBytes.clone();ByteBuffer.wrap(negativeSize).putLong(132,-1L);reject("negative_archive_length",negativeSize,"bounded positive archive length",()->ModernWorldResourceProtocol.decodeResult(negativeSize,PEER));
        var prepare=new Request(scope,Action.PREPARE,0,1,1,0,0,0,0,0,ZERO);var prepareBytes=positive("native_prepare_with_resource_proof",new ModernWorldResourceProtocol.Request(11,2,plan.digest(),prepare));
        var missing=prepareBytes.clone();Arrays.fill(missing,20,52,(byte)0);reject("zero_prepare_resource_proof",missing,"zero allowed only unmatched READ",()->ModernWorldResourceProtocol.decodeRequest(missing,PEER));
        reject("forged_peer_without_dependencies",good,"extension declaration lacks selected WORLD capability",()->ModernWorldResourceProtocol.decodeRequest(good,new ModernSessionBoundary.Negotiated(1,47,1,3,EXT)));
        checks.add(Map.of("name","plan_digest_immutability","value",plan.digest(),"canonical_bytes",ModernWorldResourceProtocol.identityBytes(plan).length));
    }
    Object service(NativeWorldResourceCandidate.Candidate c,ModernWorldResourceProtocol.Plan plan)throws Exception {
        var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(c.binding().key()));var reads=new AtomicInteger();var players=new ArrayList<Player>();
        var service=ModernWorldService.withNativeResources(rooms,Map.of(c.binding().key(),new ModernWorldService.Mapping(c.binding(),c.anchor(),100_000_000)),new ModernWorldService.Limits(1,1,1,2_000,5,new ModernBattleEngine.Limits(15_000,0,45_000)),Map.of(c.binding().key(),plan));
        var output=new LinkedHashMap<String,Object>();try {
            for(int source:List.of(7,42)){var owner=presence.connect();var scope=new ModernRooms.Scope(source,Long.MAX_VALUE,1,0);var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,Long.MAX_VALUE),"source-"+source,"wire-"+source,80,6,1);require(presence.select(owner,selection)==TournamentPresence.SelectStatus.ACCEPTED,"selection");rooms.enterLobby(owner,selection,1,0);var p=new Player(owner,scope);players.add(p);service.register(owner,scope,deadline->{reads.incrementAndGet();return new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE,selection.characterName(),6,1,80);});}
            var first=players.get(0);var second=players.get(1);ok(rooms.execute(first.owner,new ModernRooms.Command(first.scope,ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("native-wire","",2,1,564,0),"")));var s=view(rooms,first).joined().summary();ok(rooms.execute(second.owner,new ModernRooms.Command(second.scope,ModernRooms.Action.JOIN,s.id(),s.revision(),0,null,"")));ready(rooms,players);
            var roomBefore=view(rooms,first);s=roomBefore.joined().summary();var preparation=new Request(first.scope,Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO);
            var valid=new ModernWorldResourceProtocol.Request(++requestId,2,plan.digest(),preparation);var validBytes=positive("service_native_prepare",valid);
            reject("legacy_prepare_native_binding",ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldRequest(90,preparation),47),"Native binding forbids legacy route before source reads",()->service.handle(first.owner,preparation));
            reject("unnegotiated_prepare_native_binding",validBytes,"reject before source reads",()->service.nativeWorld(first.owner,valid,LEGACY));
            var wrong=new ModernWorldResourceProtocol.Request(91,2,"1".repeat(64),preparation);reject("wrong_resource_set_before_prepare",ModernWorldResourceProtocol.encodeRequest(wrong,PEER),"reject before ticket/source reads",()->service.nativeWorld(first.owner,wrong,PEER));
            require(reads.get()==0&&roomBefore.equals(view(rooms,first))&&!rooms.isPreparing(first.owner),"rejections have no prepare side effects");
            var boot=service.nativeWorld(first.owner,envelope(first,new Request(first.scope,Action.READ,0,0,0,0,0,0,0,0,ZERO),ZERO),PEER);require(boot.reply().frame()==null,"bootstrap no match");
            var initial=service.nativeWorld(first.owner,valid,PEER);result("service_native_preparing",initial);await(()->frame(service,first,plan).phase()==Phase.LOADING);var loading=frame(service,first,plan);
            var loaded=new Request(first.scope,Action.LOADED,0,0,0,loading.matchId(),loading.generation(),0,0,0,plan.geometryDigest());var loadedNative=envelope(first,loaded,plan.digest());positive("native_loaded_both_pins",loadedNative);
            var wrongGeo=new Request(first.scope,Action.LOADED,0,0,0,loading.matchId(),loading.generation(),0,0,0,"2".repeat(64));var wrongLoaded=envelope(first,wrongGeo,plan.digest());reject("wrong_geometry_before_loaded",ModernWorldResourceProtocol.encodeRequest(wrongLoaded,PEER),"resource plan/canonical geometry mismatch",()->service.nativeWorld(first.owner,wrongLoaded,PEER));require(frame(service,first,plan).actors().stream().noneMatch(ModernBattleEngine.Actor::loaded),"invalid loaded changed no actors");
            service.nativeWorld(first.owner,loadedNative,PEER);service.nativeWorld(second.owner,envelope(second,new Request(second.scope,Action.LOADED,0,0,0,loading.matchId(),loading.generation(),0,0,0,plan.geometryDigest()),plan.digest()),PEER);await(()->frame(service,first,plan).phase()==Phase.ACTIVE);var active=frame(service,first,plan);result("service_native_active",service.nativeWorld(first.owner,envelope(first,new Request(first.scope,Action.READ,0,0,0,active.matchId(),active.generation(),0,0,0,ZERO),plan.digest()),PEER));
            reject("legacy_read_active_native_binding",null,"legacy READ cannot strip namespace",()->service.handle(first.owner,new Request(first.scope,Action.READ,0,0,0,active.matchId(),active.generation(),0,0,0,ZERO)));
            var leave=envelope(second,new Request(second.scope,Action.LEAVE,0,0,0,active.matchId(),active.generation(),0,0,0,ZERO),plan.digest());positive("native_leave_with_source_proof",leave);result("service_native_return",service.nativeWorld(second.owner,leave,PEER));var abort=frame(service,first,plan);require(abort.phase()==Phase.ABORTED,"remaining terminal");service.nativeWorld(first.owner,envelope(first,new Request(first.scope,Action.LEAVE,0,0,0,abort.matchId(),abort.generation(),0,0,0,ZERO),plan.digest()),PEER);await(service::isQuiescent);
            require(view(rooms,first).joined().summary().ready()==0&&!rooms.isPreparing(first.owner),"return room reset");output.put("source_scoped_same_object",true);output.put("source_reads",reads.get());output.put("pre_rejection_reads",0);output.put("room_reset",true);output.put("native_prepare_load_active_return",true);output.put("actual_binding",Map.of("mode",1,"logical_map",564,"physical_map",564,"namespace",2));
        }finally{service.close();require(service.awaitTermination(4_000),"workers retired");for(var p:players){rooms.disconnect(p.owner);presence.disconnect(p.owner);}}
        output.put("workers_retired",true);return output;
    }
    record Player(TournamentPresence.Owner owner,ModernRooms.Scope scope) {}
    ModernWorldResourceProtocol.Request envelope(Player p,Request r,String digest){return new ModernWorldResourceProtocol.Request(++requestId,2,digest,r);}
    Frame frame(ModernWorldService service,Player p,ModernWorldResourceProtocol.Plan plan){return service.nativeWorld(p.owner,envelope(p,new Request(p.scope,Action.READ,0,0,0,0,0,0,0,0,ZERO),plan.digest()),PEER).reply().frame();}
    static ModernRooms.View view(ModernRooms rooms,Player p){return rooms.execute(p.owner,new ModernRooms.Command(p.scope,ModernRooms.Action.READ,0,0,0,null,"")).view();}
    static void ready(ModernRooms rooms,List<Player> players){for(var p:players){var s=view(rooms,p).joined().summary();ok(rooms.execute(p.owner,new ModernRooms.Command(p.scope,ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}}
    static void ok(ModernRooms.Result r){require(r.status()==ModernRooms.Status.OK,"room "+r.reason());}
    static void await(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(!condition.getAsBoolean()){require(System.nanoTime()<end,"bounded wait");Thread.sleep(5);}}
    static void require(boolean pass,String detail){if(!pass)throw new AssertionError(detail);}
    static final class CountingInput extends ByteArrayInputStream {int read;CountingInput(byte[] b){super(b);}@Override public synchronized int read(){int r=super.read();if(r>=0)read++;return r;}@Override public synchronized int read(byte[] b,int o,int l){int n=super.read(b,o,l);if(n>0)read+=n;return n;}}
}
