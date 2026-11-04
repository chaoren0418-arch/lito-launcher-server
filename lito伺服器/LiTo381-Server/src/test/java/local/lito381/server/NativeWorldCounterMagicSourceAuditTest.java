package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill31 production, source exceptions and full ward lifecycle. */
final class NativeWorldCounterMagicSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path WORKSPACE = Path.of("..");
    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");

    @Test
    void selectedDatabaseAndLiToProducerStayExact() throws Exception {
        String database = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (31, 0, '魔法屏障', 4, 6, 20, 0, "
                + "40318, 1, 10, 16, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, "
                + "0, 0, 64, '$1980', 19, 10701, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (31, '魔法屏障', NULL, NULL);"));

        String retainedIds = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"));
        assertTrue(retainedIds.contains("public static final int COUNTER_MAGIC = 31;"));
        String retainedUse = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(retainedUse.contains("case 31:// 魔法屏障 1250"));

        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatch = between(assembly, "loc_4CBBD4:", "loc_4CBBED:");
        assertTrue(dispatch.contains("cases 1,2,12,13,30,31"));
        assertTrue(dispatch.contains("and     ecx, 7"));
        assertTrue(dispatch.contains("sar     esi, 3"));
        assertTrue(dispatch.contains("push    14h"));
        assertTrue(dispatch.contains("push    offset off_5028CC"));
        assertTrue(dispatch.contains("call    sub_4C6AE0"));
    }

    @Test
    void retainedHardcodedAndSelectedDatabaseExceptionsAreMerged() throws Exception {
        String source = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        String exceptions = between(source,
                "private static final int[] EXCEPT_COUNTER_MAGIC", "};");
        for (String value : new String[] {"1, 2, 3, 5", "26, 31, 32", "37",
                "42, 43, 44", "SHOCK_STUN", "COUNTER_BARRIER", "ARMOR_BREAK",
                "MIRROR_IMAGE", "ILLUSION_AVATAR"}) {
            assertTrue(exceptions.contains(value), value);
        }
        assertTrue(source.contains(
                "SkillCounterMagicExceptionTable.get().contains(_skillId)"));
        assertTrue(source.contains("cha.removeSkillEffect(COUNTER_MAGIC);"));
        assertTrue(source.contains("new S_SkillSound(cha.getId(), castgfx)"));

        String database = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        for (int skill : Set.of(44, 87, 112, 132, 153, 161, 173, 174, 187, 188, 192)) {
            assertTrue(database.contains(
                    "INSERT INTO `skills_魔屏無效技能` VALUES (" + skill + ","),
                    Integer.toString(skill));
        }
    }

    @Test
    void serverActivationConsumptionCancellationAndWorldExpiryStayConnected()
            throws Exception {
        String session = read(SESSION);
        String activate = between(session,
                "private void useCounterMagicSpell(",
                "private void useAbsoluteBarrierSpell(");
        assertTrue(activate.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(activate.contains("phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(activate.contains("removeCatalog("));
        assertTrue(activate.contains(
                "timedStatus.activateCounterMagic(now, spell.durationSeconds())"));
        assertTrue(activate.contains("S_EFFECT counter-magic-activate"));

        assertTrue(session.contains("publishCounterMagicConsumption("));
        assertTrue(session.contains("sourceCounterMagicBypassSkills.contains("));
        assertTrue(session.contains("playerTarget.timedStatus.cancelMagicEffects()"));
        assertTrue(session.contains("timed.counterMagic()"));
        assertTrue(session.contains("timedStatus.expireCounterMagic(nowNanos)\n"
                + "                && (inBattle || inClassicWorld)"));

        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateCounterMagic(start, 16);
        assertTrue(status.consumeCounterMagic(start + 1));
        assertFalse(status.consumeCounterMagic(start + 2));
        status.activateCounterMagic(start, 16);
        assertTrue(status.cancelMagicEffects().counterMagic());
        assertFalse(status.counterMagicActive(start));
        status.activateCounterMagic(start, 16);
        assertTrue(status.expireCounterMagic(start + TimeUnit.SECONDS.toNanos(16)));
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(begin >= 0 && finish > begin, start + " .. " + end);
        return source.substring(begin, finish);
    }
}
