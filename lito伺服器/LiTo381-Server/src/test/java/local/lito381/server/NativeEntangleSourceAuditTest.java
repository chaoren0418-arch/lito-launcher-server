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

/** Cross-source proof for native LiTo selector18/7 and retained skill152. */
final class NativeEntangleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/SkillBuffIconHelper.java"),
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
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinSkillProbabilityBossImmunityAndMaterial()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (152, 0, '地面障礙',"
                + " 19, 7, 20, 0, 40319, 1, 0, 64, 'buff', 3, 0, 0, 0,"
                + " 32, 18, 0, 1, 0, 10, 0, 0, 128, '', 19, 2250, 0, 711, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (152, '地面障礙',"
                + " 19, 7, 20, 0, 40319, 1, 0, 64, 'buff', 3, 0, 0, 0,"
                + " 32, 18, 0, 1, 0, 10, 0, 0, 128, '', 19, 2250, 0, 711, 0, 280);"));
        String control = "INSERT INTO `skills_負面技能控制` VALUES (152, '地面障礙',"
                + " 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES"
                + " (152, '地面障礙', 0);";
        String item = "INSERT INTO `skills_item` VALUES"
                + " (152, '地面障礙', NULL, NULL);";
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains(control));
            assertTrue(sql.contains(boss));
            assertTrue(sql.contains(item));
        }
    }

    @Test void retainedExecutorPinsImmunityNeutralizationIconAndStop()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("this._skillId == ENTANGLE"));
        assertTrue(use.contains("pc.getHasteItemEquipped() > 0"));
        assertTrue(use.contains("cha.getBraveSpeed() == 5"));
        assertTrue(use.contains("new S_SkillHaste(pc.getId(), 2,"));
        assertTrue(use.contains("cha.removeSkillEffect(this._skillId);"));
        assertTrue(use.contains("cha.setMoveSpeed(0);"));

        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/SkillBuffIconHelper.java"));
        assertTrue(icons.contains("case ENTANGLE:"));
        assertTrue(icons.contains("new S_SkillHaste(pc.getId(), 2, timeSec)"));
        assertTrue(icons.contains("new S_SkillHaste(pc.getId(), 2, 0)"));

        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case ENTANGLE:"));
        assertTrue(stop.contains("new S_SkillHaste(pc.getId(), 0, 0)"));
        assertTrue(stop.contains("sendStopMessage(pc, skillId);"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("ENTANGLE"));
    }

    @Test void selectedLiToPinsCase151Range10ProducerAndZeroCastClock()
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
                0x635774L + (long) (BattleSpellCatalog.ENTANGLE - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesSpeedLeaseAndStop711()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.entangleSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(session.contains("target.inventory.hasEquippedHasteArmor()"));
        assertTrue(session.contains("== BattleTimedStatus.BRAVE_SECOND"));
        assertTrue(session.contains("hub.slowClassicWorldMonster("));
        assertTrue(session.contains("sendSlowStopMessage("));
        assertTrue(session.contains("Protocol.messageCode(spell.stopMessage())"));
        String population = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"));
        assertTrue(population.contains("long slowExpiresAtNanos"));
        assertTrue(population.contains("BattleActionInterval.npcSourceMillis("));
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
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
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
