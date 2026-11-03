package local.lito381.server;

import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import static local.lito381.server.ModernNativeCanonicalWorldProtocol.*;

/** Fixed Native564 pins; canonical DTO/codec probe only, not service/SQL/TLS/GUI admission. */
public final class NativeCanonicalCodecProbeMain {
    private static final String ZERO="0".repeat(64);
    interface Checked {void run() throws Exception;}
    static int rejected;
    static void rejects(Checked task)throws Exception {try{task.run();}catch(ModernApplicationProtocol.ProtocolException|IllegalArgumentException|SecurityException expected){rejected++;return;}throw new IllegalStateException("negative codec vector accepted");}
    public static void main(String[] args)throws Exception {
        String geometry="0de89778ac5b0642467cde6bc46e3f6e0f20e86ddb838eb14b7b50e8718009d6";
        var pins=List.of(new ModernWorldResourceProtocol.ArchivePin(1,1,33884,"EFD13705C355BAAE6E1C95C88E24FDCDB2948D17F8936167486F97C8C0DE330E"),
                new ModernWorldResourceProtocol.ArchivePin(2,1,148485306,"6E80F46CA19D32BFA53FB360D2398755F4BBBA7383A6B4CAD811BCB79F910D22"),
                new ModernWorldResourceProtocol.ArchivePin(3,1,13136,"73536646591E04BFB62B55320E075F0C6D5F8DC17FE29BC012C7925EED670BD8"),
                new ModernWorldResourceProtocol.ArchivePin(4,1,63004112,"7029D561D8EF1420128D318E827BAE07D552E5316839CDDB362D9223F2FB3962"));
        var plan=new ModernWorldResourceProtocol.Plan(2,564,geometry,pins);
        var manifest=new ModernWorldMap.Manifest(564,List.of(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(32767,32768),128028,"b68ee758f17448316c70ed5d49ad65eb6432af483f0591a5f8aa7987f9af6865")),geometry);
        var source=new SourceAttachment(plan,manifest);
        var selected=new ModernSessionBoundary.Negotiated(1,47,1,ModernSessionProtocol.KNOWN_CAPABILITIES,256);
        var plain=new ModernSessionBoundary.Negotiated(1,47,1,ModernSessionProtocol.KNOWN_CAPABILITIES,0);
        var scope=new ModernRooms.Scope(7,Long.MAX_VALUE,11,0);
        var vectors=new ArrayList<String>();
        var read=new Request(10,2,ZERO,ZERO,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,ZERO));
        var prepare=new Request(11,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.PREPARE,1,10,0,0,ZERO));
        var loadedBody=new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.LOADED,1,0,101,11,geometry);
        var loaded=new Request(12,2,plan.digest(),loadProof(source,loadedBody),loadedBody);
        var matchedRead=new Request(13,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.READ,1,0,101,11,ZERO));
        var leave=new Request(14,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.LEAVE,1,0,101,11,ZERO));
        for(var request:List.of(read,prepare,loaded,matchedRead,leave)){
            byte[] wire=encodeRequest(request,selected);if(wire.length!=180||!request.equals(decodeRequest(wire,selected)))throw new IllegalStateException("request roundtrip differs");
            var application=new ModernApplicationProtocol.NativeCanonicalWorldRequest(request);
            if(!java.util.Arrays.equals(wire,ModernApplicationProtocol.encode(application,selected))
                    ||!application.equals(ModernApplicationProtocol.decode(wire,selected))
                    ||!java.util.Arrays.equals(wire,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(wire),selected)))throw new IllegalStateException("selected application dispatch differs");
            vectors.add("{\"action\":\""+request.world().action()+"\",\"bytes\":180,\"hex\":\""+HexFormat.of().formatHex(wire)+"\"}");
        }
        var detached=new Result(read,null,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,null,null,null));
        byte[] detachedBytes=encodeResult(detached,selected);if(detachedBytes.length!=244||!detached.equals(decodeResultWithoutData(detachedBytes,selected)))throw new IllegalStateException("detached roundtrip differs");
        var preparing=new Result(prepare,source,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,new ModernTargetHistory.World(1,101,11),new ModernBattleLifecycle(1,ModernWorldProtocol.Phase.PREPARING,ModernWorldProtocol.AbortReason.NONE,null,null,ModernPetRaceProtocol.Snapshot.disabled(),false,false),null));
        byte[] preparingBytes=encodeResult(preparing,selected);if(preparingBytes.length!=584||!preparing.equals(decodeResultWithoutData(preparingBytes,selected)))throw new IllegalStateException("preparing roundtrip differs");
        authorize(source,loaded,selected);
        rejects(()->encodeRequest(prepare,plain));rejects(()->decodeRequest(encodeRequest(prepare,selected),plain));rejects(()->decodeResultWithoutData(preparingBytes,plain));
        rejects(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCanonicalWorldRequest(prepare),47));
        rejects(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCanonicalWorldResult(preparing),47));
        rejects(()->ModernApplicationProtocol.decode(encodeRequest(prepare,selected),47));
        rejects(()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(encodeRequest(prepare,selected)),plain));
        byte[] tooLargeRequest=java.util.Arrays.copyOf(encodeRequest(prepare,selected),16);ByteBuffer.wrap(tooLargeRequest).putInt(4,Integer.MAX_VALUE);
        rejects(()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(tooLargeRequest),selected));
        byte[] tooLargeResult=java.util.Arrays.copyOf(preparingBytes,16);ByteBuffer.wrap(tooLargeResult).putInt(4,maximumResult(selected)+1);
        rejects(()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(tooLargeResult),selected));
        var navigationOnly=new ModernSessionBoundary.Negotiated(1,47,1,ModernSessionExtensions.NATIVE_DEPENDENCIES,256);
        rejects(()->encodeRequest(read,navigationOnly));
        for(int offset:List.of(0,4,12,14,16,19,108,109)){
            byte[] bad=encodeRequest(prepare,selected);bad[offset]^=(byte)0xFF;rejects(()->decodeRequest(bad,selected));
        }
        byte[] zeroId=encodeRequest(prepare,selected);java.util.Arrays.fill(zeroId,8,12,(byte)0);rejects(()->decodeRequest(zeroId,selected));
        byte[] missing=encodeRequest(prepare,selected);java.util.Arrays.fill(missing,20,52,(byte)0);rejects(()->decodeRequest(missing,selected));
        byte[] wrong=encodeRequest(loaded,selected);wrong[52]^=1;var wrongDecoded=decodeRequest(wrong,selected);rejects(()->authorize(source,wrongDecoded,selected));
        var foreignScope=new ModernRooms.Scope(42,Long.MAX_VALUE,11,0);
        var foreign=new ModernBattleWorldProtocol.Request(foreignScope,ModernBattleWorldProtocol.Action.LOADED,1,0,101,11,geometry);
        rejects(()->authorize(source,new Request(15,2,plan.digest(),loaded.loadProof(),foreign),selected));
        var stale=new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.LOADED,1,0,101,12,geometry);
        rejects(()->authorize(source,new Request(16,2,plan.digest(),loaded.loadProof(),stale),selected));
        for(int offset:List.of(84,88,92,95,96,99,128,136,168,172,216,348)){
            byte[] bad=preparingBytes.clone();bad[offset]^=(byte)0xFF;rejects(()->decodeResultWithoutData(bad,selected));
        }
        rejects(()->decodeResultWithoutData(java.util.Arrays.copyOf(preparingBytes,preparingBytes.length+1),selected));
        var noLarge=new ModernSessionBoundary.Negotiated(1,47,1,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_LARGE_MAPS,256);
        var many=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<17;i++)many.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(32767,i),128028,manifest.sectors().get(0).rawSha256()));
        var largeSource=new SourceAttachment(plan,new ModernWorldMap.Manifest(564,many,geometry));
        var largeReply=new Result(prepare,largeSource,preparing.reply());
        byte[] largeWire=encodeResult(largeReply,selected);
        rejects(()->encodeResult(largeReply,noLarge));rejects(()->decodeResultWithoutData(largeWire,noLarge));
        rejects(()->authorize(largeSource,prepare,noLarge));
        var overBudget=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<5;i++)overBudget.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(32767,i),8*1024*1024,manifest.sectors().get(0).rawSha256()));
        rejects(()->new SourceAttachment(plan,new ModernWorldMap.Manifest(564,overBudget,geometry)));
        rejects(()->new SourceAttachment(plan,new ModernWorldMap.Manifest(564,List.of(manifest.sectors().get(0),manifest.sectors().get(0)),geometry)));
        var presence=new TournamentPresence();var owner=presence.connect();var other=presence.connect();
        var identity=new ModernTargetHistory.World(1,101,11);var ledger=new ModernNativeResourceLedger();
        ledger.rememberDiscovery(owner,scope,1,10,source,selected);
        rejects(()->ledger.admitPrepare(other,prepare,1,10,source,selected));
        rejects(()->ledger.admitPrepare(owner,prepare,1,11,source,selected));
        rejects(()->ledger.admitPrepare(owner,prepare,2,10,source,selected));
        var pendingPrepare=ledger.admitPrepare(owner,prepare,1,10,source,selected);
        ledger.retire();ledger.rememberDiscovery(owner,scope,1,10,source,selected);
        rejects(()->ledger.completePrepare(pendingPrepare,preparing.reply()));
        ledger.completePrepare(ledger.admitPrepare(owner,prepare,1,10,source,selected),preparing.reply());
        rejects(()->ledger.requireVerified(owner,scope,identity,source));
        rejects(()->ledger.admitLoaded(other,loaded,selected));
        rejects(()->ledger.admitLoaded(owner,new Request(15,2,plan.digest(),loaded.loadProof(),foreign),selected));
        var admission=ledger.admitLoaded(owner,loaded,selected);
        rejects(()->ledger.completeLoaded(admission,preparing.reply()));
        rejects(()->ledger.requireVerified(owner,scope,identity,source));
        ledger.retire();ledger.rememberDiscovery(owner,scope,1,10,source,selected);
        ledger.completePrepare(ledger.admitPrepare(owner,prepare,1,10,source,selected),preparing.reply());
        rejects(()->ledger.completeLoaded(admission,null));
        rejects(()->ledger.rememberDiscovery(owner,scope,1,11,source,selected));
        rejects(()->ledger.requireVerified(owner,scope,identity,source));
        ledger.retire();
        var opaqueScope=new ModernRooms.Scope(7,Long.MAX_VALUE,Long.MIN_VALUE,0);
        var opaqueBody=new ModernBattleWorldProtocol.Request(opaqueScope,ModernBattleWorldProtocol.Action.LOADED,1,0,101,11,geometry);
        var opaque=new Request(20,2,plan.digest(),loadProof(source,opaqueBody),opaqueBody);
        byte[] opaqueWire=encodeRequest(opaque,selected);if(!opaque.equals(decodeRequest(opaqueWire,selected))||ByteBuffer.wrap(opaqueWire).getLong(96)!=Long.MIN_VALUE)throw new IllegalStateException("opaque epoch bits changed");
        authorize(source,opaque,selected);
        var detachedWithSource=new Result(read,source,detached.reply());
        if(!detachedWithSource.equals(decodeResultWithoutData(encodeResult(detachedWithSource,selected),selected)))throw new IllegalStateException("detached known-source presence differs");
        rejects(()->new Result(prepare,null,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.BUSY,ModernWorldProtocol.Reason.CAPACITY,null,null,null)));
        rejects(()->new Result(new Request(30,2,"1".repeat(64),ZERO,prepare.world()),source,preparing.reply()));
        var leaveReply=new Result(leave,source,detached.reply());
        if(!leaveReply.equals(decodeResultWithoutData(encodeResult(leaveReply,selected),selected)))throw new IllegalStateException("leave source echo differs");
        var presenceVectors=new ArrayList<String>();
        for(var status:List.of(ModernWorldProtocol.Status.DENIED,ModernWorldProtocol.Status.BUSY,ModernWorldProtocol.Status.UNAVAILABLE)){
            var reason=status==ModernWorldProtocol.Status.DENIED?ModernWorldProtocol.Reason.NOT_READY:status==ModernWorldProtocol.Status.BUSY?ModernWorldProtocol.Reason.CAPACITY:ModernWorldProtocol.Reason.NO_BINDING;
            var reply=new Result(prepare,source,new ModernBattleWorldProtocol.Reply(status,reason,null,null,null));
            byte[] wire=encodeResult(reply,selected);if(!reply.equals(decodeResultWithoutData(wire,selected)))throw new IllegalStateException("failure presence differs");
            presenceVectors.add("{\"case\":\""+status+"\",\"source_present\":true,\"hex\":\""+HexFormat.of().formatHex(wire)+"\"}");
        }
        var aborted=new Result(matchedRead,source,new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,identity,new ModernBattleLifecycle(2,ModernWorldProtocol.Phase.ABORTED,ModernWorldProtocol.AbortReason.PREPARATION_FAILED,null,null,ModernPetRaceProtocol.Snapshot.disabled(),false,false),null));
        byte[] abortedBytes=encodeResult(aborted,selected);if(!aborted.equals(decodeResultWithoutData(abortedBytes,selected)))throw new IllegalStateException("aborted presence differs");
        presenceVectors.add("{\"case\":\"ABORTED\",\"source_present\":true,\"hex\":\""+HexFormat.of().formatHex(abortedBytes)+"\"}");
        presenceVectors.add("{\"case\":\"LEAVE\",\"source_present\":true,\"hex\":\""+HexFormat.of().formatHex(encodeResult(leaveReply,selected))+"\"}");
        presenceVectors.add("{\"case\":\"DETACHED_KNOWN_READ\",\"source_present\":true,\"hex\":\""+HexFormat.of().formatHex(encodeResult(detachedWithSource,selected))+"\"}");
        vectors.add("{\"action\":\"LOADED_OPAQUE_EPOCH\",\"bytes\":180,\"load_proof\":\""+opaque.loadProof()+"\",\"hex\":\""+HexFormat.of().formatHex(opaqueWire)+"\"}");
        Path output=Path.of(args[0]);Files.writeString(output.resolve("canonical-codec-vectors.json"),"{\"schema\":1,\"source_set_digest\":\""+plan.digest()+"\",\"load_proof\":\""+loaded.loadProof()+"\",\"requests\":["+String.join(",",vectors)+"],\"presence_vectors\":["+String.join(",",presenceVectors)+"],\"detached_hex\":\""+HexFormat.of().formatHex(detachedBytes)+"\",\"preparing_hex\":\""+HexFormat.of().formatHex(preparingBytes)+"\"}");
        Files.writeString(output.resolve("canonical-codec-probe.json"),"{\"state\":\"passed_codec_and_unverified_ledger_fences\",\"requests\":6,\"results\":8,\"negative_vectors\":"+rejected+",\"request_bytes\":180,\"result_min_bytes\":244,\"before_engine_bytes\":584,\"selected_max_result_bytes\":"+maximumResult(selected)+",\"opaque_epoch_high_bit\":true,\"pending_room_revision_fence\":true,\"pre_engine_large_map_and_total_bounds\":true,\"local_static_success_grants_mutation\":false,\"retired_admission_refused\":true,\"data_vector\":false,\"service_ledger_integrated\":false,\"tls\":false,\"gui\":false}");
        System.out.println("canonical codec request/result and "+rejected+" negative vectors passed; DATA/service ledger pending");
    }
}
