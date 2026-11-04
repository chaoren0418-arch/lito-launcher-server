package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill21 producer, rows and retained item-local armor path. */
final class NativeWorldBlessedArmorSourceAuditTest {
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

    @Test void selectedDatabasePinsArmorMetadataImmunityAndAc() throws Exception {
        String database = LocalResources.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (21, 0, '鎧甲護持', 3, 4, 20, 0, 0, 0, 10, 1800, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 16, '$1450', 19, 748, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (21, '鎧甲護持');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (21, '鎧甲護持', 'AC', -3);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (21, '鎧甲護持', '40508,40504,40521,88', '45,3,3,3');"));
    }

    @Test void selectedClientUsesSharedInventoryCallbackAndExactCccd()
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

    @Test void retainedSourceBindsMinusThreeToSelectedBodyArmor() throws Exception {
        String skillUse = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/貓大src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("else if (this._skillId == 21)"));
        assertTrue(skillUse.contains(
                "pc2.getInventory().getItem(this._itemobjid)"));
        assertTrue(skillUse.contains(
                "item.getItem().getType2() == 2 && item.getItem().getType() == 2"));
        assertTrue(skillUse.contains(
                "item.setSkillArmorEnchant(pc2, this._skillId, this._skill.getBuffDuration() * 1000)"));

        String item = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/古老src/com/lineage/server/model/Instance/L1ItemInstance.java"),
                StandardCharsets.UTF_8);
        int method = item.indexOf("setSkillArmorEnchant(");
        int end = item.indexOf("setSkillWeaponEnchant(", method);
        String lease = item.substring(method, end);
        assertTrue(lease.contains("isEquipped()"));
        assertTrue(lease.contains("pc.addAc(-3)"));
        assertTrue(lease.contains("setAcByMagic(3)"));
        assertTrue(item.contains("new S_ServerMessage(308, getLogName())"));
    }

    @Test void productionUsesSharedItemLeaseAndWorldDispatcher() throws Exception {
        String catalog = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("static final int BLESSED_ARMOR = 21;"));
        assertTrue(catalog.contains("new ItemArmorEnchantSpell("));
        assertTrue(catalog.contains("20, 10, 1_800, ATTRIBUTE_UNSPECIFIED, 2"));
        assertTrue(catalog.contains("16, -3, 79, 161, 308"));

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int castStart = session.indexOf("private void useItemArmorEnchantSpell(");
        int castEnd = session.indexOf("private void useMagicalWeaponSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(cast.contains("armor.armorType() == 2"));
        assertTrue(cast.contains("itemArmorEnchantStatus.activate("));
        assertTrue(cast.contains("cancellation-immune=true"));
        assertTrue(session.contains("itemArmorEnchantStatus.modifier("));
        assertTrue(session.contains("itemArmorEnchantStatus.expire(nowNanos)"));
        int armorUseStart = session.indexOf("private void useSourceArmor(");
        int armorUseEnd = session.indexOf(
                "private void useNativeArmor(", armorUseStart);
        assertTrue(session.substring(armorUseStart, armorUseEnd).contains(
                "!equipmentChangePermitted()"));
        int equipmentPhaseStart = session.indexOf(
                "private boolean equipmentChangePermitted()");
        int equipmentPhaseEnd = session.indexOf(
                "private void usePcActionHelper(", equipmentPhaseStart);
        String equipmentPhases = session.substring(
                equipmentPhaseStart, equipmentPhaseEnd);
        assertTrue(equipmentPhases.contains("phase == Phase.CHANNEL"));
        assertTrue(equipmentPhases.contains("phase == Phase.ROOM"));
        assertTrue(equipmentPhases.contains("phase == Phase.BATTLE"));
        assertTrue(equipmentPhases.contains("phase == Phase.WORLD"));
        assertTrue(session.contains("int armorClass = battleArmorClass(now);"));
        assertTrue(session.contains(
                "int armorClass = battleArmorClass(nowNanos);"));
    }
}
