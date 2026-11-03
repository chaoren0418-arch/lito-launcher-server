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
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector19/0 and retained skill153. */
final class NativeEraseMagicSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void databasesPinSelectedReuseProbabilityBossBypassAndMaterial()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (153, 0, '魔法消除',"
                + " 20, 0, 10, 0, 40319, 1, 600, 32, 'buff', 3, 0, 0, 0,"
                + " 10, 10, 0, 1, 0, -1, 0, 0, 1, '', 19, 10706, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (153, '魔法消除',"
                + " 20, 0, 10, 0, 40319, 1, 0, 32, 'buff', 3, 0, 0, 0,"
                + " 10, 10, 0, 1, 0, -1, 0, 0, 1, '', 19, 10706, 0, 0, 0, 280);"));
        String control = "INSERT INTO `skills_負面技能控制` VALUES (153, '魔法消除',"
                + " 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES"
                + " (153, '魔法消除', 0);";
        String item = "INSERT INTO `skills_item` VALUES"
                + " (153, '魔法消除', NULL, NULL);";
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains(control));
            assertTrue(sql.contains(boss));
            assertTrue(sql.contains(item));
        }
        assertTrue(primary.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (153, '魔法消除');"));
    }

    @Test void retainedExecutorPinsQuarterMrProbabilityConsumptionAndAttackAba()
            throws Exception {
        String character = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1Character.java"));
        assertTrue(character.contains("if (this.hasSkillEffect(ERASE_MAGIC) == true)"));
        assertTrue(character.contains("return this._mr >> 2;// / 4;"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("isSuccess = magic.calcProbabilityMagic(this._skillId);"));
        assertTrue(use.contains("if (this._skillId != ERASE_MAGIC)"));
        assertTrue(use.contains("cha.removeSkillEffect(ERASE_MAGIC); // 魔法消除"));
        assertTrue(use.contains("eraseMagicLease = cha.captureSkillEffectLease(ERASE_MAGIC);"));
        assertTrue(use.contains("target.killSkillEffectTimer(ERASE_MAGIC, eraseMagicLease);"));
        assertTrue(use.contains("case ERASE_MAGIC:"));
        assertTrue(use.contains("|| (cha instanceof L1SummonInstance)"));
        assertTrue(use.contains("|| (cha instanceof L1PetInstance)"));

        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Icons.java"));
        assertTrue(icons.contains("data[21] = (byte)"
                + " (pc.getSkillEffectTimeSec(ERASE_MAGIC) / 4);"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("ERASE_MAGIC"));
    }

    @Test void selectedLiToPinsCase152Range10CccdhhAndZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(DISASSEMBLY, "loc_4CB755:", 28);
        assertTrue(selector.contains(
                "cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(selector.contains("mov     edx, 0Ah"));
        assertTrue(selector.contains("call    sub_4CA6E0"));
        String producer = windowAfter(DISASSEMBLY, "loc_4CA77C:", 42);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.ERASE_MAGIC - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesAllActorFamiliesAndExactLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.eraseMagicSpell(selector.skillId())"));
        assertTrue(session.contains("private void useEraseMagicSpell("));
        assertTrue(session.contains("target.eraseMagicStatus.activate("));
        assertTrue(session.contains("controlledCompanion.activateEraseMagic("));
        assertTrue(session.contains("hub.eraseMagicClassicWorldMonster("));
        assertTrue(session.contains("hub.eraseMagicQuestMonster("));
        assertTrue(session.contains("hub.eraseMagicTimeAttackActor("));
        assertTrue(session.contains("consumeEraseMagicAfterProbability("));
        assertTrue(session.contains("captureQuestMonsterEraseMagic("));
        assertTrue(session.contains("consumeQuestMonsterEraseMagic("));
        assertTrue(session.contains(
                "icon=omitted-no-selected-LiTo-packetbox-receiver"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleEraseMagicStatus.java"));
        assertTrue(status.contains("baseMagicResistance >> 2"));
        assertTrue(status.contains("generation != lease.generation()"));
        assertTrue(status.contains("expiresAtNanos != lease.expiresAtNanos()"));
        String companion = read(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"));
        assertTrue(companion.contains("private final BattleEraseMagicStatus eraseMagicStatus"));
        assertTrue(companion.contains("synchronized boolean activateEraseMagic("));
        assertTrue(companion.contains("eraseMagicStatus.clear();"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) {
            count++;
        }
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        assertEquals(0x0000_4550, image.getInt(pe), "invalid PE signature");
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
                int fileOffset = Math.toIntExact(rawOffset + rva - sectionRva);
                return Short.toUnsignedInt(image.getShort(fileOffset));
            }
        }
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = Files.newBufferedReader(
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
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
