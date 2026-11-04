package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks LiTo.bin case8, selected skill9 data and retained cure semantics. */
final class NativeWorldCurePoisonSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CURE_POISON.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/REMOVE_CURSE.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    @Test
    void nativeProducerSelectedRowRetainedMeaningAndServerRouteStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (9, 0, '解毒術', 2, 0, 15, 0, 0, 0, 10, 0, 'buff', 3, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 1, '$1441', 19, 871, 0, 211, 0, 280);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(dispatch.contains("cases 7,8,22,36,42,43,47,54"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        String clientUse = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String objectCases = between(clientUse, "case 1://", "case 2://");
        assertTrue(objectCases.contains("case 9:// 解毒術"));
        assertTrue(objectCases.contains("targetId = readD();"));

        String curePoison = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/skillmode/CURE_POISON.java"),
                StandardCharsets.UTF_8);
        assertTrue(curePoison.contains("cha.curePoison();"));
        assertTrue(!curePoison.contains("cha.cureParalaysis();"));
        String removeCurse = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/skillmode/REMOVE_CURSE.java"),
                StandardCharsets.UTF_8);
        assertTrue(removeCurse.contains("STATUS_CURSE_PARALYZING"));
        assertTrue(removeCurse.contains("STATUS_CURSE_PARALYZED"));
        assertTrue(removeCurse.contains("cha.cureParalaysis();"));

        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "CURE_POISON, new CurseRemovalSpell(CURE_POISON, 1, 0, 19, 871,"));
        assertTrue(catalog.contains(
                "15, 10, -1, false, \"cure-poison\")"));
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains(
                "BattleSpellCatalog.curseRemovalSpell(selector.skillId())"));
        assertTrue(session.contains(
                "useClassicWorldCurseRemovalSpell(payload, worldCurseRemoval)"));
        assertTrue(session.contains("if (spell.curesCurseParalysis())"));
        assertTrue(session.contains(
                "playerTarget.timedStatus.curePoisonSilence()"));
        assertTrue(session.contains(
                "classicWorldPoisonKind\n                    == ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
