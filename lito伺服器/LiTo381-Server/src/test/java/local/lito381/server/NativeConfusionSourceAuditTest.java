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
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill202 Confusion. */
final class NativeConfusionSourceAuditTest {
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

    @Test void selectedRowsPinPrimaryReuseProbabilityBossBookAndNoImmunity()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (202, 0, '混亂', 26, 1,"
                + " 15, 10, 0, 0, 3000, 8, 'attack', 3, 11, 0, 0, 35, 0,"
                + " 0, 4, 0, 3, 0, 0, 2, '$5936', 18, 6525, 0, 0, 0, 0);",
                skillRow(primary, 202));
        assertEquals("INSERT INTO `skills` VALUES (202, '混亂', 26, 1, 15,"
                + " 10, 0, 0, 0, 8, 'attack', 3, 11, 0, 0, 35, 0, 0, 4,"
                + " 0, 3, 0, 0, 2, '$5936', 18, 6525, 0, 0, 0, 0);",
                skillRow(secondary, 202));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (202, '混亂-固定隨機機率', 25, 25, 25, 0, 0, 0, 0, 0,"
                + " 1, 0, 0, 0);";
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains(probability));
            assertTrue(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (202, '混亂', 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (202, '混亂', NULL, NULL);"));
            assertFalse(sql.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                    + " (202,"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (202,"));
        }
        assertTrue(primary.contains("49118, '記憶水晶(混亂)',"
                + " 'skill.Skill_Illusionist', '$5682'"));
    }

    @Test void selectedLiToEmitsOnly141901AndHasZeroCastClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635906L));
        assertEquals("14 19 01", Protocol.hex(
                new byte[] {0x14, 0x19, 0x01}));
    }

    @Test void retainedRuntimePinsCurseOrderZeroDamageAndNoRefresh()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/CONFUSION.java"));
        String original381 = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/skill/"
                        + "skillmode/CONFUSION.java"));
        for (String source : new String[] {mode, original381}) {
            assertTrue(source.contains("final int dmg = 0;"));
            assertTrue(source.contains("if (!cha.hasSkillEffect(L1SkillId.SILENCE))"));
            assertTrue(source.contains(
                    "cha.setSkillEffect(L1SkillId.SILENCE, integer * 1000)"));
            assertTrue(source.contains("new S_ServerMessage(1339)"));
            assertTrue(source.contains("return dmg;"));
        }

        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.indexOf("SkillsProbabilityTable.get().getConfig(skillId)")
                < magic.indexOf("case CONFUSION:// 混亂"));
        assertTrue(magic.contains("final int trad = _random.nextInt(10) + 20;"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("case L1Skills.TYPE_CURSE:"));
        assertTrue(use.contains("isSuccess = magic.calcProbabilityMagic(this._skillId)"));
        int probability = use.indexOf(
                "isSuccess = magic.calcProbabilityMagic(this._skillId)");
        int erase = use.indexOf("cha.removeSkillEffect(ERASE_MAGIC)", probability);
        int sleep = use.indexOf("cha.removeSkillEffect(FOG_OF_SLEEPING)", erase);
        int counter = use.indexOf("this.isUseCounterMagic(cha)", sleep);
        assertTrue(probability < erase && erase < sleep && sleep < counter);
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertFalse(exceptCounter.contains("CONFUSION"));
    }

    @Test void implementationPinsBothPhasesAndProbabilityOrdering()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "BattleSpellCatalog.confusionSpell(",
                "useSelectorOnlyTargetedAttackSpell(",
                "config.confusionFixtureD100()",
                "consumeEraseMagicAfterProbability(",
                "classic-world-probability-spell-",
                "activateSilenceIfAbsent(",
                "confusionSilenceQuestMonster(",
                "confusionSilenceTimeAttackActor(",
                "confusionSilenceClassicWorldMonster(",
                "confusion.targetMessageCode()"}) {
            assertTrue(session.contains(fragment),
                    "missing Confusion implementation: " + fragment);
        }
        assertTrue(session.contains("if (confusion != null) {\n"
                + "            damage = 0;"));
    }

    private static String skillRow(String sql, int skillId) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (" + skillId + ","))
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
