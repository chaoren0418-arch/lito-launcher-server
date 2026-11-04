package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BattlePanicActorLifecycleTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void companionLeaseIsNonRefreshingAndCancellationReportsIt() {
        BattleCompanion companion = new BattleCompanion(0x6000_0217L);
        companion.resetForBattle(100, 100);
        long now = System.nanoTime();
        assertTrue(companion.activatePanic(now, 64, -1));
        assertFalse(companion.activatePanic(now + 1, 64, -1));
        assertEquals(-1, companion.panicAbilityScoreModifier(now + 2));
        assertEquals(14, companion.snapshot().strength());
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertNotNull(cancellation);
        assertTrue(cancellation.panic());
        assertEquals(0, companion.panicAbilityScoreModifier(now + 3));
    }

    @Test void questAndTimeActorsOwnCancelableExpiringLeases() {
        long now = TimeUnit.SECONDS.toNanos(2);
        BattleQuestEncounter quest = new BattleQuestEncounter();
        quest.reset(BattleRules.decode(new GameHub.RoomSettings(
                "quest", "", 8, BattleRules.Mode.QUEST_BATTLE.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(573));
        BattleQuestEncounter.Snapshot questMonster = quest.snapshots(0).get(0);
        assertTrue(quest.panic(questMonster.objectId(), 0, now, 64, -1));
        assertFalse(quest.panic(questMonster.objectId(), 0, now + 1, 64, -1));
        assertEquals(-1, quest.panicAbilityScoreModifier(
                questMonster.objectId(), now + 2));
        BattleQuestEncounter.MagicCancellation questCancellation =
                quest.cancelMagicEffects(questMonster.objectId(), 0);
        assertNotNull(questCancellation);
        assertTrue(questCancellation.panic());

        BattleTimeAttackEncounter time = new BattleTimeAttackEncounter();
        time.reset(BattleRules.decode(new GameHub.RoomSettings(
                "time", "", 60, BattleRules.Mode.TIME_ATTACK.wireId(),
                0, 0, 0, 0, List.of())), ArenaMapCatalog.forMap(540));
        BattleTimeAttackEncounter.Snapshot actor =
                time.startAfterTowerDestroyed().get(0);
        assertFalse(actor.finalBoss());
        assertTrue(time.panic(actor.objectId(), now, 64, -1));
        assertEquals(-1, time.panicAbilityScoreModifier(actor.objectId(), now + 1));
        assertEquals(1, time.expirePanics(
                now + TimeUnit.SECONDS.toNanos(64)).size());
        assertEquals(0, time.panicAbilityScoreModifier(
                actor.objectId(), now + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void classicWorldMonsterLeaseExpiresAndCancellationClearsIt()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClassicWorldMonsterPopulation.Snapshot monster =
                    hub.classicWorldMonstersInScreen(4, -1, 33313, 32339)
                    .stream().filter(candidate -> candidate.present()
                            && candidate.alive() && !candidate.template().boss())
                    .findFirst().orElseThrow();
            long now = TimeUnit.SECONDS.toNanos(3);
            assertTrue(hub.panicClassicWorldMonster(
                    4, -1, monster.objectId(), now, 64, -1));
            assertEquals(-1, hub.classicWorldMonsterPanicAbilityScoreModifier(
                    4, -1, monster.objectId(), now + 1));
            assertTrue(hub.cancelClassicWorldMonsterPanic(
                    4, -1, monster.objectId()));
            assertEquals(0, hub.classicWorldMonsterPanicAbilityScoreModifier(
                    4, -1, monster.objectId(), now + 2));
            assertTrue(hub.panicClassicWorldMonster(
                    4, -1, monster.objectId(), now + 3, 64, -1));
            assertEquals(1, hub.expireClassicWorldMonsterPanics(
                    4, -1, now + 3 + TimeUnit.SECONDS.toNanos(64)).size());
        }
    }
}
