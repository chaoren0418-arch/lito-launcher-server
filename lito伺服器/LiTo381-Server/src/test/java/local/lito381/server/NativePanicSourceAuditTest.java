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

/** Cross-source proof for selected Illusionist skill217 Panic. */
final class NativePanicSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path MODE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/PANIC.java");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path STOP = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillStop.java");
    private static final Path CANCELLATION = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/CANCELLATION.java");
    private static final Path CHAR_BUFF = RETAINED.resolve(
            "src/com/lineage/server/datatables/user/CharBuffTable.java");
    private static final Path MAGIC_PC = RETAINED.resolve(
            "src/com/lineage/server/model/L1MagicPc.java");
    private static final Path PROBABILITY = RETAINED.resolve(
            "src/com/lineage/server/datatables/SkillsProbabilityTable.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test void hashesRowsBookProbabilityAndBossImmunityAreLocked()
            throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("2EB918C93DA4F07F0E4CD12BB826E784955EDE27110DA14213C6EBEB5020CE09",
                sha256(MODE));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("1C48B3681AAA4EEEA671BA9E25837D3D52EE504CA669516B50418CE62DC0AF52",
                sha256(STOP));
        assertEquals("BF802004D6538C879EBD71FA192F93B6357CB3FA063CBD3CCBC7F79800359FB7",
                sha256(CANCELLATION));
        assertEquals("A84F5617B64373F0BC239F87E7B076E6AF618D29F271EA77D633F6761CC20F61",
                sha256(CHAR_BUFF));
        assertEquals("FDEA6BF489E6FEF997BE6DFC48136476E88B701E00F6DBDF6410B918B1A56546",
                sha256(MAGIC_PC));
        assertEquals("28BCAB274658209EA7E392B650ED17DFF8BBCD9E95FE685EA7C9695BFBDE53FA",
                sha256(PROBABILITY));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (217, 0, '恐慌',"
                + " 28, 0, 30, 30, 0, 0, 4000, 64, 'buff', 3"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (217, '恐慌',"
                + " 28, 0, 30, 30, 0, 0, 0, 64, 'buff', 3"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_負面技能控制` VALUES"
                    + " (217, '恐慌', 55, 45, 35, 0, 0, 0.25, 0, 0, 1,"
                    + " 0.05, 0, 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (217, '恐慌', 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (217, '恐慌', NULL, NULL);"));
            assertTrue(sql.contains("49133, '記憶水晶(恐慌)',"
                    + " 'skill.Skill_Illusionist', '$5697'"));
        }
    }

    @Test void clientSelectorClockAndRetainedLifecycleAreExact()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635924L));
        assertEquals(BattleSpellCatalog.PANIC,
                Protocol.selectorOnlySpellRequest(
                        new byte[] {0x14, 0x1B, 0x00}).skillId());

        String mode = read(MODE);
        assertTrue(mode.contains("if (!cha.hasSkillEffect(L1SkillId.PANIC))"));
        for (String stat : new String[] {"Str", "Con", "Dex", "Wis", "Int"}) {
            assertTrue(mode.contains("pc.add" + stat + "((byte) -1);"), stat);
            assertTrue(mode.contains("pc.add" + stat + "(1);"), stat);
            assertTrue(mode.contains("tgnpc.add" + stat + "((byte) -1);"), stat);
            assertTrue(mode.contains("tgnpc.add" + stat + "(1);"), stat);
        }
        assertFalse(mode.contains("addCha"));
        assertTrue(mode.contains(
                "pc.setSkillEffect(L1SkillId.PANIC, integer * 1000);"));
        assertTrue(mode.contains(
                "tgnpc.setSkillEffect(L1SkillId.PANIC, integer * 1000);"));
        assertEquals(2, count(mode, "new S_OwnCharStatus2(pc)"));
        assertFalse(read(CHAR_BUFF).contains("PANIC"));

        String use = read(USE);
        String except = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertFalse(except.contains("PANIC"));
        String curse = use.substring(use.indexOf("case L1Skills.TYPE_CURSE:"),
                use.indexOf("break;", use.indexOf("case L1Skills.TYPE_CURSE:")));
        assertTrue(curse.contains("removeSkillEffect(FOG_OF_SLEEPING)"));
        assertTrue(read(CANCELLATION).contains(
                "for (int skillNum = SKILLS_BEGIN; skillNum <= SKILLS_END; skillNum++)"));
    }

    @Test void implementationCoversBothPhasesAllActorFamiliesAndConsumers()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        for (String fragment : new String[] {
                "PANIC, new PanicSpell(", "PANIC, 27, 0, 19, 6_506",
                "30, 30, 4_000, 0, 64, 3", "55, 45, 35, 0.25D",
                "49_133L, \"$5697\", \"$5951\", false, false"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "usePanicSpell(payload,", "activatePanic(",
                "panicClassicWorldMonster(", "panicQuestMonster(",
                "panicTimeAttackActor(", "consumeCounterMagic(castNow)",
                "cancelSleep(\"probability-spell-\" + spell.skillId())",
                "abilityStatus.panic(nowNanos).abilityScoreModifier()",
                "classicWorldMonsterPanicAdjustedAbility(",
                "timeAttackActorPanicAdjustedAbility(",
                "expirePanic(nowNanos)", "ability.panic()"}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static long count(String value, String needle) {
        long result = 0;
        for (int from = 0; (from = value.indexOf(needle, from)) >= 0;
             from += needle.length()) result++;
        return result;
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
