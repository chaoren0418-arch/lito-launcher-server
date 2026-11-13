package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * #138 主控裁定 3：monster tick 持有 population 鎖時呼叫
 * {@link NativeStaticNpcMotion#occupies(int, int, int, ClassicWorldMonsterPopulation)}，
 * 不得再取 hub 鎖（0.6.71 的 client 執行緒是 hub → population）。
 */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class NativeStaticNpcMotionLockOrderTest {
    private static final int MAP = 410;

    @Test
    void monsterTickOccupancyNeverWaitsForHubHeldByClientThread() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation(7);
            NativeStaticNpcMotion.bind(population, hub, -1);
            ClassicWorldNpcCatalog.StaticNpc npc = entrance();
            synchronized (hub) {
                // 讓 occupies 走 scene actor 的查詢分支。
                NativeStaticNpcScene.state(hub, true);
            }

            CountDownLatch clientHasHub = new CountDownLatch(1);
            CountDownLatch tickHasPopulation = new CountDownLatch(1);
            CountDownLatch tickDone = new CountDownLatch(1);
            AtomicReference<Boolean> occupied = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();

            // 正式 0.6.71 既有路徑：client 執行緒 hub → population。
            Thread client = new Thread(() -> {
                try {
                    synchronized (hub) {
                        clientHasHub.countDown();
                        tickHasPopulation.await();
                        tickDone.await();
                        synchronized (population) {
                            assertTrue(Thread.holdsLock(hub));
                        }
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }, "lock-order-client");
            // 候選新增路徑：battle clock monster tick 持有 population 時查詢靜態 NPC 佔位。
            Thread tick = new Thread(() -> {
                try {
                    synchronized (population) {
                        tickHasPopulation.countDown();
                        clientHasHub.await();
                        occupied.set(NativeStaticNpcMotion.occupies(
                                MAP, npc.x(), npc.y(), population));
                        tickDone.countDown();
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }, "lock-order-monster-tick");
            client.setDaemon(true);
            tick.setDaemon(true);
            client.start();
            tick.start();

            // 確定性判斷：兩邊各自拿到第一把鎖之後，tick 要嘛完成查詢，
            // 要嘛被 client 持有的 hub 監視器擋住（BLOCKED 且 lock owner 是 client）。
            // 後者就是取鎖順序相反，直接判定失敗，不靠逾時。
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            while (tickDone.getCount() > 0) {
                if (failure.get() != null) {
                    break;
                }
                if (tick.getState() == Thread.State.BLOCKED) {
                    ThreadInfo info = threads.getThreadInfo(tick.getId());
                    if (info != null && info.getLockOwnerId() == client.getId()) {
                        client.interrupt();
                        fail("monster tick waits for the hub monitor while holding population: "
                                + info.getLockName());
                    }
                }
                Thread.onSpinWait();
            }
            client.join();
            tick.join();
            assertNull(failure.get());
            assertEquals(Boolean.TRUE, occupied.get(),
                    "static L1Quest NPC still occupies its tile");
        }
    }

    @Test
    void lockFreeOccupancyMatchesHubLockedViewIncludingRetiredActors() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation(7);
            NativeStaticNpcMotion.bind(population, hub, -1);
            ClassicWorldNpcCatalog.StaticNpc npc = entrance();

            // 沒有 scene：與 hub 鎖內的查詢一致。
            assertTrue(NativeStaticNpcMotion.occupies(MAP, npc.x(), npc.y(), population));
            assertEquals(NativeStaticNpcMotion.occupies(hub, -1, MAP, npc.x(), npc.y(), 0),
                    NativeStaticNpcMotion.occupies(MAP, npc.x(), npc.y(), population));

            // 被 Cancellation 取代（retired）的靜態 NPC 不再佔位，兩種查詢一致。
            synchronized (hub) {
                CombatAccess.set(NativeStaticNpcScene.actor(hub, npc, -1, true),
                        NativeStaticCancellationReplacement.FIELD, true);
            }
            assertFalse(NativeStaticNpcMotion.occupies(MAP, npc.x(), npc.y(), population)
                    && !otherNpcAt(npc), "retired static NPC must not occupy");
            assertEquals(NativeStaticNpcMotion.occupies(hub, -1, MAP, npc.x(), npc.y(), 0),
                    NativeStaticNpcMotion.occupies(MAP, npc.x(), npc.y(), population));
        }
    }

    private static boolean otherNpcAt(ClassicWorldNpcCatalog.StaticNpc npc) {
        return ClassicWorldNpcCatalog.all(MAP).stream().anyMatch(other ->
                other.objectId() != npc.objectId()
                        && !ClassicWorldGuardianCatalog.isGuardian(other)
                        && !ClassicWorldGuardCatalog.isGuard(other)
                        && other.x() == npc.x() && other.y() == npc.y());
    }

    private static ClassicWorldNpcCatalog.StaticNpc entrance() {
        return ClassicWorldNpcCatalog.all(MAP).stream()
                .filter(npc -> npc.npcId() == 80133)
                .findFirst().orElseThrow();
    }
}
