package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for LiTo.bin case133 and retained skill134 behavior. */
final class NativeCounterMirrorSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path SELECTED_DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothResourceDumpsPinExactSelfWardMaterialLeaseAndNoItemMode()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (134, 0, '鏡反射',"
                + " 17, 5, 10, 0, 40319, 1, 1000, 16, 'none', 0, 0, 0, 0,"
                + " 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 4395, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (134, '鏡反射',"
                + " 17, 5, 10, 0, 40319, 1, 0, 16, 'none', 0, 0, 0, 0,"
                + " 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 4395, 0, 0, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (134, '鏡反射', NULL, NULL);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (134, '鏡反射', NULL, NULL);"));
    }

    @Test void retainedPcAndNpcConsumersPinFullDamageZeroBasedWisdomAndLeaseUse()
            throws Exception {
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(pc.contains("if (_targetPc.hasSkillEffect(COUNTER_MIRROR))"));
        assertTrue(pc.contains("if (_calcType == PC_PC)"));
        assertTrue(pc.contains("_targetPc.getWis() >= _random.nextInt(100)"));
        assertTrue(pc.contains("_pc.receiveDamage(_targetPc, dmg, false, false);"));
        assertTrue(pc.contains("dmg = 0;"));
        assertTrue(pc.contains("_targetPc.killSkillEffectTimer(COUNTER_MIRROR);"));

        String npc = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicNpc.java"));
        assertTrue(npc.contains("if (!this._npc.getNpcTemplate().get_IsErase())"));
        assertTrue(npc.contains("this._targetPc.getWis() >= _random.nextInt(100)"));
        assertTrue(npc.contains("this._npc.receiveDamage(this._targetPc, dmg);"));
        for (int npcId : new int[] {45681, 45682, 45683, 45684,
                91161, 91159, 91160, 91162, 71014, 71015, 71016,
                71026, 71027, 71028}) {
            assertTrue(npc.contains("case " + npcId + ":"),
                    "missing retained Counter Mirror denylist NPC " + npcId);
        }

        String useSkill = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(useSkill.contains("(_skillId == COUNTER_MIRROR)"));
        assertTrue(useSkill.contains(
                "_player.sendPackets(new S_SkillSound(targetid, castgfx));"));
    }

    @Test void selectedLiToPinsCase133GenericSelectorOnlyProducer()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String producer = windowAfter(SELECTED_DISASSEMBLY,
                "loc_4CBBD4:", 20);
        assertTrue(producer.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,"
                + "96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,"
                + "133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset off_5028CC"));
        assertTrue(producer.contains("call    sub_4C6AE0"));
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
