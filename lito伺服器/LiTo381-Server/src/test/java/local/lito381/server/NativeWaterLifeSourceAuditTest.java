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

/** Cross-source proof for native Elf skill170 Water Life. */
final class NativeWaterLifeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/skill/Skill_SpiritCrystal_Water.java"),
                Path.of("../1-伺服器/src/com/lineage/list/PcLvSkillList.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
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
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path ASM = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void databasesPinExactPcTargetMaterialTimingAndNoAuxiliaryRows()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (170, 0,"
                + " '水之元氣', 22, 1, 1, 0, 40319, 1, 100, 64, 'buff',"
                + " 1, 0, 0, 0, 0, 0, 4, 2, 0, -1, 0, 0, 2, '', 19,"
                + " 4401, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (170,"
                + " '水之元氣', 22, 1, 1, 0, 40319, 1, 0, 64, 'buff',"
                + " 1, 0, 0, 0, 0, 0, 4, 2, 0, -1, 0, 0, 2, '', 19,"
                + " 4401, 0, 0, 0, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (170, '水之元氣', NULL, NULL);"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_buff_ability` VALUES (170,"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_負面技能控制` VALUES (170,"));
        }
    }

    @Test void retainedEnginePinsWaterAcquisitionOneShotHealingAndTransientStop()
            throws Exception {
        String list = read(RETAINED.resolve("src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 4) != 0)"));
        assertTrue(list.contains("skillList.add(WATER_LIFE - 1)"));
        String crystal = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/skill/"
                        + "Skill_SpiritCrystal_Water.java"));
        assertTrue(crystal.contains("nameId.equalsIgnoreCase(\"$3266\")"));
        assertTrue(crystal.contains("skillid = WATER_LIFE;"));
        assertTrue(crystal.contains("magicLv = 13;"));

        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 170:// 水之元氣"));
        assertTrue(request.contains("targetId = readD();"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("166, 168, 169, 170, 171"));
        assertOrdered(use,
                "if (cha.hasSkillEffect(WATER_LIFE))",
                "this._dmg = (this._dmg << 1);",
                "if (cha.hasSkillEffect(POLLUTE_WATER))",
                "cha.removeSkillEffect(WATER_LIFE);");
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case WATER_LIFE: // 水之元氣"));
        assertTrue(stop.contains("new S_PacketBoxWaterLife()"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("WATER_LIFE"),
                "skill170 is a transient lease, not a reconnect buff");
        assertEquals(0xFA, Protocol.S_LITO_CLAN,
                "selected opcode FA cannot carry retained PacketBox 3B");
    }

    @Test void selectedLiToPinsCase169ObjectProducerAndFourHundredMsClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String selector = windowAfter(ASM, "loc_4CB71D:", 20);
        assertTrue(selector.contains("cases 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        assertTrue(selector.contains("call    sub_4C9880"));
        String producer = windowAfter(ASM, "loc_4C968A:", 24);
        assertTrue(producer.contains("push    edi"));
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     eax, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));
        assertEquals(400, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.WATER_LIFE - 1) * 2L));
    }

    @Test void implementationHasTwoPhaseObjectRouteMaterialAndTransientLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.waterLifeSpell(selector.skillId())"));
        assertTrue(session.contains("private void useWaterLifeSpell("));
        String method = session.substring(
                session.indexOf("private void useWaterLifeSpell("),
                session.indexOf("private void useAquaProtectSpell("));
        assertTrue(method.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("target.waterLifeStatus.activate("));
        assertTrue(method.contains("nextInventory.removeCatalog("));
        assertTrue(method.contains("S_MESSAGE_CODE Water-Life-missing-reagent=299"));
        assertTrue(method.contains("state.saveEconomyState("));
        assertTrue(method.contains("persisted=false"));
        assertEquals(0, occurrences(method, "saveTimed"));
        assertEquals(0, occurrences(method, "consumeCounterMagic"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int found = text.indexOf(fragment, previous + 1);
            assertTrue(found > previous,
                    "missing/out-of-order fragment: " + fragment);
            previous = found;
        }
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
