package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks skill114's selector-only producer and ordinary-WORLD party route. */
final class NativeWorldGlowingAuraSourceAuditTest {
    @Test
    void nativeDatabasePartyHitMrAndCancellationStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (114, 0, '激勵士氣', 15, 1, 40, 0, 0, 0, 0, 640, 'none', 8, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 2, '$1996', 19, 2277, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (114, '激勵士氣', 'HIT', 5);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (114, '激勵士氣', 'BOW_HIT', 5);"));

        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("L1Skills.TARGET_TO_PARTY"));
        assertTrue(source.contains("_player.getParty().isMember(xpc)"));
        assertTrue(source.contains("World.get().getVisibleObjects(this._user)"));
        assertTrue(source.contains("pc.addMr(20)"));
        assertTrue(source.contains("new S_SPMR(pc)"));
    }

    @Test
    void worldDispatcherPartyRoiCommitExpiryAndStatusStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.RoyalAuraSpell worldRoyalAura", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "BattleSpellCatalog.GLOWING_AURA"));

        int start = session.indexOf("private void useRoyalAuraSpell(");
        int end = session.indexOf("private void useCallClanSpell(", start);
        String method = session.substring(start, end);
        assertTrue(method.contains("originPhase == Phase.WORLD"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("classicWorldMembers(casterMap)"));
        assertTrue(method.contains("sameClassicParty(candidate)"));
        assertTrue(method.contains("candidate.classicWorldInstanceId == originInstanceId"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("activateRoyalAura("));
        assertTrue(method.contains("target.sendMagicStatus("));
        assertTrue(method.contains("\" phase=\" + originPhase"));
        assertTrue(session.contains(
                "expiredRoyalAura != 0 && (inBattle || inClassicWorld)"));
    }
}
