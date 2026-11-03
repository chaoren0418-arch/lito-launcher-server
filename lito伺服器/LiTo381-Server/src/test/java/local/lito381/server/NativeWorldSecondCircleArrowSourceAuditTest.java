package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skills15/16 and their ordinary-WORLD damaging-magic route. */
final class NativeWorldSecondCircleArrowSourceAuditTest {
    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerSelectedRowsAndRetainedWireStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (15, 0, '火箭', 2, 6, 6, 0, 0, 0, 10, 0, 'attack', 3, 8, 10, 2, 0, 0, 1, 64, 0, 10, 0, 0, 64, '$1968', 18, 1583, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (16, 0, '地獄之牙', 2, 7, 6, 0, 0, 0, 10, 0, 'attack', 3, 8, 10, 2, 0, 0, 2, 64, 0, 10, 0, 0, 128, '$1969', 18, 1801, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (15, '火箭', '40495,40499,40505,88', '10,8,3,1');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (16, '地獄之牙', '40495,40499,40505,88', '10,8,3,1');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB989:", "loc_4CB9A3:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 14,15,33,65,75"));
        assertTrue(dispatch.contains("mov     edx, 0Ah"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        String clientUse = Files.readString(
                RETAINED.resolve("clientpackets/C_UseSkill.java"),
                StandardCharsets.UTF_8);
        String coordinateCases = between(
                clientUse, "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(coordinateCases.contains("case 15:// 火箭"));
        assertTrue(coordinateCases.contains("case 16:// 地獄之牙"));
        assertTrue(coordinateCases.contains("targetId = readD();"));
        assertTrue(coordinateCases.contains("targetX = readH();"));
        assertTrue(coordinateCases.contains("targetY = readH();"));
    }

    @Test
    void retainedMpCounterMagicAndElementMappingStayExact() throws Exception {
        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "(this._player.getInt() > 12) && (this._skillId > HOLY_WEAPON)"));
        assertTrue(skillUse.contains(
                "(this._player.getInt() > 13) && (this._skillId > STALAC)"));
        int exceptStart = skillUse.indexOf(
                "private static final int[] EXCEPT_COUNTER_MAGIC");
        int exceptEnd = skillUse.indexOf("};", exceptStart);
        String exceptions = skillUse.substring(exceptStart, exceptEnd);
        assertFalse(exceptions.matches("(?s).*\\b15\\b.*"));
        assertFalse(exceptions.matches("(?s).*\\b16\\b.*"));

        String skills = Files.readString(
                RETAINED.resolve("templates/L1Skills.java"),
                StandardCharsets.UTF_8);
        assertTrue(skills.contains("ATTR_EARTH = 1"));
        assertTrue(skills.contains("ATTR_FIRE = 2"));
        String magic = Files.readString(
                RETAINED.resolve("model/L1MagicMode.java"),
                StandardCharsets.UTF_8);
        assertTrue(magic.contains("case L1Skills.ATTR_EARTH:"));
        assertTrue(magic.contains("resist = _targetPc.getEarth();"));
        assertTrue(magic.contains("case L1Skills.ATTR_FIRE:"));
        assertTrue(magic.contains("resist = _targetPc.getFire();"));
        assertTrue(magic.contains(
                "final double attrDeffence = resistFloor / 32.0;"));
    }

    @Test
    void sharedBattleAndWorldImplementationKeepsSelectedAttributes() throws Exception {
        BattleSpellCatalog.AttackSpell fire = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.FIRE_ARROW);
        BattleSpellCatalog.AttackSpell stalac = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.STALAC);
        assertTrue(fire.attribute() == BattleSpellCatalog.ATTRIBUTE_EARTH);
        assertTrue(stalac.attribute() == BattleSpellCatalog.ATTRIBUTE_FIRE);

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int dispatchStart = session.indexOf(
                "BattleSpellCatalog.AttackSpell worldAttack");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.AreaAttackSpell worldAreaAttack",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.attackSpell(selector.skillId())"));
        assertTrue(dispatch.contains(
                "useClassicWorldAttackSpell(payload, worldAttack)"));

        int handlerStart = session.indexOf(
                "private void useClassicWorldAttackSpell(");
        int handlerEnd = session.indexOf(
                "private void publishClassicWorldAttackSpellPacket(",
                handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains("learnedSkills.contains(spell.skillId())"));
        assertTrue(handler.contains("map.attackPosition("));
        assertTrue(handler.contains("spell.effectiveMpCost("));
        assertTrue(handler.contains("playerTarget.timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(handler.contains(
                "effectiveSpellElementalResistance(spell.attribute())"));
        assertTrue(handler.contains("ordinaryMagicDamage("));
        assertTrue(handler.contains("Protocol.attackSpell("));
        assertTrue(handler.contains("takeClassicWorldPlayerDamage("));
        assertTrue(handler.contains("damageClassicWorldMonster("));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
