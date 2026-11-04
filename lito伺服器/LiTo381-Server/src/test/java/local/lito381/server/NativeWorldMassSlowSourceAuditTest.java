package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill76 and its complete ordinary-WORLD area-slow families. */
final class NativeWorldMassSlowSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedAreaRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (76, 0, '集體緩速術', 10, 3, 20, 0, 40318, 2, 3000, 64, 'buff', 3, 0, 0, 0, 0, 7, 1, 1, 0, -1, 3, 0, 8, '$1484', 19, 3932, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (76, '集體緩速術', 3);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (76,"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CB989:");
        String route = assembly.substring(routeStart, routeStart + 500);
        assertTrue(route.contains("cases 14,15,33,65,75"));
        assertTrue(route.contains("mov     edx, 0Ah"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        assertTrue(assembly.contains("aCccdhh db 'cccdhh',0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "objects = World.get().getVisibleObjects(this._target, this._skill.getArea());"));
        assertTrue(skillUse.contains("case MASS_SLOW:"));
        assertTrue(skillUse.contains("if (this._user.getId() == cha.getId())"));
        assertTrue(skillUse.contains("cha instanceof L1SummonInstance"));
        assertTrue(skillUse.contains("summon.getMaster().getId()"));
        assertTrue(skillUse.contains("cha instanceof L1PetInstance"));
        assertTrue(skillUse.contains("pet.getMaster().getId()"));
        assertTrue(skillUse.contains(
                "(this._skillId == SLOW) || (this._skillId == MASS_SLOW)"));
        assertTrue(skillUse.contains("&& (isErase == false)"));
        assertTrue(skillUse.contains("pc.getHasteItemEquipped() > 0"));
        assertTrue(skillUse.contains("cha.getBraveSpeed() == 5"));
    }

    @Test
    void worldDispatcherConnectsPlayersCompanionsAndOrdinaryMonsters()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.MassSlowSpell worldMassSlow", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldMassSlowSpell(payload, worldMassSlow)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldMassSlowSpell(");
        int methodEnd = session.indexOf(
                "private void useMassSlowSpell(", methodStart);
        assertTrue(methodStart > 0 && methodEnd > methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("owner != this"));
        assertTrue(method.contains("hub.classicWorldMonstersInScreen("));
        assertTrue(method.contains("spell.areaRadius()"));
        assertTrue(method.contains("map.attackPosition("));
        assertTrue(method.contains("map.isSafetyZone("));
        assertTrue(method.contains("selectedSourceAccounts()"));
        assertTrue(method.contains(".npcPhysicalStats("));
        assertTrue(method.contains("stats.eraseEligible()"));
        assertTrue(method.contains(
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("earthBindBlocksProbabilityMagic("));
        assertTrue(method.contains("consumeCounterMagic("));
        assertTrue(method.contains(".controlled().activateSlow("));
        assertTrue(method.contains("hub.slowClassicWorldMonster("));
        assertTrue(method.contains("hub.provokeClassicWorldMonster("));
        assertTrue(method.contains("Protocol.speed("));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("spell.failureMessage()"));
        assertFalse(method.contains("hub.roomMembers("));
        assertFalse(method.contains("hub.roomCombatants("));

        String companion = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"),
                StandardCharsets.UTF_8);
        assertTrue(companion.contains(
                "synchronized BattleTimedStatus.SpeedChange activateSlow("));
        assertTrue(companion.contains("slowExpiresAtNanos"));
        assertTrue(companion.contains("expireSlow("));
        assertTrue(companion.contains("BattleActionInterval.SLOW_MULTIPLIER"));
    }
}
