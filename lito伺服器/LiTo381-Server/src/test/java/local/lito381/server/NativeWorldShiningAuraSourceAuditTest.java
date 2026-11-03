package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks skill115's source and ordinary-WORLD clan-or-party route. */
final class NativeWorldShiningAuraSourceAuditTest {
    @Test
    void nativeDatabaseClanPartyAcReplacementAndCancellationStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (115, 0, '鋼鐵士氣', 15, 2, 40, 0, 0, 0, 0, 640, 'none', 12, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 4, '$1977', 19, 3941, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (115, '鋼鐵士氣', 'AC', -8);"));
        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("L1Skills.TARGET_TO_CLAN"));
        assertTrue(source.contains("_player.getClanid() != 0"));
        assertTrue(source.contains("L1Skills.TARGET_TO_PARTY"));
        assertTrue(source.contains("{ GLOWING_AURA, SHINING_AURA }"));
    }

    @Test
    void worldDispatcherClanPartyRoiAcExpiryAndStatusStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.RoyalAuraSpell worldRoyalAura", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String route = session.substring(dispatch, unsupported);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(route.contains("BattleSpellCatalog.GLOWING_AURA"));
        assertTrue(route.contains("BattleSpellCatalog.SHINING_AURA"));

        int start = session.indexOf("private void useRoyalAuraSpell(");
        int end = session.indexOf("private void useCallClanSpell(", start);
        String method = session.substring(start, end);
        assertTrue(method.contains("boolean clanTarget = (spell.targetTo() & 4) != 0"));
        assertTrue(method.contains("boolean partyTarget = (spell.targetTo() & 8) != 0"));
        assertTrue(method.contains("sameNonemptyClan(candidate)"));
        assertTrue(method.contains("sameClassicParty(candidate)"));
        assertTrue(method.contains("classicWorldMembers(casterMap)"));
        assertTrue(method.contains("activateRoyalAura("));
        assertTrue(method.contains("target.sendArmorClassStatus("));
        assertTrue(method.contains("candidate.classicWorldInstanceId == originInstanceId"));
        assertTrue(session.contains(
                "expiredRoyalAura != 0 && (inBattle || inClassicWorld)"));
    }
}
