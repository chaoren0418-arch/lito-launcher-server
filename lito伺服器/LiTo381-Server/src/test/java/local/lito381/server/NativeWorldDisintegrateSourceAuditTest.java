package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill77 and the complete ordinary-WORLD attack target family. */
final class NativeWorldDisintegrateSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (77, 0, '究極光裂術', 10, 4, 70, 0, 0, 0, 5000, 0, 'attack', 3, 190, 15, 8, 0, 0, 16, 64, 1, 10, 0, 0, 16, '$1485', 18, 11746, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (77, '究極光裂術', NULL, NULL);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (77,"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CB755:");
        String route = assembly.substring(routeStart, routeStart + 900);
        assertTrue(route.contains("cases 3,5,6,17,28,37,44,49,64,73,76"));
        assertTrue(route.contains("push    12h"));
        assertTrue(route.contains("mov     edx, 0Ah"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        assertTrue(assembly.contains("aCccdhh db 'cccdhh',0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "(this._skillId == DISINTEGRATE) && (pc.getLawful() < 500)"));
        assertTrue(skillUse.contains("new S_ServerMessage(352, \"$967\")"));
        assertTrue(skillUse.contains("isPcSummonPet(_target)"));
        assertTrue(skillUse.contains("_player.checkNonPvP(_player, _target)"));
        assertTrue(skillUse.contains("magic.calcMagicDamage(_skillId)"));
        assertTrue(skillUse.contains(
                "_disintegrateCrit = (_skillId == DISINTEGRATE && magic.isDisintegrateCrit())"));
        assertTrue(skillUse.contains("ConfigSkill.CRITICAL_SPR"));
    }

    @Test
    void worldHandlerConnectsPlayersCompanionsAndOrdinaryMonsters()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int methodStart = session.indexOf(
                "private void useClassicWorldAttackSpell(");
        int methodEnd = session.indexOf(
                "private void publishClassicWorldAttackSpellPacket(", methodStart);
        assertTrue(methodStart > 0 && methodEnd > methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(method.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("classicWorldMonster("));
        assertTrue(method.contains("kind().classicWorldEligible()"));
        assertTrue(method.contains("controlledCompanion.effectiveMagicResistance("));
        assertTrue(method.contains("controlledCompanion.captureEraseMagic("));
        assertTrue(method.contains("controlledCompanion.consumeEraseMagic("));
        assertTrue(method.contains("applyClassicWorldMonsterCompanionDamage("));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("spell.specialCritical("));
        assertTrue(method.contains("spell.effectGfx(specialCritical)"));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertFalse(method.contains("hub.roomMembers("));
        assertFalse(method.contains("hub.roomCombatants("));

        int battleStart = session.indexOf(
                "private void useAttackSpell(byte[] payload,",
                session.indexOf("private void useAttackSpell(byte[] payload,") + 1);
        int battleEnd = session.indexOf(
                "private void useClassicWorldAreaAttackSpell(", battleStart);
        assertTrue(battleStart > 0 && battleEnd > battleStart);
        String battleMethod = session.substring(battleStart, battleEnd);
        assertTrue(battleMethod.contains("cancelAbsoluteBarrier("));
        assertTrue(battleMethod.contains("revealInvisibility("));
    }
}
