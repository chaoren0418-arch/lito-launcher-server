package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * T131 (Issue #382): lock order between the {@link GameHub} monitor and the static
 * {@code DurableBattleSettlement.CONTEXTS} map.
 *
 * <p>CI run 43264734337 deadlocked the whole JVM: one thread ran {@code GameHub.disconnect ->
 * resetOrRemove -> cleanupAllowed} (hub monitor held, waiting for CONTEXTS) while another ran
 * {@code DurableBattleSettlement.begin -> context -> new Context -> NativeRoomRules.testRoom}
 * (CONTEXTS held, waiting for the hub monitor). The rule is now "GameHub before CONTEXTS, and
 * nothing is acquired while CONTEXTS is held" (docs/design/gameplay.md §4.2).
 *
 * <p>No sleeps and no reliance on scheduling luck: the per-hub room map stored inside CONTEXTS is
 * replaced by a {@link HashMap} whose {@code get} parks the calling thread exactly at the point
 * where {@code context()} holds CONTEXTS. Thread states are then read through
 * {@link ThreadMXBean}; every wait is on a latch or on such a state.
 */
@Timeout(value = 90, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
final class DurableBattleSettlementLockOrderTest {
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final long WAIT_NANOS = TimeUnit.SECONDS.toNanos(30);
    /** Room ids that never exist: the settlement worker of the MySQL job then never resets them. */
    private static final long PROBE_ROOM = 0x7131_0001L;
    private static final long RACE_ROOM = 0x7131_0002L;

    /** Per-hub room map whose lookup runs a hook while the caller holds CONTEXTS. */
    private static final class HookedRooms extends HashMap<Long, DurableBattleSettlement.Context> {
        private static final long serialVersionUID = 1L;
        private transient volatile Thread target;
        private final transient AtomicReference<Runnable> hook = new AtomicReference<>();

        /** Only {@code thread}'s next lookup runs the hook; other callers (settlement worker) pass. */
        void arm(Thread thread, Runnable action) {
            target = thread;
            hook.set(action);
        }

        @Override
        public DurableBattleSettlement.Context get(Object key) {
            if (Thread.currentThread() == target) {
                Runnable armed = hook.getAndSet(null);
                if (armed != null) armed.run();
            }
            return super.get(key);
        }
    }

    @Test
    void contextNeverEntersTheHubMonitorWhileHoldingContexts() throws Exception {
        Object contexts = contexts();
        GameHub hub = new GameHub(config());
        HookedRooms rooms = new HookedRooms();
        try {
            synchronized (contexts) {
                @SuppressWarnings("unchecked")
                var map = (Map<GameHub, Map<Long, DurableBattleSettlement.Context>>) contexts;
                map.put(hub, rooms);
            }
            probe(contexts, hub, rooms);
            synchronized (contexts) {
                rooms.clear();
            }
            battleStartRacesDisconnectCleanup(contexts, hub, rooms);
        } finally {
            synchronized (contexts) {
                ((Map<?, ?>) contexts).remove(hub);
            }
            hub.close();
        }
    }

    /**
     * Step 1, safe on broken code: thread A only holds the hub monitor (it does not ask for
     * CONTEXTS), so a violation shows up as "B is blocked on a monitor owned by A while holding
     * CONTEXTS" and can still be released. Asking for CONTEXTS here as well would be the real,
     * unrecoverable deadlock and would hang every later test of the same JVM.
     */
    private static void probe(Object contexts, GameHub hub, HookedRooms rooms) throws Exception {
        CountDownLatch insideContexts = new CountDownLatch(1);
        CountDownLatch hubHeld = new CountDownLatch(1);
        CountDownLatch releaseHub = new CountDownLatch(1);
        AtomicBoolean hookHeldContexts = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<DurableBattleSettlement.Context> created = new AtomicReference<>();

        Thread starter = daemon("t131-probe-battle-start", failure,
                () -> created.set(DurableBattleSettlement.context(hub, PROBE_ROOM)));
        rooms.arm(starter, () -> {
            hookHeldContexts.set(Thread.holdsLock(contexts));
            insideContexts.countDown();
            await(hubHeld, failure);
        });
        Thread holder = daemon("t131-probe-hub-holder", failure, () -> {
            await(insideContexts, failure);
            synchronized (hub) {
                hubHeld.countDown();
                await(releaseHub, failure);
            }
        });
        starter.start();
        holder.start();

        boolean blockedOnHub = false;
        long deadline = System.nanoTime() + WAIT_NANOS;
        try {
            while (starter.isAlive() && !blockedOnHub) {
                assertTrue(System.nanoTime() < deadline,
                        () -> "context() neither returned nor blocked: " + describe(starter, holder));
                blockedOnHub = blockedBy(starter, holder);
                if (!blockedOnHub) starter.join(2);
            }
        } finally {
            releaseHub.countDown();
        }
        join(starter, holder);
        assertNoFailure(failure);
        assertTrue(hookHeldContexts.get(), "the hook must run inside synchronized(CONTEXTS)");
        assertFalse(blockedOnHub,
                "DurableBattleSettlement.context() waited for the GameHub monitor while holding "
                        + "CONTEXTS; GameHub.disconnect/leaveRoom/endBattle call cleanupAllowed "
                        + "(CONTEXTS) with the hub monitor held, so this order deadlocks the JVM");
        assertSame(created.get(), DurableBattleSettlement.context(hub, PROBE_ROOM),
                "same room and generation keeps its context");
    }

    /**
     * Step 2, the interleaving of the CI thread dump with the production call on both sides:
     * B is inside {@code context()} holding CONTEXTS, A is inside the synchronized
     * {@code GameHub.endBattle -> requireDurable -> cleanupAllowed} blocked on CONTEXTS with the
     * hub monitor held. Only then is B allowed to continue. Reached only when step 1 passed.
     */
    private static void battleStartRacesDisconnectCleanup(Object contexts, GameHub hub,
            HookedRooms rooms) throws Exception {
        CountDownLatch insideContexts = new CountDownLatch(1);
        AtomicBoolean cleanupWaitedForContexts = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<DurableBattleSettlement.Context> created = new AtomicReference<>();

        Thread cleanup = daemon("t131-race-hub-cleanup", failure, () -> {
            await(insideContexts, failure);
            hub.endBattle(RACE_ROOM);
        });
        Thread starter = daemon("t131-race-battle-start", failure,
                () -> created.set(DurableBattleSettlement.context(hub, RACE_ROOM)));
        rooms.arm(starter, () -> {
            Thread self = Thread.currentThread();
            insideContexts.countDown();
            long deadline = System.nanoTime() + WAIT_NANOS;
            while (System.nanoTime() < deadline) {
                if (blockedBy(cleanup, self)) {
                    cleanupWaitedForContexts.set(true);
                    return;
                }
                Thread.onSpinWait();
            }
        });
        starter.start();
        cleanup.start();
        join(starter, cleanup);
        assertNoFailure(failure);
        assertTrue(cleanupWaitedForContexts.get(),
                "hub cleanup must have been blocked on CONTEXTS (hub monitor held) while "
                        + "context() was inside CONTEXTS");
        assertEquals(RACE_ROOM, created.get().room);
        assertTrue(DurableBattleSettlement.cleanupAllowed(hub, RACE_ROOM));
    }

    /** The hub values moved out of the CONTEXTS block must still be the ones the context stores. */
    @Test
    void contextStillRecordsTheTestRoomFlagAndBattleGeneration() throws Exception {
        try (GameHub hub = new GameHub(config())) {
            List<Long> defaults = hub.roomSnapshots(0).stream().map(GameHub.RoomSnapshot::id)
                    .filter(id -> NativeRoomRules.testRoom(hub, id)).toList();
            assertEquals(1, defaults.size(), "channel 0 owns exactly one default (test) room");
            long custom = hub.createRoom(0, new GameHub.RoomSettings(
                    "T131 ranked", "", 2, GameHub.RoomSettings.ALLOW_OBSERVERS,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));

            DurableBattleSettlement.Context test =
                    DurableBattleSettlement.context(hub, defaults.get(0));
            DurableBattleSettlement.Context ranked = DurableBattleSettlement.context(hub, custom);
            DurableBattleSettlement.Context lobby = DurableBattleSettlement.context(hub, 0);
            assertTrue(test.testRoom, "default room is the unranked test room");
            assertFalse(ranked.testRoom, "a created room is ranked");
            assertFalse(lobby.testRoom, "room id 0 is never a test room");
            assertSame(hub, ranked.hub.get());
            assertEquals(custom, ranked.room);
            assertEquals(roomGeneration(hub, custom), ranked.generation);
            assertSame(ranked, DurableBattleSettlement.context(hub, custom));

            // A new battle generation of the same room gets a new context (and a new battle id).
            Object room = ((Map<?, ?>) field(hub, "rooms")).get(custom);
            Field generation = room.getClass().getDeclaredField("battleGeneration");
            generation.setAccessible(true);
            synchronized (hub) {
                generation.setLong(room, ranked.generation + 1);
            }
            DurableBattleSettlement.Context next = DurableBattleSettlement.context(hub, custom);
            assertNotSame(ranked, next);
            assertEquals(ranked.generation + 1, next.generation);
            assertFalse(next.testRoom);
            assertFalse(ranked.battle.equals(next.battle));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static ServerConfig config() throws Exception {
        return ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
    }

    private static Object contexts() throws Exception {
        Field field = DurableBattleSettlement.class.getDeclaredField("CONTEXTS");
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static long roomGeneration(GameHub hub, long roomId) throws Exception {
        synchronized (hub) {
            Object room = ((Map<?, ?>) field(hub, "rooms")).get(roomId);
            return (long) field(room, "battleGeneration");
        }
    }

    /** True when {@code waiter} is blocked entering a monitor currently owned by {@code owner}. */
    private static boolean blockedBy(Thread waiter, Thread owner) {
        ThreadInfo info = THREADS.getThreadInfo(waiter.getId());
        return info != null && info.getThreadState() == Thread.State.BLOCKED
                && info.getLockOwnerId() == owner.getId();
    }

    private static Thread daemon(String name, AtomicReference<Throwable> failure, Runnable body) {
        Thread thread = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
            }
        }, name);
        thread.setDaemon(true);
        return thread;
    }

    private static void await(CountDownLatch latch, AtomicReference<Throwable> failure) {
        try {
            if (!latch.await(WAIT_NANOS, TimeUnit.NANOSECONDS)) {
                failure.compareAndSet(null, new AssertionError("latch not released in time"));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failure.compareAndSet(null, interrupted);
        }
    }

    private static void join(Thread... threads) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_NANOS;
        for (Thread thread : threads) {
            long remaining = Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
            thread.join(remaining);
        }
        for (Thread thread : threads) {
            assertFalse(thread.isAlive(), () -> "thread did not finish: " + describe(threads));
        }
    }

    private static void assertNoFailure(AtomicReference<Throwable> failure) {
        Throwable error = failure.get();
        if (error != null) throw new AssertionError("worker thread failed", error);
    }

    private static String describe(Thread... threads) {
        StringBuilder text = new StringBuilder();
        long[] deadlocked = THREADS.findDeadlockedThreads();
        text.append("deadlocked=").append(deadlocked == null ? "none" : Arrays.toString(deadlocked));
        for (Thread thread : threads) {
            ThreadInfo info = THREADS.getThreadInfo(thread.getId(), 12);
            text.append(System.lineSeparator()).append(thread.getName()).append(" id=")
                    .append(thread.getId());
            if (info == null) {
                text.append(" terminated");
                continue;
            }
            text.append(' ').append(info.getThreadState()).append(" on ")
                    .append(info.getLockName()).append(" owned by ").append(info.getLockOwnerName());
            for (StackTraceElement frame : info.getStackTrace()) {
                text.append(System.lineSeparator()).append("    at ").append(frame);
            }
        }
        return text.toString();
    }
}
