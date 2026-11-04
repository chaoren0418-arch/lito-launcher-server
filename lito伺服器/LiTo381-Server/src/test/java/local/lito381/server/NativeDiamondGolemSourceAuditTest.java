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

/** Cross-source proof for selected Illusionist skill214 Diamond Golem. */
final class NativeDiamondGolemSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CANCELLATION.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path STOP = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillStop.java");
    private static final Path CANCELLATION = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/CANCELLATION.java");
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
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("1C48B3681AAA4EEEA671BA9E25837D3D52EE504CA669516B50418CE62DC0AF52",
                sha256(STOP));
        assertEquals("BF802004D6538C879EBD71FA192F93B6357CB3FA063CBD3CCBC7F79800359FB7",
                sha256(CANCELLATION));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (214, 0, '幻覺：鑽石高崙', 27, 5,"
                + " 25, 0, 0, 0, 2000, 64, 'buff', 1, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 3, 0, 0, 32, '$5948', 19, 6576, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (214, '幻覺：鑽石高崙', 27, 5, 25,"
                + " 0, 0, 0, 0, 64, 'buff', 1, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 3, 0, 0, 32, '$5948', 19, 6576, 0, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertEquals(1, count(sql, "INSERT INTO `skills_buff_ability` VALUES"
                    + " (214, '幻覺：鑽石高崙', 'AC', -20);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (214, '幻覺：鑽石高崙', NULL, NULL);"));
            assertTrue(sql.contains("49130, '記憶水晶(幻覺：鑽石高侖)',"
                    + " 'skill.Skill_Illusionist', '$5694'"));
        }
    }

    @Test void selectedLiToEmitsOnly141a05AndHasZeroCastClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x63591EL));
        byte[] exact = new byte[] {0x14, 0x1A, 0x05};
        assertEquals("14 1A 05", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.ILLUSION_DIAMOND_GOLEM,
                Protocol.selectorOnlySpellRequest(exact).skillId());
    }

    @Test void retainedRuntimePinsPcRefreshCounterBypassCancellationAndNoPersistence()
            throws Exception {
        String use = read(USE);
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("ILLUSION_DIA_GOLEM"));
        assertTrue(use.contains("this._skillId == ILLUSION_DIA_GOLEM"));
        assertTrue(use.contains("pc.updateBuffStatus()"));
        String repeated = use.substring(use.indexOf("ALWAYS_REPEATED_SKILLS"),
                use.indexOf("private void deleteRepeatedSkills"));
        assertFalse(repeated.contains("ILLUSION_DIA_GOLEM"));
        String stop = read(STOP);
        assertTrue(stop.contains("case ILLUSION_DIA_GOLEM"));
        assertTrue(stop.contains("pc.updateBuffStatus()"));

        String cancellation = read(CANCELLATION);
        assertTrue(cancellation.contains(
                "for (int skillNum = SKILLS_BEGIN; skillNum <= SKILLS_END; skillNum++)"));
        String skillIds = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"));
        assertTrue(skillIds.contains("public static final int ILLUSION_DIA_GOLEM = 214;"));
        assertTrue(skillIds.contains("public static final int SKILLS_END = 220;"));
        String primary = read(PRIMARY);
        int immunityTable = primary.indexOf(
                "CREATE TABLE `skills_相消免疫技能`");
        int nextTable = primary.indexOf("CREATE TABLE `", immunityTable + 20);
        String notCancelable = primary.substring(immunityTable, nextTable);
        assertFalse(notCancelable.contains("VALUES (214,"),
                "selected skill214 is cancelable");
        String charBuff = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(charBuff.contains("ILLUSION_DIA_GOLEM"));
    }

    @Test void implementationPinsBothPhasesAcAndCompleteLifecycle()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbilityStatus.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "ILLUSION_DIAMOND_GOLEM, new DiamondGolemSpell(",
                "ILLUSION_DIAMOND_GOLEM, 26, 5, 19, 6_576",
                "25, 2_000, 0, 64, 3", "1, 2, 32, -20",
                "49_130L, \"$5694\", \"$5948\", true, false",
                "RequestShape.SELECTOR_ONLY"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "activateDiamondGolem(", "diamondGolem(long nowNanos)",
                "expireDiamondGolem(", "clearDiamondGolem();",
                "boolean diamondGolem,"}) {
            assertTrue(status.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.diamondGolemSpell(",
                "useDiamondGolemSpell(payload,", "activateDiamondGolem(",
                "abilityStatus.diamondGolem(nowNanos).armorClass()",
                "Protocol.armorClass(", "ability.diamondGolem()",
                "expireDiamondGolem(nowNanos)"}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static long count(String value, String needle) {
        long result = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0;
             from += needle.length()) result++;
        return result;
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (214,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
