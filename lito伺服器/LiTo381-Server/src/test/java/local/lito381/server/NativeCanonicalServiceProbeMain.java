package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static local.lito381.server.ModernNativeCanonicalWorldProtocol.*;

/** Explicit Native564 bytes through real Rooms/service. In-memory rows; no SQL/TLS/renderer acceptance. */
public final class NativeCanonicalServiceProbeMain {
    static final String ZERO="0".repeat(64);
    static int negatives;
    interface Checked {void run()throws Exception;}
    static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void reject(Checked action)throws Exception{try{action.run();}catch(SecurityException expected){negatives++;return;}throw new AssertionError("negative service admission accepted");}
    static void rejectInvalid(Checked action)throws Exception{try{action.run();}catch(IllegalArgumentException expected){negatives++;return;}throw new AssertionError("invalid Native source combination accepted");}
    static void await(BooleanSupplier ready)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(!ready.getAsBoolean()){require(System.nanoTime()<end,"bounded wait");Thread.sleep(5);}}
    record Player(TournamentPresence.Owner owner,ModernRooms.Scope scope) {}
    static ModernRooms.RoomSnapshot room(ModernRooms rooms,Player p){return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
    static void command(ModernRooms rooms,Player p,ModernRooms.Action action,int value){var s=room(rooms,p).summary();require(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,s.id(),s.revision(),value,null,"")).status()==ModernRooms.Status.OK,"room command");}
    static Request request(int id,Player p,ModernBattleWorldProtocol.Action action,ModernTargetHistory.World world,SourceAttachment source){
        var body=new ModernBattleWorldProtocol.Request(p.scope(),action,world==null?0:world.roomId(),0,world==null?0:world.matchId(),world==null?0:world.generation(),action==ModernBattleWorldProtocol.Action.LOADED?source.manifest().digest():ZERO);
        return new Request(id,2,source==null?ZERO:source.plan().digest(),action==ModernBattleWorldProtocol.Action.LOADED?loadProof(source,body):ZERO,body);
    }
    static Result send(ModernWorldService service,Player p,Request q,ModernSessionBoundary.Negotiated peer)throws Exception{
        var result=service.nativeCanonicalWorld(p.owner(),q,peer);var encoded=ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCanonicalWorldResult(result),peer);
        require(ByteBuffer.wrap(encoded).getInt(4)==encoded.length&&ByteBuffer.wrap(encoded).getShort(12)==89,"selected result framing");return result;
    }
    static ModernApplicationProtocol.BattleOperationRequest move(Player p,Result state,long sequence,int heading){
        var view=state.reply().view();var actor=view.world().actors().stream().filter(a->a.pose().entityId()==view.personal().entityId()).findFirst().orElseThrow();
        var pose=actor.pose();var proof=new ModernBattleCommandLife.Proof(p.scope(),view.world().identity(),pose.entityId(),1,view.world().revision());var cell=pose.position();
        var command=new ModernApplicationProtocol.WorldRequest(100,new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.MOVE,heading,0,0,view.world().identity().matchId(),view.world().identity().generation(),sequence,cell.x(),cell.y(),ZERO));
        return new ModernApplicationProtocol.BattleOperationRequest(100,new ModernBattleActionProtocol.Request(proof,ModernBattleActionProtocol.NO_TARGET,command));
    }
    public static void main(String[] args)throws Exception {
        var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(Path.of(args[0])),()->false);
        var plan=plan(candidate);var source=new SourceAttachment(plan,candidate.binding().map().manifest());
        int caps=ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;
        var peer=new ModernSessionBoundary.Negotiated(1,47,1,caps,256);ModernSessionExtensions.requireContext(peer);
        var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(candidate.binding().key()));var players=new ArrayList<Player>();var release=new CountDownLatch(1);var entered=new CountDownLatch(1);
        var mapping=new ModernWorldService.Mapping(candidate.binding(),candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100));
        var fullVipMapping=new ModernWorldService.Mapping(candidate.binding(),candidate.anchor(),TimeUnit.MILLISECONDS.toNanos(100),TimeUnit.MILLISECONDS.toNanos(100)).withFullVip(true);
        rejectInvalid(()->ModernWorldService.withNativeCanonicalResources(rooms,Map.of(candidate.binding().key(),fullVipMapping),new ModernWorldService.Limits(1,1,1,4000,5,new ModernBattleEngine.Limits(15000,0,45000)),Map.of(candidate.binding().key(),plan)));
        var service=ModernWorldService.withNativeCanonicalResources(rooms,Map.of(candidate.binding().key(),mapping),new ModernWorldService.Limits(1,1,1,4000,5,new ModernBattleEngine.Limits(15000,0,45000)),Map.of(candidate.binding().key(),plan));
        var vectors=new LinkedHashMap<String,Object>();boolean retired=false;
        try {
            for(int sourceId:List.of(7,42)) {
                var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(sourceId,Long.MAX_VALUE),"source-"+sourceId,"canonical-"+sourceId,80,6,1);
                require(presence.select(owner,selection)==TournamentPresence.SelectStatus.ACCEPTED,"selection");rooms.enterLobby(owner,selection,Long.MIN_VALUE,0);
                var p=new Player(owner,new ModernRooms.Scope(sourceId,Long.MAX_VALUE,Long.MIN_VALUE,0));players.add(p);
                service.register(owner,p.scope(),deadline->{entered.countDown();if(!release.await(3,TimeUnit.SECONDS))throw new java.io.IOException("bounded preparation");return new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE,selection.characterName(),6,1,80);},null,false,null,false,null,false,false,false,false,false,false,peer);
            }
            var a=players.get(0);var b=players.get(1);
            var bootstrap=send(service,a,request(1,a,ModernBattleWorldProtocol.Action.READ,null,null),peer);require(bootstrap.source()==null,"bootstrap absence");
            require(rooms.execute(a.owner(),new ModernRooms.Command(a.scope(),ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("native-canonical","",2,1,564,0),"")).status()==ModernRooms.Status.OK,"create");
            var summary=room(rooms,a).summary();require(rooms.execute(b.owner(),new ModernRooms.Command(b.scope(),ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")).status()==ModernRooms.Status.OK,"join");
            command(rooms,a,ModernRooms.Action.READY,1);command(rooms,b,ModernRooms.Action.READY,1);
            var q=request(2,a,ModernBattleWorldProtocol.Action.READ,null,null);require(send(service,a,q,peer).source().equals(source),"detached discovery");
            summary=room(rooms,a).summary();final var s=summary;
            var prepare=new Request(3,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,s.id(),s.revision(),0,0,ZERO));
            service.retireNativeDiscovery(a.owner());reject(()->send(service,a,prepare,peer));send(service,a,q,peer);
            var wrongRevision=new Request(4,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,s.id(),s.revision()-1,0,0,ZERO));reject(()->send(service,a,wrongRevision,peer));
            reject(()->send(service,a,new Request(5,2,"1".repeat(64),ZERO,prepare.world()),peer));
            command(rooms,a,ModernRooms.Action.READY,0);command(rooms,a,ModernRooms.Action.READY,1);reject(()->send(service,a,prepare,peer));
            var currentRoom=room(rooms,a).summary();var admittedPrepare=new Request(20,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,currentRoom.id(),currentRoom.revision(),0,0,ZERO));send(service,a,q,peer);
            send(service,b,request(6,b,ModernBattleWorldProtocol.Action.READ,null,null),peer);
            reject(()->service.battleWorld(a.owner(),prepare.world()));reject(()->service.handle(a.owner(),prepare.world().command()));reject(()->service.vipWorld(a.owner(),prepare.world()));
            var vipPeer=new ModernSessionBoundary.Negotiated(1,47,1,ModernSessionExtensions.DEPENDENCIES,256|16|128);ModernSessionExtensions.requireContext(vipPeer);
            reject(()->service.nativeCanonicalWorld(a.owner(),q,vipPeer));
            var preparing=send(service,a,admittedPrepare,peer);require(preparing.reply().state()==ModernBattleWorldProtocol.State.BEFORE_ENGINE,"before engine");require(entered.await(2,TimeUnit.SECONDS),"worker entered");
            var world=preparing.reply().world();var read=request(7,a,ModernBattleWorldProtocol.Action.READ,world,source);
            require(send(service,a,request(8,a,ModernBattleWorldProtocol.Action.LOADED,world,source),peer).reply().status()!=ModernWorldProtocol.Status.OK,"early loaded cannot verify");
            reject(()->service.readBattle(a.owner(),new ModernBattleReadProtocol.Request(a.scope(),world)));
            release.countDown();await(()->service.nativeCanonicalWorld(a.owner(),read,peer).reply().state()==ModernBattleWorldProtocol.State.DATA);
            var loading=send(service,a,read,peer);require(source.manifest().equals(loading.reply().view().world().manifest()),"full manifest DATA equality");
            var originalPin=source.manifest().sectors().get(0);var changedManifest=new ModernWorldMap.Manifest(source.manifest().mapId(),List.of(new ModernWorldMap.SectorManifest(originalPin.key(),originalPin.byteLength(),"1".repeat(64))),source.manifest().digest());
            rejectInvalid(()->new Result(loading.echo(),new SourceAttachment(plan,changedManifest),loading.reply()));
            var operation=move(a,loading,1,0);reject(()->service.operation(a.owner(),operation));reject(()->service.action(a.owner(),new ModernApplicationProtocol.BattleActionRequest(operation.requestId(),operation.request())));
            var revive=new ModernApplicationProtocol.BattleRevivalRequest(101,new ModernBattleRevivalProtocol.Request(operation.request().proof(),1));reject(()->service.revival(a.owner(),revive));
            var loaded=request(9,a,ModernBattleWorldProtocol.Action.LOADED,world,source);reject(()->send(service,a,new Request(10,2,plan.digest(),"1".repeat(64),loaded.world()),peer));reject(()->service.nativeCanonicalWorld(b.owner(),loaded,peer));
            var one=send(service,a,loaded,peer);require(one.reply().view().world().actors().get(0).pose().loaded(),"self Loaded publication");
            require(service.readBattle(a.owner(),new ModernBattleReadProtocol.Request(a.scope(),world)).state()==ModernBattleReadProtocol.State.DATA,"verified 60 DATA read");
            reject(()->service.readBattle(b.owner(),new ModernBattleReadProtocol.Request(b.scope(),world)));
            var both=send(service,b,request(11,b,ModernBattleWorldProtocol.Action.LOADED,world,source),peer);require(both.reply().view().world().phase()==ModernBattleEngine.Phase.ACTIVE,"both loaded active");
            var active=send(service,a,read,peer);var actor=active.reply().view().world().actors().get(0).pose();var positions=active.reply().view().world().actors().stream().map(x->x.pose().position()).toList();int heading=-1;
            reject(()->service.action(a.owner(),new ModernApplicationProtocol.BattleActionRequest(operation.requestId(),operation.request())));
            reject(()->service.nativeWorld(a.owner(),new ModernWorldResourceProtocol.Request(21,2,plan.digest(),read.world().command()),peer));
            int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};for(int h=0;h<8;h++){var target=new ModernWorldMap.Cell(actor.position().x()+dx[h],actor.position().y()+dy[h]);if(candidate.binding().map().canStep(actor.position(),target)&&!positions.contains(target)){heading=h;break;}}require(heading>=0,"vacant step");
            var lawfulMove=move(a,active,1,heading);var correctLife=lawfulMove.request().proof();var staleLife=new ModernBattleCommandLife.Proof(correctLife.player(),correctLife.world(),correctLife.entity(),correctLife.life()+1,correctLife.revision());
            var rejectedLife=service.operation(a.owner(),new ModernApplicationProtocol.BattleOperationRequest(100,new ModernBattleActionProtocol.Request(staleLife,lawfulMove.request().target(),lawfulMove.request().command())));
            require(rejectedLife.receipt().rejection()==ModernBattleCommandLife.Rejection.STALE_LIFE&&rejectedLife.receipt().view().world().actors().get(0).pose().lastMoveSequence()==0,"Native stale life consumes no MOVE sequence");
            var moved=service.operation(a.owner(),move(a,active,1,heading));require(ModernBattleOperationProtocol.outcome(moved.echo(),moved.receipt()).disposition()==ModernBattleOperationProtocol.Disposition.APPLIED,"verified canonical MOVE applied");
            var payload=ModernApplicationProtocol.encode(new ModernApplicationProtocol.NativeCanonicalWorldResult(active),peer);vectors.put("loading89_hex",HexFormat.of().formatHex(encodeResult(loading,peer)));vectors.put("loaded89_hex",HexFormat.of().formatHex(encodeResult(one,peer)));vectors.put("active89_hex",HexFormat.of().formatHex(payload));vectors.put("operation65_hex",HexFormat.of().formatHex(ModernApplicationProtocol.encode(moved,peer)));
            vectors.put("native_manifest",source.manifest());vectors.put("source_plan_sha256",plan.digest());vectors.put("selected_capabilities",Integer.toUnsignedLong(caps));vectors.put("selected_extensions",256);
            send(service,a,request(12,a,ModernBattleWorldProtocol.Action.LEAVE,world,source),peer);reject(()->send(service,a,loaded,peer));reject(()->service.operation(a.owner(),operation));
            var aborted=send(service,b,request(13,b,ModernBattleWorldProtocol.Action.READ,world,source),peer);require(aborted.reply().lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED,"follower abort DATA");send(service,b,request(14,b,ModernBattleWorldProtocol.Action.LEAVE,world,source),peer);await(service::isQuiescent);
            command(rooms,a,ModernRooms.Action.READY,1);command(rooms,b,ModernRooms.Action.READY,1);
            for(var p:players)send(service,p,request(30,p,ModernBattleWorldProtocol.Action.READ,null,null),peer);
            var nextRoom=room(rooms,a).summary();var nextPrepare=new Request(31,2,plan.digest(),ZERO,new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,nextRoom.id(),nextRoom.revision(),0,0,ZERO));
            var next=send(service,a,nextPrepare,peer);var nextWorld=next.reply().world();require(nextWorld.matchId()>world.matchId()&&nextWorld.generation()>world.generation(),"new match generation");
            var nextRead=request(32,a,ModernBattleWorldProtocol.Action.READ,nextWorld,source);await(()->service.nativeCanonicalWorld(a.owner(),nextRead,peer).reply().state()==ModernBattleWorldProtocol.State.DATA);
            reject(()->service.readBattle(a.owner(),new ModernBattleReadProtocol.Request(a.scope(),nextWorld)));
            for(var p:players)send(service,p,request(33,p,ModernBattleWorldProtocol.Action.LOADED,nextWorld,source),peer);
            reject(()->send(service,a,loaded,peer));reject(()->service.operation(a.owner(),operation));
            for(var p:players)send(service,p,request(34,p,ModernBattleWorldProtocol.Action.LEAVE,nextWorld,source),peer);await(service::isQuiescent);
        }finally{release.countDown();service.close();retired=service.awaitTermination(4000);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}require(retired,"owned workers retired");}
        Path output=Path.of(args[1]);Files.writeString(output.resolve("canonical-service-data-vectors.json"),NativeTerrainProbeMain.json(vectors));
        var report=new LinkedHashMap<String,Object>();report.put("state","PASSED_NATIVE_CANONICAL_SERVICE_NAVIGATION_DATA");report.put("source_namespace","NATIVE_LITO_LTMAP_V1");report.put("source_same_object_cross_source",true);report.put("high_bit_epoch",true);report.put("actual_DATA",true);report.put("negative_vectors",negatives);report.put("service_ledger_integrated",true);report.put("verified_operation_move",true);report.put("reprepare_requires_new_LOADED",true);report.put("old_LOADED_and_operation_rejected_after_reprepare",true);report.put("workers_retired",retired);report.put("SQL",false);report.put("TLS",false);report.put("GUI",false);report.put("renderer_loaded_qualified",false);report.put("combat_settlement_qualified",false);
        report.put("Native_stale_life_receipt_consumes_no_sequence",true);
        Files.writeString(output.resolve("canonical-service-probe.json"),NativeTerrainProbeMain.json(report));System.out.println("Native canonical service DATA and MOVE passed; "+negatives+" negative admissions; no SQL/TLS/renderer qualification");
    }
    static ModernWorldResourceProtocol.Plan plan(NativeWorldResourceCandidate.Candidate candidate){
        var archives=List.of(new ModernWorldResourceProtocol.ArchivePin(1,1,33884,"EFD13705C355BAAE6E1C95C88E24FDCDB2948D17F8936167486F97C8C0DE330E"),new ModernWorldResourceProtocol.ArchivePin(2,1,148485306,"6E80F46CA19D32BFA53FB360D2398755F4BBBA7383A6B4CAD811BCB79F910D22"),new ModernWorldResourceProtocol.ArchivePin(3,1,13136,"73536646591E04BFB62B55320E075F0C6D5F8DC17FE29BC012C7925EED670BD8"),new ModernWorldResourceProtocol.ArchivePin(4,1,63004112,"7029D561D8EF1420128D318E827BAE07D552E5316839CDDB362D9223F2FB3962"));
        return new ModernWorldResourceProtocol.Plan(2,564,candidate.binding().map().digest(),archives);
    }
}
