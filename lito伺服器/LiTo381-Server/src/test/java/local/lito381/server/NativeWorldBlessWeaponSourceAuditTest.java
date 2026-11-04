package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill48 and both ordinary-WORLD player physical consumers. */
final class NativeWorldBlessWeaponSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedWeaponRulesStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (48, 0, '祝福魔法武器', 6, 7, 20, 0, 0, 0, 10, 1200, 'buff', 3, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 128, '$1985', 19, 2176, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (48, '祝福魔法武器', 'BOW_HIT', 2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (48, '祝福魔法武器', 'DMG', 2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (48, '祝福魔法武器', 'HIT', 2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (48, '祝福魔法武器', NULL, NULL);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(dispatch.contains("cases 7,8,22,36,42,43,47,54"));
        assertTrue(dispatch.contains("mov     dl, bl"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("push    eax"));
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        String clientUse = retained("clientpackets/C_UseSkill.java");
        assertTrue(clientUse.contains("case 48:"));
        assertTrue(clientUse.contains("targetId = readD();"));
        String skillUse = retained("model/skill/L1SkillUse.java");
        assertTrue(skillUse.contains("{ HOLY_WEAPON, BLESS_WEAPON, ENCHANT_WEAPON }"));
        assertTrue(skillUse.contains("this._skillId == BLESS_WEAPON"));
        assertTrue(skillUse.contains("pc.getWeapon() == null"));
        assertTrue(skillUse.contains("new S_ServerMessage(79)"));
        assertTrue(skillUse.contains(
                "new S_ServerMessage(161, pc.getWeapon().getLogName(), \"$245\", \"$247\")"));
        String ability = retained("model/Instance/L1PcBuffAbilityCalc.java");
        assertTrue(ability.contains(
                "AbilityType.HIT, v -> pc.addHitup(v * multiplier)"));
        assertTrue(ability.contains(
                "AbilityType.DMG, v -> pc.addDmgup(v * multiplier)"));
        assertTrue(ability.contains(
                "AbilityType.BOW_HIT, v -> pc.addBowHitup(v * multiplier)"));
    }

    @Test
    void worldDispatcherAdmissionLeaseAndBothConsumersStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String dispatch = between(session,
                "BattleSpellCatalog.DamageProtectionSpell worldDamageProtection",
                "BattleSpellCatalog.AquaProtectSpell worldAquaProtect");
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.WeaponBuffSpell worldWeaponBuff"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.weaponBuffSpell(selector.skillId())"));
        assertTrue(dispatch.contains(
                "useWeaponBuffSpell(payload, worldWeaponBuff)"));

        String handler = between(session,
                "private void useWeaponBuffSpell(",
                "private void useHasteSpell(");
        assertTrue(handler.contains("originPhase == Phase.WORLD"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("classicWorldCompanionSnapshots()"));
        assertTrue(handler.contains("hub.classicWorldMonster("));
        assertTrue(handler.contains("admitClassicWorldSpellFamily("));
        assertTrue(handler.contains("\"weapon-buff\""));
        assertTrue(handler.contains("positionX != originX"));
        assertTrue(handler.contains("playerTarget.positionX != targetX"));
        assertTrue(handler.contains("locked.x() != targetX"));
        assertTrue(handler.contains("playerTarget.weaponBuffStatus.activate("));
        assertTrue(handler.contains("weapon-buff-missing"));
        assertTrue(handler.contains("weapon-buff-success"));
        assertCleanupAfterAllFourCommits(handler);
        assertTrue(handler.contains("phase=\" + originPhase"));

        String worldMonster = between(session,
                "private void attackClassicWorld(",
                "private void attackClassicWorldPlayer(");
        assertTrue(worldMonster.contains("weaponBuffStatus.modifier("));
        assertTrue(worldMonster.contains("weaponBuff.hit(ranged)"));
        assertTrue(worldMonster.contains("weaponBuff.damage(ranged)"));
        assertTrue(worldMonster.contains("weapon-buff-skill="));
        assertTrue(worldMonster.contains("weapon-buff-hit="));
        assertTrue(worldMonster.contains("weapon-buff-damage="));

        String worldPlayer = between(session,
                "private void attackClassicWorldPlayer(",
                "private void publishClassicWorldPhysicalAttack(");
        assertTrue(worldPlayer.contains("weaponBuffStatus.modifier("));
        assertTrue(worldPlayer.contains("weaponBuff.hit(ranged)"));
        assertTrue(worldPlayer.contains("weaponBuff.damage(ranged)"));
        assertTrue(session.contains("weaponBuffStatus.expire(nowNanos)"));
        assertTrue(session.contains(
                "Cancellation player cleared weapon buff target="));

        String acceptance = LocalResources.readString(Path.of(
                "tools/classic_world_bless_weapon_selftest.py"),
                StandardCharsets.UTF_8);
        assertTrue(acceptance.contains(
                "request_prefix = bytes((0x14, *SELECTOR))"));
        assertTrue(acceptance.contains(
                "npc_request = request_prefix + struct.pack"));
        assertTrue(acceptance.contains(
                "pc_request = request_prefix + struct.pack"));
        assertTrue(acceptance.contains("exact_damage_delta"));
        assertTrue(acceptance.contains("encrypted_stream_aligned"));
        String barrierAcceptance = LocalResources.readString(Path.of(
                "tools/classic_world_bless_weapon_barrier_selftest.py"),
                StandardCharsets.UTF_8);
        assertTrue(barrierAcceptance.contains("ABSOLUTE_BARRIER"));
        assertTrue(barrierAcceptance.contains("reason=spell-48"));
        assertTrue(barrierAcceptance.contains(
                "REQUEST_PREFIX + struct.pack(\"<I\", TARGET_ID)"));
        String config = LocalResources.readString(Path.of(
                "src/test/resources/server-config/config/server.bless-weapon-native-world-selftest.properties"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("database.read.only=true"));
        assertTrue(config.contains("classic.world.monster.ai.enabled=false"));
        assertTrue(config.contains("shadow.fang.damage.fixture.weapon.roll=3"));
    }

    private static String retained(String relative) throws Exception {
        return LocalResources.readString(RETAINED.resolve(relative).normalize(),
                StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }

    private static void assertCleanupAfterAllFourCommits(String method) {
        String commitNeedle = "spellReadyNanos.put(spell.skillId()";
        int commits = 0;
        int cursor = 0;
        while ((cursor = method.indexOf(commitNeedle, cursor)) >= 0) {
            commits++;
            cursor += commitNeedle.length();
        }
        int commit = method.lastIndexOf(commitNeedle);
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisibility = method.indexOf("revealInvisibility(", barrier);
        assertTrue(commits == 4);
        assertTrue(commit >= 0 && meditation > commit
                && barrier > meditation && invisibility > barrier);
        assertTrue(!method.contains("commitSpellReady(spell.skillId()"));
    }
}
