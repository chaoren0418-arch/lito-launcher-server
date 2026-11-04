package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill74 and its ordinary-WORLD target-centred damage flow. */
final class NativeWorldMeteorStrikeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerSelectedRowAndRetainedTargetListStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (74, 0, '流星雨', 10, 1, 60, 0, 0, 0, 3000, 0, 'attack', 3, 110, 15, 8, 0, 0, 2, 64, 0, 10, 3, 0, 2, '$1482', 18, 762, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (74, '流星雨', NULL, NULL);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (74,"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (74,"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB755:");
        String producer = assembly.substring(dispatch, dispatch + 600);
        assertTrue(producer.contains("push    12h"));
        assertTrue(producer.contains("mov     edx, 0Ah"));
        assertTrue(producer.contains("call    sub_4CA6E0"));
        assertTrue(assembly.contains("aCccdhh"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "objects = World.get().getVisibleObjects(this._target, this._skill.getArea());"));
        assertTrue(skillUse.contains("this.isUseCounterMagic(cha)"));
        assertTrue(skillUse.contains("S_RangeSkill.TYPE_DIR, true"));
    }

    @Test
    void worldDispatcherAdmissionAreaDamageAndNativePacketStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.AreaAttackSpell worldAreaAttack",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldAreaAttackSpell(payload, worldAreaAttack)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldAreaAttackSpell(");
        int methodEnd = session.indexOf(
                "private void publishClassicWorldAreaAttackSpellPacket(",
                methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("hub.classicWorldMonstersInScreen("));
        assertTrue(method.contains("Math.abs(candidate.positionX - targetX)"));
        assertTrue(method.contains("spell.areaRadius()"));
        assertTrue(method.contains("map.attackPosition("));
        assertTrue(method.contains("map.isNormalZone(positionX, positionY)"));
        assertTrue(method.contains(
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("selectedSourceAccounts().npcPhysicalStats("));
        assertTrue(method.contains("target.timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(method.contains("publishCounterMagicConsumption("));
        assertTrue(method.contains("rollAreaSpellSourceDamage(spell)"));
        assertTrue(method.contains("areaSpellDamage("));
        assertTrue(method.contains("effectiveSpellElementalResistance("));
        assertTrue(method.contains("classicWorldMonsterEffectiveMagicResistance("));
        assertTrue(method.contains("combatDamageAfterCooking("));
        assertTrue(method.contains("earthBindProtectedCombatDamage("));
        assertTrue(method.contains("counterMirrorDecision("));
        assertTrue(method.contains("Protocol.attackMany("));
        assertTrue(method.contains("takeClassicWorldPlayerDamage("));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertTrue(method.contains("publishClassicWorldSpellMonsterDamage("));
        assertTrue(method.contains("request.x() != targetX"));
        assertFalse(method.contains("hub.roomMembers("));
    }
}
