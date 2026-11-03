package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector18/1 and skill146 Blood to Soul. */
final class NativeBloodySoulSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesAndConfigsPinHpFortyMpTwentyContract()
            throws Exception {
        String primary = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(SECONDARY.resolve("db/380yiwei_db.sql"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (146, 0,"
                + " '魂體轉換', 19, 1, 0, 40, 0, 0, 1200, 0, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, '', 19, 2178,"
                + " 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (146,"
                + " '魂體轉換', 19, 1, 0, 40, 0, 0, 1200, 0, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, '', 19, 2178,"
                + " 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (146, '魂體轉換', NULL, NULL);"));
        }
        assertTrue(read(RETAINED.resolve(
                "config/其他控制端/4-職業_技能設定表.properties"))
                .contains("BLOODY_SOULADDMP = 20"));
        assertTrue(read(SECONDARY.resolve(
                "config/其他控制端/4-職業_技能設定表.properties"))
                .contains("BLOODY_SOULADDMP = 20"));
    }

    @Test void retainedModePinsInstantClampedManaAndOneHpFloor()
            throws Exception {
        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 146:// 魂體轉換 2500"));
        String skillIds = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"));
        assertTrue(skillIds.contains("BLOODY_SOUL = 146"));
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/BLOODY_SOUL.java"));
        assertTrue(mode.contains("adjustCurrentMp(ConfigSkill.BLOODY_SOULADDMP)"));
        String secondaryMode = read(SECONDARY.resolve(
                "src/com/lineage/server/model/skill/skillmode/BLOODY_SOUL.java"));
        assertTrue(secondaryMode.contains(
                "setCurrentMp(srcpc.getCurrentMp() + ConfigSkill.BLOODY_SOULADDMP)"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("currentHp < this._hpConsume + 1"));
        assertTrue(use.contains(
                "this._player.adjustCurrentHp(-((long) this._hpConsume))"));
        assertTrue(read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillMode.java"))
                .contains("_skillMode.put(BLOODY_SOUL, new BLOODY_SOUL())"));
        assertFalse(read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"))
                .contains("BLOODY_SOUL"));
        assertTrue(read(RETAINED.resolve(
                "其他src參考用/L1JGO-Whale-master/吃檔/TW13081901.txt"))
                .contains("#2178 1   blood to soul e"));
    }

    @Test void selectedLiToPinsCase145SelectorOnlyProducer() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,"
                + "96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,"
                + "133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(disassembly.contains("loc_4CBBD4:"));
        assertTrue(disassembly.contains("and     ecx, 7"));
        assertTrue(disassembly.contains("sar     esi, 3"));
        assertTrue(disassembly.contains("push    14h"));
        assertTrue(disassembly.contains("push    offset off_5028CC"));
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
