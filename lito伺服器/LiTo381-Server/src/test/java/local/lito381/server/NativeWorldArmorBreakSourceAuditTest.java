package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks LiTo's selector-only skill112 producer and ordinary-WORLD lifecycle. */
final class NativeWorldArmorBreakSourceAuditTest {
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
                "INSERT INTO `skills` VALUES (112, 0, '破壞盔甲', 14, 7, 40, 30, 40321, 2, 2000, 8, 'buff', 1, 0, 0, 0, 50, 50, 0, 4, 0, 1, 0, 0, 128, '$13934', 19, 3400, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (112, '破壞盔甲', 50, 40, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (112, '破壞盔甲', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (112, '破壞盔甲');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CB5F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CB5F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        int routeStart = producer.indexOf("def_4CB66E:");
        int routeEnd = producer.indexOf("loc_4CBC76:", routeStart);
        String route = producer.substring(routeStart, routeEnd);
        assertTrue(route.contains(
                "default case, cases 23,80,82-85,87,88,91,93-95,111"));
        assertTrue(route.contains("mov     [edi+68h], esi"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));

        String attackPc = Files.readString(RETAINED.resolve(
                "model/L1AttackPc.java"), StandardCharsets.UTF_8);
        assertTrue(attackPc.contains("_targetPc.hasSkillEffect(ARMOR_BREAK)"));
        assertTrue(attackPc.contains("dmg = (int) (dmg * 1.58)"));
        assertTrue(attackPc.contains(
                "(_weaponType != 20) && (_weaponType != 62)"));
        String skillUse = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("ARMOR_BREAK, 113"));
        assertTrue(skillUse.contains("breakInterruptedCastEffects();"));
        assertTrue(skillUse.contains("breakInvisibilityOnCast();"));
    }

    @Test
    void worldDispatcherTargetRecoveryStatusDamageAndCancellationStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.ArmorBreakSpell worldArmorBreak", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useArmorBreakSpell(payload, worldArmorBreak)"));

        int methodStart = session.indexOf("private void useArmorBreakSpell(");
        int methodEnd = session.indexOf(
                "private void useReductionArmorSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("lastCombatTargetObjectId"));
        assertTrue(method.contains("classicWorldMonstersInScreen("));
        assertTrue(method.contains("activateClassicWorldArmorBreak("));
        assertTrue(method.contains("activateClassicWorldMonsterArmorBreak("));
        assertTrue(method.contains("classicWorldMonsterArmorBreakActive("));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("counter-magic-consumed=false"));
        assertTrue(method.contains("member.classicWorldInstanceId == originInstanceId"));

        assertTrue(session.contains(
                "lastCombatTargetObjectId = target.objectId();"));
        assertTrue(session.contains(
                "lastCombatTargetObjectId = target.playerObjectId();"));
        assertTrue(session.contains(
                "classicWorldArmorBreakActive(now)"));
        assertTrue(session.contains(
                "hub.classicWorldMonsterArmorBreakActive("));
        assertTrue(session.contains(
                "BattleArmorBreak.damageAfterBreak("));
        assertTrue(session.contains(
                "playerTarget.cancelClassicWorldArmorBreak()"));

        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("long armorBreakExpiresAtNanos;"));
        assertTrue(population.contains("synchronized boolean armorBreak("));
        assertTrue(population.contains("synchronized boolean armorBreakActive("));
        assertTrue(population.contains("boolean armorBreak = actor.armorBreakExpiresAtNanos != 0"));
    }
}
