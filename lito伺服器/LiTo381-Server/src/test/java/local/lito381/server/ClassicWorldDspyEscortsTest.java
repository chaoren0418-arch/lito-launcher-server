package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldDspyEscortsTest {
    @Test
    void selectedNpcItemAndStoryRowsArePinned() {
        assertEquals(80012, ClassicWorldDspyCatalog.DSPY_NPC_ID);
        assertEquals(2779, ClassicWorldDspyCatalog.DSPY_GFX_ID);
        assertEquals(2500, ClassicWorldDspyCatalog.DSPY_MAX_HP);
        assertEquals(-15, ClassicWorldDspyCatalog.DSPY_ARMOR_CLASS);
        assertEquals(960, ClassicWorldDspyCatalog.DSPY_PASSISPEED_MILLIS);
        assertEquals(80011, ClassicWorldDspyCatalog.ASSASSIN.npcId());
        assertEquals(3989, ClassicWorldDspyCatalog.ASSASSIN.gfxId());
        assertEquals(53, ClassicWorldDspyCatalog.ASSASSIN.level());
        assertEquals(1800, ClassicWorldDspyCatalog.ASSASSIN.hp());
        assertEquals(14, ClassicWorldDspyCatalog.ASSASSIN.lightSize());
        assertTrue(ClassicWorldDspyCatalog.ASSASSIN.aggressive());

        InventoryCatalog.Definition reward = InventoryCatalog.require(49163);
        assertEquals(0xFF, reward.useType());
        assertEquals(912, reward.icon());
        assertEquals(22, reward.groundGfx());
        assertEquals("$5583", reward.name());
        assertEquals(127, reward.itemDescriptionId());
        assertEquals(0, reward.unitWeight());
        assertFalse(reward.tradable());

        ClassicWorldStoryQuestCatalog.Event event =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(candidate -> candidate.id() == 40000261)
                        .findFirst().orElseThrow();
        assertEquals(11603, event.questId());
        assertEquals(80012, event.npcId());
        assertEquals("start", event.action());
        assertEquals(49163, event.givenItems().get(0).itemId());
        assertEquals(11604, event.chainedQuestId());

        ClassicWorldNpcCatalog.StaticNpc source =
                ClassicWorldNpcCatalog.all(400).stream()
                        .filter(npc -> npc.spawnId() == 1182)
                        .findFirst().orElseThrow();
        assertEquals(80012, source.npcId());
        assertEquals(32864, source.x());
        assertEquals(32818, source.y());
        assertEquals(5, source.heading());
    }

    @Test
    void ownerRectangleCompletesBeforeDistanceLossAndSuppressesAssassin() {
        ClassicWorldDspyEscorts escorts = new ClassicWorldDspyEscorts(18);
        ClassicWorldDspyEscorts.Snapshot follower = escorts.claimStatic(
                "Server\0Alice", 32864, 32818, 5, 0);

        ClassicWorldDspyEscorts.Cycle cycle = escorts.cycle(
                "server\0alice", 400, 32557, 32656, true, 0,
                (a, b, c, d, id) -> true, (x, y) -> true);

        assertEquals(ClassicWorldDspyEscorts.TransitionKind.ARRIVED,
                cycle.transition().kind());
        assertEquals(follower.objectId(), cycle.transition().before().objectId());
        assertNull(cycle.assassin());
        assertNull(escorts.owned("Server\0Alice"));
    }

    @Test
    void nativeThreePercentRollRunsBeforeOwnerLossAndFreeReplacement() {
        ClassicWorldDspyEscorts escorts = new ClassicWorldDspyEscorts(18);
        ClassicWorldDspyEscorts.Snapshot follower = escorts.claimStatic(
                "Alice", 32864, 32818, 5, 0);

        ClassicWorldDspyEscorts.Cycle cycle = escorts.cycle(
                "Alice", 400, 32900, 32818, true, 0,
                (a, b, c, d, id) -> true, (x, y) -> true);

        assertNotNull(cycle.assassin());
        assertEquals(follower.objectId(), cycle.assassin().linkedObjectId());
        assertTrue(Math.abs(cycle.assassin().x() - follower.x()) <= 4);
        assertTrue(Math.abs(cycle.assassin().y() - follower.y()) <= 4);
        assertEquals(ClassicWorldDspyEscorts.TransitionKind.ABANDONED,
                cycle.transition().kind());
        assertFalse(cycle.transition().after().owned());
        assertNotNull(escorts.claimFree(
                "Bob", cycle.transition().after().objectId(), 1));
    }

    @Test
    void movementUsesRetainedPassispeedAndIdleUsesNpcAiThreeHundredMs() {
        ClassicWorldDspyEscorts moving = new ClassicWorldDspyEscorts(0);
        moving.claimStatic("Alice", 100, 100, 4, 0);
        ClassicWorldDspyEscorts.Cycle moved = moving.cycle(
                "Alice", 400, 105, 100, true, 0,
                (a, b, c, d, id) -> true, (x, y) -> true);
        assertEquals(ClassicWorldDspyEscorts.TransitionKind.MOVED,
                moved.transition().kind());
        assertEquals(101, moved.transition().after().x());
        assertEquals(2, moved.transition().after().heading());
        assertEquals(ClassicWorldDspyEscorts.MOVE_INTERVAL_NANOS,
                moved.transition().after().nextAiNanos());
        assertEquals(ClassicWorldDspyEscorts.TransitionKind.NONE,
                moving.cycle("Alice", 400, 105, 100, true,
                        ClassicWorldDspyEscorts.MOVE_INTERVAL_NANOS - 1,
                        (a, b, c, d, id) -> true,
                        (x, y) -> true).transition().kind());

        ClassicWorldDspyEscorts idle = new ClassicWorldDspyEscorts(0);
        idle.claimStatic("Alice", 100, 100, 4, 0);
        idle.cycle("Alice", 400, 101, 100, true, 0,
                (a, b, c, d, id) -> true, (x, y) -> true);
        assertEquals(ClassicWorldDspyEscorts.IDLE_INTERVAL_NANOS,
                idle.owned("Alice").nextAiNanos());
    }

    @Test
    void lethalDamageKeepsTenSecondCorpseThenDeletesWithoutFreeNpc() {
        ClassicWorldDspyEscorts escorts = new ClassicWorldDspyEscorts(0);
        ClassicWorldDspyEscorts.Snapshot follower = escorts.claimStatic(
                "Alice", 100, 100, 4, 0);

        ClassicWorldDspyEscorts.Damage damage = escorts.damage(
                follower.objectId(), 3000, 5);
        assertTrue(damage.diedNow());
        assertEquals(0, damage.after().currentHp());
        assertFalse(damage.after().alive());
        assertEquals(5 + ClassicWorldDspyEscorts.CORPSE_NANOS,
                damage.after().removeAtNanos());
        assertTrue(escorts.expireCorpses(
                damage.after().removeAtNanos() - 1).isEmpty());
        assertEquals(1, escorts.expireCorpses(
                damage.after().removeAtNanos()).size());
        assertEquals(0, escorts.size());
    }

    @Test
    void followerPacketMatchesRetainedFollowerPackTail() {
        ClassicWorldDspyEscorts escorts = new ClassicWorldDspyEscorts(0);
        ClassicWorldDspyEscorts.Snapshot follower = escorts.claimStatic(
                "Alice", 32864, 32818, 5, 0);
        Protocol.Reader in = new Protocol.Reader(
                Protocol.classicWorldDspyFollower(follower));

        assertEquals(Protocol.S_PUT_OBJECT, in.c());
        assertEquals(32864, in.h());
        assertEquals(32818, in.h());
        assertEquals(follower.objectId(), in.d());
        assertEquals(2779, in.h());
        assertEquals(0, in.c());
        assertEquals(5, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals(0, in.h());
        assertEquals("$5800", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0, in.d());
        assertEquals("", in.s());
        assertEquals("", in.s());
        assertEquals(0, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0xFF, in.c());
        assertEquals(0, in.remaining());
    }

    @Test
    void globalPopulationOwnsDedicatedOneShotRangeAndLinkedTarget() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21);
        ClassicWorldMonsterPopulation.Snapshot assassin =
                population.spawnQuestMonsterAt(
                        400, 80011, 32860, 32820, 0,
                        ClassicWorldDspyCatalog.ASSASSIN);
        assertNotNull(assassin);
        assertTrue(assassin.objectId()
                >= ClassicWorldMonsterPopulation.QUEST_DYNAMIC_OBJECT_BASE);
        assertTrue(assassin.objectId()
                <= ClassicWorldMonsterPopulation.QUEST_DYNAMIC_OBJECT_END);
        assertTrue(population.provoke(400, assassin.objectId(), 0x6D00_0000L));
        assertEquals(0x6D00_0000L,
                population.targetObjectId(400, assassin.objectId()));

        ClassicWorldMonsterPopulation.Hit killed = population.damage(
                400, assassin.objectId(), 0x6000_0001L, 5000);
        assertTrue(killed.diedNow());
        assertNotNull(population.removeCorpse(400, assassin.objectId()));
        assertNull(population.actor(400, assassin.objectId()));
    }
}
