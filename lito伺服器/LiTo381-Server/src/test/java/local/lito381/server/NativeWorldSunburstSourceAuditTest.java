package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill46 source, resources and shared attack convergence. */
final class NativeWorldSunburstSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    void bothDatabasesAndCurrentCatalogAgreeOnTheSelectedReuseAndRange()
            throws Exception {
        String selected = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (46, 0, '烈炎術', 6, 5, 20, "
                + "0, 0, 0, 500, 0, 'attack', 3, 32, 5, 16, 0, 0, 2, 64, "
                + "0, 3, 0, 0, 32, '$1973', 18, 1811, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (46, '烈炎術', NULL, NULL);"));

        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (46, '烈炎術', 6, 5, 20, 0, "
                + "0, 0, 500, 0, 'attack', 3, 32, 5, 16, 0, 0, 2, 64, 0, "
                + "3, 0, 0, 32, '$1973', 18, 1811, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (46, '烈炎術', NULL, NULL);"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "SUNBURST, new AttackSpell(SUNBURST, 5, 5, 18, 1811,"));
        assertTrue(catalog.contains(
                "20, 500, 3, 32, 5, 16, ATTRIBUTE_FIRE, \"sunburst\""));
    }

    @Test
    void retainedFireDamageAndCurrentSharedRoutesPreserveEveryBoundary()
            throws Exception {
        String retainedMagic = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                StandardCharsets.UTF_8);
        for (String anchor : new String[] {
                "private int calcMagicDiceDamage(final int skillId)",
                "magicDamage += (_random.nextInt(dice) + 1)",
                "final double attrDeffence = calcAttrResistance(l1skills.getAttr())",
                "charaIntelligence * 3.0 / 32.0",
                "_pc.getOriginalMagicCritical()",
                "magicDamage += _pc.getOriginalMagicDamage()",
                "magicDamage += _pc.get_magic_modifier_dmg()",
                "private int calcMrDefense(int dmg)"}) {
            assertTrue(retainedMagic.contains(anchor), anchor);
        }
        String retainedUse = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        String normal = retainedUse.substring(
                retainedUse.indexOf("case TYPE_NORMAL ->"),
                retainedUse.indexOf("case TYPE_LOGIN ->"));
        assertTrue(normal.indexOf("commitMaterialAndAcquireCastLocks()")
                < normal.indexOf("breakInterruptedCastEffects()"));
        assertTrue(normal.indexOf("breakInterruptedCastEffects()")
                < normal.indexOf("breakInvisibilityOnCast()"));
        assertTrue(retainedUse.contains("case L1Skills.TYPE_ATTACK"));
        assertTrue(retainedUse.contains("isUseCounterMagic(cha)"));

        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String battle = interval(source,
                "private void useAttackSpell(\n"
                        + "            Protocol.AttackSpellRequest request,",
                "private BoneBreakApplication applyBattleBoneBreak(");
        assertCleanupOrder(battle);
        for (String anchor : new String[] {
                "hub.guardianTower(", "hub.questMonster(",
                "hub.timeAttackActor(", "hub.damageBot(",
                "target.timedStatus.consumeCounterMagic(castNow)",
                "target.earthBindProtectedCombatDamage("}) {
            assertTrue(battle.contains(anchor), anchor);
        }

        String world = interval(source,
                "private void useClassicWorldAttackSpell(\n"
                        + "            Protocol.AttackSpellRequest request,",
                "private BoneBreakApplication applyClassicWorldBoneBreak(");
        assertCleanupOrder(world);
        for (String anchor : new String[] {
                "classicWorldCompanionSnapshots()", "hub.classicWorldMonster(",
                "classicCastleTrebuchetCombatSnapshot(",
                "classicCastleTowerCombatSnapshot(",
                "dwarfCastleDoorCombatSnapshot(",
                "mainlandCastleDoorCombatTarget(",
                "playerTarget.timedStatus.consumeCounterMagic(castNow)",
                "playerTarget.earthBindProtectedCombatDamage("}) {
            assertTrue(world.contains(anchor), anchor);
        }
    }

    private static String interval(String source, String start, String end) {
        int first = source.indexOf(start);
        int last = source.indexOf(end, first);
        assertTrue(first >= 0 && last > first);
        return source.substring(first, last);
    }

    private static void assertCleanupOrder(String method) {
        int commit = method.indexOf("spellReadyNanos.put(spell.skillId()");
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisibility = method.indexOf("revealInvisibility(", barrier);
        assertTrue(commit >= 0 && meditation > commit
                && barrier > meditation && invisibility > barrier);
        assertTrue(!method.contains("commitSpellReady(spell.skillId()"));
    }
}
