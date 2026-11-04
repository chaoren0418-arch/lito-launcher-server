package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the retained HIDDEN_STATUS_FLY visibility and landing contract. */
final class NativeClassicWorldFlyingMonsterSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void retainedFlyingMonsterLifecycleAndTargetRejectionStayExact()
            throws Exception {
        String monster = Files.readString(RETAINED.resolve(
                "model/Instance/L1MonsterInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(monster.contains(
                "getHiddenStatus() == HIDDEN_STATUS_FLY"));
        assertTrue(monster.contains("ActionCodes.ACTION_Moveup"));
        assertTrue(monster.contains("damage <= 0 || getHiddenStatus() == HIDDEN_STATUS_SINK"));
        assertTrue(monster.contains("|| getHiddenStatus() == HIDDEN_STATUS_FLY"));
        for (int npcId : new int[] {
                45067, 45264, 45452, 45090, 45321, 45445}) {
            assertTrue(monster.contains("case " + npcId + ":"),
                    "missing retained flying NPC " + npcId);
        }

        String npc = Files.readString(RETAINED.resolve(
                "model/Instance/L1NpcInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(npc.contains("public static final int HIDDEN_STATUS_FLY = 2"));
        assertTrue(npc.contains(
                "getTileLineDistance(this.getLocation()) <= 1"));
        assertTrue(npc.contains("ActionCodes.ACTION_Movedown"));
        assertTrue(npc.contains("setHiddenStatus(HIDDEN_STATUS_NONE)"));
        assertTrue(npc.contains("_target = pc"));
        assertTrue(npc.contains("onNpcAI();"));

        String skill = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        assertTrue(skill.contains(
                "case L1NpcInstance.HIDDEN_STATUS_FLY:\n\t\t\t\treturn false;"));
        String attack = Files.readString(RETAINED.resolve(
                "clientpackets/C_Attack.java"), StandardCharsets.UTF_8);
        assertTrue(attack.contains(
                "hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK || hiddenStatus == L1NpcInstance.HIDDEN_STATUS_FLY"));
    }

    @Test
    void serverUsesMoveupStatusFourAndMovedownAtRadiusOne() throws Exception {
        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("HIDDEN_STATUS_FLY = 2"));
        assertTrue(population.contains(
                "case 45067, 45264, 45452, 45090, 45321, 45445, 45681 -> true"));
        assertTrue(population.contains("hiddenStatus = HIDDEN_STATUS_FLY"));
        assertTrue(population.contains("actorStatus = 4"));
        assertTrue(population.contains("revealFlyNear"));
        assertTrue(population.contains(
                "actor.currentHp != actor.definition.template().hp()"));
        assertTrue(population.contains(
                "Math.abs(y - actor.y)) > 1"));
        assertTrue(population.contains("BattleActionCodes.MOVE_DOWN"));

        String animation = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleAnimationCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(animation.contains("MOVE_UP = 44"));
        assertTrue(animation.contains("MOVE_DOWN = 45"));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("BattleActionCodes.MOVE_UP"));
        assertTrue(session.contains("reveal.actionId()"));

        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertTrue(hub.contains("population.revealFlyNear("));
        assertTrue(hub.contains(
                "!= ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE"));
    }
}
