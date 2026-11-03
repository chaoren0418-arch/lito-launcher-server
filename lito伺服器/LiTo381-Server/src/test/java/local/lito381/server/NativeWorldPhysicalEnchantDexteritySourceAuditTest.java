package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill26/42 producers, retained semantics and shared WORLD route. */
final class NativeWorldPhysicalEnchantDexteritySourceAuditTest {
    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void selectedAndSecondaryRowsStayExact() throws Exception {
        String primary = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (26, 0, '通暢氣脈術', 4, 1, 50, 0, 0, 0, 10, 1200, 'buff', 4, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, 0, 2, '$1452', 19, 750, 0, 294, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (26, '通暢氣脈術', 'DEX', 5);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (26, '通暢氣脈術', NULL, NULL);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (42, 0, '體魄強健術', 6, 1, 50, 0, 0, 0, 10, 1200, 'buff', 4, 0, 0, 0, 0, 0, 2, 2, 0, -1, 0, 0, 2, '$1462', 19, 6342, 0, 292, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (42, '體魄強健術', 'STR', 5);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (42, '體魄強健術', NULL, NULL);"));

        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (26, '通暢氣脈術', 4, 1, 50, 0, 0, 0, 0, 1200, 'buff', 4, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, 0, 2, '$1452', 19, 750, 0, 294, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (26, '通暢氣脈術', 'DEX', 5);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (26, '通暢氣脈術', NULL, NULL);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (42, '體魄強健術', 6, 1, 50, 0, 0, 0, 0, 1200, 'buff', 4, 0, 0, 0, 0, 0, 2, 2, 0, -1, 0, 0, 2, '$1462', 19, 6342, 0, 292, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (42, '體魄強健術', 'STR', 5);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (42, '體魄強健術', NULL, NULL);"));
    }

    @Test
    void selectedClientProducerRemainsObjectTargetCccd() throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB741:", "loc_4CB755:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 25,41,67"));
        assertTrue(dispatch.contains("call    sub_4C9C80"));

        String picker = between(assembly,
                "sub_4C9C80 proc near", "sub_4C9C80 endp");
        assertTrue(picker.contains("call    sub_4C9A20"));
        assertTrue(picker.contains(
                "mov     dword_75A82C, offset sub_4C9A20"));

        String producer = between(assembly,
                "sub_4C9A20 proc near", "sub_4C9A20 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));
        assertTrue(producer.contains("push    esi"));

        String packetReader = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String targetOnly = between(packetReader,
                "case 1:// 初級治癒術 1300", "default:");
        assertTrue(targetOnly.contains("case 26:// 通暢氣脈術 1250"));
        assertTrue(targetOnly.contains("case 42:// 體魄強健術 1650"));
        assertTrue(targetOnly.contains("targetId = readD();"));

        String strengthReceiver = between(assembly,
                "sub_4AFE90 proc near", "sub_4AFE90 endp");
        assertTrue(strengthReceiver.contains("mov     si, [ecx]"));
        assertTrue(strengthReceiver.contains("mov     al, [ecx+2]"));
        assertTrue(strengthReceiver.contains("mov     byte_8348B0, al"));
        assertTrue(strengthReceiver.contains("mov     byte_8348CB, al"));
        assertTrue(strengthReceiver.contains("cmp     bl, 2"));
    }

    @Test
    void retainedMpAndDexterityPacketOrderingStayExact() throws Exception {
        String skillId = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillId.contains(
                "public static final int PHYSICAL_ENCHANT_DEX = 26;"));
        assertTrue(skillId.contains(
                "public static final int PHYSICAL_ENCHANT_STR = 42;"));

        String use = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        String consume = between(use,
                "// MP的INT減免", "if (currentHp < this._hpConsume + 1)");
        assertTrue(consume.contains(
                "(this._player.getInt() > 12) && (this._skillId > HOLY_WEAPON)"));
        assertTrue(consume.contains("case PHYSICAL_ENCHANT_DEX:// 通暢氣脈術"));
        assertTrue(consume.contains(
                "this._player.getInventory().checkEquipped(20013)"));
        assertTrue(consume.contains("case PHYSICAL_ENCHANT_STR:// 體魄強健術"));
        assertTrue(consume.contains(
                "this._player.getInventory().checkEquipped(20015)"));
        assertTrue(consume.contains(
                "this._mpConsume = this._mpConsume >> 1;"));
        assertTrue(consume.indexOf("this._mpConsume = Math.max(this._mpConsume, 1)")
                > consume.indexOf("case PHYSICAL_ENCHANT_DEX:"));
        assertTrue(consume.indexOf("getOriginalMagicConsumeReduction()")
                > consume.indexOf("this._mpConsume = Math.max(this._mpConsume, 1)"));

        String icon = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/IndependentSkillIcon.java"),
                StandardCharsets.UTF_8);
        String dexIcon = between(icon,
                "case PHYSICAL_ENCHANT_DEX: {", "case PHYSICAL_ENCHANT_STR: {");
        assertTrue(dexIcon.contains(
                "timeSec == 0 ? 1 : getAbility(PHYSICAL_ENCHANT_DEX, AbilityType.DEX, 5)"));
        assertTrue(dexIcon.contains("new S_Dexup(pc, value, timeSec)"));
        String strIcon = between(icon,
                "case PHYSICAL_ENCHANT_STR: {", "case GLOWING_AURA:");
        assertTrue(strIcon.contains(
                "timeSec == 0 ? 1 : getAbility(PHYSICAL_ENCHANT_STR, AbilityType.STR, 5)"));
        assertTrue(strIcon.contains("new S_Strup(pc, value, timeSec)"));

        String writer = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Dexup.java"),
                StandardCharsets.UTF_8);
        assertTrue(writer.contains("this.writeH(time);"));
        assertTrue(writer.contains("this.writeC(pc.getDex());"));
        assertTrue(writer.contains("this.writeC(type);"));

        String strengthWriter = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Strup.java"),
                StandardCharsets.UTF_8);
        assertTrue(strengthWriter.contains("this.writeH(time);"));
        assertTrue(strengthWriter.contains("this.writeC(pc.getStr());"));
        assertTrue(strengthWriter.contains(
                "this.writeC(pc.getInventory().getWeight240());"));
        assertTrue(strengthWriter.contains("this.writeC(type);"));
    }

    @Test
    void catalogAndWorldRouteStayConnected() throws Exception {
        BattleSpellCatalog.AbilityBuffSpell spell =
                BattleSpellCatalog.abilityBuffSpell(
                        BattleSpellCatalog.PHYSICAL_ENCHANT_DEX);
        assertEquals(3, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(750, spell.effectGfx());
        assertEquals(50, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.Ability.DEXTERITY, spell.ability());
        assertEquals(5, spell.modifier());
        assertEquals(5, spell.activeType());
        assertEquals(1, spell.stopType());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertEquals(50, spell.effectiveMpCost(12, 0, false));
        assertEquals(47, spell.effectiveMpCost(15, 0, false));
        assertEquals(21, spell.effectiveMpCost(15, 2, true));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int methodStart = session.indexOf("private void useAbilityBuffSpell(");
        int methodEnd = session.indexOf(
                "private void sendAbilityBuffStop(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("sameNonemptyClan(target)"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("InventoryCatalog.DEXTERITY_MAGIC_HELM"));
        assertTrue(method.contains("spell.effectiveMpCost("));
        assertTrue(method.contains("target.abilityStatus.activateDexterity("));
        assertTrue(method.contains("Protocol.mageDexterity(spell.durationSeconds()"));
        assertTrue(method.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(method.contains("revealInvisibility(\"spell-\""));
        assertFalse(method.contains("phase != Phase.BATTLE || playerHp <= 0"));

        BattleSpellCatalog.AbilityBuffSpell strength =
                BattleSpellCatalog.abilityBuffSpell(
                        BattleSpellCatalog.PHYSICAL_ENCHANT_STR);
        assertEquals(5, strength.row());
        assertEquals(1, strength.column());
        assertEquals(19, strength.actionId());
        assertEquals(6342, strength.effectGfx());
        assertEquals(50, strength.mpCost());
        assertEquals(10, strength.reuseDelayMillis());
        assertEquals(1_200, strength.durationSeconds());
        assertEquals(BattleSpellCatalog.Ability.STRENGTH, strength.ability());
        assertEquals(5, strength.modifier());
        assertEquals(5, strength.activeType());
        assertEquals(1, strength.stopType());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                strength.requestShape());
        assertEquals(50, strength.effectiveMpCost(12, 0, false));
        assertEquals(47, strength.effectiveMpCost(15, 0, false));
        assertEquals(21, strength.effectiveMpCost(15, 2, true));
        assertTrue(method.contains("InventoryCatalog.STRENGTH_MAGIC_HELM"));
        assertTrue(method.contains("target.abilityStatus.activateStrength("));
        assertTrue(method.contains(
                "Protocol.mageStrength(spell.durationSeconds()"));
        assertTrue(method.contains("target.currentWeight240(now)"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
