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

/** Cross-source proof for native Elf skill175 Soul of Flame. */
final class NativeSoulOfFlameSourceAuditTest {
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

    @Test void selectedClientPinsCase174SelectorOnlyAndFiveSecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String branch = windowAfter(ASM, "loc_4CBACB:", 18);
        assertTrue(branch.contains("jumptable 004CB66E cases 174,175"));
        assertOrdered(branch, "mov     edx, esi", "and     edx, 7",
                "push    edx", "jmp     loc_4CBBDA");
        String producer = windowAfter(ASM, "loc_4CBBDA:", 18);
        assertOrdered(producer, "sar     esi, 3", "push    esi", "push    14h",
                "push    offset off_5028CC", "call    sub_4C6AE0");
        assertEquals(5_000, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.SOUL_OF_FLAME - 1) * 2L));
    }

    @Test void databasesPinExactSelfBuffMaterialDurationAndSourceReuse()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (175, 0, '烈焰之魂',"
                + " 22, 6, 30, 0, 40319, 3, 300, 64, 'none', 0, 0, 0, 0,"
                + " 0, 0, 2, 2, 0, 0, 0, 0, 64, '', 19, 5833, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (175, '烈焰之魂',"
                + " 22, 6, 30, 0, 40319, 3, 0, 64, 'none', 0, 0, 0, 0,"
                + " 0, 0, 2, 2, 0, 0, 0, 0, 64, '', 19, 5833, 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (175, '烈焰之魂', NULL, NULL);"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_buff_ability` VALUES (175,"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_負面技能控制` VALUES (175,"));
        }
    }

    @Test void retainedEnginePinsAcquisitionMaximumMultiplierDurabilityAndTransientIcon()
            throws Exception {
        String list = read(RETAINED.resolve("src/com/lineage/list/PcLvSkillList.java"));
        assertOrdered(list, "if ((elfAttr & 2) != 0)",
                "skillList.add(SOUL_OF_FLAME - 1)");
        String crystal = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/skill/Skill_SpiritCrystal_Fire.java"));
        assertOrdered(crystal, "nameId.equalsIgnoreCase(\"$4714\")",
                "skillid = SOUL_OF_FLAME;", "magicLv = 15;");
        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 175:// 烈焰之魂"));
        assertTrue(request.contains("targetId = readD();"));

        String attack = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java"));
        assertOrdered(attack, "if (_pc.hasSkillEffect(SOUL_OF_FLAME)"
                        + " && weaponMaxDamage > 0)",
                "weaponDamage = weaponMaxDamage;");
        assertOrdered(attack, "private double calcBuffDamage(double dmg)",
                "if (_pc.hasSkillEffect(SOUL_OF_FLAME))",
                "Math.max(_damageMultiplier, ConfigSkill.SOUL_OF_FLAME_DAMAGE)");
        String config = read(RETAINED.resolve(
                "src/com/lineage/config/ConfigSkill.java"));
        assertTrue(config.contains("SOUL_OF_FLAME_DAMAGE = _getDouble(p,"
                + " \"SOUL_OF_FLAME_DAMAGE\", 1.5);"));
        String commit = read(RETAINED.resolve(
                "src/com/lineage/server/model/combat/PhysicalHitCommitPlan.java"));
        assertTrue(commit.contains("attacker.hasSkillEffect(SOUL_OF_FLAME)"));
        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Icons.java"));
        assertTrue(icons.contains("data[31] = (byte)"
                + " (pc.getSkillEffectTimeSec(SOUL_OF_FLAME) / 4);"));
        assertFalse(read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"))
                .contains("SOUL_OF_FLAME"));
    }

    @Test void implementationPinsTwoPhaseLeaseAndAllPlayerPhysicalPipelines()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("SOUL_OF_FLAME, new PhysicalProcBuffSpell("));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleTimedStatus.java"));
        assertTrue(status.contains("activateSoulOfFlame"));
        assertTrue(status.contains("soulOfFlameExpiresAtNanos"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("worldPhysicalProcBuff"));
        assertTrue(session.contains("timedStatus.activateSoulOfFlame("));
        assertTrue(session.contains("sourceSkillReuseDelays.getOrDefault("));
        assertTrue(session.contains("sourceReuseDelayMillis * 1_000_000L"));
        assertTrue(occurrences(session, "BattleSoulOfFlame.meleeWeaponRoll(") >= 3);
        assertTrue(occurrences(session, "BattleSoulOfFlame.rangedWeaponBase(") >= 3);
        assertTrue(session.contains("BattlePhysicalProcBuff.soulOfFlameMultiplier("));
        assertTrue(session.contains("reason=Soul-of-Flame"));
        assertTrue(session.contains("expireSoulOfFlame"));
        assertTrue(session.contains("persisted=false"));
        assertEquals(0, occurrences(session, "saveSoulOfFlame"));
        String accounts = read(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"));
        assertTrue(accounts.contains("SELECT skill_id,reuseDelay FROM "));
        assertTrue(accounts.contains("Map<Integer, Integer> skillReuseDelays()"));
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
        byte[] bytes = Files.readAllBytes(path);
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
