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

/** Cross-source proof for native Elf skill147 Elemental Protection. */
final class NativeElementalProtectionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_NPCAction.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
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

    @Test void bothDatabasesPinExactElementalProtectionRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (147, 0,"
                + " '單屬性防禦', 19, 2, 6, 0, 40319, 1, 0, 64, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 4, '', 19, 2285, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (147,"
                + " '單屬性防禦', 19, 2, 6, 0, 40319, 1, 0, 64, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 4, '', 19, 2285, 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (147, '單屬性防禦', NULL, NULL);"));
        }
    }

    @Test void retainedExecutorPinsPreconditionMaskAndFiftyPointLifecycle()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(this._skillId == ELEMENTAL_PROTECTION)"
                + " && (pc.getElfAttr() == 0)"));
        assertTrue(use.contains("this._skillId == ELEMENTAL_PROTECTION"));
        assertTrue(use.contains("final int attr = pc.getElfAttr()"));
        assertTrue(use.contains("pc.addEarth(50)"));
        assertTrue(use.contains("pc.addFire(50)"));
        assertTrue(use.contains("pc.addWater(50)"));
        assertTrue(use.contains("pc.addWind(50)"));

        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case ELEMENTAL_PROTECTION"));
        assertTrue(stop.contains("cha.addEarth(-50)"));
        assertTrue(stop.contains("cha.addFire(-50)"));
        assertTrue(stop.contains("cha.addWater(-50)"));
        assertTrue(stop.contains("cha.addWind(-50)"));
        assertTrue(stop.contains("pc.sendPackets(new S_OwnCharAttrDef(pc))"));

        String npcAction = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_NPCAction.java"));
        assertTrue(npcAction.contains("pc.hasSkillEffect(ELEMENTAL_PROTECTION)"));
        assertTrue(npcAction.contains("pc.removeSkillEffect(ELEMENTAL_PROTECTION)"));
    }

    @Test void selectedLiToPinsCase146SelectorProducer() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,"
                + "96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,"
                + "133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(disassembly.contains("loc_4CBBD4:"));
        assertTrue(disassembly.contains("push    14h"));
        assertTrue(disassembly.contains("push    offset off_5028CC"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
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
