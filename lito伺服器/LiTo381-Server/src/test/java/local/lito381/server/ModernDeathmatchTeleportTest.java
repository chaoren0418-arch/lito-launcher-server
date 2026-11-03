package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Real room capability and engine; synthetic PCs/RNG, no production SQL. */
class ModernDeathmatchTeleportTest {
    static final ModernWorldMap.SectorKey SECTOR=new ModernWorldMap.SectorKey(0x8000,0x8000);
    static final long CADENCE=1_200_000_000L;
    static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(SECTOR.origin().x()+x,SECTOR.origin().y()+y);}
    static ModernWorldMap islands()throws Exception {
        byte[] raw=ModernWorldMapTest.openSectorBytes();
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)ModernWorldMapTest.setAttributes(raw,x,y,(short)1,(short)1);
        for(int y=2;y<=10;y++)for(int x=2;x<=10;x++)
            ModernWorldMapTest.setAttributes(raw,x,y,(short)(y>2?0:1),(short)(x<10?0:1));
        ModernWorldMapTest.setAttributes(raw,50,50,(short)1,(short)0);
        return ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,raw),()->false);
    }
    @Test void nativeStandingIsNotEnoughToAdmitAnUnreachableIslandDestination()throws Exception {
        var map=islands();var groups=ModernDeathmatchSharedMapTest.components(map);assertEquals(List.of(81,2),groups.stream().map(Set::size).toList());
        var bound=new AtomicLong();
        try(var f=new Fixture(map,List.of(cell(3,3),cell(4,3)),b->{bound.set(b);return b-1;})) {
            var before=f.active();var reply=f.cast(0,1);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());
            assertTrue(groups.get(0).contains(reply.snapshot().actors().get(0).position()),"teleport must remain in the qualified battle arena");
            assertEquals(79,bound.get(),"only this arena minus two occupied positions is eligible");
            assertEquals(before.vitals().get(0).mp()-10,reply.snapshot().vitals().get(0).mp());
            f.walkAfterCast(reply.snapshot());
        }
    }
    @Test void actualShared531TinyIslandCannotBeChosenByTheAuthoritativeRng()throws Exception {
        actualMapStaysInArena(531);
    }
    @Test void actualShared551And552AlsoStayInTheirOwnSpawnArena()throws Exception {
        actualMapStaysInArena(551);actualMapStaysInArena(552);
    }
    @Test void arenaSelectionUsesTheActualSpawnNotTheLargestOrLowestIsland()throws Exception {
        var map=islands();var groups=ModernDeathmatchSharedMapTest.components(map);
        for(var component:groups) {
            var spawn=component.iterator().next();var actual=ModernDeathmatchTeleportSpace.candidates(map,spawn,()->false);
            assertEquals(map.teleportCells().stream().filter(component::contains).toList(),actual);
            assertThrows(UnsupportedOperationException.class,()->actual.clear());
        }
    }
    @Test void connectedMultiSectorMapKeepsEveryCandidateInItsOriginalRngOrder()throws Exception {
        var next=new ModernWorldMap.SectorKey(SECTOR.x()+1,SECTOR.y());
        var map=ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,ModernWorldMapTest.openSectorBytes(),next,ModernWorldMapTest.openSectorBytes()),()->false);
        assertEquals(8192,map.teleportCells().size());
        assertEquals(map.teleportCells(),ModernDeathmatchTeleportSpace.candidates(map,next.origin(),()->false));
    }
    @Test void cancellationDuringTraversalOrOrderingAndInterruptionDoNotPoisonTheMap()throws Exception {
        var map=ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        // 16 checks in each full-sector traversal/order pass, plus boundaries.
        for(int cutoff:List.of(0,3,20,33)) {
            var checks=new java.util.concurrent.atomic.AtomicInteger();
            var error=assertThrows(ModernWorldMap.MapException.class,()->ModernDeathmatchTeleportSpace.candidates(map,cell(4,4),()->checks.incrementAndGet()>cutoff));
            assertEquals(ModernWorldMap.Failure.CANCELLED,error.failure());
        }
        Thread.currentThread().interrupt();
        try {assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernDeathmatchTeleportSpace.candidates(map,cell(4,4),()->false)).failure());}
        finally {assertTrue(Thread.interrupted());}
        assertEquals(map.teleportCells(),ModernDeathmatchTeleportSpace.candidates(map,cell(4,4),()->false));
        for(var invalid:List.of(cell(-1,0),new ModernWorldMap.Cell(Integer.MAX_VALUE,Integer.MIN_VALUE)))
            assertEquals(ModernWorldMap.Failure.INVALID_SPAWNS,assertThrows(ModernWorldMap.MapException.class,
                    ()->ModernDeathmatchTeleportSpace.candidates(map,invalid,()->false)).failure());
    }
    @Test void repeatedEndpointDrawsStayInTheArenaAndChargeExactlyOneCastEach()throws Exception {
        var map=islands();var arena=ModernDeathmatchSharedMapTest.components(map).get(0);var draws=new AtomicLong();
        try(var f=new Fixture(map,List.of(cell(3,3),cell(4,3)),b->draws.getAndIncrement()%2==0?0:b-1)) {
            var before=f.active();
            for(int sequence=1;sequence<=100;sequence++) {
                var reply=f.cast(0,sequence);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());var s=reply.snapshot();
                assertTrue(arena.contains(s.actors().get(0).position()));assertNotEquals(s.actors().get(1).position(),s.actors().get(0).position());
                assertEquals(sequence,s.casts().get(0).castCount());assertEquals(sequence*10,s.casts().get(0).mpSpent());
                assertEquals(before.vitals().get(0).mp()-sequence*10,s.vitals().get(0).mp());assertEquals(before.vitals().get(1),s.vitals().get(1));
                f.clock.addAndGet(CADENCE);
            }assertEquals(100,draws.get());
        }
    }
    @Test void cancellationAfterATeleportFreezesItsPositionMpAndEvent()throws Exception {
        try(var f=new Fixture(islands(),List.of(cell(3,3),cell(4,3)),b->b-1)) {
            f.active();var before=f.cast(0,1).snapshot();f.rooms.rooms.releasePreparation(f.ticket);f.clock.addAndGet(CADENCE);
            var denied=f.cast(0,2);assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,denied.reason());
            assertEquals(ModernBattleEngine.Phase.ABORTED,denied.snapshot().phase());
            assertEquals(before.actors(),denied.snapshot().actors());assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.casts(),denied.snapshot().casts());
        }
    }
    static void actualMapStaysInArena(int id)throws Exception {
        var mapping=ModernWorldConfiguration.parse(ModernSharedDeathmatchPresetTest.worlds(ModernDeathmatchSharedMapTest.sharedRoot(),List.of(id)))
                .load().get(new ModernWorldBinding.Key(1,id));
        var map=mapping.binding().map();var groups=ModernDeathmatchSharedMapTest.components(map);var arena=groups.get(0);
        var positions=map.serverChosenSpawns(mapping.anchor(),2);var old=map.teleportCells().stream().filter(c->!positions.contains(c)).toList();
        int unreachable=-1;for(int i=0;i<old.size();i++)if(!arena.contains(old.get(i))){unreachable=i;break;}
        assertTrue(unreachable>=0,"real disconnected geometry required for this regression");
        final int target=unreachable;var bound=new AtomicLong();
        try(var f=new Fixture(map,positions,b->{bound.set(b);return Math.min(target,b-1);})) {
            f.active();var reply=f.cast(0,1);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());
            assertTrue(arena.contains(reply.snapshot().actors().get(0).position()),"shared map "+id+" left the battle component");
            assertEquals(arena.size()-2,bound.get());f.walkAfterCast(reply.snapshot());
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernWorldMap map;final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(1_000_000_000L);
        Fixture(ModernWorldMap map,List<ModernWorldMap.Cell> positions,ModernPhysicalCombat.BoundedRandom random)throws Exception {
            this.map=map;players.add(rooms.player(7,3333,30,0));players.add(rooms.player(42,3333,30,0));
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(2,1,map.mapId(),0)));
            ModernRoomsTest.accepted(rooms.join(players.get(1),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();
            var preparation=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(preparation.result());ticket=preparation.ticket();
            var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<ticket.participants().size();i++) {
                var p=ticket.participants().get(i);var m=p.member();
                var row=new ModernApplicationProtocol.CharacterRow(m.key().objectId(),m.name(),m.characterClass(),m.sex(),m.level());
                var source=ModernPhysicalCombatTest.fixtureSource(m.key().sourceId(),row,100,10,0);
                var equipment=ModernCombatProfileTest.withVitals(source.input().equipment(),100,5000,999,999,10);
                source=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(equipment,true,source.input().weapons()),source.growth(),source.templates(),source.procCatalogIds());
                var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(equipment),Optional.empty(),0);
                var skills=new ModernSkills.Snapshot(m.key().sourceId(),row,List.of(new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT)));
                prepared.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,positions.get(i),200_000_000L,fighter,700_000_000L,skills,CADENCE));
            }
            try {engine=new ModernBattleEngine(ticket,new ModernWorldBinding(1,map.mapId(),map),prepared,new ModernBattleEngine.Limits(10000,0,600000),clock::get,()->1000L,random,false);}
            catch(RuntimeException failure){retireRoom();throw failure;}
        }
        ModernBattleEngine.Snapshot active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());var s=engine.tick();assertEquals(ModernBattleEngine.Phase.ACTIVE,s.phase());return s;}
        ModernBattleEngine.CastResult cast(int who,long sequence){var p=players.get(who);return engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),sequence,5));}
        void walkAfterCast(ModernBattleEngine.Snapshot snapshot) {
            var actor=snapshot.actors().get(0);var occupied=snapshot.actors().stream().map(ModernBattleEngine.Actor::position).collect(java.util.stream.Collectors.toSet());
            int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};int heading=-1;
            for(int h=0;h<8;h++){var to=new ModernWorldMap.Cell(actor.position().x()+dx[h],actor.position().y()+dy[h]);if(map.canStep(actor.position(),to)&&!occupied.contains(to)){heading=h;break;}}
            assertTrue(heading>=0,"teleported human has an immediate free walking edge");clock.addAndGet(CADENCE);var p=players.get(0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),actor.lastMoveSequence()+1,actor.position().x(),actor.position().y(),heading).status());
        }
        void retireRoom(){rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
        @Override public void close(){engine.close();retireRoom();assertEquals(0,rooms.rooms.memberCount());}
    }
}
