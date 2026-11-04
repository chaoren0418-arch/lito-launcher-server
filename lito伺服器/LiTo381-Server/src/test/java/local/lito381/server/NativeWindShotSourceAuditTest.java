package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill149 Wind Shot. */
final class NativeWindShotSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinExactWindShotRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (149, 0,"
                + " '風之神射', 19, 4, 15, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 16, '', 19, 2246,"
                + " 0, 0, 724, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (149,"
                + " '風之神射', 19, 4, 15, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 16, '', 19, 2246,"
                + " 0, 0, 724, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (149, '風之神射', 'BOW_HIT', 6);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (149, '風之神射', NULL, NULL);"));
        }
    }

    @Test void retainedExecutorPinsAttributeAbilityCoexistenceAndPersistence()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(magicattr != 0)"));
        assertTrue(use.contains("this._skillId == WIND_SHOT"));
        assertTrue(use.contains("pc.addBowHitup(6)"));
        assertTrue(use.contains("{ FIRE_WEAPON, WIND_SHOT, FIRE_BLESS,"
                + " STORM_EYE, BURNING_WEAPON, STORM_SHOT }"));
        assertTrue(use.contains("if (!SkillCoexistEventTable.get().isCoexistEnabled())"));

        String ability = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcBuffAbilityCalc.java"));
        assertTrue(ability.contains(
                "AbilityType.BOW_HIT, v -> pc.addBowHitup(v * multiplier)"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case WIND_SHOT"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("WIND_SHOT, // 風之神射"));
    }

    @Test void selectedLiToPinsCase148ObjectProducer() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("loc_4CB71D:"));
        assertTrue(disassembly.contains("cases 7,8,22,36,42,43,47,54,66,78,147,148,"
                + "150,157,159,162,164,165,167,169"));
        assertTrue(disassembly.contains("mov     dl, bl"));
        assertTrue(disassembly.contains("call    sub_4C9880"));
        assertTrue(disassembly.contains("push    offset aCccd"));
        assertTrue(disassembly.contains("push    14h"));
    }

    @Test void everyReachablePlayerPhysicalHitConsumerUsesTheLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(1, session.split(
                "windShotStatus\\.physicalHit", -1).length - 1);
        assertEquals(3, session.split(
                "NativeWeaponBuffModifiers nativeWeaponBuffs =", -1)
                .length - 1);
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
