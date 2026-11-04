package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill27 producer, retained durability/stack rules and WORLD routes. */
final class NativeWorldWeaponBreakSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackNpc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1Inventory.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void selectedAndSecondaryRowsStayExact() throws Exception {
        String primary = LocalResources.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (27, 0, '壞物術', 4, 2, 32, 0, 40318, 1, 3000, 0, 'buff', 3, 0, 0, 0, 0, 10, 2, 4, -10, 4, 0, 0, 4, '$1453', 19, 172, 0, 0, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (27, '壞物術', 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (27, '壞物術', 3);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (27, '壞物術', NULL, NULL);"));

        String secondary = LocalResources.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (27, '壞物術', 4, 2, 32, 0, 40318, 1, 3000, 0, 'buff', 3, 0, 0, 0, 0, 10, 2, 4, -10, 4, 0, 0, 4, '$1453', 19, 172, 0, 0, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (27, '壞物術', 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (27, '壞物術', 3);"));
    }

    @Test
    void selectedClientProducerRemainsCccdhhWithThreeSecondClock()
            throws Exception {
        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB96F:", "loc_4CB989:");
        assertTrue(dispatch.contains("jumptable 004CB66E cases 26,45"));
        assertTrue(dispatch.contains("mov     edx, 4"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));

        String producer = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        Path recovered = Path.of(
                "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
        assertEquals(3_000, peWordAtVirtualAddress(
                LocalResources.readAllBytes(recovered), 0x635774 + 26 * 2));

        String packetReader = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String targetOnly = between(packetReader,
                "case 1:// 初級治癒術 1300", "default:");
        assertTrue(targetOnly.contains("case 27:// 壞物術 4200"));
        assertTrue(targetOnly.contains("targetId = readD();"));
        assertTrue(targetOnly.contains("checkLoc = true;"));
    }

    @Test
    void retainedProbabilityPlayerDamageAndNpcStackRulesStayExact()
            throws Exception {
        String magic = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"),
                StandardCharsets.UTF_8);
        String configured = between(magic,
                "private int calcProbabilityWithConfig(",
                "private int getTargetResistValue(");
        assertTrue(configured.contains("probability = config.getProbHigh();"));
        assertTrue(configured.contains("probability = config.getProbEqual();"));
        assertTrue(configured.contains("probability = config.getProbLow();"));
        assertTrue(configured.contains(
                "probability += _pc.getInt() * config.getBonusInt();"));
        assertTrue(configured.contains("probability += _pc.getMagicHit();"));
        assertTrue(configured.contains(
                "probability -= targetMr * config.getMrMultiplier();"));

        String use = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        String effect = between(use,
                "} else if (this._skillId == WEAPON_BREAK) { // 武器破壞",
                "} else if (this._skillId == FOG_OF_SLEEPING) {");
        assertTrue(effect.contains("random.nextInt(this._user.getInt() / 3) + 1"));
        assertTrue(effect.contains("new S_ServerMessage(268, weapon.getLogName())"));
        assertTrue(effect.contains(
                "pc.getInventory().receiveDamage(weapon, weaponDamage)"));
        assertTrue(effect.contains("((L1NpcInstance) cha).addWeaponBreakStack();"));

        String inventory = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1Inventory.java"),
                StandardCharsets.UTF_8);
        String receiveDamage = between(inventory,
                "public L1ItemInstance receiveDamage(final L1ItemInstance item, final int count)",
                "public L1ItemInstance recoveryDamage(");
        assertTrue(receiveDamage.contains(
                "final int maxDurability = item.getEnchantLevel() + 5;"));
        assertTrue(receiveDamage.contains(
                "if (durability > maxDurability)"));

        String npc = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(npc.contains("if (this._weaponBreakStack < 10)"));
        assertTrue(npc.contains("this._weaponBreakStack++;"));

        String attack = LocalResources.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackNpc.java"),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains("dmg = dmg * (100 - stacks * 5) / 100.0;"));
        assertTrue(attack.contains("if (dmg < 1)"));
    }

    @Test
    void catalogAndBattleWorldRoutesStayConnected() throws Exception {
        BattleSpellCatalog.WeaponBreakSpell spell =
                BattleSpellCatalog.weaponBreakSpell(
                        BattleSpellCatalog.WEAPON_BREAK);
        assertEquals(3, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(172, spell.effectGfx());
        assertEquals(32, spell.mpCost());
        assertEquals(3_000, spell.reuseDelayMillis());
        assertEquals(4, spell.maximumRange());
        assertEquals(280, spell.failureMessage());
        assertEquals(InventoryCatalog.MAGIC_GEM, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(58, spell.probabilityForTarget(50, 49, 15, 5, 0, 20, false));
        assertEquals(19, spell.probabilityForTarget(50, 49, 15, 5, 0, 20, true));
        assertEquals(32, spell.effectiveMpCost(12, 0));
        assertEquals(29, spell.effectiveMpCost(15, 0));

        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String battle = between(session,
                "private void useWeaponBreakSpell(",
                "private void useClassicWorldWeaponBreakSpell(");
        String world = between(session,
                "private void useClassicWorldWeaponBreakSpell(",
                "private void useClassicWorldWeaknessSpell(");
        assertTrue(battle.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(world.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(world.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(world.contains("!map.isSafetyZone(positionX, positionY)"));
        assertTrue(world.contains("playerTarget.applyWeaponBreakWeaponDamage(weaponDamage)"));
        assertTrue(world.contains("hub.addWeaponBreakStackClassicWorldMonster("));
        assertTrue(world.contains("owner.classicWorldCompanionSnapshots()"));
        assertTrue(world.contains("companionControlled.addWeaponBreakStack()"));
        assertTrue(world.contains("companionControlled.earthBindActive(castNow)"));
        assertTrue(world.contains("timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(session.contains(
                "BattleWeaponBreak.durabilityAfterDamage(\n                        current.durability(), current.enchantLevel(), points)"));
        assertTrue(occurrences(session, "BattleWeaponBreak.damageAfterStacks(") >= 4);
        assertFalse(world.contains("Protocol.objectSpellRequest(payload)"));
    }

    @Test
    void releaseCoverageContractKeepsTheWholeNativeSlice() throws Exception {
        String coverage = LocalResources.readString(Path.of("tools/protocol_coverage.py"),
                StandardCharsets.UTF_8);
        assertTrue(coverage.contains(
                "skill27 Weapon Break / 壞物術 follows selected LiTo.bin"));
        assertTrue(coverage.contains("exact object-coordinate cccdhh 14 03 02"));
        assertTrue(coverage.contains("caps new durability at enchant+5"));
        assertTrue(coverage.contains("up to ten stacks"));
        assertTrue(coverage.contains("physical damage42->39"));
    }

    private static int peWordAtVirtualAddress(byte[] data, int address) {
        ByteBuffer bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = bytes.getInt(0x3C);
        int sections = Short.toUnsignedInt(bytes.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(bytes.getShort(pe + 20));
        int optional = pe + 24;
        int imageBase = bytes.getInt(optional + 28);
        int rva = address - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            int virtualSize = bytes.getInt(section + 8);
            int sectionRva = bytes.getInt(section + 12);
            int rawSize = bytes.getInt(section + 16);
            int rawOffset = bytes.getInt(section + 20);
            long upper = Integer.toUnsignedLong(sectionRva)
                    + Math.max(Integer.toUnsignedLong(virtualSize),
                            Integer.toUnsignedLong(rawSize));
            if (Integer.toUnsignedLong(rva) >= Integer.toUnsignedLong(sectionRva)
                    && Integer.toUnsignedLong(rva) + 2 <= upper) {
                int offset = rawOffset + rva - sectionRva;
                return Short.toUnsignedInt(bytes.getShort(offset));
            }
        }
        throw new IllegalArgumentException("unmapped PE address " + address);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int at = 0; (at = source.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
