package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for Dragon Knight skill191 Mortal Body. */
final class NativeMortalBodySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/其他控制端/4-職業_技能設定表.properties"),
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/combat/LegacyDefenseFeedbackPlan.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test void selectedRowsPinCastProbabilityBossAndCancellationRules()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (191, 0, '致命身軀', 24, 6,"
                + " 0, 50, 0, 0, 1000, 300, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 64, '$5930', 19, 6519, 0, 0, 0, 0);",
                skillRow(primary, 191));
        assertEquals("INSERT INTO `skills` VALUES (191, '致命身軀', 24, 6, 0,"
                + " 50, 0, 0, 0, 300, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0,"
                + " 0, 0, 0, 64, '$5930', 19, 6519, 0, 0, 0, 0);",
                skillRow(secondary, 191));
        assertTrue(primary.contains("INSERT INTO `skills_負面技能控制` VALUES"
                + " (191, '致命身軀', 35, 25, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (191, '致命身軀', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (191, '致命身軀', NULL, NULL);"));
        assertFalse(primary.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                + " (191, '致命身軀');"));
        assertTrue(primary.contains("49112, '龍騎士書版(致命身軀)'"));
    }

    @Test void selectedClientUsesGenericSelectorAndThirtyMillisecondClock()
            throws Exception {
        BattleSpellCatalog.MortalBodySpell spell =
                BattleSpellCatalog.mortalBodySpell(BattleSpellCatalog.MORTAL_BODY);
        assertEquals(23, spell.row());
        assertEquals(6, spell.column());
        assertEquals(30, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (spell.skillId() - 1) * 2L));
    }

    @Test void retainedCommitAndDamageFormulaStayConnected() throws Exception {
        String config = read(RETAINED.resolve(
                "config/其他控制端/4-職業_技能設定表.properties"));
        assertTrue(config.contains("MORTAL_BODY_DMG = 20"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        for (String fragment : new String[] {
                "!suppressCombatFeedback && this.getId() != attacker.getId()",
                "this.hasSkillEffect(MORTAL_BODY)",
                "calcProbabilityMagic(MORTAL_BODY)",
                "ConfigSkill.MORTAL_BODY_DMG",
                "sumMortalBodyReflectWeaponEnchant(attacker)",
                "d -= getDamageReductionByArmor()",
                "d -= getPvpRedux()", "d -= getMeleeRedux()",
                "return Math.max(1, d)",
                "DamageEvent(attacker, this, damage, 0, false, true)"}) {
            assertTrue(pc.contains(fragment), "missing retained rule: " + fragment);
        }
        String feedback = read(RETAINED.resolve(
                "src/com/lineage/server/model/combat/LegacyDefenseFeedbackPlan.java"));
        assertTrue(feedback.contains("attacker == null || attacker.isDead()"));
        assertTrue(feedback.contains("applyFeedbackDamage(\"MORTAL_BODY\""));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useMortalBodySpell(payload, worldMortalBody)",
                "useMortalBodySpell(payload, mortalBodySpell)",
                "tryMortalBodyPlayerReflection(",
                "tryMortalBodyClassicWorldMonsterReflection(",
                "tryMortalBodyArenaBotReflection(",
                "tryMortalBodyEncounterReflection(",
                "totalPvpReduction()", "recursive-feedback-suppressed=true"}) {
            assertTrue(session.contains(fragment),
                    "missing implementation: " + fragment);
        }
    }

    @Test void npcHitReactionSuppressionExactlyMatchesSelectedSource()
            throws Exception {
        Matcher rows = Pattern.compile(
                "INSERT INTO `系統_受擊無動作gfx` VALUES \\((\\d+),")
                .matcher(read(PRIMARY));
        Set<Integer> selected = new HashSet<>();
        while (rows.find()) selected.add(Integer.parseInt(rows.group(1)));
        assertEquals(107, selected.size());
        assertEquals(selected,
                BattleHitReactionSuppressCatalog.suppressedGfx());
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }
}
