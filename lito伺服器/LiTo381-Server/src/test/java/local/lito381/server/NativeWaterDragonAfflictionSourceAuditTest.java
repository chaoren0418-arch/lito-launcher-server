package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Source and selected-client proof for server-produced Water Dragon afflictions. */
final class NativeWaterDragonAfflictionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/hp/UserAddHp.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/quest/AD80_2_Timer.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");

    @Test
    void selectedDatabasePinsBaseRowsAliasesAndMonsterProducers() throws Exception {
        String primary = read(PRIMARY_SQL);
        String commonTail = ", 0, 0, 0, 0, 0, 0, 0, 12000, 'buff', 3,"
                + " 0, 0, 0, 0, 0, 0, 128, 0, 0, 0, 0, 0, '', 0, ";
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (4011, 0,"
                + " '藥水侵蝕術'" + commonTail + "7781, 0, 0, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (4012, 0,"
                + " '污濁的水流'" + commonTail + "7782, 0, 0, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (4013, 0,"
                + " '治癒侵蝕術'" + commonTail + "7780, 0, 0, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (49011, 4011,"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (49012, 4012,"));
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (49013, 4013,"));
        assertTrue(primary.contains("[BOSS]不死的木乃伊王>技能4013"));
        assertTrue(primary.contains("[BOSS]邪惡的鐮刀死神>技能4011"));
        assertTrue(primary.contains("[BOSS]邪惡的鐮刀死神>技能4013"));

        // The older secondary dump predates these rows; selected 380_db owns
        // this encounter contract instead of silently fabricating fallbacks.
        String secondary = read(SECONDARY_SQL);
        assertFalse(secondary.contains("VALUES (4011, '藥水侵蝕術'"));
        assertFalse(secondary.contains("VALUES (4012, '污濁的水流'"));
        assertFalse(secondary.contains("VALUES (4013, '治癒侵蝕術'"));
    }

    @Test
    void retainedTimerAndRecoveryPathsPinMutualExclusionAndModifierOrder()
            throws Exception {
        String timer = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/quest/AD80_2_Timer.java"));
        assertTrue(timer.contains("final int timeMillis = 5000;"));
        assertOrdered(timer, "case 0:", "ADLV80_2_1", "gfxid = 7781",
                "case 1:", "ADLV80_2_2", "gfxid = 7782",
                "case 2:", "ADLV80_2_3", "gfxid = 7780");
        assertTrue(timer.contains("tgpc.setSkillEffect(skillid, 12 * 1000);"));
        assertOrdered(timer, "tgpc.hasSkillEffect(L1SkillId.ADLV80_2_1)",
                "tgpc.hasSkillEffect(L1SkillId.ADLV80_2_2)",
                "tgpc.hasSkillEffect(L1SkillId.ADLV80_2_3)",
                "tgpc.getX() > 32942", "tgpc.getX() < 32980",
                "tgpc.getY() > 32810", "tgpc.getY() < 32871");

        String potion = read(RETAINED.resolve(
                "src/com/lineage/data/item_etcitem/hp/UserAddHp.java"));
        assertOrdered(potion, "if (pc.hasSkillEffect(POLLUTE_WATER))",
                "if (pc.hasSkillEffect(ADLV80_2_2))",
                "if (pc.hasSkillEffect(ADLV80_2_1))",
                "pc.adjustCurrentHp(addhp);");
        String healing = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertOrdered(healing, "if (cha.hasSkillEffect(POLLUTE_WATER))",
                "if (cha.hasSkillEffect(ADLV80_2_2))",
                "if (cha.hasSkillEffect(ADLV80_2_3))");
        assertTrue(healing.contains("this._dmg *= -1;"));
    }

    @Test
    void selectedClientAndImportedSpriteContractContainEveryEffect() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String sprite = LocalResources.readString(Path.of(
                "src/main/resources/battle/spr-action-380.csv"),
                StandardCharsets.UTF_8);
        assertTrue(sprite.contains("7780,0,"));
        assertTrue(sprite.contains("7781,0,"));
        assertTrue(sprite.contains("7782,0,"));
        byte[] effect = Protocol.effectObject(0x3810_4011L, 7781);
        assertEquals(Protocol.S_EFFECT, effect[0] & 0xFF);
        assertEquals(7781, (effect[5] & 0xFF) | (effect[6] & 0xFF) << 8);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int found = text.indexOf(fragment, previous + 1);
            assertTrue(found > previous, "missing/out-of-order fragment: " + fragment);
            previous = found;
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = input.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
