package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the native, retained and server boundaries for empty skill slots 81..86. */
final class NativeReservedGeneralSkillGapSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void bothDatabasesAndRetainedSkillIdsClassify81Through86AsNone()
            throws Exception {
        String selected = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        String[] selectedRows = {
            "INSERT INTO `skills` VALUES (81, 0, 'none', 0, 0, 0, 0, 0, 0, 2000, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (82, 0, 'none', 0, 0, 0, 0, 0, 0, 923, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (83, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (84, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (85, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 900, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 2059, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (86, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 30, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 760, 0, 0, 0, 0);",
        };
        String[] secondaryRows = {
            "INSERT INTO `skills` VALUES (81, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (82, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (83, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (84, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 0, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (85, 'none', 0, 0, 0, 0, 0, 0, 0, 900, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 2059, 0, 0, 0, 0);",
            "INSERT INTO `skills` VALUES (86, 'none', 0, 0, 0, 0, 0, 0, 0, 30, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 19, 760, 0, 0, 0, 0);",
        };
        for (int index = 0; index < selectedRows.length; index++) {
            int skillId = 81 + index;
            assertTrue(selected.contains(selectedRows[index]));
            assertTrue(secondary.contains(secondaryRows[index]));
            String item = "INSERT INTO `skills_item` VALUES (" + skillId
                    + ", 'none', NULL, NULL);";
            assertTrue(selected.contains(item));
            assertTrue(secondary.contains(item));
            for (String table : new String[] {
                    "skills_buff_ability", "skills_負面boss免疫",
                    "skills_負面技能控制", "skills_相消免疫技能"}) {
                assertFalse(selected.contains(
                        "INSERT INTO `" + table + "` VALUES (" + skillId + ","));
                assertFalse(secondary.contains(
                        "INSERT INTO `" + table + "` VALUES (" + skillId + ","));
            }
        }
        String skillIds = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillIds.contains("// none = 81 - 86"));
    }

    @Test
    void selectedClientProducesCccButServerClassifiesEverySlotAsReserved()
            throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int start = assembly.indexOf("sub_4CB5F0 proc near");
        int end = assembly.indexOf("sub_4CB5F0 endp", start);
        String producer = assembly.substring(start, end);
        assertTrue(producer.contains(
                "default case, cases 23,80,82-85,87,88,91,93-95"));
        assertTrue(producer.contains(
                "loc_4CBBD4:                             ; CODE XREF: sub_4CB5F0+7E"));
        assertTrue(producer.contains(
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81"));
        int fallbackStart = producer.indexOf("def_4CB66E:");
        int fallbackEnd = producer.indexOf("loc_4CBC76:", fallbackStart);
        String fallback = producer.substring(fallbackStart, fallbackEnd);
        assertTrue(fallback.contains("and     edx, 7"));
        assertTrue(fallback.contains("sar     esi, 3"));
        assertTrue(fallback.contains("push    14h"));
        assertTrue(fallback.contains("call    sub_4C6AE0"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("RESERVED_GENERAL_81 = 81"));
        assertTrue(catalog.contains("RESERVED_GENERAL_86 = 86"));
        assertTrue(catalog.contains(
                "&& requestShape == RequestShape.SELECTOR_ONLY"));
        String mastery = Files.readString(Path.of(
                "src/main/java/local/lito381/server/NativeSkillMastery.java"),
                StandardCharsets.UTF_8);
        assertTrue(mastery.contains(
                "BattleSpellCatalog.reservedSkillSlot(skillId) != null"));
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains(
                "C_USE_SPELL rejected reserved level-zero slot="));
    }
}
