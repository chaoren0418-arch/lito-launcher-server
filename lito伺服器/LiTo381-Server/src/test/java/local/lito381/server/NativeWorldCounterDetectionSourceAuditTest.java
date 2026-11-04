package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill72 and its ordinary-WORLD retained boundaries. */
final class NativeWorldCounterDetectionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerSelectedRowAndRetainedSpecialCasesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (72, 0, '強力無所遁形術', 9, 7, 30, 0, 0, 0, 500, 0, 'none', 3, 24, 10, 4, 0, 0, 0, 64, 0, 0, -1, 0, 128, '$1991', 19, 3934, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (72, '強力無所遁形術', NULL, NULL);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (72,"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (72,"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset off_5028CC"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("68, 69, 72, 73, 75, 78, 79"));
        assertTrue(skillUse.contains("(_skillId == COUNTER_DETECTION)"));
        assertTrue(skillUse.contains("cha.hasSkillEffect(INVISIBILITY)"));
        assertTrue(skillUse.contains("case L1NpcInstance.HIDDEN_STATUS_SINK:"));
        assertTrue(skillUse.contains(
                "this._dmg = magic.calcMagicDamage(this._skillId);"));
        assertTrue(skillUse.contains("World.get().getVisiblePlayer(pc)"));
        assertTrue(skillUse.contains("tgt.delInvis();"));
        assertTrue(skillUse.contains("WorldTrap.get().onDetection(pc);"));
    }

    @Test
    void worldDispatcherAdmissionDamageRevealAndNpcAnimationStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.CounterDetectionSpell worldCounterDetection",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldCounterDetectionSpell("));

        int methodStart = session.indexOf(
                "private void useClassicWorldCounterDetectionSpell(");
        int methodEnd = session.indexOf(
                "private void useCounterDetectionSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("hub.classicWorldMonstersInScreen("));
        assertTrue(method.contains("map.isNormalZone(originX, originY)"));
        assertTrue(method.contains(
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("spell.rollDamage("));
        assertTrue(method.contains("ordinaryMagicDamage("));
        assertTrue(method.contains("Protocol.attackMany("));
        assertTrue(method.contains("spell.effectGfx(), 0,"));
        assertTrue(method.contains("counter-magic-exempt=true"));
        assertTrue(method.contains("takeClassicWorldPlayerDamage("));
        assertTrue(method.contains("target.cancelInvisibilityState()"));
        assertTrue(method.contains("target.publishInvisibilityReveal("));
        assertTrue(method.contains("applyClassicWorldDetectionContext("));
        assertTrue(method.contains("hidden-sink-reveals="));
        assertTrue(method.contains("trap-activations="));
        assertTrue(method.contains("trap-effects="));
        assertFalse(method.contains("consumeCounterMagic("));
        assertFalse(method.contains("damageClassicWorldMonster("));
    }
}
