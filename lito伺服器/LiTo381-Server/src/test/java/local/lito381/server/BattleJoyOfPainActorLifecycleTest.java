package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class BattleJoyOfPainActorLifecycleTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void companionRefreshesAndCancellationClearsCurrentGeneration() {
        BattleCompanion companion = new BattleCompanion(0x6000_0218L);
        companion.resetForBattle(100, 100);
        long now = TimeUnit.SECONDS.toNanos(1);
        BattleJoyOfPainStatus.Activation first =
                companion.activateJoyOfPain(now, 2);
        BattleJoyOfPainStatus.Lease stale =
                companion.captureJoyOfPain(now + 1);
        BattleJoyOfPainStatus.Activation refresh =
                companion.activateJoyOfPain(now + 2, 2);
        assertNotNull(first);
        assertTrue(first.firstApplication());
        assertTrue(refresh.refreshed());
        assertFalse(companion.consumeJoyOfPain(stale));
        assertNotNull(companion.captureJoyOfPain(now + 3));
        assertTrue(companion.cancelJoyOfPain());
        assertNull(companion.captureJoyOfPain(now + 4));
    }

    @Test void questAndTimeActorsCaptureConsumeAndExpireExactLease() {
        long now = TimeUnit.SECONDS.toNanos(2);
        BattleQuestEncounter quest = new BattleQuestEncounter();
        quest.reset(BattleRules.decode(new GameHub.RoomSettings(
                "quest", "", 8, BattleRules.Mode.QUEST_BATTLE.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(573));
        BattleQuestEncounter.Snapshot questMonster = quest.snapshots(0).get(0);
        assertNotNull(quest.joyOfPain(
                questMonster.objectId(), 0, now, 2));
        BattleJoyOfPainStatus.Lease questLease = quest.captureJoyOfPain(
                questMonster.objectId(), now + 1);
        assertTrue(quest.consumeJoyOfPain(
                questMonster.objectId(), questLease));

        BattleTimeAttackEncounter time = new BattleTimeAttackEncounter();
        time.reset(BattleRules.decode(new GameHub.RoomSettings(
                "time", "", 60, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                time.startAfterTowerDestroyed().get(0);
        assertNotNull(time.joyOfPain(actor.objectId(), now, 2));
        assertTrue(time.expireJoyOfPain(
                now + TimeUnit.SECONDS.toNanos(2)).stream()
                .anyMatch(expired -> expired.objectId() == actor.objectId()));
        assertNull(time.captureJoyOfPain(
                actor.objectId(), now + TimeUnit.SECONDS.toNanos(2)));
    }

    @Test void classicWorldMonsterAndGuardianTowerOwnTransientLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation.Snapshot monster =
                    hub.classicWorldMonstersInScreen(4, -1, 33313, 32339)
                    .stream().filter(candidate -> candidate.present()
                            && candidate.alive()).findFirst().orElseThrow();
            long now = TimeUnit.SECONDS.toNanos(3);
            assertNotNull(hub.joyOfPainClassicWorldMonster(
                    4, -1, monster.objectId(), now, 2));
            BattleJoyOfPainStatus.Lease lease =
                    hub.captureClassicWorldMonsterJoyOfPain(
                            4, -1, monster.objectId(), now + 1);
            assertTrue(hub.consumeClassicWorldMonsterJoyOfPain(
                    4, -1, monster.objectId(), lease));
        }

        BattleGuardianTower tower = new BattleGuardianTower();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        tower.reset(0, arena, arena.spawn(4),
                BattleGuardianTower.GFX_FIRE,
                BattleGuardianTower.NAME_FIRE, "test");
        assertNotNull(tower.activateJoyOfPain(0, 2));
        tower.deactivate();
        assertNull(tower.activateJoyOfPain(1, 2));
    }
}
