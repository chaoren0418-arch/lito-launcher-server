package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the selected skill12 producer, database row and item-local source path. */
final class NativeWorldEnchantWeaponSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/古老src/com/lineage/server/model/Instance/L1ItemInstance.java"),
                Path.of("../1-伺服器/其他src參考用/貓大src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path WORKSPACE = Path.of("..").toAbsolutePath().normalize();
    private static final Path RETAINED = WORKSPACE.resolve("1-伺服器");

    @Test void selectedDatabasePinsEnchantWeaponMetadataAndImmunity()
            throws Exception {
        String database = LocalResources.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (12, 0, '擬似魔法武器', 2, 3, 20, 0, 0, 0, 10, 1800, 'none', 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 8, '$1444', 19, 747, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (12, '擬似魔法武器');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (12, '擬似魔法武器', 'DMG', 2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (12, '擬似魔法武器', '40495,40499,40505,88', '10,8,3,1');"));
    }

    @Test void selectedClientOpensInventoryAndCallbackEmitsExactCccd()
            throws Exception {
        String assembly = LocalResources.readString(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        assertTrue(assembly.contains(
                "mov     ecx, esi                        ; jumptable 004CB66E cases 11,20,72,92,99,106"));
        assertTrue(assembly.contains("call    sub_4C9DC0"));
        int callback = assembly.indexOf("loc_4C9D76:");
        int callbackEnd = assembly.indexOf("sub_4C9DC0 proc near", callback);
        String producer = assembly.substring(callback, callbackEnd);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));
    }

    @Test void olderItemLocalSourcePinsSelectedObjectReplacementAndPlusTwo()
            throws Exception {
        String skillUse = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/貓大src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("else if (this._skillId == 12)"));
        assertTrue(skillUse.contains(
                "getInventory().getItem(this._itemobjid)"));
        assertTrue(skillUse.contains(
                "item.setSkillWeaponEnchant(pc2, this._skillId, this._skill.getBuffDuration() * 1000)"));
        assertTrue(skillUse.contains(
                "new S_ServerMessage(161, item.getLogName(), \"$245\", \"$247\")"));

        String item = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/古老src/com/lineage/server/model/Instance/L1ItemInstance.java"),
                StandardCharsets.UTF_8);
        int method = item.indexOf("setSkillWeaponEnchant(");
        int end = item.indexOf("private int _itemOwnerId", method);
        String lease = item.substring(method, end);
        assertTrue(lease.contains("if (_isRunning)"));
        assertTrue(lease.contains("setDmgByMagic(0)"));
        assertTrue(lease.contains("case ENCHANT_WEAPON:"));
        assertTrue(lease.contains("setDmgByMagic(2)"));
        assertTrue(lease.contains("case SHADOW_FANG:"));
        assertTrue(lease.contains("setDmgByMagic(5)"));
    }

    @Test void productionRouteUsesSharedItemLeaseAndBothPhysicalPaths()
            throws Exception {
        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("static final int ENCHANT_WEAPON = 12;"));
        assertTrue(catalog.contains(
                "Map.entry(ENCHANT_WEAPON, new ItemWeaponEnchantSpell("));
        assertTrue(catalog.contains("20, 10, 1_800, ATTRIBUTE_FIRE, 2"));
        assertTrue(catalog.contains("8, 2, 2, 79, 161, 308"));

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int castStart = session.indexOf("private void useItemWeaponEnchantSpell(");
        int castEnd = session.indexOf("private void useMagicalWeaponSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(cast.contains("spell.consumesItem()"));
        assertTrue(cast.contains("itemWeaponEnchantStatus.activate("));
        assertTrue(cast.contains("weaponBuffStatus.cancelMagicEffects()"));
        assertTrue(cast.contains("spell.successMessage()"));
        assertTrue(session.contains("spell.expirationMessage()"));

        assertTrue(session.contains(
                "itemWeaponEnchantStatus.modifier(\n"
                        + "                        physicalAttackNowNanos, runtimeWeaponObjectId)"));
        assertTrue(session.contains(
                "itemWeaponEnchantStatus.modifier(nowNanos, runtimeWeaponObjectId)"));
        assertTrue(session.contains("itemWeaponEnchantStatus.remove("));
    }
}
