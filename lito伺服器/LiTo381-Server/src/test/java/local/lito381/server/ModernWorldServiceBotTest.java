package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** True owned preparation/settlement workers over synthetic providers; deliberately no JDBC or sockets. */
class ModernWorldServiceBotTest {
    private static final String ZERO = "0".repeat(64);
    private static final long STEP = 1000000L, ATTACK = 1000000L, CAST = 10000000L;

    @Test void prospectiveArmorProviderCannotLeakUnrepresentedEffectsIntoExistingPublicWorld()throws Exception {
        try(var f=new Fixture(1,true)) {
            f.register(deadline->{
                f.physicalReads.incrementAndGet();var p=f.player();
                var source=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.row(),1000,1000,0);
                return ModernArmorProtectionLoadoutTest.qualify(ModernArmorProtectionLoadoutTest.protection(source));
            });
            assertEquals(Status.OK,f.prepare().status());await(()->f.read().phase()==Phase.ABORTED);
            assertEquals(1,f.physicalReads.get());assertEquals(0,f.skillsReads.get());assertEquals(0,f.persisted.get());
            assertTrue(f.read().actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
        }
    }

    @Test void ninetyNineBotsConsumeNoFakeHumanRegistrationsReadersOrLoadAcknowledgements() throws Exception {
        try (var f = new Fixture(99, true)) {
            var loading = f.loading();
            assertEquals(100, loading.actors().size()); assertEquals(1, f.service.registrationCount());
            assertEquals(1, f.physicalReads.get()); assertEquals(1, f.skillsReads.get()); assertEquals(0, f.fallbackReads.get());
            assertFalse(loading.actors().get(0).loaded());
            assertTrue(loading.actors().subList(1, 100).stream().allMatch(ModernBattleEngine.Actor::loaded));
            assertEquals(99, loading.actors().stream().filter(a -> !a.member().identity().isHuman()).count());
            var ready = f.service.handle(f.player().owner(), loaded(f.player(), loading));
            assertEquals(Status.OK, ready.status()); assertEquals(Phase.ACTIVE, ready.frame().phase());
            var state = f.service.combat(f.player().owner(), new ModernCombatProtocol.Request(f.player().scope(),
                    ModernCombatProtocol.Action.READ, loading.matchId(), loading.generation(), 0, 0)).frame();
            assertEquals(100, state.vitals().size()); assertEquals(100, state.damages().size());
            assertTrue(state.vitals().subList(1, 100).stream().allMatch(v -> v.hp() == 40 && v.maxHp() == 40 && v.mp() == 0));
            assertTrue(state.damages().subList(1, 100).stream().allMatch(d -> d.castCount() == 0));
            f.service.handle(f.player().owner(), leave(f.player(), ready.frame()));
            await(f.service::isQuiescent); assertEquals(0, f.service.matchCount());
        }
    }

    @Test void oneHumanWalksOutsideTheEntireBirthHaloWithoutMovingOrDamagingAnyOfThreeEightTwentyOrNinetyNineBots() throws Exception {
        for (int bots : java.util.List.of(3,8,20,99)) try (var f = new Fixture(bots,true,ModernBattleEngineBotTest.cell(0,0))) {
            var loading=f.loading();f.service.handle(f.player().owner(),loaded(f.player(),loading));
            var before=f.service.combat(f.player().owner(),new ModernCombatProtocol.Request(f.player().scope(),
                    ModernCombatProtocol.Action.READ,loading.matchId(),loading.generation(),0,0)).frame();
            var seats=loading.actors().stream().map(ModernBattleEngine.Actor::position).toList();
            var goal=ModernWorldSpawns.qualify(f.room.map,loading.room().members(),seats,java.util.Set.of(),()->false);
            var route=ModernWorldSpawnsTest.route(f.room.map,seats,java.util.Set.of(),0,goal);
            assertTrue(route.size()>1);Frame last=loading;
            for(int i=1;i<route.size();i++) {
                var from=route.get(i-1);var to=route.get(i);int heading=heading(from,to);
                Thread.sleep(2); // Existing one-millisecond fixture cadence, no production acceleration.
                var moved=f.service.handle(f.player().owner(),new Request(f.player().scope(),Action.MOVE,heading,0,0,
                        loading.matchId(),loading.generation(),i,from.x(),from.y(),ZERO));
                assertEquals(Status.OK,moved.status());last=moved.frame();
                assertEquals(to,last.actors().get(0).position());
                assertEquals(loading.actors().subList(1,bots+1),last.actors().subList(1,bots+1));
            }
            assertTrue(ModernWorldSpawnsTest.outside(last.actors().get(0).position(),seats));
            var after=f.service.combat(f.player().owner(),new ModernCombatProtocol.Request(f.player().scope(),
                    ModernCombatProtocol.Action.READ,loading.matchId(),loading.generation(),0,0)).frame();
            assertEquals(before.vitals(),after.vitals());assertEquals(before.damages(),after.damages());
            assertEquals(before.world().casts(),after.world().casts());assertNull(after.world().completion());
            assertEquals(0,f.persisted.get());assertEquals(1,f.physicalReads.get());assertEquals(1,f.skillsReads.get());
            f.service.handle(f.player().owner(),leave(f.player(),last));await(f.service::isQuiescent);
        }
    }
    private static int heading(ModernWorldMap.Cell from,ModernWorldMap.Cell to) {
        int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
        for(int i=0;i<8;i++)if(to.x()-from.x()==dx[i]&&to.y()-from.y()==dy[i])return i;
        throw new AssertionError("route must contain actual adjacent moves");
    }

    @Test void actualServiceStoresTheCompleteTypedFinalEventOnceAndReturnsTheOriginalRoom() throws Exception {
        try (var f = new Fixture(1, true)) {
            var loading = f.loading(); f.service.handle(f.player().owner(), loaded(f.player(), loading));
            ModernCombatProtocol.Reply last = null;
            for (long sequence = 1; sequence <= 3; sequence++) {
                last = f.service.combat(f.player().owner(), new ModernCombatProtocol.Request(f.player().scope(),
                        ModernCombatProtocol.Action.ATTACK, loading.matchId(), loading.generation(), sequence, 2));
                assertEquals(ModernCombatProtocol.Status.OK, last.status()); Thread.sleep(2);
            }
            assertEquals(Phase.FINISHED, last.frame().world().phase());
            var finished = last.frame().world();
            await(() -> f.read().completion().state() == SettlementState.SAVED);
            assertEquals(1, f.persisted.get()); assertNotNull(f.event.get()); assertTrue(f.event.get().hasBots());
            assertEquals(finished.completion().event(), f.event.get());
            assertEquals(ModernParticipantId.human(7, 1), f.event.get().participants().get(0).identity());
            assertEquals(ModernParticipantId.bot(7, 1), f.event.get().participants().get(1).identity());
            assertEquals(0, last.frame().vitals().get(1).hp());
            assertEquals(finished.completion().event().engineRevision() + 1, last.frame().vitals().get(1).deathRevision());
            var reply = f.service.handle(f.player().owner(), leave(f.player(), f.read()));
            assertEquals(Status.OK, reply.status()); assertNull(reply.frame());
            await(f.service::isQuiescent); assertEquals(0, f.service.matchCount());
            assertEquals(loading.room().summary().id(), f.room.view().summary().id());
            assertEquals(2, f.room.view().members().size());
            assertFalse(f.room.view().members().get(0).ready()); assertTrue(f.room.view().members().get(1).ready());
        }
    }

    @Test void oldMappingCannotSilentlyAcceptBotWorldsAndRejectionDoesNotReadAnySource() throws Exception {
        try (var f = new Fixture(1, false)) {
            var reply = f.prepare();
            assertEquals(Status.UNAVAILABLE, reply.status()); assertEquals(Reason.NO_BINDING, reply.reason());
            assertEquals(0, f.physicalReads.get()); assertEquals(0, f.skillsReads.get());
            assertEquals(0, f.service.matchCount()); assertFalse(f.room.rooms.isPreparing(f.player().owner()));
            assertFalse(f.mapping.serverBots());
            assertThrows(IllegalArgumentException.class, () -> new ModernWorldService.Mapping(f.room.binding,
                    ModernBattleEngineBotTest.cell(10, 10), STEP, 0, 0, false, true));
        }
    }

    @Test void closeDuringRealHumanReadKeepsWorkerOwnedAndNeverPublishesAReadyBotOnlyEngine() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var f = new Fixture(2, true)) {
            f.register(deadline -> {
                f.physicalReads.incrementAndGet(); entered.countDown();
                long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (release.getCount() != 0 && System.nanoTime() < limit) {
                    try { release.await(20, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { /* actual work remains owned */ }
                }
                if (release.getCount() != 0) throw new IllegalStateException("test reader deadline");
                return f.fighter();
            });
            var preparing = f.prepare(); assertEquals(Status.OK, preparing.status());
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertTrue(preparing.frame().actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
            assertEquals(Phase.PREPARING, preparing.frame().phase());
            f.service.close(); assertFalse(f.service.awaitTermination(10)); assertEquals(1, f.service.active());
            release.countDown(); assertTrue(f.service.awaitTermination(3000));
            assertEquals(0, f.service.active()); assertEquals(0, f.service.registrationCount());
            assertEquals(0, f.service.matchCount()); assertEquals(0, f.skillsReads.get()); assertEquals(0, f.persisted.get());
        } finally { release.countDown(); }
    }

    private static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;
        final ModernWorldService.Mapping mapping; final ModernWorldService service;
        final AtomicInteger physicalReads = new AtomicInteger(), skillsReads = new AtomicInteger(), fallbackReads = new AtomicInteger();
        final AtomicInteger persisted = new AtomicInteger();
        final AtomicReference<ModernMatchSettlement.Event> event = new AtomicReference<>();
        Fixture(int bots, boolean enabled) throws Exception {
            this(bots,enabled,ModernBattleEngineBotTest.cell(10,10));
        }
        Fixture(int bots, boolean enabled, ModernWorldMap.Cell anchor) throws Exception {
            room = new ModernBattleEngineBotTest.RoomFixture(1, bots);
            mapping = enabled ? new ModernWorldService.Mapping(room.binding, anchor,
                    STEP, ATTACK, CAST, true, true) : new ModernWorldService.Mapping(room.binding,
                    ModernBattleEngineBotTest.cell(10, 10), STEP, ATTACK, CAST, true);
            service = new ModernWorldService(room.rooms, Map.of(room.binding.key(), mapping),
                    new ModernWorldService.Limits(2, 1, 0, 2000, 5, new ModernBattleEngine.Limits(2000, 0, 10000)),
                    bound -> bound - 1, (value, deadline) -> { event.set(value); persisted.incrementAndGet(); });
            register(deadline -> { physicalReads.incrementAndGet(); return fighter(); });
        }
        ModernBattleEngineBotTest.Player player() { return room.players.get(0); }
        ModernPhysicalCombat.Fighter fighter() {
            return ModernMagicCombatTest.createFighter(player().scope().sourceId(), player().row(), 100, 30, 12, 0);
        }
        void register(ModernWorldService.PhysicalReader reader) {
            var p = player();
            service.register(p.owner(), p.scope(), deadline -> { fallbackReads.incrementAndGet(); return p.row(); }, reader,
                    true, deadline -> { skillsReads.incrementAndGet(); return new ModernSkills.Snapshot(p.scope().sourceId(),
                            p.row(), ModernBattleEngineBotTest.learned()); }, true);
        }
        Reply prepare() {
            var s = room.view().summary();
            return service.handle(player().owner(), new Request(player().scope(), Action.PREPARE, 0,
                    s.id(), s.revision(), 0, 0, 0, 0, 0, ZERO));
        }
        Frame read() { return service.handle(player().owner(), new Request(player().scope(), Action.READ,
                0, 0, 0, 0, 0, 0, 0, 0, ZERO)).frame(); }
        Frame loading() throws Exception {
            var reply = prepare(); assertEquals(Status.OK, reply.status());
            await(() -> { var value = read(); assertNotEquals(Phase.ABORTED, value.phase()); return value.phase() == Phase.LOADING; });
            return read();
        }
        @Override public void close() throws Exception {
            service.close(); assertTrue(service.awaitTermination(4000)); room.close();
        }
    }
    private static Request loaded(ModernBattleEngineBotTest.Player p, Frame frame) {
        return new Request(p.scope(), Action.LOADED, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, frame.manifest().digest());
    }
    private static Request leave(ModernBattleEngineBotTest.Player p, Frame frame) {
        return new Request(p.scope(), Action.LEAVE, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, ZERO);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) { assertTrue(System.nanoTime() < deadline, "bounded BOT service completion"); Thread.sleep(5); }
    }
}
