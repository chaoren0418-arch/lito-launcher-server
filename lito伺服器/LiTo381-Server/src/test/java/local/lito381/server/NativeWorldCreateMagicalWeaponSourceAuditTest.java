package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill73 and its ordinary-WORLD inventory mutation flow. */
final class NativeWorldCreateMagicalWeaponSourceAuditTest {
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
    void nativeInventoryCallbackSelectedRowsAndRetainedMutationStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (73, 0, '創造魔法武器', 10, 0, 200, 0, 40318, 10, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, '$1481', 19, 763, 0, 685, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (73, '創造魔法武器', NULL, NULL);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (73,"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (73,"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CBA1E:");
        String route = assembly.substring(dispatch, dispatch + 320);
        assertTrue(route.contains(
                "jumptable 004CB66E cases 11,20,72,92,99,106"));
        assertTrue(route.contains("call    sub_4C9DC0"));
        int callback = assembly.indexOf("loc_4C9D76:");
        String producer = assembly.substring(callback, callback + 430);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("68, 69, 72, 73, 75, 78, 79"));
        assertTrue(skillUse.contains("case CREATE_MAGICAL_WEAPON:"));
        assertTrue(skillUse.contains("_itemobjid = target_id;"));
        assertTrue(skillUse.contains("safe_enchant < 0"));
        assertTrue(skillUse.contains("safe_enchant == 0"));
        assertTrue(skillUse.contains("enchant_level == 0"));
        assertTrue(skillUse.contains("item.setEnchantLevel(1);"));
        assertTrue(skillUse.contains("L1PcInventory.COL_ENCHANTLVL"));
        assertTrue(skillUse.contains("new S_ServerMessage(161, \"+0 \""));
        assertTrue(skillUse.contains("new S_ServerMessage(79)"));
    }

    @Test
    void worldDispatcherAdmissionMutationPersistenceAndRoiStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.MagicalWeaponSpell worldMagicalWeapon",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useMagicalWeaponSpell(payload, worldMagicalWeapon)"));

        int methodStart = session.indexOf(
                "private void useMagicalWeaponSpell(");
        int methodEnd = session.indexOf(
                "private void useClassicWorldCounterDetectionSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("phase != originPhase"));
        assertTrue(method.contains("nextInventory.removeCatalog("));
        assertTrue(method.contains("nextInventory.createMagicalWeapon("));
        assertTrue(method.contains("inventory.catalogItemStates()"));
        assertTrue(method.contains("+0 +0 "));
        assertTrue(method.contains("Protocol.changeInventoryDescriptionExtended("));
        assertTrue(method.contains("sendNativeBattleStatus("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertFalse(method.contains("consumeCounterMagic("));
    }
}
