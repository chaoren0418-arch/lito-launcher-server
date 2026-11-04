package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks skill117's source and ordinary-WORLD party/physical route. */
final class NativeWorldBraveAuraSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    void nativePartyIndependentLeaseAndMeleeDamageStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (117, 0, '衝擊士氣', 15, 4, 40, 0, 0, 0, 0, 640, 'none', 8, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 16, '$3178', 19, 3942, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (117, '衝擊士氣', 'DMG', 5);"));
        String use = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(use.contains("L1Skills.TARGET_TO_PARTY"));
        assertTrue(use.contains("_player.getParty().isMember(xpc)"));
        assertTrue(use.contains("{ GLOWING_AURA, SHINING_AURA }"));
        assertTrue(!use.contains("{ GLOWING_AURA, SHINING_AURA, BRAVE_AURA }"));
    }

    @Test
    void worldDispatcherPartyRoiMonsterDamageAndExpiryStayConnected()
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
        String route = session.substring(dispatch, unsupported);
        assertTrue(route.contains("BattleSpellCatalog.BRAVE_AURA"));
        String method = session.substring(
                session.indexOf("private void useRoyalAuraSpell("),
                session.indexOf("private void useCallClanSpell("));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("classicWorldMembers(casterMap)"));
        assertTrue(method.contains("sameClassicParty(candidate)"));
        assertTrue(method.contains("target.abilityStatus.activateBraveAura("));
        assertTrue(session.contains("ranged ? 0 : braveAura.meleeDamage()"));
        assertTrue(session.contains("brave-aura-damage="));
        assertTrue(session.contains("expireBraveAura(nowNanos)"));
        assertTrue(session.contains("inBattle || inClassicWorld"));
        assertTrue(session.contains("Cancellation player cleared Brave Aura"));
    }
}
