package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill79 and its ordinary-WORLD maximum-vital lifecycle. */
final class NativeWorldAdvanceSpiritSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedFormulaStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (79, 0, '靈魂昇華', 10, 6, 20, 0, 0, 0, 10, 1200, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 64, '$1993', 19, 3935, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (79, '靈魂昇華', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (79, '靈魂昇華');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CB71D:");
        int routeEnd = assembly.indexOf("loc_4CB741:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains(
                "jumptable 004CB66E cases 7,8,22,36,42,43,47,54,66,78"));
        assertTrue(route.contains("call    sub_4C9880"));
        int senderStart = assembly.indexOf("sub_4C9880 proc near");
        int senderEnd = assembly.indexOf("sub_4C9880 endp", senderStart);
        String sender = assembly.substring(senderStart, senderEnd);
        assertTrue(sender.contains("push    offset aCccd"));
        assertTrue(sender.contains("push    14h"));
        assertTrue(sender.contains("call    sub_4C6AE0"));

        String mode = Files.readString(
                RETAINED.resolve("model/skill/skillmode/ADVANCE_SPIRIT.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("pc.setAdvenHp(pc.getBaseMaxHp() / 5)"));
        assertTrue(mode.contains("pc.setAdvenMp(pc.getBaseMaxMp() / 5)"));
        assertTrue(mode.contains(
                "new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp())"));
        assertTrue(mode.contains(
                "new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp())"));
        assertTrue(mode.contains("pc.setAdvenHp(0)"));
        assertTrue(mode.contains("pc.setAdvenMp(0)"));
    }

    @Test
    void worldHandlerUsesBaseMaximumAndPublishesWorldExpiry() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int dispatchStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.CounterMagicSpell worldCounterMagic",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains("worldAdvanceSpirit"));
        assertTrue(dispatch.contains(
                "useAdvanceSpiritSpell(payload, worldAdvanceSpirit)"));
        assertTrue(dispatch.contains("learnedSkills.contains(selector.skillId())"));

        int handlerStart = session.indexOf("private void useAdvanceSpiritSpell(");
        int handlerEnd = session.indexOf("private void useArmorBreakSpell(", handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(handler.contains("sameClassicWorldMap(target)"));
        assertTrue(handler.contains("phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(handler.contains("target.phase != phase"));
        assertTrue(handler.contains("commitSpellReady(spell.skillId()"));
        assertTrue(handler.contains("target.rebuildMaximumVitals(now)"));

        int maximumStart = session.indexOf(
                "private int battleMaximumWithAdvanceSpirit(");
        int maximumEnd = session.indexOf("private void sendBattleObject(", maximumStart);
        String maximum = session.substring(maximumStart, maximumEnd);
        assertTrue(maximum.contains(
                "spell.maximumWithBonus(")
                && maximum.contains("sourceBaseMaximum"));

        int expiryStart = session.indexOf(
                "if (timedStatus.expireAdvanceSpirit(nowNanos)");
        int expiryEnd = session.indexOf("List<BattleCookingCatalog.Recipe>", expiryStart);
        String expiry = session.substring(expiryStart, expiryEnd);
        assertTrue(expiry.contains("(inBattle || inClassicWorld)"));
        assertTrue(expiry.contains("rebuildMaximumVitals(nowNanos)"));
        assertTrue(expiry.contains("S_HIT_POINT advance-spirit-expired"));
        assertTrue(expiry.contains("S_MANA_POINT advance-spirit-expired"));
    }
}
