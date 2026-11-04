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

/** Cross-source proof for selected Illusionist skill218 Joy of Pain. */
final class NativeJoyOfPainSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path PC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path FEEDBACK = RETAINED.resolve(
            "src/com/lineage/server/model/combat/LegacyDefenseFeedbackPlan.java");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path CONFIG = RETAINED.resolve(
            "src/com/lineage/config/ConfigSkill.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test void hashesRowsBookAndNoProbabilityOrMaterialAreLocked()
            throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PC));
        assertEquals("749337CF2A0C4ECF68A6FFA4B798D580B9C01B776DA5010F6EF97B20FC8D3709",
                sha256(FEEDBACK));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("FFC81F59CDD57D18371538B92AB0D6B591009FBB37355BE86737ADBC414E7D7E",
                sha256(CONFIG));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (218, 0,"
                + " '疼痛的歡愉', 28, 1, 40, 0, 0, 0, 3000, 2, 'buff', 3"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (218,"
                + " '疼痛的歡愉', 28, 1, 40, 0, 0, 0, 0, 2, 'buff', 3"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (218, '疼痛的歡愉', NULL, NULL);"));
            assertTrue(sql.contains("49134, '記憶水晶(疼痛的歡愉)',"
                    + " 'skill.Skill_Illusionist', '$5698'"));
            assertFalse(sql.contains("`skills_負面技能控制` VALUES"
                    + " (218, '疼痛的歡愉'"));
            assertFalse(sql.contains("`skills_負面boss免疫` VALUES"
                    + " (218, '疼痛的歡愉'"));
        }
    }

    @Test void clientSelectorClockAndRetainedOneShotOrderingAreExact()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635926L));
        assertEquals(BattleSpellCatalog.JOY_OF_PAIN,
                Protocol.selectorOnlySpellRequest(
                        new byte[] {0x14, 0x1B, 0x01}).skillId());

        String pc = read(PC);
        assertTrue(pc.contains("attacker.captureSkillEffectLease(JOY_OF_PAIN)"));
        assertTrue(pc.contains("final int hpup = this.get_other().get_addhp();"));
        assertTrue(pc.contains("joyDamage = (getMaxHp() - getCurrentHp() - hpup)"));
        assertTrue(pc.contains("/ ConfigSkill.JOY_OF_PAIN_COUNTDMG;"));
        assertTrue(pc.contains("Math.min(joyDamage, ConfigSkill.JOY_OF_PAIN_DMG)"));

        String feedback = read(FEEDBACK);
        assertTrue(feedback.indexOf("mortalBodyDamage > 0")
                < feedback.indexOf("joyOfPainLease != null"));
        assertTrue(feedback.contains(
                "attacker.killSkillEffectTimer(JOY_OF_PAIN, joyOfPainLease)"));
        assertTrue(feedback.contains("receiveDamage(defender, damage, false, true)"));
        assertTrue(feedback.contains(
                "HitReactionSuppressTable.get().shouldSendDamageAction(attackerNpc)"));

        String config = read(CONFIG);
        assertTrue(config.contains("\"JOY_OF_PAIN_COUNTDMG\", 3"));
        assertTrue(config.contains("\"JOY_OF_PAIN_DMG\", 1500"));
    }

    @Test void targetAdmissionAndImplementationCoverEveryActorFamily()
            throws Exception {
        String use = read(USE);
        assertTrue(use.contains(
                "cha.hasSkillEffect(EARTH_BIND) && (_skillId == EARTH_BIND)"));
        String absolute = use.substring(use.indexOf("if (pc.hasSkillEffect(ABSOLUTE_BARRIER))"),
                use.indexOf("default:",
                        use.indexOf("if (pc.hasSkillEffect(ABSOLUTE_BARRIER))")));
        assertFalse(absolute.contains("JOY_OF_PAIN"));
        String counterSwitch = use.substring(use.indexOf("switch (_skill.getType())"),
                use.indexOf("if ((_skill.getType() == L1Skills.TYPE_OTHER)",
                        use.indexOf("switch (_skill.getType())")));
        assertFalse(counterSwitch.contains("TYPE_OTHER"));

        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        for (String fragment : new String[] {
                "JOY_OF_PAIN, new JoyOfPainSpell(",
                "JOY_OF_PAIN, 27, 1, 19, 6_528",
                "40, 0, 3_000, 0, 2, 3",
                "49_134L, \"$5698\", \"$5952\", true, false"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useJoyOfPainSpell(payload,", "joyOfPainStatus.activateOrRefresh(",
                "applyJoyOfPainPlayerFeedback(",
                "applyJoyOfPainClassicWorldMonsterFeedback(",
                "applyJoyOfPainCompanionFeedback(",
                "applyJoyOfPainArenaBotFeedback(",
                "applyJoyOfPainEncounterFeedback(",
                "BattleHitReactionSuppressCatalog.shouldSendDamageAction(attacker.gfx())",
                "consumeQuestMonsterJoyOfPain(",
                "consumeTimeAttackActorJoyOfPain("}) {
            assertTrue(session.contains(fragment), fragment);
        }
        for (String source : new String[] {
                "BattleCompanion.java", "BattleQuestEncounter.java",
                "BattleTimeAttackEncounter.java",
                "ClassicWorldMonsterPopulation.java", "BattleGuardianTower.java"}) {
            String value = read(Path.of("src/main/java/local/lito381/server", source));
            assertTrue(value.contains("BattleJoyOfPainStatus"), source);
            assertTrue(value.contains("BattleJoyOfPainStatus.Activation"), source);
        }
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
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
