package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill111, retained ER rules, and the ordinary-WORLD route. */
final class NativeWorldDressEvasionSourceAuditTest {
    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void retainedSourcePinsErModifierBowRollAndCancellation() throws Exception {
        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (111, 0, '閃避提升', 14, 6, 20, 0, 0, 0, 0, 32, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 64, '', 19, 11766, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (111, '閃避提升', NULL, NULL);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (111,"));
        assertFalse(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (111,"));

        String pc = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(pc.contains("this.hasSkillEffect(DRESS_EVASION)"));
        assertTrue(pc.contains("er += 12;"));

        String attack = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackMode.java"),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains("final int er = _targetPc.getEr()"));
        assertTrue(attack.contains("ThreadLocalRandom.current().nextInt(100) + 1"));
        assertTrue(attack.contains("return er < rnd"));
    }

    @Test
    void worldRouteRoiCommitAndExpiryRemainConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "RangedEvasionBuffSpell worldRangedEvasion", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && dispatch < unsupported);
        String route = session.substring(dispatch, unsupported);
        assertTrue(route.contains(
                "BattleSpellCatalog.rangedEvasionBuffSpell(selector.skillId())"));
        assertTrue(route.contains(
                "useRangedEvasionBuffSpell(payload, worldRangedEvasion)"));

        int methodStart = session.indexOf(
                "private void useRangedEvasionBuffSpell(");
        int methodEnd = session.indexOf(
                "private void usePoisonResistanceSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("originPhase != Phase.BATTLE"));
        assertTrue(method.contains("originPhase != Phase.WORLD"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("revealInvisibility(\"spell-\""));
        assertTrue(method.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(method.contains("phase=\" + originPhase"));
        assertFalse(method.contains("phase != Phase.BATTLE || observer"));

        int expiry = session.indexOf(
                "if (abilityStatus.expireDressEvasion(nowNanos)");
        assertTrue(session.substring(expiry, expiry + 240).contains(
                "(inBattle || inClassicWorld)"));
    }
}
