package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector21/5 and retained skill174. */
final class NativeStrikerGaleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/config/ConfigSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/skill/Skill_SpiritCrystal_Wind.java"),
                Path.of("../1-伺服器/src/com/lineage/list/PcLvSkillList.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackNpc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_Icons.java"),
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
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path ASM = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedBinaryPinsCase173CccdhhRangeAndThreeSecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String selector = windowAfter(ASM, "loc_4CB755:", 28);
        assertTrue(selector.contains(
                "cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(selector.contains("mov     edx, 0Ah"));
        assertTrue(selector.contains("call    sub_4CA6E0"));
        String producer = windowAfter(ASM, "loc_4CA77C:", 42);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));
        assertEquals(3_000, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.STRIKER_GALE - 1) * 2L));
    }

    @Test void bothDatabasesPinSkillProbabilityBossAndCounterMagicProfiles()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (174, 0, '精準射擊',"
                + " 22, 5, 10, 0, 40319, 5, 3000, 192, 'buff', 3, 0, 0, 0,"
                + " 50, 30, 8, 1, 0, -1, 0, 0, 32, '', 19, 5826, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (174, '精準射擊',"
                + " 22, 5, 10, 0, 40319, 5, 0, 192, 'buff', 3, 0, 0, 0,"
                + " 50, 30, 8, 1, 0, -1, 0, 0, 32, '', 19, 5826, 0, 0, 0, 280);"));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (174, '精準射擊', 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES"
                + " (174, '精準射擊', 0);";
        assertTrue(primary.contains(probability));
        assertTrue(secondary.contains(probability));
        assertTrue(primary.contains(boss));
        assertTrue(secondary.contains(boss));
        assertTrue(tableBlock(primary, "skills_魔屏無效技能").contains(
                "VALUES (174, '精準射擊');"));
        assertFalse(tableBlock(secondary, "skills_魔屏無效技能")
                .contains("VALUES (174,"));
    }

    @Test void retainedServerPinsAcquisitionErDamageAndTransientIconEvidence()
            throws Exception {
        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 174:// 精準射擊"));
        assertTrue(request.contains("targetId = readD();"));
        assertTrue(request.contains("checkLoc = true;"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertOrdered(pc, "public int getEr() {",
                "if (this.hasSkillEffect(STRIKER_GALE))", "return 0;");
        String attackPc = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java"));
        assertTrue(attackPc.contains("_targetPc.hasSkillEffect(STRIKER_GALE)"
                + " && (_weaponType == 20 || _weaponType == 62)"));
        assertTrue(attackPc.contains("_targetNpc.hasSkillEffect(STRIKER_GALE)"
                + " && (_weaponType == 20 || _weaponType == 62)"));
        assertTrue(attackPc.contains("_damage = (int) (_damage * ConfigSkill.STRIKER_DMG2);"));
        String attackNpc = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackNpc.java"));
        assertTrue(attackNpc.contains("_targetPc.hasSkillEffect(STRIKER_GALE)"
                + " && !isShortDistance()"));
        assertOrdered(attackNpc, "public boolean isShortDistance",
                "getTileLineDistance(new Point(_targetX, _targetY)) > 1",
                "final int bowActId = _npc.getBowActId()",
                "isShortDistance = false");
        String config = read(RETAINED.resolve(
                "src/com/lineage/config/ConfigSkill.java"));
        assertTrue(config.contains("STRIKER_DMG2 = _getDouble(p, \"STRIKER_DMG2\", 1.2);"));
        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertOrdered(list, "if ((elfAttr & 8) != 0)",
                "skillList.add(STRIKER_GALE - 1)");
        String crystal = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/skill/Skill_SpiritCrystal_Wind.java"));
        assertOrdered(crystal, "nameId.equalsIgnoreCase(\"$4718\")",
                "skillid = STRIKER_GALE;", "magicLv = 15;");
        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Icons.java"));
        assertTrue(icons.contains("data[30] = (byte)"
                + " (pc.getSkillEffectTimeSec(STRIKER_GALE) / 4);"));
        assertEquals(0xFA, Protocol.S_LITO_CLAN);
        assertFalse(read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"))
                .contains("STRIKER_GALE"));
    }

    @Test void implementationPinsAllActorFamiliesErAndPhysicalPipelines()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("STRIKER_GALE, new StrikerGaleSpell("));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("worldStrikerGale"));
        assertTrue(session.contains("useTargetNegativeStatusSpell"));
        assertTrue(session.contains("activateStrikerGale"));
        assertTrue(session.contains("strikerGaleClassicWorldMonster"));
        assertTrue(session.contains("strikerGaleQuestMonster"));
        assertTrue(session.contains("strikerGaleTimeAttackActor"));
        assertTrue(session.contains("if (timedStatus.strikerGaleActive(nowNanos)) return 0;"));
        assertTrue(occurrences(session,
                "BattleStrikerGale.damageAfterStatus(") >= 5);
        assertTrue(session.contains("sourceCounterMagicBypassSkills.contains(spell.skillId())"));
        assertTrue(session.contains("persisted=false"));
        assertTrue(read(Path.of(
                "src/main/java/local/lito381/server/BattleTimedStatus.java"))
                .contains("strikerGaleExpiresAtNanos"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int sections = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = sections + index * 40;
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
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String tableBlock(String sql, String table) {
        String marker = "-- Table structure for " + table;
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table block " + table);
        int end = sql.indexOf("-- Table structure for ", start + marker.length());
        return sql.substring(start, end < 0 ? sql.length() : end);
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int found = text.indexOf(fragment, previous + 1);
            assertTrue(found > previous, "missing/out-of-order: " + fragment);
            previous = found;
        }
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = LocalResources.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!found && line.contains(marker)) {
                    found = true;
                    remaining = followingLines;
                }
                if (found) {
                    result.append(line).append('\n');
                    if (remaining-- == 0) break;
                }
            }
        }
        assertTrue(found, "missing IDA marker: " + marker);
        return result.toString();
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
