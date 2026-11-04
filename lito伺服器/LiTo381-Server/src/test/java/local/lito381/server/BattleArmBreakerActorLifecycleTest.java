package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattleArmBreakerActorLifecycleTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void companionOwnsNonRefreshingCancelableMeleePenalty() {
        BattleCompanion companion = new BattleCompanion(0x6000_0213L);
        companion.resetForBattle(100, 100);
        long now = TimeUnit.SECONDS.toNanos(1);
        BattleArmBreakerStatus.Activation first =
                companion.activateArmBreaker(now, 12);
        BattleArmBreakerStatus.Activation recast =
                companion.activateArmBreaker(now + 1, 12);
        assertTrue(first.accepted());
        assertTrue(recast.alreadyActive());
        assertEquals(first.expiresAtNanos(), recast.expiresAtNanos());
        assertEquals(-2, companion.armBreakerMeleeDamageModifier(now + 2));
        assertNotNull(companion.cancelMagicEffects());
        assertEquals(0, companion.armBreakerMeleeDamageModifier(now + 3));
    }

    @Test void questAndTimeActorsOwnTheSameStatus1026Lifecycle() {
        long now = TimeUnit.SECONDS.toNanos(2);
        BattleQuestEncounter quest = new BattleQuestEncounter();
        quest.reset(BattleRules.decode(new GameHub.RoomSettings(
                "quest", "", 8, BattleRules.Mode.QUEST_BATTLE.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(573));
        BattleQuestEncounter.Snapshot questMonster = quest.snapshots(0).get(0);
        BattleArmBreakerStatus.Activation questFirst = quest.armBreaker(
                questMonster.objectId(), 0, now, 12);
        BattleArmBreakerStatus.Activation questRecast = quest.armBreaker(
                questMonster.objectId(), 0, now + 1, 12);
        assertTrue(questFirst.accepted());
        assertTrue(questRecast.alreadyActive());
        assertEquals(questFirst.expiresAtNanos(), questRecast.expiresAtNanos());
        assertEquals(-2, quest.armBreakerMeleeDamageModifier(
                questMonster.objectId(), now + 2));
        assertNotNull(quest.cancelMagicEffects(questMonster.objectId(), 0));
        assertEquals(0, quest.armBreakerMeleeDamageModifier(
                questMonster.objectId(), now + 3));

        BattleTimeAttackEncounter time = new BattleTimeAttackEncounter();
        time.reset(BattleRules.decode(new GameHub.RoomSettings(
                "time", "", 60, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                time.startAfterTowerDestroyed().get(0);
        assertTrue(time.armBreaker(actor.objectId(), now, 12).accepted());
        assertEquals(-2, time.armBreakerMeleeDamageModifier(
                actor.objectId(), now + 1));
        assertNotNull(time.cancelMagicEffects(actor.objectId()));
        assertEquals(0, time.armBreakerMeleeDamageModifier(
                actor.objectId(), now + 2));
    }

    @Test void classicWorldMonsterLeaseExpiresAndCancellationClearsIt()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation.Snapshot monster =
                    hub.classicWorldMonstersInScreen(4, -1, 33313, 32339)
                    .stream().filter(candidate -> candidate.present()
                            && candidate.alive()).findFirst().orElseThrow();
            long now = TimeUnit.SECONDS.toNanos(3);
            assertTrue(hub.armBreakerClassicWorldMonster(
                    4, -1, monster.objectId(), now, 12).accepted());
            assertEquals(-2,
                    hub.classicWorldMonsterArmBreakerMeleeDamageModifier(
                            4, -1, monster.objectId(), now + 1));
            assertEquals(0,
                    hub.classicWorldMonsterArmBreakerMeleeDamageModifier(
                            4, -1, monster.objectId(),
                            now + TimeUnit.SECONDS.toNanos(12)));
            assertTrue(hub.armBreakerClassicWorldMonster(
                    4, -1, monster.objectId(), now + 1, 12).accepted());
            assertTrue(hub.cancelClassicWorldMonsterArmBreaker(
                    4, -1, monster.objectId()));
            assertEquals(0,
                    hub.classicWorldMonsterArmBreakerMeleeDamageModifier(
                            4, -1, monster.objectId(), now + 2));
        }
    }
}
