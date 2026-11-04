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

/** Cross-source proof for native Elf skill171 Elemental Fire. */
final class NativeElementalFireSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/config/ConfigSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/skill/Skill_SpiritCrystal_Fire.java"),
                Path.of("../1-伺服器/src/com/lineage/list/PcLvSkillList.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackPc.java"),
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

    @Test void databasesPinExactFireSelfProcMaterialAndTiming() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (171, 0,"
                + " '屬性之火', 22, 2, 20, 0, 40319, 1, 100, 192, 'none',"
                + " 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 4, '', 19,"
                + " 4402, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (171,"
                + " '屬性之火', 22, 2, 20, 0, 40319, 1, 0, 192, 'none',"
                + " 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 4, '', 19,"
                + " 4402, 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (171, '屬性之火', NULL, NULL);"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_buff_ability` VALUES (171,"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_負面技能控制` VALUES (171,"));
        }
    }

    @Test void retainedEnginePinsFireAcquisitionSharedMeleeProcAndTransientLease()
            throws Exception {
        String list = read(RETAINED.resolve("src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 2) != 0)"));
        assertTrue(list.contains("skillList.add(ELEMENTAL_FIRE - 1)"));
        String crystal = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/skill/"
                        + "Skill_SpiritCrystal_Fire.java"));
        assertTrue(crystal.contains("nameId.equalsIgnoreCase(\"$3267\")"));
        assertTrue(crystal.contains("skillid = ELEMENTAL_FIRE;"));
        assertTrue(crystal.contains("magicLv = 15;"));

        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 171:// 屬性之火"));
        String attack = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java"));
        assertOrdered(attack,
                "final int random = ThreadLocalRandom.current().nextInt(100) + 1;",
                "if (_pc.hasSkillEffect(ELEMENTAL_FIRE))",
                "random <= ConfigSkill.ELEMENTAL_FIRE_RND",
                "Math.max(_damageMultiplier, ConfigSkill.ELEMENTAL_FIRE)",
                "if (_pc.hasSkillEffect(BURNING_SPIRIT))");
        assertTrue(attack.contains("if (_weaponType == 20)"));
        assertTrue(attack.contains("if (_weaponType == 62)"));
        assertTrue(attack.contains("if (_weaponType2 == 17)"));
        String config = read(RETAINED.resolve(
                "src/com/lineage/config/ConfigSkill.java"));
        assertTrue(config.contains("ELEMENTAL_FIRE = 1.5;"));
        assertTrue(config.contains("ELEMENTAL_FIRE_RND = 33;"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("166, 168, 169, 170, 171"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("ELEMENTAL_FIRE"),
                "skill171 is a transient lease, not a reconnect buff");
    }

    @Test void selectedLiToPinsCase170SelectorOnlyAndThreeHundredMsClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String selector = windowAfter(ASM, "loc_4CBBD4:", 22);
        assertTrue(selector.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(selector.contains("push    offset off_5028CC"));
        assertEquals(300, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.ELEMENTAL_FIRE - 1) * 2L));
    }

    @Test void implementationHasTwoPhaseSelectorMaterialSharedProcAndNoSidecar()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("ELEMENTAL_FIRE, new PhysicalProcBuffSpell("));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("worldPhysicalProcBuff"));
        String method = session.substring(
                session.indexOf("private void usePhysicalProcBuffSpell("),
                session.indexOf("private void useDodgeBuffSpell("));
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("timedStatus.activateElementalFire("));
        assertTrue(method.contains("nextInventory.removeCatalog("));
        assertTrue(method.contains("physical-proc-buff-missing-reagent=299"));
        assertTrue(method.contains("state.saveEconomyState("));
        assertTrue(method.contains("persisted=false"));
        assertEquals(0, occurrences(method, "saveTimed"));
        assertEquals(0, occurrences(method, "consumeCounterMagic"));
        assertTrue(session.contains("BattlePhysicalProcBuff.elementalFire("));
        assertTrue(session.contains("burningSpirit.multiplier()"));
        assertTrue(session.contains("elementalFire.multiplier()"));
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
