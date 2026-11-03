package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BattlePhantasmActorLifecycleTest {
    @Test void companionSleepLocksAiWakesOnDamageAndCancellationReportsIt() {
        BattleCompanion companion = new BattleCompanion(0x6000_0212L);
        companion.resetForBattle(100, 100);
        companion.selectMasterTarget(0x3810_0212L);
        long now = 1_000_000L;

        assertTrue(companion.activateSleep(now, 5));
        assertTrue(companion.sleepActive(now));
        assertFalse(companion.ready(now));
        assertTrue(companion.applyDamage(1).appliedDamage() > 0);
        assertFalse(companion.sleepActive(now));

        assertTrue(companion.activateSleep(now + 1, 5));
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertNotNull(cancellation);
        assertTrue(cancellation.sleep());
        assertFalse(companion.sleepActive(now + 1));
    }

    @Test void classicWorldMonsterSleepLocksAiAndAcceptedDamageWakesIt()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation.Snapshot monster =
                    hub.classicWorldMonstersInScreen(4, -1, 33313, 32339)
                    .stream().filter(candidate -> candidate.present()
                            && candidate.alive()).findFirst().orElseThrow();
            long now = System.nanoTime();
            assertTrue(hub.sleepClassicWorldMonster(
                    4, -1, monster.objectId(), now, 5));
            assertTrue(hub.classicWorldMonsterSleepActive(
                    4, -1, monster.objectId(), now));
            assertNotNull(hub.damageClassicWorldMonster(
                    4, -1, monster.objectId(), 1));
            assertFalse(hub.classicWorldMonsterSleepActive(
                    4, -1, monster.objectId(), now));
        }
    }
}
