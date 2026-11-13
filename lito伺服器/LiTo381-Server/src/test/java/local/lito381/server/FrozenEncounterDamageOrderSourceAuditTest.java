package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Issue #384（#338 Z7）：凍結中的 Quest／Time Attack 怪物傷害為 0 之後，造成冰凍的那一擊
 * 必須「先扣血、再寫凍結」，否則會被自己擋掉。這裡鎖住 ClientSession 三條冰凍技能路徑
 * （範圍、單體、直線）與 NativeSourceTargetNpc 佇列路徑的先後順序。
 */
final class FrozenEncounterDamageOrderSourceAuditTest {
    private static final Path MAIN = Path.of("src/main/java/local/lito381/server");
    /** 單體路徑的扣血與凍結之間隔著約 60 行的其他目標分支。 */
    private static final int WINDOW_LINES = 90;

    private static List<String> lines(String name) throws Exception {
        return Files.readAllLines(MAIN.resolve(name), StandardCharsets.UTF_8);
    }

    private static int assertDamagePrecedesFreeze(
            List<String> lines, String freezeCall, String damageCall) {
        int sites = 0;
        for (int index = 0; index < lines.size(); index++) {
            if (!lines.get(index).contains(freezeCall)) continue;
            sites++;
            boolean found = false;
            for (int back = index - 1;
                    back >= Math.max(0, index - WINDOW_LINES); back--) {
                if (lines.get(back).contains(damageCall)) {
                    found = true;
                    break;
                }
            }
            int line = index + 1;
            assertTrue(found, () -> freezeCall + " at ClientSession.java:" + line
                    + " is not preceded by " + damageCall);
        }
        return sites;
    }

    @Test
    void everyFreezeSpellDamagesTheEncounterNpcBeforeWritingTheFreeze()
            throws Exception {
        List<String> session = lines("ClientSession.java");
        assertTrue(assertDamagePrecedesFreeze(session,
                "this.hub.freezeQuestMonster(",
                "NativeSourceNpcDamageDisplay.magicQuest(") >= 3);
        assertTrue(assertDamagePrecedesFreeze(session,
                "this.hub.freezeTimeAttackActor(",
                "NativeSourceNpcDamageDisplay.magicTime(") >= 3);
    }

    @Test
    void queuedFreezeIsReleasedOnlyAfterTheHpCommitWasAccepted() throws Exception {
        String queued = String.join("\n", lines("NativeSourceTargetNpc.java"));
        assertTrue(queued.contains("!claim(m,p.accepted)"));
        assertTrue(queued.contains(
                "static boolean claim(Mutation mutation,boolean accepted){"
                        + "return mutation!=null&&accepted&&"));
    }

    @Test
    void encounterDamageChecksFreezeTogetherWithEarthBindButPoisonBypassesBoth()
            throws Exception {
        for (String name : List.of(
                "BattleQuestEncounter.java", "BattleTimeAttackEncounter.java")) {
            String source = String.join("\n", lines(name));
            assertTrue(source.contains(
                    "return this.earthBindActive(nowNanos) || this.freezeActive(nowNanos);"),
                    name);
            assertTrue(source.contains(
                    "NativeCombatAttacker.earth(earthBindBlocks, this)"
                            + " && this.damageZeroActive(System.nanoTime())"), name);
            assertTrue(source.contains("return damage(amount, false);"), name);
        }
    }
}
