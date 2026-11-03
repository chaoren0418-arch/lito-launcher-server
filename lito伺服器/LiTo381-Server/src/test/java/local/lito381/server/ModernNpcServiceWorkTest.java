package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Real engine at the admitted player/NPC limits. Timings are diagnostics,
 * not machine-independent latency assertions or actual client UI evidence. */
class ModernNpcServiceWorkTest {
    static ModernBattleEngineNpcTest.Fixture fullRoom() throws Exception {
        var positions = new ArrayList<ModernWorldMap.Cell>();
        for (int y = 0; y < 10; y++) for (int x = 0; x < 10; x++)
            positions.add(ModernBattleEngineNpcTest.cell(2 + 3 * x, 2 + 3 * y));
        var map = ModernWorldMap.fromSectorBytes(554,
                Map.of(ModernBattleEngineBotTest.SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
        return new ModernBattleEngineNpcTest.Fixture(100, 0, false, 9_007_199_254_740_993L, 0,
                UnaryOperator.identity(), ModernNpcMeleeTest.catalog(), true, map, positions);
    }

    static List<ModernNpcRoster.Actor> populate(ModernBattleEngineNpcTest.Fixture f) {
        f.active();
        var rows = new ArrayList<ModernNpcRoster.Spawn>();
        var template = f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        for (int y = 30; y < 38; y++) for (int x = 30; x < 46; x++)
            rows.add(new ModernNpcRoster.Spawn(template, rows.size() % 2, 1,
                    ModernBattleEngineNpcTest.cell(x, y), 0));
        var spawned = f.engine.spawnNpcs(f.scope, rows).npcs().actors();
        assertEquals(100, f.engine.serviceObservation().actors().size());
        assertEquals(128, spawned.size());
        return spawned;
    }

    @Test void fullRoomIdleNpcTickWorkHasNoGameMutation() throws Exception {
        try (var f = fullRoom()) {
            var rows = populate(f);
            var before = f.engine.serviceObservation();
            var bean = ManagementFactory.getPlatformMXBean(com.sun.management.ThreadMXBean.class);
            assertNotNull(bean);
            assertTrue(bean.isThreadAllocatedMemorySupported());
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().getId();
            long[] allocated = new long[2];
            for (int mode = 0; mode < 2; mode++) {
                boolean unpublished = mode == 1;
                for (int round = 0; round < 8; round++) idleTick(f, rows, unpublished);
                long bytes = bean.getThreadAllocatedBytes(thread), started = System.nanoTime();
                for (int round = 0; round < 12; round++) idleTick(f, rows, unpublished);
                long elapsed = System.nanoTime() - started;
                allocated[mode] = bean.getThreadAllocatedBytes(thread) - bytes;
                assertEquals(before, f.engine.npcTickObservation());
                System.out.println("full-room NPC idle ticks players=100 npcs=128 samples=12 unpublished=" + unpublished
                        + " averageMicros=" + elapsed / 12 / 1000 + " allocatedBytesPerTick=" + allocated[mode] / 12);
            }
            var after = f.engine.serviceObservation();
            assertEquals(before, after);
            assertEquals(0, f.draws.get());
            assertTrue(allocated[1] < allocated[0], "removing discarded snapshots must reduce allocation; no timing threshold");
        }
    }

    private static void idleTick(ModernBattleEngineNpcTest.Fixture f, List<ModernNpcRoster.Actor> rows, boolean unpublished) {
        var observation = f.engine.serviceObservation();
        for (var actor : rows) {
            if (unpublished) assertEquals(ModernNpcBattle.Reason.WAIT,
                    f.engine.stepNpcUnpublished(actor.id(), actor.life(), 64, false));
            else {
                var step = f.engine.observeCommand(() -> f.engine.stepNpc(actor.id(), actor.life(), 64, false));
                assertEquals(ModernNpcBattle.Reason.WAIT, step.outcome().reason());
                observation = step.observation();
            }
        }
        if (unpublished) observation = f.engine.npcTickObservation();
        assertEquals(128, observation.npcs().roster().actors().size());
    }

    @Test void unpublishedAndObservedStepsKeepExactStateAndRandomDraws() throws Exception {
        for (boolean regen : List.of(false, true)) try (
                var old = new ModernBattleEngineNpcTest.Fixture(2,0,regen,1000,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true);
                var next = new ModernBattleEngineNpcTest.Fixture(2,0,regen,1000,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            old.active(); next.active();
            var observed = List.of(old.spawn(0,11,10),old.spawn(1,21,10),old.spawn(0,35,35));
            var unpublished = List.of(next.spawn(0,11,10),next.spawn(1,21,10),next.spawn(0,35,35));
            var outcomes = java.util.EnumSet.noneOf(ModernNpcBattle.Reason.class);
            for (long ms : new long[]{0,100,101,750,1500,3000,3100,5000}) {
                old.elapsed(ms); next.elapsed(ms);
                for (int i = 0; i < observed.size(); i++) {
                    var a=observed.get(i);var b=unpublished.get(i);
                    var expected=old.engine.stepNpc(a.id(),a.life(),1,false);
                    var actual=next.engine.stepNpcUnpublished(b.id(),b.life(),1,false);
                    outcomes.add(actual); assertEquals(expected.reason(),actual);
                    assertEquals(expected.frame(),ModernLegacyBattleProjection.npcs(next.engine.npcTickObservation()));
                    assertEquals(old.draws.get(),next.draws.get());
                }
            }
            assertTrue(outcomes.containsAll(List.of(ModernNpcBattle.Reason.ATTACKED,
                    ModernNpcBattle.Reason.TOO_FAST,ModernNpcBattle.Reason.BUDGET)));
        }
    }

    @Test void fullRoomActiveTicksPermitPlayerCommandsBetweenNpcSteps() throws Exception {
        try (var old=fullRoom();var next=fullRoom()) {
            var a=populate(old);var b=populate(next);
            int moved=0,attacked=0;
            long start=System.nanoTime();
            for (int tick=1;tick<=6;tick++) {
                old.elapsed(tick*300L);next.elapsed(tick*300L);
                for (int i=0;i<a.size();i++) {
                    if (i==64) {
                        int from=tick%2==1?2:3,heading=tick%2==1?2:6;
                        assertEquals(ModernBattleEngine.MoveStatus.MOVED,old.move(0,tick,from,2,heading).status());
                        assertEquals(ModernBattleEngine.MoveStatus.MOVED,next.move(0,tick,from,2,heading).status());
                        var position=ModernBattleEngineNpcTest.cell(tick%2==1?3:2,2);
                        assertEquals(position,next.engine.npcTickObservation().actors().get(0).pose().position());
                    }
                    var expected=old.engine.stepNpc(a.get(i).id(),a.get(i).life(),64,false);
                    var actual=next.engine.stepNpcUnpublished(b.get(i).id(),b.get(i).life(),64,false);
                    assertEquals(expected.reason(),actual);
                    if(actual==ModernNpcBattle.Reason.MOVED)moved++;
                    if(actual==ModernNpcBattle.Reason.ATTACKED)attacked++;
                }
                assertEquals(old.view(),ModernLegacyBattleProjection.npcs(next.engine.npcTickObservation()));
                assertEquals(old.draws.get(),next.draws.get());
            }
            assertTrue(moved>0);assertTrue(attacked>0);
            System.out.println("full-room paired active ticks=6 NPC moves="+moved+" attacks="+attacked
                    +" confirmed interleaved player moves=6 elapsedMicros="+(System.nanoTime()-start)/1000
                    +" (two engines + equivalence assertions, not a latency benchmark)");
        }
    }

    @Test void cooldownAttackNeighboursKeepSourceLaneAndDiagonalRules() throws Exception {
        int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
        for(int h=0;h<8;h++)for(int mask=0;mask<16;mask++)for(boolean otherLane:List.of(false,true)) {
            try(var roster=ModernNpcRosterTest.roster()) {
                var actor=ModernNpcRosterTest.one(roster);var map=ModernNpcRosterTest.map();
                var first=ModernNpcRosterTest.target(42,0,10+dx[h],10+dy[h]);
                int opposite=(h+4)%8;
                var second=ModernNpcRosterTest.target(7,otherLane?1:0,10+dx[opposite],10+dy[opposite]);
                var occupied=new java.util.HashSet<>(java.util.Set.of(first.cell(),second.cell()));
                for(int side=0;side<4;side++)if((mask&(1<<side))!=0)
                    occupied.add(ModernNpcRosterTest.cell(10+dx[side*2],10+dy[side*2]));
                var targets=List.of(first,second);
                var expected=targets.stream().filter(t->t.lane()==0)
                        .filter(t->ModernNpcDynamicPursuitTest.legal(map,actor.cell(),t.cell(),occupied,true))
                        .min(java.util.Comparator.comparing(ModernNpcRoster.Target::participant));
                var before=roster.snapshot();
                var actual=roster.pursue(ModernNpcRosterTest.SCOPE,actor.id(),1,targets,occupied,1,()->false);
                assertEquals(before,roster.snapshot());
                assertEquals(expected.isPresent()?ModernNpcRoster.PlanKind.IN_RANGE:ModernNpcRoster.PlanKind.WAIT,actual.kind());
                assertEquals(expected.map(ModernNpcRoster.Target::participant).orElse(null),actual.target());
            }
        }
    }

    @Test void finalCaptureDoesNotAdvanceClockOrPerformExtraRegen() throws Exception {
        try (var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(1000);
            f.engine.stepNpcUnpublished(npc.id(),npc.life(),64,false);
            var atStep=f.engine.npcTickObservation();long draws=f.draws.get();
            f.elapsed(30000);f.epoch.set(31000);
            assertEquals(atStep,f.engine.npcTickObservation());assertEquals(draws,f.draws.get());
            assertNotEquals(atStep.clock(),f.engine.serviceObservation().clock());
        }
    }

    @Test void cancellationDuringNpcRandomStillAbortsWithoutCommittingDamage() throws Exception {
        for (boolean unpublished : List.of(false,true)) try (var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(1000);long hp=f.view().world().vitals().get(0).hp();
            f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            var reason=unpublished?f.engine.stepNpcUnpublished(npc.id(),npc.life(),64,false)
                    :f.engine.stepNpc(npc.id(),npc.life(),64,false).reason();
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,reason);
            var after=f.engine.npcTickObservation();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(hp,after.actors().get(0).vital().values().hp());assertTrue(after.npcs().exchanges().isEmpty());
        }
    }

    @Test void terminalStepSealsSettlementBeforeTheLaterTickCapture() throws Exception {
        try (var f=new ModernTimeAttackEngineTest.Fixture(1,8,1,false)) {
            f.active();f.tower(0,1);var npc=f.living();
            f.elapsed(Math.max(npc.nextMoveMillis(),npc.nextAttackMillis()));f.epoch.set(2000);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(npc.id(),npc.life(),64,false));
            var field=ModernBattleEngine.class.getDeclaredField("timeAttackResult");field.setAccessible(true);
            var sealed=(ModernMatchSettlement.Event)field.get(f.engine);assertNotNull(sealed,"must seal at the terminal step, not the later capture");
            assertEquals(2000,sealed.finishedAtMillis());
            f.epoch.set(99000);f.elapsed(98000);
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.engine.npcTickObservation().phase());
            assertSame(sealed,field.get(f.engine));
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.engine.stepNpcUnpublished(npc.id(),npc.life(),64,false));
            assertSame(sealed,field.get(f.engine));
        }
    }
}
