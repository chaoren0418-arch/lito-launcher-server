package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill14 and its ordinary-WORLD weight-capacity lifecycle. */
final class NativeWorldDecreaseWeightSourceAuditTest {
    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedCapacityFormulaStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (14, 0, '負重強化', 2, 5, 10, 50, 0, 0, 10, 1800, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '$1978', 19, 2170, 0, 0, 0, 0);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CBBD4:");
        int routeEnd = assembly.indexOf("loc_4CBBED:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains("jumptable 004CB66E cases 1,2,12,13"));
        assertTrue(route.contains("and     ecx, 7"));
        assertTrue(route.contains("sar     esi, 3"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("call    sub_4C6AE0"));

        String player = Files.readString(
                RETAINED.resolve("model/Instance/L1PcInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(player.contains("if (hasSkillEffect(DECREASE_WEIGHT))"));
        assertTrue(player.contains("weightReductionByMagic = 180"));
        assertTrue(player.contains("maxWeight += weightReductionByMagic"));

        String icons = Files.readString(
                RETAINED.resolve("serverpackets/S_Icons.java"),
                StandardCharsets.UTF_8);
        assertTrue(icons.contains("pc.hasSkillEffect(DECREASE_WEIGHT)"));
        assertTrue(icons.contains(
                "data[3] = (byte) (pc.getSkillEffectTimeSec(DECREASE_WEIGHT) / 4)"));
    }

    @Test
    void worldDispatcherAndHandlerCommitTheSharedNativeLease() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int dispatchStart = session.indexOf(
                "BattleSpellCatalog.OtherNoEffectSpell worldOtherNoEffect");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.AquaProtectSpell worldAquaProtect",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains("BattleSpellCatalog.WeightSpell worldWeight"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.weightSpell(selector.skillId())"));
        assertTrue(dispatch.contains("useWeightSpell(payload, worldWeight)"));

        int handlerStart = session.indexOf("private void useWeightSpell(");
        int handlerEnd = session.indexOf(
                "private ClientSession resolveSelectorOnlyConcentrationTarget(",
                handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(handler.contains("polymorphStatus.active(requestNow)"));
        assertTrue(handler.contains("timedStatus.silenceActive(requestNow)"));
        assertTrue(handler.contains("currentWeight240(requestNow)"));
        assertTrue(handler.contains("learnedSkills.contains(spell.skillId())"));
        assertTrue(handler.contains("phase == originPhase && !observer && playerHp > 0"));
        assertTrue(handler.contains("classicWorldMapId == originMapId"));
        assertTrue(handler.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(handler.contains("weightStatus.activate(spell.skillId()"));
        assertTrue(handler.contains("revealInvisibility(\"spell-\" + spell.skillId())"));
        assertTrue(handler.contains("cancelAbsoluteBarrier(\"spell-\" + spell.skillId())"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("BattleSpellCatalog.isInNativeScreen("));

        int expiryStart = session.indexOf(
                "if (weightStatus.expire(nowNanos)");
        int expiryEnd = session.indexOf(
                "BattlePolymorphCatalog.Form expiredPolymorph", expiryStart);
        String expiry = session.substring(expiryStart, expiryEnd);
        assertTrue(expiry.contains("(inBattle || inClassicWorld)"));
        assertTrue(expiry.contains(
                "sendNativeBattleStatus(nowNanos, \"weight-spell-expired\")"));
    }
}
