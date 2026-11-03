package local.lito381.server;

import java.io.ByteArrayInputStream;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static local.lito381.server.NativeCanonicalServiceProbeMain.*;
import static local.lito381.server.ModernNativeCatalogWorldProtocol.*;

/** Catalog bytes/negotiation and actual service ledger. No renderer qualification. */
public final class NativeCatalogProbeMain {
    static final String ZERO="0".repeat(64);
    static int refused;
    interface Action {void run()throws Exception;}
    static void no(Action action)throws Exception {try{action.run();}catch(SecurityException|IllegalArgumentException|ModernSessionProtocol.ProtocolException|ModernApplicationProtocol.ProtocolException expected){refused++;return;}throw new AssertionError("catalog negative admitted");}
    static Catalog catalog(Path root)throws Exception {
        byte[] index=Files.readAllBytes(root.resolve("LTtext.idx")),pak=Files.readAllBytes(root.resolve("LTtext.pak")),decoded=Files.readAllBytes(root.resolve("decoded-LTlist.spr"));
        require(index.length==9888&&hash(index).equals("b8c0bb8c43affe59a90ebd2d26182269b6d60af388c80963eaaeedc8d8ae23c8"),"R429 index pin");
        require(pak.length==292553&&hash(pak).equals("517d853e3fe91b504953ff4b9b2eed682a3612d063f2d18e75e39fd5580f342d"),"R429 pak pin");
        require(decoded.length==39135&&hash(decoded).equals("f55779aeb5c3982d8b366068cdbe4003d802619074e54388f26ee87368f42603"),"R429 decoded member pin");
        return new Catalog(index.length,hash(index),pak.length,hash(pak),145671,39135,hash(Arrays.copyOfRange(pak,145671,145671+39135)),hash(decoded));
    }
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static Request envelope(ModernNativeCanonicalWorldProtocol.Request q,Catalog catalog,ModernNativeCanonicalWorldProtocol.SourceAttachment source){return new Request(source==null?ZERO:catalog.digest(),q.world().action()==ModernBattleWorldProtocol.Action.LOADED?loadProof(catalog,source,q.world()):ZERO,q);}
    static Result send(ModernWorldService service,Player player,Request request,ModernSessionBoundary.Negotiated peer,Map<String,Object> vectors,String name)throws Exception {
        var result=service.nativeCatalogWorld(player.owner(),request,peer);byte[] frame=encodeResult(result,peer);require(Short.toUnsignedInt(ByteBuffer.wrap(frame).getShort(12))==91,"catalog service result kind");vectors.put(name+"_request90_hex",HexFormat.of().formatHex(encodeRequest(request,peer)));vectors.put(name+"_result91_hex",HexFormat.of().formatHex(frame));return result;
    }
    public static void main(String[] args)throws Exception {
        var output=Path.of(args[1]);var catalog=catalog(output.resolve("catalog"));
        var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(Path.of(args[0])),()->false);var plan=plan(candidate);var source=new ModernNativeCanonicalWorldProtocol.SourceAttachment(plan,candidate.binding().map().manifest());
        int caps=ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;
        var peer=new ModernSessionBoundary.Negotiated(1,47,1,caps,768);var old=new ModernSessionBoundary.Negotiated(1,47,1,caps,256);ModernSessionExtensions.requireContext(peer);
        no(()->new ModernSessionExtensions.Offer(512,512));no(()->new ModernSessionExtensions.Offer(768,512));
        var scope=new ModernRooms.Scope(7,Long.MAX_VALUE,Long.MIN_VALUE,0);var p=new Player(null,scope);
        var discovery=envelope(request(10,p,ModernBattleWorldProtocol.Action.READ,null,null),catalog,null);
        byte[] valid=encodeRequest(discovery,peer);require(valid.length==264&&discovery.equals(decodeRequest(valid,peer)),"90 fixed roundtrip");
        require(new ModernApplicationProtocol.NativeCatalogWorldRequest(discovery).equals(ModernApplicationProtocol.decode(valid,peer)),"directional application dispatch");require(Arrays.equals(valid,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(valid),peer)),"preallocation bound");
        no(()->decodeRequest(valid,old));no(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCatalogWorldRequest(discovery),47));
        var negatives=new LinkedHashMap<String,Object>();
        for(int offset:new int[]{0,4,8,12,14,16,18,84,88,92,96,98,100,102}) {byte[] bad=valid.clone();bad[offset]^=1;final int at=offset;no(()->decodeRequest(bad,peer));negatives.put("malformed_request_offset_"+at,HexFormat.of().formatHex(bad));}
        no(()->decodeRequest(Arrays.copyOf(valid,263),peer));no(()->decodeRequest(Arrays.copyOf(valid,265),peer));
        var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(candidate.binding().key()));var players=new ArrayList<Player>();
        var mapping=new ModernWorldService.Mapping(candidate.binding(),candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100));
        var limits=new ModernWorldService.Limits(1,1,1,4000,5,new ModernBattleEngine.Limits(15000,0,45000));
        var service=ModernWorldService.withNativeCatalogCanonicalResources(rooms,Map.of(candidate.binding().key(),mapping),limits,Map.of(candidate.binding().key(),plan),catalog);
        require(ModernServer.extensionsFor(service).supported()==768,"catalog advertised only by catalog factory");var vectors=new LinkedHashMap<String,Object>();
        try {
            for(int sourceId:List.of(7,42)) {
                var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(sourceId,Long.MAX_VALUE),"catalog-source-"+sourceId,"catalog-"+sourceId,80,6,1);require(presence.select(owner,selection)==TournamentPresence.SelectStatus.ACCEPTED,"source selected");var player=new Player(owner,new ModernRooms.Scope(sourceId,Long.MAX_VALUE,Long.MIN_VALUE,0));players.add(player);rooms.enterLobby(owner,selection,Long.MIN_VALUE,0);
                service.register(owner,player.scope(),deadline->new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE,selection.characterName(),6,1,80),null,false,null,false,null,false,false,false,false,false,false,peer);
            }
            var a=players.get(0);var b=players.get(1);var created=rooms.execute(a.owner(),new ModernRooms.Command(a.scope(),ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("catalog","",2,1,564,0),""));var room=created.view().joined();require(rooms.execute(b.owner(),new ModernRooms.Command(b.scope(),ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null,"")).status()==ModernRooms.Status.OK,"joined");command(rooms,a,ModernRooms.Action.READY,1);command(rooms,b,ModernRooms.Action.READY,1);
            for(var player:players){var q=envelope(request(11,player,ModernBattleWorldProtocol.Action.READ,null,null),catalog,null);var r=send(service,player,q,peer,vectors,"discovery_"+player.scope().sourceId());require(catalog.equals(r.catalog())&&source.equals(r.nativeResult().source()),"catalog attached to known Native discovery");require(r.equals(decodeResultWithoutData(encodeResult(r,peer),peer)),"91 pre-engine roundtrip");}
            var summary=room(rooms,a).summary();var nativePrepare=new ModernNativeCanonicalWorldProtocol.Request(12,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,summary.id(),summary.revision(),0,0,ZERO));
            no(()->service.nativeCanonicalWorld(a.owner(),nativePrepare,peer));no(()->service.nativeCatalogWorld(a.owner(),new Request("1".repeat(64),ZERO,nativePrepare),peer));no(()->new Request(ZERO,ZERO,nativePrepare));
            var prepared=send(service,a,envelope(nativePrepare,catalog,source),peer,vectors,"prepare");var world=prepared.nativeResult().reply().world();var matched=request(13,a,ModernBattleWorldProtocol.Action.READ,world,source);
            await(()->service.nativeCatalogWorld(a.owner(),envelope(matched,catalog,source),peer).nativeResult().reply().state()==ModernBattleWorldProtocol.State.DATA);
            send(service,a,envelope(matched,catalog,source),peer,vectors,"loading");
            var loaded=request(14,a,ModernBattleWorldProtocol.Action.LOADED,world,source);var catalogLoaded=envelope(loaded,catalog,source);
            no(()->service.nativeCatalogWorld(b.owner(),catalogLoaded,peer));
            no(()->service.nativeCanonicalWorld(a.owner(),loaded,peer));no(()->service.nativeCatalogWorld(a.owner(),new Request(catalog.digest(),loaded.loadProof(),loaded),peer));no(()->new Request(catalog.digest(),ZERO,loaded));
            var wrong=new Catalog(catalog.indexBytes(),"2".repeat(64),catalog.pakBytes(),catalog.pakSha(),catalog.memberOffset(),catalog.memberBytes(),catalog.packedSha(),catalog.decodedSha());
            no(()->service.nativeCatalogWorld(a.owner(),envelope(loaded,wrong,source),peer));no(()->service.readBattle(a.owner(),new ModernBattleReadProtocol.Request(a.scope(),world)));
            byte[] missingPin=encodeRequest(catalogLoaded,peer);Arrays.fill(missingPin,20,52,(byte)0);no(()->decodeRequest(missingPin,peer));negatives.put("missing_catalog_identity_LOAD_request90",HexFormat.of().formatHex(missingPin));
            byte[] missingProof=encodeRequest(catalogLoaded,peer);Arrays.fill(missingProof,52,84,(byte)0);no(()->decodeRequest(missingProof,peer));negatives.put("missing_catalog_proof_LOAD_request90",HexFormat.of().formatHex(missingProof));
            byte[] preparedWire=encodeResult(prepared,peer);byte[] missingAttachment=ByteBuffer.allocate(preparedWire.length-168).put(preparedWire,0,92).put(preparedWire,260,preparedWire.length-260).array();ByteBuffer.wrap(missingAttachment).putInt(4,missingAttachment.length).putInt(84,0);no(()->decodeResultWithoutData(missingAttachment,peer));negatives.put("missing_catalog_known_source_result91",HexFormat.of().formatHex(missingAttachment));
            send(service,a,catalogLoaded,peer,vectors,"loaded_7");send(service,b,envelope(request(15,b,ModernBattleWorldProtocol.Action.LOADED,world,source),catalog,source),peer,vectors,"active_42");
            require(service.readBattle(a.owner(),new ModernBattleReadProtocol.Request(a.scope(),world)).state()==ModernBattleReadProtocol.State.DATA,"valid catalog proof gates canonical read");
            for(var player:players)send(service,player,envelope(request(16,player,ModernBattleWorldProtocol.Action.LEAVE,world,source),catalog,source),peer,vectors,"leave_"+player.scope().sourceId());await(service::isQuiescent);
            command(rooms,a,ModernRooms.Action.READY,1);command(rooms,b,ModernRooms.Action.READY,1);for(var player:players)service.nativeCatalogWorld(player.owner(),envelope(request(17,player,ModernBattleWorldProtocol.Action.READ,null,null),catalog,null),peer);
            summary=room(rooms,a).summary();var freshNative=new ModernNativeCanonicalWorldProtocol.Request(18,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,summary.id(),summary.revision(),0,0,ZERO));var next=service.nativeCatalogWorld(a.owner(),envelope(freshNative,catalog,source),peer).nativeResult().reply().world();require(next.generation()>world.generation(),"reprepare generation");
            no(()->service.nativeCatalogWorld(a.owner(),catalogLoaded,peer));var nextLoaded=request(19,a,ModernBattleWorldProtocol.Action.LOADED,next,source);no(()->service.nativeCatalogWorld(a.owner(),new Request(catalog.digest(),catalogLoaded.catalogProof(),nextLoaded),peer));
            negatives.put("old_v1_proof_in_catalog_LOAD_request90",HexFormat.of().formatHex(encodeRequest(new Request(catalog.digest(),loaded.loadProof(),loaded),peer)));
            negatives.put("wrong_catalog_pin_LOAD_request90",HexFormat.of().formatHex(encodeRequest(envelope(loaded,wrong,source),peer)));
            negatives.put("old_catalog_proof_new_generation_request90",HexFormat.of().formatHex(encodeRequest(new Request(catalog.digest(),catalogLoaded.catalogProof(),nextLoaded),peer)));
            for(var player:players)service.nativeCatalogWorld(player.owner(),envelope(request(20,player,ModernBattleWorldProtocol.Action.LEAVE,next,source),catalog,source),peer);await(service::isQuiescent);
        }finally{service.close();require(service.awaitTermination(5000),"catalog workers retired");for(var player:players){rooms.disconnect(player.owner());presence.disconnect(player.owner());}}
        Files.writeString(output.resolve("catalog-vectors.json"),NativeTerrainProbeMain.json(vectors));Files.writeString(output.resolve("catalog-negative-vectors.json"),NativeTerrainProbeMain.json(negatives));
        Files.writeString(output.resolve("catalog-probe.json"),NativeTerrainProbeMain.json(Map.of("state","PASSED_CATALOG_CODEC_AND_SERVICE_LEDGER","catalog_identity",catalog.digest(),"catalog_bytes_hex",HexFormat.of().formatHex(catalog.bytes()),"negative_admissions",refused,"required_extension",768,"old_v1_bypass_refused",true,"old_generation_proof_refused",true,"workers_retired",true,"renderer_loaded_qualified",false,"SQL",false)));
        System.out.println("Catalog codec and real service ledger passed; negatives="+refused);
    }
}

