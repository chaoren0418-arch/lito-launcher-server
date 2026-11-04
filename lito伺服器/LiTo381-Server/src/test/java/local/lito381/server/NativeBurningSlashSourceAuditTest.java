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

/** Cross-source proof for native Dragon Knight skill182 Burning Slash. */
final class NativeBurningSlashSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
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

    @Test void databasesPinExactDragonKnightSelfBuffAndCancellationPolicy()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (182, 0, '燃燒擊砍', 23, 5,"
                + " 0, 10, 0, 0, 2000, 60, 'none', 0, 0, 0, 0, 0, 0, 2,"
                + " 2, 0, 0, 0, 0, 32, '$5921', 19, 6590, 0, 0, 0, 0);",
                skillRow(primary, 182));
        assertEquals("INSERT INTO `skills` VALUES (182, '燃燒擊砍', 23, 5,"
                + " 0, 10, 0, 0, 0, 60, 'none', 0, 0, 0, 0, 0, 0, 2,"
                + " 2, 0, 0, 0, 0, 32, '$5921', 19, 6590, 0, 0, 0, 0);",
                skillRow(secondary, 182));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (182, '燃燒擊砍', NULL, NULL);"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (182,"));
    }

    @Test void selectedLiToEmitsExactSelectorAndPinsThirtySixHundredClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(3_600, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.BURNING_SLASH - 1) * 2L));
    }

    @Test void fourRetainedCombatSourcesAgreeOnEligibilityBonusImpactAndConsume()
            throws Exception {
        String[] sources = {
                "其他src參考用/korsrc/com/lineage/server/model/L1AttackPc.java",
                "其他src參考用/貓大src/com/lineage/server/model/L1AttackPc.java",
                "其他src參考用/加錢src/com/lineage/server/model/L1AttackPc.java",
                "其他src參考用/rk760src/com/lineage/server/model/L1AttackPc.java"
        };
        for (String relative : sources) {
            String attack = read(RETAINED.resolve(relative));
            assertTrue(attack.contains("hasSkillEffect(BURNING_SLASH)")
                    || attack.contains("hasSkillEffect(182)"), relative);
            assertTrue(attack.contains("dmg += 10"), relative);
            assertTrue(attack.contains("6591"), relative);
            assertTrue(attack.contains("killSkillEffectTimer(BURNING_SLASH)")
                    || attack.contains("killSkillEffectTimer(182)"), relative);
            int method = attack.indexOf("double calcBuffDamage(");
            int bow = attack.indexOf("_weaponType == 20", method);
            int gauntlet = attack.indexOf("_weaponType == 62", method);
            int kiringku = attack.indexOf("_weaponType2 == 17", method);
            int proc = Math.max(attack.indexOf("hasSkillEffect(BURNING_SLASH)", method),
                    attack.indexOf("hasSkillEffect(182)", method));
            assertTrue(method >= 0 && bow > method && gauntlet > bow
                    && kiringku > gauntlet && proc > kiringku, relative);
        }
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String buffArray = buffs.substring(buffs.indexOf("_buffSkill = {"),
                buffs.indexOf("};", buffs.indexOf("_buffSkill = {")));
        assertFalse(buffArray.contains("BURNING_SLASH"));
    }

    @Test void implementationRoutesBothPhasesAndAllOrdinaryPhysicalTargets()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.burningSlashSpell(selector.skillId())"));
        assertTrue(session.contains("burningSlashStatus.activate("));
        assertTrue(session.contains("burningSlashStatus.consume("));
        assertTrue(session.contains("burningSlashStatus.cancel()"));
        assertTrue(session.contains("burningSlashStatus.expire("));
        assertFalse(session.contains("saveTimedBurningSlashBuff"));
        assertEquals(3, occurrences(session, "boolean burningSlashConsumed ="
                + " consumeBurningSlash("));
        assertTrue(session.contains("physicalHit && !ranged && !kiringkuAttack"));
        assertEquals(2, occurrences(session,
                "hit && !ranged && !kiringkuAttack"));
        assertTrue(session.contains("Protocol.effectLocation(\n"
                + "                targetX, targetY, spell.impactGfx())"));
        String physical = read(Path.of(
                "src/main/java/local/lito381/server/BattlePhysicalAttack.java"));
        assertTrue(physical.contains("case 5 -> level / 25; // dragon knight"));
        assertTrue(physical.contains("case 6 -> level / 40; // illusionist"));
        String dragonClass = read(RETAINED.resolve(
                "src/com/lineage/server/model/classes/"
                        + "L1DragonKnightClassFeature.java"));
        String illusionClass = read(RETAINED.resolve(
                "src/com/lineage/server/model/classes/"
                        + "L1IllusionistClassFeature.java"));
        assertTrue(dragonClass.contains("return playerLevel / 25;"));
        assertTrue(illusionClass.contains("return playerLevel / 40;"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
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
        assertTrue(Files.isRegularFile(path), "missing source: " + path);
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
