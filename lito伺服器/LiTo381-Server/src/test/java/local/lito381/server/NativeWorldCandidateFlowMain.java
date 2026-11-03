package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Artifact-only programmatic navigation/return fixture; does not claim combat, SQL or client acceptance. */
public final class NativeWorldCandidateFlowMain {
    private static final String ZERO="0".repeat(64);
    record Player(TournamentPresence.Owner owner,ModernRooms.Scope scope) {}
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("candidate config and owned output required");
        var props=NativeWorldResourceCandidate.config(Path.of(args[0]));var candidate=NativeWorldResourceCandidate.load(props,()->false);
        var negative=new LinkedHashMap<String,Object>();
        for(var change:Map.of("source.namespace","SHARED_LINEAGE","map.id","526","sector.7fff8000.sha256",ZERO,"canonical.sha256",ZERO,"parser.jar.sha256",ZERO).entrySet()) {
            var bad=new Properties();bad.putAll(props);bad.setProperty(change.getKey(),change.getValue());
            try{NativeWorldResourceCandidate.load(bad,()->false);throw new AssertionError("invalid candidate admitted");}
            catch(IllegalArgumentException expected){negative.put(change.getKey(),expected.getMessage());}
        }
        try{NativeWorldResourceCandidate.load(props,()->true);throw new AssertionError("cancelled candidate admitted");}
        catch(IllegalArgumentException expected){require("CANCELLED".equals(expected.getMessage()),"cancellation");negative.put("cancelled",expected.getMessage());}
        var rows=new ArrayList<Object>();for(int count:List.of(2,100))rows.add(flow(candidate,count));
        var output=new LinkedHashMap<String,Object>();output.put("candidate",candidate.id());output.put("namespace",candidate.namespace());
        output.put("scope","ModernWorldService/Rooms programmatic LOAD->ACTIVE movement->RETURN/reprepare; in-memory character rows, navigation mapping; no TLS/Rust client, SQL, combat settlement or GUI acceptance");
        output.put("negative_admission",negative);output.put("flows",rows);output.put("gui_acceptance",false);
        Files.writeString(Path.of(args[1]),NativeTerrainProbeMain.json(output));System.out.println("[native-world-candidate] 2/100 member LOAD ACTIVE RETURN and negative admission passed");
    }
    private static Object flow(NativeWorldResourceCandidate.Candidate c,int count)throws Exception {
        var map=c.binding().map();var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(c.binding().key()));
        var players=new ArrayList<Player>();var reads=new AtomicInteger();
        var limits=new ModernWorldService.Limits(1,1,1,2_000,5,new ModernBattleEngine.Limits(15_000,0,45_000));
        var service=new ModernWorldService(rooms,Map.of(c.binding().key(),new ModernWorldService.Mapping(c.binding(),c.anchor(),TimeUnit.MILLISECONDS.toNanos(100))),limits);
        boolean retired=false;var row=new LinkedHashMap<String,Object>();
        try {
            for(int i=0;i<count;i++) {
                var owner=presence.connect();int source=i%2==0?7:42;long object=i/2+1;
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,object),"source-"+source,"candidate-"+i,80,6,1);
                require(presence.select(owner,selection)==TournamentPresence.SelectStatus.ACCEPTED,"selection");rooms.enterLobby(owner,selection,1,0);
                var player=new Player(owner,new ModernRooms.Scope(source,object,1,0));players.add(player);
                service.register(owner,player.scope(),deadline->{reads.incrementAndGet();return new ModernApplicationProtocol.CharacterRow(object,selection.characterName(),6,1,80);});
            }
            var first=players.get(0);ok(rooms.execute(first.owner(),new ModernRooms.Command(first.scope(),ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("native-candidate","",count,1,564,0),"")));
            for(var player:players.subList(1,players.size())){var summary=view(rooms,first).joined().summary();ok(rooms.execute(player.owner(),new ModernRooms.Command(player.scope(),ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")));}
            ready(rooms,players);var frame=start(service,rooms,players);
            require(frame.manifest().mapId()==564&&frame.manifest().digest().equals(map.digest()),"wire identity");
            require(frame.actors().size()==count,"actor count");var positions=frame.actors().stream().map(ModernBattleEngine.Actor::position).toList();map.validateSpawns(positions);
            var actor=frame.actors().get(0);var position=actor.position();int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
            int blocked=-1,allowed=-1;for(int h=0;h<8;h++){var target=new ModernWorldMap.Cell(position.x()+dx[h],position.y()+dy[h]);if(!map.canStep(position,target))blocked=h;else if(!positions.contains(target))allowed=h;}
            // Dense 100-player anchor may have no adjacent vacancy. Collision gate remains testable on the 2-member round.
            var movement=new LinkedHashMap<String,Object>();
            if(count==2) {
                require(allowed>=0,"open vacant edge");var moved=service.handle(first.owner(),move(first,frame,1,allowed,position));
                require(moved.status()==Status.OK,"allowed move");var movedCell=new ModernWorldMap.Cell(position.x()+dx[allowed],position.y()+dy[allowed]);
                require(read(service,first).actors().get(0).position().equals(movedCell),"movement committed");
                var route=pathToBlocked(map,movedCell,new HashSet<>(positions.subList(1,positions.size())));
                long seq=1;var current=movedCell;
                for(int h:route){Thread.sleep(110);var result=service.handle(first.owner(),move(first,frame,++seq,h,current));require(result.status()==Status.OK,"path move");current=new ModernWorldMap.Cell(current.x()+dx[h],current.y()+dy[h]);}
                int wall=-1;for(int h=0;h<8;h++)if(!map.canStep(current,new ModernWorldMap.Cell(current.x()+dx[h],current.y()+dy[h]))){wall=h;break;}
                require(wall>=0,"blocked edge located");Thread.sleep(110);var rejected=service.handle(first.owner(),move(first,frame,++seq,wall,current));
                require(rejected.status()!=Status.OK&&read(service,first).actors().get(0).position().equals(current),"wall rejects unchanged position");
                movement.put("allowed_move",true);movement.put("blocked_reason",rejected.reason().name());movement.put("wall_from",current);movement.put("wall_heading",wall);movement.put("path_steps",route.size());
            } else movement.put("scope","100 distinct actors standing and connected; movement gate exercised in two-member round");
            var before=frame;var departing=players.get(players.size()-1);require(service.handle(departing.owner(),leave(departing,before)).frame()==null,"leave return");
            require(read(service,first).phase()==Phase.ABORTED,"remaining actor terminal");
            for(var player:players) {var current=read(service,player);if(current!=null)require(service.handle(player.owner(),leave(player,current)).frame()==null,"return acknowledgement");}
            await(service::isQuiescent);require(!rooms.isPreparing(first.owner())&&view(rooms,first).joined().summary().ready()==0,"room reset");
            ready(rooms,players);var next=start(service,rooms,players);require(next.matchId()>before.matchId()&&next.generation()>before.generation(),"new generation");
            boolean stale=false;try{service.handle(first.owner(),loaded(first,before));}catch(SecurityException expected){stale=true;}require(stale,"stale loaded rejected");
            for(var player:players){var current=read(service,player);if(current!=null)service.handle(player.owner(),leave(player,current));}await(service::isQuiescent);
            row.put("members",count);row.put("source_scoped_same_object_ids",true);row.put("backend_reads",reads.get());row.put("distinct_connected_positions",positions.size());row.put("movement",movement);
            row.put("return_all_frames_cleared",players.stream().allMatch(p->read(service,p)==null));row.put("room_preparing",rooms.isPreparing(first.owner()));row.put("room_ready",view(rooms,first).joined().summary().ready());row.put("reprepare_new_generation",true);row.put("stale_loaded_rejected",true);
        } finally {service.close();retired=service.awaitTermination(4_000);for(var player:players){rooms.disconnect(player.owner());presence.disconnect(player.owner());}require(retired,"workers retired");}
        row.put("workers_retired",retired);return row;
    }
    private static List<Integer> pathToBlocked(ModernWorldMap map,ModernWorldMap.Cell start,Set<ModernWorldMap.Cell> occupied) {
        int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};var paths=new HashMap<ModernWorldMap.Cell,List<Integer>>();var q=new ArrayDeque<ModernWorldMap.Cell>();paths.put(start,List.of());q.add(start);
        while(!q.isEmpty()){var cell=q.remove();for(int h=0;h<8;h++)if(!map.canStep(cell,new ModernWorldMap.Cell(cell.x()+dx[h],cell.y()+dy[h])))return paths.get(cell);
            for(int h=0;h<8;h++){var next=new ModernWorldMap.Cell(cell.x()+dx[h],cell.y()+dy[h]);if(map.canStep(cell,next)&&!occupied.contains(next)&&!paths.containsKey(next)){var path=new ArrayList<>(paths.get(cell));path.add(h);paths.put(next,path);q.add(next);}}}throw new AssertionError("no wall in bounded component");
    }
    private static Frame start(ModernWorldService service,ModernRooms rooms,List<Player> players)throws Exception {
        var first=players.get(0);var s=view(rooms,first).joined().summary();require(service.handle(first.owner(),new Request(first.scope(),Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO)).status()==Status.OK,"prepare");
        await(()->{var f=read(service,first);return f!=null&&f.phase()==Phase.LOADING;});var loading=read(service,first);
        for(var p:players)require(service.handle(p.owner(),loaded(p,loading)).status()==Status.OK,"loaded");await(()->read(service,first).phase()==Phase.ACTIVE);return read(service,first);
    }
    private static void ready(ModernRooms rooms,List<Player> players){for(var p:players){var s=view(rooms,p).joined().summary();ok(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}}
    private static ModernRooms.View view(ModernRooms rooms,Player p){return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view();}
    private static Frame read(ModernWorldService service,Player p){return service.handle(p.owner(),new Request(p.scope(),Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();}
    private static Request loaded(Player p,Frame f){return new Request(p.scope(),Action.LOADED,0,0,0,f.matchId(),f.generation(),0,0,0,f.manifest().digest());}
    private static Request leave(Player p,Frame f){return new Request(p.scope(),Action.LEAVE,0,0,0,f.matchId(),f.generation(),0,0,0,ZERO);}
    private static Request move(Player p,Frame f,long seq,int h,ModernWorldMap.Cell pos){return new Request(p.scope(),Action.MOVE,h,0,0,f.matchId(),f.generation(),seq,pos.x(),pos.y(),ZERO);}
    private static void await(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!condition.getAsBoolean()){require(System.nanoTime()<end,"bounded wait");Thread.sleep(5);}}
    private static void ok(ModernRooms.Result r){require(r.status()==ModernRooms.Status.OK,"rooms: "+r.reason());}
    private static void require(boolean pass,String text){if(!pass)throw new AssertionError(text);}
}
