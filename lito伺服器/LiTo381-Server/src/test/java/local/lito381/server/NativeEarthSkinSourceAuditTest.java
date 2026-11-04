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

/** Cross-source proof for native Elf skill151 Earth Skin. */
final class NativeEarthSkinSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/IndependentSkillIcon.java"),
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

    @Test void bothDatabasesPinExactEarthSkinRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (151, 0,"
                + " '大地防護', 19, 6, 15, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 64, '', 19, 2249,"
                + " 0, 0, 725, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (151,"
                + " '大地防護', 19, 6, 15, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 64, '', 19, 2249,"
                + " 0, 0, 725, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (151, '大地防護', 'AC', -6);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (151, '大地防護', NULL, NULL);"));
        }
    }

    @Test void retainedExecutorPinsReplacementAcIconStopAndPersistence()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains(
                "{ SHIELD, SHADOW_ARMOR, EARTH_SKIN, EARTH_BLESS, IRON_SKIN }"));
        assertTrue(use.contains("this._skillId == EARTH_SKIN"));
        assertTrue(use.contains("pc.addAc(-6)"));
        assertTrue(use.contains("IndependentSkillIcon.sendStart(pc, this._skillId"));

        String icons = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/IndependentSkillIcon.java"));
        assertTrue(icons.contains("case EARTH_SKIN:"));
        assertTrue(icons.contains("new S_SkillIconShield(6, timeSec)"));

        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case EARTH_SKIN:"));
        assertTrue(stop.contains("cha.addAc(6)"));
        assertTrue(stop.contains("pc.updateBuffStatus()"));
        assertTrue(stop.contains("IndependentSkillIcon.sendStop(pc, skillId)"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("EARTH_SKIN, // 大地防護"));
    }

    @Test void selectedLiToPinsCase150ObjectOnlyProducer() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY).replace("\r\n", "\n");
        assertTrue(disassembly.contains("loc_4CB71D:"));
        assertTrue(disassembly.contains("cases 7,8,22,36,42,43,47,54,66,78,"
                + "147,148,150,157,159,162,164,165,167,169"));
        assertTrue(disassembly.contains("db    1Ch,     1,     1,     4\n"
                + "db      4,     1,     4,     2"));
        assertTrue(disassembly.contains("mov     ecx, esi\n"
                + "push    eax\n"
                + "and     ecx, 7\n"
                + "sar     esi, 3\n"
                + "push    ecx\n"
                + "push    esi\n"
                + "push    14h\n"
                + "push    offset aCccd"));
        assertTrue(disassembly.contains("aCccd db 'cccd',0"));
    }

    @Test void implementationRoutesBothPhasesAndCorrectsBarrierWhitelist()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains(
                "BattleSpellCatalog.shieldSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(session.contains("new GameStateStore.TimedArmorProtection("));
        assertTrue(session.contains("classicWorldMembers(originMapId)"));

        String barrier = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbsoluteBarrierRules.java"));
        assertTrue(barrier.contains("BattleSpellCatalog.EARTH_SKIN"));
        assertTrue(!barrier.contains("149, // EARTH_SKIN"));
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
