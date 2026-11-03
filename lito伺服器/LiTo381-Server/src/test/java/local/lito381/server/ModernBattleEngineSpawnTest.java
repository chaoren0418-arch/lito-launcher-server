package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernBattleEngineSpawnTest {
    @Test void unsafeInitialPlacementFailsBeforeClaimAndSafeReplacementCanUseTheSameTicket() throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(1,3)) {
            var ticket=room.capture();var roster=ticket.snapshot().members();
            var trapped=room.map.serverChosenSpawns(ModernBattleEngineBotTest.cell(0,0),4);
            var calls=new AtomicInteger();
            assertThrows(IllegalArgumentException.class,()->create(room,ticket,trapped,()->false,()->1000,calls));
            assertEquals(0,calls.get());
            var plan=ModernWorldSpawns.allocate(room.map,roster,List.of(ModernBattleEngineBotTest.cell(0,0)),Set.of(),()->false);
            try(var engine=create(room,ticket,plan.positions(),()->false,()->1000,calls)) {
                assertEquals(1,engine.ownerCount());assertEquals(ModernBattleEngine.Phase.LOADING,engine.tick().phase());
                assertEquals(plan.positions(),engine.tick().actors().stream().map(ModernBattleEngine.Actor::position).toList());
                assertEquals(0,calls.get());
            }
        }
    }

    @Test void originalPreparationCancellationStopsQualificationWithoutConsumingTheCapability() throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(1,1)) {
            var ticket=room.capture();var positions=room.map.serverChosenSpawns(ModernBattleEngineBotTest.cell(10,10),2);
            var checks=new AtomicInteger();var random=new AtomicInteger();
            assertThrows(IllegalStateException.class,()->create(room,ticket,positions,()->checks.incrementAndGet()>20,()->1000,random));
            assertTrue(checks.get()>20);assertEquals(0,random.get());
            try(var engine=create(room,ticket,positions,()->false,()->1000,random)){assertEquals(1,engine.ownerCount());}
        }
    }

    @Test void deadlineThatExpiresAfterPureQualificationStillCannotClaimTheEngine() throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(1,1)) {
            var ticket=room.capture();var positions=room.map.serverChosenSpawns(ModernBattleEngineBotTest.cell(10,10),2);
            var expired=new AtomicBoolean();var calls=new AtomicInteger();
            assertThrows(IllegalStateException.class,()->create(room,ticket,positions,expired::get,
                    ()->{expired.set(true);return 1000;},calls));
            assertTrue(expired.get());assertEquals(0,calls.get());
            try(var engine=create(room,ticket,positions,()->false,()->1000,calls)){assertEquals(1,engine.ownerCount());}
        }
    }

    private static ModernBattleEngine create(ModernBattleEngineBotTest.RoomFixture room,ModernRooms.FrozenPreparation ticket,
            List<ModernWorldMap.Cell> positions,java.util.function.BooleanSupplier cancelled,
            java.util.function.LongSupplier clock,AtomicInteger random) {
        var old=room.prepared(ticket).get(0);var first=positions.get(0);
        var prepared=new ModernBattleEngine.PreparedMember(old.owner(),old.scope(),old.character(),first,
                old.stepIntervalNanos(),old.fighter(),old.attackIntervalNanos(),old.skills(),old.castIntervalNanos(),old.damageMagic());
        var bots=ticket.bots().stream().map(m->new ModernBattleEngine.BotPrepared(m.identity(),
                ModernBotTemplate.roomTestDummy(m.characterClass(),m.sex()),positions.get(ticket.snapshot().members().indexOf(m)))).toList();
        return new ModernBattleEngine(ticket,room.binding,List.of(prepared),bots,ModernBattleEngineBotTest.LIMITS,
                clock,()->1000,bound->{random.incrementAndGet();return bound-1;},false,null,cancelled);
    }
}
