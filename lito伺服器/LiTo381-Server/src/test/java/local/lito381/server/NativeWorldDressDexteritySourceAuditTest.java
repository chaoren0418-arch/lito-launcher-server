package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill110 and its ordinary-WORLD replacement lifecycle. */
final class NativeWorldDressDexteritySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void retainedSourcePinsDexterityReplacementAndTypeTwoPackets() throws Exception {
        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (110, 0, '敏捷提升', 14, 5, 10, 0, 0, 0, 0, 960, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 3910, 0, 294, 295, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (110, '敏捷提升', 'DEX', 2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (110, '敏捷提升', NULL, NULL);"));

        String use = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(use.contains("{ PHYSICAL_ENCHANT_DEX, DRESS_DEXTERITY }"));
        assertTrue(use.contains("this._skillId == DRESS_DEXTERITY"));

        String icon = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/IndependentSkillIcon.java"),
                StandardCharsets.UTF_8);
        assertTrue(icon.contains("case DRESS_DEXTERITY:"));
        assertTrue(icon.contains("new S_Dexup(pc, 2, timeSec)"));

        String writer = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Dexup.java"),
                StandardCharsets.UTF_8);
        assertTrue(writer.contains("this.writeH(time);"));
        assertTrue(writer.contains("this.writeC(pc.getDex());"));
        assertTrue(writer.contains("this.writeC(type);"));
    }

    @Test
    void worldRouteContextRoiAndExpiryRemainConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.AbilityBuffSpell worldAbilityBuff", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String route = session.substring(dispatch, unsupported);
        assertTrue(route.contains(
                "BattleSpellCatalog.abilityBuffSpell(selector.skillId())"));
        assertTrue(route.contains("admitClassicWorldSpellFamily("));
        assertTrue(route.contains("useAbilityBuffSpell(payload, worldAbilityBuff)"));

        int methodStart = session.indexOf("private void useAbilityBuffSpell(");
        int methodEnd = session.indexOf(
                "private void sendAbilityBuffStop(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("originPhase == Phase.WORLD"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains(
                "target.classicWorldInstanceId == originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(method.contains("revealInvisibility(\"spell-\""));
        assertTrue(method.contains("phase=\" + originPhase"));
        assertTrue(method.contains("target.abilityStatus.activateDexterity("));
        assertTrue(method.contains("Protocol.mageDexterity(spell.durationSeconds()"));

        int dexterityExpiry = session.indexOf("int expiredDexteritySkillId =");
        int strengthExpiry = session.indexOf(
                "int expiredStrengthSkillId =", dexterityExpiry);
        assertTrue(session.substring(dexterityExpiry, strengthExpiry).contains(
                "(inBattle || inClassicWorld)"));
        assertFalse(method.contains("phase != Phase.BATTLE || playerHp <= 0"));
    }
}
