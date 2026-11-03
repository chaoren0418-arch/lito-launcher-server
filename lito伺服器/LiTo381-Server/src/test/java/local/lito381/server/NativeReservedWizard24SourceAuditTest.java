package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected-client producer and retained level-zero semantics for skill24. */
final class NativeReservedWizard24SourceAuditTest {
    @Test
    void nativeProducerSelectedRowsAndExplicitReservedRouteStayExact()
            throws Exception {
        String primary = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (24, 0, 'none', 0, 7, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, '', 19, 189, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (24, 'none', NULL, NULL);"));

        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (24, 'none', 0, 7, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, '', 19, 189, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (24, 'none', NULL, NULL);"));

        String skillIds = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillIds.contains("// none = 24"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly,
                "sub_4CB5F0 proc near", "loc_4CB675:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E default case, cases 23,80,82-85"));
        String producer = between(assembly, "def_4CB66E:", "loc_4CBC76:");
        assertTrue(producer.contains("and     edx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset off_5028CC"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("static final int RESERVED_WIZARD_24 = 24;"));
        assertTrue(catalog.contains(
                "Map.entry(RESERVED_WIZARD_24, new ReservedSkillSlot("));
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("C_USE_SPELL rejected reserved slot="));
        assertTrue(session.contains(
                "C_USE_SPELL rejected reserved level-zero slot="));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
