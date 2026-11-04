package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

/** Cross-source proof for selected Illusionist skill206 Concentration. */
final class NativeConcentrationSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_Icons.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"),
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
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinPcRangeCostDurationBookAndNoAbilityRow()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (206, 0, '專注', 26, 5, 20,"
                + " 0, 0, 0, 1000, 600, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0,"
                + " 3, 0, 0, 32, '$5940', 19, 6527, 0, 0, 0, 280);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (206, '專注', 26, 5, 20, 0,"
                + " 0, 0, 0, 600, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0, 3, 0,"
                + " 0, 32, '$5940', 19, 6527, 0, 0, 0, 280);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (206, '專注', NULL, NULL);"));
            assertTrue(sql.contains("49122, '記憶水晶(專注)',"
                    + " 'skill.Skill_Illusionist', '$5686'"));
            assertFalse(sql.lines().anyMatch(line ->
                    line.contains("skills_buff_ability")
                            && line.contains("(206,")));
        }
    }

    @Test void selectedLiToEmitsOnly141905AndHas1200MillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(1_200, readPeWordAtVa(UNPACKED, 0x63590EL));

        byte[] exact = new byte[] {0x14, 0x19, 0x05};
        assertEquals("14 19 05", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.CONCENTRATION,
                Protocol.selectorOnlySpellRequest(exact).skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(0x14).c(0x19).c(0x05).d(3).bytes()));
    }

    @Test void retainedAndCurrentRuntimePinMprLifecycleAndIconBoundary()
            throws Exception {
        String regen = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"));
        assertTrue(regen.contains("_mpSkills.put(CONCENTRATION, 2);"));
        assertTrue(regen.contains("if (tgpc.hasSkillEffect(skillid))"));
        assertTrue(regen.contains("baseMpr += integer;"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("cha.setSkillEffect(getEffectSkillId(), duration);"));
        assertTrue(use.contains("breakInterruptedCastEffects();"));
        String requestParser = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(requestParser.contains("case 206:// 專注 2050"));
        assertTrue(requestParser.contains("targetId = readD();"));
        String persistence = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(persistence.contains("CONCENTRATION"),
                "retained logout snapshot does not persist Concentration");

        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Icons.java"));
        assertTrue(icons.contains("data[52] = (byte)"
                + " (pc.getSkillEffectTimeSec(CONCENTRATION) / 16);"));
        String asm = read(DISASSEMBLY);
        String fa = function(asm, "sub_4AC1C0");
        assertTrue(fa.contains("aDdsd"));

        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        for (String fragment : new String[] {
                "CONCENTRATION, new RegenSpell(CONCENTRATION, 25, 5, 19, 6527",
                "20, 0, 1_000, 1_200, 600, RequestShape.SELECTOR_ONLY",
                "Targeting.PLAYER_OBJECT, 3",
                "RegenEffect.CONCENTRATION, 2, false"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "resolveSelectorOnlyConcentrationTarget(",
                "target.timedStatus.activateConcentration(",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())",
                "meditationActive, concentrationActive, cookingMpRegen",
                "inBattle || inClassicWorld"}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static String function(String body, String name) {
        int start = body.indexOf(name + " proc near");
        int end = body.indexOf(name + " endp", start);
        assertTrue(start >= 0 && end > start, "missing " + name);
        return body.substring(start, end + name.length() + 5);
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (206,"))
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
        for (int i = 0; i < sections; i++) {
            int section = table + i * 40;
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
