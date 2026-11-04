package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill216 Insight. */
final class NativeInsightSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path MODE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/INSIGHT.java");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path STOP = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillStop.java");
    private static final Path CANCELLATION = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/CANCELLATION.java");
    private static final Path STATUS2 = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_OwnCharStatus2.java");
    private static final Path CHAR_BUFF = RETAINED.resolve(
            "src/com/lineage/server/datatables/user/CharBuffTable.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void retainedInputsHaveLockedHashesAndExactRows() throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("9B64DE9C636B5FA96C9A4E668C4EF6376A4454E6D8B403289BE49A61E6A175E8",
                sha256(MODE));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("1C48B3681AAA4EEEA671BA9E25837D3D52EE504CA669516B50418CE62DC0AF52",
                sha256(STOP));
        assertEquals("BF802004D6538C879EBD71FA192F93B6357CB3FA063CBD3CCBC7F79800359FB7",
                sha256(CANCELLATION));
        assertEquals("363D5C64F19995539132D5E8E730C55CCF2C7B3220330DD68C02E15E5D999B6B",
                sha256(STATUS2));
        assertEquals("A84F5617B64373F0BC239F87E7B076E6AF618D29F271EA77D633F6761CC20F61",
                sha256(CHAR_BUFF));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (216, 0, '洞察', 27, 7, 40,"
                + " 0, 0, 0, 1000, 600, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0,"
                + " 3, 0, 0, 128, '$5950', 19, 6505, 0, 0, 0, 280);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (216, '洞察', 27, 7, 40, 0,"
                + " 0, 0, 0, 600, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0, 3, 0,"
                + " 0, 128, '$5950', 19, 6505, 0, 0, 0, 280);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (216, '洞察', NULL, NULL);"));
            assertTrue(sql.contains("49132, '記憶水晶(洞察)',"
                    + " 'skill.Skill_Illusionist', '$5696'"));
        }
        assertTrue(count(primary,
                "INSERT INTO `skill_狀態天賦設置` VALUES (2707, 216, '洞察'") > 0);
    }

    @Test void selectedLiToEmitsOnly141a07AndPins3600msClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(3_600, readPeWordAtVa(UNPACKED, 0x635922L));
        byte[] exact = new byte[] {0x14, 0x1A, 0x07};
        assertEquals("14 1A 07", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.INSIGHT,
                Protocol.selectorOnlySpellRequest(exact).skillId());
    }

    @Test void retainedModePinsFiveStatsNoChaNoRefreshAndFullStop()
            throws Exception {
        String mode = read(MODE);
        assertTrue(mode.contains("if (!cha.hasSkillEffect(L1SkillId.INSIGHT))"));
        for (String stat : new String[] {"Str", "Con", "Dex", "Wis", "Int"}) {
            assertTrue(mode.contains("pc.add" + stat + "(1);"), stat);
            assertTrue(mode.contains("pc.add" + stat + "(-1);"), stat);
            assertTrue(mode.contains("tgnpc.add" + stat + "(1);"), stat);
            assertTrue(mode.contains("tgnpc.add" + stat + "(-1);"), stat);
        }
        assertFalse(mode.contains("addCha"));
        assertTrue(mode.contains(
                "pc.setSkillEffect(L1SkillId.INSIGHT, integer * 1000);"));
        assertEquals(2, count(mode, "new S_OwnCharStatus2(pc)"));
        String npcCaster = mode.substring(mode.indexOf(
                "public int start(final L1NpcInstance npc"),
                mode.indexOf("public void start(final L1PcInstance srcpc"));
        assertFalse(npcCaster.contains("setSkillEffect"));

        String use = read(USE);
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("INSIGHT"));
        String cancellation = read(CANCELLATION);
        assertTrue(cancellation.contains(
                "for (int skillNum = SKILLS_BEGIN; skillNum <= SKILLS_END; skillNum++)"));
        String ids = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"));
        assertTrue(ids.contains("public static final int INSIGHT = 216;"));
        assertTrue(ids.contains("public static final int SKILLS_END = 220;"));
        assertFalse(read(CHAR_BUFF).contains("INSIGHT"));
    }

    @Test void implementationPinsBothPhasesConsumersAndLifecycle()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbilityStatus.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "INSIGHT, new InsightSpell(",
                "INSIGHT, 26, 7, 19, 6_505",
                "40, 1_000, 3_600, 600, 3",
                "1, 2, 128, 1",
                "49_132L, \"$5696\", \"$5950\", true, false",
                "RequestShape.SELECTOR_ONLY"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "activateInsight(", "insight(long nowNanos)",
                "expireInsight(", "clearInsight();", "boolean insight,"}) {
            assertTrue(status.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.insightSpell(",
                "useInsightSpell(payload,", "activateInsight(",
                "abilityStatus.insight(nowNanos).abilityScoreModifier()",
                "effectiveConstitution(long nowNanos)",
                "baseIntelligence(long nowNanos)",
                "sendAbilityScoresStatus(", "ability.insight()",
                "expireInsight(nowNanos)"}) {
            assertTrue(session.contains(fragment), fragment);
        }
        assertFalse(session.contains("2707, 216"),
                "custom talent table is not the native base effect");
    }

    private static long count(String value, String needle) {
        long result = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0;
             from += needle.length()) result++;
        return result;
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (216,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(Math.toIntExact(
                        rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE VA");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
