package local.lito381.server;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
import static local.lito381.server.ModernNativeCanonicalWorldProtocol.*;

/** Cancellation does not retire a delayed reader or authorize its later completion. */
public final class NativeCanonicalRetiredWorkerProbeMain {
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    public static void main(String[] args)throws Exception {
        var candidate=NativeWorldResourceCandidate.load(NativeWorldResourceCandidate.config(Path.of(args[0])),()->false);var plan=NativeCanonicalServiceProbeMain.plan(candidate);var source=new SourceAttachment(plan,candidate.binding().map().manifest());
        int caps=ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;var peer=new ModernSessionBoundary.Negotiated(1,47,1,caps,256);
        var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(candidate.binding().key()));var players=new ArrayList<NativeCanonicalServiceProbeMain.Player>();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var service=ModernWorldService.withNativeCanonicalResources(rooms,Map.of(candidate.binding().key(),new ModernWorldService.Mapping(candidate.binding(),candidate.anchor(),100_000_000)),new ModernWorldService.Limits(1,1,1,4000,5,new ModernBattleEngine.Limits(15000,0,45000)),Map.of(candidate.binding().key(),plan));
        Object entry=null;boolean keptCapacity=false,retired=false,lateEngineRejected=false;
        try {
            for(int sourceId:List.of(7,42)) {
                var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(sourceId,100),"source-"+sourceId,"delayed-"+sourceId,80,6,1);presence.select(owner,selection);rooms.enterLobby(owner,selection,1,0);var p=new NativeCanonicalServiceProbeMain.Player(owner,new ModernRooms.Scope(sourceId,100,1,0));players.add(p);
                service.register(owner,p.scope(),deadline->{entered.countDown();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(release.getCount()!=0&&System.nanoTime()<end){try{release.await(20,TimeUnit.MILLISECONDS);}catch(InterruptedException deliberatelyDelayed){}}require(release.getCount()==0,"bounded delayed reader");return new ModernApplicationProtocol.CharacterRow(100,selection.characterName(),6,1,80);},null,false,null,false,null,false,false,false,false,false,false,peer);
            }
            var a=players.get(0);var b=players.get(1);require(rooms.execute(a.owner(),new ModernRooms.Command(a.scope(),ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("delayed Native worker","",2,1,564,0),"")).status()==ModernRooms.Status.OK,"create");var room=NativeCanonicalServiceProbeMain.room(rooms,a).summary();require(rooms.execute(b.owner(),new ModernRooms.Command(b.scope(),ModernRooms.Action.JOIN,room.id(),room.revision(),0,null,"")).status()==ModernRooms.Status.OK,"join");
            for(var p:players)NativeCanonicalServiceProbeMain.command(rooms,p,ModernRooms.Action.READY,1);
            for(var p:players)service.nativeCanonicalWorld(p.owner(),NativeCanonicalServiceProbeMain.request(1,p,ModernBattleWorldProtocol.Action.READ,null,null),peer);
            room=NativeCanonicalServiceProbeMain.room(rooms,a).summary();var q=new Request(2,2,plan.digest(),"0".repeat(64),new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,room.id(),room.revision(),0,0,"0".repeat(64)));var preparing=service.nativeCanonicalWorld(a.owner(),q,peer);require(entered.await(2,TimeUnit.SECONDS),"actual worker owns delayed reader");
            var registrations=(Map<?,?>)field(service,"registrations");var registration=registrations.get(a.owner());entry=field(registration,"entry");var ledger=(ModernNativeResourceLedger)field(registration,"nativeLedger");
            service.close();keptCapacity=!service.awaitTermination(50)&&!service.isQuiescent();require(keptCapacity,"cancellation retained real reader capacity");
            NativeCanonicalServiceProbeMain.reject(()->ledger.requireAttachment(a.owner(),a.scope(),preparing.reply().world(),source));
            boolean refused=false;try{service.nativeCanonicalWorld(a.owner(),NativeCanonicalServiceProbeMain.request(3,a,ModernBattleWorldProtocol.Action.LOADED,preparing.reply().world(),source),peer);}catch(IllegalStateException|SecurityException expected){refused=true;}require(refused,"closed service cannot acknowledge stale LOADED");
            release.countDown();retired=service.awaitTermination(4000);lateEngineRejected=field(entry,"engine")==null&&field(entry,"publication")==null;require(retired&&service.isQuiescent()&&lateEngineRejected,"late completion published no engine or DATA");
        }finally{release.countDown();service.close();require(service.awaitTermination(4000),"owned delayed worker retired");for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
        Files.writeString(Path.of(args[1]).resolve("canonical-retired-worker-probe.json"),NativeTerrainProbeMain.json(Map.of("state","PASSED_RETIRED_NATIVE_WORKER_FENCE","cancellation_retains_reader_capacity",keptCapacity,"late_completion_published_engine_or_DATA",!lateEngineRejected,"retired_ledger_refused",true,"workers_retired",retired,"SQL",false,"TLS",false,"GUI",false)));
        System.out.println("Retired Native worker: cancellation retained capacity; late reader completion published no engine/DATA");
    }
}
