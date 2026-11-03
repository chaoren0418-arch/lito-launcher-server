package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected skill45 source, resource and BATTLE/WORLD convergence fidelity. */
final class NativeWorldEruptionSourceAuditTest {
    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");

    @Test
    void selectedAndSecondaryRowsKeepTheirDistinctReuseValues()
            throws Exception {
        String selected = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (45, 0, '地裂術', 6, 4, 20, "
                + "0, 0, 0, 100, 0, 'attack', 3, 40, 3, 5, 0, 0, 1, 64, "
                + "0, 8, 0, 0, 16, '$1465', 18, 129, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (45, '地裂術', NULL, NULL);"));

        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (45, '地裂術', 6, 4, 20, 0, "
                + "0, 0, 400, 0, 'attack', 3, 40, 3, 5, 0, 0, 1, 64, 0, "
                + "8, 0, 0, 16, '$1465', 18, 129, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (45, '地裂術', NULL, NULL);"));

        String catalog = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "ERUPTION, new AttackSpell(ERUPTION, 5, 4, 18, 129,"));
        assertTrue(catalog.contains(
                "20, 100, 8, 40, 3, 5, ATTRIBUTE_EARTH, \"eruption\""));
    }

    @Test
    void retainedDamageAndSuccessfulCastCleanupOrderReachEveryTargetFamily()
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
        String normalCast = retainedUse.substring(
                retainedUse.indexOf("case TYPE_NORMAL ->"),
                retainedUse.indexOf("case TYPE_LOGIN ->"));
        assertTrue(normalCast.indexOf("commitMaterialAndAcquireCastLocks()")
                < normalCast.indexOf("breakInterruptedCastEffects()"));
        assertTrue(normalCast.indexOf("breakInterruptedCastEffects()")
                < normalCast.indexOf("breakInvisibilityOnCast()"));
        assertTrue(retainedUse.contains("case L1Skills.TYPE_ATTACK"));
        assertTrue(retainedUse.contains("isUseCounterMagic(cha)"));

        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int battleStart = source.indexOf(
                "private void useAttackSpell(\n"
                + "            Protocol.AttackSpellRequest request,");
        int battleEnd = source.indexOf(
                "private BoneBreakApplication applyBattleBoneBreak(", battleStart);
        String battle = source.substring(battleStart, battleEnd);
        assertCleanupAfterCommit(battle);
        assertTrue(!battle.contains("commitSpellReady(spell.skillId()"));
        for (String anchor : new String[] {
                "hub.guardianTower(", "hub.questMonster(",
                "hub.timeAttackActor(", "hub.damageBot(",
                "target.timedStatus.consumeCounterMagic(castNow)",
                "target.earthBindProtectedCombatDamage("}) {
            assertTrue(battle.contains(anchor), anchor);
        }

        int worldStart = source.indexOf(
                "private void useClassicWorldAttackSpell(\n"
                + "            Protocol.AttackSpellRequest request,");
        int worldEnd = source.indexOf(
                "private BoneBreakApplication applyClassicWorldBoneBreak(",
                worldStart);
        String world = source.substring(worldStart, worldEnd);
        assertCleanupAfterCommit(world);
        assertTrue(!world.contains("commitSpellReady(spell.skillId()"));
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

    private static void assertCleanupAfterCommit(String method) {
        int commit = method.indexOf("spellReadyNanos.put(spell.skillId()");
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisibility = method.indexOf("revealInvisibility(", barrier);
        assertTrue(commit >= 0 && meditation > commit
                && barrier > meditation && invisibility > barrier);
    }
}
