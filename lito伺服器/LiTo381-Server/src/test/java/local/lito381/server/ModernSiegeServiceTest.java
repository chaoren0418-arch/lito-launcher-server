package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Actual Rooms/service worker/scheduler and production v26 codec, not a UI test. */
class ModernSiegeServiceTest {
    static ModernWorldService.Limits limits(){return new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000));}
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();final ModernWorldService service;final ModernWorldService.Mapping mapping;
        final ModernTimeAttackServiceTest.Journal journal;final AtomicInteger roleDraws=new AtomicInteger();
        Fixture(int attacker,int duration,ModernTimeAttackServiceTest.Journal journal)throws Exception {
            this.journal=journal;var b=ModernSiegeTest.binding(547);var c=ModernSiegeConfigurationTest.policy(50,duration);
            mapping=new ModernWorldService.Mapping(b,c.attackCamp().get(0),1_000_000,1_000_000,0,false,true).withSiege(c);
            rooms=new ModernRooms(Set.of(b.key()),Set.of(b.key()));
            service=new ModernWorldService(rooms,Map.of(b.key(),mapping),limits(),bound->{if(bound==2){roleDraws.incrementAndGet();return attacker;}return bound-1;},journal);
            for(int i=0;i<2;i++){
                int source=i==0?7:42;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(3333,"human-"+i,1,0,80);
                var select=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"server-"+source,row.name(),80,1,0);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,select));rooms.enterLobby(owner,select,1,0,true);
                var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(source,3333,1,0),row);players.add(p);
                var s=i==0?null:room().summary();command(p,i==0?ModernRooms.Action.CREATE:ModernRooms.Action.JOIN,s,0,i==0?ModernRoomsTest.draft(100,3,547,0):null);
                register(p,26,null);
            }
            var p=players.get(0);var s=room().summary();var grant=new ModernGmCommands.Snapshot(7,"fixture",p.row(),200,false,false,new ModernGmCommands.Definition("de",200,"L1SpawnDe",false));
            var result=rooms.spawnBots(p.owner(),p.scope(),s.id(),s.revision(),28,grant);ModernRoomsTest.accepted(result.result());assertEquals(28,result.added());
            for(var player:players)command(player,ModernRooms.Action.READY,room().summary(),1,null);
        }
        void register(ModernBattleEngineBotTest.Player p,int version,ModernWorldService.PhysicalReader custom){
            service.register(p.owner(),p.scope(),deadline->{fail("combat must read selected physical source");return null;},custom!=null?custom:deadline->ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.row(),100,100)),
                    true,null,false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))));
        }
        void command(ModernBattleEngineBotTest.Player p,ModernRooms.Action action,ModernRooms.Summary s,int value,ModernRooms.Draft draft){ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,s==null?0:s.id(),s==null?0:s.revision(),value,draft,"")));}
        ModernRooms.RoomSnapshot room(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        ModernWorldProtocol.Frame world(){var p=players.get(0);return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST)).frame();}
        ModernWorldProtocol.Reply prepare(){var p=players.get(0);var s=room().summary();return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));}
        ModernWorldProtocol.Frame active()throws Exception{
            var r=prepare();assertEquals(ModernWorldProtocol.Status.OK,r.status());assertNotNull(r.frame().siege());assertEquals(1,journal.reserved.get());assertEquals(1,roleDraws.get());
            ModernNpcServiceTest.await(()->world().phase()!=ModernWorldProtocol.Phase.PREPARING);var w=world();assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase());
            for(var p:players){var reply=service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,w.matchId(),w.generation(),0,0,0,w.manifest().digest()));assertEquals(ModernWorldProtocol.Status.OK,reply.status());
                if(p==players.get(0))assertEquals(ModernWorldProtocol.Phase.LOADING,reply.frame().phase());}
            w=world();assertEquals(ModernWorldProtocol.Phase.ACTIVE,w.phase());return w;
        }
        ModernSiegeProtocol.Reply send(int who,ModernSiegeProtocol.Action action,long sequence,ModernSiege.Slot slot)throws Exception {
            var p=players.get(who);var w=world();var q=new ModernSiegeProtocol.Request(p.scope(),w.siege().snapshot().state().scope(),action,sequence,slot);
            var message=new ModernApplicationProtocol.SiegeResult(77,q,service.siege(p.owner(),q));
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,26),26));return message.reply();
        }
        void walk(int who,ModernWorldMap.Cell target)throws Exception {var p=players.get(who);
            for(int i=0;i<100;i++){var w=world();var a=w.actors().get(who);var from=a.position();if(from.equals(target))return;Thread.sleep(2);
                int dx=Integer.compare(target.x(),from.x()),dy=Integer.compare(target.y(),from.y());int heading=dx==0?(dy<0?0:4):dy==0?(dx>0?2:6):dx>0?(dy<0?1:3):(dy<0?7:5);
                var result=service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.MOVE,heading,0,0,w.matchId(),w.generation(),a.lastMoveSequence()+1,from.x(),from.y(),ModernWorldProtocol.ZERO_DIGEST));assertEquals(ModernWorldProtocol.Status.OK,result.status());}
            fail("walk budget exhausted");
        }
        void leave(int who,ModernWorldProtocol.Frame w){var p=players.get(who);assertEquals(ModernWorldProtocol.Status.OK,service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST)).status());}
        @Override public void close()throws Exception{service.close();assertTrue(service.awaitTermination(5000));for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
    }
    @Test void bothAssignedRolesTravelThroughGateCaptureJournalReceiptAndNormalReturn()throws Exception {
        for(int attacker:List.of(0,1)){var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new Fixture(attacker,60000,journal)) {
            var w=f.active();assertEquals(attacker,w.siege().snapshot().state().attacker());assertEquals(30,w.actors().size());
            assertEquals(f.mapping.siege().attackCamp().get(0),w.actors().get(attacker).position());
            assertEquals(ModernSiegeProtocol.Reason.ATTACKER,f.send(attacker,ModernSiegeProtocol.Action.PORTAL,1,null).reason());
            assertEquals(ModernSiegeProtocol.Reason.SAME_TEAM,f.send(1-attacker,ModernSiegeProtocol.Action.ATTACK_TOWER,1,ModernSiege.Slot.OUTER_LEFT).reason());
            assertEquals(ModernSiegeProtocol.Reason.NONE,f.send(attacker,ModernSiegeProtocol.Action.ATTACK_TOWER,1,ModernSiege.Slot.OUTER_LEFT).reason());
            f.walk(attacker,ModernSiegeTest.cell(11,10));Thread.sleep(2);assertEquals(ModernSiegeProtocol.Reason.NONE,f.send(attacker,ModernSiegeProtocol.Action.ATTACK_TOWER,2,ModernSiege.Slot.OUTER_RIGHT).reason());
            f.walk(attacker,ModernSiegeTest.cell(29,10));f.walk(attacker,ModernSiegeTest.cell(29,9));f.walk(attacker,ModernSiegeTest.cell(31,9));Thread.sleep(2);
            var done=f.send(attacker,ModernSiegeProtocol.Action.ATTACK_TOWER,3,ModernSiege.Slot.INNER).frame().world();assertEquals(ModernWorldProtocol.Phase.FINISHED,done.phase());
            assertEquals(journal.event,done.siege().event(done));assertEquals(1,journal.records.get());assertEquals(attacker,journal.event.siege().winner());
            journal.state=ModernMatchSettlement.ReceiptState.SAVED;var saved=f.world();assertEquals(ModernWorldProtocol.SettlementState.SAVED,saved.siege().receipt().state());assertEquals(journal.event,saved.siege().event(saved));
            f.leave(0,saved);f.leave(1,saved);assertNull(f.world());assertFalse(f.rooms.isPreparing(f.players.get(0).owner()));
        }assertEquals(1,journal.released.get());}
    }
    @Test void timeoutPersistsDefenderWinButActiveLeaveIsUnscoredAbort()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new Fixture(1,80,journal)){f.active();ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);
            assertEquals(0,journal.event.siege().winner());assertEquals(ModernSiegeSettlement.End.TIMEOUT,journal.event.siege().end());assertEquals(1,journal.records.get());}
        journal=new ModernTimeAttackServiceTest.Journal();try(var f=new Fixture(0,60000,journal)){var w=f.active();f.leave(1,w);assertEquals(ModernWorldProtocol.Phase.ABORTED,f.world().phase());assertNull(journal.event);assertEquals(1,journal.released.get());}
    }
    @Test void oldPeerFailsAdmissionBeforeSourceReadAndDoesNotReserveResult()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new Fixture(0,60000,journal)){
            var p=f.players.get(1);f.register(p,25,d->{fail("unqualified peer must not read source");return null;});
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,journal.reserved.get());assertNull(f.world());assertFalse(f.rooms.isPreparing(p.owner()));
        }
    }
    @Test void cancellationWhileSourceBlockedPublishesAbortWithGeometryAndReleasesReservation()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture(0,60000,journal)){var p=f.players.get(0);f.register(p,26,d->{entered.countDown();release.await(2,TimeUnit.SECONDS);return ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(7,p.row(),100,100));});
            var r=f.prepare();assertEquals(ModernWorldProtocol.Status.OK,r.status());assertTrue(entered.await(2,TimeUnit.SECONDS));f.leave(1,r.frame());release.countDown();
            assertEquals(ModernWorldProtocol.Phase.ABORTED,f.world().phase());assertNotNull(f.world().siege());assertNull(journal.event);assertEquals(1,journal.released.get());
        }finally{release.countDown();}
    }
    @Test void noLegacyWriterOrAbsentJournalCanAdmitSiege()throws Exception {
        var b=ModernSiegeTest.binding(547);var c=ModernSiegeConfigurationTest.policy(50,60000);var m=new ModernWorldService.Mapping(b,c.attackCamp().get(0),1_000_000,1_000_000).withSiege(c);
        var rooms=new ModernRooms(Set.of(b.key()));assertThrows(IllegalArgumentException.class,()->new ModernWorldService(rooms,Map.of(b.key(),m),limits()));
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService(rooms,Map.of(b.key(),m),limits(),n->0,(e,d)->{}));
    }
}
