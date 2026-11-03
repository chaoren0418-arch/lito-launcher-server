package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldPolicyServiceTest {
    @TempDir Path temporary;
    @Test void sqlEffectiveTeamCampsAndClocksReachActualPreparationWithoutChangingFrozenMappingsOrResultPolicy()throws Exception{
        byte[] bytes=ModernWorldMapTest.openSectorBytes();Files.write(Files.createDirectories(temporary.resolve("map/514")).resolve("80008000.s32"),bytes);
        var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),bytes),()->false);
        var key=ModernWorldPolicyTest.KEY;var config=ModernWorldConfiguration.parse(ModernWorldPolicyTest.properties(temporary,map,List.of(key)));
        var camps=List.of(new ModernWorldMap.Cell(32790,32790),new ModernWorldMap.Cell(32810,32810));
        var maps=config.load(ModernWorldPolicyTest.snapshot(1,List.of(ModernWorldPolicyTest.row(map,key,camps.get(0),camps,new ModernWorldService.ClockOverrides(2000,0L,500)))),()->false);
        var rooms=new ModernRoomsTest.Fixture();var a=rooms.player(7,3333,80,0);var b=rooms.player(42,3333,80,0);
        var service=new ModernWorldService(rooms.rooms,maps,new ModernWorldService.Limits(2,1,1,2000,5,new ModernBattleEngine.Limits(30000,3_000_000_000L,45000)),
                bound->{throw new AssertionError("layout-only Scene must not manufacture combat RNG");});
        try {
            for(var p:List.of(a,b))service.register(p.owner(),p.scope(),deadline->{var selected=rooms.presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(3333,selected.characterName(),selected.characterClass(),selected.sex(),80);},
                    null,false,null,false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,15,1,65903));
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
            for(var p:List.of(a,b))ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));var room=rooms.view(a).joined().summary();
            var prepared=service.handle(a.owner(),new Request(a.scope(),Action.PREPARE,0,room.id(),room.revision(),0,0,0,0,0,"0".repeat(64)));
            assertEquals(Status.OK,prepared.status());assertEquals(Phase.PREPARING,prepared.frame().phase());assertEquals(500,prepared.frame().descriptor().activeTimeoutMillis());
            var frame=waitPhase(service,a,Phase.LOADING);
            for(var actor:frame.actors())assertEquals(camps.get(actor.member().team()),actor.position());
            var next=config.load(ModernWorldPolicyTest.snapshot(2,List.of(ModernWorldPolicyTest.row(map,key,new ModernWorldMap.Cell(32820,32820),List.of(),new ModernWorldService.ClockOverrides(null,2L,9000)))),()->false);
            assertEquals(camps,maps.get(key).spawnCamps());assertNotEquals(maps.get(key).anchor(),next.get(key).anchor());
            for(var p:List.of(a,b))service.handle(p.owner(),new Request(p.scope(),Action.LOADED,0,0,0,frame.matchId(),frame.generation(),0,0,0,frame.manifest().digest()));
            var active=waitPhase(service,a,Phase.ACTIVE);assertEquals(prepared.frame().descriptor(),active.descriptor());
            var own=active.actors().stream().filter(actor->actor.member().identity().sourceId()==a.scope().sourceId()).findFirst().orElseThrow();
            var moved=service.handle(a.owner(),new Request(a.scope(),Action.MOVE,0,0,0,active.matchId(),active.generation(),1,own.position().x(),own.position().y(),"0".repeat(64)));
            assertEquals(Status.OK,moved.status());assertEquals(1,moved.frame().actors().stream().filter(actor->actor.entityId()==own.entityId()).findFirst().orElseThrow().lastMoveSequence());
            var terminal=waitPhase(service,a,Phase.ABORTED);assertEquals(AbortReason.ACTIVE_TIMEOUT,terminal.abortReason());assertNull(terminal.completion());assertFalse(terminal.descriptor().settlementEnabled());
            for(var p:List.of(a,b))assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LEAVE,0,0,0,terminal.matchId(),terminal.generation(),0,0,0,"0".repeat(64))).status());
        }finally{service.close();assertTrue(service.awaitTermination(4000));for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}assertTrue(service.isQuiescent());}
    }
    private static Frame waitPhase(ModernWorldService service,ModernRoomsTest.Player player,Phase phase)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        for(;;){var frame=service.handle(player.owner(),new Request(player.scope(),Action.READ,0,0,0,0,0,0,0,0,"0".repeat(64))).frame();
            if(frame.phase()==phase)return frame;assertTrue(deadline-System.nanoTime()>0,"phase="+frame.phase()+" abort="+frame.abortReason());Thread.sleep(2);}
    }
}
