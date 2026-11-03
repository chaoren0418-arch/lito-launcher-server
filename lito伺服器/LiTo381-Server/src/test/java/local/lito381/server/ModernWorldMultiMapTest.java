package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Synthetic geometry + real authority/service/engine lifecycle. This is not evidence of decoded shared 509 bytes or SQL. */
class ModernWorldMultiMapTest {
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms;
        final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final List<ModernMatchSettlement.Event> saved=new CopyOnWriteArrayList<>();
        final List<AtomicInteger> fallbackRowReads=List.of(new AtomicInteger(),new AtomicInteger());
        final List<AtomicInteger> physicalReads=List.of(new AtomicInteger(),new AtomicInteger());
        final ModernWorldService service;
        final Map<ModernWorldBinding.Key,ModernWorldService.Mapping> mappings;
        Fixture()throws Exception{this(Set.of(509,514));}
        Fixture(Set<Integer> selectedMaps)throws Exception {
            var chosen=new java.util.HashMap<ModernWorldBinding.Key,ModernWorldService.Mapping>();
            for(int id:selectedMaps) {
                byte[] bytes=ModernWorldMapTest.openSectorBytes();
                if(id==509)ModernWorldMapTest.setAttributes(bytes,0,0,(short)1,(short)0);
                var map=ModernWorldMap.fromSectorBytes(id,Map.of(ModernWorldMapQualificationTest.SECTOR,bytes),()->false);
                var key=new ModernWorldBinding.Key(1,id);
                var expected=new ModernWorldMapQualification.Expected(key,map.manifest(),ModernWorldMapQualificationTest.ANCHOR,100);
                var binding=ModernWorldMapQualification.qualify(map,expected);
                chosen.put(key,new ModernWorldService.Mapping(binding,expected.anchor(),1_000_000,1_000_000));
            }
            mappings=Map.copyOf(chosen);rooms=new ModernRooms(mappings.keySet());
            for(int source:List.of(7,42)) {
                var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,Long.MAX_VALUE),
                        "source-"+source,"player-"+source,30,6,1);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));
                rooms.enterLobby(owner,selection,1,0);players.add(new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,Long.MAX_VALUE,1,0)));
            }
            service=new ModernWorldService(rooms,mappings,new ModernWorldService.Limits(4,2,2,2_000,5,
                    new ModernBattleEngine.Limits(2_000,0,10_000)),bound->0,(event,deadline)->{
                assertTrue(deadline-System.nanoTime()>0);assertTrue(saved.stream().noneMatch(old->old.resultId().equals(event.resultId())));
                saved.add(event);
            });
            roomAction(0,ModernRooms.Action.CREATE,0,ModernRoomsTest.draft(100,1,514,0));
            var created=view().joined().summary();var guest=players.get(1);
            ModernRoomsTest.accepted(rooms.execute(guest.owner(),new ModernRooms.Command(guest.scope(),ModernRooms.Action.JOIN,created.id(),created.revision(),0,null,"")));
            for(int index=0;index<players.size();index++) {
                final int at=index;var player=players.get(index);
                service.register(player.owner(),player.scope(),deadline->{fallbackRowReads.get(at).incrementAndGet();return row(player);},deadline->{
                    physicalReads.get(at).incrementAndGet();var source=ModernPhysicalCombatTest.fixtureSource(player.scope().sourceId(),row(player),1,10,0);
                    return ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),System.currentTimeMillis());
                },true);
            }
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p){var s=presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(s.key().objectId(),s.characterName(),s.characterClass(),s.sex(),s.level());}
        ModernRooms.View view(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view();}
        ModernRooms.Result roomAction(int who,ModernRooms.Action action,int value,ModernRooms.Draft draft) {
            var player=players.get(who);var room=action==ModernRooms.Action.CREATE?null:view().joined().summary();
            var result=rooms.execute(player.owner(),new ModernRooms.Command(player.scope(),action,room==null?0:room.id(),room==null?0:room.revision(),value,draft,""));
            ModernRoomsTest.accepted(result);return result;
        }
        void ready(){for(int n=0;n<players.size();n++)roomAction(n,ModernRooms.Action.READY,1,null);}
        Reply prepare(){var room=view().joined().summary();var p=players.get(0);return service.handle(p.owner(),new Request(p.scope(),Action.PREPARE,0,room.id(),room.revision(),0,0,0,0,0,ZERO_DIGEST));}
        Frame read(int who){var p=players.get(who);return service.handle(p.owner(),new Request(p.scope(),Action.READ,0,0,0,0,0,0,0,0,ZERO_DIGEST)).frame();}
        Request request(int who,Action action,Frame frame){var p=players.get(who);return new Request(p.scope(),action,0,0,0,frame.matchId(),frame.generation(),0,0,0,action==Action.LOADED?frame.manifest().digest():ZERO_DIGEST);}
        Frame loading()throws Exception {ready();assertEquals(Status.OK,prepare().status());await(()->read(0).phase()==Phase.LOADING);return read(0);}
        Frame active()throws Exception {var loading=loading();for(int who=0;who<players.size();who++)assertEquals(Status.OK,service.handle(players.get(who).owner(),request(who,Action.LOADED,loading)).status());await(()->read(0).phase()==Phase.ACTIVE);return read(0);}
        Frame finish(Frame active,int winner,int expectedCalls)throws Exception {
            var p=players.get(winner);var result=service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,active.matchId(),active.generation(),1,winner==0?2:1));
            assertEquals(ModernCombatProtocol.Status.OK,result.status());assertEquals(Phase.FINISHED,result.frame().world().phase());
            await(()->saved.size()==expectedCalls&&read(0).completion().state()==SettlementState.SAVED);return read(0);
        }
        void leave(int who,Frame frame){assertNull(service.handle(players.get(who).owner(),request(who,Action.LEAVE,frame)).frame());}
        void leaveBoth(Frame frame)throws Exception{leave(0,frame);leave(1,frame);await(service::isQuiescent);assertNull(read(0));assertNull(read(1));}
        @Override public void close()throws Exception {
            service.close();assertTrue(service.awaitTermination(4_000));assertTrue(service.isQuiescent());
            for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}
            assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.memberCount());assertEquals(0,rooms.roomCount());assertEquals(0,presence.ownerCount());assertEquals(0,presence.userCount());
        }
    }
    static void await(BooleanSupplier condition)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!condition.getAsBoolean()){assertTrue(end-System.nanoTime()>0,"bounded multi-map authority condition");Thread.sleep(5);}
    }
    @Test void sameOwnersComplete514Then509Then514WithDistinctFreshCapturesAndExactlyOneResultEach()throws Exception {
        try(var f=new Fixture()) {
            var originalOwners=f.players.stream().map(ModernRoomsTest.Player::owner).toList();Frame previous=null;
            int[] maps={514,509,514};
            for(int round=0;round<maps.length;round++) {
                if(round>0){var changed=f.roomAction(0,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(100,1,maps[round],0));assertEquals(0,changed.view().joined().summary().ready());}
                var active=f.active();assertEquals(maps[round],active.manifest().mapId());assertEquals(maps[round],active.room().summary().config().mapId());
                assertEquals(f.mappings.get(new ModernWorldBinding.Key(1,maps[round])).binding().map().manifest(),active.manifest());
                if(previous!=null){assertTrue(active.matchId()>previous.matchId());assertTrue(active.generation()>previous.generation());assertEquals(previous.room().summary().id(),active.room().summary().id());}
                var finished=f.finish(active,round%2,round+1);var event=finished.completion().event();assertEquals(maps[round],event.mapId());assertEquals(active.matchId(),event.matchId());assertEquals(active.generation(),event.generation());
                assertEquals(List.of(7,42),event.participants().stream().map(p->p.key().sourceId()).toList());assertTrue(event.participants().stream().allMatch(p->p.key().objectId()==Long.MAX_VALUE));
                assertEquals(ModernMatchSettlement.Verdict.WIN,event.participants().get(round%2).verdict());
                // Combat preparation uses the freshly qualified fighter's exact source row, not a second non-atomic roster read.
                for(var count:f.fallbackRowReads)assertEquals(0,count.get());for(var count:f.physicalReads)assertEquals(round+1,count.get());
                assertEquals(0,f.view().joined().summary().ready());f.leaveBoth(finished);previous=finished;
            }
            assertEquals(3,f.saved.stream().map(ModernMatchSettlement.Event::resultId).distinct().count());assertEquals(originalOwners,f.players.stream().map(ModernRoomsTest.Player::owner).toList());
            assertEquals(2,f.saved.stream().filter(e->e.participants().get(0).verdict()==ModernMatchSettlement.Verdict.WIN).count());
            assertEquals(1,f.saved.stream().filter(e->e.participants().get(1).verdict()==ModernMatchSettlement.Verdict.WIN).count());
        }
    }
    @Test void oldManifestGenerationAndAllOldScopedActionsCannotMutateTheNextMap()throws Exception {
        try(var f=new Fixture()){var old=f.finish(f.active(),0,1);f.leaveBoth(old);f.roomAction(0,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(100,1,509,0));var next=f.loading();var p=f.players.get(0);
            var wrongDigest=new Request(p.scope(),Action.LOADED,0,0,0,next.matchId(),next.generation(),0,0,0,old.manifest().digest());
            assertThrows(SecurityException.class,()->f.service.handle(p.owner(),wrongDigest));assertEquals(next,f.read(0));
            for(var action:List.of(Action.READ,Action.LOADED,Action.LEAVE))assertThrows(SecurityException.class,()->f.service.handle(p.owner(),f.request(0,action,old)));
            var oldMove=new Request(p.scope(),Action.MOVE,2,0,0,old.matchId(),old.generation(),1,old.actors().get(0).position().x(),old.actors().get(0).position().y(),ZERO_DIGEST);
            assertThrows(SecurityException.class,()->f.service.handle(p.owner(),oldMove));
            assertThrows(SecurityException.class,()->f.service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,old.matchId(),old.generation(),1,2)));
            var wrongGeneration=new Request(p.scope(),Action.READ,0,0,0,next.matchId(),old.generation(),0,0,0,ZERO_DIGEST);
            assertThrows(SecurityException.class,()->f.service.handle(p.owner(),wrongGeneration));
            assertThrows(SecurityException.class,()->f.service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,next.matchId(),old.generation(),1,2)));
            assertEquals(next,f.read(0));assertEquals(1,f.saved.size());
            for(int who=0;who<2;who++)f.service.handle(f.players.get(who).owner(),f.request(who,Action.LOADED,next));
            assertEquals(Phase.ACTIVE,f.read(0).phase());f.finish(f.read(0),1,2);
        }
    }
    @Test void changingRoomMapCannotEraseAnotherFollowersUnacknowledgedCompletedResult()throws Exception {
        try(var f=new Fixture()){var result=f.finish(f.active(),0,1);f.leave(0,result);f.roomAction(0,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(100,1,509,0));f.ready();
            assertEquals(Status.BUSY,f.prepare().status());assertNull(f.read(0));assertEquals(result,f.read(1));assertEquals(1,f.saved.size());
            f.leave(1,result);var next=f.active();assertEquals(509,next.manifest().mapId());f.finish(next,1,2);}
    }
    @Test void unmappedRoomCannotBorrowOtherGeometryAndFailedCaptureReleasesReadinessAndCapacity()throws Exception {
        try(var f=new Fixture(Set.of(514))){f.roomAction(0,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(100,1,509,0));f.ready();
            var reply=f.prepare();assertEquals(Status.UNAVAILABLE,reply.status());assertEquals(Reason.NO_BINDING,reply.reason());assertNull(reply.frame());assertNull(f.read(0));
            assertFalse(f.rooms.isPreparing(f.players.get(0).owner()));assertEquals(0,f.view().joined().summary().ready());assertTrue(f.saved.isEmpty());assertTrue(f.fallbackRowReads.stream().allMatch(n->n.get()==0));assertTrue(f.physicalReads.stream().allMatch(n->n.get()==0));
            f.roomAction(0,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(100,1,514,0));f.finish(f.active(),0,1);}
    }
    @Test void activeFreezeAndStaleRoomRevisionCannotRebindAnExistingWorld()throws Exception {
        try(var f=new Fixture()){var before=f.view().joined().summary();var active=f.active();var p=f.players.get(0);var current=f.view().joined().summary();
            var denied=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.CONFIGURE,current.id(),current.revision(),0,ModernRoomsTest.draft(100,1,509,0),""));
            assertEquals(ModernRooms.Status.DENIED,denied.status());assertEquals(active,f.read(0));var result=f.finish(active,0,1);f.leaveBoth(result);
            var stale=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.CONFIGURE,before.id(),before.revision(),0,ModernRoomsTest.draft(100,1,509,0),""));
            assertEquals(ModernRooms.Status.CONFLICT,stale.status());assertEquals(514,f.view().joined().summary().config().mapId());}
    }
}
